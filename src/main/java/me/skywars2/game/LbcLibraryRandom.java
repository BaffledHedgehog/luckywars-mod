package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** The source random-library extras use finite native states and the existing spell factories. */
final class LbcLibraryRandom {
    private final GameController game;
    private final List<Emitter> emitters=new ArrayList<>();
    private final List<Flying> flying=new ArrayList<>();
    private final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    private record Expiry(ServerPlayer owner,int[] remaining) { }
    private final Map<Entity,Expiry> expiries=new IdentityHashMap<>();
    private ServerPlayer launching;private int sourceLifetime;
    private static final int[] BASIC={5,25,10,15,1,20,17,0,112,116,125,120,0,123,0,0,0,0,0,0,0,0,134,0,136,141,0,148,154,171,167,184,165,181,160,176,173,163,157,188,195,229,212,225,216,204,199,220,0,233,0,0,159};
    private static final class Emitter {
        final ServerPlayer owner;final ServerLevel level;final String epoch;final Vec3 at;final int type,lifetime;int age;
        Emitter(ServerPlayer owner,Vec3 at,int type,int lifetime){this.owner=owner;level=owner.level();epoch=me.skywars2.SkyWars2.game().items().epoch();this.at=at;this.type=type;this.lifetime=lifetime;}
    }
    private record Flying(ServerPlayer owner,ServerLevel level,String epoch,Display.ItemDisplay display,int kind,int[] age) { }
    LbcLibraryRandom(GameController game){this.game=game;}
    void captured(Entity entity){if(launching!=null&&sourceLifetime>0)expiries.put(entity,new Expiry(launching,new int[]{sourceLifetime}));}
    static void castRandom(GameController g,ServerPlayer p,InteractionHand hand,int roll){
        if(roll<1||roll>53)throw new IllegalArgumentException("Random spell "+roll);
        try{
        if(BASIC[roll-1]>0){LbcLibrarySpells.cast(g,p,hand,BASIC[roll-1]);return;}
        switch(roll){
            case 8 -> g.items().consuming().castScroll(p,true);
            case 13,16,17,18,19,20,21,22 -> g.items().libraryRandom().ray(p,roll);
            case 15 -> g.items().consuming().castScroll(p,false);
            case 24 -> g.items().hedgehog().cast(p,LbcHedgehog.Kind.ORDINARY);
            case 27 -> g.items().clouds().volleyAcid(p,p.getEyePosition(),p.getLookAngle(),6,false,false);
            case 49 -> g.items().psycho().psychoblind(p,0);
            case 51 -> LbcGaycaster.cast(g,p);
            case 52 -> {g.items().iceWand().spawn(p,hand,ItemStack.EMPTY);LbcOverload.sound(p,p.position(),"minecraft:lbcsounds.ice_wand",1,.7f,0);}
            default -> throw new IllegalArgumentException("Missing random spell "+roll);
        }
        }finally{LbcOverload.sound(p,p.position(),"minecraft:item.book.page_turn",10,1,0);}
    }
    private boolean active(ServerPlayer p,String epoch,ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&p.level()==level&&game.items().currentEpoch(epoch)&&game.canUseGameplayItems(p);}
    private void ray(ServerPlayer p,int type){
        var epoch=game.items().epoch();var level=p.level();var direction=p.getLookAngle();
        net.minecraft.core.particles.ParticleOptions particle=switch(type){
            case 13->ParticleTypes.END_ROD;case 16->ParticleTypes.EXPLOSION;
            case 17->new net.minecraft.core.particles.DustParticleOptions(0xffffff,2);
            case 18->new net.minecraft.core.particles.DustParticleOptions(0x818181,3);
            case 19->ParticleTypes.HEART;case 20->ParticleTypes.FLAME;case 21->ParticleTypes.LAVA;
            case 22->new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState());
            default->throw new IllegalArgumentException("Unknown random ray "+type);
        };
        game.items().exasperato().searchSpell(p,p.getEyePosition(),direction,200,false,(pos,state)->!state.is(TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("airs"))),
                e->e instanceof LivingEntity&&e!=p&&game.items().wands().validTarget(e),(from,to)->{
                    double distance=from.distanceTo(to);for(double d=0;d<=distance;d++){
                        var at=from.add(direction.scale(d));level.sendParticles(particle,true,false,at.x,at.y,at.z,1,0,0,0,.01);
                    }
                }).thenAccept(hit->{if(hit!=null&&active(p,epoch,level))impact(p,hit.getLocation().subtract(direction.scale(hit instanceof net.minecraft.world.phys.BlockHitResult?1:0)),type);});
    }
    void impact(ServerPlayer p,Vec3 at,int type){
        if(type==16){
            for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                if(x==0&&y==0&&z==0)continue;
                var tnt=new PrimedTnt(p.level(),at.x,at.y+.5,at.z,p);tnt.setFuse(56);tnt.setDeltaMovement(x*.4,y*.4,z*.4);insert(p,tnt);
            }
        }else if(type==13||type==18||type==19){
            if(type==13)game.items().bolts().libraryChainArea(p,at,16);
            for(var target:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(type==13?16:15),
                    e->game.items().wands().validTarget(e)&&e.position().distanceToSqr(at)<=(type==13?256:225))){
                if(type==13){continue;}
                else target.addEffect(ItemEffectLevels.create(type==18?MobEffects.WITHER:MobEffects.INSTANT_HEALTH,type==18?1200:200,type==18?0:4,false,false),p);
            }
            p.level().sendParticles(type==13?ParticleTypes.ELECTRIC_SPARK:type==18?ParticleTypes.SMOKE:ParticleTypes.HEART,at.x,at.y,at.z,type==13?200:type==18?100:300,10,10,10,0);
            if(type!=13)LbcOverload.sound(p,at,type==18?"minecraft:entity.wither.ambient":"minecraft:entity.player.levelup",1,type==18?1:1.5f,0);
        }else if(type==20)lava(p,BlockPos.containing(at));
        else if(type==17||type==21||type==22){
            if(type==17){p.level().sendParticles(ParticleTypes.POOF,at.x,at.y,at.z,100,1,1,1,1);LbcOverload.sound(p,at,"minecraft:particle.soul_escape",1,0,0);}
            if(type==21)p.level().sendParticles(ParticleTypes.FLAME,at.x,at.y+10,at.z,200,2,10,2,.1);
            emitters.add(new Emitter(p,type==17?at.add(0,1,0):new Vec3(at.x,310,at.z),type,type==17?1600:type==21?500:60));
        }else throw new IllegalArgumentException("Random terminal "+type);
    }
    private void insert(ServerPlayer p,Entity entity){
        game.items().summons().track(p,entity);actors.add(entity);game.items().timeStop().prepareFrozen(entity);
        if(!p.level().addFreshEntity(entity)){actors.remove(entity);entity.discard();}
    }
    private void lava(ServerPlayer p,BlockPos center){
        String[][] rules={{"to_nylium","crimson_nylium"},{"logs","crimson_hyphae"},{"leaves","nether_wart_block"},{"to_shroomlight","shroomlight"},{"to_soulsand","soul_sand"},{"to_magma","magma_block"},{"to_netherrack","netherrack"},{"fluids","obsidian"},{"airs_to_fire","fire"},{"to_lava","lava"}};
        var scan=new BlockAreaWork(p.level(),s->true,(pos,state)->{
            if(pos.getX()==center.getX()&&pos.getZ()==center.getZ()&&pos.getY()>=1&&pos.getY()<=126)return Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,1);
            var next=state;for(var rule:rules)if(next.is(TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace(rule[0]))))next=BuiltInBlock(rule[1]);
            // Source sourcelava is a31-cube containing only source lava, integrity0.0001.
            if(Math.abs(pos.getX()-center.getX())<=15&&Math.abs(pos.getY()-center.getY())<=15&&Math.abs(pos.getZ()-center.getZ())<=15&&p.getRandom().nextFloat()<.0001f)return Blocks.LAVA.defaultBlockState();
            return next;
        },2|16|32,true);
        scan.include(new BlockAreaWork.Bounds(center.getX()-15,center.getY()-15,center.getZ()-15,center.getX()+15,center.getY()+15,center.getZ()+15));
        scan.include(new BlockAreaWork.Bounds(center.getX(),1,center.getZ(),center.getX(),126,center.getZ()));
        var epoch=game.items().epoch();var level=p.level();
        game.items().stonekivi().queuedWork(new LbcStonekivi.Work(){public boolean step(){if(!active(p,epoch,level)){cancel();return true;}scan.step(game,p,1024,System.nanoTime()+1_000_000);return !scan.hasWork();}public void cancel(){scan.clear();}});
        for(var offset:List.of(new Vec3(0,30,15),new Vec3(0,30,-15),new Vec3(15,30,0),new Vec3(-15,30,0))){
            var ghast=EntityTypes.GHAST.create(p.level(),EntitySpawnReason.TRIGGERED);if(ghast!=null){ghast.setPos(Vec3.atLowerCornerOf(center).add(offset));insert(p,ghast);}
        }
    }
    private static net.minecraft.world.level.block.state.BlockState BuiltInBlock(String name){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Identifier.withDefaultNamespace(name)).orElseThrow().value().defaultBlockState();}
    static void flyingWand(GameController g,ServerPlayer p){g.items().libraryRandom().fly(p,p.getRandom().nextInt(9));}
    void fly(ServerPlayer p,int kind){
        String model=new String[]{"ruby","terra","electro","air","acid","teleport","stone","fire","blind"}[kind];
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,p.level());
        try{display.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),TagParser.parseCompoundFully("{teleport_duration:1,interpolation_duration:1,item_display:'fixed',transformation:[0f,0f,2.5f,0f,1.7678f,-1.7678f,0f,0f,1.7678f,1.7678f,0f,0f,0f,0f,0f,1f],item:{id:'minecraft:carrot_on_a_stick',count:1,components:{'minecraft:item_model':'lbc:st/"+model+"'}}}")));}catch(Exception e){throw new IllegalStateException(e);}
        display.entityTags().add("magic");display.setPos(p.getEyePosition().add(p.getLookAngle().scale(2)));insert(p,display);
        flying.add(new Flying(p,p.level(),game.items().epoch(),display,kind,new int[]{0}));
        LbcOverload.sound(p,p.position(),"minecraft:block.enchantment_table.use",2,1,0);
    }
    void flyingPayload(ServerPlayer p,Vec3 at,Vec3 direction,int kind){
        var empty=ItemStack.EMPTY;
        var previous=launching;int previousLifetime=sourceLifetime;launching=p;sourceLifetime=new int[]{50,200,0,120,300,60,80,0,100}[kind];
        try{
        switch(kind){
            case 0->game.items().ruby().castAt(p,at,direction);
            case 1->game.items().terra().castAt(p,at,direction,LbcTerra.Kind.REGULAR);
            case 2->game.items().wands().libraryFlyingElectro(p,at,direction);
            case 3->game.items().air().spawn(p,InteractionHand.MAIN_HAND,0,empty,at,direction);
            case 4->captured(game.items().clouds().sourceAcid(p.level(),at,(float)Math.toDegrees(Math.atan2(-direction.x,direction.z)),(float)-Math.toDegrees(Math.asin(direction.y)),300,false,p));
            case 5->game.items().teleport().castAt(p,InteractionHand.MAIN_HAND,0,empty,at,direction);
            case 6->game.items().stone().castAt(p,at,direction);
            case 7->{var ball=new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(EntityTypes.SMALL_FIREBALL,p.level());game.items().wands().launchFire(p,ball,at,direction.scale(2.5));LbcOverload.sound(p,at,"minecraft:item.firecharge.use",1,1,0);}
            case 8->game.items().blind().spawn(p,at,direction,false,100,InteractionHand.MAIN_HAND,false);
            default->throw new IllegalArgumentException("Flying wand kind "+kind);
        }
        }finally{launching=previous;sourceLifetime=previousLifetime;}
    }
    void tick(){
        actors.removeIf(Entity::isRemoved);
        for(var entry:List.copyOf(expiries.entrySet())){
            var entity=entry.getKey();var expiry=entry.getValue();
            if(entity.isRemoved()){expiries.remove(entity);continue;}
            if(!active(expiry.owner,game.items().epoch(),(ServerLevel)entity.level())){entity.discard();expiries.remove(entity);continue;}
            if(game.items().timeStop().frozen(entity)||game.items().timeStop().stopped((ServerLevel)entity.level()))continue;
            if(--expiry.remaining[0]<=0){entity.discard();expiries.remove(entity);}
        }
        for(var e:List.copyOf(emitters)){
            if(!active(e.owner,e.epoch,e.level)||e.age>=e.lifetime){emitters.remove(e);continue;}
            if(e.type==17&&game.items().magic().suppressedAt(e.level,e.at)){emitters.remove(e);continue;}
            if(game.items().timeStop().stopped(e.level))continue;
            if(e.type==17&&e.age%5==0)e.level.sendParticles(ParticleTypes.SOUL,e.at.x,e.at.y,e.at.z,50,10,10,10,0);
            if(e.type==17&&e.age%10==0){
                var targets=e.level.getEntitiesOfClass(LivingEntity.class,new AABB(e.at,e.at).inflate(16),t->game.items().wands().validTarget(t)&&!t.entityTags().contains("nomagic_active")&&!game.items().magic().suppressedAt(e.level,t.position())&&t.position().distanceToSqr(e.at)<=256);
                if(!targets.isEmpty())game.items().vexSword().phantomShot(e.owner,targets.get(e.owner.getRandom().nextInt(targets.size())),e.owner.getRandom().nextInt(8)+1,true);
            }else if(e.type==21&&e.age%2==0){for(int i=0;i<3;i++){
                var at=e.at.add(e.owner.getRandom().nextInt(71)-35,0,e.owner.getRandom().nextInt(71)-35);
                var ball=new LargeFireball(e.level,e.owner,new Vec3(0,-.12,0),3);game.items().wands().launchFire(e.owner,ball,at,new Vec3(0,-.12,0));
            }}else if(e.type==22){for(int i=0;i<10;i++){
                var at=e.at.add(e.owner.getRandom().nextInt(31)-15,0,e.owner.getRandom().nextInt(31)-15);
                if(!e.level.hasChunkAt(BlockPos.containing(at)))continue;
                var block=EntityTypes.FALLING_BLOCK.create(e.level,EntitySpawnReason.TRIGGERED);if(block==null)continue;
                try{block.load(TagValueInput.create(ProblemReporter.DISCARDING,block.registryAccess(),TagParser.parseCompoundFully("{BlockState:'minecraft:stone_bricks',Time:1,DropItem:0b,HurtEntities:1b,FallHurtMax:10,FallHurtAmount:10f}")));}catch(Exception error){throw new IllegalStateException(error);}
                block.setPos(at);block.setDeltaMovement(0,-2,0);insert(e.owner,block);
            }}
            e.age++;
        }
        for(var f:List.copyOf(flying)){
            var at=f.display.position();
            if(!active(f.owner,f.epoch,f.level)||f.display.isRemoved()||f.age[0]>=200||!f.level.hasChunkAt(BlockPos.containing(at))||!f.level.getBlockState(BlockPos.containing(at)).is(TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("airs")))){f.display.discard();flying.remove(f);continue;}
            if(game.items().magic().suppressedAt(f.level,at)){f.display.discard();flying.remove(f);continue;}
            if(game.items().timeStop().stopped(f.level))continue;
            var target=f.level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(150),t->t!=f.owner&&game.items().wands().target(f.owner,t)&&t.position().distanceToSqr(at)<=22500).stream().min(Comparator.comparingDouble(t->t.position().distanceToSqr(at))).orElse(null);
            var direction=target==null?f.owner.getLookAngle():target.getEyePosition().subtract(at).normalize();
            f.display.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));f.display.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
            int period=new int[]{60,20,20,60,20,80,60,5,10}[f.kind];
            if(f.age[0]%period==1)flyingPayload(f.owner,at,direction,f.kind);
            var away=at.subtract(f.owner.getEyePosition()).normalize();f.display.setPos(at.add(away.scale(.1)));f.age[0]++;
            f.level.sendParticles(ParticleTypes.WITCH,at.x,at.y,at.z,2,.5,.5,.5,1);
        }
    }
    int count(){return emitters.size()+flying.size()+actors.size()+expiries.size();}
    void forget(ServerPlayer p){emitters.removeIf(e->e.owner==p);for(var f:List.copyOf(flying))if(f.owner==p){f.display.discard();flying.remove(f);}for(var e:List.copyOf(actors))if(game.items().summons().owner(e)==p){e.discard();actors.remove(e);}for(var e:List.copyOf(expiries.entrySet()))if(e.getValue().owner==p){e.getKey().discard();expiries.remove(e.getKey());}}
    void stop(){emitters.clear();flying.clear();var old=List.copyOf(actors);actors.clear();old.forEach(Entity::discard);var children=List.copyOf(expiries.keySet());expiries.clear();children.forEach(Entity::discard);}
}
