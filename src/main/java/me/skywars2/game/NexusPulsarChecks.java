package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Observes registered native ticks; never advances a production clock from the fixture. */
public final class NexusPulsarChecks {
    private NexusPulsarChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Nexus Pulsar: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scheduled fixture");running=new Fixture(game);
        try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw new IllegalStateException(error);}
    }
    public static void tick(GameController game){
        if(game==null||running==null)return;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS nexus-pulsar (native UseItem/UseBlock and offhand admission; ordinary/Nexus/Imprinted; positive artifact/MAG/TECH immunity; block-only cow ray; exact0/40/60/70/75 phases, connected bedrock/water/chest and disconnected control; native TimeStop/cooldown; typed nonempty detached checkpoint; exact cleanup)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL nexus-pulsar",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final ServerLevel home;final LbcNexusCombat combat;final LbcNexusPulsar pulse;
        final BlockPos seed,control;final Map<String,Integer> counters=new LinkedHashMap<>();
        RegenerationBeltChecks.ScheduledClient client,audience;Marker mag,tech;Cow cow;float cowHealth;
        int stage,when,sequence,updates;boolean closed,pausedOnce;long frozenAge;int frozenCooldown,frozenPhase;int[] frontier,visited;
        Fixture(GameController game){
            this.game=game;base=new ChefChecks.Fixture(game);home=base.owner.level();combat=game.items().nexusCombat();pulse=combat.pulsar();seed=base.center.offset(0,5,0);control=seed.offset(6,0,0);
            require(pulse.count()==0&&combat.pending()==0,"no unrelated accepted casts");
            require(!home.dimension().equals(LbcHedgehogDimensions.NEXUS)&&!home.dimension().equals(LbcHedgehogDimensions.IMPRINTED),"ordinary-world fixture");
            for(String key:List.of("pulsar_cd","nexus_tools_phase","nexus_roadgun_cd","nexus_corrupter_cd"))counters.put(key,game.items().mana().score(base.owner,key).get());
        }
        int score(String name){return game.items().mana().score(base.owner,name).get();}
        void score(String name,int value){game.items().mana().score(base.owner,name).set(value);}
        void begin(int next){stage=next;when=game.server().getTickCount();}
        void init(){
            base.init();client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,base.other);
            require(Arrays.equals(LbcNexusCombat.PULSAR_DELAYS,new int[]{0,40,60,70,75}),"source independent phase deadlines");
            for(String key:counters.keySet())score(key,0);
            base.owner.getAbilities().invulnerable=true;game.config().offhandWands=true;
            base.move(base.other,Vec3.atCenterOf(seed).add(5,0,-2));audience.acknowledge();
            base.other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(base.other);
            // The inventory notification is dirtied now and resolved by the next real item tick.
            mag=marker("stopper_magic");tech=marker("stopper");component();aim();
            require(game.items().magic().suppressed(base.owner)&&game.items().magic().suppressedAt(home,Vec3.atCenterOf(seed)),"positive suppression at caster and target");
            // Checked after the first native inventory/marker-index update.
            cow=base.cow(Vec3.atCenterOf(seed).add(0,-.5,-2));cowHealth=cow.getHealth();
            require(LbcNexusCombat.matches(new ItemStack(LbcNexusToolItems.PULSAR)),"public native Pulsar identity");
            updates=client.updates();begin(-1);
        }
        Marker marker(String tag){var marker=new Marker(EntityTypes.MARKER,home);marker.setPos(Vec3.atCenterOf(tag.equals("stopper")?seed.offset(0,0,6):seed.offset(-6,0,0)));marker.entityTags().add(tag);require(home.addFreshEntity(marker),"owned native jammer");game.items().magic().loaded(marker);return marker;}
        void component(){base.block(seed,Blocks.STONE);base.block(seed.east(),Blocks.BEDROCK);base.block(seed.east(2),Blocks.WATER);base.block(seed.east(3),Blocks.CHEST);base.block(control,Blocks.STONE);}
        void aim(){
            base.move(base.owner,new Vec3(seed.getX()+.5,seed.getY()+.5-base.owner.getEyeHeight(),seed.getZ()-2));client.acknowledge();
            base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.PULSAR));
        }
        void item(InteractionHand hand){base.owner.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(hand,++sequence,base.owner.getYRot(),base.owner.getXRot()));base.owner.connection.handleClientTickEnd(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.INSTANCE);}
        void block(InteractionHand hand){
            var hit=new BlockHitResult(new Vec3(seed.getX()+.5,seed.getY()+.5,seed.getZ()),Direction.NORTH,seed,false);
            require(client.ready(),"native UseBlock ready: "+client.diagnostics());
            require(home.mayInteract(base.owner,seed),"native UseBlock mayInteract: border="+home.getWorldBorder().isWithinBounds(seed)+" spawnProtected="+game.server().isUnderSpawnProtection(home,seed,base.owner)+" seed="+seed+" borderCenter="+home.getWorldBorder().getCenterX()+","+home.getWorldBorder().getCenterZ()+" size="+home.getWorldBorder().getSize());
            require(base.owner.isWithinBlockInteractionRange(seed,1),"native UseBlock range: seed="+seed+" eye="+base.owner.getEyePosition()+" range="+base.owner.blockInteractionRange());
            base.owner.connection.handleUseItemOn(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(hand,hit,++sequence));
            base.owner.connection.handleClientTickEnd(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.INSTANCE);
        }
        void freezeSnapshot(){
            frozenAge=pulse.age();frontier=new int[5];visited=new int[5];for(int i=0;i<5;i++){frontier[i]=pulse.frontierSize(i);visited[i]=pulse.visitedSize(i);}
        }
        void sameWave(){require(pulse.count()==1&&pulse.age()==frozenAge,"age/count preserved");for(int i=0;i<5;i++)require(frontier[i]==pulse.frontierSize(i)&&visited[i]==pulse.visitedSize(i),"phase frontier/visited preserved "+i);}
        void colors(){
            long age=pulse.age();if(age<0)return;
            var expected=age<40?DyeColor.WHITE:age<60?DyeColor.LIME:age<70?DyeColor.RED:age<75?DyeColor.BLACK:null;
            if(age>0&&expected!=null)require(home.getBlockState(seed).is(Blocks.CONCRETE.pick(expected)),"actual seed source color at active age="+age+" expected="+expected+" actual="+home.getBlockState(seed)+" visited="+pulse.visitedSize(0));
            if(age>=75)require(home.getBlockState(seed).isAir(),"source seed final AIR at75");
            require(home.getBlockState(control).is(Blocks.STONE),"disconnected component survives every wave");
            require(cow.isAlive()&&cow.getHealth()==cowHealth,"block-only ray ignores intervening cow");
        }
        boolean step(){
            int elapsed=game.server().getTickCount()-when;require(elapsed<=500,"bounded scheduled phase "+stage+" age="+pulse.age()+" pending="+combat.pending());
            switch(stage){
                case -1->{if(!game.items().magic().artifactActive(base.other))return false;
                    require(game.items().magic().suppressed(base.owner),"actual other-player inventory artifact and MAG cover caster");
                    require(game.items().magic().mechanismSuppressed(base.owner),"actual TECH source also covers caster");
                    item(InteractionHand.MAIN_HAND);require(score("pulsar_cd")==15,"public UseItem hit charges15seconds immediately despite all jammers");begin(0);
                }
                case 0->{
                    if(pulse.count()==0)return false;colors();
                    if(!pausedOnce&&pulse.age()>=5){
                        freezeSnapshot();var checkpoint=pulse.checkpoint();require(checkpoint.count()==1,"nonempty typed detached checkpoint");pulse.prepareFrozen();var plan=checkpoint.prepare(id->null);require(plan.pending().isEmpty(),"detached wave requires no player/body rebinding");plan.install().run();pulse.publishFrozen();sameWave();
                        game.items().timeStop().begin(home,100);frozenCooldown=score("pulsar_cd");frozenPhase=score("nexus_tools_phase");pausedOnce=true;begin(1);
                    }
                }
                case 1->{sameWave();require(score("pulsar_cd")==frozenCooldown&&score("nexus_tools_phase")==frozenPhase,"TimeStop freezes canonical cooldown and shared subsecond phase");require(home.getBlockState(seed).is(Blocks.CONCRETE.pick(DyeColor.WHITE)),"TimeStop freezes terrain color");if(elapsed>=10){game.items().timeStop().end(home);begin(2);}}
                case 2->{colors();if(pulse.count()==0){
                    require(client.updates()>updates,"native registered connection scheduler advanced");for(int i=0;i<4;i++)require(home.getBlockState(seed.east(i)).isAir(),"connected stone/bedrock/water/chest removed "+i);require(pulse.loadingChunks()==0&&pulse.pendingChunks()==0,"finished wave releases all loading/frontier tickets");
                    component();aim();score("pulsar_cd",0);score("nexus_tools_phase",0);base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcNexusToolItems.PULSAR));game.config().offhandWands=false;
                    require(!combat.use(base.owner,InteractionHand.OFF_HAND)&&score("pulsar_cd")==0,"disabled offhand rejected before cooldown");game.config().offhandWands=true;block(InteractionHand.OFF_HAND);require(score("pulsar_cd")==15,"enabled offhand actual UseBlock accepted under positive jammers; blocked="+game.items().actionsBlocked(base.owner)+", inputStop="+game.items().wands().inputTimeStopped(base.owner)+", worldStop="+game.items().timeStop().stopped(home)+", allowed="+game.canUseGameplayItems(base.owner)+", casting="+combat.pending()+", enabled="+base.owner.getOffhandItem().isItemEnabled(home.enabledFeatures())+", cooled="+base.owner.getCooldowns().isOnCooldown(base.owner.getOffhandItem())+", client="+client.diagnostics());begin(3);
                }}
                case 3->{if(elapsed==0)return false;if(pulse.count()>0){colors();return false;}if(combat.pending()>0)return false;
                    require(home.getBlockState(seed).isAir(),"offhand UseBlock performed real hit wave");score("pulsar_cd",0);score("nexus_tools_phase",0);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcNexusToolItems.PULSAR));base.owner.teleportTo(home,base.owner.getX(),home.getMaxY()-4,base.owner.getZ(),Set.of(),0,-90,true);client.acknowledge();item(InteractionHand.MAIN_HAND);require(score("pulsar_cd")==15,"actual upward UseItem miss charges15seconds");require(!combat.use(base.owner,InteractionHand.MAIN_HAND),"cooldown rejects repeat input");begin(4);
                }
                case 4->{require(pulse.count()==0,"upward miss never creates terrain wave");require(score("pulsar_cd")==Math.max(0,15-elapsed/20),"native cooldown exact15seconds elapsed="+elapsed+" cd="+score("pulsar_cd"));if(elapsed>=300&&combat.pending()==0){worldAdmission();begin(5);}}
                case 5->{if(combat.pending()==0){require(pulse.count()==0,"Nexus upward ray misses without terrain changes");return true;}}
                default->throw new IllegalStateException("Pulsar fixture stage");
            }
            return false;
        }
        void worldAdmission(){
            var imprinted=game.server().getLevel(LbcHedgehogDimensions.IMPRINTED);var nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);require(imprinted!=null&&nexus!=null,"native special worlds exist");
            base.owner.teleportTo(imprinted,base.at.x,imprinted.getMaxY()-4,base.at.z,Set.of(),0,-90,true);client.acknowledge();require(!combat.use(base.owner,InteractionHand.MAIN_HAND)&&score("pulsar_cd")==0,"Imprinted alone excludes cast without cooldown");
            base.owner.teleportTo(nexus,base.at.x,nexus.getMaxY()-4,base.at.z,Set.of(),0,-90,true);client.acknowledge();require(combat.use(base.owner,InteractionHand.MAIN_HAND)&&score("pulsar_cd")==15,"Nexus public route also accepts Pulsar");
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->{game.items().timeStop().end(home);pulse.stop();},
                    ()->{for(var marker:Arrays.asList(mag,tech))if(marker!=null){game.items().magic().unloaded(marker);marker.discard();}},
                    ()->{if(base.owner.level()!=home){base.owner.teleportTo(home,base.at.x,base.at.y,base.at.z,Set.of(),0,0,true);if(client!=null)client.acknowledge();}},
                    ()->{if(client!=null)client.close();if(audience!=null)audience.close();},
                    base::close,
                    ()->{counters.forEach(this::score);game.items().nexusTools().inventoryChanged(base.owner);})){try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}}
            if(failure!=null)throw new IllegalStateException("Pulsar fixture restoration",failure);
        }
    }
}
