package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.mixin.SyringeMobAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import java.util.*;

/** Native equipment, exact reversible stats, special damage/AI, conversions, persistence and pool closure. */
public final class SyringeChecks {
    private SyringeChecks(){ }
    private static void require(boolean b,String why){if(!b)throw new IllegalStateException("Syringe: "+why);}
    private static void near(double a,double b,String why){require(Math.abs(a-b)<1e-5,why+" actual="+a+" expected="+b);}
    private static double arrowDamage(Entity arrow){var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,arrow.registryAccess());arrow.saveWithoutId(out);return out.buildResult().getDoubleOr("damage",-1);}
    private static void reset(AllGrimoireChecks.Fixture f){f.base.game.items().syringe().stop();f.restoreWorld=true;f.reset();f.base.owner.getInventory().clearContent();}
    private static ItemStack loot(AllGrimoireChecks.Fixture f){var items=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:ported/siggewinne_syringe");
        require(items.size()==1&&items.getFirst().is(LbcSyringe.ITEM),"one registered source item");return items.getFirst();}
    private static <T extends Mob>T spawn(AllGrimoireChecks.Fixture f,T m,int distance,boolean own){
        m.setPos(f.at.add(distance,0,0));m.setNoGravity(true);m.setNoAi(true);
        if(own)f.base.game.items().summons().track(f.base.owner,m);
        require(f.level.addFreshEntity(m),"native mob fixture");return m;
    }
    private static Skeleton skeleton(AllGrimoireChecks.Fixture f,int distance,boolean own){var m=spawn(f,new Skeleton(EntityTypes.SKELETON,f.level),distance,own);
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);m.setHealth(10);m.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);m.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.2);return m;}
    private static int power(Mob m){return EnchantmentHelper.getItemEnchantmentLevel(m.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),m.getMainHandItem());}
    private static void power(Mob m,int value){EnchantmentHelper.updateEnchantments(m.getMainHandItem(),v->v.set(m.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),value));}
    private static void equip(AllGrimoireChecks.Fixture f,InteractionHand hand){f.base.owner.setItemInHand(hand,loot(f));f.base.game.items().syringe().refresh();}
    private static void presentation(AllGrimoireChecks.Fixture f){var item=loot(f);var defaults=LbcSyringe.ITEM.getDefaultInstance();
        for(var c:List.of(DataComponents.CUSTOM_NAME,DataComponents.LORE,DataComponents.CUSTOM_DATA,DataComponents.ITEM_MODEL,DataComponents.TOOLTIP_STYLE))require(Objects.equals(item.get(c),defaults.get(c)),"source/default components "+c);
        require(item.getMaxStackSize()==1&&!item.has(DataComponents.MAX_DAMAGE)&&item.get(DataComponents.LORE).lines().size()==7,"source non-durable single syringe and seven lore rows");
    }
    private static void reversible(AllGrimoireChecks.Fixture f){var service=f.base.game.items().syringe();
        for(var hand:InteractionHand.values()){
            reset(f);var a=skeleton(f,1,true);var b=skeleton(f,2,true);var c=skeleton(f,3,true);var far=skeleton(f,4,true);var enemy=skeleton(f,0,false);
            f.base.game.items().summons().track(f.beta,enemy);var npc=skeleton(f,0,true);npc.entityTags().add("strict_map_object");
            a.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));power(a,100);
            f.base.mana.set(f.base.owner,-500);equip(f,hand);
            require(service.count()==3&&service.buffed(a)&&service.buffed(b)&&service.buffed(c)&&!service.buffed(far)&&!service.buffed(enemy)&&!service.buffed(npc),"three nearest own mobs only / no roles/enemy");
            near(a.getMaxHealth(),120,"max HP times6");near(a.getHealth(),30,"current HP times3");near(a.getAttributeValue(Attributes.ATTACK_DAMAGE),6,"attack times3");near(a.getAttributeValue(Attributes.MOVEMENT_SPEED),.25,"speed times1.25");
            require(power(a)>100&&f.base.mana.get(f.base.owner)==-500,"Power amplified without mana debit/admission");
            f.base.owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,loot(f));service.refresh();
            near(a.getMaxHealth(),120,"two hands do not stack");f.base.owner.setItemInHand(hand,ItemStack.EMPTY);service.refresh();near(a.getHealth(),30,"moving syringe between hands does not reapply");
            a.setHealth(21);f.base.owner.getInventory().clearContent();service.refresh();
            require(service.count()==0&&power(a)==100,"exact high Power restoration");near(a.getMaxHealth(),20,"original max HP");near(a.getHealth(),7,"damage survives exact HP division");near(a.getAttributeValue(Attributes.ATTACK_DAMAGE),2,"original attack");near(a.getAttributeValue(Attributes.MOVEMENT_SPEED),.2,"original speed");
            for(int n=0;n<30;n++){equip(f,hand);f.base.owner.getInventory().clearContent();service.refresh();}
            near(a.getHealth(),7,"thirty swaps cannot heal");require(power(a)==100,"thirty swaps preserve integer Power");
            equip(f,hand);service.unloaded(a);require(!service.buffed(a),"unload removes owned modifier state");near(a.getHealth(),7,"unload exact rollback");
            f.base.owner.getInventory().clearContent();service.refresh();
        }
    }
    private static void special(AllGrimoireChecks.Fixture f){reset(f);var service=f.base.game.items().syringe();
        var ghast=spawn(f,new Ghast(EntityTypes.GHAST,f.level),1,true);var creeper=spawn(f,new Creeper(EntityTypes.CREEPER,f.level),2,true);
        ((SyringeMobAccess.GhastPower)ghast).skywars2$syringePower(2);((SyringeMobAccess.CreeperRadius)creeper).skywars2$syringeRadius(4);
        equip(f,InteractionHand.OFF_HAND);require(ghast.getExplosionPower()==6&&((SyringeMobAccess.CreeperRadius)creeper).skywars2$syringeRadius()==12,"native explosion field amplification");
        f.base.owner.getInventory().clearContent();service.refresh();require(ghast.getExplosionPower()==2&&((SyringeMobAccess.CreeperRadius)creeper).skywars2$syringeRadius()==4,"exact explosion field rollback");
        reset(f);var blaze=spawn(f,new Blaze(EntityTypes.BLAZE,f.level),1,true);equip(f,InteractionHand.MAIN_HAND);
        f.beta.teleportTo(f.level,f.at.x+20,f.at.y,f.at.z,Set.of(),0,0,true);f.beta.setNoGravity(true);
        service.blaze(blaze);var shots=new ArrayList<LargeFireball>();for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&e instanceof LargeFireball shot&&!shot.isRemoved())shots.add(shot);
        require(shots.size()==1&&shots.getFirst().getOwner()==f.base.owner,"one owned native bonus large fireball");
        var fireData=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess());shots.getFirst().saveWithoutId(fireData);
        require(fireData.buildResult().getByteOr("ExplosionPower",(byte)0)==2,"source native fireball explosion power");near(shots.getFirst().accelerationPower,.05,"source fireball acceleration");near(shots.getFirst().getDeltaMovement().length(),.7,"source initial velocity");
        for(int tick=0;tick<3;tick++)f.level.tickNonPassenger(shots.getFirst());require(!shots.getFirst().isRemoved()&&shots.getFirst().getX()>blaze.getX()+1,"native bonus fireball leaves its launch mob without self-impact");
        reset(f);var guardian=spawn(f,new Guardian(EntityTypes.GUARDIAN,f.level),1,true);equip(f,InteractionHand.MAIN_HAND);
        f.beta.getInventory().clearContent();f.beta.removeAllEffects();f.beta.setAbsorptionAmount(0);f.beta.setHealth(1000);f.beta.setInvulnerableTime(0);f.beta.damageCooldownTime=0;
        var source=new DamageSource(f.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),guardian,guardian);
        require(f.beta.hurtServer(f.level,source,1),"real native guardian accepted hit");near(f.beta.getHealth(),989,"accepted guardian hit adds10 arrow damage exactly once");
        reset(f);
    }
    private static void persistence(AllGrimoireChecks.Fixture f){reset(f);var service=f.base.game.items().syringe();var mob=skeleton(f,1,true);
        mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));power(mob,4);equip(f,InteractionHand.MAIN_HAND);
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess());mob.saveWithoutId(out);
        var reloaded=new Skeleton(EntityTypes.SKELETON,f.level);reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),out.buildResult()));
        reloaded.setUUID(UUID.randomUUID());reloaded.setPos(f.at.add(6,0,0));require(f.level.addFreshEntity(reloaded),"native saved entity readmission");
        near(reloaded.getMaxHealth(),20,"saved permanent modifier repaired on load");near(reloaded.getHealth(),10,"saved health repaired on load");require(power(reloaded)==4&&!reloaded.entityTags().contains("syringe_buffed"),"saved Power/tags repaired");
        reset(f);var ordinary=f.base.game.items().summons();ordinary.castOrdinary(f.base.owner,LbcSummonerItems.kinds().getFirst());equip(f,InteractionHand.MAIN_HAND);
        var old=ordinary.ordinarySnapshot("skeletons").stream().filter(service::buffed).findFirst().orElseThrow();
        var changed=old.convertTo(EntityTypes.STRAY,ConversionParams.single(old,true,true),EntitySpawnReason.CONVERSION,m->{});
        require(changed!=null&&service.buffed(changed)&&!service.buffed(old)&&service.count()==3&&ordinary.owner(changed)==f.base.owner,"native conversion transfers one selected buff and owner");
        near(changed.getMaxHealth(),120,"converted native stats amplified once");f.base.owner.getInventory().clearContent();service.refresh();near(changed.getMaxHealth(),20,"converted native stats restored");
        reset(f);mob=skeleton(f,1,true);mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));power(mob,4);mob.setDropChance(EquipmentSlot.MAINHAND,1);equip(f,InteractionHand.MAIN_HAND);
        mob.hurtServer(f.level,mob.damageSources().playerAttack(f.base.owner),10000);require(!mob.isAlive()&&!service.buffed(mob),"real death clears buff before equipment loot");
        for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&e instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(Items.BOW))
            require(EnchantmentHelper.getItemEnchantmentLevel(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),item.getItem())==4,"dropped bow keeps original Power");
        reset(f);
    }
    private static void mannequin(AllGrimoireChecks.Fixture f){reset(f);
        var actor=new net.minecraft.world.entity.decoration.Mannequin(EntityTypes.MANNEQUIN,f.level);actor.setPos(f.at.add(1,0,0));actor.setNoGravity(true);actor.setHealth(10);
        f.base.game.items().summons().track(f.base.owner,actor);require(f.level.addFreshEntity(actor),"owned native mannequin in source mobs tag");
        equip(f,InteractionHand.OFF_HAND);require(f.base.game.items().syringe().buffed(actor),"source mobs tag includes native mannequins, not only Java Mob");near(actor.getMaxHealth(),120,"mannequin max HP boost");
        f.base.owner.getInventory().clearContent();f.base.game.items().syringe().refresh();near(actor.getMaxHealth(),20,"mannequin max HP restored");near(actor.getHealth(),10,"mannequin HP restored");reset(f);
    }
    private static void cultists(AllGrimoireChecks.Fixture f){reset(f);var summons=f.base.game.items().summons();
        var luck=summons.spawnPreset(f.base.owner,"luck",f.at.add(1,0,0));var omni=summons.spawnPreset(f.base.owner,"omnipresence",f.at.add(2,0,0));
        var obsession=summons.spawnPreset(f.base.owner,"obsession",f.at.add(3,0,0));require(luck!=null&&omni!=null&&obsession!=null,"three native source cultists");
        for(var m:List.of(luck,omni,obsession)){m.setNoAi(true);m.setNoGravity(true);}equip(f,InteractionHand.OFF_HAND);
        var target=spawn(f,new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,f.level),4,false);
        summons.tick();require(f.base.game.items().psycho().effectValue(target,"twitching")==15,"actual syringe tag triples Obsession's affliction");
        var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());f.level.getAllEntities().forEach(before::add);
        require(luck.hurtServer(f.level,luck.damageSources().playerAttack(f.beta),1),"actual Luck hurt reaction");
        var volley=new ArrayList<net.minecraft.world.entity.projectile.arrow.AbstractArrow>();for(var e:f.level.getAllEntities())if(!before.contains(e)&&e instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow a)volley.add(a);
        require(volley.size()==8&&volley.stream().allMatch(a->a.getOwner()==f.base.owner&&arrowDamage(a)==2),"buffed Luck emits eight owned source retaliation arrows");
        before.clear();f.level.getAllEntities().forEach(before::add);require(omni.hurtServer(f.level,omni.damageSources().playerAttack(f.beta),1),"actual Omnipresence hurt reaction");
        volley.clear();for(var e:f.level.getAllEntities())if(!before.contains(e)&&e instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow a)volley.add(a);
        require(volley.size()==1&&arrowDamage(volley.getFirst())==3&&volley.getFirst().getOwner()==f.base.owner,"buffed Omnipresence emits an owned damage3 arrow");reset(f);
    }
    private static void pool(AllGrimoireChecks.Fixture f){for(int roll=0;roll<256;roll++)for(var item:f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:transformables_summons_all"))
        require(LbcSummonerItems.kind(item)!=null||item.is(LbcWands.SUMMONER_VEXES)||item.is(LbcWands.PSYCHOVEX)||item.is(LbcKfcRewards.PENIS_CULTUS)||item.is(LbcSyringe.ITEM),"expanded weighted pool only exposes complete native outcomes");}
    public static void run(GameController g){try(var f=new AllGrimoireChecks.Fixture(g)){try{presentation(f);reversible(f);special(f);persistence(f);mannequin(f);cultists(f);pool(f);}finally{g.items().syringe().stop();}}SkyWars2.LOGGER.info("[check] PASS syringe");}
    private static AllGrimoireChecks.Fixture retained;private static List<Mob> actors;private static String epoch;
    public static void stopProbe(GameController g){require(retained==null,"exclusive stop fixture");var f=new AllGrimoireChecks.Fixture(g,true);
        try{reset(f);for(var k:LbcSummonerItems.kinds())g.items().summons().castOrdinary(f.base.owner,k);equip(f,InteractionHand.MAIN_HAND);require(g.items().syringe().count()==3,"three selected effects retained until stop");
            actors=g.items().summons().snapshot();require(actors.size()==42,"all native ordinary actors retained");epoch=g.items().epoch();retained=f;
        }catch(Throwable error){g.items().syringe().stop();f.close();throw error;}SkyWars2.LOGGER.info("[check] PASS syringe-stop-probe");}
    public static void cleanup(GameController g){require(retained!=null,"actual-stop fixture");try{require(!g.items().currentEpoch(epoch)&&g.items().syringe().count()==0&&actors.stream().allMatch(Entity::isRemoved),"actual stop clears buffs and all42 actors");}
        finally{retained.close();retained=null;actors=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS syringe-cleanup");}
}
