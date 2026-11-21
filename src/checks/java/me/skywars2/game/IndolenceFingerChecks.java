package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Producer proof is scoped; rebirth uses actual participant deaths and replacement players. */
public final class IndolenceFingerChecks {
    private IndolenceFingerChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Indolence fingers: "+why);}
    private static void equipmentUpdate(LivingEntity player){try{var m=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");m.setAccessible(true);m.invoke(player);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static final class Producer implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final Set<String> tags;
        final List<Entity> own=new ArrayList<>();final Map<Entity,Vec3> positions=new IdentityHashMap<>();
        final Set<UUID> previous=new HashSet<>();int phase,start;boolean closed;
        Producer(GameController game){this.game=game;base=new ChefChecks.Fixture(game);tags=new HashSet<>(base.owner.entityTags());
            require(game.items().indolenceFingers().snapshot().isEmpty(),"no borrowed managed fingers");}
        void init(){base.init();base.owner.entityTags().add("sin_indolence");base.move(base.other,base.at.add(12,0,0));
            for(int i=0;i<24;i++){var e=new Marker(EntityTypes.MARKER,base.owner.level());e.setPos(base.at.add((i%6)-3,2,i/6-2));require(base.owner.level().addFreshEntity(e),"seed native non-mob source position");own.add(e);}
            start=game.server().getTickCount();}
        boolean step(){require(game.server().getTickCount()-start<60,"bounded producer fixture");
            if(phase==0){if(game.server().getTickCount()-start<2)return false;
                for(var world:game.server().getAllLevels())for(var e:world.getAllEntities()){previous.add(e.getUUID());if(!(e instanceof net.minecraft.world.entity.player.Player))positions.put(e,e.position());}
                var created=game.items().indolenceFingers().acquired(base.owner);require(created.size()==20,"source global random nonplayer limit20, no mob-only approximation");
                for(var mob:created){require(positions.entrySet().stream().anyMatch(entry->entry.getKey().level()==mob.level()&&entry.getValue().equals(mob.position())),"each actor begins at a real selected entity's native position");
                    require(mob.isPersistenceRequired()&&mob.isSilent()&&mob.getMainHandItem().is(Items.BOW)&&!mob.isNoAi(),"source persistent silent skeleton with native bow AI");
                    require(!game.items().magic().magical(mob),"finger remains physical after common team conversion");
                    for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                        var item=mob.getItemBySlot(slot);var eq=item.get(DataComponents.EQUIPPABLE);
                        require(item.has(DataComponents.UNBREAKABLE)&&eq!=null&&eq.slot()==slot&&eq.assetId().isPresent()
                                &&eq.assetId().get().identifier().toString().equals(slot==EquipmentSlot.FEET||slot==EquipmentSlot.LEGS?"lbc:finger":"lbc:void"),"source body armor assets and slots: "+slot);
                    }
                    var source=game.items().spellSources().source(mob);require(source!=null&&source.author().equals(base.owner.getUUID())&&source.cause().friendlyFire(),"shared physical actor provenance");
                    mob.setNoAi(true);mob.setNoGravity(true);
                }
                game.items().indolenceFingers().reward(base.owner);equipmentUpdate(base.owner);
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                    var item=base.owner.getItemBySlot(slot);var attrs=item.get(DataComponents.ATTRIBUTE_MODIFIERS);require(attrs!=null&&attrs.modifiers().size()==2,"exact two source reward modifiers");
                    require(attrs.modifiers().stream().allMatch(e->e.slot().test(slot)),"approved actual equipment modifier slot: "+slot);
                }
                for(var tag:List.of("stopper_magic","stopper")){var jammer=new Marker(EntityTypes.MARKER,base.owner.level());jammer.setPos(base.at);jammer.entityTags().addAll(Set.of(tag,"not_block"));own.add(jammer);base.owner.level().addFreshEntity(jammer);game.items().magic().loaded(jammer);}
                phase++;start=game.server().getTickCount();return false;
            }
            if(game.server().getTickCount()-start<3)return false;
            require(game.items().indolenceFingers().snapshot().size()==20&&game.items().indolenceFingers().snapshot().stream().noneMatch(Entity::isRemoved),"actual MAGIC and TECH do not erase physical fingers");
            require(base.owner.getAttributeValue(Attributes.ARMOR)==20,"all four +5 armor entries actually apply: value="+base.owner.getAttributeValue(Attributes.ARMOR)+" base="+base.owner.getAttributeBaseValue(Attributes.ARMOR)+" modifiers="+base.owner.getAttribute(Attributes.ARMOR).getModifiers());
            return true;
        }
        public void close(){if(closed)return;closed=true;game.items().indolenceFingers().stop();for(var e:own){game.items().magic().unloaded(e);e.discard();}game.items().magic().flushCleansing();
            for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())if(e instanceof net.minecraft.world.entity.item.ItemEntity&&!previous.contains(e.getUUID()))e.discard();
            base.close();base.owner.entityTags().clear();base.owner.entityTags().addAll(tags);}
    }
    private static final class Rebirth implements AutoCloseable {
        final GameController game;final UUID id,author;final int lives,deaths,kills;final Set<String> tags;
        final List<Skeleton> own=new ArrayList<>();final List<Entity> jammers=new ArrayList<>();ServerPlayer before,player;Skeleton selected,other;int start,at,phase;boolean stopped;
        Rebirth(GameController game){this.game=game;var players=game.alivePlayers();require(game.playing()&&players.size()==2,"two real live match participants");
            before=player=players.getLast();id=player.getUUID();author=players.getFirst().getUUID();tags=new HashSet<>(player.entityTags());
            lives=game.lives(id);deaths=game.players().get(id).deaths;kills=game.players().get(author).kills;
            require(lives>=2&&game.allowDamage(player)&&game.items().indolenceFingers().snapshot().isEmpty(),"ordinary remaining-life scenario");}
        void init(){player.entityTags().add("sin_indolence");player.setNoGravity(true);
            for(int i=0;i<2;i++){var s=new Skeleton(EntityTypes.SKELETON,player.level());s.entityTags().add("indolence_finger");s.setPersistenceRequired();s.setNoAi(true);s.setNoGravity(true);s.setPos(player.position().add(3+i*5,2,0));require(player.level().addFreshEntity(s),"native finger publication");own.add(s);}
            player.removeAllEffects();player.addEffect(new MobEffectInstance(MobEffects.SPEED,600,0));player.addEffect(new MobEffectInstance(MobEffects.SPEED,200,1));
            game.items().ownedEffects().grant(player,"check:indolence",new MobEffectInstance(MobEffects.LUCK,600,2),player);
            for(var tag:List.of("stopper_magic","stopper")){var marker=new Marker(EntityTypes.MARKER,player.level());marker.setPos(player.position());marker.entityTags().addAll(Set.of(tag,"not_block"));jammers.add(marker);player.level().addFreshEntity(marker);game.items().magic().loaded(marker);}
            start=game.server().getTickCount();}
        void die(boolean protectedLife){var killer=game.server().getPlayerList().getPlayer(author);player.setInvulnerableTime(0);player.damageCooldownTime=0;
            require(player.hurtServer(player.level(),player.damageSources().playerAttack(killer),10000),"accepted native lethal attack");
            require(player.isDeadOrDying()&&game.awaitingRespawn(id)&&game.lives(id)==lives-(protectedLife?0:1),"native death with exact authority life debit");
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());var old=player;player=game.server().getPlayerList().getPlayer(id);
            require(player!=null&&player!=old&&player.isAlive()&&game.allowDamage(player)&&!game.awaitingRespawn(id),"actual replacement-player respawn");}
        boolean step(){int now=game.server().getTickCount();require(now-start<85,"bounded real rebirth scenario");
            if(phase==0){if(now-start<3)return false;require(game.items().magic().suppressed(player)&&game.items().magic().mechanismSuppressed(player),"both native jammer regions cover the actual death");die(true);
                require(game.players().get(id).deaths==deaths+1&&game.players().get(author).kills==kills+1,"native credited death and kill still happen once");
                selected=own.stream().filter(e->e.position().distanceTo(player.position())<.01).findFirst().orElseThrow(()->new IllegalStateException("respawn at exact chosen finger"));other=own.stream().filter(e->e!=selected).findFirst().orElseThrow();
                require(!selected.isRemoved()&&!other.isRemoved()&&player.entityTags().contains("kill_one_skeleton")&&!player.entityTags().contains("indolence_active"),"finger cost is deferred, active flag completed");
                require(player.hasEffect(MobEffects.SPEED)&&player.getEffect(MobEffects.SPEED).getAmplifier()==1
                        &&((EffectLayers.Access)player.getEffect(MobEffects.SPEED)).skywars2$layerHidden()!=null&&player.hasEffect(MobEffects.LUCK),"native effect chain and owned layer survive replacement");
                game.items().ownedEffects().withdraw(player,"check:indolence");require(!player.hasEffect(MobEffects.LUCK),"restored lease remains independently removable");
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))require(player.getItemBySlot(slot).get(DataComponents.EQUIPPABLE)!=null,"source gear restored on real player");
                require(player.getInventory().countItem(Items.ARROW)==64&&player.getInventory().countItem(Items.BOW)==1,"source bow and64 arrows");
                player.teleportTo(player.level(),other.getX(),other.getY(),other.getZ(),TeleportMomentum.positionFlags(Set.of()),player.getYRot(),player.getXRot(),true);
                game.items().timeStop().begin(player.level(),100);stopped=true;at=now;phase++;return false;
            }
            if(phase==1){require(!selected.isRemoved()&&!other.isRemoved(),"Time Stop pauses deferred finger cost");if(now-at<7)return false;game.items().timeStop().end(player.level());stopped=false;at=now;phase++;return false;}
            if(phase==2){if(now-at<5){require(!selected.isRemoved(),"no finger consumed before five running ticks");return false;}
                require(selected.isRemoved()&&!other.isRemoved()&&!player.entityTags().contains("kill_one_skeleton"),"exact captured finger deleted after five ticks despite moving beside other finger");
                other.discard();at=now;phase++;return false;}
            if(phase==3){if(now-at<2)return false;die(false);
                require(game.players().get(id).deaths==deaths+2&&game.players().get(author).kills==kills+2&&game.lives(id)==lives-1,"no remaining finger uses normal native life/death rules");
                require(player.getInventory().countItem(Items.CARROT_ON_A_STICK)>=1&&!player.entityTags().contains("indolence_active"),"authority without active rebirth still receives the claw");
                require(game.alivePlayers().size()==2&&game.playing()&&game.stage()!=GameController.Stage.ENDING,"protected death never finishes the continuing match");return true;}
            return false;
        }
        public void close(){if(stopped)game.items().timeStop().end(player.level());for(var e:own)e.discard();for(var e:jammers){game.items().magic().unloaded(e);e.discard();}game.items().magic().flushCleansing();var current=game.server().getPlayerList().getPlayer(id);if(current!=null){game.items().indolenceFingers().forget(current);current.entityTags().clear();current.entityTags().addAll(tags);current.setNoGravity(false);game.items().ownedEffects().withdraw(current,"check:indolence");}}
    }
    private static Producer producer;private static Rebirth rebirth;
    public static void producer(GameController game){require(producer==null&&rebirth==null,"exclusive scenario");producer=new Producer(game);try{producer.init();}catch(Throwable e){producer.close();producer=null;throw e;}SkyWars2.LOGGER.info("[check] START indolence-fingers");}
    public static void rebirth(GameController game){require(producer==null&&rebirth==null,"exclusive scenario");rebirth=new Rebirth(game);try{rebirth.init();}catch(Throwable e){rebirth.close();rebirth=null;throw e;}SkyWars2.LOGGER.info("[check] START indolence-rebirth");}
    public static void tick(GameController game){if(producer!=null)try{if(producer.step()){producer.close();producer=null;SkyWars2.LOGGER.info("[check] PASS indolence-fingers (actual random20 producer, native body equipment/AI, physical classification, shared cause and approved reward armor slots)");}}catch(Throwable e){try{producer.close();}catch(Throwable c){e.addSuppressed(c);}producer=null;SkyWars2.LOGGER.error("[check] FAIL indolence-fingers",e);}
        if(rebirth!=null)try{if(rebirth.step()){rebirth.close();rebirth=null;SkyWars2.LOGGER.info("[check] PASS indolence-rebirth (real native deaths/replacement players, preserved life/effect chains/owned leases, exact chosen finger five-tick cost with TimeStop, source gear and ordinary fallback)");}}catch(Throwable e){try{rebirth.close();}catch(Throwable c){e.addSuppressed(c);}rebirth=null;SkyWars2.LOGGER.error("[check] FAIL indolence-rebirth",e);}}
}
