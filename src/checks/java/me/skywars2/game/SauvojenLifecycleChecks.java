package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.LbcSpellSource;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.entities.mobs.LbcSauvojenBoss;
import me.skywars2.game.items.wands.LbcSauvojen;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.mixin.entities.ActorCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Original scheduler on actual server ticks, including a genuine creator logout. */
public final class SauvojenLifecycleChecks {
    private SauvojenLifecycleChecks(){}
    private static Fixture running;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Sauvojen lifecycle: "+detail);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final SauvojenBossChecks.Fixture base;
        final ServerPlayer creator;
        final ServerLevel level;
        final Vec3 ownerPoint,betaPoint,bossPoint;
        final Vec3 coldPoint;
        final LbcSpellSource.Source bossAuthority;
        long initialSelections;
        int ticks,phase,lastTick,lastSelectionTick=-1,pauseTick,logoutTick;
        long selections,pauseSelections,pauseEmissions,logoutSelections,logoutEmissions;
        int pauseClouds;
        int visibilityPhase,visibilityTick;
        long hiddenChoices,hiddenShots;
        Marker carrier;
        Vec3 carrierPosition;
        LbcSpellSource.Source carrierAuthority;
        boolean stopped;
        Fixture(GameController game){
            this.game=game;level=game.maps().level();
            creator=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma")));
            require(game.canUseGameplayItems(creator)&&!game.items().timeStop().stopped(game.maps().level()),"third real participant and no borrowed time stop");
            base=new SauvojenBossChecks.Fixture(game);
            try{base.activation(creator);
            ownerPoint=base.owner.position();betaPoint=base.beta.position();bossPoint=base.boss.position();
            for(var player:List.of(base.owner,base.beta)){
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(player.getMaxHealth());
                player.getInventory().clearContent();
            }
            var service=game.items().sauvojenBoss();
            var site=visibilitySite();
            coldPoint=site.hidden();
            var nearby=base.cow(site.target());
            require(level.getEntity(nearby.getUUID())==nearby&&nearby.distanceToSqr(coldPoint)<110*110,
                    "loaded ordinary cow really qualifies beside the cold column");
            bossAuthority=game.items().spellSources().source(base.boss);
            require(bossAuthority!=null&&bossAuthority.emitter().equals(base.boss.getUUID())
                    &&bossAuthority.author()==null&&bossAuthority.unownedMobs(),"bound native boss starts with its hostile source");
            hiddenChoices=service.selections();
            visibilityTick=lastTick=game.server().getTickCount();
            base.boss.getRandom().setSeed(rubySeed());
            base.boss.setPos(coldPoint);
            require(registeredHidden(base.boss),"real Ghast loses visibility but remains registered");
            }catch(Throwable error){try{base.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
        }
        private record VisibilitySite(Vec3 hidden,Vec3 target){}
        VisibilitySite visibilitySite(){
            int chunkX=base.owner.blockPosition().getX()>>4,chunkZ=base.owner.blockPosition().getZ()>>4;
            for(int ring=1;ring<=12;ring++)
                for(int dx=-ring;dx<=ring;dx++)for(int dz=-ring;dz<=ring;dz++){
                    if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
                    int x=chunkX+dx,z=chunkZ+dz;
                    if(level.getChunkSource().getChunkNow(x,z)!=null||level.areEntitiesLoaded(ChunkPos.pack(x,z)))continue;
                    var hidden=new Vec3(x*16+8.5,bossPoint.y,z*16+8.5);
                    for(int a=-4;a<=4;a++)for(int b=-4;b<=4;b++){
                        int warmX=x+a,warmZ=z+b;
                        if(level.getChunkSource().getChunkNow(warmX,warmZ)==null||!level.areEntitiesLoaded(ChunkPos.pack(warmX,warmZ)))continue;
                        var target=new Vec3(warmX*16+8.5,bossPoint.y,warmZ*16+8.5);
                        if(level.isPositionEntityTicking(BlockPos.containing(target)))return new VisibilitySite(hidden,target);
                    }
                }
            throw new IllegalStateException("Sauvojen lifecycle: no real entity-ticking neighbor within 110 blocks of a cold column");
        }
        boolean registeredHidden(Entity body){
            return body.getRemovalReason()==null&&level.getEntity(body.getUUID())==null
                    &&((ActorCheckpointAccess.LevelEntities)level).skywars2$manager().isLoaded(body.getUUID());
        }
        private static long rubySeed(){
            for(long seed=0;;seed++)if(RandomSource.create(seed).nextInt(10)==LbcSauvojen.Kind.RUBY.ordinal())return seed;
        }
        boolean visibility(int now,LbcSauvojenBoss service){
            int elapsed=now-visibilityTick;
            switch(visibilityPhase){
                case 0->{
                    require(service.selections()==hiddenChoices,"registered-hidden boss released a selection on native tick "+elapsed);
                    require(registeredHidden(base.boss)&&game.items().spellSources().source(base.boss)==bossAuthority,
                            "deferred visibility callback retains original Ghast and exact hostile source");
                    if(elapsed<45)return false;
                    base.boss.setPos(bossPoint);
                    require(level.getEntity(base.boss.getUUID())==base.boss,"same Ghast becomes natively visible");
                    base.boss.getRandom().setSeed(rubySeed());
                }
                case 1->{
                    require(elapsed<=125,"visible Ghast resumes the original selection boundary");
                    if(service.cloudCount()==0){
                        base.boss.setPos(bossPoint);
                        base.boss.getRandom().setSeed(rubySeed());
                        return false;
                    }
                    require(service.selections()>hiddenChoices&&service.cloudCount()==1,"visible selection creates one real wand");
                    carrier=level.getEntitiesOfClass(Marker.class,new net.minecraft.world.phys.AABB(bossPoint,bossPoint).inflate(4),
                            e->e.entityTags().contains("savoujen_cloud")).getFirst();
                    carrierPosition=carrier.position();carrierAuthority=game.items().spellSources().source(carrier);
                    require(carrierAuthority!=null,"real wand marker has native hostile authority");
                    hiddenChoices=service.selections();hiddenShots=service.emissions();
                    carrier.setPos(coldPoint);base.boss.setPos(coldPoint);
                    require(registeredHidden(carrier)&&registeredHidden(base.boss),"both real bodies can stay registered outside visibility");
                }
                case 2->{
                    require(registeredHidden(carrier)&&registeredHidden(base.boss)&&service.cloudCount()==1
                            &&service.selections()==hiddenChoices&&service.emissions()==hiddenShots
                            &&game.items().spellSources().source(carrier)==carrierAuthority,
                            "hidden cloud retains the same carrier, source and idle clock through native tick "+elapsed);
                    if(elapsed<85)return false;
                    carrier.setPos(carrierPosition);
                    require(level.getEntity(carrier.getUUID())==carrier&&game.items().spellSources().source(carrier)==carrierAuthority,
                            "same wand marker returns as the original emitter");
                }
                case 3->{
                    require(elapsed<=85&&service.selections()==hiddenChoices&&registeredHidden(base.boss),
                            "returned wand can resume independently of its hidden boss");
                    if(service.emissions()==hiddenShots)return false;
                    require(service.emissions()==hiddenShots+1&&service.cloudCount()==1&&level.getEntity(carrier.getUUID())==carrier,
                            "same resumed wand emits once and retains its remaining active lifetime");
                    base.boss.setPos(bossPoint);
                    initialSelections=selections=service.selections();
                    visibilityPhase++;
                    return true;
                }
                default->{return true;}
            }
            visibilityPhase++;visibilityTick=now;
            return false;
        }
        boolean step(){
            int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            require(++ticks<=920&&game.playing(),"bounded actual match, phase="+phase+" visibility="+visibilityPhase);
            var service=game.items().sauvojenBoss();
            require(!base.boss.isRemoved()&&base.boss.isAlive()&&service.count()==1,"independent native boss remains alive");
            // Keep the test targets alive and resident; do not drive the spell or boss clocks.
            maintain(base.owner,ownerPoint);maintain(base.beta,betaPoint);
            if(!visibility(now,service))return false;
            base.boss.setPos(bossPoint);base.boss.setDeltaMovement(Vec3.ZERO);
            if(phase==0){
                long current=service.selections();
                if(current!=selections){
                    require(current==selections+1,"one selection per scheduler boundary");
                    if(lastSelectionTick>=0)require(now-lastSelectionTick==40,"original 40-tick selection cadence");
                    lastSelectionTick=now;selections=current;
                }
                if(current-initialSelections<2||service.cloudCount()==0||!service.captureReady())return false;
                replayClouds();
                pauseSelections=service.selections();pauseEmissions=service.emissions();pauseClouds=service.cloudCount();pauseTick=now;
                game.items().timeStop().begin(level,200);stopped=true;phase=1;return false;
            }
            if(phase==1){
                require(service.selections()==pauseSelections&&service.emissions()==pauseEmissions&&service.cloudCount()==pauseClouds,
                        "TimeStop pauses actual selection, emission and cloud lifetime");
                if(now-pauseTick<20)return false;
                game.items().timeStop().end(level);stopped=false;
                creator.connection.onDisconnect(new DisconnectionDetails(Component.literal("Sauvojen creator lifecycle check")));
                require(game.server().getPlayerList().getPlayer(creator.getUUID())==null&&service.owner(base.boss)==null,"real native creator departure");
                require(service.cloudCount()==pauseClouds&&!base.boss.isRemoved(),"logout leaves boss and active clouds intact");
                logoutSelections=service.selections();logoutEmissions=service.emissions();logoutTick=now;phase=2;return false;
            }
            if(now-logoutTick<80||!service.captureReady())return false;
            require(service.selections()>=logoutSelections+2&&service.emissions()>logoutEmissions,"real scheduler and cloud attacks continue without creator");
            require(game.items().spellSources().source(base.boss).author()==null,"no player authority is recaptured after logout");
            replayClouds();
            return true;
        }
        void maintain(ServerPlayer player,Vec3 point){
            require(player.isAlive()&&game.canUseGameplayItems(player),"actual remaining participant stays eligible");
            player.setHealth(player.getMaxHealth());player.setDeltaMovement(Vec3.ZERO);player.setNoGravity(true);
            if(player.level()!=level||player.position().distanceToSqr(point)>1e-6)
                player.teleportTo(level,point.x,point.y,point.z,Set.of(),0,0,true);
        }
        void replayClouds(){
            var service=game.items().sauvojenBoss();
            require(service.captureReady()&&service.cloudCount()>0,"live cloud and drained native terrain work at capture boundary");
            var entities=new ArrayList<Entity>();level.getAllEntities().forEach(entities::add);
            var links=new MatchSpellCheckpoint.Links(new MatchDamageSources(game,entities));
            var picture=service.checkpoint(links);int count=service.cloudCount();long shots=service.emissions(),choices=service.selections();
            require(links.resolve(id->{
                var player=game.server().getPlayerList().getPlayer(id);if(player!=null)return player;
                for(var world:game.server().getAllLevels()){var entity=world.getEntity(id);if(entity!=null)return entity;}
                return null;
            }).isEmpty(),"native cloud links resolve without borrowed creator");
            service.prepareFrozen();picture.prepare(links).run();service.publishFrozen();
            require(service.cloudCount()==count&&service.emissions()==shots&&service.selections()==choices,
                    "typed cloud checkpoint republishes nullable-owner visuals and exact counters");
        }
        @Override public void close(){if(stopped)game.items().timeStop().end(level);base.close();}
    }
    public static void run(GameController game){
        require(running==null,"exclusive fixture");
        running=new Fixture(game);
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;
            SkyWars2.LOGGER.info("[check] PASS sauvojen-lifecycle (real 40-tick scheduler; retained registered-hidden boss and wand with independent resume; TimeStop, native creator disconnect, continued NPC attacks and typed cloud replay)");
        }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;
            SkyWars2.LOGGER.error("[check] FAIL sauvojen-lifecycle",error);}
    }
}
