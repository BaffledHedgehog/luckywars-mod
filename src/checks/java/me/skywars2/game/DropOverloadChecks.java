package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.validation.mixin.NativeFixtureAccess;
import me.skywars2.game.entities.mobs.LbcSauvojenBoss;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.drops.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Real20-full-chest TNT publication, synthetic MSPT admission probes, at most3 actual baby zombies. */
public final class DropOverloadChecks {
    private DropOverloadChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Drop overload: "+why);}
    private static void average(MinecraftServer server,long nanos){
        ((NativeFixtureAccess.Server)server).skywars2$validationAggregatedTickTime(nanos*Math.min(100,Math.max(1,server.getTickCount())));
        require(server.getAverageTickTimeNanos()==nanos,"controlled native average tick-time input");
    }
    private static List<ItemStack> profile(GameController game,WoolBucketChecks.Fixture f){
        var result=new ArrayList<ItemStack>();
        result.add(new ItemStack(LbcSlabDropItem.Kind.NORMAL.item()));
        result.add(new ItemStack(LbcCreatureDropItem.Kind.VEX.item()));
        result.add(new ItemStack(LbcPotionSpawnerDropItem.Kind.NORMAL.item()));
        ItemStack boss=ItemStack.EMPTY,forest=ItemStack.EMPTY;
        for(int attempt=0;attempt<512&&(boss.isEmpty()||forest.isEmpty());attempt++)
            for(var stack:game.lbc().rollTable(f.level,f.owner.position(),f.owner,"lbc:traps")){
                if(LbcSauvojenBoss.recognized(stack))boss=stack.copyWithCount(1);
                if(ItemData.intValue(stack,"forest",0)==1)forest=stack.copyWithCount(1);
            }
        require(!boss.isEmpty()&&!forest.isEmpty(),"actual authored mini-boss and zombie-forest trap profiles");result.add(boss);result.add(forest);
        for(var stack:result)require(stack.getMaxStackSize()>=64&&LbcSpecialDrops.nativeRecognized(stack),"actual stackable native trap profile");
        return result;
    }
    private static List<ItemEntity> tokens(WoolBucketChecks.Fixture f,Set<UUID> before){
        var result=new ArrayList<ItemEntity>();for(var entity:f.level.getAllEntities())
            if(entity instanceof ItemEntity item&&!before.contains(item.getUUID())&&!item.isRemoved()&&LbcSpecialDrops.nativeRecognized(item.getItem()))result.add(item);
        return result;
    }
    private static long units(List<ItemEntity> tokens){return tokens.stream().filter(item->!item.isRemoved()).mapToLong(item->item.getItem().getCount()).sum();}
    private static long actors(WoolBucketChecks.Fixture f,Set<UUID> before){
        long count=0;for(var entity:f.level.getAllEntities())if(!(entity instanceof ItemEntity)&&!entity.isRemoved()&&!before.contains(entity.getUUID()))count++;return count;
    }
    private static void pressureBoundary(GameController game,ItemEntity probe){
        int originalTick=game.server().getTickCount();
        var clock=(NativeFixtureAccess.Server)game.server();
        try{
            clock.skywars2$validationTickCount(originalTick+(20-Math.floorMod(originalTick,20))%20);average(game.server(),150_000_000L);
            require(!game.items().specialDrops().admit(probe),"overload has no twentieth-tick admission leak");
        }finally{clock.skywars2$validationTickCount(originalTick);average(game.server(),150_000_000L);}
    }
    public static void run(GameController game){
        require(game.items().specialDrops().pendingCount()==0&&game.items().sauvojenBoss().count()==0
                &&game.items().slabDrops().snapshot().isEmpty()&&game.items().chaosTrapActors().snapshot().isEmpty()
                &&game.items().potionSpawnerDrops().snapshot().isEmpty(),"exclusive temporary overload families");
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var server=game.server();var initial=new HashSet<UUID>();for(var entity:f.level.getAllEntities())initial.add(entity.getUUID());
            var chests=new ArrayList<BlockPos>();boolean drops=f.level.getGameRules().get(GameRules.BLOCK_DROPS),decay=f.level.getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY);
            var clock=(NativeFixtureAccess.Server)server;
            long oldAggregate=clock.skywars2$validationAggregatedTickTime();int originalTick=server.getTickCount();
            try{
                var center=new BlockPos((f.owner.blockPosition().getX()&~15)+8,Math.min(f.level.getMaxY()-24,260),(f.owner.blockPosition().getZ()&~15)+8);
                // Validate resident empty sections before placing anything: the real blast
                // cannot edit a borrowed island, request a cold chunk, or destroy other actors.
                for(int x=(center.getX()-18)>>4;x<=(center.getX()+18)>>4;x++)for(int z=(center.getZ()-18)>>4;z<=(center.getZ()+18)>>4;z++){
                    var chunk=f.level.getChunkSource().getChunkNow(x,z);require(chunk!=null,"resident native TNT footprint");
                    for(int y=(center.getY()-18)>>4;y<=(center.getY()+18)>>4;y++)
                        require(chunk.getSection(f.level.getSectionIndex(y<<4)).hasOnlyAir(),"empty native test sections isolate the whole blast");
                }
                f.move(f.owner,Vec3.atCenterOf(center).add(0,0,-40));f.move(f.other,Vec3.atCenterOf(center).add(0,0,-44));
                var pool=profile(game,f);var random=net.minecraft.util.RandomSource.create(25112026);int[] kinds=new int[pool.size()];
                for(int x=-2;x<=2;x++)for(int z=-2;z<=1;z++){
                    var pos=center.offset(x,0,z);f.set(pos,Blocks.CHEST.defaultBlockState());chests.add(pos);
                    require(f.level.getBlockEntity(pos) instanceof ChestBlockEntity,"actual native chest block entity");var chest=(ChestBlockEntity)f.level.getBlockEntity(pos);
                    require(chest.getContainerSize()==27,"one full single chest");
                    for(int slot=0;slot<27;slot++){int kind=random.nextInt(pool.size());kinds[kind]++;chest.setItem(slot,pool.get(kind).copyWithCount(64));}
                }
                require(chests.size()==20&&Arrays.stream(kinds).allMatch(n->n>0),"20 full27-slot chests include all five authored trap families");
                f.level.getGameRules().set(GameRules.BLOCK_DROPS,true,server);f.level.getGameRules().set(GameRules.TNT_EXPLOSION_DROP_DECAY,false,server);
                average(server,150_000_000L);
                var at=Vec3.atCenterOf(center).add(0,1.5,0);var tnt=new PrimedTnt(f.level,at.x,at.y,at.z,f.owner);
                var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,f.owner.registryAccess());tnt.saveWithoutId(out);var data=out.buildResult();data.putFloat("explosion_power",8);
                tnt.load(TagValueInput.create(ProblemReporter.DISCARDING,f.owner.registryAccess(),data));tnt.setFuse(0);tnt.setNoGravity(true);tnt.setDeltaMovement(Vec3.ZERO);
                require(f.level.addFreshEntity(tnt),"real owned native TNT body");long started=System.nanoTime();tnt.tick();long explosionNanos=System.nanoTime()-started;
                require(tnt.isRemoved()&&chests.stream().allMatch(pos->f.level.getBlockEntity(pos)==null),"native TNT removes all20 actual full chests");
                var burst=tokens(f,initial);long total=units(burst);int initialQueue=game.items().specialDrops().pendingCount();
                require(total==20L*27*64&&initialQueue>64&&initialQueue<=burst.size(),"all34560 native inventory units publish into a real pending overload without activation loss");
                require(actors(f,initial)==0,"chest publication cannot eagerly create trap actors under pressure");
                pressureBoundary(game,burst.getFirst());
                for(int n=0;n<Math.min(128,burst.size());n++)require(!game.items().specialDrops().admit(burst.get(n)),"high-MSPT full burst remains deferred");

