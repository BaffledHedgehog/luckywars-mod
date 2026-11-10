package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.*;

/**
 * Actual native ant insertion/combat/rewards and shared acid
 * movement/effect/lifecycle fixtures.
 */
public final class AcidAntDropChecks {
    private AcidAntDropChecks() {
    }

    private static State running, stopping, retained;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Entity> stopActors = List.of();
    private static final Set<Entity> stopBaseline = Collections.newSetFromMap(new IdentityHashMap<>());

    private static void require(boolean ok, String text) {
        if (!ok)
            throw new IllegalStateException("Acid/ant drops: " + text);
    }

    private static CompoundTag saved(Entity e, ServerLevel level) {
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        e.saveWithoutId(out);
        return out.buildResult();
    }

    private static final class State implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer alpha, beta;
        final GameChecks.WandThirdPlayerState alphaState, betaState;
        final Vec3 origin = new Vec3(-400.63, 150.7, -400.19);
        final BlockPos center = BlockPos.containing(origin);
        final Set<Entity> existing = Collections.newSetFromMap(new IdentityHashMap<>()),
                mine = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ItemEntity> tokens = new ArrayList<>();
        final Set<Long> forced = new HashSet<>();
        final Map<BlockPos, MapManager.BlockSnapshot> blocks = new LinkedHashMap<>();
        final List<ScheduledTick<net.minecraft.world.level.block.Block>> blockTicks = new ArrayList<>();
        final List<ScheduledTick<net.minecraft.world.level.material.Fluid>> fluidTicks = new ArrayList<>();
        final BoundingBox bounds;
        final Difficulty difficulty;
        final boolean drops, spawnMobs, spawnMonsters;
        final int alphaKills, betaKills;
        int phase, lastTick, warmTicks, wait, attempts;
        ItemEntity stack, paused, unloaded;
        Zombie caller;
        LbcClouds.AcidProjectile lifetime300, lifetime500, frozen;
        Vec3 frozenPos;
        int frozenAge;
        final Map<Entity, Vec3> frozenPositions = new IdentityHashMap<>();

