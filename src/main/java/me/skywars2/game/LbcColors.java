package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Все 16 веток цветового артефакта; учёт предметов и подавление принадлежат LbcMagic. */
public final class LbcColors {
    private static TagKey<Block> blockTag(String id) { return TagKey.create(Registries.BLOCK, Identifier.parse(id)); }
    private static final TagKey<Block> COLORED = blockTag("minecraft:colored"), INSIDE = blockTag("minecraft:in_colored"),
            AIRS = blockTag("minecraft:airs");
    // Порядок команд исходника важен при пересечении тегов и телепортации внутри dispatch.
    private static final List<TagKey<Block>> COLORS = List.of(blockTag("minecraft:red"), blockTag("minecraft:white"),
            blockTag("minecraft:blue"), blockTag("minecraft:yellow"), blockTag("minecraft:orange"),
            blockTag("minecraft:light_blue"), blockTag("minecraft:purple"), blockTag("minecraft:gray"),
            blockTag("minecraft:light_gray"), blockTag("minecraft:lime"), blockTag("minecraft:pink"),
            blockTag("minecraft:cyan"), blockTag("minecraft:black"), blockTag("minecraft:brown"),
            blockTag("minecraft:green"), blockTag("minecraft:magenta"));
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final int FILL_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private final GameController game;
    private final Set<LivingEntity> teleporting = new LinkedHashSet<>();
    private final Set<LivingEntity> waterEffects = new LinkedHashSet<>();

    LbcColors(GameController game) { this.game = game; }

