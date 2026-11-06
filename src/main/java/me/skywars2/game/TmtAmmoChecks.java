package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Genuine native volleys, accepted collisions and persistent independent ammo descriptors. */
public final class TmtAmmoChecks {
    private TmtAmmoChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT ammo: "+why);}
    private static ItemStack weapon(List<String> flags,List<Item> ammunition){
        var result=LbcTmtItems.prototype(3);var data=result.get(DataComponents.CUSTOM_DATA).copyTag();
        var slots=new ArrayList<LbcTmtAbilities.Ability>();
        for(var flag:flags){var p=new CompoundTag();p.putInt(flag,1);slots.add(new LbcTmtAbilities.Ability("check/"+flag,CustomData.of(p)));}
        for(var item:ammunition){var p=item.getDefaultInstance().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            var id=BuiltInRegistries.ITEM.getKey(item);p.putString(LbcTmtAbilities.NATIVE,id.toString());slots.add(new LbcTmtAbilities.Ability("native/ammo/"+id.getPath(),CustomData.of(p)));}
        LbcTmtAbilities.write(data,slots);result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return result;
    }
    private static ItemStack fire(WoolBucketChecks.Fixture f,ItemStack weapon){
        var ammo=new ItemStack(Items.ARROW,5);f.owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);f.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
        weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(ammo.copyWithCount(1))));
        require(f.owner.gameMode.useItem(f.owner,f.level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"actual registered crossbow accepts its real loaded volley");
        require(!net.minecraft.world.item.CrossbowItem.isCharged(weapon)&&ammo.getCount()==5,"one stored native chamber consumed; no fabricated reload or extra inventory ammo debit");
        return ammo;
    }
    private static void clone(GameController game,WoolBucketChecks.Fixture f,Projectile original){
        var before=new HashSet<java.util.UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
        require(game.items().cloneSkills().copyNow(original,15),"canonical x2 accepts the native composite carrier");
        Projectile clone=null;for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof Projectile found&&e.getClass()==original.getClass())clone=found;
        var source=LbcTmtAmmoPayloads.state(original);var copied=clone==null?null:LbcTmtAmmoPayloads.state(clone);
        require(clone!=null&&clone.getOwner()==original.getOwner()&&copied!=null&&copied!=source&&copied.entries.size()==source.entries.size(),"canonical cloned body retains independent descriptor state and original owner");
        for(int i=0;i<source.entries.size();i++)require(source.entries.get(i)!=copied.entries.get(i)&&source.entries.get(i).age==copied.entries.get(i).age,"clone owns each mutable native payload cursor");
        f.own(clone);game.items().entityUnloaded(clone);clone.discard();
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();require(game.items().exasperato().pendingSearches()==0,"exclusive native beam queue");
            var before=new HashSet<java.util.UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            Cow victim=null;
            try{
                var physical=weapon(List.of("imba_shotgun"),List.of(LbcImbaNativeItems.QUIVER_EVOKER_ARROW,LbcImbaNativeItems.QUIVER_LIGHTSHOOT_ARROW,LbcImbaNativeItems.HOMING_ARROW));
                var ammo=fire(f,physical);var arrows=new ArrayList<AbstractArrow>();
                for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof AbstractArrow arrow&&!arrow.isRemoved())arrows.add(arrow);
                require(arrows.size()==256,"native shotgun expands the one accepted family");
                for(var arrow:arrows){var payloads=LbcTmtAmmoPayloads.state(arrow);
                    require(payloads!=null&&payloads.entries.size()==3&&payloads.hasVariant(102)&&payloads.hasVariant(112)&&payloads.hasVariant(300),"all expanded arrows retain three independent properties");
                    require(ItemStack.isSameItemSameComponents(arrow.getPickupItemStackOrigin(),ammo),"original actual arrow/pickup identity is unchanged");
                    require(LbcHomingArrows.isManaged(arrow),"extra homing property enters the existing native flight owner");
                }
                var accepted=arrows.getFirst();clone(game,f,accepted);var image=LbcTmtRanged.image(game,accepted);var loaded=LbcTmtRanged.donors(f.owner,List.of(image)).getFirst();
                try{require(LbcTmtAmmoPayloads.state(loaded)!=null&&LbcTmtAmmoPayloads.state(loaded).entries.size()==3,"actual entity codec preserves all descriptor snapshots/state");}
                finally{game.items().entityUnloaded(loaded);loaded.discard();}
                var beam=LbcBeamAmmo.capture(game,f.owner,physical,new ArrayList<Projectile>(arrows));
                require(beam!=null&&beam.hasVariant(102)&&beam.hasVariant(112)&&beam.hasVariant(300),"native beam capture retains every descriptor variant");
                var beamCopy=((LbcBeamAmmo)beam).copy();require(beamCopy.hasVariant(102)&&beamCopy.hasVariant(112)&&beamCopy.hasVariant(300),"beam continuation copy retains composition");
                victim=new Cow(EntityTypes.COW,f.level);victim.setNoAi(true);victim.setNoGravity(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);
                victim.setPos(f.owner.position().add(0,500,6));require(f.level.addFreshEntity(victim),"real native collision target");
                accepted.setPos(victim.position().add(0,.5,-2));accepted.setDeltaMovement(new Vec3(0,0,4));accepted.tick();
                require(victim.getHealth()<1000&&game.items().quiver().queued(victim,102)==1&&game.items().quiver().queued(victim,112)==50,"one real accepted arrow hit delivers both native source statuses once: hp="+victim.getHealth()+", evoker="+game.items().quiver().queued(victim,102)+", light="+game.items().quiver().queued(victim,112)+", removed="+accepted.isRemoved()+", at="+accepted.position());
                game.items().quiver().clear(victim);
                for(var arrow:arrows){game.items().entityUnloaded(arrow);arrow.discard();}game.items().homingArrows().stop();
                var converted=weapon(List.of("shulker_new"),List.of(LbcImbaNativeItems.QUIVER_EVOKER_ARROW,LbcImbaNativeItems.QUIVER_LIGHTSHOOT_ARROW));fire(f,converted);
                ShulkerBullet bullet=null;for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof ShulkerBullet found&&LbcShulkerBow.managed(found)&&!found.isRemoved())bullet=found;
                require(bullet!=null&&LbcTmtAmmoPayloads.state(bullet)!=null&&LbcTmtAmmoPayloads.state(bullet).entries.size()==2,"native shulker conversion retains both independent descriptors");
                clone(game,f,bullet);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.setHealth(1000);
                bullet.setPos(victim.position().add(0,.5,-2));bullet.setDeltaMovement(new Vec3(0,0,4));bullet.tick();
                require(victim.getHealth()==996&&game.items().quiver().queued(victim,102)==1&&game.items().quiver().queued(victim,112)==50,"real shulker hit keeps native4HP damage and both source statuses: hp="+victim.getHealth()+", evoker="+game.items().quiver().queued(victim,102)+", light="+game.items().quiver().queued(victim,112));
            }finally{
                if(victim!=null)game.items().quiver().clear(victim);
                game.items().shulkerBow().stop();game.items().homingArrows().stop();game.items().chargedBows().stop();game.items().exasperato().stop();
                for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())){f.own(e);game.items().entityUnloaded(e);e.discard();}
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-ammo (real256-arrow native volley, unchanged actual ammo/debit, three extra payloads/homing, canonical x2, entity codec and beam copies, accepted physical and4HP shulker contacts with two independent native statuses)");
    }
}
