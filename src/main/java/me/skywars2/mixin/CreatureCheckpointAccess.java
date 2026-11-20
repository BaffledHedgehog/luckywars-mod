package me.skywars2.mixin;

import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native creature links and combat state that cannot be replayed through gameplay callbacks. */
public final class CreatureCheckpointAccess {
    private CreatureCheckpointAccess() { }

    @Mixin(AbstractSchoolingFish.class)
    public interface School {
        @Accessor("leader") AbstractSchoolingFish skywars2$leader();
        @Accessor("leader") void skywars2$leader(AbstractSchoolingFish value);
        @Accessor("schoolSize") int skywars2$schoolSize();
        @Accessor("schoolSize") void skywars2$schoolSize(int value);
    }

    @Mixin(Llama.class)
    public interface Caravan {
        @Accessor("caravanHead") void skywars2$caravanHead(Llama value);
        @Accessor("caravanTail") Llama skywars2$caravanTail();
        @Accessor("caravanTail") void skywars2$caravanTail(Llama value);
    }

    @Mixin(SpearUseGoal.class)
    public interface Spear {
        @Accessor("state") SpearUseGoal.SpearUseState skywars2$spearState();
        @Accessor("state") void skywars2$spearState(SpearUseGoal.SpearUseState value);
    }

    @Mixin(SpearUseGoal.SpearUseState.class)
    public interface SpearState {
        @Accessor("engageTime") int skywars2$engageTime();
        @Accessor("engageTime") void skywars2$engageTime(int value);
        @Accessor("fleeingTime") int skywars2$fleeingTime();
        @Accessor("fleeingTime") void skywars2$fleeingTime(int value);
        @Accessor("awayPos") Vec3 skywars2$awayPosition();
        @Accessor("awayPos") void skywars2$awayPosition(Vec3 value);
        @Accessor("done") boolean skywars2$done();
        @Accessor("done") void skywars2$done(boolean value);
    }
}
