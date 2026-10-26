package me.skywars2.game;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
    private int activeTicker;
    static final class Encounter {
        final ServerLevel level;final UUID author;final LbcIndirectKills.Owner cause;final String epoch;final Silverfish boss;
        boolean retired;
        Encounter(ServerLevel level,ServerPlayer author,LbcIndirectKills.Owner cause,String epoch,Silverfish boss){
            this.level=level;this.author=author.getUUID();this.cause=cause;this.epoch=epoch;this.boss=boss;
        }
    }
    private static final class Actor {
        final Encounter encounter;final Display.ItemDisplay display;final int kind;Marker marker;Vec3 point,direction;int age;
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
        @Override public void setTarget(LivingEntity victim){super.setTarget(victim!=null&&!target(victim,false)?null:victim);}
        @Override public boolean doHurtTarget(ServerLevel level,Entity victim){
            if(encounter==null||!valid(encounter)||!target(victim,false))return false;
            boolean[] result={false};game.items().indirectKills().withCause((LivingEntity)victim,encounter.cause,()->result[0]=super.doHurtTarget(level,victim));return result[0];
        }
        @Override public void tick(){
            if(encounter==null||!valid(encounter)){discard();return;}
            if(paused(this))return;
            if(getTarget()!=null&&!target(getTarget(),false))setTarget(null);
            super.tick();
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
    private boolean paused(Entity e){return game.items().wands().timeStopped()||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped");}
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
    public void inheritChild(Entity source,Entity child){var e=find(source);if(e==null)return;blastSources.put(child,e);blastChildren.add(child);child.entityTags().add(BLAST);game.items().markCurrentItemEpoch(child);}
    private boolean target(Entity entity,boolean arrow){
        if(!entity.getType().builtInRegistryHolder().is(MOBS)||!game.items().wands().validRoleTarget(entity,false)
                ||entity instanceof Silverfish||paused(entity))return false;
        return !arrow||(!LbcGreed.active(entity)&&game.items().mana().score(entity,"void_resistance").get()<1);
    }
    public Silverfish summon(ServerLevel level,Vec3 at,ServerPlayer author){
        if(author==null||!game.playing()||!resident(level,at))return null;
        for(var old:encounters)if(old.level==level&&valid(old)&&old.boss.isAlive()&&!old.boss.isRemoved())return old.boss;
        game.items().hedgehog().eraseForBoss(level,null);
        var boss=new Boss(level);boss.setPos(at);boss.setPersistenceRequired();boss.setCustomNameVisible(true);
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
        for(float pitch:new float[]{1,1.05f,1.1f,.95f})sound(level,at,"minecraft:lbcsounds.flowey_laugh",1,pitch,1);
        for(var player:level.players()){
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("godfled_spawn").withStyle(net.minecraft.ChatFormatting.DARK_RED)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Ж".repeat(40)).withStyle(net.minecraft.ChatFormatting.DARK_RED,net.minecraft.ChatFormatting.OBFUSCATED)));
        }
        return boss;
    }
    public void tick(){
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
        var visual=display(e,at,aim,false);if(visual==null)return;
        actors.add(new Actor(e,visual,1,at,aim));sound(e.level,at,"minecraft:block.beacon.activate",.5f,2,0);
        e.level.sendParticles(ParticleTypes.WITCH,at.x,at.y,at.z,5,.4,.4,.4,2);
        for(int n=1;n<=40;n++){var p=at.add(0,.2,0).add(aim.scale(n*.25));e.level.sendParticles(new DustParticleOptions(0xffffff,1.5f),p.x,p.y,p.z,1,0,0,0,0);}
    }
    private LivingEntity nearest(ServerLevel level,Vec3 from,double radius){
        LivingEntity found=null;double best=radius*radius;
        for(var entity:level.getAllEntities())if(entity instanceof LivingEntity living&&target(entity,false)){
            double d=from.distanceToSqr(entity.position());if(d<=best){best=d;found=living;}
        }return found;
    }
    private static Vec3 direction(double yaw,double pitch){double y=Math.toRadians(yaw),p=Math.toRadians(pitch);return new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));}
    private void seed(Encounter e,Vec3 at,Vec3 dir){var d=display(e,at,dir,true);if(d!=null)actors.add(new Actor(e,d,0,at,dir));}
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
        for(int n=0;n<=20;n++){
            var p=a.point.add(a.direction.scale(n*.125));if(!resident(level,p))continue;
            level.sendParticles(ParticleTypes.END_ROD,true,false,p.x,p.y,p.z,1,.01,.01,.01,0);
            sound(level,p,"minecraft:block.beacon.deactivate",.02f,2,0);
            // The authored paired cell tests are distinct; the first cell supplies actual victims.
            var first=new AABB(p,p.add(1,1,1));var second=new AABB(p.add(-.99,-.99,-.99),p.add(.01,.01,.01));
            if(level.getEntities((Entity)null,second,v->target(v,true)).isEmpty())continue;
            for(var entity:level.getEntities((Entity)null,first,v->target(v,true)))if(entity instanceof LivingEntity living)
                game.items().indirectKills().inheritedKill(living,a.encounter.cause);
        }
        a.point=a.point.add(a.direction.scale(2.5));a.marker.setPos(a.point);if(++a.age>=20)remove(a);
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
    private void remove(Actor actor){actors.remove(actor);if(actor.body()!=null&&!actor.body().isRemoved())actor.body().discard();}
    private void retire(Encounter e){e.retired=true;for(var a:List.copyOf(actors))if(a.encounter==e)remove(a);lasers.removeIf(l->l.encounter==e);for(var child:List.copyOf(blastChildren))if(find(child)==e){blastChildren.remove(child);child.discard();}if(!e.boss.isRemoved())e.boss.discard();encounters.remove(e);}
    public void loaded(Entity entity){if(managed(entity)&&!spawning.contains(entity)){var e=find(entity);if(e==null||!valid(e))entity.discard();}}
    public void unloaded(Entity entity){var e=find(entity);if(e!=null&&entity==e.boss)retire(e);else for(var a:List.copyOf(actors))if(a.body()==entity)remove(a);}
    public void removeOwned(ServerPlayer player){for(var e:List.copyOf(encounters))if(e.author.equals(player.getUUID()))retire(e);}
    public void stop(){for(var e:List.copyOf(encounters))retire(e);for(var child:List.copyOf(blastChildren))child.discard();blastChildren.clear();actors.clear();lasers.clear();blastSources.clear();activeTicker=0;}
    public int count(){return encounters.size()+actors.size()+lasers.size()+blastChildren.size();}
    public List<Entity> snapshot(){var list=new ArrayList<Entity>();for(var e:encounters)if(!e.boss.isRemoved())list.add(e.boss);for(var a:actors)if(a.body()!=null)list.add(a.body());return List.copyOf(list);}
    int pendingBlasts(){return lasers.stream().mapToInt(l->101-l.cursor).sum();}
    int actorCount(){return actors.size();}
    void phaseForCheck(int phase){activeTicker=phase;}
    /** Explicit source-age boundaries for native fixtures; these do not advance elapsed server time. */
    Entity eaterBoundary(Entity boss,Vec3 at,Vec3 direction,int age){
        var e=find(boss);if(e==null)return null;int previous=actors.size();seed(e,at,direction);if(actors.size()==previous)return null;var a=actors.getLast();a.age=age;return a.body();
    }
    Entity whiteBoundary(Entity boss,Vec3 at,Vec3 direction,int age){
        var e=find(boss);if(e==null)return null;var d=display(e,at,direction,false);if(d==null)return null;
        var a=new Actor(e,d,1,at,direction);a.age=age;actors.add(a);return d;
    }
    void laserBoundary(Entity boss,Vec3 at,Vec3 direction){var e=find(boss);if(e!=null)lasers.add(new Laser(e,at,direction));}
}
