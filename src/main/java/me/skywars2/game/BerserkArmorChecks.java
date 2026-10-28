package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded source payload and real native incoming-criterion checks for Berserk armor. */
public final class BerserkArmorChecks {
    private BerserkArmorChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Berserk armor: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing native fixture field "+name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object replacement){try{field(value,name).set(value,replacement);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    /** This memory listener is advanced by the real registered server connection scheduler. */
    private static final class Wire extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();ClientboundPlayerPositionPacket position;
        Wire(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnecting(){return false;}
        @Override public boolean isConnected(){return true;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);if(packet instanceof ClientboundPlayerPositionPacket p)position=p;}
        @Override public void flushChannel(){}
    }
    private static final class Client implements AutoCloseable {
        final GameController game;final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous,listener;final Wire wire=new Wire();
        Client(GameController game,ServerPlayer player){this.game=game;this.player=player;previous=player.connection;listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),wire,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));set(wire,"packetListener",listener);listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());listener.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());ack();game.server().getConnection().getConnections().add(wire);}
        void ack(){listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(read(listener,"awaitingPositionFromClient")!=null){require(wire.position!=null,"actual teleport packet");listener.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(wire.position.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));}require(listener.hasClientLoaded()&&!player.isChangingDimension()&&read(listener,"awaitingPositionFromClient")==null,"loaded native listener");}
        int updates(){return (Integer)read(listener,"tickCount");}
        public void close(){game.server().getConnection().getConnections().remove(wire);player.connection=previous;previous.resetPosition();}
    }
    /** Capture mutable author state touched by real damage, jumping, equipment and scheduled player ticks. */
    private static final class State {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final boolean ground,sprint,shift;final Vec3 known;final net.minecraft.world.entity.Pose pose;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> values=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        State(GameController game,ServerPlayer p){player=p;require(!p.isUsingItem(),"fixture starts with idle hands");state=new GameChecks.WandThirdPlayerState(p,game.items().mana());ground=p.onGround();sprint=p.isSprinting();shift=p.isShiftKeyDown();known=p.getKnownMovement();pose=p.getPose();
            for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=Stats.CUSTOM.get(id);stats.put(s,p.getStats().getValue(s));}for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.SHIELD,Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET)){var s=type.get(item);stats.put(s,p.getStats().getValue(s));}
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(score!=null)scores.put(objective,score.value());}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var name:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource","lastRecordedFoodLevel","lastSentFood","lastFoodSaturationZero","lastSentHealth","lastRecordedHealthAndAbsorption"))values.put(field(p,name),read(p,name));
            try{for(var f:p.getCombatTracker().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);var v=f.get(p.getCombatTracker());if(v instanceof List<?> list)combat.put(f,new ArrayList<>(list));else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers()))combat.put(f,v);}for(var f:p.getFoodData().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(p.getFoodData()));}for(var f:game.players().get(p.getUUID()).getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(game.players().get(p.getUUID()));profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> s?new HashSet<>(s):v);}for(var entry:((Map<?,?>)read(p,"lastEquipmentItems")).entrySet())equipment.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings({"rawtypes","unchecked"}) void restore(GameController game){state.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(state.health);player.setAbsorptionAmount(state.absorption);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setKnownMovement(known);player.setPose(pose);stats.forEach((s,v)->player.getStats().setValue(player,s,v));for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(player,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(player,o);advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            try{for(var e:values.entrySet())e.getKey().set(player,e.getValue());for(var e:combat.entrySet())if(e.getValue() instanceof List old){var current=(List)e.getKey().get(player.getCombatTracker());current.clear();current.addAll(old);}else e.getKey().set(player.getCombatTracker(),e.getValue());for(var e:food.entrySet())e.getKey().set(player.getFoodData(),e.getValue());for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());var equipmentMap=(Map<EquipmentSlot,ItemStack>)read(player,"lastEquipmentItems");equipmentMap.clear();equipment.forEach((s,i)->equipmentMap.put(s,i.copy()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game; final ServerPlayer owner,beta; final State saved,otherSaved;
        final LbcBerserkArmor service; final Client client,other; final SauvojenMeditationChecks.Attribution attribution;
        final ItemStack[] pieces=new ItemStack[4]; final List<Entity> created=new ArrayList<>();
        Fixture(GameController game){
            this.game=game;service=game.items().armorSets().berserk();
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&game.canUseGameplayItems(owner)&&owner.containerMenu==owner.inventoryMenu&&beta.containerMenu==beta.inventoryMenu,"active closed-inventory participants");
            require(!game.items().timeStop().stopped(owner.level())&&!game.items().actionsBlocked(owner),"no ambient freeze or stun");
            saved=new State(game,owner);otherSaved=new State(game,beta);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta));
            for(var p:List.of(owner,beta)){
                p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);p.setHealth(100);p.setAbsorptionAmount(0);
                for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))p.entityTags().remove(tag);
            }
            owner.teleportTo(owner.level(),owner.getX(),Math.min(240,owner.level().getMaxY()-20),owner.getZ(),Set.of(),0,0,true);
            beta.teleportTo(owner.level(),owner.getX(),owner.getY(),owner.getZ()+3,Set.of(),180,0,true);
            client=new Client(game,owner);other=new Client(game,beta);
        }
        List<ItemStack> roll(String id){require(LootTestCatalog.isSuggested(Identifier.parse("lbc:"+id)),"public source readiness "+id);return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id);}
        void wear(int count){for(int n=0;n<4;n++)owner.setItemSlot(SLOTS[n],n<count?pieces[n].copy():ItemStack.EMPTY);}
        void ready(float hp,float absorption){owner.removeAllEffects();owner.setHealth(hp);owner.setAbsorptionAmount(absorption);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;client.wire.packets.clear();other.wire.packets.clear();}
        int random(float draw,Runnable work){Object original=read(owner,"random");int[] calls={0};
            try{set(owner,"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{if(m.getName().equals("nextFloat")){calls[0]++;return draw;}try{return m.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));work.run();return calls[0];}
            finally{set(owner,"random",original);}
        }
        void proc(boolean expected,String why){require(owner.hasEffect(MobEffects.REGENERATION)==expected,why);if(expected){var e=owner.getEffect(MobEffects.REGENERATION);require(e.getAmplifier()==1&&e.getDuration()==100&&e.isVisible()&&e.showIcon(),"source visible Regeneration II 100ticks");}}
        void instant(){var e=owner.getEffect(MobEffects.INSTANT_HEALTH);require(e!=null&&e.getAmplifier()==0&&e.getDuration()==1,"source native instant health I default duration1");float before=owner.getHealth();require(!e.tickServer(owner.level(),owner,()->{}),"native instant effect expires after one tick");require(owner.getHealth()==Math.min(owner.getMaxHealth(),before+4),"native instant effect heals exactly4HP");owner.removeEffect(MobEffects.INSTANT_HEALTH);}
        void helper(boolean expected,String why){ready(10,0);int calls=random(0,()->service.incoming(owner));proc(expected,why);if(expected){require(calls==1,"one independent source random draw");instant();require(owner.getHealth()==14,"native instant health I adds exactly4HP");}}
        void acquisition(){
            String[][] ids={{"daerisar1mor","daerisattac2k_damage","daerisarmor_to3ughness","daerisattack_spee5d","daerismovement8_speed"},{"geyiii67rmor","cryjcrdgchdfgjage","gkvurcx6tyj65cjhss","gckfygc6chkdgyhbkfg","gkhfcvy7kvknuiki"},{"yiyiyiyir","attack_damage","genericiyic6kilighness","jycfgcdhdfjgjvckdyj","hxdfghntuspeed"},{"genbtxhfbgfrmor","generic.attack_damage","generic.arkhhhess","gfhtxdtjht_speed","generiggggggggggcsped"}};
            var types=List.of(Attributes.ARMOR,Attributes.ATTACK_DAMAGE,Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_SPEED,Attributes.MOVEMENT_SPEED);
            for(int n=0;n<4;n++){
                var out=roll("neko_set_berserk/"+PARTS[n]);require(out.size()==1&&out.getFirst().getCount()==1,"singleton leaf");var s=pieces[n]=out.getFirst();
                require(s.is(List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET).get(n))&&s.getMaxDamage()==400&&s.getDamageValue()==0&&s.getEnchantments().isEmpty(),"source leather durability400/no enchantment");
                var d=LbcFartus.data(s);require(d.getIntOr("berserk",0)==1&&d.getIntOr("transformable",0)==1&&d.getIntOr(PARTS[n],0)==1,"exact raw source flags");
                require(s.get(DataComponents.EQUIPPABLE).slot()==SLOTS[n]&&s.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:berserk"))&&s.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:armors/berserk"+(n==3?"":"_"+PARTS[n])))&&s.get(DataComponents.TOOLTIP_STYLE).equals(Identifier.parse("lbc:tier_b")),"source model/equipment/tooltip");
                var name=s.get(DataComponents.CUSTOM_NAME);require(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals(n==3?"berserker_hat":"berserker_"+PARTS[n])&&!name.getStyle().isItalic()&&name.getStyle().getColor().getValue()==0xff5555,"source red nonitalic translated name");
                var lore=s.get(DataComponents.LORE).lines();require(lore.size()==3,"three source lore rows");var keys=new ArrayList<String>();for(var line:lore)collectTranslations(line,keys);require(keys.equals(List.of("type",PARTS[n],"set_bonus","set_bonus_berserker")),"exact source lore translation order");
                var a=s.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(a.size()==5,"five source signed modifiers");double[] amounts={new double[]{-2,-6,-8,-3}[n],new double[]{3,4,5,3}[n],-4,.15,.05};
                for(int j=0;j<5;j++){var e=a.get(j);require(e.attribute().equals(types.get(j))&&e.modifier().id().equals(Identifier.withDefaultNamespace(ids[n][j]))&&Math.abs(e.modifier().amount()-amounts[j])<1e-8&&e.modifier().operation()==(j<3?net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE:net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE)&&e.slot().test(SLOTS[n]),"exact attribute ID/type/signed amount/op/slot");}
            }
            var full=roll("neko_set_berserk_full");require(full.size()==4,"source full four outputs");for(var p:pieces)require(full.stream().anyMatch(s->ItemStack.matches(s,p)),"full exact leaf");
            var pre=roll("neko_set_berserk_pre");require(pre.size()==5,"source five pre draws");for(var s:pre)require(Arrays.stream(pieces).anyMatch(p->ItemStack.matches(s,p)),"pre exact leaf");
            var seen=new HashSet<Integer>();for(int a=0;a<256&&seen.size()<4;a++){var out=roll("neko_set_berserk");require(out.size()==1,"one root draw");int found=-1;for(int n=0;n<4;n++)if(ItemStack.matches(out.getFirst(),pieces[n]))found=n;require(found>=0,"root exact leaf");seen.add(found);}require(seen.size()==4,"all four source root branches");
        }
        @SuppressWarnings("unchecked") void reforges(){var at=owner.position();
            for(int n=0;n<4;n++){
                require(LbcBolts.reforgeTables(pieces[n],false).equals(List.of("transformables_"+PARTS[n]))&&LbcBolts.reforgeTables(pieces[n],true).equals(List.of("neko_set_berserk")),"original normal slot and Ultra family routes");
                var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Identifier.parse("lbc:transformables_"+PARTS[n])));
                var pool=((List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools")).getFirst();var sequence=(Optional<Identifier>)read(table,"randomSequence");require(sequence.isEmpty(),"ordinary reforge uses native level random");
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);int total=0,offset=-1,weight=0;
                for(int i=0;i<pool.entries.size();i++){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pool.entries.get(i).expand(context,expanded::add);for(var e:expanded){int w=e.getWeight(context.getLuck());if(w<=0)continue;if(i==2){if(offset<0)offset=total;weight+=w;}total+=w;}}require(weight==1&&offset>=0,"original weight1 Berserk slot branch");int bound=total,pick=offset;Object random=read(owner.level(),"random");int[] calls={0};var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual normal reforge donor");created.add(donor);var out=new ArrayList<ItemStack>();
                try{set(owner.level(),"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{if(m.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return m.invoke(random,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));reforge(donor,LbcBolts.ReforgeFamily.NORMAL,out);require(calls[0]==1&&donor.isRemoved()&&out.size()==1&&ItemStack.matches(out.getFirst(),pieces[n]),"actual native normal reforge consumes source donor and retains exact slot leaf");}finally{set(owner.level(),"random",random);donor.discard();}
                donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual Ultra source donor");created.add(donor);out.clear();reforge(donor,LbcBolts.ReforgeFamily.ULTRA,out);require(donor.isRemoved()&&out.size()==1&&Arrays.stream(pieces).anyMatch(p->ItemStack.matches(p,out.getFirst())),"actual native Ultra reforge follows berserk family root");
            }
        }
        void reforge(net.minecraft.world.entity.item.ItemEntity donor,LbcBolts.ReforgeFamily family,List<ItemStack> outputs){try{var method=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);method.setAccessible(true);var work=(LbcStonekivi.Work)method.invoke(game.items().bolts(),donor,family,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"existing native reforge Work completes");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}

        void anyHealthAndEffects(){
            wear(4);for(float hp:new float[]{.25f,19,20,20.001f,21,100,1000})for(float absorbed:new float[]{0,10,100}){ready(hp,absorbed);random(0,()->service.incoming(owner));proc(true,"author permits every health/absorption combination "+hp+"/"+absorbed);instant();}
            ready(10,0);require(random(Math.nextDown(.7f),()->service.incoming(owner))==1,"success samples native random once");proc(true,"draw immediately below .7 succeeds");
            ready(10,0);require(random(.7f,()->service.incoming(owner))==1,"failure samples native random once");proc(false,"draw equal .7 fails");require(owner.getHealth()==10,"failed draw has no heal");
            ready(10,0);owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,231,2));owner.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,200,0));random(0,()->service.incoming(owner));proc(true,"stronger short native layer");var hidden=(MobEffectInstance)read(owner.getEffect(MobEffects.REGENERATION),"hiddenEffect");require(hidden!=null&&hidden.getAmplifier()==0&&hidden.getDuration()==200,"native hidden weaker regeneration retained");require(owner.getEffect(MobEffects.SPEED).getDuration()==231,"unrelated effect preserved");
            ready(10,0);owner.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,220,3));random(0,()->service.incoming(owner));require(owner.getEffect(MobEffects.REGENERATION).getAmplifier()==3&&owner.getEffect(MobEffects.REGENERATION).getDuration()==220,"stronger existing regen preserved");
            ready(10,0);random(0,()->service.incoming(owner));var hearts=client.wire.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle().getType()==ParticleTypes.HEART).toList();require(hearts.size()==1,"one source hearts packet");var h=hearts.getFirst();require(h.count()==3&&Math.abs(h.y()-owner.getY()-1)<1e-6&&h.xDist()==.5f&&h.yDist()==1&&h.zDist()==.5f&&h.xMaxSpeed()==0&&h.yMaxSpeed()==0&&h.zMaxSpeed()==0,"three hearts y+1 spread .5/1/.5 speed0");
            for(var c:List.of(client,other))require(c.wire.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).anyMatch(p->p.getSource()==net.minecraft.sounds.SoundSource.MASTER&&p.getVolume()==1&&p.getPitch()==1&&p.getSound().value().location().equals(Identifier.parse("minecraft:entity.wolf.growl"))),"source wolf growl master1/1 reaches both players");
        }
        void equipment(){for(int n=0;n<4;n++){wear(n);helper(false,"partial set "+n);}wear(0);for(int n=0;n<4;n++)owner.getInventory().setItem(9+n,pieces[n].copy());owner.setItemInHand(InteractionHand.OFF_HAND,pieces[3].copy());helper(false,"carried and offhand pieces excluded");owner.getInventory().clearContent();
            wear(4);owner.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,pieces[3].copy());helper(false,"missing head cannot be supplied by held helmet");owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            wear(4);var changed=pieces[3].copy();var tag=LbcFartus.data(changed).copy();tag.putInt("berserk",2);changed.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));owner.setItemSlot(EquipmentSlot.HEAD,changed);helper(false,"raw berserk2 does not count as berserk1");
            wear(4);owner.setItemSlot(EquipmentSlot.HEAD,pieces[0].copy());helper(true,"source requires raw set flag in each physical slot, independent of leather base or slot flag");
        }
        void nativeCriterion(){
            wear(4);ready(10,0);random(0,()->require(owner.hurtServer(owner.level(),owner.damageSources().generic(),1),"actual accepted native damage"));proc(true,"native accepted criterion");require(owner.getHealth()<10,"actual damage occurred before scheduled native healing");instant();require(owner.getHealth()>10&&owner.getHealth()<14,"native instant4 follows accepted damage");
            ready(10,0);owner.setInvulnerableTime(100);owner.damageCooldownTime=100;random(0,()->require(!owner.hurtServer(owner.level(),owner.damageSources().inFire(),.001f),"actual cooldown rejection"));proc(false,"rejected native damage cannot proc");
            ready(5,10);require(owner.getAbsorptionAmount()==10,"actual absorption capacity");random(0,()->owner.hurtServer(owner.level(),owner.damageSources().generic(),1));proc(true,"native fully absorbed criterion");require(owner.getAbsorptionAmount()<10&&owner.getHealth()==5,"absorbed hit preserves HP before native instant effect tick");instant();require(owner.getHealth()==9,"native absorbed criterion instant healing");
            ready(10,0);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));owner.startUsingItem(InteractionHand.MAIN_HAND);set(owner,"useItemRemaining",(Integer)read(owner,"useItemRemaining")-6);require(owner.isBlocking(),"native shield warmup positive control");random(0,()->owner.hurtServer(owner.level(),owner.damageSources().playerAttack(beta),4));proc(true,"native fully shield-blocked criterion");require(owner.getHealth()==10,"fully shielded hit has no health loss before native instant tick");instant();require(owner.getHealth()==14,"native shield criterion heals exactly4");owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            ready(21,0);random(0,()->owner.hurtServer(owner.level(),owner.damageSources().generic(),4));proc(true,"native actual hit crossing old20HP boundary remains eligible");instant();
        }
        void nativeAttributes(){
            wear(0);equipmentUpdate();var types=List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.MOVEMENT_SPEED);double[] baseline={0,0,1,4,.1},expected={-19,-16,16,6.4,.12};
            for(int n=0;n<types.size();n++){var a=owner.getAttribute(types.get(n));a.removeModifiers();a.setBaseValue(baseline[n]);}
            wear(4);equipmentUpdate();for(int n=0;n<types.size();n++)require(Math.abs(owner.getAttributeValue(types.get(n))-expected[n])<1e-7,"actual native equipped signed attribute "+types.get(n)+"="+owner.getAttributeValue(types.get(n)));
            wear(0);equipmentUpdate();for(int n=0;n<types.size();n++)require(Math.abs(owner.getAttributeValue(types.get(n))-baseline[n])<1e-7,"actual native unequip removes source attribute "+types.get(n));
        }
        void equipmentUpdate(){try{var m=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");m.setAccessible(true);m.invoke(owner);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        void capturedAdmission(){
            wear(4);ready(100,100);boolean result=service.withIncomingAttempt(owner,()->{owner.setHealth(0);try{random(0,()->net.minecraft.advancements.triggers.CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(owner,owner.damageSources().generic(),200,200,false));proc(true,"native fatal-state criterion retains pre-attempt admission");require(owner.getEffect(MobEffects.INSTANT_HEALTH)!=null,"fatal-state criterion still issues source instant effect");return true;}finally{owner.setHealth(100);}});require(result,"native attempt wrapper retains return value");
            ready(100,100);owner.entityTags().add("spectator");try{service.withIncomingAttempt(owner,()->{owner.entityTags().remove("spectator");try{random(0,()->service.incoming(owner));proc(false,"pre-attempt rejected role cannot become admitted during criterion");return true;}finally{owner.entityTags().add("spectator");}});}finally{owner.entityTags().remove("spectator");}
            helper(true,"attempt scope removed after normal return");
            try{service.withIncomingAttempt(owner,()->{throw new IllegalArgumentException("owned Berserk scope probe");});throw new IllegalStateException("expected owned scope probe exception");}catch(IllegalArgumentException expected){require(expected.getMessage().equals("owned Berserk scope probe"),"owned scoped exception");}
            helper(true,"attempt scope removed after exceptional return");
        }
        void policy(){wear(4);
            for(String tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways")){owner.entityTags().add(tag);try{helper(false,"source role/Greed guard "+tag);}finally{owner.entityTags().remove(tag);}}
            owner.entityTags().add("sin_greed_active");try{ready(10,0);random(0,()->service.incoming(owner));proc(true,"own-source grants preserve shared Greed admission");float before=owner.getHealth();var instant=owner.getEffect(MobEffects.INSTANT_HEALTH);require(instant!=null&&!instant.tickServer(owner.level(),owner,()->{}),"native instant duration still expires under Greed");require(owner.getHealth()==before,"shared signed instant formula blocks tick benefit under Greed");owner.removeEffect(MobEffects.INSTANT_HEALTH);ready(10,0);random(0,()->require(!owner.hurtServer(owner.level(),owner.damageSources().playerAttack(beta),1),"Greed rejects actual hostile damage"));proc(false,"Greed-rejected damage has no incoming criterion");}finally{owner.entityTags().remove("sin_greed_active");}
            game.items().timeStop().begin(owner.level(),40);try{helper(false,"actual dimension Time Stop");}finally{game.items().timeStop().end(owner.level());}
            var old=game.stage();try{set(game,"stage",GameController.Stage.ENDING);helper(false,"ENDING guard");}finally{set(game,"stage",old);}
            for(String tag:List.of("stopper_magic","stopper")){var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add(tag);require(owner.level().addFreshEntity(marker),"actual jammer marker");created.add(marker);game.items().magic().loaded(marker);
                try{require(tag.equals("stopper_magic")?game.items().magic().beaconSuppressed(owner.position()):game.items().magic().mechanismSuppressed(owner),"jammer positive control "+tag);helper(tag.equals("stopper"),"new proc jammer policy "+tag);ready(10,0);owner.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,31,1));owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,200,2));random(0,()->service.incoming(owner));require(owner.getEffect(MobEffects.REGENERATION).getDuration()==(tag.equals("stopper")?100:31),"jammer preserves already granted regeneration or permits normal TECH refresh");require(owner.getEffect(MobEffects.SPEED).getDuration()==200,"jammer does not clear unrelated existing effects");}
                finally{game.items().magic().unloaded(marker);marker.discard();}
            }
        }
        void criterion(){var advancement=Objects.requireNonNull(game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/armor/berserker")));for(int n:new int[]{3,4}){advancement.value().criteria().keySet().forEach(key->owner.getAdvancements().revoke(advancement,key));wear(n);net.minecraft.advancements.triggers.CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(owner,beta,owner.damageSources().playerAttack(owner));require(owner.getAdvancements().getOrStartProgress(advancement).isDone()==(n==4),"native original Berserker PvP criterion");}}
        @Override public void close(){owner.stopUsingItem();beta.stopUsingItem();created.forEach(Entity::discard);client.close();other.close();saved.restore(game);otherSaved.restore(game);attribution.close();owner.connection.resetPosition();beta.connection.resetPosition();}
    }
    private static void collectTranslations(net.minecraft.network.chat.Component c,List<String> keys){if(c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t)keys.add(t.getKey());c.getSiblings().forEach(s->collectTranslations(s,keys));}
    public static void run(GameController game){try(var f=new Fixture(game)){f.acquisition();f.reforges();f.nativeAttributes();f.equipment();f.anyHealthAndEffects();f.nativeCriterion();f.capturedAdmission();f.policy();f.criterion();}SkyWars2.LOGGER.info("[check] PASS berserk-armor (seven source tables, original payload/signed modifiers, native normal/Ultra reforge, four physical slots,70percent boundary, native effect layers and source heart/growl packets, accepted/rejected/absorbed/shield criteria, any health/absorption, role/Greed/Time Stop and MAG/TECH policy)");}
}
