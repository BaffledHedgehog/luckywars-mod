package me.skywars2.game.entities.projectiles;

import java.util.UUID;

import me.skywars2.game.GameController;
import me.skywars2.game.items.wands.LbcExasperato;
import me.skywars2.game.items.wands.LbcGaycaster;
import me.skywars2.game.items.wands.LbcExasperato.ReplayableRay;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint.Image;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links;
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
public final class LbcRainbowRay implements LbcExasperato.ReplayableRay {
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private final GameController game;private final UUID author;private final String epoch;
    private final Vec3 origin,direction;private final int color,tint;private double next;
    private final me.skywars2.game.combat.LbcIndirectKills.Owner cause;
    public LbcRainbowRay(GameController game,ServerPlayer owner,Vec3 origin,Vec3 direction,int color,int tint){this.game=game;author=owner.getUUID();epoch=game.items().epoch();this.origin=origin;this.direction=direction;this.color=color;this.tint=tint;cause=game.items().indirectKills().coldCause(owner,true);}
    private ServerPlayer owner(){return game.server().getPlayerList().getPlayer(author);}
    public boolean block(BlockPos pos,BlockState state){return !state.is(AIRS);}
    public boolean target(Entity entity){var owner=owner();return owner!=null&&LbcGaycaster.target(game,owner,entity);}
    public void trail(Vec3 from,Vec3 to){var owner=owner();if(owner==null)return;double end=origin.distanceTo(to);while(next<=end&&next<125){var point=origin.add(direction.scale(next));var dust=color==0?LbcGaycaster.dust(LbcGaycaster.hue(next),1.5f):new net.minecraft.core.particles.DustParticleOptions(tint,1.5f);owner.level().sendParticles(dust,true,false,point.x,point.y,point.z,1,0,0,0,0);next+=.5;}}
    public void complete(HitResult hit){
        var owner=owner();if(hit==null||owner==null||!game.items().currentEpoch(epoch))return;
        var direct=hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity living?living:null;
        var point=hit.getLocation();
        // Secondary lightning, clouds and typed statuses retain this queued ray's item too.
        try(var enchantments=me.skywars2.game.combat.WeaponEnchantments.causeScope(owner,cause)){
            if(color==0)LbcGaycaster.impact(game,owner,point,direction,LbcGaycaster.hue(origin.distanceTo(point)),direct,cause);
            else game.items().gaycasterAcademy().impact(owner,point,direction,color,direct,cause);
        }
    }
    public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){double sample=next;return ignored->()->next=sample;}
}
