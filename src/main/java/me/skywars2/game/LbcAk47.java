package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Player AK-47 only; the Stalin boss has its own source behavior and is not a gun mode. */
public final class LbcAk47 {
    public static final Item ITEM=registerItem();
    private static final CompoundTag FLAG=flag("ak47"),FINITE=flag("finite");
    private static final TagKey<Block> INDESTRUCTIBLE=TagKey.create(Registries.BLOCK,
            Identifier.parse("minecraft:indestructible_very"));
    private final GameController game;
    // The source counter belongs to the player, including players who do not currently carry a gun.
    private final Map<UUID,ServerPlayer> counterOwners=new HashMap<>();

    LbcAk47(GameController game) { this.game=game; }
    public static void register() { }
    private static CompoundTag flag(String name) { var tag=new CompoundTag(); tag.putInt(name,1); return tag; }
    private static Item registerItem() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:ak47"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:ak47"); }
        }.setId(key).stacksTo(1).overrideDescription("AK-47")
                .component(DataComponents.INTERACT_ANIMATION,new net.minecraft.world.item.component.SwingAnimation(
                        net.minecraft.world.item.SwingAnimationType.NONE,6)),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isAk47(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && (stack.is(ITEM) || data!=null && data.matchedBy(FLAG));
    }
    public static boolean isFinite(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return isAk47(stack) && data!=null && data.matchedBy(FINITE);
    }
    public boolean use(ServerPlayer owner,InteractionHand hand) {
        if(!valid(owner) || !isAk47(owner.getItemInHand(hand))
                || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || game.items().actionsBlocked(owner) || game.items().wands().inputTimeStopped(owner)
                || game.items().magic().mechanismSuppressedAt(owner.level(),owner.position())) return false;
        // (user) Each hand pays only for its own finite gun; both copies share the player's rounds.
        boolean finite=isFinite(owner.getItemInHand(hand));
        if(finite && ammo(owner)<1) return false;
        String epoch=game.items().epoch();
        nativeRay(owner);
        // An accepted hit may synchronously end the match or remove the shooter.
        if(valid(owner) && game.items().currentEpoch(epoch)) {
            float yaw=owner.getYRot()+owner.getRandom().nextInt(9)-4;
            float pitch=owner.getXRot()+owner.getRandom().nextInt(5)-6;
            owner.teleportTo(owner.level(),owner.getX(),owner.getY(),owner.getZ(),Set.of(),yaw,pitch,true);
            LbcOverload.sound(owner,owner.position(),"minecraft:lbcsounds.shot",.5f,1,200,.05f);
        }
        if(finite && valid(owner) && game.items().currentEpoch(epoch)) setAmmo(owner,ammo(owner)-1);
        return true;
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && owner.isAlive() && !owner.isRemoved() && !game.isObserver(owner)
                && owner.level()==game.maps().level() && game.canUseGameplayItems(owner)
                && game.stage()!=GameController.Stage.ENDING;
    }
    private void nativeRay(ServerPlayer owner) {
        nativeRay(owner,LbcGunAccuracy.ak47(owner),null);
    }
    /** One gun engine: NPC collision is supplied explicitly; player policy is unchanged. */
    void nativeRay(net.minecraft.world.entity.LivingEntity owner,Vec3 direction,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> collision) {
        nativeRay(owner,direction,collision,160);
    }
    void nativeRay(net.minecraft.world.entity.LivingEntity owner,Vec3 direction,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> collision,double range) {
        var level=(net.minecraft.server.level.ServerLevel)owner.level();
        Vec3 origin=owner.getEyePosition();
        var originBlock=BlockPos.containing(origin);
        if(level.getChunkSource().getChunkNow(originBlock.getX()>>4,originBlock.getZ()>>4)==null) return;
        Vec3 end=origin.add(direction.scale(range));
        var hit=owner instanceof ServerPlayer player && collision==null
                ?game.items().wands().firstHit(player,origin,end)
                :game.items().wands().firstHit(owner,origin,end,collision);
        if(hit!=null) end=hit.getLocation();
        trail(owner,origin,end);
        if(hit instanceof EntityHitResult entityHit) {
            var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.ARROW),owner);
            entityHit.getEntity().hurtServer(level,source,9);
            sound(owner,end,"minecraft:entity.pig.hurt",1,1,0,0);
        } else if(hit instanceof BlockHitResult wall && level.getChunkSource().getChunkNow(
                wall.getBlockPos().getX()>>4,wall.getBlockPos().getZ()>>4)!=null) {
            Vec3 cloud=end.subtract(direction.scale(.4));
            level.sendParticles(ParticleTypes.CLOUD,cloud.x,cloud.y,cloud.z,1,0,0,0,.1);
            if(!level.getBlockState(wall.getBlockPos()).is(INDESTRUCTIBLE)) {
                try {
                    game.items().indirectKills().worldChanges(level,owner,!(owner instanceof ServerPlayer),
                            ()->game.items().wands().destroyRubyCell(owner,wall.getBlockPos()));
                } finally { game.items().indirectKills().finishSupportRemoval(); }
            }
        }
        // No shieldHit: neither source AK ray has a shield_blockable marker.
    }
    static void sound(net.minecraft.world.entity.LivingEntity owner,Vec3 at,String id,float volume,float pitch,
            double radius,float minimum) {
        if(owner instanceof ServerPlayer player) {LbcOverload.sound(player,at,id,volume,pitch,radius,minimum);return;}
        var sound=net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse(id)));
        double audible=sound.value().getRange(volume);long seed=owner.getRandom().nextLong();
        for(var viewer:((net.minecraft.server.level.ServerLevel)owner.level()).getServer().getPlayerList().getPlayers()) {
            if(viewer.level()!=owner.level()) continue;
            double distance=viewer.position().distanceToSqr(at);
            if(radius>0 && distance>radius*radius) continue;
            var heard=at;float loudness=volume;
            if(distance>audible*audible) {
                if(minimum<=0) continue;
                heard=viewer.position().add(at.subtract(viewer.position()).normalize().scale(2));loudness=minimum;
            }
            viewer.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound,
                    net.minecraft.sounds.SoundSource.MASTER,heard.x,heard.y,heard.z,loudness,pitch,seed));
        }
    }
    private void trail(net.minecraft.world.entity.LivingEntity owner,Vec3 origin,Vec3 end) {
        var level=(net.minecraft.server.level.ServerLevel)owner.level();
        Vec3 direction=end.subtract(origin).normalize();
        int samples=Math.min(801,(int)Math.floor(origin.distanceTo(end)/.2)+1);
        for(int i=0;i<samples;i++) {
            Vec3 point=origin.add(direction.scale(i*.2));
            level.sendParticles(ParticleTypes.WHITE_ASH,point.x,point.y,point.z,1,0,0,0,0);
            if(owner.getRandom().nextInt(20)==0)
                level.sendParticles(ParticleTypes.SMALL_FLAME,point.x,point.y,point.z,1,0,0,0,0);
        }
    }
    public int ammo(ServerPlayer owner) { return game.items().mana().score(owner,"hitbullet").get(); }
    public void setAmmo(ServerPlayer owner,int amount) {
        counterOwners.put(owner.getUUID(),owner);
        game.items().mana().score(owner,"hitbullet").set(amount);
    }
    /** One source damage event grants one round, independent of its size beyond the threshold. */
    public void receivedDamage(ServerPlayer owner,float acceptedDamage) {
        if(valid(owner) && Math.round(acceptedDamage*10)>=35) setAmmo(owner,ammo(owner)+1);
    }
    void addPlayerCounters(ServerPlayer owner) { setAmmo(owner,ammo(owner)+1); }
    public void forget(ServerPlayer owner) {
        counterOwners.remove(owner.getUUID());
        var objective=game.server().getScoreboard().getObjective("hitbullet");
        if(objective!=null) game.server().getScoreboard().getOrCreatePlayerScore(owner,objective).set(0);
    }
    public void stop() {
        for(var owner:java.util.List.copyOf(counterOwners.values())) forget(owner);
        counterOwners.clear();
    }
}
