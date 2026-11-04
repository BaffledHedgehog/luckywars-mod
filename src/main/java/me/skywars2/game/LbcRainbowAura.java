package me.skywars2.game;

import com.mojang.math.Transformation;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Skill32. Private state contains control/cosmetics only; native owners retain resources and terrain. */
public final class LbcRainbowAura {
    public enum Color { RED, ORANGE, YELLOW, LIME, AQUA, BLUE, PURPLE }
    static final Identifier ATTACK=Identifier.parse("minecraft:uhf78arf7ahfkdhmfiashfdrehf");
    static final Identifier ARMOR=Identifier.parse("minecraft:uhf78arf7ahfkdhmfiash");
    static final Identifier TOUGHNESS=Identifier.parse("minecraft:uhf78arf7ashfdrehf");
    static final Identifier KNOCKBACK=Identifier.parse("minecraft:ahfkdhmfiashfdrehf");
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs_only"));
    private static final ItemParticleOption RED=new ItemParticleOption(ParticleTypes.ITEM,Items.REDSTONE_BLOCK);
    private static final ItemParticleOption GOLD=new ItemParticleOption(ParticleTypes.ITEM,Items.RAW_GOLD_BLOCK);
    private static final DustParticleOptions BLUE=new DustParticleOptions(0x3e2af7,.8f);
    private static final DustParticleOptions PURPLE=new DustParticleOptions(0xbe42ce,.8f);
    private static final Vec3[] AXES={new Vec3(.1,0,0),new Vec3(0,.1,0),new Vec3(0,0,.1),
            new Vec3(-.1,0,0),new Vec3(0,-.1,0),new Vec3(0,0,-.1)};
    private static final int[][] NEIGHBORS={{1,0},{-1,0},{0,1},{0,-1}};
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private final GameController game;
    private final Predicate<ServerPlayer> ownerAdmission;
    private final BiPredicate<ServerPlayer,LivingEntity> targetAdmission;
    private final LinkedHashMap<UUID,State> owners=new LinkedHashMap<>();
    private final Set<Entity> spawning=Collections.newSetFromMap(new IdentityHashMap<>());
    private Dispatcher queued;
    private int lastTick=Integer.MIN_VALUE,turn;
    private boolean restoring;
    private final Map<UUID,ArrayDeque<Boolean>> incomingAttempts=new HashMap<>();

