package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
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
}
