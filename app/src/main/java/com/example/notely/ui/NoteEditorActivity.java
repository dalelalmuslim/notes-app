package com.example.notely.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

    private NoteRepository repository;
    private EditText titleInput;
    private EditText contentInput;
    private String noteId;
    private boolean saving;
    private String originalTitle = "";
    private String originalContent = "";

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
        titleInput = findViewById(R.id.editor_title);
        contentInput = findViewById(R.id.editor_content);

        noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
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
            attachLimitWatchers();
            titleInput.requestFocus();
        } else {
            loadExistingNote();
        }
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
                originalTitle = note.title;
                originalContent = note.content;
                titleInput.setText(note.title);
                contentInput.setText(note.content);
                // Watchers attach only after the stored values are shown so a
                // legacy note that already exceeds a limit is never silently
                // truncated; the limit applies to new input only.
                attachLimitWatchers();
                contentInput.requestFocus();
            }
        });
    }

    private void attachLimitWatchers() {
        titleInput.addTextChangedListener(limitWatcher(
                titleInput, TextLimits.MAX_TITLE_LENGTH, R.string.title_limit_reached));
        contentInput.addTextChangedListener(limitWatcher(
                contentInput, TextLimits.MAX_CONTENT_LENGTH, R.string.content_limit_reached));
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

    private boolean hasUnsavedChanges() {
        String title = titleInput.getText().toString();
        String content = contentInput.getText().toString();
        if (noteId == null) {
            return !title.isEmpty() || !content.isEmpty();
        }
        return !title.equals(originalTitle) || !content.equals(originalContent);
    }

    @Override
    public void onBackPressed() {
        if (saving) {
            return;
        }
        if (!hasUnsavedChanges()) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.unsaved_changes_title)
                .setMessage(R.string.unsaved_changes_message)
                .setPositiveButton(R.string.discard,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveNote() {
        if (saving) {
            return;
        }
        final String title = titleInput.getText().toString();
        final String content = contentInput.getText().toString();

        if (title.trim().isEmpty() && content.trim().isEmpty()) {
            Toast.makeText(this, R.string.empty_note_rejected, Toast.LENGTH_SHORT).show();
            return;
        }

        saving = true;
        if (noteId == null) {
            repository.createNote(title, content, new ResultCallback<Note>() {
                @Override
                public void onResult(Note created) {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    saving = false;
                    if (created == null) {
                        Toast.makeText(NoteEditorActivity.this, R.string.save_failed,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    finish();
                }
            });
        } else {
            repository.updateNote(noteId, title, content, new ResultCallback<Boolean>() {
                @Override
                public void onResult(Boolean ok) {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    saving = false;
                    if (!ok) {
                        Toast.makeText(NoteEditorActivity.this, R.string.save_failed,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    finish();
                }
            });
        }
    }
}
