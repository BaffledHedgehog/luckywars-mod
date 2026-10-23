package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The finite Paper storm, driven by one shared update rather than one scheduled task per block. */
public final class LbcAntigravity {
    public static final String ACTOR="skywars2.antigravity_block";
    private final GameController game;
    private final List<Storm> storms=new ArrayList<>();
    private final Map<Display.BlockDisplay,Lifted> displays=new LinkedHashMap<>();
    private record Storm(ServerLevel level,Vec3 origin,ServerPlayer author,String epoch,int[] age) { }
    private static final class Lifted {
        final Storm storm;
        double speed;
        int stale;
        Lifted(Storm storm) { this.storm=storm; }
    }
    public LbcAntigravity(GameController game) { this.game=game; }
    private boolean active(ServerLevel level,String epoch) {
        return game.playing() && game.stage()!=GameController.Stage.ENDING
                && level==game.maps().level() && game.items().currentEpoch(epoch);
    }
    public boolean ready(ItemEntity token) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcAntigravityDropItem.matches(token.getItem())
                || !(token.level() instanceof ServerLevel level) || !active(level,game.items().epoch())
                || game.items().timeStop().stopped(level)) return false;
        for(var tag:token.entityTags())
            if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        return resident(level,token.blockPosition());
    }
    private static boolean resident(ServerLevel level,BlockPos center) {
        for(int x=(center.getX()-1)>>4;x<=(center.getX()+1)>>4;x++)
            for(int z=(center.getZ()-1)>>4;z<=(center.getZ()+1)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    public boolean spawn(ItemEntity token) {
        if(!ready(token)) return false;
        var level=(ServerLevel)token.level();
        game.items().markCurrentItemEpoch(token);
        var origin=Vec3.atLowerCornerOf(token.blockPosition());
        var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        var storm=new Storm(level,origin,author,game.items().epoch(),new int[1]);
        storms.add(storm);
        launch(storm,token.blockPosition());
        for(var player:level.players()) if(eligible(player) && player.position().distanceToSqr(origin)<=17*17) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0,40,10));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("antigravity_started")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("antigravity_started_sub")));
        }
        return true;
    }
    private boolean eligible(LivingEntity entity) { return game.items().wands().validTarget(entity); }
    private void launch(Storm storm,BlockPos center) {
        if(!resident(storm.level,center)) return;
        Runnable changes=()->{
            for(var mutable:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {
                var pos=mutable.immutable();
                if(storm.level.isOutsideBuildHeight(pos)) continue;
                var state=storm.level.getBlockState(pos);
                if(!state.isSolid()) continue;
                var display=EntityTypes.BLOCK_DISPLAY.create(storm.level,EntitySpawnReason.COMMAND);
                if(display==null) continue;
                display.setBlockState(state);
                display.setViewRange(128); display.setShadowRadius(0); display.setShadowStrength(0);
                display.setBillboardConstraints(Display.BillboardConstraints.FIXED);
                display.setTransformationInterpolationDuration(1); display.setPosRotInterpolationDuration(1);
                display.snapTo(pos.getX()-.5,pos.getY(),pos.getZ()-.5,0,0);
                display.entityTags().add(ACTOR); game.items().markCurrentItemEpoch(display);
                displays.put(display,new Lifted(storm));
                game.items().timeStop().prepareFrozen(display);
                if(!storm.level.addFreshEntity(display)) { displays.remove(display); display.discard(); continue; }
                // CraftBlock.setType(AIR,false) removes the BE before its no-physics replacement.
                // Reproduce that source behavior instead of triggering native inventory spills.
                storm.level.removeBlockEntity(pos);
                storm.level.setBlock(pos,Blocks.AIR.defaultBlockState(),530);
            }
        };
        if(storm.author!=null) game.items().indirectKills().trapChanges(storm.level,storm.author,true,changes);
        else changes.run();
    }
    public void tick() {
        // Existing displays move first: a cube emitted below starts moving next tick, like the source scheduler.
        for(var entry:List.copyOf(displays.entrySet())) {
            var display=entry.getKey(); var lifted=entry.getValue(); var storm=lifted.storm;
            if(display.isRemoved()) { displays.remove(display); continue; }
            if(!active(storm.level,storm.epoch)) { displays.remove(display); display.discard(); continue; }
            if(game.items().timeStop().stopped(storm.level)) continue;
            if(lifted.speed<3) lifted.speed+=.04;
            var next=display.position().add(0,lifted.speed,0);
            display.setPos(next);
            for(var living:storm.level.getEntitiesOfClass(LivingEntity.class,box(next,1,1,1),this::eligible))
                lift(living,lifted.speed+.2,storm.author);
            boolean nearby=false;
            var range=box(next,80,110,80);
            for(var player:storm.level.players()) if(eligible(player) && range.intersects(player.getBoundingBox())) { nearby=true; break; }
            lifted.stale=nearby?0:lifted.stale+1;
            if(lifted.stale>=20) { displays.remove(display); display.discard(); }
        }
        for(var storm:List.copyOf(storms)) {
            if(!active(storm.level,storm.epoch)) { storms.remove(storm); continue; }
            if(game.items().timeStop().stopped(storm.level)) continue;
            int age=++storm.age[0];
            if(age%10==0) pulse(storm);
            if(age>=300) storms.remove(storm);
        }
    }
    private void pulse(Storm storm) {
        var random=storm.level.getRandom();
        var at=storm.origin.add(random.nextDouble()*32-16,0,random.nextDouble()*32-16);
        launch(storm,BlockPos.containing(at));
        storm.level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,at.x+.5,at.y+1,at.z+.5,25,1,1,1,0);
        sound(storm.level,at,SoundEvents.PARROT_FLY); sound(storm.level,at,SoundEvents.STONE_BREAK);
        for(var living:storm.level.getEntitiesOfClass(LivingEntity.class,box(storm.origin,17,60,17),this::eligible)) {
            lift(living,.55,storm.author);
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,30,0,true,false,false),storm.author);

        }
    }
    private void lift(LivingEntity living,double speed,ServerPlayer author) {
        if(game.items().frost().isFrozen(living)) return;
        var motion=living.getDeltaMovement();
        if(motion.y>=speed) return;
        living.setDeltaMovement(motion.x,speed,motion.z); living.needsSync=true; living.syncVelocity=true;
        if(author!=null) game.items().indirectKills().displaced(living,author,true);
    }
    private static AABB box(Vec3 at,double x,double y,double z) {
        return new AABB(at.x-x,at.y-y,at.z-z,at.x+x,at.y+y,at.z+z);
    }
    private static void sound(ServerLevel level,Vec3 at,SoundEvent event) {
        var holder=Holder.direct(event); double range=event.getRange(1.5f); long seed=level.getRandom().nextLong();
        for(var player:level.players()) if(player.position().distanceToSqr(at)<=range*range)
            player.connection.send(new ClientboundSoundPacket(holder,SoundSource.BLOCKS,at.x,at.y,at.z,1.5f,1,seed));
    }
    public void loaded(Entity entity) {
        if(entity instanceof Display.BlockDisplay display && entity.entityTags().contains(ACTOR) && !displays.containsKey(display))
            display.discard(); // A reloaded visual without its live motion state cannot linger indefinitely.
    }
    public void unloaded(Entity entity) { displays.remove(entity); }
    public void stop() {
        storms.clear(); var old=List.copyOf(displays.keySet()); displays.clear(); old.forEach(Entity::discard);
    }
    public int stormCount() { return storms.size(); }
    public int displayCount() { return displays.size(); }
    public List<Display.BlockDisplay> snapshot() { return List.copyOf(displays.keySet()); }
}
