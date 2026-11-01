package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Stateless source shulker recipes and the native lucky-container generation reward. */
public final class LbcInfiniteShulker {
    private static final ResourceKey<LootTable> BASE = table("lbc:infinity");
    private static final ResourceKey<LootTable> UPGRADED = table("lbc:infinity_upgraded");
    private static final ResourceKey<LootTable> LUCKY = table("lbc:infinity_upgraded_luck");

    private LbcInfiniteShulker() {}

    private static ResourceKey<LootTable> table(String id) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id));
    }

    private static boolean unopened(ItemStack stack, String flag, ResourceKey<LootTable> table) {
        if (stack.isEmpty() || LbcFartus.data(stack).getIntOr(flag, 0) != 1) return false;
        var loot = stack.get(DataComponents.CONTAINER_LOOT);
        return loot != null && loot.lootTable().equals(table);
    }

    public static boolean isBase(ItemStack stack) { return unopened(stack, "inf_shulker", BASE); }
    public static boolean isUpgraded(ItemStack stack) { return unopened(stack, "inf_inf_shulker", UPGRADED); }
    public static boolean isLuckyWand(ItemStack stack) {
        return !stack.isEmpty() && LbcFartus.data(stack).getIntOr("luckyblockwand", 0) == 1;
    }

    /** The same exact donor recognition is shared by native slots and tracked ground items. */
    public static LbcCraftingRecipe.Kind pairKind(ItemStack first, ItemStack second) {
        if (isBase(first) && isBase(second)) return LbcCraftingRecipe.Kind.INFINITE_SHULKER;
        if (isUpgraded(first) && isLuckyWand(second) || isLuckyWand(first) && isUpgraded(second))
            return LbcCraftingRecipe.Kind.LUCKY_SHULKER;
        return null;
    }

    public static LbcCraftingRecipe.Kind recipeKind(CraftingInput input) {
        if (input.ingredientCount() != 2) return null;
        ItemStack first = ItemStack.EMPTY;
        for (var stack : input.items()) if (!stack.isEmpty()) {
            if (first.isEmpty()) first = stack;
            else return pairKind(first, stack);
        }
        return null;
    }

    public static ItemStack assemble(GameController game, CraftingInput input) {
        return result(game, recipeKind(input));
    }

    /** Fixed local RNG: preview never advances a world or named loot-sequence RNG. */
    public static ItemStack result(GameController game, LbcCraftingRecipe.Kind kind) {
        String id;
        if (kind == LbcCraftingRecipe.Kind.INFINITE_SHULKER) id = "lbc:infinity_upgraded_only_shulker";
        else if (kind == LbcCraftingRecipe.Kind.LUCKY_SHULKER) id = "lbc:infinity_upgraded_only_shulker_luck";
        else return ItemStack.EMPTY;
        var level = game.maps().level();
        if (level == null) return ItemStack.EMPTY;
        var loot = game.server().reloadableRegistries().getLootTable(table(id));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.COMMAND);
        var rolls = loot.getRandomItems(params, 1L);
        return rolls.size() == 1 && rolls.getFirst().getCount() == 1 ? rolls.getFirst().copy() : ItemStack.EMPTY;
    }

    /** Invoke only after a successful native take/publication, never during matching or preview. */
    public static void crafted(ServerLevel level, Vec3 at, LbcCraftingRecipe.Kind kind) {
        if (kind != LbcCraftingRecipe.Kind.INFINITE_SHULKER && kind != LbcCraftingRecipe.Kind.LUCKY_SHULKER) return;
        int color = kind == LbcCraftingRecipe.Kind.LUCKY_SHULKER
                ? net.minecraft.util.ARGB.colorFromFloat(1, 1, .867f, .271f) & 0xffffff : 0;
        level.sendParticles(new DustParticleOptions(color, 2), true, false,
                at.x, at.y + 1, at.z, 100, 1, 1, 1, 1);
        LbcOverload.sound(level, at, "minecraft:block.enchantment_table.use", 1, 1, 0, 0);
    }

    /** Common role, dimension and clock guards; item suppression policy has a separate owner. */
    public static boolean allowed(GameController game, ServerPlayer player) {
        return game != null && player != null && game.server().isSameThread()
                && !player.isSpectator() && game.canUseGameplayItems(player)
                && LbcDimensionItems.ordinary(player) && !MatchPlayerCheckpoint.restoring(player);
    }

    /** Revalidate a cached deterministic result before vanilla owns debit/delivery. */
    public static boolean blockTake(net.minecraft.world.inventory.AbstractContainerMenu menu,int slot,ServerPlayer player){
        if(slot!=0||!(menu instanceof net.minecraft.world.inventory.AbstractCraftingMenu crafting))return false;
        var slots=crafting.getInputGridSlots();
        if(slots.isEmpty()||!(slots.getFirst().container instanceof net.minecraft.world.inventory.CraftingContainer grid)
                ||recipeKind(grid.asCraftInput())==null)return false;
        var game=SkyWars2.game();
        boolean valid=allowed(game,player)&&!game.server().tickRateManager().isFrozen()
                &&!game.items().timeStop().stopped(player.level())&&!game.items().wands().timeStopped()&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&player.isAlive()&&!player.isRemoved()&&!game.isObserver(player)&&!game.awaitingRespawn(player.getUUID())
                &&player.level()==game.maps().level()&&game.items().wands().validRoleTarget(player,false)
                &&!game.items().actionsBlocked(player)&&!game.items().wands().inputTimeStopped(player)
                &&player.containerMenu==menu&&menu.stillValid(player);
        if(!valid)menu.sendAllDataToRemote();return !valid;
    }

    /** Called after native fill returns: a null new key alone cannot reward a repeat opening. */
    public static void generated(RandomizableContainer container, ResourceKey<LootTable> oldKey, ServerPlayer player) {
        if (!LUCKY.equals(oldKey) || container.getLootTable() != null || container.getLevel() != player.level()) return;
        var game = SkyWars2.game();
        if (!allowed(game, player)) return;
        var stack = result(game, LbcCraftingRecipe.Kind.LUCKY_SHULKER);
        if (stack.isEmpty()) return;
        var at = player.position();
        var output = new ItemEntity(player.level(), at.x, at.y, at.z, stack);
        output.setDefaultPickUpDelay();
        output.setThrower(player);
        game.items().markCurrentItemEpoch(output);
        if (!player.level().addFreshEntity(output)) return;

    }
}
