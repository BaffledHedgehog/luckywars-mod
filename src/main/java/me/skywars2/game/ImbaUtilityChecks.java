package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Actual finite utility inputs and accepted native delayed/terrain work; disposable server only. */
public final class ImbaUtilityChecks {
    private ImbaUtilityChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("IMBA utilities: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){require(running==null,"exclusive fixture");var f=new Fixture(game,rewind);running=f;try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{require(game==f.game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} (actual source utility inputs, finite geometry, active-clock delay and native ownership)",f.rewind?"imba-utilities-rewind":"imba-utilities");}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+(f.rewind?"imba-utilities-rewind":"imba-utilities"),error);}}
    private static Field field(Object object,String name){for(var c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static Object get(Object object,String name){try{return field(object,name).get(object);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static Object call(Object object,String name,Object...args){for(var m:object.getClass().getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{m.setAccessible(true);return m.invoke(object,args);}catch(InvocationTargetException error){throw new IllegalStateException(error.getCause());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}throw new IllegalStateException(name);}
    private static final class DiceRandom implements AutoCloseable,InvocationHandler {
        final ServerPlayer player;final RandomSource original;final Field randomField;final int first,lucky;final List<Integer> bounds=new ArrayList<>();
        DiceRandom(ServerPlayer player,int roll,int luckRoll){this.player=player;first=roll-1;lucky=luckRoll-4;randomField=field(player,"random");original=player.getRandom();try{randomField.set(player,Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},this));}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        public Object invoke(Object proxy,Method method,Object[] args)throws Throwable{
            if(method.getName().equals("nextInt")&&args!=null&&args.length==1){int bound=(Integer)args[0];if(bound==6||bound==3){bounds.add(bound);return bound==6?first:lucky;}}
            try{return method.invoke(original,args);}catch(InvocationTargetException error){throw error.getCause();}
        }
        public void close(){try{randomField.set(player,original);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final ServerPlayer owner,other;final ServerLevel level;final MatchCoreCheckpoint core;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final SauvojenMeditationChecks.Attribution attribution;final MatchRandomState playerRandom;final boolean frozen;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final Set<UUID> existing=new HashSet<>();
        final Map<net.minecraft.world.entity.Marker,Map<BlockPos,MapManager.BlockSnapshot>> fields=new LinkedHashMap<>();
        final Vec3 dirterOrigin=new Vec3(1032.5,58,1032.5),padPosition=new Vec3(1032.5,90,1032.5);
        final Map<BlockPos,BlockState> bastionExpected=new LinkedHashMap<>();
        final List<net.minecraft.world.entity.animal.cow.Cow> arrowWitnesses=new ArrayList<>();
        LbcWtf.Chef arrowChef;long arrowSteps;List<LbcWhiteArrows.View> capturedRays;
        BlockPos padOrigin,bastionOrigin,dirterPlaced;Vec3 bastionActor;long expectedRandom;int phase,started,when,remaining,activeTicks;boolean closed,stopped,restoring;ServerPlayer doomed;
        List<LbcImbaTerrainUtilities.JobView> capturedJobs;List<LbcTeleportGadgets.ImbaDelay> capturedDelays;BlockPos changed;BlockState capturedBlock;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&!game.maps().busy()&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"actual participants and stable exclusive coordinator");
            require(game.items().imbaTerrainUtilities().state().jobs()==0&&game.items().teleportGadgets().pendingImba()==0&&game.items().teleportGadgets().imbaPads().isEmpty()&&game.items().whiteArrows().state().active()==0,"exclusive utility work families");
            require(!game.items().timeStop().stopped(level),"unpaused utility scene");core=new MatchCoreCheckpoint(game);saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));playerRandom=MatchRandomState.capture(owner.getRandom());frozen=game.server().tickRateManager().isFrozen();
            level.getAllEntities().forEach(e->existing.add(e.getUUID()));client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);started=game.server().getTickCount();
        }
        void next(int value){phase=value;when=game.server().getTickCount();}
        void move(Vec3 at){require(owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual native utility fixture teleport");owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setOnGround(true);client.acknowledge();}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        void remember(BlockPos at){require(level.hasChunkAt(at),"snapshot resident accepted footprint "+at);terrain.putIfAbsent(at.immutable(),game.maps().snapshot(level,at));}
        void set(BlockPos at,BlockState state){remember(at);level.setBlock(at,state,2|16|32);}
        ItemStack leaf(String id){var rolled=game.lbc().rollTable(level,owner.position(),owner,"lbc:ported/"+id);require(rolled.size()==1&&!rolled.getFirst().isEmpty(),"canonical source ready leaf "+id);return rolled.getFirst();}
        void begin(){
            game.server().tickRateManager().setFrozen(false);game.config().noBorders=true;game.config().offhandWands=true;game.config().lbcSkills=false;game.config().lbcLevelups=false;
            owner.closeContainer();owner.stopUsingItem();owner.getInventory().clearContent();owner.removeAllEffects();owner.setNoGravity(true);owner.getAbilities().invulnerable=false;owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            game.items().mana().noMagic(owner,0);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();presentation();
            if(!rewind)dice();owner.removeAllEffects();owner.entityTags().remove("sin_greed_active");move(dirterOrigin);prepareDirterFootprint();
            if(rewind){roll(owner,6,InteractionHand.MAIN_HAND,false);game.items().mana().maximum(owner,2000);game.items().mana().set(owner,2000);owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcWhiteArrowItem.TOKEN));require(owner.gameMode.useItem(owner,level,owner.getOffhandItem(),InteractionHand.OFF_HAND).consumesAction(),"actual source6 reward consumer before whole capture");launchTeleport();placeDirter();move(padPosition);next(10);}
            else if(game.config().gameMode==1){lethalDie();next(-11);}else beginArrow();
        }
        void presentation(){
            String[] names={"teleporter_imba","dirter","bedrock_bastion","god_dice"};Item[] types={LbcImbaTeleporterItem.TOKEN,LbcImbaTerrainUtilityItems.DIRTER,LbcImbaTerrainUtilityItems.BASTION,LbcGodDiceItem.TOKEN};Item[] carriers={Items.SNOWBALL,Items.COARSE_DIRT,Items.PANDA_SPAWN_EGG,Items.SNOWBALL};
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)client.connection;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{for(int n=0;n<names.length;n++){var stack=leaf(names[n]);require(stack.is(types[n])&&stack.get(DataComponents.CUSTOM_NAME)!=null&&stack.get(DataComponents.LORE)!=null,"native identity/full source display "+names[n]);require(ItemData.intValue(stack,"imba",0)==1||n==3,"source IMBA flag "+names[n]);
                if(n==0)require(ItemData.intValue(stack,"teleport1_imba",0)==1,"source teleporter identity flag");if(n==1)require(ItemData.intValue(stack,"dirter",0)==1,"source Dirter identity flag");if(n==2)require(LbcImbaTerrainUtilities.bastion(stack)&&Identifier.parse("minecraft:bedrock").equals(stack.get(DataComponents.ITEM_MODEL)),"source instant slowraycast marker and Bedrock model");if(n==3)require(ItemData.intValue(stack,"superdice",0)==1&&ItemData.intValue(stack,"gadget",0)==1&&Identifier.parse("lbc:superdice").equals(stack.get(DataComponents.ITEM_MODEL)),"source GodDice flags/model");
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),owner.registryAccess());require(proxy.is(carriers[n])&&ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),owner.registryAccess())),"source carrier/all-component Polymer roundtrip "+names[n]);
            }});
        }
        void seedAttributes(){double[] values={.73,15.777,7.333,9.37,4.37,-1.21,101.9,.12345};var keys=List.of(Attributes.KNOCKBACK_RESISTANCE,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.LUCK,Attributes.MAX_HEALTH,Attributes.MOVEMENT_SPEED);for(int n=0;n<keys.size();n++)owner.getAttribute(keys.get(n)).setBaseValue(values[n]);owner.setHealth(100);}
        void near(Holder<Attribute> key,double expected){require(Math.abs(owner.getAttribute(key).getBaseValue()-expected)<1e-7,"source attribute arithmetic "+key+" actual="+owner.getAttribute(key).getBaseValue()+" expected="+expected);}
        void roll(ServerPlayer player,int outcome,InteractionHand hand,boolean lucky){
            var holder=net.minecraft.world.scores.ScoreHolder.forNameOnly("final_luck_modifier");game.items().mana().score(holder,"lbc.math").set(lucky?7:1);
            player.setItemInHand(hand,new ItemStack(LbcGodDiceItem.TOKEN,3));var opposite=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;player.setItemInHand(opposite,new ItemStack(Items.STICK,7));var used=player.getItemInHand(hand);
            boolean lethal=outcome==1&&!LbcGreed.active(player);
            try(var random=new DiceRandom(player,lucky?2:outcome,outcome)){
                require(player.gameMode.useItem(player,player.level(),used,hand).consumesAction()&&used.getCount()==2&&(lethal||player.getItemInHand(opposite).getCount()==7),"actual GodDice used-hand debit1 outcome"+outcome);
                require(random.bounds.equals(lucky?List.of(6,3):List.of(6)),"source RNG call order and exact normal/luck7 domains outcome"+outcome+" "+random.bounds);
            }
        }
        void dice(){
            seedAttributes();owner.entityTags().add("sin_greed_active");roll(owner,1,InteractionHand.MAIN_HAND,false);require(owner.isAlive(),"source Greed protects outcome1 while actual used die is consumed");owner.entityTags().remove("sin_greed_active");
            seedAttributes();roll(owner,2,InteractionHand.OFF_HAND,false);near(Attributes.KNOCKBACK_RESISTANCE,.36);near(Attributes.ARMOR,7.888);near(Attributes.ARMOR_TOUGHNESS,3.666);near(Attributes.ATTACK_DAMAGE,4.6);near(Attributes.ATTACK_SPEED,2.18);near(Attributes.LUCK,-.7);near(Attributes.MAX_HEALTH,50);near(Attributes.MOVEMENT_SPEED,.0617);
            owner.removeAllEffects();roll(owner,3,InteractionHand.MAIN_HAND,false);for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.HUNGER,MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.SLOWNESS,MobEffects.WEAKNESS)){var actual=owner.getEffect(effect);require(actual!=null&&actual.isInfiniteDuration()&&actual.getAmplifier()==0,"source six infinite negative effects "+effect);}
            owner.removeAllEffects();roll(owner,4,InteractionHand.OFF_HAND,false);for(var effect:List.of(MobEffects.FIRE_RESISTANCE,MobEffects.HASTE,MobEffects.JUMP_BOOST,MobEffects.INVISIBILITY,MobEffects.SPEED,MobEffects.SLOW_FALLING,MobEffects.NIGHT_VISION,MobEffects.RESISTANCE,MobEffects.WATER_BREATHING)){var actual=owner.getEffect(effect);require(actual!=null&&actual.isInfiniteDuration()&&actual.getAmplifier()==0,"source nine infinite positive effects "+effect);}
            owner.removeAllEffects();seedAttributes();roll(owner,5,InteractionHand.MAIN_HAND,false);near(Attributes.ARMOR,21);near(Attributes.ARMOR_TOUGHNESS,9);near(Attributes.ATTACK_DAMAGE,12);near(Attributes.ATTACK_SPEED,5.2);near(Attributes.MAX_HEALTH,141);near(Attributes.MOVEMENT_SPEED,.14345);
            owner.getInventory().clearContent();roll(owner,6,InteractionHand.OFF_HAND,false);require(java.util.stream.IntStream.range(0,owner.getInventory().getContainerSize()).anyMatch(i->{var s=owner.getInventory().getItem(i);return ItemData.intValue(s,"whitearrow",0)==1&&ItemData.intValue(s,"raycastspell",0)==1&&Identifier.parse("lbc:whitearrow").equals(s.get(DataComponents.ITEM_MODEL));}),"source6 actual White Arrow reward components");
            for(int outcome=4;outcome<=6;outcome++){seedAttributes();owner.removeAllEffects();roll(owner,outcome,InteractionHand.MAIN_HAND,true);}
            owner.removeAllEffects();var mag=field(false,owner.position().add(0,10,0));var tech=field(true,owner.position().add(0,11,0));require(game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"actual MAG/TECH GodDice controls");roll(owner,6,InteractionHand.MAIN_HAND,false);removeField(mag);removeField(tech);
        }
        net.minecraft.world.entity.Marker field(boolean tech,Vec3 at){
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().add(tech?"stopper":"stopper_magic");var local=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();for(var pos:List.of(marker.blockPosition(),marker.blockPosition().above())){remember(pos);local.put(pos,game.maps().snapshot(level,pos));}fields.put(marker,local);require(level.addFreshEntity(marker),"actual native utility suppression field");game.items().magic().loaded(marker);return marker;
        }
        void removeField(net.minecraft.world.entity.Marker marker){game.items().magic().unloaded(marker);marker.discard();var local=fields.remove(marker);if(local!=null)for(var entry:local.entrySet())if(level.getBlockState(entry.getKey()).is(LbcMagic.MECHANISM_BEACON_BLOCK)||level.getBlockState(entry.getKey()).is(LbcMagic.MAGIC_BEACON_BLOCK)){game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),2);game.items().magic().blockChanged(entry.getKey(),level.getBlockState(entry.getKey()));}}
        void removeFields(){for(var marker:List.copyOf(fields.keySet()))removeField(marker);}
        void lethalDie(){
            require(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("UtilityDoom"))==null,"owned disposable outcome1 participant");doomed=GameChecks.createBot(game,"UtilityDoom");
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival UtilityDoom");
        }
        void disconnectDoom(){if(doomed!=null){doomed.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("IMBA utility fixture cleanup")));doomed=null;}}
        net.minecraft.world.entity.animal.cow.Cow arrowCow(Vec3 at){var cow=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);cow.setOnGround(true);cow.setDeltaMovement(Vec3.ZERO);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);require(level.addFreshEntity(cow),"real grounded White Arrow witness");arrowWitnesses.add(cow);return cow;}
        void beginArrow(){
            var from=dirterOrigin.add(0,22,0);for(int z:new int[]{16,32,48})move(from.add(0,0,z));move(from);
            var source=java.util.stream.IntStream.range(0,owner.getInventory().getContainerSize()).mapToObj(owner.getInventory()::getItem).filter(LbcWhiteArrows::matches).findFirst().orElseThrow().copy();
            require(source.is(LbcWhiteArrowItem.TOKEN)&&source.getMaxDamage()==25,"actual GodDice6 reward enters canonical White Arrow item");
            game.items().mana().maximum(owner,2000);game.items().mana().set(owner,0);owner.setItemInHand(InteractionHand.MAIN_HAND,source);
            var mag=field(false,from.add(6,0,0));require(!owner.gameMode.useItem(owner,level,source,InteractionHand.MAIN_HAND).consumesAction()&&game.items().mana().maximum(owner)==2000&&game.items().mana().get(owner)==0,"caster MAG rejects White Arrow without cost");removeField(mag);
            var greed=arrowCow(from.add(0,.8,3));greed.entityTags().add("sin_greed_active");var ally=arrowCow(from.add(0,.8,4));var role=arrowCow(from.add(0,.8,5));role.entityTags().add("strict_map_object");var victim=arrowCow(from.add(0,.8,6));
            var team=owner.getTeam();if(team==null){team=game.server().getScoreboard().addPlayerTeam("sw2_utility_arrow");game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);}game.server().getScoreboard().addPlayerToTeam(ally.getScoreboardName(),team);
            set(BlockPos.containing(from).offset(0,1,2),Blocks.STONE.defaultBlockState());
            field(false,victim.position().add(0,0,19.5));field(true,from.add(6,0,0));require(!game.items().magic().suppressed(owner)&&game.items().magic().suppressedAt(level,victim.position())&&game.items().magic().mechanismSuppressed(owner),"target MAG and caster TECH are actual positive controls");
            arrowChef=game.items().wtf().spawn(owner,victim);require(arrowChef!=null,"own native Chef witness");arrowChef.setPos(from.add(0,0,1.5));arrowChef.setNoGravity(true);arrowChef.setDeltaMovement(Vec3.ZERO);
            arrowSteps=game.items().whiteArrows().state().steps();
            require(owner.gameMode.useItem(owner,level,source,InteractionHand.MAIN_HAND).consumesAction()&&game.items().mana().maximum(owner)==1500&&game.items().mana().get(owner)==-500,"TECH admits source White Arrow and debits independent MAX/current500 including negative current");
            require(ItemStack.matches(source,new ItemStack(LbcWhiteArrowItem.TOKEN)),"source default White Arrow activation leaves token unchanged");
            owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcWhiteArrowItem.TOKEN));require(owner.gameMode.useItem(owner,level,owner.getOffhandItem(),InteractionHand.OFF_HAND).consumesAction()&&game.items().mana().maximum(owner)==1000&&game.items().mana().get(owner)==-1000,"actual offhand White Arrow independently pays source costs");
            var rays=game.items().whiteArrows().views();require(rays.size()==2&&rays.get(0).point().equals(owner.getEyePosition())&&rays.get(1).point().distanceToSqr(owner.getEyePosition().add(owner.getLookAngle().scale(.5)))<1e-8,"actual main/offhand source ray origins");pause();next(-10);
        }
        void finishArrow(){
            var greed=arrowWitnesses.get(0);var ally=arrowWitnesses.get(1);var role=arrowWitnesses.get(2);var victim=arrowWitnesses.get(3);
            require((!victim.isAlive()||victim.isRemoved())&&victim.getKillCredit()==owner,"precise native White Arrow pierces wall and target MAG with actual owner credit");
            require(greed.isAlive()&&greed.getHealth()==100&&ally.isAlive()&&ally.getHealth()==100&&role.isAlive()&&role.getHealth()==100,"White Arrow preserves Greed, cast-team ally and protected role");
            require(arrowChef.isRemoved()&&!game.items().wtf().snapshot().contains(arrowChef),"same native White Arrow permanently erases Chef authority");
            for(var cow:arrowWitnesses){game.server().getScoreboard().removePlayerFromTeam(cow.getScoreboardName());game.items().indirectKills().clear(cow);cow.discard();}removeFields();game.items().whiteArrows().stop();move(dirterOrigin);launchTeleport();move(padPosition);pause();remaining=delay().remaining();next(0);
        }
        void prepareDirterFootprint(){var center=BlockPos.containing(dirterOrigin);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=0;y<=57;y++)remember(new BlockPos(center.getX()+x,y,center.getZ()+z));dirterPlaced=center.east(2);set(dirterPlaced.below(),Blocks.STONE.defaultBlockState());set(dirterPlaced,Blocks.AIR.defaultBlockState());}
        void placeDirter(){
            field(false,dirterOrigin.add(6,0,0));field(true,dirterOrigin.add(7,0,0));require(game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"real MAG/TECH cover Dirter activation");
            var stack=leaf("dirter").copyWithCount(3);owner.setItemInHand(InteractionHand.OFF_HAND,stack);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));
            var hit=new BlockHitResult(Vec3.atCenterOf(dirterPlaced.below()).add(0,.5,0),Direction.UP,dirterPlaced.below(),false);
            require(stack.useOn(new UseOnContext(owner,InteractionHand.OFF_HAND,hit)).consumesAction()&&stack.getCount()==2&&owner.getMainHandItem().getCount()==7&&level.getBlockState(dirterPlaced).is(Blocks.COARSE_DIRT),"real Dirter BlockItem placement/sole offhand debit");
            require(game.items().imbaTerrainUtilities().views().stream().anyMatch(v->v.kind()==LbcImbaTerrainUtilities.Kind.DIRTER&&v.origin().equals(dirterOrigin)),"accepted Dirter uses exact player-origin geometry");
        }
        LbcTeleportGadgets.ImbaDelay delay(){return game.items().teleportGadgets().imbaDelays().stream().filter(d->d.owner().equals(owner.getUUID())).findFirst().orElseThrow(()->new IllegalStateException("accepted IMBA delay missing"));}
        void launchTeleport(){
            owner.setItemInHand(InteractionHand.MAIN_HAND,leaf("teleporter_imba").copyWithCount(3));owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STICK,7));var used=owner.getMainHandItem();var from=owner.position();var destination=from.add(owner.getLookAngle().scale(1488));int resistance=game.items().mana().hudValue(owner,"void_resistance");
            var tech=field(true,from.add(6,0,0));require(game.items().magic().mechanismSuppressedAt(level,from,24),"actual TECH24 launch control");require(!owner.gameMode.useItem(owner,level,used,InteractionHand.MAIN_HAND).consumesAction()&&used.getCount()==3&&owner.position().equals(from)&&game.items().teleportGadgets().pendingImba()==0,"TECH24 rejects teleport with no debit or pending output");removeField(tech);
            var mag=field(false,from.add(6,0,0));require(game.items().magic().suppressed(owner),"actual MAG teleport positive control");
            require(owner.gameMode.useItem(owner,level,used,InteractionHand.MAIN_HAND).consumesAction()&&used.getCount()==2&&owner.getOffhandItem().getCount()==7,"actual native IMBA teleporter ignores MAG and debits actual hand once");client.acknowledge();removeField(mag);
            require(owner.position().distanceToSqr(destination)<1e-8&&game.items().mana().hudValue(owner,"void_resistance")==resistance+30&&owner.hasEffect(MobEffects.SLOW_FALLING)&&owner.getEffect(MobEffects.SLOW_FALLING).getDuration()==600,"source1488 forward teleport/void30/slowfall600");require(delay().remaining()==20,"accepted source20 active-tick pad delay");
            move(dirterOrigin);padOrigin=LbcTeleportGadgets.imbaPadOrigin(padPosition);for(int x=0;x<5;x++)for(int z=0;z<5;z++)remember(padOrigin.offset(x,0,z));
            require(LbcTeleportGadgets.imbaPadOrigin(new Vec3(-.1,1,-.1)).equals(new BlockPos(-3,318,-3))&&LbcTeleportGadgets.imbaPadOrigin(new Vec3(-.1,321,-.1)).equals(new BlockPos(-3,318,-3)),"source floor-negative coordinates and low/highY320 clamp");
        }
        void assertPad(){for(int x=0;x<5;x++)for(int z=0;z<5;z++)require(level.getBlockState(padOrigin.offset(x,0,z)).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE)),"exact source5x1x5 current-position pad");require(owner.position().distanceToSqr(padPosition)<1e-8,"delayed pad never teleports owner a second time");}
        void assertDirter(){var center=BlockPos.containing(dirterOrigin);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=0;y<=57;y++){var expected=y==57?Blocks.GRASS_BLOCK:y>=53?Blocks.DIRT:y>=50?Blocks.STONE:y>=1?Blocks.DEEPSLATE:Blocks.BEDROCK;var at=new BlockPos(center.getX()+x,y,center.getZ()+z);var actual=level.getBlockState(at);boolean shadedSourceGrass=y==57&&at.equals(dirterPlaced.below())&&level.getBlockState(at.above()).is(Blocks.COARSE_DIRT)&&actual.is(Blocks.DIRT);require(actual.is(expected)||shadedSourceGrass,"source complete9x9 ordered strata "+at+" expected="+expected+" actual="+actual);}}
        void beginBastion(){
            move(dirterOrigin.add(40,0,0));var clicked=owner.blockPosition().east(2).below();set(clicked,Blocks.STONE.defaultBlockState());set(clicked.above(),Blocks.AIR.defaultBlockState());
            field(false,owner.position().add(12,0,0));field(true,owner.position().add(13,0,0));require(game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"real MAG/TECH cover Bastion activation");
            owner.setItemInHand(InteractionHand.MAIN_HAND,leaf("bedrock_bastion").copyWithCount(3));owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STICK,7));var used=owner.getMainHandItem();var hit=new BlockHitResult(Vec3.atCenterOf(clicked).add(0,.5,0),Direction.UP,clicked,false);
            require(used.useOn(new UseOnContext(owner,InteractionHand.MAIN_HAND,hit)).consumesAction()&&used.getCount()==2&&owner.getOffhandItem().getCount()==7,"actual Bastion egg useOn/one debit/no vanilla fallback");pause();
            var view=game.items().imbaTerrainUtilities().views().stream().filter(v->v.kind()==LbcImbaTerrainUtilities.Kind.BASTION).findFirst().orElseThrow();bastionActor=view.origin();require(bastionActor.equals(new Vec3(clicked.getX()+.5,clicked.getY()+1,clicked.getZ()+.5)),"source zero-size marker's exact native collision offset");bastionOrigin=BlockPos.containing(bastionActor.add(-8,-1,-8));next(4);
        }
        void readBastion(){
            try(var in=ImbaUtilityChecks.class.getResourceAsStream("/data/lbc/structure/bedrock_bastion.nbt")){
                require(in!=null,"exact existing Bastion source template resource");var tag=NbtIo.readCompressed(in,NbtAccounter.create(2*1024*1024));var palette=tag.getListOrEmpty("palette");var registry=game.server().registryAccess().lookupOrThrow(Registries.BLOCK);
                for(var raw:tag.getListOrEmpty("blocks")){var row=(CompoundTag)raw;var p=row.getListOrEmpty("pos");var at=bastionOrigin.offset(p.getIntOr(0,0),p.getIntOr(1,0),p.getIntOr(2,0));var state=legacyBastionState((CompoundTag)palette.get(row.getIntOr("state",0)));if(!state.is(Blocks.STRUCTURE_VOID)){remember(at);bastionExpected.put(at,state);}}
                require(bastionExpected.size()==4913&&game.items().imbaTerrainUtilities().templateCells()==4913,"all4913 exactsource BEDROCK/AIR cells");
                var decoded=new LinkedHashMap<BlockPos,BlockState>();
                for(var cell:(List<?>)get(game.items().imbaTerrainUtilities(),"bastion"))decoded.put(bastionOrigin.offset((BlockPos)get(cell,"offset")),((MapManager.BlockSnapshot)get(cell,"snapshot")).state());
                for(var entry:bastionExpected.entrySet())require(decoded.get(entry.getKey())==entry.getValue(),"prepared source Bastion cell "+entry.getKey()+" wanted="+entry.getValue()+" decoded="+decoded.get(entry.getKey()));
            }catch(java.io.IOException error){throw new IllegalStateException("Read finite Bastion source fixture",error);}
        }
        BlockState legacyBastionState(CompoundTag palette){
            // The independent 1.21 source uses Name; 26.3 NbtUtils only reads the data-fixed id.
            require(palette.getCompoundOrEmpty("Properties").isEmpty(),"exact Bastion source palette has no properties");
            return switch(palette.getStringOr("Name",palette.getStringOr("id",""))){
                case "minecraft:bedrock"->Blocks.BEDROCK.defaultBlockState();case "minecraft:air"->Blocks.AIR.defaultBlockState();
                default->throw new IllegalStateException("Unexpected exact Bastion source palette "+palette);
            };
        }
        Object bastionJob(){for(var job:((Map<?,?>)get(game.items().imbaTerrainUtilities(),"jobs")).values())if(get(job,"kind")==LbcImbaTerrainUtilities.Kind.BASTION)return job;throw new IllegalStateException("accepted Bastion job missing");}
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<1500,"bounded native utility work phase="+phase+" terrain="+game.items().imbaTerrainUtilities().state());
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(rewind)return replayStep();
            if(phase==-11){
                // Nested native commands finish after the command that started this asynchronous fixture.
                require(game.allowDamage(doomed)&&game.canUseGameplayItems(doomed),"canonical command enrolls actual lethal witness");
                doomed.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                roll(doomed,1,InteractionHand.OFF_HAND,false);require(!doomed.isAlive()||doomed.isRemoved(),"source1 kills actual non-Greed native participant");
                disconnectDoom();beginArrow();return false;
            }
            if(phase==-10){if(game.server().getTickCount()-when<8)return false;require(game.items().whiteArrows().state().steps()==arrowSteps&&game.items().whiteArrows().views().stream().allMatch(v->v.age()==0),"TimeStop pauses accepted White Arrow rays");thaw();next(-9);return false;}
            if(phase==-9){if(game.items().whiteArrows().state().active()!=0)return false;require(game.items().whiteArrows().state().steps()==arrowSteps+40,"two source rays each advance20 native active ticks");finishArrow();return false;}
            if(phase==0){if(game.server().getTickCount()-when<8)return false;require(delay().remaining()==remaining&&game.items().teleportGadgets().imbaPads().isEmpty(),"TimeStop holds exact accepted pad delay");thaw();next(1);return false;}
            if(phase==1){var delays=game.items().teleportGadgets().imbaDelays();int now=delays.isEmpty()?0:delay().remaining();require(now==remaining||now==remaining-1,"delay decrements at most once per active native tick");if(now<remaining)activeTicks++;remaining=now;if(now>0){require(!level.getBlockState(padOrigin).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE)),"no early pad before20 active ticks");return false;}require(activeTicks==20,"exact20 active ticks before pad admission");if(!game.items().teleportGadgets().imbaPads().isEmpty())return false;assertPad();move(dirterOrigin);placeDirter();next(2);return false;}
            if(phase==2){if(game.items().imbaTerrainUtilities().state().jobs()!=0)return false;assertDirter();require(game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"accepted Dirter completes under real MAG/TECH");removeFields();beginBastion();return false;}
            if(phase==4){var bounds=game.items().imbaTerrainUtilities().bastionBounds(bastionActor);boolean ready=true;var job=bastionJob();for(int x=bounds.min().getX()>>4;x<=bounds.max().getX()>>4;x++)for(int z=bounds.min().getZ()>>4;z<=bounds.max().getZ()>>4;z++)ready&=(Boolean)call(job,"resident",new BlockPos(x*16,58,z*16));if(!ready)return false;require(game.items().imbaTerrainUtilities().views().stream().allMatch(v->v.writes()==0),"paused accepted Bastion plans no writes");readBastion();thaw();next(5);return false;}
            if(phase==5){if(game.items().imbaTerrainUtilities().state().jobs()!=0)return false;for(var entry:bastionExpected.entrySet())require(level.getBlockState(entry.getKey())==entry.getValue(),"exact source Bastion cell atoffset(-8,-1,-8): "+entry.getKey()+" wanted="+entry.getValue()+" actual="+level.getBlockState(entry.getKey())+" metrics="+game.items().imbaTerrainUtilities().state());noFallbackActors();return true;}return false;
        }
        boolean replayStep(){
            if(phase==10){var jobs=game.items().imbaTerrainUtilities().views();if(jobs.isEmpty()||jobs.stream().noneMatch(v->v.writes()>0))return false;require(game.items().teleportGadgets().pendingImba()==1&&!game.items().whiteArrows().views().isEmpty(),"actual unfinished delay and White Arrow overlap partial accepted terrain");removeFields();pause();require(game.vanity().requestCapture(owner,false),"canonical coordinator accepts nonempty delay/terrain/ray cursor");next(11);return false;}
            if(phase==11){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;capturedJobs=game.items().imbaTerrainUtilities().views();capturedDelays=game.items().teleportGadgets().imbaDelays();capturedRays=game.items().whiteArrows().views();expectedRandom=owner.getRandom().nextLong();changed=BlockPos.containing(dirterOrigin).below();capturedBlock=level.getBlockState(changed);level.setBlock(changed,Blocks.GOLD_BLOCK.defaultBlockState(),2);owner.getAttribute(Attributes.ARMOR).setBaseValue(777);owner.getInventory().clearContent();game.items().whiteArrows().stop();
                require(game.vanity().requestRestoreForCheck(),"actual whole coordinator restore of typed utility work");restoring=true;next(12);return false;}
            if(phase==12){if(game.vanity().busy())return false;require(game.items().imbaTerrainUtilities().views().equals(capturedJobs)&&game.items().teleportGadgets().imbaDelays().equals(capturedDelays)&&game.items().whiteArrows().views().equals(capturedRays),"exact accepted terrain cursor, active delay and partial ray restored");require(level.getBlockState(changed)==capturedBlock&&owner.getAttribute(Attributes.ARMOR).getBaseValue()!=777&&owner.getRandom().nextLong()==expectedRandom,"whole player/world/native RNG restored together");require(game.items().timeStop().stopped(level),"captured TimeStop restored");game.vanity().stop();thaw();next(13);return false;}
            if(phase==13){if(game.items().whiteArrows().state().active()!=0||game.items().imbaTerrainUtilities().state().jobs()!=0||game.items().teleportGadgets().pendingImba()!=0||!game.items().teleportGadgets().imbaPads().isEmpty())return false;assertPad();assertDirter();noFallbackActors();return true;}return false;
        }
        void noFallbackActors(){for(var e:level.getAllEntities())if(!existing.contains(e.getUUID()))require(e.getType()!=EntityTypes.SNOWBALL&&e.getType()!=EntityTypes.PANDA,"no stray vanilla snowball/panda fallback actor");}
        public void close(){if(closed)return;closed=true;Throwable failure=null;game.server().tickRateManager().setFrozen(true);
            for(Runnable action:List.<Runnable>of(()->{if(rewind)game.vanity().stop();game.items().imbaTerrainUtilities().stop();game.items().teleportGadgets().stop();game.items().whiteArrows().stop();removeFields();disconnectDoom();if(stopped)thaw();},()->terrain.forEach((at,s)->{game.maps().restoreSnapshot(level,at,s,2|16|32);game.items().magic().blockChanged(at,level.getBlockState(at));}),()->{var actors=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!(e instanceof ServerPlayer)&&!existing.contains(e.getUUID()))actors.add(e);for(var e:actors){game.items().spellSources().unloaded(e);game.items().magic().unloaded(e);e.discard();}},()->{saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();core.restoreFrozen();playerRandom.restore(owner.getRandom());game.players().save();},client::close,audience::close,attribution::close,()->game.server().tickRateManager().setFrozen(frozen)))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw new IllegalStateException("IMBA utility scoped cleanup",failure);
        }
    }
}
