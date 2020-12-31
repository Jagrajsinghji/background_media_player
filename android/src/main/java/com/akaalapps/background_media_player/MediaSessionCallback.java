package com.akaalapps.background_media_player;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static android.content.Context.NOTIFICATION_SERVICE;


/**
 * Media Player States enum
 * MEDIA_PLAYER_STATE_ERROR        = 0,        //   0
 * MEDIA_PLAYER_IDLE               = 1 << 0,   //   1
 * MEDIA_PLAYER_INITIALIZED        = 1 << 1,   //   2
 * MEDIA_PLAYER_PREPARING          = 1 << 2,   //   4
 * MEDIA_PLAYER_PREPARED           = 1 << 3,   //   8
 * MEDIA_PLAYER_STARTED            = 1 << 4,   //  16
 * MEDIA_PLAYER_PAUSED             = 1 << 5,   //  32
 * MEDIA_PLAYER_STOPPED            = 1 << 6,   //  64
 * MEDIA_PLAYER_PLAYBACK_COMPLETE  = 1 << 7    // 128
 */

class MediaSessionCallback extends MediaSessionCompat.Callback implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {

    static final String SERVICE_EVENT = "Background_Media_Player__Service_Event";
    static final private MediaMetadataCompat.Builder mBuilder = new MediaMetadataCompat.Builder();
    static final private PlaybackStateCompat.Builder plBuilder = new PlaybackStateCompat.Builder();
    static List<Map<String, String>> mediaQueue = new ArrayList<>();
    static int currentItem = 0;
    static MediaSessionCompat mediaSessionCompat;
    static private AudioFocusRequest audioFocusRequest;
    static private PlaybackStateCompat playbackStateCompat;
    static private AudioManager audioManager;
    static private NotificationManager notificationManager;
    static private MediaPlayer mMediaPlayer;
    static private MediaMetadataCompat metadataCompat;
    static private MediaPlaybackService mediaPlaybackService;
    private static int buffer = 0, duration = 0;
    final private String TAG = "MediaSessionCallback";
    final private Context context;
    final private Handler mHandler = new Handler(Looper.getMainLooper());
    final private Handler notificationArtHandler;
    final private NoisyReceiver noisyReceiver = new NoisyReceiver();


