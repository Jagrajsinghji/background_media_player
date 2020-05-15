import 'dart:async';

import 'package:background_media_player/background_media_player.dart';
import 'package:flutter/services.dart';

import 'Models/Modes.dart';
import 'Models/PlaybackState.dart';

class BackgroundMediaPlayer {
  static const MethodChannel _channel =
      const MethodChannel('background_media_player_method');
  static const EventChannel _eventChannel =
      const EventChannel('background_media_player_event');

  static RepeatMode repeatMode;
  static ShuffleMode shuffleMode;
  static PlaybackState playbackState;
  static int currentItem = 0;
  static List<MediaItem> mediaQueue = [];

  static void init() async {
    Map data = await _channel.invokeMethod("Init");
    currentItem = data['currentItem'] ?? 0;
    shuffleMode = getShuffleModeFromInt(data['shuffleMode'] ?? -1);
    repeatMode = getRepeatModeFromInt(data['repeatMode'] ?? -1);
    playbackState = getPlaybackStateFromInt(data['playBackState'] ?? 0);
    List queue = data['mediaQueue'] ?? [];
    mediaQueue.clear();
    queue.forEach((element) => mediaQueue.add(MediaItem.fromMap(element)));
    _eventChannel.receiveBroadcastStream().listen(_onEvent);
  }

  static void setQueue(List<MediaItem> mediaQueue) async {
    List<Map<String, String>> queue = [];
    mediaQueue.forEach((element) => queue.add(element.toMap()));
    await _channel.invokeMethod("SetQueue", queue);
  }

  static void setRepeatMode(RepeatMode mode) async {
    await _channel.invokeMethod("SetRepeatMode", mode.index - 1);
  }

  static Future<RepeatMode> getRepeatMode() async {
    int rMode = await _channel.invokeMethod("GetRepeatMode");
    repeatMode = getRepeatModeFromInt(rMode);
    return repeatMode;
  }

  static void setShuffleMode(ShuffleMode mode) async {
    await _channel.invokeMethod("SetShuffleMode", mode.index - 1);
  }

  static Future<ShuffleMode> getShuffleMode() async {
    int rMode = await _channel.invokeMethod("GetShuffleMode");
    shuffleMode = getShuffleModeFromInt(rMode);
    return shuffleMode;
  }

  static Future<PlaybackState> getPlaybackState() async {
    int rMode = await _channel.invokeMethod("GetPlaybackState");
    playbackState = getPlaybackStateFromInt(rMode);
    return playbackState;
  }

  static Future<int> getCurrentIndex() async {
    currentItem = await _channel.invokeMethod("GetCurrentIndex");
    return currentItem;
  }

  static Future<List<MediaItem>> getQueue() async {
    List queue =
        await _channel.invokeMethod("GetQueue") ?? [];
    mediaQueue.clear();
    queue.forEach((element) => mediaQueue.add(MediaItem.fromMap(element)));
    return mediaQueue;
  }

  static void play(int index) async {
    currentItem = index;
    await _channel.invokeMethod('Play', index);
    await getPlaybackState();
  }

  static void toggle() async {
    await _channel.invokeMethod('Toggle');
    await getPlaybackState();
  }

  static void next() async {
    await _channel.invokeMethod('Next');
    await getPlaybackState();
    await getCurrentIndex();
  }

  static void prev() async {
    await _channel.invokeMethod('Prev');
    await getPlaybackState();
    await getCurrentIndex();

  }

  static void stop() async {
    await _channel.invokeMethod('Stop');
    await getPlaybackState();
  }

  static void seekTo(Duration duration) async {
    int millis = duration.inMilliseconds;
    await _channel.invokeMethod("SeekTo", millis);
  }

  static _onEvent(dynamic name)async{
    print("Event performed: $name");


  }
}