package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;

/** Actual native use/draw/volley callbacks; no nearby-arrow substitution or second ammunition debit. */
public final class ImbaCrossbowChecks {
    private ImbaCrossbowChecks(){}
    static void require(boolean value,String why){if(!value)throw new IllegalStateException("IMBA crossbows: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final List<Entity> owned=new ArrayList<>();final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final boolean skills,levelups;final int trinketLimit;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game,true);skills=game.config().lbcSkills;levelups=game.config().lbcLevelups;trinketLimit=game.config().lbcTrinketLimit;game.config().lbcSkills=false;game.config().lbcLevelups=false;game.config().lbcTrinketLimit=0;base.initialize();}
        ItemStack leaf(LbcImbaCrossbows.Kind kind){var items=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/"+kind.id);require(items.size()==1&&items.getFirst().is(kind.item),"canonical native leaf "+kind);return items.getFirst();}
        void clear(){base.owner.stopUsingItem();base.owner.getInventory().clearContent();for(var e:owned){game.items().entityUnloaded(e);e.discard();}owned.clear();base.clear();game.items().magic().inventoryChanged(base.owner);game.items().magic().flushCleansing();base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setDeltaMovement(Vec3.ZERO);}
        Set<Entity> bodies(){var set=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());base.owner.level().getAllEntities().forEach(set::add);return set;}
        List<Projectile> added(Set<Entity> previous){var out=new ArrayList<Projectile>();for(var e:base.owner.level().getAllEntities())if(!previous.contains(e)&&!e.isRemoved()){owned.add(e);if(e instanceof Projectile p&&p.getOwner()==base.owner)out.add(p);}return out;}
        ItemStack equip(LbcImbaCrossbows.Kind kind,ItemStack ammo){clear();var weapon=leaf(kind);weapon.set(DataComponents.CUSTOM_NAME,Component.literal("preserve exact witness"));base.owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);base.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);return weapon;}
        List<Projectile> shoot(LbcImbaCrossbows.Kind kind,ItemStack ammo){var weapon=equip(kind,ammo);weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(ammo.copyWithCount(1))));var before=bodies();require(base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction(),"actual native use "+kind);return added(before);}
        ItemStack poison(int count){var ammo=new ItemStack(Items.TIPPED_ARROW,count);ammo.set(DataComponents.CUSTOM_NAME,Component.literal("native ammo witness"));ammo.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x123456),List.of(new MobEffectInstance(MobEffects.POISON,160,1)),Optional.empty()));return ammo;}
        void presentation(){
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)base.owner.connection;
            for(var kind:LbcImbaCrossbows.Kind.values()){
                var item=leaf(kind);var defaults=kind.item.getDefaultInstance();
                for(var component:List.of(DataComponents.CUSTOM_NAME,DataComponents.LORE,DataComponents.CUSTOM_DATA,DataComponents.TOOLTIP_STYLE,DataComponents.ITEM_MODEL,DataComponents.ENCHANTMENTS))require(Objects.equals(item.get(component),defaults.get(component)),"give/loot exact component "+component+" "+kind);
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(item,TooltipFlag.NORMAL,provider.getPacketContext(),base.owner.registryAccess());
                require(ItemStack.matches(item,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),base.owner.registryAccess())),"native Polymer roundtrip "+kind);
            }
        }
        void shotgun(){
            var ammo=poison(2);var shots=shoot(LbcImbaCrossbows.Kind.SHOTGUN,ammo);
            require(shots.size()==256,"exact source256 physical arrows");
            for(var p:shots){var arrow=(AbstractArrow)p;require(arrow.getOwner()==base.owner&&arrow.getDeltaMovement().z>=4.5&&arrow.getDeltaMovement().z<=5.5&&Math.abs(arrow.getDeltaMovement().x)<=.5&&Math.abs(arrow.getDeltaMovement().y)<=.5,"source5 speed/cartesian spread and actual owner");require(arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS).equals(ammo.get(DataComponents.POTION_CONTENTS))&&arrow.pickup==AbstractArrow.Pickup.DISALLOWED,"all source nondropping pellets preserve actual custom ammo");}
            require(ammo.getCount()==2,"already charged volley never debits inventory twice");clear();
        }
        void beams(){
            for(var kind:List.of(LbcImbaCrossbows.Kind.SNIPER,LbcImbaCrossbows.Kind.SUPER)){
                var shots=shoot(kind,poison(1));require(shots.isEmpty(),"beam replaces own accepted native arrows");var traces=game.items().wands().electroSnapshot();int count=kind==LbcImbaCrossbows.Kind.SNIPER?1:32;
                require(traces.size()==count&&traces.stream().allMatch(t->t.erecto.kind==LbcErecto.CROSSBOW&&t.erecto.ammunition!=null&&t.owner==base.owner&&t.position.equals(base.owner.position())&&t.lifetime==(count==1?200:50)),"exact source count/lifetime/origin and accepted ammo "+kind);
                if(count==1){
                    var target=new Cow(EntityTypes.COW,base.owner.level());target.setNoAi(true);target.setNoGravity(true);target.setPos(base.owner.position().add(0,-.2,8));target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);owned.add(target);require(base.owner.level().addFreshEntity(target),"native living target");
                    var trace=traces.getFirst();int age=trace.age;game.items().timeStop().begin(base.owner.level(),40);game.items().wands().tick();require(trace.age==age,"shared TimeStop pauses beam");game.items().timeStop().end(base.owner.level());
                    require(game.items().erecto().step(trace)&&target.getHealth()==860&&target.hasEffect(MobEffects.POISON),"source7 unit-cell hits at20 arrow damage, legacy bypass cooldown and retained actual ammo effect hp="+target.getHealth()+" poison="+target.hasEffect(MobEffects.POISON)+" trace="+trace.position+" target="+target.position());
                    var clone=new LbcWands.ElectroTrace(trace);require(clone.erecto.ammunition!=trace.erecto.ammunition&&clone.position.equals(trace.position),"typed clone detaches accepted payload state");
                }
                clear();
            }
        }
        void continuous(){
            for(var kind:List.of(LbcImbaCrossbows.Kind.FAST,LbcImbaCrossbows.Kind.GATLING)){
                var ammo=poison(7);var weapon=equip(kind,ammo);var before=bodies();require(base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction()&&base.owner.isUsingItem(),"native sustained use "+kind);
                for(int i=0;i<2;i++)weapon.getItem().onUseTick(base.owner.level(),base.owner,weapon,72000-i);
                var shots=added(before);require(ammo.getCount()==(kind==LbcImbaCrossbows.Kind.FAST?5:1),"selected debit1/3 per native active use tick "+kind);
                require(kind==LbcImbaCrossbows.Kind.FAST?shots.size()==2:shots.isEmpty()&&game.items().wands().electroSnapshot().size()==8,"exact continuous physical/beam family "+kind);
                require(base.owner.getMainHandItem()==weapon&&weapon.getHoverName().getString().equals("preserve exact witness"),"same native weapon/components retained");
                clear();ammo=new ItemStack(LbcImbaNativeItems.INFINITE_ARROW,1);weapon=equip(kind,ammo);base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND);weapon.getItem().onUseTick(base.owner.level(),base.owner,weapon,72000);require(ammo.getCount()==1,"only selected infinite ammo is free "+kind);clear();
            }
        }
        void rockets(){
            var shots=shoot(LbcImbaCrossbows.Kind.SHOTGUN,new ItemStack(Items.FIREWORK_ROCKET));
            require(shots.stream().filter(AbstractArrow.class::isInstance).count()==256&&shots.stream().filter(net.minecraft.world.entity.projectile.FireworkRocketEntity.class::isInstance).count()==1,"source shotgun adds pellets while preserving native firework");clear();
            shots=shoot(LbcImbaCrossbows.Kind.SNIPER,new ItemStack(Items.FIREWORK_ROCKET));
            require(shots.size()==1&&shots.getFirst() instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity&&game.items().wands().electroSnapshot().size()==1,"source sniper retains actual native firework alongside beam");clear();
        }
        void guards(){
            for(var kind:LbcImbaCrossbows.Kind.values())if(kind!=LbcImbaCrossbows.Kind.QUICK){
                var ammo=poison(3);var weapon=equip(kind,ammo);var marker=new Marker(EntityTypes.MARKER,base.owner.level());marker.setPos(base.owner.position());marker.entityTags().add("stopper");owned.add(marker);require(base.owner.level().addFreshEntity(marker),"native TECH field");game.items().magic().loaded(marker);
                require(game.items().magic().mechanismSuppressed(base.owner)&&!base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction()&&ammo.getCount()==3,"TECH rejects native input before debit "+kind);game.items().magic().unloaded(marker);clear();
                ammo=poison(3);weapon=equip(kind,ammo);base.owner.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(base.owner);game.items().magic().flushCleansing();
                require(game.items().magic().artifactSuppressedAt(base.owner.level(),base.owner.position())&&!base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction()&&ammo.getCount()==3,"actual strong artifact rejects before debit "+kind);clear();
                weapon=equip(kind,poison(3));base.owner.entityTags().add("time_stopped");try{require(!base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND).consumesAction(),"personal stop rejects "+kind);}finally{base.owner.entityTags().remove("time_stopped");}clear();
            }
        }
        public void close(){try{clear();}finally{game.config().lbcSkills=skills;game.config().lbcLevelups=levelups;game.config().lbcTrinketLimit=trinketLimit;base.close();}}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.presentation();f.shotgun();f.beams();f.continuous();f.rockets();f.guards();}SkyWars2.LOGGER.info("[check] PASS imba-crossbows (six canonical native prototypes, Polymer, exact256 pellets, Erecto1/32/4 source traces, native20 source-cell hits/ammo effects, selected infinite draw, held-use input and TECH/TimeStop)");}
}
