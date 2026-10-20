package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Очереди стрелкового оружия; физика, попадания и автор урона остаются у ванильных стрел. */
public final class LbcShooters {
    private static final String PROJECTILE_TAG = "skywars2.razorpine";
    private static final CompoundTag PINE_FLAG = new CompoundTag(), SLR_FLAG = new CompoundTag(), INFINITE_ARROW = new CompoundTag();
    private static final TagKey<Block> BREAKABLE_SLR = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:breakable_slr"));
    private static final TagKey<Item> AMMO = TagKey.create(Registries.ITEM, Identifier.parse("minecraft:razor_ammo"));
    private static final TagKey<Item> BEST_AMMO = TagKey.create(Registries.ITEM, Identifier.parse("minecraft:razor_ammo_best"));
    public static final Item PINE = registerPine();
    public static final Item SLR = registerSlr();
    static { PINE_FLAG.putInt("razorpine", 1); SLR_FLAG.putInt("slr",1); INFINITE_ARROW.putInt("infinite_arrow",1); }

    private final GameController game;
    private final Map<UUID, Burst> bursts = new HashMap<>();
    private final Set<Arrow> arrows = new LinkedHashSet<>();
    private final Map<UUID,Integer> slrCooldowns = new HashMap<>();
    private final Map<LivingEntity,Stun> stuns = new java.util.IdentityHashMap<>();
    private static final class Stun {
        final ServerPlayer owner;
        final String epoch;
        final float yaw,pitch;
        int ticks=80;
        Stun(ServerPlayer owner,LivingEntity target,String epoch) {
            this.owner=owner; this.epoch=epoch; yaw=target.getYRot(); pitch=target.getXRot();
        }
    }
    private static final class Burst {
        final ServerPlayer owner;
        final boolean best;
        int volleys = 6, delay = 2;
        Burst(ServerPlayer owner, boolean best) { this.owner = owner; this.best = best; }
    }

