package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.skywars2.mixin.PlayerCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Native player-body component. Full Vanity still requires world/core/actor/queue coordination. */
public final class MatchPlayerCheckpoint {
    private record Restore(GameController game,UUID id) { }
    private static final ThreadLocal<Restore> RESTORING=new ThreadLocal<>();
    public static boolean restoring(ServerPlayer player) {
        var current=RESTORING.get();return current!=null&&current.game.server()==player.level().getServer()&&current.id.equals(player.getUUID());
    }

    private final GameController game;
    private final String epoch;
    private final UUID id;
    private final ServerLevel level;
    private final CompoundTag nbt;
    private final Vec3 position,motion;
    private final float yaw,pitch;
    private final net.minecraft.world.entity.player.Input input;
    private final List<ItemStack> crafting=new ArrayList<>();
    private final ItemStack cursor;
    private final MatchEntityState body;
    private final MatchPlayerProgress progress;
    private final Map<Identifier,Object> cooldowns;
    private final int cooldownTick;

    MatchPlayerCheckpoint(GameController game,ServerPlayer player) {
        if(!game.server().isSameThread()||game.server().getPlayerList().getPlayer(player.getUUID())!=player||!game.playing())
            throw new IllegalStateException("Capture a current match player on the native server thread");
        MatchPlayerViews.validateMenu(player);
        this.game=game;id=player.getUUID();epoch=game.items().epoch();level=player.level();
        body=new MatchEntityState(game,player);nbt=body.nbt;
        position=body.position;motion=body.motion;yaw=body.yaw;pitch=body.pitch;input=player.getLastClientInput();
        for(int n=0;n<player.inventoryMenu.getCraftSlots().getContainerSize();n++)crafting.add(player.inventoryMenu.getCraftSlots().getItem(n).copy());
        cursor=player.inventoryMenu.getCarried().copy();
        progress=new MatchPlayerProgress(player);
        var cd=(PlayerCheckpointAccess.Cooldowns)player.getCooldowns();cooldownTick=cd.skywars2$tick();cooldowns=new LinkedHashMap<>(cd.skywars2$cooldowns());
    }

    UUID id() { return id; }
    ServerPlayer restoreFrozen() {
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen()||!game.playing()||!game.items().currentEpoch(epoch)||game.maps().busy())
            throw new IllegalStateException("Restore the original match on a frozen native server thread");
        var player=game.server().getPlayerList().getPlayer(id);
        if(player==null)throw new IllegalStateException("Player is offline; retain its image for the full owner's login phase");
        if(!level.hasChunkAt(BlockPos.containing(position)))throw new IllegalStateException("Saved player destination is not resident");
        var previous=RESTORING.get();RESTORING.set(new Restore(game,id));
        try {
            MatchPlayerViews.closeFutureStorage(player);
            // Personal future buffers are discarded, not dropped by native menu close.
            // Supported storage cursors belong to the earlier view image/close phase.
            player.containerMenu.setCarried(ItemStack.EMPTY);player.inventoryMenu.getCraftSlots().clearContent();
            if(player.isDeadOrDying()&&nbt.getFloatOr("Health",0)>0) {
                player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                player=game.server().getPlayerList().getPlayer(id);
                if(player==null||player.isDeadOrDying())throw new IllegalStateException("Native respawn did not provide a living replacement");
            }
            player.closeContainer();player.stopRiding();player.setCamera(player);player.stopUsingItem();
            player.getPostEffects().clear();
            if(!player.teleportTo(level,position.x,position.y,position.z,java.util.Set.of(),yaw,pitch,true))throw new IllegalStateException("Native player dimension transfer failed");
            body.load(player);
            var recipe=nbt.read("recipeBook",net.minecraft.stats.ServerRecipeBook.Packed.CODEC).orElseThrow();
            ((PlayerCheckpointAccess.Recipes)player.getRecipeBook()).skywars2$apply(recipe);
            for(int n=0;n<crafting.size();n++)player.inventoryMenu.getCraftSlots().setItem(n,crafting.get(n).copy());
            player.inventoryMenu.setCarried(cursor.copy());
            var cd=(PlayerCheckpointAccess.Cooldowns)player.getCooldowns();
            for(var group:List.copyOf(cd.skywars2$cooldowns().keySet()))player.getCooldowns().removeCooldown(group);
            cd.skywars2$tick(cooldownTick);cd.skywars2$cooldowns().putAll(cooldowns);
            for(var entry:cooldowns.entrySet())cd.skywars2$publish(entry.getKey(),((PlayerCheckpointAccess.Cooldown)entry.getValue()).skywars2$end()-cooldownTick);
            player.refreshDimensions();player.setLastClientInput(input);
            player.setDeltaMovement(motion);player.needsSync=true;
            // Native teleport establishes listener/chunk/position bookkeeping. Explicit
            // motion packets restore exact velocity, including values NBT.load caps at 10.
            player.connection.teleport(position.x,position.y,position.z,yaw,pitch);
            // Native teleport resets the head rotation and previous pose/rotation values.
            body.finishRuntime(player);
            player.setDeltaMovement(motion);
            progress.restore(player);sync(player);game.cancelQueuedEntityUnload(player);
            return player;
        } finally {if(previous==null)RESTORING.remove();else RESTORING.set(previous);}
    }

    private void sync(ServerPlayer player) {
        player.resetSentInfo();player.onUpdateAbilities();player.sendPostEffects();
        player.connection.send(new ClientboundSetHealthPacket(player.getHealth(),player.getFoodData().getFoodLevel(),player.getFoodData().getSaturationLevel()));
        player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress,player.totalExperience,player.experienceLevel));
        player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
        player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(),player.getAttributes().getSyncableAttributes()));
        for(var effect:player.getActiveEffects())player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(),effect,false));
        var data=new ClientboundSetEntityDataPacket(player.getId(),body.metadata());
        player.connection.send(data);level.getChunkSource().sendToTrackingPlayers(player,data);
        var velocity=new ClientboundSetEntityMotionPacket(player);player.connection.send(velocity);level.getChunkSource().sendToTrackingPlayers(player,velocity);
        player.inventoryMenu.sendAllDataToRemote();player.getRecipeBook().sendInitialRecipeBook(player);
        FullInvisibility.effectsChanged(player);SignedEffectVisuals.joined(player);
    }
}
