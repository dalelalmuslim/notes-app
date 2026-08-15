package com.example.notely.data;

/**
 * Minimal persistence abstraction for app preferences.
 *
 * Implementations store string values behind this interface so the settings
 * logic can be unit tested without an Android runtime. The production
 * implementation is {@link SharedPreferencesStore}.
 */
public interface PreferenceStore {

    String getString(String key, String defaultValue);

    void putString(String key, String value);
}
