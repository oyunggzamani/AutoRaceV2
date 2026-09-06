package com.autoracev2.waypoints.persist;

import java.util.ArrayList;
import java.util.List;

final class ParsedDocument<T> {
    enum Kind {
        VALUE,
        EMPTY,
        CORRUPT
    }

    private final Kind kind;
    private final T value;
    private final List<String> warnings;

    private ParsedDocument(Kind kind, T value, List<String> warnings) {
        this.kind = kind;
        this.value = value;
        this.warnings = warnings;
    }

    static <T> ParsedDocument<T> value(T value, List<String> warnings) {
        return new ParsedDocument<>(Kind.VALUE, value, warnings);
    }

    static <T> ParsedDocument<T> empty(String warning) {
        return new ParsedDocument<>(Kind.EMPTY, null, warning == null ? List.of() : List.of(warning));
    }

    static <T> ParsedDocument<T> corrupt(String warning) {
        return new ParsedDocument<>(Kind.CORRUPT, null, List.of(warning));
    }

    Kind kind() {
        return kind;
    }

    T value() {
        return value;
    }

    List<String> warnings() {
        return warnings == null ? List.of() : warnings;
    }

    List<String> mutableWarnings() {
        return new ArrayList<>(warnings());
    }
}
