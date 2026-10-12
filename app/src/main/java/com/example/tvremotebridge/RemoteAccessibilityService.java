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

    private class RemoteHttpServer extends NanoHTTPD {
        public RemoteHttpServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri().replace("/", "").toLowerCase();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            // Agar user direct browser me IP khole (http://192.168.100.5:8080)
            if (uri.isEmpty() || uri.equals("remote")) {
                String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<title>MStar Remote</title><style>"
                    + "*{box-sizing:border-box;touch-action:manipulation}body{margin:0;padding:16px;background:#0f172a;color:#fff;font-family:system-ui;display:flex;justify-content:center;align-items:center;min-height:100vh}"
                    + ".box{width:100%;max-width:320px;background:#1e293b;border-radius:32px;padding:24px 18px;display:flex;flex-direction:column;gap:20px;align-items:center}"
                    + ".btn{border:none;display:flex;align-items:center;justify-content:center;color:#fff;border-radius:14px;background:#334155;font-weight:bold;cursor:pointer;-webkit-tap-highlight-color:transparent}"
                    + ".btn:active{transform:scale(.92);background:#475569}.pwr{width:44px;height:44px;border-radius:50%!important;background:#dc2626!important}"
                    + ".dpad{position:relative;width:200px;height:200px;background:#0f172a;border-radius:50%}"
                    + ".dpad .btn{position:absolute;width:52px;height:52px}"
                    + ".up{top:6px;left:50%;transform:translateX(-50%)}.down{bottom:6px;left:50%;transform:translateX(-50%)}.left{left:6px;top:50%;transform:translateY(-50%)}.right{right:6px;top:50%;transform:translateY(-50%)}"
                    + ".ok{width:64px!important;height:64px!important;border-radius:50%!important;background:#2563eb!important;left:50%;top:50%;transform:translate(-50%,-50%);z-index:2}"
                    + ".g3{width:100%;display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.g3 .btn{height:48px;width:100%}"
                    + ".g2{width:100%;display:grid;grid-template-columns:1fr 1fr;gap:10px}.g2 .btn{height:52px;background:#0284c7;width:100%}"
                    + "</style></head><body><div class='box'>"
                    + "<div style='width:100%;display:flex;justify-content:space-between;align-items:center'><b style='font-size:12px;color:#94a3b8'>MStar Remote</b><button class='btn pwr' onclick='send(\"power\")'>⏻</button></div>"
                    + "<div class='dpad'><button class='btn up' onclick='send(\"up\")'>▲</button><button class='btn left' onclick='send(\"left\")'>◀</button><button class='btn ok' onclick='send(\"select\")'>OK</button><button class='btn right' onclick='send(\"right\")'>▶</button><button class='btn down' onclick='send(\"down\")'>▼</button></div>"
                    + "<div class='g3'><button class='btn' onclick='send(\"back\")'>BACK</button><button class='btn' onclick='send(\"home\")'>HOME</button><button class='btn' onclick='send(\"recents\")'>APPS</button></div>"
                    + "<div class='g2'><button class='btn' onclick='send(\"volume_down\")'>VOL −</button><button class='btn' onclick='send(\"volume_up\")'>VOL ＋</button></div>"
                    + "</div><script>"
                    + "function send(cmd){ fetch('/' + cmd + '?t=' + Date.now()).catch(()=>{}); }"
                    + "</script></body></html>";
                return newFixedLengthResponse(Response.Status.OK, "text/html", html);
            }

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