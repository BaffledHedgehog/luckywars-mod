package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Actual consumed Erecto/Fireblaze/Stonekivi input and shared native work lifecycle. */
public final class ShapingGrimoireChecks {
    private ShapingGrimoireChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Shaping grimoire: "+why);}
    private static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;final BlockPos wall;final BlockState oldWall;final int ray;
        final Cow target;boolean retain;
        Fixture(GameController game) {
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();
            require(game.items().stonekivi().ready()&&game.items().stonekivi().count()==0&&game.items().blaze().count()==0
                    &&game.items().erecto().sphereCount()==0,"exclusive prepared families");
            ray=base.mana.score(base.owner,"ray").get();base.mana.score(base.owner,"ray").set(0);
            wall=BlockPos.containing(base.owner.getEyePosition().add(0,0,8));oldWall=base.owner.level().getBlockState(wall);
            base.owner.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2|16|32);
            target=new Cow(EntityTypes.COW,base.owner.level());target.setPos(base.owner.position().add(4,0,5));target.setNoGravity(true);
            require(base.owner.level().addFreshEntity(target),"native nearby fire target");
        }
        List<Projectile> shots(){var out=new ArrayList<Projectile>();for(var e:base.owner.level().getAllEntities())if(!base.before.contains(e)&&!e.isRemoved()&&e instanceof Projectile p&&p.getOwner()==base.owner)out.add(p);return out;}
        void reset(){
            base.game.items().erecto().stop();base.game.items().wands().stop();base.game.items().blaze().stop();base.game.items().stonekivi().stop();
            shots().forEach(Entity::discard);base.owner.removeEffect(MobEffects.FIRE_RESISTANCE);base.owner.setYRot(0);base.owner.setXRot(0);
            base.recording.packets.clear();
        }
        void payload(LbcElementalGrimoires.Kind kind) {
            var items=base.game.items();var owner=base.owner;
            switch(kind) {
                case ERECTO -> require(items.wands().electroCount()==9&&items.wands().electroSnapshot().stream().allMatch(t->t.owner==owner&&t.erecto!=null&&t.erecto.stack.isEmpty()),"nine owned central/side arcs, no Academy trial stack");
                case ERECTO_RAY -> require(items.erecto().rayPending(owner)==40&&items.wands().electroCount()==0,"forty stored ray ticks");
                case ERECTO_SNIPER -> require(items.wands().electroCount()==1&&owner.getXRot()==-15&&items.wands().electroSnapshot().getFirst().erecto!=null,"one sniper discharge and source recoil");
                case ERECTO_SPHERE -> require(items.erecto().sphereCount()==1&&items.erecto().sphereSnapshot().getFirst().owner==owner,"one contact sphere");
                case FIREBLAZE,FIREBLAZE_MEGA,FIREBLAZE_WORLD -> {
                    require(shots().size()>=1&&shots().stream().allMatch(p->base.game.items().currentEpoch(p)),"owned native fire volley "+kind);
                    require(shots().stream().allMatch(p->p.getType()==(kind==LbcElementalGrimoires.Kind.FIREBLAZE_MEGA?EntityTypes.FIREBALL:EntityTypes.SMALL_FIREBALL)),"correct fireball type "+kind);
                }
                case FIREBLAZE_KIUAS,FIREBLAZE_PILLARS -> require(items.blaze().count()==1&&owner.getEffect(MobEffects.FIRE_RESISTANCE)!=null
                        &&owner.getEffect(MobEffects.FIRE_RESISTANCE).isInfiniteDuration(),"captured terrain job and infinite source protection "+kind);
                case STONEKIVI,STONEKIVI_EARTH,STONEKIVI_SUN,STONEKIVI_COMET -> require(items.stonekivi().count()==1,"one native contact work sequence "+kind);
                default -> throw new IllegalArgumentException("shaping kind "+kind);
            }
            for(var e:owner.level().getAllEntities())require(base.before.contains(e)||e.getType()!=EntityTypes.SNOWBALL,"no carrier snowball");
        }
        void casts(){
            for(var kind:LbcElementalGrimoires.shaping()) {
                base.presentation(kind);
                for(var hand:InteractionHand.values())for(boolean shift:new boolean[]{false,true}) {
                    reset();var stack=base.held(kind,hand,1);base.owner.setShiftKeyDown(shift);base.mana.set(base.owner,shift?-500:0);int balance=base.mana.get(base.owner);
                    int used=base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item));
                    require(base.input(hand)&&stack.isEmpty()&&base.mana.get(base.owner)==balance,"actual final book, no admission/debit "+kind+" "+hand+" shift="+shift);
                    require(base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item))==used+1,"one native use stat "+kind);payload(kind);
                }
            }
            reset();base.owner.setShiftKeyDown(false);var stack=base.held(LbcElementalGrimoires.Kind.ERECTO_SNIPER,InteractionHand.MAIN_HAND,1);base.owner.setXRot(90);base.mana.set(base.owner,-10);
            require(base.input(InteractionHand.MAIN_HAND)&&stack.isEmpty()&&base.game.items().wands().electroCount()==1
                    &&base.mana.get(base.owner)==-10,"source library sniper discharges downward without paid-wand assist/refund");reset();
            stack=base.held(LbcElementalGrimoires.Kind.ERECTO_RAY,InteractionHand.MAIN_HAND,2);
            require(base.input(InteractionHand.MAIN_HAND)&&base.input(InteractionHand.MAIN_HAND)&&stack.isEmpty()&&base.game.items().erecto().rayPending(base.owner)==80,"consumed ray accumulates");
            base.game.items().erecto().tick();require(base.game.items().erecto().rayPending(base.owner)==79,"paid-in-books ray continues after consumption");reset();
            var main=base.held(LbcElementalGrimoires.Kind.ERECTO,InteractionHand.MAIN_HAND,1);var off=base.loot(LbcElementalGrimoires.Kind.STONEKIVI_COMET);base.owner.setItemInHand(InteractionHand.OFF_HAND,off);
            require(base.input(InteractionHand.MAIN_HAND)&&main.isEmpty()&&off.isEmpty()&&base.game.items().wands().electroCount()==9&&base.game.items().stonekivi().count()==1,"parallel independent consumed families");reset();
        }
        void guards(){for(var kind:LbcElementalGrimoires.shaping()) {
            var stack=base.held(kind,InteractionHand.MAIN_HAND,2);base.game.items().timeStop().begin(base.owner.level(),40);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"time stop preserves "+kind);}finally{base.game.items().timeStop().end(base.owner.level());}
            base.field=new Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(base.owner.position());base.field.entityTags().add("stopper_magic");require(base.owner.level().addFreshEntity(base.field),"antimagic fixture");base.game.items().magic().loaded(base.field);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"antimagic preserves "+kind);}finally{base.removeField();}
            stack=base.held(kind,InteractionHand.OFF_HAND,2);base.game.config().offhandWands=false;
            try{require(!base.input(InteractionHand.OFF_HAND)&&stack.getCount()==2,"disabled offhand preserves "+kind);}finally{base.game.config().offhandWands=true;}
        }}
        @Override public void close(){if(!retain)reset();target.discard();base.owner.level().setBlock(wall,oldWall,2|16|32);base.retain=retain;base.close();if(!retain)base.mana.score(base.owner,"ray").set(ray);}
    }
    private static String epoch;private static UUID owner;private static int oldRay;
    public static void run(GameController game){try(var f=new Fixture(game)){f.casts();f.guards();}SkyWars2.LOGGER.info("[check] PASS shaping-grimoires");}
    public static void stopProbe(GameController game){require(epoch==null,"exclusive stop fixture");try(var f=new Fixture(game)){
        for(var kind:List.of(LbcElementalGrimoires.Kind.ERECTO,LbcElementalGrimoires.Kind.ERECTO_RAY,LbcElementalGrimoires.Kind.ERECTO_SPHERE,LbcElementalGrimoires.Kind.FIREBLAZE_KIUAS,LbcElementalGrimoires.Kind.STONEKIVI_EARTH)){
            f.base.held(kind,InteractionHand.MAIN_HAND,1);require(f.base.input(InteractionHand.MAIN_HAND),"retained actual input "+kind);
        }
        require(game.items().erecto().rayPending(f.base.owner)==40&&game.items().erecto().sphereCount()==1&&game.items().stonekivi().count()==1&&game.items().blaze().count()==1,"retained native queues and centers");
        epoch=game.items().epoch();owner=f.base.owner.getUUID();oldRay=f.ray;f.retain=true;
    }SkyWars2.LOGGER.info("[check] PASS shaping-grimoires-stop-probe");}
    public static void cleanup(GameController game){require(epoch!=null,"retained epoch");try{
        var p=game.server().getPlayerList().getPlayer(owner);require(!game.items().currentEpoch(epoch)&&game.items().wands().electroCount()==0&&game.items().stonekivi().count()==0&&game.items().blaze().count()==0&&game.items().erecto().sphereCount()==0&&game.items().erecto().rayPending(p)==0,"actual stop clears shared work and ray");
        game.items().mana().score(p,"ray").set(oldRay);
    }finally{epoch=null;owner=null;}SkyWars2.LOGGER.info("[check] PASS shaping-grimoires-cleanup");}
}
