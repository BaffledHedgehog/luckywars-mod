package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Нативная катана Electro и её отложенные, приписанные владельцу разряды. */
public final class LbcElectro {
    public static final String SHOCK_BOLT = "skywars2.electro.shock";
    public static final String SELF_SHOCK_BOLT = "skywars2.electro.self_shock";
    public static final String EGG_BOLT = "skywars2.electro.egg";
    // Старый tag охватывал также сущности вне Projectile. Не включаем ради этого все legacy-теги в JAR.
    private static final Set<EntityType<?>> REFLECTABLE = Set.of(EntityTypes.FALLING_BLOCK,EntityTypes.TNT,
            EntityTypes.MARKER,EntityTypes.ARMOR_STAND,EntityTypes.AREA_EFFECT_CLOUD,
            EntityTypes.ITEM_DISPLAY,EntityTypes.BLOCK_DISPLAY,EntityTypes.INTERACTION);
    public static final Item CYBERKATANA = registerKatana("cyberkatana");
    public static final Item SHIT_KATANA = registerKatana("shit_katana");
    public static final Item SHIELD = registerShield();
    public static final Item BLOCKER = registerBlocker();

    private static final int CHARGE_HITS = 10;
    private static final int SHOCK_RADIUS = 30;
    private static final int SHOCK_INTERVAL = 40;
    private static final CompoundTag KATANA_FLAG = flag("cyberkatana");
    private static final CompoundTag YELLOW_KATANA_FLAG = flag("shit_katana");
    private static final CompoundTag SHIELD_FLAG = shieldFlags();
    private static final CompoundTag SHIELD_USING_FLAG = flag("energyshield");
    private static final CompoundTag BLOCKER_FLAG = flag("electroblocker");

