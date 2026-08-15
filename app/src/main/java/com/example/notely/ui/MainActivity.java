package com.example.notely.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notely.R;
import com.example.notely.data.NoteRepository;
import com.example.notely.data.ResultCallback;
import com.example.notely.data.UpdateChecker;
import com.example.notely.data.UpdateInfo;
import com.example.notely.model.Note;

import java.util.List;

public final class MainActivity extends BaseActivity {

    private NoteRepository repository;
    private NotesAdapter adapter;
    private RecyclerView list;
    private LinearLayout emptyState;
    private UpdateChecker updateChecker;
    private boolean updateDialogShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new NoteRepository(this);
        updateChecker = new UpdateChecker(this);

        list = findViewById(R.id.notes_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotesAdapter(this, new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                NoteEditorActivity.openForEdit(MainActivity.this, note.id);
            }

            @Override
            public void onNoteLongClick(Note note) {
                confirmDelete(note);
            }
        });
        list.setAdapter(adapter);

        emptyState = findViewById(R.id.empty_state);
        findViewById(R.id.empty_state_new_note).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteEditorActivity.openForCreate(MainActivity.this);
            }
        });

        ImageButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteEditorActivity.openForCreate(MainActivity.this);
            }
        });

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotes();
        maybeCheckForUpdates();
    }

    private void refreshNotes() {
        repository.getNotes(new ResultCallback<List<Note>>() {
            @Override
            public void onResult(List<Note> notes) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                adapter.setNotes(notes);
                boolean empty = notes.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void maybeCheckForUpdates() {
        if (updateDialogShown) {
            return;
        }
        updateChecker.check(new ResultCallback<UpdateInfo>() {
            @Override
            public void onResult(UpdateInfo info) {
                if (isFinishing() || isDestroyed() || updateDialogShown) {
                    return;
                }
                if (info == null || !info.available) {
                    return;
                }
                updateDialogShown = true;
                UpdatePrompts.showAvailable(MainActivity.this, info);
            }
        });
    }

    private void confirmDelete(final Note note) {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                deleteNote(note);
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        ThemeUtils.resolveColor(MainActivity.this, R.attr.notelyDanger));
            }
        });
        dialog.show();
    }

    private void deleteNote(final Note note) {
        repository.deleteNote(note.id, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!success) {
                    Toast.makeText(MainActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                refreshNotes();
            }
        });
    }
}
