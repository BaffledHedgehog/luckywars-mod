package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Real raw-hand input and loaded dropped copier, including a normal elapsed server tick. */
public final class LooseRewardChecks {
    private LooseRewardChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Loose rewards: "+why);}
    static ItemStack raw(Item item,String flag,int count){var stack=new ItemStack(item,count);var nbt=new CompoundTag();nbt.putInt(flag,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(nbt));return stack;}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final LbcLooseRewards service;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final List<ItemEntity> owned=new ArrayList<>();
        ItemEntity copier,donor;ItemStack payload;int start;boolean closed;
        Fixture(GameController game){this.game=game;base=new AllGrimoireChecks.Fixture(game);service=game.items().looseRewards();base.level.getAllEntities().forEach(before::add);}
        void ack(){var p=base.base.owner;p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var packets=base.base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
            if(!packets.isEmpty())p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(packets.getLast().id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));}
        List<ItemEntity> drops(){var result=new ArrayList<ItemEntity>();for(var e:base.level.getAllEntities())if(e instanceof ItemEntity i&&!i.isRemoved()&&!before.contains(i))result.add(i);return result;}
        void clearDrops(){for(var item:drops()){service.unloaded(item);game.items().entityUnloaded(item);item.discard();}}
        void lucky(){
            var p=base.base.owner;game.config().offhandWands=false;p.getInventory().clearContent();
            for(var hand:InteractionHand.values()){
                game.config().offhandWands=hand==InteractionHand.OFF_HAND;var wand=raw(Items.CARROT_ON_A_STICK,"luckyblockwand",2);
                p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(hand,wand);ack();
                var rng=MatchRandomState.capture(base.level.getRandom());var expected=new ArrayList<>(game.lbc().rollTable(base.level,p.position(),p,"lbc:luckyblock"));rng.restore(base.level.getRandom());
                require(game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:luckyblock")))!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"original complete reward table is resident");int mana=game.items().mana().get(p);
                require(p.gameMode.useItem(p,base.level,wand,hand).consumesAction(),"actual raw lucky wand input "+hand);
                var actual=drops();require(actual.size()==expected.size(),"source loot entry count "+hand);
                for(var item:actual){int found=-1;for(int i=0;i<expected.size();i++)if(ItemStack.matches(item.getItem(),expected.get(i))){found=i;break;}
                    require(found>=0,"actual original table components/count "+hand);expected.remove(found);require(item.getOwner()==p,"causal source is actual user");}
                require(wand.getCount()==2&&game.items().mana().get(p)==mana,"no invented item/mana debit");clearDrops();
            }
            game.config().offhandWands=false;p.setItemInHand(InteractionHand.OFF_HAND,raw(Items.CARROT_ON_A_STICK,"luckyblockwand",1));
            require(!game.items().wands().use(p,InteractionHand.OFF_HAND)&&drops().isEmpty(),"disabled offhand cannot roll");
        }
        ItemEntity drop(ItemStack stack,Vec3 pos){var item=new ItemEntity(base.level,pos.x,pos.y,pos.z,stack);item.setNoGravity(true);item.setNeverPickUp();item.setThrower(base.base.owner);require(base.level.addFreshEntity(item),"native item insertion");owned.add(item);return item;}
        void begin(){lucky();var at=base.at.add(0,4,8);
            var empty=drop(raw(Items.REPEATING_COMMAND_BLOCK,"copypaster",2),at.add(5,0,0));require(!service.copy(empty)&&empty.getItem().getCount()==2,"no donor leaves copier intact");empty.discard();
            payload=new ItemStack(Items.DIAMOND,7);payload.set(DataComponents.CUSTOM_NAME,Component.literal("copied components"));
            donor=drop(payload.copy(),at.add(.7,0,0));copier=drop(raw(Items.REPEATING_COMMAND_BLOCK,"copypaster",2),at);copier.setDeltaMovement(.1,.2,.3);
            require(service.count()>=1,"loaded copier enrolled by real callback");start=game.server().getTickCount();
        }
        boolean step(){
            int now=game.server().getTickCount();if(now==start&&copier.getItem().getCount()==2)return false;require(now-start<=5,"bounded real copier tick");
            require(!copier.isRemoved()&&copier.getItem().getCount()==1,"one imitator consumed during one native frame: count="+copier.getItem().getCount()+" removed="+copier.isRemoved()+" frame="+(now-start));
            var copies=drops().stream().filter(i->i!=donor&&i!=copier&&ItemStack.matches(i.getItem(),payload)).toList();
            require(copies.size()==1&&ItemStack.matches(donor.getItem(),payload),"whole components/count copied; donor untouched");
            require(copies.getFirst().getDeltaMovement().distanceToSqr(copier.getDeltaMovement())<1e-12,"copy takes copier motion");
            int count=drops().size();service.tick();require(drops().size()==count&&copier.getItem().getCount()==1,"same real tick cannot copy twice");
            return true;
        }
        public void close(){if(closed)return;closed=true;try{clearDrops();}finally{base.close();}}
    }
    public static void start(GameController game){require(running==null,"exclusive native fixture");running=new Fixture(game);try{running.begin();}catch(Throwable failure){running.close();running=null;throw failure;}SkyWars2.LOGGER.info("[check] START loose-rewards");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS loose-rewards (actual original lucky table/two-hand input/no debit; native dropped copier, nearest donor, whole components/count, retained donor, no-target preservation, one copy per native tick)");}catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL loose-rewards",failure);}}
}
