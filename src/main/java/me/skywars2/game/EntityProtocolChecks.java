package me.skywars2.game;

import com.google.gson.JsonParser;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;

/** Wire IDs are checked against an independent vanilla26.3 ordering, not the server's own decode table. */
public final class EntityProtocolChecks {
    private EntityProtocolChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Entity protocol: "+why);}
    private static List<Identifier> vanilla(){
        try(var input=EntityProtocolChecks.class.getResourceAsStream("/skywars2/vanilla_entity_order_26_3.json")){
            if(input==null)throw new IllegalStateException("Missing vanilla entity ID oracle");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var result=new java.util.ArrayList<Identifier>();root.getAsJsonArray("entities").forEach(e->result.add(Identifier.parse(e.getAsString())));return List.copyOf(result);
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public static void run(GameController game){
        var order=vanilla();require(order.size()==161,"complete vanilla26.3 entity oracle");
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            var provider=(PacketContextProvider)(Object)f.client.connection;
            PacketContext.runWithContext(provider,()->{
                // Text first reproduces the reported display-scale-on-tadpole failure on the old archive.
                for(var type:List.of(EntityTypes.TEXT_DISPLAY,EntityTypes.ITEM_DISPLAY,EntityTypes.BLOCK_DISPLAY)){
                    var display=(Display)type.create(f.level,EntitySpawnReason.COMMAND);require(display!=null,"native display constructor");
                    display.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f(.5F),new org.joml.Quaternionf()));
                    var raw=Unpooled.buffer();
                    try{
                        var buffer=new RegistryFriendlyByteBuf(raw,f.owner.registryAccess());
                        var spawn=new ClientboundAddEntityPacket(display.getId(),display.getUUID(),4.5,201,4.5,0,0,type,0,Vec3.ZERO,0);
                        ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer,spawn);
                        require(buffer.readVarInt()==display.getId(),"wire entity ID");buffer.readUUID();int id=buffer.readVarInt();
                        require(id>=0&&id<order.size(),"vanilla client can resolve raw entity type");
                        var decodedId=order.get(id);var decodedType=BuiltInRegistries.ENTITY_TYPE.getValue(decodedId);
                        me.skywars2.SkyWars2.LOGGER.info("[check] entity wire source={} raw={} vanillaClient={}",BuiltInRegistries.ENTITY_TYPE.getKey(type),id,decodedId);
                        buffer.clear();
                        var metadata=new ClientboundSetEntityDataPacket(display.getId(),display.getEntityData().getNonDefaultValues());
                        ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,metadata);
                        var receivedMetadata=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                        var clientEntity=decodedType.create(f.level,EntitySpawnReason.COMMAND);require(clientEntity!=null,"canonical client entity constructor");
                        clientEntity.getEntityData().assignValues(receivedMetadata.packedItems());
                        require(decodedType==type,"wire resolves the intended display type");
                    }finally{raw.release();}
                }
                for(int id=0;id<order.size();id++){
                    var type=BuiltInRegistries.ENTITY_TYPE.getValue(order.get(id));
                    require(type!=null&&BuiltInRegistries.ENTITY_TYPE.getId(type)==id,"vanilla raw ID retained "+order.get(id));
                    require(!RegistrySyncUtils.isServerEntry(BuiltInRegistries.ENTITY_TYPE,type),"vanilla entity is never server-only "+order.get(id));
                }
                var ordinary=new AreaEffectCloud(f.level,0,200,0);var chemical=new AreaEffectCloud(f.level,0,200,0);
                chemical.setPotionContents(new PotionContents(java.util.Optional.empty(),java.util.Optional.empty(),List.of(new MobEffectInstance(MobEffects.RAID_OMEN,40,234)),java.util.Optional.empty()));
                require(PolymerEntity.get(ordinary)!=null&&PolymerEntity.get(chemical)!=null,"vanilla cloud per-instance constructor remains registered");
                require(PolymerEntity.get(ordinary).getPolymerEntityType(PacketContext.get())==EntityTypes.AREA_EFFECT_CLOUD,"ordinary clouds retain native visuals");
                require(PolymerEntity.get(chemical).getPolymerEntityType(PacketContext.get())==EntityTypes.MARKER,"chemical clouds retain the hidden authored visual");
            });
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS entity-protocol (161 independent vanilla raw IDs; native spawn/metadata wire decoded as vanilla, three displays, ordinary/chemical per-instance cloud projections)");
    }
}
