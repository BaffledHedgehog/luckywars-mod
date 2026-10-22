package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native harmless falling anvil plus the complete normal/legacy book rewards. */
public final class LbcOpAnvilDrop {
    private static final String ACTOR="skywars2.op_anvil_drop";
    private static final String REWARD="skywars2.op_anvil_reward";
    private static final CompoundTag ANVIL=preset("{BlockState:\"minecraft:anvil\",Time:1,DropItem:1b,HurtEntities:0b,FallHurtMax:0,fall_distance:0.0f,FallHurtAmount:0.0f}");
    private static final CompoundTag XP=preset("{Value:1395}");
    private final GameController game;
    private final Set<FallingBlockEntity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Entity> rewards=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity,LivingEntity> causes=new IdentityHashMap<>();
    private final Map<UUID,LivingEntity> unloadedCauses=new HashMap<>();

    public LbcOpAnvilDrop(GameController game){this.game=game;}
    private static CompoundTag preset(String snbt){
        try{return TagParser.parseCompoundFully(snbt);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException exception){throw new ExceptionInInitializerError(exception);}
    }
    public boolean ready(ItemEntity token,LbcOpAnvilDropItem.Kind kind){
        if(token.isRemoved() || token.getAge()>=5999 || !LbcOpAnvilDropItem.matches(token.getItem(),kind)
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        var center=token.blockPosition();
        for(int x=(center.getX()-1)>>4;x<=(center.getX()+1)>>4;x++)
            for(int z=(center.getZ()-1)>>4;z<=(center.getZ()+1)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    public boolean spawn(ItemEntity token,LbcOpAnvilDropItem.Kind kind){
        if(!ready(token,kind))return false;
        var level=(ServerLevel)token.level();var center=token.blockPosition();
        var cause=game.items().specialDrops().owner(token);
        game.items().indirectKills().worldChanges(level,cause,true,()->{
            if(!level.isDebug())LbcDropBlocks.fill(level,center,BlockPos.containing(token.position().add(0,5,0)),
                    Blocks.AIR,kind==LbcOpAnvilDropItem.Kind.NORMAL);
        });
        var fallingPos=token.position().add(0,5,0);
        if(Level.isInSpawnableBounds(BlockPos.containing(fallingPos))){
            var actor=EntityTypes.FALLING_BLOCK.create(level,EntitySpawnReason.COMMAND);
            if(actor!=null){
                actor.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),ANVIL.copy()));
                actor.snapTo(kind==LbcOpAnvilDropItem.Kind.NORMAL?center.getX()+.5:token.getX(),fallingPos.y,
                        kind==LbcOpAnvilDropItem.Kind.NORMAL?center.getZ()+.5:token.getZ(),actor.getYRot(),actor.getXRot());
                prepare(actor,cause);if(!level.addFreshEntity(actor)){forget(actor);actor.discard();}
            }
        }
        if(Level.isInSpawnableBounds(center)){
            var orb=EntityTypes.EXPERIENCE_ORB.create(level,EntitySpawnReason.COMMAND);
            if(orb!=null){
                orb.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),XP.copy()));
                orb.snapTo(token.getX(),token.getY(),token.getZ(),orb.getYRot(),orb.getXRot());
                prepare(orb,cause);if(!level.addFreshEntity(orb)){forget(orb);orb.discard();}
            }
        }
        // Native /loot spawn uses a chest context, THIS_ENTITY=executing token, and zero luck.
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,token.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY,token).create(LootContextParamSets.CHEST);
        var table=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.parse(kind==LbcOpAnvilDropItem.Kind.NORMAL?"lbc:opbook":"lbc:opbook_old")));
        for(var stack:table.getRandomItems(params)){
            // Unlike summon defaults, native loot spawn intentionally randomizes item motion.
            var item=new ItemEntity(level,token.getX(),token.getY(),token.getZ(),stack.copy());
            item.setDefaultPickUpDelay();prepare(item,cause);
            if(!level.addFreshEntity(item)){forget(item);item.discard();}
        }
        return true;
    }
    private void prepare(Entity actor,LivingEntity cause){
        actor.entityTags().add(actor instanceof FallingBlockEntity?ACTOR:REWARD);
        game.items().markCurrentItemEpoch(actor);causes.put(actor,cause);
        if(actor instanceof FallingBlockEntity falling)actors.add(falling);else rewards.add(actor);
        game.items().timeStop().prepareFrozen(actor);
    }
    private void forget(Entity actor){actors.remove(actor);rewards.remove(actor);causes.remove(actor);}
    public static boolean managed(Entity actor){
        return actor instanceof FallingBlockEntity && actor.entityTags().contains(ACTOR)
                || (actor instanceof ItemEntity || actor instanceof ExperienceOrb) && actor.entityTags().contains(REWARD);
    }
    public LivingEntity owner(Entity actor){
        if(actor instanceof ItemEntity token && LbcOpAnvilDropItem.kind(token.getItem())!=null)
            return game.items().specialDrops().owner(token);
        return managed(actor)?causes.get(actor):null;
    }
    private boolean active(Entity actor){return game.playing() && game.stage()!=GameController.Stage.ENDING
            && actor.level()==game.maps().level() && game.items().currentEpoch(actor);}
    public void withTick(FallingBlockEntity actor,Runnable nativeTick){
        if(managed(actor) && active(actor) && actor.level() instanceof ServerLevel level){
            if(owner(actor) instanceof ServerPlayer player)game.items().indirectKills().trapChanges(level,player,true,nativeTick);
            else game.items().indirectKills().worldChanges(level,owner(actor),true,nativeTick);
        }
        else nativeTick.run();
    }
    /** Native placement failure/airborne expiry can drop an anvil after discarding its actor. */
    public void inheritDrop(Entity parent,ItemEntity child){
        if(child!=null && parent instanceof FallingBlockEntity && managed(parent) && active(parent))prepare(child,owner(parent));
    }
    public void loaded(Entity actor){
        if(!managed(actor))return;
        if(!active(actor)){unloadedCauses.remove(actor.getUUID());actor.discard();return;}
        if(!causes.containsKey(actor))causes.put(actor,unloadedCauses.remove(actor.getUUID()));
        if(actor instanceof FallingBlockEntity falling)actors.add(falling);else rewards.add(actor);
        game.items().timeStop().prepareFrozen(actor);
    }
    public void unloaded(Entity actor){
        if(!managed(actor))return;actors.remove(actor);rewards.remove(actor);
        if(active(actor) && actor.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)
            unloadedCauses.put(actor.getUUID(),owner(actor));
        // Cause survives synchronous native discard/drop callbacks until this service ticks.
    }
    public void tick(){
        for(var actor:List.copyOf(actors))if(actor.isRemoved())actors.remove(actor);else if(!active(actor)){actors.remove(actor);actor.discard();}
        for(var actor:List.copyOf(rewards))if(actor.isRemoved())rewards.remove(actor);else if(!active(actor)){rewards.remove(actor);actor.discard();}
        causes.keySet().removeIf(actor->actor.isRemoved() || !active(actor) || !actors.contains(actor) && !rewards.contains(actor));
        if(!game.playing() || game.stage()==GameController.Stage.ENDING)unloadedCauses.clear();
    }
    public void stop(){
        var previousActors=List.copyOf(actors);var previousRewards=List.copyOf(rewards);actors.clear();rewards.clear();unloadedCauses.clear();
        for(var actor:previousActors)if(!actor.isRemoved())actor.discard();
        for(var actor:previousRewards)if(!actor.isRemoved())actor.discard();
        causes.clear();
    }
    public int trackedCount(){return actors.size();}
    public List<FallingBlockEntity> snapshot(){return List.copyOf(actors);}
    public List<Entity> snapshotRewards(){return List.copyOf(rewards);}
}
