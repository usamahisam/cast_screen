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

  @override
  void initState() {
    castScreen = CastScreen();
    castScreen.handlePlatformChannelMethods(() {});
    super.initState();
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
              const Text('Hello World!'),
              ElevatedButton(
                onPressed: () async {
                  await castScreen.startService();
                },
                child: const Text("WOI"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
