package me.skywars2.game.entities.mobs.summons;

import eu.pb4.polymer.core.api.item.PolymerItem;
import me.skywars2.SkyWars2;
import me.skywars2.game.entities.LbcDimensionItems;
import me.skywars2.game.entities.mobs.LbcFakePlayer;
import me.skywars2.game.entities.mobs.LbcStalin;
import me.skywars2.game.entities.mobs.LbcTransformator;
import me.skywars2.game.items.drops.LbcAntDrops;
import me.skywars2.game.items.drops.LbcCreatureDrops;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.skywars2.game.GameController;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.gadgets.LbcImbaFollowup;
import me.skywars2.game.items.gadgets.LbcImbaPlaceableItems;

/** Source eggs retain vanilla placement, finalizeSpawn, fluid use and dispenser spawning. */
public final class LbcSpecialEggs {
    private static final String SPAWNER_MARK = "skywars2.spawner_egg.";

    public enum Kind {
        MOM("skywars2:mom_spawn_egg", "mom_spawn_egg", Items.PIG_SPAWN_EGG, EntityTypes.ITEM, "lbc:tier_ff", """
                {"PickupDelay":32767,"Item":{"count":1,"id":"skywars2:mom_drop"}}
                """),
        OTCHIM("skywars2:otchim_spawn_egg", "otchim_spawn_egg", Items.SNOW_GOLEM_SPAWN_EGG, EntityTypes.ITEM, "lbc:tier_ff", """
                {"PickupDelay":32767,"Item":{"count":1,"id":"skywars2:otchim_drop"}}
                """),
        SLAB("skywars2:slab_spawn_egg", "slab_spawn_egg", Items.ZOMBIE_SPAWN_EGG, EntityTypes.ZOMBIE, "lbc:tier_ff", """
                {PersistenceRequired:true,DeathLootTable:"minecraft:empty",IsBaby:true,Silent:true,
                 equipment:{head:{id:"minecraft:oak_slab",count:1,components:{
                  "minecraft:item_name":{translate:"agressive_slab",italic:false},
                  "minecraft:attribute_modifiers":[{type:"minecraft:attack_damage",id:"minecraft:base_attack_damage",amount:7,operation:"add_value",slot:"mainhand"}]}}},
                 drop_chances:{head:1.0f},active_effects:[{id:"minecraft:invisibility",amplifier:1,duration:2000000,show_particles:false}]}
                """),
        LOW_VEX("skywars2:low_vex_spawn_egg", "low_vex_spawn_egg", Items.VEX_SPAWN_EGG, EntityTypes.VEX, "lbc:tier_f", """
                {DeathLootTable:"lbc:vex_sword",equipment:{mainhand:{}},Health:1.0f,attributes:[{id:"minecraft:max_health",base:1.0d}]}
                """),
        SARVATRA_CREEPER("skywars2:sarvatra_creeper_spawn_egg", "sarvatra_creeper_spawn_egg", Items.CREEPER_SPAWN_EGG, EntityTypes.CREEPER, "lbc:tier_f", """
                {ExplosionRadius:1,DeathLootTable:"lbc:creeper_wand"}
                """),
        SHAHID("skywars2:shahid_spawn_egg", "shahid_spawn_egg", Items.ZOMBIE_SPAWN_EGG, EntityTypes.ITEM, "lbc:tier_ff", """
                {PickupDelay:32767s,Item:{count:1,id:"minecraft:repeating_command_block",components:{
                 "minecraft:custom_data":{shahid:1,drop:1},"minecraft:item_model":"lbc:drop_trap"}}}
                """),
        HOHOL("skywars2:hohol_spawn_egg", "hohol_spawn_egg", Items.BAT_SPAWN_EGG, EntityTypes.ITEM, "lbc:tier_f", """
                {PickupDelay:32767s,Item:{count:1,id:"minecraft:repeating_command_block",components:{
                 "minecraft:custom_data":{droppertnt:1,drop:1},"minecraft:item_model":"lbc:drop_boss"}}}
                """),
        FAT_SKELETON("skywars2:fat_skeleton_spawn_egg", "fat_skeleton_spawn_egg", Items.SKELETON_SPAWN_EGG, EntityTypes.SKELETON, "lbc:tier_b", """
                {"Tags":["fat_skeleton"],"DeathLootTable":"lbc:fat_skeleton_rewards","PersistenceRequired":true,"CustomNameVisible":true,"CustomName":{"translate":"fat_skeleton","italic":false},"equipment":{"mainhand":{"id":"minecraft:bone","count":1,"components":{"custom_name":{"translate":"omega_bone","italic":false},"minecraft:enchantments":{"minecraft:knockback":1,"vanishing_curse":1}}},"chest":{"id":"minecraft:iron_chestplate","count":1,"components":{"minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false}},"head":{"id":"minecraft:skeleton_skull","count":1,"components":{"minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false}}},"drop_chances":{"mainhand":0.0,"offhand":0.0,"head":0.0,"chest":0.0,"legs":0.0,"feet":0.0},"attributes":[{"id":"minecraft:movement_speed","base":0.6},{"id":"minecraft:armor","base":20},{"id":"minecraft:attack_knockback","base":100},{"id":"minecraft:armor_toughness","base":5}]}
                """),
        LUCKY_PIG("skywars2:lucky_pig_spawn_egg", "lucky_pig_spawn_egg", Items.PIG_SPAWN_EGG, EntityTypes.PIG, "lbc:tier_d", """
                {"CustomNameVisible":true,"DeathLootTable":"lbc:applepig","CustomName":{"text":"Lucky Pig","color":"gold","italic":false}}
                """),
        HONEY_BEE("skywars2:honey_bee_spawn_egg", "honey_bee_spawn_egg", Items.BEE_SPAWN_EGG, EntityTypes.BEE, "lbc:tier_ff", """
                {"DeathLootTable":"lbc:honey","HasNectar":true}
                """),
        STALIN("skywars2:stalin_spawn_egg", "stalin_spawn_egg", Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, EntityTypes.ZOMBIFIED_PIGLIN, "lbc:tier_f", """
                {"Silent":true,"CustomNameVisible":true,"PersistenceRequired":true,"Health":500,"Tags":["stalin","lucky_mob"],"CustomName":{"translate":"stalin","color":"red","italic":false},"equipment":{"mainhand":{"id":"minecraft:carrot_on_a_stick","count":1,"components":{"minecraft:item_model":"lbc:ak47"}},"feet":{"id":"minecraft:leather_boots","count":1,"components":{"minecraft:equippable":{"slot":"feet","asset_id":"lbc:stalin"},"minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false}},"legs":{"id":"minecraft:leather_leggings","count":1,"components":{"minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false}},"chest":{"id":"minecraft:leather_chestplate","count":1,"components":{"minecraft:equippable":{"slot":"chest","asset_id":"lbc:stalin"},"minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false}},"head":{"id":"minecraft:leather_helmet","count":1,"components":{"minecraft:equippable":{"slot":"head","asset_id":"lbc:stalin"},"minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false,"minecraft:unbreakable":{}}}},"drop_chances":{"mainhand":0.0,"offhand":0.0,"head":0.0,"chest":0.0,"legs":0.0,"feet":0.0},"active_effects":[{"id":"minecraft:fire_resistance","amplifier":0,"duration":2000000},{"id":"minecraft:invisibility","amplifier":0,"duration":2000000}],"attributes":[{"id":"minecraft:max_health","base":500},{"id":"minecraft:follow_range","base":100}],"DeathLootTable":"lbc:stalin"}
                """),
        TRANSFORMATOR("skywars2:transformator_spawn_egg", "transformator_spawn_egg", Items.ZOMBIE_SPAWN_EGG, EntityTypes.ZOMBIE, "lbc:tier_ff", """
                {"NoGravity":false,"Silent":true,"Invulnerable":false,"CustomNameVisible":true,"PersistenceRequired":true,"NoAI":true,"Health":500,"IsBaby":true,"Tags":["transformator","lucky_mob"],"CustomName":[{"translate":"transformator","color":"gold","italic":false},{"text":" Nightmare23 Birthday (05.06.2024)","color":"gray"}],"equipment":{"head":{"id":"minecraft:repeating_command_block","count":1,"components":{"minecraft:item_model":"lbc:transformator","minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false}}},"drop_chances":{"head":0.0},"active_effects":[{"id":"minecraft:invisibility","amplifier":1,"duration":2000000,"show_particles":false}],"attributes":[{"id":"minecraft:max_health","base":500},{"id":"minecraft:knockback_resistance","base":100}]}
                """),
        HELICOPTER("skywars2:helicopter_spawn_egg", "helicopter_spawn_egg", Items.SNOW_GOLEM_SPAWN_EGG, EntityTypes.ITEM, "lbc:tier_ff", """
                {"PickupDelay":32767,"Item":{"count":1,"id":"minecraft:repeating_command_block","components":{"minecraft:custom_data":"{helicopter_boss: 1,drop: 1}","minecraft:item_model":"lbc:drop_boss"}}}
                """),
        ANT("skywars2:muravei_spawn_egg", "muravei_spawn_egg", Items.ZOMBIE_SPAWN_EGG, EntityTypes.ZOMBIE, "lbc:tier_f", """
                {"Silent":true,"DeathLootTable":"lbc:ant_rewards","PersistenceRequired":true,"Health":10,"IsBaby":true,"Tags":["muravei","lucky_mob"],"CustomName":{"translate":"muravei"},"equipment":{"head":{"id":"minecraft:repeating_command_block","count":1,"components":{"minecraft:item_model":"lbc:muravei","minecraft:enchantments":{"vanishing_curse":1},"minecraft:enchantment_glint_override":false}}},"active_effects":[{"id":"minecraft:invisibility","amplifier":1,"duration":-1,"show_particles":false}],"attributes":[{"id":"minecraft:max_health","base":10},{"id":"minecraft:follow_range","base":50},{"id":"minecraft:movement_speed","base":0.25},{"id":"minecraft:attack_damage","base":3},{"id":"minecraft:armor","base":5},{"id":"minecraft:armor_toughness","base":5},{"id":"minecraft:spawn_reinforcements","base":100}]}
                """);

