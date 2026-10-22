package me.skywars2.game;

import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native closed creature drops; actual source attribution never changes native mob/projectile owners. */
public final class LbcCreatureDrops {
    public enum Kind { VEX_OLD, SKELETON_OLD, BULLETS }
    private static final String ACTOR="skywars2.creature_drop", OWNER="skywars2.creature_owner.";
    private static final CompoundTag VEX=preset("{Health:1.0f,attributes:[{id:\"minecraft:max_health\",base:1}]}");
    private static final CompoundTag SKELETON=preset("""
            {PersistenceRequired:1b,CustomNameVisible:1b,CustomName:{"italic":false,"translate":"fat_skeleton"},equipment:{mainhand:{id:"minecraft:bone",components:{"minecraft:enchantments":{"minecraft:knockback":1},"minecraft:custom_name":{"italic":false,"translate":"omega_bone"},"minecraft:attribute_modifiers":[{type:"minecraft:attack_damage",slot:"mainhand",amount:7,operation:"add_value",id:"minecraft:base_attack_damage"},{type:"minecraft:attack_speed",slot:"mainhand",id:"minecraft:base_attack_speed",amount:-3.5,operation:"add_value"},{type:"minecraft:knockback_resistance",slot:"mainhand",id:"d90ihe89fyw39tg",amount:1,operation:"add_value"},{type:"minecraft:movement_speed",slot:"mainhand",id:"ih89urgh8ghfdg",amount:-0.7,operation:"add_multiplied_base"}]},count:1},feet:{id:"minecraft:bone_meal",count:64},legs:{id:"minecraft:bone",count:32},chest:{id:"minecraft:iron_chestplate",count:1},head:{id:"minecraft:skeleton_skull",count:1}},drop_chances:{mainhand:1.0f,legs:1.0f,feet:1.0f},attributes:[{id:"minecraft:movement_speed",base:0.6d},{id:"minecraft:armor",base:20},{id:"minecraft:attack_knockback",base:100},{id:"minecraft:armor_toughness",base:5}]}
            """);
    private static final CompoundTag BULLET=preset("{Tags:[\"angry\"],Steps:40}");
    private static final int[][] BULLET_OFFSETS={{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,0}};
    private final GameController game;
    private final Set<Entity> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();
    public LbcCreatureDrops(GameController game){this.game=game;}
    private static CompoundTag preset(String snbt){try{return TagParser.parseCompoundFully(snbt);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}}
    private static boolean matches(ItemEntity token,Kind kind){return LbcCreatureDropItem.matches(token.getItem(),switch(kind){case VEX_OLD->LbcCreatureDropItem.Kind.OLD_VEX;case SKELETON_OLD->LbcCreatureDropItem.Kind.OLD_SKELETON;case BULLETS->LbcCreatureDropItem.Kind.BULLETS;});}
    public boolean ready(ItemEntity token,Kind kind){
        if(token.isRemoved() || token.getAge()>=5999 || !matches(token,kind) || !game.playing()
                || game.stage()==GameController.Stage.ENDING || token.level()!=game.maps().level()
                || !(token.level() instanceof ServerLevel level) || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        var center=token.blockPosition();int radius=kind==Kind.VEX_OLD?3:kind==Kind.BULLETS?1:0;
        for(int x=(center.getX()-radius)>>4;x<=(center.getX()+radius)>>4;x++)
            for(int z=(center.getZ()-radius)>>4;z<=(center.getZ()+radius)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    public boolean spawn(ItemEntity token,Kind kind){
        if(!ready(token,kind))return false;
        var level=(ServerLevel)token.level();var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        switch(kind){
            case VEX_OLD->{
                var local=new LocalCoordinates(0,0,3);
                for(int i=0;i<5;i++){
                    var at=local.apply(token.position(),new Vec2(token.getXRot(),token.getYRot()));
                    if(Level.isInSpawnableBounds(BlockPos.containing(at))){
                        var vex=EntityTypes.VEX.create(level,EntitySpawnReason.COMMAND);
                        if(vex!=null){vex.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),VEX.copy()));admit(level,vex,at,author);}
                    }
                    if(i<4)token.setYRot(Mth.wrapDegrees(token.getYRot()+72));
                }
            }
            case SKELETON_OLD->{
                if(Level.isInSpawnableBounds(token.blockPosition())){
                    var skeleton=EntityTypes.SKELETON.create(level,EntitySpawnReason.COMMAND);
                    if(skeleton!=null){skeleton.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),SKELETON.copy()));admit(level,skeleton,token.position(),author);}
                }
            }
            case BULLETS->{
                for(var offset:BULLET_OFFSETS){
                    var at=token.position().add(offset[0],2,offset[1]);
                    if(!Level.isInSpawnableBounds(BlockPos.containing(at)))continue;
                    var bullet=EntityTypes.SHULKER_BULLET.create(level,EntitySpawnReason.COMMAND);if(bullet==null)continue;
                    var data=BULLET.copy();
                    var target=nearest(level,at);
                    if(target!=null)data.put("Target",UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE,target.getUUID()).getOrThrow());
                    bullet.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
                    // Keep native owner null: the trap may hit its thrower. Causal author is stored separately.
                    admit(level,bullet,at,author);
                }
            }
        }
        return true;
    }
    private ServerPlayer nearest(ServerLevel level,Vec3 at){
        ServerPlayer result=null;double distance=Double.POSITIVE_INFINITY;
        for(var player:level.players())if(game.items().wands().validTarget(player)){
            double candidate=player.position().distanceToSqr(at);
            if(candidate<distance){distance=candidate;result=player;}
        }
        return result;
    }
    private void admit(ServerLevel level,Entity actor,Vec3 position,ServerPlayer author){
        actor.snapTo(position.x,position.y,position.z,actor.getYRot(),actor.getXRot());
        actor.entityTags().add(ACTOR);if(author!=null){authors.put(author.getUUID(),author);actor.entityTags().add(OWNER+author.getUUID());}
        game.items().markCurrentItemEpoch(actor);tracked.add(actor);game.items().timeStop().prepareFrozen(actor);
        if(!level.addFreshEntity(actor)){tracked.remove(actor);actor.discard();}
    }
    public static boolean managed(Entity actor){return actor!=null && (actor instanceof Vex || actor instanceof AbstractSkeleton || actor instanceof ShulkerBullet) && actor.entityTags().contains(ACTOR);}
    public LivingEntity owner(Entity actor){
        if(actor instanceof ItemEntity token && LbcCreatureDropItem.matches(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER))try{
            var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);return online==null?authors.get(id):online;
        }catch(IllegalArgumentException ignored){}
        return null;
    }
    private boolean active(Entity actor){return game.playing() && game.stage()!=GameController.Stage.ENDING && actor.level()==game.maps().level() && game.items().currentEpoch(actor);}
    public boolean canAffect(Entity actor,Entity target){
        if(!managed(actor))return true;
        if(target==null || !active(actor) || !game.items().wands().validTarget(target))return false;
        if(actor instanceof ShulkerBullet)return true; // WILD trap: self, allies and enemies are all eligible.
        return !(owner(actor) instanceof ServerPlayer author) || target==author || game.items().wands().target(author,target);
    }
    public boolean canTarget(Entity actor,LivingEntity target){return target==null || canAffect(actor,target);}
    public void loaded(Entity actor){if(!managed(actor))return;if(!active(actor)){actor.discard();return;}tracked.add(actor);game.items().timeStop().prepareFrozen(actor);}
    public void unloaded(Entity actor){if(managed(actor))tracked.remove(actor);}
    public void tick(){for(var actor:List.copyOf(tracked))if(actor.isRemoved())tracked.remove(actor);else if(!active(actor)){tracked.remove(actor);actor.discard();}}
    public void stop(){var actors=List.copyOf(tracked);tracked.clear();for(var actor:actors)if(!actor.isRemoved())actor.discard();authors.clear();}
    public List<Entity> snapshot(){return List.copyOf(tracked);}
    public int trackedCount(){return tracked.size();}
}
