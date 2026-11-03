package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native reward pigs; token ordering and the one-unit debit belong to LbcSpecialDrops. */
public final class LbcLuckyPigs {
    private static final String PIG = "skywars2.lucky_pig";
    private static final String FAKE = "skywars2.fake_lucky_pig", FAKE_BOLT="skywars2.fake_lucky_pig.bolt";
    private static final double[][] OFFSETS = {{1,0}, {0,1}, {-1,0}, {0,-1}};
    private final GameController game;
    private final Set<Pig> pigs = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ZombifiedPiglin> converted = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, LivingEntity> causes = new IdentityHashMap<>();
    // Same-match chunk sleep retains the cause without retaining the unloaded actor instance.
    private final Map<UUID, LivingEntity> unloadedCauses = new HashMap<>();
    private final Set<LightningBolt> fakeBolts=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerLevel,Integer> fakeTimers=new IdentityHashMap<>();
    private record FakeSite(net.minecraft.world.phys.Vec3 at,LbcSpellSource.Source source){}
    private final Map<ServerLevel,List<FakeSite>> fakeSites=new IdentityHashMap<>();

    public LbcLuckyPigs(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcLuckyPigDropItem.matches(token.getItem())
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        // Admit all four offsets together without synchronously loading neighbouring chunks.
        for (var offset : OFFSETS) {
            var pos = BlockPos.containing(token.position().add(offset[0], 0, offset[1]));
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        }
        return true;
    }

