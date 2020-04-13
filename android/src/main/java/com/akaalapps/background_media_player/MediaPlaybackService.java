package com.akaalapps.background_media_player;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;

public class MediaPlaybackService extends Service {


    private MediaSessionCallback mediaSessionCallback;
    private MediaSessionCompat mediaSessionCompat;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSessionCompat = new MediaSessionCompat(getApplicationContext(), this.getClass().getSimpleName());
        mediaSessionCallback = new MediaSessionCallback(this.getApplicationContext(), mediaSessionCompat, this);
        mediaSessionCompat.setCallback(mediaSessionCallback);
        Log.i("MediaPlaybackService", "Service Created.");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mediaSessionCallback.destroy();
        mediaSessionCompat.release();
        super.onDestroy();
        Log.i("MediaPlaybackService", "Service Destroyed.");
    }


}

