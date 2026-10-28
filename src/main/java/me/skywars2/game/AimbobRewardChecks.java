package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual native source reward components, passive helmet and apple consumption. */
public final class AimbobRewardChecks {
    private AimbobRewardChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Aimbob rewards: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<.00001,why+" actual="+actual+" expected="+expected);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing native fixture field "+name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object replacement){try{field(value,name).set(value,replacement);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    /** Capture mutable author state touched by real damage, jumping, equipment and scheduled player ticks. */
    private static final class State {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final boolean ground,sprint,shift;final Vec3 known;final net.minecraft.world.entity.Pose pose;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> values=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        State(GameController game,ServerPlayer p){player=p;require(!p.isUsingItem(),"fixture starts with idle hands");state=new GameChecks.WandThirdPlayerState(p,game.items().mana());ground=p.onGround();sprint=p.isSprinting();shift=p.isShiftKeyDown();known=p.getKnownMovement();pose=p.getPose();
            for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=Stats.CUSTOM.get(id);stats.put(s,p.getStats().getValue(s));}for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.GOLDEN_APPLE,Items.APPLE,Items.IRON_HELMET,Items.SHIELD,Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET)){var s=type.get(item);stats.put(s,p.getStats().getValue(s));}
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(score!=null)scores.put(objective,score.value());}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var name:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource","lastRecordedFoodLevel","lastSentFood","lastFoodSaturationZero","lastSentHealth","lastRecordedHealthAndAbsorption"))values.put(field(p,name),read(p,name));
            try{for(var f:p.getCombatTracker().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);var v=f.get(p.getCombatTracker());if(v instanceof List<?> list)combat.put(f,new ArrayList<>(list));else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers()))combat.put(f,v);}for(var f:p.getFoodData().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(p.getFoodData()));}for(var f:game.players().get(p.getUUID()).getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(game.players().get(p.getUUID()));profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> s?new HashSet<>(s):v);}for(var entry:((Map<?,?>)read(p,"lastEquipmentItems")).entrySet())equipment.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings({"rawtypes","unchecked"}) void restore(GameController game){state.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(state.health);player.setAbsorptionAmount(state.absorption);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setKnownMovement(known);player.setPose(pose);stats.forEach((s,v)->player.getStats().setValue(player,s,v));for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(player,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(player,o);advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            try{for(var e:values.entrySet())e.getKey().set(player,e.getValue());for(var e:combat.entrySet())if(e.getValue() instanceof List old){var current=(List)e.getKey().get(player.getCombatTracker());current.clear();current.addAll(old);}else e.getKey().set(player.getCombatTracker(),e.getValue());for(var e:food.entrySet())e.getKey().set(player.getFoodData(),e.getValue());for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());var equipmentMap=(Map<EquipmentSlot,ItemStack>)read(player,"lastEquipmentItems");equipmentMap.clear();equipment.forEach((s,i)->equipmentMap.put(s,i.copy()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
    }
    private static final String[] SOURCE_LEAVES={
        "{\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"weight\":1,\"name\":\"minecraft:iron_helmet\",\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":1},{\"type\":\"minecraft:set_components\",\"components\":{\"minecraft:unbreakable\":{},\"minecraft:tooltip_style\":\"lbc:tier_b\",\"minecraft:enchantments\":{\"minecraft:protection\":4,\"minecraft:fire_protection\":4,\"minecraft:blast_protection\":4,\"minecraft:projectile_protection\":4,\"minecraft:respiration\":3,\"minecraft:aqua_affinity\":1,\"minecraft:thorns\":3}}},{\"type\":\"minecraft:set_name\",\"name\":[{\"translate\":\"aimbob_helmet\",\"italic\":false}]},{\"type\":\"minecraft:set_custom_data\",\"tag\":\"{night_vision:1}\"}]}]}]}",
        "{\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"weight\":1,\"name\":\"minecraft:golden_apple\",\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":2},{\"type\":\"minecraft:set_components\",\"components\":{\"minecraft:item_model\":\"lbc:aimbobapple\",\"minecraft:tooltip_style\":\"lbc:tier_a\"}},{\"type\":\"minecraft:set_name\",\"name\":[{\"translate\":\"aimbob_apple\",\"italic\":false}]},{\"type\":\"minecraft:set_custom_data\",\"tag\":\"{aimbobapple:1}\"}]}]}]}",
    };
    private static void effect(ServerPlayer p,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int ticks,int amplifier){var e=p.getEffect(type);require(e!=null&&e.getDuration()==ticks&&e.getAmplifier()==amplifier,"native source effect "+type+" ticks="+ticks+" amplifier="+amplifier);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner;final State saved;final LbcAimbobRewards service;final Map<ServerPlayer,Integer> phases;final Object epoch;
        ItemStack helmet,apple;final List<net.minecraft.world.entity.Marker> markers=new ArrayList<>();
        final net.minecraft.server.network.ServerGamePacketListenerImpl connection;
        final ElementalGrimoireChecks.Recording recording;

        /** Complete the real teleport handshake; resetPosition alone leaves dimension-change immunity active. */
        void acknowledge(){
            owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            if(read(owner.connection,"awaitingPositionFromClient")!=null){
                ClientboundPlayerPositionPacket position=null;
                for(var packet:recording.packets)if(packet instanceof ClientboundPlayerPositionPacket update)position=update;
                require(position!=null,"native dimension transition emitted a position packet");
                owner.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(position.id(),
                        owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot()));
            }
            require(owner.connection.hasClientLoaded()&&!owner.isChangingDimension()
                    &&read(owner.connection,"awaitingPositionFromClient")==null,"native dimension/load acknowledgement completed");
        }
        @SuppressWarnings("unchecked") Fixture(GameController game){this.game=game;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));service=game.items().aimbobRewards();require(game.playing()&&game.canUseGameplayItems(owner)&&owner.containerMenu==owner.inventoryMenu,"active native participant with closed container");require(!game.items().timeStop().stopped(owner.level()),"fixture starts without native TimeStop");saved=new State(game,owner);
            connection=owner.connection;recording=new ElementalGrimoireChecks.Recording(owner);
            owner.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),recording,owner,
                    new net.minecraft.server.network.CommonListenerCookie(owner.getGameProfile(),0,owner.clientInformation(),false));
            owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            phases=new IdentityHashMap<>((Map<ServerPlayer,Integer>)read(service,"phases"));epoch=read(service,"epoch");owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);owner.getAbilities().invulnerable=false;owner.getInventory().clearContent();owner.removeAllEffects();game.players().get(owner.getUUID()).lbcSkill=0;}
        net.minecraft.world.level.storage.loot.LootParams params(){return new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,owner.position()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);}
        @SuppressWarnings("unchecked") void payloads(){var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:aimbob")));var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools");require(pools.size()==2&&pools.get(1).entries.size()==20,"original Aimbob reward table has its native20-entry gear pool");var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            for(int n=0;n<2;n++){var oracle=net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC.parse(ops,com.google.gson.JsonParser.parseString(SOURCE_LEAVES[n])).getOrThrow();var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params()).withOptionalRandomSeed(17).create(Optional.empty());var entries=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pools.get(1).entries.get(n==0?4:16).expand(context,entries::add);require(entries.size()==1,"actual original source leaf expands once");var actual=new ArrayList<ItemStack>();entries.getFirst().createItemStack(actual::add,context);var expected=oracle.getRandomItems(params(),17);require(actual.size()==1&&expected.size()==1&&ItemStack.matches(actual.getFirst(),expected.getFirst()),"independent frozen original native source components "+n);if(n==0)helmet=actual.getFirst();else apple=actual.getFirst();}
            require(helmet.is(Items.IRON_HELMET)&&LbcFartus.data(helmet).getIntOr("night_vision",0)==1&&helmet.has(DataComponents.UNBREAKABLE),"actual source unbreakable reward helmet raw flag");require(apple.is(Items.GOLDEN_APPLE)&&apple.getCount()==2&&LbcFartus.data(apple).getIntOr("aimbobapple",0)==1&&Identifier.parse("lbc:aimbobapple").equals(apple.get(DataComponents.ITEM_MODEL)),"actual two original golden apple rewards retain source model and flag");}
        void clear(){owner.stopUsingItem();owner.getInventory().clearContent();owner.removeAllEffects();service.forget(owner);}
        void pulse(int ticks){for(int i=0;i<ticks;i++)service.tickPlayer(owner);}
        void debuffs(){owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.BLINDNESS,200,0));owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.DARKNESS,200,0));owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.NAUSEA,200,0));owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.SPEED,137,2));}
        void cleansed(){effect(owner,net.minecraft.world.effect.MobEffects.NIGHT_VISION,300,0);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)&&!owner.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)&&!owner.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA),"source helmet clears exactly three debuffs");effect(owner,net.minecraft.world.effect.MobEffects.SPEED,137,2);}
        void helmet(){clear();owner.setItemSlot(EquipmentSlot.HEAD,helmet.copy());debuffs();pulse(19);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)&&owner.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"helmet waits19 actual service player ticks");pulse(1);cleansed();owner.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);pulse(19);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"helmet second pulse keeps source20 cadence");pulse(1);effect(owner,net.minecraft.world.effect.MobEffects.NIGHT_VISION,300,0);
            clear();owner.getInventory().setItem(9,helmet.copy());owner.setItemInHand(InteractionHand.OFF_HAND,helmet.copy());pulse(20);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"carried and offhand helmet do not activate equipped-head source");clear();owner.setItemSlot(EquipmentSlot.HEAD,helmet.copy());pulse(20);owner.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.BLINDNESS,111,0));pulse(20);effect(owner,net.minecraft.world.effect.MobEffects.NIGHT_VISION,300,0);effect(owner,net.minecraft.world.effect.MobEffects.BLINDNESS,111,0);}
        ItemStack eat(ItemStack source){owner.getFoodData().setFoodLevel(1);var held=source.copy();owner.setItemInHand(InteractionHand.MAIN_HAND,held);int before=held.getCount();var output=held.finishUsingItem(owner.level(),owner);require(output.getCount()==before-1,"actual native consumption debits exactly one");return output;}
        void apple(){
            clear();eat(apple);
            effect(owner,net.minecraft.world.effect.MobEffects.HEALTH_BOOST,6000,9);
            effect(owner,net.minecraft.world.effect.MobEffects.REGENERATION,60,5);
            effect(owner,net.minecraft.world.effect.MobEffects.RESISTANCE,100,4);
            effect(owner,net.minecraft.world.effect.MobEffects.SATURATION,1,127);
            effect(owner,net.minecraft.world.effect.MobEffects.ABSORPTION,2400,0);
            owner.removeAllEffects();eat(apple);
            effect(owner,net.minecraft.world.effect.MobEffects.HEALTH_BOOST,6000,9);
            require(game.server().getAdvancements().get(Identifier.parse("lbc:aimbobapple"))==null,
                    "native repeatable consumption uses no hidden legacy function advancement");
            clear();eat(new ItemStack(Items.GOLDEN_APPLE,2));
            require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST)&&!owner.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE),
                    "unflagged native golden apple keeps its ordinary payload");
        }
        net.minecraft.world.entity.Marker marker(String tag){var m=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());m.setPos(owner.position());m.entityTags().add(tag);require(owner.level().addFreshEntity(m),"native jammer marker");markers.add(m);game.items().magic().loaded(m);return m;}
        void admission(){for(String tag:List.of("stopper_magic","stopper")){clear();var m=marker(tag);try{owner.setItemSlot(EquipmentSlot.HEAD,helmet.copy());debuffs();pulse(20);if(tag.equals("stopper")){require(game.items().magic().mechanismSuppressed(owner),"native TECH positive control");require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)&&owner.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)&&owner.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)&&owner.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA),"TECH blocks new helmet effects and cleansing");owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.NIGHT_VISION,111,0));pulse(20);effect(owner,net.minecraft.world.effect.MobEffects.NIGHT_VISION,111,0);}else cleansed();owner.removeAllEffects();eat(apple);effect(owner,net.minecraft.world.effect.MobEffects.HEALTH_BOOST,6000,9);effect(owner,net.minecraft.world.effect.MobEffects.REGENERATION,60,5);}finally{game.items().magic().unloaded(m);m.discard();markers.remove(m);}}
            for(String tag:List.of("spectator","strict_map_object","magic_academy","rcdenyalways","time_stopped")){clear();owner.setItemSlot(EquipmentSlot.HEAD,helmet.copy());owner.entityTags().add(tag);try{pulse(20);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"helmet shared protected-role admission "+tag);eat(apple);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST),"apple shared protected-role admission "+tag);}finally{owner.entityTags().remove(tag);}}
            clear();owner.setItemSlot(EquipmentSlot.HEAD,helmet.copy());pulse(19);var phase=read(service,"phases");var before=new IdentityHashMap<>((Map<?,?>)phase);game.items().timeStop().begin(owner.level(),40);try{pulse(20);require(before.equals(read(service,"phases"))&&!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"native dimension TimeStop pauses helmet clock");eat(apple);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST),"native TimeStop prevents new apple bonus");}finally{game.items().timeStop().end(owner.level());}pulse(1);effect(owner,net.minecraft.world.effect.MobEffects.NIGHT_VISION,300,0);
            var main=owner.level();for(String name:List.of("nexus","imprinted")){var level=game.server().getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION,Identifier.parse("minecraft:"+name)));require(level!=null,"original sealed source dimension exists "+name);clear();owner.setItemSlot(EquipmentSlot.HEAD,helmet.copy());try{require(owner.teleportTo(level,0,100,0,Set.of(),0,0,true),"native test dimension transition");acknowledge();require(!LbcDimensionItems.ordinary(owner),"shared dimension helper rejects original sealed realm");pulse(20);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"helmet rejects sealed dimension "+name);service.consumed(owner,apple);require(!owner.hasEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST),"already completed consume callback rejects sealed dimension "+name);}finally{owner.teleportTo(main,saved.state.position.x,saved.state.position.y,saved.state.position.z,Set.of(),saved.state.yaw,saved.state.pitch,true);acknowledge();}}}
        @SuppressWarnings("unchecked") public void close(){try{for(var m:markers){game.items().magic().unloaded(m);m.discard();}owner.stopUsingItem();saved.restore(game);acknowledge();}finally{var current=(Map<ServerPlayer,Integer>)read(service,"phases");current.clear();current.putAll(phases);set(service,"epoch",epoch);owner.connection=connection;connection.resetPosition();}}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.payloads();f.helmet();f.apple();f.admission();}SkyWars2.LOGGER.info("[check] PASS aimbob-rewards (independent source reward components, native consumption, helmet cadence/cleansing and shared admission)");}
}
