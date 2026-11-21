package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.LbcSpellSource;
import me.skywars2.game.entities.mobs.LbcGrimoireDungeon;
import me.skywars2.game.entities.mobs.LbcGrimoireRooms;
import me.skywars2.game.items.drops.LbcPotionSpawnerDrops;
import me.skywars2.game.items.loot.LbcLuckyBlocks;
import me.skywars2.game.items.weapons.melee.LbcChargedSwords;
import me.skywars2.game.match.checkpoints.MatchRandomState;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import me.skywars2.game.entities.mobs.LbcGrimoireBoss;

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
                legacyEggs(game, base.owner, pos);
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
                        case 11->{require(level.getBlockState(pos).is(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL))&&level.getBlockState(pos.above()).is(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NEKO))
                                    &&level.getBlockState(pos.above(2)).is(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI)),"three native Lucky Block blocks");}
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
        SkyWars2.LOGGER.info("[check] PASS grimoire-rooms (actual imba eggs activate Golden, Tiny and the Monument; ownerless Monument; all12 native rewards/loot BEs/spawners; barrier one-shot; exact tiny/golden bodies;11 bones; shared golden portal warmup/survival/finite retirement; native stop; scoped restoration)");
    }
    private static void legacyEggs(GameController game, ServerPlayer owner, BlockPos center) {
        var level = owner.level();
        var rooms = game.items().grimoireRooms();
        var boss = game.items().grimoireBoss();
        require(rooms.actors() == 0 && boss.count() == 0, "exclusive native dungeon egg actors");
        var before = entities(level);
        var catUsed = Stats.ITEM_USED.get(Items.CAT_SPAWN_EGG);
        var skeletonUsed = Stats.ITEM_USED.get(Items.SKELETON_SPAWN_EGG);
        int cats = owner.getStats().getValue(catUsed), skeletons = owner.getStats().getValue(skeletonUsed);
        try {
            var monumentEgg = sourceEgg(game, owner, "cat_spawn_egg__f4176ec0dbb6");
            var monument = useEgg(owner, center.below(), monumentEgg, EntityTypes.MARKER);
            var golden = (Skeleton) useEgg(owner, center.offset(-2, -1, 0),
                    sourceEgg(game, owner, "skeleton_spawn_egg__639a4e70973f"), EntityTypes.SKELETON);
            var tiny = (Skeleton) useEgg(owner, center.offset(2, -1, 0),
                    sourceEgg(game, owner, "skeleton_spawn_egg__d294e41bee26"), EntityTypes.SKELETON);
            require(game.items().currentEpoch(monument) && game.items().currentEpoch(golden) && game.items().currentEpoch(tiny),
                    "native ENTITY_LOAD admits all three actual imba eggs into the encounter");
            require(golden.getHealth() == 30 && golden.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET)
                    && saved(golden).getStringOr("DeathLootTable", "").equals("lbc:golden_skeleton")
                    && tiny.getHealth() == 45 && IdentifierString(tiny.getMainHandItem().get(DataComponents.ITEM_MODEL)).equals("lbc:tiny_bone")
                    && saved(tiny).getStringOr("DeathLootTable", "").equals("lbc:tiny_skeleton"),
                    "native loot, health and equipment from the original eggs are preserved");
            var ordinary = useEgg(owner, center.offset(0, -1, 2), new ItemStack(Items.SKELETON_SPAWN_EGG), EntityTypes.SKELETON);
            var similar = Objects.requireNonNull(EntityTypes.SKELETON.create(level, EntitySpawnReason.COMMAND));
            similar.entityTags().addAll(Set.of("lucky_mob", "dungeon_mob", "tiny_skeleton"));
            similar.setPos(Vec3.atBottomCenterOf(center.offset(1, 0, 2)));
            require(level.addFreshEntity(similar) && !game.items().currentEpoch(ordinary) && !game.items().currentEpoch(similar),
                    "plain skeleton and a tag-only mimic retain ordinary vanilla lifecycle");

            var ownerless = nativeMonument(level, monumentEgg, Vec3.atBottomCenterOf(center.north(2)), false);
            var pending = nativeMonument(level, monumentEgg, Vec3.atBottomCenterOf(center.north(3)), true);
            require(game.items().currentEpoch(ownerless) && !game.items().currentEpoch(pending),
                    "raw ownerless native payload activates while an in-progress jigsaw publication waits");
            var source = game.items().spellSources().source(monument);
            require(source != null && owner.getUUID().equals(source.author()) && source.cause() != null,
                    "direct Monument carries the actual native egg user before the room schedules");
            rooms.tick();
            require(monument.isRemoved() && ownerless.isRemoved() && !pending.isRemoved()
                    && level.getBlockState(monument.blockPosition().east()).is(Blocks.BARRIER)
                    && level.getBlockState(ownerless.blockPosition().east()).is(Blocks.BARRIER),
                    "both actual Monument actors place the source barrier and consume only themselves");
            var wardens = boss.snapshot().stream().filter(Warden.class::isInstance).toList();
            require(wardens.size() == 2, "actual boss service publishes one Warden for each Monument");
            Entity unownedBoss = null;
            for (var warden : wardens) {
                var authority = game.items().spellSources().source(warden);
                require(authority != null && authority.persistent(), "native boss retains its persistent attack authority");
                if (warden.position().equals(monument.position())) {
                    require(owner.getUUID().equals(authority.author()) && authority.cause() != null,
                            "direct Monument boss keeps the accepted player's cause");
                } else {
                    require(warden.position().equals(ownerless.position()) && authority.author() == null
                            && authority.cause() == null && authority.unownedMobs(),
                            "ownerless Monument boss has native attacks without an invented player cause");
                    unownedBoss = warden;
                }
            }
            require(unownedBoss != null, "native ownerless boss was published");
            var image = saved(unownedBoss);
            image.putString("id", "minecraft:warden");
            UUID bossId = unownedBoss.getUUID();
            unownedBoss.discard();
            boss.unloaded(unownedBoss);
            var restored = EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,
                    level.registryAccess(), image), level, EntitySpawnReason.LOAD, entity -> entity);
            require(restored instanceof Warden && restored.getUUID().equals(bossId) && level.addFreshEntity(restored),
                    "native save/load reintroduces the ownerless boss with its original UUID");
            var restoredAuthority = game.items().spellSources().source(restored);
            require(restoredAuthority != null && restoredAuthority.persistent() && restoredAuthority.unownedMobs()
                    && restoredAuthority.author() == null && restoredAuthority.cause() == null
                    && game.items().spellSources().target(restoredAuthority, ordinary)
                    && !game.items().spellSources().target(restoredAuthority, similar),
                    "restored source attacks eligible bodies and excludes dungeon allies without player credit");
            require(tiny.hurtServer(level, tiny.damageSources().playerAttack(owner), 1000) && tiny.isDeadOrDying(),
                    "real fatal native hit against the raw Tiny Skeleton");
            require(fresh(level, before).stream().filter(e -> e instanceof LbcChargedSwords.ThrownSword shot
                    && shot.getOwner() == tiny && shot.entityTags().contains("bone")).count() == 11,
                    "raw Tiny Skeleton runs the actual eleven-bone damage reaction once");
            for (int tick = 1; tick < 199; tick++) rooms.tick();
            require(fresh(level, before).stream().noneMatch(e -> e.entityTags().contains("triplesword_portal")),
                    "raw Golden Skeleton does not fire before its original cadence");
            rooms.tick();
            require(fresh(level, before).stream().filter(e -> e.entityTags().contains("triplesword_portal")).count() == 1,
                    "raw Golden Skeleton publishes the real portal exactly on the 200th room tick");
        } finally {
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            owner.getStats().setValue(owner, catUsed, cats);
            owner.getStats().setValue(owner, skeletonUsed, skeletons);
            boss.stop();
            rooms.stop();
            retireSwords(game, level, before);
            for (var entity : fresh(level, before)) {
                game.items().entityUnloaded(entity);
                entity.discard();
            }
        }
    }
    private static ItemStack sourceEgg(GameController game, ServerPlayer owner, String leaf) {
        var loot = game.lbc().rollTable(owner.level(), owner.position(), owner, "lbc:item_defs/minecraft/" + leaf);
        require(loot.size() == 1 && loot.getFirst().has(DataComponents.ENTITY_DATA), "actual source loot egg " + leaf);
        return loot.getFirst();
    }
    private static Entity useEgg(ServerPlayer owner, BlockPos pad, ItemStack egg, EntityType<?> type) {
        var level = owner.level();
        level.setBlock(pad, Blocks.STONE.defaultBlockState(), 818);
        var click = new BlockHitResult(Vec3.atCenterOf(pad).add(0, .5, 0), Direction.UP, pad, false);
        var before = entities(level);
        var stack = egg.copyWithCount(2);
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        require(owner.gameMode.useItemOn(owner, level, stack, InteractionHand.MAIN_HAND, click).consumesAction()
                && stack.getCount() == 1, "native spawn egg consumes one unit through actual UseOn");
        var spawned = fresh(level, before).stream().filter(entity -> entity.getType() == type).toList();
        require(spawned.size() == 1, "one native body published for " + type);
        return spawned.getFirst();
    }
    private static Entity nativeMonument(ServerLevel level, ItemStack egg, Vec3 at, boolean pending) {
        var payload = egg.get(DataComponents.ENTITY_DATA).copyTagWithoutId();
        payload.putString("id", "minecraft:marker");
        var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), payload);
        var entity = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.SPAWNER, actor -> {
            actor.setPos(at);
            if (pending) actor.entityTags().add(LbcGrimoireDungeon.PENDING);
            return actor;
        });
        require(entity != null && level.addFreshEntity(entity), "publish the original native marker payload without a player");
        return entity;
    }
    private static void retireSwords(GameController game,ServerLevel level,Set<Entity> initial){for(var entity:fresh(level,initial))if(entity instanceof LbcChargedSwords.ThrownSword)game.items().chargedSwords().unloaded(entity);}
    private static String IdentifierString(net.minecraft.resources.Identifier id){return id==null?"":id.toString();}
}
