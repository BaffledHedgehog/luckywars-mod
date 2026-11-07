package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.*;
import me.skywars2.mixin.ClockFragmentSprintAccess;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Whole-world instant simulation requested by the author; Minecraft owns every actual tick. */
public final class LbcClockFragments {
    public static final String ID="skywars2:clock_fragment";
    public static final Map<String,Integer> DURATIONS;
    private static final Map<ServerTickRateManager,LbcClockFragments> OWNERS=new WeakHashMap<>();
    static {
        var values=new LinkedHashMap<String,Integer>();
        values.put("1t",1);values.put("2t",2);values.put("5t",5);values.put("10t",10);
        values.put("1s",20);values.put("2s",40);values.put("3s",60);values.put("4s",80);
        values.put("5s",100);values.put("10s",200);values.put("15s",300);
        // The authored selector named30s calls tick90s. Preserve its actual mapping.
        values.put("30s",1800);
        values.put("jump_1s",20);values.put("jump_2s",40);values.put("jump_5s",100);values.put("jump_10s",200);
        DURATIONS=Collections.unmodifiableMap(values);
    }
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,Identifier.parse(ID),new Fragment());
    private static final class Fragment extends Item implements PolymerItem {
        Fragment(){super(properties());}
        private static Properties properties(){
            var data=new CompoundTag();data.putInt("transformable",1);data.putInt("accessory",1);
            data.putInt("magicwand",1);data.putInt("clocks",1);
            // No selector on the bare registry default: only authored profiles are usable.
            return new Properties().setId(ResourceKey.create(Registries.ITEM,Identifier.parse(ID))).durability(25)
                    .component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                    .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:time_clock"))
                    .component(DataComponents.CUSTOM_NAME,Component.translatable("time_fragment").withStyle(s->s.withItalic(false)));
        }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.CARROT_ON_A_STICK;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var game=me.skywars2.SkyWars2.game();
            var owner=game==null?null:OWNERS.get(game.server().tickRateManager());
            return player instanceof ServerPlayer server&&owner!=null&&owner.use(server,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
    }
    public static void register(){}
    public static int duration(ItemStack stack){return ItemData.intValue(stack,"clocks",0)==1
            ?DURATIONS.getOrDefault(ItemData.stringValue(stack,"type",""),0):0;}
    private record Request(String epoch,int ticks){}
    private final GameController game;
    private final ArrayDeque<Request> pending=new ArrayDeque<>();
    private boolean owned,requesting,finishing;
    private String epoch;
    private int started,ticks;
    public LbcClockFragments(GameController game){this.game=game;var manager=game.server().tickRateManager();
        var previous=OWNERS.get(manager);if(previous!=null)previous.stop();OWNERS.put(manager,this);}
    public int pendingCount(){return pending.size();}
    public boolean running(){return owned;}
    public int requestedTicks(){return ticks;}
    public int startedAt(){return started;}
    public boolean use(ServerPlayer player,InteractionHand hand){
        var manager=game.server().tickRateManager();var stack=player.getItemInHand(hand);int duration=duration(stack);
        if(duration==0||ItemData.intValue(stack,"magicwand",0)!=1
                ||LbcTmtItems.matches(stack)&&!LbcTmtAbilities.active(stack)
                ||!game.server().isSameThread()||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||!game.canUseGameplayItems(player)||!LbcDimensionItems.ordinary(player)||!game.items().wands().validRoleTarget(player,false)||!player.isAlive()||player.isRemoved()||player.isSpectator()
                ||game.isObserver(player)||game.awaitingRespawn(player.getUUID())||game.items().actionsBlocked(player)
                ||game.items().wands().inputTimeStopped(player)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||game.items().magic().suppressed(player)||game.maps().busy()||game.vanity().busy()
                ||manager.isFrozen()||manager.isSteppingForward()||manager.isSprinting()&&!owned)return false;
        pending.addLast(new Request(game.items().epoch(),duration));return true;
    }
    /** Call once at the real END_SERVER_TICK, AFTER match, visuals and Vanity maintenance. */
    public void endTick(){
        var manager=game.server().tickRateManager();
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||game.maps().busy()
                ||game.vanity().busy()||owned&&!game.items().currentEpoch(epoch)){stop();return;}
        if(owned){if(manager.isSprinting())return;owned=false;epoch=null;}
        if(pending.isEmpty()||manager.isSprinting()||manager.isFrozen()||manager.isSteppingForward())return;
        var request=pending.removeFirst();if(!game.items().currentEpoch(request.epoch())){pending.clear();return;}
        epoch=request.epoch();ticks=request.ticks();started=game.server().getTickCount();owned=true;requesting=true;
        try{manager.requestGameToSprint(ticks);}catch(Throwable error){owned=false;epoch=null;throw error;}
        finally{requesting=false;}
    }
    /** Freeze/foreign sprint mixins run BEFORE that native owner changes scheduler state. */
    public static void beforeFreeze(ServerTickRateManager manager,boolean frozen){
        var owner=OWNERS.get(manager);if(frozen&&owner!=null&&!owner.finishing)owner.stop();
    }
    public static void beforeSprint(ServerTickRateManager manager){
        var owner=OWNERS.get(manager);if(owner!=null&&!owner.requesting)owner.stop();
    }
    /** Also settles vanilla's remaining0/final-iteration state; never touches a foreign sprint. */
    public void stop(){
        pending.clear();var manager=game.server().tickRateManager();
        if(owned&&manager.isSprinting()){
            finishing=true;
            try{((ClockFragmentSprintAccess)manager).skywars2$finishClockSprint();}
            finally{finishing=false;owned=false;epoch=null;}
        }else{owned=false;epoch=null;}
    }
}
