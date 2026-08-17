package com.example.notely.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notely.R;
import com.example.notely.data.NoteRepository;
import com.example.notely.data.ResultCallback;
import com.example.notely.model.Note;
import com.example.notely.util.TextLimits;

public final class NoteEditorActivity extends BaseActivity {

    private static final String EXTRA_NOTE_ID = "note_id";

    /**
     * Debounce window for background autosaves. Typing restarts the timer, so
     * a burst of keystrokes produces a single persistence attempt shortly after
     * the user pauses. Delayed saves are never the only safety net: leaving the
     * editor triggers a deterministic final save regardless of this timer.
     */
    private static final long AUTOSAVE_DEBOUNCE_MS = 400L;

    /**
     * Consecutive autosave failures after which the editor stops retrying in
     * the background. The content stays in the editor and the explicit Save
     * button (or a later edit) can retry; this bounds disk churn on a broken
     * filesystem.
     */
    private static final int MAX_AUTOSAVE_RETRIES = 3;

    private NoteRepository repository;
    private EditText titleInput;
    private EditText contentInput;
    private Handler autosaveHandler;

    private final Runnable autosaveRunnable = new Runnable() {
        @Override
        public void run() {
            flush();
        }
    };

    private String noteId;
    /** True while a repository save is still in flight; at most one at a time. */
    private boolean saving;
    /** True when the editor content differs from the last confirmed save. */
    private boolean dirty;
    /** True while the activity is paused; used to flush immediately after an in-flight save. */
    private boolean paused;
    /** Set when the user asked to leave; the editor finishes only after the latest content is saved. */
    private boolean finishRequested;
    private int autosaveFailures;

    public static void openForCreate(Context context) {
        context.startActivity(new Intent(context, NoteEditorActivity.class));
    }

