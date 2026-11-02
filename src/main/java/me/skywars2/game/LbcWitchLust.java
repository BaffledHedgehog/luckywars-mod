package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Flesh modes share the existing health, source, native dragon and player visibility owners. */
public final class LbcWitchLust {
    private static final String MANAGED="skywars2.witch.lust_actor",DRAGON="skywars2.witch.lust_dragon";
    private static final String PAYLOAD="{Tags:[\"lust_dragon\",\"lucky_mob\"],PersistenceRequired:1b,Health:20.0f,CustomName:{\"italic\":false,\"translate\":\"lust_dragon\"},attributes:[{id:\"minecraft:max_health\",base:20},{id:\"minecraft:follow_range\",base:200}],DragonPhase:1}";
    private final GameController game;
    private IdentityHashMap<Entity,LbcSpellSource.Source> actors=new IdentityHashMap<>();
    private final Map<UUID,LbcSpellSource.Source> unloaded=new HashMap<>();
    private final Set<ServerPlayer> hidden=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    private LbcSpellSource.Source producing;private int phaseOffset;private boolean advancing;
    private int lastTick=Integer.MIN_VALUE;
    LbcWitchLust(GameController game){this.game=game;}
    public static boolean matches(ItemStack item){return !item.isEmpty()&&LbcFartus.data(item).getIntOr("lust_cooldown",0)==1;}
    private boolean live(){return game.playing()&&game.stage()!=GameController.Stage.ENDING;}
    private boolean eligible(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&!game.isObserver(p)&&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p)&&game.items().wands().validRoleTarget(p,false);}
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    private List<? extends Entity> select(ServerLevel level,Vec3 at,String literal){try{
        var selector=selectors.get(literal);if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();selectors.put(literal,selector);}
        return List.copyOf(selector.findEntities(game.server().createCommandSourceStack().withLevel(level).withPosition(at).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput()));
    }catch(CommandSyntaxException error){throw new IllegalStateException("Lust source selector "+literal,error);}}
    private ServerPlayer holder(Entity e){return select((ServerLevel)e.level(),e.position(),"@p[gamemode=!spectator,tag=sin_lust]").stream().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).filter(this::eligible).findFirst().orElse(null);}
    public boolean use(ServerPlayer p,InteractionHand hand){
        if(!matches(p.getItemInHand(hand))||!live()||!eligible(p)||!p.entityTags().contains("sin_lust")||paused(p.level())
                ||game.items().actionsBlocked(p)||game.items().wands().inputTimeStopped(p)||game.items().magic().suppressed(p)
                ||p.entityTags().contains("nomagic_active")||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        boolean applied;
        if(p.isShiftKeyDown()){
            if(positive(p,"lust_cd"))return false;
            if(p.onGround()){area(p);game.items().mana().score(p,"lust_cd").set(30);applied=true;}
            else {produce(p,false);p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),p);
                p.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,20),p);game.items().mana().score(p,"lust_cd").set(10);applied=true;}
        }else if(p.onGround())applied=swap(p);
        else {if(positive(p,"lust_cd_small"))return false;applied=produce(p,true);if(applied){p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),p);game.items().mana().score(p,"lust_cd_small").set(10);}}
        if(applied)game.items().wands().wandCasted(p,false);return applied;
    }
    private boolean positive(ServerPlayer p,String name){Integer n=new LbcSourceCounters(game).read(p,name);return n!=null&&n>0;}
    private boolean swap(ServerPlayer p){
        var targets=select(p.level(),p.position(),"@e[type=#minecraft:mobs,tag=!spectator,tag=!owner,sort=random]").stream()
                .filter(e->e!=p&&game.items().wands().validRoleTarget(e,false)&&!game.items().polymorph().concealed(e)&&!LbcGreed.active(e)
                        &&!game.items().magic().suppressedAt((ServerLevel)e.level(),e.position())).toList();
        if(targets.isEmpty())return false;var target=targets.getFirst();var from=p.position();var dest=target.position();var destination=(ServerLevel)target.level();
        var level=p.level();float yaw=p.getYRot(),pitch=p.getXRot();var motion=p.getDeltaMovement();
        try{p.teleportTo(destination,dest.x,dest.y,dest.z,TeleportMomentum.positionFlags(Set.of()),target.getYRot(),target.getXRot(),true);}
        finally{p.setDeltaMovement(motion);}
        var move=target.getDeltaMovement();var arrived=target.teleport(new net.minecraft.world.level.portal.TeleportTransition(level,from,Vec3.ZERO,yaw,pitch,TeleportMomentum.positionFlags(Set.of()),net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        if(arrived!=null)arrived.setDeltaMovement(move);return arrived!=null;
    }
    private void area(ServerPlayer p){var targets=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(20),e->
            e!=p&&e.getType().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs")))
            &&p.distanceToSqr(e)>=.0001&&p.distanceToSqr(e)<=400&&!e.entityTags().contains("lust_dragon")&&game.items().wands().target(p,e)
            &&!game.items().magic().suppressedAt((ServerLevel)e.level(),e.position()));
        game.items().witchPassives().transferHealth(p,targets,8);
    }
    private boolean produce(ServerPlayer p,boolean dragon){
        var source=new LbcSpellSource.Source(p.getUUID(),p.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(p),Set.of("strict_map_object","rcdenyalways","magic_academy"),true);
        var previous=producing;producing=source;
        try(var scope=game.items().spellSources().scope(source,p.level())){
            if(!dragon){for(int i=0;i<20;i++)LbcRandomMobSpawn.random(game,p);return true;}
            var at=p.position().add(p.getLookAngle().scale(3));var pos=BlockPos.containing(at);
            if(!p.level().getWorldBorder().isWithinBounds(pos)||p.level().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)return false;
            try{var entity=SummonCommand.createEntity(p.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput(),
                    BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse("minecraft:ender_dragon")).orElseThrow(),at,TagParser.parseCompoundFully(PAYLOAD),false);
                if(!(entity instanceof EnderDragon d)||d.isRemoved())return false;d.entityTags().add(DRAGON);
                game.items().communism().subjugateAt(p,p.level(),at,e->true);d.setDeltaMovement(p.getLookAngle().scale(2));
                p.startRiding(d,true,true);refresh(p);return true;
            }catch(CommandSyntaxException rejected){return false;}
        }finally{producing=previous;}
    }
    public boolean controlled(Entity e){return e instanceof EnderDragon&&!e.isRemoved()&&actors.containsKey(e)&&e.entityTags().contains(DRAGON)&&game.items().currentEpoch(actors.get(e).epoch());}
    public boolean riding(ServerPlayer p){return controlled(p.getVehicle());}
    private void refresh(ServerPlayer p){if(riding(p))hidden.add(p);else hidden.remove(p);FullInvisibility.effectsChanged(p);}
    public Vec3 flight(EnderDragon dragon){var p=holder(dragon);if(p==null||p.level()!=dragon.level())return Vec3.ZERO;
        var step=p.getLookAngle().scale(2);var next=BlockPos.containing(dragon.position().add(step));
        if(!((ServerLevel)dragon.level()).getWorldBorder().isWithinBounds(next)||((ServerLevel)dragon.level()).getChunkSource().getChunkNow(next.getX()>>4,next.getZ()>>4)==null)return Vec3.ZERO;
        dragon.setYRot(p.getYRot()+180);dragon.setXRot(p.getXRot());return step;}
    public boolean harmless(Entity dragon,Entity target){return controlled(dragon)&&(target==holder(dragon)||dragon.getPassengers().contains(target));}
    public void tick(){if(advancing)throw new IllegalStateException("Reentrant authority advance");advancing=true;try{int now=game.server().getTickCount();if(lastTick==now||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        for(var e:List.copyOf(actors.keySet())){
            var source=actors.get(e);if(e.isRemoved()){actors.remove(e);continue;}if(!live()||!game.items().currentEpoch(source.epoch())){retire(e);continue;}
            if(paused((ServerLevel)e.level())||game.items().timeStop().frozen(e)||game.items().polymorph().suspended(e))continue;
            var p=holder(e);if(p!=null&&e instanceof LivingEntity&&!source.author().equals(p.getUUID())){
                if(controlled(e))game.items().communism().subjugate(p,e,true,true);else game.items().spellSources().controllerChanged(e,p);actors.put(e,game.items().spellSources().source(e));}
            if(!controlled(e))continue;
            if(game.items().magic().suppressedAt((ServerLevel)e.level(),e.position())||p!=null&&(game.items().magic().suppressed(p)||p.entityTags().contains("nomagic_active"))){retire(e);continue;}
            for(var passenger:List.copyOf(e.getPassengers()))if(passenger instanceof ServerPlayer old&&old!=p){old.stopRiding();refresh(old);}
            if(p!=null&&p.level()==e.level()&&p.distanceToSqr(e)<=400&&!riding(p)){p.startRiding(e,true,true);refresh(p);}
            if((now+phaseOffset)%5==0){if(p==null||p.level()!=e.level()||p.distanceToSqr(e)>400||select((ServerLevel)e.level(),e.position(),"@a[gamemode=!spectator,tag=sin_lust]").stream().anyMatch(e2->e2 instanceof ServerPlayer s&&s.isShiftKeyDown())){retire(e);continue;}
                p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),p);}
        }
        for(var p:List.copyOf(hidden))if(!riding(p))refresh(p);
    }finally{advancing=false;}}
    private void retire(Entity e){var passengers=List.copyOf(e.getPassengers());e.ejectPassengers();actors.remove(e);e.discard();for(var passenger:passengers)if(passenger instanceof ServerPlayer p)refresh(p);}
    public void loaded(Entity e){
        var birth=producing!=null?producing:game.items().spellSources().source(e);
        boolean descendant=birth!=null&&actors.keySet().stream().anyMatch(parent->parent!=e&&parent.getUUID().equals(birth.emitter()));
        if((producing!=null||descendant)&&!(e instanceof net.minecraft.world.entity.player.Player)&&!(e instanceof net.minecraft.world.entity.item.ItemEntity)&&!(e instanceof ExperienceOrb)){
            // A projectile keeps its accepted parent cause; new living emitters own their future attacks.
            var s=e instanceof LivingEntity?new LbcSpellSource.Source(e.getUUID(),birth.author(),birth.epoch(),birth.cause(),birth.excluded(),true):birth;
            actors.put(e,s);e.entityTags().addAll(Set.of(MANAGED,s.epoch()));game.items().spellSources().bind(e,s);game.items().timeStop().prepareFrozen(e);return;}
        if(!e.entityTags().contains(MANAGED))return;var s=actors.get(e);if(s==null)s=unloaded.remove(e.getUUID());
        if(s==null||!live()||!game.items().currentEpoch(s.epoch())){e.discard();return;}actors.put(e,s);game.items().spellSources().bind(e,s);game.items().timeStop().prepareFrozen(e);
    }
    public void unloaded(Entity e){var s=actors.remove(e);if(s!=null&&e.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(e.getUUID(),s);}
    public void forget(ServerPlayer p){hidden.remove(p);if(riding(p))p.stopRiding();FullInvisibility.effectsChanged(p);}
    public void stop(){for(var e:List.copyOf(actors.keySet()))retire(e);for(var p:List.copyOf(hidden)){hidden.remove(p);FullInvisibility.effectsChanged(p);}actors.clear();unloaded.clear();producing=null;lastTick=Integer.MIN_VALUE;phaseOffset=0;}
    List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    void checkpointBoundary(){if(advancing||producing!=null)throw new IllegalStateException("Finish Lust producer/advance callbacks first");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private final MatchSpellCheckpoint.IdentityImage<Entity,LbcSpellSource.Source> image;
        private final Map<UUID,LbcSpellSource.Source> asleep=new HashMap<>(unloaded);
        private final List<MatchDamageSources.Ref> players;
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,phaseOffset);
        Checkpoint(MatchSpellCheckpoint.Links links){image=new MatchSpellCheckpoint.IdentityImage<>(actors,links);players=hidden.stream().map(links::key).toList();}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=image.restore(links);var current=Collections.newSetFromMap(new IdentityHashMap<ServerPlayer,Boolean>());
            for(var ref:players){if(!(links.get(ref) instanceof ServerPlayer p))throw new IllegalStateException("Restore hidden Lust players first");current.add(p);}
            return ()->{actors=restored;unloaded.clear();unloaded.putAll(asleep);hidden.clear();hidden.addAll(current);lastTick=clock.last(game);phaseOffset=clock.offset(game);};}
    }

    static boolean cloneRecognizes(Entity e){return e.entityTags().contains(MANAGED);}
    static boolean cloneProtected(Entity e){return e.entityTags().contains(DRAGON);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var state=actors.get(source);if(state==null||cloneProtected(source))return null;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var next=clone instanceof LivingEntity?new LbcSpellSource.Source(clone.getUUID(),state.author(),state.epoch(),state.cause(),state.excluded(),state.persistent()):state;
                actors.put(clone,next);game.items().spellSources().bind(clone,next);
            }
            public void rollback(Entity clone){actors.remove(clone);}
        };
    }

}
