package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

/** Жезлы используют общий расход маны и каст-хуки; временные снаряды хранит существующий трекер. */
public final class LbcWands {
    public static final Item TINY_FIRE=register("skywars2:tiny_fire_wand","wand_fire_tiny","lbc:st/fire_tiny");
    public static final Item TINY_BLIND=register("skywars2:tiny_blind_wand","wand_blind_tiny","lbc:st/blind_tiny");
    public static final Item TINY_AIR=register("skywars2:tiny_air_wand","wand_air_tiny","lbc:st/air_tiny");
    public static final Item TINY_STONE=register("skywars2:tiny_stone_wand","wand_stone_tiny","lbc:st/stone_tiny");
    public static final Item TINY_TELEPORT=register("skywars2:tiny_teleport_wand","wand_teleport_tiny","lbc:st/teleport_tiny");
    public static final Item TINY_RUBY=register("skywars2:tiny_ruby_wand","wand_ruby_tiny","lbc:st/ruby_tiny");
    public static final Item TINY_TERRA=register("skywars2:tiny_terra_wand","wand_terra_tiny","lbc:st/terra_tiny");
    private enum Spell {
        FIRE(TINY_FIRE,"stfire_tiny","tiny_fire_wand_mana_use",25),
        BLIND(TINY_BLIND,"stblind_tiny","tiny_blind_wand_mana_use",63),
        AIR(TINY_AIR,"stair_tiny","tiny_air_wand_mana_use",250),
        STONE(TINY_STONE,"ststone_tiny","tiny_stone_wand_mana_use",500),
        TELEPORT(TINY_TELEPORT,"stteleport_tiny","tiny_teleport_wand_mana_use",750),
        RUBY(TINY_RUBY,"struby_tiny","tiny_ruby_wand_mana_use",500),
        TERRA(TINY_TERRA,"stterra_tiny","tiny_terra_wand_mana_use",250);
        final Item item;
        final CompoundTag flag;
        final ScoreHolder cost;
        final int defaultCost;
        Spell(Item item,String flag,String cost,int defaultCost) {
            this.item=item; this.flag=flag(flag); this.cost=ScoreHolder.forNameOnly(cost); this.defaultCost=defaultCost;
        }
    }
    private static final Spell[] SPELLS=Spell.values();
    private static final ScoreHolder MAGE_BONUS=ScoreHolder.forNameOnly("mage_set_bonus");
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final int STRICT_FLAGS=Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;
    private static final String FIRE_PROJECTILE="skywars2.wand.fire";
    private static final String AIR_PROJECTILE="skywars2.wand.air";
    private static final CompoundTag AIR_ROCKET=airRocketPreset();
    private static final DustParticleOptions RUBY_PARTICLE=new DustParticleOptions(ARGB.colorFromFloat(1,.89f,0,.973f) & 0xFFFFFF,1);
    private static final DustColorTransitionOptions TERRA_PARTICLE=new DustColorTransitionOptions(
            ARGB.colorFromFloat(1,.01f,1,.134f) & 0xFFFFFF,ARGB.colorFromFloat(1,.004f,1,.137f) & 0xFFFFFF,1);
    private final GameController game;
    private final LbcMana mana;

