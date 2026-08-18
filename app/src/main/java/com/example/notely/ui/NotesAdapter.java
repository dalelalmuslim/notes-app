package com.example.notely.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notely.R;
import com.example.notely.model.Note;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView adapter for the note list.
 *
 * Click/long-click listeners are attached once in {@link #onCreateViewHolder}
 * and read the adapter position at click time, so no per-bind allocations
 * happen and recycled views can never carry stale callbacks.
 */
public final class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);
    }

    private final LayoutInflater inflater;
    private final DateFormat dateFormat;
    private final OnNoteClickListener listener;
    private final List<Note> notes = new ArrayList<Note>();

    public NotesAdapter(Context context, OnNoteClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        this.listener = listener;
    }

    public void setNotes(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_note, parent, false);
        final NoteViewHolder holder = new NoteViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNoteClick(notes.get(position));
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNoteLongClick(notes.get(position));
                    return true;
                }
                return false;
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);

        holder.itemView.setBackgroundResource(NoteColorResources.cardBackground(note.color));

        holder.pinView.setVisibility(note.isPinned ? View.VISIBLE : View.GONE);

        if (note.title.isEmpty()) {
            holder.titleView.setVisibility(View.GONE);
        } else {
            holder.titleView.setVisibility(View.VISIBLE);
            holder.titleView.setText(note.title);
        }

        if (note.content.isEmpty()) {
            holder.previewView.setVisibility(View.GONE);
        } else {
            holder.previewView.setVisibility(View.VISIBLE);
            holder.previewView.setText(note.content);
        }

        holder.dateView.setText(dateFormat.format(new Date(note.updatedAt)));
    }

    static final class NoteViewHolder extends RecyclerView.ViewHolder {
        final ImageView pinView;
        final TextView titleView;
        final TextView previewView;
        final TextView dateView;

        NoteViewHolder(View itemView) {
            super(itemView);
            pinView = itemView.findViewById(R.id.note_pin);
            titleView = itemView.findViewById(R.id.note_title);
            previewView = itemView.findViewById(R.id.note_preview);
            dateView = itemView.findViewById(R.id.note_date);
        }
    }
}
