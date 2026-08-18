package com.example.notely.data;

import android.content.Context;

import com.example.notely.model.Note;
import com.example.notely.model.NoteColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Coordinates all note operations and persistence.
 *
 * The UI talks only to this class. Storage and file details stay inside
 * {@link LocalJsonStorage}. The disk-facing API is asynchronous: work runs on
 * a single background worker and results are delivered on the main thread.
 *
 * The UI must never manipulate the JSON file directly.
 *
 * Notes in the Trash ({@code deletedAt != null}) are excluded from the active
 * list and from search. Active notes are ordered pinned-first, then by
 * {@code updatedAt} descending, with the note id as a deterministic tie-breaker.
 */
public final class NoteRepository {

    private final LocalJsonStorage storage;

    public NoteRepository(Context context) {
        this.storage = new LocalJsonStorage(context.getFilesDir());
    }

    /**
     * Package-private for tests so the repository can be exercised against a
     * temporary directory without an Android Context.
     */
    NoteRepository(LocalJsonStorage storage) {
        this.storage = storage;
    }

    // ------------------------------------------------------------------
    // Async API (main-thread UI)
    // ------------------------------------------------------------------

    public void getActiveNotes(ResultCallback<List<Note>> callback) {
        runOffMain(new Supplier<List<Note>>() {
            @Override
            public List<Note> get() {
                return getActiveNotesSync();
            }
        }, callback);
    }

    public void getTrashedNotes(ResultCallback<List<Note>> callback) {
        runOffMain(new Supplier<List<Note>>() {
            @Override
            public List<Note> get() {
                return getTrashedNotesSync();
            }
        }, callback);
    }

    public void searchNotes(String query, ResultCallback<List<Note>> callback) {
        runOffMain(new Supplier<List<Note>>() {
            @Override
            public List<Note> get() {
                return searchNotesSync(query);
            }
        }, callback);
    }

    public void getNoteById(String id, ResultCallback<Note> callback) {
        runOffMain(new Supplier<Note>() {
            @Override
            public Note get() {
                return getNoteByIdSync(id);
            }
        }, callback);
    }

    public void createNote(String title, String content, ResultCallback<Note> callback) {
        runOffMain(new Supplier<Note>() {
            @Override
            public Note get() {
                return createNoteSync(title, content);
            }
        }, callback);
    }

