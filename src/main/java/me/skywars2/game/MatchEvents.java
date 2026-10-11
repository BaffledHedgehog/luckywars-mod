package me.skywars2.game;

import static me.skywars2.SkyWars2.text;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

/** Owns the timed phase events for one ordinary SkyWars match. */
public final class MatchEvents {
    private static final int TICKS_PER_SECOND = 20;
    private static final TagKey<Item> SKILL_FOOD = TagKey.create(Registries.ITEM, Identifier.parse("swrg:food"));
    private static final Map<Integer, List<String>> KIT_COMMANDS = Map.ofEntries(
            Map.entry(1, List.of("minecraft:tnt 8", "minecraft:flint_and_steel", "minecraft:golden_chestplate[enchantments={\"minecraft:blast_protection\":100}]")),
            Map.entry(2, List.of("minecraft:wolf_spawn_egg 2", "minecraft:bone 16")),
            Map.entry(3, List.of("minecraft:splash_potion[potion_contents={potion:\"minecraft:strong_healing\"}]", "minecraft:splash_potion[potion_contents={potion:\"minecraft:swiftness\"}]", "minecraft:splash_potion[potion_contents={potion:\"minecraft:regeneration\"}]")),
            Map.entry(4, List.of("minecraft:wooden_sword[enchantments={\"minecraft:knockback\":1}]", "minecraft:leather_chestplate[enchantments={\"minecraft:fire_protection\":1,\"minecraft:blast_protection\":1,\"minecraft:projectile_protection\":1}]")),
            Map.entry(5, List.of("minecraft:golden_apple", "minecraft:bread 8", "minecraft:golden_carrot 2")),
            Map.entry(6, List.of("minecraft:chainmail_chestplate[enchantments={\"minecraft:thorns\":4}]")),
            Map.entry(7, List.of("minecraft:golden_chestplate[enchantments={\"minecraft:protection\":4},attribute_modifiers=[{id:\"34f9eab4-c40b-4ae2-be4e-501ae2e4d57a\",type:\"minecraft:knockback_resistance\",amount:3,operation:\"add_value\",slot:\"chest\"},{id:\"2b4d9ea4-5855-4792-90d8-0338c0e6492b\",type:\"minecraft:armor\",amount:5,operation:\"add_value\",slot:\"chest\"}]]")),
            Map.entry(8, List.of("minecraft:splash_potion[potion_contents={potion:\"minecraft:poison\"}]", "minecraft:cobweb 7", "minecraft:string 64")),
            Map.entry(9, List.of("minecraft:gilded_blackstone 16", "minecraft:gravel 16", "minecraft:gold_ingot 16", "minecraft:potion[potion_contents={potion:\"minecraft:fire_resistance\"}]")),
            Map.entry(10, List.of("minecraft:shield[max_damage=25]")),
            Map.entry(11, List.of("minecraft:enchanting_table", "minecraft:bookshelf 6", "minecraft:lapis_lazuli 20")),
            Map.entry(12, List.of("minecraft:splash_potion[potion_contents={potion:\"minecraft:poison\"}]", "minecraft:milk_bucket 2")),
            Map.entry(13, List.of("minecraft:golden_pickaxe", "minecraft:golden_axe")),
            Map.entry(14, List.of("minecraft:gunpowder 32", "minecraft:paper 32", "minecraft:red_dye 32", "minecraft:fire_charge 12", "minecraft:crossbow[minecraft:max_damage=165]")),
            Map.entry(15, List.of("minecraft:potion[potion_contents={potion:\"minecraft:invisibility\"}]")),
            Map.entry(16, List.of("minecraft:stone_sword[enchantments={\"minecraft:fire_aspect\":2}]", "minecraft:potion[potion_contents={potion:\"minecraft:long_fire_resistance\"}]", "minecraft:lava_bucket", "minecraft:flint_and_steel")),
            Map.entry(17, List.of("minecraft:cake 2", "minecraft:slime_block 8", "minecraft:honey_block 8", "minecraft:firework_rocket[fireworks={explosions:[{shape:\"large_ball\",colors:[16740583],fade_colors:[7264511],has_trail:1b,has_twinkle:1b}],flight_duration:2}]", "minecraft:golden_sword")),
            Map.entry(20, List.of("minecraft:golden_sword[enchantments={\"minecraft:fire_aspect\":1}]", "minecraft:golden_helmet", "minecraft:golden_chestplate", "minecraft:golden_leggings", "minecraft:golden_boots", "minecraft:gold_ore 10", "minecraft:golden_apple")),
            Map.entry(21, List.of("minecraft:oak_slab[enchantments={\"minecraft:sharpness\":5}]", "minecraft:cobblestone_slab 16")),
            Map.entry(22, List.of()),
            Map.entry(23, List.of("minecraft:mace[enchantments={\"minecraft:wind_burst\":1},attribute_modifiers=[{amount:3,id:\"minecraft:base_attack_damage\",slot:\"mainhand\",operation:\"add_value\",type:\"minecraft:attack_damage\"},{amount:-2.4,id:\"minecraft:base_attack_speed\",slot:\"mainhand\",operation:\"add_value\",type:\"minecraft:attack_speed\"}]]")),
            Map.entry(24, List.of("minecraft:wind_charge[max_stack_size=99] 12")));

