package com.example.notely.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.notely.model.Note;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NoteRepositoryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private NoteRepository repo() {
        return new NoteRepository(new LocalJsonStorage(tmp.getRoot()));
    }

    @Test
    public void create_persistsAndAppearsInList() {
        NoteRepository repository = repo();
        Note created = repository.createNoteSync("Title", "Body");
        assertNotNull(created);
        assertEquals(created, repository.getNoteByIdSync(created.id));
        List<Note> notes = repository.getNotesSync();
        assertEquals(1, notes.size());
        assertEquals(created.id, notes.get(0).id);
    }

    @Test
    public void create_reportsFailureWhenPersistenceFails() {
        NoteRepository repository = new NoteRepository(
                new LocalJsonStorage(new File(tmp.getRoot(), "missing-dir")));
        assertNull(repository.createNoteSync("Title", "Body"));
    }

    @Test
    public void update_preservesIdentityAndRefreshesTimestamp() {
        NoteRepository repository = repo();
        Note created = repository.createNoteSync("Old", "Body");
        assertTrue(repository.updateNoteSync(created.id, "New", "Changed"));
        Note updated = repository.getNoteByIdSync(created.id);
        assertEquals(created.id, updated.id);
        assertEquals(created.createdAt, updated.createdAt);
        assertEquals("New", updated.title);
        assertEquals("Changed", updated.content);
        assertTrue(updated.updatedAt >= created.updatedAt);
    }

    @Test
    public void update_missingNote_returnsFalse() {
        NoteRepository repository = repo();
        assertFalse(repository.updateNoteSync("does-not-exist", "x", "y"));
    }

    @Test
    public void delete_removesNote() {
        NoteRepository repository = repo();
        Note created = repository.createNoteSync("T", "C");
        assertTrue(repository.deleteNoteSync(created.id));
        assertNull(repository.getNoteByIdSync(created.id));
        assertTrue(repository.getNotesSync().isEmpty());
    }

    @Test
    public void delete_preservesOtherNotes() {
        NoteRepository repository = repo();
        Note a = repository.createNoteSync("a", "x");
        Note b = repository.createNoteSync("b", "y");
        assertTrue(repository.deleteNoteSync(a.id));
        List<Note> notes = repository.getNotesSync();
        assertEquals(1, notes.size());
        assertEquals(b.id, notes.get(0).id);
        assertNotNull(repository.getNoteByIdSync(b.id));
    }

    @Test
    public void delete_missingNote_isIdempotentNoOp() {
        NoteRepository repository = repo();
        assertTrue(repository.deleteNoteSync("does-not-exist"));
    }

    @Test
    public void getNoteById_missing_returnsNull() {
        assertNull(repo().getNoteByIdSync("nope"));
    }

    @Test
    public void ordering_mostRecentlyUpdatedFirst() throws Exception {
        LocalJsonStorage storage = new LocalJsonStorage(tmp.getRoot());
        storage.writeAll(Arrays.asList(
                new Note("old", "old", "", 1L, 100L),
                new Note("newest", "newest", "", 1L, 300L),
                new Note("middle", "middle", "", 1L, 200L)));
        NoteRepository repository = new NoteRepository(storage);
        List<Note> ordered = repository.getNotesSync();
        assertEquals("newest", ordered.get(0).id);
        assertEquals("middle", ordered.get(1).id);
        assertEquals("old", ordered.get(2).id);
    }

    @Test
    public void repeatedCreateUpdateDeleteLeavesStorageConsistent() {
        NoteRepository repository = repo();
        for (int i = 0; i < 20; i++) {
            Note created = repository.createNoteSync("n" + i, "body" + i);
            assertNotNull(created);
            assertTrue(repository.updateNoteSync(created.id, "edited", "v" + i));
            assertTrue(repository.deleteNoteSync(created.id));
        }
        assertTrue(repository.getNotesSync().isEmpty());
    }

    @Test
    public void createThenUpdate_autosaveSequence_preservesSingleNote() {
        // The persistence sequence autosave produces for a brand-new note: a
        // first create, then an update with newer content. Exactly one note
        // must remain, keeping its identity and creation time.
        NoteRepository repository = repo();
        Note created = repository.createNoteSync("First", "draft");
        assertNotNull(created);
        assertTrue(repository.updateNoteSync(created.id, "First", "final"));
        List<Note> notes = repository.getNotesSync();
        assertEquals(1, notes.size());
        Note saved = notes.get(0);
        assertEquals(created.id, saved.id);
        assertEquals(created.createdAt, saved.createdAt);
        assertEquals("final", saved.content);
    }

    @Test
    public void serializedSaves_newerContentCannotBeOverwrittenByOlder() throws Exception {
        // Mimics the autosave pipeline: successive snapshots are applied
        // through a single worker (exactly like AppExecutors' single-threaded
        // disk executor), so an older snapshot can never land after a newer
        // one, and no update is lost.
        NoteRepository repository = repo();
        Note created = repository.createNoteSync("base", "base");
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            final CountDownLatch done = new CountDownLatch(2);
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    repository.updateNoteSync(created.id, "base", "older");
                    done.countDown();
                }
            });
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    repository.updateNoteSync(created.id, "base", "newer");
                    done.countDown();
                }
            });
            assertTrue(done.await(3, TimeUnit.SECONDS));
        } finally {
            worker.shutdownNow();
        }
        Note finalNote = repository.getNoteByIdSync(created.id);
        assertEquals("newer", finalNote.content);
        assertEquals(1, repository.getNotesSync().size());
    }

    @Test
    public void asyncUpdateQueue_latestContentWins() throws Exception {
        // Through the real async API, saves are serialized on the single disk
        // worker; the newest queued save must be the final persisted state and
        // must not create duplicates.
        NoteRepository repository = repo();
        Note created = repository.createNoteSync("t", "v0");
        repository.updateNote(created.id, "t", "v1", new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean ok) {
            }
        });
        repository.updateNote(created.id, "t", "v2", new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean ok) {
            }
        });
        long deadline = System.currentTimeMillis() + 3000;
        Note latest = null;
        while (System.currentTimeMillis() < deadline) {
            Note note = repository.getNoteByIdSync(created.id);
            if (note != null && "v2".equals(note.content)) {
                latest = note;
                break;
            }
            Thread.sleep(10);
        }
        assertNotNull(latest);
        assertEquals("v2", latest.content);
        assertEquals(1, repository.getNotesSync().size());
    }
}
