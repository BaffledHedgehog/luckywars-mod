package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.ScoreHolder;

/** Instant corruption casts; native statuses retain their caster through the shared effect cause owner. */
final class LbcCorruption {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<EntityType<?>> UNHOLY=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:unholy_monsters"));

    static boolean use(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Mth.clamp(data.getIntOr("mode",1),1,5);
        // Mode selection is free even when the owner cannot afford a cast, in the actual used hand.
        if(academy&&owner.isShiftKeyDown()){
            mode=mode%5+1;data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            owner.getInventory().setChanged();
            owner.sendSystemMessage(mode==5?Component.translatableWithFallback("mode_alternating","Чередующийся")
                    :Component.translatable(switch(mode){case 1->"effect_poison";case 2->"effect_wither";case 3->"effect_hunger";default->"effect_damage";}));
            return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("corruption_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        boolean alternating=academy&&mode==5;
        if(alternating)mode=Mth.clamp(data.getIntOr("alternating_next",1),1,4);
        if(!academy)mode=owner.getRandom().nextInt(4)+1;
        var at=owner.position();
        var targets=owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(15),target->{
            double distance=target.position().distanceToSqr(at);
            return distance>=.0001&&distance<=225&&game.items().wands().target(owner,target)
                    &&!target.entityTags().contains("nomagic_active")&&!game.items().magic().suppressedAt(owner.level(),target.position());
        });
        for(var target:targets){
            // The source's undead replacement is an authored tag, not vanilla's broader undead classification.
            if(mode==1){
                boolean unholy=target.getType().builtInRegistryHolder().is(UNHOLY);
                target.addEffect(new MobEffectInstance(unholy?MobEffects.WITHER:MobEffects.POISON,200,unholy?1:0,false,false),owner);
            }else if(mode==2)target.addEffect(new MobEffectInstance(MobEffects.WITHER,140,1,false,false),owner);
            else if(mode==3)target.addEffect(new MobEffectInstance(MobEffects.HUNGER,40,255,false,false),owner);
        }
        // Ordinary dust is rolled on every cast. Academy narrows that same per-target chance to damage mode.
        if(!academy||mode==4)for(var target:targets)
            if(target.getType().builtInRegistryHolder().is(MOBS)&&owner.getRandom().nextInt(10)==0)dust(game,owner,target);
        if(mode==4){
            var cactus=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),owner);
            for(var target:targets)if(target.getType().builtInRegistryHolder().is(MOBS))target.hurtServer(owner.level(),cactus,8);
            if(!academy&&owner.getItemInHand(hand)==stack&&LbcAcademy.trial(stack)
                    &&targets.stream().anyMatch(target->target.isAlive()&&target.getType().builtInRegistryHolder().is(MOBS)
                        &&target.hasEffect(MobEffects.POISON)&&target.hasEffect(MobEffects.WITHER)&&target.hasEffect(MobEffects.HUNGER)))
                game.items().academy().ready(owner,hand,stack);
        }
        LbcOverload.sound(owner,at,"minecraft:entity.wither.ambient",1,1.2f,0);
        owner.level().sendParticles(ParticleTypes.ASH,academy,false,at.x,at.y+1,at.z,1500,10,10,10,0);
        owner.level().sendParticles(ParticleTypes.ASH,true,false,at.x,at.y+1,at.z,300,1,1,1,1);
        for(var target:targets)if(target instanceof ServerPlayer player&&target.isAlive())
            player.sendSystemMessage(Component.translatable("player_corrupted").withStyle(ChatFormatting.RED));
        if(alternating){
            var updated=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            updated.putInt("alternating_next",mode%4+1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(updated));owner.getInventory().setChanged();
        }
        game.items().wands().wandCasted(owner);
        return true;
    }

    private static void dust(GameController game,ServerPlayer owner,LivingEntity target){
        // /loot spawn uses a chest context with the executor as THIS_ENTITY, independently of the target's luck.
        var params=new LootParams.Builder(owner.level()).withParameter(LootContextParams.ORIGIN,target.position())
                .withParameter(LootContextParams.THIS_ENTITY,owner).create(LootContextParamSets.CHEST);
        var table=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:corrupted_dust")));
        for(var result:table.getRandomItems(params))if(!result.isEmpty()){
            var item=new ItemEntity(owner.level(),target.getX(),target.getY(),target.getZ(),result.copy());
            item.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(item);game.items().timeStop().prepareFrozen(item);
            if(!owner.level().addFreshEntity(item))item.discard();
        }
    }
}
