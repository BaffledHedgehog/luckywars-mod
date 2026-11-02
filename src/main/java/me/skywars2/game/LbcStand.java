package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Получение/сохранение Stand, часы, награда Overheaven и ORAORA; остановкой мира владеет LbcTimeStop. */
public final class LbcStand {
    public static final Item ARROW = registerArrow();
    public static final Item ORAORA = registerOraora();
    public static final Item CLOCKS = registerClocks();
    public static final Item HOMING_LIGHTNING = registerHomingLightning();
    private static final CompoundTag CLOCKS_FLAG=new CompoundTag();
    private static final CompoundTag FIELD_FLAG=new CompoundTag(), LIFESTEALER_FLAG=new CompoundTag();
    static { CLOCKS_FLAG.putInt("stand_clocks",1); FIELD_FLAG.putInt("homing_lightning",1); LIFESTEALER_FLAG.putInt("lifestealer",1); }
    private static final String FIST_TAG = "skywars2.stand.fist";
    private static final double[][] OFFSETS = {
            {2,0,0},{-2,0,0},{0,2,0},{1.5,1.5,0},{-1.5,1.5,0},
            {0,0,1.6},{1,0,1},{0,-1,1},{-1,0,1},{0,1,1}
    };
    private static final CompoundTag NORMAL_FIST = fistPreset("lbc:ora"), HEAVEN_FIST = fistPreset("lbc:overheaven");
    private final GameController game;
    private final Map<UUID,Burst> bursts = new HashMap<>();
    private final Set<Fist> fists = new LinkedHashSet<>();
    private final Map<ServerPlayer,List<LivingEntity>> tickTargets = new HashMap<>();
    private final Map<UUID,RetainedStand> retained = new HashMap<>();
    private int ticks;
    private static final int BURST_FISTS = 100;

    private static final class RetainedStand {
        ServerPlayer owner;
        final String epoch;
        boolean started, empowered, resistanceOwned;
        int grantDelay, lightningCooldown, falling;
        RetainedStand(ServerPlayer owner,String epoch) { this.owner=owner; this.epoch=epoch; }
    }

