package me.skywars2.game;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** The complete WILD encounter. No permanent invulnerability or artificial encounter lifetime. */
public final class LbcGodHedgehog {
    public static final String TAG="god_hedgehog", MANAGED="skywars2.god_hedgehog", BLAST=MANAGED+".blast";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    private final GameController game;
    private final List<Encounter> encounters=new ArrayList<>();
    private final List<Actor> actors=new ArrayList<>();
    private final ArrayDeque<Laser> lasers=new ArrayDeque<>();
    private final Map<Entity,Encounter> blastSources=new WeakHashMap<>();
    private final Set<Entity> spawning=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Entity> blastChildren=Collections.newSetFromMap(new IdentityHashMap<>());
    private int activeTicker,checkpointCallbacks;
    private boolean checkpointRestoring;
    static final class Encounter {
        final ServerLevel level;final UUID author;final LbcIndirectKills.Owner cause;final String epoch;final Silverfish boss;
        boolean retired;
        Encounter(ServerLevel level,ServerPlayer author,LbcIndirectKills.Owner cause,String epoch,Silverfish boss){
            this(level,author.getUUID(),cause,epoch,boss);
        }
        Encounter(ServerLevel level,UUID author,LbcIndirectKills.Owner cause,String epoch,Silverfish boss){
            this.level=level;this.author=author;this.cause=cause;this.epoch=epoch;this.boss=boss;
        }
    }
    private static final class Actor {
        final Encounter encounter;final Display.ItemDisplay display;final int kind;Marker marker;Vec3 point,direction;int age;
        TrialWarningVisual warning;
        Actor(Encounter e,Display.ItemDisplay d,int kind,Vec3 point,Vec3 direction){encounter=e;display=d;this.kind=kind;this.point=point;this.direction=direction;}
        Entity body(){return display!=null?display:marker;}
    }
    private static final class Laser {
        final Encounter encounter;final Vec3 origin,direction;int cursor;
        Laser(Encounter e,Vec3 origin,Vec3 direction){encounter=e;this.origin=origin;this.direction=direction;}
    }
    private final class Boss extends Silverfish {
        Encounter encounter;
        Boss(ServerLevel level){super(EntityTypes.SILVERFISH,level);}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void setTarget(LivingEntity victim){super.setTarget(victim!=null&&!MatchActorCheckpoint.restoring(this)&&!target(victim,false)?null:victim);}
        @Override public boolean doHurtTarget(ServerLevel level,Entity victim){
            enterCheckpointCallback();try {
                if(encounter==null||!valid(encounter)||!target(victim,false))return false;
                boolean[] result={false};game.items().indirectKills().withCause((LivingEntity)victim,encounter.cause,()->result[0]=super.doHurtTarget(level,victim));return result[0];
            }finally{leaveCheckpointCallback();}
        }
        @Override public void tick(){
            enterCheckpointCallback();try {
                if(checkpointRestoring)return;
                if(encounter==null||!valid(encounter)){discard();return;}
                if(paused(this))return;
                if(getTarget()!=null&&!target(getTarget(),false))setTarget(null);
                super.tick();
            }finally{leaveCheckpointCallback();}
        }
        @Override protected void dropFromLootTable(ServerLevel level,DamageSource source,boolean byPlayer){}
        @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean byPlayer){
            if(encounter!=null&&!encounter.retired&&game.items().currentEpoch(encounter.epoch))
                spawnAtLocation(level,new ItemStack(LbcHedgehogItems.GOD));
        }
    }
    private static final class Visual extends Display.ItemDisplay {
        Visual(ServerLevel level){super(EntityTypes.ITEM_DISPLAY,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    private static final class RayBody extends Marker {
        RayBody(ServerLevel level){super(EntityTypes.MARKER,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    public LbcGodHedgehog(GameController game){this.game=game;}
    public static boolean managed(Entity entity){return entity!=null&&(entity.entityTags().contains(MANAGED)||entity.entityTags().contains(BLAST));}
    private boolean valid(Encounter e){return !e.retired&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.items().currentEpoch(e.epoch);}
    private boolean paused(Entity e){return checkpointRestoring||game.items().wands().timeStopped()||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped");}
    private static boolean resident(ServerLevel level,Vec3 point){var p=BlockPos.containing(point);return level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    private Encounter find(Entity entity){
        if(!managed(entity))return null;
        var fromBlast=blastSources.get(entity);if(fromBlast!=null)return fromBlast;
        for(var e:encounters)if(e.boss==entity)return e;
        for(var actor:actors)if(actor.body()==entity)return actor.encounter;
        return null;
    }
    public ServerPlayer owner(Entity entity){var e=find(entity);return e==null?null:game.server().getPlayerList().getPlayer(e.author);}
    public LbcIndirectKills.Owner cause(Entity entity){var e=find(entity);return e==null?null:e.cause;}
    public boolean isBoss(Entity entity){return encounters.stream().anyMatch(e->valid(e)&&e.boss==entity);}
    /** (user) The living boss permanently forbids every other new silverfish. */
    public boolean blocksSilverfish(ServerLevel level){return encounters.stream().anyMatch(e->e.level==level&&valid(e)&&e.boss.isAlive()&&!e.boss.isRemoved());}
    public boolean canAffect(Entity source,Entity victim){return find(source)==null||(source.entityTags().contains(BLAST)
            ?game.items().wands().validRoleTarget(victim,false)&&!paused(victim):target(victim,false));}
    public void inheritChild(Entity source,Entity child){
        enterCheckpointCallback();try {
        var e=find(source);if(e==null)return;blastSources.put(child,e);blastChildren.add(child);child.entityTags().add(BLAST);game.items().markCurrentItemEpoch(child);
        }finally{leaveCheckpointCallback();}
    }
    private boolean target(Entity entity,boolean arrow){
        if((!entity.getType().builtInRegistryHolder().is(MOBS)&&!(arrow&&LbcWtf.managed(entity)))||!game.items().wands().validRoleTarget(entity,false)
                ||entity instanceof Silverfish||paused(entity))return false;
        return !arrow||(!LbcGreed.active(entity)&&game.items().mana().score(entity,"void_resistance").get()<1);
    }
    public Silverfish summon(ServerLevel level,Vec3 at,ServerPlayer author){
        enterCheckpointCallback();try {
            if(author==null||!game.playing()||!resident(level,at))return null;
            for(var old:encounters)if(old.level==level&&valid(old)&&old.boss.isAlive()&&!old.boss.isRemoved())return old.boss;
            game.items().hedgehog().eraseForBoss(level,null);
            var spawnAt=at.add(0,20,0);
            var boss=new Boss(level);boss.setPos(spawnAt);boss.setPersistenceRequired();boss.setCustomNameVisible(true);
            var name=Component.empty();String[] glyphs={"𝕬","𝕽","𝕮","𝕳","𝕺","𝕹","𝕱","𝕷","𝕰","𝕯","𝕲","𝕺","𝕯","𝕲","𝕰","𝕳","𝕺","𝕲"};
            int[] colors={0xff0000,0xff1800,0xff2f00,0xff4700,0xff5e00,0xff7600,0xff8e00,0xffa500,0xffb200,0xffbb00,0xffc400,0xffce00,0xffd700,0xffe100,0xffea00,0xeaef08,0xc6f215,0xa3f422};
            for(int i=0;i<glyphs.length;i++){final int color=colors[i];name.append(Component.literal(glyphs[i]).withStyle(s->s.withColor(TextColor.fromRgb(color))));}
            boss.setCustomName(name);boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2147483647d);
            boss.getAttribute(Attributes.ARMOR).setBaseValue(20);boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(20);
            boss.getAttribute(Attributes.SCALE).setBaseValue(16);boss.setHealth(boss.getMaxHealth());
            boss.entityTags().addAll(List.of(TAG,MANAGED,"lucky_mob"));game.items().markCurrentItemEpoch(boss);
            var e=new Encounter(level,author,game.items().indirectKills().wildCause(author),game.items().epoch(),boss);boss.encounter=e;encounters.add(e);
            game.items().timeStop().prepareFrozen(boss);
            if(!level.addFreshEntity(boss)||boss.isRemoved()){retire(e);return null;}
            for(float pitch:new float[]{1,1.05f,1.1f,.95f})sound(level,spawnAt,"minecraft:lbcsounds.flowey_laugh",1,pitch,1);
            for(var player:level.players()){
                player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("godfled_spawn").withStyle(net.minecraft.ChatFormatting.DARK_RED)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Ж".repeat(40)).withStyle(net.minecraft.ChatFormatting.DARK_RED,net.minecraft.ChatFormatting.OBFUSCATED)));
            }
            return boss;
        }finally{leaveCheckpointCallback();}
    }
    public void tick(){
        enterCheckpointCallback();try {
            if(checkpointRestoring)return;
            if(game.items().wands().timeStopped())return;
            blastChildren.removeIf(Entity::isRemoved);
            int phase=Math.floorMod(activeTicker++,500);
            for(var e:List.copyOf(encounters)){
                if(!valid(e)||e.boss.isRemoved()&&!resident(e.level,e.boss.position())){retire(e);continue;}
                if(e.boss.isAlive()&&!e.boss.isRemoved()&&!paused(e.boss))bossTick(e,phase);
            }
            // Copy once: descendants start on the following active tick, as in source entity selectors.
            for(var actor:List.copyOf(actors)){
                if(!valid(actor.encounter)||actor.body()==null||actor.body().isRemoved()||!resident(actor.encounter.level,actor.point)){remove(actor);continue;}
                if(paused(actor.body()))continue;
                if(actor.kind==0)eaterTick(actor);else if(actor.kind==1)whiteTick(actor);else rayTick(actor);
            }
            blastTick();
            encounters.removeIf(e->!e.boss.isAlive()&&actors.stream().noneMatch(a->a.encounter==e)&&lasers.stream().noneMatch(l->l.encounter==e));
        }finally{leaveCheckpointCallback();}
    }
    private void bossTick(Encounter e,int phase){
        var boss=e.boss;
        if(Math.floor(boss.getY())<=0&&resident(e.level,new Vec3(0,250,0)))boss.setPos(0,250,0);
        boss.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,2000,0,false,false));
        // (user) Continuous native spawn rejection replaces the old 500-tick silverfish sweep.
        if(phase>=10&&phase<=50&&phase%10==0){
            boss.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false));
            var victim=nearest(e.level,boss.getEyePosition(),200);
            if(victim!=null)lasers.add(new Laser(e,boss.getEyePosition(),victim.getEyePosition().subtract(boss.getEyePosition()).normalize()));
            boss.setDeltaMovement(Vec3.ZERO);
            var next=boss.position().add(new Vec3(0,150,0).subtract(boss.position()).normalize().scale(15));
            if(resident(e.level,next))boss.setPos(next);
            return;
        }
        if(phase>=100&&phase<=140){
            var dir=boss.getLookAngle().normalize();var at=boss.getEyePosition().add(dir.scale(10));seed(e,at,dir);
            sound(e.level,at,"minecraft:lbcsounds.energy_2",1,1,.5f);
            var next=boss.position().add(0,-2,0);if(resident(e.level,next))boss.setPos(next);boss.setYRot(boss.getYRot()+20);return;
        }
        if(phase<240||phase>440)return;
        var candidates=e.level.players().stream().filter(p->!p.isSpectator()&&game.canUseGameplayItems(p)&&!game.isObserver(p)).toList();
        if(candidates.isEmpty())return;
        var player=candidates.get(e.level.getRandom().nextInt(candidates.size()));
        int t=activeTicker-1;double yaw,pitch=0,left=0,turn=0;
        if(phase<=280){yaw=5d*t;left=1;turn=1;}
        else if(phase<=320){yaw=(int)(.1*t);left=(int)(2.5*(Math.floorMod(t,20)-10));turn=1;}
        else if(phase<=360){yaw=7d*t;pitch=Math.floorMod(t*7,180)-90;}
        else if(phase<=400){yaw=180d*t;turn=e.level.getRandom().nextInt(21)-10;}
        else{yaw=95d*t;pitch=Math.floorMod(t*45,180)-90;}
        var at=player.position().add(0,.4,0).add(direction(yaw,pitch).scale(10));var aim=player.getEyePosition().subtract(at).normalize();
        double facingYaw=Math.toDegrees(Math.atan2(-aim.x,aim.z));double facingPitch=-Math.toDegrees(Math.asin(aim.y));
        at=at.add(direction(facingYaw-90,0).scale(left));aim=direction(facingYaw+turn,facingPitch);
        if(white(e,at,aim,0)==null)return;
        sound(e.level,at,"minecraft:block.beacon.activate",.5f,2,0);
        e.level.sendParticles(ParticleTypes.WITCH,at.x,at.y,at.z,5,.4,.4,.4,2);
    }
    private LivingEntity nearest(ServerLevel level,Vec3 from,double radius){
        LivingEntity found=null;double best=radius*radius;
        for(var entity:level.getAllEntities())if(entity instanceof LivingEntity living&&target(entity,false)){
            double d=from.distanceToSqr(entity.position());if(d<=best){best=d;found=living;}
        }return found;
    }
    private static Vec3 direction(double yaw,double pitch){double y=Math.toRadians(yaw),p=Math.toRadians(pitch);return new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));}
    private void seed(Encounter e,Vec3 at,Vec3 dir){var d=display(e,at,dir,true);if(d!=null)actors.add(new Actor(e,d,0,at,dir));}
    private Actor white(Encounter e,Vec3 at,Vec3 direction,int age){
        var display=display(e,at,direction,false);if(display==null)return null;
        var actor=new Actor(e,display,1,at,direction);actor.age=age;
        var from=at.add(0,.2,0);
        actor.warning=TrialWarningVisual.worldBeam(game,e.level,e.epoch,from,from.add(direction.scale(10)),false,Math.max(1,40-age));
        actors.add(actor);return actor;
    }
    private Display.ItemDisplay display(Encounter e,Vec3 at,Vec3 dir,boolean eater){
        if(!resident(e.level,at))return null;var d=new Visual(e.level);
        String model=eater?"hedgegod_eater":"whitearrow";String scale=eater?"[.01f,.01f,.01f]":"[1.5f,1.5f,1.5f]";
        String rotation=eater?"[0f,0f,0f,1f]":"[.6532815f,.27059805f,.6532815f,.27059805f]";
        try{d.load(TagValueInput.create(ProblemReporter.DISCARDING,e.level.registryAccess(),TagParser.parseCompoundFully(
                "{interpolation_duration:1,teleport_duration:1,item_display:'fixed',view_range:"+(eater?"1000f":"1f")
                +",transformation:{translation:[0f,0f,0f],scale:"+scale+",left_rotation:"+rotation+",right_rotation:[0f,0f,0f,1f]},"
                +(eater?"brightness:{sky:15,block:15},":"")+"item:{id:'minecraft:"+(eater?"stick":"carrot_on_a_stick")+"',count:1,components:{'minecraft:item_model':'lbc:"+model+"'}}}")));}
        catch(Exception failure){throw new IllegalStateException("God Hedgehog display",failure);}
        d.setPos(at);d.setYRot((float)Math.toDegrees(Math.atan2(-dir.x,dir.z)));d.setXRot((float)-Math.toDegrees(Math.asin(dir.y)));d.setNoGravity(true);
        d.entityTags().addAll(List.of(MANAGED,"slowraycast","rare",eater?"hedgegod_eater":"floatingwhitearrow"));if(eater)d.entityTags().add("magic");
        game.items().markCurrentItemEpoch(d);game.items().timeStop().prepareFrozen(d);
        spawning.add(d);try{return e.level.addFreshEntity(d)&&!d.isRemoved()?d:null;}finally{spawning.remove(d);}
    }
    private static void scale(Display.ItemDisplay display,float scale,int duration){display.setTransformationInterpolationDuration(duration);display.setTransformationInterpolationDelay(0);display.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale),new Quaternionf()));}
    private void eaterTick(Actor a){
        ++a.age;
        if(a.age==1)scale(a.display,.2f,80);
        if(a.age==3&&a.encounter.level.getRandom().nextFloat()<.96f){
            var victim=nearest(a.encounter.level,a.point,Double.POSITIVE_INFINITY);
            if(victim!=null){var dir=victim.getEyePosition().subtract(a.point).normalize().scale(.3).add(a.direction).normalize();seed(a.encounter,a.point.add(dir.scale(4)),dir);}
        }
        if(a.age==79)scale(a.display,4,1);
        if(a.age==80)eaterBoom(a);
        if(a.age==81)scale(a.display,.0000004f,1);
        if(a.age>=83)remove(a);
    }
    private void eaterBoom(Actor a){
        var e=a.encounter;var p=a.point;for(float pitch:new float[]{1,1.5f,2})sound(e.level,p,"minecraft:lbcsounds.energy_3",1,pitch,0);
        e.level.sendParticles(ParticleTypes.SQUID_INK,p.x,p.y,p.z,50,3,3,3,0);
        var center=BlockPos.containing(p);var kills=game.items().indirectKills();
        kills.withSupportCandidates(e.level,new AABB(p,p).inflate(4),()->kills.trapChanges(e.level,e.cause,()->{
            for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)if(x*x+y*y+z*z<=12){
                var pos=center.offset(x,y,z);if(!resident(e.level,Vec3.atCenterOf(pos))||e.level.getBlockState(pos).isAir())continue;
                kills.supportRemoved(e.level,e.cause,pos);if(e.level.getBlockEntity(pos) instanceof Container container)container.clearContent();
                e.level.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
            }
        }));
        var source=kills.captureDamage(new DamageSource(e.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.OUTSIDE_BORDER),e.boss),e.cause);
        for(var entity:e.level.getEntities((Entity)null,new AABB(p,p).inflate(4),v->target(v,false)&&v.position().distanceToSqr(p)<=16))
            if(entity instanceof LivingEntity living)kills.withCause(living,e.cause,()->living.hurtServer(e.level,source,10));
    }
    private void whiteTick(Actor a){
        if(a.warning!=null)a.warning.tick();
        if(++a.age!=40)return;var level=a.encounter.level;var p=a.point;
        sound(level,p,"minecraft:block.beacon.power_select",2,2,0);level.sendParticles(ParticleTypes.END_ROD,p.x,p.y,p.z,10,.2,.2,.2,.05);
        var ray=new Actor(a.encounter,null,2,p,a.direction);ray.marker=new RayBody(level);ray.marker.setPos(p);
        ray.marker.setYRot(a.display.getYRot());ray.marker.setXRot(a.display.getXRot());
        ray.marker.entityTags().addAll(List.of(MANAGED,"raycastarrow","player","slowraycast"));game.items().markCurrentItemEpoch(ray.marker);game.items().timeStop().prepareFrozen(ray.marker);
        actors.add(ray);if(!level.addFreshEntity(ray.marker)||ray.marker.isRemoved())remove(ray);remove(a);
    }
    private void rayTick(Actor a){
        if(!resident(a.encounter.level,a.point)){remove(a);return;}
        var level=a.encounter.level;
        var from=a.point;var to=from.add(a.direction.scale(2.5));
        // A flat broadphase needs a numerical margin; only the native hitbox determines a hit.
        for(var entity:level.getEntities((Entity)null,new AABB(from,to).inflate(1e-7),v->target(v,true)))
            if(entity instanceof LivingEntity living&&LbcBeamGeometry.intersects(living.getBoundingBox(),from,to))
                game.items().indirectKills().inheritedExecution(living,a.encounter.cause);
        for(int n=0;n<=20;n++){
            var p=a.point.add(a.direction.scale(n*.125));if(!resident(level,p))continue;
            level.sendParticles(ParticleTypes.END_ROD,true,false,p.x,p.y,p.z,1,.01,.01,.01,0);
            sound(level,p,"minecraft:block.beacon.deactivate",.02f,2,0);
        }
        a.point=to;a.marker.setPos(a.point);if(++a.age>=20)remove(a);
    }
    private void blastTick(){
        long deadline=System.nanoTime()+2_000_000;int turns=lasers.size();
        for(int n=0;n<turns&&!lasers.isEmpty()&&System.nanoTime()<deadline;n++){
            var laser=lasers.removeFirst();var e=laser.encounter;if(!valid(e))continue;
            var point=laser.origin.add(laser.direction.scale(laser.cursor*3d));
            if(!paused(e.boss)&&resident(e.level,point)){
                game.items().indirectKills().trapChanges(e.level,e.cause,()->game.items().exasperato().encounterBlast(e.boss,e.level,point,10,tnt->{
                    tnt.entityTags().add(BLAST);tnt.setCustomName(Component.translatable("godbam"));blastSources.put(tnt,e);
                }));
                laser.cursor++;
            }
            if(laser.cursor<101)lasers.addLast(laser);
            // One slice per series per round; outstanding source explosions are retained, never capped or dropped.
            if(n+1==turns&&System.nanoTime()<deadline){turns+=lasers.size();}
        }
    }
    private static void sound(ServerLevel level,Vec3 at,String id,float volume,float pitch,float minimum){LbcOverload.sound(level,at,id,volume,pitch,0,minimum);}
    private void remove(Actor actor){actors.remove(actor);if(actor.warning!=null)actor.warning.close();if(actor.body()!=null&&!actor.body().isRemoved())actor.body().discard();}
    private void retire(Encounter e){e.retired=true;for(var a:List.copyOf(actors))if(a.encounter==e)remove(a);lasers.removeIf(l->l.encounter==e);for(var child:List.copyOf(blastChildren))if(find(child)==e){blastChildren.remove(child);child.discard();}if(!e.boss.isRemoved())e.boss.discard();encounters.remove(e);}
    public void loaded(Entity entity){
        enterCheckpointCallback();try {
        if(checkpointRestoring)return;if(managed(entity)&&!spawning.contains(entity)){var e=find(entity);if(e==null||!valid(e))entity.discard();}
        }finally{leaveCheckpointCallback();}
    }
    public void unloaded(Entity entity){
        enterCheckpointCallback();try {
        if(checkpointRestoring)return;var e=find(entity);if(e!=null&&entity==e.boss)retire(e);else for(var a:List.copyOf(actors))if(a.body()==entity)remove(a);
        }finally{leaveCheckpointCallback();}
    }
    public void removeOwned(ServerPlayer player){
        enterCheckpointCallback();try {
        for(var e:List.copyOf(encounters))if(e.author.equals(player.getUUID()))retire(e);
        }finally{leaveCheckpointCallback();}
    }
    public void stop(){
        enterCheckpointCallback();try {
        for(var e:List.copyOf(encounters))retire(e);for(var child:List.copyOf(blastChildren))child.discard();blastChildren.clear();actors.clear();lasers.clear();blastSources.clear();activeTicker=0;checkpointRestoring=false;
        }finally{leaveCheckpointCallback();}
    }
    public int count(){return encounters.size()+actors.size()+lasers.size()+blastChildren.size();}
    public List<Entity> snapshot(){var list=new ArrayList<Entity>();for(var e:encounters)if(!e.boss.isRemoved())list.add(e.boss);for(var a:actors)if(a.body()!=null)list.add(a.body());return List.copyOf(list);}
    int pendingBlasts(){return lasers.stream().mapToInt(l->101-l.cursor).sum();}
    int actorCount(){return actors.size();}
    List<TrialWarningVisual> warningSnapshot(){return actors.stream().filter(a->a.warning!=null).map(a->a.warning).toList();}
    void phaseForCheck(int phase){activeTicker=phase;}
    /** Explicit source-age boundaries for native fixtures; these do not advance elapsed server time. */
    Entity eaterBoundary(Entity boss,Vec3 at,Vec3 direction,int age){
        enterCheckpointCallback();try {
            var e=find(boss);if(e==null)return null;int previous=actors.size();seed(e,at,direction);if(actors.size()==previous)return null;var a=actors.getLast();a.age=age;return a.body();
        }finally{leaveCheckpointCallback();}
    }
    Entity whiteBoundary(Entity boss,Vec3 at,Vec3 direction,int age){
        enterCheckpointCallback();try {
            var e=find(boss);if(e==null)return null;var a=white(e,at,direction,age);return a==null?null:a.body();
        }finally{leaveCheckpointCallback();}
    }
    void laserBoundary(Entity boss,Vec3 at,Vec3 direction){
        enterCheckpointCallback();try {
        var e=find(boss);if(e!=null)lasers.add(new Laser(e,at,direction));
        }finally{leaveCheckpointCallback();}
    }

    void checkpointBoundary(){if(checkpointCallbacks!=0||!spawning.isEmpty())throw new IllegalStateException("Finish God Hedgehog native callbacks before checkpointing");}
    private void enterCheckpointCallback(){checkpointCallbacks++;}
    private void leaveCheckpointCallback(){checkpointCallbacks--;}
    void prepareFrozen(){
        checkpointBoundary();checkpointRestoring=true;
        for(var actor:actors){if(actor.warning!=null)actor.warning.close();actor.warning=null;}
        encounters.clear();actors.clear();lasers.clear();blastSources.clear();blastChildren.clear();spawning.clear();
    }
    void publishFrozen(){
        checkpointBoundary();
        for(var actor:actors)if(actor.kind==1&&actor.age<40&&actor.warning==null){
            var from=actor.point.add(0,.2,0);
            actor.warning=TrialWarningVisual.worldBeam(game,actor.encounter.level,actor.encounter.epoch,from,from.add(actor.direction.scale(10)),false,Math.max(1,40-actor.age));
        }
        checkpointRestoring=false;
    }
    void cancelFrozen(){checkpointBoundary();checkpointRestoring=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record EncounterImage(Encounter original,MatchDamageSources.Ref boss,boolean retired,boolean active){}
        private record ActorImage(Actor original,MatchDamageSources.Ref body,Vec3 point,Vec3 direction,int age){}
        private record LaserImage(Laser original,int cursor){}
        private record SourceImage(MatchDamageSources.Ref body,Encounter encounter,boolean child){}
        private final List<EncounterImage> savedEncounters;
        private final List<ActorImage> savedActors;
        private final List<LaserImage> savedLasers;
        private final List<SourceImage> savedSources;
        private final int ticker=activeTicker;
        Checkpoint(MatchSpellCheckpoint.Links links){
            var accepted=new java.util.LinkedHashSet<Encounter>(encounters);
            for(var actor:actors)accepted.add(actor.encounter);for(var laser:lasers)accepted.add(laser.encounter);accepted.addAll(blastSources.values());
            savedEncounters=accepted.stream().map(e->new EncounterImage(e,links.key(e.boss),e.retired,encounters.contains(e))).toList();
            savedActors=actors.stream().map(a->new ActorImage(a,links.key(a.body()),a.point,a.direction,a.age)).toList();
            savedLasers=lasers.stream().map(l->new LaserImage(l,l.cursor)).toList();
            var sources=new ArrayList<SourceImage>();blastSources.forEach((body,e)->sources.add(new SourceImage(links.key(body),e,blastChildren.contains(body))));
            for(var child:blastChildren)if(!blastSources.containsKey(child))throw new IllegalStateException("God Hedgehog child has no accepted encounter source");
            savedSources=List.copyOf(sources);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var plans=new ArrayList<Runnable>();
            for(var image:savedEncounters){var e=image.original;var body=links.get(image.boss);
                if(body!=e.boss)throw new IllegalStateException("Restore the same native God Hedgehog boss");
                MatchAuthorityCheckpoint.world(game,e.level);
                plans.add(()->{e.retired=image.retired;if(e.boss instanceof Boss boss)boss.encounter=e;if(image.active)encounters.add(e);});
            }
            for(var image:savedActors){var a=image.original;var body=links.get(image.body);
                if(body!=a.body())throw new IllegalStateException("Restore the same native God Hedgehog attack actor");
                plans.add(()->{a.point=image.point;a.direction=image.direction;a.age=image.age;a.warning=null;actors.add(a);});
            }
            for(var image:savedLasers)plans.add(()->{image.original.cursor=image.cursor;lasers.addLast(image.original);});
            for(var image:savedSources){var body=links.get(image.body);plans.add(()->{blastSources.put(body,image.encounter);if(image.child)blastChildren.add(body);});}
            return ()->{checkpointRestoring=true;encounters.clear();actors.clear();lasers.clear();blastSources.clear();blastChildren.clear();plans.forEach(Runnable::run);activeTicker=ticker;};
        }
    }

    static boolean cloneRecognizes(Entity e){return !cloneActorRecognizes(e)&&(managed(e)||e instanceof LbcGodHedgehog.Boss);}
    static boolean cloneProtected(Entity e){return false;}
    LbcCloneSkills.ActorImage cloneImage(Entity original) {
        var encounter=find(original);if(encounter==null||encounter.retired)return null;
        if(original instanceof Boss){var author=encounter.author;var cause=encounter.cause;var epoch=encounter.epoch;
            // Phase is service-global activeTicker: adding one Encounter preserves the same live phase.
            return CloneComplexProjectileStates.image(Boss::new,clone->{var boss=(Boss)clone;
                var copy=new Encounter((ServerLevel)clone.level(),author,cause,epoch,boss);
                boss.encounter=copy;encounters.add(copy);
            },clone->{var copy=((Boss)clone).encounter;if(copy!=null){encounters.remove(copy);copy.retired=true;}
                ((Boss)clone).encounter=null;});
        }
        if(blastSources.containsKey(original)){boolean child=blastChildren.contains(original);
            return CloneComplexProjectileStates.image(clone->{blastSources.put(clone,encounter);if(child)blastChildren.add(clone);},
                    clone->{blastSources.remove(clone);blastChildren.remove(clone);});
        }
        return null;
    }

    static boolean cloneActorRecognizes(Entity source){return source instanceof Visual||source instanceof RayBody;}
    LbcCloneSkills.ActorImage cloneActorImage(Entity source){
        var actor=actors.stream().filter(a->a.body()==source).findFirst().orElse(null);if(actor==null)return null;
        var encounter=actor.encounter;var point=actor.point;var direction=actor.direction;var origin=source.position();int kind=actor.kind,age=actor.age;
        return new LbcCloneSkills.ActorImage(){
            public Entity create(Entity original,ServerLevel level){return original instanceof RayBody?new RayBody(level):new Visual(level);}
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var copiedBoss=family.get(encounter.boss);var e=copiedBoss instanceof Boss b&&b.encounter!=null?b.encounter:encounter;
                var at=point.add(clone.position().subtract(origin));var a=new Actor(e,clone instanceof Display.ItemDisplay d?d:null,kind,at,direction);
                if(clone instanceof Marker marker)a.marker=marker;a.age=age;actors.add(a);
                if(kind==1&&age<40){var from=at.add(0,.2,0);a.warning=TrialWarningVisual.worldBeam(game,e.level,e.epoch,from,from.add(direction.scale(10)),false,Math.max(1,40-age));}
            }
            public void rollback(Entity clone){for(var a:List.copyOf(actors))if(a.body()==clone){actors.remove(a);if(a.warning!=null)a.warning.close();}}
        };
    }

}
