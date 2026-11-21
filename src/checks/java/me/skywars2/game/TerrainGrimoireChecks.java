package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Actual consumed terrain-book input; the full shared wand suites exercise the terrain work. */
public final class TerrainGrimoireChecks {
    private TerrainGrimoireChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Terrain grimoire: "+why);}
    private static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;
        final BlockPos wall;final BlockState oldWall;
        boolean retain;
        Fixture(GameController game){
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();
            require(game.items().consuming().ready()&&game.items().consuming().count()==0&&game.items().duplicating().count()==0
                    &&game.items().blackHoleVisuals().count()==0,"exclusive prepared terrain families");
            wall=BlockPos.containing(base.owner.getEyePosition().add(0,0,8));oldWall=base.owner.level().getBlockState(wall);
            base.owner.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2|16|32);
        }
        void reset(){
            base.game.items().consuming().stop();base.game.items().duplicating().stop();base.game.items().stonekivi().stop();
            base.owner.removeEffect(MobEffects.LEVITATION);base.owner.removeEffect(MobEffects.SLOW_FALLING);
            base.owner.setYRot(0);base.owner.setXRot(0);base.recording.packets.clear();
        }
        void payload(LbcElementalGrimoires.Kind kind){
            var items=base.game.items();var consuming=items.consuming();var duplicating=items.duplicating();var owner=base.owner;
            switch(kind){
                case CONSUMING -> require(consuming.pendingWorkCount()==1&&!consuming.partitionSnapshot().isEmpty()
                        &&consuming.partitionSnapshot().stream().allMatch(p->p.owner()==owner&&items.currentEpoch(p.epoch())),"shared owned normal ray partitions and terrain work");
                case CONSUMING_AVARICE -> require(consuming.pendingWorkCount()==1&&consuming.partitionSnapshot().isEmpty(),"one native global-gather work item at real ray contact");
                case CONSUMING_SILENCE -> require(consuming.pendingWorkCount()==2&&owner.getEffect(MobEffects.LEVITATION)!=null
                        &&owner.getEffect(MobEffects.LEVITATION).getAmplifier()==30&&owner.getEffect(MobEffects.SLOW_FALLING)!=null,"native silence entity/terrain work and source caster effects");
                case CONSUMING_END -> require(consuming.pendingWorkCount()==1&&consuming.partitionSnapshot().isEmpty(),"one merged five-ray End terrain batch");
                case DUPLICATING_DIVIDE -> require(duplicating.count()==1&&duplicating.markerSnapshot().isEmpty()&&duplicating.pendingWorkCount()==1,"one native divide contact job without a carrier entity");
                case DUPLICATING,DUPLICATING_HOLE,DUPLICATING_EXPAND -> {
                    int mode=kind==LbcElementalGrimoires.Kind.DUPLICATING_HOLE?4:kind==LbcElementalGrimoires.Kind.DUPLICATING_EXPAND?2:1;
                    int lifetime=mode==4?300:mode==2?100:200;double offset=mode==4?8:mode==2?14:7;
                    require(duplicating.markerSnapshot().size()==1,"one native moving center "+kind);
                    var center=duplicating.markerSnapshot().getFirst();
                    require(center.owner()==owner&&items.currentEpoch(center.epoch())&&center.mode()==mode&&center.age()==0
                            &&center.lifetime()==lifetime&&center.point().distanceToSqr(owner.getEyePosition().add(owner.getLookAngle().scale(offset)))<1e-8,"center owner/mode/lifetime/launch position "+kind);
                    require(items.blackHoleVisuals().count()==(mode==4?1:0),"shared shader only for hole mode "+kind);
                }
                default -> throw new IllegalArgumentException("terrain kind "+kind);
            }
            require(consuming.summonSnapshot().isEmpty(),"no forged Consuming Academy trial");
            for(var entity:owner.level().getAllEntities())require(base.before.contains(entity)||entity.getType()!=EntityTypes.SNOWBALL,"no carrier snowball");
        }
        void casts(){
            for(var kind:LbcElementalGrimoires.terrain()){
                base.presentation(kind);
                for(var hand:InteractionHand.values())for(boolean shift:new boolean[]{false,true}){
                    reset();var stack=base.held(kind,hand,1);base.owner.setShiftKeyDown(shift);base.mana.set(base.owner,shift?-500:0);
                    int balance=base.mana.get(base.owner),used=base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item));
                    require(base.input(hand)&&stack.isEmpty()&&base.mana.get(base.owner)==balance,"actual final book/no mana admission or debit "+kind+" "+hand+" shift="+shift);
                    require(base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item))==used+1,"one native use stat "+kind);payload(kind);
                }
            }
            reset();base.owner.setShiftKeyDown(false);var stack=base.held(LbcElementalGrimoires.Kind.DUPLICATING_EXPAND,InteractionHand.MAIN_HAND,2);
            require(base.input(InteractionHand.MAIN_HAND)&&base.input(InteractionHand.MAIN_HAND)&&stack.isEmpty()
                    &&base.game.items().duplicating().markerSnapshot().size()==2,"consecutive books produce independent centers");reset();
            var main=base.held(LbcElementalGrimoires.Kind.CONSUMING,InteractionHand.MAIN_HAND,1);
            var off=base.loot(LbcElementalGrimoires.Kind.DUPLICATING_HOLE);base.owner.setItemInHand(InteractionHand.OFF_HAND,off);
            require(base.input(InteractionHand.MAIN_HAND)&&main.isEmpty()&&off.isEmpty()&&base.game.items().consuming().pendingWorkCount()==1
                    &&base.game.items().duplicating().markerSnapshot().size()==1&&base.game.items().blackHoleVisuals().count()==1,"parallel consumed terrain families");reset();
        }
        void guards(){for(var kind:LbcElementalGrimoires.terrain()){
            var stack=base.held(kind,InteractionHand.MAIN_HAND,2);base.game.items().timeStop().begin(base.owner.level(),40);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"time stop preserves "+kind);}finally{base.game.items().timeStop().end(base.owner.level());}
            base.field=new Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(base.owner.position());base.field.entityTags().add("stopper_magic");
            require(base.owner.level().addFreshEntity(base.field),"antimagic fixture");base.game.items().magic().loaded(base.field);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"antimagic preserves "+kind);}finally{base.removeField();}
            stack=base.held(kind,InteractionHand.OFF_HAND,2);base.game.config().offhandWands=false;
            try{require(!base.input(InteractionHand.OFF_HAND)&&stack.getCount()==2,"disabled offhand preserves "+kind);}finally{base.game.config().offhandWands=true;}
        }}
        @Override public void close(){if(!retain)reset();base.owner.level().setBlock(wall,oldWall,2|16|32);base.retain=retain;base.close();}
    }
    private static String epoch;
    public static void run(GameController game){try(var f=new Fixture(game)){f.casts();f.guards();}SkyWars2.LOGGER.info("[check] PASS terrain-grimoires");}
    public static void stopProbe(GameController game){require(epoch==null,"exclusive retained fixture");try(var f=new Fixture(game)){
        for(var kind:List.of(LbcElementalGrimoires.Kind.CONSUMING,LbcElementalGrimoires.Kind.CONSUMING_END,LbcElementalGrimoires.Kind.DUPLICATING_EXPAND,LbcElementalGrimoires.Kind.DUPLICATING_HOLE)){
            f.base.held(kind,InteractionHand.MAIN_HAND,1);require(f.base.input(InteractionHand.MAIN_HAND),"retained actual input "+kind);
        }
        require(game.items().consuming().pendingWorkCount()==2&&game.items().duplicating().markerSnapshot().size()==2
                &&game.items().blackHoleVisuals().count()==1,"retained owned terrain jobs, centers and shader");
        epoch=game.items().epoch();f.retain=true;
    }SkyWars2.LOGGER.info("[check] PASS terrain-grimoires-stop-probe");}
    public static void cleanup(GameController game){require(epoch!=null,"retained epoch");try{
        require(!game.items().currentEpoch(epoch)&&game.items().consuming().count()==0&&game.items().consuming().pendingWorkCount()==0
                &&game.items().duplicating().count()==0&&game.items().duplicating().pendingWorkCount()==0
                &&game.items().blackHoleVisuals().count()==0,"actual stop clears shared terrain work, centers and shader");
    }finally{epoch=null;}SkyWars2.LOGGER.info("[check] PASS terrain-grimoires-cleanup");}
}
