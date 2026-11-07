package me.skywars2.game;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Actual boss rewards, protected before publication rather than a delayed area search. */
final class LbcBossLoot {
    private static final String TAG="skywars2.boss_reward";
    private LbcBossLoot(){}
    static ItemEntity protect(ItemEntity item){
        // Entity has no public invulnerability setter in this target. One creation-time
        // native round trip preserves all stack/owner properties; no per-tick NBT work.
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,item.registryAccess());
        item.saveWithoutId(output);var saved=output.buildResult();saved.putBoolean("Invulnerable",true);
        item.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,item.registryAccess(),saved));
        item.entityTags().add(TAG);item.setNoGravity(true);
        item.setGlowingTag(true);item.setUnlimitedLifetime();item.setDeltaMovement(Vec3.ZERO);
        return item;
    }
    static boolean protectedReward(Entity entity){return entity instanceof ItemEntity&&entity.entityTags().contains(TAG);}
}
