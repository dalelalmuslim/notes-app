package com.example.notely.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.notely.R;
import com.example.notely.data.NoteRepository;
import com.example.notely.data.ResultCallback;
import com.example.notely.model.Note;

public final class NoteEditorActivity extends Activity {

    private static final String EXTRA_NOTE_ID = "note_id";

    private NoteRepository repository;
    private EditText titleInput;
    private EditText contentInput;
    private String noteId;
    private boolean saving;

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
        if (noteId != null) {
            loadExistingNote();
        }
        setTitle(noteId == null ? R.string.new_note : R.string.edit_note);

        findViewById(R.id.editor_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });
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
            }
        });
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
