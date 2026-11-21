package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ArenaChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/** Native action/loot/worker/cache probes. These intentionally test the author's tag-based
 * rework, never the obsolete216-counter/cube draft. No benchmark or client claim. */
public final class ImbaAxeChecks {
    private ImbaAxeChecks() { }
    private static Fixture running;
    private static boolean observing;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("IMBA Axe: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){
        require(running==null,"exclusive fixture");
        if(!observing){observing=true;ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var fixture=running;if(fixture!=null)fixture.outputPublished(entity,level);
        });}
        var f=new Fixture(game,rewind);running=f;
        try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(game==f.game,"same controller");if(!f.step())return;running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS {} (real break/melee; copied-palette worker; native chest/shulker/lectern/campfire loot; dirty/clean cache; owned physical delivery{})",f.rewind?"imba-axe-rewind":"imba-axe",f.rewind?"; nonempty whole-coordinator replay":"");
        }catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL imba-axe",error);}
    }
    private static int count(List<ItemStack> stacks,Item item){
        int total=0;
        for(var stack:stacks){if(stack.is(item))total+=stack.getCount();var packed=stack.get(DataComponents.CONTAINER);
            if(packed!=null)total+=count(packed.nonEmptyItemCopyStream().toList(),item);}
        return total;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final ServerPlayer owner,other;final ServerLevel level;final LbcImbaAxe service;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final RegenerationBeltChecks.ScheduledClient client,audience;
        final MatchCoreCheckpoint core;final MatchWorldCheckpoint world;final MatchWorldRandomCheckpoint random;
        final MatchRandomState ownerRandom,otherRandom;
        final boolean frozen;final Vec3 origin;final int began;
        final Set<UUID> before=new HashSet<>(),observed=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final List<BlockPos> targets=new ArrayList<>();final String markerName="imba-fixture-"+UUID.randomUUID();
        ItemStack axe;Marker jammer;BlockPos trigger,dirty;int stage,when;boolean stopped,closed,restored,captureArmed;
        CompletableFuture<Integer> pure;List<LbcImbaAxe.View> captured;LbcImbaAxe.Metrics baseline,initialMetrics;
        Map<BlockPos,BlockState> capturedTerrain;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();service=game.items().imbaAxe();
            require(game.playing()&&game.canUseGameplayItems(owner)&&LbcDimensionItems.ordinary(level)&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"stable actual ordinary match");
            require(service.count()==0&&game.items().stonekivi().queuedSnapshot().isEmpty(),"exclusive empty shared terrain queue");
            core=new MatchCoreCheckpoint(game);world=new MatchWorldCheckpoint(game);random=new MatchWorldRandomCheckpoint(game);
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
            ownerRandom=MatchRandomState.capture(owner.getRandom());otherRandom=MatchRandomState.capture(other.getRandom());
            frozen=game.server().tickRateManager().isFrozen();origin=new Vec3(owner.getX(),level.getMaxY()-24,owner.getZ());began=game.server().getTickCount();
            level.getAllEntities().forEach(e->before.add(e.getUUID()));
            captureLoadedUndo();
        }
        void captureLoadedUndo(){
            // Fixture-only undo over ALL resident chunks, not an arena/cube approximation.
            // Repeat before later actions so newly resident cells are included as well.
            for(var holder:((ArenaChunkAccess)level.getChunkSource().chunkMap).skywars2$visibleChunks().values())
                if(holder.getLatestChunk() instanceof LevelChunk chunk)for(int s=0;s<chunk.getSectionsCount();s++){
                    var section=chunk.getSection(s);if(section.hasOnlyAir()||!section.maybeHas(service::effective))continue;
                    for(int cell=0;cell<4096;cell++)if(service.effective(section.getBlockState(cell&15,cell>>8,(cell>>4)&15)))
                        remember(new BlockPos((chunk.getPos().x()<<4)+(cell&15),(chunk.getSectionYFromSectionIndex(s)<<4)+(cell>>8),(chunk.getPos().z()<<4)+((cell>>4)&15)));
                }
        }
        void remember(BlockPos at){terrain.putIfAbsent(at.immutable(),game.maps().snapshot(level,at));}
        void block(BlockPos at,BlockState state){remember(at);require(level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)!=null,"fixture uses resident chunks only");require(level.setBlock(at,state,818),"fixture block install");}
        BlockPos at(int x,int z){return BlockPos.containing(origin).offset(x,0,z);}
        void next(int n){stage=n;when=game.server().getTickCount();}
        List<ItemStack> harvest(BlockPos at){return service.harvest(owner,level,at,level.getBlockState(at),axe,game.items().indirectKills().wildCause(owner));}
        void kernels(){
            var chest=at(3,0);block(chest,Blocks.CHEST.defaultBlockState());((Container)level.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,11));
            var loot=harvest(chest);require(loot!=null&&count(loot,Items.CHEST)==1&&count(loot,Items.DIAMOND)==11,"native chest and exact contents once");
            var shulker=at(4,0);block(shulker,Blocks.SHULKER_BOX.defaultBlockState());((Container)level.getBlockEntity(shulker)).setItem(0,new ItemStack(Items.EMERALD,17));
            loot=Block.getDrops(level.getBlockState(shulker),level,shulker,level.getBlockEntity(shulker),owner,axe);
            require(count(loot,Items.SHULKER_BOX)==1&&count(loot,Items.EMERALD)==17,"native shulker loot already packs its contents once");
            require(harvest(shulker)==null&&((Container)level.getBlockEntity(shulker)).getItem(0).getCount()==17,"non-axe-effective shulker and packed contents remain intact");
            var furnace=at(5,0);block(furnace,Blocks.FURNACE.defaultBlockState());((Container)level.getBlockEntity(furnace)).setItem(0,new ItemStack(Items.COAL,13));
            require(harvest(furnace)==null&&((Container)level.getBlockEntity(furnace)).getItem(0).getCount()==13,"non-axe-effective furnace and contents remain intact");
            var lectern=at(6,0);block(lectern,Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.HAS_BOOK,true));
            ((LecternBlockEntity)level.getBlockEntity(lectern)).setBook(new ItemStack(Items.WRITABLE_BOOK));
            loot=harvest(lectern);require(loot!=null&&count(loot,Items.WRITABLE_BOOK)==1,"native lectern non-Container book ejection captured once");
            var campfire=at(7,0);block(campfire,Blocks.CAMPFIRE.defaultBlockState());((CampfireBlockEntity)level.getBlockEntity(campfire)).getItems().set(0,new ItemStack(Items.BEEF,3));
            loot=harvest(campfire);require(loot!=null&&count(loot,Items.BEEF)==3,"native campfire non-Container cooking contents captured once");
            var bamboo=at(8,0);block(bamboo,Blocks.BAMBOO_MOSAIC.defaultBlockState());loot=harvest(bamboo);
            require(loot!=null&&count(loot,Items.BAMBOO_MOSAIC)==1,"native bamboo loot has no obsolete duplicate output row");
            var consumed=at(10,0);block(consumed,LbcConsumedBlock.BLOCK.defaultBlockState());loot=harvest(consumed);
            require(loot!=null&&loot.isEmpty()&&level.getBlockState(consumed).isAir(),"exact native black consumed block is axe-effective and retains empty native loot");
            var uniform=new ArrayList<ItemStack>();int chunkX=(owner.getBlockX()>>4)<<4,chunkZ=(owner.getBlockZ()>>4)<<4;
            for(int i=0;i<64;i++){
                var cell=new BlockPos(chunkX+(i&7),(int)origin.y+2,chunkZ+(i>>3));block(cell,Blocks.OAK_PLANKS.defaultBlockState());
                var nativeLoot=harvest(cell);require(nativeLoot!=null,"native identical-block harvest accepted");uniform.addAll(nativeLoot);
            }
            var packed=LbcImbaAxe.compactLoot(uniform);
            require(count(uniform,Items.OAK_PLANKS)==64&&packed.size()==1&&packed.getFirst().getCount()==64&&count(packed,Items.OAK_PLANKS)==64,
                    "64 real identical block loot results become one legal physical-output stack with all quantities preserved");
            require(uniform.stream().allMatch(stack->stack.getCount()==1),"packing never mutates native input stacks");
            var named=new ItemStack(Items.PAPER,63);named.set(DataComponents.CUSTOM_NAME,Component.literal("distinct-first"));
            var matching=named.copyWithCount(2);var distinct=named.copyWithCount(3);distinct.set(DataComponents.CUSTOM_NAME,Component.literal("distinct-second"));
            var mixed=LbcImbaAxe.compactLoot(List.of(named,matching,distinct));
            require(mixed.size()==3&&mixed.get(0).getCount()==64&&mixed.get(1).getCount()==1&&mixed.get(2).getCount()==3
                    &&mixed.get(0).get(DataComponents.CUSTOM_NAME).getString().equals("distinct-first")
                    &&mixed.get(2).get(DataComponents.CUSTOM_NAME).getString().equals("distinct-second"),"native component identity preserves distinct contents and splits only at legal stack maximum");
            var stale=at(9,0);block(stale,Blocks.BIRCH_PLANKS.defaultBlockState());
            require(service.harvest(owner,level,stale,Blocks.OAK_PLANKS.defaultBlockState(),axe,game.items().indirectKills().wildCause(owner))==null&&level.getBlockState(stale).is(Blocks.BIRCH_PLANKS),"stale expected state cannot remove a replacement");
            for(var kind:LbcLuckyBlocks.Kind.values())require(service.effective(LbcLuckyBlocks.block(kind).defaultBlockState()),"all three canonical native Lucky blocks qualify "+kind);
            require(!service.effective(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState()),"unknown black-empty identity is never guessed from vanilla black concrete");
            var escaped=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(e instanceof ItemEntity item&&!before.contains(e.getUUID()))escaped.add(item);
            require(escaped.isEmpty(),"supporting native removal buffers contents without publishing physical items at source");
            var ordinary=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(Items.PAPER));require(!LbcImbaAxe.captureDrop(level,ordinary),"native ItemEntity capture scope is already closed");ordinary.discard();
        }
        void begin(){
            for(var p:List.of(owner,other)){p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
            require(owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true),"native participant scene teleport");client.acknowledge();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)block(at(x,z).below(),Blocks.STONE.defaultBlockState());
            var source=game.lbc().rollTable(level,origin,owner,"lbc:ported/imba_axe");require(source.size()==1&&LbcImbaAxe.recognized(source.getFirst()),"real source leaf yields exact registered native axe");axe=source.getFirst();
            require(!LbcImbaAxe.recognized(new ItemStack(Items.NETHERITE_AXE)),"ordinary netherite axe has no IMBA handler");
            var detached=new net.minecraft.world.level.chunk.LevelChunkSection(net.minecraft.world.level.chunk.PalettedContainerFactory.create(level.registryAccess()));
            detached.setBlockState(0,0,0,Blocks.BAMBOO_MOSAIC.defaultBlockState());var frozenCopy=detached.copy();detached.setBlockState(0,0,0,Blocks.STONE.defaultBlockState());
            var eligible=Set.of(Blocks.BAMBOO_MOSAIC);var token=new AtomicLong();
            pure=game.maps().computeFromSnapshot(()->LbcImbaAxe.scan(frozenCopy,eligible,token,0).size());
            kernels();
            // Large enough to capture an actual partial candidate/output cursor, in already loaded cells.
            int baseX=(owner.getBlockX()>>4)<<4,baseZ=(owner.getBlockZ()>>4)<<4,y=(int)origin.y+4;
            for(int i=0;i<1024;i++){var cell=new BlockPos(baseX+(i&15),y+(i>>8),baseZ+((i>>4)&15));block(cell,Blocks.OAK_PLANKS.defaultBlockState());targets.add(cell);}
            var chest=at(3,2);block(chest,Blocks.CHEST.defaultBlockState());var marked=new ItemStack(Items.DIAMOND,37);marked.set(DataComponents.CUSTOM_NAME,Component.literal(markerName));((Container)level.getBlockEntity(chest)).setItem(0,marked);targets.add(chest);
            for(int slot=1;slot<=8;slot++){var distinct=new ItemStack(Items.PAPER);distinct.set(DataComponents.CUSTOM_NAME,Component.literal(markerName+"-distinct-"+slot));((Container)level.getBlockEntity(chest)).setItem(slot,distinct);}
            jammer=new Marker(EntityTypes.MARKER,level);jammer.setPos(origin);jammer.entityTags().add("stopper");jammer.entityTags().add("stopper_magic");require(level.addFreshEntity(jammer),"actual local TECH/MAG jammer scene");game.items().magic().flushCleansing();
            owner.setItemInHand(InteractionHand.MAIN_HAND,axe);trigger=at(1,0);block(trigger,Blocks.OAK_PLANKS.defaultBlockState());
            captureLoadedUndo();
            initialMetrics=service.metrics();
            require(owner.gameMode.destroyBlock(trigger)&&service.count()==1,"real successful break admits one native mass harvest under jammers");
            var mob=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);mob.setPos(origin.add(0,0,2));mob.setNoAi(true);mob.setNoGravity(true);require(level.addFreshEntity(mob),"real native melee witness");float hp=mob.getHealth();owner.attack(mob);
            require(mob.getHealth()<hp&&service.count()==1,"real melee coalesces with same-owner job instead of starting another world harvest");mob.discard();
            for(var e:level.getAllEntities())if(e instanceof ItemEntity)observed.add(e.getUUID());
            captured=service.views();game.items().timeStop().begin(level,10000);stopped=true;next(0);
        }
        int markerTotal(){
            var all=new ArrayList<ItemStack>();for(int slot=0;slot<owner.getInventory().getContainerSize();slot++)all.add(owner.getInventory().getItem(slot));
            for(var e:level.getAllEntities())if(e instanceof ItemEntity item)all.add(item.getItem());
            return all.stream().filter(stack->stack.is(Items.DIAMOND)&&stack.get(DataComponents.CUSTOM_NAME)!=null&&stack.get(DataComponents.CUSTOM_NAME).getString().equals(markerName)).mapToInt(ItemStack::getCount).sum();
        }
        void observeDelivery(){for(var e:level.getAllEntities())if(e instanceof ItemEntity item&&!before.contains(e.getUUID())&&observed.add(e.getUUID()))require(item.getOwner()==null&&item.position().distanceToSqr(owner.position())<9,"new native harvested output appears at user's position without inventing a player throw");}
        void outputPublished(Entity body,ServerLevel world){
            if(!rewind||restored||captureArmed||closed||stage!=1||world!=level||!(body instanceof ItemEntity item))return;
            var name=item.getItem().get(DataComponents.CUSTOM_NAME);
            if(name==null||!name.getString().equals(markerName)||service.views().stream().noneMatch(v->v.outputs()>4&&v.output()<v.outputs()))return;
            // Fixture-only pause during real publication. Never checkpoint a reentrant quantum:
            // its remaining four-stack allowance completes, then the next fixture tick captures.
            captureArmed=true;stopped=true;game.items().timeStop().begin(level,10000);
        }
        boolean idle(){return service.count()==0;}
        void afterHarvest(){
            require(targets.stream().allMatch(p->level.getBlockState(p).isAir())&&markerTotal()==37,"every accepted target removed and exact named container payout delivered once");
            require(service.metrics().mergedStacks()>initialMetrics.mergedStacks()&&service.metrics().publishedEntities()>initialMetrics.publishedEntities(),
                    "actual many-identical-block work publishes fewer physical entities than unmerged legal loot stacks");
            var payload=new ArrayList<ItemStack>();for(int slot=0;slot<owner.getInventory().getContainerSize();slot++)payload.add(owner.getInventory().getItem(slot));
            for(var entity:level.getAllEntities())if(entity instanceof ItemEntity item)payload.add(item.getItem());
            for(int distinct=1;distinct<=8;distinct++){
                String wanted=markerName+"-distinct-"+distinct;
                require(payload.stream().filter(stack->stack.get(DataComponents.CUSTOM_NAME)!=null&&stack.get(DataComponents.CUSTOM_NAME).getString().equals(wanted)).mapToInt(ItemStack::getCount).sum()==1,
                        "each distinct component-bearing pending output survives packing and replay exactly once");
            }
        }
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-began<8000,"bounded native worker/cache fixture stage "+stage);
            int elapsed=game.server().getTickCount()-when;
            if(stage==41&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            observeDelivery();
            switch(stage){
                case 0->{require(service.views().equals(captured),"TimeStop holds accepted roster/worker/publication cursors");if(elapsed<3||!pure.isDone())return false;require(pure.join()==1,"worker sees immutable copied palette despite later live-snapshot mutation");game.items().timeStop().end(level);stopped=false;next(1);}
                case 1->{
                    if(rewind&&!restored&&captureArmed){
                        require(stopped&&service.views().stream().anyMatch(v->v.output()>0&&v.output()<v.outputs()),"actual four-stack publication prefix leaves a real partial output cursor");
                        captured=service.views();capturedTerrain=new LinkedHashMap<>();for(var p:targets)capturedTerrain.put(p,level.getBlockState(p));
                        require(game.vanity().requestCapture(owner,false),"whole coordinator accepts real nonempty palette/candidate/output state");next(40);return false;
                    }
                    if(!idle())return false;require(!rewind||restored,"rewind mode reached a real partial capture");afterHarvest();baseline=service.metrics();
                    block(trigger,Blocks.OAK_PLANKS.defaultBlockState());captureLoadedUndo();owner.setItemInHand(InteractionHand.MAIN_HAND,axe);require(owner.gameMode.destroyBlock(trigger),"second actual break for clean-cache proof");
                    for(var e:level.getAllEntities())if(e instanceof ItemEntity)observed.add(e.getUUID());next(10);
                }
                case 10->{if(!idle())return false;require(service.metrics().cacheHits()>baseline.cacheHits()&&service.metrics().workerScans()-baseline.workerScans()<=1,"clean sections avoid another palette scan");
                    dirty=targets.getLast();remember(dirty);var chunk=level.getChunkSource().getChunkNow(dirty.getX()>>4,dirty.getZ()>>4);require(chunk.setBlockState(dirty,Blocks.OAK_PLANKS.defaultBlockState(),818)!=null,"direct native chunk write dirty-cache witness");
                    block(trigger,Blocks.OAK_PLANKS.defaultBlockState());captureLoadedUndo();require(owner.gameMode.destroyBlock(trigger),"third actual break after direct chunk edit");for(var e:level.getAllEntities())if(e instanceof ItemEntity)observed.add(e.getUUID());next(11);}
                case 11->{if(!idle())return false;require(level.getBlockState(dirty).isAir()&&markerTotal()==37,"direct LevelChunk dirtiness finds new axe-effective block without duplicating old payout");return true;}
                case 40->{if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;require(service.views().equals(captured),"capture consumes no accepted output");game.items().timeStop().end(level);stopped=false;next(42);}
                case 42->{if(!idle())return false;afterHarvest();require(game.vanity().requestRestoreForCheck(),"native coordinator restore of pending worker/candidate/output graph");next(41);}
                case 41->{if(game.vanity().busy())return false;require(service.views().equals(captured),"same accepted native cursors/tool/pending work restored without future worker publication");for(var e:capturedTerrain.entrySet())require(level.getBlockState(e.getKey())==e.getValue(),"native accepted terrain restored");
                    observed.clear();for(var e:level.getAllEntities())if(e instanceof ItemEntity)observed.add(e.getUUID());game.items().timeStop().end(level);stopped=false;restored=true;next(1);}
                default->throw new IllegalStateException("Axe check stage "+stage);
            }
            return false;
        }
        public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->{if(rewind)game.vanity().stop();service.stop();if(stopped){game.items().timeStop().end(level);stopped=false;}game.items().stonekivi().stop();},
                    ()->{var fresh=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!(e instanceof ServerPlayer)&&!before.contains(e.getUUID()))fresh.add(e);fresh.forEach(Entity::discard);},
                    ()->{for(var e:terrain.entrySet())game.maps().restoreSnapshot(level,e.getKey(),e.getValue(),818);service.invalidate(level);},
                    ()->{game.server().tickRateManager().setFrozen(true);core.restoreFrozen();world.restoreFrozen();random.restoreFrozen();},
                    ()->{saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();ownerRandom.restore(owner.getRandom());otherRandom.restore(other.getRandom());game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();},
                    client::close,audience::close,()->game.server().tickRateManager().setFrozen(frozen)))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("IMBA Axe scoped native restoration",failure);
        }
    }
}
