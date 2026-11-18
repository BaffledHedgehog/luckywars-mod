package me.skywars2.game;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.game.genie.GenieWorldHost;
import me.skywars2.game.genie.script.*;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Actual native commands, selectors and collision shapes, using the existing owned Genie fixture. */
final class GenieWorldChecks {
    private GenieWorldChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Genie world API: "+why);}
    private static void rejected(Runnable action,String why){
        try{action.run();throw new IllegalStateException("Genie world API: "+why);}catch(IllegalArgumentException expected){}
    }
    private static JsonArray vector(Vec3 v){var a=new JsonArray();a.add(v.x);a.add(v.y);a.add(v.z);return a;}
    private static ScriptMachine.Pending queued(GenieWorldHost host,String method,JsonElement...args){
        var result=host.call(method,List.of(args));require(result instanceof ScriptMachine.Pending,"world action is queued");
        return (ScriptMachine.Pending)result;
    }
    private static JsonElement complete(GenieWorldHost host,ScriptMachine.Pending work){
        require(!work.future().isDone(),"no world action before scheduler admission");
        require(host.drain(1,Long.MAX_VALUE)==1&&work.future().isDone(),"one native job completes in its owned budget");
        return work.future().join();
    }
    private static void authorityScope(GameController game,WoolBucketChecks.Fixture base,
                                       GenieWorldHost host,BlockPos start){
        var foreign=java.util.stream.StreamSupport.stream(game.server().getAllLevels().spliterator(),false)
            .filter(level->level!=base.level).findFirst().orElseThrow();
        var witness=base.other;
        var before=witness.position();var original=witness.level();
        float yaw=witness.getYRot(),pitch=witness.getXRot();var movement=witness.getDeltaMovement();
        String marker="skywars2.genie.authority.fixture";
        String uuid=witness.getUUID().toString(),playerName=witness.getGameProfile().name();
        try{
            require(witness.teleportTo(foreign,before.x,before.y,before.z,Set.of(),yaw,pitch,true),"native foreign-dimension participant");
            base.audience.acknowledge();
            var scoped=complete(host,queued(host,"world.select",new JsonPrimitive("@a"))).getAsJsonArray();
            require(scoped.contains(new JsonPrimitive(base.owner.getUUID().toString()))&&!scoped.contains(new JsonPrimitive(uuid)),
                "local player remains visible, foreign player is excluded");
            for(String lookup:List.of(uuid,playerName)){
                var selected=complete(host,queued(host,"world.select",new JsonPrimitive(lookup))).getAsJsonArray();
                require(selected.isEmpty(),"explicit identity still follows scoped selector authority");
                var tagged=complete(host,queued(host,"world.command",new JsonPrimitive("tag "+lookup+" add "+marker))).getAsJsonObject();
                require(tagged.get("successes").getAsInt()==0&&!witness.entityTags().contains(marker),"raw named or UUID target remains untouched");
            }
            require(complete(host,queued(host,"entity.info",new JsonPrimitive(uuid))).isJsonNull(),"foreign UUID is outside item world info");
            var nested=complete(host,queued(host,"world.command",new JsonPrimitive("execute as @a run tag @s add "+marker))).getAsJsonObject();
            require(nested.get("successes").getAsInt()>0&&base.owner.entityTags().contains(marker)&&!witness.entityTags().contains(marker),
                "execute child keeps its source dimension");
            try{
                var normal=game.server().createCommandSourceStack().withLevel(base.level);
                var unrestricted=net.minecraft.commands.arguments.EntityArgument.players().parse(new com.mojang.brigadier.StringReader("@a"));
                require(unrestricted.findPlayers(normal).contains(witness),"ordinary selectors keep native cross-dimension lookup");
            }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
            rejected(()->host.validateCommand("clone from "+foreign.dimension().identifier()+" ~ ~ ~ ~ ~ ~ ~ ~1 ~"),
                "cross-dimension clone argument accepted");
        }finally{
            base.owner.entityTags().remove(marker);witness.entityTags().remove(marker);
            require(witness.teleportTo(original,before.x,before.y,before.z,Set.of(),yaw,pitch,true),"foreign-dimension witness restored");
            witness.setDeltaMovement(movement);base.audience.acknowledge();
        }

        host.validateCommand("give @s minecraft:bundle[bundle_contents=[{id:\"minecraft:stone\",count:1}]]");
        rejected(()->host.validateCommand("give @s minecraft:bundle[bundle_contents=[{id:\"minecraft:structure_block\",count:1}]]"),
            "nested item payload bypassed typed input checks");
        host.validateCommand("setblock ~ ~ ~ minecraft:chest{Items:[{Slot:0b,id:\"minecraft:stone\",count:1}]}");
        rejected(()->host.validateCommand("setblock ~ ~ ~ minecraft:chest{Items:[{Slot:0b,id:\"minecraft:structure_block\",count:1}]}"),
            "typed block entity payload bypassed nested checks");

        var from=start.offset(1,1,0);var to=start.offset(7,1,0);
        base.set(from,Blocks.STONE.defaultBlockState());
        base.set(to,Blocks.AIR.defaultBlockState());
        var ok=complete(host,queued(host,"world.command",new JsonPrimitive("clone "+coordinates(from)+" "+coordinates(from)+" "+coordinates(to)))).getAsJsonObject();
        require(ok.get("successes").getAsInt()==1&&base.level.getBlockState(to).is(Blocks.STONE),"real scoped clone of ordinary block");
        var control=from.east();
        base.set(control,Blocks.STRUCTURE_BLOCK.defaultBlockState());base.set(to,Blocks.AIR.defaultBlockState());
        base.set(to.east(),Blocks.AIR.defaultBlockState());
        var denied=complete(host,queued(host,"world.command",new JsonPrimitive("execute positioned "+coordinates(from)+
            " run clone ~ ~ ~ ~1 ~ ~ "+coordinates(to)))).getAsJsonObject();
        require(denied.get("successes").getAsInt()==0&&base.level.getBlockState(to).isAir()&&base.level.getBlockState(to.east()).isAir(),
            "native clone rejects selected control block before mutating even ordinary preceding cells");
        var fill=complete(host,queued(host,"world.command",new JsonPrimitive("fill "+coordinates(from)+" "+
            coordinates(control)+" minecraft:air destroy"))).getAsJsonObject();
        require(fill.get("successes").getAsInt()==0&&base.level.getBlockState(from).is(Blocks.STONE)
            &&base.level.getBlockState(control).is(Blocks.STRUCTURE_BLOCK),"fill rejects protected destinations before destroying any selected cell");
        var single=complete(host,queued(host,"world.command",new JsonPrimitive("setblock "+coordinates(control)+
            " minecraft:air destroy"))).getAsJsonObject();
        require(single.get("successes").getAsInt()==0&&base.level.getBlockState(control).is(Blocks.STRUCTURE_BLOCK),
            "single destroy preserves native control destination");
        var filtered=complete(host,queued(host,"world.command",new JsonPrimitive("fill "+coordinates(from)+" "+
            coordinates(control)+" minecraft:dirt replace minecraft:stone"))).getAsJsonObject();
        require(filtered.get("successes").getAsInt()==1&&base.level.getBlockState(from).is(Blocks.DIRT)
            &&base.level.getBlockState(control).is(Blocks.STRUCTURE_BLOCK),"filtered local fill still edits its ordinary selected target");
        base.set(from,Blocks.AIR.defaultBlockState());base.set(control,Blocks.AIR.defaultBlockState());
        structure(base,host,to);
        gameItemTransfers(base,host,to);
    }

