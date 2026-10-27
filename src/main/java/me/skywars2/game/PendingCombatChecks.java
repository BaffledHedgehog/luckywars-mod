package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native accepted hits and scheduled expiry; no manual entity, effect or world ticking. */
public final class PendingCombatChecks {
    private PendingCombatChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Pending combat: "+why);}
    private static final List<Holder<Attribute>> TYPES=List.of(Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.GRAVITY,Attributes.STEP_HEIGHT,
        Attributes.SAFE_FALL_DISTANCE,Attributes.JUMP_STRENGTH,Attributes.MOVEMENT_SPEED,Attributes.KNOCKBACK_RESISTANCE);
    private static final Identifier CURSE=Identifier.parse("minecraft:gravity_curse");
    private static Trial running;
    private static final class Trial implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final LbcPendingCombat service;
        final List<Cow> actors=new ArrayList<>();final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        Cow cursed;int started;boolean sawNine;LbcIndirectKills.TerrainProvenance terrainBefore;
        static Object field(Object target,String name){try{var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        @SuppressWarnings("unchecked") Map<Object,Object> provenance(String name){
            var result=new HashMap<Object,Object>();((Map<Object,Object>)field(game.items().indirectKills(),name)).forEach((key,value)->result.put(key,value instanceof Map<?,?> nested?new HashMap<>(nested):value));return result;
        }
        Trial(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);service=game.items().pendingCombat();
            require(service.snapshot().isEmpty(),"exclusive empty Chaos service");base.initialize();base.owner.removeAllEffects();
            base.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);base.owner.setHealth(100);
            base.owner.entityTags().remove("sin_greed_active");base.owner.entityTags().remove("nomagic_active");
        }
        ItemStack leaf(String name){var result=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/"+name);
            require(result.size()==1,"native imported singleton "+name);return result.getFirst();}
        Cow cow(Vec3 at){var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(at);cow.setNoGravity(true);cow.setNoAi(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);require(base.owner.level().addFreshEntity(cow),"native resident target");actors.add(cow);return cow;}
        void hit(Cow cow,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type){cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            require(cow.hurtServer(base.owner.level(),new DamageSource(base.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),base.owner),1),"actual accepted native damage");}
        void identity(){var piston=leaf("piston_sword");var gravity=leaf("gravity_sword");var chaos=leaf("chaos_sword");
            require(piston.is(Items.CARROT_ON_A_STICK)&&LbcPendingCombat.isPiston(piston)&&Identifier.parse("lbc:piston_sword").equals(piston.get(DataComponents.ITEM_MODEL)),"Piston native source identity/model");
            require(gravity.is(Items.IRON_SWORD)&&LbcFartus.data(gravity).getIntOr("gravitationsword",0)==1&&Identifier.parse("lbc:gravitysword").equals(gravity.get(DataComponents.ITEM_MODEL)),"Gravity vanilla sword/model/flag");
            require(chaos.is(Items.GOLDEN_SWORD)&&LbcFartus.data(chaos).getIntOr("chaos_swo",0)==1&&chaos.has(DataComponents.UNBREAKABLE)&&Objects.equals(chaos.get(DataComponents.ITEM_MODEL),Items.GOLDEN_SWORD.getDefaultInstance().get(DataComponents.ITEM_MODEL)),"Chaos original vanilla golden sword/unbreakable/model omission");
        }
        void piston(){var level=base.owner.level();var first=BlockPos.containing(base.owner.getEyePosition()).offset(0,0,2);
            // This exceeds vanilla's twelve-block piston limit and keeps every changed native cell for restoration.
            for(int i=0;i<=25;i++){var pos=first.offset(0,0,i);require(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null&&level.getBlockState(pos).isAir(),"resident clear chain fixture");terrain.put(pos,game.maps().snapshot(level,pos));}
            for(int i=0;i<25;i++)level.setBlockAndUpdate(first.offset(0,0,i),(i%2==0?Blocks.STONE:Blocks.DIRT).defaultBlockState());
            level.setBlockAndUpdate(first.offset(0,0,12),Blocks.CHEST.defaultBlockState());
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(first.offset(0,0,12));
            chest.setItem(7,new ItemStack(Items.DIAMOND,13));chest.setChanged();
            var snapshots=new ArrayList<MapManager.BlockSnapshot>();for(var pos:terrain.keySet())snapshots.add(game.maps().snapshot(level,pos));
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null,"independent original terrain author");
            var indirect=game.items().indirectKills();var previous=indirect.captureTerrain(level,terrain.keySet());terrainBefore=previous;
            level.setBlockAndUpdate(first.offset(0,0,24),Blocks.MAGMA_BLOCK.defaultBlockState());snapshots.set(24,game.maps().snapshot(level,first.offset(0,0,24)));
            indirect.stonePlaced(level,first.offset(0,0,23),other);indirect.hazardPlaced(level,first.offset(0,0,24),other);
            var standing=cow(Vec3.atCenterOf(first.offset(0,0,24)).add(0,.5,0));standing.setOnGround(true);
            indirect.displaced(standing,other);
            indirect.supportRemoved(level,indirect.coldCause(other,false),first.offset(0,0,24));
            Map<Object,Object> stoneBefore=provenance("stone"),hazardsBefore=provenance("hazards"),supportBefore=provenance("removedSupports"),fallsBefore=provenance("falls");
            require(supportBefore.containsKey(standing),"real native support shape seeds previous independent support cause");
            var tickSource=first.offset(0,0,20);level.scheduleTick(tickSource,Blocks.STONE,600);
            int[] writes={0};require(!service.shift(base.owner,first,new Vec3(0,0,1),(pos,snapshot)->{
                if(++writes[0]==4)throw new IllegalStateException("fixture rejected fourth native write");game.maps().restoreSnapshot(level,pos,snapshot);
            }),"native transaction rejects injected late write failure");require(writes[0]==4,"failure occurs after three actual changed cells");
            int index=0;for(var pos:terrain.keySet())require(game.maps().snapshot(level,pos).equals(snapshots.get(index++)),"entire chain rolled back after actual partial writes");
            require(provenance("stone").equals(stoneBefore)&&provenance("hazards").equals(hazardsBefore)&&provenance("removedSupports").equals(supportBefore)&&provenance("falls").equals(fallsBefore),"late failure restores prior stone/hazard/support authors after trap finalization");
            require(level.getBlockTicks().hasScheduledTick(tickSource,Blocks.STONE)&&!level.getBlockTicks().hasScheduledTick(tickSource.offset(0,0,1),Blocks.STONE),"failed transaction adds no copied native block tick");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,leaf("piston_sword"));base.owner.setYRot(0);base.owner.setXRot(0);
            var border=level.getWorldBorder();double centerX=border.getCenterX(),centerZ=border.getCenterZ(),size=border.getSize();
            try{
                border.setCenter(base.owner.getX(),base.owner.getZ());border.setSize(12);
                long writesBefore=service.pistonWrites();
                require(service.use(base.owner,InteractionHand.MAIN_HAND),"ordinary input traces toward chain before border endpoint cancellation");
                require(service.pistonWrites()==writesBefore,"first complete boundary preflight cancels with zero native writer calls");
                int cell=0;for(var pos:terrain.keySet())require(game.maps().snapshot(level,pos).equals(snapshots.get(cell++)),"border cancellation preserves entire source chain");
            }finally{border.setCenter(centerX,centerZ);border.setSize(size);}
            require(service.use(base.owner,InteractionHand.MAIN_HAND),"ordinary Piston input admitted");
            require(level.getBlockState(first).is(Blocks.AIR)&&level.getBlockState(first.offset(0,0,25)).is(Blocks.MAGMA_BLOCK),"ordinary input synchronously shifts twenty-five blocks");
            var moved=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(first.offset(0,0,13));
            require(moved!=null&&moved.getItem(7).is(Items.DIAMOND)&&moved.getItem(7).getCount()==13,"native block entity payload travels with the chain");
            require(level.getBlockTicks().hasScheduledTick(tickSource.offset(0,0,1),Blocks.STONE),"successful native shift copies scheduled block tick");
            level.getBlockTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(terrain.keySet()).orElseThrow());
            indirect.clear(standing);standing.discard();indirect.restoreTerrain(previous);
            for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());terrain.clear();terrainBefore=null;
            diagonalTicks();diagonalFluidPlacement();
        }
        void diagonalTicks(){var level=base.owner.level();var first=BlockPos.containing(base.owner.position()).offset(3,4,3);
            var diagonal=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            for(int i=0;i<=5;i++){var pos=first.offset(i,0,i);require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir(),"resident sparse diagonal fixture");diagonal.put(pos,game.maps().snapshot(level,pos));}
            var outside=first.offset(0,0,1);var outsideCopy=outside.offset(1,0,1);
            require(level.getBlockState(outside).isAir()&&level.getBlockState(outsideCopy).isAir(),"outside diagonal tick witnesses clear");
            try{
                for(int i=0;i<5;i++)level.setBlockAndUpdate(first.offset(i,0,i),Blocks.STONE.defaultBlockState());
                level.scheduleTick(first,Blocks.STONE,600);level.scheduleTick(outside,Blocks.DIRT,600);
                require(service.shift(base.owner,first,new Vec3(1,0,1).normalize()),"diagonal digital shift succeeds");
                require(level.getBlockTicks().hasScheduledTick(first.offset(1,0,1),Blocks.STONE),"diagonal native tick copies by exact constant step");
                require(level.getBlockTicks().hasScheduledTick(outside,Blocks.DIRT)&&!level.getBlockTicks().hasScheduledTick(outsideCopy,Blocks.DIRT),"enclosing-box tick outside actual chain is never copied");
            }finally{
                var bounds=net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(diagonal.keySet()).orElseThrow();level.getBlockTicks().clearArea(bounds);
                for(var entry:diagonal.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
            }
        }
        @SuppressWarnings("unchecked") void diagonalFluidPlacement(){
            var level=base.owner.level();var first=BlockPos.containing(base.owner.position()).offset(-5,5,3);var destination=first.offset(1,0,1);
            var outside=first.offset(0,0,1);var outsideCopy=outside.offset(1,0,1);
            var saved=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            for(var pos:List.of(first,destination,outside,outsideCopy)){
                require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir(),"resident isolated diagonal fluid fixture");saved.put(pos,game.maps().snapshot(level,pos));
            }
            var bounds=net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(saved.keySet()).orElseThrow();
            var access=(LbcTimeStopSchedules.Access<net.minecraft.world.level.material.Fluid>)(Object)level.getFluidTicks();
            try{
                for(var fluid:List.of(net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.LAVA)){
                    var state=fluid.defaultFluidState().createLegacyBlock();
                    level.setBlock(first,state,net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
                        |net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE);
                    level.scheduleTick(first,fluid,600);level.scheduleTick(outside,fluid,600);
                    int[] writes={0};require(!service.shift(base.owner,first,new Vec3(1,0,1).normalize(),(pos,snapshot)->{
                        if(++writes[0]==2)throw new IllegalStateException("fixture rejects source clear after fluid destination write");
                        game.maps().restoreSnapshot(level,pos,snapshot,net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
                            |net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE);
                    }),"late rejected diagonal fluid transaction");
                    require(level.getBlockState(first)==state&&level.getBlockState(destination).isAir()&&!level.getFluidTicks().hasScheduledTick(destination,fluid),"rollback restores fluid with no destination placement callback");
                    long expected=level.getGameTime()+fluid.getTickDelay(level);
                    require(service.shift(base.owner,first,new Vec3(1,0,1).normalize()),"single diagonal fluid source shifts successfully");
                    require(level.getBlockState(destination)==state&&level.getFluidTicks().hasScheduledTick(destination,fluid),"committed diagonal fluid receives native placement scheduling");
                    var container=access.skywars2$containers().get(net.minecraft.world.level.ChunkPos.pack(destination));
                    var tick=container.getAll().filter(entry->entry.type()==fluid&&entry.pos().equals(destination)).findFirst().orElseThrow();
                    require(tick.triggerTick()==expected,"destination uses native fluid delay instead of copying source600 clock");
                    require(level.getFluidTicks().hasScheduledTick(outside,fluid)&&!level.getFluidTicks().hasScheduledTick(outsideCopy,fluid),"unrelated source fluid queue is never copied");
                    level.getFluidTicks().clearArea(bounds);level.getBlockTicks().clearArea(bounds);
                    for(var entry:saved.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
                }
            }finally{
                level.getFluidTicks().clearArea(bounds);level.getBlockTicks().clearArea(bounds);
                for(var entry:saved.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
            }
        }
        void chaos(){var victim=cow(base.owner.position().add(0,0,3));base.owner.setItemInHand(InteractionHand.MAIN_HAND,leaf("chaos_sword"));
            hit(victim,DamageTypes.ARROW);require(service.snapshot().isEmpty(),"arrow-only accepted hit does not activate Chaos");
            hit(victim,DamageTypes.PLAYER_ATTACK);var volley=service.snapshot();require(volley.size()==40,"one accepted native hit creates exactly forty source copies");
            var origin=base.owner.position();require(volley.stream().filter(copy->copy.display.position().equals(origin.add(11,5,-11))).count()==2,"original lastY5 duplicate remains");
            require(volley.stream().allMatch(copy->copy.owner==base.owner&&copy.display.getItemStack().is(Items.GOLDEN_SWORD)&&copy.display.entityTags().contains("magic")&&game.items().currentEpoch(copy.display)),"native actors preserve original item/magic, epoch and captured owner");
            service.forget(base.owner);require(service.snapshot().isEmpty()&&volley.stream().allMatch(copy->copy.display.isRemoved()),"owner lifecycle discards every native copy");victim.discard();
        }
        void gravity(){cursed=cow(base.owner.position().add(0,0,3));base.owner.setItemInHand(InteractionHand.MAIN_HAND,leaf("gravity_sword"));
            hit(cursed,DamageTypes.PLAYER_ATTACK);require(service.curseSeconds(cursed)==10,"actual accepted native hit starts ten-second curse");
            var old=new ArrayList<net.minecraft.world.entity.ai.attributes.AttributeModifier>();
            for(var type:TYPES){var modifier=cursed.getAttribute(type).getModifier(CURSE);require(modifier!=null&&modifier.operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,"source multiplied-total operation "+type);old.add(modifier);}
            double resistance=old.getLast().amount();require(resistance>=-3&&resistance<=0,"author knockback amount range");
            hit(cursed,DamageTypes.PLAYER_ATTACK);for(int i=0;i<TYPES.size();i++)require(cursed.getAttribute(TYPES.get(i)).getModifier(CURSE)!=old.get(i),"reapplication replaces modifier "+TYPES.get(i));
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);started=game.server().getTickCount();
        }
        boolean step(){int elapsed=game.server().getTickCount()-started;
            if(service.curseSeconds(cursed)==9)sawNine=true;
            if(elapsed<205)return false;
            require(sawNine&&service.curseSeconds(cursed)==0,"real scheduled second pulses and expiry");
            for(var type:TYPES)require(cursed.getAttribute(type).getModifier(CURSE)==null,"expiry removes all seven modifiers "+type);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,leaf("gravity_sword"));hit(cursed,DamageTypes.PLAYER_ATTACK);
            require(service.curseSeconds(cursed)==10,"curse reapplies after expiry");service.unloaded(cursed);
            for(var type:TYPES)require(cursed.getAttribute(type).getModifier(CURSE)==null,"unload cleanup removes all seven modifiers");return true;
        }
        @Override public void close(){service.forget(base.owner);for(var actor:actors){service.clearEffects(actor);game.items().indirectKills().clear(actor);actor.discard();}
            if(!terrain.isEmpty())base.owner.level().getBlockTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(terrain.keySet()).orElseThrow());
            for(var entry:terrain.entrySet())game.maps().restoreSnapshot(base.owner.level(),entry.getKey(),entry.getValue());
            if(terrainBefore!=null)game.items().indirectKills().restoreTerrain(terrainBefore);base.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive scheduled trial");var trial=new Trial(game);
        try{trial.identity();trial.piston();trial.chaos();trial.gravity();running=trial;SkyWars2.LOGGER.info("[check] pending-combat running (native expiry scheduled)");}
        catch(Throwable problem){try{trial.close();}catch(Throwable cleanup){problem.addSuppressed(cleanup);}throw problem;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS pending-combat (native leaves, actual partial-write rollback and25-block input, accepted hits/forty Chaos/lifecycle, seven-attribute replacement and scheduled expiry)");}}
        catch(Throwable problem){try{running.close();}catch(Throwable cleanup){problem.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL pending-combat",problem);}}
}
