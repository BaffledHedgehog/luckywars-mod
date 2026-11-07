package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Native legacy destinations: the map itself is the actual Overworld. */
public final class LbcHedgehogDimensions {
    static final ResourceKey<Level> NEXUS=key("minecraft:nexus"), IMPRINTED=key("minecraft:imprinted");
    static final String RETURN_COUNTER="imprinted_return";
    static final int RETURN_TICKS=45*20;
    private final GameController game;
    public LbcHedgehogDimensions(GameController game){this.game=game;}
    private static ResourceKey<Level> key(String id){return ResourceKey.create(Registries.DIMENSION,Identifier.parse(id));}
    public boolean teleport(LivingEntity target,ServerPlayer owner){return teleport(target,owner,owner.getRandom().nextInt(4)+1);}
    ServerLevel destination(ServerLevel from,int roll){
        if(roll<1||roll>4)throw new IllegalArgumentException("Hedgehog dimension roll must be 1..4");
        var origin=from.dimension();
        if(origin.equals(Level.OVERWORLD))return world(switch(roll){case 1->Level.NETHER;case 2->Level.END;case 3->NEXUS;default->IMPRINTED;});
        if(origin.equals(Level.NETHER))return world(switch(roll){case 1->Level.OVERWORLD;case 2->Level.END;case 3->NEXUS;default->IMPRINTED;});
        if(origin.equals(Level.END))return world(switch(roll){case 1->Level.OVERWORLD;case 2->Level.NETHER;case 3->NEXUS;default->IMPRINTED;});
        if(origin.equals(IMPRINTED))return world(switch(roll){case 1->Level.NETHER;case 2->Level.END;case 3->NEXUS;default->Level.OVERWORLD;});
        if(origin.equals(NEXUS))return world(switch(roll){case 1->Level.OVERWORLD;case 2->Level.NETHER;case 3->Level.END;default->IMPRINTED;});
        return null;
    }
    private ServerLevel world(ResourceKey<Level> key){return game.server().getLevel(key);}
    /** Residence belongs to the player, regardless of the ability that brought them here. */
    public void changedWorld(ServerPlayer player,ServerLevel from,ServerLevel to){
        if(from==to||game.server().tickRateManager().isFrozen())return;
        var mana=game.items().mana();
        if(to.dimension().equals(IMPRINTED))mana.score(player,RETURN_COUNTER).set(RETURN_TICKS);
        else if(mana.hudValue(player,RETURN_COUNTER)>0)mana.score(player,RETURN_COUNTER).set(0);
    }
    void tickReturns(){
        if(!game.server().tickRateManager().runsNormally())return;
        var mana=game.items().mana();
        for(var player:game.server().getPlayerList().getPlayers()){
            int left=mana.hudValue(player,RETURN_COUNTER);
            if(!player.level().dimension().equals(IMPRINTED)){
                if(left>0)mana.score(player,RETURN_COUNTER).set(0);
                continue;
            }
            if(!player.isAlive()||player.isRemoved())continue;
            // Also covers logins already in this dimension and teleports during native freeze.
            if(left<=0){mana.score(player,RETURN_COUNTER).set(RETURN_TICKS);continue;}
            if(game.items().timeStop().stopped(player.level()))continue;
            if(left>1){mana.score(player,RETURN_COUNTER).set(left-1);continue;}
            var overworld=game.server().overworld();
            var arrival=player.teleport(new TeleportTransition(overworld,player.position(),player.getDeltaMovement(),
                    player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));
            if(arrival!=null){
                mana.score(player,RETURN_COUNTER).set(0);
                LbcOverload.sound(overworld,player.position(),"minecraft:block.portal.travel",1,1,32,0);
            }
        }
    }
    boolean teleport(LivingEntity target,ServerPlayer owner,int roll){
        return transfer(target,owner,roll)!=null;
    }
    /** Timed dimension shifting supplies the source world's RNG; the destination engine stays shared. */
    boolean teleport(LivingEntity target,ServerPlayer owner,int roll,net.minecraft.util.RandomSource random){
        return transfer(target,owner,roll,random)!=null;
    }
    /** Actual native arrival also exists when its destination section is not entity-visible. */
    LivingEntity transfer(LivingEntity target,ServerPlayer owner,int roll){
        return owner==null?null:transfer(target,owner,roll,owner.getRandom());
    }
    private LivingEntity transfer(LivingEntity target,ServerPlayer owner,int roll,net.minecraft.util.RandomSource random){
        if(target==null||owner==null||!game.canUseGameplayItems(owner)||!game.items().wands().validTarget(target)
                ||target instanceof ServerPlayer player&&player!=owner&&owner.isAlliedTo(player)
                ||!(target.level() instanceof ServerLevel from))return null;
        return transferTo(target,owner,from,destination(from,roll),random);
    }
    /** Oreshek calls the same Nexus transfer directly, without another dimension roll. */
    boolean nexus(LivingEntity target,ServerPlayer owner,net.minecraft.util.RandomSource random){
        if(target==null||owner==null||!game.canUseGameplayItems(owner)||!game.items().wands().validTarget(target)
                ||!(target.level() instanceof ServerLevel from))return false;
        return transferTo(target,owner,from,world(NEXUS),random)!=null;
    }
    /** Source altitude entry is environmental, independent of a usable item or a spell owner. */
    boolean heightNexus(ServerPlayer player){
        return transferTo(player,player,player.level(),world(NEXUS),player.getRandom())!=null;
    }
    private LivingEntity transferTo(LivingEntity target,ServerPlayer owner,ServerLevel from,ServerLevel destination,
            net.minecraft.util.RandomSource random){
        if(destination==null||destination==from)return null;
        boolean nexus=destination.dimension().equals(NEXUS);
        double scale=DimensionType.getTeleportationScale(from.dimensionType(),destination.dimensionType());
        Vec3 point=nexus?new Vec3(random.nextInt(9601)-4800,2332,random.nextInt(9601)-4800)
                :new Vec3(target.getX()*scale,target.getY(),target.getZ()*scale);
        if(!Level.isInSpawnableBounds(BlockPos.containing(point)))return null;
        // Native teleports intentionally acquire the destination chunk; no portal/safe-surface relocation.
        destination.getChunkAt(BlockPos.containing(point));
        var moved=target.teleport(new TeleportTransition(destination,point,target.getDeltaMovement(),
                target.getYRot(),target.getXRot(),TeleportTransition.DO_NOTHING));
        if(!(moved instanceof LivingEntity arrival))return null;
        if(arrival instanceof PathfinderMob mob)mob.getNavigation().stop();
        if(nexus){var gravity=arrival.getAttribute(Attributes.GRAVITY);if(gravity!=null)gravity.removeModifier(Identifier.parse("minecraft:antigravity_effect"));}
        game.items().indirectKills().teleported(arrival,owner,false);
        return arrival;
    }
}
