package com.autoracev2.waypoints.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

final class PrettyJson {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private PrettyJson() {
    }

    static String toPretty(JsonElement element) {
        return GSON.toJson(element) + System.lineSeparator();
    }
}
