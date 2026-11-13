package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** Full completed server-tick timings with 24 actual Carpet players, not a timer around the AI method. */
final class BotPopulationChecks {
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot population: "+why);}
    private static final class Fixture extends CarpetBotChecks.Fixture {
        final List<Long> baseline=new ArrayList<>(),looting=new ArrayList<>(),combat=new ArrayList<>();
        final Map<UUID,Vec3> start=new LinkedHashMap<>();final Set<UUID> acted=new HashSet<>(),preexisting=new HashSet<>();
        final List<BlockPos> starters=new ArrayList<>();PlayerTeam team;int at,spawnedAt,minimumCombat=24;boolean released;
        Fixture(GameController game){super(game,new Vec3(.5,120,.5));}
        @Override void setup(){for(var actor:base.level.getAllEntities())preexisting.add(actor.getUUID());super.setup();disconnect(bot);actors.clear();
            if(base.level.getBlockEntity(firstChest) instanceof net.minecraft.world.Container old)old.clearContent();
            base.set(firstChest,Blocks.AIR.defaultBlockState());at=game.server().getTickCount();
        }
        void population(){
            var center=BlockPos.containing(origin);
            for(int x=(center.getX()-25)>>4;x<=(center.getX()+77)>>4;x++)for(int z=(center.getZ()-25)>>4;z<=(center.getZ()+38)>>4;z++){
                long key=ChunkPos.pack(x,z);if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}base.level.getChunk(x,z);
            }
            for(int x=-24;x<=75;x++)for(int z=-24;z<=37;z++)for(int y=-2;y<=4;y++)
                base.set(center.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            var board=game.server().getScoreboard();require(board.getPlayerTeam("botBenchmark")==null,"owned temporary team name unused");
            team=board.addPlayerTeam("botBenchmark");
            for(int i=0;i<24;i++){
                var spot=origin.add((i%8)*10,0,(i/8)*12);
                var p=create(String.format(Locale.ROOT,"botBench%02d",i),spot);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.getFoodData().setFoodLevel(20);
                board.addPlayerToTeam(p.getScoreboardName(),team);start.put(p.getUUID(),spot);
                for(int j=0;j<3;j++){
                    var pos=BlockPos.containing(spot).offset(j==2?3:0,0,j==0?-2:2);starters.add(pos);base.set(pos,Blocks.CHEST.defaultBlockState());
                    var chest=(ChestBlockEntity)base.level.getBlockEntity(pos);chest.clearContent();
                    chest.setLootTable(MapManager.chestLootTable(2,"default",false));chest.setLootTableSeed(11000+i*3+j);
                    chest.getItem(0);MapManager.normalizeLootCounts(chest);
                    // A repeatable population measurement excludes random trap payloads;
                    // the separate full trap suites retain all ordinary hazards and 20 HP.
                    for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).getItem() instanceof net.minecraft.world.item.BlockItem block
                            &&LbcLuckyBlocks.kind(block.getBlock().defaultBlockState())!=null)chest.setItem(slot,net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            require(actors.size()==24&&actors.stream().allMatch(p->p.isAlive()&&game.allowDamage(p)&&LbcCarpetBots.bot(p)),"24 native live participants with ordinary HP");
            spawnedAt=game.server().getTickCount();
        }
        long previousTick(){int tick=game.server().getTickCount()-1;return game.server().getTickTimesNanos()[Math.floorMod(tick,100)];}
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            if(start.isEmpty()){
                if(now-at>20)baseline.add(previousTick());
                if(now-at>=100)population();return false;
            }
            int elapsed=now-spawnedAt;
            int live=0;
            for(var entry:start.entrySet()){
                var p=game.server().getPlayerList().getPlayer(entry.getKey());if(p==null||!p.isAlive()||!game.allowDamage(p))continue;live++;
                var observation=game.carpetBots().observation(p);
                if(p.position().distanceToSqr(entry.getValue())>.25||observation.looted()>0||observation.attempts()>0)acted.add(p.getUUID());
            }
            if(!released){
                require(live==24,"all 24 bots remain alive during the shared-team native looting phase");
                if(elapsed>100)looting.add(previousTick());
                if(elapsed<400)return false;
                require(acted.size()==24,"every one of the 24 scheduled bots actually moves, loots or attacks");
                for(var actor:actors)game.server().getScoreboard().removePlayerFromTeam(actor.getScoreboardName());released=true;
                return false;
            }
            minimumCombat=Math.min(minimumCombat,live);combat.add(previousTick());
            if(elapsed<600)return false;
            require(baseline.size()>=75&&looting.size()>=290&&combat.size()>=190,"full native tick sample windows, excluding population startup");
            SkyWars2.LOGGER.info("[check] bot-population-24 metrics: baseline={} looting24={} combat={} minimumLiveCombat={} startupExcluded=true fullServerTicks=true",
                    metrics(baseline),metrics(looting),metrics(combat),minimumCombat);
            return true;
        }
        @Override public void close(){
            for(var actor:base.level.getAllEntities())if(!(actor instanceof ServerPlayer)&&!preexisting.contains(actor.getUUID())&&actor.position().distanceToSqr(origin)<160*160)base.owned.add(actor.getUUID());
            Throwable failure=null;try{super.close();}catch(Throwable error){failure=error;}
            var board=game.server().getScoreboard();if(team!=null&&board.getPlayerTeam(team.getName())==team)board.removePlayerTeam(team);
            if(failure!=null)throw new IllegalStateException("Population cleanup",failure);
        }
    }
    private static Map<String,Object> metrics(List<Long> values){
        var sorted=values.stream().mapToLong(Long::longValue).sorted().toArray();
        return Map.of("samples",sorted.length,"meanMs",Arrays.stream(sorted).average().orElseThrow()/1e6,
                "p95Ms",sorted[(int)Math.floor((sorted.length-1)*.95)]/1e6,"p99Ms",sorted[(int)Math.floor((sorted.length-1)*.99)]/1e6,"maxMs",sorted[sorted.length-1]/1e6);
    }
    static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS bot-population-24 (actual native 24-player looting and combat, completed full-server tick measurements, scoped cleanup)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL bot-population-24",error);}
    }
}
