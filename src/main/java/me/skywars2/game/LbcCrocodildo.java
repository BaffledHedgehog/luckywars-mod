package me.skywars2.game;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Real 200 HP encounter; falling bombs use the same nuclear owner as all other nukes. */
public final class LbcCrocodildo {
    private static final String BODY="skywars2.crocodildo",VISUAL=BODY+".visual",BOMB=BODY+".bomb";
    private static final String MUSIC="minecraft:lbcsounds.crocodildo";
    private final GameController game;
    private final Set<Actor> actors=new LinkedHashSet<>();
    private final Set<ItemEntity> pending=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerBossEvent> bars=new HashMap<>();
    private int depth,lastTick=Integer.MIN_VALUE;private boolean frozen;
    private static final class Actor {
        LivingEntity body,source;Display.ItemDisplay visual;final String epoch;final boolean bomb;
        int age;boolean terminal;
        Actor(LivingEntity body,Display.ItemDisplay visual,String epoch,boolean bomb,LivingEntity source){
            this.body=body;this.visual=visual;this.epoch=epoch;this.bomb=bomb;this.source=source;
        }
    }
    LbcCrocodildo(GameController game){this.game=game;}
    static boolean trigger(ItemStack stack){return ItemData.intValue(stack,"crocodildo_boss",0)==1&&ItemData.intValue(stack,"drop",0)==1;}
    static boolean managed(Entity e){return e!=null&&(e.entityTags().contains(BODY)||e.entityTags().contains(BOMB)||e.entityTags().contains(VISUAL));}
    static boolean visual(Entity e){return e!=null&&e.entityTags().contains(VISUAL);}
    private boolean live(String epoch){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch);}
    private static boolean resident(Entity e){return !e.isRemoved()&&((ServerLevel)e.level()).getChunkSource().getChunkNow(e.blockPosition().getX()>>4,e.blockPosition().getZ()>>4)!=null;}
    private boolean stopped(Entity e){return game.items().timeStop().stopped((ServerLevel)e.level())||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped");}
    private static void load(Entity e,String snbt){try{e.load(TagValueInput.create(ProblemReporter.DISCARDING,e.registryAccess(),TagParser.parseCompoundFully(snbt)));}catch(Exception error){throw new IllegalStateException(error);}}
    private Display.ItemDisplay presentation(ServerLevel level,boolean bomb){
        var d=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);
        load(d,bomb?"{view_range:1000f,interpolation_duration:1,teleport_duration:1,billboard:'center',item:{id:'minecraft:stick',count:1,components:{'minecraft:item_model':'lbc:nuke'}},transformation:{scale:[3f,3f,3f],translation:[0f,-0.4f,0f]}}"
                :"{view_range:1000f,interpolation_duration:1,teleport_duration:1,item_display:'fixed',item:{id:'minecraft:stick',count:1,components:{'minecraft:item_model':'lbc:crocodildo'}},transformation:{scale:[15f,15f,15f],translation:[0f,1f,0f]}}");
        d.entityTags().add(VISUAL);return d;
    }
    boolean spawn(ItemEntity token){
        if(frozen||token.isRemoved()||token.getAge()>=5999||!trigger(token.getItem())||!live(game.items().epoch())||!game.items().currentEpoch(token)
                ||token.level()!=game.maps().level()||stopped(token)||game.maps().busy())return false;
        depth++;try{
            var level=(ServerLevel)token.level();var p=token.position();var inward=new Vec3(-p.x,0,-p.z).normalize();
            if(inward.lengthSqr()==0)inward=new Vec3(0,0,1);
            var at=p.add(0,80,0).subtract(inward.scale(150));if(!level.hasChunkAt(BlockPos.containing(at)))return false;
            var body=EntityTypes.TURTLE.create(level,EntitySpawnReason.COMMAND);if(body==null)return false;
            load(body,"{NoAI:true,Silent:true,PersistenceRequired:true,DeathLootTable:'minecraft:empty'}");
            body.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);body.setHealth(200);body.getAttribute(Attributes.SCALE).setBaseValue(10);
            body.setInvisible(true);body.setCustomName(Component.translatable("crocodilo").withStyle(ChatFormatting.DARK_RED,ChatFormatting.BOLD));
            var d=presentation(level,false);body.setPos(at);d.setPos(at);body.setYRot((float)Math.toDegrees(Math.atan2(-inward.x,inward.z)));body.entityTags().addAll(List.of(BODY,"crocodildo"));
            var actor=new Actor(body,d,game.items().epoch(),false,body);actors.add(actor);prepare(body);prepare(d);
            if(!level.addFreshEntity(body)||!level.addFreshEntity(d)){retire(actor);return false;}d.startRiding(body,true,true);
            var remaining=token.getItem().copy();remaining.shrink(1);if(remaining.isEmpty())token.discard();else token.setItem(remaining);
            bar(actor);if(actors.stream().filter(a->!a.bomb).count()==1)music(true);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,p.x,p.y,p.z,100,10,10,10,0);return true;
        }finally{depth--;}
    }
    private void prepare(Entity e){game.items().markCurrentItemEpoch(e);game.items().timeStop().prepareFrozen(e);}
    private void bomb(Actor boss){
        var level=(ServerLevel)boss.body.level();var p=boss.body.position().add(0,-3,0);if(!level.hasChunkAt(BlockPos.containing(p)))return;
        var pig=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);if(pig==null)return;
        load(pig,"{NoAI:true,Silent:true,Invulnerable:true,PersistenceRequired:true,DeathLootTable:'minecraft:empty'}");
        pig.setInvisible(true);pig.setPos(p);pig.entityTags().addAll(List.of(BOMB,"nuke_pig","mob_projectile"));var d=presentation(level,true);d.setPos(p);
        var actor=new Actor(pig,d,boss.epoch,true,boss.body);actors.add(actor);prepare(pig);prepare(d);
        if(!level.addFreshEntity(pig)||!level.addFreshEntity(d)){retire(actor);return;}d.startRiding(pig,true,true);
        LbcOverload.sound(level,p,"minecraft:lbcsounds.drop_bomb",SoundSource.HOSTILE,10,0,200,0);
    }
    void tick(){
        if(frozen||lastTick==game.server().getTickCount()||!game.server().tickRateManager().runsNormally())return;
        lastTick=game.server().getTickCount();depth++;try{
            for(var token:List.copyOf(pending)){if(token.isRemoved()||!trigger(token.getItem())||!live(game.items().epoch()))pending.remove(token);else spawn(token);}
            for(var a:List.copyOf(actors)){
                if(!live(a.epoch)||a.body.level()!=game.maps().level()){retire(a);continue;}
                if(a.body.isRemoved()&&a.body.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)continue;
                if(a.body.isRemoved()){retire(a);continue;}
                if(!resident(a.body)||stopped(a.body))continue;
                if(!a.body.isAlive()){died(a.body);continue;}
                var level=(ServerLevel)a.body.level();
                if(game.items().magic().mechanismSuppressedAt(level,a.body.position())){
                    if(a.bomb)retire(a);else a.body.hurtServer(level,a.body.damageSources().genericKill(),Float.MAX_VALUE);continue;
                }
                if(a.bomb){if(a.body.onGround()){var p=a.body.position();retire(a);game.items().nukeEvent().encounterBurst(level,p,a.source);}continue;}
                a.age++;bar(a);move(a);if(a.age%100==0)bomb(a);
            }
        }finally{depth--;}
    }
    private void move(Actor a){
        var at=a.body.position();LivingEntity nearest=null;double best=Double.POSITIVE_INFINITY;
        var candidates=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());candidates.addAll(game.items().lifetimes().loadedTargets());candidates.addAll(((ServerLevel)a.body.level()).players());
        for(var e:candidates)if(e.level()==a.body.level()&&e!=a.body&&!managed(e)&&!e.entityTags().contains("nodamage")&&!e.entityTags().contains("nodamage1")&&game.items().wands().validRoleTarget(e,false)){
            var d=e.position().distanceToSqr(at);if(d<best){best=d;nearest=e;}
        }
        if(nearest==null)return;var direction=a.body.getLookAngle().add(nearest.getEyePosition().subtract(a.body.getEyePosition()).normalize().scale(.025)).multiply(1,0,1).normalize();
        var next=at.add(direction);if(!((ServerLevel)a.body.level()).hasChunkAt(BlockPos.containing(next)))return;
        a.body.setPos(next);float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));a.body.setYRot(yaw);a.body.setYHeadRot(yaw);a.visual.setYRot(yaw);
    }
    void died(LivingEntity body){
        if(frozen||!body.isDeadOrDying())return;var a=actors.stream().filter(x->x.body==body&&!x.bomb&&!x.terminal).findFirst().orElse(null);if(a==null)return;
        a.terminal=true;actors.remove(a);a.visual.discard();closeBar(body.getUUID());
        if(!live(a.epoch))return;var level=(ServerLevel)body.level();
        for(var stack:game.lbc().rollTable(level,body.position(),null,"lbc:64_nukes")){
            var drop=LbcBossLoot.protect(new ItemEntity(level,body.getX(),body.getY(),body.getZ(),stack));prepare(drop);level.addFreshEntity(drop);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,true,false,body.getX(),body.getY(),body.getZ(),1000,50,50,50,0);
        for(float pitch:new float[]{0,1,2})LbcOverload.sound(level,body.position(),"minecraft:entity.generic.explode",SoundSource.HOSTILE,100,pitch,0,0);
    }
    void damaged(LivingEntity body){
        if(!body.entityTags().contains(BODY)||actors.stream().noneMatch(a->a.body==body&&!a.terminal))return;
        var level=(ServerLevel)body.level();LbcOverload.sound(level,body.position(),"minecraft:entity.iron_golem.damage",SoundSource.HOSTILE,20,.5f,0,1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,true,false,body.getX(),body.getY(),body.getZ(),1,0,0,0,0);
    }
    private void bar(Actor a){
        var bar=bars.computeIfAbsent(a.body.getUUID(),id->new ServerBossEvent(id,Component.translatable("crocodildo"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_20));
        bar.setProgress(Math.clamp(a.body.getHealth()/200f,0,1));
        for(var p:List.copyOf(bar.getPlayers()))if(p.level()!=a.body.level()||p.hasDisconnected())bar.removePlayer(p);
        for(var p:((ServerLevel)a.body.level()).players())bar.addPlayer(p);
    }
    private void music(boolean play){for(var p:game.server().getPlayerList().getPlayers())if(play)p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse(MUSIC))),SoundSource.MASTER,p.getX(),p.getY(),p.getZ(),1,1,0));
        else p.connection.send(new ClientboundStopSoundPacket(Identifier.parse(MUSIC),SoundSource.MASTER));}
    private void closeBar(UUID id){var bar=bars.remove(id);if(bar!=null)bar.removeAllPlayers();if(bars.isEmpty())music(false);}
    private void retire(Actor a){actors.remove(a);a.terminal=true;if(!a.bomb)closeBar(a.body.getUUID());a.visual.discard();if(!a.body.isRemoved())a.body.discard();}
    void loaded(Entity e){
        if(frozen||MatchActorCheckpoint.restoring(e))return;
        if(e instanceof ItemEntity item&&trigger(item.getItem())){pending.add(item);return;}
        if(!managed(e))return;
        boolean found=false;
        for(var a:actors){if(a.body.getUUID().equals(e.getUUID()))a.body=(LivingEntity)e;if(a.visual.getUUID().equals(e.getUUID()))a.visual=(Display.ItemDisplay)e;
            if(a.source.getUUID().equals(e.getUUID())&&e instanceof LivingEntity living)a.source=living;
            if(a.body==e||a.visual==e)found=true;}
        if(found)prepare(e);else e.discard();
    }
    void unloaded(Entity e){if(e instanceof ItemEntity item)pending.remove(item);}
    void stop(){for(var a:List.copyOf(actors))retire(a);pending.clear();for(var b:bars.values())b.removeAllPlayers();bars.clear();}
    List<LivingEntity> bodies(){return actors.stream().map(a->a.body).toList();}
    int age(Entity body){return actors.stream().filter(a->a.body==body).mapToInt(a->a.age).findFirst().orElse(-1);}
    boolean captureReady(){return depth==0&&!frozen;}
    void checkpointBoundary(){if(depth!=0)throw new IllegalStateException("Finish Crocodildo callback before checkpoint");}
    void prepareFrozen(){checkpointBoundary();frozen=true;for(var b:bars.values())b.removeAllPlayers();bars.clear();actors.clear();pending.clear();}
    void publishFrozen(){checkpointBoundary();frozen=false;lastTick=Integer.MIN_VALUE;for(var a:actors)if(!a.bomb&&resident(a.body))bar(a);if(!bars.isEmpty())music(true);}
    void cancelFrozen(){frozen=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        record Value(MatchDamageSources.Ref body,MatchDamageSources.Ref visual,MatchDamageSources.Ref source,String epoch,boolean bomb,int age){}
        final List<Value> values=new ArrayList<>();final List<MatchDamageSources.Ref> tokens=new ArrayList<>();
        Checkpoint(MatchSpellCheckpoint.Links links){for(var a:actors)values.add(new Value(links.key(a.body),links.key(a.visual),links.key(a.source),a.epoch,a.bomb,a.age));for(var p:pending)tokens.add(links.key(p));}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var next=new ArrayList<Actor>();for(var v:values){var a=new Actor((LivingEntity)links.get(v.body),(Display.ItemDisplay)links.get(v.visual),v.epoch,v.bomb,(LivingEntity)links.get(v.source));a.age=v.age;next.add(a);}
            var drops=tokens.stream().map(r->(ItemEntity)links.get(r)).toList();return ()->{actors.clear();actors.addAll(next);pending.clear();pending.addAll(drops);};
        }
    }
    LbcCloneSkills.ActorImage cloneImage(Entity original){
        var a=actors.stream().filter(x->x.body==original&&!x.terminal).findFirst().orElse(null);if(a==null)return null;
        var picture=new CloneEncounterStates.Presentation(a.visual);var epoch=a.epoch;boolean bomb=a.bomb;int age=a.age;var source=a.source;
        return CloneComplexProjectileStates.image(clone->{
            var d=(Display.ItemDisplay)picture.create((ServerLevel)clone.level());d.setPos(clone.position());
            var copy=new Actor((LivingEntity)clone,d,epoch,bomb,source==original?(LivingEntity)clone:source);copy.age=age;actors.add(copy);prepare(d);
            if(!((ServerLevel)clone.level()).addFreshEntity(d)||!d.startRiding(clone,true,true))throw new IllegalStateException("Crocodildo clone visual publication");
        },clone->{for(var copy:List.copyOf(actors))if(copy.body==clone)retire(copy);});
    }
}
