package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import me.skywars2.game.genie.GenieRuntime;
import me.skywars2.game.genie.GenieScriptItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** One accepted-volley continuation for native bows/crossbows and automatic bow pulses. */
public final class LbcRangedShots {
    private LbcRangedShots(){}

    /** Receives only bodies from this shot's synchronous weapon branches. */
    public static final class Shot implements Consumer<Entity> {
        private final GameController game;
        private final ServerPlayer owner;
        private final InteractionHand hand;
        private final ItemStack physical, fired;
        private final LinkedHashSet<Entity> bodies;

        Shot(GameController game, ServerPlayer owner, InteractionHand hand, ItemStack physical, ItemStack fired,
             List<Projectile> nativeFamily) {
            this.game = game;
            this.owner = owner;
            this.hand = hand;
            this.physical = physical;
            this.fired = fired;
            bodies = new LinkedHashSet<>(nativeFamily);
        }

        @Override public void accept(Entity body) {
            bodies.add(body);
        }

        public void include(List<? extends Entity> family) {
            bodies.addAll(family);
        }

        public void complete() {
            // Ammo donors and retired arrows never enter the delivered event.
            var live = bodies.stream().filter(body -> !body.isRemoved()
                && owner.level().getEntity(body.getUUID()) == body).toList();
            GenieCombat.launched(game, owner, hand, physical, fired, live);
            var deliver = game.items().genie().runtime().captureShot(owner, hand, physical, fired, live, false);
            LbcTmtShotCompletion.afterNativeAction(deliver);
        }
    }

    public static boolean replacesNative(GameController game,ServerPlayer player,ItemStack firedWeapon){
        return GenieScriptItem.replacesShot(firedWeapon)&&game.items().genie().runtime().admitted(player);
    }

    public static void acceptedReplacement(GameController game, ServerPlayer player, InteractionHand hand,
            ItemStack weapon, ItemStack firedWeapon, List<Projectile> templates) {
        var deliver = game.items().genie().runtime().captureShot(player, hand, weapon, firedWeapon, templates, true);
        LbcTmtShotCompletion.afterNativeAction(deliver);
    }

    public static void accepted(GameController game,ServerPlayer player,InteractionHand hand,ItemStack weapon,
            ItemStack firedWeapon,List<Projectile> spawned){
        var family = List.copyOf(spawned);
        var shot = !family.isEmpty() && GenieRuntime.matches(firedWeapon)
            ? new Shot(game, player, hand, weapon, firedWeapon, family) : null;
        if (LbcTmtRanged.onShot(game, player, hand, weapon, firedWeapon, family, shot)) return;
        try {
            acceptedTraits(game, player, hand, weapon, firedWeapon, family, shot);
        } finally {
            if (shot != null) shot.complete();
        }
    }

    private static void acceptedTraits(GameController game, ServerPlayer player, InteractionHand hand,
            ItemStack weapon, ItemStack fired, List<Projectile> nativeFamily, Shot shot) {
        LbcLegacyRanged.installRocketAmmo(game, player, fired, nativeFamily);
        boolean traitsRolled = ItemData.intValue(fired, "explosive", 0) == 1;
        if (traitsRolled) {
            game.items().trinkets().shot(player, nativeFamily);
            if (game.items().balance().explosiveBow(player, fired, nativeFamily, shot)) return;
        }
        if (LbcChargedBows.matches(fired)) {
            if (!traitsRolled) game.items().trinkets().shot(player, nativeFamily);
            game.items().chargedBows().release(player, fired, player.getTicksUsingItem(), nativeFamily);
            return;
        }
        var imba = LbcImbaCrossbows.augment(game, player, fired, nativeFamily);
        if (imba.isEmpty()) return;
        if (shot != null) shot.include(imba);
        if (LbcImbaCrossbows.isBeam(fired)) {
            game.items().trinkets().shot(player, imba);
            LbcImbaCrossbows.release(game, player, fired, imba);
            return;
        }
        var oldFamily = game.items().balance().augment(player, fired, imba);
        if (shot != null) shot.include(oldFamily);
        var family = game.items().shulkerBow().augment(player, fired, oldFamily);
        if (shot != null) shot.include(family);
        game.items().madokaBow().augment(player, fired, family);
        if (!traitsRolled) game.items().trinkets().shot(player, family);
        LbcLegacyRanged.convert(game, player, fired, family, shot);
        LbcMinigunCrossbow.afterShot(game, player, hand, weapon, family, fired);
        LbcLegacyRanged.afterShot(game, player, hand, weapon, family, fired);
    }
}
