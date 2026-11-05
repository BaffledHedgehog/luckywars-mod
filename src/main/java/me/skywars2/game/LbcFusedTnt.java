package me.skywars2.game;

import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

/** Native fuse/explosion own the complete clock; no parallel TNT ticking or NBT state. */
public final class LbcFusedTnt {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private LbcFusedTnt(){}
    public static boolean managed(Entity actor){return actor instanceof PrimedTnt&&actor.entityTags().contains("funny_tnt");}
    public static void exploding(GameController game,PrimedTnt source){
        if(!managed(source)||!(source.level() instanceof ServerLevel level)||!game.playing()||game.maps().busy()
                ||game.items().timeStop().stopped(level)||game.items().magic().mechanismSuppressedAt(level,source.position()))return;
        LivingEntity nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(var candidate:game.items().lifetimes().loadedTargets()){
            if(candidate.level()!=level||!candidate.getType().builtInRegistryHolder().is(MOBS)||!game.items().wands().validRoleTarget(candidate,false))continue;
            double next=candidate.position().distanceToSqr(source.position());if(next<distance){nearest=candidate;distance=next;}
        }
        // Failed facing selector in the source means no successor; the original native blast still occurs.
        if(nearest==null)return;
        var at=source.position().add(nearest.getEyePosition().subtract(source.position()).normalize().scale(.5));
        if(!level.isInValidBounds(net.minecraft.core.BlockPos.containing(at))||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(at)))return;
        var child=new PrimedTnt(EntityTypes.TNT,level);child.setPos(at);child.setFuse(30);child.setDeltaMovement(Vec3.ZERO);child.entityTags().addAll(Set.of("funny_tnt","instant"));
        var authority=game.items().spellSources().source(source);if(authority!=null)game.items().spellSources().bind(child,authority);
        game.items().timeStop().prepareFrozen(child);
        try(var ignored=game.items().spellSources().scope((LbcSpellSource.Source)null,level)){level.addFreshEntity(child);}
    }
}
