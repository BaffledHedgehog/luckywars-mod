package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Hostile world encounter. Native boss ownership stays distinct from a dropped token's causal author. */
public final class LbcHelicopterEncounter {
    public static final String DISPLAY="skywars2.helicopter.encounter.display", HITBOX="helicopter_boss_hitbox";
    public static final String ATTACK="skywars2.helicopter.encounter.attack";
    private final GameController game;
    private final Set<Pair> pairs=new LinkedHashSet<>();
    private final Set<Entity> attacks=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ItemEntity> pending=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity,LivingEntity> authors=new WeakHashMap<>();
    private final Map<ServerLevel,Long> clocks=new IdentityHashMap<>();
    private final Map<ServerLevel,Integer> lastDispatch=new IdentityHashMap<>();
    private int lastPendingTick=Integer.MIN_VALUE;
    private int checkpointDepth;private boolean checkpointFrozen;private Runnable stagedPublication;
    private static final class Pair {
        Display.ItemDisplay display;
        Ravager hitbox;
        final String epoch;
        int phase=1, frame=1;
        long dispatches;
        int lastTick=Integer.MIN_VALUE;
        boolean terminal;
        Pair(Display.ItemDisplay display,Ravager hitbox,String epoch) { this.display=display;this.hitbox=hitbox;this.epoch=epoch; }
    }
    /** EntityReference forgets discarded mobs; delayed attacks retain their actual causal boss. */
    private static final class BossArrow extends Arrow {
        private Entity causal;
        BossArrow(ServerLevel level,Ravager source) {super(EntityTypes.ARROW,level);setOwner(source);}
        @Override public void setOwner(Entity owner) {
            super.setOwner(owner);
            causal=owner;
        }
        @Override public Entity getOwner() {var owner=super.getOwner();return owner==null?causal:owner;}
        @Override public boolean shouldBeSaved() {return false;}
    }
    private static final class BossTnt extends PrimedTnt {
        private Ravager source;
        final float power;
        BossTnt(ServerLevel level,Vec3 at,Ravager source,float power) {super(level,at.x,at.y,at.z,source);this.source=source;this.power=power;}
        @Override public LivingEntity getOwner() {var owner=super.getOwner();return owner==null?source:owner;}
        @Override public boolean shouldBeSaved() {return false;}
    }
    public record View(Display.ItemDisplay display,Ravager hitbox,int phase,int frame,long dispatches) { }
    LbcHelicopterEncounter(GameController game) { this.game=game; }
    private static CompoundTag tag(String snbt) {
        try { return TagParser.parseCompoundFully(snbt); }
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e) { throw new IllegalStateException(e); }
    }
    private static void load(Entity entity,CompoundTag data) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),data));
    }
    private static final CompoundTag TRIGGER=tag("{helicopter_boss:1,drop:1}");
    public static boolean isTrigger(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(TRIGGER);
    }
    boolean spawn(ItemEntity token) {
        if(checkpointFrozen)return false;checkpointDepth++;try{
        if(token.isRemoved() || token.getAge()>=5999 || !isTrigger(token.getItem()) || !(token.level() instanceof ServerLevel level)
                || level!=game.maps().level() || !game.playing() || game.stage()==GameController.Stage.ENDING || stopped(level)) return false;
        Vec3 forward=Vec3.directionFromRotation(token.getXRot(),token.getYRot());
        Vec3 up=Vec3.directionFromRotation(token.getXRot()-90,token.getYRot());
        Vec3 at=token.position().add(up.scale(20)).add(forward.scale(50));
        if(!level.hasChunkAt(BlockPos.containing(at))) return false;
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);
        load(display,tag("{view_range:10.0f,interpolation_duration:1,teleport_duration:1,item_display:'fixed',transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[3f,3f,3f]},item:{id:'minecraft:repeating_command_block',count:1,components:{'minecraft:item_model':'lbc:helicopter_1'}}}"));
        display.setPos(at); face(display,token.position().subtract(at),true);
        display.entityTags().addAll(List.of(DISPLAY,"helicopter_boss","mecha","mob"));
        var hitbox=new Ravager(EntityTypes.RAVAGER,level);
        load(hitbox,tag("{NoGravity:1b,Silent:1b,CustomNameVisible:1b,DeathLootTable:'minecraft:empty',PersistenceRequired:1b,NoAI:1b,Health:150f,CustomName:{italic:false,translate:'helicopter_boss'},active_effects:[{id:'minecraft:invisibility',amplifier:1b,duration:-1,show_particles:0b}],attributes:[{id:'minecraft:max_health',base:150d},{id:'minecraft:armor',base:10d},{id:'minecraft:armor_toughness',base:10d}]}"));
        hitbox.setPos(at);hitbox.setInvisible(true);hitbox.entityTags().add(HITBOX);
        // (user) Player-thrown drops inherit kills; natural/ownerless encounters remain ownerless.
        var author=LbcIndirectKills.playerSource(token.getOwner());
        if(author!=null){authors.put(hitbox,author);authors.put(display,author);}
        game.items().markCurrentItemEpoch(display);game.items().markCurrentItemEpoch(hitbox);
        var pair=new Pair(display,hitbox,game.items().epoch());pairs.add(pair);
        if(!level.addFreshEntity(hitbox) || !level.addFreshEntity(display)) { terminal(pair,false);return false; }
        var remaining=token.getItem().copy();remaining.shrink(1);
        if(remaining.isEmpty()) token.discard();else token.setItem(remaining);
        LbcOverload.sound(level,token.position(),"minecraft:lbcsounds.helicopter",.1f,1,20,.1f);
        LbcOverload.sound(level,token.position(),"minecraft:lbcsounds.kazahstan",1,1,20,.4f);
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xff0000,2),token.getX(),token.getY()+10,token.getZ(),1000,0,100,0,0);
        return true;

        }finally{checkpointDepth--;}
    }
    private boolean stopped(ServerLevel level) {
        // The native stop service owns dimension clocks; the legacy scoreboard remains a global gate.
        var objective=game.server().getScoreboard().getObjective("lbc.math");
        var legacy=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("time_stopped"),objective);
        return game.items().timeStop().stopped(level) || legacy!=null && legacy.value()>=1;
    }
    private boolean target(Entity entity) {
        return (entity instanceof net.minecraft.world.entity.Mob || entity instanceof net.minecraft.server.level.ServerPlayer)
                && !entity.entityTags().contains(HITBOX) && game.items().wands().validTarget(entity);
    }
    private LivingEntity nearest(Pair pair,Vec3 origin,double range) {
        var d=pair.display; LivingEntity best=null;double distance=range*range;
        // The source phase2 selector has no radius. Iterate loaded entities, never load target chunks.
        Iterable<? extends Entity> candidates=Double.isFinite(range)
                ? ((ServerLevel)d.level()).getEntities(d,new AABB(origin,origin).inflate(range),this::target)
                : ((ServerLevel)d.level()).getAllEntities();
        for(var entity:candidates) if(target(entity)) {
            double candidate=entity.position().distanceToSqr(origin);
            if(candidate<=distance) { distance=candidate;best=(LivingEntity)entity; }
        }
        return best;
    }
    void tick() {
        if(checkpointFrozen)return;checkpointDepth++;try{
        // Native /give pickup visuals become fake items (age5999) after ENTITY_LOAD.
        int serverTick=game.server().getTickCount();
        if(lastPendingTick!=serverTick) for(var token:List.copyOf(pending)) {
            if(token.isRemoved() || token.getAge()>=5999 || !isTrigger(token.getItem()) || !game.playing() || token.level()!=game.maps().level()) {pending.remove(token);continue;}
            spawn(token);
            if(token.isRemoved() || token.getItem().isEmpty()) pending.remove(token);
        }
        lastPendingTick=serverTick;
        attacks.removeIf(Entity::isRemoved);
        for(var attack:List.copyOf(attacks)) if(attack instanceof BossTnt tnt
                && !tnt.isRemoved() && game.items().currentEpoch(tnt) && !stopped((ServerLevel)tnt.level())
                && tnt.onGround() && tnt.entityTags().remove("groundbam")) {
            attacks.remove(tnt);tnt.discard();
            blast(tnt.source,(ServerLevel)tnt.level(),tnt.position(),tnt.power,true);
        }
        for(var pair:List.copyOf(pairs)) {
            var d=pair.display;var level=(ServerLevel)d.level();
            if(pair.terminal) continue;
            if(!game.items().currentEpoch(pair.epoch) || game.stage()==GameController.Stage.ENDING) { terminal(pair,false);continue; }
            if(d.isRemoved() || pair.hitbox.isRemoved()) { terminal(pair,false);continue; }
            if(!pair.hitbox.isAlive()) { terminal(pair,true);continue; }
            if(stopped(level)) continue;
            if(game.items().magic().mechanismSuppressedAt(level,d.position())) {
                terminal(pair,true);continue;
            }
            int now=game.server().getTickCount();
            if(pair.lastTick==now) continue;pair.lastTick=now;
            if(lastDispatch.getOrDefault(level,Integer.MIN_VALUE)!=now) {lastDispatch.put(level,now);clocks.merge(level,1L,Long::sum);}
            dispatch(pair,clocks.get(level)-1);
        }

        }finally{checkpointDepth--;}
    }
    private void dispatch(Pair pair,long parity) {
        var d=pair.display;var level=(ServerLevel)d.level();var random=level.getRandom();pair.dispatches++;
        Vec3 origin=d.position();float contextYaw=d.getYRot(),contextPitch=d.getXRot();
        if(!level.hasChunkAt(d.blockPosition())) {terminal(pair,false);return;}
        if(!level.getBlockState(d.blockPosition()).isAir()) tnt(pair,d.position(),0,4,false);
        if(pair.terminal || d.isRemoved() || !game.items().currentEpoch(pair.epoch)) return;
        if(pair.phase==1) {
            d.setPos(d.position().add(d.getLookAngle()));
            if(nearest(pair,origin,50)!=null && random.nextFloat()<.01f) pair.phase=3;
            var victim=nearest(pair,origin,100);
            if(victim!=null) shoot(pair,origin,victim.getEyePosition().subtract(origin).normalize(),parity);
            else pair.phase=2;
        }
        if(pair.phase==2) {
            var victim=nearest(pair,origin,Double.POSITIVE_INFINITY);
            if(victim!=null) {
                Vec3 blended=victim.getEyePosition().subtract(origin).normalize().scale(.5).add(d.getLookAngle());
                face(d,blended,true);d.setPos(origin.add(d.getLookAngle()));
            }
            if(random.nextFloat()<.4f) {
                var h=pair.hitbox;boolean below=false;
                for(var entity:level.getEntities(h,new AABB(h.getX()-4,h.getY()-10000,h.getZ()-4,h.getX()+4,h.getY(),h.getZ()+4),this::target))
                    if(entity.position().distanceToSqr(h.position())<=150*150) {below=true;break;}
                if(below) game.items().kfcRewards().spawnLooseBomb(h,h.position().add(0,-3,0),random.nextInt(6)+1);
            }
            if(random.nextFloat()<.005f) pair.phase=1;
        }
        if(pair.phase==3) {
            d.setPos(origin);d.setYRot(contextYaw+10);d.setXRot(contextPitch);
            var at=origin;at=new Vec3(Mth.floor(at.x)+random.nextInt(15)-7,at.y,Mth.floor(at.z)+random.nextInt(15)-7);
            tnt(pair,at,1000,1,true);
            if(random.nextFloat()<.1f) LbcOverload.sound(level,origin,"minecraft:lbcsounds.drop_bomb",.5f,1,150,.5f);
            if(random.nextFloat()<.01f) pair.phase=1;
        }
        if(pair.terminal || d.isRemoved()) return;
        pair.frame=pair.frame%4+1;
        var frame=new ItemStack(net.minecraft.world.item.Items.REPEATING_COMMAND_BLOCK);
        frame.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:helicopter_"+pair.frame));d.setItemStack(frame);
        pair.hitbox.setPos(d.position().add(0,-3,0));pair.hitbox.setDeltaMovement(Vec3.ZERO);
        if(pair.hitbox.hurtTime==10) {level.sendParticles(new net.minecraft.core.particles.DustColorTransitionOptions(0xffe01f,0x7d701c,4),true,false,d.getX(),d.getY(),d.getZ(),50,.5,.5,.5,1);LbcOverload.sound(level,origin,"minecraft:entity.blaze.hurt",1,1.5f,150,1);}
        d.needsSync=true;pair.hitbox.needsSync=true;
    }
    private static void face(Entity entity,Vec3 direction,boolean horizontal) {
        entity.setYRot((float)(Math.toDegrees(Math.atan2(direction.z,direction.x))-90));
        entity.setXRot(horizontal?0:(float)-Math.toDegrees(Math.atan2(direction.y,Math.sqrt(direction.horizontalDistanceSqr()))));
    }
    private void shoot(Pair pair,Vec3 origin,Vec3 forward,long parity) {
        var d=pair.display;var level=(ServerLevel)d.level();var arrow=new BossArrow(level,pair.hitbox);
        float yaw=(float)(Math.toDegrees(Math.atan2(forward.z,forward.x))-90);
        Vec3 left=new Vec3(Math.cos(Math.toRadians(yaw)),0,Math.sin(Math.toRadians(yaw)));
        arrow.setOwner(pair.hitbox);arrow.pickup=AbstractArrow.Pickup.CREATIVE_ONLY;arrow.setBaseDamage(1);
        arrow.setPos(origin.add(forward.scale(8.5)).add(left.scale(parity%2==0?1.5:-1.5)));
        // Native launch initializes both current and previous rotation before the spawn packet.
        arrow.shoot(forward.x,forward.y,forward.z,6,0);
        arrow.entityTags().add("killedarrow");track(arrow);level.addFreshEntity(arrow);
        var shotOrigin=origin.add(forward.scale(8));
        shotSound(level,shotOrigin,shotOrigin.add(left.scale(parity%2==0?2:-2)),parity%2==0?2:1.8f);
    }
    private void track(Entity entity) {entity.entityTags().add(ATTACK);game.items().markCurrentItemEpoch(entity);attacks.add(entity);game.items().timeStop().prepareFrozen(entity);}
    private void tnt(Pair pair,Vec3 at,int fuse,int power,boolean ground) {
        var level=(ServerLevel)pair.display.level();if(!level.hasChunkAt(BlockPos.containing(at))) return;
        if(fuse==0) {blast(pair.hitbox,level,at,power,false);return;}
        var bomb=new BossTnt(level,at,pair.hitbox,power);
        var data=new CompoundTag();data.putFloat("explosion_power",power);
        data.put("owner",net.minecraft.core.UUIDUtil.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,pair.hitbox.getUUID()).getOrThrow());load(bomb,data);
        bomb.setPos(at);bomb.setFuse(fuse);
        if(ground) bomb.entityTags().add("groundbam");track(bomb);level.addFreshEntity(bomb);
    }
    private void blast(LivingEntity owner,ServerLevel level,Vec3 at,float power,boolean protectedDamage) {
        game.items().exasperato().protectedBlast(owner,level,at,power,source->{
            source.entityTags().add(ATTACK);
            if(!protectedDamage) source.entityTags().add(LbcDamage.UNPROTECTED_TNT);
        });
    }
    /** Called before the beacon's generic kill, so suppression has an explicit terminal cause. */
    boolean mechanismRemoved(Entity entity) {
        if(checkpointFrozen)return false;checkpointDepth++;try{
        for(var pair:List.copyOf(pairs)) if(pair.display==entity || pair.hitbox==entity) {
            terminal(pair,true);
            return true;
        }
        return false;

        }finally{checkpointDepth--;}
    }
    public void died(LivingEntity entity,DamageSource source) {
        if(checkpointFrozen)return;checkpointDepth++;try{
        for(var pair:List.copyOf(pairs)) if(pair.hitbox==entity) terminal(pair,true);

        }finally{checkpointDepth--;}
    }
    private void terminal(Pair pair,boolean reward) {
        checkpointDepth++;try{
        if(pair.terminal) return;pair.terminal=true;pairs.remove(pair);
        var level=(ServerLevel)pair.display.level();var at=pair.display.position();
        // Detach state before callbacks; ordinary discard/unload can never award a second reward.
        pair.display.discard();
        if(reward && game.items().currentEpoch(pair.epoch) && game.stage()!=GameController.Stage.ENDING) {
            for(String id:List.of("lbcsounds.helicopter","lbcsounds.kazahstan"))
                for(var player:game.server().getPlayerList().getPlayers()) player.connection.send(new ClientboundStopSoundPacket(Identifier.withDefaultNamespace(id),null));
            int[][] yz={{0,0},{1,0},{0,1},{-1,0},{0,-1},{1,0},{1,0},{1,1},{-1,0},{1,-1}};
            for(var offset:yz) for(int x:new int[]{0,1,-1}) tnt(pair,at.add(x,offset[0],offset[1]),1000,4,true);
            for(var stack:game.lbc().rollTable(level,at,pair.hitbox,"lbc:ported/helicopter_boss")) if(!stack.isEmpty()) {
                var drop=new ItemEntity(level,at.x,at.y,at.z,stack);
                // Loading an ItemEntity without its Item discards it; the native setter preserves this exact reward.
                drop.setUnlimitedLifetime();
                drop.setPermanentlyInvulnerable(true);drop.setNoGravity(true);drop.setGlowingTag(true);game.items().markCurrentItemEpoch(drop);level.addFreshEntity(drop);
            }
        }
        if(!pair.hitbox.isRemoved()) pair.hitbox.discard();

        }finally{checkpointDepth--;}
    }
    void entityUnloaded(Entity entity) {
        checkpointDepth++;try{if(checkpointFrozen||MatchActorCheckpoint.restoring(entity))return;
        pending.remove(entity);
        attacks.remove(entity);
        for(var pair:List.copyOf(pairs)) if(pair.display==entity || pair.hitbox==entity) terminal(pair,false);

        }finally{checkpointDepth--;}
    }
    void loaded(Entity entity) {
        checkpointDepth++;try{if(checkpointFrozen||MatchActorCheckpoint.restoring(entity))return;
        if(entity instanceof ItemEntity token && isTrigger(token.getItem()) && game.playing() && token.level()==game.maps().level()) pending.add(token);
        if((entity.entityTags().contains(DISPLAY) || entity.entityTags().contains(HITBOX))
                && pairs.stream().noneMatch(pair->pair.display==entity || pair.hitbox==entity)) entity.discard();
        if(entity.entityTags().contains(ATTACK) && !attacks.contains(entity)) entity.discard();

        }finally{checkpointDepth--;}
    }
    void stop() {
        checkpointDepth++;try{checkpointFrozen=false;stagedPublication=null;
        pending.clear();
        for(var pair:List.copyOf(pairs)) terminal(pair,false);
        for(String id:List.of("lbcsounds.helicopter","lbcsounds.kazahstan"))
            for(var player:game.server().getPlayerList().getPlayers()) player.connection.send(new ClientboundStopSoundPacket(Identifier.withDefaultNamespace(id),null));
        var detached=List.copyOf(attacks);attacks.clear();for(var entity:detached) entity.discard();authors.clear();clocks.clear();lastDispatch.clear();lastPendingTick=Integer.MIN_VALUE;

        }finally{checkpointDepth--;}
    }
    private boolean damageTarget(Entity target) {return !target.entityTags().contains(HITBOX) && game.items().wands().validTarget(target);}
    public boolean canHit(Projectile projectile,Entity target) {return !projectile.entityTags().contains(ATTACK) || damageTarget(target);}
    public boolean explosionTarget(Entity source,Entity target) {
        boolean boss=source!=null && (source.entityTags().contains(ATTACK)
                || source instanceof PrimedTnt tnt && tnt.getOwner()!=null && tnt.getOwner().entityTags().contains(HITBOX));
        return !boss || damageTarget(target);
    }
    public boolean allowDamage(LivingEntity victim,DamageSource source) {
        // Native lightning stays WILD even when a boss is its causal source.
        if(LbcDamage.isNativeLightning(source)) return true;
        var direct=source.getDirectEntity();var owner=source.getEntity();
        return !(direct!=null && direct.entityTags().contains(ATTACK) || owner!=null && owner.entityTags().contains(HITBOX)) || damageTarget(victim);
    }
    public LivingEntity owner(Entity entity) { return authors.get(entity); }
    boolean managed(Entity entity) {return pairs.stream().anyMatch(pair->pair.display==entity || pair.hitbox==entity);}
    int activeCount() {return pairs.size();}
    List<View> snapshot() {return pairs.stream().map(p->new View(p.display,p.hitbox,p.phase,p.frame,p.dispatches)).toList();}
    List<Entity> attackSnapshot() {return List.copyOf(attacks);}
    private static void shotSound(ServerLevel level,Vec3 selectorOrigin,Vec3 at,float pitch) {
        // The selector is centered at the command context, independently of the local sound offset.
        var sound=net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST);
        double range=sound.value().getRange(1);long seed=level.getRandom().nextLong();
        for(var viewer:level.players()) {
            if(viewer.position().distanceToSqr(selectorOrigin)>80*80) continue;
            boolean distant=viewer.position().distanceToSqr(at)>range*range;
            Vec3 heardAt=distant?viewer.position().add(at.subtract(viewer.position()).normalize().scale(2)):at;
            viewer.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound,SoundSource.MASTER,
                    heardAt.x,heardAt.y,heardAt.z,distant?.5f:1,pitch,seed));
        }
    }

    boolean captureReady(){return checkpointDepth==0&&!checkpointFrozen;}
    void checkpointBoundary(){if(checkpointDepth!=0)throw new IllegalStateException("Finish Helicopter native callbacks before checkpointing");}
    void prepareFrozen(){checkpointBoundary();checkpointFrozen=true;stagedPublication=null;pairs.clear();attacks.clear();pending.clear();authors.clear();clocks.clear();lastDispatch.clear();}
    void publishFrozen(){checkpointBoundary();if(stagedPublication!=null)stagedPublication.run();stagedPublication=null;checkpointFrozen=false;}
    void cancelFrozen(){checkpointBoundary();stagedPublication=null;checkpointFrozen=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();if(!captureReady())throw new IllegalStateException("Capture Helicopter outside native construction");return new Checkpoint(links);}
    final class Checkpoint {
        private record Value(Pair original,MatchDamageSources.Ref display,MatchDamageSources.Ref hitbox,int phase,int frame,long dispatches,int lastTick,boolean terminal){}
        private record Author(MatchDamageSources.Ref actor,MatchDamageSources.Ref author){}
        private record Attack(MatchDamageSources.Ref actor,MatchDamageSources.Ref source){}
        private final List<Value> values=new ArrayList<>();private final List<Author> attribution=new ArrayList<>();private final List<Attack> shots=new ArrayList<>();
        private final List<MatchDamageSources.Ref> tokens=new ArrayList<>();
        private final Map<ServerLevel,Long> worldClocks=new IdentityHashMap<>(clocks);
        private final Map<ServerLevel,Integer> dispatchFrames=new IdentityHashMap<>(lastDispatch);
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastPendingTick,0);
        Checkpoint(MatchSpellCheckpoint.Links links){
            for(var pair:pairs){var display=links.key(pair.display);var hitbox=links.key(pair.hitbox);if(display.historical()!=null||hitbox.historical()!=null)throw new IllegalStateException("Capture current helicopter pair bodies");
                values.add(new Value(pair,display,hitbox,pair.phase,pair.frame,pair.dispatches,pair.lastTick,pair.terminal));}
            authors.forEach((actor,author)->attribution.add(new Author(links.key(actor),links.key(author))));
            for(var attack:attacks){var source=attack instanceof BossTnt tnt?tnt.source:attack instanceof BossArrow arrow?arrow.causal:null;shots.add(new Attack(links.key(attack),links.key(source)));}
            for(var token:pending)tokens.add(links.key(token));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();for(var world:worldClocks.keySet())MatchAuthorityCheckpoint.world(game,world);for(var world:dispatchFrames.keySet())MatchAuthorityCheckpoint.world(game,world);
            var nextDisplays=new IdentityHashMap<Pair,Display.ItemDisplay>();var nextHitboxes=new IdentityHashMap<Pair,Ravager>();
            for(var saved:values){var display=(Display.ItemDisplay)links.get(saved.display);var hitbox=(Ravager)links.get(saved.hitbox);if(display.level()!=hitbox.level())throw new IllegalStateException("Restore helicopter pair in its source world");nextDisplays.put(saved.original,display);nextHitboxes.put(saved.original,hitbox);}
            var nextAuthors=new ArrayList<Map.Entry<Entity,LivingEntity>>();for(var saved:attribution)nextAuthors.add(Map.entry(links.get(saved.actor),(LivingEntity)links.get(saved.author)));
            var nextAttacks=new ArrayList<Entity>();var nextSources=new IdentityHashMap<Entity,Entity>();for(var saved:shots){var actor=links.get(saved.actor);var source=links.get(saved.source);if(actor instanceof BossTnt&&!(source instanceof Ravager))throw new IllegalStateException("Restore native helicopter TNT source");nextAttacks.add(actor);nextSources.put(actor,source);}
            var nextTokens=new ArrayList<ItemEntity>();for(var token:tokens)nextTokens.add((ItemEntity)links.get(token));
            return ()->{
                pairs.clear();for(var saved:values){var pair=saved.original;pair.display=nextDisplays.get(pair);pair.hitbox=nextHitboxes.get(pair);pair.phase=saved.phase;pair.frame=saved.frame;pair.dispatches=saved.dispatches;pair.terminal=saved.terminal;pairs.add(pair);}
                attacks.clear();attacks.addAll(nextAttacks);for(var actor:nextAttacks){var source=nextSources.get(actor);if(actor instanceof BossTnt tnt)tnt.source=(Ravager)source;else if(actor instanceof BossArrow arrow)arrow.causal=source;}
                pending.clear();pending.addAll(nextTokens);authors.clear();for(var saved:nextAuthors)authors.put(saved.getKey(),saved.getValue());clocks.clear();clocks.putAll(worldClocks);
                stagedPublication=()->{lastPendingTick=clock.last(game);lastDispatch.clear();dispatchFrames.forEach((world,frame)->lastDispatch.put(world,frame==Integer.MIN_VALUE?frame:clock.time(frame,game)));
                    for(var saved:values)saved.original.lastTick=saved.lastTick==Integer.MIN_VALUE?saved.lastTick:clock.time(saved.lastTick,game);};
            };
        }
    }

    static boolean cloneRecognizes(Entity e){return e.entityTags().contains(DISPLAY)||e.entityTags().contains(HITBOX)
            ||e.entityTags().contains(ATTACK)||e instanceof BossArrow||e instanceof BossTnt;}
    static boolean cloneProtected(Entity e){return e.entityTags().contains(DISPLAY);}
    LbcCloneSkills.ActorImage cloneImage(Entity original) {
        var pair=pairs.stream().filter(p->p.hitbox==original&&!p.terminal).findFirst().orElse(null);
        if(pair!=null){
            var visual=new CloneEncounterStates.Presentation(pair.display);var at=original.position();
            var epoch=pair.epoch;int phase=pair.phase,frame=pair.frame,lastTick=pair.lastTick;long dispatches=pair.dispatches;
            var bodyAuthor=authors.get(original);var visualAuthor=authors.get(pair.display);
            return CloneComplexProjectileStates.image(clone->{
                var level=(ServerLevel)clone.level();var display=(Display.ItemDisplay)visual.create(level);
                display.setPos(visual.position.add(clone.position().subtract(at)));
                var copy=new Pair(display,(Ravager)clone,epoch);copy.phase=phase;copy.frame=frame;
                copy.dispatches=dispatches;copy.lastTick=lastTick;pairs.add(copy);
                if(bodyAuthor!=null)authors.put(clone,bodyAuthor);if(visualAuthor!=null)authors.put(display,visualAuthor);
                game.items().markCurrentItemEpoch(display);game.items().timeStop().prepareFrozen(display);
                if(!level.addFreshEntity(display)||display.isRemoved())throw new IllegalStateException("Helicopter clone visual rejected");
            },clone->{
                for(var copy:List.copyOf(pairs))if(copy.hitbox==clone){pairs.remove(copy);copy.terminal=true;
                    authors.remove(copy.display);copy.display.discard();}
                authors.remove(clone);
            });
        }
        if(!attacks.contains(original))return null;
        var author=authors.get(original);
        java.util.function.Function<ServerLevel,Entity> factory=null;
        if(original instanceof BossArrow arrow)factory=level->new BossArrow(level,null);
        if(original instanceof BossTnt tnt){var source=tnt.source;float power=tnt.power;
            factory=level->new BossTnt(level,Vec3.ZERO,source,power);}
        return CloneComplexProjectileStates.image(factory,clone->{attacks.add(clone);if(author!=null)authors.put(clone,author);},
                clone->{attacks.remove(clone);authors.remove(clone);});
    }

}
