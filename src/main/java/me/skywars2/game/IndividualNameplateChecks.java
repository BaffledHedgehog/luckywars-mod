package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.*;

/** Actual presentation packets reconstructed into independent vanilla client scoreboards. */
public final class IndividualNameplateChecks {
    private IndividualNameplateChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Individual nameplates: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){require(running==null,"exclusive fixture");var f=new Fixture(game,rewind);running=f;try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{require(game==f.game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} (actual packet projection, individual source suffixes, canonical teams preserved and client state reconstructed)",f.rewind?"nameplates-rewind":"nameplates-native");}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+(f.rewind?"nameplates-rewind":"nameplates-native"),error);}}
    private static Object get(Object object,String name){for(var c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}throw new IllegalStateException(name);}
    private static int mode(ClientboundSetPlayerTeamPacket packet){if(packet.getTeamAction()==ClientboundSetPlayerTeamPacket.Action.ADD)return 0;if(packet.getTeamAction()==ClientboundSetPlayerTeamPacket.Action.REMOVE)return 1;if(packet.getPlayerAction()==ClientboundSetPlayerTeamPacket.Action.ADD)return 3;if(packet.getPlayerAction()==ClientboundSetPlayerTeamPacket.Action.REMOVE)return 4;return 2;}
    /** The same five operations as ClientPacketListener.handleSetPlayerTeamPacket, without client code. */
    private static final class Client implements AutoCloseable {
        final GameController game;final ServerPlayer player;final RegenerationBeltChecks.ScheduledClient wire;
        final Scoreboard board=new Scoreboard();final Set<Integer> modes=new HashSet<>();final List<ClientboundSetPlayerTeamPacket> teams=new ArrayList<>();
        int cursor,bundles;
        Client(GameController game,ServerPlayer player){this.game=game;this.player=player;wire=new RegenerationBeltChecks.ScheduledClient(game,player);}
        @SuppressWarnings("unchecked") void receive(){wire.acknowledge();var packets=(List<Packet<?>>)get(wire.connection,"packets");while(cursor<packets.size())apply(packets.get(cursor++));}
        void apply(Packet<?> packet){
            if(packet instanceof ClientboundBundlePacket bundle){bundles++;for(var child:bundle.subPackets())apply(child);return;}
            if(!(packet instanceof ClientboundSetPlayerTeamPacket teamPacket))return;teams.add(teamPacket);modes.add(mode(teamPacket));
            var action=teamPacket.getTeamAction();PlayerTeam team;
            if(action==ClientboundSetPlayerTeamPacket.Action.ADD)team=board.addPlayerTeam(teamPacket.getName());
            else{team=board.getPlayerTeam(teamPacket.getName());require(team!=null,"client received metadata/membership/removal before team definition: "+teamPacket.getName()+" mode="+mode(teamPacket));}
            if(teamPacket.getParameters().isPresent()){var p=teamPacket.getParameters().orElseThrow();team.setDisplayName(p.displayName());team.setColor(p.color());team.unpackOptions(p.options());team.setNameTagVisibility(p.nameTagVisibility());team.setCollisionRule(p.collisionRule());team.setPlayerPrefix(p.playerPrefix());team.setPlayerSuffix(p.playerSuffix());}
            if(teamPacket.getPlayerAction()==ClientboundSetPlayerTeamPacket.Action.ADD)for(var name:teamPacket.getPlayers())board.addPlayerToTeam(name,team);
            else if(teamPacket.getPlayerAction()==ClientboundSetPlayerTeamPacket.Action.REMOVE)for(var name:teamPacket.getPlayers()){require(board.getPlayersTeam(name)==team,"client membership removal targets its actual team "+name);board.removePlayerFromTeam(name,team);}
            if(action==ClientboundSetPlayerTeamPacket.Action.REMOVE)board.removePlayerTeam(team);
        }
        PlayerTeam team(ServerPlayer target){return Objects.requireNonNull(board.getPlayersTeam(target.getScoreboardName()),"projected membership for "+target.getScoreboardName());}
        public void close(){wire.close();}
    }
    private record NativeTeam(PlayerTeam identity,ClientboundSetPlayerTeamPacket.Parameters metadata,Team.Visibility deaths,Set<String> members){
        NativeTeam(PlayerTeam t){this(t,new ClientboundSetPlayerTeamPacket.Parameters(t),t.getDeathMessageVisibility(),Set.copyOf(t.getPlayers()));}
        void verify(GameController game){require(game.server().getScoreboard().getPlayerTeam(identity.getName())==identity&&metadata.equals(new ClientboundSetPlayerTeamPacket.Parameters(identity))&&deaths==identity.getDeathMessageVisibility()&&members.equals(Set.copyOf(identity.getPlayers())),"presentation changes no actual team identity/metadata/members "+identity.getName());}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final ServerPlayer owner,other;final MatchCoreCheckpoint core;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final boolean frozen;final Set<UUID> originalActors=new HashSet<>();
        Client self,audience;PlayerTeam shared,enemy;int state,started,when;boolean closed,restoring,paused;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&!game.maps().busy()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"actual participants and exclusive stable coordinator");
            require(game.items().fpvDrone().count()==0&&!game.items().timeStop().stopped(owner.level()),"exclusive unpaused FPV family");
            frozen=game.server().tickRateManager().isFrozen();require(!frozen,"normally ticking scene");core=new MatchCoreCheckpoint(game);saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            owner.level().getAllEntities().forEach(e->originalActors.add(e.getUUID()));self=new Client(game,owner);audience=new Client(game,other);started=game.server().getTickCount();
        }
        String name(){return rewind?"nameplates-rewind":"nameplates-native";}
        void receive(){self.receive();audience.receive();}
        void refresh(){game.items().cosmetics().refreshSuffixes(List.of(owner,other));receive();}
        void sync(Client client){for(var team:game.server().getScoreboard().getPlayerTeams())client.player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team,true));client.receive();}
        void select(int a,int b){game.players().get(owner.getUUID()).suffix=a;game.players().get(other.getUUID()).suffix=b;refresh();}
        void grant(ServerPlayer player){var profile=game.players().get(player.getUUID());for(int n=1;n<=29;n++)profile.legacyUnlocks.add(n<=26?"swrg:rewards/suffix_"+n:"swrg:rewards/turnir/trophy"+(n-26));LbcSourceUnlocks.publish(player,profile);}
        void begin(){
            grant(owner);grant(other);refresh();sync(self);sync(audience);
            var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_np_shared")==null&&board.getPlayerTeam("sw2_np_enemy")==null,"exclusive actual team names");
            shared=board.addPlayerTeam("sw2_np_shared");shared.setDisplayName(Component.literal("Nameplate fixture"));shared.setPlayerPrefix(Component.literal("prefix:"));shared.setPlayerSuffix(Component.literal(":native"));shared.setColor(Optional.of(TeamColor.RED));shared.setAllowFriendlyFire(false);shared.setSeeFriendlyInvisibles(true);shared.setNameTagVisibility(Team.Visibility.ALWAYS);shared.setDeathMessageVisibility(Team.Visibility.HIDE_FOR_OTHER_TEAMS);shared.setCollisionRule(Team.CollisionRule.ALWAYS);
            board.addPlayerToTeam(owner.getScoreboardName(),shared);board.addPlayerToTeam(other.getScoreboardName(),shared);board.addPlayerToTeam("np_actor",shared);select(17,29);
            if(rewind){pause();require(game.vanity().requestCapture(owner,false),"whole coordinator accepts shared actual team and distinct profile suffixes");state=10;return;}
            nativePackets();launchFpv();state=0;when=game.server().getTickCount();
        }
        void projected(Client view,ServerPlayer target,int suffix){
            var actual=target.getTeam();var team=view.team(target);var expected=(actual==null?Component.empty():actual.getPlayerSuffix().copy()).append(LbcCosmetics.suffix(suffix));
            require(team.getPlayerSuffix().equals(expected)&&team.getPlayerPrefix().equals(actual==null?Component.empty():actual.getPlayerPrefix())&&team.getColor().equals(actual==null?Optional.empty():actual.getColor()),"actual packet client gets owner's prefix/color/selected suffix "+target.getScoreboardName()+" id="+suffix+" suffix="+team.getPlayerSuffix());
            if(actual!=null)require(team.isAllowFriendlyFire()==actual.isAllowFriendlyFire(),"client friendly-fire option retains native choice");
        }
        void relation(boolean ally){
            var board=game.server().getScoreboard();if(ally)board.addPlayerToTeam(owner.getScoreboardName(),shared);else board.addPlayerToTeam(owner.getScoreboardName(),enemy);
            for(var visibility:Team.Visibility.values())for(var collision:Team.CollisionRule.values()){
                shared.setNameTagVisibility(visibility);shared.setCollisionRule(collision);var real=new NativeTeam(shared);refresh();real.verify(game);
                boolean visible=switch(visibility){case ALWAYS->true;case NEVER->false;case HIDE_FOR_OTHER_TEAMS->ally;case HIDE_FOR_OWN_TEAM->!ally;};
                boolean push=switch(collision){case ALWAYS->true;case NEVER->false;case PUSH_OWN_TEAM->!ally;case PUSH_OTHER_TEAMS->ally;};
                require(self.team(other).getNameTagVisibility()==(visible?Team.Visibility.ALWAYS:Team.Visibility.NEVER),"native viewer relation controls name visibility "+visibility+" ally="+ally);
                require(self.team(other).getCollisionRule()==(push?Team.CollisionRule.ALWAYS:Team.CollisionRule.NEVER),"native viewer relation controls client collision "+collision+" ally="+ally);
            }
        }
        void nativePackets(){
            var board=game.server().getScoreboard();var baseline=new NativeTeam(shared);
            for(int n=1;n<=29;n++){select(n,n%29+1);for(var view:List.of(self,audience)){projected(view,owner,n);projected(view,other,n%29+1);require(view.team(owner)!=view.team(other),"same actual team has separate owner projections");var nativeTeam=view.board.getPlayerTeam(shared.getName());require(nativeTeam!=null&&view.board.getPlayersTeam("np_actor")==nativeTeam&&nativeTeam.getPlayerSuffix().equals(shared.getPlayerSuffix()),"native team definition and non-player suffix/membership remain");}baseline.verify(game);}
            select(0,0);projected(self,owner,0);projected(self,other,0);baseline.verify(game);
            enemy=board.addPlayerTeam("sw2_np_enemy");enemy.setCollisionRule(Team.CollisionRule.ALWAYS);relation(true);relation(false);
            board.addPlayerToTeam(owner.getScoreboardName(),shared);shared.setNameTagVisibility(Team.Visibility.ALWAYS);shared.setCollisionRule(Team.CollisionRule.ALWAYS);select(17,29);
            board.addPlayerToTeam("np_temporary",shared);receive();require(self.board.getPlayersTeam("np_temporary").getName().equals(shared.getName()),"native non-player JOIN retained");board.removePlayerFromTeam("np_temporary",shared);receive();require(self.board.getPlayersTeam("np_temporary")==null,"native non-player LEAVE retained");
            var foreign=new PlayerTeam(new Scoreboard(),"foreign_np");foreign.setPlayerPrefix(Component.literal("foreign:"));foreign.getPlayers().add("foreign_actor");
            owner.connection.send(new ClientboundBundlePacket(List.of(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(foreign,true),ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(foreign,false),ClientboundSetPlayerTeamPacket.createRemovePacket(foreign))));self.receive();require(self.bundles>0&&self.board.getPlayerTeam(foreign.getName())==null,"actual bundle preserves foreign team lifecycle");
            board.removePlayerFromTeam(other.getScoreboardName(),shared);receive();projected(self,other,29);require(other.getTeam()==null,"actual native leave still owns membership");
            board.removePlayerTeam(shared);receive();require(self.board.getPlayerTeam(shared.getName())==null&&self.board.getPlayersTeam("np_actor")==null,"native team REMOVE removes only its remaining non-player group");projected(self,owner,17);
            require(self.modes.containsAll(Set.of(0,1,2,3,4)),"all five actual team packet modes decoded");
            // A fresh native listener has no cached client teams; connection replacement must rebuild.
            audience.close();audience=new Client(game,other);refresh();sync(audience);projected(audience,owner,17);projected(audience,other,29);
        }
        void launchFpv(){
            var at=new Vec3(0,Math.min(220,owner.level().getMaxY()-40),0);require(owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true),"native FPV fixture body teleport");owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);self.wire.acknowledge();
            var stack=new ItemStack(Items.CARROT_ON_A_STICK,2);var data=new net.minecraft.nbt.CompoundTag();data.putInt("drone_pult",1);data.putInt("raycastspell",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(owner.gameMode.useItem(owner,owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction()&&game.items().fpvDrone().controlling(owner),"actual FPV controller starts native owner-only hide");
        }
        void pause(){game.items().timeStop().begin(owner.level(),10000);paused=true;}
        void thaw(){game.items().timeStop().end(owner.level());paused=false;}
        boolean step(){
            receive();require(game.server().getTickCount()-started<450,"bounded actual packet fixture state="+state);
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(rewind)return replayStep();
            var session=((Map<?,?>)get(game.items().fpvDrone(),"controllers")).get(owner.getUUID());
            if(session==null||!(Boolean)get(session,"attached")){require(game.server().getTickCount()-when<80,"real FPV camera attaches");return false;}
            var hidden=(PlayerTeam)get(session,"hiddenTeam");require(hidden!=null&&self.team(owner).getName().equals(hidden.getName()),"actual foreign FPV hidden membership survives projection");
            var hiddenBefore=new ClientboundSetPlayerTeamPacket.Parameters(self.team(owner));select(19,28);
            require(self.team(owner).getName().equals(hidden.getName())&&hiddenBefore.equals(new ClientboundSetPlayerTeamPacket.Parameters(self.team(owner)))&&!self.team(owner).canSeeFriendlyInvisibles(),"selection refresh never rewrites or undoes FPV own-body hide");projected(audience,owner,19);
            game.items().fpvDrone().stop();refresh();projected(self,owner,19);require(!self.team(owner).getName().startsWith("sw2fpv"),"FPV release restores ordinary individual membership");return true;
        }
        boolean replayStep(){
            if(state==10){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;projected(self,owner,17);projected(self,other,29);
                select(5,2);shared.setPlayerPrefix(Component.literal("future:"));shared.setColor(Optional.of(TeamColor.BLUE));game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),shared);refresh();projected(self,owner,5);projected(self,other,2);
                require(game.vanity().requestRestoreForCheck(),"actual canonical whole coordinator restore");restoring=true;state=11;return false;
            }
            if(game.vanity().busy())return false;receive();require(owner.getTeam()==shared&&other.getTeam()==shared&&shared.getPlayerPrefix().getString().equals("prefix:")&&shared.getColor().equals(Optional.of(TeamColor.RED)),"whole coordinator restores original actual shared team metadata/membership");
            projected(self,owner,17);projected(self,other,29);projected(audience,owner,17);projected(audience,other,29);
            require(self.teams.stream().anyMatch(p->p.getName().equals(shared.getName())&&p.getParameters().isPresent()),"restored native original team definitions still reach actual packet stream");return true;
        }
        public void close(){if(closed)return;closed=true;Throwable failure=null;game.server().tickRateManager().setFrozen(true);
            for(Runnable action:List.<Runnable>of(()->{if(rewind)game.vanity().stop();game.items().fpvDrone().stop();if(paused)thaw();},()->{saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();core.restoreFrozen();game.players().save();},self::close,()->audience.close(),()->game.items().cosmetics().refreshSuffixes(game.server().getPlayerList().getPlayers()),()->game.server().tickRateManager().setFrozen(frozen)))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw new IllegalStateException("Individual nameplate scoped cleanup",failure);
        }
    }
}
