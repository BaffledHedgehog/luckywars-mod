package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Temporary ordinary polymorph, permanent academy polymorph, and the authored WILD sheep-hole consequence. */
public final class LbcSheep {
    public static final Item ITEM=item(false), ACADEMY_ITEM=item(true);
    private static final String TAG="skywars2.sheep", BODY="skywars2.sheep_hole";
    // Distance is a sample counter: samples 0..150 inclusive, separated by .75 blocks.
    static final double RANGE=112.5, RADIUS=Math.sqrt(552), INFLUENCE=48;
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    private final GameController game;
    private final List<Converted> sheep=new ArrayList<>();
    private final List<Hole> holes=new ArrayList<>();
    private final Set<Entity> spawning=Collections.newSetFromMap(new IdentityHashMap<>());
    private int terrainTurn;
    private int callbackDepth;private boolean rewinding;
    private final List<Hole> restoreVisuals=new ArrayList<>();
    private record Converted(Sheep entity,UUID owner,LbcIndirectKills.Owner cause,String epoch) {}
    static final class Hole {
        Marker entity;final UUID owner;final LbcIndirectKills.Owner cause;final String epoch;
        final ServerLevel level;Vec3 point,velocity=Vec3.ZERO;final Quaternionf tilt=new Quaternionf();
        BlackHoleErosion terrain;BlackHoleVisuals.Handle visual;
        Hole(Marker entity,ServerPlayer owner,Vec3 point,LbcIndirectKills.Owner cause,String epoch){
            this.entity=entity;this.owner=owner.getUUID();this.level=owner.level();this.point=point;this.cause=cause;this.epoch=epoch;
        }
    }
    private static final class Body extends Marker {
        Body(ServerLevel level){super(EntityTypes.MARKER,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    private static final class MagicSheep extends Sheep {
        MagicSheep(ServerLevel level){super(EntityTypes.SHEEP,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    public LbcSheep(GameController game,LbcMana mana){
        this.game=game;
        var holder=ScoreHolder.forNameOnly("sheep_wand_mana_use");var board=game.server().getScoreboard();
        var objective=board.getObjective("lbc.math");
        if(objective==null||board.getPlayerScoreInfo(holder,objective)==null)mana.score(holder,"lbc.math").set(500);
    }
    public static void register(){}
    private static Item item(boolean academy){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(academy?"skywars2:academy_sheep_wand":"skywars2:sheep_wand"));
        var data=new CompoundTag();for(var flag:List.of("transformable","staff","raycastspell","sheepwand"))data.putInt(flag,1);
        if(academy){data.putInt("upgraded",1);data.putInt("mode",1);data.putInt("upgradable",0);data.putInt("upgrade_available",1);}
        var model=Identifier.parse("lbc:sheepwand");
        var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(1)
                .overrideDescription("sheep_wand").component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(academy?"lbc:tier_x":"lbc:tier_a"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isWand(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||stack.is(ACADEMY_ITEM)||LbcFartus.data(stack).getIntOr("sheepwand",0)==1);}
    public static boolean isAcademy(ItemStack stack){return isWand(stack)&&(stack.is(ACADEMY_ITEM)||LbcFartus.data(stack).getIntOr("upgraded",0)==1);}
    public static boolean managed(Entity entity){return entity!=null&&(entity.entityTags().contains(TAG)||entity.entityTags().contains(BODY));}
    public ServerPlayer owner(Entity entity){
        for(var c:sheep)if(c.entity==entity)return player(c.owner);
        for(var h:holes)if(h.entity==entity)return player(h.owner);
        return null;
    }
    private ServerPlayer player(UUID id){return game.server().getPlayerList().getPlayer(id);}
    private boolean validOwner(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&game.canUseGameplayItems(p)&&!game.isObserver(p);}
    private static boolean resident(ServerLevel level,Vec3 at){var pos=BlockPos.containing(at);return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private boolean suppressed(ServerLevel level,Vec3 at){return game.items().magic().suppressedAt(level,at);}
    private boolean frozen(Entity e){return game.items().wands().timeStopped()||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped");}
    public boolean use(ServerPlayer p,InteractionHand hand){
        if(rewinding)return false;
        callbackDepth++;try{
        var stack=p.getItemInHand(hand);
        if(!isWand(stack)||!validOwner(p)||game.items().actionsBlocked(p)||game.items().wands().inputTimeStopped(p)
                ||game.items().magic().suppressed(p)||suppressed(p.level(),p.getEyePosition()))return false;
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("sheep_wand_mana_use"),"lbc.math").get();
        if(cost<0||!game.items().mana().trySpendWand(p,cost))return false;
        cast(p,hand,stack);game.items().wands().wandCasted(p);return true;

        }finally{callbackDepth--;}
    }
    private boolean rayTarget(ServerPlayer p,Entity e,boolean academy){
        if(e==p||e instanceof Marker||e instanceof Display||e instanceof Interaction||managed(e)
                ||e.entityTags().contains("magicalsheep")||!game.items().wands().validTarget(e)||frozen(e))return false;
        if(e instanceof ItemEntity item)return potion(item.getItem());
        if(e instanceof ServerPlayer other)return (academy||hasPotion(other))&&game.items().wands().projectileTarget(p,other);
        return game.items().wands().projectileTarget(p,e);
    }
    private void cast(ServerPlayer p,InteractionHand hand,ItemStack stack){
        cast(p,hand,stack,isAcademy(stack));
    }
    void cast(ServerPlayer p,InteractionHand hand,ItemStack stack,boolean academy){
        Vec3 from=p.getEyePosition(),direction=p.getLookAngle().normalize();
        // The generic LivingEntity overload accepts explicit collision eligibility, including dropped potions.
        Vec3 to=from.add(direction.scale(RANGE));
        var hit=game.items().wands().firstHit((LivingEntity)p,from,to,e->rayTarget(p,e,academy));
        Vec3 end=hit==null?to:hit.getLocation();
        var greed=LbcGreed.firstHit(p.level(),from,end,0);
        if(greed!=null&&greed.getEntity()!=p){end=greed.getLocation();hit=greed;}
        boolean blocked=false;double length=from.distanceTo(end);
        var dust=new DustParticleOptions(academy?0x813681:0xff00ff,2);
        for(double d=0;d<=length;d+=.75){
            Vec3 at=from.add(direction.scale(d));
            if(!resident(p.level(),at)||suppressed(p.level(),at)){blocked=true;break;}
            p.level().sendParticles(dust,at.x,at.y,at.z,academy?1:2,0,0,0,.1);
            // Original inventory/dropped-potion proximity checks run at each source ray sample.
            if(consumePotion(p,hand,stack,at))return;
        }
        if(blocked||hit==null||!resident(p.level(),end)||suppressed(p.level(),end))return;
        if(hit instanceof EntityHitResult entityHit){
            var target=entityHit.getEntity();if(LbcGreed.active(target))return;
            transform(p,target,academy);
        }
        consumePotion(p,hand,stack,end);
    }
    static boolean potion(ItemStack stack){var d=LbcFartus.data(stack);return !stack.isEmpty()&&d.getIntOr("transformable",0)==1&&d.getIntOr("potion",0)==1&&d.getIntOr("succubus",0)==1;}
    private static boolean hasPotion(ServerPlayer p){for(int i=0;i<p.getInventory().getContainerSize();i++)if(potion(p.getInventory().getItem(i)))return true;return false;}
    private boolean potionHolder(ServerPlayer player){return LbcDimensionItems.ordinary(player)&&game.canUseGameplayItems(player)&&game.items().wands().validTarget(player)&&!frozen(player)&&!suppressed(player.level(),player.position());}
    /** Two independently authored resources: nearby dropped potion, or nearby player's matching inventory. */
    boolean consumePotion(ServerPlayer p,InteractionHand hand,ItemStack used,Vec3 at){
        boolean dropped=!p.level().getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(2),e->game.items().wands().validTarget(e)&&e.position().distanceToSqr(at)<=4&&potion(e.getItem())).isEmpty();
        boolean inventory=false;
        for(var other:p.level().getEntitiesOfClass(ServerPlayer.class,new AABB(at,at).inflate(2),e->e!=p&&potionHolder(e)&&e.position().distanceToSqr(at)<=4))if(hasPotion(other)){inventory=true;break;}
        if(!dropped&&!inventory)return false;
        boolean created=false;
        if(dropped&&spawnHole(p,at)!=null){clearItems(p.level(),at);created=true;}
        if(inventory&&spawnHole(p,at)!=null){
            for(var other:p.level().getEntitiesOfClass(ServerPlayer.class,new AABB(at,at).inflate(5),e->potionHolder(e)&&e.position().distanceToSqr(at)<=25)){
                for(int i=0;i<other.getInventory().getContainerSize();i++)if(potion(other.getInventory().getItem(i)))other.getInventory().setItem(i,ItemStack.EMPTY);
                other.getInventory().setChanged();other.containerMenu.broadcastChanges();
            }created=true;
        }
        if(created&&!isAcademy(used)&&LbcAcademy.trial(used)){
            game.items().academy().ready(p,hand,used);p.entityTags().add("ohno");
        }
        return created;
    }
    /** Academy's source gender branch includes players, but the final polymorph always excludes them. */
    Sheep transform(ServerPlayer p,Entity original,boolean academy){
        if(rewinding)return null;
        callbackDepth++;try{
        if(original instanceof ServerPlayer player&&game.items().magic().rejectCustomDebuff(player))return null;
        if(!validOwner(p)||!rayTarget(p,original,academy)||original instanceof ItemEntity
                ||!resident(p.level(),original.position())||suppressed(p.level(),original.position()))return null;
        if(academy&&original.getType().builtInRegistryHolder().is(MOBS)){
            if(!original.entityTags().contains("male")&&!original.entityTags().contains("female"))original.entityTags().add(p.getRandom().nextBoolean()?"male":"female");
            if(original.entityTags().contains("male")){if(spawnHole(p,original.position())!=null)clearItems(p.level(),original.position());}
        }
        if(original instanceof LivingEntity living&&living.getHealth()>=1000)game.items().mana().award(p,"lbc:true_advancements/staffs/polymorph");
        if(!academy&&original instanceof LivingEntity living)return game.items().polymorph().transform(living,game.items().indirectKills().coldCause(p,false),true);
        if(original instanceof Player)return null;
        var replacement=new MagicSheep(p.level());
        try{replacement.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),TagParser.parseCompoundFully(
                "{DeathLootTable:'minecraft:empty',CustomName:'jeb_',CustomNameVisible:0b,attributes:[{id:'minecraft:knockback_resistance',base:20}]}")));}
        catch(Exception e){throw new IllegalStateException("Cannot create magical sheep",e);}
        replacement.setCustomName(Component.literal("jeb_"));replacement.setCustomNameVisible(false);
        replacement.setPos(original.position());replacement.entityTags().addAll(List.of(TAG,"magicalsheep","magic"));
        game.items().markCurrentItemEpoch(replacement);game.items().timeStop().prepareFrozen(replacement);
        var c=new Converted(replacement,p.getUUID(),game.items().indirectKills().coldCause(p,false),game.items().epoch());sheep.add(c);
        spawning.add(replacement);
        try{if(!p.level().addFreshEntity(replacement)||replacement.isRemoved()){sheep.remove(c);return null;}}
        finally{spawning.remove(replacement);}
        if(original instanceof LivingEntity living)game.items().indirectKills().clear(living);
        // Discard has no loot, death side effects, remote teleport, or invented restoration timer.
        if(!game.items().wtf().erase(original))original.discard();
        p.level().sendParticles(ParticleTypes.POOF,replacement.getX(),replacement.getY()+.5,replacement.getZ(),20,.1,.1,.1,.5);
        LbcOverload.sound(p,replacement.position(),"minecraft:block.fire.extinguish",1,2,0);
        return replacement;

        }finally{callbackDepth--;}
    }
    Hole spawnHole(ServerPlayer p,Vec3 at){
        if(rewinding)return null;
        callbackDepth++;try{
        if(!validOwner(p)||!resident(p.level(),at)||suppressed(p.level(),at))return null;
        var marker=new Body(p.level());marker.setPos(at);marker.entityTags().addAll(List.of(BODY,"sheep_black_hole","slowraycast","magic","rare"));
        game.items().markCurrentItemEpoch(marker);game.items().timeStop().prepareFrozen(marker);
        var h=new Hole(marker,p,at,game.items().indirectKills().wildCause(p),game.items().epoch());holes.add(h);
        if(!p.level().addFreshEntity(marker)||marker.isRemoved()||!holes.contains(h)){remove(h);return null;}
        updateVisual(h,p);return h;

        }finally{callbackDepth--;}
    }
    private void clearItems(ServerLevel level,Vec3 at){for(var e:level.getEntities((Entity)null,new AABB(at,at).inflate(5),e->e instanceof ItemEntity&&game.items().wands().validTarget(e)&&e.position().distanceToSqr(at)<=25))e.discard();}
    private boolean physical(Hole h,Entity e){return e!=h.entity&&!(e instanceof Marker)&&!(e instanceof Display)&&!(e instanceof Interaction)
            &&game.items().wands().validTarget(e)&&!frozen(e)&&!suppressed(h.level,e.position());}
    void physics(Hole h,ServerPlayer author){
        if(rewinding)return;
        callbackDepth++;try{
        // Player friendship admission runs before LivingEntity's inherited-cause
        // hook. Carry WILD ownership on the source before applying direct HP damage.
        var source=game.items().indirectKills().captureDamage(new DamageSource(author.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.OUTSIDE_BORDER),h.entity,author),h.cause);
        for(var target:h.level.getEntities((Entity)null,new AABB(h.point,h.point).inflate(INFLUENCE),e->physical(h,e))){
            Vec3 offset=h.point.subtract(target.position());double distance=offset.length();
            if(target instanceof LivingEntity living&&BlackHoleDamage.distanceSquared(target,h.point)<=25
                    &&game.items().mana().score(target,"void_resistance").get()<1)
                game.items().indirectKills().withCause(living,h.cause,()->BlackHoleDamage.apply(game,living,source,10));
            if(distance>INFLUENCE)continue;
            if(distance<=5){
                if(target instanceof Projectile||target instanceof ItemEntity){target.discard();continue;}
            }
            if(distance<1.0e-6||distance>RADIUS&&target instanceof ServerPlayer p&&!p.getLastClientInput().equals(Input.EMPTY))continue;
            double force=distance<=RADIUS?.24:.012*Math.max(0,(INFLUENCE-distance)/(INFLUENCE-RADIUS));
            target.setDeltaMovement(target.getDeltaMovement().add(offset.scale(force/distance)));target.needsSync=true;
            if(target instanceof ServerPlayer p)p.connection.send(new ClientboundSetEntityMotionPacket(p));
            if(target instanceof LivingEntity living)game.items().indirectKills().displaced(living,h.cause);
        }
        Vec3 goal=null;
        if(author.entityTags().contains("ohno")&&!frozen(author))goal=author.position();
        if(goal==null){
            double furthest=-1;
            for(var e:h.level.getAllEntities())if(e.getType().builtInRegistryHolder().is(MOBS)&&physical(h,e)&&resident(h.level,e.position())){
                double distance=e.position().distanceToSqr(h.point);if(distance>furthest){furthest=distance;goal=e.position();}
            }
        }
        h.velocity=goal==null?Vec3.ZERO:goal.subtract(h.point).normalize().scale(Math.min(.05,goal.distanceTo(h.point)));
        var next=h.point.add(h.velocity);
        if(resident(h.level,next)&&!suppressed(h.level,next)){h.point=next;h.entity.setPos(next);}else h.velocity=Vec3.ZERO;
        var desired=new Quaternionf();double horizontal=Math.hypot(h.velocity.x,h.velocity.z);
        if(horizontal>1.0e-6){float lean=(float)Math.sin(Math.toRadians(18)),up=(float)Math.cos(Math.toRadians(18));
            desired.rotationTo(new Vector3f(0,1,0),new Vector3f((float)(h.velocity.x/horizontal)*lean,up,(float)(h.velocity.z/horizontal)*lean));}
        h.tilt.slerp(desired,.12f).normalize();
        LbcOverload.sound(author,h.point,"minecraft:entity.sheep.ambient",1,1,80,.5f);

        }finally{callbackDepth--;}
    }
    private void updateVisual(Hole h,ServerPlayer p){
        callbackDepth++;try{
        float radius=(float)(RADIUS/14);
        if(h.visual==null)h.visual=game.items().blackHoleVisuals().create(p,h.point,radius,true);
        else {game.items().blackHoleVisuals().rebind(h.visual,p);game.items().blackHoleVisuals().update(h.visual,h.point,radius,true);}
        game.items().blackHoleVisuals().tilt(h.visual,h.tilt);

        }finally{callbackDepth--;}
    }
    int terrain(Hole h,ServerPlayer author,int allowance,long deadline){
        if(rewinding)return 0;
        callbackDepth++;try{
        if(h.terrain==null)h.terrain=new BlackHoleErosion(h.level,h.entity.getUUID().getLeastSignificantBits(),true);
        h.terrain.move(h.point,RADIUS,game.server().getTickCount());
        int[] used={0};var kills=game.items().indirectKills();
        kills.withSupportCandidates(h.level,new AABB(h.point,h.point).inflate(RADIUS+1),()->
            kills.trapChanges(h.level,h.cause,()->used[0]=h.terrain.step(allowance,deadline,(pos,state)->{
                if(suppressed(h.level,Vec3.atCenterOf(pos)))return;
                kills.supportRemoved(h.level,h.cause,pos);
                if(h.level.getBlockEntity(pos) instanceof Container container)container.clearContent();
                h.level.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
            })));
        return used[0];

        }finally{callbackDepth--;}
    }
    public void tick(){
        if(rewinding)return;
        callbackDepth++;try{
        if(!game.server().tickRateManager().runsNormally())return;
        for(var c:List.copyOf(sheep)){
            var p=player(c.owner);
            if(c.entity.isRemoved()||!validOwner(p)||!game.items().currentEpoch(c.epoch)||c.entity.level()!=p.level()
                    ||suppressed((ServerLevel)c.entity.level(),c.entity.position()))remove(c);
        }
        var current=List.copyOf(holes);int size=current.size();
        var active=new ArrayList<Hole>();
        for(int i=0;i<size;i++){
            var h=current.get((terrainTurn+i)%size);var p=player(h.owner);
            if(h.entity.isRemoved()||!validOwner(p)||!game.items().currentEpoch(h.epoch)||p.level()!=h.level
                    ||!resident(h.level,h.point)||suppressed(h.level,h.point)){remove(h);continue;}
            if(frozen(h.entity))continue;
            physics(h,p);updateVisual(h,p);
            active.add(h);
        }
        // Physics cannot consume the terrain allowance before its first slice starts.
        long deadline=System.nanoTime()+6_000_000;int budget=12288;
        for(var h:active){
            if(budget<=0||System.nanoTime()>=deadline)break;
            var p=player(h.owner);if(validOwner(p)&&holes.contains(h))budget-=terrain(h,p,budget,deadline);
        }
        terrainTurn=size==0?0:(terrainTurn+1)%size;

        }finally{callbackDepth--;}
    }
    private void remove(Converted c){sheep.remove(c);if(!c.entity.isRemoved())c.entity.discard();}
    private void remove(Hole h){holes.remove(h);if(h.terrain!=null)h.terrain.clear();if(h.visual!=null)game.items().blackHoleVisuals().remove(h.visual);if(!h.entity.isRemoved())h.entity.discard();}
    public void loaded(Entity e){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(e))return;if(managed(e)&&!spawning.contains(e)&&sheep.stream().noneMatch(c->c.entity==e)&&holes.stream().noneMatch(h->h.entity==e))e.discard();
        }finally{callbackDepth--;}
    }
    public void unloaded(Entity e){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(e))return;for(var c:List.copyOf(sheep))if(c.entity==e)remove(c);for(var h:List.copyOf(holes))if(h.entity==e)remove(h);
        }finally{callbackDepth--;}
    }
    public void forget(UUID id){
        if(rewinding)return;
        callbackDepth++;try{game.items().polymorph().forgetAuthor(id);for(var c:List.copyOf(sheep))if(c.owner.equals(id))remove(c);for(var h:List.copyOf(holes))if(h.owner.equals(id))remove(h);
        }finally{callbackDepth--;}
    }
    public void forget(ServerPlayer p){forget(p.getUUID());}
    public void stop(){
        callbackDepth++;try{for(var c:List.copyOf(sheep))remove(c);for(var h:List.copyOf(holes))remove(h);spawning.clear();
        }finally{callbackDepth--;}
    }
    record SheepView(UUID entity,UUID owner,Vec3 point){}
    record HoleView(UUID entity,UUID owner,Vec3 point,Vec3 velocity,double radius,boolean disc,int queuedBlocks){}
    List<SheepView> sheepViews(){var result=new ArrayList<SheepView>(game.items().polymorph().sheepViews());result.addAll(sheep.stream().map(c->new SheepView(c.entity.getUUID(),c.owner,c.entity.position())).toList());return List.copyOf(result);}
    List<HoleView> holeViews(){return holes.stream().map(h->new HoleView(h.entity.getUUID(),h.owner,h.point,h.velocity,RADIUS,true,h.terrain==null?0:h.terrain.queuedBlockCount())).toList();}
    List<Hole> holes(){return List.copyOf(holes);}

    /** Permanent academy sheep and authored holes share native identities with the actor checkpoint. */
    void checkpointBoundary(){if(callbackDepth!=0||!spawning.isEmpty())throw new IllegalStateException("Finish sheep conversion/hole callbacks before checkpointing");}
    private void detachCheckpointVisual(Hole hole){
        if(hole.visual!=null){game.items().blackHoleVisuals().detachForCheckpoint(hole.visual);hole.visual=null;}
        if(hole.terrain!=null){hole.terrain.clear();hole.terrain=null;}
    }
    void prepareFrozen(){checkpointBoundary();rewinding=true;restoreVisuals.clear();for(var hole:holes)detachCheckpointVisual(hole);}
    void publishFrozen(){
        checkpointBoundary();for(var hole:restoreVisuals){var owner=player(hole.owner);if(owner!=null&&owner.level()==hole.level)updateVisual(hole,owner);}
        restoreVisuals.clear();rewinding=false;
    }
    void cancelFrozen(){checkpointBoundary();restoreVisuals.clear();rewinding=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record Image(Hole original,MatchDamageSources.Ref entity,Vec3 point,Vec3 velocity,Quaternionf tilt,boolean visible){}
        private record SheepImage(Converted original,MatchDamageSources.Ref entity){}
        private final List<Image> values=new ArrayList<>();private final List<SheepImage> converted=new ArrayList<>();
        private final int turn=terrainTurn;
        Checkpoint(MatchSpellCheckpoint.Links links){
            if(rewinding)throw new IllegalStateException("Publish sheep holes before checkpointing");
            for(var hole:holes){var entity=links.key(hole.entity);if(entity.historical()!=null)throw new IllegalStateException("Sheep hole requires its canonical native marker");
                values.add(new Image(hole,entity,hole.point,hole.velocity,new Quaternionf(hole.tilt),hole.visual!=null));var owner=player(hole.owner);if(owner!=null)links.key(owner);}
            for(var body:sheep){var entity=links.key(body.entity);if(entity.historical()!=null)throw new IllegalStateException("Academy sheep requires its canonical native body");
                converted.add(new SheepImage(body,entity));var owner=player(body.owner);if(owner!=null)links.key(owner);}
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var bodies=values.stream().map(image->(Marker)links.get(image.entity)).toList();var transformed=new ArrayList<Converted>();
            for(var image:values)MatchAuthorityCheckpoint.world(game,image.original.level);
            for(var image:converted){var entity=(Sheep)links.get(image.entity);var value=image.original;transformed.add(entity==value.entity?value:new Converted(entity,value.owner,value.cause,value.epoch));}
            return ()->{for(var future:List.copyOf(holes))detachCheckpointVisual(future);holes.clear();restoreVisuals.clear();
                for(int i=0;i<values.size();i++){var image=values.get(i);var hole=image.original;hole.entity=bodies.get(i);hole.point=image.point;hole.velocity=image.velocity;hole.tilt.set(image.tilt);
                    hole.terrain=null;hole.visual=null;holes.add(hole);if(image.visible)restoreVisuals.add(hole);}
                sheep.clear();sheep.addAll(transformed);terrainTurn=turn;};
        }
    }
}
