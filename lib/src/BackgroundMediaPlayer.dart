import 'dart:async';

import 'package:background_media_player/background_media_player.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

import 'Models/Modes.dart';
import 'Models/PlaybackState.dart';

class BackgroundMediaPlayer {
  /// Method Channel
  static const MethodChannel _channel =
      const MethodChannel('background_media_player_method');

  /// Event Channel
  static const EventChannel _eventChannel =
      const EventChannel('background_media_player_event');

  /// StreamSubscription to hold _eventChannel incoming stream
  StreamSubscription _subscription;

  /// static instance of this class for making singleton
  static BackgroundMediaPlayer _instance = BackgroundMediaPlayer._private();

  /// use this instance to call all methods of this class.
  /// This class is a singleton class.
  static BackgroundMediaPlayer get instance => _instance;

  /// Private Constructor
  BackgroundMediaPlayer._private();

  /// [RepeatMode] of Media Player
  RepeatMode repeatMode;

  ///[ShuffleMode] of Media Player
  ShuffleMode shuffleMode;

  ///[PlaybackState] of Media Player
  PlaybackState playbackState;

  /// [int] index of current Item Playing in [BackgroundMediaPlayer.mediaQueue]
  int currentIndex = 0;

  /// MediaPlayer Queue holds media
  /// It takes Map as element and the necessary fields
  /// of the each map item are :-
  ///{    "artist": "Baby Yoda",
  ///      "album": "My Playlist",
  ///      "albumArt":
  ///          "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
  ///      "title": "First Song",
  ///      "source": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
  ///      }
  ///      You can Pass Other fields as per requirement
  ///      but at plugin level they will get ignored
  ///      because I am currently picking only
  ///      `artist`,`album`,albumArt,`title`,source
  List mediaQueue = [];

  ///StreamControllers for media player

  final StreamController<PlaybackState> _playbackStateController =
      StreamController.broadcast();
  final StreamController<void> _prepareController =
      StreamController.broadcast();
  final StreamController<void> _playController = StreamController.broadcast();
  final StreamController<void> _pauseController = StreamController.broadcast();
  final StreamController<void> _skipToNextController =
      StreamController.broadcast();
  final StreamController<void> _skipToPrevController =
      StreamController.broadcast();
  final StreamController<void> _stopController = StreamController.broadcast();
  final StreamController<double> _seekController = StreamController.broadcast();
  final StreamController<void> _completionController =
      StreamController.broadcast();
  final StreamController<double> _bufferUpdateController =
      StreamController.broadcast();
  final StreamController<Map> _errorController = StreamController.broadcast();
  final StreamController<double> _durationController =
      StreamController.broadcast();
  final StreamController<double> _positionController =
      StreamController.broadcast();

  /// returns [PlaybackState] of media player
  Stream<PlaybackState> get onPlaybackStateChange =>
      _playbackStateController.stream;

  /// return event every time when media player gets prepared
  Stream<void> get onPrepared => _prepareController.stream;

  /// return event every time when media player gets played
  Stream<void> get onPlay => _playController.stream;

  /// return event every time when media player gets paused
  Stream<void> get onPause => _pauseController.stream;

  /// return event every time when media player gets skipToNext Action
  Stream<void> get onSkipToNext => _skipToNextController.stream;

  /// return event every time when media player gets skipToPrevious Action
  Stream<void> get onSkipToPrevious => _skipToPrevController.stream;

  /// return event every time when media player stops
  Stream<void> get onStopped => _stopController.stream;

  /// return event every time when media player gets seek Action
  Stream<double> get onSeek => _seekController.stream;

  /// return event every time when media player gets completed
  Stream<void> get onComplete => _completionController.stream;

  /// return event every time when media player buffers media
  Stream<double> get onBufferUpdate => _bufferUpdateController.stream;

  /// return Map having error code, every time when media player gets error
  Stream<Map> get onError => _errorController.stream;

  /// return duration every time when media player gets duration of media
  Stream<double> get onDurationUpdate => _durationController.stream;

  /// return position every time when media player is playing
  Stream<double> get onPositionUpdate => _positionController.stream;

  /// Initialises the Plugin
  /// return [Future] when fully initialise the plugin data.
  /// It is recommended to perform other operations after a
  /// successful initialisation.
  Future<bool> init() async {
    Map data = await _channel.invokeMethod("Init");
    currentIndex = data['currentItem'] ?? 0;
    shuffleMode = getShuffleModeFromInt(data['shuffleMode'] ?? -1);
    repeatMode = getRepeatModeFromInt(data['repeatMode'] ?? -1);
    playbackState = getPlaybackStateFromInt(data['playBackState'] ?? 0);
    List queue = data['mediaQueue'] ?? [];
    mediaQueue.clear();
    mediaQueue.addAll(queue);
    _subscription = _eventChannel.receiveBroadcastStream().listen(_onEvent);
    return true;
  }

