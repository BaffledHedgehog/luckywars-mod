package me.skywars2.mixin;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class CombatCheckpointAccess {
    private CombatCheckpointAccess() { }
    @Mixin(LivingEntity.class) public interface Living {
        @Accessor("lastHurtByPlayer") EntityReference<Player> skywars2$player();
        @Accessor("lastHurtByPlayer") void skywars2$player(EntityReference<Player> value);
        @Accessor("lastHurtByMob") EntityReference<LivingEntity> skywars2$mob();
        @Accessor("lastHurtByMob") void skywars2$mob(EntityReference<LivingEntity> value);
        @Accessor("lastHurtMob") void skywars2$attacked(LivingEntity value);
        @Accessor("lastHurtByPlayerMemoryTime") int skywars2$memory();
        @Accessor("lastHurtByPlayerMemoryTime") void skywars2$memory(int value);
        @Accessor("lastHurtByMobTimestamp") void skywars2$mobTime(int value);
        @Accessor("lastHurtMobTimestamp") void skywars2$attackTime(int value);
        @Accessor("lastDamageSource") DamageSource skywars2$damage();
        @Accessor("lastDamageSource") void skywars2$damage(DamageSource value);
        @Accessor("lastDamageStamp") long skywars2$damageTime();
        @Accessor("lastDamageStamp") void skywars2$damageTime(long value);
        @Accessor("autoSpinAttackItemStack") ItemStack skywars2$spin();
        @Accessor("autoSpinAttackItemStack") void skywars2$spin(ItemStack value);
        @Accessor("recentKineticEnemies") Object2LongMap<Entity> skywars2$kinetic();
        @Accessor("recentKineticEnemies") void skywars2$kinetic(Object2LongMap<Entity> value);
        @Accessor("lastClimbablePos") Optional<BlockPos> skywars2$climb();
        @Accessor("lastClimbablePos") void skywars2$climb(Optional<BlockPos> value);
        @Accessor("lastPos") BlockPos skywars2$position();
        @Accessor("lastPos") void skywars2$position(BlockPos value);
    }
    @Mixin(CombatTracker.class) public interface Tracker {
        @Accessor("entries") List<CombatEntry> skywars2$entries();
        @Accessor("lastDamageTime") int skywars2$last();
        @Accessor("lastDamageTime") void skywars2$last(int value);
        @Accessor("combatStartTime") int skywars2$start();
        @Accessor("combatStartTime") void skywars2$start(int value);
        @Accessor("combatEndTime") int skywars2$end();
        @Accessor("combatEndTime") void skywars2$end(int value);
        @Accessor("inCombat") boolean skywars2$combat();
        @Accessor("inCombat") void skywars2$combat(boolean value);
        @Accessor("takingDamage") boolean skywars2$taking();
        @Accessor("takingDamage") void skywars2$taking(boolean value);
    }
    @Mixin(DamageSource.class) public interface Source {
        @Mutable @Accessor("directEntity") void skywars2$direct(Entity value);
        @Mutable @Accessor("causingEntity") void skywars2$cause(Entity value);
    }
}
