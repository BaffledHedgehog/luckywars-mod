package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** KFC reward abilities. Native bats and item physics; no world/inventory polling. */
public final class LbcKfcRewards {
    public static final Item WIN_BUTTON=registerWin();
    public static final Item PENIS_CULTUS=registerCultus();
    static final ResourceKey<DamageType> CULTUS_ARROW=ResourceKey.create(Registries.DAMAGE_TYPE,Identifier.parse("skywars2:cultus_arrow"));
    private final GameController game;
    private final Map<Entity,UUID> owned=new IdentityHashMap<>();
    private final Map<CultItem,Bomb> bombs=new IdentityHashMap<>();
    private final List<Celebration> celebrations=new ArrayList<>();
    private final Map<LbcStonekivi.Work,UUID> multicasts=new IdentityHashMap<>();
    private final Map<Entity,LivingEntity> looseConsequences=new IdentityHashMap<>();
    private record Bomb(UUID owner,LivingEntity hostileOwner,String epoch,int variant) { }
    private static final class Celebration {
        final UUID owner; final String epoch,tag; final List<Marker> markers=new ArrayList<>();
        int delay=20;
        Celebration(UUID owner,String epoch) {
            this.owner=owner; this.epoch=epoch; tag="skywars2_fake_win_"+UUID.randomUUID().toString().replace("-","");
        }
    }
    LbcKfcRewards(GameController game) { this.game=game; }
    static void register() { }

