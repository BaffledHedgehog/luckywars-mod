package me.skywars2.game.items.phone;

import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.commands.SkyWarsCommandTree;
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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.*;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Native cookie-import UI and the persistent authenticated For You preparation queue. */
public final class TikTokAccounts {
    private record Dialog(String token,long expires,boolean file) {}
    private static final Map<ServerPlayer,Dialog> dialogs=new WeakHashMap<>();
    private static TikTokCache cache;
    private static volatile TikTokPlayback playback;
    private static MinecraftServer server;
    private static long tick;
    private static ExecutorService importer;
    private static Future<String> importing;
    private static ServerPlayer importPlayer;
    private TikTokAccounts(){}

    public static void register(){
        TikTokClientSupport.register();
        CommandRegistrationCallback.EVENT.register((dispatcher,registries,environment)->commands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(TikTokAccounts::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(ignored->stop());
        ServerTickEvents.END_SERVER_TICK.register(current->{
            if(server!=current)return;tick++;
            completeImport();
            if(playback!=null)playback.tick();
            if(cache!=null){cache.demand(current.getPlayerList().getPlayers().stream().anyMatch(p->!TikTokClientSupport.independentHands(p)));cache.tick(tick,playback==null?Set.of():playback.pins());}
            if(tick%200==0)dialogs.entrySet().removeIf(e->e.getValue().expires<tick);
        });
    }

    private static void commands(CommandDispatcher<CommandSourceStack> dispatcher){
        SkyWarsCommandTree.add(dispatcher,Commands.literal("tiktok")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("import").requires(me.skywars2.game.commands.OwnerCommands::allowed).executes(ctx->{open(ctx.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("importfile").requires(me.skywars2.game.commands.OwnerCommands::allowed).executes(ctx->{open(ctx.getSource().getPlayerOrException(),true);return 1;})
                    .then(Commands.argument("path",com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(ctx->{
                        importFile(ctx.getSource().getPlayerOrException(),com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"path"));return 1;
                    })))
                .then(Commands.literal("status").executes(ctx->{
                    ctx.getSource().sendSuccess(()->Component.translatable("tiktok.status",cache==null?0:cache.sizeReady(),TikTokCache.CAPACITY,
                            Component.translatable(cache!=null&&cache.configured()?"tiktok.configured":"tiktok.not_configured"),
                            Component.translatable(cache==null?"tiktok.unavailable":cache.stateKey()))
                            .append(Component.translatableWithFallback("tiktok.direct_status"," · ссылки для клиентского мода: %s/20",playback==null?0:playback.directReady()))
                            .append(cache!=null&&!cache.error().isEmpty()?" ("+cache.error()+")":""),false);return 1;
                })));
    }

    private static void start(MinecraftServer current){
        stop();server=current;tick=0;
        importer=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"skywars-tiktok-cookie-import");t.setDaemon(true);return t;});
        Path root=current.getWorldPath(LevelResource.ROOT).resolve("data/luckywars/tiktok");
        Path cookies=FabricLoader.getInstance().getConfigDir().resolve("skywars2/tiktok/cookies.json");
        try{cache=new TikTokCache(root,cookies);playback=new TikTokPlayback(current,cache);}
        catch(IOException failure){SkyWars2.LOGGER.warn("TikTok preparation unavailable (cache initialization)");}
    }
    private static void stop(){
        if(importer!=null)importer.shutdownNow();importer=null;importing=null;importPlayer=null;
        if(playback!=null)playback.close();playback=null;
        if(cache!=null)cache.close();cache=null;server=null;dialogs.clear();
    }
    public static TikTokPlayback playback(){return playback;}
    private static boolean allowed(ServerPlayer player){
        return server!=null&&server.getPlayerList().getPlayer(player.getUUID())==player
                &&me.skywars2.game.commands.OwnerCommands.allowed(player.createCommandSourceStack())
                &&Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    static Holder<net.minecraft.server.dialog.Dialog> dialog(String token){
        return dialog(token,false);
    }
    static Holder<net.minecraft.server.dialog.Dialog> dialog(String token,boolean file){
        var input=new Input(file?"path":"cookies",new TextInput(400,Component.translatable(file?"tiktok.cookie_file":"tiktok.cookies"),true,"",
                file?2048:TikTokCookies.MAX_DIALOG_INPUT,file?Optional.empty():Optional.of(new TextInput.MultilineOptions(Optional.empty(),Optional.of(150)))));
        var common=new CommonDialogData(Component.translatable(file?"tiktok.import_file":"tiktok.import"),Optional.empty(),true,false,
                DialogAction.CLOSE,List.of(new PlainMessage(file?Component.translatable("tiktok.import_file_hint",TikTokCookies.MAX_INPUT/1024/1024)
                        :Component.translatable("tiktok.import_hint",TikTokCookies.MAX_DIALOG_INPUT),400)),List.of(input));
        var save=new ActionButton(new CommonButtonData(Component.translatable("save"),150),
                Optional.of(new CustomAll(Identifier.parse((file?"skywars2:tiktok/import_file/":"skywars2:tiktok/import/")+token),Optional.empty())));
        var fileButton=new ActionButton(new CommonButtonData(Component.translatable("tiktok.import_file"),200),
                Optional.of(new StaticAction(new ClickEvent.Custom(Identifier.parse("skywars2:tiktok/open_file/"+token),Optional.empty()))));
        return Holder.direct(new MultiActionDialog(common,file?List.of(save):List.of(save,fileButton),Optional.empty(),1));
    }
    private static void open(ServerPlayer player){
        open(player,false);
    }
    private static void open(ServerPlayer player,boolean file){
        if(!allowed(player))return;
        if(cache==null){message(player,"tiktok.unavailable",true);return;}
        String token=UUID.randomUUID().toString();dialogs.put(player,new Dialog(token,tick+1200,file));
        player.openDialog(dialog(token,file));
    }
    public static void click(ServerPlayer player,Identifier id,Optional<Tag> payload){
        if(!allowed(player))return;
        var pending=dialogs.get(player);
        if(pending==null||pending.expires<tick)return;
        if(!pending.file&&id.equals(Identifier.parse("skywars2:tiktok/open_file/"+pending.token))){dialogs.remove(player);open(player,true);return;}
        if(!id.equals(Identifier.parse((pending.file?"skywars2:tiktok/import_file/":"skywars2:tiktok/import/")+pending.token)))return;
        dialogs.remove(player);
        String value=payload.orElse(null) instanceof CompoundTag tag?tag.getString(pending.file?"path":"cookies").orElse(""):"";
        if(pending.file){importFile(player,value);return;}
        try{
            if(cache==null)throw new IOException();
            cache.importCookies(value);
            if(importing!=null){importing.cancel(true);importing=null;importPlayer=null;}
            if(playback!=null)playback.reset();message(player,"tiktok.imported",false);
        }catch(IllegalArgumentException invalid){message(player,"tiktok.cookies_invalid",true);}
        catch(IOException failure){message(player,"tiktok.save_failed",true);}
    }
    private static void importFile(ServerPlayer player,String value){
        if(!allowed(player))return;
        if(importing!=null){message(player,"tiktok.loading",true);return;}
        try{
            String name=value.strip();if(name.length()>1&&name.startsWith("\"")&&name.endsWith("\""))name=name.substring(1,name.length()-1);
            Path file=Path.of(name);if(!file.isAbsolute())file=FabricLoader.getInstance().getGameDir().resolve(file);
            final Path path=file.toAbsolutePath().normalize();
            importing=importer.submit(()->TikTokCookies.fromFile(path));importPlayer=player;
        }catch(RuntimeException error){message(player,"tiktok.file_invalid",true);}
    }
    private static void completeImport(){
        if(importing==null||!importing.isDone())return;
        var actor=importPlayer;
        try{
            String normalized=importing.get();
            if(actor!=null&&allowed(actor)&&cache!=null){cache.applyImportedCookies(normalized);if(playback!=null)playback.reset();message(actor,"tiktok.imported",false);}
        }catch(Exception error){if(actor!=null&&allowed(actor))message(actor,"tiktok.file_invalid",true);}
        finally{importing=null;importPlayer=null;}
    }
    private static void message(ServerPlayer player,String key,boolean error){
        player.sendSystemMessage(Component.translatable(key).withStyle(style->style.withItalic(false)
                .withColor(error?ChatFormatting.RED:ChatFormatting.GREEN)));
    }
}
