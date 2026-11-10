package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Raw specials.mcfunction projectiles. Native bodies own flight, hits, teleports, clouds and fuse clocks. */
public final class LbcSpecialFireballs {
    private static final String TAG="skywars2.special_fireball", CHILD="skywars2.special_fireball.child";
    private static final String DELTA="skywars2.delta_payload";
    private static final String OWNER="skywars2.special_fireball.owner.";
    private static final String[] FLAGS={"smallfireball","fireball","dragfireball","smallfireball_imba","fireball_imba","dragfireball_imba","ender_pernul","impulse"};
    private final GameController game;
    private final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Snowball> pending=new LinkedHashSet<>();
    private final Set<FireFill> fires=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean restoring;
    public LbcSpecialFireballs(GameController game){this.game=game;}
    public static int variant(ItemStack stack){
        var selected=LbcTmtAbilities.nativePredicatesActive()?LbcTmtAbilities.nativePredicateItem(stack):null;
        var nativeItem=selected==null?stack.getItem():selected;
        if(!(nativeItem instanceof net.minecraft.world.item.SnowballItem)||ItemData.intValue(stack,"special",0)!=1)return 0;
        for(int i=0;i<FLAGS.length;i++)if(ItemData.intValue(stack,FLAGS[i],0)==1)return i+1;
        return 0;
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}

