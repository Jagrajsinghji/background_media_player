package com.akaalapps.background_media_player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;



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
             Context context= flutterPluginBinding.getApplicationContext();
            intent.setAction(Intent.ACTION_MEDIA_BUTTON);
            switch (call.method){
                case "Play":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY));
                    context.startService(intent);
                    result.success("Playing");
                    break;
                case "Toggle":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    context.startService(intent);
                    result.success("Paused");
                    break;
                case "Next":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_NEXT));
                    context.startService(intent);
                    result.success("Next");
                    break;
                case "Prev":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                    context.startService(intent);
                    result.success("Prev");
                    break;
                case "Stop":
                    intent.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_STOP));
                    context.startService(intent);
                    result.success("Stop");
                    break;
                case "Seek":

                    default:
                        result.notImplemented();
            }

        });
        final EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "background_media_player_event");
        eventChannel.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                eventReceiver = createEventReceiver(events);
                flutterPluginBinding.getApplicationContext().registerReceiver(eventReceiver, new android.content.IntentFilter("jg"));//TODO:Service Event

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
                events.success(intent.getStringExtra("name"));
            }
        };
    }

}
