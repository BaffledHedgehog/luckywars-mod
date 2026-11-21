package me.skywars2.game;

import io.netty.buffer.Unpooled;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;

/** Decoded native TAB cache compared with native chat binding across team and round changes. */
public final class PlayerNameChecks {
    private PlayerNameChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Player names: "+why);
    }
    public static void run(GameController game) {
        require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;
        try {f.begin();} catch(Throwable error) {running=null;f.close();throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var f=running;
        try {
            if(!f.step())return;running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS player-names (decoded native TAB/chat prefix, color and suffix parity; coalescing; mutable translated prefix; round reroll, lobby restore, new listener)");
        } catch(Throwable error) {
            running=null;try {f.close();} catch(Throwable cleanup) {error.addSuppressed(cleanup);}
            SkyWars2.LOGGER.error("[check] FAIL player-names",error);
        }
    }
    private static final class Client {
        final GameController game;
        final RegenerationBeltChecks.ScheduledClient wire;
        final Map<UUID,Component> names=new HashMap<>();
        final Set<UUID> changed=new HashSet<>();
        int cursor,updates;
        Client(GameController game,RegenerationBeltChecks.ScheduledClient wire) {this.game=game;this.wire=wire;}
        RegistryFriendlyByteBuf buffer() {return new RegistryFriendlyByteBuf(Unpooled.buffer(),game.server().registryAccess());}
        void receive() {wire.acknowledge();var packets=wire.packets();while(cursor<packets.size())apply(packets.get(cursor++));}
        void apply(Packet<?> packet) {
            if(packet instanceof ClientboundBundlePacket bundle) {for(var child:bundle.subPackets())apply(child);return;}
            if(!(packet instanceof ClientboundPlayerInfoUpdatePacket update)
                    ||!update.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME))return;
            var buffer=buffer();
            try {
                ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.encode(buffer,update);
                var decoded=ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.decode(buffer);
                updates++;
                for(var entry:decoded.entries()) {names.put(entry.profileId(),entry.displayName());changed.add(entry.profileId());}
            } finally {buffer.release();}
        }
        Component chat(ServerPlayer player) {
            var bound=ChatType.bind(ResourceKey.create(Registries.CHAT_TYPE,Identifier.parse("minecraft:chat")),player);
            var buffer=buffer();
            try {ChatType.Bound.STREAM_CODEC.encode(buffer,bound);return ChatType.Bound.STREAM_CODEC.decode(buffer).name();}
            finally {buffer.release();}
        }
        void same(ServerPlayer player) {
            var cached=names.get(player.getUUID());var current=chat(player);
            require(current.equals(cached),"client TAB differs from native chat for "+player.getScoreboardName()
                    +"; cached="+(cached==null?"null":cached.getString())+" chat="+current.getString()+" updates="+updates);
        }
        void clear() {updates=0;changed.clear();}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture base;
        final Client self,audience;
        final LbcTeamColors rounds;
        final MatchRandomState random;
        PlayerTeam lobby;
        MutableComponent argument;
        Component translated;
        int phase,when;
        boolean closed;
        Fixture(GameController game) {
            this.game=game;base=new WoolBucketChecks.Fixture(game,false);
            self=new Client(game,base.client);audience=new Client(game,base.audience);
            rounds=new LbcTeamColors(game.server().getScoreboard());random=MatchRandomState.capture(base.level.getRandom());
        }
        List<ServerPlayer> players() {return List.of(base.owner,base.other);}
        void receive() {self.receive();audience.receive();}
        void clear() {receive();self.clear();audience.clear();}
        void same() {receive();for(var p:players()) {self.same(p);audience.same(p);}}
        void next(int value) {phase=value;when=game.server().getTickCount();}
        void begin() {
            base.prepareParticipants();
            var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_names_lobby")==null,"exclusive lobby team name");
            lobby=board.addPlayerTeam("sw2_names_lobby");lobby.setPlayerPrefix(Component.literal("Lobby "));
            lobby.setPlayerSuffix(Component.literal(" [native]"));lobby.setColor(Optional.of(TeamColor.GREEN));
            for(var p:players()) {board.addPlayerToTeam(p.getScoreboardName(),lobby);var profile=game.players().get(p.getUUID());profile.suffix=17;profile.legacyUnlocks.add("swrg:rewards/suffix_17");}
            game.items().cosmetics().refreshSuffixes(players());same();clear();
            lobby.setPlayerPrefix(Component.literal("Changed "));lobby.setPlayerSuffix(Component.literal(" [new]"));lobby.setColor(Optional.of(TeamColor.BLUE));
            next(0);
        }
        boolean ready() {return game.server().getTickCount()-when>=2;}
        void oneUpdate() {
            same();
            require(self.updates==1&&audience.updates==1,"one coalesced native update for a team mutation burst: "+self.updates+"/"+audience.updates);
            require(self.changed.containsAll(players().stream().map(ServerPlayer::getUUID).toList()),"both teammates updated in same batch");
        }
        boolean step() {
            receive();if(!ready())return false;
            switch(phase) {
                case 0 -> {oneUpdate();clear();next(1);}
                case 1 -> {
                    same();require(self.updates==0&&audience.updates==0,"unchanged names produce no repeating packets");
                    argument=Component.literal("nested");translated=Component.translatableWithFallback("skywars2.name_fixture","[%s] ",argument);
                    lobby.setPlayerPrefix(translated);clear();next(2);
                }
                case 2 -> {
                    oneUpdate();clear();
                    var queued=new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),players());
                    String expected=queued.entries().stream().filter(e->e.profileId().equals(base.owner.getUUID())).findFirst().orElseThrow().displayName().getString();
                    argument.append("!");lobby.setPlayerPrefix(translated);
                    var delayed=new Client(game,base.client);delayed.apply(queued);
                    require(delayed.names.get(base.owner.getUUID()).getString().equals(expected),"native packet encoded after prefix mutation retains its original snapshot");
                    next(3);
                }
                case 3 -> {
                    oneUpdate();clear();game.config().prefixes=0;rounds.begin(players());
                    for(var p:players())require(!rounds.assigned(p).getPlayerPrefix().getString().isEmpty(),"first round selects actual source prefix");
                    next(4);
                }
                case 4 -> {
                    oneUpdate();clear();for(var p:players())rounds.assigned(p).setPlayerPrefix(Component.literal("STALE_ROUND "));
                    rounds.stop();next(5);
                }
                case 5 -> {
                    oneUpdate();require(base.owner.getTeam()==lobby&&base.other.getTeam()==lobby,"native original lobby memberships restored");
                    clear();rounds.begin(players());
                    for(var p:players())require(!rounds.assigned(p).getPlayerPrefix().getString().contains("STALE_ROUND")&&!rounds.assigned(p).getPlayerPrefix().getString().isEmpty(),"next round regenerates source prefix");
                    next(6);
                }
                case 6 -> {oneUpdate();clear();rounds.stop();next(7);}
                case 7 -> {
                    oneUpdate();clear();game.players().get(base.owner.getUUID()).suffix=0;
                    game.items().cosmetics().refreshSuffixes(List.of(base.owner));same();
                    require(!self.changed.contains(base.other.getUUID())&&self.changed.contains(base.owner.getUUID()),"personal suffix selection updates only its owner");
                    next(8);
                }
                case 8 -> {
                    same();
                    try(var reconnected=new RegenerationBeltChecks.ScheduledClient(game,base.other)) {
                        var fresh=new Client(game,reconnected);
                        reconnected.listener.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(game.server().getPlayerList().getPlayers()));
                        fresh.receive();for(var p:players())fresh.same(p);
                    }
                    return true;
                }
                default -> throw new IllegalStateException("unknown name phase");
            }
            return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;
            try {rounds.stop();if(lobby!=null&&game.server().getScoreboard().getPlayerTeam(lobby.getName())==lobby)game.server().getScoreboard().removePlayerTeam(lobby);random.restore(base.level.getRandom());}
            finally {base.close();}
        }
    }
}
