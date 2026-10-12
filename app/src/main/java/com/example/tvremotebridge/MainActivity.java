package com.example.tvremotebridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView status = new TextView(this);
        status.setText("TV Remote Bridge\nClick below to enable Accessibility:");
        status.setTextSize(20);
        layout.addView(status);

        Button enableBtn = new Button(this);
        enableBtn.setText("Open Accessibility Settings");
        enableBtn.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        layout.addView(enableBtn);

        setContentView(layout);
    }
}
