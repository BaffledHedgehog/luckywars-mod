package me.skywars2.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Both rainbow producers share a replayable per-cast sample cursor and current author lookup. */
final class LbcRainbowRay implements LbcExasperato.ReplayableRay {
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private final GameController game;private final UUID author;private final String epoch;
    private final Vec3 origin,direction;private final int color,tint;private double next;
    LbcRainbowRay(GameController game,ServerPlayer owner,Vec3 origin,Vec3 direction,int color,int tint){this.game=game;author=owner.getUUID();epoch=game.items().epoch();this.origin=origin;this.direction=direction;this.color=color;this.tint=tint;}
    private ServerPlayer owner(){return game.server().getPlayerList().getPlayer(author);}
    public boolean block(BlockPos pos,BlockState state){return !state.is(AIRS);}
    public boolean target(Entity entity){var owner=owner();return owner!=null&&LbcGaycaster.target(game,owner,entity);}
    public void trail(Vec3 from,Vec3 to){var owner=owner();if(owner==null)return;double end=origin.distanceTo(to);while(next<=end&&next<125){var point=origin.add(direction.scale(next));var dust=color==0?LbcGaycaster.dust(LbcGaycaster.hue(next),1.5f):new net.minecraft.core.particles.DustParticleOptions(tint,1.5f);owner.level().sendParticles(dust,true,false,point.x,point.y,point.z,1,0,0,0,0);next+=.5;}}
    public void complete(HitResult hit){var owner=owner();if(hit==null||owner==null||!game.items().currentEpoch(epoch))return;var direct=hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity living?living:null;var point=hit.getLocation();if(color==0)LbcGaycaster.impact(game,owner,point,direction,LbcGaycaster.hue(origin.distanceTo(point)),direct);else game.items().gaycasterAcademy().impact(owner,point,direction,color,direct);}
    public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){double sample=next;return ignored->()->next=sample;}
}
