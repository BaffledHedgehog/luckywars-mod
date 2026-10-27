package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The finite Paper storm, driven by one shared update rather than one scheduled task per block. */
public final class LbcAntigravity {
    public static final String ACTOR="skywars2.antigravity_block";
    public static final int DURATION_TICKS=1200;
    public interface PlayerTransport { boolean skywars2$hasPendingTeleport(); }
    private final GameController game;
    private final List<Storm> storms=new ArrayList<>();
    private final Map<Display.BlockDisplay,Lifted> displays=new LinkedHashMap<>();
    private final List<Platform> platforms=new ArrayList<>();
    private final Map<LivingEntity,Passenger> riders=new java.util.IdentityHashMap<>();
    private record Storm(ServerLevel level,Vec3 origin,ServerPlayer author,String epoch,int[] age,
                         List<BlockPos> columns) { }
    private static final class Lifted {
        final Platform platform;
        final Vec3 origin;
        final List<AABB> surfaces;
        int stale;
        Lifted(Platform platform,Vec3 origin,List<AABB> surfaces) {this.platform=platform;this.origin=origin;this.surfaces=surfaces;}
    }
    private static final class Platform {
        final Storm storm;
        final List<Display.BlockDisplay> cells=new ArrayList<>();
        final List<Passenger> passengers=new ArrayList<>();
        double speed,offset;
        Platform(Storm storm){this.storm=storm;}
    }
    private static final class Passenger {
        final LivingEntity entity;
        final Platform platform;
        boolean controlled,detached,impulseApplied;
        double appliedSpeed,localTop,landingDebt;
        Vec3 lastImpulse;
        Passenger(LivingEntity entity,Platform platform,double top) {this.entity=entity;this.platform=platform;localTop=top-platform.offset;}
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
        var storm=new Storm(level,origin,author,game.items().epoch(),new int[1],new ArrayList<>());
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
    private static boolean sourceColumn(Storm storm,BlockPos pos) {
        var origin=BlockPos.containing(storm.origin);
        return pos.getX()>=origin.getX()-16 && pos.getX()<origin.getX()+16
                && pos.getZ()>=origin.getZ()-16 && pos.getZ()<origin.getZ()+16;
    }
    private static BlockPos terrain(Storm storm,BlockPos sampled) {
        if(resident(storm.level,sampled)) {
            for(var pos:BlockPos.betweenClosed(sampled.offset(-1,-1,-1),sampled.offset(1,1,1)))
                if(sourceColumn(storm,pos) && !storm.level.isOutsideBuildHeight(pos)
                        && storm.level.getBlockState(pos).isSolid()) return sampled;
            var pos=surface(storm.level,sampled.getX(),sampled.getZ(),sampled.getY());
            if(pos!=null) return sampled.atY(pos.getY());
        }
        // Revalidate the sparse index first. A drained/stale index needs one fresh resident-column
        // sweep: blocks may have appeared in columns which were empty when it was built.
        boolean refreshed=false;
        while(true) {
            while(!storm.columns.isEmpty()) {
                int index=storm.level.getRandom().nextInt(storm.columns.size());
                var center=storm.columns.get(index);
                if(resident(storm.level,center)) {
                    var pos=surface(storm.level,center.getX(),center.getZ(),center.getY());
                    if(pos!=null) return center.atY(pos.getY());
                }
                storm.columns.remove(index);
            }
            if(refreshed) return null; // Confirmed empty now, rather than one failed random sample.
            refreshed=true;
            var origin=BlockPos.containing(storm.origin);
            for(int x=-16;x<16;x++) for(int z=-16;z<16;z++) {
                var center=origin.offset(x,0,z);
                if(resident(storm.level,center) && surface(storm.level,center.getX(),center.getZ(),center.getY())!=null)
                    storm.columns.add(center);
            }
        }
    }
    private void launch(Storm storm,BlockPos center) {
        if(!resident(storm.level,center)) return;
        // Snapshot actual support before any block is cleared. One broad query per cube, not per display/tick.
        var candidates=storm.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(Vec3.atLowerCornerOf(center.offset(-1,-1,-1)),Vec3.atLowerCornerOf(center.offset(2,4,2))),this::eligible);
        var platform=new Platform(storm);
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
                var surfaces=state.getCollisionShape(storm.level,pos,CollisionContext.empty()).toAabbs();
                displays.put(display,new Lifted(platform,display.position(),surfaces));
                game.items().timeStop().prepareFrozen(display);
                if(!storm.level.addFreshEntity(display)) { displays.remove(display); display.discard(); continue; }
                platform.cells.add(display);
                // CraftBlock.setType(AIR,false) removes the BE before its no-physics replacement.
                // Reproduce that source behavior instead of triggering native inventory spills.
                storm.level.removeBlockEntity(pos);
                storm.level.setBlock(pos,Blocks.AIR.defaultBlockState(),530);
            }
        };
        if(storm.author!=null) game.items().indirectKills().trapChanges(storm.level,storm.author,true,changes);
        else changes.run();
        if(!platform.cells.isEmpty()) {
            platforms.add(platform);
            for(var living:candidates) {
                double top=supportTop(living,platform,0,living.getBoundingBox());
                if(Math.abs(living.getBoundingBox().minY-top)<=.0625)
                    platform.passengers.add(new Passenger(living,platform,top));
            }
        }
    }
    public void tick() {
        // All blocks of one captured cube share motion and one union of supporting surfaces.
        var carried=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<LivingEntity,Boolean>());
        for(var platform:List.copyOf(platforms)) {
            var storm=platform.storm;
            platform.cells.removeIf(d->d.isRemoved()||!displays.containsKey(d));
            if(!active(storm.level,storm.epoch)||platform.cells.isEmpty()) {
                for(var d:List.copyOf(platform.cells)){displays.remove(d);d.discard();}
                platforms.remove(platform);continue;
            }
            if(game.items().timeStop().stopped(storm.level))continue;
            if(platform.speed<3)platform.speed+=.04;
            catchFalling(platform);
            for(var passenger:List.copyOf(platform.passengers)) {
                var living=passenger.entity;
                if(passenger.detached||carried.contains(living))continue;
                if(living.level()!=storm.level||!eligible(living)||game.items().frost().isFrozen(living)
                        ||jumping(living)||living.isPassenger()
                        ||living instanceof ServerPlayer player&&(!player.connection.hasClientLoaded()
                            ||((PlayerTransport)player.connection).skywars2$hasPendingTeleport())
                        ||passenger.impulseApplied&&(living.getDeltaMovement().y>passenger.appliedSpeed+.125
                        ||living.verticalCollision&&!living.onGround())) {
                    detach(passenger);continue;
                }
                double top=living instanceof ServerPlayer?playerTop(passenger,living.getBoundingBox())
                        :supportTop(living,platform,platform.offset,living.getBoundingBox());
                double gap=living.getBoundingBox().minY-top;
                if(passenger.landingDebt>0&&Double.isFinite(top))
                    passenger.landingDebt=Math.min(passenger.landingDebt,Math.max(0,top-living.getBoundingBox().minY)+.25);
                double below=passenger.controlled?belowAllowance(living,platform)+passenger.landingDebt:.0625;
                double above=living instanceof ServerPlayer?1.5:.625;
                if(!Double.isFinite(top)||gap>above||gap< -below
                        ||!passenger.controlled&&living.getDeltaMovement().y>platform.speed+.125) {
                    detach(passenger);continue;
                }
                double speed=Math.max(0,top+platform.speed-living.getBoundingBox().minY);
                passenger.controlled=true;passenger.localTop=top-platform.offset;riders.put(living,passenger);
                if(living instanceof ServerPlayer player)carryPlayer(player,top,passenger);
                else {lift(living,speed,storm.author);passenger.appliedSpeed=speed;passenger.impulseApplied=true;}
                carried.add(living);
                living.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,30,0,true,false,false),storm.author);
            }
            platform.passengers.removeIf(passenger->passenger.detached);
            platform.offset+=platform.speed;
            for(var display:List.copyOf(platform.cells)) {
                var lifted=displays.get(display);if(lifted==null)continue;
                var next=lifted.origin.add(0,platform.offset,0);display.setPos(next);
                boolean nearby=false;var range=box(next,80,110,80);
                for(var player:storm.level.players())if(eligible(player)&&range.intersects(player.getBoundingBox())){nearby=true;break;}
                lifted.stale=nearby?0:lifted.stale+1;
                if(lifted.stale>=20){displays.remove(display);display.discard();}
            }
        }
        riders.entrySet().removeIf(e->!supported(e.getKey()));
        for(var storm:List.copyOf(storms)) {
            if(!active(storm.level,storm.epoch)){storms.remove(storm);continue;}
            if(game.items().timeStop().stopped(storm.level))continue;
            int age=++storm.age[0];
            if(age%10==0 && !pulse(storm)){storms.remove(storm);continue;}
            if(age>=DURATION_TICKS)storms.remove(storm);
        }
    }
    private static boolean jumping(LivingEntity living) {
        return living.isJumping()||living instanceof ServerPlayer player&&player.getLastClientInput().jump();
    }
    private void detach(Passenger passenger) {
        passenger.detached=true;
        if(riders.get(passenger.entity)==passenger)riders.remove(passenger.entity);
    }
    private static double latencyTicks(ServerPlayer player){
        return Math.max(0,Math.min(5,player.connection.latency()/100d));
    }
    private static double belowAllowance(LivingEntity living,Platform platform){
        return living instanceof ServerPlayer player
                ?Math.max(1.25,platform.speed*(latencyTicks(player)+2)+Math.abs(player.getKnownMovement().y)*latencyTicks(player)+.5)
                :platform.speed+.625;
    }
    private void carryPlayer(ServerPlayer player,double top,Passenger passenger) {
        var platform=passenger.platform;var motion=player.getDeltaMovement();var known=player.getKnownMovement();
        double modifier=player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.AIR_DRAG_MODIFIER);
        double verticalDrag=net.minecraft.util.Mth.clamp(1-(1-.98)*modifier,.05,1);
        double horizontalDrag=net.minecraft.util.Mth.clamp(1-(1-.91)*modifier,0,1);
        // Position reports are a one-way trip old. Predict the client's current height
        // instead of chasing that obsolete Y with a teleport or ever-larger impulse.
        double gap=player.getBoundingBox().minY+known.y*latencyTicks(player)-top;
        double gain=.3/(1+latencyTicks(player)*.35);
        double catchup=Math.max(.75,Math.min(2,passenger.landingDebt*.5));
        double feedback=net.minecraft.util.Mth.clamp((.15-gap)*gain,-.25,catchup);
        double rise=Math.max(.01,platform.speed+feedback);
        double compensation=net.minecraft.util.Mth.clamp(rise/verticalDrag-rise+player.getGravity(),0,.2);
        double y=rise+compensation;
        // Known movement is displacement before air drag. Restore a small part of
        // that drag without accumulating our own previous impulse as fresh input.
        double horizontal=Math.min(1,horizontalDrag*1.04);
        Vec3 xz=new Vec3(known.x*horizontal,0,known.z*horizontal);
        boolean external=passenger.lastImpulse==null||Math.abs(motion.x-passenger.lastImpulse.x)>1e-6
                ||Math.abs(motion.z-passenger.lastImpulse.z)>1e-6;
        if(external&&motion.horizontalDistanceSqr()>xz.horizontalDistanceSqr())
            xz=new Vec3(motion.x*1.04,0,motion.z*1.04);
        Vec3 impulse=new Vec3(xz.x,y,xz.z);
        player.setDeltaMovement(impulse);passenger.appliedSpeed=y;passenger.lastImpulse=impulse;passenger.impulseApplied=true;
        player.setOnGround(false);player.resetFallDistance();
        // This is an ordinary native velocity packet. No position, rotation, ACK
        // state or requested client coordinate is changed by moving-platform carry.
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(),impulse));
        if(platform.storm.author!=null)game.items().indirectKills().displaced(player,platform.storm.author,true);
    }
    /** A falling body can land after launch; contact is a swept relative crossing. */
    private void catchFalling(Platform platform){
        AABB area=null;
        for(var display:platform.cells){var cell=displays.get(display);if(cell==null)continue;
            for(var shape:cell.surfaces){var box=shape.move(cell.origin.add(0,platform.offset,0));area=area==null?box:area.minmax(box);}}
        if(area==null)return;
        for(var living:platform.storm.level.getEntitiesOfClass(LivingEntity.class,area.inflate(.5,12,.5),this::eligible)){
            if(riders.containsKey(living)||!admissible(living)
                    ||platform.passengers.stream().anyMatch(p->p.entity==living&&!p.detached))continue;
            var movement=living instanceof ServerPlayer player?player.getKnownMovement():living.getDeltaMovement();
            if(movement.y>=-.001)continue;
            var before=living.getBoundingBox();
            var after=before.move(movement.x,Math.max(-12,movement.y),movement.z);
            admitCrossing(living,platform,before,after,platform.offset,platform.offset+platform.speed);
        }
    }
    private boolean admissible(LivingEntity living){
        return eligible(living)&&!game.items().frost().isFrozen(living)&&!jumping(living)&&!living.isPassenger()
                &&(!(living instanceof ServerPlayer player)||player.connection.hasClientLoaded()
                    &&!((PlayerTransport)player.connection).skywars2$hasPendingTeleport());
    }
    private void admitCrossing(LivingEntity living,Platform platform,AABB before,AABB after,double oldOffset,double newOffset){
        if(riders.containsKey(living)||!admissible(living))return;
        double top=supportTop(platform,newOffset,after,Double.POSITIVE_INFINITY);
        double oldTop=top-(newOffset-oldOffset);
        if(!Double.isFinite(top)||before.minY<oldTop-.0625||after.minY>top+.0625
                ||after.minY-before.minY>=newOffset-oldOffset)return;
        double liveTop=top+platform.offset-newOffset;
        var passenger=new Passenger(living,platform,liveTop);passenger.controlled=true;
        // A fast fall can cross a whole block between two accepted position reports.
        // Retain that measured contact debt until velocity naturally recovers the surface.
        passenger.landingDebt=Math.min(16,Math.max(0,liveTop-after.minY)+.25);
        platform.passengers.add(passenger);riders.put(living,passenger);
        living.resetFallDistance();
    }
    private boolean pulse(Storm storm) {
        var random=storm.level.getRandom();
        var at=storm.origin.add(random.nextDouble()*32-16,0,random.nextDouble()*32-16);
        var center=terrain(storm,BlockPos.containing(at));
        if(center==null) return false; // Retire the center; already flying platforms keep their own lifecycle.
        launch(storm,center);
        at=new Vec3(center.getX()+at.x-Math.floor(at.x),center.getY(),center.getZ()+at.z-Math.floor(at.z));
        storm.level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,at.x+.5,at.y+1,at.z+.5,25,1,1,1,0);
        sound(storm.level,at,SoundEvents.PARROT_FLY); sound(storm.level,at,SoundEvents.STONE_BREAK);
        return true;
    }
    private double supportTop(LivingEntity living,Platform platform,double offset,AABB feet) {
        return supportTop(platform,offset,feet,living.getBoundingBox().minY+Math.max(.0625,living.maxUpStep())+1e-6);
    }
    /** Follow the admitted player's surface even if a delayed position is inside a lower layer. */
    private double playerTop(Passenger passenger,AABB feet) {
        var platform=passenger.platform;
        return supportTop(platform,platform.offset,feet,
                platform.offset+passenger.localTop+Math.max(.0625,passenger.entity.maxUpStep())+1e-6);
    }
    private double supportTop(Platform platform,double offset,AABB feet,double maximumTop) {
        double best=Double.NEGATIVE_INFINITY;
        for(var display:platform.cells) {
            var cell=displays.get(display);if(cell==null||display.isRemoved())continue;
            for(var shape:cell.surfaces) {
                double top=cell.origin.y+offset+shape.maxY;
                if(top>maximumTop)continue;
                if(feet.maxX>cell.origin.x+shape.minX+1e-6&&feet.minX<cell.origin.x+shape.maxX-1e-6
                        &&feet.maxZ>cell.origin.z+shape.minZ+1e-6&&feet.minZ<cell.origin.z+shape.maxZ-1e-6)
                    best=Math.max(best,top);
            }
        }
        return best;
    }
    public boolean supported(Entity entity) {
        if(!(entity instanceof LivingEntity living))return false;
        var rider=riders.get(living);
        if(rider==null||rider.detached||!rider.controlled||!platforms.contains(rider.platform)
                ||living.level()!=rider.platform.storm.level||!eligible(living)||jumping(living)
                ||living.isPassenger()||game.items().frost().isFrozen(living)||!active(rider.platform.storm.level,rider.platform.storm.epoch))return false;
        double top=living instanceof ServerPlayer?playerTop(rider,living.getBoundingBox())
                :supportTop(living,rider.platform,rider.platform.offset,living.getBoundingBox());
        double gap=living.getBoundingBox().minY-top;
        return Double.isFinite(top)&&gap<=(living instanceof ServerPlayer?1.5:.625)
                &&gap>=-belowAllowance(living,rider.platform)-rider.landingDebt;
    }
    /** Add real server collision surfaces only for admitted riders, preserving native terrain. */
    public List<VoxelShape> collisions(Entity entity,AABB area,List<VoxelShape> original) {
        // Displays have no client collision. Server-only walls would reject free X/Z
        // movement from delayed player reports; their support is exclusively velocity.
        if(entity instanceof ServerPlayer||!supported(entity))return original;
        var platform=riders.get((LivingEntity)entity).platform;List<VoxelShape> out=null;
        for(var display:platform.cells) {
            var cell=displays.get(display);if(cell==null||display.isRemoved())continue;
            for(var shape:cell.surfaces) {
                var world=shape.move(cell.origin.add(0,platform.offset,0));
                if(area.intersects(world)){if(out==null)out=new ArrayList<>(original);out.add(Shapes.create(world));}
            }
        }
        return out==null?original:out;
    }
    /** Observe falling contact without rewriting any coordinate or rotation in the packet. */
    public ServerboundMovePlayerPacket supportInput(ServerPlayer player,ServerboundMovePlayerPacket packet) {
        if(!packet.hasPosition()||!admissible(player))return packet;
        double x=packet.getX(player.getX()),y=packet.getY(player.getY()),z=packet.getZ(player.getZ());
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return packet;
        var rider=riders.get(player);
        var before=player.getBoundingBox();var after=before.move(x-player.getX(),y-player.getY(),z-player.getZ());
        if(rider!=null){
            if(supported(player)&&Double.isFinite(playerTop(rider,after)))return packet;
            detach(rider); // The same descending report may land on another moving piece.
        }
        if(y>=player.getY()-.001)return packet;
        double lag=latencyTicks(player);
        for(var platform:platforms){
            if(platform.storm.level!=player.level()||!active(platform.storm.level,platform.storm.epoch)
                    ||game.items().timeStop().stopped(platform.storm.level))continue;
            double current=platform.offset-platform.speed*lag;
            admitCrossing(player,platform,before,after,current-platform.speed,current);
            if(riders.containsKey(player))break;
        }
        return packet;
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
        storms.clear();platforms.clear();riders.clear();var old=List.copyOf(displays.keySet());displays.clear();old.forEach(Entity::discard);
    }
    public int stormCount() { return storms.size(); }
    public int displayCount() { return displays.size(); }
    public List<Display.BlockDisplay> snapshot() { return List.copyOf(displays.keySet()); }
}
