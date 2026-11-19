package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.skywars2.game.genie.script.GenieScriptReply.NativeAbility;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Projects catalogued TMT properties onto the actual Genie reward, using the normal input owners. */
public final class GenieNativeAbilities {
    private record Variant(LbcTmtCatalog.Family family, LbcTmtAbilities.Ability ability) {}
    private static final Map<LbcTmtTriggers.Action, LbcTmtTriggers.Input> DEFAULT_INPUTS = Map.of(
            LbcTmtTriggers.Action.USE, LbcTmtTriggers.Input.RIGHT_CLICK,
            LbcTmtTriggers.Action.SELF, LbcTmtTriggers.Input.TICK,
            LbcTmtTriggers.Action.MELEE, LbcTmtTriggers.Input.HIT,
            LbcTmtTriggers.Action.INCOMING, LbcTmtTriggers.Input.HURT,
            LbcTmtTriggers.Action.JUMP, LbcTmtTriggers.Input.JUMP,
            LbcTmtTriggers.Action.SHOT, LbcTmtTriggers.Input.SHOT,
            LbcTmtTriggers.Action.CONSUME, LbcTmtTriggers.Input.EAT,
            LbcTmtTriggers.Action.BLOCK, LbcTmtTriggers.Input.BLOCK_USE,
            LbcTmtTriggers.Action.MINING, LbcTmtTriggers.Input.MINING);
    private final GameController game;
    private final Map<String, Variant> variants;
    private final String index;

    GenieNativeAbilities(GameController game, LbcTmtCatalog catalog) {
        this.game = game;
        var lookup = new HashMap<String, Variant>();
        var indexRows = new JsonArray();
        for (var family : catalog.families()) {
            var row = new JsonObject();
            row.addProperty("id", family.id());
            row.addProperty("name", family.name());
            var events = new JsonArray();
            family.events().stream().sorted().forEach(event -> events.add(event.name()));
            row.add("events", events);
            var locations = new JsonArray();
            family.locations().stream().sorted().forEach(location -> locations.add(location.name()));
            row.add("locations", locations);
            var bindings = new JsonObject();
            for (var action : LbcTmtTriggers.actions(family)) bindings.addProperty(action.name(), DEFAULT_INPUTS.get(action).name());
            row.add("default_bindings", bindings);
            var variants = new JsonArray();
            for (var ability : family.variants()) {
                var payload = ability.data().copyTag();
                String token = UUID.nameUUIDFromBytes((family.id() + LbcTmtCatalog.canonical(payload))
                        .getBytes(StandardCharsets.UTF_8)).toString();
                if (lookup.putIfAbsent(token, new Variant(family, ability)) != null)
                    throw new IllegalStateException("Duplicate native ability variant " + token);
                var info = new JsonObject();
                info.addProperty("variant", token);
                String source = payload.getStringOr(LbcTmtAbilities.NATIVE, "");
                if (!source.isEmpty()) info.addProperty("source_item", source);
                for (String field : List.of("mode", "upgraded", "type", "spellid", "spellcount", "cooltype", "pill", "glyphkind"))
                    if (payload.getIntOr(field, 0) != 0) info.addProperty(field, payload.getIntOr(field, 0));
                variants.add(info);
            }
            row.add("variants", variants);
            indexRows.add(row);
        }
        variants = Map.copyOf(lookup);
        index = indexRows.toString();
    }

    public String index() { return index; }

    public ItemStack attach(ItemStack gift, Identifier icon, List<NativeAbility> requested) {
        if (requested.isEmpty()) return gift;
        var properties = new ArrayList<LbcTmtAbilities.Ability>();
        var familyIds = new HashSet<String>();
        for (var request : requested) {
            var variant = variants.get(request.variant());
            if (variant == null) throw new IllegalArgumentException("Unknown native ability variant");
            if (!familyIds.add(variant.family.id())) throw new IllegalArgumentException("Repeated native ability family");
            properties.add(bind(gift, variant, request.bindings()));
        }
        // Use the same typed client-facing carriers for native shot, consume and using-item continuations.
        var carrier = LbcTmtItems.carrierFor(gift);
        if (!gift.is(carrier)) gift = gift.transmuteCopy(carrier);
        gift.set(DataComponents.ITEM_MODEL, icon);
        var data = gift.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt("tmt_item", 1);
        data.putInt("tmt_texture_seed", UUID.randomUUID().hashCode() & Integer.MAX_VALUE);
        data.putInt(LbcTmtTriggers.SCHEMA, 1);
        LbcTmtAbilities.write(data, properties);
        gift.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        LbcTmtEquipment.apply(game, gift);
        LbcTmtTotems.apply(gift);
        LbcTmtContinuous.apply(gift);
        return gift;
    }

    private static LbcTmtAbilities.Ability bind(ItemStack carrier, Variant variant, Map<String, String> custom) {
        var actions = LbcTmtTriggers.actions(variant.family);
        if (!LbcTmtTriggers.compatible(variant.family, carrier))
            throw new IllegalArgumentException("A native shot requires a bow or crossbow carrier");
        var inputs = LbcTmtTriggers.inputs(carrier);
        for (String name : custom.keySet())
            if (!actions.contains(LbcTmtTriggers.Action.valueOf(name)))
                throw new IllegalArgumentException("Native ability has no rebindable " + name + " action");
        if (actions.isEmpty()) return variant.ability;
        var payload = variant.ability.data().copyTag();
        var bindings = new CompoundTag();
        for (var action : actions) {
            var preferred = DEFAULT_INPUTS.get(action);
            String chosen = custom.get(action.name());
            var input = chosen == null ? (inputs.contains(preferred) ? preferred : LbcTmtTriggers.Input.RIGHT_CLICK)
                    : LbcTmtTriggers.Input.valueOf(chosen);
            if (!inputs.contains(input)) throw new IllegalArgumentException("Native input is unavailable on this carrier");
            bindings.putString(action.name(), input.name());
        }
        payload.put(LbcTmtTriggers.KEY, bindings);
        return new LbcTmtAbilities.Ability(variant.ability.id(), CustomData.of(payload));
    }
}
