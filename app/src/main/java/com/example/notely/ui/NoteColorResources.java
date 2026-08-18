package com.example.notely.ui;

import com.example.notely.R;
import com.example.notely.model.NoteColors;

/**
 * Maps the persisted semantic note colors ({@link NoteColors}) to Android
 * drawable resources. All actual colors live in {@code values/colors.xml}
 * (light) and {@code values-night/colors.xml} (dark); this class never embeds
 * raw color values. Unknown colors resolve to the default card/swatch.
 */
public final class NoteColorResources {

    private NoteColorResources() {
    }

    /** Ripple card background drawable for a note color. */
    public static int cardBackground(String color) {
        if (NoteColors.RED.equals(color)) {
            return R.drawable.item_note_background_red;
        }
        if (NoteColors.ORANGE.equals(color)) {
            return R.drawable.item_note_background_orange;
        }
        if (NoteColors.YELLOW.equals(color)) {
            return R.drawable.item_note_background_yellow;
        }
        if (NoteColors.GREEN.equals(color)) {
            return R.drawable.item_note_background_green;
        }
        if (NoteColors.BLUE.equals(color)) {
            return R.drawable.item_note_background_blue;
        }
        if (NoteColors.PURPLE.equals(color)) {
            return R.drawable.item_note_background_purple;
        }
        return R.drawable.item_note_background;
    }

    /** Circular color swatch drawable for the editor palette. */
    public static int swatch(String color) {
        if (NoteColors.RED.equals(color)) {
            return R.drawable.swatch_red;
        }
        if (NoteColors.ORANGE.equals(color)) {
            return R.drawable.swatch_orange;
        }
        if (NoteColors.YELLOW.equals(color)) {
            return R.drawable.swatch_yellow;
        }
        if (NoteColors.GREEN.equals(color)) {
            return R.drawable.swatch_green;
        }
        if (NoteColors.BLUE.equals(color)) {
            return R.drawable.swatch_blue;
        }
        if (NoteColors.PURPLE.equals(color)) {
            return R.drawable.swatch_purple;
        }
        return R.drawable.swatch_default;
    }
}