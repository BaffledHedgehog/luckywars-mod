package me.skywars2.game;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.genie.GenieScriptItem;
import me.skywars2.game.genie.script.ScriptRepository;
import me.skywars2.mixin.ActorCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A real lobby transition with visible and registered-hidden gift bodies in the End. */
final class GenieLobbyChecks {
    private static Fixture active;

    static void run(GameController game) {
        require(active==null,"exclusive Genie lobby fixture");
        var fixture=new Fixture(game);
        try { fixture.begin(); active=fixture; }
        catch(Throwable error) { fixture.close(); throw error; }
    }

    static void tick(GameController game) {
        var fixture=active;
        if(fixture==null||fixture.game!=game)return;
        try {
            if(!fixture.step())return;
            active=null;fixture.close();
            SkyWars2.LOGGER.info("[check] PASS genie-lobby (native gift use, visible and registered-hidden End cows, canonical lobby teardown, ordinary control survives targeted stop, cold column untouched)");
        } catch(Throwable error) {
            active=null;
            try { fixture.close(); } catch(Throwable cleanup) { error.addSuppressed(cleanup); }
            SkyWars2.LOGGER.error("[check] FAIL genie-lobby",error);
        }
    }

    private static void require(boolean test,String message) {
        if(!test)throw new IllegalStateException("Genie lobby: "+message);
    }

    private static final class Fixture {
        private static final Vec3 ORIGIN=new Vec3(.5,160,.5);
        private static final Vec3 COLD=new Vec3(2304.5,160,2304.5);
        final GameController game;
        final ServerPlayer owner;
        final ServerLevel end;
        final GameChecks.WandThirdPlayerState saved;
        final RegenerationBeltChecks.ScheduledClient client;
        final CompletableFuture<ScriptRepository.Loaded> script;
        final int start;
        final boolean noGravity;
        Cow visible,hidden,ordinary;
        ItemStack gift;
        int phase,hiddenAt;
        boolean ownWarmTicket;

        Fixture(GameController game) {
            this.game=game;
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            end=Objects.requireNonNull(game.server().getLevel(Level.END));
            require(game.playing()&&game.canUseGameplayItems(owner)&&!game.maps().busy()
                    &&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"a settled actual match");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            noGravity=owner.isNoGravity();
            start=game.server().getTickCount();
            script=game.items().genie().runtime().scripts().prepare("""
                    {"version":1,"state":{},"handlers":{"USE_REPLACE":"launch"},"functions":{"launch":{
                    "parameters":["event"],"code":[
                    ["load","state"],["const","visible"],["const","minecraft:cow"],
                    ["const",[2.5,160,0.5]],["const",[0,0,0]],["const",600],["call","entity.launch",4],["put"],["drop"],
                    ["load","state"],["const","hidden"],["const","minecraft:cow"],
                    ["const",[4.5,160,0.5]],["const",[0,0,0]],["const",600],["call","entity.launch",4],["put"],["return"]]}}}
                    """);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);
        }

        void begin() {
            // Keep the ordinary control loaded after the lobby teleports the player away.
            if(!end.getForceLoadedChunks().contains(ChunkPos.pack(0,0))){
                end.setChunkForced(0,0,true);ownWarmTicket=true;
            }
            owner.closeContainer();owner.stopUsingItem();owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);
            require(owner.teleportTo(end,ORIGIN.x,ORIGIN.y,ORIGIN.z,Set.of(),0,0,true),"native transfer to End");
            client.acknowledge();
        }

        boolean registered(Entity entity) {
            return ((ActorCheckpointAccess.LevelEntities)end).skywars2$manager().isLoaded(entity.getUUID());
        }

        boolean cold() {
            var pos=BlockPos.containing(COLD);
            int x=pos.getX()>>4,z=pos.getZ()>>4;
            return end.getChunkSource().getChunkNow(x,z)==null&&!end.areEntitiesLoaded(ChunkPos.pack(x,z));
        }

        Cow launched(com.google.gson.JsonObject state,String key) {
            require(state.has(key)&&!state.get(key).isJsonNull(),"script published the "+key+" actor: "+state);
            var actor=end.getEntity(UUID.fromString(state.get(key).getAsString()));
            require(actor instanceof Cow && registered(actor) && game.items().currentEpoch(actor),"native registered gift cow: "+key);
            var source=game.items().spellSources().source(actor);
            require(source!=null&&source.emitter().equals(actor.getUUID())&&source.author().equals(owner.getUUID()),
                    "real gift cause and physical emitter: "+key);
            var cow=(Cow)actor;cow.setNoAi(true);cow.setNoGravity(true);
            return cow;
        }

        boolean step() {
            int now=game.server().getTickCount();
            require(now-start<400,"bounded native transition, phase="+phase+" stage="+game.stage());
            if(phase==0){
                require(owner.level()==end&&game.canUseGameplayItems(owner),"gift remains usable in actual End");
                if(!script.isDone()||end.getChunkSource().getChunkNow(0,0)==null
                        ||!end.areEntitiesLoaded(ChunkPos.pack(0,0)))return false;
                gift=GenieScriptItem.construct(script.join(),"crossbow","End herd","Launches native cows.",1);
                owner.setItemInHand(InteractionHand.MAIN_HAND,gift);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
                owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,21700,0,0));
                phase=1;return false;
            }
            if(phase==1){
                var state=GenieScriptItem.state(gift);
                if(!state.has("hidden"))return false;
                visible=launched(state,"visible");hidden=launched(state,"hidden");
                require(visible!=hidden&&cold(),"independent gift actors and genuinely cold native section");
                ordinary=Objects.requireNonNull(EntityTypes.COW.create(end,EntitySpawnReason.TRIGGERED));
                ordinary.setPos(6.5,160,.5);ordinary.setNoAi(true);ordinary.setNoGravity(true);
                require(end.addFreshEntity(ordinary)&&registered(ordinary)&&end.getEntity(ordinary.getUUID())==ordinary,
                        "same-type ordinary native control in End");
                hidden.setPos(COLD);hiddenAt=now;phase=2;return false;
            }
            if(phase==2){
                if(now-hiddenAt<2)return false;
                require(registered(hidden)&&end.getEntity(hidden.getUUID())==null&&!hidden.isRemoved()&&cold(),
                        "accepted gift remains natively registered after its visibility-only unload in a cold column");
                require(end.getEntity(visible.getUUID())==visible&&!visible.isRemoved()&&visible.tickCount<600,
                        "visible gift is still alive before the original lifetime expires");
                game.stopMatch();
                require(!game.playing(),"canonical stop left the active match");
                phase=3;
                for(var cow:java.util.List.of(visible,hidden))require(cow.getRemovalReason()==Entity.RemovalReason.DISCARDED&&!registered(cow),
                        "canonical stop discards offworld gift before lobby publication: "+cow.getUUID()+" reason="+cow.getRemovalReason());
                require(!ordinary.isRemoved()&&registered(ordinary),"ordinary End entity survives the scoped Genie retirement");
                return false;
            }
            if(!game.inLobby())return false;
            require(owner.level()==game.maps().level()&&!registered(visible)&&!registered(hidden)&&cold(),
                    "real lobby publication does not revive either gift or load the cold column");
            return true;
        }

        void close() {
            for(var entity:new Entity[]{visible,hidden,ordinary})if(entity!=null&&!entity.isRemoved())entity.discard();
            try { if(phase<3&&game.playing())saved.restore(game);else owner.setNoGravity(noGravity); }
            finally {
                if(ownWarmTicket)end.setChunkForced(0,0,false);
                client.close();
            }
        }
    }
}
