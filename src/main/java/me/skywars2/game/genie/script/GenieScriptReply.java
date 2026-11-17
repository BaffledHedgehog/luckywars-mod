package me.skywars2.game.genie.script;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Set;

/** The model decides whether to grant a wish. The server checks the format and verifier witness. */
public sealed interface GenieScriptReply {
    String VERIFIER_VERSION = "luckywars-script-1.1.0";

    record Rejected() implements GenieScriptReply {}

    record Grant(String icon, String name, String description, String answer, String twist, String program)
            implements GenieScriptReply {}

    static GenieScriptReply read(String reply, Collection<JsonObject> executions, JsonObject contract) {
        JsonObject result = ScriptJson.parse(reply);
        return switch (text(result.get("decision"), 8)) {
            case "reject" -> {
                requireKeys(result, Set.of("decision"));
                yield new Rejected();
            }
            case "grant" -> readGrant(result, executions, contract);
            default -> throw new IllegalArgumentException("unknown wish decision");
        };
    }

    private static Grant readGrant(JsonObject result, Collection<JsonObject> executions, JsonObject contract) {
        requireKeys(result, Set.of("decision", "item", "answer", "twist", "program", "receipt"));
        JsonObject item = object(result.get("item"));
        requireKeys(item, Set.of("icon", "name", "description"));
        JsonObject program = object(result.get("program"));
        JsonObject receipt = object(result.get("receipt"));
        requireKeys(receipt, Set.of("ok", "verifier", "program_sha256", "contract_sha256", "functions", "instructions"));
        var functions = object(program.get("functions"));
        int instructions = 0;
        for (var function : functions.entrySet()) {
            JsonElement code = object(function.getValue()).get("code");
            if (code == null || !code.isJsonArray()) throw new IllegalArgumentException("missing verified function code");
            instructions = Math.addExact(instructions, code.getAsJsonArray().size());
        }
        String canonical = ScriptJson.canonical(program);
        if (!receipt.has("ok") || !receipt.get("ok").equals(new com.google.gson.JsonPrimitive(true))
                || !VERIFIER_VERSION.equals(text(receipt.get("verifier"), 32))
                || !ScriptJson.digest(program).equals(text(receipt.get("program_sha256"), 64))
                || !ScriptJson.digest(contract).equals(text(receipt.get("contract_sha256"), 64))
                || !receipt.get("functions").equals(new com.google.gson.JsonPrimitive(functions.size()))
                || !receipt.get("instructions").equals(new com.google.gson.JsonPrimitive(instructions))
                || !executions.contains(receipt)) {
            throw new IllegalArgumentException("program lacks a matching cloud verifier result");
        }
        return new Grant(text(item.get("icon"), 256), text(item.get("name"), 128), text(item.get("description"), 512),
            text(result.get("answer"), 512), text(result.get("twist"), 96), canonical);
    }

    private static JsonObject object(JsonElement value) {
        if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("expected wish response object");
        return value.getAsJsonObject();
    }

    private static void requireKeys(JsonObject object, Set<String> keys) {
        if (!object.keySet().equals(keys)) throw new IllegalArgumentException("unexpected wish response fields");
    }

    private static String text(JsonElement value, int length) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("expected wish response text");
        String text = value.getAsString();
        if (text.isBlank() || text.length() > length || text.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("invalid wish response text");
        return text;
    }
}
