package com.example.notesapp.data;

import android.util.Log;

import com.example.notesapp.model.Note;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns all access to the local JSON file.
 *
 * The UI layer must never touch this class directly; it is reached through
 * {@link NoteRepository}. Note contents are never logged.
 */
public final class LocalJsonStorage {

    private static final String TAG = "LocalJsonStorage";
    private static final String FILE_NAME = "notes.json";
    private static final String TEMP_FILE_NAME = "notes.json.tmp";
    private static final String CORRUPT_FILE_PREFIX = "notes.json.corrupt-";

    private final File directory;
    private final File targetFile;
    private final File tempFile;

    public LocalJsonStorage(File directory) {
        this.directory = directory;
        this.targetFile = new File(directory, FILE_NAME);
        this.tempFile = new File(directory, TEMP_FILE_NAME);
    }

    /**
     * Reads all notes.
     *
     * - Missing file is treated as an empty list.
     * - Blank file is treated as an empty list.
     * - Malformed JSON never crashes the app; the unreadable file is preserved
     *   as a backup under a distinct name and an empty list is returned.
     *   Valid data is never silently overwritten by a failed read.
     */
    public List<Note> readAll() {
        if (!targetFile.exists()) {
            return new ArrayList<Note>();
        }

        String text = readText(targetFile);
        if (text == null) {
            return new ArrayList<Note>();
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<Note>();
        }

        try {
            return parseNotes(trimmed);
        } catch (JsonParseException e) {
            Log.w(TAG, "notes.json could not be parsed; preserving file as backup");
            preserveUnreadableFile();
            return new ArrayList<Note>();
        }
    }

    /**
     * Persists the complete new state using a write-then-rename approach:
     * the full state is serialized, written to a temporary file, and the old
     * file is replaced only after the temporary write fully succeeds.
     *
     * Returns true only when persistence actually succeeded.
     */
    public boolean writeAll(List<Note> notes) {
        String json = serializeNotes(notes);
        if (!writeText(tempFile, json)) {
            return false;
        }
        boolean replaced = tempFile.renameTo(targetFile);
        if (!replaced) {
            Log.w(TAG, "Failed to replace notes.json");
            tempFile.delete();
        }
        return replaced;
    }

    private void preserveUnreadableFile() {
        File backup = new File(directory, CORRUPT_FILE_PREFIX + System.currentTimeMillis());
        if (targetFile.renameTo(backup)) {
            Log.w(TAG, "Unreadable notes.json preserved as " + backup.getName());
        } else {
            Log.w(TAG, "Could not preserve unreadable notes.json");
        }
    }

    private static String readText(File file) {
        try {
            InputStream in = new FileInputStream(file);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not read notes.json");
            return null;
        }
    }