    private final GameController controller;
    private final MapManager maps;
    private final GameConfig config;
    private final PlayerStore players;
    private final Map<UUID, SkillState> skillStates = new HashMap<>();
    private final PriorityQueue<SkillState> pendingVex = new PriorityQueue<>(
            java.util.Comparator.comparingInt(state -> state.vexTick));
    private int nextWitherTick = -1;
    private int nextTntTick = -1;
    private boolean running;

    private static final class SkillState {
        final int skill;
        final ServerPlayer owner;
        final Set<Vex> vexes = new HashSet<>();
        int vexTick = -1;
        SkillState(int skill, ServerPlayer owner) { this.skill = skill; this.owner = owner; }
    }

    /** Vanilla entity ID/AI; only ownership, friendly targets and match lifetime differ. */
    private final class SkillVex extends Vex {
        private final SkillState state;
        SkillVex(SkillState state) {
            super(EntityTypes.VEX, state.owner.level());
            this.state = state;
        }
        @Override public boolean canAttack(LivingEntity target) {
            return state != null && activeSkillOwner(state.owner) && state.skill == 13
                    && skillStates.get(state.owner.getUUID()) == state && players.get(state.owner.getUUID()).skill == 13
                    && target != state.owner && !state.owner.isAlliedTo(target)
                    && (!(target instanceof ServerPlayer player) || controller.allowDamage(player))
                    && super.canAttack(target);
        }
        @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
            return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level, target);
        }
        // Reloading as a plain vanilla Vex would lose the Java owner/target guard.
        @Override public boolean shouldBeSaved() { return false; }
    }

    public MatchEvents(GameController controller, MapManager maps, GameConfig config, PlayerStore players) {
        this.controller = controller;
        this.maps = maps;
        this.config = config;
        this.players = players;
    }

    /** Activates the repeat-event runner for a new match. Stage timing belongs to the controller. */
    public void start() {
        stop();
        running = true;
        skillStates.clear();
    }

    /** Starts a phase-one event; 0 disables it, 1 picks randomly, 2..9 select 1..8. */
    public void stageOne(int selection) {
        if (!running || !controller.playing()) return;
        int event = resolve(selection, 8);
        if (event == 0) return;
        controller.command("execute as @a at @s run playsound minecraft:item.shield.break ambient @s ~ ~ ~");
        controller.announce(stageAnnouncement(true, event));
        switch (event) {
            case 1 -> effect("minecraft:speed", 1, -1);
            case 2 -> effect("minecraft:haste", 1, -1);
            case 3 -> refillChests(false, false);
            case 4 -> effect("minecraft:jump_boost", 3, -1);
            case 5 -> effect("minecraft:health_boost", 4, -1);
            case 6 -> refillChests(true, true);
            case 7 -> effect("minecraft:invisibility", 0, 300);
            case 8 -> effect("minecraft:glowing", 0, -1);
            default -> { }
        }
    }

    /** Starts a phase-two event; 0 disables it, 1 picks randomly, 2..8 select 1..7. */
    public void stageTwo(int selection) {
        if (!running || !controller.playing()) return;
        int event = resolve(selection, 7);
        if (event == 0) return;
        controller.command("execute as @a at @s run playsound minecraft:entity.ender_dragon.ambient player @s ~ ~ ~ 1");
        controller.announce(stageAnnouncement(false, event));
        switch (event) {
            case 1 -> summonAbovePlayers("minecraft:wither", 10);
            case 2 -> summonAbovePlayers("minecraft:ender_dragon", 20);
            case 3 -> {
                applyWither();
                nextWitherTick = controller.matchTicks() + TICKS_PER_SECOND;
            }
            case 4 -> {
                dropTnt();
                nextTntTick = controller.matchTicks() + 5 * TICKS_PER_SECOND;
            }
            case 5 -> shrinkBorder();
            case 6 -> effect("minecraft:levitation", 0, 30);
            case 7 -> dropCenterChests();
            default -> { }
        }
    }

    /** Runs due stage and repeat events. Call once per server tick. */
    public void tick() {
        if (!running) return;
        if (!controller.playing()) {
            stop();
            return;
        }
        int now = controller.matchTicks();
        if (nextWitherTick >= 0 && now >= nextWitherTick) {
            applyWither();
            nextWitherTick = now + TICKS_PER_SECOND;
        }
        if (nextTntTick >= 0 && now >= nextTntTick) {
            dropTnt();
            nextTntTick = now + 5 * TICKS_PER_SECOND;
        }
        tickVexSummons(now);
        if (config.vanillaSkills) {
            for (ServerPlayer player : controller.alivePlayers()) tickSkill(player);
        }
    }

    /** Cancels every delayed/repeating action immediately. */
    public void stop() {
        running = false;
        nextWitherTick = -1;
        nextTntTick = -1;
        pendingVex.clear();
        for (SkillState state : skillStates.values()) discardVexes(state);
        skillStates.clear();
    }

    /** Gives only the player's selected vanilla kit; an unset/unsupported kit is empty. */
    public void giveKit(ServerPlayer player) {
        if (!config.vanillaKits) return;
        PlayerStore.Profile profile = players.get(player.getUUID());
        if (profile == null || profile.kit <= 0) return;
        String target = player.getScoreboardName();
        for (String item : KIT_COMMANDS.getOrDefault(profile.kit, List.of())) {
            issue("give " + target + " " + item, player);
        }
        if (profile.kit == 11) issue("experience add " + target + " 20 levels", player);
        if (profile.kit == 12 || profile.kit == 18 || profile.kit == 22) {
            issue("loot give " + target + " loot swrg:game/kit/" + (profile.kit == 22 ? "halloween_pies" : profile.kit), player);
        }
    }

    /** Records the selected skill for a fresh spawn or respawn. */
    public void giveSkill(ServerPlayer player) {
        removePlayerSkill(player);
        if (!config.vanillaSkills) {
            return;
        }
        int skill = players.get(player.getUUID()).skill;
        skillStates.put(player.getUUID(), new SkillState(skill, player));
    }

    public void removePlayerSkill(ServerPlayer player) {
        SkillState previous = skillStates.remove(player.getUUID());
        if (previous != null) discardVexes(previous);
    }

    private static void discardVexes(SkillState state) {
        // ENTITY_UNLOAD also removes entries, so discard a snapshot.
        for (Vex vex : List.copyOf(state.vexes)) vex.discard();
        state.vexes.clear();
    }

    public void entityUnloaded(Entity entity) {
        if (entity instanceof SkillVex vex) vex.state.vexes.remove(vex);
    }

    private boolean activeSkillOwner(ServerPlayer player) {
        return running && config.vanillaSkills && controller.playing() && player.isAlive()
                && !player.isSpectator() && controller.allowDamage(player)
                && controller.server().getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private SkillState skillState(ServerPlayer player) {
        int selected = players.get(player.getUUID()).skill;
        SkillState state = skillStates.get(player.getUUID());
        if (state == null || state.skill != selected || state.owner != player) {
            giveSkill(player);
            state = skillStates.get(player.getUUID());
        }
        return state;
    }

    public void onLanding(ServerPlayer player, double distance) {
        if (!activeSkillOwner(player) || players.get(player.getUUID()).skill != 2 || !Double.isFinite(distance)) return;
        long centimeters = Math.round(distance * 100.0);
        if (centimeters < 335) return;
        float damage = (float) (Math.min(centimeters, 3999) * 0.00915);
        var level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
        // Source affects all entities at 0.1..6, including allies; it has no player attacker attribution.
        for (Entity target : level.getEntities(player, player.getBoundingBox().inflate(6), entity -> {
            double squared = player.distanceToSqr(entity);
            return squared >= 0.01 && squared <= 36;
        })) target.hurtServer(level, level.damageSources().fall(), damage);
    }

    public void onConsumed(ServerPlayer player, ItemStack consumed) {
        if (!activeSkillOwner(player) || players.get(player.getUUID()).skill != 4 || !consumed.is(SKILL_FOOD)) return;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false));
        if (chance(0.50)) player.removeEffect(MobEffects.HUNGER);
    }

    /** HurtTime may begin before or after the player's tick; check only pending hits near tick five. */
    public void onPlayerHurt(ServerPlayer player) {
        if (!activeSkillOwner(player) || players.get(player.getUUID()).skill != 13 || player.hurtTime < 5) return;
        SkillState state = skillState(player);
        if (state.vexTick >= 0) return;
        state.vexTick = controller.matchTicks() + Math.max(1, player.hurtTime - 6);
        pendingVex.add(state);
    }

    private void tickVexSummons(int now) {
        while (!pendingVex.isEmpty() && pendingVex.peek().vexTick <= now) {
            SkillState state = pendingVex.remove();
            state.vexTick = -1;
            ServerPlayer owner = state.owner;
            if (skillStates.get(owner.getUUID()) != state || !activeSkillOwner(owner)
                    || players.get(owner.getUUID()).skill != 13) continue;
            if (owner.hurtTime > 5) {
                state.vexTick = now + 1;
                pendingVex.add(state);
                continue;
            }
            if (owner.hurtTime != 5 || !chance(0.50)) continue;
            SkillVex vex = new SkillVex(state);
            vex.setPos(owner.getX(), owner.getY() + 1, owner.getZ());
            vex.setHealth(0.1F);
            var followRange = vex.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) followRange.setBaseValue(50);
            if (maps.level().addFreshEntity(vex)) state.vexes.add(vex);
            maps.level().sendParticles(ParticleTypes.CLOUD, owner.getX(), owner.getY() + 1, owner.getZ(),
                    3, 0.3, 0.6, 0.3, 0);
        }
    }

    /** Call from the server's successful living-entity damage callback. */
    public void onPlayerDamaged(ServerPlayer victim, DamageSource source, float damage) {
        if (!running || !config.vanillaSkills || damage <= 0 || !controller.playing()) return;
        int victimSkill = players.get(victim.getUUID()).skill;
        Entity sourceEntity = source.getEntity();
        if (victimSkill == 1 && sourceEntity instanceof ServerPlayer attacker
                && attacker != victim && victim.distanceTo(attacker) >= 0.1F) {
            giveEffect(attacker, "minecraft:poison", 0, 3);
        }
        if (victimSkill == 10 && chance(0.20) && controller.alivePlayers().stream()
                .anyMatch(other -> other != victim && victim.distanceTo(other) >= 0.1F && victim.distanceTo(other) <= 7.0F)) {
            lightning(victim);
        }
    }

    /** Call for the attacker from the same successful-damage callback. */
    public void onPlayerAttack(ServerPlayer attacker, ServerPlayer victim, DamageSource source, float damage) {
        if (!running || !config.vanillaSkills || damage <= 0 || !controller.playing() || attacker == victim) return;
        int skill = players.get(attacker.getUUID()).skill;
        switch (skill) {
            case 6 -> {
                if (source.getDirectEntity() instanceof AbstractArrow && damage >= 3.0F) {
                    attacker.getInventory().add(new ItemStack(Items.ARROW));
                    if (chance(0.10)) attacker.getInventory().add(new ItemStack(Items.ARROW));
                }
            }
            case 9 -> giveEffect(victim, "minecraft:darkness", 0, 7);
            case 10 -> {
                if (victim.distanceTo(attacker) <= 20 && chance(0.50)) lightning(attacker);
            }
            case 11 -> {
                giveEffect(attacker, "minecraft:regeneration", 1, 5);
                if (chance(0.25)) giveEffect(attacker, "minecraft:instant_health", 0, 1);
            }
            default -> { }
        }
    }

    private void tickSkill(ServerPlayer player) {
        PlayerStore.Profile profile = players.get(player.getUUID());
        int skill = profile.skill;
        skillState(player);
        if (skill == 2 || skill == 4 || skill == 13) return;

        float health = player.getHealth();
        switch (skill) {
            case 3 -> {
                if (player.hurtTime == 10) giveEffect(player, "minecraft:speed", 1, 5);
            }
            case 5 -> {
                if (health <= 10.0F) giveEffect(player, "minecraft:resistance", 3, 3);
                else if (health <= 15.0F) giveEffect(player, "minecraft:resistance", 1, 3);
            }
            case 7 -> {
                if (health <= 10.0F) giveEffect(player, "minecraft:strength", 2, 3);
                else if (health <= 15.0F) giveEffect(player, "minecraft:strength", 0, 3);
            }
            case 8 -> {
                if (player.getRemainingFireTicks() > 0) {
                    giveEffect(player, "minecraft:strength", 0, 2);
                    giveEffect(player, "minecraft:resistance", 0, 2);
                }
            }
            case 12 -> {
                if (player.isSprinting() && !player.onGround()) {
                    float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
                    double forward = 0.1;
                    player.setDeltaMovement(player.getDeltaMovement().add(-Math.sin(yaw) * forward, -0.01, Math.cos(yaw) * forward));
                }
            }
            default -> { }
        }
    }

    private void giveEffect(ServerPlayer player, String effect, int amplifier, int seconds) {
        controller.command("effect give " + player.getScoreboardName() + " " + effect + " " + seconds + " " + amplifier + " true");
    }

    private static Component stageAnnouncement(boolean firstStage, int event) {
        String key =
                firstStage
                        ? switch (event) {
                            case 1 -> "swrg.title.stage1.event1";
                            case 2 -> "swrg.title.stage1.event2";
                            case 3 -> "swrg.title.stage1.event3";
                            case 4 -> "swrg.title.stage1.event4";
                            case 5 -> "swrg.title.stage1.event5";
                            case 6 -> "swrg.title.stage1.event6";
                            case 7 -> "swrg.title.stage1.event7";
                            default -> "swrg.title.stage1.event8";
                        }
                        : switch (event) {
                            case 1 -> "swrg.title.stage2.event1";
                            case 2 -> "swrg.title.stage2.event2";
                            case 3 -> "swrg.title.stage2.event3";
                            case 4 -> "swrg.title.stage2.event4";
                            case 5 -> "swrg.title.stage2.event5";
                            case 6 -> "swrg.title.stage2.event6";
                            default -> "swrg.title.stage2.event7";
                        };
        return Component.translatable(key)
                .withStyle(firstStage ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED);
    }

    private static boolean chance(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    private void lightning(ServerPlayer at) {
        Vec3 position = at.position();
        controller.command("summon minecraft:lightning_bolt " + position.x + " " + position.y + " " + position.z);
    }

    private int resolve(int selection, int eventCount) {
        if (selection == 0) return 0;
        if (selection == 1) return ThreadLocalRandom.current().nextInt(1, eventCount + 1);
        int event = selection - 1;
        return event >= 1 && event <= eventCount ? event : 0;
    }

    private void issue(String command, ServerPlayer player) {
        if (!controller.command(command)) {
            controller.announce(text("game.give_failed", player.getDisplayName(), command));
        }
    }

    private void effect(String effect, int amplifier, int duration) {
        for (ServerPlayer player : controller.alivePlayers()) {
            String time = duration < 0 ? "infinite" : Integer.toString(duration);
            controller.command("effect give " + player.getScoreboardName() + " " + effect + " " + time + " " + amplifier + " true");
        }
    }

    private void applyWither() {
        effect("minecraft:wither", 0, 2);
    }

    private void refillChests(boolean upgrade, boolean moveToCenter) {
        for (String tag : new String[] {"swrg.default", "swrg.midle", "swrg.center"}) {
            String selector = "@e[type=minecraft:marker,tag=" + tag + "]";
            if (moveToCenter && !tag.equals("swrg.center")) {
                controller.command("tag " + selector + " add swrg.center");
            }
            controller.command("execute as " + selector + " at @s run data remove block ~ ~ ~ Items");
        }
        maps.fillChests(upgrade);
    }

    private void summonAbovePlayers(String entity, int height) {
        for (ServerPlayer player : controller.alivePlayers()) {
            Vec3 pos = player.position();
            String data = entity.equals("minecraft:ender_dragon") ? " {DragonPhase:8}" : "";
            controller.command("summon " + entity + " " + pos.x + " " + (pos.y + height) + " " + pos.z + data);
        }
    }

    private void dropTnt() {
        for (ServerPlayer player : controller.alivePlayers()) {
            Vec3 pos = player.position();
            for (int i = 0; i < 3; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
                double radius = ThreadLocalRandom.current().nextDouble(0.0, 190.0);
                double x = pos.x + Math.cos(angle) * radius;
                double z = pos.z + Math.sin(angle) * radius;
                controller.command("summon minecraft:tnt " + x + " 110 " + z + " {fuse:100s}");
            }
        }
    }

    private void shrinkBorder() {
        List<ServerPlayer> alive = controller.alivePlayers();
        if (alive.isEmpty()) return;
        ServerPlayer center = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
        maps.level().getWorldBorder().setCenter(center.getX(), center.getZ());
        maps.level().getWorldBorder().setSize(384.0);
        maps.level().getWorldBorder().lerpSizeBetween(384.0, 4.0, maps.level().getGameTime(), 4000L * TICKS_PER_SECOND);
    }

    private void dropCenterChests() {
        List<MapManager.Spawn> spawns = maps.spawns();
        double centerX = spawns.stream().mapToDouble(spawn -> spawn.position().x).average().orElse(0.0);
        double centerZ = spawns.stream().mapToDouble(spawn -> spawn.position().z).average().orElse(0.0);
        for (int i = 0; i < 10; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
            double radius = ThreadLocalRandom.current().nextDouble(0.0, 192.0);
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            controller.command("summon minecraft:chest_minecart " + x + " 110 " + z + " {Glowing:1b,DropContentsWhenDead:1b,LootTable:\"swrg:chests/center_0\"}");
        }
    }
}
