package com.example.notely.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.example.notely.R;
import com.example.notely.data.AppSettings;

/**
 * Base class for all Notely activities.
 *
 * Applies the persisted language in {@code attachBaseContext} (before any
 * resource is resolved) and the persisted theme in {@code onCreate}. If the
 * language or theme changed while the activity was paused (e.g. from the
 * Settings screen), the activity recreates itself so every string, label and
 * layout direction is up to date.
 */
public abstract class BaseActivity extends Activity {

    private String appliedLanguage;
    private String appliedTheme;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.attachLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        appliedLanguage = AppSettings.getLanguage(this);
        appliedTheme = AppSettings.getTheme(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String language = AppSettings.getLanguage(this);
        String theme = AppSettings.getTheme(this);
        if (!language.equals(appliedLanguage) || !theme.equals(appliedTheme)) {
            appliedLanguage = language;
            appliedTheme = theme;
            recreate();
        }
    }

    private void applyTheme() {
        String theme = AppSettings.getTheme(this);
        if (AppSettings.THEME_DARK.equals(theme)) {
            setTheme(R.style.Theme_Notely_Dark);
        } else if (AppSettings.THEME_LIGHT.equals(theme)) {
            setTheme(R.style.Theme_Notely_Light);
        }
        // THEME_SYSTEM uses the manifest theme (Theme.Notely), which resolves
        // to Light or Dark based on the system night mode.
    }
}
