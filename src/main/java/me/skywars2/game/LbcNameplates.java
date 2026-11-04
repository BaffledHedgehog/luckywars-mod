package me.skywars2.game;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.IndividualNameplateMixin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.*;

/** Per-connection cosmetic teams. Canonical scoreboard membership is never changed here. */
public final class LbcNameplates {
    private static final String PREFIX="sw2np:";
    private record TeamImage(String name,ClientboundSetPlayerTeamPacket.Parameters parameters,Set<String> members){}
    private record Person(UUID uuid,String name,String team,int suffix,int group,int order){}
    private record Image(long generation,Map<String,TeamImage> teams,Map<String,Person> people){}
    private static final class View {
        final Object connection;
        final Map<String,String> ids=new HashMap<>();
        final Map<String,ClientboundSetPlayerTeamPacket.Parameters> parameters=new HashMap<>();
        final Map<String,Set<String>> members=new HashMap<>();
        final Set<String> canonical=new HashSet<>(),foreign=new HashSet<>();
        String hiddenTeam;
        long generation=Long.MIN_VALUE;
        View(Object connection){this.connection=connection;}
    }
    private final GameController game;
    private final Map<UUID,View> views=new ConcurrentHashMap<>();
    private final Set<UUID> departing=new HashSet<>();
    private final ThreadLocal<Boolean> emitting=ThreadLocal.withInitial(()->false);
    private volatile Image image=new Image(0,Map.of(),Map.of());
    private long generation;
    public LbcNameplates(GameController game){this.game=game;}