    /** Custom fireballs already publish real positions every tick. Disable only the client's extra
     * extrapolation; their server velocity, native collision and damage remain authoritative. */
    public net.minecraft.network.protocol.Packet<?> clientPacket(ServerPlayer viewer,net.minecraft.network.protocol.Packet<?> packet){
        if(!game.server().isSameThread())return packet;
        if(packet instanceof net.minecraft.network.protocol.game.ClientboundBundlePacket bundle){
            ArrayList<net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>> output=null;
            int unchanged=0;
            for(var child:bundle.subPackets()){
                var corrected=clientPacket(viewer,child);
                if(output==null&&corrected!=child){
                    output=new ArrayList<>();int n=0;
                    for(var prefix:bundle.subPackets()){if(n++==unchanged)break;output.add(prefix);}
                }
                if(output!=null)output.add(gamePacket(corrected));
                unchanged++;
            }
            return output==null?packet:new net.minecraft.network.protocol.game.ClientboundBundlePacket(output);
        }
        int id=packet instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket spawn?spawn.getId():
                packet instanceof net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket motion?motion.id():-1;
        if(id<0||!(viewer.level().getEntity(id) instanceof net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile actor)||!managed(actor))return packet;
        if(packet instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket spawn){
            if(spawn.getMovement().equals(Vec3.ZERO))return packet;
            return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(spawn.getId(),spawn.getUUID(),spawn.getX(),spawn.getY(),spawn.getZ(),
                    spawn.getXRot(),spawn.getYRot(),spawn.getType(),spawn.getData(),Vec3.ZERO,spawn.getYHeadRot());
        }
        var motion=(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket)packet;
        return motion.movement().equals(Vec3.ZERO)?packet:new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(id,Vec3.ZERO);
    }
    @SuppressWarnings("unchecked")
    private static net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener> gamePacket(net.minecraft.network.protocol.Packet<?> packet){
        return (net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>)packet;
    }
    private static boolean horizontal(BlockPos pos){return pos.getX()>=-30000000&&pos.getX()<30000000&&pos.getZ()>=-30000000&&pos.getZ()<30000000;}
    private boolean resident(ServerLevel level,Vec3 at){var pos=BlockPos.containing(at);return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private boolean active(Entity e){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&e.level()==game.maps().level()&&(game.items().currentEpoch(e)||e.entityTags().contains(DELTA)&&game.items().spellSources().source(e)!=null);}
    private boolean frozen(Entity e){return game.items().polymorph().suspended(e)||!game.server().tickRateManager().runsNormally()||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped");}
    /** Native input configures vanilla throw motion once; only final actors enter the world. */
    public boolean use(ServerPlayer owner,net.minecraft.world.InteractionHand hand){
        var stack=owner.getItemInHand(hand);
        if(variant(stack)==0||!game.playing()||game.stage()==GameController.Stage.ENDING||game.maps().busy()
                ||owner.level()!=game.maps().level()||!LbcDimensionItems.ordinary(owner)||!owner.isAlive()||owner.isRemoved()
                ||!game.canUseGameplayItems(owner)||game.isObserver(owner)||game.awaitingRespawn(owner.getUUID())
                ||!game.items().wands().validRoleTarget(owner,false)||game.items().actionsBlocked(owner)
                ||game.items().wands().inputTimeStopped(owner))return false;
        var carrier=new Snowball(owner.level(),owner,stack);
        carrier.shootFromRotation(owner,owner.getXRot(),owner.getYRot(),0,1.5f,1);
        game.items().projectileLevelups().beforePublish(owner,carrier,LbcProjectileLevelups.Launch.SNOWBALL);
        carrier.entityTags().add(game.items().epoch()); // Geometry only: never index this unpublished carrier.
        if(convert(carrier).isEmpty()){carrier.discard();return false;}
        carrier.discard();
        LbcOverload.sound(owner,owner.position(),variant(stack)==8?"minecraft:entity.snowball.throw":"minecraft:item.firecharge.use",1,1,0);
        owner.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(stack.getItem()));
        owner.gameEvent(net.minecraft.world.level.gameevent.GameEvent.PROJECTILE_SHOOT);
        stack.consume(1,owner);return true;
    }
    public void loaded(Entity entity){
        if(restoring||MatchActorCheckpoint.restoring(entity))return;
        // Epoch-less native TNT must regain its existing authority before the registry admission check.
        if(managed(entity)&&entity.entityTags().contains(DELTA))game.items().spellSources().loaded(entity);
        if(managed(entity)){if(active(entity)){actors.add(entity);game.items().timeStop().prepareFrozen(entity);}else entity.discard();return;}
        if(entity instanceof Snowball ball&&variant(ball.getItem())!=0&&ball.getOwner() instanceof ServerPlayer p
                &&game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!game.isObserver(p)
                &&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p)&&!game.items().actionsBlocked(p)&&entity.level()==game.maps().level()){
            game.items().markCurrentItemEpoch(ball);pending.add(ball);
        }
    }
    private <T extends Entity>T insert(ServerPlayer p,T actor,Vec3 at,Vec3 velocity,boolean child){
        actor.setPos(at);actor.setDeltaMovement(velocity);actor.entityTags().add(TAG);if(p!=null)actor.entityTags().add(OWNER+p.getUUID());
        if(child)actor.entityTags().add(CHILD);if(!(actor instanceof PrimedTnt&&actor.entityTags().contains(DELTA)))game.items().markCurrentItemEpoch(actor);
        if(actor instanceof Projectile projectile){projectile.setOwner(p);LbcShooters.orient(projectile);}
        actors.add(actor);game.items().timeStop().prepareFrozen(actor);
        if(!actor.level().addFreshEntity(actor)||actor.isRemoved()){actors.remove(actor);actor.discard();return null;}return actor;
    }
    /** Current firegun uses the native small-fireball factory, lifecycle and hit protection. */
    public Projectile spawnRangedFire(ServerPlayer owner,Vec3 at,Vec3 velocity,Projectile donor){
        if(!resident(owner.level(),at))return null;
        var fire=new SmallFireball(EntityTypes.SMALL_FIREBALL,owner.level());fire.entityTags().addAll(donor.entityTags());fire.entityTags().add("can_deleted");
        LbcTmtAmmoPayloads.copyPayloads(donor,fire);return insert(owner,fire,at,velocity,false);
    }
    public void rangedContact(Projectile shot,LivingEntity target,Vec3 point){if(managed(shot))LbcLegacyRanged.contact(game,shot,target,point);}
    public void rangedGround(Projectile shot,Vec3 point){if(managed(shot))LbcLegacyRanged.ground(game,shot,point);}
    /** Exact source branches 0..18; native bodies retain all fuse and flight clocks. */
    public Entity spawnDelta(LbcSpellSource.Source source,ServerLevel level,Vec3 at,int branch){
        if(source==null||!game.items().currentEpoch(source.epoch())||!resident(level,at)||!horizontal(BlockPos.containing(at)))return null;
        var p=game.server().getPlayerList().getPlayer(source.author());Entity actor;
        if(branch==2){var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setCause(p);actor=bolt;}
        else if(branch>=3&&branch<=7)actor=new LargeFireball(level,p,Vec3.ZERO,new int[]{1,5,10,15,20}[branch-3]);
        else {
            var tnt=new PrimedTnt(level,at.x,at.y,at.z,p);tnt.setFuse(branch==8?0:branch==9||branch>=15?1000:100);
            tnt.setNoGravity(branch==1||branch>=10&&branch<=14);
            if(branch>=9)tnt.entityTags().add("groundbam");
            if(branch>=11){int kind=(branch-11)%4;tnt.entityTags().add(new String[]{"evoker","fire","electro","strong"}[kind]);}
            actor=tnt;
        }
        actor.entityTags().add(DELTA);game.items().spellSources().bind(actor,source);
        // No epoch tag on TNT: the generic thrown-dynamite hook keys on that tag.
        try(var ignored=game.items().spellSources().scope((LbcSpellSource.Source)null,level)){
            return insert(p,actor,at,Vec3.ZERO,false);
        }
    }
    private static double scaled(double value,double read,double write){return Math.floor(value*read)*write;}
    private static Vec3 scaled(Vec3 v,double read,double write){return new Vec3(scaled(v.x,read,write),scaled(v.y,read,write),scaled(v.z,read,write));}
    /** Use the actual snowball's position, rotation and velocity, including any native throw spread. */
    public List<Entity> convert(Snowball ball){
        int kind=variant(ball.getItem());if(kind==0||ball.isRemoved()||!(ball.getOwner() instanceof ServerPlayer p)||!active(ball)||frozen(ball))return List.of();
        var direction=Vec3.directionFromRotation(ball.getXRot(),ball.getYRot());var at=ball.position().add(direction.scale(kind==3||kind==6||kind==7||kind==8?0:1.5));
        if(!resident(p.level(),at)||!resident(p.level(),at.add(0,0,3)))return List.of();
        var result=new ArrayList<Entity>();var motion=ball.getDeltaMovement();
        switch(kind){
            case 1->{var fire=new SmallFireball(EntityTypes.SMALL_FIREBALL,p.level());fire.entityTags().add("can_deleted");result.add(insert(p,fire,at,scaled(motion,10000,.0006),false));}
            case 2,5->{var fire=new LargeFireball(p.level(),p,Vec3.ZERO,kind==5?127:2);fire.setNoGravity(true);fire.setPermanentlyInvulnerable(kind==5);result.add(insert(p,fire,at,scaled(motion,100000,.00000075),false));}
            case 3,6->{var velocity=scaled(motion,100000,.0000005);var fire=new DragonFireball(EntityTypes.DRAGON_FIREBALL,p.level());fire.setNoGravity(true);result.add(insert(p,fire,at,velocity,false));
                if(kind==6)for(int i=0;i<16;i++){var child=new DragonFireball(EntityTypes.DRAGON_FIREBALL,p.level());
                    var v=new Vec3((Math.floor(velocity.x*1000)+p.getRandom().nextInt(61)-30)*.001,(Math.floor(velocity.y*1000)+p.getRandom().nextInt(11)-5)*.001,(Math.floor(velocity.z*1000)+p.getRandom().nextInt(61)-30)*.001);
                    result.add(insert(p,child,at,v,true));}}
            case 4->{var velocity=scaled(motion,10000,.0006);var tnt=new PrimedTnt(p.level(),at.x,at.y,at.z,p);tnt.setFuse(100);tnt.entityTags().addAll(List.of("can_deleted","groundbam","puknul"));result.add(insert(p,tnt,at,velocity,false));
                // Summoned TNT has default rotation [0,0]; the helper's local ^^^3 is world +Z.
                var childAt=at.add(0,0,3);for(int i=0;i<32;i++){var child=new PrimedTnt(p.level(),childAt.x,childAt.y,childAt.z,p);child.setFuse(100);child.setPermanentlyInvulnerable(true);child.entityTags().addAll(List.of("groundbam","evoker","fire","electro","strong"));
                    var v=new Vec3((Math.floor(velocity.x*10)+p.getRandom().nextInt(21)-10)*.1,(Math.floor(velocity.y*10)+p.getRandom().nextInt(21)-10)*.1,(Math.floor(velocity.z*10)+p.getRandom().nextInt(21)-10)*.1);
                    result.add(insert(p,child,childAt,v,true));}}
            case 7->{var velocity=scaled(motion,10000,.00025);
                result.add(insert(p,new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl(EntityTypes.ENDER_PEARL,p.level()),at,velocity,false));
                for(int i=0;i<16;i++){
                    var v=new Vec3((Math.floor(velocity.x*10)+p.getRandom().nextInt(21)-10)*.1,
                            (Math.floor(velocity.y*10)+p.getRandom().nextInt(21)-10)*.1,
                            (Math.floor(velocity.z*10)+p.getRandom().nextInt(21)-10)*.1);
                    result.add(insert(p,new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl(EntityTypes.ENDER_PEARL,p.level()),at,v,true));
                }}
            case 8->{
                var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(p.level(),p,new ItemStack(Items.ARROW),null);
                arrow.setBaseDamage(1);arrow.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.CREATIVE_ONLY;
                result.add(insert(p,arrow,at,scaled(motion,10000,.0006),false));
            }
            default->{}
        }
        result.removeIf(Objects::isNull);
        if(!result.isEmpty()&&ItemData.intValue(ball.getItem(),"tmt_item",0)!=1)ball.discard();
        return List.copyOf(result);
    }
    public LivingEntity owner(Entity e){
        if(!managed(e))return null;
        if(e instanceof Projectile projectile&&projectile.getOwner() instanceof LivingEntity living)return living;
        if(e instanceof PrimedTnt tnt&&tnt.getOwner()!=null)return tnt.getOwner();
        if(e instanceof EvokerFangs fang&&fang.getOwner()!=null)return fang.getOwner();
        if(e instanceof LightningBolt bolt&&bolt.getCause()!=null)return bolt.getCause();
        if(e instanceof AreaEffectCloud cloud&&cloud.getOwner()!=null)return cloud.getOwner();
        for(var tag:e.entityTags())if(tag.startsWith(OWNER))try{return game.server().getPlayerList().getPlayer(UUID.fromString(tag.substring(OWNER.length())));}catch(IllegalArgumentException ignored){}
        return null;
    }
    public boolean canAffect(Entity source,Entity target){
        if(!managed(source))return true;
        if(!active(source)||!game.items().wands().validTarget(target))return false;
        var ranged=LbcLegacyRanged.canAffect(game,source,target);return ranged==null||ranged;
    }
    public boolean explosionTarget(Entity source,Entity target){return canAffect(source,target);}
    /** Native DragonFireball constructs its cloud; retain its physical policy before the load callback. */
    public void inherit(Entity parent,Entity child){
        if(!managed(parent)||!active(parent)||!(child instanceof AreaEffectCloud))return;
        child.entityTags().add(TAG);child.entityTags().add(CHILD);
        for(var tag:parent.entityTags())if(tag.startsWith(OWNER))child.entityTags().add(tag);
        game.items().markCurrentItemEpoch(child);
    }
    public void tick(){
        if(restoring)return;
        for(var ball:List.copyOf(pending)){
            if(ball.isRemoved()||!active(ball)){pending.remove(ball);if(!ball.isRemoved())ball.discard();continue;}
            if(frozen(ball))continue;pending.remove(ball);convert(ball);
        }
        for(var actor:List.copyOf(actors)){
            if(actor.isRemoved()){actors.remove(actor);continue;}if(!active(actor)){actors.remove(actor);actor.discard();continue;}
            if(frozen(actor)||!resident((ServerLevel)actor.level(),actor.position()))continue;
            LbcLegacyRanged.flight(game,actor);
            if(actor instanceof net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile){
                // Client fireballs predict inertia and collisions; correct through the native tracker each tick.
                // Keep the authored launch position and the real projectile's motion/impact authority.
                actor.syncPosition=true;actor.needsSync=true;
            }
            if(actor instanceof PrimedTnt tnt&&tnt.onGround()&&tnt.entityTags().remove("groundbam")){
                tnt.setFuse(2);if(tnt.entityTags().contains(CHILD)||tnt.entityTags().contains(DELTA))landing(tnt);
            }
        }
    }
    private <T extends Entity>T carried(Entity parent,T child){
        child.entityTags().add(TAG);
        if(parent.entityTags().contains(DELTA)){child.entityTags().add(DELTA);game.items().spellSources().bind(child,game.items().spellSources().source(parent));}
        for(var tag:parent.entityTags())if(tag.startsWith(OWNER))child.entityTags().add(tag);
        return child;
    }
    private void landing(PrimedTnt tnt){
        var p=owner(tnt) instanceof ServerPlayer player?player:null;var level=(ServerLevel)tnt.level();var at=tnt.position();
        if(tnt.entityTags().contains("evoker"))for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),e->canAffect(tnt,e)))if(target.distanceToSqr(at)<=100)
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)insert(p,carried(tnt,new EvokerFangs(level,target.getX()+x,target.getY(),target.getZ()+z,0,0,null)),new Vec3(target.getX()+x,target.getY(),target.getZ()+z),Vec3.ZERO,true);
        if(tnt.entityTags().contains("fire")&&tnt.entityTags().contains(DELTA)){
            var source=game.items().spellSources().source(tnt);if(source!=null){var fill=new FireFill(level,at,source);fires.add(fill);game.items().stonekivi().queuedWork(fill);}
        }else if(tnt.entityTags().contains("fire"))game.items().indirectKills().worldChanges(level,tnt,true,()->{
            var center=BlockPos.containing(at);for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5)))
                if(resident(level,Vec3.atCenterOf(pos))&&level.isInValidBounds(pos)&&level.getBlockState(pos).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.withDefaultNamespace("airs")))){
                    var authority=game.items().spellSources().source(tnt);
                    if(tnt.entityTags().contains(DELTA)&&authority!=null)game.items().writeOwnedTerrain(level,pos,Blocks.FIRE.defaultBlockState(),authority.cause());
                    else level.setBlock(pos,Blocks.FIRE.defaultBlockState(),3);
                }
        });
        // The real carrier keeps its two-tick fuse; strong adds a separate immediate explosion.
        if(tnt.entityTags().contains("electro")){var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setCause(p);insert(p,carried(tnt,bolt),at,Vec3.ZERO,true);}
        if(tnt.entityTags().contains("strong")){
            var authority=game.items().spellSources().source(tnt);
            if(authority!=null&&tnt.entityTags().contains(DELTA))spawnDelta(authority,level,at,8);
            else game.items().exasperato().encounterBlast(p,level,at,4,blast->carried(tnt,blast));
        }
    }
    public void unloaded(Entity e){actors.remove(e);pending.remove(e);}
    /** Released native actors retain their own clocks and owner UUID when their thrower leaves. */
    public void forget(ServerPlayer p){}
    public void stop(){for(var fill:List.copyOf(fires))fill.cancel();restoring=false;var old=new ArrayList<Entity>(actors);old.addAll(pending);actors.clear();pending.clear();old.forEach(Entity::discard);}
    public int pendingCount(){return pending.size();}
    public int trackedCount(){return actors.size();}
    public List<Entity> snapshot(){return List.copyOf(actors);}

    static boolean cloneRecognizes(Entity e){return managed(e)||e instanceof Snowball b&&variant(b.getItem())!=0;}
    LbcCloneSkills.ActorImage cloneImage(Entity source) {
        boolean actor=actors.contains(source),waiting=pending.contains(source);
        if(!actor&&!waiting)return null;
        // CHILD marks damaging released siblings too; it is not a visual/protected role tag.
        var payload=source.getAttached(LbcLegacyRanged.ammoType());var copied=payload==null?null:payload.copy();
        return CloneComplexProjectileStates.image(e->{if(actor)actors.add(e);if(waiting)pending.add((Snowball)e);if(copied!=null)e.setAttached(LbcLegacyRanged.ammoType(),copied.copy());},
                e->{actors.remove(e);pending.remove(e);});
    }

    /** Accepted source fill survives its carrier's two-tick fuse and uses the shared terrain queue. */
    private final class FireFill implements LbcStonekivi.Work {
        final ServerLevel level;final BlockPos min;final LbcSpellSource.Source source;int cursor;boolean closed;
        FireFill(ServerLevel level,Vec3 origin,LbcSpellSource.Source source){this.level=level;min=BlockPos.containing(origin).offset(-5,-5,-5);this.source=source;}
        public boolean ready(){return closed||!restoring&&game.server().tickRateManager().runsNormally()&&!game.items().wands().timeStopped()&&!game.items().timeStop().stopped(level);}
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(closed)return true;if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(source.epoch())){cancel();return true;}
            if(!ready())return false;game.items().stonekivi().beginNativeWork();try{
                long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
                while(cursor<1331&&System.nanoTime()<deadline){var pos=min.offset(cursor%11,cursor/121,cursor/11%11);
                    if(level.isInValidBounds(pos)&&!level.hasChunkAt(pos)){game.maps().prepareChunk(level,new net.minecraft.world.level.ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;}
                    cursor++;if(level.isInValidBounds(pos)&&level.getBlockState(pos).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.withDefaultNamespace("airs"))))game.items().writeOwnedTerrain(level,pos,Blocks.FIRE.defaultBlockState(),source.cause());
                }
                if(cursor==1331)cancel();return closed;
            }finally{game.items().stonekivi().endNativeWork();}
        }
        public void cancel(){closed=true;fires.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){int saved=cursor;boolean ended=closed;return links->{MatchAuthorityCheckpoint.world(game,level);return ()->{cursor=saved;closed=ended;};};}
    }
    public void checkpointBoundary(){}
    public void prepareFrozen(){restoring=true;actors.clear();pending.clear();fires.clear();}
    public void publishFrozen(){restoring=false;}
    public void cancelFrozen(){stop();}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
        var bodies=actors.stream().map(graph.links::key).toList();var waiting=pending.stream().map(graph.links::key).toList();var fills=List.copyOf(fires);fills.forEach(graph::capture);
        return links->{var restored=bodies.stream().map(links::get).toList();var queued=waiting.stream().map(ref->(Snowball)links.get(ref)).toList();return ()->{actors.clear();actors.addAll(restored);pending.clear();pending.addAll(queued);fires.clear();fires.addAll(fills);};};
    }

}
