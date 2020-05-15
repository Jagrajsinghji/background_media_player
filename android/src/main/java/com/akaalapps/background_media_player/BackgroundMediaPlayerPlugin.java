package com.akaalapps.background_media_player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodChannel;


/**
 * BackgroundMediaPlayerPlugin
 */
public class BackgroundMediaPlayerPlugin implements FlutterPlugin {
    private BroadcastReceiver eventReceiver;

    @Override
    public void onAttachedToEngine(@NonNull final FlutterPluginBinding flutterPluginBinding) {
        final MethodChannel channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "background_media_player_method");
        channel.setMethodCallHandler((call, result) -> {
            Intent intent = new Intent(flutterPluginBinding.getApplicationContext(), MediaPlaybackService.class);
            Context context = flutterPluginBinding.getApplicationContext();
            Log.e("Method Call", call.method);
            intent.setAction(Intent.ACTION_MEDIA_BUTTON);
            switch (call.method) {
                case "Init": {
                    Map<Object, Object> data = new HashMap<>();
                    data.put("currentItem", MediaSessionCallback.currentItem);
                    data.put("mediaQueue", MediaSessionCallback.mediaQueue);

                    if (MediaSessionCallback.mediaSessionCompat != null) {
                        data.put("repeatMode", MediaSessionCallback.mediaSessionCompat.getController().getRepeatMode());
                        data.put("shuffleMode", MediaSessionCallback.mediaSessionCompat.getController().getShuffleMode());
                        data.put("playBackState", MediaSessionCallback.mediaSessionCompat.getController().getPlaybackState().getState());
                    }
                    context.startService(intent);
                    result.success(data);
                }
                break;
                case "SetQueue":
                    List<Map<String, String>> args = (List<Map<String, String>>) call.arguments;
                    MediaSessionCallback.mediaQueue.clear();
                    MediaSessionCallback.mediaQueue.addAll(args);
                    result.success(true);
                    break;
                case "SetRepeatMode":
                    if (MediaSessionCallback.mediaSessionCompat != null) {
                        MediaSessionCallback.mediaSessionCompat.setRepeatMode((int) call.arguments);
                        result.success(0);
                    } else
                        result.success(-1);
                    break;
                case "SetShuffleMode":
                    if (MediaSessionCallback.mediaSessionCompat != null) {
                        MediaSessionCallback.mediaSessionCompat.setShuffleMode((int) call.arguments);
                        result.success(0);
                    } else
                        result.success(-1);
                    break;
                case "GetCurrentIndex":
                    result.success(MediaSessionCallback.currentItem);
                    break;
                case "Play":
                    MediaSessionCallback.currentItem = (int) call.arguments;
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
                    context.startService(intent);
                    result.success("Playing");
                    break;
                case "Toggle":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    context.startService(intent);
                    result.success("Paused");
                    break;
                case "Next":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
                    context.startService(intent);
                    result.success("Next");
                    break;
                case "Prev":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                    context.startService(intent);
                    result.success("Prev");
                    break;
                case "Stop":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
                    context.startService(intent);
                    result.success("Stop");
                    break;
                case "SeekTo":
                    if (MediaSessionCallback.mediaSessionCompat != null)
                        MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().seekTo((long) call.arguments);
                    break;

                default:
                    result.notImplemented();
            }

        });


        final EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "background_media_player_event");
        eventChannel.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                eventReceiver = createEventReceiver(events);
                flutterPluginBinding.getApplicationContext().registerReceiver(eventReceiver, new android.content.IntentFilter(MediaSessionCallback.SERVICE_EVENT));//TODO:Service Event

            }

            @Override
            public void onCancel(Object arguments) {
                flutterPluginBinding.getApplicationContext().unregisterReceiver(eventReceiver);
                eventReceiver = null;
            }
        });
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    }

    private BroadcastReceiver createEventReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, android.content.Intent intent) {
                Log.e("event call", intent.getStringExtra("name"));
                events.success(intent.getStringExtra("name"));
            }
        };
    }


}
