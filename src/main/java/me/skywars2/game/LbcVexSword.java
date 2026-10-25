package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Source Vex Sword counters, anchored arrow field and native owned arrows. */
public final class LbcVexSword {
    public static final Item ITEM=registerItem();
    private static final String ARROW_TAG="skywars2.vex_sword_arrow";
    private static final net.minecraft.nbt.CompoundTag SWORD_FLAG=flag();
    private static net.minecraft.nbt.CompoundTag flag() { var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("vexsword",1);return tag; }
    private static final String CHARGING="swordcharging4", CHARGED="swordcharged4";
    private static final double[][] PHANTOM={
            {5,0,-1,0},{-5,0,1,0},{0,5,0,-1},{0,-5,0,1},
            {3.5,3.5,-.7,-.7},{-3.5,3.5,.7,-.7},{3.5,-3.5,-.7,.7},{-3.5,-3.5,.7,.7}};
    private final GameController game;
    private final LbcMana mana;
    private final Set<VexArrow> arrows=new LinkedHashSet<>();
    private final Set<Zone> zones=new LinkedHashSet<>();

    LbcVexSword(GameController game,LbcMana mana) { this.game=game; this.mana=mana; }
    public static void register() { }
    private static Item registerItem() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:vex_sword"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties() {
                    @Override public Identifier effectiveModel() { return Identifier.parse("lbc:vex_sword"); }
                }.setId(key).durability(25).overrideDescription("vex_sword"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isSword(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && (stack.is(ITEM) || data!=null && data.matchedBy(SWORD_FLAG));
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && owner.level()==game.maps().level() && mana.visible(owner)
                && game.items().wands().validTarget(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    /** Already launched projectiles retain their author after death; admission still requires an active player. */
    private boolean flightOwner(ServerPlayer owner) {
        return owner!=null && owner.level()==game.maps().level() && game.playing()
                && game.stage()!=GameController.Stage.ENDING;
    }
    public static boolean managed(Entity entity) { return entity instanceof LbcVexSword.VexArrow; }
    private boolean admitted(ServerPlayer owner) {
        return valid(owner) && !game.items().actionsBlocked(owner)
                && !game.items().timeStop().stopped(owner.level())
                && !game.items().magic().suppressed(owner)
                && !game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());
    }
    private boolean enemy(ServerPlayer owner,LivingEntity target) {
        return game.items().wands().target(owner,target) && !target.entityTags().contains("nomagic_active")
                && !game.items().magic().suppressedAt(owner.level(),target.position());
    }
    private boolean physicalTarget(ServerPlayer owner,LivingEntity target) {
        return target.level()==owner.level() && game.items().wands().projectileTarget(owner,target,true);
    }
    public int chargeProgress(ServerPlayer owner) { return mana.score(owner,CHARGING).get(); }
    public int charges(ServerPlayer owner) { return mana.score(owner,CHARGED).get(); }
    public int hudValue(ServerPlayer owner) { return charges(owner); }
    /** KFC's one source score unit is one accumulated charge, not ten admitted damage events. */
    public void addPlayerCounter(ServerPlayer owner) { mana.score(owner,CHARGED).add(1); }
    public boolean eligibleDamage(ServerPlayer owner,DamageSource source) {
        // (user) Only a genuine strike with this held sword can charge or create a side arrow.
        return source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                && source.getEntity()==owner && source.getDirectEntity()==owner && isSword(owner.getMainHandItem());
    }
    /** Called only after Minecraft has accepted positive damage. */
    public void damaged(LivingEntity victim,DamageSource source,float acceptedAmount) {
        if(!(acceptedAmount>0) || !(source.getEntity() instanceof ServerPlayer owner)
                || !eligibleDamage(owner,source) || !admitted(owner) || !physicalTarget(owner,victim)) return;
        var progress=mana.score(owner,CHARGING);
        progress.add(1);
        if(progress.get()>=10) { progress.set(0); mana.score(owner,CHARGED).add(1); }
        sideShot(owner,victim,owner.getRandom().nextBoolean()?1:-1);
    }
    /** The native damaged target supplies the single initial aim; native arrow flight follows. */
    Arrow sideShot(ServerPlayer owner,LivingEntity actualVictim,int side) {
        if(!admitted(owner) || !physicalTarget(owner,actualVictim)) return null;
        var forward=owner.getLookAngle();
        var left=new Vec3(Math.cos(Math.toRadians(owner.getYRot())),0,Math.sin(Math.toRadians(owner.getYRot())));
        var origin=owner.getEyePosition().add(left.scale(side<0?-3:3)).add(forward);
        var direction=actualVictim.getEyePosition().subtract(origin).normalize().scale(2);
        var arrow=spawn(owner,origin,direction,2,100,false);
        if(arrow!=null) {
            owner.level().sendParticles(ParticleTypes.CRIT,origin.x,origin.y,origin.z,10,.1,.1,.1,0);
            LbcOverload.sound(owner,origin,"minecraft:entity.arrow.shoot",1,2,0);
        }
        return arrow;
    }
    public boolean use(ServerPlayer owner,InteractionHand hand) {
        if(!isSword(owner.getItemInHand(hand)) || !admitted(owner) || charges(owner)<=0
                || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !loadedSegment(owner.level(),owner.position(),owner.position())) return false;
        mana.score(owner,CHARGED).add(-1);
        owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,1200,1,true,false),owner);
        owner.addEffect(new MobEffectInstance(MobEffects.LEVITATION,20,30,true,false),owner);
        owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,400,0),owner);
        zones.add(new Zone(owner,owner.position(),game.items().epoch()));
        var p=owner.position();
        var marker=new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState());
        for(var viewer:owner.level().players()) if(viewer.position().distanceToSqr(p)>=.01)
            owner.level().sendParticles(viewer,marker,true,false,p.x,p.y,p.z,300,10,5,10,0);
        for(int i=0;i<10;i++) LbcOverload.sound(owner,p,"minecraft:ambient.cave",5,1,0);
        game.items().wands().wandCasted(owner);
        return true;
    }
    static final class Zone {
        final ServerPlayer owner; final ServerLevel level; final Vec3 origin; final String epoch;
        int remaining=70;
        Zone(ServerPlayer owner,Vec3 origin,String epoch) {
            this.owner=owner; this.level=owner.level(); this.origin=origin; this.epoch=epoch;
        }
    }
    List<Zone> zoneSnapshot() { return List.copyOf(zones); }
    List<Arrow> arrowSnapshot() { return List.copyOf(arrows); }
    public int zoneCount() { return zones.size(); }
    public int arrowCount() { return arrows.size(); }
    public int activeCount() { return zones.size()+arrows.size(); }
    private boolean immune(LivingEntity target) {
        if(!(target instanceof ServerPlayer player)) return false;
        for(int i=0;i<player.getInventory().getContainerSize();i++)
            if(isSword(player.getInventory().getItem(i))) return true;
        return false;
    }
    public void tick() {
        arrows.removeIf(Entity::isRemoved);
        for(var zone:List.copyOf(zones)) {
            var owner=zone.owner;
            if(!flightOwner(owner) || owner.level()!=zone.level || !game.items().currentEpoch(zone.epoch)) { zones.remove(zone); continue; }
            if(game.items().timeStop().stopped(zone.level) || owner.entityTags().contains("time_stopped")
                    || !loadedSegment(zone.level,zone.origin,zone.origin)) continue;
            if(game.items().magic().suppressedAt(zone.level,zone.origin)) { zones.remove(zone); continue; }
            {
                var p=zone.origin;
                // Source selector deltas include their upper block coordinate (+1 in AABB terms).
                var bounds=new AABB(p.x-30,p.y-30,p.z-30,p.x+31,p.y+6,p.z+31);
                var targets=new ArrayList<>(zone.level.getEntitiesOfClass(LivingEntity.class,bounds,
                        target->enemy(owner,target) && !immune(target)));
                for(int i=0;i<5 && !targets.isEmpty();i++) {
                    // Every arrow rolls independently, including repeat hits on a still-living target.
                    targets.removeIf(target->!enemy(owner,target) || immune(target));
                    if(targets.isEmpty()) break;
                    var target=targets.get(owner.getRandom().nextInt(targets.size()));
                    phantomShot(owner,target,owner.getRandom().nextInt(8)+1);
                }
            }
            if(--zone.remaining<=0) zones.remove(zone);
        }
    }
    Arrow phantomShot(ServerPlayer owner,LivingEntity target,int direction) {
        if(direction<1 || direction>8) throw new IllegalArgumentException("phantom direction must be 1..8");
        if(!flightOwner(owner) || !enemy(owner,target) || immune(target)) return null;
        var data=PHANTOM[direction-1];
        var origin=target.position().add(data[0],0,data[1]);
        var arrow=spawn(owner,origin,new Vec3(data[2],0,data[3]),2.5,100,true);
        if(arrow!=null) {
            arrow.setCustomName(Component.translatable("phantom_arrow"));
            owner.level().sendParticles(ParticleTypes.SOUL,origin.x,origin.y,origin.z,5,.5,.5,.5,.1);
            LbcOverload.sound(owner,target.position(),"minecraft:entity.arrow.shoot",1,1,0);
        }
        return arrow;
    }
    private Arrow spawn(ServerPlayer owner,Vec3 origin,Vec3 motion,double damage,int pierce,boolean groundbreaker) {
        if(!loadedSegment(owner.level(),origin,origin.add(motion))) return null;
        var arrow=new VexArrow(owner.level(),groundbreaker);
        arrow.setOwner(owner); arrow.setPos(origin); arrow.setDeltaMovement(motion);
        arrow.setBaseDamage(damage);
        ((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$setPierceLevel((byte)pierce);
        arrow.pickup=AbstractArrow.Pickup.CREATIVE_ONLY;
        arrow.entityTags().add(ARROW_TAG);
        arrow.entityTags().add(groundbreaker?"groundbreaker":"vex_arrow");
        LbcShooters.orient(arrow);
        game.items().markCurrentItemEpoch(arrow);
        if(!owner.level().addFreshEntity(arrow)) return null;
        arrows.add(arrow); game.items().timeStop().prepareFrozen(arrow);
        return arrow;
    }
    private static boolean loadedSegment(ServerLevel level,Vec3 from,Vec3 to) {
        int minX=Math.min(BlockPos.containing(from).getX()>>4,BlockPos.containing(to).getX()>>4);
        int maxX=Math.max(BlockPos.containing(from).getX()>>4,BlockPos.containing(to).getX()>>4);
        int minZ=Math.min(BlockPos.containing(from).getZ()>>4,BlockPos.containing(to).getZ()>>4);
        int maxZ=Math.max(BlockPos.containing(from).getZ()>>4,BlockPos.containing(to).getZ()>>4);
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++)
            if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    private final class VexArrow extends Arrow {
        final boolean groundbreaker;
        boolean retired;
        VexArrow(ServerLevel level,boolean groundbreaker) { super(EntityTypes.ARROW,level); this.groundbreaker=groundbreaker; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override protected boolean canHitEntity(Entity target) {
            return getOwner() instanceof ServerPlayer owner && flightOwner(owner)
                    && game.items().wands().projectileTarget(owner,target)
                    && super.canHitEntity(target);
        }
        @Override public void tick() {
            if(retired || !(getOwner() instanceof ServerPlayer owner) || !flightOwner(owner)
                    || owner.level()!=level() || !game.items().currentEpoch(this)) { LbcVexSword.this.remove(this); return; }
            if(game.items().timeStop().stopped(owner.level()) || entityTags().contains("time_stopped")) return;
            var from=position(); var to=from.add(getDeltaMovement());
            if(!loadedSegment(owner.level(),from,to)) { LbcVexSword.this.remove(this); return; }
            super.tick();
        }
        @Override protected void onHitBlock(BlockHitResult hit) {
            if(groundbreaker && getOwner() instanceof ServerPlayer owner && flightOwner(owner)
                    && game.items().currentEpoch(this)) groundbreak(this,owner,hit.getLocation());
            // Source prein always kills the groundbreaker and killedarrow side shot on block entry.
            LbcVexSword.this.remove(this);
        }
    }
    private void groundbreak(VexArrow arrow,ServerPlayer owner,Vec3 at) {
        var level=owner.level();
        try {
            game.items().indirectKills().worldChanges(level,arrow,()-> {
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-.1,-.1,-.1)),BlockPos.containing(at.add(.1,.1,.1))))
                if(!level.isOutsideBuildHeight(pos) && level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null)
                    level.destroyBlock(pos,true,arrow);
        });
        } finally { game.items().indirectKills().finishSupportRemoval(); }
        level.sendParticles(ParticleTypes.EXPLOSION,at.x,at.y,at.z,1,0,0,0,0);
        LbcOverload.sound(owner,at,"minecraft:entity.generic.explode",1,2,0);
    }
    private void remove(VexArrow arrow) { arrows.remove(arrow); arrow.retired=true; if(!arrow.isRemoved()) arrow.discard(); }
    boolean loaded(Entity entity) {
        if(!entity.entityTags().contains(ARROW_TAG)) return false;
        if(!(entity instanceof VexArrow arrow) || arrow.retired || !game.items().currentEpoch(entity)
                || !(arrow.getOwner() instanceof ServerPlayer owner) || !flightOwner(owner) || owner.level()!=entity.level()) entity.discard();
        else arrows.add(arrow);
        return true;
    }
    void unloaded(Entity entity) { if(entity instanceof VexArrow arrow) remove(arrow); }
    public void purify(ServerPlayer owner) { mana.score(owner,CHARGING).set(0); mana.score(owner,CHARGED).set(0); }
    public void clear(ServerPlayer owner) {
        purify(owner);
        zones.removeIf(zone->zone.owner==owner);
        for(var arrow:List.copyOf(arrows)) if(arrow.getOwner()==owner) remove(arrow);
    }
    public void reset(ServerPlayer owner) { clear(owner); }
    public void forget(ServerPlayer owner) { clear(owner); }
    public void stop() {
        for(var player:game.server().getPlayerList().getPlayers()) { mana.score(player,CHARGING).set(0); mana.score(player,CHARGED).set(0); }
        zones.clear();
        for(var arrow:List.copyOf(arrows)) remove(arrow);
        arrows.clear();
    }
}
