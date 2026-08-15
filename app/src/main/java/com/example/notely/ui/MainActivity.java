package com.example.notely.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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

public final class MainActivity extends Activity {

    private static final int MAX_RELEASE_NOTES_LENGTH = 400;

    private NoteRepository repository;
    private NotesAdapter adapter;
    private RecyclerView list;
    private LinearLayout emptyState;
    private UpdateChecker updateChecker;
    private boolean updateDialogShown;
    private String currentVersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new NoteRepository(this);
        updateChecker = new UpdateChecker(this);
        currentVersionName = versionName();

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
                showUpdateDialog(info);
            }
        });
    }

    private void showUpdateDialog(UpdateInfo info) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.update_title)
                .setMessage(buildUpdateMessage(info))
                .setPositiveButton(R.string.update_action, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        openUpdateDestination(info);
                    }
                })
                .setNegativeButton(R.string.update_later, null)
                .show();
    }

    private String buildUpdateMessage(UpdateInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.update_current_version,
                currentVersionName == null ? "?" : currentVersionName));
        sb.append('\n');
        sb.append(getString(R.string.update_latest_version, info.latestVersion.toString()));
        if (info.releaseNotes != null && !info.releaseNotes.trim().isEmpty()) {
            String notes = info.releaseNotes.trim();
            if (notes.length() > MAX_RELEASE_NOTES_LENGTH) {
                notes = notes.substring(0, MAX_RELEASE_NOTES_LENGTH) + "…";
            }
            sb.append("\n\n");
            sb.append(getString(R.string.update_release_notes, notes));
        }
        return sb.toString();
    }

    private void openUpdateDestination(UpdateInfo info) {
        String url = info.updateUrl != null ? info.updateUrl : info.apkUrl;
        if (url == null) {
            Toast.makeText(this, R.string.update_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.update_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return null;
        }
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
