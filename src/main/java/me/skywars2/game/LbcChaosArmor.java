package me.skywars2.game;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** The four physical Chaos slots, using the shared armor clock and native item services. */
public final class LbcChaosArmor {
    private static final String FLAG="chaoschestplate";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private final GameController game;
    private final Predicate<ServerPlayer> eligible,admitted;
    private final Map<ServerPlayer,Integer> phases=new IdentityHashMap<>();
    private final Set<Terrain> terrain=Collections.newSetFromMap(new IdentityHashMap<>());
    private record Attempt(ServerPlayer player,boolean allowed){}
    private final Deque<Attempt> attempts=new ArrayDeque<>();
    private long incomingCount,jumpCount,sprintCount,rescueCount,helmetCount;
    private int lastIncoming,lastJump,lastSprint;

    LbcChaosArmor(GameController game,Predicate<ServerPlayer> eligible,Predicate<ServerPlayer> admitted){
        this.game=game;this.eligible=eligible;this.admitted=admitted;
    }
    static boolean piece(ServerPlayer p,EquipmentSlot slot){return !p.getItemBySlot(slot).isEmpty()&&LbcFartus.data(p.getItemBySlot(slot)).getIntOr(FLAG,0)==1;}
    static boolean full(ServerPlayer p){for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!piece(p,slot))return false;return true;}
    private boolean magic(ServerPlayer p){return !game.items().magic().suppressed(p);}
    private boolean active(ServerPlayer p){return admitted.test(p)&&magic(p);}
    public long incomingCount(){return incomingCount;}
    public long jumpCount(){return jumpCount;}
    public long sprintCount(){return sprintCount;}
    public long rescueCount(){return rescueCount;}
    public long helmetCount(){return helmetCount;}
    public int lastIncoming(){return lastIncoming;}
    public int lastJump(){return lastJump;}
    public int lastSprint(){return lastSprint;}

    public void tickPlayer(ServerPlayer p){
        if(!eligible.test(p)){forget(p);return;}
        if(!admitted.test(p))return;
        int phase=(phases.getOrDefault(p,0)+1)%200;phases.put(p,phase);
        if(!magic(p))return;
        if(phase%5==0&&piece(p,EquipmentSlot.CHEST))particles(p,new DustParticleOptions(0xffffff,1),1,3,.3,.3,.3,0);
        if(p.isSprinting()&&p.onGround()&&!game.items().actionsBlocked(p)&&piece(p,EquipmentSlot.FEET)&&p.getRandom().nextInt(100)<5)
            sprintBranch(p,p.getRandom().nextInt(7)+1);
        if(phase==0&&piece(p,EquipmentSlot.HEAD))helmet(p);
        if(phase%20==0&&full(p)&&p.getY()<=-10)rescue(p);
    }
    public boolean withIncomingAttempt(ServerPlayer p,BooleanSupplier hurt){
        attempts.push(new Attempt(p,active(p)&&piece(p,EquipmentSlot.CHEST)));
        try{return hurt.getAsBoolean();}finally{attempts.pop();}
    }
    /** Called only by the native accepted hurt criterion, including blocked and fatal hurt. */
    public void incoming(ServerPlayer p){
        var attempt=attempts.peek();
        if(!(attempt!=null&&attempt.player()==p?attempt.allowed():active(p)&&piece(p,EquipmentSlot.CHEST)))return;
        incomingBranch(p,p.getRandom().nextInt(17)+1);
    }
    public void jump(ServerPlayer p){if(active(p)&&!game.items().actionsBlocked(p)&&piece(p,EquipmentSlot.LEGS))jumpBranch(p,p.getRandom().nextInt(7)+1);}
    /** TMT's existing source items/armor activation shares the real jump selector. */
    public void activate(ServerPlayer p){jump(p);}

    void incomingBranch(ServerPlayer p,int branch){
        incomingCount++;lastIncoming=branch;
        switch(branch){
            case 1->arrows(p);
            case 2->{self(p,MobEffects.JUMP_BOOST,400,2);area(p,10,MobEffects.LEVITATION,20,4,true,false);particles(p,ParticleTypes.SMOKE,0,100,7,7,7,.05);sound(p,"minecraft:entity.firework_rocket.launch",1,1);}
            case 3->flame(p);
            case 4->{self(p,MobEffects.INSTANT_HEALTH,1,0);particles(p,ParticleTypes.HEART,1,10,.5,1,.5,0);sound(p,"minecraft:entity.player.levelup",1,1.5f);}
            case 5->{for(var q:enemies(p,10,true))q.hurtServer(p.level(),p.damageSources().source(DamageTypes.CACTUS,p,p),6);particles(p,ParticleTypes.DAMAGE_INDICATOR,0,40,8,8,8,0);sound(p,"minecraft:entity.player.attack.strong",1,0);}
            case 6->backwards(p);
            case 7->wolf(p);
            case 8->corrosion(p);
            case 9->{self(p,MobEffects.RESISTANCE,20,3,true);var option=new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.BARRIER.defaultBlockState());for(double y:new double[]{.5,1.5})particles(p,option,y,1,0,0,0,0);sound(p,"minecraft:block.anvil.use",1,2);}
            case 10->smoke(p);
            case 11->{area(p,10,MobEffects.WEAKNESS,40,0,false,false);particles(p,ParticleTypes.ASH,1,200,.5,1,.5,.2);sound(p,"minecraft:entity.elder_guardian.curse",1,1);}
            case 12->{area(p,10,MobEffects.NAUSEA,200,0,false,true);area(p,10,MobEffects.POISON,60,0,false,false);area(p,10,MobEffects.WITHER,60,0,false,false);particles(p,new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1,.231f,.047f,.047f)&0xffffff,1),1,1000,7,7,7,1);sound(p,"minecraft:entity.bee.loop",1,0);}
            case 13->transfusion(p);
            case 14->speed(p);
            case 15->{self(p,MobEffects.ABSORPTION,600,0);particles(p,ParticleTypes.DRIPPING_HONEY,1,40,.5,1,.5,0);sound(p,"minecraft:block.beehive.enter",1,0);}
            case 16->{particles(p,ParticleTypes.SNEEZE,1,150,.5,1,.5,1);area(p,10,MobEffects.WITHER,40,10,false,false);area(p,10,MobEffects.POISON,40,10,false,false);area(p,10,MobEffects.HUNGER,40,10,false,true);area(p,10,MobEffects.SLOWNESS,40,10,false,false);sound(p,"minecraft:entity.shulker.ambient",1,0);}
            case 17->explode(p);
            default->throw new IllegalArgumentException("Chaos incoming branch "+branch);
        }
    }
    void jumpBranch(ServerPlayer p,int branch){
        jumpCount++;lastJump=branch;
        switch(branch){
            case 1->flame(p);
            case 2->{replace(p,5,Blocks.SOUL_SOIL,SOLID,true);particles(p,ParticleTypes.SOUL,0,20,3,3,3,0);area(p,6,MobEffects.SLOWNESS,200,3,false,false);}
            case 3->{self(p,MobEffects.LEVITATION,20,30);self(p,MobEffects.SLOW_FALLING,600,0);sound(p,"minecraft:block.beacon.activate",1,1);}
            case 4->{var at=p.position().subtract(p.getLookAngle().scale(.5));p.level().sendParticles(ParticleTypes.FALLING_HONEY,at.x,at.y,at.z,100,.1,.1,.1,.5);self(p,MobEffects.SLOWNESS,20,9,true);sound(p,"minecraft:block.honey_block.break",1,0);area(p,10,MobEffects.POISON,40,10,false,false);area(p,10,MobEffects.WITHER,40,10,false,false);area(p,10,MobEffects.NAUSEA,40,10,false,true);}
            case 5->explode(p);
            case 6->speed(p);
            case 7->{var origin=p.position();teleport(p,origin,10,120);portal(p,origin);self(p,MobEffects.RESISTANCE,40,4);}
            default->throw new IllegalArgumentException("Chaos jump branch "+branch);
        }
    }
    void sprintBranch(ServerPlayer p,int branch){
        sprintCount++;lastSprint=branch;
        switch(branch){
            case 1->{fireworks(p);self(p,MobEffects.SPEED,600,1);}
            case 2->{particles(p,ParticleTypes.SNEEZE,1,10,.5,1,.5,1);sound(p,"minecraft:entity.firework_rocket.twinkle_far",1,1);self(p,MobEffects.JUMP_BOOST,600,2);}
            case 3->{var origin=p.position();teleport(p,origin.add(p.getLookAngle().scale(10)),5,120);portal(p,origin);}
            case 4->nitro(p);
            case 5->{fireworks(p);self(p,MobEffects.SPEED,40,20);}
            case 6->bridge(p);
            case 7->{particles(p,ColorParticleOption.create(ParticleTypes.FLASH,0xffffff00),0,1000,10,10,10,1);area(p,15,MobEffects.BLINDNESS,100,0,false,true);area(p,15,MobEffects.SLOWNESS,100,9,false,false);}
            default->throw new IllegalArgumentException("Chaos sprint branch "+branch);
        }
    }
    private List<LivingEntity> enemies(ServerPlayer p,double radius,boolean mobs){
        return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),q->q!=p&&q.distanceToSqr(p)>=.0001
                &&q.distanceToSqr(p)<=radius*radius&&(!mobs||q.getType().builtInRegistryHolder().is(MOBS))&&game.items().wands().target(p,q));
    }
    private void area(ServerPlayer p,double radius,Holder<MobEffect> type,int ticks,int amplifier,boolean hidden,boolean players){
        if(players){
            for(var q:p.level().players())if(q!=p&&q.distanceToSqr(p)>=.0001&&q.distanceToSqr(p)<=radius*radius&&game.items().wands().target(p,q))
                q.addEffect(ItemEffectLevels.create(type,ticks,amplifier,false,!hidden,!hidden),p);
        }else for(var q:enemies(p,radius,false))q.addEffect(ItemEffectLevels.create(type,ticks,amplifier,false,!hidden,!hidden),p);
    }
    private static void self(ServerPlayer p,Holder<MobEffect> type,int ticks,int amplifier){self(p,type,ticks,amplifier,false);}
    private static void self(ServerPlayer p,Holder<MobEffect> type,int ticks,int amplifier,boolean hidden){p.addEffect(ItemEffectLevels.create(type,ticks,amplifier,false,!hidden,!hidden),p);}
    private void arrows(ServerPlayer p){
        p.level().sendParticles(ParticleTypes.END_ROD,true,false,p.getX(),p.getY()+1,p.getZ(),40,.3,1,.3,.1);
        for(int layer=0;layer<3;layer++)for(int[] d:new int[][]{{0,1},{0,-1},{1,0},{-1,0},{1,1},{-1,-1},{1,-1},{-1,1}}){
            double y=layer==0?1.15:layer==1?1.7:1,vy=layer==0?0:layer==1?.7:-.7;
            var arrow=new Arrow(p.level(),p,new ItemStack(Items.ARROW),null);arrow.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;arrow.setPos(p.position().add(d[0],y,d[1]));
            var motion=new Vec3(d[0],vy,d[1]);arrow.setDeltaMovement(motion);
            ((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$setPierceLevel((byte)10);LbcShooters.orient(arrow);
            arrow.entityTags().add("killedarrow");game.items().incomingArtifacts().trackWild(p,arrow);
        }
        sound(p,"minecraft:entity.skeleton.shoot",1,1);
    }
    private void wolf(ServerPlayer p){
        var wolf=EntityTypes.WOLF.create(p.level(),EntitySpawnReason.COMMAND);
        if(wolf!=null){
            try{wolf.load(TagValueInput.create(ProblemReporter.DISCARDING,wolf.registryAccess(),TagParser.parseCompoundFully("{DeathLootTable:\"minecraft:empty\",Sitting:false,CollarColor:14}")));}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
            wolf.setPos(p.position());wolf.setOwner(p);wolf.setTame(true,false);wolf.setOrderedToSit(false);
            wolf.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(200);wolf.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.5);wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);
            game.items().incomingArtifacts().track(p,wolf);
        }
        particles(p,new DustParticleOptions(0x999999,1),0,10,.5,.5,.5,.02);sound(p,"minecraft:entity.wolf.growl",1,1);
    }
    private void backwards(ServerPlayer p){
        for(var q:enemies(p,10,true)){
            var destination=q.position().subtract(q.getLookAngle().scale(3));
            if(!q.level().hasChunkAt(BlockPos.containing(destination)))continue;
            if(q.teleportTo(p.level(),destination.x,destination.y,destination.z,Set.of(),q.getYRot(),q.getXRot(),true))game.items().indirectKills().teleported(q,p,false);
        }
        particles(p,ParticleTypes.SMOKE,1,50,.5,1,.5,.7);sound(p,"minecraft:entity.firework_rocket.large_blast",2,0);
    }
    private void explode(ServerPlayer p){
        var protection=ExplosionItemProtection.area(game,p.level(),p.position(),8,5);if(game.balanceLegacy())protect(p,8);self(p,MobEffects.SLOW_FALLING,200,0,true);
        for(var offset:new Vec3[]{new Vec3(.5,0,0),new Vec3(-.5,0,0),new Vec3(0,0,.5),new Vec3(0,0,-.5)}){
            var at=p.position().add(offset);var tnt=new PrimedTnt(p.level(),at.x,at.y,at.z,p);tnt.setFuse(5);tnt.setDeltaMovement(Vec3.ZERO);ExplosionItemProtection.stamp(tnt,protection);game.items().incomingArtifacts().trackWild(p,tnt);
        }
        sound(p,"minecraft:entity.tnt.primed",1,1);
    }
    private void protect(ServerPlayer p,int radius){
        for(var q:p.level().players())if(q.distanceToSqr(p)<=radius*radius&&game.items().wands().validRoleTarget(q,false))self(q,MobEffects.RESISTANCE,20,4,true);
    }
    private void nitro(ServerPlayer p){
        var protection=ExplosionItemProtection.area(game,p.level(),p.position(),2,5);if(game.balanceLegacy())protect(p,2);self(p,MobEffects.SLOW_FALLING,400,0,true);
        var at=p.position().subtract(Vec3.directionFromRotation(0,p.getYRot()));var kills=game.items().indirectKills();
        // Native ordinary creeper radius=3, source nine blasts; no helper mobs or Techno radius=1 substitution.
        var cause=kills.wildCause(p);var damage=kills.captureDamage(p.damageSources().explosion(null,p),cause);
        for(int i=0;i<9;i++)kills.trapChanges(p.level(),cause,()->ExplosionItemProtection.with(damage,protection,()->p.level().explode(null,damage,null,at.x,at.y,at.z,3,false,Level.ExplosionInteraction.MOB)));
    }
    private void flame(ServerPlayer p){self(p,MobEffects.FIRE_RESISTANCE,1200,0);replace(p,5,Blocks.FIRE,AIRS,false);sound(p,"minecraft:item.firecharge.use",1,1);particles(p,ParticleTypes.FLAME,1,32,.5,1,.5,.1);}
    private void speed(ServerPlayer p){fireworks(p);self(p,MobEffects.SPEED,200,4);self(p,MobEffects.JUMP_BOOST,200,4);}
    private void fireworks(ServerPlayer p){particles(p,ParticleTypes.FIREWORK,1,100,.5,1,.5,1);sound(p,"minecraft:entity.firework_rocket.twinkle_far",1,1);}
    private void transfusion(ServerPlayer p){
        if(!enemies(p,10,true).isEmpty()){self(p,MobEffects.REGENERATION,120,0,true);self(p,MobEffects.SATURATION,1,40,true);}
        game.items().mana().score(p,"food_count").add(1);
        area(p,10,MobEffects.WITHER,100,0,false,false);area(p,10,MobEffects.HUNGER,100,0,false,true);
        LbcOverload.sound(p,"minecraft:block.portal.travel",.15f,1,.15f);particles(p,ParticleTypes.PORTAL,1,100,.5,1,.5,1);
    }
    private void smoke(ServerPlayer p){
        var option=new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState());
        for(var viewer:p.level().players())if(viewer.distanceToSqr(p)>=.0001)p.level().sendParticles(viewer,option,true,false,p.getX(),p.getY()+1,p.getZ(),400,6,3,6,.15);
        sound(p,"minecraft:block.fire.extinguish",2,1);
    }
    private static void portal(ServerPlayer p,Vec3 origin){p.level().sendParticles(ParticleTypes.PORTAL,origin.x,origin.y+1,origin.z,100,0,1,0,1);LbcOverload.sound(p.level(),origin,"minecraft:entity.enderman.teleport",1,1,0,0);}
    private boolean teleport(ServerPlayer p,Vec3 center,int radius,int netherCeiling){
        int ceiling=p.level().dimension().equals(Level.NETHER)?netherCeiling:p.level().getMaxY()+1;
        var destination=LbcSurfaceTeleport.destination(p.level(),center,radius,ceiling,p.getRandom());
        return destination!=null&&p.teleportTo(p.level(),destination.x,destination.y,destination.z,Set.of(),p.getYRot(),p.getXRot(),true);
    }
    private void helmet(ServerPlayer p){
        helmetCount++;
        for(var q:p.level().players())if(q!=p&&q.distanceToSqr(p)>=.0001&&q.distanceToSqr(p)<=625&&game.items().wands().target(p,q))game.items().chaosTraps().spawnArmor(p,q.position());
    }
    private void rescue(ServerPlayer p){
        rescueCount++;self(p,MobEffects.RESISTANCE,20,4);
        boolean moved=teleport(p,game.config().noBorders?p.position():Vec3.ZERO,190,125);
        if(game.balanceLegacy())game.items().chaosTraps().spawnArmor(p,new Vec3(p.getX(),100,p.getZ()));
        else if(moved)game.items().chaosTraps().spawnArmor(p,p.position());
    }
    private void replace(ServerPlayer p,int radius,Block block,TagKey<Block> replace,boolean strict){
        var level=p.level();var center=p.blockPosition();var from=center.offset(-radius,-radius,-radius);var to=center.offset(radius,radius,radius);
        queue(p,from,to,block==Blocks.FIRE,()->{
            long volume=(long)(radius*2+1)*(radius*2+1)*(radius*2+1);
            if(level.isOutsideBuildHeight(from)||level.isOutsideBuildHeight(to)||volume>level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS))return;
            for(var at:BlockPos.betweenClosed(from,to))if(level.getBlockState(at).is(replace))LbcDropBlocks.setBlock(level,at,block.defaultBlockState(),strict);
        });
    }
    private void bridge(ServerPlayer p){
        var level=p.level();var origin=p.position().add(0,-1.5,0);var forward=p.getLookAngle();var end=origin.add(forward.scale(40));
        var from=BlockPos.containing(Math.min(origin.x,end.x)-.5,Math.min(origin.y,end.y),Math.min(origin.z,end.z)-.5);
        var to=BlockPos.containing(Math.max(origin.x,end.x)+.5,Math.max(origin.y,end.y),Math.max(origin.z,end.z)+.5);
        queue(p,from,to,false,()->{
            for(int i=0;i<=40;i++){
                var at=origin.add(forward.scale(i));var low=BlockPos.containing(at.add(-.5,0,-.5));var high=BlockPos.containing(at.add(.5,0,.5));
                if(level.isOutsideBuildHeight(low)||level.isOutsideBuildHeight(high))continue;
                long volume=(long)(high.getX()-low.getX()+1)*(high.getZ()-low.getZ()+1);
                if(volume>level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS))continue;
                for(var cell:BlockPos.betweenClosed(low,high))if(level.getBlockState(cell).is(AIRS))LbcDropBlocks.setBlock(level,cell,Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE).defaultBlockState(),true);
            }
        });
    }
    private void corrosion(ServerPlayer p){
        var level=p.level();var origin=p.blockPosition().offset(-24,-24,-24);
        var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:air")).orElseThrow();
        var size=template.getSize();
        queue(p,origin,origin.offset(size.getX(),size.getY(),size.getZ()),true,()->{
            template.placeInWorld(level,origin,origin,new StructurePlaceSettings().setIgnoreEntities(true).addProcessor(new BlockRotProcessor(.05f))
                    .setRandom(net.minecraft.world.level.block.entity.StructureBlockEntity.createRandom(0)),net.minecraft.world.level.block.entity.StructureBlockEntity.createRandom(0),2);
        });
        particles(p,ParticleTypes.LARGE_SMOKE,0,50,1,1,1,1);sound(p,"minecraft:block.portal.ambient",1,1);
    }
    private static <T extends ParticleOptions> void particles(ServerPlayer p,T type,double y,int count,double x,double dy,double z,double speed){p.level().sendParticles(type,p.getX(),p.getY()+y,p.getZ(),count,x,dy,z,speed);}
    private static void sound(ServerPlayer p,String id,float volume,float pitch){LbcOverload.sound(p,id,volume,pitch,0);}
    private void queue(ServerPlayer p,BlockPos from,BlockPos to,boolean wild,Runnable writes){var task=new Terrain(p,from,to,wild,writes);terrain.add(task);game.items().stonekivi().queuedWork(task);}
    private final class Terrain implements LbcStonekivi.Work {
        final ServerLevel level;final UUID author;final String epoch=game.items().epoch();final BlockPos from,to;final Runnable writes;final LbcIndirectKills.Owner cause;boolean cancelled;
        Terrain(ServerPlayer p,BlockPos from,BlockPos to,boolean wild,Runnable writes){level=p.level();author=p.getUUID();this.from=from;this.to=to;this.writes=writes;cause=wild?game.items().indirectKills().wildCause(p):game.items().indirectKills().coldCause(p,false);}
        boolean live(){return !cancelled&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&level==game.maps().level()&&game.items().currentEpoch(epoch);}
        @Override public boolean ready(){
            if(!live())return true;if(game.items().timeStop().stopped(level))return false;
            for(int x=from.getX()>>4;x<=to.getX()>>4;x++)for(int z=from.getZ()>>4;z<=to.getZ()>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
            return true;
        }
        @Override public boolean step(){if(live())game.items().indirectKills().trapChanges(level,cause,writes);cancel();return true;}
        @Override public void cancel(){cancelled=true;terrain.remove(this);}
    }
    public void inventoryChanged(ServerPlayer p){if(!eligible.test(p))forget(p);}
    public void pausedMaintenance(){for(var p:game.server().getPlayerList().getPlayers())inventoryChanged(p);}
    public void loaded(Entity e){}
    public void unloaded(Entity e){if(e instanceof ServerPlayer p)forget(p);}
    public void forget(ServerPlayer p){phases.remove(p);for(var work:List.copyOf(terrain))if(work.author.equals(p.getUUID()))work.cancel();}
    public void stop(){for(var work:List.copyOf(terrain))work.cancel();phases.clear();}
}
