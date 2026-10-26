package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Explicit rifle hand admission checks; skyward native fire never changes arena terrain. */
public final class RifleMainHandChecks {
    private RifleMainHandChecks() { }
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Rifle main hand: "+message);
    }
    private static void ready(GameController game,ServerPlayer owner,ItemStack gun,ItemStack ammo,InteractionHand hand) {
        game.items().magnum().forget(owner);
        game.items().shooters().resetCooldown(owner);
        game.items().rifleZoom().forget(owner);
        owner.getInventory().clearContent();
        owner.setItemInHand(hand,gun);
        owner.getInventory().setItem(9,ammo.copy());
        owner.setShiftKeyDown(false);
        owner.setYRot(0); owner.setXRot(-90); owner.setDeltaMovement(Vec3.ZERO);
    }
    private static void unchanged(GameController game,ServerPlayer owner,ItemStack gun,ItemStack before,ItemStack ammo,int mana,String context) {
        require(ItemStack.matches(gun,before) && ItemStack.matches(owner.getInventory().getItem(9),ammo)
                && game.items().mana().get(owner)==mana && game.items().magnum().shots(owner)==0
                && game.items().magnum().queued(owner)==0 && game.items().shooters().cooldownRemaining(owner)==0,
                context+" preserves gun components, ammunition, mana and shot/queue/cooldown state");
    }
    public static void run(GameController game) {
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null && game.playing() && game.allowDamage(owner),"requires active TestAlpha");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        boolean offhand=game.config().offhandWands,shift=owner.isShiftKeyDown();
        try {
            owner.closeContainer(); owner.removeAllEffects(); owner.setGameMode(GameType.SURVIVAL);
            owner.entityTags().removeAll(java.util.Set.of("time_stopped","nomagic_active"));
            game.items().mana().noMagic(owner,0); game.items().mana().set(owner,1234);
            owner.setNoGravity(true);
            owner.teleportTo(game.maps().level(),32.5,300,32.5,java.util.Set.of(),0,-90,true);
            var magnum=new ItemStack(LbcMagnum.ITEM);
            ItemStack round=null;
            for(int attempt=0;attempt<200 && round==null;attempt++)
                for(var candidate:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/magnum_ammo"))
                    if(candidate.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY)
                            .copyTag().getIntOr("standard_round",0)==1) { round=candidate.copy(); round.setCount(1); break; }
            require(round!=null,"source standard Magnum round is available");
            for(boolean enabled:new boolean[]{false,true}) {
                game.config().offhandWands=enabled;
                for(boolean isMagnum:new boolean[]{true,false}) {
                    var gun=isMagnum?magnum.copy():new ItemStack(LbcShooters.SLR);
                    var ammo=isMagnum?round:new ItemStack(Items.ARROW,3);
                    ready(game,owner,gun,ammo,InteractionHand.OFF_HAND);
                    var before=gun.copy(); int mana=game.items().mana().get(owner);
                    require(!(isMagnum?game.items().magnum().use(owner,InteractionHand.OFF_HAND)
                            :game.items().shooters().use(owner,InteractionHand.OFF_HAND)),"service rejects offhand with setting "+enabled);
                    require(gun.getItem().use(owner.level(),owner,InteractionHand.OFF_HAND)==InteractionResult.FAIL,
                            "native item callback rejects offhand with setting "+enabled);
                    for(int repeat=0;repeat<6;repeat++)
                        require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"repeated shared MAIN dispatch rejects offhand rifle");
                    owner.setShiftKeyDown(true);
                    require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)
                            && gun.getItem().use(owner.level(),owner,InteractionHand.OFF_HAND)==InteractionResult.FAIL,
                            "offhand mode switching is rejected");
                    unchanged(game,owner,gun,before,ammo,mana,"offhand rifle setting "+enabled);
                    ready(game,owner,gun,ammo,InteractionHand.MAIN_HAND);
                    require(gun.getItem().use(owner.level(),owner,InteractionHand.MAIN_HAND).consumesAction(),"ordinary native main-hand rifle fires");
                    require(owner.getInventory().getItem(9).getCount()==ammo.getCount()-1
                            && game.items().mana().get(owner)==mana && game.items().shooters().cooldownRemaining(owner)>0,
                            "main-hand fire pays exactly one ammo without mana");
                    if(isMagnum) {
                        require(game.items().magnum().shots(owner)==1 && game.items().magnum().queued(owner)==4,"main-hand Magnum queues four follow-up shots");
                        owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                        owner.setItemInHand(InteractionHand.OFF_HAND,gun);
                        game.items().magnum().tick();
                        require(game.items().magnum().shots(owner)==1 && game.items().magnum().queued(owner)==0,
                                "moving exact queued gun into offhand cancels before follow-up shot");
                        game.items().magnum().addPlayerCounters(owner);
                        require(game.items().magnum().queued(owner)==0,"KFC counter extension cannot enqueue offhand-only Magnum");
                    }
                }
            }
            // Shared MAIN dispatch must reject the spare rifle without cycling its mode.
            game.config().offhandWands=true;
            for(boolean mainMagnum:new boolean[]{true,false}) {
                var main=mainMagnum?magnum.copy():new ItemStack(LbcShooters.SLR);
                var spare=mainMagnum?new ItemStack(LbcShooters.SLR):magnum.copy();
                ready(game,owner,main,round,InteractionHand.MAIN_HAND);
                owner.setItemInHand(InteractionHand.OFF_HAND,spare); owner.setShiftKeyDown(true);
                var before=spare.copy();
                require(game.items().wands().use(owner,InteractionHand.MAIN_HAND) && ItemStack.matches(spare,before),
                        "mixed rifle main-hand mode switch leaves spare rifle untouched");
            }
            SkyWars2.LOGGER.info("[check] PASS rifle-main-hand");
        } finally {
            game.items().rifleZoom().forget(owner); game.items().magnum().forget(owner);
            game.items().shooters().resetCooldown(owner);
            game.config().offhandWands=offhand; saved.restore(game); owner.setShiftKeyDown(shift);
        }
    }
}
