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
    BackgroundMediaPlayer.instance
        .init()
        .then((value) => BackgroundMediaPlayer.instance.setQueue(mediaQueue));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: StreamBuilder<PlaybackState>(
            stream: BackgroundMediaPlayer.instance.onPlaybackStateChange,
            builder: (context, snapshot) {
              var _playbackState = snapshot?.data;
              return Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  RaisedButton(
                    onPressed: (_playbackState == PlaybackState.STATE_PLAYING)
                        ? null
                        : () {
                            BackgroundMediaPlayer.instance.play(0);
                          },
                    child: Text("Play"),
                  ),
                  RaisedButton(
                    onPressed: (_playbackState == PlaybackState.STATE_PLAYING)
                        ? () {
                            BackgroundMediaPlayer.instance.toggle();
                          }
                        : null,
                    child: Text("Pause"),
                  ),
                  RaisedButton(
                    onPressed: (_playbackState != PlaybackState.STATE_STOPPED)
                        ? () {
                            BackgroundMediaPlayer.instance.next();
                          }
                        : null,
                    child: Text("Next"),
                  ),
                  RaisedButton(
                    onPressed: (_playbackState != PlaybackState.STATE_STOPPED)
                        ? () {
                            BackgroundMediaPlayer.instance.prev();
                          }
                        : null,
                    child: Text("Prev"),
                  ),
                  RaisedButton(
                    onPressed: (_playbackState == PlaybackState.STATE_PLAYING)
                        ? () {
                            BackgroundMediaPlayer.instance.stop();
                            // final List mediaQueue = [
                            //   {
                            //     "artist": "Baby Yoda",
                            //     "album": "My Playlist",
                            //     "albumArt":
                            //         "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
                            //     "title": "First Song",
                            //     "source":
                            //         "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            //   },
                            //   {
                            //     "artist": "Baby Yoda",
                            //     "album": "My Playlist",
                            //     "albumArt":
                            //         "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
                            //     "title": "Second Song",
                            //     "source":
                            //         "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            //   },
                            //   {
                            //     "artist": "Baby Yoda",
                            //     "album": "My Playlist",
                            //     "albumArt":
                            //         "https://media.wnyc.org/i/1400/1400/l/80/1/ScienceFriday_WNYCStudios_1400.jpg",
                            //     "title": "Third Song",
                            //     "source":
                            //         "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            //   }
                            // ];
                          }
                        : null,
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

                      BackgroundMediaPlayer.instance.setQueue(mediaQueue);
                    },
                    child: Text("SetQueue"),
                  ),
                  StreamBuilder<PlaybackState>(
                      stream:
                          BackgroundMediaPlayer.instance.onPlaybackStateChange,
                      builder: (context, playBackSnap) {
                        return StreamBuilder<double>(
                            stream:
                                BackgroundMediaPlayer.instance.onDurationUpdate,
                            builder: (context, durationSnap) {
                              return StreamBuilder<double>(
                                  stream: BackgroundMediaPlayer
                                      .instance.onPositionUpdate,
                                  builder: (context, positionSnap) {
                                    return StreamBuilder<double>(
                                        stream: BackgroundMediaPlayer
                                            .instance.onBufferUpdate,
                                        builder: (context, bufferSnap) {
                                          return Text(
                                              "PlayBackState: ${playBackSnap.data}\nDuration: ${durationSnap.data}\nPosition: ${positionSnap.data}\nBuffer Percent: ${bufferSnap.data}",textAlign: TextAlign.center,);
                                        });
                                  });
                            });
                      })
                ],
              );
            }),
      ),
    );
  }

  @override
  void dispose() {
    /// Canceling all streams
    BackgroundMediaPlayer.instance.dispose();
    super.dispose();
  }
}
