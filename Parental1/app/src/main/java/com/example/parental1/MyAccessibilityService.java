package com.example.parental1;

import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityService;


public class MyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Code to handle accessibility events here
    }

    @Override
    public void onInterrupt() {
        // Code to handle service interruption here
    }
}
