package me.skywars2.game;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Author Q5: base-health revivals; canonical participant lives never become a parallel health counter. */
public final class LbcPhoenixSkill {
    private record State(String epoch,double base,boolean pending,boolean terminal){}
    private final GameController game;
    private Map<UUID,State> members=new LinkedHashMap<>();
    private boolean restoring;
    public LbcPhoenixSkill(GameController game){this.game=game;}

    public boolean active(UUID id){var state=members.get(id);return state!=null&&game.items().currentEpoch(state.epoch);}
    public double pendingBaseHealth(UUID id){var state=members.get(id);return state==null?Double.NaN:state.base;}
    private static double base(ServerPlayer p){return p.getAttribute(Attributes.MAX_HEALTH).getBaseValue();}
    private static void applyBase(ServerPlayer p,double value){
        if(!Double.isFinite(value)||value<=0)throw new IllegalStateException("Invalid Phoenix base health");
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(value);
    }
    /** Initial match start after all loadout/base-HP benefits; caller replaces its life count with the return value. */
    public int start(ServerPlayer p,int canonicalLives){
        if(restoring||!game.lbc().hasSkill(p,31))return canonicalLives;
        if(active(p.getUUID()))return 1;
        enroll(p,canonicalLives);return 1;
    }
    void enroll(ServerPlayer p,int canonicalLives){
        if(canonicalLives<1)throw new IllegalArgumentException("Phoenix start needs a living participant");
        if(active(p.getUUID()))throw new IllegalStateException("Phoenix already started");
        double value=base(p)+5d*(canonicalLives-1L);
        members.put(p.getUUID(),new State(game.items().epoch(),value,false,value<=1));applyBase(p,value);
    }
    /** Positive canonical life grants become base HP. Return the consumed count; no per-second score correction. */
    public int convertLives(ServerPlayer p,int amount){
        if(restoring||amount<=0||!active(p.getUUID()))return 0;
        var state=members.get(p.getUUID());
        // A late accepted grant cannot resurrect a terminal participant or queue ordinary extra lives.
        if(state.terminal)return amount;
        double value=(state.pending?state.base:base(p))+5d*amount;
        members.put(p.getUUID(),new State(state.epoch,value,state.pending,false));
        if(!state.pending)applyBase(p,value);
        return amount;
    }
    /** Call once at the actual native death decision, before ordinary cleanup and any bed/life debit. */
    public boolean beforeDeath(ServerPlayer p){
        if(restoring||!active(p.getUUID()))return false;
        var state=members.get(p.getUUID());if(state.terminal)return false;
        if(state.pending)return true; // Canonical duplicate death admission cannot halve twice.
        double value=base(p)*.5d;
        boolean terminal=!Double.isFinite(value)||value<=1;
        members.put(p.getUUID(),new State(state.epoch,value,!terminal,terminal));
        return !terminal;
    }
    /** After native ordinary/BedWars respawn and loadout reset, restore the one accepted base-HP result. */
    public void bodyReady(ServerPlayer p){
        if(restoring||!active(p.getUUID()))return;
        var state=members.get(p.getUUID());if(state.terminal)return;
        applyBase(p,state.base);
        if(state.pending)p.setHealth(p.getMaxHealth());
        members.put(p.getUUID(),new State(state.epoch,state.base,false,false));
    }
    public void forget(ServerPlayer p){members.remove(p.getUUID());}
    public void stop(){members.clear();}
    void prepareFrozen(){restoring=true;}
    void publishFrozen(){restoring=false;}
    void cancelFrozen(){restoring=false;}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){return new Checkpoint();}
    final class Checkpoint {
        final Map<UUID,State> image=Collections.unmodifiableMap(new LinkedHashMap<>(members));
        Runnable prepare(MatchSpellCheckpoint.Links links){return ()->members=new LinkedHashMap<>(image);}
    }
}
