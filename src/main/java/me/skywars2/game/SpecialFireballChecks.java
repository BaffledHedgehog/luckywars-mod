package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual snowball use/load conversion and native SmallFireball contacts; no simulated flight. */
public final class SpecialFireballChecks {
    private SpecialFireballChecks(){}
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Special fireballs: "+why);}
    private static final String[] FLAGS={"smallfireball","fireball","dragfireball","smallfireball_imba","fireball_imba","dragfireball_imba"};
    static ItemStack token(int index,int count){var stack=new ItemStack(Items.SNOWBALL,count);var data=new CompoundTag();data.putInt("special",1);data.putInt(FLAGS[index],1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcSpecialFireballs service;final ServerPlayer owner,other;final HedgehogChecks.Saved saved,otherSaved;final SauvojenMeditationChecks.Attribution attribution;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final Set<Long> forced=new HashSet<>();final Vec3 at;
        Fixture(GameController game){this.game=game;service=game.items().specialFireballs();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&other.level()==owner.level()&&!game.items().timeStop().stopped(owner.level()),"two active native participants");
            require(service.trackedCount()==0&&service.pendingCount()==0,"exclusive empty service");saved=new HedgehogChecks.Saved(game,owner);otherSaved=new HedgehogChecks.Saved(game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));owner.level().getAllEntities().forEach(before::add);
            at=new Vec3((owner.blockPosition().getX()>>4)*16+8.37,270,(owner.blockPosition().getZ()>>4)*16+8.37);
        }
        void initialize(){
            var base=BlockPos.containing(at);for(int x=(base.getX()-24)>>4;x<=(base.getX()+24)>>4;x++)for(int z=(base.getZ()-24)>>4;z<=(base.getZ()+24)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);
            }
            for(var pos:BlockPos.betweenClosed(base.offset(-4,-1,-4),base.offset(4,5,18)))require(owner.level().getBlockState(pos).isAir(),"empty physical fixture volume");
            require(before.stream().noneMatch(e->!(e instanceof ServerPlayer)&&!e.isRemoved()&&e.position().distanceToSqr(at)<900),"empty entity fixture volume");
            owner.getInventory().clearContent();owner.setGameMode(GameType.SURVIVAL);owner.removeAllEffects();owner.setNoGravity(true);owner.setShiftKeyDown(false);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);owner.setDeltaMovement(Vec3.ZERO);
            // Raw snowballs are physical items; the caster's no-magic status does not reject conversion.
            game.items().mana().noMagic(owner,1000);game.items().magic().inventoryChanged(owner);
        }
        void clear(){service.stop();for(var e:owner.level().getAllEntities())if(!before.contains(e)&&!(e instanceof ServerPlayer)){
            if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}}
        Snowball source(int kind,Vec3 position,Vec3 motion){var ball=new Snowball(owner.level(),owner,token(kind,1));ball.setPos(position);ball.setYRot(0);ball.setXRot(0);ball.setDeltaMovement(motion);
            require(owner.level().addFreshEntity(ball),"actual native snowball load callback");require(service.pendingCount()==1,"callback captures exactly one source");return ball;}
        void rawUses(){
            for(int kind=0;kind<6;kind++)for(var hand:InteractionHand.values()){
                var stack=token(kind,2);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,stack);
                int stat=owner.getStats().getValue(Stats.ITEM_USED.get(Items.SNOWBALL));
                require(owner.gameMode.useItem(owner,owner.level(),stack,hand).consumesAction(),"real raw snowball use "+FLAGS[kind]);
                require(stack.getCount()==1&&owner.getStats().getValue(Stats.ITEM_USED.get(Items.SNOWBALL))==stat+1,"one native consumption/stat "+hand+"/"+FLAGS[kind]);
                require(service.pendingCount()==1,"real use enters load callback");service.tick();require(service.trackedCount()==(kind==3?33:kind==5?17:1),"source exact native multiplicity "+FLAGS[kind]);clear();
            }
        }
        void geometry(){
            var magic=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:magic_entities"));var velocity=new Vec3(-.123456,.234567,1.345678);
            for(int kind=0;kind<6;kind++){
                var ball=source(kind,at,velocity);var output=service.convert(ball);require(ball.isRemoved(),"source replaced after successful launch");
                var first=output.getFirst();require(first.getClass()==(kind==0?SmallFireball.class:kind==3?PrimedTnt.class:kind==1||kind==4?LargeFireball.class:DragonFireball.class),"exact vanilla runtime type "+FLAGS[kind]);
                require(service.owner(first)==owner&&game.items().currentEpoch(first),"native owner and current epoch "+FLAGS[kind]);
                var position=at.add(0,0,kind==2||kind==5?0:1.5);require(first.position().distanceToSqr(position)<1e-12,"source local launch offset");
                double read=kind==0||kind==3?10000:100000,write=kind==0||kind==3?.0006:kind==1||kind==4?.00000075:.0000005;
                var expected=new Vec3(Math.floor(velocity.x*read)*write,Math.floor(velocity.y*read)*write,Math.floor(velocity.z*read)*write);
                require(first.getDeltaMovement().distanceToSqr(expected)<1e-20,"source signed floor/scaling "+FLAGS[kind]);
                require(first.getType().builtInRegistryHolder().is(magic)==(kind!=3),"source type-based magic purge membership");
                if(first instanceof LargeFireball){var save=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess());first.saveWithoutId(save);
                    require(save.buildResult().getByteOr("ExplosionPower",(byte)0)==(kind==4?127:2)&&first.isNoGravity()&&first.isInvulnerable()==(kind==4),"native large power/gravity/invulnerability");}
                if(kind==3){require(output.size()==33&&output.stream().allMatch(e->e instanceof PrimedTnt t&&t.getFuse()==100&&t.getOwner()==owner&&e.entityTags().contains("groundbam")),"all33 native TNT fuse/owner/landing");
                    for(var child:output.subList(1,output.size()))require(child.position().distanceToSqr(position.add(0,0,3))<1e-12&&child.isInvulnerable()&&child.entityTags().containsAll(List.of("evoker","fire","electro","strong")),"source32 child position/tags");}
                if(kind==5){require(output.size()==17,"source sixteen dragon children");for(var child:output.subList(1,output.size())){
                    var v=child.getDeltaMovement();require(!child.isNoGravity()&&child instanceof Projectile p&&p.getOwner()==owner,"child native default gravity and owner");
                    require(Math.abs(v.x-Math.floor(expected.x*1000)*.001)<=.03000001&&Math.abs(v.y-Math.floor(expected.y*1000)*.001)<=.00500001&&Math.abs(v.z-Math.floor(expected.z*1000)*.001)<=.03000001,"source integer dragon spread");}}
                clear();
            }
        }
        Cow cow(){var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(at.add(0,0,8));cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);cow.setHealth(200);require(owner.level().addFreshEntity(cow),"native impact witness");return cow;}
        void nativeHits(){
            for(boolean protectedRole:List.of(false,true)){
                var cow=cow();if(protectedRole)cow.entityTags().add("strict_map_object");
                var ball=source(0,at.add(0,.5,4),new Vec3(0,0,.5));var fire=(SmallFireball)service.convert(ball).getFirst();for(int i=0;i<3&&!fire.isRemoved();i++)fire.tick();
                require(protectedRole?cow.getHealth()==200&&!cow.isOnFire():cow.getHealth()<200&&cow.isOnFire(),"native small-fireball damage/ignition respects role "+protectedRole);clear();
            }
            var cow=cow();var ball=source(2,at.add(0,.5,7),new Vec3(0,0,40));var dragon=(DragonFireball)service.convert(ball).getFirst();dragon.tick();
            var cloud=owner.level().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class,cow.getBoundingBox().inflate(5)).stream().filter(c->c.getOwner()==owner).findFirst().orElseThrow();
            require(dragon.isRemoved()&&LbcSpecialFireballs.managed(cloud)&&game.items().currentEpoch(cloud)&&service.snapshot().contains(cloud),"native dragon hit emits managed owned cloud before load callback");
            var board=game.server().getScoreboard();var oldOwner=owner.getTeam();var oldOther=other.getTeam();require(board.getPlayerTeam("special_fireballs")==null,"exclusive fixture team");var team=board.addPlayerTeam("special_fireballs");team.setAllowFriendlyFire(false);
            try{
                board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
                other.removeAllEffects();other.getInventory().clearContent();other.setGameMode(GameType.SURVIVAL);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);other.getAttribute(Attributes.ARMOR).setBaseValue(0);other.setHealth(200);other.setAbsorptionAmount(0);other.setNoGravity(true);
                other.teleportTo(owner.level(),cloud.getX(),cloud.getY(),cloud.getZ(),Set.of(),0,0,true);other.setDeltaMovement(Vec3.ZERO);other.setInvulnerableTime(0);other.damageCooldownTime=0;
                require(owner.isAlliedTo(other)&&service.canAffect(cloud,other)&&game.items().indirectKills().allowProjectileEffect(other,new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INSTANT_DAMAGE,1,1),cloud),"native derived cloud retains WILD allied admission");
                for(int i=0;i<40;i++){cloud.tickCount++;cloud.tick();}
                require(other.getHealth()<200,"actual native derived cloud hurts allied participant");
            }finally{board.removePlayerFromTeam(owner.getScoreboardName());board.removePlayerFromTeam(other.getScoreboardName());if(oldOwner!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwner);if(oldOther!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOther);board.removePlayerTeam(team);}
            clear();
        }
        void releasedLifetime(){
            var ball=source(0,at,new Vec3(0,0,.1));var shot=service.convert(ball).getFirst();service.forget(owner);require(!shot.isRemoved()&&service.snapshot().contains(shot),"released actor survives author forget/logout callback");clear();
        }
        @Override public void close(){try{clear();}finally{try{saved.restore(game);otherSaved.restore(game);attribution.close();}finally{for(var key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);}}}
    }
    private static void landing(GameController game){
        var service=game.items().specialFireballs();
        try(var base=new ChefChecks.Fixture(game)){
            base.init();var at=base.at.add(0,2,0);var level=base.owner.level();
            var witness=base.cow(at.add(2,0,0));witness.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);witness.setHealth(200);
            var ball=new Snowball(level,base.owner,token(3,1));ball.setPos(at.add(0,0,-4.5));ball.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(ball);
            var output=service.convert(ball);require(output.size()==33,"actual full TNT fanout before landing");
            var child=(PrimedTnt)output.get(1);for(var e:output)if(e!=child){service.unloaded(e);e.discard();}
            child.setOnGround(true);service.tick();
            require(!child.isRemoved()&&child.getFuse()==2&&!child.entityTags().contains("groundbam"),"native child retains fuse2 carrier after ordered landing payload");
            require(service.snapshot().stream().filter(e->e instanceof net.minecraft.world.entity.projectile.EvokerFangs).count()==9,"source nine ownerless fangs at actual nearby mob");
            require(service.snapshot().stream().anyMatch(e->e instanceof net.minecraft.world.entity.LightningBolt),"source landing adds real lightning");
            require(service.snapshot().stream().filter(e->e instanceof net.minecraft.world.entity.projectile.EvokerFangs).allMatch(e->((net.minecraft.world.entity.projectile.EvokerFangs)e).getOwner()==null),"causal attribution never adds native fang owner/team immunity");
        }finally{service.stop();}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.initialize();f.rawUses();f.geometry();f.nativeHits();f.releasedLifetime();}landing(game);SkyWars2.LOGGER.info("[check] PASS special-fireballs (six raw aliases, both native hands/stat/debit, source geometry/power/spread, native hits/WILD allied cloud/protected role, native TNT landing/retained fuse and released-actor logout lifetime)");}
}
