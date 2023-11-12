package com.example.cast_screen;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class CastService extends Service implements ImageReader.OnImageAvailableListener {

    private final IBinder binder = new CastService.LocalBinder();
    private MediaProjection mediaProjection;
    public static String ACTION_HANDLE_DATA = "SEND_INTENT_DATA";
    public static String EXTRA_DATA = "INTENT_DATA";
    private DisplayMetrics metrics;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;

    public class LocalBinder extends Binder {
        public CastService getService() {
            return CastService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        metrics = getResources().getDisplayMetrics();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent data = null;
        int resultCode = 0;
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                if (action == ACTION_HANDLE_DATA) {
                    data = intent.getParcelableExtra(EXTRA_DATA);
                }
            }
            resultCode = intent.getIntExtra("result_code", 0);
        }
        if (data != null && resultCode != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startNotification();
                mediaProjection = ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(resultCode, data);
                mediaProjection.registerCallback(call, null);
                initImageReader();
                initVirtualDisplay();
            } else {
                stopSelf();
            }
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private MediaProjection.Callback call = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            destroy();
            super.onStop();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startNotification() {
        String CHANNEL_ID = "channel_screencast";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Pemberitahuan peringatan", NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        NotificationCompat.Builder notificationCompat = new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationCompat.setContentTitle("Cast Service");
        notificationCompat.setContentText("Proses latar belakang berjalan");
        notificationCompat.setSmallIcon(R.drawable.baseline_fit_screen_24);
        notificationCompat.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_fit_screen_24));
        notificationCompat.setOngoing(true);
        notificationCompat.setAutoCancel(false);
        notificationCompat.setPriority(NotificationCompat.PRIORITY_MAX);
        notificationCompat.setTicker("CAPTURE_STATUS");
        notificationCompat.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
        notificationCompat.setCategory(NotificationCompat.CATEGORY_PROGRESS);
        Notification notif = notificationCompat.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(405, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(405, notif);
        }
    }

    @SuppressLint("WrongConstant")
    private void initImageReader() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2);
            imageReader.setOnImageAvailableListener(this, null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("VIRTUAL_DISPLAY_123", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        try {
            Image image = imageReader.acquireNextImage();
            getImage(image);
        } catch (Exception e) {
        }
    }

    int pixelStride, rowStride, rowPadding;
    private CastCallback castCallback;
    private TaskImage taskImage;

    public void registerCastCallback(CastCallback castCallback) {
        this.castCallback = castCallback;
    }

    public void unregisterCastCallback() {
        this.castCallback = null;
    }

    private void getImage(Image image) {
        if (image != null) {
            Image.Plane plane = image.getPlanes()[0];
            pixelStride = plane.getPixelStride();
            rowStride = plane.getRowStride();
            rowPadding = rowStride - pixelStride * metrics.widthPixels;
            ByteBuffer buffer = plane.getBuffer();
            image.close();
            ByteBuffer[] buffers = { buffer };
            taskImage = new TaskImage();
            taskImage.execute(buffers);
            if (castCallback != null) {
                castCallback.onResultBufferImage(buffer);
            }
        }
    }

    private class TaskImage extends AsyncTask<ByteBuffer, Void, Void> {
        @Override
        protected Void doInBackground(ByteBuffer... buffers) {
//            try {
//                Bitmap bitmap = Bitmap.createBitmap(metrics.widthPixels + rowPadding / pixelStride, metrics.heightPixels, Bitmap.Config.ARGB_8888);
//                bitmap.copyPixelsFromBuffer(buffers[0]);
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 20, stream);
//                String baseData = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
////                UserData.setScreenBase(CastService.this, Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT));
//            } catch (Exception e) {
//            }
            return null;
        }
    }

    public void destroy() {
        if (taskImage != null) {
            try {
                taskImage.cancel(true);
            } catch (Exception e) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaProjection.unregisterCallback(call);
        }
        stopForeground(true);
        unregisterCastCallback();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (taskImage != null) {
            try {
                taskImage.cancel(true);
            } catch (Exception e) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaProjection.unregisterCallback(call);
        }
        stopForeground(true);
        unregisterCastCallback();
        super.onDestroy();
    }
}
