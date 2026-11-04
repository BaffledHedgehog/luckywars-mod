package me.skywars2.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.Level;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** A distinct native token, with the source prototype decoded once during registry bootstrap. */
public final class LbcBridgeBuilderItem {
    private static final String DEFINITION =
            "/data/lbc/loot_table/item_defs/lbc/bridgebuilder__d20c5b8711ad.json";
    public static final Item TOKEN = registerAuthored();

    private LbcBridgeBuilderItem() { }
    public static void register() { }

    private static Item registerAuthored() {
        try (var input = LbcBridgeBuilderItem.class.getResourceAsStream(DEFINITION)) {
            if (input == null) throw new IllegalStateException("Missing IMBA Bridge Builder item definition");
            var source = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject();
            if (!source.get("name").getAsString().equals("minecraft:snowball"))
                throw new IllegalStateException("Unexpected IMBA Bridge Builder source carrier");
            var rows = source.getAsJsonArray("modifier");
            Identifier authoredModel = null;
            for (var value : rows) {
                var row = value.getAsJsonObject();
                if (row.get("type").getAsString().equals("minecraft:set_components")) {
                    var components = row.getAsJsonObject("components");
                    if (components.has("minecraft:item_model"))
                        authoredModel = Identifier.parse(components.get("minecraft:item_model").getAsString());
                }
            }
            if (authoredModel == null) throw new IllegalStateException("Missing IMBA Bridge Builder source model");
            final Identifier model = authoredModel;
            var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:bridge_builder_imba"));
            var properties = new Item.Properties() {
                @Override public Identifier effectiveModel() { return model; }
            }.setId(key).stacksTo(16).overrideDescription("bridge_builder_imba");
            for (var value : rows) {
                var row = value.getAsJsonObject();
                switch (row.get("type").getAsString()) {
                    case "minecraft:set_count" -> {
                        if (row.get("count").getAsInt() != 1)
                            throw new IllegalStateException("IMBA Bridge Builder source count requires loot update");
                    }
                    case "minecraft:set_components" -> {
                        for (var component : row.getAsJsonObject("components").entrySet()) {
                            var type = BuiltInRegistries.DATA_COMPONENT_TYPE
                                    .getValue(Identifier.parse(component.getKey()));
                            if (type == null) throw new IllegalStateException("Unknown IMBA Bridge Builder source component");
                            component(properties, type, component.getValue());
                        }
                    }
                    case "minecraft:set_name" -> properties.component(DataComponents.CUSTOM_NAME,
                            ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, row.get("name")).getOrThrow());
                    case "minecraft:set_custom_data" -> properties.component(DataComponents.CUSTOM_DATA,
                            CustomData.of(TagParser.parseCompoundFully(row.get("tag").getAsString())));
                    case "minecraft:set_lore" -> {
                        if (!row.get("mode").getAsString().equals("replace_all"))
                            throw new IllegalStateException("Unexpected IMBA Bridge Builder source lore operation");
                        var lines = new ArrayList<Component>();
                        for (var line : row.getAsJsonArray("lore"))
                            lines.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, line).getOrThrow());
                        properties.component(DataComponents.LORE, new ItemLore(lines));
                    }
                    default -> throw new IllegalStateException("Unsupported IMBA Bridge Builder source modifier");
                }
            }
            return Registry.register(BuiltInRegistries.ITEM, key,
                    new NativeSnowball(properties));
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid IMBA Bridge Builder source item definition", failure);
        }
    }


    /** Vanilla SnowballItem owns the projectile, causal thrower, aim, sound, stat and sole debit. */
    private static final class NativeSnowball extends SnowballItem implements PolymerItem {
        NativeSnowball(Properties properties) { super(properties); }
        @Override public Item getPolymerItem(ItemStack stack, PacketContext context) { return Items.SNOWBALL; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            var game = me.skywars2.SkyWars2.game();
            if (game == null || !(player instanceof ServerPlayer owner) || !game.playing()
                    || !game.items().bridgeBuilder().ready()
                    || game.stage() == GameController.Stage.ENDING || game.maps().busy()
                    || !owner.isAlive() || owner.isRemoved() || owner.isSpectator() || game.isObserver(owner)
                    || !game.canUseGameplayItems(owner) || !LbcDimensionItems.ordinary(level)
                    || !LbcBedwarsRoleGuards.publication(owner.level())
                    || game.items().actionsBlocked(owner)
                    || game.items().magic().mechanismSuppressed(owner)
                    || game.items().wands().inputTimeStopped(owner)) return InteractionResult.FAIL;
            return super.use(level, player, hand);
        }
    }

    private static <T> void component(Item.Properties properties, DataComponentType<T> type, JsonElement value) {
        properties.component(type, type.codec().parse(JsonOps.INSTANCE, value).getOrThrow());
    }
}
