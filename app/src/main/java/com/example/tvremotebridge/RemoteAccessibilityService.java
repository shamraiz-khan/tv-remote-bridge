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

    // --- NODE SELECTION & CLICKING LOGIC ---

    private void clickAction() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focused == null) {
                focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            }
            if (focused != null) {
                focused.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                focused.recycle();
                root.recycle();
                return;
            }
            root.recycle();
        }
        // Fallback tap agar koi node focus na ho
        dispatchTap(683, 384);
    }

    private void moveFocusNode(boolean forward) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focused == null) {
                focused = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            }
            if (focused != null) {
                int action = forward ? AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY 
                                     : AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY;
                focused.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                focused.recycle();
            }
            root.recycle();
        }
    }

    // --- SCREEN GESTURE DISPATCHERS ---

    private void dispatchTap(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 50);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        dispatchGesture(builder.build(), null, null);
    }

    private void dispatchSwipe(float fromX, float fromY, float toX, float toY) {
        Path path = new Path();
        path.moveTo(fromX, fromY);
        path.lineTo(toX, toY);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 75);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        dispatchGesture(builder.build(), null, null);
    }

    // --- EMBEDDED HTTP SERVER ---

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
                case "3":
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    break;
                case "back":
                case "4":
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    break;
                case "recents":
                case "apps":
                case "82":
                    performGlobalAction(GLOBAL_ACTION_RECENTS);
                    break;
                case "power":
                case "26":
                    performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                    break;
                case "volume_up":
                case "24":
                    if (am != null) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
                    }
                    break;
                case "volume_down":
                case "25":
                    if (am != null) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
                    }
                    break;
                case "select":
                case "ok":
                case "23":
                    clickAction();
                    break;
                case "up":
                case "19":
                    moveFocusNode(false);
                    dispatchSwipe(683, 500, 683, 200);
                    break;
                case "down":
                case "20":
                    moveFocusNode(true);
                    dispatchSwipe(683, 200, 683, 500);
                    break;
                case "left":
                case "21":
                    dispatchSwipe(550, 384, 200, 384);
                    break;
                case "right":
                case "22":
                    dispatchSwipe(200, 384, 550, 384);
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