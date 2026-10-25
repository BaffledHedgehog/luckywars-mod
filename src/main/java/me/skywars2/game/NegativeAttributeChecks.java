package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.util.*;

/** Real native attack, fall and underwater base-tick paths; explicit synchronous boundaries. */
public final class NegativeAttributeChecks {
    private NegativeAttributeChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Negative attributes: "+message);}
    private static Field fieldInHierarchy(Class<?> type,String name)throws NoSuchFieldException{
        for(Class<?> current=type;current!=null;current=current.getSuperclass())try{var field=current.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(type.getName()+" hierarchy: "+name);
    }
    private static boolean clean(GameController game,ServerPlayer player){
        try{for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){field.setAccessible(true);if(((Map<?,?>)field.get(game.items().indirectKills())).containsKey(player))return false;}}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}return true;
    }
    private record AttributeState(double base,List<AttributeModifier> modifiers,Set<Identifier> permanent){
        AttributeState(AttributeInstance attribute){this(attribute.getBaseValue(),List.copyOf(attribute.getModifiers()),attribute.getPermanentModifiers().stream().map(AttributeModifier::id).collect(java.util.stream.Collectors.toSet()));}
        void restore(AttributeInstance attribute){attribute.removeModifiers();attribute.setBaseValue(base);for(var modifier:modifiers)if(permanent.contains(modifier.id()))attribute.addPermanentModifier(modifier);else attribute.addTransientModifier(modifier);}
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;
        final Map<Holder<Attribute>,AttributeState> attributes=new LinkedHashMap<>();
        final Map<Field,Object> fields=new LinkedHashMap<>(),profileFields=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;final int fallStat;final PlayerStore.Profile profile;
        Saved(GameController game,ServerPlayer player){
            this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());team=player.getTeam();profile=game.players().get(player.getUUID());
            for(var type:List.of(Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.SAFE_FALL_DISTANCE,Attributes.OXYGEN_BONUS,Attributes.ENTITY_INTERACTION_RANGE))attributes.put(type,new AttributeState(player.getAttribute(type)));
            fallStat=player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FALL_ONE_CM));
            try{
                for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp","lastHurt","lastDamageSource","lastDamageStamp","hurtTime","hurtDuration")){var field=LivingEntity.class.getDeclaredField(name);field.setAccessible(true);fields.put(field,field.get(player));}
                for(String name:List.of("attackStrengthTicker","itemSwapTicker")){var field=fieldInHierarchy(player.getClass(),name);fields.put(field,field.get(player));}
                for(var field:profile.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){var value=field.get(profile);if(value instanceof int[] array)value=array.clone();else if(value instanceof Set<?> set)value=new HashSet<>(set);profileFields.put(field,value);}
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void restore(GameController game){
            state.restore(game);attributes.forEach((type,value)->value.restore(player.getAttribute(type)));
            var board=game.server().getScoreboard();if(player.getTeam()!=null)board.removePlayerFromTeam(player.getScoreboardName(),player.getTeam());if(team!=null)board.addPlayerToTeam(player.getScoreboardName(),team);
            try{for(var entry:fields.entrySet())entry.getKey().set(player,entry.getValue());for(var entry:profileFields.entrySet())entry.getKey().set(profile,entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            player.getStats().setValue(player,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FALL_ONE_CM),fallStat);
            game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;final Saved saved,otherSaved;final Vec3 at;
        final Map<BlockPos,MapManager.BlockSnapshot> cells=new LinkedHashMap<>();final List<LivingEntity> actors=new ArrayList<>();
        final net.minecraft.world.scores.PlayerTeam team;final boolean ranked,skills;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.allowDamage(owner)&&game.allowDamage(other),"two active native match participants");
            for(var player:List.of(owner,other))require(player.containerMenu==player.inventoryMenu&&clean(game,player)&&!game.items().actionsBlocked(player),"fixture owns clean menu/actions/combat boundary");
            require(!game.items().timeStop().stopped(owner.level()),"fixture starts outside Time Stop");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);at=Vec3.atCenterOf(owner.blockPosition().offset(0,24,0));
            ranked=game.config().ranked;skills=game.config().lbcSkills;team=game.server().getScoreboard().addPlayerTeam("negative_attribute_fixture");team.setAllowFriendlyFire(false);
        }
        void set(ServerPlayer player,Holder<Attribute> type,double value){var attribute=player.getAttribute(type);attribute.removeModifiers();attribute.setBaseValue(value);require(attribute.getValue()==value,"signed attribute reaches actual native value "+type+"="+value);}
        void move(ServerPlayer player,Vec3 point){player.teleportTo(player.level(),point.x,point.y,point.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        void initialize(){
            var center=BlockPos.containing(at);for(var pos:BlockPos.betweenClosed(center.offset(-2,-1,-2),center.offset(8,4,8))){require(owner.level().hasChunkAt(pos),"resident native fixture area");cells.put(pos.immutable(),game.maps().snapshot(owner.level(),pos));}
            for(var pos:cells.keySet())owner.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            game.config().ranked=false;game.config().lbcSkills=false;
            for(var player:List.of(owner,other)){
                player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setShiftKeyDown(false);player.setRemainingFireTicks(0);player.setInvulnerableTime(0);player.damageCooldownTime=0;player.hurtTime=0;player.hurtDuration=0;player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);player.setHealth(20);player.setAbsorptionAmount(0);player.fallDistance=0;
                player.entityTags().remove("sin_greed_active");player.entityTags().remove("time_stopped");
                var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().mana().noMagic(player,0);game.items().magic().inventoryChanged(player);
            }
            move(owner,at);move(other,at.add(7,0,0));game.items().magic().flushCleansing();
            set(owner,Attributes.ATTACK_DAMAGE,-10);set(owner,Attributes.ATTACK_SPEED,4);set(owner,Attributes.ENTITY_INTERACTION_RANGE,3);set(owner,Attributes.SAFE_FALL_DISTANCE,3);
        }
        Cow cow(Vec3 point){var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(point);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);cow.setHealth(5);owner.level().addFreshEntity(cow);actors.add(cow);return cow;}
        void ready(){try{fieldInHierarchy(owner.getClass(),"attackStrengthTicker").setInt(owner,200);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}owner.fallDistance=0;}
        void playerMelee(){
            var target=cow(at.add(0,0,1.5));ready();owner.attack(target);
            require(target.getHealth()==15&&target.hurtTime==0&&target.getLastHurtByMob()==null&&target.getDeltaMovement().lengthSqr()==0,"real player melee heals scaled negative attack without hurt/credit/knockback: hp="+target.getHealth()+" max="+target.getMaxHealth()+" hurt="+target.hurtTime+" credit="+target.getLastHurtByMob()+" velocity="+target.getDeltaMovement()+" currentStrength="+owner.getAttackStrengthScale(.5f));
            require(owner.getAttackStrengthScale(.5f)<.9,"healing consumes native attack cooldown exactly once");
            target.setHealth(19);ready();owner.attack(target);require(target.getHealth()==20,"negative attack healing caps at actual maximum health");
            target.entityTags().add("strict_map_object");target.setHealth(5);ready();owner.attack(target);require(target.getHealth()==5,"technical role rejects negative melee support");target.entityTags().remove("strict_map_object");
            target.entityTags().add("sin_greed_active");ready();owner.attack(target);require(target.getHealth()==5,"active Greed rejects external negative melee support");target.entityTags().remove("sin_greed_active");
            target.setPos(at.add(0,0,7));ready();owner.attack(target);require(target.getHealth()==5,"direct native caller cannot heal beyond native melee reach");target.setPos(at.add(0,0,1.5));
            owner.entityTags().add("time_stopped");ready();owner.attack(target);require(target.getHealth()==5,"Time Stop rejects negative attack support");owner.entityTags().remove("time_stopped");
            var board=game.server().getScoreboard();board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);move(other,at.add(0,0,1.5));other.setHealth(5);ready();owner.attack(other);require(other.getHealth()==15&&other.hurtTime==0,"allied player receives healing without hostile damage path");move(other,at.add(7,0,0));
            var undead=new Zombie(EntityTypes.ZOMBIE,owner.level());undead.setPos(at.add(0,0,1.5));undead.setNoAi(true);undead.setNoGravity(true);undead.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);undead.setHealth(5);owner.level().addFreshEntity(undead);actors.add(undead);target.discard();ready();owner.attack(undead);require(undead.getHealth()==15&&undead.hurtTime==0,"undead receives direct healing without instant-effect inversion");undead.discard();
            var mobileTarget=cow(at.add(5,0,1));var healer=new Zombie(EntityTypes.ZOMBIE,owner.level());healer.setPos(at.add(5,0,0));healer.setNoAi(true);healer.setNoGravity(true);healer.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(-6);owner.level().addFreshEntity(healer);actors.add(healer);
            require(healer.doHurtTarget(owner.level(),mobileTarget)&&mobileTarget.getHealth()==11&&mobileTarget.hurtTime==0&&mobileTarget.getLastHurtByMob()==null,"actual native mob melee heals its final negative damage amount without hurt side effects");healer.discard();mobileTarget.discard();
        }
        void directMeleeFallback(){
            var slot=net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            var target=cow(at.add(0,0,1.5));ready();
            require(!owner.stabAttack(slot,target,-10,true,false,false)&&target.getHealth()==15&&target.hurtTime==0&&target.getLastHurtByMob()==null,"actual Player.stabAttack heals signed final damage without accepting a native hurt");
            move(other,at.add(0,0,1.5));other.setHealth(5);other.hurtTime=0;ready();
            require(!owner.stabAttack(slot,other,-10,true,false,false)&&other.getHealth()==15&&other.hurtTime==0,"ServerPlayer direct fallback heals once despite player and living hurt overrides");move(other,at.add(7,0,0));target.discard();
            target=cow(at.add(0,0,3));owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.WOODEN_SPEAR));ready();
            require(owner.getMainHandItem().getDamageSource(owner).is(net.minecraft.world.damagesource.DamageTypes.SPEAR)&&!owner.stabAttack(slot,target,-10,true,false,false)&&target.getHealth()==15&&target.hurtTime==0,"actual native spear damage type reaches signed stab healing at its own weapon reach");owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);target.discard();
            target=cow(at.add(5,0,1));var mob=new Zombie(EntityTypes.ZOMBIE,owner.level());mob.setPos(at.add(5,0,0));mob.setNoAi(true);mob.setNoGravity(true);mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(-6);owner.level().addFreshEntity(mob);actors.add(mob);
            require(!mob.stabAttack(slot,target,-6,true,false,false)&&target.getHealth()==11&&target.hurtTime==0&&target.getLastHurtByMob()==null,"actual inherited LivingEntity.stabAttack heals signed mob melee without hurt side effects");mob.discard();
            target.setHealth(5);var golem=new net.minecraft.world.entity.animal.golem.IronGolem(EntityTypes.IRON_GOLEM,owner.level());golem.setPos(at.add(5,0,0));golem.setNoAi(true);golem.setNoGravity(true);golem.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(-5);owner.level().addFreshEntity(golem);actors.add(golem);
            require(!golem.doHurtTarget(owner.level(),target)&&target.getHealth()==10&&target.hurtTime==0&&target.getDeltaMovement().lengthSqr()==0,"custom IronGolem attack override heals negative native melee without uppercut/hurt effects");golem.discard();
            target.setHealth(5);var bee=new net.minecraft.world.entity.animal.bee.Bee(EntityTypes.BEE,owner.level());bee.setPos(at.add(5,0,0));bee.setNoAi(true);bee.setNoGravity(true);bee.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(-4);owner.level().addFreshEntity(bee);actors.add(bee);
            require(!bee.doHurtTarget(owner.level(),target)&&target.getHealth()==9&&target.hurtTime==0&&!target.hasEffect(MobEffects.POISON)&&!bee.hasStung(),"native STING override heals negative melee without poison or fake successful sting");bee.discard();
            target.setHealth(5);var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,owner.level());arrow.setOwner(owner);
            target.hurtServer(owner.level(),target.damageSources().arrow(arrow,owner),-4);require(target.getHealth()==5,"negative projectile damage remains outside direct-melee healing");
            target.setInvulnerableTime(0);target.damageCooldownTime=0;target.hurtServer(owner.level(),target.damageSources().indirectMagic(owner,owner),-4);require(target.getHealth()==5,"negative indirect-magic damage remains outside native melee whitelist");game.items().indirectKills().clear(target);target.discard();
        }
        void speedAndFall(){
            var target=cow(at.add(0,0,1.5));
            for(double speed:new double[]{0,-4,-Double.MAX_VALUE}){
                set(owner,Attributes.ATTACK_SPEED,speed);ready();
                require(!Float.isNaN(owner.getCurrentItemAttackStrengthDelay())&&!Float.isNaN(owner.getAttackStrengthScale(0))&&!Float.isNaN(owner.getItemSwapScale(0)),"zero/negative/extreme attack speed retains defined native cooldown math");
                target.setHealth(5);owner.attack(target);require(Math.abs(target.getHealth()-7)<1e-5,"native signed-speed melee retains weak20percent negative attack scale");require(Double.isFinite(owner.position().x)&&Double.isFinite(owner.getDeltaMovement().lengthSqr()),"signed attack speed never injects NaN into native world state");
            }
            target.discard();set(owner,Attributes.ATTACK_SPEED,4);set(owner,Attributes.FALL_DAMAGE_MULTIPLIER,-1);owner.setHealth(5);owner.hurtTime=0;owner.damageCooldownTime=0;
            boolean hurt=owner.causeFallDamage(10,1,owner.damageSources().fall());require(!hurt&&owner.getHealth()==13&&owner.hurtTime==0,"one native fall heals abs(floor(signed native fall product)) exactly once without fake damage");
            owner.setHealth(5);owner.causeFallDamage(1,1,owner.damageSources().fall());require(owner.getHealth()==5,"short safe fall neither heals nor becomes damage from two negative terms");
            owner.setHealth(19);owner.causeFallDamage(100,1,owner.damageSources().fall());require(owner.getHealth()==20,"negative fall healing caps at actual maximum health");
        }
        void oxygen(){
            var center=BlockPos.containing(at);for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,3,1)))owner.level().setBlock(pos,Blocks.WATER.defaultBlockState(),2);
            move(owner,at);set(owner,Attributes.OXYGEN_BONUS,0);owner.setAirSupply(100);owner.baseTick();require(owner.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)&&owner.getAirSupply()==99,"real underwater native tick retains ordinary one-air loss at zero bonus");
            set(owner,Attributes.OXYGEN_BONUS,-3);owner.setAirSupply(100);owner.baseTick();require(owner.getAirSupply()==96,"real underwater native tick spends1+abs(negative oxygen bonus)");
            set(owner,Attributes.OXYGEN_BONUS,-.5);int sum=0;for(int i=0;i<64;i++){owner.setAirSupply(100);owner.baseTick();int loss=100-owner.getAirSupply();require(loss==1||loss==2,"fractional negative oxygen uses bounded Bernoulli loss");sum+=loss;}require(sum>64&&sum<128,"fractional negative oxygen has faster expected depletion without integer truncation");
            set(owner,Attributes.OXYGEN_BONUS,-Double.MAX_VALUE);owner.setHealth(20);owner.setAirSupply(100);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.baseTick();require(owner.getAirSupply()==0&&owner.getHealth()==18&&owner.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.DROWN),"extreme negative oxygen saturates safely and invokes one native drowning consequence");
        }
        public void close(){
            for(var actor:actors){game.items().indirectKills().clear(actor);actor.discard();}
            for(var player:List.of(owner,other)){game.items().shooters().clearStun(player);game.items().indirectKills().clear(player);}
            cells.forEach((pos,state)->game.maps().restoreSnapshot(owner.level(),pos,state));game.config().ranked=ranked;game.config().lbcSkills=skills;saved.restore(game);otherSaved.restore(game);game.server().getScoreboard().removePlayerTeam(team);game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.initialize();fixture.playerMelee();fixture.directMeleeFallback();fixture.speedAndFall();fixture.oxygen();}SkyWars2.LOGGER.info("[check] PASS negative-attributes (real player/mob/stab/custom-override melee healing, native fall healing, underwater native air depletion and signed attack-speed stability; explicit boundaries)");}
}
