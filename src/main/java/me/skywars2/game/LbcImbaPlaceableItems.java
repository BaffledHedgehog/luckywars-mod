package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Authored spawn-egg inputs reuse native templates and the common native egg path. */
public final class LbcImbaPlaceableItems {
    public static final Item END_PORTAL=register("end_portal",new Token(properties("end_portal","minecraft/end_portal_frame__7857d95d485e"),LbcImbaTerrainUtilities.Kind.END_PORTAL));
    public static final Item NETHER_PORTAL=register("nether_portal",new Token(properties("nether_portal","minecraft/crying_obsidian__89fc6825b475"),LbcImbaTerrainUtilities.Kind.NETHER_PORTAL));
    public static final Item MYSTICAL_TREE=register("mystical_tree",new Token(properties("mystical_tree","lbc/mystical_tree__78942364b3c2"),null));
    public static final Item END_CRYSTAL=register("free_end_crystal",new LbcSpecialEggs.Egg(properties("free_end_crystal","minecraft/end_crystal__3f1c3facaeb9"),Items.PANDA_SPAWN_EGG));
    public static final Item NUCLEAR_BOMB=register("imba_nuclear_bomb",new Nuclear(properties("imba_nuclear_bomb","lbc/nuke__c507f2f53fda")));
    private LbcImbaPlaceableItems(){}
    public static void register(){}
    private static Item register(String id,Item item){return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),item);}
    private static Item.Properties properties(String id,String definition){return LbcUtilityItemPrototypes.properties("skywars2:"+id,"/data/lbc/loot_table/item_defs/"+definition+".json",Items.PANDA_SPAWN_EGG,64);}
    static boolean tagged(ItemStack stack,String flag){var data=stack.get(DataComponents.ENTITY_DATA);return data!=null&&data.type()==EntityTypes.MARKER&&data.getUnsafe().getListOrEmpty("Tags").contains(StringTag.valueOf(flag));}
    static LbcImbaTerrainUtilities.Kind templateKind(ItemStack stack){return tagged(stack,"end_portal")?LbcImbaTerrainUtilities.Kind.END_PORTAL:tagged(stack,"nether_portal")?LbcImbaTerrainUtilities.Kind.NETHER_PORTAL:null;}
    static Item legacyTarget(ItemStack stack){return tagged(stack,"end_portal")?END_PORTAL:tagged(stack,"nether_portal")?NETHER_PORTAL:tagged(stack,"mystical_tree")?MYSTICAL_TREE:crystal(stack)?END_CRYSTAL:null;}
    private static boolean crystal(ItemStack stack){var data=stack.get(DataComponents.ENTITY_DATA);return data!=null&&data.type()==EntityTypes.END_CRYSTAL&&ItemData.intValue(stack,"imba",0)==1;}
    static void adopt(GameController game,Entity entity,ServerPlayer player,ItemStack stack){
        if(player==null||!crystal(stack)||entity.getType()!=EntityTypes.END_CRYSTAL)return;
        game.items().spellSources().bind(entity,new LbcSpellSource.Source(entity.getUUID(),player.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(player),java.util.Set.of(),true));
        game.items().markCurrentItemEpoch(entity);game.items().timeStop().prepareFrozen(entity);
    }
    /** One canonical nuke-bruh marker; warning, terrain, waves and cleanup stay in the nuclear owner. */
    private static final class Nuclear extends LbcMarkerEggItem {
        Nuclear(Properties properties){super(properties);}
        protected InteractionResult activate(ServerPlayer player,InteractionHand hand,Vec3 origin,BlockPos at){
            var game=SkyWars2.game();
            if(!LbcSpecialEggs.allowed(game,player)||!game.items().nukeEvent().start(player,origin,false))return InteractionResult.FAIL;
            var stack=player.getItemInHand(hand);stack.consume(1,player);
            player.level().gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE,at);
            return InteractionResult.SUCCESS;
        }
    }
    private static final class Token extends LbcMarkerEggItem {
        final LbcImbaTerrainUtilities.Kind kind;
        Token(Properties properties,LbcImbaTerrainUtilities.Kind kind){super(properties);this.kind=kind;}
        protected InteractionResult activate(ServerPlayer player,InteractionHand hand,Vec3 origin,BlockPos at){
            var game=SkyWars2.game();if(game==null)return InteractionResult.FAIL;
            boolean used=kind==null?game.items().mysticalTree().use(player,hand,origin):game.items().imbaTerrainUtilities().useTemplate(player,hand,origin,kind);
            if(!used)return InteractionResult.FAIL;player.level().gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE,at);return InteractionResult.SUCCESS;
        }
    }
}
