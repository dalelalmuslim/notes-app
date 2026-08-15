package com.example.notely.util;

/**
 * User-facing text limits for notes.
 *
 * Limits are counted in Unicode code points, so Arabic text, emoji and other
 * supplementary characters are never split into invalid halves. {@link #truncate}
 * is used by the editor to enforce the limits at the input boundary without
 * ever silently altering data that is only being read.
 */
public final class TextLimits {

    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_CONTENT_LENGTH = 20000;

    private TextLimits() {
    }

    /**
     * Returns the number of Unicode code points in {@code text}.
     */
    public static int codePointCount(String text) {
        if (text == null) {
            return 0;
        }
        return text.codePointCount(0, text.length());
    }

    /**
     * Truncates {@code text} so it contains at most {@code maxCodePoints}
     * Unicode code points. Surrogate pairs are never split. Returns an empty
     * string for null input or a negative limit.
     */
    public static String truncate(String text, int maxCodePoints) {
        if (text == null || maxCodePoints < 0) {
            return "";
        }
        if (codePointCount(text) <= maxCodePoints) {
            return text;
        }
        int endIndex = text.offsetByCodePoints(0, maxCodePoints);
        return text.substring(0, endIndex);
    }
}
