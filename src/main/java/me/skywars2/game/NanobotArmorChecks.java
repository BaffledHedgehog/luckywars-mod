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

/** Complete source family, signed scoreboard mechanics, native hurt criteria and the real twenty-tick clock. */
public final class NanobotArmorChecks {
    private NanobotArmorChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Nanobots armor: "+why);}
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
        final GameController game; final LbcArmorSets armor; final LbcNanobotArmor service;
        final ElementalGrimoireChecks.Fixture base; final ServerPlayer owner,beta; final State saved,betaSaved;
        final ItemStack[] pieces=new ItemStack[4]; final List<Entity> created=new ArrayList<>(); final Vec3 at;
        final Object oldPhase,oldEpoch; final SauvojenMeditationChecks.Attribution attribution;
        Client client,other; boolean closed; int start,stage=-1,updates;
        Fixture(GameController game){this.game=game;armor=game.items().armorSets();service=armor.nanobots();
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            saved=new State(game,owner);betaSaved=new State(game,beta);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta));oldPhase=read(armor,"phase");oldEpoch=read(armor,"epoch");
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();at=owner.position();
            for(var p:List.of(owner,beta)){p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);p.setHealth(500);p.setAbsorptionAmount(0);for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))p.entityTags().remove(tag);}
            beta.teleportTo(owner.level(),at.x,at.y,at.z+3,Set.of(),180,0,true);client=new Client(game,owner);other=new Client(game,beta);
        }
        List<ItemStack> roll(String id){require(LootTestCatalog.isSuggested(Identifier.parse("lbc:"+id)),"public readiness "+id);return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id);}
        void wear(int n){for(int i=0;i<4;i++)owner.setItemSlot(SLOTS[i],i<n?pieces[i].copy():ItemStack.EMPTY);owner.removeAllEffects();}
        void scores(int tier,int cooldown){game.items().mana().score(owner,"nanobots_tier").set(tier);game.items().mana().score(owner,"nanobots_coldown").set(cooldown);}
        void values(int tier,int cooldown,String why){require(service.tier(owner)==tier&&service.cooldown(owner)==cooldown,why+" got "+service.tier(owner)+"/"+service.cooldown(owner));}
        void effect(int amp){var e=owner.getEffect(MobEffects.RESISTANCE);require(e!=null&&e.getAmplifier()==amp&&e.getDuration()==40&&!e.isVisible()&&!e.showIcon(),"hidden Resistance amp"+amp+" duration40");}
        List<ClientboundLevelParticlesPacket> dust(){return client.wire.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle().getType()==ParticleTypes.DUST).toList();}
        void acquisition(){
            for(int n=0;n<4;n++){var out=roll("neko_set_nanobots/"+PARTS[n]);require(out.size()==1&&out.getFirst().getCount()==1,"original singleton "+PARTS[n]);var s=pieces[n]=out.getFirst();require(LbcFartus.data(s).getIntOr("nanobots",0)==1&&LbcFartus.data(s).getIntOr("transformable",0)==1,"authored flags");require(s.get(DataComponents.EQUIPPABLE).slot()==SLOTS[n]&&s.has(DataComponents.CUSTOM_NAME)&&s.has(DataComponents.ITEM_MODEL)&&s.has(DataComponents.LORE)&&s.has(DataComponents.TOOLTIP_STYLE),"authored wearable presentation");}
            var full=roll("neko_set_nanobots_full");require(full.size()==4,"four original full outputs");for(var leaf:pieces)require(full.stream().anyMatch(s->ItemStack.matches(s,leaf)),"exact full leaf components");
            var pre=roll("neko_set_nanobots_pre");require(pre.size()==5,"source pre five outputs");for(var out:pre)require(Arrays.stream(pieces).anyMatch(leaf->ItemStack.matches(out,leaf)),"all five pre draws retain exact authored leaf components; replacement allowed");
            var seen=new HashSet<Integer>();for(int attempt=0;attempt<256&&seen.size()<4;attempt++){var out=roll("neko_set_nanobots");require(out.size()==1,"one original weighted root output");int index=-1;for(int n=0;n<4;n++)if(ItemStack.matches(out.getFirst(),pieces[n]))index=n;require(index>=0,"root exact original leaf");seen.add(index);}require(seen.size()==4,"all original root branches");
        }
        @SuppressWarnings("unchecked") void reforges(){
            for(int n=0;n<4;n++){
                require(LbcBolts.reforgeTables(pieces[n],false).equals(List.of("transformables_"+PARTS[n]))&&LbcBolts.reforgeTables(pieces[n],true).equals(List.of("neko_set_nanobots")),"original normal slot and Ultra family routes");
                var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Identifier.parse("lbc:transformables_"+PARTS[n])));
                var pool=((List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools")).getFirst();var sequence=(Optional<Identifier>)read(table,"randomSequence");require(sequence.isEmpty(),"ordinary reforge uses native level random");
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);int total=0,offset=-1,weight=0;
                for(int i=0;i<pool.entries.size();i++){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pool.entries.get(i).expand(context,expanded::add);for(var e:expanded){int w=e.getWeight(context.getLuck());if(w<=0)continue;if(i==15){if(offset<0)offset=total;weight+=w;}total+=w;}}require(weight==1&&offset>=0,"original weight1 Nanobots slot branch");int bound=total,pick=offset;Object random=read(owner.level(),"random");int[] calls={0};var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual normal reforge donor");created.add(donor);var out=new ArrayList<ItemStack>();
                try{set(owner.level(),"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{if(m.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return m.invoke(random,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));reforge(donor,LbcBolts.ReforgeFamily.NORMAL,out);require(calls[0]==1&&donor.isRemoved()&&out.size()==1&&ItemStack.matches(out.getFirst(),pieces[n]),"actual native normal reforge consumes source donor and retains exact slot leaf");}finally{set(owner.level(),"random",random);donor.discard();}
                donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual Ultra source donor");created.add(donor);out.clear();reforge(donor,LbcBolts.ReforgeFamily.ULTRA,out);require(donor.isRemoved()&&out.size()==1&&Arrays.stream(pieces).anyMatch(p->ItemStack.matches(p,out.getFirst())),"actual native Ultra reforge follows nanobots family root");
            }
        }
        void reforge(net.minecraft.world.entity.item.ItemEntity donor,LbcBolts.ReforgeFamily family,List<ItemStack> outputs){try{var method=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);method.setAccessible(true);var work=(LbcStonekivi.Work)method.invoke(game.items().bolts(),donor,family,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"existing native reforge Work completes");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}

        void exactLeaves(){
            for(int i=0;i<4;i++){var v=pieces[i];require(v.is(List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET).get(i))&&v.getMaxDamage()==7000&&v.getEnchantments().isEmpty(),"source leather/no enchantment/durability7000 "+PARTS[i]);require(LbcFartus.data(v).getIntOr(PARTS[i],0)==1&&Identifier.parse("lbc:armors/nanobots_"+PARTS[i]).equals(v.get(DataComponents.ITEM_MODEL))&&v.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:nanobot")),"exact source slot flag/model/equipment asset");var name=v.get(DataComponents.CUSTOM_NAME);require(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("nanobots_"+PARTS[i])&&name.getStyle().getColor().getValue()==0x55ffff&&!name.getStyle().isItalic(),"exact authored aqua translatable name");}
        }
        void arithmetic(){
            for(int i=0;i<4;i++){wear(0);owner.setItemSlot(SLOTS[i],pieces[i].copy());scores(0,0);service.pulse(owner);values(new int[]{1,3,4,2}[i],0,"individual physical-slot gain");}
            wear(4);scores(0,0);service.pulse(owner);values(10,-4,"full set signed cooldown");service.pulse(owner);values(20,-8,"negative cooldown persists");
            for(int tier:new int[]{118,119,120,121}){scores(tier,0);service.pulse(owner);values(tier<120?tier+2:tier,-4,"sequential head first overshoot "+tier);}
            scores(0,1);service.pulse(owner);values(0,-3,"full recovery happens after gain gate");service.pulse(owner);values(10,-7,"following pulse sees signed recovery");
            wear(0);for(int n=0;n<4;n++)owner.getInventory().setItem(9+n,pieces[n].copy());owner.setItemInHand(InteractionHand.OFF_HAND,pieces[3].copy());scores(0,1);service.pulse(owner);values(0,0,"carried/offhand set cannot charge or accelerate");owner.getInventory().clearContent();
            owner.setItemSlot(EquipmentSlot.HEAD,pieces[0].copy());scores(0,0);service.pulse(owner);values(2,0,"source flag admission follows actual head slot even wrong leather base");
            wear(0);scores(-5,-8);service.pulse(owner);values(-5,-8,"negative scores preserved");
            scores(Integer.MAX_VALUE,Integer.MAX_VALUE);service.pulse(owner);values(Integer.MAX_VALUE,Integer.MAX_VALUE-1,"external INT_MAX not saturated or reset");
            scores(5,0);service.pulse(owner);game.items().mana().score(owner,"nanobots_tier").set(23);game.items().mana().score(owner,"nanobots_coldown").set(7);service.pulse(owner);values(23,6,"cached service observes external native scoreboard edits");
            scores(9,20);var ledger=LbcKfcFood.sourceScoreAdditions();require(ledger.contains("nanobots_tier")&&ledger.contains("nanobots_coldown"),"existing KFC source ledger includes both authoritative counters");game.items().mana().score(owner,"nanobots_tier").add(1);game.items().mana().score(owner,"nanobots_coldown").add(1);values(10,21,"KFC shared scoreboard addition path");
            service.forget(owner);values(10,21,"forget retains authoritative scores");service.reset(owner);values(0,0,"reset zeros both objectives");
        }
        void bands(){
            wear(0);int[] edges={-1,0,1,3,4,11,12,23,24,39,40,59,60,120,Integer.MAX_VALUE};
            for(int value:edges){owner.removeAllEffects();client.wire.packets.clear();scores(value,0);service.pulse(owner);int amp=value<4?-1:value<12?0:value<24?1:value<40?2:value<60?3:4;require(owner.hasEffect(MobEffects.RESISTANCE)==(amp>=0),"threshold edge "+value);if(amp>=0){effect(amp);var d=dust();require(d.size()==1,"one source dust packet at edge "+value);var q=d.getFirst();require(q.count()==5&&!q.overrideLimiter()&&!q.alwaysShow()&&Math.abs(q.x()-owner.getX())<1e-6&&Math.abs(q.y()-owner.getY()-1)<1e-6&&Math.abs(q.xDist()-.3)<1e-6&&Math.abs(q.yDist()-.6)<1e-6&&Math.abs(q.zDist()-.3)<1e-6&&q.xMaxSpeed()==0&&q.yMaxSpeed()==0&&q.zMaxSpeed()==0,"source5 dust normal/spread/origin/speed");}else require(dust().isEmpty(),"below threshold no dust");}
            wear(4);scores(3,0);service.pulse(owner);values(13,-4,"gain crosses band after resistance phase");require(!owner.hasEffect(MobEffects.RESISTANCE),"no early resistance from newly grown tier");service.pulse(owner);effect(1);
            wear(0);owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,200,4));scores(4,0);service.pulse(owner);var e=owner.getEffect(MobEffects.RESISTANCE);require(e.getAmplifier()==4&&e.getDuration()==200,"weaker refresh retains stronger long native layer");
            owner.removeAllEffects();owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,200,0));scores(60,0);service.pulse(owner);effect(4);var hidden=(MobEffectInstance)read(owner.getEffect(MobEffects.RESISTANCE),"hiddenEffect");require(hidden!=null&&hidden.getAmplifier()==0&&hidden.getDuration()==200,"native stronger short grant preserves weaker long hidden layer");
        }
        void incomingMath(){wear(0);for(int tier:new int[]{0,1,4,5,6,60})for(int cd:new int[]{-8,0,20,21,25}){owner.removeAllEffects();scores(tier,cd);service.incoming(owner);values(tier==0?0:Math.max(0,tier-5),tier==0?cd:cd<=20?cd+5:cd,"damage tier/cooldown boundary "+tier+"/"+cd);}scores(17,0);owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.hurtServer(owner.level(),owner.damageSources().generic(),1);values(12,5,"real native environmental hurt criterion once");owner.removeAllEffects();scores(17,0);owner.setInvulnerableTime(100);owner.damageCooldownTime=100;owner.hurtServer(owner.level(),owner.damageSources().generic(),.1f);values(17,0,"rejected native cooldown attempt has no criterion");owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setAbsorptionAmount(10);require(owner.getAbsorptionAmount()==10,"native absorption positive control before the single incoming hit");scores(17,0);float hp=owner.getHealth();owner.hurtServer(owner.level(),owner.damageSources().generic(),1);values(12,5,"native absorbed hurt criterion once");require(owner.getHealth()==hp&&owner.getAbsorptionAmount()<10,"actual absorption consumed without health loss");owner.setAbsorptionAmount(0);}
        void guards(){wear(4);for(String tag:List.of("time_stopped","spectator","strict_map_object")){scores(60,10);owner.entityTags().add(tag);try{service.pulse(owner);service.incoming(owner);values(60,10,"role guard "+tag);require(!owner.hasEffect(MobEffects.RESISTANCE),"guard blocks resistance "+tag);}finally{owner.entityTags().remove(tag);}}
            scores(60,10);game.items().timeStop().begin(owner.level(),40);try{service.pulse(owner);service.incoming(owner);values(60,10,"actual dimension Time Stop blocks source operations");}finally{game.items().timeStop().end(owner.level());}
            var old=game.stage();try{set(game,"stage",GameController.Stage.ENDING);service.pulse(owner);service.incoming(owner);values(60,10,"ENDING guard");}finally{set(game,"stage",old);}
            var level=owner.level();var pos=owner.position();var elsewhere=java.util.stream.StreamSupport.stream(game.server().getAllLevels().spliterator(),false).filter(l->l!=level).findFirst().orElseThrow();try{owner.teleportTo(elsewhere,pos.x,pos.y,pos.z,Set.of(),0,0,true);service.pulse(owner);service.incoming(owner);values(60,10,"foreign dimension guard");}finally{owner.teleportTo(level,pos.x,pos.y,pos.z,Set.of(),0,0,true);client.ack();}
        }
        void policy(){
            wear(4);scores(60,0);owner.entityTags().add("nomagic_active");game.items().mana().noMagic(owner,100);
            try{service.pulse(owner);values(70,-4,"personal no-magic permits biological passive");effect(4);}finally{owner.entityTags().remove("nomagic_active");game.items().mana().noMagic(owner,0);}
            wear(4);scores(60,0);require(game.items().shooters().stun(beta,owner,10),"real stun positive control");
            try{require(game.items().actionsBlocked(owner),"stun blocks active actions");service.pulse(owner);values(70,-4,"stun permits biological passive");effect(4);}finally{game.items().shooters().clearStun(owner);}
            wear(4);var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add("stopper");require(owner.level().addFreshEntity(marker),"real TECH jammer marker");game.items().magic().loaded(marker);
            try{require(game.items().magic().mechanismSuppressed(owner),"native TECH positive control");scores(60,10);service.pulse(owner);values(60,9,"TECH blocks gain/full recovery but natural cooldown works");require(!owner.hasEffect(MobEffects.RESISTANCE),"TECH blocks new resistance");scores(60,0);service.pulse(owner);values(60,0,"TECH also blocks accumulation when ready");owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,31,2,false,false));service.pulse(owner);require(owner.getEffect(MobEffects.RESISTANCE).getDuration()==31,"TECH leaves existing resistance to expire");scores(60,20);service.incoming(owner);values(60,20,"TECH temporarily disables charge use without erasing accumulated tier");require(owner.getEffect(MobEffects.RESISTANCE).getDuration()==31,"TECH damage helper cannot refresh resistance");}
            finally{game.items().magic().unloaded(marker);marker.discard();}
            marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add("stopper_magic");require(owner.level().addFreshEntity(marker),"real MAG marker");game.items().magic().loaded(marker);
            try{require(game.items().magic().beaconSuppressed(owner.position()),"MAG positive control");owner.removeAllEffects();scores(60,0);service.pulse(owner);values(70,-4,"MAG allows gains/full recovery");effect(4);}
            finally{game.items().magic().unloaded(marker);marker.discard();}
            wear(0);scores(60,2);service.pulse(owner);values(60,1,"unequip retains stored tier and natural cooldown");effect(4);
        }
        void hud(){wear(0);for(int low:new int[]{-1,0,1,3}){scores(low,0);require(!game.items().hud(owner).getString().contains("\uE114"),"below4 no Nanobots HUD");}for(int value:new int[]{4,11,12,23,24,39,40,59,60,120}){scores(value,0);var h=game.items().hud(owner);require(h.getString().contains("\uE114"),"shared HUD includes source glyph");var styles=new ArrayList<net.minecraft.network.chat.Style>();h.visit((style,text)->{if(text.equals(Integer.toString(value)))styles.add(style);return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);require(styles.stream().anyMatch(st->st.getColor()!=null&&st.getColor().getValue()==(value<12?0xff5555:value<24?0xffaa00:value<40?0xffff55:value<60?0x55ff55:0x55ffff)),"source raw tier color "+value);}}
        void criterion(){
            var advancement=Objects.requireNonNull(game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/armor/nano")));
            for(int count:new int[]{3,4}){
                advancement.value().criteria().keySet().forEach(key->owner.getAdvancements().revoke(advancement,key));
                wear(count);
                net.minecraft.advancements.triggers.CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(owner,beta,owner.damageSources().playerAttack(owner));
                require(owner.getAdvancements().getOrStartProgress(advancement).isDone()==(count==4),"native original PvP criterion requires four equipped pieces");
            }
        }
        void prepare(){acquisition();exactLeaves();reforges();criterion();arithmetic();bands();incomingMath();guards();policy();hud();wear(0);scores(0,0);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));owner.startUsingItem(InteractionHand.MAIN_HAND);client.ack();other.ack();start=game.server().getTickCount();updates=client.updates();stage=0;}
        boolean step(){client.ack();other.ack();if(stage<0){prepare();return false;}require(game.server().getTickCount()-start<260,"bounded real scheduled fixture");if(stage==0){if(client.updates()-updates<7)return false;require(owner.isBlocking(),"actual registered native shield warmed");scores(17,0);owner.removeAllEffects();owner.setInvulnerableTime(0);owner.damageCooldownTime=0;float hp=owner.getHealth();owner.hurtServer(owner.level(),owner.damageSources().playerAttack(beta),4);values(12,5,"actual fully shield-blocked criterion once");require(owner.getHealth()==hp,"native shield blocks health loss");owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);wear(4);scores(0,0);// GameChecks.tick runs after game.tick at server END: this is a completed-END baseline.
            set(armor,"epoch",game.items().epoch());set(armor,"phase",0);start=game.server().getTickCount();updates=client.updates();stage=1;return false;}
            int elapsed=game.server().getTickCount()-start;values(Math.min(120,10*(elapsed/20)),-4*(elapsed/20),"actual shared20tick cadence at "+elapsed);require((Integer)read(armor,"phase")==elapsed%20,"native shared phase follows actual server clock");if(elapsed<200)return false;require(elapsed==200&&client.updates()-updates>=200,"two hundred actual registered player connection updates");lifecycle();return true;}
        void lifecycle(){
            wear(0);scores(37,-11);game.items().forget(owner);values(37,-11,"actual disconnect cleanup preserves authoritative Nanobots counters");
            service.pulse(owner);values(37,-11,"re-entered passive reads preserved disconnected score");
            scores(73,19);game.items().clear(owner);values(0,0,"public purify clears both authoritative counters");
            scores(73,-19);service.reset(owner);values(0,0,"explicit lifecycle reset clears signed counters");
        }
        @Override public void close(){if(closed)return;closed=true;try{game.items().timeStop().end(game.maps().level());owner.stopUsingItem();created.forEach(Entity::discard);if(client!=null)client.close();if(other!=null)other.close();base.retain=true;base.close();saved.restore(game);betaSaved.restore(game);attribution.close();set(armor,"phase",oldPhase);set(armor,"epoch",oldEpoch);}finally{owner.connection.resetPosition();beta.connection.resetPosition();}}
    }
    private static Fixture running;
    private static String stopEpoch;
    public static void stopProbe(GameController game){
        require(running==null&&game.playing(),"actual stop probe owns an active match");stopEpoch=game.items().epoch();
        for(String name:List.of("TestAlpha","TestBeta")){
            var player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name)));
            game.items().mana().score(player,"nanobots_tier").set(73);
            game.items().mana().score(player,"nanobots_coldown").set(19);
        }
        SkyWars2.LOGGER.info("[check] PASS nanobot-armor-stop-probe");
    }
    public static void cleanup(GameController game){
        require(stopEpoch!=null&&!game.items().currentEpoch(stopEpoch),"real match epoch changed");
        for(String name:List.of("TestAlpha","TestBeta")){
            var player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name)));
            require(game.items().armorSets().nanobots().tier(player)==0&&game.items().armorSets().nanobots().cooldown(player)==0,
                    "actual match stop resets both Nanobots objectives");
            require(!game.items().hud(player).getString().contains("\uE114"),"reset source charge disappears from shared HUD");
        }
        stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS nanobot-armor-cleanup");
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS nanobot-armor (seven original source tables; normal/Ultra acquisition; signed authoritative counters, sequential119 overshoot, native effect layers, role/dimension/Time Stop gates; real rejected/absorbed/shield hurt criterion; actual20tick cadence over200ticks)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL nanobot-armor",error);}}
}
