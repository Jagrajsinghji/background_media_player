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
import io.flutter.plugin.common.MethodCall;
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
        _channel.setMethodCallHandler((call, result) -> _mHandler(call, result, flutterPluginBinding));
        final EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "background_media_player_event");
        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                eventReceiver = createEventReceiver(events);
                flutterPluginBinding.getApplicationContext().registerReceiver(eventReceiver, new android.content.IntentFilter(MediaSessionCallback.SERVICE_EVENT));//TODO:Service Event

            }

            @Override
            public void onCancel(Object arguments) {
                if (eventReceiver != null)
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

    }

    void _mHandler(MethodCall call, MethodChannel.Result result, FlutterPluginBinding flutterPluginBinding) {
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
                intent.setAction(Intent.ACTION_MEDIA_BUTTON);
                intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
                context.startService(intent);
                result.success("Playing");
                break;
            case "Toggle":
                int state = (int) call.arguments;
                if (MediaSessionCallback.mediaSessionCompat != null) {
                    if (state == 0)
                        MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().play();
                    else
                        MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().pause();
                    result.success("Toogle");
                } else
                    result.error("MediaSessionCallback is Null", "MediaSessionCallback is Null so can not play/pause the media.", "");
                break;
            case "Next":
                if (MediaSessionCallback.mediaSessionCompat != null) {
                    MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().skipToNext();
                    result.success("Next");
                } else
                    result.error("MediaSessionCallback is Null", "MediaSessionCallback is Null so can not skip the media.", "");
                break;
            case "Prev":
                if (MediaSessionCallback.mediaSessionCompat != null) {
                    MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().skipToPrevious();
                    result.success("Prev");
                } else
                    result.error("MediaSessionCallback is Null", "MediaSessionCallback is Null so can not skip the media.", "");
                break;
            case "Stop":
                if (MediaSessionCallback.mediaSessionCompat != null) {
                    MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().stop();
                    result.success("Stop");
                } else
                    result.error("MediaSessionCallback is Null", "MediaSessionCallback is Null so can not stop the media.", "");
                break;
            case "SeekTo":
                if (MediaSessionCallback.mediaSessionCompat != null) {
                    MediaSessionCallback.mediaSessionCompat.getController().getTransportControls().seekTo(Long.valueOf((Integer) call.arguments));
                    result.success("Seek");
                } else
                    result.error("MediaSessionCallback is Null", "MediaSessionCallback is Null so can not seek the media.", "");
                break;
            default:
                result.notImplemented();
        }

    }


    private BroadcastReceiver createEventReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, android.content.Intent intent) {
                if (intent.getStringExtra("name") != null) {
                    Map data = new HashMap<String, Map>();
                    String key = intent.getStringExtra("name");
                    HashMap mapData = (HashMap) intent.getSerializableExtra("data");
                    data.put(key, mapData);
                    events.success(data);
                }
            }
        };
    }
}
