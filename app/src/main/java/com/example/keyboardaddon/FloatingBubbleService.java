package com.example.keyboardaddon;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

public class FloatingBubbleService extends AccessibilityService {
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
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        bubbleView = inflater.inflate(R.layout.bubble_layout, null);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100; params.y = 100;

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

        Button btnSymbol = bubbleView.findViewById(R.id.btnSymbol);
        btnSymbol.setOnClickListener(v -> { if (currentNode != null) insertTextAtCursor(currentNode, "<>"); });

        Button btnPaste = bubbleView.findViewById(R.id.btnPaste);
        btnPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null && currentNode != null) insertTextAtCursor(currentNode, text.toString());
                }
            }
        });

        bubbleView.setVisibility(View.GONE);
        windowManager.addView(bubbleView, params);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.isEditable()) {
                currentNode = source;
                if (bubbleView.getVisibility() != View.VISIBLE) bubbleView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void insertTextAtCursor(AccessibilityNodeInfo node, String textToInsert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            CharSequence currentText = node.getText();
            if (currentText == null) currentText = "";

            int start = node.getTextSelectionStart();
            int end = node.getTextSelectionEnd();

            // אם אין בחירה מדויקת או סמן פעיל, נכניס בסוף הטקסט
            if (start < 0 || end < 0) {
                start = currentText.length();
                end = currentText.length();
            }

            // בניית המחרוזת החדשה בשילוב הטקסט שהוזרק במיקום הסמן
            StringBuilder sb = new StringBuilder(currentText);
            sb.replace(Math.min(start, end), Math.max(start, end), textToInsert);

            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sb.toString());
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        }
    }

    @Override public void onInterrupt() { if (bubbleView != null) windowManager.removeView(bubbleView); }
    @Override public void onDestroy() { super.onDestroy(); if (bubbleView != null) windowManager.removeView(bubbleView); }
}
