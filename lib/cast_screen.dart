import 'package:flutter/services.dart';

class CastScreen {
  static const platform = MethodChannel("com.example.cast_screen");

  Future<void> startService() async {
    try {
      await platform.invokeMethod("startService");
      // ignore: empty_catches
    } catch (e) {}
  }

  Future<void> checkService() async {
    try {
      await platform.invokeMethod("checkService");
      // ignore: empty_catches
    } catch (e) {}
  }

  Future<void> handlePlatformChannelMethods(VoidCallback callback) async {
    platform.setMethodCallHandler((methodCall) async {
      print('NativeChanell background...');
      print('>> ${methodCall.method}');
      callback();
    });
  }
}
