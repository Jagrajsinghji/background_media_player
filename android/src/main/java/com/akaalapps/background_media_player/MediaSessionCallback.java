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

    final private String TAG = "MediaSessionCallback";
    final private Context context;
    final private Handler mHandler = new Handler(Looper.getMainLooper());

    final private Handler notificationArtHandler;

    final private NoisyReceiver noisyReceiver = new NoisyReceiver();

    static final private MediaMetadataCompat.Builder mBuilder = new MediaMetadataCompat.Builder();
    static final private PlaybackStateCompat.Builder plBuilder = new PlaybackStateCompat.Builder();

    static private AudioFocusRequest audioFocusRequest;
    static private PlaybackStateCompat playbackStateCompat;
    static private AudioManager audioManager;
    static private NotificationManager notificationManager;
    static private MediaPlayer mMediaPlayer;
    static private MediaMetadataCompat metadataCompat;
    static private MediaPlaybackService mediaPlaybackService;

    static List<Map<String, String>> mediaQueue = new ArrayList<Map<String, String>>();
    static int currentItem = 0;
    static MediaSessionCompat mediaSessionCompat;
    static final String SERVICE_EVENT = "Background_Media_Player__Service_Event";


    MediaSessionCallback(Context appcontext, MediaSessionCompat mediasessionCompat, MediaPlaybackService playbackService) {
        context = appcontext;
        mediaSessionCompat = mediasessionCompat;
        mediaPlaybackService = playbackService;
        notificationManager = ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE));
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMediaPlayer = createMediaPlayer();
        HandlerThread bgThread = new HandlerThread("BitmapLoaderThread");
        bgThread.start();
        notificationArtHandler = new Handler(bgThread.getLooper());
    }


    private void callEvent(String name) {
        Intent intent = new Intent(SERVICE_EVENT);
        intent.putExtra("name",name);
        context.sendBroadcast(intent);
    }

    private void updateProgress(String tag) {
        mHandler.postDelayed(() -> {
//            Log.e("Runnable", tag);
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                try {
                int currentPosition = mMediaPlayer.getCurrentPosition();
                int duration = mMediaPlayer.getDuration();
                playbackStateCompat = plBuilder
                        .setState(PlaybackStateCompat.STATE_PLAYING, currentPosition, 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO)
                        .build();
                mediaSessionCompat.setPlaybackState(playbackStateCompat);
                setMetaData(duration, true);
                callEvent("UpdateProgress:"+currentPosition+":"+duration);
                } catch (Exception err) {
                    Log.e(TAG, "updateProgress: ", err);
                }
                updateProgress(tag);
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
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap).build();
                mediaSessionCompat.setMetadata(metadataCompat);
            }, 0);

    }

    private Bitmap getBitmap(String uri) {
        try {
            return Glide.with(context)
                    .asBitmap()
                    .override(600)
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
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        Log.e(TAG, "Media Player Created");
        return mMediaPlayer;
    }

    void destroy() {
        mediaQueue = null;
        notificationArtHandler.getLooper().quit();
        notificationManager.cancel(1);
        mMediaPlayer.release();
        mMediaPlayer = null;
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
                mediaSessionCompat.setActive(true);
                playbackStateCompat = plBuilder
                        .setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO)
                        .build();
                mediaSessionCompat.setPlaybackState(playbackStateCompat);
                Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
                mediaPlaybackService.startForeground(1, notification);
                updateProgress("ONPLAY");
                try {
                    context.registerReceiver(noisyReceiver
                            , new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                } catch (IllegalStateException err) {
                    Log.e("NoisyReceiver", err.getMessage());
                }


            } else {
                mMediaPlayer.reset();
                try {
                    MediaItem item = MediaItem.fromMap(mediaQueue.get(currentItem));
                    setMetaData(0, false);
                    mMediaPlayer.setDataSource(item.source);
                    mMediaPlayer.prepareAsync();
                    playbackStateCompat = plBuilder
                            .setState(PlaybackStateCompat.STATE_BUFFERING, 0, 1.0f)
                            .build();
                    mediaSessionCompat.setPlaybackState(playbackStateCompat);
                    Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
                    mediaPlaybackService.startForeground(1, notification);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            callEvent("UpdateState:"+ mediaSessionCompat.getController().getPlaybackState().getState());
        }
        Log.e(TAG, "Playing");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        mediaSessionCompat.setActive(true);
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_PLAYING, mp.getCurrentPosition(), 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        setMetaData(mp.getDuration(), false);
        Notification notification = new MediaNotification(context, mediaSessionCompat).generateNotification();
        notificationManager.notify(1, notification);
        updateProgress("ONPrepared");
        try {
            context.registerReceiver(noisyReceiver
                    , new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        } catch (IllegalStateException err) {
            Log.e("NoisyReceiver", err.getMessage());
        }
        callEvent("UpdateState:"+ mediaSessionCompat.getController().getPlaybackState().getState());
        Log.e(TAG, "Prepared");
    }

    @Override
    public void onPause() {
        super.onPause();
        mMediaPlayer.pause();
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
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
        callEvent("UpdateState:"+ mediaSessionCompat.getController().getPlaybackState().getState());
        Log.e(TAG, "Paused");
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        callEvent("UpdateState:"+ mediaSessionCompat.getController().getPlaybackState().getState());
        int repeat = mediaSessionCompat.getController().getRepeatMode();
        if (repeat == PlaybackStateCompat.REPEAT_MODE_ONE) {
            onPlay();
            return;
        } else {
            int shuffle = mediaSessionCompat.getController().getShuffleMode();
            if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                currentItem = new Random().nextInt(mediaQueue.size());
                onPlay();
                return;
            } else {
                currentItem = mediaQueue.size() == currentItem + 1 ? 0 : currentItem + 1;
                onPlay();
            }
        }

        Log.e(TAG, "Next");
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
        playbackStateCompat = plBuilder
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SEEK_TO)
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        callEvent("UpdateState:"+ mediaSessionCompat.getController().getPlaybackState().getState());
        int repeat = mediaSessionCompat.getController().getRepeatMode();
        if (repeat == PlaybackStateCompat.REPEAT_MODE_ONE) {
            onPlay();
            return;
        } else {
            int shuffle = mediaSessionCompat.getController().getShuffleMode();
            if (shuffle == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                currentItem = new Random().nextInt(mediaQueue.size());
                onPlay();
                return;
            } else {
                currentItem = currentItem == 0 ? mediaQueue.size() - 1 : currentItem - 1;
                onPlay();
            }
        }
        Log.e(TAG, "Previous");
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaPlayer.stop();
        playbackStateCompat = plBuilder
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
        callEvent("UpdateState:"+ mediaSessionCompat.getController().getPlaybackState().getState());
        Log.e(TAG, "Stopped");
    }


    @Override
    public void onSeekTo(long pos) {
        Log.e("SEEK", "" + pos);
        mMediaPlayer.seekTo((int) pos);
        playbackStateCompat = plBuilder
                .setState(mediaSessionCompat.getController().getPlaybackState().getState(), pos, 1.0f)
                .setActions(mediaSessionCompat.getController().getPlaybackState().getActions())
                .build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
        super.onSeekTo(pos);
        callEvent("UpdateProgress:"+(int)pos+":"+mMediaPlayer.getDuration());
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        onSkipToNext();
        Log.e(TAG, "Completed");
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        callEvent("BufferUpdate:"+percent+":"+mp.getDuration());
        Log.e("BUFFER", String.valueOf(percent));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error in MediaPlayer, Code:" + what);
        mediaPlaybackService.stopSelf();
        mMediaPlayer.reset();
        playbackStateCompat = plBuilder
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
        callEvent("UpdateState:"+ mediaSessionCompat.getController().getPlaybackState().getState());
        return false;
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