        private final Item item;
        private final EntityType<?> entityType;
        private final String nameKey;
        private final String spawnerMark;
        Kind(String id, String name, Item proxy, EntityType<?> type, String tier, String payload) {
            this.entityType = type;
            this.nameKey = name;
            this.spawnerMark = SPAWNER_MARK + name;
            var key=ResourceKey.create(Registries.ITEM, Identifier.parse(id));
            var flags=new CompoundTag(); flags.putInt("imba",1);
            item=Registry.register(BuiltInRegistries.ITEM,key,new Egg(new Item.Properties() {
                    // Item finalization writes effectiveModel after explicit components.
                    @Override public Identifier effectiveModel() { return BuiltInRegistries.ITEM.getKey(proxy); }
                }.setId(key)
                    .component(DataComponents.CUSTOM_NAME,Component.translatable(name).withStyle(ChatFormatting.RED).withStyle(s->s.withItalic(false)))
                    .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("imba_version")
                            .withStyle(ChatFormatting.DARK_PURPLE).withStyle(s->s.withItalic(false)))))
                    .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(tier))
                    .component(DataComponents.CUSTOM_DATA,CustomData.of(flags))
                    .component(DataComponents.ENTITY_DATA,TypedEntityData.of(type,parse(payload))),proxy));
        }
        public Item item() { return item; }
        private static CompoundTag parse(String payload) {
            try { return TagParser.parseCompoundFully(payload); }
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new ExceptionInInitializerError(error); }
        }
    }
    public static final Item SLAB=Kind.SLAB.item(), LOW_VEX=Kind.LOW_VEX.item(),
            SARVATRA_CREEPER=Kind.SARVATRA_CREEPER.item(), SHAHID=Kind.SHAHID.item();
    private static final Map<String, Kind> AUTHORED_NAMES = EnumSet.allOf(Kind.class).stream()
            .collect(Collectors.toUnmodifiableMap(kind -> kind.nameKey, Function.identity()));
    private static final Map<String, Kind> SPAWNER_KINDS = EnumSet.allOf(Kind.class).stream()
            .collect(Collectors.toUnmodifiableMap(kind -> kind.spawnerMark, Function.identity()));
    private LbcSpecialEggs() { }
    public static void register() { }
    public static Kind kind(ItemStack stack) {
        for(var kind:Kind.values()) if(stack.is(kind.item())) return kind;
        if (!(stack.getItem() instanceof SpawnEggItem)) return null;
        var name = stack.get(DataComponents.CUSTOM_NAME);
        var data = stack.get(DataComponents.ENTITY_DATA);
        if (name == null || !(name.getContents() instanceof TranslatableContents translated) || data == null
                || ItemData.intValue(stack, "imba", 0) != 1) return null;
        var authored = AUTHORED_NAMES.get(translated.getKey());
        return authored != null && authored.entityType == data.type() && legacyPayload(authored, data.getUnsafe())
                ? authored : null;
    }

    private static boolean legacyPayload(Kind kind, CompoundTag data) {
        var tags = data.getListOrEmpty("Tags");
        var loot = data.getStringOr("DeathLootTable", "");
        var equipment = data.getCompoundOrEmpty("equipment");
        var main = equipment.getCompoundOrEmpty("mainhand").getCompoundOrEmpty("components");
        var head = equipment.getCompoundOrEmpty("head");
        var headModel = head.getCompoundOrEmpty("components").getStringOr("minecraft:item_model", "");
        var item = data.getCompoundOrEmpty("Item").getStringOr("id", "");
        return switch (kind) {
            case STALIN -> tags.contains(StringTag.valueOf("stalin")) && tags.contains(StringTag.valueOf("lucky_mob"))
                    && loot.equals("lbc:stalin") && main.getStringOr("minecraft:item_model", "").equals("lbc:ak47");
            case TRANSFORMATOR -> tags.contains(StringTag.valueOf("transformator")) && tags.contains(StringTag.valueOf("lucky_mob"))
                    && data.getBooleanOr("NoAI", false) && headModel.equals("lbc:transformator");
            case ANT -> tags.contains(StringTag.valueOf("muravei")) && tags.contains(StringTag.valueOf("lucky_mob"))
                    && (loot.equals("lbc:muravei") || loot.equals("lbc:ant_rewards")) && headModel.equals("lbc:muravei");
            case FAT_SKELETON -> tags.contains(StringTag.valueOf("fat_skeleton"))
                    && (loot.equals("lbc:fatskeleton") || loot.equals("lbc:fat_skeleton_rewards"));
            case SLAB -> data.getBooleanOr("IsBaby", false) && loot.equals("minecraft:empty")
                    && head.getStringOr("id", "").equals("minecraft:oak_slab");
            case LOW_VEX -> loot.equals("lbc:vex_sword") && data.getFloatOr("Health", 0) == 1;
            case SARVATRA_CREEPER -> loot.equals("lbc:creeper_wand") && data.getIntOr("ExplosionRadius", 0) == 1;
            case LUCKY_PIG -> loot.equals("lbc:applepig");
            case HONEY_BEE -> loot.equals("lbc:honey") && data.getBooleanOr("HasNectar", false);
            case HELICOPTER, SHAHID, HOHOL -> item.equals("minecraft:repeating_command_block");
            case MOM -> item.equals("skywars2:mom_drop");
            case OTCHIM -> item.equals("skywars2:otchim_drop");
        };
    }

    public static TypedEntityData<EntityType<?>> spawnerData(ItemStack stack, TypedEntityData<EntityType<?>> data,
                                                             HolderLookup.Provider registries) {
        data = LbcFakePlayer.spawnerData(stack, data, registries);
        Kind kind = kind(stack);
        if (kind == null || kind.entityType != data.type()) return data;
        var payload = data.copyTagWithoutId();
        var tags = payload.getListOrEmpty("Tags");
        var marker = StringTag.valueOf(kind.spawnerMark);
        if (!tags.contains(marker)) tags.add(marker);
        payload.put("Tags", tags);
        return TypedEntityData.of(data.type(), payload);
    }

    /** The spawner has no egg item or actor. The marker is consumed only after native publication. */
    public static void adoptFromSpawner(GameController game, Entity entity) {
        if (!activeArena(game, entity) || entity.entityTags().isEmpty()) return;
        for (var tag = entity.entityTags().iterator(); tag.hasNext();) {
            Kind kind = SPAWNER_KINDS.get(tag.next());
            if (kind == null || kind.entityType != entity.getType()) continue;
            tag.remove();
            adoptKind(game, entity, null, kind);
            return;
        }
        Kind legacy = legacyLoadedKind(entity);
        if (legacy != null) adoptKind(game, entity, null, legacy);
    }

    private static boolean activeArena(GameController game, Entity entity) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && entity.level() == game.maps().level() && LbcDimensionItems.ordinary(entity.level());
    }

    private static Kind legacyLoadedKind(Entity entity) {
        if (!(entity instanceof Mob mob)) return null;
        var tags = mob.entityTags();
        if (mob.getType() == EntityTypes.SKELETON && tags.contains("fat_skeleton") && !LbcCreatureDrops.managed(mob)
                && hasLoot(mob, "lbc:fatskeleton", "lbc:fat_skeleton_rewards")) return Kind.FAT_SKELETON;
        if (!tags.contains("lucky_mob")) return null;
        if (mob.getType() == EntityTypes.ZOMBIFIED_PIGLIN && tags.contains("stalin") && !LbcStalin.managed(mob)
                && hasLoot(mob, "lbc:stalin") && model(mob, EquipmentSlot.MAINHAND, "lbc:ak47")) return Kind.STALIN;
        if (mob.getType() != EntityTypes.ZOMBIE) return null;
        if (tags.contains("transformator") && !LbcTransformator.managed(mob) && mob.isNoAi()
                && model(mob, EquipmentSlot.HEAD, "lbc:transformator")) return Kind.TRANSFORMATOR;
        if (tags.contains("muravei") && !LbcAntDrops.managed(mob) && hasLoot(mob, "lbc:muravei", "lbc:ant_rewards")
                && model(mob, EquipmentSlot.HEAD, "lbc:muravei")) return Kind.ANT;
        return null;
    }

    private static boolean hasLoot(Mob mob, String... tables) {
        var key = mob.getLootTable().orElse(null);
        if (key == null) return false;
        String id = key.identifier().toString();
        for (var table : tables) if (table.equals(id)) return true;
        return false;
    }

    private static boolean model(Mob mob, EquipmentSlot slot, String expected) {
        var model = mob.getItemBySlot(slot).get(DataComponents.ITEM_MODEL);
        return model != null && model.toString().equals(expected);
    }
    public static boolean matches(ItemStack stack) { return kind(stack)!=null; }
    public static boolean allowed(GameController game,Player user) {
        return game!=null && user instanceof ServerPlayer player && game.allowDamage(player)
                && !game.isObserver(player) && player.level()==game.maps().level()
                && !game.items().actionsBlocked(player) && !game.items().timeStop().stopped(player.level());
    }
    public static final class Egg extends SpawnEggItem implements PolymerItem {
        private final Item proxy;
        public Egg(Item.Properties properties,Item proxy) { super(properties); this.proxy=proxy; }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context) { return proxy; }
        @Override public InteractionResult useOn(UseOnContext context) {
            if(!allowed(SkyWars2.game(),context.getPlayer()) || !(context.getLevel() instanceof ServerLevel level))
                return InteractionResult.FAIL;
            // Admission must not turn an invalid remote use into a synchronous chunk load.
            var clicked=context.getClickedPos();
            if(!resident(level,clicked)) return InteractionResult.FAIL;
            if(level.getBlockEntity(clicked) instanceof net.minecraft.world.level.Spawner) return super.useOn(context);
            var state=level.getBlockState(clicked);
            var spawn=state.getCollisionShape(level,clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
            if(!resident(level,spawn)) return InteractionResult.FAIL;
            return super.useOn(context);
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            return allowed(SkyWars2.game(),player) ? super.use(level,player,hand) : InteractionResult.FAIL;
        }
        private boolean resident(ServerLevel level,net.minecraft.core.BlockPos pos) {
            return !level.isOutsideBuildHeight(pos) && level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
        }
    }

    /** Called after the native ENTITY_DATA processor, while the actor is still outside the world. */
    public static void adopt(GameController game,Entity entity,LivingEntity user,ItemStack stack) {
        LbcFakePlayer.adopt(entity,user,stack);
        if(game!=null){game.items().balance().adoptEgg(entity,user);LbcImbaFollowup.adopt(game,entity,user,stack);LbcImbaPlaceableItems.adopt(game,entity,user instanceof ServerPlayer player?player:null,stack);}
        if (game != null) game.items().grimoireRooms().captureMonumentAuthor(entity, user);
        var kind=kind(stack);
        if(game==null || kind==null || kind.entityType != entity.getType()) return;
        if (!stack.is(kind.item()) && !activeArena(game, entity)) return;
        adoptKind(game, entity, user, kind);
        restoreAuthoredName(entity,stack);
    }

    private static void adoptKind(GameController game, Entity entity, LivingEntity user, Kind kind) {
        var author=user instanceof ServerPlayer player ? player : null;
        switch(kind) {
            case SLAB -> { if(entity instanceof Zombie zombie) game.items().slabDrops().adoptEgg(zombie,author); }
            case LOW_VEX -> { if(entity instanceof Vex vex) game.items().creatureDrops().adoptEggVex(vex,author); }
            case SARVATRA_CREEPER -> { if(entity instanceof Creeper creeper) game.items().creeperDrops().adoptEgg(creeper,user); }
            case FAT_SKELETON -> { if(entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton actor) game.items().creatureDrops().adoptEggSkeleton(actor,author); }
            case LUCKY_PIG -> { if(entity instanceof net.minecraft.world.entity.animal.pig.Pig actor) game.items().luckyPigs().adoptEgg(actor,user); }
            case HONEY_BEE -> { if(entity instanceof net.minecraft.world.entity.animal.bee.Bee actor) game.items().beeDrops().adoptEgg(actor,author); }
            case STALIN -> { if(entity instanceof net.minecraft.world.entity.monster.zombie.ZombifiedPiglin actor) game.items().stalin().adoptEgg(actor,user); }
            case TRANSFORMATOR -> { if(entity instanceof Zombie actor) game.items().transformator().adoptEgg(actor,user); }
            case ANT -> { if(entity instanceof Zombie actor) game.items().antDrops().adoptEgg(actor,author); }
            case HELICOPTER -> {
                if(entity instanceof ItemEntity token) {
                    if(user!=null) token.setThrower(user);
                    game.items().markCurrentItemEpoch(token);game.items().helicopterEncounter().loaded(token);
                }
            }
            case SHAHID,HOHOL,MOM,OTCHIM -> {
                if(entity instanceof ItemEntity token) {
                    if(user!=null) token.setThrower(user);
                    game.items().specialDrops().loaded(token);
                }
            }
        }
    }
    /** ENTITY_DATA recursively merges compounds; an omitted color must not inherit the red egg title. */
    private static void restoreAuthoredName(Entity entity,ItemStack stack) {
        var data=stack.get(DataComponents.ENTITY_DATA);
        if(data==null || data.type()!=entity.getType() || !data.contains("CustomName")) return;
        var input=net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
                entity.registryAccess(),data.copyTagWithoutId());
        input.read("CustomName",net.minecraft.network.chat.ComponentSerialization.CODEC).ifPresent(entity::setCustomName);
    }
}
