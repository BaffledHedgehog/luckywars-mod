package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ScoreboardCheckpointAccess;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Actual extra sessions, death/replacement, operator enrollment and disconnect/login. */
public final class VanityCoreChecks {
    private VanityCoreChecks() { }
    private static Fixture running;
    private static void require(boolean value,String why) { if(!value)throw new IllegalStateException("Vanity core: "+why); }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final MatchCoreCheckpoint original;
        final ChefChecks.Fixture base;
        final UUID gammaId=GameChecks.offlineUuid("TestGamma"),deltaId=GameChecks.offlineUuid("TestDelta");
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<String> alphaTags,betaTags;
        final boolean wasFrozen;
        final ServerGamePacketListenerImpl alphaConnection;
        final ElementalGrimoireChecks.Recording recording;
        final ZoneBarrier.Checkpoint originalZone;
        MatchCoreCheckpoint image;
        ServerPlayer gamma,delta;
        PlayerStore.Profile profile,alphaProfile;
        int[] keys,levelups,trinkets;
        Set<String> unlocks;
        Objective objective;
        PlayerTeam team;
        ScoreAccess cached;
        Object nativeInfo,nativeScore;
        int tick,phase=-1,matchTick,coins,kills,deaths;
        int savedHealth;
        String savedMap;
        boolean savedHalloween;
        boolean gammaOwned,deltaOwned,closed;
        ItemStack root;

