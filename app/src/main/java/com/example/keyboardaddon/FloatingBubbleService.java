package com.example.keyboardaddon;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

public class FloatingBubbleService extends AccessibilityService {
    private static final String TAG = "FloatingBubbleService";

    private WindowManager windowManager;
    private View bubbleView;
    private WindowManager.LayoutParams params;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private AccessibilityNodeInfo currentNode;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingBubble();
    }

    private void createFloatingBubble() {
        // תיקון: עוטפים את ה-Context של ה-Service בתמה של האפליקציה (AppCompat)
        // לפני יצירת ה-inflater. בלי זה, ה-inflater מקבל את התמה הבסיסית
        // של המערכת (ולא את זו שמוגדרת ב-Manifest), ואז ה-Button לא מצליח
        // לפענח את אחד האטריביוטים של סגנון ברירת המחדל שלו וקורס עם
        // UnsupportedOperationException: Failed to resolve attribute.
        Context themedContext = new ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        bubbleView = inflater.inflate(R.layout.bubble_layout, null);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100; params.y = 100;

        // מנגנון גרירת הבועה על המסך
        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isMoving = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x; initialY = params.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        isMoving = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) isMoving = true;
                        params.x = initialX + deltaX; params.y = initialY + deltaY;
                        windowManager.updateViewLayout(bubbleView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isMoving) v.performClick();
                        return true;
                }
                return false;
            }
        });

        // כפתור חץ שמאלה
        Button btnLeft = bubbleView.findViewById(R.id.btnLeft);
        if (btnLeft != null) {
            btnLeft.setOnClickListener(v -> {
                if (currentNode != null) moveCursor(currentNode, -1);
            });
        }

        // כפתור חץ ימינה
        Button btnRight = bubbleView.findViewById(R.id.btnRight);
        if (btnRight != null) {
            btnRight.setOnClickListener(v -> {
                if (currentNode != null) moveCursor(currentNode, 1);
            });
        }

        // כפתור הדבקה מהלוח במיקום הסמן
        Button btnPaste = bubbleView.findViewById(R.id.btnPaste);
        if (btnPaste != null) {
            btnPaste.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence text = clip.getItemAt(0).getText();
                        if (text != null && currentNode != null) {
                            insertTextAtCursor(currentNode, text.toString());
                        }
                    }
                }
            });
        }

        bubbleView.setVisibility(View.GONE);

        // עוטפים את addView ב-try/catch כדי לתפוס במפורש מקרה של הרשאת
        // חלון צף חסרה (SecurityException/BadTokenException), במקום קריסה
        // שקטה שקשה לאבחן.
        try {
            windowManager.addView(bubbleView, params);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add bubble view - is SYSTEM_ALERT_WINDOW permission granted?", e);
            Toast.makeText(this, "שגיאה: לא ניתן להציג את הבועה - בדוק הרשאת 'הצגה מעל אפליקציות אחרות'", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();

        if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                if (source.isEditable()) {
                    currentNode = source;
                    if (bubbleView.getVisibility() != View.VISIBLE) {
                        bubbleView.setVisibility(View.VISIBLE);
                    }
                } else {
                    currentNode = null;
                    if (bubbleView.getVisibility() == View.VISIBLE) {
                        bubbleView.setVisibility(View.GONE);
                    }
                }
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentNode = null;
            if (bubbleView.getVisibility() == View.VISIBLE) {
                bubbleView.setVisibility(View.GONE);
            }
        }
    }

    private void moveCursor(AccessibilityNodeInfo node, int direction) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            CharSequence text = node.getText();
            if (text == null) return;

            int start = node.getTextSelectionStart();
            int end = node.getTextSelectionEnd();

            if (start < 0 || end < 0) {
                start = text.length();
                end = text.length();
            }

            int newPos = start + direction;
            if (newPos < 0) newPos = 0;
            if (newPos > text.length()) newPos = text.length();

            Bundle args = new Bundle();
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newPos);
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newPos);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args);
        }
    }

    private void insertTextAtCursor(AccessibilityNodeInfo node, String textToInsert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            CharSequence currentText = node.getText();
            if (currentText == null) currentText = "";

            int start = node.getTextSelectionStart();
            int end = node.getTextSelectionEnd();

            if (start < 0 || end < 0) {
                start = currentText.length();
                end = currentText.length();
            }

            StringBuilder sb = new StringBuilder(currentText);
            sb.replace(Math.min(start, end), Math.max(start, end), textToInsert);

            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sb.toString());
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

            int newCursorPos = Math.min(start, end) + textToInsert.length();
            Bundle selectionArgs = new Bundle();
            selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursorPos);
            selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursorPos);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs);
        }
    }

    @Override public void onInterrupt() { if (bubbleView != null) windowManager.removeView(bubbleView); }
    @Override public void onDestroy() { super.onDestroy(); if (bubbleView != null) windowManager.removeView(bubbleView); }
}
