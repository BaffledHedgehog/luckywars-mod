package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real coordinator capture/rewind across native ticks; no direct calls to component restore methods. */
public final class VanityRollbackChecks {
    private static Fixture running;
    private VanityRollbackChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Vanity coordinator: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ChefChecks.Fixture base;
        final MatchCoreCheckpoint originalCore;
        final List<MatchPlayerCheckpoint> originalPlayers;
        final ServerGamePacketListenerImpl alphaConnection,betaConnection;
        ElementalGrimoireChecks.Recording alphaWire,betaWire;
        Cow saved,future,morphed;
        net.minecraft.world.entity.animal.sheep.Sheep sheep;
        LbcVoidArcanum.Hole hole;
        LbcWtf.Chef chef;
        net.minecraft.world.entity.Marker wealthField;
        Vec3 holePoint;
        int constructionSteps,formRemaining,curseRemaining;
        ServerPlayer gamma,carpet;
        final String gammaName;
        int expectedGammaLives;
        ElementalGrimoireChecks.Recording gammaWire;
        BlockPos block;
        Vec3 spot;
        int phase=-1,ticks,round,offlineRecaptures,schedulePhase;
        long scheduledDeadline;
        VanityCheckpointSchedule.State savedSchedule;
        boolean closed;
        Fixture(GameController game){
            this.game=game;require(!game.vanity().hasSnapshot()&&!game.vanity().busy(),"exclusive save point");
            // A neighboring suite may own TestGamma for the entire batch. Keep its body and
            // connection untouched while this fixture deliberately logs its own holder out.
            gammaName=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"))==null?"TestGamma":"TestVanityGamma";
            require(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(gammaName))==null,"exclusive owned offline holder name");
            originalCore=new MatchCoreCheckpoint(game);
            originalPlayers=game.server().getPlayerList().getPlayers().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();
            base=new ChefChecks.Fixture(game);alphaConnection=base.owner.connection;betaConnection=base.other.connection;
        }
        ElementalGrimoireChecks.Recording wire(ServerPlayer player){
            var wire=new ElementalGrimoireChecks.Recording(player);
            player.connection=new ServerGamePacketListenerImpl(game.server(),wire,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());return wire;
        }
        void init(){
            base.init();alphaWire=wire(base.owner);betaWire=wire(base.other);
            require(game.items().voidArcanum().count()==0&&game.items().polymorph().count()==0,"exclusive long-lived source fixtures");
            try{
                var profile=new com.mojang.authlib.GameProfile(GameChecks.offlineUuid("TestVanityCarpet"),"TestVanityCarpet");
                var type=Class.forName("carpet.patches.EntityPlayerMPFake");
                carpet=(ServerPlayer)type.getMethod("respawnFake",net.minecraft.server.MinecraftServer.class,net.minecraft.server.level.ServerLevel.class,com.mojang.authlib.GameProfile.class,net.minecraft.server.level.ClientInformation.class)
                        .invoke(null,game.server(),base.owner.level(),profile,base.owner.clientInformation());
                var connection=(net.minecraft.network.Connection)Class.forName("carpet.patches.FakeClientConnection").getConstructor(net.minecraft.network.protocol.PacketFlow.class).newInstance(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
                game.server().getPlayerList().placeNewPlayer(connection,carpet,new CommonListenerCookie(profile,0,carpet.clientInformation(),false));
                require(game.returnFromGameModeCommand(carpet,net.minecraft.world.level.GameType.SURVIVAL),"actual Carpet fixture enrolls");
                carpet.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);carpet.setNoGravity(true);base.move(carpet,base.at.add(-8,3,0));
                carpet.hasChangedDimension();carpet.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                carpet.getInventory().setItem(6,new ItemStack(Items.AMETHYST_SHARD,7));
                carpet.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,game.lbc().rollTable(carpet.level(),carpet.position(),carpet,"lbc:neko_set_technomk2/boots").getFirst());
                carpet.setOnGround(false);carpet.setShiftKeyDown(true);game.items().mana().score(carpet,"reactivefuel").set(100);
                game.items().armorSets().techno().flight().tick(carpet);
                require(game.items().armorSets().techno().flight().flying(carpet),"actual source MK2 flight begins before capture");
            }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            gamma=GameChecks.createBot(game,gammaName);
            expectedGammaLives=Math.max(1,game.lives(gamma.getUUID()));
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival "+gammaName);
            base.move(gamma,base.at.add(8,3,0));gammaWire=wire(gamma);gamma.setNoGravity(true);

            base.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(LbcVoidArcanum.ITEM));
            game.items().mana().set(base.owner,100000);base.owner.entityTags().add("time_stopped");
            hole=game.items().voidArcanum().spawn(base.owner,base.at.add(0,8,0));require(hole!=null,"actual long-lived black-hole source");game.items().voidArcanum().updateVisual(hole,base.owner);
            morphed=base.cow(base.at.add(-5,2,4));sheep=game.items().polymorph().transform(morphed,game.items().indirectKills().coldCause(base.owner,false));
            require(sheep!=null,"actual temporary polymorph source");
            chef=base.spawn(base.owner,base.cow(base.at.add(4,2,5)));chef.entityTags().add("time_stopped");
            wealthField=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,base.owner.level());
            wealthField.setPos(base.at.add(0,4,0));wealthField.entityTags().addAll(List.of(LbcEternalWealth.TAG,LbcEternalWealth.TAG+".kind.FIELD",LbcEternalWealth.TAG+".age.0",LbcEternalWealth.TAG+".owner."+base.owner.getUUID(),LbcEternalWealth.TAG+".order.1","magic"));
            game.items().markCurrentItemEpoch(wealthField);base.owner.level().addFreshEntity(wealthField);
            require(game.items().eternalWealth().fields()==1,"nonempty functional cursed-field source before full capture");
            int began=game.server().getTickCount();
            game.items().stonekivi().queuedWork(new LbcStonekivi.Work(){public boolean ready(){return game.server().getTickCount()>=began+constructionSteps;}public boolean step(){
                base.block(base.center.offset(-8+constructionSteps,0,-4),Blocks.DIAMOND_BLOCK);return ++constructionSteps==12;
            }});
            for(var p:List.of(base.owner,base.other)){p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
            block=base.center.offset(0,0,4);base.block(block,Blocks.DIAMOND_BLOCK);
            spot=base.at.add(0,2,0);saved=base.cow(spot);saved.setHealth(16);
            base.owner.getInventory().setItem(2,new ItemStack(Items.DIAMOND,9));
            game.items().mana().set(base.owner,321);base.owner.entityTags().add("vanity_fixture");

            SkyWars2.LOGGER.info("[check] START vanity-rollback");
        }
        void ack(ServerPlayer player,ElementalGrimoireChecks.Recording wire){
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var positions=wire.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
            if(!positions.isEmpty())player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(positions.getLast().id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
        }
        void mutate(){
            gamma.entityTags().remove("vanity_schedule_point");
            game.items().armorSets().techno().flight().forget(carpet);
            base.block(block,Blocks.DIRT);saved.discard();future=base.cow(spot.add(3,0,0));
            for(int n=0;n<12;n++)base.block(base.center.offset(-8+n,0,-4),Blocks.DIRT);
            game.items().voidArcanum().forget(base.owner);game.items().polymorph().stop();
            chef.discard();game.items().wtf().unloaded(chef);game.items().eternalWealth().stop();
            if(gamma!=null&&!gamma.isRemoved()){game.items().clearTurnCurse(gamma);game.items().resetRocket(gamma);}
            base.owner.getInventory().setItem(2,new ItemStack(Items.IRON_INGOT,55));
            game.items().mana().set(base.owner,900);base.owner.entityTags().remove("vanity_fixture");
            require(game.vanity().requestRestoreForCheck(),"actual coordinator rewind request");
        }
        boolean step(){
            require(++ticks<500,"bounded coordinator phases: "+game.vanity().status());
            ack(base.owner,alphaWire);ack(base.other,betaWire);
            var live=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(gammaName));
            if(live!=null){gamma=live;ack(gamma,gammaWire);}
            if(phase==-1){
                require(game.allowDamage(gamma)&&!game.isObserver(gamma)&&game.lives(gamma.getUUID())==expectedGammaLives&&expectedGammaLives>0,"native operator enrollment completed before authority admission");
                base.move(gamma,base.at.add(8,3,0));gamma.setNoGravity(true);gamma.getInventory().setItem(4,new ItemStack(Items.EMERALD,13));
                try{var clock=GameController.class.getDeclaredField("gameTicks");clock.setAccessible(true);clock.setInt(game,12000);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                for(int n=0;n<100;n++)game.items().addPlayerCounters(gamma);
                gamma.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(LbcItems.ROCKET_BOOTS));
                game.items().rocketEquipment(gamma,gamma.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
                require(game.items().rocketGlide(gamma)&&game.items().rocketJumps(gamma)==2,"real rocket use creates a charged resident ability");
                gamma.setDeltaMovement(Vec3.ZERO);
                game.items().mana().score(gamma,"dmg_dealt").set(0);game.items().mana().score(gamma,"gens_type").set(5);
                gamma.getInventory().setSelectedSlot(0);gamma.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,game.lbc().rollTable(gamma.level(),gamma.position(),gamma,"lbc:witch_gens").getFirst().copyWithCount(2));
                gamma.setShiftKeyDown(false);game.items().magic().inventoryChanged(gamma);
                require(game.items().witchClaims().use(gamma,net.minecraft.world.InteractionHand.MAIN_HAND),"qualified source acquisition requests the actual save point");
                phase=0;return false;
            }
            if(phase==0){
                if(!game.vanity().hasSnapshot()||game.vanity().busy())return false;
                var clock=game.vanity().scheduleState();
                require(clock!=null,"successful source acquisition fixes the rolling checkpoint timeline");
                if(schedulePhase==0){
                    scheduledDeadline=clock.nextAt();
                    require(scheduledDeadline>=600&&scheduledDeadline<=12000,"first interval is 30..600 seconds");
                    gamma.entityTags().add("vanity_schedule_point");
                    game.vanity().restoreSchedule(new VanityCheckpointSchedule.State(clock.seed(),scheduledDeadline-1,clock.ordinal(),scheduledDeadline));
                    schedulePhase=1;return false;
                }
                if(clock.nextAt()<=scheduledDeadline)return false;
                savedSchedule=clock;
                require(gamma.entityTags().contains("sin_vanity")&&LbcWitchClaims.matches(gamma.getMainHandItem())&&gamma.getMainHandItem().getCount()==1,"exactly one source token consumed for the granted authority");
                require(gamma.getInventory().getItem(4).is(Items.EMERALD),"native point includes the prepared real-player inventory");
                require(constructionSteps==12,"accepted structure completes before capture instead of being abandoned");
                curseRemaining=game.items().curseTicks(gamma);require(curseRemaining>0,"live turn-curse source exists at capture");
                holePoint=hole.point;formRemaining=game.items().polymorph().remaining(morphed);
                mutate();phase=1;return false;
            }
            if(game.vanity().busy())return false;
            if(phase==3){
                if(!gamma.getInventory().getItem(4).is(Items.EMERALD))return false;
                require(game.items().curseTicks(gamma)>=curseRemaining-3&&game.items().rocketJumps(gamma)==2,"private curse and rocket state bind to the real reconnect body");
                require(gamma.getInventory().getItem(4).getCount()==13,"offline native player image applies on real login");
                require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000),"actual holder death enters native death handling");phase=4;return false;
            }
            if(phase==4){require(game.canUseGameplayItems(gamma)&&game.allowDamage(gamma)&&gamma.isAlive()&&gamma.getInventory().getItem(4).is(Items.EMERALD),"death requests real native respawn plus world rewind: alive="+gamma.isAlive()+" observer="+game.isObserver(gamma)+" allowed="+game.allowDamage(gamma)+" lives="+game.lives(gamma.getUUID())+" awaiting="+game.awaitingRespawn(gamma.getUUID())+" emerald="+gamma.getInventory().getItem(4));return true;}
            require(game.items().armorSets().techno().flight().flying(carpet),"saved sustained MK2 flight survives future cancellation and resumes");
            require(carpet.connection.hasClientLoaded()&&!carpet.isChangingDimension()&&carpet.getInventory().getItem(6).is(Items.AMETHYST_SHARD),"socketless real Carpet player completes restoration without a client ACK");
            if(phase==2&&offlineRecaptures>0){
                if(!game.vanity().status().startsWith("IDLE"))return false;
                formRemaining=game.items().polymorph().remaining(morphed);
                if(offlineRecaptures<2){require(game.vanity().requestCapture(base.owner,false),"second replacement point with a genuinely absent player");offlineRecaptures++;return false;}
                gamma=GameChecks.createBot(game,gammaName);gammaWire=wire(gamma);phase=3;return false;
            }
            require(!game.server().tickRateManager().isFrozen(),"native tick rate restored");
            var restoredClock=game.vanity().scheduleState();
            require(restoredClock.seed()==savedSchedule.seed()&&restoredClock.ordinal()==savedSchedule.ordinal()
                    &&restoredClock.nextAt()==savedSchedule.nextAt(),"rewinds retain the acquisition-fixed future schedule");
            if(phase!=2)require(gamma.entityTags().contains("vanity_schedule_point"),"death returns to the newer automatic point, not the acquisition point");
            require(base.owner.level().getBlockState(block).is(Blocks.DIAMOND_BLOCK),"saved terrain returns through actual coordinator");
            require(!saved.isRemoved()&&saved.getHealth()==16&&saved.position().distanceToSqr(spot)<.001,"same native actor returns with its saved HP and position");
            require(future.isRemoved(),"future native actor removed");
            require(!wealthField.isRemoved()&&game.items().eternalWealth().fields()==1,"native cursed-field body and typed owner resume through actual full coordinator");
            var stack=base.owner.getInventory().getItem(2);
            require(stack.is(Items.DIAMOND)&&stack.getCount()==9,"native player inventory rewound");
            require(base.owner.entityTags().contains("vanity_fixture"),"native player tags rewound");
            require(hole.visual!=null&&game.items().blackHoleVisuals().count()>0,"black-hole display holder is republished after actual rewind");
            require(game.items().voidArcanum().snapshot().contains(hole)&&!hole.marker.isRemoved()&&hole.point.distanceToSqr(holePoint)<.001,"same functional black-hole owner and body return");
            require(game.items().polymorph().active(morphed)&&!sheep.isRemoved()&&game.items().polymorph().remaining(morphed)>=formRemaining-2,"temporary form resumes its saved duration");
            require(game.items().wtf().snapshot().contains(chef)&&!chef.isRemoved(),"same Chef returns with its private owner state");
            for(int n=0;n<12;n++)require(base.owner.level().getBlockState(base.center.offset(-8+n,0,-4)).is(Blocks.DIAMOND_BLOCK),"entire accepted structure belongs to the point");
            if(++round<2){mutate();return false;}
            if(phase==1){
                gamma.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("Vanity offline fixture")));
                mutate();phase=2;return false;
            }
            require(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(gammaName))==null,"offline player is never simulated by a fake body");
            require(game.vanity().requestCapture(base.owner,false),"replacement point retains offline player data and resident owners");offlineRecaptures=1;return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;game.vanity().stop();boolean frozen=game.server().tickRateManager().isFrozen();
            game.server().tickRateManager().setFrozen(true);
            try{
                if(carpet!=null&&!carpet.isRemoved())carpet.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("Vanity Carpet fixture cleanup")));
                var live=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(gammaName));
                if(live!=null)live.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("Vanity fixture cleanup")));
                game.items().polymorph().stop();game.items().voidArcanum().forget(base.owner);game.items().eternalWealth().stop();
                base.owner.entityTags().remove("time_stopped");base.owner.entityTags().remove("vanity_fixture");base.owner.connection=alphaConnection;base.other.connection=betaConnection;
                base.close();originalCore.restoreFrozen();for(var player:originalPlayers)player.restoreFrozen();
            }finally{game.server().tickRateManager().setFrozen(frozen);}
        }
    }
    public static void run(GameController game){
        require(running==null,"exclusive fixture");running=new Fixture(game);
        try{running.init();}catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;throw failure;}
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-rollback (production end-tick coordinator, completed construction, repeated black-hole/polymorph/Chef/terrain/actor/player replay, offline real login, native holder death and respawn, packet/load acknowledgements, qualified one-token source acquisition, actual Carpet player restoration, private curse/rocket/MK2 flight restoration, repeated offline point replacement and online-role reconciliation)");}
        catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-rollback",failure);}
    }
}
