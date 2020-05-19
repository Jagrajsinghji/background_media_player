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

  static StreamSubscription _subscription;

  static StreamController<int> _duartionController =
      StreamController.broadcast();
  static StreamController<int> _bufferController = StreamController.broadcast();
  static StreamController<PlaybackState> _playbackStateController =
      StreamController.broadcast();
  static StreamController<int> _positionController =
      StreamController.broadcast();

  static RepeatMode repeatMode;
  static ShuffleMode shuffleMode;
  static PlaybackState playbackState;
  static int currentItem = 0;
  static final List mediaQueue = [];

  static void init() async {
    Map data = await _channel.invokeMethod("Init");
    currentItem = data['currentItem'] ?? 0;
    shuffleMode = getShuffleModeFromInt(data['shuffleMode'] ?? -1);
    repeatMode = getRepeatModeFromInt(data['repeatMode'] ?? -1);
    playbackState = getPlaybackStateFromInt(data['playBackState'] ?? 0);
    List queue = data['mediaQueue'] ?? [];
    mediaQueue.clear();
//    queue.forEach((element) => mediaQueue.add(MediaItem.fromMap(element)));
    mediaQueue.addAll(queue);
    _subscription = _eventChannel.receiveBroadcastStream().listen(_onEvent);
  }

  static void setNotificationColor(int color) async {
    await _channel.invokeMethod("SetNotificationColor", color);
  }

  static void setQueue(List mQueue) async {
    mediaQueue.clear();
    mediaQueue.addAll(mQueue);
//    List<Map<String, String>> queue = [];
//    mediaQueue.forEach((element) => queue.add(element.toMap()));
    await _channel.invokeMethod("SetQueue", mQueue);
  }

  static void setRepeatMode(RepeatMode mode) async {
    repeatMode = mode;
    await _channel.invokeMethod("SetRepeatMode", mode.index - 1);
  }

  static void setShuffleMode(ShuffleMode mode) async {
    shuffleMode = mode;
    await _channel.invokeMethod("SetShuffleMode", mode.index - 1);
  }

  static Future<int> getCurrentIndex() async {
    currentItem = await _channel.invokeMethod("GetCurrentIndex");
    return currentItem;
  }

  static void play(int index) async {
    currentItem = index;
    await _channel.invokeMethod('Play', index);
  }

  static void toggle() async {
    await _channel.invokeMethod('Toggle');
  }

  static void next() async {
    await _channel.invokeMethod('Next');
  }

  static void prev() async {
    await _channel.invokeMethod('Prev');
  }

  static void stop() async {
    await _channel.invokeMethod('Stop');
  }

  static void seekTo(Duration duration) async {
    int millis = duration.inMilliseconds;
    await _channel.invokeMethod("SeekTo", millis);
  }

  static _onEvent(dynamic event) async {
    await getCurrentIndex();
    List<String> data = event.toString().split(":");
    switch (data[0]) {
      case "UpdateProgress":
        assert(data.length == 3);
        int _currentPos = int.parse(data[1] ?? "0");
        int _duration = int.parse(data[2] ?? "0");
        _positionController.sink.add(_currentPos);
        _duartionController.sink.add(_duration);

        break;
      case "UpdateState":
        assert(data.length == 2);
        int _state = int.parse(data[1] ?? "0");
        _playbackStateController.sink.add(getPlaybackStateFromInt(_state));
        break;
      case "BufferUpdate":
        assert(data.length == 3);
        int _percent = int.parse(data[1] ?? "0");
        int _duration = int.parse(data[2] ?? "0");
        _bufferController.sink.add(_percent);
        _duartionController.sink.add(_duration);
        break;
      default:
        print("no callback for event ${data[0]}");
    }
  }

//  static void onUpdateProgress(Function(int position,int duration) callback){
//    _listeners.addAll({
//      "UpdateProgress":callback
//    });
//  }
//  static void onUpdateState(Function(PlaybackState state) callback){
//    _listeners.addAll({
//      "UpdateState":callback
//    });}
//  static void onBufferUpdate(Function(int percent, int duration)callback){
//    _listeners.addAll({
//      "BufferUpdate":callback
//    });
//  }

  /// cancels event channel subscription only.
  /// caution : it does not destroys the media player
  static cancelStreams() {
    _subscription?.cancel();
    _playbackStateController?.close();
    _bufferController?.close();
    _duartionController?.close();
    _positionController?.close();
  }

  static Stream<int> get onBufferUpdate => _bufferController.stream;

  static Stream<PlaybackState> get onPlaybackStateChange =>
      _playbackStateController.stream;

  static Stream<int> get onPositionUpdate => _positionController.stream;

  static Stream<int> get onDurationUpdate => _duartionController.stream;
}
