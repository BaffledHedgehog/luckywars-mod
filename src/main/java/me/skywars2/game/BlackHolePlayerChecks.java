package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.mixin.BlackHoleDeathProtectionAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.stats.Stats;

/** Native health publication must avoid LocalPlayer.hurtTo's independent hit reaction. */
final class BlackHolePlayerChecks {
    private BlackHolePlayerChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Hole HP: "+why);}
    private static boolean healthPacket(ElementalGrimoireChecks.Recording wire,ServerPlayer player,float value){
        int field=BlackHoleDeathProtectionAccess.skywars2$healthData().id();
        return wire.packets.stream().filter(ClientboundSetEntityDataPacket.class::isInstance)
                .map(ClientboundSetEntityDataPacket.class::cast).filter(p->p.id()==player.getId())
                .flatMap(p->p.packedItems().stream()).anyMatch(v->v.id()==field&&v.value().equals(value));
    }
    private static long sound(Packet<?> packet,String sound){
        if(packet instanceof ClientboundBundlePacket bundle){long count=0;for(var child:bundle.subPackets())count+=sound(child,sound);return count;}
        if(packet instanceof ClientboundSoundPacket value)return value.getSound().value().location().toString().equals(sound)?1:0;
        if(packet instanceof ClientboundSoundEntityPacket value)return value.getSound().value().location().toString().equals(sound)?1:0;
        return 0;
    }
    private static CompoundTag food(ServerPlayer player){
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());
        player.getFoodData().addAdditionalSaveData(output);return output.buildResult();
    }
    static void run(GameController game){
        try(var base=new ElementalGrimoireChecks.Fixture(game)){
            base.initialize();var player=base.owner;
            player.hasChangedDimension();
            player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            var positions=base.recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance)
                    .map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).toList();
            if(!positions.isEmpty())player.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(
                    positions.getLast().id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
            double absorptionBase=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).getBaseValue();
            var oldFood=food(player);int oldNoAction=player.getNoActionTime();
            int oldDamageTaken=player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));
            try{
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(other!=null,"actual opponent");
            var source=new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC),other);
            require(!player.isInvulnerableTo(player.level(),source)&&player.canHarmPlayer(other),"loaded, acknowledged native enemy fixture");
            BlackHoleContactChecks.run(game,player);
            var otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());
            var otherConnection=other.connection;var otherWire=new ElementalGrimoireChecks.Recording(other);
            int oldTier=game.items().mana().score(player,LbcNanobotArmor.TIER).get();
            int oldCooldown=game.items().mana().score(player,LbcNanobotArmor.COOLDOWN).get();
            try{
            other.connection=new ServerGamePacketListenerImpl(game.server(),otherWire,other,
                    new CommonListenerCookie(other.getGameProfile(),0,other.clientInformation(),false));
            other.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            other.teleportTo(player.level(),player.getX()+2,player.getY(),player.getZ(),java.util.Set.of(),0,0,true);
            other.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            other.getInventory().clearContent();other.removeAllEffects();game.items().mana().noMagic(other,0);
            game.items().mana().set(other,0);
            var reactive=new ItemStack(Items.PAPER);var flags=new net.minecraft.nbt.CompoundTag();
            flags.putInt("mystical",2);flags.putInt("type",6);
            reactive.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(flags));
            other.getInventory().setItem(0,reactive);
            game.items().mana().score(player,LbcNanobotArmor.TIER).set(20);
            game.items().mana().score(player,LbcNanobotArmor.COOLDOWN).set(0);
            long criteria=game.items().incomingArtifacts().criterionCount();
            player.setHealth(100);player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(8);
            player.setAbsorptionAmount(8);player.setDeltaMovement(new Vec3(.1,.05,-.2));
            float initialHealth=player.getHealth();
            player.setInvulnerableTime(31);
            require(!BlackHoleDamage.apply(game,player,source,.25f)&&player.getHealth()==initialHealth,
                    "true native temporary immunity remains distinct from the hit cooldown");
            player.setInvulnerableTime(0);
            player.hurtTime=3;player.hurtDuration=4;player.damageCooldownTime=17;
            player.setNoActionTime(143);
            player.getStats().setValue(player,Stats.CUSTOM.get(Stats.DAMAGE_TAKEN),0);
            var foodBefore=food(player);int damageBefore=player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));
            var velocity=player.getDeltaMovement();base.recording.packets.clear();otherWire.packets.clear();
            require(BlackHoleDamage.apply(game,player,source,.25f)&&BlackHoleDamage.apply(game,player,source,.25f),"consecutive player HP contacts");
            require(player.getNoActionTime()==0,"accepted direct HP contact resets native no-action timer");
            float exhausted=foodBefore.getFloatOr("foodExhaustionLevel",0);
            exhausted=Math.min(40,exhausted+source.getFoodExhaustion());exhausted=Math.min(40,exhausted+source.getFoodExhaustion());
            require(Math.abs(food(player).getFloatOr("foodExhaustionLevel",-1)-exhausted)<.00001,
                    "each accepted direct HP contact retains native source food exhaustion");
            require(player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN))==damageBefore+2*Math.round(.25f*10),
                    "each accepted direct HP contact awards native rounded DAMAGE_TAKEN exactly once; before="+damageBefore+" after="+player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN)));
            String hurtSound=source.type().effects().sound().location().toString();
            require(otherWire.packets.stream().mapToLong(packet->sound(packet,hurtSound)).sum()==2,
                    "nearby observer receives one actual native hurt sound per accepted contact");
            require(base.recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance)
                    .map(ClientboundSoundPacket.class::cast).filter(packet->packet.getSound().value().location().toString().equals(hurtSound)).count()==2,
                    "victim receives exactly one native sound packet per accepted contact without a damage-event response");
            require(game.items().incomingArtifacts().criterionCount()==criteria+2&&game.items().armorSets().nanobots().tier(player)==10,
                    "each HP drain synchronously invokes the shared incoming criterion and nanobot wear exactly once");
            require(game.items().mana().get(other)==2000,"each accepted drain synchronously triggers the existing reactive artifact exactly once");
            other.getInventory().clearContent();
            require(player.getHealth()==initialHealth-.5f&&player.getAbsorptionAmount()==8,"raw HP only");
            require(player.hurtTime==3&&player.hurtDuration==4&&player.damageCooldownTime==17&&player.getInvulnerableTime()==0
                    &&player.getDeltaMovement().equals(velocity),"existing hit state and velocity preserved, no renewed stun");
            require(healthPacket(base.recording,player,initialHealth-.25f)&&healthPacket(base.recording,player,initialHealth-.5f),"self native health metadata precedes next ordinary health/food packet");
            require(base.recording.packets.stream().noneMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
                    ||p instanceof net.minecraft.network.protocol.game.ClientboundDamageEventPacket),"no hurt-event or camera packet");
            // The ordinary generic/hole source has exhaustion zero. Exercise a positive native
            // source too, so a missing exhaustion callback cannot pass as unchanged zero.
            var foodProbe=food(player);foodProbe.putFloat("foodExhaustionLevel",1.25f);
            player.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),foodProbe));
            var exhaustingSource=new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.PLAYER_ATTACK),other);
            require(exhaustingSource.getFoodExhaustion()>0,"positive native exhaustion control");
            int exhaustingStat=player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));
            player.setNoActionTime(87);base.recording.packets.clear();otherWire.packets.clear();
            require(BlackHoleDamage.apply(game,player,exhaustingSource,.25f)
                    &&Math.abs(food(player).getFloatOr("foodExhaustionLevel",-1)-(1.25f+exhaustingSource.getFoodExhaustion()))<.00001
                    &&player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN))==exhaustingStat+Math.round(.25f*10)
                    &&player.getNoActionTime()==0,"nonzero source exhaustion/stat/no-action feedback stays native");
            require(otherWire.packets.stream().mapToLong(packet->sound(packet,exhaustingSource.type().effects().sound().location().toString())).sum()==1,
                    "positive-exhaustion control retains one native observer hurt sound");
            require(player.hurtTime==3&&player.hurtDuration==4&&player.damageCooldownTime==17&&player.getDeltaMovement().equals(velocity)
                    &&base.recording.packets.stream().noneMatch(packet->packet instanceof ClientboundHurtAnimationPacket||packet instanceof ClientboundDamageEventPacket),
                    "ordinary feedback does not renew victim hit state/camera/velocity even with a player-attack source");

            var cow=new Cow(EntityTypes.COW,player.level());
            try{
                cow.setHealth(1);cow.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
                require(BlackHoleDamage.apply(game,cow,source,2)&&cow.isAlive()&&cow.getHealth()==1
                        &&cow.getMainHandItem().isEmpty(),"native death protection retains its source-dependent behavior");
            }finally{cow.discard();}
            // A player's temporary sheep mirrors HP; it needs the same no-stun publication.
            var body=game.items().polymorph().transform(player,game.items().indirectKills().wildCause(other));
            require(body!=null&&player.getHealth()==8,"native sheep body");base.recording.packets.clear();
            try{
                require(BlackHoleDamage.apply(game,body,source,.5f)&&body.getHealth()==7.5f&&player.getHealth()==7.5f
                        &&healthPacket(base.recording,player,7.5f),"polymorph body mirrors and publishes direct HP without a hurt packet");
            }finally{game.items().polymorph().forget(player);}
            }finally{
                other.connection=otherConnection;otherConnection.resetPosition();
                game.items().mana().score(player,LbcNanobotArmor.TIER).set(oldTier);
                game.items().mana().score(player,LbcNanobotArmor.COOLDOWN).set(oldCooldown);
                otherSaved.restore(game);
            }
            }finally{
                game.items().polymorph().forget(player);
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(absorptionBase);
                player.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),oldFood));
                player.setNoActionTime(oldNoAction);
                player.getStats().setValue(player,Stats.CUSTOM.get(Stats.DAMAGE_TAKEN),oldDamageTaken);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS black-hole-contact (native body contact, repeated HP/self wire without renewed hit state, observer hurt sounds, no-action/food/stat feedback, native totem and mirrored sheep HP)");
    }
}
