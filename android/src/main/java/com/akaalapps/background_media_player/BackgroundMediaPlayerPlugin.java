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
    private BMethodHandler _mHandler;

    @Override
    public void onAttachedToEngine(@NonNull final FlutterPluginBinding flutterPluginBinding) {
        _channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "background_media_player_method");
        _channel.setMethodCallHandler((call, result) -> _mHandler.methodHandler(call, result, flutterPluginBinding));
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
