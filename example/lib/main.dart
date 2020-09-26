import 'package:background_media_player/background_media_player.dart';
import 'package:flutter/material.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  /// These Five Fields are mandatory for media payer to work.
  /// You can add additional fields for your app comfort but media player will ignore them all.
  final List mediaQueue = [
    {
      "artist": "Baby Yoda",
      "album": "My Playlist",
      "albumArt":
          "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
      "title": "First Song",
      "source": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    },
  ];

  @override
  void initState() {
    super.initState();

    /// initializing media player
    BackgroundMediaPlayer.init()
        .then((value) => BackgroundMediaPlayer.setQueue(mediaQueue));
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
            RaisedButton(
              onPressed: () {
                BackgroundMediaPlayer.play(0);
              },
              child: Text("Play"),
            ),
            RaisedButton(
              onPressed: () {
                BackgroundMediaPlayer.toggle();
              },
              child: Text("Pause"),
            ),
            RaisedButton(
              onPressed: () {
                BackgroundMediaPlayer.next();
              },
              child: Text("Next"),
            ),
            RaisedButton(
              onPressed: () {
                BackgroundMediaPlayer.prev();
              },
              child: Text("Prev"),
            ),
            RaisedButton(
              onPressed: () {
                BackgroundMediaPlayer.stop();
              },
              child: Text("Stop"),
            ),
            RaisedButton(
              onPressed: () {
                final List mediaQueue = [
                  {
                    "artist": "Baby Yoda",
                    "album": "My Playlist",
                    "albumArt":
                        "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
                    "title": "First Song",
                    "source":
                        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                  },
                  {
                    "artist": "Baby Yoda",
                    "album": "My Playlist",
                    "albumArt":
                        "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
                    "title": "Second Song",
                    "source":
                        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                  },
                  {
                    "artist": "Baby Yoda",
                    "album": "My Playlist",
                    "albumArt":
                        "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
                    "title": "Third Song",
                    "source":
                        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                  }
                ];

                BackgroundMediaPlayer.setQueue(mediaQueue);
              },
              child: Text("SetQueue"),
            ),
            RaisedButton(
              onPressed: () {
                BackgroundMediaPlayer.setNotificationColor(Colors.pink.value);
              },
              child: Text("Change Buffering Notification Color To Pink"),
            ),
            StreamBuilder<MediaPlayerState>(
                stream: BackgroundMediaPlayer.onStateChange,
                builder: (context, snapshot) {
                  PlaybackState state =
                      snapshot.data?.playerState ?? PlaybackState.STATE_NONE;
                  Duration position = snapshot.data?.position ?? Duration();
                  Duration duration = snapshot.data?.duration ?? Duration();
                  int percent = snapshot.data?.bufferPercent ?? Duration();
                  return Text(
                      "Position : $position\nDuration : $duration\nBufferPercent : $percent\nState : $state\nCurent Index : ${BackgroundMediaPlayer.currentItem}\n"
                      "pos percent ${((position.inMilliseconds ?? 0) / (duration.inMilliseconds ?? 1) * 100)}");
                })
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    /// Canceling all streams
    BackgroundMediaPlayer.cancelStreams();
    super.dispose();
  }
}
