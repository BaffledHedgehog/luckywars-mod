package me.skywars2.validation.mixin;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.skywars2.game.GameController;
import me.skywars2.game.LbcItems;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.effects.LbcFrost;
import me.skywars2.game.effects.magic.LbcMana;
import me.skywars2.game.entities.mobs.LbcPsycho;
import me.skywars2.game.entities.projectiles.LbcHoming;
import me.skywars2.game.items.trinkets.LbcOrdinaryAccessories;
import me.skywars2.game.items.wands.LbcElectro;
import me.skywars2.game.items.weapons.ranged.LbcQuiver;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-edition access to state owned by real listeners, players, and item services. */
public final class ItemFixtureAccess {
    private ItemFixtureAccess() {}

    @Mixin(value = LbcIndirectKills.class, remap = false)
    public interface TerrainLedger {
        @Accessor("stone") Map<ServerLevel, ? extends Map<BlockPos, ?>> validation$stone();
        @Accessor("hazards") Map<ServerLevel, ? extends Map<BlockPos, ?>> validation$hazards();
    }

    @Mixin(LivingEntity.class)
    public interface Equipment {
        @Accessor("lastEquipmentItems") Map<EquipmentSlot, ItemStack> validation$equipment();
    }

    @Mixin(ServerPlayer.class)
    public interface PlayerSync {
        @Accessor("lastRecordedFoodLevel") int validation$recordedFood();
        @Accessor("lastRecordedFoodLevel") void validation$recordedFood(int value);
        @Accessor("lastSentFood") int validation$sentFood();
        @Accessor("lastSentFood") void validation$sentFood(int value);
        @Accessor("lastFoodSaturationZero") boolean validation$zeroSaturation();
        @Accessor("lastFoodSaturationZero") void validation$zeroSaturation(boolean value);
        @Accessor("lastSentHealth") float validation$sentHealth();
        @Accessor("lastSentHealth") void validation$sentHealth(float value);
        @Accessor("lastRecordedHealthAndAbsorption") float validation$recordedHealth();
        @Accessor("lastRecordedHealthAndAbsorption") void validation$recordedHealth(float value);
    }

    @Mixin(FoodData.class)
    public interface Food {
        @Accessor("exhaustionLevel") float validation$exhaustion();
        @Accessor("exhaustionLevel") void validation$exhaustion(float value);
        @Accessor("tickTimer") int validation$timer();
        @Accessor("tickTimer") void validation$timer(int value);
    }

    @Mixin(Level.class)
    public interface LevelRandom {
        @Mutable @Accessor("random") void validation$random(RandomSource value);
    }

    @Mixin(LootTable.class)
    public interface Loot {
        @Accessor("randomSequence") Optional<Identifier> validation$sequence();
        @Accessor("pools") List<LootPool> validation$pools();
    }

    @Mixin(ItemEntity.class)
    public interface Drop {
        @Accessor("age") void validation$age(int value);
        @Accessor("pickupDelay") int validation$pickupDelay();
        @Accessor("thrower") EntityReference<Entity> validation$thrower();
        @Accessor("target") UUID validation$target();
    }

    @Mixin(AbstractArrow.class)
    public interface Arrow {
        @Accessor("baseDamage") double validation$damage();
    }

    @Mixin(value = GameController.class, remap = false)
    public interface Match {
        @Accessor("stage") void validation$stage(GameController.Stage stage);
    }

    @Mixin(value = LbcOrdinaryAccessories.class, remap = false)
    public interface Accessories {
        @Accessor("phase") int validation$phase();
        @Accessor("phase") void validation$phase(int value);
    }

    @Mixin(value = LbcQuiver.class, remap = false)
    public interface Quiver {
        @Accessor("clock") int validation$clock();
        @Accessor("clock") void validation$clock(int value);
    }

    @Mixin(value = LbcPsycho.class, remap = false)
    public interface Psycho {
        @Accessor("effects") Map<LivingEntity, ?> validation$effects();
        @Accessor("rays") Map<ServerPlayer, Integer> validation$rays();
    }

    @Mixin(value = LbcElectro.class, remap = false)
    public interface Electro {
        @Accessor("shocks") IdentityHashMap<LivingEntity, ?> validation$shocks();
    }

    @Mixin(targets = "me.skywars2.game.items.wands.LbcAcidCorruption", remap = false)
    public interface Corruption {
        @Accessor("deaths") Map<LivingEntity, ?> validation$deaths();
    }

    @Mixin(value = LbcMana.class, remap = false)
    public interface Mana {
        @Accessor("reverseOwners") Map<ServerPlayer, ?> validation$reverseOwners();
    }

    @Mixin(value = LbcItems.class, remap = false)
    public interface Items {
        @Accessor("turnCurses") Map<UUID, ?> validation$turnCurses();
        @Accessor("rotations") Map<UUID, ?> validation$rotations();
    }

    @Mixin(value = LbcHoming.class, remap = false)
    public interface Homing {
        @Accessor("curses") IdentityHashMap<LivingEntity, ?> validation$curses();
    }

    @Mixin(value = LbcFrost.class, remap = false)
    public interface Frost {
        @Accessor("DATA")
        static AttachmentType<CompoundTag> validation$data() {
            throw new AssertionError("Untransformed test accessor");
        }

        @Accessor("affected") Map<LivingEntity, ?> validation$affected();
    }
}
