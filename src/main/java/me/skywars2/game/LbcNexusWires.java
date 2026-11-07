package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Nexus XP production and the red wire's existing resource owners, without a second charge ledger. */
public final class LbcNexusWires {
    private final GameController game;
    private static final TicketType GATEWAY_TICKET=new TicketType(TicketType.NO_TIMEOUT,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:nexus_wire"),GATEWAY_TICKET);}
    static final class Gateway {
        final ServerLevel level;final LbcIndirectKills.Owner cause;BlockPos position;
        Gateway(ServerLevel level,BlockPos position,LbcIndirectKills.Owner cause){this.level=level;this.position=position;this.cause=cause;}
    }
    private record Chunk(ServerLevel level,ChunkPos pos){}
    private final List<Gateway> gateways=new ArrayList<>();
    private final Map<Chunk,CompletableFuture<?>> loads=new HashMap<>();
    private final Set<UUID> dirty=new HashSet<>(),carriers=new HashSet<>();
    private final Set<ExperienceOrb> pending=new LinkedHashSet<>();
    private boolean frozen;
    private int lastTick=Integer.MIN_VALUE,heightPhase;
    LbcNexusWires(GameController game){this.game=game;}
    List<Gateway> pendingGateways(){return List.copyOf(gateways);}
    boolean captureReady(){return gateways.isEmpty();}
    int loadingChunks(){return loads.size();}
    public void inventoryChanged(ServerPlayer player){dirty.add(player.getUUID());}
    boolean nexusExperience(ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&level.dimension().equals(LbcHedgehogDimensions.NEXUS);}
    private boolean paused(Entity entity){return frozen||entity.level() instanceof ServerLevel level&&game.items().timeStop().stopped(level)||entity.entityTags().contains("time_stopped");}
    /** Called before XP spawn merging: one actual source orb yields one wire, regardless of its value. */
    boolean convert(ExperienceOrb orb){
        if(orb.isRemoved()||!(orb.level() instanceof ServerLevel level)||!nexusExperience(level)||paused(orb)||MatchActorCheckpoint.restoring(orb))return false;
        var drop=new ItemEntity(level,orb.getX(),orb.getY(),orb.getZ(),new ItemStack(LbcNexusWireItems.RED));
        drop.setDefaultPickUpDelay();drop.setNoGravity(true);game.items().markCurrentItemEpoch(drop);
        if(!level.addFreshEntity(drop))return false;
        orb.discard();pending.remove(orb);return true;
    }
    void loaded(Entity entity){if(entity instanceof ExperienceOrb orb&&orb.level() instanceof ServerLevel level&&nexusExperience(level)&&!convert(orb))pending.add(orb);}
    void unloaded(Entity entity){if(entity instanceof ExperienceOrb orb)pending.remove(orb);}
    private boolean admitted(ServerPlayer p){return p!=null&&!p.isRemoved()&&p.isAlive()&&game.canUseGameplayItems(p)&&!game.isObserver(p)
            &&LbcDimensionItems.ordinary(p)&&game.items().wands().validRoleTarget(p,false)&&!game.items().wands().inputTimeStopped(p)&&!paused(p);}
    private void consume(ServerPlayer p,boolean black){
        if(!admitted(p))return;
        java.util.function.Predicate<ItemStack> matches=stack->!LbcTmtItems.matches(stack)&&(black?LbcNexusWireItems.isBlack(stack):LbcNexusWireItems.isRed(stack));
        if(game.items().inventoryHazards().protectedInventory(p)&&!matches.test(p.getMainHandItem()))return;
        if(p.getInventory().clearOrCountMatchingItems(matches,false,1,p.inventoryMenu.getCraftSlots())==0)return;
        grant(p,black);
        p.getInventory().setChanged();game.items().mana().inventoryChanged(p);
    }
    private int consumeTmt(ServerPlayer p){
        if(!admitted(p))return 0;
        boolean protectedInventory=game.items().inventoryHazards().protectedInventory(p);
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++){
            var stack=p.getInventory().getItem(slot);
            if(!LbcTmtItems.matches(stack)||protectedInventory&&stack!=p.getMainHandItem())continue;
            int mask=LbcTmtIntake.wires(stack);if(mask==0)continue;
            stack.shrink(1);
            if((mask&2)!=0)grant(p,true);
            if((mask&1)!=0)grant(p,false);
            p.getInventory().setChanged();game.items().mana().inventoryChanged(p);return mask;
        }
        return 0;
    }
    private void grant(ServerPlayer p,boolean black){
        if(black){var r=p.getRandom();gateways.add(new Gateway(p.level(),new BlockPos(r.nextInt(4001)-2000,r.nextInt(320),r.nextInt(4001)-2000),game.items().indirectKills().wildCause(p)));}
        else grantRed(p);
        p.sendSystemMessage(Component.translatable(black?"black_wire_destroyed":"red_wire_destroyed").withColor(black?0xaaaaaa:0xff5555));
    }
    /** Own temporary tickets keep remote gateway loads out of the match-long map-loading cache. */
    private void tickGateways(){
        for(var it=gateways.iterator();it.hasNext();){var gateway=it.next();var level=gateway.level;
            if(game.items().timeStop().stopped(level))continue;
            if(level.isOutsideBuildHeight(gateway.position)){it.remove();continue;}
            var chunk=new Chunk(level,new ChunkPos(gateway.position.getX()>>4,gateway.position.getZ()>>4));
            var future=loads.computeIfAbsent(chunk,c->level.getChunkSource().addTicketAndLoadWithRadius(GATEWAY_TICKET,c.pos(),0));
            if(!future.isDone()||level.getChunkSource().getChunkNow(chunk.pos().x(),chunk.pos().z())==null)continue;
            game.items().indirectKills().capturedWorldChanges(level,gateway.cause,()->{
                LbcDropBlocks.setBlock(level,gateway.position,Blocks.END_GATEWAY.defaultBlockState(),false);
                if(level.getBlockEntity(gateway.position) instanceof TheEndGatewayBlockEntity entity)entity.setExitPosition(new BlockPos(1,15000000,1),true);
            });it.remove();
        }
        var needed=new HashSet<Chunk>();for(var g:gateways)needed.add(new Chunk(g.level,new ChunkPos(g.position.getX()>>4,g.position.getZ()>>4)));
        for(var it=loads.entrySet().iterator();it.hasNext();){var key=it.next().getKey();
            if(!needed.contains(key)){key.level().getChunkSource().removeTicketWithRadius(GATEWAY_TICKET,key.pos(),0);it.remove();}
        }
    }
    void grantRed(ServerPlayer p){
        p.giveExperiencePoints(20);game.items().mana().add(p,5000);
        new LbcSourceCounters(game).addSwordCharges(p,5);
        game.items().mana().score(p,"nanobots_tier").add(100);
    }
    public void tick(){
        int tick=game.server().getTickCount();if(tick==lastTick||frozen)return;lastTick=tick;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.server().tickRateManager().runsNormally()||game.items().wands().legacyTimeStopped())return;
        tickGateways();
        for(var orb:List.copyOf(pending)){
            if(orb.isRemoved()||!(orb.level() instanceof ServerLevel level)||!nexusExperience(level)){pending.remove(orb);continue;}
            convert(orb);
        }
        for(var id:Set.copyOf(dirty)){
            dirty.remove(id);var p=game.server().getPlayerList().getPlayer(id);boolean found=false;
            if(p!=null)for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(LbcTmtItems.matches(stack)?LbcTmtIntake.wires(stack)!=0:LbcNexusWireItems.isRed(stack)||LbcNexusWireItems.isBlack(stack)){found=true;break;}}
            if(found)carriers.add(id);else carriers.remove(id);
        }
        for(var id:Set.copyOf(carriers)){var p=game.server().getPlayerList().getPlayer(id);int applied=consumeTmt(p);if((applied&2)==0)consume(p,true);if((applied&1)==0)consume(p,false);}
        if(++heightPhase%20==0)for(var p:game.server().getPlayerList().getPlayers())
            if(p.isAlive()&&!p.isSpectator()&&!game.isObserver(p)&&LbcDimensionItems.ordinary(p)&&p.getY()>=14500000&&!paused(p))game.items().hedgehogDimensions().heightNexus(p);
    }
    void stop(){pending.clear();dirty.clear();carriers.clear();gateways.clear();for(var key:loads.keySet())key.level().getChunkSource().removeTicketWithRadius(GATEWAY_TICKET,key.pos(),0);loads.clear();frozen=false;heightPhase=0;}
    void prepareFrozen(){stop();frozen=true;}
    void publishFrozen(){frozen=false;lastTick=game.server().getTickCount();for(var p:game.server().getPlayerList().getPlayers())inventoryChanged(p);}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final int phase=heightPhase;
        final Set<UUID> waiting=pending.stream().filter(e->!e.isRemoved()).map(Entity::getUUID).collect(java.util.stream.Collectors.toUnmodifiableSet());
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var missing=new LinkedHashSet<UUID>();var orbs=new LinkedHashSet<ExperienceOrb>();
            for(var id:waiting)if(lookup.apply(id) instanceof ExperienceOrb orb)orbs.add(orb);else missing.add(id);
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{pending.clear();pending.addAll(orbs);heightPhase=phase;});
        }
    }
}
