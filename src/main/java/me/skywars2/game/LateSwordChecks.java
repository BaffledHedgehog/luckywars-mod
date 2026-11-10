package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real match checks for the two player-charge swords and Kara's full-height native terrain fan. */
public final class LateSwordChecks {
    private LateSwordChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Late swords: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-4,why+" actual="+actual+" expected="+expected);}
    private static final class Preparation {
        final ServerPlayer owner;final net.minecraft.server.level.ServerLevel level;final String epoch;final Vec3 origin;final int tick;
        final Set<Long> forced=new LinkedHashSet<>();
        Preparation(GameController game,ServerPlayer owner){this.owner=owner;level=owner.level();epoch=game.items().epoch();tick=game.server().getTickCount();
            origin=new Vec3(Math.floor(owner.getX()/16)*16+4096.5,240.5,Math.floor(owner.getZ()/16)*16+.5);}
        void release(){for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);forced.clear();}
    }
    private static Preparation prepared;
    private static void releasePrepared(){if(prepared!=null){var previous=prepared;prepared=null;previous.release();}}
    public static void prepare(GameController game){
        releasePrepared();var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null&&game.playing()&&game.canUseGameplayItems(owner),"active participant for isolated native preparation");
        var setup=new Preparation(game,owner);prepared=setup;
        try{for(int x=(int)Math.floor((setup.origin.x-48)/16);x<=(int)Math.floor((setup.origin.x+48)/16);x++)
            for(int z=(int)Math.floor((setup.origin.z-16)/16);z<=(int)Math.floor((setup.origin.z+144)/16);z++){
                if(setup.level.setChunkForced(x,z,true))setup.forced.add(net.minecraft.world.level.ChunkPos.pack(x,z));
                var chunk=setup.level.getChunkAt(new BlockPos(x*16,240,z*16));var sections=chunk.getSections();
                for(int i=0;i<sections.length;i++){int y=chunk.getSectionYFromSectionIndex(i)<<4;
                    if(y>=0&&y<=319)require(sections[i].hasOnlyAir(),"full-height isolated native void region "+x+","+z+" section"+y);}
            }
        }catch(RuntimeException|Error error){releasePrepared();throw error;}
        SkyWars2.LOGGER.info("[check] PASS late-swords-prepare (isolated native resident region, exact scoped force tickets; prepareTick{})",setup.tick);
    }
    public static void releasePreparation(GameController game){releasePrepared();SkyWars2.LOGGER.info("[check] PASS late-swords-preparation-cleanup");}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final LbcChargedSwords swords;final LbcKaraSlabs kara;
        final SauvojenMeditationChecks.Attribution attribution;final Vec3 origin;
        final Map<BlockPos,MapManager.BlockSnapshot> cells=new LinkedHashMap<>();final Set<Long> forced=new LinkedHashSet<>();
        final List<Cow> cows=new ArrayList<>();final Map<String,Integer> scores=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advances=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>();final boolean blockDrops,mobDrops;boolean retain;
        final Map<Entity,List<Boolean>> ambientTags=new IdentityHashMap<>();
        Fixture(GameController game){
            this.game=game;swords=game.items().chargedSwords();kara=game.items().karaSlabs();
            require(swords.count()==0&&kara.count()==0,"exclusive native late-sword services");
            require(prepared!=null,"run late-swords-prepare in a separate native command before late-swords/stop-probe");
            require(game.server().getTickCount()>prepared.tick,"late-swords requires an actual later native server tick after preparation");
            require(prepared.owner==game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"))&&prepared.level==game.maps().level()&&game.items().currentEpoch(prepared.epoch),"preparation must belong to exact native participant, level and current epoch");
            base=new ElementalGrimoireChecks.Fixture(game);
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(base.owner));
            for(var entity:base.before)ambientTags.put(entity,List.of(entity.entityTags().contains("caster"),entity.entityTags().contains("hitten")));
            for(var name:List.of("swordcharged8","swordcharged9"))scores.put(name,score(name));
            for(var stat:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var key=Stats.CUSTOM.get(stat);stats.put(key,base.owner.getStats().getValue(key));}
            for(var item:List.of(LbcChargedSwords.TRIPLE,LbcChargedSwords.BLAZE,LbcKaraSlabs.OXIDIZED,LbcKaraSlabs.WEATHERED,LbcKaraSlabs.EXPOSED))
                for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_CRAFTED)){var stat=type.get(item);stats.put(stat,base.owner.getStats().getValue(stat));}
            var killed=Stats.ENTITY_KILLED.get(EntityTypes.COW);stats.put(killed,base.owner.getStats().getValue(killed));
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){
                var completed=new HashSet<String>();base.owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(completed::add);advances.put(advancement,completed);
            }
            var ownerProfile=game.players().get(base.owner.getUUID());try{for(var field:ownerProfile.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){
                var value=field.get(ownerProfile);profile.put(field,value instanceof int[] array?array.clone():value instanceof Set<?> set?new HashSet<>(set):value);
            }}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            blockDrops=base.owner.level().getGameRules().get(GameRules.BLOCK_DROPS);mobDrops=base.owner.level().getGameRules().get(GameRules.MOB_DROPS);
            var setup=prepared;require(setup!=null,"separate native late-swords preparation command required");origin=setup.origin;
            base.initialize();base.owner.removeAllEffects();ownerProfile.lbcSkill=0;game.config().offhandWands=true;
            try{
                require(setup==prepared&&setup.owner==base.owner&&setup.level==base.owner.level()&&game.items().currentEpoch(setup.epoch),"prepared native region belongs to exact participant/level/match epoch");
                require(game.server().getTickCount()>setup.tick,"isolated chunk preparation must precede fixture by actual native server tick");
                forced.addAll(setup.forced);setup.forced.clear();prepared=null;
                SkyWars2.LOGGER.info("[check] Late swords native preparation boundary prepareTick{} fixtureTick{}",setup.tick,game.server().getTickCount());
                require(base.owner.level().hasChunkAt(BlockPos.containing(origin)),"prepared native region remains resident");
                base.owner.teleportTo(base.owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);
                base.owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);base.owner.setHealth(200);base.owner.setAbsorptionAmount(0);
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))base.owner.setItemSlot(slot,ItemStack.EMPTY);
                base.mana.inventoryChanged(base.owner);base.owner.level().getGameRules().set(GameRules.BLOCK_DROPS,true,game.server());base.owner.level().getGameRules().set(GameRules.MOB_DROPS,false,game.server());
                score("swordcharged8",0);score("swordcharged9",0);held("triple_golden_sword",InteractionHand.MAIN_HAND);
            }catch(RuntimeException|Error error){close();throw error;}
        }
        int score(String name){return base.mana.score(base.owner,name).get();}
        void score(String name,int value){base.mana.score(base.owner,name).set(value);}
        ItemStack loot(String leaf){var result=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/"+leaf);require(result.size()==1,"source leaf "+leaf);return result.getFirst();}
        ItemStack held(String leaf,InteractionHand hand){base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var stack=loot(leaf);base.owner.setItemInHand(hand,stack);return stack;}
        boolean input(InteractionHand hand){return base.owner.gameMode.useItem(base.owner,base.owner.level(),base.owner.getItemInHand(hand),hand).consumesAction();}
        Cow cow(Vec3 at,float health){var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(health);cow.setHealth(health);require(base.owner.level().addFreshEntity(cow),"actual native cow");cows.add(cow);return cow;}
        boolean damage(Cow target,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type,float amount){target.setInvulnerableTime(0);target.damageCooldownTime=0;return target.hurtServer(base.owner.level(),new DamageSource(base.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),base.owner),amount);}
        void clearActors(){swords.forget(base.owner);kara.forget(base.owner);for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}cows.clear();}
        void set(BlockPos p,net.minecraft.world.level.block.state.BlockState state){cells.computeIfAbsent(p.immutable(),key->game.maps().snapshot(base.owner.level(),key));base.owner.level().setBlockAndUpdate(p,state);}
        void restoreTerrain(){cells.forEach((p,s)->{if(base.owner.level().getBlockEntity(p) instanceof net.minecraft.world.Container container)container.clearContent();game.maps().restoreSnapshot(base.owner.level(),p,s);});cells.clear();}
        void presentation(){
            var triple=loot("triple_golden_sword");var blaze=loot("blazefire_sword");
            require(triple.is(LbcChargedSwords.TRIPLE)&&triple.getMaxDamage()==900&&Identifier.parse("lbc:tier_b").equals(triple.get(DataComponents.TOOLTIP_STYLE)),"Triple source item/durability900/tierB");
            require(blaze.is(LbcChargedSwords.BLAZE)&&blaze.getMaxDamage()==600&&blaze.get(DataComponents.TOOLTIP_STYLE)==null,"Blaze durability600 and absent tooltip style");
            for(var pair:List.of(Map.entry(triple,"triple_golden_sword"),Map.entry(blaze,"lbckit.18"))){
                require(pair.getKey().get(DataComponents.CUSTOM_NAME).getContents() instanceof TranslatableContents t&&t.getKey().equals(pair.getValue()),"source sword name "+pair.getValue());
            }
            require(triple.get(DataComponents.LORE).lines().size()==3&&blaze.get(DataComponents.LORE).lines().size()==4,"exact source lore row counts");
            for(var entry:List.of(Map.entry(triple,8d),Map.entry(blaze,6d))){var mods=entry.getKey().get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
                require(mods.size()==2&&mods.stream().anyMatch(m->m.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)&&m.modifier().amount()==entry.getValue())
                        &&mods.stream().anyMatch(m->m.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)&&m.modifier().amount()==-2.4),"source exact sword attack attributes");}
            var first=blaze.get(DataComponents.LORE).lines().getFirst();require(first.getStyle().isItalic()&&first.getStyle().getColor().getValue()==0xa6f0e6,"Blaze special italic colored first lore");
            for(var leaf:List.of("kara_slab","kara_weathered_slab","kara_exposed_slab")){
                var stack=loot(leaf);require(LbcKaraSlabs.isSlab(stack)&&Identifier.parse("lbc:tier_ss").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"Kara native variant/SS "+leaf);
                require(stack.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("kara",0)==1,"source Kara flag");
                var name=stack.get(DataComponents.CUSTOM_NAME);require(name.getContents() instanceof TranslatableContents tr&&tr.getKey().equals(leaf.equals("kara_slab")?"god_slab_imba":"not_decaying_slab_imba")
                        &&name.getStyle().getColor()==null&&!name.getStyle().isItalic(),"source Kara translated uncolored name/style");
                require(EnchantmentHelper.getItemEnchantmentLevel(base.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE),stack)==255,"source Smite255 "+leaf);
                require(stack.get(DataComponents.LORE).lines().size()==2,"Kara exactly two source lore lines");
            }
        }
        void kit(){
            var owner=base.owner;var config=game.config();var saved=new GameChecks.WandThirdPlayerState(owner,base.mana);
            var ownerProfile=game.players().get(owner.getUUID());int oldKit=ownerProfile.lbcKit;var unlocks=new HashSet<>(ownerProfile.lbcUnlocks);
            boolean ranked=config.ranked,kits=config.lbcKits,skills=config.lbcSkills,levels=config.lbcLevelups,challenges=config.lbcChallenges;
            var gate="lbc:true_advancements/legends/academy/blazefire";var advancement=game.server().getAdvancements().get(Identifier.parse(gate));
            require(advancement!=null,"native source Blaze kit gate advancement");
            var completed=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(completed::add);
            require(!game.isObserver(owner),"actual participant before scoped menu observer");
            try{
                game.spectators().enter(owner);ownerProfile.lbcUnlocks.remove(gate);
                for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,criterion);
                config.ranked=false;require(!game.lbc().select(owner,LbcLoadouts.Kind.KITS,9000,19),"native source kit slot19 denied while locked");
                config.ranked=true;require(game.lbc().select(owner,LbcLoadouts.Kind.KITS,9000,19)&&ownerProfile.lbcKit==19,"native ranked bypass selects exact source kit19");
                config.ranked=false;require(game.lbc().unlock(owner,gate)&&game.lbc().select(owner,LbcLoadouts.Kind.KITS,9000,19),"native explicit gate unlock enables same kit");
                owner.getInventory().clearContent();config.lbcKits=true;config.lbcSkills=false;config.lbcLevelups=false;config.lbcChallenges=false;
                game.lbc().apply(owner,false);int swordsGranted=0;for(int i=0;i<owner.getInventory().getContainerSize();i++){
                    var item=owner.getInventory().getItem(i);if(LbcChargedSwords.isBlaze(item)){swordsGranted+=item.getCount();require(item.getMaxDamage()==600&&item.get(DataComponents.LORE).lines().size()==4,"native kit grant retains source Blaze components");}}
                require(swordsGranted==1,"actual native selected kit grants one source sword without invented extra passive");
            }finally{
                config.ranked=ranked;config.lbcKits=kits;config.lbcSkills=skills;config.lbcLevelups=levels;config.lbcChallenges=challenges;
                ownerProfile.lbcKit=oldKit;ownerProfile.lbcUnlocks.clear();ownerProfile.lbcUnlocks.addAll(unlocks);
                for(var criterion:advancement.value().criteria().keySet())if(completed.contains(criterion))owner.getAdvancements().award(advancement,criterion);else owner.getAdvancements().revoke(advancement,criterion);
                game.spectators().remove(owner);saved.restore(game);game.lbc().syncRanked();game.players().save();
            }
        }
        boolean melee(Cow target){
            target.setInvulnerableTime(0);target.damageCooldownTime=0;float before=target.getHealth();
            var tickers=new LinkedHashMap<java.lang.reflect.Field,Integer>();
            try{for(var name:List.of("attackStrengthTicker","itemSwapTicker")){
                Class<?> type=base.owner.getClass();java.lang.reflect.Field field=null;
                while(type!=null&&field==null){try{field=type.getDeclaredField(name);}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}
                if(field==null)throw new NoSuchFieldException(name);field.setAccessible(true);tickers.put(field,field.getInt(base.owner));field.setInt(base.owner,200);
            }base.owner.attack(target);}catch(ReflectiveOperationException error){throw new IllegalStateException("native charged-sword melee fixture",error);}
            finally{try{for(var entry:tickers.entrySet())entry.getKey().setInt(base.owner,entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
            return target.getHealth()<before;
        }
        void chargeHooks(){
            var target=cow(origin.add(0,0,2),200);held("triple_golden_sword",InteractionHand.MAIN_HAND);score("swordcharged8",0);
            require(damage(target,DamageTypes.MAGIC,1)&&score("swordcharged8")==0,"player-attributed magic does not count as a native Triple melee hit");
            for(int i=0;i<8;i++)require(melee(target),"accepted actual native Triple melee control");
            require(swords.tripleCharges(base.owner)==6,"source accepted native melee Triple charge cap6");
            score("swordcharged8",0);require(damage(target,DamageTypes.ARROW,1)&&score("swordcharged8")==0,"arrow-only cannot charge Triple");
            target.setPermanentlyInvulnerable(true);require(!melee(target),"native invulnerable melee rejection");require(score("swordcharged8")==0,"rejected native hurt grants no charge");target.setPermanentlyInvulnerable(false);
            target.setHealth(1);require(melee(target)&&target.isDeadOrDying()&&score("swordcharged8")==1,"actual fatal native melee charge hook once");clearActors();
            target=cow(origin.add(0,0,2),200);held("blazefire_sword",InteractionHand.MAIN_HAND);score("swordcharged9",5);
            require(damage(target,DamageTypes.MAGIC,1)&&swords.blazeCharges(base.owner)==5,"player-attributed spell does not recharge Blaze");
            require(melee(target)&&swords.blazeCharges(base.owner)==6,"uncapped source Blaze native melee charge");
            damage(target,DamageTypes.ARROW,1);require(score("swordcharged9")==6,"Blaze arrow followup never recharges");
            target.setHealth(1);require(melee(target)&&target.isDeadOrDying()&&score("swordcharged9")==7,"Blaze lethal native melee charges exactly once before death cleanup");clearActors();
        }
        void triple(){
            held("triple_golden_sword",InteractionHand.MAIN_HAND);score("swordcharged8",0);base.mana.set(base.owner,-999);
            require(!input(InteractionHand.MAIN_HAND),"Triple requires one player charge");score("swordcharged8",2);require(input(InteractionHand.MAIN_HAND)&&score("swordcharged8")==1&&base.mana.get(base.owner)==-999,"mana-free source triple activation/debit1");
            var copies=swords.snapshot();require(copies.size()==3,"three actual native sword copies");
            // Decode the authored row-major NBT through native Display, independently of the producer's JOML construction.
            com.mojang.math.Transformation expected;
            try{
                var source=new net.minecraft.world.entity.Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,base.owner.level());
                source.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,base.owner.registryAccess(),
                        net.minecraft.nbt.TagParser.parseCompoundFully("{transformation:[0f,0f,2.5f,0f,1.7678f,-1.7678f,0f,0f,1.7678f,1.7678f,0f,0f,0f,0f,0f,3f]}")));
                var read=net.minecraft.world.entity.Display.class.getDeclaredMethod("createTransformation",net.minecraft.network.syncher.SynchedEntityData.class);read.setAccessible(true);
                expected=(com.mojang.math.Transformation)read.invoke(null,source.getEntityData());
            }catch(ReflectiveOperationException|com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("authored native matrix decode",error);}
            var from=base.owner.getEyePosition().add(0,-.5,0);
            for(int i=0;i<3;i++){var copy=copies.get(i);near(copy.position().distanceToSqr(from),0,"source eye minus.5 origin");
                near(copy.direction().distanceToSqr(Vec3.directionFromRotation(0,10-i*10)),0,"source three authored yaw directions");
                near(copy.entity().display().getScale().distance(expected.scale()),0,"source projective matrix scale decomposition");
                require(Math.abs(copy.entity().display().getLeftRotation().dot(expected.leftRotation()))>.9999,"source native display matrix rotation");
            }
            swords.tick();for(var copy:copies){near(copy.entity().position().distanceToSqr(from.add(copy.direction().scale(1.2))),0,"source active1.2 flight step");copy.entity().age=98;}
            swords.tick();require(swords.count()==3&&swords.snapshot().stream().allMatch(v->v.age()==99),"Triple alive at99 active ticks");swords.tick();require(swords.count()==0,"Triple source expiry100");
            for(var hand:InteractionHand.values()){held("triple_golden_sword",hand);score("swordcharged8",1);require(input(hand)&&swords.count()==3,"actual independently charged used hand "+hand);clearActors();}
            held("triple_golden_sword",InteractionHand.MAIN_HAND);base.owner.setItemInHand(InteractionHand.OFF_HAND,loot("triple_golden_sword"));score("swordcharged8",2);
            require(input(InteractionHand.MAIN_HAND)&&swords.count()==6&&score("swordcharged8")==0,"same native click processes both copies against one shared player charge counter");clearActors();
            held("triple_golden_sword",InteractionHand.MAIN_HAND);score("swordcharged8",1);var victim=cow(from.add(0,-.4,1.2),100);
            var sweep=new net.minecraft.world.phys.AABB(from,from.add(0,0,1.2)).inflate(1);
            require(base.owner.level().getEntities(base.owner,sweep,e->e==victim).contains(victim),"prepared native spatial query must see actual newly spawned collision cow; position="+victim.position()+" box="+victim.getBoundingBox()+" entityTicking="+base.owner.level().isPositionEntityTicking(victim.blockPosition()));
            require(input(InteractionHand.MAIN_HAND),"native Triple collision volley");var contacting=swords.snapshot().get(1).entity();swords.tick();
            require(victim.getHealth()<100&&victim.getLastHurtByPlayer()==base.owner,"real native swept arrow4 hit retains caster; HP="+victim.getHealth()+" credit="+victim.getLastHurtByPlayer()+" target="+victim.position()+" box="+victim.getBoundingBox()+" shot="+contacting.position()+" removed="+contacting.isRemoved()+" age="+contacting.age+" grounded="+contacting.grounded+" velocity="+contacting.getDeltaMovement()+" owner="+contacting.getOwner()+" playerEyes="+base.owner.getEyePosition()+" yaw="+base.owner.getYRot()+" pitch="+base.owner.getXRot()+" targetEligible="+game.items().wands().projectileTarget(base.owner,victim)+" timeStopped="+game.items().timeStop().stopped(base.owner.level())+" suppressed="+game.items().magic().suppressed(base.owner)+" entityTicking="+base.owner.level().isPositionEntityTicking(victim.blockPosition()));
            require(contacting.isRemoved()&&contacting.holder==null,"native gameplay carrier retires immediately at accepted contact");
            var contact=swords.finishingSnapshot().stream().filter(view->view.display()==contacting.display()).findFirst().orElseThrow();
            require(contact.remainingTicks()==2&&contact.owner()==base.owner&&game.items().currentEpoch(contact.epoch()),"contact visual alone retains exact owner/epoch for bounded native interpolation");
            near(contact.position().distanceToSqr(contacting.position()),0,"retired contact visual stores exact native impact");
            near(contact.display().getCurrentPos().distanceToSqr(contact.position()),0,"detached contact frame reaches exact impact");
            near(contact.display().getOffset().lengthSqr(),0,"retired display has no forecast beyond contact");
            float healthAfterContact=victim.getHealth();swords.tick();
            require(swords.finishingSnapshot().stream().anyMatch(view->view.display()==contact.display()&&view.remainingTicks()==1),"contact model survives one complete interpolation update");
            near(victim.getHealth(),healthAfterContact,"retired visual cannot repeat native damage");swords.tick();
            require(swords.finishingSnapshot().stream().noneMatch(view->view.display()==contact.display()),"contact model retires after bounded final frame");
            near(victim.getHealth(),healthAfterContact,"final visual retirement cannot repeat native damage");clearActors();
            held("triple_golden_sword",InteractionHand.MAIN_HAND);score("swordcharged8",1);input(InteractionHand.MAIN_HAND);
            var central=swords.snapshot().get(1);BlockPos wall=BlockPos.containing(from.add(0,0,1));set(wall,Blocks.IRON_BARS.defaultBlockState());
            swords.tick();require(central.entity().grounded&&central.entity().embeddedBlock!=null,"native swept thin bars collision embeds source display");
            near(central.entity().position().z,wall.getZ()+.4375+.4,"native thin-shape boundary plus source.4 embedding");
            set(wall,Blocks.AIR.defaultBlockState());swords.tick();require(central.entity().isRemoved(),"embedded carrier breaks when forward space opens");clearActors();restoreTerrain();
        }
        void settle(){for(int i=0;i<128&&swords.blazePulses(base.owner)>0;i++)game.items().exasperato().tick();require(swords.blazePulses(base.owner)==0,"source loaded Blaze search settles");}
        void pulse(){swords.tick();game.items().exasperato().tick();swords.tick();game.items().exasperato().tick();settle();}
        void blaze(){
            held("blazefire_sword",InteractionHand.MAIN_HAND);score("swordcharged9",4);base.mana.set(base.owner,-900);
            require(!input(InteractionHand.MAIN_HAND),"Blaze requires source five charges");score("swordcharged9",6);require(input(InteractionHand.MAIN_HAND)&&swords.activeBlaze(base.owner),"native firing starts");
            var target=cow(origin.add(0,0,5),100);
            var clouds=game.items().clouds();var point=base.owner.getEyePosition().add(base.owner.getLookAngle().scale(2));
            var acids=List.of(clouds.ordinaryWandAcid(base.owner),clouds.academyWandAcid(base.owner,false),
                    clouds.greatAcid(base.owner,point,base.owner.getLookAngle()),
                    clouds.awakenedAcid(base.owner,point,base.owner.getLookAngle(),1,false),
                    clouds.awakenedAcid(base.owner,point,base.owner.getLookAngle(),2,false),
                    clouds.sourceAcid(base.owner.level(),point,0,0,300,true,base.owner),
                    clouds.flyingWandAcid(base.owner,point,base.owner.getLookAngle()));
            try{
                for(var acid:acids){acid.setPos(point);acid.setDeltaMovement(Vec3.ZERO);}
                pulse();
                require(acids.stream().allMatch(a->a.entityTags().contains("on_fire")),"real ultimate ignites every native acid variant regardless of its ownership or target policy");
            }finally{for(var acid:acids){clouds.unloaded(acid);acid.discard();}}
            near(target.getHealth(),98.9,"one in_fire1 plus arrow.1 per target per pulse");
            require(score("swordcharged9")==3&&swords.activeBlaze(base.owner)&&base.mana.get(base.owner)==-900,"own attributed fire never recharges melee-only charges; one mana-free debit3");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);pulse();require(score("swordcharged9")==0&&!swords.activeBlaze(base.owner),"already active stream persists without held sword until the second debit exhausts charges");pulse();require(score("swordcharged9")==0&&!swords.activeBlaze(base.owner)&&!base.owner.entityTags().contains("firefarting"),"source <=0 clamp/removes native fire state");clearActors();
            held("blazefire_sword",InteractionHand.OFF_HAND);score("swordcharged9",6);require(input(InteractionHand.OFF_HAND),"native offhand Blaze start");
            swords.clear(base.owner);require(score("swordcharged9")==0&&swords.activeBlaze(base.owner),"source purify counter0 retains already active stream");pulse();require(!swords.activeBlaze(base.owner),"source zero-charge tail executes then stops");clearActors();
            held("blazefire_sword",InteractionHand.MAIN_HAND);score("swordcharged9",6);var distant=cow(origin.add(0,0,110),100);input(InteractionHand.MAIN_HAND);pulse();
            require(distant.getHealth()<100,"source loaded Blaze ray reaches beyond arbitrary100-block arena");
            for(var entity:base.owner.level().getAllEntities())if(!base.before.contains(entity))
                require(!entity.entityTags().contains("hitten")&&!entity.entityTags().contains("caster"),"owned native helpers leave no ambient source selector tags");
            ambientTags.forEach((entity,tags)->require(entity.entityTags().contains("caster")==tags.get(0)&&entity.entityTags().contains("hitten")==tags.get(1),"preexisting actors keep exact ambient selector tags"));clearActors();
        }
        void kara(){
            held("kara_slab",InteractionHand.MAIN_HAND);var victim=cow(origin.add(0,0,2),200);
            require(melee(victim),"accepted actual native Kara melee");
            require(victim.getHealth()<200&&kara.count()==1,"actual accepted native Player.attack with slab produces one source center");
            var root=kara.snapshot().getFirst();near(root.position().distanceToSqr(base.owner.getEyePosition().add(0,0,1.5)),0,"source eye/yaw-only forward1.5 root");
            var column=BlockPos.containing(root.position());set(new BlockPos(column.getX(),-1,column.getZ()),Blocks.STONE.defaultBlockState());
            for(int y:new int[]{0,10,319})set(new BlockPos(column.getX(),y,column.getZ()),Blocks.STONE.defaultBlockState());
            var chest=new BlockPos(column.getX(),128,column.getZ());set(chest,Blocks.CHEST.defaultBlockState());((ChestBlockEntity)base.owner.level().getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,3));
            var supported=cow(new Vec3(column.getX()+.5,11,column.getZ()+.5),100);supported.setOnGround(true);
            kara.tick();require(kara.count()==3,"center emits exactly two native child markers before movement");
            var views=kara.snapshot();var center=views.stream().filter(LbcKaraSlabs.View::central).findFirst().orElseThrow();near(center.position().distanceToSqr(root.position().add(0,0,.7)),0,"source center movement.7");
            var children=views.stream().filter(v->!v.central()).toList();require(children.size()==2&&children.stream().allMatch(v->v.age()==1),"source children inherit current age1");
            for(var child:children){var side=child.yaw()==30?.8:-.8;var start=root.position().add(side,0,0);
                near(child.position().distanceToSqr(start),0,"source child born at local lateral.8; next selector tick begins movement");}
            require(children.stream().anyMatch(v->v.yaw()==30)&&children.stream().anyMatch(v->v.yaw()==-30),"source child yaw fan plus/minus30");
            for(int y:new int[]{0,10,128,319})require(base.owner.level().getBlockState(new BlockPos(column.getX(),y,column.getZ())).isAir(),"full absolute Kara column removal at"+y);
            require(base.owner.level().getBlockState(new BlockPos(column.getX(),-1,column.getZ())).is(Blocks.STONE),"negative Y terrain deliberately preserved");
            require(java.util.stream.StreamSupport.stream(base.owner.level().getAllEntities().spliterator(),false).anyMatch(e->e instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(Items.DIAMOND)&&item.getItem().getCount()==3),"native destroyed container drops actual contents");
            game.items().indirectKills().finishSupportRemoval();supported.setNoGravity(false);supported.setOnGround(false);supported.setHealth(1);supported.setInvulnerableTime(0);supported.damageCooldownTime=0;
            require(supported.hurtServer(base.owner.level(),supported.damageSources().fall(),100)&&supported.isDeadOrDying()&&supported.getKillCredit()==base.owner,"actual native fatal lost-support fall retains original Kara owner");
            long started=System.nanoTime();for(int i=1;i<100;i++)kara.tick();
            require(kara.count()==2&&kara.snapshot().stream().allMatch(v->!v.central()&&v.age()==100),"source central runs100 operations and leaves final two inherited-age children");
            var finalChild=kara.snapshot().getFirst();var finalCell=BlockPos.containing(finalChild.position()).atY(319);set(finalCell,Blocks.STONE.defaultBlockState());kara.tick();
            require(kara.count()==0&&base.owner.level().getBlockState(finalCell).isAir(),"age101 final children perform full column operation before removal");
            SkyWars2.LOGGER.info("[check] Kara full100+1 native fan CPUms{}",(System.nanoTime()-started)/1_000_000d);clearActors();restoreTerrain();
        }
        void placements(){
            int offset=0;for(var leaf:List.of("kara_slab","kara_weathered_slab","kara_exposed_slab")){
                var at=BlockPos.containing(origin.add(3+offset++,0,0));set(at.below(),Blocks.STONE.defaultBlockState());set(at,Blocks.AIR.defaultBlockState());
                base.owner.teleportTo(base.owner.level(),at.getX()-1.5,origin.y,at.getZ()+.5,Set.of(),-90,0,true);
                var stack=held(leaf,InteractionHand.MAIN_HAND);var hit=new BlockHitResult(Vec3.atCenterOf(at.below()).add(0,.5,0),Direction.UP,at.below(),false);
                require(base.owner.gameMode.useItemOn(base.owner,base.owner.level(),stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"real vanilla-placeable native Kara variant "+leaf);
                var expected=leaf.equals("kara_slab")?Blocks.CUT_COPPER_SLAB.waxed().oxidized():leaf.equals("kara_weathered_slab")?Blocks.CUT_COPPER_SLAB.waxed().weathered():Blocks.CUT_COPPER_SLAB.waxed().exposed();
                require(base.owner.level().getBlockState(at).is(expected)&&stack.isEmpty(),"correct vanilla slab block and native one-item consumption "+leaf);
            }restoreTerrain();base.owner.teleportTo(base.owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);
        }
        void blazeProtection(){
            var owner=base.owner;var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null,"actual native allied player");
            var state=new GameChecks.WandThirdPlayerState(other,base.mana);var indirect=new SauvojenMeditationChecks.Attribution(game,List.of(other));
            var board=game.server().getScoreboard();var previousOwner=owner.getTeam();var previousOther=other.getTeam();
            require(board.getPlayerTeam("sw2_late_guard")==null,"exclusive native protection team");var team=board.addPlayerTeam("sw2_late_guard");team.setAllowFriendlyFire(false);
            try{
                board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
                other.getInventory().clearContent();other.removeAllEffects();other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);other.getAbilities().invulnerable=false;
                other.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);other.setHealth(100);other.setAbsorptionAmount(0);other.setNoGravity(true);
                other.teleportTo(other.level(),origin.x,origin.y,origin.z+8,Set.of(),0,0,true);
                held("triple_golden_sword",InteractionHand.MAIN_HAND);score("swordcharged8",0);other.setInvulnerableTime(0);other.damageCooldownTime=0;
                var wild=game.items().indirectKills().captureDamage(new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),owner),game.items().indirectKills().wildCause(owner));
                require(other.hurtServer(other.level(),wild,1)&&swords.tripleCharges(owner)==0,"accepted allied WILD spell damage keeps friendly bypass but never counts as melee charge");
                other.setHealth(100);other.setInvulnerableTime(0);other.damageCooldownTime=0;
                var technical=cow(origin.add(0,0,4),100);technical.entityTags().add("rcdenyalways");var greed=cow(origin.add(0,0,12),100);greed.entityTags().add("sin_greed_active");
                held("blazefire_sword",InteractionHand.MAIN_HAND);score("swordcharged9",6);require(input(InteractionHand.MAIN_HAND),"protected target pulse activation");pulse();
                require(technical.getHealth()==100&&greed.getHealth()==100&&other.getHealth()==100,"native loaded Blaze preserves technical roles, Greed and allied players");
            }finally{
                clearActors();board.removePlayerTeam(team);if(previousOwner!=null)board.addPlayerToTeam(owner.getScoreboardName(),previousOwner);if(previousOther!=null)board.addPlayerToTeam(other.getScoreboardName(),previousOther);
                try{state.restore(game);}finally{indirect.close();}
            }
        }
        void guards(){
            held("triple_golden_sword",InteractionHand.MAIN_HAND);score("swordcharged8",1);input(InteractionHand.MAIN_HAND);kara.spawn(base.owner);
            var before=swords.snapshot();var fan=kara.snapshot();game.items().timeStop().begin(base.owner.level(),40);
            try{swords.tick();kara.tick();require(swords.snapshot().size()==3&&swords.snapshot().getFirst().age()==before.getFirst().age()&&kara.snapshot().getFirst().age()==fan.getFirst().age(),"native TimeStop pauses both exact producer ages");}finally{game.items().timeStop().end(base.owner.level());}
            swords.unloaded(before.getFirst().entity());require(before.getFirst().entity().isRemoved()&&swords.count()==2,"exact Triple unload cleanup");
            kara.unloaded(fan.getFirst().carrier());require(kara.count()==0,"exact Kara unload cleanup");clearActors();
            held("blazefire_sword",InteractionHand.MAIN_HAND);score("swordcharged9",6);input(InteractionHand.MAIN_HAND);
            base.field=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(base.owner.position());base.field.entityTags().add("stopper_magic");base.owner.level().addFreshEntity(base.field);game.items().magic().loaded(base.field);
            try{swords.tick();require(!swords.activeBlaze(base.owner)&&swords.blazePulses(base.owner)==0,"native suppression cancels active stream without ambient tags");}finally{base.removeField();}clearActors();
            held("triple_golden_sword",InteractionHand.MAIN_HAND);score("swordcharged8",1);input(InteractionHand.MAIN_HAND);kara.spawn(base.owner);
            game.items().forget(base.owner);require(swords.count()==0&&kara.count()==0&&swords.tripleCharges(base.owner)==0&&swords.blazeCharges(base.owner)==0,"actual shared owner-forget closes both families and counters");
        }
        @Override public void close(){
            try{
                if(!retain)clearActors();else base.retain=true;restoreTerrain();base.owner.level().getGameRules().set(GameRules.BLOCK_DROPS,blockDrops,game.server());base.owner.level().getGameRules().set(GameRules.MOB_DROPS,mobDrops,game.server());
                if(!retain)scores.forEach(this::score);stats.forEach((stat,value)->base.owner.getStats().setValue(base.owner,stat,value));
                advances.forEach((adv,completed)->{for(var criterion:adv.value().criteria().keySet())if(completed.contains(criterion))base.owner.getAdvancements().award(adv,criterion);else base.owner.getAdvancements().revoke(adv,criterion);});
                var ownerProfile=game.players().get(base.owner.getUUID());for(var entry:profile.entrySet())entry.getKey().set(ownerProfile,entry.getValue());
                var fresh=new ArrayList<Entity>();for(var entity:base.owner.level().getAllEntities())if(!base.before.contains(entity)&&!(entity instanceof ServerPlayer)
                        &&(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb)&&Math.abs(entity.getX()-origin.x)<64&&Math.abs(entity.getZ()-origin.z)<160)fresh.add(entity);fresh.forEach(Entity::discard);
            }catch(IllegalAccessException error){throw new IllegalStateException(error);}
            finally{try{base.close();}finally{try{attribution.close();}finally{for(long key:forced)base.owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);forced.clear();}}}
        }
    }
    private static List<Entity> retained;private static ServerPlayer retainedOwner;private static String retainedEpoch;
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.presentation();fixture.kit();fixture.chargeHooks();fixture.triple();fixture.blaze();fixture.blazeProtection();fixture.kara();fixture.placements();fixture.guards();}
        SkyWars2.LOGGER.info("[check] PASS late-swords (Triple/Blaze charge/fatal/input/native collision and source clocks, Kara100+1full-height fan/native container drops/support/three vanilla placements, state/lifecycle)");}
    public static void stopProbe(GameController game){require(retained==null,"exclusive late-sword stop probe");try(var fixture=new Fixture(game)){
        fixture.held("triple_golden_sword",InteractionHand.MAIN_HAND);fixture.score("swordcharged8",2);require(fixture.input(InteractionHand.MAIN_HAND),"retain Triple");
        fixture.held("blazefire_sword",InteractionHand.MAIN_HAND);fixture.score("swordcharged9",8);require(fixture.input(InteractionHand.MAIN_HAND),"retain Blaze");fixture.kara.spawn(fixture.base.owner);
        retained=new ArrayList<>();fixture.swords.snapshot().forEach(v->retained.add(v.entity()));fixture.kara.snapshot().forEach(v->retained.add(v.carrier()));
        require(retained.size()==4&&retained.stream().noneMatch(Entity::isRemoved),"four live native producers before actual stop");retainedOwner=fixture.base.owner;retainedEpoch=game.items().epoch();fixture.retain=true;
    }SkyWars2.LOGGER.info("[check] PASS late-swords-stop-probe");}
    public static void cleanup(GameController game){releasePrepared();require(retained!=null,"late sword stop probe exists");try{var service=game.items().chargedSwords();
        require(game.inLobby()&&!game.items().currentEpoch(retainedEpoch)&&service.count()==0&&service.finishingModels()==0&&service.tripleCharges(retainedOwner)==0&&service.blazeCharges(retainedOwner)==0
                &&!service.activeBlaze(retainedOwner)&&service.blazePulses(retainedOwner)==0&&game.items().karaSlabs().count()==0&&retained.stream().allMatch(Entity::isRemoved),"actual stop clears all owned carriers/queues/charge/epoch");
    }finally{retained.forEach(Entity::discard);retained=null;retainedOwner=null;retainedEpoch=null;}SkyWars2.LOGGER.info("[check] PASS late-swords-cleanup");}
}
