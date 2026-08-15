package com.example.notely.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recursive-descent JSON parser (objects, arrays, strings, numbers,
 * booleans, null). Produces a tree of {@code Map}, {@code List}, {@code String},
 * {@code Long}/{@code Double}, {@code Boolean}, or {@code null}.
 *
 * This is the parser used by {@link LocalJsonStorage}; it is shared so the
 * update checker can parse the same way without an external JSON dependency.
 * Malformed input never crashes: it throws {@link JsonParseException}.
 */
public final class Json {

    private Json() {
    }

    public static Object parse(String text) throws JsonParseException {
        if (text == null) {
            throw new JsonParseException("null input");
        }
        return new Parser(text).parseValue();
    }

    public static final class JsonParseException extends Exception {
        JsonParseException(String message) {
            super(message);
        }
    }

    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
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
}
