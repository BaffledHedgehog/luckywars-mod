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
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native closed creature drops; actual source attribution never changes native mob/projectile owners. */
public final class LbcCreatureDrops {
    public enum Kind { VEX_OLD, VEX, SKELETON_OLD, FAT_SKELETON, BOB_OLD, BULLETS }
    private static final String ACTOR="skywars2.creature_drop", OWNER="skywars2.creature_owner.";
    private static final CompoundTag VEX=preset("{Health:1.0f,attributes:[{id:\"minecraft:max_health\",base:1}]}");
    private static final String NORMAL_VEX="skywars2.normal_vex_drop";
    private static final CompoundTag NORMAL_VEX_DATA=preset("{Tags:[\"skywars2.normal_vex_drop\"],DeathLootTable:\"lbc:vex_sword\",equipment:{mainhand:{}},Health:1.0f,attributes:[{id:\"minecraft:max_health\",base:1}]}");
    private static final CompoundTag SKELETON=preset("""
            {PersistenceRequired:1b,CustomNameVisible:1b,CustomName:{"italic":false,"translate":"fat_skeleton"},equipment:{mainhand:{id:"minecraft:bone",components:{"minecraft:enchantments":{"minecraft:knockback":1},"minecraft:custom_name":{"italic":false,"translate":"omega_bone"},"minecraft:attribute_modifiers":[{type:"minecraft:attack_damage",slot:"mainhand",amount:7,operation:"add_value",id:"minecraft:base_attack_damage"},{type:"minecraft:attack_speed",slot:"mainhand",id:"minecraft:base_attack_speed",amount:-3.5,operation:"add_value"},{type:"minecraft:knockback_resistance",slot:"mainhand",id:"d90ihe89fyw39tg",amount:1,operation:"add_value"},{type:"minecraft:movement_speed",slot:"mainhand",id:"ih89urgh8ghfdg",amount:-0.7,operation:"add_multiplied_base"}]},count:1},feet:{id:"minecraft:bone_meal",count:64},legs:{id:"minecraft:bone",count:32},chest:{id:"minecraft:iron_chestplate",count:1},head:{id:"minecraft:skeleton_skull",count:1}},drop_chances:{mainhand:1.0f,legs:1.0f,feet:1.0f},attributes:[{id:"minecraft:movement_speed",base:0.6d},{id:"minecraft:armor",base:20},{id:"minecraft:attack_knockback",base:100},{id:"minecraft:armor_toughness",base:5}]}
            """);
    private static final CompoundTag FAT_SKELETON=preset("""
            {Tags:["fat_skeleton"],DeathLootTable:"lbc:fat_skeleton_rewards",PersistenceRequired:1b,CustomNameVisible:1b,CustomName:{"italic":false,"translate":"fat_skeleton"},equipment:{mainhand:{id:"minecraft:bone",components:{"minecraft:enchantments":{"minecraft:knockback":1},"minecraft:custom_name":{"italic":false,"translate":"omega_bone"},"minecraft:attribute_modifiers":[{type:"minecraft:attack_damage",slot:"mainhand",id:"minecraft:base_attack_damage",amount:7,operation:"add_value"},{type:"minecraft:attack_speed",slot:"mainhand",id:"minecraft:base_attack_speed",amount:-3.5,operation:"add_value"},{type:"minecraft:knockback_resistance",slot:"mainhand",id:"dhesfubugbrgbffgg",amount:1,operation:"add_value"},{type:"minecraft:movement_speed",slot:"mainhand",id:"ughvshfg7udfhgud",amount:-0.7,operation:"add_multiplied_base"}]},count:1},feet:{id:"minecraft:bone_meal",count:64},legs:{id:"minecraft:bone",count:32},chest:{id:"minecraft:iron_chestplate",count:1},head:{id:"minecraft:skeleton_skull",count:1}},drop_chances:{mainhand:0.0f,offhand:0.0f,head:0.0f,chest:0.0f,legs:0.0f,feet:0.0f},attributes:[{id:"minecraft:movement_speed",base:0.6d},{id:"minecraft:armor",base:20},{id:"minecraft:attack_knockback",base:100},{id:"minecraft:armor_toughness",base:5},{id:"minecraft:attack_damage",base:7}]}
            """);
    private static final CompoundTag BOB=preset("""
            {CustomNameVisible:1b,PersistenceRequired:1b,IsBaby:0b,CustomName:{"color":"green","italic":false,"translate":"bob"},equipment:{mainhand:{id:"minecraft:netherite_sword",components:{"minecraft:enchantments":{"minecraft:sharpness":5,"minecraft:smite":5,"minecraft:bane_of_arthropods":5,"minecraft:knockback":2,"minecraft:fire_aspect":2,"minecraft:looting":3,"minecraft:sweeping_edge":3,"minecraft:unbreaking":3},"minecraft:custom_name":{"italic":false,"translate":"bob_sword"}},count:1},offhand:{id:"minecraft:enchanted_golden_apple",components:{"minecraft:custom_name":{"italic":false,"translate":"bob_apple"}},count:6},feet:{id:"minecraft:netherite_boots",components:{"minecraft:enchantments":{"minecraft:protection":4,"minecraft:fire_protection":4,"minecraft:feather_falling":4,"minecraft:blast_protection":4,"minecraft:projectile_protection":4,"minecraft:thorns":3,"minecraft:depth_strider":3,"minecraft:frost_walker":2,"minecraft:unbreaking":3,"minecraft:soul_speed":3},"minecraft:custom_name":{"italic":false,"translate":"bob_boots"}},count:1},legs:{id:"minecraft:netherite_leggings",components:{"minecraft:enchantments":{"minecraft:protection":4,"minecraft:fire_protection":4,"minecraft:blast_protection":4,"minecraft:projectile_protection":4,"minecraft:thorns":3,"minecraft:unbreaking":3},"minecraft:custom_name":{"italic":false,"translate":"bob_leggings"}},count:1},chest:{id:"minecraft:netherite_chestplate",components:{"minecraft:enchantments":{"minecraft:protection":4,"minecraft:fire_protection":4,"minecraft:blast_protection":4,"minecraft:projectile_protection":4,"minecraft:thorns":3,"minecraft:unbreaking":3},"minecraft:custom_name":{"italic":false,"translate":"bob_chestplate"}},count:1},head:{id:"minecraft:netherite_helmet",components:{"minecraft:enchantments":{"minecraft:protection":4,"minecraft:fire_protection":4,"minecraft:blast_protection":4,"minecraft:projectile_protection":4,"minecraft:respiration":3,"minecraft:aqua_affinity":1,"minecraft:thorns":1,"minecraft:unbreaking":1},"minecraft:custom_name":{"italic":false,"translate":"bob_helmet"}},count:1}},drop_chances:{mainhand:0.5f,offhand:0.5f,head:0.5f,chest:0.5f,legs:0.5f,feet:0.5f},attributes:[{id:"minecraft:follow_range",base:400}]}
            """);
    private static final CompoundTag BULLET=preset("{Tags:[\"angry\"],Steps:40}");
    static final String ARMOR_SHELL="skywars2.shulker_armor_shell";
    private static final CompoundTag ARMOR_SHELL_DATA=preset("{Tags:[\"shell\",\"skywars2.shulker_armor_shell\"],Motion:[0d,5d,0d],Steps:40}");
    private static final int[][] BULLET_OFFSETS={{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,0}};
    private final GameController game;
    private final Set<Entity> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<net.minecraft.world.entity.boss.enderdragon.EnderDragon> nativeDragons=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();
    public LbcCreatureDrops(GameController game){this.game=game;}
    private static CompoundTag preset(String snbt){try{return SourceKnockbackBalance.entity(TagParser.parseCompoundFully(snbt));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}}
    private static boolean matches(ItemEntity token,Kind kind){return LbcCreatureDropItem.matches(token.getItem(),switch(kind){case VEX_OLD->LbcCreatureDropItem.Kind.OLD_VEX;case VEX->LbcCreatureDropItem.Kind.VEX;case SKELETON_OLD->LbcCreatureDropItem.Kind.OLD_SKELETON;case FAT_SKELETON->LbcCreatureDropItem.Kind.FAT_SKELETON;case BOB_OLD->LbcCreatureDropItem.Kind.OLD_BOB;case BULLETS->LbcCreatureDropItem.Kind.BULLETS;});}
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
            case VEX->{
                if(Level.isInSpawnableBounds(token.blockPosition()))for(int i=0;i<10;i++){
                    var vex=EntityTypes.VEX.create(level,EntitySpawnReason.COMMAND);
                    if(vex!=null){vex.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),NORMAL_VEX_DATA.copy()));admit(level,vex,token.position(),author);}
                }
            }
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
            case SKELETON_OLD,FAT_SKELETON->{
                if(Level.isInSpawnableBounds(token.blockPosition())){
                    var skeleton=EntityTypes.SKELETON.create(level,EntitySpawnReason.COMMAND);
                    if(skeleton!=null){skeleton.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),(kind==Kind.FAT_SKELETON?FAT_SKELETON:SKELETON).copy()));admit(level,skeleton,token.position(),author);}
                }
            }
            case BOB_OLD->{
                if(Level.isInSpawnableBounds(token.blockPosition())){
                    var bob=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
                    if(bob!=null){bob.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),BOB.copy()));admit(level,bob,token.position(),author);}
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
        prepare(actor,author);
        if(!level.addFreshEntity(actor)){tracked.remove(actor);actor.discard();}
    }
    /** Same native Shulker transport/lifecycle as the trap, with the helmet's real projectile owner. */
    ShulkerBullet launchArmorShell(ServerPlayer owner,ServerPlayer target){
        var level=owner.level();var at=owner.position().add(0,3,0);
        if(!Level.isInSpawnableBounds(BlockPos.containing(at))||!level.hasChunkAt(BlockPos.containing(at)))return null;
        var bullet=EntityTypes.SHULKER_BULLET.create(level,EntitySpawnReason.COMMAND);if(bullet==null)return null;
        var data=ARMOR_SHELL_DATA.copy();
        data.put("Owner",UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE,owner.getUUID()).getOrThrow());
        if(target!=null)data.put("Target",UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE,target.getUUID()).getOrThrow());
        bullet.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
        admit(level,bullet,at,owner);return bullet.isRemoved()?null:bullet;
    }
    /** Keep native egg finalization and data, then remove the unwanted vanilla sword before insertion. */
    void adoptEggVex(Vex actor,ServerPlayer author){
        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,net.minecraft.world.item.ItemStack.EMPTY);
        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,net.minecraft.world.item.ItemStack.EMPTY);
        actor.entityTags().add(NORMAL_VEX);
        prepare(actor,author);
    }
    void adoptEggSkeleton(net.minecraft.world.entity.monster.skeleton.Skeleton actor,ServerPlayer author){
        prepare(actor,author);
    }
    /** Compass Wardens share native hostile ownership, target protection, Time Stop and epoch cleanup. */
    void adoptDeathWarden(net.minecraft.world.entity.monster.warden.Warden actor,ServerPlayer author){
        prepare(actor,author);
    }
    private void prepare(Entity actor,ServerPlayer author){
        if(actor instanceof net.minecraft.world.entity.Mob){actor.entityTags().add("skywars2.players_only_trap");game.items().spellSources().unowned(actor);}
        actor.entityTags().add(ACTOR);if(author!=null){authors.put(author.getUUID(),author);actor.entityTags().add(OWNER+author.getUUID());}
        game.items().markCurrentItemEpoch(actor);tracked.add(actor);game.items().timeStop().prepareFrozen(actor);
    }
    public static boolean managed(Entity actor){return actor!=null && (actor instanceof Vex || actor instanceof AbstractSkeleton || actor instanceof Zombie || actor instanceof ShulkerBullet || actor instanceof net.minecraft.world.entity.monster.warden.Warden) && actor.entityTags().contains(ACTOR);}
    public LivingEntity owner(Entity actor){
        if(LbcSpellSource.unownedHazard(actor)){if(actor instanceof LivingEntity living)return living;if(actor instanceof net.minecraft.world.entity.projectile.Projectile shot&&shot.getOwner() instanceof LivingEntity living)return living;}
        if(actor instanceof ItemEntity token && LbcCreatureDropItem.matches(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        if(actor instanceof ShulkerBullet bullet&&bullet.getOwner() instanceof LivingEntity current)return current;
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER))try{
            var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);return online==null?authors.get(id):online;
        }catch(IllegalArgumentException ignored){}
        return null;
    }
    private boolean active(Entity actor){return game.playing() && game.stage()!=GameController.Stage.ENDING && actor.level()==game.maps().level() && game.items().currentEpoch(actor);}
    public boolean canAffect(Entity actor,Entity target){
        if(actor!=null&&actor.entityTags().contains("skywars2.balance.old_scroll_actor"))return game.items().spellSources().canAffect(actor,target);
        if(!managed(actor))return true;
        if(target==null || !active(actor) || !game.items().wands().validTarget(target))return false;
        if(game.items().spellSources().lootProjectile(actor))return game.items().spellSources().canAffect(actor,target);
        if(LbcSpellSource.unownedHazard(actor))return true;
        if(actor instanceof ShulkerBullet || actor instanceof Vex && actor.entityTags().contains(NORMAL_VEX))return true; // WILD trap: self, allies and enemies are all eligible.
        return !(owner(actor) instanceof ServerPlayer author) || target==author || game.items().wands().target(author,target);
    }
    public boolean canTarget(Entity actor,LivingEntity target){
        if(target==null)return true;
        // A shell may accidentally hit any participant without acquiring its caster or allies.
        if(actor instanceof ShulkerBullet&&actor.entityTags().contains(ARMOR_SHELL)&&!game.items().spellSources().lootProjectile(actor)){
            var current=owner(actor);
            if(target==current||current instanceof ServerPlayer author&&!game.items().wands().projectileTarget(author,target))return false;
        }
        return canAffect(actor,target);
    }
    public void loaded(Entity actor){if(actor instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon)nativeDragons.add(dragon);if(!managed(actor))return;if(!active(actor)){actor.discard();return;}tracked.add(actor);game.items().timeStop().prepareFrozen(actor);}
    public void unloaded(Entity actor){nativeDragons.remove(actor);if(managed(actor))tracked.remove(actor);}
    /** Source mobs_1s applies to native dragons even with NoAI, in every ordinary dimension. */
    void nativeMobSecond(){
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.server().tickRateManager().runsNormally()||game.server().getTickCount()%20!=0)return;
        nativeDragons.removeIf(Entity::isRemoved);
        for(var dragon:nativeDragons)if(LbcDimensionItems.ordinary(dragon.level())&&!game.items().timeStop().stopped((ServerLevel)dragon.level())
                &&dragon.getPhaseManager().getCurrentPhase().getPhase()==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOVERING)
            dragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.STRAFE_PLAYER);
    }
    public void tick(){for(var actor:List.copyOf(tracked))if(actor.isRemoved())tracked.remove(actor);else if(!active(actor)){tracked.remove(actor);actor.discard();}}
    public void stop(){var actors=List.copyOf(tracked);tracked.clear();for(var actor:actors)if(!actor.isRemoved())actor.discard();authors.clear();}
    public List<Entity> snapshot(){return List.copyOf(tracked);}
    public int trackedCount(){return tracked.size();}
}
