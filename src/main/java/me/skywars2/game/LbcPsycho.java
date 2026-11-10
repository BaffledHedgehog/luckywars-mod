package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Psycho/Психослеп: нативные заряды и конечные состояния без marker-сканирования мира. */
public final class LbcPsycho {
    public static final EntityType<Charge> TYPE=registerType();
    private static final DustColorTransitionOptions RAY_DUST=new DustColorTransitionOptions(0x4bffe1,0x132724,1);
    private static final double[][] SCARE_SHIFTS={{.5,0},{-.5,0},{0,.5},{0,-.5},{-.5,-.5},{.5,-.5},{-.5,.5},{.5,.5}};
    private static final int[][] SCARE_ANGLES={{5,0},{-5,0},{0,5},{0,-5},{5,5},{-5,5},{5,-5},{-5,-5}};
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final Map<LivingEntity,Affliction> effects=new IdentityHashMap<>();
    private final Map<ServerPlayer,Integer> rays=new IdentityHashMap<>();
    // Existing RP clip: 4.036333 seconds at pitch 2, renewed independently of the caster's counter.
    static final Identifier BEAM_SOUND=Identifier.parse("minecraft:lbcsounds.scream");
    static final int BEAM_SOUND_TICKS=40;
    private final Map<ServerPlayer,BeamAudio> beamAudio=new IdentityHashMap<>();
    private static final class BeamAudio {
        final Map<ServerPlayer,Integer> sources=new IdentityHashMap<>();
        int started;
    }
    private final Set<PsychoVex> vexes=new LinkedHashSet<>();
    private int ticks;
    private boolean statusRestoring;
    private int statusCallbacks;

