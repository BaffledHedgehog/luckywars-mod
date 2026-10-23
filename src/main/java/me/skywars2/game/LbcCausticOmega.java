package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.ScoreHolder;

/** Instant casts only: existing native acid and damage services own all deferred work. */
final class LbcCausticOmega {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final int[] COLORS={0xffff1919,0xffff7f19,0xffffff19,0xff19ff19,0xff1933ff,0xffb219b2};
    /** Preserve native cactus damage/death text while distinguishing the ordinary WILD cast. */
    static final class WildSource extends DamageSource {
        WildSource(ServerPlayer owner){super(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),owner);}
    }
    static boolean use(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean acid,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||owner.entityTags().contains("time_stopped")
                ||game.items().timeStop().stopped(owner.level())||game.items().magic().suppressed(owner)
                ||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Mth.clamp(data.getIntOr("mode",1),1,2);
        if(acid&&academy&&owner.isShiftKeyDown()){
            mode=mode%2+1;data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();
            owner.sendSystemMessage(Component.translatable(mode==1?"mode_default":"mode_ignited"));return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly(acid?"acid_wand_mana_use":"absurd_singularity_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        if(acid){
            var from=owner.getEyePosition().add(owner.getLookAngle().scale(hand==InteractionHand.OFF_HAND?.5:0));
            game.items().clouds().volleyAcid(owner,from,owner.getLookAngle(),academy?8:6,false,academy&&mode==2);
            if(!academy&&owner.isOnFire()&&owner.getItemInHand(hand)==stack&&LbcAcademy.trial(stack))game.items().academy().ready(owner,hand,stack);
        }else omega(game,owner,hand,stack,academy);
        game.items().wands().wandCasted(owner);return true;
    }
    private static void omega(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        int before=Mth.floor(owner.getHealth());
        // Source growl is deliberately global to this dimension, positioned high above the cast.
        LbcOverload.sound(owner,new net.minecraft.world.phys.Vec3(owner.getX(),100000,owner.getZ()),"minecraft:entity.ender_dragon.growl",100000,2,0);
        var types=owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        DamageSource cactus=academy?new DamageSource(types.getOrThrow(DamageTypes.CACTUS),owner):new WildSource(owner);
        var generic=new DamageSource(types.getOrThrow(DamageTypes.GENERIC),owner);
        // Snapshot: damage callbacks can remove entities from the native world index.
        var targets=new java.util.ArrayList<LivingEntity>();
        owner.level().getAllEntities().forEach(e->{if(e instanceof LivingEntity living&&e.getType().builtInRegistryHolder().is(MOBS))targets.add(living);});
        for(var target:targets){
            if(!game.items().wands().validTarget(target)||game.items().magic().suppressedAt(owner.level(),target.position())
                    ||academy&&(target==owner||!game.items().wands().target(owner,target)
                        ||target.entityTags().contains("nomagic_active")))continue;
            for(int color:COLORS)owner.level().sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,color),
                    target.getX(),target.getY()+1,target.getZ(),16,.4,.5,.4,0);
            target.hurtServer(owner.level(),academy&&!(target instanceof ServerPlayer)?generic:cactus,6);
        }
        if(!academy&&before<=Mth.floor(owner.getHealth())&&owner.getItemInHand(hand)==stack&&LbcAcademy.trial(stack))game.items().academy().ready(owner,hand,stack);
    }
}
