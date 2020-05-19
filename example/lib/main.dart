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
    {
      "artist": "Baby Yoda",
      "album": "My Playlist",
      "albumArt":
          "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
      "title": "Second Song",
      "source": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    },
    {
      "artist": "Baby Yoda",
      "album": "My Playlist",
      "albumArt":
          "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
      "title": "Third Song",
      "source": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    }
  ];

  @override
  void initState() {
    super.initState();

    /// initializing media player
    BackgroundMediaPlayer.init();
    BackgroundMediaPlayer.setQueue(mediaQueue);
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
                BackgroundMediaPlayer.setNotificationColor(Colors.pink.value);
              },
              child: Text("Change Buffering Notification Color To Pink"),
            ),
            StreamBuilder<PlaybackState>(
                stream: BackgroundMediaPlayer.onPlaybackStateChange,
                builder: (context, snapshot) {
                  PlaybackState state = snapshot.data;
                  return StreamBuilder<int>(
                      stream: BackgroundMediaPlayer.onBufferUpdate,
                      builder: (context, snapshot1) {
                        int percent = snapshot1.data;
                        return StreamBuilder<int>(
                            stream: BackgroundMediaPlayer.onDurationUpdate,
                            builder: (context, snapshot2) {
                              int duration = snapshot2.data;
                              return StreamBuilder<int>(
                                  stream:
                                      BackgroundMediaPlayer.onPositionUpdate,
                                  builder: (context, snapshot3) {
                                    int position = snapshot3.data;
                                    return Text(
                                        "Position : $position\nDuration : $duration\nBufferPercent : $percent\nState : $state\nCurent Index : ${BackgroundMediaPlayer.currentItem}\n"
                                        "pos percent ${((position ?? 0) / (duration ?? 1) * 100)}");
                                  });
                            });
                      });
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
