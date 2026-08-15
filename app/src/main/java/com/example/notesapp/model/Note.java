package com.example.notesapp.model;

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
}
