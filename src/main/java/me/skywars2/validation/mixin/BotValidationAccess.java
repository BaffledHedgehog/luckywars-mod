package me.skywars2.validation.mixin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.skywars2.game.LbcBotCrafting;
import me.skywars2.game.LbcBotDefense;
import me.skywars2.game.LbcBotSurvival;
import me.skywars2.game.LbcCarpetBots;
import me.skywars2.game.LbcChefBrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposed to the native Carpet fixtures only in the test-server artifact. */
public final class BotValidationAccess {
    private BotValidationAccess() {}

    @Mixin(value=LbcCarpetBots.class,remap=false)
    public interface Bots {
        @Accessor("states") Map<UUID,?> skywars2$validationStates();
    }

    @Mixin(targets="me.skywars2.game.LbcCarpetBots$State",remap=false)
    public interface State {
        @Accessor("brain") LbcChefBrain skywars2$validationBrain();
        @Accessor("survival") LbcBotSurvival skywars2$validationSurvival();
        @Accessor("crafting") LbcBotCrafting skywars2$validationCrafting();
        @Accessor("defense") LbcBotDefense skywars2$validationDefense();
        @Accessor("startingLoot") boolean skywars2$validationStartingLoot();
        @Accessor("startingLoot") void skywars2$validationStartingLoot(boolean enabled);
        @Accessor("nextLuckyScan") void skywars2$validationNextLuckyScan(int tick);
        @Accessor("nextScan") void skywars2$validationNextScan(int tick);
        @Accessor("nextRoam") void skywars2$validationNextRoam(int tick);
        @Accessor("chest") BlockPos skywars2$validationChest();
        @Accessor("chest") void skywars2$validationChest(BlockPos chest);
        @Accessor("chests") List<BlockPos> skywars2$validationChests();
        @Accessor("goal") void skywars2$validationGoal(Vec3 goal);
        @Accessor("mining") BlockPos skywars2$validationMining();
        @Accessor("miningSince") int skywars2$validationMiningSince();
        @Accessor("crystalsCleared") int skywars2$validationCrystalsCleared();
    }

    @Mixin(AbstractArrow.class)
    public interface Arrow {
        @Accessor("lastState") BlockState skywars2$validationLastState();
    }
}
