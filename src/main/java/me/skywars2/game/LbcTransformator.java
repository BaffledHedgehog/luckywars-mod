package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** One owned encounter pair; native entity ticks own gravity, health, effects and fireball collision. */
public final class LbcTransformator {
    private static final String ACTOR="skywars2.transformator.actor", TEXT="skywars2.transformator.text",
            ATTACK="skywars2.transformator.attack";
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(
            Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private final GameController game;
    private static final class Actor extends Zombie {
        Actor(ServerLevel level) {super(EntityTypes.ZOMBIE,level);}
        @Override public boolean shouldBeSaved() {return false;}
    }
    private static final class Label extends Display.TextDisplay {
        Label(ServerLevel level) {super(EntityTypes.TEXT_DISPLAY,level);}
        @Override public boolean shouldBeSaved() {return false;}
    }
    private static final class Blast extends LargeFireball {
        final LivingEntity cause;
        Blast(ServerLevel level,LivingEntity cause) {super(level,cause,new Vec3(0,-1,0),12);this.cause=cause;}
        // AbstractHurtingProjectile discards if getOwner() returns a removed entity.
        // Retain the cause separately; native flight has no removed owner to follow.
        @Override public Entity getOwner() {var owner=super.getOwner();return owner==null || owner.isRemoved()?null:owner;}
        @Override public void tick() {
            if(!blastResident((ServerLevel)level(),position(),getDeltaMovement(),12)) {discard();return;}
            super.tick();
        }
        @Override public boolean shouldBeSaved() {return false;}
    }
    private static final class Pair {
        final Zombie actor; final Display.TextDisplay text; final String epoch;
        int ticks;
        Pair(Zombie actor,Display.TextDisplay text,String epoch) {this.actor=actor;this.text=text;this.epoch=epoch;}
    }
    public record View(Zombie actor,Display.TextDisplay text,int activeTicks) { }
    private final Set<Pair> pairs=new LinkedHashSet<>();
    private final Set<Entity> attacks=Collections.newSetFromMap(new IdentityHashMap<>());
    // Causes survive terminal removal while a launched ray/fireball still references its source.
    private final Map<Entity,LivingEntity> authors=new WeakHashMap<>();
    private final Map<UUID,LivingEntity> unloadedAuthors=new HashMap<>();
    LbcTransformator(GameController game) {this.game=game;}
    private static void load(Entity entity,String snbt) {
        try {entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),TagParser.parseCompoundFully(snbt)));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) {throw new IllegalStateException(error);}
    }
    public static boolean managed(Entity entity) {
        return entity!=null && (entity.entityTags().contains(ACTOR) || entity.entityTags().contains(TEXT)
                || entity.entityTags().contains(ATTACK));
    }
    public LivingEntity owner(Entity entity) {
        if(entity==null) return null;
        if(entity instanceof net.minecraft.world.entity.projectile.Projectile shot && shot.getOwner() instanceof ServerPlayer player)
            return player;
        var reflected=game.items().electro().reflectedOwner(entity);
        return reflected==null?authors.get(entity):reflected;
    }
    boolean ready(ItemEntity token) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcNpcDropItems.isTransformator(token.getItem())
                || !(token.level() instanceof ServerLevel level) || level!=game.maps().level()
                || !game.playing() || game.stage()==GameController.Stage.ENDING) return false;
        var pos=token.blockPosition();
        if(level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.below(2))) return false;
        for(int x=(pos.getX()-1)>>4;x<=(pos.getX()+1)>>4;x++)
            for(int z=(pos.getZ()-1)>>4;z<=(pos.getZ()+1)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    private static boolean resident(ServerLevel level,BlockPos pos) {return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    /** Cover native ray discovery, exposure and movement reads without requesting surrounding chunks. */
    private static boolean blastResident(ServerLevel level,Vec3 at,Vec3 movement,float power) {
        var box=new AABB(at,at.add(movement)).inflate(power*2+2);
        for(int x=net.minecraft.util.Mth.floor(box.minX)>>4;x<=net.minecraft.util.Mth.floor(box.maxX)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(box.minZ)>>4;z<=net.minecraft.util.Mth.floor(box.maxZ)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    void spawn(ItemEntity token) {
        if(!ready(token)) return;
        var level=(ServerLevel)token.level(); var floor=token.blockPosition();
        var at=new Vec3(floor.getX()+.5,floor.getY()-.1,floor.getZ()+.5);
        var actor=new Actor(level);
        load(actor,"{DeathLootTable:\"minecraft:empty\",Silent:1b,CustomNameVisible:1b,PersistenceRequired:1b,NoAI:1b,Health:500f,IsBaby:0b,CustomName:{color:'gold',italic:false,translate:'transformator',extra:[{color:'gray',text:' Nightmare23 Birthday'}]},equipment:{head:{id:'minecraft:repeating_command_block',count:1,components:{'minecraft:item_model':'lbc:transformator'}}},drop_chances:{head:0f},active_effects:[{id:'minecraft:invisibility',amplifier:1,duration:2000000,show_particles:0b}],attributes:[{id:'minecraft:max_health',base:500d},{id:'minecraft:knockback_resistance',base:100d}]}");
        // Effect metadata normally refreshes on the first living tick; the spawn packet must already hide the zombie.
        actor.setInvisible(true);actor.setPos(at);actor.entityTags().addAll(List.of(ACTOR,"transformator","lucky_mob"));
        var text=new Label(level);
        load(text,"{view_range:2f,shadow_radius:0f,shadow_strength:0f,width:200f,height:200f,billboard:'center',see_through:1b,alignment:'center',text:[{translate:'transformator',color:'gold',italic:false},{text:' Nightmare23 Birthday',color:'gray'}]}");
        text.setPos(at.add(0,3,0));text.entityTags().addAll(List.of(TEXT,"tr_text"));
        game.items().markCurrentItemEpoch(actor);game.items().markCurrentItemEpoch(text);
        var author=game.items().specialDrops().owner(token);
        if(author!=null) authors.put(actor,author);
        var pair=new Pair(actor,text,game.items().epoch());pairs.add(pair);
        if(!level.addFreshEntity(actor) || !level.addFreshEntity(text)) {terminal(pair);return;}
        game.items().timeStop().prepareFrozen(actor);game.items().timeStop().prepareFrozen(text);
    }
    /** The source egg has a visible custom name and no separate text-display label. */
    void adoptEgg(Zombie actor,LivingEntity author) {
        ((me.skywars2.mixin.MobLootAccess)actor).skywars2$lootTable(Optional.of(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:empty"))));
        actor.entityTags().addAll(List.of(ACTOR,"transformator","lucky_mob"));
        actor.setInvisible(true);game.items().markCurrentItemEpoch(actor);
        if(author!=null) authors.put(actor,author);
        pairs.add(new Pair(actor,null,game.items().epoch()));game.items().timeStop().prepareFrozen(actor);
    }
    void tick() {
        attacks.removeIf(Entity::isRemoved);
        for(var attack:List.copyOf(attacks)) if(!game.items().currentEpoch(attack) || !game.playing()
                || game.stage()==GameController.Stage.ENDING) {attacks.remove(attack);attack.discard();}
        for(var pair:List.copyOf(pairs)) {
            var actor=pair.actor;var level=(ServerLevel)actor.level();
            if(!game.items().currentEpoch(pair.epoch) || !game.playing() || game.stage()==GameController.Stage.ENDING
                    || actor.isRemoved() || !actor.isAlive() || pair.text!=null && pair.text.isRemoved()
                    || !resident(level,actor.blockPosition())) {terminal(pair);continue;}
            if(game.items().magic().mechanismSuppressedAt(level,actor.position())) {terminal(pair);continue;}
            if(game.items().timeStop().stopped(level)) continue;
            if(++pair.ticks%5==0) pulse(actor);
        }
    }
    /** The producer is mechanism-driven; magic-only beacons do not suppress it. */
    void pulse(Zombie actor) {
        var pair=find(actor);
        if(pair==null || !actor.isAlive() || game.items().timeStop().stopped((ServerLevel)actor.level())
                || game.items().magic().mechanismSuppressedAt((ServerLevel)actor.level(),actor.position())) return;
        var random=actor.getRandom();var level=(ServerLevel)actor.level();
        if(random.nextFloat()<.1f) burst(actor);
        // (user) Preserve the undead 6/12 damage amounts, without scheduling Instant Health effects.
        if(random.nextFloat()<.05f) actor.hurtServer(level,actor.damageSources().magic(),6);
        if(random.nextFloat()<.05f) actor.hurtServer(level,actor.damageSources().magic(),12);
        if(!actor.isAlive() || !pairs.contains(pair)) return;
        int health=(int)actor.getHealth();
        if(health<=490 && random.nextFloat()<.5f) warning(actor);
        if(health<=480) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,actor.getX(),actor.getY()+1,actor.getZ(),20,.3,.3,.3,.01);
        if(health<=470 && random.nextFloat()<.5f) sound(actor,"minecraft:entity.zombie_villager.cure",1,1);
        if(health<=460 && random.nextFloat()<.5f) sound(actor,"minecraft:entity.zombie_villager.cure",2,1);
        if(health<=450) game.items().indirectKills().worldChanges(level,actor,true,()-> {
            for(var pos:BlockPos.betweenClosed(actor.blockPosition().offset(-1,-1,-1),actor.blockPosition().offset(1,1,1)))
                if(!level.isOutsideBuildHeight(pos) && resident(level,pos) && level.getBlockState(pos).is(AIRS))
                    level.setBlock(pos,Blocks.FIRE.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        });
        if(health<=440) explode(pair);
    }
    void burst(Zombie actor) {
        if(find(actor)==null || !actor.isAlive() || game.items().timeStop().stopped((ServerLevel)actor.level())
                || game.items().magic().mechanismSuppressedAt((ServerLevel)actor.level(),actor.position())) return;
        var level=(ServerLevel)actor.level(); var origin=actor.position().add(0,1,0);
        LivingEntity nearest=null; double distance=30*30;
        for(var entity:level.getEntities(actor,new AABB(origin,origin).inflate(30),entity->entity instanceof LivingEntity
                && entity.getType().builtInRegistryHolder().is(MOBS) && !entity.entityTags().contains("transformator")
                && game.items().wands().validTarget(entity))) {
            double d=entity.position().distanceToSqr(origin);
            if(d<=distance) {nearest=(LivingEntity)entity;distance=d;}
        }
        var direction=nearest==null?actor.getLookAngle():nearest.getEyePosition().subtract(origin).normalize();
        for(int i=0;i<7;i++) game.items().wands().mechanismElectro(actor,origin,direction);
        sound(actor,"minecraft:entity.firework_rocket.twinkle",1,1);
    }
    private void warning(Zombie actor) {
        sound(actor,"minecraft:entity.creeper.primed",1,1);
        var level=(ServerLevel)actor.level();var forward=Vec3.directionFromRotation(actor.getXRot(),actor.getYRot());
        var left=Vec3.directionFromRotation(0,actor.getYRot()-90);
        for(double y:new double[]{1,2.5,0}) for(var point:new double[][]{{0,-1},{0,-.7},{0,-1.3},{.3,-1},{-.3,-1}})
            if(actor.getRandom().nextFloat()<.85f) {
                var at=actor.position().add(left.scale(point[0])).add(0,y,0).add(forward.scale(point[1]));
                level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,at.x,at.y,at.z,0,0,1,0,.6);
            }
    }
    private void explode(Pair pair) {
        var actor=pair.actor;var level=(ServerLevel)actor.level();
        var ball=new Blast(level,actor);ball.setPos(actor.position().add(0,2,0));ball.setDeltaMovement(0,-10,0);
        ball.entityTags().add(ATTACK);game.items().markCurrentItemEpoch(ball);attacks.add(ball);
        var author=owner(actor);if(author!=null) authors.put(ball,author);
        if(!level.addFreshEntity(ball)) {attacks.remove(ball);ball.discard();}
        game.items().indirectKills().worldChanges(level,actor,true,()-> {
            var below=actor.blockPosition().below();
            if(!level.isOutsideBuildHeight(below) && resident(level,below)) {
                if(level.getBlockEntity(below) instanceof net.minecraft.world.Container container) {
                    net.minecraft.world.Containers.dropContents(level,below,container);container.clearContent();
                }
                level.setBlock(below,Blocks.DIRT.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            }
        });
        // Source /kill is a genuine native death, unlike quiet lifecycle cleanup.
        actor.hurtServer(level,actor.damageSources().genericKill(),Float.MAX_VALUE);
        terminal(pair);
    }
    private Pair find(Zombie actor) {for(var pair:pairs) if(pair.actor==actor)return pair;return null;}
    private void terminal(Pair pair) {
        pairs.remove(pair);
        if(pair.text!=null && !pair.text.isRemoved()) pair.text.discard();
        if(!pair.actor.isRemoved()) pair.actor.discard();
    }
    private static void sound(Zombie actor,String id,float volume,float pitch) {
        actor.level().playSound(null,actor.getX(),actor.getY(),actor.getZ(),SoundEvent.createVariableRangeEvent(Identifier.parse(id)),SoundSource.MASTER,volume,pitch);
    }
    public boolean canAffect(Entity source,Entity target) {
        if(!managed(source)) return true;
        if(source instanceof net.minecraft.world.entity.projectile.Projectile shot && shot.getOwner() instanceof ServerPlayer player)
            return game.items().wands().projectileTarget(player,target);
        return game.items().wands().validTarget(target);
    }
    public boolean allowExplosion(Entity source,ServerLevel level,Vec3 at,float power) {
        return !managed(source) || game.items().currentEpoch(source) && game.playing()
                && game.stage()!=GameController.Stage.ENDING && blastResident(level,at,Vec3.ZERO,power);
    }
    public void loaded(Entity entity) {
        if(!managed(entity)) return;
        if(!game.items().currentEpoch(entity) || !game.playing() || game.stage()==GameController.Stage.ENDING) {
            unloadedAuthors.remove(entity.getUUID());entity.discard();return;
        }
        if(entity.entityTags().contains(ATTACK)) {
            var author=unloadedAuthors.remove(entity.getUUID());if(author!=null) authors.put(entity,author);
            attacks.add(entity);
        }
    }
    public void inheritChild(Entity source,Entity child) {
        if(!managed(source) || !game.items().currentEpoch(source)
                || !(child instanceof net.minecraft.world.entity.item.PrimedTnt)) return;
        child.entityTags().add(ATTACK);game.items().markCurrentItemEpoch(child);
        var author=LbcIndirectKills.playerSource(source);if(author!=null) authors.put(child,author);
        attacks.add(child);game.items().timeStop().prepareFrozen(child);
    }
    public void unloaded(Entity entity) {
        if(!managed(entity)) return;
        for(var pair:List.copyOf(pairs)) if(pair.actor==entity || pair.text==entity) terminal(pair);
        attacks.remove(entity);
        if(entity.getRemovalReason()!=null && !entity.getRemovalReason().shouldDestroy() && authors.get(entity)!=null)
            unloadedAuthors.put(entity.getUUID(),authors.get(entity));
    }
    void stop() {
        for(var pair:List.copyOf(pairs)) terminal(pair);
        for(var attack:List.copyOf(attacks)) attack.discard();
        attacks.clear();authors.clear();unloadedAuthors.clear();
    }
    List<View> snapshot() {return pairs.stream().map(pair->new View(pair.actor,pair.text,pair.ticks)).toList();}
    List<Entity> attackSnapshot() {return List.copyOf(attacks);}
    int activeCount() {return pairs.size();}

    static boolean cloneProtected(Entity actor){return actor instanceof Label || actor.entityTags().contains(TEXT);}
    LbcCloneSkills.ActorImage cloneImage(Entity source) {
        if(!managed(source)||cloneProtected(source))return null;
        var pair=source instanceof Zombie zombie?find(zombie):null;
        if(source.entityTags().contains(ACTOR)&&pair==null)return null;
        boolean hasLabel=pair!=null&&pair.text!=null;int ticks=pair==null?0:pair.ticks;
        var epoch=pair==null?game.items().epoch():pair.epoch;
        var author=authors.get(source);if(author==null)author=unloadedAuthors.get(source.getUUID());
        final var causalAuthor=author;
        var blastCause=source instanceof Blast blast?blast.cause:null;
        return new LbcCloneSkills.ActorImage() {
            public Entity create(Entity original,ServerLevel level){
                if(original instanceof Actor)return new Actor(level);
                if(original instanceof Blast)return new Blast(level,blastCause);
                return LbcCloneSkills.ActorImage.super.create(original,level);
            }
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                if(causalAuthor!=null)authors.put(clone,causalAuthor);
                if(clone.entityTags().contains(ATTACK))attacks.add(clone);
                if(clone.entityTags().contains(ACTOR)){
                    Label label=null;
                    if(hasLabel){label=new Label((ServerLevel)clone.level());
                        load(label,"{view_range:2f,shadow_radius:0f,shadow_strength:0f,width:200f,height:200f,billboard:'center',see_through:1b,alignment:'center',text:[{translate:'transformator',color:'gold',italic:false},{text:' Nightmare23 Birthday',color:'gray'}]}");
                        label.setPos(clone.position().add(0,3,0));label.entityTags().addAll(List.of(TEXT,"tr_text"));
                        game.items().markCurrentItemEpoch(label);game.items().spellSources().inherit(clone,label);
                    }
                    var copied=new Pair((Zombie)clone,label,epoch);copied.ticks=ticks;pairs.add(copied);
                    if(label!=null){game.items().timeStop().prepareFrozen(label);
                        if(!clone.level().addFreshEntity(label)||label.isRemoved())throw new IllegalStateException("Transformator clone label publication failed");}
                }
            }
            public void rollback(Entity clone){
                for(var pair:List.copyOf(pairs))if(pair.actor==clone)terminal(pair);
                attacks.remove(clone);authors.remove(clone);unloadedAuthors.remove(clone.getUUID());
            }
        };
    }
}
