package me.skywars2.game;

import com.mojang.math.Transformation;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Successful-shot additions to actual native arrows; no duplicate flight/collision engine. */
public final class LbcTrinketArrows {
    public static final int BEDROCK=1,VOLOS=2,COCKTAIL=4,ROCKET=8;
    private static final String PREFIX="skywars2.trinket_arrow.",ROCKET_ACTOR=PREFIX+"rocket_actor";
    private static final String AUTHOR=PREFIX+"author:",ORDER=PREFIX+"order:",TEAM=PREFIX+"team:";
    private static final String[] FLAGS={"bedrock_arrow","volos_arrow","cocktail_arrow","rocket_spell"};
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    private final GameController game;
    private final Set<Display.ItemDisplay> rockets=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Projectile> arrows=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcTrinketArrows(GameController game){this.game=game;}
    public void mark(Projectile arrow,int mask){
        if(!equivalent(arrow)||(mask&(BEDROCK|VOLOS|COCKTAIL|ROCKET))==0)return;
        game.items().markCurrentItemEpoch(arrow);
        arrows.add(arrow);
        for(int i=0;i<FLAGS.length;i++)if((mask&(1<<i))!=0)arrow.entityTags().add(FLAGS[i]);
        if((mask&COCKTAIL)!=0)arrow.entityTags().add("killedarrow");
        if(arrow.getOwner() instanceof ServerPlayer owner&&value(arrow,AUTHOR)==null){
            var cause=game.items().indirectKills().coldCause(owner,true);
            arrow.entityTags().add(AUTHOR+cause.player());arrow.entityTags().add(ORDER+cause.order());
            if(owner.getTeam()!=null)arrow.entityTags().add(TEAM+owner.getTeam().getName());
        }
    }
    /** Called by the actual ownership-transfer event; detached rockets keep their accepted cause. */
    public void ownerChanged(Entity actor,ServerPlayer owner){
        if(!(actor instanceof Projectile)||!equivalent(actor)||(mask(actor)==0&&value(actor,AUTHOR)==null))return;
        var cause=game.items().indirectKills().coldCause(owner,true);
        actor.entityTags().removeIf(tag->tag.startsWith(AUTHOR)||tag.startsWith(ORDER)||tag.startsWith(TEAM));
        actor.entityTags().add(AUTHOR+cause.player());actor.entityTags().add(ORDER+cause.order());
        if(owner.getTeam()!=null)actor.entityTags().add(TEAM+owner.getTeam().getName());
    }
    private int mask(Entity e){int mask=0;for(int i=0;i<FLAGS.length;i++)if(e.entityTags().contains(FLAGS[i]))mask|=1<<i;return mask;}
    private boolean equivalent(Entity actor){return actor instanceof AbstractArrow||actor instanceof net.minecraft.world.entity.projectile.ShulkerBullet;}
    private static String value(Entity e,String prefix){for(var tag:e.entityTags())if(tag.startsWith(prefix))return tag.substring(prefix.length());return null;}
    private LbcIndirectKills.Owner cause(Entity e){try{return new LbcIndirectKills.Owner(UUID.fromString(value(e,AUTHOR)),Long.parseLong(value(e,ORDER)),false,true);}catch(RuntimeException invalid){return null;}}
    private LbcSpellSource.Source source(Entity e){var cause=cause(e);return cause==null?null:new LbcSpellSource.Source(e.getUUID(),cause.player(),game.items().epoch(),cause,Set.of(),true);}
    private boolean paused(Entity e){return game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().stopped((ServerLevel)e.level())||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped");}
    private boolean resident(ServerLevel level,Vec3 at){var p=BlockPos.containing(at);return level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    private boolean currentContext(Entity actor){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.items().currentEpoch(actor)&&actor.level() instanceof ServerLevel&&LbcDimensionItems.ordinary(actor.level());}
    /** MAG removes only this upgrade. The native carrier and coexisting traits retain their own policy. */
    private void cancelBedrockInMagic(Projectile arrow,Vec3 at){if(arrow.entityTags().contains("bedrock_arrow")
            &&game.items().magic().suppressedAt((ServerLevel)arrow.level(),at))arrow.entityTags().remove("bedrock_arrow");}
    /** Clips only the MAG test, never native movement or collision. No chunk loading or entity hit synthesis. */
    public void bedrockBeforeNativeMove(Projectile shot){
        if(!equivalent(shot)||!shot.entityTags().contains("bedrock_arrow")||!currentContext(shot)||paused(shot))return;
        var level=(ServerLevel)shot.level();var from=shot.position();var to=from.add(shot.getDeltaMovement());
        var wall=net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,level,(world,pos)->{
            if(!resident(world,Vec3.atCenterOf(pos)))return net.minecraft.world.phys.shapes.Shapes.block().clip(from,to,pos);
            return world.getBlockState(pos).getCollisionShape(world,pos,net.minecraft.world.phys.shapes.CollisionContext.of(shot)).clip(from,to,pos);
        },world->null);
        if(Double.isFinite(game.items().magic().suppressionFraction(level,from,wall==null?to:wall.getLocation())))shot.entityTags().remove("bedrock_arrow");
    }
    private boolean mob(Entity e){return e instanceof LivingEntity&&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validRoleTarget(e,false);}
    private boolean target(Entity shot,Entity e){if(!mob(e)||!game.items().wands().validTarget(e))return false;
        var authority=cause(shot);if(authority!=null&&authority.player().equals(e.getUUID()))return false;
        var team=value(shot,TEAM);return !(e instanceof ServerPlayer player)||team==null||player.getTeam()==null||!team.equals(player.getTeam().getName());}

    /** Called once at native tick HEAD, before vanilla changes position or applies collision damage. */
    public boolean beforeArrow(AbstractArrow arrow){return beforeProjectile(arrow);}
    public boolean beforeProjectile(Projectile arrow){
        if(!equivalent(arrow))return true;
        int mask=mask(arrow);if(mask==0)return true;
        if(!currentContext(arrow)){arrows.remove(arrow);arrow.discard();return false;}
        if(arrow.isRemoved())return false;if(paused(arrow))return false;
        var level=(ServerLevel)arrow.level();var at=arrow.position();
        if(arrow instanceof AbstractArrow)bedrockBeforeNativeMove(arrow);
        cancelBedrockInMagic(arrow,at);mask=mask(arrow);
        // Shared ordinary-arrow deletion remains authoritative; electrical auras have no MAG check.
        if(game.items().magic().mechanismSuppressedAt(level,at)){arrow.discard();return false;}
        if((mask&COCKTAIL)!=0){
            var nearby=level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(4),e->mob(e)&&e.position().distanceToSqr(at)<=16);
            if(!nearby.isEmpty()){
                for(var target:nearby)if(target(arrow,target)&&!game.items().magic().mechanismSuppressedAt(level,target.position())){
                    target.hurtServer(level,game.items().indirectKills().captureDamage(new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),arrow,arrow.getOwner()),cause(arrow)),.5f);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,target.getX(),target.getY()+1,target.getZ(),4,.3,.6,.3,0);
                }
                if(level.getRandom().nextFloat()<.01f){arrow.discard();return false;}
            }
        }
        if((mask&VOLOS)!=0&&level.getRandom().nextBoolean()){
            var nearby=level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(100),e->mob(e)&&e.position().distanceToSqr(at)<=10000);
            if(!nearby.isEmpty()){
                var nearest=nearby.stream().filter(e->target(arrow,e)).min(Comparator.comparingDouble(e->e.position().distanceToSqr(at))).orElse(null);
                var aim=nearest==null?Vec3.directionFromRotation(arrow.getXRot(),arrow.getYRot()):nearest.getEyePosition().subtract(at).normalize();
                game.items().blind().spawnFromTrinket(arrow,at,aim.scale(1.8));
                if(level.getRandom().nextFloat()<.05f){cube(arrow,at,Blocks.AIR);level.sendParticles(ParticleTypes.SQUID_INK,at.x,at.y,at.z,100,1,1,1,0);
                    LbcOverload.sound(level,at,"minecraft:block.amethyst_cluster.fall",1,1,0,0);arrow.discard();return false;}
            }
        }
        if((mask&ROCKET)!=0){var clock=game.items().mana().score(arrow,"Lifetime");clock.set(clock.get()+1);if(clock.get()>=300){arrow.discard();return false;}}
        if(arrow instanceof AbstractArrow&&((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)arrow).skywars2$isInGround()){terminal(arrow,at,true);return !arrow.isRemoved();}
        return true;
    }
    public void hit(Projectile arrow,HitResult hit){if(equivalent(arrow)&&hit.getType()!=HitResult.Type.MISS&&mask(arrow)!=0){
        if(!currentContext(arrow)){arrows.remove(arrow);arrow.discard();return;}
        terminal(arrow,hit.getLocation(),hit instanceof BlockHitResult);}}
    private void terminal(Projectile arrow,Vec3 at,boolean block){cancelBedrockInMagic(arrow,at);int mask=mask(arrow);
        if((mask&ROCKET)!=0){arrow.entityTags().remove("rocket_spell");rocket(arrow,at);}
        if(block&&(mask&BEDROCK)!=0){cube(arrow,at,Blocks.BEDROCK);LbcOverload.sound((ServerLevel)arrow.level(),at,"minecraft:block.stone.place",1,1,0,0);arrow.discard();}
        if((mask&ROCKET)!=0||(mask&COCKTAIL)!=0)arrow.discard();
    }
    private void cube(Entity actor,Vec3 at,Block block){var level=(ServerLevel)actor.level();var from=BlockPos.containing(at.add(-.5,-.5,-.5));var to=BlockPos.containing(at.add(.5,.5,.5));
        for(var pos:BlockPos.betweenClosed(from,to))if(!resident(level,Vec3.atCenterOf(pos)))return;
        game.items().indirectKills().trapChanges(level,cause(actor),()->{
            for(var pos:BlockPos.betweenClosed(from,to)){level.destroyBlock(pos,true,actor,Block.UPDATE_LIMIT);LbcDropBlocks.setBlock(level,pos,block.defaultBlockState(),false);}
        });
    }
    private void rocket(Projectile arrow,Vec3 contact){
        var level=(ServerLevel)arrow.level();var horizontal=new Vec3(-contact.x,0,-contact.z).normalize();
        var at=contact.add(horizontal.scale(-40)).add(0,80,0);if(!resident(level,at)||cause(arrow)==null)return;
        var rocket=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);rocket.setPos(at);
        rocket.setItemTransform(net.minecraft.world.item.ItemDisplayContext.FIXED);rocket.setShadowRadius(0);rocket.setShadowStrength(0);rocket.setNoGravity(true);
        // The tp command uses the caller's facing-origin horizontal frame, overriding summon Rotation.
        rocket.setYRot((float)Math.toDegrees(Math.atan2(-horizontal.x,horizontal.z)));rocket.setXRot(-61.5f);rocket.setPosRotInterpolationDuration(1);rocket.setTransformationInterpolationDuration(1);
        var model=new ItemStack(Items.GOLDEN_SWORD);model.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:rocket_govnowars"));rocket.setItemStack(model);
        rocket.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(-2,-2,2),new Quaternionf()));
        rocket.entityTags().addAll(List.of(ROCKET_ACTOR,"rocketspell","slowraycast","player","mecha"));
        for(var tag:arrow.entityTags())if(tag.startsWith(AUTHOR)||tag.startsWith(ORDER)||tag.startsWith(TEAM))rocket.entityTags().add(tag);
        game.items().markCurrentItemEpoch(rocket);game.items().spellSources().bind(rocket,source(rocket));game.items().timeStop().prepareFrozen(rocket);
        if(level.addFreshEntity(rocket)){rockets.add(rocket);LbcOverload.sound(level,at,"minecraft:lbcsounds.rocketspell",10,1,0,0);}
    }
    public void tick(){for(var rocket:List.copyOf(rockets)){
        if(rocket.isRemoved()||!currentContext(rocket)){retire(rocket);continue;}
        if(paused(rocket))continue;var level=(ServerLevel)rocket.level();var at=rocket.position();if(!resident(level,at))continue;
        if(game.items().magic().mechanismSuppressedAt(level,at)){retire(rocket);continue;}
        boolean solid=false,resident=true;for(var offset:List.of(Vec3.ZERO,new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(0,0,1),new Vec3(0,0,-1))){var p=at.add(offset);
            if(!resident(level,p)){resident=false;break;}if(level.getBlockState(BlockPos.containing(p)).is(SOLID))solid=true;}
        if(!resident)continue;if(solid){explode(rocket);continue;}
        // Local ^ ^-1 ^: minus the orthogonal local-up vector; no forward displacement.
        var forward=Vec3.directionFromRotation(rocket.getXRot(),rocket.getYRot());var left=Vec3.directionFromRotation(0,rocket.getYRot()-90);
        var next=at.subtract(forward.cross(left));if(!resident(level,next))continue;
        if(Double.isFinite(game.items().magic().mechanismSuppressionFraction(level,at,next))){retire(rocket);continue;}
        level.sendParticles(new net.minecraft.core.particles.DustColorTransitionOptions(0xffffdc40,0xff000000,4),at.x,at.y,at.z,1,0,0,0,.2);
        rocket.setPos(next);rocket.setXRot(rocket.getXRot()+.75f);
    }}
    private void explode(Display.ItemDisplay rocket){var level=(ServerLevel)rocket.level();var at=rocket.position();var source=source(rocket);if(source==null){retire(rocket);return;}
        // Seven radius4 native blasts need their entire exposure/terrain neighbourhood resident.
        for(int x=net.minecraft.util.Mth.floor(at.x-11)>>4;x<=net.minecraft.util.Mth.floor(at.x+11)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(at.z-11)>>4;z<=net.minecraft.util.Mth.floor(at.z+11)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return;
        var owner=game.server().getPlayerList().getPlayer(source.author());rockets.remove(rocket);
        try(var scope=game.items().spellSources().scope(source,level)){
            for(var offset:List.of(Vec3.ZERO,new Vec3(2,0,0),new Vec3(-2,0,0),new Vec3(0,0,2),new Vec3(0,0,-2),new Vec3(0,2,0),new Vec3(0,-2,0))){
                var p=at.add(offset);if(!resident(level,p))continue;var tnt=new PrimedTnt(level,p.x,p.y,p.z,owner);tnt.setFuse(1);
                game.items().markCurrentItemEpoch(tnt);game.items().spellSources().bind(tnt,source);game.items().timeStop().prepareFrozen(tnt);level.addFreshEntity(tnt);
            }
        }finally{rocket.discard();}
    }
    public void loaded(Entity entity){if(entity instanceof Projectile arrow&&equivalent(arrow)&&mask(arrow)!=0){
        if(!currentContext(arrow)){if(!MatchActorCheckpoint.restoring(entity))arrow.discard();return;}arrows.add(arrow);
    }if(entity instanceof Display.ItemDisplay rocket&&cloneRecognizes(entity)){
        if(cause(rocket)==null||!currentContext(rocket)){if(!MatchActorCheckpoint.restoring(entity))entity.discard();return;}
        rockets.add(rocket);game.items().spellSources().bind(rocket,source(rocket));game.items().timeStop().prepareFrozen(rocket);
    }}
    public void unloaded(Entity entity){rockets.remove(entity);arrows.remove(entity);}
    private void retire(Display.ItemDisplay rocket){rockets.remove(rocket);rocket.discard();}
    public void stop(){for(var arrow:List.copyOf(arrows))arrow.discard();arrows.clear();for(var rocket:List.copyOf(rockets))retire(rocket);rockets.clear();}
    List<Display.ItemDisplay> snapshot(){return List.copyOf(rockets);}
    List<AbstractArrow> arrowSnapshot(){return arrows.stream().filter(a->!a.isRemoved()&&a instanceof AbstractArrow).map(a->(AbstractArrow)a).toList();}
    List<Projectile> genericSnapshot(){return arrows.stream().filter(a->!a.isRemoved()).toList();}
    int arrowMask(Entity arrow){return mask(arrow);}
    static boolean cloneRecognizes(Entity entity){return entity.entityTags().contains(ROCKET_ACTOR);}
    LbcCloneSkills.ActorImage cloneImage(Entity original){if(!(original instanceof Display.ItemDisplay)||!rockets.contains(original)||cause(original)==null)return null;
        return CloneComplexProjectileStates.image(clone->{rockets.add((Display.ItemDisplay)clone);game.items().spellSources().bind(clone,source(clone));},rockets::remove);}
    void reindexRestored(Iterable<Entity> restored){rockets.clear();arrows.clear();for(var entity:restored)if(!entity.isRemoved()&&(cloneRecognizes(entity)||equivalent(entity)&&mask(entity)!=0))loaded(entity);}
}

