package me.skywars2.game;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

/** Skill34: source second pulses and native, attempt-local damage statistics. */
public final class LbcStoneGolemSkill {
    private record State(int layers,int charge) {}
    private static final class Attempt {
        final UUID owner;long taken,resisted;boolean handled;
        Attempt(ServerPlayer player){owner=player.getUUID();}
    }
    private final GameController game;
    private final Predicate<ServerPlayer> admission;
    private final int secondLayerThresholdUnits;
    private final Map<UUID,State> states=new LinkedHashMap<>();
    private final Map<ServerLevel,Integer> phases=new IdentityHashMap<>();
    private final Deque<Attempt> attempts=new ArrayDeque<>();
    private int lastTick=Integer.MIN_VALUE;
    private boolean restoring;

    public LbcStoneGolemSkill(GameController game,Predicate<ServerPlayer> admission,int secondLayerThresholdHP){
        if(secondLayerThresholdHP<=0||secondLayerThresholdHP>Integer.MAX_VALUE/10)throw new IllegalArgumentException("layer2 HP threshold");
        this.game=game;this.admission=Objects.requireNonNull(admission);secondLayerThresholdUnits=secondLayerThresholdHP*10;
    }
    public int layers(ServerPlayer player){var s=states.get(player.getUUID());return s==null?0:s.layers;}
    public int charge(ServerPlayer player){var s=states.get(player.getUUID());return s==null?0:s.charge;}
    int phase(ServerLevel level){return phases.getOrDefault(level,0);}
    int secondLayerThresholdHP(){return secondLayerThresholdUnits/10;}
    private boolean owner(ServerPlayer p){return game.playing()&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()
            &&!game.isObserver(p)&&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p.level())&&game.lbc().hasSkill(p,34);}
    private boolean paused(ServerPlayer p){return !game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().stopped(p.level())||game.items().timeStop().frozen(p)
            ||game.items().wands().inputTimeStopped(p);}
    private boolean active(ServerPlayer p){return !restoring&&owner(p)&&!paused(p)&&admission.test(p);}

    public void tick(){
        int now=game.server().getTickCount();if(restoring||lastTick==now)return;lastTick=now;
        states.keySet().removeIf(id->{var p=game.server().getPlayerList().getPlayer(id);return p!=null&&!owner(p);});
        if(!game.playing())return;
        for(var p:game.server().getPlayerList().getPlayers())if(owner(p)){
            states.putIfAbsent(p.getUUID(),new State(0,0));phases.putIfAbsent(p.level(),0);
        }
        for(var level:List.copyOf(phases.keySet())){
            if(states.keySet().stream().noneMatch(id->{var p=game.server().getPlayerList().getPlayer(id);return p!=null&&p.level()==level;})){
                if(states.isEmpty())phases.remove(level);continue;
            }
            if(!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(level))continue;
            int phase=(phases.get(level)+1)%20;phases.put(level,phase);if(phase!=0)continue;
            for(var id:List.copyOf(states.keySet())){
                var p=game.server().getPlayerList().getPlayer(id);if(p==null||p.level()!=level||!active(p))continue;
                var s=states.get(id);int layers=s.layers,charge=s.charge;
                if(layers<5&&++charge>=10){layers++;charge=0;feedback(p,true);}
                states.put(id,new State(layers,charge));
                if(layers>0)p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40,layers-1,false,false));
            }
        }
    }
    private void feedback(ServerPlayer p,boolean added){
        p.level().playSound(null,p.blockPosition(),added?SoundEvents.STONE_BREAK:SoundEvents.IRON_GOLEM_HURT,
                SoundSource.PLAYERS,1,added?1:1.5f);
        p.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,(added?Blocks.STONE:Blocks.IRON_ORE).defaultBlockState()),
                p.getX(),p.getY()+1,p.getZ(),added?50:60,0.5,0.8,0.5,0.1);
    }
    /** The shared native hurt wrapper owns the scope; nested same-player calls own separate awards. */
    public boolean withIncomingAttempt(ServerPlayer player,BooleanSupplier damage){
        if(!active(player))return damage.getAsBoolean();
        var frame=new Attempt(player);attempts.push(frame);
        try{return damage.getAsBoolean();}finally{if(attempts.pop()!=frame)throw new IllegalStateException("Stone attempt nesting");}
    }
    private Attempt attempt(ServerPlayer p){for(var frame:attempts)if(frame.owner.equals(p.getUUID()))return frame;return null;}
    /** Called from native awardStat, using its raw count before lifetime-stat saturation. */
    public void statistic(ServerPlayer player,Stat<?> stat,int count){
        if(count<=0)return;var frame=attempt(player);if(frame==null||frame.handled)return;
        if(stat==Stats.CUSTOM.get(Stats.DAMAGE_TAKEN))frame.taken+=count;
        else if(stat==Stats.CUSTOM.get(Stats.DAMAGE_RESISTED))frame.resisted+=count;
    }
    /** The native incoming criterion also fires for Resistance V and shield zero-HP impacts. */
    public void incoming(ServerPlayer player,DamageSource source,float original,float actual,boolean blocked){
        if(!active(player))return;var frame=attempt(player);if(frame!=null&&frame.handled)return;
        long taken=frame==null?(Float.isFinite(actual)&&actual>0?Math.round(actual*10f):0):frame.taken;
        long resisted=frame==null?0:frame.resisted;
        if(frame!=null)frame.handled=true;
        int layers=layers(player);
        boolean remove=switch(layers){
            case 1->taken>=80;
            case 2->taken>=secondLayerThresholdUnits;
            case 3->resisted>=80;
            case 4->resisted>=100;
            case 5->player.getRandom().nextBoolean();
            default->false;
        };
        states.put(player.getUUID(),new State(remove?layers-1:layers,0));
        if(remove)feedback(player,false);
    }
    /** Effect expiry belongs to vanilla: never strip foreign or stronger resistance. */
    public void clear(ServerPlayer player){if(!restoring&&!MatchPlayerCheckpoint.restoring(player))states.remove(player.getUUID());}
    public void stop(){states.clear();phases.clear();lastTick=Integer.MIN_VALUE;restoring=false;}
    public void prepareFrozen(){boundary();restoring=true;states.clear();phases.clear();}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    private void boundary(){if(!attempts.isEmpty())throw new IllegalStateException("Stone checkpoint inside native damage attempt");}
    Checkpoint checkpoint(){boundary();return new Checkpoint();}
    final class Checkpoint {
        private final Map<UUID,State> saved=Map.copyOf(states);
        private final Map<ServerLevel,Integer> clocks=new IdentityHashMap<>(phases);
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            return new MatchFieldCheckpoint.Plan(Set.of(),()->{states.clear();states.putAll(saved);phases.clear();phases.putAll(clocks);});
        }
    }
}
