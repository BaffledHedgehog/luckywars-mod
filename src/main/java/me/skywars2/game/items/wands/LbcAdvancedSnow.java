package me.skywars2.game.items.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.skywars2.game.GameController;
import me.skywars2.game.academy.LbcAcademy;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.effects.ItemEffectLevels;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.match.checkpoints.MatchItemStackLinks;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.match.checkpoints.MatchItemStackLinks.Ref;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint.Image;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links;
import me.skywars2.game.presentation.projectiles.LbcParticleBatch;
import me.skywars2.game.presentation.projectiles.LbcParticleBatch.Group;
import me.skywars2.game.items.gadgets.LbcModeScroll;
import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;

/** Budgeted instant rays and source native snow clouds, sharing the existing Frost owner. */
public final class LbcAdvancedSnow {
    static final String CLOUD="skywars2.wand.snow_cloud";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    public record Cloud(AreaEffectCloud entity,InteractionHand hand,ItemStack stack){}
    private final GameController game;
    private final Map<AreaEffectCloud,Cloud> clouds=new LinkedHashMap<>();
    public LbcAdvancedSnow(GameController game){this.game=game;}
    public List<Cloud> snapshot(){return List.copyOf(clouds.values());}
    private boolean active(ServerPlayer p){return game.items().mana().visible(p)&&!game.items().actionsBlocked(p)
            &&!p.entityTags().contains("time_stopped")&&!game.items().timeStop().stopped(p.level())
            &&!game.items().magic().suppressed(p)&&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());}
    public boolean use(ServerPlayer p,InteractionHand hand,ItemStack stack,boolean snow,boolean academy){
        if(!active(p))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Math.max(1,Math.min(3,data.getIntOr("mode",1)));
        if(!snow&&academy&&p.isShiftKeyDown()){
            mode=LbcModeScroll.next(mode,3);data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.getInventory().setChanged();
            p.sendSystemMessage(Component.translatable(switch(mode){case 1->"mode_default";case 2->"mode_uppest_block";default->"mode_wall_clip";}));return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly(snow?"snow_wand_mana_use":"advanced_ender_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(p,cost))return false;
        if(snow&&!academy)spawn(p,p.getEyePosition(),hand,stack);
        else aim(p,hand,stack,snow,academy,mode,cost);
        // Source upgraded snow/cast omits the mage/trinket hook; the ordinary cast and both Enders have it.
        if(!snow||!academy)game.items().wands().wandCasted(p);
        return true;
    }
    public void grimoire(ServerPlayer p,InteractionHand hand,boolean snow,boolean academy,int mode){
        if(snow&&!academy)spawn(p,p.getEyePosition(),hand,ItemStack.EMPTY);
        else aim(p,InteractionHand.MAIN_HAND,ItemStack.EMPTY,snow,academy,mode,0);
    }
    private void aim(ServerPlayer p,InteractionHand hand,ItemStack stack,boolean snow,boolean academy,int mode,int cost){
        Vec3 direction=p.getLookAngle();
        Vec3 from=p.getEyePosition().add(direction.scale(hand==InteractionHand.OFF_HAND?.5:0));
        aim(p,from,direction,hand,stack,snow,academy,mode,cost);
    }
    public void scriptCast(ServerPlayer p,InteractionHand hand,Vec3 from,Vec3 direction,boolean snow,boolean academy,int mode){
        if(snow&&!academy)spawn(p,from,hand,ItemStack.EMPTY);
        else aim(p,from,direction,hand,ItemStack.EMPTY,snow,academy,mode,0);
    }
    private void aim(ServerPlayer p,Vec3 from,Vec3 direction,InteractionHand hand,ItemStack stack,
                     boolean snow,boolean academy,int mode,int cost){
        game.items().exasperato().replayableTool(p,from,direction,Double.POSITIVE_INFINITY,snow||academy&&mode==3,
                new Aim(p,from,direction,hand,stack,snow,academy,mode,cost),false);
    }
    private final class Aim implements LbcExasperato.ReplayableRay {
        final java.util.UUID author;final net.minecraft.server.level.ServerLevel level;final String epoch;
        final Vec3 from,direction;final InteractionHand hand;final boolean snow,academy,clip,borrowed;final int mode,cost;
        ItemStack stack;boolean entered;
        Aim(ServerPlayer p,Vec3 from,Vec3 direction,InteractionHand hand,ItemStack stack,boolean snow,boolean academy,int mode,int cost){
            author=p.getUUID();level=p.level();epoch=game.items().epoch();this.from=from;this.direction=direction;this.hand=hand;this.stack=stack;
            this.snow=snow;this.academy=academy;this.mode=mode;this.cost=cost;clip=!snow&&academy&&mode==3;
            borrowed=WandCastOrigin.borrowed(p);
        }
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            boolean air=state.is(AIRS);if(!clip)return !air;if(!air){entered=true;return false;}
            return entered&&level.getBlockState(pos.above()).is(AIRS);
        }
        public boolean target(Entity e){return !e.getUUID().equals(author)&&e instanceof LivingEntity&&game.items().wands().validTarget(e);}
        public void trail(Vec3 start,Vec3 end){
            double length = start.distanceTo(end), step = snow ? .5 : clip ? 1 : academy ? .7 : .5;
            int samplesPerPacket = game.balanceLegacy() ? 1 : step <= .6 ? 3 : 2;
            try (var trail = new LbcParticleBatch.Group(level,
                    snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.PORTAL, snow ? 0 : .1, snow ? .1 : 1,
                    false, step + .01, samplesPerPacket)) {
                for (double t = 0; t < length; t += step) trail.add(start.lerp(end, t / length), snow ? 1 : 5);
            }
        }
        public void complete(HitResult hit){
            var p=game.server().getPlayerList().getPlayer(author);if(p==null||p.level()!=level)return;
            inProgress++;try(var origin=WandCastOrigin.scope(p,borrowed)) {
            if(hit==null||!game.items().currentEpoch(epoch)||!p.isAlive()||p.isRemoved())return;
            Vec3 point=hit.getLocation();
            // Upgraded/check positions +1 above impact; summon adds another +2.
            if(snow){spawn(p,point.add(0,1,0),hand,stack);return;}
            Vec3 destination;
            if(academy&&mode==2){
                destination = topDestination(p, hit);
                if (destination == null) {
                    if (cost > 0) game.items().mana().add(p, cost);
                    p.sendSystemMessage(Component.literal("X").withStyle(
                            net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
                    return;
                }
            }else{
                if(hit.getType()==HitResult.Type.BLOCK&&!clip)point=point.subtract(direction.scale(academy?.7:.5));
                destination=Vec3.atCenterOf(clip&&hit instanceof net.minecraft.world.phys.BlockHitResult block?block.getBlockPos():BlockPos.containing(point));
            }
            LbcOverload.sound(p,p.position(),"minecraft:entity.enderman.teleport",1,1,0);
            boolean moved=p.teleportTo(p.level(),destination.x,destination.y,destination.z,Set.of(),p.getYRot(),p.getXRot(),true);
            if(academy){
                if(moved)p.resetFallDistance();
                p.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,20,0),p);
            }else{
                p.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_DAMAGE,1,0),p);
                if(p.getItemInHand(hand)==stack&&LbcAcademy.trial(stack)&&!p.level().getBlockState(p.blockPosition().above()).is(AIRS))game.items().academy().ready(p,hand,stack);
                double distance=from.distanceTo(hit.getLocation());if(distance>=89.5&&distance<100)game.items().mana().award(p,"lbc:true_advancements/staffs/teleport");
            }
            LbcOverload.sound(p,p.position(),"minecraft:entity.enderman.teleport",1,1,0);

            }finally{inProgress--;}
        }
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
            var item=links.stack(game,stack);boolean crossed=entered;
            return current->{var rebound=item.get(current);return ()->{stack=rebound;entered=crossed;};};
        }
    }
    private static Vec3 topDestination(ServerPlayer player, HitResult hit) {
        var cell = hit instanceof net.minecraft.world.phys.BlockHitResult block
                ? block.getBlockPos() : BlockPos.containing(hit.getLocation());
        var level = player.level();
        var chunk = level.getChunkSource().getChunkNow(cell.getX() >> 4, cell.getZ() >> 4);
        if (chunk == null) return null;
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, cell.getX(), cell.getZ()) + 1;
        if (surface <= 0 || surface <= level.getMinY()) return null;

        // A ray can hit a wall exactly on its face. Land over that block with
        // the player's full body clear of the neighboring column.
        var destination = Vec3.atCenterOf(new BlockPos(cell.getX(), surface, cell.getZ()));
        var body = player.getBoundingBox().move(destination.subtract(player.position()));
        return level.noCollision(player, body) ? destination : null;
    }

    private int inProgress,cadenceOffset;
    public void checkpointBoundary(){if(inProgress!=0)throw new IllegalStateException("Finish snow/ender native callbacks before checkpointing");}
    public Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    public final class Checkpoint {
        private record Image(MatchDamageSources.Ref cloud,InteractionHand hand,MatchItemStackLinks.Ref stack){}
        private final List<Image> values;private final int phase=Math.floorMod(game.server().getTickCount()-cadenceOffset,5);
        Checkpoint(MatchSpellCheckpoint.Links links){values=clouds.values().stream().map(c->new Image(links.key(c.entity()),c.hand(),c.stack()==null?null:links.stack(game,c.stack()))).toList();}
        public void rebaseFrozen(){cadenceOffset=game.server().getTickCount()-phase;}
        public Runnable prepare(MatchSpellCheckpoint.Links links){
            var next=new LinkedHashMap<AreaEffectCloud,Cloud>();for(var value:values){var cloud=(AreaEffectCloud)links.get(value.cloud);next.put(cloud,new Cloud(cloud,value.hand,value.stack==null?null:value.stack.get(links)));}
            return ()->{clouds.clear();clouds.putAll(next);cadenceOffset=game.server().getTickCount()-phase;};
        }
    }
    private void spawn(ServerPlayer owner,Vec3 at,InteractionHand hand,ItemStack stack){
        inProgress++;try {
        var pos=at.add(0,2,0);if(!owner.level().hasChunkAt(BlockPos.containing(pos)))return;
        var cloud=new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,owner.level());
        cloud.setPos(pos);cloud.setOwner(owner);cloud.setRadius(4);cloud.setDuration(250);
        cloud.setCustomParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST,Blocks.SNOW.defaultBlockState()));
        cloud.entityTags().add(CLOUD);cloud.entityTags().add("magic");game.items().markCurrentItemEpoch(cloud);
        if(owner.level().addFreshEntity(cloud))clouds.put(cloud,new Cloud(cloud,hand,stack));
        LbcOverload.sound(owner,at,"minecraft:lbcsounds.snow_wand",2,.8f,0);
        owner.level().sendParticles(ParticleTypes.SNOWFLAKE,at.x,at.y+1,at.z,300,2,2,2,.2);
        if(owner.level().players().stream().anyMatch(p->p!=owner&&!p.isSpectator()&&!owner.isAlliedTo(p)&&p.position().distanceToSqr(at)<=25))game.items().mana().award(owner,"lbc:true_advancements/staffs/snow_wand");

        }finally{inProgress--;}
    }
    public void pulse(Cloud state){
        inProgress++;try {
        var cloud=state.entity();if(!(cloud.getOwner() instanceof ServerPlayer owner))return;
        int frozen=0;
        for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(cloud.position(),cloud.position()).inflate(5),
                e->e.position().distanceToSqr(cloud.position())<=25&&game.items().wands().projectileTarget(owner,e))){
            game.items().frost().snow(target,owner);if(game.items().frost().isFrozen(target))frozen++;
        }
        if(frozen>=3&&state.hand()!=null&&owner.getItemInHand(state.hand())==state.stack()&&LbcAcademy.trial(state.stack()))game.items().academy().ready(owner,state.hand(),state.stack());

        }finally{inProgress--;}
    }
    public void tick(){
        inProgress++;try {for(var state:List.copyOf(clouds.values())){
        var cloud=state.entity();
        if(cloud.isRemoved()||!game.playing()||!game.items().currentEpoch(cloud)||!cloud.level().hasChunkAt(cloud.blockPosition())
                ||!(cloud.getOwner() instanceof ServerPlayer owner)||!game.items().mana().visible(owner)
                ||game.items().magic().suppressedAt(owner.level(),cloud.position())){unloaded(cloud);continue;}
        if(!cloud.entityTags().contains("time_stopped")&&Math.floorMod(game.server().getTickCount()-cadenceOffset,5)==0)pulse(state);
    }
        }finally{inProgress--;}
    }
    public void loaded(Entity e){if(MatchActorCheckpoint.restoring(e))return;if(e instanceof AreaEffectCloud cloud&&e.entityTags().contains(CLOUD)){
        if(game.playing()&&game.items().currentEpoch(e)&&cloud.getOwner() instanceof ServerPlayer)clouds.putIfAbsent(cloud,new Cloud(cloud,null,null));else e.discard();
    }}
    public void unloaded(Entity e){if(MatchActorCheckpoint.restoring(e))return;if(e instanceof AreaEffectCloud cloud&&clouds.remove(cloud)!=null&&!cloud.isRemoved())cloud.discard();}
    public void forget(ServerPlayer p){for(var cloud:List.copyOf(clouds.keySet()))if(cloud.getOwner()==p)unloaded(cloud);}
    public void stop(){for(var cloud:List.copyOf(clouds.keySet()))unloaded(cloud);cadenceOffset=0;}
}