    private static final class Walker {
        final boolean blue;
        Vec3 position,direction;
        Walker(boolean blue,Vec3 position,Vec3 direction){this.blue=blue;this.position=position;this.direction=direction;}
        Walker(Walker w){this(w.blue,w.position,w.direction);}
    }
    private static final class Gram {
        final UUID actor;
        int distance;
        boolean reverse;
        Gram(UUID actor){this.actor=actor;}
        Gram(Gram g){actor=g.actor;distance=g.distance;reverse=g.reverse;}
    }
    /** One accepted source generation. Queued cursors stretch logical time under shared load. */
    private static final class Frame {
        final Color color;
        final Vec3 position;
        final float yaw;
        final long ticker;
        final int phase;
        int stage,index,walker,sample,gram;
        final int walkers,grams;
        final BlockPos support;
        final MapManager.BlockSnapshot copy;
        Frame(State s,ServerPlayer p,long ticker,int phase,MapManager.BlockSnapshot copy){
            color=s.color;position=p.position();yaw=p.getYRot();this.ticker=ticker;this.phase=phase;
            walkers=s.walkers.size();grams=s.grams.size();support=BlockPos.containing(position.add(0,-.5,0));this.copy=copy;
        }
        Frame(Frame f){color=f.color;position=f.position;yaw=f.yaw;ticker=f.ticker;phase=f.phase;stage=f.stage;
            index=f.index;walker=f.walker;sample=f.sample;gram=f.gram;walkers=f.walkers;grams=f.grams;support=f.support;copy=copy(f.copy);}
    }
    private static final class State {
        final UUID owner;
        final ServerLevel level;
        final String epoch;
        Color color=Color.RED;
        long random,ticker;
        int phase;
        boolean incoming,seeded,applied;
        final ArrayList<Walker> walkers=new ArrayList<>();
        final ArrayList<Gram> grams=new ArrayList<>();
        Frame frame;
        State(ServerPlayer p,String epoch){owner=p.getUUID();level=p.level();this.epoch=epoch;random=p.getRandom().nextLong();}
        State(State s){owner=s.owner;level=s.level;epoch=s.epoch;color=s.color;random=s.random;ticker=s.ticker;phase=s.phase;seeded=s.seeded;applied=s.applied;
            s.walkers.forEach(w->walkers.add(w==null?null:new Walker(w)));s.grams.forEach(g->grams.add(g==null?null:new Gram(g)));
            frame=s.frame==null?null:new Frame(s.frame);}
        int random(int bound){long u,r;do{random+=0x9e3779b97f4a7c15L;long z=random;
            z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;
            u=(z^(z>>>31))>>>1;r=u%bound;}while(u-r+(bound-1)<0);return (int)r;}
    }
    public LbcRainbowAura(GameController game,Predicate<ServerPlayer> ownerAdmission,
            BiPredicate<ServerPlayer,LivingEntity> targetAdmission){
        this.game=game;this.ownerAdmission=Objects.requireNonNull(ownerAdmission);
        this.targetAdmission=Objects.requireNonNull(targetAdmission);
    }
    private boolean selected(ServerPlayer p){return game.lbc().hasSkill(p,32)&&ownerAdmission.test(p)
            &&LbcDimensionItems.ordinary(p.level())&&!p.isRemoved()&&!game.isObserver(p);}
    private boolean paused(ServerPlayer p){return !game.server().tickRateManager().runsNormally()
            ||game.server().tickRateManager().isFrozen()||game.items().timeStop().stopped(p.level())
            ||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p);}
    /** Call from the existing selection/reconcile event after native skill selection is committed. */
    public void selected(ServerPlayer p,boolean enabled){
        if(restoring||MatchPlayerCheckpoint.restoring(p))return;
        if(!enabled||!selected(p)){clear(p);return;}
        var previous=owners.get(p.getUUID());
        if(previous!=null&&previous.level==p.level()&&game.items().currentEpoch(previous.epoch))return;
        if(previous!=null)clear(p);
        var state=new State(p,game.items().epoch());owners.put(p.getUUID(),state);
        state.applied=!game.items().magic().suppressed(p);if(state.applied)enter(p,Color.RED,true);
    }
    /** Criterion admission is captured before hurt, so fatal accepted events retain their source selection. */
    public boolean captureIncoming(ServerPlayer p){return !restoring&&owners.containsKey(p.getUUID())&&selected(p)&&!paused(p)&&!game.items().magic().suppressed(p);}
    public boolean withIncomingAttempt(ServerPlayer p,java.util.function.BooleanSupplier action){
        if(!owners.containsKey(p.getUUID()))return action.getAsBoolean();
        var stack=incomingAttempts.computeIfAbsent(p.getUUID(),id->new ArrayDeque<>());
        stack.push(captureIncoming(p));
        try{return action.getAsBoolean();}finally{stack.pop();if(stack.isEmpty())incomingAttempts.remove(p.getUUID());}
    }
    public void incoming(ServerPlayer p,DamageSource source,float original,float actual,boolean blocked,boolean admittedBeforeHurt){
        if(!admittedBeforeHurt||restoring)return;
        var s=owners.get(p.getUUID());if(s==null||s.incoming||s.level!=p.level()||!game.items().currentEpoch(s.epoch))return;
        s.incoming=true;
        try{if(s.applied)leave(p,s.color);s.color=Color.values()[(s.color.ordinal()+1)%7];
            s.applied=!game.items().magic().suppressed(p);if(s.applied)enter(p,s.color,true);}
        finally{s.incoming=false;}
    }
    /** Convenience only for criterion callbacks whose caller preserves pre-hurt selection separately. */
    public void incoming(ServerPlayer p,DamageSource source,float original,float actual,boolean blocked){
        var stack=incomingAttempts.get(p.getUUID());
        incoming(p,source,original,actual,blocked,stack==null||stack.isEmpty()?captureIncoming(p):stack.peek());
    }
    private static void modifier(ServerPlayer p,Holder<Attribute> type,Identifier id,double amount){
        var a=p.getAttribute(type);if(a!=null){a.removeModifier(id);a.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));}
    }
    private static void remove(ServerPlayer p,Holder<Attribute> type,Identifier id){var a=p.getAttribute(type);if(a!=null)a.removeModifier(id);}
    private void enter(ServerPlayer p,Color color,boolean sound){
        String name;float pitch;
        switch(color){
            case RED->{modifier(p,Attributes.ATTACK_DAMAGE,ATTACK,5);name="minecraft:entity.wolf.growl";pitch=2;}
            case ORANGE->{name="minecraft:item.firecharge.use";pitch=1;}
            case YELLOW->{modifier(p,Attributes.ARMOR,ARMOR,40);modifier(p,Attributes.ARMOR_TOUGHNESS,TOUGHNESS,40);
                modifier(p,Attributes.KNOCKBACK_RESISTANCE,KNOCKBACK,100);name="minecraft:block.bell.resonate";pitch=.8f;}
            case LIME->{name="minecraft:item.honey_bottle.drink";pitch=.8f;}
            case AQUA->{game.items().mana().baseRegen(p,game.items().mana().baseRegen(p)+5);name="minecraft:item.honey_bottle.drink";pitch=.8f;}
            case BLUE->{name="minecraft:entity.lightning_bolt.thunder";pitch=2;}
            case PURPLE->{name="minecraft:block.stone.break";pitch=0;}
            default->throw new IllegalStateException();
        }
        if(sound)LbcOverload.sound(p.level(),p.position(),name,1,pitch,0,0);
    }
    private void leave(ServerPlayer p,Color color){
        switch(color){
            case RED->remove(p,Attributes.ATTACK_DAMAGE,ATTACK);
            case YELLOW->{remove(p,Attributes.ARMOR,ARMOR);remove(p,Attributes.ARMOR_TOUGHNESS,TOUGHNESS);remove(p,Attributes.KNOCKBACK_RESISTANCE,KNOCKBACK);}
            case AQUA->game.items().mana().baseRegen(p,game.items().mana().baseRegen(p)-5);
            default->{}
        }
    }
    /** Shared regeneration starts from this command-base contribution before item SET/MULT/ADD. */
    public double regenerationBase(ServerPlayer p){var s=owners.get(p.getUUID());return !restoring&&s!=null&&s.color==Color.LIME
            &&s.level==p.level()&&game.items().currentEpoch(s.epoch)&&selected(p)&&!paused(p)&&!game.items().magic().suppressed(p)?.025:0;}
    public Color color(ServerPlayer p){var s=owners.get(p.getUUID());return s==null?null:s.color;}
    /** Parent passes the existing source ticker and shared 0..99 cadence; no second clock. */
    public void tick(long sourceTicker,int sourcePhase){
        int tick=game.server().getTickCount();
        if(restoring||lastTick==tick||!game.playing()||game.maps().busy()||!game.server().tickRateManager().runsNormally())return;
        lastTick=tick;
        for(var id:List.copyOf(owners.keySet())){
            var s=owners.get(id);var p=game.server().getPlayerList().getPlayer(id);
            // Real disconnect must call clear(player) before native player removal, to reverse aqua.
            if(p==null){retire(s);owners.remove(id);continue;}
            if(!selected(p)||!p.isAlive()||s.level!=p.level()||!game.items().currentEpoch(s.epoch)){clear(p);continue;}
            boolean enabled=!game.items().magic().suppressed(p);
            if(s.applied!=enabled){if(enabled)enter(p,s.color,false);else leave(p,s.color);s.applied=enabled;}
            // No new copy is admitted in MAG. Finish only the bounded copy already partly written.
            if(!enabled&&s.frame!=null&&s.frame.color==Color.PURPLE&&s.frame.stage==0&&s.frame.copy!=null&&s.frame.index<=1)s.frame=null;
            if(!enabled||paused(p)||s.frame!=null)continue;
            // Sample support only once after target-independent owner admission. Never clears it.
            MapManager.BlockSnapshot copy=null;
            var support=BlockPos.containing(p.getX(),p.getY()-.5,p.getZ());
            if(s.color==Color.PURPLE&&p.onGround()&&loaded(s.level,support)
                    &&!s.level.getBlockState(BlockPos.containing(p.getX(),p.getY()-.1,p.getZ())).isAir())copy=supportCopy(s.level,support);
            // Preserve every cadence while work stretches logical time; no skipped phase or catch-up burst.
            if(!s.seeded){s.ticker=sourceTicker;s.phase=Math.floorMod(sourcePhase,100);s.seeded=true;}
            s.frame=new Frame(s,p,s.ticker,s.phase,copy);s.ticker++;s.phase=(s.phase+1)%100;
        }
        ensureQueued();
    }
    private void ensureQueued(){if(queued==null&&owners.values().stream().anyMatch(s->s.frame!=null)){
        queued=new Dispatcher();game.items().stonekivi().queuedWork(queued);
    }}
    private final class Dispatcher implements LbcStonekivi.Work {
        boolean cancelled,stepping;
        private boolean empty(){return owners.values().stream().noneMatch(s->s.frame!=null);}
        @Override public boolean ready(){return !restoring&&(cancelled||empty()||owners.values().stream().anyMatch(s->{
            var p=game.server().getPlayerList().getPlayer(s.owner);return s.frame!=null&&p!=null&&!paused(p)&&(!game.items().magic().suppressed(p)||drainingCopy(s));}));}
        /** Typed cosmetic/control cursors can rewind; accepted permanent block copies must drain. */
        @Override public boolean snapshotIdle(){return !stepping&&owners.values().stream().noneMatch(s->
                s.frame!=null&&s.frame.color==Color.PURPLE&&s.frame.stage==0&&s.frame.copy!=null);}
        @Override public boolean step(){
            if(cancelled)return true;
            stepping=true;
            try{
                var states=new ArrayList<>(owners.values());
                for(int i=0;i<states.size();i++){
                    var s=states.get(Math.floorMod(turn++,states.size()));var p=game.server().getPlayerList().getPlayer(s.owner);
                    if(s.frame==null||p==null||paused(p)||game.items().magic().suppressed(p)&&!drainingCopy(s))continue;
                    if(!selected(p)||s.level!=p.level()||!game.items().currentEpoch(s.epoch)){clear(p);break;}
                    stepFrame(p,s);break;
                }
                if(empty()){if(queued==this)queued=null;return true;}return false;
            }finally{stepping=false;}
        }
        @Override public void cancel(){cancelled=true;if(queued==this)queued=null;}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            boolean saved=cancelled;return links->()->{cancelled=saved;if(!cancelled)queued=this;};
        }
    }
    private static boolean drainingCopy(State s){return s.frame!=null&&s.frame.color==Color.PURPLE&&s.frame.stage==0&&s.frame.copy!=null&&s.frame.index>1;}
    private void stepFrame(ServerPlayer p,State s){
        var f=s.frame;
        if(f.stage==0){if(payload(p,s,f)){f.stage=1;f.index=0;}return;}
        if(f.stage==1){
            if(f.walker<f.walkers){
                var w=s.walkers.get(f.walker);
                if(w==null){f.walker++;f.sample=0;return;}
                // Source cosmetic markers sleep with their native chunk; no TTL or forced loading.
                if(!loaded(s.level,BlockPos.containing(w.position))){f.walker++;f.sample=0;return;}
                if(w.blue){particle(s.level,ParticleTypes.ELECTRIC_SPARK,w.position,1,0,0,0,0);particle(s.level,BLUE,w.position,1,0,0,0,0);
                    if(s.random(100)<5)w.direction=AXES[s.random(6)];}
                else particle(s.level,PURPLE,w.position.add(0,.2,0),1,0,0,0,0);
                w.position=w.position.add(w.direction);
                if(++f.sample==7){if(s.random(100)<(w.blue?3:20))s.walkers.set(f.walker,null);f.walker++;f.sample=0;}
                return;
            }
            f.stage=2;
        }
        if(f.stage==2&&f.gram<f.grams){
            var g=s.grams.get(f.gram++);if(g!=null)stepGram(s,g);return;
        }
        s.walkers.removeIf(Objects::isNull);s.grams.removeIf(g->{if(g==null)return true;
            var actor=s.level.getEntity(g.actor);return actor!=null&&actor.isRemoved();});
        s.frame=null;
    }
    /** Each call performs one bounded visual segment or one constant-size gameplay transaction. */
    private boolean payload(ServerPlayer p,State s,Frame f){
        switch(f.color){
            case RED->{
                int arm=f.index/22,point=f.index%22;double angle=-3.0*(f.ticker+90L*arm);
                var at=f.position.add(0,4,0).add(forward(angle).scale(.2*Math.min(point,10)));
                if(point>=11)at=at.add(forward(angle-90).scale(.2*(point-11)));
                particle(s.level,RED,at,0,0,.5,0,1);return ++f.index==88;
            }
            case YELLOW->{
                long n=Math.floorMod(f.ticker,60);double y=(n>=30?60-n:n)*.1;
                particle(s.level,GOLD,f.position.add(0,y,0).add(forward(f.index*51.4).scale(2)),5,.05,.05,.05,0);
                return ++f.index==7;
            }
            case LIME->{particle(s.level,ParticleTypes.TOTEM_OF_UNDYING,f.position.add(0,1,0)
                    .add(forward(f.yaw+f.ticker*18.0)),1,0,0,0,0);return true;}
            case ORANGE->{
                if(f.phase%5!=0)return true;
                if(f.index==0){
                    p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,20,0,false,false,false));
                    // Existing ordinary roles/teams and native hurt own actual acceptance; never ignite.
                    for(var victim:s.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().move(f.position.subtract(p.position())).inflate(3.5),
                            e->e!=p&&e.position().distanceToSqr(f.position)<=12.25&&targetAdmission.test(p,e)))
                        victim.hurtServer(s.level,s.level.damageSources().source(DamageTypes.ON_FIRE,p),1);
                    f.index++;return false;
                }
                int n=f.index-1,arm=n/71,point=n%71;double start=f.yaw+(arm==2?120:0),angle=start+(arm==1?-150:150);
                var at=f.position.add(forward(start).scale(4)).add(forward(angle).scale(point*.1));
                particle(s.level,ParticleTypes.SMALL_FLAME,at,0,0,1,0,1);return ++f.index==214;
            }
            case AQUA->{if(f.phase==0){
                int value=game.items().mana().get(p);if(value<=9500)value+=500;if(value>=9501&&value<=9999)value=10000;
                game.items().mana().set(p,value);spawnGram(p,s,f.position.add(0,8,0));
                LbcOverload.sound(s.level,f.position,"minecraft:ambient.underwater.exit",1,1.5f,0,0);
            }return true;}
            case BLUE->{if(f.phase%20==0){
                var score=game.items().mana().score(p,"nanobots_tier");int value=score.get();
                if(game.items().armorSets().nanobots().chargeAllowed(p)&&value<120)score.set(Math.min(120,value+3));
                for(int i:new int[]{0,2,3,4,5})s.walkers.add(new Walker(true,f.position.add(0,.8,0),AXES[i]));
            }return true;}
            case PURPLE->{
                if(f.index==0){s.walkers.add(new Walker(false,f.position,forward(s.random(361)-180).scale(.1)));f.index++;return false;}
                if(f.copy!=null){var d=NEIGHBORS[f.index-1];copySupport(p,s,f.support.offset(d[0],0,d[1]),f.copy);}
                return ++f.index==5;
            }
            default->throw new IllegalStateException();
        }
    }
    private static Vec3 forward(double yaw){double r=Math.toRadians(yaw);return new Vec3(-Math.sin(r),0,Math.cos(r));}
    private static boolean loaded(ServerLevel level,BlockPos pos){return pos.getY()>=level.getMinY()&&pos.getY()<level.getMaxY()&&level.hasChunkAt(pos);}
    private static <T extends ParticleOptions> void particle(ServerLevel level,T type,Vec3 at,int count,double x,double y,double z,double speed){
        if(loaded(level,BlockPos.containing(at)))level.sendParticles(type,at.x,at.y,at.z,count,x,y,z,speed);
    }
    private static MapManager.BlockSnapshot copy(MapManager.BlockSnapshot snapshot){return snapshot==null?null:
            new MapManager.BlockSnapshot(snapshot.state(),snapshot.entity()==null?null:snapshot.entity().copy());}
    private MapManager.BlockSnapshot supportCopy(ServerLevel level,BlockPos pos){
        var saved=game.maps().snapshot(level,pos);var state=saved.state();var kind=LbcLuckyBlocks.kind(state);
        if(kind!=null)state=switch(kind){case NORMAL->Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
            case NEKO->Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();case LOLI->Blocks.MUSHROOM_STEM.defaultBlockState();};
        if(state.is(Blocks.RED_MUSHROOM_BLOCK)||state.is(Blocks.BROWN_MUSHROOM_BLOCK)||state.is(Blocks.MUSHROOM_STEM))
            for(var face:List.of(HugeMushroomBlock.DOWN,HugeMushroomBlock.UP,HugeMushroomBlock.NORTH,
                    HugeMushroomBlock.SOUTH,HugeMushroomBlock.EAST,HugeMushroomBlock.WEST))state=state.setValue(face,true);
        var tag=saved.entity()==null?null:saved.entity().copy();
        if(tag!=null)tag.remove("Items");
        return new MapManager.BlockSnapshot(state,tag);
    }
    private void copySupport(ServerPlayer p,State s,BlockPos target,MapManager.BlockSnapshot snapshot){
        if(!loaded(s.level,target)||!s.level.getBlockState(target).is(AIRS)||!LbcBedwarsRoleGuards.publication(s.level))return;
        var arena=LbcBedwarsArena.at(s.level);if(arena!=null&&!arena.allowsType(s.level,target,snapshot.state()))return;
        // Copy/clear work starts only after arena admission. Original support is never written.
        var cause=game.items().indirectKills().coldCause(p,true);
        game.items().indirectKills().trapChanges(s.level,cause,()->{
            if(s.level==game.maps().level())game.maps().recordBlockChange(target);
            game.items().indirectKills().supportRemoved(s.level,p,target);
            game.maps().restoreSnapshot(s.level,target,copy(snapshot),FLAGS);
        });
    }
    private void spawnGram(ServerPlayer p,State s,Vec3 position){
        if(!loaded(s.level,BlockPos.containing(position)))return;
        var actor=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,s.level);
        var item=new ItemStack(Items.STICK);item.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:managram"));
        actor.setItemStack(item);actor.setPos(position);actor.setYRot(0);actor.setXRot(0);
        actor.setPosRotInterpolationDuration(1);actor.setTransformationInterpolationDuration(1);
        actor.entityTags().addAll(Set.of("slowraycast","player","managram","skywars2.rainbow_visual"));
        game.items().markCurrentItemEpoch(actor);game.items().timeStop().prepareFrozen(actor);
        spawning.add(actor);try{if(s.level.addFreshEntity(actor)&&!actor.isRemoved())s.grams.add(new Gram(actor.getUUID()));}
        finally{spawning.remove(actor);}
    }
    private void stepGram(State s,Gram g){
        var entity=s.level.getEntity(g.actor);if(!(entity instanceof Display.ItemDisplay actor)||actor.isRemoved())return;
        if(game.items().timeStop().frozen(actor))return;
        g.distance+=g.reverse?-1:5;
        actor.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(g.distance*.25f),new Quaternionf()));
        actor.setYRot(actor.getYRot()+10);if(g.distance==100)g.reverse=true;if(g.distance<=0){actor.discard();
            for(int i=0;i<s.grams.size();i++)if(s.grams.get(i)==g){s.grams.set(i,null);break;}}
    }
    /** Wire into the shared clone adapter's protectedActor; cosmetics never become inert copies. */
    public boolean protectedActor(Entity actor){return spawning.contains(actor)||actor.entityTags().contains("skywars2.rainbow_visual");}
    /** Native load event handles sleeping displays after owner departure without a world/tag scan. */
    public void loaded(Entity actor){
        if(restoring||spawning.contains(actor)||!actor.entityTags().contains("skywars2.rainbow_visual"))return;
        boolean owned=owners.values().stream().anyMatch(s->s.level==actor.level()&&game.items().currentEpoch(s.epoch)
                &&s.grams.stream().anyMatch(g->g!=null&&g.actor.equals(actor.getUUID())));
        if(!owned)actor.discard();
    }
    /** Retire an unavailable cosmetic link; never make future checkpoints wait for its chunk. */
    public void unloaded(Entity actor){
        if(restoring||MatchActorCheckpoint.restoring(actor)||!actor.entityTags().contains("skywars2.rainbow_visual"))return;
        for(var s:owners.values())for(int i=0;i<s.grams.size();i++){
            var g=s.grams.get(i);if(g!=null&&g.actor.equals(actor.getUUID()))s.grams.set(i,null);
        }
    }
    private void retire(State s){for(var gram:s.grams)if(gram!=null){var e=s.level.getEntity(gram.actor);if(e!=null)e.discard();}s.frame=null;s.walkers.clear();s.grams.clear();}
    public void clear(ServerPlayer p){
        if(restoring||MatchPlayerCheckpoint.restoring(p))return;
        var s=owners.remove(p.getUUID());if(s==null)return;if(s.applied)leave(p,s.color);retire(s);
    }
    public void stop(){
        if(queued!=null)queued.cancel();
        for(var s:List.copyOf(owners.values())){var p=game.server().getPlayerList().getPlayer(s.owner);if(p!=null&&s.applied)leave(p,s.color);retire(s);}
        owners.clear();turn=0;lastTick=Integer.MIN_VALUE;restoring=false;
    }
    /** No resource subtraction or native body disposal here: whole coordinator owns those images. */
    public void prepareFrozen(){restoring=true;if(queued!=null)queued.cancel();owners.clear();spawning.clear();}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        private final List<State> saved=owners.values().stream().map(State::new).toList();
        private final int savedTurn=turn;
        Set<UUID> actorIds(){var ids=new LinkedHashSet<UUID>();for(var s:saved)for(var g:s.grams)if(g!=null)ids.add(g.actor);return Set.copyOf(ids);}
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var pending=new LinkedHashSet<UUID>();
            for(var s:saved)for(var g:s.grams)if(g!=null){var e=lookup.apply(g.actor);
                if(!(e instanceof Display.ItemDisplay)||e.level()!=s.level)pending.add(g.actor);}
            var restored=new LinkedHashMap<UUID,State>();for(var s:saved)restored.put(s.owner,new State(s));
            return new MatchFieldCheckpoint.Plan(Set.copyOf(pending),()->{
                if(!pending.isEmpty())throw new IllegalStateException("Rainbow visual links are pending");
                owners.clear();owners.putAll(restored);turn=savedTurn;
            });
        }
    }
}
