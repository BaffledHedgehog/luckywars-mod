package me.skywars2.game;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.resources.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Native catalog, identity and owned-entity fixture. Never builds a plaza in the match map. */
public final class ContentGalleryChecks {
    private ContentGalleryChecks() { }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("Content gallery: " + message);
    }
    public static void run(GameController game) {
        var player = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")), "TestAlpha fixture");
        var level = player.level();
        var origin = player.blockPosition();
        var catalog = new ContentGallery.Catalog(game, level, origin);
        int batches = 0;
        while (!catalog.step(24)) require(++batches < 100000, "finite public-definition traversal");
        var contents = catalog.finish();
        require(contents.stream().filter(stack->stack.is(LbcKfcFood.GOVNO_EATEN)).allMatch(stack->stack.has(DataComponents.TOOLTIP_STYLE)),"gallery fallback uses authored edible-govno style");
        require(catalog.errorCount() == 0, "all public definitions decode successfully");
        require(!contents.isEmpty(), "live public catalog is populated");
        var identities = new HashSet<ContentGallery.Identity>();
        Set<Item> represented = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var stack : contents) {
            require(stack.getCount() == 1 && identities.add(new ContentGallery.Identity(stack)), "one copy per complete native identity");
            represented.add(stack.getItem());
            require(!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")
                    || !ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem())), "no ordinary vanilla novelty section");
        }
        require(!represented.contains(LbcNexusWireItems.BLUE)&&represented.contains(LbcNexusWireItems.WHITE),
                "ready white wire is public while deferred outside green/TMT reward stays internal");
        for (var item : BuiltInRegistries.ITEM) if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("skywars2") && LootTestCatalog.publicNativeItem(item))
            require(represented.contains(item), "every registered native mod item represented: " + BuiltInRegistries.ITEM.getKey(item));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        for (String id : List.of("lbc:ported/phd_certificate", "lbc:ported/pills/pill_0", "lbc:ported/fat_skeleton_spawn_egg",
                "lbc:ported/helicopter_spawn_egg", "lbc:ported/muravei_spawn_egg")) {
            var nativeItems = game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id)))
                    .getRandomItems(params, 268361115L);
            require(nativeItems.size() == 1 && identities.contains(new ContentGallery.Identity(nativeItems.getFirst())), "exact canonical native authored item/egg represented: " + id);
        }
        var base = contents.getFirst().copy();
        var many = base.copyWithCount(37);
        require(new ContentGallery.Identity(base).equals(new ContentGallery.Identity(many)), "quantity does not duplicate a display");
        var modified = base.copy(); modified.set(DataComponents.CUSTOM_NAME, Component.literal("gallery identity fixture"));
        require(!new ContentGallery.Identity(base).equals(new ContentGallery.Identity(modified)), "component variants remain separate");
        var isolatedKey = new ContentGallery.Identity(base);
        base.set(DataComponents.CUSTOM_NAME, Component.literal("later mutation"));
        require(isolatedKey.equals(new ContentGallery.Identity(contents.getFirst())), "identity key owns a defensive copy");

        var layout = new ContentGallery.Layout(level.dimension().identifier().toString(), origin.below(), UUID.randomUUID(), UUID.randomUUID());
        var service = new ContentGallery(game, layout);
        var frames = new ArrayList<ItemFrame>();
        var terrain=new LinkedHashMap<net.minecraft.core.BlockPos,MapManager.BlockSnapshot>();
        var chest=origin.offset(10,0,0);var foreignCell=chest.east();
        terrain.put(chest,game.maps().snapshot(level,chest));terrain.put(foreignCell,game.maps().snapshot(level,foreignCell));
        var inventory = new ArrayList<ItemStack>();
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) inventory.add(player.getInventory().getItem(index).copy());
        try {
            if(ContentGallery.allowed(player))try{
                var sourceRolls=ContentGallery.Catalog.class.getDeclaredField("rolls");sourceRolls.setAccessible(true);
                var liveRolls=ContentGallery.class.getDeclaredField("rolls");liveRolls.setAccessible(true);
                liveRolls.set(service,sourceRolls.get(catalog));
                var shovel=contents.stream().filter(stack->BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("skywars2:random_enchanted_shovel")).findFirst().orElseThrow();
                var randomFrame=ContentGallery.frame(level,origin.above(2),shovel,layout);frames.add(randomFrame);
                require(level.addFreshEntity(randomFrame),"randomized native showroom frame");
                var variants=new HashSet<Integer>();
                for(int roll=0;roll<24;roll++){
                    player.getInventory().clearContent();require(service.useEntity(player,randomFrame),"actual operator right click");
                    var result=player.getInventory().getItem(0);
                    require(result.is(shovel.getItem())&&Objects.equals(result.get(DataComponents.TOOLTIP_STYLE),shovel.get(DataComponents.TOOLTIP_STYLE)),"rerolled item retains authored native identity/tier");
                    variants.add(ItemStack.hashItemAndComponents(result));
                }
                require(variants.size()>1,"right click rerolls enchantments instead of cloning the framed seed");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            require(service.ownsChest(level, layout.chest()) && !service.ownsChest(level, layout.chest().east()), "precise owned central chest position");
            require(!service.useBlock(player, layout.chest().east()), "foreign block never intercepted");
            require(!service.useEntity(player, player), "foreign entity never intercepted");
            require(ContentGallery.allowed(player) == player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER), "native operator permission gate");
            var source = contents.getFirst().copy();
            var nativeFrame = ContentGallery.frame(level, origin.above(2), source, layout);
            frames.add(nativeFrame);
            require(level.addFreshEntity(nativeFrame), "actual native fixture frame added");
            require(service.owned(nativeFrame) && service.protectEntity(nativeFrame) && nativeFrame.survives(), "owned frame survives without support terrain and is protected from creative attacks");
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            nativeFrame.saveWithoutId(output); var data = output.buildResult();
            require(data.getBooleanOr("Fixed", false) && data.getBooleanOr("Invulnerable", false)
                    && data.getFloatOr("ItemDropChance", 1) == 0, "native fixed/invulnerable/no-drop flags");
            require(ItemStack.matches(nativeFrame.getItem(), source), "framed item preserves canonical native components");
            source.set(DataComponents.CUSTOM_NAME, Component.literal("fixture mutation"));
            require(!ItemStack.matches(source, nativeFrame.getItem()), "frame owns its independent item copy");
            if (!ContentGallery.allowed(player)) {
                require(service.useEntity(player, nativeFrame), "non-operator owned-frame interaction consumed");
                for (int index = 0; index < inventory.size(); index++)
                    require(ItemStack.matches(inventory.get(index), player.getInventory().getItem(index)), "non-operator cannot retrieve a copy");
                service.spawn(player);
                require(service.ownsChest(level, layout.chest()), "non-operator spawn cannot relocate the gallery");
            }
            var foreignLayout = new ContentGallery.Layout(layout.dimension(), layout.center(), UUID.randomUUID(), UUID.randomUUID());
            var foreign = ContentGallery.frame(level, origin.above(3), contents.getFirst(), foreignLayout);
            frames.add(foreign); require(level.addFreshEntity(foreign), "foreign ownership fixture added");
            require(!service.owned(foreign) && !service.useEntity(player, foreign), "foreign frame is never claimed");
            service.close();
            require(!nativeFrame.isRemoved() && !foreign.isRemoved(), "shutdown preserves persistent and foreign display entities");
            game.maps().restoreSnapshot(level,chest,new MapManager.BlockSnapshot(net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),null));
            var container=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chest);
            container.setItem(0,new ItemStack(Items.DIAMOND,3));container.setChanged();
            var before=game.maps().snapshot(level,chest);
            game.maps().restoreSnapshot(level,foreignCell,new MapManager.BlockSnapshot(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),null));
            service.writeFixtureCell(level,chest,net.minecraft.world.level.block.Blocks.STONE_BRICKS);
            service.writeFixtureCell(level,chest,net.minecraft.world.level.block.Blocks.ENDER_CHEST);
            service.writeFixtureCell(level,foreignCell,net.minecraft.world.level.block.Blocks.STONE_BRICKS);
            level.setBlock(foreignCell,net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(),2);
            service.roundTripJournalForChecks();
            // Retiring hundreds of frames must not mutate a live native entity-map iterator.
            for(int i=0;i<600;i++){
                var frame=ContentGallery.frame(level,origin.above(4),contents.getFirst(),layout);
                frames.add(frame);require(level.addFreshEntity(frame),"bulk obsolete-frame fixture admitted");
            }
            service.clearForMatchStart();
            var restored=game.maps().snapshot(level,chest);
            require(restored.state()==before.state() && Objects.equals(restored.entity(),before.entity()),"exact first baseline including container items survives repeated writes and compact journal roundtrip");
            require(level.getBlockState(foreignCell).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK),"cleanup preserves foreign replacement of a gallery-written cell");
            require(nativeFrame.isRemoved() && !foreign.isRemoved(), "match admission removes owned frames and preserves foreign frames");
            require(frames.stream().filter(service::owned).allMatch(ItemFrame::isRemoved),"all600 obsolete frames retire across native linked-map resizes without invalidating traversal");
            require(!service.ownsChest(level,layout.chest()),"suspended gallery no longer intercepts its chest");
            service.tick();
            require(!foreign.isRemoved(),"suspended tick cannot regenerate display placements or remove foreign frames");

            // Invoke the real callback with pending ownership, as Fabric does before publishing a chunk.
            service.writeFixtureCell(level,chest,net.minecraft.world.level.block.Blocks.STONE_BRICKS);
            service.writeFixtureCell(level,foreignCell,net.minecraft.world.level.block.Blocks.STONE_BRICKS);
            service.retireTerrain();
            service.roundTripJournalForChecks();
            for(var pos:List.of(chest,foreignCell)) {
                var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                require(chunk!=null,"cleanup fixture chunk is resident");
                service.chunkLoaded(level,chunk);
            }
            require(level.getBlockState(chest).is(net.minecraft.world.level.block.Blocks.STONE_BRICKS),
                    "CHUNK_LOAD leaves matching pending cells untouched until a normal resident drain");
            level.setBlock(foreignCell,net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(),18);
            service.tick();
            restored=game.maps().snapshot(level,chest);
            require(restored.state()==before.state() && Objects.equals(restored.entity(),before.entity()),
                    "resident tick restores pending exact container baseline after CHUNK_LOAD");
            require(level.getBlockState(foreignCell).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK),
                    "foreign replacement between CHUNK_LOAD and resident drain survives");

            service.writeFixtureCell(level,foreignCell,net.minecraft.world.level.block.Blocks.STONE_BRICKS);
            service.retireTerrain();
            level.setBlock(foreignCell,net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),18);
            var chunk=level.getChunkSource().getChunkNow(foreignCell.getX()>>4,foreignCell.getZ()>>4);
            service.chunkLoaded(level,chunk);
            level.setBlock(foreignCell,net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),18);
            service.tick();
            require(level.getBlockState(foreignCell).is(net.minecraft.world.level.block.Blocks.STONE_BRICKS),
                    "foreign cell observed at CHUNK_LOAD retires cleanup even if later replaced with gallery material");

            level.setBlock(foreignCell,net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(),18);
            service.writeFixtureCell(level,foreignCell,net.minecraft.world.level.block.Blocks.STONE_BRICKS);
            service.retireTerrain();
            service.chunkLoaded(level,chunk);
            // MapManager's native map-write hook fires even for identical-state imported writes.
            service.mapBlockWritten(level,foreignCell);
            level.setBlock(foreignCell,net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),18);
            service.roundTripJournalForChecks();
            service.tick();
            require(level.getBlockState(foreignCell).is(net.minecraft.world.level.block.Blocks.STONE_BRICKS),
                    "same-material imported map write takes ownership before deferred gallery cleanup");
        } finally {
            terrain.forEach((pos,snapshot)->game.maps().restoreSnapshot(level,pos,snapshot));
            for (var frame : frames) { frame.discard(); require(frame.isRemoved(), "only fixture-owned entities cleaned up"); }
            for (int index = 0; index < inventory.size(); index++) player.getInventory().setItem(index, inventory.get(index));
            player.inventoryMenu.broadcastChanges();
            service.close();
        }
    }
}
