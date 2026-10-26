package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Signed environmental mechanics. All work bounds bound work per tick, never stored levels. */
public final class SignedEnvironmentalEffects {
    private SignedEnvironmentalEffects() {}
    private record Author(MobEffectInstance effect,ServerPlayer player) {}
    private static final Map<LivingEntity,Map<Holder<MobEffect>,Author>> authors=new WeakHashMap<>();
    private static final Map<Entity,ServerPlayer> offspringOwners=new WeakHashMap<>();
    private static final Map<LivingEntity,FireFill> fireFills=new WeakHashMap<>();
    private static final Map<LivingEntity,FireFill> waterFills=new WeakHashMap<>();
    private static int waterFillOffset;
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final class FireFill {
        final BlockPos center;final int radius;int cursor;
        FireFill(BlockPos center,int radius){this.center=center;this.radius=radius;}
    }
    public static void effectChanged(LivingEntity target,MobEffectInstance effect,Entity source) {
        if(!(effect.is(MobEffects.FIRE_RESISTANCE)||effect.is(MobEffects.WIND_CHARGED)||effect.is(MobEffects.WEAVING)||effect.is(MobEffects.OOZING)||effect.is(MobEffects.INFESTED)))return;
        var player=LbcIndirectKills.playerSource(source);
        var map=authors.computeIfAbsent(target,k->new HashMap<>());
        if(player==null)map.remove(effect.getEffect());else map.put(effect.getEffect(),new Author(effect,player));
    }
    private static ServerPlayer author(LivingEntity source,Holder<MobEffect> effect) {
        var map=authors.get(source);var author=map==null?null:map.get(effect);
        return author!=null&&author.effect==source.getEffect(effect)?author.player:source instanceof ServerPlayer p?p:null;
    }
    public static ServerPlayer spawnedOwner(Entity entity) { return offspringOwners.get(entity); }
    public static long level(int amplifier) { return (long)amplifier+1; }
    public static long level(LivingEntity entity, Holder<MobEffect> effect) {
        var current=entity.getEffect(effect);return current==null?0:level(current.getAmplifier());
    }
    public static int radius(long level,int legacyRadius,int cap) {
        return Math.min(cap,Math.max(legacyRadius,(int)Math.ceil(legacyRadius*Math.sqrt(Math.max(1,level)/3.0))));
    }
    public static int omenAmplifier(int amplifier) { return (int)Math.max(-1,Math.min(4,level(amplifier)-1)); }
    public static double fireMultiplier(long level) { return level<0?1+.2*Math.abs(level):1; }
    public static boolean protectedTarget(Entity target) {
        var game=SkyWars2.game();
        return LbcGreed.active(target)||target.isSpectator()||target.entityTags().contains("spectator")
                ||paused(target)
                ||(game!=null&&!game.items().wands().validRoleTarget(target,true))
                ||(game!=null&&target instanceof ServerPlayer p&&!game.canUseGameplayItems(p));
    }
    private static boolean paused(Entity target) {
        var game=SkyWars2.game();return target.entityTags().contains("time_stopped")||(game!=null&&(
                !game.server().tickRateManager().runsNormally()||game.items().wands().timeStopped()
                ||game.items().timeStop().frozen(target)||target instanceof LivingEntity living&&game.items().frost().isFrozen(living)));
    }
    public static boolean breathes(LivingEntity e) {
        return level(e,MobEffects.WATER_BREATHING)>0||level(e,MobEffects.CONDUIT_POWER)>0||level(e,MobEffects.BREATH_OF_THE_NAUTILUS)>0;
    }
    public static long airPenalty(LivingEntity e) {
        long result=0;
        for(var effect:List.of(MobEffects.WATER_BREATHING,MobEffects.CONDUIT_POWER,MobEffects.BREATH_OF_THE_NAUTILUS)) {
            long l=level(e,effect);if(l<0)result+=Math.abs(l);
        }
        return result;
    }
    public static int drainAir(int supply,long amount) {
        // Native drowning starts at -20; crossing that boundary must not overflow or skip it.
        return (int)Math.max(-20,(long)supply-amount);
    }
    public static boolean outsideDrain(LivingEntity e) {
        return level(e,MobEffects.WATER_BREATHING)>=2&&!e.isEyeInFluid(FluidTags.WATER)&&!protectedTarget(e);
    }
    public static void airTick(LivingEntity e) {
        if(e.level().isClientSide()||!outsideDrain(e))return;
        long l=level(e,MobEffects.WATER_BREATHING);
        if(l==2&&(e.tickCount&1)!=0)return;
        e.setAirSupply(Math.max(0,drainAir(e.getAirSupply(),Math.max(1,(l-1)/2))));
    }
    public static void fireTick(LivingEntity e) {
        if(!(e.level() instanceof ServerLevel level)||(e.tickCount&1)!=0||protectedTarget(e))return;
        long l=level(e,MobEffects.FIRE_RESISTANCE);if(l<2){fireFills.remove(e);return;}
        int r=l==2?0:l==3?1:Math.min(15,(int)Math.ceil(Math.sqrt(l-2)));
        var center=e.blockPosition();
        // Immediate local legacy cube; extended spheres advance in bounded strips.
        int near=Math.min(1,r);
        for(int x=-near;x<=near;x++)for(int y=-near;y<=near;y++)for(int z=-near;z<=near;z++) {
            var pos=center.offset(x,y,z);if(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.FIRE.defaultBlockState(),2);
        }
        if(l<4)return;
        var fill=fireFills.get(e);
        if(fill==null||!fill.center.equals(center)||fill.radius!=r){fill=new FireFill(center,r);fireFills.put(e,fill);}
        int side=2*r+1,total=side*side*side;
        for(int budget=0;budget<128;budget++) {
            int c=fill.cursor++;if(fill.cursor==total)fill.cursor=0;
            int x=c%side-r,y=c/(side*side)-r,z=c/side%side-r;
            if(x*x+y*y+z*z>r*r)continue;
            var pos=center.offset(x,y,z);if(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.FIRE.defaultBlockState(),2);
        }
        var game=SkyWars2.game();var owner=author(e,MobEffects.FIRE_RESISTANCE);
        var cause=game!=null&&owner!=null?game.items().indirectKills().wildCause(owner):null;
        double squared=(double)l*l;
        for(var target:level.getAllEntities())if(target!=e&&target instanceof LivingEntity living&&!protectedTarget(target)&&target.distanceToSqr(e)<=squared) {
            Runnable hurt=()->living.hurtServer(level,level.damageSources().onFire(),2);
            if(game!=null)game.items().indirectKills().withCause(living,cause,hurt);else hurt.run();
        }
    }
    /** AboveIII conduit: immediate nearby water, bounded spherical extension; legacy II/III remain in LbcColors. */
    public static void conduitFill(LivingEntity source,int radius) {
        if(!(source.level() instanceof ServerLevel level)||protectedTarget(source))return;
        var center=source.blockPosition();var fill=waterFills.get(source);
        if(fill==null||!fill.center.equals(center)||fill.radius!=radius)waterFills.put(source,new FireFill(center,radius));
        var game=SkyWars2.game();Runnable writes=()->{
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)waterCell(level,center.offset(x,y,z));
        };
        if(game!=null)game.items().indirectKills().effectWorldChanges(source,MobEffects.CONDUIT_POWER,writes);else writes.run();
    }
    private static void waterCell(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos))return;var old=level.getBlockState(pos);
        if(old.is(AIRS)&&level.setBlock(pos,Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))level.updateNeighboursOnBlockSet(pos,old);
    }
    private static void tickWaterFills() {
        int budget=2048;var entries=new ArrayList<>(waterFills.entrySet());int size=entries.size();if(size==0)return;
        int start=waterFillOffset%size;waterFillOffset=(start+1)%size;
        for(int index=0;index<size;index++) {
            var entry=entries.get((start+index)%size);var source=entry.getKey();var fill=entry.getValue();
            if(source==null)continue;
            if(source.isRemoved()||!source.isAlive()||level(source,MobEffects.CONDUIT_POWER)<=3){waterFills.remove(source);continue;}
            if(budget<=0||protectedTarget(source)||!(source.level() instanceof ServerLevel level))continue;
            int work=Math.min(128,budget);budget-=work;
            Runnable writes=()->{
                int r=fill.radius,side=2*r+1,total=side*side*side;
                for(int i=0;i<work;i++) {
                    int c=fill.cursor++;if(fill.cursor==total)fill.cursor=0;
                    int x=c%side-r,y=c/(side*side)-r,z=c/side%side-r;
                    if(x*x+y*y+z*z<=r*r)waterCell(level,fill.center.offset(x,y,z));
                }
            };
            var game=SkyWars2.game();if(game!=null)game.items().indirectKills().effectWorldChanges(source,MobEffects.CONDUIT_POWER,writes);else writes.run();
        }
    }
    public static long infestationCount(int amplifier,float random) {
        return infestationCount(amplifier,random,1);
    }
    public static long infestationCount(int amplifier,float random,int fractionalCount) {
        long magnitude=Math.abs(level(amplifier));
        return magnitude/10+(random<(magnitude%10)/10.0?fractionalCount:0);
    }
    private static final ArrayDeque<SpawnJob> spawns=new ArrayDeque<>();
    private record SpawnJob(ServerLevel level,Vec3 at,boolean negative,long remaining,LivingEntity source,ServerPlayer author) {}
    public static void infested(ServerLevel level,LivingEntity mob,int amplifier) {
        if(protectedTarget(mob))return;
        long count=infestationCount(amplifier,mob.getRandom().nextFloat(),1+mob.getRandom().nextInt(2));
        if(count>0)spawns.add(new SpawnJob(level,mob.position().add(0,mob.getBbHeight()/2,0),level(amplifier)<0,count,mob,author(mob,MobEffects.INFESTED)));
    }
    /** Call once each server tick; keeps the exact remaining count using long arithmetic. */
    public static void tick() {
        tickWebs();
        tickWaterFills();
        int budget=32;
        for(int jobs=spawns.size();jobs>0&&budget>0;jobs--) {
            var job=spawns.removeFirst();var game=SkyWars2.game();
            if(game!=null&&(!game.playing()||job.level!=game.maps().level()))continue;
            if(paused(job.source)){spawns.addLast(job);continue;}
            if(!job.negative&&game!=null&&game.items().godHedgehog().blocksSilverfish(job.level))continue;
            long remaining=job.remaining;
            while(remaining>0&&budget-->0) {
                var actor=(job.negative?EntityTypes.ENDERMITE:EntityTypes.SILVERFISH).create(job.level,EntitySpawnReason.TRIGGERED);
                if(actor!=null){actor.snapTo(job.at.x,job.at.y,job.at.z,job.level.getRandom().nextFloat()*360,0);actor.setDeltaMovement(job.source.getLookAngle().scale(.3));if(job.author!=null)offspringOwners.put(actor,job.author);job.level.addFreshEntity(actor);}
                remaining--;
            }
            if(remaining>0)spawns.addLast(new SpawnJob(job.level,job.at,job.negative,remaining,job.source,job.author));
        }
    }
    public static void clear() { spawns.clear();webs.clear();fireFills.clear();waterFills.clear();authors.clear();offspringOwners.clear(); }
    public static long queuedSpawns() { long n=0;for(var job:spawns)n+=job.remaining;return n; }
    public static void removed(ServerLevel level,LivingEntity mob,int amplifier,Entity.RemovalReason reason,MobEffect effect) {
        if(reason!=Entity.RemovalReason.KILLED||protectedTarget(mob))return;
        long l=level(amplifier);if(l==0)return;
        if(effect==MobEffects.WIND_CHARGED.value())wind(level,mob,l);
        else if(effect==MobEffects.WEAVING.value())weave(level,mob,l);
        else if(effect==MobEffects.OOZING.value())ooze(level,mob,l);
    }
    private static void wind(ServerLevel level,LivingEntity mob,long l) {
        double radius=4*Math.abs(l),squared=radius*radius;
        var center=mob.position().add(0,mob.getBbHeight()/2,0);
        var game=SkyWars2.game();var owner=author(mob,MobEffects.WIND_CHARGED);
        // Loaded entity iteration avoids the native explosion's radius-dependent chunk/block scan.
        for(var target:level.getAllEntities()) {
            if(target==mob||protectedTarget(target))continue;
            var delta=target.position().subtract(center);double distance=delta.length();
            if(distance==0||distance*distance>squared)continue;
            double force=(1-distance/radius)*Math.abs(l)*(l<0?-1:1);
            var push=delta.scale(force/distance);target.push(push.x,push.y,push.z);target.syncVelocity=true;
            if(game!=null&&owner!=null&&target instanceof LivingEntity living)game.items().indirectKills().displaced(living,owner,true);
        }
    }
    private static void ooze(ServerLevel level,LivingEntity mob,long l) {
        for(int i=0;i<2;i++) {
            AbstractCubeMob slime=(l<0?EntityTypes.MAGMA_CUBE:EntityTypes.SLIME).create(level,EntitySpawnReason.TRIGGERED);
            if(slime!=null){slime.setSize(oozeSize(l),true);slime.snapTo(mob.getX(),mob.getY()+.5,mob.getZ(),level.getRandom().nextFloat()*360,0);var owner=author(mob,MobEffects.OOZING);if(owner!=null)offspringOwners.put(slime,owner);level.addFreshEntity(slime);}
        }
    }
    public static int oozeSize(long l) { return 2+63-Long.numberOfLeadingZeros(Math.abs(l)); }
    private static final ArrayDeque<WebJob> webs=new ArrayDeque<>();
    private static final class WebJob {
        final ServerLevel level;final BlockPos center;final int radius;final boolean negative;final LivingEntity source;
        long remaining;int cursor;
        WebJob(ServerLevel level,LivingEntity source,long l){this.level=level;this.source=source;center=source.blockPosition();radius=Math.min(50,(int)Math.ceil(Math.sqrt(Math.abs(l))));negative=l<0;remaining=2*Math.abs(l);}
    }
    private static void weave(ServerLevel level,LivingEntity mob,long l) {
        if(!(mob instanceof Player)&&!level.getGameRules().get(GameRules.MOB_GRIEFING))return;
        webs.add(new WebJob(level,mob,l));
    }
    public static void tickWebs() {
        int budget=2048;
        for(int n=webs.size();n>0&&budget>0;n--) {
            var job=webs.removeFirst();var game=SkyWars2.game();
            if(game!=null&&(!game.playing()||job.level!=game.maps().level()))continue;
            if(paused(job.source)){webs.addLast(job);continue;}
            int side=2*job.radius+1,total=side*side*side;
            while(job.cursor<total&&job.remaining>0&&budget-->0) {
                int c=job.cursor++;var pos=job.center.offset(c%side-job.radius,c/(side*side)-job.radius,c/side%side-job.radius);
                if(!job.level.hasChunkAt(pos))continue;var state=job.level.getBlockState(pos);
                if(job.negative?state.is(Blocks.COBWEB):state.canBeReplaced()&&job.level.getBlockState(pos.below()).isFaceSturdy(job.level,pos.below(),Direction.UP)) {
                    job.level.setBlockAndUpdate(pos,(job.negative?Blocks.AIR:Blocks.COBWEB).defaultBlockState());job.remaining--;
                }
            }
            if(job.cursor<total&&job.remaining>0)webs.addLast(job);
        }
    }
}
