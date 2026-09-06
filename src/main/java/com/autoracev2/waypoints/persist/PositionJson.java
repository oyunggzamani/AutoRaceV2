package com.autoracev2.waypoints.persist;

import com.autoracev2.waypoints.api.WorldPosition;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;

final class PositionJson {
    private PositionJson() {
    }

    static JsonObject toJson(WorldPosition position) {
        JsonObject object = new JsonObject();
        object.addProperty("dimension", position.dimension());
        object.addProperty("x", position.x());
        object.addProperty("y", position.y());
        object.addProperty("z", position.z());
        return object;
    }

    static JsonObject toJson(int id, WorldPosition position) {
        JsonObject object = toJson(position);
        object.addProperty("id", id);
        return object;
    }

    static Optional<WorldPosition> fromJson(JsonElement element, StringBuilder error) {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        if (!element.isJsonObject()) {
            error.append("position is not an object");
            return Optional.empty();
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.has("dimension") || !object.has("x") || !object.has("y") || !object.has("z")) {
            error.append("missing dimension/x/y/z");
            return Optional.empty();
        }
        try {
            String dimension = object.get("dimension").getAsString();
            double x = object.get("x").getAsDouble();
            double y = object.get("y").getAsDouble();
            double z = object.get("z").getAsDouble();
            return Optional.of(new WorldPosition(dimension, x, y, z));
        } catch (RuntimeException exception) {
            error.append(exception.getMessage() == null ? "invalid position values" : exception.getMessage());
            return Optional.empty();
        }
    }

    static int readId(JsonObject object, int fallback) {
        if (!object.has("id") || object.get("id").isJsonNull()) {
            return fallback;
        }
        try {
            return object.get("id").getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
