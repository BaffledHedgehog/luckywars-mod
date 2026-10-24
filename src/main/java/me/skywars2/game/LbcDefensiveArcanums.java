package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.ScoreHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Held Eternal Life and Absolute Barrier: each hand dispatches main then off. */
public final class LbcDefensiveArcanums {
    private static final CompoundTag HEALING_FLAG=flag("healing_arcanum"),BARRIER_FLAG=flag("barrier_arcanum");
    public static final Item HEALING=registerItem("skywars2:healing_arcanum","healing_arcanum","lbc:arcanumhealing");
    public static final Item BARRIER=registerItem("skywars2:barrier_arcanum","barrier_arcanum","lbc:arcanumbarrier");
    private static final int STRICT_FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    private final GameController game;
    private final LbcMana mana;
    private final Map<UUID,Hands> hands=new HashMap<>();
    private static final class Hands {
        final ServerLevel level;
        LbcIndirectKills.Owner main,off;
        LbcIndirectKills.Owner cause;
        int paidAt=Integer.MIN_VALUE;
        Hands(ServerLevel level){this.level=level;}
    }

    LbcDefensiveArcanums(GameController game,LbcMana mana){
        this.game=game;this.mana=mana;
        config("healing_arcanum_mana_tick",500);
        config("healing_arcanum_mana_tick_sec",10000);
        config("barrier_arcanum_mana_activate",100);
        config("barrier_arcanum_mana_tick",40);
        config("barrier_arcanum_mana_tick_sec",800);
    }
    private void config(String name,int value){mana.score(ScoreHolder.forNameOnly(name),"lbc.math").set(value);}
    private int config(String name){return mana.score(ScoreHolder.forNameOnly(name),"lbc.math").get();}
    private static CompoundTag flag(String name){var tag=new CompoundTag();tag.putInt(name,1);return tag;}
    private static Item registerItem(String id,String name,String modelId){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        var data=flag(name);data.putInt("transformable",1);data.putInt("arcanum",1);data.putInt("arcanumt",1);
        var model=Identifier.parse(modelId);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).overrideDescription(name).component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_a"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.REPEATING_COMMAND_BLOCK,true));
    }
    public static void register(){}
    private static boolean matches(ItemStack stack,Item item,CompoundTag flag){
        if(stack.isEmpty())return false;
        if(stack.is(item))return true;
        var custom=stack.get(DataComponents.CUSTOM_DATA);return custom!=null&&custom.matchedBy(flag);
    }
    public static boolean isHealing(ItemStack stack){return matches(stack,HEALING,HEALING_FLAG);}
    public static boolean isBarrier(ItemStack stack){return matches(stack,BARRIER,BARRIER_FLAG);}

    public void tick(){
        if(!game.server().tickRateManager().runsNormally())return;
        var present=new HashSet<UUID>();
        for(var player:game.server().getPlayerList().getPlayers()){
            present.add(player.getUUID());tickPlayer(player);
        }
        hands.keySet().removeIf(id->!present.contains(id));
    }
    void tickPlayer(ServerPlayer player){
        if(!game.server().tickRateManager().runsNormally())return;
        UUID id=player.getUUID();
        if(!mana.visible(player)||!player.isAlive()||game.isObserver(player)){forget(player);return;}
        boolean main=isBarrier(player.getMainHandItem()),off=isBarrier(player.getOffhandItem());
        var state=hands.get(id);
        if(state!=null&&state.level!=player.level()){hands.remove(id);state=null;}
        // Unequip is observed even while paused; swapping equal barriers keeps the source hand latch.
        if(state!=null){if(!main)state.main=null;if(!off)state.off=null;}
        if(!main&&!off)hands.remove(id);
        if(mana.noMagic(player)>0){hands.remove(id);return;}
        if(game.items().timeStop().frozen(player)||game.items().timeStop().stopped(player.level())
                ||player.entityTags().contains("time_stopped")||game.items().frost().isFrozen(player)
                ||game.items().magic().suppressed(player))return;
        if(state==null&&(main||off)){state=new Hands(player.level());hands.put(id,state);}
        boolean paid=false;
        if(isHealing(player.getMainHandItem()))heal(player);
        if(main)paid=barrier(player,state,main,off);
        if(isHealing(player.getOffhandItem()))heal(player);
        if(off&&!main)paid=barrier(player,state,main,off);
        if(paid&&main&&off)clearTerrain(player,state.main);
        if(state!=null&&state.main==null&&state.off==null)hands.remove(id);
    }
    private void heal(ServerPlayer player){
        if(player.getHealth()>=player.getMaxHealth())return;
        int price=config("healing_arcanum_mana_tick");
        if(price<0||!mana.trySpend(player,price))return;
        // The original health objective rounds up Health + Absorption, unlike the healing admission.
        if(player.getHealth()+player.getAbsorptionAmount()<=1)mana.award(player,"lbc:true_advancements/arcanums/heal");
        // Native healing keeps existing direct/indirect damage ownership intact.
        MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(player.level(),null,null,player,0,1.0);
        player.level().sendParticles(ParticleTypes.HEART,player.getX(),player.getY()+1,player.getZ(),5,2,2,2,0);
        player.level().playSound(null,player.blockPosition(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.MASTER,1,1);
    }
    /** Author ruling: 500% regeneration per tick, once for either or both hands. */
    int barrierPrice(ServerPlayer player){
        long price=Math.max(0L,(long)mana.regeneration(player)*5);
        return price>Integer.MAX_VALUE?-1:(int)price;
    }
    private boolean barrier(ServerPlayer player,Hands state,boolean main,boolean off){
        int price=barrierPrice(player);
        if(price<0||!mana.trySpend(player,price)){
            state.main=null;state.off=null;state.paidAt=Integer.MIN_VALUE;return false;
        }
        // A newly acquired hand keeps its original influence order through all subsequent upkeep.
        if(state.cause==null)state.cause=game.items().indirectKills().coldCause(player,false);
        if(main)state.main=state.cause;
        if(off)state.off=state.cause;
        state.paidAt=game.server().getTickCount();
        sparks(player);
        return true;
    }
    /** Native damage gate runs before hurt/knockback side effects; holding is checked at impact time. */
    public boolean blocksDamage(LivingEntity target,DamageSource source){
        if(!(target instanceof ServerPlayer player)||source.is(DamageTypes.FELL_OUT_OF_WORLD))return false;
        var state=hands.get(player.getUUID());
        if(state==null||state.level!=player.level()||!mana.visible(player)||!player.isAlive()||game.isObserver(player)
                ||mana.noMagic(player)>0||game.items().magic().suppressed(player))return false;
        if(!isBarrier(player.getMainHandItem())&&!isBarrier(player.getOffhandItem()))return false;
        // Service dispatch is at END tick, while native entity damage can happen earlier in that tick.
        int elapsed=game.server().getTickCount()-state.paidAt;
        return elapsed>=0&&elapsed<=1&&(state.main!=null||state.off!=null);
    }
    private void sparks(ServerPlayer player){
        var world=player.level();double x=player.getX(),y=player.getY(),z=player.getZ();
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK,x,y+2.5,z,10,1,0,1,0);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK,x,y-1.5,z,10,1,0,1,0);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK,x+1.5,y+1,z,10,0,1,1,0);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK,x-1.5,y+1,z,10,0,1,1,0);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK,x,y+1,z+1.5,10,1,1,0,0);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK,x,y+1,z-1.5,10,1,1,0,0);
    }
    private void clearTerrain(ServerPlayer player,LbcIndirectKills.Owner cause){
        var world=player.level();var feet=player.blockPosition();
        var first=feet.offset(-1,player.isShiftKeyDown()?-1:0,-1);var last=feet.offset(1,2,1);
        var kills=game.items().indirectKills();
        kills.withSupportCandidates(world,new AABB(first.getX(),first.getY(),first.getZ(),last.getX()+1,last.getY()+1,last.getZ()+1),
                ()->kills.trapChanges(world,cause,()->{
                    for(var pos:BlockPos.betweenClosed(first,last)){
                        if(world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)continue;
                        if(!world.getBlockState(pos).isAir())world.setBlock(pos,Blocks.AIR.defaultBlockState(),STRICT_FLAGS);
                    }
                }));
    }
    int activeHands(ServerPlayer player){
        var state=hands.get(player.getUUID());
        if(state==null)return 0;
        return (state.main!=null&&isBarrier(player.getMainHandItem())?1:0)
                +(state.off!=null&&isBarrier(player.getOffhandItem())?1:0);
    }
    public int count(){return hands.size();}
    public void forget(ServerPlayer player){hands.remove(player.getUUID());}
    public void stop(){hands.clear();}
}
