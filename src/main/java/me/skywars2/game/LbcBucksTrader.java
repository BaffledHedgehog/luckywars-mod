package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.*;
import me.skywars2.mixin.BoundViewCheckpointAccess;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Immediate native money input and merchants; legacy snowballs only detected the RMB event. */
public final class LbcBucksTrader {
    public static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:bucks"),new Money(
            LbcUtilityItemPrototypes.properties("skywars2:bucks","/data/lbc/loot_table/imba/random_bucks.json",Items.SNOWBALL,64)));
    private static final CompoundTag TRADER=traderData();
    private final GameController game;
    public LbcBucksTrader(GameController game){this.game=game;}
    public static void register(){}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"bucks",0)==1;}
    private static CompoundTag traderData(){try{return TagParser.parseCompoundFully("""
            {Tags:["luck_trader"],CustomNameVisible:true,DeathLootTable:"lbc:imba/random_bucks",
             PersistenceRequired:true,Health:1000000.0f,Xp:9999999,LastRestock:1L,
             CustomName:{color:"yellow",italic:false,translate:"baffled"},
             attributes:[{id:"minecraft:max_health",base:1000000.0d}],
             VillagerData:{level:99,profession:"minecraft:mason",type:"minecraft:swamp"}}
            """);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}}
    private boolean active(ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()
            &&level==game.maps().level()&&LbcDimensionItems.ordinary(level)&&LbcBedwarsRoleGuards.publication(level);}
    private boolean admitted(ServerPlayer owner){return active(owner.level())&&owner.isAlive()&&!owner.isRemoved()&&!owner.isSpectator()
            &&!game.isObserver(owner)&&!game.awaitingRespawn(owner.getUUID())&&game.canUseGameplayItems(owner)
            &&game.items().wands().validRoleTarget(owner,false)&&!game.items().actionsBlocked(owner)
            &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level())
            &&!game.server().tickRateManager().isFrozen()&&game.server().tickRateManager().runsNormally();}
    /** Bucks deliberately bypass both magic and mechanism suppression. */
    public boolean use(ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);if(!matches(stack)||!admitted(owner))return false;
        var level=owner.level();var at=owner.position();
        // Native child publication remains inside the caller's existing source scope.
        // Prepare the complete source body before exposing any reward or merchant.
        var published=new ArrayList<Entity>();
        try{
            var rewards=new ArrayList<ItemStack>();
            for(int i=0;i<2;i++)rewards.addAll(game.lbc().rollTable(level,at,owner,"lbc:imba/random_bucks"));
            var trader=new Villager(EntityTypes.VILLAGER,level);
            trader.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),TRADER.copy()));trader.setPos(at);
            var offers=new MerchantOffers();
            for(int i=0;i<10;i++){
                // Legacy gen_trades executes as/at this merchant after rc's two player-context rewards.
                var item=one(level,at,trader,"lbc:random_glack");var bucks=one(level,at,trader,"lbc:imba/random_bucks");
                offers.add(offer(i<7?bucks:item,i<7?item:bucks));
            }
            ((BoundViewCheckpointAccess.Villager)trader).skywars2$offers(offers);trader.entityTags().add("spawned");game.items().markCurrentItemEpoch(trader);
            for(var rewardStack:rewards){
                var reward=new ItemEntity(level,at.x,at.y,at.z,rewardStack);reward.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(reward);
                if(!level.addFreshEntity(reward)){reward.discard();return false;}published.add(reward);
            }
            if(!level.addFreshEntity(trader)){trader.discard();return false;}published.add(trader);
            owner.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,owner);
            published.clear();return true;
        }finally{for(var entity:published)entity.discard();}
    }
    private ItemStack one(ServerLevel level,Vec3 at,Entity source,String table){
        var result=game.lbc().rollTable(level,at,source,table);
        if(result.size()!=1||result.getFirst().isEmpty())throw new IllegalStateException("Bucks needs one logical item from "+table);
        return result.getFirst();
    }
    /** Exact native predicates retain the complete chosen input, including nested/private components. */
    static MerchantOffer offer(ItemStack buy,ItemStack sell){
        if(buy.isEmpty()||sell.isEmpty())throw new IllegalArgumentException("Empty bucks offer");
        var cost=new ItemCost(buy.getItem().builtInRegistryHolder(),buy.getCount(),DataComponentExactPredicate.allOf(buy.getComponents()),buy.copy());
        return new MerchantOffer(cost,Optional.empty(),sell.copy(),1,Integer.MAX_VALUE,1,0);
    }
    private static final class Money extends Item implements PolymerItem {
        Money(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.SNOWBALL;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer owner&&game.items().bucksTrader().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    }
}
