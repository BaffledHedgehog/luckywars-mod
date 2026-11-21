package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.mixin.PlayerCheckpointAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Shared native value image. Registration, player lifecycle and relationship phases stay with their owners. */
public final class MatchEntityState {
    final Entity original;
    final ServerLevel level;
    final CompoundTag nbt;
    final Vec3 position,motion;
    final float yaw,pitch;
    private final AttributeMap attributes;
    private final List<SynchedEntityData.DataValue<?>> metadata;
    private final MatchEntityRuntime runtime;
    private final MatchRandomState random;
    private final Object owned;
    private final GameController game;

    public MatchEntityState(GameController game,Entity entity) {
        this.game=game;original=entity;level=(ServerLevel)entity.level();random=MatchRandomState.capture(entity.getRandom());
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,entity.registryAccess());entity.saveWithoutId(out);nbt=out.buildResult().copy();
        // Passengers have independent images; NBT must never manufacture duplicate children.
        nbt.remove("Passengers");position=entity.position();motion=entity.getDeltaMovement();yaw=entity.getYRot();pitch=entity.getXRot();
        if(entity instanceof LivingEntity living) {
            @SuppressWarnings("unchecked") var type=(net.minecraft.world.entity.EntityType<? extends LivingEntity>)living.getType();
            attributes=new AttributeMap(DefaultAttributes.getSupplier(type));
            BuiltInRegistries.ATTRIBUTE.listElements().forEach(attribute->{if(attributes.hasAttribute(attribute))attributes.getInstance(attribute);});
            attributes.assignAllValues(living.getAttributes());owned=game.items().ownedEffects().checkpoint(living);
        }else{attributes=null;owned=null;}
        var data=new ArrayList<SynchedEntityData.DataValue<?>>();
        for(var item:((PlayerCheckpointAccess.Metadata)entity.getEntityData()).skywars2$items())data.add(item.value());
        metadata=List.copyOf(data);runtime=new MatchEntityRuntime(entity);
    }
    private static <T> SynchedEntityData.DataValue<T> copy(SynchedEntityData.DataValue<T> value) {
        return new SynchedEntityData.DataValue<>(value.id(),value.serializer(),value.serializer().copy(value.value()));
    }
    List<SynchedEntityData.DataValue<?>> metadata() {
        var result=new ArrayList<SynchedEntityData.DataValue<?>>();for(var value:metadata)result.add(copy(value));return result;
    }
    public void load(Entity target) {
        if(target.getType()!=original.getType()||target.getClass()!=original.getClass())throw new IllegalArgumentException("Different native entity body type");
        runtime.prepareLoad(target);
        if(original instanceof LivingEntity living&&!MatchOfflinePlayers.alias(original))game.items().ownedEffects().forget(living);
        if(target instanceof LivingEntity living){game.items().ownedEffects().forget(living);living.removeAllEffects();}
        var input=nbt.copy();
        // Native Raider loading joins the current Raid and changes its leader map. The world owner
        // and body graph are restored separately during experimental actor replay.
        if(MatchMobGoals.enabled()&&MatchActorCheckpoint.restoring(target)&&target instanceof net.minecraft.world.entity.raid.Raider)input.remove("RaidId");
        target.load(TagValueInput.create(ProblemReporter.DISCARDING,target.registryAccess(),input));
        if(target instanceof LivingEntity living){living.getAttributes().assignAllValues(attributes);ShadowAbsorption.reconcile(living,false);}
        target.getEntityData().assignValues(metadata());
        // Pose/scale callbacks may refresh dimensions; the checkpoint owns the final position.
        target.setPos(position);
        if(target instanceof LivingEntity living)game.items().ownedEffects().restoreCheckpoint(living,owned);
        finishRuntime(target);
    }
    void finishRandom(Entity target){random.restore(target.getRandom());}
    void captureGoalReferences(MatchDamageSources sources){runtime.captureGoalReferences(sources);}
    void restoreGoalReferences(java.util.function.Function<java.util.UUID,Entity> lookup,java.util.Set<java.util.UUID> pending){runtime.restoreGoalReferences(lookup,pending);}
    void finishRuntime(Entity target) {runtime.restore(target);finishRandom(target);target.setDeltaMovement(motion);target.needsSync=true;}
}
