package me.skywars2.game;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.scores.ScoreHolder;

/** Original food/potion status consumers; the source scoreboard owns every remaining lifetime. */
public final class LbcFoodStatuses {
    private static final List<String> COUNTERS=List.of("all_seeing_eye","living_legend","dimensional_shifting","end_damage");
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final ColorParticleOption MOB_DOT=ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,ARGB.colorFromFloat(1,0,.007f,0));
    private static final ColorParticleOption PLAYER_DOT=ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,ARGB.colorFromFloat(1,0,.01f,0));
    private static final DustColorTransitionOptions LEGEND_DUST=new DustColorTransitionOptions(
            ARGB.colorFromFloat(1,1,1,1),ARGB.colorFromFloat(1,10,10,10),4);
    private static final DyeColor[] GLASS={DyeColor.WHITE,DyeColor.RED,DyeColor.BLACK,DyeColor.BLUE,
            DyeColor.LIGHT_BLUE,DyeColor.MAGENTA,DyeColor.PURPLE,DyeColor.YELLOW,DyeColor.PINK,
            DyeColor.BROWN,DyeColor.ORANGE,DyeColor.GREEN,DyeColor.LIME,DyeColor.CYAN,DyeColor.GRAY,DyeColor.LIGHT_GRAY};
    private final GameController game;
    private int lastTick=Integer.MIN_VALUE;

    public LbcFoodStatuses(GameController game){this.game=game;}
    private int read(ServerPlayer p,String objective){return game.items().mana().score(p,objective).get();}
    private void drain(ServerPlayer p,String objective){game.items().mana().score(p,objective).add(-1);}
    private boolean admitted(ServerPlayer p){
        return game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
                &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)
                &&LbcDimensionItems.ordinary(p)&&!game.items().timeStop().stopped(p.level())
                &&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(p);
    }
    /** Called once from the shared actual server tick, without a separate food lifetime clock. */
    public void tick(){
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        for(var p:List.copyOf(game.server().getPlayerList().getPlayers())){
            if(!admitted(p))continue;
            if(!game.items().magic().suppressedAt(p.level(),p.position())){
                if(read(p,"all_seeing_eye")>0)allSeeingEye(p);
                if(read(p,"living_legend")>0)livingLegend(p);
            }
            if(now%20==0&&read(p,"dimensional_shifting")>0)shifting(p);
            if(now%200==0&&p.level().dimension().equals(Level.END)&&!LbcGreed.active(p))endDamage(p);
        }
    }
    /** Original players_10s End consequence; the same ledger is also changed by stew/KFC. */
    private void endDamage(ServerPlayer p){
        int value=(read(p,"end_damage")+1)*6/5;
        game.items().mana().score(p,"end_damage").set(value);
        if(read(p,"void_resistance")<=0&&value>0)p.hurtServer(p.level(),new net.minecraft.world.damagesource.DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.OUTSIDE_BORDER)),value);
    }
    private void allSeeingEye(ServerPlayer p){
        var level=p.level();
        for(var entity:level.getAllEntities()){
            if(entity instanceof ServerPlayer||!entity.getType().builtInRegistryHolder().is(MOBS)
                    ||entity.position().distanceToSqr(p.position())<.0001)continue;
            level.sendParticles(p,MOB_DOT,true,false,entity.getX(),entity.getY()+.5,entity.getZ(),0,0,0,0,1);
        }
        for(var target:level.players()){
            if(target.isSpectator()||target.position().distanceToSqr(p.position())<.0001)continue;
            // This source helper is a full twenty-point silhouette, not Techno's single helmet dot.
            for(int i=0;i<20;i++)level.sendParticles(p,PLAYER_DOT,true,false,target.getX(),target.getY()+i*.1,target.getZ(),0,0,0,0,1);
        }
        var origin=p.position().add(0,5,0);
        if(!game.items().magic().mechanismSuppressedAt(level,origin)){
            ServerPlayer viewer=null;double nearest=Double.POSITIVE_INFINITY;
            for(var candidate:level.players())if(!candidate.isSpectator()){
                double distance=candidate.position().distanceToSqr(origin);
                if(distance<nearest){nearest=distance;viewer=candidate;}
            }
            if(viewer!=null)for(var target:level.players()){
                if(target.isSpectator()||target.position().distanceToSqr(p.position())<2500)continue;
                LbcTechnoArmor.directionParticles(viewer,origin,target.getEyePosition().subtract(origin).normalize(),PLAYER_DOT);
            }
        }
        p.removeEffect(MobEffects.DARKNESS);p.removeEffect(MobEffects.BLINDNESS);p.removeEffect(MobEffects.NAUSEA);
        game.items().mana().award(p,"lbc:true_advancements/potions/effect_all_seeing_eye");
        drain(p,"all_seeing_eye");
    }
    private void livingLegend(ServerPlayer p){
        var level=p.level();int color=level.getRandom().nextInt(16);
        game.items().mana().score(ScoreHolder.forNameOnly("#random16"),"lbc.math").set(color+1);
        var center=BlockPos.containing(p.position().add(0,-1,0));
        var from=center.offset(-3,0,-3);var to=center.offset(3,0,3);
        // /fill resolves both endpoints through native loaded BlockPos arguments before writing.
        if(level.hasChunkAt(from)&&level.hasChunkAt(to))game.items().indirectKills().trapChanges(level,game.items().indirectKills().wildCause(p),()->
                LbcDropBlocks.fill(level,from,to,Blocks.STAINED_GLASS.pick(GLASS[color]),true,state->state.is(AIRS)));
        level.sendParticles(LEGEND_DUST,p.getX(),p.getY()+2,p.getZ(),40,2,2,2,40);
        drain(p,"living_legend");
        game.items().mana().award(p,"lbc:true_advancements/potions/effect_living_legend");
    }
    private void shifting(ServerPlayer p){
        var random=p.level().getRandom();
        if(random.nextFloat()<.05f){
            int roll=random.nextInt(4)+1;
            game.items().mana().score(ScoreHolder.forNameOnly("rand2"),"lbc.math").set(roll);
            // The source's top dispatcher rechecks the executor after each preceding transition.
            // Each actual transfer remains owned by the existing shared destination engine.
            for(var origin:List.of(Level.OVERWORLD,Level.NETHER,Level.END,LbcHedgehogDimensions.IMPRINTED))
                if(p.level().dimension().equals(origin))game.items().hedgehogDimensions().teleport(p,p,roll,random);
        }
        drain(p,"dimensional_shifting");
        game.items().mana().award(p,"lbc:true_advancements/potions/effect_shifting");
    }
    /** Source death/leave resets; temporary suppression only pauses and never calls this method. */
    public void clear(ServerPlayer p){for(var objective:COUNTERS)game.items().mana().score(p,objective).set(0);}
    public void forget(ServerPlayer p){clear(p);}
    public void stop(){for(var p:game.server().getPlayerList().getPlayers())clear(p);lastTick=Integer.MIN_VALUE;}
}
