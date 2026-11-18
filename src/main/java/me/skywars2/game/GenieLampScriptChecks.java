package me.skywars2.game;

import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import me.skywars2.game.genie.*;
import me.skywars2.game.genie.script.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

/** Native lamp packets and inventory publication. The model observation is injected at the browser boundary. */
final class GenieLampScriptChecks implements AutoCloseable {
    private record Call(JsonObject contract, CompletableFuture<GenieScriptModel.Observation> response) {}
    private final GameController game;
    private final WoolBucketChecks.Fixture base;
    private final GenieService service;
    private final GenieScriptModel model, previous;
    private final Field field;
    private final BlockingQueue<Call> requests = new LinkedBlockingQueue<>();
    private int phase, when, sequence = 900;

    GenieLampScriptChecks(GameController game, WoolBucketChecks.Fixture base) throws ReflectiveOperationException {
        this.game = game;
        this.base = base;
        service = game.items().genie();
        field = GenieService.class.getDeclaredField("scriptModel");
        field.setAccessible(true);
        previous = (GenieScriptModel) field.get(service);
        model = new GenieScriptModel((prompt, knowledge, contract, verifier) -> {
            require(prompt.contains("LuckyWars script lamp witness"), "real player input reaches the scripted wish prompt");
            require(!game.server().isSameThread(), "model runs away from the tick thread");
            require(new String(verifier, StandardCharsets.UTF_8).contains(GenieScriptReply.VERIFIER_VERSION), "actual versioned checker is packaged");
            // Game reference data includes every loaded table and locale. It is not a VM program.
            JsonObject facts = com.google.gson.JsonParser.parseString(knowledge).getAsJsonObject();
            require(facts.getAsJsonArray("logical_luckywars_items").size() > 1000 && !facts.has("constructor_schema"),
                "the new request carries the actual index without the old rule budget");
            require(facts.getAsJsonArray("script_grimoires").contains(new com.google.gson.JsonPrimitive("skywars2:tesla_instant_grimoire"))
                && facts.getAsJsonArray("script_launchable_entity_types").contains(new com.google.gson.JsonPrimitive("minecraft:creeper")),
                "the model receives the executable definitions of real factory types and spells");
            var tables = facts.getAsJsonObject("loot_tables");
            var registry = game.server().reloadableRegistries().lookup().lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE);
            var ids = registry.listElementIds().map(key -> key.identifier().toString()).collect(java.util.stream.Collectors.toSet());
            require(tables.keySet().equals(ids), "the model sees all loaded loot table IDs");
            require(tables.asMap().values().stream().noneMatch(table -> table.getAsJsonObject().has("contents_unavailable")),
                "all loaded loot tables were decoded for the model");
            String boots = tables.get("lbc:ported/moisei_boots").toString();
            require(boots.contains("skywars2:moisei_boots") && boots.contains("\"translate\":\"moisei_boots\""),
                "a named loot entry includes its carrier and original translation key");
            var names = facts.getAsJsonObject("item_names");
            require(names.size() == net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().size()
                && names.get("minecraft:diamond_sword").getAsJsonObject().get("translate").getAsString().equals("item.minecraft.diamond_sword"),
                "the model sees display names for all registered items, including vanilla");
            var locales = facts.getAsJsonObject("item_name_translations");
            require(locales.getAsJsonObject("en_us").get("item.minecraft.diamond_sword").getAsString().equals("Diamond Sword"),
                "vanilla translations are human-readable without an external locale file");
            if (Boolean.getBoolean("skywars2.check.genie.locales")) {
                me.skywars2.SkyWars2.LOGGER.info("[check] Genie content reference: {} loot tables, {} names, {} locales, {} characters",
                    tables.size(), names.size(), locales.size(), knowledge.length());
                require(locales.getAsJsonObject("ru_ru").get("moisei_boots").getAsString().equals("Сапоги Моисея"),
                    "the resource-pack translation is attached by its exact key");
            }
            var response = new CompletableFuture<GenieScriptModel.Observation>();
            requests.add(new Call(ScriptJson.parse(contract), response));
            return response.get(8, TimeUnit.SECONDS);
        });
        field.set(service, model);
    }

    private static void require(boolean ok, String reason) {
        if (!ok) throw new IllegalStateException("Genie scripted lamp: " + reason);
    }

    private void use() {
        base.client.acknowledge();
        base.owner.connection.handleClientTickEnd(new ServerboundClientTickEndPacket());
        base.owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence++,
            base.owner.getYRot(), base.owner.getXRot()));
    }

    void submit() {
        base.owner.getInventory().setSelectedSlot(0);
        base.owner.getInventory().setItem(10, ItemStack.EMPTY);
        base.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GenieItems.LAMP));
        base.owner.getMainHandItem().set(DataComponents.CUSTOM_NAME, Component.literal("Лампа из проверки"));
        use();
        var dialog = ((Map<?,?>) WoolBucketChecks.read(service, "dialogs")).get(base.owner.getUUID());
        require(dialog != null, "the native use packet opens the lamp dialog");
        var shown = base.client.packets().stream().filter(ClientboundShowDialogPacket.class::isInstance)
                .map(ClientboundShowDialogPacket.class::cast).reduce((first, last) -> last).orElseThrow();
        require(shown.dialog().value() instanceof MultiActionDialog content && content.actions().size() == 1
                && content.common().body().getFirst() instanceof PlainMessage warning
                && warning.contents().getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.RED))
                && warning.contents().getString().lines().count() == 2, "warning is two red lines of plain text");
        String token = (String) WoolBucketChecks.read(dialog, "token");
        var input = new CompoundTag();input.putString("wish", "LuckyWars script lamp witness");
        base.owner.connection.handleCustomClickAction(new ServerboundCustomClickActionPacket(
            Identifier.parse("skywars2:genie/" + token), Optional.of(input)));
        var lamp = base.owner.getMainHandItem();
        require(service.locked(base.owner, lamp), "a scripted request reserves the specific lamp");
        require(lamp.getHoverName().getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("genie.lamp.thinking")
                && Identifier.parse("lbc:tier_q").equals(lamp.get(DataComponents.TOOLTIP_STYLE)),
                "thinking lamp uses the independent translation and Q tooltip");
        base.owner.getInventory().setItem(10, lamp);
        base.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 3));
        when = game.server().getTickCount();
    }

    private static GenieScriptModel.Observation grant(JsonObject contract, boolean witnessed) {
        var program = ScriptJson.parse("""
            {"version":1,"state":{"counter":0},"handlers":{"USE":"main"},"functions":{
            "main":{"parameters":["event"],"code":[["load","state"],["const","counter"],["const",1],["put"],["return"]]}}}
            """);
        var receipt = new JsonObject();
        receipt.addProperty("ok", true);receipt.addProperty("verifier", GenieScriptReply.VERIFIER_VERSION);
        receipt.addProperty("program_sha256", ScriptJson.digest(program));receipt.addProperty("contract_sha256", ScriptJson.digest(contract));
        receipt.addProperty("functions", 1);receipt.addProperty("instructions", 5);
        var item = new JsonObject();item.addProperty("icon", "minecraft:stick");
        item.addProperty("name", "One-count stick");item.addProperty("description", "It counts up to one.");
        var reply = new JsonObject();reply.addProperty("decision", "grant");reply.add("item", item);
        reply.addProperty("answer", "A simple count! It might fit on one finger.");reply.addProperty("twist", "one-count ceiling");
        reply.add("program", program);reply.add("receipt", receipt);
        return new GenieScriptModel.Observation(reply.toString(), witnessed ? List.of(receipt) : List.of());
    }

    boolean tick() {
        require(game.server().getTickCount() - when < 120, "model and code completion have a bounded fixture deadline");
        if (phase == 0 || phase == 2) {
            Call request = requests.poll();
            if (request == null) return false;
            request.response.complete(grant(request.contract, phase == 2));phase++;return false;
        }
        var item = base.owner.getInventory().getItem(10);
        if (phase == 1) {
            if (service.locked(base.owner, item)) return false;
            require(item.is(GenieItems.LAMP) && !GenieScriptItem.matches(item) && base.owner.getMainHandItem().getCount() == 3,
                "an unwitnessed grant returns the moved lamp without punishing the player");
            require(item.getHoverName().getString().equals("Лампа из проверки"),"the original custom name is restored after the request");
            submit();phase = 2;return false;
        }
        if (phase == 3) {
            if (service.locked(base.owner, item)) return false;
            require(item.is(Items.STICK) && GenieScriptItem.matches(item) && base.owner.getMainHandItem().getCount() == 3,
                "verified code replaces the exact moved lamp with a native scripted item");
            base.owner.getInventory().setItem(10, ItemStack.EMPTY);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND, item);
            use();phase = 4;return false;
        }
        return GenieScriptItem.state(base.owner.getMainHandItem()).get("counter").getAsInt() == 1;
    }

    @Override public void close() {
        service.cancel(base.owner);
        model.close();
        try { field.set(service, previous); }
        catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }
}
