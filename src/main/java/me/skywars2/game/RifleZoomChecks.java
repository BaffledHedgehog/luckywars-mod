package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Explicit native input and post-effect lifecycle regression. No client rendering claim. */
public final class RifleZoomChecks {
    private static final Identifier ZOOM=Identifier.parse("lbc:zoom");
    private static final Identifier OTHER=Identifier.parse("minecraft:blur");
    private RifleZoomChecks() { }
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Rifle zoom: "+message);
    }
    private static ServerPlayer player(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null,"requires TestAlpha"); return player;
    }
    private static boolean zoomed(ServerPlayer player) { return player.getPostEffects().contains(ZOOM); }
    public static void run(GameController game) {
        var player=player(game); var zoom=game.items().rifleZoom();
        require(game.playing() && game.allowDamage(player),"requires active match participant");
        var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var effects=List.copyOf(player.getPostEffects());
        boolean hadStand=player.entityTags().remove("have_stand");
        try {
            zoom.forget(player); player.removePostEffect(ZOOM); player.addPostEffect(OTHER);
            player.getInventory().clearContent();
            for(String table:List.of("lbc:ported/slr","lbc:ported/magnum")) {
                var gun=game.lbc().rollTable(player.level(),player.position(),player,table).getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND,gun);
                player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
                require(zoomed(player),"native punch enables "+table);
                player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
                require(zoomed(player),"same-tick duplicate cannot flip zoom twice");
                int now=game.server().getTickCount();
                for(int tick=1;tick<=20;tick++) zoom.punch(player,now+tick);
                require(zoomed(player),"held mining packet stream stays enabled");
                zoom.punch(player,now+23);
                require(!zoomed(player) && player.getPostEffects().contains(OTHER),
                        "next separate click disables only zoom");
                zoom.punch(player,now+26);
                require(zoomed(player),"a later click enables again");
                player.setItemInHand(InteractionHand.MAIN_HAND,gun.copy()); zoom.tick();
                require(!zoomed(player),"replacing even an identical gun removes zoom");
                // Existing consumable/model components are not needed to enable a scope.
                require(gun.get(DataComponents.ITEM_MODEL)!=null,"native loot keeps its original model");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            zoom.punch(player);
            require(!zoomed(player),"unrelated main hand cannot enable zoom");
            player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcMagnum.ITEM));
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(zoomed(player),"rifle in offhand supports the same left-click toggle");
            player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY); zoom.tick();
            require(!zoomed(player),"unholding offhand rifle clears zoom");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.SLR));
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(zoomed(player),"native give prototype is supported");
            zoom.forget(player);
            require(!zoomed(player) && player.getPostEffects().contains(OTHER),"disconnect cleanup retains other effects");
            game.items().timeStop().begin(player.level(),40);
            require(game.items().actionsBlocked(player),"native time stop really blocks the fixture player");
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(!zoomed(player),"blocked input cannot enable zoom");
            game.items().timeStop().end(player.level());
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            require(zoomed(player),"unblocked input works again");
            game.items().timeStop().begin(player.level(),40); zoom.tick();
            require(!zoomed(player),"blocking actions also clears an existing scope");
            game.items().timeStop().end(player.level());
            player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
            float health=player.getHealth();
            player.setHealth(0); zoom.tick();
            require(!zoomed(player),"death clears existing zoom");
            player.setHealth(health);
            SkyWars2.LOGGER.info("[check] PASS rifle-zoom");
        } finally {
            zoom.forget(player); game.items().timeStop().end(player.level()); saved.restore(game);
            if(hadStand) player.entityTags().add("have_stand");
            player.clearPostEffects(); for(var effect:effects) player.addPostEffect(effect);
        }
    }
    public static void stopProbe(GameController game) {
        var player=player(game); game.items().rifleZoom().forget(player);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.SLR));
        player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
        require(zoomed(player),"stop probe enters with active scope");
        SkyWars2.LOGGER.info("[check] PASS rifle-zoom-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(game.inLobby() && !zoomed(player(game)),"actual match stop removes scope");
        SkyWars2.LOGGER.info("[check] PASS rifle-zoom-cleanup");
    }
}
