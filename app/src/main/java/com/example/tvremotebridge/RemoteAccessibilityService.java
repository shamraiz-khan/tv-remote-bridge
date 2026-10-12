package com.example.tvremotebridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.media.AudioManager;
import android.view.accessibility.AccessibilityEvent;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;

public class RemoteAccessibilityService extends AccessibilityService {

    private RemoteHttpServer server;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        server = new RemoteHttpServer(8080);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }

    private void clickScreen(float x, float y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(clickPath, 0, 50);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        dispatchGesture(builder.build(), null, null);
    }

    private void swipe(float fromX, float fromY, float toX, float toY) {
        Path path = new Path();
        path.moveTo(fromX, fromY);
        path.lineTo(toX, toY);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 150);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        dispatchGesture(builder.build(), null, null);
    }

    private class RemoteHttpServer extends NanoHTTPD {
        public RemoteHttpServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri().replace("/", "");
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            switch (uri) {
                case "home":
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    break;
                case "back":
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    break;
                case "recents":
                    performGlobalAction(GLOBAL_ACTION_RECENTS);
                    break;
                case "power":
                    performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                    break;
                case "volume_up":
                    if (am != null) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    }
                    break;
                case "volume_down":
                    if (am != null) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    }
                    break;
                case "select":
                    clickScreen(683, 384);
                    break;
                case "up":
                    swipe(683, 500, 683, 300);
                    break;
                case "down":
                    swipe(683, 300, 683, 500);
                    break;
                case "left":
                    swipe(500, 384, 300, 384);
                    break;
                case "right":
                    swipe(300, 384, 500, 384);
                    break;
                default:
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Unknown command");
            }

            Response res = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK");
            res.addHeader("Access-Control-Allow-Origin", "*");
            return res;
        }
    }
}