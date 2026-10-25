package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Source fang formations and rhythm casts; native fangs keep their animation and attack clock. */
public final class LbcEvokerBass {
    private static final String FANG="skywars2.evoker_wand";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Set<EvokerFangs> fangs=new LinkedHashSet<>();
    private int clock;
    LbcEvokerBass(GameController game) { this.game=game; }
    static boolean managed(Entity entity) { return entity instanceof EvokerFangs && entity.entityTags().contains(FANG); }
    List<EvokerFangs> snapshot() { return List.copyOf(fangs); }

    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean evoker,boolean academy) {
        if(!game.items().mana().visible(owner) || game.items().actionsBlocked(owner)
                || owner.entityTags().contains("time_stopped") || game.items().timeStop().stopped(owner.level())
                || game.items().magic().suppressed(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int count=evoker?3:4,choices=evoker?4:count,mode=Math.max(1,Math.min(choices,data.getIntOr("mode",1)));
        if(academy && owner.isShiftKeyDown()) {
            mode=mode%choices+1;data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            owner.getInventory().setChanged();
            owner.sendSystemMessage(evoker?(mode==4?net.minecraft.network.chat.Component.translatableWithFallback("mode_alternating","Чередующийся"):net.minecraft.network.chat.Component.translatable("formation").append(switch(mode){case 1->"@";case 2->"O";default->"\\|/";}))
                    :net.minecraft.network.chat.Component.translatable(switch(mode){case 1->"effect_weakness";case 2->"effect_slowness";case 3->"effect_blindness";default->"effect_mining_fatigue";}));
            return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly(evoker?"evoker_wand_mana_use":"bass_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        boolean alternating=evoker&&academy&&mode==4;
        if(alternating)mode=Math.max(1,Math.min(count,data.getIntOr("alternating_next",1)));
        if(!academy)mode=owner.getRandom().nextInt(count)+1;
        if(evoker)evoker(owner,hand,stack,mode);else bass(owner,hand,stack,mode,academy);
        if(alternating){
            var updated=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            updated.putInt("alternating_next",mode%count+1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(updated));owner.getInventory().setChanged();
        }
        game.items().wands().wandCasted(owner);return true;
    }
    private List<LivingEntity> mobs(ServerPlayer owner,double radius) {
        return owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(owner.position(),owner.position()).inflate(radius),
                e->e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(owner.position())<=radius*radius
                        &&game.items().wands().validTarget(e));
    }
    private void spawn(ServerPlayer owner,Vec3 position) {
        if(!owner.level().hasChunkAt(BlockPos.containing(position)) || game.items().magic().suppressedAt(owner.level(),position))return;
        var fang=new EvokerFangs(owner.level(),position.x,position.y,position.z,0,0,owner);
        fang.entityTags().add(FANG);fang.entityTags().add("magic");game.items().markCurrentItemEpoch(fang);
        if(owner.level().addFreshEntity(fang))fangs.add(fang);
    }
    void evoker(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode) {
        if(mode==1)for(var mob:mobs(owner,17)) {
            if(mob.position().distanceToSqr(owner.position())<2.25)continue;
            var at=mob.position();spawn(owner,at);spawn(owner,at.add(1.2,0,0));spawn(owner,at.add(-1.2,0,0));spawn(owner,at.add(0,0,1.2));spawn(owner,at.add(0,0,-1.2));
        }
        else if(mode==2)for(int y=0;y<=4;y+=2) {
            for(int[] p:new int[][]{{3,0},{-3,0},{0,3},{0,-3},{1,2},{2,1},{-1,-2},{-2,-1},{1,-2},{-1,2},{2,-1},{-2,1}})
                spawn(owner,owner.position().add(p[0],y,p[1]));
        } else {
            // Exact local-coordinate fan, including pitch, anchored at the caster's feet.
            Vec3 forward=owner.getLookAngle(),left=Vec3.directionFromRotation(0,owner.getYRot()-90);
            for(int z=2;z<=20;z++)spawn(owner,owner.position().add(forward.scale(z)));
            for(int z=2;z<=17;z++)for(int side:new int[]{-1,1})spawn(owner,owner.position().add(forward.scale(z)).add(left.scale(side*(z-1)*.5)));
        }
        if(!owner.level().getEntitiesOfClass(net.minecraft.world.entity.monster.illager.Evoker.class,owner.getBoundingBox().inflate(10),e->e.distanceToSqr(owner)<=100).isEmpty())
            award(owner,"lbc:true_advancements/staffs/evoker");
        if(LbcAcademy.trial(stack)) {
            int near=0;
            for(var fang:owner.level().getEntitiesOfClass(EvokerFangs.class,owner.getBoundingBox().inflate(20),f->f.distanceToSqr(owner)<=400))
                if(!owner.level().getEntitiesOfClass(LivingEntity.class,fang.getBoundingBox().inflate(1),e->e!=owner&&e.distanceToSqr(fang)<=1&&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validTarget(e)).isEmpty())near++;
            if(near>=40)game.items().academy().ready(owner,hand,stack);
        }
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.evoker.prepare_attack",1,2,0);
    }
    /** Called only at the native attack tick; replaces its owner-allies shortcut for friendly mobs. */
    public void fangHit(EvokerFangs fang,LivingEntity victim) {
        if(!(fang.getOwner() instanceof ServerPlayer owner)||!game.items().currentEpoch(fang)
                ||!game.items().wands().projectileTarget(owner,victim)||victim.isInvulnerable()
                ||game.items().magic().suppressedAt(owner.level(),fang.position())
                ||victim.entityTags().contains("nomagic_active")||game.items().magic().suppressedAt(owner.level(),victim.position()))return;
        var source=owner.damageSources().indirectMagic(fang,owner);
        if(victim.hurtServer(owner.level(),source,6))net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(owner.level(),victim,source);
    }
    void bass(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,boolean academy) {
        Holder<MobEffect> effect=switch(mode){case 1->MobEffects.WEAKNESS;case 2->MobEffects.SLOWNESS;case 3->MobEffects.BLINDNESS;default->MobEffects.MINING_FATIGUE;};
        int duration=switch(mode){case 2->240;case 4->180;default->160;};
        for(var mob:owner.level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(25),e->e.distanceToSqr(owner)<=625&&e.distanceToSqr(owner)>=.0001)) {
            if(!game.items().wands().target(owner,mob) || academy&&mode==3&&!(mob instanceof ServerPlayer))continue;
            mob.addEffect(new MobEffectInstance(effect,duration,mode==2?3:0,false,false),owner);
            if(mob instanceof ServerPlayer p)p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("player_stunned"));
        }
        combo(owner);
        if(LbcAcademy.trial(stack)&&mobs(owner,10).stream().anyMatch(e->e!=owner&&e.hasEffect(MobEffects.WEAKNESS)&&e.hasEffect(MobEffects.SLOWNESS)&&e.hasEffect(MobEffects.BLINDNESS)&&e.hasEffect(MobEffects.MINING_FATIGUE)))
            game.items().academy().ready(owner,hand,stack);
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.firework_rocket.large_blast",2,0,0);
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.firework_rocket.blast",2,0,0);
        owner.level().sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0x1AFFFFFF),owner.getX(),owner.getY(),owner.getZ(),100,17,17,17,1);
    }
    private int score(ServerPlayer p,String name){return game.items().mana().score(p,name).get();}
    private void score(ServerPlayer p,String name,int value){game.items().mana().score(p,name).set(value);}
    private void combo(ServerPlayer owner) {
        int combo=score(owner,"bass_combo");
        if(combo>=2) {
            int delta=clock-score(owner,"bass_combo_ticker");
            if(delta>4&&Math.abs((long)delta-score(owner,"bass_combo_len"))<=1) {
                combo++;score(owner,"bass_combo",combo);game.items().mana().set(owner,game.items().mana().get(owner)+500);
                owner.level().sendParticles(ParticleTypes.FIREWORK,owner.getX(),owner.getY()+3,owner.getZ(),150,0,0,0,.5);
                LbcOverload.sound(owner,owner.position(),"minecraft:entity.firework_rocket.blast",1,1.5f,0);
                LbcOverload.sound(owner,owner.position(),"minecraft:entity.firework_rocket.twinkle",1,1.5f,0);
                if(combo>=10)award(owner,"lbc:true_advancements/staffs/bass");
                var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.SONIC_BOOM),owner);
                for(var mob:mobs(owner,25))if(mob!=owner)mob.hurtServer(owner.level(),source,combo);
            } else {combo=0;score(owner,"bass_combo",0);}
            score(owner,"bass_combo_ticker",clock);
        }
        // Preserve source order: a failed beat starts a new combo in the same cast.
        if(combo==1) {score(owner,"bass_combo_len",clock-score(owner,"bass_combo_ticker"));score(owner,"bass_combo_ticker",clock);score(owner,"bass_combo",2);}
        else if(combo==0){score(owner,"bass_combo_ticker",clock);score(owner,"bass_combo",1);}
    }
    private void award(ServerPlayer p,String id){var a=game.server().getAdvancements().get(Identifier.parse(id));if(a!=null)for(String c:a.value().criteria().keySet())p.getAdvancements().award(a,c);}
    public void tick() {
        if(!game.items().timeStop().stopped(game.maps().level()))clock++;
        for(var fang:List.copyOf(fangs))if(fang.isRemoved()||!game.playing()||!game.items().currentEpoch(fang))unloaded(fang);
    }
    void loaded(Entity e){if(managed(e)){if(game.playing()&&game.items().currentEpoch(e))fangs.add((EvokerFangs)e);else e.discard();}}
    void unloaded(Entity e){if(e instanceof EvokerFangs f&&fangs.remove(f)&&!f.isRemoved())f.discard();}
    void forget(ServerPlayer p){for(String n:List.of("bass_combo","bass_combo_ticker","bass_combo_len"))score(p,n,0);for(var f:List.copyOf(fangs))if(f.getOwner()==p)unloaded(f);}
    public void stop(){for(var f:List.copyOf(fangs))unloaded(f);for(var p:game.server().getPlayerList().getPlayers())for(String n:List.of("bass_combo","bass_combo_ticker","bass_combo_len"))score(p,n,0);clock=0;}
}
