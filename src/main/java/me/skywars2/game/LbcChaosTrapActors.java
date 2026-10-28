package me.skywars2.game;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Original normal Bob/Aimbob, evil Lucky Blocks and moving forest; shared dispatcher owns debit. */
public final class LbcChaosTrapActors {
    private static final String ACTOR="skywars2.chaos_trap_actor", OWNER=ACTOR+".owner.", AGE=ACTOR+".age.";
    private record Payload(EntityType<?> type,double x,double y,double z,CompoundTag data){}
    private static final CompoundTag BOB=preset("{Tags:[\"bob\"],DeathLootTable:\"lbc:bob\",CustomNameVisible:1b,PersistenceRequired:1b,IsBaby:0b,CustomName:{\"color\":\"green\",\"italic\":false,\"translate\":\"bob\"},equipment:{mainhand:{id:\"minecraft:netherite_sword\",components:{\"minecraft:enchantments\":{\"minecraft:sharpness\":5,\"minecraft:smite\":5,\"minecraft:bane_of_arthropods\":5,\"minecraft:knockback\":2,\"minecraft:fire_aspect\":2,\"minecraft:looting\":3,\"minecraft:sweeping_edge\":3,\"minecraft:unbreaking\":3},\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"bob_sword\"}},count:1},offhand:{id:\"minecraft:enchanted_golden_apple\",components:{\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"bob_apple\"}},count:6},feet:{id:\"minecraft:netherite_boots\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:feather_falling\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:thorns\":3,\"minecraft:depth_strider\":3,\"minecraft:frost_walker\":2,\"minecraft:unbreaking\":3,\"minecraft:soul_speed\":3},\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"bob_boots\"}},count:1},legs:{id:\"minecraft:netherite_leggings\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:thorns\":3,\"minecraft:unbreaking\":3},\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"bob_leggings\"}},count:1},chest:{id:\"minecraft:netherite_chestplate\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:thorns\":3,\"minecraft:unbreaking\":3},\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"bob_chestplate\"}},count:1},head:{id:\"minecraft:netherite_helmet\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:respiration\":3,\"minecraft:aqua_affinity\":1,\"minecraft:thorns\":1,\"minecraft:unbreaking\":1},\"minecraft:custom_name\":{\"italic\":false,\"translate\":\"bob_helmet\"}},count:1}},drop_chances:{mainhand:0.0f,offhand:0.0f,head:0.0f,chest:0.0f,legs:0.0f,feet:0.0f},attributes:[{id:\"minecraft:follow_range\",base:400}]}");
    private static final CompoundTag AIMBOB=preset("{Tags:[\"aimbob\"],DeathLootTable:\"lbc:aimbob\",PersistenceRequired:1b,active_effects:[{id:\"minecraft:instant_damage\",amplifier:100b,duration:20}],CustomName:{\"color\":\"aqua\",\"italic\":false,\"translate\":\"aimbob\"},equipment:{mainhand:{id:\"minecraft:bow\",components:{\"minecraft:enchantments\":{\"minecraft:unbreaking\":3,\"minecraft:power\":5,\"minecraft:punch\":2,\"minecraft:flame\":1,\"minecraft:infinity\":1,\"minecraft:mending\":1},\"minecraft:attribute_modifiers\":[{slot:\"mainhand\",id:\"hdvfugergeghjkjkd\",type:\"minecraft:movement_speed\",amount:1,operation:\"add_multiplied_total\"}],\"minecraft:custom_name\":{\"color\":\"gold\",\"italic\":false,\"translate\":\"aimbob_bow\"}},count:1},offhand:{id:\"minecraft:apple\",components:{\"minecraft:custom_name\":{\"color\":\"gold\",\"italic\":false,\"translate\":\"aimbob_apple\"},\"minecraft:item_model\":\"lbc:aimbobapple\",\"minecraft:custom_data\":{aimbobapple:1b}},count:6},feet:{id:\"minecraft:iron_boots\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:feather_falling\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:thorns\":3,\"minecraft:depth_strider\":3,\"minecraft:frost_walker\":2,\"minecraft:unbreaking\":3,\"minecraft:mending\":1,\"minecraft:soul_speed\":3},\"minecraft:custom_name\":{\"color\":\"gold\",\"italic\":false,\"translate\":\"aimbob_boots\"}},count:1},legs:{id:\"minecraft:iron_leggings\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:thorns\":3,\"minecraft:unbreaking\":3,\"minecraft:mending\":1},\"minecraft:custom_name\":{\"color\":\"gold\",\"italic\":false,\"translate\":\"aimbob_leggings\"}},count:1},chest:{id:\"minecraft:iron_chestplate\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:thorns\":3,\"minecraft:unbreaking\":3,\"minecraft:mending\":1},\"minecraft:custom_name\":{\"color\":\"gold\",\"italic\":false,\"translate\":\"aimbob_chestplate\"}},count:1},head:{id:\"minecraft:iron_helmet\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:respiration\":1,\"minecraft:aqua_affinity\":3,\"minecraft:thorns\":3,\"minecraft:unbreaking\":3,\"minecraft:mending\":1},\"minecraft:custom_name\":{\"color\":\"gold\",\"italic\":false,\"translate\":\"aimbob_helmet\"}},count:1}},drop_chances:{mainhand:0.0f,offhand:0.0f,head:0.0f,chest:0.0f,legs:0.0f,feet:0.0f}}");
    private static final CompoundTag SHISH=preset("{CustomNameVisible:1b,DeathLootTable:\"lbc:reactive_shish\",PersistenceRequired:1b,Health:30.0f,IsBaby:1b,CustomName:{\"color\":\"green\",\"italic\":false,\"translate\":\"reactive_shish\"},active_effects:[{id:\"minecraft:jump_boost\",amplifier:6b,duration:2000000,show_particles:0b},{id:\"minecraft:fire_resistance\",duration:2000000,show_particles:0b}],attributes:[{id:\"minecraft:max_health\",base:30},{id:\"minecraft:attack_knockback\",base:1}],equipment:{head:{id:\"minecraft:spruce_sapling\",count:1}},drop_chances:{head:0.0f}}");
    private static final List<Payload> EVIL=List.of(new Payload(EntityTypes.ZOMBIE,0,0,0,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},IsBaby:1b,Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.WITHER_SKELETON,1,0,0,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.ZOMBIFIED_PIGLIN,0,0,1,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.DROWNED,-1,0,0,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},IsBaby:1b,Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.HUSK,0,0,-1,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},IsBaby:1b,Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.SKELETON,1,0,1,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.VINDICATOR,-1,0,-1,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.HUSK,1,0,-1,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")),
            new Payload(EntityTypes.STRAY,-1,0,1,preset("{CustomNameVisible:0b,CustomName:{\"color\":\"yellow\",\"italic\":false,\"text\":\"Lucky Block\"},Silent:1b,DeathLootTable:\"lbc:luckyblock\",equipment:{head:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:\"textures\",value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ==\"}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:\"minecraft:invisibility\",amplifier:1b,duration:2000000,show_particles:0b}]}")));
    private static final List<String> FEATURES=List.of("acacia","birch","oak","dark_oak","jungle_tree",
            "rooted_azalea_tree","spruce","azalea_tree","mangrove","fancy_oak","tall_mangrove","birch_tall");
    private final GameController game;
    private final Set<Entity> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();
    private int lastTick=Integer.MIN_VALUE;

    public LbcChaosTrapActors(GameController game){this.game=game;}
    private static CompoundTag preset(String snbt){
        try{return SourceKnockbackBalance.entity(TagParser.parseCompoundFully(snbt));}
        catch(CommandSyntaxException error){throw new ExceptionInInitializerError(error);}
    }
    public static boolean matches(ItemStack stack){
        if(stack.isEmpty())return false;
        var data=LbcFartus.data(stack);
        if(data.getIntOr("drop",0)!=1)return false;
        for(var flag:List.of("bob","aimbob","evilluckyblock","forest"))if(data.getIntOr(flag,0)==1)return true;
        return false;
    }
    public boolean ready(ItemEntity token){
        if(token.isRemoved()||token.getAge()>=5999||!matches(token.getItem())||!game.playing()
                ||game.stage()==GameController.Stage.ENDING||token.level()!=game.maps().level()
                ||!(token.level() instanceof ServerLevel level)||game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.")&&!game.items().currentEpoch(tag))return false;
        return resident(level,token.blockPosition(),LbcFartus.data(token.getItem()).getIntOr("evilluckyblock",0)==1?1:0);
    }
    /** Source independent matching branches, source order; never consumes or replaces the dropped stack. */
    public boolean spawn(ItemEntity token){
        if(!ready(token))return false;
        for(var flag:List.of("forest","evilluckyblock","aimbob","bob"))spawn(token,flag);
        return true;
    }
    /** Shared dispatcher can interleave these original flags with its other native source branches. */
    public boolean spawn(ItemEntity token,String flag){
        if(!ready(token))return false;
        var level=(ServerLevel)token.level();var data=LbcFartus.data(token.getItem());
        var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        if(data.getIntOr(flag,0)!=1)return false;
        if(flag.equals("forest")){
            var marker=EntityTypes.MARKER.create(level,EntitySpawnReason.COMMAND);
            if(marker!=null){marker.entityTags().addAll(List.of("forest","rc_5t","magic",AGE+"0"));admit(level,marker,token.position(),author);}
        }
        if(flag.equals("evilluckyblock"))for(var payload:EVIL)
            spawn(level,payload.type(),payload.data(),token.position().add(payload.x(),payload.y(),payload.z()),author);
        if(flag.equals("aimbob"))spawn(level,EntityTypes.SKELETON,AIMBOB,token.position(),author);
        if(flag.equals("bob"))spawn(level,EntityTypes.ZOMBIE,BOB,token.position(),author);
        return true;
    }
    private void spawn(ServerLevel level,EntityType<?> type,CompoundTag data,Vec3 at,ServerPlayer author){
        if(!net.minecraft.world.level.Level.isInSpawnableBounds(BlockPos.containing(at)))return;
        var actor=type.create(level,EntitySpawnReason.COMMAND);
        if(actor==null)return;
        actor.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data.copy()));
        admit(level,actor,at,author);
    }
    private void admit(ServerLevel level,Entity actor,Vec3 at,ServerPlayer author){
        actor.snapTo(at.x,at.y,at.z,actor.getYRot(),actor.getXRot());prepare(actor,author);
        if(!level.addFreshEntity(actor)){tracked.remove(actor);actor.discard();}
    }
    private void prepare(Entity actor,ServerPlayer author){
        actor.entityTags().add(ACTOR);
        if(author!=null){authors.put(author.getUUID(),author);actor.entityTags().removeIf(t->t.startsWith(OWNER));actor.entityTags().add(OWNER+author.getUUID());}
        game.items().markCurrentItemEpoch(actor);tracked.add(actor);game.items().timeStop().prepareFrozen(actor);
    }
    public static boolean managed(Entity actor){return actor!=null&&actor.entityTags().contains(ACTOR);}
    public LivingEntity owner(Entity actor){
        if(actor instanceof Projectile projectile&&managed(projectile.getOwner()))actor=projectile.getOwner();
        if(actor instanceof ItemEntity token&&matches(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER))try{
            var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);
            return online==null?authors.get(id):online;
        }catch(IllegalArgumentException ignored){}
        return null;
    }
    private boolean active(Entity actor){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&actor.level()==game.maps().level()&&game.items().currentEpoch(actor);}
    public boolean canAffect(Entity actor,Entity target){
        if(actor instanceof Projectile projectile&&managed(projectile.getOwner()))actor=projectile.getOwner();
        if(!managed(actor))return true;
        if(target==null||!active(actor)||!game.items().wands().validTarget(target))return false;
        return !(owner(actor) instanceof ServerPlayer author)||target==author||game.items().wands().target(author,target);
    }
    public boolean canTarget(Entity actor,LivingEntity target){return target==null||canAffect(actor,target);}
    public void inheritConversion(Entity parent,Entity child){
        if(managed(parent)&&active(parent))prepare(child,LbcIndirectKills.playerSource(owner(parent)));
    }
    /** Keep the native skeleton as projectile owner; carry the causal wearer separately across unload. */
    public void inheritProjectile(Entity parent,Entity child){
        if(managed(parent)&&active(parent))prepare(child,LbcIndirectKills.playerSource(owner(parent)));
    }
    /** Called while a native death-loot child is still outside the world; preserve its original components. */
    public void inheritDrop(Entity parent,ItemEntity child){
        if(!managed(parent)||!active(parent))return;
        var author=owner(parent);if(author!=null)child.setThrower(author);
        game.items().markCurrentItemEpoch(child);game.items().timeStop().prepareFrozen(child);
    }
    public void loaded(Entity actor){
        if(!managed(actor)&&actor instanceof Projectile projectile&&managed(projectile.getOwner()))
            inheritProjectile(projectile.getOwner(),actor);
        if(!managed(actor))return;
        if(!active(actor)){actor.discard();return;}
        tracked.add(actor);game.items().timeStop().prepareFrozen(actor);
    }
    public void unloaded(Entity actor){tracked.remove(actor);}
    public void tick(){
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        for(var actor:List.copyOf(tracked)){
            if(actor.isRemoved()){tracked.remove(actor);continue;}
            if(!active(actor)){tracked.remove(actor);actor.discard();continue;}
            if(!(actor instanceof Marker marker)||!actor.entityTags().contains("forest"))continue;
            var level=(ServerLevel)actor.level();
            if(game.items().timeStop().stopped(level)||!resident(level,actor.blockPosition(),32))continue;
            // The marker is magical; physical native mobs already released by it keep their own rules.
            if(game.items().magic().suppressedAt(level,marker.position())){marker.discard();tracked.remove(marker);continue;}
            int age=age(marker)+1;marker.entityTags().removeIf(t->t.startsWith(AGE));marker.entityTags().add(AGE+age);
            if(age>=800){marker.discard();tracked.remove(marker);continue;}
            if(now%5==0)forest(marker);
        }
    }
    private static int age(Entity marker){
        for(var tag:marker.entityTags())if(tag.startsWith(AGE))try{return Integer.parseInt(tag.substring(AGE.length()));}catch(NumberFormatException ignored){}
        return 0;
    }
    private void forest(Marker marker){
        var level=(ServerLevel)marker.level();var author=LbcIndirectKills.playerSource(owner(marker));
        int branch=marker.getRandom().nextInt(12);var at=marker.position();
        Runnable growth=()->execute(marker,"place feature minecraft:"+FEATURES.get(branch));
        if(author!=null)game.items().indirectKills().trapChanges(level,author,true,growth);else growth.run();
        if(branch==6)spawn(level,EntityTypes.ZOMBIE,SHISH,at,author);
        else spawn(level,EntityTypes.ZOMBIE,preset("{IsBaby:1b}"),at.add(1,0,1),author);
        String nether=level.dimension()==net.minecraft.world.level.Level.NETHER?" under 120":"";
        // Preserve source spreadplayers' native height/safety/random selection on this marker only.
        execute(marker,"spreadplayers "+at.x+" "+at.z+" 0 10"+nether+" false @s");
    }
    private int execute(Entity actor,String command){
        var source=game.server().createCommandSourceStack().withSource(CommandSource.NULL).withLevel((ServerLevel)actor.level())
                .withEntity(actor).withPosition(actor.position()).withRotation(actor.getRotationVector())
                .withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
        try{return game.server().getCommands().getDispatcher().execute(command,source);}
        catch(CommandSyntaxException expectedFailure){return 0;}
    }
    private static boolean resident(ServerLevel level,BlockPos pos,int radius){
        for(int x=(pos.getX()-radius)>>4;x<=(pos.getX()+radius)>>4;x++)
            for(int z=(pos.getZ()-radius)>>4;z<=(pos.getZ()+radius)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    public List<Entity> snapshot(){return List.copyOf(tracked);}
    public int trackedCount(){return tracked.size();}
    public void stop(){var old=snapshot();tracked.clear();for(var actor:old)if(!actor.isRemoved())actor.discard();authors.clear();lastTick=Integer.MIN_VALUE;}
}
