package me.skywars2.game;

import java.util.*;

import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.PathfindToRaidGoal;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Real native raid creation, recruitment and service removal, with full owner/body replay. */
final class VanityRaidChecks {
    private VanityRaidChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity raid: "+why);}
    private static Object read(Object owner,String name){return WoolBucketChecks.read(owner,name);}
    private static int count(Object owner,String name){return (Integer)read(owner,name);}
    private static void restore(GameController game,MatchActorCheckpoint image,Set<UUID> omitted){VanityActionGoalChecks.restore(game,image,null,omitted);}
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var before=MatchActorCheckpoint.captureRegistered(game);var player=new MatchPlayerCheckpoint(game,f.owner);
        boolean raidsEnabled=f.level.getGameRules().get(GameRules.RAIDS);
        try{
            f.owner.setGameMode(GameType.SURVIVAL);f.owner.removeEffect(net.minecraft.world.effect.MobEffects.RAID_OMEN);
            f.level.getGameRules().set(GameRules.RAIDS,true,game.server());
            BlockPos at=BlockPos.containing(f.home).above(4).offset(18,0,80);
            var manager=f.level.getRaids();var ownerMap=read(manager,"raidMap");
            var raid=manager.createOrExtendRaid(f.owner,at);
            require(raid!=null&&raid.isActive()&&!f.level.isVillage(at),"native world service creates active raid outside occupied village");
            int id=manager.getId(raid).orElseThrow();
            var captain=add(f,at);var friend=add(f,at.east(4));var later=add(f,at.east(7));
            captain.setCanJoinRaid(true);friend.setCanJoinRaid(true);later.setCanJoinRaid(false);
            captain.setPatrolLeader(true);
            raid.joinRaid(f.level,raid.getGroupsSpawned(),captain,null,true);raid.setLeader(captain.getWave(),captain);
            var goal=new PathfindToRaidGoal<>(captain);var selector=VanityActionGoalChecks.selector(captain);selector.addGoal(1,goal);
            for(int i=0;i<5;i++)f.level.tickNonPassenger(captain);selector.tick();
            var wrapper=selector.getAvailableGoals().iterator().next();
            require(wrapper.isRunning()&&friend.getCurrentRaid()==raid&&later.getCurrentRaid()==null&&raid.getTotalRaidersAlive()==2,
                    "actual native raid path task recruits only eligible neighbor");
            int deadline=count(goal,"recruitmentTick"),bodyTime=captain.tickCount;
            require(deadline==bodyTime+20,"actual recruitment delay is tied to native body clock");
            var members=read(raid,"groupRaiderMap");var leaders=read(raid,"groupToLeaderMap");
            var group=((Map<?,?>)members).get(captain.getWave());
            var bar=(ServerBossEvent)read(raid,"raidEvent");
            bar.setName(Component.literal("saved native raid"));bar.setProgress(.42f);bar.setColor(BossEvent.BossBarColor.BLUE);bar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_6);
            bar.setDarkenScreen(true);bar.setPlayBossMusic(true);bar.setCreateWorldFog(true);bar.addPlayer(f.owner);
            var random=(RandomSource)read(raid,"random");random.nextGaussian();
            captain.setTicksOutsideRaid(17);friend.setTicksOutsideRaid(9);
            var image=MatchActorCheckpoint.captureRegistered(game);int nextId=count(manager,"nextId"),tick=count(manager,"tick");
            var persisted=net.minecraft.world.entity.raid.Raids.CODEC.encodeStart(NbtOps.INSTANCE,manager).getOrThrow().copy();
            long nextRandom=random.nextLong();double nextGaussian=random.nextGaussian();
            int delay=recruit(captain,later,selector,raid);
            require(delay==21,"actual native recruitment waits until body clock passes its saved deadline");
            for(int replay=0;replay<3;replay++){
                raid.addHeroOfTheVillage(f.other);raid.setRaidOmenLevel(4);
                captain.setTicksOutsideRaid(91);friend.setTicksOutsideRaid(92);
                raid.removeFromRaid(f.level,friend,true);raid.removeLeader(captain.getWave());raid.stop();manager.tick(f.level);
                require(manager.get(id)==null,"actual native service tick removes stopped raid");
                var future=manager.createOrExtendRaid(f.owner,at.offset(256+replay*128,0,0));
                require(future!=null&&future!=raid,"genuine future native raid has another owner identity");
                var futureBar=(ServerBossEvent)read(future,"raidEvent");futureBar.addPlayer(f.other);
                bar.setName(Component.literal("future"));bar.setColor(BossEvent.BossBarColor.RED);bar.setVisible(true);bar.addPlayer(f.other);
                int packets=f.client.packets().size();restore(game,image,Set.of());
                require(manager==f.level.getRaids()&&read(manager,"raidMap")==ownerMap&&manager.get(id)==raid
                        &&count(manager,"nextId")==nextId&&count(manager,"tick")==tick,
                        "same native world service, map, raid owner, ID allocator and service clock restored");
                require(persisted.equals(net.minecraft.world.entity.raid.Raids.CODEC.encodeStart(NbtOps.INSTANCE,manager).getOrThrow())&&manager.isDirty(),
                        "native saved-data codec returns exact original contents and owner is dirty for persistence");
                require(raid.isActive()&&!raid.isStopped()&&read(raid,"groupRaiderMap")==members&&read(raid,"groupToLeaderMap")==leaders
                        &&((Map<?,?>)members).get(captain.getWave())==group&&raid.getLeader(captain.getWave())==captain,
                        "original native membership map, member set and captain relationship restored");
                require(raid.getTotalRaidersAlive()==2&&captain.getCurrentRaid()==raid&&friend.getCurrentRaid()==raid&&later.getCurrentRaid()==null
                        &&!later.canJoinRaid()&&captain.tickCount==bodyTime&&count(goal,"recruitmentTick")==deadline&&wrapper.isRunning(),
                        "original participant graph and body-local recruitment phase restored without native NBT rejoining");
                require(count(captain,"ticksOutsideRaid")==17&&count(friend,"ticksOutsideRaid")==9&&captain.isPatrolLeader(),
                        "native non-NBT outside-raid counters and actual patrol-leader body restore");
                require(bar.getPlayers().equals(Set.of(f.owner))&&bar.isVisible()&&bar.getName().getString().equals("saved native raid")
                        &&bar.getProgress()==.42f&&bar.getColor()==BossEvent.BossBarColor.BLUE&&bar.getOverlay()==BossEvent.BossBarOverlay.NOTCHED_6
                        &&bar.shouldDarkenScreen()&&bar.shouldPlayBossMusic()&&bar.shouldCreateWorldFog(),"same raid boss event restores all display data and exact audience");
                require(futureBar.getPlayers().isEmpty()&&!futureBar.isVisible()
                        &&f.client.packets().subList(packets,f.client.packets().size()).stream().anyMatch(ClientboundBossEventPacket.class::isInstance),
                        "future boss event retired and native boss-event repair packets actually published");
                require(random.nextLong()==nextRandom&&Double.doubleToLongBits(random.nextGaussian())==Double.doubleToLongBits(nextGaussian),
                        "same native raid RNG and cached Gaussian sequence replay");
                require(recruit(captain,later,selector,raid)==delay,"same actual body-tick delay until native recruitment after every replay");
            }
            restore(game,image,Set.of(friend.getUUID()));
            require(raid.getTotalRaidersAlive()==1&&friend.isRemoved()&&friend.getCurrentRaid()==null&&captain.getCurrentRaid()==raid,
                    "explicitly omitted participant leaves both native membership and owner link without resurrection");
            restore(game,image,Set.of(captain.getUUID()));
            require(raid.getTotalRaidersAlive()==1&&raid.getLeader(captain.getWave())==null&&captain.getCurrentRaid()==null&&friend.getCurrentRaid()==raid,
                    "omitted captain is also removed from leadership while remaining member survives");
            restore(game,image,Set.of());require(raid.getTotalRaidersAlive()==2&&raid.getLeader(captain.getWave())==captain,
                    "same complete source image reusable after both membership omissions");
            bar.removePlayer(f.owner);bar.addPlayer(f.other);bar.setProgress(.65f);captain.setTicksOutsideRaid(27);
            links(game,image);
            require(bar.getPlayers().equals(Set.of(f.other))&&bar.getProgress()==.65f&&count(captain,"ticksOutsideRaid")==27,
                    "later link-only replay preserves ongoing audience, bar progress and body counters");
            raid.removeFromRaid(f.level,friend,true);raid.stop();manager.tick(f.level);int laterTick=count(manager,"tick");
            links(game,image);
            require(manager.get(id)==null&&count(manager,"tick")==laterTick&&raid.isStopped()
                    &&friend.getCurrentRaid()==null&&raid.getTotalRaidersAlive()==1&&bar.getPlayers().isEmpty(),
                    "late link-only replay cannot resurrect ended service, old members or raid HUD");
            restore(game,image,Set.of());require(manager.get(id)==raid&&raid.isActive()&&raid.getTotalRaidersAlive()==2,
                    "new full body replay resets link-phase guard and restores complete original raid again");
            var old=add(f,at.east(12));raid.joinRaid(f.level,captain.getWave(),old,null,true);
            var oldId=old.getUUID();old.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION);
            var replacement=new Pillager(EntityTypes.PILLAGER,f.level);replacement.setUUID(oldId);
            VanityActionGoalChecks.add(f,replacement,Vec3.atBottomCenterOf(at.east(13)));
            var historical=MatchActorCheckpoint.captureRegistered(game);raid.removeFromRaid(f.level,old,true);
            restore(game,historical,Set.of(oldId));
            require(old.isRemoved()&&replacement.isRemoved()&&old.getCurrentRaid()==raid&&replacement.getCurrentRaid()==null
                    &&raid.getTotalRaidersAlive()==3&&((Set<?>)((Map<?,?>)members).get(captain.getWave())).contains(old),
                    "older removed raid member stays historical when a distinct captured body sharing its UUID is omitted");
            restore(game,historical,Set.of());
            require(old.isRemoved()&&!replacement.isRemoved()&&old.getCurrentRaid()==raid&&replacement.getCurrentRaid()==null,
                    "historical member remains distinct when current same-UUID body returns");
        }finally{
            try{VanityActionGoalChecks.restore(game,before,player,Set.of());}
            finally{f.level.getGameRules().set(GameRules.RAIDS,raidsEnabled,game.server());}
        }
    }
    private static Pillager add(WoolBucketChecks.Fixture f,BlockPos pos){return VanityActionGoalChecks.add(f,new Pillager(EntityTypes.PILLAGER,f.level),Vec3.atBottomCenterOf(pos));}
    private static void links(GameController game,MatchActorCheckpoint image){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{require(image.restoreLinksFrozen().isEmpty(),"late raid/body links resolve");}finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    private static int recruit(Pillager captain,Pillager later,net.minecraft.world.entity.ai.goal.GoalSelector selector,Raid raid){
        later.setCanJoinRaid(true);int ticks=0;
        while(later.getCurrentRaid()==null&&ticks<24){((net.minecraft.server.level.ServerLevel)captain.level()).tickNonPassenger(captain);selector.tick();ticks++;}
        require(later.getCurrentRaid()==raid&&raid.getTotalRaidersAlive()==3,"real native recruitment changes body ownership and raid membership");return ticks;
    }
}