    private static Item register(String id,String name,String model) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        var properties=new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse(model); }
        }.setId(key).durability(25).overrideDescription(name);
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var game=SkyWars2.game();
                return game!=null && player instanceof ServerPlayer p && game.items().wands().use(p,hand)
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
    }
    static void register() { }
    private static CompoundTag flag(String name) { var value=new CompoundTag(); value.putInt(name,1); return value; }
    private static boolean matches(ItemStack stack,Item nativeItem,CompoundTag flag) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(nativeItem) || data!=null && data.matchedBy(flag);
    }
    private static CompoundTag airRocketPreset() {
        try {
            return TagParser.parseCompoundFully("{Life:0,LifeTime:0,ShotAtAngle:1b,FireworksItem:{id:\"minecraft:firework_rocket\",count:1,components:{\"minecraft:fireworks\":{explosions:[{shape:\"burst\",colors:[16770831],fade_colors:[12422150],has_trail:1b,has_twinkle:1b}]}}}}");
        } catch(Exception failure) { throw new IllegalStateException("Invalid Tiny Air rocket preset",failure); }
    }
    LbcWands(GameController game,LbcMana mana) {
        this.game=game; this.mana=mana;
        for(var spell:SPELLS) mana.score(spell.cost,"lbc.math").set(spell.defaultCost);
        mana.score(MAGE_BONUS,"lbc.math").set(150);
    }
    /** Каждый жезл оплачивается отдельно; countdown nomagic приостанавливает regen, а не эти касты. */
    public boolean use(ServerPlayer player,InteractionHand hand) {
        if(hand==InteractionHand.OFF_HAND && !game.config().offhandWands) return false;
        boolean applied=cast(player,hand);
        // Старый items/spells последовательно обрабатывал две руки на один ПКМ, если lefthand разрешён.
        if(hand==InteractionHand.MAIN_HAND && game.config().offhandWands)
            applied=cast(player,InteractionHand.OFF_HAND) || applied;
        return applied;
    }
    private boolean cast(ServerPlayer player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        Spell spell=null;
        for(var candidate:SPELLS) if(matches(stack,candidate.item,candidate.flag)) { spell=candidate; break; }
        if(spell==null || !mana.trySpendWand(player,mana.score(spell.cost,"lbc.math").get())) return false;
        switch(spell) {
            case FIRE -> fire(player);
            case RUBY -> rubyRay(player);
            case TERRA -> terraRay(player);
            default -> ray(player,spell);
        }
        if(mana.mageFullSet(player)) mana.maximum(player,mana.maximum(player)+mana.score(MAGE_BONUS,"lbc.math").get());
        game.items().trinkets().castWand(player,player.getEyePosition());
        return true;
    }
    private void fire(ServerPlayer player) {
        Vec3 direction=player.getLookAngle();
        var projectile=new SmallFireball(EntityTypes.SMALL_FIREBALL,player.level());
        projectile.setOwner(player);
        projectile.setPos(player.getEyePosition().add(direction));
        // Исходный marker в начале мира вычислял именно этот вектор, не мировую цель полёта.
        projectile.setDeltaMovement(direction.scale(2.5));
        projectile.entityTags().add(FIRE_PROJECTILE);
        projectile.entityTags().add("stwandfireball");
        projectile.entityTags().add("can_deleted");
        game.items().trinkets().spawn(projectile);
        LbcOverload.sound(player,player.getEyePosition(),"minecraft:item.firecharge.use",1,2,0);
    }
    /** (user) Ruby намеренно прорезает всю линию, включая неразрушимые обычным инструментом блоки. */
    private void rubyRay(ServerPlayer player) {
        var level=player.level();
        Vec3 origin=player.getEyePosition(),step=player.getLookAngle().scale(.35);
        BlockPos previous=null;
        boolean drops=!(player.getAbilities().instabuild && game.isDeveloper(player));
        for(int i=0;i<=100;i++) {
            Vec3 point=origin.add(step.scale(i));
            var pos=BlockPos.containing(point);
            if(!level.hasChunkAt(pos)) break;
            level.sendParticles(RUBY_PARTICLE,true,false,point.x,point.y,point.z,1,0,0,0,0);
            // Шаг меньше блока: соседние samples одной клетки не требуют повторного destroy/loot.
            if(pos.equals(previous)) continue;
            previous=pos;
            var old=level.getBlockState(pos);
            if(old.isAir()) continue;
            level.destroyBlock(pos,drops,player,Block.UPDATE_LIMIT);
            // destroyBlock оставляет fluid state; исходный fill air destroy удалял и саму жидкость.
            if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
                level.updateNeighboursOnBlockSet(pos,old);
        }
        LbcOverload.sound(player,origin,"minecraft:block.amethyst_cluster.break",1,2,0);
    }
    /** (user) Terra пробивает существ, но каждая цель получает лишь одну попытку 5 cactus-урона за каст. */
    private void terraRay(ServerPlayer player) {
        var level=player.level();
        Vec3 origin=player.getEyePosition(),step=player.getLookAngle().scale(.35);
        var candidates=level.getEntities((Entity)null,rayBounds(origin,step),e->target(player,e));
        var damaged=new java.util.HashSet<Entity>();
        var damage=new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),player);
        for(int i=0;i<=100;i++) {
            Vec3 point=origin.add(step.scale(i));
            var pos=BlockPos.containing(point);
            if(!level.hasChunkAt(pos)) break;
            if(player.getRandom().nextFloat()<.3f)
                level.sendParticles(TERRA_PARTICLE,true,false,point.x,point.y,point.z,1,0,0,0,0);
            LbcOverload.sound(player,point,"minecraft:block.amethyst_cluster.hit",.1f,2,0);
            var cell=new AABB(point,point.add(1,1,1));
            for(var entity:candidates) if(!damaged.contains(entity) && target(player,entity)
                    && entity.position().distanceToSqr(point)<=64 && entity.getBoundingBox().intersects(cell)) {
                // Не сбрасываем нативную неуязвимость: уже раненая цель сохраняет обычную защиту от повторов.
                damaged.add(entity);
                entity.hurtServer(level,damage,5);
            }
            if(!level.getBlockState(pos).is(AIRS)) {
                // Source setblock сначала ставит траву над стеной, затем превращает стену в мох.
                placeTerraBlock(player,pos.above(),Blocks.SHORT_GRASS);
                placeTerraBlock(player,pos,Blocks.MOSS_BLOCK);
                break;
            }
        }
    }
    private void placeTerraBlock(ServerPlayer player,BlockPos pos,Block block) {
        var level=player.level();
        if(!level.hasChunkAt(pos) || !level.isInValidBounds(pos)) return;
        var old=level.getBlockState(pos);
        // Нативные setblock flags 258, включая обновление соседей после успешной замены.
        if(level.setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
            level.updateNeighboursOnBlockSet(pos,old);
    }
    /** Малые жезлы делят мгновенный луч: кандидаты выбираются один раз на каст, без marker-тикеров. */
    void blindRay(ServerPlayer player) {
        ray(player,Spell.BLIND);
    }
    private static AABB rayBounds(Vec3 origin,Vec3 step) {
        // Попадание требует пересечения ячейки point..point+1: дальность selector 8 не расширяет её.
        // Включаем предыдущий шаг для wall-end у самых глаз; лишние секции вокруг луча не опрашиваются.
        return new AABB(origin.subtract(step),origin.add(step.scale(100))).expandTowards(1,1,1);
    }
    private void ray(ServerPlayer player,Spell spell) {
        Vec3 origin=player.getEyePosition(), step=player.getLookAngle().scale(.35);
        var candidates=player.level().getEntities((Entity)null,rayBounds(origin,step),e->target(player,e));
        ParticleOptions particle=switch(spell) {
            case BLIND -> ParticleTypes.SQUID_INK;
            case AIR -> ParticleTypes.FIREWORK;
            case STONE -> new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState());
            case TELEPORT -> ParticleTypes.CLOUD;
            default -> throw new IllegalArgumentException("Not a ray spell: "+spell);
        };
        for(int i=0;i<=100;i++) {
            Vec3 point=origin.add(step.scale(i));
            if(spell!=Spell.BLIND || player.getRandom().nextFloat()<.3f)
                player.level().sendParticles(particle,true,false,point.x,point.y,point.z,1,0,0,0,0);
            Entity hit=rayTarget(player,candidates,point);
            if(hit!=null) {
                // (user) Первый контакт завершает луч: исходный повтор при entity+wall был багом.
                rayEnd(player,spell,point,hit);
                break;
            }
            var pos=BlockPos.containing(point);
            if(!player.level().hasChunkAt(pos) || !player.level().getBlockState(pos).is(AIRS)) {
                // Стена оставляет terminal на предыдущей свободной точке, действие выполняется один раз.
                var previous=point.subtract(step);
                rayEnd(player,spell,previous,rayTarget(player,candidates,previous));
                break;
            }
        }
        raySound(player,spell,origin);
    }
    private void raySound(ServerPlayer player,Spell spell,Vec3 point) {
        String sound=switch(spell) {
            case BLIND -> "minecraft:entity.squid.squirt";
            case AIR -> "minecraft:entity.generic.explode";
            case STONE -> "minecraft:block.stone.place";
            case TELEPORT -> "minecraft:entity.lightning_bolt.thunder";
            default -> throw new IllegalArgumentException("Not a ray spell: "+spell);
        };
        LbcOverload.sound(player,point,sound,1,2,0);
    }
    private void rayEnd(ServerPlayer player,Spell spell,Vec3 point,Entity hit) {
        switch(spell) {
            case BLIND -> { blindEnd(player,point,hit); return; }
            case AIR -> {
                var rocket=new FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET,player.level());
                // Нативный load задаёт LifeTime=0 и source-палитру без reflection/accessor к private полям.
                rocket.load(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),AIR_ROCKET.copy()));
                rocket.setOwner(player);
                rocket.setPos(point);
                rocket.entityTags().add(AIR_PROJECTILE);
                game.items().trinkets().spawn(rocket);
            }
            case STONE -> {
                // Эквивалент fill strict: ровно 27 блоков, без соседних обновлений и старого дропа.
                for(var pos:BlockPos.betweenClosed(BlockPos.containing(point.add(-1,-1,-1)),BlockPos.containing(point.add(1,1,1))))
                    if(player.level().hasChunkAt(pos)) player.level().setBlock(pos,Blocks.STONE.defaultBlockState(),STRICT_FLAGS);
            }
            case TELEPORT -> {
                var cell=new AABB(point,point.add(1,1,1));
                // Union исходных двух селекторов: одинаковый эффект не требует двух записей одной цели.
                for(var entity:player.level().getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(12),e->target(player,e)))
                    if(entity.getType().builtInRegistryHolder().is(MOBS) && (entity.position().distanceToSqr(point)<=4
                            || entity.position().distanceToSqr(point)<=144 && entity.getBoundingBox().intersects(cell)))
                        entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION,20,30),player);
            }
            default -> throw new IllegalArgumentException("Not a ray spell: "+spell);
        }
        raySound(player,spell,point);
    }
    private Entity rayTarget(ServerPlayer player,java.util.List<Entity> candidates,Vec3 point) {
        var cell=new AABB(point,point.add(1,1,1));
        for(var entity:candidates) if(target(player,entity) && entity.position().distanceToSqr(point)<=64
                && entity.getBoundingBox().intersects(cell)) return entity;
        return null;
    }
    private void blindEnd(ServerPlayer player,Vec3 point,Entity hit) {
        if(hit!=null) {
            if(hit instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,80,0));
            hit.hurtServer(player.level(),new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.ARROW),player),.25f);
        }
        LbcOverload.sound(player,point,"minecraft:entity.squid.squirt",1,2,0);
    }
    private boolean target(ServerPlayer player,Entity entity) {
        return entity!=player && entity.isAlive() && !entity.isRemoved() && !player.isAlliedTo(entity)
                && !entity.isSpectator() && !entity.entityTags().contains("spectator")
                && (!(entity instanceof ServerPlayer p) || game.canUseGameplayItems(p));
    }
    /** Нативный canHitEntity пропускает союзников и наблюдателей до поджигания/урона small fireball. */
    public boolean canHit(Projectile projectile,Entity target) {
        if(!projectile.entityTags().contains(FIRE_PROJECTILE) && !projectile.entityTags().contains(AIR_PROJECTILE)) return true;
        return projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner) && target(owner,target);
    }
    /** Радиус взрыва native firework обходит canHitEntity; союзников защищаем и в финальном уроне. */
    public boolean allowDamage(Entity victim,DamageSource source) {
        if(!(source.getDirectEntity() instanceof FireworkRocketEntity rocket) || !rocket.entityTags().contains(AIR_PROJECTILE)) return true;
        return rocket.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && (victim==owner || !owner.isAlliedTo(victim));
    }
}
