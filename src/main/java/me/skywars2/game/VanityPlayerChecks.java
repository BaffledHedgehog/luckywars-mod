package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.PlayerCheckpointAccess;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Composes core/body owners using real UUID sessions, death, replacement and dimension packets. */
public final class VanityPlayerChecks {
    private VanityPlayerChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why) { if(!ok)throw new IllegalStateException("Vanity player: "+why); }
    private static java.lang.reflect.Field field(Class<?> type,String name) {
        try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static void set(Object target,Class<?> type,String name,Object value) {try{field(type,name).set(target,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static int integer(Object target,Class<?> type,String name) {try{return field(type,name).getInt(target);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final MatchCoreCheckpoint originalCore;
        final ChefChecks.Fixture base;
        final UUID id=GameChecks.offlineUuid("TestGamma");
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final boolean frozen;
        ServerPlayer gamma;
        MatchCoreCheckpoint core;
        MatchPlayerCheckpoint image;
        ElementalGrimoireChecks.Recording wire;
        ItemStack bow;
        AdvancementHolder advancement,rewardProbe,tab;
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipe,futureRecipe;
        final Identifier savedPost=Identifier.parse("lbc:zoom_magnum"),futurePost=Identifier.parse("lbc:zoom_slg");
        final Vec3 motion=new Vec3(12.25,-13.5,14.75),known=new Vec3(.27,.04,-.32);
        Vec3 position;
        int phase=-1,ticks,clock,xp,deathCount,profileDeaths,useRemaining;
        double hidden;
        boolean owned,closed;

        Fixture(GameController game) {
            this.game=game;frozen=game.server().tickRateManager().isFrozen();
            require(game.playing()&&!game.maps().busy()&&!frozen&&game.server().getPlayerList().getPlayer(id)==null,"exclusive real match and owned extra UUID");
            originalCore=new MatchCoreCheckpoint(game);base=new ChefChecks.Fixture(game);
            for(var level:game.server().getAllLevels())level.getAllEntities().forEach(before::add);
        }
        void init() {
            base.init();gamma=GameChecks.createBot(game,"TestGamma");owned=true;
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
            SkyWars2.LOGGER.info("[check] START vanity-player");
        }
        void connectWire() {
            wire=new ElementalGrimoireChecks.Recording(gamma);
            gamma.connection=new ServerGamePacketListenerImpl(game.server(),wire,gamma,
                    new CommonListenerCookie(gamma.getGameProfile(),0,gamma.clientInformation(),false));
            gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        void ack() {
            gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var positions=packets().stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
            if(!positions.isEmpty())gamma.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(positions.getLast().id(),gamma.getX(),gamma.getY(),gamma.getZ(),gamma.getYRot(),gamma.getXRot()));
        }
        List<net.minecraft.network.protocol.Packet<?>> packets() {
            var result=new ArrayList<net.minecraft.network.protocol.Packet<?>>();for(var packet:wire.packets)flatten(packet,result);return result;
        }
        void flatten(net.minecraft.network.protocol.Packet<?> packet,List<net.minecraft.network.protocol.Packet<?>> out) {
            if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flatten(child,out);else out.add(packet);
        }
        void prepare() {
            require(game.allowDamage(gamma)&&game.lives(id)==1,"real native operator enrollment");
            game.server().tickRateManager().setFrozen(true);base.move(gamma,base.at);connectWire();gamma.setNoGravity(true);
            gamma.removeAllEffects();gamma.getInventory().clearContent();gamma.getEnderChestInventory().clearContent();
            gamma.entityTags().add("checkpoint.before");gamma.setAirSupply(1777);gamma.setRemainingFireTicks(50000);gamma.setTicksFrozen(123);
            bow=new ItemStack(Items.BOW);var tag=new CompoundTag();tag.putString("checkpoint","exact payload");bow.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            bow.set(DataComponents.CUSTOM_NAME,Component.literal("Saved bow"));
            var ench=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);ench.set(game.server().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),1000);
            bow.set(DataComponents.ENCHANTMENTS,ench.toImmutable());
            gamma.getInventory().setSelectedSlot(3);gamma.setItemInHand(InteractionHand.MAIN_HAND,bow.copy());gamma.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
            gamma.getInventory().setItem(12,new ItemStack(Items.DIAMOND,17));gamma.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
            gamma.getEnderChestInventory().setItem(5,new ItemStack(Items.EMERALD,23));
            gamma.inventoryMenu.getCraftSlots().setItem(0,new ItemStack(Items.APPLE,3));gamma.inventoryMenu.setCarried(new ItemStack(Items.GOLD_INGOT,7));
            try{var detect=net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");detect.setAccessible(true);detect.invoke(gamma);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            gamma.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80);
            gamma.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(Identifier.parse("skywars2:checkpoint_transient"),40,AttributeModifier.Operation.ADD_VALUE));
            gamma.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.27);gamma.getAbilities().setWalkingSpeed(.13f);gamma.getAbilities().setFlyingSpeed(.073f);
            gamma.setHealth(120);gamma.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,600,-5));gamma.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,400,0));gamma.setHealth(100);
            hidden=ShadowAbsorption.state(gamma).stored;require(hidden>0,"actual signed absorption has hidden healed HP");
            gamma.addEffect(new MobEffectInstance(MobEffects.SPEED,200,1));gamma.addEffect(new MobEffectInstance(MobEffects.SPEED,500,-2));
            gamma.addEffect(new MobEffectInstance(MobEffects.HASTE,1000,0));
            game.items().ownedEffects().grant(gamma,"checkpoint:a",new MobEffectInstance(MobEffects.HASTE,400,0),gamma);
            game.items().ownedEffects().grant(gamma,"checkpoint:b",new MobEffectInstance(MobEffects.HASTE,700,2),gamma);
            gamma.getPostEffects().clear();gamma.getPostEffects().add(savedPost);
            gamma.getFoodData().setFoodLevel(13);gamma.getFoodData().setSaturation(2.5f);set(gamma.getFoodData(),net.minecraft.world.food.FoodData.class,"exhaustionLevel",1.75f);
            gamma.getCooldowns().addCooldown(new ItemStack(Items.ENDER_PEARL),120);
            var recipes=new ArrayList<>(game.server().getRecipeManager().getRecipes());require(recipes.size()>1,"real native recipe registry");
            recipe=recipes.get(0).id();futureRecipe=recipes.get(1).id();
            var book=gamma.getRecipeBook();((PlayerCheckpointAccess.Recipes)book).skywars2$apply(new net.minecraft.stats.ServerRecipeBook.Packed(book.pack().settings(),List.of(),List.of()));book.add(recipe);
            advancement=game.server().getAdvancements().getAllAdvancements().stream().filter(a->!a.value().criteria().isEmpty()).findFirst().orElseThrow();
            tab=game.server().getAdvancements().getAllAdvancements().stream().filter(a->a.value().isRoot()&&a.value().display().isPresent()).findFirst().orElseThrow();
            gamma.getAdvancements().setSelectedTab(tab);
            for(var criterion:advancement.value().criteria().keySet())gamma.getAdvancements().award(advancement,criterion);
            // Production filters vanilla advancements and its LBC tree has no rewards.
            // A scoped native holder exercises actual XP rewards without changing that registry/filter.
            var a=advancement.value();rewardProbe=new AdvancementHolder(Identifier.parse("skywars2:checkpoint_reward_probe"),
                    new net.minecraft.advancements.Advancement(java.util.Optional.empty(),java.util.Optional.empty(),
                            net.minecraft.advancements.AdvancementRewards.Builder.experience(77).build(),a.criteria(),a.requirements(),false));
            int priorXp=gamma.totalExperience;
            for(var criterion:rewardProbe.value().criteria().keySet())gamma.getAdvancements().award(rewardProbe,criterion);
            require(gamma.getAdvancements().getOrStartProgress(advancement).isDone()&&gamma.getAdvancements().getOrStartProgress(rewardProbe).isDone()
                    &&gamma.totalExperience==priorXp+77,"registered native criterion and scoped native reward holder completed before capture");
            gamma.experienceLevel=17;gamma.totalExperience=1234;gamma.experienceProgress=.375f;xp=gamma.totalExperience;
            gamma.getStats().setValue(gamma,Stats.CUSTOM.get(Stats.JUMP),37);deathCount=gamma.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));profileDeaths=game.players().get(id).deaths;
            gamma.setShiftKeyDown(true);gamma.setSprinting(true);gamma.setPose(Pose.CROUCHING);gamma.setOnGround(false);
            gamma.setYRot(37.5f);gamma.setXRot(-12.25f);gamma.setYHeadRot(49);gamma.yBodyRot=61;
            gamma.setKnownMovement(known);gamma.setDeltaMovement(motion);gamma.startUsingItem(InteractionHand.MAIN_HAND);useRemaining=gamma.getUseItemRemainingTicks();
            set(gamma,net.minecraft.world.entity.LivingEntity.class,"attackStrengthTicker",2);set(gamma,net.minecraft.world.entity.LivingEntity.class,"itemSwapTicker",3);
            gamma.setInvulnerableTime(9);gamma.hurtTime=3;gamma.hurtDuration=10;gamma.damageCooldownTime=7;gamma.fallDistance=14.5;
            position=gamma.position();image=new MatchPlayerCheckpoint(game,gamma);core=new MatchCoreCheckpoint(game);clock=game.matchTicks();phase=0;
        }
        void mutate(boolean kill) {
            gamma.stopUsingItem();gamma.removeAllEffects();gamma.getInventory().clearContent();gamma.getEnderChestInventory().clearContent();
            gamma.getInventory().setItem(15,new ItemStack(Items.DIRT,64));gamma.getEnderChestInventory().setItem(8,new ItemStack(Items.DIRT,64));
            gamma.inventoryMenu.getCraftSlots().setItem(0,new ItemStack(Items.DIRT,5));gamma.inventoryMenu.setCarried(new ItemStack(Items.DIRT,9));
            gamma.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);gamma.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.11);
            gamma.getPostEffects().clear();gamma.getPostEffects().add(futurePost);gamma.entityTags().remove("checkpoint.before");gamma.entityTags().add("checkpoint.future");
            gamma.getFoodData().setFoodLevel(2);gamma.experienceLevel=1;gamma.totalExperience=1;gamma.experienceProgress=0;
            gamma.getStats().setValue(gamma,Stats.CUSTOM.get(Stats.JUMP),999);gamma.getStats().setValue(gamma,Stats.CUSTOM.get(Stats.FLY_ONE_CM),999);
            for(var criterion:advancement.value().criteria().keySet())gamma.getAdvancements().revoke(advancement,criterion);
            for(var criterion:rewardProbe.value().criteria().keySet())gamma.getAdvancements().revoke(rewardProbe,criterion);
            gamma.getAdvancements().setSelectedTab(null);
            gamma.getRecipeBook().remove(recipe);gamma.getRecipeBook().add(futureRecipe);
            gamma.getCooldowns().removeCooldown(Identifier.withDefaultNamespace("ender_pearl"));gamma.getCooldowns().addCooldown(new ItemStack(Items.APPLE),600);
            gamma.setDeltaMovement(Vec3.ZERO);gamma.setKnownMovement(Vec3.ZERO);gamma.setShiftKeyDown(false);gamma.setSprinting(false);gamma.setPose(Pose.STANDING);
            if(kill) {
                var nether=game.server().getLevel(Level.NETHER);nether.getChunk(0,0);
                // Preserve preexisting entities deserialized by the deliberate warm load.
                for(var level:game.server().getAllLevels())level.getAllEntities().forEach(before::add);
                require(gamma.teleportTo(nether,.5,200,.5,Set.of(),0,0,true),"actual native cross-dimension mutation");ack();
                gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;
                require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying()&&game.lives(id)==0,"actual native death before checkpoint-driven respawn");
            }
        }
        void restore() {
            var beforeCore=gamma.getHealth()+"/"+gamma.isDeadOrDying();core.restoreFrozen();var previous=gamma;
            var afterCore=gamma.getHealth()+"/"+gamma.isDeadOrDying();wire.packets.clear();gamma=image.restoreFrozen();verifyMovementBeforeInput();ack();
            require(game.server().getPlayerList().getPlayer(id)==gamma&&game.allowDamage(gamma)&&game.lives(id)==1&&!game.awaitingRespawn(id),"core/body composition keeps actual participant lives and current UUID session");
            if(phase==0)require(previous!=gamma&&packets().stream().anyMatch(ClientboundRespawnPacket.class::isInstance),
                    "checkpoint uses real native respawn replacement without manual client respawn or kit reroll: same="+(previous==gamma)+", beforeCore="+beforeCore+", afterCore="+afterCore);
        }
        void verifyMovementBeforeInput() {
            require(gamma.level()==base.owner.level()&&gamma.position().equals(position)&&gamma.getYRot()==37.5f&&gamma.getXRot()==-12.25f
                    &&gamma.getDeltaMovement().equals(motion)&&gamma.getKnownMovement().equals(known)&&gamma.getYHeadRot()==49&&gamma.yBodyRot==61,
                    "exact movement before new native input: pos="+gamma.position()+", wanted="+position+", motion="+gamma.getDeltaMovement()+", known="+gamma.getKnownMovement()+", head="+gamma.getYHeadRot());
        }
        void verify() {
            // Native teleport ACK is real subsequent movement input and recomputes known movement.
            require(gamma.level()==base.owner.level()&&gamma.position().equals(position)&&gamma.getYRot()==37.5f&&gamma.getXRot()==-12.25f
                    &&gamma.getDeltaMovement().equals(motion),"exact dimension, position, camera and unclamped motion after actual native ACK: pos="+gamma.position()+", motion="+gamma.getDeltaMovement());
            require(gamma.getHealth()==100&&gamma.getAttribute(Attributes.MAX_HEALTH).getBaseValue()==80&&gamma.getAttributeValue(Attributes.MAX_HEALTH)==108
                    &&ShadowAbsorption.state(gamma).stored==hidden&&ShadowAbsorption.state(gamma).entries.size()==2,"health, transient attributes and both signed absorption layers/hidden HP: health="+gamma.getHealth()+", max="+gamma.getAttributeValue(Attributes.MAX_HEALTH)+", hidden="+ShadowAbsorption.state(gamma).stored);
            require(gamma.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()==.27&&Math.abs(gamma.getAbilities().getWalkingSpeed()-.13f)<1e-6,"native load cannot replace movement attribute with ability walking speed");
            require(gamma.getInventory().getSelectedSlot()==3&&ItemStack.isSameItemSameComponents(gamma.getMainHandItem(),bow)&&gamma.getOffhandItem().is(Items.SHIELD)
                    &&gamma.getInventory().getItem(12).getCount()==17&&gamma.getInventory().getItem(15).isEmpty()&&gamma.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"full inventory, offhand, equipment, slot and large enchantment/custom components");
            require(gamma.getEnderChestInventory().getItem(5).getCount()==23&&gamma.getEnderChestInventory().getItem(8).isEmpty()
                    &&gamma.inventoryMenu.getCraftSlots().getItem(0).getCount()==3&&gamma.inventoryMenu.getCarried().getCount()==7,"ender chest, crafting grid and cursor; future contents absent");
            require(gamma.getEffect(MobEffects.SPEED).getAmplifier()==1&&gamma.getEffect(MobEffects.SPEED).getDuration()==200
                    &&((EffectLayers.Access)gamma.getEffect(MobEffects.SPEED)).skywars2$layerHidden().getAmplifier()==-2,"signed hidden native effect chain restored");
            require(gamma.getEffect(MobEffects.HASTE).getAmplifier()==2&&gamma.getEffect(MobEffects.HASTE).getDuration()==700,"owned effect active layer restored");
            game.items().ownedEffects().withdraw(gamma,"checkpoint:b");game.items().ownedEffects().withdraw(gamma,"checkpoint:a");
            require(gamma.getEffect(MobEffects.HASTE).getAmplifier()==0&&gamma.getEffect(MobEffects.HASTE).getDuration()==1000,"restored leases withdraw independently and retain external baseline");
            require(gamma.entityTags().contains("checkpoint.before")&&!gamma.entityTags().contains("checkpoint.future")&&gamma.getPostEffects().equals(List.of(savedPost)),"tags/post effects replaced instead of appended");
            require(gamma.getFoodData().getFoodLevel()==13&&gamma.getFoodData().getSaturationLevel()==2.5f&&gamma.totalExperience==xp&&gamma.experienceLevel==17&&gamma.experienceProgress==.375f,"food and experience restore without replayed advancement rewards");
            require(gamma.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP))==37&&gamma.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS))==deathCount
                    &&gamma.getStats().getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM))==0
                    &&game.players().get(id).deaths==profileDeaths&&gamma.getAdvancements().getOrStartProgress(advancement).isDone()
                    &&gamma.getAdvancements().getOrStartProgress(rewardProbe).isDone(),"native statistics, core death counters and advancement criterion progress");
            require(((PlayerCheckpointAccess.Advancements)gamma.getAdvancements()).skywars2$tab()==tab
                    &&packets().stream().filter(ClientboundSelectAdvancementsTabPacket.class::isInstance).map(ClientboundSelectAdvancementsTabPacket.class::cast).anyMatch(p->tab.id().equals(p.getTab())),"selected advancement tab republished after client reset");
            require(gamma.getRecipeBook().contains(recipe)&&!gamma.getRecipeBook().contains(futureRecipe)&&gamma.getCooldowns().getCooldownPercent(new ItemStack(Items.ENDER_PEARL),0)==1
                    &&!gamma.getCooldowns().isOnCooldown(new ItemStack(Items.APPLE)),"recipe book and exact cooldown clock replace future values");
            require(gamma.getRemainingFireTicks()==50000&&gamma.getAirSupply()==1777&&gamma.getTicksFrozen()==123&&gamma.isShiftKeyDown()&&gamma.isSprinting()
                    &&gamma.getPose()==Pose.CROUCHING&&gamma.isUsingItem()&&gamma.getUseItem()==gamma.getMainHandItem()&&gamma.getUseItemRemainingTicks()==useRemaining
                    &&integer(gamma,net.minecraft.world.entity.LivingEntity.class,"attackStrengthTicker")==2&&gamma.getInvulnerableTime()==9&&gamma.damageCooldownTime==7,"full-width fire timer, pose, use-item alias and transient attack/damage clocks");
            require(packets().stream().filter(ClientboundSetEntityMotionPacket.class::isInstance).map(ClientboundSetEntityMotionPacket.class::cast).anyMatch(p->p.id()==gamma.getId()&&p.movement().equals(motion))
                    &&packets().stream().anyMatch(ClientboundAwardStatsPacket.class::isInstance)&&packets().stream().anyMatch(ClientboundUpdateAdvancementsPacket.class::isInstance)
                    &&packets().stream().anyMatch(ClientboundSetHealthPacket.class::isInstance),"exact native motion/status/stat/advancement sync packets produced");
            image.restoreFrozen();verifyMovementBeforeInput();ack();require(gamma.getEffect(MobEffects.HASTE).getAmplifier()==2&&gamma.totalExperience==xp,"repeated snapshot remains immutable and reward-free");
        }
        boolean step() {
            require(++ticks<140,"bounded native body fixture phase="+phase);
            if(phase==-1){prepare();return false;}
            if(phase==0){mutate(true);restore();verify();gamma.setDeltaMovement(Vec3.ZERO);gamma.stopUsingItem();gamma.clearFire();game.server().tickRateManager().setFrozen(false);phase=1;return false;}
            if(phase==1){if(game.matchTicks()-clock<4)return false;game.server().tickRateManager().setFrozen(true);mutate(false);restore();verify();
                gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity body fixture")));
                boolean rejected=false;try{image.restoreFrozen();}catch(IllegalStateException expected){rejected=true;}require(rejected,"offline image retained instead of spawning a fake player");
                gamma=GameChecks.createBot(game,"TestGamma");connectWire();restore();verify();return true;}
            return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);
            try {
                var live=game.server().getPlayerList().getPlayer(id);if(owned&&live!=null)live.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity body cleanup")));
                for(var level:game.server().getAllLevels())for(var entity:new ArrayList<>(java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).toList()))
                    if(!before.contains(entity)&&(entity instanceof ItemEntity||entity instanceof ExperienceOrb))entity.discard();
                base.close();originalCore.restoreFrozen();
            }finally{game.server().tickRateManager().setFrozen(frozen);}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-player (native checkpoint-driven respawn/dimension/reconnect, inventory/ender/craft/cursor/components, full attributes and unclamped movement, metadata/use/attack clocks, signed hidden effects/owned leases/shadow HP, recipes/cooldowns/stats/advancements without rewards, repeated real-tick restore and native packets; full coordinator remains gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-player",error);}}
}
