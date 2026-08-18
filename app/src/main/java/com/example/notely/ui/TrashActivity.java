package com.example.notely.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notely.R;
import com.example.notely.data.NoteRepository;
import com.example.notely.data.ResultCallback;
import com.example.notely.model.Note;

import java.util.List;

/**
 * Trash screen: shows soft-deleted notes and offers Restore and Delete forever.
 *
 * Deleting a note here is the only destructive path; it is guarded by a
 * confirmation dialog because it is not reversible.
 */
public final class TrashActivity extends BaseActivity {

    private NoteRepository repository;
    private NotesAdapter adapter;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        repository = new NoteRepository(this);

        ((TextView) findViewById(R.id.header_title)).setText(R.string.trash_title);
        findViewById(R.id.header_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        RecyclerView list = findViewById(R.id.trash_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotesAdapter(this, new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                showNoteActions(note);
            }

            @Override
            public void onNoteLongClick(Note note) {
                showNoteActions(note);
            }
        });
        list.setAdapter(adapter);

        emptyState = findViewById(R.id.trash_empty_state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        repository.getTrashedNotes(new ResultCallback<List<Note>>() {
            @Override
            public void onResult(List<Note> notes) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                adapter.setNotes(notes);
                boolean empty = notes.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                findViewById(R.id.trash_list).setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void showNoteActions(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle(note.title.isEmpty() ? note.content : note.title)
                .setItems(new String[]{getString(R.string.restore),
                                getString(R.string.delete_forever)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    restoreNote(note);
                                } else {
                                    confirmDeleteForever(note);
                                }
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restoreNote(final Note note) {
        repository.restoreNote(note.id, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!Boolean.TRUE.equals(success)) {
                    Toast.makeText(TrashActivity.this, R.string.restore_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(TrashActivity.this, R.string.note_restored,
                        Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
    }

    private void confirmDeleteForever(final Note note) {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_forever_title)
                .setMessage(R.string.delete_forever_message)
                .setPositiveButton(R.string.delete_forever,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                deleteForever(note);
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        ThemeUtils.resolveColor(TrashActivity.this, R.attr.notelyDanger));
            }
        });
        dialog.show();
    }

    private void deleteForever(final Note note) {
        repository.deleteNote(note.id, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!Boolean.TRUE.equals(success)) {
                    Toast.makeText(TrashActivity.this, R.string.delete_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(TrashActivity.this, R.string.note_deleted,
                        Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
    }
}