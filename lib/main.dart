import 'package:cast_screen/cast_screen.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(const MainApp());
}

class MainApp extends StatefulWidget {
  const MainApp({super.key});

  @override
  State<MainApp> createState() => _MainAppState();
}

class _MainAppState extends State<MainApp> {
  late CastScreen castScreen;

  bool startCast = false;
  int woyo = 0;

  @pragma('vm:entry-point')
  @override
  void initState() {
    WidgetsFlutterBinding.ensureInitialized();
    castScreen = CastScreen();
    checkCast();
    castScreen.serviceCallback((String v) {
      print("Service callback $v");
      if (v == "service_start") {
        setState(() {
          startCast = true;
        });
      } else if (v == "service_stop") {
        setState(() {
          startCast = false;
        });
      } else if (v == "result_buffer_image") {
        setState(() {
          woyo++;
        });
      }
    });
    super.initState();
  }

  Future<void> checkCast() async {
    bool a = await castScreen.checkService();
    setState(() {
      startCast = a;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: SizedBox(
          width: double.infinity,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text('WOYO! $woyo'),
              ElevatedButton(
                onPressed: () async {
                  if (!startCast) {
                    await castScreen.startService();
                  } else {
                    await castScreen.stopService();
                  }
                },
                child: !startCast ? const Text("START") : const Text("STOP"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
