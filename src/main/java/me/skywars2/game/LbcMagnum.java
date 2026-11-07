package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Six source Magnum rounds share the SLR timer and the existing secondary projectile engines. */
public final class LbcMagnum {
    private static final String[] MODES={"standard","incendiary","electric","acid","void","explosive"};
    private static final int[] COOLDOWNS={25,25,15,20,15,40};
    private static final CompoundTag FLAG=flag("magnum");
    public static final Item ITEM=registerItem();
    private static final java.util.List<Item> ROUNDS=java.util.Arrays.stream(MODES).map(LbcMagnum::registerRound).toList();
    private static Item registerRound(String mode) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:magnum_round_"+mode));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:round_"+mode); }
        }.setId(key).overrideDescription("round_"+mode)
                .component(DataComponents.CUSTOM_DATA,CustomData.of(flag(mode+"_round"))),Items.REPEATING_COMMAND_BLOCK,true));
    }

    private final GameController game;
    private final Map<UUID,Burst> bursts=new HashMap<>();
    private final Map<UUID,Integer> fired=new HashMap<>();
    private static final class Burst {
        final ServerPlayer owner;
        final InteractionHand hand;
        final ItemStack gun;
        final int mode;
        final String epoch;
        int remaining;
        Burst(ServerPlayer owner,InteractionHand hand,ItemStack gun,int mode,String epoch,int remaining) {
            this.owner=owner; this.hand=hand; this.gun=gun; this.mode=mode; this.epoch=epoch; this.remaining=remaining;
        }
    }
    LbcMagnum(GameController game) { this.game=game; }
    public static void register() { }
    private static CompoundTag flag(String name) { var tag=new CompoundTag(); tag.putInt(name,1); return tag; }
    private static Component line(String key,ChatFormatting color) {
        return Component.translatable(key).withStyle(color).withStyle(style->style.withItalic(false));
    }
    private static Item registerItem() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:magnum"));
        var data=flag("magnum");
        for(String name:List.of("transformable","shooter","raycastspell")) data.putInt(name,1);
        data.putInt("mode",0);
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:magnum"); }
        }.setId(key).stacksTo(1).overrideDescription("magnum_mode_standard")
                .component(DataComponents.ATTACK_ANIMATION,LbcRifleZoom.SCOPE_ATTACK_ANIMATION)
                .component(DataComponents.INTERACT_ANIMATION,new net.minecraft.world.item.component.SwingAnimation(
                        net.minecraft.world.item.SwingAnimationType.NONE,6))
                .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:magnum"))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_ss"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("magnum_mode_standard").withStyle(style->style.withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(
                        line("type",ChatFormatting.AQUA).copy().append(line("shooter",ChatFormatting.AQUA)),
                        line("magnum_lore_1",ChatFormatting.WHITE),line("magnum_lore_2",ChatFormatting.WHITE),
                        line("magnum_lore_3",ChatFormatting.AQUA)))),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return hand==InteractionHand.MAIN_HAND && controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isMagnum(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && (stack.is(ITEM) || data!=null && data.matchedBy(FLAG));
    }
    public static int mode(ItemStack stack) {
        return Math.clamp(ItemData.intValue(stack,"mode",0),0,5);
    }
    /** Catalog source selector; ammo intake itself follows the native inventory debit below. */
    static int roundMode(ItemStack stack) {
        for(int mode=0;mode<MODES.length;mode++)if(ItemData.intValue(stack,MODES[mode]+"_round",0)==1)return mode;
        return -1;
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && owner.isAlive() && !owner.isRemoved() && !game.isObserver(owner)
                && owner.level()==game.maps().level() && game.canUseGameplayItems(owner)
                && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean blocked(ServerPlayer owner) {
        return game.items().actionsBlocked(owner) || game.items().wands().inputTimeStopped(owner)
                || game.items().magic().mechanismSuppressedAt(owner.level(),owner.position());
    }
    public boolean use(ServerPlayer owner,InteractionHand hand) {
        if(hand!=InteractionHand.MAIN_HAND || !valid(owner)
                || !isMagnum(owner.getItemInHand(hand)) || blocked(owner)) return false;
        var gun=owner.getItemInHand(hand);
        int mode=mode(gun);
        if(owner.isShiftKeyDown()) {
            int next=(mode+1)%6;
            var tag=gun.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); tag.putInt("mode",next);
            gun.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            gun.set(DataComponents.CUSTOM_NAME,Component.translatable("magnum_mode_"+MODES[next]).withStyle(style->style.withItalic(false)));
            LbcOverload.sound(owner,"minecraft:ui.button.click",.8f,1.5f,0);
            return true;
        }
        if(game.items().shooters().cooldownRemaining(owner)>0 || bursts.containsKey(owner.getUUID())) return false;
        var marker=flag(MODES[mode]+"_round");
        if(owner.getInventory().clearOrCountMatchingItems(stack->LbcTmtInventory.matches(stack,marker),
                false,1,owner.inventoryMenu.getCraftSlots())!=1) return false;
        var burst=new Burst(owner,hand,gun,mode,game.items().epoch(),4);
        bursts.put(owner.getUUID(),burst);
        shoot(burst);
        return true;
    }
    private boolean retained(Burst burst) {
        return valid(burst.owner) && game.items().currentEpoch(burst.epoch)
                && burst.hand==InteractionHand.MAIN_HAND
                && burst.owner.getItemInHand(burst.hand)==burst.gun && isMagnum(burst.gun);
    }
    public void tick() {
        for(var burst:List.copyOf(bursts.values())) {
            if(!retained(burst)) { bursts.remove(burst.owner.getUUID(),burst); continue; }
            if(game.server().tickRateManager().isFrozen() || game.items().wands().inputTimeStopped(burst.owner)) continue;
            if(--burst.remaining<=0) bursts.remove(burst.owner.getUUID(),burst);
            // A jammed or stunned shooter misses this scheduled shot instead of storing it for later.
            if(!blocked(burst.owner)) shoot(burst);
        }
    }
    private void shoot(Burst burst) {
        var owner=burst.owner;
        game.items().shooters().setCooldown(owner,COOLDOWNS[burst.mode]);
        fired.merge(owner.getUUID(),1,Integer::sum);
        ray(burst);
        // Native damage can synchronously stop the match, clear this service and replace the epoch.
        if(!valid(owner) || !game.items().currentEpoch(burst.epoch)) return;
        game.items().mana().award(owner,"lbc:true_advancements/shooter/magnum");
        LbcOverload.sound(owner,"minecraft:entity.firework_rocket.blast",.7f,1.2f,0);
        LbcOverload.sound(owner,"minecraft:entity.firework_rocket.large_blast",.7f,1.5f,0);
        LbcOverload.sound(owner,"minecraft:item.trident.return",1,2,0);
        if(burst.mode==1) LbcOverload.sound(owner,"minecraft:entity.blaze.shoot",1,1.3f,0);
        if(burst.mode==2) LbcOverload.sound(owner,"minecraft:entity.firework_rocket.twinkle",1,2,0);
        if(burst.mode==3) LbcOverload.sound(owner,"minecraft:entity.zombie_villager.cure",.5f,2,0);
        if(burst.mode==4) LbcOverload.sound(owner,"minecraft:ui.toast.out",1,0,0);
        int recoil=burst.mode==4?1:burst.mode==0 || burst.mode==5?3:2;
        owner.forceSetRotation(0,true,-recoil,true);
    }
    private void ray(Burst burst) {
        var owner=burst.owner;
        Vec3 origin=owner.getEyePosition(),direction=LbcGunAccuracy.rifle(game,owner),end=origin.add(direction.scale(200));
        double suppression=game.items().magic().mechanismSuppressionFraction(owner.level(),origin,end);
        if(suppression<=1) end=origin.lerp(end,Math.max(0,suppression));
        int mode=burst.mode;
        if(mode==2 || mode==3 || mode==5) {
            var hit=game.items().wands().firstHit(owner,origin,end);
            trail(owner,origin,hit==null?end:hit.getLocation(),mode);
            if(hit instanceof EntityHitResult entity) terminal(burst,entity.getLocation(),(LivingEntity)entity.getEntity(),direction);
            else if(hit instanceof BlockHitResult wall && owner.level().hasChunkAt(wall.getBlockPos()))
                terminal(burst,wall.getLocation(),null,direction);
            return;
        }
        // Void stops only at an unloaded chunk; every other block is erased after native entity tracing.
        var wall=mode==4?LbcTerra.wallHit(owner.level(),origin,end,state->false):LbcTerra.wallHit(owner.level(),origin,end);
        if(wall!=null) end=wall.getLocation();
        var hits=new java.util.ArrayList<>(ProjectileUtil.getManyEntityHitResult(owner.level(),owner,origin,end,new AABB(origin,end).inflate(1),
                target->game.items().lifetimes().target(owner,target),0,ClipContext.Block.COLLIDER,true,false));
        hits.sort(java.util.Comparator.comparingDouble(hit->origin.distanceToSqr(hit.getLocation())));
        var seen=new java.util.HashSet<UUID>();
        for(var hit:hits) {
            if(!seen.add(hit.getEntity().getUUID())) continue;
            var target=(LivingEntity)hit.getEntity();
            if(mode==4 && (game.items().mana().hudValue(target,"void_resistance")>0
                    || target.entityTags().contains("sin_greed_active"))) continue;
            var type=mode==0?DamageTypes.ARROW:mode==1?DamageTypes.GENERIC:DamageTypes.OUTSIDE_BORDER;
            boolean accepted=target.hurtServer(owner.level(),new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),owner),
                    mode==0?16:mode==1?12:4);
            if(!valid(owner) || !game.items().currentEpoch(burst.epoch)) return;
            if(mode==1 && accepted) {
                target.setRemainingFireTicks(401); game.items().indirectKills().ignited(target,owner);
            }
        }
        trail(owner,origin,end,mode);
        if(mode==4) erase(owner,origin,end,direction);
        else if(wall!=null && owner.level().hasChunkAt(wall.getBlockPos())) {
            if(mode==1) game.items().blaze().incendiaryCube(owner,Vec3.atCenterOf(wall.getBlockPos()));
            else owner.level().sendParticles(ParticleTypes.WHITE_ASH,end.x,end.y,end.z,100,.2,.2,.2,.01);
        }
    }
    private void terminal(Burst burst,Vec3 point,LivingEntity target,Vec3 direction) {
        var owner=burst.owner;
        if(burst.mode==5) game.items().exasperato().instantBlast(owner,owner.level(),point,4,burst.epoch);
        else if(burst.mode==2) {
            Vec3 impact=target==null?point.subtract(direction.scale(.5)):point;
            double radius=target==null?10:6; int count=0;
            for(var candidate:owner.level().getEntities(owner,new AABB(impact,impact).inflate(radius),
                    entity->game.items().lifetimes().target(owner,entity) && game.items().wands().target(owner,entity)
                            && entity.position().distanceToSqr(impact)<=radius*radius)) {
                game.items().wands().magnumElectro(owner,impact,candidate.getEyePosition());
                if(++count==10) break;
            }
        } else {
            if(target!=null) target.hurtServer(owner.level(),new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.GENERIC),owner),6);
            if(!valid(owner) || !game.items().currentEpoch(burst.epoch)) return;
            game.items().clouds().magnumAcid(owner,target==null?point.subtract(direction.scale(.5)):point,new Vec3(0,target==null?-1:1,0),target==null);
        }
    }
    private void erase(ServerPlayer owner,Vec3 from,Vec3 end,Vec3 direction) {
        game.items().indirectKills().trapChanges(owner,()->{
            var visited=new java.util.HashSet<BlockPos>();
            for(double distance=0;distance<=from.distanceTo(end)+.0001;distance+=.5) {
                var point=from.add(direction.scale(distance));
                for(var pos:BlockPos.betweenClosed(BlockPos.containing(point.add(-.4,-.4,-.4)),BlockPos.containing(point.add(.4,.4,.4)))) {
                    if(visited.add(pos.immutable()) && owner.level().hasChunkAt(pos) && !owner.level().getBlockState(pos).isAir())
                        owner.level().setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE
                                |Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                }
            }
        });
    }
    private static final net.minecraft.core.particles.DustColorTransitionOptions ELECTRIC_DUST=
            new net.minecraft.core.particles.DustColorTransitionOptions(0x00ccff,0x00334d,.8f);
    private static final net.minecraft.core.particles.DustColorTransitionOptions VOID_DUST=
            new net.minecraft.core.particles.DustColorTransitionOptions(0x4d0066,0x000000,1);
    private void trail(ServerPlayer owner,Vec3 origin,Vec3 end,int mode) {
        var tracer=game.items().rifleZoom().tracer(owner);
        net.minecraft.core.particles.ParticleOptions particle=switch(mode) {
            case 1->ParticleTypes.SMALL_FLAME; case 2->ELECTRIC_DUST;
            case 3->ParticleTypes.SNEEZE; case 4->VOID_DUST; case 5->ParticleTypes.LARGE_SMOKE; default->ParticleTypes.ASH;
        };
        Vec3 direction=end.subtract(origin).normalize(); double length=origin.distanceTo(end);
        int count=mode==0?5:mode==3?1:2; double spread=mode==0?.02:.05;
        for(double distance=0;distance<=length;distance+=.5) {
            var point=origin.add(direction.scale(distance));
            tracer.send(particle,point,count,spread,0);
            if(mode==2) tracer.send(ParticleTypes.ELECTRIC_SPARK,point,3,.05,1);
        }
    }
    public int queued(ServerPlayer owner) { var burst=bursts.get(owner.getUUID()); return burst==null?0:burst.remaining; }
    public int shots(ServerPlayer owner) { return fired.getOrDefault(owner.getUUID(),0); }
    void addPlayerCounters(ServerPlayer owner) {
        fired.merge(owner.getUUID(),1,Integer::sum);
        var burst=bursts.get(owner.getUUID());
        if(burst!=null && retained(burst)) { burst.remaining++; return; }
        bursts.remove(owner.getUUID());
        if(!valid(owner)) return;
        InteractionHand hand=isMagnum(owner.getMainHandItem())?InteractionHand.MAIN_HAND:null;
        if(hand!=null) {
            var gun=owner.getItemInHand(hand);
            bursts.put(owner.getUUID(),new Burst(owner,hand,gun,mode(gun),game.items().epoch(),1));
        }
    }
    public void purify(ServerPlayer owner) { game.items().shooters().resetCooldown(owner); }
    public void forget(ServerPlayer owner) { bursts.remove(owner.getUUID()); fired.remove(owner.getUUID()); }
    public void stop() { bursts.clear(); fired.clear(); }
}
