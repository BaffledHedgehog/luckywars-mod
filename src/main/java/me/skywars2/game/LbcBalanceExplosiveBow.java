package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** The old consumable TNT bow converts one published, finalized family without a second trait roll. */
final class LbcBalanceExplosiveBow {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<net.minecraft.world.level.block.Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final String TAG="skywars2.balance.tnt_bow";
    private final GameController game;
    private final Map<PrimedTnt,State> actors=new IdentityHashMap<>();
    private int callbacks;
    private static final class Family {
        ServerPlayer author;final LbcIndirectKills.Owner cause;final String epoch;final LbcChargedBows.BeamPayload payload;
        Set<Integer> extraVariants=Set.of();
        Family(ServerPlayer author,LbcIndirectKills.Owner cause,String epoch,LbcChargedBows.BeamPayload payload){this.author=author;this.cause=cause;this.epoch=epoch;this.payload=payload;}
    }
    private static final class State {
        final Family family;final int variant;boolean landed,delivered,continued;
        final Set<Integer> extraVariants;
        State(Family family,int variant){this(family,variant,family.extraVariants);}
        State(Family family,int variant,Set<Integer> extraVariants){this.family=family;this.variant=variant;this.extraVariants=extraVariants;}
        boolean hasVariant(int value){return variant==value||extraVariants.contains(value);}
        Set<Integer> childVariants(int child,int completed){
            if(extraVariants.isEmpty())return Set.of();
            var next=new java.util.HashSet<>(extraVariants);if(variant>0)next.add(variant);
            next.remove(child);next.remove(completed);return Set.copyOf(next);
        }
    }
    LbcBalanceExplosiveBow(GameController game){this.game=game;}
    static boolean managed(Entity body){return body instanceof PrimedTnt&&body.entityTags().contains(TAG);}
    ServerPlayer owner(Entity body){var state=body instanceof PrimedTnt tnt?actors.get(tnt):null;return state==null?null:state.family.author;}
    boolean convert(ServerPlayer owner,ItemStack weapon,List<Projectile> finalizedFamily){
        if(!LbcBalanceItems.flag(weapon,"explosive")||game.items().magic().mechanismSuppressedAt(owner.level(),owner.position()))return false;
        var payload=LbcBeamAmmo.captureImpact(game,owner,weapon,finalizedFamily);if(payload==null)return false;
        var wild=game.items().indirectKills().wildCause(owner);var family=new Family(owner,new LbcIndirectKills.Owner(wild.player(),wild.order(),true,true),game.items().epoch(),payload);boolean converted=false;
        if(finalizedFamily.stream().anyMatch(p->LbcTmtAmmoPayloads.state(p)!=null)){
            var extra=new java.util.HashSet<Integer>();for(var p:finalizedFamily)extra.addAll(LbcTmtAmmoPayloads.variants(p));family.extraVariants=Set.copyOf(extra);
        }
        for(var projectile:finalizedFamily){if(!(projectile instanceof AbstractArrow arrow)||arrow.isRemoved()||arrow.getOwner()!=owner)continue;
            int variant=variant(arrow);var origin=arrow.position().add(0,.75,0);var forward=arrow.getLookAngle();var left=Vec3.directionFromRotation(0,arrow.getYRot()-90);var up=forward.cross(left);
            shells(family,variant,arrow,origin,forward,left,up,LbcBalanceTntShells.BASE);
            if(variant==101||family.extraVariants.contains(101))shells(family,variant,arrow,origin,forward,left,up,LbcBalanceTntShells.EXTRA);
            arrow.discard();converted=true;
        }
        if(converted&&!owner.isCreative())owner.getInventory().clearOrCountMatchingItems(s->LbcBalanceItems.flag(s,"explosive"),false,1,owner.inventoryMenu.getCraftSlots());
        return converted;
    }
    private int variant(AbstractArrow arrow){
        if(arrow instanceof SpectralArrow)return 304;
        var contents=arrow.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);int color=contents.customColor().orElse(-1);
        if(color==6710877)return 300;if(color>=6710881&&color<=6710883)return color-6710881+301;
        for(var effect:contents.getAllEffects())if(effect.is(net.minecraft.world.effect.MobEffects.RAID_OMEN))return effect.getAmplifier();return 0;
    }
    private void shells(Family family,int variant,AbstractArrow arrow,Vec3 origin,Vec3 forward,Vec3 left,Vec3 up,LbcBalanceTntShells.Shell[] shells){
        for(var shell:shells){var at=origin.add(left.scale(shell.x())).add(up.scale(shell.y())).add(forward.scale(shell.z()));
            var body=spawn(family,(ServerLevel)arrow.level(),at,game.items().timeStop().persistedVelocity(arrow,arrow.getDeltaMovement()),shell.fuse(),variant,true,game.items().timeStop().persistedNoGravity(arrow,arrow.isNoGravity())||variant==303||family.extraVariants.contains(303));
            if(body!=null){body.setRemainingFireTicks(arrow.getRemainingFireTicks());body.setGlowingTag(variant==103||variant==304||family.extraVariants.contains(103)||family.extraVariants.contains(304));}
        }
    }
    private PrimedTnt spawn(Family family,ServerLevel level,Vec3 at,Vec3 motion,int fuse,int variant,boolean ground){
        return spawn(family,level,at,motion,fuse,variant,ground,false);
    }
    private PrimedTnt spawn(Family family,ServerLevel level,Vec3 at,Vec3 motion,int fuse,int variant,boolean ground,boolean noGravity){
        return spawn(family,level,at,motion,fuse,variant,ground,noGravity,family.extraVariants);
    }
    private PrimedTnt spawn(Family family,ServerLevel level,Vec3 at,Vec3 motion,int fuse,int variant,boolean ground,boolean noGravity,Set<Integer> extraVariants){
        if(!level.hasChunkAt(BlockPos.containing(at)))return null;
        var body=new PrimedTnt(level,at.x,at.y,at.z,family.author);body.setFuse(fuse);body.setDeltaMovement(motion);body.setNoGravity(noGravity);body.entityTags().add(TAG);if(ground)body.entityTags().add("groundbam");
        game.items().spellSources().bind(body,new LbcSpellSource.Source(body.getUUID(),family.cause.player(),family.epoch,family.cause,Set.of(),true));
        game.items().markCurrentItemEpoch(body);game.items().summons().track(family.author,body);game.items().timeStop().prepareFrozen(body);actors.put(body,new State(family,variant,extraVariants));
        if(!level.addFreshEntity(body)){actors.remove(body);body.discard();return null;}return body;
    }
    private boolean paused(PrimedTnt body){return game.items().timeStop().stopped((ServerLevel)body.level())||game.items().timeStop().frozen(body)||body.entityTags().contains("time_stopped");}
    /** Called at native TNT tick HEAD; false cancels only a managed frozen/retired actor. */
    boolean beforeTick(PrimedTnt body){var state=actors.get(body);if(state==null)return true;callbacks++;try{
        if(!game.playing()||!game.items().currentEpoch(state.family.epoch)){actors.remove(body);body.discard();return false;}
        if(paused(body))return false;
        var level=(ServerLevel)body.level();var owner=state.family.author;var at=body.position();
        if(state.hasVariant(112))game.items().quiver().beamSample(owner,level,at,state.family.cause,112);
        if(state.hasVariant(113))game.items().quiver().beamSample(owner,level,at,state.family.cause,113);
        if(!state.landed&&body.onGround()&&body.entityTags().remove("groundbam")){
            state.landed=true;body.setFuse(2);
            if(state.hasVariant(102))for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),e->e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(at)<=100&&game.items().wands().projectileTarget(owner,e)))
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)game.items().evokerBass().spawnQuiverOwned(level,owner,target.position().add(x,0,z));
            if(state.hasVariant(111))game.items().indirectKills().capturedWorldChanges(level,state.family.cause,()->{var center=body.blockPosition();for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5)))if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(AIRS))level.setBlock(pos,Blocks.FIRE.defaultBlockState(),3);});
            if(state.hasVariant(103)){var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setPos(at);bolt.setCause(owner);game.items().summons().track(owner,bolt);game.items().markCurrentItemEpoch(bolt);level.addFreshEntity(bolt);}
            if(state.hasVariant(301))spawn(state.family,level,at,Vec3.ZERO,0,0,false,false,state.childVariants(0,301));
            if(state.hasVariant(302)){deliver(body,state);level.sendParticles(ParticleTypes.EXPLOSION,at.x,at.y,at.z,1,0,0,0,0);LbcOverload.sound(owner,at,"minecraft:entity.generic.explode",1,1,0);actors.remove(body);body.discard();return false;}
        }
        if(state.hasVariant(300)&&!state.continued&&body.getFuse()==1){state.continued=true;
            var nearest=game.items().lifetimes().nearestBeamTarget(level,at,target->!target.isRemoved()&&target.isAlive()&&target.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,target));
            if(nearest!=null)spawn(state.family,level,at.add(nearest.getEyePosition().subtract(at).normalize().scale(.5)),Vec3.ZERO,30,300,false,false,state.childVariants(300,-1));
        }
        return true;
    }finally{callbacks--;}}
    /** Native fuse explosion HEAD, before ordinary damage. A family shares its accepted contact ledger. */
    void exploding(PrimedTnt body){var state=actors.get(body);if(state!=null)deliver(body,state);}
    private void deliver(PrimedTnt body,State state){if(state.delivered)return;state.delivered=true;callbacks++;try{state.family.payload.sample((ServerLevel)body.level(),body.position(),state.family.cause,false);}finally{callbacks--;}}
    void loaded(Entity body){
        if(!(body instanceof PrimedTnt tnt)||!managed(body))return;
        if(!game.playing()||!game.items().currentEpoch(body)){body.discard();return;}
        if(actors.containsKey(tnt))return;
        for(var entry:List.copyOf(actors.entrySet()))if(entry.getKey().getUUID().equals(tnt.getUUID())){actors.remove(entry.getKey());actors.put(tnt,entry.getValue());var family=entry.getValue().family;game.items().spellSources().bind(tnt,new LbcSpellSource.Source(tnt.getUUID(),family.cause.player(),family.epoch,family.cause,Set.of(),true));break;}
    }
    void unloaded(Entity body){}
    void tick(){actors.keySet().removeIf(body->body.isRemoved()&&body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK);}
    void stop(){var old=List.copyOf(actors.keySet());actors.clear();old.forEach(Entity::discard);}
    LbcCloneSkills.ActorImage cloneImage(Entity body){
        var saved=body instanceof PrimedTnt tnt?actors.get(tnt):null;if(saved==null)return null;
        var family=saved.family;int variant=saved.variant;var extraVariants=saved.extraVariants;boolean landed=saved.landed,delivered=saved.delivered,continued=saved.continued;
        return CloneComplexProjectileStates.image(clone->{var state=new State(family,variant,extraVariants);state.landed=landed;state.delivered=delivered;state.continued=continued;actors.put((PrimedTnt)clone,state);game.items().spellSources().bind(clone,new LbcSpellSource.Source(clone.getUUID(),family.cause.player(),family.epoch,family.cause,Set.of(),true));},clone->actors.remove((PrimedTnt)clone));
    }
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        if(callbacks!=0)throw new IllegalStateException("Finish TNT bow callbacks before capture");
        record Image(MatchDamageSources.Ref body,Family family,int variant,Set<Integer> extraVariants,boolean landed,boolean delivered,boolean continued){}
        var saved=new ArrayList<Image>();var families=new IdentityHashMap<Family,MatchQueuedWorkCheckpoint.Image>();var authors=new IdentityHashMap<Family,MatchDamageSources.Ref>();
        actors.forEach((body,state)->{saved.add(new Image(links.key(body),state.family,state.variant,state.extraVariants,state.landed,state.delivered,state.continued));families.computeIfAbsent(state.family,f->f.payload.checkpoint(links));authors.computeIfAbsent(state.family,f->links.key(f.author));});
        return current->{var next=new IdentityHashMap<PrimedTnt,State>();for(var image:saved){var state=new State(image.family,image.variant,image.extraVariants);state.landed=image.landed;state.delivered=image.delivered;state.continued=image.continued;next.put((PrimedTnt)current.get(image.body),state);}var payloads=families.values().stream().map(image->image.prepare(current)).toList();var owners=new IdentityHashMap<Family,ServerPlayer>();authors.forEach((family,ref)->owners.put(family,(ServerPlayer)current.get(ref)));
            return ()->{actors.clear();actors.putAll(next);owners.forEach((family,owner)->family.author=owner);payloads.forEach(Runnable::run);};};
    }
}
