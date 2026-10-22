package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The shared drop dispatcher owns admission and consumption of the levitation token. */
public final class LbcEnzanDrop {
    private final GameController game;

    public LbcEnzanDrop(GameController game) { this.game = game; }

    public void spawn(ItemEntity token) {
        if (!(token.level() instanceof ServerLevel level)) return;
        var target = game.items().cageDrops().target(token);
        if (target != null) {
            // Native effect hooks decide acceptance and retain the actual thrower's causal source.
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 127),
                    game.items().specialDrops().owner(token));
            var reward = bucket();
            // A bucket cannot merge; bypass add's full-Creative deletion when no slot is available.
            if (target.getInventory().getFreeSlot() >= 0) target.getInventory().add(reward);
            if (!reward.isEmpty()) target.drop(reward, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        level.playSound(null, token.getX(), token.getY(), token.getZ(),
                SoundEvents.WOOL_BREAK, SoundSource.MASTER, 1, 1);
    }

    public static ItemStack bucket() {
        var result = new ItemStack(Items.WATER_BUCKET);
        var name = Component.literal("Enzan")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false));
        name.append(Component.literal(" Beta Server Harem event")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(false)));
        result.set(DataComponents.CUSTOM_NAME, name);
        return result;
    }
}
