package com.example.notely.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class NoteTest {

    @Test
    public void createNew_generatesIdAndTimestamps() {
        Note note = Note.createNew("Title", "Body", 1234L);
        assertNotNull(note.id);
        assertEquals("Title", note.title);
        assertEquals("Body", note.content);
        assertEquals(1234L, note.createdAt);
        assertEquals(1234L, note.updatedAt);
    }

    @Test
    public void withEditedContent_preservesIdAndCreatedAt() {
        Note note = Note.createNew("Title", "Body", 100L);
        Note edited = note.withEditedContent("New Title", "New Body", 200L);
        assertEquals(note.id, edited.id);
        assertEquals(note.createdAt, edited.createdAt);
        assertEquals(200L, edited.updatedAt);
        assertEquals("New Title", edited.title);
        assertEquals("New Body", edited.content);
    }

    @Test
    public void emptyTitleAndContentAllowedAtModelLevel() {
        Note note = Note.createNew("", "", 1L);
        assertEquals("", note.title);
        assertEquals("", note.content);
    }

    @Test
    public void equalityReflectsAllFields() {
        Note a = new Note("id", "t", "c", 1L, 2L);
        Note b = new Note("id", "t", "c", 1L, 2L);
        Note c = new Note("id", "t", "c", 1L, 3L);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }
}