    private static boolean writeText(File file, String text) {
        try {
            Writer writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8);
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Could not write " + file.getName());
            file.delete();
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    private static String serializeNotes(List<Note> notes) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < notes.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Note note = notes.get(i);
            sb.append('{');
            sb.append("\"id\":");
            appendString(sb, note.id);
            sb.append(',');
            sb.append("\"title\":");
            appendString(sb, note.title);
            sb.append(',');
            sb.append("\"content\":");
            appendString(sb, note.content);
            sb.append(',');
            sb.append("\"createdAt\":");
            sb.append(note.createdAt);
            sb.append(',');
            sb.append("\"updatedAt\":");
            sb.append(note.updatedAt);
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private static void appendString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int pad = hex.length(); pad < 4; pad++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        // Non-ASCII characters (e.g. Arabic) pass through
                        // unescaped; the file is always encoded as UTF-8.
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    private static List<Note> parseNotes(String text) throws JsonParseException {
        JsonParser parser = new JsonParser(text);
        Object value = parser.parseValue();
        if (!(value instanceof List)) {
            throw new JsonParseException("root value is not an array");
        }

        List<Note> notes = new ArrayList<Note>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                Note note = noteFromMap((Map<?, ?>) item);
                if (note != null) {
                    notes.add(note);
                }
            }
        }
        return notes;
    }

    @SuppressWarnings("unchecked")
    private static Note noteFromMap(Map<?, ?> map) {
        Object id = map.get("id");
        Object title = map.get("title");
        Object content = map.get("content");
        Object createdAt = map.get("createdAt");
        Object updatedAt = map.get("updatedAt");

        if (!(id instanceof String)
                || !(title instanceof String)
                || !(content instanceof String)
                || !(createdAt instanceof Long)
                || !(updatedAt instanceof Long)) {
            Log.w(TAG, "Skipping malformed note entry in notes.json");
            return null;
        }
        return new Note((String) id, (String) title, (String) content,
                (Long) createdAt, (Long) updatedAt);
    }

    /** Minimal recursive-descent JSON parser (objects, arrays, strings, numbers). */
    private static final class JsonParser {
        private final String src;
        private int pos;

        JsonParser(String src) {
            this.src = src;
        }

        Object parseValue() throws JsonParseException {
            skipWhitespace();
            if (pos >= src.length()) {
                throw new JsonParseException("unexpected end of input");
            }
            char c = src.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                default:
                    return parseNumberOrLiteral();
            }
        }

        private Map<String, Object> parseObject() throws JsonParseException {
            Map<String, Object> map = new HashMap<String, Object>();
            pos++;
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw new JsonParseException("expected string key");
                }
                String key = parseString();
                skipWhitespace();
                if (peek() != ':') {
                    throw new JsonParseException("expected ':'");
                }
                pos++;
                map.put(key, parseValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    return map;
                } else {
                    throw new JsonParseException("expected ',' or '}'");
                }
            }
        }

        private List<Object> parseArray() throws JsonParseException {
            List<Object> list = new ArrayList<Object>();
            pos++;
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    return list;
                } else {
                    throw new JsonParseException("expected ',' or ']'");
                }
            }
        }

        private String parseString() throws JsonParseException {
            if (peek() != '"') {
                throw new JsonParseException("expected string");
            }
            pos++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= src.length()) {
                    throw new JsonParseException("unterminated string");
                }
                char c = src.charAt(pos);
                if (c == '"') {
                    pos++;
                    return sb.toString();
                }
                if (c == '\\') {
                    pos++;
                    if (pos >= src.length()) {
                        throw new JsonParseException("unterminated escape");
                    }
                    char esc = src.charAt(pos);
                    switch (esc) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(esc);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (pos + 4 >= src.length()) {
                                throw new JsonParseException("invalid \\u escape");
                            }
                            String hex = src.substring(pos + 1, pos + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                throw new JsonParseException("invalid \\u escape");
                            }
                            pos += 4;
                            break;
                        default:
                            throw new JsonParseException("invalid escape");
                    }
                    pos++;
                } else {
                    sb.append(c);
                    pos++;
                }
            }
        }

        private Object parseNumberOrLiteral() throws JsonParseException {
            char c = peek();
            if (c == 't') {
                expectLiteral("true");
                return Boolean.TRUE;
            }
            if (c == 'f') {
                expectLiteral("false");
                return Boolean.FALSE;
            }
            if (c == 'n') {
                expectLiteral("null");
                return null;
            }
            if (c == '-' || (c >= '0' && c <= '9')) {
                int start = pos;
                if (c == '-') {
                    pos++;
                }
                boolean isDouble = false;
                while (pos < src.length()) {
                    char d = src.charAt(pos);
                    if (d >= '0' && d <= '9') {
                        pos++;
                    } else if (d == '.' || d == 'e' || d == 'E' || d == '+' || d == '-') {
                        isDouble = true;
                        pos++;
                    } else {
                        break;
                    }
                }
                String number = src.substring(start, pos);
                try {
                    if (isDouble) {
                        return Double.parseDouble(number);
                    }
                    return Long.parseLong(number);
                } catch (NumberFormatException e) {
                    throw new JsonParseException("invalid number");
                }
            }
            throw new JsonParseException("unexpected character");
        }

        private void expectLiteral(String literal) throws JsonParseException {
            if (pos + literal.length() > src.length()
                    || !src.regionMatches(pos, literal, 0, literal.length())) {
                throw new JsonParseException("invalid literal");
            }
            pos += literal.length();
        }

        private char peek() throws JsonParseException {
            if (pos >= src.length()) {
                throw new JsonParseException("unexpected end of input");
            }
            return src.charAt(pos);
        }

        private void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }
    }

    private static final class JsonParseException extends Exception {
        JsonParseException(String message) {
            super(message);
        }
    }
}
