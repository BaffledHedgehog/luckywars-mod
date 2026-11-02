package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Очереди стрелкового оружия; физика, попадания и автор урона остаются у ванильных стрел. */
public final class LbcShooters {
    private static final String PROJECTILE_TAG = "skywars2.razorpine";
    static final String HELICOPTER_PROJECTILE_TAG="skywars2.attack_helicopter";
    static final String BOSS_HELICOPTER_PROJECTILE_TAG="skywars2.boss_helicopter";
    private static final CompoundTag HELICOPTER_FLAG=new CompoundTag();
    private static final CompoundTag BOSS_HELICOPTER_FLAG=new CompoundTag();
    public static final Item HELICOPTER=registerHelicopter();
    public static final Item BOSS_HELICOPTER=registerBossHelicopter();
    private static final String PHANTOM_PROJECTILE_TAG = "skywars2.phantom_knives";
    private static final CompoundTag PHANTOM_FLAG = new CompoundTag();
    private static final double[] PHANTOM_ORIGINS = {.2,.1,0,-.1,-.2};
    private static final double[] PHANTOM_OFFSETS = {.3,.15,0,-.15,-.3};
    private static final int[] STAND_FAN = {-10,-5,5,10}, OVERHEAVEN_FAN = {-15,-11,-7,-3,3,7,11,15};
    private static final CompoundTag PINE_FLAG = new CompoundTag(), SLR_FLAG = new CompoundTag(), INFINITE_ARROW = new CompoundTag();
    private static final TagKey<Block> BREAKABLE_SLR = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:breakable_slr"));
    private static final TagKey<Item> AMMO = TagKey.create(Registries.ITEM, Identifier.parse("minecraft:razor_ammo"));
    private static final TagKey<Item> BEST_AMMO = TagKey.create(Registries.ITEM, Identifier.parse("minecraft:razor_ammo_best"));
    private static final CompoundTag WOODIFICATOR_FLAG=new CompoundTag();
    private static final BlockParticleOption WOODIFICATOR_PARTICLE=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.OAK_PLANKS.defaultBlockState());
    public static final Item WOODIFICATOR=registerWoodificator();
    public static final Item PINE = registerPine();
    public static final Item SLR = registerSlr();
    public static final Item KFC_GUN = registerKfc();
    public static final Item PHANTOM_KNIVES = registerPhantomKnives();
    private static final String KFC_OWNER = "skywars2.kfc.owner.";
    private static final CompoundTag KFC_FLAG = new CompoundTag();
    static { KFC_FLAG.putInt("kfcgun",1); WOODIFICATOR_FLAG.putInt("woodificator",1); }
    static { PHANTOM_FLAG.putInt("phantomknifes",1); HELICOPTER_FLAG.putInt("attack_helicopter",1); BOSS_HELICOPTER_FLAG.putInt("boss_helicopter",1); }
    static { PINE_FLAG.putInt("razorpine", 1); SLR_FLAG.putInt("slr",1); INFINITE_ARROW.putInt("infinite_arrow",1); }

    private final GameController game;
    private final Map<Block,BlockState> woodificatorPalette;
    private final Set<Block> woodificatorPreservedWood;
    private final Set<WoodificatorWork> woodificatorWork=new LinkedHashSet<>();
    private final Map<UUID,HelicopterBurst> helicopterBursts=new HashMap<>();
    private final Map<UUID,HelicopterBurst> bossHelicopterBursts=new HashMap<>();
    private static final class HelicopterBurst {
        final ServerPlayer owner; int remaining,phase;
        HelicopterBurst(ServerPlayer owner) { this.owner=owner; }
    }
    private static final class HelicopterArrow extends Arrow {
        boolean retired;
        HelicopterArrow(ServerLevel level) { super(EntityTypes.ARROW,level); }
        @Override public boolean shouldBeSaved() { return false; }
    }
    private final Map<UUID, Burst> bursts = new HashMap<>();
    private final Map<UUID, PhantomBurst> phantomBursts = new HashMap<>();
    private final Set<Arrow> arrows = new LinkedHashSet<>();
    private final Map<UUID,Integer> slrCooldowns = new HashMap<>();
    private final Map<UUID,Integer> kfcCooldowns = new HashMap<>();
    private final Map<Chicken,UUID> chickens = new java.util.IdentityHashMap<>();
    private final Map<UUID,Object> chickenGenerations = new HashMap<>();
    /** Native chicken physics; neither disk reload nor a previous owner life may restore the summon. */
    private final class KfcChicken extends Chicken {
        final UUID ownerId;
        final Object generation;
        KfcChicken(ServerPlayer owner) {
            super(EntityTypes.CHICKEN,owner.level());
            ownerId=owner.getUUID();
            generation=chickenGenerations.computeIfAbsent(ownerId,ignored->new Object());
        }
        boolean currentOwnerLife() { return chickenGenerations.get(ownerId)==generation; }
        @Override public boolean shouldBeSaved() { return false; }
    }
    private java.util.IdentityHashMap<LivingEntity,Stun> stuns = new java.util.IdentityHashMap<>();
    private int statusCallbacks;
    private boolean statusRestoring;
    private final java.util.IdentityHashMap<LivingEntity,Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,Integer>> restoredStunAges=new java.util.IdentityHashMap<>();
    private static final Identifier STUN_MOVEMENT=Identifier.parse("skywars2:stun_movement");
    private record StunEffect(MobEffectInstance installed,MobEffectInstance previous,int installedAt){}
    private static final class Stun {
        ServerPlayer owner;
        boolean retainedOwner,retainedTarget;
        final String epoch;
        final float yaw,pitch;
        final Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,StunEffect> effects=new java.util.IdentityHashMap<>();
        int ticks;
        Stun(ServerPlayer owner,LivingEntity target,String epoch,int ticks) {
            this.owner=owner; this.epoch=epoch; this.ticks=ticks; yaw=target.getYRot(); pitch=target.getXRot();
        }
    }
    private static final class Burst {
        final ServerPlayer owner;
        final boolean best;
        int counter = 20, delay = 2;
        Burst(ServerPlayer owner, boolean best) { this.owner = owner; this.best = best; }
    }
    private static final class PhantomBurst {
        final ServerPlayer owner;
        int cooldown=25, delay=2;
        PhantomBurst(ServerPlayer owner) { this.owner=owner; }
    }

    LbcShooters(GameController game) {
        this.game=game;
        var palette=new java.util.IdentityHashMap<Block,BlockState>();
        var preserved=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Block,Boolean>());
        // Only IDs are loaded. Texture sampling/color matching belongs to the offline generator.
        try(var input=LbcShooters.class.getResourceAsStream("/skywars2/woodificator_palette.json")) {
            if(input==null) throw new IllegalStateException("Missing generated Woodificator palette");
            try(var reader=new com.google.gson.stream.JsonReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8))) {
                reader.beginObject();
                while(reader.hasNext()) {
                    switch(reader.nextName()) {
                        case "mapping" -> {
                            reader.beginObject();
                            while(reader.hasNext()) {
                                var source=paletteBlock(reader.nextName()); var target=paletteBlock(reader.nextString());
                                if(!target.defaultBlockState().isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO)
                                        || target.defaultBlockState().hasBlockEntity())
                                    throw new IllegalStateException("Invalid Woodificator target: "+target);
                                palette.put(source,target.defaultBlockState());
                            }
                            reader.endObject();
                        }
                        case "preserved_wood" -> {
                            reader.beginArray();
                            while(reader.hasNext()) preserved.add(paletteBlock(reader.nextString()));
                            reader.endArray();
                        }
                        default -> reader.skipValue();
                    }
                }
                reader.endObject();
            }
        } catch(java.io.IOException failure) {
            throw new IllegalStateException("Cannot load generated Woodificator palette",failure);
        }
        if(palette.isEmpty() || preserved.isEmpty()) throw new IllegalStateException("Incomplete generated Woodificator palette");
        woodificatorPalette=java.util.Collections.unmodifiableMap(palette);
        woodificatorPreservedWood=java.util.Collections.unmodifiableSet(preserved);
    }
    private static Block paletteBlock(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id))
                .orElseThrow(()->new IllegalStateException("Unknown Woodificator block: "+id));
    }
    static void register() { }
    private static Item registerWoodificator() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:woodificator"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties() {
                    @Override public Identifier effectiveModel() { return Identifier.parse("lbc:woodificator"); }
                }.setId(key).durability(25).overrideDescription("woodificator"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isWoodificator(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(WOODIFICATOR) || data!=null && data.matchedBy(WOODIFICATOR_FLAG);
    }
    BlockState woodificatorReplacement(BlockState state) {
        // Wooden furniture/shapes stay intact, but actual item storage still transforms and spills.
        if(!state.hasBlockEntity() && woodificatorPreservedWood.contains(state.getBlock())) return state;
        var replacement=woodificatorPalette.get(state.getBlock());
        return replacement==null || replacement.getBlock()==state.getBlock()?state:replacement;
    }
    private static void spillWoodificatorContents(ServerLevel level,BlockPos pos) {
        var storage=level.getBlockEntity(pos);
        // Item-only removal: generic preRemoveSideEffects also releases furnace XP/other actors.
        if(storage instanceof net.minecraft.world.Container container) {
            net.minecraft.world.Containers.dropContents(level,pos,container);
            container.clearContent();
        } else if(storage instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire) {
            net.minecraft.world.Containers.dropContents(level,pos,campfire.getItems());
            campfire.clearContent();
        } else if(storage instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern) {
            net.minecraft.world.Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),lectern.getBook());
            lectern.clearContent();
        } else if(storage instanceof net.minecraft.world.level.block.entity.BrushableBlockEntity brushable) {
            net.minecraft.world.Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),brushable.getItem());
        }
    }
    int woodificatorWorkCount() { return woodificatorWork.size(); }
    void cancelQueuedTerrainForRollback(){woodificatorWork.clear();}
    int woodificatorQueuedBlockCount() { return woodificatorWork.stream().mapToInt(job->job.terrain.queuedBlockCount()).sum(); }
    boolean useWoodificator(ServerPlayer owner,InteractionHand hand) {
        if(hand==InteractionHand.OFF_HAND && !game.config().offhandWands || !canFire(owner)
                || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        var job=woodificatorWork.stream().filter(work->work.owner==owner && work.level==owner.level()
                && game.items().currentEpoch(work.epoch)).findFirst().orElseGet(()->new WoodificatorWork(owner));
        var from=owner.getEyePosition(); var direction=owner.getLookAngle();
        // Distance is incremented before recursion: the source samples 0..75 inclusive, through walls/entities.
        // Union masks retain exactly the overlapping 5^3 cubes; no enclosing corridor or duplicate writes.
        for(int sample=0;sample<=50;sample++) {
            var point=from.add(direction.scale(sample*1.5));
            var center=BlockPos.containing(point);
            job.terrain.include(new BlockAreaWork.Bounds(center.getX()-2,center.getY()-2,center.getZ()-2,
                    center.getX()+2,center.getY()+2,center.getZ()+2));
            owner.level().sendParticles(WOODIFICATOR_PARTICLE,true,false,point.x,point.y,point.z,1,0,0,0,0);
            if(owner.getRandom().nextFloat()<.05f)
                LbcOverload.sound(owner,point,"minecraft:block.roots.break",.5f,1,0);
        }
        // Repeated free casts merge into the owner's pending masks without dropping accepted regions.
        if(job.terrain.hasWork() && woodificatorWork.add(job)) game.items().stonekivi().queuedWork(job);
        return true;
    }
    private final class WoodificatorWork implements LbcStonekivi.Work {
        final ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        final BlockAreaWork terrain;
        boolean cancelled;
        WoodificatorWork(ServerPlayer owner) {
            this.owner=owner; level=owner.level(); epoch=game.items().epoch();
            terrain=new BlockAreaWork(level,state->replacement(state)!=state,(pos,state)-> {
                var result=replacement(state);
                game.items().indirectKills().supportRemoved(level,owner,pos);
                spillWoodificatorContents(level,pos);
                return result;
            },Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
                    |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE,false,true);
        }
        BlockState replacement(BlockState state) { return woodificatorReplacement(state); }
        @Override public boolean step() {
            if(cancelled || !active(owner) || owner.level()!=level || !game.items().currentEpoch(epoch)) { cancel(); return true; }
            if(game.items().wands().timeStopped() || game.items().wands().inputTimeStopped(owner)) return false;
            try { terrain.step(game,owner,512,System.nanoTime()+1_000_000); }
            finally { game.items().indirectKills().finishSupportRemoval(); }
            if(!terrain.hasWork()) { cancel(); return true; }
            return false;
        }
        @Override public void cancel() { cancelled=true; terrain.clear(); woodificatorWork.remove(this); }
    }
    private static Item registerHelicopter() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:attack_helicopter"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties() {
                    @Override public Identifier effectiveModel() { return Identifier.parse("lbc:attack_helicopter"); }
                }.setId(key).durability(25).overrideDescription("attack_helicopter"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isHelicopter(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(HELICOPTER) || data!=null && data.matchedBy(HELICOPTER_FLAG);
    }
    private static Item registerBossHelicopter() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:boss_helicopter"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties() {
                    @Override public Identifier effectiveModel() { return Identifier.parse("lbc:helicopter_boss"); }
                }.setId(key).durability(25).overrideDescription("boss_helicopter"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isBossHelicopter(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(BOSS_HELICOPTER) || data!=null && data.matchedBy(BOSS_HELICOPTER_FLAG);
    }
    int bossHelicopterCooldownRemaining(ServerPlayer owner) {
        var burst=bossHelicopterBursts.get(owner.getUUID()); return burst==null?0:burst.remaining;
    }
    /** Source purify resets the shared timer; already fired native arrows retain their normal lifecycle. */
    void resetBossHelicopterCooldown(ServerPlayer owner) {
        bossHelicopterBursts.remove(owner.getUUID());
    }
    int bossHelicopterArrowCount() {
        return (int)arrows.stream().filter(arrow->!arrow.isRemoved() && arrow.entityTags().contains(BOSS_HELICOPTER_PROJECTILE_TAG)).count();
    }
    /** (user) Both helicopter weapons are technology, sharing the same mechanism-only suppression. */
    private boolean canBeginBossHelicopter(ServerPlayer owner) { return canFireHelicopter(owner); }
    boolean beginBossHelicopter(ServerPlayer owner,InteractionHand hand) {
        if(!isBossHelicopter(owner.getItemInHand(hand)) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || bossHelicopterCooldownRemaining(owner)>0 || !canBeginBossHelicopter(owner)) return false;
        var burst=bossHelicopterBursts.computeIfAbsent(owner.getUUID(),ignored->new HelicopterBurst(owner));
        burst.remaining=600;
        return true;
    }
    /** Source fires before decrement: 600..400 inclusive is 201 volleys, independently for each held hand. */
    void tickBossHelicopters() {
        for(var burst:List.copyOf(bossHelicopterBursts.values())) {
            var owner=burst.owner;
            if(!active(owner)) { forget(owner); continue; }
            if(game.items().wands().timeStopped() || game.items().wands().inputTimeStopped(owner)) continue;
            // Jammed volleys are skipped while the original recharge timer continues.
            if(burst.remaining>=400 && canFireHelicopter(owner)) {
                if(isBossHelicopter(owner.getMainHandItem())) helicopterVolley(owner,.75,true);
                if(game.config().offhandWands && isBossHelicopter(owner.getOffhandItem())) helicopterVolley(owner,1.45,true);
            }
            if(--burst.remaining<=0) bossHelicopterBursts.remove(owner.getUUID());
        }
    }
    int helicopterCooldownRemaining(ServerPlayer owner) {
        var state=helicopterBursts.get(owner.getUUID()); return state==null?0:state.remaining;
    }
    int helicopterArrowCount() {
        return (int)arrows.stream().filter(arrow->!arrow.isRemoved() && arrow.entityTags().contains(HELICOPTER_PROJECTILE_TAG)
                && !arrow.entityTags().contains(BOSS_HELICOPTER_PROJECTILE_TAG)).count();
    }
    private boolean canFireHelicopter(ServerPlayer owner) {
        // (user) Technology ignores magical suppression; only the mechanism beacon jams it.
        return active(owner) && !stunned(owner) && !game.items().frost().isFrozen(owner)
                && !game.items().wands().inputTimeStopped(owner)
                && !game.items().magic().mechanismSuppressedAt(owner.level(),owner.position());
    }
    boolean beginHelicopter(ServerPlayer owner,InteractionHand hand) {
        if(!isHelicopter(owner.getItemInHand(hand)) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !canFireHelicopter(owner) || helicopterCooldownRemaining(owner)>0) return false;
        var burst=helicopterBursts.computeIfAbsent(owner.getUUID(),ignored->new HelicopterBurst(owner));
        burst.remaining=800; burst.phase=0;
        return true;
    }
    /** Only active cooldowns tick: 200 firing ticks followed by 600 recharge ticks. */
    void tickHelicopters() {
        for(var burst:List.copyOf(helicopterBursts.values())) {
            var owner=burst.owner;
            if(!active(owner)) { forget(owner); continue; }
            if(game.items().wands().timeStopped() || game.items().wands().inputTimeStopped(owner)) continue;
            if(--burst.remaining<=0) { helicopterBursts.remove(owner.getUUID()); continue; }
            if(++burst.phase<2) continue;
            burst.phase=0;
            if(burst.remaining<600 || !canFireHelicopter(owner)) continue;
            boolean main=isHelicopter(owner.getMainHandItem());
            boolean off=game.config().offhandWands && isHelicopter(owner.getOffhandItem());
            if(main) helicopterVolley(owner,.2);
            if(off) helicopterVolley(owner,.6);
            if(main && off) game.items().mana().award(owner,"lbc:true_advancements/shooter/attack_helicopter");
        }
    }
    private void helicopterVolley(ServerPlayer owner,double width) {
        helicopterVolley(owner,width,false);
    }
    private void helicopterVolley(ServerPlayer owner,double width,boolean boss) {
        // Attack Helicopter flattens pitch; Boss uses full local facing. Native gravity/drag follow.
        var forward=Vec3.directionFromRotation(boss?owner.getXRot():0,owner.getYRot());
        var left=new Vec3(Math.cos(Math.toRadians(owner.getYRot())),0,Math.sin(Math.toRadians(owner.getYRot())));
        for(int side:new int[]{-1,1}) {
            var arrow=new HelicopterArrow(owner.level());
            // Native Owner replaces the legacy malformed UUID assignment, retaining kill attribution.
            arrow.setOwner(owner); arrow.pickup=AbstractArrow.Pickup.CREATIVE_ONLY; arrow.setBaseDamage(boss?.2:.1);
            ((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$setPierceLevel((byte)10);
            arrow.setPos(owner.getEyePosition().add(forward.scale(.5)).add(left.scale(width*side)));
            arrow.setDeltaMovement(new Vec3(Math.floor(forward.x*6000)*.001,Math.floor(forward.y*6000)*.001,Math.floor(forward.z*6000)*.001));
            orient(arrow);
            arrow.entityTags().add(HELICOPTER_PROJECTILE_TAG); game.items().markCurrentItemEpoch(arrow);
            if(boss) arrow.entityTags().add(BOSS_HELICOPTER_PROJECTILE_TAG);
            owner.level().addFreshEntity(arrow); game.items().timeStop().prepareFrozen(arrow);
        }
        LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:entity.firework_rocket.blast",1,2,0);
    }
    private void helicopterGroundHit(Projectile projectile) {
        if(!(projectile.getOwner() instanceof ServerPlayer owner) || !active(owner)
                || owner.level()!=projectile.level() || !game.items().currentEpoch(projectile)) return;
        var at=projectile.position(); var level=owner.level();
        try {
            game.items().indirectKills().worldChanges(level,owner,()-> {
                for(var cell:BlockPos.betweenClosed(BlockPos.containing(at.add(-.1,-.1,-.1)),BlockPos.containing(at.add(.1,.1,.1))))
                    game.items().wands().destroyRubyCell(owner,cell);
            });
        } finally { game.items().indirectKills().finishSupportRemoval(); }
        level.sendParticles(ParticleTypes.EXPLOSION,at.x,at.y,at.z,1,0,0,0,0);
        LbcOverload.sound(owner,at,"minecraft:entity.generic.explode",1,2,0);
    }
    private static Item registerPhantomKnives() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:phantom_knives"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties() {
                    @Override public Identifier effectiveModel() { return Identifier.parse("lbc:phantomknifes"); }
                }.setId(key).durability(25).overrideDescription("phantom_knifes"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isPhantomKnives(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(PHANTOM_KNIVES) || data!=null && data.matchedBy(PHANTOM_FLAG);
    }
    int phantomCooldownRemaining(ServerPlayer owner) {
        var burst=phantomBursts.get(owner.getUUID());
        return burst==null?-20:burst.cooldown;
    }
    int phantomBurstCount() { return (int)phantomBursts.values().stream().filter(burst->burst.cooldown>=0).count(); }
    int phantomArrowCount() { return (int)arrows.stream().filter(arrow->arrow.entityTags().contains(PHANTOM_PROJECTILE_TAG)).count(); }
    /** Source shared coldown13: free activation, five waves, then recovery down to -20. */
    boolean beginPhantomBurst(ServerPlayer owner,InteractionHand hand) {
        if(hand==InteractionHand.OFF_HAND && !game.config().offhandWands || !canFire(owner)
                || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())
                || phantomCooldownRemaining(owner)>=-16) return false;
        phantomBursts.put(owner.getUUID(),new PhantomBurst(owner));
        return true;
    }
    /** Knives consume existing Stand state; acquisition and clocks abilities remain a separate migration. */
    int phantomVariant(ServerPlayer owner) {
        if(!owner.entityTags().contains("have_stand") || !LbcStand.hasClocks(owner)) return 0;
        // Пятый сохранённый матч усилен до конца даже при уже списанном profile-счётчике.
        return game.items().stand().overheaven(owner)>0?2:1;
    }
    private static Item registerKfc() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:kfc_gun"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties().setId(key).durability(25).overrideDescription("kfc_gun")
                        .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:kfcgun")),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isKfcGun(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(KFC_GUN) || data!=null && data.matchedBy(KFC_FLAG);
    }
    int kfcCooldownRemaining(ServerPlayer owner) { return kfcCooldowns.getOrDefault(owner.getUUID(),0); }
    /** Existing burst clocks are source-owned; reducing them must not restart a paid queue. */
    int sourceCounter(ServerPlayer owner,String name) {return switch(name){
        case "coldown13"->phantomCooldownRemaining(owner);case "coldown17"->kfcCooldownRemaining(owner);
        case "coldown25"->helicopterCooldownRemaining(owner);case "coldownMagnum"->cooldownRemaining(owner);
        case "coldown18","coldown18b"->{var burst=bursts.get(owner.getUUID());
            yield burst!=null&&burst.best==name.equals("coldown18b")?burst.counter:-4;}
        default->throw new IllegalArgumentException(name);
    };}
    void reduceSourceCounter(ServerPlayer owner,String name,int value) {switch(name){
        case "coldown13"->{var burst=phantomBursts.get(owner.getUUID());if(burst!=null)burst.cooldown=value;}
        case "coldown17"->{if(value<=0)kfcCooldowns.remove(owner.getUUID());else kfcCooldowns.put(owner.getUUID(),value);}
        case "coldown25"->{var burst=helicopterBursts.get(owner.getUUID());if(burst!=null)burst.remaining=value;}
        case "coldownMagnum"->setCooldown(owner,value);
        case "coldown18","coldown18b"->{var burst=bursts.get(owner.getUUID());if(burst!=null&&burst.best==name.equals("coldown18b")){
            if(value < -2)bursts.remove(owner.getUUID());else burst.counter=value;}}
        default->throw new IllegalArgumentException(name);
    }}
    int chickenCount() { return chickens.size(); }
    /** KFC's player-counter outcome extends actual Java timers as well as its legacy ledger. */
    void addPlayerCounters(ServerPlayer player) {
        helicopterBursts.computeIfAbsent(player.getUUID(),ignored->new HelicopterBurst(player)).remaining++;
        bossHelicopterBursts.computeIfAbsent(player.getUUID(),ignored->new HelicopterBurst(player)).remaining++;
        var helicopter=helicopterBursts.get(player.getUUID());
        if(helicopter.remaining>1) helicopter.phase++;
        var boss=bossHelicopterBursts.get(player.getUUID());
        if(boss.remaining>1) boss.phase++;
        var stun=stuns.get(player);
        if(stun!=null && !(LbcParasite.active(player)||game.items().magic().rejectEffect(player,MobEffects.SLOWNESS))) stun.ticks++;
        kfcCooldowns.merge(player.getUUID(),1,Integer::sum);
        slrCooldowns.merge(player.getUUID(),1,Integer::sum);
        var burst=bursts.get(player.getUUID());
        if(burst!=null) { burst.delay++; burst.counter++; }
        var phantom=phantomBursts.get(player.getUUID());
        if(phantom==null) {
            phantom=new PhantomBurst(player); phantom.cooldown=-19;
            phantomBursts.put(player.getUUID(),phantom);
        } else { phantom.cooldown++; phantom.delay++; }
    }
    private boolean useKfc(ServerPlayer owner,InteractionHand hand) {
        if(hand==InteractionHand.OFF_HAND && !game.config().offhandWands || !canFire(owner)
                || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()) || kfcCooldownRemaining(owner)>0) return false;
        var chicken=new KfcChicken(owner);
        var preset=new CompoundTag();
        preset.putString("DeathLootTable","minecraft:empty");
        preset.putInt("Age",-19980); preset.putInt("EggLayTime",1999980); preset.putBoolean("HasVisualFire",true);
        chicken.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess(),preset));
        chicken.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30); chicken.setHealth(30);
        chicken.setRemainingFireTicks(2000);
        chicken.setPos(owner.getEyePosition().add(owner.getLookAngle()));
        var direction=owner.getLookAngle();
        chicken.setDeltaMovement(new Vec3(Math.floor(direction.x*6000)*.001,
                Math.floor(direction.y*6000)*.001,Math.floor(direction.z*6000)*.001));
        chicken.entityTags().add("chick"); chicken.entityTags().add("lucky_mob");
        chicken.entityTags().add(KFC_OWNER+owner.getUUID()); game.items().markCurrentItemEpoch(chicken);
        if(!owner.level().addFreshEntity(chicken)) return false;
        chickens.put(chicken,owner.getUUID()); kfcCooldowns.put(owner.getUUID(),20);
        return true;
    }
    ServerPlayer owner(Entity entity) {
        var id=entity instanceof KfcChicken chicken && chicken.currentOwnerLife()?chickens.get(chicken):null;
        return id==null?null:game.server().getPlayerList().getPlayer(id);
    }
    /** Accepted native damage, not a separate collision attack, activates the original chicken turret. */
    public void damaged(LivingEntity target,DamageSource source) {
        if(!(target instanceof Chicken chicken) || !chickens.containsKey(chicken) || !chicken.isAlive()
                || chicken.getHealth()>20 || chicken.entityTags().contains("time_stopped")
                || game.items().wands().timeStopped()) return;
        var owner=owner(chicken);
        if(owner==null || !active(owner) || owner.level()!=chicken.level() || !game.items().currentEpoch(chicken)) return;
        chickens.remove(chicken);
        game.items().blaze().chickenTurret(owner,chicken.position().add(0,1,0));
        chicken.discard();
    }
    private void tickChickens() {
        for(var chicken:List.copyOf(chickens.keySet())) {
            var owner=owner(chicken);
            if(owner==null || !active(owner) || !chicken.isAlive() || chicken.isRemoved()
                    || chicken.level()!=owner.level() || !game.items().currentEpoch(chicken)) {
                chickens.remove(chicken); chicken.discard(); continue;
            }
            if(game.items().wands().timeStopped() || chicken.entityTags().contains("time_stopped")) continue;
            if(!chicken.isOnFire()) {
                // Remove the tracked state before spawning anything: load/unload callbacks cannot duplicate the drop.
                chickens.remove(chicken);
                var point=chicken.position();
                for(var stack:game.lbc().rollTable(owner.level(),point,owner,"lbc:kfc_yumyumyummy")) {
                    var drop=new ItemEntity(owner.level(),point.x,point.y,point.z,stack);
                    drop.setDefaultPickUpDelay(); owner.level().addFreshEntity(drop);
                }
                LbcOverload.sound(owner,point,"minecraft:entity.player.levelup",.5f,1,0);
                owner.level().sendParticles(ParticleTypes.CLOUD,point.x,point.y+.5,point.z,10,.1,.1,.1,.1);
                chicken.discard();
            }
        }
    }
    private static Item registerPine() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:razorpine"));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(
                new Item.Properties().setId(key).durability(25).overrideDescription("razorpine")
                        .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:razorpine")),
                Items.CARROT_ON_A_STICK, true) {
            @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                var controller = me.skywars2.SkyWars2.game();
                return controller != null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
    }
    private static Item registerSlr() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:slr"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties().setId(key).durability(25).overrideDescription("slr")
                        .component(DataComponents.ATTACK_ANIMATION,LbcRifleZoom.SCOPE_ATTACK_ANIMATION)
                        .component(DataComponents.INTERACT_ANIMATION,new net.minecraft.world.item.component.SwingAnimation(
                                net.minecraft.world.item.SwingAnimationType.NONE,6))
                        .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:super_rifle")),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return hand==InteractionHand.MAIN_HAND && controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isSlr(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(SLR) || data!=null && data.matchedBy(SLR_FLAG);
    }
    static int mode(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4);
    }
    int cooldownRemaining(ServerPlayer player) { return slrCooldowns.getOrDefault(player.getUUID(),0); }
    void setCooldown(ServerPlayer player,int ticks) {
        if(ticks<=0) slrCooldowns.remove(player.getUUID()); else slrCooldowns.put(player.getUUID(),ticks);
    }
    void resetCooldown(ServerPlayer player) { setCooldown(player,0); }
    private boolean useSlr(ServerPlayer player,InteractionHand hand) {
        if(hand!=InteractionHand.MAIN_HAND || !canFire(player,true)) return false;
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()) {
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int next=mode(stack)%4+1; data.putInt("mode",next);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(next) {
                case 2 -> "karich_mode_2";
                case 3 -> "karich_mode_3";
                case 4 -> "karich_mode_4";
                default -> "karich_mode_1";
            }));
            return true;
        }
        if(cooldownRemaining(player)>0 || player.getInventory().clearOrCountMatchingItems(
                ammo->ammo.is(Items.ARROW),true,0,player.inventoryMenu.getCraftSlots())==0) return false;
        boolean infinite=player.getInventory().clearOrCountMatchingItems(ammo->{
            var data=ammo.get(DataComponents.CUSTOM_DATA); return data!=null && data.matchedBy(INFINITE_ARROW);
        },true,0,player.inventoryMenu.getCraftSlots())>0;
        if(!infinite) player.getInventory().clearOrCountMatchingItems(ammo->ammo.is(Items.ARROW),false,1,player.inventoryMenu.getCraftSlots());
        slrCooldowns.put(player.getUUID(),15);
        // Source local offset ^-.2 ^-.2 ^, before recoil changes the player's look.
        var forward=player.getLookAngle();
        var left=new Vec3(Math.cos(Math.toRadians(player.getYRot())),0,Math.sin(Math.toRadians(player.getYRot())));
        var up=forward.cross(left);
        var origin=player.getEyePosition().subtract(left.scale(.2)).subtract(up.scale(.2));
        var direction=LbcGunAccuracy.rifle(game,player);
        game.items().indirectKills().trapChanges(player,()->slrRay(player,origin,direction,mode(stack)));
        LbcOverload.sound(player,origin.add(forward.scale(.5)),"minecraft:lbcsounds.karich",2,2,120,.5f);
        float yaw=player.getYRot()+(player.getRandom().nextInt(1201)-600)*.01f;
        float pitch=player.getXRot()+(player.getRandom().nextInt(1001)-1500)*.01f;
        player.forceSetRotation(yaw,false,pitch,false);
        return true;
    }
    private void slrRay(ServerPlayer owner,Vec3 origin,Vec3 direction,int mode) {
        var level=owner.level();
        var tracer=game.items().rifleZoom().tracer(owner);
        var seen=new java.util.HashSet<UUID>();
        // At most 19 narrow spatial queries, with DDA checking every crossed block before reads.
        for(int part=0;part<19;part++) {
            var from=origin.add(direction.scale(part*16));
            var to=origin.add(direction.scale(Math.min(300,(part+1)*16)));
            var wall=BlockGetter.traverseBlocks(from,to,level,(world,pos)-> {
                if(!world.hasChunkAt(pos)) return Shapes.block().clip(from,to,pos);
                var state=world.getBlockState(pos);
                if(mode!=3 && state.is(BREAKABLE_SLR)) {
                    world.destroyBlock(pos,true,owner,Block.UPDATE_LIMIT);
                    return null;
                }
                return state.isAir()?null:Shapes.block().clip(from,to,pos);
            },world->null);
            var end=wall==null?to:wall.getLocation();
            var hits=new java.util.ArrayList<net.minecraft.world.phys.EntityHitResult>();
            for(var entity:level.getEntities(owner,new AABB(from,end).inflate(1),e->
                    game.items().lifetimes().target(owner,e) && !e.entityTags().contains("strict_map_object") && !seen.contains(e.getUUID()))) {
                var box=entity.getBoundingBox();
                var point=box.contains(from)?java.util.Optional.of(from):box.clip(from,end);
                if(point.isPresent()) hits.add(new net.minecraft.world.phys.EntityHitResult(entity,point.get()));
            }
            hits.sort(java.util.Comparator.comparingDouble(hit->from.distanceToSqr(hit.getLocation())));
            for(var hit:hits) {
                var entity=hit.getEntity(); seen.add(entity.getUUID());
                slrHit(owner,(LivingEntity)entity,hit.getLocation(),mode);
                if(mode==3) { slrExplode(owner,hit.getLocation()); end=hit.getLocation(); break; }
            }
            var particle=switch(mode) { case 2->ParticleTypes.SNEEZE; case 3->ParticleTypes.CRIT; case 4->ParticleTypes.ELECTRIC_SPARK; default->ParticleTypes.ASH; };
            double length=from.distanceTo(end);
            for(double distance=0;distance<=length;distance+=.2) {
                var point=from.add(direction.scale(distance));
                tracer.send(particle,point,1,0,0);
            }
            if(mode==3 && !hits.isEmpty()) return;
            if(wall!=null) {
                if(mode==3 && level.hasChunkAt(wall.getBlockPos())) slrExplode(owner,end);
                return;
            }
        }
    }
    private void slrHit(ServerPlayer owner,LivingEntity target,Vec3 point,int mode) {
        var damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),owner);
        if(mode==1) {
            if(target.getEyePosition().distanceToSqr(point)<=.25) {
                target.hurtServer(owner.level(),damage,20);
                target.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
                game.items().mana().award(owner,"lbc:true_advancements/shooter/slr");
                LbcOverload.sound(owner,target.position(),"minecraft:entity.item.break",1,1,0);
            }
            target.hurtServer(owner.level(),damage,10);
        } else if(mode==2) {
            target.hurtServer(owner.level(),damage,8);
            if(target.addEffect(ItemEffectLevels.create(MobEffects.POISON,200,0),owner)) game.items().indirectKills().poisoned(target,owner);
            target.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,200,0),owner);
            target.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,200,0),owner);
            target.addEffect(ItemEffectLevels.create(MobEffects.HUNGER,200,1),owner);
        } else if(mode==3) target.hurtServer(owner.level(),damage,5);
        else {
            if(!stun(owner,target,80))return;
            for(var effect:List.of(MobEffects.SLOWNESS,MobEffects.WEAKNESS,MobEffects.MINING_FATIGUE)){
                var before=target.getEffect(effect);var previous=before==null?null:new MobEffectInstance(before);
                if(target.addEffect(ItemEffectLevels.create(effect,80,127,false,false),owner)){
                    var installed=target.getEffect(effect);
                    if(installed!=null&&installed.getAmplifier()==127)
                        stuns.get(target).effects.put(effect,new StunEffect(installed,previous,target.tickCount));
                }
            }
        }
    }
    /** Shared full input lock. Spell families own their own additional status payloads. */
    public boolean stun(ServerPlayer owner,LivingEntity target,int ticks){
        statusCallbacks++;try {if(statusRestoring)return false;
            if(ticks<=0||!active(owner)||target.level()!=owner.level()
                    ||!game.items().wands().projectileTarget(owner,target)
                    ||game.items().wands().inputTimeStopped(owner)
                    ||game.items().timeStop().frozen(target)
                    ||target instanceof ServerPlayer player&&(LbcParasite.active(player)||game.items().magic().rejectEffect(player,MobEffects.SLOWNESS)))return false;
            clearStun(target);
            var movement=target.getAttribute(Attributes.MOVEMENT_SPEED);
            if(movement!=null)movement.addOrUpdateTransientModifier(new AttributeModifier(STUN_MOVEMENT,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            stuns.put(target,new Stun(owner,target,game.items().epoch(),ticks));
            game.items().indirectKills().displaced(target,owner);
            prepareStunnedInput(target);
            return true;
        }finally{statusCallbacks--;}
    }
    public boolean stunned(LivingEntity target) { return !game.items().polymorph().masked(target)&&stunTicks(target)>0; }
    public int stunTicks(LivingEntity target) {
        var stun=stuns.get(target);
        return stun==null?0:stun.ticks;
    }
    public void clearStun(LivingEntity target) {
        statusCallbacks++;try {
            if(statusRestoring)return;
            var stun=stuns.remove(target);if(stun==null)return;
            // Clean our high-amplifier SLR effects before removing the zero multiplier.
            // Generic stuns never claim ownership of pre-existing potion effects.
            for(var entry:stun.effects.entrySet()){
                var owned=entry.getValue();var current=target.getEffect(entry.getKey());
                if(current!=owned.installed()||current.getAmplifier()!=127)continue;
                target.removeEffect(entry.getKey());
                var previous=owned.previous();
                if(previous!=null){
                    int remaining=previous.isInfiniteDuration()?-1:previous.getDuration()-Math.max(0,target.tickCount-owned.installedAt());
                    if(remaining==-1||remaining>0)target.addEffect(new MobEffectInstance(entry.getKey(),remaining,previous.getAmplifier(),previous.isAmbient(),previous.isVisible(),previous.showIcon()));
                }
            }
            var movement=target.getAttribute(Attributes.MOVEMENT_SPEED);if(movement!=null)movement.removeModifier(STUN_MOVEMENT);
        }finally{statusCallbacks--;}
    }
    /** Remove voluntary input only: deltaMovement belongs to native physics and external forces. */
    public void prepareStunnedInput(LivingEntity target) {
        statusCallbacks++;try {
            if(LbcParasite.active(target)){clearStun(target);return;}
            var stun=stuns.get(target); if(statusRestoring||stun==null||stun.retainedTarget||game.items().polymorph().masked(target)) return;
            target.xxa=0; target.yya=0; target.zza=0;
            target.setJumping(false); target.setSprinting(false); target.setShiftKeyDown(false);
            target.setSwimming(false);
            if(target.isUsingItem()) target.stopUsingItem();
            target.setYRot(stun.yaw); target.setXRot(stun.pitch); target.setYHeadRot(stun.yaw);
            if(target instanceof ServerPlayer player) player.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);
        }finally{statusCallbacks--;}
    }
    /** Correct only pose; the shared teleport policy keeps server and client momentum independently. */
    public void correctStun(ServerPlayer player) {
        statusCallbacks++;try {
            if(LbcParasite.active(player)){clearStun(player);return;}
            var state=stuns.get(player);if(statusRestoring||state!=null&&state.retainedTarget||!stunned(player)) return;
            prepareStunnedInput(player);
            player.connection.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
        }finally{statusCallbacks--;}
    }
    private void tickStuns() {
        statusCallbacks++;try {
            if(statusRestoring)return;
            for(var target:List.copyOf(stuns.keySet())) {
                if(game.items().polymorph().masked(target))continue;
                var stun=stuns.get(target);
                if(stun.retainedTarget)continue;
                if(!target.isAlive() || target.isRemoved() || target.level()!=game.maps().level()
                        || !stun.retainedOwner&&!active(stun.owner) || !game.items().currentEpoch(stun.epoch)
                        || target instanceof ServerPlayer player && (!active(player)
                            || (LbcParasite.active(player)||game.items().magic().rejectEffect(player,MobEffects.SLOWNESS)))) {
                    clearStun(target); continue;
                }
                if(game.items().timeStop().stopped((ServerLevel)target.level())||game.items().timeStop().frozen(target))continue;
                if(--stun.ticks<=0){clearStun(target);continue;}
                prepareStunnedInput(target);
                if(target instanceof ServerPlayer player) correctStun(player);
            }
        }finally{statusCallbacks--;}
    }
    void checkpointStatusBoundary(){if(statusCallbacks!=0)throw new IllegalStateException("Finish Shooter stun callbacks before checkpointing");}
    void prepareStatusFrozen(){checkpointStatusBoundary();statusRestoring=true;stuns.clear();restoredStunAges.clear();}
    void publishStatusFrozen(){
        checkpointStatusBoundary();restoredStunAges.forEach((target,ages)->{
            var stun=stuns.get(target);if(stun!=null)stun.effects.replaceAll((type,owned)->new StunEffect(owned.installed,owned.previous,target.tickCount-ages.get(type)));
        });restoredStunAges.clear();statusRestoring=false;
    }
    void cancelStatusFrozen(){checkpointStatusBoundary();restoredStunAges.clear();statusRestoring=false;}
    StatusCheckpoint checkpointStatuses(MatchSpellCheckpoint.Links links){checkpointStatusBoundary();return new StatusCheckpoint(links);}
    final class StatusCheckpoint {
        private record EffectImage(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,MobEffectInstance installed,MobEffectInstance previous,int age,boolean owned) { }
        private record StunImage(Stun original,MatchDamageSources.Ref owner,int ticks,List<EffectImage> effects,AttributeModifier movement) { }
        private final Map<MatchDamageSources.Ref,StunImage> images=new java.util.LinkedHashMap<>();
        StatusCheckpoint(MatchSpellCheckpoint.Links links){
            stuns.forEach((target,stun)->{
                var effects=new java.util.ArrayList<EffectImage>();stun.effects.forEach((type,owned)->effects.add(new EffectImage(type,EffectLayers.copy(owned.installed),
                        owned.previous==null?null:EffectLayers.copy(owned.previous),target.tickCount-owned.installedAt,target.getEffect(type)==owned.installed)));
                var movement=target.getAttribute(Attributes.MOVEMENT_SPEED);
                images.put(links.key(target),new StunImage(stun,links.key(stun.owner),stun.ticks,List.copyOf(effects),movement==null?null:movement.getModifier(STUN_MOVEMENT)));
            });
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointStatusBoundary();var next=new java.util.IdentityHashMap<LivingEntity,Stun>();var plans=new java.util.ArrayList<Runnable>();
            for(var entry:images.entrySet()){
                var target=(LivingEntity)links.get(entry.getKey());var image=entry.getValue();var owner=(ServerPlayer)links.get(image.owner);
                boolean offlineTarget=MatchOfflinePlayers.retained(target),offlineOwner=MatchOfflinePlayers.retained(owner);
                var movement=target.getAttribute(Attributes.MOVEMENT_SPEED);
                if(!offlineTarget&&!java.util.Objects.equals(image.movement,movement==null?null:movement.getModifier(STUN_MOVEMENT)))
                    throw new IllegalStateException("Restore native stun movement lease before private status");
                var effects=new java.util.IdentityHashMap<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,StunEffect>();var ages=new java.util.IdentityHashMap<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,Integer>();
                for(var effect:image.effects){
                    var installed=effect.owned&&!offlineTarget?target.getEffect(effect.type):EffectLayers.copy(effect.installed);
                    if(effect.owned&&!offlineTarget&&!sameEffect(installed,effect.installed))throw new IllegalStateException("Restore native stun-owned effect before private baseline");
                    effects.put(effect.type,new StunEffect(installed,effect.previous==null?null:EffectLayers.copy(effect.previous),target.tickCount-effect.age));ages.put(effect.type,effect.age);
                }
                plans.add(()->{var stun=image.original;stun.owner=owner;stun.retainedOwner=offlineOwner;stun.retainedTarget=offlineTarget;stun.ticks=image.ticks;
                    stun.effects.clear();stun.effects.putAll(effects);restoredStunAges.put(target,ages);});next.put(target,image.original);
            }
            return ()->{statusRestoring=true;stuns=next;restoredStunAges.clear();plans.forEach(Runnable::run);};
        }
    }
    private static boolean sameEffect(MobEffectInstance left,MobEffectInstance right){
        while(left!=null&&right!=null){if(!left.equals(right))return false;left=((EffectLayers.Access)left).skywars2$layerHidden();right=((EffectLayers.Access)right).skywars2$layerHidden();}
        return left==null&&right==null;
    }

    private void slrExplode(ServerPlayer owner,Vec3 point) {
        for(int i=0;i<2;i++) {
            game.items().exasperato().protectedBlast(owner,owner.level(),point,4,null);
        }
    }
    public static boolean isPine(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(PINE) || data != null && data.matchedBy(PINE_FLAG);
    }
    private boolean active(ServerPlayer player) {
        return game.canUseGameplayItems(player) && player.isAlive() && !player.isRemoved()
                && player.level() == game.maps().level() && game.stage() != GameController.Stage.ENDING;
    }
    private boolean canFire(ServerPlayer player) {
        return canFire(player,false);
    }
    private boolean canFire(ServerPlayer player,boolean mechanism) {
        return active(player) && !stunned(player) && !game.items().frost().isFrozen(player)
                && !game.items().wands().inputTimeStopped(player)
                && !(mechanism?game.items().magic().mechanismSuppressed(player):game.items().magic().suppressed(player));
    }
    private int hands(ServerPlayer player) {
        return (isPine(player.getMainHandItem()) ? 1 : 0)
                + (game.config().offhandWands && isPine(player.getOffhandItem()) ? 1 : 0);
    }
    public boolean use(ServerPlayer player, InteractionHand hand) {
        if(isBossHelicopter(player.getItemInHand(hand))) return beginBossHelicopter(player,hand);
        if(isHelicopter(player.getItemInHand(hand))) return beginHelicopter(player,hand);
        if(isWoodificator(player.getItemInHand(hand))) return useWoodificator(player,hand);
        if(isPhantomKnives(player.getItemInHand(hand))) return beginPhantomBurst(player,hand);
        if(isKfcGun(player.getItemInHand(hand))) return useKfc(player,hand);
        if(isSlr(player.getItemInHand(hand))) return useSlr(player,hand);
        if (!isPine(player.getItemInHand(hand)) || hand == InteractionHand.OFF_HAND && !game.config().offhandWands
                || bursts.containsKey(player.getUUID()) || !canFire(player)) return false;
        // Как source /clear: сначала еловые боеприпасы, затем обычные; ровно одна единица на всю очередь.
        boolean best = consume(player, BEST_AMMO);
        if (!best && !consume(player, AMMO)) return false;
        bursts.put(player.getUUID(), new Burst(player, best));
        if (best) {
            var advancement = game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/shooter/razorpine"));
            if (advancement != null) for (var criterion : advancement.value().criteria().keySet())
                player.getAdvancements().award(advancement, criterion);
        }
        return true;
    }
    private static boolean consume(ServerPlayer player, TagKey<Item> tag) {
        return player.getInventory().clearOrCountMatchingItems(stack -> stack.is(tag), false, 1,
                player.inventoryMenu.getCraftSlots()) == 1;
    }
    public void tick() {
        tickStuns();
        tickChickens();
        tickHelicopters();
        tickBossHelicopters();
        tickPhantomBursts();
        kfcCooldowns.replaceAll((id,ticks)->ticks-1);
        kfcCooldowns.values().removeIf(ticks->ticks<=0);
        slrCooldowns.replaceAll((id,ticks)-> {
            if(ticks==1) {
                var player=game.server().getPlayerList().getPlayer(id);
                if(player!=null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        SoundEvents.UI_BUTTON_CLICK,SoundSource.MASTER,player.getX(),player.getY(),player.getZ(),1,2,player.getRandom().nextLong()));
            }
            return ticks-1;
        });
        slrCooldowns.values().removeIf(ticks->ticks<=0);
        if (bursts.isEmpty()) return;
        boolean stopped = game.items().wands().timeStopped();
        for (var burst : List.copyOf(bursts.values())) {
            var owner = burst.owner;
            if (!active(owner)) { forget(owner); continue; }
            if (stopped || owner.entityTags().contains("time_stopped")) continue;
            if (--burst.delay > 0) continue;
            burst.delay = 2;
            if (canFire(owner)) {
                // (user) Два хвоемёта удваивают очередь без второго расхода боеприпаса.
                for (int hand = 0, held = hands(owner); hand < held; hand++) volley(owner, burst.best);
            }
            burst.counter-=4; if (burst.counter < -2) bursts.remove(owner.getUUID());
        }
    }
    void tickPhantomBursts() {
        for(var burst:List.copyOf(phantomBursts.values())) {
            var owner=burst.owner;
            if(!active(owner)) { forget(owner); continue; }
            if(game.items().wands().inputTimeStopped(owner)) continue;
            if(--burst.delay>0) continue;
            burst.delay=2;
            if(burst.cooldown>0 && burst.cooldown<=25 && burst.cooldown%5==0 && canFire(owner)) {
                int wave=(25-burst.cooldown)/5;
                if(isPhantomKnives(owner.getMainHandItem())) phantomWave(owner,wave);
                if(game.config().offhandWands && isPhantomKnives(owner.getOffhandItem())) phantomWave(owner,4-wave);
            }
            burst.cooldown-=5;
            if(burst.cooldown<=-20) phantomBursts.remove(owner.getUUID());
        }
    }
    private void phantomWave(ServerPlayer owner,int wave) {
        int variant=phantomVariant(owner);
        if(variant<0) return;
        float yaw=owner.getYRot()+(variant==0?0:-20+wave*10);
        var forward=Vec3.directionFromRotation(owner.getXRot(),yaw);
        var left=new Vec3(Math.cos(Math.toRadians(yaw)),0,Math.sin(Math.toRadians(yaw)));
        var origin=owner.getEyePosition().add(left.scale(PHANTOM_ORIGINS[wave])).add(forward.scale(.5));
        if(variant==0) {
            var up=forward.cross(left);
            phantomArrow(owner,origin,forward.add(left.scale(PHANTOM_OFFSETS[wave])).add(up.scale(wave==2?.01:0)),2);
        } else {
            for(int offset:variant==1?STAND_FAN:OVERHEAVEN_FAN)
                phantomArrow(owner,origin,Vec3.directionFromRotation(owner.getXRot(),yaw+offset),variant==1?10:20);
        }
        owner.level().playSound(null,owner.getX(),owner.getY(),owner.getZ(),SoundEvents.TRIDENT_RIPTIDE_1,SoundSource.MASTER,1,2);
    }
    private void phantomArrow(ServerPlayer owner,Vec3 origin,Vec3 direction,double damage) {
        var arrow=new Arrow(EntityTypes.ARROW,owner.level());
        arrow.setOwner(owner);
        arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(damage);
        ((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)arrow).skywars2$setPierceLevel((byte)127);
        arrow.setPos(origin);
        arrow.setDeltaMovement(new Vec3(Math.floor(direction.x*1000)*.0015,
                Math.floor(direction.y*1000)*.0015,Math.floor(direction.z*1000)*.0015));
        orient(arrow);
        arrow.entityTags().add(PHANTOM_PROJECTILE_TAG);
        game.items().markCurrentItemEpoch(arrow);
        owner.level().addFreshEntity(arrow);
        game.items().timeStop().prepareFrozen(arrow);
    }
    private void volley(ServerPlayer owner, boolean best) {
        var direction = owner.getLookAngle();
        var origin = owner.getEyePosition().subtract(direction.scale(.2));
        int speed = best ? 5000 : 2000;
        for (int shot = 0; shot < (best ? 2 : 3); shot++) {
            var arrow = new Arrow(EntityTypes.ARROW, owner.level());
            arrow.setOwner(owner);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setBaseDamage(best ? 1.5 : .5);
            arrow.setSoundEvent(SoundEvents.AZALEA_LEAVES_BREAK);
            arrow.setPos(origin);
            arrow.setDeltaMovement(new Vec3(component(owner, direction.x, speed),
                    component(owner, direction.y, speed), component(owner, direction.z, speed)));
            // Arrow использует углы скорости, а не углы взгляда игрока; задаём их до spawn-пакета.
            orient(arrow);
            arrow.entityTags().add(PROJECTILE_TAG);
            owner.level().addFreshEntity(arrow);
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.MASTER, 1, 2);
    }
    static void orient(Projectile projectile) {
        var motion=projectile.getDeltaMovement();
        projectile.setYRot((float)(net.minecraft.util.Mth.atan2(motion.x,motion.z)*180/Math.PI));
        projectile.setXRot((float)(net.minecraft.util.Mth.atan2(motion.y,motion.horizontalDistance())*180/Math.PI));
        projectile.setOldRot();
    }
    private static double component(ServerPlayer owner, double direction, int speed) {
        return (Math.floor(direction * speed) + owner.getRandom().nextInt(601) - 300) * .001;
    }
    boolean loaded(Entity entity) {
        if(entity.entityTags().contains(HELICOPTER_PROJECTILE_TAG)) {
            if(!(entity instanceof HelicopterArrow arrow) || arrow.retired || !game.items().currentEpoch(entity)
                    || !(arrow.getOwner() instanceof ServerPlayer owner) || !active(owner) || owner.level()!=entity.level()) entity.discard();
            else arrows.add(arrow);
            return true;
        }
        if(entity instanceof KfcChicken chicken) {
            var owner=game.server().getPlayerList().getPlayer(chicken.ownerId);
            if(!chicken.currentOwnerLife() || !game.items().currentEpoch(chicken) || owner==null
                    || !active(owner) || owner.level()!=chicken.level()) chicken.discard();
            else chickens.put(chicken,chicken.ownerId);
            return true;
        }
        if(entity instanceof Chicken chicken) for(String tag:entity.entityTags()) if(tag.startsWith(KFC_OWNER)) {
            // Plain saved chickens have no owner-generation guard and cannot be adopted safely.
            chicken.discard();
            return true;
        }
        if (!(entity instanceof Arrow arrow) || !trackedArrow(entity)) return false;
        if(entity.entityTags().contains(PHANTOM_PROJECTILE_TAG) && !game.items().currentEpoch(entity)) {
            arrow.discard(); return true;
        }
        if (entity.level() == game.maps().level() && (game.playing() || game.items().currentEpoch(entity)
                || arrow.getOwner() instanceof ServerPlayer owner && active(owner))) arrows.add(arrow);
        else arrow.discard();
        return true;
    }
    void unloaded(Entity entity) {
        if(entity instanceof HelicopterArrow arrow) arrow.retired=true;
        arrows.remove(entity);
        chickens.remove(entity);
        if(entity instanceof LivingEntity living) clearStun(living);
    }
    public boolean canHit(Projectile projectile, Entity target) {
        return !trackedArrow(projectile)
                || projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && game.items().wands().physicalProjectileTarget(projectile, target);
    }
    /** Source prein удаляет хвою в земле; перехватываем событие вместо чтения NBT каждого снаряда в тике. */
    public void hit(Projectile projectile, HitResult hit) {
        if (hit.getType() == HitResult.Type.BLOCK && trackedArrow(projectile)) {
            if(projectile.entityTags().contains(HELICOPTER_PROJECTILE_TAG)) helicopterGroundHit(projectile);
            projectile.discard();
        }
    }
    private static boolean trackedArrow(Entity entity) {
        return entity.entityTags().contains(PROJECTILE_TAG) || entity.entityTags().contains(PHANTOM_PROJECTILE_TAG)
                || entity.entityTags().contains(HELICOPTER_PROJECTILE_TAG);
    }
    public int burstCount() { return bursts.size(); }
    public int arrowCount() { return arrows.size(); }
    public void forget(ServerPlayer player) {
        helicopterBursts.remove(player.getUUID());
        bossHelicopterBursts.remove(player.getUUID());
        for(var arrow:List.copyOf(arrows)) if(arrow.entityTags().contains(HELICOPTER_PROJECTILE_TAG) && arrow.getOwner()==player) {
            arrows.remove(arrow); arrow.discard();
        }
        for(var job:List.copyOf(woodificatorWork)) if(job.owner==player) job.cancel();
        bursts.remove(player.getUUID()); slrCooldowns.remove(player.getUUID());
        phantomBursts.remove(player.getUUID());
        chickenGenerations.remove(player.getUUID());
        kfcCooldowns.remove(player.getUUID());
        for(var chicken:List.copyOf(chickens.keySet())) if(player.getUUID().equals(chickens.get(chicken))) {
            chickens.remove(chicken); chicken.discard();
        }
        for(var target:List.copyOf(stuns.keySet())) if(target==player || stuns.get(target).owner==player) clearStun(target);
    }
    void stop() {
        helicopterBursts.clear();
        bossHelicopterBursts.clear();
        for(var job:List.copyOf(woodificatorWork)) job.cancel();
        bursts.clear(); slrCooldowns.clear();
        phantomBursts.clear();
        kfcCooldowns.clear();
        chickenGenerations.clear();
        var chicks=List.copyOf(chickens.keySet()); chickens.clear(); for(var chicken:chicks) chicken.discard();
        for(var target:List.copyOf(stuns.keySet())) clearStun(target);
        var removed = List.copyOf(arrows); arrows.clear();
        for (var arrow : removed) arrow.discard();
    }
}