    public void updateNote(String id, String title, String content, ResultCallback<Boolean> callback) {
        runOffMain(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return updateNoteSync(id, title, content);
            }
        }, callback);
    }

    public void setNotePinned(String id, boolean pinned, ResultCallback<Boolean> callback) {
        runOffMain(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return setNotePinnedSync(id, pinned);
            }
        }, callback);
    }

    public void setNoteColor(String id, String color, ResultCallback<Boolean> callback) {
        runOffMain(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return setNoteColorSync(id, color);
            }
        }, callback);
    }

    public void softDeleteNote(String id, ResultCallback<Boolean> callback) {
        runOffMain(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return softDeleteNoteSync(id);
            }
        }, callback);
    }

    public void restoreNote(String id, ResultCallback<Boolean> callback) {
        runOffMain(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return restoreNoteSync(id);
            }
        }, callback);
    }

    public void deleteNote(String id, ResultCallback<Boolean> callback) {
        runOffMain(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return deleteNoteSync(id);
            }
        }, callback);
    }

    private <T> void runOffMain(Supplier<T> task, ResultCallback<T> callback) {
        AppExecutors.runOnDiskIo(new Runnable() {
            @Override
            public void run() {
                final T result = task.get();
                AppExecutors.postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(result);
                    }
                });
            }
        });
    }

    // ------------------------------------------------------------------
    // Synchronous core (package-private; used by the async API and tests)
    // ------------------------------------------------------------------

    /**
     * Returns all active (non-trashed) notes: pinned first, then most recently
     * updated first, with the note id as a deterministic tie-breaker.
     */
    List<Note> getActiveNotesSync() {
        List<Note> notes = storage.readAll();
        List<Note> active = new ArrayList<Note>();
        for (Note note : notes) {
            if (!note.isTrashed()) {
                active.add(note);
            }
        }
        Collections.sort(active, activeOrdering());
        return active;
    }

    /**
     * Returns trashed notes, most recently deleted first, with the note id as
     * a deterministic tie-breaker. A trashed note's pinned state is preserved
     * internally but never influences this list.
     */
    List<Note> getTrashedNotesSync() {
        List<Note> notes = storage.readAll();
        List<Note> trashed = new ArrayList<Note>();
        for (Note note : notes) {
            if (note.isTrashed()) {
                trashed.add(note);
            }
        }
        Collections.sort(trashed, new Comparator<Note>() {
            @Override
            public int compare(Note a, Note b) {
                if (a.deletedAt != b.deletedAt) {
                    return a.deletedAt > b.deletedAt ? -1 : 1;
                }
                return b.id.compareTo(a.id);
            }
        });
        return trashed;
    }

    /**
     * Returns active notes whose title or content contains {@code query},
     * case-insensitively. An empty or blank query behaves like
     * {@link #getActiveNotesSync()}. Trashed notes never match.
     */
    List<Note> searchNotesSync(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return getActiveNotesSync();
        }
        List<Note> active = storage.readAll();
        List<Note> matches = new ArrayList<Note>();
        for (Note note : active) {
            if (note.isTrashed()) {
                continue;
            }
            if (note.title.toLowerCase(Locale.ROOT).contains(needle)
                    || note.content.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(note);
            }
        }
        Collections.sort(matches, activeOrdering());
        return matches;
    }

    /**
     * Returns all notes sorted by updatedAt descending, so the most recently
     * created or edited note comes first. Includes trashed notes.
     */
    List<Note> getNotesSync() {
        List<Note> notes = storage.readAll();
        Collections.sort(notes, new Comparator<Note>() {
            @Override
            public int compare(Note a, Note b) {
                if (a.updatedAt != b.updatedAt) {
                    return a.updatedAt > b.updatedAt ? -1 : 1;
                }
                return b.id.compareTo(a.id);
            }
        });
        return notes;
    }

    Note getNoteByIdSync(String id) {
        for (Note note : storage.readAll()) {
            if (note.id.equals(id)) {
                return note;
            }
        }
        return null;
    }

    /**
     * Creates a new note. Returns null if persistence failed, so callers never
     * report success that did not happen.
     */
    Note createNoteSync(String title, String content) {
        long now = System.currentTimeMillis();
        Note note = Note.createNew(title, content, now);
        List<Note> notes = storage.readAll();
        notes.add(note);
        return storage.writeAll(notes) ? note : null;
    }

    /**
     * Updates title and content of an existing note, refreshing updatedAt.
     * The id, createdAt, pin, color and trash state are preserved. Returns
     * false if the note does not exist or persistence failed.
     */
    boolean updateNoteSync(String id, String title, String content) {
        long now = System.currentTimeMillis();
        return replaceNoteSync(id, new NoteTransform() {
            @Override
            public Note apply(Note note) {
                return note.withEditedContent(title, content, now);
            }
        });
    }

    /**
     * Sets the pinned state of an existing note. A trashed note may carry a
     * pinned state but it never affects the active list. Returns true when the
     * note exists (a no-op that already matches is a successful no-op).
     */
    boolean setNotePinnedSync(String id, boolean pinned) {
        return replaceNoteSync(id, new NoteTransform() {
            @Override
            public Note apply(Note note) {
                return note.isPinned == pinned ? note : note.withPinned(pinned);
            }
        });
    }

    /**
     * Sets the semantic color of an existing note. Unknown colors fall back to
     * the default. Returns true when the note exists.
     */
    boolean setNoteColorSync(String id, String color) {
        final String safeColor = NoteColors.isValid(color) ? color : NoteColors.DEFAULT;
        return replaceNoteSync(id, new NoteTransform() {
            @Override
            public Note apply(Note note) {
                return note.color.equals(safeColor) ? note : note.withColor(safeColor);
            }
        });
    }

    /**
     * Moves an existing note into the Trash by setting deletedAt. The note and
     * all its data are preserved; the active list simply stops showing it.
     * Returns true when the note exists (already-trashed notes are a
     * successful no-op).
     */
    boolean softDeleteNoteSync(String id) {
        final long now = System.currentTimeMillis();
        return replaceNoteSync(id, new NoteTransform() {
            @Override
            public Note apply(Note note) {
                return note.isTrashed() ? note : note.withTrashed(now);
            }
        });
    }

    /**
     * Restores a trashed note by clearing deletedAt and refreshing updatedAt.
     * id, title, content, createdAt, pin and color are preserved. Returns true
     * when the note exists (already-active notes are a successful no-op).
     */
    boolean restoreNoteSync(String id) {
        final long now = System.currentTimeMillis();
        return replaceNoteSync(id, new NoteTransform() {
            @Override
            public Note apply(Note note) {
                return note.isTrashed() ? note.withRestored(now) : note;
            }
        });
    }

    /**
     * Deletes a note permanently.
     *
     * Semantics are explicit and idempotent: deleting a note that does not
     * exist is a successful no-op (true, no persistence write attempted).
     * Deleting an existing note returns true only when the persistence write
     * actually succeeded; otherwise it returns false and the note remains.
     */
    boolean deleteNoteSync(String id) {
        List<Note> notes = storage.readAll();
        boolean removed = false;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id.equals(id)) {
                notes.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) {
            return true;
        }
        return storage.writeAll(notes);
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    private interface NoteTransform {
        Note apply(Note note);
    }

    /**
     * Loads the full list, applies a transformation to the matching note and
     * persists the result. Returns true when the note exists; an unchanged
     * transform is a successful no-op that does not write to disk.
     */
    private boolean replaceNoteSync(String id, NoteTransform transform) {
        List<Note> notes = storage.readAll();
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.id.equals(id)) {
                Note replaced = transform.apply(note);
                if (replaced == note) {
                    return true;
                }
                notes.set(i, replaced);
                return storage.writeAll(notes);
            }
        }
        return false;
    }

    private static Comparator<Note> activeOrdering() {
        return new Comparator<Note>() {
            @Override
            public int compare(Note a, Note b) {
                if (a.isPinned != b.isPinned) {
                    return a.isPinned ? -1 : 1;
                }
                if (a.updatedAt != b.updatedAt) {
                    return a.updatedAt > b.updatedAt ? -1 : 1;
                }
                return b.id.compareTo(a.id);
            }
        };
    }
}