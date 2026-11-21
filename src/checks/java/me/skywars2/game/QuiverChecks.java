package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.mixin.CombatCheckpointAccess;
import me.skywars2.validation.mixin.GameCheckAccess;
import me.skywars2.validation.mixin.ItemFixtureAccess;
import me.skywars2.validation.mixin.NativeFixtureAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.server.level.ServerLevel;

/** Original29-way native loot, real bow/crossbow and scheduled full arrow closure. */
public final class QuiverChecks {
    private QuiverChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Quiver: "+why);}
    private static double baseDamage(Arrow arrow){return ((ItemFixtureAccess.Arrow)(Object)arrow).validation$damage();}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-5,why+" actual="+actual+" expected="+expected);}

    /** The real network scheduler owns this connection's native super.tick; the fixture never invokes it. */
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
        final ScheduledConnection connection=new ScheduledConnection();
        ScheduledClient(GameController game,ServerPlayer player){this.game=game;this.player=player;previous=player.connection;
            try{
                listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),connection,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
                // A fresh memory connection has no protocol channel pipeline; bind only its own native dispatch field.
                ((NativeFixtureAccess.ConnectionBinding)(Object)connection).validation$packetListener(listener);
                listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                listener.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
                var teleport=connection.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
                listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
                require(listener.hasClientLoaded()&&((GameCheckAccess.TeleportAck)(Object)listener).validation$awaitingPosition()==null,"fresh native listener loaded and real teleport acknowledged");
                game.server().getConnection().getConnections().add(connection);
            }catch(RuntimeException|Error error){game.server().getConnection().getConnections().remove(connection);player.connection=previous;throw new IllegalStateException("scoped native connection scheduler",error);}
        }
        void acknowledge(){
            listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            if(((GameCheckAccess.TeleportAck)(Object)listener).validation$awaitingPosition()!=null){
                var teleport=Objects.requireNonNull(connection.latestPosition,"actual emitted teleport packet survives measurement windows");
                listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(
                        teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
            }
            require(ready(),"native player loaded and latest actual teleport acknowledged");
        }
        boolean ready(){return listener.hasClientLoaded()&&!player.isChangingDimension()
                &&((GameCheckAccess.TeleportAck)(Object)listener).validation$awaitingPosition()==null;}
        int updates(){return ((GameCheckAccess.TeleportAck)(Object)listener).validation$listenerTicks();}
        @Override public void close(){try{game.server().getConnection().getConnections().remove(connection);}finally{player.connection=previous;}}
    }
    private static final class Snapshot {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final ItemFixtureChecks.Nutrition food;
        final boolean ground,sprint,shift,swimming,using;final InteractionHand useHand;final int useRemaining;final ItemStack useStack;final Vec3 knownMovement;final net.minecraft.world.entity.Pose pose;final ItemFixtureChecks.PlayerTicks tickers;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));final Map<EquipmentSlot,ItemStack> equipment;
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final ItemFixtureChecks.Combat combatTracker;
        final ItemFixtureChecks.PlayerSync playerSync;
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final MatchValueSnapshot profile;
        Snapshot(GameController game,ServerPlayer player){this.player=player;for(var holder:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var attr=player.getAttribute(holder);if(attr!=null)attributes.getInstance(holder).replaceFrom(attr);}equipment=ItemFixtureChecks.captureEquipment(player);for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(player,objective);if(score!=null)scores.put(objective,score.value());}state=new GameChecks.WandThirdPlayerState(player,game.items().mana());using=player.isUsingItem();useHand=using?player.getUsedItemHand():InteractionHand.MAIN_HAND;useRemaining=player.getUseItemRemainingTicks();useStack=player.getItemInHand(useHand);ground=player.onGround();sprint=player.isSprinting();shift=player.isShiftKeyDown();swimming=player.isSwimming();knownMovement=player.getKnownMovement();pose=player.getPose();
            tickers=ItemFixtureChecks.PlayerTicks.capture(player);
            combatTracker=ItemFixtureChecks.Combat.capture(player);
            food=ItemFixtureChecks.Nutrition.capture(player.getFoodData());
            playerSync=ItemFixtureChecks.PlayerSync.capture(player);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
            for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.WOODEN_SWORD,Items.BOW,Items.CROSSBOW,Items.CARROT_ON_A_STICK,Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS)){var stat=type.get(item);stats.put(stat,player.getStats().getValue(stat));}
            var picked=Stats.ITEM_PICKED_UP.get(LbcAccessoryItems.Kind.INFINITE_QUIVER.item);stats.put(picked,player.getStats().getValue(picked));for(var type:List.of(EntityTypes.COW,EntityTypes.SKELETON)){var killed=Stats.ENTITY_KILLED.get(type);stats.put(killed,player.getStats().getValue(killed));}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            profile=new MatchValueSnapshot(game.players().get(player.getUUID()));
        }
        void restoreAttributes(){
            combatTracker.restore(player);
            player.getAttributes().assignAllValues(attributes);
            ItemFixtureChecks.restoreEquipment(player,equipment);
        }
        void restoreUse(){
            if(using){player.setItemInHand(useHand,useStack);player.startUsingItem(useHand);
                ((CombatCheckpointAccess.Living)player).skywars2$useItemRemaining(useRemaining);}
        }
        void restoreProfile(){profile.restore();}
        void restore(GameController game){for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(player,objective,true).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(player,objective);stats.forEach((s,v)->player.getStats().setValue(player,s,v));
            advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});state.restore(game);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setSwimming(swimming);player.setKnownMovement(knownMovement);player.setPose(pose);
            tickers.restore(player,true);
            playerSync.restore(player);
            food.restore(player.getFoodData());
            restoreProfile();
        }
    }

    private static final class RandomControl extends ItemFixtureChecks.DelegatingRandom implements AutoCloseable {
        private static final StackWalker WALKER=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        final ServerLevel level;
        final ArrayDeque<Integer> picks=new ArrayDeque<>();
        int draws;
        boolean maximum,scheduled;

        RandomControl(ServerLevel level){
            super(level.getRandom());
            this.level=level;
            ItemFixtureChecks.random(level,this);
        }

        private boolean calledByQuiver(String method){
            return WALKER.walk(frames->frames.anyMatch(frame->frame.getDeclaringClass()==LbcQuiver.class
                    &&(method==null||frame.getMethodName().equals(method))));
        }

        @Override public int nextInt(int bound){
            if(calledByQuiver(null)){
                if(bound==281){
                    draws++;
                    require(!picks.isEmpty(),"no duplicate/quadratic extra pool draw");
                    return picks.removeFirst();
                }
                if((bound==8||bound==3||bound==4)&&calledByQuiver("pulse"))return maximum?bound-1:0;
                if(scheduled&&bound==3&&calledByQuiver("trapper"))return 1;
            }
            return super.nextInt(bound);
        }

        @Override public float nextFloat(){return scheduled&&calledByQuiver(null)?0f:super.nextFloat();}

        void add(int index,int count){
            int offset=index<24?index*10:index==24?240:241+(index-25)*10;
            for(int n=0;n<count;n++)picks.add(offset);
        }

        @Override public void close(){ItemFixtureChecks.random(level,delegate);}
    }
    private static final class Witness extends Cow {
        final Map<Holder<MobEffect>,MobEffectInstance> grants=new HashMap<>();
        Witness(ServerLevel level){super(EntityTypes.COW,level);}
        @Override protected void onEffectAdded(MobEffectInstance effect,Entity source){super.onEffectAdded(effect,source);grants.put(effect.getEffect(),new MobEffectInstance(effect));}
        @Override protected void onEffectUpdated(MobEffectInstance effect,boolean refresh,Entity source){super.onEffectUpdated(effect,refresh,source);if(refresh)grants.put(effect.getEffect(),new MobEffectInstance(effect));}
    }
    private static final class Fixture implements AutoCloseable {
        private static final List<Integer> LIGHT_TNT_OFFSETS=List.of(0,3,-1,-2,-3,-4,-5,-7,-9,-11,-13,-15,-17,-19,-21,-23,-25,-27,-29,-31,-33,-35,-37,-39,-41,-43,-45);
        private static final List<Integer> LIGHT_BOLT_OFFSETS=List.of(0,3,-1,-2,-3,-4,-5,-6,-7,-9,-13,-17,-21,-25,-33,-45);
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,other;final ServerLevel level;final LbcQuiver service;final Snapshot savedOwner,savedOther;final SauvojenMeditationChecks.Attribution attribution;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final List<Entity> owned=new ArrayList<>();final ItemStack[] ammo=new ItemStack[29];final Vec3 at;final ItemStack quiver;final net.minecraft.world.scores.PlayerTeam team,ownerTeam,otherTeam;
        final Set<Entity> columnChildren=Collections.newSetFromMap(new IdentityHashMap<>());
        RandomControl random;ScheduledClient client,audience;int start,initialDraws,nativePhase,lightBirth;boolean skyStopped,columnSeen,trapperSeen;Arrow light,trap,weak,reactive,strong;AbstractArrow crossbow;Witness hit102,hit103,bowTarget,crossTarget,mixed;
        Marker lightTrail;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null&&game.canUseGameplayItems(other),"both actual active participants");savedOwner=new Snapshot(game,owner);savedOther=new Snapshot(game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));service=game.items().quiver();base.initialize();level=owner.level();at=owner.position();level.getAllEntities().forEach(before::add);
            var board=game.server().getScoreboard();ownerTeam=owner.getTeam();otherTeam=other.getTeam();require(board.getPlayerTeam("sw2_quiver_check")==null,"exclusive native team fixture");team=board.addPlayerTeam("sw2_quiver_check");team.setAllowFriendlyFire(false);board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
            for(var p:List.of(owner,other)){p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;profile.lbcChallenge=0;Arrays.fill(profile.trinkets,0);}
            other.teleportTo(level,at.x+30,at.y,at.z,Set.of(),0,0,true);var leaf=game.lbc().rollTable(level,at,owner,"lbc:ported/infinite_quiver");require(leaf.size()==1,"original singleton Quiver leaf");quiver=leaf.getFirst();random=new RandomControl(level);
        }
        void emptyInventory(){owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);owner.removeAllEffects();}
        List<ItemEntity> drops(){var drops=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(e instanceof ItemEntity item&&!before.contains(e)&&!item.isRemoved())drops.add(item);return drops;}
        void removeDrops(){for(var item:drops())item.discard();}
        int count(ItemEntity drop){return drop.getItem().getCount();}
        void originalTable(){
            require(quiver.is(LbcAccessoryItems.Kind.INFINITE_QUIVER.item)&&LbcFartus.data(quiver).getIntOr("arrowgenerator",0)==1&&LbcFartus.data(quiver).getIntOr("transformable",0)==1&&LbcFartus.data(quiver).getIntOr("accessory",0)==1,"exact source Quiver vanilla flags");require(Identifier.parse("lbc:quiver").equals(quiver.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_b").equals(quiver.get(DataComponents.TOOLTIP_STYLE))&&quiver.get(DataComponents.LORE).lines().size()==3,"source Quiver presentation");
            var task=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/accesories/infinite_quiver"));require(task!=null,"actual original advancement loaded");for(var criterion:task.value().criteria().keySet())owner.getAdvancements().revoke(task,criterion);
            String[] potion={"slowness","long_slowness","weakness","long_weakness","harming","strong_harming","poison","strong_poison","long_poison"};
            int[] colors={6710881,6710882,-1,2883328,6710883,16746496,6184020,65493,52479,16777215,16763161,2236962};
            for(int i=0;i<29;i++)for(boolean maximum:List.of(false,true)){
                emptyInventory();removeDrops();owner.getInventory().setItem(9,quiver.copy());random.maximum=maximum;random.add(i,1);int draws=random.draws;service.pulse(owner);var output=drops();require(random.draws==draws+1&&output.size()==1,"actual native281weight pool independently selects index"+i);var stack=output.getFirst().getItem().copy();int max=i==21||i==24?1:i==22?3:i==23?4:8;require(stack.getCount()==(maximum?max:1),"source native count distribution index"+i);if(!maximum)ammo[i]=stack.copyWithCount(1);
                if(i==9||i==11)require(stack.is(Items.ARROW),"literal duplicate ordinary arrow entries retained");else if(i==10||i==14)require(stack.is(Items.SPECTRAL_ARROW),"literal duplicate spectral entries retained");else if(i==24)require(LbcStand.isArrow(stack),"original referenced Stand identity stays functional");else require(stack.getItem() instanceof net.minecraft.world.item.TippedArrowItem,"native tipped carrier index"+i);
                var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
                if(i<=8)require(contents.potion().orElseThrow().unwrapKey().orElseThrow().identifier().toString().equals("minecraft:"+potion[i]),"native vanilla potion index"+i);
                if(i>=25)require(contents.potion().orElseThrow().unwrapKey().orElseThrow().identifier().toString().equals("minecraft:"+List.of("oozing","infested","wind_charged","weaving").get(i-25)),"new vanilla potion family index"+i);
                if(i>=12&&i<=23&&i!=14)require(contents.customColor().orElse(-1)==colors[i-12],"source special color index"+i);
                float scale=stack.getOrDefault(DataComponents.POTION_DURATION_SCALE,1f);
                if(i==24){
                    var effects=new ArrayList<MobEffectInstance>();
                    contents.forEachEffect(effects::add,scale);
                    require(effects.size()==1&&effects.getFirst().is(MobEffects.RAID_OMEN)
                            &&effects.getFirst().getAmplifier()==119&&effects.getFirst().getDuration()==20,
                            "authored Stand arrow carries the full native 20-tick RAID_OMEN 119 marker");
                }else if(stack.getItem() instanceof net.minecraft.world.item.TippedArrowItem){
                    near(scale,.125,"native tipped duration scale index"+i);
                }
            }
            require(owner.getAdvancements().getOrStartProgress(task).isDone(),"actual emitted native Stand stack awards original Quiver task");removeDrops();emptyInventory();
        }
        void batchingAndScopes(){
            for(int slot:new int[]{0,9,36,40,41,42}){emptyInventory();removeDrops();owner.getInventory().setItem(slot,quiver.copyWithCount(2));int before=random.draws;random.add(9,slot==0||slot==9||slot==40?2:0);random.maximum=false;service.pulse(owner);require(random.draws-before==(slot==0||slot==9||slot==40?2:0),"container/hand trigger versus equipment-only scope"+slot);}
            emptyInventory();removeDrops();owner.setItemInHand(InteractionHand.MAIN_HAND,quiver.copyWithCount(2));owner.setItemInHand(InteractionHand.OFF_HAND,quiver.copyWithCount(3));owner.getInventory().setItem(36,quiver.copyWithCount(4));owner.inventoryMenu.getCraftSlots().setItem(0,quiver.copyWithCount(5));owner.containerMenu.setCarried(quiver.copyWithCount(6));int counted=owner.getInventory().clearOrCountMatchingItems(s->LbcFartus.data(s).getIntOr("arrowgenerator",0)==1,true,0,owner.inventoryMenu.getCraftSlots());require(counted>=20,"actual full native clear count includes hands/equipment/craft/cursor");int previous=random.draws;random.add(9,counted);service.pulse(owner);require(random.draws-previous==counted&&owner.getMainHandItem().getCount()==2&&owner.getOffhandItem().getCount()==3,"one original rollN batch, no duplicate hand branch or consumption");removeDrops();emptyInventory();
        }
        <T>T weightedAccessory(java.util.function.Supplier<T> action){
            var key=net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:transformables_accessory"));
            var table=game.server().reloadableRegistries().getLootTable(key);
            var loot=(ItemFixtureAccess.Loot)(Object)table;
            var pools=loot.validation$pools();
            require(pools.size()==1&&pools.getFirst().entries.size()>1,"actual original accessory pool");
            var sequence=loot.validation$sequence();
            require(sequence.isEmpty(),"original accessory pool uses normal native level RNG");
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at)
                    .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);
            var pool=pools.getFirst();
            require(context.getLuck()==0&&(pool.condition.isEmpty()||pool.condition.get().value().test(context)),
                    "actual normal reforge context/pool admission");
            int total=0,offset=-1,selected=0;
            var weights=new ArrayList<Integer>();
            for(int i=0;i<pool.entries.size();i++){
                var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();
                pool.entries.get(i).expand(context,expanded::add);
                int sum=0;
                for(var entry:expanded){
                    int weight=entry.getWeight(context.getLuck());
                    if(weight<=0)continue;
                    if(i==6){if(offset<0)offset=total;selected++;}
                    total=Math.addExact(total,weight);sum=Math.addExact(sum,weight);
                }
                weights.add(sum);
            }
            require(selected==1&&offset>=0&&weights.get(6)==10,"original Quiver entry6 native effective weight10");
            int bound=total,pick=offset;
            var original=level.getRandom();
            int[] calls={0};
            ItemFixtureChecks.random(level,new ItemFixtureChecks.DelegatingRandom(original){
                @Override public int nextInt(int actualBound){
                    if(actualBound!=bound)return super.nextInt(actualBound);
                    calls[0]++;
                    return pick;
                }
            });
            try{
                T result=action.get();
                require(calls[0]==1,"one actual normal pool selection, no retries");
                SkyWars2.LOGGER.info("[check] Quiver native original pool entry6 weights={} total={} selectedOffset={}",weights,bound,pick);
                return result;
            }finally{ItemFixtureChecks.random(level,original);}
        }
        void reforge(){
            emptyInventory();require(owner.getLuck()==0,"normal reforge source luck0");
            var source=weightedAccessory(()->game.lbc().rollTable(level,at,owner,"lbc:transformables_accessory"));
            require(source.size()==1&&ItemStack.matches(source.getFirst(),quiver),"original weighted table delivers the exact Quiver leaf");
            var donorStack=new ItemStack(Items.STICK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("transformable",1);data.putInt("accessory",1);donorStack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            var donor=new ItemEntity(level,at.x,at.y,at.z,donorStack);donor.setNoGravity(true);owned.add(donor);
            require(level.addFreshEntity(donor),"actual normal accessory reforge input");
            var outputs=OrdinaryAccessoryChecks.reforgeUniform(game,owner,donor,"custom/3e91b10f60871790c5d6");
            require(donor.isRemoved()&&donorStack.isEmpty()&&outputs.size()==1&&ItemStack.matches(outputs.getFirst(),quiver),"existing normal reforge delivers original Quiver identity/presentation and consumes1");
        }

        Witness cow(Vec3 position){var c=new Witness(level);c.setPos(position);c.setYRot(0);c.setXRot(0);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);require(level.addFreshEntity(c),"actual native resident target");owned.add(c);return c;}
        Arrow arrow(ItemStack ammo,Vec3 pos,Vec3 motion){var a=new Arrow(level,owner,ammo.copyWithCount(1),null);a.setPos(pos);a.setDeltaMovement(motion);require(level.addFreshEntity(a),"actual native source arrow admitted");owned.add(a);return a;}
        AbstractArrow latest(Set<Entity> previous){for(var e:level.getAllEntities())if(e instanceof AbstractArrow arrow&&!previous.contains(e)&&arrow.getOwner()==owner){owned.add(arrow);return arrow;}throw new IllegalStateException("actual native weapon emitted no arrow");}
        void aimAt(LivingEntity target){var delta=target.getBoundingBox().getCenter().subtract(owner.getEyePosition());owner.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));owner.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));owner.setYHeadRot(owner.getYRot());owner.setYBodyRot(owner.getYRot());}
        String shotState(AbstractArrow arrow){return "pos="+arrow.position()+",motion="+arrow.getDeltaMovement()+",removed="+arrow.isRemoved()+",owner="+(arrow.getOwner()==null?null:arrow.getOwner().getUUID());}
        void setupNative(){
            emptyInventory();removeDrops();random.scheduled=true;random.maximum=false;client=new ScheduledClient(game,owner);audience=new ScheduledClient(game,other);client.acknowledge();audience.acknowledge();
            bowTarget=cow(at.add(0,0,4));aimAt(bowTarget);var previous=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(previous::add);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));owner.setItemInHand(InteractionHand.OFF_HAND,ammo[12].copy());owner.gameMode.useItem(owner,level,owner.getMainHandItem(),InteractionHand.MAIN_HAND);require(Items.BOW.releaseUsing(owner.getMainHandItem(),level,owner,71980),"actual native bow input/release accepts generated Strong arrow");owner.stopUsingItem();strong=(Arrow)latest(previous);require(strong.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).customColor().orElse(-1)==6710881,"native bow retained exact original arrow components");
            crossTarget=cow(at.add(4,0,0));aimAt(crossTarget);previous.clear();level.getAllEntities().forEach(previous::add);var cross=new ItemStack(Items.CROSSBOW);cross.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(ammo[10].copy())));owner.setItemInHand(InteractionHand.MAIN_HAND,cross);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.gameMode.useItem(owner,level,cross,InteractionHand.MAIN_HAND);crossbow=latest(previous);require(crossbow instanceof SpectralArrow,"native crossbow preserves generated vanilla spectral arrow");owner.setYRot(0);
            weak=arrow(ammo[13],at.add(-3,2,0),new Vec3(-1,0,0));reactive=arrow(ammo[16],at.add(3,2,0),new Vec3(1,0,0));hit102=cow(at.add(0,0,14));hit103=cow(at.add(4,0,14));arrow(ammo[18],hit102.position().add(0,.6,-2),new Vec3(0,0,3));arrow(ammo[20],hit103.position().add(0,.6,-2),new Vec3(0,0,3));
            double sky=level.getMaxY()+60;light=arrow(ammo[21],new Vec3(at.x,sky,at.z),Vec3.ZERO);light.setNoGravity(true);trap=arrow(ammo[23],at.add(0,60,0),Vec3.ZERO);trap.setNoGravity(true);cow(at.add(4,60,0));
            mixed=cow(at.add(14,0,0));other.teleportTo(level,mixed.getX()+.4,mixed.getY(),mixed.getZ(),Set.of(),0,0,true);audience.acknowledge();var normal=arrow(ammo[17],at.add(-6,2,-6),Vec3.ZERO);normal.setNoGravity(true);require(service.accept(mixed,ItemEffectLevels.create(MobEffects.RAID_OMEN,2,101),normal),"normal101 source accepted into shared FIFO");normal.discard();var babylon=game.items().babylon().emit(owner,3,1);require(babylon!=null&&game.items().babylonEffects().accept(mixed,babylon,ItemEffectLevels.create(MobEffects.RAID_OMEN,2,101)),"independent managed Babylon101 shares counter");game.items().babylon().unloaded(babylon);require(game.items().babylonEffects().queued(mixed,101)==2,"mixed producers additive single FIFO");mixed.entityTags().add("sin_greed_active");
            emptyInventory();owner.setItemInHand(InteractionHand.MAIN_HAND,quiver.copyWithCount(3));random.add(9,6);initialDraws=random.draws;((ItemFixtureAccess.Quiver)(Object)service).validation$clock(299);start=game.server().getTickCount();nativePhase=0;
        }
        void grant(Witness target,Holder<MobEffect> type,int duration,int amplifier){var e=target.grants.get(type);require(e!=null&&e.getDuration()==duration&&e.getAmplifier()==amplifier&&!e.isVisible(),"native onEffectAdded exact duration/amp/hidden "+type+" actual="+e);}

        void lightColumn(int elapsed) {
            if(lightTrail==null){
                if(light.tickCount==0)return;
                var trails=service.trails().stream().filter(marker->marker.entityTags().contains("lightarrowtrail")).toList();
                require(trails.size()==1,"one light trail from the first real arrow tick: "+trails.size());
                lightTrail=trails.getFirst();
                lightBirth=elapsed;
                light.discard(); // One physical flight tick contributes one independent native trail.
            }
            int age=elapsed-lightBirth+1;
            boolean legacy=game.balanceLegacy();
            int finalAge=legacy?199:201, retirementAge=legacy?200:201;
            var tnt=new HashSet<Vec3>();
            var bolts=new HashSet<Vec3>();
            for(var child:service.children()){
                if(child.isRemoved()||!(child instanceof PrimedTnt||child instanceof LightningBolt)||!columnChildren.add(child))continue;
                var cause=service.cause(child);
                require(cause!=null&&cause.player().equals(owner.getUUID()),"light column child retains exact physical owner");
                if(child instanceof PrimedTnt explosive){
                    require(explosive.getFuse()==0&&!cause.friendlyFire()&&!service.canAffect(child,other)&&service.canAffect(child,owner),
                            "native zero-fuse TNT retains default ally/self policy");
                    require(tnt.add(child.position()),"no duplicate TNT offset in a light trail slice");
                }else if(child instanceof LightningBolt lightning){
                    var source=new net.minecraft.world.damagesource.DamageSource(owner.damageSources().lightningBolt().typeHolder(),lightning,owner);
                    var attributed=game.items().indirectKills().babylonCause(source);
                    require(lightning.getCause()==owner&&cause.friendlyFire()&&service.canAffect(lightning,other)
                                    &&attributed!=null&&attributed.friendlyFire(),
                            "native LightningBolt retains physical cause and WILD damage attribution");
                    require(bolts.add(child.position()),"no duplicate lightning offset in a light trail slice");
                }
            }
            var origin=lightTrail.position();
            List<Integer> expectedTnt=List.of();
            if(age>=199&&age<=finalAge){
                int first=legacy?0:(age-199)*9;
                int end=legacy?LIGHT_TNT_OFFSETS.size():first+9;
                expectedTnt=LIGHT_TNT_OFFSETS.subList(first,end);
            }
            var expectedBolts=age==finalAge?LIGHT_BOLT_OFFSETS:List.<Integer>of();
            require(tnt.equals(lightPositions(origin,expectedTnt))&&bolts.equals(lightPositions(origin,expectedBolts)),
                    "exact native light column slice at marker age "+age+" legacy="+legacy+" TNT="+tnt+" bolts="+bolts);
            require(lightTrail.isRemoved()==(age>=retirementAge),"light trail marker retires after its original or sliced final publication");
            if(age==finalAge){
                require(columnChildren.size()==43,"all 27 TNT and 16 lightning actors were independently published");
                columnSeen=true;
            }
        }

        private static Set<Vec3> lightPositions(Vec3 origin,List<Integer> offsets){
            return offsets.stream().map(y->origin.add(0,y,0)).collect(java.util.stream.Collectors.toSet());
        }

        boolean step(){client.acknowledge();audience.acknowledge();int elapsed=game.server().getTickCount()-start;
            int clock=((ItemFixtureAccess.Quiver)(Object)service).validation$clock();
            require(clock==300+elapsed,"native source phase clock="+clock+" elapsed="+elapsed+" expected="+(300+elapsed));
            lightColumn(elapsed);

            if(elapsed>=1&&!skyStopped){require(light.tickCount>0&&trap.tickCount>0,"real world scheduling ticked source arrows");near(baseDamage(strong),5,"Strong source damage5 before native collision");near(baseDamage(weak),1,"Weak source damage1");require(reactive.isNoGravity()&&!weak.isNoGravity(),"Reactive source NoGravity without broad native override");light.discard();trap.discard();weak.discard();reactive.discard();require(service.trails().stream().anyMatch(m->m.entityTags().contains("lightarrowtrail"))&&service.trails().stream().anyMatch(m->m.entityTags().contains("trappertrail")),"ordinary native flight emits both complete marker families");require(random.draws-initialDraws==3,"first scheduled batch exactly3 at300 boundary");removeDrops();require(hit102.getHealth()<1000&&hit103.getHealth()<1000&&!hit102.hasEffect(MobEffects.RAID_OMEN)&&!hit103.hasEffect(MobEffects.RAID_OMEN),"actual swept accepted ordinary arrow hits consume sentinels");require(service.queued(hit102,102)==0&&service.queued(hit103,103)==0,"native20*.125=2 normalizes to one pulse");grant(hit103,MobEffects.SPEED,60,1);grant(hit103,MobEffects.WITHER,60,5);require(game.items().evokerBass().snapshot().stream().filter(f->f.getOwner()==owner).count()>=9,"native102 produces full nine-fang formation");skyStopped=true;}
            if(elapsed<=2)require(game.items().babylonEffects().queued(mixed,101)==2,"true Greed pauses shared mixed101 FIFO");
            if(elapsed>=2&&nativePhase==0){require(bowTarget.getHealth()<1000&&crossTarget.getHealth()<1000,"native bow and crossbow both reach actual targets elapsed="+elapsed+" bowHealth="+bowTarget.getHealth()+" bowBox="+bowTarget.getBoundingBox()+" bowShot="+shotState(strong)+" crossHealth="+crossTarget.getHealth()+" crossBox="+crossTarget.getBoundingBox()+" crossShot="+shotState(crossbow)+" ownerPos="+owner.position());strong.discard();crossbow.discard();mixed.entityTags().remove("sin_greed_active");nativePhase=1;}
            if(elapsed>=3&&nativePhase==1){require(game.items().babylonEffects().queued(mixed,101)==1&&other.getHealth()==1000,"first default Quiver101 pulse protects allied player, one shared pulse");nativePhase=2;}
            if(elapsed>=4&&nativePhase==2){require(game.items().babylonEffects().queued(mixed,101)==0&&other.getHealth()<1000,"second WILD Babylon101 retains independent FIFO allegiance");other.setHealth(1000);other.setDeltaMovement(Vec3.ZERO);other.teleportTo(level,at.x+30,at.y,at.z,Set.of(),0,0,true);audience.acknowledge();nativePhase=3;}
            if(elapsed>=121&&!trapperSeen){require(service.trails().stream().filter(m->m.entityTags().contains("trappertrail")).allMatch(m->m.getX()==Math.floor(m.getX())&&m.getY()==Math.floor(m.getY())&&m.getZ()==Math.floor(m.getZ())),"trapper native120age branch snaps every coordinate before jitter");trapperSeen=true;}
            if(elapsed>=202)require(columnSeen,"native light trail completes all authored column children within the real scheduler budget");if(elapsed<300)require(random.draws-initialDraws==3,"no premature generation during300tick interval");if(elapsed>=300){require(random.draws-initialDraws==6,"second native generation exactly300ticks after first");removeDrops();}
            if(elapsed>=401){require(service.trails().isEmpty(),"both light200 and trapper390/400 bounded cleanup");require(client.updates()>300&&audience.updates()>300,"actual native client scheduling and ACKs drove full cadence");require(random.picks.isEmpty(),"all expected one-per-unit native table draws consumed");return true;}return false;
        }
        @Override public void close(){try{if(client!=null)client.close();if(audience!=null)audience.close();if(random!=null)random.close();service.forget(owner);game.items().evokerBass().forget(owner);for(var e:owned){if(e instanceof LivingEntity living){service.clear(living);game.items().babylonEffects().clear(living);game.items().indirectKills().clear(living);}e.discard();}removeDrops();var board=game.server().getScoreboard();board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(otherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),otherTeam);savedOwner.restoreAttributes();savedOther.restoreAttributes();savedOwner.restore(game);savedOther.restore(game);}finally{try{base.close();}finally{attribution.close();}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive native Quiver fixture");running=new Fixture(game);try{running.originalTable();running.batchingAndScopes();running.reforge();running.setupNative();}catch(Throwable error){try{running.close();}finally{running=null;}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS quiver (all29 weighted native outcomes/count endpoints; original weighted acquisition and uniform NORMAL reforge; full Stand marker and emitted award; one-count/full-clear batches; native bow/crossbow,20-to2-to1 sentinels, mixed101 FIFO/Greed/default-vs-WILD, full light TNT/lightning and trapper clocks, actual300tick cadence)");}}catch(Throwable error){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL quiver",error);}}
}
