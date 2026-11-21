package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.BoundViewCheckpointAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;

/** Disposable native match checks; shared pool membership/uniformity has its own structural oracle. */
public final class BucksTraderChecks {
    private static Scene running;
    private static boolean nativeHooks;
    private BucksTraderChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bucks trader: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive source-event fixture");
        var owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        require(game.playing()&&game.canUseGameplayItems(owner)&&!owner.isUsingItem()&&owner.containerMenu==owner.inventoryMenu,"active native participant with closed hands/menu");
        require(!(LbcBucksTrader.TOKEN instanceof SnowballItem),"money RMB uses a real native Item without projectile transport");
        var saved=new RegenerationBeltChecks.Snapshot(game,owner);var random=MatchRandomState.capture(owner.getRandom());
        var levelRandom=MatchRandomState.capture(owner.level().getRandom());var used=Stats.ITEM_USED.get(LbcBucksTrader.TOKEN);int uses=owner.getStats().getValue(used);
        var created=new ArrayList<Entity>();var level=owner.level();var area=new AABB(owner.position(),owner.position()).inflate(16);
        var previous=new HashSet<UUID>();for(var entity:level.getEntities((Entity)null,area))previous.add(entity.getUUID());
        try{
            template(game,owner);
            int accepted=0;
            for(var hand:InteractionHand.values()){
                var rewards=game.lbc().rollTable(level,owner.position(),owner,"lbc:imba/random_bucks");
                require(rewards.size()==1&&rewards.getFirst().is(LbcBucksTrader.TOKEN)&&LbcBucksTrader.matches(rewards.getFirst()),"canonical native currency identity");
                var stack=rewards.getFirst().copyWithCount(3);owner.setItemInHand(hand,stack);
                var opposite=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;owner.setItemInHand(opposite,new ItemStack(Items.STICK,7));
                var at=owner.position();int beforeTick=game.server().getTickCount();var beforeSnowballs=new HashSet<UUID>();
                for(var snowball:level.getEntitiesOfClass(net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball.class,area))beforeSnowballs.add(snowball.getUUID());
                require(owner.gameMode.useItem(owner,level,stack,hand).consumesAction()&&stack.getCount()==2&&owner.getItemInHand(opposite).getCount()==7,"native used-hand input and sole one-unit debit");
                require(game.server().getTickCount()==beforeTick&&owner.getStats().getValue(used)==uses+(++accepted),"accepted native RMB publishes and debits/stat-counts synchronously in the same tick");
                require(level.getEntitiesOfClass(net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball.class,area).stream().allMatch(e->beforeSnowballs.contains(e.getUUID())),"immediate money input creates no native snowball or deferred collision actor");
                var spawned=level.getEntities((Entity)null,new AABB(at,at).inflate(1)).stream().filter(e->!previous.contains(e.getUUID())&&!created.contains(e)).toList();
                created.addAll(spawned);spawned.forEach(e->previous.add(e.getUUID()));
                var traders=spawned.stream().filter(Villager.class::isInstance).map(Villager.class::cast).toList();
                var drops=spawned.stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).toList();
                require(traders.size()==1&&drops.size()==2&&drops.stream().allMatch(e->LbcBucksTrader.matches(e.getItem())&&e.getItem().getCount()>=1&&e.getItem().getCount()<=64&&e.hasPickUpDelay()),"one trader and two independent source1..64 currency rewards with native loot-spawn pickup delay");
                require(traders.getFirst().position().distanceToSqr(at)<1.0E-12&&drops.stream().allMatch(e->e.position().distanceToSqr(at)<1.0E-12),"trader and both immediate rewards use the executor's actual RMB position without flight or raycast offset");
                var trader=traders.getFirst();require(trader.entityTags().contains("luck_trader")&&trader.entityTags().contains("spawned")&&trader.isPersistenceRequired(),"authored merchant role and persistence");
                var traderData=trader.getVillagerData();
                require(trader.getHealth()==1_000_000F&&trader.getMaxHealth()==1_000_000F
                        &&trader.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue()==1_000_000D,"actual native trader source health/max-health one million");
                require(traderData.level()==99&&net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(traderData.profession().value()).equals(Identifier.parse("minecraft:mason"))
                        &&net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE.getKey(traderData.type().value()).equals(Identifier.parse("minecraft:swamp")),"actual native mason/swamp/level99 source data");
                var traderOutput=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());trader.saveWithoutId(traderOutput);
                var traderSaved=traderOutput.buildResult();
                require(trader.getVillagerXp()==9_999_999&&traderSaved.getIntOr("Xp",-1)==9_999_999&&traderSaved.getLongOr("LastRestock",-1)==1L
                        &&traderSaved.getIntOr("RestocksToday",-1)==0,"native merchant XP/last-restock/default stock-state save fields");
                require(traderSaved.getStringOr("DeathLootTable","").equals("lbc:imba/random_bucks"),"native saved death-loot owner uses the real source table field");
                var offers=((BoundViewCheckpointAccess.Villager)trader).skywars2$offers();require(offers.size()==10,"all ten authored offers");
                for(int i=0;i<offers.size();i++){
                    var offer=offers.get(i);var buy=offer.getBaseCostA();var sell=offer.getResult();
                    require(!buy.isEmpty()&&!sell.isEmpty()&&offer.getUses()==1&&offer.getMaxUses()==Integer.MAX_VALUE&&offer.getXp()==1&&offer.shouldRewardExp()&&offer.getPriceMultiplier()==0,"valid native source offer metadata "+i);
                    require(i<7?LbcBucksTrader.matches(buy):LbcBucksTrader.matches(sell),"authored7 selling/3 buying directions "+i);
                    var payment=offer.getCostA();int count=payment.getCount();require(offer.satisfiedBy(payment,ItemStack.EMPTY)&&offer.take(payment,ItemStack.EMPTY)&&payment.getCount()==0&&count>0,"native merchant accepts and debits exact chosen input "+i);
                    require(ItemStack.matches(offer.assemble(),sell)&&offer.assemble()!=sell,"native output is full detached chosen stack "+i);
                    var codec=MerchantOffer.CODEC;var ops=owner.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                    var restored=codec.parse(ops,codec.encodeStart(ops,offer).getOrThrow()).getOrThrow();
                    require(ItemStack.matches(restored.getBaseCostA(),buy)&&ItemStack.matches(restored.getResult(),sell),"merchant native saved-copy retains input/output components "+i);
                }
                var deathArea=trader.getBoundingBox().inflate(4);var beforeDeath=new HashSet<UUID>();
                for(var item:level.getEntitiesOfClass(ItemEntity.class,deathArea))beforeDeath.add(item.getUUID());
                trader.setHealth(1);trader.setInvulnerableTime(0);trader.damageCooldownTime=0;
                require(trader.hurtServer(level,level.damageSources().genericKill(),Float.MAX_VALUE)&&!trader.isAlive(),"actual native non-player generic-kill death of this fixture merchant");
                var deathDrops=level.getEntitiesOfClass(ItemEntity.class,deathArea).stream().filter(item->!beforeDeath.contains(item.getUUID())).toList();
                created.addAll(deathDrops);deathDrops.forEach(item->previous.add(item.getUUID()));
                require(deathDrops.size()==1,"merchant native death publishes exactly one source currency reward and no unrelated item drops");
                var deathReward=deathDrops.getFirst().getItem();
                require(deathReward.is(LbcBucksTrader.TOKEN)&&deathReward.getCount()>=1&&deathReward.getCount()<=64
                        &&ItemStack.isSameItemSameComponents(deathReward,drops.getFirst().getItem()),"actual death-loot reward keeps the native currency/full canonical components and positive source count");
            }
            require(owner.getStats().getValue(used)==uses+2,"two actual native-use stats");
        }finally{
            for(var entity:level.getEntities((Entity)null,area))if(!previous.contains(entity.getUUID())&&!created.contains(entity))created.add(entity);
            for(var entity:created){game.items().entityUnloaded(entity);entity.discard();}
            random.restore(owner.getRandom());levelRandom.restore(level.getRandom());owner.getStats().setValue(owner,used,uses);saved.restore(game);saved.restoreAttributes();saved.restoreUse();
        }
        if(!nativeHooks){
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_LEVEL_TICK.register(BucksTraderChecks::nativeInputTick);
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{var scene=running;if(scene!=null&&world==scene.f.level)scene.published(entity);});nativeHooks=true;
        }
        var scene=new Scene(game);running=scene;try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){var scene=running;if(scene==null)return;try{
        require(game==scene.game,"same source-event controller");if(!scene.step())return;running=null;scene.close();
        SkyWars2.LOGGER.info("[check] PASS bucks-trader (ordinary immediate native RMB/source merchant/death loot/cache; mixed carrot RMB, native snowball base throw, accepted native Bow/Crossbow shots and real Apple use/food boundaries, both hands)");
    }catch(Throwable error){failed(scene,error);}}
    private static void nativeInputTick(net.minecraft.server.level.ServerLevel level){var scene=running;if(scene==null||level!=scene.f.level)return;try{scene.input();}catch(Throwable error){failed(scene,error);}}
    private static void failed(Scene scene,Throwable error){if(running!=scene)return;running=null;error.addSuppressed(new IllegalStateException("Native source-event state: "+scene.describe()));try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL bucks-trader",error);}
    private static final class Scene implements AutoCloseable {
        enum Action { NONE, DIRECT, NO_AMMO, START_USING, RELEASE_BOW, RELEASE_CROSSBOW, FIRE_CROSSBOW }
        final GameController game;final WoolBucketChecks.Fixture f;final MatchRandomState levelRandom;
        final LbcTmtAbilities.Ability bucks,sibling;final Set<UUID> initial=new HashSet<>(),before=new HashSet<>();
        final Map<net.minecraft.stats.Stat<Item>,Integer> stats=new LinkedHashMap<>();
        final List<Entity> outputs=new ArrayList<>();final int started;
        ItemStack held,expected,ammo,other,publicationSource;CustomData privateData;net.minecraft.stats.Stat<Item> used;
        int index,carrier,phase,when,usedBefore,foodBefore,foodExpected;InteractionHand hand,opposite;Action action=Action.NONE,lastAction=Action.NONE;
        boolean nativeAction,caseDone,closed;net.minecraft.world.phys.Vec3 at;
        Scene(GameController game){
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);levelRandom=MatchRandomState.capture(f.level.getRandom());started=game.server().getTickCount();
            for(var entity:f.level.getAllEntities())initial.add(entity.getUUID());
            String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(LbcBucksTrader.TOKEN).toString();
            bucks=game.items().tmt().useProfiles().families().stream().flatMap(family->family.variants().stream()).filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readString(LbcTmtAbilities.NATIVE,"").equals(id)).findFirst().orElseThrow();
            sibling=game.items().tmt().catalog().families().stream().flatMap(family->family.variants().stream()).filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readInt("reroll_machine",0)==1).findFirst().orElseThrow();
            require(!bucks.id().equals(sibling.id()),"real Bucks use property and independent harmless reroll sibling");
            for(int i=0;i<5;i++){var stat=Stats.ITEM_USED.get(LbcTmtItems.prototype(i).getItem());stats.put(stat,f.owner.getStats().getValue(stat));}
            var stat=Stats.ITEM_USED.get(LbcBucksTrader.TOKEN);stats.put(stat,f.owner.getStats().getValue(stat));
            var air=Stats.ITEM_USED.get(Items.AIR);stats.put(air,f.owner.getStats().getValue(air));
        }
        void begin(){f.prepareParticipants();setup();}
        String describe(){return "case="+index+" carrier="+carrier+" hand="+hand+" phase="+phase+" action="+lastAction+" tick="+game.server().getTickCount()
                +" using="+f.owner.isUsingItem()+" usingTicks="+f.owner.getTicksUsingItem()+" heldCount="+(held==null?-1:held.getCount())
                +" charged="+(held!=null&&net.minecraft.world.item.CrossbowItem.isCharged(held))+" ammo="+(ammo==null?-1:ammo.getCount())
                +" admitted="+game.items().tmt().admitted(f.owner)+" observedMoneySource="+(publicationSource!=null);}
        void setup(){
            carrier=new int[]{0,2,1,3,4}[index/2];hand=InteractionHand.values()[index%2];opposite=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            f.owner.stopUsingItem();f.owner.removeAllEffects();f.owner.getInventory().clearContent();
            held=LbcTmtItems.prototype(carrier);if(carrier!=1&&carrier!=3){held.set(DataComponents.MAX_STACK_SIZE,16);held.remove(DataComponents.MAX_DAMAGE);held.setCount(3);}
            var data=held.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(bucks,sibling));data.putString("bucks_fixture_private","unchanged");held.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            held.set(DataComponents.CUSTOM_NAME,Component.literal("source money boundary "+carrier));held.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3))));
            privateData=held.get(DataComponents.CUSTOM_DATA);expected=held.copy();f.owner.setItemInHand(hand,held);other=new ItemStack(Items.STICK,7);f.owner.setItemInHand(opposite,other);
            used=Stats.ITEM_USED.get(held.getItem());usedBefore=f.owner.getStats().getValue(used);ammo=null;publicationSource=null;caseDone=false;phase=0;when=game.server().getTickCount();
            if(carrier==4){f.owner.getFoodData().setFoodLevel(2);foodExpected=Math.min(20,2+held.get(DataComponents.FOOD).nutrition());foodBefore=game.items().mana().score(f.owner,"food_count").get();}
            action=carrier==1||carrier==3?Action.NO_AMMO:carrier==4?Action.START_USING:Action.DIRECT;
        }
        void published(Entity entity){
            if(nativeAction&&entity instanceof Villager&&entity.entityTags().contains("luck_trader")&&publicationSource==null)
                publicationSource=LbcTmtAbilities.outerCopy(held);
        }
        void capture(){before.clear();for(var entity:f.level.getAllEntities())before.add(entity.getUUID());at=f.owner.position();}
        List<Entity> added(){var result=new ArrayList<Entity>();for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID())){result.add(entity);if(!outputs.contains(entity)){outputs.add(entity);f.own(entity);}}return result;}
        void retireOutputs(){added();for(var entity:outputs)if(!entity.isRemoved())f.discard(entity);outputs.clear();}
        void privateState(ItemStack stack){
            require(stack.get(DataComponents.CUSTOM_DATA)==privateData&&LbcTmtAbilities.read(stack).size()==2&&LbcTmtAbilities.read(stack).stream().anyMatch(property->property.id().equals(sibling.id())&&property.data().equals(sibling.data())),"full original private data/harmless sibling retained at native event boundary");
            require(!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"selected native scopes do not escape accepted input");
        }
        void noMoney(){var entities=added();require(entities.stream().noneMatch(entity->entity instanceof Villager||entity instanceof ItemEntity),"non-trigger event cannot fabricate money/merchant outputs");}
        void money(List<Entity> entities){
            var traders=entities.stream().filter(Villager.class::isInstance).map(Villager.class::cast).toList();var rewards=entities.stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).toList();
            require(traders.size()==1&&rewards.size()==2&&traders.getFirst().position().distanceToSqr(at)<1E-12&&rewards.stream().allMatch(entity->entity.position().distanceToSqr(at)<1E-12&&entity.getItem().is(LbcBucksTrader.TOKEN)&&entity.getItem().getCount()>=1&&entity.getItem().getCount()<=64),"legitimate source trigger immediately emits one merchant/two full currency rewards at actual executor position; "+describe()+" traders="+traders.size()+" rewards="+rewards.size()+" actors="+entities.stream().map(entity->entity.getType().toString()).toList());
        }
        void input(){
            if(action==Action.NONE)return;var selected=action;lastAction=selected;action=Action.NONE;capture();int tick=game.server().getTickCount();nativeAction=true;
            SkyWars2.LOGGER.info("[check] bucks-source-event begin {}",describe());
            try{
                if(selected==Action.NO_AMMO){
                    require(!f.owner.gameMode.useItem(f.owner,f.level,held,hand).consumesAction()&&!f.owner.isUsingItem()&&ItemStack.matches(held,expected),"actual native Bow/Crossbow no-ammo RMB rejects without spending a money carrier");noMoney();require(added().isEmpty()&&f.owner.getStats().getValue(used)==usedBefore,"no-ammo RMB creates no base shot or spell stat");privateState(held);
                    ammo=new ItemStack(Items.ARROW,5);ammo.set(DataComponents.CUSTOM_NAME,Component.literal("unchanged ordinary native ammo"));f.owner.getInventory().setItem(9,ammo);phase=1;when=tick;return;
                }
                if(selected==Action.START_USING){
                    require(f.owner.gameMode.useItem(f.owner,f.level,held,hand).consumesAction()&&f.owner.isUsingItem()&&f.owner.getUseItem()==held,"real native charging/eating begins from actual game-mode input");noMoney();require(added().isEmpty()&&ItemStack.matches(held,expected),"initial using event preserves the carrier and cannot cast money");privateState(held);phase=2;when=tick;return;
                }
                if(selected==Action.RELEASE_CROSSBOW){
                    require(net.minecraft.world.item.CrossbowItem.isCharged(held)&&ammo.getCount()==4,"actual native charge loaded one ordinary arrow before release");
                    f.owner.releaseUsingItem();noMoney();
                    require(!f.owner.isUsingItem()&&added().isEmpty()&&net.minecraft.world.item.CrossbowItem.isCharged(held)&&ammo.getCount()==4
                            &&held.getCount()==1&&held.getDamageValue()==0&&f.owner.getStats().getValue(used)==usedBefore,"real crossbow release/using continuation preserves chamber/ammo/carrier and cannot cast money");
                    privateState(held);phase=3;when=tick;return;
                }
                if(selected==Action.DIRECT){
                    require(f.owner.gameMode.useItem(f.owner,f.level,held,hand).consumesAction(),"actual direct carrot/snowball input");var entities=added();money(entities);
                    if(carrier==0){require(entities.size()==3&&held.getCount()==2&&f.owner.getStats().getValue(used)==usedBefore+1,"carrot RMB owns exactly one money debit/stat with no projectile");}
                    else{
                        var balls=entities.stream().filter(net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball.class::isInstance).map(net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball.class::cast).toList();
                        require(entities.size()==4&&balls.size()==1&&balls.getFirst().getOwner()==f.owner&&LbcTmtItems.matches(balls.getFirst().getItem())&&held.getCount()==1&&f.owner.getStats().getValue(used)==usedBefore+2,"source snowball keeps its one native base throw/debit/stat in addition to immediate money");
                    }
                    require(ItemStack.matches(held,expected.copyWithCount(carrier==0?2:1))&&f.owner.getItemInHand(opposite)==other&&other.getCount()==7,"direct source event preserves all remaining carrier components and opposite hand");privateState(held);caseDone=true;return;
                }
                if(selected==Action.RELEASE_BOW){require(f.owner.isUsingItem()&&f.owner.getTicksUsingItem()>=20,"actual native bow draw ticks reached full release");f.owner.releaseUsingItem();}
                else if(selected==Action.FIRE_CROSSBOW){
                    require(tick>when&&!f.owner.isUsingItem()&&net.minecraft.world.item.CrossbowItem.isCharged(held)&&ammo.getCount()==4,"subsequent actual RMB occurs in a later native tick after completed crossbow release");
                    require(f.owner.gameMode.useItem(f.owner,f.level,held,hand).consumesAction(),"actual native charged crossbow shot");
                }
                var entities=added();SkyWars2.LOGGER.info("[check] bucks-source-event accepted {} actors={}",describe(),entities.stream().map(entity->entity.getType().toString()).toList());money(entities);var arrows=entities.stream().filter(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class::isInstance).map(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class::cast).toList();
                require(entities.size()==4&&arrows.size()==1&&arrows.getFirst().getOwner()==f.owner&&ammo.getCount()==4
                        &&ItemStack.isSameItemSameComponents(arrows.getFirst().getPickupItemStackOrigin(),ammo),"one actual accepted native arrow preserves ordinary ammunition identity and exactly one inventory ammo debit");
                require(f.owner.getItemInHand(hand).isEmpty()&&held.isEmpty()&&f.owner.getStats().getValue(used)==usedBefore+2,"accepted native shot and money retain their independent base-shot stat and one real carrier debit");
                require(publicationSource!=null&&publicationSource.getCount()==1&&publicationSource.getDamageValue()==1&&publicationSource.has(DataComponents.MAX_DAMAGE),"native merchant publication observes the genuinely worn count-one weapon before money consumes it");
                var worn=expected.copy();worn.setDamageValue(1);if(carrier==3)worn.set(DataComponents.CHARGED_PROJECTILES,net.minecraft.world.item.component.ChargedProjectiles.EMPTY);
                require(ItemStack.matches(publicationSource,worn)&&f.owner.getItemInHand(opposite)==other&&other.getCount()==7,"accepted ranged source retains full outer components, native durability and independent opposite hand");privateState(publicationSource);caseDone=true;
            }finally{nativeAction=false;require(game.server().getTickCount()==tick,"all native accepted-input outputs remain synchronous");if(caseDone)retireOutputs();}
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<800,"bounded real source-event fixture case="+index+" phase="+phase);
            if(caseDone){f.owner.stopUsingItem();retireOutputs();if(++index==10)return true;setup();return false;}
            if(phase==1&&action==Action.NONE){action=Action.START_USING;return false;}
            if(phase==3&&action==Action.NONE){noMoney();privateState(held);action=Action.FIRE_CROSSBOW;return false;}
            if(phase==2){
                noMoney();privateState(held);
                if(carrier==1){require(ItemStack.matches(held,expected)&&ammo.getCount()==5,"native bow using ticks do not fire spells, consume ammo or wear weapon");if(f.owner.getTicksUsingItem()>=24)action=Action.RELEASE_BOW;}
                else if(carrier==3){if(net.minecraft.world.item.CrossbowItem.isCharged(held)){require(ammo.getCount()==4,"real native crossbow load debits exactly one inventory arrow");action=Action.RELEASE_CROSSBOW;}else require(ammo.getCount()==5,"crossbow charge cannot fabricate early ammo debit");}
                else if(carrier==4&&!f.owner.isUsingItem()){
                    require(game.server().getTickCount()-when>=20&&held.getCount()==2&&f.owner.getItemInHand(hand)==held&&ItemStack.matches(held,expected.copyWithCount(2)),"real Apple ticks/finish consume exactly one food while preserving both private properties");
                    require(f.owner.getFoodData().getFoodLevel()==foodExpected&&game.items().mana().score(f.owner,"food_count").get()==foodBefore+1&&f.owner.getStats().getValue(used)==usedBefore+1,"native Apple nutrition, source food counter and one ordinary use statistic");
                    require(added().isEmpty()&&f.owner.getItemInHand(opposite)==other&&other.getCount()==7,"Apple usage/finish never invents money or projectile output");caseDone=true;
                }
            }
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;try{
            f.owner.stopUsingItem();game.items().tmt().forget(f.owner);for(var entity:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(entity.getUUID())){f.own(entity);f.discard(entity);}
            stats.forEach((stat,value)->f.owner.getStats().setValue(f.owner,stat,value));levelRandom.restore(f.level.getRandom());
        }finally{f.close();}}
    }
    private static void template(GameController game,ServerPlayer owner){
        removedDefaultWeapon(owner);
        var buy=new ItemStack(Items.PAPER,17);var data=new CompoundTag();data.putString("identity","chosen full logical item");buy.set(DataComponents.CUSTOM_DATA,CustomData.of(data));buy.set(DataComponents.CUSTOM_NAME,Component.literal("component witness"));
        buy.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3))));
        var sell=buy.copyWithCount(4);var offer=LbcBucksTrader.offer(buy,sell);
        require(ItemStack.matches(offer.getBaseCostA(),buy)&&ItemStack.matches(offer.getResult(),sell),"chosen full stack templates preserved on both native sides");
        require(!offer.satisfiedBy(new ItemStack(Items.PAPER,17),ItemStack.EMPTY),"base identity cannot replace custom input");
        var wrong=buy.copy();wrong.remove(DataComponents.CONTAINER);require(!offer.satisfiedBy(wrong,ItemStack.EMPTY),"nested components participate in native input matching");
        buy.setCount(1);sell.setCount(1);require(offer.getBaseCostA().getCount()==17&&offer.getResult().getCount()==4,"offer detaches both original templates");
    }
    /** Regression: positive ItemCost predicates cannot encode source default removals. */
    private static void removedDefaultWeapon(ServerPlayer owner){
        var weapon=new ItemStack(Items.DIAMOND_SWORD,17);weapon.set(DataComponents.MAX_STACK_SIZE,99);weapon.remove(DataComponents.MAX_DAMAGE);
        require(ItemStack.validateStrict(weapon).error().isEmpty(),"source max99 weapon with removed max-damage is legal");
        var offers=new net.minecraft.world.item.trading.MerchantOffers();offers.add(LbcBucksTrader.offer(weapon,new ItemStack(Items.EMERALD,5)));
        var cost=offers.getFirst().getBaseCostA();require(ItemStack.matches(cost,weapon)&&!cost.has(DataComponents.MAX_DAMAGE),"live offer cached buy stack retains explicit removed default");
        require(!weapon.has(DataComponents.CUSTOM_DATA),"extra-component control is actually absent from the chosen full source predicate");
        var extra=weapon.copy();var added=new CompoundTag();added.putString("bucks_extra_component","allowed");extra.set(DataComponents.CUSTOM_DATA,CustomData.of(added));
        require(offers.getFirst().satisfiedBy(extra,ItemStack.EMPTY),"native positive input predicate allows unspecified additional components");
        var ops=owner.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var codec=net.minecraft.world.item.trading.MerchantOffers.CODEC;var encoded=codec.encodeStart(ops,offers).getOrThrow();var restored=codec.parse(ops,encoded).getOrThrow();
        var savedCost=restored.getFirst().getBaseCostA();
        require(!savedCost.has(DataComponents.MAX_DAMAGE)&&ItemStack.matches(savedCost,weapon)
                &&ItemStack.validateStrict(savedCost).error().isEmpty(),"native MerchantOffers saved-copy must not reconstruct illegal damageable+stackable cached weapon");
        require(restored.getFirst().satisfiedBy(weapon,ItemStack.EMPTY)&&restored.getFirst().satisfiedBy(extra,ItemStack.EMPTY)
                &&restored.getFirst().getCostA().getCount()==17,"saved native offer retains full count and positive matching semantics");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),owner.registryAccess());
        try{
            net.minecraft.world.item.trading.MerchantOffers.STREAM_CODEC.encode(buffer,offers);
            var wire=net.minecraft.world.item.trading.MerchantOffers.STREAM_CODEC.decode(buffer);var wireCost=wire.getFirst().getBaseCostA();
            require(ItemStack.matches(wireCost,weapon)&&ItemStack.validateStrict(wireCost).error().isEmpty()
                    &&wire.getFirst().satisfiedBy(extra,ItemStack.EMPTY)&&buffer.readableBytes()==0,"native offer stream constructor repairs the same cached conflict without changing positive matching or adding wire fields");
        }finally{buffer.release();}
        var normal=new net.minecraft.world.item.trading.ItemCost(Items.DIAMOND_SWORD,1);
        require(normal.itemStack().has(DataComponents.MAX_DAMAGE)&&normal.itemStack().getMaxStackSize()==1,"ordinary max-one/default-predicate weapon cache unchanged");
        var explicit=net.minecraft.core.component.DataComponentExactPredicate.builder().expect(DataComponents.MAX_STACK_SIZE,99).expect(DataComponents.MAX_DAMAGE,1561).build();
        var required=new net.minecraft.world.item.trading.ItemCost(Items.DIAMOND_SWORD.builtInRegistryHolder(),17,explicit);
        require(required.components()==explicit&&required.itemStack().has(DataComponents.MAX_DAMAGE)
                &&!required.test(weapon),"explicit positive max-damage requirement is retained even with an intentional conflicting max-stack predicate");
        var inherited=net.minecraft.core.component.DataComponentExactPredicate.expect(DataComponents.MAX_STACK_SIZE,99);
        var repaired=new net.minecraft.world.item.trading.ItemCost(Items.DIAMOND_SWORD.builtInRegistryHolder(),17,inherited);
        var extraDamage=weapon.copy();extraDamage.set(DataComponents.MAX_DAMAGE,1561);
        require(repaired.components()==inherited&&!repaired.itemStack().has(DataComponents.MAX_DAMAGE)
                &&repaired.test(weapon)&&repaired.test(extraDamage),"cache repair adds no absent-component matching requirement and never mutates the native predicate");
        require(Items.DIAMOND_SWORD.getDefaultInstance().has(DataComponents.MAX_DAMAGE),"cache repair cannot mutate the registered item's default component prototype");
    }
}
