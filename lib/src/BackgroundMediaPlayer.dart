import 'dart:async';

import 'package:background_media_player/background_media_player.dart';
import 'package:flutter/services.dart';

import 'Models/MediaPlayerState.dart';
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
  static StreamSubscription _subscription;

  /// StreamController of type [MediaPlayerState]
  /// tells the current state of MediaPlayer.
  static StreamController<MediaPlayerState> _mediaPlayerStateController =
      StreamController.broadcast();

  /// [RepeatMode] of Media Player
  static RepeatMode repeatMode;

  ///[ShuffleMode] of Media Player
  static ShuffleMode shuffleMode;

  ///[PlaybackState] of Media Player
  static PlaybackState playbackState;

  /// [int] index of current Item Playing in [BackgroundMediaPlayer.mediaQueue]
  static int currentItem = 0;

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
  static List mediaQueue = [];

  /// Initialises the Plugin
  /// return [Future] when fully initialise the plugin.
  /// It is recommended to perform other operations after a
  /// successful initialisation.
  static Future<bool> init() async {
    Map data = await _channel.invokeMethod("Init");
    currentItem = data['currentItem'] ?? 0;
    shuffleMode = getShuffleModeFromInt(data['shuffleMode'] ?? -1);
    repeatMode = getRepeatModeFromInt(data['repeatMode'] ?? -1);
    playbackState = getPlaybackStateFromInt(data['playBackState'] ?? 0);
    List queue = data['mediaQueue'] ?? [];
    mediaQueue.clear();
    mediaQueue.addAll(queue);
    _subscription = _eventChannel.receiveBroadcastStream().listen(_onEvent);
    return true;
  }

  /// Sets Notification Color of Buffering Notification.
  /// Buffering Notification shows when MediaPlayer is buffering while playing
  static void setNotificationColor(int color) async {
    await _channel.invokeMethod("SetNotificationColor", color);
  }

  ///Sets [mediaQueue]
  static void setQueue(List mQueue) async {
    mediaQueue.clear();
    mediaQueue.addAll(mQueue);
    currentItem = 0 ;
    await _channel.invokeMethod("SetQueue", mediaQueue);
  }

  ///Sets [repeatMode]
  static void setRepeatMode(RepeatMode mode) async {
    repeatMode = mode;
    await _channel.invokeMethod("SetRepeatMode", mode.index - 1);
  }

  ///Sets [shuffleMode]
  static void setShuffleMode(ShuffleMode mode) async {
    shuffleMode = mode;
    await _channel.invokeMethod("SetShuffleMode", mode.index - 1);
  }

  ///Get Current Item index.
  ///It automatically updates the [currentItem].
  static Future<int> getCurrentIndex() async {
    currentItem = await _channel.invokeMethod("GetCurrentIndex");
    return currentItem;
  }

  ///Play item at @param index of [mediaQueue] in MediaPlayer.
  static void play(int index) async {
    currentItem = index;
    await _channel.invokeMethod('Play', index);
  }

  /// Play/Pause action
  static void toggle() async {
    await _channel.invokeMethod('Toggle');
  }

  ///Skip to Next
  static void next() async {
    await _channel.invokeMethod('Next');
  }

  ///Skip to Previous
  static void prev() async {
    await _channel.invokeMethod('Prev');
  }

  ///Stop Media Player
  ///It just stop the service, it does not destroy and release media player.
  static void stop() async {
    await _channel.invokeMethod('Stop');
  }

  ///Seek Current Position to a Particular Position
  static void seekTo(int milliSec) async {
    await _channel.invokeMethod("SeekTo", milliSec);
  }

  /// Callback for [_eventChannel] stream
  static _onEvent(dynamic event) async {
    await getCurrentIndex();
    List<String> data = event.toString().split(":");
    switch (data[0]) {
      case "UpdateProgress":
        assert(data.length == 5);
        int pos = int.parse(data[1] ?? "0");
        int dur = int.parse(data[2] ?? "0");
        int buffer = int.parse(data[3] ?? "0");
        int state = int.parse(data[4] ?? "0");
        MediaPlayerState mediaPlayerState =
            MediaPlayerState(pos, dur, buffer, state);
        _mediaPlayerStateController.add(mediaPlayerState);
        break;
      default:
        print("No callback for event ${data[0]}");
    }
  }

  /// cancels event channel subscription only.
  /// caution : it does not destroys the media player
  static cancelStreams() {
    _subscription?.cancel();
    _mediaPlayerStateController?.close();
  }

  ///return a stream of type [MediaPlayerState]
  ///gets triggered when a listener is active
  ///and [_eventChannel] receive events.
  static Stream<MediaPlayerState> get onStateChange =>
      _mediaPlayerStateController.stream;
}
