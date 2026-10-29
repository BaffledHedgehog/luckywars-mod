package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** True Falling Stars и Exahoming: ведение штатных fireball, без второго движка взрывов. */
public final class LbcHoming {
    public static final String TAG="skywars2.homing";
    public static final String DONOR_TAG="skywars2.fallingstars";
    public static final String SLOW_TAG="skywars2.homing.slow";
    private static final int[][] STAR_OFFSETS={{0,0},{0,3},{3,0},{0,-3},{-3,0},{2,2},{2,-2},
            {-2,2},{-2,-2},{1,0},{0,1},{-1,0},{0,-1},{0,0}};
    private static final int[][] HOMING_OFFSETS={{0,40,0},{8,50,0},{0,60,8},{-8,70,0},{0,80,-8},
            {12,90,12},{-12,100,-12},{12,110,-12},{-12,120,12},{0,130,0}};
    private static final DustParticleOptions BLACK=new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1,.001f,0,.001f)&0xffffff,4);
    private static final DustParticleOptions STAR_DUST=new DustParticleOptions(0xccccff,4);
    private final GameController game;
    private final Set<LargeFireball> fireballs=new LinkedHashSet<>();
    // Для глобального Curse нужен индекс уже загруженных снарядов, не новый трекер их физики.
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> PROJECTILES=
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    net.minecraft.resources.Identifier.parse("minecraft:projectiles"));
    private final Set<Entity> projectiles=new LinkedHashSet<>();
    private final Set<java.util.concurrent.CompletableFuture<net.minecraft.world.phys.HitResult>> pendingRays=new LinkedHashSet<>();
    private final Map<LargeFireball,Integer> ages=new IdentityHashMap<>();
    private final Map<LargeFireball,LivingEntity> steeringTargets=new IdentityHashMap<>();
    private final List<Effect> rains=new ArrayList<>(),destinies=new ArrayList<>();
    private final Map<LivingEntity,Curse> curses=new IdentityHashMap<>();
    private int ticks;

    private static final class Effect {
        final ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        Vec3 position;
        int remaining;
        Effect(ServerPlayer owner,Vec3 position,int remaining,String epoch) {
            this.owner=owner; this.level=owner.level(); this.position=position;
            this.remaining=remaining; this.epoch=epoch;
        }
    }
    private static final class Curse {
        final ServerPlayer owner;
        long remaining;boolean rawWild;
        Curse(ServerPlayer owner,long remaining) { this.owner=owner; this.remaining=remaining; }
    }

    LbcHoming(GameController game) {
        this.game=game;
        if(game.maps().level()!=null) for(var entity:game.maps().level().getAllEntities())
            if(entity instanceof Projectile || entity.getType().builtInRegistryHolder().is(PROJECTILES)) projectiles.add(entity);
    }
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform) {
        transformOrigins(rains,level,transform);
        transformOrigins(destinies,level,transform);
    }
    private static void transformOrigins(List<Effect> effects,ServerLevel level,
            java.util.function.UnaryOperator<Vec3> transform) {
        effects.removeIf(effect->{
            if(effect.level!=level) return false;
            Vec3 next=transform.apply(effect.position);
            if(next==null) return true;
            effect.position=next; return false;
        });
    }

    public int count() { return fireballs.size()+rains.size()+destinies.size()+curses.size(); }
    record EffectView(ServerPlayer owner,String epoch,Vec3 position,int remaining){}
    List<EffectView> destinySnapshot(){return destinies.stream().map(e->new EffectView(e.owner,e.epoch,e.position,e.remaining)).toList();}
    List<LargeFireball> snapshot() { return List.copyOf(fireballs); }
    public int rainCount() { return rains.size(); }
    public int destinyCount() { return destinies.size(); }
    public int curseCount() { return curses.size(); }
    public int pendingRayCount() { return pendingRays.size(); }
    public int curseValue(LivingEntity entity) {
        var curse=curses.get(entity); return curse==null?0:(int)Math.min(Integer.MAX_VALUE,(curse.remaining+19)/20);
    }
    int curseTicks(LivingEntity entity) { var curse=curses.get(entity); return curse==null?0:(int)Math.min(Integer.MAX_VALUE,curse.remaining); }
    public int curseSeconds(LivingEntity entity) { return curseValue(entity); }
    void adoptRawFoodStatuses(ServerPlayer player){
        int seconds=game.items().mana().hudValue(player,"target");
        if(seconds<=0||!valid(player)||game.items().magic().rejectCustomEffect(player))return;
        var curse=curses.computeIfAbsent(player,p->new Curse(player,0));curse.remaining=(long)seconds*20;curse.rawWild=true;
    }
    private boolean curseTarget(Curse curse,LivingEntity target){return curse.rawWild
            ?game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target)
            :target(curse.owner,target);}
    private void syncCurse(LivingEntity target){game.items().mana().score(target,"target").set(curseValue(target));}
    void addPlayerCounters(ServerPlayer player) {
        if(!game.items().magic().rejectCustomEffect(player))
            curses.computeIfAbsent(player,ignored->new Curse(player,0)).remaining+=20;
        syncCurse(player);
        // These are sustained casts owned by the player, rather than projectile/entity ages.
        for(var effect:rains) if(effect.owner==player) effect.remaining++;
        for(var effect:destinies) if(effect.owner==player) effect.remaining++;
    }
    public void clearEffects(LivingEntity entity) { clear(entity); }
    /** Release the source Target curse without canceling independent homing casts or projectiles. */
    void clearCurse(ServerPlayer player) {
        clear(player);
        game.items().mana().score(player,"target").set(0);
    }
    private boolean valid(ServerPlayer owner) { return owner!=null && game.items().mana().visible(owner); }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")
                || game.items().frost().isFrozen(owner) || game.items().magic().suppressed(owner);
    }
    private boolean target(ServerPlayer owner,LivingEntity entity) {
        return projectileTarget(owner,entity) && game.items().wands().target(owner,entity);
    }
    private boolean projectileTarget(ServerPlayer owner,LivingEntity entity) {
        return entity.level()==owner.level() && game.items().lifetimes().target(owner,entity)
                && !entity.entityTags().contains("strict_map_object")
                && !game.items().magic().suppressedAt(owner.level(),entity.getEyePosition());
    }

    /** Вход только из общего ПКМ-dispatch; бонусы каста начисляются один раз после расхода. */
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean donor,boolean upgraded) {
        if(!valid(owner) || paused(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        var mana=game.items().mana();
        if(upgraded && owner.isShiftKeyDown()) {
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=Math.floorMod(tag.getIntOr("mode",1),donor?3:4)+1;
            tag.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            owner.sendSystemMessage(Component.translatable(donor?switch(mode) {
                case 2->"mode_big"; case 3->"mode_fire_rain"; default->"mode_default";
            }:switch(mode) {
                case 2->"mode_sisyfus"; case 3->"mode_destiny"; case 4->"mode_curse"; default->"mode_default";
            }));
            return true;
        }
        int cost=mana.score(ScoreHolder.forNameOnly(donor?"falling_stars_wand_mana_use":"exahoming_wand_mana_use"),"lbc.math").get();
        if(!mana.trySpendWand(owner,cost)) return false;
        // Ordinary Exahoming clicks sound even when the ray finds no target; Academy modes are separate.
        if(!donor&&!upgraded)LbcOverload.sound(owner,owner.position(),"minecraft:entity.firework_rocket.shoot",1,1,0);
        int mode=upgraded?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)
                .copyTag().getIntOr("mode",1),1,donor?3:4):1;
        launch(owner,donor,mode,upgraded,true);
        if(!upgraded && owner.isShiftKeyDown() && LbcAcademy.trial(stack)) {
            trial(owner,donor);
            game.items().academy().ready(owner,hand,stack);
        }
        game.items().wands().wandCasted(owner); return true;
    }

    void grimoire(ServerPlayer owner,int mode) {
        if(mode==1)LbcOverload.sound(owner,owner.position(),"minecraft:entity.firework_rocket.shoot",1,1,0);
        launch(owner,false,mode,mode!=1,false);
    }
    void libraryStars(ServerPlayer owner,int mode){launch(owner,true,mode,mode!=1,false);}
    private void launch(ServerPlayer owner,boolean donor,int mode,boolean upgraded,boolean achievement) {
        launchAt(owner,donor,mode,upgraded,achievement,owner.getEyePosition(),owner.getLookAngle());
    }
    void emitAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        launchAt(owner,false,mode,mode!=1,false,origin,direction);
    }
    private void launchAt(ServerPlayer owner,boolean donor,int mode,boolean upgraded,boolean achievement,Vec3 origin,Vec3 direction){
        if(!donor && mode==3) destinies.add(new Effect(owner,origin,600,game.items().epoch()));
        else if(!donor && mode==4) curseRay(owner,origin,direction);
        else {
            requestRay(owner,origin,direction,true,(from,to)->trail(owner,from,to,donor&&!upgraded?.5:1,!donor),hit->{
                if(hit==null) return;
                Vec3 point=hit.getLocation();
                if(donor) stars(owner,point,mode); else homing(owner,point,mode==2);
                if(achievement && donor && origin.distanceToSqr(point)>=150*150 && nearest(owner,point,7)!=null)
                    game.items().mana().award(owner,"lbc:true_advancements/staffs/falling_stars");
            });
        }
    }

    /** Единое первое попадание: блок и существо в одной ячейке не удваивают эффект. */
    private Vec3 ray(ServerPlayer owner,Vec3 from,Vec3 direction,double range,double step) {
        Vec3 to=from.add(direction.scale(range));
        double barrier=game.items().magic().suppressionFraction(owner.level(),from,to);
        if(Double.isFinite(barrier)) to=from.lerp(to,barrier);
        var hit=game.items().wands().firstHit(owner,from,to,e->e instanceof LivingEntity living && game.items().lifetimes().target(owner,living)
                && !game.items().magic().suppressedAt(owner.level(),living.getEyePosition()));
        Vec3 end=hit==null?to:hit.getLocation();
        double length=from.distanceTo(end);
        for(double d=0;d<=length;d+=step) {
            Vec3 p=from.add(direction.scale(d));
            owner.level().sendParticles(new DustParticleOptions(0x412c13,1),true,false,p.x,p.y,p.z,1,0,0,0,0);
        }
        if(hit==null || hit instanceof BlockHitResult wall && !owner.level().hasChunkAt(wall.getBlockPos())
                || Double.isFinite(barrier) && end.distanceToSqr(from)>=to.distanceToSqr(from)-1e-8) return null;
        return end;
    }
    private void stars(ServerPlayer owner,Vec3 point,int mode) {
        LbcOverload.sound(owner,point,"minecraft:entity.firework_rocket.launch",3,1,0);
        owner.level().sendParticles(ParticleTypes.FIREWORK,point.x,point.y+15,point.z,140,0,15,0,0);
        var dust=mode==2?new DustParticleOptions(0,4):mode==3?
                new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1,.855f,.455f,.129f)&0xffffff,4):STAR_DUST;
        // Шесть полос исходной печати; цвет зависит от режима, а не меняет её рисунок.
        double[][] strips={{0,0,2,0},{0,0,0,2},{-2.5,5,1.5,0},{2.5,-5,1.5,0},{5,2.5,0,1.5},{-5,-2.5,0,1.5}};
        for(var strip:strips) owner.level().sendParticles(dust,true,false,point.x+strip[0],point.y+5,
                point.z+strip[1],140,strip[2],0,strip[3],0);
        if(mode==2) fire(owner,point.add(0,400,0),15,new Vec3(0,-.2,0),false,false,false);
        else if(mode==3) rains.add(new Effect(owner,point.add(0,80,0),100,game.items().epoch()));
        else for(int i=0;i<STAR_OFFSETS.length;i++) fire(owner,point.add(STAR_OFFSETS[i][0],50+25*i,STAR_OFFSETS[i][1]),
                i==13?4:2,new Vec3(0,-.2,0),false,false,false);
    }
    private void homing(ServerPlayer owner,Vec3 point,boolean slow) {
        figure(owner,point.add(0,10,0));
        if(slow) fire(owner,point.add(0,40,0),100,Vec3.ZERO,true,true,false);
        else for(var offset:HOMING_OFFSETS)
            fire(owner,point.add(offset[0],offset[1],offset[2]),2,Vec3.ZERO,true,false,false);
    }
    private void figure(ServerPlayer owner,Vec3 point) {
        int[] angles={0,30,60,90,120,150,180,210,240,270,300,320};
        for(int angle:angles) {
            double yaw=Math.toRadians(angle);
            Vec3 forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
            Vec3 side=new Vec3(-Math.cos(yaw),0,-Math.sin(yaw));
            for(int i=0;i<15;i++) {
                Vec3 p=i<12?point.add(forward.scale(i*.5)):
                        point.add(forward.scale(5.5)).add(side.scale((i-11)*.5));
                owner.level().sendParticles(BLACK,true,false,p.x,p.y,p.z,1,0,0,0,0);
            }
        }
    }
    private LargeFireball fire(ServerPlayer owner,Vec3 point,int power,Vec3 velocity,boolean guided,boolean slow,boolean trial) {
        if(!owner.level().hasChunkAt(BlockPos.containing(point)) || game.items().magic().suppressedAt(owner.level(),point)) return null;
        var fireball=new LargeFireball(owner.level(),owner,velocity,power);
        fireball.entityTags().add(guided?TAG:DONOR_TAG);
        if(slow) fireball.entityTags().add(SLOW_TAG);
        if(trial) fireball.entityTags().add(LbcBlaze.TRIAL_PROJECTILE);
        game.items().wands().launchFire(owner,fireball,point,velocity);
        return fireball;
    }
    private void trial(ServerPlayer owner,boolean donor) {
        if(donor) for(int i=0;i<STAR_OFFSETS.length;i++) {
            int height=i<2?150+25*i:1050+25*i;
            fire(owner,owner.position().add(STAR_OFFSETS[i][0],height,STAR_OFFSETS[i][1]),
                    i==0?1:i==1?2:i==13?4:5,new Vec3(0,-.2,0),false,false,true);
        } else {
            int[][] offsets={{0,500,0},{50,450,0},{0,450,50},{-50,400,0},{0,400,-50}};
            for(var p:offsets) fire(owner,owner.position().add(p[0],p[1],p[2]),20,Vec3.ZERO,true,false,true);
        }
    }

    public void loaded(Entity entity) {
        if(entity.level()!=game.maps().level()) return;
        if(entity instanceof Projectile || entity.getType().builtInRegistryHolder().is(PROJECTILES)) projectiles.add(entity);
        if(entity instanceof LargeFireball fireball && family(fireball)) {
            if(!game.items().currentEpoch(entity)) entity.discard();
            else { fireballs.add(fireball); ages.put(fireball,0); }
        }
    }
    public void unloaded(Entity entity) {
        projectiles.remove(entity);
        if(entity instanceof LargeFireball fireball) { fireballs.remove(fireball); ages.remove(fireball); steeringTargets.remove(fireball); }
        if(entity instanceof LivingEntity living) clear(living);
    }
    private boolean family(Entity entity) { return entity.entityTags().contains(TAG) || entity.entityTags().contains(DONOR_TAG); }

    /** До native inertia/collision: остановка не должна позволить шару попасть во время паузы. */
    public boolean beforeFireTick(AbstractHurtingProjectile projectile) {
        if(!(projectile instanceof LargeFireball fireball) || !family(projectile)) return true;
        if(!game.items().currentEpoch(projectile) || !(projectile.getOwner() instanceof ServerPlayer owner)
                || !valid(owner) || owner.level()!=projectile.level()) { projectile.discard(); return false; }
        if(game.items().magic().suppressedAt(owner.level(),projectile.position())) { projectile.discard(); return false; }
        if(game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")) return false;
        int age=ages.merge(fireball,1,Integer::sum);
        // Нативный owner меняется при отражении; сохранённый автор каста здесь не используется.
        boolean guided=projectile.entityTags().contains(TAG),trial=projectile.entityTags().contains(LbcBlaze.TRIAL_PROJECTILE);
        if(guided && (age==1 || age%5==0)) {
            var next=nearest(owner,projectile.position(),Double.POSITIVE_INFINITY);
            if(trial && owner.isAlive() && (next==null || owner.distanceToSqr(projectile)<next.distanceToSqr(projectile))) next=owner;
            if(next==null) steeringTargets.remove(fireball); else steeringTargets.put(fireball,next);
        }
        var nearest=steeringTargets.get(fireball);
        if(nearest!=null) {
            game.items().lifetimes().forget(projectile);
            if(age%5==0 && (trial && owner.distanceToSqr(projectile)>=64 || hasDistantTarget(owner,projectile.position()))) {
                projectile.setDeltaMovement(nearest.getEyePosition().subtract(projectile.position()).normalize()
                        .scale(projectile.entityTags().contains(SLOW_TAG)?.5:2));
                LbcShooters.orient(projectile);
            }
        } else {
            game.items().lifetimes().track(projectile,true);
            game.items().lifetimes().advance(projectile,age);
        }
        // Native applyInertia увеличивает шаг ДО столкновений; проверяем ту же длину заранее.
        Vec3 motion=projectile.getDeltaMovement();
        Vec3 nativeMotion=motion.add(motion.normalize().scale(projectile.accelerationPower))
                .scale(projectile.isInWater()?.8:.95);
        Vec3 to=projectile.position().add(nativeMotion);
        double barrier=game.items().magic().suppressionFraction(owner.level(),projectile.position(),to);
        if(Double.isFinite(barrier) || !projectile.level().hasChunkAt(BlockPos.containing(to))) {
            projectile.discard(); return false;
        }
        return true;
    }
    private boolean hasDistantTarget(ServerPlayer owner,Vec3 point) {
        for(var living:game.items().lifetimes().loadedTargets())
            if(target(owner,living) && living.position().distanceToSqr(point)>=64) return true;
        return false;
    }
    private LivingEntity nearest(ServerPlayer owner,Vec3 point,double radius) {
        LivingEntity nearest=null; double distance=radius*radius;
        for(var living:game.items().lifetimes().loadedTargets()) if(target(owner,living)) {
            double d=living.position().distanceToSqr(point);
            if(d<=distance) { distance=d; nearest=living; }
        }
        return nearest;
    }

    public void tick() {
        ++ticks;
        tickEffects(rains,true); tickEffects(destinies,false);
        tickCurses();
    }
    private void tickEffects(List<Effect> effects,boolean rain) {
        // spawn/unload callbacks могут менять другие коллекции, поэтому задания удаляются до вызова эффекта.
        for(int i=effects.size()-1;i>=0;i--) {
            var effect=effects.get(i); var owner=effect.owner;
            if(!game.items().currentEpoch(effect.epoch) || !valid(owner) || owner.level()!=effect.level
                    || !effect.level.hasChunkAt(BlockPos.containing(effect.position))
                    || game.items().magic().suppressedAt(effect.level,effect.position)) { effects.remove(i); continue; }
            if(game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")) continue;
            if(rain) for(int shot=0;shot<3;shot++) {
                Vec3 p=new Vec3(Math.floor(effect.position.x)+owner.getRandom().nextInt(31)-15,effect.position.y,
                        Math.floor(effect.position.z)+owner.getRandom().nextInt(31)-15);
                fire(owner,p,1,new Vec3(0,-.1,0),false,false,false);
            } else destiny(effect);
            if(--effect.remaining<=0) effects.remove(i);
        }
    }
    private void destiny(Effect effect) {
        var owner=effect.owner; var target=nearest(owner,effect.position,Double.POSITIVE_INFINITY);
        if(target==null) return;
        double distance=target.position().distanceToSqr(effect.position);
        Vec3 direction=target.getEyePosition().subtract(effect.position).normalize();
        if(distance<=900 && owner.getRandom().nextFloat()<.05f) {
            Vec3 point=ray(owner,effect.position,direction,30,.5);
            if(point!=null) for(int i=0;i<3;i++)
                game.items().exasperato().instantBlast(owner,effect.level,point,4,effect.epoch);
        }
        if(distance>400) {
            Vec3 to=effect.position.add(direction.scale(.3));
            if(effect.level.hasChunkAt(BlockPos.containing(to))
                    && !Double.isFinite(game.items().magic().suppressionFraction(effect.level,effect.position,to))) {
                effect.position=to;
                effect.level.sendParticles(ParticleTypes.SOUL,true,false,to.x,to.y,to.z,1,0,0,0,0);
            }
        }
    }

    /** Curse проходит блоки, но не границы незагруженных чанков и подавления магии. */
    private void curseRay(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        requestRay(owner,origin,direction,false,null,hit->{
            if(!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity living)
                    || !projectileTarget(owner,living)) return;
            if(living instanceof ServerPlayer player && game.items().magic().rejectCustomEffect(player)) return;
            curses.compute(living,(entity,old)->new Curse(owner,(old==null?0:old.remaining)+1200));
            syncCurse(living);
            owner.level().sendParticles(new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1,.314f,.294f,.129f)&0xffffff,3),
                    living.getX(),living.getY(),living.getZ(),50,1,1,1,.3);
            LbcOverload.sound(owner,living.position(),"minecraft:block.sculk_catalyst.bloom",1,1,0);
        });
    }

    private void requestRay(ServerPlayer owner,Vec3 from,Vec3 direction,boolean walls,
            java.util.function.BiConsumer<Vec3,Vec3> trail,java.util.function.Consumer<net.minecraft.world.phys.HitResult> hit) {
        var pending=game.items().exasperato().searchLoadedRay(owner,from,direction,walls,trail);
        pendingRays.add(pending);
        pending.whenComplete((result,error)->{
            pendingRays.remove(pending);
            if(!pending.isCancelled() && error==null && valid(owner)) hit.accept(result);
        });
    }
    private void trail(ServerPlayer owner,Vec3 from,Vec3 to,double step,boolean force) {
        Vec3 delta=to.subtract(from); double length=delta.length();
        for(double d=0;d<length;d+=step) {
            Vec3 p=from.add(delta.scale(d/length));
            owner.level().sendParticles(ParticleTypes.FIREWORK,force,false,p.x,p.y,p.z,2,.5,.5,.5,0);
        }
    }
    private void tickCurses() {
        for(var entry:List.copyOf(curses.entrySet())) {
            var victim=entry.getKey(); var curse=entry.getValue();
            if(game.items().polymorph().masked(victim))continue;
            if(!valid(curse.owner) || !(curse.rawWild?curseTarget(curse,victim):projectileTarget(curse.owner,victim))
                    || victim instanceof ServerPlayer player && game.items().magic().rejectCustomEffect(player)) { clear(victim); continue; }
            if(paused(curse.owner) || victim.entityTags().contains("time_stopped")
                    || game.items().frost().isFrozen(victim)) continue;
            if(--curse.remaining<=0) clear(victim);else syncCurse(victim);
        }
        if(ticks%20!=0 || curses.isEmpty() || game.items().wands().timeStopped()) return;
        // Глобальность исходного режима сохраняется; каждый снаряд выбирает только допустимую ему цель.
        for(var projectile:List.copyOf(projectiles)) {
            if(projectile.isRemoved()) { projectiles.remove(projectile); continue; }
            LivingEntity nearest=null; Curse chosen=null; double distance=Double.POSITIVE_INFINITY;
            for(var entry:curses.entrySet()) {
                var victim=entry.getKey(); var curse=entry.getValue();
                if(victim.level()!=projectile.level() || !curseTarget(curse,victim) || paused(curse.owner)) continue;
                double d=projectile.position().distanceToSqr(victim.position());
                if(d<distance) { distance=d; nearest=victim; chosen=curse; }
            }
            if(nearest!=null) {
                Entity original=projectile instanceof Projectile shot?shot.getOwner():
                        projectile instanceof net.minecraft.world.entity.item.PrimedTnt tnt?tnt.getOwner():game.items().electro().reflectedOwner(projectile);
                // Последнее уточнение user: чужой игрок сохраняет авторство; ничейные и свои забирает проклявший.
                if(original==null || original==nearest) game.items().electro().assignOwner(projectile,chosen.owner);
                projectile.setDeltaMovement(nearest.getEyePosition().subtract(projectile.position()).normalize());
                projectile.needsSync=true;
                if(projectile instanceof Projectile shot) LbcShooters.orient(shot);
            }
        }
        game.items().wands().redirectCursedElectro();
        game.items().danmaku().redirectCursedCards();
    }
    public void clear(LivingEntity entity) { curses.remove(entity);game.items().mana().score(entity,"target").set(0); }
    /** Manual Danmaku cards use the same global Curse target and ownership rule as native projectiles. */
    void redirect(LbcDanmaku.Card card) {
        LivingEntity nearest=null;Curse chosen=null;double distance=Double.POSITIVE_INFINITY;
        for(var entry:curses.entrySet()) {
            var victim=entry.getKey();var curse=entry.getValue();
            if(victim.level()!=card.display.level() || !curseTarget(curse,victim) || paused(curse.owner)) continue;
            double d=card.display.position().distanceToSqr(victim.position());
            if(d<distance){distance=d;nearest=victim;chosen=curse;}
        }
        if(nearest==null)return;
        if(card.owner==null || card.owner==nearest)card.owner=chosen.owner;
        var direction=nearest.getEyePosition().subtract(card.display.position());
        if(direction.lengthSqr()<1e-12)return;
        card.direction=direction.normalize();
        card.display.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
        card.display.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
    }
    void redirect(LbcWands.ElectroTrace trace) {
        LivingEntity nearest=null; Curse chosen=null; double distance=Double.POSITIVE_INFINITY;
        for(var entry:curses.entrySet()) {
            var victim=entry.getKey(); var curse=entry.getValue();
            if(victim.level()!=trace.level || !curseTarget(curse,victim) || paused(curse.owner)) continue;
            double d=trace.position.distanceToSqr(victim.position());
            if(d<distance) { distance=d; nearest=victim; chosen=curse; }
        }
        if(nearest==null) return;
        var original=LbcIndirectKills.playerSource(trace.source());
        if(original==null || original==nearest){trace.owner=chosen.owner;trace.sourcePolicy=null;trace.cause=null;}
        Vec3 direction=nearest.getEyePosition().subtract(trace.position);
        trace.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        trace.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.sqrt(direction.x*direction.x+direction.z*direction.z)));
    }
    public void forget(ServerPlayer owner) {
        rains.removeIf(e->e.owner==owner); destinies.removeIf(e->e.owner==owner);
        curses.entrySet().removeIf(e->e.getKey()==owner || e.getValue().owner==owner);
        for(var fireball:List.copyOf(fireballs)) if(fireball.getOwner()==owner) fireball.discard();
    }
    public void stop() {
        for(var pending:List.copyOf(pendingRays)) pending.cancel(false);
        pendingRays.clear();
        var pending=List.copyOf(fireballs); fireballs.clear(); ages.clear(); steeringTargets.clear();
        rains.clear(); destinies.clear(); curses.clear(); ticks=0;
        for(var fireball:pending) fireball.discard();
        // Индекс прочих нативных снарядов сохраняется: stop не является выгрузкой мира.
    }
}
