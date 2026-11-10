package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/** Native marker progress is the checkpoint: no parallel scoreboard or asynchronous world access. */
public final class LbcRailroad {
    private static final String TAG="skywars2.railroad",AGE=TAG+".age.",DOWN=TAG+".down";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private final GameController game;
    private final Set<Marker> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    LbcRailroad(GameController game){this.game=game;}
    static boolean managed(Entity actor){return actor instanceof Marker&&actor.entityTags().contains(TAG);}
    public boolean use(ServerPlayer player,InteractionHand hand,Vec3 at){
        if(!LbcSpecialEggs.allowed(game,player)||game.maps().busy()||!LbcImbaPlaceableItems.tagged(player.getItemInHand(hand),"railroad")
                ||game.items().magic().mechanismSuppressed(player)||!resident(player.level(),BlockPos.containing(at)))return false;
        var marker=new Marker(EntityTypes.MARKER,player.level());
        marker.setPos(Math.floor(at.x)+.5,Math.floor(at.y+.5)+.5,Math.floor(at.z)+.5);
        var direction=Direction.fromYRot(player.getYRot());marker.setYRot(direction.toYRot());marker.setXRot(0);
        marker.entityTags().addAll(Set.of(TAG,"railroad","mecha","slowraycast"));age(marker,0);
        game.items().markCurrentItemEpoch(marker);
        game.items().spellSources().bind(marker,new LbcSpellSource.Source(marker.getUUID(),player.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(player),Set.of(),true));
        game.items().timeStop().prepareFrozen(marker);
        if(!player.level().addFreshEntity(marker))return false;
        actors.add(marker);player.getItemInHand(hand).consume(1,player);return true;
    }
    private static int age(Marker marker){for(var tag:marker.entityTags())if(tag.startsWith(AGE))return Integer.parseInt(tag.substring(AGE.length()));return 0;}
    private static void age(Marker marker,int age){marker.entityTags().removeIf(t->t.startsWith(AGE));marker.entityTags().add(AGE+age);}
    private static boolean resident(ServerLevel level,BlockPos at){
        for(int x=(at.getX()-2)>>4;x<=(at.getX()+2)>>4;x++)for(int z=(at.getZ()-2)>>4;z<=(at.getZ()+2)>>4;z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private boolean endless(){var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");if(objective==null)return false;var score=board.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("inf_rail"),objective);return score!=null&&score.value()==1;}
    private void write(Marker marker,BlockPos at,BlockState state,boolean strict){var source=game.items().spellSources().source(marker);game.items().writeOwnedTerrain((ServerLevel)marker.level(),at,state,source==null?null:source.cause(),strict);}
    private void end(Marker marker){write(marker,marker.blockPosition(),Blocks.OAK_WOOD.defaultBlockState(),true);write(marker,marker.blockPosition().above(),Blocks.OAK_WOOD.defaultBlockState(),true);marker.discard();actors.remove(marker);}
    public void tick(){
        if(!game.server().tickRateManager().runsNormally())return;
        for(var marker:List.copyOf(actors)){
            var level=(ServerLevel)marker.level();
            if(marker.isRemoved()){actors.remove(marker);continue;}
            if(!game.playing()||!game.items().currentEpoch(marker)){marker.discard();actors.remove(marker);continue;}
            if(game.maps().busy()||game.items().timeStop().stopped(level)||game.items().timeStop().frozen(marker))continue;
            if(game.items().magic().mechanismSuppressedAt(level,marker.position())){marker.discard();actors.remove(marker);continue;}
            if(level.players().stream().noneMatch(p->!p.isSpectator()&&p.distanceToSqr(marker)<=75*75)||!resident(level,marker.blockPosition()))continue;
            int time=age(marker)+1;age(marker,time);
            var forward=Direction.fromYRot(marker.getYRot());var at=marker.blockPosition();
            if(time==1){
                var cart=new Minecart(EntityTypes.MINECART,level);cart.setPos(marker.position().relative(forward,1).add(0,3,0));game.items().markCurrentItemEpoch(cart);level.addFreshEntity(cart);
                write(marker,at.relative(forward,-1),Blocks.OAK_WOOD.defaultBlockState(),true);write(marker,at.relative(forward,-1).above(),Blocks.OAK_WOOD.defaultBlockState(),true);continue;
            }
            boolean infinite=endless();if(time>=200&&!infinite){end(marker);continue;}
            build(marker,forward,time,infinite);
        }
    }
    private void build(Marker marker,Direction forward,int time,boolean infinite){
        var level=(ServerLevel)marker.level();var at=marker.blockPosition();
        LbcOverload.sound(level,marker.position(),"minecraft:block.anvil.place",.5f,1,0,0);
        write(marker,at.below(),(time%7==1?Blocks.REDSTONE_BLOCK:Blocks.OAK_PLANKS).defaultBlockState(),true);
        for(int y=0;y<=2;y++)write(marker,at.above(y),Blocks.AIR.defaultBlockState(),true);
        for(var pos:BlockPos.betweenClosed(at.offset(-2,0,-2),at.offset(2,4,2))){
            var state=level.getBlockState(pos);if(state.is(Blocks.WATER)||state.is(Blocks.LAVA))write(marker,pos,(state.is(Blocks.WATER)?Blocks.GLASS:Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.ORANGE)).defaultBlockState(),false);
        }
        var next=at.relative(forward);boolean straight=true;
        for(int y=1;y>=-29;y--)if(!level.getBlockState(next.above(y)).is(AIRS)){straight=false;break;}
        var front=level.getBlockState(next);var below=level.getBlockState(next.below());
        straight|=front.is(AIRS)&&!below.is(AIRS)||!below.getFluidState().isEmpty();
        int rise=0;
        if(!straight&&!front.is(AIRS)&&at.getY()<level.getMaxY()-2)rise=marker.entityTags().contains(DOWN)?0:1;
        else if(!straight)rise=at.getY()<=(infinite?level.getMinY()+30:30)?0:-1;
        RailShape shape=forward.getAxis()==Direction.Axis.X?RailShape.EAST_WEST:RailShape.NORTH_SOUTH;
        if(rise!=0){var uphill=rise>0?forward:forward.getOpposite();shape=switch(uphill){case NORTH->RailShape.ASCENDING_NORTH;case SOUTH->RailShape.ASCENDING_SOUTH;case WEST->RailShape.ASCENDING_WEST;default->RailShape.ASCENDING_EAST;};}
        write(marker,at,Blocks.POWERED_RAIL.defaultBlockState().setValue(PoweredRailBlock.SHAPE,shape),false);
        if(rise<=0&&time%7==1)for(var support=at.below(2);support.getY()>=level.getMinY()&&level.getBlockState(support).is(AIRS);support=support.below())write(marker,support,Blocks.OAK_LOG.defaultBlockState(),true);
        if(rise<0)marker.entityTags().add(DOWN);else marker.entityTags().remove(DOWN);
        marker.setPos(marker.position().relative(forward,1).add(0,rise,0));
    }
    public void loaded(Entity actor){if(managed(actor)){var marker=(Marker)actor;if(game.items().currentEpoch(marker))actors.add(marker);else marker.discard();}}
    public void unloaded(Entity actor){if(actor instanceof Marker marker)actors.remove(marker);}
    public void stop(){for(var marker:List.copyOf(actors))marker.discard();actors.clear();}
    int count(){return actors.size();}
}
