import 'package:flutter/services.dart';

class CastScreen {
  static const platform = MethodChannel("com.example.cast_screen");

  Future<void> startService() async {
    try {
      await platform.invokeMethod("startService");
      // ignore: empty_catches
    } catch (e) {}
  }

  Future<void> stopService() async {
    try {
      await platform.invokeMethod("stopService");
      // ignore: empty_catches
    } catch (e) {}
  }

  Future<void> checkService() async {
    try {
      await platform.invokeMethod("checkService");
      // ignore: empty_catches
    } catch (e) {}
  }

  Future<void> serviceCallback(Function(String) callback) async {
    platform.setMethodCallHandler((methodCall) async {
      callback(methodCall.method);
    });
  }
}
