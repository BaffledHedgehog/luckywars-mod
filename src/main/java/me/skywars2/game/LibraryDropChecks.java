package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real dropped tokens, native blocks/containers and native reward serialization in an actual match. */
public final class LibraryDropChecks {
    private LibraryDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Entity> stopRewards=List.of();
    private static int stopXp;
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Library drop: "+text);}
    private static ItemEntity token(ServerLevel level,Vec3 at,int count,ServerPlayer author){
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(LbcLibraryDropItem.Kind.NORMAL.item(),count));item.setThrower(author);
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static net.minecraft.nbt.CompoundTag saved(Entity entity,ServerLevel level){
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());entity.saveWithoutId(output);return output.buildResult();
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final Set<Long> forced=new HashSet<>();
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());final List<ItemEntity> tokens=new ArrayList<>();
        final Map<Entity,Vec3> frozen=new IdentityHashMap<>();
        final Vec3 origin;final BlockPos center;final int maxBlocks;ItemEntity stack,paused,unloaded;int phase,lastTick,wait;
        State(GameController game){this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);center=BlockPos.containing(origin);
            maxBlocks=level.getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);level.getAllEntities().forEach(existing::add);
        }
        void initialize(){initialize(false);}
        void initialize(boolean preserveOthers){
            for(var p:BlockPos.betweenClosed(center.offset(-3,-2,-4),center.offset(5,16,4))){var pos=p.immutable();long key=net.minecraft.world.level.ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}level.getChunkAt(pos);blocks.put(pos,game.maps().snapshot(pos));}
            if(!preserveOthers)game.items().specialDrops().stop();game.items().libraryDrop().stop();
            for(var p:List.of(alpha,beta)){p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);}
            place();reset();presentation();lastTick=game.server().getTickCount();
        }
        void place(){alpha.teleportTo(level,origin.x+20,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);}
        void reset(){game.items().libraryDrop().stop();for(var p:blocks.keySet()){if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,Blocks.DIAMOND_BLOCK.defaultBlockState(),818);}}
        ItemEntity drop(int count){var t=token(level,origin.add(-.21,0,.13),count,alpha);tokens.add(t);return t;}
        void presentation(){var s=new ItemStack(LbcLibraryDropItem.Kind.NORMAL.item());var data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr("library",0)==1 && data.getIntOr("drop",0)==1 && LbcLibraryDropItem.matches(s),"exact source flags");
            require(Identifier.parse("lbc:drop_not_trap").equals(s.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_d").equals(s.get(DataComponents.TOOLTIP_STYLE)),"source model/tier");
            require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals("drop_library") && !s.getHoverName().getStyle().isItalic() && s.getHoverName().getStyle().getColor().getValue()==0xffff55,"source translated yellow name");
            var lore=s.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"source lore");
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/library_drop");require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(s,loot.getFirst()),"public leaf/native prototype equality");
            for(var item:List.of(Items.BARRIER,Items.REPEATING_COMMAND_BLOCK,Items.PAPER))require(!LbcLibraryDropItem.matches(new ItemStack(item)),"vanilla lookalike rejected");
            var old=new ItemStack(LbcLibraryDropItem.Kind.OLD.item());require(LbcLibraryDropItem.matches(old) && Identifier.parse("minecraft:barrier").equals(old.get(DataComponents.ITEM_MODEL)) && !old.has(DataComponents.CUSTOM_NAME) && !old.has(DataComponents.TOOLTIP_STYLE) && old.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines().isEmpty(),"legacy plain barrier");
            var oldLoot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/library_drop_old");require(oldLoot.size()==1 && ItemStack.isSameItemSameComponents(old,oldLoot.getFirst()),"complete legacy public leaf");
        }
        // Independent final geometry predicates: doors cut bookshelves; corner posts overwrite walls.
        BlockState expected(int x,int y,int z){
            if(x>=-1 && x<=3 && z>=-2 && z<=2){
                if(y==-1)return Blocks.OAK_PLANKS.defaultBlockState();if(y==3)return Blocks.OAK_WOOD.defaultBlockState();
                if(y>=0 && y<=2){
                    if((x==-1 || x==3) && (z==-2 || z==2))return Blocks.OAK_WOOD.defaultBlockState();
                    if(y<=1 && (((x==-1 || x==3) && z==0) || (x==1 && (z==-2 || z==2))))return Blocks.AIR.defaultBlockState();
                    if((x>=0 && x<=2 && (z==-2 || z==2)) || ((x==-1 || x==3) && z>=-1 && z<=1))return Blocks.BOOKSHELF.defaultBlockState();
                    if(x==1 && z==0 && y==0)return Blocks.ENCHANTING_TABLE.defaultBlockState();
                    if(x==1 && z==0 && y==2)return Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING,Direction.DOWN);
                }
            }return Blocks.DIAMOND_BLOCK.defaultBlockState();
        }
        void geometry(){for(var p:blocks.keySet()){var d=p.subtract(center);require(level.getBlockState(p).equals(expected(d.getX(),d.getY(),d.getZ())),"exact native geometry/interior/outside sentinel "+d);}}
        List<Entity> rewards(){return game.items().libraryDrop().snapshot();}
        void reward(){var actors=rewards();require(actors.size()==3,"exactly three reward entities");int lapis=0,orb=0,bottle=0;for(var e:actors){
                require(e.position().distanceTo(origin.add(1,1,0))<1e-8 && game.items().currentEpoch(e),"fractional selected-target reward origin and epoch");
                var tag=saved(e,level);
                if(e instanceof ItemEntity item){lapis++;require(item.getItem().is(Items.LAPIS_LAZULI) && item.getItem().getCount()==32 && ItemStack.isSameItemSameComponents(item.getItem(),new ItemStack(Items.LAPIS_LAZULI)),"native lapis32");}
                else if(e instanceof ExperienceOrb){orb++;require(tag.getIntOr("Value",-1)==1395,"ONE native orb retains Value1395 without splitting or direct grant");}
                else {bottle++;require(e.getType()==net.minecraft.world.entity.EntityTypes.EXPERIENCE_BOTTLE && e.getDeltaMovement().equals(Vec3.ZERO) && !e.isNoGravity(),"ordinary native bottle, default motion/gravity");}
            }require(lapis==1 && orb==1 && bottle==1,"one lapis stack, orb and bottle");}
        void activate(){var t=drop(2);boolean dispatched=game.items().specialDrops().spawn(t);
            if(!dispatched || t.getItem().getCount()!=1){var target=game.items().cageDrops().target(t);var missing=new ArrayList<String>();if(target!=null){var c=target.blockPosition();for(int x=(c.getX()-2)>>4;x<=(c.getX()+4)>>4;x++)for(int z=(c.getZ()-3)>>4;z<=(c.getZ()+3)>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)missing.add(x+","+z);}
                throw new IllegalStateException("Library drop: shared dispatcher debits one; dispatched="+dispatched+" count="+t.getItem().getCount()+" ready="+game.items().libraryDrop().ready(t)+" removed="+t.isRemoved()+" age="+t.getAge()+" stopped="+game.items().timeStop().stopped(level)+" stage="+game.stage()+" pending="+game.items().specialDrops().pendingCount()+" token="+t.position()+" target="+(target==null?"none":target.getScoreboardName()+"@"+target.position())+" missingTargetChunks="+missing+" tokenResident="+(level.getChunkSource().getChunkNow(t.blockPosition().getX()>>4,t.blockPosition().getZ()>>4)!=null)+" tags="+t.entityTags());}
            require(dispatched && t.getItem().getCount()==1,"shared dispatcher debits one");require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"same tick duplicate blocked");t.discard();}
        void targets(){
            reset();activate();reward(); // nearest other participant
            alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+20,origin.y,origin.z,Set.of(),0,0,true);reset();activate();reward(); // self is eligible
            place();beta.setGameMode(GameType.CREATIVE);try{reset();activate();reward();}finally{beta.setGameMode(GameType.SURVIVAL);beta.setNoGravity(true);}
            var board=game.server().getScoreboard();var oldAlpha=alpha.getTeam();var oldBeta=beta.getTeam();require(board.getPlayerTeam("codex_library_probe")==null,"fixture team available");var team=board.addPlayerTeam("codex_library_probe");
            try{board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);reset();activate();reward();}finally{board.removePlayerTeam(team);if(oldAlpha!=null)board.addPlayerToTeam(alpha.getScoreboardName(),oldAlpha);if(oldBeta!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBeta);}
            alpha.teleportTo(level,origin.x-.1,origin.y,origin.z,Set.of(),0,0,true);
            for(String tag:List.of("spectator","rcdenyalways","sauvojen_tuntija","strict_map_object","magic_academy","transformator")){boolean added=alpha.entityTags().add(tag);try{reset();activate();reward();}finally{if(added)alpha.entityTags().remove(tag);}}
            alpha.setGameMode(GameType.SPECTATOR);try{reset();activate();reward();}finally{alpha.setGameMode(GameType.SURVIVAL);alpha.setNoGravity(true);}
            game.spectators().enter(alpha);try{alpha.setGameMode(GameType.SURVIVAL);reset();activate();reward();}finally{game.spectators().remove(alpha);alpha.setGameMode(GameType.SURVIVAL);alpha.setNoGravity(true);}
            var modes=new LinkedHashMap<ServerPlayer,GameType>();for(var p:level.players())modes.put(p,p.gameMode.getGameModeForPlayer());try{for(var p:modes.keySet())p.setGameMode(GameType.SPECTATOR);reset();activate();require(rewards().isEmpty() && level.getBlockState(center).is(Blocks.DIAMOND_BLOCK),"no eligible target still consumes one unit without structure or rewards");}finally{for(var e:modes.entrySet()){e.getKey().setGameMode(e.getValue());e.getKey().noPhysics=false;}}place();
            reset();var legacy=token(level,origin,2,alpha);tokens.add(legacy);legacy.setItem(new ItemStack(LbcLibraryDropItem.Kind.OLD.item(),2));require(game.items().specialDrops().spawn(legacy) && legacy.getItem().getCount()==1,"legacy actual dispatcher");geometry();reward();legacy.discard();
        }
        void nativeCases(){
            reset();var chest=center.offset(0,0,2);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),818);var be=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chest);require(be!=null,"native overwritten chest");be.setItem(0,new ItemStack(Items.NETHERITE_INGOT,17));
            activate();geometry();reward();for(var e:level.getAllEntities())if(!existing.contains(e) && e instanceof ItemEntity i)require(!i.getItem().is(Items.NETHERITE_INGOT),"strict replacement ejects no container contents");
            var actors=List.copyOf(rewards());var before=beta.totalExperience;game.items().libraryDrop().stop();require(actors.stream().allMatch(Entity::isRemoved) && beta.totalExperience==before,"quiet cleanup discards native rewards without granting XP");
            for(int limit:List.of(0,1,2,3,8,9,24,25)){reset();level.getGameRules().set(GameRules.MAX_BLOCK_MODIFICATIONS,limit,game.server());activate();
                require(level.getBlockState(center.offset(1,0,0)).is(Blocks.ENCHANTING_TABLE) && level.getBlockState(center.offset(1,2,0)).getValue(EndRodBlock.FACING)==Direction.DOWN,"setblock unaffected by fill gamerule "+limit);
                require(level.getBlockState(center.below()).is(limit>=25?Blocks.OAK_PLANKS:Blocks.DIAMOND_BLOCK),"independent floor25 gate "+limit);
                require(level.getBlockState(center.offset(0,2,2)).is(limit>=9?Blocks.BOOKSHELF:Blocks.DIAMOND_BLOCK),"wall9 gate "+limit);
                require(level.getBlockState(center.offset(3,2,2)).is(limit>=3?Blocks.OAK_WOOD:Blocks.DIAMOND_BLOCK),"later post3 after rejected larger fills "+limit);
                require(level.getBlockState(center.offset(1,0,2)).is(limit>=2?Blocks.AIR:Blocks.DIAMOND_BLOCK),"later doorway2 gate "+limit);reward();}
            level.getGameRules().set(GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());reset();var both=drop(2);var flags=both.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();flags.putInt("lightning",1); // source shaft runs before Library
            flags.putInt("levitation",1);both.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(flags));require(game.items().specialDrops().spawn(both) && both.getItem().getCount()==1,"multi-flag single debit");require(level.getBlockState(center.above(3)).is(Blocks.OAK_WOOD),"Library roof overwrites earlier native falling-block shaft in source order");reward();game.items().fallingNetheriteDrops().stop();require(beta.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION),"independent Enzan branch runs after Library");beta.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);both.discard();beta.getInventory().clearContent();
        }
        void guards(){var stale=drop(2);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().specialDrops().spawn(stale) && stale.getItem().getCount()==2,"stale epoch no debit");stale.discard();var fake=drop(2);fake.makeFakeItem();require(!game.items().specialDrops().spawn(fake),"native give visual no activation");fake.discard();
            var remote=drop(2);int x=(center.getX()>>4)+128,z=(center.getZ()>>4)+128;while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;remote.setPos(x*16+8,origin.y,z*16+8);require(!game.items().specialDrops().spawn(remote) && remote.getItem().getCount()==2 && level.getChunkSource().getChunkNow(x,z)==null,"unresident token neither loads nor debits");remote.discard();
        }
        boolean step(){require(game.playing(),"actual match retained");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;switch(phase){
            case 0->{guards();targets();nativeCases();reset();for(var p:BlockPos.betweenClosed(center,center.offset(2,2,1)))level.setBlock(p,Blocks.AIR.defaultBlockState(),818);stack=drop(2);phase++;}
            case 1->{require(stack.getItem().getCount()==1 && rewards().size()==3,"actual first tick dispatches one unit");phase++;}
            case 2->{require(stack.isRemoved() && rewards().size()>=3,"actual second tick consumes final unit");for(var e:rewards())frozen.put(e,e.position());game.items().timeStop().begin(level,200);paused=drop(2);wait=0;phase++;}
            case 3->{require(paused.getItem().getCount()==2 && frozen.entrySet().stream().allMatch(e->!e.getKey().isRemoved() && e.getKey().position().distanceTo(e.getValue())<1e-8),"time stop pauses native rewards and pending token");if(++wait>=3){paused.discard();game.items().timeStop().end(level);game.items().libraryDrop().stop();unloaded=drop(2);game.items().specialDrops().unloaded(unloaded);phase++;}}
            case 4->{require(!unloaded.isRemoved() && unloaded.getItem().getCount()==2 && rewards().isEmpty(),"unloaded token cannot replay");unloaded.discard();return true;}
            default->throw new IllegalStateException("Unknown Library phase "+phase);
        }return false;}
        public void close(){close(false);}
        void close(boolean keepRewards){game.items().timeStop().end(level);tokens.forEach(Entity::discard);if(!keepRewards){game.items().specialDrops().stop();game.items().libraryDrop().stop();game.items().fallingNetheriteDrops().stop();}
            var leftovers=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e) && !(e instanceof ServerPlayer) && !(keepRewards && LbcLibraryDrop.managed(e)) && Math.abs(e.getX()-origin.x)<8 && Math.abs(e.getZ()-origin.z)<8)leftovers.add(e);leftovers.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(level.getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(e.getKey(),e.getValue());}for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            level.getGameRules().set(GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());alphaState.restore(game);betaState.restore(game);}
    }
    private static void closeFailure(State state,Throwable error){try{state.close();}catch(Throwable cleanup){if(error!=cleanup)error.addSuppressed(cleanup);}finally{running=null;}}
    public static void run(GameController game){require(running==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error error){closeFailure(running,error);throw error;}}
    public static void tick(GameController game){if(running==null)return;var state=running;try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS library-drop");}}catch(Throwable error){closeFailure(state,error);SkyWars2.LOGGER.error("[check] FAIL library-drop",error);}}
    public static void stopProbe(GameController game){
        require(game.playing(),"actual match stop probe");game.items().timeStop().end(game.maps().level());var fixture=new State(game);
        try{fixture.initialize(true);fixture.activate();fixture.reward();stopRewards=List.copyOf(fixture.rewards());fixture.close(true);}
        catch(RuntimeException|Error error){closeFailure(fixture,error);throw error;}
        var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        stopXp=alpha.totalExperience+beta.totalExperience;stopEpoch=game.items().epoch();game.items().timeStop().begin(level,200);
        stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),3,alpha);
        require(stopRewards.size()==3 && !game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"real retained native rewards and pending Library under stop");SkyWars2.LOGGER.info("[check] PASS library-drop-stop-probe");
    }
    public static void cleanup(GameController game){
        var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(!game.items().currentEpoch(stopEpoch) && stopToken!=null && stopToken.getItem().getCount()==3 && game.items().libraryDrop().trackedCount()==0,"actual stop invalidates epoch and clears reward service without debit");
        require(stopRewards.size()==3 && stopRewards.stream().allMatch(Entity::isRemoved) && alpha.totalExperience+beta.totalExperience==stopXp,"actual stop quietly discards native rewards without XP grants");
        require(!game.items().specialDrops().spawn(stopToken),"stopped match cannot replay Library");stopToken.discard();stopToken=null;stopEpoch=null;stopRewards=List.of();SkyWars2.LOGGER.info("[check] PASS library-drop-cleanup");
    }
}
