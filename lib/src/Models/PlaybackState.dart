enum PlaybackState {
  STATE_NONE, //0
  STATE_STOPPED, //1
  STATE_PAUSED, //2
  STATE_PLAYING, //3
  STATE_BUFFERING, //6
  STATE_ERROR, //7
  STATE_SKIPPING_TO_PREVIOUS, //9
  STATE_SKIPPING_TO_NEXT //10
}

PlaybackState getPlaybackStateFromInt(int state) {
  switch (state) {
    case 8:
    case 6:
      state = 4;
      break;
    case 7:
      state = 5;
      break;
    case 9:
      state = 6;
      break;
    case 10:
      state = 7;
      break;
  }
  return PlaybackState.values.elementAt(state);
}
