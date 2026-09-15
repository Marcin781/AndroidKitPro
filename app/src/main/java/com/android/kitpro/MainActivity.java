package com.android.kitpro;

import android.os.Bundle;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.status_text);
        checkCyberAgent();
    }

    private void checkCyberAgent() {
        statusText.setText("CyberAgent\nŁączenie z backendem…");
        new Thread(() -> {
            try {
                JSONObject feed = CyberAgentApi.fetchThreatFeed();
                int version = feed.optInt("version", 0);
                int indicators = feed.optJSONArray("indicators") == null
                        ? 0 : feed.optJSONArray("indicators").length();
                runOnUiThread(() -> statusText.setText(
                        "CyberAgent\nBackend: ONLINE ✓\nFeed: v" + version
                                + "\nIOC: " + indicators));
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText(
                        "CyberAgent\nBackend: OFFLINE / BŁĄD\n" + e.getMessage()));
            }
        }).start();
    }
}
