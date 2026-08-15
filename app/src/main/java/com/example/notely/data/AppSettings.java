package com.example.notely.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * User-facing application preferences (language and theme) plus locale wiring.
 *
 * Language and theme selections persist across app restarts through
 * {@link SharedPreferences}. The language drives Android resource
 * localization and layout directionality via {@link #attachLocale(Context)},
 * which Activities apply in {@code attachBaseContext}. The theme drives
 * which visual palette the Activities select.
 *
 * Preferences are read through a {@link PreferenceStore} so the behavior is
 * unit testable without an Android runtime.
 */
public final class AppSettings {

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_ARABIC = "ar";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static final String PREFS_NAME = "notely_settings";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";

    private final PreferenceStore store;

    public AppSettings(PreferenceStore store) {
        this.store = store;
    }

    public static AppSettings get(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new AppSettings(new SharedPreferencesStore(prefs));
    }

    public static String getLanguage(Context context) {
        return get(context).getLanguage();
    }

    public static String getTheme(Context context) {
        return get(context).getTheme();
    }

    public String getLanguage() {
        String value = store.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH);
        return LANGUAGE_ARABIC.equals(value) ? LANGUAGE_ARABIC : LANGUAGE_ENGLISH;
    }

    public boolean isArabic() {
        return LANGUAGE_ARABIC.equals(getLanguage());
    }

    public void setLanguage(String language) {
        store.putString(KEY_LANGUAGE,
                LANGUAGE_ARABIC.equals(language) ? LANGUAGE_ARABIC : LANGUAGE_ENGLISH);
    }

    public String getTheme() {
        String value = store.getString(KEY_THEME, THEME_SYSTEM);
        if (THEME_LIGHT.equals(value) || THEME_DARK.equals(value)) {
            return value;
        }
        return THEME_SYSTEM;
    }

    public void setTheme(String theme) {
        store.putString(KEY_THEME,
                THEME_LIGHT.equals(theme) || THEME_DARK.equals(theme) ? theme : THEME_SYSTEM);
    }

    /**
     * Returns a context whose resources use the persisted language and whose
     * default locale matches. Must be applied in {@code attachBaseContext}
     * before the activity's resources are first used.
     */
    public static Context attachLocale(Context base) {
        Locale locale = LANGUAGE_ARABIC.equals(getLanguage(base))
                ? new Locale(LANGUAGE_ARABIC)
                : Locale.ENGLISH;
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        return base.createConfigurationContext(configuration);
    }
}
