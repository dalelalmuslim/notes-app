package com.example.notesapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.notesapp.R;
import com.example.notesapp.model.Note;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class NotesAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final DateFormat dateFormat;
    private final List<Note> notes = new ArrayList<Note>();

    public NotesAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    }

    public void setNotes(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public Note getItem(int position) {
        return notes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_note, parent, false);
            holder = new ViewHolder();
            holder.titleView = convertView.findViewById(R.id.note_title);
            holder.previewView = convertView.findViewById(R.id.note_preview);
            holder.dateView = convertView.findViewById(R.id.note_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = getItem(position);

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
        return convertView;
    }

    private static final class ViewHolder {
        TextView titleView;
        TextView previewView;
        TextView dateView;
    }
}
