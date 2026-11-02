package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Real scheduled ten-active-tick charges; never manually advances production or world ticks. */
public final class TeleportGadgetChecks {
    private TeleportGadgetChecks(){}
    private static State running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Teleport gadgets: "+why);}
    private static void near(Vec3 a,Vec3 b,String why){require(a.distanceToSqr(b)<1e-10,why+" actual="+a+" expected="+b);}
    private static boolean key(Component c){return c.getContents() instanceof TranslatableContents t&&t.getKey().equals("lbc.swapper.charge_failed")||c.getSiblings().stream().anyMatch(TeleportGadgetChecks::key);}
    private static final class State implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final ServerPlayer p,beta;
        final LbcTeleportGadgets service;final RegenerationBeltChecks.Snapshot saved,betaSaved;
        final RegenerationBeltChecks.ScheduledClient client,audience;final List<net.minecraft.network.protocol.Packet<?>> ownerPackets,betaPackets;final Vec3 origin,target;
        final List<Marker> fields=new ArrayList<>();final Map<Marker,Map<BlockPos,MapManager.BlockSnapshot>> fieldCells=new IdentityHashMap<>();final Map<ServerPlayer,Set<String>> tags=new IdentityHashMap<>();
        final Map<net.minecraft.world.item.Item,Integer> stats=new IdentityHashMap<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final MatchRandomState random,ownerRandom,betaRandom;
        final net.minecraft.world.scores.PlayerTeam oldTeam,oldBetaTeam,team;
        ItemStack stack;int phase,started,sequence;boolean closed,paused;
        State(GameController game){this.game=game;p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(p!=null&&beta!=null&&p.level()==beta.level(),"two native participants");saved=new RegenerationBeltChecks.Snapshot(game,p);betaSaved=new RegenerationBeltChecks.Snapshot(game,beta);
            for(var who:p.level().players())tags.put(who,new HashSet<>(who.entityTags()));
            base=new AllGrimoireChecks.Fixture(game);base.reset();service=game.items().teleportGadgets();require(service.chargingCount()==0,"exclusive charge service");
            random=MatchRandomState.capture(base.level.getRandom());ownerRandom=MatchRandomState.capture(p.getRandom());betaRandom=MatchRandomState.capture(beta.getRandom());
            client=new RegenerationBeltChecks.ScheduledClient(game,p);audience=new RegenerationBeltChecks.ScheduledClient(game,beta);ownerPackets=packets(client);betaPackets=packets(audience);
            origin=base.at;target=origin.add(8,0,0);oldTeam=p.getTeam();oldBetaTeam=beta.getTeam();var board=game.server().getScoreboard();
            require(board.getPlayerTeam("teleport_gadget_fixture")==null,"exclusive team");team=board.addPlayerTeam("teleport_gadget_fixture");team.setAllowFriendlyFire(false);board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
            for(var item:List.of(LbcGadgetItems.ISLAND_TELEPORTER,LbcGadgetItems.SWAPPER,Items.SNOWBALL))stats.put(item,p.getStats().getValue(Stats.ITEM_USED.get(item)));
            base.cells.keySet().forEach(pos->terrain.put(pos,game.maps().snapshot(base.level,pos)));sequence=game.server().getTickCount();
        }
        @SuppressWarnings("unchecked") List<net.minecraft.network.protocol.Packet<?>> packets(RegenerationBeltChecks.ScheduledClient who){try{Object connection=who.connection;var f=connection.getClass().getDeclaredField("packets");f.setAccessible(true);return (List<net.minecraft.network.protocol.Packet<?>>)f.get(connection);}catch(ReflectiveOperationException error){throw new IllegalStateException("native recording packet window",error);}}
        void move(ServerPlayer who,Vec3 at){who.teleportTo(base.level,at.x,at.y,at.z,Set.of(),who==p?31:-73,who==p?-17:23,true);who.setNoGravity(true);who.setDeltaMovement(Vec3.ZERO);who.setKnownMovement(Vec3.ZERO);if(who==p)client.acknowledge();else audience.acknowledge();}
        void reset(){service.forget(p);for(var f:List.copyOf(fields))remove(f);for(var who:List.of(p,beta)){who.getInventory().clearContent();who.removeAllEffects();who.setShiftKeyDown(false);who.getInventory().setSelectedSlot(0);
                who.entityTags().removeAll(Set.of("strict_map_object","sin_greed_active","time_stopped","nomagic_active"));game.items().magic().inventoryChanged(who);game.items().magic().flushCleansing();}
            for(var who:tags.keySet())if(who!=p&&who!=beta)who.entityTags().add("strict_map_object");
            move(p,origin);move(beta,target);ownerPackets.clear();betaPackets.clear();}
        ItemStack held(String id,InteractionHand hand,boolean raw){var out=game.lbc().rollTable(base.level,p.position(),p,"lbc:ported/"+id);require(out.size()==1,"canonical singleton "+id);var source=out.getFirst();
            var s=raw?source.transmuteCopy(Items.SNOWBALL):source;if(raw)for(var c:List.of(DataComponents.CUSTOM_DATA,DataComponents.ITEM_MODEL,DataComponents.CUSTOM_NAME,DataComponents.LORE))copy(s,source,c);
            s.setCount(2);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(hand,s);return s;}
        <T> void copy(ItemStack to,ItemStack from,net.minecraft.core.component.DataComponentType<T> c){var v=from.get(c);if(v!=null)to.set(c,v);}
        boolean input(InteractionHand h,boolean raw){client.acknowledge();return raw?net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,base.level,h).consumesAction():p.gameMode.useItem(p,base.level,p.getItemInHand(h),h).consumesAction();}
        Marker field(String tag){var f=new Marker(EntityTypes.MARKER,base.level);f.setPos(p.position());var cells=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();for(var pos:List.of(f.blockPosition(),f.blockPosition().above()))cells.put(pos,game.maps().snapshot(base.level,pos));fieldCells.put(f,cells);f.entityTags().add(tag);require(base.level.addFreshEntity(f),"real field");fields.add(f);game.items().magic().loaded(f);return f;}
        void remove(Marker f){game.items().magic().unloaded(f);f.discard();fields.remove(f);var cells=fieldCells.remove(f);if(cells!=null)cells.forEach((pos,snapshot)->{game.maps().restoreSnapshot(base.level,pos,snapshot);game.items().magic().blockChanged(pos,base.level.getBlockState(pos));});}
        void artifact(boolean active){p.getInventory().setItem(9,active?new ItemStack(LbcMagic.MYSTICAL_ARTIFACT):ItemStack.EMPTY);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}
        void world(){for(var e:terrain.entrySet())require(e.getValue().equals(game.maps().snapshot(base.level,e.getKey())),"no platform/clearing or terrain edit at "+e.getKey()+" expected="+e.getValue()+" actual="+game.maps().snapshot(base.level,e.getKey()));}
        void islands(){for(boolean raw:new boolean[]{false,true})for(var hand:InteractionHand.values()){
            reset();field("stopper_magic");var s=held("island_teleporter",hand,raw);int stat=p.getStats().getValue(Stats.ITEM_USED.get(s.getItem()));
            require(input(hand,raw)&&s.getCount()==1&&game.maps().spawns().stream().anyMatch(spawn->spawn.position().distanceToSqr(p.position())<1e-10),"actual native/raw island spawn and exact used-hand debit "+hand+" raw="+raw);
            require(p.hasEffect(MobEffects.SLOW_FALLING)&&p.getEffect(MobEffects.SLOW_FALLING).getDuration()==60&&p.getStats().getValue(Stats.ITEM_USED.get(s.getItem()))==stat+1,"source60 Slow Falling and one use stat");world();
        }
            for(String id:List.of("island_teleporter","swapper")){reset();var s=held(id,InteractionHand.MAIN_HAND,false);var f=field("stopper");require(!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2,"TECH denies "+id);remove(f);artifact(true);require(!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2,"strong artifact denies "+id);artifact(false);}
        }
        void begin(int next,boolean raw){reset();phase=next;stack=held("swapper",raw?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,raw);field("stopper_magic");
            require(input(raw?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,raw)&&service.chargingCount()==1&&stack.getCount()==2,"charge accepted without early debit");
            require(ownerPackets.stream().anyMatch(packet->packet instanceof ClientboundSoundPacket sound&&sound.getSound().value().location().toString().equals("minecraft:lbcsounds.swap_charge")),"private charge sound starts");
            require(betaPackets.stream().noneMatch(packet->packet instanceof ClientboundSoundPacket sound&&sound.getSound().value().location().toString().equals("minecraft:lbcsounds.swap_charge")),"charge sound stays private");started=game.server().getTickCount();}
        void failed(){require(service.chargingCount()==0&&stack.getCount()==2,"cancel/no-target has no item debit");
            require(ownerPackets.stream().anyMatch(packet->packet instanceof ClientboundSystemChatPacket chat&&key(chat.content())&&Objects.equals(chat.content().getStyle().getColor(),net.minecraft.network.chat.TextColor.fromRgb(0xff5555))),"red translated charge_failed feedback");
            require(ownerPackets.stream().anyMatch(packet->packet instanceof ClientboundStopSoundPacket stop&&stop.getName()!=null&&stop.getName().toString().equals("minecraft:lbcsounds.swap_charge")),"failure stops own charge sound");require(ownerPackets.stream().noneMatch(packet->packet instanceof ClientboundSoundPacket sound&&sound.getSound().value().location().toString().equals("minecraft:lbcsounds.swap_go")),"cancel never plays success sound");for(var field:List.copyOf(fields))remove(field);world();}
        void swapped(){require(ownerPackets.stream().anyMatch(packet->packet instanceof ClientboundSoundPacket sound&&sound.getSound().value().location().toString().equals("minecraft:lbcsounds.swap_go"))&&betaPackets.stream().anyMatch(packet->packet instanceof ClientboundSoundPacket sound&&sound.getSound().value().location().toString().equals("minecraft:lbcsounds.swap_go")),"success sound only after completed swap for both participants");require(service.chargingCount()==0&&stack.getCount()==1,"successful completion consumes exactly one");near(p.position(),target,"owner reaches actual ally");near(beta.position(),origin,"ally reaches exact old owner position");
            require(p.getYRot()==91&&p.getXRot()==-32&&beta.getYRot()==-73&&beta.getXRot()==23,"both retain own look; camera free throughout charge");
            for(var packets:List.of(ownerPackets,betaPackets)){var packet=packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
                require(packet.relatives().containsAll(Set.of(net.minecraft.world.entity.Relative.DELTA_X,net.minecraft.world.entity.Relative.DELTA_Y,net.minecraft.world.entity.Relative.DELTA_Z))&&packet.change().deltaMovement().equals(Vec3.ZERO),"each client retains its own velocity through relative zero impulse");}world();}
        boolean tick(){client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-sequence<180,"finite scheduled trial");int age=game.server().getTickCount()-started;
            switch(phase){
                case 0->{if(age==1)move(p,origin.add(.009,0,0));if(age==2)move(p,origin);p.setYRot(91);p.setXRot(-32);if(age<10){require(service.chargingCount()==1&&stack.getCount()==2,"first9 real ticks retain charge/item");require(p.position().distanceToSqr(origin)<=.0001,"camera rotation and total sub-centimetre inertia retain charge");return false;}require(age==10,"exact10 real active ticks");swapped();begin(1,false);move(p,origin.add(.011,0,0));return false;}
                case 1->{failed();begin(2,false);p.getInventory().setSelectedSlot(1);return false;}
                case 2->{failed();begin(3,false);field("stopper");return false;}
                case 3->{failed();begin(4,false);artifact(true);return false;}
                case 4->{failed();begin(5,false);for(var who:tags.keySet())if(who!=p)who.entityTags().add("strict_map_object");return false;}
                case 5->{if(age<10){require(service.chargingCount()==1,"no target remains charged until completion");return false;}require(age==10,"no-target completes at10");failed();restoreTargetTags();begin(6,true);beta.entityTags().add("strict_map_object");return false;}
                case 6->{p.setYRot(91);p.setXRot(-32);if(age==9)beta.entityTags().remove("strict_map_object");if(age<10){require(service.chargingCount()==1,"completion samples target rather than launch");return false;}require(age==10,"raw offhand completes at10");swapped();begin(7,false);game.items().timeStop().begin(base.level,100);paused=true;return false;}
                case 7->{p.setYRot(91);p.setXRot(-32);if(age<=3){require(service.chargingCount()==1&&stack.getCount()==2,"time stop pauses pending charge");if(age==3){game.items().timeStop().end(base.level);paused=false;}return false;}if(age<13){require(service.chargingCount()==1,"resumed charge retains remaining active ticks");return false;}require(age==13,"three paused plus10 active ticks");swapped();return true;}
                default->throw new IllegalStateException("charge fixture phase");
            }
        }
        void restoreTargetTags(){tags.forEach((who,before)->{who.entityTags().clear();who.entityTags().addAll(before);});}
        void initialize(){islands();begin(0,false);}
        public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{service.forget(p);if(paused)game.items().timeStop().end(base.level);for(var f:List.copyOf(fields))remove(f);restoreTargetTags();},()->{var board=game.server().getScoreboard();board.removePlayerFromTeam(p.getScoreboardName(),team);board.removePlayerFromTeam(beta.getScoreboardName(),team);board.removePlayerTeam(team);if(oldTeam!=null)board.addPlayerToTeam(p.getScoreboardName(),oldTeam);if(oldBetaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBetaTeam);},client::close,audience::close,base::close,()->saved.restore(game),saved::restoreAttributes,saved::restoreUse,()->betaSaved.restore(game),betaSaved::restoreAttributes,betaSaved::restoreUse,()->{stats.forEach((item,n)->p.getStats().setValue(p,Stats.ITEM_USED.get(item),n));random.restore(base.level.getRandom());ownerRandom.restore(p.getRandom());betaRandom.restore(beta.getRandom());game.items().magic().inventoryChanged(p);game.items().magic().inventoryChanged(beta);restoreTargetTags();game.players().save();}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("teleport gadget scoped restore",failure);
        }
    }
    public static void start(GameController game){require(running==null,"exclusive finite trial");var state=new State(game);try{state.initialize();running=state;SkyWars2.LOGGER.info("[check] START teleport-gadgets");}catch(Throwable error){try{state.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS teleport-gadgets (native/raw hands, actual island spawns/60 Slow Falling, real10 active tick swap, free camera/allies, movement/slot/TECH/artifact/no-target cancel without debit, private charge sound/failure stop/red translated feedback, completion target sampling, scheduled time-stop pause, no terrain edits)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL teleport-gadgets",error);}}
}
