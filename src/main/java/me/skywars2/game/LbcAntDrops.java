package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.*;

/** Ant source actors and ordinary native descendants retain distinct identities. */
public final class LbcAntDrops {
    private static final String ACTOR="skywars2.ant_drop",OWNER="skywars2.ant_owner.";
    private static final CompoundTag ANT=parse("""
            {Silent:1b,DeathLootTable:"lbc:ant_rewards",PersistenceRequired:1b,drop_chances:{head:0.0f},Health:10.0f,IsBaby:1b,Tags:["muravei","lucky_mob"],CustomName:{"translate":"muravei"},equipment:{head:{id:"minecraft:repeating_command_block",components:{"minecraft:item_model":"lbc:muravei","minecraft:unbreakable":{}},count:1}},active_effects:[{id:"minecraft:invisibility",amplifier:1b,duration:-1,show_particles:0b}],attributes:[{id:"minecraft:max_health",base:10},{id:"minecraft:follow_range",base:50},{id:"minecraft:movement_speed",base:0.25},{id:"minecraft:attack_damage",base:3},{id:"minecraft:armor",base:5},{id:"minecraft:armor_toughness",base:5},{id:"minecraft:spawn_reinforcements",base:100}]}
            """);
    private final GameController game;
    private final Set<Entity> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();
    public LbcAntDrops(GameController game){this.game=game;}
    private static CompoundTag parse(String text){try{return TagParser.parseCompoundFully(text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new ExceptionInInitializerError(e);}}
    public static CompoundTag payload(){return ANT.copy();}
    public boolean ready(ItemEntity token,LbcAntDropItem.Kind kind){
        if(token.isRemoved() || token.getAge()>=5999 || !LbcAntDropItem.matches(token.getItem(),kind)
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        var pos=token.blockPosition();return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    public boolean spawn(ItemEntity token,LbcAntDropItem.Kind kind){
        if(!ready(token,kind))return false;
        var level=(ServerLevel)token.level();var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        if(author!=null)authors.put(author.getUUID(),author);
        if(kind==LbcAntDropItem.Kind.ACID_BURST){
            for(int i=1;i<=3;i++){
                var origin=token.position().add(0,1+i*.01,0);
                if(Level.isInSpawnableBounds(BlockPos.containing(origin)))
                    game.items().clouds().sourceAcid(level,origin,(level.getRandom().nextInt(360001)-180000)*.001f,
                            (level.getRandom().nextInt(180001)-90000)*.001f,500,false,author);
            }
            game.items().clouds().sourceSound(level,token.position().add(0,1,0),SoundEvents.SPIDER_DEATH,2,1.3f);
        }else if(Level.isInSpawnableBounds(token.blockPosition())){
            for(int i=0;i<5;i++){
                var ant=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);if(ant==null)continue;
                ant.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),payload()));
                ant.snapTo(token.getX(),token.getY(),token.getZ(),ant.getYRot(),ant.getXRot());
                tag(ant,author);game.items().timeStop().prepareFrozen(ant);
                if(!level.addFreshEntity(ant)){tracked.remove(ant);ant.discard();}
            }
        }
        return true;
    }
    void adoptEgg(Zombie actor,ServerPlayer author){
        tag(actor,author);game.items().timeStop().prepareFrozen(actor);
    }
    private void tag(Entity actor,ServerPlayer author){
        actor.entityTags().add(ACTOR);game.items().markCurrentItemEpoch(actor);
        if(author!=null){authors.put(author.getUUID(),author);actor.entityTags().add(OWNER+author.getUUID());}
    }
    public static boolean managed(Entity actor){return (actor instanceof Zombie || actor instanceof Chicken) && actor.entityTags().contains(ACTOR);}
    public static boolean caster(Entity actor){return actor instanceof Zombie && managed(actor) && actor.entityTags().contains("muravei");}
    private boolean active(Entity actor){return game.playing() && game.stage()!=GameController.Stage.ENDING
            && actor.level()==game.maps().level() && game.items().currentEpoch(actor);}
    public LivingEntity owner(Entity actor){
        if(actor instanceof ItemEntity token && LbcAntDropItem.matches(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER))try{
            var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);
            return online==null?authors.get(id):online;
        }catch(IllegalArgumentException ignored){}
        return null;
    }
    private boolean friendlyFire(){return true;} // User: ants are dangerous to everyone.
    public boolean acidWild(Entity source){return friendlyFire();}
    public boolean wild(Entity source){return managed(source) && friendlyFire();}
    public boolean acidCanAffect(Entity source,Entity target){
        if(target==null || !game.items().wands().validTarget(target))return false;
        var owner=game.items().clouds().sourceAcidOwner(source);
        return friendlyFire() || !(owner instanceof ServerPlayer author) || game.items().wands().target(author,target);
    }
    public boolean canAffect(Entity source,Entity target){
        if(!managed(source))return true;
        if(!active(source) || target==null || !game.items().wands().validTarget(target))return false;
        return friendlyFire() || !(owner(source) instanceof ServerPlayer author) || game.items().wands().target(author,target);
    }
    public boolean canTarget(Entity source,LivingEntity target){return target==null || canAffect(source,target);}
    public void inheritReinforcement(Entity parent,Entity child){
        if(!managed(parent) || !active(parent) || !(child instanceof Zombie))return;
        // The native factory/finalizeSpawn owns all gear, AI and loot. Only causal lifecycle tags cross.
        tag(child,LbcIndirectKills.playerSource(owner(parent)));game.items().timeStop().prepareFrozen(child);
        var zombie=(Zombie)child;if(zombie.getTarget()!=null && !canTarget(child,zombie.getTarget()))zombie.setTarget(null);
    }
    /** Native finalizeSpawn's newly allocated mount only; never called for a preexisting chicken. */
    public void inheritJockey(Entity parent,Entity child){
        if(!managed(parent) || !active(parent) || !(child instanceof Chicken))return;
        tag(child,LbcIndirectKills.playerSource(owner(parent)));game.items().timeStop().prepareFrozen(child);
    }
    public void inheritConversion(Entity parent,Entity child){
        if(!managed(parent) || !active(parent) || !(child instanceof Zombie))return;
        tag(child,LbcIndirectKills.playerSource(owner(parent)));game.items().timeStop().prepareFrozen(child);
        var zombie=(Zombie)child;if(zombie.getTarget()!=null && !canTarget(child,zombie.getTarget()))zombie.setTarget(null);
    }
    public boolean residentReinforcement(Entity source,LevelReader level,BlockPos pos){
        if(!managed(source) || !(level instanceof ServerLevel server))return true;
        for(int x=(pos.getX()-1)>>4;x<=(pos.getX()+1)>>4;x++)
            for(int z=(pos.getZ()-1)>>4;z<=(pos.getZ()+1)>>4;z++)if(server.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    public LivingEntity target(Zombie ant){
        LivingEntity result=null;double nearest=Double.POSITIVE_INFINITY;
        for(var candidate:((ServerLevel)ant.level()).getEntitiesOfClass(LivingEntity.class,ant.getBoundingBox().inflate(10),e->
                !e.entityTags().contains("muravei") && e.getType().builtInRegistryHolder().is(
                        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.Identifier.parse("minecraft:mobs")))
                        && canAffect(ant,e))){
            double distance=candidate.distanceToSqr(ant);
            if(distance<=100 && distance<nearest){result=candidate;nearest=distance;}
        }
        return result;
    }
    public LbcClouds.AcidProjectile shoot(Zombie ant,LivingEntity target){
        if(!caster(ant) || !active(ant) || game.items().timeStop().stopped((ServerLevel)ant.level()) || target==null)return null;
        var origin=ant.getEyePosition();var direction=target.getEyePosition().subtract(origin);
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        var random=((ServerLevel)ant.level()).getRandom();
        yaw=(Mth.floor(yaw*100)+random.nextInt(6001)-3000)*.01f;
        pitch=(Mth.floor(pitch*100)+random.nextInt(4001)-2000)*.01f;
        var acid=game.items().clouds().sourceAcid((ServerLevel)ant.level(),origin,yaw,pitch,300,true,LbcIndirectKills.playerSource(owner(ant)));
        acid.entityTags().add("skywars2.ant_acid");
        game.items().clouds().sourceSound((ServerLevel)ant.level(),ant.position(),SoundEvents.ZOMBIE_VILLAGER_CURE,1,2);
        return acid;
    }
    public void loaded(Entity actor){if(!managed(actor))return;if(!active(actor)){actor.discard();return;}tracked.add(actor);game.items().timeStop().prepareFrozen(actor);}
    public void unloaded(Entity actor){tracked.remove(actor);}
    public void tick(){
        for(var actor:List.copyOf(tracked)){
            if(actor.isRemoved()){tracked.remove(actor);continue;}
            if(!active(actor)){tracked.remove(actor);actor.discard();continue;}
            var level=(ServerLevel)actor.level();if(!caster(actor) || !actor.isAlive() || game.items().timeStop().stopped(level))continue;
            var ant=(Zombie)actor;
            if(level.getRandom().nextFloat()<.00125f)game.items().clouds().sourceSound(level,ant.position(),SoundEvents.BAT_TAKEOFF,1,1.6f);
            if(level.getRandom().nextFloat()<.03f){var target=target(ant);if(target!=null)shoot(ant,target);}
        }
    }
    public void stop(){var actors=List.copyOf(tracked);tracked.clear();for(var actor:actors)if(!actor.isRemoved())actor.discard();authors.clear();}
    public List<Entity> snapshot(){return List.copyOf(tracked);}
    public int trackedCount(){return tracked.size();}

    LbcCloneSkills.ActorImage cloneImage(Entity source) {
        if(!managed(source))return null;
        var cause=LbcIndirectKills.playerSource(owner(source));
        boolean knownCause=cause==null||authors.containsKey(cause.getUUID());
        return new LbcCloneSkills.ActorImage() {
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                if(cause!=null)authors.putIfAbsent(cause.getUUID(),cause);
                // Exact age/lineage/source flags are in native tags; loaded admits the fresh body.
            }
            public void rollback(Entity clone){tracked.remove(clone);if(!knownCause)authors.remove(cause.getUUID(),cause);}
        };
    }
}
