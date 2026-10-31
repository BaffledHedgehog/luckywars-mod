package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Finite native actors/BEs and real shared projectile contact, with exact fixture restoration. */
public final class GrimoireRoomChecks {
    private GrimoireRoomChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Grimoire rooms: "+why);}
    private static CompoundTag saved(Entity e){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,e.registryAccess());e.saveWithoutId(out);return out.buildResult();}
    private static Set<Entity> entities(ServerLevel level){var result=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(result::add);return result;}
    private static List<Entity> fresh(ServerLevel level,Set<Entity> previous){return entities(level).stream().filter(e->!previous.contains(e)&&!e.isRemoved()).toList();}
    private static Marker marker(GameController game,LbcGrimoireRooms rooms,Vec3 at,String kind){
        var marker=EntityTypes.MARKER.create(game.maps().level(),EntitySpawnReason.COMMAND);require(marker!=null,"native marker created");
        marker.setPos(at);marker.entityTags().addAll(List.of("slowraycast","grimoire_dungeon",kind));
        game.items().markCurrentItemEpoch(marker);require(game.maps().level().addFreshEntity(marker),"native marker admitted");
        var author=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        game.items().spellSources().bind(marker,new LbcSpellSource.Source(marker.getUUID(),author.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(author),Set.of("dungeon_mob"),true));
        rooms.acceptedMarker(marker);return marker;
    }
    public static void run(GameController game){
        require(game.items().chargedSwords().count()==0,"exclusive native sword fixture");
        try(var base=new ElementalGrimoireChecks.Fixture(game)){
            var level=base.owner.level();var rng=MatchRandomState.capture(level.getRandom());
            var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            var initial=entities(level);var boss=new ArrayList<Vec3>();var shots=new ArrayList<Boolean>();
            var rooms=new LbcGrimoireRooms(game,(marker,world,at)->boss.add(at),(source,at,yaw,pitch,bone)->{
                shots.add(bone);game.items().chargedSwords().spawnHostile(source,at,yaw,pitch,bone);
            });
            try{
                base.initialize();base.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);base.owner.setHealth(1000);var at=base.owner.position().add(0,0,3);var pos=BlockPos.containing(at);
                for(var p:BlockPos.betweenClosed(pos.offset(-5,-1,-5),pos.offset(5,4,5)))blocks.put(p.immutable(),game.maps().snapshot(level,p));
                for(int choice=1;choice<=12;choice++){
                    for(var p:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,3,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
                    var seed=marker(game,rooms,at,"room_reward");var before=entities(level);rooms.reward(seed,at,choice);var spawned=fresh(level,before);
                    switch(choice){
                        case 1->require(level.getBlockState(pos.below()).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.RED))&&spawned.stream().anyMatch(e->e instanceof Marker&&e.entityTags().contains("miniboss")),"red glass/native proximity marker");
                        case 2,3->{var be=level.getBlockEntity(pos);require(be!=null,"actual loot container");var data=be.saveWithFullMetadata(level.registryAccess());
                            require(data.getStringOr("LootTable","").equals(choice==2?"lbc:random_glack_1":"lbc:random_grimoires"),"native deferred original chest loot");
                            require(level.getBlockState(pos.below()).is(choice==2?Blocks.GOLD_BLOCK:Blocks.DIAMOND_BLOCK),"original reward pedestal");}
                        case 4,12->{var data=level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
                            require(level.getBlockState(pos).is(Blocks.SPAWNER)&&data.getIntOr("SpawnCount",-1)==(choice==4?1:5)
                                    &&data.getIntOr("RequiredPlayerRange",-1)==(choice==4?5:3),"native spawner count/range");
                            var potential=data.getListOrEmpty("SpawnPotentials").getCompoundOrEmpty(0).getCompoundOrEmpty("data");
                            require(potential.getCompoundOrEmpty("entity").getStringOr("id","").equals(choice==4?"minecraft:silverfish":"minecraft:firework_rocket"),"real source native spawner actor");
                            require(LbcPotionSpawnerDrops.managed((net.minecraft.world.level.block.entity.SpawnerBlockEntity)level.getBlockEntity(pos)),"native room producer adopted by common spawner owner");
                            var child=EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),potential.getCompoundOrEmpty("entity")),level,EntitySpawnReason.SPAWNER,e->{e.setPos(at.add(0,1,0));return e;});
                            require(child!=null&&level.addFreshEntity(child)&&LbcPotionSpawnerDrops.managed(child),"actual native mob/firework factory and generic producer admission");
                            var inherited=game.items().spellSources().source(child);var original=game.items().spellSources().source(seed);
                            require(inherited!=null&&inherited.author().equals(original.author())&&inherited.cause().order()==original.cause().order()&&inherited.excluded().contains("dungeon_mob"),"native producer retains original source cause and exclusions");
                            game.items().entityUnloaded(child);child.discard();}
                        case 5,6->require(spawned.size()==3&&spawned.stream().allMatch(e->e instanceof Mob&&e.entityTags().contains("dungeon_mob")),"three persistent native hostile bodies");
                        case 7,9->require(spawned.size()==2&&spawned.stream().allMatch(e->e instanceof Mob),"two native evokers/vindicators or breezes");
                        case 8->require(spawned.size()==1&&saved(spawned.getFirst()).getBooleanOr("powered",false),"ordinary charged hostile creeper");
                        case 10->{require(spawned.size()==3,"three original Lucky Block mobs");for(var entity:spawned){
                            var data=saved(entity);require(data.getStringOr("DeathLootTable","").equals("lbc:luckyblock"),"original native Lucky Block loot");
                            require(entity instanceof LivingEntity living&&living.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD),"exact source head carrier");}}
                        case 11->{require(level.getBlockState(pos).is(Blocks.RED_MUSHROOM_BLOCK)&&level.getBlockState(pos.above()).is(Blocks.BROWN_MUSHROOM_BLOCK)
                                    &&level.getBlockState(pos.above(2)).is(Blocks.MUSHROOM_STEM),"three native Lucky Block blocks");
                            require(!level.getBlockState(pos).getValue(HugeMushroomBlock.UP)&&!level.getBlockState(pos).getValue(HugeMushroomBlock.DOWN),"original all-false mushroom faces");}
                    }
                    for(var entity:spawned){rooms.unloaded(entity);game.items().entityUnloaded(entity);entity.discard();}rooms.unloaded(seed);seed.discard();
                }
                level.setBlock(pos,Blocks.STONE.defaultBlockState(),818);var blocked=marker(game,rooms,at,"room_reward");rooms.tick();require(!blocked.isRemoved(),"non-air non-barrier room waits");
                level.setBlock(pos,Blocks.BARRIER.defaultBlockState(),818);rooms.tick();require(blocked.isRemoved()&&boss.size()==1&&boss.getFirst().equals(at.add(0,-.5,0)),"barrier callback exact source half-block position/one-shot");rooms.tick();require(boss.size()==1,"consumed boss marker cannot duplicate");
                var tinySeed=marker(game,rooms,at,"miniboss");var tiny=rooms.spawnMiniboss(tinySeed,at,false);rooms.unloaded(tinySeed);tinySeed.discard();
                require(tiny!=null&&tiny.getMaxHealth()==45&&tiny.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)==17
                        &&IdentifierString(tiny.getMainHandItem().get(DataComponents.ITEM_MODEL)).equals("lbc:tiny_bone"),"native tiny source health/base damage/model");
                rooms.damaged(tiny);require(shots.size()==11&&shots.stream().allMatch(Boolean::booleanValue),"eleven source bone native projectiles per hit");
                retireSwords(game,level,initial);
                var target=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);var ally=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);
                require(target!=null&&ally!=null,"actual native contact bodies");
                target.setPos(at.add(2,1,0));ally.setPos(at.add(.7,1,0));ally.entityTags().add("dungeon_mob");
                for(var pig:List.of(target,ally)){pig.setNoAi(true);pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);pig.setHealth(100);game.items().markCurrentItemEpoch(pig);require(level.addFreshEntity(pig),"contact body admitted");}
                var source=new LbcSpellSource.Source(tiny.getUUID(),base.owner.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(base.owner),Set.of("dungeon_mob"),true);
                game.items().spellSources().bind(tiny,source);
                game.items().chargedSwords().spawnHostile(tiny,at.add(0,1.4,0),-90,0,true);
                var copy=game.items().chargedSwords().snapshot().getFirst().entity();
                require(game.items().spellSources().source(copy).cause().equals(source.cause()),"native projectile inherits immutable original source cause");
                game.items().chargedSwords().tick();game.items().chargedSwords().tick();
                require(target.getHealth()==96&&ally.getHealth()==100&&copy.isRemoved(),"same precise native contact gives source4 damage once, skipping dungeon ally");
                target.discard();ally.discard();tiny.discard();rooms.unloaded(tiny);
                rooms.stop();var goldenSeed=marker(game,rooms,at,"miniboss");var golden=rooms.spawnMiniboss(goldenSeed,at,true);rooms.unloaded(goldenSeed);goldenSeed.discard();
                require(golden!=null&&golden.getMaxHealth()==30&&golden.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)==10
                        &&golden.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET),"golden source native attributes/equipment");
                // Repeated finite ticks cover shared10s,60-tick warmup and chance-gated120-tick retirement.
                level.getRandom().setSeed(0x6A71B00BL);for(int tick=0;tick<200;tick++)rooms.tick();
                require(entities(level).stream().anyMatch(e->!e.isRemoved()&&e.entityTags().contains("triplesword_portal")),"native golden portal after shared10s cadence");
                for(int tick=0;tick<59;tick++)rooms.tick();int warm=shots.size();
                require(warm==11,"portal has no shot during first59 ticks");
                golden.discard();rooms.unloaded(golden);
                for(int tick=0;tick<240;tick++){rooms.tick();game.items().chargedSwords().tick();}
                require(shots.size()>warm&&shots.subList(warm,shots.size()).stream().noneMatch(Boolean::booleanValue),"portal emits shared native golden swords after source body removal");
                require(entities(level).stream().noneMatch(e->!e.isRemoved()&&e.entityTags().contains("triplesword_portal")),"finite chance-gated portal retired");
                rooms.stop();require(rooms.actors()==0,"native stop retires room-owned actors");
            }finally{
                rooms.stop();retireSwords(game,level,initial);
                for(var entity:fresh(level,initial)){game.items().entityUnloaded(entity);entity.discard();}
                for(var entry:blocks.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
                rng.restore(level.getRandom());
            }
        }
        SkyWars2.LOGGER.info("[check] PASS grimoire-rooms (all12 native rewards/loot BEs/spawners; barrier one-shot; exact tiny/golden bodies;11 bones; shared golden portal warmup/survival/finite retirement; native stop; scoped restoration)");
    }
    private static void retireSwords(GameController game,ServerLevel level,Set<Entity> initial){for(var entity:fresh(level,initial))if(entity instanceof LbcChargedSwords.ThrownSword)game.items().chargedSwords().unloaded(entity);}
    private static String IdentifierString(net.minecraft.resources.Identifier id){return id==null?"":id.toString();}
}
