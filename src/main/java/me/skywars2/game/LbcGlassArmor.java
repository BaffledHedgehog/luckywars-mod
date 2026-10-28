package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ServerExplosion;

/** Physical glass wear and shards share the actual native hurt-player criterion. */
public final class LbcGlassArmor {
    static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private final GameController game;
    private final Predicate<ServerPlayer> admitted;
    private record Attempt(ServerPlayer player,boolean admitted){}
    private final Deque<Attempt> attempts=new ArrayDeque<>();
    /** Keep the ordinary ARROW damage type while distinguishing physical glass shards from other arrows. */
    static final class ShardDamage extends DamageSource {
        ShardDamage(ServerPlayer player){super(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),player);}
    }

    LbcGlassArmor(GameController game,Predicate<ServerPlayer> admitted){this.game=game;this.admitted=admitted;}
    static boolean piece(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("glass",0)==1;}

    /** A nonsingular source data-get fails when zero or multiple armor entries match. */
    static double armor(ItemStack stack){
        double value=0;int matches=0;
        for(var entry:stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers())
            if(entry.attribute().equals(Attributes.ARMOR)){value=entry.modifier().amount();matches++;}
        return matches==1?value:0;
    }
    static double decayed(double value){return (int)Math.floor(value*1000)*.00095;}
    /** Preserve source integer shard calculation, then soften intact glass by its actual mean armor. */
    static float shardDamage(double... armor){
        if(armor.length==0)return 0;
        int scaled=0;double total=0;
        for(double value:armor){scaled+=(int)Math.floor(value*.17);total+=value;}
        int raw=4*armor.length-Math.floorDiv(scaled,armor.length);
        if(raw<=0)return 0;
        double mean=total/armor.length;
        return (float)(raw*(mean>2?2/mean:1));
    }
    private static void decay(ItemStack stack){
        double next=decayed(armor(stack));
        var modifiers=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        var changed=new ArrayList<ItemAttributeModifiers.Entry>(modifiers.modifiers().size());
        for(var entry:modifiers.modifiers()){
            boolean glassAttribute=entry.attribute().equals(Attributes.ARMOR)||entry.attribute().equals(Attributes.ARMOR_TOUGHNESS);
            var old=entry.modifier();
            changed.add(glassAttribute?new ItemAttributeModifiers.Entry(entry.attribute(),
                    new AttributeModifier(old.id(),next,old.operation()),entry.slot(),entry.display()):entry);
        }
        // Author: wear only changes these attributes, never repairs damage or removes player-added components.
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(changed));
    }
    public boolean withIncomingAttempt(ServerPlayer player,BooleanSupplier hurt){
        attempts.push(new Attempt(player,admitted.test(player)));
        try{return hurt.getAsBoolean();}finally{attempts.pop();}
    }
    public void incoming(ServerPlayer player,DamageSource source){
        // Author: a shard impact must never wear more glass or release a secondary shard burst.
        if(!(LbcIndirectKills.originalDamage(source) instanceof ShardDamage))incoming(player);
    }
    public void incoming(ServerPlayer player){
        var attempt=attempts.peek();
        if(!(attempt!=null&&attempt.player()==player?attempt.admitted():admitted.test(player)))return;
        var pieces=new ArrayList<ItemStack>(4);
        for(var slot:SLOTS){var stack=player.getItemBySlot(slot);if(piece(stack)){decay(stack);pieces.add(stack);}}
        if(pieces.isEmpty())return;
        player.getInventory().setChanged();
        float damage=shardDamage(pieces.stream().mapToDouble(LbcGlassArmor::armor).toArray());
        if(damage<=0)return;
        var origin=player.position();var center=player.getBoundingBox().getCenter();
        var epoch=game.items().epoch();
        // Tiny broad-phase margin retains exact distance7 at positive vertical boundaries.
        var hits=player.level().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(7.0000001),
                target->target!=player&&game.items().wands().target(player,target)
                        &&target.position().distanceToSqr(origin)>=.000001&&target.position().distanceToSqr(origin)<=49);
        var arrow=new ShardDamage(player);
        for(var target:hits){
            if(!game.items().currentEpoch(epoch)||game.stage()==GameController.Stage.ENDING)return;
            if(!game.items().wands().target(player,target))continue;
            // Shared vanilla collision-shape sampling lets cover stop shards, including partial cover.
            float exposure=ServerExplosion.getSeenPercent(center,target);
            if(exposure>0)target.hurtServer(player.level(),arrow,damage*exposure);
        }
    }
}
