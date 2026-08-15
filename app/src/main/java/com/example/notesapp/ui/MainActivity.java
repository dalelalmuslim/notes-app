package com.example.notesapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.notesapp.R;
import com.example.notesapp.data.NoteRepository;
import com.example.notesapp.model.Note;

import java.util.List;

public final class MainActivity extends Activity {

    private NoteRepository repository;
    private NotesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new NoteRepository(this);
        adapter = new NotesAdapter(this);

        ListView listView = findViewById(R.id.notes_list);
        listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.empty_state));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = (Note) parent.getItemAtPosition(position);
                NoteEditorActivity.openForEdit(MainActivity.this, note.id);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = (Note) parent.getItemAtPosition(position);
                confirmDelete(note);
                return true;
            }
        });

        ImageButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteEditorActivity.openForCreate(MainActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Note> notes = repository.getNotes();
        adapter.setNotes(notes);
    }

    private void confirmDelete(final Note note) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.delete_confirm_message))
                .setPositiveButton(R.string.delete, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        deleteNote(note);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteNote(Note note) {
        if (!repository.deleteNote(note.id)) {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        adapter.setNotes(repository.getNotes());
    }
}
