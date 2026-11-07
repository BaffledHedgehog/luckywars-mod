package me.skywars2.game;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.*;

/** Real native input and outer server-loop simulation, never a manual world/entity tick. */
public final class ClockFragmentChecks {
    private static Scene running;
    private ClockFragmentChecks(){}
    static {ServerTickEvents.START_SERVER_TICK.register(server->{var scene=running;if(scene==null||scene.input==null)return;
        var action=scene.input;scene.input=null;try{action.run();}catch(Throwable error){scene.failure=error;}});}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Clock fragments: "+why);}
    public static void run(GameController game){require(running==null,"exclusive native scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS clock-fragments (16 complete native profiles/both hands; real native carrot/snowball/bow/crossbow TMT events; apple native consumption negative; exact whole-server/world/match durations including1800, actual physics/effects/block/fluid/packets; immutable accepted queue, MAG/TECH/offhand/private-source, TimeStop, final0 freeze, foreign sprint and real coordinator rewind; resident scene only, no peak-TPS/client claim)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL clock-fragments",error);}}
    public static void cancel(GameController game){if(running==null)return;var scene=running;running=null;scene.close();}
    // Independent expected author durations;30s deliberately preserves actual tick90s mapping.
    private static final Map<String,Integer> EXPECTED=Map.ofEntries(Map.entry("1t",1),Map.entry("2t",2),Map.entry("5t",5),Map.entry("10t",10),
            Map.entry("1s",20),Map.entry("2s",40),Map.entry("3s",60),Map.entry("4s",80),Map.entry("5s",100),Map.entry("10s",200),
            Map.entry("15s",300),Map.entry("30s",1800),Map.entry("jump_1s",20),Map.entry("jump_2s",40),Map.entry("jump_5s",100),Map.entry("jump_10s",200));
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcClockFragments owner;
        final List<ItemStack> profiles=new ArrayList<>();final Set<UUID> original=new HashSet<>();
        final List<ScheduledTick<Block>> savedBlock=new ArrayList<>();
        final List<ScheduledTick<net.minecraft.world.level.material.Fluid>> savedFluid=new ArrayList<>();
        final BoundingBox area;final BlockPos lamp,water,destination;final NativeMeleeChecks.NativeStats stats;
        Runnable input;Throwable failure;LbcTmtAbilities.Ability privateClock;ItemStack physical;CustomData privateData;
        Cow witness;ItemEntity falling;Arrow arrow;Vec3 fallingStart,arrowStart;
        int phase,index,expected,useTick,startTick,matchStart,effectStart,clientStart,audienceStart,packetStart,when,queueStage;
        long worldStart;boolean physics,closed,rewinding;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);owner=game.items().clockFragments();
            var center=new BlockPos((f.owner.blockPosition().getX()>>4)*16+8,(int)f.home.y+4,(f.owner.blockPosition().getZ()>>4)*16+8);lamp=center.offset(3,0,0);water=center.offset(0,2,0);destination=water.below();
            area=new BoundingBox(center.getX()-1,center.getY()-1,center.getZ()-1,center.getX()+3,center.getY()+4,center.getZ()+1);
            var items=new ArrayList<Item>();items.add(LbcClockFragments.ITEM);for(int n=0;n<5;n++)items.add(LbcTmtItems.prototype(n).getItem());stats=new NativeMeleeChecks.NativeStats(f.owner,items);
            f.level.getAllEntities().forEach(entity->original.add(entity.getUUID()));}
        @SuppressWarnings("unchecked") int packets(){return ((List<?>)WoolBucketChecks.read(f.client.connection,"packets")).size();}
        void begin(){
            require(!game.server().tickRateManager().isSprinting(),"no preexisting sprint ownership");f.prepareParticipants();
            game.config().stageOneSeconds=1000000;game.config().stageTwoSeconds=1000001;
            try(var stream=Objects.requireNonNull(getClass().getResourceAsStream("/data/lbc/loot_table/clocks.json"))){
                var table=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                for(var entry:table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")){
                    var stack=f.loot(entry.getAsJsonObject().get("value").getAsString());String type=ItemData.stringValue(stack,"type","");
                    require(stack.is(LbcClockFragments.ITEM)&&EXPECTED.containsKey(type)&&LbcClockFragments.duration(stack)==EXPECTED.get(type),"actual complete authored profile "+type);
                    require(stack.has(DataComponents.CUSTOM_NAME)&&stack.has(DataComponents.LORE)&&stack.has(DataComponents.ITEM_MODEL)
                            &&ItemData.intValue(stack,"transformable",0)==1&&ItemData.intValue(stack,"accessory",0)==1&&ItemData.intValue(stack,"magicwand",0)==1,"authored profile payload intact "+type);
                    profiles.add(stack);
                }}catch(Exception error){throw new IllegalStateException("Invalid authored clock chooser",error);}
            require(profiles.size()==16&&profiles.stream().map(s->ItemData.stringValue(s,"type","")).distinct().count()==16,"all16 distinct selectors from real source chooser");
            privateClock=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                    .filter(property->{var source=LbcTmtComponents.source(game,property);return source.is(LbcClockFragments.ITEM)&&ItemData.stringValue(source,"type","").equals("1t");}).findFirst().orElseThrow();
            require(LbcClockFragments.duration(new ItemStack(LbcClockFragments.ITEM))==0,"bare native default has no executable selector");
            // Preserve only this small resident lab's original native scheduled work.
            var chunk=f.level.getChunk(lamp);((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->area.isInside(t.pos())).forEach(savedBlock::add);
            ((LevelChunkTicks<net.minecraft.world.level.material.Fluid>)chunk.getFluidTicks()).getAll().filter(t->area.isInside(t.pos())).forEach(savedFluid::add);
            for(var pos:BlockPos.betweenClosed(new BlockPos(area.minX(),area.minY(),area.minZ()),new BlockPos(area.maxX(),area.maxY(),area.maxZ())))f.set(pos,Blocks.STONE.defaultBlockState());
            f.level.getBlockTicks().clearArea(area);f.level.getFluidTicks().clearArea(area);
            f.set(water,Blocks.AIR.defaultBlockState());f.set(destination,Blocks.AIR.defaultBlockState());
            guards();queueCase();
        }
        ItemStack profile(String type){return profiles.stream().filter(s->ItemData.stringValue(s,"type","").equals(type)).findFirst().orElseThrow().copy();}
        ItemStack combined(int carrier){var stack=LbcTmtItems.prototype(carrier);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            LbcTmtAbilities.write(data,List.of(privateClock));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));if(carrier==2||carrier==4)stack.setCount(2);return stack;}
        void held(ItemStack stack,InteractionHand hand){f.owner.stopUsingItem();f.owner.getInventory().clearContent();f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);f.owner.setItemInHand(hand,stack);}
        void guards(){var stack=profile("1t");held(stack,InteractionHand.OFF_HAND);game.config().offhandWands=false;
            require(!owner.use(f.owner,InteractionHand.OFF_HAND),"ordinary offhand config admission");game.config().offhandWands=true;
            var mag=f.field(false,f.owner.position());require(!owner.use(f.owner,InteractionHand.OFF_HAND),"actual MAG suppresses fragment intake");f.removeFields();
            f.field(true,f.owner.position());require(owner.use(f.owner,InteractionHand.OFF_HAND),"TECH does not acquire invented suppression");owner.stop();f.removeFields();
            var forged=LbcTmtItems.prototype(0);var data=forged.get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("clocks",1);data.putInt("magicwand",1);data.putString("type","1t");forged.set(DataComponents.CUSTOM_DATA,CustomData.of(data));held(forged,InteractionHand.MAIN_HAND);
            require(!owner.use(f.owner,InteractionHand.MAIN_HAND)&&owner.pendingCount()==0,"forged outer TMT flags are not a selected private source");
            var invalid=profile("1t");data=invalid.get(DataComponents.CUSTOM_DATA).copyTag();data.putString("type","not-a-selector");invalid.set(DataComponents.CUSTOM_DATA,CustomData.of(data));held(invalid,InteractionHand.MAIN_HAND);
            require(!owner.use(f.owner,InteractionHand.MAIN_HAND),"invalid selector cannot request native simulation");}
        void queueCase(){
            if(index==40){phase=5;when=game.server().getTickCount();input=()->{physical=combined(4);held(physical,InteractionHand.MAIN_HAND);privateData=physical.get(DataComponents.CUSTOM_DATA);
                require(f.owner.gameMode.useItem(f.owner,f.level,physical,InteractionHand.MAIN_HAND).consumesAction(),"actual TMT apple begins ordinary consumable use");};return;}
            phase=0;input=()->{
                int carrier=index<32?-1:new int[]{0,1,2,3}[(index-32)/2];var hand=index%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
                physical=carrier<0?profiles.get(index/2).copy():combined(carrier);expected=carrier<0?EXPECTED.get(ItemData.stringValue(physical,"type","")):1;
                privateData=physical.get(DataComponents.CUSTOM_DATA);held(physical,hand);useTick=game.server().getTickCount();
                if(carrier==1)f.owner.getInventory().setItem(9,new ItemStack(Items.ARROW));
                if(carrier==3)physical.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
                require(f.owner.gameMode.useItem(f.owner,f.level,physical,hand).consumesAction(),"real native physical input index="+index+" hand="+hand);
                if(carrier==1){require(f.owner.isUsingItem(),"real native bow begins ammo draw");phase=3;}else phase=1;
            };
        }
        void witnesses(){
            witness=new Cow(EntityTypes.COW,f.level);witness.setPos(f.home.add(0,5,0));witness.setNoAi(true);witness.setNoGravity(true);witness.addEffect(ItemEffectLevels.create(MobEffects.GLOWING,4000,0),f.owner);
            require(f.level.addFreshEntity(witness),"native effect witness");f.own(witness);effectStart=witness.getEffect(MobEffects.GLOWING).getDuration();
            physics=expected==20;
            if(physics){var air=Vec3.atBottomCenterOf(water.offset(-4,14,-4));falling=new ItemEntity(f.level,air.x,air.y,air.z,new ItemStack(Items.STONE));falling.setPickUpDelay(200);falling.setDeltaMovement(.04,.05,0);require(f.level.addFreshEntity(falling),"native item gravity witness");f.own(falling);fallingStart=falling.position();
                arrow=new Arrow(EntityTypes.ARROW,f.level);arrow.setOwner(f.owner);arrow.setPos(air.add(1,0,0));arrow.setDeltaMovement(.08,.2,0);require(f.level.addFreshEntity(arrow),"native projectile witness");f.own(arrow);arrowStart=arrow.position();
                f.level.getBlockTicks().clearArea(area);f.level.getFluidTicks().clearArea(area);f.set(lamp,Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT,true));f.set(destination,Blocks.AIR.defaultBlockState());f.set(water,Blocks.WATER.defaultBlockState());
                f.level.getBlockTicks().clearArea(area);f.level.getFluidTicks().clearArea(area);f.level.scheduleTick(lamp,Blocks.REDSTONE_LAMP,3);f.level.scheduleTick(water,Fluids.WATER,2);}
            startTick=game.server().getTickCount();worldStart=f.level.getGameTime();matchStart=game.matchTicks();clientStart=f.client.updates();audienceStart=f.audience.updates();packetStart=packets();
        }
        void clearWitnesses(){f.discard(witness);witness=null;if(falling!=null){f.discard(falling);falling=null;}if(arrow!=null){f.discard(arrow);arrow=null;}}
        void measured(){int elapsed=game.server().getTickCount()-startTick;
            require(elapsed>0&&elapsed<=expected,"exact bounded native duration index="+index+" elapsed="+elapsed+" expected="+expected);
            require(owner.running()&&game.server().tickRateManager().isSprinting(),"owned through native final iteration");
            require(f.level.getGameTime()-worldStart==elapsed&&game.matchTicks()-matchStart==elapsed,"actual native world and match clocks agree");
            require(f.client.updates()-clientStart==elapsed&&f.audience.updates()-audienceStart==elapsed,"both real connection schedulers advance each native iteration");
            require(witness.getEffect(MobEffects.GLOWING).getDuration()==effectStart-elapsed,"native living effect clock advances with real simulation");
            if(elapsed<expected)return;
            require(packets()>packetStart,"actual native outbound packets during simulation");
            if(physics){require(!falling.isRemoved()&&falling.tickCount==20&&falling.getY()<fallingStart.y-1&&falling.getX()>fallingStart.x,"actual item gravity and motion");
                require(!arrow.isRemoved()&&arrow.tickCount==20&&arrow.getY()<arrowStart.y-1&&arrow.getDeltaMovement().y<0,"actual native projectile flight/gravity");
                require(!f.level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT)&&!f.level.getBlockTicks().hasScheduledTick(lamp,Blocks.REDSTONE_LAMP),"native scheduled block callback changes real state");
                require(f.level.getFluidState(destination).getType().isSame(Fluids.WATER),"native fluid callback creates actual downflow");}
            if(index>=32)require(physical.get(DataComponents.CUSTOM_DATA)==privateData,"selected private payload restored after real event");
            clearWitnesses();phase=4;when=game.server().getTickCount();
        }
        boolean step(){
            if(failure!=null)throw new IllegalStateException("native scheduled input",failure);f.client.acknowledge();f.audience.acknowledge();
            require(game.server().getTickCount()-f.started<20000,"bounded resident simulation scene phase="+phase+" index="+index);
            if(rewinding&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(phase==0)return false;
            if(phase==3){if(game.server().getTickCount()-useTick<4)return false;phase=0;input=()->{f.owner.releaseUsingItem();phase=1;};return false;}
            if(phase==1){require(owner.pendingCount()==1&&!owner.running(),"actual source event queued exactly one jump; no current-tick recursion index="+index);witnesses();phase=2;return false;}
            if(phase==2){measured();return false;}
            if(phase==4){if(owner.running()||game.server().tickRateManager().isSprinting()){require(game.server().getTickCount()-when<=2,"native final0 settles at next outer boundary");return false;}index++;queueCase();return false;}
            if(phase==5){if(input!=null||f.owner.isUsingItem())return false;require(physical.getCount()==1&&physical.get(DataComponents.CUSTOM_DATA)==privateData
                    &&f.owner.hasEffect(MobEffects.ABSORPTION)&&owner.pendingCount()==0&&!owner.running(),"actual32tick native apple consumes one physical item and preserves private clock without invented direct-use event");
                phase=6;input=()->{held(profile("5s"),InteractionHand.MAIN_HAND);require(owner.use(f.owner,InteractionHand.MAIN_HAND),"accepted TimeStop test source");};return false;}
            if(phase==6){if(input!=null)return false;require(owner.pendingCount()==1,"accepted request before later world TimeStop");expected=100;witnesses();game.items().timeStop().begin(f.level,20);f.stopped=true;phase=7;return false;}
            if(phase==7){int elapsed=game.server().getTickCount()-startTick;if(elapsed<100)return false;
                require(elapsed==100&&!game.items().timeStop().stopped(f.level)&&witness.getEffect(MobEffects.GLOWING).getDuration()==effectStart-80,"canonical TimeStop holds20 real native entity updates then naturally thaws inside100tick sprint");
                f.stopped=false;clearWitnesses();owner.stop();phase=8;when=game.server().getTickCount();return false;}
            if(phase==8){phase=9;input=()->{held(profile("1t"),InteractionHand.MAIN_HAND);require(owner.use(f.owner,InteractionHand.MAIN_HAND),"actual1tick final-state source");};return false;}
            if(phase==9){if(input!=null)return false;phase=10;return false;}
            if(phase==10){require(owner.running()&&game.server().tickRateManager().isSprinting(),"native1tick final remaining0 still owned");game.server().tickRateManager().setFrozen(true);
                require(!owner.running()&&!game.server().tickRateManager().isSprinting()&&game.server().tickRateManager().isFrozen(),"native final0 settled BEFORE external freeze ownership");
                require(!owner.use(f.owner,InteractionHand.MAIN_HAND),"global freeze rejects a new jump");game.server().tickRateManager().setFrozen(false);
                game.server().tickRateManager().requestGameToSprint(2);require(!owner.use(f.owner,InteractionHand.MAIN_HAND),"foreign native sprint is not overwritten");phase=11;when=game.server().getTickCount();return false;}
            if(phase==11){if(game.server().tickRateManager().isSprinting()){require(game.server().getTickCount()-when<=2,"foreign native sprint remains unmodified");return false;}
                require(!owner.running(),"foreign completion never becomes owned");held(profile("1t"),InteractionHand.MAIN_HAND);require(owner.use(f.owner,InteractionHand.MAIN_HAND),"first accepted immutable queue source");
                held(profile("2t"),InteractionHand.OFF_HAND);require(owner.use(f.owner,InteractionHand.OFF_HAND),"second independent accepted queue source");f.owner.getInventory().clearContent();
                require(owner.pendingCount()==2,"accepted source requests serialize rather than overwrite");phase=12;queueStage=0;when=game.server().getTickCount();return false;}
            if(phase==12){if(queueStage==0&&owner.running()){require(owner.requestedTicks()==1,"FIFO first native duration");queueStage=1;}
                if(queueStage==1&&owner.running()&&owner.requestedTicks()==2)queueStage=2;
                if(queueStage!=2||owner.running()||owner.pendingCount()!=0){require(game.server().getTickCount()-when<12,"bounded serialized genuine queue");return false;}
                held(profile("5s"),InteractionHand.MAIN_HAND);require(owner.use(f.owner,InteractionHand.MAIN_HAND),"native overlap before external replacement");phase=15;return false;}
            if(phase==15){require(owner.running()&&owner.requestedTicks()==100,"genuine running sprint before foreign replacement");
                game.server().tickRateManager().requestGameToSprint(2);require(!owner.running()&&owner.pendingCount()==0&&game.server().tickRateManager().isSprinting(),"foreign native replacement releases only old owner before accepting its own duration");
                phase=16;when=game.server().getTickCount();return false;}
            if(phase==16){if(game.server().tickRateManager().isSprinting()){require(game.server().getTickCount()-when<=2,"foreign replacement keeps its own exact duration");return false;}
                held(profile("5s"),InteractionHand.MAIN_HAND);require(owner.use(f.owner,InteractionHand.MAIN_HAND),"genuine mid-flight freeze source");phase=17;return false;}
            if(phase==17){require(owner.running(),"real sprint before external mid-flight freeze");game.server().tickRateManager().setFrozen(true);
                require(!owner.running()&&!game.server().tickRateManager().isSprinting()&&game.server().tickRateManager().isFrozen(),"external freeze wins over owned native mid-flight continuation");game.server().tickRateManager().setFrozen(false);
                held(profile("5s"),InteractionHand.MAIN_HAND);require(owner.use(f.owner,InteractionHand.MAIN_HAND),"real rewind overlap request");require(game.vanity().requestCapture(f.owner,false),"actual coordinator point admission");phase=13;when=game.server().getTickCount();return false;}
            if(phase==13){if(!game.vanity().hasSnapshot()||game.vanity().capturePreparing()){require(game.server().getTickCount()-when<200,"native point capture readiness");return false;}
                require(owner.running(),"native sprint overlaps real coordinator point");require(game.vanity().requestRestoreForCheck(),"actual coordinator rewind admission");rewinding=true;phase=14;when=game.server().getTickCount();return false;}
            if(phase==14){require(!owner.running()&&owner.pendingCount()==0&&!game.server().tickRateManager().isSprinting(),"rewind's actual freeze cancels owned sprint and pending future before restore");
                if(game.vanity().busy()){require(game.server().getTickCount()-when<400,"bounded native coordinator restoration");return false;}
                require(!game.server().tickRateManager().isFrozen(),"native rewind retains its own original freeze state");game.vanity().stop();rewinding=false;return true;}
            throw new IllegalStateException("Unexpected native clock phase "+phase);
        }
        public void close(){if(closed)return;closed=true;input=null;owner.stop();game.vanity().stop();stats.close();
            f.level.getBlockTicks().clearArea(area);f.level.getFluidTicks().clearArea(area);savedBlock.forEach(f.level.getBlockTicks()::schedule);savedFluid.forEach(f.level.getFluidTicks()::schedule);
            for(var entity:f.level.getAllEntities())if(!original.contains(entity.getUUID())&&(entity instanceof Projectile||entity instanceof ItemEntity||entity==witness))f.own(entity);
            f.close();}
    }
}
