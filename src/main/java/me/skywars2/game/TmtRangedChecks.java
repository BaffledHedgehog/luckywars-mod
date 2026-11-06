package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Actual native crossbow publication, debit, terminal branches and codec contexts. */
public final class TmtRangedChecks {
    private TmtRangedChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("TMT ranged: "+why);}
    private static ItemStack weapon(String...flags){
        var result=LbcTmtItems.prototype(3);var root=new CompoundTag();root.putInt("tmt_item",1);
        var abilities=new ArrayList<LbcTmtAbilities.Ability>();
        for(var flag:flags){var data=new CompoundTag();data.putInt(flag,1);abilities.add(new LbcTmtAbilities.Ability("native/check_"+flag,CustomData.of(data)));}
        LbcTmtAbilities.write(root,abilities);result.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return result;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final boolean skills,levelups;final int trinketLimit;
        final Set<Entity> initial=Collections.newSetFromMap(new IdentityHashMap<>());
        Fixture(GameController game){
            this.game=game;base=new ElementalGrimoireChecks.Fixture(game,true);
            skills=game.config().lbcSkills;levelups=game.config().lbcLevelups;trinketLimit=game.config().lbcTrinketLimit;
            game.config().lbcSkills=false;game.config().lbcLevelups=false;game.config().lbcTrinketLimit=0;
            base.initialize();base.owner.level().getAllEntities().forEach(initial::add);
        }
        List<Entity> added(){var result=new ArrayList<Entity>();for(var body:base.owner.level().getAllEntities())if(!initial.contains(body)&&!body.isRemoved())result.add(body);return result;}
        ItemStack ammo(){var ammo=new ItemStack(Items.TIPPED_ARROW,5);ammo.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(),Optional.of(0x123456),List.of(new MobEffectInstance(MobEffects.POISON,160,1)),Optional.empty()));return ammo;}
        ItemStack shoot(ItemStack stack){return shoot(stack,false);}
        ItemStack shoot(ItemStack stack,boolean scoped){
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);var ammo=ammo();base.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
            stack.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(ammo.copyWithCount(1))));
            java.util.function.BooleanSupplier release=()->base.owner.gameMode.useItem(base.owner,base.owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction();
            require(scoped?LbcTmtAbilities.invoke(stack,LbcTmtAbilities.read(stack).getFirst().id(),release):release.getAsBoolean(),"actual precharged native crossbow input accepted");
            return ammo;
        }
        void clear(){
            base.owner.stopUsingItem();game.items().chargedBows().stop();game.items().shulkerBow().stop();game.items().madokaBow().clear();game.items().balance().stop();
            for(var body:added()){game.items().entityUnloaded(body);body.discard();}
            base.clear();base.owner.getInventory().clearContent();base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setDeltaMovement(Vec3.ZERO);
        }
        void physical(){
            var stack=weapon("imba_shotgun","double_old","madoka_bow","fast");var ammo=shoot(stack,true);
            var shots=added().stream().filter(AbstractArrow.class::isInstance).map(AbstractArrow.class::cast).toList();
            require(shots.size()==512,"native256 shotgun expansion composes with old double and Madoka");
            for(var arrow:shots){require(arrow.getOwner()==base.owner&&LbcMadokaBow.recognizes(arrow),"all expanded arrows retain native owner and independent Madoka");
                require(arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS).equals(ammo.get(DataComponents.POTION_CONTENTS)),"all512 exact accepted poison payloads retained");}
            require(ammo.getCount()==4&&net.minecraft.world.item.CrossbowItem.isCharged(stack),"one native minigun reload debit after whole accepted volley");
            require(base.owner.getMainHandItem()==stack&&ItemData.intValue(stack,"tmt_item",0)==1&&LbcTmtAbilities.read(stack).size()==4,"actual carrier identity and all private slots retained");clear();
        }
        void terminals(){
            game.config().lbcTrinketLimit=10000;
            base.owner.getInventory().setItem(9,new ItemStack(LbcTrinkets.ROCKET_SPELL,99));
            game.items().mana().set(base.owner,24);
            var stack=weapon("shulker_new","madoka_bow","imba_sniper_crossbow","railgun_bow","fast");var before=stack.get(DataComponents.CUSTOM_DATA);var ammo=shoot(stack);
            var shots=added().stream().filter(Projectile.class::isInstance).map(Projectile.class::cast).toList();
            require(shots.size()==2&&shots.stream().filter(LbcShulkerBow::managed).count()==1
                    &&shots.stream().filter(LbcMadokaBow::recognizes).count()==1,"one accepted volley independently produces one Shulker and one Madoka; no phantom vanilla arrow");
            require(game.items().wands().electroSnapshot().size()==1&&game.items().exasperato().pendingSearches()>0,"both independent IMBA and charged native beam owners execute");
            require(ammo.getCount()==4&&stack.get(DataComponents.CUSTOM_DATA).equals(before),"terminal siblings spend only one reload and restore aggregate metadata");
            require(game.items().mana().get(base.owner)==18,"guaranteed native rocket trinket rolls and spends mana exactly once across all branches");
            for(var shot:shots){require(shot.getOwner()==base.owner,"every independent output keeps real owner");
                require(shot.entityTags().contains("rocket_spell"),"every physical branch retains the same once-rolled rocket trait");
                if(shot instanceof AbstractArrow arrow)require(arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS).equals(ammo.get(DataComponents.POTION_CONTENTS)),"Madoka donor uses exact native ammo codec");}
            clear();game.config().lbcTrinketLimit=0;
        }
        void explosive(){
            var ammo=shoot(weapon("explosive","shulker_new","madoka_bow"));var bodies=added();
            require(bodies.stream().anyMatch(PrimedTnt.class::isInstance)&&bodies.stream().filter(LbcShulkerBow::managed).count()==1
                    &&bodies.stream().filter(LbcMadokaBow::recognizes).count()==1,"explosive token debit cannot hide later independent Shulker/Madoka outputs");
            require(ammo.getCount()==5&&base.owner.getMainHandItem().isEmpty(),"native explosive weapon debit exactly once without extra ammunition debit");clear();
        }
        void beams(){
            var ammo=shoot(weapon("imba_sniper_crossbow","imba_super_shotgun","imba_gatlingator"));
            require(game.items().wands().electroSnapshot().size()==37,"all source1/32/4 beam families run instead of first-match dispatch");
            require(added().stream().noneMatch(AbstractArrow.class::isInstance)&&ammo.getCount()==5,"one precharged native volley retires original arrows without another debit");clear();
        }
        @Override public void close(){try{clear();}finally{game.config().lbcSkills=skills;game.config().lbcLevelups=levelups;game.config().lbcTrinketLimit=trinketLimit;base.close();}}
    }
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.physical();fixture.terminals();fixture.explosive();fixture.beams();}
        SkyWars2.LOGGER.info("[check] PASS tmt-ranged (actual native accepted volleys, private flags,512 physical arrows, independent terminal owners, native codec/ammo and single reload debit)");
    }
}
