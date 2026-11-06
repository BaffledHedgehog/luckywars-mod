package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.util.*;

/** Explicit operator fixture: actual item scheduler triggers every attack under test. */
public final class DeathCompassChecks {
    private DeathCompassChecks() { }
    private static Fixture running;
    private static Warden stopWitness;
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Death Compass: "+why);
    }
    private static Field field(Object target,String name) {
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try {
            var result=type.getDeclaredField(name);result.setAccessible(true);return result;
        }catch(NoSuchFieldException ignored) { }
        throw new IllegalStateException("Missing native field "+name);
    }
    private static Object read(Object target,String name) {
        try{return field(target,name).get(target);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static void write(Object target,String name,Object value) {
        try{field(target,name).set(target,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    // Reuse the full existing player/combat/profile snapshot and genuinely scheduled native client.
    // These are deliberately private to their original fixture; reflection avoids a second copy.
    private static Object shared(String name,GameController game,ServerPlayer player) {
        try {
            var type=Class.forName(PassiveConsumableChecks.class.getName()+"$"+name);
            var constructor=type.getDeclaredConstructor(GameController.class,ServerPlayer.class);
            constructor.setAccessible(true);return constructor.newInstance(game,player);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Shared native fixture "+name,error);}
    }
    private static Object call(Object target,String name,Object... args) {
        try {
            for(var method:target.getClass().getDeclaredMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length) {
                method.setAccessible(true);return method.invoke(target,args);
            }
            throw new NoSuchMethodException(name);
        }catch(InvocationTargetException error){throw new IllegalStateException(name,error.getCause());}
        catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}
    }
    private static CompoundTag saved(Entity actor) {
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,actor.registryAccess());
        actor.saveWithoutId(output);return output.buildResult();
    }
    private static boolean flag(ItemStack stack,String name) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getIntOr(name,0)==1;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final boolean stopOnly;
        ServerPlayer owner,other;
        ServerLevel level;
        LbcDeathCompass service;
        Object snapshot,client;
        SauvojenMeditationChecks.Attribution attribution;
        Optional<GlobalPos> death,otherDeath;
        Vec3 origin;
        ItemStack compass;
        Marker marker;
        Warden last;
        Object oldStage;
        int oldPhase,start,boundary,stage,mana;
        boolean closed,oldSkills,configured,stopped,stunned,observer,rulesCaptured,mobDrops;
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> baseline=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        Fixture(GameController game,boolean stopOnly){this.game=game;this.stopOnly=stopOnly;}
        void initialize() {
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.allowDamage(owner)&&game.allowDamage(other),"two actual match participants");
            require(owner.containerMenu==owner.inventoryMenu,"closed personal inventory");
            require(!game.items().timeStop().stopped(owner.level())&&!game.items().shooters().stunned(owner)
                    &&!game.items().frost().isFrozen(owner),"ambient status is not overwritten");
            snapshot=shared("Snapshot",game,owner);death=owner.getLastDeathLocation();otherDeath=other.getLastDeathLocation();
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));
            level=owner.level();origin=owner.position();service=game.items().deathCompass();oldPhase=(Integer)read(service,"phase");
            mobDrops=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS);rulesCaptured=true;
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,true,game.server());
            baseline.addAll(game.items().creatureDrops().snapshot());oldStage=read(game,"stage");
            oldSkills=game.config().lbcSkills;configured=true;
            owner.stopUsingItem();owner.removeAllEffects();owner.getInventory().clearContent();
            owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);
            owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);
            client=shared("ScheduledClient",game,owner);
            var loot=game.lbc().rollTable(level,origin,owner,"lbc:ported/death_compass");
            require(loot.size()==1,"one scoped original staging leaf");compass=loot.getFirst();
            require(compass.is(LbcSpecialArtifactItems.Kind.DEATH_COMPASS.item)&&compass.getCount()==1&&flag(compass,"death_compass"),"original vanilla carrier/count/flag");
            require(!flag(compass,"transformable")&&!flag(compass,"accessory")&&!flag(compass,"gadget"),"no invented source flags");
            require(compass.has(DataComponents.CUSTOM_NAME)&&compass.has(DataComponents.LORE)
                    &&compass.get(DataComponents.TOOLTIP_STYLE).equals(Identifier.parse("lbc:tier_q")),"original name/lore/tier presentation");
            if(!stopOnly){matching();terrain();}reset();
            // Commands can run before this tick's controller callback. Arm only from GameChecks after it.
            start=game.server().getTickCount();boundary=start;stage=-1;
        }
        void death(Optional<GlobalPos> value){write(owner,"lastDeathLocation",value);}
        void matching() {
            var at=owner.position();
            try {
                owner.setPos(-.25,origin.y,-.25);
                death(Optional.of(GlobalPos.of(Level.NETHER,new BlockPos(-1,-200,-1))));
                require(LbcDeathCompass.matchingDeath(owner),"negative coordinates floor; death Y and dimension ignored");
                death(Optional.of(GlobalPos.of(level.dimension(),new BlockPos(0,Mth.floor(origin.y),0))));
                require(!LbcDeathCompass.matchingDeath(owner),"negative coordinates do not truncate toward zero");
                death(Optional.empty());
                write(other,"lastDeathLocation",Optional.of(GlobalPos.of(level.dimension(),new BlockPos(-1,-200,-1))));
                require(LbcDeathCompass.matchingDeath(owner)&&owner.getLastDeathLocation().isEmpty(),"missing own death uses another remembered death without inventing own history");
            }finally{owner.setPos(at);death(death);write(other,"lastDeathLocation",otherDeath);}
        }
        void put(BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
            terrain.computeIfAbsent(pos.immutable(),game.maps()::snapshot);
            game.maps().restoreSnapshot(pos,new MapManager.BlockSnapshot(state,null));
        }
        void terrain() {
            int x=Mth.floor(origin.x),z=Mth.floor(origin.z),y=Math.min(120,level.getMaxY()-4);
            require(level.getChunkSource().getChunkNow(x>>4,z>>4)!=null,"controlled column already resident");
            // A whole column is needed to prove the search fails rather than landing on a lower map floor.
            for(int cy=level.getMinY();cy<=y+3;cy++)put(new BlockPos(x,cy,z),Blocks.AIR.defaultBlockState());
            require(LbcDeathCompass.spawnY(level,x,z,y+2)==Integer.MIN_VALUE,"no floor fails without an invented landing");
            put(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
            require(LbcDeathCompass.spawnY(level,x,z,y+2)==y+1,"native two-air floor returns feet height");
            put(new BlockPos(x,y+2,z),Blocks.STONE.defaultBlockState());
            require(LbcDeathCompass.spawnY(level,x,z,y+2)==Integer.MIN_VALUE,"one air cell is insufficient");
            put(new BlockPos(x,y+2,z),Blocks.AIR.defaultBlockState());put(new BlockPos(x,y,z),Blocks.LAVA.defaultBlockState());
            require(LbcDeathCompass.spawnY(level,x,z,y+2)==Integer.MIN_VALUE,"liquid floor is unsafe");
            put(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
            require(LbcDeathCompass.spawnY(level,x,z,y)==Integer.MIN_VALUE,"floor at explicit under limit is rejected");
            spread(x,z,y);
            int distantX=20_000_000,distantZ=20_000_000;
            require(level.getChunkSource().getChunkNow(distantX>>4,distantZ>>4)==null,"negative residency control starts unloaded");
            require(LbcDeathCompass.spawnY(level,distantX,distantZ,120)==Integer.MIN_VALUE
                    &&level.getChunkSource().getChunkNow(distantX>>4,distantZ>>4)==null,"resident spread floor helper never loads absent chunk");
            restoreTerrain();
        }
        void spread(int x,int z,int y) {
            var actor=new Marker(EntityTypes.MARKER,level);actor.setPos(x+.5,y+15,z+.5);actors.add(actor);
            require(level.addFreshEntity(actor),"real controlled spread target inserted");
            int[] draws={0};var delegate=net.minecraft.util.RandomSource.create(1);
            var random=(net.minecraft.util.RandomSource)Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),
                    new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{
                        if(method.getName().equals("nextDouble")&&(args==null||args.length==0)){draws[0]++;return .5;}
                        try{return method.invoke(delegate,args);}catch(InvocationTargetException error){throw error.getCause();}
                    });
            require(LbcDeathCompass.spread(level,actor,y+2,random)&&draws[0]==2
                    &&actor.position().distanceToSqr(new Vec3(x+.5,y+1,z+.5))<1e-8,"native square center and floor+.5 successful spread");
            put(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());draws[0]=0;var previous=actor.position();
            require(!LbcDeathCompass.spread(level,actor,120,random)&&draws[0]==20002&&actor.position().equals(previous),
                    "explicit under120 unsafe spread exhausts10000 independent square samples without fallback");
            actor.discard();
        }
        void restoreTerrain(){for(var entry:terrain.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());terrain.clear();}
        List<Warden> fresh() {
            return game.items().creatureDrops().snapshot().stream().filter(actor->!baseline.contains(actor)&&!actor.isRemoved())
                    .filter(Warden.class::isInstance).map(Warden.class::cast).toList();
        }
        void reset() {
            for(var actor:fresh()){actors.add(actor);actor.discard();}
            game.items().creatureDrops().tick();
            if(marker!=null){marker.discard();marker=null;}
            if(stunned){game.items().shooters().clearStun(owner);stunned=false;}
            if(stopped){game.items().timeStop().end(level);stopped=false;}
            if(observer){game.spectators().remove(owner);observer=false;call(snapshot,"restore",game);call(snapshot,"restoreAttributes");}
            write(game,"stage",oldStage);owner.removeAllEffects();owner.entityTags().remove("nomagic_active");
            owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);
            owner.setNoGravity(true);owner.setPos(origin);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);
            death(Optional.of(GlobalPos.of(Level.NETHER,new BlockPos(Mth.floor(origin.x),-200,Mth.floor(origin.z)))));
            call(client,"acknowledge");game.items().mana().inventoryChanged(owner);game.items().mana().baseRegen(owner,0);game.items().mana().set(owner,0);mana=0;
        }
        void pulse(){write(service,"phase",19);boundary=game.server().getTickCount();}
        void absent(String why) {
            require(fresh().isEmpty()&&!owner.hasEffect(MobEffects.DARKNESS),why);
        }
        Warden activated(String why) {
            var list=fresh();require(list.size()==1,"exactly one actual native Warden: "+why);
            last=list.getFirst();actors.add(last);
            require(last.entityTags().contains("death_mob")&&LbcCreatureDrops.managed(last)
                    &&game.items().creatureDrops().owner(last)==owner&&game.items().currentEpoch(last),"shared owner and current epoch");
            require(last.getHealth()==150&&last.getMaxHealth()==500&&!last.isNoAi()&&!last.isPersistenceRequired()
                    &&last.getCustomName()==null,"source native health150/default max500/AI/persistence/name");
            var data=saved(last);
            require(data.getStringOr("DeathLootTable","").equals("lbc:death_loot"),"real source death loot table");
            var brain=data.getCompoundOrEmpty("Brain").getCompoundOrEmpty("memories").getCompoundOrEmpty("minecraft:dig_cooldown");
            require(brain.getLongOr("ttl",0)>=32766&&brain.getLongOr("ttl",0)<=32767,"actual serialized dig cooldown32767");
            var darkness=owner.getEffect(MobEffects.DARKNESS);
            require(darkness!=null&&darkness.getAmplifier()==0&&darkness.getDuration()>=599
                    &&darkness.getDuration()<=600&&darkness.isVisible(),"source visible Darkness600t");
            require(game.items().mana().get(owner)==mana,"activation spends no mana");
            require(game.items().creatureDrops().canAffect(last,owner),"original owner self consequence admitted");
            require(!last.getBoundingBox().intersects(owner.getBoundingBox()),"owned Warden spawns nearby outside player footprint");
            var board=game.server().getScoreboard();var ownTeam=owner.getTeam();var otherTeam=other.getTeam();
            var testTeam=board.addPlayerTeam("compass_fixture");
            try {
                board.addPlayerToTeam(owner.getScoreboardName(),testTeam);board.addPlayerToTeam(other.getScoreboardName(),testTeam);
                require(!last.canTargetEntity(other),"native Warden brain/vibration/sonic selector protects allied player");
                var hp=other.getHealth();var darknessBefore=other.getEffect(MobEffects.DARKNESS);
                require(!other.hurtServer(level,level.damageSources().sonicBoom(last),10)&&other.getHealth()==hp,
                        "actual native sonic damage respects same owned-actor protection");
                require(!other.addEffect(ItemEffectLevels.create(MobEffects.DARKNESS,260,0),last)
                        &&other.getEffect(MobEffects.DARKNESS)==darknessBefore,"native Warden Darkness retains allied-player protection");
                require(game.items().creatureDrops().canAffect(last,owner),"alliance never removes original holder self consequence");
            }finally {
                if(ownTeam==null)board.removePlayerFromTeam(owner.getScoreboardName());else board.addPlayerToTeam(owner.getScoreboardName(),ownTeam);
                if(otherTeam==null)board.removePlayerFromTeam(other.getScoreboardName());else board.addPlayerToTeam(other.getScoreboardName(),otherTeam);
                board.removePlayerTeam(testTeam);
            }
            var packets=(java.util.List<?>)read(read(client,"connection"),"packets");
            require(packets.stream().anyMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound
                    &&sound.getSound().value().location().equals(Identifier.parse("minecraft:entity.warden.dig"))
                    &&sound.getSource()==net.minecraft.sounds.SoundSource.MASTER&&sound.getVolume()==1&&sound.getPitch()==1),
                    "source dig sound reaches admitted listener with original MASTER/volume/pitch");
            last.setNoAi(true);last.setNoGravity(true);last.setDeltaMovement(Vec3.ZERO);
            return last;
        }
        void reward(Warden warden) {
            var before=new HashSet<UUID>();for(var item:level.getEntitiesOfClass(ItemEntity.class,warden.getBoundingBox().inflate(8)))before.add(item.getUUID());
            require(warden.hurtServer(level,warden.damageSources().fellOutOfWorld(),10000)&&warden.isDeadOrDying(),"actual native Warden death");
            var drops=level.getEntitiesOfClass(ItemEntity.class,warden.getBoundingBox().inflate(8),item->!before.contains(item.getUUID()));
            actors.addAll(drops);
            require(drops.size()==1&&flag(drops.getFirst().getItem(),"death_skythe"),"actual engine generates one original Death Scythe");
            require(drops.getFirst().getItem().is(Items.CARROT_ON_A_STICK)||drops.getFirst().getItem().is(LbcSpecialTools.SCYTHE),
                    "reward uses original carrier or established replacement");
        }
        boolean step() {
            int now=game.server().getTickCount();require(now-start<100,"bounded real scheduler fixture stage"+stage);
            if(stage==-1){owner.getInventory().setItem(9,compass.copy());write(service,"phase",stopOnly?19:0);start=now;boundary=now;stage=stopOnly?20:0;return false;}
            if(stage==20){require(now==boundary+1,"real next-tick stop-probe activation");stopWitness=activated("actual-stop witness");return true;}
            if(stage==0) {
                int elapsed=now-boundary;
                if(elapsed<20){absent("no early20tick activation elapsed"+elapsed);require(owner.getInventory().getItem(9).getCount()==1,"early item retained");return false;}
                require(elapsed==20,"exact actual20tick cadence");activated("container");
                require(owner.getInventory().getItem(9).isEmpty(),"container consumed once");
                reward(last);reset();owner.setItemSlot(EquipmentSlot.HEAD,compass.copy());owner.containerMenu.setCarried(compass.copy());owner.inventoryMenu.getCraftSlots().setItem(0,compass.copy());pulse();stage++;return false;
            }
            require(now==boundary+1,"one actual scheduler boundary per isolated case");
            switch(stage) {
                case 1 -> {absent("armor/cursor/craft alone never activate");reset();owner.setItemSlot(EquipmentSlot.OFFHAND,compass.copy());owner.setItemSlot(EquipmentSlot.HEAD,compass.copy());owner.containerMenu.setCarried(compass.copy());pulse();}
                case 2 -> {activated("broad clear");require(owner.getOffhandItem().getCount()==1&&owner.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&owner.containerMenu.getCarried().getCount()==1,"offhand triggers but native full clear1 consumes earlier armor match");var pointer=owner.getOffhandItem().get(DataComponents.LODESTONE_TRACKER);require(pointer!=null&&pointer.target().equals(owner.getLastDeathLocation())&&!pointer.tracked()&&Identifier.parse("lbc:death_compass").equals(owner.getOffhandItem().get(DataComponents.ITEM_MODEL)),"real carried pointer uses resolved target and recovery-compass model");reset();owner.setItemSlot(EquipmentSlot.OFFHAND,compass.copy());death(Optional.empty());write(other,"lastDeathLocation",Optional.of(GlobalPos.of(level.dimension(),BlockPos.containing(origin))));owner.entityTags().add("nomagic_active");game.config().lbcSkills=false;require(game.items().shooters().stun(other,owner,10),"real input stun installed");stunned=true;require(game.items().actionsBlocked(owner),"active input is blocked");pulse();}
                case 3 -> {activated("offhand fallback passive under stun/noMagic/skills disabled");require(owner.getOffhandItem().isEmpty()&&owner.getLastDeathLocation().isEmpty(),"fallback consumes once without writing own death history");reset();owner.getInventory().setItem(9,compass.copy());game.items().timeStop().begin(level,8);stopped=true;pulse();}
                case 4 -> {absent("actual dimension Time Stop pauses compass");require((Integer)read(service,"phase")==19&&owner.getInventory().getItem(9).getCount()==1,"Time Stop preserves phase/item");reset();owner.getInventory().setItem(9,compass.copy());marker=new Marker(EntityTypes.MARKER,level);marker.setPos(origin);marker.entityTags().add("stopper_magic");require(level.addFreshEntity(marker),"actual source beacon marker");require(game.items().magic().beaconSuppressed(origin),"beacon suppression positive control");pulse();}
                case 5 -> {absent("source beacon suppresses activation");require(owner.getInventory().getItem(9).getCount()==1,"beacon item retained");reset();owner.getInventory().setItem(9,compass.copy());death(Optional.of(GlobalPos.of(level.dimension(),BlockPos.containing(origin).offset(1,0,0))));pulse();}
                case 6 -> {absent("different floor X rejects activation");reset();
                    // Admission is synchronous: allowing the actual controller to tick in ENDING starts a real map restore.
                    write(game,"stage",GameController.Stage.ENDING);
                    try{require(!(Boolean)call(service,"admitted",owner),"ENDING rejects source admission");}finally{write(game,"stage",oldStage);}
                    game.spectators().enter(owner);observer=true;owner.getInventory().setItem(9,compass.copy());pulse();}
                case 7 -> {absent("actual observer role prevents activation");reset();write(service,"phase",7);service.stop();require((Integer)read(service,"phase")==0,"stop resets cadence");return true;}
                default -> throw new IllegalStateException("unknown Compass fixture stage"+stage);
            }
            stage++;return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;
            List<Runnable> actions=new ArrayList<>();
            actions.add(()->{for(var actor:fresh())if(!baseline.contains(actor))actors.add(actor);});
            actions.add(()->{for(var actor:actors)if(actor!=stopWitness)actor.discard();});
            actions.add(()->{if(marker!=null)marker.discard();});
            actions.add(()->{if(stopped)game.items().timeStop().end(level);});
            actions.add(()->{if(stunned)game.items().shooters().clearStun(owner);});
            actions.add(()->{if(observer)game.spectators().remove(owner);});
            actions.add(()->{if(oldStage!=null)write(game,"stage",oldStage);});
            actions.add(this::restoreTerrain);
            actions.add(()->{if(service!=null)write(service,"phase",oldPhase);});
            actions.add(()->{if(configured)game.config().lbcSkills=oldSkills;});
            actions.add(()->{if(rulesCaptured)level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,mobDrops,game.server());});
            actions.add(()->{if(client!=null)call(client,"close");});
            actions.add(()->{if(snapshot!=null)call(snapshot,"restore",game);});
            actions.add(()->{if(snapshot!=null)call(snapshot,"restoreAttributes");});
            actions.add(()->{if(snapshot!=null)call(snapshot,"restoreUse");});
            actions.add(()->{if(snapshot!=null)call(snapshot,"restoreProfile",game);});
            actions.add(()->{if(death!=null)death(death);});
            actions.add(()->{if(otherDeath!=null)write(other,"lastDeathLocation",otherDeath);});
            actions.add(()->{if(attribution!=null)attribution.close();});
            actions.add(()->game.items().creatureDrops().tick());
            Throwable failure=null;for(var action:actions)try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Compass exact scoped restoration",failure);
        }
    }
    public static void run(GameController game) {
        require(running==null,"exclusive fixture");running=new Fixture(game,false);
        try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;
        try{if(running.step()){boolean probe=running.stopOnly;running.close();running=null;SkyWars2.LOGGER.info(probe?"[check] PASS death-compass-stop-probe":"[check] PASS death-compass (original staging leaf; real20tick native scheduler; floor XZ/death Y and dimension; inventory/offhand and excluded slots; source clear1; Warden health/brain/native death reward/shared owner; Time Stop/beacon/role/ENDING and input-stun passive; resident floor safety; scoped restoration)");}}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL death-compass",error);}
    }
    public static void stopProbe(GameController game) {
        require(running==null&&stopWitness==null,"exclusive stop probe");running=new Fixture(game,true);
        try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}
    }
    public static void cleanup(GameController game) {
        if(running!=null){var old=running;running=null;old.close();}
        if(stopWitness!=null){try{require(!game.playing()&&stopWitness.isRemoved()
                    &&!game.items().creatureDrops().snapshot().contains(stopWitness),"real match stop removes managed Warden");}
            finally{stopWitness.discard();stopWitness=null;}}
        SkyWars2.LOGGER.info("[check] PASS death-compass-cleanup");
    }
}
