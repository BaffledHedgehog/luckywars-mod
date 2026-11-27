package me.skywars2.game;

import java.util.Map;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.LbcDamage;
import me.skywars2.game.items.wands.LbcTerra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real one-step casts with misses measured from the native target's bounding box. */
public final class ProjectileSweepChecks {
    private static final Vec3 DIRECTION = new Vec3(1, 0, 0);
    private enum Shot { ELECTRO, TERRA, SHADOW, DOPPEL }

    private ProjectileSweepChecks() {}

    public static void run(GameController game) {
        try (var fixture = new WoolBucketChecks.Fixture(game, false)) {
            fixture.prepareParticipants();
            require(game.items().wands().electroCount() == 0 && game.items().terra().count() == 0
                && game.items().blind().snapshot().isEmpty() && game.items().psycho().snapshot().isEmpty(), "exclusive projectile fixture");
            var origin = fixture.resident(false).add(0, .5, 0);
            var start = BlockPos.containing(origin);
            for (var cell : BlockPos.betweenClosed(start.offset(-1, -1, -2), start.offset(4, 1, 2))) {
                if (!fixture.level.getBlockState(cell).isAir()) fixture.set(cell, Blocks.AIR.defaultBlockState());
            }
            fixture.move(fixture.owner, origin.add(-3, 0, 0));
            fixture.move(fixture.other, origin.add(-5, 0, 0));
            for (var shot : Shot.values()) {
                scenario(fixture, shot, origin, .20, true, "positive flank");
                scenario(fixture, shot, origin, -.20, true, "negative flank");
                scenario(fixture, shot, origin, shot == Shot.DOPPEL ? .55 : .30, false, "outside sweep");
                var wall = start.east();
                fixture.set(wall, Blocks.OBSIDIAN.defaultBlockState());
                try {
                    scenario(fixture, shot, origin, .20, shot == Shot.DOPPEL, "solid wall");
                } finally {
                    fixture.set(wall, Blocks.AIR.defaultBlockState());
                }
            }
            directArcSamples(fixture, origin);
            tinyPower(fixture, start);
            tinyTerraContainers(fixture, start);
        }
        SkyWars2.LOGGER.info("[check] PASS projectile-sweep (real Electro, Terra, Volos and Doppel contacts, out-of-range grazes, wall order, direct electric damage and both Tiny Terra container replacements)");
    }

