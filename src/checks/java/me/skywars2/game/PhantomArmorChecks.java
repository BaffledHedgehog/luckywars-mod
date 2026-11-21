package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Source acquisition, real native effect ownership and accepted melee, followed by real registered player ticks. */
public final class PhantomArmorChecks {
    private PhantomArmorChecks(){}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final String[] PARTS={"boots","leggings","chestplate","helmet"};
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Phantom armor: "+why);}
    private static java.lang.reflect.Field field(Object object,String name){for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static Object get(Object o,String n){try{return field(o,n).get(o);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void set(Object o,String n,Object v){try{field(o,n).set(o,v);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,beta;
        final GameChecks.WandThirdPlayerState betaSaved;final ServerGamePacketListenerImpl betaConnection;
        final ElementalGrimoireChecks.Recording audience;final LbcPhantomArmor service;final ItemStack[] pieces=new ItemStack[4];
        final Object oldPhase,oldEpoch;final List<Cow> cows=new ArrayList<>();Marker marker;ScheduledClient client;boolean closed,preparing=true;int started,stage,updates;
        Fixture(GameController game){this.game=game;service=game.items().armorSets().phantom();oldPhase=get(game.items().armorSets(),"phase");oldEpoch=get(game.items().armorSets(),"epoch");base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));betaSaved=new GameChecks.WandThirdPlayerState(beta,game.items().mana());betaConnection=beta.connection;audience=new ElementalGrimoireChecks.Recording(beta);
            beta.connection=new ServerGamePacketListenerImpl(game.server(),audience,beta,new CommonListenerCookie(beta.getGameProfile(),0,beta.clientInformation(),false));beta.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());base.initialize();owner.removeAllEffects();beta.removeAllEffects();far();
        }
        List<ItemStack> roll(String id){return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id);}
        void far(){beta.teleportTo(owner.level(),owner.getX()+12,owner.getY(),owner.getZ(),Set.of(),0,0,true);beta.setNoGravity(true);beta.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
        void near(){beta.teleportTo(owner.level(),owner.getX()+2,owner.getY(),owner.getZ(),Set.of(),0,0,true);}
        void reset(){game.items().ownedEffects().withdraw(owner,LbcPhantomArmor.EFFECT_OWNER);service.forget(owner);owner.removeAllEffects();beta.removeAllEffects();for(var slot:SLOTS)owner.setItemSlot(slot,ItemStack.EMPTY);base.recording.packets.clear();audience.packets.clear();far();}
        void wear(int amount){reset();for(int n=0;n<amount;n++)owner.setItemSlot(SLOTS[n],pieces[n].copy());service.forget(owner);owner.removeAllEffects();base.recording.packets.clear();audience.packets.clear();}
        void call(){service.pulse(owner);}
        MobEffectInstance layer(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int amp){for(var e=owner.getEffect(type);e!=null;e=((EffectLayers.Access)e).skywars2$layerHidden())if(e.getAmplifier()==amp)return e;return null;}
        void bonus(){for(var type:List.of(MobEffects.INVISIBILITY,MobEffects.STRENGTH)){var e=layer(type,1);require(e!=null&&e.getDuration()==6000&&!e.isVisible()&&!e.showIcon(),"source hidden II6000 "+type);}}
        void absent(){require(!owner.hasEffect(MobEffects.INVISIBILITY)&&!owner.hasEffect(MobEffects.STRENGTH),"only bonus absent");}
        void acquisition(){
            for(int n=0;n<4;n++){var out=roll("neko_set_phantom/"+PARTS[n]);require(out.size()==1&&out.getFirst().getCount()==1,"original singleton "+PARTS[n]);var s=pieces[n]=out.getFirst();require(LbcFartus.data(s).getIntOr("phantom",0)==1&&LbcFartus.data(s).getIntOr("transformable",0)==1,"authored flags");require(s.get(DataComponents.EQUIPPABLE).slot()==SLOTS[n]&&s.has(DataComponents.CUSTOM_NAME)&&s.has(DataComponents.ITEM_MODEL)&&s.has(DataComponents.LORE)&&s.has(DataComponents.TOOLTIP_STYLE),"authored wearable presentation");}
            var full=roll("neko_set_phantom_full");require(full.size()==4,"four original full outputs");for(var leaf:pieces)require(full.stream().anyMatch(s->ItemStack.matches(s,leaf)),"exact full leaf components");
            var pre=roll("neko_set_phantom_pre");require(pre.size()==5,"source pre five outputs");for(var out:pre)require(Arrays.stream(pieces).anyMatch(leaf->ItemStack.matches(out,leaf)),"all five pre draws retain exact authored leaf components; replacement allowed");
            var seen=new HashSet<Integer>();for(int attempt=0;attempt<256&&seen.size()<4;attempt++){var out=roll("neko_set_phantom");require(out.size()==1,"one original weighted root output");int index=-1;for(int n=0;n<4;n++)if(ItemStack.matches(out.getFirst(),pieces[n]))index=n;require(index>=0,"root exact original leaf");seen.add(index);}require(seen.size()==4,"all original root branches");
        }
        @SuppressWarnings("unchecked") void reforges(){
            for(int slot:new int[]{0,1,2,3})for(var family:List.of("phantom")){
                int selected=12;String id="transformables_"+PARTS[slot];
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

        void equipment(){for(int amount=0;amount<=4;amount++){wear(amount);call();if(amount==4)bonus();else absent();}
            reset();for(int n=0;n<4;n++)owner.getInventory().setItem(9+n,pieces[n].copy());call();absent();owner.getInventory().clearContent();
            for(double distance:new double[]{.005,.02,8,8.001}){wear(4);beta.teleportTo(owner.level(),owner.getX()+distance,owner.getY(),owner.getZ(),Set.of(),0,0,true);call();if(distance>=.01&&distance<=8)absent();else bonus();}
            wear(4);near();call();absent();far();call();bonus();near();call();bonus();
            far();owner.setItemSlot(SLOTS[0],ItemStack.EMPTY);service.inventoryChanged(owner);absent();
        }
        void owned(){
            for(var type:List.of(MobEffects.INVISIBILITY,MobEffects.STRENGTH)){
                wear(4);owner.addEffect(ItemEffectLevels.create(type,900,-2));owner.addEffect(ItemEffectLevels.create(type,800,0));owner.addEffect(ItemEffectLevels.create(type,700,4));owner.addEffect(ItemEffectLevels.create(type,600,1));call();
                require(layer(type,1)!=null&&layer(type,4)!=null,"sourceII admitted behind stronger real potion");
                owner.getEffect(type).tickServer(owner.level(),owner,()->{});service.activity(owner);
                require(layer(type,1).getDuration()==599&&layer(type,4).getDuration()==699&&layer(type,0).getDuration()==799&&layer(type,-2).getDuration()==899,"withdraw preserves independently aged same/higher/lower/negative external layers");
                wear(4);call();owner.addEffect(ItemEffectLevels.create(type,321,1));owner.getEffect(type).tickServer(owner.level(),owner,()->{});service.activity(owner);require(layer(type,1)!=null&&layer(type,1).getDuration()==320,"external same amplifier drawn while owned survives with native elapsed clock");
                wear(4);call();owner.addEffect(ItemEffectLevels.create(type,210,1));owner.setItemSlot(SLOTS[0],ItemStack.EMPTY);service.inventoryChanged(owner);require(layer(type,1).getDuration()==210,"gear removal withdraws only owned source");
                wear(4);call();owner.removeEffect(type);service.activity(owner);require(!owner.hasEffect(type),"native external removal never resurrects saved baseline");
            }
            wear(4);var leases=game.items().ownedEffects();owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,3,0));require(leases.grant(owner,"phantom-check-expiry",ItemEffectLevels.create(MobEffects.SPEED,2,1),owner),"short real owned lease admission");
            for(int n=0;n<2;n++)owner.getEffect(MobEffects.SPEED).tickServer(owner.level(),owner,()->{});leases.withdraw(owner,"phantom-check-expiry");require(layer(MobEffects.SPEED,1)==null&&layer(MobEffects.SPEED,0)!=null&&layer(MobEffects.SPEED,0).getDuration()==1,"native lease expiry retains independently ticking external source");owner.removeEffect(MobEffects.SPEED);
            owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,90,1));
            require(leases.grant(owner,"phantom-check-first",ItemEffectLevels.create(MobEffects.SPEED,100,1),owner)&&leases.grant(owner,"phantom-check-second",ItemEffectLevels.create(MobEffects.SPEED,120,1),owner),"two independent native owner leases");
            owner.getEffect(MobEffects.SPEED).tickServer(owner.level(),owner,()->{});leases.withdraw(owner,"phantom-check-second");require(layer(MobEffects.SPEED,1).getDuration()==99,"withdrawing longer owner retains other live native lease");leases.withdraw(owner,"phantom-check-first");require(layer(MobEffects.SPEED,1).getDuration()==89,"last owner withdrawal restores independently aged baseline");owner.removeEffect(MobEffects.SPEED);
            owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,-1,1));require(leases.grant(owner,"phantom-check-infinite",ItemEffectLevels.create(MobEffects.SPEED,20,1),owner),"lease can coexist with infinite native baseline");owner.getEffect(MobEffects.SPEED).tickServer(owner.level(),owner,()->{});leases.withdraw(owner,"phantom-check-infinite");require(layer(MobEffects.SPEED,1).isInfiniteDuration(),"infinite external baseline survives native tick/withdraw");owner.removeEffect(MobEffects.SPEED);
            require(leases.grant(owner,"phantom-check-force",ItemEffectLevels.create(MobEffects.SPEED,100,1),owner),"lease before native forced replacement");owner.forceAddEffect(ItemEffectLevels.create(MobEffects.SPEED,45,1),owner);leases.withdraw(owner,"phantom-check-force");require(layer(MobEffects.SPEED,1)!=null&&layer(MobEffects.SPEED,1).getDuration()==45,"forceAddEffect replaces ownership with native baseline");owner.removeEffect(MobEffects.SPEED);
            require(leases.grant(owner,"phantom-check-clear",ItemEffectLevels.create(MobEffects.SPEED,100,1),owner),"lease before native full cleanse");owner.removeAllEffects();leases.withdraw(owner,"phantom-check-clear");require(!owner.hasEffect(MobEffects.SPEED),"native removeAllEffects invalidates ownership without resurrecting it");
            require(!leases.grant(owner,"phantom-check-expired",ItemEffectLevels.create(MobEffects.SPEED,0,1),owner)&&!owner.hasEffect(MobEffects.SPEED),"expired grant rejected without installing native effect");
        }
        void zone(String tag){if(marker!=null){game.items().magic().unloaded(marker);marker.discard();marker=null;}if(tag!=null){marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add(tag);require(owner.level().addFreshEntity(marker),"native MAG marker");game.items().magic().loaded(marker);}}
        void guards(){wear(4);call();owner.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,188,1));zone("stopper_magic");call();require(layer(MobEffects.INVISIBILITY,1).getDuration()==188&&!owner.hasEffect(MobEffects.STRENGTH),"MAG withdraw preserves external same amplifier");zone(null);owner.removeAllEffects();call();bonus();
            wear(4);owner.entityTags().add("nomagic_active");game.items().mana().noMagic(owner,100);try{call();bonus();}finally{owner.entityTags().remove("nomagic_active");game.items().mana().noMagic(owner,0);}
            for(var tag:List.of("time_stopped","spectator","strict_map_object")){wear(4);owner.entityTags().add(tag);try{call();absent();}finally{owner.entityTags().remove(tag);}}
        }
        Cow cow(){var c=new Cow(EntityTypes.COW,owner.level());c.setNoAi(true);c.setNoGravity(true);c.setPos(owner.position().add(0,0,2));require(owner.level().addFreshEntity(c),"native mob victim");cows.add(c);return c;}
        void hit(Cow c,boolean melee){c.setInvulnerableTime(0);c.damageCooldownTime=0;require(c.hurtServer(owner.level(),melee?c.damageSources().playerAttack(owner):c.damageSources().indirectMagic(owner,owner),.1f),"accepted native damage");}
        List<ClientboundLevelParticlesPacket> dust(ElementalGrimoireChecks.Recording r){return r.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle().getType()==ParticleTypes.DUST).toList();}
        void melee(){wear(4);call();near();var c=cow();hit(c,false);require(!beta.hasEffect(MobEffects.BLINDNESS),"magic damage never flashes");hit(c,true);absent();var e=beta.getEffect(MobEffects.BLINDNESS);require(e!=null&&e.getAmplifier()==0&&e.getDuration()==100&&e.isVisible(),"accepted melee against mob blinds nearby enemy I100");require(!owner.hasEffect(MobEffects.BLINDNESS)&&dust(base.recording).isEmpty(),"wearer excluded from flash");var packets=dust(audience);require(packets.size()==1,"one ordinary audience dust emission");var p=packets.getFirst();require(p.count()==50&&!p.overrideLimiter()&&Math.abs(p.x()-owner.getX())<1e-6&&Math.abs(p.y()-owner.getY()-1)<1e-6&&Math.abs(p.z()-owner.getZ())<1e-6&&Math.abs(p.xDist()-.3)<1e-6&&Math.abs(p.yDist()-.6)<1e-6&&Math.abs(p.zDist()-.3)<1e-6&&Math.abs(p.xMaxSpeed()-.3)<1e-6&&Math.abs(p.yMaxSpeed()-.3)<1e-6&&Math.abs(p.zMaxSpeed()-.3)<1e-6&&!p.alwaysShow(),"exact source dust origin/spread/count/speed");
            beta.removeAllEffects();c.setPermanentlyInvulnerable(true);require(!c.hurtServer(owner.level(),c.damageSources().playerAttack(owner),1)&&!beta.hasEffect(MobEffects.BLINDNESS),"rejected native hit never flashes");c.setPermanentlyInvulnerable(false);
            for(String tag:List.of("stopper_magic","personal")){wear(4);near();if(tag.equals("personal")){owner.entityTags().add("nomagic_active");game.items().mana().noMagic(owner,100);}else zone(tag);try{hit(c,true);require(!beta.hasEffect(MobEffects.BLINDNESS),"flash suppression "+tag);}finally{zone(null);owner.entityTags().remove("nomagic_active");game.items().mana().noMagic(owner,0);}}
            wear(4);call();beta.teleportTo(owner.level(),owner.getX(),owner.getY()+2,owner.getZ(),Set.of(),0,0,true);
            zone("stopper_magic");game.items().magic().unloaded(marker);marker.setPos(owner.position().add(0,21,0));game.items().magic().loaded(marker);
            require(!game.items().magic().beaconSuppressed(owner.position())&&game.items().magic().beaconSuppressed(beta.position()),"native target-only MAG volume leaves actual caster outside");
            hit(c,true);require(!beta.hasEffect(MobEffects.BLINDNESS)&&dust(audience).size()==1,"target inside MAG is protected while actual outside caster still emits flash");zone(null);
            hit(c,true);require(beta.hasEffect(MobEffects.BLINDNESS),"same actual target outside retired MAG accepts flash");
            var board=game.server().getScoreboard();var oldA=owner.getTeam();var oldB=beta.getTeam();var team=board.addPlayerTeam("phantom_fixture");try{board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);wear(4);near();call();bonus();hit(c,true);require(!beta.hasEffect(MobEffects.BLINDNESS),"native ally never blinded");}finally{board.removePlayerTeam(team);if(oldA!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldA);if(oldB!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldB);}
            cows.forEach(Cow::discard);cows.clear();
        }
        void input(){wear(4);call();owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));owner.getInventory().setItem(9,new ItemStack(Items.ARROW));require(owner.gameMode.useItem(owner,owner.level(),owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&owner.isUsingItem(),"native survival bow use admitted with real arrow");absent();owner.releaseUsingItem();owner.getInventory().setItem(9,ItemStack.EMPTY);
            wear(4);call();owner.attack(cow());absent();cows.forEach(Cow::discard);cows.clear();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            wear(4);call();var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(owner.level(),owner,new ItemStack(Items.ARROW),null);arrow.setPos(owner.position().add(0,2,0));arrow.setNoGravity(true);try{require(owner.level().addFreshEntity(arrow),"fresh native owned projectile published");absent();}finally{arrow.discard();}
        }
        void prepare(){acquisition();reforges();ultra();equipment();owned();guards();melee();input();wear(4);client=new ScheduledClient(game,owner);stage=-1;started=game.server().getTickCount();preparing=false;}
        boolean step(){int elapsed=game.server().getTickCount()-started;require(elapsed<100,"bounded actual scheduler fixture");client.acknowledge();
            if(stage<0){call();bonus();service.activity(owner);absent();started=game.server().getTickCount();updates=client.updates();stage=0;return false;}
            if(stage==0){if(elapsed<20){absent();return false;}if(!owner.hasEffect(MobEffects.INVISIBILITY)){require(elapsed<25,"return within five active ticks of20lock");return false;}require(client.updates()-updates>=20,"real registered player ticks");near();started=game.server().getTickCount();stage=1;return false;}
            if(stage==1){require(owner.hasEffect(MobEffects.INVISIBILITY),"enemy approach retains existing bonus");if(elapsed<6)return false;service.activity(owner);game.items().timeStop().begin(owner.level(),100);started=game.server().getTickCount();stage=2;return false;}
            if(stage==2){absent();if(elapsed<8)return false;game.items().timeStop().end(owner.level());far();started=game.server().getTickCount();stage=3;return false;}
            if(stage==3){if(elapsed<20){absent();return false;}if(!owner.hasEffect(MobEffects.INVISIBILITY)){require(elapsed<25,"Time Stop never advances20active tick lock");return false;}zone("stopper_magic");started=game.server().getTickCount();stage=4;return false;}
            if(stage==4){if(elapsed<6)return false;absent();zone(null);started=game.server().getTickCount();stage=5;return false;}
            if(stage==5){if(!owner.hasEffect(MobEffects.INVISIBILITY)){require(elapsed<6,"automatic return after MAG without inventory input");return false;}near();service.activity(owner);started=game.server().getTickCount();stage=6;return false;}
            if(stage==6){absent();if(elapsed<26)return false;far();started=game.server().getTickCount();stage=7;return false;}
            if(!owner.hasEffect(MobEffects.INVISIBILITY)){require(elapsed<6,"automatic return after enemy leaves expired lock");return false;}return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{zone(null);if(client!=null)client.close();cows.forEach(Cow::discard);game.items().timeStop().end(owner.level());game.items().ownedEffects().withdraw(owner,LbcPhantomArmor.EFFECT_OWNER);service.forget(owner);set(game.items().armorSets(),"phase",oldPhase);set(game.items().armorSets(),"epoch",oldEpoch);beta.connection=betaConnection;betaSaved.restore(game);}finally{base.retain=true;base.close();}}
    }
    private static final class ScheduledConnection extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket latestPosition;
        ScheduledConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnecting(){return false;}
        @Override public boolean isConnected(){return true;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);if(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket position)latestPosition=position;}
        @Override public void flushChannel(){}
    }
    private static final class ScheduledClient implements AutoCloseable {
        final GameController game;final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous,listener;
        final ScheduledConnection connection=new ScheduledConnection();final java.lang.reflect.Field updates;
        ScheduledClient(GameController game,ServerPlayer player){this.game=game;this.player=player;previous=player.connection;
            try{
                listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),connection,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
                // A fresh memory connection has no protocol channel pipeline; bind only its own native dispatch field.
                field(connection,"packetListener").set(connection,listener);updates=field(listener,"tickCount");
                listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                listener.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
                var teleport=connection.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
                listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
                require(listener.hasClientLoaded()&&field(listener,"awaitingPositionFromClient").get(listener)==null,"fresh native listener loaded and real teleport acknowledged");
                game.server().getConnection().getConnections().add(connection);
            }catch(ReflectiveOperationException|RuntimeException|Error error){game.server().getConnection().getConnections().remove(connection);player.connection=previous;throw new IllegalStateException("scoped native connection scheduler",error);}
        }
        void acknowledge(){listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());try{if(field(listener,"awaitingPositionFromClient").get(listener)!=null){var teleport=Objects.requireNonNull(connection.latestPosition,"actual emitted teleport packet survives measurement windows");listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));}}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}require(ready(),"native player loaded and latest actual teleport acknowledged");}
        boolean ready(){try{return listener.hasClientLoaded()&&!player.isChangingDimension()&&field(listener,"awaitingPositionFromClient").get(listener)==null;}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        int updates(){try{return updates.getInt(listener);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        @Override public void close(){try{game.server().getConnection().getConnections().remove(connection);}finally{player.connection=previous;}}
    }

    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.preparing){f.prepare();return;}if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS phantom-armor (native source/reforge, owned same/hidden/negative clocks, native melee/input, enemy/ally/audience, MAG and actual active20lock/TimeStop/automatic return)");}}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL phantom-armor",error);}}
}
