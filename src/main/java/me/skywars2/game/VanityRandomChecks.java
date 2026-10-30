package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongFunction;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.RandomCheckpointAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Exact native draws, real projectile spread/loot and player next-enchantment seeds through death/login. */
public final class VanityRandomChecks {
    private VanityRandomChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity random: "+why);}
    private static List<Long> tape(RandomSource r){
        var out=new ArrayList<Long>();out.add(Double.doubleToRawLongBits(r.nextGaussian()));out.add((long)r.nextInt(31));out.add(r.nextLong());
        out.add((long)Float.floatToRawIntBits(r.nextFloat()));out.add(Double.doubleToRawLongBits(r.nextDouble()));out.add(r.nextBoolean()?1L:0L);
        out.add((long)r.nextInt(Integer.MAX_VALUE));out.add(Double.doubleToRawLongBits(r.triangle(1.25,.4)));out.add(Double.doubleToRawLongBits(r.nextGaussian()));
        var fork=r.fork();out.add(fork.nextLong());out.add(Double.doubleToRawLongBits(fork.nextGaussian()));var factory=r.forkPositional();
        out.add(factory.at(11,-2,37).nextLong());out.add(factory.fromHashOf("vanity-native-check").nextLong());return List.copyOf(out);
    }
    private static void algorithms(){
        List<LongFunction<RandomSource>> factories=List.of(LegacyRandomSource::new,ThreadSafeLegacyRandomSource::new,SingleThreadedRandomSource::new,XoroshiroRandomSource::new,
                seed->new WorldgenRandom(new LegacyRandomSource(seed)),seed->new WorldgenRandom(new XoroshiroRandomSource(seed)));
        for(var factory:factories)for(boolean gaussian:List.of(false,true)){
            var r=factory.apply(73019);var control=factory.apply(73019);r.nextLong();control.nextLong();if(gaussian){r.nextGaussian();control.nextGaussian();}if(r instanceof WorldgenRandom){((RandomCheckpointAccess.Worldgen)r).skywars2$count(Integer.MAX_VALUE-2);((RandomCheckpointAccess.Worldgen)control).skywars2$count(Integer.MAX_VALUE-2);}
            var image=MatchRandomState.capture(r);var expected=tape(r);int count=r instanceof WorldgenRandom world?world.getCount():0;
            require(expected.equals(tape(control)),"capturing the native RNG does not advance it");
            for(int pass=0;pass<3;pass++){
                r.setSeed(999+pass);for(int i=0;i<7;i++)r.nextGaussian();image.restore(r);
                if(!gaussian&&r instanceof SingleThreadedRandomSource)require(((RandomCheckpointAccess.Single)r).skywars2$gaussian()==null,"lazy Gaussian absence restored");
                require(tape(r).equals(expected),"native algorithm/cache replay "+r.getClass()+" cached="+gaussian);
                if(r instanceof WorldgenRandom world)require(world.getCount()==count,"worldgen wrapped int count and delegate continuation");
            }
            var replacement=factory.apply(-9123);replacement.nextGaussian();image.restore(replacement);require(tape(replacement).equals(expected),"current replacement RNG object receives exact native state");
        }
        var unknown=new LegacyRandomSource(17){};boolean rejected=false;try{MatchRandomState.capture(unknown);}catch(IllegalStateException expected){rejected=true;}
        require(rejected&&unknown.nextLong()==new LegacyRandomSource(17).nextLong(),"unknown algorithm is rejected without advancing it");
        var known=MatchRandomState.capture(new LegacyRandomSource(23));var other=new XoroshiroRandomSource(23);rejected=false;
        try{known.restore(other);}catch(IllegalStateException expected){rejected=true;}require(rejected&&other.nextLong()==new XoroshiroRandomSource(23).nextLong(),"cross-algorithm replay is rejected before mutation");
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final MatchCoreCheckpoint originalCore;final MatchWorldRandomCheckpoint originalWorld;
        final Map<UUID,MatchRandomState> originalPlayers=new LinkedHashMap<>();final UUID id=GameChecks.offlineUuid("TestGamma");final boolean frozen;
        MatchActorCheckpoint originalActors,actors;MatchCoreCheckpoint core;MatchWorldRandomCheckpoint world;List<MatchPlayerCheckpoint> players;
        ServerPlayer gamma,retired;Cow cow;Projectile arrow;RandomSource borrowed;LootTable loot;RandomCheckpointAccess.Sequences sequences;
        Set<Identifier> keys;List<Object> expected;int expectedNextSeed,expectedXp,phase=-1,ticks,clock;boolean owned,closed;
        final Identifier key=Identifier.parse("skywars2:vanity_rng_probe"),future=Identifier.parse("skywars2:vanity_rng_future"),fresh=Identifier.parse("skywars2:vanity_rng_fresh");
        Fixture(GameController game){this.game=game;frozen=game.server().tickRateManager().isFrozen();require(!frozen&&game.playing()&&!game.maps().busy()&&game.server().getPlayerList().getPlayer(id)==null,"exclusive actual native match");
            originalCore=new MatchCoreCheckpoint(game);originalWorld=new MatchWorldRandomCheckpoint(game);for(var p:game.server().getPlayerList().getPlayers())originalPlayers.put(p.getUUID(),MatchRandomState.capture(p.getRandom()));base=new ChefChecks.Fixture(game);}
        void init(){originalActors=MatchActorCheckpoint.captureRegistered(game);base.init();gamma=GameChecks.createBot(game,"TestGamma");owned=true;game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");SkyWars2.LOGGER.info("[check] START vanity-random");}
        List<ServerPlayer> live(){return List.of(gamma,base.owner,base.other);}
        void setup(){
            game.server().tickRateManager().setFrozen(true);require(game.allowDamage(gamma)&&game.lives(id)==1,"actual one-life native participant");algorithms();
            for(var p:live()){base.move(p,base.at.add(0,1,0));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.getInventory().clearContent();p.getRandom().nextGaussian();p.experienceLevel=1000;}
            cow=(Cow)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:cow")).create(base.owner.level(),EntitySpawnReason.COMMAND);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(base.at.add(3,2,0));require(base.owner.level().addFreshEntity(cow),"native cow registration");cow.getRandom().nextGaussian();
            arrow=(Projectile)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:arrow")).create(base.owner.level(),EntitySpawnReason.COMMAND);arrow.setNoGravity(true);arrow.setPos(base.at.add(3,4,0));arrow.setOwner(gamma);require(base.owner.level().addFreshEntity(arrow),"native arrow registration");arrow.getRandom().nextGaussian();
            for(var level:game.server().getAllLevels()){level.getRandom().nextGaussian();((RandomCheckpointAccess.World)level).skywars2$sounds().nextGaussian();}
            borrowed=game.server().getRandomSequence(key);borrowed.nextGaussian();sequences=(RandomCheckpointAccess.Sequences)game.server().getRandomSequences();require(!sequences.skywars2$sequences().containsKey(fresh),"owned fresh native sequence identifier");
            loot=LootTable.DIRECT_CODEC.parse(game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(
                    "{\"random_sequence\":\"skywars2:vanity_rng_probe\",\"pools\":[{\"rolls\":12,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\",\"weight\":3},{\"type\":\"minecraft:item\",\"name\":\"minecraft:emerald\",\"weight\":5}]}]}" )).getOrThrow();
            keys=Set.copyOf(sequences.skywars2$sequences().keySet());actors=MatchActorCheckpoint.captureRegistered(game);players=live().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();core=new MatchCoreCheckpoint(game);world=new MatchWorldRandomCheckpoint(game);
            expected=probe();gamma.onEnchantmentPerformed(new ItemStack(Items.BOOK),1);expectedNextSeed=gamma.getEnchantmentSeed();expectedXp=gamma.experienceLevel;phase=0;
        }
        List<Object> probe(){
            var result=new ArrayList<Object>();for(var p:live())result.add(tape(p.getRandom()));result.add(tape(cow.getRandom()));
            arrow.shoot(.25,1,.5,3,2.75f);result.add(arrow.getDeltaMovement());result.add(arrow.getYRot());result.add(arrow.getXRot());result.add(tape(arrow.getRandom()));
            for(var level:game.server().getAllLevels()){
                result.add(level.dimension());result.add(tape(level.getRandom()));result.add(tape(((RandomCheckpointAccess.World)level).skywars2$sounds()));
                var positions=new ArrayList<net.minecraft.core.BlockPos>();for(int i=0;i<7;i++)positions.add(level.getBlockRandomPos(32,64,-48,15));result.add(List.copyOf(positions));
            }
            result.add(tape(borrowed));result.add(tape(game.server().getRandomSequence(key)));result.add(tape(game.server().getRandomSequence(fresh)));
            var params=new LootParams.Builder(gamma.level()).withParameter(LootContextParams.ORIGIN,gamma.position()).withParameter(LootContextParams.THIS_ENTITY,gamma).create(LootContextParamSets.COMMAND);
            result.add(loot.getRandomItems(params).stream().map(ItemStack::toString).toList());return List.copyOf(result);
        }
        void mutate(){
            for(var p:live()){p.getRandom().setSeed(801);for(int i=0;i<5;i++)p.getRandom().nextGaussian();}
            cow.getRandom().setSeed(802);arrow.getRandom().setSeed(803);for(var level:game.server().getAllLevels()){level.getRandom().setSeed(804);((RandomCheckpointAccess.World)level).skywars2$sounds().setSeed(805);for(int i=0;i<21;i++)level.getBlockRandomPos(0,0,0,31);}
            var owner=game.server().getRandomSequences();owner.clear();owner.setSeedDefaults(54127,false,false);owner.reset(key,42L);game.server().getRandomSequence(future).nextLong();borrowed.nextGaussian();
            gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying(),"actual native player death");
        }
        void restore(boolean offline){
            retired=gamma;core.restoreFrozen();actors.restoreBodiesFrozen();for(var image:players)if(game.server().getPlayerList().getPlayer(image.id())!=null)image.restoreFrozen();
            if(!offline){gamma=game.server().getPlayerList().getPlayer(id);gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());}
            require(actors.restoreLinksFrozen().equals(offline?Set.of(id):Set.of()),"native owner/offline UUID replay");world.restoreFrozen();
        }
        void verify(){
            require(game.server().getRandomSequence(key).equals(borrowed),"original borrowed named wrapper and current service share restored sequence identity");
            require(sequences.skywars2$sequences().keySet().equals(keys)&&!sequences.skywars2$sequences().containsKey(future),"future named entries removed without replacing the native map");
            require(arrow.getOwner()==gamma,"native restored projectile uses current player body");var actual=probe();
            for(int i=0;i<expected.size();i++)require(expected.get(i).equals(actual.get(i)),"actual native random/arrow/world/named/loot continuation field "+i+" expected="+expected.get(i)+" actual="+actual.get(i));
            gamma.onEnchantmentPerformed(new ItemStack(Items.BOOK),1);require(gamma.getEnchantmentSeed()==expectedNextSeed&&gamma.experienceLevel==expectedXp,"native next enchantment seed and XP restore exactly after body replacement/login");
        }
        void park(){for(var p:live()){p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);}arrow.setDeltaMovement(Vec3.ZERO);arrow.setPos(base.at.add(3,4,0));}
        boolean step(){
            require(++ticks<140,"bounded native RNG fixture phase="+phase);
            if(phase==-1){setup();return false;}
            if(phase==0){mutate();restore(false);require(gamma!=retired,"actual native replacement player RNG object");verify();park();clock=game.matchTicks();game.server().tickRateManager().setFrozen(false);phase=1;return false;}
            if(phase==1){if(game.matchTicks()-clock<4)return false;game.server().tickRateManager().setFrozen(true);mutate();restore(false);require(gamma!=retired,"second actual replacement body after real ticks");verify();
                gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity random offline fixture")));restore(true);gamma=GameChecks.createBot(game,"TestGamma");restore(false);verify();
                var held=game.server().getRandomSequence(key);var image=MatchRandomState.capture(held);var expected=tape(held);held.setSeed(91234);image.restore(held);require(tape(held).equals(expected),"dirty native wrapper restores exact underlying source and Gaussian cache");return true;}
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);
            try{var p=game.server().getPlayerList().getPlayer(id);if(owned&&p!=null)p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity random cleanup")));
                if(originalActors!=null){originalActors.restoreBodiesFrozen();originalActors.restoreLinksFrozen();}base.close();originalCore.restoreFrozen();
                originalPlayers.forEach((uuid,state)->{var current=game.server().getPlayerList().getPlayer(uuid);if(current!=null)state.restore(current.getRandom());});originalWorld.restoreFrozen();
            }finally{game.server().tickRateManager().setFrozen(frozen);}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-random (six native algorithm/delegate shapes and cached/lazy Gaussian states, forks and current replacement sources, world block/sound RNGs and global named-map/default/borrowed-wrapper identities; actual native projectile spread, weighted loot and next-enchantment seed through real death/respawn, four world ticks and offline/login; full coordinator remains gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-random",error);}}
}