    private static final class Burst {
        final ServerPlayer owner;
        final String epoch;
        int cooldown=800, remaining, delay=2;
        boolean retainedOffline;
        Burst(ServerPlayer owner,String epoch,int count) { this.owner=owner; this.epoch=epoch; remaining=count; }
    }
    private static final class Fist {
        final ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        final Display.ItemDisplay display;
        final int variant;
        Vec3 direction;
        int age;
        Fist(ServerPlayer owner,String epoch,Display.ItemDisplay display,Vec3 direction,int variant) {
            this(owner,owner.level(),epoch,display,direction,variant);
        }
        Fist(ServerPlayer owner,ServerLevel level,String epoch,Display.ItemDisplay display,Vec3 direction,int variant){
            this.owner=owner;this.level=level;this.epoch=epoch;this.display=display;this.direction=direction;this.variant=variant;
        }
    }
    private static final class StandArrow extends TippedArrowItem implements PolymerItem {
        StandArrow(Properties properties) { super(properties); }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context) { return Items.TIPPED_ARROW; }
    }
    /** Native item displays have no physics or saving; their source five-tick lifetime is owned below. */
    private static final class FistDisplay extends Display.ItemDisplay {
        final ServerPlayer owner;
        FistDisplay(ServerPlayer owner) { this(owner,owner.level()); }
        FistDisplay(ServerPlayer owner,ServerLevel level){super(EntityTypes.ITEM_DISPLAY,level);this.owner=owner;}
        @Override public boolean shouldBeSaved() { return false; }
    }

    /** Только управляемые кулаки свободного владельца двигаются внутри остановленного мира (user). */
    boolean movesDuringTimeStop(Entity entity) {
        return entity instanceof FistDisplay display && display.owner.isAlive() && !display.owner.isRemoved()
                && display.owner.level()==display.level() && !game.items().timeStop().frozen(display.owner);
    }

    LbcStand(GameController game) { this.game=game; }
    static void register() { }
    private static Item.Properties properties(ResourceKey<Item> key,String name,String model) {
        return new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse(model); }
        }.setId(key).overrideDescription(name);
    }
    private static Item registerArrow() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:stand_arrow"));
        return Registry.register(BuiltInRegistries.ITEM,key,new StandArrow(properties(key,"stand_arrow","lbc:stand_arrow")));
    }
    private static Item registerOraora() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:stand_oraora"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                properties(key,"oraora","lbc:oraora").durability(25),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    private static Item registerClocks() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:stand_clocks"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                properties(key,"stand_clocks","lbc:stand_clock").durability(25),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    private static Item registerHomingLightning() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:homing_lightning"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                properties(key,"homing_lightning","lbc:homing_lightning").durability(25),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    private static CompoundTag fistPreset(String model) {
        try {
            return TagParser.parseCompoundFully("{interpolation_duration:1,teleport_duration:1,shadow_radius:0f,shadow_strength:0f,item_display:\"fixed\",transformation:{scale:[2f,2f,2f]},item:{id:\"minecraft:repeating_command_block\",count:1,components:{\"minecraft:item_model\":\""+model+"\"}}}");
        } catch(Exception e) { throw new IllegalStateException("Invalid Stand fist preset",e); }
    }
    private static boolean flagged(ItemStack stack,String flag) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && data!=null && data.copyTag().getIntOr(flag,0)==1;
    }
    public static boolean isArrow(ItemStack stack) { return stack.is(ARROW) || flagged(stack,"stand_arrow"); }
    public static boolean isOraora(ItemStack stack) { return stack.is(ORAORA) || flagged(stack,"oraora"); }
    public static boolean isClocks(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(CLOCKS) || data!=null && data.matchedBy(CLOCKS_FLAG);
    }
    public static boolean isHomingLightning(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(HOMING_LIGHTNING) || data!=null && data.matchedBy(FIELD_FLAG);
    }
    /** Consume this transport marker in the existing special-effect hook; never apply/tick vanilla Raid Omen. */
    public static boolean isArrowMarker(MobEffectInstance effect) {
        return effect.is(MobEffects.RAID_OMEN) && effect.getAmplifier()==119;
    }
    public static boolean hasClocks(ServerPlayer owner) {
        if(isClocks(owner.getOffhandItem())) return true;
        for(int slot=0;slot<36;slot++) if(isClocks(owner.getInventory().getItem(slot))) return true;
        return false;
    }
    int overheaven(ServerPlayer player) {
        var state=retained.get(player.getUUID());
        // Списанный при старте последний матч всё ещё усиливает игрока до своего конца (user).
        if(state!=null && game.items().currentEpoch(state.epoch) && state.empowered) return 1;
        return legacyHeaven(player);
    }
    private int legacyHeaven(ServerPlayer player) {
        var board=game.server().getScoreboard();
        var objective=board.getObjective("overheaven");
        var score=objective==null?null:board.getPlayerScoreInfo(player,objective);
        return score==null?0:score.value();
    }
    int retainedMatches(ServerPlayer player) {
        var profile=game.players().get(player.getUUID());
        if(profile.standOverheavenMatches<0) {
            // Отсутствующее поле старого профиля импортируется из scoreboard ровно один раз.
            profile.standOverheavenMatches=Math.max(0,legacyHeaven(player));
            game.players().save();
        }
        return profile.standOverheavenMatches;
    }
    private void mirrorRemaining(ServerPlayer player) {
        game.items().mana().score(player,"overheaven").set(retainedMatches(player));
    }
    private RetainedStand state(ServerPlayer player) {
        var state=retained.get(player.getUUID());
        if(state==null || !game.items().currentEpoch(state.epoch)) {
            state=new RetainedStand(player,game.items().epoch()); retained.put(player.getUUID(),state);
        }
        state.owner=player;
        return state;
    }
    public void joined(ServerPlayer player) { mirrorRemaining(player); }
    public void matchStarted(ServerPlayer player) {
        var state=state(player);
        if(state.started) return;
        state.started=true;
        int left=retainedMatches(player);
        if(left<=0) return;
        state.empowered=true; state.grantDelay=120;
        player.entityTags().add("have_stand");
        game.players().get(player.getUUID()).standOverheavenMatches=left-1;
        mirrorRemaining(player); game.players().save();
    }
    public void respawned(ServerPlayer player) {
        var state=state(player);
        if(state.empowered) player.entityTags().add("have_stand");
        if(player.entityTags().contains("have_stand") && !hasOraora(player)) giveOraora(player);
    }
    public void died(ServerPlayer player) {
        cancelBurst(player);
        var state=retained.get(player.getUUID());
        if(state!=null) { state.lightningCooldown=0; clearResistance(state); }
        // Усиление текущего матча сохраняется между жизнями; новую попытку при respawn не списываем.
    }
    private static boolean hasOraora(ServerPlayer player) {
        if(isOraora(player.getOffhandItem())) return true;
        for(int slot=0;slot<36;slot++) if(isOraora(player.getInventory().getItem(slot))) return true;
        return false;
    }
    private static boolean hasLifestealer(ServerPlayer player) {
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            var data=player.getInventory().getItem(slot).get(DataComponents.CUSTOM_DATA);
            if(data!=null && data.matchedBy(LIFESTEALER_FLAG)) return true;
        }
        return false;
    }
    boolean upgrade(ServerPlayer player) {
        if(!active(player) || !player.entityTags().contains("have_stand") || !hasClocks(player)
                || player.getY()<200 || overheaven(player)>0 || game.items().actionsBlocked(player)
                || game.items().magic().suppressedAt(player.level(),player.getEyePosition())) return false;
        var state=state(player); state.empowered=true;
        game.players().get(player.getUUID()).standOverheavenMatches=5;
        mirrorRemaining(player); game.players().save();
        player.sendSystemMessage(Component.translatable("upgraded_overheaven").withStyle(ChatFormatting.GOLD));
        for(var item:game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/homing_lightning"))
            player.level().addFreshEntity(new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),item));
        game.items().mana().award(player,"lbc:true_advancements/legends/overheaven");
        return true;
    }
    /** Для будущей полной очистки: предметы/полученный Stand остаются, сохранённый Overheaven снимается. */
    public void clearRetained(ServerPlayer player) {
        var state=retained.get(player.getUUID());
        if(state!=null) { clearResistance(state); state.empowered=false; state.lightningCooldown=0; state.grantDelay=0; }
        game.players().get(player.getUUID()).standOverheavenMatches=0;
        mirrorRemaining(player); game.players().save();
    }
    int lightningCooldownTicks(ServerPlayer player) {
        var state=retained.get(player.getUUID()); return state==null?0:state.lightningCooldown;
    }
    private boolean useHomingLightning(ServerPlayer player,InteractionHand hand) {
        if(!isHomingLightning(player.getItemInHand(hand)) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !active(player) || game.items().actionsBlocked(player) || game.items().wands().inputTimeStopped(player)
                || lightningCooldownTicks(player)>0 || game.items().magic().suppressedAt(player.level(),player.getEyePosition())) return false;
        state(player).lightningCooldown=600;
        // Старые два счётчика давали примерно 7,5 с; сохраняем выбранный пользователем реальный срок.
        game.items().bolts().homingField(player,150);
        return true;
    }
    private void tickRetained() {
        for(var player:game.server().getPlayerList().getPlayers()) {
            if(!active(player) || game.items().wands().timeStopped() || player.entityTags().contains("time_stopped")) continue;
            var state=retained.get(player.getUUID());
            if(state!=null) {
                state.owner=player;
                if(state.lightningCooldown>0) state.lightningCooldown--;
                if(state.grantDelay>0 && --state.grantDelay==0 && player.entityTags().contains("have_stand") && !hasOraora(player)) giveOraora(player);
            }
            if(overheaven(player)>0) {
                if(state==null) state=state(player);
                if(ticks%100==0 && player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,-1,0,true,false))) state.resistanceOwned=true;
                if(player.onGround()) state.falling=0;
                else if(state.falling++>=16 && !player.isInWater()
                        && !game.items().magic().suppressedAt(player.level(),player.getEyePosition()))
                    player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,true,false));
            }
            if(ticks%10!=0 || !player.entityTags().contains("have_stand") || !hasClocks(player)
                    || game.items().magic().suppressedAt(player.level(),player.getEyePosition())) continue;
            if(!hasLifestealer(player)) player.hurtServer(player.level(),player.damageSources().inFire(),1);
            if(player.isAlive()) upgrade(player);
        }
    }
    private void clearResistance(RetainedStand state) {
        var effect=state.owner.getEffect(MobEffects.RESISTANCE);
        if(state.resistanceOwned && effect!=null && effect.getAmplifier()==0 && effect.isInfiniteDuration())
            state.owner.removeEffect(MobEffects.RESISTANCE);
        state.resistanceOwned=false;
    }
    boolean isStandProjectile(Projectile projectile) {
        return projectile instanceof AbstractArrow arrow && isArrow(arrow.getPickupItemStackOrigin());
    }
    /** Return through the existing projectile target hook, including intentional self acquisition. */
    boolean canHit(Projectile projectile,Entity target) {
        var owner=LbcIndirectKills.playerSource(projectile);
        return owner!=null && (target==owner && active(owner) || game.items().wands().projectileTarget(owner,target));
    }
    /** Call only for accepted native damage; no polling of HurtTime or marker-effect counters. */
    public void onAcceptedDamage(LivingEntity target,DamageSource source) {
        var owner=LbcIndirectKills.playerSource(source.getEntity());
        if(owner==null) owner=LbcIndirectKills.playerSource(source.getDirectEntity());
        if(owner==null) return;
        boolean projectile=source.getDirectEntity() instanceof Projectile shot && isStandProjectile(shot);
        boolean melee=source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity()==owner && isArrow(owner.getMainHandItem());
        if(projectile || melee) {
            // Принятый смертельный удар уже не может дать Stand погибшему, но
            // возврат стрелы всё равно происходит ровно один раз в этом событии.
            if(!target.isAlive()) {
                if(!target.isRemoved() && target.level()==owner.level() && !game.isObserver(owner)
                        && !game.items().magic().suppressedAt(owner.level(),target.getEyePosition())) returnArrow(target,owner);
            } else resolveArrowHit(target,owner,owner.getRandom().nextInt(100)+1,true,projectile);
        }
    }
    /** The roll/drop decision is explicit so the approved policy can be tested independently. */
    boolean resolveArrowHit(LivingEntity target,ServerPlayer owner,int roll,boolean returnsOneArrow) {
        return resolveArrowHit(target,owner,roll,returnsOneArrow,false);
    }
    private boolean resolveArrowHit(LivingEntity target,ServerPlayer owner,int roll,boolean returnsOneArrow,boolean projectile) {
        if(roll<1 || roll>100) throw new IllegalArgumentException("Stand roll outside 1..100");
        if(!active(owner) || target.level()!=owner.level() || !target.isAlive() || target.isRemoved()
                || target!=owner && !(projectile ? game.items().wands().projectileTarget(owner,target) : game.items().wands().target(owner,target))
                || game.items().magic().suppressedAt(owner.level(),target.getEyePosition())) return false;
        if(roll<=10) {
            if(!target.entityTags().contains("sin_greed_active")) target.hurtServer(owner.level(),
                    new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(DamageTypes.GENERIC_KILL),owner),Float.MAX_VALUE);
        } else if(!target.entityTags().contains("have_stand")
                && (!(target instanceof ServerPlayer player) || overheaven(player)<=0)) {
            target.entityTags().add("have_stand");
            if(target instanceof ServerPlayer player) giveOraora(player);
        }
        if(returnsOneArrow) returnArrow(target,owner);
        return true;
    }
    /** Raw arrow-hit status enters the same native roll, unlock and return path as an actual arrow. */
    public boolean arrowHitPre(ServerPlayer player){return resolveArrowHit(player,player,player.level().getRandom().nextInt(100)+1,true);}
    private void returnArrow(LivingEntity target,ServerPlayer owner) {
        for(var stack:game.lbc().rollTable(owner.level(),target.position(),owner,"lbc:ported/stand_arrow"))
            owner.level().addFreshEntity(new ItemEntity(owner.level(),target.getX(),target.getY(),target.getZ(),stack));
    }
    void giveOraora(ServerPlayer player) {
        for(var stack:game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/stand_oraora"))
            if(!player.getInventory().add(stack)) player.level().addFreshEntity(
                    new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),stack));
    }
    private boolean active(ServerPlayer owner) {
        return owner.isAlive() && !owner.isRemoved() && game.canUseGameplayItems(owner) && !game.isObserver(owner)
                && owner.level()==game.maps().level() && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().inputTimeStopped(owner);
    }
    boolean use(ServerPlayer owner,InteractionHand hand) {
        if(isClocks(owner.getItemInHand(hand))) return game.items().timeStop().useClocks(owner,hand);
        if(isHomingLightning(owner.getItemInHand(hand))) return useHomingLightning(owner,hand);
        if(!isOraora(owner.getItemInHand(hand)) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !active(owner) || !owner.entityTags().contains("have_stand") || cooldownTicks(owner)>0
                || game.items().wands().inputTimeStopped(owner) || game.items().actionsBlocked(owner)
                || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        bursts.put(owner.getUUID(),new Burst(owner,game.items().epoch(),BURST_FISTS));
        owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,1200,4));
        owner.addEffect(ItemEffectLevels.create(MobEffects.JUMP_BOOST,1200,4));
        owner.addEffect(ItemEffectLevels.create(MobEffects.HEALTH_BOOST,1200,9));
        LbcOverload.sound(owner,owner.position(),"minecraft:lbcsounds.oraora",10,1,0);
        return true;
    }
    int cooldownTicks(ServerPlayer owner) {
        var burst=bursts.get(owner.getUUID()); return burst==null?0:burst.cooldown;
    }
    /** KFC меняет реальные счётчики игрока, а не оставшийся legacy scoreboard. */
    void addPlayerCounters(ServerPlayer owner) {
        var state=state(owner); state.lightningCooldown++; state.falling++;
        if(state.grantDelay>0) state.grantDelay++;
        var burst=bursts.get(owner.getUUID());
        if(burst==null) {
            burst=new Burst(owner,game.items().epoch(),0);
            burst.cooldown=1;
            bursts.put(owner.getUUID(),burst);
        } else {
            burst.cooldown++;
            if(burst.remaining>0) { burst.remaining++; burst.delay++; }
        }
    }
    int fistCount() { return fists.size(); }
    List<Display.ItemDisplay> fistSnapshot() { return fists.stream().map(fist->fist.display).toList(); }
    Vec3 fistDirection(Display.ItemDisplay display) {
        return fists.stream().filter(fist->fist.display==display).findFirst().orElseThrow().direction;
    }
    int burstRemaining(ServerPlayer owner) {
        var burst=bursts.get(owner.getUUID()); return burst==null?0:burst.remaining;
    }
    void tick() {
        ticks++; tickRetained();
        tickTargets.clear();
        for(var burst:List.copyOf(bursts.values())) {
            if(burst.retainedOffline)continue;
            if(!active(burst.owner) || !game.items().currentEpoch(burst.epoch)) { cancelBurst(burst.owner); continue; }
            if(paused(burst.owner)) continue;
            if(--burst.cooldown<=0) { bursts.remove(burst.owner.getUUID()); continue; }
            if(burst.remaining<=0 || --burst.delay>0) continue;
            burst.delay=2; burst.remaining--;
            if(game.items().actionsBlocked(burst.owner)
                    || game.items().magic().suppressedAt(burst.owner.level(),burst.owner.getEyePosition())) continue;
            burst.owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,true,false));
            createFist(burst.owner);
        }
        for(var fist:List.copyOf(fists)) tickFist(fist);
        tickTargets.clear();
    }
    private void createFist(ServerPlayer owner) {
        var offset=OFFSETS[owner.getRandom().nextInt(OFFSETS.length)];
        Vec3 forward=owner.getLookAngle().normalize();
        Vec3 left=Vec3.directionFromRotation(0,owner.getYRot()-90);
        Vec3 up=forward.cross(left).normalize();
        Vec3 position=owner.getEyePosition().add(left.scale(offset[0])).add(up.scale(offset[1])).add(forward.scale(offset[2]));
        if(!owner.level().hasChunkAt(BlockPos.containing(position))) return;
        int variant=overheaven(owner)>0?2:hasClocks(owner)?1:0;
        var display=new FistDisplay(owner);
        display.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),(variant==2?HEAVEN_FIST:NORMAL_FIST).copy()));
        display.setPos(position); display.setYRot(owner.getYRot()); display.setXRot(owner.getXRot());
        display.setOldPosAndRot();
        display.entityTags().add(FIST_TAG); display.entityTags().add("magic");
        game.items().markCurrentItemEpoch(display);
        var fist=new Fist(owner,game.items().epoch(),display,forward,variant);
        fists.add(fist);
        if(!owner.level().addFreshEntity(display)) remove(fist);
    }
    private LivingEntity nearest(Fist fist) {
        LivingEntity nearest=null; double distance=Double.POSITIVE_INFINITY;
        var candidates=tickTargets.computeIfAbsent(fist.owner,owner->game.items().lifetimes().loadedTargets().stream()
                .filter(candidate->candidate.level()==fist.level && game.items().lifetimes().target(owner,candidate)).toList());
        for(var candidate:candidates) {
            if(candidate.level()!=fist.level || !game.items().lifetimes().target(fist.owner,candidate)) continue;
            double next=candidate.position().distanceToSqr(fist.display.position());
            if(next<distance) { nearest=candidate; distance=next; }
        }
        return nearest;
    }
    private void tickFist(Fist fist) {
        if(fist.display.isRemoved() || !active(fist.owner) || fist.owner.level()!=fist.level
                || !game.items().currentEpoch(fist.epoch)) { remove(fist); return; }
        if(paused(fist.owner) || fist.display.entityTags().contains("time_stopped")) return;
        if(game.items().magic().suppressedAt(fist.level,fist.display.position())) { remove(fist); return; }
        for(int step=0;step<3;step++) {
            var target=nearest(fist);
            if(target!=null) fist.direction=fist.direction.add(target.getEyePosition()
                    .subtract(fist.display.position()).normalize().scale(.3)).normalize();
            Vec3 from=fist.display.position(), next=from.add(fist.direction);
            var wall=LbcTerra.wallHit(fist.level,from,next);
            var end=wall==null?next:wall.getLocation();
            // Source dx=dy=dz=0 selects a one-block cube anchored at the display. Sweep that
            // volume through each curved step using the existing native entity-hit helper.
            var centerFrom=from.add(.5,.5,.5); var centerEnd=end.add(.5,.5,.5);
            var hits=ProjectileUtil.getManyEntityHitResult(fist.level,fist.owner,centerFrom,centerEnd,
                    new AABB(centerFrom,centerEnd).inflate(.5),
                    candidate->candidate instanceof LivingEntity && game.items().lifetimes().target(fist.owner,candidate),
                    .5f,ClipContext.Block.COLLIDER,true,false);
            var hit=hits.stream().min(java.util.Comparator.comparingDouble(result->result.getLocation().distanceToSqr(centerFrom))).orElse(null);
            if(hit!=null) {
                fist.display.setPos(hit.getLocation().subtract(.5,.5,.5));
                hitFist(fist,(LivingEntity)hit.getEntity());
                return;
            }
            if(wall!=null) { remove(fist); return; }
            fist.display.setPos(next);
        }
        fist.display.setYRot((float)(Math.atan2(fist.direction.z,fist.direction.x)*180/Math.PI)-90);
        fist.display.setXRot((float)(-Math.atan2(fist.direction.y,Math.sqrt(fist.direction.x*fist.direction.x+fist.direction.z*fist.direction.z))*180/Math.PI));
        if(++fist.age>=5) remove(fist);
    }
    private void hitFist(Fist fist,LivingEntity target) {
            var source=new DamageSource(fist.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.ARROW),fist.display,fist.owner);
            target.hurtServer(fist.level,source,2.5f);
            if(fist.variant>0 && target.isAlive()) target.hurtServer(fist.level,source,fist.variant==2?12.5f:5f);
            fist.level.sendParticles(ParticleTypes.EXPLOSION,fist.display.getX(),fist.display.getY()+1,fist.display.getZ(),3,.1,.1,.1,1);
            LbcOverload.sound(fist.owner,fist.display.position(),"minecraft:entity.player.attack.strong",1,1,0);
            remove(fist);
    }
    private void remove(Fist fist) { fists.remove(fist); fist.display.discard(); }
    void loaded(Entity entity) {
        if(entity instanceof ServerPlayer player && !game.playing()) player.entityTags().remove("have_stand");
        if(entity.entityTags().contains(FIST_TAG) && fists.stream().noneMatch(fist->fist.display==entity)) entity.discard();
    }
    void unloaded(Entity entity) {
        for(var fist:List.copyOf(fists)) if(fist.display==entity) remove(fist);
    }
    void transformOrigins(ServerLevel level,UnaryOperator<Vec3> transform) {
        for(var fist:fists) if(fist.level==level) fist.display.setPos(transform.apply(fist.display.position()));
    }
    void forget(ServerPlayer owner) {
        var state=retained.remove(owner.getUUID());
        if(state!=null) { clearResistance(state); if(state.empowered) mirrorRemaining(owner); }
        cancelBurst(owner);
    }
    private void cancelBurst(ServerPlayer owner) {
        bursts.remove(owner.getUUID());
        for(var fist:List.copyOf(fists)) if(fist.owner==owner) remove(fist);
    }
    void stop() {
        for(var state:List.copyOf(retained.values())) {
            clearResistance(state);
            if(state.empowered) mirrorRemaining(state.owner);
        }
        retained.clear();
        bursts.clear();
        tickTargets.clear();
        for(var fist:List.copyOf(fists)) remove(fist);
        for(var player:game.server().getPlayerList().getPlayers()) player.entityTags().remove("have_stand");
    }

    Set<UUID> transientCheckpointActors(){var ids=new java.util.LinkedHashSet<UUID>();for(var fist:fists)ids.add(fist.display.getUUID());return Set.copyOf(ids);}
    ResidentCheckpoint residentCheckpoint(MatchSpellCheckpoint.Links links){return new ResidentCheckpoint(links);}
    void prepareResidentFrozen(){
        retained.clear();bursts.clear();tickTargets.clear();
        var old=List.copyOf(fists);fists.clear();for(var fist:old)if(!fist.display.isRemoved())fist.display.discard();
    }
    final class ResidentCheckpoint {
        private record StandImage(MatchDamageSources.Ref owner,String epoch,boolean started,boolean empowered,boolean resistance,
                int grantDelay,int lightningCooldown,int falling){}
        private record BurstImage(MatchDamageSources.Ref owner,String epoch,int cooldown,int remaining,int delay){}
        private final List<StandImage> owners;
        private final List<BurstImage> series;
        private final int cadence=ticks;
        ResidentCheckpoint(MatchSpellCheckpoint.Links links){
            owners=retained.values().stream().map(s->new StandImage(links.key(s.owner),s.epoch,s.started,s.empowered,s.resistanceOwned,s.grantDelay,s.lightningCooldown,s.falling)).toList();
            series=bursts.values().stream().map(b->new BurstImage(links.key(b.owner),b.epoch,b.cooldown,b.remaining,b.delay)).toList();
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var nextOwners=new HashMap<UUID,RetainedStand>();var nextBursts=new HashMap<UUID,Burst>();
            for(var image:owners){var player=(ServerPlayer)links.get(image.owner);var state=new RetainedStand(player,image.epoch);
                state.started=image.started;state.empowered=image.empowered;state.resistanceOwned=image.resistance;state.grantDelay=image.grantDelay;
                state.lightningCooldown=image.lightningCooldown;state.falling=image.falling;nextOwners.put(player.getUUID(),state);}
            for(var image:series){var player=(ServerPlayer)links.get(image.owner);var burst=new Burst(player,image.epoch,image.remaining);
                burst.cooldown=image.cooldown;burst.delay=image.delay;burst.retainedOffline=MatchOfflinePlayers.retained(player);nextBursts.put(player.getUUID(),burst);}
            return ()->{retained.clear();retained.putAll(nextOwners);bursts.clear();bursts.putAll(nextBursts);ticks=cadence;tickTargets.clear();};
        }
    }

    static boolean cloneRecognizes(Entity source){return source instanceof FistDisplay||source.entityTags().contains(FIST_TAG);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var fist=fists.stream().filter(f->f.display==source).findFirst().orElse(null);if(fist==null)return null;
        var owner=fist.owner;var fistLevel=fist.level;var epoch=fist.epoch;var direction=fist.direction;int age=fist.age,variant=fist.variant;
        return new LbcCloneSkills.ActorImage(){
            public Entity create(Entity original,ServerLevel level){return new FistDisplay(owner,level);}
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){var f=new Fist(owner,fistLevel,epoch,(Display.ItemDisplay)clone,direction,variant);f.age=age;fists.add(f);}
            public void rollback(Entity clone){fists.removeIf(f->f.display==clone);}
        };
    }

}
