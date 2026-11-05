package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Canonical authored prototypes; the existing native egg and block-item paths own input. */
public final class LbcImbaFollowupItems {
    public static final Item DEBRIS=register("funny_debris",new Debris(properties("funny_debris","item_defs/minecraft/ancient_debris__16df99fdddce",Items.ANCIENT_DEBRIS).fireResistant()));
    public static final Item DIAMONDIFIER=register("diamondificator",new MarkerEgg(properties("diamondificator","imba/diamond_area_block",Items.PANDA_SPAWN_EGG),true));
    public static final Item MIDAS_BLOCK=register("midas_block",new MarkerEgg(properties("midas_block","item_defs/minecraft/gold_block__43a5c83ff880",Items.PANDA_SPAWN_EGG),false));
    public static final Item FUSED_TNT=register("fused_tnt_funny",new LbcSpecialEggs.Egg(properties("fused_tnt_funny","item_defs/lbc/fusedtnt__fc529c866f79",Items.PANDA_SPAWN_EGG),Items.PANDA_SPAWN_EGG));
    public static final Item SUPER_GHAST=register("super_ghast_funny",new LbcSpecialEggs.Egg(properties("super_ghast_funny","item_defs/minecraft/ghast_spawn_egg__0f33983f4058",Items.GHAST_SPAWN_EGG),Items.GHAST_SPAWN_EGG));
    private LbcImbaFollowupItems(){}
    public static void register(){}
    private static Item register(String name,Item item){return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+name),item);}
    static boolean midas(ItemStack stack){var data=stack.get(net.minecraft.core.component.DataComponents.ENTITY_DATA);return data!=null&&data.type()==net.minecraft.world.entity.EntityTypes.MARKER&&data.getUnsafe().getListOrEmpty("Tags").contains(net.minecraft.nbt.StringTag.valueOf("midas_block"));}
    /** Saved legacy carriers enter these same native input owners without replacing user components. */
    static Item legacyTarget(ItemStack stack){return ItemData.intValue(stack,"debriser",0)==1?DEBRIS:LbcDiamondifier.matches(stack)?DIAMONDIFIER:midas(stack)?MIDAS_BLOCK:LbcImbaRecursionItems.legacyTarget(stack);}
    private static Item.Properties properties(String name,String source,Item carrier){return LbcUtilityItemPrototypes.properties("skywars2:"+name,"/data/lbc/loot_table/"+source+".json",carrier,64);}
    private static final class Debris extends BlockItem implements PolymerItem {
        Debris(Properties properties){super(Blocks.ANCIENT_DEBRIS,properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.ANCIENT_DEBRIS;}
        @Override public InteractionResult place(BlockPlaceContext context){
            var game=SkyWars2.game();if(game==null||!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;
            var service=game.items().imbaTerrainUtilities();var origin=player.position();
            if(!service.canActivate(player,LbcImbaTerrainUtilities.Kind.DEBRIS,origin))return InteractionResult.FAIL;
            var result=super.place(context);if(result.consumesAction())service.debrisPlaced(player,origin);return result;
        }
    }
    private static final class MarkerEgg extends LbcMarkerEggItem {
        private final boolean diamond;
        MarkerEgg(Properties properties,boolean diamond){super(properties);this.diamond=diamond;}
        protected InteractionResult activate(ServerPlayer player,InteractionHand hand,Vec3 origin,BlockPos at){
            var game=SkyWars2.game();if(game==null)return InteractionResult.FAIL;
            boolean used=diamond?game.items().diamondifier().use(player,hand,origin):LbcImbaFollowup.midas(game,player,hand,origin);
            if(!used)return InteractionResult.FAIL;player.level().gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE,at);return InteractionResult.SUCCESS;
        }
    }
}
