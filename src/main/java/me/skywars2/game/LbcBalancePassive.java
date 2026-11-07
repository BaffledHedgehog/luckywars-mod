package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Inventory is decoded only on inventory/equipment events, never from the old tick loops. */
final class LbcBalancePassive {
    private static final TagKey<Block> PORTALS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:portals_restricted"));
    record Gear(boolean hat,boolean belt,boolean reactive,boolean jump,boolean healing,int mystical){}
    private final GameController game;
    private final Map<UUID,Gear> gear=new LinkedHashMap<>();
    LbcBalancePassive(GameController game){this.game=game;}
    void changed(ServerPlayer p){
        boolean belt=false,healing=false;int mystical=0;
        for(int slot=0;slot<=40;slot++){var stack=p.getInventory().getItem(slot);if(stack.isEmpty())continue;
            if(LbcTmtItems.matches(stack)){
                belt|=LbcBalanceItems.flag(stack,"manabelt_old");healing|=LbcBalanceItems.flag(stack,"heall_old");
                mystical|=(LbcTmtInventory.mysticalTypes(LbcFartus.data(stack),1)>>>16)&0x7e;
                continue;
            }
            if(!stack.is(Items.REPEATING_COMMAND_BLOCK))continue;
            belt|=LbcBalanceItems.flag(stack,"manabelt_old");healing|=LbcBalanceItems.flag(stack,"heall_old");
            if(LbcBalanceItems.flag(stack,"mystical_old")){int type=LbcFartus.data(stack).getIntOr("type",0);if(type>=1&&type<=6)mystical|=1<<type;}
        }
        var head=p.getItemBySlot(EquipmentSlot.HEAD);var feet=p.getItemBySlot(EquipmentSlot.FEET);
        gear.put(p.getUUID(),new Gear(LbcBalanceItems.flag(head,"magichat_old"),belt,LbcTmtPassiveQueries.reactiveOldFeet(feet),LbcBalanceItems.flag(feet,"jumpboost_old"),healing,mystical));
    }
    int cooldownBonus(ServerPlayer p){var g=gear.get(p.getUUID());return g==null?0:(g.hat?4:0)+(g.belt?2:0);}
    void portals(ServerPlayer p){var origin=p.blockPosition();for(var pos:BlockPos.betweenClosed(origin.offset(-5,-5,-5),origin.offset(5,5,5)))if(p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).is(PORTALS))p.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);}
    void tick(ServerPlayer p,int clock){
        var g=gear.get(p.getUUID());if(g==null)return;
        if(g.reactive){p.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,20,0,false,false),p);p.level().sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY()-.2,p.getZ(),1,.2,.02,.2,.01);}
        if((g.mystical&(1<<2))!=0)for(var e:near(p,4))if(Set.of(EntityTypes.AREA_EFFECT_CLOUD,EntityTypes.EVOKER_FANGS,EntityTypes.EXPERIENCE_BOTTLE,EntityTypes.EXPERIENCE_ORB,EntityTypes.DRAGON_FIREBALL).contains(e.getType()))e.discard();
        if((g.mystical&(1<<4))!=0)for(var e:near(p,4))if(e instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow){drop(p,e,Items.ARROW);e.discard();}
        if((g.mystical&(1<<5))!=0)for(var e:near(p,7)){
            double d=e.distanceToSqr(p);var type=e.getType();Item output=null;
            if(type==EntityTypes.TNT&&d<=25)output=Items.TNT;
            if(type==EntityTypes.FIREBALL&&d<=36)output=Items.FIRE_CHARGE;
            if(type==EntityTypes.END_CRYSTAL&&d<=49)output=Items.END_CRYSTAL;
            if(output!=null){drop(p,e,output);e.discard();}else if(type==EntityTypes.CREEPER&&d<=25)e.discard();
        }
        if((g.mystical&(1<<6))!=0){var origin=p.blockPosition();for(var pos:BlockPos.betweenClosed(origin.offset(-2,-2,-2),origin.offset(2,2,2))){if(!p.level().hasChunkAt(pos))continue;var state=p.level().getBlockState(pos);if(state.is(Blocks.LAVA))p.level().setBlock(pos,Blocks.BASALT.defaultBlockState(),3);else if(state.is(Blocks.WATER))p.level().setBlock(pos,Blocks.FROSTED_ICE.defaultBlockState(),3);}}
        if(clock%20==0){
            if((g.mystical&(1<<1))!=0)for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.SLOWNESS,MobEffects.WEAKNESS,MobEffects.STRENGTH,MobEffects.LEVITATION,MobEffects.SLOW_FALLING,MobEffects.HASTE,MobEffects.MINING_FATIGUE))p.removeEffect(effect);
            if((g.mystical&(1<<3))!=0)for(var effect:List.of(MobEffects.REGENERATION,MobEffects.WITHER,MobEffects.POISON,MobEffects.INSTANT_DAMAGE,MobEffects.INSTANT_HEALTH,MobEffects.ABSORPTION,MobEffects.HEALTH_BOOST))p.removeEffect(effect);
            if(g.jump)p.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,20,3,false,true),p);
        }
        if(clock%100==0&&g.healing)p.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,20,2,false,false),p);
    }
    private List<Entity> near(ServerPlayer p,double radius){return p.level().getEntities((Entity)null,p.getBoundingBox().inflate(radius),e->e!=p&&e.distanceToSqr(p)<=radius*radius&&game.items().wands().validRoleTarget(e,false));}
    private void drop(ServerPlayer p,Entity e,Item item){var body=new ItemEntity(p.level(),e.getX(),e.getY(),e.getZ(),new ItemStack(item));game.items().markCurrentItemEpoch(body);p.level().addFreshEntity(body);}
    void forget(ServerPlayer p){gear.remove(p.getUUID());}
    void clear(){gear.clear();}
    Map<UUID,Gear> image(){return Map.copyOf(gear);}
    void restore(Map<UUID,Gear> image){gear.clear();gear.putAll(image);}
}
