package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real original loot, Fabric use callbacks and native scheduled clocks; isolated active legacy match only. */
public final class BalanceRuntimeChecks {
    private BalanceRuntimeChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Balance runtime: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcBalance service;final ServerPlayer owner;final GameChecks.WandThirdPlayerState saved;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final Set<Long> forced=new LinkedHashSet<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Vec3 origin=new Vec3(-240.5,240,-240.5);final int skill,trinket;final int[] trinkets;
        final Map<UUID,int[]> cooldowns=new LinkedHashMap<>();final Map<UUID,Integer> phases=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<UUID,Integer> tntFuses=new LinkedHashMap<>();final Map<UUID,Vec3> tntPositions=new LinkedHashMap<>();
        ItemEntity luckyToken;BlockPos luckyPos;boolean sawSpawner;
        boolean sawRainProducer;
        UUID eggActor;Vec3 nukeAt;long nukeBlasts;
        final int start;int step,at,cooldown,mana;boolean closed;
        @SuppressWarnings("unchecked") Fixture(GameController game,LbcBalance service){
            this.game=game;this.service=service;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            require(game.playing()&&game.balanceLegacy()&&game.canUseGameplayItems(owner),"active original Balance overlay and native participant");
            require(!game.items().timeStop().stopped(owner.level()),"no borrowed TimeStop");saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            var profile=game.players().get(owner.getUUID());skill=profile.lbcSkill;trinket=profile.lbcTrinket;trinkets=profile.trinkets.clone();
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}for(var item:List.of(Items.BOW,Items.SNOWBALL,Items.PANDA_SPAWN_EGG,Items.PIG_SPAWN_EGG)){var stat=net.minecraft.stats.Stats.ITEM_USED.get(item);stats.put(stat,owner.getStats().getValue(stat));}
            var values=(Map<UUID,int[]>)read(service,"cooldowns");values.forEach((id,array)->cooldowns.put(id,array.clone()));phases.putAll((Map<UUID,Integer>)read(service,"clocks"));
            owner.level().getAllEntities().forEach(before::add);
            for(int x=-17;x<=-14;x++)for(int z=-17;z<=-14;z++){if(owner.level().setChunkForced(x,z,true))forced.add(net.minecraft.world.level.ChunkPos.pack(x,z));owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);
            owner.teleportTo(owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);game.items().mana().set(owner,15000);game.items().mana().inventoryChanged(owner);service.inventoryChanged(owner);
            for(int z=0;z<=10;z++){var pos=BlockPos.containing(owner.getEyePosition().add(0,0,z));blocks.put(pos,game.maps().snapshot(owner.level(),pos));owner.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);}
            start=game.server().getTickCount();at=start;
        }
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:item_defs/"+leaf);require(list.size()==1,"one actual original loot output "+leaf);return list.getFirst();}
        boolean callback(InteractionHand hand){return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(owner,owner.level(),hand).consumesAction();}
        void equip(){owner.setItemSlot(EquipmentSlot.HEAD,loot("minecraft/leather_helmet__4cbbe51cf2c5"));owner.getInventory().setItem(8,loot("lbc/manaregenerationbelt__d638c6be0801"));owner.getInventory().setItem(9,loot("lbc/regenerationbelt__44ae94f9db7b"));owner.setItemSlot(EquipmentSlot.FEET,loot("lbc/reactive__a2337e384e16"));service.inventoryChanged(owner);}
        boolean step(){int now=game.server().getTickCount();require(now-start<1500,"bounded actual native scheduler timeout phase="+step+" "+game.vanity().status());
            for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(step==0){if(now-start<5)return false;presets();
                var witness=EntityTypes.COW.create(owner.level(),EntitySpawnReason.TRIGGERED);require(witness!=null,"native old formation witness");witness.setPos(origin.add(5,0,0));witness.setNoAi(true);witness.setNoGravity(true);witness.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);witness.setHealth(1000);require(owner.level().addFreshEntity(witness),"native witness published");
                var evoker=loot("lbc/evokerwand__ad4508c26379");require(LbcBalanceItems.wand(evoker)==LbcBalanceItems.Wand.EVOKER,"source old flag identifies exact native runtime");
                owner.setItemInHand(InteractionHand.MAIN_HAND,evoker);mana=game.items().mana().get(owner);require(callback(InteractionHand.MAIN_HAND),"real Fabric callback old Evoker succeeds");
                require(service.cooldown(owner,LbcBalanceItems.Wand.EVOKER)==1000&&game.items().mana().get(owner)==mana,"old source cooldown1000 and no mana debit");
                require(owner.level().getEntitiesOfClass(EvokerFangs.class,owner.getBoundingBox().inflate(22),f->f.getOwner()==owner&&!before.contains(f)).size()>0,"real native owned old fang formation");
                owner.getAbilities().invulnerable=true;
                for(var leaf:List.of("lbc/corruptwand__c0eee1c1260c","lbc/enderwand__9609d9697317","lbc/vampirewand__29f7307d4cdd","lbc/foxuswand__5b42d21aff61","lbc/killingwand__912da49fd111","lbc/badwiki__b018be2e3746")){
                    var original=loot(leaf);var kind=LbcBalanceItems.wand(original);require(kind!=null,"actual original old wand resolver "+leaf);owner.setItemInHand(InteractionHand.MAIN_HAND,original);int beforeMana=game.items().mana().get(owner);require(callback(InteractionHand.MAIN_HAND),"actual original old wand callback "+kind);require(service.cooldown(owner,kind)==1000&&game.items().mana().get(owner)==beforeMana,"actual old native cooldown/no mana "+kind);owner.removeAllEffects();owner.teleportTo(owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);owner.setDeltaMovement(Vec3.ZERO);
                }
                owner.setItemInHand(InteractionHand.OFF_HAND,evoker.copy());owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);require(!callback(InteractionHand.OFF_HAND),"source old wands do not route offhand");owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                owner.setItemInHand(InteractionHand.MAIN_HAND,loot("lbc/basswand__fba71048cfce"));require(callback(InteractionHand.MAIN_HAND),"old Bass callback succeeds");require(!callback(InteractionHand.MAIN_HAND),"old Bass cooldown rejects immediate recast");
                equip();cooldown=service.cooldown(owner,LbcBalanceItems.Wand.BASS);game.items().timeStop().begin(owner.level(),100);require(!callback(InteractionHand.MAIN_HAND),"native TimeStop denies old callback");at=now;step++;return false;
            }
            if(step==1){if(now-at<8)return false;require(service.cooldown(owner,LbcBalanceItems.Wand.BASS)==cooldown,"native stopped world freezes old cooldown");game.items().timeStop().end(owner.level());at=now;step++;return false;}
            if(step==2){if(now-at<6)return false;int delta=cooldown-service.cooldown(owner,LbcBalanceItems.Wand.BASS);require(delta>=11&&delta%11==0,"hat4 plus belt2 plus base5 exact old cooldown step");require(owner.hasEffect(MobEffects.SLOW_FALLING),"real equipped reactive_old passive runs on scheduled ticks");
                var stack=loot("lbc/tnts__21b99d7319be");require(LbcBalanceRays.kind(stack)==LbcBalanceRays.Kind.TNT,"actual source old scroll metadata");stack.setCount(2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setYRot(0);owner.setXRot(0);var terminal=BlockPos.containing(owner.getEyePosition().add(0,0,8));owner.level().setBlock(terminal,Blocks.STONE.defaultBlockState(),2|16|32);mana=game.items().mana().get(owner);
                require(callback(InteractionHand.MAIN_HAND),"actual Fabric source old TNT ray callback");require(owner.getMainHandItem().getCount()==2,"old scroll debit waits for native terminal");at=now;step++;return false;
            }
            if(step==3){if(owner.getMainHandItem().getCount()==2){require(now-at<80,"fair shared ray queue reaches original terminal");return false;}
                require(owner.getMainHandItem().getCount()==1&&game.items().mana().get(owner)==mana,"one source old scroll debit without mana");
                var bombs=owner.level().getEntitiesOfClass(PrimedTnt.class,owner.getBoundingBox().inflate(20),e->!before.contains(e));require(bombs.size()==26&&bombs.stream().allMatch(t->t.getOwner()==owner&&t.getFuse()>0),"all26 native fuse56 owned source TNT directions");cleanFresh();
                var doubled=shoot("lbc/doublebow__2a65394e4d19",new ItemStack(Items.ARROW));require(doubled.size()==2&&doubled.stream().allMatch(e->e instanceof AbstractArrow a&&a.getOwner()==owner&&a.getPickupItemStackOrigin().is(Items.ARROW)),"actual source double_old native family exactly two owned ammo-preserving arrows");cleanFresh();
                var tipped=new ItemStack(Items.TIPPED_ARROW);tipped.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x123456),List.of(new MobEffectInstance(MobEffects.SLOWNESS,160,2)),Optional.empty()));
                var shulker=shoot("lbc/shulkerbow__ae63b4e4317f",tipped);require(shulker.size()==1&&LbcShulkerBow.managed(shulker.getFirst()),"actual old shulker source uses shared native carrier");require(ItemStack.matches(((LbcShulkerBow.Carrier)shulker.getFirst()).skywars2$shulkerState().ammo,tipped),"old shulker retains exact tipped ammo components");cleanFresh();
                for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin).offset(-3,-4,-5),BlockPos.containing(origin).offset(10,3,14)))blocks.putIfAbsent(pos.immutable(),game.maps().snapshot(owner.level(),pos));
                owner.setItemInHand(InteractionHand.MAIN_HAND,loot("lbc/bridgebuilder__3d4a19af6969/variant_7212e75be48f"));require(owner.gameMode.useItem(owner,owner.level(),owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"actual source bridge snowball native throw");at=now;step++;return false;
            }
            if(step==4){if(now-at<3)return false;boolean wool=false;for(var pos:blocks.keySet())if(owner.level().getBlockState(pos).is(Blocks.WOOL.pick(DyeColor.LIME)))wool=true;require(wool,"actual thrown source most projectile creates lime wool bridge");cleanFresh();
                owner.setItemSlot(EquipmentSlot.CHEST,loot("minecraft/leather_chestplate__c702455e0e43"));owner.getAbilities().invulnerable=false;owner.setHealth(owner.getMaxHealth());owner.removeAllEffects();owner.setInvulnerableTime(0);require(owner.hurtServer(owner.level(),owner.damageSources().generic(),1),"actual positive native chest damage accepted");
                var chestBombs=owner.level().getEntitiesOfClass(PrimedTnt.class,owner.getBoundingBox().inflate(2),e->!before.contains(e));require(chestBombs.size()==1&&chestBombs.getFirst().getFuse()==80&&chestBombs.getFirst().getOwner()==owner,"source dynamite chest actual damage callback emits owned fuse80 TNT");owner.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);cleanFresh();
                luckyPos=BlockPos.containing(origin.add(5,0,0));luckyToken=new ItemEntity(owner.level(),luckyPos.getX()+.5,luckyPos.getY(),luckyPos.getZ()+.5,loot("minecraft/barrier__1a3bf7f67b6c"));luckyToken.setThrower(owner);luckyToken.setNoGravity(true);require(owner.level().addFreshEntity(luckyToken),"actual original luckyspawner_old token drop");at=now;step++;return false;
            }
            if(step==5){if(owner.level().getBlockState(luckyPos).is(Blocks.SPAWNER))sawSpawner=true;boolean lucky=false;for(var pos:blocks.keySet())if(owner.level().getBlockState(pos).is(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL)))lucky=true;
                if(!lucky){require(now-at<100,"native original old lucky spawner emits actual LuckyBlock");return false;}require(sawSpawner&&luckyToken.isRemoved(),"actual source spawner placement plus central one-token debit");owner.level().setBlock(luckyPos,Blocks.AIR.defaultBlockState(),3);cleanFresh();
                var firestorm=loot("lbc/firestorm__391ee2213bdf");firestorm.setCount(2);owner.setItemInHand(InteractionHand.MAIN_HAND,firestorm);require(callback(InteractionHand.MAIN_HAND),"actual source old Firestorm ray callback");at=now;step=9;return false;
            }
            if(step==9){
                var carts=owner.level().getEntitiesOfClass(MinecartSpawner.class,owner.getBoundingBox().inflate(65),e->!before.contains(e)&&e.entityTags().contains("killedcart"));
                if(!carts.isEmpty()){var cart=carts.getFirst();var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,cart.registryAccess());cart.saveWithoutId(output);var data=output.buildResult();require(data.getIntOr("SpawnCount",0)==1&&data.getIntOr("SpawnRange",0)==10&&data.getIntOr("MinSpawnDelay",0)==2&&data.getIntOr("MaxSpawnDelay",0)==2&&data.getIntOr("RequiredPlayerRange",0)==100&&cart.isNoGravity(),"actual native old Firestorm exact BaseSpawner configuration");sawRainProducer=true;}
                var fireballs=owner.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball.class,owner.getBoundingBox().inflate(70),e->!before.contains(e)&&game.items().spellSources().source(e)!=null);
                if(fireballs.isEmpty()){require(now-at<80,"native old Firestorm BaseSpawner really emits a source-owned fireball");return false;}
                require(sawRainProducer&&owner.getMainHandItem().getCount()==1,"actual original cart emits child with one scroll debit");cleanFresh();var anvils=loot("lbc/anvilcast_old__fbd7975ff2c2");anvils.setCount(2);owner.setItemInHand(InteractionHand.MAIN_HAND,anvils);require(callback(InteractionHand.MAIN_HAND),"actual source old Anvil ray callback");at=now;sawRainProducer=false;step=10;return false;
            }
            if(step==10){
                var carts=owner.level().getEntitiesOfClass(MinecartSpawner.class,owner.getBoundingBox().inflate(65),e->!before.contains(e)&&e.entityTags().contains("killedanvils"));
                if(!carts.isEmpty()){var cart=carts.getFirst();var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,cart.registryAccess());cart.saveWithoutId(output);var data=output.buildResult();require(data.getIntOr("SpawnCount",0)==50&&data.getIntOr("SpawnRange",0)==8&&data.getIntOr("MinSpawnDelay",0)==19980&&data.getIntOr("MaxSpawnDelay",0)==19980&&data.getIntOr("RequiredPlayerRange",0)==999&&cart.isNoGravity(),"actual native old Anvil exact50/8/19980/999 BaseSpawner configuration");sawRainProducer=true;}
                var falling=owner.level().getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,owner.getBoundingBox().inflate(70),e->!before.contains(e)&&game.items().spellSources().source(e)!=null);
                if(falling.isEmpty()){require(now-at<80,"native old Anvil BaseSpawner really emits source-owned falling anvils");return false;}require(falling.stream().allMatch(e->e.getBlockState().is(Blocks.ANVIL)),"native rain preserves actual anvil block state");require(sawRainProducer&&owner.getMainHandItem().getCount()==1,"actual original Anvil native producer and one debit");cleanFresh();
                shoot("lbc/firework__8003a6bf1028",new ItemStack(Items.ARROW));require(owner.getMainHandItem().isEmpty(),"consumable original explosive bow debits exactly its one native stack");
                var shells=owner.level().getEntitiesOfClass(PrimedTnt.class,owner.getBoundingBox().inflate(8),e->!before.contains(e));require(shells.size()==29&&shells.stream().allMatch(t->t.getOwner()==owner),"actual original native explosive bow emits exactly29 owned TNT shells");
                var distribution=new TreeMap<Integer,Integer>();for(var body:shells){distribution.merge(body.getFuse(),1,Integer::sum);tntFuses.put(body.getUUID(),body.getFuse());tntPositions.put(body.getUUID(),body.position());}
                require(distribution.equals(new TreeMap<>(Map.ofEntries(Map.entry(30,3),Map.entry(400,5),Map.entry(800,2),Map.entry(810,2),Map.entry(820,2),Map.entry(830,2),Map.entry(840,3),Map.entry(850,2),Map.entry(860,2),Map.entry(870,2),Map.entry(880,2),Map.entry(890,2)))),"independent exact original29 fuse distribution");
                game.items().timeStop().begin(owner.level(),500);at=now;step=6;return false;
            }
            if(step==6){if(now-at<5)return false;for(var entry:tntFuses.entrySet()){var body=(PrimedTnt)owner.level().getEntity(entry.getKey());require(body!=null&&body.getFuse()==entry.getValue()&&body.position().equals(tntPositions.get(entry.getKey())),"real TimeStop preserves all native TNT shell fuses and geometry");}
                require(game.vanity().requestCapture(owner,false),"real full coordinator capture with nonempty old TNT family");step++;return false;
            }
            if(step==7){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;for(var id:tntFuses.keySet()){var body=(PrimedTnt)owner.level().getEntity(id);require(body!=null,"native captured TNT body");body.setFuse(2);body.setPos(body.position().add(.25,1,.25));}
                require(game.vanity().requestRestoreForCheck(),"real complete coordinator rewind of native old TNT family");step++;return false;
            }
            if(step==8){if(game.vanity().busy()||!game.vanity().status().equals("IDLE"))return false;for(var entry:tntFuses.entrySet()){var body=(PrimedTnt)owner.level().getEntity(entry.getKey());require(body!=null&&body.getFuse()==entry.getValue()&&body.position().equals(tntPositions.get(entry.getKey())),"full coordinator restores original nonempty29 TNT fuses/body geometry");}
                game.vanity().stop();game.items().timeStop().end(owner.level());cleanFresh();egg("lbc/omegatnt__b29a8435d113");at=now;step=11;return false;
            }
            if(step==11){var shells=owner.level().getEntitiesOfClass(PrimedTnt.class,owner.getBoundingBox().inflate(15),e->!before.contains(e));if(shells.isEmpty()){require(now-at<20,"actual compressed old egg callback resolves its native marker");return false;}
                require(shells.size()==16&&shells.stream().allMatch(t->t.isNoGravity()&&game.items().spellSources().source(t)!=null&&t.getDeltaMovement().lengthSqr()<1e-12),"actual original compressed egg produces16 stationary no-gravity WILD native TNT");cleanFresh();egg("minecraft/pig_spawn_egg__091d3d7e2b95");
                var pigs=owner.level().getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class,owner.getBoundingBox().inflate(15),e->!before.contains(e));require(pigs.size()==1&&pigs.getFirst().entityTags().contains("fakepig"),"actual source fake pig entity_data egg");var pig=pigs.getFirst();eggActor=pig.getUUID();require(pig.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"actual native hurt starts source fake-pig HurtTime2 reaction");at=now;step=12;return false;
            }
            if(step==12){var piglins=owner.level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.ZombifiedPiglin.class,owner.getBoundingBox().inflate(18),e->!before.contains(e));
                var dressed=piglins.stream().filter(p->p.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.TOTEM_OF_UNDYING)&&p.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.GOLDEN_AXE)&&p.getMaxHealth()==50).findFirst();
                if(dressed.isEmpty()){require(now-at<70,"actual fake-pig native lightning conversion and source5t gear event");return false;}require(dressed.get().getItemBySlot(EquipmentSlot.FEET).is(Items.GOLDEN_BOOTS)&&dressed.get().getCustomName()!=null&&dressed.get().getCustomName().getString().equals("Fake Lucky Pig"),"exact source fake-pig native armor/name");cleanFresh();owner.getAbilities().invulnerable=true;require(game.vanity().requestCapture(owner,false),"full point before native old nuke egg");step=13;return false;
            }
            if(step==13){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;sawRainProducer=false;egg("lbc/nuke__03c95bf6e9e0");var markers=owner.level().getEntitiesOfClass(net.minecraft.world.entity.Marker.class,owner.getBoundingBox().inflate(15),e->!before.contains(e)&&e.entityTags().contains("nuke_old"));require(markers.size()==1,"actual original old nuke egg yields its owned service actor");eggActor=markers.getFirst().getUUID();nukeAt=markers.getFirst().position();nukeBlasts=((Number)read(game.items().nukeEvent(),"blasts")).longValue();at=now;step=14;return false;}
            if(step==14){var marker=owner.level().getEntity(eggActor);if(marker!=null&&marker.entityTags().contains("nuke1_old")){var pos=BlockPos.containing(nukeAt);require(owner.level().getBlockState(pos).is(Blocks.NETHERITE_BLOCK)&&owner.level().getBlockState(pos.above()).is(Blocks.BEACON)&&owner.level().getBlockState(pos.above(2)).is(Blocks.STAINED_GLASS_PANE.pick(DyeColor.RED)),"actual original old nuke3x3 beacon/redpane source stage");sawRainProducer=true;}
                if(((Number)read(game.items().nukeEvent(),"blasts")).longValue()<nukeBlasts+9){require(now-at<700,"actual original20s old nuke callback reaches all9 native MOB explosions");return false;}
                require(sawRainProducer&&now-at>=400,"source old nuke warning/beacon and actual20s deadline preserved");require(game.vanity().requestRestoreForCheck(),"full coordinator restores terrain/actors after native old nuke egg");step=15;return false;
            }
            if(step==15){
                if(game.vanity().busy()||!game.vanity().status().equals("IDLE"))return false;
                owner.setYRot(0);owner.setXRot(-30);owner.setDeltaMovement(Vec3.ZERO);
                var gadget=loot("lbc/teleporter__4e10da09a998/variant_eff47abd2bbf");gadget.setCount(2);owner.setItemInHand(InteractionHand.MAIN_HAND,gadget);
                var blocker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());blocker.setPos(owner.position().add(23,0,0));blocker.entityTags().add("stopper");require(owner.level().addFreshEntity(blocker),"native source24 TECH marker");
                require(!game.items().magic().mechanismSuppressedAt(owner.level(),owner.position())&&game.items().magic().mechanismSuppressedAt(owner.level(),owner.position(),24),"old source24 differs from ordinary20 radius");
                require(!callback(InteractionHand.MAIN_HAND)&&gadget.getCount()==2,"TECH24 blocks old gadget without debit");blocker.discard();game.items().magic().unloaded(blocker);
                var start=owner.position();var expected=start.add(owner.getLookAngle().scale(5));
                require(callback(InteractionHand.MAIN_HAND)&&gadget.getCount()==1&&owner.position().distanceToSqr(expected)<.0001,"actual old gadget immediate feet-local5 and one debit");
                var position=BlockPos.containing(owner.position()).offset(7,0,0);var prior=game.maps().snapshot(owner.level(),position);
                try{
                    owner.level().setBlock(position,Blocks.SHULKER_BOX.defaultBlockState(),2|16|32);
                    var box=(net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity)owner.level().getBlockEntity(position);
                    box.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:infinity_old")),0);
                    box.unpackLootTable(owner);int self=0,outputs=0;
                    for(int i=0;i<box.getContainerSize();i++){var contents=box.getItem(i);if(contents.has(DataComponents.CONTAINER_LOOT))self++;else if(!contents.isEmpty())outputs++;}
                    require(self==1&&outputs==26,"native lazy old shulker keeps one selfreference and26 random outputs without eager recursion: self="+self+", outputs="+outputs);
                }finally{game.maps().restoreSnapshot(owner.level(),position,prior);}
                return true;
            }
            return false;
        }
        List<Projectile> shoot(String leaf,ItemStack ammo){owner.setYRot(0);owner.setXRot(0);owner.setDeltaMovement(Vec3.ZERO);owner.setItemInHand(InteractionHand.MAIN_HAND,loot(leaf));owner.setItemInHand(InteractionHand.OFF_HAND,ammo.copy());var weapon=owner.getMainHandItem();var previous=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());owner.level().getAllEntities().forEach(previous::add);require(owner.gameMode.useItem(owner,owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction(),"actual original source native bow use "+leaf);require(Items.BOW.releaseUsing(weapon,owner.level(),owner,71980),"actual native successful original bow release "+leaf);owner.stopUsingItem();require(owner.getOffhandItem().isEmpty(),"one original ammo debit "+leaf);var outputs=new ArrayList<Projectile>();owner.level().getAllEntities().forEach(e->{if(!previous.contains(e)&&e instanceof Projectile p&&!p.isRemoved()&&p.getOwner()==owner)outputs.add(p);});return outputs;}
        void egg(String leaf){var block=BlockPos.containing(origin).offset(6,-1,0);owner.level().setBlock(block,Blocks.STONE.defaultBlockState(),3);var source=loot(leaf);source.setCount(1);owner.setItemInHand(InteractionHand.MAIN_HAND,source);var context=new net.minecraft.world.item.context.UseOnContext(owner,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(block),net.minecraft.core.Direction.UP,block,false));require(source.useOn(context).consumesAction(),"actual native original entity_data egg use "+leaf);require(owner.getMainHandItem().isEmpty(),"exact one original native egg debit "+leaf);}
        void cleanFresh(){var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e!=owner&&e.position().distanceToSqr(origin)<40000)fresh.add(e);});for(var e:fresh){game.items().entityUnloaded(e);e.discard();}owner.setPos(origin);owner.setYRot(0);owner.setXRot(0);owner.setDeltaMovement(Vec3.ZERO);}
        void presets(){require(LbcBalanceWiki.EFFECTS.length==81,"finite exact81 old Wiki outcomes");var first=LbcBalanceWiki.EFFECTS[0];var last=LbcBalanceWiki.EFFECTS[80];require(first.id().equals("absorption")&&first.seconds()==120&&first.amplifier()==0&&last.id().equals("wither")&&last.seconds()==30&&last.amplifier()==2,"independent source first/last Wiki presets");var exceptional=LbcBalanceWiki.EFFECTS[19];require(exceptional.id().equals("saturation")&&exceptional.amplifier()==3,"source exceptional saturation outcome retained");}
        @SuppressWarnings("unchecked") public void close(){if(closed)return;closed=true;game.vanity().stop();game.items().timeStop().end(owner.level());
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e!=owner&&e.position().distanceToSqr(origin)<40000)fresh.add(e);});for(var e:fresh){game.items().entityUnloaded(e);e.discard();}
            blocks.forEach((pos,value)->game.maps().restoreSnapshot(owner.level(),pos,value));for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            saved.restore(game);stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));var profile=game.players().get(owner.getUUID());profile.lbcSkill=skill;profile.lbcTrinket=trinket;System.arraycopy(trinkets,0,profile.trinkets,0,trinkets.length);
            var values=(Map<UUID,int[]>)read(service,"cooldowns");values.clear();cooldowns.forEach((id,array)->values.put(id,array.clone()));var clocks=(Map<UUID,Integer>)read(service,"clocks");clocks.clear();clocks.putAll(phases);service.inventoryChanged(owner);
        }
    }
    private static Object read(Object target,String name){try{var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    public static void run(GameController game,LbcBalance service){require(running==null,"exclusive fixture");running=new Fixture(game,service);}
    public static void run(GameController game){run(game,game.items().balance());}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS balance-runtime (actual8 old source wands/Fabric callbacks/native fangs/no mana/hat+belt/TimeStop/reactive/TNT ray26/81Wiki; double/shulker/bridge/luckyspawner/chest/explosive29/native rain carts; nonempty29 TNT coordinator rewind; actual compressed/fakepig/20s9blast oldnuke eggs and whole terrain restore)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL balance-runtime",error);}}
}
