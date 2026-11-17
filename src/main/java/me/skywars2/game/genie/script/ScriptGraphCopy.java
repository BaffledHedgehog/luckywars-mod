package me.skywars2.game.genie.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;

/** Copies a set of roots as one graph, retaining aliases between state, frames and handlers. */
public final class ScriptGraphCopy {
    private final IdentityHashMap<JsonElement,JsonElement> copies = new IdentityHashMap<>();
    private final ArrayDeque<JsonElement> pending = new ArrayDeque<>();

    public JsonElement copy(JsonElement root) {
        JsonElement result = reference(root);
        while (!pending.isEmpty()) {
            JsonElement source = pending.removeFirst();
            JsonElement target = copies.get(source);
            if (source instanceof JsonObject object) {
                for (var entry : object.entrySet()) target.getAsJsonObject().add(entry.getKey(), reference(entry.getValue()));
            } else {
                for (var child : source.getAsJsonArray()) target.getAsJsonArray().add(reference(child));
            }
        }
        return result;
    }

    private JsonElement reference(JsonElement source) {
        if (!(source instanceof JsonObject) && !(source instanceof JsonArray)) return source;
        JsonElement known = copies.get(source);
        if (known != null) return known;
        JsonElement target = source.isJsonObject() ? new JsonObject() : new JsonArray();
        copies.put(source, target);
        pending.addLast(source);
        return target;
    }
}
