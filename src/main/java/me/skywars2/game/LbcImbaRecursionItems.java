package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Canonical TNT tokens share the existing native marker-egg input and collision pose. */
public final class LbcImbaRecursionItems {
    public static final Item FUNNY_TNT=register("funny_tnt","minecraft/tnt__8255b57c8090",false);
    public static final Item DELTA_TNT=register("delta_tnt","lbc/omegatnt__ff0a27b525da",true);
    private LbcImbaRecursionItems(){}
    public static void register(){}
    static Item legacyTarget(ItemStack stack){return LbcFunnyTnt.matches(stack)?FUNNY_TNT:LbcDeltaTnt.matches(stack)?DELTA_TNT:null;}
    private static Item register(String name,String definition,boolean delta){
        var properties=LbcUtilityItemPrototypes.properties("skywars2:"+name,"/data/lbc/loot_table/item_defs/"+definition+".json",Items.PANDA_SPAWN_EGG,64);
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+name),new Token(properties,delta));
    }
    private static final class Token extends LbcMarkerEggItem {
        final boolean delta;
        Token(Properties properties,boolean delta){super(properties);this.delta=delta;}
        protected InteractionResult activate(ServerPlayer player,InteractionHand hand,Vec3 origin,BlockPos at){
            var game=SkyWars2.game();if(game==null)return InteractionResult.FAIL;
            boolean used=delta?game.items().deltaTnt().use(player,hand,origin):game.items().funnyTnt().use(player,hand,origin);
            if(!used)return InteractionResult.FAIL;player.level().gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE,at);return InteractionResult.SUCCESS;
        }
    }
}
