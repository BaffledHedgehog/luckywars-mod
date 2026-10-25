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
import net.minecraft.world.phys.shapes.CollisionContext;

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
    private record Storm(ServerLevel level,Vec3 origin,ServerPlayer author,String epoch,int[] age,
                         List<BlockPos> columns,boolean[] indexed) { }
    private static final class Lifted {
        final Storm storm;
        final List<Passenger> passengers;
        double speed;
        int stale;
        Lifted(Storm storm,List<Passenger> passengers) { this.storm=storm; this.passengers=passengers; }
    }
    private static final class Passenger {
        final LivingEntity entity;
        final List<AABB> surfaces;
        boolean controlled,detached;
        double appliedSpeed;
        Passenger(LivingEntity entity,List<AABB> surfaces) {this.entity=entity;this.surfaces=surfaces;}
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
        var storm=new Storm(level,origin,author,game.items().epoch(),new int[1],new ArrayList<>(),new boolean[1]);
        storms.add(storm);
        var initial=terrain(storm,token.blockPosition());
        if(initial!=null) launch(storm,initial);
        for(var player:level.players()) if(eligible(player) && player.position().distanceToSqr(origin)<=17*17) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0,40,10));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("antigravity_started")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("antigravity_started_sub")));
        }
        return true;
    }
    private boolean eligible(LivingEntity entity) { return game.items().wands().validTarget(entity); }
    /** No chunk tickets: search resident nonempty sections for the closest actual column surface. */
    private static BlockPos surface(ServerLevel level,int x,int z,int y) {
        var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);
        if(chunk==null) return null;
        BlockPos best=null; long distance=Long.MAX_VALUE;
        var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++) {
            var section=sections[i];
            if(section.hasOnlyAir()) continue;
            int base=chunk.getSectionYFromSectionIndex(i)<<4;
            if((y<base && (long)base-y>distance) || (y>base+15 && (long)y-base-15>distance)) continue;
            for(int localY=15;localY>=0;localY--) {
                int atY=base+localY; long delta=Math.abs((long)atY-y);
                if(delta>=distance || !section.getBlockState(x&15,localY,z&15).isSolid()) continue;
                var pos=new BlockPos(x,atY,z);
                if(level.getBlockState(pos.above()).isSolid()) continue;
                best=pos; distance=delta;
            }
        }
        return best;
    }
    private static BlockPos nearbySurface(ServerLevel level,BlockPos center) {
        if(!resident(level,center)) return null;
        BlockPos best=null; long distance=Long.MAX_VALUE;
        for(int x=center.getX()-1;x<=center.getX()+1;x++) for(int z=center.getZ()-1;z<=center.getZ()+1;z++) {
            var pos=surface(level,x,z,center.getY());
            if(pos!=null && Math.abs((long)pos.getY()-center.getY())<distance) {
                best=center.atY(pos.getY()); distance=Math.abs((long)pos.getY()-center.getY());
            }
        }
        return best;
    }
    private static BlockPos terrain(Storm storm,BlockPos sampled) {
        if(resident(storm.level,sampled)) {
            for(var pos:BlockPos.betweenClosed(sampled.offset(-1,-1,-1),sampled.offset(1,1,1)))
                if(!storm.level.isOutsideBuildHeight(pos) && storm.level.getBlockState(pos).isSolid()) return sampled;
            var nearby=nearbySurface(storm.level,sampled);
            if(nearby!=null) return nearby;
        }
        // Build the sparse fallback only on a miss; revalidate selected columns against live terrain.
        if(!storm.indexed[0]) {
            storm.indexed[0]=true;
            var origin=BlockPos.containing(storm.origin);
            for(int x=-16;x<16;x++) for(int z=-16;z<16;z++) {
                var center=origin.offset(x,0,z);
                if(resident(storm.level,center) && surface(storm.level,center.getX(),center.getZ(),center.getY())!=null)
                    storm.columns.add(center);
            }
        }
        while(!storm.columns.isEmpty()) {
            int index=storm.level.getRandom().nextInt(storm.columns.size());
            var center=storm.columns.get(index);
            if(resident(storm.level,center)) {
                var pos=surface(storm.level,center.getX(),center.getZ(),center.getY());
                if(pos!=null) return center.atY(pos.getY());
            }
            storm.columns.remove(index);
        }
        return null;
    }
    private void launch(Storm storm,BlockPos center) {
        if(!resident(storm.level,center)) return;
        // Snapshot actual support before any block is cleared. One broad query per cube, not per display/tick.
        var candidates=storm.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(Vec3.atLowerCornerOf(center.offset(-1,-1,-1)),Vec3.atLowerCornerOf(center.offset(2,4,2))),this::eligible);
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
                // Native BlockDisplay renders the unit model from its lower corner.
                display.snapTo(pos.getX(),pos.getY(),pos.getZ(),0,0);
                display.entityTags().add(ACTOR); game.items().markCurrentItemEpoch(display);
                var passengers=new ArrayList<Passenger>();
                for(var living:candidates) {
                    var surfaces=state.getCollisionShape(storm.level,pos,CollisionContext.of(living)).toAabbs();
                    if(touches(living,display.position(),surfaces)) passengers.add(new Passenger(living,surfaces));
                }
                displays.put(display,new Lifted(storm,passengers));
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
        var carried=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<LivingEntity,Boolean>());
        for(var entry:List.copyOf(displays.entrySet())) {
            var display=entry.getKey(); var lifted=entry.getValue(); var storm=lifted.storm;
            if(display.isRemoved()) { displays.remove(display); continue; }
            if(!active(storm.level,storm.epoch)) { displays.remove(display); display.discard(); continue; }
            if(game.items().timeStop().stopped(storm.level)) continue;
            if(lifted.speed<3) lifted.speed+=.04;
            var previous=display.position();
            var next=previous.add(0,lifted.speed,0);
            for(var passenger:lifted.passengers) {
                var living=passenger.entity;
                if(passenger.detached || carried.contains(living))continue;
                if(living.level()!=storm.level || !eligible(living) || game.items().frost().isFrozen(living)
                        || living.isJumping() || living.isPassenger()
                        || (passenger.controlled && (living.getDeltaMovement().y>passenger.appliedSpeed+.125
                        || living.verticalCollision && !living.onGround()))) {
                    passenger.detached=true;continue;
                }
                double top=supportTop(living,previous,passenger.surfaces);
                double gap=living.getBoundingBox().minY-top;
                // A captured passenger may lag one native movement step, but never rejoin from above/at distance.
                double below=passenger.controlled?lifted.speed+.125:.0625;
                if(!Double.isFinite(top) || gap>.0625 || gap< -below
                        || (!passenger.controlled && living.getDeltaMovement().y>lifted.speed+.125)) {
                    passenger.detached=true;continue;
                }
                // Air travel moves first, then applies gravity/drag. Targeting the next top needs no teleport,
                // onGround forgery or .2 launch: the following native move reaches the same moving surface.
                double speed=Math.max(0,top+lifted.speed-living.getBoundingBox().minY);
                lift(living,speed,storm.author);
                passenger.appliedSpeed=speed;passenger.controlled=true;carried.add(living);
                living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,30,0,true,false,false),storm.author);
            }
            display.setPos(next);
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
        var center=terrain(storm,BlockPos.containing(at));
        if(center==null) return; // An empty loaded footprint cannot produce a platform or entity-only pulse.
        launch(storm,center);
        at=new Vec3(center.getX()+at.x-Math.floor(at.x),center.getY(),center.getZ()+at.z-Math.floor(at.z));
        storm.level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,at.x+.5,at.y+1,at.z+.5,25,1,1,1,0);
        sound(storm.level,at,SoundEvents.PARROT_FLY); sound(storm.level,at,SoundEvents.STONE_BREAK);
    }
    private static double supportTop(LivingEntity living,Vec3 origin,List<AABB> surfaces) {
        var feet=living.getBoundingBox();
        double best=Double.NEGATIVE_INFINITY;
        for(var shape:surfaces) if(feet.maxX>origin.x+shape.minX+1e-6 && feet.minX<origin.x+shape.maxX-1e-6
                && feet.maxZ>origin.z+shape.minZ+1e-6 && feet.minZ<origin.z+shape.maxZ-1e-6)
            best=Math.max(best,origin.y+shape.maxY);
        return best;
    }
    /** Initial admission requires real feet contact, before any terrain is cleared. */
    private static boolean touches(LivingEntity living,Vec3 origin,List<AABB> surfaces) {
        return Math.abs(living.getBoundingBox().minY-supportTop(living,origin,surfaces))<=.0625;
    }
    private void lift(LivingEntity living,double speed,ServerPlayer author) {
        if(game.items().frost().isFrozen(living)) return;
        var motion=living.getDeltaMovement();
        if(Math.abs(motion.y-speed)<1e-12) return;
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
