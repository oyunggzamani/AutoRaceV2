package com.autoracev2.waypoints.persist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LoadReport {
    public enum Status {
        LOADED,
        CREATED,
        EMPTY,
        CORRUPT,
        IO_ERROR
    }

    private final Status status;
    private final String fileName;
    private final List<String> details;

    public LoadReport(Status status, String fileName, List<String> details) {
        this.status = status;
        this.fileName = fileName;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public static LoadReport of(Status status, String fileName, String... details) {
        List<String> list = new ArrayList<>();
        if (details != null) {
            Collections.addAll(list, details);
        }
        return new LoadReport(status, fileName, list);
    }

    public Status status() {
        return status;
    }

    public String fileName() {
        return fileName;
    }

    public List<String> details() {
        return details;
    }

    public boolean usedFallback() {
        return status == Status.CORRUPT || status == Status.IO_ERROR || status == Status.EMPTY || status == Status.CREATED;
    }
}
