enum RepeatMode {
  REPEAT_MODE_INVALID,
  REPEAT_MODE_NONE,
  REPEAT_MODE_ONE,
  REPEAT_MODE_ALL,
  REPEAT_MODE_GROUP
}

enum ShuffleMode{
  SHUFFLE_MODE_INVALID,
  SHUFFLE_MODE_NONE,
  SHUFFLE_MODE_ALL,
  SHUFFLE_MODE_GROUP

}

RepeatMode getRepeatModeFromInt(int mode){
  mode++;
  return RepeatMode.values.elementAt(mode);
}

ShuffleMode getShuffleModeFromInt(int mode){
  mode++;
  return ShuffleMode.values.elementAt(mode);
}
