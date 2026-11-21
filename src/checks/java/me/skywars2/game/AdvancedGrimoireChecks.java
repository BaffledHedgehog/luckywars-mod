package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
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

/** Consumed Homing/Psychoblind/Boltbolt actual input, shared spell contracts and lifecycle. */
public final class AdvancedGrimoireChecks {
    private AdvancedGrimoireChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Advanced grimoire: "+why);}
    private static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;
        final BlockPos wall;final BlockState oldWall;
        Cow victim;boolean retain;
        Fixture(GameController game) {
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();
            require(game.items().homing().count()==0&&game.items().homing().pendingRayCount()==0
                    &&game.items().psycho().count()==0&&game.items().psycho().rayCount(base.owner)==0
                    &&game.items().bolts().count()==0&&game.items().exasperato().pendingSearches()==0,"exclusive native families");
            wall=BlockPos.containing(base.owner.getEyePosition().add(0,0,8));oldWall=base.owner.level().getBlockState(wall);
            base.owner.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2|16|32);
        }
        void drain() {
            for(int i=0;i<32&&base.game.items().exasperato().pendingSearches()>0;i++)base.game.items().exasperato().tick();
            require(base.game.items().exasperato().pendingSearches()==0&&base.game.items().homing().pendingRayCount()==0,"shared resident ray completes");
        }
        void reset() {
            base.owner.stopRiding();base.game.items().homing().stop();base.game.items().psycho().stop();base.game.items().bolts().stop();
            base.game.items().teleport().stop();base.game.items().exasperato().stop();
            if(victim!=null){victim.discard();victim=null;}
            var remove=new ArrayList<Entity>();for(var e:base.owner.level().getAllEntities())if(!base.before.contains(e)
                    &&(e.getType()==EntityTypes.LIGHTNING_BOLT||e instanceof Projectile p&&p.getOwner()==base.owner))remove.add(e);
            remove.forEach(Entity::discard);base.owner.removeEffect(MobEffects.STRENGTH);base.owner.removeEffect(MobEffects.WATER_BREATHING);
            base.recording.packets.clear();
        }
        void payload(LbcElementalGrimoires.Kind kind) {
            var game=base.game;var owner=base.owner;
            switch(kind) {
                case EXAHOMING -> require(game.items().homing().snapshot().size()==10,"ten owned homing fireballs");
                case EXAHOMING_CURSE -> require(victim!=null&&game.items().homing().curseTicks(victim)==1200,"sixty-second curse on actual ray target");
                case EXAHOMING_DESTINY -> require(game.items().homing().destinyCount()==1,"one captured destiny center");
                case EXAHOMING_SISYPHUS -> require(game.items().homing().snapshot().size()==1,"one Sisyphus fireball");
                case PSYCHOBLIND -> require(game.items().psycho().count()==4&&game.items().psycho().snapshot().stream().noneMatch(p->p.invisible||p.spray),"four visible psychic charges");
                case PSYCHOBLIND_INVISIBLE -> require(game.items().psycho().count()==3&&game.items().psycho().snapshot().stream().allMatch(p->p.invisible&&!p.spray),"three invisible psychic charges");
                case PSYCHOBLIND_RAY -> require(game.items().psycho().count()==0&&game.items().psycho().rayCount(owner)==40,"forty queued scream-ray ticks");
                case PSYCHOBLIND_SPRAY -> require(game.items().psycho().count()==1&&game.items().psycho().snapshot().getFirst().spray,"one psychic spray source");
                case BOLTBOLT -> require(game.items().bolts().snapshot().size()==5&&game.items().bolts().snapshot().stream().allMatch(b->b.owner==owner&&b.lifetime==200&&b.strikeInterval==40),"five ten-second wandering bolts, two-second strike interval");
                case BOLTBOLT_METEOR -> require(game.items().bolts().meteorCount()==1&&game.items().bolts().meteorSnapshot().getFirst().owner==owner
                        &&!game.items().bolts().meteorSnapshot().getFirst().display.isRemoved(),"one live owned snow meteor");
                case BOLTBOLT_WATERING -> require(game.items().bolts().waterCount()==5&&game.items().bolts().waterSnapshot().stream().allMatch(w->w.owner==owner)
                        &&owner.getEffect(MobEffects.WATER_BREATHING).getDuration()==4800&&owner.getEffect(MobEffects.STRENGTH).getAmplifier()==2,"five watering sources and source buffs");
                case BOLTBOLT_RACER -> require(game.items().teleport().cloudCount()==1&&owner.getVehicle() instanceof Projectile p&&p.getOwner()==owner
                        &&Math.abs(p.getDeltaMovement().length()-2)<1e-7,"owned wind racer carrier");
                default -> throw new IllegalArgumentException("advanced kind "+kind);
            }
            for(var p:game.items().homing().snapshot())require(p.getOwner()==owner&&game.items().currentEpoch(p),"fireball attribution and epoch");
            for(var p:game.items().psycho().snapshot())require(p.getOwner()==owner&&game.items().currentEpoch(p)&&p.lifetime==100,"psychic attribution and source lifetime");
            for(var e:owner.level().getAllEntities())require(base.before.contains(e)||e.getType()!=EntityTypes.SNOWBALL,"no disposable snowball entity");
        }
        void casts() {
            for(var kind:LbcElementalGrimoires.advanced()) {
                base.presentation(kind);
                for(var hand:InteractionHand.values())for(boolean shift:new boolean[]{false,true}) {
                    reset();var stack=base.held(kind,hand,1);base.owner.setShiftKeyDown(shift);base.mana.set(base.owner,shift?-500:0);
                    if(kind==LbcElementalGrimoires.Kind.EXAHOMING_CURSE){victim=new Cow(EntityTypes.COW,base.owner.level());victim.setPos(base.owner.getEyePosition().add(0,-.5,4));victim.setNoGravity(true);require(base.owner.level().addFreshEntity(victim),"native curse target");}
                    int balance=base.mana.get(base.owner),used=base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item));
                    require(base.input(hand)&&stack.isEmpty()&&base.mana.get(base.owner)==balance,"actual final-book use without mana gate/debit "+kind+" "+hand+" shift="+shift);
                    require(base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item))==used+1,"one use stat "+kind);
                    drain();payload(kind);
                }
            }
            reset();base.owner.setShiftKeyDown(false);
            var main=base.held(LbcElementalGrimoires.Kind.PSYCHOBLIND,InteractionHand.MAIN_HAND,1);
            var off=base.loot(LbcElementalGrimoires.Kind.BOLTBOLT_WATERING);base.owner.setItemInHand(InteractionHand.OFF_HAND,off);
            require(base.input(InteractionHand.MAIN_HAND)&&main.isEmpty()&&off.isEmpty()&&base.game.items().psycho().count()==4
                    &&base.game.items().bolts().waterCount()==5,"parallel mixed consumed families");reset();
            var stack=base.held(LbcElementalGrimoires.Kind.PSYCHOBLIND_RAY,InteractionHand.MAIN_HAND,2);
            require(base.input(InteractionHand.MAIN_HAND)&&base.input(InteractionHand.MAIN_HAND)&&stack.isEmpty()
                    &&base.game.items().psycho().rayCount(base.owner)==80,"consumed scream-ray casts accumulate");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.game.items().psycho().tick();
            require(base.game.items().psycho().rayCount(base.owner)==79&&base.recording.packets.stream().anyMatch(ClientboundLevelParticlesPacket.class::isInstance),"ray continues visibly without a held book");reset();
        }
        void guards() {
            for(var kind:LbcElementalGrimoires.advanced()) {
                var stack=base.held(kind,InteractionHand.MAIN_HAND,2);
                base.game.items().timeStop().begin(base.owner.level(),40);
                try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"time stop preserves "+kind);}
                finally{base.game.items().timeStop().end(base.owner.level());}
                base.field=new Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(base.owner.position());base.field.entityTags().add("stopper_magic");require(base.owner.level().addFreshEntity(base.field),"antimagic fixture");base.game.items().magic().loaded(base.field);
                try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"antimagic preserves "+kind);}
                finally{base.removeField();}
                stack=base.held(kind,InteractionHand.OFF_HAND,2);base.game.config().offhandWands=false;
                try{require(!base.input(InteractionHand.OFF_HAND)&&stack.getCount()==2,"disabled offhand preserves "+kind);}
                finally{base.game.config().offhandWands=true;}
            }
        }
        @Override public void close(){if(!retain)reset();base.owner.level().setBlock(wall,oldWall,2|16|32);base.retain=retain;base.close();}
    }
    private static List<Entity> retained;
    private static String retainedEpoch;
    private static UUID retainedOwner;
    public static void run(GameController game){try(var f=new Fixture(game)){f.casts();f.guards();}SkyWars2.LOGGER.info("[check] PASS advanced-grimoires");}
    public static void stopProbe(GameController game) {
        require(retained==null,"exclusive stop fixture");
        try(var f=new Fixture(game)) {
            for(var kind:List.of(LbcElementalGrimoires.Kind.PSYCHOBLIND,LbcElementalGrimoires.Kind.PSYCHOBLIND_RAY,
                    LbcElementalGrimoires.Kind.EXAHOMING_DESTINY,LbcElementalGrimoires.Kind.EXAHOMING,
                    LbcElementalGrimoires.Kind.BOLTBOLT,LbcElementalGrimoires.Kind.BOLTBOLT_METEOR,LbcElementalGrimoires.Kind.BOLTBOLT_WATERING)) {
                f.base.held(kind,InteractionHand.MAIN_HAND,1);require(f.base.input(InteractionHand.MAIN_HAND),"retained input "+kind);f.drain();
            }
            var actors=new ArrayList<Entity>();actors.addAll(game.items().psycho().snapshot());actors.addAll(game.items().homing().snapshot());
            actors.add(game.items().bolts().meteorSnapshot().getFirst().display);actors.forEach(e->e.entityTags().add("time_stopped"));
            require(actors.size()==15&&game.items().psycho().rayCount(f.base.owner)==40&&game.items().homing().destinyCount()==1&&game.items().bolts().count()==11,"retained shared actors, timers and centers");
            retained=List.copyOf(actors);retainedEpoch=game.items().epoch();retainedOwner=f.base.owner.getUUID();f.retain=true;
        }
        SkyWars2.LOGGER.info("[check] PASS advanced-grimoires-stop-probe");
    }
    public static void cleanup(GameController game) {
        var actors=retained;require(actors!=null,"retained actors");
        try{var owner=game.server().getPlayerList().getPlayer(retainedOwner);
            require(!game.items().currentEpoch(retainedEpoch)&&actors.stream().allMatch(Entity::isRemoved)&&game.items().homing().count()==0
                    &&game.items().psycho().count()==0&&game.items().psycho().rayCount(owner)==0&&game.items().bolts().count()==0,"actual stop clears every consumed shared family");
        }finally{actors.forEach(Entity::discard);retained=null;retainedEpoch=null;retainedOwner=null;}
        SkyWars2.LOGGER.info("[check] PASS advanced-grimoires-cleanup");
    }
}
