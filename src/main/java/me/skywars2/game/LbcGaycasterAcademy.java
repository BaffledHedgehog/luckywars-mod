package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Source six-stage trial owns only its local helpers; colour casts use the shared resident ray queue. */
final class LbcGaycasterAcademy {
    private static final TagKey<Block> AIRS=tag("airs"),SOLID=tag("solid");
    private static final String[] MODES={"mode_red","mode_orange","mode_yellow","mode_green","mode_light_blue","mode_blue","mode_purple","mode_queued"};
    private static final int[] COLORS={0xff0000,0xff8000,0xffff00,0x00ff00,0x00ffff,0x0000ff,0xff00ff};
    private final GameController game;
    private final Map<ServerPlayer,Trial> trials=new IdentityHashMap<>();
    LbcGaycasterAcademy(GameController game){this.game=game;}
    private static TagKey<Block> tag(String id){return TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:"+id));}
    /** Trial hazards deliberately have no team filter in the source. Keep native damage types and caster credit. */
    static final class TrialDamage extends DamageSource {
        TrialDamage(ServerPlayer owner,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type){
            super(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),owner);
        }
    }
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack){
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Mth.clamp(data.getIntOr("mode",1),1,8);
        if(owner.isShiftKeyDown()){
            mode=mode%8+1;data.putInt("mode",mode);data.putInt("color",mode);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();
            owner.sendSystemMessage(Component.translatable(MODES[mode-1]));return true;
        }
        if(!game.items().mana().trySpendWand(owner,cost()))return false;
        int color=mode;
        if(mode==8){int previous=data.getIntOr("color",8);color=previous>=7||previous<1?1:previous+1;data.putInt("color",color);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();}
        cast(owner,color);game.items().wands().wandCasted(owner);return true;
    }
    private int cost(){return game.items().mana().score(ScoreHolder.forNameOnly("gaycaster_mana_use"),"lbc.math").get();}
    private void cast(ServerPlayer owner,int color){
        Vec3 from=owner.getEyePosition(),direction=owner.getLookAngle();double[] next={0};
        LbcOverload.sound(owner,from,"minecraft:lbcsounds.laser_mg_shot",1,0,0);
        LbcOverload.sound(owner,from,"minecraft:item.trident.return",1,0,0);
        game.items().exasperato().searchSpell(owner,from,direction,125,false,(p,s)->!s.is(AIRS),e->LbcGaycaster.target(game,owner,e),
                (a,b)->{double end=from.distanceTo(b);while(next[0]<=end&&next[0]<125){
                    particles(owner,new DustParticleOptions(COLORS[color-1],1.5f),from.add(direction.scale(next[0])),1,0,0,0,0);next[0]+=.5;}
                }).thenAccept(hit->{if(hit!=null)impact(owner,hit.getLocation(),direction,color,
                        hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity l?l:null);});
    }
    void impact(ServerPlayer owner,Vec3 point,Vec3 direction,int color,LivingEntity direct){
        if(game.items().magic().suppressedAt(owner.level(),point))return;
        var nearby=direct==null?owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(1.5),
                e->e.position().distanceToSqr(point)<=2.25&&LbcGaycaster.target(game,owner,e)):
                LbcGaycaster.target(game,owner,direct)?List.of(direct):List.<LivingEntity>of();
        switch(color){
            case 1->{boolean accepted=false;
                for(var e:nearby)accepted=e.hurtServer(owner.level(),LbcGaycaster.damage(owner,DamageTypes.MAGIC),6)||accepted;
                if(accepted){owner.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH,1,1,false,false,false),owner);
                    particles(owner,ParticleTypes.HEART,owner.position(),5,.3,.5,.3,0);}
                particles(owner,ParticleTypes.DAMAGE_INDICATOR,point,direct==null?10:5,.3,.5,.3,0);
            }
            case 2->fillAir(owner,point,Blocks.LAVA);
            case 3->{for(var e:nearby)game.items().psycho().yellow(e,owner,direct==null?100:200);}
            case 4->game.items().clouds().greatAcid(owner,point,new Vec3(0,1,0));
            case 5->{ // (user) Preserve the source pairing: cyan electricity, blue water.
                Vec3 from=direct==null?point.subtract(direction.scale(.5)):owner.getEyePosition().subtract(direction);
                Vec3 aim=direct==null?owner.getEyePosition().subtract(from):direction;
                game.items().wands().shortElectro(owner,from,from.add(aim),false);
                particles(owner,ParticleTypes.ELECTRIC_SPARK,point,20,.5,.5,.5,0);
            }
            case 6->{fillAir(owner,point,Blocks.WATER);particles(owner,ParticleTypes.SPLASH,point,30,.4,.4,.4,0);
                LbcOverload.sound(owner,point,"minecraft:lbcsounds.yametekudasai",direct==null?1:3,1,0);}
            case 7->{for(var e:nearby)if(game.items().mana().hudValue(e,"void_resistance")<=0)
                    e.hurtServer(owner.level(),LbcGaycaster.damage(owner,DamageTypes.FELL_OUT_OF_WORLD),5);
                if(direct==null)game.items().indirectKills().trapChanges(owner.level(),game.items().indirectKills().coldCause(owner,true),()->{
                    for(var p:BlockPos.betweenClosed(BlockPos.containing(point.add(-.5,-.5,-.5)),BlockPos.containing(point.add(.5,.5,.5))))
                        if(owner.level().hasChunkAt(p))owner.level().setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);
                });particles(owner,ParticleTypes.SQUID_INK,point,direct==null?30:20,.6,.6,.6,0);}
            default->throw new IllegalArgumentException("Gaycaster colour "+color);
        }
    }
    private void fillAir(ServerPlayer owner,Vec3 point,Block block){
        // These are ray impacts: friendly mobs remain eligible for the resulting terrain hazards too.
        game.items().indirectKills().trapChanges(owner.level(),game.items().indirectKills().coldCause(owner,true),()->{
            for(var p:BlockPos.betweenClosed(BlockPos.containing(point.add(-2,-2,-2)),BlockPos.containing(point.add(2,2,2))))
                if(owner.level().hasChunkAt(p)&&owner.level().getBlockState(p).is(AIRS))owner.level().setBlock(p,block.defaultBlockState(),3);
        });
        if(block==Blocks.LAVA)particles(owner,ParticleTypes.LAVA,point,10,0,0,0,.3);
    }
    boolean meditate(ServerPlayer owner,InteractionHand hand,ItemStack stack){
        // Source meditation has an admission check, but neither debit nor cast_wand hook.
        if(game.items().mana().get(owner)<cost())return false;
        forget(owner);var trial=new Trial(owner,hand,stack);trials.put(owner,trial);title(owner,"meditation_gaycaster");
        counter(trial);return true;
    }
    private static void title(ServerPlayer owner,String key){
        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(" ")));
        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.translatable(key)));
    }
    private static final class Hazard {
        final int kind;Vec3 at;final Vec3 direction;int age;Entity display;UUID visual;
        Hazard(int kind,Vec3 at,Vec3 direction){this.kind=kind;this.at=at;this.direction=direction;}
        void close(){if(display!=null)display.discard();}
    }
    private final class Trial {
        final ServerPlayer owner;final InteractionHand hand;final ItemStack stack;final ServerLevel level;final String epoch;
        final LbcIndirectKills.Owner terrainCause;
        final List<Hazard> hazards=new ArrayList<>();int remaining=115,ticks;float rotation;
        Trial(ServerPlayer p,InteractionHand h,ItemStack s){
            owner=p;hand=h;stack=s;level=p.level();epoch=game.items().epoch();
            var cause=game.items().indirectKills().coldCause(p,true);
            terrainCause=new LbcIndirectKills.Owner(cause.player(),cause.order(),true,true);
        }
    }
    private void counter(Trial trial){game.items().mana().score(trial.owner,"gaycaster_upgrade").set(trial.remaining);}
    int remaining(ServerPlayer owner){var t=trials.get(owner);return t==null?0:t.remaining*5-t.ticks%5;}
    int helperCount(ServerPlayer owner){var t=trials.get(owner);return t==null?0:t.hazards.size();}
    void tick(){
        for(var t:List.copyOf(trials.values())){
            if(!t.owner.isAlive()||t.owner.isRemoved()||!game.items().currentEpoch(t.epoch)||!game.playing()||game.isObserver(t.owner)
                    ||t.owner.level()!=t.level){finish(t,null);continue;}
            if(game.items().timeStop().stopped(t.level))continue;
            if(++t.ticks%5==0){
                if(t.owner.getItemInHand(t.hand)!=t.stack||!LbcAcademy.trial(t.stack)){finish(t,"gaycaster_meditate_no_wand");continue;}
                if(!game.items().magic().suppressed(t.owner)){
                    phase(t);
                    if(t.remaining==1){game.items().academy().ready(t.owner,t.hand,t.stack);finish(t,null);continue;}
                    t.remaining--;counter(t);
                }
            }
            // Cover the actual helper positions and prism segments, even after the caster moves away.
            AABB bounds=new AABB(t.owner.position(),t.owner.position());
            for(var h:t.hazards){bounds=bounds.minmax(new AABB(h.at,h.at));
                if(h.kind==1)bounds=bounds.minmax(new AABB(h.at,h.at.add(h.direction.scale(27))));}
            var living=t.level.getEntitiesOfClass(LivingEntity.class,bounds.inflate(16),
                    e->game.items().wands().validTarget(e));
            for(var h:List.copyOf(t.hazards)){
                if(trials.get(t.owner)!=t)break;
                if(!t.level.hasChunkAt(BlockPos.containing(h.at))||game.items().magic().suppressedAt(t.level,h.at)||tickHazard(t,h,living)){
                    t.hazards.remove(h);h.close();
                }
            }
        }
    }
    private void phase(Trial t){
        int n=t.remaining;Vec3 at=t.owner.position();var random=t.owner.getRandom();
        if(n==110||n==95||n==80||n==65||n==50||n==35)title(t.owner,"gaycaster_meditate_"+(n==110?1:n==95?2:n==80?3:n==65?4:n==50?5:6));
        if(n>=100&&n<=110){
            t.rotation=random.nextInt(7)==0?random.nextInt(361)-180:t.rotation+93;
            Vec3 direction=Vec3.directionFromRotation(0,t.rotation);var h=new Hazard(1,at.add(0,1,0).subtract(direction.scale(10)),direction);
            t.hazards.add(h);display(t,h,false);
        }else if(n>=85&&n<=95){
            int x=Mth.floor(at.x)+random.nextInt(27)-13,z=Mth.floor(at.z)+random.nextInt(27)-13;
            int y=Mth.floor(at.y)+3;var p=new BlockPos.MutableBlockPos(x,y,z);
            if(t.level.hasChunkAt(p)){while(p.getY()>t.level.getMinY()&&t.level.getBlockState(p).is(AIRS))p.move(0,-1,0);
                t.hazards.add(new Hazard(2,new Vec3(x+.5,p.getY()+2.5,z+.5),Vec3.ZERO));}
        }else if(n>=70&&n<=80)t.hazards.add(new Hazard(3,at.add(0,15,0),Vec3.ZERO));
        else if(n>=55&&n<=65&&random.nextBoolean()){
            Vec3 direction=Vec3.directionFromRotation(0,random.nextInt(360));t.hazards.add(new Hazard(4,at.subtract(direction.scale(20)),direction));
        }else if(n==50)t.hazards.add(new Hazard(5,at.add(0,-5,0),Vec3.ZERO));
        else if(n>=8&&n<=35)for(int i=0;i<16;i++){
            var h=new Hazard(6,new Vec3(Mth.floor(at.x)+random.nextInt(41)-20,at.y-30,Mth.floor(at.z)+random.nextInt(41)-20),Vec3.ZERO);
            t.hazards.add(h);display(t,h,true);
        }
    }
    private Entity display(Trial t,Hazard h,boolean block){
        String preset=block?"{teleport_duration:1,interpolation_duration:1,block_state:{Name:'minecraft:black_concrete'},glow_color_override:3866675,Glowing:1b,transformation:{translation:[0f,0f,0f],scale:[3f,3f,3f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}":
                "{teleport_duration:1,interpolation_duration:1,shadow_radius:0f,shadow_strength:0f,item_display:'fixed',transformation:[0f,0f,1f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,0f,1f],item:{id:'minecraft:carrot_on_a_stick',count:1,components:{'minecraft:item_model':'lbc:prism'}}}";
        Entity d=block?new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,t.level):new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,t.level);
        try{d.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,t.owner.registryAccess(),TagParser.parseCompoundFully(preset)));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
        d.setPos(h.at);d.setYRot((float)Math.toDegrees(Math.atan2(-h.direction.x,h.direction.z)));
        d.entityTags().add("skywars2.gaycaster.trial");game.items().markCurrentItemEpoch(d);
        h.display=d;h.visual=d.getUUID();t.level.addFreshEntity(d);return d;
    }
    private boolean tickHazard(Trial t,Hazard h,List<LivingEntity> living){
        h.age++;
        switch(h.kind){
            case 1->{
                if(h.age==1||h.age==11||h.age==21){for(int i=0;i<=90;i++)trialParticles(t.owner,new DustParticleOptions(0xff0000,1),h.at.add(h.direction.scale(i*.3)),1,0,0,0,0);
                    LbcOverload.sound(t.owner,h.at,"minecraft:block.beacon.activate",1,2,0);}
                if(h.age<31)return false;
                var hit=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());
                for(int i=0;i<=90;i++){Vec3 point=h.at.add(h.direction.scale(i*.3));paint(t,point,point,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.RED));
                    trialParticles(t.owner,ParticleTypes.FIREWORK,point,1,.01,.01,.01,.01);
                    for(var e:living)if(!hit.contains(e)&&e.getBoundingBox().intersects(new AABB(point,point.add(1,1,1)))){
                        hit.add(e);hurt(t,e,7,DamageTypes.ARROW);}
                }LbcOverload.sound(t.owner,h.at,"minecraft:lbcsounds.laser_attack",1.5f,1,0);
                trialParticles(t.owner,net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xffff0000),h.at,1,0,0,0,0);return true;
            }
            case 2->{
                if(h.age==1){for(int i=0;i<364;i++){double angle=Math.toRadians(i*4);trialParticles(t.owner,new net.minecraft.core.particles.DustColorTransitionOptions(0xdd9000,0xff0000,3),h.at.add(5*Math.cos(angle),0,5*Math.sin(angle)),1,0,0,0,0);}}
                if(h.age!=49)return h.age>=50;
                paint(t,h.at.add(-5,-5,-5),h.at.add(5,5,5),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.ORANGE));
                paint(t,h.at.add(-4,-4,-4),h.at.add(4,4,4),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.YELLOW));
                trialParticles(t.owner,ParticleTypes.EXPLOSION,h.at,15,3,3,3,0);trialParticles(t.owner,ParticleTypes.FLAME,h.at,50,3,3,3,1);
                LbcOverload.sound(t.owner,h.at,"minecraft:entity.generic.explode",1,1,0);
                for(var e:living)if(e.position().distanceToSqr(h.at)<=25)hurt(t,e,10,DamageTypes.EXPLOSION);return true;
            }
            case 3->{
                trialParticles(t.owner,ParticleTypes.SNEEZE,h.at,1,0,0,0,.1);
                LbcOverload.sound(t.owner,h.at,"minecraft:block.fire.extinguish",.5f,1,0);
                for(var e:living)if(e.position().distanceToSqr(h.at)<=4){e.addEffect(new MobEffectInstance(MobEffects.POISON,100,1),t.owner);failed(t,e);}
                if(t.level.getBlockState(BlockPos.containing(h.at.add(0,-1,0))).is(AIRS))h.at=h.at.add(0,-.5,0);
                paint(t,h.at.add(-1,-2,-1),h.at.add(1,-1,1),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIME));
            }
            case 4->{
                Vec3 left=new Vec3(h.direction.z,0,-h.direction.x);
                for(int offset=-14;offset<=14;offset+=7){Vec3 point=h.at.add(left.scale(offset));
                    trialParticles(t.owner,ParticleTypes.ELECTRIC_SPARK,point.add(0,1,0),5,0,1,0,0);
                    trialParticles(t.owner,new net.minecraft.core.particles.DustColorTransitionOptions(0x00ccff,0x269eb6,1.5f),point.add(0,1,0),5,0,1,0,0);
                    trialParticles(t.owner,new DustParticleOptions(0xff0000,1),point.add(h.direction.scale(7)).add(0,1,0),5,0,1,0,0);
                    paint(t,point.add(0,-1,0),point.add(0,2,0),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE));
                    for(var e:living)if(e.getBoundingBox().intersects(new AABB(point.add(-.99,-.49,-.99),point.add(1,1.5,1))))hurt(t,e,8,DamageTypes.ARROW);
                }LbcOverload.sound(t.owner,h.at,"minecraft:entity.firework_rocket.twinkle",.5f,1,0);h.at=h.at.add(h.direction.scale(.5));return h.age>=80;
            }
            case 5->{
                trialParticles(t.owner,new DustParticleOptions(0x0000ff,3),h.at,15,10,0,10,0);
                trialParticles(t.owner,new net.minecraft.core.particles.DustColorTransitionOptions(0,0xff0000,.5f),h.at.add(0,3,0),8,10,0,10,0);
                LbcOverload.sound(t.owner,h.at,"minecraft:block.water.ambient",1,1,0);
                for(var e:living)if(e.position().distanceToSqr(h.at)<=144&&Mth.floor(e.getY())==Mth.floor(h.at.y))hurt(t,e,4,DamageTypes.DROWN);
                paint(t,h.at.add(-10,0,-10),h.at.add(10,0,10),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));
                h.at=h.at.add(0,.065,0);return h.age>=120;
            }
            case 6->{
                paint(t,h.at,h.at,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.PURPLE));
                for(var e:living)if(e.getBoundingBox().intersects(new AABB(h.at,h.at.add(1,1,1))))hurt(t,e,4,DamageTypes.FELL_OUT_OF_WORLD);
                h.at=h.at.add(0,1,0);if(h.display!=null&&!h.display.isRemoved())h.display.setPos(h.at);
            }
        }return false;
    }
    private void paint(Trial t,Vec3 from,Vec3 to,Block block){
        // Continuing trial paints keep the original influence order; a later real player hit still wins.
        game.items().indirectKills().trapChanges(t.level,t.terrainCause,()->{
            for(var p:BlockPos.betweenClosed(BlockPos.containing(from),BlockPos.containing(to)))if(t.level.hasChunkAt(p)&&t.level.getBlockState(p).is(SOLID))
                t.level.setBlock(p,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);
        });
    }
    private void hurt(Trial t,LivingEntity target,float amount,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type){
        target.hurtServer(t.level,new TrialDamage(t.owner,type),amount);failed(t,target);
    }
    private void failed(Trial source,LivingEntity hit){
        if(hit instanceof ServerPlayer player){var own=trials.get(player);if(own!=null)finish(own,"gaycaster_meditate_fail");}
    }
    private void finish(Trial t,String key){
        if(trials.get(t.owner)!=t)return;trials.remove(t.owner);t.hazards.forEach(Hazard::close);t.hazards.clear();t.remaining=0;counter(t);
        if(key!=null)t.owner.sendSystemMessage(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.RED));
    }
    void forget(ServerPlayer player){var t=trials.get(player);if(t!=null)finish(t,null);}
    void loaded(Entity entity){
        if(!entity.entityTags().contains("skywars2.gaycaster.trial"))return;
        // A display can return as a different Java object after a chunk reload. Only its live trial may reclaim it.
        for(var t:trials.values())if(game.items().currentEpoch(t.epoch)&&entity.level()==t.level)
            for(var h:t.hazards)if(entity.getUUID().equals(h.visual)){h.display=entity;return;}
        entity.discard();
    }
    void unloaded(Entity entity){
        if(!entity.entityTags().contains("skywars2.gaycaster.trial"))return;
        for(var t:trials.values())for(var h:t.hazards)if(h.display==entity)h.display=null;
    }
    void stop(){for(var t:List.copyOf(trials.values()))finish(t,null);}
    /** Source trial particles are local; only its flash explicitly uses forced-distance delivery. */
    private static void trialParticles(ServerPlayer owner,net.minecraft.core.particles.ParticleOptions particle,Vec3 point,int count,double x,double y,double z,double speed){
        if(particle.getType()==ParticleTypes.FLASH)particles(owner,particle,point,count,x,y,z,speed);
        else owner.level().sendParticles(particle,point.x,point.y,point.z,count,x,y,z,speed);
    }
    private static void particles(ServerPlayer owner,net.minecraft.core.particles.ParticleOptions particle,Vec3 point,int count,double x,double y,double z,double speed){
        LbcGaycaster.particles(owner,particle,point,count,x,y,z,speed);
    }
}
