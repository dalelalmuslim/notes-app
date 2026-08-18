package com.example.notely.model;

import java.util.UUID;

/**
 * Immutable note state.
 *
 * Besides the note content, a note carries three metadata fields that are
 * persisted alongside it and survive app restarts:
 *
 * - {@code isPinned}: whether the note is pinned above unpinned notes in the
 *   active list. Defaults to false for old notes.
 * - {@code color}: a stable semantic color from {@link NoteColors} (e.g.
 *   {@code "red"}). Defaults to {@code "default"} for old notes.
 * - {@code deletedAt}: null for an active note; non-null (a millisecond
 *   timestamp) for a note that is in the Trash. Soft-deleted notes keep all
 *   their data and can be restored.
 */
public final class Note {

    public final String id;
    public final String title;
    public final String content;
    public final long createdAt;
    public final long updatedAt;
    public final boolean isPinned;
    public final String color;
    public final Long deletedAt;

    public Note(String id, String title, String content, long createdAt, long updatedAt) {
        this(id, title, content, createdAt, updatedAt, false, NoteColors.DEFAULT, null);
    }

    public Note(String id, String title, String content, long createdAt, long updatedAt,
                boolean isPinned, String color, Long deletedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPinned = isPinned;
        this.color = NoteColors.isValid(color) ? color : NoteColors.DEFAULT;
        this.deletedAt = deletedAt;
    }

    public static Note createNew(String title, String content, long now) {
        return new Note(UUID.randomUUID().toString(), title, content, now, now,
                false, NoteColors.DEFAULT, null);
    }

    /** Returns a copy with new title/content and a refreshed updatedAt; all metadata is preserved. */
    public Note withEditedContent(String newTitle, String newContent, long now) {
        return new Note(id, newTitle, newContent, createdAt, now, isPinned, color, deletedAt);
    }

    /** Returns a copy with a new pinned state; id, timestamps, color and trash state are preserved. */
    public Note withPinned(boolean pinned) {
        return new Note(id, title, content, createdAt, updatedAt, pinned, color, deletedAt);
    }

    /** Returns a copy with a new color; id, timestamps, pin and trash state are preserved. */
    public Note withColor(String newColor) {
        return new Note(id, title, content, createdAt, updatedAt, isPinned, newColor, deletedAt);
    }

    /** Returns a copy that is in the Trash, preserving all content and metadata. */
    public Note withTrashed(long now) {
        return new Note(id, title, content, createdAt, now, isPinned, color, now);
    }

    /** Returns a restored copy (no longer in the Trash) with a refreshed updatedAt. */
    public Note withRestored(long now) {
        return new Note(id, title, content, createdAt, now, isPinned, color, null);
    }

    /** True when this note is in the Trash (has a non-null deletedAt). */
    public boolean isTrashed() {
        return deletedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Note)) {
            return false;
        }
        Note note = (Note) o;
        return createdAt == note.createdAt
                && updatedAt == note.updatedAt
                && isPinned == note.isPinned
                && id.equals(note.id)
                && title.equals(note.title)
                && content.equals(note.content)
                && color.equals(note.color)
                && java.util.Objects.equals(deletedAt, note.deletedAt);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + content.hashCode();
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        result = 31 * result + (int) (updatedAt ^ (updatedAt >>> 32));
        result = 31 * result + (isPinned ? 1 : 0);
        result = 31 * result + color.hashCode();
        result = 31 * result + (deletedAt != null ? deletedAt.hashCode() : 0);
        return result;
    }
}