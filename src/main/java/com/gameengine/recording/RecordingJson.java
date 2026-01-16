package com.gameengine.recording;

import java.util.ArrayList;
import java.util.List;

public final class RecordingJson {
    private RecordingJson() {}

    public static String field(String json, String key) {
        // Find the start of the key
        String search = "\"" + key + "\":";
        int pos = json.indexOf(search);
        if (pos < 0) return null;

        // Move past the colon
        pos += search.length();

        // Skip whitespace after the colon
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        if (pos >= json.length()) return null;

        char startChar = json.charAt(pos);
        int end = pos;
        int depth = 0;

        if (startChar == '{' || startChar == '[') {
            // Object or array: track nesting depth
            depth = 1;
            end = pos + 1;
            char open = startChar;
            char close = (open == '{' ? '}' : ']');

            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        end++;  // Include the closing brace/bracket
                        break;
                    }
                }
                end++;
            }

            if (depth != 0) {
                // Unbalanced - malformed JSON
                return null;
            }
        } else if (startChar == '"') {
            // String value: find matching closing quote (simple, no escaped quotes in this JSON)
            end = pos + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                    end++;  // Include closing quote
                    break;
                }
                end++;
            }
        } else {
            // Primitive: number, true, false, null - stop at comma, closing brace, or space
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                    break;
                }
                end++;
            }
        }

        // Extract and trim the value substring
        String value = json.substring(pos, end).trim();
        return value;
    }

    public static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    public static double parseDouble(String s) {
        if (s == null) return 0.0;
        try { return Double.parseDouble(stripQuotes(s)); } catch (Exception e) { return 0.0; }
    }

    public static String[] splitTopLevel(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0; int start = 0;
        for (int i = 0; i < arr.length(); i++) {
            char ch = arr.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') depth--;
            else if (ch == ',' && depth == 0) {
                out.add(arr.substring(start, i));
                start = i + 1;
            }
        }
        if (start < arr.length()) out.add(arr.substring(start));
        return out.stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    public static String extractArray(String json, int startIdx) {
        int i = startIdx;
        if (i >= json.length() || json.charAt(i) != '[') return "";
        int depth = 1;
        int begin = i + 1;
        i++;
        for (; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(begin, i);
                }
            }
        }
        return "";
    }
}


