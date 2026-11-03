package me.skywars2.game;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Explicit entity_data egg callbacks. Native eggs own placement and their single consumption. */
final class LbcBalanceEggActors {
    private static final String TAG="skywars2.balance.old_egg_marker";
    private final GameController game;
    private final Map<Marker,State> markers=new IdentityHashMap<>();
    private final Map<ServerLevel,Integer> deadlines=new IdentityHashMap<>();
    private final Map<ServerLevel,Integer> born=new IdentityHashMap<>();
    private final Set<PrimedTnt> children=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final class State {
        final LbcIndirectKills.Owner cause;final String epoch;final boolean nuclear;boolean armed;
        State(LbcIndirectKills.Owner cause,String epoch,boolean nuclear){this.cause=cause;this.epoch=epoch;this.nuclear=nuclear;}
    }
    LbcBalanceEggActors(GameController game){this.game=game;}
    void adopt(Entity actor,LivingEntity user){
        if(actor instanceof net.minecraft.world.entity.animal.pig.Pig pig&&actor.entityTags().contains("fakepig")){game.items().luckyPigs().adoptFakeEgg(pig,user);return;}
        if(!(actor instanceof Marker marker)||!actor.entityTags().contains("compressedtnt")&&!actor.entityTags().contains("nuke_old"))return;
        var author=LbcIndirectKills.playerSource(user);var cause=author==null?new LbcIndirectKills.Owner(new UUID(0,0),0,true,true):game.items().indirectKills().wildCause(author);
        var state=new State(cause,game.items().epoch(),actor.entityTags().contains("nuke_old"));markers.put(marker,state);actor.entityTags().add(TAG);game.items().markCurrentItemEpoch(actor);
        game.items().spellSources().bind(actor,new LbcSpellSource.Source(actor.getUUID(),cause.player(),state.epoch,cause,Set.of(),true));game.items().timeStop().prepareFrozen(actor);
    }
    private void compressed(Marker marker,State state){
        var level=(ServerLevel)marker.level();var at=marker.position().add(0,.2,0);
        game.items().indirectKills().capturedWorldChanges(level,state.cause,()->LbcDropBlocks.setBlock(level,BlockPos.containing(at),Blocks.REDSTONE_BLOCK.defaultBlockState(),false));
        var author=game.server().getPlayerList().getPlayer(state.cause.player());
        for(int tier=0;tier<4;tier++){double radius=new double[]{1,.8,1.2,.8}[tier],y=tier==1?.2:0;int fuse=60+tier*5;
            for(var offset:List.of(new Vec3(radius,y,0),new Vec3(-radius,y,0),new Vec3(0,y,radius),new Vec3(0,y,-radius))){var pos=at.add(offset);var tnt=new PrimedTnt(level,pos.x,pos.y,pos.z,author);tnt.setFuse(fuse);tnt.setNoGravity(true);tnt.setDeltaMovement(Vec3.ZERO);
                game.items().markCurrentItemEpoch(tnt);game.items().spellSources().bind(tnt,new LbcSpellSource.Source(marker.getUUID(),state.cause.player(),state.epoch,state.cause,Set.of(),true));game.items().timeStop().prepareFrozen(tnt);children.add(tnt);if(!level.addFreshEntity(tnt)){children.remove(tnt);tnt.discard();}
            }
        }
        for(float pitch:new float[]{1,0,2,1.5f,.5f,.2f})LbcOverload.sound(level,at,"minecraft:entity.tnt.primed",1,pitch,0,0);
        markers.remove(marker);marker.discard();
    }
    private void arm(Marker marker,State state){
        var level=(ServerLevel)marker.level();state.armed=true;marker.entityTags().add("nuke1_old");deadlines.put(level,400);born.put(level,game.server().getTickCount());
        var nearest=level.players().stream().filter(p->!p.isSpectator()).min(Comparator.comparingDouble(p->p.distanceToSqr(marker))).orElse(null);
        var subtitle=nearest==null?Component.empty():nearest.getDisplayName().copy().withStyle(ChatFormatting.RED);
        subtitle.append(Component.translatable("nuke_activated").withStyle(ChatFormatting.DARK_RED));
        for(var p:game.server().getPlayerList().getPlayers()){p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));}
    }
    private void beacon(Marker marker,State state){var level=(ServerLevel)marker.level();var pos=marker.blockPosition();
        game.items().indirectKills().capturedWorldChanges(level,state.cause,()->{
            LbcDropBlocks.fill(level,pos.offset(-1,0,-1),pos.offset(1,0,1),Blocks.NETHERITE_BLOCK,true);
            LbcDropBlocks.fill(level,pos.above(3),new BlockPos(pos.getX(),255,pos.getZ()),Blocks.AIR,true);
            LbcDropBlocks.setBlock(level,pos.above(),Blocks.BEACON.defaultBlockState(),true);LbcDropBlocks.setBlock(level,pos.above(2),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(),true);
        });LbcOverload.sound(level,marker.position(),"minecraft:block.note_block.bell",100,0,0,0);
    }
    void tick(){
        children.removeIf(body->body.isRemoved()&&body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK);
        for(var entry:List.copyOf(markers.entrySet())){var marker=entry.getKey();var state=entry.getValue();
            if(marker.isRemoved()&&marker.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK){markers.remove(marker);continue;}
            if(!game.items().currentEpoch(state.epoch)){markers.remove(marker);marker.discard();continue;}
            var level=(ServerLevel)marker.level();if(marker.isRemoved()||game.items().timeStop().stopped(level)||game.items().timeStop().frozen(marker))continue;
            if(!state.nuclear){compressed(marker,state);continue;}if(!state.armed)arm(marker,state);beacon(marker,state);
        }
        for(var level:List.copyOf(deadlines.keySet())){
            if(game.items().timeStop().stopped(level)||born.getOrDefault(level,-1)==game.server().getTickCount())continue;
            int remaining=deadlines.get(level)-1;deadlines.put(level,remaining);if(remaining>0)continue;deadlines.remove(level);born.remove(level);
            var sites=new ArrayList<Map.Entry<Marker,State>>();for(var entry:markers.entrySet())if(entry.getKey().level()==level&&entry.getValue().nuclear&&entry.getValue().armed&&!entry.getKey().isRemoved())sites.add(Map.entry(entry.getKey(),entry.getValue()));
            for(var site:sites){game.items().nukeEvent().legacyBurst(level,site.getKey().position(),site.getValue().cause);markers.remove(site.getKey());site.getKey().discard();}
        }
    }
    void loaded(Entity actor){
        if(MatchActorCheckpoint.restoring(actor))return;
        if(!(actor instanceof Marker marker)||!actor.entityTags().contains(TAG))return;if(!game.playing()||!game.items().currentEpoch(actor)){actor.discard();return;}
        for(var entry:List.copyOf(markers.entrySet()))if(entry.getKey()!=marker&&entry.getKey().getUUID().equals(marker.getUUID())){markers.remove(entry.getKey());markers.put(marker,entry.getValue());break;}
    }
    void unloaded(Entity actor){}
    void stop(){var old=new ArrayList<Entity>(markers.keySet());old.addAll(children);markers.clear();children.clear();deadlines.clear();born.clear();old.forEach(Entity::discard);}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        record Image(MatchDamageSources.Ref marker,State state,boolean armed){}
        var saved=markers.entrySet().stream().map(e->new Image(links.key(e.getKey()),e.getValue(),e.getValue().armed)).toList();var descendants=children.stream().map(links::key).toList();var clocks=new IdentityHashMap<>(deadlines);var phases=new IdentityHashMap<ServerLevel,Integer>();born.forEach((level,tick)->phases.put(level,game.server().getTickCount()-tick));
        return current->{var next=new IdentityHashMap<Marker,State>();for(var image:saved)next.put((Marker)current.get(image.marker),image.state);var bodies=descendants.stream().map(ref->(PrimedTnt)current.get(ref)).toList();return ()->{markers.clear();markers.putAll(next);for(var image:saved)image.state.armed=image.armed;children.clear();children.addAll(bodies);deadlines.clear();deadlines.putAll(clocks);born.clear();phases.forEach((level,age)->born.put(level,game.server().getTickCount()-age));};};
    }
}
