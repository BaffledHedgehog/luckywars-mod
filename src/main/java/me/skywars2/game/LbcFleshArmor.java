package me.skywars2.game;

import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Physical set multiplier; native attributes own health clamping, saving and synchronization. */
public final class LbcFleshArmor {
    static final Identifier BONUS=Identifier.parse("minecraft:44a8aa48-d53d-42c0-b229-4557d3c581b1");
    private static final AttributeModifier MODIFIER=new AttributeModifier(BONUS,1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private final GameController game;
    private final Predicate<ServerPlayer> eligible;

    LbcFleshArmor(GameController game,Predicate<ServerPlayer> eligible){this.game=game;this.eligible=eligible;}
    static boolean full(ServerPlayer player){
        for(var slot:SLOTS){var stack=player.getItemBySlot(slot);if(stack.isEmpty()||LbcFartus.data(stack).getIntOr("flesh",0)!=1)return false;}
        return true;
    }
    private static void bonus(ServerPlayer player,boolean wanted){
        var health=player.getAttribute(Attributes.MAX_HEALTH);
        if(health==null)return;
        var old=health.getModifier(BONUS);
        if(!wanted){if(old!=null)health.removeModifier(BONUS);return;}
        if(old!=null&&old.amount()==MODIFIER.amount()&&old.operation()==MODIFIER.operation()
                &&health.getPermanentModifiers().contains(old))return;
        if(old==null){health.addPermanentModifier(MODIFIER);return;}
        // Repair the legacy x3 atomically: remove/add on the live attribute would briefly
        // shrink its maximum to the bare set and permanently discard otherwise valid HP.
        var corrected=new AttributeInstance(health.getAttribute(),ignored->{});
        corrected.replaceFrom(health);corrected.removeModifier(BONUS);corrected.addPermanentModifier(MODIFIER);
        health.replaceFrom(corrected);
    }
    public void refresh(ServerPlayer player){bonus(player,eligible.test(player)&&full(player));}
    public void inventoryChanged(ServerPlayer player){refresh(player);}
    public void loaded(Entity entity){if(entity instanceof ServerPlayer player)refresh(player);}
    public void tick(){for(var player:game.server().getPlayerList().getPlayers())refresh(player);}
    /** Rebuild the physical saved-equipment maximum before native setHealth reads saved HP. */
    public static void prepareLoadedEquipment(ServerPlayer player){bonus(player,full(player));}
    /** The source modifier is saved. Disconnect must not reduce max/current HP before saving. */
    public void forget(ServerPlayer player){}
    public void stop(){for(var player:game.server().getPlayerList().getPlayers())bonus(player,false);}
}
