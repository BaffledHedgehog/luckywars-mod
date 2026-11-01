package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.RandomCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/** Real native grids, placed containers and scheduled tracked-ground crafting; no callback simulation. */
public final class InfiniteShulkerChecks {
    private InfiniteShulkerChecks() {}
    private static Fixture active;
    @SuppressWarnings("unchecked") private static List<net.minecraft.network.protocol.Packet<?>> packets(RegenerationBeltChecks.ScheduledClient client){
        try{Object connection=client.connection;var f=connection.getClass().getDeclaredField("packets");f.setAccessible(true);return (List<net.minecraft.network.protocol.Packet<?>>)f.get(connection);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }

    private static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException("Infinite shulker: " + why);
    }
    private static ResourceKey<LootTable> key(String id) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id));
    }
    private static boolean shell(ItemStack stack, String flag, String table) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        var loot = stack.get(DataComponents.CONTAINER_LOOT);
        return !stack.isEmpty() && data != null && data.copyTag().getIntOr(flag, 0) == 1
                && loot != null && loot.lootTable().equals(key(table));
    }
    private static long[] tape(RandomSource random) {
        var state = MatchRandomState.capture(random);
        try { long[] values = new long[8]; for (int i = 0; i < values.length; i++) values[i] = random.nextLong(); return values; }
        finally { state.restore(random); }
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerPlayer p;
        final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved;
        final SauvojenMeditationChecks.Attribution attribution;
        RegenerationBeltChecks.ScheduledClient client;
        final Set<ItemEntity> owned = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos, MapManager.BlockSnapshot> blocks = new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>, Integer> stats = new LinkedHashMap<>();
        final Vec3 at, ground;
        final BlockPos table, box;
        ItemStack base, upgraded, lucky, wand;
        int phase, started, when, updates;
        ItemEntity first, second;
        boolean ownsStop, closed;

        Fixture(GameController game) {
            this.game = game;
            p = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(p != null && game.playing() && game.canUseGameplayItems(p)
                    && p.containerMenu == p.inventoryMenu && game.server().tickRateManager().runsNormally(), "active idle native participant");
            level = p.level();
            require(level == game.maps().level() && !game.items().timeStop().stopped(level)
                    && !game.items().wands().timeStopped(), "ordinary match and no borrowed world stop");
            saved = new RegenerationBeltChecks.Snapshot(game, p);
            attribution = new SauvojenMeditationChecks.Attribution(game, List.of(p));
            at = new Vec3(p.getX(), Math.min(240, level.getMaxY() - 40), p.getZ());
            ground = at.add(5, 0, 0);
            table = BlockPos.containing(at.add(0, 0, 2));
            box = BlockPos.containing(at.add(2, 0, 2));
            for (var pos : BlockPos.betweenClosed(BlockPos.containing(at.add(-3, -2, -3)), BlockPos.containing(at.add(9, 3, 4))))
                require(level.hasChunkAt(pos) && level.getBlockState(pos).isAir(), "resident exclusive high-altitude air volume");
            require(level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at.add(-4,-4,-4),at.add(10,4,5))).isEmpty(), "no borrowed item entities in fixture volume");
            for (var pos : List.of(table, box)) blocks.put(pos, game.maps().snapshot(level, pos));
            try {
            level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
            for (var item : List.of(Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.BLACK), Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.YELLOW)))
                for (var type : List.of(Stats.ITEM_CRAFTED, Stats.ITEM_PICKED_UP)) {var stat = type.get(item); stats.put(stat, p.getStats().getValue(stat));}
            p.stopUsingItem(); p.getInventory().clearContent(); p.inventoryMenu.getCraftSlots().clearContent();
            p.containerMenu.setCarried(ItemStack.EMPTY); p.removeAllEffects(); p.setGameMode(GameType.SURVIVAL);
            p.setNoGravity(true); p.noPhysics = true; p.setDeltaMovement(Vec3.ZERO); p.setKnownMovement(Vec3.ZERO);
            p.setSprinting(false); p.setShiftKeyDown(false); p.getInventory().setSelectedSlot(0);
            p.setLastClientInput(new net.minecraft.world.entity.player.Input(false,false,false,false,false,false,false));
            p.entityTags().removeAll(List.of("time_stopped", "spectator", "strict_map_object", "nomagic_active"));
            var profile = game.players().get(p.getUUID()); profile.lbcKit = 0; profile.lbcChallengeKit = 0;
            profile.lbcSkill = 0; profile.lbcChallenge = 0; profile.lbcTrinket = 0;
            Arrays.fill(profile.levelups, 0); Arrays.fill(profile.trinkets, 0);
            game.items().magic().inventoryChanged(p); game.items().magic().flushCleansing();
            p.teleportTo(level, at.x, at.y, at.z, Set.of(), 0, 0, true);
            client = new RegenerationBeltChecks.ScheduledClient(game, p);
            var baseRoll = game.lbc().rollTable(level, at, p, "lbc:infinity");
            base = baseRoll.stream().filter(s -> shell(s,"inf_shulker","lbc:infinity")).findFirst().orElseThrow().copy();
            upgraded = single("lbc:infinity_upgraded_only_shulker");
            lucky = single("lbc:infinity_upgraded_only_shulker_luck");
            wand = single("lbc:infinite_lucky_block");
            require(base.getCount() == 1 && shell(upgraded,"inf_inf_shulker","lbc:infinity_upgraded")
                    && shell(lucky,"sigma_shulker","lbc:infinity_upgraded_luck"), "actual source shell leaves");
            } catch (Throwable error) {try {close();} catch (Throwable cleanup) {error.addSuppressed(cleanup);} throw error;}
        }

        ItemStack single(String id) {
            var rolls = game.lbc().rollTable(level, at, p, id);
            require(rolls.size() == 1 && rolls.getFirst().getCount() == 1, "one original source leaf " + id);
            return rolls.getFirst().copy();
        }
        void clearMenu() {
            if (p.containerMenu instanceof AbstractCraftingMenu menu) {
                menu.setCarried(ItemStack.EMPTY); menu.getInputGridSlots().forEach(slot -> slot.setByPlayer(ItemStack.EMPTY));
            }
            p.closeContainer(); p.getInventory().clearContent(); p.inventoryMenu.getCraftSlots().clearContent();
        }
        AbstractCraftingMenu menu(boolean bench) {
            clearMenu();
            if (bench) p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, player) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(level, table)), Component.literal("Infinite shulker native fixture")));
            return (AbstractCraftingMenu) p.containerMenu;
        }
        void grids() {
            for (boolean bench : new boolean[]{false,true}) for (boolean sigma : new boolean[]{false,true}) {
                var expected = sigma ? lucky : upgraded;
                for (boolean shift : new boolean[]{false,true}) {
                    var menu = menu(bench); var slots = menu.getInputGridSlots();
                    var a = (sigma ? upgraded : base).copyWithCount(shift ? 1 : 2);
                    var b = (sigma ? wand : base).copyWithCount(shift ? 1 : 2);
                    a.set(DataComponents.CUSTOM_NAME, Component.literal("first exact donor"));
                    b.set(DataComponents.CUSTOM_NAME, Component.literal("second exact donor"));
                    slots.getFirst().setByPlayer(a); slots.getLast().setByPlayer(b);
                    var preview = menu.getResultSlot().getItem().copy();
                    require(ItemStack.matches(preview, expected), "actual " + (bench?"3x3":"2x2") + " output components sigma=" + sigma);
                    var oldA = a.copy(); var oldB = b.copy();
                    var sequenceMap = ((RandomCheckpointAccess.Sequences)game.server().getRandomSequences()).skywars2$sequences();
                    var keysBefore = Set.copyOf(sequenceMap.keySet());
                    var rng = new IdentityHashMap<RandomSource,long[]>(); rng.put(level.getRandom(), tape(level.getRandom())); rng.put(p.getRandom(), tape(p.getRandom()));
                    for (var id : keysBefore) {var random = game.server().getRandomSequence(id); rng.put(random,tape(random));}
                    packets(client).clear();
                    for (int i = 0; i < 8; i++) {menu.slotsChanged(p.getInventory()); require(ItemStack.matches(menu.getResultSlot().getItem(), preview),"stable repeated native preview");}
                    require(ItemStack.matches(slots.getFirst().getItem(),oldA) && ItemStack.matches(slots.getLast().getItem(),oldB),"preview preserves every input component/count");
                    require(keysBefore.equals(sequenceMap.keySet()), "preview creates no random sequence");
                    rng.forEach((random, values) -> require(Arrays.equals(values,tape(random)),"preview preserves native RNG tape"));
                    require(packets(client).stream().noneMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket
                            || packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket),"preview has no sound/particles");
                    if (shift) {
                        for (int i=0;i<36;i++) p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
                        GameChecks.inventoryClick(p,0,0,ContainerInput.QUICK_MOVE);
                        require(slots.getFirst().getItem().getCount()==1 && slots.getLast().getItem().getCount()==1
                                && ItemStack.matches(menu.getResultSlot().getItem(),expected),"full inventory shift leaves donors and preview intact");
                        p.getInventory().setItem(9,ItemStack.EMPTY);
                    }
                    int before = p.getStats().getValue(Stats.ITEM_CRAFTED.get(expected.getItem()));
                    GameChecks.inventoryClick(p,0,0,shift ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
                    if (shift) {
                        int count=0; for(int i=0;i<36;i++) {var stack=p.getInventory().getItem(i); if(ItemStack.isSameItemSameComponents(stack,expected))count+=stack.getCount();}
                        require(count==1 && menu.getCarried().isEmpty() && slots.getFirst().getItem().isEmpty() && slots.getLast().getItem().isEmpty(),"native shift moves one exact result and debits both final units");
                    } else {
                        require(ItemStack.matches(menu.getCarried(),expected) && ItemStack.matches(slots.getFirst().getItem(),oldA.copyWithCount(1))
                                && ItemStack.matches(slots.getLast().getItem(),oldB.copyWithCount(1)),"native output take debits exactly one per donor, preserves remaining components");
                    }
                    require(p.getStats().getValue(Stats.ITEM_CRAFTED.get(expected.getItem()))==before+1,"native crafted statistic once per actual output");
                    var particles=packets(client).stream().filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance)
                            .map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast).toList();
                    var sounds=packets(client).stream().filter(net.minecraft.network.protocol.game.ClientboundSoundPacket.class::isInstance)
                            .map(net.minecraft.network.protocol.game.ClientboundSoundPacket.class::cast).toList();
                    require(particles.size()==1 && sounds.size()==1,"one actual take emits source feedback exactly once");
                    var particle=particles.getFirst();int color=sigma?net.minecraft.util.ARGB.colorFromFloat(1,1,.867f,.271f)&0xffffff:0;
                    require(particle.particle() instanceof net.minecraft.core.particles.DustParticleOptions dust
                            && dust.getScale()==2 && dust.getColor().equals(net.minecraft.util.ARGB.vector3fFromRGB24(color))
                            && particle.count()==100 && particle.overrideLimiter() && particle.y()==p.getY()+1
                            && particle.xDist()==1 && particle.yDist()==1 && particle.zDist()==1
                            && particle.xMaxSpeed()==1 && particle.yMaxSpeed()==1 && particle.zMaxSpeed()==1,"source black/gold force dust100/scale2/spread1/speed1");
                    var sound=sounds.getFirst();require(sound.getSound().value().location().toString().equals("minecraft:block.enchantment_table.use")
                            && sound.getVolume()==1 && sound.getPitch()==1 && sound.getSource()==net.minecraft.sounds.SoundSource.MASTER,"source enchantment-table master sound1/1");
                    clearMenu();
                }
                var menu=menu(bench); var slots=menu.getInputGridSlots();
                var opened=(sigma?upgraded:base).copy();opened.remove(DataComponents.CONTAINER_LOOT);
                var wrong=(sigma?upgraded:base).copy();wrong.set(DataComponents.CONTAINER_LOOT,new SeededContainerLoot(key("minecraft:chests/simple_dungeon"),0));
                for(var invalid:List.of(opened,wrong,new ItemStack(Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.BLACK)))) {
                    slots.getFirst().setByPlayer(invalid);slots.getLast().setByPlayer((sigma?wand:base).copy());
                    require(!ItemStack.isSameItemSameComponents(menu.getResultSlot().getItem(),expected),"opened/wrong table/unmarked vanilla donor rejected");
                }
                slots.getFirst().setByPlayer((sigma?upgraded:base).copy()); slots.get(1).setByPlayer(new ItemStack(Items.DIRT));
                require(menu.getResultSlot().getItem().isEmpty(),"third occupied native grid slot rejected"); clearMenu();
            }
            var extra=CraftingInput.of(2,2,List.of(base.copy(),base.copy(),base.copy(),ItemStack.EMPTY));
            require(LbcCraftingRecipe.craftingKind(extra)==null,"exact occupied-slot count");
        }

        List<ItemEntity> localOwned() {
            return level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(at.add(-3,-3,-3),at.add(9,3,4)),
                    entity -> !entity.isRemoved() && entity.getOwner()==p);
        }
        void capture() {
            for (var entity : localOwned()) if (owned.add(entity)) {
                // Stabilize only fixture-owned published bodies after observing their native creation.
                entity.setNoGravity(true); entity.noPhysics=true; entity.setDeltaMovement(Vec3.ZERO);
            }
        }
        void retire() {capture();for(var entity:List.copyOf(owned))entity.discard();owned.clear();}
        long shells(String flag, String table) {
            return localOwned().stream().filter(entity->shell(entity.getItem(),flag,table)).mapToLong(entity->entity.getItem().getCount()).sum();
        }
        void discardBox() {
            if(level.getBlockEntity(box) instanceof ShulkerBoxBlockEntity previous) {
                // Clear the deferred key first: clearing an unopened native container must not
                // itself unpack loot, trigger an advancement or introduce fixture cleanup drops.
                previous.setLootTable(null);previous.clearContent();
            }
            level.setBlock(box,Blocks.AIR.defaultBlockState(),2);
        }
        ShulkerBoxBlockEntity placed(String loot) {
            // A fresh block identity is needed even when both successive shells are yellow.
            discardBox();
            require(level.setBlock(box,Blocks.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.YELLOW).defaultBlockState(),2),"actual placed native shulker");
            var container=(ShulkerBoxBlockEntity)level.getBlockEntity(box);
            require(container!=null,"resident native shulker block entity");container.setLootTable(key(loot),174329L);return container;
        }
        List<ItemStack> contents(ShulkerBoxBlockEntity container) {
            var stacks=new ArrayList<ItemStack>();for(int i=0;i<container.getContainerSize();i++)if(!container.getItem(i).isEmpty())stacks.add(container.getItem(i).copy());return stacks;
        }
        String describe(List<ItemStack> stacks) {
            var rows=new ArrayList<String>();
            for(int i=0;i<stacks.size();i++) {
                var stack=stacks.get(i);
                rows.add(i+":"+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                        +" count="+stack.getCount()+" maxStack="+stack.getMaxStackSize()
                        +" maxDamage="+stack.get(DataComponents.MAX_DAMAGE));
            }
            return rows.toString();
        }
        void luckyContents(ShulkerBoxBlockEntity container, List<ItemStack> actual) {
            var source=game.server().reloadableRegistries().getLootTable(key("lbc:infinity_upgraded_luck"));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,Vec3.atCenterOf(box))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,p)
                    .withLuck(p.getLuck()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            // luckyblock has THREE source pools: the normal reward and two additional
            // conditional neko/loli pools (normally5% each), plus nested reward tables.
            // Thus26 root rolls can emit more than26 stacks. Native fill shuffles and
            // truncates overflow at27 slots; it splits stacks only when slots remain.
            // Both oracle calls use their own explicit local RNG seed, never a world clock.
            var raw=source.getRandomItems(params,174329L).stream().filter(stack->!stack.isEmpty()).toList();
            var expected=new net.minecraft.world.SimpleContainer(27);
            source.fill(expected,params,174329L);
            var filled=new ArrayList<ItemStack>();
            for(int i=0;i<expected.getContainerSize();i++)if(!expected.getItem(i).isEmpty())filled.add(expected.getItem(i).copy());
            boolean valid=!raw.isEmpty() && raw.stream().allMatch(stack->stack.getCount()==64
                    && stack.getMaxStackSize()==64 && !stack.has(DataComponents.MAX_DAMAGE))
                    && !actual.isEmpty() && actual.size()==filled.size()
                    && actual.stream().allMatch(stack->stack.getCount()>0 && stack.getCount()<=64
                    && stack.getMaxStackSize()==64 && !stack.has(DataComponents.MAX_DAMAGE));
            for(int i=0;i<27;i++)valid &= ItemStack.matches(container.getItem(i),expected.getItem(i));
            if(!valid)SkyWars2.LOGGER.error("[check] infinite-shulker lucky contents mismatch raw={} expectedFill={} actualFill={}",
                    describe(raw),describe(filled),describe(actual));
            require(valid,"actual native lucky contents match seeded source raw64/maxstack64/no durability and native27-slot overflow/splitting; raw="
                    +raw.size()+" filled="+actual.size()+" units="+actual.stream().mapToInt(ItemStack::getCount).sum());
            SkyWars2.LOGGER.info("[check] infinite-shulker lucky source rawStacks={} rawUnits={} filledStacks={} filledUnits={}",
                    raw.size(),raw.stream().mapToInt(ItemStack::getCount).sum(),actual.size(),actual.stream().mapToInt(ItemStack::getCount).sum());
        }
        void containers() {
            for(boolean upgrade:new boolean[]{false,true}) {
                String tableId=upgrade?"lbc:infinity_upgraded":"lbc:infinity";
                var container=placed(tableId);container.unpackLootTable(p);var contents=contents(container);
                require(container.getLootTable()==null && contents.size()==27,"native base/upgraded generates27 occupied source slots");
                require(contents.stream().filter(s->shell(s,upgrade?"inf_inf_shulker":"inf_shulker",tableId)).count()==1,"one unopened original replacement stays inside native base/upgraded contents");
                require(localOwned().isEmpty(),"base/upgraded create no separate lucky reward");
                if(upgrade)for(var stack:contents)if(!shell(stack,"inf_inf_shulker",tableId))
                    require(stack.getCount()==64 && stack.getMaxStackSize()==64 && !stack.has(DataComponents.MAX_DAMAGE),"upgraded26 rewards exact64/maxstack64/no durability");
            }
            for(int generation=1;generation<=2;generation++) {
                var container=placed("lbc:infinity_upgraded_luck");container.unpackLootTable(p);capture();
                var contents=contents(container);
                luckyContents(container,contents);
                require(contents.stream().noneMatch(s->shell(s,"sigma_shulker","lbc:infinity_upgraded_luck")),"lucky source has no embedded replacement");
                require(shells("sigma_shulker","lbc:infinity_upgraded_luck")==generation,"one separate physical lucky shell per actual native generation");
                for(var entity:localOwned()) {
                    require(ItemStack.matches(entity.getItem(),lucky) && entity.position().distanceToSqr(p.position())<1e-10
                            && game.items().currentEpoch(entity) && entity.hasPickUpDelay(),"source player-position exact shell/owner/epoch/normal pickup");
                }
                var before=contents(container);container.unpackLootTable(p);capture();
                require(shells("sigma_shulker","lbc:infinity_upgraded_luck")==generation,"reopening filled native container creates no reward");
                var after=contents(container);for(int i=0;i<before.size();i++)require(ItemStack.matches(before.get(i),after.get(i)),"repeat opening preserves filled contents");
            }
            var noPlayer=placed("lbc:infinity_upgraded_luck");noPlayer.unpackLootTable(null);capture();
            require(noPlayer.getLootTable()==null && shells("sigma_shulker","lbc:infinity_upgraded_luck")==2,"null-player generation has no separate reward");
            var unrelated=placed("minecraft:chests/simple_dungeon");unrelated.unpackLootTable(p);capture();
            require(shells("sigma_shulker","lbc:infinity_upgraded_luck")==2,"unrelated native container has no replacement");
            p.setGameMode(GameType.SPECTATOR);
            try {placed("lbc:infinity_upgraded_luck").unpackLootTable(p);capture();require(shells("sigma_shulker","lbc:infinity_upgraded_luck")==2,"spectator native generation cannot reward");}
            finally {p.setGameMode(GameType.SURVIVAL);}
            retire();discardBox();
        }

        ItemEntity donor(ItemStack source, String name, Vec3 point, int count) {
            var stack=source.copyWithCount(count);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));
            var entity=new ItemEntity(level,point.x,point.y,point.z,stack);entity.setNoGravity(true);entity.noPhysics=true;
            entity.setDeltaMovement(Vec3.ZERO);entity.setThrower(p);entity.setNeverPickUp();game.items().markCurrentItemEpoch(entity);
            owned.add(entity);require(level.addFreshEntity(entity),"actual native tracked ground donor");return entity;
        }
        void donors(boolean sigma) {
            retire();first=donor(sigma?upgraded:base,"first nonmerge donor",ground,2);
            second=donor(sigma?wand:base,"second nonmerge donor",ground.add(.25,0,0),1);
        }
        void begin() {
            grids();containers();donors(false);
            game.items().timeStop().begin(level,200);ownsStop=true;
            phase=0;started=when=game.server().getTickCount();updates=client.updates();
        }
        void next(int value) {phase=value;when=game.server().getTickCount();}
        boolean step() {
            client.acknowledge();capture();int now=game.server().getTickCount(),elapsed=now-when;
            require(now-started<180,"bounded real scheduled crafting fixture");
            require(client.updates()>updates || elapsed==0,"actual native client scheduler advances");
            switch(phase) {
                case 0 -> {
                    require(first.getItem().getCount()==2 && !second.isRemoved() && second.getItem().getCount()==1
                            && shells("inf_inf_shulker","lbc:infinity_upgraded")==0,"Time Stop pauses real tracked ground recipe and both debits");
                    if(elapsed<25)return false;game.items().timeStop().end(level);ownsStop=false;next(1);
                }
                case 1 -> {
                    long result=shells("inf_inf_shulker","lbc:infinity_upgraded");
                    if(result==0){require(elapsed<=20,"ground publication occurs within one actual20tick cadence");return false;}
                    require(result==1 && first.getItem().getCount()==1 && second.isRemoved(),"source two entities consume one unit each and publish exactly one upgraded shell");
                    next(2);
                }
                case 2 -> {
                    require(first.getItem().getCount()==1 && shells("inf_inf_shulker","lbc:infinity_upgraded")==1,"depleted pair cannot repeat across later actual cadence");
                    if(elapsed<40)return false;donors(true);next(3);
                }
                case 3 -> {
                    long result=shells("sigma_shulker","lbc:infinity_upgraded_luck");
                    if(result==0){require(elapsed<=20,"lucky ground publication within one actual20tick cadence");return false;}
                    require(result==1 && first.getItem().getCount()==1 && second.isRemoved(),"lucky ground exact upgraded+wand one-unit debits and one source shell");next(4);
                }
                case 4 -> {
                    require(first.getItem().getCount()==1 && shells("sigma_shulker","lbc:infinity_upgraded_luck")==1,"lucky depleted pair cannot repeat");
                    if(elapsed>=40)return true;
                }
                default -> throw new IllegalStateException("Infinite shulker phase");
            }
            return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;
            try {if(ownsStop)game.items().timeStop().end(level);retire();clearMenu();discardBox();blocks.forEach((pos,image)->game.maps().restoreSnapshot(level,pos,image));}
            finally {try {if(client!=null)client.close();saved.restore(game);saved.restoreAttributes();stats.forEach((stat,value)->p.getStats().setValue(p,stat,value));}
                finally {attribution.close();}}
        }
    }
    public static void run(GameController game) {
        require(active==null,"no concurrent fixture");
        var fixture=new Fixture(game);
        try {fixture.begin();active=fixture;SkyWars2.LOGGER.info("[check] START infinite-shulker (native grids/placed container generation passed; awaiting real ground scheduler)");}
        catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game) {
        if(active==null)return;
        try {require(active.game==game,"same native match owner");if(!active.step())return;active.close();active=null;
            SkyWars2.LOGGER.info("[check] PASS infinite-shulker (native2x2/3x3 pickup/shift/full inventory, exact donor debits, deterministic RNG-pure preview, strict unopened identity, placed base/upgraded/lucky generation, one separate owner/epoch shell per lucky fill, repeat/null-player/unrelated/spectator exclusions, real20tick ground and Time Stop pause)");}
        catch(Throwable error){try{active.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{active=null;}SkyWars2.LOGGER.error("[check] FAIL infinite-shulker",error);}
    }
}
