package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.phys.Vec3;

/** Independent native command controls, actual consumption and actual scheduled status ticks. */
public final class AttributeFoodChecks {
    private AttributeFoodChecks(){}
    private static final String[] COUNTERS={"all_seeing_eye","living_legend","dimensional_shifting"};
    private static final Identifier STORAGE=Identifier.parse("skywars2:attribute_food_checks");
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Attribute food: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<.0000001,why+" actual="+actual+" expected="+expected);}
    private static Field field(Object object,String name){for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static Object read(Object object,String name){try{return field(object,name).get(object);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object object,String name,Object value){try{field(object,name).set(object,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void flatten(net.minecraft.network.protocol.Packet<?> packet,List<net.minecraft.network.protocol.Packet<?>> result){if(packet instanceof ClientboundBundlePacket bundle){for(var child:bundle.subPackets())flatten(child,result);}else result.add(packet);}
    private record Fixed(String name,int scale,int increase,double storageScale,double start){}
    private static final List<Fixed> FIXED=List.of(new Fixed("armor",1,2,1,3.75),new Fixed("armor_toughness",1,1,1,2.875),
            new Fixed("attack_damage",1,1,1,3.75),new Fixed("attack_speed",5,1,.2,4.375),
            new Fixed("knockback_resistance",10,1,.1,.375),new Fixed("luck",1,1,1,3.75),
            new Fixed("max_health",1,2,1,21.875),new Fixed("movement_speed",100000,2000,.00001,.1234567));
    /** Source-only interception; unrelated world RNG calls always reach the original native RNG. */
    private static final class RandomScope implements AutoCloseable {
        final ServerLevel level;final Object original;final String source;final RandomSource control;final Float probability;final Integer choice;
        int floats,ints;
        RandomScope(ServerLevel level,String source,RandomSource control,Float probability,Integer choice){
            this.level=level;this.source=source;this.control=control;this.probability=probability;this.choice=choice;original=read(level,"random");
            set(level,"random",Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                boolean selected=StackWalker.getInstance().walk(frames->frames
                        .filter(frame->!frame.getClassName().equals(RandomScope.class.getName())&&!frame.getClassName().startsWith("java.")&&!frame.getClassName().startsWith("jdk."))
                        .findFirst().map(frame->frame.getClassName().equals(source)).orElse(false));
                if(selected&&method.getName().equals("nextFloat")){floats++;return probability!=null?probability:control.nextFloat();}
                if(selected&&method.getName().equals("nextInt")&&args!=null&&args.length==1){ints++;return choice!=null?choice:control.nextInt((Integer)args[0]);}
                try{return method.invoke(original,args);}catch(InvocationTargetException e){throw e.getCause();}
            }));
        }
        @Override public void close(){set(level,"random",original);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer p;final ServerLevel level;
        final AttributeMap attributes=new AttributeMap(DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<String,Integer> shared=new LinkedHashMap<>();final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> customStats=new LinkedHashMap<>();
        final Map<EquipmentSlot,ItemStack> equipmentCache=new EnumMap<>(EquipmentSlot.class);
        final Map<Field,Object> food=new LinkedHashMap<>();final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Map<String,ItemStack> sourceFoods=new LinkedHashMap<>();final Map<Field,Object> controls=new LinkedHashMap<>();
        final net.minecraft.world.entity.player.Input input;final net.minecraft.world.entity.Pose pose;
        final LbcIndirectKills.TerrainProvenance provenance;final SauvojenMeditationChecks.Attribution attribution;
        final CompoundTag storage;final int previousLastTick;final Vec3 origin;final BlockPos floor;
        final int appleStat;final Vec3 knownMovement;Marker zone;Cow witness;RandomScope random;int phase=-1,start,phaseTick;boolean closed;
        Fixture(GameController game){
            this.game=game;base=new ElementalGrimoireChecks.Fixture(game);p=base.owner;level=p.level();
            try{
                for(var h:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
                for(var objective:game.server().getScoreboard().getObjectives()){var value=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(value!=null)scores.put(objective,value.value());}
                for(var key:List.of("#random_out","#random16","rand2","#attribute_food_control")){var objective=game.server().getScoreboard().getObjective("lbc.math");var value=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(key),objective);shared.put(key,value==null?null:value.value());}
                for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
                for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);customStats.put(stat,p.getStats().getValue(stat));}
                for(var item:List.of(Items.APPLE,Items.COOKED_BEEF,Items.GLOW_BERRIES,Items.SWEET_BERRIES,Items.DRIED_KELP,Items.COOKIE,Items.GOLDEN_CARROT,Items.MELON_SLICE,Items.POISONOUS_POTATO,Items.GOLDEN_APPLE)){var stat=net.minecraft.stats.Stats.ITEM_USED.get(item);customStats.put(stat,p.getStats().getValue(stat));}
                for(var f:p.getFoodData().getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(p.getFoodData()));}
                for(var entry:((Map<?,?>)read(p,"lastEquipmentItems")).entrySet())equipmentCache.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());
                knownMovement=p.getKnownMovement();
                input=p.getLastClientInput();pose=p.getPose();for(var name:List.of("xxa","yya","zza","jumping","jumpTriggerTime","noJumpDelay"))controls.put(field(p,name),read(p,name));
                appleStat=p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(Items.APPLE));
                storage=game.server().getCommandStorage().get(STORAGE).copy();previousLastTick=(Integer)read(game.items().foodStatuses(),"lastTick");
                require(!game.items().timeStop().stopped(level),"no borrowed ambient Time Stop");for(var name:COUNTERS)require(readOnlyScore(name)==0,"exclusive empty borrowed source counter "+name);base.initialize();ack();stationary();origin=p.position();floor=BlockPos.containing(origin.add(0,-1,0));
                for(var pos:BlockPos.betweenClosed(floor.offset(-12,-3,-12),floor.offset(12,17,12))){require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir(),"resident bounded native-motion cleanup volume "+pos);terrain.put(pos.immutable(),game.maps().snapshot(level,pos));}
                provenance=game.items().indirectKills().captureTerrain(level,terrain.keySet());attribution=new SauvojenMeditationChecks.Attribution(game,List.of(p));
                require(level==game.server().overworld()&&game.server().getLevel(LbcHedgehogDimensions.IMPRINTED)!=null,"real Overworld and Imprinted worlds for sequential dispatch");
                for(var other:game.server().getPlayerList().getPlayers())if(other!=p)for(var key:COUNTERS){var objective=game.server().getScoreboard().getObjective(key);var value=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(other,objective);require(value==null||value.value()==0,"exclusive other-player source counter "+key);}
                counters(0,0,0);start=game.server().getTickCount();
            }catch(ReflectiveOperationException|RuntimeException|Error e){base.close();throw new IllegalStateException("Attribute food fixture setup",e);}
        }
        Holder<Attribute> key(String name){return BuiltInRegistries.ATTRIBUTE.get(Identifier.parse("minecraft:"+name)).orElseThrow();}
        void stationary(){p.setLastClientInput(new net.minecraft.world.entity.player.Input(false,false,false,false,false,false,false));p.setNoGravity(true);p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setSprinting(false);p.setShiftKeyDown(false);for(var name:List.of("xxa","yya","zza"))set(p,name,0f);set(p,"jumping",false);set(p,"jumpTriggerTime",0);set(p,"noJumpDelay",0);}
        AttributeInstance attr(String name){return Objects.requireNonNull(p.getAttribute(key(name)));}
        int score(String objective){return game.items().mana().score(p,objective).get();}
        int readOnlyScore(String name){var objective=game.server().getScoreboard().getObjective(name);var value=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(p,objective);return value==null?0:value.value();}
        void counters(int eye,int legend,int shift){int[] values={eye,legend,shift};for(int i=0;i<3;i++)game.items().mana().score(p,COUNTERS[i]).set(values[i]);}
        void ack(){p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(read(p.connection,"awaitingPositionFromClient")!=null){
            var packet=base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
            p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(packet.id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
        }}
        int command(String text){try{return game.server().getCommands().getDispatcher().execute(text,game.server().createCommandSourceStack().withEntity(p).withLevel(p.level()).withPosition(p.position()).withSuppressedOutput());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException("native source oracle: "+text,e);}}
        ItemStack raw(String flag,int value){var stack=new ItemStack(Items.APPLE,2);var data=new CompoundTag();data.putInt(flag,value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        void eat(ItemStack stack){p.getFoodData().setFoodLevel(1);p.setItemInHand(InteractionHand.MAIN_HAND,stack);var result=stack.finishUsingItem(p.level(),p);require(result.getCount()==1,"actual native food path debits once");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        @SuppressWarnings("unchecked") void sourceFoods(String id,List<String> flags,List<Item> carriers){
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,p).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).withOptionalRandomSeed(17).create(Optional.empty());
            var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools");require(pools.size()==1&&pools.getFirst().entries.size()==flags.size(),"actual registered source entry count "+id);
            int index=0;for(var source:pools.getFirst().entries){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();source.expand(context,expanded::add);require(expanded.size()==1,"native singleton expansion "+id+"/"+index);var outputs=new ArrayList<ItemStack>();expanded.getFirst().createItemStack(outputs::add,context);require(outputs.size()==1,"native singleton source output "+id+"/"+index);var item=outputs.getFirst();var flag=flags.get(index);
                require(item.getCount()==1&&item.is(carriers.get(index))&&LbcFartus.data(item).getIntOr(flag,0)==1,"actual registered source flag/carrier/count "+id+"/"+flag+" output="+item+" data="+LbcFartus.data(item));
                require(item.has(DataComponents.CONSUMABLE)&&item.has(DataComponents.CUSTOM_NAME)&&item.has(DataComponents.LORE)&&item.has(DataComponents.TOOLTIP_STYLE),"complete authored source modifiers retained "+flag);
                sourceFoods.put(flag,item.copy());index++;
            }
        }
        ItemStack source(String flag){return Objects.requireNonNull(sourceFoods.get(flag),flag).copyWithCount(2);}
        double fixedOracle(Fixed f,double value){attr(f.name()).setBaseValue(value);
            command("execute store result score #attribute_food_control lbc.math run attribute @s minecraft:"+f.name()+" base get "+f.scale());
            command("scoreboard players add #attribute_food_control lbc.math "+f.increase());
            command("execute store result storage "+STORAGE+" tmp double "+java.math.BigDecimal.valueOf(f.storageScale()).toPlainString()+" run scoreboard players get #attribute_food_control lbc.math");
            return game.server().getCommandStorage().get(STORAGE).getDoubleOr("tmp",Double.NaN);
        }
        void attributeCases(){
            sourceFoods("attributefood",List.of("max_healthattribute","attack_damageattribute","luckattribute","attack_speedattribute","armorattribute","armor_toughnessattribute","knockback_resistanceattribute","movement_speedattribute"),List.of(Items.APPLE,Items.COOKED_BEEF,Items.GLOW_BERRIES,Items.SWEET_BERRIES,Items.DRIED_KELP,Items.COOKIE,Items.GOLDEN_CARROT,Items.MELON_SLICE));
            sourceFoods("sussy_molten_penis",List.of("random_health","random_damage","random_speed"),List.of(Items.DRIED_KELP,Items.POISONOUS_POTATO,Items.GOLDEN_APPLE));
            for(var f:FIXED)for(double value:new double[]{f.start(),-f.start()}){
                double expected=fixedOracle(f,value);require(Double.isFinite(expected),"independent native execute-store storage oracle");
                attr(f.name()).setBaseValue(value);eat(source(f.name()+"attribute"));near(attr(f.name()).getBaseValue(),expected,"native original command sequence "+f.name()+" from "+value);
                attr(f.name()).setBaseValue(f.start());eat(raw(f.name()+"attribute",2));near(attr(f.name()).getBaseValue(),f.start(),"raw exact-one predicate "+f.name());
                attr(f.name()).replaceFrom(attributes.getInstance(key(f.name())));p.setHealth(20);p.removeAllEffects();
            }
            for(String name:List.of("attack_damage","movement_speed","max_health")){
                int scale=name.equals("attack_damage")?3000:name.equals("movement_speed")?4000:2500;double value=name.equals("movement_speed")?.12345:17.75;
                for(long seed:new long[]{17,998244353}){
                    attr(name).setBaseValue(value);int bound=command("attribute @s minecraft:"+name+" base get "+scale);
                    Object original=read(level,"random");double expected;
                    try{set(level,"random",RandomSource.create(seed));int result=command("random value 1.."+bound);expected=result*.001;}finally{set(level,"random",original);}
                    try(var rng=new RandomScope(level,LbcAttributeFood.class.getName(),RandomSource.create(seed),null,null)){
                        eat(source(name.equals("attack_damage")?"random_damage":name.equals("movement_speed")?"random_speed":"random_health"));
                        near(attr(name).getBaseValue(),expected,"same native world RNG as independent random command "+name);require(rng.ints==1,"one native random draw "+name);
                    }
                    require(p.getEffect(MobEffects.NAUSEA)!=null&&p.getEffect(MobEffects.NAUSEA).getDuration()==60&&p.getEffect(MobEffects.BLINDNESS)!=null&&p.getEffect(MobEffects.BLINDNESS).getDuration()==40,"source hidden random-food consequences");
                    require(!p.getEffect(MobEffects.NAUSEA).isVisible()&&!p.getEffect(MobEffects.BLINDNESS).showIcon(),"native hidden particles/icon");
                    attr(name).replaceFrom(attributes.getInstance(key(name)));p.setHealth(20);p.removeAllEffects();
                }
            }
            game.items().mana().score(ScoreHolder.forNameOnly("#random_out"),"lbc.math").set(1777);attr("attack_damage").setBaseValue(-.25);
            command("scoreboard players set #attribute_food_control lbc.math 1777");
            boolean reversed=false;try{game.server().getCommands().getDispatcher().execute("execute store result score #attribute_food_control lbc.math run random value 1..-750",game.server().createCommandSourceStack().withLevel(level).withSuppressedOutput());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){reversed=true;}
            require(reversed,"independent native reversed range fails parsing");command("execute store result storage "+STORAGE+" tmp double .001 run scoreboard players get #attribute_food_control lbc.math");
            double failedRange=game.server().getCommandStorage().get(STORAGE).getDoubleOr("tmp",Double.NaN);
            try(var rng=new RandomScope(level,LbcAttributeFood.class.getName(),RandomSource.create(1),null,null)){
                eat(raw("random_damage",1));near(attr("attack_damage").getBaseValue(),failedRange,"native failed macro range retains actual shared result");require(rng.ints==0,"invalid range has no substitute random draw");
            }
            attr("attack_damage").replaceFrom(attributes.getInstance(key("attack_damage")));p.removeAllEffects();
            var both=raw("armorattribute",1);var data=LbcFartus.data(both);data.putInt("luckattribute",1);both.set(DataComponents.CUSTOM_DATA,CustomData.of(data));attr("armor").setBaseValue(2);attr("luck").setBaseValue(3);eat(both);near(attr("armor").getBaseValue(),4,"independent armor flag");near(attr("luck").getBaseValue(),4,"independent simultaneous luck flag");
            p.getAttributes().assignAllValues(attributes);p.setHealth(20);p.removeAllEffects();
            var modifierId=Identifier.parse("minecraft:attribute_food_fixture");attr("armor").addTransientModifier(new AttributeModifier(modifierId,.375,AttributeModifier.Operation.ADD_VALUE));
            try{eat(raw("armorattribute",1));require(attr("armor").getModifier(modifierId)!=null&&attr("armor").getModifier(modifierId).amount()==.375,"native base reward preserves independent modifier identity/value");}finally{attr("armor").removeModifier(modifierId);}
            p.getAttributes().assignAllValues(attributes);p.setHealth(20);
            for(String role:List.of("spectator","rcdenyalways","strict_map_object")){p.entityTags().add(role);try{double before=attr("armor").getBaseValue();game.items().attributeFood().consumed(p,raw("armorattribute",1));near(attr("armor").getBaseValue(),before,"shared role admission "+role);}finally{p.entityTags().remove(role);}}
            for(boolean tech:new boolean[]{false,true}){zone(tech);try{double expected=fixedOracle(FIXED.getFirst(),2.75);attr("armor").setBaseValue(2.75);eat(raw("armorattribute",1));near(attr("armor").getBaseValue(),expected,"physical source base update permits "+(tech?"TECH":"MAG"));}finally{removeZone();}}
            game.items().timeStop().begin(level,40);try{double before=attr("armor").getBaseValue();game.items().attributeFood().consumed(p,raw("armorattribute",1));near(attr("armor").getBaseValue(),before,"completed callback still respects shared Time Stop");}finally{game.items().timeStop().end(level);}
            p.getAttributes().assignAllValues(attributes);p.setHealth(20);p.removeAllEffects();
            base.recording.packets.clear();
        }
        void removeZone(){if(zone!=null){game.items().magic().unloaded(zone);zone.discard();zone=null;}}
        void zone(boolean tech){zone=new Marker(EntityTypes.MARKER,level);zone.setPos(origin);zone.entityTags().add(tech?"stopper":"stopper_magic");require(level.addFreshEntity(zone),"actual native jammer");game.items().magic().loaded(zone);require(tech?game.items().magic().mechanismSuppressedAt(level,origin):game.items().magic().suppressedAt(level,origin),"actual shared jammer predicate");}
        void begin(int next){removeZone();if(random!=null){random.close();random=null;}game.items().timeStop().end(level);phase=next;phaseTick=game.server().getTickCount();base.recording.packets.clear();
            stationary();require(p.position().distanceToSqr(origin)<.0000001,"phase starts at native stationary origin phase="+next+" actual="+p.position()+" expected="+origin);
            counters(100,100,0);
            if(next==0){level.setBlock(floor,Blocks.STRUCTURE_VOID.defaultBlockState(),818);level.setBlock(floor.offset(1,0,0),Blocks.STONE.defaultBlockState(),818);p.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,200,0),p);witness=new Cow(EntityTypes.COW,level);witness.setPos(origin.add(1,0,1));witness.setNoGravity(true);witness.setNoAi(true);require(level.addFreshEntity(witness),"native private-dot witness");}
            if(next==1)zone(false);if(next==2)zone(true);if(next==3)game.items().timeStop().begin(level,40);
            if(next>=4){require(game.items().wands().validTarget(p),"shared native teleport target admitted tags="+p.entityTags());counters(0,0,1);random=new RandomScope(level,LbcFoodStatuses.class.getName(),null,next==4?.99f:0f,next==4?null:3);}
        }
        boolean step(){
            require(game.server().getTickCount()-start<180,"bounded actual status scheduler");ack();
            if(phase<0){attributeCases();begin(0);return false;}
            require(p.position().distanceToSqr(origin)<.0000001,"native status fixture moved phase="+phase+" actual="+p.position()+" expected="+origin+" input="+p.getLastClientInput());stationary();
            int elapsed=game.server().getTickCount()-phaseTick;
            if(phase<4){if(elapsed<4)return false;require(elapsed==4,"one fixture observation per actual tick");boolean pause=phase==1||phase==3;
                require(score("all_seeing_eye")==100-(pause?0:4)&&score("living_legend")==100-(pause?0:4),"actual scoreboard lifetime phase="+phase);
                if(phase==0){require(!p.hasEffect(MobEffects.NAUSEA),"actual eye cleanses native nausea");require(Arrays.stream(net.minecraft.world.item.DyeColor.values()).anyMatch(color->level.getBlockState(floor).is(Blocks.STAINED_GLASS.pick(color))),"source custom airs replacement produced native glass");require(level.getBlockState(floor.offset(1,0,0)).is(Blocks.STONE),"solid source exclusion retained");require(base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).anyMatch(packet->packet.particle().getType()==net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT&&packet.count()==0&&packet.overrideLimiter()),"native forced private status dot");}
                begin(phase+1);return false;
            }
            if(score("dimensional_shifting")>0)return false;
            require(random.floats==1&&random.ints==(phase==4?0:1),"actual source one-second chance draw and conditional one dispatch roll");
            if(phase==4){require(p.level()==level,"native false chance retains source world");begin(5);return false;}
            require(p.level()==level&&p.position().distanceToSqr(origin)<.0000001,"real roll4 executes Overworld to Imprinted then original Overworld");
            var packets=new ArrayList<net.minecraft.network.protocol.Packet<?>>();base.recording.packets.forEach(packet->flatten(packet,packets));long respawns=packets.stream().filter(ClientboundRespawnPacket.class::isInstance).count();
            var destinations=packets.stream().filter(ClientboundRespawnPacket.class::isInstance).map(ClientboundRespawnPacket.class::cast).map(packet->packet.commonPlayerSpawnInfo().dimension()).toList();int imprinted=destinations.indexOf(LbcHedgehogDimensions.IMPRINTED);
            require(imprinted>=0&&destinations.subList(imprinted+1,destinations.size()).contains(Level.OVERWORLD),"actual ordered Imprinted then Overworld native transitions prove sequential dispatcher count="+respawns+" destinations="+destinations+" raw="+base.recording.packets.stream().map(packet->packet.getClass().getSimpleName()).toList()+" flattened="+packets.stream().map(packet->packet.getClass().getSimpleName()).toList());
            counters(3,4,5);game.items().foodStatuses().forget(p);for(var key:COUNTERS)require(score(key)==0,"source death/leave clear "+key);return true;
        }
        @SuppressWarnings("unchecked") @Override public void close(){
            if(closed)return;closed=true;
            try{
                removeZone();if(random!=null){random.close();random=null;}game.items().timeStop().end(level);counters(0,0,0);
                if(witness!=null)witness.discard();if(p.level()!=level)p.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);ack();
                p.getAttributes().assignAllValues(attributes);game.server().getCommandStorage().set(STORAGE,storage);set(game.items().foodStatuses(),"lastTick",previousLastTick);
            }finally{
                try{base.close();}finally{
                p.getAttributes().assignAllValues(attributes);p.setHealth(base.saved.health);p.setAbsorptionAmount(base.saved.absorption);
                p.setKnownMovement(knownMovement);p.setLastClientInput(input);p.setPose(pose);controls.forEach((f,v)->{try{f.set(p,v);}catch(IllegalAccessException e){throw new IllegalStateException(e);}});var cache=(Map<EquipmentSlot,ItemStack>)read(p,"lastEquipmentItems");cache.clear();equipmentCache.forEach((slot,item)->cache.put(slot,item.copy()));
                for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(p,objective,true).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(p,objective);
                var objective=game.server().getScoreboard().getObjective("lbc.math");if(objective!=null)shared.forEach((name,value)->{var holder=ScoreHolder.forNameOnly(name);if(value==null)game.server().getScoreboard().resetSinglePlayerScore(holder,objective);else game.server().getScoreboard().getOrCreatePlayerScore(holder,objective,true).set(value);});
                advancements.forEach((a,done)->{for(var criterion:a.value().criteria().keySet())if(done.contains(criterion))p.getAdvancements().award(a,criterion);else p.getAdvancements().revoke(a,criterion);});
                p.getStats().setValue(p,net.minecraft.stats.Stats.ITEM_USED.get(Items.APPLE),appleStat);
                customStats.forEach((stat,value)->p.getStats().setValue(p,stat,value));
                food.forEach((f,v)->{try{f.set(p.getFoodData(),v);}catch(IllegalAccessException e){throw new IllegalStateException(e);}});
                terrain.forEach((pos,state)->game.maps().restoreSnapshot(level,pos,state));game.items().indirectKills().restoreTerrain(provenance);attribution.close();
                for(var entry:terrain.entrySet())require(level.getBlockState(entry.getKey()).equals(entry.getValue().state()),"post-teardown exact native terrain "+entry.getKey());
                for(var name:COUNTERS)require(readOnlyScore(name)==0,"post-equipment native callback teardown does not resurrect "+name);
                require(p.isNoGravity()==base.saved.noGravity&&p.noPhysics==base.saved.noPhysics&&p.getAbilities().invulnerable==base.saved.abilitiesInvulnerable&&p.getLastClientInput().equals(input),"exact borrowed physics/input after teardown");
                }
            }
        }
    }
    private static Fixture running;
    public static void start(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);}
    public static void run(GameController game){start(game);}
    public static boolean tick(GameController game){if(running==null)return true;try{if(!running.step())return false;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS attribute-food (native consume single debit; eight original command quantization controls, three native RNG controls and negative shared result; actual status clocks/MAG TECH TimeStop/terrain/private particles; two native sequential dimension transitions; scoped restore)");return true;}catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL attribute-food",failure);return true;}}
    public static void cancel(GameController game){if(running!=null){try{running.close();}finally{running=null;}}}
}
