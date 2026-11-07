package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real native launches/arming, allied payloads, impact physics, cloud publication and tracker packets. */
public final class NativeTestReportCombatChecks {
    private NativeTestReportCombatChecks(){}
    @SuppressWarnings("unchecked")
    private static List<net.minecraft.network.protocol.Packet<?>> packets(RegenerationBeltChecks.ScheduledClient client){
        return (List<net.minecraft.network.protocol.Packet<?>>)WoolBucketChecks.read((Object)client.connection,"packets");
    }
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Native test report combat: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;
        try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(f.game==game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS native-test-report-combat (WILD self/ally utilities, real owner arming, pipe impact origin/ordinary rendering, tree cloud isolation, firecharge and native correction packets; physical client offset remains unverified)");}
        catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL native-test-report-combat",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> baseline=new HashSet<>();
        final String[] names={"ruchka","honew","bucket_projectile","throwable_pumpkin"};
        int phase,index,when,corrections;Snowball shot;Entity fire;boolean closed;
        Fixture(GameController game){this.game=game;
            require(game.items().specialFireballs().trackedCount()==0&&game.items().metalPipe().trackedCount()==0&&game.items().mysticalTree().count()==0&&game.items().psycho().effectCount()==0,"exclusive native actor services");
            f=new WoolBucketChecks.Fixture(game,false);
            for(var e:f.level.getAllEntities())baseline.add(e.getUUID());
        }
        void begin(){
            var prototypes=new ArrayList<String>(List.of(names));prototypes.addAll(List.of("throwable_metal_pipe","throwable_small_fireball","mystical_tree"));
            for(var name:prototypes){var stat=net.minecraft.stats.Stats.ITEM_USED.get(f.leaf(name).getItem());f.usedStats.putIfAbsent(stat,f.owner.getStats().getValue(stat));}
            f.prepareParticipants();
            var team=game.server().getScoreboard().addPlayerTeam("native_report_combat");team.setAllowFriendlyFire(false);
            for(var p:List.of(f.owner,f.other))game.server().getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(f.home).offset(-2,0,-2),BlockPos.containing(f.home).offset(2,4,20)))f.set(pos,Blocks.AIR.defaultBlockState());
            allies();startSelf();
        }
        void reset(ServerPlayer target){
            game.items().psycho().clearEffects(target);target.stopUsingItem();target.removeAllEffects();target.getInventory().clearContent();
            target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setHealth(1000);target.setDeltaMovement(Vec3.ZERO);
            game.items().magic().inventoryChanged(target);game.items().magic().flushCleansing();
            target.getInventory().setItem(9,new ItemStack(Items.DIAMOND,5));
        }
        void collect(){for(var e:f.level.getAllEntities())if(!baseline.contains(e.getUUID()))f.own(e);}
        void verify(int kind,ServerPlayer target,Snowball ball){
            require(ball.isRemoved(),"one terminal payload "+names[kind]);
            if(kind==0)require(game.items().psycho().hudValue(target,"twitching")==600,"actual native Twitching on WILD target");
            if(kind==1)require(target.getInventory().getItem(9).isEmpty()&&f.level.getEntitiesOfClass(ItemEntity.class,target.getBoundingBox().inflate(3),e->e.getItem().is(Items.DIAMOND)&&e.getItem().getCount()==5).size()==1,"exact native inventory ejection on WILD target");
            if(kind==2)require(target.getHealth()<1000&&ItemData.intValue(target.getItemBySlot(EquipmentSlot.HEAD),"bucket_head",0)==1,"native allied/self bucket damage and helmet");
            if(kind==3)require(target.getHealth()<1000&&target.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN),"native allied/self pumpkin damage and helmet");
            collect();
            // Ejections are fixture products; remove before the next independent target check.
            for(var e:List.copyOf(f.level.getEntitiesOfClass(ItemEntity.class,new AABB(f.home,f.home).inflate(35))))if(!baseline.contains(e.getUUID()))f.discard(e);
            game.items().psycho().clearEffects(target);
        }
        void allies(){
            f.move(f.owner,f.home);f.move(f.other,f.home.add(12,0,0));
            require(!game.items().wands().projectileTarget(f.owner,f.other),"real protected allied control");
            for(int kind=0;kind<names.length;kind++){
                reset(f.other);var ball=f.launch(f.leaf(names[kind]),InteractionHand.MAIN_HAND);
                ball.setPos(f.other.position().add(0,1,0));ball.setDeltaMovement(Vec3.ZERO);
                game.items().thrownUtilities().beforeTick(ball);verify(kind,f.other,ball);
            }
        }
        void startSelf(){
            reset(f.owner);f.move(f.owner,f.home);f.move(f.other,f.home.add(25,0,0));
            shot=f.launch(f.leaf(names[index]),InteractionHand.MAIN_HAND);
            require(!((LbcThrownUtilities.OwnerGate)shot).skywars2$leftOwner(),"actual native launch starts unarmed");
            game.items().thrownUtilities().beforeTick(shot);require(!shot.isRemoved(),"thrower proximity cannot consume launch");
            when=game.server().getTickCount();phase=0;
        }
        ItemEntity launchPipe(){
            var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            var stack=f.leaf("throwable_metal_pipe").copyWithCount(3);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==2,"real native throwable pipe debit");
            var found=f.level.getEntitiesOfClass(ItemEntity.class,f.owner.getBoundingBox().inflate(4),e->!before.contains(e.getUUID())&&e.entityTags().contains(LbcMetalPipe.THROWN));
            require(found.size()==1,"one true physical thrown pipe");collect();return found.getFirst();
        }
        void pipes(){
            reset(f.owner);f.move(f.owner,f.home);f.move(f.other,f.home.add(25,0,0));
            var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(f.home.add(5,0,0));cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);require(f.level.addFreshEntity(cow),"real living impact control");f.own(cow);
            var pipe=launchPipe();var context=net.fabricmc.fabric.api.networking.v1.context.PacketContext.get();
            require(eu.pb4.polymer.core.api.entity.PolymerEntity.get(pipe).getPolymerEntityType(context)==EntityTypes.MARKER,"thrown body hidden behind aligned display");
            pipe.setPos(cow.position().add(-2,1,0));pipe.setDeltaMovement(Vec3.ZERO);game.items().metalPipe().tick();
            pipe.setPos(cow.position().add(.5,1,0));game.items().metalPipe().tick();
            require(cow.getHealth()<100&&cow.getDeltaMovement().x>0,"native living contact damages and knocks away from impact left of victim, independently of distant thrower: hp="+cow.getHealth()+" motion="+cow.getDeltaMovement()+" cow="+cow.position()+" pipe="+pipe.position()+" tags="+pipe.entityTags());
            f.move(f.owner,f.home.add(12,0,0));reset(f.owner);
            pipe.setPos(f.owner.position().add(-2,1,0));pipe.setDeltaMovement(Vec3.ZERO);game.items().metalPipe().tick();
            pipe.setPos(f.owner.position().add(.5,1,0));game.items().metalPipe().tick();
            require(f.owner.getHealth()<1000&&f.owner.getDeltaMovement().x>0,"armed pipe can strike its own thrower with impact-centered knockback: hp="+f.owner.getHealth()+" motion="+f.owner.getDeltaMovement()+" owner="+f.owner.position()+" pipe="+pipe.position()+" tags="+pipe.entityTags());f.discard(pipe);f.discard(cow);
            var ordinary=new ItemEntity(f.level,f.home.x,f.home.y,f.home.z,f.loot("lbc:metal_pipe"));ordinary.setDeltaMovement(.3,.2,.1);require(f.level.addFreshEntity(ordinary),"ordinary source pipe control");f.own(ordinary);
            require(!ordinary.entityTags().contains(LbcMetalPipe.THROWN)&&eu.pb4.polymer.core.api.entity.PolymerEntity.get(ordinary).getPolymerEntityType(context)==EntityTypes.ITEM,"ordinary physical pipe retains native item rendering");f.discard(ordinary);
        }
        int positions(){int count=0;for(var packet:packets(f.client)){
            if(packet instanceof ClientboundEntityPositionSyncPacket sync&&sync.id()==fire.getId())count++;
            if(packet instanceof ClientboundMoveEntityPacket move&&move.getEntity(f.level)==fire&&move.hasPosition())count++;
        }return count;}
        void fireball(){
            f.move(f.owner,f.home);f.move(f.other,f.home.add(25,0,0));reset(f.owner);
            int sounds=packets(f.client).size();var stack=f.leaf("throwable_small_fireball").copyWithCount(3);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==2,"native fireball use/debit");
            fire=game.items().specialFireballs().snapshot().stream().filter(e->!e.isRemoved()).findFirst().orElseThrow();f.own(fire);
            require(packets(f.client).subList(sounds,packets(f.client).size()).stream().anyMatch(p->p instanceof ClientboundSoundPacket sound&&sound.getSound().value().location().equals(Identifier.parse("minecraft:item.firecharge.use"))),"actual restored firecharge sound packet");
            corrections=positions();when=game.server().getTickCount();phase=1;
        }
        void tree(){
            f.discard(fire);var point=new Vec3(f.home.x+12,11,f.home.z);
            for(var p:BlockPos.betweenClosed(BlockPos.containing(point).offset(-2,-2,-2),BlockPos.containing(point).offset(2,2,2)))f.remember(p);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,f.leaf("mystical_tree"));
            require(game.items().mysticalTree().use(f.owner,InteractionHand.MAIN_HAND,point),"actual tree owner creates terminal actor");collect();when=game.server().getTickCount();phase=2;
        }
        void cloud(Creeper creeper){try{var method=Creeper.class.getDeclaredMethod("spawnLingeringCloud");method.setAccessible(true);method.invoke(creeper);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        boolean step(){
            require(game.server().getTickCount()-f.started<240,"bounded native fixture");
            if(phase==0){
                require(!shot.isRemoved(),"native flight survives initial owner departure");
                if(!((LbcThrownUtilities.OwnerGate)shot).skywars2$leftOwner()){require(game.server().getTickCount()-when<12,"actual owner departure arms flight");return false;}
                shot.setPos(f.owner.position().add(0,1,0));shot.setDeltaMovement(Vec3.ZERO);game.items().thrownUtilities().beforeTick(shot);verify(index,f.owner,shot);
                if(++index<names.length){startSelf();return false;}pipes();fireball();return false;
            }
            if(phase==1){if(game.server().getTickCount()-when<4)return false;require(!fire.isRemoved()&&positions()-corrections>=2,"native tracker sends repeated position corrections for client-predicted fireball");tree();return false;}
            if(phase==2){
                collect();var creepers=f.level.getEntitiesOfClass(Creeper.class,new AABB(new Vec3(f.home.x+12,11,f.home.z),new Vec3(f.home.x+12,11,f.home.z)).inflate(4),e->e.entityTags().contains("skywars2.mystical_tree.creeper"));
                if(creepers.isEmpty()){require(game.server().getTickCount()-when<70,"actual tree terminal emits invisible creeper");return false;}
                var source=creepers.getFirst();require(source.hasEffect(MobEffects.INVISIBILITY),"native tree actor stays invisible");
                var bounds=source.getBoundingBox().inflate(4);int before=f.level.getEntitiesOfClass(AreaEffectCloud.class,bounds).size();cloud(source);
                require(f.level.getEntitiesOfClass(AreaEffectCloud.class,bounds).size()==before,"actual tree cloud publication leaves no invisibility residue");
                var ordinary=new Creeper(EntityTypes.CREEPER,f.level);ordinary.setPos(source.position());ordinary.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,1000));require(f.level.addFreshEntity(ordinary),"ordinary creeper control");f.own(ordinary);cloud(ordinary);collect();
                require(f.level.getEntitiesOfClass(AreaEffectCloud.class,bounds).size()==before+1,"ordinary creeper still publishes its native potion cloud");return true;
            }
            return false;
        }
        public void close(){if(closed)return;closed=true;collect();for(var p:List.of(f.owner,f.other))game.items().psycho().clearEffects(p);f.close();}
    }
}
