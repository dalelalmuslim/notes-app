package com.example.notesapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.notesapp.R;
import com.example.notesapp.data.NoteRepository;
import com.example.notesapp.model.Note;

public final class NoteEditorActivity extends Activity {

    private static final String EXTRA_NOTE_ID = "note_id";

    private NoteRepository repository;
    private EditText titleInput;
    private EditText contentInput;
    private String noteId;

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
        Note note = repository.getNoteById(noteId);
        if (note == null) {
            Toast.makeText(this, R.string.note_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        titleInput.setText(note.title);
        contentInput.setText(note.content);
    }

    private void saveNote() {
        String title = titleInput.getText().toString();
        String content = contentInput.getText().toString();

        if (title.trim().isEmpty() && content.trim().isEmpty()) {
            Toast.makeText(this, R.string.empty_note_rejected, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved;
        if (noteId == null) {
            Note created = repository.createNote(title, content);
            saved = created != null;
        } else {
            saved = repository.updateNote(noteId, title, content);
        }

        if (!saved) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        finish();
    }
}
