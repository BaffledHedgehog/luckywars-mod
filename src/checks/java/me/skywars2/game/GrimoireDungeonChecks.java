package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;

/** Real raw drop, registry/template closure, accepted native jigsaw work and coordinator rewind. */
public final class GrimoireDungeonChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Grimoire dungeon: "+why);}
    private static Fixture running;
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final MatchCoreCheckpoint core;final List<MatchPlayerCheckpoint> players;
        final CompletableFuture<?> ready;
        ItemEntity token;int phase,frames;boolean sawAccepted,closed;
        Fixture(GameController game){
            this.game=game;require(!game.vanity().hasSnapshot()&&!game.vanity().busy()&&game.items().grimoireDungeon().pending()==0,"exclusive native structure fixture");
            core=new MatchCoreCheckpoint(game);players=game.server().getPlayerList().getPlayers().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();
            require(base.owner.level().registryAccess().lookupOrThrow(Registries.STRUCTURE).get(LbcGrimoireDungeon.STRUCTURE).isPresent(),"packaged native jigsaw registry");
            var futures=new ArrayList<CompletableFuture<?>>();
            int cx=base.owner.getBlockX()>>4,cz=base.owner.getBlockZ()>>4;
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)futures.add(game.maps().prepareChunk(base.owner.level(),new ChunkPos(cx+x,cz+z)));
            ready=CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
        void ack(){for(var p:game.server().getPlayerList().getPlayers())((MatchPlayerCheckpoint.SimulatedAcknowledgement)p.connection).skywars2$acknowledgeRestore();}
        boolean step(){require(++frames<2000,"bounded native structure construction/rewind phase="+phase+" "+game.vanity().status());ack();
            if(phase==0){if(!ready.isDone())return false;ready.join();require(game.vanity().requestCapture(base.owner,false),"full original native world point");phase=1;return false;}
            if(phase==1){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                var loot=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:item_defs/lbc/drop_not_trap__77de94f9f93e");
                require(loot.size()==1&&LbcGrimoireDungeon.matches(loot.getFirst()),"actual source canonical dungeon reward");
                var stack=loot.getFirst();stack.setCount(1);token=new ItemEntity(base.owner.level(),base.owner.getX(),base.owner.getY(),base.owner.getZ(),stack);token.setThrower(base.owner);token.setNoGravity(true);
                require(base.owner.level().addFreshEntity(token),"native actual drop insertion");phase=2;return false;}
            if(phase==2){
                if(game.items().grimoireDungeon().pending()>0){sawAccepted=true;require(!game.items().stonekivi().captureSettled(),"full construction cannot be captured halfway");return false;}
                if(!sawAccepted)return false;
                require(token.isRemoved(),"one source drop unit consumed");
                var markers=new ArrayList<Entity>();base.owner.level().getAllEntities().forEach(e->{if(!base.before.contains(e)&&e.entityTags().contains("grimoire_dungeon"))markers.add(e);});
                require(markers.size()>4,"original native jigsaw generated multiple real room markers");
                for(var marker:markers)require(!marker.entityTags().contains(LbcGrimoireDungeon.PENDING)&&game.items().spellSources().source(marker)!=null
                        &&game.items().spellSources().source(marker).author().equals(base.owner.getUUID()),"all room markers published after complete terrain with original author");
                require(game.vanity().requestRestoreForCheck(),"whole native dungeon rewind");phase=3;return false;
            }
            if(phase==3)return !game.vanity().busy()&&game.vanity().status().equals("IDLE");
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;game.vanity().stop();boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{game.items().stonekivi().stop();base.close();core.restoreFrozen();for(var player:players)player.restoreFrozen();}finally{game.server().tickRateManager().setFrozen(frozen);}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS grimoire-dungeon (actual source loot/drop and single debit, native jigsaw templates/registry, completed construction before room publication, author propagation and full native world rewind)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL grimoire-dungeon",error);}}
}
