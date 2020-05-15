class MediaItem {
  final String title;
  final String artist;
  final String album;
  final String albumArt;
  final String source;

  MediaItem._(this.title, this.artist, this.album, this.albumArt, this.source);

  Map<String, String> toMap() {
    Map<String, String> item = {
      "title": title,
      "artist": artist,
      "album": album,
      "albumArt": albumArt,
      "source": source
    };
    return item;
  }

  factory MediaItem.fromMap(Map<String, String> item) {
    return new MediaItem._(item["title"], item["artist"], item["album"],
        item["albumArt"], item["source"]);
  }
}
