package me.skywars2.game.genie;

import java.util.*;
import net.minecraft.util.Prediction;
import java.util.concurrent.CompletionException;
import me.skywars2.game.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
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
    private final GameController game;
    private final GenieRuntime runtime;
    private GenieModelClient model;
    private final Map<UUID,Dialog> dialogs=new HashMap<>();
    private final Map<UUID,Wish> wishes=new HashMap<>();
    private final ArrayDeque<TwistKind> recent=new ArrayDeque<>();
    private final Map<UUID,Integer> warnings=new HashMap<>();
    private record Dialog(String token,String epoch,ItemStack lamp){}
    private static final class Wish {
        final ServerPlayer owner;final String token,epoch;final ItemStack lamp;final GenieModelClient.Pending request;
        Wish(ServerPlayer owner,String token,String epoch,ItemStack lamp,GenieModelClient.Pending request){this.owner=owner;this.token=token;this.epoch=epoch;this.lamp=lamp;this.request=request;}
    }
    public GenieService(GameController game){this.game=game;runtime=new GenieRuntime(game);}
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
        var warning=new ActionButton(new CommonButtonData(text("genie.warning").withStyle(ChatFormatting.RED),Optional.of(text("genie.warning").withStyle(ChatFormatting.RED)),360),Optional.empty());
        var data=new CommonDialogData(text("genie.lamp"),Optional.empty(),true,false,DialogAction.CLOSE,List.of(),List.of(input));
        p.openDialog(Holder.direct(new MultiActionDialog(data,List.of(warning,send),Optional.empty(),1)));return true;
    }
    public void click(ServerPlayer p,Identifier id,Optional<Tag> payload){
        var session=dialogs.get(p.getUUID());if(session==null||!id.equals(Identifier.parse("skywars2:genie/"+session.token)))return;
        dialogs.remove(p.getUUID());
        if(!allowed(p)||!game.items().currentEpoch(session.epoch)||wishes.containsKey(p.getUUID())||!containsIdentity(p,session.lamp))return;
        String wish=payload.orElse(null) instanceof CompoundTag tag?tag.getString("wish").orElse(""):"";
        try{GenieBlueprint.plain(wish,2000);}catch(IllegalArgumentException invalid){tell(p,"genie.invalid_wish",ChatFormatting.RED);return;}
        if(model==null)model=GenieConfig.load();
        if(!model.configured()){tell(p,"genie.unavailable",ChatFormatting.RED);return;}
        // An overstack is legal server loot. Reserve precisely ONE lamp and preserve its siblings.
        if(session.lamp.getCount()>1){
            int free=p.getInventory().getFreeSlot();if(free<0){tell(p,"genie.need_slot",ChatFormatting.RED);return;}
            var rest=session.lamp.copyWithCount(session.lamp.getCount()-1);session.lamp.setCount(1);p.getInventory().setItem(free,rest);
        }
        String token=UUID.randomUUID().toString();mark(session.lamp,token);
        GenieModelClient.Pending request;
        try{request=model.request(wish,List.copyOf(recent));}catch(RuntimeException failed){mark(session.lamp,"");tell(p,"genie.unavailable",ChatFormatting.RED);return;}
        var pending=new Wish(p,token,session.epoch,session.lamp,request);wishes.put(p.getUUID(),pending);
        tell(p,"genie.thinking",ChatFormatting.GOLD);sync(p);
        request.future().whenComplete((reply,error)->game.server().execute(()->finish(pending,reply,error)));
    }
    private static void mark(ItemStack lamp,String token){
        if(lamp.isEmpty())return;var data=lamp.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(token.isEmpty())data.remove(TOKEN);else data.putString(TOKEN,token);
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
    private void finish(Wish wish,Reply reply,Throwable failure){
        if(wishes.get(wish.owner.getUUID())!=wish)return;
        if(!livingOwner(wish.owner)||!game.items().currentEpoch(wish.epoch)){cancel(wish.owner);return;}
        var location=find(wish.owner,wish.token);if(location==null){cancel(wish.owner);return;}
        ItemStack reward=ItemStack.EMPTY;
        if(failure==null&&reply!=null&&reply.granted())try{reward=GenieRuntime.construct(reply.item());}catch(RuntimeException invalid){failure=invalid;}
        wishes.remove(wish.owner.getUUID());
        if(failure!=null||reply==null){mark(location.get(),"");tell(wish.owner,"genie.failed",ChatFormatting.RED);sync(wish.owner);return;}
        if(!reply.granted()){
            location.set(ItemStack.EMPTY);game.items().kfcFood().poopPockets(wish.owner,10);
            tell(wish.owner,"genie.rejected",ChatFormatting.GOLD);
        }else {
            location.set(reward);recent.addLast(reply.item().twist().kind());while(recent.size()>12)recent.removeFirst();
            wish.owner.sendSystemMessage(Component.translatable("genie.answer",reply.answer()).withStyle(ChatFormatting.GREEN));
        }
        sync(wish.owner);
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
    public void cancel(ServerPlayer p){
        dialogs.remove(p.getUUID());var wish=wishes.remove(p.getUUID());
        if(wish!=null){wish.request.close();var at=find(p,wish.token);if(at!=null)mark(at.get(),"");mark(wish.lamp,"");sync(p);}
        warnings.remove(p.getUUID());runtime.forget(p);
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
    @Override public void close(){stop();runtime.scripts().close();if(model!=null){model.close();model=null;}}
}
