package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Native input, exact attribute identities and the production resident sample queue. */
public final class ImbaRuneChecks {
    private ImbaRuneChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Imba rune: "+why);}
    private static final Map<LbcImbaRunes.Kind,String> TABLES=Map.ofEntries(
        Map.entry(LbcImbaRunes.Kind.FEHU,"lbc:item_defs/lbc/rune_fehu__07babb3b21ec"),Map.entry(LbcImbaRunes.Kind.URUZ,"lbc:item_defs/lbc/rune_uruz__86d2f9a9754f"),
        Map.entry(LbcImbaRunes.Kind.THURISAZ,"lbc:item_defs/lbc/rune_thurisaz__a1701c73e812"),Map.entry(LbcImbaRunes.Kind.ANSUZ,"lbc:item_defs/lbc/rune_ansuz__a935839a23b7"),
        Map.entry(LbcImbaRunes.Kind.RAIDO,"lbc:item_defs/lbc/rune_raido__38f7807f0c5f"),Map.entry(LbcImbaRunes.Kind.KENAZ,"lbc:item_defs/lbc/rune_kenaz__ee7387212597"),
        Map.entry(LbcImbaRunes.Kind.GIFU,"lbc:item_defs/lbc/rune_gifu__32e86d4413f1"),Map.entry(LbcImbaRunes.Kind.WUNJO,"lbc:item_defs/lbc/rune_wunjo__d4ad18b1f1e5"),
        Map.entry(LbcImbaRunes.Kind.HAGALAZ,"lbc:item_defs/lbc/rune_hagalaz__9d23135f3921"),Map.entry(LbcImbaRunes.Kind.NAUTIZ,"lbc:item_defs/lbc/rune_nautiz__9c5bd62461f9"));
    static ItemStack loot(GameController game,net.minecraft.server.level.ServerPlayer p,LbcImbaRunes.Kind kind){var result=game.lbc().rollTable(p.level(),p.position(),p,TABLES.get(kind));
        require(result.size()==1&&LbcImbaRunes.kind(result.getFirst())==kind,"real singleton original leaf "+kind);return result.getFirst();}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final net.minecraft.server.level.ServerLevel level;
        final AttributeMap attributes=new AttributeMap(DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Set<UUID> before=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final int carrotStat;Cow cow;Marker field;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);var p=base.owner;level=p.level();
            require(game.items().exasperato().pendingSearches()==0&&game.items().exasperato().pendingBlasts()==0,"exclusive native resident rays/blasts");
            for(var holder:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(holder);if(a!=null)attributes.getInstance(holder).replaceFrom(a);}
            carrotStat=p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK));p.level().getAllEntities().forEach(e->before.add(e.getUUID()));base.initialize();}
        ItemStack hold(LbcImbaRunes.Kind kind,InteractionHand hand){var p=base.owner;p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var stack=loot(game,p,kind);stack.setCount(2);p.setItemInHand(hand,stack);return stack;}
        boolean input(InteractionHand hand){return base.owner.gameMode.useItem(base.owner,base.owner.level(),base.owner.getItemInHand(hand),hand).consumesAction();}
        void drain(){for(int n=0;n<30&&game.items().exasperato().pendingSearches()>0;n++)game.items().exasperato().tick();require(game.items().exasperato().pendingSearches()==0,"bounded shared native queue settles");}
        void raw(){for(var k:LbcImbaRunes.Kind.values()){var stack=loot(game,base.owner,k);require(stack.is(Items.CARROT_ON_A_STICK)&&stack.getMaxStackSize()==64,"unchanged raw carrier/max stack "+k);
                var data=LbcFartus.data(stack);data.putInt(k.flag(),2);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));require(!LbcImbaRunes.matches(stack),"integer1 required "+k);
                data.putInt(k.flag(),1);data.putInt("spell",0);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));require(!LbcImbaRunes.matches(stack),"outer original spell selector required "+k);}
            var named=new ItemStack(Items.CARROT_ON_A_STICK);named.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("rune_uruz"));require(!LbcImbaRunes.matches(named),"names do not admit");}
        void guards(){var p=base.owner;field=new Marker(EntityTypes.MARKER,p.level());field.setPos(p.position().add(1,0,0));field.entityTags().add("stopper_magic");require(p.level().addFreshEntity(field),"real beacon marker");game.items().magic().loaded(field);
            try{for(var kind:LbcImbaRunes.Kind.values())for(var hand:InteractionHand.values()){var held=hold(kind,hand);input(hand);require(held.getCount()==2&&game.items().exasperato().pendingSearches()==0,"actual beacon prevents cast/debit "+kind+"/"+hand);}}
            finally{game.items().magic().unloaded(field);field.discard();field=null;}
            p.getInventory().setItem(8,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            require(game.items().magic().suppressed(p),"actual stronger artifact field active");
            try{for(var kind:LbcImbaRunes.Kind.values())for(var hand:InteractionHand.values()){var held=hold(kind,hand);input(hand);require(held.getCount()==2&&game.items().exasperato().pendingSearches()==0,"user stronger artifact suppresses all runes "+kind+"/"+hand);}}
            finally{p.getInventory().setItem(8,ItemStack.EMPTY);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}}
        void immediate(){var p=base.owner;require(p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,p.getBoundingBox().inflate(50),e->e!=p).isEmpty(),"giant fixture owns its native target cohort");
            cow=new Cow(EntityTypes.COW,p.level());cow.setPos(p.position().add(3,0,0));cow.setNoAi(true);cow.setNoGravity(true);require(p.level().addFreshEntity(cow),"native smaller target");
            for(var hand:InteractionHand.values()){
                p.getAttributes().assignAllValues(attributes);var held=hold(LbcImbaRunes.Kind.THURISAZ,hand);require(input(hand)&&held.getCount()==1,"actual giant used hand one debit "+hand);
                require(p.getAttribute(Attributes.SCALE).getModifier(LbcImbaRunes.POSITIVE).amount()==1&&p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getModifier(LbcImbaRunes.POSITIVE).amount()==2,"original positive values and ID");
                require(p.getAttribute(Attributes.ATTACK_KNOCKBACK).getModifier(LbcImbaRunes.POSITIVE).operation()==AttributeModifier.Operation.ADD_VALUE,"sole additive modifier");
                require(cow.getAttribute(Attributes.SCALE).getModifier(LbcImbaRunes.NEGATIVE).amount()==-.9375&&cow.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(LbcImbaRunes.NEGATIVE).amount()==-.8&&cow.entityTags().contains("submissive"),"original native hostile modifiers/identity/tag");
                held=hold(LbcImbaRunes.Kind.THURISAZ,hand);require(input(hand)&&held.getCount()==1,"repeat rune debits once with existing IDs");require(p.getAttribute(Attributes.SCALE).getModifier(LbcImbaRunes.POSITIVE).amount()==1,"repeat does not multiply modifier identity");
                held=hold(LbcImbaRunes.Kind.URUZ,hand);require(input(hand)&&held.getCount()==1,"actual Uruz hand one debit "+hand);require(p.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==1e100&&p.getEffect(MobEffects.RESISTANCE).isInfiniteDuration()&&p.getEffect(MobEffects.RESISTANCE).getAmplifier()==4,"native original enormous damage/infinite ResistanceV");
                held=hold(LbcImbaRunes.Kind.ANSUZ,hand);require(input(hand)&&held.getCount()==1&&p.isCreative(),"native Creative transition/one debit "+hand);
            }LbcImbaRunes.reset(p);cow.discard();cow=null;p.getAttributes().assignAllValues(attributes);p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);}
        void samples(){var p=base.owner;p.setXRot(-90);p.setYRot(0);
            for(var kind:List.of(LbcImbaRunes.Kind.FEHU,LbcImbaRunes.Kind.GIFU))for(var hand:InteractionHand.values()){
                var held=hold(kind,hand);require(input(hand)&&game.items().exasperato().pendingSearches()==1,"real raw source ray queued "+kind+"/"+hand);drain();require(held.getCount()==2,"finite empty-sky miss has no terminal debit "+kind);}
            for(double step:new double[]{1,.33}){var points=new ArrayList<Vec3>();var start=p.getEyePosition();
                game.items().exasperato().replayableSamples(p,start,new Vec3(0,1,0),200,new LbcExasperato.ReplayableRay(){
                    public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return false;}public boolean target(Entity e){return false;}public void trail(Vec3 a,Vec3 b){}
                    public double sampleStep(){return step;}public HitResult sampleAt(Vec3 at){points.add(at);return null;}public void complete(HitResult hit){require(hit==null,"finite sample miss stays null");}});
                drain();require(points.size()==201,"independent original recursive201 sample count step="+step);for(int n=0;n<points.size();n++)require(points.get(n).distanceTo(start.add(0,n*step,0))<1e-8,"exact queue sample cursor "+n);}
            var block=BlockPos.containing(p.getEyePosition().add(0,4,0));terrain.put(block,game.maps().snapshot(p.level(),block));p.level().setBlock(block,Blocks.STONE.defaultBlockState(),3);
            var held=hold(LbcImbaRunes.Kind.GIFU,InteractionHand.MAIN_HAND);require(input(InteractionHand.MAIN_HAND),"actual Gifu block terminal");drain();require(held.getCount()==1,"terminal debits once");
            var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();for(var e:p.level().getAllEntities())if(!before.contains(e.getUUID())&&e instanceof net.minecraft.world.entity.item.ItemEntity item)drops.add(item);
            require(!drops.isEmpty()&&drops.stream().allMatch(e->e.getY()<block.getY()&&e.getY()>block.getY()-1),"native loot at source last0.33 sample before block");drops.forEach(Entity::discard);
            held=hold(LbcImbaRunes.Kind.GIFU,InteractionHand.OFF_HAND);require(input(InteractionHand.OFF_HAND),"actual offhand queued terminal");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);drain();require(held.getCount()==2,"lost accepted physical stack cancels terminal debit");}
        @Override public void close(){if(field!=null){game.items().magic().unloaded(field);field.discard();}drain();if(cow!=null)cow.discard();
            for(var e:new ArrayList<>(pEntities()))if(!before.contains(e.getUUID())){game.items().entityUnloaded(e);e.discard();}
            terrain.forEach((pos,snapshot)->game.maps().restoreSnapshot(level,pos,snapshot));base.owner.getAttributes().assignAllValues(attributes);
            try{base.close();}finally{base.owner.getAttributes().assignAllValues(attributes);base.owner.setHealth(base.saved.health);base.owner.setAbsorptionAmount(base.saved.absorption);base.owner.getStats().setValue(base.owner,net.minecraft.stats.Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK),carrotStat);}}
        List<Entity> pEntities(){var out=new ArrayList<Entity>();base.owner.level().getAllEntities().forEach(out::add);return out;}
    }
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.raw();fixture.guards();fixture.immediate();fixture.samples();}
        SkyWars2.LOGGER.info("[check] PASS imba-runes (ten real raw leaves/beacon guards; both native hands URUZ/THURISAZ/ANSUZ; native modifiers;201 original sample positions; empty-sky no debit, Gifu actual terminal loot/single debit/physical-stack cancellation; scoped restore)");}
    private static GlobalFixture globals;
    /** Global payloads borrow the actual full-world coordinator, including native entity replacement. */
    private static final class GlobalFixture implements AutoCloseable {
        final GameController game;final MatchCoreCheckpoint originalCore;final List<MatchPlayerCheckpoint> originalPlayers;
        final Fixture base;final net.minecraft.server.level.ServerLevel home;final BlockPos clearProbe,rayBlock,galleryOrigin;
        final net.minecraft.core.Vec3i gallerySize;final UUID witness,item;final int initialLives,galleryAirBaseline;
        final List<LbcImbaRunes.Kind> kinds=List.of(LbcImbaRunes.Kind.WUNJO,LbcImbaRunes.Kind.RAIDO,LbcImbaRunes.Kind.KENAZ,LbcImbaRunes.Kind.FEHU,LbcImbaRunes.Kind.HAGALAZ);
        int phase,index,ticks,started;ItemStack held;boolean closed;
        GlobalFixture(GameController game){this.game=game;require(!game.vanity().hasSnapshot()&&!game.vanity().busy(),"exclusive full-state savepoint");
            var gallery=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:item_gallery"))
                    .orElseThrow(()->new IllegalStateException("Packaged native item_gallery is required before borrowing fixture state"));
            originalCore=new MatchCoreCheckpoint(game);originalPlayers=game.server().getPlayerList().getPlayers().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();
            base=new Fixture(game);var p=base.base.owner;home=p.level();initialLives=game.lives(p.getUUID());p.setXRot(-90);p.setYRot(0);
            clearProbe=new BlockPos(0,140,0);home.getChunkAt(clearProbe);base.terrain.put(clearProbe,game.maps().snapshot(home,clearProbe));home.setBlock(clearProbe,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
            rayBlock=BlockPos.containing(p.getEyePosition().add(0,4,0));base.terrain.put(rayBlock,game.maps().snapshot(home,rayBlock));home.setBlock(rayBlock,Blocks.STONE.defaultBlockState(),3);
            galleryOrigin=BlockPos.containing(new Vec3(p.getEyePosition().x,p.getEyePosition().y+4,p.getEyePosition().z).add(-19,0,-19));
            gallerySize=gallery.getSize();
            for(int x=galleryOrigin.getX()>>4;x<=(galleryOrigin.getX()+gallerySize.getX())>>4;x++)for(int z=galleryOrigin.getZ()>>4;z<=(galleryOrigin.getZ()+gallerySize.getZ())>>4;z++)home.getChunk(x,z);
            galleryAirBaseline=nonAir();var cow=new Cow(EntityTypes.COW,home);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(p.position().add(4,0,0));require(home.addFreshEntity(cow),"native global dimension witness");witness=cow.getUUID();
            var drop=new net.minecraft.world.entity.item.ItemEntity(home,0,141,0,new ItemStack(Items.EMERALD));drop.setNoGravity(true);drop.setUnlimitedLifetime();require(home.addFreshEntity(drop),"native map-clear item witness");item=drop.getUUID();
            require(game.vanity().requestCapture(p,false),"real full-state capture request");}
        int nonAir(){int count=0;for(var pos:BlockPos.betweenClosed(galleryOrigin,galleryOrigin.offset(gallerySize.getX()-1,gallerySize.getY()-1,gallerySize.getZ()-1)))if(!home.getBlockState(pos).isAir())count++;return count;}
        void ack(){for(var player:game.server().getPlayerList().getPlayers())
            ((MatchPlayerCheckpoint.SimulatedAcknowledgement)player.connection).skywars2$acknowledgeRestore();}
        boolean step(){require(++ticks<1500,"bounded real global coordinator "+game.vanity().status()+" phase="+phase+" index="+index);ack();var p=base.base.owner;
            if(phase==0){if(!game.vanity().hasSnapshot()||game.vanity().busy()||!game.vanity().status().equals("IDLE"))return false;phase=1;}
            if(phase==1){var kind=kinds.get(index);held=base.hold(kind,index%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);p.setXRot(-90);p.setYRot(0);
                require(base.input(index%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND),"actual global native use "+kind);started=game.server().getTickCount();phase=2;
                if(kind==LbcImbaRunes.Kind.RAIDO){require(game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("bottom_nexus"),"lbc.math").get()==1,"canonical bottom-Nexus activation score");p.teleportTo(home,p.getX(),-101,p.getZ(),TeleportMomentum.positionFlags(Set.of()),0,0,true);}
                return false;}
            if(phase==2){var kind=kinds.get(index);if(game.maps().busy()||game.items().exasperato().pendingSearches()!=0)return false;
                if(kind==LbcImbaRunes.Kind.HAGALAZ&&(!home.getBlockState(clearProbe).isAir()||!game.items().stonekivi().captureSettled()))return false;
                require(held.getCount()==1,"single actual global terminal debit "+kind);
                switch(kind){
                    case WUNJO->require(game.lives(p.getUUID())==initialLives+16&&home.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY),"real participant sixteen lives/global keepInventory");
                    case RAIDO->{if(game.server().getTickCount()-started<25&&p.level()==home)return false;require(p.level().dimension().equals(LbcHedgehogDimensions.NEXUS)&&p.getY()==2332&&Math.abs(p.getX())<=4800&&Math.abs(p.getZ())<=4800,"actual once-per-second shared Nexus RTP");}
                    case KENAZ->{var nether=game.server().getLevel(net.minecraft.world.level.Level.NETHER);
                        if(nether!=null&&(nether.getEntity(witness)==null||home.getEntity(witness)!=null)&&game.server().getTickCount()-started<100)return false;
                        require(nether!=null&&nether.getEntity(witness)!=null&&home.getEntity(witness)==null,"actual native Nether entity transfer after native chunk publication");}
                    case FEHU->require(nonAir()>galleryAirBaseline+10,"actual original gallery native terrain placed");
                    case HAGALAZ->{require(home.getBlockState(clearProbe).isAir()&&(home.getEntity(item)==null||home.getEntity(item).isRemoved()),"actual map clear and native item purge");}
                    default->throw new IllegalStateException("global fixture kind "+kind);
                }
                require(game.vanity().requestRestoreForCheck(),"actual full-state global rewind "+kind);phase=3;return false;}
            if(phase==3){if(game.vanity().busy()||!game.vanity().status().equals("IDLE"))return false;
                require(p.level()==home&&game.lives(p.getUUID())==initialLives&&home.getBlockState(clearProbe).is(Blocks.DIAMOND_BLOCK)&&home.getEntity(witness)!=null&&home.getEntity(item)!=null,"coordinator restores native bodies/world/core after global rune");
                if(++index==kinds.size())return true;phase=1;return false;}
            return false;}
        @Override public void close(){if(closed)return;closed=true;game.vanity().stop();boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{base.close();originalCore.restoreFrozen();for(var player:originalPlayers)player.restoreFrozen();}finally{game.server().tickRateManager().setFrozen(frozen);}}
    }
    public static void startGlobals(GameController game){require(globals==null,"exclusive globals fixture");globals=new GlobalFixture(game);}
    public static boolean tickGlobals(GameController game){if(globals==null)return true;try{if(!globals.step())return false;globals.close();globals=null;
        SkyWars2.LOGGER.info("[check] PASS imba-runes-globals (real Wunjo lives/keepInventory, Raido source score/native Nexus RTP, Kenaz native Nether transfer, Fehu original gallery, Hagalaz map/item clear; actual full-world Vanity rewind between each; Nautiz restart requires separate epoch fixture)");return true;
        }catch(Throwable failure){try{globals.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{globals=null;}SkyWars2.LOGGER.error("[check] FAIL imba-runes-globals",failure);return true;}}
    public static void cancelGlobals(GameController game){if(globals!=null){try{globals.close();}finally{globals=null;}}}
    private static RestartFixture restart;
    /** Destructive final-match fixture: the disposable native harness owns restoring its server snapshot. */
    private static final class RestartFixture implements AutoCloseable {
        final GameController game;final UUID author;final String epoch;final ItemStack held;
        final net.minecraft.server.network.ServerGamePacketListenerImpl connection;final ElementalGrimoireChecks.Recording wire;
        int frames;boolean closed;
        RestartFixture(GameController game){this.game=game;require(globals==null&&!game.vanity().busy()&&game.playing()&&!game.maps().busy(),"final isolated actual playing match");
            var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(p!=null&&game.canUseGameplayItems(p)&&!game.items().magic().suppressed(p),"actual unsuppressed restart author");
            author=p.getUUID();epoch=game.items().epoch();connection=p.connection;wire=new ElementalGrimoireChecks.Recording(p);
            p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),wire,p,new net.minecraft.server.network.CommonListenerCookie(p.getGameProfile(),0,p.clientInformation(),false));
            p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setGameMode(GameType.SURVIVAL);
            held=loot(game,p,LbcImbaRunes.Kind.NAUTIZ);held.setCount(2);p.setItemInHand(InteractionHand.MAIN_HAND,held);
            require(p.gameMode.useItem(p,p.level(),held,InteractionHand.MAIN_HAND).consumesAction()&&held.getCount()==1,"actual Nautiz input and one native debit before restart");}
        boolean step(){require(++frames<2400,"bounded native controller restart/map preparation");var p=game.server().getPlayerList().getPlayer(author);require(p!=null,"restart preserves real participant connection");
            p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());var positions=wire.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).toList();
            if(!positions.isEmpty())p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(positions.getLast().id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
            if(game.items().currentEpoch(epoch)||game.maps().busy()||!game.playing())return false;
            require(game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.lives(author)==Math.max(1,game.config().lives)&&game.selectedMapReady(),"actual selected new match/reinitialized participant lives/ready native map");return true;}
        @Override public void close(){if(closed)return;closed=true;var p=game.server().getPlayerList().getPlayer(author);if(p!=null){p.connection=connection;p.connection.resetPosition();}}
    }
    public static void startRestart(GameController game){require(restart==null,"exclusive final restart fixture");restart=new RestartFixture(game);}
    public static boolean tickRestart(GameController game){if(restart==null)return true;try{if(!restart.step())return false;restart.close();restart=null;
        SkyWars2.LOGGER.info("[check] PASS imba-runes-restart (actual final-match Nautiz raw input/single debit; native controller map preparation/new epoch/active participant and reset lives; disposable harness owns original server snapshot)");return true;
        }catch(Throwable failure){try{restart.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{restart=null;}SkyWars2.LOGGER.error("[check] FAIL imba-runes-restart",failure);return true;}}
    public static void cancelRestart(GameController game){if(restart!=null){try{restart.close();}finally{restart=null;}}}
}
