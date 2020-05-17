package com.akaalapps.background_media_player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;


import static android.content.Context.NOTIFICATION_SERVICE;

class MediaNotification {

    final private PlaybackStateCompat playbackStateCompat;
    final private Context context;
    final private MediaSessionCompat.Token token;
    final private NotificationManager notificationManager;
    final private MediaMetadataCompat metadataCompat;


    MediaNotification(Context context, MediaSessionCompat mediaSessionCompat) {
        this.context = context;
        notificationManager = ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE));
        this.token = mediaSessionCompat.getSessionToken();
        this.playbackStateCompat = mediaSessionCompat.getController().getPlaybackState();
        this.metadataCompat = mediaSessionCompat.getController().getMetadata();


    }

    Notification generateNotification() {
        String CHANNEL = "Media Notifications";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL, CHANNEL, importance);
                notificationManager.createNotificationChannel(channel);
            }
        }

        MediaDescriptionCompat description = metadataCompat.getDescription();

        Bitmap art = description.getIconBitmap();
        if(art ==null)
                art = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_music_note);

        NotificationCompat.Builder
                notificationBuilder = new NotificationCompat.Builder(context, CHANNEL)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_stat_music_note)
                .setColor(BackgroundMediaPlayerPlugin.notificationColor)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setContentIntent(PendingIntent.getActivity(context, 0, context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP));
        if (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING) {
            Log.e("TAG", "generateNotification: state playing");
            notificationBuilder
                    .setLargeIcon(art)
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_previous_black_36, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_pause_black_36, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_next_black_36, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)).build())
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowCancelButton(true)
                            .setMediaSession(token)
                            .setShowActionsInCompactView(0, 1, 2)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
        } else if (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PAUSED) {
            Log.e("TAG", "generateNotification: state paused");
            notificationBuilder
                    .setLargeIcon(art)
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_previous_black_36, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_play_arrow_black_36, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_next_black_36, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)).build())
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowCancelButton(true)
                            .setMediaSession(token)
                            .setShowActionsInCompactView(0, 1, 2)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
        } else {
            notificationBuilder
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_previous_black_36, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)).build())
//                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_play_arrow_black_36, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_next_black_36, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)).build())
//                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
//                            .setShowCancelButton(true)
//                            .setMediaSession(token)
//                            .setShowActionsInCompactView(0, 1)
//                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
           .setProgress(100, 50, true);
        }
        return notificationBuilder.build();

    }



}
