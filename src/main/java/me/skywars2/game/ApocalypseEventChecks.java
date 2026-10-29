package me.skywars2.game;

import me.skywars2.SkyWars2;
import java.util.*;
import com.mojang.brigadier.StringReader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real throws, bounded real ticks and selected actual phase actions; no full apocalypse cascade. */
public final class ApocalypseEventChecks {
    private ApocalypseEventChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Apocalypse events: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static boolean key(Component c,String value){if(c.getContents() instanceof TranslatableContents t&&t.getKey().equals(value))return true;return c.getSiblings().stream().anyMatch(s->key(s,value));}
    private static final class State {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final boolean ground,sprint,shift;final Vec3 known;final net.minecraft.world.entity.Pose pose;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> values=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        State(GameController game,ServerPlayer p){player=p;require(!p.isUsingItem(),"fixture starts with idle hands");state=new GameChecks.WandThirdPlayerState(p,game.items().mana());ground=p.onGround();sprint=p.isSprinting();shift=p.isShiftKeyDown();known=p.getKnownMovement();pose=p.getPose();
            for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=Stats.CUSTOM.get(id);stats.put(s,p.getStats().getValue(s));}for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.SNOWBALL,Items.REPEATING_COMMAND_BLOCK,Items.GOLDEN_APPLE,Items.APPLE,Items.IRON_HELMET,Items.SHIELD,Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET)){var s=type.get(item);stats.put(s,p.getStats().getValue(s));}
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(score!=null)scores.put(objective,score.value());}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var name:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource","lastRecordedFoodLevel","lastSentFood","lastFoodSaturationZero","lastSentHealth","lastRecordedHealthAndAbsorption"))values.put(field(p,name),read(p,name));
            try{for(var f:p.getCombatTracker().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);var v=f.get(p.getCombatTracker());if(v instanceof List<?> list)combat.put(f,new ArrayList<>(list));else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers()))combat.put(f,v);}for(var f:p.getFoodData().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(p.getFoodData()));}for(var f:game.players().get(p.getUUID()).getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(game.players().get(p.getUUID()));profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> s?new HashSet<>(s):v);}for(var entry:((Map<?,?>)read(p,"lastEquipmentItems")).entrySet())equipment.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings({"rawtypes","unchecked"}) void restore(GameController game){state.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(state.health);player.setAbsorptionAmount(state.absorption);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setKnownMovement(known);player.setPose(pose);stats.forEach((s,v)->player.getStats().setValue(player,s,v));for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(player,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(player,o);advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            try{for(var e:values.entrySet())e.getKey().set(player,e.getValue());for(var e:combat.entrySet())if(e.getValue() instanceof List old){var current=(List)e.getKey().get(player.getCombatTracker());current.clear();current.addAll(old);}else e.getKey().set(player.getCombatTracker(),e.getValue());for(var e:food.entrySet())e.getKey().set(player.getFoodData(),e.getValue());for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());var equipmentMap=(Map<EquipmentSlot,ItemStack>)read(player,"lastEquipmentItems");equipmentMap.clear();equipment.forEach((s,i)->equipmentMap.put(s,i.copy()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
    }

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

    private record Cell(ServerLevel level,BlockPos at){}
    private record EntityState(Set<String> tags,Vec3 at,Vec3 motion){}
    private static final class Fixture implements AutoCloseable {
        final GameController game;ElementalGrimoireChecks.Fixture base;ServerPlayer owner;ServerLevel level;
        LbcApocalypseEvents events;Services services;final List<State> participants=new ArrayList<>();
        final Map<Entity,EntityState> borrowed=new IdentityHashMap<>();final Map<Cell,MapManager.BlockSnapshot> frame=new LinkedHashMap<>();final Set<Cell> writes=new HashSet<>();
        final List<Entity> created=new ArrayList<>();final ItemStack[] source=new ItemStack[3];
        Collection<LbcStonekivi.Work> queue;List<LbcStonekivi.Work> originalQueue;Vec3 at;Marker look;Entity lastBall;
        boolean closed,ownsStop;int stage,started,pausedAt;long naturalPhase;List<LbcApocalypseEvents.PendingView> pausedPending;
        Fixture(GameController game){this.game=game;}
        @SuppressWarnings("unchecked") void initialize(){
            events=game.items().apocalypseEvents();require(game.playing()&&events.prepared()&&events.pendingSnapshot().isEmpty()&&events.snapshot().isEmpty()&&events.terrainCount()==0,"prepared exclusive empty event service");
            require(game.items().apocalypseActors().count()==0&&game.items().shishEncounter().actorCount()==0&&game.items().chaosTrapActors().trackedCount()==0,"exclusive empty actor/forest services");
            require(game.items().nukeEvent().markerCount()==0&&game.items().nukeEvent().jobCount()==0&&game.items().nukeEvent().pendingTicks()==-1&&game.items().nukeEvent().pointPendingTicks()==-1,"exclusive nuclear service");
            require(!game.items().timeStop().stopped(game.maps().level()),"no borrowed TimeStop");
            queue=(Collection<LbcStonekivi.Work>)read(game.items().stonekivi(),"work");originalQueue=new ArrayList<>(queue);require(originalQueue.isEmpty(),"exclusive native terrain work queue");
            base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;owner=base.owner;level=owner.level();
            for(var p:game.server().getPlayerList().getPlayers())participants.add(new State(game,p));
            for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())borrowed.put(e,new EntityState(Set.copyOf(e.entityTags()),e.position(),e.getDeltaMovement()));
            var i=game.items();services=new Services(owner,i,events,i.apocalypseActors(),i.shishEncounter(),i.chaosTrapActors(),i.nukeEvent(),i.spellSources(),i.indirectKills(),i.legacyEffects(),i.reactive(),i.magic(),i.potionSpawnerDrops(),i.stonekivi());
            base.initialize();acknowledge();game.players().get(owner.getUUID()).lbcSkill=0;at=owner.position();owner.removeAllEffects();owner.getInventory().clearContent();
            for(var p:game.server().getPlayerList().getPlayers())if(p!=owner)p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            for(var e:borrowed.keySet())if(e!=owner)e.entityTags().add("spectator");
            for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())if(e.entityTags().contains("swrg.look"))capture(world,e.blockPosition().offset(-4,-4,-4),e.blockPosition().offset(4,4,4));
            capture(level,owner.blockPosition().offset(-8,-3,-8),new BlockPos(owner.blockPosition().getX()+8,319,owner.blockPosition().getZ()+8));
            look=new Marker(EntityTypes.MARKER,level);look.setPos(at.add(4,0,0));look.entityTags().addAll(List.of("swrg.look","spectator"));require(level.addFreshEntity(look),"actual native scoped source center marker");created.add(look);
            level.setBlock(look.blockPosition(),Blocks.STONE.defaultBlockState(),2);
            payloads();sync();
            clearActors();base.recording.packets.clear();throwItem(source[0]);drain();
            var first=events.pendingSnapshot();require(first.size()==1&&first.getFirst().kind()==LbcApocalypseEvents.Kind.END&&first.getFirst().phase()==2&&first.getFirst().ticks()==2,"actual native throw schedules sourcephase2 two ticks");
            long starts=events.startCount();throwItem(source[0]);require(events.startCount()==starts+1&&events.pendingSnapshot().size()==1&&events.pendingSnapshot().getFirst().ticks()==2,"same native functionID REPLACE reentry creates onephase2 deadline");drain();
            naturalPhase=events.phaseCount();pausedPending=events.pendingSnapshot();game.items().timeStop().begin(level,40);ownsStop=true;pausedAt=game.server().getTickCount();stage=0;started=pausedAt;
        }
        void capture(ServerLevel world,BlockPos from,BlockPos to){for(var p:BlockPos.betweenClosed(from,to)){require(world.hasChunkAt(p),"resident complete bounded frame "+p);var cell=new Cell(world,p.immutable());frame.putIfAbsent(cell,game.maps().snapshot(world,p));}}
        void ownCube(ServerLevel world,BlockPos center,int r){for(var p:BlockPos.betweenClosed(center.offset(-r,-r,-r),center.offset(r,r,r)))writes.add(new Cell(world,p.immutable()));}
        void acknowledge(){owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(read(owner.connection,"awaitingPositionFromClient")!=null){ClientboundPlayerPositionPacket last=null;for(var packet:base.recording.packets)if(packet instanceof ClientboundPlayerPositionPacket update)last=update;require(last!=null,"native teleport packet");owner.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(last.id(),owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot()));}require(!owner.isChangingDimension(),"native loaded participant");}
        void payloads(){for(int attempt=0;attempt<256&&(source[0]==null||source[1]==null);attempt++)for(var item:game.lbc().rollTable(level,at,owner,"lbc:end_of_everything")){
                var kind=LbcApocalypseEvents.kind(item);if(kind==LbcApocalypseEvents.Kind.END)source[0]=item.copy();if(kind==LbcApocalypseEvents.Kind.BAFFLED)source[1]=item.copy();}
            var result=game.lbc().rollTable(level,at,owner,"lbc:end_of_technomagic");require(result.size()==1,"actual singleton Shish source table");source[2]=result.getFirst().copy();
            for(int n=0;n<3;n++)require(source[n]!=null&&source[n].is(Items.SNOWBALL)&&source[n].getCount()==1&&LbcFartus.data(source[n]).getIntOr("gadget",0)==1,"actual original singleton snowball source output "+n);
        }
        void throwItem(ItemStack original){var stack=original.copy();stack.setCount(2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);int used=owner.getStats().getValue(Stats.ITEM_USED.get(Items.SNOWBALL));
            require(owner.gameMode.useItem(owner,level,stack,InteractionHand.MAIN_HAND).consumesAction(),"actual native snowball use");require(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount()==1&&owner.getStats().getValue(Stats.ITEM_USED.get(Items.SNOWBALL))==used+1,"native snowball two->one debit and single stat");
        }
        void drain(){for(int n=0;n<1024&&!queue.isEmpty();n++)game.items().stonekivi().tick();require(queue.isEmpty(),"bounded actual terrain worker drained without advancing event clocks");}
        void actualPhase(LbcApocalypseEvents.Kind kind,int phase){try{var method=LbcApocalypseEvents.class.getDeclaredMethod("phase",LbcApocalypseEvents.Authority.class,LbcApocalypseEvents.Kind.class,int.class,net.minecraft.commands.CommandSourceStack.class);method.setAccessible(true);
            var authority=new LbcApocalypseEvents.Authority(owner.getUUID(),game.items().indirectKills().wildCause(owner),game.items().epoch());
            method.invoke(events,authority,kind,phase,game.server().createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS).withSuppressedOutput());
        }catch(java.lang.reflect.InvocationTargetException e){if(e.getCause() instanceof RuntimeException error)throw error;throw new IllegalStateException(e.getCause());}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        void sync(){
            for(int n=0;n<3;n++){clearActors();owner.removeAllEffects();base.recording.packets.clear();long count=events.startCount();throwItem(source[n]);
                require(events.startCount()==count+1&&events.pendingSnapshot().size()==1,"actual source event flag starts once "+n);
                require(events.pendingSnapshot().getFirst().phase()==2&&events.pendingSnapshot().getFirst().ticks()==(n==0?2:n==1?80:100),"exact source first next-phase delay "+n);
                if(n==0){require(base.recording.packets.stream().anyMatch(packet->packet instanceof ClientboundLevelParticlesPacket dot&&dot.count()==1000&&dot.x()==0&&dot.y()==64&&dot.z()==0),"source activation particle uses positioned0,64,0,notthrowlocation");
                    for(var e:borrowed.keySet())if(e.entityTags().contains("swrg.look"))ownCube((ServerLevel)e.level(),e.blockPosition(),3);ownCube(level,look.blockPosition(),3);drain();
                    require(level.getBlockState(look.blockPosition()).isAir(),"real source center native first-phase air fill");
                }else if(n==1){var speed=owner.getEffect(net.minecraft.world.effect.MobEffects.SPEED);require(speed!=null&&speed.getAmplifier()==15&&speed.getDuration()==4000,"actual Baffled source allmob Snus engine grants");}
                else require(game.items().chaosTrapActors().snapshot().stream().anyMatch(e->e.entityTags().contains("forest")),"actual Shish starts shared native800tick forest");
            }
            clearActors();var mixed=source[0].copy();var data=LbcFartus.data(mixed).copy();data.putInt("baffled_of_everything",1);data.putInt("shish_of_everything",1);mixed.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
            base.recording.packets.clear();long start=events.startCount();throwItem(mixed);require(events.startCount()==start+3&&events.pendingSnapshot().size()==3,"one native mixed-flag snowball starts exactly3independent source branches");
            var texts=base.recording.packets.stream().filter(ClientboundSetSubtitleTextPacket.class::isInstance).map(ClientboundSetSubtitleTextPacket.class::cast).map(ClientboundSetSubtitleTextPacket::text).toList();
            require(texts.size()==3&&key(texts.get(0),"the_salt")&&texts.get(1).getString().equals("Baffled")&&texts.get(2).getString().equals("Shish"),"actual three source subtitle outputs retain branch order");
            var authorities=events.pendingSnapshot().stream().map(LbcApocalypseEvents.PendingView::authority).distinct().toList();require(authorities.size()==1&&authorities.getFirst().author().equals(owner.getUUID())&&authorities.getFirst().cause().friendlyFire()&&authorities.getFirst().epoch().equals(game.items().epoch()),"all mixed siblings share one accepted WILD epoch authority");
            clearActors();selected();clearActors();
        }
        void selected(){
            // Direct production branch calls test real payloads, not elapsed77phase scheduling.
            owner.getInventory().clearContent();actualPhase(LbcApocalypseEvents.Kind.BAFFLED,8);
            var luck=owner.getEffect(net.minecraft.world.effect.MobEffects.LUCK);require(luck!=null&&luck.isInfiniteDuration()&&luck.getAmplifier()==4,"actual Baffled lastphase infiniteLuck4");
            int red=0,brown=0,stem=0;for(int slot=0;slot<owner.getInventory().getContainerSize();slot++){var item=owner.getInventory().getItem(slot);if(item.is(Items.RED_MUSHROOM_BLOCK))red+=item.getCount();if(item.is(Items.BROWN_MUSHROOM_BLOCK))brown+=item.getCount();if(item.is(Items.MUSHROOM_STEM))stem+=item.getCount();}
            require(red==64&&brown==64&&stem==64,"actual lastphase gives original64normal/Neko/Loli mushroom carriers");
            require(!events.pendingSnapshot().stream().anyMatch(v->v.kind()==LbcApocalypseEvents.Kind.BAFFLED),"source lastphase does not inventnextschedule");
            owner.getInventory().clearContent();owner.removeAllEffects();
            var defaultContext=game.server().createCommandSourceStack();var border=defaultContext.getLevel().getWorldBorder();double x=border.getCenterX(),z=border.getCenterZ(),size=border.getSize(),target=border.getLerpTarget();long remaining=border.getLerpTime();
            try{actualPhase(LbcApocalypseEvents.Kind.SHISH,3);require(border.getCenterX()==0&&border.getCenterZ()==0&&border.getSize()==382.75,"actual source server-context worldborder phase,notcachedthroworigin");}
            finally{border.setCenter(x,z);if(remaining>0)border.lerpSizeBetween(size,target,remaining,defaultContext.getLevel().getGameTime());else border.setSize(size);}
            clearActors();
            // Scope the source nonspectator selector with actual entity tags, preserving every borrowed tag in finally.
            owner.entityTags().add("spectator");var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);require(cow!=null,"native lastphase target");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(at.add(0,0,4));require(level.addFreshEntity(cow),"native selected source chest target");created.add(cow);
            var cell=new Cell(level,cow.blockPosition());writes.add(cell);
            try{actualPhase(LbcApocalypseEvents.Kind.END,58);drain();var snapshot=game.maps().snapshot(level,cow.blockPosition());
                require(level.getBlockState(cow.blockPosition()).is(Blocks.CHEST)&&snapshot.entity()!=null&&snapshot.entity().getStringOr("LootTable","").equals("lbc:lucky_items_1"),"actual finalphase native chest entity references full27draw source table");
            }finally{owner.entityTags().remove("spectator");cow.discard();}
            clearActors();
            var nuke=game.items().nukeEvent();var site=at.add(-3,0,0);var accepted=game.items().indirectKills().wildCause(owner);
            require(nuke.startEoe(owner,level,site,accepted)&&nuke.pointPendingTicks()==400&&nuke.pendingTicks()==-1,"actual EOE400timer is independent from bruh700timer");
            var center=BlockPos.containing(site);for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))writes.add(new Cell(level,pos.immutable()));
            for(int y=center.getY()+3;y<=319;y++)writes.add(new Cell(level,new BlockPos(center.getX(),y,center.getZ())));writes.add(new Cell(level,center.above()));writes.add(new Cell(level,center.above(2)));
            drain();for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))require(level.getBlockState(pos).is(Blocks.NETHERITE_BLOCK),"actual EOEsmall3x3nativebase");
            require(level.getBlockState(center.above()).is(Blocks.BEACON)&&level.getBlockState(center.above(2)).is(Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.RED)),"actual EOEbeacon/redpane nativepayload");
            require(nuke.markers().stream().anyMatch(m->m.entityTags().containsAll(List.of("nuke","nuke1","rc_1sec","marker_2t","mecha"))&&!m.entityTags().contains("nuke_bruh")),"actual ordinary EOEarmedmarker differsfrombruh");
            require(nuke.startEoe(null,level,site,accepted)&&nuke.pointPendingTicks()==400,"accepted offlinestyle author source replaces sameglobalEOEdeadline");drain();nuke.stop();
            require(nuke.pointPendingTicks()==-1&&nuke.jobCount()==0,"explicitstop cancels EOEbefore400deadline");neighbors();
        }
        void clearActors(){events.stop();game.items().apocalypseActors().stop();game.items().shishEncounter().stop();game.items().chaosTrapActors().stop();game.items().nukeEvent().stop();
            var remove=new ArrayList<Entity>();for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())if(!borrowed.containsKey(e)&&e!=look)remove.add(e);
            for(var e:remove){game.items().entityUnloaded(e);e.discard();}drain();}
        boolean step(){int now=game.server().getTickCount();require(now-started<80,"bounded native clock fixture timeout");
            if(stage==0){require(events.phaseCount()==naturalPhase&&events.pendingSnapshot().equals(pausedPending),"eight real TimeStop ticks preserve actualpending sourcefunction clock");
                if(now-pausedAt<8)return false;require(!events.begin(owner,LbcApocalypseEvents.Kind.END),"native TimeStop entry admission blocksnewactivation");
                game.items().timeStop().end(level);ownsStop=false;stage=1;return false;
            }
            if(events.phaseCount()==naturalPhase)return false;
            require(events.phaseCount()==naturalPhase+1&&events.pendingSnapshot().size()==1&&events.pendingSnapshot().getFirst().phase()==3,"actual native two-tick scheduled phase2 thenphase3 enqueue");
            // Reentry replaces phase2 only; the old laterphase3 remains pending.
            throwItem(source[0]);var all=events.pendingSnapshot();require(all.size()==2&&all.stream().anyMatch(v->v.phase()==2)&&all.stream().anyMatch(v->v.phase()==3),"native perfunctionREPLACE preserves existinglaterphase onreentry");
            clearActors();neighbors();return true;
        }
        void neighbors(){for(var e:frame.entrySet())if(!writes.contains(e.getKey()))require(game.maps().snapshot(e.getKey().level(),e.getKey().at()).equals(e.getValue()),"exact native neighboring cell outside allboundedsourcewrites "+e.getKey());}
        private static void cleanup(Throwable[] failure,Runnable action){try{action.run();}catch(Throwable error){if(failure[0]==null)failure[0]=error;else failure[0].addSuppressed(error);}}
        public void close(){if(closed)return;closed=true;var failure=new Throwable[1];
            cleanup(failure,()->{if(ownsStop)game.items().timeStop().end(level);ownsStop=false;});
            cleanup(failure,()->{if(services!=null)clearActors();});
            cleanup(failure,()->{for(var e:created){game.items().entityUnloaded(e);e.discard();}});
            cleanup(failure,()->{for(var entry:frame.entrySet())game.maps().restoreSnapshot(entry.getKey().level(),entry.getKey().at(),entry.getValue());});
            cleanup(failure,()->{if(base!=null)base.close();});cleanup(failure,()->{if(services!=null)services.close();});
            cleanup(failure,()->{for(var s:participants)s.restore(game);});
            cleanup(failure,()->{for(var e:borrowed.entrySet())if(!e.getKey().isRemoved()){e.getKey().entityTags().clear();e.getKey().entityTags().addAll(e.getValue().tags());e.getKey().setPos(e.getValue().at());e.getKey().setDeltaMovement(e.getValue().motion());}});
            cleanup(failure,()->{if(queue!=null){queue.clear();queue.addAll(originalQueue);}if(owner!=null)owner.connection.resetPosition();});
            if(failure[0] instanceof Error e)throw e;if(failure[0]!=null)throw new IllegalStateException("Apocalypse fixture cleanup",failure[0]);
        }
    }
    public static void run(GameController game){require(running==null,"one active native fixture");var f=new Fixture(game);try{f.initialize();running=f;}catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS apocalypse-events (native throws/3flags,sourcephaseclock/TimeStop/perfunctionREPLACE,selectedlastphasepayloads/EOE400; nofullcascade/341rewardqualification)");}}catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL apocalypse-events",error);}}
    public static void cancel(){if(running!=null){try{running.close();}finally{running=null;}}}
}
