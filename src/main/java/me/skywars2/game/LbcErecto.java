package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Higher electricity shares Electro traces; this owner stores only the source ray counter and spheres. */
final class LbcErecto {
    static final int CENTRAL=0,SIDE=1,PHANTOM=2,SNIPER=3;
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> CONDUCTIVE=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:electroprovod"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final DustColorTransitionOptions DUST=new DustColorTransitionOptions(0x00ccff,0x003333,.8f);
    private static final DustColorTransitionOptions SNIPER_DUST=new DustColorTransitionOptions(0x00ccff,0xe5e5ff,.8f);
    static final class Path {
        final int kind,focusSteps;final Vec3 origin;final ServerPlayer caster;final InteractionHand hand;final ItemStack stack;
        Vec3 focus;int side;LbcIndirectKills.Owner terrainCause;
        Path(int kind,Vec3 origin,Vec3 focus,int focusSteps,int side,ServerPlayer caster,InteractionHand hand,ItemStack stack){
            this.kind=kind;this.origin=origin;this.focus=focus;this.focusSteps=focusSteps;this.side=side;this.caster=caster;this.hand=hand;this.stack=stack;
        }
    }
    static final class Sphere {
        final ServerPlayer owner;final ServerLevel level;final Vec3 position;final String epoch;int age;
        Sphere(ServerPlayer owner,Vec3 position,String epoch){this.owner=owner;level=owner.level();this.position=position;this.epoch=epoch;}
    }
    private final GameController game;
    private final Set<Sphere> spheres=new LinkedHashSet<>();
    private final Map<ServerPlayer,String> rays=new IdentityHashMap<>();
    private static final Identifier SNIPER_WINDUP=Identifier.parse("skywars2:erecto_sniper_windup");
    private final Map<ServerPlayer,Integer> sniperWindups=new IdentityHashMap<>();
    LbcErecto(GameController game){this.game=game;}
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int mode=Mth.clamp(data.getIntOr("mode",1),1,4);
        if(academy&&owner.isShiftKeyDown()){
            mode=mode%4+1;data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();
            owner.sendSystemMessage(Component.translatable(switch(mode){case 2->"mode_ray";case 3->"mode_sphere";case 4->"mode_sniper";default->"mode_default";}));return true;
        }
        var mana=game.items().mana();int cost=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("erecto_wand_mana_use"),"lbc.math").get();
        if(!mana.trySpendWand(owner,cost))return false;
        if(!academy||mode!=4)castMode(owner,hand,stack,academy?mode:1);
        else if(owner.getXRot()>=89){
            // Looking straight down is source aim assistance, not a paid sniper discharge.
            mana.set(owner,mana.get(owner)+cost);LivingEntity farthest=null;double distance=-1;
            for(var entity:owner.level().getAllEntities())if(entity instanceof LivingEntity living&&game.items().wands().target(owner,entity)){
                double d=entity.distanceToSqr(owner);if(d>distance){distance=d;farthest=living;}
            }
            if(farthest!=null)rotate(owner,farthest.getEyePosition().subtract(owner.getEyePosition()),0);
        }else{
            mana.set(owner,mana.get(owner)-3900);sniper(owner,hand,stack);
        }
        game.items().wands().wandCasted(owner);return true;
    }
    void castMode(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode) {
        switch(mode) {
            case 1 -> volley(owner,hand,stack);
            case 2 -> {game.items().mana().score(owner,"ray").add(40);rays.put(owner,game.items().epoch());}
            case 3 -> {var hit=first(owner,150,true);if(hit!=null)spheres.add(new Sphere(owner,hit.getLocation().subtract(owner.getLookAngle()),game.items().epoch()));}
            case 4 -> sniper(owner,hand,stack);
            default -> throw new IllegalArgumentException("Erecto mode "+mode);
        }
    }
    void emitAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        switch(mode){
            case 1->volleyAt(owner,hand,ItemStack.EMPTY,origin,direction);
            case 2->rayAt(owner,origin,direction);
            case 3->{var hit=firstAt(owner,origin,direction,150,true);if(hit!=null)spheres.add(new Sphere(owner,hit.getLocation().subtract(direction),game.items().epoch()));}
            case 4->{
                add(owner,origin,yaw,pitch,200,new Path(SNIPER,origin,null,0,0,owner,hand,ItemStack.EMPTY));
                LbcOverload.sound(owner,origin,"minecraft:item.trident.return",3,1.3f,0);
                owner.level().sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xff00ffff),true,false,origin.x,origin.y,origin.z,1,0,0,0,0);
            }
            default->throw new IllegalArgumentException("Erecto producer mode "+mode);
        }
    }
    private void sniper(ServerPlayer owner,InteractionHand hand,ItemStack stack) {
        add(owner,owner.getEyePosition(),owner.getYRot(),owner.getXRot(),200,
                    new Path(SNIPER,owner.getEyePosition(),null,0,0,owner,hand,stack));
            // Source Slowness CI immobilizes for one second. The uncapped attribute
            // contract would turn its -1515% modifier into negative movement speed.
            // Multiply the current speed by zero without changing unrelated effects.
            owner.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(
                    new AttributeModifier(SNIPER_WINDUP,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            sniperWindups.put(owner,20);
            rotate(owner,owner.getLookAngle(),-15);LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:item.trident.return",3,1.3f,0);
            owner.level().sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xff00ffff),true,false,owner.getEyePosition().x,owner.getEyePosition().y,owner.getEyePosition().z,1,0,0,0,0);
    }
    private static void rotate(ServerPlayer owner,Vec3 direction,float recoil){
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));float pitch=Mth.clamp((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()))+recoil,-90,90);
        owner.forceSetRotation(yaw,false,pitch,false);
    }
    /** Shared resident DDA and native entity boxes resolve a cast-local focus without allocating a marker. */
    private HitResult first(ServerPlayer owner,double range,boolean entities){return firstAt(owner,owner.getEyePosition(),owner.getLookAngle(),range,entities);}
    private HitResult firstAt(ServerPlayer owner,Vec3 from,Vec3 direction,double range,boolean entities){
        Vec3 to=from.add(direction.scale(range));var wall=LbcTerra.wallHit(owner.level(),from,to);
        if(wall!=null)to=wall.getLocation();
        var entity=entities?ProjectileUtil.getEntityHitResult(owner,from,to,new AABB(from,to).inflate(1),e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,e),from.distanceToSqr(to)):null;
        if(entity!=null)return entity;
        return wall!=null&&owner.level().hasChunkAt(BlockPos.containing(wall.getLocation()))?wall:null;
    }
    private void volley(ServerPlayer owner,InteractionHand hand,ItemStack stack){volleyAt(owner,hand,stack,owner.getEyePosition(),owner.getLookAngle());}
    private void volleyAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,Vec3 origin,Vec3 direction){
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        var hit=firstAt(owner,origin,direction,200,true);Vec3 focus=hit==null?null:hit.getLocation();int steps=hit==null?401:Math.min(401,1+Mth.ceil(origin.distanceTo(focus)*2));
        add(owner,origin,yaw,pitch,200,new Path(CENTRAL,origin,focus,steps,0,owner,hand,stack));
        for(int side:new int[]{1,-1,2,-2,3,-3,4,-4})add(owner,origin,yaw+side*10,pitch,200,new Path(SIDE,origin,focus,steps,side,owner,hand,stack));
    }
    private void add(ServerPlayer owner,Vec3 at,float yaw,float pitch,int lifetime,Path path){
        var trace=new LbcWands.ElectroTrace(owner,at,game.items().epoch(),lifetime);trace.yaw=yaw;trace.pitch=pitch;trace.erecto=path;game.items().wands().higherElectro(trace);
    }
    boolean step(LbcWands.ElectroTrace trace){
        Path p=trace.erecto;if(p.kind==SNIPER)return sniper(trace);
        double spacing=p.kind==SIDE?.25:p.kind==PHANTOM?.2:.5;int samples=p.kind==SIDE?9:p.kind==PHANTOM?10:4;
        Vec3 from=trace.position,direction=Vec3.directionFromRotation(trace.pitch,trace.yaw),end=from;boolean clear=true;
        var shield=p.kind==PHANTOM?game.items().wands().shieldHit(trace.owner,from,from.add(direction.scale(samples*spacing))):null;
        double shieldDistance=shield==null?Double.POSITIVE_INFINITY:from.distanceTo(shield.getLocation());boolean emitted=false;
        double mechanismDistance=game.items().magic().mechanismSuppressionFraction(trace.level,from,from.add(direction.scale(samples*spacing)))*samples*spacing;
        boolean jammed=false;
        for(int i=0;i<=samples;i++){
            Vec3 point=from.add(direction.scale(i*spacing));if(i*spacing>=Math.min(mechanismDistance,shieldDistance)){
                jammed=mechanismDistance<=shieldDistance;end=jammed?from.add(direction.scale(mechanismDistance)):shield.getLocation();clear=false;break;
            }
            var pos=BlockPos.containing(point);if(!trace.level.hasChunkAt(pos)){clear=false;break;}
            emitted=true;end=point;
            if(p.kind!=PHANTOM||trace.level.getRandom().nextFloat()<.2f)particles(trace,point,p.kind==CENTRAL?.03f:.01f,false);
            if(p.kind!=PHANTOM&&!trace.level.getBlockState(pos).is(AIRS)){clear=false;break;}
        }
        if(emitted)hurt(trace,from,end,spacing,p.kind==PHANTOM?.5f:1,2);
        trace.position=end;
        if(!clear){if(!jammed&&(shield==null||from.distanceTo(end)<shieldDistance-1e-6))impact(trace,end);return false;}
        if(p.kind==SIDE&&p.side!=0){
            if(trace.age+1>p.focusSteps/7){
                if(p.focus!=null)aim(trace,p.focus.subtract(end));p.side=0;
            }else{int bend=((400-p.focusSteps)/11)*3;trace.yaw-=Math.signum(p.side)*Math.abs(p.side)*bend*bend*.0001f;}
        }
        if(p.kind==PHANTOM){
            trace.yaw+=(trace.level.getRandom().nextInt(1601)-800)*.01f;trace.pitch+=(trace.level.getRandom().nextInt(1601)-800)*.01f;
            if(trace.level.getBlockState(BlockPos.containing(end)).is(CONDUCTIVE))game.items().wands().teslaConduction(trace,end);
        }
        return true;
    }
    private void particles(LbcWands.ElectroTrace t,Vec3 at,float volume,boolean sniper){
        t.level.sendParticles(sniper?SNIPER_DUST:DUST,true,false,at.x,at.y,at.z,1,.1,.1,.1,0);
        if(!sniper)t.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,1,.1,.1,.1,1);
        if(volume>0)LbcOverload.sound(t.owner,at,sniper?"minecraft:item.trident.riptide_3":"minecraft:entity.firework_rocket.twinkle",volume,2,0);
    }
    private static void aim(LbcWands.ElectroTrace t,Vec3 direction){t.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));t.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));}
    private void hurt(LbcWands.ElectroTrace t,Vec3 from,Vec3 to,double spacing,float amount,int limit){
        var damage=new DamageSource(t.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),t.owner);
        LbcDamage.hurtSegment(t.level,damage,from,to,Vec3.ZERO,spacing,amount,limit,e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(t.owner,e)
                && (t.erecto.kind!=CENTRAL&&t.erecto.kind!=SNIPER||!e.entityTags().contains("transformator")),false,e->{
            var p=t.erecto;
            if(p.kind==SIDE&&t.owner==p.caster&&e.position().distanceToSqr(p.origin)>4900&&LbcAcademy.trial(p.stack))game.items().academy().ready(p.caster,p.hand,p.stack);
        });
    }
    private void impact(LbcWands.ElectroTrace trace,Vec3 point){
        var level=trace.level;if(!level.hasChunkAt(BlockPos.containing(point)))return;
        Vec3 particles=point.subtract(Vec3.directionFromRotation(trace.pitch,trace.yaw).scale(.5));level.sendParticles(ParticleTypes.END_ROD,particles.x,particles.y,particles.z,25,.1,.1,.1,.1);
        if(!level.getBlockState(BlockPos.containing(point)).is(CONDUCTIVE))return;
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_CONDUCTION),trace.owner);
        for(var e:level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(15),e->e.position().distanceToSqr(point)<=225&&game.items().wands().projectileTarget(trace.owner,e)&&!e.entityTags().contains("transformator"))){
            e.hurtServer(level,damage,8);level.sendParticles(ParticleTypes.END_ROD,e.getX(),e.getY()+1,e.getZ(),3,.3,.6,.3,.2);
        }
    }
    private boolean sniper(LbcWands.ElectroTrace trace){
        Vec3 from=trace.position,direction=Vec3.directionFromRotation(trace.pitch,trace.yaw),end=from;boolean alive=true,emitted=false;
        double mechanismDistance=game.items().magic().mechanismSuppressionFraction(trace.level,from,from.add(direction.scale(18)))*18;
        for(int i=0;i<=60;i++){
            if(i*.3>=mechanismDistance){end=from.add(direction.scale(mechanismDistance));alive=false;break;}
            Vec3 point=from.add(direction.scale(i*.3));var pos=BlockPos.containing(point);if(!trace.level.hasChunkAt(pos)){alive=false;break;}
            emitted=true;end=point;particles(trace,point,trace.level.getRandom().nextBoolean()?.1f:0,true);
            if((i+1)%12==0){
                Vec3 right=new Vec3(Math.cos(Math.toRadians(trace.yaw)),0,Math.sin(Math.toRadians(trace.yaw)));
                for(int sign:new int[]{-1,1}){var at=point.add(right.scale(sign));trace.level.sendParticles(ParticleTypes.SCULK_SOUL,true,false,at.x,at.y,at.z,3,.1,.1,.1,0);}
            }
            if(!trace.level.getBlockState(pos).is(AIRS)){
                impact(trace,point);var p=trace.erecto;
                if(p.terrainCause==null||!p.terrainCause.player().equals(trace.owner.getUUID()))p.terrainCause=game.items().indirectKills().coldCause(trace.owner,true);
                game.items().indirectKills().trapChanges(trace.level,p.terrainCause,()->{
                    for(var block:BlockPos.betweenClosed(BlockPos.containing(point.add(-.5,-.5,-.5)),BlockPos.containing(point.add(.5,.5,.5))))
                        if(trace.level.hasChunkAt(block)&&!trace.level.getBlockState(block).isAir())trace.level.destroyBlock(block,true,trace.owner);
                });
                if(trace.level.getRandom().nextFloat()<.2f){alive=false;break;}
            }
        }
        if(emitted)hurt(trace,from,end,.3,20,2);trace.position=end;return alive;
    }
    int rayPending(ServerPlayer owner){return Math.max(0,game.items().mana().hudValue(owner,"ray"));}
    void addPlayerCounter(ServerPlayer owner){game.items().mana().score(owner,"ray").add(1);rays.put(owner,game.items().epoch());}
    /** A source food can set ray directly; it must not create a second ray clock or another debit. */
    void adoptRawFoodStatuses(ServerPlayer owner){
        if(rayPending(owner)>0&&!game.items().magic().rejectCustomEffect(owner))rays.put(owner,game.items().epoch());
    }

    int sphereCount(){return spheres.size();}
    List<Sphere> sphereSnapshot(){return List.copyOf(spheres);}
    void tick(){
        for(var entry:List.copyOf(sniperWindups.entrySet())){
            var owner=entry.getKey();
            if(!owner.isAlive()||owner.isRemoved()||owner.level()!=game.maps().level()||!game.canUseGameplayItems(owner))clearSniperWindup(owner);
            else if(!game.items().timeStop().stopped(owner.level())){
                int remaining=entry.getValue()-1;
                if(remaining<=0)clearSniperWindup(owner);else sniperWindups.put(owner,remaining);
            }
        }
        for(var entry:List.copyOf(rays.entrySet())){
            var owner=entry.getKey();if(!active(owner,entry.getValue())||game.items().magic().rejectCustomEffect(owner)){clearRay(owner);continue;}
            if(rayPending(owner)<=0){rays.remove(owner);continue;}if(game.items().timeStop().stopped(owner.level()))continue;
            ray(owner);game.items().mana().score(owner,"ray").add(-1);if(rayPending(owner)<=0)rays.remove(owner);
        }
        for(var sphere:List.copyOf(spheres)){
            if(!active(sphere.owner,sphere.epoch)||sphere.owner.level()!=sphere.level||!sphere.level.hasChunkAt(BlockPos.containing(sphere.position))){spheres.remove(sphere);continue;}
            if(game.items().timeStop().stopped(sphere.level))continue;
            if(++sphere.age%5==0)for(int i=0;i<16;i++)add(sphere.owner,sphere.position,sphere.owner.getRandom().nextFloat()*360-180,sphere.owner.getRandom().nextFloat()*180-90,8,
                    new Path(PHANTOM,sphere.position,null,0,0,sphere.owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY));
            if(sphere.age>=300)spheres.remove(sphere);
        }
    }
    private boolean active(ServerPlayer owner,String epoch){return owner.isAlive()&&!owner.isRemoved()&&game.items().currentEpoch(epoch)&&game.canUseGameplayItems(owner);}
    private void ray(ServerPlayer owner){rayAt(owner,owner.getEyePosition(),owner.getLookAngle());}
    private void rayAt(ServerPlayer owner,Vec3 from,Vec3 direction){
        Vec3 end=from.add(direction.scale(50));double boundary=game.items().magic().mechanismSuppressionFraction(owner.level(),from,end);
        if(Double.isFinite(boundary))end=from.lerp(end,Math.max(0,Math.nextDown(boundary)));
        var wall=LbcTerra.wallHit(owner.level(),from,end);if(wall!=null)end=wall.getLocation();
        if(from.distanceToSqr(end)<1e-12)return;
        for(int i=0;i<=100&&i*.5<=from.distanceTo(end)+1e-7;i++)if(owner.getRandom().nextFloat()<.35f){var point=from.add(direction.scale(i*.5));owner.level().sendParticles(DUST,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);owner.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,1,.1,.1,.1,1);LbcOverload.sound(owner,point,"minecraft:entity.firework_rocket.twinkle",.03f,2,0);}
        var damage=new DamageSource(owner.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),owner);
        var affected=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        LbcDamage.hurtSegment(owner.level(),damage,from,end,Vec3.ZERO,.5,.5f,1,e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,e),false,e->{
            if(affected.add(e)&&e instanceof LivingEntity living){living.addEffect(ItemEffectLevels.create(MobEffects.GLOWING,20,0),owner);living.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,20,100),owner);}
        },null);
    }
    void clearRay(ServerPlayer owner){rays.remove(owner);game.items().mana().score(owner,"ray").set(0);clearSniperWindup(owner);}
    int sniperWindupTicks(ServerPlayer owner){return sniperWindups.getOrDefault(owner,0);}
    private void clearSniperWindup(ServerPlayer owner){sniperWindups.remove(owner);owner.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SNIPER_WINDUP);}
    void forget(ServerPlayer owner){clearRay(owner);clearSniperWindup(owner);spheres.removeIf(s->s.owner==owner);}
    void stop(){for(var owner:List.copyOf(rays.keySet()))clearRay(owner);for(var owner:List.copyOf(sniperWindups.keySet()))clearSniperWindup(owner);spheres.clear();}
}
