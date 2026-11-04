package me.skywars2.game;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Source Champion stances live entirely in native transient modifiers and player checkpoints. */
public final class LbcChampionSkill {
    static final Identifier ARMOR=Identifier.parse("minecraft:7374yg374g347g37gh34");
    static final Identifier TOUGHNESS=Identifier.parse("minecraft:7374yg3274g347g37gh34");
    static final Identifier ATTACK=Identifier.parse("minecraft:g8wu4g8k4wug84khu84hu48h");
    private static final TagKey<DamageType> ARROWS=TagKey.create(Registries.DAMAGE_TYPE,
            Identifier.parse("minecraft:arrow_only"));
    private final GameController game;
    private final Predicate<ServerPlayer> ownerAdmission;
    private final BiPredicate<ServerPlayer,LivingEntity> targetAdmission;

    public LbcChampionSkill(GameController game,Predicate<ServerPlayer> ownerAdmission,
            BiPredicate<ServerPlayer,LivingEntity> targetAdmission){
        this.game=game;this.ownerAdmission=Objects.requireNonNull(ownerAdmission);
        this.targetAdmission=Objects.requireNonNull(targetAdmission);
    }
    private boolean admitted(ServerPlayer player){
        return game.lbc().hasSkill(player,33)&&ownerAdmission.test(player)
                &&game.server().tickRateManager().runsNormally()&&!game.server().tickRateManager().isFrozen()
                &&!game.items().wands().inputTimeStopped(player)&&!game.items().timeStop().frozen(player)
                &&!game.items().timeStop().stopped(player.level());
    }
    /** Outgoing accepted non-arrow hits, including owner-attributed magic and summoned damage. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(source.is(ARROWS))return;
        var owner=LbcManaVampireSkill.owner(game,source);
        if(owner==null||owner==victim||!admitted(owner)||owner.level()!=victim.level()
                ||!LbcDimensionItems.ordinary(victim.level())||!targetAdmission.test(owner,victim))return;
        remove(owner,Attributes.ATTACK_DAMAGE,ATTACK);
        add(owner,Attributes.ARMOR,ARMOR,20);add(owner,Attributes.ARMOR_TOUGHNESS,TOUGHNESS,5);
        LbcOverload.sound(owner.level(),owner.position(),"minecraft:entity.iron_golem.hurt",1,1,0,0);
        owner.level().sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()),
                owner.getX(),owner.getY()+1,owner.getZ(),20,.5,1,.5,0);
    }
    /** Existing native entity_hurt_player criterion owns acceptance, including shield/resistance. */
    public void incoming(ServerPlayer player,DamageSource source,float original,float actual,boolean blocked){
        if(!admitted(player))return;
        remove(player,Attributes.ARMOR,ARMOR);remove(player,Attributes.ARMOR_TOUGHNESS,TOUGHNESS);
        add(player,Attributes.ATTACK_DAMAGE,ATTACK,6);
        LbcOverload.sound(player.level(),player.position(),"minecraft:entity.wolf.growl",1,2,0,0);
        player.level().sendParticles(ParticleTypes.CRIT,player.getX(),player.getY()+1,player.getZ(),20,.5,1,.5,0);
    }
    private static void add(ServerPlayer p,Holder<Attribute> type,Identifier id,double amount){
        var attribute=p.getAttribute(type);
        if(attribute!=null&&attribute.getModifier(id)==null)
            attribute.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static void remove(ServerPlayer p,Holder<Attribute> type,Identifier id){
        var attribute=p.getAttribute(type);if(attribute!=null)attribute.removeModifier(id);
    }
    public void clear(ServerPlayer player){
        if(MatchPlayerCheckpoint.restoring(player))return;
        remove(player,Attributes.ARMOR,ARMOR);remove(player,Attributes.ARMOR_TOUGHNESS,TOUGHNESS);
        remove(player,Attributes.ATTACK_DAMAGE,ATTACK);
    }
    public void stop(){for(var player:game.server().getPlayerList().getPlayers())clear(player);}
}
