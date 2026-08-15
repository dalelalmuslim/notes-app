package com.example.notely.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppVersionTest {

    private static int cmp(String a, String b) {
        return AppVersion.parse(a).compareTo(AppVersion.parse(b));
    }

    @Test
    public void equalVersions_compareEqual() {
        assertEquals(0, cmp("1.0.0", "1.0.0"));
    }

    @Test
    public void patchRelease_isNewer() {
        assertTrue(cmp("1.0.0", "1.0.1") < 0);
        assertTrue(cmp("1.0.1", "1.0.0") > 0);
    }

    @Test
    public void minorRelease_isNewer() {
        assertTrue(cmp("1.0.0", "1.1.0") < 0);
    }

    @Test
    public void majorRelease_isNewer() {
        assertTrue(cmp("1.0.0", "2.0.0") < 0);
    }

    @Test
    public void comparisonIsNumericNotLexicographic() {
        assertTrue(cmp("1.9.0", "1.10.0") < 0);
        assertTrue(cmp("1.10.0", "1.9.0") > 0);
    }

    @Test
    public void leadingVTagIsAccepted() {
        assertTrue(cmp("v1.0.1", "1.0.0") > 0);
        assertEquals(0, cmp("v1.0.0", "1.0.0"));
    }

    @Test
    public void malformedVersionsAreRejected() {
        assertNull(AppVersion.parse(null));
        assertNull(AppVersion.parse(""));
        assertNull(AppVersion.parse("   "));
        assertNull(AppVersion.parse("abc"));
        assertNull(AppVersion.parse("1.2"));
        assertNull(AppVersion.parse("1.2.3.4"));
        assertNull(AppVersion.parse("1.a.3"));
        assertNull(AppVersion.parse("1.2.-3"));
        assertNull(AppVersion.parse("1.2.3a"));
        assertNull(AppVersion.parse("1..3"));
        assertNull(AppVersion.parse("."));
    }

    @Test
    public void toString_printsSemver() {
        assertEquals("1.2.3", AppVersion.parse("1.2.3").toString());
        assertEquals("1.10.0", AppVersion.parse("1.10.0").toString());
    }
}