                // Cancel most pending tokens as fixture cleanup, never activate them.
                // Retain63 real native split bodies to expose the former64->63 gap.
                var keep=new ArrayList<ItemEntity>();for(var token:burst)if(LbcSlabDropItem.matches(token.getItem(),LbcSlabDropItem.Kind.NORMAL)&&keep.size()<3)keep.add(token);
                require(keep.size()==3,"three independent native slab bodies for bounded recovery");
                for(var token:burst)if(keep.size()<63&&!keep.contains(token))keep.add(token);
                var kept=Collections.newSetFromMap(new IdentityHashMap<ItemEntity,Boolean>());kept.addAll(keep);
                for(var token:burst)if(!kept.contains(token)){game.items().entityUnloaded(token);token.discard();}
                require(game.items().specialDrops().pendingCount()==63,"actual pending tail below64");
                for(var token:keep)require(!game.items().specialDrops().admit(token)&&game.items().specialDrops().blocksPickup(token),"small tail retains pressure guard and native pickup lock");
                pressureBoundary(game,keep.getLast());

                average(server,0);int resumed=0;long remainingBefore=units(keep);started=System.nanoTime();
                for(int n=0;n<3;n++)if(game.items().specialDrops().spawn(keep.get(n)))resumed++;
                long recoveryNanos=System.nanoTime()-started;
                require(resumed>0&&resumed<=3&&game.items().slabDrops().snapshot().size()==resumed&&actors(f,initial)==resumed
                        &&units(keep)==remainingBefore-resumed,"healthy server resumes only the explicitly bounded real actor subset and exact debits");
                for(var actor:game.items().slabDrops().snapshot()){actor.setNoAi(true);actor.setNoGravity(true);}
                average(server,150_000_000L);for(var token:keep)require(!game.items().specialDrops().admit(token),"renewed pressure halts growth despite an existing small actor population");
                require(actors(f,initial)==resumed,"pressure probes do not accumulate additional actors");

