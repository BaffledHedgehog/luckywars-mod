package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Native oversized quantities retain standalone components, storage and ordinary transfer limits. */
public final class BulkLootChecks {
    private BulkLootChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bulk loot: "+why);}
    private static LootTable mass(GameController game,int count){
        var json=JsonParser.parseString("""
            {"pools":[{"rolls":1,"entries":[{"type":"minecraft:loot_table","value":"lbc:ported/terra_sword",
            "modifier":[{"type":"minecraft:set_count","count":%d}]}]},
            {"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:apple"}]}]}
            """.formatted(count));
        return LootTable.DIRECT_CODEC.parse(game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();
    }
    private static int swordSlot(Container container,int count,ItemStack original){
        int sword=-1,occupied=0,apples=0;
        for(int slot=0;slot<container.getContainerSize();slot++){
            var stack=container.getItem(slot);if(stack.isEmpty())continue;occupied++;
            if(stack.is(original.getItem())){
                require(sword<0,"bulk swords occupy exactly one slot");sword=slot;
                require(stack.getCount()==count&&ItemStack.isSameItemSameComponents(stack,original),"one complete sword stack retains every original component: container="+container.getClass().getName()+" expected="+count+" actual="+stack.getCount()+" componentsEqual="+ItemStack.isSameItemSameComponents(stack,original));
            }else if(stack.is(Items.APPLE))apples+=stack.getCount();else require(false,"no unexpected filler item");
        }
        require(occupied==2&&apples==1&&sword>=0,"bulk quantity leaves another loot entry its own slot");return sword;
    }
    public static void run(GameController game){
        var owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var saved=new RegenerationBeltChecks.Snapshot(game,owner);var level=owner.level();var pos=owner.blockPosition().above(5);var upper=pos.above();
        require(level.hasChunkAt(pos)&&level.hasChunkAt(upper),"resident storage fixture");var before=game.maps().snapshot(level,pos);var above=game.maps().snapshot(level,upper);
        try(var client=new RegenerationBeltChecks.ScheduledClient(game,owner)){
            owner.stopUsingItem();game.players().get(owner.getUUID()).lbcSkill=0;
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,owner.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,owner).create(LootContextParamSets.CHEST);
            var standalone=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:ported/terra_sword")));
            var original=standalone.getRandomItems(params,52).getFirst();
            require(original.getMaxStackSize()==1&&original.has(DataComponents.MAX_DAMAGE)&&!original.has(DataComponents.UNBREAKABLE),"real standalone Terra sword keeps native breakability and max1");
            for(int count:List.of(64,99)){
                var table=mass(game,count);
                clientStorage(owner,original,count);
                storedContents(owner, original, count);
                for(long seed=0;seed<8;seed++){
                    var result=table.getRandomItems(params,seed);require(result.size()==2,"public loot does not split mass sword quantity");
                    var stack=result.stream().filter(s->s.is(original.getItem())).findFirst().orElseThrow();
                    require(stack.getCount()==count&&ItemStack.isSameItemSameComponents(stack,original),"outer count alone preserves native standalone source");
                }
                var simple=new SimpleContainer(27);table.fill(simple,params,52);swordSlot(simple,count,original);
                for(var block:List.of(Blocks.CHEST,Blocks.BARREL,Blocks.SHULKER_BOX,Blocks.HOPPER,Blocks.FURNACE)){
                    clear(level,pos);level.setBlock(pos,block.defaultBlockState(),2|16|32);
                    var entity=Objects.requireNonNull(level.getBlockEntity(pos));require(entity instanceof Container,"actual native storage block");var container=(Container)entity;
                    table.fill(container,params,52);int slot=swordSlot(container,count,original);
                    var encoded=entity.saveWithFullMetadata(level.registryAccess());container.clearContent();
                    entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),encoded));swordSlot(container,count,original);
                    container.setItem(slot,container.getItem(slot).copy());swordSlot(container,count,original);
                    var damaged=container.getItem(slot).copy();damaged.hurtAndBreak(1,level,null,broken->{});
                    require(damaged.getCount()==count&&damaged.getDamageValue()==original.getDamageValue()+1,"bulk swords still take native durability damage");
                    var brokenItems=new ArrayList<ItemStack>();damaged.hurtAndBreak(damaged.getMaxDamage(),level,null,brokenItems::add);
                    require(damaged.getCount()==count-1&&damaged.getDamageValue()==0,"native break consumes only one sword from the bulk stack");
                    require(brokenItems.size()==1&&brokenItems.getFirst().getCount()==1&&brokenItems.getFirst().isBroken(),"native break callback receives exactly one broken item with its original components");
                    if (block==Blocks.SHULKER_BOX) shulkerDropWire(owner,pos,(ShulkerBoxBlockEntity)entity);
                    if(block==Blocks.CHEST){
                        owner.getInventory().clearContent();owner.closeContainer();require(owner.openMenu((ChestBlockEntity)entity).isPresent(),"actual native chest menu opens");
                        client.acknowledge();var menu=owner.containerMenu;
                        storedSwaps(owner,client,container,slot,count,original);
                        // quickMoveStack performs ONE move. The real input handler owns
                        // repeated moves until the player slots fill or the source empties.
                        owner.connection.handleContainerClick(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),(short)slot,(byte)0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,it.unimi.dsi.fastutil.ints.Int2ObjectMaps.emptyMap(),net.minecraft.network.HashedStack.EMPTY));
                        int moved=0;
                        for(int index=0;index<owner.getInventory().getContainerSize();index++){
                            var item=owner.getInventory().getItem(index);if(item.isEmpty())continue;
                            require(item.is(original.getItem())&&item.getCount()<=item.getMaxStackSize()&&ItemStack.isSameItemSameComponents(item,original),"player transfer uses original max1 and every source component");moved+=item.getCount();
                        }
                        require(moved==36&&container.getItem(slot).getCount()==count-moved,"native Shift-click conserves swords and fills36 legal player slots: moved="+moved+" remainder="+container.getItem(slot).getCount());
                        owner.getInventory().clearContent();owner.closeContainer();
                    }
                }
                var cart=net.minecraft.world.entity.EntityTypes.CHEST_MINECART.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                require(cart instanceof Container,"actual native chest cart");
                try{table.fill((Container)cart,params,52);swordSlot((Container)cart,count,original);}finally{cart.discard();}
            }
            clear(level,pos);clear(level,upper);level.setBlock(pos,Blocks.HOPPER.defaultBlockState(),2|16|32);level.setBlock(upper,Blocks.CHEST.defaultBlockState(),2|16|32);
            var hopper=(HopperBlockEntity)level.getBlockEntity(pos);var chest=(ChestBlockEntity)level.getBlockEntity(upper);chest.setItem(0,original.copyWithCount(64));
            require(HopperBlockEntity.suckInItems(level,hopper)&&chest.getItem(0).getCount()==63&&hopper.getItem(0).getCount()==1,"real hopper extracts one native-max sword without losing the oversized remainder");
            var ordinary=new ArrayList<ItemStack>();LootTable.createStackSplitter(level,ordinary::add).accept(original.copyWithCount(200));
            require(ordinary.size()==200&&ordinary.stream().allMatch(s->s.getCount()==1&&ItemStack.isSameItemSameComponents(s,original)),"quantities beyond native persistent count99 retain ordinary safe splitting");
            SkyWars2.LOGGER.info("[check] PASS bulk-loot (count64/99, original Terra components/durability, native storage save-load and packed shulker contents, legal SWAP, Shift-click and hopper transfer)");
        }finally{
            owner.closeContainer();
            clear(level,pos);clear(level,upper);game.maps().restoreSnapshot(level,upper,above);game.maps().restoreSnapshot(level,pos,before);
            saved.restore(game);saved.restoreAttributes();saved.restoreUse();game.players().save();
        }
    }
    private static void storedSwaps(ServerPlayer owner, RegenerationBeltChecks.ScheduledClient client,
            Container container, int source, int count, ItemStack original) {
        var inventory = owner.getInventory();
        var menu = owner.containerMenu;
        for (int button : new int[]{0, Inventory.SLOT_OFFHAND}) {
            var target = menu.findSlot(inventory, button);
            for (var held : List.of(ItemStack.EMPTY, new ItemStack(Items.STICK))) {
                inventory.setItem(button, held);
                var stored = original.copyWithCount(count);
                container.setItem(source, stored);
                menu.sendAllDataToRemote();
                client.packets().clear();

                // The vanilla client predicts the entire exchange once it sees the packed display stack.
                var predicted = new Int2ObjectOpenHashMap<HashedStack>();
                predicted.put(source, HashedStack.create(held, component -> 0));
                target.ifPresent(slot -> predicted.put(slot, HashedStack.create(stored, component -> 0)));
                owner.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,
                        menu.getStateId(), (short)source, (byte)button, ContainerInput.SWAP, predicted, HashedStack.EMPTY));

                var expectedTarget = held.isEmpty() ? original : held;
                var expectedSource = original.copyWithCount(count - (held.isEmpty() ? 1 : 0));
                require(ItemStack.matches(expectedTarget, inventory.getItem(button))
                                && ItemStack.matches(expectedSource, container.getItem(source)),
                        "number-key SWAP moves one legal sword or preserves both occupied stacks: " + button);
                require(client.packets().stream().anyMatch(packet -> packet instanceof ClientboundContainerSetSlotPacket update
                                && update.getSlot()==source && ItemStack.matches(expectedSource, update.getItem())),
                        "accepted SWAP publishes the actual stored remainder: " + button);
                require(client.packets().stream().anyMatch(packet -> target.isPresent()
                                ? packet instanceof ClientboundContainerSetSlotPacket slotUpdate && slotUpdate.getSlot()==target.getAsInt()
                                        && ItemStack.matches(expectedTarget, slotUpdate.getItem())
                                : packet instanceof ClientboundSetPlayerInventoryPacket inventoryUpdate && inventoryUpdate.slot()==button
                                        && ItemStack.matches(expectedTarget, inventoryUpdate.contents())),
                        "accepted SWAP corrects the predicted hotbar/offhand slot: " + button);
            }
            inventory.setItem(button, ItemStack.EMPTY);
        }
        container.setItem(source, original.copyWithCount(count));
        menu.sendAllDataToRemote();
    }

    private static void shulkerDropWire(net.minecraft.server.level.ServerPlayer owner, net.minecraft.core.BlockPos pos,
            ShulkerBoxBlockEntity box) {
        var level = owner.level();
        var picks = new ItemStack(Items.IRON_PICKAXE,32);
        picks.setDamageValue(6);
        var beds = new ItemStack(Items.BED.pick(DyeColor.RED),16);
        for(boolean mixed:new boolean[]{false,true}){
            box.clearContent();
            if(mixed)box.setItem(0,picks);
            box.setItem(9,beds);
            var expected = mixed ? List.of(picks,beds) : List.of(beds);
            var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos),level,pos,box,owner,new ItemStack(Items.IRON_PICKAXE));
            require(drops.size()==1&&drops.getFirst().is(Items.SHULKER_BOX),"actual shulker drop table returns the packed block");
            var stack = drops.getFirst();
            var actual = stack.copy();
            var stored = stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
            var slots = NonNullList.withSize(27,ItemStack.EMPTY);
            stored.copyInto(slots);
            require(slots.stream().filter(item->!item.isEmpty()).count()==expected.size()
                    &&ItemStack.matches(slots.get(0),mixed?picks:ItemStack.EMPTY)&&ItemStack.matches(slots.get(9),beds),
                    "drop keeps packed bed/tool counts, slots and original components");
            var item = new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack);
            var packet = new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(item.getId(),item.getEntityData().getNonDefaultValues());
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(owner.connection,()->{
                var raw = io.netty.buffer.Unpooled.buffer();
                try {
                    var wire = new net.minecraft.network.RegistryFriendlyByteBuf(raw,owner.registryAccess());
                    net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(wire,packet);
                    require(wire.readableBytes()>0&&wire.readableBytes()<65_536,"exact item entity metadata is valid and compact");
                    // An outbound packet decodes on a vanilla client without Polymer's serverbound restoration context.
                    var decoded = net.fabricmc.fabric.api.networking.v1.context.PacketContext.supplyWithoutContext(
                            ()->net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(wire));
                    require(decoded.id()==item.getId()&&!wire.isReadable(),"client can decode the entire drop packet");
                    var received = decoded.packedItems().stream().map(net.minecraft.network.syncher.SynchedEntityData.DataValue::value)
                            .filter(ItemStack.class::isInstance).map(ItemStack.class::cast).findFirst().orElseThrow();
                    var templates = received.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItems().iterator();
                    var context = owner.connection.getPacketContext();
                    var projected = eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack, context, owner.registryAccess());
                    var projectedTemplates = projected.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItems().iterator();
                    for(var original:expected){
                        require(templates.hasNext()&&projectedTemplates.hasNext(),"client receives a packed tooltip template for each real stack");
                        var planned = projectedTemplates.next();
                        var preview = templates.next().create();
                        // The server-only materialization patch can return a stack the vanilla tooltip would reject.
                        var validation = ItemStack.validateStrict(preview);
                        require(preview.is(original.getItem())&&preview.getCount()==original.getCount()&&!validation.isError(),
                                "wire tooltip template must pass vanilla validation: real="+original+" (max "+original.getMaxStackSize()
                                        +"), projected="+planned.count()+" (max "+planned.getMaxStackSize()+"), wire="+preview
                                        +" (max "+preview.getMaxStackSize()+"), validation="+validation);
                    }
                    require(!templates.hasNext(),"tooltip keeps exactly the real occupied slots");
                    require(ItemStack.matches(actual,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(received, context, owner.registryAccess())),
                            "serverbound restoration resolves the original packed stack instead of the client projection");
                } finally {
                    raw.release();
                }
            });
            require(ItemStack.matches(actual,item.getItem())&&ItemStack.matches(beds,box.getItem(9))
                            &&ItemStack.matches(mixed?picks:ItemStack.EMPTY,box.getItem(0)),
                    "client tooltip projection preserves real components, max stack sizes and counts");
        }
        mergedGroundStackWire(owner, pos);
    }

    private static void mergedGroundStackWire(net.minecraft.server.level.ServerPlayer owner, net.minecraft.core.BlockPos pos) {
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(owner.connection, () -> {
            for (ItemStack actual : List.of(new ItemStack(LbcKfcFood.GOVNO_EATEN, 128), new ItemStack(Items.STONE, 512))) {
                var drop = new net.minecraft.world.entity.item.ItemEntity(owner.level(),pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,actual);
                var packet = new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                        drop.getId(),drop.getEntityData().getNonDefaultValues());
                int serverCount = actual.getCount();
                var raw = io.netty.buffer.Unpooled.buffer();
                try {
                    var wire = new net.minecraft.network.RegistryFriendlyByteBuf(raw,owner.registryAccess());
                    net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(wire,packet);
                    var received = net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(wire);
                    require(received.id()==drop.getId()&&!wire.isReadable(),"native client decodes the complete wide-stack metadata");
                    var view = received.packedItems().stream().map(net.minecraft.network.syncher.SynchedEntityData.DataValue::value)
                            .filter(ItemStack.class::isInstance).map(ItemStack.class::cast).findFirst().orElseThrow();
                    require(!view.isEmpty()&&view.getCount()==99,"world display keeps a legal visible stack instead of Polymer's failed projection");
                    require(drop.getItem()==actual&&actual.getCount()==serverCount,"outgoing data does not change the native drop or its quantity");
                } finally {
                    raw.release();
                }
            }
        });
    }

    private static void storedContents(net.minecraft.server.level.ServerPlayer owner, ItemStack original, int count) {
        ItemStack swords = original.copyWithCount(count);
        swords.setDamageValue(1);
        ItemStack apples = new ItemStack(Items.APPLE, 3);
        ItemContainerContents contents = ItemContainerContents.fromItems(List.of(swords, ItemStack.EMPTY, apples));
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, contents);

        var ops = owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var encoded = ItemStack.CODEC.encodeStart(ops, box).getOrThrow();
        ItemStack saved = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        require(ItemStack.matches(box, saved), "packed box retains its exact components during serialization");
        ItemStack restored = ItemStackTemplate.fromNonEmptyStack(saved).create();
        require(ItemStack.matches(saved, restored), "materializing a box also accepts its stored slot count");
        ItemContainerContents restoredContents = restored.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        NonNullList<ItemStack> slots = NonNullList.withSize(4, ItemStack.EMPTY);
        restoredContents.copyInto(slots);
        require(ItemStack.matches(slots.get(0), swords) && slots.get(0).getMaxStackSize() == 1
                        && slots.get(1).isEmpty() && ItemStack.matches(slots.get(2), apples) && slots.get(3).isEmpty(),
                "packed loot restores one slot, including real maximum, damage, components and empty indices");
        require(ItemStack.matches(restoredContents.copyOne(), swords), "slot zero restores the full quantity");
        var copies = restoredContents.itemCopies().toList();
        require(copies.size() == 3 && ItemStack.matches(copies.get(0), swords) && copies.get(1).isEmpty()
                        && ItemStack.matches(copies.get(2), apples), "slot stream restores all slots in order");
        var nonEmpty = restoredContents.nonEmptyItemCopyStream().toList();
        require(nonEmpty.size() == 2 && ItemStack.matches(nonEmpty.get(0), swords)
                        && ItemStack.matches(nonEmpty.get(1), apples), "nonempty stream restores both stored stacks");
        require(restoredContents.asMutable().toImmutable().equals(contents), "mutable view preserves exact contents");
        require(ItemStack.validateStrict(swords).isError(), "standalone template validation retains the native limit");
    }

    private static void clientStorage(net.minecraft.server.level.ServerPlayer owner,ItemStack original,int count){
        try{
            // Load the UNTRANSFORMED native client container, not the patched server
            // class. Shared ItemStack/Container types still come from the live JVM.
            var name=SimpleContainer.class.getName();byte[] bytes;
            try(var input=SimpleContainer.class.getResourceAsStream("SimpleContainer.class")){require(input!=null,"original vanilla class resource");bytes=input.readAllBytes();}
            var loader=new ClassLoader(SimpleContainer.class.getClassLoader()){
                @Override protected Class<?> loadClass(String wanted,boolean resolve)throws ClassNotFoundException{
                    if(!wanted.equals(name))return super.loadClass(wanted,resolve);
                    synchronized(getClassLoadingLock(wanted)){
                        var type=findLoadedClass(wanted);if(type==null)type=defineClass(wanted,bytes,0,bytes.length);if(resolve)resolveClass(type);return type;
                    }
                }
            };
            var vanilla=(Container)loader.loadClass(name).getConstructor(int.class).newInstance(27);
            vanilla.setItem(0,original.copyWithCount(count));require(vanilla.getItem(0).getCount()==1,"unpatched vanilla client really clamps original max1");
            var source=original.copyWithCount(count);source.setDamageValue(1);var before=source.copy();
            var context=owner.connection.getPacketContext();
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)owner.connection,()->{
                var projected=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(source,TooltipFlag.NORMAL,context,owner.registryAccess());
                vanilla.setItem(0,projected);
                require(vanilla.getItem(0).getCount()==count&&projected.getMaxDamage()==original.getMaxDamage()&&projected.getDamageValue()==1,"vanilla client storage retains visible count and durability");
                require(ItemStack.matches(before,source)&&ItemStack.matches(source,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(projected,context,owner.registryAccess())),"display projection restores exact real max1/count/durability/components");
            });
        }catch(ReflectiveOperationException|java.io.IOException error){throw new IllegalStateException("Unmodified vanilla container witness",error);}
    }
    private static void clear(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){
        var entity=level.getBlockEntity(pos);if(entity instanceof RandomizableContainerBlockEntity random)random.setLootTable(null);if(entity instanceof Container container)container.clearContent();
    }
}
