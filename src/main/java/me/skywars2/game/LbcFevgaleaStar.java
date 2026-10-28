package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The standalone sword's shared player counter and four source input branches. */
public final class LbcFevgaleaStar {
    public static final Item SWORD=registerItem();
    private static final String CHARGE="fevgalea_charge";
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    LbcFevgaleaStar(GameController game){this.game=game;}
    static void register(){}
    private static Item registerItem(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:fevgalea_star"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties(){
            @Override public Identifier effectiveModel(){return Identifier.parse("lbc:fevgalea_star");}
        }.setId(key).durability(4000).overrideDescription("fevgalea_star"),Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null&&player instanceof ServerPlayer owner&&controller.items().wands().use(owner,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isSword(ItemStack stack){return !stack.isEmpty()&&(stack.is(SWORD)
            ||stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY)
            .copyTag().getIntOr("fevgalea_star",0)==1);}
    public int charges(ServerPlayer owner){return game.items().mana().score(owner,CHARGE).get();}
    public void addPlayerCounter(ServerPlayer owner){
        game.items().mana().score(owner,CHARGE).set((int)Math.min(Integer.MAX_VALUE,(long)charges(owner)+1));
    }
    private boolean admitted(ServerPlayer owner){return owner!=null&&owner.isAlive()&&!owner.isRemoved()
            &&owner.level()==game.maps().level()&&game.canUseGameplayItems(owner)
            &&game.stage()!=GameController.Stage.ENDING&&!game.items().actionsBlocked(owner)
            &&!game.items().timeStop().stopped(owner.level())&&!owner.entityTags().contains("time_stopped")
            &&!game.items().magic().suppressed(owner)&&!game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())
            &&owner.level().hasChunkAt(BlockPos.containing(owner.getEyePosition()));}
    public boolean use(ServerPlayer owner,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND||!isSword(owner.getMainHandItem())||!admitted(owner))return false;
        boolean applied=cast(owner,hand);
        return game.config().offhandWands&&game.items().wands().use(owner,InteractionHand.OFF_HAND)||applied;
    }
    private boolean cast(ServerPlayer owner,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND||!isSword(owner.getMainHandItem())||!admitted(owner))return false;
        var stack=owner.getMainHandItem();int charge=charges(owner);var mana=game.items().mana();
        var stars=game.items().sauvojen().stars();var eye=owner.getEyePosition();var aim=owner.getLookAngle();
        if(owner.isShiftKeyDown()){
            if(charge>=7){
                burst(owner,0xf5ff9d);LbcOverload.sound(owner,owner.position(),"minecraft:item.trident.riptide_3",3,1.5f,0);
                var focus=focus(owner);var cause=game.items().indirectKills().coldCause(owner,true);
                for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(20),
                        e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().target(owner,e))){
                    double distance=target.position().distanceToSqr(owner.position());if(distance<.0001||distance>400)continue;
                    float amount=Math.max(0,net.minecraft.util.Mth.floor((double)target.getHealth()*.5));
                    if(amount>0){var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(DamageTypes.OUTSIDE_BORDER),focus,owner);
                        target.hurtServer(owner.level(),game.items().indirectKills().captureDamage(source,cause),amount);}
                }
                mana.score(owner,CHARGE).set(charge-7);
            }else{
                if(!mana.trySpendWand(owner,5000))return false;
                var at=owner.position();owner.level().sendParticles(ParticleTypes.HEART,at.x,at.y+1,at.z,100,4,4,4,0);
                burst(owner,0xff9d9d);owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,140,4),owner);
                owner.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,1,99),owner);
                var counter=mana.score(owner,"void_resistance");counter.set((int)Math.min(Integer.MAX_VALUE,(long)counter.get()+7));
            }
        }else if(charge>=1){
            stars.playerBeam(owner,eye,aim);mana.score(owner,CHARGE).set(charge-1);
        }else{
            if(!mana.trySpendWand(owner,1000))return false;
            stars.castAt(owner,hand,stack,6,eye,aim);
        }
        return true;
    }
    private void burst(ServerPlayer owner,int color){
        Vec3 at=owner.position();var level=owner.level();
        level.sendParticles(new DustParticleOptions(color,1),at.x,at.y,at.z,1000,15,15,15,0);
        level.sendParticles(ParticleTypes.FIREWORK,at.x,at.y,at.z,1000,15,15,15,0);
        level.sendParticles(ParticleTypes.END_ROD,at.x,at.y,at.z,1000,15,15,15,0);
    }
    /** No spawned helper is needed merely to preserve a direct source and prevent hook recursion. */
    static Marker focus(ServerPlayer owner){
        var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.getEyePosition());
        marker.entityTags().add(SauvojenStars.TAG);return marker;
    }
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)||!isSword(owner.getMainHandItem())||!admitted(owner)
                ||source.is(ARROW_ONLY)||source.getDirectEntity()!=null&&source.getDirectEntity().entityTags().contains(SauvojenStars.TAG)
                ||!game.items().wands().validRoleTarget(victim,true))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        addPlayerCounter(owner);game.items().sauvojen().stars().castAt(owner,InteractionHand.MAIN_HAND,
                owner.getMainHandItem(),7,owner.getEyePosition(),owner.getLookAngle());
    }
    public void clear(ServerPlayer owner){game.items().mana().score(owner,CHARGE).set(0);}
    public void forget(ServerPlayer owner){clear(owner);}
    public void stop(){for(var owner:game.server().getPlayerList().getPlayers())clear(owner);}
}
