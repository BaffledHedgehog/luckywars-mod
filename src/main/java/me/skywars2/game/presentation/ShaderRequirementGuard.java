package me.skywars2.game.presentation;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

/** Recipient-only fallback geometry. Working core shaders clip its exact private material. */
public final class ShaderRequirementGuard {
    public static final int MARKER=0xFE01FB;
    private record View(ServerPlayer player,ServerLevel level,ElementHolder holder,
                        ItemDisplayElement cover,List<TextDisplayElement> labels) { }
    private static final Map<ServerGamePacketListenerImpl,View> VIEWS=new IdentityHashMap<>();
    private ShaderRequirementGuard() { }

    public static void synchronize(GameController game,ServerPlayer player) {
        if(player.connection==null)return;
        var view=VIEWS.get(player.connection);
        if(view!=null&&(view.player!=player||view.level!=player.level())){forget(player);view=null;}
        if(view==null){
            var item=new ItemStack(Items.PAPER);
            item.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:shader_guard"));
            var cover=new ItemDisplayElement(item);
            cover.setItemDisplayContext(ItemDisplayContext.NONE);
            cover.setBillboardMode(Display.BillboardConstraints.FIXED);
            cover.setDisplaySize(0,0);cover.setViewRange(64);
            cover.setInterpolationDuration(0);cover.setTeleportDuration(0);
            var holder=new ElementHolder();holder.addElement(cover);
            var labels=new ArrayList<TextDisplayElement>();
            var warning=Component.translatableWithFallback("shader.required.line1","Для игры необходимо")
                    .append("\n").append(Component.translatableWithFallback("shader.required.line2","ВКЛЮЧИТЬ ШЕЙДЕРЫ"))
                    .withStyle(style->style.withColor(MARKER).withItalic(false));
            for(int i=0;i<3;i++){
                var label=new TextDisplayElement(warning);
                label.setTextOpacity((byte)181);label.setBackground(0);
                label.setDefaultBackground(false);label.setShadow(false);label.setSeeThrough(true);
                label.setLineWidth(500);label.setBillboardMode(Display.BillboardConstraints.CENTER);
                label.setDisplaySize(0,0);label.setViewRange(.032f);
                label.setInterpolationDuration(0);label.setTeleportDuration(0);
                holder.addElement(label);labels.add(label);
            }
            game.visualTracking().attach(holder,player.level(),player::getEyePosition);
            view=new View(player,player.level(),holder,cover,List.copyOf(labels));
            VIEWS.put(player.connection,view);
            update(view);holder.startWatching(player);
        }else update(view);
        view.holder.tick();
    }
    private static void update(View view){
        float scale=view.player.getScale();
        view.cover.setScale(new Vector3f(4*scale));
        double[] distances={.5,-3.5,3.5};
        for(int i=0;i<3;i++){
            var label=view.labels.get(i);
            label.setOffset(view.player.getLookAngle().scale(distances[i]*scale));
            label.setScale(new Vector3f(.003f*scale));
        }
    }
    public static void tick(MinecraftServer server){
        var game=SkyWars2.game();if(game==null){stop();return;}
        var connected=Collections.newSetFromMap(new IdentityHashMap<ServerGamePacketListenerImpl,Boolean>());
        for(var player:server.getPlayerList().getPlayers())connected.add(player.connection);
        VIEWS.entrySet().removeIf(entry->{if(connected.contains(entry.getKey()))return false;entry.getValue().holder.destroy();return true;});
        for(var player:server.getPlayerList().getPlayers())synchronize(game,player);
    }
    public static void forget(ServerPlayer player){var view=VIEWS.remove(player.connection);if(view!=null)view.holder.destroy();}
    public static void stop(){for(var view:VIEWS.values())view.holder.destroy();VIEWS.clear();}
    public static List<ItemDisplayElement> displays(ServerPlayer player){var view=VIEWS.get(player.connection);return view==null?List.of():List.of(view.cover);}
    public static List<TextDisplayElement> labels(ServerPlayer player){var view=VIEWS.get(player.connection);return view==null?List.of():view.labels;}
}
