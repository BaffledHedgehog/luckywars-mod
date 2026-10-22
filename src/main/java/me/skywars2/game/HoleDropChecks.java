package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real-match terrain fixtures. No active participant is killed by a probe. */
public final class HoleDropChecks {
    private HoleDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Hole drops: "+message);}
    private static ItemEntity token(ServerLevel level,Vec3 at,ItemStack stack,ServerPlayer author){
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);if(author!=null)item.setThrower(author);
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"real native token admission");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final net.minecraft.world.scores.PlayerTeam alphaTeam,betaTeam;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ItemEntity> tokens=new ArrayList<>();final Vec3 origin;final BlockPos center;
        final boolean alphaNoPhysics,betaNoPhysics;final int maxBlocks,alphaMobKills,betaMobKills;
        final String[] flags={"yama","yama_old"};
        final String[] ids={"skywars2:hole_drop","skywars2:hole_drop_old"};
        final String[] names={"trap_hole","block.minecraft.barrier"};
        ItemEntity stack,paused,unloaded;int phase,lastTick,wait;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two active actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            var board=game.server().getScoreboard();alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());betaTeam=board.getPlayersTeam(beta.getScoreboardName());
            alphaNoPhysics=alpha.noPhysics;betaNoPhysics=beta.noPhysics;
            maxBlocks=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS);
            alphaMobKills=alpha.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));
            betaMobKills=beta.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,240.7,Math.floor(alpha.getZ()/16)*16+8.81);center=BlockPos.containing(origin);
            level.getAllEntities().forEach(existing::add);
        }
        void initialize(){
            // Include neighbours receiving ordinary fill shape updates and native fire contact.
            for(var p:BlockPos.betweenClosed(center.offset(-4,-23,-4),center.offset(4,5,4)))save(p.immutable());
            game.items().specialDrops().stop();
            var board=game.server().getScoreboard();board.removePlayerFromTeam(alpha.getScoreboardName());board.removePlayerFromTeam(beta.getScoreboardName());
            for(var player:List.of(alpha,beta)){
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.noPhysics=false;player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
            }
            alpha.teleportTo(level,origin.x+20,origin.y+8,origin.z,Set.of(),0,0,true);reset();
            for(int i=0;i<2;i++)presentation(i);
            lastTick=game.server().getTickCount();
        }
        void save(BlockPos p){
            if(blocks.containsKey(p))return;long key=net.minecraft.world.level.ChunkPos.pack(p.getX()>>4,p.getZ()>>4);
            if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(p.getX()>>4,p.getZ()>>4,true);}
            level.getChunkAt(p);blocks.put(p,game.maps().snapshot(p));
            if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
        }
        void placeBeta(){beta.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.setDeltaMovement(Vec3.ZERO);}
        void presentation(int i){
            var kind=LbcHoleDropItem.Kind.values()[i];var stack=new ItemStack(kind.item());var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(kind.flag().equals(flags[i]) && kind.id().toString().equals(ids[i]) && kind.nameKey().equals(names[i]),"independent source IDs "+kind);
            require(data.size()==2 && data.getIntOr(flags[i],0)==1 && data.getIntOr("drop",0)==1,"exact source flags "+kind);
            if(kind==LbcHoleDropItem.Kind.OLD){
                require(Identifier.parse("minecraft:barrier").equals(stack.get(DataComponents.ITEM_MODEL)) && !stack.has(DataComponents.CUSTOM_NAME)
                        && !stack.has(DataComponents.TOOLTIP_STYLE) && stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines().isEmpty(),"source old barrier stays undecorated");
            }else{
            require(Identifier.parse("lbc:drop_trap").equals(stack.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source model/tier "+kind);
            require(stack.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals(names[i])
                    && !stack.getHoverName().getStyle().isItalic() && stack.getHoverName().getStyle().getColor()!=null && stack.getHoverName().getStyle().getColor().getValue()==0xffff55,"source yellow nonitalic name "+kind);
            var lore=stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines();
            require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc")
                    && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor()!=null && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"source white lore "+kind);
            }
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/"+ids[i].substring("skywars2:".length()));
            require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(stack,loot.getFirst()),"native/ported complete presentation "+kind);
            game.command("give TestAlpha "+ids[i]+" 1");
        }
        ItemEntity drop(LbcHoleDropItem.Kind kind,int count){var t=token(level,origin,new ItemStack(kind.item(),count),alpha);tokens.add(t);return t;}
        void reset(){
            for(var p:BlockPos.betweenClosed(center.offset(-2,-20,-2),center.offset(2,1,2)))level.setBlock(p,Blocks.STONE.defaultBlockState(),818);
            placeBeta();
        }
        void batch(LbcHoleDropItem.Kind kind){
            var before=beta.position();var t=drop(kind,3);
            require(game.items().holeDrops().ready(t,kind,beta) && game.items().holeDrops().spawn(t,kind,beta),"resident native pit admitted "+kind);
            require(beta.position().equals(before),"terrain trap never teleports target");
            require(!t.isRemoved() && t.getItem().getCount()==3,"geometry service leaves debit to shared dispatcher");t.discard();
        }
        void geometry(LbcHoleDropItem.Kind kind){
            int air=0;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                for(int y=-15;y<=0;y++){require(level.getBlockState(center.offset(x,y,z)).isAir(),"exact shaft air cell "+x+","+y+","+z);air++;}
                require(level.getBlockState(center.offset(x,-16,z)).is(Blocks.COBWEB),"nine cobweb cells Y-16");
                require(level.getBlockState(center.offset(x,-17,z)).is(kind==LbcHoleDropItem.Kind.NORMAL?Blocks.FIRE:Blocks.SOUL_FIRE),"nine native fire cells Y-17");
                require(level.getBlockState(center.offset(x,-18,z)).is(kind==LbcHoleDropItem.Kind.NORMAL?Blocks.OAK_LOG:Blocks.SOUL_SOIL),"nine native support cells Y-18");
                require(level.getBlockState(center.offset(x,-19,z)).is(kind==LbcHoleDropItem.Kind.NORMAL?Blocks.COBBLESTONE:Blocks.STONE),"normal bottom/old untouched Y-19");
                require(level.getBlockState(center.offset(x,-20,z)).is(Blocks.STONE),"no invented lower carve");
            }
            require(air==144,"exact 144 air cells");
            for(int y=-19;y<=0;y++)require(level.getBlockState(center.offset(2,y,0)).is(Blocks.STONE),"no lateral overfill");
        }
        void targets(){
            var t=drop(LbcHoleDropItem.Kind.NORMAL,1);
            alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);
            require(game.items().cageDrops().target(t)==alpha,"nearest includes actual thrower");
            alpha.setGameMode(GameType.SPECTATOR);require(game.items().cageDrops().target(t)==beta,"spectator excluded");alpha.setGameMode(GameType.SURVIVAL);alpha.noPhysics=false;
            var board=game.server().getScoreboard();var team=board.addPlayerTeam("codex_hole_probe");
            try{board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);placeBeta();
                alpha.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);require(game.items().cageDrops().target(t)==beta,"nearest allied participant included");}
            finally{board.removePlayerTeam(team);}
            t.discard();alpha.teleportTo(level,origin.x+20,origin.y+8,origin.z,Set.of(),0,0,true);placeBeta();
        }
        void transactions(){
            reset();var t=drop(LbcHoleDropItem.Kind.NORMAL,2);
            var data=t.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("yama_old",1);t.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            require(game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"both hole flags use single debit");
            // Old runs after normal: its fire/soil win, while normal's deeper cobble remains.
            require(level.getBlockState(center.below(17)).is(Blocks.SOUL_FIRE) && level.getBlockState(center.below(18)).is(Blocks.SOUL_SOIL)
                    && level.getBlockState(center.below(19)).is(Blocks.COBBLESTONE),"source order normal then old");
            require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"same tick cannot repeat debit");t.discard();
            var modes=new LinkedHashMap<ServerPlayer,GameType>();for(var p:level.players())modes.put(p,p.gameMode.getGameModeForPlayer());
            try{for(var p:modes.keySet())p.setGameMode(GameType.SPECTATOR);var empty=drop(LbcHoleDropItem.Kind.NORMAL,2);
                require(game.items().cageDrops().target(empty)==null,"no nonspectator yields null target");
                require(game.items().specialDrops().spawn(empty) && empty.getItem().getCount()==1,"null target still debits once");empty.discard();
            }finally{for(var e:modes.entrySet()){e.getKey().setGameMode(e.getValue());e.getKey().noPhysics=false;}}
            placeBeta();
        }
        void containers(){
            for(var kind:LbcHoleDropItem.Kind.values()){
                reset();var pos=center.below(5);level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
                var gem=new ItemStack(Items.DIAMOND,7);gem.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("hole replacement probe"));
                ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos)).setItem(0,gem.copy());
                int before=count(gem);batch(kind);
                require(level.getBlockEntity(pos)==null && level.getBlockState(pos).isAir() && count(gem)==before,"native fill skips BE side effects/no inventory ejection "+kind);
            }
        }
        int count(ItemStack stack){int n=0;for(var e:level.getAllEntities())if(e instanceof ItemEntity item && ItemStack.isSameItemSameComponents(item.getItem(),stack))n+=item.getItem().getCount();return n;}
        void fillLimit(){
            reset();level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,100,game.server());
            try{batch(LbcHoleDropItem.Kind.NORMAL);
                require(level.getBlockState(center.below(5)).is(Blocks.STONE),"native limit rejects complete 144-cell air command");
                require(level.getBlockState(center.below(16)).is(Blocks.COBWEB) && level.getBlockState(center.below(17)).is(Blocks.FIRE)
                        && level.getBlockState(center.below(18)).is(Blocks.OAK_LOG) && level.getBlockState(center.below(19)).is(Blocks.COBBLESTONE),"independent nine-cell commands still execute after rejected fill");
            }finally{level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());}
        }
        void residency(){
            var t=drop(LbcHoleDropItem.Kind.NORMAL,2);var previous=beta.position();int x=(center.getX()>>4)+128,z=(center.getZ()>>4)+128;
            while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;
            try{beta.setPos(x*16+8.37,origin.y,z*16+8.81);
                require(!game.items().holeDrops().ready(t,LbcHoleDropItem.Kind.NORMAL,beta) && !game.items().holeDrops().spawn(t,LbcHoleDropItem.Kind.NORMAL,beta),"unresident target rejects before reads/writes");
                require(level.getChunkSource().getChunkNow(x,z)==null && t.getItem().getCount()==2,"guard cannot load or debit");
            }finally{beta.setPos(previous);t.discard();}
        }
        net.minecraft.world.entity.animal.cow.Cow cow(Vec3 at){
            var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
            cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);require(level.addFreshEntity(cow),"disposable native victim admitted");return cow;
        }
        void causalFalls(){
            for(boolean voidDeath:List.of(false,true)){
                reset();var cow=cow(new Vec3(center.getX()+.5,center.getY(),center.getZ()+.5));
                try{
                    cow.setOnGround(true);require(!level.getBlockState(center.below()).isAir(),"real support before source removal");
                    batch(LbcHoleDropItem.Kind.NORMAL);
                    require(level.getBlockState(center.below()).isAir(),"pit actually removes victim support");cow.setOnGround(false);
                    cow.setHealth(1);cow.damageCooldownTime=0;
                    var source=voidDeath?cow.damageSources().fellOutOfWorld():cow.damageSources().fall();
                    cow.hurtServer(level,source,1000);
                    require(cow.isDeadOrDying() && cow.getKillCredit()==alpha && cow.getLastDamageSource()!=null
                            && cow.getLastDamageSource().getEntity()==null && cow.getLastDamageSource().is(voidDeath?net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD:net.minecraft.world.damagesource.DamageTypes.FALL),
                            "real support removal retains thrower through native "+(voidDeath?"void":"fall")+" death/environment source");
                }finally{game.items().indirectKills().clear(cow);cow.discard();}
            }
            reset();var newer=cow(new Vec3(center.getX()+.5,center.getY(),center.getZ()+.5));
            try{
                newer.setOnGround(true);batch(LbcHoleDropItem.Kind.NORMAL);newer.setOnGround(false);
                newer.hurtServer(level,newer.damageSources().playerAttack(beta),1);newer.setHealth(1);newer.damageCooldownTime=0;newer.setInvulnerableTime(0);
                newer.hurtServer(level,newer.damageSources().fall(),1000);
                require(newer.isDeadOrDying() && newer.getKillCredit()==beta,"newer actual attacker takes precedence over pit owner");
            }finally{game.items().indirectKills().clear(newer);newer.discard();}
        }
        void fireContact(){
            for(var kind:LbcHoleDropItem.Kind.values()){
                reset();batch(kind);var at=new Vec3(center.getX()+.5,center.getY()-17,center.getZ()+.5);var cow=cow(at);
                try{
                    // Native movement contact collector applies the actual fire block; no synthetic IN_FIRE hit.
                    for(int i=0;i<25 && cow.getLastDamageSource()==null;i++)cow.applyEffectsFromBlocks(cow.position(),cow.position());
                    require(cow.getHealth()<cow.getMaxHealth() && cow.getLastDamageSource()!=null && cow.getLastDamageSource().getEntity()==null
                            && cow.getKillCredit()==alpha,"native fire contact keeps environmental source and author "+kind);
                    cow.setHealth(1);cow.damageCooldownTime=0;cow.setInvulnerableTime(0);
                    for(int i=0;i<25 && cow.isAlive();i++){cow.damageCooldownTime=0;cow.setInvulnerableTime(0);cow.applyEffectsFromBlocks(cow.position(),cow.position());}
                    require(cow.isDeadOrDying() && cow.getKillCredit()==alpha,"native fire contact death credited "+kind);
                }finally{game.items().indirectKills().clear(cow);cow.discard();}
            }
            reset();batch(LbcHoleDropItem.Kind.OLD);
            var board=game.server().getScoreboard();var team=board.addPlayerTeam("codex_hole_wild");
            try{
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                beta.teleportTo(level,center.getX()+.5,center.getY()-17,center.getZ()+.5,Set.of(),0,0,true);
                beta.noPhysics=false;beta.setHealth(1000);beta.damageCooldownTime=0;beta.setInvulnerableTime(0);
                for(int i=0;i<25 && beta.getHealth()==1000;i++)beta.applyEffectsFromBlocks(beta.position(),beta.position());
                require(beta.getHealth()<1000 && beta.getLastDamageSource()!=null && beta.getLastDamageSource().getEntity()==null
                        && beta.getKillCredit()==alpha,"WILD real fire contact harms allied player with causal author");
            }finally{board.removePlayerTeam(team);beta.setRemainingFireTicks(0);placeBeta();}
        }
        boolean step(){
            require(game.playing(),"fixture keeps actual match active");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{for(int i=0;i<2;i++)require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(i),new ItemStack(LbcHoleDropItem.Kind.values()[i].item())),"give complete presentation "+ids[i]);require(level.getBlockState(center).is(Blocks.STONE),"held/give tokens cannot activate");phase++;}
                case 1->{
                    // A vanilla item carrying only the visual model is neither a native token nor a source flag.
                    var fake=new ItemStack(Items.REPEATING_COMMAND_BLOCK);fake.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:drop_trap"));
                    var f=token(level,origin,fake,alpha);tokens.add(f);require(!game.items().specialDrops().spawn(f),"fake visual cannot activate");f.discard();
                    var visual=drop(LbcHoleDropItem.Kind.NORMAL,1);visual.makeFakeItem();require(!game.items().specialDrops().spawn(visual),"native fake give item cannot activate");visual.discard();
                    alpha.getInventory().clearContent();targets();
                    for(var kind:LbcHoleDropItem.Kind.values()){reset();batch(kind);geometry(kind);}
                    containers();transactions();fillLimit();residency();causalFalls();fireContact();reset();stack=drop(LbcHoleDropItem.Kind.NORMAL,3);phase++;
                }
                case 2->{require(!stack.isRemoved() && stack.getItem().getCount()==2,"shared dispatcher stack3 first tick");geometry(LbcHoleDropItem.Kind.NORMAL);phase++;}
                case 3->{require(!stack.isRemoved() && stack.getItem().getCount()==1,"shared dispatcher stack2 next tick");phase++;}
                case 4->{require(stack.isRemoved(),"last unit removes token");reset();unloaded=drop(LbcHoleDropItem.Kind.NORMAL,1);game.items().specialDrops().unloaded(unloaded);phase++;}
                case 5->{require(!unloaded.isRemoved() && level.getBlockState(center).is(Blocks.STONE),"unloaded pending cannot activate");unloaded.discard();game.items().timeStop().begin(level,200);paused=drop(LbcHoleDropItem.Kind.NORMAL,2);phase++;}
                case 6->{require(!paused.isRemoved() && paused.getItem().getCount()==2 && level.getBlockState(center).is(Blocks.STONE),"time stop prevents pit/debit");if(++wait>=3){paused.discard();game.items().timeStop().end(level);phase++;}}
                case 7->{var stale=drop(LbcHoleDropItem.Kind.NORMAL,1);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().holeDrops().spawn(stale,LbcHoleDropItem.Kind.NORMAL,beta),"stale epoch rejected");stale.discard();game.items().specialDrops().stop();phase++;}
                case 8->{require(level.getBlockState(center).is(Blocks.STONE),"quiet stop prevents delayed pit");return true;}
                default->throw new IllegalStateException("Unknown hole phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();
            // Native discard mutates the live entity iterator; collect before removing anything.
            var leftovers=new ArrayList<Entity>();
            for(var e:level.getAllEntities())if(e!=null && !existing.contains(e) && !(e instanceof ServerPlayer) && e.position().distanceTo(origin)<30)leftovers.add(e);
            leftovers.forEach(Entity::discard);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());
            alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaMobKills);
            beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaMobKills);
            var board=game.server().getScoreboard();if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);
            alphaState.restore(game);betaState.restore(game);alpha.noPhysics=alphaNoPhysics;beta.noPhysics=betaNoPhysics;
        }
    }
    private static void closeAfterFailure(State state,Throwable failure){
        try{state.close();}catch(Throwable cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}
        finally{running=null;}
    }
    public static void run(GameController game){
        require(running==null,"already running");running=new State(game);
        try{running.initialize();}catch(RuntimeException|Error failure){closeAfterFailure(running,failure);throw failure;}
    }
    public static void tick(GameController game){
        if(running==null)return;var state=running;
        try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS hole-drops");}}
        catch(Throwable failure){closeAfterFailure(state,failure);SkyWars2.LOGGER.error("[check] FAIL hole-drops",failure);}
    }
    public static void stopProbe(GameController game){
        require(game.playing(),"stop probe in actual match");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        game.items().timeStop().end(level);stopEpoch=game.items().epoch();
        stopToken=token(level,alpha.position(),new ItemStack(LbcHoleDropItem.Kind.NORMAL.item(),3),alpha);stopToken.entityTags().add(stopEpoch);
        game.items().timeStop().begin(level,200);
        require(game.items().specialDrops().pendingCount()>0 && stopToken.getItem().getCount()==3,"frozen real pit token pending for actual match stop");
        SkyWars2.LOGGER.info("[check] PASS hole-drops-stop-probe");
    }
    public static void cleanup(GameController game){
        require(!game.items().currentEpoch(stopEpoch) && game.items().specialDrops().pendingCount()==0,"real stop clears common pending and epoch");
        if(stopToken!=null){require(!game.items().specialDrops().spawn(stopToken),"old epoch cannot make a delayed pit");stopToken.discard();}
        stopToken=null;stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS hole-drops-cleanup");
    }
}
