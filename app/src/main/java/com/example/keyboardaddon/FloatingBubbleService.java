package com.example.keyboardaddon;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.Gravity;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.ContextThemeWrapper;
import androidx.annotation.Nullable;

public class FloatingBubbleService extends Service {

    private WindowManager windowManager;
    private View bubbleView;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // פתרון הבעיה: שימוש ב-ContextThemeWrapper עם ערכת נושא תקינה כדי שפקודות ה-?attr יפעלו כשורה
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, R.style.Theme_AppCompat_Light);
        
        // יצירת LayoutInflater מתוך המעטפת המעוצבת
        LayoutInflater inflater = LayoutInflater.from(contextThemeWrapper);
        
        // טעינת קובץ ה-XML של הבועה (שורה 35 המשוערת אצלך)
        bubbleView = inflater.inflate(R.layout.your_bubble_layout, null);

        // הגדרת פרמטרים לחלון הצף בהתאם לגרסת האנדרואיד
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        // הוספת התצוגה למסך
        windowManager.addView(bubbleView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null && windowManager != null) {
            windowManager.removeView(bubbleView);
        }
    }
}
