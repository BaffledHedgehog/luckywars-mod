package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.Level;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** A distinct native token, with the source prototype decoded once during registry bootstrap. */
public final class LbcBridgeBuilderItem {
    private static final String DEFINITION =
            "/data/lbc/loot_table/item_defs/lbc/bridgebuilder__d20c5b8711ad.json";
    public static final Item TOKEN = registerAuthored();

    private LbcBridgeBuilderItem() { }
    public static void register() { }

    private static Item registerAuthored() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:bridge_builder_imba"));
        var properties=LbcUtilityItemPrototypes.properties("skywars2:bridge_builder_imba",DEFINITION,Items.SNOWBALL,16)
                .overrideDescription("bridge_builder_imba");
        return Registry.register(BuiltInRegistries.ITEM,key,new NativeSnowball(properties));
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


}