                // Probe the healthy cap on one temporary logical frame without executing
                // callbacks or spawning actors. Restore both native inputs immediately.
                try{
                    clock.skywars2$validationTickCount(originalTick+1);average(server,0);int permits=0;
                    for(int n=0;n<32;n++)if(game.items().specialDrops().admit(keep.getLast()))permits++;
                    require(permits==16,"healthy tail below64 still shares the existing16-admission frame cap");
                }finally{clock.skywars2$validationTickCount(originalTick);average(server,150_000_000L);}
                SkyWars2.LOGGER.info("[check] drop-overload realChests=20 slots=540 inventoryUnits={} nativeSplitBodies={} pendingPeak={} nativeExplosionMs={} syntheticMspt=150 tail=63 recoveryActivations={} recoveryMs={} survivingActors={} nativeFrames=1; remaining queue cancelled by fixture cleanup",
                        total,burst.size(),initialQueue,explosionNanos/1e6,resumed,recoveryNanos/1e6,actors(f,initial));
            }finally{
                clock.skywars2$validationTickCount(originalTick);clock.skywars2$validationAggregatedTickTime(oldAggregate);
                // No delayed activator or native actor is allowed to survive this finite test.
                var fresh=new ArrayList<Entity>();for(var entity:f.level.getAllEntities())if(!initial.contains(entity.getUUID())&&!(entity instanceof net.minecraft.server.level.ServerPlayer))fresh.add(entity);
                for(var entity:fresh){game.items().entityUnloaded(entity);entity.discard();}game.items().specialDrops().stop();
                for(var pos:chests)if(f.level.getBlockEntity(pos) instanceof ChestBlockEntity chest)chest.clearContent();
                f.level.getGameRules().set(GameRules.BLOCK_DROPS,drops,server);f.level.getGameRules().set(GameRules.TNT_EXPLOSION_DROP_DECAY,decay,server);
                require(server.getTickCount()==originalTick&&clock.skywars2$validationAggregatedTickTime()==oldAggregate,"native tick counter and aggregated tick time restored exactly");
            }
        }
        SkyWars2.LOGGER.info("[check] PASS drop-overload (actual20 full chests/native TNT, exact inventory conservation, synthetic pressure at large/tail/20th tick, bounded healthy recovery, no uncapped actor run)");
    }
}
