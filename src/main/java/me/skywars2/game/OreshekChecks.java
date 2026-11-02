package me.skywars2.game;

import me.skywars2.SkyWars2;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.Vec3;

/** Bounded native nut consumption and actual shared47 producer; no ticking dangerous counters. */
public final class OreshekChecks {
    private OreshekChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Oreshek: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object replacement){try{field(value,name).set(value,replacement);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static final class State {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final boolean ground,sprint,shift;final Vec3 known;final net.minecraft.world.entity.Pose pose;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> values=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        State(GameController game,ServerPlayer p){player=p;require(!p.isUsingItem(),"fixture starts with idle hands");state=new GameChecks.WandThirdPlayerState(p,game.items().mana());ground=p.onGround();sprint=p.isSprinting();shift=p.isShiftKeyDown();known=p.getKnownMovement();pose=p.getPose();
            for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=Stats.CUSTOM.get(id);stats.put(s,p.getStats().getValue(s));}for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(LbcOreshek.ITEM,Items.REPEATING_COMMAND_BLOCK,Items.GOLDEN_APPLE,Items.APPLE,Items.IRON_HELMET,Items.SHIELD,Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET)){var s=type.get(item);stats.put(s,p.getStats().getValue(s));}
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(score!=null)scores.put(objective,score.value());}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var name:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource","lastRecordedFoodLevel","lastSentFood","lastFoodSaturationZero","lastSentHealth","lastRecordedHealthAndAbsorption"))values.put(field(p,name),read(p,name));
            try{for(var f:p.getCombatTracker().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);var v=f.get(p.getCombatTracker());if(v instanceof List<?> list)combat.put(f,new ArrayList<>(list));else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers()))combat.put(f,v);}for(var f:p.getFoodData().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(p.getFoodData()));}for(var f:game.players().get(p.getUUID()).getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(game.players().get(p.getUUID()));profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> s?new HashSet<>(s):v);}for(var entry:((Map<?,?>)read(p,"lastEquipmentItems")).entrySet())equipment.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings({"rawtypes","unchecked"}) void restore(GameController game){state.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(state.health);player.setAbsorptionAmount(state.absorption);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setKnownMovement(known);player.setPose(pose);stats.forEach((s,v)->player.getStats().setValue(player,s,v));for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(player,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(player,o);advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            try{for(var e:values.entrySet())e.getKey().set(player,e.getValue());for(var e:combat.entrySet())if(e.getValue() instanceof List old){var current=(List)e.getKey().get(player.getCombatTracker());current.clear();current.addAll(old);}else e.getKey().set(player.getCombatTracker(),e.getValue());for(var e:food.entrySet())e.getKey().set(player.getFoodData(),e.getValue());for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());var equipmentMap=(Map<EquipmentSlot,ItemStack>)read(player,"lastEquipmentItems");equipmentMap.clear();equipment.forEach((s,i)->equipmentMap.put(s,i.copy()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
    }

    /** Replace only the explicit source branch draws, delegating all unrelated native random. */
    static final class Choices implements AutoCloseable {
        final ServerLevel level;final Object random;final ArrayDeque<Float> floats=new ArrayDeque<>();boolean debuff;
        Choices(ServerLevel level,float... values){this.level=level;random=read(level,"random");for(float v:values)floats.add(v);debuff=true;
            set(level,"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(p,m,args)->{
                if(m.getName().equals("nextInt")&&args!=null&&args.length==1){int bound=(Integer)args[0];
                    if(bound==47&&debuff&&callerIs(LbcLegacyEffects.class,"randomDebuff")){debuff=false;return 2;} // harmless visible Glowing selection3
                    if(bound==9601&&callerIs(LbcHedgehogDimensions.class,"transferTo"))return 4800; // source Nexus coordinates0,2332,0
                }
                if(m.getName().equals("nextFloat")&&callerIs(LbcOreshek.class,"consumed")){
                    require(!floats.isEmpty(),"unexpected source rare-branch draw");return floats.remove();}
                try{return m.invoke(random,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));
        }
        /** The first real caller after this handler/proxy, never a deeper ancestor such as consumed->explode. */
        private static boolean callerIs(Class<?> type,String method){return StackWalker.getInstance().walk(frames->frames
                .dropWhile(f->f.getClassName().equals(Choices.class.getName())||f.getClassName().startsWith("jdk.proxy")
                        ||f.getClassName().startsWith("com.sun.proxy.")||f.getClassName().startsWith("java.lang.reflect.")
                        ||f.getClassName().startsWith("jdk.internal.reflect."))
                .findFirst().map(f->f.getClassName().equals(type.getName())&&f.getMethodName().equals(method)).orElse(false));}
        public void close(){set(level,"random",random);require(floats.isEmpty()&&!debuff,"all and only source branch draws consumed");}
    }
    /** Snapshot existing mutable service registries; isolate this participant before adoption mutates a state object. */
    private static final class Services implements AutoCloseable {
        record Saved(Object service,java.lang.reflect.Field field,Object value){}
        final List<Saved> saved=new ArrayList<>();final List<Runnable> attachments=new ArrayList<>();
        @SuppressWarnings({"rawtypes","unchecked"}) Services(ServerPlayer p,Object... services){try{for(var service:services)for(var f:service.getClass().getDeclaredFields()){
            f.setAccessible(true);var value=f.get(service);
            if(java.lang.reflect.Modifier.isStatic(f.getModifiers())){
                if(value instanceof net.fabricmc.fabric.api.attachment.v1.AttachmentType type){var previous=p.getAttached(type);var old=previous instanceof net.minecraft.nbt.CompoundTag tag?tag.copy():previous;
                    attachments.add(()->{if(old==null)p.removeAttached(type);else p.setAttached(type,old);});}continue;}
            if(value instanceof Map map){if(value.getClass().getName().contains("ImmutableCollections"))continue;
                saved.add(new Saved(service,f,new LinkedHashMap(map)));map.remove(p);map.remove(p.getUUID());
            }else if(value instanceof Collection collection){if(value.getClass().getName().contains("ImmutableCollections"))continue;
                saved.add(new Saved(service,f,new ArrayList(collection)));collection.removeIf(x->x==p||Objects.equals(x,p.getUUID()));
            }else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers())&&(f.getType().isPrimitive()||value instanceof Enum<?>))saved.add(new Saved(service,f,value));
        }}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
        @SuppressWarnings({"rawtypes","unchecked"}) public void close(){try{for(var s:saved){var current=s.field.get(s.service);
            if(current instanceof Map map){map.clear();map.putAll((Map)s.value);}else if(current instanceof Collection collection){collection.clear();collection.addAll((Collection)s.value);}else s.field.set(s.service,s.value);
        }for(var restore:attachments)restore.run();}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    }
    private static final List<Holder<MobEffect>> EFFECTS=List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.GLOWING,MobEffects.HUNGER,MobEffects.INFESTED,MobEffects.INSTANT_DAMAGE,MobEffects.LEVITATION,MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.OOZING,MobEffects.POISON,MobEffects.SLOWNESS,MobEffects.UNLUCK,MobEffects.WEAKNESS,MobEffects.WEAVING,MobEffects.WIND_CHARGED,MobEffects.WITHER);
    private static final List<String> COUNTERS=List.of("cold","cold_cd","delayed_death","effect_big_explosion","effect_evoker","effect_evokerarrow","effect_explosion","effect_firebreak","effect_gravity_curse","effect_hedgehog_boom","effect_kiuaskivi","effect_lightarrow","effect_lightning_bolt","effect_nuking","effect_remove_mana","effect_small_explosion","effect_snus","effect_stand_arrow_hit","effect_traps","effect_witch_gens_reset","frozen","gigaponos","lookback","nomagic","piss","scared","shock","skill_issue","target","twitching");
    private static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;final GameController game;final ServerPlayer p;final State state;final Services services;
        final ServerLevel level;final Vec3 high;final Integer priorFake;final net.minecraft.world.scores.Objective math;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final ItemStack nut;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);p=base.owner;state=new State(game,p);level=p.level();
            high=new Vec3(p.getX(),level.getMaxY()+256,p.getZ());
            require(level.hasChunkAt(BlockPos.containing(high)),"already resident high launch column");
            require(high.y-32>level.getMaxY(),"entire native15 blast cube above terrain build volume");
            for(var e:level.getAllEntities()){before.add(e);require(e==p||e.position().distanceToSqr(high)>1024,"no neighboring entity within native blast diameter");}
            require(game.items().nukeEvent().snapshot().isEmpty(),"no existing nuke event");
            math=game.server().getScoreboard().getObjective("lbc.math");var fake=math==null?null:game.server().getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("randebuff"),math);priorFake=fake==null?null:fake.value();
            var i=game.items();services=new Services(p,i,i.mana(),i.oreshek(),i.legacyEffects(),i.frost(),i.acidCorruption(),i.babylonEffects(),i.quiver(),i.kiuas(),i.kfcFood(),i.psycho(),i.magic(),i.electro(),i.blind(),i.blaze(),i.awakenedAcid(),i.erecto(),i.foodStatuses(),i.homing(),i.indirectKills());
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getInventory().clearContent();p.removeAllEffects();
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setNoGravity(true);p.teleportTo(level,high.x,high.y,high.z,Set.of(),0,0,true);acknowledge();p.setInvulnerableTime(0);p.damageCooldownTime=0;
            game.players().get(p.getUUID()).lbcSkill=0;reset();
            var pool=game.lbc().rollTable(level,high,p,"lbc:oreshki_10");nut=pool.stream().filter(LbcOreshek::matches).findFirst().orElseThrow(()->new IllegalStateException("actual source nut leaf missing"));
            require(nut.is(LbcOreshek.ITEM)&&nut.getCount()==1&&nut.has(DataComponents.CONSUMABLE)&&nut.has(DataComponents.FOOD),"actual registered public nut identity/rawflag/food/consumable/single count");
        }
        void acknowledge(){p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(read(p.connection,"awaitingPositionFromClient")!=null){ClientboundPlayerPositionPacket last=null;for(var packet:base.recording.packets)if(packet instanceof ClientboundPlayerPositionPacket update)last=update;require(last!=null,"native dimension position packet");p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(last.id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));}require(!p.isChangingDimension(),"native client acknowledges dimension");}
        void reset(){game.items().clear(p);for(var key:COUNTERS)game.items().mana().score(p,key).set(0);p.removeAllEffects();p.entityTags().remove("skywars2.oreshek.returned");p.setHealth(1000);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.setPos(high);}
        void eat(){var stack=nut.copy();p.setItemInHand(InteractionHand.MAIN_HAND,stack);var out=stack.finishUsingItem(p.level(),p);require(out.isEmpty(),"native finished nut consumes exactly its one item");}
        void branches(){var service=game.items().oreshek();long used=service.consumedCount();
            try(var choices=new Choices(level,.5f,.5f,.5f)){eat();}
            require(service.consumedCount()==used+1&&service.lastOutcome()==LbcOreshek.Outcome.NONE&&!service.pendingDeathMessage(p)&&p.hasEffect(MobEffects.GLOWING),"actual fallthrough runs native debuff then revokes return latch");
            reset();try(var choices=new Choices(level,0)){eat();}
            require(service.lastOutcome()==LbcOreshek.Outcome.EXPLOSION&&!service.pendingDeathMessage(p)&&service.explosionCount()>0&&p.getHealth()<1000&&p.isAlive(),"actual native15 survivor retains no deferred death announcement");
            long beforeAgain=service.consumedCount();try(var choices=new Choices(level,0)){eat();}require(service.consumedCount()==beforeAgain+1&&!service.pendingDeathMessage(p),"second actual consumed nut still runs without a persistent death-message latch");
            int messages=base.recording.packets.size();p.entityTags().add("skywars2.oreshek.returned");
            require(!service.resetBoundary(p)&&!service.pendingDeathMessage(p)&&!p.entityTags().contains("skywars2.oreshek.returned")&&!service.resetBoundary(p),"reset only clears obsolete persistent marker and never announces a survivor death");
            require(base.recording.packets.subList(messages,base.recording.packets.size()).stream().noneMatch(packet->packet instanceof ClientboundSystemChatPacket c&&c.content().getString().equals("\uE202")),"survivor reset emits no global U+E202 packet");
            reset();try(var choices=new Choices(level,.5f,0)){eat();}
            require(service.lastOutcome()==LbcOreshek.Outcome.NEXUS&&!service.pendingDeathMessage(p)&&p.level().dimension().equals(LbcHedgehogDimensions.NEXUS)&&p.getX()==0&&p.getY()==2332&&p.getZ()==0,"actual native Nexus early-return transfer at exact source random boundaries");
            p.teleportTo(level,high.x,high.y,high.z,Set.of(),0,0,true);acknowledge();reset();
        }
        void producer(){var service=game.items().legacyEffects();
            for(int n=1;n<=47;n++){reset();if(n>=18)game.items().mana().score(p,COUNTERS.get(n-18)).set(7);service.applyDebuff(p,n);
                require(service.lastSelection()==n&&p.entityTags().containsAll(List.of("have_lucky_effects","have_lucky_effects_1s","have_lucky_effects_5t")),"actual producer literal selection and all3source tags "+n);
                if(n<=17){var effect=p.getEffect(EFFECTS.get(n-1));require(effect!=null&&effect.getDuration()==(n==6?1:600)&&effect.getAmplifier()==0&&effect.isVisible(),"independent exact vanilla source grant "+n);}
                else{String key=COUNTERS.get(n-18);require(game.items().mana().score(p,key).get()==37,"actual source add30 preserves prior7 "+key);adopted(key);}
            }reset();
            for(String tag:List.of("spectator","strict_map_object","magic_academy","rcdenyalways","time_stopped")){long before=game.items().oreshek().consumedCount();p.entityTags().add(tag);try{eat();require(game.items().oreshek().consumedCount()==before,"actual native consume script role/TimeStop exclusion "+tag);}finally{p.entityTags().remove(tag);}}
            game.items().timeStop().begin(level,40);try{long before=game.items().oreshek().consumedCount();eat();require(game.items().oreshek().consumedCount()==before,"actual native dimension TimeStop consume exclusion");}finally{game.items().timeStop().end(level);}
        }
        void adopted(String key){var i=game.items();switch(key){
            case "effect_small_explosion"->require(i.babylonEffects().queued(p,101)==37,"actual shared explosive queue adoption");
            case "effect_lightning_bolt"->require(i.babylonEffects().queued(p,109)==37,"actual shared lightning queue adoption");
            case "effect_firebreak"->require(i.babylonEffects().queued(p,110)==37,"actual shared flame queue adoption");
            case "effect_evoker"->require(i.quiver().queued(p,102)==37,"actual shared evoker queue adoption");
            case "effect_lightarrow"->require(i.quiver().queued(p,112)==37,"actual shared lightarrow queue adoption");
            case "effect_evokerarrow"->require(i.quiver().queued(p,113)==37,"actual shared evokerarrow queue adoption");
            case "cold","frozen"->require(((Map<?,?>)read(i.frost(),"affected")).containsKey(p),"actual native Frost owner adopted "+key);
            case "cold_cd"->require(i.mana().hudValue(p,"cold")==0&&!((Map<?,?>)read(i.frost(),"affected")).containsKey(p),"source bare cold_cd stays dormant without cold; raw37 remains authoritative");
            case "delayed_death"->require(((Map<?,?>)read(i.acidCorruption(),"deaths")).containsKey(p),"actual shared delayed death WILD owner");
            case "lookback","piss","scared","twitching"->require(((Map<?,?>)read(i.psycho(),"effects")).containsKey(p),"actual shared Psycho owner adopted "+key);
            case "effect_kiuaskivi"->require(((Map<?,?>)read(i.kiuas(),"cursed")).containsKey(p),"actual Kiuas terrain owner adopted");
            case "gigaponos"->require(i.kfcFood().gigaponosRemaining(p)==37,"existing native KFC owner adopted");
            case "shock"->require(((Map<?,?>)read(i.electro(),"shocks")).containsKey(p),"actual Electro pulse owner adopted");
            case "skill_issue"->require(i.curseTicks(p)==37,"actual existing turn curse adopted");
            default->{} // Generic scores are the actual consumer state; dangerous actions deliberately do not tick.
        }}
        private static void cleanup(Throwable[] failure,Runnable action){try{action.run();}catch(Throwable error){if(failure[0]==null)failure[0]=error;else failure[0].addSuppressed(error);}}
        public void close(){var failure=new Throwable[1];
            cleanup(failure,()->{if(p.level()!=level){p.teleportTo(level,high.x,high.y,high.z,Set.of(),0,0,true);acknowledge();}});
            cleanup(failure,()->game.items().clear(p));
            cleanup(failure,()->{var created=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity))created.add(entity);
                for(var entity:created){game.items().entityUnloaded(entity);entity.discard();}});
            // Run the borrowed fixture's clearing BEFORE restoring saved registries, so its stop calls cannot erase them.
            cleanup(failure,base::close);
            cleanup(failure,services::close);
            cleanup(failure,()->state.restore(game));
            cleanup(failure,()->{if(math!=null){var holder=net.minecraft.world.scores.ScoreHolder.forNameOnly("randebuff");
                if(priorFake==null)game.server().getScoreboard().resetSinglePlayerScore(holder,math);
                else game.server().getScoreboard().getOrCreatePlayerScore(holder,math,true).set(priorFake);}});
            cleanup(failure,()->p.connection.resetPosition());
            if(failure[0] instanceof Error error)throw error;
            if(failure[0]!=null)throw new IllegalStateException("Oreshek fixture cleanup",failure[0]);
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.branches();f.producer();}SkyWars2.LOGGER.info("[check] PASS oreshek (actual nut consumption, native15 isolated survivor blast, Nexus, silent obsolete-latch reset,47producer; no Zelensky/nuke cascade qualification)");}
}