    public static void openForEdit(Context context, String noteId) {
        Intent intent = new Intent(context, NoteEditorActivity.class);
        intent.putExtra(EXTRA_NOTE_ID, noteId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        repository = new NoteRepository(this);
        autosaveHandler = new Handler(Looper.getMainLooper());
        titleInput = findViewById(R.id.editor_title);
        contentInput = findViewById(R.id.editor_content);

        noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        // Restore the id of a note that was created on this screen before an
        // activity recreation (e.g. rotation), so the reopened editor shows the
        // saved note instead of a blank new one.
        if (noteId == null && savedInstanceState != null) {
            noteId = savedInstanceState.getString(EXTRA_NOTE_ID);
        }
        int titleRes = noteId == null ? R.string.new_note : R.string.edit_note;
        setTitle(titleRes);
        ((TextView) findViewById(R.id.header_title)).setText(titleRes);

        findViewById(R.id.header_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        findViewById(R.id.editor_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        if (noteId == null) {
            attachEditorWatchers();
            titleInput.requestFocus();
        } else {
            loadExistingNote();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (noteId != null) {
            outState.putString(EXTRA_NOTE_ID, noteId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
        // Deterministic final-save path: whatever the debounce timer was going
        // to do, the latest content must reach persistence while leaving.
        flushNow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autosaveHandler.removeCallbacks(autosaveRunnable);
    }

    private void loadExistingNote() {
        repository.getNoteById(noteId, new ResultCallback<Note>() {
            @Override
            public void onResult(Note note) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (note == null) {
                    Toast.makeText(NoteEditorActivity.this, R.string.note_not_found,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                titleInput.setText(note.title);
                contentInput.setText(note.content);
                // Watchers attach only after the stored values are shown so a
                // legacy note that already exceeds a limit is never silently
                // truncated; the limit applies to new input only. They also
                // must not fire while the stored values are loaded, otherwise a
                // fresh screen would immediately autosave untouched content.
                attachEditorWatchers();
                contentInput.requestFocus();
            }
        });
    }

    private void attachEditorWatchers() {
        titleInput.addTextChangedListener(limitWatcher(
                titleInput, TextLimits.MAX_TITLE_LENGTH, R.string.title_limit_reached));
        contentInput.addTextChangedListener(limitWatcher(
                contentInput, TextLimits.MAX_CONTENT_LENGTH, R.string.content_limit_reached));
        titleInput.addTextChangedListener(autosaveWatcher());
        contentInput.addTextChangedListener(autosaveWatcher());
    }

    private TextWatcher autosaveWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scheduleAutosave();
            }
        };
    }

    private TextWatcher limitWatcher(final EditText input, final int max,
                                     final int messageRes) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextLimits.codePointCount(s.toString()) <= max) {
                    return;
                }
                int cursor = Selection.getSelectionStart(s);
                String truncated = TextLimits.truncate(s.toString(), max);
                s.replace(0, s.length(), truncated);
                int position = cursor < 0 ? truncated.length() : Math.min(cursor, truncated.length());
                input.setSelection(position);
                Toast.makeText(NoteEditorActivity.this, messageRes, Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public void onBackPressed() {
        // Content is autosaved, so leaving never discards anything: persist the
        // latest content and finish once it is confirmed on disk.
        finishRequested = true;
        flushNow();
    }

    private void saveNote() {
        final String title = titleInput.getText().toString();
        final String content = contentInput.getText().toString();

        if (title.trim().isEmpty() && content.trim().isEmpty()) {
            Toast.makeText(this, R.string.empty_note_rejected, Toast.LENGTH_SHORT).show();
            return;
        }
        finishRequested = true;
        flushNow();
    }

    /**
     * Records a content change and schedules a debounced save. Rapid typing
     * restarts the timer, so a typing burst yields a single persistence
     * attempt shortly after the user pauses.
     */
    private void scheduleAutosave() {
        dirty = true;
        autosaveHandler.removeCallbacks(autosaveRunnable);
        autosaveHandler.postDelayed(autosaveRunnable, AUTOSAVE_DEBOUNCE_MS);
    }

    /**
     * Starts the final save immediately, cancelling any pending debounce.
     * Called by the back button, the Save button, and onPause so leaving the
     * editor always persists the latest content instead of relying on a timer.
     */
    private void flushNow() {
        autosaveHandler.removeCallbacks(autosaveRunnable);
        if (saving) {
            // A save is in flight; its completion handler re-saves newer
            // content, so there is nothing to do here.
            return;
        }
        if (!dirty) {
            if (finishRequested) {
                finish();
            }
            return;
        }
        flush();
    }

    /**
     * Persists the current content if needed. Only one save is ever in flight;
     * a newer change made while saving is picked up by the completion handler,
     * so an older save can never overwrite a newer one.
     */
    private void flush() {
        if (saving || !dirty) {
            return;
        }
        final String title = titleInput.getText().toString();
        final String content = contentInput.getText().toString();

        // Mirror the explicit-save semantics: an empty note is never created.
        // If the note is brand new, nothing meaningful has been entered, so
        // nothing is persisted. (An emptied existing note keeps its previous
        // content, matching the app rule that notes are never empty.)
        if (title.trim().isEmpty() && content.trim().isEmpty()) {
            dirty = false;
            if (finishRequested) {
                finish();
            }
            return;
        }

        startSave(title, content);
    }

    private void startSave(final String title, final String content) {
        saving = true;
        if (noteId == null) {
            repository.createNote(title, content, new ResultCallback<Note>() {
                @Override
                public void onResult(Note created) {
                    if (created != null) {
                        noteId = created.id;
                    }
                    onSaveComplete(title, content, created != null);
                }
            });
        } else {
            repository.updateNote(noteId, title, content, new ResultCallback<Boolean>() {
                @Override
                public void onResult(Boolean ok) {
                    onSaveComplete(title, content, Boolean.TRUE.equals(ok));
                }
            });
        }
    }

    /**
     * Handles the outcome of the save that wrote {@code savedTitle}/
     * {@code savedContent}. If the editor content advanced while the save was
     * in flight, another save with the latest content is started immediately
     * (or after the debounce while the user is still typing). Leaving the
     * editor is deferred until the latest content is confirmed on disk.
     */
    private void onSaveComplete(String savedTitle, String savedContent, boolean success) {
        saving = false;
        if (!success) {
            // Keep the content in the editor and retry; it is never silently
            // dropped. The explicit Save button reports the failure.
            dirty = true;
            autosaveFailures++;
            if (autosaveFailures > MAX_AUTOSAVE_RETRIES) {
                return;
            }
            if (finishRequested && !isFinishing() && !isDestroyed()) {
                Toast.makeText(NoteEditorActivity.this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            }
            scheduleAutosave();
            return;
        }
        autosaveFailures = 0;

        String currentTitle = titleInput.getText().toString();
        String currentContent = contentInput.getText().toString();
        if (currentTitle.equals(savedTitle) && currentContent.equals(savedContent)) {
            dirty = false;
            if (finishRequested) {
                finish();
            }
        } else {
            dirty = true;
            if (finishRequested || paused) {
                flush();
            } else {
                scheduleAutosave();
            }
        }
    }
}
