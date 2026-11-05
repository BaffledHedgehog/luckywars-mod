package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Authoring fixture: actual crossbow input reaches the shared successful native volley callback. */
public final class MinigunCrossbowChecks {
    private MinigunCrossbowChecks() { }
    private static void require(boolean value,String why) {
        if (!value) throw new IllegalStateException("Minigun crossbow: "+why);
    }
    private static ItemStack flagged(net.minecraft.world.item.Item item,int count,String flag) {
        var result=new ItemStack(item,count);var tag=new CompoundTag();tag.putInt(flag,1);
        tag.putString("fixture_payload","exact_component_witness");
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return result;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final List<Entity> owned=new ArrayList<>();
        final int nativeUses;final Integer previousScore;
        final net.minecraft.advancements.AdvancementHolder advancement;final Set<String> completed=new HashSet<>();
        Marker jammer;
        Fixture(GameController game) {
            this.game=game;base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;
            nativeUses=base.owner.getStats().getValue(Stats.ITEM_USED.get(Items.CROSSBOW));
            var objective=game.server().getScoreboard().getObjective("adv_game_minigun_shooted");
            var score=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(base.owner,objective);
            previousScore=score==null?null:score.value();
            advancement=Objects.requireNonNull(game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/bows/minigun")));
            base.owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(completed::add);
        }
        void clear() {
            base.owner.stopUsingItem();base.owner.getInventory().clearContent();
            base.owner.inventoryMenu.getCraftSlots().clearContent();base.owner.inventoryMenu.setCarried(ItemStack.EMPTY);
        }
        ItemStack weapon(String flag,InteractionHand hand,ItemStack initial,boolean multi) {
            clear();var result=flagged(Items.CROSSBOW,1,flag);
            result.set(DataComponents.CUSTOM_NAME,Component.literal("Preserve minigun witness"));
            result.set(DataComponents.DAMAGE,7);
            if(multi)result.enchant(base.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT),1);
            result.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(initial.copyWithCount(1))));
            base.owner.setItemInHand(hand,result);return result;
        }
        List<Projectile> shoot(InteractionHand hand,ItemStack weapon,int expected) {
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
            base.owner.level().getAllEntities().forEach(before::add);
            var preserved=weapon.copy();int damage=weapon.getDamageValue();
            require(base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,hand).consumesAction(),"actual native crossbow use accepted");
            var family=new ArrayList<Projectile>();
            for(var actor:base.owner.level().getAllEntities())if(!before.contains(actor)) {
                owned.add(actor);if(actor instanceof Projectile p&&p.getOwner()==base.owner)family.add(p);
            }
            require(family.size()==expected,"actual native successful family expected="+expected+" actual="+family.size());
            require(base.owner.getItemInHand(hand)==weapon,"original weapon object retained");
            require(weapon.getDamageValue()==damage+expected,"native shot durability retained");
            var actual=weapon.copy();actual.remove(DataComponents.CHARGED_PROJECTILES);actual.remove(DataComponents.DAMAGE);
            preserved.remove(DataComponents.CHARGED_PROJECTILES);preserved.remove(DataComponents.DAMAGE);
            require(ItemStack.matches(actual,preserved),"all non-charge/non-native-damage weapon components exactly retained");
            return family;
        }
        void charged(ItemStack weapon,ItemStack ammo,int count) {
            var charged=weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY).itemCopies().toList();
            require(charged.size()==count,"native reload count="+count);
            for(int i=0;i<charged.size();i++){
                var expected=ammo.copyWithCount(1);
                if(i>0||LbcInfiniteAmmo.conserves(ammo)||base.owner.hasInfiniteMaterials())expected.set(DataComponents.INTANGIBLE_PROJECTILE,net.minecraft.util.Unit.INSTANCE);
                require(ItemStack.matches(charged.get(i),expected),"exact selected ammo components and native intangible policy in every charged member");
            }
        }
        void cases() {
            base.initialize();game.items().mana().score(base.owner,"adv_game_minigun_shooted").set(0);
            for(var item:List.of(Items.ARROW,Items.TIPPED_ARROW,Items.SPECTRAL_ARROW)) {
                var ammo=flagged(item,3,"custom_ammo");
                if(item==Items.TIPPED_ARROW)ammo.set(DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(0x123456),
                                List.of(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,91,2)),Optional.empty()));
                var weapon=weapon("fast",InteractionHand.MAIN_HAND,ammo,false);
                base.owner.getInventory().setItem(9,ammo);
                var first=shoot(InteractionHand.MAIN_HAND,weapon,1);
                require(ammo.getCount()==2,"exact selected ammo decremented once");charged(weapon,ammo,1);
                var second=shoot(InteractionHand.MAIN_HAND,weapon,1);
                require(ammo.getCount()==1,"repeated shot consumes next exact ammo once");charged(weapon,ammo,1);
                for(var p:List.of(first.getFirst(),second.getFirst())) {
                    require((p instanceof SpectralArrow)==(item==Items.SPECTRAL_ARROW),"native spectral identity preserved");
                    require(p instanceof AbstractArrow arrow&&ItemStack.matches(arrow.getPickupItemStackOrigin().copyWithCount(1),ammo.copyWithCount(1)),"actual native projectile retains selected custom pickup components");
                }
            }
            var ammo=flagged(Items.SPECTRAL_ARROW,2,"offhand_ammo");
            var weapon=weapon("fast",InteractionHand.MAIN_HAND,ammo,true);
            base.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
            int count=EnchantmentHelper.processProjectileCount(base.owner.level(),weapon,base.owner,1);
            shoot(InteractionHand.MAIN_HAND,weapon,1);charged(weapon,ammo,count);
            var lastAmmo=ammo.copyWithCount(1);
            shoot(InteractionHand.MAIN_HAND,weapon,count);require(ammo.isEmpty(),"multishot uses one offhand stack member per reload");
            charged(weapon,lastAmmo,count);
            shoot(InteractionHand.MAIN_HAND,weapon,count);charged(weapon,lastAmmo,0);

            ammo=flagged(Items.ARROW,3,"infinite_ammo");weapon=weapon("fast",InteractionHand.OFF_HAND,ammo,false);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ammo);
            base.owner.inventoryMenu.getCraftSlots().setItem(0,flagged(Items.PAPER,1,"infinite_arrow"));
            shoot(InteractionHand.OFF_HAND,weapon,1);require(ammo.getCount()==2,"craft holder cannot conserve unflagged selected ammo");charged(weapon,ammo,1);
            base.owner.inventoryMenu.getCraftSlots().clearContent();
            base.owner.getInventory().setItem(9,flagged(Items.PAPER,1,"infinite_arrow"));
            shoot(InteractionHand.OFF_HAND,weapon,1);require(ammo.getCount()==1,"inventory holder cannot conserve unflagged selected ammo");charged(weapon,ammo,1);

            ammo=flagged(Items.ARROW,2,"old_ammo");weapon=weapon("fast_old",InteractionHand.MAIN_HAND,ammo,false);
            base.owner.getInventory().setItem(9,ammo);base.owner.getInventory().setItem(10,flagged(Items.PAPER,1,"infinite_arrow"));
            int prior=game.items().mana().score(base.owner,"adv_game_minigun_shooted").get();
            shoot(InteractionHand.MAIN_HAND,weapon,1);require(ammo.getCount()==1,"fast_old spends unflagged selected ammo despite another inventory flag");charged(weapon,ammo,1);
            require(game.items().mana().score(base.owner,"adv_game_minigun_shooted").get()==prior,"source fast_old has no achievement counter");

            for(var flag:List.of("fast","fast_old")){
                ammo=flagged(Items.SPECTRAL_ARROW,2,"infinite_arrow");weapon=weapon(flag,InteractionHand.MAIN_HAND,ammo,true);
                base.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
                count=EnchantmentHelper.processProjectileCount(base.owner.level(),weapon,base.owner,1);
                shoot(InteractionHand.MAIN_HAND,weapon,1);require(ammo.getCount()==2,"selected flagged arrow conserved for "+flag);charged(weapon,ammo,count);
                var family=shoot(InteractionHand.MAIN_HAND,weapon,count);require(ammo.getCount()==2,"repeated selected flagged arrow conserved for "+flag);charged(weapon,ammo,count);
                for(var projectile:family){
                    require(projectile instanceof AbstractArrow arrow&&arrow.pickup==AbstractArrow.Pickup.CREATIVE_ONLY,"free native volley cannot duplicate pickups");
                    var payload=((AbstractArrow)projectile).getPickupItemStackOrigin().copyWithCount(1);payload.remove(DataComponents.INTANGIBLE_PROJECTILE);
                    require(ItemStack.matches(payload,ammo.copyWithCount(1)),"free native volley preserves selected payload with native intangible marker");
                }
            }

            ammo=flagged(Items.FIREWORK_ROCKET,2,"infinite_arrow");weapon=weapon("fast",InteractionHand.MAIN_HAND,ammo,false);
            base.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
            // Firework native durability is three per successful projectile.
            var before=weapon.copy();base.owner.gameMode.useItem(base.owner,base.owner.level(),weapon,InteractionHand.MAIN_HAND);
            for(var actor:base.owner.level().getAllEntities())if(!base.before.contains(actor)&&!owned.contains(actor))owned.add(actor);
            require(weapon.getDamageValue()==before.getDamageValue()+3&&ammo.getCount()==1,"native held flagged firework still spends ammo and native durability");charged(weapon,ammo,1);

            weapon=weapon("fast",InteractionHand.MAIN_HAND,new ItemStack(Items.ARROW),false);
            game.items().mana().score(base.owner,"adv_game_minigun_shooted").set(319);
            shoot(InteractionHand.MAIN_HAND,weapon,1);charged(weapon,new ItemStack(Items.ARROW),0);
            require(game.items().mana().score(base.owner,"adv_game_minigun_shooted").get()==320
                    &&base.owner.getAdvancements().getOrStartProgress(advancement).isDone(),"no-ammo successful volley increments once and grants actual source advancement");

            ammo=flagged(Items.ARROW,2,"jammed_ammo");weapon=weapon("fast",InteractionHand.MAIN_HAND,ammo,false);base.owner.getInventory().setItem(9,ammo);
            jammer=new Marker(EntityTypes.MARKER,base.owner.level());jammer.setPos(base.owner.position());jammer.entityTags().add("stopper");
            require(base.owner.level().addFreshEntity(jammer),"TECH fixture publication");game.items().magic().loaded(jammer);
            require(game.items().magic().mechanismSuppressed(base.owner),"actual TECH source field active");
            shoot(InteractionHand.MAIN_HAND,weapon,1);require(ammo.getCount()==2,"TECH blocks reload but retains native shot");charged(weapon,ammo,0);
            require(game.items().mana().score(base.owner,"adv_game_minigun_shooted").get()==320,"TECH suppresses source reload function counter");
        }
        @Override public void close() {
            if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();}
            for(var actor:owned)actor.discard();
            var objective=game.server().getScoreboard().getObjective("adv_game_minigun_shooted");
            if(objective!=null){if(previousScore==null)game.server().getScoreboard().resetSinglePlayerScore(base.owner,objective);else game.server().getScoreboard().getOrCreatePlayerScore(base.owner,objective).set(previousScore);}
            for(var criterion:advancement.value().criteria().keySet())if(completed.contains(criterion))base.owner.getAdvancements().award(advancement,criterion);else base.owner.getAdvancements().revoke(advancement,criterion);
            base.owner.getStats().setValue(base.owner,Stats.ITEM_USED.get(Items.CROSSBOW),nativeUses);
            base.close();
        }
    }
    public static void run(GameController game) {
        try(var fixture=new Fixture(game)){fixture.cases();}
        SkyWars2.LOGGER.info("[check] PASS minigun-crossbow (native real use, exact custom/tipped/spectral ammo, offhand, multishot, selected-only infinite ammo, paid firework, no ammo, TECH, source counter/advancement and weapon conservation)");
    }
}
