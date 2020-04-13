package com.akaalapps.background_media_player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;


import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import static android.content.Context.NOTIFICATION_SERVICE;

class MediaNotification {

    final private PlaybackStateCompat playbackStateCompat;
    final private Context context;
    final private MediaSessionCompat.Token token;
    final private NotificationManager notificationManager;

    MediaNotification(PlaybackStateCompat playbackStateCompat, Context context, MediaSessionCompat.Token token) {
        this.playbackStateCompat = playbackStateCompat;
        this.context = context;
        this.token = token;
        notificationManager = ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE));
    }

    Notification generateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String CHANNEL = "Media Notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL, CHANNEL, importance);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder
                notificationBuilder = new NotificationCompat.Builder(context, "Media_Notification")
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_stat_music_note)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_music_note))// TODO: Add Bitmap from metadata
                .setColor(ContextCompat.getColor(context, R.color.color))
                .setContentTitle("Song")
                .setContentText("Author");

        if (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING) {
            notificationBuilder
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_previous_black_36, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_pause_black_36, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_next_black_36, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)).build())
                    .setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                            .setShowCancelButton(true)
                            .setMediaSession(token)
                            .setShowActionsInCompactView(0, 1, 2)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
        } else if (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PAUSED) {
            notificationBuilder
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_previous_black_36, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_play_arrow_black_36, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.baseline_skip_next_black_36, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)).build())
                    .setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                            .setShowCancelButton(true)
                            .setMediaSession(token)
                            .setShowActionsInCompactView(0, 1, 2)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
        } else {
            notificationBuilder.setProgress(100, 50, true);
        }
        return notificationBuilder.build();

    }
}
