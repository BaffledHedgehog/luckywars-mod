package me.skywars2.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.skywars2.mixin.WandProjectileMixin.ArrowAccess;

/** Authored IMBA homing on the original physical arrow and its native collisions. */
public final class LbcHomingArrows {
    public static final int SOURCE_COLOR=6710885,ACTIVE_COLOR=6710877;
    private static final DustParticleOptions RED=new DustParticleOptions(0xff0000,2);
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    public record Flight(int age,long rng) {}
    private static final Codec<Flight> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.INT.fieldOf("age").forGetter(Flight::age),Codec.LONG.fieldOf("rng").forGetter(Flight::rng)).apply(i,Flight::new));
    private static final AttachmentType<Flight> DATA=AttachmentRegistry.create(
            Identifier.parse("skywars2:homing_arrow"),builder->builder.persistent(CODEC));
    private final GameController game;
    private final Map<AbstractArrow,Flight> flights=new LinkedHashMap<>();
    private final Map<AbstractArrow,LivingEntity> targets=new IdentityHashMap<>();
    public LbcHomingArrows(GameController game){this.game=game;}
    public static void register(){} // Forces persistent attachment registration before native entity load.
    public static boolean matches(ItemStack stack){var contents=stack.get(DataComponents.POTION_CONTENTS);
        return contents!=null&&contents.customColor().filter(c->c==SOURCE_COLOR||c==ACTIVE_COLOR).isPresent();}
    public static boolean matches(Entity entity){return entity instanceof AbstractArrow arrow
            &&(arrow instanceof Arrow&&matches(arrow.getPickupItemStackOrigin())||LbcTmtAmmoPayloads.hasVariant(arrow,300));}
    public static boolean isManaged(Entity entity){return entity instanceof AbstractArrow&&entity.getAttached(DATA)!=null;}
    public Flight state(Entity entity){return flights.get(entity);}
    public int count(){return flights.size();}
    public List<AbstractArrow> actors(){return List.copyOf(flights.keySet());}
    private void install(AbstractArrow arrow,Flight state){arrow.setAttached(DATA,state);flights.put(arrow,state);}
    public void loaded(Entity entity){
        if(!(entity instanceof AbstractArrow arrow))return;
        var state=arrow.getAttached(DATA);
        if(state!=null){flights.put(arrow,state);return;}
        if(!matches(arrow))return;
        var stack=arrow.getPickupItemStackOrigin().copy();
        var old=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        if(arrow instanceof Arrow&&old.customColor().orElse(-1)==SOURCE_COLOR){
            stack.set(DataComponents.POTION_CONTENTS,new PotionContents(old.potion(),Optional.of(ACTIVE_COLOR),old.customEffects(),old.customName()));
            arrow.getSlot(0).set(stack);
        }
        arrow.setNoGravity(true);
        game.items().markCurrentItemEpoch(arrow);
        install(arrow,new Flight(0,arrow.getUUID().getMostSignificantBits()^arrow.getUUID().getLeastSignificantBits()));
    }
    public void unloaded(Entity entity){flights.remove(entity);targets.remove(entity);}
    public void reindexRestored(Iterable<Entity> restored){flights.clear();targets.clear();for(var entity:restored)loaded(entity);}
    private boolean paused(Entity arrow){return game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().stopped((ServerLevel)arrow.level())||game.items().timeStop().frozen(arrow)||arrow.entityTags().contains("time_stopped");}
    private boolean target(AbstractArrow arrow,LivingEntity candidate){
        if(candidate.level()!=arrow.level()||candidate==arrow.getOwner()||!candidate.getType().builtInRegistryHolder().is(MOBS)
                ||!game.items().wands().validTarget(candidate))return false;
        return !(arrow.getOwner() instanceof ServerPlayer owner)||game.items().wands().projectileTarget(owner,candidate);
    }
    /** A valid cached target bounds the native spatial query without imposing an acquisition radius. */
    private LivingEntity nearest(AbstractArrow arrow){
        var at=arrow.position();var best=targets.get(arrow);
        if(best!=null&&!target(arrow,best))best=null;
        double distance=best==null?Double.POSITIVE_INFINITY:best.position().distanceToSqr(at);
        Iterable<LivingEntity> candidates=best==null?game.items().lifetimes().loadedTargets():
                ((ServerLevel)arrow.level()).getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(Math.sqrt(distance)),e->target(arrow,e));
        for(var candidate:candidates){if(!target(arrow,candidate))continue;double next=candidate.position().distanceToSqr(at);
            if(next<distance){best=candidate;distance=next;}}
        if(best==null)targets.remove(arrow);else targets.put(arrow,best);
        return best;
    }
    public boolean beforeArrow(AbstractArrow arrow){
        if(!flights.containsKey(arrow))loaded(arrow);
        var state=flights.get(arrow);if(state==null)return true;
        if(arrow.isRemoved()){unloaded(arrow);return false;}
        if(!(arrow.level() instanceof ServerLevel level))return true;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(arrow)||!LbcDimensionItems.ordinary(level)){
            unloaded(arrow);arrow.discard();return false;}
        if(paused(arrow))return false;
        if(game.items().magic().mechanismSuppressedAt(level,arrow.position())){unloaded(arrow);arrow.discard();return false;}
        var at=arrow.position();level.sendParticles(RED,true,false,at.x,at.y,at.z,1,0,0,0,0);
        // Java int addition/multiplication deliberately retains source scoreboard overflow semantics.
        int age=state.age()+1;var target=nearest(arrow);
        if(target!=null)arrow.setDeltaMovement(steer(arrow.getDeltaMovement(),target.getEyePosition().subtract(at).normalize(),age));
        long rng=state.rng();
        if(((ArrowAccess)arrow).skywars2$isInGround()){
            var motion=arrow.getDeltaMovement();double[] components={motion.x,motion.y,motion.z};
            for(int i=0;i<3;i++){rng=nextRandom(rng);if((randomBits(rng)>>>1)%100<35)components[i]=fixed(components[i]*-100000)*.0001;}
            arrow.setDeltaMovement(new Vec3(components[0],components[1],components[2]));
            ((ArrowAccess)arrow).skywars2$setInGround(false);
        }
        install(arrow,new Flight(age,rng));return true;
    }
    static Vec3 steer(Vec3 motion,Vec3 direction,int age){
        double acceleration=age*.01;
        int x=fixed(motion.x*10000)+fixed(direction.x*acceleration*10000);
        int y=fixed(motion.y*10000)+fixed(direction.y*acceleration*10000);
        int z=fixed(motion.z*10000)+fixed(direction.z*acceleration*10000);
        while(x>100000||x< -100000||y>100000||y< -100000||z>100000||z< -100000){
            x=Math.floorDiv(x*3,4);y=Math.floorDiv(y*3,4);z=Math.floorDiv(z*3,4);
        }
        return new Vec3(x*.0001,y*.0001,z*.0001);
    }
    private static int fixed(double value){return (int)Math.floor(value);}
    private static long nextRandom(long seed){return seed+0x9e3779b97f4a7c15L;}
    private static long randomBits(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    public void stop(){for(var arrow:List.copyOf(flights.keySet()))arrow.discard();flights.clear();targets.clear();}
    static boolean cloneRecognizes(Entity entity){return isManaged(entity);}
    LbcCloneSkills.ActorImage cloneImage(Entity original){var saved=flights.get(original);if(saved==null)return null;
        return CloneComplexProjectileStates.image(clone->{if(clone instanceof AbstractArrow arrow)install(arrow,
                new Flight(saved.age(),saved.rng()^clone.getUUID().getMostSignificantBits()^clone.getUUID().getLeastSignificantBits()));},this::unloaded);}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private record Image(MatchDamageSources.Ref body,Flight state){}
        private final List<Image> images;
        Checkpoint(MatchSpellCheckpoint.Links links){images=flights.entrySet().stream().map(e->new Image(links.key(e.getKey()),e.getValue())).toList();}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=new LinkedHashMap<AbstractArrow,Flight>();
            for(var image:images)restored.put((AbstractArrow)links.get(image.body()),image.state());
            return ()->{flights.clear();targets.clear();restored.forEach(LbcHomingArrows.this::install);};}
    }
}
