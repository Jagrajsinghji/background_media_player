import 'package:background_media_player/background_media_player.dart';

class MediaPlayerState {
  final int _playerState, _buffer, _duration, _position;

  const MediaPlayerState(
      this._position, this._duration, this._buffer, this._playerState);

  int get position => _position;

  int get duration => _duration;

  int get bufferPercent => _buffer;

  PlaybackState get playerState => getPlaybackStateFromInt(_playerState);

  factory MediaPlayerState.initialState() {
    return MediaPlayerState(0, 0, 0, 0);
  }
}
