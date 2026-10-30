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
public final class CorrosionDropChecks {
    private CorrosionDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Corrosion drops: "+message);}
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
        final String[] flags={"korrosion","korrosion_old"};
        final String[] ids={"skywars2:corrosion_drop","skywars2:corrosion_drop_old"};
        final String[] names={"trap_corrosion","block.minecraft.barrier"};
        final String[] lootIds={"lbc:ported/corrosion_drop","lbc:ported/corrosion_drop_old"};
        ItemEntity stack,paused,unloaded;int phase,lastTick,wait,naturalTicks;boolean naturalProgress;
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
            origin=new Vec3(-55.37, Math.min(270.7,level.getMaxY()-26.3),-55.81);center=BlockPos.containing(origin);
            level.getAllEntities().forEach(existing::add);
        }
        void initialize(){
            // Snapshot the full random footprint and shape-update halo before any edit.
            for(var p:BlockPos.betweenClosed(center.offset(-26,-26,-26),center.offset(26,26,26)))if(!level.isOutsideBuildHeight(p))save(p.immutable());
            game.items().specialDrops().stop();
            var board=game.server().getScoreboard();board.removePlayerFromTeam(alpha.getScoreboardName());board.removePlayerFromTeam(beta.getScoreboardName());
            for(var player:List.of(alpha,beta)){
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.noPhysics=false;player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
            }
            alpha.teleportTo(level,origin.x+60,origin.y+8,origin.z,Set.of(),0,0,true);reset();
            for(int i=0;i<2;i++)presentation(i);
            lastTick=game.server().getTickCount();
        }
        void save(BlockPos p){
            if(blocks.containsKey(p))return;long key=net.minecraft.world.level.ChunkPos.pack(p.getX()>>4,p.getZ()>>4);
            if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(p.getX()>>4,p.getZ()>>4,true);}
            level.getChunkAt(p);blocks.put(p,game.maps().snapshot(p));
            if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
        }
        void placeBeta(){beta.teleportTo(level,origin.x+50,origin.y+8,origin.z,Set.of(),0,0,true);beta.setDeltaMovement(Vec3.ZERO);}
        void presentation(int i){
            var kind=LbcCorrosionDropItem.Kind.values()[i];var stack=new ItemStack(kind.item());var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(kind.flag().equals(flags[i]) && kind.id().toString().equals(ids[i]) && kind.nameKey().equals(names[i]),"independent source IDs "+kind);
            require(data.size()==2 && data.getIntOr(flags[i],0)==1 && data.getIntOr("drop",0)==1,"exact source flags "+kind);
            if(kind==LbcCorrosionDropItem.Kind.OLD){
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
            var loot=game.lbc().rollTable(level,origin,alpha,lootIds[i]);
            require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(stack,loot.getFirst()),"native/ported complete presentation "+kind);
            game.command("give TestAlpha "+ids[i]+" 1");
        }
        ItemEntity drop(LbcCorrosionDropItem.Kind kind,int count){var t=token(level,origin,new ItemStack(kind.item(),count),alpha);tokens.add(t);return t;}
        void reset(){
            game.items().corrosionDrops().stop();
            for(var p:blocks.keySet()){
                if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();
                if(!level.getBlockState(p).isAir())level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
            }
            placeBeta();
        }
        void put(BlockPos p,net.minecraft.world.level.block.Block block){save(p);level.setBlock(p,block.defaultBlockState(),818);}
        void drain(){int steps=game.items().corrosionDrops().drainForCheck();require(steps>=0 && steps<=100000 && game.items().corrosionDrops().count()==0,"bounded complete queued work");}
        void batch(LbcCorrosionDropItem.Kind kind,long seed){
            var before=beta.position();var t=drop(kind,3);
            require(game.items().corrosionDrops().ready(t,kind) && game.items().corrosionDrops().spawnForCheck(t,kind,seed),"resident native corrosion admitted "+kind);
            require(!t.isRemoved() && t.getItem().getCount()==3,"geometry leaves debit to dispatcher");
            drain();require(beta.position().equals(before),"token-position geometry never teleports nearest player");t.discard();
        }
        void geometry(){
            reset();
            // All six outside faces prove floor(-55.37)=-56 and the asymmetric48-cell extent.
            var outside=List.of(center.offset(-25,0,0),center.offset(24,0,0),center.offset(0,-25,0),center.offset(0,24,0),center.offset(0,0,-25),center.offset(0,0,24));
            for(var p:outside)put(p,Blocks.BEDROCK);
            var inside=new ArrayList<BlockPos>();
            for(int i=-24;i<=23;i++)for(int j=-4;j<=4;j++){
                inside.add(center.offset(i,j,0));inside.add(center.offset(0,j,i));
            }
            for(var p:inside)put(p,Blocks.BEDROCK);
            batch(LbcCorrosionDropItem.Kind.NORMAL,73);
            int removed=0;for(var p:inside)if(level.getBlockState(p).isAir())removed++;
            require(removed>inside.size()/3 && removed<inside.size()*2/3,"independent approximately50% random bedrock placements");
            for(var p:outside)require(level.getBlockState(p).is(Blocks.BEDROCK),"no49th-cell/negative-floor overreach "+p);
            for(int edge:List.of(-24,23)){
                int n=0;for(int j=-4;j<=4;j++)if(level.getBlockState(center.offset(edge,j,0)).isAir())n++;
                require(n>0,"both asymmetric cube edges participate "+edge);
            }
            for(var block:List.of(Blocks.WATER,Blocks.LIGHT,Blocks.STRUCTURE_VOID)){
                reset();for(var p:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3)))put(p.immutable(),block);
                batch(LbcCorrosionDropItem.Kind.NORMAL,902);
                int air=0;for(var p:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3)))if(level.getBlockState(p).isAir())air++;
                require(air>100 && air<240,"native random eligibility "+block);
            }
            reset();for(var p:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(7,7,7)))put(p.immutable(),Blocks.BEDROCK);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,1,game.server());
            try{batch(LbcCorrosionDropItem.Kind.NORMAL,55);int air=0;
                for(var p:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(7,7,7)))if(level.getBlockState(p).isAir())air++;
                require(air>1700 && air<2400,"dense4096 statistical mask; random template has no fill-limit gate");
            }finally{level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());}
        }
        int count(ItemStack stack){int n=0;for(var e:level.getAllEntities())if(e instanceof ItemEntity item && ItemStack.isSameItemSameComponents(item.getItem(),stack))n+=item.getItem().getCount();return n;}
        void containers(){
            var gem=new ItemStack(Items.DIAMOND,7);gem.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("corrosion container probe"));
            Long skipped=null,selected=null;
            for(int attempt=1;attempt<=32 && (skipped==null || selected==null);attempt++){long seed=attempt*1_000_003L;
                reset();put(center,Blocks.CHEST);((net.minecraft.world.Container)level.getBlockEntity(center)).setItem(0,gem.copy());int before=count(gem);
                batch(LbcCorrosionDropItem.Kind.NORMAL,seed);
                if(level.getBlockState(center).isAir()){require(count(gem)==before+7,"native template ejects complete replaced inventory");selected=seed;}
                else{require(count(gem)==before,"mask-skipped container never ejects");skipped=seed;}
            }
            require(skipped!=null && selected!=null,"deterministic seed probes cover both native mask branches");
            reset();put(center,Blocks.CHEST);((net.minecraft.world.Container)level.getBlockEntity(center)).setItem(0,gem.copy());put(center.above(),Blocks.BEDROCK);int before=count(gem);
            batch(LbcCorrosionDropItem.Kind.OLD,skipped);
            require(level.getBlockState(center).isAir() && level.getBlockState(center.above()).isAir() && count(gem)==before,"old independent strict center removes skipped inventory without ejection");
            reset();put(center,Blocks.CHEST);((net.minecraft.world.Container)level.getBlockEntity(center)).setItem(0,gem.copy());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,1,game.server());
            try{batch(LbcCorrosionDropItem.Kind.OLD,skipped);require(level.getBlockState(center).is(Blocks.CHEST),"old two-cell fill respects limit independently of random template");}
            finally{level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,maxBlocks,game.server());}
        }
        void residency(){
            var t=drop(LbcCorrosionDropItem.Kind.NORMAL,2);var previous=t.position();int x=4096,z=4096;
            while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;
            try{
                t.setPos(x*16+8.37,origin.y,z*16+8.81);
                require(!game.items().corrosionDrops().ready(t,LbcCorrosionDropItem.Kind.NORMAL) && !game.items().corrosionDrops().spawn(t,LbcCorrosionDropItem.Kind.NORMAL),"unresident footprint rejected");
                require(level.getChunkSource().getChunkNow(x,z)==null && t.getItem().getCount()==2,"readiness cannot force-load/debit");
                // origin+48, unlike actual last cell+47, reaches one more X chunk.
                // Forced tickets make adjacent FULL chunks resident too; use exact native requests
                // so the deliberately absent +48 column stays outside this fixture footprint.
                int base=x*16;for(int cx=x;cx<x+3;cx++)for(int cz=z;cz<=z+3;cz++)level.getChunk(cx,cz);
                t.setPos(base+24.37,origin.y,z*16+24.81);
                require(level.getChunkSource().getChunkNow(x+3,z+1)==null && !game.items().corrosionDrops().ready(t,LbcCorrosionDropItem.Kind.NORMAL),"native+48 extra X chunk preflight");
            }finally{t.setPos(previous);t.discard();}
        }
        net.minecraft.world.entity.animal.cow.Cow cow(Vec3 at){var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);require(level.addFreshEntity(cow),"native causal victim admitted");return cow;}
        void causalFalls(){
            for(int mode=0;mode<3;mode++){
                reset();var victim=cow(new Vec3(center.getX()+.5,center.getY(),center.getZ()+.5));
                try{
                    boolean removed=false;
                    for(int attempt=1;attempt<=32 && !removed;attempt++){long seed=attempt*1_000_003L;put(center.below(),Blocks.BEDROCK);victim.setOnGround(true);batch(LbcCorrosionDropItem.Kind.NORMAL,seed);removed=level.getBlockState(center.below()).isAir();}
                    require(removed,"random native writes actually remove victim support");victim.setOnGround(false);
                    if(mode==2)victim.hurtServer(level,victim.damageSources().playerAttack(beta),1);
                    victim.setHealth(1);victim.damageCooldownTime=0;victim.setInvulnerableTime(0);
                    victim.hurtServer(level,mode==1?victim.damageSources().fellOutOfWorld():victim.damageSources().fall(),1000);
                    require(victim.isDeadOrDying() && victim.getKillCredit()==(mode==2?beta:alpha) && victim.getLastDamageSource().getEntity()==null,"native support-removal fall/void author and latest-attacker precedence "+mode);
                }finally{game.items().indirectKills().clear(victim);victim.discard();}
            }
            reset();var board=game.server().getScoreboard();var team=board.addPlayerTeam("codex_corrosion_wild");
            boolean oldFallDamage=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE);
            // Actual match spawn grace disables player fall damage; isolate this native contact probe.
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE,true,game.server());
            try{
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                beta.teleportTo(level,center.getX()+.5,center.getY(),center.getZ()+.5,Set.of(),0,0,true);
                boolean removed=false;for(int attempt=1;attempt<=32 && !removed;attempt++){long seed=attempt*1_000_003L;put(center.below(),Blocks.BEDROCK);beta.setOnGround(true);batch(LbcCorrosionDropItem.Kind.NORMAL,seed);removed=level.getBlockState(center.below()).isAir();}
                require(removed,"allied support actually removed");beta.setOnGround(false);beta.damageCooldownTime=0;beta.setInvulnerableTime(0);float health=beta.getHealth();
                beta.hurtServer(level,beta.damageSources().fall(),2);require(beta.getHealth()<health && beta.getLastDamageSource()!=null && beta.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.FALL),"native environmental fall harms allied participant; lethal credit checked separately");
            }finally{level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE,oldFallDamage,game.server());game.items().indirectKills().clear(beta);board.removePlayerTeam(team);placeBeta();}
        }
        void partialHeight(){
            reset();var at=new Vec3(origin.x,level.getMaxY()+.7,origin.z);var below=BlockPos.containing(at).below(2);put(below,Blocks.BEDROCK);
            var t=token(level,at,new ItemStack(LbcCorrosionDropItem.Kind.OLD.item()),alpha);tokens.add(t);
            require(game.items().corrosionDrops().ready(t,LbcCorrosionDropItem.Kind.OLD),"partial-height template admitted");
            boolean removed=false;for(int attempt=1;attempt<=32 && !removed;attempt++){long seed=attempt*1_000_003L;put(below,Blocks.BEDROCK);require(game.items().corrosionDrops().spawnForCheck(t,LbcCorrosionDropItem.Kind.OLD,seed),"partial-height queued");drain();removed=level.getBlockState(below).isAir();}
            require(removed,"valid cells survive partial-height template rather than complete cuboid rejection");t.discard();
        }
        void transactions(){
            reset();put(center,Blocks.BEDROCK);var t=drop(LbcCorrosionDropItem.Kind.NORMAL,2);var data=t.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("korrosion_old",1);t.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            require(game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"both flags share one debit");
            require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"no same-tick repeat");drain();require(level.getBlockState(center).isAir(),"old guaranteed center follows normal random placement");t.discard();
            reset();var a=drop(LbcCorrosionDropItem.Kind.OLD,1);var b=drop(LbcCorrosionDropItem.Kind.OLD,1);
            require(game.items().corrosionDrops().spawnForCheck(a,LbcCorrosionDropItem.Kind.OLD,1) && game.items().corrosionDrops().spawnForCheck(b,LbcCorrosionDropItem.Kind.OLD,2),"two concurrent jobs admitted");
            require(game.items().corrosionDrops().count()>=2,"both jobs retained");
            game.items().timeStop().begin(level,200);
            try{int before=game.items().corrosionDrops().count();require(game.items().corrosionDrops().drainForCheck()==0 && game.items().corrosionDrops().count()==before,"time stop retains admitted concurrent jobs without progress");}
            finally{game.items().timeStop().end(level);}
            drain();a.discard();b.discard();
        }
        boolean step(){
            require(game.playing(),"fixture keeps actual match active");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{for(int i=0;i<2;i++)require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(i),new ItemStack(LbcCorrosionDropItem.Kind.values()[i].item())),"give complete presentation "+ids[i]);require(game.items().corrosionDrops().count()==0,"held/give tokens cannot activate");phase++;}
                case 1->{
                    var fake=new ItemStack(Items.REPEATING_COMMAND_BLOCK);fake.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:drop_trap"));var f=token(level,origin,fake,alpha);tokens.add(f);require(!game.items().specialDrops().spawn(f),"visual-only fake rejected");f.discard();
                    var visual=drop(LbcCorrosionDropItem.Kind.NORMAL,1);visual.makeFakeItem();require(!game.items().specialDrops().spawn(visual),"fake age5999 rejected");visual.discard();alpha.getInventory().clearContent();
                    geometry();containers();residency();causalFalls();partialHeight();transactions();reset();stack=drop(LbcCorrosionDropItem.Kind.OLD,3);phase++;
                }
                case 2->{require(!stack.isRemoved() && stack.getItem().getCount()==2,"shared dispatcher first unit");drain();phase++;}
                case 3->{require(!stack.isRemoved() && stack.getItem().getCount()==1,"shared dispatcher second unit");drain();phase++;}
                case 4->{require(stack.isRemoved(),"last unit removes token");drain();reset();unloaded=drop(LbcCorrosionDropItem.Kind.NORMAL,1);game.items().specialDrops().unloaded(unloaded);phase++;}
                case 5->{require(!unloaded.isRemoved() && game.items().corrosionDrops().count()==0,"unloaded token cannot enqueue");unloaded.discard();game.items().timeStop().begin(level,200);paused=drop(LbcCorrosionDropItem.Kind.NORMAL,2);phase++;}
                case 6->{require(!paused.isRemoved() && paused.getItem().getCount()==2 && game.items().corrosionDrops().count()==0,"time stop prevents admission/debit");if(++wait>=3){paused.discard();game.items().timeStop().end(level);phase++;}}
                case 7->{var stale=drop(LbcCorrosionDropItem.Kind.NORMAL,1);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().corrosionDrops().spawn(stale,LbcCorrosionDropItem.Kind.NORMAL),"stale epoch rejected");stale.discard();
                    put(center,Blocks.BEDROCK);var queued=drop(LbcCorrosionDropItem.Kind.OLD,1);require(game.items().corrosionDrops().spawnForCheck(queued,LbcCorrosionDropItem.Kind.OLD,1),"pending job before quiet stop");queued.discard();game.items().corrosionDrops().stop();game.items().specialDrops().stop();phase++;}
                case 8->{
                    require(game.items().corrosionDrops().count()==0 && level.getBlockState(center).is(Blocks.BEDROCK),"quiet stop cannot finish cancelled geometry");
                    reset();for(var p:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(7,7,7)))put(p.immutable(),Blocks.BEDROCK);
                    var natural=drop(LbcCorrosionDropItem.Kind.NORMAL,1);
                    require(game.items().corrosionDrops().spawnForCheck(natural,LbcCorrosionDropItem.Kind.NORMAL,55) && game.items().corrosionDrops().count()>0,"natural shared-budget job retained after admission");natural.discard();phase++;
                }
                case 9->{
                    int removed=0;for(var p:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(7,7,7)))if(level.getBlockState(p).isAir())removed++;
                    if(removed>0 && game.items().corrosionDrops().count()>0)naturalProgress=true;
                    require(++naturalTicks<=200,"natural global scheduler finishes bounded fixture within200ticks");
                    if(game.items().corrosionDrops().count()==0){require(removed>1700 && removed<2400,"natural scheduler preserves complete statistical result without drain hook");
                        SkyWars2.LOGGER.info("[check] corrosion scheduler fixture ticks={} intermediateProgress={} (4096 blocks; excludes worst-case dense performance)",naturalTicks,naturalProgress);return true;}
                }
                default->throw new IllegalStateException("Unknown corrosion phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();game.items().corrosionDrops().stop();
            // Native discard mutates the live entity iterator; collect before removing anything.
            var leftovers=new ArrayList<Entity>();
            for(var e:level.getAllEntities())if(e!=null && !existing.contains(e) && !(e instanceof ServerPlayer) && e.position().distanceTo(origin)<90)leftovers.add(e);
            leftovers.forEach(Entity::discard);
            // Clear probe inventories before restoration can run native container replacement.
            for(var p:blocks.keySet())if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();
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
        try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS corrosion-drops");}}
        catch(Throwable failure){closeAfterFailure(state,failure);SkyWars2.LOGGER.error("[check] FAIL corrosion-drops",failure);}
    }
    public static void stopProbe(GameController game){
        require(game.playing(),"stop probe in actual match");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        game.items().timeStop().end(level);stopEpoch=game.items().epoch();
        stopToken=token(level,alpha.position(),new ItemStack(LbcCorrosionDropItem.Kind.NORMAL.item(),3),alpha);stopToken.entityTags().add(stopEpoch);
        game.items().timeStop().begin(level,200);
        require(game.items().specialDrops().pendingCount()>0 && stopToken.getItem().getCount()==3,"frozen real corrosion token pending for actual match stop");
        SkyWars2.LOGGER.info("[check] PASS corrosion-drops-stop-probe");
    }
    public static void cleanup(GameController game){
        require(!game.items().currentEpoch(stopEpoch) && game.items().specialDrops().pendingCount()==0 && game.items().corrosionDrops().count()==0,"real stop clears common pending, queued geometry and epoch");
        if(stopToken!=null){require(!game.items().specialDrops().spawn(stopToken),"old epoch cannot make a delayed corrosion");stopToken.discard();}
        stopToken=null;stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS corrosion-drops-cleanup");
    }
}
