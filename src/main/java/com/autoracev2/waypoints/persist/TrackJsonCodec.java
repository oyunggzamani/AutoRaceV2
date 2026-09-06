package com.autoracev2.waypoints.persist;

import com.autoracev2.waypoints.api.WorldPosition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TrackJsonCodec {
    static final int SCHEMA_VERSION = 1;

    private TrackJsonCodec() {
    }

    static String writeWaypoints(WaypointDocument document) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray array = new JsonArray();
        for (IndexedPosition entry : document.entries()) {
            array.add(PositionJson.toJson(entry.id(), entry.position()));
        }
        root.add("waypoints", array);
        return PrettyJson.toPretty(root);
    }

    static String writeCheckpoints(CheckpointDocument document) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        if (document.start() == null) {
            root.add("start", null);
        } else {
            root.add("start", PositionJson.toJson(document.start()));
        }
        if (document.finish() == null) {
            root.add("finish", null);
        } else {
            root.add("finish", PositionJson.toJson(document.finish()));
        }
        JsonArray array = new JsonArray();
        for (IndexedPosition entry : document.entries()) {
            array.add(PositionJson.toJson(entry.id(), entry.position()));
        }
        root.add("checkpoints", array);
        return PrettyJson.toPretty(root);
    }

    static ParsedDocument<WaypointDocument> readWaypoints(String raw) {
        JsonObject root = parseRoot(raw);
        if (root == null) {
            if (raw == null || raw.isBlank()) {
                return ParsedDocument.empty("empty waypoint file");
            }
            return ParsedDocument.corrupt("waypoint JSON is not a valid object");
        }
        List<String> warnings = new ArrayList<>();
        List<IndexedPosition> entries = readIndexedArray(root, "waypoints", warnings);
        return ParsedDocument.value(new WaypointDocument(entries), warnings);
    }

    static ParsedDocument<CheckpointDocument> readCheckpoints(String raw) {
        JsonObject root = parseRoot(raw);
        if (root == null) {
            if (raw == null || raw.isBlank()) {
                return ParsedDocument.empty("empty checkpoint file");
            }
            return ParsedDocument.corrupt("checkpoint JSON is not a valid object");
        }
        List<String> warnings = new ArrayList<>();
        List<IndexedPosition> entries = readIndexedArray(root, "checkpoints", warnings);
        WorldPosition start = readOptionalPosition(root, "start", warnings);
        WorldPosition finish = readOptionalPosition(root, "finish", warnings);
        return ParsedDocument.value(new CheckpointDocument(entries, start, finish), warnings);
    }

    private static List<IndexedPosition> readIndexedArray(JsonObject root, String field, List<String> warnings) {
        List<IndexedPosition> entries = new ArrayList<>();
        if (!root.has(field) || root.get(field).isJsonNull()) {
            return entries;
        }
        JsonElement element = root.get(field);
        if (!element.isJsonArray()) {
            warnings.add(field + " is not an array; ignored");
            return entries;
        }
        JsonArray array = element.getAsJsonArray();
        int fallbackId = 1;
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            StringBuilder error = new StringBuilder();
            Optional<WorldPosition> position = PositionJson.fromJson(item, error);
            if (position.isEmpty()) {
                warnings.add(field + "[" + i + "] skipped: " + error);
                continue;
            }
            int id = item.isJsonObject() ? PositionJson.readId(item.getAsJsonObject(), fallbackId) : fallbackId;
            if (id < 1) {
                id = fallbackId;
            }
            entries.add(new IndexedPosition(id, position.get()));
            fallbackId++;
        }
        return entries;
    }

    private static WorldPosition readOptionalPosition(JsonObject root, String field, List<String> warnings) {
        if (!root.has(field) || root.get(field).isJsonNull()) {
            return null;
        }
        StringBuilder error = new StringBuilder();
        Optional<WorldPosition> position = PositionJson.fromJson(root.get(field), error);
        if (position.isEmpty()) {
            warnings.add(field + " skipped: " + error);
            return null;
        }
        return position.get();
    }

    private static JsonObject parseRoot(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(raw);
            if (element == null || !element.isJsonObject()) {
                return null;
            }
            return element.getAsJsonObject();
        } catch (JsonSyntaxException ignored) {
            return null;
        }
    }
}
