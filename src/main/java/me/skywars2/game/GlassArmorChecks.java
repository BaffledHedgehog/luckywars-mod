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

/** Bounded synchronous proof of the source loot family and actual native incoming Glass criterion. */
public final class GlassArmorChecks {
    private GlassArmorChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Glass armor: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<.00001,why+" actual="+actual+" expected="+expected);}
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
    private static final class Witness extends Cow {
        int calls;float amount;net.minecraft.world.damagesource.DamageSource source;
        Witness(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){calls++;this.amount=amount;this.source=source;return super.hurtServer(level,source,amount);}
        void reset(){calls=0;source=null;amount=0;setHealth(100);setInvulnerableTime(0);damageCooldownTime=0;}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,beta;final State saved,betaSaved;
        final LbcGlassArmor service;final Vec3 at;final net.minecraft.server.level.ServerLevel level;
        final ItemStack[] pieces=new ItemStack[4];final List<Entity> created=new ArrayList<>();
        final List<SauvojenMeditationChecks.Attribution> attribution=new ArrayList<>();
        final Map<BlockPos,MapManager.BlockSnapshot> cells=new LinkedHashMap<>();
        final Client client,other;final int cowKills;
        Fixture(GameController game){
            this.game=game;service=game.items().armorSets().glass();
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&game.canUseGameplayItems(owner)&&owner.containerMenu==owner.inventoryMenu&&beta.containerMenu==beta.inventoryMenu,"active closed-inventory participants");
            require(!game.items().actionsBlocked(owner)&&!game.items().actionsBlocked(beta),"fixture never clears an ambient stun");
            require(!game.items().timeStop().stopped(owner.level()),"fixture never ends an ambient time stop");
            saved=new State(game,owner);betaSaved=new State(game,beta);
            attribution.add(new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta)));
            cowKills=owner.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));
            level=owner.level();at=new Vec3(Math.floor(owner.getX())+.5,Math.min(240,level.getMaxY()-20),Math.floor(owner.getZ())+.5);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-8,-2,-8)),BlockPos.containing(at.add(8,9,8))))
                require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir(),"resident empty bounded test volume");
            require(level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(at.add(-8,-2,-8),at.add(8,10,8)),e->e!=owner&&e!=beta).isEmpty(),"shards cannot touch ambient entities");
            for(var p:List.of(owner,beta)){
                p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);p.setHealth(500);p.setAbsorptionAmount(0);
                for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))p.entityTags().remove(tag);
            }
            owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);
            beta.teleportTo(level,at.x,at.y,at.z+10,Set.of(),180,0,true);
            client=new Client(game,owner);other=new Client(game,beta);
        }
        List<ItemStack> roll(String id){require(LootTestCatalog.isSuggested(Identifier.parse("lbc:"+id)),"public readiness "+id);return game.lbc().rollTable(level,at,owner,"lbc:"+id);}
        void wear(int count){for(int n=0;n<4;n++)owner.setItemSlot(SLOTS[n],n<count?pieces[n].copy():ItemStack.EMPTY);}
        void values(double value,String why){for(var slot:SLOTS)if(LbcGlassArmor.piece(owner.getItemBySlot(slot)))near(LbcGlassArmor.armor(owner.getItemBySlot(slot)),value,why+" "+slot);}
        void incoming(){service.incoming(owner);}
        void ready(){owner.removeAllEffects();owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setHealth(500);client.ack();other.ack();}
        Witness cow(Vec3 where){var c=new Witness(level);c.setPos(where);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);c.setHealth(100);require(level.addFreshEntity(c),"owned native cow");created.add(c);attribution.add(new SauvojenMeditationChecks.Attribution(game,List.of(c)));return c;}
        void block(BlockPos pos,BlockState state){cells.computeIfAbsent(pos,p->game.maps().snapshot(level,p));level.setBlock(pos,state,2|16|32);}
        void wall(BlockState state,int fromY,int toY){var center=BlockPos.containing(at);for(int dx=-3;dx<=3;dx++)for(int dy=fromY;dy<=toY;dy++)block(center.offset(dx,dy,2),state);}
        void acquisition(){
            String[] ids={"71eaf780-80f9-4861-8a64-49a55121b","aa20a077-1d80-4750-810e-1e2b46000e57","71eaf780-80f9-4861-8a64-49ab1247121b","71e4f780-80f9-4861-8a64-49ab1247121b"};
            for(int n=0;n<4;n++){
                var out=roll("neko_set_glass/"+PARTS[n]);require(out.size()==1&&out.getFirst().getCount()==1,"singleton source leaf");var s=pieces[n]=out.getFirst();
                require(s.is(List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET).get(n))&&s.getMaxDamage()==150&&s.getDamageValue()==0&&s.getEnchantments().isEmpty(),"fresh source base150");
                var d=LbcFartus.data(s);require(d.getIntOr("glass",0)==1&&d.getIntOr("transformable",0)==1&&d.getIntOr(PARTS[n],0)==1,"exact authored raw flags");
                require(s.get(DataComponents.EQUIPPABLE).slot()==SLOTS[n]&&s.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:glass"))&&s.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:armors/glass_"+PARTS[n]))&&s.get(DataComponents.TOOLTIP_STYLE).equals(Identifier.parse("lbc:tier_b")),"exact source model/equipment/tooltip");
                var name=s.get(DataComponents.CUSTOM_NAME);require(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("glass_"+PARTS[n])&&!name.getStyle().isItalic()&&name.getStyle().getColor().getValue()==0xffffff,"white exact source translated name");
                require(s.get(DataComponents.LORE).lines().size()==3,"three source lore rows");
                var attributes=s.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(attributes.size()==2,"exact two authored attributes");
                for(var e:attributes)require((e.attribute().equals(Attributes.ARMOR)||e.attribute().equals(Attributes.ARMOR_TOUGHNESS))&&e.modifier().amount()==40&&e.modifier().operation()==AttributeModifier.Operation.ADD_VALUE&&e.modifier().id().equals(Identifier.withDefaultNamespace(ids[n]))&&e.slot().test(SLOTS[n]),"original modifier ID/type/slot/amount");
            }
            var full=roll("neko_set_glass_full");require(full.size()==4,"source four guaranteed distinct full pieces");for(var leaf:pieces)require(full.stream().anyMatch(s->ItemStack.matches(s,leaf)),"full exact source leaf");
            var pre=roll("neko_set_glass_pre");require(pre.size()==5,"source five random draws");for(var s:pre)require(Arrays.stream(pieces).anyMatch(p->ItemStack.matches(p,s)),"pre exact source leaves");
            var seen=new HashSet<Integer>();for(int attempt=0;attempt<256&&seen.size()<4;attempt++){var out=roll("neko_set_glass");require(out.size()==1,"singleton equal-weight root");int found=-1;for(int n=0;n<4;n++)if(ItemStack.matches(out.getFirst(),pieces[n]))found=n;require(found>=0,"root exact leaf");seen.add(found);}require(seen.size()==4,"root reaches all four leaves");
        }
        @SuppressWarnings("unchecked") void reforges(){
            for(int n=0;n<4;n++){
                require(LbcBolts.reforgeTables(pieces[n],false).equals(List.of("transformables_"+PARTS[n]))&&LbcBolts.reforgeTables(pieces[n],true).equals(List.of("neko_set_glass")),"original normal slot and Ultra family routes");
                var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Identifier.parse("lbc:transformables_"+PARTS[n])));
                var pool=((List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools")).getFirst();var sequence=(Optional<Identifier>)read(table,"randomSequence");require(sequence.isEmpty(),"ordinary reforge uses native level random");
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);int total=0,offset=-1,weight=0;
                for(int i=0;i<pool.entries.size();i++){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pool.entries.get(i).expand(context,expanded::add);for(var e:expanded){int w=e.getWeight(context.getLuck());if(w<=0)continue;if(i==16){if(offset<0)offset=total;weight+=w;}total+=w;}}require(weight==1&&offset>=0,"original weight1 Glass slot branch");int bound=total,pick=offset;Object random=read(owner.level(),"random");int[] calls={0};var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual normal reforge donor");created.add(donor);var out=new ArrayList<ItemStack>();
                try{set(owner.level(),"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{if(m.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return m.invoke(random,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));reforge(donor,LbcBolts.ReforgeFamily.NORMAL,out);require(calls[0]==1&&donor.isRemoved()&&out.size()==1&&ItemStack.matches(out.getFirst(),pieces[n]),"actual native normal reforge consumes source donor and retains exact slot leaf");}finally{set(owner.level(),"random",random);donor.discard();}
                donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,pieces[n].copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual Ultra source donor");created.add(donor);out.clear();reforge(donor,LbcBolts.ReforgeFamily.ULTRA,out);require(donor.isRemoved()&&out.size()==1&&Arrays.stream(pieces).anyMatch(p->ItemStack.matches(p,out.getFirst())),"actual native Ultra reforge follows glass family root");
            }
        }
        void reforge(net.minecraft.world.entity.item.ItemEntity donor,LbcBolts.ReforgeFamily family,List<ItemStack> outputs){try{var method=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);method.setAccessible(true);var work=(LbcStonekivi.Work)method.invoke(game.items().bolts(),donor,family,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"existing native reforge Work completes");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}

        void amounts(ItemStack stack,double armor,double toughness){
            var out=new ArrayList<ItemAttributeModifiers.Entry>();
            for(var e:stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers()){
                double value=e.attribute().equals(Attributes.ARMOR)?armor:e.attribute().equals(Attributes.ARMOR_TOUGHNESS)?toughness:e.modifier().amount();
                out.add(new ItemAttributeModifiers.Entry(e.attribute(),new AttributeModifier(e.modifier().id(),value,e.modifier().operation()),e.slot(),e.display()));
            }
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(out));
        }
        void arithmetic(){
            near(LbcGlassArmor.decayed(40),38,"fresh exact decay");near(LbcGlassArmor.decayed(38),36.1,"second exact decay");
            near(LbcGlassArmor.decayed(36.1),34.295,"third exact decay");
            near(LbcGlassArmor.decayed(1.2349),1.1723,"floor millipoint before multiplication");
            near(LbcGlassArmor.decayed(-1.2341),-1.17325,"negative floor rather than truncation");
            near(LbcGlassArmor.shardDamage(),0,"no division at zero pieces");near(LbcGlassArmor.shardDamage(38),0,"fresh single negative raw skipped");
            near(LbcGlassArmor.shardDamage(38,38),4d/38,"fresh two raw2 softened");near(LbcGlassArmor.shardDamage(38,38,38),12d/38,"fresh three raw6 softened");
            near(LbcGlassArmor.shardDamage(38,38,38,38),20d/38,"fresh full raw10 softened to0.5263158");
            near(LbcGlassArmor.shardDamage(0,0,0,0),16,"exhausted set exact cap");near(LbcGlassArmor.shardDamage(1,1),8,"mean below2 never amplifies");
            near(LbcGlassArmor.shardDamage(2,2),8,"mean2 boundary");near(LbcGlassArmor.shardDamage(5.88,5.89),32d/11.77,"floor each slot before integer mean");
            for(int n=1;n<=4;n++){wear(n);incoming();values(38,"one synchronous criterion wears every equipped piece once N="+n);}
            for(int i=0;i<4;i++){wear(0);owner.setItemSlot(SLOTS[i],pieces[i].copy());incoming();near(LbcGlassArmor.armor(owner.getItemBySlot(SLOTS[i])),38,"each independent native slot");}
            wear(4);incoming();incoming();values(36.1,"sequential callbacks observe already mutated stack");
            // Raw source identity deliberately counts Glass in any armor slot.
            wear(0);owner.setItemSlot(EquipmentSlot.HEAD,pieces[0].copy());incoming();near(LbcGlassArmor.armor(owner.getItemBySlot(EquipmentSlot.HEAD)),38,"raw flag in mismatched armor slot retains source behavior");
            wear(0);var carried=pieces[0].copy();owner.getInventory().setItem(12,carried);owner.setItemInHand(InteractionHand.MAIN_HAND,pieces[1].copy());owner.setItemInHand(InteractionHand.OFF_HAND,pieces[2].copy());
            incoming();near(LbcGlassArmor.armor(carried),40,"inventory does not wear");near(LbcGlassArmor.armor(owner.getMainHandItem()),40,"main hand does not wear");near(LbcGlassArmor.armor(owner.getOffhandItem()),40,"offhand does not wear");owner.getInventory().clearContent();
        }
        void preservation(){
            wear(0);var stack=pieces[0].copy();stack.setDamageValue(37);stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING),1);
            stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Player custom Glass"));
            var d=LbcFartus.data(stack);d.putString("glass_fixture_custom","keep");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(d));
            amounts(stack,12.3459,91);
            var modifiers=new ArrayList<>(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers());var template=modifiers.getFirst();
            var unrelated=new ItemAttributeModifiers.Entry(Attributes.MOVEMENT_SPEED,new AttributeModifier(Identifier.parse("skywars2:glass_fixture_speed"),.125,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),template.slot(),template.display());modifiers.add(unrelated);stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(modifiers));
            var before=stack.copy();owner.setItemSlot(EquipmentSlot.FEET,stack);incoming();
            require(owner.getItemBySlot(EquipmentSlot.FEET)==stack,"wear mutates original equipped stack");near(LbcGlassArmor.armor(stack),11.72775,"custom armor quantized and decayed");
            var after=stack.copy();after.set(DataComponents.ATTRIBUTE_MODIFIERS,before.get(DataComponents.ATTRIBUTE_MODIFIERS));require(ItemStack.matches(after,before),"all other components, durability, enchantments and custom player data preserved");
            var changed=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(changed.size()==3&&changed.get(2).equals(unrelated),"unrelated attribute retained verbatim");
            for(int i=0;i<2;i++){var a=changed.get(i);var b=modifiers.get(i);near(a.modifier().amount(),11.72775,"both armor/toughness use armor value");require(a.attribute().equals(b.attribute())&&a.modifier().id().equals(b.modifier().id())&&a.modifier().operation()==b.modifier().operation()&&a.slot().equals(b.slot())&&a.display().equals(b.display()),"modifier ID/operation/slot/display retained");}
        }
        void nativeCriterion(){
            var target=cow(at.add(3,0,0));
            try{
                ready();wear(4);float hp=owner.getHealth();require(owner.hurtServer(level,owner.damageSources().generic(),1),"native accepted environmental hit");
                require(owner.getHealth()<hp,"native health loss positive control");values(38,"actual hurt criterion exactly once");require(target.calls==1,"one native radial hit per target");near(target.amount,20d/38,"actual raw10 softened damage");
                require(target.source.is(DamageTypes.ARROW)&&target.source.getEntity()==owner&&target.source.getDirectEntity()==owner&&target.getKillCredit()==owner,"native ARROW owner attribution without a projectile/helper");
                var post=owner.getItemBySlot(EquipmentSlot.FEET).copy();target.reset();owner.setInvulnerableTime(100);owner.damageCooldownTime=100;
                require(!owner.hurtServer(level,owner.damageSources().inFire(),.01f),"native cooldown rejection positive control");require(ItemStack.matches(post,owner.getItemBySlot(EquipmentSlot.FEET))&&target.calls==0,"rejected hurt has no wear or shards");
                ready();wear(4);target.reset();owner.setAbsorptionAmount(10);require(owner.getAbsorptionAmount()==10,"native absorption capacity positive control");hp=owner.getHealth();owner.hurtServer(level,owner.damageSources().generic(),1);
                require(owner.getHealth()==hp&&owner.getAbsorptionAmount()<10,"native absorbed hit without health loss");values(38,"absorption still fires incoming criterion once");require(target.calls==1,"absorption radial callback exactly once");
                ready();wear(4);target.reset();beta.teleportTo(level,at.x,at.y,at.z+3,Set.of(),180,0,true);other.ack();
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));owner.startUsingItem(InteractionHand.MAIN_HAND);set(owner,"useItemRemaining",(Integer)read(owner,"useItemRemaining")-6);require(owner.isBlocking(),"native raised shield timing");hp=owner.getHealth();
                owner.hurtServer(level,owner.damageSources().playerAttack(beta),4);require(owner.getHealth()==hp,"actual fully blocked shield hit");values(38,"shield criterion wears once");require(target.calls==1,"shield callback produces shards exactly once");
                owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);beta.teleportTo(level,at.x,at.y,at.z+10,Set.of(),180,0,true);other.ack();
                wear(0);target.reset();ready();owner.hurtServer(level,owner.damageSources().generic(),1);require(target.calls==0,"accepted hit without equipped glass cannot burst");
                wear(4);target.reset();target.setBaby(true);target.setHealth(.25f);incoming();require(!target.isAlive()&&target.getKillCredit()==owner,"fatal attributed native ARROW kills owned baby mob without loot residue");
            }finally{owner.stopUsingItem();target.discard();beta.teleportTo(level,at.x,at.y,at.z+10,Set.of(),180,0,true);other.ack();}
        }
        void shardNonReentry(){
            ready();wear(4);beta.teleportTo(level,at.x,at.y,at.z+3,Set.of(),180,0,true);other.ack();
            try{
                for(int i=0;i<4;i++)beta.setItemSlot(SLOTS[i],pieces[i].copy());
                beta.setHealth(500);beta.setAbsorptionAmount(0);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
                incoming();
                require(beta.getHealth()<500&&owner.getHealth()==500,"hostile equipped Glass target receives actual unshielded shards without replying");
                var source=beta.getLastDamageSource();require(source!=null&&source.is(DamageTypes.ARROW)&&source.getEntity()==owner&&source.getDirectEntity()==owner,"non-reentry retains native ARROW and wearer attribution");
                values(38,"original wearer decays once with no shard reflection");
                for(var slot:SLOTS)near(LbcGlassArmor.armor(beta.getItemBySlot(slot)),40,"native unshielded shard criterion never wears target Glass");
                var kills=game.items().indirectKills();var cause=kills.wildCause(owner);
                var wrapped=kills.captureDamage(kills.captureDamage(new LbcGlassArmor.ShardDamage(owner),cause),cause);
                float previous=beta.getHealth();require(beta.hurtServer(level,wrapped,1)&&beta.getHealth()<previous,"real attributed wrapped shard hit remains damaging");
                for(var slot:SLOTS)near(LbcGlassArmor.armor(beta.getItemBySlot(slot)),40,"nested attribution wrappers retain shard non-reentry");
                values(38,"wrapped shard cannot trigger reflected armor wear");
                beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
                var ordinaryArrow=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),owner);
                require(beta.hurtServer(level,ordinaryArrow,1),"subsequent ordinary attributed native ARROW is accepted");
                for(var slot:SLOTS)near(LbcGlassArmor.armor(beta.getItemBySlot(slot)),38,"ordinary non-shard ARROW still activates target Glass once");
                values(38,"ordinary-arrow target reply remains a shard and cannot reactivate original wearer");
                ready();wear(4);
                for(int i=0;i<4;i++)beta.setItemSlot(SLOTS[i],pieces[i].copy());
                for(var p:List.of(owner,beta)){
                    p.setHealth(500);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                    var shield=new ItemStack(Items.SHIELD);shield.set(DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE);p.setItemInHand(InteractionHand.MAIN_HAND,shield);p.startUsingItem(InteractionHand.MAIN_HAND);set(p,"useItemRemaining",(Integer)read(p,"useItemRemaining")-6);require(p.isBlocking(),"actual opposing raised unbreakable shield");
                }
                incoming();require(owner.getHealth()==500&&beta.getHealth()==500,"raised native shield blocks actual shard health damage");
                values(38,"shielded shard does not reply or reactivate original Glass");
                for(var slot:SLOTS)near(LbcGlassArmor.armor(beta.getItemBySlot(slot)),40,"blocked shard criterion never wears target Glass");
                beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
                beta.hurtServer(level,ordinaryArrow,1);require(beta.getHealth()==500,"ordinary native ARROW shield control blocks health damage");
                for(var slot:SLOTS)near(LbcGlassArmor.armor(beta.getItemBySlot(slot)),38,"blocked ordinary non-shard ARROW still activates Glass exactly once");
                values(38,"blocked ordinary-arrow target reply cannot reactivate original wearer");
            }finally{
                owner.stopUsingItem();beta.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);beta.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                for(var slot:SLOTS)beta.setItemSlot(slot,ItemStack.EMPTY);beta.teleportTo(level,at.x,at.y,at.z+10,Set.of(),180,0,true);other.ack();
            }
        }
        void rangeAndRoles(){
            var target=cow(at.add(3,0,0));
            try{
                for(var offset:List.of(new Vec3(0,0,0),new Vec3(.0005,0,0),new Vec3(.002,0,0),new Vec3(7,0,0),new Vec3(7.001,0,0),new Vec3(6,0,6),new Vec3(0,7,0))){
                    target.setPos(at.add(offset));target.reset();wear(4);incoming();boolean hit=offset.lengthSqr()>=.000001&&offset.lengthSqr()<=49;require((target.calls==1)==hit,"spherical feet radius .001..7 "+offset);
                }
                target.setPos(at.add(3,0,0));
                for(var role:List.of("spectator","rcdenyalways","sauvojen_tuntija","strict_map_object","magic_academy","transformator","sin_greed_active")){
                    target.reset();target.entityTags().add(role);try{wear(4);incoming();require(target.calls==0,"protected native role/Greed "+role);}finally{target.entityTags().remove(role);}
                }
                var board=game.server().getScoreboard();var own=owner.getTeam();var otherTeam=beta.getTeam();String key="glass_fixture_allies";
                require(board.getPlayerTeam(key)==null,"exclusive native team name");var team=board.addPlayerTeam(key);team.setAllowFriendlyFire(false);
                try{
                    board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);board.addPlayerToTeam(target.getScoreboardName(),team);
                    beta.teleportTo(level,at.x,at.y,at.z+3,Set.of(),180,0,true);other.ack();beta.setHealth(500);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
                    target.reset();wear(4);incoming();require(target.calls==0&&beta.getHealth()==500,"allied native mob and player both protected");
                }finally{
                    for(var e:List.of(owner,beta,target))board.removePlayerFromTeam(e.getScoreboardName(),team);board.removePlayerTeam(team);
                    if(own!=null)board.addPlayerToTeam(owner.getScoreboardName(),own);if(otherTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),otherTeam);
                    beta.teleportTo(level,at.x,at.y,at.z+10,Set.of(),180,0,true);other.ack();
                }
            }finally{target.discard();}
        }
        void cover(){
            var target=cow(at.add(0,0,4));
            try{
                wear(4);incoming();require(target.calls==1,"open air reaches native target");near(target.amount,20d/38,"full exposure native damage");
                for(var state:List.of(Blocks.STONE.defaultBlockState(),Blocks.GLASS.defaultBlockState())){
                    wall(state,-1,3);target.reset();wear(4);float seen=ServerExplosion.getSeenPercent(owner.getBoundingBox().getCenter(),target);near(seen,0,"native COLLIDER full wall including glass");incoming();require(target.calls==0,"full native cover blocks shards");wall(Blocks.AIR.defaultBlockState(),-1,3);
                }
                // A grounded cow is entirely hidden from this low burst by a full one-block wall.
                // Raise only this partial-cover witness so native samples genuinely straddle the obstacle.
                target.setPos(at.add(0,.5,4));
                for(var state:List.of(Blocks.STONE.defaultBlockState(),Blocks.STONE_SLAB.defaultBlockState())){
                    int y=state.is(Blocks.STONE_SLAB)?1:0;wall(state,y,y);target.reset();wear(4);float seen=ServerExplosion.getSeenPercent(owner.getBoundingBox().getCenter(),target);
                    require(seen>0&&seen<1,"native partial cover collision shape "+state+" exposure="+seen);incoming();require(target.calls==1,"partial cover still hits once");near(target.amount,(20d/38)*seen,"partial cover scales native ARROW damage");wall(Blocks.AIR.defaultBlockState(),-1,3);
                }
            }finally{wall(Blocks.AIR.defaultBlockState(),-1,3);target.discard();}
        }
        void guards(){
            for(String tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways")){
                wear(4);owner.entityTags().add(tag);try{incoming();values(40,"wearer role/time stop guard "+tag);}finally{owner.entityTags().remove(tag);}
            }
            wear(4);game.items().timeStop().begin(level,40);try{incoming();values(40,"actual dimension Time Stop guard");}finally{game.items().timeStop().end(level);}
            var old=game.stage();try{set(game,"stage",GameController.Stage.ENDING);wear(4);incoming();values(40,"ENDING guard");}finally{set(game,"stage",old);}
            wear(4);game.spectators().enter(owner);try{incoming();values(40,"actual observer guard");}finally{game.spectators().remove(owner);owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);client.ack();}
            require(!LbcDimensionItems.ordinary("minecraft:nexus")&&!LbcDimensionItems.ordinary("minecraft:imprinted"),"source two forbidden dimension IDs");
            var elsewhere=java.util.stream.StreamSupport.stream(game.server().getAllLevels().spliterator(),false).filter(l->l!=level).findFirst().orElseThrow();
            try{owner.teleportTo(elsewhere,at.x,at.y,at.z,Set.of(),0,0,true);wear(4);incoming();values(40,"actual foreign dimension guard");}finally{owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);client.ack();}
        }
        void suppression(){
            var target=cow(at.add(3,0,0));
            try{
                for(String tag:List.of("stopper","stopper_magic")){
                    var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().add(tag);require(level.addFreshEntity(marker),"owned native jammer");created.add(marker);game.items().magic().loaded(marker);
                    try{require(tag.equals("stopper")?game.items().magic().mechanismSuppressed(owner):game.items().magic().beaconSuppressed(at),"actual jammer positive control "+tag);target.reset();wear(4);incoming();values(38,"physical degradation under "+tag);require(target.calls==1,"physical shards under "+tag);}
                    finally{game.items().magic().unloaded(marker);marker.discard();}
                }
                target.reset();wear(4);owner.entityTags().add("nomagic_active");game.items().mana().noMagic(owner,100);
                try{incoming();values(38,"physical wear under personal noMagic");require(target.calls==1,"noMagic does not suppress physical shards");}finally{owner.entityTags().remove("nomagic_active");game.items().mana().noMagic(owner,0);}
                require(game.items().shooters().stun(beta,owner,10),"native stun positive control");
                try{require(game.items().actionsBlocked(owner),"stun actually blocks actions");target.reset();wear(4);incoming();values(38,"physical wear under stun");require(target.calls==1,"stun does not suppress physical shards");}finally{game.items().shooters().clearStun(owner);}
            }finally{target.discard();game.items().magic().flushCleansing();}
        }
        void criterion(){
            var advancement=Objects.requireNonNull(game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/armor/glass")));
            for(int count:new int[]{3,4}){
                advancement.value().criteria().keySet().forEach(key->owner.getAdvancements().revoke(advancement,key));wear(count);
                net.minecraft.advancements.triggers.CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(owner,beta,owner.damageSources().playerAttack(owner));
                require(owner.getAdvancements().getOrStartProgress(advancement).isDone()==(count==4),"actual imported PvP criterion requires all four flags");
            }
        }
        @Override public void close(){
            owner.stopUsingItem();beta.stopUsingItem();created.forEach(Entity::discard);
            for(var e:cells.entrySet())game.maps().restoreSnapshot(level,e.getKey(),e.getValue());
            client.close();other.close();saved.restore(game);betaSaved.restore(game);
            for(int i=attribution.size()-1;i>=0;i--)attribution.get(i).close();
            owner.getStats().setValue(owner,Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);
            owner.connection.resetPosition();beta.connection.resetPosition();
        }
    }
    public static void run(GameController game){
        try(var f=new Fixture(game)){
            f.acquisition();f.reforges();f.arithmetic();f.preservation();f.criterion();f.nativeCriterion();f.shardNonReentry();f.rangeAndRoles();f.cover();f.guards();f.suppression();
        }
        SkyWars2.LOGGER.info("[check] PASS glass-armor (seven live source tables, exact IDs, native normal/Ultra reforge, floor decay and softened source shards, component preservation, accepted/rejected/absorbed/shield native criteria, radius/roles/allies, full/partial COLLIDER cover, physical MAG/TECH/stun policy, observer/dimension/Time Stop guards, native fatal ARROW attribution, unshielded/shielded native shard non-reentry with ordinary ARROW positive controls)");
    }
}
