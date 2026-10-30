package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native summons/commands/policy graph; active partial ray is explicitly seeded from a real queued ray. */
final class VanitySpellScene {
    final GameController game;final ChefChecks.Fixture base;final LbcSummons summons;
    final Mob first,second;final Cow emitter,victim;final Arrow child,authorShot;
    final LbcSpellSource.Source oldPolicy,newPolicy,authorPolicy;
    final List<Mob> iteration;final BlockPos untouched,erased;
    final int cadence,orderCadence,due,step=4;final Object queued,active;
    static Field field(Object target,String name){try{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    @SuppressWarnings("unchecked") static <T>T get(Object target,String name){try{return (T)field(target,name).get(target);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    static void set(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity spells: "+why);}
    VanitySpellScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;summons=game.items().summons();require(summons.ready()&&summons.count()==0,"exclusive prepared source summons");
        first=summons.spawnPreset(base.owner,"strangeness",base.at.add(-8,1,-5));second=summons.spawnPreset(base.owner,"strangeness",base.at.add(-8,1,5));
        require(first!=null&&second!=null,"actual source cultists");for(var mob:List.of(first,second)){mob.setNoAi(true);mob.setNoGravity(true);mob.entityTags().add("have_stand");}
        var look=gamma.getEyePosition().subtract(base.owner.getEyePosition());base.owner.setYRot((float)Math.toDegrees(Math.atan2(-look.x,look.z)));base.owner.setXRot((float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance())));
        require(summons.orders().lookedAt(base.owner)==gamma&&summons.command(base.owner),"real aimed command selects Gamma");
        require(summons.orders().assigned(first)==gamma&&summons.orders().assigned(second)==gamma,"native commands retain player focus");
        set(summons,"ticks",59);summons.tick();var waiting=VanitySpellScene.<List<Object>>get(summons,"rays");require(waiting.size()==2,"two real source warnings scheduled at tick60");
        active=waiting.remove(0);queued=waiting.getFirst();set(active,"direction",new Vec3(1,0,0));set(active,"step",step);VanitySpellScene.<ArrayDeque<Object>>get(summons,"activeRays").addLast(active);
        cadence=get(summons,"ticks");orderCadence=get(summons.orders(),"ticks");due=get(queued,"due");
        var origin=VanitySpellScene.<Vec3>get(active,"origin");untouched=BlockPos.containing(origin);erased=BlockPos.containing(origin.add(step+1,0,0));write(untouched,Blocks.BEDROCK);write(erased,Blocks.BEDROCK);
        victim=cow(origin.add(8,0,0));victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);
        emitter=cow(base.at.add(9,3,9));var sources=game.items().spellSources();game.items().communism().subjugate(base.owner,emitter,true,true);
        oldPolicy=sources.hostile(emitter,base.owner,Set.of("strict_map_object","spectator"));sources.bind(emitter,oldPolicy);
        child=arrow(base.owner,base.at.add(10,8,8));sources.inherit(emitter,child);
        game.items().communism().subjugate(base.other,emitter,true,true);newPolicy=sources.source(emitter);
        require(newPolicy!=oldPolicy&&newPolicy.author().equals(base.other.getUUID())&&sources.source(child)==oldPolicy,"new controller changes only future emitter attacks");
        authorShot=arrow(gamma,base.at.add(8,8,10));var accepted=sources.hostile(emitter,gamma,Set.of("strict_map_object"));authorPolicy=new LbcSpellSource.Source(accepted.emitter(),accepted.author(),accepted.epoch(),accepted.cause(),accepted.excluded(),true);sources.bind(authorShot,authorPolicy);sources.bind(gamma,authorPolicy);
        summons.track(base.owner,child);summons.track(gamma,authorShot);iteration=summons.snapshot();
    }
    private Cow cow(Vec3 at){var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setNoAi(true);cow.setNoGravity(true);cow.entityTags().add("have_stand");cow.setPos(at);require(base.owner.level().addFreshEntity(cow),"native policy witness");return cow;}
    private Arrow arrow(ServerPlayer author,Vec3 at){var arrow=new Arrow(author.level(),author,new ItemStack(Items.ARROW),null);arrow.setPos(at);arrow.setNoGravity(true);arrow.entityTags().add("have_stand");require(author.level().addFreshEntity(arrow),"native policy child");return arrow;}
    private void write(BlockPos at,Block state){require(base.blocks.containsKey(at),"ray within enclosing terrain");base.owner.level().setBlock(at,state.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
    void verify(ServerPlayer gamma){
        var sources=game.items().spellSources();require(sources.source(emitter)==newPolicy&&sources.source(child)==oldPolicy&&sources.source(authorShot)==authorPolicy&&sources.source(gamma)==authorPolicy,"accepted immutable policies and detached child cause identities restored");
        require(game.items().communism().owner(emitter)==base.other&&sources.author(emitter)==base.other&&sources.author(child)==base.owner&&sources.author(authorShot)==gamma,"control and causal authors resolve current native players independently");
        require(summons.owner(child)==base.owner&&summons.owner(authorShot)==gamma&&authorShot.getOwner()==gamma,"summon consequences and native projectile owners use current bodies");
        // Global stopped time suppresses assigned(), but must not erase its underlying order.
        var assignments=VanitySpellScene.<Map<Mob,?>>get(summons.orders(),"assignments");require(assignments.size()==2&&VanitySpellScene.<Object>get(assignments.get(first),"target")==gamma,"commands point to CURRENT replacement Gamma");
        require(summons.snapshot().equals(iteration)&&VanitySpellScene.<Integer>get(summons,"ticks")==cadence&&VanitySpellScene.<Integer>get(summons.orders(),"ticks")==orderCadence,"actor traversal and both cadence counters restored");
        var waiting=VanitySpellScene.<List<Object>>get(summons,"rays");var running=VanitySpellScene.<ArrayDeque<Object>>get(summons,"activeRays");
        require(waiting.size()==1&&running.size()==1&&VanitySpellScene.<Integer>get(waiting.getFirst(),"due")==due&&VanitySpellScene.<Integer>get(running.getFirst(),"step")==step,"waiting delay and partial active cursor restore independently");
    }
    void afterThaw(ServerPlayer gamma){
        require(summons.orders().assigned(first)==gamma&&summons.orders().assigned(second)==gamma,"restored orders work once time resumes");
        for(int i=0;i<10&&!VanitySpellScene.<ArrayDeque<Object>>get(summons,"activeRays").isEmpty();i++)summons.tick();
        require(VanitySpellScene.<ArrayDeque<Object>>get(summons,"activeRays").isEmpty()&&VanitySpellScene.<List<Object>>get(summons,"rays").size()==1,"actual native partial ray completes, waiting warning has not fired early");
        require(base.owner.level().getBlockState(untouched).is(Blocks.BEDROCK)&&base.owner.level().getBlockState(erased).isAir()&&victim.getHealth()<1000&&victim.getLastHurtByPlayer()==base.owner,"native ray resumes at saved cursor, erases terrain and damages/credits the real victim");
        summons.stop();child.discard();authorShot.discard();emitter.discard();victim.discard();
    }
    void pending(){require(summons.count()==0&&VanitySpellScene.<Map<?,?>>get(game.items().spellSources(),"sources").size()==90,"missing target never partially installs captured source/control/summon tables");}
    void mutate(){summons.stop();game.items().spellSources().stop();game.items().communism().stop();for(int i=0;i<90;i++){
        var mob=new Cow(EntityTypes.COW,base.owner.level());game.items().spellSources().bind(mob,oldPolicy);game.items().communism().subjugate(base.owner,mob,true,true);require(summons.spawnPreset(base.owner,"vex",base.at.add(10,3,-10))!=null,"grow native summon identity table in future");
    }summons.stop();}
}
