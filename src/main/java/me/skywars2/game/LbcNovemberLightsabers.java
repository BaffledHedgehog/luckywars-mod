package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.ScoreHolder;

/** Finite source November faction selection, phase acquisition and attributed kill scores. */
public final class LbcNovemberLightsabers {
    public enum Faction { UNDECIDED, JEDI, SITH }
    private final GameController game;
    private final Predicate<ServerPlayer> selectorAllowed;
    private final Map<UUID, String> players = new LinkedHashMap<>();
    private boolean restoring;

    /** The caller supplies the author's jammer policy; this owner adds no implicit jammer rule. */
    public LbcNovemberLightsabers(GameController game, Predicate<ServerPlayer> selectorAllowed) {
        this.game = Objects.requireNonNull(game);
        this.selectorAllowed = Objects.requireNonNull(selectorAllowed);
    }
    public Faction faction(UUID player) {
        var online = game.server().getPlayerList().getPlayer(player); var name = players.get(player);
        int score = online != null ? sourceScore(online, "november")
                : name == null ? 0 : sourceScore(ScoreHolder.forNameOnly(name), "november");
        return score == 1 ? Faction.JEDI : score == 2 ? Faction.SITH : Faction.UNDECIDED;
    }
    public int luckyBlockCount(UUID player) {
        var name = players.get(player); var online = game.server().getPlayerList().getPlayer(player);
        return online != null ? sourceScore(online, "lucky_block_count")
                : name == null ? 0 : sourceScore(ScoreHolder.forNameOnly(name), "lucky_block_count");
    }
    private int sourceScore(ScoreHolder holder, String name) {
        var board = game.server().getScoreboard(); var objective = board.getObjective(name);
        var score = objective == null ? null : board.getPlayerScoreInfo(holder, objective);
        return score == null ? 0 : score.value();
    }
    private void observe(ServerPlayer player) {
        players.put(player.getUUID(), player.getScoreboardName());
    }
    public int jediKills() { return sourceScore(ScoreHolder.forNameOnly("jedi"), "lbc.math"); }
    public int sithKills() { return sourceScore(ScoreHolder.forNameOnly("sith"), "lbc.math"); }
    public int trackedPlayers() { return players.size(); }
    public ItemStack reward(ServerPlayer player) {
        observe(player);
        return new ItemStack(switch (faction(player.getUUID())) {
            case UNDECIDED -> LbcNovemberLightsaberItems.SELECTOR;
            case JEDI -> LbcNovemberLightsaberItems.JEDI;
            case SITH -> LbcNovemberLightsaberItems.SITH;
        });
    }
    private boolean paused(ServerPlayer player) {
        return restoring || player.entityTags().contains("time_stopped") || game.items().timeStop().stopped(player.level())
                || game.items().wands().inputTimeStopped(player);
    }
    private boolean eligible(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && game.canUseGameplayItems(player)
                && !game.items().actionsBlocked(player) && !paused(player);
    }
    private void grant(ServerPlayer player) {
        for (var stack : game.lbc().rollTable(player.level(), player.position(), player, "lbc:lightsaber"))
            if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        player.getInventory().setChanged();
        game.items().mana().inventoryChanged(player);
    }

