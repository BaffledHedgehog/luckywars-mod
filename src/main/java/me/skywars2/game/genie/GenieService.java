package me.skywars2.game.genie;

import java.util.*;
import net.minecraft.util.Prediction;
import java.util.concurrent.CompletableFuture;
import me.skywars2.game.*;
import me.skywars2.game.genie.script.GenieScriptReply;
import me.skywars2.game.genie.script.ScriptRepository;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import static me.skywars2.game.genie.GenieBlueprint.*;

/** Server-thread ownership of dialogs, a movable unique pending lamp and cancellation boundaries. */
public final class GenieService implements AutoCloseable {
    public static final String TOKEN="genie_pending";
    private static final String LAMP_CUSTOM_NAME="genie_lamp_original_name";
    private final GameController game;
    private final GenieRuntime runtime;
    // The declarative transport remains injectable so existing loopback regression tests exercise old items.
    private GenieModelClient model;
    private GenieScriptModel scriptModel;
    final GenieApiDialog api = new GenieApiDialog();
    private final Map<UUID,Dialog> dialogs=new HashMap<>();
    private final Map<UUID,Wish> wishes=new HashMap<>();
    private final ArrayDeque<TwistKind> recent=new ArrayDeque<>();
    private final ArrayDeque<String> recentScriptTwists=new ArrayDeque<>();
    private final Map<UUID,Integer> warnings=new HashMap<>();
    private record Dialog(String token,String epoch,ItemStack lamp){}
    private static final class Wish {
        final ServerPlayer owner;final String token,epoch;final ItemStack lamp;
        Runnable cancelRequest = () -> {};
        CompletableFuture<ScriptRepository.Loaded> prepared;
        Wish(ServerPlayer owner,String token,String epoch,ItemStack lamp){this.owner=owner;this.token=token;this.epoch=epoch;this.lamp=lamp;}
        void cancel() {
            cancelRequest.run();
            if (prepared != null) prepared.cancel(false);
        }
    }
    public GenieService(GameController game){
        this.game=game;runtime=new GenieRuntime(game);
        scriptModel=GenieConfig.loadScript();
    }
    public static void registerCommands() { GenieApiDialog.register(); }
    public GenieRuntime runtime(){return runtime;}
    private boolean livingOwner(ServerPlayer p){return game.playing()&&!game.balanceLegacy()&&!game.isObserver(p)&&p.isAlive()&&!p.isRemoved()
            &&game.server().getPlayerList().getPlayer(p.getUUID())==p;}
    private boolean allowed(ServerPlayer p){return livingOwner(p)&&runtime.admitted(p);}
    private static net.minecraft.network.chat.MutableComponent text(String key){return Component.translatable(key);}
    private static void tell(ServerPlayer p,String key,ChatFormatting color){p.sendSystemMessage(text(key).withStyle(color));}
    public boolean open(ServerPlayer p,InteractionHand hand){
        if(!GenieItems.lamp(p.getItemInHand(hand))||!allowed(p))return false;
        if(wishes.containsKey(p.getUUID())){refuse(p);return true;}
        var lamp=p.getItemInHand(hand);var session=new Dialog(UUID.randomUUID().toString(),game.items().epoch(),lamp);dialogs.put(p.getUUID(),session);
        p.level().playSound(null,p.getX(),p.getY(),p.getZ(),Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:lbcsound.djin_says"))),SoundSource.PLAYERS,1,1);
        var input=new Input("wish",new TextInput(360,text("genie.prompt"),true,"",2000,Optional.empty()));
        var send=new ActionButton(new CommonButtonData(text("genie.submit"),150),Optional.of(new CustomAll(Identifier.parse("skywars2:genie/"+session.token),Optional.empty())));
        var warning = Component.translatableWithFallback("genie.warning.first", "Если джин сочтёт ваше желание неуважительным,")
                .append("\n").append(Component.translatableWithFallback("genie.warning.second", "он насрёт вам в карманы."))
                .withStyle(ChatFormatting.RED);
        var data=new CommonDialogData(text("genie.lamp"),Optional.empty(),true,false,DialogAction.CLOSE,
                List.of(new PlainMessage(warning, 360)),List.of(input));
        p.openDialog(Holder.direct(new MultiActionDialog(data,List.of(send),Optional.empty(),1)));return true;
    }
    public void click(ServerPlayer p,Identifier id,Optional<Tag> payload){
        if (id.getPath().startsWith(GenieApiDialog.PREFIX)) {
            api.click(this, p, id, payload);
            return;
        }
        var session=dialogs.get(p.getUUID());if(session==null||!id.equals(Identifier.parse("skywars2:genie/"+session.token)))return;
        dialogs.remove(p.getUUID());
        if(!allowed(p)||!game.items().currentEpoch(session.epoch)||wishes.containsKey(p.getUUID())||!containsIdentity(p,session.lamp))return;
        String wish=payload.orElse(null) instanceof CompoundTag tag?tag.getString("wish").orElse(""):"";
        try{GenieBlueprint.plain(wish,2000);}catch(IllegalArgumentException invalid){tell(p,"genie.invalid_wish",ChatFormatting.RED);return;}
        if (model == null && scriptModel == null) scriptModel = GenieConfig.loadScript();
        if (scriptModel == null && (model == null || !model.configured())) {tell(p,"genie.unavailable",ChatFormatting.RED);return;}
        // An overstack is legal server loot. Reserve precisely ONE lamp and preserve its siblings.
        if(session.lamp.getCount()>1){
            int free=p.getInventory().getFreeSlot();if(free<0){tell(p,"genie.need_slot",ChatFormatting.RED);return;}
            var rest=session.lamp.copyWithCount(session.lamp.getCount()-1);session.lamp.setCount(1);p.getInventory().setItem(free,rest);
        }
        String token=UUID.randomUUID().toString();mark(session.lamp,token);
        var pending=new Wish(p,token,session.epoch,session.lamp);wishes.put(p.getUUID(),pending);
        tell(p,"genie.thinking",ChatFormatting.GOLD);sync(p);
        try {
            if (scriptModel != null) {
                var request = scriptModel.request(wish, List.copyOf(recentScriptTwists),
                        game.server().reloadableRegistries().lookup(), GeniePlayerParameters.wishContext(game, p),
                        game.items().tmt().genieAbilities().index());
                pending.cancelRequest = request::close;
                request.future().whenComplete((reply,error) -> game.server().execute(() -> finishScript(pending,reply,error)));
            } else {
                var request = model.request(wish, List.copyOf(recent));
                pending.cancelRequest = request::close;
                request.future().whenComplete((reply,error) -> game.server().execute(() -> finish(pending,reply,error)));
            }
        } catch (RuntimeException failed) {
            finishReward(pending, ItemStack.EMPTY, null, false);
        }
    }
    private static void mark(ItemStack lamp,String token){
        if(lamp.isEmpty())return;var data=lamp.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(token.isEmpty()){
            data.remove(TOKEN);
            var saved=data.get(LAMP_CUSTOM_NAME);
            if(saved!=null)ComponentSerialization.CODEC.parse(NbtOps.INSTANCE,saved).result()
                    .ifPresent(name->lamp.set(DataComponents.CUSTOM_NAME,name));
            data.remove(LAMP_CUSTOM_NAME);
            lamp.set(DataComponents.ITEM_NAME,GenieItems.IDLE_NAME);
        }else{
            data.putString(TOKEN,token);
            var custom=lamp.get(DataComponents.CUSTOM_NAME);
            if(custom!=null)ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE,custom).result()
                    .ifPresent(saved->{data.put(LAMP_CUSTOM_NAME,saved);lamp.remove(DataComponents.CUSTOM_NAME);});
            lamp.set(DataComponents.ITEM_NAME,GenieItems.THINKING_NAME);
        }
        lamp.set(DataComponents.TOOLTIP_STYLE,GenieItems.TOOLTIP_STYLE);
        lamp.set(DataComponents.CUSTOM_DATA,CustomData.of(data));lamp.set(DataComponents.ITEM_MODEL,token.isEmpty()?GenieItems.IDLE:GenieItems.THINKING);
    }
    private static boolean containsIdentity(ServerPlayer p,ItemStack stack){
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==stack)return true;
        return p.containerMenu.getCarried()==stack;
    }
    private interface Location {ItemStack get();void set(ItemStack stack);}
    private static Location find(ServerPlayer p,String token){
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            final int slot=i;if(token.equals(ItemData.stringValue(p.getInventory().getItem(i),TOKEN,"")))return new Location(){public ItemStack get(){return p.getInventory().getItem(slot);}public void set(ItemStack s){p.getInventory().setItem(slot,s);}};
        }
        if(token.equals(ItemData.stringValue(p.containerMenu.getCarried(),TOKEN,"")))return new Location(){public ItemStack get(){return p.containerMenu.getCarried();}public void set(ItemStack s){p.containerMenu.setCarried(s);}};
        return null;
    }
    private Location currentLamp(Wish wish) {
        if (wishes.get(wish.owner.getUUID()) != wish) return null;
        if (!livingOwner(wish.owner) || !game.items().currentEpoch(wish.epoch)) { cancel(wish.owner); return null; }
        Location location = find(wish.owner, wish.token);
        if (location == null) cancel(wish.owner);
        return location;
    }

    private void finish(Wish wish, Reply reply, Throwable failure) {
        if (currentLamp(wish) == null) return;
        if (failure != null || reply == null) { finishReward(wish, ItemStack.EMPTY, null, false); return; }
        if (!reply.granted()) { finishReward(wish, ItemStack.EMPTY, null, true); return; }
        ItemStack reward;
        try { reward = GenieRuntime.construct(reply.item()); }
        catch (RuntimeException invalid) { finishReward(wish, ItemStack.EMPTY, null, false); return; }
        if (finishReward(wish, reward, reply.answer(), false)) remember(recent, reply.item().twist().kind());
    }

    private void finishScript(Wish wish, GenieScriptReply reply, Throwable failure) {
        if (currentLamp(wish) == null) return;
        if (failure instanceof GenieGeminiWebTransport.SessionFailure) {
            finishReward(wish, ItemStack.EMPTY, null, false,
                    Component.translatableWithFallback("genie.session_unavailable", "Веб-сессия джина сейчас недоступна. Лампа возвращена."));
            return;
        }
        if (failure != null || reply == null) { finishReward(wish, ItemStack.EMPTY, null, false); return; }
        if (reply instanceof GenieScriptReply.Rejected) { finishReward(wish, ItemStack.EMPTY, null, true); return; }
        var grant = (GenieScriptReply.Grant) reply;
        wish.prepared = runtime.scripts().prepare(grant.program());
        wish.prepared.whenComplete((code,error) -> game.server().execute(() -> {
            if (currentLamp(wish) == null) return;
            if (error != null) { finishReward(wish, ItemStack.EMPTY, null, false); return; }
            ItemStack reward;
            try {
                reward = GenieScriptItem.construct(code, grant.carrier(), grant.name(), grant.description(), 1);
                if (!grant.nativeAbilities().isEmpty())
                    reward = game.items().tmt().genieAbilities().attach(reward, Identifier.parse(grant.icon()), grant.nativeAbilities());
            }
            catch (RuntimeException invalid) { finishReward(wish, ItemStack.EMPTY, null, false); return; }
            if (finishReward(wish, reward, grant.answer(), false)) remember(recentScriptTwists, grant.twist());
        }));
    }

    private static <T> void remember(ArrayDeque<T> recent, T value) {
        recent.addLast(value);
        while (recent.size() > 12) recent.removeFirst();
    }

    private boolean finishReward(Wish wish, ItemStack reward, String answer, boolean rejected) {
        return finishReward(wish, reward, answer, rejected, text("genie.failed"));
    }

    private boolean finishReward(Wish wish, ItemStack reward, String answer, boolean rejected, net.minecraft.network.chat.MutableComponent failureNotice) {
        var location = currentLamp(wish);
        if (location == null) return false;
        wishes.remove(wish.owner.getUUID());
        if(rejected){
            location.set(ItemStack.EMPTY);game.items().kfcFood().poopPockets(wish.owner,10);
            tell(wish.owner,"genie.rejected",ChatFormatting.GOLD);
        } else if (reward.isEmpty()) {
            mark(location.get(),"");wish.owner.sendSystemMessage(failureNotice.withStyle(ChatFormatting.RED));
        } else {
            location.set(reward);
            wish.owner.sendSystemMessage(Component.translatable("genie.answer",answer).withStyle(ChatFormatting.GREEN));
        }
        sync(wish.owner);
        return !reward.isEmpty();
    }
    /** No stale/forged token locks an item after restart, rewind, death or a cancelled request. */
    public boolean locked(ServerPlayer p,ItemStack stack){
        var wish=wishes.get(p.getUUID());return wish!=null&&wish.owner==p&&wish.token.equals(ItemData.stringValue(stack,TOKEN,""));
    }
    public void refuse(ServerPlayer p){
        int now=game.server().getTickCount();if(warnings.getOrDefault(p.getUUID(),Integer.MIN_VALUE/2)+20<=now){warnings.put(p.getUUID(),now);tell(p,"genie.dont_drop",ChatFormatting.GOLD);}
        p.containerMenu.sendAllDataToRemote();
    }
    private static void sync(ServerPlayer p){p.containerMenu.broadcastChanges();if(p.containerMenu!=p.inventoryMenu)p.inventoryMenu.broadcastChanges();}
    /** Player inventory/cursor moves are allowed; a crafting, merchant or external slot is not personal storage. */
    public boolean blockedClick(ServerPlayer p,AbstractContainerMenu menu,int slot,int button,ContainerInput type){
        if(!wishes.containsKey(p.getUUID()))return false;
        ItemStack at=slot>=0&&slot<menu.slots.size()?menu.getSlot(slot).getItem():ItemStack.EMPTY;
        boolean held=locked(p,menu.getCarried()),source=locked(p,at);
        boolean personal=slot>=0&&slot<menu.slots.size()&&menu.getSlot(slot).container==p.getInventory();
        boolean fromHotbar=type==ContainerInput.SWAP&&button>=0&&button<p.getInventory().getContainerSize()&&locked(p,p.getInventory().getItem(button));
        boolean deny=switch(type){
            case THROW -> source;case CLONE -> source;case QUICK_MOVE -> source&&menu!=p.inventoryMenu;
            case PICKUP -> held&&!personal;
            case QUICK_CRAFT -> held&&AbstractContainerMenu.getQuickcraftHeader(button)==1&&!personal;
            case SWAP -> fromHotbar&&!personal;
            default -> false;
        };
        if(deny)refuse(p);return deny;
    }
    /** Creative uses slot-write packets rather than ordinary clicks. Move the real lamp, never a copy. */
    public boolean creativeSlot(ServerPlayer p,int slot,ItemStack incoming){
        var wish=wishes.get(p.getUUID());if(wish==null||!p.isCreative())return false;
        var menu=p.inventoryMenu;var at=slot>=0&&slot<menu.slots.size()?menu.getSlot(slot):null;
        if(locked(p,incoming)){
            var current=find(p,wish.token);
            if(current==null||at==null||at.container!=p.getInventory()||!at.mayPlace(incoming)||!ItemStack.matches(current.get(),incoming)){refuse(p);return true;}
            if(!locked(p,at.getItem())){
                var lamp=current.get();var displaced=at.getItem();current.set(displaced);at.setByPlayer(lamp);
            }
            p.containerMenu.sendAllDataToRemote();return true;
        }
        if(at!=null&&locked(p,at.getItem())){refuse(p);return true;}
        return false;
    }
    /** Last native drop gate also handles plugins/mechanics which extracted the stack before dropping it. */
    public boolean interceptDrop(ServerPlayer p,ItemStack stack){
        if(!locked(p,stack))return false;
        String token=ItemData.stringValue(stack,TOKEN,"");
        boolean inInventory=false;for(int i=0;i<p.getInventory().getContainerSize();i++)if(token.equals(ItemData.stringValue(p.getInventory().getItem(i),TOKEN,""))){inInventory=true;break;}
        if(!inInventory){
            if(token.equals(ItemData.stringValue(p.containerMenu.getCarried(),TOKEN,"")))p.containerMenu.setCarried(ItemStack.EMPTY);
            int free=p.getInventory().getFreeSlot();
            if(free>=0)p.getInventory().setItem(free,stack);
            else {var displaced=p.getInventory().getSelectedItem();p.getInventory().setItem(p.getInventory().getSelectedSlot(),stack);p.drop(displaced,false,Prediction.SERVER_ONLY);}
        }
        refuse(p);return true;
    }
    private void cancelWish(ServerPlayer p, boolean publish) {
        dialogs.remove(p.getUUID());var wish=wishes.remove(p.getUUID());
        if(wish!=null){wish.cancel();var at=find(p,wish.token);if(at!=null)mark(at.get(),"");mark(wish.lamp,"");if(publish)sync(p);}
        warnings.remove(p.getUUID());
    }

    public void beforeDeath(ServerPlayer p) {
        var wish = wishes.get(p.getUUID());
        if (wish == null || wish.owner != p) return;
        var location = find(p, wish.token);
        cancelWish(p, false);
        if (location == null) return;

        // The cursor is not part of native inventory death drops. Release the
        // reserved lamp exactly once from either location, already unmarked.
        ItemStack lamp = location.get();
        location.set(ItemStack.EMPTY);
        if (p.drop(lamp, false, Prediction.SERVER_ONLY) == null) location.set(lamp);
    }

    public void cancel(ServerPlayer p){
        cancelWish(p, true);
        runtime.forget(p);
    }
    public void tick(){
        runtime.tick();
        for(var wish:List.copyOf(wishes.values()))if(!livingOwner(wish.owner)||!game.items().currentEpoch(wish.epoch))cancel(wish.owner);
        // Old pending copies from disk/rollback are ordinary lamps, never a permanent inventory lock.
        if(Math.floorMod(game.server().getTickCount(),20)!=0)return;
        for(var p:game.server().getPlayerList().getPlayers()){
            for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(GenieItems.lamp(stack)&&ItemData.contains(stack,TOKEN)&&!locked(p,stack))mark(stack,"");}
            var cursor=p.containerMenu.getCarried();if(GenieItems.lamp(cursor)&&ItemData.contains(cursor,TOKEN)&&!locked(p,cursor))mark(cursor,"");
        }
    }
    public void cancelAll(){for(var wish:List.copyOf(wishes.values()))cancel(wish.owner);dialogs.clear();warnings.clear();}
    public void stop(){cancelAll();runtime.clear();}
    void activateApi(GenieScriptModel.Transport transport) {
        if (scriptModel == null) scriptModel = new GenieScriptModel(transport);
        else scriptModel.useTransport(transport);
    }
    @Override public void close(){
        api.close();stop();runtime.scripts().close();
        if(model!=null){model.close();model=null;}
        if(scriptModel!=null){scriptModel.close();scriptModel=null;}
    }
}
