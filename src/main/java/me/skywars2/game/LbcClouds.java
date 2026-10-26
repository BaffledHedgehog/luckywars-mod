package me.skywars2.game;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Учтённые химические облака BrBad и их кислотный helper; ванильный бросок и жизнь облака остаются в Minecraft. */
public final class LbcClouds {
    static final EntityType<AcidProjectile> ACID_PROJECTILE = registerAcidProjectile();
    private static final AttachmentType<Integer> ACID_AGE = AttachmentRegistry.create(
            Identifier.parse("skywars2:acid_age"), builder -> builder.persistent(Codec.INT).initializer(() -> 1));

    static void register() { } // Регистрируем тип и attachment до чтения сущностей мира.

    private static EntityType<AcidProjectile> registerAcidProjectile() {
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("skywars2:acid_projectile"));
        var type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                EntityType.Builder.<AcidProjectile>of(AcidProjectile::new, MobCategory.MISC)
                        .sized(.1f, .1f).clientTrackingRange(4).updateInterval(3).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }

    /** Нативные velocity, gravity, drag и swept collision; клиенту видны только прежние частицы. */
    public static final class AcidProjectile extends ThrowableProjectile implements PolymerEntity {
        private Vec3 shieldFrom;
        private Vec3 sweptFrom;
        AcidProjectile(EntityType<? extends AcidProjectile> type, Level level) { super(type, level); }
        @Override protected float getAirDrag() {
            return entityTags().contains(FLYING_WAND_ACID)?1:super.getAirDrag();
        }
        @Override public void tick() {
            // The awakened acid seed flies straight; its children use the shared reactive acid physics.
            boolean constantMotion=entityTags().contains(GREAT_ACID)||entityTags().contains(FLYING_WAND_ACID)||awakenedVariant(this)>0;Vec3 velocity=getDeltaMovement();
            if(awakenedVariant(this)==2)sweptFrom=position();
            if (entityTags().contains(ACID_STUCK)) {
                // Прилипший заряд использует только базовый тик сущности, без повторного sweep/блочных callbacks.
                super.baseTick();
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            shieldFrom = position();
            try {
                // ThrowableProjectile applies water inertia before moving; source nogravity acid moves the full segment.
                if(entityTags().contains(FLYING_WAND_ACID)&&isInWater())setDeltaMovement(velocity.scale(1.0/.8F));
                super.tick();
                if(constantMotion&&!isRemoved()&&!entityTags().contains(ACID_STUCK)
                        && !entityTags().remove(LbcGreed.REFLECTED))setDeltaMovement(velocity);
                if (!isRemoved()) stopAtShield(position());
            } finally { shieldFrom = null; }
        }
        /** Check the actual native gravity/drag segment, already clipped at its first collision. */
        private boolean stopAtShield(Vec3 end) {
            var game = SkyWars2.game();
            if(entityTags().contains(GREAT_ACID)||entityTags().contains(GREAT_CHILD)||entityTags().contains(FLYING_WAND_ACID)||awakenedVariant(this)>0)return false;
            if (sourceAcidManaged(this) && !entityTags().contains("shield_blockable2"))return false;
            if (shieldFrom == null || game == null || !(getOwner() instanceof ServerPlayer owner)
                    || !(level() instanceof ServerLevel level)
                    || !(game.playing() || game.items().currentEpoch(this))) return false;
            var shield = game.items().wands().shieldHit(owner, shieldFrom, end);
            if (shield == null || !level.hasChunkAt(BlockPos.containing(shield.getLocation()))
                    || !level.getBlockState(BlockPos.containing(shield.getLocation())).is(AIRS)) return false;
            game.items().clouds().sound(level, shield.getLocation(), SoundEvents.SHIELD_BLOCK.value(), 1, 1, Double.POSITIVE_INFINITY, 0);
            discard();
            return true;
        }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override protected boolean canHitEntity(Entity entity) {
            // Greed reflects even acid; ordinary actors retain source passage.
            return LbcGreed.active(entity);
        }
        @Override protected void onHit(HitResult hit) {
            if (stopAtShield(hit.getLocation())) return;
            super.onHit(hit);
            var game = SkyWars2.game();
            if (game != null && level() instanceof ServerLevel
                    && (game.playing() || game.items().currentEpoch(this))) {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    if(entityTags().contains(GREAT_ACID)){
                        game.items().clouds().burstGreatAcid(this,hit.getLocation());return;
                    }
                    entityTags().add(ACID_STUCK);
                    game.items().clouds().stick(this, hit.getLocation());
                    if(awakenedVariant(this)==0)game.items().clouds().acidImpact(this);
                    return;
                }
                game.items().clouds().acidImpact(this);
            }
            discard();
        }
    }
    private record Replacement(TagKey<Block> from, Block to) { }
    private static final List<Replacement> THERMITE = List.of(
            new Replacement(blockTag("minecraft:to_nylium"), Blocks.CRIMSON_NYLIUM),
            new Replacement(blockTag("minecraft:logs"), Blocks.CRIMSON_HYPHAE),
            new Replacement(blockTag("minecraft:leaves"), Blocks.NETHER_WART_BLOCK),
            new Replacement(blockTag("minecraft:to_shroomlight"), Blocks.SHROOMLIGHT),
            new Replacement(blockTag("minecraft:to_soulsand"), Blocks.SOUL_SAND),
            new Replacement(blockTag("minecraft:to_magma"), Blocks.MAGMA_BLOCK),
            new Replacement(blockTag("minecraft:to_netherrack"), Blocks.NETHERRACK),
            new Replacement(blockTag("minecraft:fluids"), Blocks.OBSIDIAN),
            new Replacement(blockTag("minecraft:airs_to_fire"), Blocks.FIRE),
            new Replacement(blockTag("minecraft:to_lava"), Blocks.LAVA));
    private static final TagKey<Block> SOLID = blockTag("minecraft:solid");
    private static final TagKey<Block> AIRS = blockTag("minecraft:airs");
    private static final TagKey<Block> FIRE_LAVA = blockTag("minecraft:firelava");
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final TagKey<EntityType<?>> RAYCASTERS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:raycasters"));
    private static final DustColorTransitionOptions ORDINARY_ACID_DUST = new DustColorTransitionOptions(0x09e27e,0x067564,2);
    private static final BlockRotProcessor SULFUR_EROSION = new BlockRotProcessor(0.005f);
    private static final String ACID = "skywars2.acid";
    private static final String WAND_ACID = "skywars2.wand.acid";
    private static final String ORDINARY_WAND_ACID = "skywars2.wand.acid.regular";
    private static final String FLYING_WAND_ACID = "skywars2.wand.acid.flying";
    private static final String GREAT_ACID="skywars2.acid.great";
    private static final String GREAT_CHILD="skywars2.acid.great_child";
    private static final String CAUST_ACID="skywars2.acid.caust",BURST_ACID="skywars2.acid.burst";
    private static final String SOURCE_ACID="skywars2.source_acid",ACID_500="skywars2.acid_500",ACID_OWNER="skywars2.acid_owner.";
    private final Map<java.util.UUID,ServerPlayer> acidAuthors=new java.util.HashMap<>();
    private static final String ACID_STUCK = "skywars2.acid_stuck";
    private static final class CloudState {
        final AreaEffectCloud cloud;
        final int kind;
        int lifetime;
        CloudState(AreaEffectCloud cloud, int kind) {
            this.cloud = cloud; this.kind = kind;
            lifetime = (kind == 234 ? 100 : 0) + cloud.tickCount;
        }
    }
    private static final class AcidState {
        final AcidProjectile projectile;
        int lifetime;
        AcidState(AcidProjectile projectile) {
            this.projectile = projectile;
            lifetime = projectile.getAttachedOrCreate(ACID_AGE); // universal_razbros начинает с одного; reload сохраняет возраст.
        }
    }
    private record ChangedBlock(BlockPos pos, BlockState state) { }
    private final GameController game;
    private final StructureTemplate sulfurTemplate;
    private final Map<AreaEffectCloud, CloudState> clouds = new LinkedHashMap<>();
    private final Map<AcidProjectile, AcidState> acids = new LinkedHashMap<>();
    private final Set<ThrownLingeringPotion> potions = new LinkedHashSet<>();
    private final ArrayList<CloudState> cloudBatch = new ArrayList<>();
    private final ArrayList<AcidState> acidBatch = new ArrayList<>();
    private final ArrayList<ChangedBlock> blocks = new ArrayList<>();

    LbcClouds(GameController game) {
        this.game = game;
        sulfurTemplate = game.maps().level().getStructureTemplateManager().get(Identifier.parse("lbc:air11x11")).orElseThrow();
    }

    private static TagKey<Block> blockTag(String id) { return TagKey.create(Registries.BLOCK, Identifier.parse(id)); }

    /** ENTITY_LOAD видит уже настроенное native облако; здесь не заменяем сущность и не обходим мир. */
    boolean loaded(Entity entity) {
        if (entity instanceof ThrownLingeringPotion potion && LbcItems.isChemicalPotion(potion.getItem())) {
            if (!mayRun(entity, potion.getOwner())) entity.discard();
            else potions.add(potion);
            return true;
        }
        if (entity instanceof AreaEffectCloud cloud) {
            int kind = LbcItems.chemicalAmplifier(cloud.get(DataComponents.POTION_CONTENTS));
            if (kind == 0) return false;
            if (!mayRun(entity, cloud.getOwner())) entity.discard();
            else clouds.put(cloud, new CloudState(cloud, kind));
            return true;
        }
        if (entity instanceof AcidProjectile projectile) {
            if (sourceAcidManaged(projectile) ? !sourceAcidActive(projectile) : !mayRun(entity, projectile.getOwner())) entity.discard();
            else {
                if (projectile.entityTags().contains(ACID_STUCK)) stick(projectile, projectile.position());
                acids.put(projectile, new AcidState(projectile));
            }
            return true;
        }
        return false;
    }

    private boolean mayRun(Entity entity, Entity ownerEntity) {
        if (entity.level() != game.maps().level()) return false;
        ServerPlayer owner = ownerEntity instanceof ServerPlayer player ? player : null;
        if (owner != null && game.isObserver(owner)) return false;
        if (game.playing()) return owner == null || game.canUseGameplayItems(owner);
        if (game.items().currentEpoch(entity)) return true;
        return owner != null && (game.canUseGameplayItems(owner)
                || game.isDeveloper(owner) && game.stage() == GameController.Stage.COUNTDOWN)
                && game.items().debugItemUseWindowOrCountdown();
    }

    void unloaded(Entity entity) { clouds.remove(entity); acids.remove(entity); potions.remove(entity); }
    public int cloudCount() { return clouds.size(); }
    public int acidCount() { return acids.size(); }
    public int potionCount() { return potions.size(); }

    void tick() {
        // Снимок нужен: эффекты/взрывы и ENTITY_UNLOAD могут синхронно изменить учтённые коллекции.
        cloudBatch.addAll(clouds.values());
        for (CloudState state : cloudBatch) {
            if (state.cloud.isRemoved()) continue;
            state.lifetime++;
            switch (state.kind) {
                case 234 -> thermite(state);
                case 235 -> sulfur(state.cloud);
                case 236 -> phosphine(state.cloud);
                case 237 -> fulminate(state.cloud);
                default -> { }
            }
        }
        cloudBatch.clear();
        acidBatch.addAll(acids.values());
        for (AcidState state : acidBatch) if (!state.projectile.isRemoved()) tickAcid(state);
        acidBatch.clear();
    }

    private boolean target(LivingEntity entity) {
        return entity.isAlive() && entity.getType().builtInRegistryHolder().is(MOBS) && !entity.entityTags().contains("spectator")
                && (!(entity instanceof ServerPlayer player) || game.allowDamage(player));
    }

    private List<LivingEntity> targets(ServerLevel level, Vec3 origin, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(radius),
                entity -> target(entity) && entity.position().distanceToSqr(origin) <= radius * radius);
    }

    private void thermite(CloudState state) {
        var cloud = state.cloud;
        var level = (ServerLevel) cloud.level();
        int outer = Mth.floor(state.lifetime * 0.0125), inner = Mth.floor(state.lifetime * 0.0078), core = Mth.floor(state.lifetime * 0.003);
        var center = cloud.blockPosition();
        // Один обход/чтение блока вместо 12 fill. Порядок правил и их каскад для каждой позиции сохранены.
        for (var pos : BlockPos.betweenClosed(center.offset(-outer, -outer, -outer), center.offset(outer, outer, outer))) {
            if (!level.hasChunkAt(pos)) continue;
            var original = level.getBlockState(pos);
            var result = original;
            for (var replacement : THERMITE) if (result.is(replacement.from)) result = replacement.to.defaultBlockState();
            int distance = Math.max(Math.max(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getY() - center.getY())), Math.abs(pos.getZ() - center.getZ()));
            if (distance <= inner && result.is(SOLID)) result = Blocks.MAGMA_BLOCK.defaultBlockState();
            if (distance <= core && result.is(Blocks.MAGMA_BLOCK)) result = Blocks.LAVA.defaultBlockState();
            if (original != result) blocks.add(new ChangedBlock(pos.immutable(), result));
        }
        game.items().indirectKills().worldChanges(level,cloud,()-> {
        for (var change : blocks) {
            if (level.getBlockEntity(change.pos) instanceof Clearable container) container.clearContent();
            level.setBlock(change.pos, change.state, 2);
        }
        for (var change : blocks) level.updateNeighborsAt(change.pos, change.state.getBlock());
        });
        blocks.clear();
        level.sendParticles(ParticleTypes.FLAME, true, true, cloud.getX(), cloud.getY() + .5, cloud.getZ(), 5, .2, .5, .2, .5);
        if (level.getRandom().nextFloat() < .2f) sound(level, cloud.position(), SoundEvents.LAVA_EXTINGUISH, .5f, 1, 20, .1f);
        for (var entity : targets(level, cloud.position(), outer))
            entity.hurtServer(level,ownedDamage(level,DamageTypes.LAVA,cloud,cloud.getOwner()),4);
    }

    private void sulfur(AreaEffectCloud cloud) {
        var level = (ServerLevel) cloud.level();
        var pos = cloud.blockPosition().offset(-5, -5, -5);
        var size = sulfurTemplate.getSize();
        if (level.hasChunksAt(pos, pos.offset(size))) {
            var random = StructureBlockEntity.createRandom(0);
            var settings = new StructurePlaceSettings().addProcessor(SULFUR_EROSION).setRandom(random);
            game.items().indirectKills().worldChanges(level,cloud,
                    ()->sulfurTemplate.placeInWorld(level, pos, pos, settings, StructureBlockEntity.createRandom(0), 2));
        }
        level.sendParticles(ParticleTypes.SNEEZE, cloud.getX(), cloud.getY(), cloud.getZ(), 10, 4, 4, 4, 0);
        if (level.getRandom().nextFloat() < .03f) spawnAcid(cloud);
        for (var entity : targets(level, cloud.position(), 6)) if (entity.onGround()) {
            entity.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS, 40, 4),cloud);
            entity.addEffect(ItemEffectLevels.create(MobEffects.WITHER, 200, 0),cloud);
        }
    }

    private void phosphine(AreaEffectCloud cloud) {
        var level = (ServerLevel) cloud.level();
        for (var entity : targets(level, cloud.position(), 8)) {
            entity.addEffect(ItemEffectLevels.create(MobEffects.POISON, 200, 0),cloud);
            entity.addEffect(ItemEffectLevels.create(MobEffects.WITHER, 200, 0),cloud);
            entity.addEffect(ItemEffectLevels.create(MobEffects.WEAKNESS, 200, 0),cloud);
            entity.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS, 200, 0),cloud);
        }
    }

    private void fulminate(AreaEffectCloud cloud) {
        // Fulminate использовал короткий TNT только ради взрыва с конфиг-защитой.
        cloud.discard();
        game.items().exasperato().protectedBlast(cloud.getOwner(),(ServerLevel)cloud.level(),cloud.position(),4,null);
    }

    AcidProjectile spawnAcid(AreaEffectCloud cloud) {
        var level = (ServerLevel) cloud.level();
        var projectile = new AcidProjectile(ACID_PROJECTILE, level);
        if (cloud.getOwner() != null) projectile.setOwner(cloud.getOwner());
        if (game.items().currentEpoch(cloud)) projectile.entityTags().add(game.items().epoch());
        projectile.setPos(cloud.position().add(0, 1, 0));
        projectile.setYRot((Mth.floor(cloud.getYRot() * 100) + level.getRandom().nextInt(6001) - 3000) * .01f);
        projectile.setXRot((Mth.floor(cloud.getXRot() * 100) + level.getRandom().nextInt(4001) - 2000) * .01f);
        var direction = projectile.getLookAngle();
        projectile.shoot(direction.x, direction.y, direction.z, .5f, 0);
        projectile.entityTags().addAll(List.of(ACID, "raycastacid", "player", "slowraycast", "shield_blockable2", "magic", "actived"));
        level.addFreshEntity(projectile);
        sound(level, cloud.position().add(0, 1, 0), SoundEvents.ZOMBIE_VILLAGER_CURE, 1, 2, Double.POSITIVE_INFINITY, 0);
        return projectile;
    }

    /** Малый кислотный жезл использует ту же сущность, физику, эффекты и lifecycle, что серное облако. */
    void wandAcid(ServerPlayer owner, Vec3 origin) {
        var level=owner.level();
        for(int i=0;i<6;i++) {
            var projectile=new AcidProjectile(ACID_PROJECTILE,level);
            projectile.setOwner(owner);
            projectile.setPos(origin.add(0,.01*i,0));
            projectile.setYRot(level.getRandom().nextInt(361)-180);
            projectile.setXRot(level.getRandom().nextInt(31)-90);
            var direction=projectile.getLookAngle();
            projectile.shoot(direction.x,direction.y,direction.z,.5f,0);
            projectile.entityTags().addAll(List.of(ACID,WAND_ACID,"raycastacid","player","slowraycast","shield_blockable2","magic","actived"));
            projectile.entityTags().add(game.items().epoch());
            level.addFreshEntity(projectile);
        }
    }

    private boolean wandAcidTarget(AcidProjectile projectile,Entity target) {
        return projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && game.items().wands().projectileTarget(owner,target);
    }

    /** Обычный жезл сохраняет исходный разброс, но летит на общей нативной физике кислоты (user). */
    AcidProjectile ordinaryWandAcid(ServerPlayer owner) {
        var level=owner.level();
        float yaw=(Mth.floor(owner.getYRot()*100)+level.getRandom().nextInt(6001)-3000)*.01f;
        float pitch=(Mth.floor(owner.getXRot()*100)+level.getRandom().nextInt(4001)-2000)*.01f;
        return aimedWandAcid(owner,owner.getEyePosition(),yaw,pitch,true);
    }

    /** Academy removes the ordinary aim spread while retaining its native acid physics and reactions. */
    AcidProjectile academyWandAcid(ServerPlayer owner,boolean ignited) {
        var shot=aimedWandAcid(owner,owner.getEyePosition(),owner.getYRot(),owner.getXRot(),true);
        if(ignited)shot.entityTags().add("on_fire");
        return shot;
    }

    /** Reuse acid physics/effects; source acid_get_id spreads each normal charge around the supplied aim. */
    void magnumAcid(ServerPlayer owner,Vec3 origin,Vec3 direction,boolean big) {
        volleyAcid(owner,origin,direction,big?1:6,big,false);
    }
    /** Ordinary acid staff and its Academy variant share the already ported native acid owner. */
    void volleyAcid(ServerPlayer owner,Vec3 origin,Vec3 direction,int count,boolean big,boolean ignited) {
        var level=owner.level();
        Vec3 forward=direction.lengthSqr()<1e-12 ? owner.getLookAngle() : direction.normalize();
        float yaw=forward.horizontalDistanceSqr()<1e-12 ? 0
                : (float)Math.toDegrees(Math.atan2(-forward.x,forward.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(forward.y,forward.horizontalDistance()));
        for(int i=0;i<count;i++) {
            float shotYaw=big ? yaw : (Mth.floor(yaw*100)+level.getRandom().nextInt(6001)-3000)*.01f;
            float shotPitch=big ? pitch : (Mth.floor(pitch*100)+level.getRandom().nextInt(4001)-2000)*.01f;
            var shot=aimedWandAcid(owner,origin.add(forward.scale(i*.01)),shotYaw,shotPitch,big);
            if(ignited)shot.entityTags().add("on_fire");
        }
        sound(level,origin,SoundEvents.ZOMBIE_VILLAGER_CURE,1,2,Double.POSITIVE_INFINITY,0);
    }

    /** Gaycaster's green Academy ray shares the awakened staff's ordinary seed. */
    AcidProjectile greatAcid(ServerPlayer owner,Vec3 origin,Vec3 direction){
        return awakenedAcid(owner,origin,direction,0,false);
    }
    /** All awakened modes retain native owner, reflection, collision, saved age and reactive acid semantics. */
    AcidProjectile awakenedAcid(ServerPlayer owner,Vec3 origin,Vec3 direction,int variant,boolean ignited){
        return awakenedAcid(owner,origin,direction,variant,ignited,true);
    }
    AcidProjectile awakenedAcid(ServerPlayer owner,Vec3 origin,Vec3 direction,int variant,boolean ignited,boolean audible){
        Vec3 aim=direction.normalize();float yaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));
        var shot=new AcidProjectile(ACID_PROJECTILE,owner.level());shot.setOwner(owner);shot.setPos(origin);
        shot.setYRot(yaw);shot.setXRot(pitch);shot.setDeltaMovement(aim.scale(variant==2?3:.2));shot.setNoGravity(true);
        shot.entityTags().addAll(List.of(ACID,WAND_ACID,"magic","slowraycast",game.items().epoch()));
        if(variant==0)shot.entityTags().addAll(List.of(GREAT_ACID,ACID_500,"great_acid"));
        else shot.entityTags().add(variant==1?CAUST_ACID:BURST_ACID);
        if(variant==1)shot.entityTags().add(ACID_500);
        if(ignited)shot.entityTags().add("on_fire");
        shot.setAttached(ACID_AGE,0);owner.level().addFreshEntity(shot);
        if(audible)sound(owner.level(),origin,SoundEvents.ZOMBIE_VILLAGER_CURE,1,2,Double.POSITIVE_INFINITY,0);return shot;
    }
    static int awakenedVariant(Entity entity){return entity.entityTags().contains(CAUST_ACID)?1:entity.entityTags().contains(BURST_ACID)?2:0;}
    private void burstGreatAcid(AcidProjectile shot,Vec3 at){
        if(shot.isRemoved())return;
        if(shot.getOwner() instanceof ServerPlayer owner)for(int i=0;i<40;i++){
            var child=aimedWandAcid(owner,at,owner.getRandom().nextInt(361)-180,owner.getRandom().nextInt(181)-90,false);
            child.entityTags().remove("shield_blockable2");child.entityTags().addAll(List.of(ACID_500,GREAT_CHILD));
        }
        sound((ServerLevel)shot.level(),at,SoundEvents.SHIELD_BREAK.value(),2,0,Double.POSITIVE_INFINITY,0);shot.discard();
    }

    private AcidProjectile aimedWandAcid(ServerPlayer owner,Vec3 origin,float yaw,float pitch,boolean big) {
        return aimedWandAcid(owner,origin,yaw,pitch,big,null);
    }
    private AcidProjectile aimedWandAcid(ServerPlayer owner,Vec3 origin,float yaw,float pitch,boolean big,Vec3 flyingDirection) {
        var level=owner.level();
        var projectile=new AcidProjectile(ACID_PROJECTILE,level);
        projectile.setOwner(owner);
        projectile.setPos(origin);
        projectile.setYRot(yaw);
        projectile.setXRot(pitch);
        var direction=projectile.getLookAngle();
        projectile.shoot(direction.x,direction.y,direction.z,big?.2f:.5f,0);
        // Entity tags persist with owner, age and stuck state, including after reflection/reload.
        projectile.entityTags().addAll(List.of(ACID,WAND_ACID,big?"raycastacidbig":"raycastacid",
                "player","slowraycast","shield_blockable2","magic","actived",game.items().epoch()));
        if(big) projectile.entityTags().add(ORDINARY_WAND_ACID);
        if(flyingDirection!=null) {
            projectile.entityTags().remove("shield_blockable2");
            projectile.entityTags().addAll(List.of("nogravity",FLYING_WAND_ACID));
            projectile.setNoGravity(true);
            // Native Projectile.shoot uses a different yaw/pitch convention from Entity look vectors.
            projectile.setYRot(yaw);projectile.setXRot(pitch);
            projectile.setDeltaMovement(flyingDirection.normalize().scale(1.6));
        }
        level.addFreshEntity(projectile);
        return projectile;
    }

    AcidProjectile sauvojenAcid(ServerPlayer owner,Vec3 origin,Vec3 direction){
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z))+(owner.getRandom().nextFloat()*2-1)*30;
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()))+(owner.getRandom().nextFloat()*2-1)*20;
        var shot=aimedWandAcid(owner,origin,yaw,pitch,true);
        LbcOverload.sound(owner,origin,"minecraft:entity.zombie_villager.cure",1,2,0);
        return shot;
    }
    AcidProjectile flyingWandAcid(ServerPlayer owner,Vec3 origin,Vec3 direction) {
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        var shot=aimedWandAcid(owner,origin,yaw,pitch,false,direction);
        LbcOverload.sound(owner,origin,"minecraft:entity.zombie_villager.cure",1,2,0);
        return shot;
    }

    /** Exact source variants share acid movement, collision, reactivity and effect code. */
    public AcidProjectile sourceAcid(ServerLevel level,Vec3 origin,float yaw,float pitch,int lifetime,boolean shield,ServerPlayer author){
        if(lifetime!=300 && lifetime!=500)throw new IllegalArgumentException("Source acid budget must be 300 or 500");
        var projectile=new AcidProjectile(ACID_PROJECTILE,level);
        projectile.setPos(origin);projectile.setYRot(yaw);projectile.setXRot(pitch);
        var direction=projectile.getLookAngle();projectile.shoot(direction.x,direction.y,direction.z,.5f,0);
        projectile.entityTags().addAll(List.of(ACID,SOURCE_ACID,"raycastacid","player","slowraycast","magic","actived",game.items().epoch()));
        if(shield)projectile.entityTags().add("shield_blockable2");
        if(lifetime==500)projectile.entityTags().add(ACID_500);
        projectile.setAttached(ACID_AGE,0);
        if(author!=null){acidAuthors.put(author.getUUID(),author);projectile.entityTags().add(ACID_OWNER+author.getUUID());}
        // Keep the native owner empty: source acid must not gain projectile owner collision immunity.
        game.items().timeStop().prepareFrozen(projectile);level.addFreshEntity(projectile);return projectile;
    }
    public static boolean sourceAcidManaged(Entity entity){return entity instanceof AcidProjectile && entity.entityTags().contains(SOURCE_ACID);}
    public LivingEntity sourceAcidOwner(Entity entity){
        if(!sourceAcidManaged(entity))return null;
        var reflected=game.items().electro().reflectedOwner(entity);if(reflected!=null)return reflected;
        if(((AcidProjectile)entity).getOwner() instanceof LivingEntity nativeOwner)return nativeOwner;
        for(var tag:entity.entityTags())if(tag.startsWith(ACID_OWNER))try{
            var id=java.util.UUID.fromString(tag.substring(ACID_OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);
            return online==null?acidAuthors.get(id):online;
        }catch(IllegalArgumentException ignored){}
        return null;
    }
    private boolean sourceAcidActive(Entity entity){return game.playing() && game.stage()!=GameController.Stage.ENDING
            && entity.level()==game.maps().level() && game.items().currentEpoch(entity);}
    public boolean sourceAcidCanAffect(Entity source,Entity target){
        return !sourceAcidManaged(source) || sourceAcidActive(source) && target!=null && game.items().wands().validTarget(target)
                && (!source.entityTags().contains("skywars2.ant_acid") || game.items().antDrops().acidCanAffect(source,target));
    }
    public boolean sourceAcidWild(Entity source){return sourceAcidManaged(source)
            && (!source.entityTags().contains("skywars2.ant_acid") || game.items().antDrops().acidWild(source));}
    public void sourceSound(ServerLevel level,Vec3 origin,net.minecraft.sounds.SoundEvent event,float volume,float pitch){
        sound(level,origin,event,volume,pitch,Double.POSITIVE_INFINITY,0);
    }
    public static int acidLifetime(AcidProjectile projectile){return awakenedVariant(projectile)==2?200:projectile.entityTags().contains(ACID_500)?500:300;}
    public int acidAge(AcidProjectile projectile){var state=acids.get(projectile);return state==null?projectile.getAttachedOrCreate(ACID_AGE):state.lifetime;}
    public List<AcidProjectile> acidSnapshot(){return List.copyOf(acids.keySet());}

    private void tickAcid(AcidState state) {
        var projectile = state.projectile;
        var level = (ServerLevel) projectile.level();
        if(sourceAcidManaged(projectile)){
            if(!sourceAcidActive(projectile)){projectile.discard();return;}
        }
        if(game.items().timeStop().stopped(level))return;
        state.lifetime++;
        if (projectile.entityTags().contains(ACID_STUCK)) stick(projectile, projectile.position());
        if (!level.hasChunkAt(projectile.blockPosition())) {
            projectile.setAttached(ACID_AGE, state.lifetime);
            if (state.lifetime >= acidLifetime(projectile)) projectile.discard();
            return;
        }
        acidImpact(projectile);
        if (projectile.isRemoved()) return;
        if(projectile.entityTags().contains(GREAT_ACID)&&projectile.getOwner() instanceof ServerPlayer owner&&owner.getRandom().nextFloat()<.4f){
            var child=aimedWandAcid(owner,projectile.position(),0,90,false);child.entityTags().remove("shield_blockable2");child.entityTags().add(GREAT_CHILD);
        }
        if (projectile.entityTags().contains("on_fire")&&awakenedVariant(projectile)==0) state.lifetime += 3;
        projectile.setAttached(ACID_AGE, state.lifetime);
        if (state.lifetime >= acidLifetime(projectile)) projectile.discard();
    }

    private void stick(AcidProjectile projectile, Vec3 position) {
        projectile.setPos(position);
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.setNoGravity(true);
        projectile.noPhysics = true;
    }

    /** Общие эффекты для полёта и точки нативного столкновения, без собственного расчёта движения. */
    private void acidImpact(AcidProjectile projectile) {
        if(awakenedVariant(projectile)>0){awakenedImpact(projectile);return;}
        var level = (ServerLevel) projectile.level();
        var origin = projectile.position();
        boolean wand=projectile.entityTags().contains(WAND_ACID);
        boolean ordinary=projectile.entityTags().contains(ORDINARY_WAND_ACID);
        boolean great=projectile.entityTags().contains(GREAT_ACID);
        boolean air = level.getBlockState(projectile.blockPosition()).is(AIRS);
        // shield_block_2 выполняется на блок впереди защищающегося игрока, без углового сектора.
        if (air && !great && !projectile.entityTags().contains(GREAT_CHILD) && !projectile.entityTags().contains(FLYING_WAND_ACID) && (!sourceAcidManaged(projectile) || projectile.entityTags().contains("shield_blockable2"))
                && !level.getEntitiesOfClass(ServerPlayer.class, projectile.getBoundingBox().inflate(3), player ->
                game.allowDamage(player) && player.isBlocking()
                        && (!wand || wandAcidTarget(projectile,player))
                        && (!sourceAcidManaged(projectile) || sourceAcidCanAffect(projectile,player))
                        && player.position().add(player.getLookAngle()).distanceToSqr(projectile.position()) <= 4).isEmpty()) {
            sound(level, projectile.position(), SoundEvents.SHIELD_BLOCK.value(), 1, 1, Double.POSITIVE_INFINITY, 0);
            projectile.discard(); return;
        }
        boolean burning = projectile.entityTags().contains("on_fire");
        if (!burning) {
            if(great){
                level.sendParticles(ParticleTypes.SNEEZE,origin.x,origin.y,origin.z,15,1,1,1,.1);
                level.sendParticles(ORDINARY_ACID_DUST,true,true,origin.x,origin.y,origin.z,5,2,2,2,.1);
            }else if(ordinary) {
                level.sendParticles(ParticleTypes.SNEEZE,origin.x,origin.y,origin.z,2,.5,.5,.5,.1);
                level.sendParticles(ORDINARY_ACID_DUST,true,true,origin.x,origin.y,origin.z,2,.5,.5,.5,.1);
            } else level.sendParticles(ParticleTypes.SNEEZE, true, true, projectile.getX(), projectile.getY(), projectile.getZ(), 1, .1, .1, .1, .1);
            burning = ordinary||great ? ordinaryAcidIgnited(projectile) : level.getBlockState(projectile.blockPosition()).is(FIRE_LAVA)
                    || level.getBlockState(projectile.blockPosition().below()).is(FIRE_LAVA)
                    || !level.getEntities(projectile, projectile.getBoundingBox().inflate(3.5), entity ->
                        (!(entity instanceof ServerPlayer player) || game.allowDamage(player))
                            && (entity.isOnFire() && entity.distanceToSqr(projectile) <= 6.25
                                || (entity instanceof Marker || entity instanceof AcidProjectile) && entity.entityTags().contains("on_fire") && entity.distanceToSqr(projectile) <= 12.25)).isEmpty();
            if (burning) projectile.entityTags().add("on_fire");
        }
        if (burning) {
            // Native Projectile angles use a different convention from Entity.getLookAngle().
            // Derive the trailing visual from flight; a stuck charge retains its last native orientation.
            var flight=projectile.getDeltaMovement();
            var direction=flight.lengthSqr()>1e-12 ? flight.normalize()
                    : Vec3.directionFromRotation(-projectile.getXRot(),-projectile.getYRot());
            var behind=origin.subtract(direction.scale(.5));
            if(great){
                level.sendParticles(ParticleTypes.FLAME,behind.x,behind.y,behind.z,15,1,1,1,.4);
                level.sendParticles(ParticleTypes.FLAME,origin.x,origin.y+1,origin.z,5,2,2,2,.3);
            }else if(ordinary) {
                level.sendParticles(ParticleTypes.FLAME,behind.x,behind.y,behind.z,15,.5,.1,.5,.2);
            } else level.sendParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, 5, .3, .3, .3, .15);
        }
        var damage=burning ? ownedDamage(level,DamageTypes.ON_FIRE,projectile,sourceAcidManaged(projectile)?sourceAcidOwner(projectile):projectile.getOwner()) : null;
        for (var entity : targets(level, origin, ordinary||great ? 5 : 2.5)) {
            if(wand && !wandAcidTarget(projectile,entity)) continue;
            if(sourceAcidManaged(projectile) && !sourceAcidCanAffect(projectile,entity))continue;
            if (burning) {
                entity.hurtServer(level, damage, great ? 7 : ordinary ? 4 : 3);
                level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1, entity.getZ(), 1, .3, .6, .3, .05);
            } else if (ordinary || great || !entity.entityTags().contains("muravei")) {
                var poison=ItemEffectLevels.create(MobEffects.POISON,great ? 1200 : ordinary ? 600 : 300,0);
                if(wand) {
                    if(entity.addEffect(poison,projectile.getOwner()) && projectile.getOwner() instanceof ServerPlayer owner)
                        game.items().indirectKills().poisoned(entity,owner);
                } else entity.addEffect(poison,projectile);
            }
        }
        if(ordinary||great) {
            if(burning) level.sendParticles(ParticleTypes.FLAME,origin.x,origin.y+1,origin.z,5,.3,.6,.3,.05);
            sound(level,origin,SoundEvents.FIRE_EXTINGUISH,.04f,2,Double.POSITIVE_INFINITY,0);
        } else if (level.getRandom().nextFloat() < .2f) sound(level, projectile.position(), SoundEvents.FIRE_EXTINGUISH, .15f, 2, Double.POSITIVE_INFINITY, 0);
    }

    private void awakenedImpact(AcidProjectile shot){
        var level=(ServerLevel)shot.level();int variant=awakenedVariant(shot);double radius=variant==1?15:5;
        Vec3 from=variant==2&&shot.sweptFrom!=null?shot.sweptFrom:shot.position(),to=shot.position();shot.sweptFrom=null;
        var nearby=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(radius),
                e->target(e)&&wandAcidTarget(shot,e));
        // Seven source samples remain visual/area coverage; the author permits one effect per target per tick.
        var hit=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<LivingEntity,Boolean>());
        Vec3 path=to.subtract(from);double length=path.length();int samples=variant==2?Math.min(7,1+Mth.ceil(length*2-1e-6)):1;
        if(!shot.entityTags().contains("on_fire")&&ordinaryAcidIgnited(shot))shot.entityTags().add("on_fire");
        boolean burning=shot.entityTags().contains("on_fire");
        for(int i=0;i<samples;i++){
            double distance=length>3&&samples>1?length*i/(samples-1):Math.min(length,i*.5);
            Vec3 at=length<1e-8?to:from.add(path.scale(distance/length));
            if(burning){
                var fire=new DustColorTransitionOptions(0xffbf60,0xffe600,3);
                level.sendParticles(fire,true,false,at.x,at.y,at.z,variant==1?5:1,variant==1?6:1,variant==1?6:1,variant==1?6:1,.4);
                if(variant==1)level.sendParticles(fire,at.x,at.y,at.z,40,6,6,6,.4);
            }else{
                level.sendParticles(ParticleTypes.SNEEZE,at.x,at.y,at.z,variant==1?10:1,variant==1?4:1,variant==1?4:1,variant==1?4:1,.1);
                level.sendParticles(ORDINARY_ACID_DUST,true,false,at.x,at.y,at.z,variant==1?20:1,variant==1?8:1,variant==1?8:1,variant==1?8:1,.1);
            }
            for(var e:nearby)if(e.position().distanceToSqr(at)<=radius*radius&&hit.add(e)){
                if(burning)e.hurtServer(level,ownedDamage(level,DamageTypes.ON_FIRE,shot,shot.getOwner()),variant==1?15:25);
                else{
                    e.addEffect(ItemEffectLevels.create(MobEffects.POISON,variant==1?2400:1000,10),shot.getOwner());
                    if(variant==2)e.addEffect(ItemEffectLevels.create(MobEffects.WITHER,200,10),shot.getOwner());
                }
            }
            if(level.getRandom().nextFloat()<(variant==1?.2f:.7f))sound(level,at,SoundEvents.FIRE_EXTINGUISH,variant==1?2:.5f,2,Double.POSITIVE_INFINITY,0);
        }
    }

    private boolean ordinaryAcidIgnited(AcidProjectile projectile) {
        var level=(ServerLevel)projectile.level();
        var center=projectile.blockPosition();
        for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))
            if(level.hasChunkAt(pos) && level.getBlockState(pos).is(FIRE_LAVA)) return true;
        boolean great=projectile.entityTags().contains(GREAT_ACID)||awakenedVariant(projectile)>0;
        return !level.getEntities(projectile,projectile.getBoundingBox().inflate(great?10:7),entity ->
                (!(entity instanceof ServerPlayer player) || game.allowDamage(player))
                        && (entity.isOnFire() && entity.distanceToSqr(projectile)<=(great?56.25:25)
                            || (entity instanceof AcidProjectile || entity.getType().builtInRegistryHolder().is(RAYCASTERS))
                                && entity.entityTags().contains("on_fire") && entity.distanceToSqr(projectile)<=(great?100:49))).isEmpty();
    }

    private void sound(ServerLevel level, Vec3 origin, net.minecraft.sounds.SoundEvent event, float volume, float pitch, double radius, float minimumVolume) {
        var sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event);
        for (var player : game.server().getPlayerList().getPlayers()) {
            if (player.level() != level || player.position().distanceToSqr(origin) > radius * radius) continue;
            double distance = player.position().distanceTo(origin), audible = Math.max(volume, 1) * 16;
            Vec3 at = origin;
            float effectiveVolume = volume;
            if (distance > audible) {
                if (minimumVolume <= 0) continue;
                at = player.position().add(origin.subtract(player.position()).normalize().scale(2));
                effectiveVolume = minimumVolume;
            }
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound, SoundSource.MASTER,
                    at.x, at.y, at.z, effectiveVolume, pitch, level.getRandom().nextLong()));
        }
    }
    private static DamageSource ownedDamage(ServerLevel level,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type,
                                           Entity direct,Entity owner) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),direct,owner);
    }

    void stop() {
        var entities = new ArrayList<Entity>();
        entities.addAll(clouds.keySet()); entities.addAll(acids.keySet()); entities.addAll(potions);
        clouds.clear(); acids.clear(); potions.clear();acidAuthors.clear();
        for (var entity : entities) entity.discard();
    }
}
