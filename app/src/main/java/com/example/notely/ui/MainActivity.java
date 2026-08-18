package com.example.notely.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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

    /**
     * Debounce for the search field so a burst of keystrokes triggers a single
     * in-memory filter instead of a disk read per character.
     */
    private static final long SEARCH_DEBOUNCE_MS = 250L;

    private NoteRepository repository;
    private NotesAdapter adapter;
    private RecyclerView list;
    private LinearLayout emptyState;
    private LinearLayout searchEmptyState;
    private EditText searchInput;
    private ImageButton searchClear;
    private Handler searchHandler;
    private UpdateChecker updateChecker;
    private boolean updateDialogShown;

    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            refreshNotes();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new NoteRepository(this);
        updateChecker = new UpdateChecker(this);
        searchHandler = new Handler(Looper.getMainLooper());

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
        searchEmptyState = findViewById(R.id.search_empty_state);
        findViewById(R.id.empty_state_new_note).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteEditorActivity.openForCreate(MainActivity.this);
            }
        });

        initSearch();

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    private void initSearch() {
        searchInput = findViewById(R.id.search_input);
        searchClear = findViewById(R.id.search_clear);
        searchClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInput.setText("");
                refreshNotes();
            }
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean hasQuery = s.length() > 0;
                searchClear.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void refreshNotes() {
        String query = searchInput == null ? "" : searchInput.getText().toString();
        ResultCallback<List<Note>> callback = new ResultCallback<List<Note>>() {
            @Override
            public void onResult(List<Note> notes) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showNotes(notes, query);
            }
        };
        if (query.trim().isEmpty()) {
            repository.getActiveNotes(callback);
        } else {
            repository.searchNotes(query, callback);
        }
    }

    private void showNotes(List<Note> notes, String query) {
        adapter.setNotes(notes);
        boolean searching = !query.trim().isEmpty();
        boolean anyNotes = !notes.isEmpty();
        list.setVisibility(anyNotes ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(!anyNotes && !searching ? View.VISIBLE : View.GONE);
        searchEmptyState.setVisibility(!anyNotes && searching ? View.VISIBLE : View.GONE);
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
                .setMessage(R.string.move_to_trash_message)
                .setPositiveButton(R.string.delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                moveToTrash(note);
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

    private void moveToTrash(final Note note) {
        repository.softDeleteNote(note.id, new ResultCallback<Boolean>() {
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