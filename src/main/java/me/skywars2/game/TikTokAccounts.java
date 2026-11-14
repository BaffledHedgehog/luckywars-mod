package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.body.*;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Native cookie-import UI and the persistent authenticated For You preparation queue. */
public final class TikTokAccounts {
    private record Dialog(String token,long expires) {}
    private static final Map<ServerPlayer,Dialog> dialogs=new WeakHashMap<>();
    private static TikTokCache cache;
    private static MinecraftServer server;
    private static long tick;
    private TikTokAccounts(){}

    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher,registries,environment)->commands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(TikTokAccounts::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(ignored->stop());
        ServerTickEvents.END_SERVER_TICK.register(current->{
            if(server!=current)return;tick++;
            if(cache!=null)cache.tick(tick,Set.of());
            if(tick%200==0)dialogs.entrySet().removeIf(e->e.getValue().expires<tick);
        });
    }

    private static void commands(CommandDispatcher<CommandSourceStack> dispatcher){
        SkyWarsCommandTree.add(dispatcher,Commands.literal("tiktok")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("import").executes(ctx->{open(ctx.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("status").executes(ctx->{
                    ctx.getSource().sendSuccess(()->Component.translatable("tiktok.status",cache==null?0:cache.sizeReady(),5,
                            Component.translatable(cache!=null&&cache.configured()?"tiktok.configured":"tiktok.not_configured"),
                            Component.translatable(cache==null?"tiktok.unavailable":cache.stateKey())),false);return 1;
                })));
    }

    private static void start(MinecraftServer current){
        stop();server=current;tick=0;
        Path root=current.getWorldPath(LevelResource.ROOT).resolve("data/luckywars/tiktok");
        Path cookies=FabricLoader.getInstance().getConfigDir().resolve("skywars2/tiktok/cookies.json");
        try{cache=new TikTokCache(root,cookies);}
        catch(IOException failure){SkyWars2.LOGGER.warn("TikTok preparation unavailable (cache initialization)");}
    }
    private static void stop(){
        if(cache!=null)cache.close();cache=null;server=null;dialogs.clear();
    }
    private static boolean allowed(ServerPlayer player){
        return server!=null&&server.getPlayerList().getPlayer(player.getUUID())==player
                &&Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    static Holder<net.minecraft.server.dialog.Dialog> dialog(String token){
        var input=new Input("cookies",new TextInput(400,Component.translatable("tiktok.cookies"),true,"",
                TikTokCookies.MAX_INPUT,Optional.of(new TextInput.MultilineOptions(Optional.empty(),Optional.of(150)))));
        var common=new CommonDialogData(Component.translatable("tiktok.import"),Optional.empty(),true,false,
                DialogAction.CLOSE,List.of(new PlainMessage(Component.translatable("tiktok.import_hint",TikTokCookies.MAX_INPUT),400)),List.of(input));
        var save=new ActionButton(new CommonButtonData(Component.translatable("save"),150),
                Optional.of(new CustomAll(Identifier.parse("skywars2:tiktok/import/"+token),Optional.empty())));
        return Holder.direct(new MultiActionDialog(common,List.of(save),Optional.empty(),1));
    }
    private static void open(ServerPlayer player){
        if(!allowed(player))return;
        if(cache==null){message(player,"tiktok.unavailable",true);return;}
        String token=UUID.randomUUID().toString();dialogs.put(player,new Dialog(token,tick+1200));
        player.openDialog(dialog(token));
    }
    public static void click(ServerPlayer player,Identifier id,Optional<Tag> payload){
        if(!allowed(player))return;
        var pending=dialogs.get(player);
        if(pending==null||pending.expires<tick||!id.equals(Identifier.parse("skywars2:tiktok/import/"+pending.token)))return;
        dialogs.remove(player);
        String value=payload.orElse(null) instanceof CompoundTag tag?tag.getString("cookies").orElse(""):"";
        try{
            if(cache==null)throw new IOException();
            cache.importCookies(value);message(player,"tiktok.imported",false);
        }catch(IllegalArgumentException invalid){message(player,"tiktok.cookies_invalid",true);}
        catch(IOException failure){message(player,"tiktok.save_failed",true);}
    }
    private static void message(ServerPlayer player,String key,boolean error){
        player.sendSystemMessage(Component.translatable(key).withStyle(style->style.withItalic(false)
                .withColor(error?ChatFormatting.RED:ChatFormatting.GREEN)));
    }
}
