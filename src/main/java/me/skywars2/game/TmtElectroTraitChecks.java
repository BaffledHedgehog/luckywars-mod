package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.phys.Vec3;

/** Real native use, inventory notifications and scheduled owners; no manual self/jump/tick calls. */
public final class TmtElectroTraitChecks {
    private TmtElectroTraitChecks(){}
    private static GameController requested;private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT Electro traits: "+why);}
    private static void near(Vec3 actual,Vec3 expected,String why){require(actual.distanceToSqr(expected)<1e-12,why+" actual="+actual+" expected="+expected);}
    public static void run(GameController game){require(requested==null&&running==null,"exclusive deferred END request");requested=game;}
    public static void tick(GameController game){if(requested!=null){require(requested==game,"same queued controller");requested=null;var scene=new Scene(game);running=scene;
            try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-electro-traits",error);}return;}
        var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-electro-traits (ordinary Energy plus all5 TMT carriers/both hands: actual native use/blocking, exact7 reflection/owner/velocity and outer payload, Apple consumable priority/real finish; foreign source cannot borrow sibling Energy identity; native dirty inventory/equipment Blocker index, exact14 hostile reflection/once-own acceleration/electric0.1 aura, cursor/craft/MAG negatives; real main/offhand Halloween SELF input/private0, source pitch80..90, MAG bypass/TimeStop and cooldown5 source seconds with20tick decrement; no manual owner ticks)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-electro-traits",error);}}
    public static void cancel(GameController game){requested=null;if(running==null)return;var scene=running;running=null;scene.close();}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcElectro electro;
        final NativeMeleeChecks.NativeStats stats;final CompoundTag base=new CompoundTag();final Vec3 origin;
        final Map<ServerPlayer,Integer> cadence,priorCadence;final Set<Entity> accelerated;final Map<Entity,ServerPlayer> reflections;
        final List<Arrow> probes=new ArrayList<>();LbcTmtAbilities.Ability energy,blocker,mace,sibling;
        ItemStack current,before;Arrow inner,boundary,outside,own,ownControl,onceControl;Cow victim;
        int started,when,phase,energyCase=-1,blockerCase,activation;boolean closed;
        float auraBefore;
        @SuppressWarnings("unchecked") Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);electro=game.items().electro();origin=new Vec3(f.home.x,f.level.getMaxY()+16,f.home.z);
            cadence=(Map<ServerPlayer,Integer>)WoolBucketChecks.read(game.items().halloweenWind(),"seconds");priorCadence=new HashMap<>(cadence);
            accelerated=(Set<Entity>)WoolBucketChecks.read(electro,"accelerated");reflections=(Map<Entity,ServerPlayer>)WoolBucketChecks.read(electro,"reflectedOwners");
            var items=new ArrayList<Item>(List.of(LbcElectro.SHIELD,LbcElectro.BLOCKER,LbcHalloweenWind.MACE));for(int n=0;n<5;n++)items.add(LbcTmtItems.prototype(n).getItem());stats=new NativeMeleeChecks.NativeStats(f.owner,items);}
        LbcTmtAbilities.Ability identity(Item item){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(row->LbcTmtComponents.nativeItem(row.data())==item).findFirst().orElseThrow();}
        LbcTmtAbilities.Ability flag(String key){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(row->((ItemData.IntReader)(Object)row.data()).skywars2$readInt(key,0)==1).findFirst().orElseThrow();}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();f.move(f.owner,origin);f.move(f.other,origin.add(32,0,0));
            energy=identity(LbcElectro.SHIELD);blocker=identity(LbcElectro.BLOCKER);mace=identity(LbcHalloweenWind.MACE);sibling=flag("zaika");
            try(var stream=Objects.requireNonNull(getClass().getResourceAsStream("/skywars2/tmt_properties.json"))){var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("baseFlags");data.entrySet().forEach(entry->base.putInt(entry.getKey(),entry.getValue().getAsInt()));}
            catch(Exception error){throw new IllegalStateException("Real TMT SELF source header",error);}
            foreignCandidate();energyBegin();}
        ItemStack combined(int carrier,int count,LbcTmtAbilities.Ability... properties){var stack=LbcTmtItems.prototype(carrier);var data=base.copy();data.merge(stack.get(DataComponents.CUSTOM_DATA).copyTag());LbcTmtAbilities.write(data,List.of(properties));
            data.putInt("energy_shield",0);data.putInt("energyshield",0);data.putInt("electroblocker",0);data.putInt("halloween_mace",0);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));stack.setCount(count);
            stack.set(DataComponents.CUSTOM_NAME,Component.literal("independent Electro carrier"));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("full private sibling"))));stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3))));return stack;}
        void unchanged(int count){require(ItemStack.matches(current,before.copyWithCount(count))&&!ItemData.contains(current,LbcTmtAbilities.ACTIVE)&&!LbcTmtAbilities.active(current)&&!LbcTmtAbilities.nativePredicatesActive(),"real native owner preserves complete physical components/private siblings and no ACTIVE");}
        void clearProbes(){for(var probe:probes)f.discard(probe);probes.clear();if(victim!=null){f.discard(victim);victim=null;}}
        void clear(){clearProbes();f.owner.stopUsingItem();f.owner.getInventory().clearContent();f.owner.inventoryMenu.getCraftSlots().clearContent();f.owner.containerMenu.setCarried(ItemStack.EMPTY);
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))f.owner.setItemSlot(slot,ItemStack.EMPTY);f.removeFields();f.move(f.owner,origin);}
        void foreignCandidate(){var foreign=LbcTmtComponents.source(game,mace);var data=foreign.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("tmt_item",1);LbcTmtAbilities.write(data,List.of(mace,energy));foreign.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            foreign.set(DataComponents.BLOCKS_ATTACKS,LbcTmtComponents.source(game,energy).get(DataComponents.BLOCKS_ATTACKS));var saved=foreign.copy();
            require(LbcElectro.isShield(foreign),"foreign source has a real private Energy sibling positive control");require(!LbcTmtShield.candidate(foreign)&&ItemStack.matches(foreign,saved),"pure selected-native source admission cannot turn MACE into Energy input merely through a sibling");}
        void energyBegin(){clear();int carrier=energyCase<0?-1:energyCase/2;var hand=energyCase>=0&&energyCase%2==1?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            current=carrier<0?new ItemStack(LbcElectro.SHIELD):combined(carrier,carrier==4?2:1,energy,sibling);
            var food=current.get(DataComponents.CONSUMABLE);var durability=current.get(DataComponents.MAX_DAMAGE);
            if(carrier>=0)LbcTmtShield.apply(game,current);
            require(current.has(DataComponents.BLOCKS_ATTACKS)&&current.get(DataComponents.CONSUMABLE)==food&&Objects.equals(current.get(DataComponents.MAX_DAMAGE),durability),"Energy projects real native blocking without replacing physical food/durability");
            before=current.copy();f.owner.setItemInHand(hand,current);if(energyCase==0)f.field(false,origin);
            require(f.owner.getProjectile(current).isEmpty(),"bow/crossbow Energy fallback uses actual no-ammo context");
            require(f.owner.gameMode.useItem(f.owner,f.level,current,hand).consumesAction()&&f.owner.isUsingItem()&&f.owner.getUseItem()==current,"actual native Energy Item.use starts using on carrier="+carrier+" hand="+hand);
            if(carrier==4)require(current.getUseAnimation()==ItemUseAnimation.EAT&&current.getUseDuration(f.owner)==32,"native Apple CONSUMABLE keeps priority and its actual32tick window");
            when=game.server().getTickCount();phase=0;}
        Arrow arrow(double x,ServerPlayer author,boolean gravity){var arrow=new Arrow(EntityTypes.ARROW,f.level);arrow.setOwner(author);arrow.setPos(origin.add(x,0,-.125));arrow.setNoGravity(!gravity);arrow.setDeltaMovement(0,0,.125);
            require(f.level.addFreshEntity(arrow),"native projectile fixture publication");f.own(arrow);probes.add(arrow);return arrow;}
        void energyProbes(){inner=arrow(3,f.owner,false);boundary=arrow(7,f.other,false);outside=arrow(7.01,f.other,false);when=game.server().getTickCount();phase=1;
            if(energyCase==9){f.pause();phase=3;}}
        void reflected(Arrow actor,Arrow control,String why){require(actor.getOwner()==f.owner&&actor.entityTags().contains("skywars2.electro.reflected")&&game.items().currentEpoch(actor),"native reflected owner/epoch "+why);near(actor.getDeltaMovement(),control.getDeltaMovement().reverse(),"exact full native velocity reversal "+why);}
        void blockerBegin(){clear();current=blockerCase==0?new ItemStack(LbcElectro.BLOCKER):combined(0,1,blocker,sibling);
            if(blockerCase==2){current.set(DataComponents.EQUIPPABLE,new ItemStack(Items.LEATHER_HELMET).get(DataComponents.EQUIPPABLE));f.owner.setItemSlot(EquipmentSlot.HEAD,current);}
            else if(blockerCase==3){f.owner.containerMenu.setCarried(current);f.owner.inventoryMenu.getCraftSlots().setItem(0,current.copy());}
            else f.owner.getInventory().setItem(9,current);
            if(blockerCase==4)f.field(false,origin);before=current.copy();when=game.server().getTickCount();phase=5;}
        void blockerProbes(){boundary=arrow(14,f.other,false);outside=arrow(14.01,f.other,false);own=arrow(9,f.owner,true);ownControl=arrow(16,f.owner,true);
            victim=new Cow(EntityTypes.COW,f.level);victim.setPos(origin.add(2,0,0));victim.setNoAi(true);victim.setNoGravity(true);require(f.level.addFreshEntity(victim),"native aura living witness");auraBefore=victim.getHealth();f.own(victim);when=game.server().getTickCount();phase=6;}
        int windCooldown(){return game.items().mana().score(f.owner,"cooldown_wind_charge").get();}
        void maceHeld(InteractionHand hand){clear();game.items().halloweenWind().purify(f.owner);current=combined(0,1,mace,sibling);before=current.copy();f.owner.setItemInHand(hand,current);}
        void actualMace(InteractionHand hand,float pitch,boolean expected){f.owner.setXRot(pitch);f.owner.setDeltaMovement(new Vec3(.2,.3,.4));
            f.owner.gameMode.useItem(f.owner,f.level,current,hand);
            near(f.owner.getDeltaMovement(),new Vec3(.2,expected?1.3:.3,.4),"real TMT SELF input preserves native additive mace motion pitch="+pitch+" hand="+hand);require(windCooldown()==(expected?5:0),"real source mace cooldown admission");unchanged(1);f.owner.setDeltaMovement(Vec3.ZERO);f.owner.setKnownMovement(Vec3.ZERO);}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<240,"bounded actual Electro scene phase="+phase+" energy="+energyCase+" blocker="+blockerCase);int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<6)return false;require(f.owner.isUsingItem()&&f.owner.getItemBlockingWith()==current,"real native using ticks reach authored blocking delay");unchanged(before.getCount());energyProbes();return false;}
            if(phase==1){if(elapsed<1)return false;reflected(boundary,outside,"exact radius7 boundary");reflected(inner,outside,"Energy also reflects its own projectile");require(outside.getOwner()==f.other&&!outside.entityTags().contains("skywars2.electro.reflected"),"radius7 exterior stays foreign");
                clearProbes();unchanged(before.getCount());if(energyCase==8){phase=2;return false;}if(++energyCase<10)energyBegin();else blockerBegin();return false;}
            if(phase==2){if(f.owner.isUsingItem())return false;require(current.getCount()==1&&f.owner.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION),"real Apple finishes its finite native consumption with one physical debit");unchanged(1);if(++energyCase<10)energyBegin();return false;}
            if(phase==3){require(boundary.getOwner()==f.other&&inner.getOwner()==f.owner&&!boundary.entityTags().contains("skywars2.electro.reflected"),"real TimeStop prevents Energy reflection quantum");if(elapsed<2)return false;f.thaw();when=game.server().getTickCount();phase=1;return false;}
            if(phase==5){boolean expected=blockerCase!=3;if(elapsed<1||electro.blockerActive(f.owner)!=expected){require(elapsed<4,"native inventory-change dirty cache reaches exact Blocker equipment/inventory state");return false;}
                unchanged(current.getCount());blockerProbes();return false;}
            if(phase==6){if(elapsed<1)return false;boolean active=blockerCase!=3&&blockerCase!=4;
                if(active){reflected(boundary,outside,"exact Blocker radius14");require(outside.getOwner()==f.other,"Blocker excludes radius14 exterior");near(own.getDeltaMovement(),ownControl.getDeltaMovement().scale(2),"actual own projectile acceleration doubles whole native motion once");require(own.isNoGravity()&&own.getOwner()==f.owner&&accelerated.contains(own),"existing owner owns one-time acceleration/gravity state");
                    require(Math.abs(victim.getHealth()-(auraBefore-.1f))<.00001&&victim.getLastDamageSource()!=null&&victim.getLastDamageSource().is(LbcDamage.ELECTRIC)&&victim.getLastDamageSource().getEntity()==f.owner,"actual native0.1 hostile aura uses shared electric damage/caster");
                    onceControl=arrow(17,f.owner,false);onceControl.setDeltaMovement(own.getDeltaMovement());when=game.server().getTickCount();phase=7;return false;
                }else{require(boundary.getOwner()==f.other&&victim.getHealth()==auraBefore,"cursor/craft or MAG never creates active Blocker reflection/aura");near(own.getDeltaMovement(),ownControl.getDeltaMovement(),"inactive Blocker does not accelerate own projectile");}
                unchanged(current.getCount());if(++blockerCase<5)blockerBegin();else{maceHeld(InteractionHand.MAIN_HAND);actualMace(InteractionHand.MAIN_HAND,79,false);when=game.server().getTickCount();phase=8;}return false;}
            if(phase==7){if(elapsed<1)return false;near(own.getDeltaMovement(),onceControl.getDeltaMovement(),"later native quantum never doubles the same projectile again");unchanged(current.getCount());if(++blockerCase<5)blockerBegin();return false;}
            if(phase==8){if(elapsed<1)return false;actualMace(InteractionHand.MAIN_HAND,80,true);activation=game.server().getTickCount();when=activation;phase=9;return false;}
            if(phase==9){int ticks=game.server().getTickCount()-activation;require(windCooldown()==(ticks<20?5:4),"source cooldown5 decrements on20native-tick second boundary, never after5ticks");
                if(ticks==1){f.owner.setDeltaMovement(Vec3.ZERO);f.owner.gameMode.useItem(f.owner,f.level,current,InteractionHand.MAIN_HAND);near(f.owner.getDeltaMovement(),Vec3.ZERO,"real repeated SELF input is blocked by cooldown");}
                if(ticks<20)return false;require(ticks==20,"first exact native cooldown second observed");maceHeld(InteractionHand.OFF_HAND);f.owner.setXRot(90);f.pause();when=game.server().getTickCount();phase=10;return false;}
            if(phase==10){f.owner.gameMode.useItem(f.owner,f.level,current,InteractionHand.OFF_HAND);require(windCooldown()==0,"actual TimeStop blocks native TMT SELF entry");if(elapsed<2)return false;f.thaw();f.field(false,origin);actualMace(InteractionHand.OFF_HAND,90,true);f.pause();when=game.server().getTickCount();phase=11;return false;}
            if(phase==11){require(windCooldown()==5,"existing TimeStop holds accepted source mace cooldown");if(elapsed<2)return false;f.thaw();unchanged(1);return true;}
            throw new IllegalStateException("Unexpected Electro phase "+phase);}
        public void close(){if(closed)return;closed=true;try{clearProbes();if(f.stopped)f.thaw();f.owner.stopUsingItem();f.removeFields();}
            finally{try{f.close();}finally{stats.close();cadence.clear();cadence.putAll(priorCadence);accelerated.removeIf(actor->f.owned.contains(actor.getUUID()));reflections.keySet().removeIf(actor->f.owned.contains(actor.getUUID()));}}}
    }
}