    private static void structure(WoolBucketChecks.Fixture fixture, GenieWorldHost host, BlockPos at) {
        try {
            var template = TagParser.parseCompoundFully("{size:[2,1,1],palette:[{Name:'minecraft:iron_block'},"
                    + "{Name:'minecraft:oak_log',Properties:{axis:'x'}}],blocks:[{pos:[0,0,0],state:0},{pos:[1,0,0],state:1}],entities:[]}");
            var file = new java.io.ByteArrayOutputStream();
            NbtIo.writeCompressed(template, file);
            var archive = new JsonPrimitive("nbt:" + Base64.getEncoder().encodeToString(file.toByteArray()));
            var base = fixture.owner.blockPosition();
            var relative = new JsonArray();
            relative.add(at.getX()-base.getX());relative.add(at.getY()-base.getY());relative.add(at.getZ()-base.getZ());
            fixture.set(at,Blocks.AIR.defaultBlockState());
            fixture.set(at.east(),Blocks.STRUCTURE_BLOCK.defaultBlockState());
            require(complete(host,queued(host,"structure.place",archive,relative)).getAsInt()==0
                    &&fixture.level.getBlockState(at).isAir()&&fixture.level.getBlockState(at.east()).is(Blocks.STRUCTURE_BLOCK),
                    "relative template preflights every target before any block changes");
            fixture.set(at.east(),Blocks.AIR.defaultBlockState());
            require(complete(host,queued(host,"structure.place",archive,relative)).getAsInt()==2
                    &&fixture.level.getBlockState(at).is(Blocks.IRON_BLOCK)
                    &&fixture.level.getBlockState(at.east()).getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)==net.minecraft.core.Direction.Axis.X,
                    "real gzip .nbt placed both cells at the offset with the authored block state");
            fixture.set(at,Blocks.AIR.defaultBlockState());
            fixture.set(at.east(),Blocks.AIR.defaultBlockState());
        } catch (java.io.IOException | com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void gameItemTransfers(WoolBucketChecks.Fixture base,GenieWorldHost host,BlockPos box){
        base.set(box,Blocks.SHULKER_BOX.defaultBlockState());
        var source=(net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity)base.level.getBlockEntity(box);
        var inventory=base.owner.getInventory();
        var original=inventory.getItem(7);
        String copy="item replace entity @s hotbar.7 from block "+coordinates(box)+" container.0";
        String loot="loot replace entity @s hotbar.7 mine "+coordinates(box);
        try{
            source.setItem(0,new ItemStack(Items.STONE,3));
            var allowed=complete(host,queued(host,"world.command",new JsonPrimitive(copy))).getAsJsonObject();
            require(allowed.get("successes").getAsInt()==1&&inventory.getItem(7).is(Items.STONE)&&inventory.getItem(7).getCount()==3,
                "actual native inventory transfer preserves ordinary item and count");
            source.setItem(0,new ItemStack(Items.STRUCTURE_BLOCK));
            var denied=complete(host,queued(host,"world.command",new JsonPrimitive(copy))).getAsJsonObject();
            require(denied.get("successes").getAsInt()==0&&inventory.getItem(7).is(Items.STONE)
                &&source.getItem(0).is(Items.STRUCTURE_BLOCK),"materialized stack is checked before target write");
            denied=complete(host,queued(host,"world.command",new JsonPrimitive(loot))).getAsJsonObject();
            require(denied.get("successes").getAsInt()==0&&inventory.getItem(7).is(Items.STONE),
                "real generated loot with restricted nested payload is checked before distribution");
            source.setItem(0,new ItemStack(Items.STONE,2));
            allowed=complete(host,queued(host,"world.command",new JsonPrimitive(loot))).getAsJsonObject();
            require(allowed.get("successes").getAsInt()==1&&inventory.getItem(7).is(Items.SHULKER_BOX),
                "ordinary generated loot remains usable");
            String modify="item modify entity @s hotbar.7 {type:\"minecraft:set_count\",count:2}";
            inventory.setItem(7,new ItemStack(Items.STRUCTURE_BLOCK));
            denied=complete(host,queued(host,"world.command",new JsonPrimitive(modify))).getAsJsonObject();
            require(denied.get("successes").getAsInt()==0&&inventory.getItem(7).getCount()==1,
                "modifier skips a protected original instead of writing it back");
            inventory.setItem(7,new ItemStack(Items.STONE));
            allowed=complete(host,queued(host,"world.command",new JsonPrimitive(modify))).getAsJsonObject();
            require(allowed.get("successes").getAsInt()==1&&inventory.getItem(7).getCount()==2,
                "ordinary native modifier still changes a slot");
            String forbiddenOutput="item modify entity @s hotbar.7 {type:\"minecraft:set_item\",item:\"minecraft:structure_block\"}";
            denied=complete(host,queued(host,"world.command",new JsonPrimitive(forbiddenOutput))).getAsJsonObject();
            require(denied.get("successes").getAsInt()==0&&inventory.getItem(7).is(Items.STONE)
                &&inventory.getItem(7).getCount()==2,"modifier validates produced stack before slot write");
        }finally{
            inventory.setItem(7,original);
            source.clearContent();base.set(box,Blocks.AIR.defaultBlockState());
        }
    }

    private static String coordinates(BlockPos pos){return pos.getX()+" "+pos.getY()+" "+pos.getZ();}

    private static void directedCasts(GameController game, WoolBucketChecks.Fixture base, Vec3 origin) {
        var wands = game.items().wands();
        var initialTraces = wands.electroSnapshot();
        var main = base.owner.getMainHandItem();
        var off = base.owner.getOffhandItem();
        var weapon = new ItemStack(Items.IRON_SWORD);
        weapon.enchant(base.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 3);
        var aim = new Vec3(2, 0, 0);
        var arc = new JsonPrimitive("skywars2:electro_arc");
        var victim = Objects.requireNonNull(EntityTypes.COW.create(base.level, EntitySpawnReason.TRIGGERED));
        victim.setPos(origin.add(1.5, -.65, 0));
        victim.setNoAi(true);
        require(base.level.addFreshEntity(victim), "native directed cast target");
        base.own(victim);
        try (var host = new GenieWorldHost(game, base.owner, weapon, () -> true, () -> 0)) {
            var pending = queued(host, "lbc.cast", arc, vector(origin), vector(aim));
            require(wands.electroSnapshot().equals(initialTraces), "deferred cast emits nothing before drain");
            require(complete(host, pending).getAsBoolean(), "directed arc accepted");
            var traces = wands.electroSnapshot().stream().filter(trace -> !initialTraces.contains(trace)).toList();
            require(traces.size() == 1, "single native Electro arc");
            var trace = traces.getFirst();
            require(trace.owner == base.owner && trace.position.equals(origin) && trace.lifetime == 40
                    && Vec3.directionFromRotation(trace.pitch, trace.yaw).distanceTo(aim.normalize()) < 1e-5,
                "native arc keeps explicit origin and aim");
            require(trace.cause != null && trace.cause.player().equals(base.owner.getUUID())
                    && ItemStack.matches(trace.cause.enchantmentWeapon(), weapon),
                "the delayed arc carries the originating item's enchantments");
            float health = victim.getHealth();
            wands.tick();
            require(victim.getHealth() < health, "native arc actually hits along the supplied direction");

            var terra = new JsonPrimitive("skywars2:terra_charge");
            require(complete(host, queued(host, "lbc.cast", terra, vector(origin), vector(aim))).getAsBoolean(),
                "real Terra launch accepted");
            var shots = base.level.getEntitiesOfClass(LbcTerra.TerraProjectile.class, new AABB(origin, origin).inflate(1),
                shot -> shot.getOwner() == base.owner);
            require(shots.size() == 1, "one actual native Terra projectile published");
            var shot = shots.getFirst();
            base.own(shot);
            try {
                require(shot.position().equals(origin) && shot.getDeltaMovement().distanceTo(new Vec3(1.8, 0, 0)) < 1e-5,
                    "Terra keeps native speed at the supplied normalized aim");
                var damage = new net.minecraft.world.damagesource.DamageSource(
                    base.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getOrThrow(net.minecraft.world.damagesource.DamageTypes.CACTUS), shot, base.owner);
                require(ItemStack.matches(game.items().indirectKills().damageWeapon(damage), weapon)
                        && ItemStack.matches(game.items().indirectKills().enchantmentWeapon(damage), weapon),
                    "native projectile damage and loot can recover the captured weapon snapshot");
            } finally {
                base.discard(shot);
            }

            var variants = java.util.Map.of(
                    "academy_terra_charge", LbcTerra.Kind.ACADEMY,
                    "terra_sword_charge", LbcTerra.Kind.SWORD,
                    "true_terra_charge", LbcTerra.Kind.TRUE_CHARGE,
                    "true_terra_beam", LbcTerra.Kind.TRUE_BEAM,
                    "terra_razor", LbcTerra.Kind.RAZOR);
            for (var variant : variants.entrySet()) {
                require(complete(host, queued(host, "lbc.cast", new JsonPrimitive("skywars2:" + variant.getKey()),
                        vector(origin), vector(aim))).getAsBoolean(), "native Terra variant accepted: " + variant.getKey());
                var kind = variant.getValue();
                var projectile = base.level.getEntitiesOfClass(LbcTerra.TerraProjectile.class,
                        new AABB(origin, origin).inflate(1), body -> body.getOwner() == base.owner).getFirst();
                try {
                    var velocity = projectile.getDeltaMovement();
                    Vec3 expected = aim.normalize().scale(kind.speed);
                    double tolerance = 1e-5;
                    if (kind == LbcTerra.Kind.RAZOR) {
                        require(Math.abs(projectile.getYRot() + 90) <= 3.0001 && Math.abs(projectile.getXRot()) <= 3.0001,
                                "native Razor keeps its three-degree spread on both axes");
                        expected = Vec3.directionFromRotation(projectile.getXRot(), projectile.getYRot()).scale(kind.speed);
                        // Rounding the stored yaw can advance one entry of the native 65536-step sine table.
                        tolerance += kind.speed * (Math.TAU / 65536);
                    }
                    require(WoolBucketChecks.read(projectile, "kind") == kind
                            && kind.model.equals(projectile.display().getItem().get(net.minecraft.core.component.DataComponents.ITEM_MODEL))
                            && velocity.distanceTo(expected) < tolerance,
                            "native model, velocity and aim match Terra variant " + kind + " actual=" + velocity + " expected=" + expected);
                } finally {
                    base.discard(projectile);
                }
            }

            base.field(false, origin);
            require(game.items().magic().suppressedAt(base.level, origin), "native field covers requested position");
            require(!complete(host, queued(host, "lbc.cast", arc, vector(origin), vector(aim))).getAsBoolean(),
                "native magic suppression denies directed launch");
            var absent = new Vec3(1048576.25, origin.y, 1048576.25);
            require(!complete(host, queued(host, "lbc.cast", terra, vector(absent), vector(aim))).getAsBoolean()
                    && !base.level.hasChunkAt(BlockPos.containing(absent)),
                "directed cast does not load an absent chunk");
            require(base.owner.getMainHandItem() == main && base.owner.getOffhandItem() == off,
                "script casts never replace either physical hand");
        } finally {
            base.removeFields();
            base.discard(victim);
            wands.electroSnapshot().stream().filter(trace -> !initialTraces.contains(trace))
                .forEach(trace -> trace.age = trace.lifetime);
        }
    }

    private static void targetedOreshnik(GameController game, WoolBucketChecks.Fixture base, Vec3 target) {
        var rockets = game.items().cinema().rockets();
        require(rockets.flightCount() == 0 && rockets.blastCount() == 0, "Oreshnik fixture starts empty");
        var id = new JsonPrimitive(GenieNative.ORESHNIK.toString());
        var captured = new ItemStack(Items.SPYGLASS);
        try (var host = new GenieWorldHost(game, base.owner, captured, () -> true, () -> 0)) {
            var absent = new Vec3(1048576.25, target.y, 1048576.25);
            require(!complete(host, queued(host, "lbc.cast_at", id, vector(absent))).getAsBoolean()
                    && !base.level.hasChunkAt(BlockPos.containing(absent)), "an unloaded target does not schedule a rocket or load terrain");
            base.field(true, target);
            require(game.items().magic().mechanismSuppressedAt(base.level, target)
                    && !complete(host, queued(host, "lbc.cast_at", id, vector(target))).getAsBoolean(),
                    "the native mechanism jammer still blocks the targeted ability");
            base.removeFields();
            base.field(false, target);
            require(game.items().magic().suppressedAt(base.level, target), "the ordinary antimagic control covers the target");
            var code = ScriptJson.parse("""
                {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
                ["const","%s"],["const",%s],["call","lbc.cast_at",2],["return"]]}}}
                """.formatted(id.getAsString(), vector(target)));
            var program = ScriptProgram.compile(code, GenieWorldHost.api(), Set.of("USE"));
            var fiber = new ScriptMachine(program, "main", List.of(new JsonObject()), program.initialState(), host);
            require(fiber.advance(0, 100) == ScriptMachine.Status.WAITING && rockets.flightCount() == 0,
                    "compiled USE handler queues the ability until native work is admitted");
            require(host.drain(1, Long.MAX_VALUE) == 1 && fiber.advance(1, 100) == ScriptMachine.Status.DONE
                    && fiber.result().getAsBoolean() && rockets.flightCount() == 1, "script runs the original single Oreshnik approach");
            var flight = ((SequencedSet<?>) WoolBucketChecks.read(rockets, "flights")).getFirst();
            var cause = (LbcIndirectKills.Owner) WoolBucketChecks.read(flight, "cause");
            require(target.equals(WoolBucketChecks.read(flight, "target")) && cause.player().equals(base.owner.getUUID())
                    && ItemStack.matches(cause.enchantmentWeapon(), captured), "native flight keeps the exact target, owner and item attribution");
        } finally {
            base.removeFields();
            rockets.stop();
        }
    }

    private static void targetedHedgehogs(GameController game, WoolBucketChecks.Fixture base, Vec3 target) {
        var service = game.items().hedgehog();
        var initial = service.views().stream().map(LbcHedgehog.View::entity).collect(java.util.stream.Collectors.toSet());
        var modes = java.util.Map.of(
                GenieNative.TargetedCast.HEDGEHOG, LbcHedgehog.Kind.ORDINARY,
                GenieNative.TargetedCast.HEDGEHOG_STANDARD, LbcHedgehog.Kind.DEFAULT,
                GenieNative.TargetedCast.HEDGEHOG_HEALER, LbcHedgehog.Kind.HEALER,
                GenieNative.TargetedCast.HEDGEHOG_TANK, LbcHedgehog.Kind.TANK,
                GenieNative.TargetedCast.HEDGEHOG_SNIPER, LbcHedgehog.Kind.SNIPER,
                GenieNative.TargetedCast.HEDGEHOG_LASER, LbcHedgehog.Kind.LASER);
        try (var host = new GenieWorldHost(game, base.owner, new ItemStack(Items.STICK), () -> true, () -> 0)) {
            var sniper = new JsonPrimitive(GenieNative.TargetedCast.HEDGEHOG_SNIPER.id.toString());
            base.field(false, target);
            require(!complete(host, queued(host, "lbc.cast_at", sniper, vector(target))).getAsBoolean(),
                    "normal antimagic denies a magic summon");
            base.removeFields();
            var cell = BlockPos.containing(target);
            base.set(cell, Blocks.STONE.defaultBlockState());
            require(!complete(host, queued(host, "lbc.cast_at", sniper, vector(target))).getAsBoolean()
                    && service.views().stream().allMatch(view -> initial.contains(view.entity())),
                    "no summon is admitted inside a solid block");
            base.set(cell, Blocks.AIR.defaultBlockState());
            for (var mode : modes.entrySet()) {
                require(complete(host, queued(host, "lbc.cast_at", new JsonPrimitive(mode.getKey().id.toString()),
                        vector(target))).getAsBoolean(), "native helper accepted: " + mode.getKey());
                var view = service.views().stream().filter(v -> !initial.contains(v.entity()))
                        .filter(v -> base.level.getEntity(v.entity()) != null).findFirst().orElseThrow();
                require(view.kind() == mode.getValue() && view.owner().equals(base.owner.getUUID())
                        && view.point().equals(target), "native helper keeps exact variant, owner and target");
                base.discard(base.level.getEntity(view.entity()));
            }
            for (var pos : BlockPos.betweenClosed(cell.offset(-3, 0, -3), cell.offset(3, 2, 3)))
                base.set(pos.immutable(), Blocks.AIR.defaultBlockState());
            var wall = new JsonPrimitive(GenieNative.TargetedCast.HEDGEHOG_WALL.id.toString());
            require(complete(host, queued(host, "lbc.cast_at", wall, vector(target))).getAsBoolean(),
                    "native Hedgehog Wall accepted");
            var towers = service.views().stream().filter(v -> !initial.contains(v.entity()))
                    .filter(v -> base.level.getEntity(v.entity()) != null).toList();
            require(towers.size() == 65 && towers.stream().filter(LbcHedgehog.View::passenger).count() == 60
                    && towers.stream().allMatch(v -> v.kind() == LbcHedgehog.Kind.WALL && v.owner().equals(base.owner.getUUID())),
                    "native Hedgehog Wall keeps all five towers and contextual ownership");
        } finally {
            base.removeFields();
            for (var view : service.views()) {
                if (!initial.contains(view.entity())) base.discard(base.level.getEntity(view.entity()));
            }
        }
    }

    private static void danmakuCasts(GameController game, WoolBucketChecks.Fixture base, Vec3 center) {
        var danmaku = game.items().danmaku();
        require(danmaku.count() == 0, "exclusive Danmaku effects fixture");
        var owner = base.owner;
        var weapon = new ItemStack(Items.GOLDEN_SWORD);
        var chargeScore = game.items().mana().score(owner, "danmaku_score");
        int initialCharge = chargeScore.get();
        var victim = Objects.requireNonNull(EntityTypes.COW.create(base.level, EntitySpawnReason.TRIGGERED));
        victim.setPos(center);
        victim.setNoAi(true);
        require(base.level.addFreshEntity(victim), "native Danmaku victim");
        base.own(victim);
        try (var host = new GenieWorldHost(game, owner, weapon, () -> true, () -> 0)) {
            base.field(false, center);
            require(!complete(host, queued(host, "lbc.cast_at", new JsonPrimitive("skywars2:gohei_spellcard"),
                    vector(center))).getAsBoolean() && danmaku.bombs().isEmpty(), "Gohei shell respects antimagic at its center");
            base.removeFields();
            var bursts = List.of(GenieNative.TargetedCast.GOHEI_SPELLCARD,
                    GenieNative.TargetedCast.GOHEI_SPELLCARD_FULL, GenieNative.TargetedCast.HAKUREI_BOMB);
            for (var burst : bursts) {
                require(complete(host, queued(host, "lbc.cast_at", new JsonPrimitive(burst.id.toString()),
                        vector(center))).getAsBoolean(), "native sphere accepted: " + burst);
                var body = danmaku.bombs().getFirst();
                boolean fast = burst == GenieNative.TargetedCast.HAKUREI_BOMB;
                int charge = burst == GenieNative.TargetedCast.GOHEI_SPELLCARD_FULL ? 1500 : 300;
                var expectedGlow = fast ? LbcDanmaku.HAKUREI_BOMB_GLOW : LbcDanmaku.SPELLCARD_GLOW;
                require(body.owner == owner && body.charge == charge && body.fast == fast
                        && body.display.position().equals(center) && body.display.getGlowColorOverride() == expectedGlow
                        && body.cause.player().equals(owner.getUUID()) && ItemStack.matches(body.cause.enchantmentWeapon(), weapon),
                        "sphere has its native center, variant, color and captured weapon");
                float health = victim.getHealth();
                for (int i = 0; i < 9; i++) danmaku.tick();
                require(victim.getHealth() == health, "sphere does not debit health before the native pulse");
                danmaku.tick();
                require(body.age == (fast ? 100 : 40) && victim.getLastHurtByMob() == owner
                        && Math.abs(health - victim.getHealth() - (fast ? 1 : .5)) < 1e-4
                        && chargeScore.get() == initialCharge, "sphere deals authored damage on tick ten without taking staff charge");
                base.discard(body.display);
            }

            var origin = center.add(-1, .65, 0);
            var aim = new Vec3(1, 0, 0);
            var homing = new JsonPrimitive("skywars2:danmaku_homing_card");
            require(complete(host, queued(host, "lbc.cast", homing, vector(origin), vector(aim))).getAsBoolean(),
                    "directed native homing card accepted");
            var guided = danmaku.cards().getFirst();
            require(guided.homing && guided.display.position().equals(origin) && guided.direction.equals(aim)
                    && ItemStack.matches(guided.cause.enchantmentWeapon(), weapon), "homing card has exact origin and source");
            require(danmaku.reflectCard(guided.display, base.other) && guided.owner == base.other && guided.cause == null,
                    "reflecting a scripted card transfers control without retaining the old weapon");
            base.discard(guided.display);

            var ordinary = new JsonPrimitive("skywars2:danmaku_card");
            require(complete(host, queued(host, "lbc.cast", ordinary, vector(origin), vector(aim))).getAsBoolean(),
                    "directed native ordinary card accepted");
            var straight = danmaku.cards().getFirst();
            float before = victim.getHealth();
            danmaku.tick();
            require(!straight.homing && straight.display.isRemoved() && Math.abs(before - victim.getHealth() - .325) < 1e-4,
                    "ordinary card hits once and retires with its original damage");
        } finally {
            base.removeFields();
            for (var bomb : danmaku.bombs()) base.discard(bomb.display);
            for (var card : danmaku.cards()) base.discard(card.display);
            base.discard(victim);
            chargeScore.set(initialCharge);
        }
    }

    private static void sauvojenCasts(GameController game, WoolBucketChecks.Fixture base, Vec3 origin) {
        var owner = base.owner;
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var before = Set.copyOf(game.items().clouds().acidSnapshot());
        var wandOffhand = game.config().offhandWands;
        var mana = game.items().mana();
        int balance = mana.get(owner);
        var aim = new Vec3(1, 0, 0);
        var plain = new JsonPrimitive("skywars2:sauvojen_acid");
        var awake = new JsonPrimitive("skywars2:sauvojen_acid_awakened_caustic");
        var offhand = new JsonPrimitive("offhand");
        try (var host = new GenieWorldHost(game, owner, new ItemStack(Items.STICK), () -> true, () -> 0)) {
            game.config().offhandWands = false;
            require(!complete(host, queued(host, "lbc.cast", awake, vector(origin), vector(aim), offhand)).getAsBoolean()
                    && Set.copyOf(game.items().clouds().acidSnapshot()).equals(before), "native offhand toggle denies the directed cast");
            game.config().offhandWands = true;
            require(complete(host, queued(host, "lbc.cast", plain, vector(origin), vector(aim))).getAsBoolean(),
                    "ordinary Sauvojen acid route accepted");
            require(complete(host, queued(host, "lbc.cast", awake, vector(origin), vector(aim), offhand)).getAsBoolean(),
                    "Academy Sauvojen acid mode accepted in the allowed offhand");
            var shots = game.items().clouds().acidSnapshot().stream().filter(shot -> !before.contains(shot)).toList();
            require(shots.size() == 2 && shots.stream().allMatch(shot -> shot.getOwner() == owner
                    && shot.position().equals(origin) && shot.getDeltaMovement().normalize().distanceTo(aim) < 1e-5)
                    && shots.stream().anyMatch(shot -> LbcClouds.awakenedVariant(shot) == 1)
                    && shots.stream().anyMatch(shot -> LbcClouds.awakenedVariant(shot) != 1),
                    "both real variants keep their authored projectile and supplied aim");
            require(mana.get(owner) == balance && owner.getMainHandItem() == main && owner.getOffhandItem() == off
                    && game.items().sauvojen().count() == 0, "script emits the individual payload without spawning a drone, spending mana or switching hands");

            var stars = game.items().sauvojen().stars();
            var star = new JsonPrimitive("skywars2:sauvojen_terra_star_charge");
            require(complete(host, queued(host, "lbc.cast", star, vector(origin), vector(aim))).getAsBoolean()
                    && stars.snapshot().stream().anyMatch(actor -> actor.owner() == owner && actor.position().equals(origin)
                    && actor.direction().equals(aim)), "a managed Fevgalea charge is published through the source dispatcher");
        } finally {
            game.config().offhandWands = wandOffhand;
            game.items().clouds().acidSnapshot().stream().filter(shot -> !before.contains(shot)).forEach(base::discard);
            game.items().sauvojen().stars().stop();
        }
    }

    private static void nativeSummons(GameController game, WoolBucketChecks.Fixture base, Vec3 near) {
        var service = game.items().summons();
        var initial = Set.copyOf(service.snapshot());
        var owner = base.owner;
        var center = near.add(0, -22, 0);
        var block = BlockPos.containing(center);
        // The native Ghast Soul formation reaches ten blocks in both horizontal directions.
        for (int x = (block.getX() - 10) >> 4; x <= ((block.getX() + 10) >> 4); x++)
            for (int z = (block.getZ() - 10) >> 4; z <= ((block.getZ() + 10) >> 4); z++)
                base.level.getChunk(x, z);

        var soul = new JsonPrimitive("skywars2:summoner_guardians");
        var mana = game.items().mana();
        int balance = mana.get(owner);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        try (var host = new GenieWorldHost(game, owner, new ItemStack(Items.STICK), () -> true, () -> 0)) {
            var absent = new Vec3(1048576.25, center.y, 1048576.25);
            require(complete(host, queued(host, "lbc.summon", soul, vector(absent))).getAsJsonArray().isEmpty()
                    && !base.level.hasChunkAt(BlockPos.containing(absent)), "summoning does not fetch absent chunks");
            base.field(false, center);
            require(complete(host, queued(host, "lbc.summon", soul, vector(center))).getAsJsonArray().isEmpty()
                    && Set.copyOf(service.snapshot()).equals(initial), "antimagic suppresses the scripted formation");
            base.removeFields();

            for (var option : LbcSummons.scriptSummons()) {
                var id = new JsonPrimitive(option.id().toString());
                var ids = complete(host, queued(host, "lbc.summon", id, vector(center))).getAsJsonArray();
                int expected = switch (option.variant()) {
                    case "ghostrider", "blaze" -> 3;
                    default -> option.roots();
                };
                require(ids.size() == expected, "returns every native body in " + option.id());
                var bodies = ids.asList().stream().map(value -> base.level.getEntity(UUID.fromString(value.getAsString()))).toList();
                require(bodies.stream().allMatch(entity -> entity instanceof Mob mob && service.owner(mob) == owner
                        && game.items().currentEpoch(mob) && !mob.canAttack(owner)),
                        "actual mobs have native ownership and cannot target their controller " + option.id());
                if (option.variant().equals("ghostrider")) {
                    require(bodies.get(0).getType() == EntityTypes.BAT && bodies.get(1).getType() == EntityTypes.SKELETON_HORSE
                            && bodies.get(1).getVehicle() == bodies.get(0) && bodies.get(2).getVehicle() == bodies.get(1),
                            "returns the mounted Ghost Rider in the native passenger order");
                }
                if (option.variant().equals("guardians")) {
                    require(bodies.stream().allMatch(entity -> entity.getType() == EntityTypes.GUARDIAN && entity.isNoGravity()),
                            "native Guardian Soul ships all four no-gravity bodies");
                    var next = complete(host, queued(host, "lbc.summon", id, vector(center))).getAsJsonArray();
                    require(next.size() == expected && bodies.stream().allMatch(Entity::isRemoved),
                            "same-family recast replaces only those bodies");
                }
                service.snapshot().stream().filter(mob -> !initial.contains(mob)).forEach(base::discard);
            }
            require(mana.get(owner) == balance && owner.getMainHandItem() == main && owner.getOffhandItem() == off,
                    "borrowing a summon factory does not consume the existing wand or spend its mana");
        } finally {
            base.removeFields();
            service.snapshot().stream().filter(mob -> !initial.contains(mob)).forEach(base::discard);
        }
    }

    static void run(GameController game,WoolBucketChecks.Fixture base){
        var center=BlockPos.containing(base.resident(false));var start=center.offset(-6,0,0);
        for(var pos:BlockPos.betweenClosed(start.offset(-1,-1,-1),start.offset(12,2,1)))base.set(pos.immutable(),Blocks.AIR.defaultBlockState());
        var first=start.offset(1,-1,0);var second=start.offset(2,-1,0);
        boolean[] admitted={true};
        try(var host=new GenieWorldHost(game,base.owner,new ItemStack(Items.STICK),()->admitted[0],()->123L)){
            var command=queued(host,"world.command",new JsonPrimitive("setblock "+first.getX()+" "+first.getY()+" "+first.getZ()+" minecraft:stone"));
            require(base.level.getBlockState(first).isAir(),"native setblock has not run during enqueue");
            admitted[0]=false;require(host.drain(1,Long.MAX_VALUE)==0&&!command.future().isDone(),"paused item does not run queued native work");
            admitted[0]=true;
            var result=complete(host,command).getAsJsonObject();
            require(base.level.getBlockState(first).is(Blocks.STONE)&&result.get("successes").getAsInt()==1,"real native command and success callback");
            var cancelled=queued(host,"world.command",new JsonPrimitive("setblock "+second.getX()+" "+second.getY()+" "+second.getZ()+" minecraft:stone"));
            cancelled.cancel().run();require(host.pending()==0&&cancelled.future().isCancelled()&&base.level.getBlockState(second).isAir(),"cancelled work never reaches the native world");
            var self=complete(host,queued(host,"world.select",new JsonPrimitive("@s"))).getAsJsonArray();
            require(self.size()==1&&self.get(0).getAsString().equals(base.owner.getUUID().toString()),"native selector retains actual player source");
            var time=(ScriptMachine.Value)host.call("time.tick",List.of());require(time.value().getAsLong()==123,"host uses caller's logical clock");

            // Verify gameplay authority through the native dispatcher without executing any admin command.
            try{
                var method=GenieWorldHost.class.getDeclaredMethod("source",CommandSource.class);method.setAccessible(true);
                var source=(CommandSourceStack)method.invoke(host,CommandSource.NULL);
                require(Commands.getParseException(game.server().getCommands().getDispatcher().parse("stop",source))!=null,
                    "the account's operator status cannot grant native administrator authority to item code");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            rejected(()->host.validateCommand("reload"),"reload accepted");
            rejected(()->host.validateCommand("execute run reload"),"execute wrapper bypassed reload guard");
            rejected(()->host.validateCommand("return run reload"),"return wrapper bypassed reload guard");
            rejected(()->host.validateCommand("datapack disable vanilla"),"pack reload accepted");
            rejected(()->host.validateCommand("skywars reloadconfig"),"mod configuration reload accepted");
            rejected(()->host.validateCommand("execute as @s run sw stop"),"match administration alias accepted inside execute");
            rejected(()->host.validateCommand("return run function lbc:test"),"arbitrary server function accepted");
            rejected(()->host.validateCommand("execute if function lbc:test"),"execute condition invoked an arbitrary server function");
            rejected(()->host.validateCommand("data merge storage skywars2:test {witness:1b}"),"external data storage write accepted");
            rejected(()->host.validateCommand("setblock ~ ~ ~ minecraft:command_block"),"operator block creation accepted");
            rejected(()->host.validateCommand("give @s minecraft:command_block"),"operator control item creation accepted");
            rejected(()->host.validateCommand("summon minecraft:command_block_minecart"),"operator vehicle creation accepted");
            host.validateCommand("datapack list");
            host.validateCommand("execute if entity @s run give @s minecraft:stick");
            host.validateCommand("summon minecraft:pig");
            authorityScope(game,base,host,start);

            Vec3 launchSpot=Vec3.atBottomCenterOf(start.offset(6,0,0));
            var motion=new Vec3(.1,.2,.9);
            var uuid=complete(host,queued(host,"entity.launch",new JsonPrimitive("minecraft:creeper"),vector(launchSpot),vector(motion),new JsonPrimitive(20)));
            var spawned=base.level.getEntity(UUID.fromString(uuid.getAsString()));
            require(spawned instanceof Creeper&&spawned.getDeltaMovement().equals(motion)&&spawned.position().equals(launchSpot),"the real creeper factory preserves explicit origin and initial velocity");
            base.own(spawned);
            try {
                var cause=game.items().spellSources().source(spawned);
                require(cause!=null&&cause.emitter().equals(spawned.getUUID())&&cause.author().equals(base.owner.getUUID()),"native body is the source, player retains statistical credit");
                var arrows=complete(host,queued(host,"entity.launch",new JsonPrimitive("minecraft:arrow"),vector(launchSpot.add(0,3,0)),vector(motion),new JsonPrimitive(19)));
                var arrow=base.level.getEntity(UUID.fromString(arrows.getAsString()));
                require(arrow instanceof net.minecraft.world.entity.projectile.arrow.Arrow a&&a.getOwner()==base.owner
                    &&a.getDeltaMovement().distanceTo(motion)<.00001,"an actual native arrow retains the owner and exact initial direction");
                base.own(arrow);base.discard(arrow);
            } finally { base.discard(spawned); }

            directedCasts(game, base, launchSpot.add(0, .65, 0));
            targetedOreshnik(game, base, launchSpot);
            targetedHedgehogs(game, base, launchSpot);
            danmakuCasts(game, base, launchSpot);
            sauvojenCasts(game, base, launchSpot.add(0, .65, 0));
            nativeSummons(game, base, launchSpot);

            require(GenieNative.grimoires().containsKey(net.minecraft.resources.Identifier.parse("skywars2:tesla_instant_grimoire")),"real Tesla grimoire is indexed by the engine");
            var oldSpeed=base.owner.getEffect(MobEffects.SPEED);
            base.owner.removeEffect(MobEffects.SPEED);
            try {
                var cast=complete(host,queued(host,"lbc.grimoire",new JsonPrimitive("skywars2:badwiki_23_grimoire")));
                var speed=base.owner.getEffect(MobEffects.SPEED);
                require(cast.getAsBoolean()&&speed!=null&&speed.getAmplifier()==0&&speed.getDuration()==1200,"registered grimoire runs its exact native effect and duration");
            } finally {
                base.owner.removeEffect(MobEffects.SPEED);
                if(oldSpeed!=null)base.owner.addEffect(oldSpeed);
            }

            var grass=start.offset(2,0,0);var slab=start.offset(4,0,0);var stone=start.offset(10,0,0);
            base.set(grass,Blocks.SHORT_GRASS.defaultBlockState());base.set(slab,Blocks.STONE_SLAB.defaultBlockState());base.set(stone,Blocks.STONE.defaultBlockState());
            Vec3 from=Vec3.atLowerCornerOf(start).add(.25,.75,.5),to=from.add(11,0,0);
            var cow=Objects.requireNonNull(EntityTypes.COW.create(base.level,EntitySpawnReason.TRIGGERED));
            cow.setNoAi(true);cow.setPos(Vec3.atLowerCornerOf(start).add(7,.0,.5));
            require(base.level.addFreshEntity(cow),"native cow witness published");base.own(cow);
            var snow=new Snowball(base.level,from.x+6,from.y,from.z,new ItemStack(Items.SNOWBALL));
            snow.setNoGravity(true);require(base.level.addFreshEntity(snow),"native projectile witness published");base.own(snow);
            try{
                var ray=complete(host,queued(host,"world.raycast",vector(from),vector(to),new JsonPrimitive(0),new JsonPrimitive(true))).getAsJsonObject();
                var wall=ray.getAsJsonObject("block");
                require(wall.get("loaded").getAsBoolean()&&wall.get("id").getAsString().equals("minecraft:stone")
                    &&wall.getAsJsonArray("position").get(0).getAsInt()==stone.getX(),"collision ray skips grass and empty upper half of slab");
                var hits=new HashSet<String>();for(var value:ray.getAsJsonArray("entities"))hits.add(value.getAsJsonObject().get("entity").getAsString());
                require(hits.contains(cow.getUUID().toString())&&hits.contains(snow.getUUID().toString()),
                    "one ray traverses multiple bodies and includes native projectiles: result="+ray+" cow="+cow.getBoundingBox()+" snow="+snow.getBoundingBox());
                require(ray.get("entity").getAsString().equals(snow.getUUID().toString())
                    &&ray.get("entity").equals(ray.getAsJsonArray("entities").get(0).getAsJsonObject().get("entity")),
                    "saved programs can read the nearest entity directly without losing the ordered hit list");
                var info=complete(host,queued(host,"entity.info",new JsonPrimitive(cow.getUUID().toString()))).getAsJsonObject();
                require(info.get("type").getAsString().equals("minecraft:cow")&&info.has("eye_position")&&info.get("health").getAsFloat()==cow.getHealth(),"native entity information is actual world state");
            }finally{base.discard(cow);base.discard(snow);}
            Vec3 absent=new Vec3(1048576.25,from.y,1048576.25);var absentPos=BlockPos.containing(absent);
            require(!base.level.hasChunkAt(absentPos),"remote witness starts unresident");
            var remote=complete(host,queued(host,"world.raycast",vector(absent),vector(absent.add(32,0,0)),new JsonPrimitive(0),new JsonPrimitive(true))).getAsJsonObject();
            require(!remote.getAsJsonObject("block").get("loaded").getAsBoolean()
                &&remote.getAsJsonObject("block").get("id").isJsonNull()&&remote.get("entity").isJsonNull()&&!base.level.hasChunkAt(absentPos)
                &&!base.level.hasChunkAt(absentPos.offset(32,0,0)),"ray and hit metadata do not load missing chunks");

            // The real VM resolves world calls through this host; a completed command resumes once.
            var code=ScriptJson.parse("""
                {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
                ["const","execute if entity @s run data get entity @s Pos"],
                ["call","world.command",1],["return"]]}}}
                """);
            var program=ScriptProgram.compile(code,GenieWorldHost.api(),Set.of("USE"));
            var fiber=new ScriptMachine(program,"main",List.of(new JsonObject()),program.initialState(),host);
            require(fiber.advance(0,100)==ScriptMachine.Status.WAITING,"real VM yields for queued native command");
            require(host.drain(1,Long.MAX_VALUE)==1&&fiber.advance(1,100)==ScriptMachine.Status.DONE,"VM resumes after real command");
            require(fiber.result().getAsJsonObject().get("successes").getAsInt()>0
                &&!fiber.result().getAsJsonObject().getAsJsonArray("output").isEmpty(),"native data-get result and output are available to code");

            var queuedCode = ScriptJson.parse("""
                {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
                ["const","setblock %d %d %d minecraft:gold_block"],["call","world.command",1],["return"]]}}}
                """.formatted(second.getX(),second.getY(),second.getZ()));
            var nativeProgram = ScriptProgram.compile(queuedCode, GenieWorldHost.api(), Set.of("USE"));
            var nativeFiber = new ScriptMachine(nativeProgram,"main",List.of(new JsonObject()),nativeProgram.initialState(),host);
            require(nativeFiber.advance(0,100)==ScriptMachine.Status.WAITING,"saved real write awaits its host job");
            var graph = new ScriptGraphCopy();
            var beforeWrite = nativeFiber.checkpoint(graph, pending -> host.checkpointPending(pending, graph));
            require(host.drain(1,Long.MAX_VALUE)==1&&base.level.getBlockState(second).is(Blocks.GOLD_BLOCK),"original queued command applies once");
            var afterWrite = nativeFiber.checkpoint(graph, pending -> { throw new AssertionError("completed result has no queued work"); });
            nativeFiber.cancel();
            base.set(second, Blocks.AIR.defaultBlockState());
            try (var restoredHost = new GenieWorldHost(game,base.owner,new ItemStack(Items.STICK),()->true,()->2L)) {
                var restored = beforeWrite.restore(new JsonObject(),new ScriptGraphCopy(),restoredHost,new ScriptMemory());
                require(restoredHost.pending()==1&&base.level.getBlockState(second).isAir(),"installing the snapshot never executes queued work");
                require(restoredHost.drain(1,Long.MAX_VALUE)==1&&restored.advance(2,100)==ScriptMachine.Status.DONE
                    &&base.level.getBlockState(second).is(Blocks.GOLD_BLOCK),"restored wait is connected to a fresh native job");
                base.set(second, Blocks.AIR.defaultBlockState());
                var completed = afterWrite.restore(new JsonObject(),new ScriptGraphCopy(),restoredHost,new ScriptMemory());
                require(restoredHost.pending()==0&&completed.advance(2,100)==ScriptMachine.Status.DONE&&base.level.getBlockState(second).isAir()
                    &&completed.result().getAsJsonObject().get("successes").getAsInt()==1,"restoring a completed result does not replay its command");
            }
            var abandoned=queued(host,"world.select",new JsonPrimitive("@s"));
            host.close();require(abandoned.future().isCancelled()&&host.pending()==0,"item context close cancels every owned pending job");
        }
    }
}