    LbcShooters(GameController game) { this.game = game; }
    static void register() { }
    private static Item registerPine() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:razorpine"));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(
                new Item.Properties().setId(key).durability(25).overrideDescription("razorpine")
                        .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:razorpine")),
                Items.CARROT_ON_A_STICK, true) {
            @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                var controller = me.skywars2.SkyWars2.game();
                return controller != null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
    }
    private static Item registerSlr() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:slr"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties().setId(key).durability(25).overrideDescription("slr")
                        .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:super_rifle")),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isSlr(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(SLR) || data!=null && data.matchedBy(SLR_FLAG);
    }
    static int mode(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4);
    }
    int cooldownRemaining(ServerPlayer player) { return slrCooldowns.getOrDefault(player.getUUID(),0); }
    private boolean useSlr(ServerPlayer player,InteractionHand hand) {
        if(hand==InteractionHand.OFF_HAND && !game.config().offhandWands || !canFire(player)) return false;
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()) {
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int next=mode(stack)%4+1; data.putInt("mode",next);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(next) {
                case 2 -> "karich_mode_2";
                case 3 -> "karich_mode_3";
                case 4 -> "karich_mode_4";
                default -> "karich_mode_1";
            }));
            return true;
        }
        if(cooldownRemaining(player)>0 || player.getInventory().clearOrCountMatchingItems(
                ammo->ammo.is(Items.ARROW),true,0,player.inventoryMenu.getCraftSlots())==0) return false;
        boolean infinite=player.getInventory().clearOrCountMatchingItems(ammo->{
            var data=ammo.get(DataComponents.CUSTOM_DATA); return data!=null && data.matchedBy(INFINITE_ARROW);
        },true,0,player.inventoryMenu.getCraftSlots())>0;
        if(!infinite) player.getInventory().clearOrCountMatchingItems(ammo->ammo.is(Items.ARROW),false,1,player.inventoryMenu.getCraftSlots());
        slrCooldowns.put(player.getUUID(),15);
        // Source local offset ^-.2 ^-.2 ^, before recoil changes the player's look.
        var forward=player.getLookAngle();
        var left=new Vec3(Math.cos(Math.toRadians(player.getYRot())),0,Math.sin(Math.toRadians(player.getYRot())));
        var up=forward.cross(left);
        var origin=player.getEyePosition().subtract(left.scale(.2)).subtract(up.scale(.2));
        game.items().indirectKills().trapChanges(player,()->slrRay(player,origin,forward,mode(stack)));
        LbcOverload.sound(player,origin.add(forward.scale(.5)),"minecraft:lbcsounds.karich",2,2,.5f);
        float yaw=player.getYRot()+(player.getRandom().nextInt(1201)-600)*.01f;
        float pitch=player.getXRot()+(player.getRandom().nextInt(1001)-1500)*.01f;
        player.teleportTo(player.level(),player.getX(),player.getY(),player.getZ(),Set.of(),yaw,pitch,true);
        return true;
    }
    private void slrRay(ServerPlayer owner,Vec3 origin,Vec3 direction,int mode) {
        var level=owner.level();
        var seen=new java.util.HashSet<UUID>();
        // At most 19 narrow spatial queries, with DDA checking every crossed block before reads.
        for(int part=0;part<19;part++) {
            var from=origin.add(direction.scale(part*16));
            var to=origin.add(direction.scale(Math.min(300,(part+1)*16)));
            var wall=BlockGetter.traverseBlocks(from,to,level,(world,pos)-> {
                if(!world.hasChunkAt(pos)) return Shapes.block().clip(from,to,pos);
                var state=world.getBlockState(pos);
                if(mode!=3 && state.is(BREAKABLE_SLR)) {
                    world.destroyBlock(pos,true,owner,Block.UPDATE_LIMIT);
                    return null;
                }
                return state.isAir()?null:Shapes.block().clip(from,to,pos);
            },world->null);
            var end=wall==null?to:wall.getLocation();
            var hits=new java.util.ArrayList<net.minecraft.world.phys.EntityHitResult>();
            for(var entity:level.getEntities(owner,new AABB(from,end).inflate(1),e->
                    game.items().lifetimes().target(owner,e) && !e.entityTags().contains("strict_map_object") && !seen.contains(e.getUUID()))) {
                var box=entity.getBoundingBox();
                var point=box.contains(from)?java.util.Optional.of(from):box.clip(from,end);
                if(point.isPresent()) hits.add(new net.minecraft.world.phys.EntityHitResult(entity,point.get()));
            }
            hits.sort(java.util.Comparator.comparingDouble(hit->from.distanceToSqr(hit.getLocation())));
            for(var hit:hits) {
                var entity=hit.getEntity(); seen.add(entity.getUUID());
                slrHit(owner,(LivingEntity)entity,hit.getLocation(),mode);
                if(mode==3) { slrExplode(owner,hit.getLocation()); end=hit.getLocation(); break; }
            }
            var particle=switch(mode) { case 2->ParticleTypes.SNEEZE; case 3->ParticleTypes.CRIT; case 4->ParticleTypes.ELECTRIC_SPARK; default->ParticleTypes.ASH; };
            double length=from.distanceTo(end);
            for(double distance=0;distance<=length;distance+=.2) {
                var point=from.add(direction.scale(distance));
                level.sendParticles(particle,true,false,point.x,point.y,point.z,1,0,0,0,0);
            }
            if(mode==3 && !hits.isEmpty()) return;
            if(wall!=null) {
                if(mode==3 && level.hasChunkAt(wall.getBlockPos())) slrExplode(owner,end);
                return;
            }
        }
    }
    private void slrHit(ServerPlayer owner,LivingEntity target,Vec3 point,int mode) {
        var damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),owner);
        if(mode==1) {
            if(target.getEyePosition().distanceToSqr(point)<=.25) {
                target.hurtServer(owner.level(),damage,20);
                target.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
                game.items().mana().award(owner,"lbc:true_advancements/shooter/slr");
                LbcOverload.sound(owner,target.position(),"minecraft:entity.item.break",1,1,0);
            }
            target.hurtServer(owner.level(),damage,10);
        } else if(mode==2) {
            target.hurtServer(owner.level(),damage,8);
            if(target.addEffect(new MobEffectInstance(MobEffects.POISON,200,0),owner)) game.items().indirectKills().poisoned(target,owner);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200,0),owner);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,0),owner);
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER,200,1),owner);
        } else if(mode==3) target.hurtServer(owner.level(),damage,5);
        else {
            if(target instanceof ServerPlayer player && game.items().magic().rejectEffect(player,MobEffects.SLOWNESS)) return;
            stuns.put(target,new Stun(owner,target,game.items().epoch()));
            game.items().indirectKills().displaced(target,owner);
            prepareStunnedInput(target);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,80,127,false,false),owner);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,80,127,false,false),owner);
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,80,127,false,false),owner);
        }
    }
    public boolean stunned(LivingEntity target) { return stunTicks(target)>0; }
    public int stunTicks(LivingEntity target) {
        var stun=stuns.get(target);
        return stun==null?0:stun.ticks;
    }
    public void clearStun(LivingEntity target) {
        if(stuns.remove(target)==null) return;
        target.removeEffect(MobEffects.SLOWNESS);
        target.removeEffect(MobEffects.WEAKNESS);
        target.removeEffect(MobEffects.MINING_FATIGUE);
    }
    /** Remove voluntary input only: deltaMovement belongs to native physics and external forces. */
    public void prepareStunnedInput(LivingEntity target) {
        var stun=stuns.get(target); if(stun==null) return;
        target.xxa=0; target.yya=0; target.zza=0;
        target.setJumping(false); target.setSprinting(false); target.setShiftKeyDown(false);
        target.setSwimming(false);
        if(target.isUsingItem()) target.stopUsingItem();
        target.setYRot(stun.yaw); target.setXRot(stun.pitch); target.setYHeadRot(stun.yaw);
        if(target instanceof ServerPlayer player) player.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);
    }
    /** Native correction retains current server velocity, unlike teleport(x,y,z,yaw,pitch). */
    public void correctStun(ServerPlayer player) {
        if(!stunned(player)) return;
        prepareStunnedInput(player);
        player.connection.teleport(net.minecraft.world.entity.PositionMoveRotation.of(player),Set.of());
    }
    private void tickStuns() {
        for(var target:List.copyOf(stuns.keySet())) {
            var stun=stuns.get(target);
            if(!target.isAlive() || target.isRemoved() || target.level()!=game.maps().level()
                    || !active(stun.owner) || !game.items().currentEpoch(stun.epoch)
                    || target instanceof ServerPlayer player && (!active(player)
                        || game.items().magic().rejectEffect(player,MobEffects.SLOWNESS)) || --stun.ticks<=0) {
                clearStun(target); continue;
            }
            prepareStunnedInput(target);
            if(target instanceof ServerPlayer player) correctStun(player);
        }
    }
    private void slrExplode(ServerPlayer owner,Vec3 point) {
        for(int i=0;i<2;i++) {
            var tnt=new PrimedTnt(owner.level(),point.x,point.y,point.z,owner);
            tnt.setFuse(2); tnt.setDeltaMovement(Vec3.ZERO);
            tnt.entityTags().add(LbcExasperato.EXPLOSIVE);
            game.items().markCurrentItemEpoch(tnt);
            owner.level().addFreshEntity(tnt);
        }
    }
    public static boolean isPine(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(PINE) || data != null && data.matchedBy(PINE_FLAG);
    }
    private boolean active(ServerPlayer player) {
        return game.canUseGameplayItems(player) && player.isAlive() && !player.isRemoved()
                && player.level() == game.maps().level() && game.stage() != GameController.Stage.ENDING;
    }
    private boolean canFire(ServerPlayer player) {
        return active(player) && !stunned(player) && !game.items().frost().isFrozen(player)
                && !player.entityTags().contains("time_stopped") && !game.items().wands().timeStopped()
                && !game.items().magic().suppressed(player);
    }
    private int hands(ServerPlayer player) {
        return (isPine(player.getMainHandItem()) ? 1 : 0)
                + (game.config().offhandWands && isPine(player.getOffhandItem()) ? 1 : 0);
    }
    public boolean use(ServerPlayer player, InteractionHand hand) {
        if(isSlr(player.getItemInHand(hand))) return useSlr(player,hand);
        if (!isPine(player.getItemInHand(hand)) || hand == InteractionHand.OFF_HAND && !game.config().offhandWands
                || bursts.containsKey(player.getUUID()) || !canFire(player)) return false;
        // Как source /clear: сначала еловые боеприпасы, затем обычные; ровно одна единица на всю очередь.
        boolean best = consume(player, BEST_AMMO);
        if (!best && !consume(player, AMMO)) return false;
        bursts.put(player.getUUID(), new Burst(player, best));
        if (best) {
            var advancement = game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/shooter/razorpine"));
            if (advancement != null) for (var criterion : advancement.value().criteria().keySet())
                player.getAdvancements().award(advancement, criterion);
        }
        return true;
    }
    private static boolean consume(ServerPlayer player, TagKey<Item> tag) {
        return player.getInventory().clearOrCountMatchingItems(stack -> stack.is(tag), false, 1,
                player.inventoryMenu.getCraftSlots()) == 1;
    }
    public void tick() {
        tickStuns();
        slrCooldowns.replaceAll((id,ticks)->ticks-1);
        slrCooldowns.values().removeIf(ticks->ticks<=0);
        if (bursts.isEmpty()) return;
        boolean stopped = game.items().wands().timeStopped();
        for (var burst : List.copyOf(bursts.values())) {
            var owner = burst.owner;
            if (!active(owner)) { forget(owner); continue; }
            if (stopped || owner.entityTags().contains("time_stopped")) continue;
            if (--burst.delay > 0) continue;
            burst.delay = 2;
            if (canFire(owner)) {
                // (user) Два хвоемёта удваивают очередь без второго расхода боеприпаса.
                for (int hand = 0, held = hands(owner); hand < held; hand++) volley(owner, burst.best);
            }
            if (--burst.volleys == 0) bursts.remove(owner.getUUID());
        }
    }
    private void volley(ServerPlayer owner, boolean best) {
        var direction = owner.getLookAngle();
        var origin = owner.getEyePosition().subtract(direction.scale(.2));
        int speed = best ? 5000 : 2000;
        for (int shot = 0; shot < (best ? 2 : 3); shot++) {
            var arrow = new Arrow(EntityTypes.ARROW, owner.level());
            arrow.setOwner(owner);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setBaseDamage(best ? 1.5 : .5);
            arrow.setSoundEvent(SoundEvents.AZALEA_LEAVES_BREAK);
            arrow.setPos(origin);
            arrow.setDeltaMovement(new Vec3(component(owner, direction.x, speed),
                    component(owner, direction.y, speed), component(owner, direction.z, speed)));
            // Arrow использует углы скорости, а не углы взгляда игрока; задаём их до spawn-пакета.
            orient(arrow);
            arrow.entityTags().add(PROJECTILE_TAG);
            owner.level().addFreshEntity(arrow);
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.MASTER, 1, 2);
    }
    static void orient(Projectile projectile) {
        var motion=projectile.getDeltaMovement();
        projectile.setYRot((float)(net.minecraft.util.Mth.atan2(motion.x,motion.z)*180/Math.PI));
        projectile.setXRot((float)(net.minecraft.util.Mth.atan2(motion.y,motion.horizontalDistance())*180/Math.PI));
        projectile.setOldRot();
    }
    private static double component(ServerPlayer owner, double direction, int speed) {
        return (Math.floor(direction * speed) + owner.getRandom().nextInt(601) - 300) * .001;
    }
    boolean loaded(Entity entity) {
        if (!(entity instanceof Arrow arrow) || !entity.entityTags().contains(PROJECTILE_TAG)) return false;
        if (entity.level() == game.maps().level() && (game.playing() || game.items().currentEpoch(entity)
                || arrow.getOwner() instanceof ServerPlayer owner && active(owner))) arrows.add(arrow);
        else arrow.discard();
        return true;
    }
    void unloaded(Entity entity) {
        arrows.remove(entity);
        if(entity instanceof LivingEntity living) clearStun(living);
    }
    public boolean canHit(Projectile projectile, Entity target) {
        return !projectile.entityTags().contains(PROJECTILE_TAG)
                || projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && game.items().wands().target(owner, target);
    }
    /** Source prein удаляет хвою в земле; перехватываем событие вместо чтения NBT каждого снаряда в тике. */
    public void hit(Projectile projectile, HitResult hit) {
        if (hit.getType() == HitResult.Type.BLOCK && projectile.entityTags().contains(PROJECTILE_TAG)) projectile.discard();
    }
    public int burstCount() { return bursts.size(); }
    public int arrowCount() { return arrows.size(); }
    public void forget(ServerPlayer player) {
        bursts.remove(player.getUUID()); slrCooldowns.remove(player.getUUID());
        for(var target:List.copyOf(stuns.keySet())) if(target==player || stuns.get(target).owner==player) clearStun(target);
    }
    void stop() {
        bursts.clear(); slrCooldowns.clear();
        for(var target:List.copyOf(stuns.keySet())) clearStun(target);
        var removed = List.copyOf(arrows); arrows.clear();
        for (var arrow : removed) arrow.discard();
    }
}
