package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Native source acquisition, equipped admission, jammer semantics and exact recipient-specific presentation. */
public final class AssassinArmorChecks {
    private AssassinArmorChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Assassin armor: "+why);}
    private static java.lang.reflect.Field field(Object object,String name){for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static Object get(Object object,String name){try{return field(object,name).get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void set(Object object,String name,Object value){try{field(object,name).set(object,value);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,beta;
        final GameChecks.WandThirdPlayerState betaSaved;final ServerGamePacketListenerImpl betaConnection;
        final ElementalGrimoireChecks.Recording audience;final LbcArmorSets service;final ItemStack[] pieces=new ItemStack[4];
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Object oldPhase,oldEpoch;Marker marker;boolean closed;int started,steps;
        Fixture(GameController game){this.game=game;service=game.items().armorSets();oldPhase=get(service,"phase");oldEpoch=get(service,"epoch");base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));betaSaved=new GameChecks.WandThirdPlayerState(beta,game.items().mana());betaConnection=beta.connection;audience=new ElementalGrimoireChecks.Recording(beta);
            beta.connection=new ServerGamePacketListenerImpl(game.server(),audience,beta,new CommonListenerCookie(beta.getGameProfile(),0,beta.clientInformation(),false));beta.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            base.initialize();beta.teleportTo(owner.level(),owner.getX()+2,owner.getY(),owner.getZ(),Set.of(),0,0,true);beta.setNoGravity(true);
        }
        List<ItemStack> roll(String id){return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id);}
        void empty(){for(var slot:SLOTS)owner.setItemSlot(slot,ItemStack.EMPTY);owner.removeAllEffects();base.recording.packets.clear();audience.packets.clear();}
        void wear(int amount){empty();for(int n=0;n<amount;n++)owner.setItemSlot(SLOTS[n],pieces[n].copy());}
        void call(){service.assassin(owner);}
        void invisible(){var e=owner.getEffect(MobEffects.INVISIBILITY);require(e!=null&&e.getDuration()==100&&e.getAmplifier()==1&&!e.isVisible(),"exact hidden InvisibilityII100");}
        List<ClientboundLevelParticlesPacket> particles(ElementalGrimoireChecks.Recording recorder){return recorder.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle().getType()==ParticleTypes.BLOCK_MARKER).toList();}
        void acquisition(){
            for(int n=0;n<4;n++){var out=roll("neko_set_assasin/"+PARTS[n]);require(out.size()==1&&out.getFirst().getCount()==1,"original singleton "+PARTS[n]);var s=pieces[n]=out.getFirst();require(LbcFartus.data(s).getIntOr("assasin",0)==1&&LbcFartus.data(s).getIntOr("transformable",0)==1,"authored flags");require(s.get(DataComponents.EQUIPPABLE).slot()==SLOTS[n]&&s.has(DataComponents.CUSTOM_NAME)&&s.has(DataComponents.ITEM_MODEL)&&s.has(DataComponents.LORE)&&s.has(DataComponents.TOOLTIP_STYLE),"authored wearable presentation");}
            var full=roll("neko_set_assasin_full");require(full.size()==4,"four original full outputs");for(var leaf:pieces)require(full.stream().anyMatch(s->ItemStack.matches(s,leaf)),"exact full leaf components");
            var pre=roll("neko_set_assasin_pre");require(pre.size()==5,"source pre five outputs");for(var out:pre)require(Arrays.stream(pieces).anyMatch(leaf->ItemStack.matches(out,leaf)),"all five pre draws retain exact authored leaf components; replacement allowed");
            var seen=new HashSet<Integer>();for(int attempt=0;attempt<256&&seen.size()<4;attempt++){var out=roll("neko_set_assasin");require(out.size()==1,"one original weighted root output");int index=-1;for(int n=0;n<4;n++)if(ItemStack.matches(out.getFirst(),pieces[n]))index=n;require(index>=0,"root exact original leaf");seen.add(index);}require(seen.size()==4,"all original root branches");
        }
        @SuppressWarnings("unchecked") void reforges(){
            for(int slot:new int[]{0,1,2,3})for(var family:List.of("assasin")){
                int selected=10;String id="transformables_"+PARTS[slot];
                var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:"+id)));
                var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)get(table,"pools");require(pools.size()==1,"one native original armor reforge pool");
                var sequence=(Optional<net.minecraft.resources.Identifier>)get(table,"randomSequence");require(sequence.isEmpty(),"normal armor reforge uses native level RNG");
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,owner.position()).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
                var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);var pool=pools.getFirst();int total=0,offset=-1,selectedWeight=0;
                for(int n=0;n<pool.entries.size();n++){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pool.entries.get(n).expand(context,expanded::add);for(var entry:expanded){int weight=entry.getWeight(context.getLuck());if(weight<=0)continue;if(n==selected){if(offset<0)offset=total;selectedWeight+=weight;}total+=weight;}}
                require(selectedWeight==1&&offset>=0,"exact original weight1 set reforge branch");int bound=total,pick=offset;var random=field(owner.level(),"random");Object original=get(owner.level(),"random");int[] calls={0};
                var input=new net.minecraft.world.item.ItemStack(Items.STICK);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("transformable",1);flags.putInt(PARTS[slot],1);input.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(flags));var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),owner.getX(),owner.getY(),owner.getZ(),input);donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"real native armor reforge donor");var outputs=new ArrayList<ItemStack>();
                try{random.set(owner.level(),java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return method.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));
                    var factory=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);factory.setAccessible(true);var work=(LbcStonekivi.Work)factory.invoke(game.items().bolts(),donor,LbcBolts.ReforgeFamily.NORMAL,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"existing native normal armor reforge Work");
                    require(calls[0]==1&&donor.isRemoved()&&input.isEmpty()&&outputs.size()==1&&ItemStack.matches(outputs.getFirst(),pieces[slot]),"native reforge consumes one input and retains exact authored leaf "+family+"/"+PARTS[slot]);
                }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}finally{try{random.set(owner.level(),original);}catch(IllegalAccessException e){throw new IllegalStateException(e);}finally{donor.discard();}}
            }
        }
        void ultra(){
            for(int slot=0;slot<4;slot++){
                var input=pieces[slot].copy();var donor=new net.minecraft.world.entity.item.ItemEntity(owner.level(),owner.getX(),owner.getY(),owner.getZ(),input);donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"native ultra donor");var outputs=new ArrayList<ItemStack>();
                try{var factory=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);factory.setAccessible(true);var work=(LbcStonekivi.Work)factory.invoke(game.items().bolts(),donor,LbcBolts.ReforgeFamily.ULTRA,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step()&&input.isEmpty()&&donor.isRemoved()&&outputs.size()==1,"native ultra consumes one and rolls one original family");var output=outputs.getFirst();require(Arrays.stream(pieces).anyMatch(s->ItemStack.matches(s,output)),"ultra exact original source piece");}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}finally{donor.discard();}
            }
        }
        void criterion(){var a=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse("lbc:true_advancements/armor/assasin"));require(a!=null&&a.value().criteria().size()==1&&a.value().criteria().containsKey("requirement"),"original native Assassin advancement registered");var trigger=a.value().criteria().get("requirement").triggerInstance();require(trigger instanceof net.minecraft.advancements.triggers.KilledTrigger.TriggerInstance,"original native player-killed-entity trigger instance");}
        void equipment(){for(int amount=0;amount<=4;amount++){wear(amount);call();require(owner.hasEffect(MobEffects.INVISIBILITY)==(amount==4),"actual four equipped pieces count="+amount);if(amount==4)invisible();}
            empty();for(int n=0;n<4;n++)owner.getInventory().setItem(9+n,pieces[n].copy());call();require(!owner.hasEffect(MobEffects.INVISIBILITY),"carried full set does not activate");owner.getInventory().clearContent();
            wear(4);owner.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,123,0));owner.addEffect(ItemEffectLevels.create(MobEffects.DARKNESS,123,0));owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,234,2));call();invisible();require(!owner.hasEffect(MobEffects.BLINDNESS)&&!owner.hasEffect(MobEffects.DARKNESS)&&owner.getEffect(MobEffects.SPEED).getDuration()==234,"exact two cleanses preserve unrelated effect");
        }
        void presentation(){wear(4);call();require(particles(base.recording).isEmpty(),"wearer excluded from block markers");var packets=particles(audience);require(packets.size()==32,"all32 exact source enemy packets");
            int[][] offsets={{0,0},{4,0},{-4,0},{0,4},{0,-4},{4,4},{-4,4},{-4,-4},{4,-4},{8,-4},{8,0},{8,4},{8,8},{4,8},{0,8},{-4,8}};
            for(int n=0;n<32;n++){var p=packets.get(n);var off=offsets[n%16];require(Math.abs(p.x()-owner.getX()-off[0])<1e-6&&Math.abs(p.z()-owner.getZ()-off[1])<1e-6&&Math.abs(p.y()-owner.getY()-(n<16?1.6:.6))<1e-6,"exact source ordered origin "+n);require(p.particle() instanceof BlockParticleOption b&&b.getState().is(Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.BLACK))&&p.count()==3&&p.overrideLimiter()&&p.xMaxSpeed()==0&&p.yMaxSpeed()==0&&p.zMaxSpeed()==0&&Math.abs(p.xDist()-.2)<1e-6&&Math.abs(p.yDist()-.2)<1e-6&&Math.abs(p.zDist()-.2)<1e-6,"forced black marker source payload "+n);}
            var board=game.server().getScoreboard();var oldOwner=owner.getTeam();var oldBeta=beta.getTeam();require(board.getPlayerTeam("assassin_fixture")==null,"exclusive team fixture");var team=board.addPlayerTeam("assassin_fixture");
            try{board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);audience.packets.clear();call();require(particles(audience).isEmpty(),"native teammate receives no illusion markers");}
            finally{board.removePlayerFromTeam(owner.getScoreboardName(),team);board.removePlayerFromTeam(beta.getScoreboardName(),team);board.removePlayerTeam(team);if(oldOwner!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwner);if(oldBeta!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBeta);}
        }
        void zone(String tag){if(marker!=null){game.items().magic().unloaded(marker);marker.discard();marker=null;}if(tag!=null){marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add(tag);require(owner.level().addFreshEntity(marker),"native jammer marker");game.items().magic().loaded(marker);}}
        void guards(){wear(4);zone("stopper_magic");require(game.items().magic().beaconSuppressed(owner.position()),"actual MAG positive control");call();require(!owner.hasEffect(MobEffects.INVISIBILITY)&&particles(audience).isEmpty(),"MAG blocks new grant and particles");owner.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,73,1));call();require(owner.getEffect(MobEffects.INVISIBILITY).getDuration()==73,"MAG preserves existing lifetime");zone("stopper");owner.removeAllEffects();call();invisible();zone(null);
            for(var tag:List.of("time_stopped","spectator","strict_map_object")){wear(4);owner.entityTags().add(tag);try{call();require(!owner.hasEffect(MobEffects.INVISIBILITY),"role/stop guard "+tag);}finally{owner.entityTags().remove(tag);}}
            wear(4);game.items().timeStop().begin(owner.level(),40);try{call();require(!owner.hasEffect(MobEffects.INVISIBILITY),"actual dimension Time Stop");}finally{game.items().timeStop().end(owner.level());}
            wear(4);owner.entityTags().add("nomagic_active");game.items().mana().noMagic(owner,100);try{call();invisible();}finally{owner.entityTags().remove("nomagic_active");game.items().mana().noMagic(owner,0);}
            wear(4);require(game.items().shooters().stun(beta,owner,10),"native stun control");try{call();invisible();}finally{game.items().shooters().clearStun(owner);}
            wear(4);var stage=game.stage();try{set(game,"stage",GameController.Stage.ENDING);call();require(!owner.hasEffect(MobEffects.INVISIBILITY),"ENDING rejects passive");}finally{set(game,"stage",stage);}
            var level=owner.level();var at=owner.position();var elsewhere=java.util.stream.StreamSupport.stream(game.server().getAllLevels().spliterator(),false).filter(l->l!=level).findFirst().orElseThrow();try{owner.teleportTo(elsewhere,at.x,at.y,at.z,Set.of(),0,0,true);call();require(!owner.hasEffect(MobEffects.INVISIBILITY),"foreign dimension rejects passive");}finally{owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);}
        }
        void prepare(){acquisition();reforges();ultra();criterion();equipment();presentation();guards();wear(4);steps=-1;started=game.server().getTickCount();}
        boolean step(){int elapsed=game.server().getTickCount()-started;require(elapsed<20,"bounded actual tick fixture");if(steps<0){steps=0;started=game.server().getTickCount();owner.removeAllEffects();audience.packets.clear();return false;}if(elapsed==0)return false;invisible();require(particles(audience).size()==32,"one complete enemy illusion on each actual service tick");steps++;if(steps>=3)return true;owner.removeAllEffects();audience.packets.clear();return false;}
        @Override public void close(){if(closed)return;closed=true;try{zone(null);game.items().shooters().clearStun(owner);game.items().timeStop().end(game.maps().level());set(service,"phase",oldPhase);set(service,"epoch",oldEpoch);advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))owner.getAdvancements().award(a,key);else owner.getAdvancements().revoke(a,key);});beta.connection=betaConnection;betaSaved.restore(game);}finally{base.retain=true;base.close();}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);try{f.prepare();running=f;}catch(RuntimeException|Error error){f.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS assassin-armor (native source acquisition, equipment, audience/32offsets, cleanses, jammers/roles and actual every-tick scheduler)");}}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL assassin-armor",error);}}
}
