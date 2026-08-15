package com.example.notely.data;

import android.content.SharedPreferences;

/**
 * {@link PreferenceStore} backed by Android {@link SharedPreferences}.
 */
public final class SharedPreferencesStore implements PreferenceStore {

    private final SharedPreferences preferences;

    public SharedPreferencesStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    @Override
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }
}
