package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Small source feedback drops; the shared dispatcher owns admission and the unit debit. */
public final class LbcFeedbackDrops {
    private final GameController game;

    public LbcFeedbackDrops(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token, LbcFeedbackDropItem.Kind kind) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcFeedbackDropItem.matches(token.getItem(), kind)
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags())
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        var pos = token.blockPosition();
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /** Filter before choosing the nearest player: an immune player does not consume the selection. */
    ServerPlayer target(ItemEntity token) {
        ServerPlayer nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        if (!(token.level() instanceof ServerLevel level)) return null;
        for (var player : level.players()) {
            if (!game.items().wands().validTarget(player) || player.entityTags().contains("sin_greed_active")
                    || player.level().dimension().identifier().toString().equals("minecraft:nexus")) continue;
            double candidate = player.distanceToSqr(token);
            if (candidate < distance) { distance = candidate; nearest = player; }
        }
        return nearest;
    }

    public boolean spawn(ItemEntity token, LbcFeedbackDropItem.Kind kind) {
        if (!ready(token, kind)) return false;
        var level = (ServerLevel) token.level();
        if (kind == LbcFeedbackDropItem.Kind.YAMETE) {
            var pos = token.position();
            level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 100, .5, .5, .5, 1);
            level.sendParticles(SpellParticleOption.create(ParticleTypes.EFFECT, -1, 1),
                    pos.x, pos.y, pos.z, 10, .5, .5, .5, 1);
            var sound = Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:lbcsounds.yametekudasai")));
            double range = sound.value().getRange(1);
            long seed = level.getRandom().nextLong();
            for (var viewer : level.players()) if (viewer.position().distanceToSqr(pos) <= range * range)
                viewer.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER,
                        pos.x, pos.y, pos.z, 1, 1, seed));
            return true;
        }
        var victim = target(token);
        if (victim == null) return true;
        // JSON decoding in the legacy title yields the single U+E203 glyph. Both sends are authored.
        victim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\uE203")));
        game.items().psycho().afflict(victim, LbcIndirectKills.playerSource(game.items().specialDrops().owner(token)), 0, 0, 1000, 0);
        var message = Component.empty().append(victim.getDisplayName().copy())
                .append(Component.translatable("died_from_cum").withStyle(ChatFormatting.DARK_RED));
        game.server().getPlayerList().broadcastSystemMessage(message, false);
        victim.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\uE203")));
        var sound = Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:lbcsounds.ultrascream")));
        Vec3 pos = victim.position();
        victim.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER,
                pos.x, pos.y, pos.z, 10_000_000, 1, level.getRandom().nextLong()));
        return true;
    }
}
