package com.example.notely.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppSettingsTest {

    @Test
    public void defaults_languageEnglish_themeSystem() {
        AppSettings settings = new AppSettings(new MemoryPreferenceStore());
        assertEquals(AppSettings.LANGUAGE_ENGLISH, settings.getLanguage());
        assertFalse(settings.isArabic());
        assertEquals(AppSettings.THEME_SYSTEM, settings.getTheme());
    }

    @Test
    public void setLanguage_persistsArabic() {
        AppSettings settings = new AppSettings(new MemoryPreferenceStore());
        settings.setLanguage(AppSettings.LANGUAGE_ARABIC);
        assertEquals(AppSettings.LANGUAGE_ARABIC, settings.getLanguage());
        assertTrue(settings.isArabic());
    }

    @Test
    public void setLanguage_invalidValueFallsBackToEnglish() {
        AppSettings settings = new AppSettings(new MemoryPreferenceStore());
        settings.setLanguage("fr");
        assertEquals(AppSettings.LANGUAGE_ENGLISH, settings.getLanguage());
    }

    @Test
    public void theme_persistsAndInvalidFallsBackToSystem() {
        AppSettings settings = new AppSettings(new MemoryPreferenceStore());
        assertEquals(AppSettings.THEME_SYSTEM, settings.getTheme());

        settings.setTheme(AppSettings.THEME_DARK);
        assertEquals(AppSettings.THEME_DARK, settings.getTheme());

        settings.setTheme(AppSettings.THEME_LIGHT);
        assertEquals(AppSettings.THEME_LIGHT, settings.getTheme());

        settings.setTheme("neon");
        assertEquals(AppSettings.THEME_SYSTEM, settings.getTheme());
    }

    @Test
    public void preferencesSurviveNewInstanceOnSameStore() {
        MemoryPreferenceStore store = new MemoryPreferenceStore();
        AppSettings first = new AppSettings(store);
        first.setLanguage(AppSettings.LANGUAGE_ARABIC);
        first.setTheme(AppSettings.THEME_DARK);

        AppSettings second = new AppSettings(store);
        assertEquals(AppSettings.LANGUAGE_ARABIC, second.getLanguage());
        assertEquals(AppSettings.THEME_DARK, second.getTheme());
    }
}
