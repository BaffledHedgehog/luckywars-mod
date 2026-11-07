package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Real Magnum ammo intake and LbcItems' scheduled burst; never calls Magnum.tick manually. */
public final class TmtMagnumAmmoChecks {
    private TmtMagnumAmmoChecks(){}
    private static Scene running;
    private static GameController requested;
    private static final String[] ROUNDS={"standard","incendiary","electric","acid","void","explosive"};
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT Magnum ammo: "+why);}
    /** Command admission only; setup/shooting belongs to GameChecks after this real END's item work. */
    public static void run(GameController game){require(running==null&&requested==null,"exclusive native request");requested=game;}
    public static void tick(GameController game){
        if(requested!=null){require(requested==game,"same queued controller");requested=null;var scene=new Scene(game);running=scene;
            try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-magnum-ammo",error);}return;}
        var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same native controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-magnum-ammo (six modes/all5 private TMT carriers; one native ammo debit, full remainder/siblings/no ACTIVE; native inventory/crafting/cursor order and empty slots; wrong mode, sneak/cooldown/TECH/MAG/TimeStop/offhand; initial shot plus4 actual LbcItems scheduled shots; actual held Magnum+round exhaustion and retained native snowball action; vanilla snowball throws={})",scene.nativeThrows);
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-magnum-ammo",error);}}
    public static void cancel(GameController game){requested=null;if(running==null)return;var scene=running;running=null;scene.close();}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcMagnum magnum;final LbcShooters shooters;
        final Map<Integer,LbcTmtAbilities.Ability> rounds=new HashMap<>();final Set<UUID> original=new HashSet<>();
        final Map<UUID,Integer> fired;final Integer priorFired;final int priorCooldown;
        final NativeMeleeChecks.NativeStats stats;
        LbcTmtAbilities.Ability sibling,gunProperty;ItemStack gun,ammo,before,wrong,inventoryLater,craft,cursor;
        int phase,index,when,started,shotsBefore,ammoCount,order,nativeThrows;boolean closed;
        @SuppressWarnings("unchecked") Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);magnum=game.items().magnum();shooters=game.items().shooters();
            require(magnum.queued(f.owner)==0,"no prior accepted Magnum burst to disturb");fired=(Map<UUID,Integer>)WoolBucketChecks.read(magnum,"fired");priorFired=fired.get(f.owner.getUUID());priorCooldown=shooters.cooldownRemaining(f.owner);
            var items=new ArrayList<Item>();items.add(LbcMagnum.ITEM);for(int n=0;n<5;n++)items.add(LbcTmtItems.prototype(n).getItem());stats=new NativeMeleeChecks.NativeStats(f.owner,items);
            f.level.getAllEntities().forEach(entity->original.add(entity.getUUID()));}
        LbcTmtAbilities.Ability property(String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                .filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();}
        void begin(){f.prepareParticipants();f.owner.setGameMode(GameType.SURVIVAL);started=game.server().getTickCount();
            require(f.owner.teleportTo(f.level,f.home.x,f.level.getMaxY()+16,f.home.z,Set.of(),0,-90,true),"native empty-sky teleport above build height");f.owner.setNoGravity(true);aim();f.client.acknowledge();
            sibling=property("zaika");
            for(var row:game.items().tmt().catalog().families())for(var property:row.variants()){
                if(property.id().equals("native/magnum/ammunition")){int mode=LbcMagnum.roundMode(LbcTmtComponents.source(game,property));if(mode>=0)rounds.putIfAbsent(mode,property);}
                var data=(ItemData.IntReader)(Object)property.data();if(gunProperty==null&&data.skywars2$readInt("magnum",0)==1&&data.skywars2$readInt("mode",0)==0)gunProperty=property;
            }
            require(rounds.size()==6&&rounds.values().stream().map(LbcTmtAbilities.Ability::id).distinct().count()==1,"one native ammo family covers all six actual selectors");
            require(gunProperty!=null,"real admitted native Magnum property for held-source witness");positive();}
        void aim(){f.owner.setShiftKeyDown(false);f.owner.setYRot(0);f.owner.setXRot(-90);f.owner.setDeltaMovement(Vec3.ZERO);f.owner.setKnownMovement(Vec3.ZERO);}
        void clear(){f.owner.stopUsingItem();f.owner.getInventory().clearContent();f.owner.inventoryMenu.getCraftSlots().clearContent();f.owner.containerMenu.setCarried(ItemStack.EMPTY);f.removeFields();}
        ItemStack combined(int carrier,int count,LbcTmtAbilities.Ability... properties){var stack=LbcTmtItems.prototype(carrier);var tag=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            LbcTmtAbilities.write(tag,List.of(properties));for(String round:ROUNDS)tag.putInt(round+"_round",0);tag.putString("magnum_fixture","physical sibling data");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.setCount(count);
            stack.set(DataComponents.CUSTOM_NAME,Component.literal("private native Magnum ammunition"));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("preserved physical lore"))));
            stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3))));
            stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x345678),List.of(),Optional.of("independent physical payload")));return stack;}
        ItemStack ammo(int mode,int carrier,int count){return combined(carrier,count,rounds.get(mode),sibling);}
        ItemStack gun(int mode){var stack=f.leaf("magnum");var tag=stack.get(DataComponents.CUSTOM_DATA).copyTag();tag.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
        void ready(int mode){clear();magnum.purify(f.owner);gun=gun(mode);f.owner.setItemInHand(InteractionHand.MAIN_HAND,gun);aim();shotsBefore=magnum.shots(f.owner);}
        void intact(ItemStack actual,ItemStack expected,String why){require(ItemStack.matches(actual,expected)&&!ItemData.contains(actual,LbcTmtAbilities.ACTIVE),why);
            if(!actual.isEmpty()){var ops=f.owner.registryAccess().createSerializationContext(NbtOps.INSTANCE);var decoded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,actual).getOrThrow()).getOrThrow();require(ItemStack.matches(actual,decoded),"native codec retains full ammo remainder "+why);}}
        void remainder(){if(ammoCount==1)require(f.owner.getInventory().getItem(9).isEmpty(),"native consumed carrier is physically empty");
            else intact(f.owner.getInventory().getItem(9),before.copyWithCount(ammoCount-1),"one native inventory debit retains all independent components and private siblings");}
        void fire(){require(magnum.use(f.owner,InteractionHand.MAIN_HAND),"actual native Magnum accepted source phase="+phase+" case="+index);
            require(magnum.shots(f.owner)==shotsBefore+1&&magnum.queued(f.owner)==4,"accepted native initial shot queues exactly four continuations");when=game.server().getTickCount();}
        void positive(){int mode=index/5,carrier=index%5;ready(mode);
            // An unrelated selected source must never satisfy this Magnum mode.
            wrong=ammo((mode+1)%6,2,2);f.owner.getInventory().setItem(9,wrong);var unchanged=wrong.copy();
            require(!magnum.use(f.owner,InteractionHand.MAIN_HAND)&&magnum.shots(f.owner)==shotsBefore&&magnum.queued(f.owner)==0,"wrong private mode cannot fire or claim a burst mode="+mode+" carrier="+carrier);
            intact(wrong,unchanged,"wrong-mode source remains complete");
            var prototype=LbcTmtItems.prototype(carrier);ammoCount=Math.min(2,prototype.getMaxStackSize());ammo=ammo(mode,carrier,ammoCount);before=ammo.copy();f.owner.getInventory().setItem(9,ammo);
            require(ItemData.intValue(ammo,ROUNDS[mode]+"_round",-1)==0,"outer marker0 forces the genuine private-ammo query");intact(before,before,"full legal input snapshot before native debit");phase=0;fire();remainder();}
        boolean burst(){int elapsed=game.server().getTickCount()-when;require(elapsed>=0&&elapsed<=4,"bounded genuine scheduled burst phase="+phase+" elapsed="+elapsed);
            require(magnum.shots(f.owner)==shotsBefore+1+elapsed&&magnum.queued(f.owner)==4-elapsed,"one continuation per real LbcItems frame, no replayed owner.tick; phase="+phase+" shots="+magnum.shots(f.owner)+" queued="+magnum.queued(f.owner)+" elapsed="+elapsed);
            return elapsed==4;}
        void orderBegin(){ready(0);var empty=ammo(0,2,1);empty.setCount(0);f.owner.getInventory().setItem(9,empty);
            wrong=ammo(1,2,1);f.owner.getInventory().setItem(10,wrong);ammo=ammo(0,0,1);f.owner.getInventory().setItem(11,ammo);
            inventoryLater=ammo(0,2,1);f.owner.getInventory().setItem(18,inventoryLater);craft=ammo(0,4,1);f.owner.inventoryMenu.getCraftSlots().setItem(0,craft);cursor=ammo(0,2,1);f.owner.containerMenu.setCarried(cursor);
            order=0;orderFire();}
        void orderFire(){magnum.purify(f.owner);shotsBefore=magnum.shots(f.owner);phase=1;fire();
            require(f.owner.getInventory().getItem(9).isEmpty()&&wrong.getCount()==1,"empty native slots skipped and wrong-mode stack untouched");
            require(f.owner.getInventory().getItem(11).isEmpty(),"first matching native inventory slot consumed before later inventory/crafting/cursor");
            require(f.owner.getInventory().getItem(18).isEmpty()==(order>=1),"native inventory slot order retained");
            require(f.owner.inventoryMenu.getCraftSlots().getItem(0).isEmpty()==(order>=2),"native crafting search follows complete inventory");
            require(f.owner.containerMenu.getCarried().isEmpty()==(order>=3),"native cursor follows inventory and crafting");
            for(var stack:List.of(inventoryLater,craft,cursor))if(!stack.isEmpty())require(!ItemData.contains(stack,LbcTmtAbilities.ACTIVE),"unconsumed source stays outside all private scopes");}
        void guards(){ready(0);ammo=ammo(0,2,2);before=ammo.copy();f.owner.getInventory().setItem(9,ammo);
            require(!magnum.use(f.owner,InteractionHand.OFF_HAND)&&magnum.shots(f.owner)==shotsBefore&&magnum.queued(f.owner)==0,"native Magnum rejects offhand even when offhandWands enabled");int mana=game.items().mana().get(f.owner);
            shooters.setCooldown(f.owner,7);f.owner.setShiftKeyDown(true);
            for(int n=0;n<6;n++)require(magnum.use(f.owner,InteractionHand.MAIN_HAND),"native sneak cycling during cooldown");
            require(LbcMagnum.mode(gun)==0&&magnum.shots(f.owner)==shotsBefore&&magnum.queued(f.owner)==0&&shooters.cooldownRemaining(f.owner)==7&&game.items().mana().get(f.owner)==mana,"six real free cycles preserve cooldown/mana and never fire");intact(ammo,before,"sneak never debits ammunition");aim();magnum.purify(f.owner);
            f.field(true,f.owner.position());require(!magnum.use(f.owner,InteractionHand.MAIN_HAND)&&magnum.shots(f.owner)==shotsBefore&&magnum.queued(f.owner)==0,"actual TECH blocks source before ammo intake");intact(ammo,before,"TECH rejects without debit");f.removeFields();
            f.pause();require(!magnum.use(f.owner,InteractionHand.MAIN_HAND)&&magnum.shots(f.owner)==shotsBefore&&magnum.queued(f.owner)==0,"canonical TimeStop rejects native input");intact(ammo,before,"TimeStop input preserves ammo");f.thaw();
            phase=2;fire();ammoCount=2;remainder();f.pause();}
        void sameCarrier(boolean snow){ready(0);int carrier=snow?2:0,count=snow?3:1;ammo=combined(carrier,count,gunProperty,rounds.get(0),sibling);before=ammo.copy();f.owner.setItemInHand(InteractionHand.MAIN_HAND,ammo);gun=ammo;
            var ids=new HashSet<UUID>();f.level.getAllEntities().forEach(entity->ids.add(entity.getUUID()));
            require(f.owner.gameMode.useItem(f.owner,f.level,ammo,InteractionHand.MAIN_HAND).consumesAction(),"actual same-carrier Magnum+round native input "+carrier);
            require(magnum.shots(f.owner)==shotsBefore+1&&magnum.queued(f.owner)==4,"same held physical source buys exactly one native initial burst");
            int throwsNow=0;for(var entity:f.level.getAllEntities())if(entity instanceof Snowball&&!ids.contains(entity.getUUID())){throwsNow++;f.own(entity);}
            if(snow){require(throwsNow==1,"literal NativeSnowball.use falls through to exactly one real native snowball after the non-sustained Magnum action");nativeThrows=throwsNow;
                int retained=count-1-throwsNow;require(retained>0&&f.owner.getMainHandItem()==ammo,"legal physical remainder retains original native gun identity");
                intact(ammo,before.copyWithCount(retained),"same-carrier source debits one ammo unit plus each actual native carrier throw, without refunds");phase=5;
            }else{require(throwsNow==0&&f.owner.getMainHandItem().isEmpty(),"count1 carrot source is genuinely exhausted by its own ammo debit");phase=4;}
            when=game.server().getTickCount();}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();aim();require(game.server().getTickCount()-started<280,"bounded actual Magnum ammo scene phase="+phase+" case="+index);
            if(phase==0){if(!burst())return false;remainder();require(shooters.cooldownRemaining(f.owner)>0,"last native shot owns real shared cooldown");
                require(!magnum.use(f.owner,InteractionHand.MAIN_HAND)&&magnum.shots(f.owner)==shotsBefore+5&&magnum.queued(f.owner)==0,"completed burst cooldown rejects a new native source");remainder();index++;
                if(index<30)positive();else orderBegin();return false;}
            if(phase==1){if(!burst())return false;if(++order<4)orderFire();else guards();return false;}
            if(phase==2){int elapsed=game.server().getTickCount()-when;require(magnum.shots(f.owner)==shotsBefore+1&&magnum.queued(f.owner)==4,"real TimeStop preserves accepted four-shot continuation without owner ticks");remainder();if(elapsed<3)return false;f.thaw();phase=3;when=game.server().getTickCount();return false;}
            if(phase==3){if(!burst())return false;remainder();ready(0);ammo=ammo(0,2,2);before=ammo.copy();ammoCount=2;f.owner.getInventory().setItem(9,ammo);f.field(false,f.owner.position());phase=6;fire();remainder();return false;}
            if(phase==4){if(game.server().getTickCount()==when)return false;require(magnum.shots(f.owner)==shotsBefore+1&&magnum.queued(f.owner)==0,"native retained-gun check cancels queued shots when count1 physical weapon was consumed");sameCarrier(true);return false;}
            if(phase==5){if(!burst())return false;require(f.owner.getMainHandItem()==ammo&&ammo.getCount()==3-1-nativeThrows,"four real scheduled shots retain legal same-carrier identity and never consume additional ammo");return true;}
            if(phase==6){if(!burst())return false;remainder();f.removeFields();sameCarrier(false);return false;}
            throw new IllegalStateException("Unexpected Magnum ammo phase "+phase);}
        public void close(){if(closed)return;closed=true;
            try{magnum.forget(f.owner);shooters.resetCooldown(f.owner);f.removeFields();if(f.stopped)f.thaw();
                for(var entity:f.level.getAllEntities())if(!original.contains(entity.getUUID())&&entity instanceof Projectile)f.own(entity);
            }finally{try{f.close();}finally{stats.close();shooters.setCooldown(f.owner,priorCooldown);if(priorFired==null)fired.remove(f.owner.getUUID());else fired.put(f.owner.getUUID(),priorFired);}}}
    }
}
