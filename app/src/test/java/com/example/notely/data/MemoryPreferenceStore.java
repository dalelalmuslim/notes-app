package com.example.notely.data;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link PreferenceStore} for unit tests.
 */
final class MemoryPreferenceStore implements PreferenceStore {

    private final Map<String, String> values = new HashMap<String, String>();

    @Override
    public String getString(String key, String defaultValue) {
        String value = values.get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putString(String key, String value) {
        values.put(key, value);
    }
}
