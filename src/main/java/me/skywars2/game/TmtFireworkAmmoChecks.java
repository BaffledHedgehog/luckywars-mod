package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

/** Real held-ammo selection, native charge/debit, charged codec and vanilla rocket factory. */
public final class TmtFireworkAmmoChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT firework ammo: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var s=new Scene(game);running=s;try{s.begin();}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{require(s.game==game,"same controller");if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS tmt-firework-ammo (ordinary/native/private TMT held selection; actual charge and one debit; charged codec/power/wear; exact private fireworks without outer/sibling leakage; no arrow fallback)");}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL tmt-firework-ammo",e);}}
    static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        ItemStack weapon,ammo,before;Fireworks expected;int kind,when;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(Items.CROSSBOW,Items.FIREWORK_ROCKET,LbcImbaNativeItems.FIREWORK,LbcTmtItems.prototype(0).getItem()));}
        void begin(){f.prepareParticipants();charge();}
        ItemStack source(){
            var nativeAmmo=LbcImbaNativeItems.FIREWORK.getDefaultInstance().copyWithCount(2);
            if(kind==0){var ordinary=new ItemStack(Items.FIREWORK_ROCKET,2);ordinary.set(DataComponents.FIREWORKS,nativeAmmo.get(DataComponents.FIREWORKS));return ordinary;}
            if(kind==1)return nativeAmmo;
            var property=game.items().tmt().useProfiles().families().stream().flatMap(row->row.variants().stream()).filter(p->LbcTmtComponents.nativeItem(p.data())==LbcImbaNativeItems.FIREWORK).findFirst().orElseThrow();
            var sibling=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt("zaika",0)==1).findFirst().orElseThrow();
            var stack=LbcTmtItems.prototype(0).copyWithCount(2);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(property,sibling));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            stack.set(DataComponents.FIREWORKS,new Fireworks(0,List.of()));return stack;
        }
        void charge(){
            weapon=new ItemStack(Items.CROSSBOW);ammo=source();before=ammo.copy();expected=LbcImbaNativeItems.FIREWORK.getDefaultInstance().get(DataComponents.FIREWORKS);require(expected!=null&&!expected.explosions().isEmpty(),"actual authored firework payload");
            f.owner.getInventory().clearContent();f.owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);f.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
            require(f.owner.getProjectile(weapon)==ammo,"native held selection chooses actual physical ammo");
            require(f.owner.getProjectile(new ItemStack(Items.BOW)).isEmpty(),"fireworks are not bow ammunition");
            f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);f.owner.getInventory().setItem(9,ammo);
            require(f.owner.getProjectile(weapon).isEmpty(),"inventory selection retains vanilla arrow-only rule");
            f.owner.getInventory().setItem(9,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
            require(f.owner.gameMode.useItem(f.owner,f.level,weapon,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.isUsingItem(),"real native charging input");when=game.server().getTickCount();
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-when;
            var loaded=weapon.get(DataComponents.CHARGED_PROJECTILES);if(loaded==null||loaded.isEmpty()){require(elapsed<40,"native charge completes");return false;}
            require(ammo.getCount()==1&&ItemStack.matches(ammo,before.copyWithCount(1)),"one actual physical ammo debit, unchanged properties");
            var ops=f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            weapon=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,weapon).getOrThrow()).getOrThrow();f.owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);
            require(weapon.get(DataComponents.CHARGED_PROJECTILES).contains(Items.FIREWORK_ROCKET),"native charged query survives serialization");
            var old=new HashSet<UUID>();f.level.getAllEntities().forEach(e->old.add(e.getUUID()));
            require(f.owner.gameMode.useItem(f.owner,f.level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"native charged shot input");
            var fresh=new ArrayList<Projectile>();for(var e:f.level.getAllEntities())if(e instanceof Projectile p&&!old.contains(e.getUUID())){fresh.add(p);f.own(e);}
            require(fresh.size()==1&&fresh.getFirst() instanceof FireworkRocketEntity,"one genuine native rocket, no extra arrow");var rocket=(FireworkRocketEntity)fresh.getFirst();
            require(rocket.getOwner()==f.owner&&rocket.isShotAtAngle()&&expected.equals(rocket.getItem().get(DataComponents.FIREWORKS)),"actual source owner/angle/exact private fireworks");
            require(rocket.getDeltaMovement().length()>1.55&&rocket.getDeltaMovement().length()<1.65&&weapon.getDamageValue()==3,"native rocket speed and durability");
            require(ammo.getCount()==1&&weapon.get(DataComponents.CHARGED_PROJECTILES).isEmpty(),"native shot clears chamber without second ammo debit");f.discard(rocket);
            if(++kind==3)return true;charge();return false;
        }
        public void close(){if(closed)return;closed=true;try{f.owner.stopUsingItem();stats.close();}finally{f.close();}}
    }
}