    private static Item registerBlocker() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:electroblocker"));
        var props=new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:electroblocker"); }
        }.setId(key).overrideDescription("electroblocker");
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.REPEATING_COMMAND_BLOCK,true));
    }

    private static Item registerKatana(String name) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:"+name));
        var props = new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:"+name); }
        }.setId(key).durability(25).overrideDescription(name);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(props, Items.CARROT_ON_A_STICK, true) {
                    @Override public InteractionResult use(net.minecraft.world.level.Level level,
                            Player player, InteractionHand hand) {
                        var game = SkyWars2.game();
                        return game != null && player instanceof ServerPlayer serverPlayer
                                && game.items().wands().use(serverPlayer, hand)
                                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
                    }
                });
    }

    private static Item registerShield() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:electro_shield"));
        // ItemStack ещё недоступен до bind компонентов реестра. Используем нативный delayed builder щита.
        // finalizeInitializer заново задаёт ITEM_MODEL из effectiveModel: простой component() здесь перезаписывается.
        var props = new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("minecraft:shield"); }
        }.setId(key).durability(336).overrideDescription("energy_shield")
                .component(DataComponents.BANNER_PATTERNS,net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY)
                .repairable(net.minecraft.tags.ItemTags.WOODEN_TOOL_MATERIALS)
                .equippableUnswappable(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS,context->new net.minecraft.world.item.component.BlocksAttacks(
                        .25f,1,java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(
                                90,java.util.Optional.empty(),0,1)),
                        new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3,1,1),
                        java.util.Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)))
                .component(DataComponents.BREAK_SOUND,net.minecraft.sounds.SoundEvents.SHIELD_BREAK);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(props, Items.SHIELD, true));
    }

    static void register() { }


    private static CompoundTag flag(String key) { var data = new CompoundTag(); data.putInt(key, 1); return data; }
    private static CompoundTag shieldFlags() {
        return flag("energy_shield");
    }
    private static boolean matches(ItemStack stack, Item item, CompoundTag flags) {
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(item) || custom != null && custom.matchedBy(flags);
    }
    public static boolean isKatana(ItemStack stack) { return matches(stack, CYBERKATANA, KATANA_FLAG); }
    public static boolean isShitKatana(ItemStack stack) { return matches(stack, SHIT_KATANA, YELLOW_KATANA_FLAG); }
    public static boolean isAnyKatana(ItemStack stack) { return isKatana(stack) || isShitKatana(stack); }
    public static boolean isBlocker(ItemStack stack) {
        return stack.is(BLOCKER)||LbcTmtInventory.matches(stack,BLOCKER_FLAG)
                ||LbcTmtItems.matches(stack)&&LbcTmtInventory.string(stack,LbcTmtAbilities.NATIVE,"skywars2:electroblocker");
    }
    public static boolean isShield(ItemStack stack) {
        return stack.is(SHIELD)||LbcTmtInventory.matches(stack,SHIELD_FLAG)
                ||ItemData.intValue(stack,"energyshield",0)==1
                ||LbcTmtInventory.matches(stack,SHIELD_USING_FLAG)
                ||LbcTmtItems.matches(stack)&&LbcTmtInventory.string(stack,LbcTmtAbilities.NATIVE,"skywars2:electro_shield");
    }
    public static boolean managedLightning(LightningBolt bolt) { return bolt.entityTags().contains(SHOCK_BOLT); }
    public static boolean eggLightning(Entity entity) {
        return entity instanceof LightningBolt && entity.entityTags().contains(EGG_BOLT);
    }
    /** Native bolts ignore friendship; observers and service entities remain protected. */
    public boolean lightningTarget(LightningBolt bolt,Entity target) {
        return game.items().wands().validTarget(target);
    }

    private static final class Charge {
        int hits;
        int charges;
    }
    private static final class Shock {
        ServerPlayer owner;
        int pulses;
        int untilNext = SHOCK_INTERVAL;
        boolean retainedTarget,retainedOwner;
        Shock(ServerPlayer owner, int pulses) { this.owner = owner; this.pulses = pulses; }
    }

    private final GameController game;
    private int checkpointDepth;
    private boolean checkpointFrozen;
    private final Map<UUID, Charge> charges = new HashMap<>();
    private final Map<UUID, Charge> yellowCharges = new HashMap<>();
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<net.minecraft.world.level.block.Block> CONDUCTIVE = TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:electroprovod"));
    private static final net.minecraft.core.particles.DustColorTransitionOptions YELLOW_PARTICLE =
            new net.minecraft.core.particles.DustColorTransitionOptions(0xffff00,0xcccc00,1);
    private static final net.minecraft.core.particles.DustParticleOptions DIRT_PARTICLE =
            new net.minecraft.core.particles.DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1,.208f,.118f,.027f)&0xffffff,1);
    private static final net.minecraft.core.particles.DustParticleOptions SPIN_PARTICLE =
            new net.minecraft.core.particles.DustParticleOptions(0xffff00,1);
    static final class YellowTrace {
        ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        final boolean spinning;
        Vec3 position;
        float yaw,pitch;
        int age;
        YellowTrace(ServerPlayer owner,Vec3 position,String epoch,boolean spinning) {
            this.owner=owner; this.level=owner.level(); this.position=position; this.epoch=epoch; this.spinning=spinning;
            yaw=owner.getYRot(); pitch=owner.getXRot();
        }
    }
    private final Set<YellowTrace> yellowTraces = new java.util.LinkedHashSet<>();
    private final ArrayList<YellowTrace> yellowBatch = new ArrayList<>();
    // Только затронутые цели; выгрузка/смерть/очистка и конец матча удаляют запись явно.
    private IdentityHashMap<LivingEntity, Shock> shocks = new IdentityHashMap<>();
    private final ArrayList<LivingEntity> shockBatch = new ArrayList<>();
    private final Set<LightningBolt> liveBolts = new HashSet<>();
    private final ArrayList<LightningBolt> boltBatch = new ArrayList<>();
    // У TNT, falling blocks и старых marker нет общего native owner; удерживаем автора до удаления сущности.
    final IdentityHashMap<Entity, ServerPlayer> reflectedOwners = new IdentityHashMap<>();
    private final Set<UUID> blockerCarriers = new HashSet<>();
    private final Set<Entity> accelerated = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    /** Общий dirty inventory snapshot LbcMagic: аксессуар не требует повторного сканирования каждый тик. */
    void inventory(ServerPlayer player,boolean blocker) {
        if(blocker) blockerCarriers.add(player.getUUID()); else blockerCarriers.remove(player.getUUID());
    }
    public boolean blockerActive(ServerPlayer player) { return blockerCarriers.contains(player.getUUID()); }

    public LbcElectro(GameController game) { this.game = game; }

    private boolean active(ServerPlayer player) {
        return game.canUseGameplayItems(player) && player.isAlive() && !player.isRemoved()
                && player.level() == game.maps().level() && game.stage() != GameController.Stage.ENDING
                && !player.entityTags().contains("time_stopped") && !game.items().wands().timeStopped()
                && !game.items().frost().isFrozen(player);
    }

    /** Только успешный ближний удар катаной: её молнии/эффекты не запускают новый заряд и ауру. */
    public void damaged(ServerPlayer owner, net.minecraft.world.damagesource.DamageSource source) {
        if(checkpointFrozen)return;checkpointDepth++;try{
        if(isShitKatana(owner.getMainHandItem())) {
            if(LbcDamage.isMeleeAttack(source,owner) && active(owner)) yellowHit(owner);
            return;
        }
        if (!LbcDamage.isMeleeAttack(source, owner) || !active(owner) || !isKatana(owner.getMainHandItem())) return;
        var state = charges.computeIfAbsent(owner.getUUID(), ignored -> new Charge());
        if (++state.hits >= CHARGE_HITS) { state.hits = 0; state.charges++; }
        var level = owner.level();
        var at = owner.position();
        for (var target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(6), entity -> game.items().wands().target(owner, entity)
                        && entity.distanceToSqr(owner) <= 36)) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1,
                    target.getZ(), 30, .5, 1, .5, .5);
            debuff(owner, target, MobEffects.WITHER);
            debuff(owner, target, MobEffects.SLOWNESS);
            debuff(owner, target, MobEffects.GLOWING);
            if (target instanceof ServerPlayer player) debuff(owner, player, MobEffects.BLINDNESS);
        }
        LbcOverload.sound(owner, owner.position(), "minecraft:entity.firework_rocket.twinkle", 1, 2, 0);

        }finally{checkpointDepth--;}
    }

    private void debuff(ServerPlayer owner, LivingEntity target, net.minecraft.core.Holder<MobEffect> effect) {
        if (target instanceof ServerPlayer player && game.items().magic().rejectEffect(player, effect)) {
            return;
        }
        target.addEffect(ItemEffectLevels.create(effect, 20, 10, false, true, true), owner);
    }

    private void yellowHit(ServerPlayer owner) {
        var state=yellowCharges.computeIfAbsent(owner.getUUID(),ignored->new Charge());
        boolean skill=game.lbc().skillsEnabled() && game.players().get(owner.getUUID()).lbcSkill==9;
        for(int pass=0;pass<(skill ? 2 : 1);pass++) {
            if(++state.hits>=7) { state.hits=0; state.charges++; }
            int count=pass==0 ? 7 : 4;
            for(int i=0;i<count;i++) yellowTraces.add(new YellowTrace(owner,
                    owner.getEyePosition().add(0,-i*.01,0),game.items().epoch(),false));
            LbcOverload.sound(owner,"minecraft:entity.firework_rocket.twinkle",1,2,0);
        }
    }
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform) {
        yellowTraces.removeIf(trace->{
            if(trace.level!=level) return false;
            Vec3 next=transform.apply(trace.position);
            if(next==null) return true;
            trace.position=next; return false;
        });
    }

    public int yellowCount() { return yellowTraces.size(); }
    java.util.List<YellowTrace> yellowSnapshot() { return java.util.List.copyOf(yellowTraces); }
    public int yellowCharges(ServerPlayer player) {
        var state=yellowCharges.get(player.getUUID()); return state==null ? 0 : state.charges;
    }
    public int yellowCharging(ServerPlayer player) {
        var state=yellowCharges.get(player.getUUID()); return state==null ? 0 : state.hits;
    }
    private void reflectYellow(ServerPlayer player,boolean blocker) {
        double radiusSquared=blocker ? 196 : 49;
        for(var trace:yellowTraces) if(trace.level==player.level() && trace.position.distanceToSqr(player.position())<=radiusSquared
                && (!blocker || trace.owner!=player && !player.isAlliedTo(trace.owner))) {
            trace.owner=player; trace.yaw+=180; trace.pitch=-trace.pitch;
            trace.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,trace.position.x,trace.position.y,trace.position.z,
                    blocker ? 10 : 5,0,0,0,.3);
        }
    }
    /** Виртуальные лучи не требуют marker/scoreboard; damage callbacks могут вызвать stop посреди обхода. */
    private void tickYellow() {
        if(yellowTraces.isEmpty()) return;
        yellowBatch.addAll(yellowTraces);
        try {
            for(var trace:yellowBatch) {
                if(!yellowTraces.contains(trace)) continue;
                var owner=trace.owner;
                if(!game.items().currentEpoch(trace.epoch) || trace.level!=game.maps().level()
                        || owner.level()!=trace.level || !owner.isAlive() || owner.isRemoved()
                        || !game.canUseGameplayItems(owner) || game.stage()==GameController.Stage.ENDING) {
                    yellowTraces.remove(trace); continue;
                }
                if(game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")) continue;
                if(!trace.level.hasChunkAt(net.minecraft.core.BlockPos.containing(trace.position))
                        || !(trace.spinning ? yellowSpin(trace) : yellowStep(trace))
                        || ++trace.age>=(trace.spinning ? 200 : 50)) yellowTraces.remove(trace);
            }
        } finally { yellowBatch.clear(); }
    }
    private boolean yellowTarget(ServerPlayer owner,Entity entity) {
        return entity.getType().builtInRegistryHolder().is(MOBS) && game.items().wands().projectileTarget(owner,entity);
    }
    private net.minecraft.world.damagesource.DamageSource yellowDamage(YellowTrace trace,
            ResourceKey<net.minecraft.world.damagesource.DamageType> type) {
        return new net.minecraft.world.damagesource.DamageSource(trace.level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),trace.owner);
    }
    private boolean yellowStep(YellowTrace trace) {
        Vec3 from=trace.position,direction=Vec3.directionFromRotation(trace.pitch,trace.yaw),end=from.add(direction);
        var wall=LbcTerra.wallHit(trace.level,from,end);
        var shield=game.items().wands().shieldHit(trace.owner,from,wall==null ? end : wall.getLocation());
        if(wall!=null) end=wall.getLocation();
        if(shield!=null) end=shield.getLocation();
        double distance=from.distanceTo(end);
        for(int i=0;i<=2;i++) {
            Vec3 point=from.add(direction.scale(i*.5));
            if(i*.5>distance+1e-7) break;
            trace.level.sendParticles(YELLOW_PARTICLE,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
            trace.level.sendParticles(DIRT_PARTICLE,point.x,point.y,point.z,1,.1,.1,.1,1);
            LbcOverload.sound(trace.owner,point,"minecraft:entity.firework_rocket.twinkle",.03f,2,0);
            LbcOverload.sound(trace.owner,point,"minecraft:block.rooted_dirt.break",.03f,2,0);
        }
        LbcDamage.hurtSegment(trace.level,yellowDamage(trace,LbcDamage.YELLOW_BOLT),from,end,Vec3.ZERO,.5,.25f,2,
                entity->yellowTarget(trace.owner,entity),false);
        trace.position=end;
        if(!yellowTraces.contains(trace) || !game.items().currentEpoch(trace.epoch)
                || game.stage()==GameController.Stage.ENDING) return false;
        if(shield!=null) {
            LbcOverload.sound(trace.owner,end,"minecraft:item.shield.block",1,1,0); return false;
        }
        if(wall!=null) {
            if(trace.level.hasChunkAt(wall.getBlockPos())) {
                var particles=end.subtract(direction.scale(.5));
                trace.level.sendParticles(ParticleTypes.END_ROD,particles.x,particles.y,particles.z,25,.1,.1,.1,.1);
                if(trace.level.getBlockState(wall.getBlockPos()).is(CONDUCTIVE)) yellowConduction(trace);
            }
            return false;
        }
        // Исходная рефракция применяется после трёх samples, один раз за тик полёта.
        trace.yaw=net.minecraft.util.Mth.floor(trace.yaw*100)*.01f+(trace.level.getRandom().nextInt(1601)-800)*.01f;
        trace.pitch=net.minecraft.util.Mth.floor(trace.pitch*100)*.01f+(trace.level.getRandom().nextInt(1601)-800)*.01f;
        return true;
    }
    private void yellowConduction(YellowTrace trace) {
        // (user) Проводник поражает всех врагов рядом; положение самих целей относительно проводников не важно.
        var at=trace.position;
        var damage=yellowDamage(trace,LbcDamage.YELLOW_CONDUCTION);
        for(var target:trace.level.getEntities(trace.owner,new AABB(at,at).inflate(15),entity->
                (entity instanceof LbcFpvDrone.Drone || yellowTarget(trace.owner,entity)) && entity.distanceToSqr(at)<=225)) {
            if(!yellowTraces.contains(trace) || !game.items().currentEpoch(trace.epoch)
                    || game.stage()==GameController.Stage.ENDING) return;
            if(game.items().fpvDrone().tryShortCircuit(target))continue;
            target.hurtServer(trace.level,damage,8);
            if(!game.items().currentEpoch(trace.epoch) || game.stage()==GameController.Stage.ENDING) return;
            var center=target.blockPosition();
            game.items().indirectKills().fillTrap(trace.owner,center.offset(-1,-1,-1),center.offset(1,2,1),
                    net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
        }
    }
    private boolean yellowSpin(YellowTrace trace) {
        // (user) Это фиксированная вращающаяся полоса: стены и обычный магический щит не обрывают её.
        trace.yaw+=4;
        var direction=Vec3.directionFromRotation(0,trace.yaw);
        var from=trace.position.add(0,1.1,0).subtract(direction.scale(8));
        Vec3 end=from;
        trace.level.sendParticles(SPIN_PARTICLE,trace.position.x,trace.position.y+1,trace.position.z,1,.2,1,.2,0);
        for(int step=0;step<=64;step++) {
            var point=from.add(direction.scale(step*.25));
            if(!trace.level.hasChunkAt(net.minecraft.core.BlockPos.containing(point))) break;
            end=point;
            trace.level.sendParticles(SPIN_PARTICLE,point.x,point.y+.5,point.z,3,.01,1,.01,0);
        }
        // Старый dx=0,dy=8,dz=0 означает полосу 1×9×1 вокруг каждого sample, а не 65 entity queries.
        LbcDamage.hurtSegment(trace.level,yellowDamage(trace,LbcDamage.YELLOW_SPIN),from.add(0,1.5,0),end.add(0,1.5,0),
                new Vec3(.5,4.5,.5),.25,.3f,Integer.MAX_VALUE,entity->yellowTarget(trace.owner,entity),false);
        return true;
    }

    void adoptRawFoodStatuses(ServerPlayer player){
        if((LbcParasite.active(player)||game.items().magic().rejectEffect(player,MobEffects.WITHER)))return;
        int pulses=game.items().mana().hudValue(player,"shock");if(pulses<=0)return;
        var existing=shocks.computeIfAbsent(player,p->new Shock(player,0));existing.pulses=pulses;
    }
    public void clearEffects(LivingEntity entity) { shocks.remove(entity); game.items().mana().score(entity,"shock").set(0); }
    /** Общий статус shock используется также медитацией жезла телепорта; выбор цели остаётся у способности. */
    public boolean shock(LivingEntity target,ServerPlayer owner,int pulses) {
        if(pulses<=0 || !target.isAlive() || target.isRemoved()
                || target instanceof ServerPlayer player && (LbcParasite.active(player)||game.items().magic().rejectEffect(player,MobEffects.WITHER))) return false;
        var shock=shocks.computeIfAbsent(target,ignored->new Shock(owner,0));
        shock.owner=owner;shock.retainedTarget=MatchOfflinePlayers.retained(target);shock.retainedOwner=MatchOfflinePlayers.retained(owner); shock.pulses+=pulses; game.items().mana().score(target,"shock").set(shock.pulses); return true;
    }

    /** ПКМ расходует накопленный заряд; обычная атака остаётся нативной. */
    public boolean use(ServerPlayer owner, InteractionHand hand) {
        if(isShitKatana(owner.getItemInHand(hand))) {
            if(!active(owner)) return false;
            var state=yellowCharges.get(owner.getUUID());
            if(state==null || state.charges<=0) return false;
            state.charges--;
            yellowTraces.add(new YellowTrace(owner,owner.getEyePosition(),game.items().epoch(),true));
            return true;
        }
        if (!isKatana(owner.getItemInHand(hand)) || !active(owner)) return false;
        var state = charges.get(owner.getUUID());
        if (state == null || state.charges <= 0) return false;
        state.charges--;
        var level = owner.level();
        var at = owner.position();
        for (var target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(SHOCK_RADIUS), entity -> game.items().wands().target(owner, entity)
                        && entity.distanceToSqr(owner) <= (double) SHOCK_RADIUS * SHOCK_RADIUS)) {
            if(target instanceof ServerPlayer player && (LbcParasite.active(player)||game.items().magic().rejectEffect(player,MobEffects.WITHER))) continue;
            shock(target,owner,2);
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 20, target.getZ(),
                    400, .5, 20, .5, .01);
        }
        LbcOverload.sound(owner, owner.position(), "minecraft:entity.lightning_bolt.thunder", 2, 1, 0);
        LbcOverload.sound(owner, owner.position(), "minecraft:entity.lightning_bolt.thunder", 2, 2, 0);
        LbcOverload.sound(owner, owner.position(), "minecraft:entity.firework_rocket.twinkle", 2, 1, 0);
        return true;
    }

    /** Ежематричный тик; общая остановка и локальная time_stopped сохраняют таймер импульса. */
    public void tick() {
        if(checkpointFrozen)return;checkpointDepth++;try{
        tickYellow();
        boolean globallyStopped = game.items().wands().timeStopped();
        tickShields();
        reflectedOwners.keySet().removeIf(Entity::isRemoved);
        accelerated.removeIf(Entity::isRemoved);
        boltBatch.addAll(liveBolts);
        for (var bolt : boltBatch) if (bolt.isRemoved()) liveBolts.remove(bolt);
        boltBatch.clear();
        if (globallyStopped || game.stage() == GameController.Stage.ENDING) return;
        shockBatch.addAll(shocks.keySet());
        try {
            for (var target : shockBatch) {
                if(game.items().polymorph().masked(target))continue;
                Shock shock = shocks.get(target);
                if (shock == null||shock.retainedTarget||shock.retainedOwner) continue;
                ServerPlayer owner = shock.owner;
                if (!target.isAlive() || target.isRemoved() || target.level() != game.maps().level()
                        || target instanceof ServerPlayer player && (!game.canUseGameplayItems(player)
                            || (LbcParasite.active(player)||game.items().magic().rejectEffect(player,MobEffects.WITHER)))) { clearEffects(target); continue; }
                if (target.entityTags().contains("time_stopped") || owner.entityTags().contains("time_stopped")) continue;
                if (--shock.untilNext > 0) continue;
                shock.untilNext = SHOCK_INTERVAL;
                emitLightning(owner, target);
                if (--shock.pulses <= 0) shocks.remove(target);
                game.items().mana().score(target,"shock").set(Math.max(0,shock.pulses));
            }
        } finally { shockBatch.clear(); }

        }finally{checkpointDepth--;}
    }

    private static final net.minecraft.world.entity.projectile.ProjectileDeflection REVERSE = (projectile,entity,random,power) -> {
        projectile.setDeltaMovement(projectile.getDeltaMovement().reverse());
        projectile.setYRot(projectile.getYRot()+180); projectile.setXRot(-projectile.getXRot());
        projectile.needsSync=true;
    };

    /** (user) Каждое использование щита отражает всё в сфере 7, даже уже отражённое и своё. */
    private void tickShields() {
        if(game.items().wands().timeStopped() || game.stage()==GameController.Stage.ENDING) return;
        for(var player:game.server().getPlayerList().getPlayers()) {
            if(!game.canUseGameplayItems(player) || !player.isAlive() || player.level()!=game.maps().level()
                    || game.items().frost().isFrozen(player) || player.entityTags().contains("time_stopped")) continue;
            boolean shield=player.isUsingItem() && isShield(player.getUseItem());
            boolean blocker=blockerActive(player) && !game.items().magic().suppressed(player);
            if(!shield && !blocker) continue;
            var center=player.position();
            int radius=blocker ? 14 : 7;
            for(var entity:player.level().getEntities((Entity)null,new AABB(center,center).inflate(radius),
                    entity->!entity.isRemoved() && entity.distanceToSqr(player)<=radius*radius
                        && (entity instanceof net.minecraft.world.entity.projectile.Projectile
                            || REFLECTABLE.contains(entity.getType())))) {
                // Щит повторно отражает всё в 7; аксессуар работает с принадлежностью, а не направлением полёта.
                if(shield && entity.distanceToSqr(player)<=49) { reflect(player,entity); continue; }
                if(!blocker) continue;
                Entity owner=entity instanceof net.minecraft.world.entity.projectile.Projectile projectile ? projectile.getOwner()
                        : entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud ? cloud.getOwner() : reflectedOwner(entity);
                if(owner==player) {
                    if(entity instanceof net.minecraft.world.entity.projectile.Projectile
                            && !entity.entityTags().contains("skywars2.electro.reflected")
                            && !entity.entityTags().contains("edited3") && accelerated.add(entity)) {
                        entity.setDeltaMovement(entity.getDeltaMovement().scale(2)); entity.setNoGravity(true); entity.needsSync=true;
                        player.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,entity.getX(),entity.getY(),entity.getZ(),10,0,0,0,.3);
                    }
                } else if(owner==null || !player.isAlliedTo(owner)) reflect(player,entity);
            }
            if(shield) {
                game.items().wands().reflectElectro(player);
                reflectYellow(player,false);
                player.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,true,false,
                        player.getX(),player.getY()+1,player.getZ(),1,.5,1,.5,.3);
            }
            if(blocker) {
                game.items().wands().reflectElectroblocker(player);
                reflectYellow(player,true);
                // Одна цель/тик; электрическая защита и авторство проходят через общий hurt pipeline.
                for(var target:player.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(2.5),
                        entity->entity.distanceToSqr(player)>=.0001 && entity.distanceToSqr(player)<=6.25
                                && game.items().wands().target(player,entity))) {
                    var damage=new net.minecraft.world.damagesource.DamageSource(
                            player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRIC),player);
                    if(target.hurtServer(player.level(),damage,.1f)) {
                        // ELECTRIC сам не отбрасывает: исключение действует только для ближней ауры.
                        var before=target.getDeltaMovement();
                        target.dealDefaultKnockback(damage,.1f,false);
                        // Масштабируем весь нативный импульс, включая вертикаль, сохраняя внешнюю скорость.
                        if(target instanceof ServerPlayer)
                            target.setDeltaMovement(before.add(target.getDeltaMovement().subtract(before).scale(.1)));
                    }
                    break;
                }
                if(game.stage()==GameController.Stage.ENDING) return;
            }
        }
    }

    private void reflect(ServerPlayer player,Entity entity) {
        if(checkpointFrozen)return;checkpointDepth++;try{
        if(entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            projectile.deflect(REVERSE,player,net.minecraft.world.entity.EntityReference.of((Entity)player),false,1);
            assignOwner(projectile,player);
        } else {
            entity.setDeltaMovement(entity.getDeltaMovement().reverse()); entity.needsSync=true;
            entity.setYRot(entity.getYRot()+180); entity.setXRot(-entity.getXRot());
            assignOwner(entity,player);
        }
        player.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,entity.getX(),entity.getY(),entity.getZ(),5,0,0,0,.3);

        }finally{checkpointDepth--;}
    }

    public boolean canHit(net.minecraft.world.entity.projectile.Projectile projectile,Entity target) {
        if(LbcBlaze.isKfcSpirit(projectile)) return game.items().wands().validTarget(target);
        return !projectile.entityTags().contains("skywars2.electro.reflected")
                || projectile.getOwner() instanceof ServerPlayer owner && game.items().wands().projectileTarget(owner,target);
    }

    public ServerPlayer reflectedOwner(Entity entity) { return reflectedOwners.get(entity); }

    /** Общая смена автора для отражений и Curse, без разворота скорости или повторного удара. */
    void assignOwner(Entity entity,ServerPlayer owner) {
        game.items().spellSources().reflected(entity);
        if(entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) projectile.setOwner(owner);
        else {
            if(entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud) cloud.setOwner(owner);
            reflectedOwners.put(entity,owner);
        }
        entity.entityTags().add("skywars2.electro.reflected");
        game.items().markCurrentItemEpoch(entity);
        game.items().summons().ownerChanged(entity,owner);
        game.items().trinketArrows().ownerChanged(entity,owner);
        game.items().madokaBow().ownerChanged(entity,owner);
        game.items().shulkerBow().ownerChanged(entity,owner);
    }

    private void emitLightning(ServerPlayer owner, LivingEntity target) {
        var bolt=lightning(owner,target.position());
        // Keep the meditation marker for compatibility; native bolts can hit their caster.
        if(bolt!=null && target==owner) bolt.entityTags().add(SELF_SHOCK_BOLT);
        if(bolt!=null) owner.level().sendParticles(ParticleTypes.END_ROD,target.getX(),target.getY()+1,target.getZ(),
                100,.5,1,.5,1);
    }

    /** Молния из яйца использует общие электрический урон, владельца, защиту целей и очистку при stop. */
    public void manageLightning(ServerPlayer owner,LightningBolt bolt) {
        if(owner.level()!=bolt.level() || bolt.isRemoved()) return;
        bolt.setCause(owner); bolt.entityTags().add(SHOCK_BOLT); bolt.entityTags().add(EGG_BOLT); liveBolts.add(bolt);
    }
    net.minecraft.world.entity.LightningBolt lightning(ServerPlayer owner,Vec3 position) {
        if(checkpointFrozen)return null;checkpointDepth++;try{
        ServerLevel level=game.items().spellSources().level(owner);
        var epoch=game.items().epoch();
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return null;
        bolt.setCause(owner);
        bolt.entityTags().add(SHOCK_BOLT);
        bolt.setPos(position);game.items().markCurrentItemEpoch(bolt);
        if (level.addFreshEntity(bolt)) {
            if(!game.items().currentEpoch(epoch) || game.stage()==GameController.Stage.ENDING) {
                bolt.discard(); return null;
            }
            liveBolts.add(bolt);
            return bolt;
        }
        return null;

        }finally{checkpointDepth--;}
    }

    /** Source score consumers alter the existing charge object, never its sibling counter. */
    void sourceCharge(ServerPlayer player,boolean yellow,boolean progress,int value) {
        var state=(yellow?yellowCharges:charges).computeIfAbsent(player.getUUID(),ignored->new Charge());
        if(progress)state.hits=value;else state.charges=value;
    }
    void sourceShock(ServerPlayer player,int value) {
        if(value>0&&LbcParasite.active(player)){clearEffects(player);return;}
        game.items().mana().score(player,"shock").set(value);
        var state=shocks.get(player);
        if(value<=0)shocks.remove(player);else if(state!=null)state.pulses=value;
    }
    public int charges(ServerPlayer player) {
        Charge state = charges.get(player.getUUID()); return state == null ? 0 : state.charges;
    }
    /** Source swordcharged1/5 and swordcharging1/5 are distinct player counters. */
    void addPlayerCounters(ServerPlayer player,int amount) {
        if(amount<=0) return;
        var cyber=charges.computeIfAbsent(player.getUUID(),ignored->new Charge());
        var yellow=yellowCharges.computeIfAbsent(player.getUUID(),ignored->new Charge());
        var shock=shocks.get(player);
        if(shock!=null && !game.items().magic().rejectCustomDebuff(player)) shock.untilNext+=amount;
        cyber.charges+=amount; cyber.hits+=amount;
        yellow.charges+=amount; yellow.hits+=amount;
    }
    public int charging(ServerPlayer player) {
        Charge state = charges.get(player.getUUID()); return state == null ? 0 : state.hits;
    }
    public int hudCharges(ServerPlayer player) {
        return isKatana(player.getMainHandItem()) || isKatana(player.getOffhandItem()) ? charges(player) : -1;
    }
    public int yellowHudCharges(ServerPlayer player) {
        return isShitKatana(player.getMainHandItem()) || isShitKatana(player.getOffhandItem()) ? yellowCharges(player) : -1;
    }
    public int shocks(LivingEntity target) { Shock shock = shocks.get(target); return shock == null ? 0 : shock.pulses; }
    public int shockCount() { return shocks.size(); }
    public int chargedCount() { return (int) charges.values().stream().filter(state -> state.charges > 0).count(); }

    public void reset(ServerPlayer player) {
        charges.remove(player.getUUID());
        yellowCharges.remove(player.getUUID());
        shocks.remove(player);
    }
    public void forget(ServerPlayer player) { reset(player); blockerCarriers.remove(player.getUUID()); }
    public void stop() {checkpointFrozen=false;
        shocks.clear(); charges.clear();
        yellowCharges.clear(); yellowTraces.clear();
        // Сначала отсоединяем состояние: ENTITY_UNLOAD может синхронно вызвать игровые callbacks.
        var cleanup=java.util.List.copyOf(liveBolts);
        liveBolts.clear(); reflectedOwners.clear();
        blockerCarriers.clear(); accelerated.clear();
        for (var bolt : cleanup) if (!bolt.isRemoved()) bolt.discard();
        // shockBatch принадлежит активному tick и очищается его finally, даже при вложенном stop.
    }

    /** Rollback drops only short accepted traces/bolts; charge and shock ownership remain separate. */
    java.util.List<LightningBolt> transientSnapshot(){return java.util.List.copyOf(liveBolts);}
    void cancelTransient(){yellowTraces.clear();var old=java.util.List.copyOf(liveBolts);liveBolts.clear();for(var bolt:old)if(!bolt.isRemoved())bolt.discard();}

    void checkpointBoundary(){if(checkpointDepth!=0||!shockBatch.isEmpty()||!yellowBatch.isEmpty()||!boltBatch.isEmpty())throw new IllegalStateException("Finish native Electro callbacks before checkpointing");}
    void prepareFrozen(){checkpointBoundary();checkpointFrozen=true;cancelTransient();}
    void publishFrozen(){checkpointBoundary();checkpointFrozen=false;}
    void cancelFrozen(){checkpointBoundary();checkpointFrozen=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record Counter(Charge original,int hits,int charges){}
        private record Status(Shock original,MatchDamageSources.Ref owner,int pulses,int untilNext){}
        private record Reflected(MatchDamageSources.Ref actor,MatchDamageSources.Ref owner){}
        private final Map<UUID,Charge> cyber=new HashMap<>(charges),yellow=new HashMap<>(yellowCharges);
        private final java.util.List<Counter> counters=new ArrayList<>();
        private final MatchSpellCheckpoint.IdentityImage<LivingEntity,Shock> targets;
        private final java.util.List<Status> statuses=new ArrayList<>();
        private final java.util.List<Reflected> reflections=new ArrayList<>();
        private final java.util.List<MatchDamageSources.Ref> sped=new ArrayList<>();
        private final Set<UUID> blockers=Set.copyOf(blockerCarriers);
        Checkpoint(MatchSpellCheckpoint.Links links){
            var omitted=MatchTransientSpells.captureOmitted(game);
            var seen=java.util.Collections.newSetFromMap(new IdentityHashMap<Charge,Boolean>());
            for(var value:charges.values())if(seen.add(value))counters.add(new Counter(value,value.hits,value.charges));
            for(var value:yellowCharges.values())if(seen.add(value))counters.add(new Counter(value,value.hits,value.charges));
            for(var target:shocks.keySet())if(links.key(target).historical()!=null)throw new IllegalStateException("Capture live shock targets");
            targets=new MatchSpellCheckpoint.IdentityImage<>(shocks,links);
            var shocksSeen=java.util.Collections.newSetFromMap(new IdentityHashMap<Shock,Boolean>());
            for(var value:shocks.values())if(shocksSeen.add(value))statuses.add(new Status(value,links.key(value.owner),value.pulses,value.untilNext));
            reflectedOwners.forEach((actor,owner)->{if(!actor.isRemoved()&&!omitted.contains(actor.getUUID()))reflections.add(new Reflected(links.key(actor),links.key(owner)));});
            for(var actor:accelerated)if(!actor.isRemoved()&&!omitted.contains(actor.getUUID()))sped.add(links.key(actor));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var nextTargets=targets.restore(links);var owners=new IdentityHashMap<Shock,ServerPlayer>();var retainedTargets=java.util.Collections.newSetFromMap(new IdentityHashMap<Shock,Boolean>());var retainedOwners=java.util.Collections.newSetFromMap(new IdentityHashMap<Shock,Boolean>());
            for(var saved:statuses){var owner=(ServerPlayer)links.get(saved.owner);owners.put(saved.original,owner);if(MatchOfflinePlayers.retained(owner))retainedOwners.add(saved.original);}
            nextTargets.forEach((target,status)->{if(MatchOfflinePlayers.retained(target))retainedTargets.add(status);});
            var nextReflections=new IdentityHashMap<Entity,ServerPlayer>();for(var saved:reflections)nextReflections.put(links.get(saved.actor),(ServerPlayer)links.get(saved.owner));
            var nextSped=new ArrayList<Entity>();for(var saved:sped)nextSped.add(links.get(saved));
            return ()->{
                for(var saved:counters){saved.original.hits=saved.hits;saved.original.charges=saved.charges;}
                charges.clear();charges.putAll(cyber);yellowCharges.clear();yellowCharges.putAll(yellow);
                for(var saved:statuses){saved.original.owner=owners.get(saved.original);saved.original.pulses=saved.pulses;saved.original.untilNext=saved.untilNext;saved.original.retainedTarget=retainedTargets.contains(saved.original);saved.original.retainedOwner=retainedOwners.contains(saved.original);}
                shocks=nextTargets;reflectedOwners.clear();reflectedOwners.putAll(nextReflections);accelerated.clear();accelerated.addAll(nextSped);
                blockerCarriers.clear();blockerCarriers.addAll(blockers);
            };
        }
    }
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var shock=source instanceof LivingEntity l?shocks.get(l):null;var reflected=reflectedOwners.get(source);
        boolean sped=accelerated.contains(source),bolt=liveBolts.contains(source);
        if(shock==null&&reflected==null&&!sped&&!bolt)return null;
        var caster=shock==null?null:shock.owner;int pulses=shock==null?0:shock.pulses,next=shock==null?0:shock.untilNext;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,java.util.Map<Entity,Entity> family){
                if(caster!=null){var s=new Shock(caster,pulses);s.untilNext=next;shocks.put((LivingEntity)clone,s);}
                if(reflected!=null)reflectedOwners.put(clone,reflected);if(sped)accelerated.add(clone);if(bolt)liveBolts.add((LightningBolt)clone);
            }
            public void rollback(Entity clone){shocks.remove(clone);reflectedOwners.remove(clone);accelerated.remove(clone);liveBolts.remove(clone);}
        };
    }


    void planetoid(ServerPlayer holder) {
        for(var trace:yellowTraces)if(game.items().currentEpoch(trace.epoch)
                &&LbcPlanetoidSkill.near(game,holder,trace.level,trace.position)){
            var aim=LbcPlanetoidSkill.direction(holder,trace.position,82);
            trace.yaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z));
            trace.pitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));
        }
    }

}
