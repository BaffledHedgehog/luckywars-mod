package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** The existing bucket fixture verifies the shared equipment transaction with native pumpkin input. */
public final class ThrownPumpkinChecks {
    private ThrownPumpkinChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Thrown pumpkin: "+why);}
    private static Object read(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(value);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}throw new IllegalStateException(name);}
    private static List<ClientboundSoundPacket> sounds(RegenerationBeltChecks.ScheduledClient client){return ((List<?>)read(client.connection,"packets")).stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(packet->{var id=packet.getSound().value().location().toString();return id.equals("minecraft:lbcsounds.yametekudasai")||id.equals("minecraft:block.decorated_pot.insert");}).toList();}
    private static ItemStack presentation(WoolBucketChecks.Fixture f){
        var head=f.loot("lbc:imba/pumpkin_head");require(head.is(Items.CARVED_PUMPKIN)&&ItemData.intValue(head,"helmet",0)==1&&head.has(DataComponents.EQUIPPABLE),"source head modifiers actually run: "+head);
        var actual=f.leaf("throwable_pumpkin");require(actual.is(LbcThrownUtilityItems.THROWABLE_PUMPKIN)&&actual.getCount()==1,"canonical public native identity/count");
        require(actual.getComponents().equals(actual.getItem().getDefaultInstance().getComponents()),"give and canonical properties agree");
        try(var input=ThrownPumpkinChecks.class.getResourceAsStream("/data/lbc/loot_table/item_defs/minecraft/jack_o_lantern__33966c7d363a.json")){
            var original=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input),StandardCharsets.UTF_8)).getAsJsonObject();
            original.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name","minecraft:snowball");
            var ops=f.game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.home).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);
            var expected=LootTable.DIRECT_CODEC.parse(ops,original).getOrThrow().getRandomItems(params,17);
            require(expected.size()==1&&ItemStack.matches(NativeFireballItemChecks.comparable(actual),expected.getFirst()),"independent original-carrier all-component/count oracle");
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)f.owner.connection;
            var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(actual,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());
            require(ItemStack.matches(actual,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),f.owner.registryAccess())),"all-component Polymer roundtrip");
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
        return actual;
    }
    private static List<ItemEntity> drops(WoolBucketChecks.Fixture f,Set<UUID> before){var result=new ArrayList<ItemEntity>();for(var actor:f.level.getAllEntities())if(actor instanceof ItemEntity item&&!item.isRemoved()&&!before.contains(item.getUUID())){f.own(item);result.add(item);}return result;}
    private static void hit(WoolBucketChecks.Fixture f,ItemStack prototype,InteractionHand hand,boolean protectedInventory){
        var old=f.helmet();var expected=old.copy();f.other.setItemSlot(EquipmentSlot.HEAD,old);f.readyDamage();
        var profile=f.game.players().get(f.other.getUUID());int previous=profile.lbcSkill;f.game.config().lbcSkills=protectedInventory;profile.lbcSkill=protectedInventory?2:0;
        try{
            require(f.game.items().inventoryHazards().protectedInventory(f.other)==protectedInventory,"actual inventory protection control");
            var shot=f.launch(prototype,hand);f.park(shot,f.other.position().add(0,-1,0));
            require(f.thrown.state(shot).kind()==LbcThrownUtilities.Kind.PUMPKIN,"typed source kind before payload");
            var before=f.itemIds();int heard=sounds(f.audience).size();float hp=f.other.getHealth();
            f.thrown.beforeTick(shot);require(shot.isRemoved()&&f.thrown.state(shot)==null,"one terminal payload claim");
            require(f.other.getHealth()==hp-5&&f.other.getLastDamageSource().is(DamageTypes.CACTUS)&&f.other.getLastDamageSource().getEntity()==f.owner,"source cactus5 through native pipeline with actual attacker");
            var emitted=drops(f,before);
            if(protectedInventory){
                require(f.other.getItemBySlot(EquipmentSlot.HEAD)==old&&emitted.isEmpty(),"Protected Inventory preserves occupied slot and publishes no copy");
                expected.setDamageValue(old.getDamageValue());require(ItemStack.matches(expected,old),"all protected properties survive ordinary native armor wear");
            }else{
                var head=f.other.getItemBySlot(EquipmentSlot.HEAD);require(head.is(Items.CARVED_PUMPKIN)&&head.getCount()==1,"source carved-pumpkin head: "+head);
                require(head.get(DataComponents.EQUIPPABLE).cameraOverlay().orElseThrow().equals(Identifier.parse("minecraft:misc/pumpkinblur")),"vanilla pumpkin mask");
                require(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE),head)==1,"source binding curse1 retained");
                require(emitted.size()==1&&ItemStack.matches(emitted.getFirst().getItem(),expected)&&emitted.getFirst().getItem()!=old,"exact nested original helmet is ejected once");
                require((Integer)read(emitted.getFirst(),"pickupDelay")==0,"old helmet can be picked up immediately");
            }
            var heardNow=sounds(f.audience);require(heardNow.size()==heard+5,"source three custom sounds plus two decorated-pot sounds");
            var batch=heardNow.subList(heard,heard+5);require(batch.get(0).getPitch()==1&&batch.get(1).getPitch()==0&&batch.get(2).getPitch()==2&&batch.get(3).getPitch()==1&&batch.get(4).getPitch()==2,"source sound pitches retained");
            f.thrown.beforeTick(shot);f.thrown.impact(shot);require(f.other.getHealth()==hp-5&&sounds(f.audience).size()==heard+5,"no duplicate equip/damage/sounds after terminal callback");
            for(var item:emitted)f.discard(item);
        }finally{profile.lbcSkill=previous;f.game.config().lbcSkills=false;}
    }
    private static void lifecycle(WoolBucketChecks.Fixture f,ItemStack prototype){
        var shot=f.launch(prototype,InteractionHand.MAIN_HAND);f.park(shot,f.home.add(0,17,0));var accepted=f.thrown.state(shot);
        f.pause();try{
            f.park(shot,f.other.position().add(0,-1,0));var head=f.other.getItemBySlot(EquipmentSlot.HEAD);float hp=f.other.getHealth();
            f.thrown.beforeTick(shot);require(!shot.isRemoved()&&f.other.getItemBySlot(EquipmentSlot.HEAD)==head&&f.other.getHealth()==hp,"TimeStop holds pending payload");
            f.thrown.unloaded(shot);f.thrown.rebind(shot);require(accepted.equals(f.thrown.state(shot)),"persistent kind/author/epoch/claim rebind");
        }finally{f.thaw();}
        f.park(shot,f.home.add(0,17,0));require(f.game.items().cloneSkills().copyNow(shot,15),"canonical x2 accepts native pumpkin");
        var pair=f.thrown.actors();require(pair.size()==2,"two independent native projectile roots");for(var body:pair)require(f.game.items().cloneSkills().copyNow(body,17),"both x2 roots accept x4");
        require(f.thrown.count()==8,"x2*x4 gives eight owned projectile claims");for(var body:List.copyOf(f.thrown.actors())){f.own(body);require(body.getOwner()==f.owner&&f.thrown.state(body).kind()==LbcThrownUtilities.Kind.PUMPKIN,"clones retain actual author and pumpkin state");f.discard(body);}
        require(f.thrown.count()==0,"no projectile state leaks after cleanup");
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var prototype=presentation(f);
            for(var hand:InteractionHand.values())hit(f,prototype,hand,false);
            f.field(false,f.home.add(6,0,0));f.field(true,f.home.add(7,0,0));
            try{require(game.items().magic().suppressed(f.owner)&&game.items().magic().mechanismSuppressedAt(f.level,f.owner.position(),20),"real MAG and TECH cover native launch");hit(f,prototype,InteractionHand.OFF_HAND,false);hit(f,prototype,InteractionHand.MAIN_HAND,true);}finally{f.removeFields();}
            lifecycle(f,prototype);
        }
        SkyWars2.LOGGER.info("[check] PASS throwable-pumpkin (canonical source/default/Polymer, both native hands/debit, cactus5/actual owner, shared exact helmet ejection/pickup0, safe vanilla mask/binding curse, unjammed MAG+TECH, Protected Inventory, one-shot sounds, TimeStop, persistent rebind, x2*x4/cleanup)");
    }
}
