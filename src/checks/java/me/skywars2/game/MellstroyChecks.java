package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Bounded real participant, native loot/crafting/economy and whole-coordinator scenes. */
public final class MellstroyChecks {
    private MellstroyChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Mellstroy: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){require(running==null,"exclusive fixture");var f=new Fixture(game,rewind);running=f;try{f.begin();}catch(Throwable e){running=null;try{f.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{require(game==f.game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} (native source mask, canonical economy, equipped layers and typed queued replay)",f.rewind?"mellstroy-rewind":"mellstroy-native");}catch(Throwable e){running=null;try{f.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL "+(f.rewind?"mellstroy-rewind":"mellstroy-native"),e);}}
    private static Object get(Object value,String name){for(var c=value.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(NoSuchFieldException ignored){}catch(IllegalAccessException e){throw new IllegalStateException(e);}throw new IllegalStateException(name);}
    private static boolean translated(Component c,String key){return c.getContents() instanceof TranslatableContents t&&t.getKey().equals(key)||c.getSiblings().stream().anyMatch(s->translated(s,key));}
    private static ItemStack troll(int count){var stack=new ItemStack(Items.CARROT_ON_A_STICK,count);var data=new net.minecraft.nbt.CompoundTag();data.putInt("troll",1);data.putInt("craftusable",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
    private static final class Fixture implements AutoCloseable {
        private record Cursor(Vec3 position,double remaining,boolean started){}
        final GameController game;final boolean rewind;final ServerPlayer owner,other;final ServerLevel level;final Vec3 origin;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final SauvojenMeditationChecks.Attribution attribution;final LbcMellstroy service;final LbcMellstroy.State original;
        final LbcMellstroyCrafting.Snapshot craftingOriginal;final Set<UUID> existing=new HashSet<>();final List<Entity> actors=new ArrayList<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final boolean frozen;
        Map<UUID,Cursor> capturedCursors;
        net.minecraft.world.scores.Objective createdIdentity;
        int started,stage,when;boolean stopped,closed,restoring;ItemStack mask;Cow target,behind,ally;LbcMellstroy.State captured;int expectedCoins,expectedMem;boolean expectedWin;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"two actual participants and exclusive coordinator");
            require(!game.items().timeStop().stopped(level)&&game.items().exasperato().pendingSearches()==0,"unpaused empty ray family");
            service=game.items().mellstroy();original=service.state();require(original.wearers().isEmpty()&&original.pending().isEmpty(),"exclusive mask owner");craftingOriginal=game.items().mellstroyCrafting().snapshot();
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));
            level.getAllEntities().forEach(e->existing.add(e.getUUID()));origin=new Vec3(owner.getX(),Math.min(240,level.getMaxY()-20),owner.getZ());frozen=game.server().tickRateManager().isFrozen();
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);started=game.server().getTickCount();
        }
        void move(ServerPlayer p,Vec3 at,float yaw,float pitch){require(p.teleportTo(level,at.x,at.y,at.z,Set.of(),yaw,pitch,true),"native fixture teleport");p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);if(p==owner)client.acknowledge();else audience.acknowledge();}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        void next(int value){stage=value;when=game.server().getTickCount();}
        void empty(){owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);service.inventoryChanged(owner);}
        void begin(){
            game.server().tickRateManager().setFrozen(false);
            for(var p:List.of(owner,other)){p.closeContainer();p.stopUsingItem();game.lbc().clear(p);p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;game.items().mana().noMagic(p,0);game.items().magic().inventoryChanged(p);}
            move(owner,origin,0,0);move(other,origin.add(12,0,0),0,0);game.items().magic().flushCleansing();
            mask=LbcMellstroyCrafting.result(game);presentation();bonusSampler();
            if(rewind){beginReplay();return;}
            crafting();casinoBoth();empty();owner.setItemSlot(EquipmentSlot.HEAD,mask.copy());service.inventoryChanged(owner);service.tick();require(service.equipped(owner),"actual armor slot activates mask");
            require(owner.hasEffect(MobEffects.REGENERATION)&&owner.hasEffect(MobEffects.RESISTANCE)&&owner.hasEffect(MobEffects.SPEED)&&owner.hasEffect(MobEffects.HASTE)&&owner.hasEffect(MobEffects.JUMP_BOOST)&&owner.getEffect(MobEffects.JUMP_BOOST).getAmplifier()==1&&owner.hasEffect(MobEffects.SATURATION),"all six source owner buff layers");
            service.forget(owner);target=cow(origin.add(0,.5,3));behind=cow(origin.add(0,.5,7));ally=cow(origin.add(3,0,0));var identity=game.server().getScoreboard().getObjective("lbcID2");if(identity==null)identity=createdIdentity=game.server().getScoreboard().addObjective("lbcID2",net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,Component.literal("Mellstroy scoped optional legacy ally identity"),net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,false,null);var own=game.server().getScoreboard().getOrCreatePlayerScore(owner,identity,true);game.server().getScoreboard().getOrCreatePlayerScore(ally,identity,true).set(own.get());var wall=BlockPos.containing(origin.add(0,owner.getEyeHeight(),5));remember(wall);require(level.setBlock(wall,Blocks.STONE.defaultBlockState(),2),"first wall native placement");
            service.inventoryChanged(owner);service.tick();next(0);
        }
        void presentation(){
            require(mask.is(LbcMellstroyMaskItem.ITEM)&&LbcMellstroy.mask(mask)&&LbcMellstroyMaskItem.isMask(mask)&&mask.getCount()==1,"actual source loot becomes canonical singleton");
            require(mask.getMaxDamage()==5000&&mask.get(DataComponents.EQUIPPABLE).slot()==EquipmentSlot.HEAD,"source durability/head slot");
            var attrs=mask.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(attrs.size()==4,"source four attributes");
            var amounts=new ArrayList<Double>();for(var a:attrs)amounts.add(a.modifier().amount());Collections.sort(amounts);require(amounts.equals(List.of(8d,20d,100d,100d)),"source20/8/100/100 attribute amounts");
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)client.connection;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(mask,TooltipFlag.NORMAL,provider.getPacketContext(),owner.registryAccess());require(proxy.is(Items.CARROT_ON_A_STICK)&&Objects.equals(mask.get(DataComponents.ITEM_MODEL),proxy.get(DataComponents.ITEM_MODEL)),"recipient source model");require(ItemStack.matches(mask,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),owner.registryAccess())),"recipient all-component roundtrip");});
        }
        @SuppressWarnings("unchecked") void bonusSampler(){
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:luckyblock")));
            var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)get(table,"pools");
            var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var serialized=net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC.encodeStart(ops,table).getOrThrow().getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
            int selected=-1;for(int n=0;n<serialized.size();n++){var entry=serialized.get(n).getAsJsonObject();if(entry.has("value")&&entry.get("value").isJsonPrimitive()&&entry.get("value").getAsString().equals("lbc:lucky_items_high_quality")&&entry.has("weight")&&entry.get("weight").getAsInt()==10000){require(selected<0,"unique loaded source HQ10000 branch");selected=n;}}
            require(selected>=0&&!pools.isEmpty()&&selected<pools.getFirst().entries.size(),"loaded native luckyblock retains source conditional HQ weight10000");var branch=pools.getFirst().entries.get(selected);
            try{
                for(int mode=0;mode<3;mode++){
                    owner.setItemSlot(EquipmentSlot.HEAD,mode==0?ItemStack.EMPTY:mask.copy());owner.getInventory().setItem(9,mode==2?new ItemStack(LbcMagic.MYSTICAL_ARTIFACT):ItemStack.EMPTY);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();if(mode==2)require(game.items().magic().suppressed(owner),"HQ sampler actual owner MAG positive control");
                    var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,owner.position()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
                    var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).withOptionalRandomSeed(17).create(Optional.empty());var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();branch.expand(context,expanded::add);
                    require(mode==0?expanded.isEmpty():expanded.size()==1&&expanded.getFirst().getWeight(context.getLuck())==10000,"actual native THIS_ENTITY HQ branch: no mask excluded, equipped native mask admitted, MAG retains10000 bonus mode="+mode);
                }
            }finally{owner.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);owner.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}
        }
        void crafting(){
            var input=CraftingInput.of(2,2,List.of(troll(2),ItemStack.EMPTY,ItemStack.EMPTY,troll(3)));require(LbcMellstroyCrafting.matches(input),"two distinct source troll inputs");require(!LbcMellstroyCrafting.matches(CraftingInput.of(2,2,List.of(troll(2),ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY))),"one stack is never two ingredients");
            var rng=MatchRandomState.capture(level.getRandom());long draw=level.getRandom().nextLong();rng.restore(level.getRandom());for(int n=0;n<5;n++)require(ItemStack.matches(mask,LbcMellstroyCrafting.result(game)),"stable native source preview");require(level.getRandom().nextLong()==draw,"preview consumes no world RNG");rng.restore(level.getRandom());
            for(boolean bench:List.of(false,true))for(boolean shift:List.of(false,true)){
                empty();if(bench){var table=BlockPos.containing(origin.add(1,0,0));remember(table);level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),2);owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new CraftingMenu(id,inv,ContainerLevelAccess.create(level,table)),Component.literal("Mellstroy native fixture")));}
                var menu=(AbstractCraftingMenu)owner.containerMenu;var slots=menu.getInputGridSlots();int second=bench?8:3;slots.get(0).setByPlayer(troll(2));slots.get(second).setByPlayer(troll(3));menu.slotsChanged(owner.getInventory());require(LbcMellstroy.mask(menu.getResultSlot().getItem()),"actual native2x2/3x3 source result slot");
                if(shift){for(int n=0;n<36;n++)owner.getInventory().setItem(n,new ItemStack(Items.COBBLESTONE,64));GameChecks.inventoryClick(owner,0,0,ContainerInput.QUICK_MOVE);require(slots.get(0).getItem().getCount()==2&&slots.get(second).getItem().getCount()==3,"full inventory shift cannot debit");owner.getInventory().setItem(9,ItemStack.EMPTY);}
                pause();GameChecks.inventoryClick(owner,0,0,ContainerInput.PICKUP);require(slots.get(0).getItem().getCount()==2&&slots.get(second).getItem().getCount()==3,"TimeStop blocks actual result take");thaw();
                GameChecks.inventoryClick(owner,0,0,shift?ContainerInput.QUICK_MOVE:ContainerInput.PICKUP);
                require(slots.get(0).getItem().getCount()==1&&slots.get(second).getItem().getCount()==2,"native take consumes exactly one from each distinct slot");require(shift?owner.getInventory().contains(LbcMellstroy::mask):LbcMellstroy.mask(menu.getCarried()),"native output delivery");for(var slot:slots)slot.set(ItemStack.EMPTY);menu.setCarried(ItemStack.EMPTY);owner.closeContainer();
            }
            empty();var a=drop(troll(1),origin.add(2,0,0));var b=drop(troll(3),origin.add(2.5,0,0));game.items().mellstroyCrafting().restore(new LbcMellstroyCrafting.Snapshot(19));pause();game.items().mellstroyCrafting().tick();require(a.getItem().getCount()==1&&b.getItem().getCount()==3,"ground TimeStop retains both donors");thaw();game.items().mellstroyCrafting().tick();require(a.isRemoved()&&b.getItem().getCount()==2,"ground consumes singleton and retains remaining donor stack");
            var output=level.getEntitiesOfClass(ItemEntity.class,a.getBoundingBox().inflate(1),e->LbcMellstroy.mask(e.getItem()));require(output.size()==1&&output.getFirst().getItem().getCount()==1,"atomic ground publication one source output");actors.addAll(output);a.discard();b.discard();output.forEach(Entity::discard);
        }
        ItemEntity drop(ItemStack stack,Vec3 at){var e=new ItemEntity(level,at.x,at.y,at.z,stack);e.setNoGravity(true);e.setDeltaMovement(Vec3.ZERO);e.setPickUpDelay(10000);game.items().markCurrentItemEpoch(e);require(level.addFreshEntity(e),"actual ground ingredient publication");game.items().mellstroyCrafting().loaded(e);actors.add(e);return e;}
        void seed(long seed){var s=service.state();service.restore(new LbcMellstroy.State(s.wearers(),s.dirty(),s.pending(),s.listeners(),MatchRandomState.capture(RandomSource.create(seed)),s.counters()));}
        Map<UUID,Cursor> cursors(){var result=new LinkedHashMap<UUID,Cursor>();service.state().pending().forEach((id,ticket)->{var search=get(ticket,"search");result.put(id,new Cursor((Vec3)get(search,"position"),(Double)get(search,"remaining"),(Boolean)get(search,"started")));});return result;}
        PlayerStore.Profile profile(){return game.players().get(owner.getUUID());}
        @SuppressWarnings("unchecked") List<net.minecraft.network.protocol.Packet<?>> packets(){return (List<net.minecraft.network.protocol.Packet<?>>)get(client.connection,"packets");}
        void casinoBoth(){
            empty();owner.setItemInHand(InteractionHand.MAIN_HAND,mask.copy());boolean won=false,lost=false;
            for(long seed=0;seed<100&&(!won||!lost);seed++){boolean win=RandomSource.create(seed).nextInt(100)<70;if(win?won:lost)continue;seed(seed);profile().coins=11;profile().memcoins=17;packets().clear();
                require(!service.use(owner,InteractionHand.OFF_HAND)&&profile().coins==11&&profile().memcoins==17,"offhand cannot gamble");require(service.use(owner,InteractionHand.MAIN_HAND),"actual native casino accepted");require(profile().coins==(win?27:0)&&profile().memcoins==(win?42:0),"same source70/30 draw governs both signed integer5/2 balances");
                var messages=packets().stream().filter(ClientboundSystemChatPacket.class::isInstance).map(ClientboundSystemChatPacket.class::cast).map(ClientboundSystemChatPacket::content).toList();
                if(win){require(messages.stream().anyMatch(c->translated(c,"coins")&&c.getString().contains("16"))&&messages.stream().anyMatch(c->translated(c,"memcoins")&&c.getString().contains("25")),"actual winning messages use respective currency deltas");won=true;}else{require(messages.stream().anyMatch(c->translated(c,"lost_all_coins"))&&messages.stream().anyMatch(c->translated(c,"lost_all_memcoins")),"actual loss feedback for both currencies");lost=true;}
                require(ItemStack.matches(mask,owner.getMainHandItem()),"casino neither consumes nor damages mask");
            }require(won&&lost,"both deterministic native casino outcomes observed");
        }
        Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);require(level.addFreshEntity(c),"native living witness");actors.add(c);return c;}
        void remember(BlockPos p){terrain.putIfAbsent(p.immutable(),game.maps().snapshot(level,p));}
        void restoreIdentity(){if(createdIdentity!=null){require(game.server().getScoreboard().getObjective("lbcID2")==createdIdentity,"same scoped optional legacy objective at cleanup");game.server().getScoreboard().removeObjective(createdIdentity);}}
        Marker field(boolean tech,Vec3 at){var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().add(tech?"stopper":"stopper_magic");remember(marker.blockPosition());require(level.addFreshEntity(marker),"actual native jammer publication");actors.add(marker);game.items().magic().loaded(marker);return marker;}
        void removeField(Marker marker){game.items().magic().unloaded(marker);marker.discard();var old=terrain.get(marker.blockPosition());if(old!=null)game.maps().restoreSnapshot(level,marker.blockPosition(),old,2);game.items().magic().blockChanged(marker.blockPosition(),level.getBlockState(marker.blockPosition()));}
        void beginReplay(){
            empty();owner.setItemSlot(EquipmentSlot.HEAD,mask.copy());service.inventoryChanged(owner);move(owner,origin,0,-90);service.tick();game.items().exasperato().tickSearchesWhileMapStopped();
            require(service.counters().samples()>original.counters().samples()&&!service.state().pending().isEmpty(),"actual fair queue contains partially sampled central beam");
            owner.setItemInHand(InteractionHand.MAIN_HAND,mask.copy());profile().coins=11;profile().memcoins=17;pause();require(game.vanity().requestCapture(owner,false),"whole coordinator accepts equipped music/partial beam/economy/RNG");next(10);
        }
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<260,"bounded normally ticking fixture");
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(rewind)return replayStep();
            if(stage==0){if(service.counters().walls()==original.counters().walls())return false;require(target.hasEffect(MobEffects.POISON)&&target.hasEffect(MobEffects.BLINDNESS),"actual source hostile aura");require(!ally.hasEffect(MobEffects.POISON)&&!ally.hasEffect(MobEffects.BLINDNESS)&&ally.getHealth()==1000,"source same-ID ally excludes hostile aura");require(behind.getHealth()==1000,"first wall protects witness behind wall");require(service.counters().attempts()>original.counters().attempts()&&target.getHealth()<1000&&target.getHealth()>999,"central ray ordinary0.05 damage retains native hurt cooldown");
                service.forget(owner);owner.removeAllEffects();target.removeAllEffects();var protection=field(false,target.position().add(0,0,19.5));require(game.items().magic().suppressedAt(level,target.position())&&!game.items().magic().suppressed(owner),"actual MAG protects target while owner stays outside radius");service.inventoryChanged(owner);service.tick();require(owner.hasEffect(MobEffects.SPEED)&&!target.hasEffect(MobEffects.POISON)&&!target.hasEffect(MobEffects.BLINDNESS),"target MAG excludes hostile aura while owner positive layers remain");removeField(protection);service.forget(owner);
                owner.removeAllEffects();var tech=field(true,owner.position());require(game.items().magic().mechanismSuppressed(owner),"actual TECH positive control");long rays=service.counters().rays();service.inventoryChanged(owner);service.tick();require(service.counters().rays()==rays&&owner.hasEffect(MobEffects.SPEED),"TECH suppresses eye ray while retaining magical buffs");owner.setItemInHand(InteractionHand.MAIN_HAND,mask.copy());require(service.use(owner,InteractionHand.MAIN_HAND),"casino remains admitted under TECH");owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);removeField(tech);service.forget(owner);
                owner.removeAllEffects();owner.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();require(game.items().magic().suppressed(owner),"real MAG positive control");service.inventoryChanged(owner);service.tick();require(service.equipped(owner)&&!owner.hasEffect(MobEffects.SPEED)&&!owner.hasEffect(MobEffects.REGENERATION),"MAG suppresses equipped positive layers without deleting ownership");
                require(service.use(owner,InteractionHand.MAIN_HAND)==false,"empty main hand is no casino input");owner.setItemInHand(InteractionHand.MAIN_HAND,mask.copy());require(service.use(owner,InteractionHand.MAIN_HAND),"casino remains admitted under MAG");return true;
            }throw new IllegalStateException("Unknown native phase");
        }
        boolean replayStep(){
            if(stage==10){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;captured=service.state();capturedCursors=cursors();require(capturedCursors.values().stream().allMatch(c->c.started()&&c.remaining()>0&&c.remaining()<342),"captured actual partial central beam cursor");require(!captured.pending().isEmpty()&&captured.wearers().containsKey(owner.getUUID())&&!captured.listeners().isEmpty(),"whole snapshot retains nonempty native beam and music ownership");
                thaw();require(service.use(owner,InteractionHand.MAIN_HAND),"actual future casino draw");expectedCoins=profile().coins;expectedMem=profile().memcoins;expectedWin=service.counters().wins()>captured.counters().wins();pause();service.stop();profile().coins=777;profile().memcoins=888;require(game.vanity().requestRestoreForCheck(),"actual coordinator restore after paid future");restoring=true;next(11);return false;}
            if(game.vanity().busy())return false;
            require(service.state().wearers().equals(captured.wearers())&&service.state().pending().keySet().equals(captured.pending().keySet())&&cursors().equals(capturedCursors),"whole coordinator restores equipped selection and exact pending beam position/remaining/started cursor");require(service.counters().equals(captured.counters())&&profile().coins==11&&profile().memcoins==17,"whole coordinator restores sample cursor counters and canonical balances");require(game.items().timeStop().stopped(level),"whole restore retains source pause");game.vanity().stop();thaw();require(service.use(owner,InteractionHand.MAIN_HAND),"replayed actual native casino draw");require(profile().coins==expectedCoins&&profile().memcoins==expectedMem&&service.counters().casinos()==captured.counters().casinos()+1&&(service.counters().wins()>captured.counters().wins())==expectedWin,"restored RNG gives same once-only canonical casino payout");return true;
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;for(Runnable action:List.<Runnable>of(()->{if(rewind)game.vanity().stop();},()->{service.stop();if(stopped)thaw();},()->{var remove=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e.getUUID())&&!(e instanceof ServerPlayer))remove.add(e);for(var e:remove){game.server().getScoreboard().resetAllPlayerScores(e);game.items().magic().unloaded(e);if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}},()->terrain.forEach((p,s)->{game.maps().restoreSnapshot(level,p,s,2);game.items().magic().blockChanged(p,level.getBlockState(p));}),()->{owner.closeContainer();other.closeContainer();saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();service.restore(original);game.items().mellstroyCrafting().restore(craftingOriginal);game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();game.players().save();},this::restoreIdentity,client::close,audience::close,attribution::close,()->game.server().tickRateManager().setFrozen(frozen)))try{action.run();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}if(failure!=null)throw new IllegalStateException("Mellstroy scoped cleanup",failure);}
    }
}
