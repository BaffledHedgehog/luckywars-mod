package me.skywars2.game;

import com.mojang.math.Transformation;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Kit11's elastic ballistic item display. Its native attachment is the sole flight state. */
public final class LbcBouncingPoop {
    static final String TAG="skywars2.bouncing_poop";
    private static final AttachmentType<CompoundTag> DATA=AttachmentRegistry.create(
            Identifier.parse("skywars2:bouncing_poop"),b->b.persistent(CompoundTag.CODEC));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<Block> SOIL=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:tannerkivi_no_dirt"));
    private final GameController game;
    private final Set<Display.ItemDisplay> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    public static void register(){}
    public LbcBouncingPoop(GameController game){this.game=game;}
    public int count(){return actors.size();}
    List<Display.ItemDisplay> snapshot(){return List.copyOf(actors);}
    static CompoundTag state(Entity actor){return actor.getAttached(DATA);}
    public static boolean matches(ItemStack stack){var custom=stack.get(DataComponents.CUSTOM_DATA);return !stack.isEmpty()
            &&(stack.is(LbcPursuitItems.BOUNCING_POOP)||custom!=null&&custom.copyTag().getIntOr("govno_raskid",0)==1);}
    private UUID author(CompoundTag d){try{return UUID.fromString(d.getStringOr("author",""));}catch(IllegalArgumentException e){return null;}}
    private LbcIndirectKills.Owner cause(CompoundTag d){return new LbcIndirectKills.Owner(author(d),d.getLongOr("order",0),false,true);}
    private LbcSpellSource.Source source(Entity actor){var d=state(actor);return new LbcSpellSource.Source(actor.getUUID(),author(d),d.getStringOr("epoch",""),cause(d),Set.of(),true);}
    public boolean use(ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);
        if(!matches(stack)||!owner.isAlive()||owner.isRemoved()||!game.playing()||!LbcDimensionItems.ordinary(owner)
                ||!game.items().wands().validRoleTarget(owner,false)||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.items().timeStop().stopped(owner.level())||!game.canUseGameplayItems(owner)||game.isObserver(owner)||game.stage()==GameController.Stage.ENDING
                ||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner))return false;
        var actor=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());
        actor.setPos(owner.getEyePosition());actor.setYRot(owner.getYRot());actor.setXRot(owner.getXRot()-20);
        var visual=new ItemStack(Items.SNOWBALL);visual.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:3dpoop"));actor.setItemStack(visual);
        var nbt=new CompoundTag();nbt.putInt("interpolation_duration",1);nbt.putInt("teleport_duration",1);nbt.putString("item_display","fixed");
        nbt.putFloat("shadow_radius",0);nbt.putFloat("shadow_strength",0);
        actor.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.level().registryAccess(),nbt));
        actor.setPos(owner.getEyePosition());actor.setYRot(owner.getYRot());actor.setXRot(owner.getXRot()-20);actor.setItemStack(visual);
        actor.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(2),new Quaternionf()));
        var d=new CompoundTag();d.putString("author",owner.getStringUUID());d.putString("epoch",game.items().epoch());
        d.putLong("order",game.items().indirectKills().coldCause(owner,true).order());d.putInt("age",0);
        d.putBoolean("mega",game.lbc().skillsEnabled()&&game.players().get(owner.getUUID()).lbcSkill==9);
        if(owner.getTeam()!=null)d.putString("team",owner.getTeam().getName());
        actor.setDeltaMovement(Vec3.directionFromRotation(actor.getXRot(),actor.getYRot()).scale(.4));
        actor.setAttached(DATA,d);actor.entityTags().add(TAG);game.items().markCurrentItemEpoch(actor);
        if(!owner.level().hasChunkAt(actor.blockPosition())||!owner.level().addFreshEntity(actor)){actor.discard();return false;}
        actors.add(actor);game.items().spellSources().bind(actor,source(actor));owner.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(stack.getItem()));stack.shrink(1);owner.getInventory().setChanged();owner.containerMenu.broadcastChanges();return true;
    }
    public void loaded(Entity entity){if(!(entity instanceof Display.ItemDisplay actor)||!entity.entityTags().contains(TAG))return;
        var d=state(actor);if(d==null||author(d)==null||!game.items().currentEpoch(d.getStringOr("epoch",""))||!game.playing()){
            if(!MatchActorCheckpoint.restoring(entity))entity.discard();return;}
        actors.add(actor);game.items().spellSources().bind(actor,source(actor));
    }
    public void unloaded(Entity entity){actors.remove(entity);}
    private boolean target(CompoundTag d,Entity target){return target instanceof LivingEntity&&target.getType().builtInRegistryHolder().is(MOBS)
            &&game.items().wands().validTarget(target)&&!target.getUUID().equals(author(d))
            &&(!(target instanceof ServerPlayer player)||d.getStringOr("team","").isEmpty()||player.getTeam()==null||!player.getTeam().getName().equals(d.getStringOr("team","")));}
    public void tick(){if(game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return;game.items().cloneSkills().beforeOwnedStep(actors);for(var actor:List.copyOf(actors)){
        var d=state(actor);if(actor.isRemoved()||d==null||!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(d.getStringOr("epoch",""))){retire(actor);continue;}
        var level=(ServerLevel)actor.level();if(game.items().timeStop().stopped(level)||actor.entityTags().contains("time_stopped")||!level.hasChunkAt(actor.blockPosition()))continue;
        var before=actor.position();var velocity=actor.getDeltaMovement();
        if(!actor.isNoGravity())velocity=velocity.add(0,-.03,0);
        if(!level.hasChunkAt(BlockPos.containing(before.add(velocity))))continue;
        // Display entities have zero native dimensions: give the collision engine the
        // physical half-block projectile body for this move, without moving its render origin.
        actor.noPhysics=false;actor.setBoundingBox(new AABB(before.x-.25,before.y,before.z-.25,before.x+.25,before.y+.5,before.z+.25));
        actor.move(MoverType.SELF,velocity);
        var next=actor.position();var travelled=next.subtract(before);
        boolean x=Math.abs(travelled.x-velocity.x)>1e-7,y=Math.abs(travelled.y-velocity.y)>1e-7,z=Math.abs(travelled.z-velocity.z)>1e-7;
        var reflected=new Vec3(x?-velocity.x:velocity.x,y?-velocity.y:velocity.y,z?-velocity.z:velocity.z);
        actor.setDeltaMovement(reflected);actor.setYRot((float)Math.toDegrees(Math.atan2(-reflected.x,reflected.z)));
        actor.setXRot((float)-Math.toDegrees(Math.atan2(reflected.y,reflected.horizontalDistance())));
        var sweep=new AABB(before,next).inflate(.5);
        var hits=level.getEntitiesOfClass(LivingEntity.class,sweep,e->target(d,e)
                &&(e.getBoundingBox().inflate(.25).contains(before)||e.getBoundingBox().inflate(.25).clip(before,next).isPresent()));
        if(!hits.isEmpty()){
            game.items().indirectKills().trapChanges(level,cause(d),()->LbcDropBlocks.setBlock(level,BlockPos.containing(next),Blocks.DIRT.defaultBlockState(),false));
            for(var hit:hits){if(hit instanceof ServerPlayer player&&!game.items().magic().rejectEffect(player,MobEffects.NAUSEA))hit.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,300,0),actor);
                hit.addEffect(ItemEffectLevels.create(MobEffects.WITHER,60,10),actor);}
            sound(level,next);if(d.getBooleanOr("mega",false))mega(actor,d,next);retire(actor);continue;
        }
        if(x||y||z){
            soil(level,next,cause(d));if(d.getBooleanOr("mega",false))mega(actor,d,before);
        }
        if(actor.isRemoved()||!actors.contains(actor))continue;
        var updated=d.copy();int age=d.getIntOr("age",0)+1;updated.putInt("age",age);actor.setAttached(DATA,updated);if(age>=600)retire(actor);
    }}
    private void soil(ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause){game.items().indirectKills().trapChanges(level,cause,()->{
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-1.5,-1.5,-1.5)),BlockPos.containing(at.add(1.5,1.5,1.5)))){
            if(!level.hasChunkAt(pos))continue;var old=level.getBlockState(pos);
            var replacement=old.is(Blocks.DIRT)?Blocks.AIR:old.is(Blocks.COARSE_DIRT)?Blocks.DIRT:old.is(Blocks.PODZOL)?Blocks.COARSE_DIRT:
                    old.is(Blocks.ROOTED_DIRT)?Blocks.PODZOL:old.is(SOIL)?Blocks.ROOTED_DIRT:null;
            if(replacement!=null)LbcDropBlocks.setBlock(level,pos,replacement.defaultBlockState(),true);
        }});}
    private void mega(Display.ItemDisplay actor,CompoundTag d,Vec3 at){
        var level=(ServerLevel)actor.level();var owner=game.server().getPlayerList().getPlayer(author(d));
        for(var player:level.players())if(!game.isObserver(player)&&player.position().distanceToSqr(at)<=4)player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,3,false,false),actor);
        for(int i=0;i<10;i++)game.items().govnomet().spawnDirt(level,at,source(actor));
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.PLAYER_EXPLOSION),actor,owner);
        damage=game.items().indirectKills().captureDamage(damage,cause(d));final var captured=damage;
        game.items().indirectKills().capturedWorldChanges(level,cause(d),()->level.explode(actor,captured,null,at,1,false,Level.ExplosionInteraction.TNT));sound(level,at);
    }
    private static void sound(ServerLevel level,Vec3 at){level.playSound(null,at.x,at.y,at.z,net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse("minecraft:block.gravel.place")),net.minecraft.sounds.SoundSource.MASTER,1,0);}
    private void retire(Display.ItemDisplay actor){actors.remove(actor);actor.discard();}
    /** Accepted actors retain their causal author after a player lifecycle transition. */
    public void forget(ServerPlayer owner){}
    public void stop(){for(var actor:List.copyOf(actors))retire(actor);actors.clear();}

    static boolean cloneRecognizes(Entity e){return e.entityTags().contains(TAG);}
    LbcCloneSkills.ActorImage cloneImage(Entity original) {
        var state=state(original);if(!(original instanceof Display.ItemDisplay)||state==null||!actors.contains(original))return null;
        var saved=state.copy();
        // DATA is the sole gameplay clock/author state. Native NBT/metadata carries its presentation.
        return CloneComplexProjectileStates.image(clone->{clone.setAttached(DATA,saved.copy());
            actors.add((Display.ItemDisplay)clone);game.items().spellSources().bind(clone,source(clone));
        },clone->actors.remove(clone));
    }


    /** Native DATA/NBT is already restored; only the suppressed loaded-body index is missing. */
    void reindexRestored(Iterable<Entity> restored){
        actors.clear();
        for(var entity:restored){
            if(entity.isRemoved()||!(entity instanceof Display.ItemDisplay actor)||!entity.entityTags().contains(TAG))continue;
            var state=state(actor);
            if(state==null||author(state)==null||!game.playing()||!game.items().currentEpoch(state.getStringOr("epoch","")))continue;
            actors.add(actor);game.items().spellSources().bind(actor,source(actor));
        }
    }

}