    MediaSessionCallback(Context appContext, MediaSessionCompat mediasessionCompat, MediaPlaybackService playbackService) {
        context = appContext;
        mediaSessionCompat = mediasessionCompat;
        mediaPlaybackService = playbackService;
        notificationManager = ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE));
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMediaPlayer = createMediaPlayer();
        HandlerThread bgThread = new HandlerThread("BitmapLoaderThread");
        bgThread.start();
        notificationArtHandler = new Handler(bgThread.getLooper());
    }


    private void sendEvent(String name, HashMap data) {
        Intent intent = new Intent(SERVICE_EVENT);
        intent.putExtra("name", name);
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }

    private void updateProgress(int lastPos) {
        mHandler.postDelayed(() -> {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                int currentPosition = 0;
                try {
                    currentPosition = mMediaPlayer.getCurrentPosition();
                    HashMap data = new HashMap();
                    data.put("position", currentPosition);
                    sendEvent("onPositionUpdate", data);
                    data.clear();
                    data.put("duration", duration);
                    sendEvent("onDurationUpdate", data);

//                    Log.e(TAG, "updateProgress: curr "+currentPosition+" : last "+lastPos );

                    if (currentPosition - lastPos <= 500) {
                        ///Buffering
                        if (mediaSessionCompat.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_BUFFERING) {
                            playbackStateCompat = plBuilder
                                    .setState(PlaybackStateCompat.STATE_BUFFERING, currentPosition, 1.0f)
                                    .setActions(PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                                    .build();
                            mediaSessionCompat.setPlaybackState(playbackStateCompat);
                            setMetaData(duration, false);
                            Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
                            notificationManager.notify(1, notification);
                            HashMap _data = new HashMap();
                            _data.put("state", 6);
                            sendEvent("onPlaybackStateChange", _data);

                        }
                    } else {
                        boolean updateNotification = mediaSessionCompat.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING;
                        playbackStateCompat = plBuilder
                                .setState(PlaybackStateCompat.STATE_PLAYING, currentPosition, 1.0f)
                                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO)
                                .build();
                        mediaSessionCompat.setPlaybackState(playbackStateCompat);
                        setMetaData(duration, !updateNotification);
                        if (updateNotification) {
                            Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
                            notificationManager.notify(1, notification);
                        }
                        HashMap _data = new HashMap();
                        _data.put("state", 3);
                        sendEvent("onPlaybackStateChange", _data);
                    }
                } catch (Exception err) {
                    Log.e(TAG, "updateProgress: ", err);
                }
                updateProgress(currentPosition);
            }
        }, 1000L);

    }

    private void setMetaData(long duration, boolean inLoop) {
        MediaItem item = MediaItem.fromMap(mediaQueue.get(currentItem));
        metadataCompat = mBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.album)
                .build();
        mediaSessionCompat.setMetadata(metadataCompat);
        if (!inLoop)
            notificationArtHandler.postDelayed(() -> {
                Bitmap bitmap = getBitmap(item.albumArt);
                metadataCompat = mBuilder
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap).build();
                mediaSessionCompat.setMetadata(metadataCompat);
                Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
                notificationManager.notify(1, notification);
            }, 0);

    }

    private Bitmap getBitmap(String uri) {
        try {
            return Glide.with(context)
                    .asBitmap()
                    .override(300)
                    .load(uri)
                    .submit()
                    .get();


        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    private MediaPlayer createMediaPlayer() {
        MediaPlayer mMediaPlayer = new MediaPlayer();
        // Make sure the media player will acquire a wake-lock while
        // playing. If we don't do that, the CPU might go to sleep while the
        // song is playing, causing playback to stop.
        mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
        } else
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        return mMediaPlayer;
    }

    void release() {
        notificationArtHandler.getLooper().quit();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        Log.e(TAG, "Media Player Released");
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.e("OnFocusGain", "" + focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                /// on focus gain setMediaSession to true
                /// because we want to listen [MediaButtonReceiver] calls
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mediaSessionCompat.setActive(true);
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                }
                if (mediaSessionCompat.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED) {
                    onPlay();
                }
                ///start playing again if paused at full volume
                /// if stopped and media player not null then
                /// set data source and prepare media player.
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mMediaPlayer != null && mediaSessionCompat.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING && mMediaPlayer.isPlaying()) {
                    onPause();
                }
                ///pause playing
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                ///Lower volume or pause playing
                if (mMediaPlayer != null && mMediaPlayer.isPlaying())
                    mMediaPlayer.setVolume(0.5f, 0.5f);
                break;

        }
    }

    @Override
    public void onPlay() {
        Log.e(TAG, "Playing");
        super.onPlay();
        sendEvent("onPlay", null);
        mMediaPlayer = mMediaPlayer == null ? createMediaPlayer() : mMediaPlayer;
        int result;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioAttributes.USAGE_MEDIA)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (mediaSessionCompat.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED) {
                mMediaPlayer.start();
                HashMap _data = new HashMap();
                _data.put("state", 3);
                sendEvent("onPlaybackStateChange", _data);
                mediaSessionCompat.setActive(true);
                playbackStateCompat = plBuilder
                        .setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO)
                        .build();
                mediaSessionCompat.setPlaybackState(playbackStateCompat);
                Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
                mediaPlaybackService.startForeground(1, notification);
                updateProgress(mMediaPlayer.getCurrentPosition());
                try {
                    context.registerReceiver(noisyReceiver
                            , new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                } catch (IllegalStateException err) {
                    Log.e("NoisyReceiver", err.getMessage());
                }

            } else {
                mMediaPlayer.reset();
                try {
                    MediaItem item = com.akaalapps.background_media_player.MediaItem.fromMap(mediaQueue.get(currentItem));
                    setMetaData(0, false);
                    HashMap _data = new HashMap();
                    _data.put("state", 6);
                    sendEvent("onPlaybackStateChange", _data);
                    _data.clear();
                    _data.put("position", 0);
                    sendEvent("onPositionUpdate", _data);
                    _data.clear();
                    _data.put("duration", 0);
                    sendEvent("onDurationUpdate", _data);
                    mMediaPlayer.setDataSource(item.source);
                    mMediaPlayer.prepareAsync();
                    playbackStateCompat = plBuilder
                            .setState(PlaybackStateCompat.STATE_BUFFERING, 0, 1.0f)
                            .setActions(PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                            .build();
                    mediaSessionCompat.setPlaybackState(playbackStateCompat);
                    Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
                    mediaPlaybackService.startForeground(1, notification);

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.e(TAG, "Prepared");
        sendEvent("onPrepared", null);
        mp.start();
        mediaSessionCompat.setActive(true);
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_PLAYING, mp.getCurrentPosition(), 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        duration = mp.getDuration();
        setMetaData(duration, false);
        HashMap _data = new HashMap();
        _data.put("state", 3);
        sendEvent("onPlaybackStateChange", _data);
        _data.clear();
        _data.put("position", mMediaPlayer.getCurrentPosition());
        sendEvent("onPositionUpdate", _data);
        _data.clear();
        _data.put("duration", duration);
        sendEvent("onDurationUpdate", _data);
        Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
        notificationManager.notify(1, notification);
        updateProgress(mMediaPlayer.getCurrentPosition());
        try {
            context.registerReceiver(noisyReceiver
                    , new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        } catch (IllegalStateException err) {
            Log.e("NoisyReceiver", err.getMessage());
        }

    }

    @Override
    public void onPause() {
        Log.e(TAG, "Paused");
        super.onPause();
        sendEvent("onPause", null);
        mMediaPlayer.pause();
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        HashMap _data = new HashMap();
        _data.put("state", 2);
        sendEvent("onPlaybackStateChange", _data);
        _data.clear();
        _data.put("position", mMediaPlayer.getCurrentPosition());
        sendEvent("onPositionUpdate", _data);
        _data.clear();
        _data.put("duration", duration);
        sendEvent("onDurationUpdate", _data);
        Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
        notificationManager.notify(1, notification);
        mediaPlaybackService.stopForeground(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null)
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        try {
            context.unregisterReceiver(noisyReceiver);
        } catch (IllegalStateException err) {
            Log.e("NoisyReceiver", err.getMessage());
        }

    }

    @Override
    public void onSkipToNext() {
        Log.e(TAG, "Next");
        super.onSkipToNext();
        sendEvent("onSkipToNext", null);
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        HashMap _data = new HashMap();
        _data.put("state", 10);
        sendEvent("onPlaybackStateChange", _data);
        _data.clear();
        _data.put("position", 0);
        sendEvent("onPositionUpdate", _data);
        _data.clear();
        _data.put("duration", 0);
        sendEvent("onDurationUpdate", _data);
        _data.clear();
        buffer =0;
        _data.put("percent", buffer);
        sendEvent("onBufferUpdate", _data);
        int repeat = mediaSessionCompat.getController().getRepeatMode();
        if (repeat != PlaybackStateCompat.REPEAT_MODE_ONE) {
            int shuffle = mediaSessionCompat.getController().getShuffleMode();
            if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                currentItem = new Random().nextInt(mediaQueue.size());
            } else {
                currentItem = mediaQueue.size() == currentItem + 1 ? 0 : currentItem + 1;
            }
        }
        onPlay();


    }

    @Override
    public void onSkipToPrevious() {
        Log.e(TAG, "Previous");
        super.onSkipToPrevious();
        sendEvent("onSkipToPrevious", null);
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        HashMap _data = new HashMap();
        _data.put("state", 9);
        sendEvent("onPlaybackStateChange", _data);
        _data.clear();
        _data.put("position", 0);
        sendEvent("onPositionUpdate", _data);
        _data.clear();
        _data.put("duration", 0);
        sendEvent("onDurationUpdate", _data);
        _data.clear();
        buffer =0;
        _data.put("percent", buffer);
        sendEvent("onBufferUpdate", _data);
        int repeat = mediaSessionCompat.getController().getRepeatMode();
        if (repeat != PlaybackStateCompat.REPEAT_MODE_ONE) {
            int shuffle = mediaSessionCompat.getController().getShuffleMode();
            if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                currentItem = new Random().nextInt(mediaQueue.size());
            } else {
                currentItem = currentItem == 0 ? mediaQueue.size() - 1 : currentItem - 1;
            }
        }
        onPlay();

    }

    @Override
    public void onStop() {
        Log.e(TAG, "Stopped");
        super.onStop();
        sendEvent("onStopped", null);
        mMediaPlayer.stop();
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        mediaSessionCompat.setActive(false);
        mediaPlaybackService.stopForeground(true);

        HashMap _data = new HashMap();
        _data.put("state", 1);
        sendEvent("onPlaybackStateChange", _data);
        _data.clear();
        _data.put("position", 0);
        sendEvent("onPositionUpdate", _data);
        _data.clear();
        _data.put("duration", 0);
        sendEvent("onDurationUpdate", _data);
        _data.clear();
        buffer =0;
        _data.put("percent", buffer);
        sendEvent("onBufferUpdate", _data);
        metadataCompat = mBuilder
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null).build();
        mediaSessionCompat.setMetadata(metadataCompat);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null)
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }

    }

    @Override
    public void onSeekTo(long pos) {
        Log.e("SEEK", "" + pos);
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo((int) pos);
            playbackStateCompat = plBuilder
                    .setState(mediaSessionCompat.getController().getPlaybackState().getState(), pos, 1.0f)
                    .setActions(mediaSessionCompat.getController().getPlaybackState().getActions())
                    .build();
            mediaSessionCompat.setPlaybackState(playbackStateCompat);
            super.onSeekTo(pos);
        }
        HashMap data = new HashMap();
        data.put("position", pos);
        sendEvent("onSeek", data);

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        int pos = mp.getCurrentPosition();
        int dur = duration;
        Log.e(TAG, "Completed " + pos + " : " + dur);
        if (dur - pos > 2000) {
            playbackStateCompat = plBuilder
                    .setState(PlaybackStateCompat.STATE_BUFFERING, pos, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                    .build();
            mediaSessionCompat.setPlaybackState(playbackStateCompat);
            HashMap _data = new HashMap();
            _data.put("state", 6);
            sendEvent("onPlaybackStateChange", _data);
            _data.clear();
            _data.put("position", pos);
            sendEvent("onPositionUpdate", _data);
            _data.clear();
            _data.put("duration", dur);
            sendEvent("onDurationUpdate", _data);
            Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
            notificationManager.notify(1, notification);
            return;
        }
        sendEvent("onComplete", null);
        onSkipToNext();

    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        buffer = percent;
        HashMap data = new HashMap();
        data.put("percent", buffer);
        sendEvent("onBufferUpdate", data);

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error in MediaPlayer, Code:" + what);
        HashMap data = new HashMap();
        data.put("ErrorCode", what);
        sendEvent("onError", data);
        mediaPlaybackService.stopSelf();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_ERROR, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        HashMap _data = new HashMap();
        _data.put("state", 7);
        sendEvent("onPlaybackStateChange", _data);
        _data.clear();
        _data.put("position", 0);
        sendEvent("onPositionUpdate", _data);
        _data.clear();
        _data.put("duration", 0);
        sendEvent("onDurationUpdate", _data);
        mediaSessionCompat.setActive(false);
        metadataCompat = mBuilder
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null).build();
        mediaSessionCompat.setMetadata(metadataCompat);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null)
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        return true;
    }


    private class NoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                onPause();
            }
        }
    }
}
