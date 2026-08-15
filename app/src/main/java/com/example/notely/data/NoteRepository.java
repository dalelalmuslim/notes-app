package com.example.notely.data;

import android.content.Context;

import com.example.notely.model.Note;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Coordinates all note operations and persistence.
 *
 * The UI talks only to this class. Storage and file details stay inside
 * {@link LocalJsonStorage}. The disk-facing API is asynchronous: work runs on
 * a single background worker and results are delivered on the main thread.
 *
 * The UI must never manipulate the JSON file directly.
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

    public void getNotes(ResultCallback<List<Note>> callback) {
        runOffMain(new Supplier<List<Note>>() {
            @Override
            public List<Note> get() {
                return getNotesSync();
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
     * Returns all notes sorted by updatedAt descending, so the most recently
     * created or edited note comes first.
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
     * The id and createdAt are preserved. Returns false if the note does not
     * exist or persistence failed.
     */
    boolean updateNoteSync(String id, String title, String content) {
        long now = System.currentTimeMillis();
        List<Note> notes = storage.readAll();
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.id.equals(id)) {
                notes.set(i, note.withEditedContent(title, content, now));
                return storage.writeAll(notes);
            }
        }
        return false;
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
}
