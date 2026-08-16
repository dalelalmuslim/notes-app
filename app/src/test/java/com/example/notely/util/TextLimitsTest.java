package com.example.notely.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TextLimitsTest {

    @Test
    public void requiredLimits() {
        assertEquals(120, TextLimits.MAX_TITLE_LENGTH);
        assertEquals(30000, TextLimits.MAX_CONTENT_LENGTH);
    }

    @Test
    public void codePointCount_countsUnicode() {
        assertEquals(3, TextLimits.codePointCount("abc"));
        assertEquals(5, TextLimits.codePointCount("مرحبا"));
        assertEquals(2, TextLimits.codePointCount("a😀"));
        assertEquals(1, TextLimits.codePointCount("😀"));
        assertEquals(0, TextLimits.codePointCount(null));
        assertEquals(0, TextLimits.codePointCount(""));
    }

    @Test
    public void truncate_noopWhenWithinLimit() {
        assertEquals("hello", TextLimits.truncate("hello", 120));
        assertEquals("", TextLimits.truncate("", 120));
    }

    @Test
    public void truncate_limitsToCodePoints() {
        assertEquals("abc", TextLimits.truncate("abcde", 3));
        assertEquals("مرحبا", TextLimits.truncate("مرحبابالعالم", 5));
    }

    @Test
    public void truncate_doesNotSplitSurrogatePairs() {
        assertEquals("a😀", TextLimits.truncate("a😀b", 2));
        assertEquals("😀", TextLimits.truncate("😀😀", 1));
    }

    @Test
    public void truncate_longContent_limitsToMaxContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40000; i++) {
            sb.append('x');
        }
        String out = TextLimits.truncate(sb.toString(), TextLimits.MAX_CONTENT_LENGTH);
        assertEquals(TextLimits.MAX_CONTENT_LENGTH, TextLimits.codePointCount(out));
    }

    @Test
    public void truncate_longTitle_limitsToMaxTitle() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append('ع');
        }
        String out = TextLimits.truncate(sb.toString(), TextLimits.MAX_TITLE_LENGTH);
        assertEquals(TextLimits.MAX_TITLE_LENGTH, TextLimits.codePointCount(out));
    }

    @Test
    public void truncate_nullAndNegativeLimit() {
        assertEquals("", TextLimits.truncate(null, 10));
        assertEquals("", TextLimits.truncate("abc", -1));
    }
}
