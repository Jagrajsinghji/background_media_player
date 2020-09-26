package com.akaalapps.background_media_player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
    static Integer notificationColor = 0xff33b5e5;
    private BroadcastReceiver eventReceiver;
    private MethodChannel _channel;
    private EventChannel _eventC;


    @Override
    public void onAttachedToEngine(@NonNull final FlutterPluginBinding flutterPluginBinding) {
        _channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "background_media_player_method");
        _channel.setMethodCallHandler((call, result) -> {
            Intent intent = new Intent(flutterPluginBinding.getApplicationContext(), MediaPlaybackService.class);
            Context context = flutterPluginBinding.getApplicationContext();
            switch (call.method) {
                case "SetNotificationColor":
                    Object color = call.arguments;
                    if (color != null) {
                        if (color instanceof Integer)
                            notificationColor = (Integer) color;
                        else
                            notificationColor = (int) ((Long) call.arguments).longValue();
                        result.success(true);
                    } else
                        result.error("Color must not be null", "Null Color Value", "Value of color is null");
                    break;
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
                    MediaSessionCallback.currentItem = 0;
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
                    intent.setAction(MediaSessionCallback.ACTION_PLAY);
                    context.startService(intent);
                    result.success("Playing");
                    break;
                case "Toggle":
                    intent.setAction(Intent.ACTION_MEDIA_BUTTON);
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    context.startService(intent);
                    result.success("Toggle");
                    break;
                case "Next":
                    intent.setAction(Intent.ACTION_MEDIA_BUTTON);
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
                    context.startService(intent);
                    result.success("Next");
                    break;
                case "Prev":
                    intent.setAction(Intent.ACTION_MEDIA_BUTTON);
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                    context.startService(intent);
                    result.success("Prev");
                    break;
                case "Stop":
                    intent.setAction(Intent.ACTION_MEDIA_BUTTON);
                    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
                    context.startService(intent);
                    result.success("Stop");
                    break;
                case "SeekTo":
                    if (MediaSessionCallback.mediaSessionCompat != null)
                        MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().seekTo(Long.valueOf((Integer) call.arguments));
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
        if (_channel != null)
            _channel.setMethodCallHandler(null);
        _channel = null;
        if (_eventC != null)
            _eventC.setStreamHandler(null);
        _eventC = null;
        if (eventReceiver != null)
            binding.getApplicationContext().unregisterReceiver(eventReceiver);

    }

    private BroadcastReceiver createEventReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, android.content.Intent intent) {
                events.success(intent.getStringExtra("name"));
            }
        };
    }


}