    /** Called once at the canonical mutation boundary, before ServerScoreboard broadcasts. */
    public static void changed(Scoreboard board){
        var game=SkyWars2.game();if(game==null||game.server().getScoreboard()!=board||!game.server().isSameThread())return;
        var service=game.items().cosmetics().nameplates();if(service!=null)service.refresh(List.of());
    }
    private static Component frozen(Component component){var copy=component.plainCopy().setStyle(component.getStyle());for(var child:component.getSiblings())copy.append(frozen(child));return copy;}
    private static ClientboundSetPlayerTeamPacket.Parameters freeze(PlayerTeam team){
        return new ClientboundSetPlayerTeamPacket.Parameters(frozen(team.getDisplayName()),frozen(team.getPlayerPrefix()),frozen(team.getPlayerSuffix()),team.getNameTagVisibility(),team.getCollisionRule(),team.getColor(),team.packOptions());
    }
    private void capture(){
        if(!game.server().isSameThread())throw new IllegalStateException("Publish nameplate images on the native server thread");
        var teams=new TreeMap<String,TeamImage>();
        for(var team:game.server().getScoreboard().getPlayerTeams())teams.put(team.getName(),new TeamImage(team.getName(),freeze(team),Set.copyOf(team.getPlayers())));
        var ranks=new HashMap<String,Integer>();int rank=1;for(var name:teams.keySet())ranks.put(name,rank++);
        var online=new ArrayList<>(game.server().getPlayerList().getPlayers());online.sort(Comparator.comparing(ServerPlayer::getScoreboardName));
        var present=new HashSet<UUID>();for(var player:online)present.add(player.getUUID());departing.retainAll(present);
        var people=new LinkedHashMap<String,Person>();int ordinal=0;
        for(var p:online){if(departing.contains(p.getUUID()))continue;var team=p.getTeam();String name=p.getScoreboardName();people.put(name,new Person(p.getUUID(),name,team==null?null:team.getName(),game.items().cosmetics().choice(p,LbcCosmetics.Kind.SUFFIX),team==null?0:ranks.getOrDefault(team.getName(),0),ordinal++));}
        image=new Image(++generation,Map.copyOf(teams),Map.copyOf(people));
    }
    /** Every viewer may depend on the changed owner's real relation, so deltas go to all viewers.
     * Packets are sent individually through the ordinary FPV and nameplate presentation hooks. */
    public void refresh(Collection<ServerPlayer> changed){
        if(!game.server().isSameThread())throw new IllegalStateException("Refresh nameplates on the native server thread");
        for(var player:changed)departing.remove(player.getUUID());
        capture();var snapshot=image;
        for(var viewer:game.server().getPlayerList().getPlayers()){
            if(departing.contains(viewer.getUUID()))continue;
            var view=view(viewer);List<Packet<?>> packets;
            synchronized(view){packets=delta(view,snapshot,viewer.getUUID());}
            boolean previous=emitting.get();emitting.set(true);
            try{for(var packet:packets)viewer.connection.send(packet);}finally{emitting.set(previous);}
        }
    }
    public void forget(ServerPlayer player){
        if(!game.server().isSameThread())throw new IllegalStateException("Forget nameplate connections on the native server thread");
        departing.add(player.getUUID());views.remove(player.getUUID());refresh(List.of());
    }
    private View view(ServerPlayer player){return views.compute(player.getUUID(),(id,old)->old!=null&&old.connection==player.connection?old:new View(player.connection));}
    private static ClientboundSetPlayerTeamPacket packet(String name,int method,ClientboundSetPlayerTeamPacket.Parameters parameters,Collection<String> players){return IndividualNameplateMixin.PacketAccess.skywars2$create(name,method,Optional.ofNullable(parameters),players);}
    private static ClientboundSetPlayerTeamPacket membership(String team,String name,boolean add){return packet(team,add?3:4,null,List.of(name));}
    private static int method(ClientboundSetPlayerTeamPacket packet){
        if(packet.getTeamAction()==ClientboundSetPlayerTeamPacket.Action.ADD)return 0;
        if(packet.getTeamAction()==ClientboundSetPlayerTeamPacket.Action.REMOVE)return 1;
        if(packet.getPlayerAction()==ClientboundSetPlayerTeamPacket.Action.ADD)return 3;
        if(packet.getPlayerAction()==ClientboundSetPlayerTeamPacket.Action.REMOVE)return 4;
        return 2;
    }
    private static Person viewer(Image image,UUID id){for(var p:image.people.values())if(p.uuid.equals(id))return p;return null;}
    private static TeamImage realTeam(Image image,Person player){return player==null||player.team==null?null:image.teams.get(player.team);}
    private static boolean collide(TeamImage a,TeamImage b){
        var own=a==null?Team.CollisionRule.ALWAYS:a.parameters.collisionRule();var other=b==null?Team.CollisionRule.ALWAYS:b.parameters.collisionRule();
        if(own==Team.CollisionRule.NEVER||other==Team.CollisionRule.NEVER)return false;
        boolean same=a!=null&&b!=null&&a.name.equals(b.name);
        if(same&&(own==Team.CollisionRule.PUSH_OWN_TEAM||other==Team.CollisionRule.PUSH_OWN_TEAM))return false;
        return same||own!=Team.CollisionRule.PUSH_OTHER_TEAMS&&other!=Team.CollisionRule.PUSH_OTHER_TEAMS;
    }
    private static ClientboundSetPlayerTeamPacket.Parameters projectParameters(ClientboundSetPlayerTeamPacket.Parameters source,TeamImage actual,TeamImage viewer,boolean self,Component personal){
        boolean same=actual!=null&&viewer!=null&&actual.name.equals(viewer.name);
        var visibility=switch(source.nameTagVisibility()){
            case ALWAYS->Team.Visibility.ALWAYS;case NEVER->Team.Visibility.NEVER;
            case HIDE_FOR_OTHER_TEAMS->viewer==null||same?Team.Visibility.ALWAYS:Team.Visibility.NEVER;
            case HIDE_FOR_OWN_TEAM->viewer==null||!same?Team.Visibility.ALWAYS:Team.Visibility.NEVER;
        };
        var suffix=source.playerSuffix().copy();if(personal!=null)suffix.append(personal);
        return new ClientboundSetPlayerTeamPacket.Parameters(source.displayName(),source.playerPrefix(),suffix,visibility,self||collide(actual,viewer)?Team.CollisionRule.ALWAYS:Team.CollisionRule.NEVER,source.color(),source.options());
    }
    private static ClientboundSetPlayerTeamPacket.Parameters empty(){return new ClientboundSetPlayerTeamPacket.Parameters(Component.empty(),Component.empty(),Component.empty(),Team.Visibility.ALWAYS,Team.CollisionRule.ALWAYS,Optional.empty(),(byte)3);}
    private String id(View view,Image snapshot,Person person,Set<String> allocated){
        // 26.3's team packet uses unrestricted readUtf(), not the old command's 16-character UI.
        // Keep vanilla TAB's team-name/name grouping without changing player profiles or list order.
        String group=person.team==null?"":person.team;
        String stem=PREFIX+group+'\0'+person.name.toLowerCase(Locale.ROOT)+'\0'+person.uuid;
        // A protocol-limit-sized foreign team name is unusual but still must remain encodable.
        if(stem.length()>32000)stem=PREFIX+String.format(Locale.ROOT,"%04x",person.group)+'\0'+person.name.toLowerCase(Locale.ROOT)+'\0'+person.uuid;
        String id=stem;int conflict=0;
        while(snapshot.teams.containsKey(id)||view.foreign.contains(id)||allocated.contains(id))id=stem+'\0'+(++conflict);
        allocated.add(id);return id;
    }
    private List<Packet<?>> delta(View view,Image snapshot,UUID viewerId){
        if(view.generation==snapshot.generation)return List.of();
        var result=new ArrayList<Packet<?>>();var viewer=viewer(snapshot,viewerId);var viewerTeam=realTeam(snapshot,viewer);
        var desired=new HashMap<String,String>();var allocated=new HashSet<String>();var sorted=new ArrayList<>(snapshot.people.values());sorted.sort(Comparator.comparingInt(Person::group).thenComparing(Person::name));
        for(var person:sorted)desired.put(person.name,id(view,snapshot,person,allocated));
        for(var entry:List.copyOf(view.ids.entrySet()))if(!Objects.equals(desired.get(entry.getKey()),entry.getValue())){
            result.add(packet(entry.getValue(),1,null,List.of()));view.parameters.remove(entry.getValue());view.members.remove(entry.getValue());view.ids.remove(entry.getKey());
        }
        // Canonical teams retain non-player entries and original client group identity for those entries.
        for(var name:List.copyOf(view.canonical)){
            var team=snapshot.teams.get(name);if(team==null)continue;
            var parameters=projectParameters(team.parameters,team,viewerTeam,false,null);
            if(!parameters.equals(view.parameters.get(name))){result.add(packet(name,2,parameters,List.of()));view.parameters.put(name,parameters);}
            var wanted=new HashSet<>(team.members);wanted.removeAll(snapshot.people.keySet());var previous=view.members.getOrDefault(name,Set.of());
            for(var member:previous)if(!wanted.contains(member))result.add(membership(name,member,false));
            for(var member:wanted)if(!previous.contains(member))result.add(membership(name,member,true));
            view.members.put(name,Set.copyOf(wanted));
        }
        for(var person:sorted){String id=desired.get(person.name);var team=realTeam(snapshot,person);var source=team==null?empty():team.parameters;
            var parameters=projectParameters(source,team,viewerTeam,person.uuid.equals(viewerId),LbcCosmetics.suffix(person.suffix));
            if(!view.ids.containsKey(person.name)){result.add(packet(id,0,parameters,List.of(person.name)));view.ids.put(person.name,id);}
            else if(!parameters.equals(view.parameters.get(id)))result.add(packet(id,2,parameters,List.of()));
            view.parameters.put(id,parameters);view.members.put(id,Set.of(person.name));
        }
        view.generation=snapshot.generation;
        if(view.hiddenTeam!=null&&viewer!=null)result.add(membership(view.hiddenTeam,viewer.name,true));
        return List.copyOf(result);
    }
    /** No profile, scoreboard, world or team reads occur on this packet path. */
    @SuppressWarnings({"unchecked","rawtypes"})
    public Packet<?> project(ServerPlayer recipient,Packet<?> original){
        if(emitting.get()||!(original instanceof ClientboundSetPlayerTeamPacket)&&!(original instanceof ClientboundBundlePacket))return original;
        var snapshot=image;var view=view(recipient);
        synchronized(view){
            var transformed=new ArrayList<Packet<?>>();rewrite(view,snapshot,recipient.getUUID(),original,transformed);
            var updates=delta(view,snapshot,recipient.getUUID());
            if(updates.isEmpty()&&transformed.size()==1)return transformed.getFirst();
            var packets=new ArrayList<Packet<? super ClientGamePacketListener>>();for(var packet:transformed)packets.add((Packet)packet);for(var packet:updates)packets.add((Packet)packet);
            var player=viewer(snapshot,recipient.getUUID());if(view.hiddenTeam!=null&&player!=null&&!updates.isEmpty())packets.add(membership(view.hiddenTeam,player.name,true));
            return new ClientboundBundlePacket(packets);
        }
    }
    private void rewrite(View view,Image snapshot,UUID viewerId,Packet<?> original,List<Packet<?>> output){
        if(original instanceof ClientboundBundlePacket bundle){for(var child:bundle.subPackets())rewrite(view,snapshot,viewerId,child,output);return;}
        if(!(original instanceof ClientboundSetPlayerTeamPacket team)){output.add(original);return;}
        int mode=method(team);String name=team.getName();var recipient=viewer(snapshot,viewerId);
        if(name.startsWith("sw2fpv")){
            if(mode==0||mode==3){view.foreign.add(name);if(recipient!=null&&team.getPlayers().contains(recipient.name))view.hiddenTeam=name;}
            boolean restore=name.equals(view.hiddenTeam)&&(mode==1||mode==4&&recipient!=null&&team.getPlayers().contains(recipient.name));
            if(mode==1)view.foreign.remove(name);
            if(restore)view.hiddenTeam=null;
            output.add(original);
            if(restore&&recipient!=null&&view.ids.containsKey(recipient.name))output.add(membership(view.ids.get(recipient.name),recipient.name,true));
            return;
        }
        if(!snapshot.teams.containsKey(name)&&!view.canonical.contains(name)){
            if(mode==0){
                // A foreign sender may choose our reserved name; retire our old client group first.
                if(view.ids.containsValue(name)){output.add(packet(name,1,null,List.of()));view.ids.entrySet().removeIf(e->e.getValue().equals(name));view.parameters.remove(name);view.members.remove(name);}
                view.foreign.add(name);view.generation=Long.MIN_VALUE;
            }
            if(mode==1){view.foreign.remove(name);view.generation=Long.MIN_VALUE;}
            output.add(original);return;
        }
        if(mode==0)view.canonical.add(name);
        if(mode==1){view.canonical.remove(name);view.parameters.remove(name);view.members.remove(name);output.add(original);return;}
        var real=snapshot.teams.get(name);var viewerTeam=realTeam(snapshot,recipient);
        var parameters=team.getParameters().map(p->projectParameters(p,real,viewerTeam,false,null)).orElse(null);
        if(parameters!=null)view.parameters.put(name,parameters);
        var previous=view.members.getOrDefault(name,Set.of());
        var names=team.getPlayers().stream().filter(member->!snapshot.people.containsKey(member))
                .filter(member->mode!=3&&mode!=4||previous.contains(member)==(mode==4)).toList();
        if(mode==0)view.members.put(name,Set.copyOf(names));
        else if(mode==3||mode==4){var members=new HashSet<>(view.members.getOrDefault(name,Set.of()));if(mode==3)members.addAll(names);else members.removeAll(names);view.members.put(name,Set.copyOf(members));}
        if(mode==3||mode==4){if(!names.isEmpty())output.add(packet(name,mode,null,names));}
        else output.add(packet(name,mode,parameters,names));
    }
}
