## 0.1.4
    Added method handler to a seperate class
## 0.1.3
    Resolved Error:  ''Context.startForegroundService() did not then call Service.startForeground()"
    **Breaking Changes**:
	Removed Previously used streams.
	Added Streams:
	     	onPrepared
		onPlay
		onPause
		onStopped
		onSeek
		onComplete
		onPlaybackStateChange
		onError
		onDuration
		onPositionChange
		onSkipToNext
		onSkipToPrevious
		onBufferUpdate
	added mediaQueue, optional parameter in play method, if not null then first it will set this queue and then play the given item at index.	
		
## 0.1.2
    Setting List of Map as mediaQueue.
## 0.1.1
    Updated Readme, pubspec.yaml.
## 0.1.0
    Initial Release

