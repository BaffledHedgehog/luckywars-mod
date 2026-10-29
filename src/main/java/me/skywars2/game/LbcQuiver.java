package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Infinite Quiver's one-count/one-roll batch and complete ordinary special-arrow lifecycle. */
public final class LbcQuiver {
    private static final String SHOT="skywars2.quiver.arrow",CHILD="skywars2.quiver.child",TRAIL="skywars2.quiver.trail";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final int[] AMPS={102,103,112,113};
    private static final String[] SCORES={"effect_evoker","effect_shock","effect_lightarrow","effect_evokerarrow"};
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<CompoundTag> DATA=net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(Identifier.parse("skywars2:quiver_effects"),b->b.persistent(CompoundTag.CODEC));
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<CompoundTag> SHOT_DATA=net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(Identifier.parse("skywars2:quiver_shot"),b->b.persistent(CompoundTag.CODEC));
    private static final ThreadLocal<Arrow> EFFECT_ARROW=new ThreadLocal<>();
    private final GameController game;
    private int clock;
    private final Map<Arrow,Shot> shots=new IdentityHashMap<>();
    private final Map<net.minecraft.world.entity.Marker,Trail> trails=new LinkedHashMap<>();
    private final Map<Entity,Authority> children=new WeakHashMap<>();
    private final Map<LivingEntity,List<ArrayDeque<Contribution>>> pending=new LinkedHashMap<>();
    private static final class Shot {int age;boolean ground;final int amp;ServerPlayer owner;LbcIndirectKills.Owner cause;final String epoch;Shot(ServerPlayer owner,int amp,LbcIndirectKills.Owner cause,String epoch){this.owner=owner;this.amp=amp;this.cause=cause;this.epoch=epoch;}}
    private record Authority(LbcIndirectKills.Owner cause,String epoch){}
    private static final class Trail {final int amp;final Authority authority;int age;Trail(int amp,Authority a){this.amp=amp;authority=a;}}
    private static final class Contribution {int remaining;final Authority authority;Contribution(int duration,Authority a){remaining=duration;authority=a;}}
    public LbcQuiver(GameController game){this.game=game;}
    public static Arrow effectArrow(){return EFFECT_ARROW.get();}
    public static Arrow beginEffects(Arrow arrow){var previous=EFFECT_ARROW.get();EFFECT_ARROW.set(arrow);return previous;}
    public static void endEffects(Arrow previous){if(previous==null)EFFECT_ARROW.remove();else EFFECT_ARROW.set(previous);}
    public static boolean isEffect(MobEffectInstance effect){return effect.is(MobEffects.RAID_OMEN)&&Set.of(101,102,103,112,113).contains(effect.getAmplifier());}
    private static int amp(Arrow arrow){for(var effect:arrow.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects())if(effect.is(MobEffects.RAID_OMEN))return effect.getAmplifier();return -1;}
    private static int color(Arrow arrow){return arrow.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).customColor().orElse(-1);}
    public static boolean custom(Arrow arrow){return !LbcBabylon.managed(arrow)&&(Set.of(6710881,6710882,6710883,2883328,65493).contains(color(arrow))||LbcStand.isArrow(arrow.getPickupItemStackOrigin())||Set.of(101,102,103,111,112,113).contains(amp(arrow)));}
    private static boolean disabled(Level level){return !LbcDimensionItems.ordinary(level);}
    private boolean runtime(Entity e){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&e.level()==game.maps().level()&&!disabled(e.level());}
    private boolean frozen(Entity e){return game.items().timeStop().stopped((ServerLevel)e.level())||game.items().timeStop().frozen(e)||e instanceof LivingEntity living&&game.items().frost().isFrozen(living)||e.entityTags().contains("time_stopped");}
    private boolean admitted(ServerPlayer owner){return runtime(owner)&&owner.isAlive()&&!owner.isRemoved()&&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&game.items().wands().validRoleTarget(owner,false)&&!game.items().timeStop().stopped(owner.level())&&!game.items().timeStop().frozen(owner)&&!game.items().wands().inputTimeStopped(owner)&&!owner.level().dimension().identifier().equals(Identifier.parse("minecraft:nexus"));}
    private static boolean quiver(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("arrowgenerator",0)==1;}
    private static boolean trigger(ServerPlayer owner){for(int slot=0;slot<36;slot++)if(quiver(owner.getInventory().getItem(slot)))return true;return quiver(owner.getMainHandItem())||quiver(owner.getOffhandItem());}
    /** One physical count and one original pool invocation per approved300tick batch. */
    public void pulse(ServerPlayer owner){
        if(!admitted(owner)||!trigger(owner)||game.items().magic().beaconSuppressed(owner.position()))return;
        int count=owner.getInventory().clearOrCountMatchingItems(LbcQuiver::quiver,true,0,owner.inventoryMenu.getCraftSlots());if(count<=0)return;
        var holder=ScoreHolder.forNameOnly("arrowgens");var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");var previous=objective==null?null:board.getPlayerScoreInfo(holder,objective);Integer saved=previous==null?null:previous.value();
        try{game.items().mana().score(holder,"lbc.math").set(count);for(var stack:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:arrows"))drop(owner,stack,true);}
        finally{var current=board.getObjective("lbc.math");if(saved!=null)board.getOrCreatePlayerScore(holder,current,true).set(saved);else if(current!=null)board.resetSinglePlayerScore(holder,current);}
        LbcOverload.sound(owner,"minecraft:entity.zombie.infect",1,2,0);
    }
    private void drop(ServerPlayer owner,ItemStack stack,boolean award){var at=owner.position();var entity=new ItemEntity(owner.level(),at.x,at.y,at.z,stack.copy());entity.setDefaultPickUpDelay();if(owner.level().addFreshEntity(entity)&&award&&LbcStand.isArrow(entity.getItem()))game.items().mana().award(owner,"lbc:true_advancements/accesories/infinite_quiver");}
    public boolean validEffectArrow(Arrow arrow,int amplifier){return arrow!=null&&custom(arrow)&&amp(arrow)==amplifier&&runtime(arrow)&&game.items().currentEpoch(arrow)&&arrow.getOwner() instanceof ServerPlayer;}
    private Shot shot(Arrow arrow){
        var current=shots.get(arrow);if(current!=null)return current;
        if(!custom(arrow)||!runtime(arrow)||!(arrow.getOwner() instanceof ServerPlayer owner))return null;
        int color=color(arrow);if(color==6710881)arrow.setBaseDamage(5);else if(color==6710882)arrow.setBaseDamage(1);else if(color==6710883)arrow.setNoGravity(true);
        arrow.entityTags().add(SHOT);game.items().markCurrentItemEpoch(arrow);current=new Shot(owner,amp(arrow),game.items().indirectKills().coldCause(owner,true),game.items().epoch());var saved=arrow.getAttached(SHOT_DATA);if(saved!=null&&game.items().currentEpoch(saved.getStringOr("epoch","")))current.age=saved.getIntOr("age",0);shots.put(arrow,current);return current;
    }
    /** Native Arrow HEAD: configure once before physics, then preserve native swept collisions. */
    public boolean beforeArrow(Arrow arrow){
        var state=shot(arrow);if(state==null)return true;
        if(!runtime(arrow)||!game.items().currentEpoch(state.epoch)){arrow.discard();shots.remove(arrow);return false;}
        if(frozen(arrow))return false;
        if(arrow.getOwner() instanceof ServerPlayer current&&current!=state.owner){state.owner=current;state.cause=game.items().indirectKills().coldCause(current,true);}
        if(state.amp>=101){var saved=new CompoundTag();saved.putString("epoch",state.epoch);saved.putInt("age",++state.age);arrow.setAttached(SHOT_DATA,saved);if(state.age>=300){arrow.discard();shots.remove(arrow);return false;}
            if(!game.items().magic().beaconSuppressed(arrow.position()))switch(state.amp){case 111->game.items().kiuas().arrowTerrain(state.owner,arrow.position(),false);case 112,113->trail(state.owner.level(),arrow.position(),state.amp,new Authority(state.cause,state.epoch));default->{}}
            if(((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$isInGround())ground(arrow);
        }
        return !arrow.isRemoved();
    }
    public void ground(Arrow arrow){var state=shot(arrow);if(state==null||state.ground||state.amp<101||frozen(arrow))return;state.ground=true;
        var at=arrow.position();var authority=new Authority(state.cause,state.epoch);switch(state.amp){
            case 101->{if(!game.items().magic().luckyEffectWarded(state.owner.level(),at)&&!game.items().magic().mechanismSuppressedAt(state.owner.level(),at))explode(state.owner.level(),at,authority);}
            case 102->{if(!game.items().magic().beaconSuppressed(at))evoker(state.owner.level(),at,authority,true);}
            case 103->{if(!game.items().magic().mechanismSuppressedAt(state.owner.level(),at))shock(state.owner.level(),at,authority);}
            case 111->{if(!game.items().magic().beaconSuppressed(at))game.items().kiuas().arrowTerrain(state.owner,at,true);}
            case 119->{for(var stack:game.lbc().rollTable(state.owner.level(),at,state.owner,"lbc:stand_arrow")){var item=new ItemEntity(state.owner.level(),at.x,at.y,at.z,stack);item.setDefaultPickUpDelay();state.owner.level().addFreshEntity(item);}}
            default->{}
        }arrow.discard();shots.remove(arrow);
    }
    private static int index(int amplifier){for(int i=0;i<AMPS.length;i++)if(AMPS[i]==amplifier)return i;return -1;}
    private boolean skillImmune(LivingEntity target){return game.items().mana().hudValue(ScoreHolder.forNameOnly("#lbcskill"),"swrg.math")==1&&game.items().mana().hudValue(target,"lbc.skill")==19;}
    public boolean accept(LivingEntity target,MobEffectInstance effect,Entity source){
        if(!isEffect(effect)||!(source instanceof Arrow arrow)||!validEffectArrow(arrow,effect.getAmplifier()))return false;
        if(effect.getAmplifier()==101)return game.items().babylonEffects().acceptQuiver101(target,effect,arrow);
        if(!runtime(target)||!game.items().wands().validTarget(target)||!target.canBeAffected(effect)||target instanceof ServerPlayer p&&game.items().magic().rejectCustomEffect(p))return false;
        if(skillImmune(target)){clear(target);return true;}int duration=effect.getDuration()==2?1:effect.getDuration();if(duration<=0)return true;
        var owner=(ServerPlayer)arrow.getOwner();var state=shot(arrow);var queues=pending.computeIfAbsent(target,k->queues());queues.get(index(effect.getAmplifier())).addLast(new Contribution(duration,new Authority(state.cause,state.epoch)));sync(target);return true;
    }
    void adoptRawFoodStatuses(ServerPlayer player){
        if(!runtime(player)||skillImmune(player)||game.items().magic().rejectCustomEffect(player))return;
        int[] desired=new int[AMPS.length];for(int i=0;i<AMPS.length;i++)desired[i]=game.items().mana().hudValue(player,SCORES[i]);
        for(int i=0;i<AMPS.length;i++){long delta=(long)desired[i]-queued(player,AMPS[i]);if(delta>0)
            pending.computeIfAbsent(player,k->queues()).get(i).addLast(new Contribution((int)Math.min(Integer.MAX_VALUE,delta),
                    new Authority(game.items().indirectKills().wildCause(player),game.items().epoch())));}
        if(pending.containsKey(player))sync(player);
    }
    private static List<ArrayDeque<Contribution>> queues(){return List.of(new ArrayDeque<>(),new ArrayDeque<>(),new ArrayDeque<>(),new ArrayDeque<>());}
    private ServerPlayer owner(Authority authority){return authority==null?null:game.server().getPlayerList().getPlayer(authority.cause.player());}
    private boolean target(ServerPlayer owner,Entity target){return owner!=null&&(target==owner?game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target):game.items().wands().projectileTarget(owner,target));}
    private boolean target(Authority authority,Entity target){
        return authority!=null&&(authority.cause.friendlyFire()
                ?game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target)
                :target(owner(authority),target));
    }
    private void effect(ServerPlayer owner,LbcIndirectKills.Owner cause,LivingEntity target,net.minecraft.core.Holder<MobEffect> kind,int duration,int amp){game.items().indirectKills().withCause(target,cause,()->target.addEffect(ItemEffectLevels.create(kind,duration,amp,false,false),owner));}
    private void shock(ServerLevel level,Vec3 at,Authority authority){
        var owner=owner(authority);if(owner==null)return;
        var players=level.players().stream().filter(p->p.position().distanceToSqr(at)<=25&&target(authority,p)).toList();
        var entities=level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(5),e->e.position().distanceToSqr(at)<=25&&target(authority,e));
        for(var player:players)effect(owner,authority.cause,player,MobEffects.NAUSEA,60,1);
        for(var entity:entities)effect(owner,authority.cause,entity,MobEffects.SPEED,60,1);
        for(var player:players)effect(owner,authority.cause,player,MobEffects.BLINDNESS,60,1);
        for(var entity:entities)effect(owner,authority.cause,entity,MobEffects.WITHER,60,10);
    }
    private void evoker(ServerLevel level,Vec3 at,Authority authority,boolean ground){
        var owner=owner(authority);if(owner==null)return;
        var entities=new ArrayList<Entity>();
        if(ground){entities.addAll(level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(8),e->!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<=64&&target(authority,e)));entities.addAll(level.players().stream().filter(p->p.position().distanceToSqr(at)<=64&&target(authority,p)).toList());}
        else entities.addAll(level.getEntities((Entity)null,new AABB(at,at).inflate(8),e->!(e instanceof net.minecraft.world.entity.projectile.EvokerFangs)&&e.position().distanceToSqr(at)<=64&&target(authority,e)));
        for(var entity:entities)for(int[] offset:new int[][]{{0,0},{1,1},{-1,-1},{1,-1},{-1,1},{1,0},{-1,0},{0,1},{0,-1}})fang(entity.position().add(offset[0],0,offset[1]),authority);
    }
    private void fang(Vec3 at,Authority authority){
        var author=owner(authority);if(author==null)return;
        var fang=game.items().evokerBass().spawnQuiverOwned(author,at);if(fang==null)return;
        children.put(fang,authority);fang.entityTags().add(CHILD);game.items().markCurrentItemEpoch(fang);
    }
    private void explode(ServerLevel level,Vec3 at,Authority authority){if(owner(authority)!=null&&level.hasChunkAt(BlockPos.containing(at)))game.items().babylonEffects().quiverExplosionAt(level,at,authority.cause);}
    private void trail(ServerLevel level,Vec3 at,int amp,Authority authority){if(!level.hasChunkAt(BlockPos.containing(at)))return;var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().addAll(List.of(TRAIL,"slowraycast","magic","other",amp==112?"lightarrowtrail":"trappertrail"));game.items().markCurrentItemEpoch(marker);trails.put(marker,new Trail(amp,authority));if(!level.addFreshEntity(marker))trails.remove(marker);}
    private void tickTrails(){for(var marker:List.copyOf(trails.keySet())){var trail=trails.get(marker);if(marker.isRemoved()||!runtime(marker)||!game.items().currentEpoch(trail.authority.epoch)){trails.remove(marker);marker.discard();continue;}if(frozen(marker))continue;var level=(ServerLevel)marker.level();var at=marker.position();int age=++trail.age;
        if(trail.amp==112){level.sendParticles(ParticleTypes.END_ROD,at.x,at.y,at.z,1,0,0,0,0);if(age==50||age==100||age==150){int from=age==50?0xffffff:age==100?0xffff00:0xff0000;int to=age==50?0xffff00:age==100?0xff0000:0x000000;level.sendParticles(new DustColorTransitionOptions(from,to,4),true,false,at.x,at.y+.5,at.z,10,1,.5,1,.05);}if(age==199)column(level,at,trail.authority);if(age>=200)removeTrail(marker);}
        else {if(age>=120&&level.getRandom().nextFloat()<.25f&&level.players().stream().anyMatch(p->p.position().distanceToSqr(at)<=6400))trapper(marker,trail);if(age>=400)removeTrail(marker);}
    }}
    private void trapper(net.minecraft.world.entity.Marker marker,Trail trail){var level=(ServerLevel)marker.level();final var center=marker.position();var at=center;var owner=owner(trail.authority);if(owner==null)return;var nearby=level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(15),e->e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(center)<=225&&target(trail.authority,e));if(nearby.stream().anyMatch(e->e.position().distanceToSqr(center)<=25))fang(at,trail.authority);
        if(nearby.stream().noneMatch(e->e.position().distanceToSqr(center)<=2.25)&&level.getRandom().nextFloat()<.5f){var nearest=nearby.stream().min(Comparator.comparingDouble(e->e.position().distanceToSqr(center))).orElse(null);if(nearest!=null){var delta=nearest.position().subtract(at);if(delta.lengthSqr()>1e-12)at=at.add(delta.normalize());}}
        at=new Vec3(Math.floor(at.x)+level.getRandom().nextInt(3)-1,Math.floor(at.y)+level.getRandom().nextInt(3)-1,Math.floor(at.z)+level.getRandom().nextInt(3)-1);if(level.hasChunkAt(BlockPos.containing(at)))marker.setPos(at);if(trail.age>=390)removeTrail(marker);
    }
    private static final int[] TNT_Y={0,3,-1,-2,-3,-4,-5,-7,-9,-11,-13,-15,-17,-19,-21,-23,-25,-27,-29,-31,-33,-35,-37,-39,-41,-43,-45};
    private static final int[] BOLT_Y={0,3,-1,-2,-3,-4,-5,-6,-7,-9,-13,-17,-21,-25,-33,-45};
    private void column(ServerLevel level,Vec3 at,Authority authority){var owner=owner(authority);if(owner==null)return;for(int y:TNT_Y){var pos=at.add(0,y,0);if(!level.hasChunkAt(BlockPos.containing(pos)))continue;var tnt=new PrimedTnt(EntityTypes.TNT,level);tnt.setPos(pos);tnt.setDeltaMovement(Vec3.ZERO);tnt.setFuse(0);child(tnt,authority);}for(int y:BOLT_Y){var pos=at.add(0,y,0);if(!level.hasChunkAt(BlockPos.containing(pos)))continue;var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setPos(pos);bolt.setCause(owner);var original=authority.cause;child(bolt,new Authority(new LbcIndirectKills.Owner(original.player(),original.order(),true,true),authority.epoch));}}
    private void child(Entity entity,Authority authority){children.put(entity,authority);entity.entityTags().add(CHILD);game.items().markCurrentItemEpoch(entity);if(!((ServerLevel)entity.level()).addFreshEntity(entity))children.remove(entity);}
    private void removeTrail(net.minecraft.world.entity.Marker marker){trails.remove(marker);if(!marker.isRemoved())marker.discard();}
    public LbcIndirectKills.Owner cause(Entity entity){var state=children.get(entity);return state==null?null:state.cause;}
    public LivingEntity owner(Entity entity){var state=children.get(entity);return owner(state);}
    public boolean managedSecondary(Entity entity){return children.containsKey(entity);}
    public boolean explosionTarget(Entity source,Entity victim){return !children.containsKey(source)||target(children.get(source),victim);}
    public boolean canAffect(Entity source,Entity victim){if(source instanceof LightningBolt&&children.containsKey(source))return game.items().wands().validRoleTarget(victim,false)&&!LbcGreed.active(victim);return explosionTarget(source,victim);}
    public boolean lightningTarget(LightningBolt source,Entity victim){return canAffect(source,victim);}
    public boolean allowDamage(LivingEntity victim,DamageSource source){return !children.containsKey(source.getDirectEntity())||canAffect(source.getDirectEntity(),victim);}
    public void inheritChild(Entity source,Entity child){var authority=children.get(source);if(authority!=null&&child instanceof PrimedTnt){children.put(child,authority);child.entityTags().add(CHILD);game.items().markCurrentItemEpoch(child);}}
    public int queued(LivingEntity target,int amp){var all=pending.get(target);int index=index(amp);return all==null||index<0?0:all.get(index).stream().mapToInt(c->c.remaining).sum();}
    private void sync(LivingEntity target){var data=new CompoundTag();data.putString("epoch",game.items().epoch());var entries=new ListTag();var all=pending.get(target);for(int i=0;i<AMPS.length;i++){game.items().mana().score(target,SCORES[i]).set(queued(target,AMPS[i]));if(all!=null)for(var c:all.get(i)){var tag=new CompoundTag();tag.putInt("amp",AMPS[i]);tag.putInt("remaining",c.remaining);tag.putString("owner",c.authority.cause.player().toString());tag.putLong("order",c.authority.cause.order());tag.putBoolean("wild",c.authority.cause.friendlyFire());entries.add(tag);}}data.put("entries",entries);target.setAttached(DATA,data);}
    private void tickEffects(){for(var target:List.copyOf(pending.keySet())){if(game.items().polymorph().masked(target))continue;if(target.entityTags().contains("sin_greed_active"))continue;if(!runtime(target)||!game.items().wands().validTarget(target)||skillImmune(target)||target instanceof ServerPlayer p&&game.items().magic().rejectCustomEffect(p)){clear(target);continue;}if(frozen(target)||target.entityTags().contains("sin_greed_active"))continue;var all=pending.get(target);for(int i=0;i<AMPS.length;i++){var next=all.get(i).peekFirst();if(next==null)continue;if(!game.items().currentEpoch(next.authority.epoch)){all.get(i).removeFirst();continue;}var level=(ServerLevel)target.level();var at=target.position();int amp=AMPS[i];switch(amp){case 102->evoker(level,at,next.authority,false);case 103->{if(!game.items().magic().mechanismSuppressedAt(level,at))shock(level,at,next.authority);}case 112,113->{if(!game.items().magic().beaconSuppressed(at)&&level.getRandom().nextFloat()<(amp==112?.25f:.35f))trail(level,at,amp,next.authority);}default->{}}if(--next.remaining<=0)all.get(i).removeFirst();}sync(target);if(all.stream().allMatch(Deque::isEmpty)){pending.remove(target);target.removeAttached(DATA);}}}
    public void tick(){if(game.maps().level()==null)return;if(!game.items().timeStop().stopped(game.maps().level())&&++clock%300==0)for(var player:game.server().getPlayerList().getPlayers())pulse(player);tickEffects();tickTrails();for(var entity:List.copyOf(children.keySet()))if(entity.isRemoved())children.remove(entity);else if(!runtime(entity)||!game.items().currentEpoch(children.get(entity).epoch)){children.remove(entity);entity.discard();}}
    public void clear(LivingEntity target){pending.remove(target);target.removeAttached(DATA);for(var score:SCORES)if(game.items().mana().hudValue(target,score)!=0)game.items().mana().score(target,score).set(0);}
    public void loaded(Entity entity){if(entity instanceof Arrow arrow){if(arrow.entityTags().contains(SHOT)&&!game.items().currentEpoch(arrow)){arrow.discard();return;}shot(arrow);}if(entity.entityTags().contains(TRAIL)&&!trails.containsKey(entity)||entity.entityTags().contains(CHILD)&&!children.containsKey(entity)){entity.discard();return;}if(entity instanceof LivingEntity target){var data=target.getAttached(DATA);if(data==null)return;if(!game.items().currentEpoch(data.getStringOr("epoch",""))){clear(target);return;}var all=queues();for(var entry:data.getListOrEmpty("entries")){if(!(entry instanceof CompoundTag tag))continue;int index=index(tag.getIntOr("amp",0)),remaining=tag.getIntOr("remaining",0);if(index<0||remaining<=0)continue;try{var cause=game.items().indirectKills().restoreColdCause(UUID.fromString(tag.getStringOr("owner","")),tag.getLongOr("order",0),true);all.get(index).addLast(new Contribution(remaining,new Authority(new LbcIndirectKills.Owner(cause.player(),cause.order(),cause.projectile(),tag.getBooleanOr("wild",false)),game.items().epoch())));}catch(IllegalArgumentException ignored){}}pending.put(target,all);sync(target);}}
    public void unloaded(Entity entity){if(entity instanceof Arrow arrow)shots.remove(arrow);if(entity instanceof net.minecraft.world.entity.Marker marker&&trails.containsKey(marker))removeTrail(marker);if(!(entity instanceof PrimedTnt&&entity.isRemoved())&&children.remove(entity)!=null&&!entity.isRemoved())entity.discard();if(entity instanceof LivingEntity living)pending.remove(living);}
    public void forget(ServerPlayer owner){clear(owner);for(var entry:List.copyOf(shots.entrySet()))if(entry.getValue().owner==owner){entry.getKey().discard();shots.remove(entry.getKey());}for(var entry:List.copyOf(trails.entrySet()))if(entry.getValue().authority.cause.player().equals(owner.getUUID()))removeTrail(entry.getKey());for(var entry:List.copyOf(children.entrySet()))if(entry.getValue().cause.player().equals(owner.getUUID())){entry.getKey().discard();children.remove(entry.getKey());}}
    public void stop(){for(var arrow:List.copyOf(shots.keySet()))arrow.discard();shots.clear();for(var marker:List.copyOf(trails.keySet()))removeTrail(marker);for(var entity:List.copyOf(children.keySet()))entity.discard();children.clear();for(var target:List.copyOf(pending.keySet()))clear(target);clock=0;EFFECT_ARROW.remove();}
    List<Arrow> arrows(){return List.copyOf(shots.keySet());}List<net.minecraft.world.entity.Marker> trails(){return List.copyOf(trails.keySet());}List<Entity> children(){return List.copyOf(children.keySet());}
}
