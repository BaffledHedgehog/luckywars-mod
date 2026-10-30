package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Invisible hands; acquisition and finger-backed respawn remain separate authority edges. */
public final class LbcWitchIndolence {
    private static final String MANAGED="skywars2.witch.indolence_hand";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final DustParticleOptions DUST=new DustParticleOptions(0x300931,3);
    record Pattern(int count,int spread,int lifetime,int cooldown,boolean controlled){}
    static Pattern pattern(int fingers,boolean sneak){
        if(fingers<1)return new Pattern(16,40,60,40,false);
        if(fingers<3)return new Pattern(8,20,60,30,false);
        if(fingers<5)return new Pattern(4,10,60,20,false);
        if(fingers<7)return sneak?new Pattern(3,0,200,15,true):new Pattern(2,5,60,10,false);
        return sneak?new Pattern(1,0,200,20,true):new Pattern(1,0,60,5,false);
    }
    private static final class Hand extends Marker {
        Hand(ServerLevel level){super(EntityTypes.MARKER,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    private static final class Flight {
        LbcSpellSource.Source source;final boolean controlled,jitter;final int lifetime;int age;
        Flight(LbcSpellSource.Source source,Pattern pattern,boolean jitter){this.source=source;controlled=pattern.controlled();lifetime=pattern.lifetime();this.jitter=jitter;}
    }
    private final GameController game;
    private final Map<Entity,Flight> flights=new LinkedHashMap<>();
    private final Map<UUID,Flight> unloaded=new HashMap<>();
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    private boolean advancing;
    private int lastTick=Integer.MIN_VALUE;
    LbcWitchIndolence(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("indolence",0)==1;}
    private boolean live(String epoch){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch);}
    private boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&!game.isObserver(p)&&game.canUseGameplayItems(p)
            &&LbcDimensionItems.ordinary(p)&&!game.items().polymorph().masked(p)&&game.items().wands().validRoleTarget(p,false);}
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    private List<? extends Entity> select(ServerLevel level,Vec3 at,String literal){try{
        var selector=selectors.get(literal);if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();selectors.put(literal,selector);}
        return List.copyOf(selector.findEntities(game.server().createCommandSourceStack().withLevel(level).withPosition(at).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput()));
    }catch(CommandSyntaxException error){throw new IllegalStateException("Indolence source selector "+literal,error);}}
    private ServerPlayer holder(Entity entity){return select((ServerLevel)entity.level(),entity.position(),"@p[tag=sin_indolence,gamemode=!spectator]").stream()
            .filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).filter(this::eligible).findFirst().orElse(null);}
    public boolean use(ServerPlayer p,InteractionHand hand){
        // (user) Both hands require the authority, closing the legacy offhand omission.
        if(!matches(p.getItemInHand(hand))||!eligible(p)||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||!p.entityTags().contains("sin_indolence")||game.items().actionsBlocked(p)||paused(p.level())||game.items().wands().inputTimeStopped(p)
                ||game.items().magic().suppressed(p)||p.entityTags().contains("nomagic_active")
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var counter=game.items().mana().score(p,"indolence_cd");if(counter.get()>0)return false;
        // No distance/AABB in this original selector: loaded fingers in other worlds count too.
        int fingers=select(p.level(),p.position(),"@e[type=minecraft:skeleton,tag=indolence_finger]").size();
        var pattern=pattern(fingers,p.isShiftKeyDown());var at=p.getEyePosition();
        if(!resident(p.level(),at)||game.items().magic().suppressedAt(p.level(),at))return false;
        int accepted=0;
        for(int i=0;i<pattern.count();i++)if(spawn(p,at,pattern,pattern.controlled()&&pattern.count()>1&&i>0)!=null)accepted++;
        if(accepted==0)return false;
        counter.set(pattern.cooldown());LbcOverload.sound(p,at,"minecraft:item.firecharge.use",1,0,0);return true;
    }
    private Entity spawn(ServerPlayer p,Vec3 at,Pattern pattern,boolean jitter){
        var marker=new Hand(p.level());marker.setPos(at);marker.entityTags().addAll(List.of(MANAGED,"slowraycast","magic","other",pattern.controlled()?"controlled_invisible_hand":"invisible_hand"));
        if(jitter)marker.entityTags().add("knockb");if(!pattern.controlled())marker.entityTags().add("active");
        marker.setYRot(p.getYRot());marker.setXRot(p.getXRot());
        if(!pattern.controlled()&&pattern.spread()>0){marker.setYRot(spread(p.getYRot(),pattern.spread(),p.level()));marker.setXRot(spread(p.getXRot(),pattern.spread(),p.level()));}
        game.items().markCurrentItemEpoch(marker);
        var source=new LbcSpellSource.Source(marker.getUUID(),p.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(p),Set.of("sin_indolence","spectator","strict_map_object","rcdenyalways","magic_academy"),true);
        var state=new Flight(source,pattern,jitter);flights.put(marker,state);game.items().spellSources().bind(marker,source);game.items().timeStop().prepareFrozen(marker);
        if(p.level().addFreshEntity(marker)&&!marker.isRemoved())return marker;flights.remove(marker);marker.discard();return null;
    }
    private static float spread(float angle,int bound,ServerLevel level){return Mth.floor(angle)+level.getRandom().nextInt(2*bound+1)-bound;}
    private static boolean resident(ServerLevel level,Vec3 at){var cell=BlockPos.containing(at);return level.getWorldBorder().isWithinBounds(cell)
            &&level.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)!=null;}
    public void tick(){if(advancing)throw new IllegalStateException("Reentrant authority advance");advancing=true;try{
        int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        for(var p:game.server().getPlayerList().getPlayers())if(game.playing()&&eligible(p)&&!paused(p.level())&&!game.items().wands().inputTimeStopped(p)){
            var objective=game.server().getScoreboard().getObjective("indolence_cd");var score=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(p,objective);
            if(score!=null&&score.value()>0)game.items().mana().score(p,"indolence_cd").set(score.value()-1);
        }
        for(var entry:List.copyOf(flights.entrySet())){
            var marker=entry.getKey();var state=entry.getValue();var level=(ServerLevel)marker.level();
            if(marker.isRemoved()){flights.remove(marker);continue;}
            if(!live(state.source.epoch())||!LbcDimensionItems.ordinary(level)||!resident(level,marker.position())||game.items().magic().suppressedAt(level,marker.position())){retire(marker);continue;}
            if(paused(level)||game.items().timeStop().frozen(marker))continue;
            var owner=holder(marker);
            if(owner!=null&&!state.source.author().equals(owner.getUUID())){
                state.source=new LbcSpellSource.Source(marker.getUUID(),owner.getUUID(),state.source.epoch(),game.items().indirectKills().wildCause(owner),state.source.excluded(),true);
                game.items().spellSources().bind(marker,state.source);
            }
            if(state.controlled){
                if(owner!=null){marker.setYRot(owner.getYRot());marker.setXRot(owner.getXRot());}
                if(state.jitter){marker.setYRot(spread(marker.getYRot(),60,level));marker.setXRot(spread(marker.getXRot(),60,level));}
                var next=marker.position().add(marker.getLookAngle().scale(.3));
                if(sample(marker,state,owner,next))marker.setPos(next);
            }else{
                var from=marker.position();var direction=marker.getLookAngle();boolean clear=true;
                for(int i=0;i<3&&clear;i++)clear=sample(marker,state,owner,from.add(direction.scale(i*.33)));
                if(clear)marker.setPos(from.add(direction));
            }
            if(!marker.isRemoved()&&++state.age>=state.lifetime)retire(marker);
        }
        unloaded.values().removeIf(f->!live(f.source.epoch()));
    }finally{advancing=false;}}
    private boolean sample(Entity marker,Flight state,ServerPlayer owner,Vec3 at){
        var level=(ServerLevel)marker.level();
        if(!resident(level,at)||game.items().magic().suppressedAt(level,at)){retire(marker);return false;}
        for(var viewer:level.players()){
            // Two source commands intentionally send twice to a holder of both authorities.
            if(viewer.entityTags().contains("sin_indolence"))particle(level,viewer,at);
            if(viewer.entityTags().contains("sin_vanity"))particle(level,viewer,at);
        }
        if(owner!=null)for(var e:select(level,at,"@e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!spectator,tag=!sin_indolence]"))
            if(e instanceof LivingEntity target&&game.items().spellSources().target(state.source,target)&&!game.items().polymorph().concealed(target)
                    &&!game.items().magic().suppressedAt(level,target.position()))
                target.hurtServer(level,game.items().indirectKills().captureDamage(marker.damageSources().source(DamageTypes.ARROW,marker,owner),state.source.cause()),3);
        if(!level.getBlockState(BlockPos.containing(at)).is(AIRS)){
            var first=BlockPos.containing(at.add(-.3,-.3,-.3));var last=BlockPos.containing(at.add(.3,.3,.3));
            game.items().indirectKills().capturedWorldChanges(level,state.source.cause(),()->{
                for(var cell:BlockPos.betweenClosed(first,last))if(resident(level,Vec3.atCenterOf(cell)))level.destroyBlock(cell,true);
            });retire(marker);return false;
        }
        return true;
    }
    private static void particle(ServerLevel level,ServerPlayer viewer,Vec3 at){level.sendParticles(viewer,DUST,true,false,at.x,at.y,at.z,1,0,0,0,0);}
    private void retire(Entity marker){flights.remove(marker);marker.discard();}
    public void loaded(Entity e){if(!e.entityTags().contains(MANAGED))return;var state=flights.get(e);if(state==null)state=unloaded.remove(e.getUUID());
        if(state==null||!live(state.source.epoch())){e.discard();return;}flights.put(e,state);game.items().spellSources().bind(e,state.source);game.items().timeStop().prepareFrozen(e);}
    public void unloaded(Entity e){var state=flights.remove(e);if(state!=null&&e.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(e.getUUID(),state);}
    public void stop(){for(var e:List.copyOf(flights.keySet()))e.discard();flights.clear();unloaded.clear();lastTick=Integer.MIN_VALUE;}
    List<Entity> snapshot(){return List.copyOf(flights.keySet());}
    int age(Entity entity){var state=flights.get(entity);return state==null?-1:state.age;}
    void checkpointBoundary(){if(advancing)throw new IllegalStateException("Finish invisible-hand callbacks first");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private record Image(LbcSpellSource.Source source,boolean controlled,boolean jitter,int lifetime,int age) { }
        private final Map<MatchDamageSources.Ref,Image> active=new LinkedHashMap<>();private final Map<UUID,Image> dormant=new HashMap<>();
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        Checkpoint(MatchSpellCheckpoint.Links links){var aliases=new IdentityHashMap<Flight,Image>();flights.forEach((actor,state)->active.put(links.key(actor),image(state,aliases)));unloaded.forEach((id,state)->dormant.put(id,image(state,aliases)));}
        private Image image(Flight f,IdentityHashMap<Flight,Image> aliases){return aliases.computeIfAbsent(f,x->new Image(x.source,x.controlled,x.jitter,x.lifetime,x.age));}
        private Flight flight(Image image,IdentityHashMap<Image,Flight> aliases){return aliases.computeIfAbsent(image,x->{var f=new Flight(x.source,new Pattern(0,0,x.lifetime,0,x.controlled),x.jitter);f.age=x.age;return f;});}
        Runnable prepare(MatchSpellCheckpoint.Links links){var aliases=new IdentityHashMap<Image,Flight>();var actors=new LinkedHashMap<Entity,Flight>();var asleep=new HashMap<UUID,Flight>();
            active.forEach((ref,image)->actors.put(links.get(ref),flight(image,aliases)));dormant.forEach((id,image)->asleep.put(id,flight(image,aliases)));
            return ()->{flights.clear();flights.putAll(actors);unloaded.clear();unloaded.putAll(asleep);lastTick=clock.last(game);};}
    }

}
