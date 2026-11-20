package me.skywars2.validation.mixin;

import java.util.Map;
import java.util.Set;
import me.skywars2.game.LbcBolts;
import me.skywars2.game.LbcHoming;
import me.skywars2.game.LbcIndirectKills;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to native state needed by the isolated Acid and Sauvojen verification fixtures. */
public final class FixtureAccess {
    private FixtureAccess() { }

    @Mixin(value = LbcIndirectKills.class, remap = false)
    public interface Attribution {
        @Accessor("logoutAbsorption") Map<ServerPlayer, ?> validation$logoutAbsorption();
        @Accessor("logoutAttacks") Map<ServerPlayer, ?> validation$logoutAttacks();
        @Accessor("lootOwners") Map<LivingEntity, ?> validation$lootOwners();
        @Accessor("falls") Map<LivingEntity, ?> validation$falls();
        @Accessor("statuses") Map<LivingEntity, ?> validation$statuses();
        @Accessor("pendingMana") Map<LivingEntity, ?> validation$pendingMana();
        @Accessor("manaDrains") Map<LivingEntity, ?> validation$manaDrains();
        @Accessor("manaDebts") Map<LivingEntity, ?> validation$manaDebts();
        @Accessor("hungerDebts") Map<ServerPlayer, ?> validation$hungerDebts();
        @Accessor("removedSupports") Map<LivingEntity, ?> validation$removedSupports();
        @Accessor("laterAttacks") Map<LivingEntity, ?> validation$laterAttacks();
        @Accessor("burning") Map<LivingEntity, ?> validation$burning();
        @Accessor("arrivals") Map<LivingEntity, ?> validation$arrivals();
    }

    @Mixin(PersistentEntitySectionManager.class)
    public interface NativeEntityGetter {
        @Mutable @Accessor("entityGetter") void validation$entityGetter(LevelEntityGetter<Entity> getter);
    }

    @Mixin(Entity.class)
    public interface EntityRandom {
        @Mutable @Accessor("random") void validation$random(RandomSource random);
    }

    @Mixin(value = LbcHoming.class, remap = false)
    public interface Homing {
        @Accessor("projectiles") Set<Entity> validation$projectiles();
    }

    @Mixin(value = LbcBolts.class, remap = false)
    public interface BoltActors {
        @Accessor("waters") Set<?> validation$waters();
    }
}
