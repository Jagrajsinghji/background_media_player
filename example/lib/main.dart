import 'package:background_media_player/background_media_player.dart';
import 'package:flutter/material.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final List<MediaItem> mediaQueue = [
    MediaItem.fromMap({
      "artist": "Sidhu Mosse Wala, Prem Dhillon, Nseeb",
      "album": "This Is Sidhu Moose Wala",
      "albumArt":
          "https://drive.google.com/uc?export=view&id=1lMu-dvmlo-VrhdQ3yEhWTwCaSbKR8cVG",
      "title": "Old Skool",
      "source": "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    }),
    MediaItem.fromMap({
      "artist": "Ragbeer Singh Beer",
      "album": "Bandagi Nama",
      "albumArt":
          "https://drive.google.com/uc?export=view&id=1QxN2YicIYKAjUHckbu967608CDuessRb",
      "title": "Sikh",
      "source":
          "https://firebasestorage.googleapis.com/v0/b/sikh-notes.appspot.com/o/Audio%20Books%2FBandagi%20Nama%40Ragveer%20Singh%20Beer%2F04.%20Sikh.mp3?alt=media&token=7bad8623-dff8-4311-be95-8beca24085c8",
    }),
    MediaItem.fromMap({
      "artist": "Baba Sewa Singh Ji(Sant)",
      "album": "Gur Sagar",
      "albumArt":
          "https://drive.google.com/uc?export=view&id=114Ow3xbIU2s7dzQL-9RxOYtYGcLOIEW3",
      "title": "Gur Sagar",
      "source":
          "https://firebasestorage.googleapis.com/v0/b/sikh-notes.appspot.com/o/Audio%20Books%2FGur%20Sagar%40Baba%20Sewa%20Singh%20Ji%20(Sant)%2F01.Gur%20Sagar%20001-006.mp3?alt=media&token=1848c863-d97e-42c1-867f-74bc45ea41f7",
    })
  ];

  int position, duration, percent;
  PlaybackState state;
  @override
  void initState() {
    super.initState();
    BackgroundMediaPlayer.init();
    BackgroundMediaPlayer.setQueue(mediaQueue);
    BackgroundMediaPlayer.onBufferUpdate((percent, duration) {
      if (mounted)
        setState(() {
          this.percent = percent;
          this.duration = duration;
        });
    });
    BackgroundMediaPlayer.onUpdateProgress((position, duration) {
      if (mounted)
        setState(() {
          this.position = position;
          this.duration = duration;
        });
    });
    BackgroundMediaPlayer.onUpdateState((state) {
      if (mounted)
        setState(() {
          this.state = state;
        });
    });
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
              onPressed: () {},
              child: Text("Send"),
            ),
            Text(
                "Position : $position\nDuration : $duration\nBufferPercent : $percent\nState : $state\n")
          ],
        ),
      ),
    );
  }
}
