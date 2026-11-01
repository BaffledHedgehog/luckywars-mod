package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The scoreboard owns the entire sustained cast; each visible whip is rebuilt from current bodies. */
public final class LbcDeus {
    private static final String CLOCK="deus_cd";
    private static final double REACH=28, STEP=.2, PULL=.4;
    private final GameController game;
    // Diagnostics only: never consulted by cast, target selection, clocks or replay.
    private record Frame(List<Vec3> path,List<UUID> hits,Map<UUID,Vec3> tangents,List<UUID> accepted) {}
    private final Map<UUID,Frame> frames=new LinkedHashMap<>();

    public LbcDeus(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty()&&(stack.is(LbcPursuitItems.DEUS)
                ||custom!=null&&custom.copyTag().getIntOr("deus",0)==1);
    }
    public int cooldown(ServerPlayer player){return game.items().mana().hudValue(player,CLOCK);}
    private boolean paused(ServerPlayer player){
        return game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.items().timeStop().stopped(player.level())||game.items().timeStop().frozen(player)
                ||game.items().wands().inputTimeStopped(player);
    }
    private boolean suppressed(ServerPlayer player){
        var magic=game.items().magic();
        return magic.mechanismSuppressed(player)||magic.mechanismSuppressedAt(player.level(),player.getEyePosition())
                ||magic.artifactSuppressedAt(player.level(),player.position())
                ||magic.artifactSuppressedAt(player.level(),player.getEyePosition());
    }
    public boolean use(ServerPlayer player,InteractionHand hand){
        if(!matches(player.getItemInHand(hand))||cooldown(player)>0||!player.isAlive()||player.isRemoved()
                ||!game.playing()||player.level()!=game.maps().level()||!LbcDimensionItems.ordinary(player)
                ||!game.canUseGameplayItems(player)||game.isObserver(player)||game.stage()==GameController.Stage.ENDING
                ||!game.items().wands().validRoleTarget(player,false)||game.items().actionsBlocked(player)
                ||paused(player)||suppressed(player))return false;
        game.items().mana().score(player,CLOCK).set(260);
        frames.remove(player.getUUID());
        return true;
    }
    public void tick(){
        if(game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return;
        for(var player:List.copyOf(game.server().getPlayerList().getPlayers())){
            int clock=cooldown(player);
            if(clock<=0)continue;
            if(!player.isAlive()||player.isRemoved()||!game.canUseGameplayItems(player)
                    ||player.level()!=game.maps().level()||!game.playing()){reset(player);continue;}
            if(paused(player)||suppressed(player))continue;
            if(clock>=200)whip(player,(260-clock)%10==0,260-clock);
            // Damage reactions may stop the match or reset a dead/offline caster inside this frame.
            if(!player.isAlive()||player.isRemoved())reset(player);
            else if(cooldown(player)==clock)game.items().mana().score(player,CLOCK).set(clock-1);
        }
    }
    private boolean eligible(ServerPlayer owner,LivingEntity target){
        return game.items().wands().target(owner,target)&&!target.isInvulnerable()
                &&!game.items().timeStop().frozen(target);
    }
    private LivingEntity nearest(List<LivingEntity> bodies,List<UUID> reached,Vec3 point,double range){
        LivingEntity selected=null;double distance=range*range;
        for(var body:bodies){
            if(!body.isAlive()||body.isRemoved()||reached.contains(body.getUUID()))continue;
            double d=body.getBoundingBox().getCenter().distanceToSqr(point);
            if(d<distance||(d==distance&&selected!=null&&body.getId()<selected.getId())){selected=body;distance=d;}
        }
        return selected;
    }
    private void whip(ServerPlayer owner,boolean pulse,int age){
        Vec3 origin=owner.getEyePosition();
        // One broadphase per attempt. Neither curve samples nor target chaining query entities.
        var bodies=owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(REACH+1),
                target->eligible(owner,target));
        var artifacts=new ArrayList<Vec3>();
        for(var holder:game.server().getPlayerList().getPlayers())
            if(holder.level()==owner.level()&&holder.isAlive()&&game.canUseGameplayItems(holder)
                    &&game.items().magic().artifactActive(holder))artifacts.add(holder.position());
        var path=new ArrayList<Vec3>();var hits=new ArrayList<UUID>(3);
        var tangents=new LinkedHashMap<UUID,Vec3>();var accepted=new ArrayList<UUID>(3);
        path.add(origin);
        Vec3 point=origin,direction=owner.getLookAngle().normalize(),lastVisual=null;
        var target=nearest(bodies,hits,point,20);
        double spent=0;
        DamageSource damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(LbcDamage.LIGHTNING),owner);
        for(int sample=0;sample<140&&spent<REACH-1e-8;sample++){
            Vec3 aim=direction;
            if(target!=null){
                Vec3 towards=target.getBoundingBox().getCenter().subtract(point);
                aim=towards.lengthSqr()<2.5*2.5?towards.normalize():direction.add(towards.normalize().scale(.01)).normalize();
            }
            Vec3 side=aim.cross(Math.abs(aim.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
            Vec3 up=side.cross(aim).normalize();
            // Two independent waves also bend a perfectly straight, stationary aim.
            double phase=sample*.57+age*.24;
            direction=aim.add(side.scale(.16*Math.sin(phase+.4)))
                    .add(up.scale(.09*Math.cos(phase*.73))).normalize();
            Vec3 next=point.add(direction.scale(Math.min(STEP,REACH-spent)));
            double limit=game.items().magic().mechanismSuppressionFraction(owner.level(),point,next);
            for(var center:artifacts)limit=Math.min(limit,sphereEntry(point,next,center));
            boolean jammed=limit<=1;
            if(jammed)next=point.lerp(next,Math.max(0,limit-1e-6));
            if(!owner.level().hasChunkAt(BlockPos.containing(next)))break;
            Vec3 contact=null;
            if(target!=null&&LbcBeamGeometry.intersects(target.getBoundingBox(),point,next))
                contact=target.getBoundingBox().contains(point)?point:target.getBoundingBox().clip(point,next).orElse(null);
            if(contact!=null){
                next=contact;hits.add(target.getUUID());tangents.put(target.getUUID(),direction);
            }
            double length=point.distanceTo(next);spent+=length;path.add(next);
            // Keep all .2-block collision segments; .6-block visual spacing bounds network work.
            if(sample%3==0||contact!=null||jammed){
                owner.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,next.x,next.y,next.z,1,0,0,0,0);
                lastVisual=next;
            }
            point=next;
            if(contact!=null){
                if(pulse){
                    if(!target.hurtServer(owner.level(),damage,2))break;
                    accepted.add(target.getUUID());pull(owner,target,direction);
                }
                if(hits.size()>=3)break;
                target=nearest(bodies,hits,point,REACH-spent);
                if(target!=null)direction=target.getBoundingBox().getCenter().subtract(point).normalize();
            }
            if(jammed)break;
        }
        if(path.size()>1&&!point.equals(lastVisual))
            owner.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,1,0,0,0,0);
        frames.put(owner.getUUID(),new Frame(List.copyOf(path),List.copyOf(hits),Map.copyOf(tangents),List.copyOf(accepted)));
    }
    private void pull(ServerPlayer owner,LivingEntity target,Vec3 incoming){
        if(!target.isAlive()||LbcGreed.active(target)||game.items().timeStop().frozen(target))return;
        double response=1-target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if(!(response>0)||!Double.isFinite(response))return;
        Vec3 impulse=incoming.scale(-PULL*response),next=target.getDeltaMovement().add(impulse);
        if(!Double.isFinite(next.x)||!Double.isFinite(next.y)||!Double.isFinite(next.z))return;
        target.push(impulse.x,impulse.y,impulse.z);target.needsSync=true;
        if(target instanceof ServerPlayer player){player.syncVelocity=true;player.connection.send(new ClientboundSetEntityMotionPacket(player));}
        game.items().indirectKills().displaced(target,owner);
    }
    /** Artifact-only sphere clipping deliberately excludes ordinary magic beacons. */
    private static double sphereEntry(Vec3 from,Vec3 to,Vec3 center){
        Vec3 offset=from.subtract(center),delta=to.subtract(from);double c=offset.lengthSqr()-400;
        if(c<=0)return 0;
        double a=delta.lengthSqr();if(a<1e-12)return Double.POSITIVE_INFINITY;
        double b=offset.dot(delta),discriminant=b*b-a*c;
        if(discriminant<0)return Double.POSITIVE_INFINITY;
        double t=(-b-Math.sqrt(discriminant))/a;return t>=0&&t<=1?t:Double.POSITIVE_INFINITY;
    }
    public void reset(ServerPlayer player){game.items().mana().score(player,CLOCK).set(0);frames.remove(player.getUUID());}
    public void forget(ServerPlayer player){reset(player);}
    public void stop(){for(var player:game.server().getPlayerList().getPlayers())reset(player);frames.clear();}
    List<Vec3> snapshotLastPath(ServerPlayer player){var f=frames.get(player.getUUID());return f==null?List.of():f.path;}
    List<UUID> snapshotLastHits(ServerPlayer player){var f=frames.get(player.getUUID());return f==null?List.of():f.hits;}
    Map<UUID,Vec3> snapshotLastTangents(ServerPlayer player){var f=frames.get(player.getUUID());return f==null?Map.of():f.tangents;}
    List<UUID> snapshotLastAccepted(ServerPlayer player){var f=frames.get(player.getUUID());return f==null?List.of():f.accepted;}
}
