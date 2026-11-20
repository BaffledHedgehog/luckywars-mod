package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import me.skywars2.mixin.CombatCheckpointAccess;
import me.skywars2.mixin.CreatureCheckpointAccess;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** One reviewed nested state, not a recursive copy of arbitrary native task graphs. */
final class MatchSpearGoal {
    final SpearUseGoal<?> goal;
    boolean wasRunning;
    private final Mob owner;
    private final LivingEntity target;
    private final SpearUseGoal.SpearUseState state;
    private final int engageTime, fleeingTime;
    private final Vec3 awayPosition;
    private final boolean done;
    private MatchDamageSources.Ref targetRef;
    private boolean referencesCaptured;

    MatchSpearGoal(Mob owner,SpearUseGoal<?> goal){
        this.owner=owner;this.goal=goal;target=owner.getTargetUnchecked();
        state = ((CreatureCheckpointAccess.Spear) goal).skywars2$spearState();
        if (state == null) {
            engageTime = 0;
            fleeingTime = 0;
            awayPosition = null;
            done = false;
        } else {
            if (state.getClass() != SpearUseGoal.SpearUseState.class) throw new IllegalStateException("Review native spear state subtype");
            var read = (CreatureCheckpointAccess.SpearState) state;
            engageTime = read.skywars2$engageTime();
            fleeingTime = read.skywars2$fleeingTime();
            awayPosition = read.skywars2$awayPosition();
            if (awayPosition != null && awayPosition.getClass() != Vec3.class)
                throw new IllegalStateException("Review mutable native spear vector");
            done = read.skywars2$done();
        }
    }
    void restore(){
        if (state != null) {
            var write = (CreatureCheckpointAccess.SpearState) state;
            write.skywars2$engageTime(engageTime);
            write.skywars2$fleeingTime(fleeingTime);
            write.skywars2$awayPosition(awayPosition);
            write.skywars2$done(done);
        }
        ((CreatureCheckpointAccess.Spear) goal).skywars2$spearState(state);
    }
    void captureReferences(MatchDamageSources sources){targetRef=sources.dependencyRef(target);referencesCaptured=true;}
    boolean restoreReferences(Function<UUID,Entity> lookup,Set<UUID> pending){
        if(!referencesCaptured||!wasRunning)return false;
        var resolved=MatchDamageSources.resolve(targetRef,lookup,pending);
        return targetRef!=null&&resolved==null&&MatchDamageSources.intentionallyAbsent(targetRef.id());
    }
    void absent() {
        ((CreatureCheckpointAccess.Spear) goal).skywars2$spearState(null);
        owner.getNavigation().stop();
        owner.setAggressive(false);
        // Native stopUsingItem can emit ITEM_INTERACT_FINISH when configured. Restore only its data values.
        var living = (CombatCheckpointAccess.Living) owner;
        living.skywars2$kinetic(null);
        living.skywars2$livingFlag(1, false);
        living.skywars2$useItem(ItemStack.EMPTY);
        living.skywars2$useItemRemaining(0);
    }
}
