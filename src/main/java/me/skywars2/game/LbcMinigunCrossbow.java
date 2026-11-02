package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Source fast/fast_old reload, called once after publication of a successful native crossbow volley. */
public final class LbcMinigunCrossbow {
    private LbcMinigunCrossbow() { }

    public static void afterShot(GameController game, ServerPlayer player, InteractionHand hand,
                                 ItemStack weapon, List<Projectile> family) {
        afterShot(game,player,hand,weapon,family,weapon);
    }

    /** The pre-shot snapshot also identifies a terminal native shot which broke its actual weapon. */
    public static void afterShot(GameController game, ServerPlayer player, InteractionHand hand,
                                 ItemStack weapon, List<Projectile> family, ItemStack firedWeapon) {
        if (!(firedWeapon.getItem() instanceof CrossbowItem) || family.isEmpty()) return;
        var data=firedWeapon.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        boolean current=data.getIntOr("fast",0)==1;
        boolean old=data.getIntOr("fast_old",0)==1;
        if ((!current && !old) || game.items().magic().mechanismSuppressed(player)) return;

        // performShooting already emptied this component before the shared native shoot call.
        // Keep the actual weapon, including native shot durability and all user components.
        if (!weapon.isEmpty() && weapon.getItem() instanceof CrossbowItem crossbow
                && player.getItemInHand(hand)==weapon && !CrossbowItem.isCharged(weapon)) {
            var ammo=selectedAmmo(player,crossbow);
            if (!ammo.isEmpty()) {
                int count=EnchantmentHelper.processProjectileCount(player.level(),weapon,player,1);
                if (count>0) {
                    var drawn=new ArrayList<ItemStack>(count);
                    for (int i=0;i<count;i++) drawn.add(ammo.copyWithCount(1));
                    weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(drawn));
                    // The source has one inventory debit per reload, including multishot.
                    // Old fast_old predates the infinite-arrow exception and source achievement.
                    if (!current || !hasInfiniteArrow(player)) {
                        ammo.shrink(1);
                        if (ammo.isEmpty()) player.getInventory().removeItem(ammo);
                    }
                }
            }
        }
        if (current) {
            var shots=game.items().mana().score(player,"adv_game_minigun_shooted");
            shots.add(1);
            if (shots.get()>=320)
                game.items().mana().award(player,"lbc:true_advancements/bows/minigun");
        }
    }

    /** Native held-first selection, without Player.getProjectile's creative generic-arrow fallback. */
    private static ItemStack selectedAmmo(ServerPlayer player, ProjectileWeaponItem weapon) {
        var held=ProjectileWeaponItem.getHeldProjectile(player,weapon.getSupportedHeldProjectiles());
        if (!held.isEmpty()) return held;
        for (int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            var candidate=player.getInventory().getItem(slot);
            if (!candidate.isEmpty() && weapon.getAllSupportedProjectiles().test(candidate)) return candidate;
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasInfiniteArrow(ServerPlayer player) {
        // Use the same native counting-only inventory/crafting/cursor scope as source /clear ports.
        return player.getInventory().clearOrCountMatchingItems(stack ->
                stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()
                        .getIntOr("infinite_arrow",0)==1,
                true,0,player.inventoryMenu.getCraftSlots())>0;
    }
}
