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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** The source random-library extras use finite native states and the existing spell factories. */
final class LbcLibraryRandom {
    private final GameController game;
    private final List<Emitter> emitters=new ArrayList<>();
    private IdentityHashMap<Entity,Boolean> actorTable=new IdentityHashMap<>();
    private Set<Entity> actors=actorTable.keySet();
    private final Set<LbcExasperato.RayTicket> pendingRays=new LinkedHashSet<>();
    private int inProgress;
    private static final int[] BASIC={5,25,10,15,1,20,17,0,112,116,125,120,0,123,0,0,0,0,0,0,0,0,134,0,136,141,0,148,154,171,167,184,165,181,160,176,173,163,157,188,195,229,212,225,216,204,199,220,0,233,0,0,159};
    private static final class Emitter {
        ServerPlayer owner;final ServerLevel level;final String epoch;final Vec3 at;final int type,lifetime;int age;
        Emitter(ServerPlayer owner,Vec3 at,int type,int lifetime){this.owner=owner;level=owner.level();epoch=me.skywars2.SkyWars2.game().items().epoch();this.at=at;this.type=type;this.lifetime=lifetime;}
    }
    LbcLibraryRandom(GameController game){this.game=game;}
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
    private void ray(ServerPlayer p,int type){ray(p,type,null,null);}
    /** Raw aliases retain accepted data; admission and debit resolve the current body at the terminal. */
    void ray(ServerPlayer p,int type,String rawFlag,Item used){
        ++inProgress;try {
            game.items().exasperato().replayableTool(p,p.getEyePosition(),p.getLookAngle(),200,false,
                    new RandomRay(p,type,rawFlag,used),false);
        }finally{--inProgress;}
    }
    private final class RandomRay implements LbcExasperato.ReplayableRay {
        final UUID author;final String epoch,rawFlag;final ServerLevel level;final Vec3 direction;
        final int type;final Item used;final net.minecraft.core.particles.ParticleOptions particle;
        LbcExasperato.RayTicket ticket;
        RandomRay(ServerPlayer p,int type,String rawFlag,Item used){
            author=p.getUUID();epoch=game.items().epoch();level=p.level();direction=p.getLookAngle();
            this.type=type;this.rawFlag=rawFlag;this.used=used;
            particle=switch(type){
                case 13->ParticleTypes.END_ROD;case 16->ParticleTypes.EXPLOSION;
                case 17->new net.minecraft.core.particles.DustParticleOptions(0xffffff,2);
                case 18->new net.minecraft.core.particles.DustParticleOptions(0x818181,3);
                case 19->ParticleTypes.HEART;case 20->ParticleTypes.FLAME;case 21->ParticleTypes.LAVA;
                case 22->new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState());
                default->throw new IllegalArgumentException("Unknown random ray "+type);
            };
        }
        public void submitted(LbcExasperato.RayTicket ticket){
            if(this.ticket!=null&&this.ticket!=ticket)throw new IllegalStateException("Random ray ticket changed");
            this.ticket=ticket;pendingRays.add(ticket);
        }
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            return !state.is(TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("airs")));
        }
        public boolean target(Entity entity){return entity instanceof LivingEntity&&!entity.getUUID().equals(author)&&game.items().wands().validTarget(entity);}
        public void trail(Vec3 from,Vec3 to){
            ++inProgress;try{
                double distance=from.distanceTo(to);for(double d=0;d<=distance;d++){
                    var at=from.add(direction.scale(d));level.sendParticles(particle,true,false,at.x,at.y,at.z,1,0,0,0,.01);
                }
            }finally{--inProgress;}
        }
        public void complete(net.minecraft.world.phys.HitResult hit){
            ++inProgress;try{
                if(!pendingRays.remove(ticket))return;
                var p=game.server().getPlayerList().getPlayer(author);
                if(hit!=null&&p!=null&&active(p,epoch,level)&&(rawFlag==null||LbcLibrarySpells.hasRaw(p,rawFlag))){
                    impact(p,hit.getLocation().subtract(direction.scale(hit instanceof net.minecraft.world.phys.BlockHitResult?1:0)),type);
                    if(rawFlag!=null)LbcLibrarySpells.debitRawRaycast(p,rawFlag,used);
                }
            }finally{--inProgress;}
        }
    }
    void impact(ServerPlayer p,Vec3 at,int type){
        ++inProgress;try {
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
        }finally{--inProgress;}
    }
    private void insert(ServerPlayer p,Entity entity){
        ++inProgress;try {
            game.items().summons().track(p,entity);actorTable.put(entity,Boolean.TRUE);game.items().timeStop().prepareFrozen(entity);
            if(!p.level().addFreshEntity(entity)){actors.remove(entity);entity.discard();}
        }finally{--inProgress;}
    }
    private void lava(ServerPlayer p,BlockPos center){
        ++inProgress;try {
            game.items().stonekivi().queuedWork(new Lava(p,center));
            for(var offset:List.of(new Vec3(0,30,15),new Vec3(0,30,-15),new Vec3(15,30,0),new Vec3(-15,30,0))){
                var ghast=EntityTypes.GHAST.create(p.level(),EntitySpawnReason.TRIGGERED);if(ghast!=null){ghast.setPos(Vec3.atLowerCornerOf(center).add(offset));insert(p,ghast);}
            }
        }finally{--inProgress;}
    }
    private final class Lava implements LbcStonekivi.Work {
        final ServerLevel level;final String epoch;final BlockPos center;
        final List<List<String>> rules=List.of(List.of("to_nylium","crimson_nylium"),List.of("logs","crimson_hyphae"),
                List.of("leaves","nether_wart_block"),List.of("to_shroomlight","shroomlight"),List.of("to_soulsand","soul_sand"),
                List.of("to_magma","magma_block"),List.of("to_netherrack","netherrack"),List.of("fluids","obsidian"),
                List.of("airs_to_fire","fire"),List.of("to_lava","lava"));
        ServerPlayer owner;final BlockAreaWork scan;
        Lava(ServerPlayer owner,BlockPos center){
            this.owner=owner;this.center=center.immutable();level=owner.level();epoch=game.items().epoch();
            scan=new BlockAreaWork(level,state->true,this::replacement,2|16|32,true);
            scan.include(new BlockAreaWork.Bounds(center.getX()-15,center.getY()-15,center.getZ()-15,center.getX()+15,center.getY()+15,center.getZ()+15));
            scan.include(new BlockAreaWork.Bounds(center.getX(),1,center.getZ(),center.getX(),126,center.getZ()));
        }
        private net.minecraft.world.level.block.state.BlockState replacement(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            if(pos.getX()==center.getX()&&pos.getZ()==center.getZ()&&pos.getY()>=1&&pos.getY()<=126)return Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,1);
            var next=state;for(var rule:rules)if(next.is(TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace(rule.get(0)))))next=BuiltInBlock(rule.get(1));
            // Source sourcelava is a31-cube containing only source lava, integrity0.0001.
            if(Math.abs(pos.getX()-center.getX())<=15&&Math.abs(pos.getY()-center.getY())<=15&&Math.abs(pos.getZ()-center.getZ())<=15&&owner.getRandom().nextFloat()<.0001f)return Blocks.LAVA.defaultBlockState();
            return next;
        }
        public boolean step(){
            ++inProgress;try{
                if(!active(owner,epoch,level)){cancel();return true;}
                scan.step(game,owner,1024,System.nanoTime()+1_000_000);return !scan.hasWork();
            }finally{--inProgress;}
        }
        public void cancel(){++inProgress;try{scan.clear();}finally{--inProgress;}}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var cells=scan.checkpoint();var author=graph.links.key(owner);
            return links->{checkpointBoundary();var current=(ServerPlayer)links.get(author);MatchAuthorityCheckpoint.world(game,level);
                return ()->{owner=current;cells.restore();};};
        }
    }
    private static net.minecraft.world.level.block.state.BlockState BuiltInBlock(String name){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Identifier.withDefaultNamespace(name)).orElseThrow().value().defaultBlockState();}
    static void flyingWand(GameController g,ServerPlayer p){g.items().libraryRandom().fly(p,p.getRandom().nextInt(9));}
    void fly(ServerPlayer p,int kind){
        game.items().sauvojen().cast(p,InteractionHand.MAIN_HAND,ItemStack.EMPTY,kind);
    }
    void flyingPayload(ServerPlayer p,Vec3 at,Vec3 direction,int kind){
        if(kind<0||kind>=LbcSauvojen.Kind.values().length)throw new IllegalArgumentException("Flying wand kind "+kind);
        game.items().sauvojen().emit(p,InteractionHand.MAIN_HAND,ItemStack.EMPTY,LbcSauvojen.Kind.values()[kind],at,direction);
    }
    void tick(){
        ++inProgress;try {
            actors.removeIf(Entity::isRemoved);
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
        }finally{--inProgress;}
    }
    int count(){return emitters.size()+game.items().sauvojen().count()+actors.size();}
    void forget(ServerPlayer p){
        ++inProgress;try {emitters.removeIf(e->e.owner==p);game.items().sauvojen().forget(p);for(var e:List.copyOf(actors))if(game.items().summons().owner(e)==p){e.discard();actors.remove(e);}
        }finally{--inProgress;}
    }
    void stop(){
        ++inProgress;try {for(var ticket:List.copyOf(pendingRays))ticket.cancel();pendingRays.clear();emitters.clear();game.items().sauvojen().stop();var old=List.copyOf(actors);actors.clear();old.forEach(Entity::discard);
        }finally{--inProgress;}
    }
    int pendingRayCount(){return pendingRays.size();}
    void checkpointBoundary(){if(inProgress!=0)throw new IllegalStateException("Finish LibraryRandom callbacks before checkpointing");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record EmitterImage(Emitter original,MatchDamageSources.Ref owner,int age) { }
        private final List<EmitterImage> saved;
        private final MatchSpellCheckpoint.IdentityImage<Entity,Boolean> actorImage;
        private final List<LbcExasperato.RayTicket> waiting=List.copyOf(pendingRays);
        Checkpoint(MatchSpellCheckpoint.Links links){
            saved=emitters.stream().map(e->new EmitterImage(e,links.key(e.owner),e.age)).toList();
            actorImage=new MatchSpellCheckpoint.IdentityImage<>(actorTable,links);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var bodies=actorImage.restore(links);var mutations=new ArrayList<Runnable>();
            for(var image:saved){var owner=(ServerPlayer)links.get(image.owner);MatchAuthorityCheckpoint.world(game,image.original.level);
                mutations.add(()->{image.original.owner=owner;image.original.age=image.age;});}
            return ()->{mutations.forEach(Runnable::run);emitters.clear();saved.forEach(e->emitters.add(e.original));
                actorTable=bodies;actors=actorTable.keySet();pendingRays.clear();pendingRays.addAll(waiting);};
        }
    }

}