  /// Callback for [_eventChannel] stream
  void _onEvent(dynamic eventData) async {
    /// get current Item index every time an event occurs
    await _getCurrentIndex();

    ///handle event calls
    if (eventData is Map && eventData != null && eventData.length > 0) {
      String eventKey = eventData.keys.first;
      switch (eventKey) {
        case "onPlaybackStateChange":
          _playbackStateController
              .add(_getCurrentPlaybackState(eventData[eventKey]['state']));
          break;
        case "onPrepared":
          _prepareController.add("Prepared");
          break;
        case "onPlay":
          _playController.add("Play");
          break;
        case "onPause":
          _pauseController.add("Pause");
          break;
        case "onSkipToNext":
          _skipToNextController.add("SkipToNext");
          break;
        case "onSkipToPrevious":
          _skipToPrevController.add("SkipToPrevious");
          break;
        case "onStopped":
          _stopController.add("Stop");
          break;
        case "onSeek":
          _seekController
              .add(double.parse(eventData[eventKey]['position'].toString()));
          break;
        case "onComplete":
          _completionController.add("Completed");
          break;
        case "onBufferUpdate":
          _bufferUpdateController
              .add(double.parse(eventData[eventKey]['percent'].toString()));
          break;
        case "onError":
          _errorController.add(eventData[eventKey]);
          break;
        case "onDurationUpdate":
          _durationController
              .add(double.parse(eventData[eventKey]['duration'].toString()));
          break;
        case "onPositionUpdate":
          _positionController
              .add(double.parse(eventData[eventKey]['position'].toString()));
          break;
        default:
          throw FlutterError("Undefined Event $eventKey");
      }
    }
  }

  /// Sets Notification Color
  /// If Album art is showing on full notification then this color will not
  /// be shown on notifications.
  static void setNotificationColor(int color) async {
    await _channel.invokeMethod("SetNotificationColor", color);
  }

  ///Sets [mediaQueue]
  Future<bool> setQueue(List mQueue) async {
    try {
      if (mQueue != null && mQueue.length > 0) {
        mediaQueue.clear();
        mediaQueue.addAll(mQueue);
        currentIndex = 0;
        await _channel.invokeMethod("SetQueue", mediaQueue);
        return true;
      } else
        throw FlutterError("Can not set an empty list.");
    } catch (err) {
      print(err);
      return false;
    }
  }

  ///Sets [repeatMode]
  void setRepeatMode(RepeatMode mode) async {
    repeatMode = mode;
    await _channel.invokeMethod("SetRepeatMode", mode.index - 1);
  }

  ///Sets [shuffleMode]
  void setShuffleMode(ShuffleMode mode) async {
    shuffleMode = mode;
    await _channel.invokeMethod("SetShuffleMode", mode.index - 1);
  }

  ///Get Current Item index.
  ///It automatically updates the [currentIndex].
  Future<void> _getCurrentIndex() async {
    currentIndex = await _channel.invokeMethod("GetCurrentIndex");
  }

  ///Get Current playbackState.
  ///It automatically updates the [playbackState].
  PlaybackState _getCurrentPlaybackState(int state) {
    playbackState = getPlaybackStateFromInt(state);
    return playbackState;
  }

  ///Play item at [index] of [mediaQueue] in MediaPlayer.
  ///also takes an optional parameter [mediaQueue], which if not null
  ///then will [setQueue] and the play the item on [index]
  void play(int index, {List mediaQueue}) async {
    currentIndex = index;
    if (mediaQueue != null && mediaQueue.length > 0) await setQueue(mediaQueue);
    await _channel.invokeMethod('Play', index);
  }

  /// Play/Pause action
  void toggle() async {
    int state = playbackState == PlaybackState.STATE_PAUSED ? 0 : 1;
    await _channel.invokeMethod('Toggle', state);
  }

  ///Skip to Next
  void next() async {
    await _channel.invokeMethod('Next');
  }

  ///Skip to Previous
  void prev() async {
    await _channel.invokeMethod('Prev');
  }

  ///Stop Media Player
  ///It just stop the service, it does not destroy and release media player.
  void stop() async {
    await _channel.invokeMethod('Stop');
  }

  ///Seek Current Position to a Particular Position
  void seekTo(int milliSec) async {
    await _channel.invokeMethod("SeekTo", milliSec);
  }

  void dispose() {
    _subscription?.cancel();
    _prepareController?.close();
    _playController?.close();
    _pauseController?.close();
    _skipToNextController?.close();
    _skipToPrevController?.close();
    _stopController?.close();
    _seekController?.close();
    _completionController?.close();
    _bufferUpdateController?.close();
    _errorController?.close();
    _durationController?.close();
    _positionController?.close();
  }
}
