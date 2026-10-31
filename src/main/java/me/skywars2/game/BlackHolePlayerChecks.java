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
            try{
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(other!=null,"actual opponent");
            var source=new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC),other);
            require(!player.isInvulnerableTo(player.level(),source)&&player.canHarmPlayer(other),"loaded, acknowledged native enemy fixture");
            BlackHoleContactChecks.run(game,player);
            player.setHealth(100);player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(8);
            player.setAbsorptionAmount(8);player.setDeltaMovement(new Vec3(.1,.05,-.2));
            float initialHealth=player.getHealth();
            player.setInvulnerableTime(31);
            require(!BlackHoleDamage.apply(game,player,source,.25f)&&player.getHealth()==initialHealth,
                    "true native temporary immunity remains distinct from the hit cooldown");
            player.setInvulnerableTime(0);
            player.hurtTime=3;player.hurtDuration=4;player.damageCooldownTime=17;
            var velocity=player.getDeltaMovement();base.recording.packets.clear();
            require(BlackHoleDamage.apply(game,player,source,.25f)&&BlackHoleDamage.apply(game,player,source,.25f),"consecutive player HP contacts");
            require(player.getHealth()==initialHealth-.5f&&player.getAbsorptionAmount()==8,"raw HP only");
            require(player.hurtTime==3&&player.hurtDuration==4&&player.damageCooldownTime==17&&player.getInvulnerableTime()==0
                    &&player.getDeltaMovement().equals(velocity),"existing hit state and velocity preserved, no renewed stun");
            require(healthPacket(base.recording,player,initialHealth-.25f)&&healthPacket(base.recording,player,initialHealth-.5f),"self native health metadata precedes next ordinary health/food packet");
            require(base.recording.packets.stream().noneMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
                    ||p instanceof net.minecraft.network.protocol.game.ClientboundDamageEventPacket),"no hurt-event or camera packet");

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
                game.items().polymorph().forget(player);
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(absorptionBase);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS black-hole-contact (native head/pose/scale/sphere contact, repeated HP and self wire without renewed hit state, ownership, native totem and mirrored sheep HP)");
    }
}
