package com.example.tvremotebridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.media.AudioManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
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

    private void clickFocusedNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focused == null) {
                focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            }
            if (focused != null) {
                focused.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                focused.recycle();
            } else {
                tap(683, 384);
            }
            root.recycle();
        }
    }

    private void navigateFocus(int direction) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focused == null) {
                focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            }
            if (focused != null) {
                AccessibilityNodeInfo next = focused.focusSearch(direction);
                if (next != null) {
                    next.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                    next.recycle();
                }
                focused.recycle();
            }
            root.recycle();
        }
    }

    private void tap(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 50);
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
            String uri = session.getUri().replace("/", "").toLowerCase();
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
                    clickFocusedNode();
                    break;
                case "up":
                    navigateFocus(AccessibilityNodeInfo.FOCUS_BACKWARD);
                    break;
                case "down":
                    navigateFocus(AccessibilityNodeInfo.FOCUS_FORWARD);
                    break;
                default:
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Unknown");
            }

            Response res = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK");
            res.addHeader("Access-Control-Allow-Origin", "*");
            return res;
        }
    }
}