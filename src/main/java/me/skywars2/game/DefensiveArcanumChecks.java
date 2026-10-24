package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Synchronous native dispatch boundaries; no claim that explicit calls advance server time. */
public final class DefensiveArcanumChecks {
    private DefensiveArcanumChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Defensive Arcanums: "+why);}
    private static Object copy(Object value){if(value instanceof int[] a)return a.clone();if(value instanceof Set<?> s)return new HashSet<>(s);return value;}
    private static Map<Field,Object> fields(Object target){
        var result=new LinkedHashMap<Field,Object>();
        try{for(var f:target.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))result.put(f,copy(f.get(target)));}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}return result;
    }
    private static void fields(Object target,Map<Field,Object> values){try{for(var e:values.entrySet())e.getKey().set(target,copy(e.getValue()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static Map<Field,Object> attackers(ServerPlayer player){
        var saved=new LinkedHashMap<Field,Object>();
        try{for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime")){
            var field=net.minecraft.world.entity.LivingEntity.class.getDeclaredField(name);field.setAccessible(true);saved.put(field,field.get(player));
        }}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        return saved;
    }
    /** Existing pending combat must belong to its game, never to an operator-run fixture. */
    private static boolean cleanAttribution(GameController game,ServerPlayer player){
        var service=game.items().indirectKills();
        try{for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){
            field.setAccessible(true);if(((Map<?,?>)field.get(service)).containsKey(player))return false;
        }}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        return true;
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final PlayerStore.Profile profile;
        final Map<Field,Object> profileFields,attackerFields;final boolean shift;final int visualRegen,cowKills;
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Stat<Identifier>,Integer> custom=new LinkedHashMap<>();
        final Map<Stat<Item>,Integer> crafted=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){
            this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());profileFields=fields(profile);attackerFields=attackers(player);
            shift=player.isShiftKeyDown();visualRegen=game.items().mana().regeneration(player);cowKills=player.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));
            for(var adv:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);advancements.put(adv,done);}
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);custom.put(stat,player.getStats().getValue(stat));}
            for(var item:BuiltInRegistries.ITEM){var stat=Stats.ITEM_CRAFTED.get(item);crafted.put(stat,player.getStats().getValue(stat));}
        }
        void restore(GameController game){
            advancements.forEach((adv,done)->{for(var key:adv.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(adv,key);else player.getAdvancements().revoke(adv,key);});
            fields(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);fields(player,attackerFields);game.items().mana().score(player,"manaregen_visual").set(visualRegen);
            custom.forEach((stat,value)->player.getStats().setValue(player,stat,value));crafted.forEach((stat,value)->player.getStats().setValue(player,stat,value));
            player.getStats().setValue(player,Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcMana mana;final LbcDefensiveArcanums service;final ServerPlayer owner,other;
        final Saved saved,otherSaved;final Map<Field,Object> config;final Vec3 at;
        final Map<String,Integer> costs=new LinkedHashMap<>();final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final List<Cow> mobs=new ArrayList<>();
        Fixture(GameController game){
            this.game=game;mana=game.items().mana();service=game.items().defensiveArcanums();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active native participants");
            require(owner.containerMenu==owner.inventoryMenu&&other.containerMenu==other.inventoryMenu,"initial inventory menus");
            for(var p:List.of(owner,other))require(service.activeHands(p)==0&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0,"clean holding and frost state");
            require(!game.items().timeStop().stopped(owner.level()),"clean initial Time Stop");
            require(cleanAttribution(game,owner)&&cleanAttribution(game,other),"fresh fixture actors without pending indirect combat");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            for(String key:List.of("healing_arcanum_mana_tick","healing_arcanum_mana_tick_sec","barrier_arcanum_mana_activate","barrier_arcanum_mana_tick","barrier_arcanum_mana_tick_sec"))costs.put(key,mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly(key),"lbc.math").get());
            at=Vec3.atCenterOf(owner.blockPosition().offset(0,48,0));require(at.y+6<owner.level().getMaxY(),"saved high-altitude box below build ceiling");
        }
        void block(BlockPos pos,Block block){require(blocks.containsKey(pos),"terrain writes stay inside previously snapshotted box");owner.level().setBlock(pos,block.defaultBlockState(),2);}
        void move(ServerPlayer p,Vec3 point){p.teleportTo(p.level(),point.x,point.y,point.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        void init(){
            var center=BlockPos.containing(at);
            // Snapshot the entire bounded area before the first terrain write or player move.
            for(var p:BlockPos.betweenClosed(center.offset(-5,-4,-5),center.offset(5,5,5))){require(owner.level().hasChunkAt(p),"resident fixture cell");blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));}
            for(var p:blocks.keySet())block(p,Blocks.AIR);
            game.config().lbcSkills=false;game.config().ranked=false;
            for(var p:List.of(owner,other)){
                p.closeContainer();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setAbsorptionAmount(0);
                p.setInvulnerableTime(0);p.damageCooldownTime=0;p.entityTags().remove("time_stopped");mana.noMagic(p,0);mana.set(p,10000);mana.score(p,"manaregen_visual").set(10);game.players().get(p.getUUID()).lbcSkill=0;game.items().magic().inventoryChanged(p);
            }
            move(owner,at);move(other,at.add(4,0,0));game.items().magic().flushCleansing();
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("healing_arcanum_mana_tick"),"lbc.math").set(500);
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("barrier_arcanum_mana_activate"),"lbc.math").set(100);
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("barrier_arcanum_mana_tick"),"lbc.math").set(40);
        }
        ItemStack loot(String key){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+key);require(list.size()==1&&list.getFirst().getCount()==1,"live "+key+" exact single result");return list.getFirst();}
        void hands(String main,String off){service.forget(owner);owner.removeAllEffects();owner.setItemInHand(InteractionHand.MAIN_HAND,main==null?ItemStack.EMPTY:loot(main));owner.setItemInHand(InteractionHand.OFF_HAND,off==null?ItemStack.EMPTY:loot(off));mana.set(owner,10000);}
        AdvancementHolder healAdv(){var adv=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/arcanums/heal"));require(adv!=null,"actual source healing advancement exists");return adv;}
        boolean healedAdv(){return owner.getAdvancements().getOrStartProgress(healAdv()).isDone();}
        void revokeHeal(){for(var key:healAdv().value().criteria().keySet())owner.getAdvancements().revoke(healAdv(),key);}
        void presentation(){
            for(String key:List.of("healing_arcanum","barrier_arcanum")){
                var stack=loot(key);boolean heal=key.equals("healing_arcanum");var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(stack.is(heal?LbcDefensiveArcanums.HEALING:LbcDefensiveArcanums.BARRIER)&&Identifier.parse(heal?"lbc:arcanumhealing":"lbc:arcanumbarrier").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"native item, source model and A tier "+key);
                require(stack.get(DataComponents.LORE)!=null&&stack.get(DataComponents.LORE).lines().size()==4,"authored lore with obsolete barrier activation line removed "+key);
                require(stack.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name&&name.getKey().equals(key),"source translatable name "+key);
                for(String flag:List.of("transformable","arcanum","arcanumt",key))require(data.getIntOr(flag,0)==1,"source flag "+flag);
                require(heal?LbcDefensiveArcanums.isHealing(stack):LbcDefensiveArcanums.isBarrier(stack),"native predicate "+key);
            }
        }
        void healing(){
            hands("healing_arcanum",null);owner.setHealth(10);mana.set(owner,499);service.tickPlayer(owner);require(owner.getHealth()==10&&mana.get(owner)==499,"missing HP below500 rejects healing");
            mana.set(owner,500);service.tickPlayer(owner);require(owner.getHealth()==14&&mana.get(owner)==0,"mainhand actual four HP instant heal for500");
            hands(null,"healing_arcanum");owner.setHealth(10);service.tickPlayer(owner);require(owner.getHealth()==14&&mana.get(owner)==9500,"offhand independent healing");service.tickPlayer(owner);require(owner.getHealth()==18&&mana.get(owner)==9000,"no cooldown on successive dispatch");
            hands("healing_arcanum","healing_arcanum");owner.setHealth(10);service.tickPlayer(owner);require(owner.getHealth()==18&&mana.get(owner)==9000,"two hands independently heal and pay");
            // Isolated assertion for the author's fresh-health versus stale-score decision.
            hands("healing_arcanum","healing_arcanum");owner.setHealth(19);service.tickPlayer(owner);require(owner.getHealth()==20&&mana.get(owner)==9500,"each hand rechecks current missing HP");
            mana.set(owner,1000);service.tickPlayer(owner);require(owner.getHealth()==20&&mana.get(owner)==1000,"full HP never pays");
            hands("healing_arcanum",null);revokeHeal();owner.setHealth(1.01f);service.tickPlayer(owner);require(!healedAdv(),"above one HP does not grant source advancement");
            owner.setHealth(1);owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200,0));require(owner.getAbsorptionAmount()>0,"native absorption effect actually supplies hearts");service.tickPlayer(owner);require(!healedAdv(),"source health objective includes absorption in advancement admission");
            owner.removeEffect(MobEffects.ABSORPTION);owner.setAbsorptionAmount(0);owner.setHealth(1);service.tickPlayer(owner);require(healedAdv()&&owner.getHealth()==5,"at one HP grants actual healing advancement");
            hands(null,null);owner.getInventory().setItem(9,loot("healing_arcanum"));owner.setHealth(10);service.tickPlayer(owner);require(owner.getHealth()==10&&mana.get(owner)==10000,"carried-only healing has no effect");
        }
        void barrier(){
            hands("barrier_arcanum",null);mana.set(owner,49);service.tickPlayer(owner);require(mana.get(owner)==49&&service.activeHands(owner)==0&&!owner.hasEffect(MobEffects.RESISTANCE),"below own500percent price rejects without charge");
            mana.set(owner,50);service.tickPlayer(owner);require(mana.get(owner)==0&&service.activeHands(owner)==1&&!owner.hasEffect(MobEffects.RESISTANCE),"exact50 pays with no activation surcharge or Resistance effect");
            owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setHealth(20);owner.setDeltaMovement(Vec3.ZERO);
            require(!owner.hurtServer(owner.level(),owner.damageSources().playerAttack(other),5)&&owner.getHealth()==20&&owner.getInvulnerableTime()==0&&owner.getDeltaMovement().equals(Vec3.ZERO),"funded barrier rejects native hit before cooldown and knockback");
            require(!owner.hurtServer(owner.level(),owner.damageSources().magic(),5)&&owner.getHealth()==20,"funded barrier rejects native magic damage");
            require(owner.hurtServer(owner.level(),owner.damageSources().fellOutOfWorld(),1)&&owner.getHealth()==19,"native void damage bypasses barrier");owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            service.tickPlayer(owner);require(mana.get(owner)==0&&service.activeHands(owner)==0,"next unaffordable dispatch disables protection");
            require(owner.hurtServer(owner.level(),owner.damageSources().generic(),1)&&owner.getHealth()==18,"failed payment immediately permits actual damage");owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            hands("barrier_arcanum",null);service.tickPlayer(owner);require(mana.get(owner)==9950,"mainhand500percent regen cost without activation");
            owner.setItemInHand(InteractionHand.OFF_HAND,loot("barrier_arcanum"));service.tickPlayer(owner);require(mana.get(owner)==9900&&service.activeHands(owner)==2,"adding second hand still pays one player upkeep");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);service.tickPlayer(owner);require(mana.get(owner)==9850&&service.activeHands(owner)==1,"offhand independently protects for one upkeep");
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            require(owner.hurtServer(owner.level(),owner.damageSources().generic(),1)&&owner.getHealth()==17,"hand removal immediately permits damage before next service tick");owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            hands("barrier_arcanum",null);owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,123,1),owner);var resistance=owner.getEffect(MobEffects.RESISTANCE);service.tickPlayer(owner);
            require(owner.getEffect(MobEffects.RESISTANCE)==resistance&&resistance.getAmplifier()==1&&resistance.getDuration()==123,"barrier leaves unrelated Resistance instance intact");
            service.forget(owner);require(service.activeHands(owner)==0&&owner.getEffect(MobEffects.RESISTANCE)==resistance,"forget clears protection without clobbering unrelated Resistance");owner.removeAllEffects();
            // A preceding different player's price must not become this player's admission threshold.
            other.setItemInHand(InteractionHand.MAIN_HAND,loot("barrier_arcanum"));mana.score(other,"manaregen_visual").set(200);mana.set(other,10000);service.tickPlayer(other);
            require(mana.get(other)==9000,"other player's own200 regen costs1000");
            hands("barrier_arcanum",null);mana.score(owner,"manaregen_visual").set(7);mana.set(owner,35);service.tickPlayer(owner);require(mana.get(owner)==0&&service.activeHands(owner)==1,"own exact35 admission ignores preceding expensive player");
            service.forget(owner);mana.set(owner,34);service.tickPlayer(owner);require(mana.get(owner)==34&&service.activeHands(owner)==0,"own500percent price cannot overdraw at34");
            mana.score(owner,"manaregen_visual").set(0);mana.set(owner,0);service.tickPlayer(owner);require(mana.get(owner)==0&&service.activeHands(owner)==1,"zero regeneration has zero upkeep and no activation surcharge");
            mana.score(owner,"manaregen_visual").set(10);
            service.forget(other);other.getInventory().clearContent();
        }
        Cow cow(Vec3 point){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(point);c.setNoAi(true);c.setNoGravity(true);owner.level().addFreshEntity(c);mobs.add(c);return c;}
        void terrain(){
            hands("barrier_arcanum",null);var center=owner.blockPosition();var within=center.offset(1,0,0);block(within,Blocks.DIRT);service.tickPlayer(owner);require(owner.level().getBlockState(within).is(Blocks.DIRT),"one hand never clears terrain");
            hands("barrier_arcanum","barrier_arcanum");owner.setShiftKeyDown(false);
            for(var p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,2,1)))block(p.immutable(),Blocks.DIRT);
            var floor=center.below();var outside=center.offset(2,0,0);block(floor,Blocks.DIRT);block(outside,Blocks.DIRT);
            var chest=center.offset(1,1,0);block(chest,Blocks.CHEST);((ChestBlockEntity)owner.level().getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,37));
            var prior=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());owner.level().getAllEntities().forEach(prior::add);service.tickPlayer(owner);
            for(var p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,2,1)))require(owner.level().getBlockState(p).isAir(),"dual strict 3x3x3 cell "+p);
            require(owner.level().getBlockState(floor).is(Blocks.DIRT)&&owner.level().getBlockState(outside).is(Blocks.DIRT),"nonsneaking floor and outside remain");
            for(var e:owner.level().getAllEntities())require(prior.contains(e)||!(e instanceof net.minecraft.world.entity.item.ItemEntity),"strict removal suppresses block and chest inventory drops");
            // Standing victim loses real support to this holder's dual-barrier terrain operation.
            var victim=cow(Vec3.atBottomCenterOf(floor.above()));victim.setOnGround(true);owner.setShiftKeyDown(true);service.tickPlayer(owner);game.items().indirectKills().finishSupportRemoval();
            require(owner.level().getBlockState(floor).isAir()&&game.items().indirectKills().altitudeAuthor(victim)==owner,"sneaking floor removal retains barrier holder cause");
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;require(victim.hurtServer(owner.level(),victim.damageSources().fellOutOfWorld(),1000)&&victim.getKillCredit()==owner,"actual subsequent void death credits removed-support holder");
            block(floor,Blocks.DIRT);var later=cow(Vec3.atBottomCenterOf(floor.above()));later.setOnGround(true);service.tickPlayer(owner);game.items().indirectKills().finishSupportRemoval();later.setOnGround(false);
            require(later.hurtServer(owner.level(),later.damageSources().playerAttack(other),1),"actual later player hit accepted");later.setInvulnerableTime(0);later.damageCooldownTime=0;
            require(later.hurtServer(owner.level(),later.damageSources().fellOutOfWorld(),1000)&&later.getKillCredit()==other,"later accepted player hit supersedes support-removal cause");
            owner.setShiftKeyDown(false);block(outside,Blocks.AIR);
        }
        void boundaries(){
            hands("healing_arcanum","barrier_arcanum");owner.setHealth(10);service.tickPlayer(owner);int value=mana.get(owner);float hp=owner.getHealth();
            game.items().timeStop().begin(owner.level(),40);try{service.tickPlayer(owner);require(mana.get(owner)==value&&owner.getHealth()==hp&&service.activeHands(owner)==1,"actual Time Stop pauses healing and upkeep");}finally{game.items().timeStop().end(owner.level());}
            var stopper=new Marker(EntityTypes.MARKER,owner.level());stopper.setPos(at);stopper.entityTags().add("stopper_magic");owner.level().addFreshEntity(stopper);game.items().magic().loaded(stopper);
            try{service.tickPlayer(owner);require(mana.get(owner)==value&&owner.getHealth()==hp,"actual antimagic suppresses both hands");owner.setInvulnerableTime(0);owner.damageCooldownTime=0;require(owner.hurtServer(owner.level(),owner.damageSources().generic(),1),"suppressed barrier permits actual native damage");owner.setInvulnerableTime(0);owner.damageCooldownTime=0;}finally{game.items().magic().unloaded(stopper);stopper.discard();}
            owner.removeAllEffects();for(int i=0;i<10;i++)game.items().frost().add(owner,null);require(game.items().frost().isFrozen(owner),"native frost fixture actually frozen");
            value=mana.get(owner);hp=owner.getHealth();service.tickPlayer(owner);require(mana.get(owner)==value&&owner.getHealth()==hp,"actual frost pauses both held arcanums");game.items().frost().clear(owner);owner.removeAllEffects();
            hands("barrier_arcanum",null);service.tickPlayer(owner);game.spectators().enter(owner);service.tickPlayer(owner);require(game.isObserver(owner)&&service.activeHands(owner)==0,"actual observer role clears active hands");
        }
        @Override public void close(){
            service.forget(owner);service.forget(other);game.items().timeStop().end(owner.level());game.items().frost().clear(owner);game.items().frost().clear(other);
            for(var mob:mobs){game.items().indirectKills().clear(mob);mob.discard();}game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(var e:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());fields(game.config(),config);saved.restore(game);otherSaved.restore(game);
            costs.forEach((key,value)->mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly(key),"lbc.math").set(value));
            for(var entity:owner.level().getAllEntities())if(!before.contains(entity)&&(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb))entity.discard();
            game.players().save();mana.inventoryChanged(owner);mana.inventoryChanged(other);game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.init();f.presentation();f.healing();f.barrier();f.terrain();f.boundaries();}SkyWars2.LOGGER.info("[check] PASS defensive-arcanums (native loot/healing, held funded immunity except void, once-per-player500percent upkeep, immediate unequip, strict terrain/support attribution, gameplay gates; explicit boundary ticks)");}

    private static Saved stopSaved;private static String stopEpoch;
    public static void stopProbe(GameController game){
        require(stopSaved==null,"exclusive stop fixture");var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null&&game.canUseGameplayItems(owner)&&game.items().defensiveArcanums().activeHands(owner)==0,"live clean stop fixture");stopSaved=new Saved(game,owner);
        try{
            // Nested after the Void probe: keep its mainhand and all outer fixture inventory intact.
            owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcDefensiveArcanums.BARRIER));game.items().mana().set(owner,100000);
            owner.entityTags().remove("time_stopped");
            try{game.items().defensiveArcanums().tickPlayer(owner);}
            finally{owner.entityTags().add("time_stopped");}
            require(game.items().defensiveArcanums().activeHands(owner)==1,"actual native offhand barrier latch before match stop");stopEpoch=game.items().epoch();
        }
        catch(RuntimeException|Error failure){restoreStop(game);throw failure;}SkyWars2.LOGGER.info("[check] PASS defensive-arcanums-stop-probe");
    }
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().defensiveArcanums().activeHands(stopSaved.player)==0,"actual match stop clears barrier latch and item epoch");}finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS defensive-arcanums-cleanup");}
    private static void restoreStop(GameController game){if(stopSaved==null)return;game.items().defensiveArcanums().forget(stopSaved.player);stopSaved.restore(game);game.players().save();game.items().magic().flushCleansing();stopSaved=null;stopEpoch=null;}
}
