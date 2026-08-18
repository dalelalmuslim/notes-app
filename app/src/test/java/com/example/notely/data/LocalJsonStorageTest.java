package com.example.notely.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.notely.model.Note;
import com.example.notely.model.NoteColors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalJsonStorageTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static void writeRaw(File file, String text) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        try {
            writer.write(text);
        } finally {
            writer.close();
        }
    }

    @Test
    public void missingFile_readsEmpty() {
        assertTrue(new LocalJsonStorage(tmp.getRoot()).readAll().isEmpty());
    }

    @Test
    public void emptyFile_readsEmpty() throws Exception {
        writeRaw(new File(tmp.getRoot(), "notes.json"), "");
        assertTrue(new LocalJsonStorage(tmp.getRoot()).readAll().isEmpty());
    }

    @Test
    public void whitespaceOnlyFile_readsEmpty() throws Exception {
        writeRaw(new File(tmp.getRoot(), "notes.json"), " \n\t ");
        assertTrue(new LocalJsonStorage(tmp.getRoot()).readAll().isEmpty());
    }

    @Test
    public void roundTrip_specialCharacters() throws Exception {
        LocalJsonStorage storage = new LocalJsonStorage(tmp.getRoot());
        String content = "خطوة عربية\n\"quotes\" \\backslash\\ \ttab\r\nمرحبا بالعالم — ✓ 😀";
        List<Note> in = Arrays.asList(
                new Note("id-1", "مرحبا", content, 100L, 200L),
                new Note("id-2", "Hello \"world\"", "line1\nline2\tend \\\\ slash", 300L, 400L),
                new Note("id-3", "", "", 500L, 600L));
        assertTrue(storage.writeAll(in));
        assertEquals(in, storage.readAll());
    }

    @Test
    public void roundTrip_manyNotesAndLongContent() throws Exception {
        LocalJsonStorage storage = new LocalJsonStorage(tmp.getRoot());
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            longText.append("مرحبا ").append(i).append('\n');
        }
        List<Note> in = new ArrayList<Note>();
        for (int i = 0; i < 50; i++) {
            in.add(new Note("id-" + i, "Note " + i, i == 0 ? longText.toString() : "body " + i, i, i));
        }
        assertTrue(storage.writeAll(in));
        assertEquals(in, storage.readAll());
    }

    @Test
    public void malformedJson_isPreservedAndEmptyReturned() throws Exception {
        File dir = tmp.getRoot();
        File target = new File(dir, "notes.json");
        writeRaw(target, "{ this is not json ]");
        LocalJsonStorage storage = new LocalJsonStorage(dir);
        assertTrue(storage.readAll().isEmpty());
        assertFalse(target.exists());
        File[] files = dir.listFiles();
        assertEquals(1, files.length);
        assertTrue(files[0].getName().startsWith("notes.json.corrupt-"));
    }

    @Test
    public void rootNotArray_isTreatedAsCorruptAndPreserved() throws Exception {
        File dir = tmp.getRoot();
        File target = new File(dir, "notes.json");
        writeRaw(target, "{\"id\":\"not-an-array\"}");
        LocalJsonStorage storage = new LocalJsonStorage(dir);
        assertTrue(storage.readAll().isEmpty());
        assertFalse(target.exists());
        File[] files = dir.listFiles();
        assertEquals(1, files.length);
        assertTrue(files[0].getName().startsWith("notes.json.corrupt-"));
    }

    @Test
    public void duplicateIds_areBothPreserved() throws Exception {
        File dir = tmp.getRoot();
        writeRaw(new File(dir, "notes.json"),
                "[{\"id\":\"dup\",\"title\":\"a\",\"content\":\"c1\",\"createdAt\":1,\"updatedAt\":1},"
                        + "{\"id\":\"dup\",\"title\":\"b\",\"content\":\"c2\",\"createdAt\":2,\"updatedAt\":2}]");
        List<Note> notes = new LocalJsonStorage(dir).readAll();
        assertEquals(2, notes.size());
        assertEquals("a", notes.get(0).title);
        assertEquals("b", notes.get(1).title);
    }

    @Test
    public void malformedTimestamps_areSkipped() throws Exception {
        File dir = tmp.getRoot();
        writeRaw(new File(dir, "notes.json"),
                "[{\"id\":\"a\",\"title\":\"ok\",\"content\":\"c\",\"createdAt\":\"1\",\"updatedAt\":1}]");
        List<Note> notes = new LocalJsonStorage(dir).readAll();
        assertTrue(notes.isEmpty());
    }

    @Test
    public void malformedEntry_withinArray_isSkipped() throws Exception {
        File dir = tmp.getRoot();
        writeRaw(new File(dir, "notes.json"),
                "[{\"id\":\"a\",\"title\":\"ok\",\"content\":\"c\",\"createdAt\":1,\"updatedAt\":1},"
                        + "{\"id\":\"b\",\"title\":5,\"content\":\"c\",\"createdAt\":1,\"updatedAt\":1}]");
        List<Note> notes = new LocalJsonStorage(dir).readAll();
        assertEquals(1, notes.size());
        assertEquals("a", notes.get(0).id);
    }

    @Test
    public void writeFailure_returnsFalseAndLeavesExistingData() throws Exception {
        File dir = tmp.getRoot();
        LocalJsonStorage storage = new LocalJsonStorage(dir);
        assertTrue(storage.writeAll(Arrays.asList(new Note("a", "t", "c", 1L, 1L))));

        LocalJsonStorage broken = new LocalJsonStorage(new File(dir, "does-not-exist"));
        assertFalse(broken.writeAll(Arrays.asList(new Note("b", "t", "c", 2L, 2L))));

        assertEquals(Arrays.asList(new Note("a", "t", "c", 1L, 1L)), storage.readAll());
    }

    @Test
    public void preservesInsertionOrder() throws Exception {
        LocalJsonStorage storage = new LocalJsonStorage(tmp.getRoot());
        List<Note> in = Arrays.asList(
                new Note("1", "a", "", 1L, 1L),
                new Note("2", "b", "", 1L, 1L),
                new Note("3", "c", "", 1L, 1L));
        assertTrue(storage.writeAll(in));
        List<Note> out = storage.readAll();
        assertEquals(3, out.size());
        assertEquals("1", out.get(0).id);
        assertEquals("3", out.get(2).id);
    }

    @Test
    public void oldRecordWithoutNewFields_loadsWithDefaults() throws Exception {
        File dir = tmp.getRoot();
        writeRaw(new File(dir, "notes.json"),
                "[{\"id\":\"a\",\"title\":\"old\",\"content\":\"body\",\"createdAt\":1,\"updatedAt\":2}]");
        List<Note> notes = new LocalJsonStorage(dir).readAll();
        assertEquals(1, notes.size());
        Note note = notes.get(0);
        assertFalse(note.isPinned);
        assertEquals(NoteColors.DEFAULT, note.color);
        assertNull(note.deletedAt);
        assertEquals("old", note.title);
        assertEquals("body", note.content);
        assertEquals(1L, note.createdAt);
        assertEquals(2L, note.updatedAt);
    }

    @Test
    public void newFields_roundTrip() throws Exception {
        LocalJsonStorage storage = new LocalJsonStorage(tmp.getRoot());
        List<Note> in = Arrays.asList(
                new Note("id-1", "title", "content", 100L, 200L, true, NoteColors.BLUE, 300L),
                new Note("id-2", "t", "c", 1L, 2L, false, NoteColors.PURPLE, null));
        assertTrue(storage.writeAll(in));
        List<Note> out = storage.readAll();
        assertEquals(in, out);
        assertTrue(out.get(0).isPinned);
        assertEquals(NoteColors.BLUE, out.get(0).color);
        assertEquals(Long.valueOf(300L), out.get(0).deletedAt);
        assertFalse(out.get(1).isPinned);
        assertNull(out.get(1).deletedAt);
    }

    @Test
    public void unknownColor_fallsBackToDefault() throws Exception {
        File dir = tmp.getRoot();
        writeRaw(new File(dir, "notes.json"),
                "[{\"id\":\"a\",\"title\":\"t\",\"content\":\"c\",\"createdAt\":1,\"updatedAt\":2,"
                        + "\"isPinned\":true,\"color\":\"neon\",\"deletedAt\":3}]");
        Note note = new LocalJsonStorage(dir).readAll().get(0);
        assertEquals(NoteColors.DEFAULT, note.color);
        assertTrue(note.isPinned);
        assertEquals(Long.valueOf(3L), note.deletedAt);
    }

    @Test
    public void nonBooleanPinned_andNonLongDeletedAt_fallBack() throws Exception {
        File dir = tmp.getRoot();
        writeRaw(new File(dir, "notes.json"),
                "[{\"id\":\"a\",\"title\":\"t\",\"content\":\"c\",\"createdAt\":1,\"updatedAt\":2,"
                        + "\"isPinned\":\"yes\",\"color\":\"red\",\"deletedAt\":\"123\"}]");
        Note note = new LocalJsonStorage(dir).readAll().get(0);
        assertFalse(note.isPinned);
        assertEquals(NoteColors.RED, note.color);
        assertNull(note.deletedAt);
    }
}
