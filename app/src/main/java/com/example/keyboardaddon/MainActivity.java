package com.example.keyboardaddon;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // כפתור פתיחת הגדרות נגישות
        Button btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility);
        btnOpenAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        // כפתור בקשת הרשאת חלון צף (SYSTEM_ALERT_WINDOW)
        Button btnOpenOverlayPermission = findViewById(R.id.btnOpenOverlayPermission);
        btnOpenOverlayPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.style = Toast.makeText(this, "ההרשאה כבר ניתנה!", Toast.LENGTH_SHORT);
                    Toast.makeText(this, "ההרשאה כבר ניתנה!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "ההרשאה ניתנת אוטומטית בגרסה זו", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
