package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.List;

/** Source eggs retain vanilla placement, finalizeSpawn, fluid use and dispenser spawning. */
public final class LbcSpecialEggs {
    public enum Kind {
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
        Kind(String id, String name, Item proxy, EntityType<?> type, String tier, String payload) {
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
    private LbcSpecialEggs() { }
    public static void register() { }
    public static Kind kind(ItemStack stack) {
        for(var kind:Kind.values()) if(stack.is(kind.item())) return kind;
        return null;
    }
    public static boolean matches(ItemStack stack) { return kind(stack)!=null; }
    public static boolean allowed(GameController game,Player user) {
        return game!=null && user instanceof ServerPlayer player && game.allowDamage(player)
                && !game.isObserver(player) && player.level()==game.maps().level()
                && !game.items().actionsBlocked(player) && !game.items().timeStop().stopped(player.level());
    }
    private static final class Egg extends SpawnEggItem implements PolymerItem {
        private final Item proxy;
        Egg(Item.Properties properties,Item proxy) { super(properties); this.proxy=proxy; }
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
        var kind=kind(stack);
        if(game==null || kind==null) return;
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
            case SHAHID,HOHOL -> {
                if(entity instanceof ItemEntity token) {
                    if(user!=null) token.setThrower(user);
                    game.items().specialDrops().loaded(token);
                }
            }
        }
        restoreAuthoredName(entity,stack);
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
