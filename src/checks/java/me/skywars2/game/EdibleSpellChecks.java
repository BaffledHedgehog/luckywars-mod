package me.skywars2.game;
import me.skywars2.SkyWars2;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
/** Bounded actual native consumption, use and delayed fang paths; never advances a production clock. */
public final class EdibleSpellChecks {
    private EdibleSpellChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Edible spells: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> t=value.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static Object read(Object v,String n){try{return field(v,n).get(v);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object v,String n,Object x){try{field(v,n).set(v,x);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
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
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final boolean ground,sprint,shift;final Vec3 known;final net.minecraft.world.entity.Pose pose;final Entity camera;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> values=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        State(GameController game,ServerPlayer p){player=p;require(!p.isUsingItem(),"fixture starts with idle hands");state=new GameChecks.WandThirdPlayerState(p,game.items().mana());ground=p.onGround();sprint=p.isSprinting();shift=p.isShiftKeyDown();known=p.getKnownMovement();pose=p.getPose();camera=p.getCamera();
            for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=Stats.CUSTOM.get(id);stats.put(s,p.getStats().getValue(s));}for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.GOLDEN_APPLE,Items.SALMON,Items.SHIELD,Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET)){var s=type.get(item);stats.put(s,p.getStats().getValue(s));}
            for(var entry:LbcNamedFoodItems.entries()){var s=Stats.ITEM_USED.get(entry.item());stats.put(s,p.getStats().getValue(s));}
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(score!=null)scores.put(objective,score.value());}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var name:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource","lastRecordedFoodLevel","lastSentFood","lastFoodSaturationZero","lastSentHealth","lastRecordedHealthAndAbsorption"))values.put(field(p,name),read(p,name));
            try{for(var f:p.getCombatTracker().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);var v=f.get(p.getCombatTracker());if(v instanceof List<?> list)combat.put(f,new ArrayList<>(list));else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers()))combat.put(f,v);}for(var f:p.getFoodData().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(p.getFoodData()));}for(var f:game.players().get(p.getUUID()).getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(game.players().get(p.getUUID()));profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> s?new HashSet<>(s):v);}for(var entry:((Map<?,?>)read(p,"lastEquipmentItems")).entrySet())equipment.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings({"rawtypes","unchecked"}) void restore(GameController game){state.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(state.health);player.setAbsorptionAmount(state.absorption);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setKnownMovement(known);player.setPose(pose);stats.forEach((s,v)->player.getStats().setValue(player,s,v));for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(player,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(player,o);advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            try{for(var e:values.entrySet())e.getKey().set(player,e.getValue());for(var e:combat.entrySet())if(e.getValue() instanceof List old){var current=(List)e.getKey().get(player.getCombatTracker());current.clear();current.addAll(old);}else e.getKey().set(player.getCombatTracker(),e.getValue());for(var e:food.entrySet())e.getKey().set(player.getFoodData(),e.getValue());for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());var equipmentMap=(Map<EquipmentSlot,ItemStack>)read(player,"lastEquipmentItems");equipmentMap.clear();equipment.forEach((s,i)->equipmentMap.put(s,i.copy()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}player.setCamera(camera);
        }
    }

    private static void restored(State saved){var p=saved.player;var old=saved.state;var a=p.getAbilities();require(a.invulnerable==old.abilitiesInvulnerable&&a.mayfly==old.mayFly&&a.flying==old.flying&&a.instabuild==old.instabuild&&a.mayBuild==old.mayBuild,"teardown restores exact borrowed native abilities including invulnerable="+old.abilitiesInvulnerable);require(p.gameMode()==old.mode&&p.noPhysics==old.noPhysics&&p.isNoGravity()==old.noGravity,"teardown restores native mode/physics");require(p.getInvulnerableTime()==old.invulnerable&&p.damageCooldownTime==old.damageCooldown,"teardown restores original damage immunity timers");require(p.entityTags().equals(old.tags),"teardown restores exact borrowed role/protection tags");}
    private static void equipmentUpdate(LivingEntity entity){try{var method=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");method.setAccessible(true);method.invoke(entity);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        ServerPlayer owner,beta;State saved,otherSaved;Client client,other;
        SauvojenMeditationChecks.Attribution attribution;
        net.minecraft.server.level.ServerLevel level;Vec3 origin;
        net.minecraft.world.scores.PlayerTeam team,ownerTeam,betaTeam;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Long> forced=new LinkedHashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Map<String,ItemStack> foods=new LinkedHashMap<>();
        int stage,start,phase;boolean closed,stopOwned,prepared;float useHealth;int useCold;ItemStack useStack;
        EvokerFangs fang;
        Fixture(GameController game){this.game=game;}
        void initialize(){
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(beta)&&owner.level()==beta.level(),"actual active native participants");
            require(owner.containerMenu==owner.inventoryMenu&&beta.containerMenu==beta.inventoryMenu&&!owner.isUsingItem()&&!beta.isUsingItem(),"borrowed participants idle with closed menus");
            require(!game.items().timeStop().stopped(owner.level())&&game.items().edibleSpells().snapshot().isEmpty()&&game.items().edibleSpells().polymorphCount()==0,"exclusive empty edible service and no borrowed TimeStop");
            require(game.items().frost().cold(owner)==0&&!game.items().frost().isFrozen(owner),"no borrowed owner cold state");
            saved=new State(game,owner);otherSaved=new State(game,beta);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta));
            level=owner.level();origin=new Vec3(Math.floor(owner.getX())+128.5,Math.min(224,level.getMaxY()-48),Math.floor(owner.getZ())+.5);
            for(var p:game.server().getPlayerList().getPlayers())if(p!=owner&&p!=beta&&p.level()==level)require(p.position().distanceToSqr(origin)>64*64,"isolated blast volume away from other native players");
            for(int x=(BlockPos.containing(origin).getX()-32)>>4;x<=(BlockPos.containing(origin).getX()+32)>>4;x++)for(int z=(BlockPos.containing(origin).getZ()-32)>>4;z<=(BlockPos.containing(origin).getZ()+32)>>4;z++){if(level.setChunkForced(x,z,true))forced.add(net.minecraft.world.level.ChunkPos.pack(x,z));level.getChunk(x,z);}
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin).offset(-12,-12,-12),BlockPos.containing(origin).offset(12,12,12))){require(level.getBlockState(pos).isAir(),"bounded empty native fixture volume "+pos);terrain.put(pos.immutable(),game.maps().snapshot(level,pos));}
            level.getAllEntities().forEach(before::add);
            ownerTeam=owner.getTeam();betaTeam=beta.getTeam();var board=game.server().getScoreboard();require(board.getPlayerTeam("edible_spell_fixture")==null,"exclusive fixture team");team=board.addPlayerTeam("edible_spell_fixture");team.setAllowFriendlyFire(false);board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
            client=new Client(game,owner);other=new Client(game,beta);prepared=true;reset();nativeFoods("rofls");nativeFoods("imba/buckets");
            for(var flag:List.of("eat_damage","eat_evoker","eat_poison","eat_regeneration","eat_weakness","eat_buffs","eat_haste","eat_chaos_explode","lava_bucket","snow_bucket"))require(foods.containsKey(flag),"actual authored native food payload "+flag);
            synchronous();prepareFang();start=game.server().getTickCount();phase=start;
        }
        @SuppressWarnings("unchecked") void nativeFoods(String id){
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).withOptionalRandomSeed(17).create(Optional.empty());
            var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools");require(!pools.isEmpty(),"native authored food source table "+id);
            for(var pool:pools)for(var source:pool.entries){var entries=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();source.expand(context,entries::add);for(var entry:entries){var outputs=new ArrayList<ItemStack>();entry.createItemStack(outputs::add,context);for(var item:outputs){var data=LbcFartus.data(item);int edible=0;for(var key:data.keySet())if(key.startsWith("eat_")&&data.getIntOr(key,0)==1)edible++;
                for(var key:data.keySet())if((key.startsWith("eat_")&&edible==1||key.equals("lava_bucket")||key.equals("snow_bucket"))&&data.getIntOr(key,0)==1){require(item.has(DataComponents.CONSUMABLE),"authored native consumable component "+key);foods.putIfAbsent(key,item.copy());}
            }}}
        }
        ItemStack food(String flag){var source=foods.get(flag);require(source!=null,"actual source food "+flag);var result=source.copy();result.setCount(2);require(ItemStack.isSameItemSameComponents(source,result),"immutable source food components retained "+flag);return result;}
        void reset(){
            owner.stopUsingItem();beta.stopUsingItem();game.items().edibleSpells().forget(owner);game.items().frost().clear(owner);
            for(var p:List.of(owner,beta)){
                p.removeAllEffects();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))p.setItemSlot(slot,ItemStack.EMPTY);
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setSprinting(false);p.setShiftKeyDown(false);
                for(var tag:List.of("time_stopped","have_stand","spectator","strict_map_object","rcdenyalways","magic_academy","sin_greed_active","nomagic_active","infinite_resistance","infinite_heal","infinite_saturation"))p.entityTags().remove(tag);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);
                equipmentUpdate(p);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);p.getAttribute(Attributes.ARMOR).setBaseValue(0);p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);p.setHealth(500);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.clearFire();p.getFoodData().setFoodLevel(1);
                p.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);
            }client.ack();other.ack();
        }
        ItemStack consume(String flag){var held=food(flag);owner.setItemInHand(InteractionHand.MAIN_HAND,held);var output=held.finishUsingItem(level,owner);require(output.getCount()==1&&held.getCount()==1,"exactly one native consume debit "+flag);return output;}
        void effect(Holder<MobEffect> type,int ticks,int amplifier,boolean visible){var effect=owner.getEffect(type);require(effect!=null&&effect.getDuration()==ticks&&effect.getAmplifier()==amplifier&&effect.isVisible()==visible,"native effect duration/amplifier/visibility "+type+" expected="+ticks+"/"+amplifier+"/"+visible+" actual="+(effect==null?"missing":effect.getDuration()+"/"+effect.getAmplifier()+"/"+effect.isVisible()));}
        List<ItemStack> inventoryStacks(ServerPlayer p){
            var out=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)out.add(p.getInventory().getItem(i));
            var craft=p.inventoryMenu.getCraftSlots();for(int i=0;i<craft.getContainerSize();i++)out.add(craft.getItem(i));out.add(p.containerMenu.getCarried());
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))out.add(p.getItemBySlot(slot));return out;
        }
        void protectedSlabInventories(){
            boolean skills=game.config().lbcSkills;
            try{
                reset();game.config().lbcSkills=true;
                for(var p:List.of(owner,beta)){
                    game.players().get(p.getUUID()).lbcSkill=2;
                    var sentinel=new ItemStack(Items.DIAMOND,7);sentinel.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("protected slab inventory"));
                    var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("slab_probe",73);sentinel.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
                    for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,sentinel.copy());
                    var craft=p.inventoryMenu.getCraftSlots();for(int i=0;i<craft.getContainerSize();i++)craft.setItem(i,sentinel.copy());p.containerMenu.setCarried(sentinel.copy());
                    var refs=inventoryStacks(p);var exact=refs.stream().map(ItemStack::copy).toList();
                    // Exercise the actual player branch without the source's dimension-wide entity destruction.
                    game.items().edibleSpells().slabInventory(p);var after=inventoryStacks(p);
                    for(int i=0;i<exact.size();i++)require(after.get(i)==refs.get(i)&&ItemStack.matches(after.get(i),exact.get(i)),"protected caster/other slab target retains every slot/craft/cursor/equipment reference, count and component "+i);
                }
                game.players().get(beta.getUUID()).lbcSkill=0;game.items().edibleSpells().slabInventory(beta);
                for(int i=0;i<36;i++)require(!beta.getInventory().getItem(i).isEmpty()&&!beta.getInventory().getItem(i).is(Items.DIAMOND),"unprotected slab inventory still replaced "+i);
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND))require(!beta.getItemBySlot(slot).isEmpty()&&!beta.getItemBySlot(slot).is(Items.DIAMOND),"unprotected slab equipment replaced "+slot);
                var craft=beta.inventoryMenu.getCraftSlots();for(int i=0;i<craft.getContainerSize();i++)require(!craft.getItem(i).isEmpty()&&!craft.getItem(i).is(Items.DIAMOND),"unprotected slab craft replaced "+i);
                require(!beta.containerMenu.getCarried().isEmpty()&&!beta.containerMenu.getCarried().is(Items.DIAMOND),"unprotected slab cursor replaced");
                game.config().lbcSkills=false;game.items().edibleSpells().slabInventory(owner);
                require(!owner.getInventory().getItem(0).is(Items.DIAMOND),"disabled skill2 preserves ordinary slab replacement");
            }finally{game.config().lbcSkills=skills;reset();}
        }
        void synchronous(){
            protectedSlabInventories();
            canonicalBuckets();
            reset();consume("eat_damage");require(owner.getHealth()==485,"actual native eat_damage callback WILD self-arrow15 despite no-friendly-fire team");require(owner.getLastDamageSource()!=null&&owner.getLastDamageSource().is(DamageTypes.ARROW),"retains native arrow damage type");
            reset();consume("eat_poison");effect(MobEffects.POISON,200,4,true);
            // Shared independent layers retain hidden food Regen VI above visible native golden-apple Regen II.
            reset();consume("eat_regeneration");effect(MobEffects.REGENERATION,200,5,false);
            var activeRegen=owner.getEffect(MobEffects.REGENERATION);
            require(!activeRegen.showIcon(),"source high regeneration icon stays hidden");
            var appleRegen=((EffectLayers.Access)activeRegen).skywars2$layerHidden();
            require(appleRegen!=null&&appleRegen.getAmplifier()==1&&appleRegen.getDuration()==100&&appleRegen.isVisible()&&appleRegen.showIcon(),"native golden-apple regeneration retains its independent lower layer");
            reset();consume("eat_weakness");effect(MobEffects.WEAKNESS,600,9,false);
            reset();consume("eat_buffs");for(var e:List.of(MobEffects.INVISIBILITY,MobEffects.SPEED,MobEffects.JUMP_BOOST))effect(e,200,1,false);
            reset();consume("eat_haste");effect(MobEffects.HASTE,1200,127,true);
            reset();var ordinary=new ItemStack(Items.GOLDEN_APPLE,2);ordinary.finishUsingItem(level,owner);require(ordinary.getCount()==1&&!owner.hasEffect(MobEffects.HASTE)&&!owner.hasEffect(MobEffects.POISON),"ordinary native food does not acquire raw custom payload");
            reset();owner.getAbilities().invulnerable=true;beta.getAbilities().invulnerable=true;consume("eat_chaos_explode");
            var children=game.items().edibleSpells().snapshot();require(children.stream().filter(FireworkRocketEntity.class::isInstance).count()==1&&children.stream().filter(e->e.getType()==EntityTypes.LIGHTNING_BOLT).count()==1,"one original native chaos rocket and lightning");
            var rocket=(FireworkRocketEntity)children.stream().filter(FireworkRocketEntity.class::isInstance).findFirst().orElseThrow();require(rocket.getOwner()==owner&&game.items().edibleSpells().owner(rocket)==owner&&game.items().currentEpoch(rocket),"native delayed rocket causal owner and epoch before release");
            var fireworks=rocket.getItem().get(DataComponents.FIREWORKS);require(fireworks!=null&&fireworks.explosions().size()==5&&rocket.getItem().getCount()==1,"original one-item five-explosion native rocket component");
            int[] colors={16745510,7405414,5857279,16728793,12069375},fades={16767065,7733201,10769663,16724828,13303741};String[] shapes={"small_ball","large_ball","star","creeper","burst"};
            for(int n=0;n<5;n++){var e=fireworks.explosions().get(n);require(e.shape().getSerializedName().equals(shapes[n])&&e.colors().equals(it.unimi.dsi.fastutil.ints.IntList.of(colors[n]))&&e.fadeColors().equals(it.unimi.dsi.fastutil.ints.IntList.of(fades[n]))&&e.hasTrail()&&e.hasTwinkle(),"immutable original firework explosion payload "+n);}
            for(var e:created())require(e.getType()!=EntityTypes.CREEPER&&!(e instanceof PrimedTnt),"no native-world creeper or fuse0 TNT carrier remains");
            game.items().edibleSpells().forget(owner);require(rocket.isRemoved()&&game.items().edibleSpells().snapshot().isEmpty(),"owned native child cleanup before any delayed rocket/lightning tick");
            reset();
        }
        void canonicalBuckets(){
            int tested=0;
            for(var entry:LbcNamedFoodItems.entries())if(entry.id().startsWith("edible_")&&entry.carrier()==Items.SALMON){
                reset();var rolled=game.lbc().rollTable(level,origin,owner,"lbc:ported/food/"+entry.id());
                require(rolled.size()==1&&rolled.getFirst().is(entry.item()),"canonical native bucket "+entry.id());
                var held=rolled.getFirst().copyWithCount(2);owner.setItemInHand(InteractionHand.MAIN_HAND,held);
                var result=held.finishUsingItem(level,owner);
                require(result==held&&held.getCount()==1,"one native consumed bucket without vanilla bucket remainder "+entry.id());
                var outputs=game.items().edibleSpells().snapshot();
                require(outputs.size()==1&&outputs.getFirst() instanceof net.minecraft.world.entity.item.ItemEntity,"one shared gain_bucket output "+entry.id());
                var bucket=(net.minecraft.world.entity.item.ItemEntity)outputs.getFirst();
                require(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(bucket.getItem().getItem()).equals(Identifier.parse("skywars2:bucket_projectile"))&&bucket.getItem().getCount()==1&&game.items().edibleSpells().owner(bucket)==owner,"returned throwing bucket has exact item/count/author "+entry.id());
                game.items().edibleSpells().forget(owner);require(bucket.isRemoved(),"owned returned bucket cleanup");tested++;
            }
            require(tested==10,"all ten native edible buckets finish through the same consumer");reset();
        }
        List<Entity> created(){var out=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(!before.contains(e)&&e!=owner&&e!=beta)out.add(e);});return out;}
        void prepareFang(){reset();consume("eat_evoker");fang=(EvokerFangs)game.items().edibleSpells().snapshot().stream().filter(EvokerFangs.class::isInstance).findFirst().orElseThrow();require(fang.getOwner()==owner&&game.items().currentEpoch(fang),"real native edible fang causal owner/epoch");owner.removeAllEffects();owner.setAbsorptionAmount(0);owner.setHealth(500);beta.setHealth(500);game.items().timeStop().begin(level,100);stopOwned=true;require(game.items().timeStop().frozen(fang),"actual native TimeStop captures delayed fang");}
        boolean step(){require(game.server().getTickCount()-start<200,"bounded real native tick timeout");client.ack();other.ack();int elapsed=game.server().getTickCount()-phase;
            if(stage==0){if(elapsed<5)return false;require(owner.getHealth()==500&&beta.getHealth()==500,"native fang does no damage while actual TimeStop pauses it");game.items().timeStop().end(level);stopOwned=false;phase=game.server().getTickCount();stage++;return false;}
            if(stage==1){if(owner.getHealth()==500||beta.getHealth()==500){require(elapsed<40,"native fang actual attack tick damages self and ally");return false;}require(owner.getHealth()==494&&beta.getHealth()==494,"native WILD fang6 hits caster and ally on no-friendly-fire team");game.items().edibleSpells().forget(owner);require(fang.isRemoved(),"native fang owned cleanup");beginUse("lava_bucket");stage++;return false;}
            if(stage==2){if(owner.getHealth()==useHealth){require(elapsed<10,"actual native ongoing lava use trigger");return false;}require(useStack.getCount()==2&&owner.isUsingItem(),"lava USING hurts before completion without consuming item");owner.stopUsingItem();float after=owner.getHealth();var result=useStack.finishUsingItem(level,owner);require(result.getCount()==1&&owner.getHealth()==after,"completed lava food consumes once without duplicate USING harm");beginUse("snow_bucket");stage++;return false;}
            if(stage==3){if(game.items().frost().cold(owner)<=useCold){require(elapsed<10,"actual native ongoing snow use trigger");return false;}require(useStack.getCount()==2&&owner.isUsingItem(),"snow USING adds cold before completion without item debit");owner.stopUsingItem();int after=game.items().frost().cold(owner);var result=useStack.finishUsingItem(level,owner);require(result.getCount()==1&&game.items().frost().cold(owner)==after,"completed snow food consumes once without duplicate cold callback");return true;}
            throw new IllegalStateException("invalid edible fixture stage");
        }
        void beginUse(String flag){reset();useStack=food(flag);owner.setItemInHand(InteractionHand.MAIN_HAND,useStack);useHealth=owner.getHealth();useCold=game.items().frost().cold(owner);require(owner.gameMode.useItem(owner,level,useStack,InteractionHand.MAIN_HAND).consumesAction()&&owner.isUsingItem(),"native actual food-use admission "+flag);require(useStack.getCount()==2,"starting native use preserves count "+flag);phase=game.server().getTickCount();}
        public void close(){if(closed)return;closed=true;try{
            if(stopOwned){game.items().timeStop().end(level);stopOwned=false;}
            if(prepared){owner.stopUsingItem();for(var e:game.items().edibleSpells().snapshot())if(game.items().edibleSpells().owner(e)==owner){game.items().entityUnloaded(e);if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}game.items().edibleSpells().forget(owner);game.items().frost().clear(owner);}
            if(level!=null){for(var e:terrain.entrySet())game.maps().restoreSnapshot(level,e.getKey(),e.getValue());for(var e:terrain.entrySet())require(game.maps().snapshot(level,e.getKey()).equals(e.getValue()),"bounded native terrain fully restored "+e.getKey());}
        }finally{try{
            if(team!=null){var board=game.server().getScoreboard();board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);}
            if(saved!=null)saved.restore(game);if(otherSaved!=null)otherSaved.restore(game);if(client!=null)client.ack();if(other!=null)other.ack();if(attribution!=null)attribution.close();if(saved!=null)restored(saved);if(otherSaved!=null)restored(otherSaved);
        }finally{if(client!=null)client.close();if(other!=null)other.close();if(level!=null)for(long key:forced)level.setChunkForced((int)key,(int)(key>>32),false);}}
        }
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive native edible fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS edible-spells (actual authored native food components/consumption, one debit, USING cancellation/completion, effect clamps, native self/allied fang after real TimeStop, WILD self-arrow, exact bounded chaos rocket and native cleanup; no full25 or peak load qualification)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL edible-spells",error);}}
}
