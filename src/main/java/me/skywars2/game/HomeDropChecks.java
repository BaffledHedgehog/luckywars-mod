package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual-match fixtures use real item dispatch, native hand use, and the normal server clock. */
public final class HomeDropChecks {
    private HomeDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Marker> stopMarkers=List.of();
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Home drop: "+message);}
    private static ItemEntity token(ServerLevel level,Vec3 at,ItemStack stack,ServerPlayer author){
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);item.setThrower(author);
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static Set<Long> forced(ServerLevel level){var result=new HashSet<Long>();for(long key:level.getForceLoadedChunks())result.add(key);return result;}
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> originalForced;final List<ItemEntity> tokens=new ArrayList<>();
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final boolean offhand;final Vec3 origin;final BlockPos center;
        ItemEntity stack,paused,unloaded;Marker first,second,suppressor;ItemStack firstMirror,secondMirror;
        int phase,lastTick,wait;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            originalForced=forced(level);offhand=game.config().offhandWands;
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);center=BlockPos.containing(origin);
            level.getAllEntities().forEach(existing::add);
        }
        void initialize(){
            game.items().specialDrops().stop();game.items().homes().stop();level.getChunkAt(center);
            for(var p:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(3,3,1))){var pos=p.immutable();blocks.put(pos,game.maps().snapshot(pos));level.setBlock(pos,Blocks.AIR.defaultBlockState(),818);}
            for(var p:List.of(alpha,beta)){p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);game.items().mana().noMagic(p,0);p.entityTags().remove("nomagic_active");}
            away();presentation();lastTick=game.server().getTickCount();
        }
        void away(){alpha.teleportTo(level,origin.x+30,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+32,origin.y,origin.z,Set.of(),0,0,true);}
        ItemEntity drop(Vec3 at,int count){var t=token(level,at,new ItemStack(LbcHomeItems.DROP,count),alpha);tokens.add(t);return t;}
        void presentation(){
            for(var entry:List.of(Map.entry(LbcHomeItems.DROP,"lbc:ported/home_drop"),Map.entry(LbcHomeItems.MIRROR,"lbc:ported/magic_mirror"))){
                var expected=new ItemStack(entry.getKey());var loot=game.lbc().rollTable(level,origin,alpha,entry.getValue());
                require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(expected,loot.getFirst()),"complete public loot/prototype equality "+entry.getValue());
            }
            var d=new ItemStack(LbcHomeItems.DROP);var m=new ItemStack(LbcHomeItems.MIRROR);
            require(LbcHomeItems.matchesDrop(d) && !LbcHomeItems.matchesDrop(new ItemStack(Items.BARRIER)) && LbcHomeItems.isMirror(m) && !LbcHomeItems.isMirror(new ItemStack(Items.CARROT_ON_A_STICK)),"typed leaves reject vanilla lookalikes");
            require(Identifier.parse("lbc:drop_not_trap").equals(d.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_b").equals(d.get(DataComponents.TOOLTIP_STYLE)),"drop source model/tier");
            require(Identifier.parse("lbc:sethomer").equals(m.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_d").equals(m.get(DataComponents.TOOLTIP_STYLE)),"mirror source model/tier");
            require(d.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents dn && dn.getKey().equals("drop_sethome") && !d.getHoverName().getStyle().isItalic() && d.getHoverName().getStyle().getColor().getValue()==0xffff55,"source drop name/color");
            require(m.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents mn && mn.getKey().equals("magic_mirror") && !m.getHoverName().getStyle().isItalic() && m.getHoverName().getStyle().getColor().getValue()==0x55ffff,"source mirror name/color");
            for(var pair:List.of(Map.entry(d,List.of("trap_desc")),Map.entry(m,List.of("magic_mirror.lore.1","magic_mirror.lore.2")))){
                var lore=pair.getKey().getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();require(lore.size()==pair.getValue().size(),"source lore row count");
                for(int i=0;i<lore.size();i++)require(lore.get(i).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals(pair.getValue().get(i)) && !lore.get(i).getStyle().isItalic() && lore.get(i).getStyle().getColor().getValue()==0xffffff,"source white nonitalic translated lore");
            }
            var bound=LbcHomeItems.mirror(UUID.randomUUID());var data=bound.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.getIntOr("magicwand",0)==1 && data.getIntOr("supersethomer",0)==1 && data.getIntOr("tpid",-1)==0 && !data.getStringOr("skywars2_home","").isEmpty(),"bound mirror preserves source flags");
            game.command("give TestAlpha skywars2:home_drop 1");game.command("give TestAlpha skywars2:magic_mirror 1");
        }
        ItemStack mirror(Marker marker){
            var found=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(e instanceof ItemEntity i && LbcHomeItems.isMirror(i.getItem())
                    && i.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("skywars2_home","").equals(marker.getUUID().toString()))found.add(i);
            require(found.size()==1,"one independently bound mirror per native marker");var item=found.getFirst();
            require(item.position().distanceTo(marker==first?origin:origin.add(2,0,0))<1e-8,"mirror begins at actual fractional token position");
            tokens.add(item);var result=item.getItem().copy();item.discard();return result;
        }
        void use(ServerPlayer p,InteractionHand hand,ItemStack mirror,boolean teleport){
            away();p.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(hand,mirror.copy());game.items().mana().set(p,0);var before=p.position();var held=p.getItemInHand(hand).copy();
            p.getItemInHand(hand).use(level,p,hand);
            var id=UUID.fromString(mirror.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("skywars2_home",""));
            var destination=game.items().homes().marker(id);
            if(teleport)require(destination!=null && p.position().distanceTo(destination.position().add(0,1,0))<1e-8,"exact same-dimension marker+1Y native hand teleport");
            else require(p.position().equals(before),"blocked or missing marker cannot teleport");
            require(game.items().mana().get(p)==0 && ItemStack.isSameItemSameComponents(held,p.getItemInHand(hand)) && held.getCount()==p.getItemInHand(hand).getCount(),"use preserves zero mana, count and every item component/durability");
        }
        void guards(){
            var fake=token(level,origin,new ItemStack(Items.PAPER),alpha);tokens.add(fake);require(!game.items().specialDrops().spawn(fake),"visual fake rejected");fake.discard();
            var stale=drop(origin,2);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().homes().ready(stale),"stale epoch rejected");stale.discard();
            var visual=drop(origin,2);visual.makeFakeItem();require(!game.items().specialDrops().spawn(visual),"native fake token rejected");visual.discard();
            var remote=drop(origin,2);int x=(center.getX()>>4)+128,z=(center.getZ()>>4)+128;while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;
            remote.setPos(x*16+8,origin.y,z*16+8);require(!game.items().homes().ready(remote) && level.getChunkSource().getChunkNow(x,z)==null && remote.getItem().getCount()==2,"unresident admission does not load/debit");remote.discard();
        }
        boolean step(){
            require(game.playing(),"actual match retained");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(0),new ItemStack(LbcHomeItems.DROP)) && ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(1),new ItemStack(LbcHomeItems.MIRROR)),"native give prototypes");alpha.getInventory().clearContent();guards();stack=drop(origin,2);phase++;}
                case 1->{
                    require(stack.getItem().getCount()==1 && game.items().homes().count()==1,"one dispatcher debit/marker per tick");require(!game.items().specialDrops().spawn(stack) && stack.getItem().getCount()==1,"same tick dispatch cannot debit twice");
                    first=game.items().homes().snapshot().getFirst();require(first.position().distanceTo(new Vec3(center.getX()+.5,center.getY()+.5,center.getZ()+.5))<1e-8 && first.entityTags().containsAll(Set.of("hometp.id","rc_1sec","magic")) && game.items().currentEpoch(first),"source marker floor center, tags and epoch");
                    firstMirror=mirror(first);stack.setPos(origin.add(2,0,0));phase++;
                }
                case 2->{
                    require(stack.isRemoved() && game.items().homes().count()==2,"second tick final unit removed");second=game.items().homes().snapshot().stream().filter(m->m!=first).findFirst().orElseThrow();secondMirror=mirror(second);
                    require(!first.getUUID().equals(second.getUUID()) && game.items().homes().ticketCount()==1 && game.items().homes().ticketReferences(level,new ChunkPos(center.getX() >> 4,center.getZ() >> 4))==2,"close marks bind distinctly and share refcounted own ticket");
                    game.config().offhandWands=true;use(beta,InteractionHand.MAIN_HAND,firstMirror,true);use(beta,InteractionHand.OFF_HAND,secondMirror,true);use(beta,InteractionHand.MAIN_HAND,firstMirror,true);
                    game.config().offhandWands=false;use(beta,InteractionHand.OFF_HAND,secondMirror,false);game.config().offhandWands=true;
                    var guard=new Marker(EntityTypes.MARKER,level);guard.setPos(origin.add(32,0,0));guard.entityTags().add("stopper_magic");require(level.addFreshEntity(guard) && game.items().magic().suppressed(beta),"real use-location antimagic admission");try{use(beta,InteractionHand.MAIN_HAND,firstMirror,false);}finally{guard.discard();}
                    beta.setGameMode(GameType.SPECTATOR);try{use(beta,InteractionHand.MAIN_HAND,firstMirror,false);}finally{beta.setGameMode(GameType.SURVIVAL);}
                    use(beta,InteractionHand.MAIN_HAND,LbcHomeItems.mirror(UUID.randomUUID()),false);
                    // Start a fresh clock so the exact first renewal cadence is observable.
                    first.discard();game.items().homes().unloaded(first);require(game.items().homes().count()==1 && game.items().homes().ticketCount()==1 && game.items().homes().ticketReferences(level,new ChunkPos(center.getX() >> 4,center.getZ() >> 4))==1,"deleting one shared mark preserves remaining ticket/reference");use(beta,InteractionHand.MAIN_HAND,firstMirror,false);use(beta,InteractionHand.MAIN_HAND,secondMirror,true);game.items().homes().stop();tokens.forEach(Entity::discard);stack=drop(origin,1);require(game.items().specialDrops().spawn(stack),"fresh scheduled home");first=game.items().homes().snapshot().getFirst();firstMirror=mirror(first);wait=0;phase++;
                }
                case 3->{
                    wait++;if(wait<20)require(level.getBlockState(center.below()).isAir(),"no early support renewal");
                    if(wait==20){require(level.getBlockState(center.below()).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE)) && level.getBlockState(center.above()).isAir() && level.getBlockState(center.above(2)).isAir(),"native first renewal exactly20 ticks");
                        level.setBlock(center.below(),Blocks.STONE.defaultBlockState(),818);level.setBlock(center.above(),Blocks.CHEST.defaultBlockState(),818);((net.minecraft.world.Container)level.getBlockEntity(center.above())).setItem(0,new ItemStack(Items.DIAMOND,7));level.setBlock(center.above(2),Blocks.STONE.defaultBlockState(),818);
                        game.items().timeStop().begin(level,200);paused=drop(origin.add(2,0,0),2);wait=0;phase++;}
                }
                case 4->{
                    require(level.getBlockState(center.below()).is(Blocks.STONE) && level.getBlockState(center.above()).is(Blocks.CHEST) && paused.getItem().getCount()==2,"time stop pauses renewal and pending dispatch");use(beta,InteractionHand.MAIN_HAND,firstMirror,false);
                    if(++wait==3){paused.discard();game.items().timeStop().end(level);wait=0;phase++;}
                }
                case 5->{
                    if(++wait<20)require(level.getBlockState(center.above()).is(Blocks.CHEST),"paused ticks do not advance renewal");
                    if(wait==20){require(level.getBlockState(center.below()).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE)) && level.getBlockState(center.above()).isAir() && level.getBlockState(center.above(2)).isAir(),"recurring support and two-cell clearance after20 active ticks");
                        for(var e:level.getAllEntities())require(!(e instanceof ItemEntity i && i.getItem().is(Items.DIAMOND) && i.position().distanceTo(origin)<4),"strict clearance suppresses replaced container ejection");unloaded=drop(origin.add(2,0,0),1);game.items().specialDrops().unloaded(unloaded);phase++;}
                }
                case 6->{require(!unloaded.isRemoved() && game.items().homes().count()==1,"unloaded pending token cannot activate");unloaded.discard();
                    suppressor=new Marker(EntityTypes.MARKER,level);suppressor.setPos(first.position());suppressor.entityTags().add("stopper_magic");require(level.addFreshEntity(suppressor),"real antimagic marker admission");phase++;}
                case 7->{require(first.isRemoved() && game.items().homes().count()==0 && game.items().homes().ticketCount()==0,"antimagic destroys home and releases last own ticket");suppressor.discard();use(beta,InteractionHand.MAIN_HAND,firstMirror,false);phase++;}
                case 8->{require(game.items().homes().count()==0 && forced(level).equals(originalForced),"dangling mirror remains inert and global forced chunks unchanged");
                    var a=drop(origin,1);game.items().homes().spawn(a);a.discard();first=game.items().homes().snapshot().getFirst();firstMirror=mirror(first);
                    var b=drop(origin.add(2,0,0),1);game.items().homes().spawn(b);b.discard();second=game.items().homes().snapshot().stream().filter(m->m!=first).findFirst().orElseThrow();secondMirror=mirror(second);
                    var artifact=new ItemStack(LbcMagic.MYSTICAL_ARTIFACT);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("mystical",1);flags.putInt("type",1);artifact.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
                    beta.getInventory().clearContent();beta.getInventory().setItem(0,artifact);beta.teleportTo(level,first.getX()+26,first.getY(),first.getZ(),Set.of(),0,0,true);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();
                    require(game.items().magic().artifactActive(beta),"real native artifact carrier active");phase++;}
                case 9->{require(!first.isRemoved() && second.isRemoved() && game.items().homes().count()==1 && game.items().homes().ticketReferences(level,new ChunkPos(center.getX() >> 4,center.getZ() >> 4))==1,"artifact25 deletes mark24 blocks away while preserving mark26 and shared ticket");
                    beta.teleportTo(level,first.getX()+24,first.getY(),first.getZ(),Set.of(),0,0,true);phase++;}
                case 10->{require(first.isRemoved() && game.items().homes().count()==0 && game.items().homes().ticketCount()==0,"artifact25 removes final marker and releases own ticket");
                    beta.getInventory().clearContent();game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();use(beta,InteractionHand.MAIN_HAND,firstMirror,false);return true;}
                default->throw new IllegalStateException("Unknown home phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(level);if(suppressor!=null)suppressor.discard();tokens.forEach(Entity::discard);game.items().homes().stop();game.items().specialDrops().stop();
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());game.config().offhandWands=offhand;alphaState.restore(game);betaState.restore(game);game.items().magic().inventoryChanged(alpha);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();
        }
    }
    private static void closeFailure(State state,Throwable failure){try{state.close();}catch(Throwable cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}finally{running=null;}}
    public static void run(GameController game){require(running==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error failure){closeFailure(running,failure);throw failure;}}
    public static void tick(GameController game){if(running==null)return;var state=running;try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS home-drop");}}catch(Throwable failure){closeFailure(state,failure);SkyWars2.LOGGER.error("[check] FAIL home-drop",failure);}}
    public static void stopProbe(GameController game){
        require(game.playing(),"actual match stop probe");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        game.items().timeStop().end(level);stopEpoch=game.items().epoch();stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),new ItemStack(LbcHomeItems.DROP,3),alpha);
        require(game.items().homes().ready(stopToken),"stop payload ready");game.items().homes().spawn(stopToken);stopMarkers=game.items().homes().snapshot();
        require(stopMarkers.size()==1 && game.items().homes().ticketCount()==1,"owned home ticket before actual stop");
        var forcedBefore=forced(level);game.items().homes().stop();
        require(game.items().homes().count()==0 && game.items().homes().ticketCount()==0 && forced(level).equals(forcedBefore),"home service stop releases only its own tickets");
        game.items().homes().spawn(stopToken);stopMarkers=game.items().homes().snapshot();
        require(stopMarkers.size()==1 && game.items().homes().ticketCount()==1,"rearmed native payload for actual match stop");game.items().timeStop().begin(level,200);SkyWars2.LOGGER.info("[check] PASS home-drop-stop-probe");
    }
    public static void cleanup(GameController game){
        require(!game.items().currentEpoch(stopEpoch) && game.items().homes().count()==0 && game.items().homes().ticketCount()==0 && stopMarkers.stream().allMatch(Entity::isRemoved),"actual match stop clears markers/epoch/own tickets");
        // MapManager separately resets arena forceloads at match end; test home-only ownership above.
        if(stopToken!=null)stopToken.discard();stopToken=null;stopMarkers=List.of();stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS home-drop-cleanup");
    }
}
