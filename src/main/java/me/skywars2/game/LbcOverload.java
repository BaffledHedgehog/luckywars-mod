package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

/** Долг маны, Crux и лучи перегрузки; движение брони отделено от расхода ресурса. */
public final class LbcOverload {
    public static final Item CRUX=registerCrux();
    public static final Item HELMET=registerArmor("skywars2:overload_helmet",Items.LEATHER_HELMET,ArmorType.HELMET);
    public static final Item CHESTPLATE=registerArmor("skywars2:overload_chestplate",Items.LEATHER_CHESTPLATE,ArmorType.CHESTPLATE);
    public static final Item LEGGINGS=registerArmor("skywars2:overload_leggings",Items.LEATHER_LEGGINGS,ArmorType.LEGGINGS);
    public static final Item BOOTS=registerArmor("skywars2:overload_boots",Items.LEATHER_BOOTS,ArmorType.BOOTS);
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=
            TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final CompoundTag CRUX_DATA=new CompoundTag();
    private static final ScoreHolder COST=ScoreHolder.forNameOnly("overload_crux_mana_use");
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final Item[] DIAMOND={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
    private static final String[] ARMOR_LOOT={"lbc:ported/overload_helmet","lbc:ported/overload_chestplate",
            "lbc:ported/overload_leggings","lbc:ported/overload_boots"};
    // Native FillCommand's strict placement flags: clients update, neighbor/placement side effects skip.
    private static final int STRICT_FILL_FLAGS=818;
    static { CRUX_DATA.putInt("overload_crux",1); }
    private final GameController game;
    private final LbcMana mana;
    private final LbcOverloadArmor armor;

    private static Item registerCrux() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:overload_crux"));
        var properties=new Item.Properties().setId(key).stacksTo(99).overrideDescription("overload_crux");
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var game=SkyWars2.game();
                return game!=null && player instanceof ServerPlayer serverPlayer
                        && game.items().mana().overload().use(serverPlayer,hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
    }
    private static Item registerArmor(String id,Item proxy,ArmorType type) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties().setId(key).humanoidArmor(ArmorMaterials.LEATHER,type),proxy,true));
    }
    static void register() { }
    LbcOverload(GameController game,LbcMana mana) {
        this.game=game; this.mana=mana; this.armor=new LbcOverloadArmor(game,mana);
        mana.score(COST,"lbc.math").set(7500);
    }
    public LbcOverloadArmor armor() { return armor; }
    private boolean active(ServerPlayer p) {
        return mana.visible(p) && !p.entityTags().contains("time_stopped") && !game.items().magic().suppressed(p);
    }
    static boolean isCrux(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(CRUX) || data!=null && data.matchedBy(CRUX_DATA);
    }
    /** Источник выдаёт один Crux до регенерации; переполнение положительного cap перевооружает выдачу. */
    void beforeRegen(ServerPlayer p) {
        if(!active(p) || mana.noMagic(p)>0) return;
        int value=mana.get(p);
        if(value<=0 && !p.entityTags().contains("obtained_crux")) {
            for(var stack:game.lbc().rollTable(p.level(),p.getEyePosition(),p,"lbc:ported/overload_crux")) {
                var drop=new ItemEntity(p.level(),p.getX(),p.getEyeY(),p.getZ(),stack);
                drop.setDefaultPickUpDelay(); p.level().addFreshEntity(drop);
            }
            p.level().sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1),p.getX(),p.getEyeY(),p.getZ(),10,0,0,0,.1);
            sendSound(p,p,"minecraft:entity.elder_guardian.curse",1,0);
            p.entityTags().add("obtained_crux");
        }
        if(value>=mana.maximum(p) && mana.maximum(p)>0) p.entityTags().remove("obtained_crux");
    }
    /** Применение Crux всегда допускает долг; полный комплект не подменяет ману временными миллионами. */
    public boolean use(ServerPlayer p,InteractionHand hand) {
        if(!active(p) || !isCrux(p.getItemInHand(hand))) return false;
        mana.add(p,-mana.score(COST,"lbc.math").get());
        sound(p,"minecraft:lbcsounds.overload_jump",1,2,0);
        sound(p,"minecraft:block.enchantment_table.use",1,.8f,0);
        for(int i=0;i<5;i++) cast(p,p.getEyePosition(),true,false);
        // Source loot-replace creates fresh armor: diamond enchantments/damage are not copied.
        for(int i=0;i<SLOTS.length;i++) if(p.getItemBySlot(SLOTS[i]).is(DIAMOND[i])) {
            var output=game.lbc().rollTable(p.level(),p.position(),p,ARMOR_LOOT[i]);
            if(!output.isEmpty()) {
                p.setItemSlot(SLOTS[i],output.getFirst());
                sound(p,"minecraft:item.trident.thunder",1,2,0);
            }
        }
        p.getInventory().clearOrCountMatchingItems(LbcOverload::isCrux,false,1,p.inventoryMenu.getCraftSlots());
        mana.inventoryChanged(p); p.inventoryMenu.broadcastChanges();
        return true;
    }
    static int debtStage(int value) {
        return value>=0 ? 0 : value>=-5000 ? 1 : value>=-10000 ? 2 : value>=-15000 ? 3 : value>=-20000 ? 4 : 5;
    }
    double debtDamage(ServerPlayer p,boolean helmet) {
        int value=mana.get(p), max=mana.maximum(p);
        // Failed scoreboard /= zero leaves its already-copied numerator intact; do not throw or clamp mana.
        int quotient=max==0 ? value : Math.floorDiv(value,max);
        int difference=Mth.floor(p.getMaxHealth())-quotient;
        return difference*(helmet ? .045 : .1);
    }
    void afterRegen(ServerPlayer p) {
        if(!active(p) || mana.noMagic(p)>0) return;
        int stage=debtStage(mana.get(p)); if(stage==0) return;
        int mask=mana.overloadArmor(p); boolean helmet=(mask&1)!=0;
        if(!helmet) {
            int slow=stage==1 ? 1 : stage==2 ? 2 : stage==3 ? 6 : 127;
            effect(p,MobEffects.SLOWNESS,20,slow);
            if(stage>=2) { effect(p,MobEffects.MINING_FATIGUE,20,stage==2 ? 0 : stage==3 ? 2 : 127);
                effect(p,MobEffects.WEAKNESS,20,stage==2 ? 0 : stage==3 ? 1 : 127); }
            if(stage>=3) { effect(p,MobEffects.NAUSEA,stage==3 ? 80 : 120,0);
                effect(p,MobEffects.HUNGER,20,stage==3 ? 0 : stage==4 ? 2 : 6); }
            if(stage==5) effect(p,MobEffects.BLINDNESS,40,0);
        }
        if(stage==5) {
            game.items().indirectKills().manaDamage(p,()-> {
            double damage=debtDamage(p,helmet);
            int health=helmet ? Mth.ceil(p.getHealth()+p.getAbsorptionAmount()) : Mth.floor(p.getHealth());
            if(Mth.floor(damage)>=health) explode(p);
            else if(damage>=0) {
                cast(p,p.getEyePosition(),mask==15,false);
                if(!p.entityTags().contains("overload_slam")) p.hurtServer(p.level(),p.damageSources().fellOutOfWorld(),(float)damage);
            }
            });
        }
        if(p.isAlive() && (mask&2)!=0)
            p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,20,stage-1,false,false,false));
        if(p.isAlive() && mana.get(p)<0 && mana.regeneration(p)<0 && mana.maximum(p)<0)
            mana.award(p,"lbc:true_advancements/potions/oh_no_mana");
    }
    private static void effect(ServerPlayer p,Holder<net.minecraft.world.effect.MobEffect> effect,int ticks,int amplifier) {
        p.addEffect(new MobEffectInstance(effect,ticks,amplifier,false,true,true));
    }
    private boolean target(ServerPlayer owner,LivingEntity target) {
        return target!=owner && target.isAlive() && !target.isRemoved() && !target.entityTags().contains("spectator")
                && target.getType().builtInRegistryHolder().is(MOBS)
                && (!(target instanceof ServerPlayer p) || game.canUseGameplayItems(p));
    }
    private void cast(ServerPlayer p,Vec3 origin,boolean full,boolean big) {
        var random=p.getRandom();
        float yaw=big ? (random.nextInt(36001)-18000)*.01f
                : p.getYRot()+(random.nextInt(full ? 2001 : 36001)-(full ? 1000 : 18000))*.01f;
        float pitch=big ? (random.nextInt(36001)-18000)*.01f
                : p.getXRot()+(random.nextInt(full ? 2001 : 8001)-(full ? 1000 : 4000))*.01f;
        float bendYaw=big ? 0 : (random.nextInt(full ? 1001 : 1201)-(full ? 500 : 600))*.01f;
        float bendPitch=big ? 0 : (random.nextInt(full ? 601 : 801)-(full ? 300 : 400))*.01f;
        double amount=mana.get(p)*(big ? -.00025 : full ? -.0001 : -.00006);
        ray(p,origin,yaw,pitch,bendYaw,bendPitch,big ? 31 : full ? 61 : 31,amount,big);
    }
    /** Одна нативная spatial query на луч вместо селектора на каждой точке; изгиб/лимиты source сохранены. */
    void ray(ServerPlayer p,Vec3 origin,float yaw,float pitch,float bendYaw,float bendPitch,int points,double amount,boolean big) {
        if(points<=0 || big && amount<0) return; // Big-ray macro cannot instantiate a negative /damage.
        var level=p.level();
        double radius=big ? 34 : 22;
        var candidates=level.getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(radius),e->target(p,e));
        Vec3 point=origin;
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.OUTSIDE_BORDER),p);
        for(int i=0;i<points;i++) {
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1),point.x,point.y,point.z,big ? 10 : 1,
                    big ? .3 : 0,big ? .3 : 0,big ? .3 : 0,0);
            int hits=0; var cell=new AABB(point,point.add(1,1,1));
            for(var e:candidates) if(amount>=0 && target(p,e) && e.position().distanceToSqr(point)<=(big ? 4 : 9)
                    && (big || e.getBoundingBox().intersects(cell))) {
                e.hurtServer(level,damage,(float)amount);
                if(!big && mana.overloadArmor(p)==15 && p.isAlive()) {
                    // EffectCommands treats the instant-effect duration argument as ticks, not seconds.
                    p.addEffect(new MobEffectInstance(MobEffects.SATURATION,1,0,false,false,false));
                    if(p.getRandom().nextFloat()<.005f)
                        p.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH,1,0,false,false,false));
                }
                if(++hits==(big ? 2 : 1)) break;
            }
            if(big) {
                var center=BlockPos.containing(point);
                game.items().indirectKills().worldChanges(level,p,()-> {
                for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))
                    if(level.hasChunkAt(pos) && !level.getBlockState(pos).isAir()) level.setBlock(pos,Blocks.AIR.defaultBlockState(),STRICT_FILL_FLAGS);
                });
            }
            yaw+=bendYaw; pitch+=bendPitch;
            Vec3 next=point.add(Vec3.directionFromRotation(pitch,yaw).scale(big ? 1 : .3));
            if(!big && !level.getBlockState(BlockPos.containing(next)).is(AIRS)) break;
            point=next;
        }
    }
    private void explode(ServerPlayer p) {
        Vec3 origin=p.position();
        for(int i=0;i<6;i++) cast(p,origin,false,false);
        for(int i=0;i<15;i++) cast(p,origin,false,true);
        sound(p,"minecraft:lbcsounds.impulse",2,1,40);
        // LivingEntity.kill использует этот же смертельный урон. Вызываем его напрямую:
        // Carpet переопределяет kill() простым disconnect, обходя die, чат и награды.
        p.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE);
    }
    public void clearEffects(ServerPlayer p) { armor.clear(p); }
    public void reset(ServerPlayer p) { clearEffects(p); p.entityTags().remove("obtained_crux"); }
    /** @a выбирает всех, но native playsound затем отсекает другое измерение и неслышимую дистанцию. */
    static void sound(ServerPlayer p,String id,float volume,float pitch,double radius) {
        sound(p,p.position(),id,volume,pitch,radius);
    }
    static void sound(ServerPlayer p,Vec3 position,String id,float volume,float pitch,double radius) {
        sound(p,position,id,volume,pitch,radius,0);
    }
    /** Аналог minVolume у playsound: радиус selector и слышимость — разные ограничения. */
    static void sound(ServerPlayer p,Vec3 position,String id,float volume,float pitch,double radius,float minVolume) {
        sound(p.level(),position,id,volume,pitch,radius,minVolume,p.getRandom().nextLong());
    }
    static void sound(net.minecraft.server.level.ServerLevel level,Vec3 position,String id,float volume,float pitch,double radius,float minVolume) {
        sound(level,position,id,volume,pitch,radius,minVolume,level.getRandom().nextLong());
    }
    private static void sound(net.minecraft.server.level.ServerLevel level,Vec3 position,String id,float volume,float pitch,double radius,float minVolume,long seed) {
        var sound=Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(id)));
        double range=sound.value().getRange(volume);
        double rangeSquared=range*range;
        for(var viewer:level.getServer().getPlayerList().getPlayers()) {
            if(viewer.level()!=level) continue;
            double distanceSquared=viewer.position().distanceToSqr(position);
            if(radius>0 && distanceSquared>radius*radius) continue;
            Vec3 heardAt=position; float heardVolume=volume;
            if(distanceSquared>rangeSquared) {
                if(minVolume<=0) continue;
                heardAt=viewer.position().add(position.subtract(viewer.position()).normalize().scale(2));
                heardVolume=minVolume;
            }
                viewer.connection.send(new ClientboundSoundPacket(sound,SoundSource.MASTER,
                        heardAt.x,heardAt.y,heardAt.z,heardVolume,pitch,seed));
        }
    }
    private static void sendSound(ServerPlayer viewer,ServerPlayer source,String id,float volume,float pitch) {
        sendSound(viewer,source,source.position(),id,volume,pitch);
    }
    private static void sendSound(ServerPlayer viewer,ServerPlayer source,Vec3 position,String id,float volume,float pitch) {
        viewer.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(id))),
                SoundSource.MASTER,position.x,position.y,position.z,volume,pitch,source.getRandom().nextLong()));
    }
}
