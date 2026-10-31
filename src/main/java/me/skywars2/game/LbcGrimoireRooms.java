package me.skywars2.game;

import java.util.*;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Native one-shot dungeon room markers and the source golden/tiny miniboss encounters. */
public final class LbcGrimoireRooms {
    public static void register(){}
    @FunctionalInterface public interface BossSpawn { void spawn(Entity marker,ServerLevel level,Vec3 origin); }
    @FunctionalInterface public interface SwordSpawn { void spawn(Entity source,Vec3 origin,float yaw,float pitch,boolean bone); }
    private static final String MANAGED="skywars2.grimoire_room";
    private static final String AGE="skywars2.grimoire_age.";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final Set<String> ROOM_TAGS=Set.of("miniboss","room_reward","room_reward_4","room_reward_4_1","spawn_monument");
    private final GameController game;
    private final BossSpawn boss;
    private final SwordSpawn swords;
    private final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcGrimoireRooms(GameController game,BossSpawn boss,SwordSpawn swords){this.game=game;this.boss=boss;this.swords=swords;}
    private static CompoundTag parse(String value){try{return TagParser.parseCompoundFully(value);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new ExceptionInInitializerError(e);}}
    private static final CompoundTag GOLDEN=parse("""
{id:"minecraft:skeleton",Tags:["lucky_mob","golden_skeleton","dungeon_mob"],CustomName:{"color":"gold","italic":false,"translate":"golden_skeleton"},DeathLootTable:"lbc:golden_skeleton",PersistenceRequired:1b,Health:30.0f,equipment:{mainhand:{id:"minecraft:golden_sword",count:1},feet:{id:"minecraft:golden_boots",count:1},legs:{id:"minecraft:golden_leggings",count:1},chest:{id:"minecraft:golden_chestplate",count:1},head:{id:"minecraft:golden_helmet",count:1}},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:100},{id:"minecraft:knockback_resistance",base:10},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}]}
        """);
    private static final CompoundTag TINY=parse("""
{id:"minecraft:skeleton",Tags:["lucky_mob","tiny_skeleton","dungeon_mob"],CustomName:{"color":"gold","italic":false,"translate":"tiny_skeleton"},DeathLootTable:"lbc:tiny_skeleton",PersistenceRequired:1b,Health:45.0f,equipment:{mainhand:{id:"minecraft:golden_sword",components:{"minecraft:item_model":"lbc:tiny_bone"},count:1}},attributes:[{id:"minecraft:max_health",base:45},{id:"minecraft:follow_range",base:100},{id:"minecraft:attack_damage",base:17},{id:"minecraft:armor",base:1},{id:"minecraft:armor_toughness",base:1}]}
        """);
    private static final CompoundTag LUCKY_0=parse("""
{id:"minecraft:drowned",Tags:["dungeon_mob"],PersistenceRequired:1b,CustomNameVisible:0b,CustomName:{"color":"yellow","italic":false,"text":"Lucky Block"},IsBaby:1b,Silent:1b,DeathLootTable:"lbc:luckyblock",equipment:{head:{id:"minecraft:player_head",components:{"minecraft:profile":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:"textures",value:"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ=="}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:"minecraft:invisibility",amplifier:1b,duration:2000000,show_particles:0b}]}
        """);
    private static final CompoundTag LUCKY_1=parse("""
{id:"minecraft:husk",Tags:["dungeon_mob"],PersistenceRequired:1b,CustomNameVisible:0b,CustomName:{"color":"yellow","italic":false,"text":"Lucky Block"},IsBaby:1b,Silent:1b,DeathLootTable:"lbc:luckyblock",equipment:{head:{id:"minecraft:player_head",components:{"minecraft:profile":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:"textures",value:"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ=="}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:"minecraft:invisibility",amplifier:1b,duration:2000000,show_particles:0b}]}
        """);
    private static final CompoundTag LUCKY_2=parse("""
{id:"minecraft:stray",Tags:["dungeon_mob"],PersistenceRequired:1b,CustomNameVisible:0b,CustomName:{"color":"yellow","italic":false,"text":"Lucky Block"},Silent:1b,DeathLootTable:"lbc:luckyblock",equipment:{head:{id:"minecraft:player_head",components:{"minecraft:profile":{id:[I;-940982597,-1981660917,-1797831442,692841550],properties:[{name:"textures",value:"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI5MmNiNDMzMzNhYTYyMWM3MGVlZjRlYmYyOTliYTQxMmI0NDZmZTEyZTM0MWNjYzU4MmYzMTkyMTg5In19fQ=="}]}},count:1}},drop_chances:{head:0.0f},active_effects:[{id:"minecraft:invisibility",amplifier:1b,duration:2000000,show_particles:0b}]}
        """);
    private static final CompoundTag SILVERFISH=parse("""
{SpawnCount:1,SpawnRange:3,Delay:1,MinSpawnDelay:4,MaxSpawnDelay:4,MaxNearbyEntities:100,RequiredPlayerRange:5,SpawnPotentials:[{weight:1,data:{custom_spawn_rules:{sky_light_limit:{min_inclusive:0,max_inclusive:15},block_light_limit:{min_inclusive:0,max_inclusive:15}},entity:{id:"minecraft:silverfish",Health:2.0f,Tags:["dungeon_mob"],attributes:[{id:"minecraft:max_health",base:2},{id:"minecraft:movement_speed",base:0.35}]}}}]}
        """);
    private static final CompoundTag MINE=parse("""
{SpawnCount:5,SpawnRange:5,Delay:0,MinSpawnDelay:200,MaxSpawnDelay:200,MaxNearbyEntities:100,RequiredPlayerRange:3,SpawnPotentials:[{weight:1,data:{custom_spawn_rules:{sky_light_limit:{min_inclusive:0,max_inclusive:15},block_light_limit:{min_inclusive:0,max_inclusive:15}},entity:{id:"minecraft:firework_rocket",Life:0,LifeTime:10,FireworksItem:{id:"minecraft:firework_rocket",components:{"minecraft:fireworks":{explosions:[{shape:"large_ball",colors:[16711680],fade_colors:[16711680],has_trail:1b,has_twinkle:1b}]}},count:1}}}}]}
        """);

