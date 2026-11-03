package me.skywars2.game;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Original hostile Sauvojen ghast and stationary nine-family spell emitters. */
public final class LbcSauvojenBoss {
    private static final String BOSS="skywars2.sauvojen_boss",CLOUD="skywars2.sauvojen_boss.cloud";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final Set<String> EXCLUDED=Set.of("sauvojen_tuntija","rcdenyalways","friendly_tuntija","strict_map_object");
    private final GameController game;
    private record Actor(UUID author,String epoch,LbcSpellSource.Source source){}
    private IdentityHashMap<Entity,Actor> actors=new IdentityHashMap<>();
    private final Map<UUID,Actor> unloaded=new HashMap<>();
    private record Cloud(LbcSauvojen.Drone visual,LbcSpellSource.Source source){}
    private final List<Cloud> clouds=new ArrayList<>();
    private final Set<Terrain> terrain=Collections.newSetFromMap(new IdentityHashMap<>());
    private int phase,callbackDepth;
    private boolean rewinding;
    private List<RestoredCloud> stagedClouds=List.of();
    private record RestoredCloud(ServerPlayer owner,Marker carrier,LbcSauvojen.Kind kind,String epoch,Vec3 position,Vec3 direction,int lifetime,int nextMode,LbcSpellSource.Source source){}
    private long selections,emissions,rewards;
    public LbcSauvojenBoss(GameController game){this.game=game;}
    public static boolean recognized(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("sauvojen_tuntija",0)==1;}
    public boolean ready(ItemEntity token){
        if(!recognized(token.getItem())||token.isRemoved()||!(token.level() instanceof ServerLevel level)||level!=game.maps().level()
                ||!game.playing()||game.stage()==GameController.Stage.ENDING||game.items().timeStop().stopped(level))return false;
        if(!game.items().currentEpoch(token))return false;
        return game.items().specialDrops().owner(token) instanceof ServerPlayer&&resident(level,token.blockPosition(),token.blockPosition().above(20));
    }
    /** Shared special drops perform the one-token debit only after this admission succeeds. */
    public boolean spawn(ItemEntity token){
        if(rewinding)return false;callbackDepth++;try{
        if(!ready(token))return false;var author=(ServerPlayer)game.items().specialDrops().owner(token);var level=author.level();var at=token.position();
        var ghast=EntityTypes.GHAST.create(level,EntitySpawnReason.COMMAND);if(ghast==null)return false;
        load(ghast,"{DeathLootTable:\"lbc:sauvojen_tuntija\",PersistenceRequired:true,ExplosionPower:3}");
        ghast.setPos(at.add(0,20,0));ghast.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100024);ghast.setHealth(100024);
        ghast.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(256);ghast.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(256);
        ghast.setCustomName(Component.literal("Sauvojen Tuntija").withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD).withStyle(s->s.withItalic(false)));
        ghast.entityTags().addAll(List.of(BOSS,"sauvojen_tuntija","rcdenyalways"));game.items().markCurrentItemEpoch(ghast);
        var source=game.items().spellSources().hostile(ghast,author,EXCLUDED);var state=new Actor(author.getUUID(),game.items().epoch(),source);
        actors.put(ghast,state);game.items().spellSources().bind(ghast,source);game.items().timeStop().prepareFrozen(ghast);
        if(!level.addFreshEntity(ghast)){actors.remove(ghast);ghast.discard();return false;}
        LbcOverload.sound(level,at,"minecraft:entity.wither.spawn",1,1,0,.5f);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,at.x,at.y+10,at.z,1000,1,10,1,0);return true;

        }finally{callbackDepth--;}
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(BOSS);}
    public ServerPlayer owner(Entity entity){
        var state=actors.get(entity);if(state==null)return game.items().spellSources().owner(entity);
        return game.server().getPlayerList().getPlayer(state.author());
    }
    public boolean canAffect(Entity source,Entity target){
        var state=actors.get(source);var authority=state==null?game.items().spellSources().source(source):state.source();
        if(authority==null)return false;var author=game.server().getPlayerList().getPlayer(authority.author());
        try(var scope=game.items().spellSources().scope(authority)){return Boolean.TRUE.equals(game.items().spellSources().target(author,target));}
    }
    public void targetChanged(Mob actor,LivingEntity target){if(managed(actor)&&target!=null&&!canAffect(actor,target))actor.setTarget(null);}
    public int count(){return actors.size();}
    public int cloudCount(){return clouds.size();}
    public long selections(){return selections;}
    public long emissions(){return emissions;}
    public long rewards(){return rewards;}
    List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    public void tick(){
        if(rewinding)return;callbackDepth++;try{
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        var level=game.maps().level();if(level==null||game.items().timeStop().stopped(level))return;
        phase=(phase+1)%240;
        for(var boss:List.copyOf(actors.keySet())){
            var state=actors.get(boss);var author=owner(boss);
            if(boss.isRemoved()||!game.items().currentEpoch(state.epoch())||boss.level()!=level||author==null||author.level()!=level||!game.canUseGameplayItems(author)){
                actors.remove(boss);if(!boss.isRemoved())boss.discard();continue;
            }
            if(!(boss instanceof LivingEntity living)||!living.isAlive()||boss.entityTags().contains("time_stopped"))continue;
            if(phase%40==0&&nearest(level,boss.position(),110)!=null)select(living,state,author);
        }
        for(var cloud:List.copyOf(clouds))tickCloud(cloud);

        }finally{callbackDepth--;}
    }
    private LivingEntity nearest(ServerLevel level,Vec3 point,double radius){
        LivingEntity nearest=null;double best=radius*radius;
        // Source's unbounded nearest selector visits loaded actors; never enumerate a huge rectangular chunk area.
        var visible=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());visible.addAll(game.items().lifetimes().loadedTargets());visible.addAll(level.players());
        for(var candidate:visible){
            if(candidate.level()!=level||!candidate.getType().builtInRegistryHolder().is(MOBS)||!game.items().wands().validRoleTarget(candidate,false)||EXCLUDED.stream().anyMatch(candidate.entityTags()::contains))continue;
            double distance=candidate.position().distanceToSqr(point);if(distance<=best){nearest=candidate;best=distance;}
        }
        return nearest;
    }
    private void select(LivingEntity boss,Actor state,ServerPlayer author){
        selections++;int branch=boss.getRandom().nextInt(10);var level=(ServerLevel)boss.level();var at=boss.position();
        if(branch<9)cloud(boss,author,LbcSauvojen.Kind.values()[branch],state.source());
        else{
            var center=boss.blockPosition();var task=new Terrain(author,center.offset(-15,-15,-15),center.offset(15,15,15),state.source());
            terrain.add(task);game.items().stonekivi().queuedWork(task);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,at.x,at.y,at.z,1000,10,10,10,0);
        }
        level.sendParticles(ParticleTypes.PORTAL,true,false,at.x,at.y+2,at.z,50,1,1,1,1);
        if(boss.getRandom().nextInt(100)<35)boss.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),author);
        var position=boss.position();double x=position.x,y=position.y,z=position.z;
        if(y>=300)y=40;if(y<=0)y=200;if(x>=120)x=-100;else if(x<=-120)x=100;if(z>=120)z=-100;else if(z<=-120)z=100;
        if(x!=position.x||y!=position.y||z!=position.z)boss.teleportTo(level,x,y,z,Set.of(),boss.getYRot(),boss.getXRot(),true);
        // Native /data get Health 1 truncates before the inclusive scoreboard comparison.
        if((int)boss.getHealth()<=99900)infinity(boss,author);
    }
    private void cloud(LivingEntity boss,ServerPlayer author,LbcSauvojen.Kind kind,LbcSpellSource.Source authority){
        var carrier=new Marker(EntityTypes.MARKER,author.level());carrier.setPos(boss.position());
        carrier.entityTags().addAll(List.of(CLOUD,"savoujen_cloud","magic","sauvojen_"+kind.name().toLowerCase(Locale.ROOT)));game.items().markCurrentItemEpoch(carrier);
        var drone=new LbcSauvojen.Drone(author,InteractionHand.MAIN_HAND,ItemStack.EMPTY,kind,game.items().epoch(),carrier);
        var exclusions=new HashSet<>(authority.excluded());if(kind==LbcSauvojen.Kind.TERRA||kind==LbcSauvojen.Kind.ELECTRO)exclusions.add("summoned_cultist");
        if(kind==LbcSauvojen.Kind.ELECTRO){exclusions.add("transformator");exclusions.add("cyborg_boss");}
        var source=new LbcSpellSource.Source(authority.emitter(),authority.author(),authority.epoch(),authority.cause(),exclusions);
        var cloud=new Cloud(drone,source);clouds.add(cloud);game.items().spellSources().bind(carrier,source);
        if(!author.level().addFreshEntity(carrier)){remove(cloud,false);return;}
        game.visualTracking().attach(drone.holder,author.level(),()->drone.position);game.items().sauvojen().sync(drone);
    }
    private void tickCloud(Cloud cloud){
        var d=cloud.visual();var author=d.owner;var level=(ServerLevel)d.carrier.level();
        if(d.carrier.isRemoved()||!game.items().currentEpoch(d.epoch)||level!=game.maps().level()||author.level()!=level||!game.canUseGameplayItems(author)){
            remove(cloud,false);return;
        }
        if(d.carrier.entityTags().contains("time_stopped")||!level.hasChunkAt(BlockPos.containing(d.position)))return;
        if(game.items().magic().suppressedAt(level,d.position)){remove(cloud,false);return;}
        if(phase%d.kind.cadence!=0)return;
        var target=nearest(level,d.position,30_000_000);
        boolean outerFacing=d.kind==LbcSauvojen.Kind.RUBY||d.kind==LbcSauvojen.Kind.AIR||d.kind==LbcSauvojen.Kind.STONE||d.kind==LbcSauvojen.Kind.TELEPORT;
        if(outerFacing&&target==null)return;
        d.lifetime+=d.kind.cadence;if(target==null)return;
        var direction=target.getEyePosition().subtract(d.position).normalize();if(direction.lengthSqr()>1e-12)d.direction=direction;
        game.items().sauvojen().emitHostile(author,d.kind,d.position,d.direction,cloud.source());emissions++;
        game.items().sauvojen().sync(d);
        if(d.lifetime>=200)remove(cloud,true);
    }
    private void remove(Cloud cloud,boolean effects){
        if(!clouds.remove(cloud))return;var d=cloud.visual();d.holder.destroy();
        if(effects){var level=(ServerLevel)d.carrier.level();level.sendParticles(ParticleTypes.WITCH,d.position.x,d.position.y,d.position.z,30,.1,.1,.1,.5);LbcOverload.sound(level,d.position,"minecraft:block.enchantment_table.use",2,1.3f,0,0);}
        d.carrier.discard();
    }
    /** Both the threshold and the native death-table drop use the same original reward sequence. */
    public void infinity(LivingEntity boss,ServerPlayer author){
        callbackDepth++;try{
        var at=boss.position();infinity(author,at);actors.remove(boss);
        boss.teleportTo(author.level(),0,-10000,0,Set.of(),boss.getYRot(),boss.getXRot(),true);
        // Source teleports before /kill, so its death-table token is emitted at the lower position.
        boss.hurtServer(author.level(),boss.damageSources().genericKill(),Float.MAX_VALUE);

        }finally{callbackDepth--;}
    }
    public void infinity(ServerPlayer author,Vec3 at){
        infinity(author.level(),at,author);
    }
    public void infinity(ServerLevel level,Vec3 at,ServerPlayer author){
        callbackDepth++;try{
        rewards++;game.items().infinity().produce(level,at,author);
        LbcOverload.sound(level,at,"minecraft:entity.wither.death",1,1,0,.5f);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,true,false,at.x,at.y,at.z,2000,1,1,1,.1);
        for(var cloud:List.copyOf(clouds))if(cloud.visual().carrier.level()==level&&cloud.visual().position.distanceToSqr(at)<=1600){
            var d=cloud.visual();
            dropCloudWand(level,d.position,d.kind,author);
            remove(cloud,false);
        }
        game.items().apocalypseActors().convertClouds(level,at,40,author);

        }finally{callbackDepth--;}
    }
    void dropCloudWand(ServerLevel level,Vec3 at,LbcSauvojen.Kind kind,ServerPlayer author){
        for(var reward:game.lbc().rollTable(level,at,author,wandTable(kind)))
            special(level,author,at,reward,Component.translatable(wandName(kind)).withStyle(ChatFormatting.GOLD).withStyle(s->s.withItalic(false)),999990);
    }
    private static String wandTable(LbcSauvojen.Kind kind){return switch(kind){
        case RUBY->"lbc:ported/ruby_wand";case TERRA->"lbc:ported/terra_wand";case ELECTRO->"lbc:ported/electro_wand";
        case AIR->"lbc:ported/air_wand";case ACID->"lbc:ported/acid_wand";case TELEPORT->"lbc:ported/teleport_wand";
        case STONE->"lbc:ported/stone_wand";case FIRE->"lbc:ported/fire_wand";case BLIND->"lbc:ported/blind_wand";
    };}
    private static String wandName(LbcSauvojen.Kind kind){return switch(kind){
        case RUBY->"sauvojen_wand_ruby";case TERRA->"sauvojen_wand_terra";case ELECTRO->"sauvojen_wand_electro";
        case AIR->"sauvojen_wand_air";case ACID->"sauvojen_wand_acid";case TELEPORT->"sauvojen_wand_teleport";
        case STONE->"sauvojen_wand_stone";case FIRE->"sauvojen_wand_fire";case BLIND->"sauvojen_wand_blind";
    };}
    private void special(ServerLevel level,ServerPlayer author,Vec3 at,ItemStack stack,Component name,int health){
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,item.registryAccess());item.saveWithoutId(output);
        var saved=output.buildResult();saved.putInt("Health",health);saved.putShort("Age",(short)-32768);saved.putBoolean("NoGravity",true);saved.putBoolean("Glowing",true);saved.putBoolean("CustomNameVisible",true);
        item.load(TagValueInput.create(ProblemReporter.DISCARDING,item.registryAccess(),saved));
        item.setCustomName(name);item.setUnlimitedLifetime();item.setDeltaMovement(Vec3.ZERO);if(author!=null)item.setThrower(author);
        game.items().markCurrentItemEpoch(item);game.items().timeStop().prepareFrozen(item);level.addFreshEntity(item);
    }
    private static void load(Entity entity,String nbt){try{entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),TagParser.parseCompoundFully(nbt)));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}}
    private static boolean resident(ServerLevel level,BlockPos from,BlockPos to){for(int x=from.getX()>>4;x<=to.getX()>>4;x++)for(int z=from.getZ()>>4;z<=to.getZ()>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;return true;}
    private final class Terrain implements LbcStonekivi.Work {
        final ServerLevel level;final UUID author;final String epoch=game.items().epoch();final BlockPos from,to;final LbcSpellSource.Source source;boolean cancelled;
        Terrain(ServerPlayer author,BlockPos from,BlockPos to,LbcSpellSource.Source source){level=author.level();this.author=author.getUUID();this.from=from;this.to=to;this.source=source;}
        boolean live(){return !cancelled&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch)&&level==game.maps().level();}
        @Override public boolean ready(){return !live()||!game.items().timeStop().stopped(level)&&resident(level,from,to);}
        @Override public boolean step(){
        callbackDepth++;try{if(live())try(var scope=game.items().spellSources().scope(source)){game.items().indirectKills().trapChanges(level,source.cause(),()->LbcDropBlocks.fill(level,from,to,Blocks.AIR,true));}cancel();return true;
        }finally{callbackDepth--;}
    }
        @Override public void cancel(){
        callbackDepth++;try{cancelled=true;terrain.remove(this);
        }finally{callbackDepth--;}
    }
    }
    public void loaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;
        if(!managed(entity))return;
        if(!game.items().currentEpoch(entity)||!game.playing()||entity.level()!=game.maps().level()){entity.discard();return;}
        if(!actors.containsKey(entity)){var state=unloaded.remove(entity.getUUID());if(state==null){entity.discard();return;}actors.put(entity,state);game.items().spellSources().bind(entity,state.source());}
        game.items().timeStop().prepareFrozen(entity);

        }finally{callbackDepth--;}
    }
    public void unloaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;
        if(managed(entity)&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK){var state=actors.remove(entity);if(state!=null)unloaded.put(entity.getUUID(),state);}
        if(entity.entityTags().contains(CLOUD))for(var cloud:List.copyOf(clouds))if(cloud.visual().carrier==entity)remove(cloud,false);

        }finally{callbackDepth--;}
    }
    /** Reconnect replaces only the real owner's reference, preserving the live cloud and its clocks. */
    void rebindRetained(ServerPlayer player){
        checkpointBoundary();
        for(var cloud:clouds){var visual=cloud.visual();if(visual.owner!=player&&visual.owner.getUUID().equals(player.getUUID()))visual.owner=player;}
    }
    public void forget(ServerPlayer player){
        callbackDepth++;try{
        for(var entity:List.copyOf(actors.keySet()))if(actors.get(entity).author().equals(player.getUUID())){actors.remove(entity);entity.discard();}
        unloaded.values().removeIf(state->state.author().equals(player.getUUID()));
        for(var cloud:List.copyOf(clouds))if(cloud.visual().owner==player)remove(cloud,false);
        for(var task:List.copyOf(terrain))if(task.author.equals(player.getUUID()))task.cancel();

        }finally{callbackDepth--;}
    }
    public void stop(){
        callbackDepth++;try{rewinding=false;stagedClouds=List.of();for(var entity:List.copyOf(actors.keySet()))entity.discard();actors.clear();unloaded.clear();for(var cloud:List.copyOf(clouds))remove(cloud,false);for(var task:List.copyOf(terrain))task.cancel();phase=0;
        }finally{callbackDepth--;}
    }

    boolean captureReady(){return callbackDepth==0&&terrain.isEmpty()&&!rewinding;}
    void checkpointBoundary(){if(callbackDepth!=0)throw new IllegalStateException("Finish Sauvojen boss callbacks before checkpointing");}
    void cancelFutureTerrainForRollback(){checkpointBoundary();for(var work:List.copyOf(terrain))work.cancel();}
    void prepareFrozen(){
        checkpointBoundary();cancelFutureTerrainForRollback();rewinding=true;
        for(var cloud:clouds)cloud.visual().holder.destroy();clouds.clear();stagedClouds=List.of();
    }
    void publishFrozen(){
        checkpointBoundary();
        for(var saved:stagedClouds){
            var drone=new LbcSauvojen.Drone(saved.owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,saved.kind,saved.epoch,saved.carrier);
            drone.position=saved.position;drone.direction=saved.direction;drone.lifetime=saved.lifetime;drone.nextMode=saved.nextMode;
            clouds.add(new Cloud(drone,saved.source));
            game.visualTracking().attach(drone.holder,saved.owner.level(),()->drone.position);game.items().sauvojen().sync(drone);
        }
        stagedClouds=List.of();rewinding=false;
    }
    void cancelFrozen(){checkpointBoundary();if(!stagedClouds.isEmpty())publishFrozen();else rewinding=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();if(!captureReady())throw new IllegalStateException("Complete Sauvojen boss terrain before capture");return new Checkpoint(links);}
    final class Checkpoint {
        private record Picture(MatchDamageSources.Ref owner,MatchDamageSources.Ref carrier,LbcSauvojen.Kind kind,String epoch,Vec3 position,Vec3 direction,int lifetime,int nextMode,LbcSpellSource.Source source){}
        private final MatchSpellCheckpoint.IdentityImage<Entity,Actor> registry;
        private final Map<UUID,Actor> dormant=new HashMap<>(unloaded);
        private final List<Picture> pictures=new ArrayList<>();private final int savedPhase=phase;
        private final long savedSelections=selections,savedEmissions=emissions,savedRewards=rewards;
        Checkpoint(MatchSpellCheckpoint.Links links){
            for(var entity:actors.keySet())if(links.key(entity).historical()!=null)throw new IllegalStateException("Capture live event actor bodies");
            registry=new MatchSpellCheckpoint.IdentityImage<>(actors,links);
            for(var state:actors.values()){var author=game.server().getPlayerList().getPlayer(state.author());if(author!=null)links.key(author);}
            for(var cloud:clouds){var d=cloud.visual();var carrier=links.key(d.carrier);if(carrier.historical()!=null)throw new IllegalStateException("Capture live boss cloud carriers");
                pictures.add(new Picture(links.key(d.owner),carrier,d.kind,d.epoch,d.position,d.direction,d.lifetime,d.nextMode,cloud.source()));}
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restored=registry.restore(links);var next=new ArrayList<RestoredCloud>();
            for(var p:pictures)next.add(new RestoredCloud((ServerPlayer)links.get(p.owner),(Marker)links.get(p.carrier),p.kind,p.epoch,p.position,p.direction,p.lifetime,p.nextMode,p.source));
            return ()->{actors=restored;unloaded.clear();unloaded.putAll(dormant);stagedClouds=List.copyOf(next);phase=savedPhase;selections=savedSelections;emissions=savedEmissions;rewards=savedRewards;};
        }
    }

    LbcCloneSkills.ActorImage cloneImage(Entity original) {
        var state=actors.get(original);if(state==null)return null;
        UUID id=original.getUUID();
        return CloneComplexProjectileStates.image(clone->{
            var source=CloneEncounterStates.rebase(state.source(),id,clone);
            actors.put(clone,new Actor(state.author(),state.epoch(),source));game.items().spellSources().bind(clone,source);
        },clone->{actors.remove(clone);unloaded.remove(clone.getUUID());});
    }


    void planetoid(ServerPlayer holder) {
        for(var cloud:clouds){var drone=cloud.visual();
            if(!drone.carrier.isRemoved()&&game.items().currentEpoch(drone.epoch)
                    &&!game.items().timeStop().frozen(drone.carrier)&&!drone.carrier.entityTags().contains("time_stopped")
                    &&LbcPlanetoidSkill.near(game,holder,(ServerLevel)drone.carrier.level(),drone.position)){
                drone.direction=LbcPlanetoidSkill.direction(holder,drone.position,82);
                LbcPlanetoidSkill.orient(drone.carrier,drone.direction);game.items().sauvojen().sync(drone);
            }
        }
    }

}