    private static void tinyPower(WoolBucketChecks.Fixture fixture,BlockPos start) {
        var owner=fixture.owner;var eye=Vec3.atCenterOf(start);
        fixture.move(owner,eye.subtract(0,owner.getEyeHeight(),0));owner.setYRot(-90);owner.setXRot(0);
        var power=owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER);
        for(int kind:new int[]{1,6})for(boolean paid:new boolean[]{true,false}){
            var wand=fixture.leaf(kind==1?"tiny_blind_wand":"tiny_terra_wand");wand.enchant(power,1);
            owner.setItemInHand(InteractionHand.MAIN_HAND,wand);fixture.game.items().mana().set(owner,10000);
            var target=new Cow(EntityTypes.COW,fixture.level);target.setPos(eye.add(2,-.5,0));target.setNoAi(true);target.setNoGravity(true);
            require(fixture.level.addFreshEntity(target),"tiny Power target");fixture.own(target);
            float before=target.getHealth();
            if(paid)require(owner.gameMode.useItem(owner,fixture.level,wand,InteractionHand.MAIN_HAND).consumesAction(),"tiny paid cast");
            else fixture.game.items().wands().extraTiny(owner,kind);
            float expected=(kind==1?.25f:5f)*(paid?1.5f:1f);
            require(Math.abs(before-target.getHealth()-expected)<.001f,"tiny Power paid="+paid+" kind="+kind+" damage="+(before-target.getHealth()));
            fixture.discard(target);
        }
    }

    private static void tinyTerraContainers(WoolBucketChecks.Fixture fixture, BlockPos start) {
        var wall = start.east(2);
        var contents = Map.of(wall,new ItemStack(Items.DIAMOND,4),wall.above(),new ItemStack(Items.EMERALD,7));
        for(var entry:contents.entrySet()){
            var cell=entry.getKey();var goods=entry.getValue();
            goods.set(DataComponents.CUSTOM_NAME,Component.literal("Tiny Terra container "+cell.getY()));
            fixture.set(cell,Blocks.BARREL.defaultBlockState());
            ((Container)fixture.level.getBlockEntity(cell)).setItem(8,goods.copy());
        }
        var owner=fixture.owner;
        var eye=Vec3.atCenterOf(start);
        fixture.move(owner,eye.subtract(0,owner.getEyeHeight(),0));
        owner.setYRot(-90);owner.setXRot(0);
        var wand=fixture.leaf("tiny_terra_wand");
        owner.setItemInHand(InteractionHand.MAIN_HAND,wand);
        fixture.game.items().mana().set(owner,10000);
        require(owner.gameMode.useItem(owner,fixture.level,wand,InteractionHand.MAIN_HAND).consumesAction(),
                "actual Tiny Terra main-hand ray reaches the two occupied cells");
        require(fixture.level.getBlockState(wall).is(Blocks.MOSS_BLOCK)&&!fixture.level.getBlockState(wall.above()).is(Blocks.BARREL),
                "native ray replaces the struck barrel and the barrel above it");
        for(var entry:contents.entrySet()){
            var goods=entry.getValue();
            var drops=fixture.level.getEntitiesOfClass(ItemEntity.class,new AABB(entry.getKey()),
                    item->ItemStack.isSameItemSameComponents(item.getItem(),goods));
            drops.forEach(fixture::own);
            require(drops.size()==1&&ItemStack.matches(drops.getFirst().getItem(),goods),
                    "exact displaced Tiny Terra contents at "+entry.getKey());
        }
    }

    private static void scenario(WoolBucketChecks.Fixture fixture, Shot shot, Vec3 origin, double gap, boolean expectHit, String description) {
        var target = cow(fixture, origin, gap);
        try {
            var health = target.getHealth();
            advance(fixture, shot, origin);
            require((target.getHealth() < health) == expectHit, shot + " " + description
                + " gap=" + gap + " HP " + health + " -> " + target.getHealth());
        } finally {
            fixture.discard(target);
        }
    }

    private static Cow cow(WoolBucketChecks.Fixture fixture, Vec3 origin, double gap) {
        var target = new Cow(EntityTypes.COW, fixture.level);
        var z = gap == 0 ? 0 : Math.copySign(target.getBbWidth() * .5 + Math.abs(gap), gap);
        target.setPos(origin.add(1.35, -.5, z));
        target.setNoAi(true);
        target.setNoGravity(true);
        require(fixture.level.addFreshEntity(target), "native collision target");
        fixture.own(target);
        return target;
    }

    private static void advance(WoolBucketChecks.Fixture fixture, Shot shot, Vec3 origin) {
        var items = fixture.game.items();
        switch (shot) {
            case ELECTRO -> {
                items.wands().directedElectro(fixture.owner, origin, DIRECTION, 1, null);
                try { items.wands().tick(); } finally { items.wands().stop(); }
            }
            case TERRA -> {
                var charge = items.terra().castAt(fixture.owner, origin, DIRECTION, LbcTerra.Kind.REGULAR, 5);
                require(charge != null && fixture.level.getEntity(charge.getUUID()) == charge, "native Terra charge");
                fixture.own(charge);
                try { items.terra().tick(); } finally { fixture.discard(charge); }
            }
            case SHADOW -> {
                var arrow = new Arrow(EntityTypes.ARROW, fixture.level);
                arrow.setOwner(fixture.owner);
                try {
                    items.blind().spawnFromTrinket(arrow, origin, DIRECTION.scale(1.8));
                    var charges = items.blind().snapshot();
                    require(charges.size() == 1, "native trinket shadow charge");
                    var charge = charges.getFirst();
                    fixture.own(charge);
                    try { items.blind().tick(); } finally { fixture.discard(charge); }
                } finally { arrow.discard(); }
            }
            case DOPPEL -> {
                require(items.psycho().emitDoppel(fixture.owner, origin, DIRECTION), "native doppel charge");
                var charges = items.psycho().snapshot();
                require(charges.size() == 1, "one native doppel projectile");
                var charge = charges.getFirst();
                fixture.own(charge);
                try { items.psycho().tick(); } finally { fixture.discard(charge); }
            }
        }
    }

    private static void directArcSamples(WoolBucketChecks.Fixture fixture, Vec3 origin) {
        var cow = cow(fixture, origin, 0);
        try {
            var source = new DamageSource(fixture.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(LbcDamage.ELECTRO_BOLT), fixture.owner);
            var end = origin.add(DIRECTION.scale(2));
            var health = cow.getHealth();
            LbcDamage.hurtSegment(fixture.level, source, origin, end, Vec3.ZERO, .2, .5f, 2, target -> target == cow, false);
            var oldDamage = health - cow.getHealth();
            cow.setHealth(health);
            cow.setInvulnerableTime(0);
            cow.damageCooldownTime = 0;
            LbcDamage.hurtArc(fixture.level, source, origin, end, .2, .5f, 2, target -> target == cow, false, false, null, null);
            require(oldDamage > .5 && oldDamage == health - cow.getHealth(), "direct arc preserves legacy sample count");
        } finally { fixture.discard(cow); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Projectile sweep: " + message);
    }
}
