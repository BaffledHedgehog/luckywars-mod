package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Actual native input, helper combat and passenger-trial checks; each phase uses a new server tick. */
public final class HedgehogChecks {
    private HedgehogChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Hedgehog: "+why);}
    private static int flag(ItemStack s,String key){return LbcFartus.data(s).getIntOr(key,0);}
    private static void flag(ItemStack s,String key,int value){var tag=LbcFartus.data(s);tag.putInt(key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    private static Object copy(Object v){if(v instanceof int[] a)return a.clone();if(v instanceof Set<?> s)return new HashSet<>(s);return v;}
    static Map<Field,Object> fields(Object object){var result=new LinkedHashMap<Field,Object>();try{for(var f:object.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))result.put(f,copy(f.get(object)));}catch(IllegalAccessException e){throw new IllegalStateException(e);}return result;}
    static void restore(Object object,Map<Field,Object> saved){try{for(var e:saved.entrySet())e.getKey().set(object,copy(e.getValue()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static final class Recording extends net.minecraft.network.Connection {
        final ServerPlayer player;final List<net.minecraft.network.protocol.Packet<?>> sent=new ArrayList<>();
        Recording(ServerPlayer player){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public net.minecraft.network.PacketListener getPacketListener(){return player.connection;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){sent.add(packet);}
        @Override public void flushChannel(){}
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous;final Recording recording;
        Swap(GameController game,ServerPlayer player){this.player=player;previous=player.connection;recording=new Recording(player);
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),recording,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));}
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    static final class Saved {
        final ServerPlayer p;final GameChecks.WandThirdPlayerState state;final Map<Field,Object> profile;
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();final List<Runnable> counters=new ArrayList<>();
        final boolean shift,sprint,ground;final net.minecraft.world.entity.player.Input input;
        Saved(GameController game,ServerPlayer p){this.p=p;state=new GameChecks.WandThirdPlayerState(p,game.items().mana());profile=fields(game.players().get(p.getUUID()));
            shift=p.isShiftKeyDown();sprint=p.isSprinting();ground=p.onGround();input=p.getLastClientInput();
            for(var objective:game.server().getScoreboard().getObjectives()){var value=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(value!=null)scores.put(objective,value.value());}
            for(var id:BuiltInRegistries.CUSTOM_STAT)stat(Stats.CUSTOM.get(id));
            for(var item:BuiltInRegistries.ITEM)for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_DROPPED,Stats.ITEM_PICKED_UP,Stats.ITEM_CRAFTED))stat(type.get(item));
            for(var type:BuiltInRegistries.ENTITY_TYPE){stat(Stats.ENTITY_KILLED.get(type));stat(Stats.ENTITY_KILLED_BY.get(type));}
            for(var adv:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);
                counters.add(()->{for(var criterion:adv.value().criteria().keySet())if(done.contains(criterion))p.getAdvancements().award(adv,criterion);else p.getAdvancements().revoke(adv,criterion);});}
        }
        <T>void stat(Stat<T> stat){int value=p.getStats().getValue(stat);counters.add(()->p.getStats().setValue(p,stat,value));}
        void restore(GameController game){HedgehogChecks.restore(game.players().get(p.getUUID()),profile);state.restore(game);p.setShiftKeyDown(shift);p.setSprinting(sprint);p.setOnGround(ground);p.setLastClientInput(input);counters.forEach(Runnable::run);
            for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(p,objective,true).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(p,objective);
            game.items().magic().inventoryChanged(p);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcHedgehog service;final ServerPlayer owner,other;final Saved saved,otherSaved;final Map<Field,Object> config;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>()),owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final Set<Long> forced=new HashSet<>(),previousForced;
        final net.minecraft.world.scores.PlayerTeam oldOwnerTeam,oldOtherTeam,team;final Vec3 at;final String epoch;final int cost;
        long last=Long.MIN_VALUE;int phase,sneezeWarmTicks;boolean closed;
        Fixture(GameController game){this.game=game;service=game.items().hedgehog();epoch=game.items().epoch();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&owner.level()==other.level()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two actual active participants");
            require(service.trackedCount()==0&&service.pendingBlasts()==0&&!game.items().godHedgehog().blocksSilverfish(owner.level())&&!game.items().timeStop().stopped(owner.level()),"exclusive empty family and active clock");
            require(owner.containerMenu==owner.inventoryMenu&&other.containerMenu==other.inventoryMenu,"closed menus");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());cost=cost();owner.level().getAllEntities().forEach(before::add);
            at=new Vec3((owner.blockPosition().getX()>>4)*16+8.37,270,(owner.blockPosition().getZ()>>4)*16+8.37);previousForced=new HashSet<>(owner.level().getForceLoadedChunks());
            oldOwnerTeam=owner.getTeam();oldOtherTeam=other.getTeam();var board=game.server().getScoreboard();require(board.getPlayerTeam("hedgehog_fixture")==null,"fixture team unique");team=board.addPlayerTeam("hedgehog_fixture");team.setAllowFriendlyFire(false);
        }
        int cost(){return game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("baffled_wand_mana_use"),"lbc.math").get();}
        void initialize(){
            require(before.stream().noneMatch(e->!e.isRemoved()&&!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<40*40),"isolated empty helper region: "+before.stream().filter(e->!e.isRemoved()&&!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<40*40).limit(6).map(e->e.getClass().getSimpleName()+"/"+e.getUUID()+"@"+e.position()).toList());
            var b=BlockPos.containing(at);for(int x=(b.getX()-24)>>4;x<=(b.getX()+24)>>4;x++)for(int z=(b.getZ()-24)>>4;z<=(b.getZ()+24)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!previousForced.contains(key)){owner.level().setChunkForced(x,z,true);forced.add(key);}owner.level().getChunk(x,z);}
            for(var p:BlockPos.betweenClosed(b.offset(-4,-1,-3),b.offset(4,14,14)))require(owner.level().getBlockState(p).isAir(),"fixture source terrain is empty air");
            game.config().offhandWands=true;game.config().ranked=false;game.config().lbcSkills=false;
            game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("baffled_wand_mana_use"),"lbc.math").set(2500);
            for(var p:List.of(owner,other)){p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.setNoGravity(true);p.setShiftKeyDown(false);p.setSprinting(false);p.setOnGround(false);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setAbsorptionAmount(0);p.damageCooldownTime=0;p.setInvulnerableTime(0);p.entityTags().removeAll(Set.of("sin_greed_active","time_stopped"));
                game.items().mana().noMagic(p,1000);game.items().mana().set(p,100000);var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);}
            var board=game.server().getScoreboard();board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
            move(owner,at);move(other,at.add(10,0,-10));
            for(int y=0;y<4;y++)place(BlockPos.containing(at.add(0,y,7)),Blocks.STONE);
        }
        void place(BlockPos p,net.minecraft.world.level.block.Block block){blocks.putIfAbsent(p.immutable(),game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,block.defaultBlockState(),2);}
        void move(ServerPlayer p,Vec3 point){p.teleportTo(p.level(),point.x,point.y,point.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        void held(ItemStack stack,InteractionHand hand){owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,stack);owner.setShiftKeyDown(false);}
        boolean input(InteractionHand hand){return owner.gameMode.useItem(owner,owner.level(),owner.getItemInHand(hand),hand).consumesAction();}
        void clear(){service.retireOwned(owner.getUUID(),true);service.retireOwned(other.getUUID(),true);
            // Native lethal fixture cows can emit both item drops and experience immediately.
            for(var actor:List.copyOf(owned))if(actor instanceof Cow&&!actor.isAlive())for(var drop:owner.level().getEntities((Entity)null,actor.getBoundingBox().inflate(3),e->!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb)))owned.add(drop);
            for(var e:List.copyOf(owned)){if(e instanceof LivingEntity l)game.items().indirectKills().clear(l);e.discard();}owned.clear();}
        Cow cow(Vec3 point){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(point);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);c.setHealth(100);require(owner.level().addFreshEntity(c),"resident native cow");owned.add(c);return c;}
        Silverfish helper(LbcHedgehog.Kind kind,Vec3 point){service.spawnHelper(owner,owner.level(),point,kind);var f=service.helpers().getLast();f.setNoGravity(true);return f;}
        void guards(){
            for(var hand:InteractionHand.values()){
                held(new ItemStack(LbcHedgehogItems.WAND),hand);game.items().mana().set(owner,2499);require(!input(hand)&&service.helpers().isEmpty()&&game.items().mana().get(owner)==2499,"ordinary exact2500 admission "+hand);
                game.items().mana().set(owner,2500);owner.entityTags().add("time_stopped");try{require(!input(hand)&&game.items().mana().get(owner)==2500,"per-player frozen admission");}finally{owner.entityTags().remove("time_stopped");}
                if(hand==InteractionHand.OFF_HAND){game.config().offhandWands=false;require(!input(hand),"disabled offhand gate");game.config().offhandWands=true;}
                require(input(hand)&&game.items().mana().get(owner)==0&&service.views().size()==1&&service.views().getFirst().kind()==LbcHedgehog.Kind.ORDINARY,"actual native ordinary cast/debit "+hand);
                require(service.helpers().getFirst().isNoAi()&&service.views().getFirst().timer()==8,"stationary gravity-enabled native fish and source timer8");clear();
            }
            held(new ItemStack(LbcHedgehogItems.WAND),InteractionHand.MAIN_HAND);game.items().mana().set(owner,2500);game.items().timeStop().begin(owner.level(),40);
            try{require(!input(InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==2500,"actual dimension Time Stop admission");}finally{game.items().timeStop().end(owner.level());}
            var stopper=new Marker(EntityTypes.MARKER,owner.level());stopper.setPos(at);stopper.entityTags().add("stopper_magic");owned.add(stopper);require(owner.level().addFreshEntity(stopper),"real stopper");game.items().magic().loaded(stopper);
            try{require(!input(InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==2500,"actual antimagic field admission");}finally{game.items().magic().unloaded(stopper);stopper.discard();game.items().magic().flushCleansing();}
        }
        void modes(){
            for(var hand:InteractionHand.values())for(int mode=1;mode<=5;mode++){
                var stack=new ItemStack(LbcHedgehogItems.ACADEMY);flag(stack,"mode",mode);held(stack,hand);game.items().mana().set(owner,0);owner.setShiftKeyDown(true);
                require(input(hand)&&flag(stack,"mode")==mode%5+1&&game.items().mana().get(owner)==0&&service.helpers().isEmpty(),"all5 free modes at0 mana actual hand "+hand+"/"+mode);
                owner.setShiftKeyDown(false);flag(stack,"mode",mode);game.items().mana().set(owner,999);require(!input(hand),"Academy exact1000 gate");
                game.items().mana().set(owner,1000);require(input(hand)&&game.items().mana().get(owner)==0&&service.views().getFirst().kind()==LbcHedgehog.Kind.values()[mode],"actual Academy mode dispatch/debit "+hand+"/"+mode);
                var f=service.helpers().getFirst();require(f.isNoAi()==(mode!=3),"Tank native AI, others stationary");if(mode==3)require(f.getMaxHealth()==100&&f.getHealth()==100&&f.getAttributeValue(Attributes.ATTACK_DAMAGE)==15&&f.getAttributeValue(Attributes.ATTACK_KNOCKBACK)==1,"native Tank attributes");clear();
            }
        }
        void timerAndArrow(){
            var f=helper(LbcHedgehog.Kind.ORDINARY,at.add(0,1,0));var target=cow(at.add(0,1,4));
            for(int i=0;i<7;i++)service.tick();require(service.projectiles().isEmpty()&&service.views().getFirst().timer()==1,"initial native source cadence7 quiet updates");service.tick();
            require(service.projectiles().size()==1&&service.views().getFirst().timer()==0,"eighth active update emits one real arrow");var arrow=service.projectiles().getFirst();
            require(arrow.getOwner()==f&&arrow.getPierceLevel()==100&&Math.abs(arrow.getDeltaMovement().length()-2)<1e-8,"physical arrow exact speed2/piercing100 and actual helper shooter");
            require(LbcIndirectKills.playerSource(arrow)==owner&&service.cause(arrow)!=null&&service.cause(arrow).player().equals(owner.getUUID()),"helper arrow retains independent player kill credit");
            arrow.setOwner(other);require(service.cause(arrow)==null&&LbcIndirectKills.playerSource(arrow)==other,"explicit native reflection is not overridden by the helper's original receipt");arrow.setOwner(f);
            // Exercise actual native Arrow collision rather than invoking its damage method.
            arrow.setPos(target.getX(),target.getY()+.5,target.getZ()-1);arrow.setDeltaMovement(0,0,2);arrow.tick();
            require(target.getHealth()<100&&target.getLastHurtByMob()==f,"actual nonlethal physical arrow retains the helper attacker");
            clear();
            var shooter=helper(LbcHedgehog.Kind.ORDINARY,at.add(0,1,0));shooter.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);shooter.setHealth(100);shooter.setPermanentlyInvulnerable(false);shooter.setInvulnerableTime(0);
            var thorny=cow(at.add(0,1,4));var armor=new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            armor.enchant(owner.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.THORNS),10);
            thorny.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,armor);
            boolean immune=owner.getAbilities().invulnerable,entityImmune=owner.isPermanentlyInvulnerable();int immunityTicks=owner.getInvulnerableTime();float hp=owner.getHealth();owner.getAbilities().invulnerable=false;owner.setPermanentlyInvulnerable(false);owner.setInvulnerableTime(0);
            try{
                for(int i=0;i<8;i++)service.tick();require(service.projectiles().size()==1,"native helper emits the thorns witness arrow");var actual=service.projectiles().getFirst();
                var second=cow(at.add(0,1,6));second.setHealth(1);
                actual.setPos(thorny.getX(),thorny.getY()+.5,thorny.getZ()-1);actual.setDeltaMovement(0,0,4);actual.tick();
                require(shooter.getHealth()<100&&owner.getHealth()==hp,"native thorns damages the actual firing helper and never the summoner");
                require(thorny.getLastDamageSource()!=null&&thorny.getLastDamageSource().getEntity()==shooter,"native arrow damage retains the actual shooter entity");
                require(second.isDeadOrDying()&&second.getKillCredit()==owner,"same native piercing collision awards player credit beyond a thorns target: hp="+second.getHealth()+",killer="+second.getKillCredit()+",arrowRemoved="+actual.isRemoved()+",source="+second.getLastDamageSource()+",receipt="+service.cause(actual));
            }finally{owner.getAbilities().invulnerable=immune;owner.setPermanentlyInvulnerable(entityImmune);owner.setInvulnerableTime(immunityTicks);clear();}
        }
        void beams(){
            var f=helper(LbcHedgehog.Kind.DEFAULT,at.add(0,1,0));var target=cow(at.add(0,1,4));
            for(int i=0;i<8;i++)service.tick();require(target.getHealth()==96&&target.getLastHurtByMob()==f,"Default source4 beam retains its native helper attacker");clear();
            f=helper(LbcHedgehog.Kind.HEALER,at.add(0,1,0));move(owner,at.add(8,0,0));other.setHealth(8);move(other,at.add(0,1,4));for(int i=0;i<8;i++)service.tick();
            require(other.getHealth()>8,"actual Healer repeated allied-cell instantaneous health");other.setHealth(20);move(other,at.add(10,0,-10));move(owner,at);clear();
            f=helper(LbcHedgehog.Kind.LASER,at.add(0,1,0));target=cow(at.add(0,1,4));place(BlockPos.containing(at.add(0,1,2)),Blocks.STONE);place(BlockPos.containing(at.add(0,2,2)),Blocks.STONE);
            service.tick();require(target.getHealth()<99.98f&&target.getHealth()>99.5f&&target.getLastHurtByMob()==f,"Laser source.02 per-cell generic beam passes solids and source imported generic tag permits repeated sample damage");
            for(int y=1;y<=2;y++)owner.level().setBlock(BlockPos.containing(at.add(0,y,2)),Blocks.AIR.defaultBlockState(),2);clear();
            f=helper(LbcHedgehog.Kind.SNIPER,at.add(0,1,0));f.getRandom().setSeed(0x4e4545444c45L);target=cow(at.add(0,1,4));
            for(int i=0;i<1000&&target.getHealth()==100;i++)service.tick();require(target.getHealth()==80&&target.getLastHurtByMob()==f,"seeded actual Sniper chance fires source20 arrow beam");clear();
            var tank=helper(LbcHedgehog.Kind.TANK,at.add(0,1,0));target=cow(at.add(0,1,1));require(tank.doHurtTarget(owner.level(),target)&&target.getHealth()==85,"native Tank15 melee");
            target.setHealth(1);target.damageCooldownTime=0;target.setInvulnerableTime(0);require(tank.doHurtTarget(owner.level(),target)&&target.isDeadOrDying()&&target.getKillCredit()==owner,"actual native Tank lethal melee inherits caster credit");
            float hp=other.getHealth();require(!tank.doHurtTarget(owner.level(),other)&&other.getHealth()==hp,"cast Tank protects allied player at impact");clear();
        }
        void wallTrial(){
            var stack=new ItemStack(LbcHedgehogItems.WAND);flag(stack,"upgradable",1);flag(stack,"upgrade_available",0);held(stack,InteractionHand.OFF_HAND);owner.setShiftKeyDown(true);game.items().mana().set(owner,2500);
            require(input(InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==0&&flag(stack,"upgrade_available")==1,"actual offhand sneak2500 trial immediately readies used stack");
            var walls=service.helpers().stream().filter(e->e.entityTags().contains("baffled_wall")).toList();require(walls.size()==65&&walls.stream().filter(Entity::isPassenger).count()==60,"five native13-high passenger towers");
            require(walls.stream().allMatch(e->e.getHealth()==30&&e.getMaxHealth()==30&&e.getAttributeValue(Attributes.ATTACK_DAMAGE)==10&&e.getAttributeValue(Attributes.ARMOR)==10&&e.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==10),"all65 authored wall attributes");
            require(walls.stream().filter(e->e.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)).count()==5,"only5 tower bases have source slow-falling");
            var wall=walls.getFirst();float expected=switch(owner.level().getDifficulty()){case EASY->6;case HARD->15;case PEACEFUL->0;default->10;};require(expected>0,"combat fixture requires nonpeaceful difficulty");
            float hp=other.getHealth();other.damageCooldownTime=0;other.setInvulnerableTime(0);boolean allyHit=wall.doHurtTarget(owner.level(),other);require(allyHit&&Math.abs(other.getHealth()-(hp-expected))<1e-5,"source wall trap WILD against allied player: accepted="+allyHit+", before="+hp+", after="+other.getHealth()+", expected="+expected+", armor="+other.getArmorValue()+", difficulty="+owner.level().getDifficulty());
            owner.damageCooldownTime=0;owner.setInvulnerableTime(0);hp=owner.getHealth();boolean selfHit=wall.doHurtTarget(owner.level(),owner);require(selfHit&&Math.abs(owner.getHealth()-(hp-expected))<1e-5,"source wall trap WILD against caster: accepted="+selfHit+", before="+hp+", after="+owner.getHealth()+", expected="+expected);
            owner.setHealth(20);other.setHealth(20);owner.setShiftKeyDown(false);
            move(owner,wall.position().add(6,0,6));move(other,wall.position().add(0,0,-4));other.damageCooldownTime=0;other.setInvulnerableTime(0);
            float beamDamage=switch(owner.level().getDifficulty()){case EASY->3;case HARD->6;case PEACEFUL->0;default->4;};
            service.fireBeam(wall,other);require(other.getHealth()==20-beamDamage&&other.getLastDamageSource().getEntity()==wall,"actual unobstructed native NPC wall beam captures WILD cause with native difficulty scaling: hp="+other.getHealth()+",owner="+owner.position()+",wall="+wall.position()+",target="+other.position());
            require(walls.stream().allMatch(e->e.getHealth()==30),"wall beam passes through all65 live own tower bodies without self damage");
            service.forget(owner);require(!wall.isRemoved(),"hostile wall persists after author-departure callback");move(other,at.add(10,0,-10));other.setHealth(20);
            clear();require(service.trackedCount()==0,"explicit owned wall retirement removes all native tower riders");
        }
        void advancement(){
            var advancement=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse("lbc:true_advancements/staffs/hedgehog"));require(advancement!=null,"authored helper advancement packaged");
            for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,criterion);
            for(int i=0;i<39;i++)helper(LbcHedgehog.Kind.ORDINARY,at.add((i%5)*.1,1,(i/5)*.1));
            held(new ItemStack(LbcHedgehogItems.WAND),InteractionHand.MAIN_HAND);game.items().mana().set(owner,2500);
            require(input(InteractionHand.MAIN_HAND)&&service.views().size()==40&&owner.getAdvancements().getOrStartProgress(advancement).isDone(),"actual40-helper source achievement is independent of wall trial");clear();
        }
        boolean prepareSneeze(){
            var level=owner.level();var center=new BlockPos(BlockPos.containing(at).getX()+96,level.getMaxY()-2,BlockPos.containing(at).getZ());
            require(++sneezeWarmTicks<=200,"native isolated blast entity chunks become ready");
            boolean ready=true;
            for(int x=(center.getX()-48)>>4;x<=(center.getX()+48)>>4;x++)for(int z=(center.getZ()-48)>>4;z<=(center.getZ()+48)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(sneezeWarmTicks==1){if(!previousForced.contains(key)&&forced.add(key))level.setChunkForced(x,z,true);level.getChunk(x,z);}
                ready&=level.areEntitiesLoaded(key);
            }
            return ready;
        }
        void sneeze(){
            require(game.items().nuclearVisuals().count()==0&&service.pendingBlasts()==0,"exclusive empty nuclear and native-blast queues");
            var level=owner.level();var center=new BlockPos(BlockPos.containing(at).getX()+96,level.getMaxY()-2,BlockPos.containing(at).getZ());var origin=Vec3.atCenterOf(center);
            require(level.getAllEntities()!=null,"native level available");
            for(int x=(center.getX()-48)>>4;x<=(center.getX()+48)>>4;x++)for(int z=(center.getZ()-48)>>4;z<=(center.getZ()+48)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!previousForced.contains(key)&&forced.add(key))level.setChunkForced(x,z,true);level.getChunk(x,z);
            }
            for(var e:level.getAllEntities())require(e.isRemoved()||e.position().distanceToSqr(origin)>70*70,"isolated native blast region excludes every unrelated entity: "+e.getClass().getSimpleName()+"@"+e.position());
            for(var point:BlockPos.betweenClosed(center.offset(-48,-56,-48),new BlockPos(center.getX()+48,level.getMaxY()-1,center.getZ()+48)))require(level.getBlockState(point).isAir(),"native blast fixture requires empty bounded reachable terrain: "+point);
            var original=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(original::add);
            var griefing=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);int protection=game.config().tntProtection;
            long completed=service.completedBlasts();
            try(var first=new Swap(game,owner);var second=new Swap(game,other)){
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,true,game.server());game.config().tntProtection=3;
                place(center,Blocks.STONE);place(center.offset(2,0,0),Blocks.TNT);
                var source=helper(LbcHedgehog.Kind.ORDINARY,origin);source.setHealth(1);service.sneeze(source);
                long firstSlice=service.completedBlasts()-completed;
                require(firstSlice>=1&&firstSlice<=4&&service.pendingBlasts()==32-firstSlice,"at least one immediate real native blast and bounded32-offset queue");
                int terrainSteps=0;
                while(!level.getBlockState(center).isAir()||!level.getBlockState(center.offset(2,0,0)).isAir()){
                    require(++terrainSteps<200,"coherent native terrain reaches both probes within bounded slices");service.tickBlasts();
                }
                require(level.getBlockState(center).isAir(),"actual native power20 MOB blast destroys the stone probe despite TNT protection3");
                require(!source.isAlive(),"actual source fish is included in native blast exposure");
                require(service.blastViews().size()==1&&owner.getUUID().equals(service.blastViews().getFirst().owner()),"queued remainder retains original WILD owner");
                var nativeTnt=service.children().stream().filter(net.minecraft.world.entity.item.PrimedTnt.class::isInstance).toList();
                require(nativeTnt.size()==1&&service.owner(nativeTnt.getFirst())==owner&&service.cause(nativeTnt.getFirst())!=null&&service.cause(nativeTnt.getFirst()).friendlyFire(),"genuine TNT block primes with captured WILD lineage");
                require(level.getAllEntities()!=null,"native source remains live");
                for(var e:level.getAllEntities())require(original.contains(e)||!(e instanceof net.minecraft.world.entity.monster.Creeper),"native sneeze creates no creeper workaround actors");
                require(game.items().nuclearVisuals().count()==1,"exactly one shared nuclear shader visual");
                for(var recording:List.of(first.recording,second.recording)){
                    long messages=recording.sent.stream().filter(net.minecraft.network.protocol.game.ClientboundSystemChatPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundSystemChatPacket.class::cast)
                            .filter(packet->packet.content().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("skywars2.hedgehog.sneeze")&&"Ёжик чихнул.".equals(t.getFallback())).count();
                    require(messages==1,"real sneeze broadcast retains exact Russian fallback for both players");
                }
                source.discard();int waiting=service.pendingBlasts();long before=service.completedBlasts();game.items().timeStop().begin(level,40);
                try{service.tickBlasts();require(service.pendingBlasts()==waiting&&service.completedBlasts()==before,"native batch pauses with its own dimension Time Stop");}finally{game.items().timeStop().end(level);}
                service.tickBlasts();require(service.completedBlasts()>before&&service.pendingBlasts()<waiting,"accepted native burst continues after actual source fish death/removal");
            }finally{
                // All real queued blasts/primed block children are owned by this exclusive probe.
                service.stop();game.items().nuclearVisuals().stop();game.items().timeStop().end(level);
                for(var e:level.getAllEntities())if(!original.contains(e)&&e.position().distanceToSqr(origin)<70*70&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))e.discard();
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,griefing,game.server());game.config().tntProtection=protection;clear();
            }
        }
        void lifecycle(){
            var f=helper(LbcHedgehog.Kind.DEFAULT,at.add(0,1,0));int timer=service.views().getFirst().timer();game.items().timeStop().begin(owner.level(),40);
            try{for(int i=0;i<4;i++)service.tick();require(!f.isRemoved()&&service.views().getFirst().timer()==timer,"dimension stop pauses cadence without deleting helper");}finally{game.items().timeStop().end(owner.level());}
            service.unloaded(f);require(f.isRemoved()&&service.trackedCount()==0,"actual unloaded callback retires actor");
            f=helper(LbcHedgehog.Kind.TANK,at.add(0,1,0));service.forget(owner);require(f.isRemoved()&&service.trackedCount()==0,"owner departure closes actor");
        }
        boolean step(){if(owner.level().getGameTime()==last)return false;last=owner.level().getGameTime();require(game.playing()&&game.items().currentEpoch(epoch),"match epoch retained");
            if(phase==6&&!prepareSneeze())return false;
            switch(phase++){case 0->guards();case 1->modes();case 2->timerAndArrow();case 3->beams();case 4->advancement();case 5->wallTrial();case 6->sneeze();case 7->{lifecycle();return true;}default->throw new IllegalStateException();}return false;}
        @Override public void close(){if(closed)return;closed=true;try{game.items().timeStop().end(owner.level());clear();
            for(var p:List.of(owner,other))game.items().indirectKills().clear(p);for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());
            restore(game.config(),config);game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("baffled_wand_mana_use"),"lbc.math").set(cost);
            var board=game.server().getScoreboard();for(var p:List.of(owner,other))if(p.getTeam()==team)board.removePlayerFromTeam(p.getScoreboardName(),team);if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldOtherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOtherTeam);board.removePlayerTeam(team);
            saved.restore(game);otherSaved.restore(game);game.players().save();game.items().magic().flushCleansing();
            }finally{for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.initialize();}catch(RuntimeException|Error e){try{f.close();}finally{running=null;}throw e;}}
    public static void runSource(GameController game){require(running==null,"exclusive source fixture");try(var f=new Fixture(game)){f.initialize();f.timerAndArrow();f.beams();f.wallTrial();}SkyWars2.LOGGER.info("[check] PASS hedgehog-source (native NPC arrow/beam attacker, actual guaranteed thorns on helper not summoner, same piercing collision lethal player credit, reflected author, native Tank kill and WILD wall melee/beam)");}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS hedgehog");}}catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL hedgehog",error);}}
    private static List<Entity> retained;private static String stopEpoch;
    public static void stopProbe(GameController game){require(retained==null&&running==null&&game.items().hedgehog().trackedCount()==0&&game.items().hedgehog().pendingBlasts()==0,"exclusive retained probe");var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null&&game.canUseGameplayItems(owner),"actual stop owner");
        var f=game.items().hedgehog();var saved=new Saved(game,owner);try{f.spawnHelper(owner,owner.level(),owner.position().add(0,10,0),LbcHedgehog.Kind.DEFAULT);f.spawnHelper(owner,owner.level(),owner.position().add(0,10,1),LbcHedgehog.Kind.TANK);
            retained=new ArrayList<>(f.helpers());require(retained.size()==2,"retained native helper and Tank");retained.forEach(e->e.entityTags().add("time_stopped"));
            // Every blast ray remains above build height; no actual participant or terrain is in range.
            var source=f.spawnHelper(owner,owner.level(),new Vec3(owner.getX(),owner.level().getMaxY()+96,owner.getZ()),LbcHedgehog.Kind.ORDINARY);require(source!=null,"retained native burst origin");source.setHealth(1);f.sneeze(source);
            require(f.pendingBlasts()>0,"actual accepted burst retains pending native power20 offsets");retained.add(source);game.items().timeStop().begin(owner.level(),200);stopEpoch=game.items().epoch();
        }finally{saved.restore(game);}SkyWars2.LOGGER.info("[check] PASS hedgehog-stop-probe");}
    public static void cleanup(GameController game){require(retained!=null,"retained probe exists");try{require(!game.items().currentEpoch(stopEpoch)&&game.items().hedgehog().trackedCount()==0&&game.items().hedgehog().pendingBlasts()==0&&retained.stream().allMatch(Entity::isRemoved),"actual match stop invalidates epoch and removes retained helper/Tank/native burst queue");}finally{retained.forEach(Entity::discard);retained=null;stopEpoch=null;}SkyWars2.LOGGER.info("[check] PASS hedgehog-cleanup");}
}
