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
public final class BabylonChecks {
    private BabylonChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Babylon: "+why);}
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
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<Field,Object> profileFields,attackerFields;final boolean shift;final int visualRegen,cowKills;final net.minecraft.world.scores.PlayerTeam team;
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Stat<Identifier>,Integer> custom=new LinkedHashMap<>();
        final Map<Stat<Item>,Integer> crafted=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){
            this.player=player;attributes.assignAllValues(player.getAttributes());team=player.getTeam();state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());profileFields=fields(profile);attackerFields=attackers(player);
            shift=player.isShiftKeyDown();visualRegen=game.items().mana().regeneration(player);cowKills=player.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));
            for(var adv:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);advancements.put(adv,done);}
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);custom.put(stat,player.getStats().getValue(stat));}
            for(var item:BuiltInRegistries.ITEM)for(var type:List.of(Stats.ITEM_CRAFTED,Stats.ITEM_USED,Stats.ITEM_PICKED_UP,Stats.ITEM_DROPPED)){var stat=type.get(item);crafted.put(stat,player.getStats().getValue(stat));}
        }
        void restore(GameController game){
            advancements.forEach((adv,done)->{for(var key:adv.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(adv,key);else player.getAdvancements().revoke(adv,key);});
            var board=game.server().getScoreboard();if(player.getTeam()!=null)board.removePlayerFromTeam(player.getScoreboardName(),player.getTeam());if(team!=null)board.addPlayerToTeam(player.getScoreboardName(),team);fields(profile,profileFields);state.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(state.health);player.setAbsorptionAmount(state.absorption);player.setShiftKeyDown(shift);fields(player,attackerFields);game.items().mana().score(player,"manaregen_visual").set(visualRegen);
            custom.forEach((stat,value)->player.getStats().setValue(player,stat,value));crafted.forEach((stat,value)->player.getStats().setValue(player,stat,value));
            player.getStats().setValue(player,Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcMana mana;final LbcBabylon service;final ServerPlayer owner,other;
        final Saved saved,otherSaved;final Map<Field,Object> config;final Vec3 at;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final List<Cow> cows=new ArrayList<>();
        final Map<String,Integer> costs=new LinkedHashMap<>();final net.minecraft.world.scores.PlayerTeam team;
        Fixture(GameController game){
            this.game=game;mana=game.items().mana();service=game.items().babylon();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two native active participants");
            require(service.count()==0&&game.items().babylonEffects().pendingCount()==0&&!game.items().timeStop().stopped(owner.level()),"clean Babylon and Time Stop");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0&&cleanAttribution(game,p),"fresh fixture actors, menus, frost and indirect attribution");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            at=Vec3.atCenterOf(owner.blockPosition().offset(0,48,0));require(at.y+9<owner.level().getMaxY(),"bounded high altitude fixture below build ceiling");
            team=game.server().getScoreboard().addPlayerTeam("babylon_fixture");team.setAllowFriendlyFire(false);
            for(String key:List.of("babylon_gate_mana_tick","babylon_gate_mana_tick_sec"))costs.put(key,mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly(key),"lbc.math").get());
        }
        void init(){
            var center=BlockPos.containing(at);
            for(var p:BlockPos.betweenClosed(center.offset(-9,-6,-9),center.offset(9,9,9))){require(owner.level().hasChunkAt(p),"resident saved clearbox");blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));}
            for(var p:blocks.keySet())block(p,Blocks.AIR);
            game.config().ranked=false;game.config().lbcSkills=false;
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                for(var type:List.of(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)){var attribute=p.getAttribute(type);attribute.setBaseValue(0);for(var modifier:List.copyOf(attribute.getModifiers()))attribute.removeModifier(modifier.id());}
                p.getFoodData().setFoodLevel(16);p.getFoodData().setSaturation(0);
                p.entityTags().remove("time_stopped");mana.noMagic(p,0);mana.set(p,10000);game.players().get(p.getUUID()).lbcSkill=0;game.items().magic().inventoryChanged(p);
            }
            move(owner,at);move(other,at.add(8,0,0));game.items().magic().flushCleansing();
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("babylon_gate_mana_tick"),"lbc.math").set(300);
        }
        void block(BlockPos p,Block block){require(blocks.containsKey(p),"writes only within saved clearbox");owner.level().setBlock(p,block.defaultBlockState(),2);}
        void move(ServerPlayer p,Vec3 v){p.teleportTo(p.level(),v.x,v.y,v.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/babylon_gate");require(result.size()==1&&result.getFirst().getCount()==1,"exact one native loot result");return result.getFirst();}
        void clear(){for(var p:service.snapshot())service.unloaded(p);require(service.count()==0,"all emitted projectiles retired");}
        void hands(boolean main,boolean off){clear();owner.setItemInHand(InteractionHand.MAIN_HAND,main?loot():ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,off?loot():ItemStack.EMPTY);mana.set(owner,10000);}
        void presentation(){
            var item=loot();var data=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(item.is(LbcBabylon.ITEM)&&LbcBabylon.isBabylon(item)&&Identifier.parse("lbc:babylongate").equals(item.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(item.get(DataComponents.TOOLTIP_STYLE)),"native item/source model/tier");
            require(data.size()==4,"exact four authored custom flags");for(String flag:List.of("transformable","arcanum","arcanum2t","gate_of_babylon"))require(data.getIntOr(flag,0)==1,"authored flag "+flag);
            require(item.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name&&name.getKey().equals("gate_of_babylon")&&item.getHoverName().getStyle().getColor().getValue()==0xffaa00&&!item.getHoverName().getStyle().isItalic(),"gold nonitalic authored translation");
            require(item.get(DataComponents.LORE)!=null&&item.get(DataComponents.LORE).lines().size()==4,"four exact source lore rows");
            require(mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("babylon_gate_mana_tick_sec"),"lbc.math").get()==3000,"runtime initialized source HUD cost score");
        }
        void manaAndHands(){
            hands(true,false);mana.set(owner,299);service.tickPlayer(owner);require(mana.get(owner)==299&&service.count()==0,"below300 rejects");
            mana.set(owner,300);service.tickPlayer(owner);require(mana.get(owner)==0&&service.count()==1,"one funded main hand emits exactly one");
            hands(false,true);service.tickPlayer(owner);require(mana.get(owner)==9700&&service.count()==1,"one offhand emits exactly one");
            hands(true,true);service.tickPlayer(owner);require(mana.get(owner)==9400&&service.count()==2,"both hands independent payment and projectile");
            hands(true,true);mana.set(owner,300);service.tickPlayer(owner);require(mana.get(owner)==0&&service.count()==1,"main-first sequential affordability");
            hands(false,false);owner.getInventory().setItem(9,loot());service.tickPlayer(owner);require(mana.get(owner)==10000&&service.count()==0,"carried item inactive");owner.getInventory().setItem(9,ItemStack.EMPTY);
            hands(true,false);int beforeMana=mana.get(owner);service.tick();int expected=(game.server().getTickCount()&1)==0?1:0;require(service.count()==expected&&mana.get(owner)==beforeMana-300*expected,"actual dispatcher uses even server ticks");clear();
        }
        static Vec3 local(ServerPlayer p,double[] v){Vec3 forward=p.getLookAngle(),left=Vec3.directionFromRotation(0,p.getYRot()-90);return left.scale(v[0]).add(forward.cross(left).scale(v[1])).add(forward.scale(v[2]));}
        void launches(){
            hands(false,false);double[][] origins={{2.5,2,0},{-2.5,2,0},{0,3,0},{4,0,0},{-4,0,0}},motions={{-.2,.2,3},{.2,.2,3},{0,0,3},{-.5,.3,3},{.5,.3,3}};
            int[] colors={16746496,16777215,16770560},amps={101,109,110};
            for(float[] angle:List.of(new float[]{0,0},new float[]{71,-23})){
                owner.setYRot(angle[0]);owner.setXRot(angle[1]);
                for(int origin=1;origin<=5;origin++)for(int kind=1;kind<=5;kind++){
                    var shot=service.emit(owner,origin,kind);require(shot!=null&&shot.getOwner()==owner&&LbcBabylon.managed(shot)&&!shot.shouldBeSaved(),"native live current-owner unsaved projectile");
                    require(shot.position().distanceToSqr(owner.getEyePosition().add(local(owner,origins[origin-1])))<1e-9,"source rotated eye-relative origin "+origin);
                    Vec3 raw=local(owner,motions[origin-1]);Vec3 expected=new Vec3(Math.floor(raw.x*1000)*.001,Math.floor(raw.y*1000)*.001,Math.floor(raw.z*1000)*.001);require(shot.getDeltaMovement().distanceToSqr(expected)<1e-12,"milliblock native launch motion");
                    if(kind<=3){require(shot instanceof net.minecraft.world.entity.projectile.arrow.Arrow,"actual native arrow");var arrow=(net.minecraft.world.entity.projectile.arrow.Arrow)shot;var contents=arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS);require(contents!=null,"native tipped arrow item");var list=new ArrayList<MobEffectInstance>();contents.getAllEffects().forEach(list::add);require(list.size()==1&&list.getFirst().is(MobEffects.RAID_OMEN)&&list.getFirst().getDuration()==20&&list.getFirst().getAmplifier()==amps[kind-1]&&contents.getColor()==colors[kind-1],"source native color and20tick Raid marker");}
                    if(kind==4)require(shot instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident,"actual native trident");
                    if(kind==5){require(shot instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity,"actual native rocket");var rocket=(net.minecraft.world.entity.projectile.FireworkRocketEntity)shot;var fw=rocket.getItem().get(DataComponents.FIREWORKS);require(rocket.isShotAtAngle()&&fw!=null&&fw.explosions().size()==4,"exact four angled firework explosions");int[] rocketColors={16754176,16743168,16775936,16748800},fade={7031040,0,7955200,8932608};String[] shapes={"SMALL_BALL","LARGE_BALL","STAR","BURST"};for(int i=0;i<4;i++){var e=fw.explosions().get(i);require(e.shape().toString().equalsIgnoreCase(shapes[i])&&e.colors().size()==1&&e.colors().getInt(0)==rocketColors[i]&&e.fadeColors().size()==1&&e.fadeColors().getInt(0)==fade[i]&&e.hasTrail()&&e.hasTwinkle(),"exact source rocket explosion"+i);}var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess());rocket.saveWithoutId(out);require(out.buildResult().getIntOr("LifeTime",-1)==100,"native rocket lifetime100");}
                    service.unloaded(shot);require(shot.isRemoved()&&service.loaded(shot)&&service.count()==0,"retired projectile cannot reload");
                }
            }
            owner.setYRot(0);owner.setXRot(0);
        }
        Cow cow(Vec3 pos){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(pos);c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);c.setHealth(20);c.setNoAi(true);c.setNoGravity(true);owner.level().addFreshEntity(c);cows.add(c);return c;}
        void nativeFlights(){
            hands(false,false);owner.setYRot(0);owner.setXRot(0);
            for(int kind:List.of(1,4,5)){
                var shot=service.emit(owner,3,kind);var position=shot.position();var velocity=shot.getDeltaMovement();float yaw=shot.getYRot(),pitch=shot.getXRot();shot.tick();
                require(!shot.isRemoved()&&shot.position().distanceToSqr(position.add(velocity))<1e-8,"first native tick moves using authored velocity for kind"+kind);
                require(Float.isFinite(shot.getYRot())&&Float.isFinite(shot.getXRot())&&Math.abs(shot.getYRot()-yaw)<.01&&Math.abs(shot.getXRot()-pitch)<.01,"first native tick keeps authored orientation kind"+kind);service.unloaded(shot);
            }
            var victim=cow(at.add(0,0,4));var trident=service.emit(owner,3,4);trident.setPos(victim.getX(),victim.getY()+.6,victim.getZ()-2);trident.setDeltaMovement(new Vec3(0,0,3));trident.tick();
            require(victim.getHealth()==12&&victim.getLastHurtByMob()==owner,"native actual trident collision deals8 with native owner");service.unloaded(trident);victim.discard();
        }
        List<net.minecraft.world.entity.LightningBolt> bolts(){var result=new ArrayList<net.minecraft.world.entity.LightningBolt>();for(var e:owner.level().getAllEntities())if(e instanceof net.minecraft.world.entity.LightningBolt bolt&&!before.contains(e)&&!e.isRemoved())result.add(bolt);return result;}
        void clearBolts(){for(var bolt:bolts()){game.items().babylonEffects().unloaded(bolt);bolt.discard();}}
        void enqueue(Cow target,ServerPlayer author,int amp,int duration){var shot=service.emit(author,3,amp==101?1:amp==109?2:3);require(game.items().babylonEffects().accept(target,shot,new MobEffectInstance(MobEffects.RAID_OMEN,duration,amp)),"accepted native transport boundary");service.unloaded(shot);}
        void nativeArrowEffects(){
            hands(false,false);var effects=game.items().babylonEffects();int[] amps={101,109,110};
            for(int kind=1;kind<=3;kind++){
                var target=cow(at.add(0,0,4));var arrow=service.emit(owner,3,kind);arrow.setPos(target.getX(),target.getY()+.6,target.getZ()-2);arrow.setDeltaMovement(new Vec3(0,0,3));arrow.tick();
                require(target.getHealth()<20&&target.getLastHurtByMob()==owner&&!target.hasEffect(MobEffects.RAID_OMEN)&&effects.queued(target,amps[kind-1])==1,"actual native Arrow hit scales20 to2 then normalizes1 and consumes Raid marker kind"+kind);
                effects.clear(target);service.unloaded(arrow);target.discard();
            }
            var reflected=cow(at.add(0,0,4));var shot=service.emit(owner,3,1);shot.setOwner(other);shot.setPos(reflected.getX(),reflected.getY()+.6,reflected.getZ()-2);shot.setDeltaMovement(new Vec3(0,0,3));shot.tick();require(effects.queued(reflected,101)==1&&!reflected.hasEffect(MobEffects.RAID_OMEN),"changed current native owner arrow transport");reflected.setHealth(1);reflected.setInvulnerableTime(0);reflected.damageCooldownTime=0;effects.tickTarget(reflected);require(reflected.isDeadOrDying()&&reflected.getKillCredit()==other,"reflected current owner owns actual secondary native explosion death");service.unloaded(shot);
            var target=cow(at.add(0,0,4));enqueue(target,owner,109,2);enqueue(target,other,109,2);require(effects.queued(target,109)==2,"two owner contributions add rather than replace");effects.tickTarget(target);var first=bolts();require(first.size()==1&&effects.cause(first.getFirst()).player().equals(owner.getUUID())&&effects.queued(target,109)==1,"first FIFO native lightning retains first author");effects.tickTarget(target);var second=bolts();require(second.size()==2&&second.stream().anyMatch(b->effects.cause(b).player().equals(other.getUUID()))&&effects.queued(target,109)==0,"later FIFO contribution retains second author");clearBolts();
            enqueue(target,owner,109,2);target.entityTags().add("sin_greed_active");effects.tickTarget(target);require(effects.queued(target,109)==1&&bolts().isEmpty(),"Greed pauses existing counters");var denied=service.emit(owner,3,2);require(!effects.accept(target,denied,new MobEffectInstance(MobEffects.RAID_OMEN,2,109)),"Greed rejects new external transport");service.unloaded(denied);target.entityTags().remove("sin_greed_active");effects.tickTarget(target);require(effects.queued(target,109)==0&&bolts().size()==1,"Greed removal resumes queued contribution");clearBolts();
            enqueue(target,owner,109,2);game.items().timeStop().begin(owner.level(),40);try{effects.tickTarget(target);require(effects.queued(target,109)==1&&bolts().isEmpty(),"actual Time Stop pauses deferred transport");}finally{game.items().timeStop().end(owner.level());}effects.unloaded(target);require(effects.queued(target,109)==0,"resident unload drops live target index");effects.loaded(target);require(effects.queued(target,109)==1,"same-epoch attachment restores queued contribution");effects.clear(target);
            var ward=new ItemStack(Items.STICK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("mystical",1);data.putInt("type",2);ward.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setItem(9,ward);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
            enqueue(target,owner,109,2);effects.tickTarget(target);require(effects.queued(target,109)==0&&bolts().isEmpty(),"nearby anyone inventory ward consumes counter without action");owner.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
            var stopper=new Marker(EntityTypes.MARKER,owner.level());stopper.setPos(target.position());stopper.entityTags().add("stopper");owner.level().addFreshEntity(stopper);game.items().magic().loaded(stopper);
            try{enqueue(target,owner,101,2);enqueue(target,owner,109,2);float hp=target.getHealth();effects.tickTarget(target);require(target.getHealth()==hp&&effects.queued(target,101)==0&&bolts().size()==1,"mechanism stopper consumes101 while109 uses magic stopper");}finally{game.items().magic().unloaded(stopper);stopper.discard();clearBolts();effects.clear(target);}
            int globalSkills=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#lbcskill"),"swrg.math").get();
            try{mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#lbcskill"),"swrg.math").set(1);enqueue(target,owner,109,2);mana.score(target,"lbc.skill").set(19);effects.tickTarget(target);require(effects.queued(target,109)==0&&bolts().isEmpty(),"skill19 clears existing deferred counter under source global gate");enqueue(target,owner,109,2);require(effects.queued(target,109)==0,"skill19 consumes incoming marker without counter");}finally{mana.score(target,"lbc.skill").set(0);mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#lbcskill"),"swrg.math").set(globalSkills);}
            // Real vanilla lightning remains WILD and its old cause cannot supersede a later accepted player hit.
            enqueue(target,owner,109,2);effects.tickTarget(target);var bolt=bolts().getFirst();
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);move(other,target.position().add(1,0,0));other.setHealth(20);other.setInvulnerableTime(0);other.damageCooldownTime=0;target.setInvulnerableTime(0);target.damageCooldownTime=0;
            bolt.tick();require(other.getHealth()==15&&target.getHealth()==15,"actual vanilla lightning5 damages allied player and mob without friendly filters: playerHp="+other.getHealth()+" mobHp="+target.getHealth()+" playerEffects="+other.getActiveEffects()+" mobEffects="+target.getActiveEffects()+" mobInvul="+target.getInvulnerableTime()+" playerInvul="+other.getInvulnerableTime());
            target.setInvulnerableTime(0);target.damageCooldownTime=0;require(target.hurtServer(owner.level(),target.damageSources().playerAttack(other),1),"later real player hit accepted after owned lightning");target.setInvulnerableTime(0);target.damageCooldownTime=0;require(target.hurtServer(owner.level(),target.damageSources().onFire(),1000)&&target.getKillCredit()==other,"later player hit retains priority over old lightning/fire cause");clearBolts();
            other.setRemainingFireTicks(0);other.setHealth(20);other.setInvulnerableTime(0);other.damageCooldownTime=0;move(other,at.add(8,0,0));game.server().getScoreboard().removePlayerFromTeam(owner.getScoreboardName(),team);game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);target.discard();
        }
        void greed(){
            hands(false,false);owner.setHealth(20);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setDeltaMovement(Vec3.ZERO);owner.entityTags().add("sin_greed_active");
            try{
                require(!owner.hurtServer(owner.level(),owner.damageSources().generic(),100)&&!owner.hurtServer(owner.level(),owner.damageSources().fellOutOfWorld(),100)&&owner.getHealth()==20,"Greed rejects native generic and void damage");
                owner.setHealth(1);require(owner.getHealth()==20,"Greed rejects direct health decrease");owner.knockback(2,1,0,owner.damageSources().generic(),0);require(owner.getDeltaMovement().equals(Vec3.ZERO),"Greed rejects native knockback");
                require(!owner.addEffect(new MobEffectInstance(MobEffects.POISON,200,1),other)&&!owner.hasEffect(MobEffects.POISON),"Greed rejects external negative potion effect");
                var arrow=service.emit(other,3,2);arrow.setPos(owner.getX(),owner.getY()+.6,owner.getZ()-2);arrow.setDeltaMovement(new Vec3(0,0,3));arrow.tick();require(owner.getHealth()==20&&!owner.hasEffect(MobEffects.RAID_OMEN)&&game.items().babylonEffects().queued(owner,109)==0&&arrow.getOwner()==owner&&arrow.getDeltaMovement().z<0,"Greed actual native arrow impact reverses flight/current owner without damage or secondary counter");service.unloaded(arrow);
            }finally{owner.entityTags().remove("sin_greed_active");}
        }
        void firebreak(){
            var effects=game.items().babylonEffects();
            for(double fraction:List.of(.2,.8)){
                var target=cow(new Vec3(Math.floor(at.x)+fraction,Math.floor(at.y)+fraction,Math.floor(at.z)+fraction));var center=target.blockPosition();
                for(var p:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))block(p.immutable(),Blocks.AIR);
                var innerLo=BlockPos.containing(target.getX()-1.5,target.getY()-1.5,target.getZ()-1.5);var innerHi=BlockPos.containing(target.getX()+1.5,target.getY()+1.5,target.getZ()+1.5);
                for(var p:BlockPos.betweenClosed(innerLo,innerHi))block(p.immutable(),Blocks.DIRT);
                block(center.offset(3,-1,0),Blocks.STONE);var outside=center.offset(4,0,0);block(outside,Blocks.DIAMOND_BLOCK);var beforeItems=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());owner.level().getAllEntities().forEach(beforeItems::add);
                enqueue(target,owner,110,2);effects.tickTarget(target);game.items().indirectKills().finishSupportRemoval();
                for(var p:BlockPos.betweenClosed(innerLo,innerHi))require(owner.level().getBlockState(p).isAir(),"fractional firebreak strict command-floor carve "+fraction+" cell"+p);
                require(owner.level().getBlockState(center.offset(3,0,0)).is(Blocks.FIRE)&&owner.level().getBlockState(outside).is(Blocks.DIAMOND_BLOCK),"native radius3 air fire and untouched outside");boolean drops=false;for(var e:owner.level().getAllEntities())if(!beforeItems.contains(e)&&e instanceof net.minecraft.world.entity.item.ItemEntity)drops=true;require(drops,"native central destroy preserves block drops");effects.clear(target);target.discard();
            }
            for(var p:blocks.keySet())block(p,Blocks.AIR);
        }
        net.minecraft.world.entity.projectile.Projectile blockShot(int kind,BlockPos wall,ServerPlayer author){
            block(wall,kind==3?Blocks.DIRT:Blocks.OBSIDIAN);
            var shot=service.emit(owner,3,kind);shot.setOwner(author);shot.setNoGravity(true);
            shot.setPos(wall.getX()+.5,wall.getY()+.5,wall.getZ()-1.5);shot.setDeltaMovement(new Vec3(0,0,3));shot.tick();
            require(shot.isRemoved()&&service.count()==0,"native solid impact consumes Babylon transport exactly once kind"+kind);
            require(shot.position().distanceToSqr(new Vec3(wall.getX()+.5,wall.getY()+.5,wall.getZ()-.05F))<1e-10,"block consequence uses native embedded arrow position before retirement");
            return shot;
        }
        void nativeBlockEffects(){
            hands(false,false);var effects=game.items().babylonEffects();var wall=BlockPos.containing(at).offset(0,2,4);
            var victim=cow(new Vec3(wall.getX()+1.3,wall.getY()-.1,wall.getZ()-.05));victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);victim.setHealth(100);
            var technical=cow(new Vec3(wall.getX()-.3,wall.getY()-.1,wall.getZ()-.05));technical.entityTags().add("strict_map_object");
            var shot=blockShot(1,wall,other);require(victim.getHealth()<100&&victim.getKillCredit()==other&&technical.getHealth()==20,"native block-triggered power1 explosion retains reflected current owner and protected roles: hp="+victim.getHealth()+" credit="+victim.getKillCredit()+" expected="+other+" technical="+technical.getHealth()+" source="+victim.getLastDamageSource());
            victim.setHealth(100);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;shot.tick();require(victim.getHealth()==100,"retired explosion transport cannot trigger again on another native tick");game.items().indirectKills().clear(victim);victim.discard();technical.discard();block(wall,Blocks.AIR);
            shot=blockShot(2,wall,other);var bolts=bolts();require(bolts.size()==1&&bolts.getFirst().position().distanceToSqr(shot.position())<1e-10&&effects.cause(bolts.getFirst()).player().equals(other.getUUID()),"native block-triggered lightning spawns once at impact with reflected owner");shot.tick();require(bolts().size()==1,"second native tick cannot duplicate block lightning");clearBolts();block(wall,Blocks.AIR);
            var support=wall.offset(3,-1,-1);block(support,Blocks.STONE);var outside=wall.offset(4,0,0);block(outside,Blocks.DIAMOND_BLOCK);
            shot=blockShot(3,wall,owner);var center=shot.blockPosition();
            require(owner.level().getBlockState(wall).isAir()&&owner.level().getBlockState(center.offset(3,0,0)).is(Blocks.FIRE)&&owner.level().getBlockState(outside).is(Blocks.DIAMOND_BLOCK),"block firebreak preserves source air fire, fractional destructive inner carve and outside boundary");
            block(wall,Blocks.DIRT);shot.tick();require(owner.level().getBlockState(wall).is(Blocks.DIRT),"retired firebreak transport cannot carve a replacement block again");
            for(var pos:blocks.keySet())block(pos,Blocks.AIR);
            var ward=new ItemStack(Items.STICK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("mystical",1);data.putInt("type",2);ward.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setItem(9,ward);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
            try{blockShot(2,wall,owner);require(bolts().isEmpty(),"nearby inventory ward consumes in-ground transport without lightning");}finally{owner.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();block(wall,Blocks.AIR);}
            block(wall,Blocks.OBSIDIAN);var paused=service.emit(owner,3,2);paused.setNoGravity(true);paused.setPos(wall.getX()+.5,wall.getY()+.5,wall.getZ()-1.5);paused.setDeltaMovement(new Vec3(0,0,3));var pausedAt=paused.position();
            game.items().timeStop().begin(owner.level(),40);
            try{paused.tick();require(!paused.isRemoved()&&paused.position().equals(pausedAt)&&bolts().isEmpty(),"Time Stop pauses native arrow before its block consequence");}finally{game.items().timeStop().end(owner.level());}
            paused.tick();require(paused.isRemoved()&&bolts().size()==1,"resumed native block contact releases exactly one deferred-by-flight consequence");clearBolts();block(wall,Blocks.AIR);
            var stopper=new Marker(EntityTypes.MARKER,owner.level());stopper.setPos(Vec3.atCenterOf(wall));stopper.entityTags().add("stopper");owner.level().addFreshEntity(stopper);game.items().magic().loaded(stopper);
            try{blockShot(2,wall,owner);require(bolts().size()==1,"mechanism-only stopper does not suppress block lightning");clearBolts();}finally{game.items().magic().unloaded(stopper);stopper.discard();block(wall,Blocks.AIR);}
            var magicStopper=new Marker(EntityTypes.MARKER,owner.level());magicStopper.setPos(Vec3.atCenterOf(wall));magicStopper.entityTags().add("stopper_magic");owner.level().addFreshEntity(magicStopper);game.items().magic().loaded(magicStopper);
            try{blockShot(2,wall,owner);require(bolts().isEmpty(),"magic stopper consumes block lightning transport without action");}finally{game.items().magic().unloaded(magicStopper);magicStopper.discard();block(wall,Blocks.AIR);}
            require(effects.pendingCount()==0,"block impacts never fabricate living-target effect queues");
        }
        void pools(){
            Set<Item> ordinary=Set.of(LbcVoidArcanum.ITEM,LbcDefensiveArcanums.BARRIER,LbcDefensiveArcanums.HEALING,LbcKiuas.KIUASKIVI,LbcTanner.ITEM,LbcBabylon.ITEM);
            var all=new HashSet<>(ordinary);all.add(LbcKiuas.SUN);all.add(LbcIceMaterials.NEUTRAL_BALL);var exactAll=Set.copyOf(all);
            for(var pool:List.of("transformables_arcanum","transformables_arcanum_all")){var expected=pool.endsWith("_all")?exactAll:ordinary;var seen=new HashSet<Item>();for(int i=0;i<256;i++){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:"+pool);require(result.size()==1&&result.getFirst().getCount()==1&&expected.contains(result.getFirst().getItem()),"closed native pool exact singleton outcome "+pool);seen.add(result.getFirst().getItem());}require(seen.equals(expected),"all immutable audited pool outcomes observed "+pool);}
        }
        void boundaries(){
            hands(true,true);mana.noMagic(owner,1);service.tickPlayer(owner);require(service.count()==0&&mana.get(owner)==10000,"noMagic rejects both hands");mana.noMagic(owner,0);
            owner.entityTags().add("time_stopped");service.tickPlayer(owner);require(service.count()==0,"per entity Time Stop tag");owner.entityTags().remove("time_stopped");
            game.items().timeStop().begin(owner.level(),40);try{service.tickPlayer(owner);require(service.count()==0,"actual dimension Time Stop");}finally{game.items().timeStop().end(owner.level());}
            for(int i=0;i<10;i++)game.items().frost().add(owner,null);require(game.items().frost().isFrozen(owner),"real frost fixture");service.tickPlayer(owner);require(service.count()==0,"real frost rejects");game.items().frost().clear(owner);owner.removeAllEffects();
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);
            try{service.tickPlayer(owner);require(service.count()==0&&mana.get(owner)==10000,"actual antimagic rejects");}finally{game.items().magic().unloaded(marker);marker.discard();}
            var shot=service.emit(owner,3,1);game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            require(!service.canAffect(shot,owner)&&!service.canAffect(shot,other),"direct native projectiles protect caster/allied player");var friend=cow(at.add(0,0,3));game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);require(service.canAffect(shot,friend),"friendly mobs remain direct targets");friend.entityTags().add("strict_map_object");require(!service.canAffect(shot,friend),"technical role excluded");clear();
            game.spectators().enter(owner);service.tickPlayer(owner);require(game.isObserver(owner)&&service.count()==0,"observer gate");
        }
        @Override public void close(){
            clear();service.forget(owner);service.forget(other);game.items().timeStop().end(owner.level());game.items().frost().clear(owner);game.items().frost().clear(other);
            for(var c:cows){game.items().babylonEffects().clear(c);game.items().indirectKills().clear(c);c.discard();}game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());fields(game.config(),config);saved.restore(game);otherSaved.restore(game);game.server().getScoreboard().removePlayerTeam(team);
            costs.forEach((key,value)->mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly(key),"lbc.math").set(value));
            var debris=new ArrayList<Entity>();
            for(var e:owner.level().getAllEntities())if(!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb||e instanceof net.minecraft.world.entity.LightningBolt))debris.add(e);
            for(var e:debris){game.items().babylonEffects().unloaded(e);e.discard();}
            game.players().save();mana.inventoryChanged(owner);mana.inventoryChanged(other);game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.init();f.presentation();f.manaAndHands();f.launches();f.nativeFlights();f.nativeArrowEffects();f.greed();f.firebreak();f.nativeBlockEffects();f.pools();f.boundaries();}SkyWars2.LOGGER.info("[check] PASS babylon (native loot, independent both-hand300 dispatch, source25 origin/weapon combinations at2 angles, native markers/rocket, block/entity consequences, gates and reload retirement; explicit boundary ticks)");}
    private static Saved stopSaved;private static String stopEpoch;private static net.minecraft.world.entity.projectile.Projectile stopShot;private static Cow stopTarget;
    public static void stopProbe(GameController game){
        require(stopSaved==null,"exclusive stop fixture");var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null&&game.canUseGameplayItems(owner)&&game.items().babylon().count()==0,"clean actual stop probe");stopSaved=new Saved(game,owner);
        try{stopShot=game.items().babylon().emit(owner,3,1);require(stopShot!=null,"native arrow staged without touching nested Void/barrier hands");stopShot.setPos(owner.getEyePosition().add(0,4,0));stopShot.setDeltaMovement(Vec3.ZERO);stopShot.setNoGravity(true);stopShot.entityTags().add("time_stopped");stopTarget=new Cow(EntityTypes.COW,owner.level());stopTarget.setPos(owner.position().add(0,4,0));stopTarget.setNoAi(true);stopTarget.setNoGravity(true);stopTarget.entityTags().add("time_stopped");owner.level().addFreshEntity(stopTarget);require(game.items().babylonEffects().accept(stopTarget,stopShot,new MobEffectInstance(MobEffects.RAID_OMEN,2,109))&&game.items().babylonEffects().queued(stopTarget,109)==1,"deferred owned contribution before actual stop");stopEpoch=game.items().epoch();}
        catch(RuntimeException|Error failure){restoreStop(game);throw failure;}SkyWars2.LOGGER.info("[check] PASS babylon-stop-probe");
    }
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().babylon().count()==0&&stopShot.isRemoved()&&game.items().babylonEffects().pendingCount()==0&&game.items().babylonEffects().queued(stopTarget,109)==0,"actual match stop clears native projectile, deferred queue and epoch");}finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS babylon-cleanup");}
    private static void restoreStop(GameController game){if(stopSaved==null)return;if(stopShot!=null)game.items().babylon().unloaded(stopShot);if(stopTarget!=null){game.items().babylonEffects().clear(stopTarget);game.items().indirectKills().clear(stopTarget);stopTarget.discard();}stopSaved.restore(game);game.players().save();game.items().magic().flushCleansing();stopSaved=null;stopShot=null;stopTarget=null;stopEpoch=null;}
}
