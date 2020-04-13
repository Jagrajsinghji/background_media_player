package com.akaalapps.background_media_player;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.IOException;

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

    final private Handler mHandler = new Handler(Looper.getMainLooper());
    final private NoisyReceiver noisyReceiver = new NoisyReceiver();
    final private Context context;
    final private NotificationManager notificationManager;
    final private MediaSessionCompat mediaSessionCompat;
    final private MediaPlaybackService mediaPlaybackService;

    private AudioFocusRequest audioFocusRequest;
    private PlaybackStateCompat playbackStateCompat;
    private AudioManager audioManager;
    private MediaPlayer mMediaPlayer;

    MediaSessionCallback(Context context, MediaSessionCompat mediaSessionCompat, MediaPlaybackService mediaPlaybackService) {
        this.context = context;
        this.mediaSessionCompat = mediaSessionCompat;
        this.mediaPlaybackService = mediaPlaybackService;
        notificationManager = ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE));
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mMediaPlayer = createMediaPlayer();
    }

    private void updateProgress() {
        mHandler.postDelayed(() -> {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                long duration = mMediaPlayer.getDuration();
                int progress = mMediaPlayer.getCurrentPosition();
                playbackStateCompat = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, progress, 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE)
                        .build();
                mediaSessionCompat.setPlaybackState(playbackStateCompat);
                mediaSessionCompat.setMetadata(new MediaMetadataCompat.Builder().putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration).build());
                updateProgress();
            }
        }, 1000L);

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
        playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .build();
        this.mediaSessionCompat.setPlaybackState(playbackStateCompat);
        Log.i("MediaSessionCallback", "Media Player Created");
        return mMediaPlayer;
    }

    void destroy() {
        mMediaPlayer.release();
        mMediaPlayer = null;
        Log.i("MediaSessionCallback", "Media Player Released");
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
                if (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PAUSED) {
                    onPlay();
                }
                ///start playing again if paused at full volume
                /// if stopped and media player not null then
                /// set data source and prepare media player.
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                onPause();
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
        super.onPlay();
        mMediaPlayer = mMediaPlayer == null ? createMediaPlayer() : mMediaPlayer;
        int result;
        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioAttributes.USAGE_MEDIA)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }


        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            if (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PAUSED) {
                mMediaPlayer.start();
                mediaSessionCompat.setActive(true);
                playbackStateCompat = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE)
                        .build();
                mediaSessionCompat.setPlaybackState(playbackStateCompat);


                notification = new MediaNotification(playbackStateCompat, context, mediaSessionCompat.getSessionToken()).generateNotification();
                try {
                    context.registerReceiver(noisyReceiver
                            , new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                } catch (IllegalStateException err) {
                    Log.e("NoisyReceiver", err.getMessage());
                }


                updateProgress();

            } else {
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");//TODO:add data source, setMetadata()
                } catch (IOException e) {
                    Log.e("MediaSessionCallback", e.getMessage());
                }
                mMediaPlayer.prepareAsync();
                playbackStateCompat = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_BUFFERING, 0, 1.0f)
                        .build();
                mediaSessionCompat.setPlaybackState(playbackStateCompat);
                notification = new MediaNotification(playbackStateCompat, context, mediaSessionCompat.getSessionToken()).generateNotification();
            }

            mediaPlaybackService.startForeground(1, notification);
        }
        Log.i("MediaSessionCallback", "Playing");
    }

    @Override
    public void onPause() {
        super.onPause();
        mMediaPlayer.pause();
        playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PLAY)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        Notification notification = new MediaNotification(playbackStateCompat, context, mediaSessionCompat.getSessionToken()).generateNotification();
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
        Log.i("MediaSessionCallback", "Paused");
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        ///TODO:maybe change metadata
        onPlay();
        Log.i("MediaSessionCallback", "Next");
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
        playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        onPlay();
        Log.i("MediaSessionCallback", "Previous");
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaPlayer.stop();
        playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        mediaSessionCompat.setActive(false);
        mediaPlaybackService.stopForeground(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null)
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        Log.i("MediaSessionCallback", "Stopped");
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        mediaSessionCompat.setActive(true);
        playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);

        Notification notification = new MediaNotification(playbackStateCompat, context, mediaSessionCompat.getSessionToken()).generateNotification();
        notificationManager.notify(1, notification);
        updateProgress();
        try {
            context.registerReceiver(noisyReceiver
                    , new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        } catch (IllegalStateException err) {
            Log.e("NoisyReceiver", err.getMessage());
        }
        Log.i("MediaSessionCallback", "Prepared");
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        onPlay();
        //TODO: check for shuffle and repeat or set it to mediaSession so that it can handle itself
        Log.i("MediaSessionCallback", "Completed");
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //TODO: send bufer data to flutter
        Log.e("BUFFER", String.valueOf(percent));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.i("MediaSessionCallback", "Error in MediaPlayer, Code:" + what);
        mediaPlaybackService.stopForeground(true);
        mMediaPlayer.reset();
        playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_ERROR, 0, 1.0f)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        mediaSessionCompat.setActive(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null)
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        return false;
    }


    private class NoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                onPause();
            }
        }
    }
}
