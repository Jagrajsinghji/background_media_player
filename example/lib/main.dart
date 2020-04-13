import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:background_media_player/background_media_player.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  @override
  void initState() {
    super.initState();
    BackgroundMediaPlayer.f();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: <Widget>[
            RaisedButton(onPressed: (){
              BackgroundMediaPlayer.play();
            },child: Text("Play"),),
            RaisedButton(onPressed: (){
              BackgroundMediaPlayer.toggle();
            },child: Text("Pause"),),
            RaisedButton(onPressed: (){
              BackgroundMediaPlayer.next();
            },child: Text("Next"),),
            RaisedButton(onPressed: (){
              BackgroundMediaPlayer.prev();
            },child: Text("Prev"),),
            RaisedButton(onPressed: (){
              BackgroundMediaPlayer.stop();
            },child: Text("Stop"),),
          ],
        ),

      ),
    );
  }
}
