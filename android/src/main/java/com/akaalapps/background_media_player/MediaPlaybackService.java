package com.akaalapps.background_media_player;

import android.app.PendingIntent;
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
        Intent mediaButtonInt = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonInt.setClass(getApplicationContext(), MediaButtonReceiver.class);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonInt, 0);
        mediaSessionCompat.setMediaButtonReceiver(mbrIntent);
        mediaSessionCompat.setCallback(mediaSessionCallback);
        Log.e("MediaPlaybackService", "Service Created.");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("INTENTS", intent.getAction());
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
//        mediaSessionCallback.handleCustomIntents(intent);
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
        Log.e("MediaPlaybackService", "Service Destroyed.");
    }


}

