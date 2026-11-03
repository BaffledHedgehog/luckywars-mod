package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** One native body per merged hole; contributions retain their own author and resource cost. */
public final class LbcVoidArcanum {
    private static final String BODY_TAG="skywars2.void_arcanum";
    // Legacy block footprints, rounded outward to a radial whole-block reach; cube corners stay excluded.
    private static final double[] RADII={0,2,3,5,9,24}, HURT={0,1.5,2,4,8,24}, PULL={0,3,4,8,12,30};
    private static final float[] DAMAGE={0,.2f,.25f,.3f,.35f,.4f};
    // Legacy voidcursed1..4 move .13 per tick; voidcursed5 moves .5.
    private static final double[] ACCELERATION={0,.025,.035,.018,.022,.025}, MAX_SPEED={0,.13,.13,.13,.13,.5};
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    public static final Item ITEM=registerItem();
    private static Item registerItem(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:void_arcanum"));
        var data=new CompoundTag();
        for(String flag:List.of("craftusable","transformable","arcanum","arcanumt","void_curse"))data.putInt(flag,1);
        var props=new Item.Properties(){
            @Override public Identifier effectiveModel(){return Identifier.parse("lbc:arcanumblackhole");}
        }.setId(key).overrideDescription("void_arcanum")
                .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:arcanumblackhole"))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_a"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.REPEATING_COMMAND_BLOCK,true));
    }
    public static void register(){}
    public static boolean isArcanum(ItemStack stack){return stack.is(ITEM);}
    private final GameController game;
    private final List<Hole> holes=new ArrayList<>();
    private final Map<UUID,Hands> hands=new HashMap<>();
    private int terrainTurn,costClock;
    private int callbackDepth;private boolean rewinding;
    private final List<Hole> restoreVisuals=new ArrayList<>();
    private final Map<Hole,UUID> restoreVisualOwners=new HashMap<>();
    private static final class Hands {boolean main,off;}
    record Contribution(UUID owner,LbcIndirectKills.Owner cause){}
    static final class Hole {
        final ServerLevel world;Marker marker;final String epoch;
        final List<Contribution> members=new ArrayList<>();
        Vec3 point;int level=1,cloneMask;boolean wild;
        Vec3 velocity=Vec3.ZERO;
        final Quaternionf tilt=new Quaternionf();
        BlackHoleVisuals.Handle visual;
        BlackHoleErosion terrain;int frontier=1,terrainLevel;
        LbcIndirectKills.Owner terrainCause;
        Hole(ServerLevel world,Marker marker,Vec3 point,String epoch){this.world=world;this.marker=marker;this.point=point;this.epoch=epoch;}
    }
    /** A native body is never serialized; a stale marker from any old version is rejected on load. */
    private static final class Body extends Marker {
        Body(ServerLevel world){super(EntityTypes.MARKER,world);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    public static final class WildDamage extends DamageSource {
        WildDamage(ServerPlayer owner){super(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC),owner);}
    }
    public LbcVoidArcanum(GameController game){
        this.game=game;
    }
    private int config(String name){return game.items().mana().score(ScoreHolder.forNameOnly(name),"lbc.math").get();}
    private ServerPlayer owner(Contribution member){return game.server().getPlayerList().getPlayer(member.owner);}
    private boolean valid(ServerPlayer owner){
        return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&owner.level()==game.maps().level()&&game.canUseGameplayItems(owner)&&!game.isObserver(owner)
                &&game.items().mana().visible(owner);
    }
    private boolean held(ServerPlayer owner){return isArcanum(owner.getMainHandItem())||isArcanum(owner.getOffhandItem());}
    private boolean paused(ServerPlayer owner){return game.items().wands().timeStopped()||owner.entityTags().contains("time_stopped")||game.items().frost().isFrozen(owner);}
    private boolean suppressed(ServerPlayer owner){return game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.position());}
    private static boolean resident(ServerLevel world,Vec3 point){var p=BlockPos.containing(point);return world.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    Hole spawn(ServerPlayer owner,Vec3 point){
        if(rewinding)return null;
        callbackDepth++;try{
        if(!valid(owner)||!resident(owner.level(),point)||game.items().magic().suppressedAt(owner.level(),point))return null;
        var body=new Body(owner.level());body.snapTo(point.x,point.y,point.z,0,0);
        body.entityTags().addAll(List.of(BODY_TAG,"void_curse","magic","player"));
        game.items().markCurrentItemEpoch(body);game.items().timeStop().prepareFrozen(body);
        var hole=new Hole(owner.level(),body,point,game.items().epoch());
        hole.members.add(new Contribution(owner.getUUID(),game.items().indirectKills().coldCause(owner,false)));
        holes.add(hole); // Publication invokes load callbacks synchronously.
        if(!hole.world.addFreshEntity(body)||body.isRemoved()||!holes.contains(hole)){remove(hole);return null;}
        return hole;

        }finally{callbackDepth--;}
    }
    private boolean activate(ServerPlayer owner){
        Vec3 at=owner.position().add(owner.getLookAngle().normalize().scale(4));
        if(!resident(owner.level(),at)||game.items().magic().suppressedAt(owner.level(),at)
                ||!game.items().mana().trySpendWand(owner,config("void_arcanum_mana_activate")))return false;
        if(spawn(owner,at)==null)return false;
        game.items().armorSets().phantom().activity(owner);
        game.items().mana().award(owner,"lbc:true_advancements/arcanums/black_hole");
        owner.level().sendParticles(ParticleTypes.END_ROD,owner.getX(),owner.getY()+1,owner.getZ(),20,1,1,1,0);
        LbcOverload.sound(owner,"minecraft:entity.evoker.cast_spell",1,1,0);
        return true;
    }
    public void handSwapped(ServerPlayer owner){
        if(rewinding)return;
        callbackDepth++;try{var state=hands.get(owner.getUUID());if(state!=null){state.main=false;state.off=false;}
        }finally{callbackDepth--;}
    }
    public void tick(){
        if(rewinding)return;
        callbackDepth++;try{
        if(!game.server().tickRateManager().runsNormally())return;
        Set<UUID> present=new HashSet<>();
        for(var owner:game.alivePlayers()){
            if(!valid(owner))continue;
            present.add(owner.getUUID());var state=hands.computeIfAbsent(owner.getUUID(),ignored->new Hands());
            boolean main=isArcanum(owner.getMainHandItem()),off=isArcanum(owner.getOffhandItem());
            if(!main)state.main=false;if(!off)state.off=false;
            if(!main&&!off||suppressed(owner)){state.main=false;state.off=false;removeOwner(owner.getUUID());continue;}
            if(paused(owner))continue;
            if(main&&!state.main)state.main=activate(owner);
            if(off&&!state.off)state.off=activate(owner);
        }
        hands.keySet().removeIf(id->!present.contains(id));
        for(var hole:List.copyOf(holes)){
            hole.members.removeIf(member->{var owner=owner(member);return !valid(owner)||!held(owner)||suppressed(owner);});
            if(hole.members.isEmpty()||!game.items().currentEpoch(hole.epoch)||hole.marker.isRemoved()
                    ||hole.world!=game.maps().level()||!resident(hole.world,hole.point)
                    ||game.items().magic().suppressedAt(hole.world,hole.point)){remove(hole);continue;}
            updateLevel(hole);
        }
        if(game.items().wands().timeStopped())return;
        merge();
        // Snapshot the merged body's whole price before any failed payer changes its membership.
        var costs=new LinkedHashMap<UUID,Integer>();
        for(var hole:holes)costShares(hole).forEach((id,amount)->costs.merge(id,amount,Integer::sum));
        costClock++;
        for(var entry:costs.entrySet()){
            var player=game.server().getPlayerList().getPlayer(entry.getKey());
            if(player==null||paused(player))continue;
            if(!game.items().mana().trySpendWand(player,entry.getValue())){
                var state=hands.get(entry.getKey());if(state!=null){state.main=false;state.off=false;}
                removeOwner(entry.getKey());
            }else game.items().armorSets().phantom().activity(player);
        }
        // Rotate the first body each frame so a large hole cannot starve later bodies.
        var current=List.copyOf(holes);int size=current.size();var active=new ArrayList<Hole>();
        for(int index=0;index<size;index++){
            var hole=current.get((terrainTurn+index)%size);
            var member=source(hole);var author=member==null?null:owner(member);
            if(author==null||paused(author)||hole.marker.entityTags().contains("time_stopped"))continue;
            var cause=cause(hole,member);hole.terrainCause=cause;
            physics(hole,author,cause);
            if(!holes.contains(hole))continue;
            updateVisual(hole,author);active.add(hole);

        }
        // Entity search/physics cannot consume the terrain allowance before it starts.
        long deadline=System.nanoTime()+6_000_000;int budget=12288;
        for(var hole:active){
            if(budget<=0||System.nanoTime()>=deadline)break;
            var author=hole.terrainCause==null?null:game.server().getPlayerList().getPlayer(hole.terrainCause.player());
            if(author==null||paused(author)||!holes.contains(hole))continue;
            budget-=terrain(hole,author,budget,deadline);
        }
        terrainTurn=size==0?0:(terrainTurn+1)%size;

        }finally{callbackDepth--;}
    }
    int upkeep(UUID owner){
        int amount=0;for(var hole:holes)amount+=costShares(hole).getOrDefault(owner,0);return amount;
    }
    private Map<UUID,Integer> costShares(Hole hole){
        var counts=new LinkedHashMap<UUID,Integer>();
        for(var member:hole.members)counts.merge(member.owner,1,Integer::sum);
        var shares=new LinkedHashMap<UUID,Integer>();if(counts.isEmpty())return shares;
        int total=config("void_arcanum_mana_tick")<<(Math.min(5,hole.members.size())-1),allocated=0;
        for(var entry:counts.entrySet()){
            int share=(int)((long)total*entry.getValue()/hole.members.size());shares.put(entry.getKey(),share);allocated+=share;
        }
        // Floor each proportional share, then rotate indivisible mana points across the owners.
        var owners=new ArrayList<>(shares.keySet());
        for(int remainder=0;remainder<total-allocated;remainder++){
            var id=owners.get(Math.floorMod(costClock+remainder,owners.size()));shares.put(id,shares.get(id)+1);
        }
        return shares;
    }
    Contribution source(Hole hole){
        callbackDepth++;try{
        if(hole.members.isEmpty())return null;
        var counts=new LinkedHashMap<UUID,Integer>();
        for(var member:hole.members)counts.merge(member.owner,1,Integer::sum);
        int most=counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        var tied=counts.entrySet().stream().filter(entry->entry.getValue()==most).map(Map.Entry::getKey).toList();
        UUID selected=tied.get(hole.world.getRandom().nextInt(tied.size()));
        // Retain the selected owner's actual contribution order; no newly minted influence.
        return hole.members.stream().filter(member->member.owner.equals(selected)).findFirst().orElseThrow();

        }finally{callbackDepth--;}
    }
    private LbcIndirectKills.Owner cause(Hole hole,Contribution member){
        var cause=member.cause;
        return hole.wild?new LbcIndirectKills.Owner(cause.player(),cause.order(),true,false):cause;
    }
    private void updateLevel(Hole hole){
        hole.level=Math.min(5,hole.members.size());
        // WILD is sticky even if the hostile contribution later expires.
        for(int i=0;i<hole.members.size()&&!hole.wild;i++)for(int j=i+1;j<hole.members.size();j++){
            var first=owner(hole.members.get(i));var second=owner(hole.members.get(j));
            if(first!=null&&second!=null&&first!=second&&!first.isAlliedTo(second)){hole.wild=true;break;}
        }
        hole.frontier=Math.min(hole.frontier,(int)RADII[hole.level]);
    }
    private void merge(){
        // A real merge consumes the second body and all of its original contributions.
        for(int i=0;i<holes.size();i++){
            Hole into=holes.get(i);
            for(int j=i+1;j<holes.size();){
                Hole other=holes.get(j);
                if(other.world!=into.world||into.point.distanceToSqr(other.point)>100
                        ||into.marker.entityTags().contains("time_stopped")||other.marker.entityTags().contains("time_stopped")){j++;continue;}
                into.members.addAll(other.members);into.wild|=other.wild;into.cloneMask|=other.cloneMask;
                remove(other);updateLevel(into);
                for(var member:into.members){var player=owner(member);if(player!=null){
                    if(into.level>=2)game.items().mana().award(player,"lbc:true_advancements/arcanums/black_hole_2");
                    if(into.level==5)game.items().mana().award(player,"lbc:true_advancements/arcanums/black_hole_max");
                }}
            }
        }
    }
    private boolean physicalTarget(Hole hole,ServerPlayer author,Entity target){
        return target!=hole.marker&&!(target instanceof Marker)&&!(target instanceof Display)&&!(target instanceof Interaction)
                &&game.items().wands().validTarget(target)&&!target.entityTags().contains("time_stopped")
                &&!game.items().magic().suppressedAt(hole.world,target.position())
                &&(hole.wild||!(target instanceof ServerPlayer)||game.items().wands().projectileTarget(author,target));
    }
    private static Vec3 chasePoint(Hole hole,LivingEntity target){
        // Arcanum alone hovers above larger victims instead of ploughing along their feet.
        // Keep the tier's legacy speed cap; this changes its destination, not its speed.
        return target.getEyePosition().add(0,.5*(hole.level-1),0);
    }
    void physics(Hole hole,ServerPlayer author,LbcIndirectKills.Owner cause){
        if(rewinding)return;
        callbackDepth++;try{
        double influence=PULL[hole.level],hurt=HURT[hole.level];
        var source=hole.wild?new WildDamage(author):new DamageSource(author.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC),author);
        for(var target:hole.world.getEntities((Entity)null,new AABB(hole.point,hole.point).inflate(influence),entity->physicalTarget(hole,author,entity))){
            Vec3 offset=hole.point.subtract(target.position());double distance=offset.length();
            if(target instanceof LivingEntity living&&BlackHoleDamage.distanceSquared(target,hole.point)<=hurt*hurt)
                BlackHoleDamage.apply(game,living,source,DAMAGE[hole.level]);
            if(distance>influence)continue;
            // The source never pulls players. Ordinary allied mobs, dropped items and projectiles remain physical.
            if(!(target instanceof ServerPlayer)&&distance>1.0e-6){
                double strength=distance<=hurt?.18*hole.level:.012*hole.level*Math.max(0,(influence-distance)/(influence-hurt));
                if(strength>0){target.setDeltaMovement(target.getDeltaMovement().add(offset.scale(strength/distance)));target.needsSync=true;
                    if(target instanceof LivingEntity living)game.items().indirectKills().displaced(living,cause);
                }
            }
        }
        // Match the source's unlimited loaded-dimension nearest selector without loading chunks.
        LivingEntity nearest=null;double best=Double.POSITIVE_INFINITY;
        for(var entity:hole.world.getAllEntities()){
            if(!(entity instanceof LivingEntity target)||!physicalTarget(hole,author,target)
                    ||!resident(hole.world,target.position())||!hole.wild&&!game.items().wands().target(author,target))continue;
            double distance=chasePoint(hole,target).distanceToSqr(hole.point);
            if(distance<best){best=distance;nearest=target;}
        }
        var desiredTilt=new Quaternionf();
        if(nearest!=null){
            Vec3 offset=chasePoint(hole,nearest).subtract(hole.point);
            // Bounded steering retains momentum while accelerating toward the current nearest target.
            // Arrive at the chase point itself: a one-block exclusion shell made contact
            // switch to farther targets, resembling physical repulsion by the victim.
            double distance=offset.length(),acceleration=ACCELERATION[hole.level];
            // Discrete braking distance: remove the old stand-off without orbiting the eyes.
            double arrivalSpeed=Math.sqrt(acceleration*acceleration+2*acceleration*distance)-acceleration;
            double speed=Math.min(MAX_SPEED[hole.level],Math.min(distance,arrivalSpeed));
            Vec3 desired=offset.normalize().scale(speed),change=desired.subtract(hole.velocity);
            double amount=change.length();
            if(amount>ACCELERATION[hole.level])change=change.scale(ACCELERATION[hole.level]/amount);
            hole.velocity=hole.velocity.add(change);
            // Membership loss may lower the tier while the old body still has
            // higher momentum. Enforce the current tier's limit on total 3D speed.
            double cap=MAX_SPEED[hole.level];
            if(hole.velocity.lengthSqr()>cap*cap)hole.velocity=hole.velocity.normalize().scale(cap);
            double horizontal=Math.hypot(offset.x,offset.z);
            if(horizontal>1.0e-6){
                // The disc's normal leans at most 18 degrees toward the chase direction.
                float lean=(float)Math.sin(Math.toRadians(18)),up=(float)Math.cos(Math.toRadians(18));
                desiredTilt.rotationTo(new Vector3f(0,1,0),new Vector3f((float)(offset.x/horizontal)*lean,up,(float)(offset.z/horizontal)*lean));
            }
            Vec3 next=hole.point.add(hole.velocity);
            if(resident(hole.world,next)&&!game.items().magic().suppressedAt(hole.world,next)){
                hole.point=next;hole.marker.snapTo(next.x,next.y,next.z,0,0);
            }else hole.velocity=Vec3.ZERO;
        }else hole.velocity=Vec3.ZERO;
        hole.tilt.slerp(hole.level==5?desiredTilt:new Quaternionf(),.12f).normalize();

        }finally{callbackDepth--;}
    }
    void updateVisual(Hole hole,ServerPlayer owner){
        callbackDepth++;try{
        float radius=(float)(RADII[hole.level]/14);
        if(hole.visual==null)hole.visual=game.items().blackHoleVisuals().create(owner,hole.point,radius,hole.level==5);
        else{game.items().blackHoleVisuals().rebind(hole.visual,owner);game.items().blackHoleVisuals().update(hole.visual,hole.point,radius,hole.level==5);}
        game.items().blackHoleVisuals().tilt(hole.visual,hole.tilt);

        }finally{callbackDepth--;}
    }
    int terrain(Hole hole,ServerPlayer owner,int allowance,long deadline){
        if(rewinding)return 0;
        callbackDepth++;try{
        int radius=(int)RADII[hole.level];
        if(hole.terrainLevel!=hole.level){
            if(hole.terrain!=null)hole.terrain.clear();hole.terrain=null;hole.terrainLevel=hole.level;
        }
        hole.frontier=radius;
        if(hole.terrain==null)hole.terrain=new BlackHoleErosion(hole.world,hole.marker.getUUID().getLeastSignificantBits(),hole.level>=3);
        hole.terrain.move(hole.point,radius,game.server().getTickCount());
        int[] used={0};
        var kills=game.items().indirectKills();
        var terrainCause=hole.terrainCause==null?kills.coldCause(owner,hole.wild):hole.terrainCause;
        kills.withSupportCandidates(hole.world,new AABB(hole.point,hole.point).inflate(radius+1),()->
            kills.trapChanges(hole.world,terrainCause,()->
                used[0]=hole.terrain.step(allowance,deadline,(pos,state)->{
                    if(game.items().magic().suppressedAt(hole.world,Vec3.atCenterOf(pos)))return;
                    kills.supportRemoved(hole.world,terrainCause,pos);
                    if(hole.level<=2){
                        // Native destruction supplies the small tiers' original drops, but
                        // preserves fluid (also from waterlogged blocks). Absorption removes
                        // that residue as well; higher tiers already replace directly with air.
                        hole.world.destroyBlock(pos,true,owner);
                        if(!state.getFluidState().isEmpty())hole.world.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
                        return;
                    }
                    if(hole.world.getBlockEntity(pos) instanceof net.minecraft.world.Container container)container.clearContent();
                    hole.world.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
                })));
        return used[0];

        }finally{callbackDepth--;}
    }
    public void loaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;if(entity.entityTags().contains(BODY_TAG)&&holes.stream().noneMatch(hole->hole.marker==entity))entity.discard();
        }finally{callbackDepth--;}
    }
    public void unloaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;for(var hole:List.copyOf(holes))if(hole.marker==entity)remove(hole);
        }finally{callbackDepth--;}
    }
    private void remove(Hole hole){
        holes.remove(hole);if(hole.terrain!=null)hole.terrain.clear();
        hole.velocity=Vec3.ZERO;hole.tilt.identity();
        if(hole.visual!=null){game.items().blackHoleVisuals().remove(hole.visual);hole.visual=null;}
        if(!hole.marker.isRemoved())hole.marker.discard();
    }
    private void removeOwner(UUID owner){
        for(var hole:List.copyOf(holes)){
            hole.members.removeIf(member->member.owner.equals(owner));
            if(hole.members.isEmpty())remove(hole);else{
                updateLevel(hole);var next=owner(hole.members.getFirst());
                if(hole.visual!=null&&next!=null)game.items().blackHoleVisuals().rebind(hole.visual,next);
            }
        }
    }
    public void forget(ServerPlayer owner){
        if(rewinding)return;
        callbackDepth++;try{hands.remove(owner.getUUID());removeOwner(owner.getUUID());
        }finally{callbackDepth--;}
    }
    public void stop(){
        callbackDepth++;try{for(var hole:List.copyOf(holes))remove(hole);hands.clear();
        }finally{callbackDepth--;}
    }
    public int count(){return holes.size();}
    List<Hole> snapshot(){return List.copyOf(holes);}

    /** Rollback preserves gameplay centers; destructive erosion may restart around those centers. */
    void checkpointBoundary(){if(callbackDepth!=0)throw new IllegalStateException("Finish void-hole native callbacks before checkpointing");}
    private void detachCheckpointVisual(Hole hole){
        if(hole.visual!=null){game.items().blackHoleVisuals().detachForCheckpoint(hole.visual);hole.visual=null;}
        if(hole.terrain!=null){hole.terrain.clear();hole.terrain=null;}
    }
    void prepareFrozen(){checkpointBoundary();rewinding=true;restoreVisuals.clear();restoreVisualOwners.clear();for(var hole:holes)detachCheckpointVisual(hole);}
    void publishFrozen(){
        checkpointBoundary();for(var hole:restoreVisuals){
            ServerPlayer author=game.server().getPlayerList().getPlayer(restoreVisualOwners.get(hole));if(author!=null&&author.level()!=hole.world)author=null;for(var member:hole.members){var current=owner(member);if(author==null&&current!=null&&current.level()==hole.world){author=current;break;}}
            if(author!=null)updateVisual(hole,author);
        }
        restoreVisuals.clear();restoreVisualOwners.clear();rewinding=false;
    }
    void cancelFrozen(){checkpointBoundary();restoreVisuals.clear();restoreVisualOwners.clear();rewinding=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record Image(Hole original,MatchDamageSources.Ref marker,List<Contribution> members,Vec3 point,Vec3 velocity,int level,boolean wild,Quaternionf tilt,LbcIndirectKills.Owner terrainCause,UUID visualOwner,boolean visible,int cloneMask){}
        private record Hand(boolean main,boolean off){}
        private final List<Image> values=new ArrayList<>();private final Map<UUID,Hand> held=new HashMap<>();
        private final int turn=terrainTurn,cost=costClock;
        Checkpoint(MatchSpellCheckpoint.Links links){
            if(rewinding)throw new IllegalStateException("Publish void holes before checkpointing");
            for(var hole:holes){var marker=links.key(hole.marker);if(marker.historical()!=null)throw new IllegalStateException("Void hole requires its canonical native marker");
                values.add(new Image(hole,marker,List.copyOf(hole.members),hole.point,hole.velocity,hole.level,hole.wild,new Quaternionf(hole.tilt),hole.terrainCause,hole.visual==null?null:hole.visual.owner.getUUID(),hole.visual!=null,hole.cloneMask));
                for(var member:hole.members){var player=owner(member);if(player!=null)links.key(player);}
            }
            hands.forEach((id,value)->held.put(id,new Hand(value.main,value.off)));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var bodies=values.stream().map(image->(Marker)links.get(image.marker)).toList();
            for(var image:values)MatchAuthorityCheckpoint.world(game,image.original.world);
            var nextHands=new HashMap<UUID,Hands>();held.forEach((id,value)->{var hand=new Hands();hand.main=value.main;hand.off=value.off;nextHands.put(id,hand);});
            return ()->{for(var future:List.copyOf(holes))detachCheckpointVisual(future);holes.clear();restoreVisuals.clear();restoreVisualOwners.clear();
                for(int i=0;i<values.size();i++){var image=values.get(i);var hole=image.original;hole.marker=bodies.get(i);hole.members.clear();hole.members.addAll(image.members);
                    hole.cloneMask=image.cloneMask;hole.point=image.point;hole.velocity=image.velocity;hole.level=image.level;hole.wild=image.wild;hole.tilt.set(image.tilt);hole.terrainCause=image.terrainCause;
                    hole.terrain=null;hole.terrainLevel=0;hole.frontier=1;hole.visual=null;holes.add(hole);if(image.visible){restoreVisuals.add(hole);restoreVisualOwners.put(hole,image.visualOwner);}}
                hands.clear();hands.putAll(nextHands);terrainTurn=turn;costClock=cost;};
        }
    }

    static boolean cloneProtected(Entity e){return e instanceof Body||e.entityTags().contains(BODY_TAG);}
    int cloneNearby(ServerPlayer holder,int skill,int budget,long deadline) {
        int made=0,count=CloneVirtualAbilityStates.count(skill),bit=CloneVirtualAbilityStates.bit(skill);
        for(var original:List.copyOf(holes)){
            if(budget-made<count||System.nanoTime()>=deadline)break;
            if(rewinding||original.marker.isRemoved()||!game.items().currentEpoch(original.epoch)
                    ||game.items().timeStop().frozen(original.marker)||original.marker.entityTags().contains("time_stopped")
                    ||!CloneVirtualAbilityStates.near(game,holder,original.world,original.point,original.cloneMask,skill))continue;
            var points=CloneVirtualAbilityStates.positions(holder,original.world,original.point,skill);if(points==null)continue;
            var created=new ArrayList<Hole>();
            try{
                for(var point:points){var marker=new Body(original.world);marker.setPos(point);
                    marker.entityTags().addAll(original.marker.entityTags());game.items().markCurrentItemEpoch(marker);
                    var copy=new Hole(original.world,marker,point,original.epoch);created.add(copy);
                    copy.members.addAll(original.members);copy.level=original.level;copy.wild=original.wild;
                    copy.velocity=skill==15?original.velocity:LbcCloneSkills.spreadMotion(original.velocity,holder,1000,1500);
                    copy.cloneMask=original.cloneMask|bit;copy.tilt.set(original.tilt);copy.terrainCause=original.terrainCause;
                    copy.frontier=original.frontier;copy.terrainLevel=original.terrainLevel;
                    if(original.terrain!=null)copy.terrain=original.terrain.copyTranslated(point.subtract(original.point));
                    holes.add(copy);game.items().spellSources().inherit(original.marker,marker);game.items().timeStop().prepareFrozen(marker);
                    if(!copy.world.addFreshEntity(marker)||marker.isRemoved()||!holes.contains(copy))throw new IllegalStateException("Void clone body rejected");
                    if(original.visual!=null)updateVisual(copy,original.visual.owner);
                }
                original.cloneMask|=bit;made+=count;
                if(skill==17)original.velocity=LbcCloneSkills.spreadMotion(original.velocity,holder,10,10);
            }catch(RuntimeException failure){for(var copy:created){game.items().spellSources().reflected(copy.marker);remove(copy);}throw failure;}
        }
        return made;
    }


    void planetoid(ServerPlayer holder) {
        for(var hole:holes)if(!hole.marker.isRemoved()&&game.items().currentEpoch(hole.epoch)
                &&!game.items().timeStop().frozen(hole.marker)&&!hole.marker.entityTags().contains("time_stopped")
                &&LbcPlanetoidSkill.near(game,holder,hole.world,hole.point))
            LbcPlanetoidSkill.orient(hole.marker,LbcPlanetoidSkill.direction(holder,hole.point,82));
        // Its source chase uses absolute Motion, not local forward movement: do not redirect velocity.
    }

}
