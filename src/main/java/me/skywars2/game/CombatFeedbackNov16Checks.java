package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Native damage criteria, real mana consumers and the actual landing surface. */
public final class CombatFeedbackNov16Checks {
    private CombatFeedbackNov16Checks() {}
    private static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException("Combat feedback: " + reason);
    }
    public static void run(GameController game) {
        try (var f = new WoolBucketChecks.Fixture(game, false)) {
            f.prepareParticipants();
            var owner = f.owner; var mana = game.items().mana(); var swords = game.items().chargedSwords();
            owner.setItemInHand(InteractionHand.MAIN_HAND, f.leaf("blazefire_sword"));
            var cow = new Cow(EntityTypes.COW, f.level);
            cow.setPos(owner.position().add(0, 0, 2)); cow.setNoAi(true); cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); cow.setHealth(1000);
            require(f.level.addFreshEntity(cow), "native target spawned"); f.own(cow);
            var melee = owner.damageSources().playerAttack(owner);
            int charge = swords.blazeCharges(owner);
            require(cow.hurtServer(f.level, melee, 4), "ordinary native contact");
            require(swords.blazeCharges(owner) == ++charge, "positive hit charges once");
            cow.setInvulnerableTime(0); cow.damageCooldownTime=0;
            cow.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);
            cow.setAbsorptionAmount(100);
            float health = cow.getHealth(); cow.hurtServer(f.level, melee, 4);
            require(cow.getHealth() == health && swords.blazeCharges(owner) == ++charge,
                    "fully resisted native contact still charges once");
            cow.damageCooldownTime=20; cow.setAbsorptionAmount(0);
            require(!cow.hurtServer(f.level, melee, 4) && swords.blazeCharges(owner) == charge,
                    "rejected damage attempt does not invent an outgoing hit");
            cow.setHealth(1); cow.setInvulnerableTime(0); cow.damageCooldownTime=0;
            cow.hurtServer(f.level, melee, 40);
            require(cow.isDeadOrDying() && swords.blazeCharges(owner) == ++charge, "fatal hit charges exactly once");

            var slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            var names = new String[]{"helmet", "chestplate", "leggings", "boots"};
            for (int i = 0; i < slots.length; i++) owner.setItemSlot(slots[i], f.leaf("overload_" + names[i]));
            mana.inventoryChanged(owner); require(mana.overloadArmor(owner) == 15, "real complete Overload set");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LbcLaserMinigun.ITEM));
            var minigun = game.items().laserMinigun();
            for (int balance : new int[]{0, -100, 99}) {
                mana.set(owner, balance); int shots = minigun.shots(owner);
                for (int i = 0; i < 35; i++) { minigun.use(owner, InteractionHand.MAIN_HAND); minigun.tick(); }
                require(minigun.spin(owner)==30 && mana.get(owner) == balance && minigun.shots(owner) == shots,
                        "real minigun cannot overdraw at mana " + balance);
            }
            require(mana.trySpendWand(owner, 50) && mana.get(owner) == 49,
                    "real wand payment retains complete-set overdraft policy");
            mana.set(owner, 0);
            require(mana.trySpendWand(owner, 50) && mana.get(owner) == -50, "wand can still overdraw from zero");
            minigun.forget(owner);

            var floor = BlockPos.containing(f.home).below(3);
            f.set(floor, Blocks.STONE.defaultBlockState()); double surface = floor.getY() + 1;
            owner.setPos(floor.getX() + .5, surface + .8, floor.getZ() + .5); owner.setOnGround(false);
            owner.entityTags().add("overload_slam"); mana.score(owner, "overload_slam").set(20);
            mana.overload().armor().tick(owner);
            require(owner.entityTags().contains("overload_slam") && mana.score(owner, "overload_slam").get() == 22,
                    "floor in cell below cannot trigger an airborne impact");
            owner.setPos(owner.getX(), surface + .02, owner.getZ());
            mana.overload().armor().tick(owner);
            require(!owner.entityTags().contains("overload_slam") && mana.score(owner, "overload_slam").get() == 0,
                    "actual contact with the collision surface triggers impact");
        }
        SkyWars2.LOGGER.info("[check] PASS combat-feedback-nov16 (native positive/resisted/fatal/rejected hits; actual Overload minigun zero/negative/insufficient mana; wand overdraft retained; physical slam landing)");
    }
}
