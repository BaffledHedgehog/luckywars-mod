package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.stats.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.*;
import net.minecraft.world.phys.Vec3;

/** Synchronous source-carrier consumption; adopted long-lived consequences are cleared before a tick. */
public final class NamedFoodChecks {
    private NamedFoodChecks(){}
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Named foods: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<.00001,why+" actual="+actual+" expected="+expected);}
    private static Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object replacement){try{field(value,name).set(value,replacement);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private record Anchor(String flag,Holder<MobEffect> effect,int ticks,int amplifier){}
    private static Anchor a(String flag,Holder<MobEffect> effect,int ticks,int amplifier){return new Anchor(flag,effect,ticks,amplifier);}
    private static final List<Anchor> ANCHORS=List.of(
        a("golden_beef",MobEffects.INSTANT_HEALTH,1,127),a("golden_beetroot",MobEffects.INSTANT_HEALTH,600,0),
        a("golden_berries",MobEffects.HASTE,3600,10),a("golden_bread",MobEffects.HEALTH_BOOST,1800,0),
        a("golden_cake",MobEffects.SPEED,2400,19),a("golden_chicken",MobEffects.SLOW_FALLING,5400,0),
        a("golden_chorus",MobEffects.RESISTANCE,2000,3),a("golden_cookie",MobEffects.SPEED,2400,2),
        a("golden_fish",MobEffects.WATER_BREATHING,1800,0),a("golden_kelp",MobEffects.CONDUIT_POWER,300,2),
        a("golden_melon",MobEffects.REGENERATION,200,5),a("golden_mutton",MobEffects.HEALTH_BOOST,800,0),
        a("golden_pie",MobEffects.SPEED,200,255),a("golden_porkchop",MobEffects.HEALTH_BOOST,60,90),
        a("golden_potato",MobEffects.FIRE_RESISTANCE,800,0),a("golden_rabbit",MobEffects.JUMP_BOOST,800,10),
        a("golden_stew",MobEffects.INSTANT_HEALTH,1000,99),a("diamond_apple",MobEffects.HASTE,7200,1),
        a("diamond_beef",MobEffects.INSTANT_HEALTH,1,127),a("diamond_berries",MobEffects.LUCK,5400,40),
        a("diamond_carrot",MobEffects.NIGHT_VISION,7200,0),a("diamond_chicken",MobEffects.SLOW_FALLING,20000,0),
        a("diamond_fish",MobEffects.CONDUIT_POWER,14400,1),a("diamond_mutton",MobEffects.HEALTH_BOOST,7200,4),
        a("diamond_porkchop",MobEffects.HEALTH_BOOST,7200,9),a("diamond_potato",MobEffects.HASTE,7200,2),
        a("diamond_rabbit",MobEffects.JUMP_BOOST,7200,127),a("diamond_stew",MobEffects.CONDUIT_POWER,3600,2),
        a("netherite_apple",MobEffects.SATURATION,10000,0),a("netherite_carrot",MobEffects.NIGHT_VISION,-1,0),
        a("netherite_fish",MobEffects.GLOWING,28800,0),a("netherite_potato",MobEffects.FIRE_RESISTANCE,-1,0),
        a("netherite_stew",MobEffects.INSTANT_DAMAGE,-1,99),a("bedrock_apple",MobEffects.SATURATION,28800,0),
        a("netherstar_fish",MobEffects.REGENERATION,57600,5),a("transdimensional_fish",MobEffects.HEALTH_BOOST,115200,49),
        a("multiverse_fish",MobEffects.STRENGTH,230400,124),a("human_eye",MobEffects.NAUSEA,20,1),
        a("human_flesh",MobEffects.NAUSEA,20,1));
    private static final List<String> RAW=List.of("acid_rifle","all_seeing_eye","blaze","delayed_death","dimensional_shifting","end_damage","gatling_fovos","lolidefecation","greed_prestige","mana","living_legend","lookback","manamax","manaregen","reverse_mana","nanobots_coldown","nanobots_tier","nitro","nomagic","piss","ray","reactivefuel","scared","screamray","shock","sleep_time","target","twitching","void_resistance");
    private static MobEffectInstance layer(ServerPlayer p,Holder<MobEffect> type,int amplifier){for(var effect=p.getEffect(type);effect!=null;effect=((EffectLayers.Access)effect).skywars2$layerHidden())if(effect.getAmplifier()==amplifier)return effect;return null;}
    private static MobEffectInstance effect(ServerPlayer p,Anchor anchor){var effect=layer(p,anchor.effect(),anchor.amplifier());require(effect!=null&&effect.getDuration()==anchor.ticks(),"source layer "+anchor.flag()+" expected="+anchor.ticks()+"/"+anchor.amplifier()+" active="+p.getEffect(anchor.effect()));return effect;}

    private static final class State {
        final ServerPlayer player;final ServerLevel level;final GameChecks.WandThirdPlayerState saved;
        final AttributeMap attributes=new AttributeMap(DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<Objective,Integer> scores=new LinkedHashMap<>();final Map<Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Field,Object> controls=new LinkedHashMap<>(),food=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        final net.minecraft.world.entity.player.Input input;final Pose pose;final Vec3 movement;final boolean ground,shift,sprint;
        State(GameController game,ServerPlayer player){this.player=player;level=player.level();saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());input=player.getLastClientInput();pose=player.getPose();movement=player.getKnownMovement();ground=player.onGround();shift=player.isShiftKeyDown();sprint=player.isSprinting();
            for(var key:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var value=player.getAttribute(key);if(value!=null)attributes.getInstance(key).replaceFrom(value);}
            for(var objective:game.server().getScoreboard().getObjectives()){var value=game.server().getScoreboard().getPlayerScoreInfo(player,objective);if(value!=null)scores.put(objective,value.value());}
            for(var item:BuiltInRegistries.ITEM){var stat=Stats.ITEM_USED.get(item);stats.put(stat,player.getStats().getValue(stat));}for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var name:List.of("xxa","yya","zza","jumping","jumpTriggerTime","noJumpDelay","attackStrengthTicker","itemSwapTicker","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource"))controls.put(field(player,name),read(player,name));
            try{for(var f:player.getFoodData().getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(player.getFoodData()));}for(var f:game.players().get(player.getUUID()).getClass().getFields())if(!Modifier.isStatic(f.getModifiers())){var value=f.get(game.players().get(player.getUUID()));profile.put(f,value instanceof int[] array?array.clone():value instanceof Set<?> set?new HashSet<>(set):value);}for(var e:((Map<?,?>)read(player,"lastEquipmentItems")).entrySet())equipment.put((EquipmentSlot)e.getKey(),((ItemStack)e.getValue()).copy());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings("unchecked") void restore(GameController game){if(player.level()!=level)player.teleportTo(level,saved.position.x,saved.position.y,saved.position.z,Set.of(),saved.yaw,saved.pitch,true);saved.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(saved.health);player.setAbsorptionAmount(saved.absorption);player.setLastClientInput(input);player.setPose(pose);player.setKnownMovement(movement);player.setOnGround(ground);player.setShiftKeyDown(shift);player.setSprinting(sprint);
            for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(player,objective,true).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(player,objective);
            stats.forEach((stat,value)->player.getStats().setValue(player,stat,value));advancements.forEach((a,done)->{for(var criterion:a.value().criteria().keySet())if(done.contains(criterion))player.getAdvancements().award(a,criterion);else player.getAdvancements().revoke(a,criterion);});
            try{for(var e:controls.entrySet())e.getKey().set(player,e.getValue());for(var e:food.entrySet())e.getKey().set(player.getFoodData(),e.getValue());for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());var cache=(Map<EquipmentSlot,ItemStack>)read(player,"lastEquipmentItems");cache.clear();equipment.forEach((slot,item)->cache.put(slot,item.copy()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
    }
    private static final class Client implements AutoCloseable {
        final ServerPlayer player;final ServerGamePacketListenerImpl previous;final ElementalGrimoireChecks.Recording wire;
        Client(GameController game,ServerPlayer player){this.player=player;previous=player.connection;wire=new ElementalGrimoireChecks.Recording(player);player.connection=new ServerGamePacketListenerImpl(game.server(),wire,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));ack();}
        void ack(){player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(read(player.connection,"awaitingPositionFromClient")!=null){var position=wire.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(position.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));}}
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    private static final class Draws implements AutoCloseable {
        final ServerLevel level;final Object previous;final float value;int count;
        Draws(ServerLevel level,float value){this.level=level;this.value=value;previous=read(level,"random");set(level,"random",Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
            boolean source=StackWalker.getInstance().walk(frames->frames.filter(frame->!frame.getClassName().equals(Draws.class.getName())&&!frame.getClassName().startsWith("java.")&&!frame.getClassName().startsWith("jdk.")).findFirst().map(frame->frame.getClassName().equals(LbcNamedFoods.class.getName())).orElse(false));
            if(source&&method.getName().equals("nextFloat")){count++;return value;}try{return method.invoke(previous,args);}catch(InvocationTargetException e){throw e.getCause();}
        }));}
        public void close(){set(level,"random",previous);}
    }
    /** Control only the native spawnegg pool's uniform 8..12 roll count, never item selection. */
    private static final class EggRolls implements AutoCloseable {
        final ServerLevel level;final Object previous;int count;
        EggRolls(ServerLevel level){this.level=level;previous=read(level,"random");set(level,"random",Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
            boolean pool=StackWalker.getInstance().walk(frames->{var names=frames.map(StackWalker.StackFrame::getClassName).toList();return names.contains(LbcNamedFoods.class.getName())&&names.contains("net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator");});
            if(pool&&method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(5)){require(count<5,"no sixth native spawnegg table invocation");return count++;}
            try{return method.invoke(previous,args);}catch(InvocationTargetException e){throw e.getCause();}
        }));}
        public void close(){set(level,"random",previous);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer p,other;final State saved,otherSaved;final Client client,otherClient;
        final ServerLevel level;final Vec3 origin;final SauvojenMeditationChecks.Attribution attribution;
        final Map<String,ItemStack> foods=new LinkedHashMap<>();final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<String,Integer> math=new LinkedHashMap<>();final String epoch;boolean closed;
        Fixture(GameController game){this.game=game;p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(p!=null&&other!=null&&game.playing()&&game.allowDamage(p)&&game.allowDamage(other),"actual mode1 Alpha/Beta");require(!p.isUsingItem()&&!other.isUsingItem(),"idle native consumers");require(game.items().blind().queued(p)==0&&game.items().blind().queued(other)==0&&((Collection<?>)read(game.items().blind(),"clouds")).stream().noneMatch(cloud->read(cloud,"owner")==p||read(cloud,"owner")==other),"no borrowed Blind gatling queue or owned cloud");saved=new State(game,p);otherSaved=new State(game,other);level=p.level();origin=new Vec3(p.getX(),240,p.getZ());epoch=game.items().epoch();
            for(var world:game.server().getAllLevels())world.getAllEntities().forEach(before::add);for(var name:List.of("tmp","tmp1")){var objective=game.server().getScoreboard().getObjective("lbc.math");var value=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(name),objective);math.put(name,value==null?null:value.value());}
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(p,other));client=new Client(game,p);otherClient=new Client(game,other);
        }
        void neutral(ServerPlayer player){player.setLastClientInput(new net.minecraft.world.entity.player.Input(false,false,false,false,false,false,false));player.setNoGravity(true);player.setOnGround(false);player.setDeltaMovement(Vec3.ZERO);player.setKnownMovement(Vec3.ZERO);player.setSprinting(false);player.setShiftKeyDown(false);for(var key:List.of("xxa","yya","zza"))set(player,key,0f);set(player,"jumping",false);set(player,"jumpTriggerTime",0);set(player,"noJumpDelay",0);}
        void removeOutputs(){for(var world:game.server().getAllLevels()){var added=new ArrayList<Entity>();for(var entity:world.getAllEntities())if(!before.contains(entity)&&!(entity instanceof ServerPlayer))added.add(entity);for(var entity:added){game.items().entityUnloaded(entity);entity.discard();}}}
        void reset(){for(var player:List.of(p,other)){game.items().clear(player);game.items().blind().forget(player);require(game.items().blind().queued(player)==0,"fixture Blind adoption queue removed before native ticks");player.removeAllEffects();for(var name:RAW)game.items().mana().score(player,name).set(0);player.entityTags().removeAll(List.of("have_lucky_effects","have_lucky_effects_5t","have_lucky_effects_1s"));player.getInventory().clearContent();player.inventoryMenu.getCraftSlots().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);player.getAttributes().assignAllValues((player==p?saved:otherSaved).attributes);player.setHealth(Math.min(20,player.getMaxHealth()));player.setAbsorptionAmount(0);neutral(player);game.items().mana().set(player,100);game.items().mana().maximum(player,10000);game.items().mana().baseRegen(player,1);}
            require(p.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true)&&other.teleportTo(level,origin.x+8,origin.y,origin.z,Set.of(),0,0,true),"native bounded baseline positions");client.ack();otherClient.ack();removeOutputs();game.items().magic().inventoryChanged(p);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();require(!game.items().timeStop().stopped(level)&&game.items().wands().validTarget(p)&&game.items().wands().validTarget(other),"ordinary unprotected native consumers");}
        @SuppressWarnings("unchecked") void table(String id){var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,p).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).withOptionalRandomSeed(17).create(Optional.empty());var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools");require(!pools.isEmpty(),"registered source table "+id);var wanted=new HashSet<String>();ANCHORS.forEach(a->wanted.add(a.flag()));wanted.add("eat_banana");for(var pool:pools)for(var source:pool.entries){var entries=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();source.expand(context,entries::add);for(var entry:entries){var result=new ArrayList<ItemStack>();entry.createItemStack(result::add,context);for(var item:result)for(var flag:wanted)if(LbcFartus.data(item).getIntOr(flag,0)==1){require(item.getCount()==1&&item.has(DataComponents.CONSUMABLE)&&item.has(DataComponents.CUSTOM_NAME)&&item.has(DataComponents.LORE),"full actual named source carrier "+flag+" item="+item);require(foods.putIfAbsent(flag,item.copy())==null,"unique actual named source leaf "+flag);}}}}
        ItemStack source(String flag){var item=foods.get(flag);require(item!=null,"actual original source leaf "+flag);return item.copyWithCount(2);}
        void eat(String flag){var item=source(flag);p.getFoodData().setFoodLevel(1);p.setItemInHand(InteractionHand.MAIN_HAND,item);var output=item.finishUsingItem(p.level(),p);require(output.getCount()==1,"single native consume debit "+flag);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        int score(String name){return game.items().mana().score(p,name).get();}
        List<ItemEntity> drops(){var out=new ArrayList<ItemEntity>();for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())if(!before.contains(e)&&e instanceof ItemEntity item)out.add(item);return out;}
        void command(String text){try{require(game.server().getCommands().getDispatcher().execute(text,game.server().createCommandSourceStack().withEntity(p).withLevel(p.level()).withPosition(p.position()).withSuppressedOutput())>0,"native effect oracle accepted");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(text,e);}}
        void profiles(){reset();table("imba/food");table("bananas");require(foods.size()==40,"all40 actual original named carriers");for(var anchor:ANCHORS){if(anchor.flag().equals("human_flesh"))continue;reset();if(anchor.flag().equals("diamond_chicken")){try(var rolls=new EggRolls(level)){eat(anchor.flag());require(rolls.count==5,"five separate native uniform8..12 source table rolls actual="+rolls.count);}}else eat(anchor.flag());effect(p,anchor);
                if(anchor.flag().equals("golden_pie"))require(score("living_legend")==200,"source legend counter units");if(anchor.flag().equals("human_eye"))require(score("all_seeing_eye")==12000,"source eye counter units");if(anchor.flag().equals("golden_stew"))require(score("piss")==1000&&game.items().psycho().effectValue(p,"piss")==1000,"raw golden stew enters existing native owner");
                if(anchor.flag().equals("diamond_chicken")){require(drops().stream().mapToInt(item->item.getItem().getCount()).sum()==50,"native five roll counts8+9+10+11+12 yield50 actual eggs, entities="+drops().size());for(var item:drops()){require(item.getItem().getItem() instanceof SpawnEggItem&&LbcFartus.data(item.getItem()).isEmpty(),"complete vanilla native spawn egg output "+item.getItem());require(item.getOwner()==p&&game.items().currentEpoch(item),"source drop wearer and current epoch");}}
                if(anchor.flag().equals("netherite_stew"))stew();
            }}
        void stew(){for(var name:RAW)require(score(name)==(name.equals("end_damage")?21474:Integer.MAX_VALUE),"exact stew raw value "+name);require(p.entityTags().containsAll(Set.of("have_lucky_effects","have_lucky_effects_5t","have_lucky_effects_1s")),"three actual source scheduler tags");for(var name:List.of("scared","lookback","twitching","piss","screamray"))require(game.items().psycho().effectValue(p,name)==Integer.MAX_VALUE,"existing Psycho adopts raw owner "+name);require(((Map<?,?>)read(game.items().blaze(),"heads")).containsKey(p.getUUID()),"Blaze existing owner registry");require(((Map<?,?>)read(game.items().erecto(),"rays")).containsKey(p),"Erecto existing owner registry");require(((Map<?,?>)read(game.items().awakenedAcid(),"rifles")).containsKey(p),"Acid existing owner registry");effect(p,a("stew invisible",MobEffects.INVISIBILITY,-1,127));require(!layer(p,MobEffects.INVISIBILITY,127).isVisible(),"source infinite invisible hidden flag");require(game.items().currentEpoch((String)((Map<?,?>)read(game.items().erecto(),"rays")).get(p)),"adopted native ray current epoch");}
        void oracles(){for(var flag:List.of("golden_beetroot","golden_stew","golden_porkchop","netherite_apple","bedrock_apple","netherite_stew","transdimensional_fish","multiverse_fish")){var anchor=ANCHORS.stream().filter(a->a.flag().equals(flag)).findFirst().orElseThrow();reset();String duration=anchor.ticks()<0?"infinite":Integer.toString(anchor.effect().value().isInstantaneous()?anchor.ticks():anchor.ticks()/20);command("effect give @s "+BuiltInRegistries.MOB_EFFECT.getKey(anchor.effect().value())+" "+duration+" "+anchor.amplifier());var control=effect(p,anchor);int ticks=control.getDuration(),amplifier=control.getAmplifier();reset();eat(flag);var actual=effect(p,anchor);require(actual.getDuration()==ticks&&actual.getAmplifier()==amplifier,"independent native command duration oracle "+flag);}}
        void bananas(){for(boolean hungry:new boolean[]{true,false}){reset();var health=p.getAttribute(Attributes.MAX_HEALTH);health.removeModifiers();health.setBaseValue(20);p.setHealth(hungry?10.25f:20);try(var draw=new Draws(level,.99f)){eat("eat_banana");require(draw.count==2,"two independent native banana chance tests");}effect(p,hungry?a("banana heal",MobEffects.INSTANT_HEALTH,1,1):a("banana strength",MobEffects.STRENGTH,100,1));require(hungry?!p.hasEffect(MobEffects.STRENGTH):!p.hasEffect(MobEffects.INSTANT_HEALTH),"banana early-return branch excludes other consequence");require(drops().isEmpty(),"false source chances do not invent drops");}}
        void swap(){reset();var excluded=new LinkedHashMap<ServerPlayer,State>();try{for(var player:game.server().getPlayerList().getPlayers())if(player!=p&&player!=other){excluded.put(player,new State(game,player));player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);}require(game.server().getPlayerList().getPlayers().stream().filter(q->q!=p&&!q.isSpectator()).count()==1,"exclusive actual random selector target");var nether=game.server().getLevel(Level.NETHER);require(nether!=null&&other.teleportTo(nether,origin.x+11,origin.y,origin.z+7,Set.of(),61,-17,true),"actual target in different native dimension");otherClient.ack();var beforeP=p.position();var beforeOther=other.position();var velocityP=new Vec3(.12,.07,-.08);var velocityOther=new Vec3(-.04,.09,.11);p.setDeltaMovement(velocityP);other.setDeltaMovement(velocityOther);eat("human_flesh");client.ack();otherClient.ack();require(p.level()==nether&&other.level()==level&&p.position().distanceToSqr(beforeOther)<.0000001&&other.position().distanceToSqr(beforeP)<.0000001,"native global selector swaps explicit cross-dimension pair");near(p.getYRot(),61,"source selected-player yaw");near(p.getXRot(),-17,"source selected-player pitch");near(other.getYRot(),0,"source marker default yaw");near(other.getXRot(),0,"source marker default pitch");require(p.getDeltaMovement().distanceToSqr(velocityP)<.0000001&&other.getDeltaMovement().distanceToSqr(velocityOther)<.0000001,"shared native transfer preserves each own velocity");effect(p,a("human_flesh",MobEffects.NAUSEA,20,1));require(game.items().currentEpoch(epoch),"same actual match epoch throughout bounded consume checks");}finally{excluded.values().forEach(state->state.restore(game));}}
        void admission(){reset();for(var role:List.of("spectator","strict_map_object","rcdenyalways")){p.entityTags().add(role);try{game.items().namedFoods().consumed(p,source("golden_beetroot"));require(!p.hasEffect(MobEffects.INSTANT_HEALTH),"completed source callback rejects role "+role);}finally{p.entityTags().remove(role);}}game.items().timeStop().begin(level,20);try{game.items().namedFoods().consumed(p,source("golden_beetroot"));require(!p.hasEffect(MobEffects.INSTANT_HEALTH),"completed source callback pauses under real Time Stop");}finally{game.items().timeStop().end(level);}}
        public void close(){if(closed)return;closed=true;try{for(var player:List.of(p,other)){game.items().clear(player);game.items().blind().forget(player);require(game.items().blind().queued(player)==0,"fixture Blind adoption queue removed before native ticks");player.removeAllEffects();for(var name:RAW)game.items().mana().score(player,name).set(0);player.entityTags().removeAll(List.of("have_lucky_effects","have_lucky_effects_5t","have_lucky_effects_1s"));}removeOutputs();}finally{try{saved.restore(game);otherSaved.restore(game);client.ack();otherClient.ack();attribution.close();var objective=game.server().getScoreboard().getObjective("lbc.math");if(objective!=null)math.forEach((name,value)->{var holder=ScoreHolder.forNameOnly(name);if(value==null)game.server().getScoreboard().resetSinglePlayerScore(holder,objective);else game.server().getScoreboard().getOrCreatePlayerScore(holder,objective,true).set(value);});}finally{client.close();otherClient.close();}}}
    }
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.profiles();fixture.oracles();fixture.bananas();fixture.swap();fixture.admission();}SkyWars2.LOGGER.info("[check] PASS named-food (actual40 source carriers/native one debit,39 source effect anchors/hidden layers,8 native duration oracles, banana exclusive branches, raw29/adopted native owners, real cross-dimension random player swap/velocity/roles/TimeStop/current epoch; synchronous cleanup before long consequences; broad descendant release remains gated)");}
}
