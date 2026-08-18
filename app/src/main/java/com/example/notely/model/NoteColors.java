package com.example.notely.model;

/**
 * Stable semantic note colors.
 *
 * Only these values are ever persisted. Notes never store raw RGB values, so a
 * palette change is a pure UI concern. Unknown values are treated as
 * {@link #DEFAULT}, which keeps old or malformed records loading safely.
 */
public final class NoteColors {

    public static final String DEFAULT = "default";
    public static final String RED = "red";
    public static final String ORANGE = "orange";
    public static final String YELLOW = "yellow";
    public static final String GREEN = "green";
    public static final String BLUE = "blue";
    public static final String PURPLE = "purple";

    /** The full palette in display order (used by the editor and validation). */
    public static final String[] PALETTE = {
            DEFAULT, RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE
    };

    private NoteColors() {
    }

    /** Returns true for any color that is part of the persisted palette. */
    public static boolean isValid(String color) {
        if (color == null) {
            return false;
        }
        for (String candidate : PALETTE) {
            if (candidate.equals(color)) {
                return true;
            }
        }
        return false;
    }
}