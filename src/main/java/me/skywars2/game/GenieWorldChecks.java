package me.skywars2.game;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.game.genie.GenieWorldHost;
import me.skywars2.game.genie.script.*;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
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
            .filter(level->level!=base.level && LbcDimensionItems.ordinary(level)).findFirst().orElseThrow();
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
            require(!applyStatus(host,witness,"personal_stasis",40)
                            && !game.items().legacyEffects().personalStasis().active(witness),
                    "foreign-dimension UUID cannot apply or strand native stasis");
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
            var author = base.owner;
            var authorPosition = author.position();
            var authorMovement = author.getDeltaMovement();
            float authorYaw = author.getYRot(), authorPitch = author.getXRot();
            var mana = game.items().mana();
            int originalReverse = mana.reverse(witness);
            try {
                require(author.teleportTo(foreign, authorPosition.x, authorPosition.y, authorPosition.z,
                        Set.of(), authorYaw, authorPitch, true), "item owner enters the same alternate ordinary world");
                base.client.acknowledge();
                require(!applyStatus(host, witness, "cold", 21) && mana.hudValue(witness, "cold") == 0
                                && !applyStatus(host, witness, "personal_stasis", 31)
                                && !game.items().legacyEffects().personalStasis().active(witness),
                        "arena-owned Frost and Stasis cannot leave orphan state in another ordinary world");
                require(applyStatus(host, witness, "reverse_mana", 21) && mana.reverse(witness) == originalReverse + 2,
                        "same-world, non-arena native mana drain remains available in an ordinary dimension");
            } finally {
                mana.reverse(witness, originalReverse);
                require(author.teleportTo(base.level, authorPosition.x, authorPosition.y, authorPosition.z,
                        Set.of(), authorYaw, authorPitch, true), "alternate-world item owner restored");
                base.client.acknowledge();
                author.setDeltaMovement(authorMovement);
            }
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

    private static void ceilingCasts(GameController game,WoolBucketChecks.Fixture base,GenieWorldHost host,BlockPos column){
        var level=base.level;var top=new BlockPos(column.getX(),level.getMaxY(),column.getZ());
        var origin=Vec3.atCenterOf(top);var above=origin.add(0,.5,0);var east=new Vec3(1,0,0);
        require(!level.isOutsideBuildHeight(top)&&level.isOutsideBuildHeight(BlockPos.containing(above)),
                "actual fractional origin inside top voxel and control on first outside plane");
        base.set(top,Blocks.AIR.defaultBlockState());
        var arrowId=new JsonPrimitive("minecraft:arrow");var terraId=new JsonPrimitive("skywars2:terra_charge");
        require(complete(host,queued(host,"entity.launch",arrowId,vector(above),vector(east),new JsonPrimitive(19))).isJsonNull(),
                "native launch rejects the first position outside build height");
        int before=game.items().terra().count();
        require(!complete(host,queued(host,"lbc.cast",terraId,vector(above),vector(east))).getAsBoolean()
                        &&game.items().terra().count()==before,
                "directed Terra denies the first outside plane without publishing a body");

        var uuid=complete(host,queued(host,"entity.launch",arrowId,vector(origin),vector(east),new JsonPrimitive(19)));
        require(!uuid.isJsonNull(),"native launch accepts the center of the top legal voxel");
        var arrow=level.getEntity(UUID.fromString(uuid.getAsString()));
        if(arrow!=null)base.own(arrow);
        require(arrow instanceof net.minecraft.world.entity.projectile.arrow.Arrow shot&&shot.getOwner()==base.owner
                        &&shot.position().equals(origin)&&shot.getDeltaMovement().distanceTo(east)<1e-5,
                "top-layer launch produces one actual arrow at exact fractional coordinates with its native owner");
        base.discard(arrow);

        require(complete(host,queued(host,"lbc.cast",terraId,vector(origin),vector(east))).getAsBoolean(),
                "directed Terra accepts the center of the top legal voxel");
        var charges=level.getEntitiesOfClass(LbcTerra.TerraProjectile.class,new AABB(origin,origin).inflate(1),
                shot->shot.getOwner()==base.owner&&shot.position().equals(origin));
        charges.forEach(base::own);
        require(charges.size()==1&&game.items().terra().count()==before+1,"one exact native directed Terra charge in the top layer");
        base.discard(charges.getFirst());
    }

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

    private static void prismCasts(GameController game, WoolBucketChecks.Fixture base, Vec3 origin) {
        var prism = game.items().prism();
        var existing = prism.snapshot();
        var standard = new JsonPrimitive("skywars2:unstable_prism_charge");
        var academy = new JsonPrimitive("skywars2:academy_prism_ray");
        var aim = new Vec3(3, 0, 0);
        var main = base.owner.getMainHandItem();
        var off = base.owner.getOffhandItem();
        int mana = game.items().mana().get(base.owner);
        var gift = new ItemStack(Items.IRON_SWORD);
        gift.enchant(base.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 3);
        var glass = BlockPos.containing(origin);
        var prior = base.level.getBlockState(glass);
        var rayOrigin = origin.add(0, 0, .25);
        try (var host = new GenieWorldHost(game, base.owner, gift, () -> true, () -> 0)) {
            var budgetTick = LbcPrism.class.getDeclaredField("budgetTick");
            var spent = LbcPrism.class.getDeclaredField("spent");
            budgetTick.setAccessible(true);
            spent.setAccessible(true);
            int beforeTick = budgetTick.getInt(prism);
            long beforeSpent = spent.getLong(prism);
            // Stand in for a tick where earlier Prism casts exhausted the shared work budget.
            budgetTick.setInt(prism, game.server().getTickCount());
            spent.setLong(prism, 3_000_000);
            try {
                var pending = queued(host, "lbc.cast", standard, vector(origin), vector(aim));
                require(prism.snapshot().equals(existing), "Prism input is deferred until the item host drains");
                require(complete(host, pending).getAsBoolean(), "native unstable Prism is accepted");
                var charge = prism.snapshot().stream().filter(body -> !existing.contains(body)).findFirst().orElseThrow();
                require(charge.getOwner() == base.owner && charge.position().equals(origin)
                        && charge.getDeltaMovement().equals(new Vec3(1, 0, 0))
                        && charge.phase == LbcPrism.Charge.Phase.UNSTABLE && charge.entityTags().contains("shield_blockable")
                        && WandCastOrigin.borrowed(charge), "moving Prism keeps actual ownership, position, aim and defense policy");
                var source = new net.minecraft.world.damagesource.DamageSource(base.level.registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getOrThrow(net.minecraft.world.damagesource.DamageTypes.CACTUS), charge, base.owner);
                require(ItemStack.matches(game.items().indirectKills().enchantmentWeapon(source), gift),
                        "native delayed damage retains the Genie item's enchantments");
                base.set(glass, Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLUE).defaultBlockState());
                charge.tick();
                require(charge.phase == LbcPrism.Charge.Phase.STABLE, "native flight stabilizes on glass");
                charge.discard();
                base.set(glass, prior);

                require(complete(host, queued(host, "lbc.cast", academy, vector(rayOrigin), vector(aim))).getAsBoolean(),
                        "native Academy Prism is accepted");
                var ray = prism.snapshot().stream().filter(body -> !existing.contains(body) && !body.isRemoved())
                        .findFirst().orElseThrow();
                require(ray.phase == LbcPrism.Charge.Phase.ACADEMY && ray.position().equals(rayOrigin)
                        && ray.getDeltaMovement().equals(new Vec3(1, 0, 0)) && ray.getOwner() == base.owner
                        && !ray.entityTags().contains("shield_blockable") && WandCastOrigin.borrowed(ray),
                        "Academy Prism joins the native immediate queue with its original phase and aim: "
                                + ray.phase + " at " + ray.position() + " with " + ray.getDeltaMovement());
                ray.discard();
                base.field(false, origin);
                require(!complete(host, queued(host, "lbc.cast", academy, vector(origin), vector(aim))).getAsBoolean()
                        && prism.snapshot().stream().noneMatch(body -> !existing.contains(body) && !body.isRemoved()),
                        "native antimagic denies the spell before publication");
                require(game.items().mana().get(base.owner) == mana && base.owner.getMainHandItem() == main
                        && base.owner.getOffhandItem() == off, "Prism casts neither spend mana nor modify physical hands");
            } finally {
                prism.snapshot().stream().filter(body -> !existing.contains(body)).forEach(body -> body.discard());
                budgetTick.setInt(prism, beforeTick);
                spent.setLong(prism, beforeSpent);
                base.set(glass, prior);
                base.removeFields();
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static net.minecraft.world.entity.animal.cow.Cow stationaryCow(WoolBucketChecks.Fixture base, Vec3 at) {
        var cow = Objects.requireNonNull(EntityTypes.COW.create(base.level, EntitySpawnReason.TRIGGERED));
        cow.setPos(at);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        require(base.level.addFreshEntity(cow), "native stationary cow");
        base.own(cow);
        return cow;
    }

    private static void polymorphCasts(GameController game, WoolBucketChecks.Fixture base, Vec3 point) {
        var polymorph = game.items().polymorph();
        var sheep = game.items().sheep();
        require(sheep.sheepViews().isEmpty() && sheep.holes().isEmpty(), "exclusive native Polymorph fixture");
        var origin = point.add(0, 3, 0);
        var aim = new Vec3(1, 0, 0);
        var cell = BlockPos.containing(origin);
        for (var pos : BlockPos.betweenClosed(cell.offset(-1,-1,-3),cell.offset(12,1,3)))
            base.set(pos.immutable(), Blocks.AIR.defaultBlockState());
        base.set(cell.east(9), Blocks.STONE.defaultBlockState());
        var near = stationaryCow(base, origin.add(3,-.6,2.2));
        var outside = stationaryCow(base, origin.add(6,-.6,2.7));
        var behind = stationaryCow(base, origin.add(11,-.6,0));
        var owner = base.owner;
        var pearl = new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl(
                base.level, owner, new ItemStack(Items.ENDER_PEARL));
        pearl.setPos(origin.add(6,0,0));
        pearl.setNoGravity(true);
        var motion = new Vec3(.08,.16,-.04);
        pearl.setDeltaMovement(motion);
        require(base.level.addFreshEntity(pearl), "native unpickable pearl enters the beam lane");
        base.own(pearl);
        var gift = new ItemStack(Items.BONE);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        int mana = game.items().mana().get(owner);
        var regular = new JsonPrimitive("skywars2:polymorph_ray");
        var directHole = new JsonPrimitive("skywars2:succubus_black_hole");
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 0)) {
            base.field(false, origin);
            require(game.items().magic().suppressedAt(base.level, origin)
                    && !complete(host, queued(host, "lbc.cast", regular, vector(origin), vector(aim))).getAsBoolean()
                    && !complete(host, queued(host, "lbc.cast_at", directHole, vector(origin))).getAsBoolean(),
                    "both Polymorph abilities respect the native magic field");
            base.removeFields();

            var code = ScriptJson.parse("""
                {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
                ["const","skywars2:polymorph_ray"],["const",%s],["const",%s],["call","lbc.cast",3],["return"]]}}}
                """.formatted(vector(origin), vector(aim)));
            var program = ScriptProgram.compile(code, GenieWorldHost.api(), Set.of("USE"));
            var fiber = new ScriptMachine(program,"main",List.of(new JsonObject()),program.initialState(),host);
            require(fiber.advance(0,100)==ScriptMachine.Status.WAITING && !polymorph.active(pearl),
                    "compiled gift cannot advance the beam before the host admits the action");
            require(host.drain(1,Long.MAX_VALUE)==1 && fiber.advance(1,100)==ScriptMachine.Status.DONE
                    && fiber.result().getAsBoolean(), "compiled gift casts from its explicit origin and direction");
            for (var original : List.of(near,pearl)) {
                require(polymorph.remaining(original)==1200, "beam pierces its near-hit target and its pearl");
                PolymorphWandChecks.absent(original);
                var form = ((Map<?,?>)WoolBucketChecks.read(polymorph,"forms")).get(original);
                var cause = (LbcIndirectKills.Owner)WoolBucketChecks.read(form,"cause");
                require(cause.player().equals(owner.getUUID()) && ItemStack.matches(cause.enchantmentWeapon(),gift),
                        "native temporary form retains the actual gift attribution");
            }
            require(!polymorph.active(outside) && !polymorph.active(behind),
                    "native fixed hit margin and solid wall stop the appropriate hits");
            var pearlForm = sheep.sheepViews().stream().filter(view -> view.point().equals(pearl.position())).findFirst().orElseThrow();
            var body = base.level.getEntity(pearlForm.entity());
            require(body instanceof net.minecraft.world.entity.animal.sheep.Sheep actual && actual.getHealth()==100
                    && actual.isPushable() && actual.getDeltaMovement().equals(motion),
                    "borrowed beam uses the actual mobile sheep and preserves projectile motion");
            polymorph.stop();
            require(base.level.getEntity(pearl.getUUID())==pearl && base.level.getEntity(pearl.getId())==pearl
                    && !pearl.isRemoved() && pearl.getDeltaMovement().equals(motion) && pearl.getOwner()==owner,
                    "reversible form restores the same native projectile, owner and velocity");
            base.discard(pearl);
            base.discard(near);

            var academyTarget = stationaryCow(base,origin.add(4,-.6,0));
            academyTarget.entityTags().add("male");
            require(complete(host, queued(host, "lbc.cast", new JsonPrimitive("skywars2:academy_polymorph_ray"),
                    vector(origin),vector(aim))).getAsBoolean(), "native Academy ray accepted");
            require(academyTarget.isRemoved() && !polymorph.active(academyTarget) && sheep.sheepViews().size()==1
                    && sheep.holes().size()==1 && sheep.holes().getFirst().point.equals(academyTarget.position()),
                    "Academy uses its actual permanent form and gender-triggered black hole");
            var fixed = origin.add(-.5,0,1);
            require(complete(host,queued(host,"lbc.cast_at",directHole,vector(fixed))).getAsBoolean()
                    && sheep.holes().size()==2 && sheep.holes().getLast().point.equals(fixed),
                    "targeted call starts the same native hole at the exact request position");
            for (var hole : sheep.holes())
                require(hole.owner.equals(owner.getUUID()) && hole.cause.friendlyFire()
                        && ItemStack.matches(hole.cause.enchantmentWeapon(),gift)
                        && base.level.getEntity(hole.entity.getUUID())==hole.entity,
                        "both black-hole sources retain native WILD admission and accepted gift");
            require(owner.getMainHandItem()==main && owner.getOffhandItem()==off
                    && game.items().mana().get(owner)==mana, "borrowed ray and hole do not charge the native staff or touch either hand");
        } finally {
            base.removeFields();
            polymorph.stop();
            sheep.stop();
        }
    }

    private static void psychoAreaCasts(GameController game, WoolBucketChecks.Fixture base, Vec3 point) {
        var psycho = game.items().psycho();
        var center = point.y + 48 <= base.level.getMaxY() ? point.add(0, 48, 0) : point.add(0, -60, 0);
        var main = base.owner.getMainHandItem();
        var off = base.owner.getOffhandItem();
        int mana = game.items().mana().get(base.owner);
        var near = stationaryCow(base, center);
        var outside = stationaryCow(base, center.add(.5, 30.25, 0));
        var direct = new JsonPrimitive("skywars2:psycho_look_back");
        var scare = new JsonPrimitive("skywars2:psycho_scare");
        var sound = new JsonPrimitive("skywars2:psycho_sound");
        var screamer = new JsonPrimitive("skywars2:psycho_screamer");
        var random = new JsonPrimitive("skywars2:psycho_area");
        var advancement = Objects.requireNonNull(game.server().getAdvancements().get(
                net.minecraft.resources.Identifier.parse("lbc:true_advancements/staffs/psycho")));
        var progress = base.owner.getAdvancements().getOrStartProgress(advancement);
        var before = new HashSet<String>();
        progress.getCompletedCriteria().forEach(before::add);
        try (var host = new GenieWorldHost(game, base.owner, new ItemStack(Items.STICK), () -> true, () -> 0)) {
            require(near.position().distanceToSqr(base.owner.position()) > 900, "test uses a remote center");
            var pending = queued(host, "lbc.cast_at", direct, vector(center));
            require(psycho.effectValue(near, "lookback") == 0, "queued effect waits for the item host drain");
            require(complete(host, pending).getAsBoolean() && psycho.effectValue(near, "lookback") == 9
                    && psycho.effectValue(outside, "lookback") == 0, "real nine-tick look-back uses the supplied 30-block sphere");
            require(complete(host, queued(host, "lbc.cast_at", scare, vector(center))).getAsBoolean()
                    && psycho.effectValue(near, "scared") == 100 && psycho.effectValue(outside, "scared") == 0,
                    "native fixed scare mode applies five-second fear");
            require(complete(host, queued(host, "lbc.cast_at", screamer, vector(center))).getAsBoolean()
                    && near.hasEffect(MobEffects.BLINDNESS) && near.getEffect(MobEffects.BLINDNESS).getDuration() == 120
                    && !outside.hasEffect(MobEffects.BLINDNESS), "native screamer applies the original blindness");
            require(complete(host, queued(host, "lbc.cast_at", sound, vector(center))).getAsBoolean()
                    && complete(host, queued(host, "lbc.cast_at", random, vector(center))).getAsBoolean(),
                    "both sound and random native effects are accepted");
            near.setHealth(1);
            require(complete(host, queued(host, "lbc.cast_at", sound, vector(center))).getAsBoolean()
                    && near.isDeadOrDying(), "the normal low-health execution still works");
            var ghost = base.level.getEntitiesOfClass(net.minecraft.world.entity.monster.Vex.class,
                    new AABB(near.position(), near.position()).inflate(2), entity -> psycho.owner(entity) == base.owner)
                    .stream().findFirst().orElseThrow();
            base.own(ghost);
            try {
                require(ghost.getMainHandItem().isEmpty() && ghost.getHealth() == 2_000_000_000f,
                        "execution publishes the original unarmed Sad Ghost with full health");
                var after = new HashSet<String>();
                progress.getCompletedCriteria().forEach(after::add);
                require(after.equals(before) && game.items().mana().get(base.owner) == mana
                        && base.owner.getMainHandItem() == main && base.owner.getOffhandItem() == off,
                        "scripted execution does not claim a real staff achievement, modify either hand or spend mana");
            } finally {
                base.discard(ghost);
            }
            base.field(false, center);
            require(!complete(host, queued(host, "lbc.cast_at", screamer, vector(center))).getAsBoolean(),
                    "an antimagic field at the target rejects the area cast");
        } finally {
            base.removeFields();
            psycho.clearEffects(near);
            psycho.clearEffects(outside);
            base.discard(near);
            base.discard(outside);
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

    private static void targetedExoperso(GameController game, WoolBucketChecks.Fixture base, Vec3 point) {
        var exoperso = game.items().exoperso();
        require(exoperso.count() == 0 && !game.items().exasperato().pendingNuclearTerrain(), "exclusive native ritual fixture");
        var center = new Vec3(point.x, base.level.getMaxY() - 60, point.z);
        var caster = base.owner;
        var gift = new ItemStack(Items.BLAZE_ROD);
        var regular = new JsonPrimitive("skywars2:exoperso");
        var academy = new JsonPrimitive("skywars2:academy_exoperso");
        var wandBefore = caster.getMainHandItem();
        var offBefore = caster.getOffhandItem();
        int manaBefore = game.items().mana().get(caster);

        try (var host = new GenieWorldHost(game, caster, gift, () -> true, () -> 0)) {
            var absent = new Vec3(1048576.25, center.y, 1048576.25);
            require(!complete(host, queued(host, "lbc.cast_at", academy, vector(absent))).getAsBoolean()
                    && !base.level.hasChunkAt(BlockPos.containing(absent)), "borrowed ritual will not load an absent chunk");
            base.field(false, center);
            require(game.items().magic().suppressedAt(base.level, center)
                    && !complete(host, queued(host, "lbc.cast_at", regular, vector(center))).getAsBoolean()
                    && exoperso.count() == 0, "borrowed ritual respects magic suppression at the target");
            base.removeFields();

            for (var id : List.of(regular, academy))
                require(complete(host, queued(host, "lbc.cast_at", id, vector(center))).getAsBoolean(), "native ritual accepted: " + id);
            var states = exoperso.rituals.snapshot();
            require(states.size() == 2 && states.getFirst().duration() == 340 && states.getLast().duration() == 180,
                    "both constructor entries start the actual normal and Academy countdowns");
            for (var state : states) {
                require(state.target.equals(center) && state.anchor.position().equals(center)
                        && state.rings.size() == 30 && state.rings.stream().allMatch(ring -> base.level.getEntity(ring.getUUID()) == ring),
                        "borrowed ritual fixes the target and publishes all thirty native displays");
                require(WandCastOrigin.borrowed(state.anchor) && state.cause.friendlyFire()
                        && state.cause.player().equals(caster.getUUID()) && ItemStack.matches(state.cause.enchantmentWeapon(),gift),
                        "borrowed native ritual stores the gift and keeps the original WILD policy");
            }
            require(caster.getMainHandItem() == wandBefore && caster.getOffhandItem() == offBefore
                    && game.items().mana().get(caster) == manaBefore, "no real staff replacement or mana debit");

            var actors = new ArrayList<Entity>();
            for (var state : states) {
                actors.add(state.anchor);
                actors.add(state.timer);
                actors.addAll(state.rings);
            }
            var graph = new MatchBlastCheckpoint.Graph(new MatchDamageSources(game, actors));
            var image = exoperso.checkpoint(graph);
            require(graph.links.resolve(base.level::getEntity).isEmpty(), "saved ritual binds every native display");
            exoperso.prepareCheckpoint();
            graph.prepare().run();
            image.prepare(graph.links).run();
            var restored = exoperso.rituals.snapshot().getLast();
            require(restored.cause == states.getLast().cause && WandCastOrigin.borrowed(restored.anchor),
                    "checkpoint retains the accepted attribution and achievement policy");

            var block = BlockPos.containing(center).below(2);
            base.set(block, Blocks.STONE.defaultBlockState());
            var victim = Objects.requireNonNull(EntityTypes.COW.create(base.level, EntitySpawnReason.TRIGGERED));
            victim.setPos(center.add(1.5, 0, 0));
            victim.setHealth(1);
            victim.setNoAi(true);
            victim.setNoGravity(true);
            require(base.level.addFreshEntity(victim), "native ritual damage witness");
            base.own(victim);
            restored.age = restored.duration() - 2;
            exoperso.tick();
            require(!restored.anchor.isRemoved() && victim.isAlive() && base.level.getBlockState(block).is(Blocks.STONE),
                    "no damage or terrain clearing on the last rune frame");
            exoperso.tick();
            var damage = victim.getLastDamageSource();
            require(restored.anchor.isRemoved() && restored.rings.stream().allMatch(Entity::isRemoved)
                    && base.level.getBlockState(block).isAir() && exoperso.count() == 1,
                    "Academy terminal uses the actual prepared blast and retires only its own runes");
            require(!victim.isAlive() && victim.getKillCredit() == caster && damage != null
                    && LbcIndirectKills.isWildCaptured(damage)
                    && ItemStack.matches(game.items().indirectKills().enchantmentWeapon(damage),gift),
                    "real lethal explosion retains WILD behavior, the player and the original gift: " + damage);
        } finally {
            base.removeFields();
            exoperso.stop();
            game.items().exasperato().stop();
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

    private static void danmakuVolleys(GameController game, WoolBucketChecks.Fixture base) {
        var danmaku = game.items().danmaku();
        var owner = base.owner;
        var origin = base.resident(false).add(0, 5, 0);
        var aim = new Vec3(1, 0, 0);
        var gift = new ItemStack(Items.GOLDEN_SWORD);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var mana = game.items().mana().score(owner, "mana");
        int initialMana = mana.get(), initialTicks = danmaku.remaining(owner);
        var charge = game.items().mana().score(owner, "danmaku_score");
        int initialCharge = charge.get();
        var ids = List.of("danmaku_volley", "danmaku_offhand_volley",
                "academy_danmaku_volley", "academy_danmaku_offhand_volley");
        int[][] rows = {{9,2,2,2,2,2,9,4,4,4}, {2,2,2,2,2,2,2,2,2,17},
                {8,8,8,8,8,8,8,8,8,17}, {6,6,6,6,6,6,6,6,6,6}};
        require(danmaku.count() == 0, "independent Gohei pattern fixture");
        var links = new MatchSpellCheckpoint.Links(new MatchDamageSources(game, List.of(owner)));
        var empty = danmaku.checkpoint(links);
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 0)) {
            for (int mode = 0; mode < ids.size(); mode++) {
                var id = new JsonPrimitive("skywars2:" + ids.get(mode));
                boolean offhandPattern = (mode & 1) == 1;
                require(complete(host, queued(host, "lbc.cast", id, vector(origin), vector(aim))).getAsBoolean()
                        && danmaku.cards().isEmpty(), "full Gohei sequence is queued: " + id);
                require(!complete(host, queued(host, "lbc.cast", id, vector(origin), vector(aim))).getAsBoolean(),
                        "one in-flight sequence per player and pattern hand");
                if (mode == 0) {
                    var pending = danmaku.checkpoint(links);
                    empty.prepare(links).run();
                    danmaku.tick();
                    require(danmaku.cards().isEmpty(), "rewind before cast clears the pending sequence");
                    pending.prepare(links).run();
                    base.pause();
                    try {
                        danmaku.tick();
                        require(danmaku.cards().isEmpty(), "a saved Gohei pattern waits during Time Stop");
                    } finally {
                        base.thaw();
                    }
                }
                for (int tick = 0; tick < 20; tick++) {
                    danmaku.tick();
                    var emitted = danmaku.cards().stream().filter(card -> card.age == 1).toList();
                    int expected = ((tick & 1) == 1) == offhandPattern ? rows[mode][tick / 2] : 0;
                    require(emitted.size() == expected, id + " native cadence, tick " + tick
                            + ": " + emitted.size() + "/" + expected);
                    for (var card : emitted) {
                        require(card.owner == owner && card.cause.player().equals(owner.getUUID())
                                && ItemStack.matches(card.cause.enchantmentWeapon(), gift)
                                && card.display.position().distanceTo(origin) < 1.8,
                                "each card retains the captured item, owner and remote cast origin");
                    }
                    if (mode == 0 && tick == 0) {
                        var card = emitted.get(2);
                        require(!card.homing && card.direction.distanceTo(aim) < 1e-5
                                && card.display.position().distanceTo(origin.add(1.2, -.5, 0)) < 1e-5
                                && emitted.stream().filter(c -> c.homing).count() == 4,
                                "ordinary first wave preserves the central shot, muzzle height and four homing cards");
                    }
                    if (mode == 2 && tick == 0)
                        require(emitted.stream().allMatch(card -> card.homing), "Academy first wave uses eight guided cards");
                    if (mode == 3 && tick == 1)
                        require(emitted.stream().allMatch(card -> card.direction.distanceTo(aim) < 1e-5),
                                "offhand Academy first wave fires the six parallel shots");
                }
                danmaku.tick();
                require(danmaku.cards().stream().noneMatch(card -> card.age == 1),
                        "each Gohei request stops after ten emission phases");
                for (var card : danmaku.cards()) base.discard(card.display);
            }
            var id = new JsonPrimitive("skywars2:danmaku_volley");
            base.field(false, origin);
            require(!complete(host, queued(host, "lbc.cast", id, vector(origin), vector(aim))).getAsBoolean(),
                    "full Gohei sequence respects suppression at the script origin");
            require(initialTicks == danmaku.remaining(owner) && mana.get() == initialMana
                    && owner.getMainHandItem() == main && owner.getOffhandItem() == off,
                    "borrowed casts retain physical hands, mana and paid Gohei cast ticks");
        } finally {
            base.removeFields();
            for (var card : danmaku.cards()) base.discard(card.display);
            empty.prepare(links).run();
            charge.set(initialCharge);
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
                if (option.family() == LbcSummons.SummonFamily.DOLPHIN
                        || option.family() == LbcSummons.SummonFamily.SAD_GHOST) continue;
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

    private static void distinctNativeSummons(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var center = base.resident(false).add(0, 4, 0);
        var gift = new ItemStack(Items.GOLDEN_SWORD);
        var dolphinId = new JsonPrimitive("skywars2:david_dolphin");
        var ghostId = new JsonPrimitive("skywars2:sad_ghost");
        var dolphins = game.items().pozdmanBot();
        var psycho = game.items().psycho();
        int balance = game.items().mana().get(owner);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var witness = new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW, base.level);
        witness.setPos(center.add(0, 0, 2));
        witness.setNoAi(true);
        witness.setHealth(.5f);
        require(base.level.addFreshEntity(witness), "a living witness for the two native summon paths");
        base.own(witness);
        var point = BlockPos.containing(center);
        base.set(point, Blocks.AIR.defaultBlockState());
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 0)) {
            var absent = new Vec3(1_048_576.25, center.y, 1_048_576.25);
            for (var id : List.of(dolphinId, ghostId)) {
                require(complete(host, queued(host, "lbc.summon", id, vector(absent))).getAsJsonArray().isEmpty()
                        && !base.level.hasChunkAt(BlockPos.containing(absent)), "native summon does not load absent destination");
            }
            base.field(true, center);
            require(game.items().magic().mechanismSuppressedAt(base.level, center)
                    && complete(host, queued(host, "lbc.summon", dolphinId, vector(center))).getAsJsonArray().isEmpty(),
                    "Shark David is blocked by a real TECH field");
            base.removeFields();
            base.field(false, center);
            require(game.items().magic().suppressedAt(base.level, center)
                    && complete(host, queued(host, "lbc.summon", ghostId, vector(center))).getAsJsonArray().isEmpty(),
                    "Sad Ghost is blocked by a real MAG field");
            var ids = complete(host, queued(host, "lbc.summon", dolphinId, vector(center))).getAsJsonArray();
            require(ids.size()==1, "native non-magical dolphin summon still works under MAG");
            var dolphin = base.level.getEntity(UUID.fromString(ids.get(0).getAsString()));
            require(dolphin instanceof Mob && dolphin.getType()==EntityTypes.DOLPHIN && dolphin.position().equals(center)
                    && dolphin.isNoGravity() && ((Mob)dolphin).getMaxHealth()==50
                    && game.items().communism().owner(dolphin)==owner && LbcTeams.allied(game, dolphin, owner)
                    && !dolphins.waiting(owner), "actual controlled David is created at the requested position with native attributes");
            try {
                var coin = dolphins.spawnCoin(dolphin);
                require(coin != null && coin.getOwner()==dolphin
                        && ItemStack.matches(dolphins.state(coin).cause().enchantmentWeapon(), gift),
                        "native coin keeps its dolphin and the exact borrowed item");
                dolphins.removed(coin);
                base.set(point, Blocks.STONE.defaultBlockState());
                var oldBoxes = Set.copyOf(base.level.getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
                        new AABB(center, center).inflate(3)));
                dolphins.dolphinTick(dolphin);
                var tnt = base.level.getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
                        new AABB(center, center).inflate(3)).stream().filter(body -> !oldBoxes.contains(body)).findFirst().orElseThrow();
                base.own(tnt);
                var charge = game.items().spellSources().source(tnt);
                require(tnt.position().equals(center) && tnt.getFuse()==2 && tnt.getOwner()==null
                        && game.items().spellSources().projectileController(tnt)==null
                        && charge != null && charge.unownedMobs() && charge.cause().friendlyFire()
                        && ItemStack.matches(charge.cause().enchantmentWeapon(), gift),
                        "David's native terrain-contact fuse and gift attribution are preserved");
                base.discard(tnt);
            } finally {
                for (var body : dolphins.snapshot()) dolphins.removed(body);
                base.set(point, Blocks.AIR.defaultBlockState());
            }
            base.removeFields();

            ids = complete(host, queued(host, "lbc.summon", ghostId, vector(center))).getAsJsonArray();
            require(ids.size()==1 && witness.isAlive(), "a standalone Sad Ghost does not run the Psycho execution");
            var actor = base.level.getEntity(UUID.fromString(ids.get(0).getAsString()));
            require(actor instanceof Mob, "actual hostile Vex body published");
            var ghost = (Mob)actor;
            base.own(ghost);
            require(ghost.getType()==EntityTypes.VEX && ghost.position().equals(center) && ghost.getMainHandItem().isEmpty()
                    && ghost.getHealth()==2_000_000_000f && ghost.getMaxHealth()==2_000_000_000f
                    && ghost.canAttack(owner) && psycho.owner(ghost)==owner,
                    "same unarmed Sad Ghost, legacy health and hostility to its caster");
            var graph = MatchActorCheckpoint.currentBodies(game);
            var links = new MatchSpellCheckpoint.Links(new MatchDamageSources(game, graph.values()));
            var checkpoint = psycho.checkpoint(links);
            psycho.unloaded(ghost);
            require(psycho.owner(ghost)==null && links.resolve(graph::get).isEmpty(),
                    "the restored private registry needs canonical ghost membership");
            checkpoint.prepare(links).run();
            psycho.publishFrozen();
            require(psycho.owner(ghost)==owner && ghost.doHurtTarget(base.level,witness)
                    && !witness.isAlive() && witness.getKillCredit()==owner && witness.getLastDamageSource().getEntity()==ghost,
                    "restored hostile ghost hits as itself, and lethal credit remains with the item user");
            var cause = game.items().indirectKills().babylonCause(witness.getLastDamageSource());
            require(cause != null && cause.friendlyFire() && ItemStack.matches(cause.enchantmentWeapon(),gift)
                    && game.items().mana().get(owner)==balance && owner.getMainHandItem()==main && owner.getOffhandItem()==off,
                    "both reusable summons retain their gift and avoid the native item costs and input effects");
            base.discard(ghost);
        } finally {
            base.removeFields();
            base.discard(witness);
        }
    }

    private static boolean directed(GenieWorldHost host, String spell, Vec3 from, Vec3 direction) {
        return complete(host, queued(host, "lbc.cast", new JsonPrimitive("skywars2:" + spell),
                vector(from), vector(direction))).getAsBoolean();
    }

    private static void terrainStaffCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var ruby = game.items().ruby();
        var stone = game.items().stone();
        var center = new BlockPos(owner.chunkPosition().getMinBlockX() + 8, 240,
                owner.chunkPosition().getMinBlockZ() + 8);
        var origin = Vec3.atCenterOf(center.west(5));
        var east = new Vec3(1, 0, 0);
        var resistance = net.minecraft.world.effect.MobEffects.RESISTANCE;
        var previousResistance = owner.getEffect(resistance);
        var main = owner.getMainHandItem();
        var offhand = owner.getOffhandItem();
        var mana = game.items().mana().get(owner);
        require(ruby.count() == 0 && stone.count() == 0 && origin.distanceToSqr(owner.getEyePosition()) > 900,
                "exclusive terrain-staff fixture uses an origin remote from the caster");
        for (var p : BlockPos.betweenClosed(center.offset(-6, -5, -5), center.offset(5, 5, 5))) {
            base.set(p, Blocks.AIR.defaultBlockState());
        }
        for (var p : BlockPos.betweenClosed(center.east(6), center.east(56))) {
            if (base.level.hasChunkAt(p)) base.remember(p);
        }
        try (var host = new GenieWorldHost(game, owner, new ItemStack(Items.AMETHYST_SHARD), () -> true, () -> 0)) {
            base.field(false, origin);
            require(!directed(host, "ruby_wand", origin, east) && !directed(host, "stone_wand", origin, east)
                            && ruby.count() == 0 && stone.count() == 0,
                    "MAG prevents both native charges at the supplied origin");
            base.removeFields();
            for (var id : List.of("ruby_wand", "academy_ruby_wand")) {
                var obstacle = center.west(4);
                base.set(obstacle, Blocks.STONE.defaultBlockState());
                require(directed(host, id, origin, east) && ruby.count() == 1, "native drill launched with " + id);
                var drill = ruby.snapshot().getFirst();
                base.own(drill);
                require(drill.position().equals(origin) && drill.getOwner() == owner
                                && drill.getDeltaMovement().normalize().equals(east),
                        "native drill carries the supplied aim and origin");
                ruby.tick();
                require(base.level.getBlockState(obstacle).isAir(), "travelling drill removes its first obstruction");
                base.discard(drill);
            }
            base.set(center, Blocks.BEDROCK.defaultBlockState());
            base.set(center.east(), Blocks.STONE.defaultBlockState());
            require(directed(host, "ruby_cutting_ray", origin, east)
                            && base.level.getBlockState(center).isAir() && base.level.getBlockState(center.east()).isAir(),
                    "cutting ray crosses and clears successive blocks including bedrock");

            base.set(center, Blocks.STONE.defaultBlockState());
            require(directed(host, "ruby_compression", origin, east)
                            && base.level.getBlockState(center).is(Blocks.BEDROCK),
                    "compression places its bedrock center inside the first block hit");
            for (var p : BlockPos.betweenClosed(center.offset(-5, -3, -3), center.offset(3, 3, 3))) {
                base.set(p, Blocks.AIR.defaultBlockState());
            }

            require(directed(host, "stone_wand", origin, east) && stone.count() == 1, "native moving rock charge");
            var rock = stone.snapshot().getFirst();
            base.own(rock);
            require(rock.position().equals(origin) && rock.getOwner() == owner && rock.getDeltaMovement().equals(east),
                    "rock charge uses the supplied origin and velocity");
            base.discard(rock);

            base.set(center, Blocks.STONE.defaultBlockState());
            require(directed(host, "academy_stone_wand", origin, east) && stone.count() == 0
                            && BlockPos.betweenClosedStream(center.offset(-4, -4, -4), center.offset(4, 4, 4))
                            .anyMatch(p -> !p.equals(center) && !base.level.getBlockState(p).isAir()),
                    "Academy rock ray places native terrain on impact without a flying charge");
            for (var p : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                base.set(p, Blocks.AIR.defaultBlockState());
            }
            base.set(center, Blocks.STONE.defaultBlockState());
            var target = stationaryCow(base, Vec3.atCenterOf(center.south(2)));
            try {
                float health = target.getHealth();
                require(directed(host, "stone_spikes", origin, east) && target.getHealth() < health,
                        "spiked ray does native area damage around its impact");
            } finally {
                base.discard(target);
            }

            var bridgeStart = center.above(8).west();
            for (var p : BlockPos.betweenClosed(bridgeStart.north(1), bridgeStart.south(1).east(30))) {
                base.set(p, Blocks.AIR.defaultBlockState());
            }
            require(directed(host, "stone_bridge", Vec3.atCenterOf(bridgeStart), east)
                            && base.level.getBlockState(bridgeStart).is(Blocks.CRYING_OBSIDIAN)
                            && !base.level.getBlockState(bridgeStart.east(6)).isAir(),
                    "bridge is oriented along the supplied horizontal aim from the explicit origin");
            owner.removeEffect(resistance);
            require(directed(host, "stone_shield", origin, east)
                            && owner.getEffect(resistance).getAmplifier() == 4
                            && owner.getEffect(resistance).getDuration() == 100,
                    "stone shield grants native Resistance V for 100 ticks to its caster");
            require(game.items().mana().get(owner) == mana && owner.getMainHandItem() == main
                            && owner.getOffhandItem() == offhand,
                    "terrain spells spend no staff mana and do not switch physical hands");
        } finally {
            base.removeFields();
            ruby.stop();
            stone.stop();
            owner.removeEffect(resistance);
            if (previousResistance != null) owner.addEffect(previousResistance);
        }
    }

    private static void starsAndWall(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var originChunk = owner.chunkPosition();
        int y = Math.min(owner.getBlockY() + 24, base.level.getMaxY() - 10);
        var target = new BlockPos(originChunk.getMinBlockX() + 8, y, originChunk.getMinBlockZ() + 8);
        var origin = Vec3.atCenterOf(target.west(5));
        var east = new Vec3(1, 0, 0);
        var homing = game.items().homing();
        require(homing.count() == 0 && homing.pendingRayCount() == 0, "exclusive Falling Stars fixture");

        var volume = BlockPos.betweenClosedStream(target.offset(-7, -3, -7), target.offset(7, 8, 7))
                .map(BlockPos::immutable).toList();
        for (var at : volume) base.set(at, Blocks.AIR.defaultBlockState());
        base.set(target, Blocks.STONE.defaultBlockState());

        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        int mana = game.items().mana().get(owner);
        float yaw = owner.getYRot();
        owner.setYRot(0);
        try (var host = new GenieWorldHost(game, owner, new ItemStack(Items.BLAZE_ROD), () -> true, () -> 123L)) {
            for (String spell : List.of("falling_stars", "academy_falling_stars", "falling_stars_big")) {
                require(directed(host, spell, origin, east), "native directed " + spell + " accepted");
                drainHoming(game);
                var shots = homing.snapshot();
                boolean large = spell.equals("falling_stars_big");
                require(shots.size() == (large ? 1 : 14) && shots.stream().allMatch(shot -> shot.getOwner() == owner
                                && base.level.getEntity(shot.getUUID()) == shot && game.items().currentEpoch(shot)
                                && shot.getDeltaMovement().equals(new Vec3(0, -.2, 0))),
                        "directed " + spell + " uses the authored native bodies and descent, got " + shots.size());
                double minHeight = shots.stream().mapToDouble(Entity::getY).min().orElseThrow();
                require(minHeight == origin.y + (large ? 400 : 50) && (large
                                ? GameChecks.fireballPower(shots.getFirst()) == 15
                                : shots.stream().map(GameChecks::fireballPower).filter(power -> power == 2).count() == 13),
                        "native Falling Stars placement and power refer to the hit, not the held item's aim");
                homing.stop();
            }
            require(directed(host, "falling_stars_fire_rain", origin, east), "native Fire Rain accepted");
            drainHoming(game);
            require(homing.rainCount() == 1 && homing.snapshot().isEmpty(), "rain enters the real sustained effect");
            homing.tick();
            require(homing.snapshot().size() == 3 && homing.snapshot().stream().allMatch(shot -> shot.getOwner() == owner
                            && GameChecks.fireballPower(shot) == 1 && shot.getY() == origin.y + 80),
                    "one native Fire Rain tick makes three power-one fireballs at hit Y plus 80");
            homing.stop();

            for (String spell : List.of("wall", "academy_wall")) {
                require(directed(host, spell, origin, east)
                                && base.level.getBlockState(target.north(3).above(3)).is(Blocks.BRICKS)
                                && base.level.getBlockState(target.west(3).above(3)).isAir()
                                && base.level.getBlockState(target).is(Blocks.STONE),
                        "vertical " + spell + " faces the explicit eastward ray and preserves the solid hit");
                for (var at : volume) if (base.level.getBlockState(at).is(Blocks.BRICKS))
                    base.set(at, Blocks.AIR.defaultBlockState());
            }
            require(directed(host, "wall_horizontal", origin, east)
                            && base.level.getBlockState(target.west(3).north(3)).is(Blocks.BRICKS)
                            && base.level.getBlockState(target.above()).isAir(), "native seven-by-seven horizontal wall");
            var beforeDrops = new HashSet<UUID>();
            base.level.getAllEntities().forEach(e -> beforeDrops.add(e.getUUID()));
            require(directed(host, "wall_dismantle", Vec3.atCenterOf(target.above(5)), new Vec3(0, -1, 0))
                            && base.level.getBlockState(target.north(2)).isAir()
                            && base.level.getBlockState(target.north(3)).is(Blocks.BRICKS)
                            && base.level.getBlockState(target).is(Blocks.STONE),
                    "dismantling removes only bricks in the native five-by-five-by-five region");
            int recovered = 0;
            for (var e : base.level.getAllEntities()) if (e instanceof net.minecraft.world.entity.item.ItemEntity drop
                    && !beforeDrops.contains(drop.getUUID()) && drop.getItem().is(Items.BRICKS)) {
                base.own(drop);
                recovered += drop.getItem().getCount();
            }
            require(recovered == 24, "real brick recovery gives one brick block per removal, got " + recovered);
            for (var at : volume) if (base.level.getBlockState(at).is(Blocks.BRICKS))
                base.set(at, Blocks.AIR.defaultBlockState());
            require(directed(host, "wall_stairs", origin, east)
                            && volume.stream().filter(at -> base.level.getBlockState(at).is(Blocks.BRICK_STAIRS)).count() == 45,
                    "stair mode publishes the original 45-stair native template");

            base.field(false, origin);
            try {
                require(!directed(host, "wall_horizontal", origin, east)
                                && !directed(host, "falling_stars_big", origin, east) && homing.pendingRayCount() == 0,
                        "a real MAG field rejects both families at the scripted origin");
            } finally {
                base.removeFields();
            }
            require(game.items().mana().get(owner) == mana && owner.getMainHandItem() == main
                            && owner.getOffhandItem() == off,
                    "borrowed casts neither pay mana nor claim a paid refund or alter held items");
        } finally {
            owner.setYRot(yaw);
            homing.stop();
        }
    }

    private static void drainHoming(GameController game) {
        var homing = game.items().homing();
        for (int i = 0; i < 100 && homing.pendingRayCount() > 0; i++) game.items().exasperato().tick();
        require(homing.pendingRayCount() == 0, "loaded Falling Stars ray reaches one native terminal hit");
    }

    private static void advancedSnowCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var service = game.items().advancedSnow();
        var rays = game.items().exasperato();
        require(service.snapshot().isEmpty() && rays.pendingSearches() == 0, "exclusive snow and teleport fixture");

        var chunk = owner.chunkPosition();
        int y = Math.min(owner.getBlockY() + 18, base.level.getMaxY() - 102);
        var target = new BlockPos(chunk.getMinBlockX() + 8, y, chunk.getMinBlockZ() + 8);
        var origin = Vec3.atCenterOf(target.west(5));
        var east = new Vec3(1, 0, 0);
        for (var cell : BlockPos.betweenClosed(target.offset(-5, -1, -1), target.offset(4, 4, 1)))
            base.set(cell.immutable(), Blocks.AIR.defaultBlockState());
        for (int height = y + 5; height <= base.level.getMaxY(); height++)
            base.set(new BlockPos(target.getX(), height, target.getZ()), Blocks.AIR.defaultBlockState());
        for (int dy = 0; dy <= 3; dy++) {
            base.set(target.above(dy), Blocks.STONE.defaultBlockState());
            base.set(target.east().above(dy), Blocks.STONE.defaultBlockState());
        }

        var position = owner.position();
        var movement = owner.getDeltaMovement();
        var fallDistance = owner.fallDistance;
        float yaw = owner.getYRot(), pitch = owner.getXRot();
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        int mana = game.items().mana().get(owner);
        var formerDamage = owner.getEffect(MobEffects.INSTANT_DAMAGE);
        var formerFalling = owner.getEffect(MobEffects.SLOW_FALLING);
        owner.removeEffect(MobEffects.INSTANT_DAMAGE);
        owner.removeEffect(MobEffects.SLOW_FALLING);
        owner.setYRot(0);
        owner.setXRot(0);
        try (var host = new GenieWorldHost(game, owner, new ItemStack(Items.STICK), () -> true, () -> 123L)) {
            var advancement = Objects.requireNonNull(game.server().getAdvancements().get(
                    net.minecraft.resources.Identifier.parse("lbc:true_advancements/staffs/teleport")));
            var progress = owner.getAdvancements().getOrStartProgress(advancement);
            var criteria = new HashSet<String>();
            progress.getCompletedCriteria().forEach(criteria::add);
            var high = Vec3.atCenterOf(target.above(98));
            require(directed(host, "advanced_ender", high, new Vec3(0, -1, 0)) && owner.position().equals(position),
                    "directed Ender ray uses native deferred search");
            drainAdvancedRay(rays);
            require(owner.position().distanceTo(Vec3.atCenterOf(target.above(4))) < .001
                            && owner.hasEffect(MobEffects.INSTANT_DAMAGE),
                    "ordinary Ender teleports below the remote origin with its native self-damage");
            for (var criterion : progress.getCompletedCriteria())
                require(criteria.contains(criterion), "queued borrowed Ender must not award the original staff achievement");
            owner.removeEffect(MobEffects.INSTANT_DAMAGE);
            owner.fallDistance = 32;

            require(directed(host, "academy_advanced_ender", origin, east), "safe Ender accepted");
            drainAdvancedRay(rays);
            var slow = owner.getEffect(MobEffects.SLOW_FALLING);
            require(owner.position().equals(Vec3.atCenterOf(target.west())) && slow != null && slow.getDuration() == 20
                            && !owner.hasEffect(MobEffects.INSTANT_DAMAGE) && owner.fallDistance == 0,
                    "safe Ender arrives before the wall and protects against falling");
            require(directed(host, "advanced_ender_top", origin, east), "topmost Ender accepted");
            drainAdvancedRay(rays);
            require(owner.getY() == y + 4.5 && owner.blockPosition().getX() == target.getX(),
                    "topmost Ender lands on the roof above the first terrain hit");
            require(directed(host, "advanced_ender_clip", origin, east), "wall clip Ender accepted");
            drainAdvancedRay(rays);
            require(owner.position().equals(Vec3.atCenterOf(target.east(2))),
                    "wall clip exits after crossing both solid blocks");

            require(directed(host, "snow_wand", origin, east) && service.snapshot().size() == 1
                            && service.snapshot().getFirst().entity().position().equals(origin.add(0, 2, 0)),
                    "ordinary Snow creates a native cloud at the explicit origin");
            service.forget(owner);
            require(directed(host, "academy_snow_wand", origin, east) && service.snapshot().isEmpty(),
                    "Academy Snow waits for the terrain hit");
            drainAdvancedRay(rays);
            require(service.snapshot().size() == 1, "Academy Snow creates exactly one cloud");
            var state = service.snapshot().getFirst();
            var cloud = state.entity();
            require(cloud.getOwner() == owner && cloud.getRadius() == 4 && cloud.getDuration() == 250
                            && cloud.position().distanceTo(new Vec3(target.getX(), origin.y + 3, origin.z)) < .001,
                    "Academy Snow retains ownership, radius, duration and position above the wall hit");
            var cow = stationaryCow(base, cloud.position().add(0, 0, 1));
            try {
                service.pulse(state);
                require(game.items().frost().cold(cow) == 2, "borrowed cloud runs the actual freezing effect");
            } finally {
                game.items().frost().clear(cow);
                base.discard(cow);
            }

            base.field(false, origin);
            try {
                require(!directed(host, "advanced_ender_clip", origin, east)
                                && !directed(host, "snow_wand", origin, east)
                                && rays.pendingSearches() == 0 && service.snapshot().size() == 1,
                        "antimagic at the explicit origin suppresses both direct abilities");
            } finally {
                base.removeFields();
            }
            require(game.items().mana().get(owner) == mana && owner.getMainHandItem() == main
                            && owner.getOffhandItem() == off,
                    "directed snow and teleport leave mana and the actual held items alone");
        } finally {
            service.forget(owner);
            owner.removeEffect(MobEffects.INSTANT_DAMAGE);
            owner.removeEffect(MobEffects.SLOW_FALLING);
            if (formerDamage != null) owner.addEffect(formerDamage);
            if (formerFalling != null) owner.addEffect(formerFalling);
            owner.teleportTo(base.level, position.x, position.y, position.z, Set.of(), yaw, pitch, true);
            owner.fallDistance = fallDistance;
            owner.setDeltaMovement(movement);
        }
    }

    private static void permafrostCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var ice = game.items().iceWand();
        var frost = game.items().frost();
        var ledger = game.items().indirectKills();
        require(ice.count() == 0, "exclusive Permafrost fixture");
        var chunk = owner.chunkPosition();
        int y = Math.min(owner.getBlockY() + 28, base.level.getMaxY() - 12);
        var wall = new BlockPos(chunk.getMinBlockX() + 8, y, chunk.getMinBlockZ() + 6);
        for (var cell : BlockPos.betweenClosed(wall.offset(-6, -1, -2), wall.offset(1, 2, 7)))
            base.set(cell.immutable(), Blocks.AIR.defaultBlockState());
        for (int z : List.of(0, 5)) {
            base.set(wall.south(z), Blocks.STONE.defaultBlockState());
            base.set(wall.south(z).above(), Blocks.STONE.defaultBlockState());
        }
        var origin = Vec3.atCenterOf(wall.west(5));
        var witness = stationaryCow(base, Vec3.atBottomCenterOf(wall.west(1).south()));
        var frozen = stationaryCow(base, Vec3.atBottomCenterOf(wall.north(5)));
        for (var cow : List.of(witness, frozen)) {
            cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
            cow.setHealth(100);
        }
        // Frost temporarily writes an ice shell, including cells outside the shot corridor.
        for (var cell : BlockPos.betweenClosed(frozen.blockPosition().offset(-2,-2,-2),frozen.blockPosition().offset(2,3,2)))
            base.remember(cell.immutable());
        var gift = new ItemStack(Items.AMETHYST_SHARD);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        int mana = game.items().mana().get(owner);
        var east = new Vec3(1,0,0);
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 123L)) {
            for (var name : List.of("ice_wand", "academy_ice_wand")) {
                var point = name.equals("ice_wand") ? origin : origin.add(0,0,5);
                require(directed(host, name, point, east), "script launches " + name);
                var shot = ice.shotSnapshot().getFirst();
                require(shot.position().equals(point) && shot.getDeltaMovement().equals(east.scale(1.5))
                                && shot.getOwner() == owner && shot.display.position().equals(point),
                        "native ice shot and display use the supplied origin and velocity");
                for (int i = 0; i < 10 && !shot.isRemoved(); i++) shot.tick();
                require(shot.isRemoved() && shot.display.isRemoved() && ice.shotSnapshot().isEmpty(),
                        "native block impact retires the flying crystal and its display");
            }
            require(ice.snapshot().size() == 2 && base.level.getBlockState(wall.west()).is(Blocks.BARRIER)
                            && base.level.getBlockState(wall.west().south(5)).is(Blocks.BARRIER)
                            && witness.getHealth() == 95 && frost.cold(witness) == 10
                            && ledger.enchantmentWeapon(witness.getLastDamageSource()).is(Items.AMETHYST_SHARD),
                    "real ice impact leaves two attached crystals and gives native damage and cold with the original item");
            frozen.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS, 100, 9), owner);
            frost.add(frozen, owner);
            require(frost.isFrozen(frozen), "actual native frozen witness");
            frozen.removeEffect(MobEffects.RESISTANCE);
            frozen.setInvulnerableTime(0);
            frozen.damageCooldownTime = 0;
            witness.setInvulnerableTime(0);
            witness.damageCooldownTime = 0;

            var detonate = new JsonPrimitive(GenieNative.TargetedCast.ICE_WAND_DETONATE.id.toString());
            var shatter = new JsonPrimitive(GenieNative.TargetedCast.ACADEMY_ICE_WAND_SHATTER.id.toString());
            var center = Vec3.atCenterOf(wall.west(1));
            base.field(false, origin);
            try {
                require(!directed(host,"ice_wand",origin,east)
                                && !complete(host,queued(host,"lbc.cast_at",detonate,vector(origin))).getAsBoolean()
                                && ice.snapshot().size() == 2,
                        "MAG suppresses both the supplied launch origin and detonation point");
            } finally { base.removeFields(); }
            require(complete(host,queued(host,"lbc.cast_at",detonate,vector(center))).getAsBoolean()
                            && ice.count() == 0 && base.level.getBlockState(wall.west()).isAir()
                            && base.level.getBlockState(wall.west().south(5)).isAir()
                            && witness.getHealth() == 90 && frost.cold(witness) == 20
                            && ledger.enchantmentWeapon(witness.getLastDamageSource()).is(Items.AMETHYST_SHARD)
                            && frost.isFrozen(frozen),
                    "ordinary detonation handles owned crystal damage and restoration without shattering frozen mobs");
            require(complete(host,queued(host,"lbc.cast_at",shatter,vector(frozen.position()))).getAsBoolean()
                            && !frost.isFrozen(frozen) && frozen.getHealth() < 100,
                    "native Academy shatter thaws and damages the frozen mob at the supplied point");
            require(game.items().mana().get(owner) == mana && owner.getMainHandItem() == main && owner.getOffhandItem() == off,
                    "all four Permafrost calls leave mana and physical hands unchanged");
        } finally {
            ice.forget(owner);
            for (var cow : List.of(witness, frozen)) {
                frost.clear(cow);
                ledger.clear(cow);
                base.discard(cow);
            }
        }
    }

    private static void fartusCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var service = game.items().fartus();
        require(service.count() == 0, "exclusive Fartus fixture");
        var chunk = owner.chunkPosition();
        int y = Math.min(owner.getBlockY() + 31, base.level.getMaxY() - 8);
        var pos = new BlockPos(chunk.getMinBlockX() + 4, y, chunk.getMinBlockZ() + 7);
        for (var cell : BlockPos.betweenClosed(pos.offset(-1,-1,-2),pos.offset(7,2,2)))
            base.set(cell.immutable(), Blocks.AIR.defaultBlockState());
        var origin = Vec3.atCenterOf(pos);
        var east = new Vec3(1,0,0);
        var gift = new ItemStack(Items.AMETHYST_SHARD);
        var mana = game.items().mana();
        var counter = mana.score(owner,"fartus_sword");
        int former = counter.get();
        counter.set(9);
        int beforeMana = mana.get(owner);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var victim = stationaryCow(base, origin.add(2,-.5,0));
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 0)) {
            require(directed(host, "fartus_wand", origin, east), "random Fartus cast");
            var random = service.snapshot();
            require(random.size() == 5 && random.getFirst().kind != LbcFartus.Kind.HOMING
                            && random.stream().allMatch(shot -> shot.kind == random.getFirst().kind),
                    "one ordinary random branch applies to the full five-shot volley");
            random.forEach(base::discard);

            require(directed(host, "fartus_banana_volley", origin, east), "explicit banana volley");
            var bananas = service.snapshot();
            require(bananas.size() == 5 && bananas.stream().allMatch(shot ->
                            shot.kind == LbcFartus.Kind.BANANA && shot.getOwner() == owner
                            && shot.position().equals(origin) && shot.display.position().equals(origin)
                            && shot.launchYaw == -90 && shot.launchPitch == 0
                            && shot.getDeltaMovement().x >= 1 && shot.getDeltaMovement().x <= 2
                            && Math.abs(shot.getDeltaMovement().y) <= .5 && Math.abs(shot.getDeltaMovement().z) <= .5),
                    "native physical bananas preserve the remote origin, supplied aim and original component spread");
            bananas.forEach(base::discard);

            require(directed(host, "fartus_label_volley", origin, east), "explicit flying label volley");
            var labels = service.snapshot();
            require(labels.size() == 5, "exactly five native labels");
            for (int i=0;i<labels.size();i++) {
                var label = labels.get(i);
                require(label.kind == LbcFartus.Kind.LABEL && label.position().equals(origin)
                                && label.getDeltaMovement().distanceTo(Vec3.directionFromRotation(0,-60-i*15).scale(.4)) < 1e-6,
                        "native label fan preserves its exact angular spacing");
            }
            var central = labels.get(2);
            for (var label : labels) if (label != central) base.discard(label);
            for (int i=0;i<10&&!central.isRemoved();i++) service.flight(central);
            require(central.isRemoved() && central.display.isRemoved()
                            && victim.getEffect(MobEffects.WITHER).getDuration() == 300
                            && victim.getEffect(MobEffects.POISON).getAmplifier() == 2
                            && victim.getEffect(MobEffects.SLOWNESS).getAmplifier() == 0,
                    "real label collision applies the original three effects and retires its display");
            require(central.borrowedCause.enchantmentWeapon().is(Items.AMETHYST_SHARD),
                    "the delayed label retains the accepted Genie weapon");

            base.field(false, origin);
            try {
                require(!directed(host,"academy_fartus_wand",origin,east) && service.count()==0,
                        "origin antimagic suppresses the native borrowed palm cast");
            } finally { base.removeFields(); }
            require(directed(host,"academy_fartus_wand",origin,east), "Academy palm sword volley");
            require(service.count() == 5 && service.snapshot().stream().allMatch(shot -> {
                        var motion = shot.getDeltaMovement();
                        // Native Mth sine/cosine table lookup can differ by one 65536th of a turn.
                        return shot.kind == LbcFartus.Kind.HOMING && shot.position().equals(origin) && shot.getOwner()==owner
                                && Math.abs(motion.length()-.4) < 4e-5
                                && Math.abs(Math.toDegrees(Math.atan2(motion.z,motion.x))) <= 50.02
                                && Math.abs(Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()))) <= 50.02;
                    }),
                    "all five native homing projectiles preserve speed and the requested aim: " + service.snapshot().stream()
                            .map(shot -> shot.kind + " origin=" + shot.position() + " owner=" + (shot.getOwner()==owner)
                                    + " speed=" + shot.getDeltaMovement().length()
                                    + " aim=" + shot.getDeltaMovement().normalize().dot(east)).toList());
            require(counter.get() == 9 && mana.get(owner) == beforeMana
                            && owner.getMainHandItem() == main && owner.getOffhandItem() == off,
                    "the four borrowed casts leave sword charges, mana and actual equipment alone");
        } finally {
            service.snapshot().forEach(base::discard);
            counter.set(former);
            game.items().indirectKills().clear(victim);
            base.discard(victim);
        }
    }

    private static void musicStaffCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var staff = game.items().musicStaff();
        require(staff.arrows().isEmpty(), "exclusive Staff 52 projectile fixture");
        var origin = new Vec3(owner.chunkPosition().getMinBlockX() + 8.5, 240,
                owner.chunkPosition().getMinBlockZ() + 8.5);
        var east = new Vec3(1, 0, 0);
        var target = stationaryCow(base, origin.add(2.5, -.65, 0));
        var gift = new ItemStack(Items.AMETHYST_SHARD);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var mana = game.items().mana().get(owner);
        var playing = staff.playing();
        require(owner.getEyePosition().distanceToSqr(origin) > 900, "Staff 52 fires from a remote origin");
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 0)) {
            base.field(false, origin);
            require(!directed(host, "music_staff", origin, east) && staff.arrows().isEmpty(),
                    "MAG rejects a Staff 52 shot at the supplied origin");
            base.removeFields();
            require(directed(host, "music_staff", origin, east) && staff.arrows().size() == 1,
                    "ordinary Staff 52 emits one native arrow");
            var arrow = staff.arrows().getFirst();
            base.own(arrow);
            var source = game.items().spellSources().source(arrow);
            require(arrow.position().equals(origin) && arrow.getOwner() == owner && arrow.fixedArrowDamage() == 8
                            && arrow.getDeltaMovement().normalize().dot(east) > .999
                            && arrow.model != null && arrow.model.display() != null
                            && ItemStack.matches(arrow.getWeaponItem(), gift)
                            && source != null && source.cause().friendlyFire()
                            && source.cause().player().equals(owner.getUUID()),
                    "native origin, aim, visual, gift snapshot and WILD attribution");
            float health = target.getHealth();
            for (int tick = 0; tick < 3 && !arrow.isRemoved() && target.getHealth() == health; tick++) arrow.tick();
            require(target.getHealth() == health - 8, "remote arrow applies fixed eight native damage");
            base.discard(arrow);
            base.discard(target);

            boolean single = false, doubleShot = false;
            for (int cast = 0; cast < 40 && !(single && doubleShot); cast++) {
                require(directed(host, "academy_music_staff", origin, east), "Academy Staff 52 cast accepted");
                var arrows = staff.arrows();
                arrows.forEach(base::own);
                require(arrows.size() == 1 || arrows.size() == 2, "one or two Academy arrows");
                require(arrows.stream().allMatch(shot -> shot.fixedArrowDamage() == 16
                                && shot.getDeltaMovement().normalize().dot(east) > .999),
                        "Academy shots keep sixteen damage and the directed native spread");
                if (arrows.size() == 1) {
                    single = true;
                    require(arrows.getFirst().position().equals(origin), "single shot uses the supplied camera origin");
                } else {
                    doubleShot = true;
                    var first = arrows.get(0);
                    var second = arrows.get(1);
                    require(first.position().add(second.position()).scale(.5).distanceToSqr(origin.add(.3, 0, 0)) < 1e-10
                                    && Math.abs(Math.abs(first.getZ() - second.getZ()) - .8) < 1e-8,
                            "Academy double uses opposite native muzzle offsets");
                    require(first.getDeltaMovement().distanceToSqr(second.getDeltaMovement()) > 1e-12,
                            "Academy arrows sample independent spread");
                }
                arrows.forEach(base::discard);
            }
            require(single && doubleShot && game.items().mana().get(owner) == mana && staff.playing() == playing
                            && owner.getMainHandItem() == main && owner.getOffhandItem() == off,
                    "both Academy branches leave real hands, music and mana untouched");
        } finally {
            base.removeFields();
            staff.arrows().forEach(base::discard);
        }
    }

    private static void voidArcanumCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var service = game.items().voidArcanum();
        var mana = game.items().mana();
        int beforeMana = mana.get(owner);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var center = new Vec3(owner.chunkPosition().getMinBlockX() + 8.5, 240,
                owner.chunkPosition().getMinBlockZ() + 8.5);
        var gift = new ItemStack(Items.AMETHYST_SHARD);
        var primary = new JsonPrimitive("skywars2:void_arcanum");
        require(service.count() == 0 && !LbcVoidArcanum.isArcanum(main) && !LbcVoidArcanum.isArcanum(off),
                "exclusive native Void Arcanum fixture with no Arcanum held");
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 0)) {
            base.field(false, center);
            require(!complete(host, queued(host, "lbc.cast_at", primary, vector(center))).getAsBoolean()
                    && service.count() == 0, "MAG in the remote chunk prevents casting a black hole");
            base.removeFields();
            require(complete(host, queued(host, "lbc.cast_at", primary, vector(center))).getAsBoolean(),
                    "ordinary default lifetime cast accepted");
            var defaultHole = service.snapshot().getFirst();
            require(defaultHole.members.getFirst().remainingTicks() == 200, "default active lifetime is 200 ticks");
            base.own(defaultHole.marker);
            base.discard(defaultHole.marker);

            var cow = stationaryCow(base, center);
            for (int tier = 1; tier <= 5; tier++) {
                var id = tier == 1 ? primary : new JsonPrimitive("skywars2:void_arcanum_level_" + tier);
                require(complete(host, queued(host, "lbc.cast_at", id, vector(center), new JsonPrimitive(1))).getAsBoolean(),
                        "script emits real Void Arcanum level " + tier);
                var hole = service.snapshot().getFirst();
                base.own(hole.marker);
                require(service.count() == 1 && hole.level == tier && hole.point.equals(center)
                                && !hole.marker.shouldBeSaved() && service.upkeep(owner.getUUID()) == 0,
                        "native body starts at exact requested center and tier without paid upkeep");
                if (tier == 5) {
                    game.items().timeStop().begin(base.level, 40);
                    try {
                        service.tick();
                        require(service.count() == 1 && hole.members.getFirst().remainingTicks() == 1
                                        && hole.visual == null && hole.point.equals(center),
                                "actual Time Stop suspends the scripted arcanum's creation frame and expiry");
                    } finally {
                        game.items().timeStop().end(base.level);
                    }
                }
                float health = cow.getHealth();
                service.tick();
                double radius = new double[] {0, 2, 3, 5, 9, 24}[tier] / 14;
                require(hole.visual != null && hole.visual.disc == (tier == 5)
                                && Math.abs(hole.visual.radius - radius) < 1e-6 && cow.getHealth() < health
                                && ItemStack.matches(game.items().indirectKills().enchantmentWeapon(cow.getLastDamageSource()), gift),
                        "native shader handle, damage and captured gift attribution for level " + tier);
                service.tick();
                require(service.count() == 0 && hole.marker.isRemoved() && hole.visual == null,
                        "one active tick expires the native body and visual");
            }
            base.discard(cow);
            require(mana.get(owner) == beforeMana && owner.getMainHandItem() == main && owner.getOffhandItem() == off,
                    "five script tiers require no held Arcanum and spend no mana");

            var activation = mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("void_arcanum_mana_activate"), "lbc.math");
            int formerActivation = activation.get();
            int upkeep = mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("void_arcanum_mana_tick"), "lbc.math").get();
            try {
                activation.set(Integer.MAX_VALUE);
                mana.set(owner, 5000);
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LbcVoidArcanum.ITEM));
                var paid = service.spawn(owner, center);
                require(paid != null, "ordinary native hole remains an eligible merging body");
                base.own(paid.marker);
                var third = new JsonPrimitive("skywars2:void_arcanum_level_3");
                require(complete(host, queued(host, "lbc.cast_at", third, vector(center), new JsonPrimitive(1))).getAsBoolean(),
                        "one temporary level-three contribution next to the held-item hole");
                service.snapshot().forEach(hole -> base.own(hole.marker));
                service.tick();
                require(service.count() == 1 && paid.level == 4 && mana.get(owner) == 5000 - upkeep,
                        "real merge adds the temporary strength and charges only the held contribution");
                service.tick();
                require(service.count() == 1 && paid.level == 1 && mana.get(owner) == 5000 - 2 * upkeep,
                        "the merged body loses the expired strength while the paid contribution survives");
            } finally {
                activation.set(formerActivation);
            }
        } finally {
            base.removeFields();
            service.stop();
            owner.setItemInHand(InteractionHand.MAIN_HAND, main);
            owner.setItemInHand(InteractionHand.OFF_HAND, off);
            mana.set(owner, beforeMana);
        }
    }

    private static void fireblazeCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner = base.owner;
        var level = base.level;
        var service = game.items().blaze();
        require(service.terrainCount() == 0, "exclusive Fireblaze terrain fixture");
        var center = new Vec3(owner.chunkPosition().getMinBlockX() + 8.5, 240, owner.chunkPosition().getMinBlockZ() + 8.5);
        var ignored = new ArrayList<LivingEntity>();
        for (var body : level.getAllEntities()) {
            if (body instanceof LivingEntity living && living != owner && living.entityTags().add("strict_map_object"))
                ignored.add(living);
        }
        var near = stationaryCow(base, center);
        var far = stationaryCow(base, center.add(0, 40, 0));
        var gift = new ItemStack(Items.AMETHYST_SHARD);
        var oldResistance = owner.getEffect(MobEffects.FIRE_RESISTANCE);
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var mana = game.items().mana().get(owner);
        var before = new HashSet<UUID>();
        for (var body : level.getAllEntities()) before.add(body.getUUID());
        var shots = new ArrayList<Projectile>();
        try (var host = new GenieWorldHost(game, owner, gift, () -> true, () -> 0)) {
            java.util.function.Function<String, Boolean> cast = mode ->
                    complete(host, queued(host, "lbc.cast_at", new JsonPrimitive("skywars2:" + mode), vector(center))).getAsBoolean();
            require(owner.position().distanceToSqr(center) > 900, "remote Fireblaze origin");
            base.field(false, center);
            require(!cast.apply("fireblaze_wand"), "native MAG blocks Fireblaze at its selected origin");
            base.removeFields();
            for (String mode : List.of("fireblaze_wand", "academy_fireblaze_world", "academy_fireblaze_mega")) {
                require(cast.apply(mode), "scripted Fireblaze accepts " + mode);
                for (var body : level.getAllEntities()) {
                    if (body instanceof Projectile shot && !before.contains(body.getUUID())) {
                        shots.add(shot);
                        base.own(shot);
                        before.add(shot.getUUID());
                    }
                }
                boolean ordinary = mode.equals("fireblaze_wand");
                boolean mega = mode.equals("academy_fireblaze_mega");
                require(shots.size() == (ordinary ? 1 : 2), "original Fireblaze target radius " + mode);
                for (var shot : shots) {
                    var enemy = shot.getY() < center.y + 20 ? near : far;
                    var towards = enemy.position().add(0, 1, 0).subtract(shot.position());
                    require((mega ? shot instanceof LargeFireball big && GameChecks.fireballPower(big) == 5
                                    : shot instanceof SmallFireball)
                            && towards.lengthSqr() <= 36 && towards.normalize().dot(shot.getDeltaMovement().normalize()) > .9999
                            && shot.getOwner() == owner && GenieCombat.spellWeapon(shot, owner).is(Items.AMETHYST_SHARD),
                            "native ball, exact motion and captured gift for " + mode);
                }
                shots.forEach(base::discard);
                shots.clear();
            }

            // Predict a column in this already resident chunk so the full native write can be restored.
            base.discard(far);
            long seed = 0;
            BlockPos column;
            do {
                var random = net.minecraft.util.RandomSource.create(++seed);
                column = near.blockPosition().offset(random.nextInt(61) - 30, 0, random.nextInt(61) - 30);
            } while (!owner.chunkPosition().equals(net.minecraft.world.level.ChunkPos.containing(column)));
            owner.getRandom().setSeed(seed);
            for (int y = 0; y <= 319; y++) {
                var pos = column.atY(y);
                base.set(pos, level.getBlockState(pos));
            }
            owner.removeEffect(MobEffects.FIRE_RESISTANCE);
            require(cast.apply("academy_fireblaze_pillars") && service.terrainCount() == 1
                            && owner.getEffect(MobEffects.FIRE_RESISTANCE).isInfiniteDuration(),
                    "actual Pillars queue and native infinite resistance");
            for (int i = 0; i < 100 && service.terrainCount() != 0; i++) service.tick();
            require(service.terrainCount() == 0 && level.getBlockState(column.atY(0)).is(Blocks.LAVA)
                            && level.getBlockState(column.atY(159)).is(Blocks.LAVA)
                            && level.getBlockState(column.atY(319)).is(Blocks.LAVA), "native Pillars runs from Y=0 to Y=319");
            owner.removeEffect(MobEffects.FIRE_RESISTANCE);
            require(cast.apply("academy_fireblaze_kiuas") && service.terrainCount() == 1
                            && owner.getEffect(MobEffects.FIRE_RESISTANCE).isInfiniteDuration(),
                    "Kiuas queues the original large fire-area work and applies resistance");
            require(game.items().mana().get(owner) == mana && owner.getMainHandItem() == main && owner.getOffhandItem() == off,
                    "all five Fireblaze calls preserve wand mana and physical hands");
        } finally {
            service.clearFutureTerrain();
            shots.forEach(base::discard);
            owner.removeEffect(MobEffects.FIRE_RESISTANCE);
            if (oldResistance != null) owner.addEffect(oldResistance);
            base.discard(near);
            base.discard(far);
            ignored.forEach(body -> body.entityTags().remove("strict_map_object"));
        }
    }

    private static void awakenedAcidCasts(GameController game, WoolBucketChecks.Fixture base) {
        var owner=base.owner;
        var service=game.items().awakenedAcid();
        var clouds=game.items().clouds();
        var ledger=game.items().indirectKills();
        require(clouds.acidSnapshot().isEmpty(),"exclusive Awakened Acid fixture");
        var chunk=owner.chunkPosition();
        var cell=new BlockPos(chunk.getMinBlockX()+5,Math.min(owner.getBlockY()+35,base.level.getMaxY()-6),chunk.getMinBlockZ()+7);
        for(var p:BlockPos.betweenClosed(cell.offset(-1,-1,-2),cell.offset(5,2,3)))
            base.set(p.immutable(),Blocks.AIR.defaultBlockState());
        var origin=Vec3.atCenterOf(cell);
        var east=new Vec3(1,0,0);
        var gift=new ItemStack(Items.AMETHYST_SHARD);
        int mana=game.items().mana().get(owner);
        int paid=service.pending(owner);
        var main=owner.getMainHandItem();
        var off=owner.getOffhandItem();
        var victim=stationaryCow(base,origin.add(0,-.5,2));
        victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        victim.setHealth(100);
        try(var host=new GenieWorldHost(game,owner,gift,()->true,()->0)) {
            for(var mode:List.of("wand","caustic","gatling")) for(boolean ignited:List.of(false,true)) {
                require(directed(host,"awakened_acid_"+mode+(ignited?"_ignited":""),origin,east),
                        "native directed " + mode + " " + (ignited?"burning":"normal"));
                var shots=clouds.acidSnapshot();
                int variant=mode.equals("wand")?0:mode.equals("caustic")?1:2;
                require(shots.size()==(variant==2?8:1) && shots.stream().allMatch(shot ->
                                shot.getOwner()==owner && shot.position().equals(origin)
                                        && LbcClouds.awakenedVariant(shot)==variant
                                        && shot.entityTags().contains("on_fire")==ignited
                                        && GenieCombat.spellWeapon(shot,owner).is(Items.AMETHYST_SHARD)
                                        && Math.abs(shot.getDeltaMovement().length()-(variant==2?3:.2))<1e-6),
                        "each native pattern has its original variant, speed, actor count and ignition");
                if(variant==2) require(shots.stream().allMatch(shot -> {
                    Vec3 aim=shot.getDeltaMovement();
                    return Math.abs(Math.toDegrees(Math.atan2(aim.z,aim.x)))<=30.02
                            && Math.abs(Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance())))<=20.02;
                }),"Gatling spreads around the requested aim on both axes");
                if(variant==1) {
                    clouds.tick();
                    if(ignited) require(victim.getHealth()==85
                                    && ledger.enchantmentWeapon(victim.getLastDamageSource()).is(Items.AMETHYST_SHARD),
                            "native burning caustic cloud hits using the captured Genie item");
                    else {
                        var poison=victim.getEffect(MobEffects.POISON);
                        var cause=ledger.effectCauses(victim).get(MobEffects.POISON);
                        require(poison!=null && poison.getDuration()==2400 && poison.getAmplifier()==4
                                        && cause!=null && cause.enchantmentWeapon().is(Items.AMETHYST_SHARD),
                                "native caustic cloud preserves the delayed poison's original weapon");
                    }
                    victim.removeEffect(MobEffects.POISON);
                    victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
                }
                shots.forEach(base::discard);
            }

            base.set(cell.east(2),Blocks.STONE.defaultBlockState());
            require(directed(host,"awakened_acid_wand",origin,east),"directed reactive cloud shot");
            var great=clouds.acidSnapshot().getFirst();
            for(int i=0;i<16&&!great.isRemoved();i++) great.tick();
            var children=clouds.acidSnapshot().stream().filter(child->!child.isRemoved()).toList();
            require(great.isRemoved() && children.size()==40
                            && children.stream().allMatch(child ->
                                    GenieCombat.spellWeapon(child,owner).is(Items.AMETHYST_SHARD)),
                    "the real wall impact releases forty live acid children with the input weapon");
            clouds.acidSnapshot().forEach(base::discard);
            base.set(cell.east(2),Blocks.AIR.defaultBlockState());

            require(directed(host,"awakened_acid_rifle",origin,east)
                            && directed(host,"awakened_acid_rifle_ignited",origin,east)
                            && clouds.acidSnapshot().isEmpty(),
                    "two directed triggers queue finite native rifle bursts");
            base.pause();
            try {
                for(int i=0;i<10;i++) service.tick();
                require(clouds.acidSnapshot().isEmpty(),"time stop pauses queued acid rifle");
            } finally {base.thaw();}
            base.field(false,origin);
            try {
                for(int i=0;i<10;i++) service.tick();
                require(clouds.acidSnapshot().isEmpty(),"antimagic at the retained origin pauses queued releases");
            } finally {base.removeFields();}
            for(int tick=1;tick<=110;tick++) {
                service.tick();
                var emitted=clouds.acidSnapshot();
                if(tick%5==0 && tick<=100) require(emitted.size()==tick/5,
                        "borrowed native rifle releases exactly one projectile per five ticks");
                if(tick%5==0 && tick<=100) {
                    boolean ignited=tick>50;
                    require(emitted.stream().filter(shot->shot.entityTags().contains("on_fire")).count()
                                    ==(ignited?(tick-50)/5:0),
                            "queued casts retain independent ignition in FIFO order");
                }
            }
            require(clouds.acidSnapshot().size()==20 && clouds.acidSnapshot().stream().allMatch(shot ->
                            shot.position().equals(origin) && shot.getDeltaMovement().equals(east.scale(3))
                                    && GenieCombat.spellWeapon(shot,owner).is(Items.AMETHYST_SHARD)),
                    "the two finite bursts retain the supplied pose and captured weapon");
            require(service.pending(owner)==paid && game.items().mana().get(owner)==mana
                            && owner.getMainHandItem()==main && owner.getOffhandItem()==off,
                    "borrowed acid modes leave paid charges, mana and equipped items untouched");
        } finally {
            service.cancelDirectedRifles();
            clouds.acidSnapshot().forEach(base::discard);
            ledger.clear(victim);
            base.discard(victim);
        }
    }

    private static void drainAdvancedRay(LbcExasperato rays) {
        for (int tick = 0; tick < 100 && rays.pendingSearches() > 0; tick++) rays.tick();
        require(rays.pendingSearches() == 0, "native snow or Ender ray resolves");
    }

    private static void playerParameters(GameController game, WoolBucketChecks.Fixture base, GenieWorldHost host) {
        var player = base.owner;
        var mana = game.items().mana();
        var playerId = new JsonPrimitive(player.getUUID().toString());
        var get = new JsonPrimitive("mana");
        var set = new JsonPrimitive("set");
        var add = new JsonPrimitive("add");
        var power = new JsonPrimitive("minecraft:movement_speed");
        var movement = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        int oldMana = mana.get(player), oldMax = mana.maximum(player), oldRegen = mana.baseRegen(player);
        int oldCharges = game.items().terra().charges(player), oldLives = game.lives(player.getUUID());
        int oldTimer = mana.hudValue(player, "effect_add_mana");
        double oldMovement = movement.getBaseValue();
        var charges = new JsonPrimitive("swordcharged2");
        var lives = new JsonPrimitive("lives");
        var timer = new JsonPrimitive("effect_add_mana");
        var unknown = new JsonPrimitive("genie_no_such_counter");
        try {
            require(complete(host, queued(host, "lbc.change_counter", playerId, get, set, new JsonPrimitive(-180))).getAsInt() == -180,
                    "signed mana uses the native resource");
            require(complete(host, queued(host, "lbc.change_counter", playerId, get, add, new JsonPrimitive(75))).getAsInt() == -105,
                    "native mana addition preserves overdraft");
            complete(host, queued(host, "lbc.change_counter", playerId, new JsonPrimitive("manamax"), set, new JsonPrimitive(7200)));
            complete(host, queued(host, "lbc.change_counter", playerId, new JsonPrimitive("manaregen"), set, new JsonPrimitive(21)));
            var reading = complete(host, queued(host, "lbc.mana", playerId)).getAsJsonObject();
            require(reading.get("current").getAsInt() == -105 && reading.get("maximum").getAsInt() == 7200
                            && reading.get("base_regen_per_tick").getAsInt() == 21
                            && reading.has("effective_regen_per_tick") && reading.has("wand_overdraft"),
                    "runtime mana read returns live current, max, base regeneration and Overload");
            require(complete(host, queued(host, "lbc.change_counter", playerId, charges, set, new JsonPrimitive(4))).getAsInt() == 4
                            && game.items().terra().charges(player) == 4
                            && complete(host, queued(host, "lbc.counter", playerId, charges)).getAsInt() == 4,
                    "an emulated legacy score writes to the real Terra sword state");
            require(complete(host, queued(host, "lbc.change_counter", playerId, lives, add, new JsonPrimitive(1))).getAsInt() == oldLives + 1
                            && game.lives(player.getUUID()) == oldLives + 1,
                    "lives use the match participant instead of a parallel score");
            require(complete(host, queued(host, "lbc.change_counter", playerId, timer, set, new JsonPrimitive(1))).getAsInt() == 1,
                    "a dormant mana potion counter can be started");
            mana.tickPotions(player);
            require(mana.get(player) == 9895 && mana.hudValue(player, "effect_add_mana") == 0,
                    "native potion scheduler consumes the registered timer");
            require(complete(host, queued(host, "lbc.change_counter", playerId, unknown, set, new JsonPrimitive(1))).isJsonNull()
                            && game.server().getScoreboard().getObjective(unknown.getAsString()) == null,
                    "a typo does not generate a silent dummy objective");
            require(complete(host, queued(host, "lbc.change_counter", playerId, new JsonPrimitive("manaregen_visual"), set, new JsonPrimitive(1))).isJsonNull(),
                    "computed regeneration remains owned by the mana engine");
            var changed = complete(host, queued(host, "entity.change_attribute", playerId, power, add, new JsonPrimitive(.05))).getAsJsonObject();
            require(changed.get("base").getAsDouble() == oldMovement + .05 && movement.getBaseValue() == oldMovement + .05
                            && changed.get("value").getAsDouble() == movement.getValue(),
                    "another attribute registry entry shares native base and effective values");
            require(complete(host, queued(host, "entity.attribute", playerId, power)).equals(changed),
                    "attribute read reports the same live result");
        } finally {
            mana.set(player, oldMana);
            mana.maximum(player, oldMax);
            mana.baseRegen(player, oldRegen);
            mana.setPotionCounter(player, timer.getAsString(), oldTimer);
            new LbcSourceCounters(game).write(player, charges.getAsString(), oldCharges);
            game.changeRemainingLives(player, oldLives);
            movement.setBaseValue(oldMovement);
        }
    }

    private static void shockCounter(GameController game, WoolBucketChecks.Fixture base, GenieWorldHost host) {
        var target = base.other;
        var electro = game.items().electro();
        var mana = game.items().mana();
        var profile = game.players().get(target.getUUID());
        int previousSkill = profile.lbcSkill;
        boolean previousSkills = game.config().lbcSkills, previousRanked = game.config().ranked;
        var uuid = new JsonPrimitive(target.getUUID().toString());
        var shock = new JsonPrimitive("shock");
        var set = new JsonPrimitive("set");
        var progress = target.getAdvancements();
        var advancement = Objects.requireNonNull(game.server().getAdvancements()
                .get(net.minecraft.resources.Identifier.parse("lbc:true_advancements/potions/effect_shock")));
        var originalCriteria = new HashSet<String>();
        progress.getOrStartProgress(advancement).getCompletedCriteria().forEach(originalCriteria::add);
        var originalBolts = Set.copyOf(electro.transientSnapshot());
        var targetPosition = target.position();
        java.util.function.Supplier<List<LightningBolt>> newBolts = () -> electro.transientSnapshot().stream()
                .filter(bolt -> !originalBolts.contains(bolt) && bolt.level() == target.level()
                        && bolt.position().equals(targetPosition)).toList();
        require(electro.shocks(target) == 0 && mana.hudValue(target, "shock") == 0, "clean native shock target");
        try {
            require(complete(host, queued(host, "lbc.change_counter", uuid, shock, set, new JsonPrimitive(2))).getAsInt() == 2
                            && electro.shocks(target) == 2 && mana.hudValue(target, "shock") == 2
                            && newBolts.get().isEmpty(), "setting a dormant shock creates exactly two delayed native pulses");
            for (int tick = 0; tick < 20; tick++) electro.tick();
            try (var otherHost = new GenieWorldHost(game, target, new ItemStack(Items.STICK), () -> true, () -> 123L)) {
                require(complete(otherHost, queued(otherHost, "lbc.change_counter", uuid, shock,
                        new JsonPrimitive("add"), new JsonPrimitive(1))).getAsInt() == 3,
                        "another script can increment the existing native shock");
            }
            for (int tick = 20; tick < 39; tick++) electro.tick();
            require(newBolts.get().isEmpty()
                            && complete(host, queued(host, "lbc.counter", uuid, shock)).getAsInt() == 3,
                    "unchanged native delay: no lightning within the first 39 ticks");
            electro.tick();
            var bolts = newBolts.get();
            require(bolts.size() == 1 && bolts.getFirst().getCause() == base.owner
                            && electro.shocks(target) == 2 && mana.hudValue(target, "shock") == 2,
                    "tick 40 publishes one actual bolt with the initial script author and updated pulse count");
            require(complete(host, queued(host, "lbc.change_counter", uuid, shock, set, new JsonPrimitive(0))).getAsInt() == 0
                            && electro.shocks(target) == 0 && mana.hudValue(target, "shock") == 0,
                    "zero clears the scheduled native shock and its scoreboard projection");

            game.config().lbcSkills = true;
            game.config().ranked = true;
            profile.lbcSkill = 19;
            require(LbcParasite.active(target), "native Parasite immunity is active on the control target");
            require(complete(host, queued(host, "lbc.change_counter", uuid, shock, set, new JsonPrimitive(2))).getAsInt() == 0
                            && electro.shocks(target) == 0 && mana.hudValue(target, "shock") == 0,
                    "native shock immunity rejects script application without a ghost score");
        } finally {
            game.config().lbcSkills = previousSkills;
            game.config().ranked = previousRanked;
            profile.lbcSkill = previousSkill;
            electro.clearEffects(target);
            for (var bolt : newBolts.get()) base.discard(bolt);
            for (var criterion : advancement.value().criteria().keySet())
                if (!originalCriteria.contains(criterion)) progress.revoke(advancement, criterion);
        }
    }

    private static boolean applyStatus(GenieWorldHost host, Entity entity, String id, int ticks) {
        return complete(host, queued(host, "lbc.apply_status", new JsonPrimitive(entity.getUUID().toString()),
                new JsonPrimitive(id), new JsonPrimitive(ticks))).getAsBoolean();
    }

    private static void customStatuses(GameController game, WoolBucketChecks.Fixture base, GenieWorldHost host) {
        var items = game.items();
        var target = base.other;
        var stasis = items.legacyEffects().personalStasis();
        var originalSlot = target.getInventory().getItem(9);
        int manaTimer = items.mana().hudValue(target, "effect_add_mana");
        var cow = stationaryCow(base, base.home);
        try {
            require(!applyStatus(host, cow, "stun", 37) && items.shooters().stunTicks(cow) == 0,
                    "player status rejects a local mob without creating native state");
            require(!applyStatus(host, target, "test_unknown_genie_status", 40)
                            && game.server().getScoreboard().getObjective("test_unknown_genie_status") == null,
                    "unknown status cannot create a score objective");
            require(applyStatus(host, target, "stun", 37)
                            && items.shooters().stunTicks(target) == 37 && items.actionsBlocked(target),
                    "native stun installs the real input lock on a second player for exactly 37 ticks");
            items.shooters().clearStun(target);
            require(!items.actionsBlocked(target), "clearing the native stun removes the real input lock");

            require(applyStatus(host, target, "cold", 21) && items.frost().cold(target) == 2
                            && items.mana().hudValue(target, "cold") == 2,
                    "command status cadence rounds 21 ticks to two native Frost units and adopts the status");
            items.frost().clear(target);

            require(applyStatus(host, target, "shock", 41) && items.electro().shocks(target) == 2
                            && items.mana().hudValue(target, "shock") == 2,
                    "41 ticks install two real delayed pulses through the authored Shock controller");
            items.electro().clearEffects(target);

            require(applyStatus(host, target, "polymorph", 29) && items.polymorph().remaining(target) == 29,
                    "polymorph installs the real finite form on a second player");
            var sheep = items.polymorph().sheepViews().stream()
                    .filter(view -> base.owner.getUUID().equals(view.owner()))
                    .map(view -> base.level.getEntity(view.entity())).filter(Objects::nonNull).toList();
            require(sheep.size() == 1 && items.polymorph().body(sheep.getFirst()),
                    "native sheep is actually visible and retains the item player's source");
            items.polymorph().clearDebuffs(target);
            require(sheep.getFirst().isRemoved() && !items.polymorph().masked(target),
                    "native polymorph cleanup restores the actual player and removes his body");

            base.pause();
            try {
                require(!applyStatus(host, target, "personal_stasis", 31) && !stasis.active(target),
                        "world Time Stop rejects application before a stasis attachment can be installed");
            } finally {
                base.thaw();
            }
            require(applyStatus(host, target, "personal_stasis", 31) && stasis.remaining(target) == 31
                            && target.entityTags().contains(LbcPersonalStasis.TAG),
                    "positive stasis enters the real persisted timeline and motion owner");
            stasis.clear(target);

            target.getInventory().setItem(9, base.leaf("mystical_artifact_clear"));
            items.magic().inventoryChanged(target);
            require(items.magic().rejectCustomEffect(target), "real cleansing artifact establishes immunity");
            require(!applyStatus(host, target, "personal_stasis", 31) && !stasis.active(target)
                            && !applyStatus(host, target, "cold", 21) && items.frost().cold(target) == 0
                            && items.mana().hudValue(target, "cold") == 0
                            && !applyStatus(host, target, "effect_add_mana", 21)
                            && items.mana().hudValue(target, "effect_add_mana") == manaTimer,
                    "harmful and useful grants respect actual immunity without adding orphan native scores");
        } finally {
            target.getInventory().setItem(9, originalSlot);
            items.magic().inventoryChanged(target);
            items.polymorph().clearDebuffs(target);
            stasis.clear(target);
            items.frost().clear(target);
            items.shooters().clearStun(target);
            items.electro().clearEffects(target);
            base.discard(cow);
        }
    }

    static final class RocketCounterProbe implements AutoCloseable {
        private static final JsonPrimitive JUMPS = new JsonPrimitive("rocket_jumps");
        private static final JsonPrimitive COOLDOWN = new JsonPrimitive("rocket_cooldown_ticks");
        private static final JsonPrimitive SET = new JsonPrimitive("set");
        private final GameController game;
        private final net.minecraft.server.level.ServerPlayer owner;
        private final ItemStack previousBoots;
        private final Vec3 previousMotion;
        private final boolean wasOnGround;
        private final int initialTick;

        RocketCounterProbe(GameController game, WoolBucketChecks.Fixture base) {
            this.game = game;
            owner = base.owner;
            previousBoots = owner.getItemBySlot(EquipmentSlot.FEET).copy();
            previousMotion = owner.getDeltaMovement();
            wasOnGround = owner.onGround();
            initialTick = game.server().getTickCount();
            require(!LbcItems.isRocketBoots(previousBoots) && game.items().rocketJumps(owner) == 0,
                    "no existing rocket state before the counter witness");
            var uuid = new JsonPrimitive(owner.getUUID().toString());
            try (var host = new GenieWorldHost(game, owner, new ItemStack(Items.STICK), () -> true, () -> 123L)) {
                require(complete(host, queued(host, "lbc.change_counter", uuid, JUMPS, SET, new JsonPrimitive(2))).isJsonNull()
                                && game.server().getScoreboard().getObjective("rocket_jumps") == null,
                        "no boots means no write and no shadow score");
                owner.setItemSlot(EquipmentSlot.FEET, new ItemStack(LbcItems.ROCKET_BOOTS));
                require(complete(host, queued(host, "lbc.counter", uuid, JUMPS)).getAsInt() == 3,
                        "equipping the real boots exposes their native charge count");
                var context = GeniePlayerParameters.wishContext(game, owner);
                require(context.getAsJsonArray("mutable_player_counter_ids").contains(COOLDOWN)
                                && context.getAsJsonObject("rocket_boots").get("rocket_jumps").getAsInt() == 3,
                        "provider sees both native counter IDs and the requesting player's actual charges");
                require(complete(host, queued(host, "lbc.change_counter", uuid, JUMPS, SET, new JsonPrimitive(4))).getAsInt() == 4
                                && complete(host, queued(host, "lbc.change_counter", uuid, COOLDOWN, SET,
                                    new JsonPrimitive(Integer.MAX_VALUE))).getAsInt() == Integer.MAX_VALUE,
                        "ordinary charge and recharge values are not script caps and deadlines do not overflow");
                require(complete(host, queued(host, "lbc.change_counter", uuid, JUMPS, SET, new JsonPrimitive(0))).getAsInt() == 0,
                        "script empties the real boots");
                owner.setOnGround(false);
                require(!game.items().rocketJumpPressed(owner), "an empty counter genuinely prevents the rocket boost");
                require(complete(host, queued(host, "lbc.change_counter", uuid, JUMPS, new JsonPrimitive("add"), new JsonPrimitive(1))).getAsInt() == 1
                                && game.items().rocketJumpPressed(owner) && game.items().rocketJumps(owner) == 0
                                && !owner.getDeltaMovement().equals(previousMotion),
                        "a script charge delivers and then pays for one actual rocket jump");
                owner.setDeltaMovement(previousMotion);
                require(complete(host, queued(host, "lbc.change_counter", uuid, COOLDOWN, SET, new JsonPrimitive(1))).getAsInt() == 1,
                        "script replaces the live recharge deadline");
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }

        boolean tick() {
            int elapsed = game.server().getTickCount() - initialTick;
            if (elapsed < 2) {
                require(game.items().rocketJumps(owner) == 0, "the native charger respects the shortened deadline before it is due");
                return false;
            }
            require(elapsed == 2 && game.items().rocketJumps(owner) == 1
                            && game.items().rocketCooldownTicks(owner) == 300,
                    "the real server tick recharges once on schedule and rearms the ordinary 300-tick delay");
            return true;
        }

        @Override public void close() {
            owner.setItemSlot(EquipmentSlot.FEET, previousBoots);
            owner.setDeltaMovement(previousMotion);
            owner.setOnGround(wasOnGround);
        }
    }

    private static void sharedNativeAbilities(GameController game, WoolBucketChecks.Fixture base) {
        var runtime = game.items().tmt();
        var nativeAbilities = runtime.genieAbilities();
        var index = JsonParser.parseString(nativeAbilities.index()).getAsJsonArray();
        var catalog = runtime.catalog();
        var icon = net.minecraft.resources.Identifier.parse("minecraft:blaze_rod");
        var keys = new HashSet<String>();
        String stoneVariant = null;
        for (var entry : index) {
            var row = entry.getAsJsonObject();
            String family = row.get("id").getAsString();
            var events = row.getAsJsonObject("default_bindings");
            var carrier = events.has("SHOT") ? Items.BOW : events.has("CONSUME") ? Items.GOLDEN_APPLE
                    : row.getAsJsonArray("locations").contains(new JsonPrimitive("EQUIPPED")) ? Items.IRON_CHESTPLATE
                    : Items.CARROT_ON_A_STICK;
            for (var choice : row.getAsJsonArray("variants")) {
                var variant = choice.getAsJsonObject();
                String key = variant.get("variant").getAsString();
                require(keys.add(key), "one published ID per TMT variant");
                var gift = new ItemStack(carrier);
                var data = new CompoundTag();
                data.putString("genie_code", "check");
                gift.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(data));
                var attached = nativeAbilities.attach(gift, icon, List.of(new GenieScriptReply.NativeAbility(key, Map.of())));
                var properties = LbcTmtAbilities.read(attached);
                require(properties.size() == 1 && properties.getFirst().id().equals(family)
                                && LbcTmtTriggers.modern(attached) && ItemData.contains(attached, "genie_code")
                                && icon.equals(attached.get(net.minecraft.core.component.DataComponents.ITEM_MODEL)),
                        "shared catalog variant becomes a native property on the original gift: " + family);
                if (variant.has("source_item") && variant.get("source_item").getAsString().equals("skywars2:stone_wand")
                        && family.equals("wand/ststone")) stoneVariant = key;
            }
        }
        require(index.size() == catalog.families().size() && keys.size() == catalog.domains().variants(),
                "the bridge includes every live TMT family and variant");
        require(stoneVariant != null, "native ordinary Stone wand present");
        String selected = stoneVariant;
        rejected(() -> nativeAbilities.attach(new ItemStack(Items.STICK), icon,
                List.of(new GenieScriptReply.NativeAbility("unknown", Map.of()))), "unknown variants must fail");
        rejected(() -> nativeAbilities.attach(new ItemStack(Items.STICK), icon,
                List.of(new GenieScriptReply.NativeAbility(selected, Map.of("USE", "SHOT")))), "incompatible carrier must fail");
        var gift = nativeAbilities.attach(new ItemStack(Items.STICK), icon,
                List.of(new GenieScriptReply.NativeAbility(selected, Map.of("USE", "LEFT_CLICK"))));
        var owner = base.owner;
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var mana = game.items().mana();
        int before = mana.get(owner);
        int price = mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("ststone_wand_mana_use"), "lbc.math").get();
        var stone = game.items().stone();
        require(stone.count() == 0 && price > 0, "isolated native paid Stone test");
        try {
            mana.set(owner, price + 10);
            owner.setItemInHand(InteractionHand.MAIN_HAND, gift);
            owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            LbcTmtTriggers.input(owner, LbcTmtTriggers.Input.RIGHT_CLICK);
            require(stone.count() == 0 && mana.get(owner) == price + 10,
                    "original trigger does not replay a rebound native property");
            LbcTmtTriggers.input(owner, LbcTmtTriggers.Input.LEFT_CLICK);
            require(stone.count() == 1 && stone.snapshot().getFirst().getOwner() == owner && mana.get(owner) == 10
                            && owner.getMainHandItem() == gift,
                    "the shared handler makes one real rock, pays once, and uses the physical gift");
            LbcTmtTriggers.input(owner, LbcTmtTriggers.Input.LEFT_CLICK);
            require(stone.count() == 1 && mana.get(owner) == 10, "same-tick duplicated input never replays the shared ability");
        } finally {
            stone.stop();
            owner.setItemInHand(InteractionHand.MAIN_HAND, main);
            owner.setItemInHand(InteractionHand.OFF_HAND, off);
            mana.set(owner, before);
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] Genie shared native abilities families={} variants={}", index.size(), keys.size());
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
            playerParameters(game,base,host);
            shockCounter(game,base,host);
            customStatuses(game,base,host);
            sharedNativeAbilities(game,base);

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

            ceilingCasts(game,base,host,BlockPos.containing(launchSpot));
            directedCasts(game, base, launchSpot.add(0, .65, 0));
            targetedOreshnik(game, base, launchSpot);
            targetedExoperso(game, base, launchSpot);
            polymorphCasts(game, base, launchSpot);
            prismCasts(game, base, launchSpot.add(0, .65, 0));
            starsAndWall(game, base);
            advancedSnowCasts(game, base);
            permafrostCasts(game, base);
            fartusCasts(game, base);
            awakenedAcidCasts(game, base);
            musicStaffCasts(game, base);
            terrainStaffCasts(game, base);
            voidArcanumCasts(game, base);
            fireblazeCasts(game, base);
            psychoAreaCasts(game, base, launchSpot);
            targetedHedgehogs(game, base, launchSpot);
            danmakuCasts(game, base, launchSpot);
            danmakuVolleys(game, base);
            sauvojenCasts(game, base, launchSpot.add(0, .65, 0));
            nativeSummons(game, base, launchSpot);
            distinctNativeSummons(game, base);

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
