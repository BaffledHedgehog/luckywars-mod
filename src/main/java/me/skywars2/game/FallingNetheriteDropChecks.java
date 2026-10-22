package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded actual-match fixtures. Landing is produced by native gravity, never a synthetic ground flag. */
public final class FallingNetheriteDropChecks {
    private FallingNetheriteDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<FallingBlockEntity> stopActors=List.of();
    private static long stopBolts;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Falling netherite drops: "+message);}
    private static ItemEntity token(ServerLevel level,Vec3 at,ItemStack stack,LivingEntity author){
        var t=new ItemEntity(level,at.x,at.y,at.z,stack);if(author!=null)t.setThrower(author);
        t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);t.setPickUpDelay(1000);
        require(level.addFreshEntity(t),"native item admission");return t;
    }
    private static net.minecraft.nbt.CompoundTag saved(Entity e,ServerLevel level){
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        e.saveWithoutId(out);return out.buildResult();
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ItemEntity> tokens=new ArrayList<>();final Vec3 origin;final BlockPos center;
        final int maxBlocks,alphaKills,betaKills;
        ItemEntity stack,paused,unloaded;FallingBlockEntity falling,foreign;Vec3 frozenPos,frozenMotion;
        int phase,lastTick,wait,variant;long baseline;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            maxBlocks=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS);
            alphaKills=kills(alpha);betaKills=kills(beta);
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);center=BlockPos.containing(origin);
            level.getAllEntities().forEach(existing::add);
        }
        int kills(ServerPlayer p){return p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));}
        void initialize(){
            for(var p:BlockPos.betweenClosed(center.offset(-4,-35,-4),center.offset(4,17,4)))save(p.immutable());
            game.items().specialDrops().stop();clear();
            for(var p:List.of(alpha,beta)){
                p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);
                p.setNoGravity(true);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);
            }
            away();for(var kind:LbcFallingNetheriteDropItem.Kind.values())presentation(kind);
            lastTick=game.server().getTickCount();
        }
        void save(BlockPos p){
            if(blocks.containsKey(p))return;long key=net.minecraft.world.level.ChunkPos.pack(p.getX()>>4,p.getZ()>>4);
            if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(p.getX()>>4,p.getZ()>>4,true);}
            level.getChunkAt(p);blocks.put(p,game.maps().snapshot(p));
            if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
        }
        void away(){alpha.teleportTo(level,origin.x+20,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+22,origin.y,origin.z,Set.of(),0,0,true);}
        void presentation(LbcFallingNetheriteDropItem.Kind kind){
            var stack=new ItemStack(kind.item());
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr(kind.flag(),0)==1 && data.getIntOr("drop",0)==1,"exact source flags "+kind);
            if(kind==LbcFallingNetheriteDropItem.Kind.NORMAL){
                require(Identifier.parse("lbc:drop_not_trap").equals(stack.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_f").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"exact normal source model and tier");
                require(stack.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals("drop_netherite")
                        && !stack.getHoverName().getStyle().isItalic() && stack.getHoverName().getStyle().getColor().getValue()==0xffff55,"normal source name style");
                var lore=stack.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();
                require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc")
                        && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"normal source lore");
            }else require(Identifier.parse("minecraft:barrier").equals(stack.get(DataComponents.ITEM_MODEL)) && !stack.has(DataComponents.CUSTOM_NAME) && !stack.has(DataComponents.TOOLTIP_STYLE)
                    && stack.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines().isEmpty(),"old source plain barrier");
            var loot=game.lbc().rollTable(level,origin,alpha,kind==LbcFallingNetheriteDropItem.Kind.OLD?"lbc:ported/falling_netherite_drop_old":"lbc:ported/falling_netherite_drop");
            require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(stack,loot.getFirst()),"complete ported/native equality "+kind);
            game.command(kind==LbcFallingNetheriteDropItem.Kind.OLD?"give TestAlpha skywars2:falling_netherite_drop_old 1":"give TestAlpha skywars2:falling_netherite_drop 1");
        }
        ItemEntity drop(LbcFallingNetheriteDropItem.Kind kind,int n){var t=token(level,origin,new ItemStack(kind.item(),n),alpha);tokens.add(t);return t;}
        void clear(){game.items().fallingNetheriteDrops().stop();}
        FallingBlockEntity batch(LbcFallingNetheriteDropItem.Kind kind){
            clear();var t=drop(kind,3);t.setTarget(beta.getUUID());
            require(game.items().fallingNetheriteDrops().ready(t,kind) && game.items().fallingNetheriteDrops().spawn(t,kind),"native service admission "+kind);
            require(t.getItem().getCount()==3 && !t.isRemoved(),"payload service leaves debit to dispatcher");t.discard();
            var actors=game.items().fallingNetheriteDrops().snapshot();require(actors.size()==1,"one native falling block");var f=actors.getFirst();
            double x=kind==LbcFallingNetheriteDropItem.Kind.NORMAL?center.getX()+.5:origin.x;
            double z=kind==LbcFallingNetheriteDropItem.Kind.NORMAL?center.getZ()+.5:origin.z;
            require(f.getClass()==FallingBlockEntity.class && f.getBlockState().is(Blocks.NETHERITE_BLOCK),"exact native netherite class/state");
            require(f.position().distanceTo(new Vec3(x,origin.y+14,z))<1e-8 && f.getDeltaMovement().equals(Vec3.ZERO),"source position +14Y and zero motion "+kind);
            var tag=saved(f,level);
            require(tag.getIntOr("Time",-1)==1 && tag.getBooleanOr("DropItem",false) && !tag.getBooleanOr("HurtEntities",true),"native Time1/default drop/no falling damage");
            require(!f.isNoGravity() && game.items().fallingNetheriteDrops().owner(f)==alpha && game.items().currentEpoch(f),"gravity, actual thrower and epoch");return f;
        }
        void guards(){
            var fake=new ItemStack(Items.PAPER);fake.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:drop_trap"));
            var f=token(level,origin,fake,alpha);tokens.add(f);require(!game.items().specialDrops().spawn(f),"visual fake rejected");f.discard();
            var visual=drop(LbcFallingNetheriteDropItem.Kind.NORMAL,1);visual.makeFakeItem();require(!game.items().specialDrops().spawn(visual),"native fake rejected");visual.discard();
            var stale=drop(LbcFallingNetheriteDropItem.Kind.NORMAL,2);stale.entityTags().add("skywars2.items.stale-fixture");
            require(!game.items().fallingNetheriteDrops().ready(stale,LbcFallingNetheriteDropItem.Kind.NORMAL) && !game.items().fallingNetheriteDrops().spawn(stale,LbcFallingNetheriteDropItem.Kind.NORMAL),"stale epoch rejected");stale.discard();
            var remote=drop(LbcFallingNetheriteDropItem.Kind.NORMAL,2);int x=(center.getX()>>4)+128,z=(center.getZ()>>4)+128;
            while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;remote.setPos(x*16+8.37,origin.y,z*16+8.81);
            require(!game.items().fallingNetheriteDrops().ready(remote,LbcFallingNetheriteDropItem.Kind.NORMAL) && !game.items().fallingNetheriteDrops().spawn(remote,LbcFallingNetheriteDropItem.Kind.NORMAL)
                    && level.getChunkSource().getChunkNow(x,z)==null && remote.getItem().getCount()==2,"unresident guard does not load or debit");remote.discard();
        }
        void fill(){
            for(var kind:LbcFallingNetheriteDropItem.Kind.values()){
                for(int y=0;y<16;y++)level.setBlock(center.above(y),Blocks.STONE.defaultBlockState(),818);
                level.setBlock(center.below(),Blocks.STONE.defaultBlockState(),818);level.setBlock(center.above(16),Blocks.STONE.defaultBlockState(),818);
                batch(kind);for(int y=0;y<16;y++)require(level.getBlockState(center.above(y)).isAir(),"exact sixteen shaft cells "+kind);
                require(level.getBlockState(center.below()).is(Blocks.STONE) && level.getBlockState(center.above(16)).is(Blocks.STONE),"shaft vertical bounds "+kind);clear();
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,15,game.server());
                try{level.setBlock(center.above(5),Blocks.STONE.defaultBlockState(),818);batch(kind);
                    require(level.getBlockState(center.above(5)).is(Blocks.STONE),"fill16 rejected independently of falling spawn "+kind);
                }finally{clear();level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());}
            }
            for(int y=-1;y<=16;y++)level.setBlock(center.above(y),Blocks.AIR.defaultBlockState(),818);
        }
        void beginFall(){
            for(int y=-34;y<=16;y++)level.setBlock(center.above(y),Blocks.AIR.defaultBlockState(),818);
            level.setBlock(center.below(35),Blocks.STONE.defaultBlockState(),818);
            level.setBlock(center.offset(2,-35,0),Blocks.STONE.defaultBlockState(),818);
            baseline=game.items().fallingNetheriteDrops().totalBoltsSpawned();falling=batch(LbcFallingNetheriteDropItem.Kind.values()[variant]);
            foreign=EntityTypes.FALLING_BLOCK.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(foreign!=null,"foreign native falling block created");
            var foreignData=new net.minecraft.nbt.CompoundTag();foreignData.putString("BlockState","minecraft:netherite_block");foreignData.putInt("Time",1);
            foreign.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),foreignData));
            foreign.setPos(origin.add(2,14,0));foreign.setDeltaMovement(Vec3.ZERO);require(level.addFreshEntity(foreign),"foreign native falling block admitted");wait=0;
        }
        void lightning(LightningBolt bolt){
            require(bolt.getCause()==alpha && game.items().fallingNetheriteDrops().owner(bolt)==alpha,"landing lightning retains actual author");
            var expected=new BlockPos(center.getX(),center.getY()-34,center.getZ());
            require(level.getBlockState(expected).is(Blocks.NETHERITE_BLOCK) && Math.abs(bolt.getX()-falling.getX())<1e-6 && Math.abs(bolt.getZ()-falling.getZ())<1e-6
                    && Math.abs(bolt.getY()-falling.getY())<.1,"one lightning at physical native placement, never launch point");
            var cow=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);cow.setPos(bolt.position());cow.setNoAi(true);cow.setNoGravity(true);
            var npc=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);npc.setPos(bolt.position().add(0,0,1));npc.setNoAi(true);npc.setNoGravity(true);npc.entityTags().add("magic_academy");
            require(level.addFreshEntity(cow) && level.addFreshEntity(npc),"disposable lightning victims");int kills=kills(alpha);
            var board=game.server().getScoreboard();var oldAlpha=alpha.getTeam();var oldBeta=beta.getTeam();
            require(board.getPlayerTeam("codex_falling_probe")==null,"fixture team available");var team=board.addPlayerTeam("codex_falling_probe");team.setAllowFriendlyFire(false);
            try{
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                for(var p:List.of(alpha,beta)){p.teleportTo(level,bolt.getX()+1,bolt.getY(),bolt.getZ(),Set.of(),0,0,true);p.setHealth(1000);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.setRemainingFireTicks(0);}
                cow.setHealth(1);bolt.tick();require(cow.isDeadOrDying() && cow.getKillCredit()==alpha && kills(alpha)==kills+1,"native lightning fatal credit on disposable mob");
                require(alpha.getHealth()<1000 && beta.getHealth()<1000 && beta.getLastDamageSource()!=null && beta.getLastDamageSource().getEntity()==alpha
                        && beta.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT),"WILD native lightning damages self and allied participant with author and native type");
                require(npc.getHealth()==npc.getMaxHealth() && !npc.isOnFire(),"native lightning excludes protected role");
                float a=alpha.getHealth(),b=beta.getHealth();bolt.tick();require(alpha.getHealth()==a && beta.getHealth()==b,"native lightning retains ordinary hurt cooldown");
            }finally{
                board.removePlayerTeam(team);if(oldAlpha!=null)board.addPlayerToTeam(alpha.getScoreboardName(),oldAlpha);if(oldBeta!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBeta);
                for(var p:List.of(alpha,beta))p.setRemainingFireTicks(0);away();game.items().indirectKills().clear(cow);cow.discard();npc.discard();
            }
        }
        boolean step(){
            require(game.playing(),"actual match retained");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{for(int i=0;i<2;i++)require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(i),new ItemStack(LbcFallingNetheriteDropItem.Kind.values()[i].item())),"native give complete components");require(game.items().fallingNetheriteDrops().count()==0,"held/give cannot activate");alpha.getInventory().clearContent();guards();fill();stack=drop(LbcFallingNetheriteDropItem.Kind.NORMAL,3);phase++;}
                case 1->{require(stack.getItem().getCount()==2 && game.items().fallingNetheriteDrops().count()==1,"stack3 one native unit");require(!game.items().specialDrops().spawn(stack) && stack.getItem().getCount()==2,"same tick cannot debit twice");clear();phase++;}
                case 2->{require(stack.getItem().getCount()==1 && game.items().fallingNetheriteDrops().count()==1,"next tick one unit");clear();phase++;}
                case 3->{require(stack.isRemoved() && game.items().fallingNetheriteDrops().count()==1,"last unit removes token");clear();unloaded=drop(LbcFallingNetheriteDropItem.Kind.NORMAL,1);game.items().specialDrops().unloaded(unloaded);phase++;}
                case 4->{require(!unloaded.isRemoved() && game.items().fallingNetheriteDrops().count()==0,"unloaded pending cannot activate");unloaded.discard();falling=batch(LbcFallingNetheriteDropItem.Kind.NORMAL);phase++;}
                case 5->{frozenPos=falling.position();frozenMotion=falling.getDeltaMovement();game.items().timeStop().begin(level,200);paused=drop(LbcFallingNetheriteDropItem.Kind.OLD,2);wait=0;phase++;}
                case 6->{require(falling.position().distanceTo(frozenPos)<1e-8 && paused.getItem().getCount()==2,"time stop freezes native falling position and pending debit");if(++wait>=3){paused.discard();game.items().timeStop().end(level);require(falling.getDeltaMovement().distanceTo(frozenMotion)<1e-8,"time stop restores native motion");baseline=game.items().fallingNetheriteDrops().totalBoltsSpawned();falling.discard();phase++;}}
                case 7->{
                    require(game.items().fallingNetheriteDrops().totalBoltsSpawned()==baseline,"midair discard cannot strike");
                    falling=batch(LbcFallingNetheriteDropItem.Kind.NORMAL);var data=saved(falling,level);data.putInt("Time",600);
                    falling.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),data));phase=11;
                }
                case 8->{
                    wait++;if(wait==26)require(!falling.isRemoved() && !falling.onGround() && game.items().fallingNetheriteDrops().totalBoltsSpawned()==baseline,"past 25 ticks airborne remains bolt-free");
                    if(falling.isRemoved()){
                        require(wait>26 && wait<100 && game.items().fallingNetheriteDrops().totalBoltsSpawned()==baseline+1,"native long-fall landing creates exactly one bolt");
                        var bolts=game.items().fallingNetheriteDrops().snapshotBolts();require(bolts.size()==1,"only own falling block strikes; foreign netherite does not");lightning(bolts.getFirst());wait=0;phase++;
                    }else require(wait<100,"bounded actual native fall completes");
                }
                case 9->{require(game.items().fallingNetheriteDrops().totalBoltsSpawned()==baseline+1,"landing deduplicated across later ticks");if(++wait>=4){foreign.discard();clear();if(++variant<2){beginFall();phase=8;}else{baseline=game.items().fallingNetheriteDrops().totalBoltsSpawned();falling=batch(LbcFallingNetheriteDropItem.Kind.NORMAL);clear();phase++;}}}
                case 10->{require(falling.isRemoved() && game.items().fallingNetheriteDrops().count()==0 && game.items().fallingNetheriteDrops().totalBoltsSpawned()==baseline,"quiet native stop cannot strike");game.items().specialDrops().stop();return true;}
                case 11->{
                    require(falling.isRemoved() && !falling.onGround() && game.items().fallingNetheriteDrops().totalBoltsSpawned()==baseline,"native midair timeout artifact conversion cannot strike");
                    boolean artifact=false;for(var e:level.getAllEntities())if(!existing.contains(e) && e instanceof ItemEntity item && item.getItem().is(Items.NETHERITE_BLOCK) && item.position().distanceTo(falling.position())<2)artifact=true;
                    require(artifact,"native default falling item artifact survives conversion");beginFall();phase=8;
                }
                default->throw new IllegalStateException("Unknown falling phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();clear();
            var leftovers=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e) && !(e instanceof ServerPlayer) && Math.abs(e.getX()-origin.x)<6 && Math.abs(e.getZ()-origin.z)<6)leftovers.add(e);
            leftovers.forEach(Entity::discard);for(var e:blocks.entrySet())game.maps().restoreSnapshot(e.getKey(),e.getValue());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());
            alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaKills);beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaKills);
            alphaState.restore(game);betaState.restore(game);
        }
    }
    private static void closeFailure(State state,Throwable failure){try{state.close();}catch(Throwable cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}finally{running=null;}}
    public static void run(GameController game){require(running==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error failure){closeFailure(running,failure);throw failure;}}
    public static void tick(GameController game){if(running==null)return;var state=running;try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS falling-netherite-drops");}}catch(Throwable failure){closeFailure(state,failure);SkyWars2.LOGGER.error("[check] FAIL falling-netherite-drops",failure);}}
    public static void stopProbe(GameController game){
        require(game.playing(),"actual match stop probe");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        game.items().timeStop().end(level);stopEpoch=game.items().epoch();stopBolts=game.items().fallingNetheriteDrops().totalBoltsSpawned();
        stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),new ItemStack(LbcFallingNetheriteDropItem.Kind.NORMAL.item(),3),alpha);
        int limit=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS);
        try{
            // Reject the independent shaft fill so this lifecycle-only probe cannot alter arena blocks.
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,15,game.server());
            require(game.items().fallingNetheriteDrops().spawn(stopToken,LbcFallingNetheriteDropItem.Kind.NORMAL),"live native stop payload");
        }finally{level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,limit,game.server());}
        stopActors=game.items().fallingNetheriteDrops().snapshot();
        require(stopActors.size()==1,"one native falling block before actual stop");game.items().timeStop().begin(level,200);SkyWars2.LOGGER.info("[check] PASS falling-netherite-drops-stop-probe");
    }
    public static void cleanup(GameController game){
        require(!game.items().currentEpoch(stopEpoch) && game.items().fallingNetheriteDrops().count()==0 && game.items().specialDrops().pendingCount()==0,"actual stop clears epoch and services");
        require(stopActors.stream().allMatch(Entity::isRemoved) && game.items().fallingNetheriteDrops().totalBoltsSpawned()==stopBolts,"actual match stop discards without impact lightning");
        if(stopToken!=null)stopToken.discard();stopToken=null;stopActors=List.of();stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS falling-netherite-drops-cleanup");
    }
}
