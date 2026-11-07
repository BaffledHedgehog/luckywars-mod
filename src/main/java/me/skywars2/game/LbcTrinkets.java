package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Тринкеты с реакцией на выстрел/урон: количество читается в момент события, без опроса по тикам. */
public final class LbcTrinkets {
    public static final Item INTERCAL = register("skywars2:intercal", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.9", "lbc:trinket_9", false);
    public static final Item GNEISS_STONE = register("skywars2:gneiss_stone", Items.STONE, "lbctrinket.15", "minecraft:stone", true);
    public static final Item BLAZE = register("skywars2:blaze_trinket", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.0", "lbc:trinket_0", false);
    public static final Item GHAST = register("skywars2:ghast_trinket", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.1", "lbc:trinket_1", false);
    public static final Item SNOW_GEM = register("skywars2:snow_gem", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.17", "lbc:snow_pearl", false);
    public static final Item ICE_GEM = register("skywars2:ice_gem", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.19", "lbc:snow_gem", false);
    public static final Item MANA_GEM = register("skywars2:mana_gem", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.20", "lbc:mana_gem", false);
    public static final Item BLOOD_ANAL_BOOM = register("skywars2:blood_anal_boom", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.4", "lbc:blood_anal_boom", false);
    public static final Item PENIS_GUN = registerAuthored("skywars2:penis_gun_trinket", "lbctrinket.2", "lbc:trinket_2", "penis_gun");
    public static final Item BEDROCK_BALLS = registerAuthored("skywars2:bedrock_balls", "lbctrinket.6", "lbc:trinket_6", "bedrcock");
    public static final Item VOLOS = registerAuthored("skywars2:volos_trinket", "lbctrinket.8", "lbc:trinket_8", "volos");
    public static final Item COCKTAIL = registerAuthored("skywars2:polarization_cocktail", "lbctrinket.10", "lbc:trinket_10", "cocktail");
    public static final Item FOURIER = registerAuthored("skywars2:fourier_trinket", "lbctrinket.12", "lbc:trinket_12", "fourier");
    public static final Item ROCKET_SPELL = registerAuthored("skywars2:rocket_spell_trinket", "lbctrinket.13", "lbc:trinket_13", "rocket_spell");
    public static final Item DROJJI = registerAuthored("skywars2:drojjevoe_odealo", "lbctrinket.3", "lbc:trinket_3", "drojjevoe_odealo");
    public static final Item MANDOAHUET = registerAuthored("skywars2:mandoahuet", "lbctrinket.5", "lbc:trinket_5", "mandoahuet");
    public static final Item NETHER_PLUG = registerAuthored("skywars2:nether_plug", "lbctrinket.7", "lbc:trinket_7", "nether_plug");
    public static final Item BANANGAY = registerAuthored("skywars2:banangay", "lbctrinket.11", "lbc:trinket_11", "banangay");
    public static final Item TWILIGHT_SPARKLE = registerAuthored("skywars2:twilight_sparkle", "lbctrinket.14", "lbc:trinket_14", "twilight_sparkle");
    public static final Item HEDGEHOG_GAUNTLET = registerAuthored("skywars2:hedgehog_gauntlet", "lbctrinket.16", "lbc:hedgehog_gauntlet", "hedgehog_gauntlet");
    public static final Item POLY_GEM = registerAuthored("skywars2:poly_gem", "lbctrinket.21", "lbc:poly_gem", "poly_gem");
    public static final Item GLASS_SHARD = registerAuthored("skywars2:glass_shard", "lbctrinket.22", "lbc:glass_shard", "glass_shard");
    public static final Item PSYCHO_SOUL = registerAuthored("skywars2:psycho_soul", "lbctrinket.23", "lbc:psycho_soul", "psycho_soul");
    private static final Item[] SHOT_ITEMS={BLAZE,GHAST,PENIS_GUN,BEDROCK_BALLS,VOLOS,COCKTAIL,FOURIER,ROCKET_SPELL};
    private static final String[] SHOT_IDENTITIES={"blaze","ghast","penis_gun","bedrcock","volos","cocktail","fourier","rocket_spell"};
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final CompoundTag INTERCAL_DATA = identity("intercal");
    private static final CompoundTag GNEISS_DATA = identity("gneiss_stone");
    private static final CompoundTag BLAZE_DATA = identity("blaze");
    private static final CompoundTag GHAST_DATA = identity("ghast");
    private static final CompoundTag SNOW_DATA = identity("snow_pearl");
    private static final CompoundTag ICE_DATA = identity("ice_gem");
    private static final CompoundTag MANA_GEM_DATA = identity("mana_gem");
    private static final CompoundTag BLOOD_ANAL_BOOM_DATA = identity("blood_anal_boom");
    private static final CompoundTag NETHER_PLUG_DATA = identity("nether_plug");
    private static final CompoundTag GHAST_ROCKET = readRocket();
    private static final String PROJECTILE_TAG = "skywars2.trinket.projectile";
    private final GameController game;
    private final Set<Projectile> projectiles = new LinkedHashSet<>();

    public LbcTrinkets(GameController game) { this.game = game; }
    public static void register() { }

    private static CompoundTag readRocket() {
        try (var input = LbcTrinkets.class.getResourceAsStream("/skywars2/ghast_rocket.snbt")) {
            if (input == null) throw new IllegalStateException("Missing Ghast rocket preset");
            return TagParser.parseCompoundFully(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception failure) { throw new IllegalStateException("Invalid source Ghast rocket preset", failure); }
    }

    private static CompoundTag identity(String id) {
        var tag = new CompoundTag(); tag.putString("trinket", id); return tag;
    }

    private static Item register(String id, Item proxy, String name, String model, boolean placeable) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
        var properties = new Item.Properties().setId(key).stacksTo(99).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL, Identifier.parse(model));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, proxy, true) {
            @Override public InteractionResult useOn(UseOnContext context) {
                // Гнейс в исходнике — обычный камень; размещение и расход оставляем нативному BlockItem.
                return placeable ? Items.STONE.useOn(context) : super.useOn(context);
            }
        });
    }

    /** Keep native /give and source loot identical without parsing item NBT during gameplay. */
    private static Item registerAuthored(String id,String name,String model,String table) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        var props=new Item.Properties().setId(key).stacksTo(99).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL,Identifier.parse(model));
        try(var input=LbcTrinkets.class.getResourceAsStream("/data/lbc/loot_table/trinket/"+table+".json")){
            if(input==null)throw new IllegalStateException("Missing trinket definition "+table);
            var root=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var entry=root.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            var functions=entry.has("modifier")?entry.getAsJsonArray("modifier"):entry.getAsJsonArray("functions");
            for(var value:functions){var row=value.getAsJsonObject();String type=row.get(row.has("type")?"type":"function").getAsString();
                if(type.equals("minecraft:set_name"))props.component(DataComponents.CUSTOM_NAME,
                        net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,row.get("name")).getOrThrow());
                if(type.equals("minecraft:set_lore")){var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
                    for(var line:row.getAsJsonArray("lore"))lines.add(net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,line).getOrThrow());
                    props.component(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(lines));}
                if(type.equals("minecraft:set_custom_data"))props.component(DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(TagParser.parseCompoundFully(row.get("tag").getAsString())));
            }
        }catch(Exception error){throw new IllegalStateException("Invalid trinket definition "+table,error);}
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.REPEATING_COMMAND_BLOCK,true));
    }

    private static int count(ServerPlayer player, Item item, CompoundTag identity) {
        if(!LbcDimensionItems.ordinary(player))return 0;
        // Как /clear ... 0: инвентарь, личная сетка крафта и предмет на курсоре, без изменения стаков.
        return player.getInventory().clearOrCountMatchingItems(stack -> {
            var data = stack.get(DataComponents.CUSTOM_DATA);
            return stack.is(item) || data != null && data.matchedBy(identity)||LbcTmtItems.matches(stack)&&LbcTmtInventory.matches(stack,identity);
        }, true, 0, player.inventoryMenu.getCraftSlots());
    }

    /** One traversal of the source inventory/crafting/cursor set, only on successful native volleys. */
    private static int[] shotCounts(ServerPlayer player){
        var counts=new int[SHOT_ITEMS.length];if(!LbcDimensionItems.ordinary(player))return counts;
        player.getInventory().clearOrCountMatchingItems(stack->{
            var identity=ItemData.stringValue(stack,"trinket","");
            var identities=LbcTmtItems.matches(stack)?LbcTmtInventory.strings(stack,"trinket"):java.util.Set.<String>of();
            for(int i=0;i<SHOT_ITEMS.length;i++)if(stack.is(SHOT_ITEMS[i])||identity.equals(SHOT_IDENTITIES[i])||identities.contains(SHOT_IDENTITIES[i]))
                counts[i]=(int)Math.min(Integer.MAX_VALUE,(long)counts[i]+stack.getCount());
            return false; // Native count-only traversal; no debit or item mutation.
        },true,0,player.inventoryMenu.getCraftSlots());
        return counts;
    }
    public void shot(ServerPlayer player){shot(player,List.of());}
    public void shot(ServerPlayer player,List<? extends Projectile> family) {
        if (game.balanceLegacy() || !game.canUseGameplayItems(player) || player.level() != game.maps().level() || game.config().lbcTrinketLimit == 0) return;
        var counts=shotCounts(player);
        int copies = counts[0];
        // Сохраняем отдельные source-ветки для одной копии и гарантированного количества.
        if (copies > 0 && (copies >= 11 || player.getRandom().nextFloat() < (copies == 1 ? .15f : (175 + 75 * copies) * .001f))) {
            var fireball = new SmallFireball(EntityTypes.SMALL_FIREBALL, player.level());
            // (user) Исправляем ownerless source: стрелок защищён при запуске, урон сохраняет авторство.
            fireball.setOwner(player);
            fireball.setPos(player.getEyePosition());
            fireball.setDeltaMovement(aim(player, 1000, .0016));
            fireball.entityTags().add("can_deleted");
            spawn(fireball);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.MASTER, 1, 1);
        }
        copies = counts[1];
        if (copies > 0 && (copies >= 16 || player.getRandom().nextFloat() < (copies == 1 ? .25f : (100 + 50 * copies) * .001f))) {
            var rocket = new FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET, player.level());
            // Нативная загрузка сохраняет source LifeTime=100, ShotAtAngle и точные пять burst-взрывов без accessor к private полям.
            rocket.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), GHAST_ROCKET.copy()));
            rocket.setOwner(player);
            rocket.setPos(player.getEyePosition());
            rocket.setDeltaMovement(aim(player, 10000, .0002));
            spawn(rocket);
        }
        // Independent source rolls apply to the exact published family, never nearby foreign arrows.
        boolean magical=(counts[3]==0&&counts[4]==0&&counts[6]==0)||!game.items().magic().suppressed(player);
        boolean technical=(counts[2]==0&&counts[5]==0&&counts[7]==0)||!game.items().magic().mechanismSuppressed(player);
        copies=counts[2];if(technical&&copies>0&&(copies>=37||player.getRandom().nextFloat()<(.1f+.025f*(copies-1))))
            game.items().kfcRewards().launchTrinketBomb(player);
        int mask=0;
        copies=counts[3];if(magical&&copies>0&&player.getRandom().nextFloat()<(.2f+.1f*(copies-1)))mask|=LbcTrinketArrows.BEDROCK;
        copies=counts[4];if(magical&&copies>0&&player.getRandom().nextFloat()<(.05f+.01f*(copies-1)))mask|=LbcTrinketArrows.VOLOS;
        copies=counts[5];if(technical&&copies>0&&player.getRandom().nextFloat()<(.1f+.05f*(copies-1)))mask|=LbcTrinketArrows.COCKTAIL;
        copies=counts[6];if(magical&&copies>0&&player.getRandom().nextFloat()<(.15f+.05f*(copies-1)))game.items().homing().launchFourier(player);
        copies=counts[7];if(technical&&copies>0&&player.getRandom().nextFloat()<(.05f+.01f*(copies-1))
                &&family.stream().anyMatch(p->p instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow||LbcShulkerBow.managed(p)||LbcLegacyRanged.hasAmmo(p))
                &&game.items().mana().trySpend(player,6)){
            mask|=LbcTrinketArrows.ROCKET;
            LbcOverload.sound(player,player.getEyePosition(),"minecraft:lbcsounds.hihihiha",2,1,0);
        }
        if(mask!=0)for(var projectile:family)if(projectile.getOwner()==player&&!projectile.isRemoved()&&projectile.level()==player.level()){
            if(projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow||LbcShulkerBow.managed(projectile))game.items().trinketArrows().mark(projectile,mask);
            else LbcLegacyRanged.markTraits(game,projectile,mask);
        }
    }

    private static Vec3 aim(ServerPlayer player, int precision, double scale) {
        var direction = player.getLookAngle();
        return new Vec3(Mth.floor(direction.x * precision) * scale, Mth.floor(direction.y * precision) * scale,
                Mth.floor(direction.z * precision) * scale);
    }

    void spawn(Projectile projectile) {
        projectile.entityTags().add(PROJECTILE_TAG);
        game.maps().level().addFreshEntity(projectile);
    }

    /** Общий on_summon: 50% для первой копии Nether Plug, затем +10% за каждую. */
    public void summoned(ServerPlayer player) {
        if(!game.canUseGameplayItems(player) || player.level()!=game.maps().level() || game.config().lbcTrinketLimit==0 || game.items().magic().suppressed(player)) return;
        int copies=count(player,Items.AIR,NETHER_PLUG_DATA);
        if(copies>0 && player.getRandom().nextFloat()<.5f+.1f*(copies-1)) game.items().blaze().summonFriend(player);
    }
    public void castWand(ServerPlayer player) {
        if (!game.canUseGameplayItems(player) || player.level() != game.maps().level()
                || game.config().lbcTrinketLimit == 0) return;
        int copies = count(player, BLOOD_ANAL_BOOM, BLOOD_ANAL_BOOM_DATA);
        if (copies > 0 && (copies >= 16 || player.getRandom().nextFloat() < (copies == 1 ? .25f : .2f + .05f * copies))) {
            game.items().castWandDynamite(player);
        }
        copies = count(player, MANA_GEM, MANA_GEM_DATA);
        if (copies > 0) game.items().mana().add(player, 60 + 15 * (copies - 1));
    }

    boolean loaded(Entity entity) {
        if (!(entity instanceof Projectile projectile)
                || !entity.entityTags().contains(PROJECTILE_TAG) && !game.items().wands().isFireProjectile(entity)) return false;
        entity.entityTags().add(PROJECTILE_TAG);
        ServerPlayer owner = projectile.getOwner() instanceof ServerPlayer player ? player : null;
        boolean ownerCanUse = owner != null && game.canUseGameplayItems(owner);
        boolean currentEpoch = game.items().currentEpoch(entity);
        if (owner != null && game.isObserver(owner)) entity.discard();
        else if (entity.level() == game.maps().level()
                && (game.playing() && (owner == null || ownerCanUse)
                    || game.items().debugItemUseWindow() && ownerCanUse || currentEpoch)) projectiles.add(projectile);
        else entity.discard();
        return true;
    }

    void unloaded(Entity entity) { projectiles.remove(entity); }
    public int projectileCount() { return projectiles.size(); }

    void stop() {
        // ENTITY_UNLOAD меняет коллекцию синхронно; очищаем её до discard и сохраняем единую эпоху LbcItems.
        var pending = List.copyOf(projectiles); projectiles.clear();
        for (var projectile : pending) projectile.discard();
    }

    public void damaged(LivingEntity victim, DamageSource source) {
        if ((!game.playing() && !game.items().debugItemUseWindow()) || game.config().lbcTrinketLimit == 0) return;
        if (victim instanceof ServerPlayer player && LbcDimensionItems.ordinary(player) && game.allowDamage(player) && player.isAlive()) {
            incoming(player,count(player,ICE_GEM,ICE_DATA),count(player,GNEISS_STONE,GNEISS_DATA));
        }
        if(source.getEntity() instanceof ServerPlayer attacker)
            outgoing(attacker,source,count(attacker,INTERCAL,INTERCAL_DATA),count(attacker,SNOW_GEM,SNOW_DATA));
    }

    /** Both old and new defensive trinkets share the actual native incoming criterion. */
    void incoming(ServerPlayer player,int ice,int copies){
        if(!player.isAlive()||!game.allowDamage(player))return;
        if (ice > 0 && player.getRandom().nextFloat() < (90 + 10d * ice) * .001) iceTargets(player);
        if (copies > 0 && player.getRandom().nextFloat() < (80 + 20d * copies) * .001) {
                player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE, 40, 4));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1, 1);
                player.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                        player.getX(), player.getY() + 1, player.getZ(), 30, .3, .6, .3, 0);
        }
    }

    void outgoing(ServerPlayer attacker,DamageSource source,int copies,int snow){
        // Source arrow_only также покрывает отдельный тип Electro bolt; остальные снаряды не исключаем.
        if (LbcDamage.isArrowDamage(source)
                || !LbcDimensionItems.ordinary(attacker) || !game.allowDamage(attacker) && !game.canUseGameplayItems(attacker)) return;
        if (snow > 0 && attacker.getRandom().nextFloat() < (80 + 20 * snow) * .001f) game.items().frost().hitTargets(attacker);
        if (copies == 0 || attacker.getRandom().nextFloat() >= (100 + 50 * copies) * .001f) return;
        boolean upgraded = game.lbc().hasSkill(attacker,9);
        for (var target : attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(10), entity ->
                entity.isAlive() && entity.getType().builtInRegistryHolder().is(MOBS)
                        && !entity.entityTags().contains("spectator") && !attacker.isAlliedTo(entity)
                        && (!(entity instanceof ServerPlayer player) || game.allowDamage(player)))) {
            double distance = attacker.distanceToSqr(target);
            if (distance < .000001 || distance > 100) continue;
            target.addEffect(ItemEffectLevels.create(MobEffects.WITHER, 200, 0), attacker);
            // Как две исходные effect give: короткий IV скрывает I, затем нативная цепочка возвращает I.
            if (upgraded) target.addEffect(ItemEffectLevels.create(MobEffects.WITHER, 60, 3), attacker);
        }
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.WITHER_AMBIENT, SoundSource.MASTER, 1, 1);
    }

    private void iceTargets(ServerPlayer owner) {
        // Ice Gem ищет всех подходящих мобов в окружении владельца; Snow Pearl использует общую область меча.
        for (var target : owner.level().getEntitiesOfClass(LivingEntity.class, new AABB(owner.position(), owner.position()).inflate(12), entity ->
                entity.isAlive() && !entity.entityTags().contains("spectator") && !owner.isAlliedTo(entity)
                        && entity.getType().builtInRegistryHolder().is(MOBS)
                        && (!(entity instanceof ServerPlayer player) || game.allowDamage(player)))) {
            double distance = owner.distanceToSqr(target);
            if (distance >= .000001 && distance <= 144) game.items().frost().add(target, owner);
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1, 1);
        owner.level().sendParticles(ParticleTypes.SNOWFLAKE, owner.getX(), owner.getY()+.5, owner.getZ(), 30, .5, .5, .5, 1);
    }

    List<Projectile> transientSnapshot(){return List.copyOf(projectiles);}
}
