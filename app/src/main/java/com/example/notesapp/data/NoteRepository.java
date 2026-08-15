package com.example.notesapp.data;

import android.content.Context;

import com.example.notesapp.model.Note;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Coordinates all note operations and persistence.
 *
 * The UI talks only to this class. Storage and file details stay inside
 * {@link LocalJsonStorage}. Notes are always returned sorted by
 * updatedAt descending, so the most recently created or edited note comes first.
 */
public final class NoteRepository {

    private final LocalJsonStorage storage;

    public NoteRepository(Context context) {
        this.storage = new LocalJsonStorage(context.getFilesDir());
    }

    public List<Note> getNotes() {
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

    public Note getNoteById(String id) {
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
    public Note createNote(String title, String content) {
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
    public boolean updateNote(String id, String title, String content) {
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
     * Deletes a note permanently. Returns false only when a persistence write
     * was attempted and failed.
     */
    public boolean deleteNote(String id) {
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
