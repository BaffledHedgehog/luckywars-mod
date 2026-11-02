package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import java.util.List;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A release/packet-boundary check: deliberately never ticks a huge velocity through native collision traversal. */
public final class RiptideVelocityChecks {
    private RiptideVelocityChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Riptide velocity: "+why);}
    @SuppressWarnings("unchecked")
    private static net.minecraft.network.syncher.EntityDataAccessor<Byte> livingFlags(){
        try{var field=net.minecraft.world.entity.LivingEntity.class.getDeclaredField("DATA_LIVING_ENTITY_FLAGS");field.setAccessible(true);
            return (net.minecraft.network.syncher.EntityDataAccessor<Byte>)field.get(null);
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    public static void run(GameController game){
        try(var f=new ElementalGrimoireChecks.Fixture(game,true)){
            f.initialize();var player=f.owner;var level=player.level();var cell=BlockPos.containing(player.position());
            var saved=game.maps().snapshot(level,cell);var provider=(PacketContextProvider)f.recording;
            var runtime=new MatchEntityRuntime(player);var flagKey=livingFlags();byte flags=player.getEntityData().get(flagKey);
            var combat=(me.skywars2.mixin.CombatCheckpointAccess.Living)(Object)player;var spin= combat.skywars2$spin();
            try{
                level.setBlock(cell,Blocks.WATER.defaultBlockState(),2|16|32);player.doTick();
                require(player.isInWaterOrRain(),"real native wet admission");player.setOnGround(false);
                var riptide=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.RIPTIDE);
                for(int value:List.of(10,255,10000,Integer.MAX_VALUE)){
                    player.setDeltaMovement(new Vec3(.25,.5,-.25));
                    var stack=new ItemStack(Items.TRIDENT);var enchantments=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    enchantments.set(riptide,value);stack.set(DataComponents.ENCHANTMENTS,enchantments.toImmutable());
                    var before=player.getDeltaMovement();double strength=EnchantmentHelper.getTridentSpinAttackStrength(stack,player);
                    require(strength>0,"actual native positive strength");
                    PacketContext.runWithContext(provider,()->{
                        var client=PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());
                        require(client.get(DataComponents.ENCHANTMENTS).getLevel(riptide)==Math.min(value,255),"actual recipient client constructor-safe level");
                        require(stack.get(DataComponents.ENCHANTMENTS).getLevel(riptide)==value,"true server enchantment level retained");
                    });
                    f.recording.packets.clear();
                    require(stack.getItem().releaseUsing(stack,level,player,71990),"actual native charged release");
                    var packets=f.recording.packets.stream().filter(ClientboundSetEntityMotionPacket.class::isInstance)
                            .map(ClientboundSetEntityMotionPacket.class::cast).filter(packet->packet.id()==player.getId()).toList();
                    require(packets.size()==1,"one replacement motion packet per release");
                    var actual=player.getDeltaMovement();require(packets.getFirst().movement().equals(actual),"packet contains final authoritative velocity");
                    var buffer=io.netty.buffer.Unpooled.buffer();
                    try{
                        ClientboundSetEntityMotionPacket.STREAM_CODEC.encode(buffer,packets.getFirst());
                        var decoded=ClientboundSetEntityMotionPacket.STREAM_CODEC.decode(buffer);
                        require(decoded.movement().distanceTo(actual)<=Math.max(.0001,actual.length()*.0002),"actual26.3 LP motion wire retains huge velocity within quantization");
                        if(value>=255)require(decoded.movement().length()>100,"wire has no legacy3.9 velocity cap");
                    }finally{buffer.release();}
                    double impulse=actual.subtract(before).length();
                    require(Math.abs(impulse-strength)<=Math.max(.00001,strength*.000001),"full native impulse, without cap or double addition");
                    // Native collision work at INT_MAX is outside this packet-boundary qualification.
                    player.setDeltaMovement(Vec3.ZERO);
                }
            }finally{player.setDeltaMovement(Vec3.ZERO);runtime.restore(player);combat.skywars2$spin(spin);
                player.getEntityData().set(flagKey,flags);game.maps().restoreSnapshot(level,cell,saved);}
        }
        SkyWars2.LOGGER.info("[check] PASS riptide-velocity (actual native release10/255/10000/INT_MAX, true server level/client255 projection, exact final motionpacket/no double impulse; no huge-speed collision tick claim)");
    }
}
