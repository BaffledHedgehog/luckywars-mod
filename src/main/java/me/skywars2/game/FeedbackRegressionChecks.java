package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;

/** Native dimension projection, body reactions and per-viewer black-hole retirement. */
public final class FeedbackRegressionChecks {
    private FeedbackRegressionChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Feedback: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final BlackHoleVisuals visuals;
        Fixture(GameController game){this.game=game;base=new AllGrimoireChecks.Fixture(game);visuals=new BlackHoleVisuals(game);}
        void ack(){var p=base.base.owner;p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var positions=base.base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
            if(!positions.isEmpty())p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(positions.getLast().id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));}
        void travel(ServerLevel level,Vec3 at){var p=base.base.owner;require(p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"native dimension transfer");ack();}
        void dimensions(){
            var p=base.base.owner;var home=p.level();var at=p.position();var id=Identifier.parse("lbc:nexus");
            var nexus=game.server().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse("minecraft:nexus")));require(nexus!=null,"source Nexus registered");
            p.removeAllEffects();var hole=visuals.create(p,at.add(0,0,4),.5f);require(visuals.displays(p).size()==1,"initial single shader row");
            try{
                travel(nexus,new Vec3(0,180,0));require(p.getPostEffects().contains(id),"actual native entry publishes Nexus shader immediately");
                for(int i=0;i<10;i++)visuals.tick();require(visuals.displays(p).isEmpty(),"previous-world holder retired on bounded audience refresh");
                travel(home,at);require(!p.getPostEffects().contains(id),"actual native exit removes Nexus shader immediately");
                // Force allocation before the old row's collapse ends; free indices must not duplicate the returning hole.
                var fresh=visuals.create(p,at.add(2,0,4),.5f);require(visuals.displays(p).isEmpty(),"world re-entry cannot reopen same hole in another free shader row");
                for(int i=0;i<6;i++)visuals.tick();require(visuals.displays(p).size()==2,"both live holes appear after one collapse window");
                visuals.remove(hole);var replacement=visuals.create(p,at.add(0,0,4),.5f);
                require(visuals.displays(p).size()==1&&!visuals.origins(p).contains(at.add(0,0,4)),"same-position replacement waits for old shader tail even with spare indices");
                for(int i=0;i<6;i++)visuals.tick();require(visuals.displays(p).size()==2,"replacement publishes after retirement, with no duplicate row");
                p.addPostEffect(id);SignedEffectVisuals.joined(p);require(!p.getPostEffects().contains(id),"join/respawn repair clears stale Nexus projection");
            }finally{if(p.level()!=home)travel(home,at);visuals.stop();}
        }
        long particles(){return base.base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).count();}
        long sounds(){return base.base.recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance).count();}
        long hitSeed(){var random=base.level.getRandom();for(long seed=0;seed<100000;seed++){random.setSeed(seed);if(random.nextFloat()<.05f){random.setSeed(seed);return seed;}}throw new IllegalStateException("No native5% seed");}
        void reactions(){
            var p=base.base.owner;var service=game.items().legacyEffects();var random=MatchRandomState.capture(base.level.getRandom());
            var key=net.minecraft.world.scores.ScoreHolder.forNameOnly("legacy");var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");var old=objective==null?null:board.getPlayerScoreInfo(key,objective);Integer legacy=old==null?null:old.value();
            try{
                game.items().mana().score(key,"lbc.math").set(0);p.removeAllEffects();p.addEffect(new MobEffectInstance(MobEffects.HUNGER,200,0));p.addEffect(new MobEffectInstance(MobEffects.NAUSEA,200,0));
                long count=particles(),sound=sounds();hitSeed();service.ambientReactions(p);require(particles()==count&&sounds()==sound,"levelI has neither random body reaction");
                p.removeAllEffects();p.addEffect(new MobEffectInstance(MobEffects.HUNGER,200,1));hitSeed();service.ambientReactions(p);
                require(particles()==count+1&&sounds()==sound+1&&!p.hasEffect(MobEffects.BLINDNESS),"HungerII native honey/sound reaction without invented debuffs");
                p.removeAllEffects();p.addEffect(new MobEffectInstance(MobEffects.NAUSEA,200,1));hitSeed();service.ambientReactions(p);
                require(particles()==count+2&&sounds()==sound+4&&p.hasEffect(MobEffects.BLINDNESS)&&p.hasEffect(MobEffects.DARKNESS),"NauseaII emits native chicken and three burps plus source one-second debuffs");
                require(p.getEffect(MobEffects.BLINDNESS).getAmplifier()==0&&p.getEffect(MobEffects.BLINDNESS).getDuration()==20&&!p.getEffect(MobEffects.BLINDNESS).isVisible(),"exact hidden blindnessI20ticks");
                game.items().mana().score(key,"lbc.math").set(1);count=particles();hitSeed();service.ambientReactions(p);require(particles()==count,"original legacy-mode opt-out preserved");
            }finally{random.restore(base.level.getRandom());if(legacy==null)board.resetSinglePlayerScore(key,board.getObjective("lbc.math"));else game.items().mana().score(key,"lbc.math").set(legacy);}
        }
        public void close(){try{visuals.stop();}finally{base.close();}}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.dimensions();f.reactions();}SkyWars2.LOGGER.info("[check] PASS feedback-regressions (real Nexus entry/exit/ACK and stale projection, same-hole/replacement row retirement; native Hunger/NauseaII5% payloads and levelI/legacy guards)");}
}