        Fixture(GameController game) {
            this.game=game;
            require(game.playing()&&!game.maps().busy()&&!game.zoneBarrier().active()&&game.items().witchClaims().ownership().isEmpty(),"exclusive stable core fixture");
            require(game.server().getPlayerList().getPlayer(gammaId)==null&&game.server().getPlayerList().getPlayer(deltaId)==null,"never borrow extra sessions");
            wasFrozen=game.server().tickRateManager().isFrozen();require(!wasFrozen,"fixture owns native freeze");
            original=new MatchCoreCheckpoint(game);base=new ChefChecks.Fixture(game);
            originalZone=game.zoneBarrier().checkpoint();
            alphaConnection=base.owner.connection;recording=new ElementalGrimoireChecks.Recording(base.owner);
            alphaTags=new HashSet<>(base.owner.entityTags());betaTags=new HashSet<>(base.other.entityTags());
            for(var entity:base.owner.level().getAllEntities())before.add(entity);
        }
        void init() {
            base.init();base.owner.connection=new ServerGamePacketListenerImpl(game.server(),recording,base.owner,
                    new CommonListenerCookie(base.owner.getGameProfile(),0,base.owner.clientInformation(),false));
            base.owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            gamma=GameChecks.createBot(game,"TestGamma");gammaOwned=true;
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
            SkyWars2.LOGGER.info("[check] START vanity-core");
        }
        void ready() {
            require(game.allowDamage(gamma)&&game.lives(gammaId)==1&&game.alivePlayers().size()==3,"native operator enrollment creates an actual third participant");
            base.move(gamma,base.at);gamma.setNoGravity(true);gamma.removeAllEffects();gamma.getInventory().clearContent();gamma.setShiftKeyDown(false);
            // This is a core checkpoint fixture, not random Wrath conversion/death proof.
            // Keep borrowed players outside its 30-block conversion radius during real ticks.
            base.move(base.owner,base.at.add(-40,0,-40));base.move(base.other,base.at.add(40,0,-40));
            root=game.lbc().rollTable(gamma.level(),gamma.position(),gamma,"lbc:witch_gens").stream().filter(LbcWitchClaims::matches).findFirst().orElseThrow();
            claim(gamma,5000);
            profile=game.players().get(gammaId);profile.coins=113;profile.keys[2]=4;profile.levelups[0]=12;profile.trinkets[0]=8;
            profile.lbcUnlocks.add("lbc:true_advancements/checkpoint_test");
            keys=profile.keys;levelups=profile.levelups;trinkets=profile.trinkets;unlocks=profile.lbcUnlocks;
            alphaProfile=game.players().get(base.owner.getUUID());coins=alphaProfile.coins;kills=alphaProfile.kills;deaths=profile.deaths;
            var board=game.server().getScoreboard();
            require(board.getObjective("vanity_core_keep")==null&&board.getPlayerTeam("vanity_core_team")==null,"owned native scoreboard names");
            objective=board.addObjective("vanity_core_keep",ObjectiveCriteria.TRIGGER,Component.literal("Saved objective"),ObjectiveCriteria.RenderType.INTEGER,false,null);
            cached=board.getOrCreatePlayerScore(gamma,objective);cached.set(73);cached.unlock();cached.display(Component.literal("Saved score"));cached.numberFormatOverride(new FixedFormat(Component.literal("Saved format")));
            board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("#saved-fake-holder"),objective).set(Integer.MIN_VALUE);
            board.setDisplayObjective(DisplaySlot.BELOW_NAME,objective);
            nativeInfo=((ScoreboardCheckpointAccess)board).skywars2$players().get(gamma.getScoreboardName());
            nativeScore=((ScoreboardCheckpointAccess.Player)nativeInfo).skywars2$scores().get(objective);
            team=board.addPlayerTeam("vanity_core_team");team.setAllowFriendlyFire(false);team.setCollisionRule(Team.CollisionRule.NEVER);
            team.setPlayerPrefix(Component.literal("Saved prefix"));board.addPlayerToTeam(gamma.getScoreboardName(),team);
            game.zoneBarrier().start(base.at.x,base.at.z,80,20,600);
            savedHealth=game.config().health;savedMap=game.config().mapId;savedHalloween=game.config().eventHalloween;
            image=new MatchCoreCheckpoint(game);matchTick=game.matchTicks();
            boolean rejected=false;
            try{image.restoreFrozen();}catch(IllegalStateException expected){rejected=true;}
            require(rejected&&cached.get()==73&&profile.coins==113,"live restoration rejected before changing any component");
            game.server().tickRateManager().setFrozen(true);phase=0;
        }
        void claim(ServerPlayer player,int progress) {
            player.setItemInHand(InteractionHand.MAIN_HAND,root.copyWithCount(1));player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            game.items().mana().score(player,"gens_type").set(2);game.items().mana().score(player,"dmg_dealt").set(progress);
            require(net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(player,player.level(),InteractionHand.MAIN_HAND).consumesAction()
                    &&player.getMainHandItem().isEmpty(),"real source-qualified authority claim");
        }
        void killAndReplace() {
            var previous=gamma;gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;
            require(gamma.hurtServer(gamma.level(),gamma.damageSources().playerAttack(base.owner),10000)&&gamma.isDeadOrDying(),"real native attributed death");
            require(base.owner.isAlive()&&base.other.isAlive(),"owned Wrath death cannot consume borrowed fixture players");
            require(game.lives(gammaId)==0&&game.awaitingRespawn(gammaId)&&profile.deaths==deaths+1
                    &&alphaProfile.kills==kills+1&&alphaProfile.coins==coins+50,"actual core elimination and kill rewards changed after checkpoint");
            gamma.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            gamma=game.server().getPlayerList().getPlayer(gammaId);gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            require(gamma!=previous&&gamma.isAlive()&&game.isObserver(gamma),"real replacement player is eliminated before core restoration");
            gamma.setNoGravity(true);base.move(gamma,base.at);
        }
        void mutate() {
            profile.coins=999;keys[2]=55;levelups[0]=77;trinkets[0]=66;unlocks.clear();
            profile.keys=new int[]{99};profile.levelups=new int[]{99};profile.trinkets=new int[]{99};profile.lbcUnlocks=new HashSet<>(Set.of("future"));
            game.config().health=57;game.config().mapId="64";game.config().eventHalloween=!game.config().eventHalloween;
            game.events().restoreTimeline(new MatchEvents.Timeline(false,12345,98765));
            game.luckyModifiers().endMatch();game.luckyModifiers().commit(7);game.zoneBarrier().stop();
            var board=game.server().getScoreboard();cached.set(999);cached.lock();
            board.removeObjective(objective);
            var future=board.addObjective("vanity_core_keep",ObjectiveCriteria.DUMMY,Component.literal("Future objective"),ObjectiveCriteria.RenderType.INTEGER,true,null);
            board.getOrCreatePlayerScore(gamma,future).set(888);board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("#future-holder"),future).set(999);
            board.removePlayerTeam(team);var futureTeam=board.addPlayerTeam("vanity_core_team");futureTeam.setAllowFriendlyFire(true);board.addPlayerToTeam(gamma.getScoreboardName(),futureTeam);
            claim(base.owner,6000);
        }
        void verify() {
            require(game.lives(gammaId)==1&&!game.awaitingRespawn(gammaId)&&game.allowDamage(gamma)&&!game.isObserver(gamma)
                    &&gamma.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&game.alivePlayers().size()==3,"captured participant lives and role follow the UUID to the native replacement body");
            require(game.isObserver(delta)&&!game.allowDamage(delta)&&game.lives(deltaId)==0,"newcomer loses later enrollment and becomes an observer");
            require(game.matchTicks()==matchTick&&game.playing(),"match timeline and active stage restored without start/kit reroll");
            require(game.config().health==savedHealth&&game.config().mapId.equals(savedMap)&&game.config().eventHalloween==savedHalloween
                    &&game.selectedMapReady(),"actual settings edits rewind without losing prepared map readiness");
            require(profile==game.players().get(gammaId)&&profile.keys==keys&&profile.levelups==levelups&&profile.trinkets==trinkets&&profile.lbcUnlocks==unlocks
                    &&keys[2]==4&&levelups[0]==12&&trinkets[0]==8&&unlocks.equals(Set.of("lbc:true_advancements/checkpoint_test"))&&profile.coins==113,
                    "profile, replaced arrays and set restore original values AND identities");
            require(profile.deaths==deaths&&alphaProfile.kills==kills&&alphaProfile.coins==coins,"post-checkpoint kill, death and coin rewards rewind");
            var board=game.server().getScoreboard();
            require(board.getObjective("vanity_core_keep")==objective&&board.getPlayerTeam("vanity_core_team")==team&&gamma.getTeam()==team
                    &&team.getCollisionRule()==Team.CollisionRule.NEVER&&!team.isAllowFriendlyFire(),"original objective/team identities, metadata and actual native membership restored");
            require(cached.get()==73&&!cached.locked()&&cached.display().getString().equals("Saved score")
                    &&board.getPlayerScoreInfo(gamma,objective).numberFormat().format(73).getString().equals("Saved format")
                    &&board.getPlayerScoreInfo(ScoreHolder.forNameOnly("#saved-fake-holder"),objective).value()==Integer.MIN_VALUE
                    &&board.getPlayerScoreInfo(ScoreHolder.forNameOnly("#future-holder"),objective)==null,"cached native ScoreAccess and signed fake-player values rewind; future holder gone");
            var currentInfo=((ScoreboardCheckpointAccess)board).skywars2$players().get(gamma.getScoreboardName());
            require(currentInfo==nativeInfo&&((ScoreboardCheckpointAccess.Player)currentInfo).skywars2$scores().get(objective)==nativeScore,
                    "original native PlayerScores and Score handles remain valid");
            require(board.getDisplayObjective(DisplaySlot.BELOW_NAME)==objective&&game.zoneBarrier().active()&&Math.abs(game.zoneBarrier().radius()-80)<.001,
                    "native display slot and zone analytical timeline/visual restored");
            require(game.items().witchClaims().ownership().get(LbcWitchClaims.Mode.WRATH).equals(gammaId),"canonical authority UUID rewinds in core phase; native tags belong to later body phase");
            require(recording.packets.stream().anyMatch(p->p instanceof ClientboundSetScorePacket score&&score.owner().equals("TestGamma")
                    &&score.objectiveName().equals("vanity_core_keep")&&score.score()==73&&score.numberFormat().orElseThrow().format(73).getString().equals("Saved format"))
                    &&recording.packets.stream().anyMatch(p->p instanceof ClientboundSetPlayerTeamPacket t&&t.getName().equals("vanity_core_team")&&t.getParameters().isPresent()),
                    "native scoreboard/team publishers send restored values to another actual connected participant");
            boolean removed=false,added=false;
            for(var packet:recording.packets)if(packet instanceof ClientboundSetPlayerTeamPacket t&&t.getName().equals("vanity_core_team")) {
                if(t.getTeamAction()==ClientboundSetPlayerTeamPacket.Action.REMOVE){removed=true;added=false;}
                else if(t.getTeamAction()==ClientboundSetPlayerTeamPacket.Action.ADD)added=true;
                else if(t.getParameters().isPresent())require(!removed||added,"native team ADD precedes MODIFY after removal");
            }
            cached.set(74);require(board.getPlayerScoreInfo(gamma,objective).value()==74,"old cached ScoreAccess still writes to the LIVE restored native scoreboard");
            image.restoreFrozen();require(cached.get()==73,"repeat restore cannot mutate saved scoreboard/profile values");
        }
        boolean step() {
            require(++tick<120,"bounded core fixture phase="+phase);
            if(phase==-1){ready();return false;}
            if(phase==0) {
                killAndReplace();delta=GameChecks.createBot(game,"TestDelta");deltaOwned=true;
                game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestDelta");
                require(game.allowDamage(delta),"actual late operator enrollment before rewind");
                mutate();recording.packets.clear();image.restoreFrozen();verify();
                // Prepare the separate body-tag phase explicitly before letting the
                // normal shared runners proceed; this is not native-body rewind proof.
                base.owner.entityTags().remove("sin_wrath");gamma.entityTags().add("sin_wrath");
                game.server().tickRateManager().setFrozen(false);phase=1;return false;
            }
            if(phase==1) {
                if(game.matchTicks()-matchTick<4)return false;
                game.server().tickRateManager().setFrozen(true);
                // Body phase is not implemented here; source tag is explicitly prepared for
                // this second actual-death control, without presenting it as body restoration.
                base.owner.entityTags().remove("sin_wrath");gamma.entityTags().add("sin_wrath");
                killAndReplace();recording.packets.clear();image.restoreFrozen();verify();
                gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity core fixture")));
                require(game.server().getPlayerList().getPlayer(gammaId)==null&&game.lives(gammaId)==0,"actual disconnect changes core membership");
                image.restoreFrozen();require(game.lives(gammaId)==1&&!game.awaitingRespawn(gammaId)&&game.alivePlayers().size()==2,
                        "captured offline UUID retains checkpoint lives without pretending an absent body is online");
                gamma=GameChecks.createBot(game,"TestGamma");require(game.isObserver(gamma),"normal login remains observer until full restore coordinator runs");
                image.restoreFrozen();verify();return true;
            }
            return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;
            game.server().tickRateManager().setFrozen(true);
            try {
                for(var id:List.of(deltaId,gammaId)) {
                    boolean owned=id.equals(gammaId)?gammaOwned:deltaOwned;
                    var player=game.server().getPlayerList().getPlayer(id);
                    if(owned&&player!=null)player.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity core fixture cleanup")));
                }
                for(var entity:new ArrayList<>(java.util.stream.StreamSupport.stream(base.owner.level().getAllEntities().spliterator(),false).toList()))
                    if(!before.contains(entity)&&(entity instanceof ItemEntity||entity instanceof ExperienceOrb))entity.discard();
                base.owner.connection=alphaConnection;base.close();original.restoreFrozen();
                require(game.zoneBarrier().checkpoint().equals(originalZone),"inactive zone counters also restored exactly during cleanup");
                base.owner.entityTags().clear();base.owner.entityTags().addAll(alphaTags);
                base.other.entityTags().clear();base.other.entityTags().addAll(betaTags);
            } finally {base.owner.connection=alphaConnection;game.server().tickRateManager().setFrozen(wasFrozen);}
        }
    }

    public static void run(GameController game) {
        require(running==null,"exclusive fixture");running=new Fixture(game);
        try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;
            SkyWars2.LOGGER.info("[check] PASS vanity-core (native death/replacement, UUID lives/roles and offline/reconnect core phase, newcomer spectator, exact settings/profiles and original array/set identities, original Objective/Team/PlayerScores/Score/ScoreAccess identities, fake scores/lock/format/display, zone and authority ownership, repeated restore; native bodies/private queues remain gated)");
        }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-core",error);}
    }
}
