import 'dart:async';

import 'package:flutter/services.dart';

class BackgroundMediaPlayer {
  static const MethodChannel _channel =
      const MethodChannel('background_media_player_method');
  static const EventChannel _eventChannel =
  const EventChannel('background_media_player_event');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
  static void play() async {
    await _channel.invokeMethod('Play',);

  } static void toggle() async {
    await _channel.invokeMethod('Toggle');
  }

  static void next() async {
    await _channel.invokeMethod('Next');

  } static void prev() async {
    await _channel.invokeMethod('Prev');
  }

  static void stop() async {
    await _channel.invokeMethod('Stop');

  }

  static void f(){
    _eventChannel.receiveBroadcastStream([5]).listen((s){
      print(s);
    });

  }
}