        State(GameController game) {
            this.game = game;
            level = game.maps().level();
            alpha = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha != null && beta != null && game.allowDamage(beta),
                    "two actual match participants");
            alphaState = new GameChecks.WandThirdPlayerState(alpha, game.items().mana());
            betaState = new GameChecks.WandThirdPlayerState(beta, game.items().mana());
            level.getAllEntities().forEach(existing::add);
            bounds = new BoundingBox(center.getX() - 44, center.getY() - 4, center.getZ() - 44, center.getX() + 44,
                    center.getY() + 12, center.getZ() + 44);
            difficulty = level.getDifficulty();
            drops = level.getGameRules().get(GameRules.MOB_DROPS);
            spawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
            spawnMonsters = level.getGameRules().get(GameRules.SPAWN_MONSTERS);
            alphaKills = kills(alpha);
            betaKills = kills(beta);
        }

        int kills(ServerPlayer p) {
            return p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));
        }

        void initialize() {
            for (int x = (center.getX() - 48) >> 4; x <= (center.getX() + 48) >> 4; x++)
                for (int z = (center.getZ() - 48) >> 4; z <= (center.getZ() + 48) >> 4; z++) {
                    long key = net.minecraft.world.level.ChunkPos.pack(x, z);
                    if (!level.getForceLoadedChunks().contains(key)) {
                        forced.add(key);
                        level.setChunkForced(x, z, true);
                    }
                    var c = level.getChunkAt(new BlockPos(x * 16, center.getY(), z * 16));
                    ((LevelChunkTicks<net.minecraft.world.level.block.Block>) c.getBlockTicks()).getAll()
                            .filter(t -> bounds.isInside(t.pos())).forEach(blockTicks::add);
                    ((LevelChunkTicks<net.minecraft.world.level.material.Fluid>) c.getFluidTicks()).getAll()
                            .filter(t -> bounds.isInside(t.pos())).forEach(fluidTicks::add);
                }
            for (var p : BlockPos.betweenClosed(center.offset(-9, -3, -9), center.offset(9, 10, 9)))
                set(p, Blocks.AIR.defaultBlockState());
            for (var p : List.of(alpha, beta)) {
                p.closeContainer();
                p.getInventory().clearContent();
                p.removeAllEffects();
                p.setGameMode(GameType.SURVIVAL);
                p.setNoGravity(true);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                p.setHealth(1000);
                p.setAbsorptionAmount(0);
                reset(p);
            }
            level.getGameRules().set(GameRules.MOB_DROPS, true, game.server());
            place();
            lastTick = game.server().getTickCount();
        }

        void set(BlockPos p, net.minecraft.world.level.block.state.BlockState state) {
            blocks.computeIfAbsent(p.immutable(), at -> game.maps().snapshot(level, at));
            if (level.getBlockEntity(p) instanceof net.minecraft.world.Container c)
                c.clearContent();
            level.setBlock(p, state, 818);
        }

        void place() {
            alpha.teleportTo(level, origin.x + 20, origin.y + 4, origin.z, Set.of(), 0, 0, true);
            beta.teleportTo(level, origin.x + 22, origin.y + 4, origin.z, Set.of(), 0, 0, true);
        }

        boolean warm() {
            boolean ready = true;
            for (int x = (center.getX() - 48) >> 4; x <= (center.getX() + 48) >> 4; x++)
                for (int z = (center.getZ() - 48) >> 4; z <= (center.getZ() + 48) >> 4; z++)
                    ready &= level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x, z));
            boolean visible = level.getEntities((Entity) null,
                    new AABB(origin.add(-48, -4, -48), origin.add(48, 12, 48)), e -> e == beta).contains(beta);
            require(++warmTicks <= 100,
                    "native warmup timeout loaded=" + ready + " betaVisible=" + visible + " beta=" + beta.position());
            return ready && visible;
        }

        void reset(LivingEntity e) {
            e.setInvulnerableTime(0);
            e.damageCooldownTime = 0;
        }

        void capture() {
            for (var e : game.items().antDrops().snapshot())
                if (!existing.contains(e) && !e.isRemoved())
                    mine.add(e);
            for (var e : game.items().clouds().acidSnapshot())
                if (!existing.contains(e) && !e.isRemoved())
                    mine.add(e);
        }

        void clear() {
            capture();
            for (var e : new ArrayList<>(mine)) {
                if (e instanceof LivingEntity l)
                    game.items().indirectKills().clear(l);
                e.discard();
            }
            mine.clear();
            tokens.forEach(Entity::discard);
            var debris = new ArrayList<Entity>();
            for (var e : level.getAllEntities())
                if (!existing.contains(e) && !(e instanceof ServerPlayer) && bounds.isInside(e.blockPosition()))
                    debris.add(e);
            debris.forEach(Entity::discard);
        }

        ItemEntity token(LbcAntDropItem.Kind kind, int count) {
            var t = new ItemEntity(level, origin.x, origin.y, origin.z, new ItemStack(kind.item(), count));
            t.setThrower(alpha);
            t.setTarget(beta.getUUID());
            t.setNoGravity(true);
            t.setDeltaMovement(Vec3.ZERO);
            t.setPickUpDelay(1000);
            require(level.addFreshEntity(t), "native token admission");
            tokens.add(t);
            return t;
        }

        void activate(LbcAntDropItem.Kind kind) {
            var t = token(kind, 2);
            require(game.items().antDrops().ready(t, kind) && game.items().specialDrops().spawn(t)
                    && t.getItem().getCount() == 1, "shared source debit " + kind);
            require(!game.items().specialDrops().spawn(t), "same tick dispatch once");
            t.discard();
            capture();
        }

        List<Zombie> ants() {
            return game.items().antDrops().snapshot().stream()
                    .filter(e -> !existing.contains(e) && !e.isRemoved() && e instanceof Zombie).map(e -> (Zombie) e)
                    .sorted(Comparator.comparingInt(Entity::getId)).toList();
        }

        List<LbcClouds.AcidProjectile> acids() {
            capture();
            return game.items().clouds().acidSnapshot().stream().filter(e -> !existing.contains(e) && !e.isRemoved())
                    .sorted(Comparator.comparingInt(Entity::getId)).toList();
        }

        Cow cow(Vec3 at) {
            var c = new Cow(EntityTypes.COW, level);
            c.setPos(at);
            c.setNoAi(true);
            c.setNoGravity(true);
            require(level.addFreshEntity(c), "native disposable Cow");
            mine.add(c);
            return c;
        }

        void presentation() {
            for (var kind : LbcAntDropItem.Kind.values()) {
                var s = new ItemStack(kind.item());
                var tag = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                require(tag.size() == 2 && tag.getIntOr(kind.flag(), 0) == 1 && tag.getIntOr("drop", 0) == 1,
                        "exact token flags " + kind);
                boolean burst = kind == LbcAntDropItem.Kind.ACID_BURST;
                require(Identifier.parse(burst ? "lbc:muravei" : "lbc:drop_boss")
                        .equals(s.get(DataComponents.ITEM_MODEL))
                        && Identifier.parse("lbc:tier_ff").equals(s.get(DataComponents.TOOLTIP_STYLE)),
                        "source model/tier");
                var name = s.get(DataComponents.CUSTOM_NAME);
                var lore = s.getOrDefault(DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY)
                        .lines();
                require(name != null
                        && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr
                        && tr.getKey().equals(burst ? "drop_acid" : "drop_muravei") && !name.getStyle().isItalic()
                        && name.getStyle().getColor().getValue() == 0xffff55 && lore.size() == 1
                        && !lore.getFirst().getStyle().isItalic()
                        && lore.getFirst().getStyle().getColor().getValue() == 0xffffff
                        && lore.getFirst()
                                .getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row
                        && row.getKey().equals("trap_desc"), "source yellow name/white lore");
                var loot = game.lbc().rollTable(level, origin, alpha,
                        "lbc:ported/" + (burst ? "acid_burst_drop" : "ant_swarm_drop"));
                require(loot.size() == 1 && loot.getFirst().getCount() == 1
                        && ItemStack.isSameItemSameComponents(s, loot.getFirst()), "complete public native leaf");
            }
            require(!LbcAntDropItem.matches(new ItemStack(Items.REPEATING_COMMAND_BLOCK)),
                    "unflagged lookalike rejected");
        }

        void payload() {
            clear();
            activate(LbcAntDropItem.Kind.ANTS);
            require(ants().size() == 5, "five native source ants");
            for (var z : ants()) {
                require(z.getClass() == Zombie.class && z.position().distanceTo(origin) < 1e-8 && z.isBaby()
                        && z.isSilent() && z.isPersistenceRequired() && !z.isNoAi() && z.getHealth() == 10
                        && z.getMaxHealth() == 10, "native baby ant health/persistence/AI/position");
                require(z.entityTags().containsAll(List.of("muravei", "lucky_mob"))
                        && saved(z, level).getStringOr("DeathLootTable", "").equals("lbc:ant_rewards"),
                        "source ant tags and real internal death rewards");
                var name = z.getCustomName();
                require(name != null && name.getStyle().isEmpty()
                        && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr
                        && tr.getKey().equals("muravei"), "actor name preserves unspecified style");
                var effect = z.getEffect(MobEffects.INVISIBILITY);
                require(effect != null && effect.getAmplifier() == 1 && effect.getDuration() == -1
                        && !effect.isVisible(), "native infinite invisibility1 hidden particles");
                var head = z.getItemBySlot(EquipmentSlot.HEAD);
                require(head.is(Items.REPEATING_COMMAND_BLOCK) && head.getCount() == 1
                        && Identifier.parse("lbc:muravei").equals(head.get(DataComponents.ITEM_MODEL))
                        && head.has(DataComponents.UNBREAKABLE)
                        && z.getDropChances().byEquipment(EquipmentSlot.HEAD) == 0,
                        "native unbreakable modeled ant head never drops");
                for (var s : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET))
                    require(z.getItemBySlot(s).isEmpty(), "no invented equipment");
                for (var e : Map.of(Attributes.FOLLOW_RANGE, 50d, Attributes.MOVEMENT_SPEED, .25d,
                        Attributes.ATTACK_DAMAGE, 3d, Attributes.ARMOR, 5d, Attributes.ARMOR_TOUGHNESS, 5d,
                        Attributes.SPAWN_REINFORCEMENTS_CHANCE, 100d).entrySet())
                    require(z.getAttribute(e.getKey()).getBaseValue() == e.getValue(),
                            "exact source attribute " + e.getKey());
                require(game.items().antDrops().owner(z) == alpha && game.items().currentEpoch(z),
                        "actual author/epoch");
            }
        }

        void antShot() {
            clear();
            activate(LbcAntDropItem.Kind.ANTS);
            for (var z : ants()) {
                z.setNoAi(true);
                z.setNoGravity(true);
            }
            var source = ants().getFirst();
            var target = cow(origin.add(0, 0, 5));
            var excluded = cow(origin.add(0, 0, 1));
            excluded.entityTags().add("spectator");
            require(game.items().antDrops().target(source) == target,
                    "actual nearest source target excludes observer and other ants");
            var p = game.items().antDrops().shoot(source, target);
            require(p != null && p.position().distanceTo(source.getEyePosition()) < 1e-8 && p.getOwner() == null
                    && game.items().clouds().sourceAcidOwner(p) == alpha && game.items().clouds().acidLifetime(p) == 300
                    && game.items().clouds().acidAge(p) == 0 && p.entityTags().contains("shield_blockable2"),
                    "actual source eye shot/native owner/budget/shield/cause");
            var aim = target.getEyePosition().subtract(source.getEyePosition());
            var v = p.getDeltaMovement();
            double yaw = Math.toDegrees(Math.atan2(-aim.x, aim.z)),
                    pitch = -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance()));
            double actualYaw = Math.toDegrees(Math.atan2(-v.x, v.z)),
                    actualPitch = -Math.toDegrees(Math.atan2(v.y, v.horizontalDistance()));
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees(actualYaw - yaw)) <= 30.02
                    && Math.abs(actualPitch - pitch) <= 20.02 && Math.abs(v.length() - .5) < 1e-6,
                    "source ant spread stays within authored30yaw/20pitch");
            p.setPos(source.position());
            p.setNoGravity(true);
            p.setDeltaMovement(Vec3.ZERO);
            game.items().clouds().tick();
            require(!source.hasEffect(MobEffects.POISON), "native ant source tag resists nonburning acid poison");
            set(source.blockPosition().below(), Blocks.LAVA.defaultBlockState());
            float hp = source.getHealth();
            game.items().clouds().tick();
            require(p.entityTags().contains("on_fire") && source.getHealth() < hp,
                    "actual burning source acid still damages ants");
            p.discard();
            set(source.blockPosition().below(), Blocks.AIR.defaultBlockState());
        }

        void burst() {
            clear();
            activate(LbcAntDropItem.Kind.ACID_BURST);
            var list = acids();
            require(list.size() == 3, "three source acid projectiles");
            for (int i = 0; i < 3; i++) {
                var p = list.get(i);
                require(p.position().distanceTo(origin.add(0, 1 + .01 * (i + 1), 0)) < 1e-8
                        && game.items().clouds().acidLifetime(p) == 500 && game.items().clouds().acidAge(p) == 0
                        && !p.isNoGravity() && !p.entityTags().contains("shield_blockable2")
                        && p.entityTags()
                                .containsAll(List.of("raycastacid", "player", "slowraycast", "magic", "actived")),
                        "source exact burst offsets500budget/gravity/no invented shield flag");
                require(Math.abs(p.getDeltaMovement().length() - .5) < 1e-6
                        && game.items().clouds().sourceAcidOwner(p) == alpha
                        && LbcIndirectKills.playerSource(p) == alpha,
                        "source native random direction/speed and separate author");
            }
            require(list.stream().map(Entity::getDeltaMovement).distinct().count() > 1,
                    "independent random shot rotations");
        }

        void motion(LbcClouds.AcidProjectile p, String kind) {
            var c = cow(origin.add(0, 3, 1));
            p.setPos(origin.add(0, 3, 0));
            p.setDeltaMovement(0, 0, 2);
            var before = p.position();
            p.tick();
            require(!p.isRemoved() && p.position().z > c.getZ() + .5 && p.position().distanceTo(before) > 1
                    && p.getDeltaMovement().y < 0, "native shared acid passes through real Cow with gravity: " + kind);
            var wall = center.offset(0, 3, 4);
            set(wall, Blocks.STONE.defaultBlockState());
            p.setPos(wall.getX() + .5, wall.getY() + .5, wall.getZ() - 1);
            p.setDeltaMovement(0, 0, 2);
            p.tick();
            require(!p.isRemoved() && p.noPhysics && p.isNoGravity() && p.getDeltaMovement().equals(Vec3.ZERO),
                    "native block collision still sticks " + kind);
            var at = p.position();
            p.tick();
            require(p.position().equals(at), "native stuck position stable " + kind);
            set(wall, Blocks.AIR.defaultBlockState());
            c.discard();
            p.discard();
        }

        void sharedMotion() {
            burst();
            var source = acids().getFirst();
            for (var p : acids())
                if (p != source)
                    p.discard();
            motion(source, "burst");
            var ordinary = game.items().clouds().ordinaryWandAcid(alpha);
            mine.add(ordinary);
            motion(ordinary, "ordinary existing wand");
            var prior = new HashSet<>(acids());
            game.items().clouds().magnumAcid(alpha, origin.add(0, 3, 0), new Vec3(0, 0, 1), false);
            var fresh = acids().stream().filter(p -> !prior.contains(p)).toList();
            require(fresh.size() == 6, "existing Magnum acid six-charge path retained");
            for (var p : fresh)
                if (p != fresh.getFirst())
                    p.discard();
            motion(fresh.getFirst(), "existing Magnum");
        }

        LbcClouds.AcidProjectile shot(Vec3 at, int lifetime, boolean shield) {
            var p = game.items().clouds().sourceAcid(level, at, 0, 0, lifetime, shield, alpha);
            require(p != null, "source acid native creation");
            mine.add(p);
            p.setNoGravity(true);
            p.setDeltaMovement(Vec3.ZERO);
            return p;
        }

        void effects() {
            clear();
            var c = cow(origin.add(0, 3, 0));
            var p = shot(c.position(), 500, false);
            game.items().clouds().tick();
            var poison = c.getEffect(MobEffects.POISON);
            require(poison != null && poison.getAmplifier() == 0 && poison.getDuration() == 300
                    && c.getHealth() == c.getMaxHealth(),
                    "source acid native300 poison aura rather than instant damage");
            float hp = c.getHealth();
            MobEffects.POISON.value().applyEffectTick(level, c, 0);
            require(c.getHealth() < hp && c.getKillCredit() == alpha,
                    "accepted native nonlethal poison tick credits actual source");
            reset(c);
            require(c.hurtServer(level, c.damageSources().playerAttack(beta), 1),
                    "native newer player hit after accepted acid poison");
            c.setLastHurtByMob(null);
            c.setLastHurtByPlayer((net.minecraft.world.entity.player.Player) null, 0);
            reset(c);
            hp = c.getHealth();
            MobEffects.POISON.value().applyEffectTick(level, c, 0);
            require(c.getHealth() < hp && c.getKillCredit() == beta,
                    "native poison tick retains newer accepted player precedence");
            c.removeAllEffects();
            game.items().indirectKills().clear(c);
            c.discard();
            p.discard();
            var victim = cow(origin.add(0, 3, 0));
            victim.setHealth(2);
            var lava = BlockPos.containing(victim.position()).below();
            set(lava, Blocks.LAVA.defaultBlockState());
            var fire = shot(victim.position(), 500, false);
            int count = kills(alpha);
            game.items().clouds().tick();
            require(fire.entityTags().contains("on_fire") && victim.isDeadOrDying() && victim.getKillCredit() == alpha
                    && kills(alpha) == count + 1 && victim.getLastDamageSource() != null
                    && victim.getLastDamageSource().getDirectEntity() == fire,
                    "real lava-ignited acid deals actual direct lethal fire aura credited to author");
            fire.discard();
            set(lava, Blocks.AIR.defaultBlockState());
            for (String role : List.of("spectator", "strict_map_object", "magic_academy", "sauvojen_tuntija",
                    "rcdenyalways", "transformator")) {
                var protectedCow = cow(origin.add(0, 3, 0));
                protectedCow.entityTags().add(role);
                float before = protectedCow.getHealth();
                var acid = shot(protectedCow.position(), 500, false);
                game.items().clouds().tick();
                require(!protectedCow.hasEffect(MobEffects.POISON) && protectedCow.getHealth() == before,
                        "source acid role immunity " + role);
                acid.discard();
                protectedCow.discard();
            }
        }

        void meleeAndConversion() {
            clear();
            activate(LbcAntDropItem.Kind.ANTS);
            var source = ants().getFirst();
            for (var z : ants()) {
                z.setNoAi(true);
                z.setNoGravity(true);
            }
            var c = cow(origin.add(2, 0, 0));
            c.setHealth(1);
            require(source.doHurtTarget(level, c) && c.isDeadOrDying() && c.getKillCredit() == alpha,
                    "actual native ant melee kill source");
            for (String role : List.of("spectator", "strict_map_object", "magic_academy")) {
                var victim = cow(origin.add(2, 0, 0));
                victim.entityTags().add(role);
                float hp = victim.getHealth();
                require(!game.items().antDrops().canAffect(source, victim) && !source.doHurtTarget(level, victim)
                        && victim.getHealth() == hp, "actual role ant melee guard");
            }
            var board = game.server().getScoreboard();
            var oldA = alpha.getTeam();
            var oldB = beta.getTeam();
            var team = board.addPlayerTeam("codex_acid_ant_wild");
            team.setAllowFriendlyFire(false);
            try {
                board.addPlayerToTeam(alpha.getScoreboardName(), team);
                board.addPlayerToTeam(beta.getScoreboardName(), team);
                for (var player : List.of(alpha, beta)) {
                    reset(player);
                    float hp = player.getHealth();
                    require(game.items().antDrops().canAffect(source, player) && source.doHurtTarget(level, player)
                            && player.getHealth() < hp, "WILD native melee damages self/allied player");
                    player.removeAllEffects();
                    player.setHealth(1000);
                    var acid = shot(player.position(), 500, false);
                    game.items().clouds().tick();
                    require(player.hasEffect(MobEffects.POISON), "WILD source acid poisons self/allied player");
                    acid.discard();
                    player.removeAllEffects();
                }
            } finally {
                board.removePlayerTeam(team);
                if (oldA != null)
                    board.addPlayerToTeam(alpha.getScoreboardName(), oldA);
                if (oldB != null)
                    board.addPlayerToTeam(beta.getScoreboardName(), oldB);
            }
            var head = source.getItemBySlot(EquipmentSlot.HEAD).copy();
            var converted = source.convertTo(EntityTypes.DROWNED, ConversionParams.single(source, true, true), e -> {
            });
            require(converted != null && source.isRemoved() && game.items().antDrops().snapshot().contains(converted)
                    && game.items().antDrops().owner(converted) == alpha && game.items().currentEpoch(converted)
                    && ItemStack.isSameItemSameComponents(head, converted.getItemBySlot(EquipmentSlot.HEAD))
                    && converted.getDropChances().byEquipment(EquipmentSlot.HEAD) == 0,
                    "native conversion preserves copied equipment/author/epoch and tracking");
            var victim = cow(origin.add(2, 0, 0));
            victim.setHealth(1);
            require(converted.doHurtTarget(level, victim) && victim.isDeadOrDying() && victim.getKillCredit() == alpha,
                    "converted native ant retains kill attribution");
        }

        void rewards() {
            ItemStack wand = ItemStack.EMPTY;
            boolean noWand = false;
            for (int i = 0; i < 128; i++) {
                var loot = game.lbc().rollTable(level, origin, alpha, "lbc:ant_rewards");
                require(loot.stream()
                        .filter(s -> LbcAntDropItem.matches(s, LbcAntDropItem.Kind.ACID_BURST) && s.getCount() == 1)
                        .count() == 1, "real reward rolls always give one burst");
                for (var s : loot)
                    require(LbcAntDropItem.matches(s, LbcAntDropItem.Kind.ACID_BURST) || s.is(LbcWands.ACID_WAND),
                            "no placeholder reward");
                var optional = loot.stream().filter(s -> s.is(LbcWands.ACID_WAND)).findFirst();
                if (optional.isPresent())
                    wand = optional.get().copy();
                else
                    noWand = true;
            }
            require(!wand.isEmpty() && noWand, "real10percent optional wand branch both outcomes");
            clear();
            activate(LbcAntDropItem.Kind.ANTS);
            for (var z : List.copyOf(ants())) {
                z.getRandom().setSeed(51001 + z.getId());
                var before = new HashSet<Entity>();
                level.getAllEntities().forEach(before::add);
                require(saved(z, level).getStringOr("DeathLootTable", "").equals("lbc:ant_rewards"),
                        "override present before native death");
                require(z.hurtServer(level, z.damageSources().playerAttack(beta), 10000) && z.isDeadOrDying(),
                        "actual ant native death");
                var items = new ArrayList<ItemEntity>();
                for (var e : level.getAllEntities())
                    if (!before.contains(e) && e instanceof ItemEntity item)
                        items.add(item);
                require(items.stream()
                        .filter(i -> LbcAntDropItem.matches(i.getItem(), LbcAntDropItem.Kind.ACID_BURST)
                                && i.getItem().getCount() == 1)
                        .count() == 1 && items.stream().noneMatch(i -> i.getItem().is(Items.REPEATING_COMMAND_BLOCK)),
                        "actual death guarantees one burst and never drops modeled head");
                items.forEach(Entity::discard);
            }
            clear();
            alpha.setItemInHand(InteractionHand.MAIN_HAND, wand);
            alpha.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            game.items().mana().set(alpha, 1000);
            int before = game.items().mana().get(alpha);
            var old = new HashSet<>(acids());
            require(wand.getItem().use(level, alpha, InteractionHand.MAIN_HAND).consumesAction()
                    && game.items().mana().get(alpha) < before, "actual rolled native wand casts and spends mana");
            require(acids().stream()
                    .anyMatch(p -> !old.contains(p) && p.entityTags().contains("skywars2.wand.acid.regular")),
                    "real wand produces established regular-acid projectile");
            alpha.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        void admissions() {
            for (var kind : LbcAntDropItem.Kind.values()) {
                var stale = token(kind, 2);
                stale.entityTags().add("skywars2.items.stale-fixture");
                require(!game.items().specialDrops().spawn(stale) && stale.getItem().getCount() == 2,
                        "stale token no debit");
                stale.discard();
                var fake = token(kind, 2);
                fake.makeFakeItem();
                require(!game.items().specialDrops().spawn(fake), "fake token rejected");
                fake.discard();
                var remote = token(kind, 2);
                int x = (center.getX() >> 4) + 128, z = (center.getZ() >> 4) + 128;
                while (level.getChunkSource().getChunkNow(x, z) != null)
                    x += 128;
                remote.setPos(x * 16 + 8, origin.y, z * 16 + 8);
                require(!game.items().specialDrops().spawn(remote) && remote.getItem().getCount() == 2
                        && level.getChunkSource().getChunkNow(x, z) == null,
                        "unresident token cannot request chunk/debit");
                remote.discard();
            }
            clear();
            var t = token(LbcAntDropItem.Kind.ANTS, 2);
            var flags = t.getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            flags.putInt("acidboom", 1);
            t.getItem().set(DataComponents.CUSTOM_DATA, CustomData.of(flags));
            require(game.items().specialDrops().spawn(t) && t.getItem().getCount() == 1,
                    "both source flags one shared debit");
            t.discard();
            capture();
            require(ants().size() == 5 && acids().size() == 3 && ants().getLast().getId() < acids().getFirst().getId(),
                    "source-ordered five ants before three burst charges");
        }

        final Map<net.minecraft.stats.Stat<Identifier>, Integer> shieldStats = new LinkedHashMap<>();

        void restoreShieldStats() {
            shieldStats.forEach((stat, value) -> beta.getStats().setValue(beta, stat, value));
            shieldStats.clear();
        }

        void armShield() {
            clear();
            for (var id : net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT) {
                var stat = net.minecraft.stats.Stats.CUSTOM.get(id);
                shieldStats.put(stat, beta.getStats().getValue(stat));
            }
            beta.teleportTo(level, origin.x, origin.y + 3, origin.z, Set.of(), 0, 0, true);
            beta.setDeltaMovement(Vec3.ZERO);
            beta.stopUsingItem();
            beta.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHIELD));
            beta.startUsingItem(InteractionHand.MAIN_HAND);
            wait = 0;
        }

        void shield() {
            require(beta.isBlocking(), "actual native shield blocking after use delay: fixtureTicks=" + wait + " using="
                    + beta.isUsingItem() + " useTicks=" + beta.getTicksUsingItem() + " remaining="
                    + beta.getUseItemRemainingTicks() + " hand=" + beta.getUsedItemHand() + " useStack="
                    + beta.getUseItem() + " held=" + beta.getMainHandItem() + " blocksAttacks="
                    + beta.getUseItem().get(DataComponents.BLOCKS_ATTACKS) + " mode="
                    + beta.gameMode.getGameModeForPlayer() + " stopped=" + game.items().timeStop().stopped(level));
            var at = beta.position().add(beta.getLookAngle());
            var burst = shot(at, 500, false);
            game.items().clouds().tick();
            require(!burst.isRemoved() && beta.hasEffect(MobEffects.POISON),
                    "source burst ignores shield and poisons blocking player");
            burst.discard();
            beta.removeAllEffects();
            var ant = shot(at, 300, true);
            game.items().clouds().tick();
            require(ant.isRemoved() && !beta.hasEffect(MobEffects.POISON),
                    "ant300 source charge retains actual shield block");
            beta.stopUsingItem();
            beta.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            restoreShieldStats();
            place();
        }

        void aged(LbcClouds.AcidProjectile p, int age) {
            var tag = saved(p, level);
            var attachment = tag.getCompoundOrEmpty("fabric:attachments");
            require(attachment.contains("skywars2:acid_age"), "native Fabric acid age serialized");
            attachment.putInt("skywars2:acid_age", age);
            tag.put("fabric:attachments", attachment);
            game.items().clouds().unloaded(p);
            p.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            game.items().clouds().loaded(p);
            require(game.items().clouds().acidAge(p) == age && game.items().clouds().sourceAcidOwner(p) == alpha,
                    "native persisted age and author reload");
        }

        void prepareLifetimes() {
            clear();
            lifetime300 = shot(origin.add(4, 6, 0), 300, true);
            lifetime500 = shot(origin.add(-4, 6, 0), 500, false);
            aged(lifetime300, 298);
            aged(lifetime500, 498);
            require(game.items().clouds().acidLifetime(lifetime300) == 300
                    && game.items().clouds().acidLifetime(lifetime500) == 500,
                    "native reload preserves distinct source budgets");
        }

        void prepareReinforcement() {
            clear();
            for (int x = -44; x <= 44; x++)
                for (int z = -44; z <= 44; z++) {
                    set(center.offset(x, 3, z), Blocks.STONE.defaultBlockState());
                    if (Math.abs(x) <= 40 && Math.abs(z) <= 40) {
                        set(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                        for (int y = 0; y <= 2; y++)
                            set(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            game.server().setDifficulty(Difficulty.HARD, true);
            level.getGameRules().set(GameRules.SPAWN_MOBS, true, game.server());
            level.getGameRules().set(GameRules.SPAWN_MONSTERS, true, game.server());
            activate(LbcAntDropItem.Kind.ANTS);
            caller = ants().getFirst();
            for (var z : ants())
                if (z != caller)
                    z.discard();
            caller.setNoAi(true);
            caller.setNoGravity(true);
            alpha.teleportTo(level, origin.x + 60, origin.y, origin.z, Set.of(), 0, 0, true);
            beta.teleportTo(level, origin.x + 62, origin.y, origin.z, Set.of(), 0, 0, true);
            caller.setTarget(beta);
            wait = 0;
            attempts = 0;
        }

        long jockeySeed(Zombie child, boolean existingChicken) {
            boolean needsBonus = !child.getAttribute(Attributes.FOLLOW_RANGE)
                    .hasModifier(Identifier.parse("minecraft:random_spawn_bonus"));
            for (long seed = 1; seed < 100000; seed++) {
                var random = net.minecraft.util.RandomSource.create(seed);
                if (needsBonus)
                    random.triangle(0, .11485000000000001);
                random.nextFloat();
                random.nextFloat();
                float first = random.nextFloat();
                if (existingChicken ? first < .05 : first >= .05 && random.nextFloat() < .05)
                    return seed;
            }
            throw new IllegalStateException("No deterministic native jockey seed");
        }

        Entity newJockey(Zombie child) {
            if (child.getVehicle() != null) {
                var old = child.getVehicle();
                child.stopRiding();
                if (LbcAntDrops.managed(old) && !existing.contains(old))
                    old.discard();
            }
            level.getRandom().setSeed(jockeySeed(child, false));
            child.finalizeSpawn(level, level.getCurrentDifficultyAt(child.blockPosition()),
                    EntitySpawnReason.REINFORCEMENT, new Zombie.ZombieGroupData(true, true));
            var chicken = child.getVehicle();
            require(chicken != null && chicken.getType() == EntityTypes.CHICKEN && LbcAntDrops.managed(chicken)
                    && game.items().antDrops().snapshot().contains(chicken)
                    && game.items().antDrops().owner(chicken) == alpha && game.items().currentEpoch(chicken),
                    "actual native finalizer creates and admits causal jockey chicken");
            require(!chicken.entityTags().contains("muravei") && !saved(chicken, level).contains("DeathLootTable"),
                    "native new mount receives only cause/lifecycle lineage");
            mine.add(chicken);
            return chicken;
        }

        Zombie jockeyChild;
        net.minecraft.world.entity.animal.chicken.Chicken foreignChicken;

        void jockeys(Zombie child) {
            var chicken = newJockey(child);
            child.stopRiding();
            chicken.discard();
            var foreign = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
            require(foreign != null, "native existing chicken fixture");
            foreign.setPos(child.position());
            foreign.setNoAi(true);
            foreign.setNoGravity(true);
            require(level.addFreshEntity(foreign), "native foreign chicken admission");
            mine.add(foreign);
            jockeyChild = child;
            foreignChicken = foreign;
            wait = 0;
            level.getGameRules().set(GameRules.SPAWN_MOBS, false, game.server());
        }

        void existingJockey() {
            var child = jockeyChild;
            var foreign = foreignChicken;
            level.getRandom().setSeed(jockeySeed(child, true));
            child.finalizeSpawn(level, level.getCurrentDifficultyAt(child.blockPosition()),
                    EntitySpawnReason.REINFORCEMENT, new Zombie.ZombieGroupData(true, true));
            require(child.getVehicle() == foreign && !LbcAntDrops.managed(foreign)
                    && game.items().antDrops().owner(foreign) == null
                    && !game.items().antDrops().snapshot().contains(foreign),
                    "actual native existing-chicken branch never claims unrelated mount: waited=" + wait + " vehicle="
                            + (child.getVehicle() == null ? "null"
                                    : child.getVehicle().getType() + "/" + child.getVehicle().getUUID())
                            + " expected=" + foreign.getUUID() + " childAlive=" + child.isAlive() + " childRemoved="
                            + child.isRemoved() + " childShift=" + child.isShiftKeyDown() + " foreignAlive="
                            + foreign.isAlive() + " foreignRemoved=" + foreign.isRemoved() + " foreignPassengers="
                            + foreign.getPassengers() + " managed=" + LbcAntDrops.managed(foreign) + " owner="
                            + game.items().antDrops().owner(foreign) + " tracked="
                            + game.items().antDrops().snapshot().contains(foreign) + " queryContains="
                            + level.getEntitiesOfClass(net.minecraft.world.entity.animal.chicken.Chicken.class,
                                    child.getBoundingBox().inflate(5, 3, 5),
                                    net.minecraft.world.entity.EntitySelector.ENTITY_NOT_BEING_RIDDEN)
                                    .contains(foreign));
            child.stopRiding();
        }

        boolean reinforcement() {
            var before = new HashSet<Entity>();
            level.getAllEntities().forEach(before::add);
            caller.setHealth(10);
            reset(caller);
            caller.getRandom().setSeed(73001 + attempts);
            require(caller.hurtServer(level, caller.damageSources().playerAttack(beta), .1f),
                    "real native hurt triggers reinforcement consideration");
            capture();
            var fresh = game.items().antDrops().snapshot().stream()
                    .filter(e -> e instanceof Zombie && !before.contains(e) && !e.isRemoved()).map(e -> (Zombie) e)
                    .toList();
            if (fresh.isEmpty()) {
                require(++attempts < 12,
                        "actual reinforcement timeout difficulty=" + level.getDifficulty() + " spawning="
                                + level.isSpawningMonsters() + " callerChance="
                                + caller.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE));
                return false;
            }
            require(fresh.size() == 1, "native branch inserts one reinforcement per accepted hurt");
            var child = fresh.getFirst();
            require(child.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == 20
                    && !child.entityTags().contains("muravei") && !child.entityTags().contains("lucky_mob")
                    && !child.hasEffect(MobEffects.INVISIBILITY) && !saved(child, level).contains("DeathLootTable")
                    && !Identifier.parse("lbc:muravei")
                            .equals(child.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.ITEM_MODEL)),
                    "actual finalized ordinary child has no invented ant caster/reward payload");
            require(game.items().antDrops().owner(child) == alpha && game.items().currentEpoch(child)
                    && child.getTarget() == beta, "actual reinforcement retains source author/epoch/native target");
            child.setNoAi(true);
            child.setNoGravity(true);
            var victim = cow(child.position().add(2, 0, 0));
            victim.setHealth(1);
            require(child.doHurtTarget(level, victim) && victim.isDeadOrDying() && victim.getKillCredit() == alpha,
                    "ordinary native reinforcement melee retains author");
            jockeyChild = child;
            wait = 0;
            if (child.getVehicle() != null) {
                var previous = child.getVehicle();
                child.stopRiding();
                if (LbcAntDrops.managed(previous) && !existing.contains(previous))
                    previous.discard();
                wait = -60;
            }
            level.getGameRules().set(GameRules.SPAWN_MOBS, false, game.server());
            return true;
        }

        void randomSetup() {
            clear();
            place();
            activate(LbcAntDropItem.Kind.ANTS);
            for (var z : ants()) {
                z.setNoAi(true);
                z.setNoGravity(true);
            }
            cow(origin.add(0, 0, 5));
            wait = 0;
        }

        boolean randomShot() {
            var list = acids();
            if (list.isEmpty()) {
                require(++wait < 100, "actual ant recurring random shot timeout");
                return false;
            }
            for (var p : list)
                require(game.items().clouds().acidLifetime(p) == 300 && p.entityTags().contains("shield_blockable2")
                        && game.items().clouds().sourceAcidOwner(p) == alpha,
                        "real recurring ant shot uses300/shield/separate author");
            return true;
        }

        LbcClouds.AcidProjectile oldFrozen;
        Cow freezeAuraVictim;
        int oldFrozenAge;
        Vec3 oldFrozenPos;

        boolean step() {
            require(game.playing(), "actual match retained");
            int now = game.server().getTickCount();
            if (now == lastTick)
                return false;
            lastTick = now;
            switch (phase) {
                case 0 -> {
                    if (!warm())
                        return false;
                    presentation();
                    payload();
                    antShot();
                    burst();
                    admissions();
                    sharedMotion();
                    effects();
                    meleeAndConversion();
                    rewards();
                    armShield();
                    phase++;
                }
                case 1 -> {
                    beta.doTick();
                    if (++wait >= 6) {
                        shield();
                        prepareLifetimes();
                        phase++;
                    }
                }
                case 2 -> {
                    require(!lifetime300.isRemoved() && !lifetime500.isRemoved()
                            && game.items().clouds().acidAge(lifetime300) == 299
                            && game.items().clouds().acidAge(lifetime500) == 499,
                            "actual tick retains299/499 age boundary");
                    phase++;
                }
                case 3 -> {
                    require(lifetime300.isRemoved() && lifetime500.isRemoved(),
                            "actual tick expires native300/500 source budgets without reset");
                    prepareReinforcement();
                    phase++;
                }
                case 4 -> {
                    if (++wait >= 5)
                        phase++;
                }
                case 5 -> {
                    if (reinforcement())
                        phase = 13;
                }
                case 13 -> {
                    if (++wait >= 1) {
                        jockeys(jockeyChild);
                        phase = 11;
                    }
                }
                case 11 -> {
                    if (++wait >= 60) {
                        existingJockey();
                        randomSetup();
                        phase = 6;
                    }
                }
                case 6 -> {
                    if (randomShot()) {
                        clear();
                        place();
                        stack = token(LbcAntDropItem.Kind.ANTS, 2);
                        phase++;
                    }
                }
                case 7 -> {
                    require(stack.getItem().getCount() == 1 && ants().size() == 5, "actual first tick dispatch/debit");
                    phase++;
                }
                case 8 -> {
                    require(stack.isRemoved() && ants().size() == 10, "actual second tick source dispatch/debit");
                    for (var z : ants()) {
                        z.setNoAi(true);
                        z.setNoGravity(true);
                        frozenPositions.put(z, z.position());
                    }
                    frozen = shot(origin.add(0, 5, 0), 500, false);
                    frozenPos = frozen.position();
                    frozenAge = game.items().clouds().acidAge(frozen);
                    oldFrozen = game.items().clouds().ordinaryWandAcid(alpha);
                    mine.add(oldFrozen);
                    oldFrozen.setPos(origin.add(5, 5, 0));
                    oldFrozen.setDeltaMovement(new Vec3(.1, .1, .1));
                    oldFrozenPos = oldFrozen.position();
                    oldFrozenAge = game.items().clouds().acidAge(oldFrozen);
                    freezeAuraVictim = cow(oldFrozen.position());
                    game.items().timeStop().begin(level, 200);
                    paused = token(LbcAntDropItem.Kind.ACID_BURST, 2);
                    wait = 0;
                    phase++;
                }
                case 9 -> {
                    require(paused.getItem().getCount() == 2 && !frozen.isRemoved()
                            && frozen.position().equals(frozenPos) && game.items().clouds().acidAge(frozen) == frozenAge
                            && frozenPositions.entrySet().stream().allMatch(
                                    e -> !e.getKey().isRemoved() && e.getKey().position().equals(e.getValue())),
                            "actual freeze retains native ants/source acid age/motion and pending debit");
                    require(!oldFrozen.isRemoved() && oldFrozen.position().equals(oldFrozenPos)
                            && game.items().clouds().acidAge(oldFrozen) == oldFrozenAge
                            && !freezeAuraVictim.hasEffect(MobEffects.POISON),
                            "actual existing ordinary acid freeze pauses native motion, service lifetime and aura");
                    if (++wait >= 3) {
                        paused.discard();
                        game.items().timeStop().end(level);
                        clear();
                        place();
                        unloaded = token(LbcAntDropItem.Kind.ANTS, 2);
                        game.items().specialDrops().unloaded(unloaded);
                        phase++;
                    }
                }
                case 10 -> {
                    require(unloaded.getItem().getCount() == 2, "unloaded pending token no replay");
                    unloaded.discard();
                    activate(LbcAntDropItem.Kind.ANTS);
                    var z = ants().getFirst();
                    game.items().antDrops().unloaded(z);
                    require(!game.items().antDrops().snapshot().contains(z), "unload detaches native actor");
                    game.items().antDrops().loaded(z);
                    require(game.items().antDrops().snapshot().contains(z) && game.items().antDrops().owner(z) == alpha,
                            "same epoch reload preserves author");
                    z.entityTags().removeIf(t -> t.startsWith("skywars2.items."));
                    z.entityTags().add("skywars2.items.stale-fixture");
                    game.items().antDrops().unloaded(z);
                    game.items().antDrops().loaded(z);
                    require(z.isRemoved(), "stale ant quietly discarded");
                    var acid = shot(origin.add(0, 5, 0), 500, false);
                    acid.entityTags().removeIf(t -> t.startsWith("skywars2.items."));
                    acid.entityTags().add("skywars2.items.stale-fixture");
                    game.items().clouds().unloaded(acid);
                    game.items().clouds().loaded(acid);
                    require(acid.isRemoved(), "stale source acid quietly rejected on native lifecycle admission");
                    return true;
                }
                default -> throw new IllegalStateException("Unknown acid/ant phase " + phase);
            }
            return false;
        }

        public void close() {
            close(true);
        }

        void restorePlayers() {
            alphaState.restore(game);
            betaState.restore(game);
            alpha.getStats().setValue(alpha, net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),
                    alphaKills);
            beta.getStats().setValue(beta, net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),
                    betaKills);
        }

        void close(boolean players) {
            restoreShieldStats();
            game.items().timeStop().end(level);
            clear();
            level.getBlockTicks().clearArea(bounds);
            level.getFluidTicks().clearArea(bounds);
            for (var e : blocks.entrySet()) {
                if (level.getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)
                    c.clearContent();
                game.maps().restoreSnapshot(level, e.getKey(), e.getValue());
            }
            level.getBlockTicks().clearArea(bounds);
            level.getFluidTicks().clearArea(bounds);
            blockTicks.forEach(level.getBlockTicks()::schedule);
            fluidTicks.forEach(level.getFluidTicks()::schedule);
            for (long key : forced)
                level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),
                        net.minecraft.world.level.ChunkPos.getZ(key), false);
            level.getGameRules().set(GameRules.MOB_DROPS, drops, game.server());
            level.getGameRules().set(GameRules.SPAWN_MOBS, spawnMobs, game.server());
            level.getGameRules().set(GameRules.SPAWN_MONSTERS, spawnMonsters, game.server());
            game.server().setDifficulty(difficulty, true);
            game.items().indirectKills().clear(alpha);
            game.items().indirectKills().clear(beta);
            if (players)
                restorePlayers();
        }
    }

    private static void failure(State s, Throwable e) {
        try {
            s.close();
        } catch (Throwable cleanup) {
            if (e != cleanup)
                e.addSuppressed(cleanup);
        }
        running = null;
        stopping = null;
    }

    public static void run(GameController game) {
        require(running == null && stopping == null && retained == null, "already running");
        running = new State(game);
        try {
            running.initialize();
        } catch (RuntimeException | Error e) {
            failure(running, e);
            throw e;
        }
    }

    public static void tick(GameController game) {
        if (stopping != null) {
            var s = stopping;
            if (game.server().getTickCount() == s.lastTick)
                return;
            s.lastTick = game.server().getTickCount();
            try {
                game.items().timeStop().end(s.level);
                s.place();
                if (!s.warm()) {
                    game.items().timeStop().begin(s.level, 200);
                    return;
                }
                s.activate(LbcAntDropItem.Kind.ANTS);
                s.newJockey(s.ants().getFirst());
                s.activate(LbcAntDropItem.Kind.ACID_BURST);
                s.capture();
                stopActors = List.copyOf(s.mine);
                require(stopActors.size() == 9,
                        "five ants, native finalizer jockey mount and three burst projectiles retained before stop");
                stopEpoch = game.items().epoch();
                stopBaseline.clear();
                s.level.getAllEntities().forEach(stopBaseline::add);
                s.restorePlayers();
                game.items().timeStop().begin(s.level, 200);
                stopToken = s.token(LbcAntDropItem.Kind.ANTS, 3);
                require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount() == 3,
                        "actual stop retains frozen pending source");
                retained = s;
                stopping = null;
                SkyWars2.LOGGER.info("[check] PASS acid-ant-drops-stop-probe");
            } catch (Throwable e) {
                failure(s, e);
                SkyWars2.LOGGER.error("[check] FAIL acid-ant-drops-stop-probe", e);
            }
            return;
        }
        if (running == null)
            return;
        var s = running;
        try {
            if (s.step()) {
                s.close();
                running = null;
                SkyWars2.LOGGER.info("[check] PASS acid-ant-drops");
            }
        } catch (Throwable e) {
            failure(s, e);
            SkyWars2.LOGGER.error("[check] FAIL acid-ant-drops", e);
        }
    }

    public static void stopProbe(GameController game) {
        require(game.playing() && running == null && stopping == null && retained == null, "actual stop probe");
        stopping = new State(game);
        try {
            stopping.initialize();
        } catch (RuntimeException | Error e) {
            failure(stopping, e);
            throw e;
        }
    }

    public static void cleanup(GameController game) {
        require(retained != null && stopEpoch != null && !game.items().currentEpoch(stopEpoch) && stopToken != null
                && stopToken.entityTags().contains(stopEpoch) && stopToken.getItem().getCount() == 3
                && stopActors.size() == 9 && stopActors.stream().allMatch(Entity::isRemoved),
                "actual stop quietly removes native ants/projectiles and invalidates pending epoch");
        require(!game.items().specialDrops().spawn(stopToken), "stopped pending token cannot replay");
        for (var e : retained.level.getAllEntities())
            require(stopBaseline.contains(e) || e == stopToken
                    || (!(e instanceof ItemEntity) && e.getType() != EntityTypes.EXPERIENCE_ORB)
                    || !retained.bounds.isInside(e.blockPosition()), "quiet actual stop emits no ant rewards/XP");
        stopToken.discard();
        retained.close(false);
        retained = null;
        stopToken = null;
        stopEpoch = null;
        stopActors = List.of();
        stopBaseline.clear();
        SkyWars2.LOGGER.info("[check] PASS acid-ant-drops-cleanup");
    }
}
