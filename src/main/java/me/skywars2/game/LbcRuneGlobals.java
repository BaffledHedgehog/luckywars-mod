package me.skywars2.game;

import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Global rune effects enter the existing match controller and accepted terrain dispatcher. */
final class LbcRuneGlobals implements LbcImbaRunes.GlobalActions {
    private final GameController game;
    LbcRuneGlobals(GameController game){this.game=game;}
    public void restart(ServerPlayer author){game.restartFromRune(author);}
    public void addLives(ServerPlayer author,int amount){game.addRuneLives(author,amount);}
    public void clearMap(ServerPlayer author){
        var level=author.level();var epoch=game.items().epoch();var cause=game.items().indirectKills().wildCause(author);
        int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
                |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
        var area=new BlockAreaWork(level,state->!state.isAir(),(pos,state)->Blocks.AIR.defaultBlockState(),flags,false,true);
        area.move(new BlockAreaWork.Bounds(-192,0,-192,192,319,192),true);
        game.items().stonekivi().queuedWork(new LbcStonekivi.Work(){
            public boolean step(){
                if(!game.items().currentEpoch(epoch)){area.clear();return true;}
                if(game.items().timeStop().stopped(level))return false;
                long deadline=EffectTerrain.deadlineNanos();int remaining=1_048_576;
                while(area.hasWork()&&remaining>0&&System.nanoTime()<deadline){
                    int used=area.step(game,author,cause,Math.min(4096,remaining),deadline);
                    if(used==0)break;remaining-=used;
                }
                return !area.hasWork();
            }
            public void cancel(){area.clear();}
        });
        clearEntities(level,cause);
        game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#mapstat"),"swrg.math").set(1);
    }
    /** Shared Hagalaz entity phase; independent of its queued terrain sweep. */
    void clearEntities(ServerLevel level,LbcIndirectKills.Owner cause){
        // The source repeats native cleanup for passengers/drops and keeps only its lobby whitelist.
        for(int pass=0;pass<6;pass++){
            var entities=new java.util.ArrayList<Entity>();level.getAllEntities().forEach(entities::add);
            for(var entity:entities){var tags=entity.entityTags();
                boolean maploader=tags.stream().anyMatch(Set.of("swrg.clear","swrg.look","swrg.spawn","swrg.center","swrg.midle","swrg.default","map_object")::contains);
                boolean lobby=entity instanceof ServerPlayer||tags.stream().anyMatch(Set.of("swrg.quickstart","swrg.look","swrg.spawn","swrg.center","swrg.midle","swrg.default","map_object","strict_map_object")::contains)
                        ||entity instanceof AreaEffectCloud&&(tags.contains("swrg.delete")||tags.contains("swrg.lobby"));
                if(maploader||!lobby){
                    // Map destruction retires Chef and his chunk tickets instead of starting another knock.
                    if(game.items().wtf().erase(entity))continue;
                    if(entity instanceof LivingEntity living)game.items().indirectKills().inheritedKill(living,cause);
                    else entity.kill(level);
                }
            }
        }
    }
}
