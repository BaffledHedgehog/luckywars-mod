package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Destructive fresh-round scene, invoked only on the disposable native qualification server. */
public final class CunnyRestartChecks {
    private CunnyRestartChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why) {
        if (!value) throw new IllegalStateException("Cunny restart: " + why);
    }
    public static void run(GameController game) {
        run(game,false);
    }
    /** Reproduce a queued lobby start winning the selected-map callback before the Cunny driver ticks. */
    public static void runQueuedStart(GameController game) {
        run(game,true);
    }
    private static void run(GameController game,boolean queueDuringPreparation) {
        require(running == null,"exclusive fixture");
        var fixture = new Fixture(game,queueDuringPreparation); running = fixture;
        try { fixture.begin(); } catch (Throwable error) { running = null; fixture.close(); throw error; }
    }
    public static void tick(GameController game) {
        if (running == null) return;
        var fixture = running;
        try {
            require(fixture.game == game,"same controller");
            if (!fixture.step()) return;
            running = null; fixture.close();
            SkyWars2.LOGGER.info("[check] PASS {} (queuedStart={}, new epoch, observer enrolled, native TimeStop, one start award, lives/settings/profiles preserved, fresh match playing)",fixture.queueDuringPreparation?"cunny-queued-restart":"cunny-restart",fixture.queueDuringPreparation);
        } catch (Throwable error) {
            running = null;
            try { fixture.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            SkyWars2.LOGGER.error("[check] FAIL cunny-restart",error);
        }
    }
    private static final class Fixture implements AutoCloseable {
        private record Profile(int coins,int games,int memcoins,int kit,int skill,int suffix,int cage,
                               Set<String> unlocks,Set<String> legacy) {
            Profile(PlayerStore.Profile p) { this(p.coins,p.games,p.memcoins,p.kit,p.skill,p.suffix,p.cage,
                    Set.copyOf(p.lbcUnlocks),Set.copyOf(p.legacyUnlocks)); }
            void verify(PlayerStore.Profile p) {
                require(p.coins == coins + 10 && p.games == games + 1,"one canonical start award and game");
                require(p.memcoins == memcoins && p.kit == kit && p.skill == skill && p.suffix == suffix
                        && p.cage == cage && p.lbcUnlocks.equals(unlocks) && p.legacyUnlocks.equals(legacy),"permanent profile retained");
            }
        }
        final GameController game;
        final ServerPlayer owner,observer;
        final String epoch,map;
        final int mode,lives,start;
        final boolean broken;
        final boolean queueDuringPreparation;
        final Map<UUID,Profile> profiles = new LinkedHashMap<>();
        int state,at; boolean closed,completed,queuedStart;
        Fixture(GameController game,boolean queueDuringPreparation) {
            this.game=game;
            this.queueDuringPreparation=queueDuringPreparation;
            var online=game.server().getPlayerList().getPlayers();
            require(game.playing() && online.size() >= 2,"actual match and at least two players");
            owner=online.getFirst();observer=online.get(1);
            require(game.allowDamage(owner) && game.allowDamage(observer),"two normal participants");
            require(!game.vanity().busy() && !game.vanity().hasSnapshot(),"exclusive coordinator scene");
            epoch=game.items().epoch();map=game.config().mapId;mode=game.config().gameMode;
            broken=game.config().broken;lives=game.config().lives;start=at=game.server().getTickCount();
        }
        void begin() {
            // Real observer ownership, rather than merely changing the vanilla game mode.
            game.spectators().enter(observer);
            for(var player:game.server().getPlayerList().getPlayers()) {
                profiles.put(player.getUUID(),new Profile(game.players().get(player.getUUID())));
                player.getInventory().setItem(8,new ItemStack(Items.STRUCTURE_VOID,7));
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING,12000),owner);
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(777);
            }
            require(game.isObserver(observer),"actual eliminated observer");
            game.items().timeStop().begin(game.maps().level(),200);
            require(game.restartForCunny(owner),"accepted normal participant");
            require(!game.restartForCunny(owner),"duplicate rejected before shared boundary");
        }
        boolean step() {
            int now=game.server().getTickCount();
            require(now-start < 2600,"bounded asynchronous restart timeout, state="+state+", stage="+game.stage());
            if(state==0) {
                if(now-at<8)return false;
                require(epoch.equals(game.items().epoch()) && game.playing(),"TimeStop holds old epoch");
                game.items().timeStop().end(game.maps().level());state=1;return false;
            }
            if(state==1) {
                if(queueDuringPreparation&&!queuedStart&&game.cunnyRestartPending()&&game.inLobby()&&!game.selectedMapReady()){
                    game.requestStart(null,false);queuedStart=true;
                }
                if(epoch.equals(game.items().epoch()) || game.stage()!=GameController.Stage.COUNTDOWN)return false;
                require(!queueDuringPreparation||queuedStart,"queued start exercised the preparation callback");
                require(!game.cunnyRestartPending(),"countdown consumes the old restart driver, including a queued-start callback");
                require(game.config().gameMode==mode && game.config().mapId.equals(map)
                        && game.config().broken==broken,"selected admitted mode/map retained");
                for(var entry:profiles.entrySet()) {
                    var player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(entry.getKey()));
                    entry.getValue().verify(game.players().get(entry.getKey()));
                    require(!game.isObserver(player) && game.lives(entry.getKey())==Math.max(1,lives),"all online enrolled with configured lives");
                    require(!player.hasEffect(MobEffects.GLOWING)
                            && player.getAttributeValue(Attributes.ATTACK_DAMAGE)!=777,"old effects and attributes cleared");
                    require(player.getInventory().getItem(8).isEmpty(),"old inventory cleared before fresh kits");
                }
                state=2;return false;
            }
            if(state==2) {
                if(!game.playing())return false;
                require(game.allowDamage(owner) && game.allowDamage(observer),"both bodies in actual fresh match");
                for(var entry:profiles.entrySet())entry.getValue().verify(game.players().get(entry.getKey()));
                completed=true;return true;
            }
            return false;
        }
        public void close() {
            if(closed)return;closed=true;
            game.items().timeStop().end(game.maps().level());
            if(!completed && game.playing() && !game.maps().busy())game.stopMatch();
        }
    }
}
