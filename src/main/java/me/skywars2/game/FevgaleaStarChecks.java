package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Standalone native Fevgalea item, four input branches and accepted owner-damage hook checks. */
public final class FevgaleaStarChecks {
    private FevgaleaStarChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Fevgalea Star: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-4,why+" actual="+actual+" expected="+expected);}
    private static void text(Component component,String key,int color){
        require(component!=null&&component.getContents() instanceof TranslatableContents translated&&translated.getKey().equals(key)
                &&component.getStyle().getColor()!=null&&component.getStyle().getColor().getValue()==color&&!component.getStyle().isItalic(),"source text/style "+key);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final LbcFevgaleaStar sword;final SauvojenStars stars;
        final SauvojenMeditationChecks.Attribution attribution;
        final List<net.minecraft.world.entity.LivingEntity> mobs=new ArrayList<>();
        final Map<java.lang.reflect.Field,Object> combatTickers=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> customStats=new LinkedHashMap<>();
        final int savedCharge,savedVoid,used,skill;final boolean offhand;boolean retain;
        Fixture(GameController game){
            this.game=game;sword=game.items().fevgaleaStar();stars=game.items().sauvojen().stars();
            require(stars.count()==0,"exclusive actual native stars service");base=new ElementalGrimoireChecks.Fixture(game);
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(base.owner));
            savedCharge=counter();savedVoid=base.mana.score(base.owner,"void_resistance").get();used=base.owner.getStats().getValue(Stats.ITEM_USED.get(LbcFevgaleaStar.SWORD));
            skill=game.players().get(base.owner.getUUID()).lbcSkill;offhand=game.config().offhandWands;
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);customStats.put(stat,base.owner.getStats().getValue(stat));}
            try{for(var name:List.of("attackStrengthTicker","itemSwapTicker")){
                Class<?> type=base.owner.getClass();java.lang.reflect.Field field=null;
                while(type!=null&&field==null){try{field=type.getDeclaredField(name);}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}
                if(field==null)throw new NoSuchFieldException(name);field.setAccessible(true);combatTickers.put(field,field.get(base.owner));
            }}catch(ReflectiveOperationException error){throw new IllegalStateException("Fevgalea native melee snapshot",error);}
            base.initialize();base.owner.removeAllEffects();game.players().get(base.owner.getUUID()).lbcSkill=0;
            base.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);base.owner.setHealth(200);base.owner.setAbsorptionAmount(0);
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))base.owner.setItemSlot(slot,ItemStack.EMPTY);
            base.mana.inventoryChanged(base.owner);charge(0);base.mana.score(base.owner,"void_resistance").set(0);held(InteractionHand.MAIN_HAND);
        }
        int counter(){return base.mana.score(base.owner,"fevgalea_charge").get();}
        void charge(int value){base.mana.score(base.owner,"fevgalea_charge").set(value);require(sword.charges(base.owner)==value,"native score is live charge authority");}
        ItemStack held(InteractionHand hand){
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var result=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/fevgalea_star");
            require(result.size()==1&&result.getFirst().is(LbcFevgaleaStar.SWORD),"ported standalone sword loot");
            var stack=result.getFirst();base.owner.setItemInHand(hand,stack);return stack;
        }
        boolean input(InteractionHand hand){return base.owner.gameMode.useItem(base.owner,base.owner.level(),base.owner.getItemInHand(hand),hand).consumesAction();}
        void clear(){stars.forget(base.owner);for(var mob:mobs){game.items().indirectKills().clear(mob);mob.discard();}mobs.clear();base.recording.packets.clear();}
        Cow cow(Vec3 point,float health){
            var mob=new Cow(EntityTypes.COW,base.owner.level());mob.setPos(point);mob.setNoAi(true);mob.setNoGravity(true);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);mob.setHealth(health);
            require(base.owner.level().addFreshEntity(mob),"native Fevgalea cow");mobs.add(mob);return mob;
        }
        Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancementSnapshot(){
            var saved=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,Set<String>>();
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){
                var completed=new HashSet<String>();base.owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(completed::add);saved.put(advancement,completed);
            }return saved;
        }
        void restoreAdvancements(Map<net.minecraft.advancements.AdvancementHolder,Set<String>> saved){
            saved.forEach((advancement,completed)->{for(var key:advancement.value().criteria().keySet())
                if(completed.contains(key))base.owner.getAdvancements().award(advancement,key);else base.owner.getAdvancements().revoke(advancement,key);});
        }
        void presentation(){
            var stack=held(InteractionHand.MAIN_HAND);require(LbcFevgaleaStar.isSword(stack)&&stack.getMaxDamage()==4000,"source sword identity/durability4000");
            require(Identifier.parse("lbc:fevgalea_star").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_x").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source model/tierX");
            text(stack.get(DataComponents.CUSTOM_NAME),"fevgalea_star",0x55ff55);var lore=stack.get(DataComponents.LORE);
            require(lore!=null&&lore.lines().size()==12,"type line plus eleven source lore lines");
            for(int i=1;i<=11;i++)text(lore.lines().get(i),"fevgalea_star.lore."+i,0xffffff);
            var flags=stack.get(DataComponents.CUSTOM_DATA).copyTag();for(var flag:List.of("swordwithability","transformable","sword","magicwand","fevgalea_star"))require(flags.getIntOr(flag,0)==1,"source flag "+flag);
            var modifiers=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            require(modifiers.size()==2,"two authored native weapon attributes");
            require(modifiers.stream().anyMatch(e->e.attribute().equals(Attributes.ATTACK_DAMAGE)&&e.modifier().id().equals(Identifier.parse("minecraft:base_attack_damage"))&&e.modifier().amount()==16
                    &&e.slot()==net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND&&e.modifier().operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),"source mainhand damage16");
            require(modifiers.stream().anyMatch(e->e.attribute().equals(Attributes.ATTACK_SPEED)&&e.modifier().id().equals(Identifier.parse("minecraft:base_attack_speed"))&&e.modifier().amount()==-2.2
                    &&e.slot()==net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND&&e.modifier().operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),"source mainhand speed-2.2");
        }
        void inputs(){
            var owner=base.owner;held(InteractionHand.MAIN_HAND);owner.setShiftKeyDown(false);charge(0);
            base.mana.set(owner,999);require(!input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==999&&stars.count()==0,"rain rejects below1000 without payment");
            base.mana.set(owner,1000);require(input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==0&&counter()==0,"uncharged normal spends1000 only");
            var rain=stars.snapshot().stream().filter(v->v.kind()==SauvojenStars.Kind.RAIN).findFirst().orElseThrow();
            near(rain.position().distanceToSqr(owner.getEyePosition().add(0,15,0)),0,"rain starts exact local up15 at pitch0");
            require(rain.owner()==owner&&rain.lifetime()==100&&rain.age()==0,"real native owner/lifetime100 rain");
            stars.tick();near(rain.carrier().position().distanceToSqr(rain.position().add(owner.getLookAngle())),0,"source rain moves1 native block per tick");clear();
            owner.setShiftKeyDown(true);owner.setHealth(25);owner.removeAllEffects();base.mana.set(owner,4999);
            require(!input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==4999&&owner.getHealth()==25,"heal rejects below5000");
            int beforeVoid=base.mana.score(owner,"void_resistance").get();base.mana.set(owner,5000);
            require(input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==0&&counter()==0,"uncharged sneak spends5000");
            var healing=owner.getEffect(MobEffects.INSTANT_HEALTH);require(healing!=null&&healing.getAmplifier()==99&&healing.getDuration()==1,
                    "source instantaneous effect command duration1 means one native tick, amplifier99");
            healing.tickServer(owner.level(),owner,()->{});
            require(owner.getHealth()==owner.getMaxHealth()&&owner.getEffect(MobEffects.RESISTANCE)!=null&&owner.getEffect(MobEffects.RESISTANCE).getAmplifier()==4
                    &&owner.getEffect(MobEffects.RESISTANCE).getDuration()==140,"source native full healing and resistanceV7seconds");
            require(base.mana.score(owner,"void_resistance").get()==beforeVoid+7,"heal adds actual source void counter7");
            owner.removeAllEffects();base.mana.score(owner,"void_resistance").set(0);owner.setShiftKeyDown(false);owner.setHealth(17.8f);charge(1);base.mana.set(owner,-200);
            var beamVictim=cow(owner.position().add(0,0,6),200);int contacts=0;
            for(int sample=0;sample<=120;sample++)if(beamVictim.position().distanceToSqr(owner.getEyePosition().add(owner.getLookAngle().scale(sample)))<=16)contacts++;
            require(contacts==7,"source one-block ray samples intersect radius4 seven times at this native geometry");
            require(input(InteractionHand.MAIN_HAND)&&counter()==0&&base.mana.get(owner)==-200,"charged normal consumes1 free of mana admission");
            near(beamVictim.getHealth(),200-contacts*Math.floor(17.8f),"source outside_border bypasses cooldown for every geometric contact sample");
            require(beamVictim.getLastHurtByPlayer()==owner,"playerHealth ray retains native caster credit");clear();
            owner.setShiftKeyDown(true);charge(7);base.mana.set(owner,-300);var half=cow(owner.position().add(0,0,6),100);half.setHealth(25.5f);
            require(input(InteractionHand.MAIN_HAND)&&counter()==0&&base.mana.get(owner)==-300,"charged sneak consumes7 free of mana admission");
            near(half.getHealth(),13.5,"charged area floors half CURRENT health25.5 to12 damage");require(half.getLastHurtByPlayer()==owner,"charged area preserves player author");clear();
            charge(6);base.mana.set(owner,4999);require(!input(InteractionHand.MAIN_HAND)&&counter()==6&&base.mana.get(owner)==4999,"six charges cannot buy charged area or bypass heal mana");
            owner.setShiftKeyDown(false);charge(0);owner.setHealth(200);
        }
        void acceptedDamage(){
            var owner=base.owner;held(InteractionHand.MAIN_HAND);charge(0);owner.setShiftKeyDown(false);
            var target=new Zombie(EntityTypes.ZOMBIE,owner.level());target.setPos(owner.position().add(0,.5,2));target.setNoAi(true);target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);target.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.9);target.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.317);
            require(owner.level().addFreshEntity(target),"actual native melee victim");mobs.add(target);
            try{for(var field:combatTickers.keySet())field.setInt(owner,200);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            owner.setOnGround(true);owner.setSprinting(false);float health=target.getHealth();owner.attack(target);
            require(target.getHealth()<health&&counter()==1,"actual accepted Player.attack adds exactly one charge");
            var actor=stars.snapshot().stream().filter(v->v.kind()==SauvojenStars.Kind.CHARGE).findFirst().orElseThrow();
            require(actor.owner()==owner&&actor.flavour()>=1&&actor.flavour()<=3&&actor.lifetime()==40,"actual melee emits one source random40-tick charge");
            near(actor.position().distanceToSqr(owner.getEyePosition()),0,"melee projectile at actual player eye origin");
            target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setHealth(200);float damage=SauvojenStars.chargeDamage(target,actor.flavour());stars.tick();
            require(target.getHealth()<200&&counter()==1,"own actual star damage is accepted without recursively granting charges");near(target.getHealth(),200-damage,"exact native selected-flavour projectile formula");clear();
            var victim=cow(owner.position().add(0,0,5),100);var type=owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
            charge(0);require(victim.hurtServer(owner.level(),new DamageSource(type.getOrThrow(DamageTypes.MAGIC),owner),2),"native external non-arrow owner damage accepted");
            require(counter()==1&&stars.snapshot().stream().filter(v->v.kind()==SauvojenStars.Kind.CHARGE).count()==1,"source any accepted owner non-arrow hit charges once");
            clear();victim=cow(owner.position().add(0,0,5),100);charge(0);
            require(victim.hurtServer(owner.level(),new DamageSource(type.getOrThrow(DamageTypes.ARROW),owner),2),"native external arrow control accepted");
            require(counter()==0&&stars.count()==0,"source arrow-only attribution grants no charge");
            victim.setPermanentlyInvulnerable(true);victim.hurtServer(owner.level(),new DamageSource(type.getOrThrow(DamageTypes.MAGIC),owner),5);
            require(counter()==0&&stars.count()==0,"rejected native hurt grants no charge");clear();
            var fatal=cow(owner.position().add(0,0,5),2);charge(0);
            int kills=owner.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));var advancementState=advancementSnapshot();
            var unlocks=new HashSet<>(game.players().get(owner.getUUID()).lbcUnlocks);boolean drops=owner.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS);
            try{
                owner.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,false,game.server());
                require(fatal.hurtServer(owner.level(),new DamageSource(type.getOrThrow(DamageTypes.MAGIC),owner),3)&&fatal.isDeadOrDying(),"actual accepted non-arrow native fatal damage");
                require(counter()==1&&stars.snapshot().stream().filter(v->v.kind()==SauvojenStars.Kind.CHARGE).count()==1,"fatal accepted callback grants exactly one charge and source actor");
                require(fatal.getKillCredit()==owner,"fatal non-arrow retains real native owner attribution");
            }finally{
                owner.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,drops,game.server());
                owner.getStats().setValue(owner,Stats.ENTITY_KILLED.get(EntityTypes.COW),kills);restoreAdvancements(advancementState);
                game.players().get(owner.getUUID()).lbcUnlocks.clear();game.players().get(owner.getUUID()).lbcUnlocks.addAll(unlocks);clear();
            }
        }
        void independentOffhand(){
            var owner=base.owner;require(game.items().ruby().count()==0,"exclusive offhand native Ruby control");
            var cost=base.mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("tiny_ruby_wand_mana_use"),"lbc.math");int previousCost=cost.get();
            int stat=owner.getStats().getValue(Stats.ITEM_USED.get(LbcWands.TINY_RUBY));boolean configured=game.config().offhandWands;
            var advancementState=advancementSnapshot();var unlocks=new HashSet<>(game.players().get(owner.getUUID()).lbcUnlocks);
            try{
                cost.set(500);game.config().offhandWands=true;held(InteractionHand.MAIN_HAND);owner.setShiftKeyDown(false);charge(0);
                var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/tiny_ruby_wand");
                require(loot.size()==1&&loot.getFirst().is(LbcWands.TINY_RUBY),"actual offhand tiny Ruby source item");owner.setItemInHand(InteractionHand.OFF_HAND,loot.getFirst());
                base.recording.packets.clear();base.mana.set(owner,500);require(input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==0&&counter()==0
                        &&stars.count()==0&&base.recording.packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket),
                        "insufficient main Fevgalea mana still independently casts affordable offhand native tiny-Ruby ray500");
                clear();base.mana.set(owner,1500);
                require(input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==0&&counter()==0
                        &&stars.snapshot().stream().filter(v->v.kind()==SauvojenStars.Kind.RAIN).count()==1
                        &&base.recording.packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket),
                        "same native use produces mainrain and offhandray, independently debiting1000+500");
            }finally{
                clear();cost.set(previousCost);game.config().offhandWands=configured;
                owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.getStats().setValue(owner,Stats.ITEM_USED.get(LbcWands.TINY_RUBY),stat);
                restoreAdvancements(advancementState);game.players().get(owner.getUUID()).lbcUnlocks.clear();game.players().get(owner.getUUID()).lbcUnlocks.addAll(unlocks);
            }
        }
        void guardsAndBonuses(){
            var owner=base.owner;held(InteractionHand.OFF_HAND);game.config().offhandWands=true;owner.setShiftKeyDown(false);charge(7);base.mana.set(owner,10000);
            require(!input(InteractionHand.OFF_HAND)&&counter()==7&&base.mana.get(owner)==10000&&stars.count()==0,"offhand ability rejected even with enabled offhand wands");
            held(InteractionHand.MAIN_HAND);charge(0);base.mana.set(owner,5000);owner.entityTags().add("time_stopped");
            try{require(!input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==5000&&stars.count()==0,"owner TimeStop rejects before payment");}finally{owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(owner.level(),40);
            try{require(!input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==5000,"global TimeStop rejects before payment");}finally{game.items().timeStop().end(owner.level());}
            base.field=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());base.field.setPos(owner.position());base.field.entityTags().add("stopper_magic");
            require(owner.level().addFreshEntity(base.field),"native antimagic fixture");game.items().magic().loaded(base.field);
            try{require(!input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==5000,"native antimagic rejects before payment");}finally{base.removeField();}
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                var armor=new ItemStack(Items.LEATHER_HELMET);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("mage",1);armor.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));owner.setItemSlot(slot,armor);
            }base.mana.inventoryChanged(owner);require(base.mana.mageFullSet(owner),"actual Mage full-set control");int maximum=base.mana.maximum(owner);
            var bonus=base.mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("mage_set_bonus"),"lbc.math");int oldBonus=bonus.get();
            try{bonus.set(137);charge(0);base.mana.set(owner,1000);require(input(InteractionHand.MAIN_HAND)&&base.mana.maximum(owner)==maximum,"standalone sword does not invoke wand Mage cast bonus137");}
            finally{bonus.set(oldBonus);}clear();
            ItemStack[] overload=new ItemStack[4];int piece=0;
            for(var leaf:List.of("overload_helmet","overload_chestplate","overload_leggings","overload_boots")){
                var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+leaf);
                require(loot.size()==1&&loot.getFirst().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("overload",0)==1,
                        "real migrated Overload armor flag "+leaf);overload[piece++]=loot.getFirst();
            }
            int index=0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))owner.setItemSlot(slot,overload[index++]);base.mana.inventoryChanged(owner);
            require(base.mana.overloadArmor(owner)==15,"actual full Overload control");charge(0);base.mana.set(owner,0);
            require(input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==-1000,"approved Overload bypass retains actual1000 overdraft debit");clear();
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))owner.setItemSlot(slot,ItemStack.EMPTY);base.mana.inventoryChanged(owner);
        }
        void lifecycle(){
            held(InteractionHand.MAIN_HAND);base.owner.setShiftKeyDown(false);charge(0);base.mana.set(base.owner,1000);require(input(InteractionHand.MAIN_HAND),"native lifecycle rain");
            var actor=stars.snapshot().getFirst();game.items().timeStop().begin(base.owner.level(),40);
            try{stars.tick();require(stars.snapshot().stream().anyMatch(a->a.carrier()==actor.carrier()&&a.age()==actor.age()&&a.position().equals(actor.position())),"TimeStop holds actual rain geometry/age");}
            finally{game.items().timeStop().end(base.owner.level());}
            charge(9);game.items().forget(base.owner);require(sword.charges(base.owner)==0&&stars.count()==0,"actual shared owner-forget resets source charge and exact native actors");
            charge(8);game.items().clear(base.owner);require(counter()==0,"actual shared full purify clears source counter");
        }
        void friendlyPlayers(){
            var owner=base.owner;var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null,"actual allied player fixture");
            var saved=new GameChecks.WandThirdPlayerState(other,base.mana);var indirect=new SauvojenMeditationChecks.Attribution(game,List.of(other));
            var board=game.server().getScoreboard();var ownerTeam=owner.getTeam();var otherTeam=other.getTeam();float oldHealth=owner.getHealth();
            require(board.getPlayerTeam("sw2_fev_safe")==null,"exclusive source sword team");var team=board.addPlayerTeam("sw2_fev_safe");team.setAllowFriendlyFire(false);
            try{
                board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
                other.getInventory().clearContent();other.removeAllEffects();other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);other.getAbilities().invulnerable=false;
                other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);other.setHealth(100);other.setAbsorptionAmount(0);other.setNoGravity(true);other.setInvulnerableTime(0);other.damageCooldownTime=0;
                other.teleportTo(other.level(),owner.getX(),owner.getY(),owner.getZ()+6,Set.of(),0,0,true);owner.setHealth(1);
                held(InteractionHand.MAIN_HAND);owner.setShiftKeyDown(false);charge(1);base.mana.set(owner,-100);
                require(input(InteractionHand.MAIN_HAND)&&other.getHealth()==100&&counter()==0,"native playerHealth beam preserves approved allied-player protection");
                other.setHealth(25.5f);charge(7);owner.setShiftKeyDown(true);
                require(input(InteractionHand.MAIN_HAND)&&other.getHealth()==25.5f&&counter()==0,"native half-health area preserves approved allied-player protection");
            }finally{
                board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(otherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),otherTeam);
                owner.setHealth(oldHealth);owner.setShiftKeyDown(false);try{saved.restore(game);}finally{indirect.close();}
            }
        }
        @Override public void close(){
            try{
                if(!retain){clear();sword.forget(base.owner);charge(savedCharge);}else base.retain=true;
                base.mana.score(base.owner,"void_resistance").set(savedVoid);base.owner.getStats().setValue(base.owner,Stats.ITEM_USED.get(LbcFevgaleaStar.SWORD),used);
                customStats.forEach((stat,value)->base.owner.getStats().setValue(base.owner,stat,value));game.players().get(base.owner.getUUID()).lbcSkill=skill;game.config().offhandWands=offhand;
                for(var entry:combatTickers.entrySet())entry.getKey().set(base.owner,entry.getValue());
            }catch(IllegalAccessException error){throw new IllegalStateException("Fevgalea melee restore",error);}
            finally{try{base.close();}finally{attribution.close();}}
        }
    }
    private static List<SauvojenStars.View> retained;private static String retainedEpoch;private static ServerPlayer retainedOwner;
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.presentation();fixture.inputs();fixture.acceptedDamage();fixture.independentOffhand();fixture.friendlyPlayers();fixture.guardsAndBonuses();fixture.lifecycle();}
        SkyWars2.LOGGER.info("[check] PASS fevgalea-star (authored sword, four native inputs/exact costs, real melee/non-arrow charge hook, self-payload recursion guard, source health formulas, Mage/Overload, pause/purify/lifetime)");
    }
    public static void stopProbe(GameController game){
        require(retained==null,"exclusive actual-stop probe");try(var fixture=new Fixture(game)){
            fixture.held(InteractionHand.MAIN_HAND);fixture.base.owner.setShiftKeyDown(false);fixture.charge(0);fixture.base.mana.set(fixture.base.owner,1000);
            require(fixture.input(InteractionHand.MAIN_HAND),"retain actual rain");fixture.charge(7);
            retained=fixture.stars.snapshot();require(!retained.isEmpty()&&retained.stream().noneMatch(a->a.carrier().isRemoved()),"live native actors retained before actual stop");
            retainedEpoch=game.items().epoch();retainedOwner=fixture.base.owner;fixture.retain=true;
        }SkyWars2.LOGGER.info("[check] PASS fevgalea-star-stop-probe");
    }
    public static void cleanup(GameController game){
        require(retained!=null,"actual stop probe exists");try{
            require(game.inLobby()&&!game.items().currentEpoch(retainedEpoch)&&game.items().sauvojen().stars().count()==0
                    &&game.items().fevgaleaStar().charges(retainedOwner)==0&&retained.stream().allMatch(a->a.carrier().isRemoved()),"actual match stop clears source actors/charges/epoch");
        }finally{for(var actor:retained)actor.carrier().discard();retained=null;retainedEpoch=null;retainedOwner=null;}
        SkyWars2.LOGGER.info("[check] PASS fevgalea-star-cleanup");
    }
}
