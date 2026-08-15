package com.example.notely.model;

import java.util.UUID;

public final class Note {

    public final String id;
    public final String title;
    public final String content;
    public final long createdAt;
    public final long updatedAt;

    public Note(String id, String title, String content, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Note createNew(String title, String content, long now) {
        return new Note(UUID.randomUUID().toString(), title, content, now, now);
    }

    public Note withEditedContent(String newTitle, String newContent, long now) {
        return new Note(id, newTitle, newContent, createdAt, now);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Note)) {
            return false;
        }
        Note note = (Note) o;
        return createdAt == note.createdAt
                && updatedAt == note.updatedAt
                && id.equals(note.id)
                && title.equals(note.title)
                && content.equals(note.content);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + content.hashCode();
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        result = 31 * result + (int) (updatedAt ^ (updatedAt >>> 32));
        return result;
    }
}
