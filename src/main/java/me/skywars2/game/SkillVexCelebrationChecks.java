package me.skywars2.game;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ActorCheckpointAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Actual skill damage reaction, fireworks, owner's leave cleanup, and automatic lobby return. */
final class SkillVexCelebrationChecks {
    private static Fixture active;

    static void run(GameController game) { run(game, false); }

    static void runBoss(GameController game) { run(game, true); }

    private static void run(GameController game, boolean bossScenario) {
        require(active==null,"exclusive fixture");
        var fixture=new Fixture(game, bossScenario);
        try { fixture.begin();active=fixture; }
        catch(Throwable error) {
            try { fixture.close(); } catch(Throwable cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    static void tick(GameController game) {
        var fixture=active;
        if(fixture==null||fixture.game!=game)return;
        try {
            if(!fixture.step())return;
            active=null;fixture.close();
            SkyWars2.LOGGER.info("[check] PASS {} (native Vex, dolphin, coin/display, summon and hedgehog retained through fireworks and owner leave; attacks pause; permanent lobby cleanup)",fixture.suite());
        } catch(Throwable error) {
            active=null;
            try { fixture.close(); } catch(Throwable cleanup) { error.addSuppressed(cleanup); }
            SkyWars2.LOGGER.error("[check] FAIL "+fixture.suite(),error);
        }
    }

    private static void require(boolean ok,String why) {
        if(!ok)throw new IllegalStateException("Vex celebration: "+why);
    }

    private static final class Fixture {
        final GameController game;
        final ServerPlayer owner,other;
        final ServerLevel level;
        final boolean bossScenario;
        final GameChecks.WandThirdPlayerState savedOwner,savedOther;
        final MatchValueSnapshot settings,ownerProfile,otherProfile;
        final RegenerationBeltChecks.ScheduledClient client,audience;
        final Set<Entity> initial=new HashSet<>();
        final Set<UUID> rockets=new HashSet<>();
        final int start;
        Cow target;
        Vex vex;
        Dolphin dolphin;
        Mob summon, hedgehog;
        Entity coin,coinDisplay;
        int dolphinAge,coinAge,dolphinTicks;
        int attempts,nextTrial,ending;
        boolean finishing,leaveChecked;

        Fixture(GameController game, boolean bossScenario) {
            this.game=game;
            this.bossScenario=bossScenario;
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            level=game.maps().level();
            require(game.playing()&&game.allowDamage(owner)&&game.allowDamage(other)&&owner.level()==level&&other.level()==level
                    &&!game.bedwars().mode()&&!game.maps().busy(),"two active ordinary participants in a ready native arena");
            savedOwner=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            savedOther=new GameChecks.WandThirdPlayerState(other,game.items().mana());
            settings=new MatchValueSnapshot(game.config());
            ownerProfile=new MatchValueSnapshot(game.players().get(owner.getUUID()));
            otherProfile=new MatchValueSnapshot(game.players().get(other.getUUID()));
            start=game.server().getTickCount();
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);
            audience=new RegenerationBeltChecks.ScheduledClient(game,other);
        }

        String suite() { return bossScenario ? "god-hedgehog-celebration" : "skill-vex-celebration"; }

        void begin() {
            game.config().vanillaSkills=true;game.config().ranked=true;game.config().endingSeconds=2;
            var profile=game.players().get(owner.getUUID());profile.skill=13;profile.winEffect=0;
            game.players().get(other.getUUID()).skill=0;
            var at=game.maps().look().add(0,8,0);
            move(owner,at,client);move(other,at.add(5,0,0),audience);
            require(game.events().selectedSkill(owner)==13&&game.events().checkpointTimeline().running(),"real selected skill13 is active");
            game.events().giveSkill(owner);
            level.getAllEntities().forEach(initial::add);
        }

        void move(ServerPlayer player,Vec3 at,RegenerationBeltChecks.ScheduledClient connection) {
            require(player.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"native arena teleport");
            player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.removeAllEffects();connection.acknowledge();
        }

        void finish() throws ReflectiveOperationException {
            require(vex.isAlive()&&level.getEntity(vex.getUUID())==vex,"normal skill Vex is alive and registered before victory");
            var skills=(Map<?,?>)WoolBucketChecks.read(game.events(),"skillStates");
            var skill=Objects.requireNonNull(skills.get(owner.getUUID()));
            require(((Set<?>)WoolBucketChecks.read(skill,"vexes")).contains(vex)&&Math.abs(vex.getHealth()-.1F)<.001,
                    "native Vex is the actual skill13 summon of the future winner");
            target=Objects.requireNonNull(EntityTypes.COW.create(level,EntitySpawnReason.TRIGGERED));
            target.setPos(vex.position().add(2,0,0));target.setNoAi(true);target.setNoGravity(true);
            require(level.addFreshEntity(target)&&vex.canAttack(target),"ordinary native cow is an eligible hostile target in PLAYING");
            vex.setNoAi(true);vex.setInvulnerableTime(80);
            spawnSummonAndHedgehog();
            spawnPozdmanActors();
            var members=(Map<?,?>)WoolBucketChecks.read(game,"participants");
            for(var member:members.entrySet())if(!owner.getUUID().equals(member.getKey())){
                var field=member.getValue().getClass().getDeclaredField("lives");field.setAccessible(true);field.setInt(member.getValue(),0);
            }
            var finish=GameController.class.getDeclaredMethod("finish");finish.setAccessible(true);finish.invoke(game);
            finishing=true;ending=game.server().getTickCount();
            require(game.stage()==GameController.Stage.ENDING&&owner.getUUID().equals(WoolBucketChecks.read(game,"victoryWinner")),
                    "real match finalization announces the fixture owner");
            requireAlive("when real fireworks begin");
        }

        private void spawnSummonAndHedgehog() {
            var at = vex.position().add(-3, 0, 0);
            summon = Objects.requireNonNull(game.items().summons().spawnPreset(owner, "vex", at),
                    "real owner-linked summon witness");
            summon.setNoAi(true);
            summon.setNoGravity(true);
            hedgehog = Objects.requireNonNull(bossScenario
                    ? game.items().godHedgehog().summon(level, at, owner)
                    : game.items().hedgehog().spawnHelper(owner, level, at.add(-2, 0, 0), LbcHedgehog.Kind.TANK),
                    "real hedgehog actor witness");
            hedgehog.setNoGravity(true);
            require(game.items().summons().owner(summon)==owner && hedgehog.isAlive(),
                    "ordinary summoned mob belongs to the winner and hedgehog starts alive");
        }

        void spawnPozdmanActors() {
            var pozdman=game.items().pozdmanBot();
            require(pozdman.count()==0&&!pozdman.waiting(owner),"exclusive dolphin and coin witnesses");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcPozdmanBot.ITEM));
            require(pozdman.use(owner,InteractionHand.MAIN_HAND,100),"admitted source bot use rolls Shark David");
            require(pozdman.count()==1,"source bot use publishes only the dolphin");
            var body=pozdman.snapshot().getFirst();
            require(body instanceof Dolphin,"native dolphin witness");
            dolphin=(Dolphin)body;
            dolphin.setNoAi(true);
            coin=Objects.requireNonNull(pozdman.spawnCoin(dolphin),"actual native physical coin");
            coinDisplay=Objects.requireNonNull(pozdman.state(coin).display(),"native coin display");
            dolphinAge=pozdman.state(dolphin).age();
            coinAge=pozdman.state(coin).age();
            dolphinTicks=dolphin.tickCount;
            requireAlive("before the victory boundary");
        }

        void requireAlive(String phase) {
            require(!vex.isRemoved()&&level.getEntity(vex.getUUID())==vex,
                    "original registered Vex retained "+phase+" reason="+vex.getRemovalReason());
            for(var entity:List.of(dolphin,coin,coinDisplay,summon,hedgehog)){
                require(!entity.isRemoved()&&level.getEntity(entity.getUUID())==entity,
                        "original Pozdman "+entity.getType()+" retained "+phase+" reason="+entity.getRemovalReason());
            }
        }

        boolean step() throws ReflectiveOperationException {
            client.acknowledge();audience.acknowledge();
            int now=game.server().getTickCount();
            require(now-start<600,"bounded skill and lobby fixture, attempts="+attempts+" stage="+game.stage());
            if(!finishing){
                for(var entity:level.getAllEntities())if(entity instanceof Vex actor&&!actor.shouldBeSaved()
                        &&!actor.isRemoved()&&!initial.contains(actor)) {vex=actor;break;}
                if(vex!=null){finish();return false;}
                if(now<nextTrial)return false;
                require(++attempts<=32,"real skill summon within 32 accepted damage trials");
                owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setHealth(owner.getMaxHealth());
                require(owner.hurtServer(level,owner.damageSources().generic(),1),"accepted native damage schedules skill13 through the normal hit hook");
                nextTrial=now+12;return false;
            }
            if(game.stage()==GameController.Stage.ENDING){
                requireAlive("during the native celebration");
                require(!game.events().checkpointTimeline().running(),"active gameplay event timers pause at the real victory boundary");
                var pozdman=game.items().pozdmanBot();
                require(pozdman.count()==2&&pozdman.state(dolphin).age()==dolphinAge
                                &&pozdman.state(coin).age()==coinAge&&pozdman.state(coin).display()==coinDisplay,
                        "dolphin and coin gameplay clocks remain paused");
                float health=target.getHealth();
                require(!vex.canAttack(target)&&!vex.doHurtTarget(level,target)&&target.getHealth()==health,
                        "retained Vex cannot damage an otherwise eligible native mob during celebration");
                require(!summon.canAttack(target)&&!hedgehog.doHurtTarget(level,target)&&target.getHealth()==health,
                        "retained ordinary summons and hedgehogs cannot deal melee damage during celebration");
                for(var entity:level.getAllEntities())if(entity instanceof FireworkRocketEntity&&!initial.contains(entity))rockets.add(entity.getUUID());
                if(now-ending>=22&&!leaveChecked){
                    require(rockets.size()>=8,"two ordinary native fireworks waves before winner cleanup");
                    require(dolphin.tickCount>dolphinTicks,"the retained native dolphin actually ticked during fireworks");
                    // This is the leave callback called by the native removal path. Keep the
                    // listener attached so the fixture can still observe the automatic lobby.
                    game.leave(owner);leaveChecked=true;
                    requireAlive("after the winner's canonical leave callback");
                }
                return false;
            }
            if(!game.inLobby())return false;
            var manager=((ActorCheckpointAccess.LevelEntities)level).skywars2$manager();
            require(leaveChecked&&vex.getRemovalReason()==Entity.RemovalReason.DISCARDED&&!manager.isLoaded(vex.getUUID())
                    &&level.getEntity(vex.getUUID())==null,"real timed return to lobby permanently retires the original Vex");
            for(var entity:List.of(dolphin,coin,coinDisplay,summon,hedgehog)){
                require(entity.getRemovalReason()==Entity.RemovalReason.DISCARDED&&!manager.isLoaded(entity.getUUID())
                                &&level.getEntity(entity.getUUID())==null,"lobby permanently retires "+entity.getType());
            }
            require(game.items().pozdmanBot().count()==0,"lobby clears the Pozdman actor ledger");
            require(game.items().godHedgehog().count()==0 && game.items().hedgehog().trackedCount()==0 && game.items().summons().count()==0,
                    "lobby clears the summon and hedgehog actor ledgers");
            return true;
        }

        void close() {
            try {
                if(vex!=null&&!vex.isRemoved())vex.discard();
                if(target!=null&&!target.isRemoved())target.discard();
                if(summon!=null&&!summon.isRemoved())summon.discard();
                if(hedgehog!=null&&!hedgehog.isRemoved())hedgehog.discard();
                if(dolphin!=null){
                    var pozdman=game.items().pozdmanBot();
                    pozdman.removed(dolphin);
                    if(coin!=null)pozdman.removed(coin);
                    if(coinDisplay!=null&&!coinDisplay.isRemoved())coinDisplay.discard();
                    pozdman.forget(owner);
                }
                if(!finishing&&game.playing())for(var saved:List.of(savedOwner,savedOther))saved.restore(game);
                settings.restore();ownerProfile.restore();otherProfile.restore();
                game.config().save();game.players().saveDeferred();
            } finally { try { client.close(); } finally { audience.close(); } }
        }
    }
}
