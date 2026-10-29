package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Native random entity launcher; only source carrier/marker work is simulated here. */
public final class LbcMobgun {
    public static final String ACTOR="skywars2.mobgun.actor";
    public static final Item ITEM=registerItem();
    private static final CompoundTag FLAG=flag();
    // User decision: preserve the actual source timer, with corrected 2.5-second lore.
    private static final int COOLDOWN=50;
    private final GameController game;
    private final Map<UUID,Integer> cooldowns=new HashMap<>();
    private final Map<Entity,Owned> actors=new IdentityHashMap<>();
    private CompletableFuture<Prepared> preparation;
    private Prepared prepared;
    private record Preset(int outcome,double offset,CompoundTag data) { }
    private record Prepared(Map<Integer,Preset> outcomes,Map<Integer,Preset> potions) { }
    private static final class Owned {
        ServerPlayer owner; final String epoch; int age;
        Owned(ServerPlayer owner,String epoch) { this.owner=owner; this.epoch=epoch; }
    }
    LbcMobgun(GameController game) { this.game=game; }
    public static void register() { }
    private static CompoundTag flag() { var tag=new CompoundTag(); tag.putInt("mobgun",1); return tag; }
    private static Item registerItem() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:mobgun"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:mobgun"); }
        }.setId(key).stacksTo(1).overrideDescription("mob_gun"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isMobgun(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(ITEM) || data!=null && data.matchedBy(FLAG);
    }
    /** IO, parsing and DFU run on the existing map preparation worker before gameplay. */
    public void prepare() {
        prepared=null;
        var fixer=game.server().getFixerUpper();
        preparation=game.maps().computeFromSnapshot(()-> {
            try(var input=LbcMobgun.class.getResourceAsStream("/skywars2/mobgun_presets.json")) {
                if(input==null) throw new IllegalStateException("Missing Mobgun presets");
                var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
                int version=json.get("originalDataVersion").getAsInt();
                var outcomes=new HashMap<Integer,Preset>(); var potions=new HashMap<Integer,Preset>();
                for(String group:List.of("outcomes","potions")) for(var raw:json.getAsJsonArray(group)) {
                    var entry=raw.getAsJsonObject(); int outcome=entry.get("outcome").getAsInt();
                    var tag=TagParser.parseCompoundFully(entry.get("nbt").getAsString());
                    // Five post-1.21.8 source types only contain native id/Tags;
                    // feeding their IDs into the older schema produces DFU errors.
                    if(!entry.has("nativeFormat") || !entry.get("nativeFormat").getAsBoolean())
                        tag=(CompoundTag)fixer.update(References.ENTITY,new Dynamic<>(NbtOps.INSTANCE,tag),version,
                                SharedConstants.getCurrentVersion().dataVersion().version()).getValue();
                    var preset=new Preset(outcome,entry.has("offset")?entry.get("offset").getAsDouble():1,tag);
                    if(group.equals("outcomes")) outcomes.put(outcome,preset);
                    else if(potions.put(outcome,preset)!=null) throw new IllegalStateException("Duplicate potion entry");
                }
                if(outcomes.size()!=113 || potions.size()!=89) throw new IllegalStateException("Incomplete Mobgun catalog");
                return new Prepared(Map.copyOf(outcomes),Map.copyOf(potions));
            } catch(Exception error) { throw new IllegalStateException("Invalid Mobgun presets",error); }
        });
    }
    public boolean ready() {
        if(prepared==null && preparation!=null && preparation.isDone()) {
            try { prepared=preparation.join(); }
            catch(java.util.concurrent.CompletionException error) {
                me.skywars2.SkyWars2.LOGGER.error("Mobgun preparation failed",error); preparation=null;
            }
        }
        return prepared!=null;
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && owner.isAlive() && !owner.isRemoved() && owner.level()==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean valid(Owned state) { return valid(state.owner) && game.items().currentEpoch(state.epoch); }
    public boolean use(ServerPlayer owner,InteractionHand hand) {
        if(!isMobgun(owner.getItemInHand(hand)) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !valid(owner) || game.items().actionsBlocked(owner) || game.items().wands().inputTimeStopped(owner)
                || game.items().magic().suppressed(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())
                || cooldown(owner)>0 || !ready()) return false;
        int outcome=owner.getRandom().nextInt(114)+1;
        var luck=game.items().mana().score(ScoreHolder.forNameOnly("final_luck_modifier"),"lbc.math");
        if(luck.get()==7) outcome=22;
        boolean fired=launch(owner,outcome,0);
        if(!fired) return false;
        cooldowns.put(owner.getUUID(),COOLDOWN);
        LbcOverload.sound(owner,owner.position(),"minecraft:block.piston.extend",1,2,0);
        if(outcome==22) game.items().mana().award(owner,"lbc:true_advancements/shooter/mob_gun");
        return true;
    }
    /** Deterministic catalog entry point for server verification; normal input chooses uniformly. */
    boolean launch(ServerPlayer owner,int outcome,int potionOutcome) {
        if(!ready() || !valid(owner) || outcome<1 || outcome>114) return false;
        // All 89 unique source item/effect combinations get equal probability. A
        // single corrected native splash/lingering entity is launched per shot.
        var selected=outcome==75?prepared.potions.get(potionOutcome==0?owner.getRandom().nextInt(89)+1:potionOutcome)
                :prepared.outcomes.get(outcome);
        if(selected==null) return false;
        var choices=List.of(selected);
        boolean result=false;
        for(var preset:choices) {
            Vec3 position=owner.getEyePosition().add(owner.getLookAngle().normalize().scale(preset.offset));
            if(!owner.level().hasChunkAt(BlockPos.containing(position))) continue;
            var tag=preset.data.copy();
            if(outcome==98) tag.put("owner",net.minecraft.core.UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE,owner.getUUID()).getOrThrow());
            var input=TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),tag);
            Entity root;
            if(outcome==98) {
                root=new PrimedTnt(owner.level(),position.x,position.y,position.z,owner); root.load(input);
                root.setPos(position); own(root,owner);
            } else root=EntityType.loadEntityRecursive(input,owner.level(),EntitySpawnReason.TRIGGERED,entity->{
                entity.setPos(position); own(entity,owner); return entity;
            });
            if(root==null) continue;
            root.setDeltaMovement(owner.getLookAngle().normalize().scale(3)); root.needsSync=true;
            if(owner.level().tryAddFreshEntityWithPassengers(root)) result=true;
            else for(var entity:root.getSelfAndPassengers().toList()) { actors.remove(entity); dispose(entity); }
        }
        return result;
    }
    private void own(Entity entity,ServerPlayer owner) {
        if(entity instanceof Player) return;
        actors.put(entity,new Owned(owner,game.items().epoch())); entity.entityTags().add(ACTOR);
        entity.entityTags().remove("prestartf"); game.items().markCurrentItemEpoch(entity);
        if(entity instanceof Projectile projectile) projectile.setOwner(owner);
        if(entity instanceof EvokerFangs fangs) fangs.setOwner(owner);
        if(entity instanceof AreaEffectCloud cloud) cloud.setOwner(owner);
        if(entity instanceof LightningBolt lightning) lightning.setCause(owner);
    }
    /** Called at native source-aware child creation, before adding the child to its level. */
    public void registerChild(Entity parent,Entity child) {
        var owner=owner(parent); if(owner!=null) registerChild(owner,child);
    }
    /** Shared native actor ownership also covers crystals produced by Prism. */
    void registerChild(ServerPlayer fallback,Entity child) {
        child.getSelfAndPassengers().forEach(entity->{
            // A later attacker may already own native chained TNT or a projectile.
            Entity nativeOwner=nativeOwner(entity);
            var authority=nativeOwner instanceof ServerPlayer player ? player : owner(nativeOwner);
            own(entity,authority!=null?authority:fallback);
        });
    }
    private record NativeParent(Entity source,ServerPlayer owner,String epoch,long generation) { }
    private final transient ThreadLocal<NativeParent> nativeParent=new ThreadLocal<>();
    private long nativeGeneration;
    /** Snapshot authority before native removal can unregister the source during its own explosion. */
    public <T> T withParent(Entity source,java.util.function.Supplier<T> action) {
        if(!isManaged(source)) return action.get();
        NativeParent previous=nativeParent.get();
        var authority=owner(source);
        nativeParent.set(new NativeParent(source,authority,game.items().epoch(),nativeGeneration));
        try { return action.get(); }
        finally { if(previous==null) nativeParent.remove(); else nativeParent.set(previous); }
    }
    /** Called synchronously before other entity-load handlers inspect a newly created child. */
    public void claimChild(Entity child) {
        NativeParent parent=nativeParent.get();
        if(parent==null || parent.source==child || child instanceof Player) return;
        if(parent.generation!=nativeGeneration || !game.items().currentEpoch(parent.epoch) || !valid(parent.owner)) {
            dispose(child);
            return;
        }
        registerChild(parent.owner,child);
    }
    /** Lifecycle disposal must not split a cube already in its native death animation. */
    public void dispose(Entity entity) {
        if(entity instanceof net.minecraft.world.entity.monster.cubemob.AbstractCubeMob)
            entity.setRemoved(Entity.RemovalReason.DISCARDED);
        else entity.discard();
    }
    public boolean isManaged(Entity entity) { return actors.containsKey(entity); }
    public ServerPlayer owner(Entity entity) {
        if(entity==null || entity instanceof Player) return null;
        var state=actors.get(entity);
        Entity parent=nativeOwner(entity);
        if(state==null) {
            NativeParent scope=nativeParent.get();
            if(scope!=null && scope.source==entity && scope.generation==nativeGeneration
                    && game.items().currentEpoch(scope.epoch) && valid(scope.owner))
                return parent instanceof ServerPlayer player && valid(player) ? player : scope.owner;
            // Ordinary native actors/player-owned projectiles do not belong to
            // this service; avoid allocating a traversal guard on every query.
            if(parent==null || parent instanceof Player) return null;
        }
        if(state!=null && (parent==null || parent instanceof ServerPlayer)) {
            if(parent instanceof ServerPlayer player) state.owner=player;
            return valid(state)?state.owner:null;
        }
        return resolveOwner(entity,Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private Entity nativeOwner(Entity entity) {
        var reflected=game.items().electro().reflectedOwner(entity);
        if(reflected!=null) return reflected;
        if(entity instanceof PrimedTnt tnt) return tnt.getOwner();
        if(entity instanceof LightningBolt bolt) return bolt.getCause();
        if(entity instanceof Projectile projectile) return projectile.getOwner();
        if(entity instanceof EvokerFangs fangs) return fangs.getOwner();
        if(entity instanceof AreaEffectCloud cloud) return cloud.getOwner();
        return null;
    }
    /** Native reflection changes authority before cleanup or child attribution reads it. */
    private ServerPlayer resolveOwner(Entity entity,Set<Entity> visiting) {
        if(entity==null || entity instanceof Player || !visiting.add(entity)) return null;
        var state=actors.get(entity);
        Entity parent=nativeOwner(entity);
        if(parent!=null && parent!=entity) {
            // Direct player ownership is authoritative only for an actor already
            // belonging to Mobgun; ordinary player projectiles stay unmanaged.
            if(state!=null && parent instanceof ServerPlayer player) state.owner=player;
            else {
                var inherited=resolveOwner(parent,visiting);
                if(inherited!=null) {
                    if(state==null) return inherited;
                    state.owner=inherited;
                }
            }
        }
        return state!=null && valid(state)?state.owner:null;
    }
    public void tick() {
        ready();
        if(!game.items().wands().timeStopped()) cooldowns.replaceAll((key,value)->Math.max(0,value-1));
        cooldowns.values().removeIf(value->value<=0);
        for(var entry:List.copyOf(actors.entrySet())) {
            Entity entity=entry.getKey(); Owned state=entry.getValue();
            owner(entity);
            if(entity.isRemoved() || !valid(state)) { actors.remove(entity); if(!entity.isRemoved()) dispose(entity); continue; }
            if(game.items().wands().timeStopped() || entity.entityTags().contains("time_stopped")) continue;
            state.age++;
            if(entity.entityTags().contains("killwhenonground") && state.age%20==0 && entity.onGround()) {
                entity.ejectPassengers(); actors.remove(entity); dispose(entity); continue;
            }
            if(entity.entityTags().contains("evoker_fang_spawner")) {
                if(state.age>=600) { actors.remove(entity); dispose(entity); continue; }
                if(state.age%5==0) {
                    var fangs=new EvokerFangs(state.owner.level(),entity.getX(),entity.getY()-.25,entity.getZ(),0,0,state.owner);
                    own(fangs,state.owner); if(!state.owner.level().addFreshEntity(fangs)) { actors.remove(fangs); dispose(fangs); }
                }
            }
        }
    }
    public int cooldown(ServerPlayer owner) { return cooldowns.getOrDefault(owner.getUUID(),0); }
    void sourceCooldown(ServerPlayer owner,int value) {
        if(value<=0)cooldowns.remove(owner.getUUID());else cooldowns.put(owner.getUUID(),value);
    }
    public void clearCooldown(ServerPlayer owner) { cooldowns.remove(owner.getUUID()); }
    public void addCounters(ServerPlayer owner) { cooldowns.merge(owner.getUUID(),1,Integer::sum); }
    public List<Entity> snapshot(ServerPlayer owner) {
        for(var entity:List.copyOf(actors.keySet())) owner(entity);
        return actors.entrySet().stream().filter(e->e.getValue().owner==owner).map(Map.Entry::getKey).toList();
    }
    public int count() { return actors.size(); }
    public void loaded(Entity entity) {
        if(entity instanceof Player) return;
        if(entity.entityTags().contains(ACTOR)) {
            if(!actors.containsKey(entity)) dispose(entity);
            return;
        }
        Entity parent=null;
        if(entity instanceof Projectile projectile) parent=projectile.getOwner();
        else if(entity instanceof EvokerFangs fangs) parent=fangs.getOwner();
        else if(entity instanceof AreaEffectCloud cloud) parent=cloud.getOwner();
        if(parent!=null) registerChild(parent,entity);
    }
    public void unloaded(Entity entity) { if(actors.remove(entity)!=null && !entity.isRemoved()) dispose(entity); }
    public void forget(ServerPlayer owner) {
        cooldowns.remove(owner.getUUID());
        for(var entity:snapshot(owner)) { actors.remove(entity); if(!entity.isRemoved()) dispose(entity); }
    }
    public void stop() {
        nativeGeneration++;
        cooldowns.clear(); var entities=List.copyOf(actors.keySet()); actors.clear();
        for(var entity:entities) if(!entity.isRemoved()) dispose(entity);
    }
}
