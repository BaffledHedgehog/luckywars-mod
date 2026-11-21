package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.item.ItemStack;

/** Real menu/start grant and the existing protected-inventory intake owner. */
public final class LuckyLoadoutChecks {
    private static Scene running;
    private LuckyLoadoutChecks() {}
    private static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException("Lucky loadouts: " + why);
    }

    public static void menu(GameController game) {
        require(game.inLobby(), "ready lobby selection");
        var player = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player != null, "real fixture player");
        var profile = game.players().get(player.getUUID());
        game.config().ranked = false;
        game.config().lbcKits = true;
        game.config().lbcSkills = true;
        profile.lbcUnlocks.remove("lbc:true_advancements/armor/chaos_full");
        profile.lbcUnlocks.remove("lbc:true_advancements/accesories/magnet");
        profile.lbcKit = 0;
        profile.lbcSkill = 0;
        require(!game.lbc().select(player, LbcLoadouts.Kind.KITS, 9000, 5)
                && !game.lbc().select(player, LbcLoadouts.Kind.SKILLS, 10000, 1), "actual source achievement locks");
        require(game.lbc().unlock(player, "lbc:true_advancements/armor/chaos_full")
                && game.lbc().unlock(player, "lbc:true_advancements/accesories/magnet"), "real native unlocks");
        require(game.lbc().select(player, LbcLoadouts.Kind.KITS, 9000, 5)
                && game.lbc().select(player, LbcLoadouts.Kind.SKILLS, 10000, 1)
                && profile.lbcKit == 6 && profile.lbcSkill == 2, "actual original menu selections");
        game.config().save();
        SkyWars2.LOGGER.info("[check] PASS lucky-loadouts-menu (source locks/unlocks and actual kit6/skill2 selection)");
    }

    public static void run(GameController game) {
        require(running == null, "exclusive scene");
        var scene = new Scene(game);
        running = scene;
        try { scene.begin(); }
        catch (Throwable error) {
            running = null;
            try { scene.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    public static void tick(GameController game) {
        var scene = running;
        if (scene == null) return;
        try {
            require(scene.f.game == game, "same live controller");
            if (!scene.step()) return;
            running = null;
            scene.close();
            SkyWars2.LOGGER.info("[check] PASS lucky-loadouts (real start kit16/1/1 native lucky blocks; selected skill2 protects inventory/offhand, deliberate mainhand native wire intake spends once, disabled skill restores ordinary intake)");
        } catch (Throwable error) {
            running = null;
            try { scene.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            SkyWars2.LOGGER.error("[check] FAIL lucky-loadouts", error);
        }
    }

    private static final class Scene implements AutoCloseable {
        final WoolBucketChecks.Fixture f;
        int phase, started;
        Scene(GameController game) { f = new WoolBucketChecks.Fixture(game, false); }
        int count(LbcLuckyBlocks.Kind kind) {
            int result = 0;
            for (int slot = 0; slot < f.owner.getInventory().getContainerSize(); slot++) {
                var stack = f.owner.getInventory().getItem(slot);
                if (stack.is(LbcLuckyBlocks.item(kind))) result += stack.getCount();
            }
            return result;
        }
        void begin() {
            require(count(LbcLuckyBlocks.Kind.NORMAL) == 16 && count(LbcLuckyBlocks.Kind.NEKO) == 1
                    && count(LbcLuckyBlocks.Kind.LOLI) == 1, "real startup grants exact native source kit, no raw mushroom carrier");
            f.prepareParticipants();
            f.game.config().lbcSkills = true;
            require(f.game.lbc().hasSkill(f.owner, 2)
                    && f.game.items().inventoryHazards().protectedInventory(f.owner), "selected ready skill admitted");
            f.owner.getInventory().setItem(9, new ItemStack(LbcNexusWireItems.RED));
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(LbcNexusWireItems.RED));
            changed();
        }
        void changed() {
            f.owner.getInventory().setChanged();
            f.game.items().nexusWires().inventoryChanged(f.owner);
            started = f.game.server().getTickCount();
        }
        boolean step() {
            int elapsed = f.game.server().getTickCount() - started;
            require(elapsed < 20, "native intake makes bounded progress");
            if (elapsed < 3) return false;
            if (phase == 0) {
                require(f.owner.getInventory().getItem(9).is(LbcNexusWireItems.RED)
                        && f.owner.getOffhandItem().is(LbcNexusWireItems.RED), "protected inventory and offhand are not automatically consumed");
                f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, f.owner.getInventory().getItem(9));
                f.owner.getInventory().setItem(9, ItemStack.EMPTY);
                phase++;
                changed();
                return false;
            }
            if (phase == 1) {
                require(f.owner.getMainHandItem().isEmpty() && f.owner.getOffhandItem().is(LbcNexusWireItems.RED),
                        "actual mainhand authorizes one native source intake, not arbitrary protected inventory");
                f.game.config().lbcSkills = false;
                require(!f.game.items().inventoryHazards().protectedInventory(f.owner), "disabled skill does not protect");
                phase++;
                changed();
                return false;
            }
            require(f.owner.getOffhandItem().isEmpty(), "ordinary native inventory intake resumes when disabled");
            return true;
        }
        public void close() { f.close(); }
    }
}