    private static Item registerWin() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:win_button"));
        return Registry.register(BuiltInRegistries.ITEM,key,new WinButton(new Item.Properties().setId(key).stacksTo(16)
                .overrideDescription("win_button").component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:win_button"))));
    }
    private static final class WinButton extends Item implements PolymerItem {
        WinButton(Properties properties) { super(properties); }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context) { return Items.PAPER; }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            var controller=me.skywars2.SkyWars2.game();
            if(!(player instanceof ServerPlayer owner) || controller==null
                    || !controller.items().kfcRewards().useWin(owner)) return InteractionResult.FAIL;
            // Snowball throwing was only the datapack consumption trigger.
            owner.getItemInHand(hand).consume(1,owner);
            return InteractionResult.SUCCESS;
        }
    }
    private static Item registerCultus() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:penis_cultus"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties().setId(key).durability(25)
                .overrideDescription("penis_cultus").component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:penis_cultus")),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner && controller.items().wands().use(owner,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isCultus(ItemStack stack) {
        var flag=new CompoundTag(); flag.putInt("penis_cultus",1);
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(PENIS_CULTUS) || data!=null && data.matchedBy(flag);
    }
    private boolean active(ServerPlayer owner) {
        return owner!=null && game.items().mana().visible(owner);
    }
    private boolean mayUse(ServerPlayer owner) {
        return active(owner) && !game.items().actionsBlocked(owner) && !game.items().wands().timeStopped()
                && !owner.entityTags().contains("time_stopped") && !game.items().magic().suppressed(owner)
                && !game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());
    }
    public boolean useWin(ServerPlayer owner) {
        if(!mayUse(owner)) return false;
        var closest=owner.level().players().stream().filter(p->!game.isObserver(p))
                .min(java.util.Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
        if(closest!=null) {
            game.announce(OriginalGuiDesign.sourceMessage("title.player_win",closest,game.players().get(closest.getUUID()),game.config()));
            closest.sendSystemMessage(OriginalGuiDesign.sourceMessage("reward.win",closest,game.players().get(closest.getUUID()),game.config()));
        }
        // schedule function ... 1s replaces the source global timer, while earlier markers remain.
        var state=celebrations.stream().filter(entry->game.items().currentEpoch(entry.epoch)).findFirst().orElse(null);
        if(state==null) { state=new Celebration(owner.getUUID(),game.items().epoch()); celebrations.add(state); }
        state.delay=20;
        for(int i=0;i<4;i++) {
            var marker=new Marker(EntityTypes.MARKER,owner.level()); marker.setPos(owner.position());
            marker.entityTags().add(state.tag); game.items().markCurrentItemEpoch(marker);
            if(owner.level().addFreshEntity(marker)) state.markers.add(marker); else marker.discard();
        }
        return true;
    }
    public boolean useCultus(ServerPlayer owner) {
        if(!mayUse(owner))return false;
        if(owner.isShiftKeyDown())return game.items().summons().command(owner);
        if(!game.items().mana().trySpend(owner,250))return false;
        owner.level().sendParticles(ParticleTypes.SOUL,owner.getX(),owner.getEyeY()+2,owner.getZ(),10,.5,.2,.5,0);
        for(var player:List.copyOf(owner.level().players())) if(game.items().wands().target(owner,player))
            spawnBomb(owner,player.position().add(0,5,0),1+owner.getRandom().nextInt(6));
        game.items().mana().award(owner,"lbc:true_advancements/summoner/penis_cultus");
        game.items().trinkets().summoned(owner); return true;
    }
    ItemEntity spawnBomb(ServerPlayer owner,Vec3 position,int variant) {
        if(variant<1 || variant>6) throw new IllegalArgumentException("Unknown cultus variant "+variant);
        var bat=new CultBat(owner.level()); bat.setPos(position); bat.setSilent(true); bat.entityTags().add("no_penis");
        bat.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,-1,0,false,false));
        bat.addEffect(ItemEffectLevels.create(MobEffects.WITHER,-1,0,false,false),owner);
        var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:penis_"+variant));
        stack.set(DataComponents.ITEM_NAME,Component.translatable("penis"));
        var flags=new CompoundTag(); flags.putInt("instantitem",1); flags.putInt("penis_bomb",1); flags.putInt("penis_num",variant);
        if(variant==2) flags.putInt("govnoq",1); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
        var item=new CultItem(owner.level(),position,stack); item.setPermanentlyInvulnerable(true);
        item.entityTags().add("penis"); item.entityTags().add("penis_bomb");
        game.items().markCurrentItemEpoch(bat); game.items().markCurrentItemEpoch(item);
        owned.put(bat,owner.getUUID()); owned.put(item,owner.getUUID());
        bombs.put(item,new Bomb(owner.getUUID(),null,game.items().epoch(),variant));
        if(!owner.level().addFreshEntity(bat) || !owner.level().addFreshEntity(item)) {
            bombs.remove(item); owned.remove(item); owned.remove(bat); bat.discard(); item.discard(); return null;
        }
        item.startRiding(bat,true,true);
        return item;
    }
    /** Source phase2 payload: a free native item, without the player Cultus bat. */
    ItemEntity spawnLooseBomb(LivingEntity hostileOwner,Vec3 position,int variant) {
        if(variant<1 || variant>6) throw new IllegalArgumentException("Unknown cultus variant "+variant);
        if(!(hostileOwner.level() instanceof ServerLevel level)) return null;
        var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:penis_"+variant));
        stack.set(DataComponents.ITEM_NAME,Component.translatable("penis"));
        stack.set(DataComponents.MAX_STACK_SIZE,1);
        stack.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_ff"));
        var flags=new CompoundTag(); flags.putInt("instantitem",1); flags.putInt("penis_bomb",1); flags.putInt("penis_num",variant);
        if(variant==2) flags.putInt("govnoq",1);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
        var item=new CultItem(level,position,stack); item.setPermanentlyInvulnerable(true);
        var random=hostileOwner.getRandom();
        item.setDeltaMovement((random.nextInt(21)-10)*.01,(random.nextInt(21)-10)*.01,(random.nextInt(21)-10)*.01);
        item.entityTags().add("penis_bomb"); game.items().markCurrentItemEpoch(item);
        bombs.put(item,new Bomb(hostileOwner.getUUID(),hostileOwner,game.items().epoch(),variant));
        if(level.addFreshEntity(item)) return item;
        bombs.remove(item); item.discard(); return null;
    }
    private LivingEntity bombOwner(Bomb state) {
        return state==null?null:state.hostileOwner!=null?state.hostileOwner:game.server().getPlayerList().getPlayer(state.owner);
    }
    private boolean payloadOwner(LivingEntity owner) {
        // A dead hostile boss remains the causal owner of payloads it already dropped.
        return owner!=null && (!(owner instanceof ServerPlayer player) || active(player));
    }
    private boolean payloadTarget(LivingEntity owner,LivingEntity victim) {
        return owner instanceof ServerPlayer player?game.items().wands().projectileTarget(player,victim)
                :victim!=owner && !victim.entityTags().contains("helicopter_boss_hitbox")
                        && game.items().wands().validTarget(victim) && !owner.isAlliedTo(victim);
    }
    /** Native bolt damage wrapper reads the real NPC causal entity. */
    public LivingEntity lightningOwner(LightningBolt bolt) { return looseConsequences.get(bolt); }
    private final class LooseBolt extends LightningBolt {
        final String epoch=game.items().epoch();
        LooseBolt(ServerLevel level) { super(EntityTypes.LIGHTNING_BOLT,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public void tick() {
            if(!game.items().currentEpoch(epoch)) { discard(); return; }
            if(game.items().wands().timeStopped() || entityTags().contains("time_stopped")) return;
            super.tick();
        }
    }
    private static final class CultBat extends Bat {
        CultBat(ServerLevel level) { super(EntityTypes.BAT,level); }
        @Override public boolean shouldBeSaved() { return false; }
    }
    private final class CultItem extends ItemEntity {
        CultItem(ServerLevel level,Vec3 point,ItemStack stack) {
            super(level,point.x,point.y,point.z,stack);
            var data=new CompoundTag(); data.putShort("Health",(short)10000);
            data.putShort("Age",(short)-32768); data.putShort("PickupDelay",(short)3);
            data.put("Item",ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),stack).getOrThrow());
            readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
            setDeltaMovement(Vec3.ZERO);
        }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public void tick() {
            var state=bombs.get(this);
            if(state!=null && state.hostileOwner!=null
                    && (game.items().wands().timeStopped() || entityTags().contains("time_stopped"))) return;
            super.tick();
        }
        @Override public void playerTouch(Player player) {
            if(!(player instanceof ServerPlayer target) || hasPickUpDelay()
                    || game.items().wands().timeStopped() || entityTags().contains("time_stopped")) return;
            // Native item pickup requires room. Restrict inspection to the contacting player's inventory.
            boolean room=target.getInventory().getFreeSlot()>=0 || target.getInventory().contains(stack->
                    ItemStack.isSameItemSameComponents(stack,getItem()) && stack.getCount()<stack.getMaxStackSize());
            if(room) activate(this,target);
        }
    }
    public ServerPlayer owner(Entity entity) {
        var id=owned.get(entity); return id==null || !game.items().currentEpoch(entity)?null:game.server().getPlayerList().getPlayer(id);
    }
    public boolean allowDamage(LivingEntity victim,DamageSource source) {
        if(LbcDamage.isNativeLightning(source)) return game.items().wands().validTarget(victim);
        var hostile=looseConsequences.get(source.getDirectEntity());
        if(hostile!=null) return payloadTarget(hostile,victim);
        var owner=owner(source.getDirectEntity());
        return owner==null || game.items().wands().projectileTarget(owner,victim);
    }

    private boolean activate(CultItem item,LivingEntity victim) {
        var state=bombs.get(item); var owner=bombOwner(state);
        if(!payloadOwner(owner) || !game.items().currentEpoch(state.epoch) || !payloadTarget(owner,victim)
                || victim.entityTags().contains("no_penis")) return false;
        // Detach before discard: the bat remains native and dies from its source wither effect.
        bombs.remove(item); owned.remove(item); item.stopRiding(); item.discard();
        effect(owner,victim,state.variant); return true;
    }
    private void effect(LivingEntity owner,LivingEntity victim,int variant) {
        var level=(ServerLevel)victim.level(); var at=victim.position();
        String[] keys={"normal","dirty","nigger","holy","horse","multi"};
        ChatFormatting[] colors={ChatFormatting.LIGHT_PURPLE,ChatFormatting.DARK_RED,ChatFormatting.DARK_GRAY,
                ChatFormatting.WHITE,ChatFormatting.LIGHT_PURPLE,ChatFormatting.GRAY};
        if(victim instanceof ServerPlayer) {
            var text=Component.literal("").append(victim.getDisplayName()).append(Component.translatable("love_"+keys[variant-1]+"_penis"));
            if(variant==1) text.withStyle(style->style.withColor(0xE921FF));
            else if(variant==2) text.withStyle(style->style.withColor(0x5C2A0E)); else text.withStyle(colors[variant-1]);
            game.announce(text);
        }
        switch(variant) {
            case 1 -> { damage(owner,victim,6); level.sendParticles(ParticleTypes.CLOUD,at.x,at.y+1,at.z,10,2,2,2,.1); }
            case 2 -> {
                victim.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,160,5,false,false),owner);
                victim.addEffect(ItemEffectLevels.create(MobEffects.WEAKNESS,80,5,false,false),owner);
                victim.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,120,5,false,false),owner);
                damage(owner,victim,4);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIRT.defaultBlockState()),at.x,at.y+1,at.z,100,.2,.2,.2,1);
            }
            case 3 -> {
                game.items().exasperato().protectedBlast(owner,level,at,4,null);
            }
            case 4 -> {
                victim.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,160,1,false,false),owner);
                victim.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,15,false,false),owner);
                LightningBolt bolt;
                if(owner instanceof ServerPlayer player) bolt=game.items().electro().lightning(player,at);
                else {
                    bolt=new LooseBolt(level); bolt.setPos(at); game.items().markCurrentItemEpoch(bolt);
                    looseConsequences.put(bolt,owner);
                    if(!level.addFreshEntity(bolt)) { looseConsequences.remove(bolt); bolt.discard(); bolt=null; }
                }
                if(bolt!=null) {
                    // Shared native lifetime/fire tracking, with the source vanilla lightning damage type.
                    bolt.entityTags().remove(LbcElectro.SHOCK_BOLT);
                    game.items().markCurrentItemEpoch(bolt); if(owner instanceof ServerPlayer) owned.put(bolt,owner.getUUID());
                }
                level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH,0xFFFFFFCC),at.x,at.y+1,at.z,100,2,2,2,1);
            }
            case 5 -> {
                String effectEpoch=game.items().epoch();
                victim.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,20,127,false,false),owner); damage(owner,victim,10);
                if(!victim.isAlive() || !game.items().currentEpoch(effectEpoch) || game.stage()==GameController.Stage.ENDING) return;
                var destination=at.add(victim.getLookAngle().scale(-5)).add(0,5,0);
                if(owner instanceof ServerPlayer player) game.items().indirectKills().displaced(victim,player);
                victim.teleportTo(destination.x,destination.y,destination.z);
                at=victim.position();
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.REDSTONE_BLOCK.defaultBlockState()),at.x,at.y+1,at.z,100,2,2,2,1);
            }
            case 6 -> { multicast(owner,victim); level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState()),at.x,at.y+1,at.z,100,2,2,2,1); }
            default -> throw new IllegalArgumentException("Unknown cultus variant "+variant);
        }
        String[] sounds={"suction","dick_1","spank","woo","shityeah","weewee"};
        if(owner instanceof ServerPlayer player) LbcOverload.sound(player,at,"minecraft:lbcsounds."+sounds[variant-1],1,1,0);
        else {
            var sound=Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:lbcsounds."+sounds[variant-1])));
            long seed=owner.getRandom().nextLong(); double range=sound.value().getRange(1);
            for(var viewer:level.players()) if(viewer.distanceToSqr(at)<=range*range)
                viewer.connection.send(new ClientboundSoundPacket(sound,SoundSource.MASTER,at.x,at.y,at.z,1,1,seed));
        }
    }
    /** Shared arrow damage keeps the established source protection and cooldown rules. */
    private void multicast(LivingEntity owner,LivingEntity victim) {
        int count=0;
        for(var entity:((ServerLevel)owner.level()).getAllEntities()) if(!entity.entityTags().contains("spectator")) count++;
        final int repetitions=count; final String epoch=game.items().epoch();
        var job=new LbcStonekivi.Work() {
            int remaining=repetitions;
            public boolean step() {
                if(!multicasts.containsKey(this) || !payloadOwner(owner) || !game.items().currentEpoch(epoch)
                        || !payloadTarget(owner,victim)) { cancel(); return true; }
                if(game.items().wands().timeStopped() || victim.entityTags().contains("time_stopped")) return false;
                if(remaining>0) { damage(owner,victim,.1f); remaining--; }
                if(remaining==0) { cancel(); return true; }
                return false;
            }
            public void cancel() { multicasts.remove(this); }
        };
        multicasts.put(job,owner.getUUID()); game.items().stonekivi().queuedWork(job);
    }
    private void damage(LivingEntity owner,LivingEntity victim,float amount) {
        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(CULTUS_ARROW),owner);
        victim.hurtServer((ServerLevel)victim.level(),source,amount);
    }
    public void tick() {
        for(var item:List.copyOf(bombs.keySet())) {
            var state=bombs.get(item); var owner=bombOwner(state);
            if(item.isRemoved() || !payloadOwner(owner) || !game.items().currentEpoch(state.epoch)) { remove(item); continue; }
            if(game.items().wands().timeStopped() || item.entityTags().contains("time_stopped")) continue;
            // Only the tracked item neighbourhood; native playerTouch handles player contact.
            var candidates=item.level().getEntitiesOfClass(LivingEntity.class,item.getBoundingBox().inflate(1.2),
                    target->!(target instanceof Player) && !target.entityTags().contains("no_penis")
                            && payloadTarget(owner,target) && target.distanceToSqr(item)<=1.44);
            var target=candidates.stream().min(java.util.Comparator.comparingDouble(item::distanceToSqr)).orElse(null);
            if(target!=null) activate(item,target);
        }
        owned.keySet().removeIf(Entity::isRemoved);
        looseConsequences.keySet().removeIf(Entity::isRemoved);
        for(var state:List.copyOf(celebrations)) {
            if(!game.items().currentEpoch(state.epoch)) { remove(state); continue; }
            if(--state.delay>0) continue; state.delay=20;
            game.command("spreadplayers 0 0 0 192 false @e[type=minecraft:marker,tag="+state.tag+"]");
            for(var marker:state.markers) if(!marker.isRemoved()) game.events().spawnVictoryFirework(marker.getX(),marker.getY(),marker.getZ());
        }
    }
    private void remove(CultItem item) { bombs.remove(item); owned.remove(item); if(!item.isRemoved()) item.discard(); }
    private void remove(Celebration state) { celebrations.remove(state); for(var marker:state.markers) marker.discard(); }
    public void entityUnloaded(Entity entity) {
        if(entity instanceof CultItem item) remove(item);
        owned.remove(entity); looseConsequences.remove(entity);
        for(var state:List.copyOf(celebrations)) if(state.markers.contains(entity)) remove(state);
    }
    public void clear(ServerPlayer owner) {
        for(var item:List.copyOf(bombs.keySet())) if(bombs.get(item).owner.equals(owner.getUUID())) remove(item);
        for(var entity:List.copyOf(owned.keySet())) if(owned.get(entity).equals(owner.getUUID())) { owned.remove(entity); entity.discard(); }
        for(var job:List.copyOf(multicasts.keySet())) if(multicasts.get(job).equals(owner.getUUID())) job.cancel();
    }
    public void stop() {
        for(var entity:List.copyOf(looseConsequences.keySet())) entity.discard();
        looseConsequences.clear();
        for(var item:List.copyOf(bombs.keySet())) remove(item);
        for(var entity:List.copyOf(owned.keySet())) { owned.remove(entity); entity.discard(); }
        for(var state:List.copyOf(celebrations)) remove(state);
        for(var job:List.copyOf(multicasts.keySet())) job.cancel();
    }
    int bombCount() { return bombs.size(); }
    int celebrationCount() { return celebrations.size(); }
    int pendingMulticastCount() { return multicasts.size(); }
}
