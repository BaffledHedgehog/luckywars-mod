package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ModeScrollChecks {
    private ModeScrollChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Mode scroll: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var p=f.owner;var profile=game.players().get(p.getUUID());boolean setting=profile.modeScroll;
            try{
                require(new PlayerStore.Profile().modeScroll,"default on");profile.modeScroll=true;
                p.getInventory().setSelectedSlot(0);p.setShiftKeyDown(true);game.items().mana().set(p,100000);
                for(var item:List.of(LbcWands.ACADEMY_TESLA,LbcWands.ACADEMY_RUBY,LbcWands.ACADEMY_STONE,LbcWands.ACADEMY_FIRE,
                        LbcWands.ACADEMY_BLIND,LbcWands.ACADEMY_BEDROCK,LbcWands.ACADEMY_BAD_WIKI,LbcWands.ACADEMY_AWAKENED_ACID,
                        LbcWands.ACADEMY_LIGHTNING_BOLTS,LbcWands.ACADEMY_CONSUMING,LbcWands.ACADEMY_LIFESTEALER)){
                    var stack=new ItemStack(item);p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setXRot(90);
                    int before=LbcFartus.data(stack).getIntOr("mode",1),mana=game.items().mana().get(p);
                    require(LbcModeScroll.withSteps(-1,()->p.gameMode.useItem(p,f.level,stack,InteractionHand.MAIN_HAND)).consumesAction(),"native reverse mode admission: "+item);
                    require(LbcFartus.data(stack).getIntOr("mode",1)!=before,"reverse changes actual held mode: "+item);
                    require(LbcModeScroll.withSteps(1,()->p.gameMode.useItem(p,f.level,stack,InteractionHand.MAIN_HAND)).consumesAction()
                            &&LbcFartus.data(stack).getIntOr("mode",1)==before&&game.items().mana().get(p)==mana,"native forward/reverse restores mode without payment: "+item);
                }
                var stack=new ItemStack(LbcWands.ACADEMY_TESLA);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
                f.client.packets().clear();p.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(4));
                require(p.getInventory().getSelectedSlot()==0&&LbcFartus.data(stack).getIntOr("mode",1)==2,"four raw slots apply four modes without changing hand");
                var ping=(ClientboundPingPacket)f.client.packets().stream().filter(e->e instanceof ClientboundPingPacket).findFirst().orElseThrow();
                p.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(6));
                require(LbcFartus.data(stack).getIntOr("mode",1)==1,"unacknowledged raw scrolling uses previous client slot");
                p.connection.handlePong(new ServerboundPongPacket(ping.getId()));
                p.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(8));
                require(LbcFartus.data(stack).getIntOr("mode",1)==3&&p.getInventory().getSelectedSlot()==0,"ordered native Pong restores cyclic negative anchor");
                profile.modeScroll=false;p.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(3));
                require(p.getInventory().getSelectedSlot()==3,"disabled preference preserves ordinary native selection");
            }finally{profile.modeScroll=setting;p.setShiftKeyDown(false);}
        }
        SkyWars2.LOGGER.info("[check] PASS mode-scroll (native bidirectional wand inputs, four-slot delta, raw burst/Pong ordering, stable hand and disabled fallback)");
    }
}
