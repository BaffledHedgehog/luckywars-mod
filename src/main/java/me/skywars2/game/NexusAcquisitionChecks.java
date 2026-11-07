package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Scheduled observations of the registered acquisition service; never calls its tick method. */
public final class NexusAcquisitionChecks {
    private NexusAcquisitionChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Nexus acquisition: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scheduled fixture");running=new Fixture(game);
        try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw new IllegalStateException(error);}
    }
    public static void tick(GameController game){
        if(game==null||running==null)return;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS nexus-acquisition (production cage rolls/all colors; actual UseEntity callbacks and offhand debit; adjacent identity and exact stack; native proximity recipe/TimeStop; exact-AIR center and native tree step; paused typed nonempty lookup replay; scoped terrain/entities/RNG restoration)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL nexus-acquisition",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final ServerLevel home,nexus;
        final LbcNexusAcquisition service;final LbcNexusEcology ecology;
        final MatchCoreCheckpoint core;final MatchWorldRandomCheckpoint random;final LbcIndirectKills.Checkpoint ledger;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Set<UUID> before=new HashSet<>(),cagesBefore=new HashSet<>(),actorsBefore=new HashSet<>();
        final List<Entity> owned=new ArrayList<>();final List<Entity> temporarilyPaused=new ArrayList<>();
        final Vec3 origin=new Vec3(8.5,180,8.5);final AABB area=new AABB(-56,115,-56,73,246,73);
        RegenerationBeltChecks.ScheduledClient client,audience;LbcIndirectKills.Owner cause;
        LbcNexusEcology.Actor center,tree;Vec3 treeStart;ItemEntity red,green,black;
        ItemDisplay unrelatedDisplay;Interaction unrelatedInteraction,ordinary;
        final BlockPos nexusMag=BlockPos.containing(origin.add(-4,0,0)),nexusTech=BlockPos.containing(origin.add(0,0,5));
        int stage,when,started;boolean captured,closed,stopped;int redTotal,greenTotal,blackTotal;
        Fixture(GameController game){
            this.game=game;core=new MatchCoreCheckpoint(game);random=new MatchWorldRandomCheckpoint(game);
            ledger=game.items().indirectKills().checkpoint(new MatchDamageSources(game,List.of()));
            base=new ChefChecks.Fixture(game);home=base.owner.level();nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);
            require(nexus!=null&&home!=nexus&&!home.dimension().equals(LbcHedgehogDimensions.IMPRINTED),"ordinary fixture and native Nexus");
            service=game.items().nexusAcquisition();ecology=service.ecology();
            require(!game.items().timeStop().stopped(nexus),"no borrowed Nexus TimeStop");
            for(var cage:service.cages())cagesBefore.add(cage.id);
            for(var actor:ecology.snapshot()){actorsBefore.add(actor.id);require(actor.level!=nexus||actor.position.distanceToSqr(origin)>4096,"no unrelated environmental actor within64");}
            for(var cage:service.cages())require(cage.level!=nexus||cage.position.distanceToSqr(origin)>4096,"no unrelated cage within64");
            require(service.cages().stream().noneMatch(c->c.level==nexus),"isolated Nexus tree fixture: source tree_end_all may clear every Nexus cage");
            require(ecology.snapshot().stream().noneMatch(a->a.level==nexus&&a.kind==LbcNexusEcology.Kind.SPREAD),"isolated Nexus global spread-clear fixture");
            require(ecology.snapshot().stream().noneMatch(a->a.level==nexus&&a.kind==LbcNexusEcology.Kind.TREE&&a.position.distanceToSqr(origin)<=10000),"source tree_end_all cannot borrow an unrelated tree within100");
        }
        void begin(int next){stage=next;when=game.server().getTickCount();}
        void init(){
            base.init();client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,base.other);
            game.config().noBorders=true;base.owner.getAbilities().invulnerable=true;base.other.getAbilities().invulnerable=true;
            // Persistent foreign actors remain resident and retain their existing state throughout this fixture.
            for(var actor:ecology.snapshot())if(actor.body!=null&&!actor.body.entityTags().contains("time_stopped")){actor.body.entityTags().add("time_stopped");temporarilyPaused.add(actor.body);}
            nexus.getAllEntities().forEach(e->before.add(e.getUUID()));started=game.server().getTickCount();begin(0);
        }
        boolean ready(){
            boolean ready=true;
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)if(nexus.getChunkSource().getChunkNow(x,z)==null||!nexus.areEntitiesLoaded(ChunkPos.pack(x,z))){game.maps().prepareChunk(nexus,new ChunkPos(x,z));ready=false;}
            return ready;
        }
        void capture(){
            nexus.getAllEntities().forEach(e->before.add(e.getUUID()));
            for(var entity:nexus.getAllEntities())if(entity instanceof ItemEntity&&!entity.entityTags().contains("time_stopped")){entity.entityTags().add("time_stopped");temporarilyPaused.add(entity);}
            for(var cage:service.cages())cagesBefore.add(cage.id);
            for(var actor:ecology.snapshot()){
                actorsBefore.add(actor.id);
                if(actor.body!=null&&!actor.body.entityTags().contains("time_stopped")){actor.body.entityTags().add("time_stopped");temporarilyPaused.add(actor.body);}
            }
            require(service.cages().stream().noneMatch(c->c.level==nexus),"loaded sections retain isolated tree-end cage guard");
            require(ecology.snapshot().stream().noneMatch(a->a.level==nexus&&(a.kind==LbcNexusEcology.Kind.SPREAD||a.kind==LbcNexusEcology.Kind.TREE&&a.position.distanceToSqr(origin)<=10000)),"loaded sections retain isolated global/tree stop guards");
            require(nexus.getEntities((Entity)null,area,e->e.position().distanceToSqr(origin)<=4096&&(e.entityTags().contains("nexus_rc")||e.entityTags().contains("wire_converter_item")||e.entityTags().contains("wire_converter_frame"))).isEmpty(),"loaded native sections contain no unrelated environment near fixture");
            // Includes every possible six-block tree step and each owned cage clear cube.
            for(var p:BlockPos.betweenClosed(new BlockPos(0,171,0),new BlockPos(15,189,15))){terrain.put(p.immutable(),game.maps().snapshot(nexus,p));require(nexus.getBlockState(p).isAir(),"isolated Nexus void footprint");}
            captured=true;cause=game.items().indirectKills().wildCause(base.owner);
            move(nexus,origin.add(0,0,-2));
            // Artifact activation belongs to the ordinary-world inventory service.
            base.other.teleportTo(home,base.at.x+3,base.at.y,base.at.z-2,Set.of(),0,0,true);audience.acknowledge();
            unrelatedDisplay=new ItemDisplay(EntityTypes.ITEM_DISPLAY,nexus);unrelatedDisplay.setPos(origin.add(0,.5,0));unrelatedDisplay.setItemStack(new ItemStack(Items.DIAMOND));add(unrelatedDisplay);
            unrelatedInteraction=new Interaction(EntityTypes.INTERACTION,nexus);unrelatedInteraction.setPos(origin);add(unrelatedInteraction);
            require(nexus.setBlock(nexusMag,LbcMagic.MAGIC_BEACON_BLOCK.defaultBlockState(),2)&&nexus.setBlock(nexusTech,LbcMagic.MECHANISM_BEACON_BLOCK.defaultBlockState(),2),"native Nexus MAG/TECH blocks placed inside saved footprint");
            base.other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(base.other);
        }
        void add(Entity entity){require(nexus.addFreshEntity(entity),"native owned entity admitted");owned.add(entity);}
        void move(ServerLevel level,Vec3 point){base.owner.teleportTo(level,point.x,point.y,point.z,Set.of(),0,0,true);base.owner.setDeltaMovement(Vec3.ZERO);client.acknowledge();}
        void stop(){if(!stopped){game.items().timeStop().begin(nexus,2000);stopped=true;}}
        void resume(){if(stopped){game.items().timeStop().end(nexus);stopped=false;}}
        LbcNexusAcquisition.Cage cage(int color,Vec3 point){
            var ids=new HashSet<UUID>();for(var c:service.cages())ids.add(c.id);
            service.createCage(nexus,point,color,cause);
            var made=service.cages().stream().filter(c->!ids.contains(c.id)).toList();require(made.size()==1,"actual source loot creates one cage color="+color);
            var cage=made.getFirst();require(cage.reward!=null&&cage.frame!=null&&cage.body!=null&&!cage.reward.getItemStack().isEmpty(),"fully bound pre-rolled native cage");
            owned.addAll(List.of(cage.body,cage.reward,cage.frame));return cage;
        }
        net.minecraft.world.InteractionResult click(LbcNexusAcquisition.Cage cage,InteractionHand hand){
            require(client.ready()&&(cage.body.isRemoved()||base.owner.isWithinEntityInteractionRange(cage.body,0)),"native loaded player in live cage range; removed target explicitly tests replay rejection");
            var result=net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(base.owner,base.owner.level(),hand,cage.body,null);
            base.owner.connection.handleClientTickEnd(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.INSTANCE);return result;
        }
        List<ItemEntity> drops(){return nexus.getEntitiesOfClass(ItemEntity.class,area,e->!before.contains(e.getUUID())&&!e.isRemoved());}
        int count(int kind){return drops().stream().filter(e->LbcNexusWireItems.kind(e.getItem())==kind).mapToInt(e->e.getItem().getCount()).sum();}
        void discardDrops(){for(var item:drops())item.discard();}
        void cages(){
            require(base.other.level()==home&&game.items().magic().artifactActive(base.other),"real ordinary-world artifact inventory activation");
            require(nexus.getBlockState(nexusMag).is(LbcMagic.MAGIC_BEACON_BLOCK)&&nexus.getBlockState(nexusTech).is(LbcMagic.MECHANISM_BEACON_BLOCK)&&Vec3.atCenterOf(nexusMag).distanceToSqr(base.owner.position())<400&&Vec3.atCenterOf(nexusTech).distanceToSqr(base.owner.position())<400,"Nexus cages run amid actual native MAG/TECH blocks; native suppression indexes the ordinary map");
            for(int color=1;color<=4;color++){
                var c=cage(color,origin);var adjacent=cage(color,origin.add(1,0,0));
                var expected=c.reward.getItemStack().copy();expected.set(DataComponents.CUSTOM_NAME,Component.literal("Exact native cage "+color));
                expected.enchant(base.owner.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING),3);
                expected.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);c.reward.setItemStack(expected.copy());
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,3));
                require(!click(c,InteractionHand.MAIN_HAND).consumesAction()&&base.owner.getMainHandItem().getCount()==3,"no wire fails without debit");
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusWireItems.item(color%4+1),3));
                require(!click(c,InteractionHand.MAIN_HAND).consumesAction()&&base.owner.getMainHandItem().getCount()==3,"wrong wire fails without debit");
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusWireItems.item(color),7));
                var mixed=LbcTmtItems.prototype(2);mixed.setCount(2);var data=mixed.get(DataComponents.CUSTOM_DATA).copyTag();
                var traits=game.items().tmt().catalog().families().stream().filter(row->row.id().equals("native/nexus_wire_red")||row.id().equals("native/nexus_wire_blue")||row.id().equals("native/nexus_wire_black")||row.id().equals("native/nexus_wire_white")).map(row->row.variants().getFirst()).toList();
                require(traits.size()==4,"all four current wire properties sampled");LbcTmtAbilities.write(data,traits);mixed.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));var expectedMixed=mixed.copyWithCount(1);base.owner.setItemInHand(InteractionHand.OFF_HAND,mixed);
                int size=drops().size();require(click(c,InteractionHand.OFF_HAND).consumesAction(),"actual offhand callback accepts color="+color);
                require(base.owner.getOffhandItem().getCount()==1&&base.owner.getMainHandItem().getCount()==7,"exactly one used-hand wire debit");
                require(ItemStack.matches(base.owner.getOffhandItem(),expectedMixed),"mixed wire cage exchange preserves every private color and physical component");
                var outputs=drops();require(outputs.size()==size+1&&outputs.stream().anyMatch(e->ItemStack.matches(e.getItem(),expected)),"exact complete pre-rolled reward stack");
                require(c.body.isRemoved()&&c.reward.isRemoved()&&c.frame.isRemoved()&&!adjacent.body.isRemoved()&&!adjacent.reward.isRemoved()&&!adjacent.frame.isRemoved(),"actual clicked adjacent cage identity");
                require(!click(c,InteractionHand.OFF_HAND).consumesAction()&&base.owner.getOffhandItem().getCount()==1&&drops().size()==size+1,"double click produces no free reward");
                adjacent.reward.setItemStack(ItemStack.EMPTY);require(!click(adjacent,InteractionHand.MAIN_HAND).consumesAction()&&base.owner.getMainHandItem().getCount()==7,"missing reward fails without debit");
                adjacent.body.discard();discardDrops();
            }
            require(!unrelatedDisplay.isRemoved()&&!unrelatedInteraction.isRemoved(),"unrelated Display and Interaction preserved");
            // Existing source cages are usable in ordinary worlds, while Imprinted admission is excluded.
            ordinary=new Interaction(EntityTypes.INTERACTION,home);ordinary.setPos(base.at);ordinary.setWidth(1);ordinary.setHeight(1);ordinary.entityTags().add("wire_converter_red");require(home.addFreshEntity(ordinary),"native ordinary interaction");owned.add(ordinary);
            var reward=new ItemDisplay(EntityTypes.ITEM_DISPLAY,home);reward.setPos(base.at.add(0,.5,0));reward.entityTags().add("wire_converter_item");reward.setItemStack(new ItemStack(Items.EMERALD));require(home.addFreshEntity(reward),"ordinary reward");owned.add(reward);
            var frame=new ItemDisplay(EntityTypes.ITEM_DISPLAY,home);frame.setPos(base.at.add(0,.5,0));frame.entityTags().add("wire_converter_frame");frame.setItemStack(new ItemStack(Items.STONE));require(home.addFreshEntity(frame),"ordinary frame");owned.add(frame);
            move(home,base.at.add(0,0,-2));
            base.block(base.center.offset(-4,0,2),LbcMagic.MAGIC_BEACON_BLOCK);base.block(base.center.offset(4,0,2),LbcMagic.MECHANISM_BEACON_BLOCK);
        }
        void ordinaryCases(){
            require(game.items().magic().artifactActive(base.other)&&game.items().magic().artifactSuppressedAt(home,base.owner.position()),"positive ordinary artifact radius control before actual cage callback");
            require(game.items().magic().beaconSuppressed(base.owner.position())&&game.items().magic().mechanismSuppressed(base.owner),"positive ordinary MAG and TECH controls before actual cage callback");
            // Outside Nexus the wire is an automatic consumer; supply it only after the index wait.
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusWireItems.RED,2));
            var result=net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(base.owner,home,InteractionHand.MAIN_HAND,ordinary,null);
            require(result.consumesAction()&&base.owner.getMainHandItem().getCount()==1,"ordinary-world native cage callback ignores positively active artifact/MAG/TECH; result="+result+", count="+base.owner.getMainHandItem().getCount());
            base.owner.connection.handleClientTickEnd(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.INSTANCE);
            var imprinted=game.server().getLevel(LbcHedgehogDimensions.IMPRINTED);require(imprinted!=null,"native Imprinted dimension");move(imprinted,origin);
            require(!LbcDimensionItems.ordinary(base.owner),"Imprinted excludes ordinary cage admission");
            var excluded=new Interaction(EntityTypes.INTERACTION,imprinted);excluded.setPos(origin.add(0,0,1));excluded.setWidth(1);excluded.setHeight(1);excluded.entityTags().add("wire_converter_red");require(imprinted.addFreshEntity(excluded),"native Imprinted interaction");owned.add(excluded);
            require(!net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(base.owner,imprinted,InteractionHand.MAIN_HAND,excluded,null).consumesAction()&&base.owner.getMainHandItem().getCount()==1&&!excluded.isRemoved(),"Imprinted actual callback fails with no wire debit");
            base.owner.connection.handleClientTickEnd(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.INSTANCE);move(nexus,origin.add(0,0,-2));
        }
        ItemEntity wire(int kind,int amount,Vec3 point){var item=new ItemEntity(nexus,point.x,point.y,point.z,new ItemStack(LbcNexusWireItems.item(kind),amount));item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setNeverPickUp();add(item);return item;}
        void recipe(){
            stop();red=wire(1,2,origin);green=wire(2,2,origin.add(1,0,0));black=wire(3,2,origin.add(-1,0,0));wire(2,1,origin.add(0,0,1));wire(3,1,origin.add(0,0,-1));
            redTotal=count(1);greenTotal=count(2);blackTotal=count(3);require(redTotal==2&&greenTotal==3&&blackTotal==3,"recipe source counts plus extra singletons");
            center=ecology.center(nexus,origin.add(-3,0,0),cause);require(center!=null,"native center admitted");nexus.setBlock(center.body.blockPosition(),Blocks.STONE.defaultBlockState(),2);
            var checkpoint=service.checkpoint();require(checkpoint.prepare(id->id.equals(center.id)?null:lookup(id)).pending().contains(center.id),"typed actor lookup reports a deliberately missing required native UUID");
            var plan=checkpoint.prepare(this::lookup);require(plan.pending().isEmpty(),"nonempty cage/wire/actor checkpoint resolves all native UUIDs");
            int actors=ecology.count(),cages=service.cages().size();service.prepareFrozen();plan.install().run();service.publishFrozen();require(ecology.count()==actors&&service.cages().size()==cages,"typed paused checkpoint rebinds native actors and cages");
            center=ecology.snapshot().stream().filter(a->a.id.equals(center.id)).findFirst().orElseThrow();begin(2);
        }
        void clearOwnedActors(){if(!captured)return;for(var actor:ecology.snapshot())if(!actorsBefore.contains(actor.id)&&actor.body!=null){actor.body.discard();ecology.unloaded(actor.body);}}
        Entity lookup(UUID id){for(var level:game.server().getAllLevels()){var entity=level.getEntity(id);if(entity!=null)return entity;}return null;}
        boolean step(){
            require(game.server().getTickCount()-started<600,"bounded real-tick fixture stage="+stage);client.acknowledge();audience.acknowledge();int elapsed=game.server().getTickCount()-when;
            switch(stage){
                case 0->{if(!ready())return false;capture();begin(1);}
                case 1->{if(!game.items().magic().artifactActive(base.other))return false;cages();begin(6);}
                case 6->{
                    if(elapsed<1)return false;
                    boolean artifact=game.items().magic().artifactActive(base.other)&&game.items().magic().artifactSuppressedAt(home,base.owner.position());
                    boolean magic=game.items().magic().beaconSuppressed(base.owner.position()),mechanism=game.items().magic().mechanismSuppressed(base.owner);
                    require(elapsed<40,"ordinary native suppression indexing deadline: artifact="+artifact+" MAG="+magic+" TECH="+mechanism);
                    if(!artifact||!magic||!mechanism)return false;
                    ordinaryCases();cage(4,origin.add(3,0,0));recipe();
                }
                case 2->{require(count(1)==redTotal&&count(2)==greenTotal&&count(3)==blackTotal&&count(4)==0&&center.lifetime==0,"TimeStop freezes recipe and ecology");if(elapsed>=5){resume();begin(3);}}
                case 3->{
                    int whites=count(4);if(whites==0)return false;stop();require(whites==1&&count(1)==redTotal-1&&count(2)==greenTotal-1&&count(3)==blackTotal-1,"one native source step debits exactly one of each despite singletons");
                    require(center.body!=null&&!center.body.isRemoved()&&nexus.getBlockState(center.body.blockPosition()).is(Blocks.STONE),"non-AIR center does not emit green");
                    require(!unrelatedDisplay.isRemoved()&&!unrelatedInteraction.isRemoved(),"recipe preserves unrelated presentation");discardDrops();nexus.setBlock(center.body.blockPosition(),Blocks.AIR.defaultBlockState(),2);resume();begin(4);
                }
                case 4->{
                    if(count(2)==0)return false;stop();require(center.body.isRemoved(),"exact AIR center consumed by registered native tick");
                    var emitted=drops().stream().filter(e->LbcNexusWireItems.isBlue(e.getItem())).toList();require(emitted.size()==1&&emitted.getFirst().isNoGravity()&&emitted.getFirst().isCurrentlyGlowing(),"green producer is one floating glowing native stack");
                    var children=ecology.snapshot().stream().filter(a->!actorsBefore.contains(a.id)&&a.kind==LbcNexusEcology.Kind.SPREAD).toList();require(children.size()==1&&children.getFirst().lifetime==0,"source-born spread waits until next native tick");
                    clearOwnedActors();discardDrops();
                    treeStart=origin;tree=ecology.tree(nexus,treeStart,cause);require(tree!=null,"native tree root");resume();begin(5);
                }
                case 5->{
                    Vec3 moved=tree.body.position().subtract(treeStart);if(moved.lengthSqr()==0)return false;stop();
                    int axes=(moved.x!=0?1:0)+(moved.y!=0?1:0)+(moved.z!=0?1:0);double length=moved.length();require(axes==1&&length>=1&&length<=6&&length==Math.rint(length),"one actual native cardinal integer tree move1..6");
                    clearOwnedActors();var a=ecology.spread(nexus,origin,cause);var b=ecology.spread(nexus,origin.add(1,0,0),game.items().indirectKills().wildCause(base.other));require(a!=null&&b!=null,"two causal spread owners");
                    // Author-confirmed stop scope is all same-kind actors in this native Nexus level.
                    require(ecology.snapshot().stream().noneMatch(x->actorsBefore.contains(x.id)&&x.level==nexus&&x.kind==LbcNexusEcology.Kind.SPREAD),"no unrelated spread borrowed for global stop");
                    ecology.clearSpread(nexus);require(a.body.isRemoved()&&b.body.isRemoved(),"same-kind Nexus clear crosses owners");return true;
                }
                default->throw new IllegalStateException("acquisition fixture stage");
            }return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;boolean wasFrozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    this::clearOwnedActors,
                    ()->{if(captured)for(var cage:service.cages())if(!cagesBefore.contains(cage.id)&&cage.body!=null)cage.body.discard();},
                    ()->{for(var entity:owned){if(entity instanceof Marker)game.items().magic().unloaded(entity);entity.discard();}},
                    ()->{if(captured){for(var entity:nexus.getEntities((Entity)null,area,e->!before.contains(e.getUUID())&&(e instanceof ItemEntity||e instanceof ExperienceOrb)))entity.discard();terrain.forEach((pos,image)->game.maps().restoreSnapshot(nexus,pos,image));}},
                    this::resume,
                    ()->{for(var entity:temporarilyPaused)entity.entityTags().remove("time_stopped");},
                    ()->{if(base.owner.level()!=home)move(home,base.at);if(base.other.level()!=home){base.other.teleportTo(home,base.at.x+2,base.at.y,base.at.z,Set.of(),0,0,true);if(audience!=null)audience.acknowledge();}},
                    base::close,core::restoreFrozen,
                    ()->require(ledger.restoreFrozen(id->game.server().getPlayerList().getPlayer(id)).isEmpty(),"original causal ledger restored"),random::restoreFrozen,
                    ()->{if(client!=null)client.close();if(audience!=null)audience.close();}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            game.server().tickRateManager().setFrozen(wasFrozen);if(failure!=null)throw new IllegalStateException("Nexus acquisition restoration",failure);
        }
    }
}
