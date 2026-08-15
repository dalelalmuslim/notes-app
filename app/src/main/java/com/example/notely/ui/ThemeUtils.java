package com.example.notely.ui;

import android.content.Context;
import android.util.TypedValue;

/**
 * Small helpers for resolving theme-driven resources at runtime.
 */
public final class ThemeUtils {

    private ThemeUtils() {
    }

    /**
     * Resolves a color attribute from the activity's current theme. Used where
     * a color must follow the selected theme (e.g. the destructive action in a
     * system dialog) rather than the raw color resource, which is night-mode
     * aware and would not match an explicitly chosen theme.
     */
    public static int resolveColor(Context context, int attrResId) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attrResId, value, true)) {
            return value.data;
        }
        return 0xFF1B1F24;
    }
}
