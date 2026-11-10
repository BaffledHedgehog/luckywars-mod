package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.*;
import java.util.*;

/** Explicit native source-branch checks. No command/function gameplay executor or fake damage target. */
public final class PozdmanChecks {
    private PozdmanChecks(){}
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException("Pozdman: "+reason);}
    private static Fixture running;
    public static void run(GameController game){
        require(running==null,"exclusive scheduled fixture");sourceBranches(game);running=new Fixture(game);
        try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw new IllegalStateException(error);}
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS pozdman-bot (native UseItem+offhand UseBlock/exact reply99/100dolphin; MAG allowed/TECH cast denied; dolphin+coins unjammable; actual TimeStop; installed x2/x4 clone; full coordinator nonempty dolphin+coin+wait replay; native fuse2 TNT; exact cleanup)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL pozdman-bot",error);}
    }
    private static void sourceBranches(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null&&game.playing()&&game.canUseGameplayItems(owner)&&!game.items().timeStop().stopped(owner.level()),"active native TestAlpha in running world");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var service=new LbcPozdmanBot(game,p->true);
        var nativeActors=new ArrayList<Entity>();var terrain=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        var level=owner.level();int mana=game.items().mana().get(owner);
        try{
            var item=new ItemStack(LbcPozdmanBot.ITEM);require(!item.isDamageableItem()&&item.get(DataComponents.ITEM_MODEL).toString().equals("lbc:pozdman_bot")&&item.get(DataComponents.LORE).lines().size()==2,"native source model, undamageable carrier and two authored lore lines");
            for(var hand:InteractionHand.values()){
                owner.setItemInHand(hand,item.copy());
                for(int roll=1;roll<100;roll++){
                    require(service.use(owner,hand,roll),"each of99 source replies through used hand");
                    require(!service.use(owner,hand,100)&&service.count()==0,"shared wait rejects second hand/rare result without actor");
                    // A use late in the same global phase waits only until that phase clears.
                    for(int tick=0;tick<20;tick++)service.tick();
                }
            }
            require(game.items().mana().get(owner)==mana,"standalone bot consumes no mana or skill resource");
            owner.setPos(owner.position().add(0,32,0));owner.setYRot(0);owner.setXRot(0);
            owner.setItemInHand(InteractionHand.MAIN_HAND,item);
            require(service.use(owner,InteractionHand.MAIN_HAND,100)&&service.count()==1,"actual100th result emits native dolphin");
            var dolphin=service.snapshot().getFirst();nativeActors.add(dolphin);
            require(dolphin.getType()==EntityTypes.DOLPHIN&&dolphin.isNoGravity()&&dolphin.position().distanceToSqr(owner.getEyePosition())<1e-9,"source eye origin and native noGravity dolphin body");
            var living=(LivingEntity)dolphin;require(living.getAttributeValue(Attributes.SCALE)==2&&living.getMaxHealth()==50&&game.items().communism().owner(dolphin)==owner,"source scale/maxHP and actual subjugated owner");
            int team=game.items().mana().score(owner,"team_number").get();var origin=dolphin.position();
            var enemy=new Cow(EntityTypes.COW,level);enemy.setNoAi(true);enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);enemy.setHealth(100);enemy.setPos(origin.add(0,0,10));nativeActors.add(enemy);require(level.addFreshEntity(enemy),"native enemy admission");
            game.items().mana().score(enemy,"team_number").set(team+1);service.loaded(enemy);
            var ally=new Cow(EntityTypes.COW,level);ally.setNoAi(true);ally.setPos(origin.add(0,0,2));nativeActors.add(ally);require(level.addFreshEntity(ally),"native ally admission");game.items().mana().score(ally,"team_number").set(team);service.loaded(ally);
            require(service.nearest(dolphin)==enemy,"pursuit uses1..200 exact team score and excludes nearer allied mob");
            service.dolphinTick(dolphin);require(Math.abs(dolphin.position().distanceTo(origin)-1)<1e-9,"native pursuit advances exactly one horizontal source block");
            var coin=service.spawnCoin(dolphin);require(coin!=null&&coin.getOwner()==owner,"physical coin inherits actual controller");nativeActors.add(coin);DisplayTransformChecks.pose(service.state(coin).display(),0,0,0,2,2,2);
            coin.setPos(enemy.position());coin.setDeltaMovement(Vec3.ZERO);
            float before=enemy.getHealth();require(service.coinContact(coin)&&coin.isRemoved()&&Math.abs(enemy.getHealth()-(before-15))<1e-6,"native falling_block15 accepted hit consumes physical coin");
            require(enemy.getLastDamageSource()!=null&&enemy.getLastDamageSource().is(DamageTypes.FALLING_BLOCK)&&enemy.getLastDamageSource().getEntity()==owner&&enemy.getLastDamageSource().getDirectEntity()==coin,"native direct coin and actual owner damage attribution");
            var safe=service.spawnCoin(dolphin);nativeActors.add(safe);safe.setPos(ally.position());safe.setDeltaMovement(Vec3.ZERO);require(!service.coinContact(safe)&&!safe.isRemoved()&&ally.getHealth()==ally.getMaxHealth(),"allied contact never damages or consumes");
            safe.setPos(origin.add(6,4,0));safe.setDeltaMovement(new Vec3(.2,0,.1));
            var block=BlockPos.containing(safe.position().add(0,1,0));terrain.put(block,game.maps().snapshot(level,block));level.setBlock(block,Blocks.STONE.defaultBlockState(),3);
            service.coinTick(safe);require(safe.isRemoved(),"source nonair block one above executes complete bonk/discard closure");
            var age=service.spawnCoin(dolphin);nativeActors.add(age);age.setPos(origin.add(6,10,0));age.setDeltaMovement(new Vec3(.2,0,.1));
            LbcPhysicalObjects.step(age,1,1,.8,1,(a,b)->false,(a,b)->false);require(Math.abs(age.getDeltaMovement().y+.08)<1e-9&&Math.abs(age.getDeltaMovement().x-.2)<1e-9,"shared physical .08 gravity and exact air friction1");
            var denied=new LbcPozdmanBot(game,p->false);require(!denied.use(owner,InteractionHand.MAIN_HAND,100)&&denied.count()==0,"injected author TECH gate blocks before waiting/spawn");
        }finally{
            service.stop();for(var actor:nativeActors){actor.discard();game.server().getScoreboard().resetAllPlayerScores(actor);}
            for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());saved.restore(game);
        }
        SkyWars2.LOGGER.info("[check] PASS pozdman-source-branches (both hands/99 replies/phase wait; actual1% native dolphin/subjugation/score selectors; physical coin native15/owner/allies/gravity; full nonair bonk closure; injected gate; scoped restoration)");
    }

    /** Only the bot's100-way draw is scripted; every native scheduler and actor random draw delegates. */
    private static final class Roll implements AutoCloseable {
        final ServerPlayer owner;final java.lang.reflect.Field random;final Object previous;int draws;final int result;
        Roll(ServerPlayer owner,int result){this.owner=owner;this.result=result;
            try{Class<?> type=owner.getClass();java.lang.reflect.Field found=null;while(type!=null){try{found=type.getDeclaredField("random");break;}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}random=Objects.requireNonNull(found);random.setAccessible(true);previous=random.get(owner);
                random.set(owner,java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{
                    if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(100)&&StackWalker.getInstance().walk(frames->frames.anyMatch(frame->frame.getClassName().equals(LbcPozdmanBot.class.getName())))){draws++;return result-1;}
                    try{return method.invoke(previous,args);}catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}
                }));
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        @Override public void close(){try{random.set(owner,previous);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcPozdmanBot service;
        final Map<ServerPlayer,Integer> teams=new IdentityHashMap<>();
        RegenerationBeltChecks.ScheduledClient client,audience;Marker mag;BlockPos techPos;
        Entity dolphin;LbcPozdmanBot.Coin coin;PrimedTnt bomb;
        UUID dolphinId,coinId,displayId;Vec3 dolphinAt,coinAt,tntContact;int coinAge,dolphinActions,stage,when,start,sequence,updates;boolean closed;
        Fixture(GameController game){this.game=game;service=game.items().pozdmanBot();require(service.count()==0&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"exclusive native actors and full coordinator");base=new ChefChecks.Fixture(game);for(var p:List.of(base.owner,base.other))teams.put(p,game.items().mana().score(p,"team_number").get());}
        void begin(int next){stage=next;when=game.server().getTickCount();}
        void init(){
            base.init();game.config().offhandWands=true;
            for(var player:List.of(base.owner,base.other)){game.items().mana().score(player,"team_number").set(0);player.getAbilities().invulnerable=true;}
            base.move(base.owner,base.at.add(-6,2,-6));base.move(base.other,base.at.add(12,2,-12));
            client=new RegenerationBeltChecks.ScheduledClient(game,base.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,base.other);
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcPozdmanBot.ITEM));mag=marker("stopper_magic",base.owner.position().add(5,0,0));
            updates=client.updates();start=game.server().getTickCount();begin(-1);
        }
        Marker marker(String tag,Vec3 at){var marker=new Marker(EntityTypes.MARKER,base.owner.level());marker.setPos(at);marker.entityTags().add(tag);require(marker.level().addFreshEntity(marker),"actual native jammer publication");game.items().magic().loaded(marker);return marker;}
        void removeMarker(Marker marker){if(marker!=null){var live=base.owner.level().getEntity(marker.getUUID());if(live!=null){game.items().magic().unloaded(live);live.discard();}}}
        void input(InteractionHand hand,boolean block,Integer result){
            if(result==null){packet(hand,block);return;}
            try(var roll=new Roll(base.owner,result)){packet(hand,block);require(roll.draws==1,"actual native hook reaches exactly one100-way bot draw");}
        }
        void packet(InteractionHand hand,boolean block){
            require(client.ready(),"native input ready "+client.diagnostics());
            if(block){var pos=base.center.offset(-6,1,-4);base.block(pos,Blocks.STONE);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);require(base.owner.isWithinBlockInteractionRange(pos,1),"native block interaction within reach");base.owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(hand,hit,++sequence));}
            else base.owner.connection.handleUseItem(new ServerboundUseItemPacket(hand,++sequence,0,0));
            base.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
        }
        @SuppressWarnings("unchecked") List<net.minecraft.network.protocol.Packet<?>> packets(){try{var connection=RegenerationBeltChecks.ScheduledClient.class.getDeclaredField("connection");connection.setAccessible(true);var wire=connection.get(client);var field=wire.getClass().getDeclaredField("packets");field.setAccessible(true);return (List<net.minecraft.network.protocol.Packet<?>>)field.get(wire);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        boolean text(String key){return packets().stream().anyMatch(packet->packet instanceof ClientboundSystemChatPacket message&&message.content().getContents() instanceof TranslatableContents content&&content.getKey().equals(key));}
        void positionParents(){dolphin.setPos(base.at.add(0,7,0));((Mob)dolphin).setNoAi(true);dolphin.setDeltaMovement(Vec3.ZERO);coin.setPos(base.at.add(6,8,0));coin.setDeltaMovement(new Vec3(.1,.02,.05));}
        void cloneBodies(){
            var beforeCoin=Set.copyOf(service.snapshot());require(game.items().cloneSkills().copyNow(coin,15),"actual installed x2 adapter accepts native physical coin");
            var copies=service.snapshot().stream().filter(e->!beforeCoin.contains(e)).toList();require(copies.size()==1&&copies.getFirst() instanceof LbcPozdmanBot.Coin,"exactly one native coin clone");
            var copy=(LbcPozdmanBot.Coin)copies.getFirst();require(copy.getOwner()==base.owner&&service.state(copy).age()==service.state(coin).age()&&service.state(copy).display()!=service.state(coin).display(),"fresh coin visual, lifetime and causal owner retained");
            require(!game.items().cloneSkills().copyNow(service.state(coin).display(),15),"visual child cannot independently clone");
            for(var clone:copies)service.removed(clone);
            var beforeDolphin=Set.copyOf(service.snapshot());require(game.items().cloneSkills().copyNow(dolphin,17),"actual installed x4 adapter accepts controlled dolphin");
            copies=service.snapshot().stream().filter(e->!beforeDolphin.contains(e)).toList();require(copies.size()==3,"exact three native dolphin copies");for(var clone:copies){require(game.items().communism().owner(clone)==base.owner,"dolphin clone controller remains actual owner");service.removed(clone);}
        }
        boolean step(){
            int elapsed=game.server().getTickCount()-when;require(game.server().getTickCount()-start<1800,"bounded actual scheduler "+stage+" "+game.vanity().status());
            if(game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            switch(stage){
                case -1->{if(elapsed<2)return false;require(game.items().magic().suppressed(base.owner)&&!game.items().magic().mechanismSuppressed(base.owner),"positive MAG source, absent TECH source");
                    packets().clear();input(InteractionHand.MAIN_HAND,false,99);require(text("pozdman_girl_99")&&service.waiting(base.owner),"native UseItem under MAG sends exact source reply99 and wait");input(InteractionHand.MAIN_HAND,false,null);require(service.count()==0,"global wait prevents native rare replay");begin(0);}
                case 0->{if(service.waiting(base.owner)){require(elapsed<=21,"original second phase clears on actual registered ticks");return false;}
                    base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcPozdmanBot.ITEM));input(InteractionHand.OFF_HAND,true,100);
                    require(text("akyla_privet")&&service.count()==1,"actual offhand UseBlock emits rare native result under MAG");dolphin=service.snapshot().getFirst();dolphinId=dolphin.getUUID();coin=service.spawnCoin(dolphin);require(coin!=null,"native physical coin before clone/full capture");coinId=coin.getUUID();positionParents();cloneBodies();
                    displayId=service.state(coin).display().getUUID();coinAge=service.state(coin).age();coinAt=coin.position();dolphinAt=dolphin.position();game.items().timeStop().begin(base.owner.level(),1000);begin(1);}
                case 1->{require(service.state(coin).age()==coinAge&&coin.position().equals(coinAt)&&dolphin.position().equals(dolphinAt)&&service.waiting(base.owner),"real world TimeStop freezes coin age/motion, native dolphin and global wait");if(elapsed<6)return false;
                    require(game.vanity().requestCapture(base.owner,false),"real full coordinator capture with native coin+dolphin+display+wait");begin(2);}
                case 2->{if(!game.vanity().status().equals("IDLE")||!game.vanity().hasSnapshot())return false;
                    service.forget(base.owner);require(service.count()==0&&coin.isRemoved()&&dolphin.isRemoved(),"future native bodies and owned state genuinely removed before rewind");
                    require(game.vanity().requestRestoreForCheck(),"request actual complete coordinator replay");begin(3);}
                case 3->{if(game.vanity().busy()||!game.vanity().status().equals("IDLE"))return false;
                    var level=base.owner.level();dolphin=level.getEntity(dolphinId);coin=(LbcPozdmanBot.Coin)level.getEntity(coinId);require(dolphin!=null&&coin!=null&&service.count()==2,"full coordinator recreates both actual native bodies");
                    require(service.state(coin)!=null&&service.state(coin).age()==coinAge&&service.state(coin).display()==level.getEntity(displayId)&&coin.getOwner()==base.owner&&game.items().communism().owner(dolphin)==base.owner,"full native replay rebinds visual, lifetime, coin author and dolphin controller");
                    require(coin.position().equals(coinAt)&&dolphin.position().equals(dolphinAt)&&service.waiting(base.owner)&&game.items().timeStop().stopped(level),"full replay restores exact stopped geometry and pending global wait");
                    client.acknowledge();audience.acknowledge();game.vanity().stop();game.items().timeStop().end(level);begin(4);}
                case 4->{if(elapsed<3)return false;require(!coin.isRemoved()&&service.state(coin).age()>coinAge,"native registered actor scheduler resumes after TimeStop/replay");
                    techPos=BlockPos.containing(base.at.add(0,3,-4));base.block(techPos,LbcMagic.MECHANISM_BEACON_BLOCK);coin.setPos(base.at.add(6,8,0));coin.setDeltaMovement(new Vec3(0,.4,0));coinAge=service.state(coin).age();dolphinActions=service.state(dolphin).age();begin(5);}
                case 5->{if(elapsed<25)return false;require(game.items().magic().mechanismSuppressed(base.owner)&&game.items().magic().mechanismSuppressedAt(base.owner.level(),dolphin.position()),"positive TECH covers actual owner and native actor");
                    require(!coin.isRemoved()&&!dolphin.isRemoved()&&service.state(coin).age()>coinAge&&service.state(dolphin).age()>dolphinActions,"native dolphin actions and physical coin survive full common TECH pulse and keep ticking");
                    if(service.waiting(base.owner))return false;int beforeCount=service.count();input(InteractionHand.OFF_HAND,false,null);require(!service.waiting(base.owner)&&service.count()==beforeCount,"positive TECH blocks actual cast before wait or new actor");
                    base.block(techPos,Blocks.AIR);require(!game.items().magic().mechanismSuppressedAt(base.owner.level(),dolphin.position()),"removed native TECH block unindexes its exact field");techPos=null;removeMarker(mag);mag=null;var solid=base.center.offset(0,6,0);base.block(solid,Blocks.STONE);tntContact=Vec3.atCenterOf(solid);dolphin.setPos(tntContact);dolphin.setDeltaMovement(Vec3.ZERO);
                    var pursuit=base.cow(base.at.add(.5,6.5,6.5));game.items().mana().score(pursuit,"team_number").set(1);dolphin.setYRot(0);dolphin.setXRot(0);dolphinActions=service.state(dolphin).age();
                    require(service.nearest(dolphin)==pursuit&&base.owner.level().getBlockState(solid).is(LbcPozdmanBot.SOLID),"native source starts inside actual solid tag with eligible pursuit out of contact cell");begin(6);}
                case 6->{
                    var bombs=base.owner.level().getEntitiesOfClass(PrimedTnt.class,new net.minecraft.world.phys.AABB(tntContact,tntContact).inflate(3),e->e.getOwner()==base.owner);
                    if(bombs.isEmpty()){
                        var state=service.state(dolphin);String diagnostic="elapsed="+elapsed+", entry="+tntContact+", actual="+dolphin.position()+", velocity="+dolphin.getDeltaMovement()+", tickCount="+dolphin.tickCount+", actionBefore="+dolphinActions+", actionNow="+(state==null?"missing":state.age())+", contactBlock="+base.owner.level().getBlockState(BlockPos.containing(tntContact))+", solid="+base.owner.level().getBlockState(BlockPos.containing(tntContact)).is(LbcPozdmanBot.SOLID)+", stopped="+game.items().timeStop().stopped(base.owner.level())+", frozen="+game.items().timeStop().frozen(dolphin)+", TECH="+game.items().magic().mechanismSuppressedAt(base.owner.level(),dolphin.position())+", epoch="+(state!=null&&game.items().currentEpoch(state.epoch()))+", removed="+dolphin.isRemoved();
                        if(elapsed==1)SkyWars2.LOGGER.info("[check] Pozdman native contact pending: {}",diagnostic);
                        require(elapsed<5,"native dolphin HEAD contact emits TNT before pursuit/travel: "+diagnostic);return false;
                    }
                    bomb=bombs.getFirst();require(bomb.getFuse()>0&&bomb.getFuse()+bomb.tickCount==2,"actual native fuse starts at2 and only registered native ticks decrement it; fuse="+bomb.getFuse()+", age="+bomb.tickCount);
                    require(service.state(dolphin).age()==dolphinActions+1&&bomb.position().distanceToSqr(tntContact)<.05,"exactly one native source action emits at pre-pursuit command origin");base.block(base.center.offset(0,6,0),Blocks.AIR);begin(7);}
                case 7->{if(elapsed<3)return false;require(bomb.isRemoved(),"native fuse2 actor actually completes scheduled explosion");require(client.updates()>updates,"registered real network scheduler advanced");return true;}
                default->throw new IllegalStateException("Unknown Pozdman fixture state "+stage);
            }
            return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->game.vanity().stop(),()->game.items().timeStop().end(base.owner.level()),service::stop,
                    ()->{removeMarker(mag);if(techPos!=null)base.block(techPos,Blocks.AIR);if(bomb!=null)bomb.discard();},
                    ()->{if(client!=null)client.close();if(audience!=null)audience.close();},base::close,
                    ()->teams.forEach((p,value)->game.items().mana().score(p,"team_number").set(value))))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Pozdman scheduled scoped restoration",failure);
        }
    }
}
