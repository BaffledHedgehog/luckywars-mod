package me.skywars2.game;

import java.util.*;
import me.skywars2.mixin.ShulkerBowMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** The shulker_new bow replaces only its own successfully published native primary arrows. */
public final class LbcShulkerBow {
    private static final String SHOT="skywars2.shulker_bow",CHILD="skywars2.shulker_bow.child",KEY="skywars2_shulker_bow";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Set<ShulkerBullet> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcShulkerBow(GameController game){this.game=game;}
    public interface Carrier {State skywars2$shulkerState();void skywars2$shulkerState(State state);}
    /** Exact accepted ammo/weapon are immutable snapshots; mutable contact history belongs to this carrier. */
    public static final class State {
        public final ItemStack ammo,weapon;
        public final double baseDamage;
        public final boolean critical,primary;
        public final int pierce,spectral,trail,fire;
        public AbstractArrow.Pickup pickup=AbstractArrow.Pickup.DISALLOWED;
        public LbcIndirectKills.Owner cause;
        public final String epoch;
        public String team;
        public final Set<UUID> contacts=new LinkedHashSet<>();
        public int age;
        public boolean terminal;
        State(ItemStack ammo,ItemStack weapon,double baseDamage,boolean critical,int pierce,int spectral,int trail,int fire,
              LbcIndirectKills.Owner cause,String epoch,String team,boolean primary){
            this.ammo=ammo.copy();this.weapon=weapon.copy();this.baseDamage=baseDamage;this.critical=critical;this.pierce=pierce;
            this.spectral=spectral;this.trail=trail;this.fire=fire;this.cause=cause;this.epoch=epoch;this.team=team;this.primary=primary;
        }
        State copy(){var s=new State(ammo,weapon,baseDamage,critical,pierce,spectral,trail,fire,cause,epoch,team,primary);s.age=age;s.terminal=terminal;s.pickup=pickup;s.contacts.addAll(contacts);return s;}
    }
    public static boolean managed(Entity actor){return actor instanceof ShulkerBullet&&actor instanceof Carrier carrier&&carrier.skywars2$shulkerState()!=null;}
    private static State state(Entity actor){return actor instanceof Carrier c?c.skywars2$shulkerState():null;}
    private void install(ShulkerBullet bullet,State state){((Carrier)bullet).skywars2$shulkerState(state);actors.add(bullet);bullet.entityTags().add(state.primary?SHOT:CHILD);game.items().markCurrentItemEpoch(bullet);}
    /** Real ownership transfers update authority without replacing accepted ammo or native flight state. */
    public void ownerChanged(Entity actor,ServerPlayer owner){
        var s=state(actor);if(s==null)return;
        var transferred=game.items().indirectKills().coldCause(owner,s.cause.projectile());
        s.cause=new LbcIndirectKills.Owner(transferred.player(),transferred.order(),s.cause.friendlyFire(),s.cause.projectile());
        s.team=owner.getTeam()==null?"":owner.getTeam().getName();
    }
    private LbcIndirectKills.Owner physicalCause(ServerPlayer owner){var wild=game.items().indirectKills().wildCause(owner);return new LbcIndirectKills.Owner(wild.player(),wild.order(),true,true);}
    public List<Projectile> augment(ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        if(LbcFartus.data(weapon).getIntOr("shulker_new",0)!=1||game.items().magic().suppressed(owner))return family;
        var result=new ArrayList<Projectile>(family.size());
        for(var projectile:family){
            if(!(projectile instanceof AbstractArrow arrow)||arrow.isRemoved()||arrow.getOwner()!=owner||arrow.level()!=owner.level()){result.add(projectile);continue;}
            var ammo=arrow.getPickupItemStackOrigin().copy();int trail=0;
            for(var effect:ammo.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects())
                if(effect.is(MobEffects.RAID_OMEN)&&(effect.getAmplifier()==112||effect.getAmplifier()==113))trail=effect.getAmplifier();
            var acceptedWeapon=arrow.getWeaponItem();
            var snapshot=new State(ammo,acceptedWeapon==null?weapon:acceptedWeapon,((ShulkerBowMixin.ArrowAccess)arrow).skywars2$baseDamage(),
                    arrow.isCritArrow(),Byte.toUnsignedInt(arrow.getPierceLevel()),arrow instanceof SpectralArrow spectral?((ShulkerBowMixin.SpectralAccess)spectral).skywars2$duration():0,
                    trail,arrow.getRemainingFireTicks(),physicalCause(owner),game.items().epoch(),owner.getTeam()==null?"":owner.getTeam().getName(),true);
            snapshot.pickup=arrow.pickup;
            var bullet=new ShulkerBullet(EntityTypes.SHULKER_BULLET,owner.level());
            bullet.setOwner(owner);bullet.snapTo(arrow.getX(),arrow.getY(),arrow.getZ(),arrow.getYRot(),arrow.getXRot());
            bullet.setDeltaMovement(game.items().timeStop().persistedVelocity(arrow,arrow.getDeltaMovement()));bullet.setNoGravity(game.items().timeStop().persistedNoGravity(arrow,arrow.isNoGravity()));bullet.setSilent(arrow.isSilent());
            bullet.entityTags().addAll(arrow.entityTags());install(bullet,snapshot);game.items().timeStop().prepareFrozen(bullet);
            if(owner.level().addFreshEntity(bullet)){arrow.discard();result.add(bullet);}else {actors.remove(bullet);result.add(arrow);}
        }
        return result;
    }
    private boolean context(Entity actor,State s){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(s.epoch)
            &&actor.level()==game.maps().level()&&LbcDimensionItems.ordinary(actor.level());}
    private boolean frozen(Entity actor){return game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().stopped((ServerLevel)actor.level())||game.items().timeStop().frozen(actor)||actor.entityTags().contains("time_stopped");}
    private boolean resident(ServerLevel level,Vec3 at){var p=BlockPos.containing(at);return level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    /** Same native DDA positions as the forthcoming ray; reads chunk residency only. */
    private boolean residentSegment(ServerLevel level,Vec3 from,Vec3 to){
        if(!resident(level,from)||!resident(level,to))return false;
        return net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,level,
                (world,pos)->world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null?Boolean.FALSE:null,
                world->Boolean.TRUE);
    }
    private boolean residentCardinals(ServerLevel level,Vec3 at){
        var pos=BlockPos.containing(at);
        if(!resident(level,at))return false;
        for(var direction:Direction.values())if(!resident(level,Vec3.atCenterOf(pos.relative(direction))))return false;
        return true;
    }
    /** Runs after vanilla gravity/steering and before its block ray or position update. */
    public boolean beforeNativeCollision(ShulkerBullet bullet){var s=state(bullet);if(s==null)return true;
        var level=(ServerLevel)bullet.level();var to=bullet.position().add(bullet.getDeltaMovement());
        return residentSegment(level,bullet.position(),to)&&(s.primary||residentCardinals(level,to));
    }
    /** Source MAG only gates augmentation; released bullets are deleted by TECH. */
    public boolean beforeBullet(ShulkerBullet bullet){var s=state(bullet);if(s==null){if(cloneRecognizes(bullet)){retire(bullet);return false;}return true;}
        actors.add(bullet);if(!context(bullet,s)){retire(bullet);return false;}if(frozen(bullet))return false;
        var level=(ServerLevel)bullet.level();if(!resident(level,bullet.position())||!resident(level,bullet.position().add(bullet.getDeltaMovement())))return false;
        if(game.items().magic().mechanismSuppressedAt(level,bullet.position())
                ||Double.isFinite(game.items().magic().mechanismSuppressionFraction(level,bullet.position(),bullet.position().add(bullet.getDeltaMovement())))){retire(bullet);return false;}
        s.age++;
        if(bullet.getOwner() instanceof ServerPlayer owner)game.items().quiver().reactive(owner,level,bullet.position(),s.ammo,s.cause);
        if(s.trail==112)game.items().quiver().shulkerLightTrail(level,bullet.position(),s.cause);
        if(s.trail==113)child(bullet,s);
        return true;
    }
    private boolean target(State s,Entity victim){return victim instanceof LivingEntity&&victim.getType().builtInRegistryHolder().is(MOBS)
            &&!s.cause.player().equals(victim.getUUID())&&game.items().wands().validTarget(victim)&&game.items().wands().validRoleTarget(victim,false)
            &&(!(victim instanceof ServerPlayer player)||player.getTeam()==null||s.team.isEmpty()||!s.team.equals(player.getTeam().getName()));}
    public boolean canHit(ShulkerBullet bullet,Entity target){var s=state(bullet);return s==null||(!s.contacts.contains(target.getUUID())&&game.items().wands().validTarget(target));}
    public void beamSample(ServerPlayer owner,ItemStack weapon,net.minecraft.world.entity.projectile.arrow.Arrow context,Vec3 at,LbcIndirectKills.Owner cause){
        if(ItemData.intValue(weapon,"shulker_new",0)!=1||game.items().magic().mechanismSuppressedAt(owner.level(),at))return;
        boolean trapper=false;
        for(var effect:context.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects())
            if(effect.is(MobEffects.RAID_OMEN)&&effect.getAmplifier()==113)trapper=true;
        if(!trapper)return;
        var parent=new ShulkerBullet(EntityTypes.SHULKER_BULLET,owner.level());parent.setOwner(owner);parent.setPos(at);
        child(parent,new State(ItemStack.EMPTY,weapon.copy(),2,false,0,0,113,0,cause,game.items().epoch(),owner.getTeam()==null?"":owner.getTeam().getName(),true));
    }
    private void child(ShulkerBullet parent,State s){var level=(ServerLevel)parent.level();
        var target=level.getEntitiesOfClass(LivingEntity.class,new AABB(parent.position(),parent.position()).inflate(100),e->target(s,e)&&e.position().distanceToSqr(parent.position())<=10000)
                .stream().min(Comparator.comparingDouble(e->e.distanceToSqr(parent))).orElse(null);
        var owner=parent.getOwner();ShulkerBullet child;
        // Native direction selection probes cardinal blocks, first at owner and then at parent.
        if(target!=null&&(!residentCardinals(level,parent.position())
                ||owner instanceof LivingEntity&&!residentCardinals(level,owner.getBoundingBox().getCenter())))return;
        if(owner instanceof LivingEntity living&&target!=null)child=new ShulkerBullet(level,living,target,Direction.Axis.Y);
        else {child=new ShulkerBullet(EntityTypes.SHULKER_BULLET,level);child.setOwner(owner);}
        child.setPos(parent.position());
        if(target!=null)((ShulkerBowMixin.BulletAccess)child).skywars2$selectDirection(Direction.Axis.Y,target);
        install(child,new State(ItemStack.EMPTY,ItemStack.EMPTY,2,false,0,0,0,0,s.cause,s.epoch,s.team,false));
        game.items().timeStop().prepareFrozen(child);if(!level.addFreshEntity(child))actors.remove(child);
    }
    /** Single damage event retains the special bullet's four-damage floor and native levitation. */
    public void entityHit(ShulkerBullet bullet,EntityHitResult hit){var s=state(bullet);if(s==null)return;var victim=hit.getEntity();
        if(!context(bullet,s)||!canHit(bullet,victim))return;s.contacts.add(victim.getUUID());
        var level=(ServerLevel)bullet.level();var owner=bullet.getOwner();var source=game.items().indirectKills().captureDamage(bullet.damageSources().mobProjectile(bullet,owner instanceof LivingEntity living?living:null),s.cause);
        // The author keeps the legacy bullet damage; ammo effects and independent traits remain attached.
        int damage=4;
        int oldFire=victim.getRemainingFireTicks();if(s.fire>0)victim.igniteForSeconds(5);
        if(victim.hurtOrSimulate(source,damage)){
            if(victim instanceof LivingEntity living){
                game.items().indirectKills().withCause(living,s.cause,()->{
                    living.addEffect(new MobEffectInstance(MobEffects.LEVITATION,200),owner==null?bullet:owner);
                    if(s.spectral>0)living.addEffect(new MobEffectInstance(MobEffects.GLOWING,s.spectral),owner==null?bullet:owner);
                    s.ammo.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).forEachEffect(effect->{
                        if(!game.items().quiver().acceptCarrier(living,effect,owner instanceof ServerPlayer player?player:null,s.cause))living.addEffect(effect,owner==null?bullet:owner);
                    },s.ammo.getOrDefault(DataComponents.POTION_DURATION_SCALE,1f));
                });
                double knockback=s.weapon.isEmpty()?0:EnchantmentHelper.modifyKnockback(level,s.weapon,living,source,0);
                if(knockback>0){var move=bullet.getDeltaMovement().multiply(1,0,1).normalize().scale(knockback*.6*Math.max(0,1-living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));if(move.lengthSqr()>0)living.push(move.x,.1,move.z);}
            }
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level,victim,source,s.weapon.isEmpty()?null:s.weapon);
            if(!victim.isAlive()&&owner instanceof ServerPlayer player&&s.primary)net.minecraft.advancements.triggers.CriteriaTriggers.KILLED_BY_ARROW.trigger(player,List.of(victim),s.weapon.isEmpty()?null:s.weapon);
        }else victim.setRemainingFireTicks(oldFire);
    }
    public boolean keepAfterContact(ShulkerBullet bullet,HitResult hit){var s=state(bullet);return s!=null&&hit instanceof EntityHitResult&&s.primary&&s.contacts.size()<=s.pierce;}
    public void blockHit(ShulkerBullet bullet,BlockHitResult hit){var s=state(bullet);if(s==null)return;
        if(!s.terminal){s.terminal=true;if(bullet.getOwner() instanceof ServerPlayer owner)
            game.items().quiver().groundCarrier(bullet,owner,(ServerLevel)bullet.level(),hit.getLocation(),bullet.getYRot(),bullet.getXRot(),s.ammo,s.cause);}

        if(s.primary&&s.pickup==AbstractArrow.Pickup.ALLOWED&&!s.ammo.isEmpty())bullet.spawnAtLocation((ServerLevel)bullet.level(),s.ammo.copy(),.1f);
        if(s.weapon.isEmpty())return;
        var level=(ServerLevel)bullet.level();EnchantmentHelper.onHitBlock(level,s.weapon,bullet.getOwner() instanceof LivingEntity living?living:null,bullet,null,
                hit.getBlockPos().clampLocationWithin(hit.getLocation()),level.getBlockState(hit.getBlockPos()),item->{});
    }
    public ItemStack ammo(ShulkerBullet bullet){var s=state(bullet);return s==null?ItemStack.EMPTY:s.ammo.copy();}
    public ItemStack weapon(ShulkerBullet bullet){var s=state(bullet);return s==null?null:s.weapon;}
    public void loaded(Entity actor){if(actor instanceof ShulkerBullet bullet&&cloneRecognizes(actor)){if(managed(actor)&&context(actor,state(actor)))actors.add(bullet);else retire(bullet);}}
    public void unloaded(Entity actor){actors.remove(actor);}
    public void tick(){for(var bullet:List.copyOf(actors))if(bullet.isRemoved())actors.remove(bullet);else if(!context(bullet,state(bullet)))retire(bullet);}
    private void retire(ShulkerBullet bullet){actors.remove(bullet);bullet.discard();}
    public void forget(ServerPlayer owner){for(var bullet:List.copyOf(actors))if(state(bullet).cause.player().equals(owner.getUUID()))retire(bullet);}
    public void reindexRestored(Iterable<Entity> restored){actors.clear();for(var actor:restored)loaded(actor);}
    public void stop(){for(var bullet:List.copyOf(actors))retire(bullet);actors.clear();}
    public LbcCloneSkills.ActorImage cloneImage(Entity actor){var state=state(actor);if(state==null)return null;var image=state.copy();
        return new LbcCloneSkills.ActorImage(){public void install(Entity original,Entity clone,Map<Entity,Entity> family){LbcShulkerBow.this.install((ShulkerBullet)clone,image.copy());}
            public void rollback(Entity clone){actors.remove(clone);if(clone instanceof Carrier c)c.skywars2$shulkerState(null);}};}
    public static boolean cloneRecognizes(Entity actor){return managed(actor)||actor.entityTags().contains(SHOT)||actor.entityTags().contains(CHILD);}
    public static void save(State s,ValueOutput output){if(s==null)return;var out=output.child(KEY);out.store("ammo",ItemStack.OPTIONAL_CODEC,s.ammo);out.store("weapon",ItemStack.OPTIONAL_CODEC,s.weapon);
        out.putBoolean("terminal",s.terminal);out.putDouble("damage",s.baseDamage);out.putBoolean("critical",s.critical);out.putBoolean("primary",s.primary);out.putInt("pierce",s.pierce);out.putInt("spectral",s.spectral);out.putInt("trail",s.trail);out.putInt("fire",s.fire);out.putInt("age",s.age);
        out.store("pickup",AbstractArrow.Pickup.LEGACY_CODEC,s.pickup);out.putBoolean("friendly",s.cause.friendlyFire());out.putString("owner",s.cause.player().toString());out.putLong("order",s.cause.order());out.putString("epoch",s.epoch);out.putString("team",s.team);
        out.store("contacts",net.minecraft.core.UUIDUtil.CODEC.listOf(),List.copyOf(s.contacts));
    }
    public static State load(ValueInput input){var child=input.child(KEY);if(child.isEmpty())return null;var in=child.get();try{
        var s=new State(in.read("ammo",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY),in.read("weapon",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY),in.getDoubleOr("damage",2),in.getBooleanOr("critical",false),
                in.getIntOr("pierce",0),in.getIntOr("spectral",0),in.getIntOr("trail",0),in.getIntOr("fire",0),new LbcIndirectKills.Owner(UUID.fromString(in.getStringOr("owner","")),in.getLongOr("order",0),in.getBooleanOr("friendly",false),true),in.getStringOr("epoch",""),in.getStringOr("team",""),in.getBooleanOr("primary",false));
        s.pickup=in.read("pickup",AbstractArrow.Pickup.LEGACY_CODEC).orElse(AbstractArrow.Pickup.DISALLOWED);s.age=in.getIntOr("age",0);s.terminal=in.getBooleanOr("terminal",false);s.contacts.addAll(in.read("contacts",net.minecraft.core.UUIDUtil.CODEC.listOf()).orElse(List.of()));return s;
    }catch(IllegalArgumentException invalid){return null;}}
}






