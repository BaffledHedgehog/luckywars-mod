package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Isolated mode1 fixtures exercise the native service without enabling
 * unfinished LuckyWars.
 */
public final class HelicopterEncounterChecks {
    private HelicopterEncounterChecks() {
    }

    private static State running;
    private static List<Entity> stopEntities = List.of();
    private static String stopEpoch;

    private static void require(boolean value, String message) {
        if (!value)
            throw new IllegalStateException("Helicopter encounter: " + message);
    }

    private static ServerPlayer player(GameController game, String name) {
        var result = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name));
        require(result != null, "requires " + name);
        return result;
    }

    private static CompoundTag saved(Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static ItemEntity token(ServerLevel level, Vec3 at, int count) {
        var stack = new ItemStack(LbcHelicopterTriggerItem.TOKEN, count);
        var data = new CompoundTag();
        data.putInt("helicopter_boss", 1);
        data.putInt("drop", 1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        var item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setYRot(0);
        item.setXRot(0);
        item.setNoGravity(true);
        item.setDeltaMovement(Vec3.ZERO);
        item.setPickUpDelay(1000);
        require(level.addFreshEntity(item), "native dropped trigger added");
        return item;
    }

    private static final class State implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final LbcHelicopterEncounter service;
        final ServerPlayer alpha, beta;
        final GameChecks.WandThirdPlayerState alphaSaved, betaSaved;
        final Set<Entity> initial = new HashSet<>();
        final ArrayList<Entity> fixtures = new ArrayList<>();
        final Set<Long> forcedChunks = new HashSet<>();
        final Vec3 origin = new Vec3(64.5, 260, 14.5);
        LbcHelicopterEncounter.View first, second, remote;
        LivingEntity causal;
        ItemEntity input, loose;
        Cow victim, bystander;
        Vec3 before;
        net.minecraft.core.BlockPos beaconPos;
        MapManager.BlockSnapshot beaconSaved;
        int phase = -3, lastTick, ticks, variant = 1;
        long dispatches;
        final java.util.Map<net.minecraft.core.BlockPos,MapManager.BlockSnapshot> terminalTerrain=new java.util.LinkedHashMap<>();
        final ArrayList<net.minecraft.world.entity.projectile.arrow.AbstractArrow> terminalTagged=new ArrayList<>();
        net.minecraft.world.entity.projectile.arrow.AbstractArrow terminalPlain,terminalAir;
        int terminalTick;boolean terminalVerified;ItemStack terminalDrop;


        State(GameController game) {
            this.game = game;
            level = game.maps().level();
            service = game.items().helicopterEncounter();
            alpha = player(game, "TestAlpha");
            beta = player(game, "TestBeta");
            require(game.playing() && game.allowDamage(alpha) && game.allowDamage(beta),
                    "two active native match players");
            alphaSaved = new GameChecks.WandThirdPlayerState(alpha, game.items().mana());
            betaSaved = new GameChecks.WandThirdPlayerState(beta, game.items().mana());
            level.getAllEntities().forEach(initial::add);
            service.stop();
            for (int x = 2; x <= 10; x++)
                for (int z = -1; z <= 8; z++)
                    force(x, z);
            for (var p : List.of(alpha, beta)) {
                p.closeContainer();
                p.getInventory().clearContent();
                p.removeAllEffects();
                p.setNoGravity(true);
                p.setGameMode(GameType.SURVIVAL);
                p.entityTags().remove("time_stopped");
            }
            var prototype = new ItemStack(LbcHelicopterTriggerItem.TOKEN);
            var loot = game.lbc().rollTable(level, origin, alpha, "lbc:ported/helicopter_encounter");
            require(loot.size() == 1 && loot.getFirst().getCount() == 1
                    && ItemStack.isSameItemSameComponents(prototype, loot.getFirst()),
                    "ported trigger loot equals native prototype components");
            game.command("give TestAlpha skywars2:helicopter_encounter 1");
            var triggerFlags = prototype.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            require(triggerFlags.size() == 2 && triggerFlags.getIntOr("drop", 0) == 1
                    && triggerFlags.getIntOr("helicopter_boss", 0) == 1, "exact source trigger flags");
            require(Identifier.parse("lbc:drop_boss").equals(prototype.get(DataComponents.ITEM_MODEL))
                    && Identifier.parse("lbc:tier_ff").equals(prototype.get(DataComponents.TOOLTIP_STYLE)),
                    "source trigger model and tooltip tier");
            require(prototype.getHoverName()
                    .getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name
                    && name.getKey().equals("drop_helicopter"), "source trigger translated name key");
            var lore = prototype.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines();
            require(lore.size() == 1
                    && lore.getFirst()
                            .getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row
                    && row.getKey().equals("trap_desc"), "source trigger lore key");
            alpha.getInventory().clearContent();
            move(alpha, new Vec3(64.5, 279, 24.5));
            move(beta, new Vec3(128.5, 279, 24.5));
            lastTick = game.server().getTickCount();
        }

        void force(int x, int z) {
            long key = net.minecraft.world.level.ChunkPos.pack(x, z);
            if (!level.getForceLoadedChunks().contains(key)) {
                forcedChunks.add(key);
                level.setChunkForced(x, z, true);
            }
            level.getChunk(x, z);
        }

        void move(ServerPlayer player, Vec3 at) {
            player.teleportTo(level, at.x, at.y, at.z, Set.of(), 0, 0, true);
            player.hasChangedDimension();
        }

        Cow cow(Vec3 at) {
            var cow = new Cow(EntityTypes.COW, level);
            cow.setPos(at);
            cow.setNoAi(true);
            cow.setNoGravity(true);
            cow.setYRot(0);
            cow.setXRot(0);
            cow.setYHeadRot(0);
            cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
            cow.setHealth(200);
            require(level.addFreshEntity(cow), "native contact cow added");
            fixtures.add(cow);
            return cow;
        }

        LbcHelicopterEncounter.View spawn(Vec3 at, int count) {
            input = token(level, at, count);
            fixtures.add(input);
            int before = service.activeCount();
            require(service.spawn(input),
                    "real dropped stack invokes native spawn at=" + at + " age=" + input.getAge() + " removed="
                            + input.isRemoved() + " loaded="
                            + level.hasChunkAt(net.minecraft.core.BlockPos.containing(at.add(0, 20, 50))));
            require(service.activeCount() == before + 1, "exactly one pair per token invocation");
            require(input.getItem().getCount() == count - 1, "one consumed unit per dropped stack invocation");
            input.discard(); // This fixture uses one pair; the separate real-drop phases test continued
                             // stack dispatch.
            return service.snapshot().getLast();
        }

        void visual(LbcHelicopterEncounter.View view, Vec3 expected) {
            require(view.display().position().distanceTo(expected) < .001
                    && view.hitbox().position().distanceTo(expected) < .001,
                    "source local +20Y/+50Z spawn expected=" + expected + " display=" + view.display().position()
                            + " hitbox=" + view.hitbox().position()
                            + " token=" + input.position() + " rotation=" + input.getYRot() + "," + input.getXRot());
            var h = view.hitbox();
            require(h.getHealth() == 150 && h.getMaxHealth() == 150, "real ravager 150 HP");
            require(h.getArmorValue() == 10
                    && h.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) == 10,
                    "native armor/toughness 10");
            require(h.isNoAi() && h.isNoGravity() && h.isInvisible() && h.isSilent() && !h.isInvulnerable(),
                    "invisible persistent native damageable hitbox: noAI=" + h.isNoAi() + " noGravity="
                            + h.isNoGravity() + " invisible=" + h.isInvisible() + " silent=" + h.isSilent()
                            + " invulnerable=" + h.isInvulnerable());
            require(h.getCustomName() != null && h.isCustomNameVisible() && h.getTeam() == null
                    && view.display().getTeam() == null, "source name visible and teamless NPC");
            var data = saved(view.display());
            require(data.getFloatOr("view_range", 0) == 10 && data.getIntOr("interpolation_duration", 0) == 1,
                    "native display view range/interpolation");
            require(data.toString().contains("lbc:helicopter_1") && data.toString().contains("3.0f"),
                    "source display model and scale3 serialized");
        }

        void cleanAttacks() {
            for (var e : service.attackSnapshot())
                e.discard();
        }

        void looseSetup() {
            cleanAttacks();
            game.items().kfcRewards().stop();
            if (victim != null)
                victim.discard();
            if (bystander != null)
                bystander.discard();
            victim = cow(new Vec3(80.5, 285, 80.5));
            bystander = cow(victim.position().add(5, 0, 0));
            before = victim.position();
            loose = game.items().kfcRewards().spawnLooseBomb(causal, victim.position(), variant);
            require(loose != null && loose.getVehicle() == null && loose.getPassengers().isEmpty(),
                    "loose subordinate item has no bat");
            fixtures.add(loose);
            var stack = loose.getItem();
            var flags = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            require(stack.getMaxStackSize() == 1
                    && Identifier.parse("lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE))
                    && Identifier.parse("lbc:penis_" + variant).equals(stack.get(DataComponents.ITEM_MODEL)),
                    "loose source maxstack/model/tier variant" + variant);
            require(flags.getIntOr("penis_num", 0) == variant && flags.getIntOr("instantitem", 0) == 1
                    && flags.getIntOr("penis_bomb", 0) == 1
                    && flags.getIntOr("govnoq", 0) == (variant == 2 ? 1 : 0), "exact loose flags variant" + variant);
            var nbt = saved(loose);
            require(nbt.getShortOr("Age", (short) 0) == -32768 && nbt.getShortOr("Health", (short) 0) == 10000
                    && nbt.getShortOr("PickupDelay", (short) 0) == 3 && loose.isInvulnerable(),
                    "native loose item Age/Health/PickupDelay/invulnerable");
            var velocity = loose.getDeltaMovement();
            for (double axis : new double[] { velocity.x, velocity.y, velocity.z })
                require(Math.abs(axis) <= .10001 && Math.abs(axis * 100 - Math.rint(axis * 100)) < 1e-7,
                        "discrete source velocity");
            loose.setNoGravity(true);
            loose.setDeltaMovement(Vec3.ZERO);
            ticks = 0;
        }

        void terminalBlock(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState value){
            force(pos.getX()>>4,pos.getZ()>>4);terminalTerrain.putIfAbsent(pos.immutable(),game.maps().snapshot(level,pos));level.setBlock(pos,value,2|16|32);
        }
        void terminalSetup(Arrow real){
            terminalTick=game.server().getTickCount();var start=new net.minecraft.core.BlockPos(128,310,24);
            terminalDrop=game.lbc().rollTable(level,Vec3.atCenterOf(start),alpha,"lbc:stand_arrow").getFirst();
            var ammo=terminalDrop.copy();
            for(int i=0;i<7;i++){
                var cell=start.offset(0,0,i*2);for(int x=0;x<4;x++)terminalBlock(cell.offset(x,0,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                if(i<6)terminalBlock(cell.offset(2,0,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow=i==0?real:i==2?new net.minecraft.world.entity.projectile.arrow.SpectralArrow(EntityTypes.SPECTRAL_ARROW,level):i==4?new Arrow(level,alpha,ammo,null):new Arrow(EntityTypes.ARROW,level);
                if(i!=0)arrow.setOwner(alpha);arrow.setNoGravity(true);arrow.setPos(Vec3.atCenterOf(i==3?cell.offset(2,0,0):cell));arrow.setDeltaMovement(i==3||i==6?Vec3.ZERO:new Vec3(4,0,0));fixtures.add(arrow);
                if(i!=5){arrow.entityTags().add("killedarrow");if(i!=6)terminalTagged.add(arrow);else terminalAir=arrow;}else terminalPlain=arrow;
                if(i!=0)require(level.addFreshEntity(arrow),"actual native terminal arrow admitted "+i);
            }
            require(real.getOwner()==first.hitbox()||real.getOwner()==second.hitbox(),"actual helicopter projectile retains NPC owner through terminal setup");
        }
        void terminalObserved(int now){
            if(terminalTagged.isEmpty()||terminalVerified||now==terminalTick)return;
            require(now==terminalTick+1,"terminal witnesses execute on first real native physics frame");
            for(var arrow:terminalTagged)require(arrow.isRemoved(),"tagged native boss/normal/spectral/embedded/payload arrow retires on impact "+arrow.getClass().getSimpleName()+" pos="+arrow.position());
            require(!terminalPlain.isRemoved()&&((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)terminalPlain).skywars2$isInGround(),"ordinary untagged arrow remains embedded with native pickup item");
            require(!terminalAir.isRemoved()&&!((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)terminalAir).skywars2$isInGround(),"terminal tag never deletes an airborne arrow");
            require(level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(128,308,30,134,313,35),item->ItemStack.isSameItemSameComponents(item.getItem(),terminalDrop)).size()==1,"actual119 impact publishes its one source drop before terminal cleanup");
            terminalPlain.discard();terminalAir.discard();terminalVerified=true;
        }
        boolean step() {
            require(game.playing(), "match remains active");
            int now = game.server().getTickCount();
            if (now == lastTick)
                return false;
            lastTick = now;
            terminalObserved(now);
            switch (phase) {
                case -3 -> {
                    // Nested performCommand queues /give until the enclosing /sw check returns.
                    var given = alpha.getInventory().getItem(0);
                    require(given.getCount() == 1
                            && ItemStack.isSameItemSameComponents(new ItemStack(LbcHelicopterTriggerItem.TOKEN), given),
                            "native give equals ported loot and prototype, got=" + given);
                    alpha.getInventory().clearContent();
                    input = token(level, origin, 3);
                    fixtures.add(input);
                    phase = -2;
                }
                case -2 -> {
                    require(service.activeCount() == 1 && !input.isRemoved() && input.getItem().getCount() == 2,
                            "actual deferred native dropped stack consumes first unit once per tick: pairs="
                                    + service.activeCount() + " count=" + input.getItem().getCount() + " removed="
                                    + input.isRemoved());
                    phase = -1;
                }
                case -1 -> {
                    require(service.activeCount() == 2 && !input.isRemoved() && input.getItem().getCount() == 1,
                            "same real dropped stack consumes second unit on next tick");
                    phase = -4;
                }
                case -4 -> {
                    require(service.activeCount() == 3 && input.isRemoved(),
                            "third native tick consumes final unit and removes dropped stack");
                    service.stop();
                    phase = 0;
                }
                case 0 -> {
                    first = spawn(origin, 3);
                    visual(first, origin.add(0, 20, 50));
                    second = spawn(origin.add(64, 0, 0), 3);
                    visual(second, origin.add(64, 20, 50));
                    require(first.display() != second.display() && first.hitbox() != second.hitbox(),
                            "two independent direct UUID pairs");
                    var bolt = new net.minecraft.world.entity.LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
                    var lightning = new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                            .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getOrThrow(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT), bolt,
                            first.hitbox());
                    require(game.items().allowDamage(first.hitbox(), lightning)
                            && game.items().allowDamage(second.hitbox(), lightning),
                            "native lightning remains WILD for own and other boss hitboxes");
                    for (int x = 24; x <= 26; x++)
                        for (int z = 27; z <= 29; z++)
                            force(x, z);
                    remote = spawn(new Vec3(400.5, 260, 400.5), 2);
                    var ordinary = new ItemEntity(level, 0, 280, 0, new ItemStack(Items.PAPER));
                    require(!service.spawn(ordinary), "unmarked drops rejected");
                    ordinary.discard();
                    dispatches = first.dispatches();
                    ticks = 0;
                    phase = 1;
                }
                case 1 -> {
                    if (remote != null) {
                        var far = service.snapshot().stream().filter(v -> v.hitbox() == remote.hitbox()).findFirst()
                                .orElseThrow();
                        require(far.dispatches() == 1
                                && Math.abs(far.display().position().distanceTo(new Vec3(400.5, 280, 450.5)) - 1) < .001
                                && far.display().getX() < 400.49,
                                "target beyond100 transitions phase1 to blended phase2 steering from original command origin in same dispatch; actual="
                                        + far.display().position());
                        require(Math.abs(far.display().getXRot()) < .001 && Math.abs(far.display().getY() - 280) < .001,
                                "phase2 source steering flattens pitch and preserves altitude");
                        service.entityUnloaded(far.display());
                        remote = null;
                    }
                    var view = service.snapshot().stream().filter(v -> v.hitbox() == first.hitbox()).findFirst()
                            .orElseThrow();
                    require(view.dispatches() == dispatches + 1, "one encounter dispatch per ordinary server tick");
                    dispatches = view.dispatches();
                    require(view.frame() == view.dispatches() % 4 + 1
                            && saved(view.display()).toString().contains("lbc:helicopter_" + view.frame()),
                            "approved complete four-frame native model cycle");
                    require(view.hitbox().position().distanceTo(view.display().position().add(0, -3, 0)) < .001,
                            "pair attachment stays native and isolated");
                    var shots = service.attackSnapshot().stream().filter(e -> e instanceof Arrow).map(e -> (Arrow) e)
                            .toList();
                    for (var shot : shots) {
                        require(shot.getOwner() == first.hitbox() || shot.getOwner() == second.hitbox(),
                                "native arrow ownership is exact NPC reference");
                        require(saved(shot).getDoubleOr("damage", -1) == 1 && shot.entityTags().contains("killedarrow"),
                                "source native arrow damage/tag");
                        var previousOwner = shot.getOwner();
                        shot.setOwner(alpha);
                        require(shot.getOwner() == alpha, "native ownership transfer replaces boss cause");
                        shot.setOwner(previousOwner);
                        require(shot.getOwner() == previousOwner, "native ownership transfer restores exact cause");
                        require(!service.canHit(shot, shot.getOwner()), "own hitbox excluded");
                        var protectedCow = cow(new Vec3(0, 280, 0));
                        protectedCow.entityTags().add("magic_academy");
                        require(!service.canHit(shot, protectedCow), "service NPC excluded");
                        protectedCow.discard();
                        require(service.canHit(shot, alpha) && service.canHit(shot, beta),
                                "hostile boss can target both active players");
                        game.spectators().enter(beta);
                        require(!service.canHit(shot, beta),
                                "native observer role excluded even outside spectator camera");
                        game.spectators().remove(beta);
                        beta.setGameMode(GameType.SURVIVAL);
                        move(beta, new Vec3(128.5, 279, 24.5));
                    }
                    if(terminalTagged.isEmpty()&&!shots.isEmpty())terminalSetup(shots.getFirst());
                    if (!shots.isEmpty() && ++ticks >= 3 && terminalVerified) {
                        game.items().timeStop().begin(level, 200);
                        ticks = 0;
                        phase = 2;
                    } else
                        require(++ticks < 40, "native AI emits actual arrow on ordinary ticks");
                }
                case 2 -> {
                    var view = service.snapshot().stream().filter(v -> v.hitbox() == first.hitbox()).findFirst()
                            .orElseThrow();
                    require(view.dispatches() == dispatches, "global timestop pauses encounter source dispatch");
                    if (++ticks == 2) {
                        game.items().timeStop().end(level);
                        phase = 3;
                    }
                }
                case 3 -> {
                    cleanAttacks();
                    causal = first.hitbox();
                    var old = new HashSet<Entity>();
                    level.getAllEntities().forEach(old::add);
                    first.hitbox().hurtServer(level, first.hitbox().damageSources().genericKill(), 10000);
                    require(first.display().isRemoved() && first.hitbox().isRemoved() && service.activeCount() == 1,
                            "native death atomically removes only its own pair");
                    var burst = service.attackSnapshot().stream()
                            .filter(e -> e instanceof PrimedTnt t && t.getOwner() == causal).map(e -> (PrimedTnt) e)
                            .toList();
                    require(burst.size() == 30, "literal thirty death TNT including duplicate offsets");
                    for (var tnt : burst)
                        require(tnt.getFuse() == 1000 && tnt.entityTags().contains("groundbam")
                                && tnt.getOwner() == causal, "death TNT retains dead NPC causal source");
                    var reward = new ArrayList<ItemEntity>();
                    level.getAllEntities().forEach(e -> {
                        if (!old.contains(e) && e instanceof ItemEntity item
                                && LbcShooters.isBossHelicopter(item.getItem()))
                            reward.add(item);
                    });
                    require(reward.size() == 1, "exactly one qualified real reward direct reference");
                    var drop = reward.getFirst();
                    require(drop.isInvulnerable() && drop.isNoGravity() && drop.isCurrentlyGlowing()
                            && saved(drop).getShortOr("Age", (short) 0) == -32768, "actual reward decorated directly");
                    var accepted = game.lbc().rollTable(level, drop.position(), alpha, "lbc:ported/helicopter_boss");
                    require(accepted.stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, drop.getItem())),
                            "actual reward equals qualified native payload");
                    service.died(first.hitbox(), first.hitbox().damageSources().genericKill());
                    require(service.attackSnapshot().stream()
                            .filter(e -> e instanceof PrimedTnt t && t.getOwner() == causal).count() == 30,
                            "repeated terminal callback cannot duplicate burst");
                    drop.discard();
                    cleanAttacks();
                    service.entityUnloaded(second.display());
                    require(second.display().isRemoved() && second.hitbox().isRemoved() && service.activeCount() == 0,
                            "unload quietly clears exact pair");
                    looseSetup();
                    phase = 4;
                }
                case 4 -> {
                    if (!loose.isRemoved()) {
                        require(++ticks < 8, "loose item activates through native contact ticks");
                        break;
                    }
                    switch (variant) {
                        case 1 ->
                            require(victim.getHealth() == 194 && victim.getLastDamageSource().getEntity() == causal,
                                    "normal6 arrow damage keeps dead boss source");
                        case 2 -> require(
                                victim.getHealth() == 196 && victim.hasEffect(MobEffects.SLOWNESS)
                                        && victim.getEffect(MobEffects.SLOWNESS).getAmplifier() == 5
                                        && victim.hasEffect(MobEffects.WEAKNESS) && victim.hasEffect(MobEffects.NAUSEA),
                                "dirty4 damage and VI source effects");
                        case 3 ->
                            require(victim.getHealth() < 200 && victim.getLastDamageSource().getEntity() == causal,
                                    "protected native explosion keeps NPC source");
                        case 4 -> require(
                                victim.hasEffect(MobEffects.BLINDNESS)
                                        && victim.getEffect(MobEffects.BLINDNESS).getAmplifier() == 1
                                        && victim.hasEffect(MobEffects.LEVITATION)
                                        && victim.getEffect(MobEffects.LEVITATION).getAmplifier() == 15,
                                "holy source blindnessII/levitationXVI");
                        case 5 -> require(
                                victim.getHealth() == 190 && victim.getLastDamageSource().getEntity() == causal
                                        && victim.position().distanceTo(before.add(0, 5, -5)) < .2
                                        && victim.getEffect(MobEffects.SLOWNESS).getAmplifier() == 127,
                                "horse10 damage/displacement/slowness128: hp=" + victim.getHealth() + " position="
                                        + victim.position() + " expected=" + before.add(0, 5, -5) + " yaw="
                                        + victim.getYRot() + " effect=" + victim.getEffect(MobEffects.SLOWNESS)
                                        + " source=" + victim.getLastDamageSource());
                        case 6 -> require(bystander.getHealth() == 200, "multicast never turns into area damage");
                    }
                    if (variant < 6) {
                        variant++;
                        looseSetup();
                    } else {
                        ticks = 0;
                        phase = 5;
                    }
                }
                case 5 -> {
                    require(bystander.getHealth() == 200, "native multicast repetitions affect same executor only");
                    if (++ticks < 3)
                        break;
                    require(victim.getHealth() < 200 && victim.getLastDamageSource().getEntity() == causal,
                            "native multicast worker retains dead boss ownership");
                    victim.discard();
                    bystander.discard();
                    loose = game.items().kfcRewards().spawnLooseBomb(causal, alpha.position().add(4, 0, 0), 1);
                    fixtures.add(loose);
                    loose.setNoGravity(true);
                    loose.setDeltaMovement(Vec3.ZERO);
                    alpha.setHealth(alpha.getMaxHealth());
                    ticks = 0;
                    phase = 6;
                }
                case 6 -> {
                    if (++ticks < 4)
                        break;
                    loose.playerTouch(alpha);
                    require(loose.isRemoved() && alpha.getLastDamageSource().getEntity() == causal,
                            "native pickup after delay preserves dead boss damage ownership");
                    var pair = spawn(origin, 2);
                    service.stop();
                    require(pair.display().isRemoved() && pair.hitbox().isRemoved() && service.activeCount() == 0
                            && service.attackSnapshot().isEmpty(), "quiet stop leaves no pairs or managed attacks");
                    game.items().kfcRewards().stop();
                    second = spawn(origin, 2);
                    beaconPos = second.display().blockPosition().below(5);
                    beaconSaved = game.maps().snapshot(beaconPos);
                    level.setBlock(beaconPos, LbcMagic.MECHANISM_BEACON_BLOCK.defaultBlockState(), 3);
                    LbcMagic.MECHANISM_BEACON_BLOCK.setPlacedBy(level, beaconPos, level.getBlockState(beaconPos), alpha,
                            new ItemStack(LbcMagic.MECHANISM_BEACON));
                    game.items().magic().blockChanged(beaconPos, level.getBlockState(beaconPos));
                    ticks = 0;
                    phase = 7;
                }
                case 7 -> {
                    if (!second.display().isRemoved()) {
                        require(++ticks < 5, "native beacon destroys paired boss on ordinary ticks");
                        break;
                    }
                    require(second.hitbox().isRemoved() && service.activeCount() == 0,
                            "approved mechanism destruction removes complete pair");
                    require(service.attackSnapshot().stream()
                            .filter(e -> e instanceof PrimedTnt t && t.getOwner() == second.hitbox()).count() == 30,
                            "mechanism destruction gets normal thirty-TNT death");
                    var drops = new ArrayList<ItemEntity>();
                    level.getAllEntities().forEach(e -> {
                        if (e instanceof ItemEntity item && !initial.contains(item)
                                && LbcShooters.isBossHelicopter(item.getItem()) && !item.isRemoved())
                            drops.add(item);
                    });
                    require(drops.size() == 1 && drops.getFirst().isNoGravity() && drops.getFirst().isInvulnerable(),
                            "mechanism destruction awards exactly one decorated real reward");
                    return true;
                }
                default -> throw new IllegalStateException("Unknown phase " + phase);
            }
            return false;
        }

        public void close() {
            game.items().timeStop().end(level);
            service.stop();
            game.items().kfcRewards().stop();
            fixtures.forEach(Entity::discard);
            for(var entry:terminalTerrain.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            for (long key : forcedChunks)
                level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),
                        net.minecraft.world.level.ChunkPos.getZ(key), false);
            if (beaconSaved != null) {
                game.maps().restoreSnapshot(beaconPos, beaconSaved);
                game.items().magic().blockChanged(beaconPos, level.getBlockState(beaconPos));
            }
            var created = new ArrayList<Entity>();
            level.getAllEntities().forEach(e -> {
                if (!initial.contains(e) && !(e instanceof ServerPlayer))
                    created.add(e);
            });
            created.forEach(Entity::discard);
            alphaSaved.restore(game);
            betaSaved.restore(game);
        }
    }

    public static void run(GameController game) {
        require(running == null, "suite already running");
        running = new State(game);
    }

    public static void tick(GameController game) {
        if (running == null)
            return;
        try {
            if (running.step()) {
                running.close();
                running = null;
                SkyWars2.LOGGER.info("[check] PASS helicopter-encounter");
            }
        } catch (Throwable error) {
            try {
                running.close();
            } finally {
                running = null;
            }
            SkyWars2.LOGGER.error("[check] FAIL helicopter-encounter", error);
        }
    }

    public static void stopProbe(GameController game) {
        var level = game.maps().level();
        var service = game.items().helicopterEncounter();
        require(game.playing(), "stop probe requires actual match");
        service.stop();
        var at = new Vec3(64.5, 260, 14.5);
        level.getChunk(4, 4);
        var item = token(level, at, 1);
        require(service.spawn(item), "stop probe native pair");
        var entities = new ArrayList<Entity>();
        for (var pair : service.snapshot()) {
            entities.add(pair.display());
            entities.add(pair.hitbox());
        }
        entities.addAll(service.attackSnapshot());
        stopEntities = List.copyOf(entities);
        stopEpoch = game.items().epoch();
        SkyWars2.LOGGER.info("[check] PASS helicopter-encounter-stop-probe");
    }

    public static void cleanup(GameController game) {
        require(!game.items().currentEpoch(stopEpoch) && game.items().helicopterEncounter().activeCount() == 0
                && game.items().helicopterEncounter().attackSnapshot().isEmpty(),
                "real match stop invalidates encounter epoch and attacks");
        require(stopEntities.stream().allMatch(Entity::isRemoved), "real match stop leaves no paired orphan");
        stopEntities = List.of();
        stopEpoch = null;
        SkyWars2.LOGGER.info("[check] PASS helicopter-encounter-cleanup");
    }
}
