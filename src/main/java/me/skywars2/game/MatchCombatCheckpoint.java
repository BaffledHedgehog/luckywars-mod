package me.skywars2.game;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import me.skywars2.mixin.CombatCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Native combat history/reference phase. Does not reapply damage, awards or enter/leave callbacks. */
final class MatchCombatCheckpoint {
    // Historical explosion/projectile anchors can already be removed or never registered.
    // Keep those original anchors; live actors/players ALWAYS resolve through the body owner.
    private record History(MatchDamageSources.Image source,float damage,net.minecraft.world.damagesource.FallLocation location,float distance) { }
    private record Kinetic(MatchDamageSources.Ref target,long stamp) { }
    private record Image(Class<?> type,UUID player,UUID mob,MatchDamageSources.Ref attacked,int memory,int mobTime,int attackTime,
                         MatchDamageSources.Image damage,long damageStamp,long worldTime,List<History> entries,int last,int start,int end,
                         boolean combat,boolean taking,ItemStack spin,EquipmentSlot spinSlot,boolean kineticPresent,
                         long kineticDefault,List<Kinetic> kinetic,Optional<BlockPos> climb,BlockPos position) { }
    private final Map<UUID,Image> images=new LinkedHashMap<>();
    private final MatchDamageSources sources;
    private final Map<UUID,Entity> registered=new LinkedHashMap<>();

    MatchCombatCheckpoint(GameController game,List<Entity> actors,MatchDamageSources sources) {
        this.sources=sources;
        for(var actor:actors)registered.put(actor.getUUID(),actor);
        for(var player:game.server().getPlayerList().getPlayers())registered.put(player.getUUID(),player);
        for(var entity:registered.values())if(entity instanceof LivingEntity living) {
            var access=(CombatCheckpointAccess.Living)living;var tracker=(CombatCheckpointAccess.Tracker)living.getCombatTracker();
            var history=new ArrayList<History>();
            for(var entry:tracker.skywars2$entries())history.add(new History(source(entry.source()),entry.damage(),entry.fallLocation(),entry.fallDistance()));
            var kinetic=new ArrayList<Kinetic>();var recent=access.skywars2$kinetic();
            if(recent!=null)for(var entry:recent.object2LongEntrySet())kinetic.add(new Kinetic(ref(entry.getKey()),entry.getLongValue()));
            var spin=access.skywars2$spin();EquipmentSlot slot=null;
            if(spin!=null&&!spin.isEmpty())for(var candidate:EquipmentSlot.values())if(living.getItemBySlot(candidate)==spin){slot=candidate;break;}
            images.put(living.getUUID(),new Image(living.getClass(),id(access.skywars2$player()),id(access.skywars2$mob()),ref(living.getLastHurtMob()),
                    access.skywars2$memory(),living.getLastHurtByMobTimestamp(),living.getLastHurtMobTimestamp(),source(access.skywars2$damage()),
                    access.skywars2$damageTime(),living.level().getGameTime(),List.copyOf(history),tracker.skywars2$last(),tracker.skywars2$start(),tracker.skywars2$end(),
                    tracker.skywars2$combat(),tracker.skywars2$taking(),spin==null?null:spin.copy(),slot,recent!=null,recent==null?0:recent.defaultReturnValue(),
                    List.copyOf(kinetic),access.skywars2$climb().map(BlockPos::immutable),access.skywars2$position()==null?null:access.skywars2$position().immutable()));
        }
    }
    private static UUID id(EntityReference<?> reference){return reference==null?null:reference.getUUID();}
    private MatchDamageSources.Ref ref(Entity entity){return sources.ref(entity);}
    private MatchDamageSources.Image source(DamageSource value){return sources.capture(value);}
    private static Entity resolve(UUID id,Function<UUID,Entity> lookup,Set<UUID> pending) {
        if(id==null)return null;var entity=lookup.apply(id);if(entity==null)pending.add(id);return entity;
    }
    private static Entity resolve(MatchDamageSources.Ref ref,Function<UUID,Entity> lookup,Set<UUID> pending) {
        return MatchDamageSources.resolve(ref,lookup,pending);
    }
    private static DamageSource value(MatchDamageSources.Image source){return MatchDamageSources.value(source);}
    /** Body and player phases must already have restored native clocks and identities. */
    void restore(Function<UUID,Entity> lookup,Set<UUID> pending) {
        // Retain exact source classes, original accepted immutable causes and nested wrappers.
        // Replacing them with a plain DamageSource would erase WILD/shard/NO_KNOCKBACK policy.
        sources.restore(lookup,pending);
        for(var entry:images.entrySet()) {
            var target=resolve(entry.getKey(),lookup,pending);if(target==null)continue;
            var image=entry.getValue();
            if(!(target instanceof LivingEntity living)||target.getClass()!=image.type)throw new IllegalStateException("Restore original living bodies before native combat");
            var access=(CombatCheckpointAccess.Living)living;
            var player=resolve(image.player,lookup,pending);var mob=resolve(image.mob,lookup,pending);var attacked=resolve(image.attacked,lookup,pending);
            access.skywars2$player(player instanceof Player p?EntityReference.of(p):image.player==null?null:EntityReference.of(image.player));
            access.skywars2$mob(mob instanceof LivingEntity m?EntityReference.of(m):image.mob==null?null:EntityReference.of(image.mob));
            access.skywars2$attacked(attacked instanceof LivingEntity l?l:null);access.skywars2$memory(image.memory);
            access.skywars2$mobTime(image.mobTime);access.skywars2$attackTime(image.attackTime);access.skywars2$damage(value(image.damage));
            long delta=living.level().getGameTime()-image.worldTime;
            access.skywars2$damageTime(image.damageStamp<0?image.damageStamp:Math.addExact(image.damageStamp,delta));
            access.skywars2$spin(image.spin==null?null:image.spinSlot==null?image.spin.copy():living.getItemBySlot(image.spinSlot));
            var kinetic=image.kineticPresent?new Object2LongOpenHashMap<Entity>():null;
            if(kinetic!=null) {
                kinetic.defaultReturnValue(image.kineticDefault);
                for(var saved:image.kinetic){var enemy=resolve(saved.target,lookup,pending);if(enemy!=null)kinetic.put(enemy,Math.addExact(saved.stamp,delta));}
            }
            access.skywars2$kinetic(kinetic);access.skywars2$climb(image.climb);access.skywars2$position(image.position);
            var tracker=(CombatCheckpointAccess.Tracker)living.getCombatTracker();tracker.skywars2$entries().clear();
            for(var saved:image.entries)tracker.skywars2$entries().add(new CombatEntry(value(saved.source),saved.damage,saved.location,saved.distance));
            tracker.skywars2$last(image.last);tracker.skywars2$start(image.start);tracker.skywars2$end(image.end);
            tracker.skywars2$combat(image.combat);tracker.skywars2$taking(image.taking);
            if(living instanceof ServerPlayer p)p.connection.send(image.combat?ClientboundPlayerCombatEnterPacket.INSTANCE:new ClientboundPlayerCombatEndPacket(living.getCombatTracker()));
        }
    }
}
