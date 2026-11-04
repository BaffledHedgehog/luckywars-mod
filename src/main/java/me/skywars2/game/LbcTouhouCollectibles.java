package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;

/** Inventory events feed one bounded inventory pass; no world, age or tick-NBT selector. */
public final class LbcTouhouCollectibles {
    public record Counters(int power,int timeOrbs,int bombFragments,int lifeFragments) { }
    private static final int[] THRESHOLDS={10,25,40,55,70,85,100,115,130,150};
    private static final double[] AMOUNTS={1,.2,1.5,.2,1,.2,1.5,.2,1,2};
    private final GameController game;
    private final Predicate<ServerPlayer> admission;
    private final Map<UUID,Counters> states=new HashMap<>();
    private final LinkedHashSet<UUID> dirty=new LinkedHashSet<>();
    private boolean draining,restoring;
    public LbcTouhouCollectibles(GameController game,Predicate<ServerPlayer> admission){
        this.game=game;this.admission=Objects.requireNonNull(admission);
    }
    public static int id(ItemStack stack){return stack.isEmpty()?0:ItemData.intValue(stack,"touhou_item",0);}
    public static boolean consumable(ItemStack stack){return ItemData.intValue(stack,"bomb_item",0)==1||ItemData.intValue(stack,"time_stopper",0)==1;}
    public void inventoryChanged(ServerPlayer player){if(!draining&&!restoring)dirty.add(player.getUUID());}
    public Counters counters(ServerPlayer player){return states.getOrDefault(player.getUUID(),new Counters(0,0,0,0));}
    private boolean paused(ServerPlayer player){return game.items().timeStop().stopped(player.level())||game.items().timeStop().frozen(player)||game.items().wands().inputTimeStopped(player);}
    public void tick(){
        if(restoring||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        int visits=Math.min(32,dirty.size());
        while(visits-->0){
            var uuid=dirty.removeFirst();
            var player=game.server().getPlayerList().getPlayer(uuid);
            if(player==null)continue;
            if(paused(player)||!admission.test(player)){dirty.add(uuid);continue;}
            intake(player);
        }
    }
    private void intake(ServerPlayer player){
        long[] counts=new long[11];
        draining=true;
        try{
            var inventory=player.getInventory();
            for(int slot=0;slot<inventory.getContainerSize();slot++){
                var stack=inventory.getItem(slot);int type=id(stack);
                if(type>=1&&type<=10){counts[type]+=stack.getCount();inventory.setItem(slot,ItemStack.EMPTY);}
            }
            if(Arrays.stream(counts).sum()==0)return;
            var old=counters(player);
            int power=(int)Math.min(150,(long)old.power+counts[1]+10*counts[2]);
            if(counts[3]>0)power=150;
            int orbs=add(old.timeOrbs,counts[6]),bombs=add(old.bombFragments,counts[7]),lives=add(old.lifeFragments,counts[8]);
            // Source performs each threshold once per select. Time orbs only check on new orbs.
            if(counts[6]>0&&orbs>=25){orbs-=25;game.lbc().grantTable(player,"lbc:time_stopper");}
            if(bombs>=5){bombs-=5;game.lbc().grantTable(player,"lbc:bomb_item");}
            int gainedLives=add(0,counts[5]);
            if(lives>=10){lives-=10;gainedLives=add(gainedLives,1);}
            var value=new Counters(power,orbs,bombs,lives);states.put(player.getUUID(),value);publish(player,value);modifiers(player,power);
            if(old.power<150&&power==150)player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable("full_power_mode").withStyle(net.minecraft.ChatFormatting.GOLD,net.minecraft.ChatFormatting.BOLD)));
            if(counts[6]>0){player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(" ")));player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(Integer.toString(orbs)).withStyle(net.minecraft.ChatFormatting.AQUA)));}
            var mana=game.items().mana();
            if(mana.get(player)<mana.maximum(player)){
                mana.set(player,(int)Math.min(mana.maximum(player),(long)mana.get(player)+100*counts[4]));
                LbcOverload.sound(player,"minecraft:item.honey_bottle.drink",.5f,2,0);
            }
            if(gainedLives>0){game.addRuneLives(player,gainedLives);player.sendSystemMessage(Component.empty().append(Component.translatable("you_gain").withColor(0xffbfff)).append(Integer.toString(gainedLives)).append(Component.translatable("lives")));}
            if(counts[9]>0)mana.baseRegen(player,add(mana.baseRegen(player),counts[9]));
            if(counts[10]>0){
                int coins=add(0,2*counts[10]);var profile=game.players().get(player.getUUID());profile.coins=add(profile.coins,coins);
                LbcOverload.sound(player,"minecraft:lbcsounds.coin",1,1,0);
                player.sendSystemMessage(Component.literal("+"+coins).withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.translatable("coins_added")));
            }
            inventory.setChanged();player.containerMenu.broadcastChanges();
        }finally{draining=false;}
    }
    private static int add(int value,long amount){return (int)Math.clamp((long)value+amount,Integer.MIN_VALUE,Integer.MAX_VALUE);}
    private void publish(ServerPlayer p,Counters value){var mana=game.items().mana();mana.score(p,"touhou_power").set(value.power);mana.score(p,"time_orbs").set(value.timeOrbs);mana.score(p,"bomb_fragments").set(value.bombFragments);mana.score(p,"life_fragments").set(value.lifeFragments);}
    private static Holder<Attribute> attribute(int index){return index==1||index==3||index==5||index==7?Attributes.MAX_HEALTH:Attributes.ATTACK_DAMAGE;}
    private static Identifier modifier(int index){return Identifier.parse("minecraft:touhou_power"+(index+1));}
    private static void modifiers(ServerPlayer player,int power){
        for(int n=0;n<10;n++){
            var attribute=player.getAttribute(attribute(n));if(attribute==null)continue;
            var id=modifier(n);attribute.removeModifier(id);
            if(power>=THRESHOLDS[n])attribute.addTransientModifier(new AttributeModifier(id,AMOUNTS[n],n==1||n==3||n==5||n==7?AttributeModifier.Operation.ADD_MULTIPLIED_BASE:AttributeModifier.Operation.ADD_VALUE));
        }
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
    }
    /** Used by the ordinary input owner before vanilla egg/snowball publication. */
    public boolean use(ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!consumable(stack)||!admission.test(player)||game.items().timeStop().frozen(player)||game.items().wands().inputTimeStopped(player)||game.items().actionsBlocked(player))return false;
        if(hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        boolean bomb=ItemData.intValue(stack,"bomb_item",0)==1;
        boolean accepted=bomb?game.items().danmaku().useTouhouBomb(player):game.items().timeStop().useTouhouClock(player);
        if(accepted){stack.shrink(1);player.getInventory().setChanged();player.containerMenu.broadcastChanges();}
        return accepted;
    }
    public void forget(ServerPlayer player){
        if(restoring||MatchPlayerCheckpoint.restoring(player))return;
        dirty.remove(player.getUUID());states.remove(player.getUUID());modifiers(player,0);publish(player,new Counters(0,0,0,0));
    }
    public void stop(){for(var p:game.server().getPlayerList().getPlayers())forget(p);dirty.clear();states.clear();}
    public void prepareFrozen(){restoring=true;dirty.clear();states.clear();}
    public void publishFrozen(){restoring=false;}
    Checkpoint checkpoint(){if(draining)throw new IllegalStateException("Touhou checkpoint during inventory drain");return new Checkpoint();}
    final class Checkpoint{
        private final Map<UUID,Counters> saved=Map.copyOf(states);
        private final List<UUID> pending=List.copyOf(dirty);
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){return new MatchFieldCheckpoint.Plan(Set.of(),()->{states.clear();states.putAll(saved);dirty.clear();dirty.addAll(pending);saved.forEach((id,value)->{if(lookup.apply(id) instanceof ServerPlayer player){publish(player,value);modifiers(player,value.power);}});});}
    }
}