    /** Source workingitems/spell1_default dispatches only the actual main hand. */
    public InteractionResult use(ServerPlayer player, InteractionHand hand) {
        if (player == null || hand != InteractionHand.MAIN_HAND || !LbcNovemberLightsaberItems.selector(player.getItemInHand(hand)))
            return InteractionResult.PASS;
        if (!eligible(player) || !selectorAllowed.test(player)) return InteractionResult.FAIL;
        var beneath = player.level().getBlockState(BlockPos.containing(player.position().add(0, -1, 0)));
        if (beneath.is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.RED))) select(player, Faction.SITH);
        else if (beneath.is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLUE))) select(player, Faction.JEDI);
        else {
            player.sendSystemMessage(Component.translatable("select_path_november"));
            // /execute rotated ~ 0: feet origin, same yaw and zero pitch, normal setblock updates.
            var source = player.createCommandSourceStack().withRotation(new net.minecraft.world.phys.Vec2(0, player.getYRot()));
            var blue = BlockPos.containing(new LocalCoordinates(1, 0, 2).getPosition(source));
            var red = BlockPos.containing(new LocalCoordinates(-1, 0, 2).getPosition(source));
            game.items().indirectKills().worldChanges(player.level(), player, () -> {
                LbcDropBlocks.setBlock(player.level(), blue, Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLUE).defaultBlockState(), false);
                LbcDropBlocks.setBlock(player.level(), red, Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(), false);
            });
        }
        LbcPhantomArmor.used(player, InteractionResult.SUCCESS);
        return InteractionResult.SUCCESS;
    }
    private void select(ServerPlayer player, Faction faction) {
        // /clear removes every matching token in inventory, armor, offhand, crafting and cursor.
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            if (LbcNovemberLightsaberItems.selector(inventory.getItem(slot))) {if(!consumeTmtSelector(inventory.getItem(slot)))inventory.setItem(slot, ItemStack.EMPTY);}
        var crafting = player.inventoryMenu.getCraftSlots();
        for (int slot = 0; slot < crafting.getContainerSize(); slot++)
            if (LbcNovemberLightsaberItems.selector(crafting.getItem(slot))) {if(!consumeTmtSelector(crafting.getItem(slot)))crafting.setItem(slot, ItemStack.EMPTY);}
        if (LbcNovemberLightsaberItems.selector(player.containerMenu.getCarried())&&!consumeTmtSelector(player.containerMenu.getCarried())) player.containerMenu.setCarried(ItemStack.EMPTY);
        observe(player);
        game.items().mana().score(player, "november").set(faction.ordinal());
        player.sendSystemMessage(Component.translatable(faction == Faction.JEDI ? "selected_jedi" : "selected_sith")
                .withStyle(faction == Faction.JEDI ? ChatFormatting.AQUA : ChatFormatting.RED));
        grant(player);
    }
    /** The selector property is spent; unrelated generated properties are not disposable source tokens. */
    private static boolean consumeTmtSelector(ItemStack stack){
        if(!LbcTmtItems.matches(stack))return false;
        LbcTmtAbilities.afterScopes(()->{
            if(stack.isEmpty())return;var slots=LbcTmtAbilities.read(stack);
            var retained=slots.stream().filter(slot->((ItemData.IntReader)(Object)slot.data()).skywars2$readInt("lightsaberrc",0)!=1).toList();
            if(retained.size()==slots.size())return;
            var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();data.remove("lightsaberrc");
            LbcTmtAbilities.write(data,retained);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
        });return true;
    }

    /** Invoke only after a real accepted LuckyBlock open, once per player open. */
    public void onLuckyBlockOpened(ServerPlayer player) {
        onCounterChanged(player);
    }
    /** Also called after native KFC/other source-score mutations; enrolls without scanning players. */
    public void onCounterChanged(ServerPlayer player) {
        sourceScoreChanged(player);
    }
    public void sourceScoreChanged(ServerPlayer player) {
        if (player != null && !restoring) observe(player);
    }
    /** Invoke at the common players_1s phase; paused worlds retain their pending acquisition. */
    public void second() {
        if (restoring || !game.playing() || !game.config().eventNovember) return;
        for (var entry : players.entrySet()) {
            var player = game.server().getPlayerList().getPlayer(entry.getKey());
            if (!eligible(player) || sourceScore(player, "lucky_block_count") < 1) continue;
            grant(player);
            game.items().mana().score(player, "lucky_block_count").set(-2147483646);
        }
    }
    /** The existing kill attribution/reward owner supplies the resolved killer identity. */
    public void onKill(ServerPlayer killer, ServerPlayer victim) {
        if (restoring || killer == null || victim == null || killer == victim || !game.playing()
                || !LbcNovemberLightsaberItems.blade(killer.getMainHandItem())) return;
        observe(killer); observe(victim);
        var attacker = faction(killer.getUUID()); var target = faction(victim.getUUID());
        if (attacker == Faction.JEDI && target == Faction.SITH)
            game.items().mana().score(ScoreHolder.forNameOnly("jedi"), "lbc.math").set(jediKills() + 1);
        if (attacker == Faction.SITH && target == Faction.JEDI)
            game.items().mana().score(ScoreHolder.forNameOnly("sith"), "lbc.math").set(sithKills() + 1);
    }
    public void clear() {
        players.clear(); restoring = false;
    }
    void prepareFrozen() { restoring = true; }
    void publishFrozen() { restoring = false; }
    Checkpoint checkpoint() { return new Checkpoint(); }
    final class Checkpoint {
        private final Map<UUID, String> saved = Map.copyOf(players);
        MatchFieldCheckpoint.Plan prepare(Function<UUID, Entity> lookup) {
            return new MatchFieldCheckpoint.Plan(Set.of(), () -> {
                players.clear(); players.putAll(saved);
            });
        }
    }
}
