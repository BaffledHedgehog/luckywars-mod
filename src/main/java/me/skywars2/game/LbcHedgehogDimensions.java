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

/** Real source destinations. The isolated match arena has the source overworld's logical role. */
public final class LbcHedgehogDimensions {
    static final ResourceKey<Level> NEXUS=key("minecraft:nexus"), IMPRINTED=key("minecraft:imprinted");
    private final GameController game;
    public LbcHedgehogDimensions(GameController game){this.game=game;}
    private static ResourceKey<Level> key(String id){return ResourceKey.create(Registries.DIMENSION,Identifier.parse(id));}
    public boolean teleport(LivingEntity target,ServerPlayer owner){return teleport(target,owner,owner.getRandom().nextInt(4)+1);}
    ServerLevel destination(ServerLevel from,int roll){
        if(roll<1||roll>4)throw new IllegalArgumentException("Hedgehog dimension roll must be 1..4");
        var origin=from.dimension();
        if(from==game.maps().level()||origin.equals(Level.OVERWORLD))return world(switch(roll){case 1->Level.NETHER;case 2->Level.END;case 3->NEXUS;default->IMPRINTED;});
        if(origin.equals(Level.NETHER))return world(switch(roll){case 1->Level.OVERWORLD;case 2->Level.END;case 3->NEXUS;default->IMPRINTED;});
        if(origin.equals(Level.END))return world(switch(roll){case 1->Level.OVERWORLD;case 2->Level.NETHER;case 3->NEXUS;default->IMPRINTED;});
        if(origin.equals(IMPRINTED))return world(switch(roll){case 1->Level.NETHER;case 2->Level.END;case 3->NEXUS;default->Level.OVERWORLD;});
        // The authored dispatcher has no Nexus handler; unsupported dimensions also remain unchanged.
        return null;
    }
    private ServerLevel world(ResourceKey<Level> key){return key.equals(Level.OVERWORLD)?game.maps().level():game.server().getLevel(key);}
    boolean teleport(LivingEntity target,ServerPlayer owner,int roll){
        return transfer(target,owner,roll)!=null;
    }
    /** Actual native arrival also exists when its destination section is not entity-visible. */
    LivingEntity transfer(LivingEntity target,ServerPlayer owner,int roll){
        if(target==null||owner==null||!game.canUseGameplayItems(owner)||!game.items().wands().validTarget(target)
                ||target instanceof ServerPlayer player&&player!=owner&&owner.isAlliedTo(player)
                ||!(target.level() instanceof ServerLevel from))return null;
        var destination=destination(from,roll);
        if(destination==null||destination==from)return null;
        boolean nexus=destination.dimension().equals(NEXUS);
        double scale=DimensionType.getTeleportationScale(from.dimensionType(),destination.dimensionType());
        Vec3 point=nexus?new Vec3(owner.getRandom().nextInt(9601)-4800,2332,owner.getRandom().nextInt(9601)-4800)
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
