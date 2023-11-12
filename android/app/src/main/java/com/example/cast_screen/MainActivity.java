package com.example.cast_screen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.nio.Buffer;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {

    private static final String CHANNEL = "com.example.cast_screen";
    private MethodChannel methodChannel;
    private static final int PERMISSION_MEDIA_PROJECTION = 120;
    private boolean isRunService = false;
    private CastService castService;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        methodChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        methodChannel.setMethodCallHandler((call, result) -> {
            if (call.method.equals("startService")) {
                requestCastScreen();
                result.success(true);
            } else if (call.method.equals("checkService")) {
                result.success(isRunService);
            } else {
                result.notImplemented();
            }
        });
    }

    private void requestCastScreen() {
        if (!isRunService) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
                startActivityForResult(permissionIntent, PERMISSION_MEDIA_PROJECTION);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void startCastScreen(int resultCode, Intent data) {
        Intent i = new Intent(this, CastService.class);
        bindService(i, connection, Context.BIND_ABOVE_CLIENT);
        if (!isRunService) {
            i.setAction(CastService.ACTION_HANDLE_DATA);
            i.putExtra(CastService.EXTRA_DATA, data);
            i.putExtra("result_code", resultCode);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CastService.LocalBinder binder = (CastService.LocalBinder) service;
            castService = binder.getService();
            castService.registerCastCallback(new CastImageCallback(methodChannel));
            runOnUiThread(() -> methodChannel.invokeMethod("service_start", true));
            isRunService = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            runOnUiThread(() -> methodChannel.invokeMethod("service_stop", true));
            isRunService = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                startCastScreen(resultCode, data);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    class CastImageCallback implements CastCallback {

        private MethodChannel methodChannel;

        public CastImageCallback(MethodChannel methodChannel) {
            this.methodChannel = methodChannel;
        }

        @Override
        public void onResultBufferImage(Buffer buffer) {
            runOnUiThread(() -> methodChannel.invokeMethod("result_buffer_image", true));
        }
    }
}
