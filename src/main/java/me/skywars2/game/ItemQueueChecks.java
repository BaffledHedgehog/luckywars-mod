package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** Real item bodies cross the live cap, preserve native payloads, and return in newest-first order. */
public final class ItemQueueChecks {
    private ItemQueueChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Item queue: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var queue=game.itemQueue();var level=f.level;
            var old=queue.checkpoint();var actors=MatchActorCheckpoint.captureRegistered(game);
            var drops=new ArrayList<ItemEntity>();
            try{
                require(queue.dormant(level)==0,"no borrowed dormant items in fixture dimension");
                for(int i=0;i<1006;i++){
                    var stack=new ItemStack(Items.PAPER,17);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Dormant native "+i));
                    var item=new ItemEntity(level,f.owner.getX(),f.owner.getY()+12,f.owner.getZ(),stack);
                    item.setNoGravity(true);item.setPickUpDelay(0);item.setThrower(f.owner);item.setDeltaMovement(99,.02,.03);item.setExtendedLifetime();
                    item.entityTags().add("queue-fixture-"+i);require(level.addFreshEntity(item),"actual native drop publication "+i);drops.add(item);
                }
                int count=queue.active(level);
                for(int i=0;i<200&&queue.active(level)>LbcItemQueue.LIMIT;i++)queue.endTick();
                require(queue.active(level)==1000&&queue.dormant(level)==count-1000,"world cap moves excess bodies into saved archive");
                require(drops.getFirst().isRemoved()&&!drops.getLast().isRemoved(),"oldest sleep while newest native drops remain");
                // Encode/reopen through the exact codec used by native dimension SavedData storage.
                var field=LbcItemQueue.class.getDeclaredField("TYPE");field.setAccessible(true);
                @SuppressWarnings("unchecked") var type=(net.minecraft.world.level.saveddata.SavedDataType<net.minecraft.world.level.saveddata.SavedData>)field.get(null);
                var store=level.getDataStorage().computeIfAbsent(type);var ops=level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                var encoded=type.codec().encodeStart(ops,store).getOrThrow();
                var bytes=new java.io.ByteArrayOutputStream();net.minecraft.nbt.NbtIo.writeCompressed((net.minecraft.nbt.CompoundTag)encoded,bytes);
                var reopened=net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()),net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                require(type.codec().encodeStart(ops,type.codec().parse(ops,reopened).getOrThrow()).getOrThrow().equals(encoded),"native saved-data codec and actual compressed NBT stream preserve every dormant entry");
                var hidden=drops.stream().filter(ItemEntity::isRemoved).toList();var newestHidden=hidden.getLast();
                // Native pickup frees one actual entity slot; no fake counter decrement.
                var picked=drops.getLast();picked.playerTouch(f.owner);
                require(picked.isRemoved(),"real inventory pickup removes native newest body");
                for(int i=0;i<8&&level.getEntity(newestHidden.getUUID())==null;i++)queue.endTick();
                var returned=level.getEntity(newestHidden.getUUID());require(returned instanceof ItemEntity,"newest dormant drop returns first");
                var item=(ItemEntity)returned;
                require(item!=newestHidden&&ItemStack.matches(item.getItem(),newestHidden.getItem())
                        &&item.getOwner()==f.owner&&item.isNoGravity()&&item.getAge()==newestHidden.getAge()&&item.getDeltaMovement().equals(newestHidden.getDeltaMovement())
                        &&item.entityTags().containsAll(newestHidden.entityTags()),"new native body retains full stack/components, thrower, tags, velocity and gravity");
                require(queue.active(level)==1000&&queue.dormant(level)==count-1001,"return conserves exact live+sleeping physical drop count after pickup");
                var saved=queue.checkpoint();queue.roundEnded();require(queue.dormant(level)==0,"round reset removes sleeping old-round loot");
                queue.restore(saved);require(queue.dormant(level)==count-1001,"checkpoint rewind restores dormant queue without inventing new loot");
                queue.terrainReplaced(level,List.of(new net.minecraft.world.level.levelgen.structure.BoundingBox(7000,0,7000,7001,1,7001)));
                require(queue.dormant(level)==count-1001,"unrelated map replacement leaves dormant loot intact");
                var at=newestHidden.blockPosition();queue.terrainReplaced(level,List.of(new net.minecraft.world.level.levelgen.structure.BoundingBox(at.getX()-1,at.getY()-1,at.getZ()-1,at.getX()+1,at.getY()+1,at.getZ()+1)));
                require(queue.dormant(level)==0,"same-round replacement retires sleeping loot from the old terrain");
            }catch(ReflectiveOperationException|java.io.IOException error){throw new IllegalStateException(error);}finally{
                game.server().tickRateManager().setFrozen(true);
                try{actors.restoreBodiesFrozen();require(actors.restoreLinksFrozen().isEmpty(),"native pre-fixture bodies restored");queue.restore(old);}
                finally{game.server().tickRateManager().setFrozen(false);}
            }
        }
        SkyWars2.LOGGER.info("[check] PASS item-queue (1006 real native drops; newest1000 active; compressed archive; native pickup/ordered revival; exact components/author/gravity/velocity/tags; count conservation; round reset and checkpoint replay)");
    }
}
