package com.example.tvremotebridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
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

    private class RemoteHttpServer extends NanoHTTPD {
        public RemoteHttpServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri().replace("/", "");

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
                case "notifications":
                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                    break;
                case "power":
                    performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                    break;
                case "select":
                    clickScreen(683, 384);
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