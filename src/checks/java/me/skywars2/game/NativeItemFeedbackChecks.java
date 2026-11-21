package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import me.skywars2.game.effects.ItemEffectLevels;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.loot.LbcAllItemPools;
import me.skywars2.game.presentation.LbcArmorTooltips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class NativeItemFeedbackChecks {
    private NativeItemFeedbackChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Native item feedback: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var p=f.owner;
            aimbobBoots(game,p);
            refreshedPoolProfiles(game,p);
            for(var id:List.of("blazefire_sword","triple_golden_sword","devil_sword")){
                var loot=game.lbc().rollTable(f.level,p.position(),p,"lbc:ported/"+id);
                require(!loot.isEmpty(),"real source sword "+id);var sword=loot.getFirst();
                require(sword.has(DataComponents.TOOL)&&sword.getDestroySpeed(Blocks.COBWEB.defaultBlockState())==Items.IRON_SWORD.getDefaultInstance().getDestroySpeed(Blocks.COBWEB.defaultBlockState()),"native cobweb sword speed "+id);
                var entity=new ItemEntity(f.level,p.getX()+2,p.getY(),p.getZ(),sword);f.own(entity);f.level.addFreshEntity(entity);
                if(ItemData.intValue(sword,"transformable",0)==1)for(var damage:List.of(f.level.damageSources().inFire(),f.level.damageSources().lava(),f.level.damageSources().cactus()))
                    require(!entity.hurtServer(f.level,damage,100)&&!entity.isRemoved(),"transformable native drop survives environment "+id);
            }
            for(var flag:List.of("reactive","technomk2")){
                var helmet=new ItemStack(Items.IRON_HELMET);var tag=new CompoundTag();tag.putInt(flag,1);helmet.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));p.setItemSlot(EquipmentSlot.HEAD,helmet);
                for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.NAUSEA)){
                    p.removeEffect(effect);require(!p.addEffect(ItemEffectLevels.create(effect,200,1),f.other)&&!p.hasEffect(effect),"helmet rejects effect before first tick "+flag);
                }
                p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            }
            var at=BlockPos.containing(f.home.add(3,0,3));f.set(at,Blocks.SPAWNER.defaultBlockState());
            var egg=new ItemStack(Items.COW_SPAWN_EGG,2);
            var entityTag=TagParser.parseCompoundFully("{Silent:1b,NoAI:1b,Health:31.0f,Tags:['feedback_egg'],attributes:[{id:'minecraft:max_health',base:31}],equipment:{mainhand:{id:'minecraft:diamond_sword',count:1}}}");
            egg.set(DataComponents.ENTITY_DATA,TypedEntityData.of(EntityTypes.COW,entityTag));p.setItemInHand(InteractionHand.MAIN_HAND,egg);
            require(!game.server().getPlayerList().isOp(p.nameAndId()),"nonoperator native player");
            require(p.gameMode.useItemOn(p,f.level,egg,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)).consumesAction()&&egg.getCount()==1,"survival native egg use/debit");
            var spawner=(SpawnerBlockEntity)f.level.getBlockEntity(at);var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());spawner.saveCustomOnly(output);
            var saved=output.buildResult().getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity");
            require(saved.getStringOr("id","").equals("minecraft:cow")&&saved.getBooleanOr("Silent",false)&&saved.getBooleanOr("NoAI",false)
                    &&saved.getFloatOr("Health",0)==31&&saved.get("attributes").equals(entityTag.get("attributes"))&&saved.get("equipment").equals(entityTag.get("equipment")),"full egg NBT enters real spawner data");
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
        SkyWars2.LOGGER.info("[check] PASS native-item-feedback (source swords mine cobweb, transformable drops resist damage, immediate helmet immunity and complete survival spawner egg NBT)");
    }

    private static void refreshedPoolProfiles(GameController game,net.minecraft.server.level.ServerPlayer player){
        var service=game.items().allItemPools();service.initialize();
        var slots=service.slots(LbcAllItemPools.Domain.CUSTOM);
        var profiles=java.util.Map.of("custom/afd15db2d050c882092f","ak47_finite",
                "custom/47f7921c9431e7c7491e","multiply_2","custom/6607f6753c440d7e24c3","multiply_3",
                "custom/0ee5fd94eb9a49a87960","multiply_4","custom/5de63e8f5c18bd176621","multiply_5");
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(player.level())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,player.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,player)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
        for(var profile:profiles.entrySet()){
            int index=slots.indexOf(profile.getKey());require(index>=0,"stable catalog slot "+profile.getValue());
            var actual=new java.util.ArrayList<ItemStack>();
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params)
                    .withOptionalRandomSource(net.minecraft.util.RandomSource.create(52)).create(java.util.Optional.empty());
            service.drawSlot(LbcAllItemPools.Domain.CUSTOM,index,context,actual::add);
            var canonical=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/"+profile.getValue());
            require(actual.size()==1&&canonical.size()==1&&ItemStack.matches(actual.getFirst(),canonical.getFirst()),
                    "catalog draw has the complete current source components, including corrected lore: "+profile.getValue());
        }
    }

    private static void aimbobBoots(GameController game,net.minecraft.server.level.ServerPlayer player){
        var loot=game.lbc().rollTable(player.level(),player.position(),player,
                "lbc:item_defs/minecraft/iron_boots__bf5a87fdd04f");
        require(loot.size()==1&&LbcArmorTooltips.aimbobBoots(loot.getFirst()),"actual Aimbob boots source");
        var boots=loot.getFirst();var before=boots.copy();
        var tooltip=boots.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(player.level()),player,
                net.minecraft.world.item.TooltipFlag.NORMAL);
        require(tooltip.stream().anyMatch(line->LbcArmorTooltips.key(line,"item.modifiers.armor"))
                &&tooltip.stream().anyMatch(line->LbcArmorTooltips.key(line,"item.modifiers.feet")),
                "native enchantment armor group reproduces the split tooltip");
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)player.connection;
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
            var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(boots,
                    net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());
            var lines=proxy.get(DataComponents.LORE).lines();
            require(lines.stream().filter(line->LbcArmorTooltips.key(line,"item.modifiers.feet")).count()==1
                    &&lines.stream().noneMatch(line->LbcArmorTooltips.key(line,"item.modifiers.armor")),
                    "client sees a single feet group for all Aimbob boot attributes");
            require(!proxy.getOrDefault(DataComponents.TOOLTIP_DISPLAY,net.minecraft.world.item.component.TooltipDisplay.DEFAULT)
                    .shows(DataComponents.ATTRIBUTE_MODIFIERS),"native client does not duplicate projected attribute rows");
            require(ItemStack.matches(before,boots)&&ItemStack.matches(boots,
                    eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),player.registryAccess())),
                    "projection preserves real enchantments, attribute slots and the exact restored stack");
            for(var line:tooltip)if(LbcArmorTooltips.key(line,"attribute.modifier.plus.0")
                    ||LbcArmorTooltips.key(line,"attribute.modifier.plus.1")||LbcArmorTooltips.key(line,"attribute.modifier.take.1"))
                require(lines.stream().anyMatch(row->row.toFlatList(eu.pb4.polymer.core.api.item.PolymerItemUtils.CLEAN_STYLE)
                        .equals(line.toFlatList(eu.pb4.polymer.core.api.item.PolymerItemUtils.CLEAN_STYLE))),"exact native attribute text/value retained");
        });
    }
}