    public boolean spawn(ItemEntity token) {
        if (!ready(token)) return false;
        var level = (ServerLevel) token.level();
        var cause = game.items().specialDrops().owner(token);
        for (var offset : OFFSETS) {
            // /summon omits finalizeSpawn: leave ordinary health, AI and breeding untouched.
            var pig = new Pig(EntityTypes.PIG, level);
            var preset = new CompoundTag();
            preset.putString("DeathLootTable", "lbc:applepig");
            pig.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), preset));
            pig.setCustomName(Component.literal("Lucky Pig")
                    .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)));
            pig.setCustomNameVisible(true);
            pig.setPos(token.position().add(offset[0], 0, offset[1]));
            pig.setYRot(0);
            pig.setXRot(0);
            pig.setYHeadRot(0);
            pig.yBodyRot = 0;
            prepare(pig, cause);
            if (!level.addFreshEntity(pig)) {
                forget(pig);
                pig.discard();
            }
        }
        return true;
    }

    void adoptEgg(Pig actor, LivingEntity cause) { prepare(actor,cause); }
    void adoptFakeEgg(Pig actor,LivingEntity cause){actor.entityTags().add(FAKE);prepare(actor,cause);bindFake(actor,cause);}
    private void bindFake(Entity actor,LivingEntity cause){
        var author=LbcIndirectKills.playerSource(cause);var accepted=author==null?new LbcIndirectKills.Owner(new UUID(0,0),0,true,false):game.items().indirectKills().wildCause(author);
        game.items().spellSources().bind(actor,new LbcSpellSource.Source(actor.getUUID(),accepted.player(),game.items().epoch(),accepted,Set.of(),true));
    }

    private void prepare(Entity actor, LivingEntity cause) {
        actor.entityTags().add(PIG);
        game.items().markCurrentItemEpoch(actor);
        causes.put(actor, cause);
        if (actor instanceof Pig pig) pigs.add(pig);
        else converted.add((ZombifiedPiglin) actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    private void forget(Entity actor) {
        pigs.remove(actor);
        converted.remove(actor);
        causes.remove(actor);
    }

    public static boolean managed(Entity actor) {
        return (actor instanceof Pig || actor instanceof ZombifiedPiglin) && actor.entityTags().contains(PIG);
    }

    public LivingEntity owner(Entity actor) {
        if (actor instanceof ItemEntity token && LbcLuckyPigDropItem.matches(token.getItem()))
            return game.items().specialDrops().owner(token);
        return managed(actor) ? causes.get(actor) : null;
    }

    /** Converted piglins keep native targeting, subject to the shared role and melee ally guards. */
    public boolean canAffect(Entity source, Entity target) {
        if (!managed(source) || target == null) return true;
        if (!active(source) || !game.items().wands().validTarget(target)) return false;
        if(source.entityTags().contains(FAKE))return game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target);
        return !(owner(source) instanceof ServerPlayer player)
                || target == player || game.items().wands().target(player, target);
    }

    public boolean canTarget(Entity source, LivingEntity target) { return canAffect(source, target); }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && (actor.level() == game.maps().level()||actor.entityTags().contains(FAKE)&&LbcDimensionItems.ordinary(actor.level())) && game.items().currentEpoch(actor);
    }

    /** Lightning conversion inherits only cause/cleanup; native piglin equipment and loot survive. */
    public void inheritConversion(Entity parent, Entity child) {
        if (!(parent instanceof Pig) || !managed(parent) || !active(parent)
                || !(child instanceof ZombifiedPiglin)) return;
        prepare(child, owner(parent));
        if(parent.entityTags().contains(FAKE)){child.entityTags().add(FAKE);bindFake(child,owner(parent));}
    }

    public void loaded(Entity actor) {
        if(actor instanceof LightningBolt bolt&&actor.entityTags().contains(FAKE_BOLT)){if(game.items().currentEpoch(actor))fakeBolts.add(bolt);else actor.discard();return;}
        if (!managed(actor)) return;
        if (!active(actor)) {
            unloadedCauses.remove(actor.getUUID());
            actor.discard();
            return;
        }
        if (!causes.containsKey(actor)) causes.put(actor, unloadedCauses.remove(actor.getUUID()));
        if (actor instanceof Pig pig) pigs.add(pig); else converted.add((ZombifiedPiglin) actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) {
        if(actor instanceof LightningBolt bolt)fakeBolts.remove(bolt);
        if (!managed(actor)) return;
        pigs.remove(actor);
        converted.remove(actor);
        if (active(actor) && actor.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK)
            unloadedCauses.put(actor.getUUID(), owner(actor));
        // Keep the cause through synchronous native death/conversion callbacks until the next tick.
    }

    public void tick() {
        var triggered=Collections.newSetFromMap(new IdentityHashMap<ServerLevel,Boolean>());
        for (var pig : List.copyOf(pigs)) {
            if (pig.isRemoved()) pigs.remove(pig);
            else if (!active(pig)) { pigs.remove(pig); pig.discard(); }
            else if(pig.entityTags().contains(FAKE)&&pig.hurtTime==2&&!game.items().timeStop().stopped((ServerLevel)pig.level())&&!game.items().timeStop().frozen(pig)){
                var level=(ServerLevel)pig.level();var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setPos(pig.position());bolt.entityTags().add("bolt");bolt.entityTags().add(FAKE_BOLT);game.items().markCurrentItemEpoch(bolt);bindFake(bolt,owner(pig));game.items().timeStop().prepareFrozen(bolt);fakeBolts.add(bolt);if(!level.addFreshEntity(bolt))fakeBolts.remove(bolt);fakeTimers.put(level,5);fakeSites.computeIfAbsent(level,ignored->new java.util.ArrayList<>()).add(new FakeSite(pig.position(),game.items().spellSources().source(bolt)));triggered.add(level);
            }
        }
        for (var piglin : List.copyOf(converted)) {
            if (piglin.isRemoved()) converted.remove(piglin);
            else if (!active(piglin)) { converted.remove(piglin); piglin.discard(); }
        }
        causes.keySet().removeIf(actor -> actor.isRemoved() || !active(actor)
                || !pigs.contains(actor) && !converted.contains(actor));
        fakeBolts.removeIf(Entity::isRemoved);
        for(var level:List.copyOf(fakeTimers.keySet())){if(triggered.contains(level)||game.items().timeStop().stopped(level))continue;int remaining=fakeTimers.get(level)-1;fakeTimers.put(level,remaining);if(remaining<=0){fakeTimers.remove(level);dressFake(level);}}
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) unloadedCauses.clear();
    }

    public void stop() {
        var bolts=List.copyOf(fakeBolts);fakeBolts.clear();fakeTimers.clear();fakeSites.clear();bolts.forEach(Entity::discard);
        unloadedCauses.clear();
        var oldPigs = List.copyOf(pigs);
        var oldConverted = List.copyOf(converted);
        pigs.clear();
        converted.clear();
        // Discard avoids both reward death and ordinary piglin loot on match cleanup.
        for (var pig : oldPigs) if (!pig.isRemoved()) pig.discard();
        for (var piglin : oldConverted) if (!piglin.isRemoved()) piglin.discard();
        causes.clear();
    }

    /** One source /data merge event five ticks after the most recent fake-pig bolt, never a tick NBT scan. */
    private void dressFake(ServerLevel level){
        final CompoundTag preset;
        try{preset=TagParser.parseCompoundFully("""
                {CustomNameVisible:1b,Health:50f,CustomName:{color:"red",italic:false,text:"Fake Lucky Pig"},
                 equipment:{mainhand:{id:"minecraft:golden_axe",count:1},offhand:{id:"minecraft:totem_of_undying",count:1},
                 feet:{id:"minecraft:golden_boots",count:1,components:{"minecraft:attribute_modifiers":[{type:"minecraft:movement_speed",id:"pigspeed34r23fhg87",amount:0.4,operation:"add_multiplied_base"}]}},
                 legs:{id:"minecraft:golden_leggings",count:1},chest:{id:"minecraft:golden_chestplate",count:1},head:{id:"minecraft:golden_helmet",count:1}},
                 drop_chances:{feet:0f,legs:0.085f,chest:0.085f,head:0.085f},attributes:[{id:"minecraft:max_health",base:50}]}
                """);}catch(Exception error){throw new IllegalStateException("Invalid fixed fake-pig equipment",error);}
        var sites=fakeSites.remove(level);if(sites==null)return;
        for(var site:sites)
            for(var piglin:level.getEntitiesOfClass(ZombifiedPiglin.class,new net.minecraft.world.phys.AABB(site.at,site.at).inflate(8),p->p.position().distanceToSqr(site.at)<=64&&game.items().wands().validRoleTarget(p,false))){
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,piglin.registryAccess());piglin.saveWithoutId(out);var data=out.buildResult();data.merge(preset.copy());
                var nearest=level.players().stream().filter(p->!p.isSpectator()).min(java.util.Comparator.comparingDouble(p->p.distanceToSqr(piglin))).orElse(null);
                if(nearest!=null)data.put("angry_at",UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE,nearest.getUUID()).getOrThrow());
                piglin.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));piglin.entityTags().add(FAKE);prepare(piglin,owner(piglin));
                if(site.source!=null)game.items().spellSources().bind(piglin,site.source);
            }
    }

    MatchQueuedWorkCheckpoint.Image fakeCheckpoint(MatchSpellCheckpoint.Links links){
        var bolts=fakeBolts.stream().map(links::key).toList();var timers=new IdentityHashMap<>(fakeTimers);var sites=new IdentityHashMap<ServerLevel,List<FakeSite>>();fakeSites.forEach((level,values)->sites.put(level,List.copyOf(values)));
        return current->{var rebound=bolts.stream().map(ref->(LightningBolt)current.get(ref)).toList();return ()->{fakeBolts.clear();fakeBolts.addAll(rebound);fakeTimers.clear();fakeTimers.putAll(timers);fakeSites.clear();sites.forEach((level,values)->fakeSites.put(level,new java.util.ArrayList<>(values)));};};
    }

    public int trackedCount() { return pigs.size(); }
    public int convertedCount() { return converted.size(); }
    public List<Pig> snapshot() { return List.copyOf(pigs); }
    public List<ZombifiedPiglin> snapshotConverted() { return List.copyOf(converted); }
}