    public static final class Charge extends Projectile implements PolymerEntity {
        int age,lifetime=100;
        boolean invisible,spray;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }
    private static final class Affliction {
        int scared,lookback,twitching,piss;
        boolean persistentPiss;
        ServerPlayer owner;
        String epoch;
        Double previousFollowRange;
        ItemStack forcedItem;
        InteractionHand forcedHand;
        int forcedTicks,lastUseTicks;
    }
    /** Враждебный vanilla Vex сохраняет настоящий последний удар и автора призыва. */
    private final class PsychoVex extends Vex {
        final ServerPlayer caster;
        PsychoVex(ServerPlayer caster) { this(caster,caster.level()); }
        PsychoVex(ServerPlayer caster,Level level) { super(EntityTypes.VEX,level); this.caster=caster; }
        @Override public boolean canAttack(LivingEntity target) {
            return caster!=null && game.playing() && game.items().currentEpoch(this)
                    && (target==caster ? game.canUseGameplayItems(caster) && !game.isObserver(caster)
                    : game.items().wands().target(caster,target,true))
                    && super.canAttack(target);
        }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) {
            return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target);
        }
        @Override public boolean shouldBeSaved() { return false; }
    }
    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:psycho_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                        .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type); return type;
    }
    static void register() { }
    LbcPsycho(GameController game) { this.game=game; }
    public int count() { return charges.size(); }
    /** Общая атрибуция учитывает только настоящий успешный удар, не попытку атаки Vex. */
    ServerPlayer owner(Entity entity) {
        return entity instanceof PsychoVex vex && vexes.contains(vex) && game.items().currentEpoch(vex)
                ? game.server().getPlayerList().getPlayer(vex.caster.getUUID()) : null;
    }
    List<Charge> snapshot() { return List.copyOf(charges); }
    int effectCount() { return effects.size(); }
    int rayCount(ServerPlayer owner) { return rays.getOrDefault(owner,0); }
    /** Player timer grant shared by food and the scream-ray caster. */
    void addRayTicks(ServerPlayer owner,int amount) {
        if(amount<=0 || !valid(owner) || game.items().magic().rejectCustomEffect(owner)) return;
        rays.merge(owner,amount,Integer::sum);
        game.items().mana().score(owner,"screamray").set(rays.get(owner));
    }
    /** Import the existing score authority without granting a second dose or resetting forced use. */
    void adoptRawFoodStatuses(ServerPlayer player) {
        if(!valid(player)||game.items().magic().rejectCustomEffect(player))return;
        var mana=game.items().mana();
        int ray=mana.hudValue(player,"screamray");if(ray>0)rays.put(player,ray);
        if(game.items().magic().rejectCustomDebuff(player)){clearDebuffs(player);return;}
        int scared=mana.hudValue(player,"scared"),lookback=mana.hudValue(player,"lookback"),
                twitching=mana.hudValue(player,"twitching"),piss=mana.hudValue(player,"piss");
        if(scared>0||lookback>0||twitching>0||piss>0){
            var effect=effects.computeIfAbsent(player,e->new Affliction());
            if(effect.owner==null)effect.owner=player;
            effect.epoch=game.items().epoch();effect.scared=scared;effect.lookback=lookback;
            effect.twitching=twitching;effect.piss=piss;
        }
    }
    private void sync(LivingEntity entity,Affliction effect){
        var mana=game.items().mana();mana.score(entity,"scared").set(effect.scared);
        mana.score(entity,"lookback").set(effect.lookback);mana.score(entity,"twitching").set(effect.twitching);
        mana.score(entity,"piss").set(effect.piss);
    }
    void addPlayerCounters(ServerPlayer player) {
        var current=effects.get(player);
        if(current!=null && current.forcedTicks>0 && !game.items().magic().rejectCustomDebuff(player)) current.forcedTicks++;
        afflict(player,player,1,1,1,1);
        addRayTicks(player,1);
    }
    int forcedUseTicks(ServerPlayer player) {
        var effect=effects.get(player); return effect==null?0:effect.forcedTicks;
    }
    /** Один принудительный клик принадлежит только уже наложенному Twitching. */
    void twitchClick(ServerPlayer player,boolean attack) {
        var effect=effects.get(player);
        if(game.items().magic().rejectCustomDebuff(player)){clearDebuffs(player);return;}
        if(effect==null || effect.twitching<=0) return;
        boolean wasUsing=player.isUsingItem();
        game.items().mouseAction(player,attack);
        if(!wasUsing && player.isUsingItem() && effects.get(player)==effect) {
            effect.forcedItem=player.getUseItem(); effect.forcedHand=player.getUsedItemHand();
            effect.forcedTicks=player.getRandom().nextInt(19)+2;
            effect.lastUseTicks=player.getTicksUsingItem();
        }
    }
    /** Source chooses one of six categories; it never rolls an unrestricted left/right click. */
    private void twitchRandomUse(ServerPlayer player){
        int kind=player.getRandom().nextInt(6)+1;
        var main=player.getMainHandItem();
        if(kind==1){
            var carrier=main.getItem();
            if(carrier instanceof eu.pb4.polymer.core.api.item.PolymerItem polymer)
                carrier=polymer.getPolymerItem(main,player.connection.getPacketContext());
            if(carrier==Items.CARROT_ON_A_STICK)twitchClick(player,false);
        }else if(kind==3){if(main.getItem() instanceof net.minecraft.world.item.BowItem)twitchClick(player,false);}
        else if(kind==4){if(main.getItem() instanceof net.minecraft.world.item.CrossbowItem
                &&!main.getOrDefault(DataComponents.CHARGED_PROJECTILES,net.minecraft.world.item.component.ChargedProjectiles.EMPTY).isEmpty())twitchClick(player,false);}
        else for(var hand:InteractionHand.values()){
            var stack=player.getItemInHand(hand);
            boolean selected=kind==2&&(stack.getItem() instanceof net.minecraft.world.item.PotionItem
                    ||stack.getItem() instanceof net.minecraft.world.item.ThrowablePotionItem)
                    ||kind==5&&stack.is(Items.SNOWBALL)||kind==6&&stack.is(Items.ENDER_PEARL);
            if(selected&&!player.isUsingItem())player.gameMode.useItem(player,player.level(),stack,hand);
        }
    }
    int effectValue(LivingEntity entity,String name) { return hudValue(entity,name); }
    public int hudValue(LivingEntity entity,String name) {
        if(name.equals("screamray")) return entity instanceof ServerPlayer player?rays.getOrDefault(player,0):0;
        var effect=effects.get(entity); if(effect==null) return 0;
        return switch(name) { case "scared"->effect.scared; case "lookback"->effect.lookback;
            case "twitching"->effect.twitching; case "piss"->effect.piss; default->0; };
    }
    /** Reduction of a source counter preserves attribution, other afflictions and forced-use phase. */
    void reduceSourceStatus(ServerPlayer player,String name,int value) {
        if(value>0&&LbcParasite.active(player)){clearStatus(player,name);return;}
        if(value<=0){clearStatus(player,name);game.items().mana().score(player,name).set(value);return;}
        var effect=effects.get(player);
        if(effect!=null)switch(name){case "scared"->effect.scared=value;case "lookback"->effect.lookback=value;
            case "twitching"->effect.twitching=value;default->throw new IllegalArgumentException(name);}
        game.items().mana().score(player,name).set(value);
    }
    /** A random cleanse removes one owned status without releasing unrelated effects or manual item use. */
    void clearStatus(LivingEntity entity,String name) {
        if(!name.equals("lookback")&&!name.equals("piss")&&!name.equals("scared")&&!name.equals("twitching"))return;
        // Keep source scoreboard consumers coherent even when no native affliction remains.
        game.items().mana().score(entity,name).set(0);
        var effect=effects.get(entity); if(effect==null)return;
        switch(name) {
            case "lookback" -> effect.lookback=0;
            case "piss" -> { effect.piss=0; restoreFollowRange(entity,effect); }
            case "scared" -> effect.scared=0;
            case "twitching" -> {
                effect.twitching=0;
                if(entity instanceof ServerPlayer player)releaseForcedUse(player,effect);
                else forgetForcedUse(effect);
            }
            default -> { return; }
        }
        if(effect.lookback<=0&&effect.scared<=0&&effect.twitching<=0&&effect.piss<=0&&effect.forcedItem==null)
            removeEffect(entity);
    }
    private boolean valid(ServerPlayer owner) { return owner!=null && game.items().mana().visible(owner); }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")
                || game.items().frost().isFrozen(owner) || game.items().magic().suppressed(owner);
    }
    private boolean target(ServerPlayer owner,Entity entity) {
        return game.items().wands().target(owner,entity)
                && !game.items().magic().suppressedAt(owner.level(),entity.position())
                && !entity.entityTags().contains("nomagic_active");
    }
    private boolean projectileTarget(ServerPlayer owner,Entity entity) {
        return game.items().wands().projectileTarget(owner,entity)
                && !game.items().magic().suppressedAt(owner.level(),entity.position())
                && !entity.entityTags().contains("nomagic_active");
    }
    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean donor,boolean academy) {
        if(!valid(owner) || paused(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        var mana=game.items().mana();
        if(academy && owner.isShiftKeyDown()) {
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),4);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            owner.sendSystemMessage(Component.translatable(donor?switch(mode) {
                case 2->"mode_screamer"; case 3->"mode_look_back"; case 4->"mode_scare"; default->"mode_sound";
            }:switch(mode) {
                case 2->"mode_mindblowing_ray"; case 3->"mode_spray"; case 4->"mode_invisible_charge"; default->"mode_default";
            }));
            return true;
        }
        int cost=mana.score(ScoreHolder.forNameOnly(donor?"psycho_wand_mana_use":"psychoblind_wand_mana_use"),"lbc.math").get();
        if(!mana.trySpendWand(owner,cost)) return false;
        int mode=academy?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4):0;
        if(donor) psycho(owner,hand,stack,mode);
        else {
            psychoblind(owner,mode);
            if(!academy && owner.isShiftKeyDown() && LbcAcademy.trial(stack)) {
                afflict(owner,owner,150,40,150,150); game.items().screamer(owner,owner);
                game.items().academy().ready(owner,hand,stack);
            }

        }
        game.items().wands().wandCasted(owner); return true;
    }
    void psychoblind(ServerPlayer owner,int mode) {
            if(mode==2) addRayTicks(owner,40);
            else if(mode==3) spawn(owner,owner.getEyePosition(),owner.getLookAngle(),false,true,100);
            else spread(owner,mode==1?4:3,mode==4);
            if(mode!=2) {
                LbcOverload.sound(owner,"minecraft:item.trident.riptide_1",.7f,0,0);
                LbcOverload.sound(owner,"minecraft:item.trident.riptide_1",.7f,1,0);
            }
    }

    void emitAt(ServerPlayer owner,int mode,Vec3 origin,Vec3 direction){
        if(mode==2){rayAt(owner,origin,direction,39);return;}
        if(mode==3)spawn(owner,origin,direction,false,true,100);
        else{
            int count=mode==1?4:3;
            float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
            for(int i=0;i<count;i++){
                Vec3 aim=Vec3.directionFromRotation(pitch+(owner.getRandom().nextFloat()*2-1)*20,
                        yaw+(owner.getRandom().nextFloat()*2-1)*30);
                spawn(owner,origin.add(direction.scale(i*.01)),aim.scale(2),mode==4,false,100);
            }
        }
        LbcOverload.sound(owner,origin,"minecraft:item.trident.riptide_1",.7f,0,0);
        LbcOverload.sound(owner,origin,"minecraft:item.trident.riptide_1",.7f,1,0);
    }
    /** Skill36 terminal: one ordinary visible charge; shared flight/status/audio ownership follows. */
    boolean emitDoppel(ServerPlayer owner,Vec3 origin,Vec3 direction){
        if(!valid(owner)||paused(owner)||!Double.isFinite(direction.x)||!Double.isFinite(direction.y)
                ||!Double.isFinite(direction.z)||direction.lengthSqr()<1.0e-12)return false;
        int before=charges.size();
        spawn(owner,origin,direction.normalize().scale(2),false,false,100);
        if(charges.size()==before)return false;
        LbcOverload.sound(owner,origin,"minecraft:item.trident.riptide_1",.5f,.5f,0);
        LbcOverload.sound(owner,origin,"minecraft:item.trident.riptide_1",.5f,1.5f,0);
        return true;
    }
    /** Keep final launch selection on the same target policy as native Psycho collision. */
    boolean doppelTarget(ServerPlayer owner,Entity entity){
        return projectileTarget(owner,entity)&&game.items().lifetimes().target(owner,entity);
    }
    void psycho(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode) {
        int roll=mode==0?owner.getRandom().nextInt(6)+1:mode==1?1:mode+2;
        boolean trial=false;
        for(var entity:owner.level().getEntities(owner,owner.getBoundingBox().inflate(30),e->target(owner,e)
                && e.position().distanceToSqr(owner.position())<=900 && e.position().distanceToSqr(owner.position())>=.0001)) {
            if(roll<=3 && entity instanceof ServerPlayer player) phantomSound(player);
            else if(roll==4 && entity instanceof LivingEntity living) game.items().screamer(living,owner);
            else if(entity instanceof LivingEntity living && game.items().lifetimes().target(owner,living)) {
                if(roll==5) { afflict(living,owner,0,9,0,0); privateSound(living,"minecraft:ambient.cave",1,1,living.position().subtract(living.getLookAngle().scale(3))); }
                if(roll==6) {
                    afflict(living,owner,100,0,0,0);
                    for(var offset:List.of(new Vec3(5,0,0),new Vec3(-5,0,0),new Vec3(0,0,5),new Vec3(0,0,-5)))
                        privateSound(living,"minecraft:entity.ghast.scream",1,0,living.position().add(offset));
                }
            }
            if(entity instanceof LivingEntity living && roll<=3 && effectValue(living,"scared")>0
                    && effectValue(living,"lookback")>0 && living.hasEffect(MobEffects.BLINDNESS)) trial=true;
            if(entity instanceof LivingEntity living && game.items().lifetimes().target(owner,living) && living.getHealth()<2) execute(owner,living);
        }
        if(mode==0 && LbcAcademy.trial(stack) && trial) game.items().academy().ready(owner,hand,stack);
        owner.level().sendParticles(ParticleTypes.SOUL,owner.getX(),owner.getY()+1,owner.getZ(),100,40,40,40,0);
        owner.level().sendParticles(ParticleTypes.SOUL,owner.getX(),owner.getY()+1,owner.getZ(),30,.5,1,.5,0);
    }
    private void execute(ServerPlayer owner,LivingEntity target) {
        var vex=new PsychoVex(owner); vex.setPos(target.position());
        vex.finalizeSpawn(owner.level(),owner.level().getCurrentDifficultyAt(vex.blockPosition()),
                net.minecraft.world.entity.EntitySpawnReason.COMMAND,null);
        // Finalization adds a vanilla iron sword; this ghost is deliberately unarmed.
        vex.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        vex.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,0);
        vex.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2_000_000_000); vex.setHealth(2_000_000_000f);
        vex.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(200);
        vex.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(100);
        vex.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(1);
        vex.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1);
        vex.getAttribute(Attributes.ARMOR).setBaseValue(40); vex.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(40);
        if(vex.getAttribute(Attributes.ATTACK_KNOCKBACK)!=null) vex.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);
        vex.setCustomName(Component.translatable("evil_vex").withStyle(net.minecraft.ChatFormatting.GRAY));
        vex.setPersistenceRequired(); vex.setLimitedLife(2_000_000); game.items().markCurrentItemEpoch(vex);
        if(owner.level().addFreshEntity(vex)) vexes.add(vex);
        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC_KILL),owner);
        target.hurtServer(owner.level(),source,Float.MAX_VALUE);
        game.items().mana().award(owner,"lbc:true_advancements/staffs/psycho");
    }
    private void spread(ServerPlayer owner,int count,boolean invisible) {
        for(int i=0;i<count;i++) {
            Vec3 direction=Vec3.directionFromRotation(owner.getXRot()+(owner.getRandom().nextFloat()*2-1)*20,
                    owner.getYRot()+(owner.getRandom().nextFloat()*2-1)*30);
            spawn(owner,owner.getEyePosition().add(owner.getLookAngle().scale(i*.01)),direction.scale(2),invisible,false,100);
        }
    }
    private void spawn(ServerPlayer owner,Vec3 point,Vec3 velocity,boolean invisible,boolean spray,int lifetime) {
        if(!owner.level().hasChunkAt(BlockPos.containing(point))) return;
        var charge=new Charge(TYPE,owner.level()); charge.setOwner(owner); charge.setPos(point);
        charge.setNoGravity(true); charge.setDeltaMovement(velocity); charge.invisible=invisible;
        charge.spray=spray; charge.lifetime=lifetime; charge.entityTags().add("magic");
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) charges.add(charge);
    }
    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || charge.level()!=game.maps().level()
                    || !(charge.getOwner() instanceof ServerPlayer)) charge.discard();
            else charges.add(charge);
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Charge charge) { charges.remove(charge); if(!charge.isRemoved()) charge.discard(); }
        if(entity instanceof LivingEntity living) removeEffect(living);
        if(entity instanceof ServerPlayer player) clearBeamAudio(player);
        if(entity instanceof PsychoVex vex) vexes.remove(vex);
    }
    public void tick() {
        if(statusRestoring)return;
        ticks++; tickCharges();
        statusCallbacks++;try{tickEffects();}finally{statusCallbacks--;}
        tickRays();
        tickBeamAudio();
        for(var vex:List.copyOf(vexes)) if(vex.isRemoved() || !game.items().currentEpoch(vex)) {
            vexes.remove(vex); vex.discard();
        }
    }
    private void tickCharges() {
        for(var charge:List.copyOf(charges)) {
            if(charge.isRemoved() || !(charge.getOwner() instanceof ServerPlayer owner) || !valid(owner)
                    || owner.level()!=charge.level() || !game.items().currentEpoch(charge)) { remove(charge); continue; }
            if(paused(owner)) continue;
            Vec3 from=charge.position(),to=from.add(charge.getDeltaMovement());
            if(!owner.level().hasChunkAt(BlockPos.containing(from)) || LbcTerra.wallHit(owner.level(),from,to,state->false)!=null) { remove(charge); continue; }
            double suppression=game.items().magic().suppressionFraction(owner.level(),from,to);
            if(Double.isFinite(suppression)) { remove(charge); continue; }
            // Снаряды исходника проходят сквозь блоки; один swept query вместо восьми поисков .25.
            var candidates=owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(1),e->projectileTarget(owner,e) && game.items().lifetimes().target(owner,e));
            candidates.removeIf(e->e.getBoundingBox().inflate(.5).clip(from,to).isEmpty() && !e.getBoundingBox().inflate(.5).contains(from));
            candidates.sort(Comparator.comparingDouble(e->e.position().distanceToSqr(from)));
            int hits=0;
            for(var target:candidates) { impact(charge,owner,target); if(++hits==2) break; }
            charge.setPos(to);
            if(!charge.invisible) for(int i=0;i<= (charge.spray?4:8);i++) {
                var p=from.lerp(to,i/(charge.spray?4.0:8));
                owner.level().sendParticles(ParticleTypes.SQUID_INK,p.x,p.y,p.z,1,0,0,0,0);
                owner.level().sendParticles(ParticleTypes.GLOW_SQUID_INK,true,false,p.x,p.y,p.z,1,0,0,0,0);
            }
            if(charge.spray) {
                float yaw=owner.getRandom().nextFloat()*360,pitch=owner.getRandom().nextFloat()*180-90;
                spawn(owner,to,Vec3.directionFromRotation(pitch,yaw).scale(2),false,false,20);
                LbcOverload.sound(owner,to,"minecraft:item.trident.riptide_1",.3f,0,0);
                LbcOverload.sound(owner,to,"minecraft:item.trident.riptide_1",.2f,1,0);
            }
            if(hits>0 && !charge.spray || ++charge.age>=charge.lifetime) { remove(charge); continue; }
            if(!charge.spray) home(owner,charge);
        }
    }
    private void home(ServerPlayer owner,Charge charge) {
        var candidates=owner.level().getEntitiesOfClass(LivingEntity.class,charge.getBoundingBox().inflate(20),e->target(owner,e) && game.items().lifetimes().target(owner,e)
                && !e.entityTags().contains("strict_map_object") && e.position().distanceToSqr(charge.position())<=400);
        if(candidates.stream().anyMatch(e->e.position().distanceToSqr(charge.position())<100)) return;
        candidates.stream().min(Comparator.comparingDouble(e->e.position().distanceToSqr(charge.position())))
                .ifPresent(e->charge.setDeltaMovement(e.getEyePosition().subtract(charge.position()).normalize().scale(2)));
    }
    private void impact(Charge charge,ServerPlayer owner,LivingEntity target) {
        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),charge,owner);
        target.hurtServer(owner.level(),source,4);
        target.addEffect(ItemEffectLevels.create(MobEffects.DARKNESS,400,0),owner);
        afflict(target,owner,15,5,10,0);
        privateSound(target,"minecraft:ambient.cave",2,0,target.position());
        privateSound(target,"minecraft:ambient.cave",2,2,target.position());
        if(target instanceof ServerPlayer player) player.level().sendParticles(player,ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,true,false,
                target.getX(),target.getY()+1,target.getZ(),200,0,0,0,1);
    }
    void afflict(LivingEntity entity,ServerPlayer owner,int scared,int lookback,int twitching,int piss) {
        if(entity instanceof ServerPlayer player && game.items().magic().rejectCustomDebuff(player)) return;
        var effect=effects.computeIfAbsent(entity,e->new Affliction()); effect.owner=owner; effect.epoch=game.items().epoch();
        effect.scared+=scared; effect.lookback+=lookback; effect.twitching+=twitching; effect.piss+=piss;
        sync(entity,effect);
    }
    /** Yellow Gaycaster sets the existing source counter rather than extending it. */
    boolean yellow(LivingEntity entity,ServerPlayer owner){
        return yellow(entity,owner,100);
    }
    boolean yellow(LivingEntity entity,ServerPlayer owner,int ticks){
        if(entity instanceof ServerPlayer player&&game.items().magic().rejectCustomDebuff(player))return false;
        afflict(entity,owner,0,0,0,0);effects.get(entity).piss=ticks;effects.get(entity).persistentPiss=false;sync(entity,effects.get(entity));return true;
    }
    boolean yellowPersistent(LivingEntity entity,ServerPlayer owner,int ticks){
        if(!yellow(entity,owner,ticks))return false;effects.get(entity).persistentPiss=true;return true;
    }
    private void tickEffects() {
        for(var entry:List.copyOf(effects.entrySet())) {
            var entity=entry.getKey(); var effect=entry.getValue();
            // Canonical replay can retain a disconnected body until the same player is republished.
            if(MatchOfflinePlayers.retained(entity)||effect.owner!=null&&MatchOfflinePlayers.retained(effect.owner))continue;
            if(game.items().polymorph().masked(entity))continue;
            if(!entity.isAlive() || entity.isRemoved() || !game.items().currentEpoch(effect.epoch)
                    || entity.level()!=game.maps().level()&&!effect.persistentPiss || entity instanceof ServerPlayer player && game.items().magic().cleansesCustomEffects(player) || LbcParasite.active(entity)
                    || entity.isSpectator() || entity.entityTags().contains("spectator")) { removeEffect(entity); continue; }
            if(LbcGreed.active(entity) || game.items().wands().timeStopped() || entity.entityTags().contains("time_stopped") || game.items().frost().isFrozen(entity)) continue;
            if(entity instanceof ServerPlayer player && effect.forcedItem!=null) {
                if(!sameForcedUse(player,effect) || player.getTicksUsingItem()<effect.lastUseTicks) forgetForcedUse(effect);
                else {
                    effect.lastUseTicks=player.getTicksUsingItem();
                    if(--effect.forcedTicks<=0) releaseForcedUse(player,effect);
                }
            }
            if(ticks%5==0) {
                if(effect.lookback>0) { rotate(entity,20,0); effect.lookback--; }
                if(effect.scared>0) { scareTick(entity); effect.scared=Math.max(0,effect.scared-5); }
            }
            if(effect.piss>0 && !entity.entityTags().contains("sin_greed_active")) {
                // effects/tick: anchored eyes positioned ^ ^ ^.3, shared by both audiences.
                Vec3 particleOrigin=entity.getEyePosition().add(entity.getLookAngle().scale(.3));
                var piss=new ItemStack(Items.STICK); piss.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:piss"));
                if(entity instanceof ServerPlayer player) {
                    player.level().sendParticles(player,new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(piss)),true,false,
                            particleOrigin.x,particleOrigin.y,particleOrigin.z,200,.3,.2,.3,0);
                    game.items().mana().award(player,"lbc:true_advancements/potions/effect_piss");
                }
                for(var viewer:game.maps().level().players()) if(viewer!=entity && viewer.position().distanceToSqr(particleOrigin)>=.01)
                    game.maps().level().sendParticles(viewer,new ItemParticleOption(ParticleTypes.ITEM,Items.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.YELLOW)),true,false,
                            particleOrigin.x,particleOrigin.y,particleOrigin.z,3,.2,.2,.2,0);
                entity.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,20,0,false,false),effect.owner);
                entity.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,20,0,false,false),effect.owner);
                if(entity instanceof Mob mob && mob.getAttribute(Attributes.FOLLOW_RANGE)!=null) {
                    var range=mob.getAttribute(Attributes.FOLLOW_RANGE);
                    if(effect.previousFollowRange==null) effect.previousFollowRange=range.getBaseValue();
                    range.setBaseValue(2);
                }
                effect.piss--;
                if(effect.piss<=0) restoreFollowRange(entity,effect);
            }
            if(ticks%2==0 && effect.twitching>0 && !(entity instanceof ServerPlayer)) effect.twitching--;
            if(ticks%2==0 && effect.twitching>0 && entity instanceof ServerPlayer player) {
                if(player.getRandom().nextFloat()<.01f) {
                    LbcChaosRewardItems.twitch(game,player,player.position());
                }
                if(player.getRandom().nextFloat()<.1f) player.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.WHITE)),
                        player.getX(),player.getY()+.8,player.getZ(),10,.1,.1,.1,0);
                // A matching hand category is only one of six: effective chance .5/6, not .5.
                // Sound and particles above have their own independent legacy rolls.
                if(player.getRandom().nextFloat()<.5f)twitchRandomUse(player);
                if(player.getRandom().nextFloat()<.4f) LbcOverload.sound(player,"minecraft:entity.slime.jump_small",.5f,1,0);
                effect.twitching--;
            }
            sync(entity,effect);
            if(effect.lookback<=0 && effect.scared<=0 && effect.twitching<=0 && effect.piss<=0 && effect.forcedItem==null) removeEffect(entity);
        }
    }
    /** Возвращаем только принадлежащее этому статусу значение, не затирая чужую позднюю запись. */
    private void restoreFollowRange(LivingEntity entity,Affliction effect) {
        if(effect.previousFollowRange==null) return;
        var range=entity.getAttribute(Attributes.FOLLOW_RANGE);
        if(range!=null && range.getBaseValue()==2) range.setBaseValue(effect.previousFollowRange);
        effect.previousFollowRange=null;
    }
    private void removeEffect(LivingEntity entity) {
        var effect=effects.remove(entity);
        if(effect!=null) {
            effect.scared=effect.lookback=effect.twitching=effect.piss=0;sync(entity,effect);
            restoreFollowRange(entity,effect);
            if(entity instanceof ServerPlayer player) releaseForcedUse(player,effect);
        }
    }
    private boolean sameForcedUse(ServerPlayer player,Affliction effect) {
        return effect.forcedItem!=null && player.isUsingItem() && player.getUseItem()==effect.forcedItem
                && player.getUsedItemHand()==effect.forcedHand && player.getItemInHand(effect.forcedHand)==effect.forcedItem;
    }
    private void forgetForcedUse(Affliction effect) {
        effect.forcedItem=null; effect.forcedHand=null; effect.forcedTicks=0; effect.lastUseTicks=0;
    }
    /** Отпускаем лишь наш неизменённый stack/hand; ручное использование игрока не трогаем. */
    private void releaseForcedUse(ServerPlayer player,Affliction effect) {
        boolean release=sameForcedUse(player,effect);
        forgetForcedUse(effect);
        if(release) player.releaseUsingItem();
    }
    private void scareTick(LivingEntity entity) {
        for(var shift:SCARE_SHIFTS) if(entity.getRandom().nextFloat()<.15f) {
            Vec3 p=entity.position().add(shift[0],0,shift[1]);
            if(entity.level().hasChunkAt(BlockPos.containing(p))) entity.teleportTo(p.x,p.y,p.z);
        }
        for(var angle:SCARE_ANGLES) if(entity.getRandom().nextFloat()<.15f) rotate(entity,angle[0],angle[1]);
    }
    private void rotate(LivingEntity entity,float yaw,float pitch) {
        if(entity instanceof ServerPlayer player) player.forceSetRotation(yaw,true,pitch,true);
        else { entity.setYRot(entity.getYRot()+yaw); entity.setXRot(entity.getXRot()+pitch); entity.setYHeadRot(entity.getYRot()); }
    }
    private void tickRays() {
        for(var owner:List.copyOf(rays.keySet())) {
            if(game.items().polymorph().masked(owner))continue;
            if(!valid(owner) || game.items().magic().rejectCustomEffect(owner)) { rays.remove(owner);game.items().mana().score(owner,"screamray").set(0); continue; }
            if(paused(owner)) continue;
            int remaining=rays.get(owner)-1;
            Vec3 from=owner.getEyePosition(),direction=owner.getLookAngle(),to=from.add(direction.scale(99));
            rayAt(owner,from,direction,remaining);
            if(remaining<=0) rays.remove(owner); else rays.put(owner,remaining);
            game.items().mana().score(owner,"screamray").set(Math.max(0,remaining));
        }
    }
    private void phantomSound(ServerPlayer player) {
        int roll=player.getRandom().nextInt(16);
        String sound=roll<4?"minecraft:entity.player.big_fall":roll<8?"minecraft:block.stone.step":roll==8?
                "minecraft:entity.creeper.primed":roll<13?"minecraft:block.stone.break":"minecraft:block.grass.place";
        Vec3 look=player.getLookAngle(),right=new Vec3(look.z,0,-look.x).normalize();
        Vec3 position=player.position().add(switch(roll) {
            case 0,7,12->look.scale(-5); case 1,6,11->look.scale(5);
            case 2,5,10->right.scale(-5); case 3,4,9->right.scale(5);
            case 8->look.scale(-4); case 13->right.scale(5).add(look.scale(5));
            case 14->right.scale(5).add(look.scale(-5)); default->right.scale(-5).add(look.scale(-5));
        });
        privateSound(player,sound,1,1,position);
    }
    private void rayAt(ServerPlayer owner,Vec3 from,Vec3 direction,int remaining){
        Vec3 to=from.add(direction.scale(99));
        double suppression=game.items().magic().suppressionFraction(owner.level(),from,to);
        if(Double.isFinite(suppression)) to=from.lerp(to,suppression);
        var wall=LbcTerra.wallHit(owner.level(),from,to);
        if(wall!=null) to=wall.getLocation();
        double length=from.distanceTo(to);
        for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(3.5),e->projectileTarget(owner,e)
                && game.items().lifetimes().target(owner,e))) {
            double projection=Math.clamp(target.position().subtract(from).dot(direction),0,length);
            if(target.position().distanceToSqr(from.add(direction.scale(projection)))>12.25) continue;
            // (user) Каждая подходящая цель получает действие один раз за тик луча.
            afflict(target,owner,1,1,0,1); game.items().screamer(target,owner);
            var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC),owner);
            target.hurtServer(owner.level(),source,.01f);
            if(target instanceof ServerPlayer player) beamSound(owner,player);
        }
        for(double d=0;d<=length;d+=.33) {
            var p=from.add(direction.scale(d));
            owner.level().sendParticles(RAY_DUST,true,false,p.x,p.y,p.z,1,1,1,1,0);
        }
    }
    /** Multiple overlapping beams renew one target-local voice; caster and bystanders receive none. */
    private void beamSound(ServerPlayer owner,ServerPlayer target) {
        var audio=beamAudio.computeIfAbsent(target,key->new BeamAudio());
        boolean start=audio.sources.isEmpty() || ticks-audio.started>=BEAM_SOUND_TICKS;
        audio.sources.put(owner,ticks);
        if(start) {
            audio.started=ticks;
            // Vanilla has no packet loop/seek control. At pitch 2 this existing clip lasts 40.363 ticks;
            // forty-tick renewal leaves ~18 ms overlap, with unavoidable network/TPS timing variation.
            target.connection.send(new ClientboundSoundEntityPacket(Holder.direct(SoundEvent.createVariableRangeEvent(BEAM_SOUND)),
                    SoundSource.MASTER,target,3,2,target.getRandom().nextLong()));
        }
    }
    private void tickBeamAudio() {
        for(var entry:List.copyOf(beamAudio.entrySet())) {
            var target=entry.getKey(); var audio=entry.getValue();
            // One tick of grace also covers library beams emitted after this service's tick.
            audio.sources.entrySet().removeIf(source->ticks-source.getValue()>1 || !valid(source.getKey())
                    || source.getKey().level()!=target.level() || paused(source.getKey())
                    || game.items().polymorph().masked(source.getKey()));
            if(audio.sources.isEmpty() || !target.isAlive() || target.isRemoved() || !valid(target)) stopBeamAudio(target);
        }
    }
    /** End-of-server-frame cleanup also runs while the gameplay scheduler is frozen. */
    public void audioMaintenance() {
        if(game.server().tickRateManager().isFrozen() || !game.playing())silenceBeamAudio();
        else tickBeamAudio();
    }
    /** Retire future renewals without touching ray clocks; the last short clip finishes naturally. */
    public void silenceBeamAudio() {
        for(var target:List.copyOf(beamAudio.keySet()))stopBeamAudio(target);
    }
    private void stopBeamAudio(ServerPlayer target) {
        // Vanilla cannot stop an individual instance; stopping this event would kill unrelated screams.
        beamAudio.remove(target);
    }
    private void clearBeamAudio(ServerPlayer player) {
        stopBeamAudio(player);
        for(var entry:List.copyOf(beamAudio.entrySet())) {
            entry.getValue().sources.remove(player);
            if(entry.getValue().sources.isEmpty())stopBeamAudio(entry.getKey());
        }
    }
    private void privateSound(LivingEntity target,String id,float volume,float pitch,Vec3 position) {
        if(target instanceof ServerPlayer player) player.connection.send(new ClientboundSoundPacket(
                Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(id))),SoundSource.MASTER,
                position.x,position.y,position.z,volume,pitch,player.getRandom().nextLong()));
    }
    private void remove(Charge charge) { charges.remove(charge); charge.discard(); }
    public void clearDebuffs(LivingEntity entity){
        removeEffect(entity);
        for(String name:List.of("scared","lookback","twitching","piss"))if(game.items().mana().hudValue(entity,name)!=0)game.items().mana().score(entity,name).set(0);
    }
    public void clearEffects(LivingEntity entity){if(entity instanceof ServerPlayer player)clearEffects(player);else removeEffect(entity);}
    public void clearEffects(ServerPlayer owner) { removeEffect(owner); rays.remove(owner); clearBeamAudio(owner); game.items().mana().score(owner,"screamray").set(0); }
    public void forget(ServerPlayer owner) {
        clearEffects(owner);
        for(var entry:List.copyOf(effects.entrySet())) if(entry.getValue().owner==owner){
            var effect=entry.getValue();if(!effect.persistentPiss)removeEffect(entry.getKey());
            else{if(entry.getKey() instanceof ServerPlayer player)releaseForcedUse(player,effect);effect.owner=null;
                effect.scared=effect.lookback=effect.twitching=0;sync(entry.getKey(),effect);}
        }
        for(var charge:List.copyOf(charges)) if(charge.getOwner()==owner) remove(charge);
        for(var vex:List.copyOf(vexes)) if(vex.caster==owner) { vexes.remove(vex); vex.discard(); }
    }
    public void stop() {
        statusRestoring=false;
        silenceBeamAudio();
        for(var charge:List.copyOf(charges)) remove(charge);
        for(var vex:List.copyOf(vexes)) vex.discard(); vexes.clear();
        for(var entity:List.copyOf(effects.keySet())) removeEffect(entity);
        rays.clear(); ticks=0;
    }

    void checkpointBoundary(){if(statusCallbacks!=0)throw new IllegalStateException("Finish native Psycho status callbacks before checkpointing");}
    /** Canonical owners restore effects, attributes, inventory and item use; only detach private future counters. */
    void prepareFrozen(){checkpointBoundary();statusRestoring=true;effects.clear();}
    void publishFrozen(){checkpointBoundary();statusRestoring=false;}
    void cancelFrozen(){checkpointBoundary();statusRestoring=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record Status(MatchDamageSources.Ref target,MatchDamageSources.Ref owner,
                int scared,int lookback,int twitching,int piss,boolean persistentPiss,String epoch,
                Double previousFollowRange,MatchItemStackLinks.Ref forcedItem,InteractionHand forcedHand,
                int forcedTicks,int lastUseTicks){}
        private final List<Status> statuses=new ArrayList<>();
        private final int phase=ticks;
        Checkpoint(MatchSpellCheckpoint.Links links){
            for(var entry:effects.entrySet()){
                var target=entry.getKey();var effect=entry.getValue();
                if(links.omitted(target))continue;
                statuses.add(new Status(links.key(target),links.key(effect.owner),effect.scared,effect.lookback,
                        effect.twitching,effect.piss,effect.persistentPiss,effect.epoch,effect.previousFollowRange,
                        effect.forcedItem==null?null:links.stack(game,effect.forcedItem),effect.forcedHand,
                        effect.forcedTicks,effect.lastUseTicks));
            }
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var next=new IdentityHashMap<LivingEntity,Affliction>();
            // Build independent images and validate all canonical aliases before any service installs its map.
            for(var saved:statuses){
                var target=(LivingEntity)links.get(saved.target);var owner=(ServerPlayer)links.get(saved.owner);
                var effect=new Affliction();effect.owner=owner;effect.epoch=saved.epoch;
                effect.scared=saved.scared;effect.lookback=saved.lookback;effect.twitching=saved.twitching;
                effect.piss=saved.piss;effect.persistentPiss=saved.persistentPiss;
                effect.previousFollowRange=saved.previousFollowRange;effect.forcedHand=saved.forcedHand;
                effect.forcedItem=saved.forcedItem==null?null:saved.forcedItem.get(links);
                effect.forcedTicks=saved.forcedTicks;effect.lastUseTicks=saved.lastUseTicks;
                if(next.put(target,effect)!=null)throw new IllegalStateException("Duplicate restored Psycho status body");
            }
            return ()->{statusRestoring=true;effects.clear();effects.putAll(next);ticks=phase;};
        }
    }

    static boolean cloneVex(Entity actor){return actor instanceof LbcPsycho.PsychoVex;}
    LbcCloneSkills.ActorImage cloneVexImage(Entity actor) {
        if(!(actor instanceof PsychoVex source))return null;
        var caster=source.caster;
        return new LbcCloneSkills.ActorImage() {
            public Entity create(Entity original,ServerLevel level){
                return new PsychoVex(caster,level);
            }
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){vexes.add((PsychoVex)clone);}
            public void rollback(Entity clone){vexes.remove(clone);}
        };
    }
    LbcCloneSkills.ActorImage cloneStatusImage(Entity actor) {
        if(!(actor instanceof LivingEntity living))return null;
        var source=effects.get(living);if(source==null)return null;
        int scared=source.scared,lookback=source.lookback,twitching=source.twitching,piss=source.piss;
        boolean persistent=source.persistentPiss;var owner=source.owner;var epoch=source.epoch;
        var follow=source.previousFollowRange;var hand=source.forcedHand;
        var item=source.forcedItem==null?null:source.forcedItem.copy();
        int forced=source.forcedTicks,last=source.lastUseTicks;
        return new LbcCloneSkills.ActorImage() {
            public void install(Entity original,Entity clone,Map<Entity,Entity> family) {
                var target=(LivingEntity)clone;var effect=new Affliction();
                effect.scared=scared;effect.lookback=lookback;effect.twitching=twitching;effect.piss=piss;
                effect.persistentPiss=persistent;effect.owner=owner;effect.epoch=epoch;effect.previousFollowRange=follow;
                effect.forcedHand=hand;effect.forcedItem=item==null?null:item.copy();effect.forcedTicks=forced;effect.lastUseTicks=last;
                effects.put(target,effect);sync(target,effect);
            }
            public void rollback(Entity clone){effects.remove(clone);}
        };
    }

}
