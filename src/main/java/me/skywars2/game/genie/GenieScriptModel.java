package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import me.skywars2.game.genie.script.GenieScriptReply;
import me.skywars2.game.genie.script.ScriptJson;
import me.skywars2.game.genie.script.ScriptProgram;

/** Builds each scripted wish on a worker, then verifies the model's observed tool result. */
public final class GenieScriptModel implements AutoCloseable {
    public record Observation(String reply, List<JsonObject> executions) {}

    @FunctionalInterface
    public interface Transport {
        Observation run(String prompt, String knowledge, String contract, byte[] verifier) throws Exception;
    }

    public record Pending(CompletableFuture<GenieScriptReply> future, Runnable cancel) {
        public void close() { cancel.run(); }
    }

    private final Transport transport;
    private final ThreadPoolExecutor worker;
    private final ConcurrentHashMap<FutureTask<Void>,CompletableFuture<GenieScriptReply>> pending = new ConcurrentHashMap<>();

    public GenieScriptModel(Transport transport) {
        this.transport = Objects.requireNonNull(transport);
        worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), runnable -> {
            Thread thread = new Thread(runnable, "skywars2-genie-chat");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    public static JsonObject contract() {
        return ScriptProgram.contract(GenieWorldHost.api(), GenieScriptRuntime.EVENTS);
    }

    public Pending request(String wish, List<String> recent) {
        var result = new CompletableFuture<GenieScriptReply>();
        var task = new FutureTask<Void>(() -> {
            try {
                var contract = contract();
                var observation = transport.run(prompt(wish), GenieKnowledge.forScriptWish(wish, recent),
                    ScriptJson.canonical(contract), verifier());
                result.complete(GenieScriptReply.read(observation.reply, observation.executions, contract));
            } catch (Exception failure) {
                result.completeExceptionally(failure);
            }
            return null;
        });
        pending.put(task, result);
        result.whenComplete((reply, error) -> pending.remove(task));
        try { worker.execute(task); }
        catch (RejectedExecutionException unavailable) { result.completeExceptionally(unavailable); }
        return new Pending(result, () -> {
            result.cancel(false);
            task.cancel(true);
            worker.remove(task);
        });
    }

    private static byte[] verifier() throws IOException {
        try (var file = GenieScriptModel.class.getResourceAsStream("/skywars2/genie/verify_genie_program.py")) {
            if (file == null) throw new IOException("missing packaged item verifier");
            return file.readAllBytes();
        }
    }

    private static String prompt(String wish) {
        return """
            You are the genie in LuckyWars on Minecraft Java Edition. The attached game index gives the exact version and existing content. The attached contract defines the item program, instructions, events and host calls. On events, the argument has type, owner UUID, tick, hand and nullable target UUID. Source profiles are reference material. Only the contract's API calls are executable.
            Decide whether the wish is disrespectful, game-breaking or impossible even with these tools. If so, return only {"decision":"reject"}. Otherwise make one item that literally fulfills the wish. Use an appropriate vanilla item for its appearance and base behavior. Always add a meaningful hidden loophole based on something the player left unspecified. Vary it from recent twists and implement it in the program. Don't contradict an explicit requirement. Give a short, playful answer in the player's language that only hints at the catch. No long dashes, emoji, filler or theatrical prose.
            world.command takes one native gameplay command from script_gameplay_commands, executed in the owner's context. Data and datapack queries are read-only. Use item state and program functions for your own persistent data and routines. world.select returns UUIDs from a selector. world.raycast takes two xyz arrays, entity hit margin in blocks and a boolean to include entities. entity.info takes a UUID. math.random takes zero args for [0,1), one upper bound or min and max. time.tick gives the item's logical tick.
            entity.launch takes an ID from script_launchable_entity_types, optional position xyz (null for the standard launch from the owner), optional velocity xyz in blocks/tick (null for a forward throw), optional positive lifetime in ticks (default 200). It returns a UUID or null if the spot is blocked. lbc.grimoire takes an item ID from script_grimoires and an optional hand (mainhand or offhand), and runs the real spell implementation under its ordinary targeting and antimagic rules. It returns whether casting was accepted. Use a registered grimoire when it implements the requested spell, for example skywars2:tesla_instant_grimoire for an instant Tesla spell.
            A real bow or crossbow can bind SHOT to act alongside its vanilla volley. Bind SHOT_REPLACE to shoot something else instead. The native draw, ammunition and wear still apply. Both events carry projectiles, an ordered array of objects with type ID, position xyz and velocity xyz, sampled from the native volley. SHOT_REPLACE does not publish those projectiles. For a bow that fires creepers, use SHOT_REPLACE and pass a volley's position and velocity to entity.launch with minecraft:creeper.
            THROW runs on the exact used item after a native projectile launches. It applies to snowballs, eggs, ender pearls, wind charges, experience bottles, splash and lingering potions, and thrown tridents. It carries the real hand and a single projectiles entry with id (the spawned entity UUID), type, position and velocity. It works on the last expendable item. Dropping on Q and riptide without a projectile are not THROW.
            HIT runs for the weapon that actually dealt an accepted native hit, including a lethal one. The event carries the victim UUID as target and the hand used to attack or launch. Native shots and throws that later hit also carry the real projectile in projectiles. Holding a second item or switching weapons during flight does not make that other item the attack source.
            For a grant, save the exact proposed program as program.json. Run the attached original verify_genie_program.py in Python with the attached contract.json and your program.json. Show its stdout via the execution tool. If it fails, repair the program and rerun the same checker. Don't invent or rewrite a receipt. Return exactly one raw JSON object with keys decision, item, answer, twist, program, receipt. decision is "grant". item has string keys icon, name and description. twist is a short label for the hidden drawback. program is the exact checked JSON object, receipt is the actual successful checker output. No Markdown, questions or explanation.
            Player wish (JSON string):
            """ + new JsonPrimitive(wish);
    }

    @Override public void close() {
        worker.shutdownNow();
        for (var entry : pending.entrySet()) {
            entry.getValue().cancel(false);
            entry.getKey().cancel(true);
        }
    }
}
