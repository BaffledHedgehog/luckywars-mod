package me.skywars2.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import me.skywars2.SkyWars2;
import me.skywars2.game.genie.GenieRuntime;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** Connects accepted damage to the actual native attack or launch that produced it. */
public final class GenieCombat {
    private record Weapon(UUID owner, String epoch, boolean offHand, ItemStack snapshot, WeakReference<ItemStack> physical) {
        Weapon(UUID owner, String epoch, boolean offHand, ItemStack snapshot) {
            this(owner, epoch, offHand, snapshot, new WeakReference<>(null));
        }
        InteractionHand hand() {
            return offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        }
    }

    private static final Codec<Weapon> CODEC = RecordCodecBuilder.create(fields -> fields.group(
        UUIDUtil.CODEC.fieldOf("owner").forGetter(Weapon::owner),
        Codec.STRING.fieldOf("epoch").forGetter(Weapon::epoch),
        Codec.BOOL.fieldOf("off_hand").forGetter(Weapon::offHand),
        ItemStack.CODEC.fieldOf("weapon").forGetter(Weapon::snapshot)
    ).apply(fields, Weapon::new));
    private static final AttachmentType<Weapon> FIRED = AttachmentRegistry.create(
        Identifier.parse("skywars2:genie_attack_source"), builder -> builder.persistent(CODEC));
    private static final AttachmentType<Weapon> SPELL = AttachmentRegistry.create(
        Identifier.parse("skywars2:genie_spell_weapon"), builder -> builder.persistent(CODEC));
    // No value retains its DamageSource key. One native source covers primary and sweeping contacts.
    private static final Map<DamageSource, Weapon> MELEE = new WeakHashMap<>();

    private GenieCombat() {}
    static void register() {}

    public static void beganMelee(ServerPlayer owner, InteractionHand hand, ItemStack stack, DamageSource source) {
        if (!GenieRuntime.matches(stack)) return;
        var game = SkyWars2.game();
        if (game != null) MELEE.put(source, capture(game, owner, hand, stack, stack));
    }

    static void launched(GameController game, ServerPlayer owner, InteractionHand hand, ItemStack physical,
                         ItemStack snapshot, Iterable<? extends Entity> bodies) {
        if (!GenieRuntime.matches(snapshot)) return;
        var weapon = capture(game, owner, hand, physical, snapshot);
        for (var body : bodies) {
            if (ownedLaunch(body, owner)) body.setAttached(FIRED, weapon);
        }
    }

    static boolean ownedLaunch(Entity body, ServerPlayer owner) {
        return body instanceof TraceableEntity trace && trace.getOwner() == owner;
    }

    static void castProjectile(GameController game, ServerPlayer owner, Projectile shot, ItemStack stack) {
        if (!stack.isEmpty()) shot.setAttached(SPELL, new Weapon(owner.getUUID(), game.items().epoch(), false, stack.copyWithCount(1)));
    }

    // A spell effect can carry enchants without admitting a second input event for the same script.
    static ItemStack spellWeapon(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer owner)
                || !(source.getDirectEntity() instanceof Projectile shot) || shot.getOwner() != owner) return ItemStack.EMPTY;
        var weapon = shot.getAttached(SPELL);
        if (weapon == null || !weapon.owner().equals(owner.getUUID())) return ItemStack.EMPTY;
        var game = SkyWars2.game();
        return game != null && weapon.epoch().equals(game.items().epoch())
            ? weapon.snapshot().copy() : ItemStack.EMPTY;
    }

    static void accepted(GameController game, LivingEntity victim, DamageSource actual) {
        var source = LbcIndirectKills.originalDamage(actual);
        if (!(source.getEntity() instanceof ServerPlayer owner) || actual.getEntity() != owner) return;
        Entity body = source.getDirectEntity();
        Weapon weapon;
        if (body == owner) {
            // Witch statistics wraps each actual contact, preserving the attack's original source.
            var attack = LbcWitchStatistics.checkpointWrappedSource(source);
            weapon = attack == null ? null : MELEE.get(attack);
        } else {
            weapon = ownedLaunch(body, owner) ? body.getAttached(FIRED) : null;
        }
        if (weapon == null || !weapon.owner().equals(owner.getUUID()) || !weapon.epoch().equals(game.items().epoch())) return;
        var physical = weapon.physical().get();
        game.items().genie().runtime().hit(owner, victim, weapon.hand(),
            physical == null ? ItemStack.EMPTY : physical, weapon.snapshot().copy(), body == owner ? null : body);
    }

    private static Weapon capture(GameController game, ServerPlayer owner, InteractionHand hand,
                                  ItemStack physical, ItemStack snapshot) {
        return new Weapon(owner.getUUID(), game.items().epoch(), hand == InteractionHand.OFF_HAND,
            snapshot.copyWithCount(1), new WeakReference<>(physical));
    }
}
