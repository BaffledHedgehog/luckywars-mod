package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native fixtures run inside an ordinary match; no LuckyWars mode is enabled. */
public final class ChestDropChecks {
    private ChestDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static BlockPos stopPos;
    private static MapManager.BlockSnapshot stopSaved;
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Chest drops: "+message);
    }
    private static ItemEntity token(ServerLevel level,Vec3 at,LbcChestDropItem.Kind kind,int count) {
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(kind.item(),count));
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native token added");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final LbcChestDrops service;
        final ServerPlayer alpha;final GameChecks.WandThirdPlayerState savedPlayer;
        final BlockPos playerPos;final MapManager.BlockSnapshot playerBlock;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();final Set<Entity> initial=new HashSet<>();
        final List<ItemEntity> fixtures=new ArrayList<>();
        final LbcChestDropItem.Kind[] kinds=LbcChestDropItem.Kind.values();
        final Vec3 origin=new Vec3(-64.2,260.7,14.8);
        ItemEntity stack,paused,cancelled;int phase,lastTick;
        final List<ItemStack> expectedLazy=new ArrayList<>();
        State(GameController game) {
            this.game=game;level=game.maps().level();service=game.items().chestDrops();
            alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(alpha!=null && game.playing() && game.allowDamage(alpha),"active native match player");
            savedPlayer=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());
            playerPos=alpha.blockPosition();playerBlock=game.maps().snapshot(playerPos);
            level.getAllEntities().forEach(initial::add);service.stop();alpha.closeContainer();alpha.getInventory().clearContent();
            for(int x=-6;x<=0;x++)for(int z=-1;z<=1;z++) {
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}
                level.getChunk(x,z);
            }
            for(int i=0;i<12;i++) save(pos(i));
            save(pos(9).east());
            for(int i=0;i<kinds.length;i++) {
                var kind=kinds[i];var prototype=new ItemStack(kind.item());
                String[] flags={"bastion","end_city","treasure","village","bonus_chest"};
                String[] names={"drop_bastion","drop_end_city","drop_treasure","drop_village","drop_bonus"};
                String[] tables={"bastion_treasure","end_city_treasure","buried_treasure","village/village_weaponsmith","spawn_bonus_chest"};
                require(kind.flag().equals(flags[i]) && kind.nameKey().equals(names[i])
                        && kind.table().identifier().toString().equals("minecraft:chests/"+tables[i]),"independent source identity "+kind);
                var data=prototype.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(data.size()==2 && data.getIntOr(kind.flag(),0)==1 && data.getIntOr("drop",0)==1,"exact two source flags "+kind);
                require(LbcChestDropItem.kind(prototype)==kind,"native token recognition "+kind);
                require(Identifier.parse("lbc:drop_not_trap").equals(prototype.get(DataComponents.ITEM_MODEL))
                        && Identifier.parse("lbc:tier_d").equals(prototype.get(DataComponents.TOOLTIP_STYLE)),"source model/tier "+kind);
                require(prototype.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name
                        && name.getKey().equals(kind.nameKey()),"translated source name "+kind);
                require(Boolean.FALSE.equals(prototype.getHoverName().getStyle().isItalic())
                        && prototype.getHoverName().getStyle().getColor()!=null
                        && prototype.getHoverName().getStyle().getColor().getValue()==0xffff55,"yellow nonitalic name "+kind);
                var lore=prototype.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines();
                require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row
                        && row.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic()
                        && lore.getFirst().getStyle().getColor()!=null && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"white nonitalic source lore "+kind);
                var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/"+kind.id().getPath());
                require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(prototype,loot.getFirst()),"ported loot equals prototype "+kind);
                // Nested command execution completes after this /sw check returns.
                game.command("give TestAlpha "+kind.id()+" 1");
            }
            lastTick=game.server().getTickCount();
        }
        BlockPos pos(int i) {return BlockPos.containing(origin.add(i*4,0,0));}
        void save(BlockPos pos) {
            long chunk=net.minecraft.world.level.ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4);
            if(!level.getForceLoadedChunks().contains(chunk)) {forced.add(chunk);level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}
            level.getChunkAt(pos);blocks.putIfAbsent(pos,game.maps().snapshot(pos));
            // Remove old container inventory without causing fixture-generated drops.
            var be=level.getBlockEntity(pos);if(be instanceof net.minecraft.world.Container c)c.clearContent();
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }
        ItemEntity drop(int index,LbcChestDropItem.Kind kind,int count) {
            var item=token(level,Vec3.atLowerCornerOf(pos(index)).add(.8,.7,.8),kind,count);fixtures.add(item);return item;
        }
        ChestBlockEntity lazy(int index,LbcChestDropItem.Kind kind) {
            var p=pos(index);require(level.getBlockEntity(p) instanceof ChestBlockEntity,"native chest at floor(item.position) "+p);
            var chest=(ChestBlockEntity)level.getBlockEntity(p);
            var tag=chest.saveWithFullMetadata(level.registryAccess());
            require(tag.getStringOr("LootTable","").equals(kind.table().identifier().toString()),"unopened vanilla loot table "+kind+" actual="+tag);
            require(!tag.contains("Items") || tag.getListOrEmpty("Items").isEmpty(),"loot stays lazy until native open "+kind);
            return chest;
        }
        boolean step() {
            require(game.playing(),"match stays active");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase) {
                case 0 -> {
                    for(int i=0;i<kinds.length;i++) require(alpha.getInventory().getItem(i).getCount()==1
                            && ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(i),new ItemStack(kinds[i].item())),"deferred give equals prototype "+kinds[i]);
                    require(service.pendingCount()==0 && game.maps().snapshot(playerPos).equals(playerBlock),"fake /give pickup visuals ignored without altering player block");phase=1;
                }
                case 1 -> {
                    require(service.pendingCount()==0 && blocks.keySet().stream().allMatch(p->level.getBlockState(p).isAir()),"holding tokens creates no chest");
                    alpha.getInventory().clearContent();stack=drop(0,kinds[0],3);phase=2;
                }
                case 2 -> {require(!stack.isRemoved() && stack.getItem().getCount()==2,"stack3 consumes first unit on first native tick");lazy(0,kinds[0]);phase=3;}
                case 3 -> {require(!stack.isRemoved() && stack.getItem().getCount()==1,"stack2 consumes next unit on next native tick");lazy(0,kinds[0]);phase=4;}
                case 4 -> {
                    require(stack.isRemoved(),"stack1 final debit removes entity");lazy(0,kinds[0]);
                    for(int i=1;i<kinds.length;i++)drop(i,kinds[i],1);phase=5;
                }
                case 5 -> {
                    for(int i=0;i<kinds.length;i++) {
                        var chest=lazy(i,kinds[i]);alpha.openMenu(chest);alpha.closeContainer();
                        require(!chest.saveWithFullMetadata(level.registryAccess()).contains("LootTable"),"native open resolves lazy table "+kinds[i]);
                        boolean nonempty=false;for(int slot=0;slot<chest.getContainerSize();slot++)nonempty|=!chest.getItem(slot).isEmpty();
                        require(nonempty,"native open generates actual vanilla loot "+kinds[i]);chest.clearContent();
                    }
                    game.items().timeStop().begin(level,200);paused=drop(5,kinds[2],1);phase=6;
                }
                case 6 -> {
                    require(!paused.isRemoved() && level.getBlockState(pos(5)).isAir(),"arena timestop pauses activation and debit");
                    game.items().timeStop().end(level);
                    var nether=game.server().getLevel(net.minecraft.world.level.Level.NETHER);
                    if(nether!=null)game.items().timeStop().begin(nether,200);phase=7;
                }
                case 7 -> {
                    require(paused.isRemoved(),"another dimension timestop does not pause arena");lazy(5,kinds[2]);
                    var nether=game.server().getLevel(net.minecraft.world.level.Level.NETHER);if(nether!=null)game.items().timeStop().end(nether);
                    cancelled=drop(6,kinds[3],1);service.unloaded(cancelled);phase=8;
                }
                case 8 -> {
                    require(!cancelled.isRemoved() && level.getBlockState(pos(6)).isAir(),"unload cancels queued admission");cancelled.discard();
                    var failure=token(level,new Vec3(origin.x,level.getMaxY()+4,origin.z),kinds[0],2);fixtures.add(failure);
                    require(service.spawn(failure) && failure.getItem().getCount()==1,"failed out-of-height placement still debits exactly one unit");failure.discard();
                    clearDrops();
                    var chest=(ChestBlockEntity)level.getBlockEntity(pos(0));chest.clearContent();chest.setItem(0,new ItemStack(Items.DIAMOND,7));
                    drop(0,kinds[1],1);phase=9;
                }
                case 9 -> {
                    lazy(0,kinds[1]);int diamonds=0;
                    for(var e:level.getAllEntities())if(e instanceof ItemEntity item && !initial.contains(e) && item.getItem().is(Items.DIAMOND))diamonds+=item.getItem().getCount();
                    require(diamonds==7,"replacement ejects old container contents once without duplication");
                    clearDrops();
                    for(int index:new int[]{7,8}) {
                        level.setBlock(pos(index),Blocks.CHEST.defaultBlockState(),3);
                        ((ChestBlockEntity)level.getBlockEntity(pos(index))).setLootTable(kinds[2].table(),991L);
                    }
                    var reference=(ChestBlockEntity)level.getBlockEntity(pos(8));reference.unpackLootTable(null);
                    for(int slot=0;slot<reference.getContainerSize();slot++)if(!reference.getItem(slot).isEmpty())expectedLazy.add(reference.getItem(slot).copy());
                    reference.clearContent();require(!expectedLazy.isEmpty(),"reference native lazy loot nonempty");
                    drop(7,kinds[4],1);phase=10;
                }
                case 10 -> {
                    lazy(7,kinds[4]);
                    var actual=new ArrayList<ItemStack>();
                    for(var e:level.getAllEntities())if(e instanceof ItemEntity item && !initial.contains(e) && !item.isRemoved())actual.add(item.getItem().copy());
                    require(sameBundle(expectedLazy,actual),"replacement materializes old lazy loot once with complete components");clearDrops();
                    var adjacent=pos(9).east();level.setBlock(adjacent,Blocks.CHEST.defaultBlockState(),3);
                    ((ChestBlockEntity)level.getBlockEntity(adjacent)).setItem(0,new ItemStack(Items.DIAMOND,13));
                    drop(9,kinds[0],1);phase=11;
                }
                case 11 -> {
                    lazy(9,kinds[0]);var neighbor=(ChestBlockEntity)level.getBlockEntity(pos(9).east());
                    require(neighbor!=null && neighbor.getItem(0).is(Items.DIAMOND) && neighbor.getItem(0).getCount()==13,"adjacent native chest keeps its own inventory");
                    for(var e:level.getAllEntities())if(e instanceof ItemEntity item && !initial.contains(e) && !item.isRemoved())require(!item.getItem().is(Items.DIAMOND),"adjacent contents were not ejected");
                    service.stop();require(service.pendingCount()==0,"quiet stop clears queue");return true;
                }
                default -> throw new IllegalStateException("Unknown phase "+phase);
            }
            return false;
        }
        void clearDrops() {
            var created=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(!initial.contains(e) && e instanceof ItemEntity)created.add(e);});created.forEach(Entity::discard);
        }
        boolean sameBundle(List<ItemStack> expected,List<ItemStack> actual) {
            var remainder=new ArrayList<ItemStack>();for(var stack:actual)remainder.add(stack.copy());
            for(var wanted:expected) {
                int count=wanted.getCount();
                for(var candidate:remainder)if(ItemStack.isSameItemSameComponents(wanted,candidate)) {int take=Math.min(count,candidate.getCount());count-=take;candidate.shrink(take);}
                if(count!=0)return false;
            }
            return remainder.stream().allMatch(ItemStack::isEmpty);
        }
        public void close() {
            game.items().timeStop().end(level);var nether=game.server().getLevel(net.minecraft.world.level.Level.NETHER);
            if(nether!=null)game.items().timeStop().end(nether);service.stop();alpha.closeContainer();fixtures.forEach(Entity::discard);
            var created=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(!initial.contains(e) && e instanceof ItemEntity)created.add(e);});created.forEach(Entity::discard);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            game.maps().restoreSnapshot(playerPos,playerBlock);savedPlayer.restore(game);
        }
    }
    public static void run(GameController game) {require(running==null,"already running");running=new State(game);}
    public static void tick(GameController game) {
        if(running==null)return;
        try {if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS chest-drops");}}
        catch(Throwable error){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL chest-drops",error);}
    }
    public static void stopProbe(GameController game) {
        require(game.playing(),"stop probe requires actual match");var level=game.maps().level();
        stopPos=new BlockPos(64,260,14);level.getChunkAt(stopPos);stopSaved=game.maps().snapshot(stopPos);
        game.items().timeStop().begin(level,200);
        stopEpoch=game.items().epoch();stopToken=token(level,Vec3.atCenterOf(stopPos),LbcChestDropItem.Kind.BASTION,3);
        require(game.items().chestDrops().pendingCount()>0,"pending admission before real match stop");
        SkyWars2.LOGGER.info("[check] PASS chest-drops-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(!game.items().currentEpoch(stopEpoch) && game.items().chestDrops().pendingCount()==0,"real match stop invalidates epoch and clears queue");
        if(stopToken!=null)stopToken.discard();if(stopSaved!=null)game.maps().restoreSnapshot(stopPos,stopSaved);
        stopEpoch=null;stopToken=null;stopSaved=null;SkyWars2.LOGGER.info("[check] PASS chest-drops-cleanup");
    }
}