    /** Placement calls this after its complete native jigsaw transaction is accepted. */
    public void acceptedMarker(Entity marker){
        if(marker.entityTags().contains("skywars2.grimoire_pending")||!(marker instanceof Marker)||!marker.entityTags().contains("grimoire_dungeon")
                ||ROOM_TAGS.stream().noneMatch(marker.entityTags()::contains))return;
        marker.entityTags().add(MANAGED);game.items().markCurrentItemEpoch(marker);loaded(marker);
    }
    public void loaded(Entity entity){
        if(entity.entityTags().contains("skywars2.grimoire_pending")||!entity.entityTags().contains(MANAGED))return;
        if(!game.items().currentEpoch(entity)||entity.level()!=game.maps().level()){entity.discard();return;}
        actors.add(entity);game.items().timeStop().prepareFrozen(entity);
    }
    public void unloaded(Entity entity){actors.remove(entity);}
    public void stop(){for(var entity:List.copyOf(actors))entity.discard();actors.clear();}
    private boolean live(Entity entity){return !entity.isRemoved()&&game.items().currentEpoch(entity)
            &&entity.level()==game.maps().level()&&LbcDimensionItems.ordinary(entity.level());}
    private boolean near(ServerLevel level,Vec3 origin,double range){
        for(var player:level.players())if(player.isAlive()&&!game.isObserver(player)&&!player.isSpectator()
                &&player.position().distanceToSqr(origin)<=range*range)return true;
        return false;
    }
    private void inherit(Entity parent,Entity child){
        child.entityTags().add(MANAGED);game.items().markCurrentItemEpoch(child);
        var source=game.items().spellSources().source(parent);
        if(source!=null){var excluded=new HashSet<>(source.excluded());excluded.add("dungeon_mob");
            game.items().spellSources().bind(child,new LbcSpellSource.Source(child.getUUID(),source.author(),source.epoch(),source.cause(),excluded,true));}
        actors.add(child);game.items().timeStop().prepareFrozen(child);
    }
    private Entity spawn(Entity parent,Vec3 at,CompoundTag preset){
        var level=(ServerLevel)parent.level();if(!level.hasChunkAt(BlockPos.containing(at)))return null;
        var entity=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),SourceKnockbackBalance.entity(preset.copy())),
                level,EntitySpawnReason.COMMAND,e->{e.setPos(at);inherit(parent,e);return e;});
        if(entity!=null&&!level.tryAddFreshEntityWithPassengers(entity)){
            for(var part:entity.getSelfAndPassengers().toList()){actors.remove(part);part.discard();}return null;
        }return entity;
    }
    private Entity mob(Entity parent,Vec3 at,String type){return spawn(parent,at,parse("{id:'minecraft:"+type+"',Tags:['dungeon_mob'],PersistenceRequired:1b}"));}
    private static int age(Entity marker){for(var tag:marker.entityTags())if(tag.startsWith(AGE))try{return Integer.parseInt(tag.substring(AGE.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void age(Entity marker,int age){marker.entityTags().removeIf(t->t.startsWith(AGE));marker.entityTags().add(AGE+age);}
    private void remove(Entity entity){actors.remove(entity);entity.discard();}
    public void tick(){
        if(!game.playing()||game.stage()==GameController.Stage.ENDING)return;
        var level=game.maps().level();if(game.items().timeStop().stopped(level))return;
        for(var entity:List.copyOf(actors)){
            if(!live(entity)){actors.remove(entity);if(!entity.isRemoved())entity.discard();continue;}
            if(entity.entityTags().contains("skywars2.grimoire_pending")||!level.hasChunkAt(entity.blockPosition()))continue;
            if(entity instanceof LivingEntity living){
                if(living.isAlive()&&entity.entityTags().contains("golden_skeleton")){
                    int phase=age(entity)+1;age(entity,phase>=200?0:phase);if(phase>=200)goldenPortal(living);
                }
                continue;
            }
            if(entity.entityTags().contains("triplesword_portal")){portal(entity);continue;}
            if(entity instanceof Marker&&near(level,entity.position(),15))trigger(entity);
        }
    }
    private void trigger(Entity marker){
        var at=marker.position();var tags=marker.entityTags();var level=(ServerLevel)marker.level();
        if(tags.contains("miniboss")){if(near(level,at,4)){spawnMiniboss(marker,at,level.getRandom().nextBoolean());announce(level,at);remove(marker);}return;}
        if(tags.contains("room_reward")){room(marker,at);return;}
        int radius=tags.contains("room_reward_4")?3:tags.contains("room_reward_4_1")?4:0;
        if(radius!=0){
            // Source continues all four execute-positioned calls even after the first kill @s.
            for(var shift:new int[][]{{radius,radius},{-radius,radius},{radius,-radius},{-radius,-radius}})room(marker,at.add(shift[0],1,shift[1]),false);
            for(var player:level.players())if(player.isAlive()&&!game.isObserver(player)&&!player.isSpectator()&&player.position().distanceToSqr(at)<=225)
                game.items().mana().award(player,"lbc:true_advancements/legends/enter_grimoire_dungeon");
            remove(marker);return;
        }
        if(tags.contains("spawn_monument")){
            block(level,BlockPos.containing(at).east(),Blocks.BARRIER.defaultBlockState(),null);
            boss.spawn(marker,level,at);remove(marker);
        }
    }
    private void room(Entity marker,Vec3 at){room(marker,at,true);}
    private void room(Entity marker,Vec3 at,boolean consume){
        var level=(ServerLevel)marker.level();var pos=BlockPos.containing(at);if(!level.hasChunkAt(pos))return;
        var state=level.getBlockState(pos);
        if(state.is(Blocks.AIR)){reward(marker,at,level.getRandom().nextInt(12)+1);if(consume)remove(marker);}
        // Separate source execute checks: a previous reward may itself have changed this block.
        if(level.getBlockState(pos).is(Blocks.BARRIER)&&near(level,at,8)){boss.spawn(marker,level,at.add(0,-.5,0));if(consume)remove(marker);}
    }
    private static void block(ServerLevel level,BlockPos at,BlockState state,CompoundTag data){
        if(level.hasChunkAt(at)&&!level.isOutsideBuildHeight(at))new BlockInput(state,Set.of(),data==null?null:data.copy()).place(level,at,818);
    }
    private static BlockState lucky(BlockState state){
        return state.setValue(HugeMushroomBlock.DOWN,false).setValue(HugeMushroomBlock.EAST,false)
                .setValue(HugeMushroomBlock.NORTH,false).setValue(HugeMushroomBlock.SOUTH,false)
                .setValue(HugeMushroomBlock.UP,false).setValue(HugeMushroomBlock.WEST,false);
    }
    /** Closed source 1..12 reward table; native BEs own loot generation and spawner clocks. */
    void reward(Entity marker,Vec3 at,int choice){
        var level=(ServerLevel)marker.level();var pos=BlockPos.containing(at);
        switch(choice){
            case 1->{block(level,pos.below(),Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(),null);
                spawn(marker,at,parse("{id:'minecraft:marker',Tags:['slowraycast','grimoire_dungeon','miniboss']}"));}
            case 2->{block(level,pos,Blocks.BARREL.defaultBlockState(),parse("{LootTable:'lbc:random_glack_1'}"));block(level,pos.below(),Blocks.GOLD_BLOCK.defaultBlockState(),null);}
            case 3->{block(level,pos,Blocks.CHEST.defaultBlockState(),parse("{LootTable:'lbc:random_grimoires'}"));block(level,pos.below(),Blocks.DIAMOND_BLOCK.defaultBlockState(),null);}
            case 4->{block(level,pos,Blocks.SPAWNER.defaultBlockState(),SILVERFISH);game.items().potionSpawnerDrops().admitSourceSpawner(level,pos,game.items().spellSources().source(marker));}
            case 5->{mob(marker,at,"skeleton");mob(marker,at,"stray");mob(marker,at,"wither_skeleton");}
            case 6->{mob(marker,at,"zombie");mob(marker,at,"husk");mob(marker,at,"drowned");}
            case 7->{mob(marker,at,"evoker");mob(marker,at,"vindicator");}
            case 8->spawn(marker,at,parse("{id:'minecraft:creeper',powered:1b,Tags:['dungeon_mob'],PersistenceRequired:1b}"));
            case 9->{mob(marker,at,"breeze");mob(marker,at,"breeze");}
            case 10->{spawn(marker,at.add(-1,0,0),LUCKY_0);spawn(marker,at.add(0,0,-1),LUCKY_1);spawn(marker,at.add(1,0,-1),LUCKY_2);}
            case 11->{block(level,pos,lucky(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()),null);block(level,pos.above(),lucky(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState()),null);block(level,pos.above(2),lucky(Blocks.MUSHROOM_STEM.defaultBlockState()),null);}
            case 12->{block(level,pos,Blocks.SPAWNER.defaultBlockState(),MINE);game.items().potionSpawnerDrops().admitSourceSpawner(level,pos,game.items().spellSources().source(marker));}
            default->throw new IllegalArgumentException("Dungeon reward "+choice);
        }
    }
    LivingEntity spawnMiniboss(Entity marker,Vec3 at,boolean golden){return (LivingEntity)spawn(marker,at,golden?GOLDEN:TINY);}
    private static void announce(ServerLevel level,Vec3 at){
        level.sendParticles(new ShriekParticleOption(5),at.x,at.y+1,at.z,20,1,1,1,0);
        LbcOverload.sound(level,at,"minecraft:block.sculk_shrieker.shriek",2,1,0,0);
    }
    private void goldenPortal(LivingEntity skeleton){
        var portal=spawn(skeleton,skeleton.getEyePosition(),parse("{id:'minecraft:marker',Tags:['slowraycast','player','triplesword_portal','magic']}"));
        if(portal!=null){age(portal,0);}
        LbcOverload.sound((ServerLevel)skeleton.level(),skeleton.position(),"minecraft:block.sculk_shrieker.shriek",2,2,0,0);
    }
    private void portal(Entity portal){
        var level=(ServerLevel)portal.level();var at=portal.position();int age=age(portal)+1;age(portal,age);
        if(game.items().magic().suppressedAt(level,at)){remove(portal);return;}
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.GOLD_BLOCK),at.x,at.y,at.z,10,.1,.5,.1,0);
        if(age<60||level.getRandom().nextInt(100)>=30)return;
        {
            var target=level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(30),e->e.getType().builtInRegistryHolder().is(MOBS)
                    &&e.position().distanceToSqr(at)<=900&&!e.entityTags().contains("dungeon_mob")&&!e.entityTags().contains("strict_map_object")
                    &&game.items().wands().validRoleTarget(e,false)).stream().min(Comparator.comparingDouble(e->e.position().distanceToSqr(at))).orElse(null);
            Vec3 direction=target==null?new Vec3(0,0,1):target.getEyePosition().subtract(at);
            float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
            swords.spawn(portal,at,yaw+level.getRandom().nextInt(6001)*.01f-30,pitch+level.getRandom().nextInt(4001)*.01f-20,false);
        }
        // The original kill at120 is inside portal_tick_attack, hence chance-gated.
        if(age>=120)remove(portal);
    }
    /** Shared accepted-damage reaction calls this exactly once, including a fatal native hit. */
    public void damaged(LivingEntity victim){
        if(!actors.contains(victim)||!live(victim)||!victim.entityTags().contains("tiny_skeleton")
                ||game.items().timeStop().stopped((ServerLevel)victim.level()))return;
        var at=victim.position().add(0,1.4,0);var random=victim.getRandom();
        for(int i=0;i<11;i++)swords.spawn(victim,at,random.nextInt(360001)*.001f-180,random.nextInt(70001)*.001f-60,true);
        ((ServerLevel)victim.level()).sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.BONE),at.x,at.y-.4,at.z,10,.5,.5,.5,0);
    }
    int actors(){return actors.size();}
}
