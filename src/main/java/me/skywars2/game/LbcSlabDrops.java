package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.*;

/** Two ordinary summoned baby zombies; native equipment/effects/AI remain authoritative. */
public final class LbcSlabDrops {
    private static final String ACTOR="skywars2.slab_drop", OWNER="skywars2.slab_owner.";
    private static final CompoundTag NORMAL=preset(true), OLD=preset(false);
    private final GameController game;
    private final Set<Zombie> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();
    public LbcSlabDrops(GameController game){this.game=game;}
    private static CompoundTag preset(boolean normal){
        String label=normal?"{italic:false,translate:\"agressive_slab\"}":"{italic:false,text:\"Агрессивный полублок\"}";
        String text="{"+(normal?"PersistenceRequired:1b,":"")+"DeathLootTable:\"empty\",IsBaby:1b,equipment:{head:{id:\"minecraft:oak_slab\",count:1,components:{\"minecraft:custom_name\":"+label+",\"minecraft:attribute_modifiers\":[{slot:\"mainhand\",id:\"minecraft:base_attack_damage\",type:\"minecraft:attack_damage\",amount:"+(normal?6:3)+",operation:\"add_value\"}]"+(normal?",\"minecraft:tooltip_style\":\"lbc:tier_d\"":"")+"}}},drop_chances:{head:1.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}";
        try{return TagParser.parseCompoundFully(text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}
    }
    public boolean ready(ItemEntity token,LbcSlabDropItem.Kind kind){
        if(token.isRemoved() || token.getAge()>=5999 || !LbcSlabDropItem.matches(token.getItem(),kind) || !game.playing() || game.stage()==GameController.Stage.ENDING || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level) || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        var pos=BlockPos.containing(token.position());return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    public boolean spawn(ItemEntity token,LbcSlabDropItem.Kind kind){
        if(!ready(token,kind))return false;if(!net.minecraft.world.level.Level.isInSpawnableBounds(BlockPos.containing(token.position())))return true;var level=(ServerLevel)token.level();var zombie=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);if(zombie==null)return true;
        zombie.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),(kind==LbcSlabDropItem.Kind.NORMAL?NORMAL:OLD).copy()));
        zombie.snapTo(token.getX(),token.getY(),token.getZ(),zombie.getYRot(),zombie.getXRot());
        var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));prepare(zombie,author);
        if(!level.addFreshEntity(zombie)){tracked.remove(zombie);zombie.discard();}return true;
    }
    private void prepare(Zombie actor,ServerPlayer author){actor.entityTags().add(ACTOR);if(author!=null){authors.put(author.getUUID(),author);actor.entityTags().removeIf(t->t.startsWith(OWNER));actor.entityTags().add(OWNER+author.getUUID());}game.items().markCurrentItemEpoch(actor);tracked.add(actor);game.items().timeStop().prepareFrozen(actor);}
    public static boolean managed(Entity actor){return actor instanceof Zombie && actor.entityTags().contains(ACTOR);}
    public LivingEntity owner(Entity actor){
        if(actor instanceof ItemEntity token && LbcSlabDropItem.matches(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER)){try{var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);return online==null?authors.get(id):online;}catch(IllegalArgumentException ignored){}}
        return null;
    }
    private boolean active(Entity actor){return game.playing() && game.stage()!=GameController.Stage.ENDING && actor.level()==game.maps().level() && game.items().currentEpoch(actor);}
    public boolean canAffect(Entity source,Entity target){if(!managed(source))return true;if(target==null || !active(source) || !game.items().wands().validTarget(target))return false;return !(owner(source) instanceof ServerPlayer player) || target==player || game.items().wands().target(player,target);}
    public boolean canTarget(Entity source,LivingEntity target){return target==null || canAffect(source,target);}
    /** Native conversion owns all equipment/effect/loot decisions; only cleanup and cause are retained. */
    public void inheritConversion(Entity parent,Entity child){if(managed(parent) && active(parent) && child instanceof Zombie zombie)prepare(zombie,LbcIndirectKills.playerSource(owner(parent)));}
    public void loaded(Entity actor){if(!managed(actor))return;if(!active(actor)){actor.discard();return;}tracked.add((Zombie)actor);game.items().timeStop().prepareFrozen(actor);}
    public void unloaded(Entity actor){if(managed(actor))tracked.remove(actor);}
    public void tick(){for(var actor:List.copyOf(tracked)){if(actor.isRemoved())tracked.remove(actor);else if(!active(actor)){tracked.remove(actor);actor.discard();}}}
    public void stop(){var old=List.copyOf(tracked);tracked.clear();for(var actor:old)if(!actor.isRemoved())actor.discard();authors.clear();}
    public int trackedCount(){return tracked.size();}
    public List<Zombie> snapshot(){return List.copyOf(tracked);}
}