    /** Два независимых source-триггера: блок снизу и частичный блок вокруг ног. */
    void pulse(ServerPlayer owner) {
        if (owner.level().getBlockState(BlockPos.containing(owner.position().add(0,-.5,0))).is(COLORED)) dispatch(owner, true);
        if (owner.level().getBlockState(owner.blockPosition()).is(INSIDE)) dispatch(owner, false);
    }
    private void dispatch(ServerPlayer owner, boolean below) {
        var pos = below ? BlockPos.containing(owner.position().add(0,-.5,0)) : owner.blockPosition();
        var state = owner.level().getBlockState(pos);
        for (int color=0;color<COLORS.size();color++) {
            // Каждая ветка source содержит `at @s`: эффекты центрированы на игроке, не на блоке под ногами.
            if (state.is(COLORS.get(color))) apply(owner,color);
        }
    }
    private void apply(ServerPlayer p, int color) {
        switch (color) {
            case 0 -> {
                effect(p,MobEffects.STRENGTH,6000,0,false); p.removeEffect(MobEffects.WEAKNESS);
                enemies(p,15,false,e->effect(e,MobEffects.WEAKNESS,1200,0,false));
                particle(p,ParticleTypes.CRIT,4,.5,1,.5,0);
            }
            case 1 -> {
                effect(p,MobEffects.SPEED,6000,1,false); p.removeEffect(MobEffects.SLOWNESS);
                enemies(p,25,false,e->effect(e,MobEffects.SLOWNESS,600,1,true));
                particle(p,ParticleTypes.END_ROD,2,.5,1,.5,0);
            }
            case 2 -> {
                effect(p,MobEffects.WATER_BREATHING,600,2,false); effect(p,MobEffects.CONDUIT_POWER,600,1,false);
                particle(p,ParticleTypes.NAUTILUS,10,.5,1,.5,0);
            }
            case 3 -> {
                effect(p,MobEffects.HASTE,6000,10,false); p.removeEffect(MobEffects.MINING_FATIGUE);
                enemies(p,25,false,e->effect(e,MobEffects.MINING_FATIGUE,1200,2,false));
                particle(p,ParticleTypes.FALLING_HONEY,8,.5,1,.5,0);
            }
            case 4 -> {
                effect(p,MobEffects.FIRE_RESISTANCE,6000,0,false); particle(p,ParticleTypes.LAVA,4,.5,1,.5,0);
                enemies(p,30,false,this::ignite);
            }
            case 5 -> {
                effect(p,MobEffects.SLOW_FALLING,6000,0,false);
                enemies(p,20,false,e->effect(e,MobEffects.LEVITATION,40,5,false));
                particle(p,ParticleTypes.CLOUD,3,.5,1,.5,.04);
            }
            case 6 -> {
                enemies(p,20,false,e->{ if(e instanceof LivingEntity living && e.getType().builtInRegistryHolder().is(MOBS)) curse(living,600); });
                portals(p); spread(p,game.config().noBorders ? p.position() : Vec3.ZERO,game.config().noBorders ? 200 : 190);
                portals(p);
            }
            case 7 -> {
                effect(p,MobEffects.RESISTANCE,200,4,false);
                var barrier = new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.BARRIER.defaultBlockState());
                particles(p,barrier,p.position().add(0,.5,0),1,0,0,0,0);
                particles(p,barrier,p.position().add(0,1.5,0),1,0,0,0,0);
            }
            case 8 -> {
                effect(p,MobEffects.RESISTANCE,6000,2,false);
                enemies(p,15,false,e->{ if(e instanceof LivingEntity living) living.removeEffect(MobEffects.RESISTANCE); });
                particle(p,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.IRON_BLOCK.defaultBlockState()),10,.5,1,.5,0);
            }
            case 9 -> {
                effect(p,MobEffects.JUMP_BOOST,6000,2,false); p.removeEffect(MobEffects.LEVITATION);
                enemies(p,15,false,e->effect(e,MobEffects.LEVITATION,20,0,true));
                // `~ 1 ~` в исходнике — абсолютная высота 1, а не смещение +1.
                particles(p,ParticleTypes.SNEEZE,new Vec3(p.getX(),1,p.getZ()),3,.5,1,.5,0);
            }
            case 10 -> {
                effect(p,MobEffects.INSTANT_HEALTH,1,0,false); effect(p,MobEffects.REGENERATION,6000,1,false);
                p.removeEffect(MobEffects.POISON); p.removeEffect(MobEffects.WITHER); p.removeEffect(MobEffects.INSTANT_DAMAGE);
                enemies(p,20,false,e->effect(e,MobEffects.WITHER,200,1,false));
                particle(p,ParticleTypes.HEART,3,.5,1,.5,0);
            }
            case 11 -> {
                effect(p,MobEffects.INVISIBILITY,6000,1,true); effect(p,MobEffects.NIGHT_VISION,6000,1,true);
                enemies(p,300,false,e->effect(e,MobEffects.GLOWING,600,0,true));
                particle(p,ParticleTypes.GLOW_SQUID_INK,2,.5,1,.5,.02);
            }
            case 12 -> {
                p.removeEffect(MobEffects.BLINDNESS); effect(p,MobEffects.NIGHT_VISION,6000,0,false);
                enemies(p,20,true,e->effect(e,MobEffects.BLINDNESS,600,0,false));
                for (var e : p.level().getEntities(p,new AABB(p.position(),p.position()).inflate(20),this::eligible)) {
                    if(e instanceof LivingEntity living && !(e instanceof ServerPlayer) && !p.isAlliedTo(e)
                            && p.distanceToSqr(e)<=400 && e.getType().builtInRegistryHolder().is(MOBS)) {
                        var range=living.getAttribute(Attributes.FOLLOW_RANGE); if(range!=null) range.setBaseValue(4);
                    }
                }
                particle(p,ParticleTypes.GLOW,4,.5,1,.5,.01);
            }
            case 13 -> {
                effect(p,MobEffects.SATURATION,1,1,false); foodCount(p,foodCount(p)+1); p.removeEffect(MobEffects.HUNGER);
                enemies(p,25,true,e->effect(e,MobEffects.HUNGER,600,1,false));
                particles(p,new ItemParticleOption(ParticleTypes.ITEM,Items.COOKED_BEEF),
                        p.getEyePosition().add(p.getLookAngle().scale(.5)),4,.2,.2,.2,0);
                sound(p,SoundEvents.PLAYER_BURP);
            }
            case 14 -> {
                p.giveExperiencePoints(3); enemies(p,200,true,e->((ServerPlayer)e).giveExperiencePoints(-3));
                particle(p,new DustParticleOptions(0x36f31d,1),5,.5,1,.5,.02);
            }
            case 15 -> {
                mana(p,mana(p)+20); enemies(p,35,true,e->{ var target=(ServerPlayer)e; int value=mana(target); if(value>=20) mana(target,value-20); });
                particle(p,ParticleTypes.WITCH,4,.5,1,.5,0);
            }
            default -> throw new IllegalArgumentException("Unknown artifact color: "+color);
        }
    }
    private boolean eligible(Entity e) {
        return e.isAlive() && !e.isRemoved() && !e.entityTags().contains("spectator")
                && (!(e instanceof ServerPlayer p) || game.canUseGameplayItems(p));
    }
    /** Игроки берутся из готового списка мира; большие радиусы не обходят пустые чанки. */
    private void enemies(ServerPlayer owner, int radius, boolean playersOnly, Consumer<Entity> action) {
        double squared=(double)radius*radius;
        if(playersOnly) {
            for(var p:owner.level().players()) if(p!=owner && eligible(p) && !owner.isAlliedTo(p)) {
                double distance=owner.distanceToSqr(p); if(distance>=.0001 && distance<=squared) action.accept(p);
            }
        } else {
            for(var e:owner.level().getEntities(owner,new AABB(owner.position(),owner.position()).inflate(radius),e->eligible(e) && !owner.isAlliedTo(e))) {
                double distance=owner.distanceToSqr(e); if(distance>=.0001 && distance<=squared) action.accept(e);
            }
        }
    }
    private static void effect(Entity entity, Holder<MobEffect> effect, int ticks, int amplifier, boolean hidden) {
        if(entity instanceof LivingEntity living) living.addEffect(new MobEffectInstance(effect,ticks,amplifier,false,!hidden,!hidden));
    }
    private void ignite(Entity target) {
        fillAir(target,Blocks.FIRE.defaultBlockState(),1);
    }
    private void fillAir(Entity target, BlockState replacement, int top) {
        var level=game.maps().level(); var center=target.blockPosition();
        var changes=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=-1;x<=1;x++) for(int y=-1;y<=top;y++) for(int z=-1;z<=1;z++) {
            var pos=center.offset(x,y,z);
            if(!level.hasChunkAt(pos)) continue;
            var old=level.getBlockState(pos);
            if(old.is(AIRS) && level.setBlock(pos,replacement,FILL_FLAGS)) changes.put(pos,old);
        }
        for(var entry:changes.entrySet()) level.updateNeighboursOnBlockSet(entry.getKey(),entry.getValue());
    }
    private static void particle(ServerPlayer p, ParticleOptions options, int count, double dx, double dy, double dz, double speed) {
        particles(p,options,p.position().add(0,1,0),count,dx,dy,dz,speed);
    }
    private static void particles(ServerPlayer p, ParticleOptions options, Vec3 pos, int count, double dx, double dy, double dz, double speed) {
        p.level().sendParticles(options,pos.x,pos.y,pos.z,count,dx,dy,dz,speed);
    }
    private static void sound(ServerPlayer p, SoundEvent sound) {
        for(var viewer:p.level().players()) if(viewer.distanceToSqr(p)<=256)
            viewer.connection.send(new ClientboundSoundPacket(Holder.direct(sound),SoundSource.MASTER,p.getX(),p.getY(),p.getZ(),1,1,p.getRandom().nextLong()));
    }
    private static void portals(ServerPlayer p) {
        particle(p,ParticleTypes.REVERSE_PORTAL,100,.1,.6,.1,1); sound(p,SoundEvents.ENDERMAN_TELEPORT);
    }
    /** Нативный spreadplayers сохраняет поиск безопасной поверхности, включая Nether `under 120`. */
    private void spread(LivingEntity target, Vec3 center, int radius) {
        var source=game.server().createCommandSourceStack().withLevel(game.maps().level())
                .withEntity(target).withPosition(target.position()).withSuppressedOutput();
        String under=target.level().dimension().equals(Level.NETHER) ? " under 120" : "";
        game.server().getCommands().performPrefixedCommand(source,"spreadplayers "+center.x+" "+center.z+" 0 "+radius+under+" false @s");
    }

    // Цветовые ветки и RTP переиспользуют общие ресурсы; здесь больше нет отдельной системы маны.
    private ScoreAccess score(Entity entity, String name) {
        return game.items().mana().score(entity,name);
    }
    public int mana(ServerPlayer p) { return score(p,"mana").get(); }
    public void mana(ServerPlayer p,int value) { score(p,"mana").set(value); }
    public int foodCount(ServerPlayer p) { return score(p,"food_count").get(); }
    public void foodCount(ServerPlayer p,int value) { score(p,"food_count").set(value); }
    void resetResources(ServerPlayer p) { game.items().mana().resetResources(p); }

    /** Raid Omen 40 в старом datapack превращался в накопительный RTP-таймер, не в обычное зелье. */
    private void curse(LivingEntity target,int ticks) {
        if(!target.canBeAffected(new MobEffectInstance(MobEffects.RAID_OMEN,ticks,40,false,false,false))) return;
        var timer=score(target,"effect_rtp_1"); timer.add(ticks); teleporting.add(target);
    }
    public int teleportTicks(LivingEntity target) { return score(target,"effect_rtp_1").get(); }
    void clearEffects(LivingEntity target) { score(target,"effect_rtp_1").set(0); teleporting.remove(target); }
    void loaded(Entity e) {
        if(e instanceof LivingEntity living) {
            effectsChanged(living);
            var board=game.server().getScoreboard(); var objective=board.getObjective("effect_rtp_1");
            var value=objective==null ? null : board.getPlayerScoreInfo(living,objective);
            if(value!=null && value.value()>0) teleporting.add(living);
        }
    }
    void unloaded(Entity e) { teleporting.remove(e); waterEffects.remove(e); }
    /** События эффектов индексируют только усиленные source-варианты; обычные зелья остаются нативными. */
    public void effectsChanged(LivingEntity target) {
        if(target.level()!=game.maps().level() || !eligible(target)) { waterEffects.remove(target); return; }
        if(enhanced(target,MobEffects.CONDUIT_POWER)>0 || enhanced(target,MobEffects.WATER_BREATHING)>0) waterEffects.add(target);
        else waterEffects.remove(target);
    }
    private static int enhanced(LivingEntity target,Holder<MobEffect> effect) {
        var current=target.getEffect(effect);
        return current!=null && (current.getAmplifier()==1 || current.getAmplifier()==2) ? current.getAmplifier() : 0;
    }
    void tickWater() {
        for(var target:List.copyOf(waterEffects)) {
            if(!eligible(target) || target.level()!=game.maps().level()) { waterEffects.remove(target); continue; }
            int conduit=enhanced(target,MobEffects.CONDUIT_POWER), breathing=enhanced(target,MobEffects.WATER_BREATHING);
            if(conduit==0 && breathing==0) { waterEffects.remove(target); continue; }
            // Source waterpower precedes drown: синий артефакт создаёт воду до проверки дыхания.
            if(conduit>0) fillAir(target,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,conduit==1 ? 1 : 0),2);
            if(breathing==0) continue;
            var feet=target.level().getBlockState(target.blockPosition());
            if(feet.is(Blocks.WATER)) {
                target.removeEffect(MobEffects.WITHER);
                effect(target,MobEffects.STRENGTH,20,breathing,false);
                effect(target,MobEffects.RESISTANCE,20,breathing+1,false);
            } else if(feet.is(Blocks.AIR)) {
                effect(target,MobEffects.WITHER,60,3,true);
                if(breathing==2) effect(target,MobEffects.POISON,60,3,true);
            }
        }
    }
    void tickTeleports() {
        for(var target:List.copyOf(teleporting)) {
            if(!eligible(target) || target.level()!=game.maps().level()) { clearEffects(target); continue; }
            if(target instanceof ServerPlayer player && game.items().magic().rejectCustomEffect(player)) {
                clearEffects(target); continue;
            }
            int ticks=teleportTicks(target);
            if(ticks<=0) { teleporting.remove(target); continue; }
            if(target.entityTags().contains("sin_greed_active")) continue;
            spread(target,target.position(),10); score(target,"effect_rtp_1").set(Math.max(0,ticks-5));
        }
    }
    void stop() { waterEffects.clear(); for(var target:List.copyOf(teleporting)) clearEffects(target); }
}
