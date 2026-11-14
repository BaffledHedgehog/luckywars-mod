package me.skywars2.game;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;

/** Source cosmetic runtime draft; no menu/economy engine. Shared native work owns every actor.
 * No code in this file reads entity NBT after explicit native actor construction. */
public final class LbcCosmetics {
    public enum Kind { SOUND,CAGE,SUFFIX,WIN,KILL }
    public interface Policy extends LbcMidas.Policy {
        boolean has(ServerPlayer player,String exactSourceKey);
        /** Narrow mode/phase/role guard. ENDING must be explicitly admitted for Q27 source default. */
        boolean death(ServerPlayer victim);
        boolean kill(ServerPlayer killer,LivingEntity actualVictim);
        boolean winner(ServerPlayer winner);
        boolean postgame(String epoch);
        boolean mobTarget(ServerPlayer author,LivingEntity target);
        boolean explosion(ServerPlayer author,ServerLevel level,Vec3 at);
        /** Only MapManager calls cage() and owns the original blocks and restore/removal set. */
        void cageWrite(ServerLevel level,BlockPos at,BlockState state);
    }
    private static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> TANNER=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:tannerkivi"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String ACTOR="skywars2.cosmetic";
    private static final String[] SOUNDS={"entity.piglin.hurt","entity.zombie.hurt","entity.witch.celebrate","entity.bee.death","entity.horse.death","entity.chicken.death","entity.pig.death","entity.wolf.growl","entity.villager.death","entity.vindicator.death","entity.lightning_bolt.thunder","entity.ender_dragon.ambient","entity.ghast.hurt"};
    private static final String[] SUFFIX={"","★","۞","♛","♔","♠","♣","♥","♦","♪","☉","20㎝","☠","卍","✟","❤","🤩","\uE186","\uE187","\uE188","\uE189","\uE190","\uE191","\uE192","\uE193","\uE194","\uE195","\uE207","\uE208","\uE209"};
    private static final DyeColor[] RAINBOW={DyeColor.RED,DyeColor.YELLOW,DyeColor.GREEN,DyeColor.BLUE,DyeColor.PURPLE};
    private static final double[][] SWORD_ORIGINS={{-3,0},{-2,1},{-1,2},{-.25,2.5},{.25,2.5},{1,2},{2,1},{3,0}};
    private static final String HAMMER="{view_range:10f,teleport_duration:1,item:{id:'minecraft:carrot_on_a_stick',count:1,components:{'minecraft:item_model':'lbc:banhammer3d'}},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[2f,2f,2f]}}";
    private static final String SWORD="{interpolation_duration:1,shadow_radius:0f,shadow_strength:0f,teleport_duration:1,item_display:'fixed',transformation:[0f,0f,2.5f,0f,1.7678f,-1.7678f,0f,0f,1.7678f,1.7678f,0f,0f,0f,0f,0f,3f],item:{id:'minecraft:golden_sword',count:1}}";
    private final GameController game;private final Policy policy;private final LbcMidas midas;private final LbcNameplates nameplates;
    private Set<Job> jobs=new LinkedHashSet<>();private Map<UUID,Entity> nativeActors=new LinkedHashMap<>();
    private boolean restoring;private int callbacks;
    public LbcCosmetics(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);midas=new LbcMidas(game,policy);nameplates=new LbcNameplates(game);}
    public LbcNameplates nameplates(){return nameplates;}
    public boolean unlocked(ServerPlayer player,Kind kind,int id){
        if(id==0)return true;if(id<0)return false;
        String key=unlockKey(kind,id);return key!=null&&policy.has(player,key);
    }
    static String unlockKey(Kind kind,int id){
        if(id<=0)return null;
        return switch(kind){
            case SOUND -> id<=13?"swrg.sound"+id:null;
            case CAGE -> id<=20?"swrg.bars"+id:null;
            case SUFFIX -> id<=26?"swrg:rewards/suffix_"+id:id<=29?"swrg:rewards/turnir/trophy"+(id-26):null;
            case WIN -> id==1?"lbc:true_advancements/event/pumpkin_sword":id==2?"swrg:rewards/turnir/trophy1":null;
            case KILL -> id==1?"lbc:true_advancements/event/soulcorrupt_wand_upgraded":id==2?"lbc:true_advancements/event/soulhunter_mace":id==3?"swrg:rewards/turnir/killer":null;
        };
    }
    int choice(ServerPlayer p,Kind kind){
        if(game.balanceLegacy()&&(kind==Kind.SUFFIX||kind==Kind.WIN||kind==Kind.KILL))return 0;
        var profile=game.players().get(p.getUUID());int id=switch(kind){case SOUND->profile.killSound;case CAGE->profile.cage;case SUFFIX->profile.suffix;case WIN->profile.winEffect;case KILL->profile.killEffect;};
        return unlocked(p,kind,id)?id:0;
    }
    /** One call at every actual life-lost boundary, including BedWars respawn deaths. */
    public void deathSound(ServerPlayer victim){
        if(restoring||!policy.death(victim))return;int id=choice(victim,Kind.SOUND);if(id==0)return;
        var sound=Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:"+SOUNDS[id-1])));
        float pitch=id==1?.8f:id==3?.9f:1;
        long seed=victim.getRandom().nextLong();
        for(var listener:game.server().getPlayerList().getPlayers()){
            var at=LbcFpvDrone.viewpoint(listener);listener.connection.send(new ClientboundSoundPacket(sound,SoundSource.MASTER,at.x,at.y,at.z,1,pitch,seed));
        }
    }
    public static Component suffix(int id){
        if(id<=0||id>=SUFFIX.length)return Component.empty();var result=Component.literal(" "+SUFFIX[id]).withStyle(s->s.withBold(false));
        return id>=17?result.withStyle(s->s.withColor(0x4E5C24)):result;
    }
    /** Q26: each server-rendered name uses its own canonical selection. Gameplay teams stay shared. */
    public Component displayName(ServerPlayer player,Component original){
        if(game.balanceLegacy()) {
            return player.getName().copy().setStyle(original.getStyle());
        }
        int id=choice(player,Kind.SUFFIX);
        return id==0?original:original.copy().append(suffix(id));
    }
    /** Native TAB entries cache their component, so publish after selection, join and rewind. */
    public void refreshSuffixes(Collection<ServerPlayer> players){
        if(players.isEmpty())return;
        nameplates.refresh(players);
        var packet=new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),players);
        game.server().getPlayerList().broadcastAll(packet);
    }
    /** Remove only the old source cosmetic projection; preserve unrelated team formatting. */
    public void startSuffixes(Collection<ServerPlayer> participants,long seed){
        var teams=new HashSet<net.minecraft.world.scores.PlayerTeam>();
        for(var player:participants){var team=player.getTeam();if(team!=null&&teams.add(team)){
            for(int id=1;id<SUFFIX.length;id++)if(team.getPlayerSuffix().equals(suffix(id))){team.setPlayerSuffix(Component.empty());break;}
        }}
        refreshSuffixes(participants);
    }
    /** Caller has built canonical glass; zero/bedwars3/4 do nothing. Every write is tracked by MapManager. */
    public void cage(ServerPlayer player,ServerLevel level,BlockPos feet,int mode){
        int id=choice(player,Kind.CAGE);if(id==0||mode==3||mode==4)return;
        for(int y=-2;y<=4;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var at=feet.offset(x,y,z);BlockState wanted=null;
            if(id==19&&Math.abs(x)==1&&Math.abs(z)==1&&y<=3)wanted=Blocks.IRON_CHAIN.defaultBlockState();
            else if(level.getBlockState(at).is(Blocks.GLASS)){
                if(id<=16)wanted=Blocks.STAINED_GLASS.pick(DyeColor.values()[id-1]).defaultBlockState();
                else if(id==17||id==19)wanted=Blocks.BARRIER.defaultBlockState();
                else if(id==18)wanted=Blocks.ICE.defaultBlockState();
                else if(y<=2)wanted=Blocks.STAINED_GLASS.pick(RAINBOW[y+2]).defaultBlockState();
            }
            if(wanted!=null)policy.cageWrite(level,at,wanted);
        }
    }
    public record DropBody(UUID identity,boolean experience,int sourceAgeBucket,Vec3 position){
        public DropBody { if(sourceAgeBucket<0||sourceAgeBucket>1)throw new IllegalArgumentException("Original native death body age must be 0 or 1"); }
    }
    /** Native die admission precedes AFTER_DEATH cleanup/awaitingRespawn; no scope survives a checkpoint. */
    record AcceptedKill(ServerPlayer killer,LivingEntity victim,ServerLevel level,Vec3 killerAt,Vec3 victimAt,
                        int selected,LbcIndirectKills.Owner cause,String epoch){}
    AcceptedKill acceptKill(ServerPlayer killer,LivingEntity victim){
        if(restoring||!policy.kill(killer,victim))return null;int id=choice(killer,Kind.KILL);if(id==0)return null;
        return new AcceptedKill(killer,victim,(ServerLevel)victim.level(),killer.position(),victim.position(),id,
                game.items().indirectKills().wildCause(killer),game.items().epoch());
    }
    /** Call once after the actual native death scope closes. Pass only original successful publications.
     * Copies from Looting/Hakurei and unrelated same-tick deaths never enter this list. */
    public void killed(ServerPlayer killer,LivingEntity victim,List<DropBody> originalBodies){
        killed(acceptKill(killer,victim),originalBodies);
    }
    void killed(AcceptedKill accepted,List<DropBody> originalBodies){
        if(accepted==null||restoring||!policy.active(accepted.epoch))return;
        var killer=accepted.killer;var victim=accepted.victim;int id=accepted.selected;
        var level=accepted.level;var at=accepted.victimAt;var cause=accepted.cause;
        if(victim instanceof ServerPlayer){
            if(id==2){announce(victim,"wagnerkilled",true);sound(level,at,"minecraft:block.bell.use",3,1,0,0);enqueue(new Job(killer,level,at.add(0,11,0),cause,Effect.HAMMER));}
            else if(id==3){particles(level,new ItemParticleOption(ParticleTypes.ITEM,Items.REDSTONE_BLOCK),at.add(0,1,0),1000,.3,.6,.3,.8,false);announce(victim,"bloodshedkilled",false);}
        }
        if(id==1||id==3){
            var job=new Job(killer,level,accepted.killerAt,cause,Effect.DROPS);job.selected=id;job.drops=List.copyOf(originalBodies);job.due=game.server().getTickCount();enqueue(job);
            if(id==3){int random=job.random(9);sound(level,accepted.killerAt,"minecraft:lbcsounds.meat"+(random%3+1),1,new float[]{1,1.5f,.8f}[random/3],20,.4f);}
        }
    }
    private void announce(LivingEntity victim,String key,boolean hammer){
        var translated=Component.translatable(key).withStyle(s->s.withColor(hammer?0xAA0000:0xFF5555).withBold(hammer));
        var text=Component.empty().append(victim.getDisplayName()).append(translated);
        for(var viewer:game.server().getPlayerList().getPlayers())viewer.sendSystemMessage(text);
    }
    /** Return true to suppress MatchEvents' old fallback fireworks for a selected source celebration. */
    public boolean winner(ServerPlayer winner,int sourceTicker){
        if(restoring||!policy.winner(winner))return false;int id=choice(winner,Kind.WIN);if(id==0)return false;
        var cause=game.items().indirectKills().wildCause(winner);var level=(ServerLevel)winner.level();
        if(id==1){for(int n=0;n<5;n++){var job=new Job(winner,level,winner.position(),cause,Effect.PUMPKIN);job.spreadCenter=Vec3.ZERO;job.spreadRange=190;job.spreading=true;enqueue(job);}sound(level,winner.position(),"minecraft:lbcsounds.halloween",.7f,0,0,.7f);}
        else {var job=new Job(winner,level,winner.position(),cause,Effect.WINNER);job.age=Math.floorMod(sourceTicker,60);enqueue(job);}return true;
    }
    private enum Effect { DROPS,SOUL,BLOOD,HAMMER,PUMPKIN,WINNER,SWORD,FILL }
    private void enqueue(Job job){jobs.add(job);game.items().stonekivi().queuedWork(job);}
    private final class Job implements LbcStonekivi.Work {
        ServerPlayer author;final ServerLevel level;final LbcIndirectKills.Owner cause;final String epoch;final Effect effect;
        Vec3 at,direction=Vec3.ZERO,spreadCenter=Vec3.ZERO;Entity display;List<DropBody> drops=List.of();
        List<BlockPos> cells=List.of();BlockState fill;TagKey<Block> replace;Job dependency;
        int due,age,selected,cursor,stage,spreadRange,spreadAttempts,spreadY=Integer.MIN_VALUE;long rng;
        boolean closed,spreading,impact,postgame,air1,air2;Vec3 candidate;
        Job(ServerPlayer author,ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause,Effect effect){
            this(author,level,at,cause,effect,author.getRandom().nextLong());
        }
        Job(ServerPlayer author,ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause,Effect effect,long seed){
            this.author=author;this.level=level;this.at=at;this.cause=cause;this.effect=effect;epoch=game.items().epoch();
            due=game.server().getTickCount();rng=seed;postgame=effect==Effect.PUMPKIN||effect==Effect.WINNER||effect==Effect.SWORD;
            if(effect==Effect.SOUL||effect==Effect.BLOOD||effect==Effect.HAMMER||effect==Effect.SWORD)age=1;
        }
        long next(){rng+=0x9E3779B97F4A7C15L;long z=rng;z=(z^(z>>>30))*0xBF58476D1CE4E5B9L;z=(z^(z>>>27))*0x94D049BB133111EBL;return z^(z>>>31);}
        int random(int bound){return (int)Long.remainderUnsigned(next(),bound);}
        double unit(){return (next()>>>11)*0x1.0p-53;}
        boolean active(){return !closed&&policy.active(epoch)&&(!postgame||policy.postgame(epoch));}
        /** Wire to a new default-false capability on the EXISTING shared Work interface. */
        public boolean postgame(){return postgame;}
        public boolean ready(){return !restoring&&(closed||!active()||!game.items().timeStop().stopped(level)&&game.server().getTickCount()>=due&&(dependency==null||dependency.closed));}
        public boolean snapshotIdle(){return true;}
        boolean loaded(Vec3 pos){return loaded(BlockPos.containing(pos));}
        boolean loaded(BlockPos pos){if(level.hasChunkAt(pos))return true;game.maps().prepareChunk(level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;}
        public boolean step(){
            if(restoring)return false;callbacks++;game.items().stonekivi().beginNativeWork();
            try{
                if(!active()){cancel();return true;}if(!ready())return false;if(!loaded(at))return false;
                if(display!=null&&display.isRemoved()){
                    if(display.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)return false;cancel();return true;
                }
                if(display!=null&&game.items().timeStop().frozen(display))return false;
                boolean finished=switch(effect){
                    case DROPS->dropActors();case SOUL->soul();case BLOOD->blood();case HAMMER->hammer();
                    case PUMPKIN->pumpkin();case WINNER->goldWinner();case SWORD->sword();case FILL->fill();
                };
                if(finished){cancel();return true;}return false;
            }finally{game.items().stonekivi().endNativeWork();callbacks--;}
        }
        void nextTick(){age++;due=game.server().getTickCount()+1;}
        boolean dropActors(){
            // Four SOURCE buckets: actual original item/XP published in the two relevant native ticks.
            var selectedBodies=new LinkedHashMap<Integer,DropBody>();var seen=new HashSet<UUID>();
            for(var body:drops){if(body.position.distanceToSqr(at)>625||!seen.add(body.identity))continue;
                int bucket=(body.experience?2:0)+body.sourceAgeBucket;var previous=selectedBodies.get(bucket);
                if(previous==null||body.position.distanceToSqr(at)<previous.position.distanceToSqr(at))selectedBodies.put(bucket,body);
            }
            for(var body:selectedBodies.values()){
                var child=new Job(author,level,body.position,cause,selected==1?Effect.SOUL:Effect.BLOOD);child.postgame=postgame;enqueue(child);
                if(selected==3)particles(level,block(Blocks.REDSTONE_BLOCK),body.position.add(0,.3,0),200,.3,.1,.3,.3,false);
            }return true;
        }
        boolean soul(){
            particles(level,ParticleTypes.SOUL_FIRE_FLAME,at,4,.2,.2,.2,0,false);
            var closest=level.getNearestPlayer(at.x,at.y,at.z,Double.MAX_VALUE,p->!p.isSpectator());
            if(closest!=null){var to=closest.position().add(0,1,0).subtract(at);at=at.add(to.normalize().scale(.4));
                for(var player:level.players())if(new AABB(at,at.add(1,1,1)).intersects(player.getBoundingBox()))return true;}
            nextTick();return false;
        }
        boolean blood(){
            particles(level,new ItemParticleOption(ParticleTypes.ITEM,Blocks.STAINED_GLASS_PANE.pick(DyeColor.RED).asItem()),at.add(0,.1,0),0,0,1,0,.5,true);
            particles(level,block(Blocks.REDSTONE_WIRE),at.add(0,.3,0),5,.3,.1,.3,.3,false);
            if(age>=80)return true;nextTick();return false;
        }
        boolean hammer(){
            if(display==null){display=display(level,at,HAMMER,cause);if(display==null)return false;}
            if(age<6){at=at.add(0,-2,0);display.setPos(at);}
            else{
                if(!impact){impact=true;particles(level,block(Blocks.REDSTONE_BLOCK),at.add(0,1.2,0),5,.5,.8,.5,0,false);particles(level,block(Blocks.REDSTONE_BLOCK),at.add(0,.1,0),1000,1,.1,1,0,false);sound(level,at,"minecraft:entity.generic.explode",3,0,0,0);}
                if(random(100)==0)return true;
            }nextTick();return false;
        }
        boolean spread(){
            // Exact mapped 26.3 SpreadPlayersCommand.Position getSpawnY/isSafe, resumed by column.
            // Source spreadDistance=0: independent square candidates require no pairwise relaxation.
            if(candidate==null)candidate=spreadCenter.add((unit()*2-1)*spreadRange,0,(unit()*2-1)*spreadRange);
            if(!loaded(candidate))return false;int x=BlockPos.containing(candidate).getX(),z=BlockPos.containing(candidate).getZ();
            int maxHeight=level.getMaxY()+1;
            if(spreadY==Integer.MIN_VALUE){spreadY=maxHeight+1;air2=level.getBlockState(new BlockPos(x,spreadY,z)).isAir();spreadY--;air1=level.getBlockState(new BlockPos(x,spreadY,z)).isAir();}
            int reads=0;while(spreadY>level.getMinY()&&reads++<64){
                spreadY--;var state=level.getBlockState(new BlockPos(x,spreadY,z));boolean air=state.isAir();
                if(!air&&air1&&air2){
                    if(spreadY<maxHeight&&!state.liquid()&&state.is(BlockTags.ENTITIES_CAN_TELEPORT_TO)){
                        at=new Vec3(x+.5,spreadY+1,z+.5);candidate=null;spreadY=Integer.MIN_VALUE;spreadAttempts=0;spreading=false;return true;
                    }
                    return rejectCandidate();
                }air2=air1;air1=air;
            }
            return spreadY<=level.getMinY()?rejectCandidate():false;
        }
        boolean rejectCandidate(){
            candidate=null;spreadY=Integer.MIN_VALUE;
            // Original failed spreadplayers leaves the marker at its current position.
            if(++spreadAttempts>=10000){spreadAttempts=0;spreading=false;return true;}return false;
        }
        boolean pumpkin(){
            if(spreading&&!spread())return false;
            if(stage==0){selected=random(10);stage=1;}
            if(stage==1){
                if(selected<3){var direction=new Vec3(0,100,0).subtract(at).normalize();if(!falling(level,at.add(direction),Blocks.PUMPKIN,cause))return false;}
                dependency=fillJob(this,at,5,Blocks.PUMPKIN.defaultBlockState(),TANNER,false);stage=2;return false;
            }
            blast(author,level,at,4,cause);spreadCenter=at;spreadRange=25;spreading=true;stage=0;dependency=null;nextTick();return false;
        }
        boolean goldWinner(){
            if(author.isRemoved()||author.level()!=level)return true;at=author.position();
            int phase=age%60;if(phase>=30)phase=60-phase;
            var ingotAt=at.add(0,5,0).add(local(author.getYRot()+phase*6,0,0,0,phase*.1));
            if(!loaded(ingotAt))return false;var ingot=new ItemEntity(level,ingotAt.x,ingotAt.y,ingotAt.z,new ItemStack(Items.GOLD_INGOT));
            // /summon item uses the zero-Motion entity constructor, unlike native dropped-stack scatter.
            ingot.setDeltaMovement(Vec3.ZERO);
            if(!publish(ingot,cause))return false;
            var offset=SWORD_ORIGINS[random(8)];var origin=author.getEyePosition().add(local(author.getYRot(),author.getXRot(),offset[0],offset[1],0));
            var child=new Job(author,level,origin,cause,Effect.SWORD);child.direction=author.getLookAngle();enqueue(child);nextTick();return false;
        }
        boolean sword(){
            if(display==null){display=display(level,at,SWORD,cause);if(display==null)return false;display.setYRot(author.getYRot());display.setXRot(author.getXRot());
                particles(level,block(Blocks.GOLD_BLOCK),at,15,.4,.4,.4,0,false);sound(level,at,"minecraft:block.iron.break",.3f,1.2f,0,0);}
            if(stage==0){
                for(int n=0;n<4;n++){
                    var point=at.add(direction.scale(n));if(!loaded(point))return false;
                    particles(level,block(Blocks.GOLD_BLOCK),point,1,0,0,0,0,true);
                    boolean hit=!level.getBlockState(BlockPos.containing(point)).is(AIRS);
                    if(!hit)hit=!level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point.add(1,1,1)),e->e.getType().builtInRegistryHolder().is(MOBS)&&!e.isSpectator()&&policy.mobTarget(author,e)).isEmpty();
                    if(hit){at=point;stage=1;selected=random(5);display.setPos(at);break;}
                }
                if(stage==0){at=at.add(direction.scale(3));display.setPos(at);if(age>=30)return true;nextTick();return false;}
            }
            return terminal();
        }
        boolean terminal(){
            switch(selected){
                case 0,3->{fillJob(this,at,3,selected==0?Blocks.GOLD_BLOCK.defaultBlockState():Blocks.RAW_GOLD_BLOCK.defaultBlockState(),SOLID,true);LbcOverload.sound(level,at,"minecraft:block.iron.place",SoundSource.BLOCKS,1,1,0,0);}
                case 1->{var pos=BlockPos.containing(at);if(policy.admittedCell(author,level,pos))policy.write(author,level,pos,Blocks.RAW_GOLD_BLOCK.defaultBlockState(),cause);midas.launch(author,level,pos,cause);}
                case 2->{while(cursor<18){double angle=Math.toRadians(display.getYRot()+cursor*10);var offset=new Vec3(-Math.sin(angle)*.5,1,Math.cos(angle)*.5);
                    if(!loaded(at.add(offset))||!falling(level,at.add(offset),Blocks.GOLD_BLOCK,cause))return false;cursor++;}blast(author,level,at,3,cause);}
                case 4->{fillJob(this,at,1,Blocks.GOLD_BLOCK.defaultBlockState(),null,false);LbcOverload.sound(level,at,"minecraft:block.iron.place",SoundSource.BLOCKS,1,1,0,0);}
            }return true;
        }
        boolean fill(){
            int visits=0;long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
            while(cursor<cells.size()&&visits++<128&&System.nanoTime()<deadline){
                var pos=cells.get(cursor);if(!loaded(pos))return false;
                var current=level.getBlockState(pos);if((replace==null||current.is(replace))&&policy.admittedCell(author,level,pos))policy.write(author,level,pos,fill,cause);cursor++;
            }return cursor==cells.size();
        }
        public void cancel(){closed=true;jobs.remove(this);if(display!=null){nativeActors.remove(display.getUUID());display.discard();display=null;}}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            boundary();var owner=graph.links.key(author);var body=display==null?null:graph.links.key(display);if(dependency!=null)graph.capture(dependency);
            var savedAt=at;var savedDirection=direction;var savedCenter=spreadCenter;var savedCandidate=candidate;var savedCells=List.copyOf(cells);var savedDrops=List.copyOf(drops);var savedDependency=dependency;
            int savedAge=age,savedSelected=selected,savedCursor=cursor,savedStage=stage,savedRange=spreadRange;long savedRng=rng;
            int savedSpreadY=spreadY,savedAttempts=spreadAttempts;boolean savedAir1=air1,savedAir2=air2;
            boolean savedClosed=closed,savedSpreading=spreading,savedImpact=impact,savedPostgame=postgame;var clock=new MatchAuthorityCheckpoint.Clock(game,due,0);
            return links->{boundary();MatchAuthorityCheckpoint.world(game,level);var rebound=(ServerPlayer)links.get(owner);var reboundBody=body==null?null:links.get(body);
                return ()->{author=rebound;display=reboundBody;at=savedAt;direction=savedDirection;spreadCenter=savedCenter;candidate=savedCandidate;cells=savedCells;drops=savedDrops;dependency=savedDependency;
                    age=savedAge;selected=savedSelected;cursor=savedCursor;stage=savedStage;spreadRange=savedRange;rng=savedRng;closed=savedClosed;spreading=savedSpreading;impact=savedImpact;postgame=savedPostgame;due=clock.last(game);
                    spreadY=savedSpreadY;spreadAttempts=savedAttempts;air1=savedAir1;air2=savedAir2;};};
        }
    }
    private Job fillJob(Job parent,Vec3 at,int radius,BlockState state,TagKey<Block> replace,boolean cross){
        var job=new Job(parent.author,parent.level,at,parent.cause,Effect.FILL);job.postgame=parent.postgame;job.fill=state;job.replace=replace;
        var center=BlockPos.containing(at);var cells=new ArrayList<BlockPos>();
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
            if(cross){int a=Math.abs(x),b=Math.abs(y),c=Math.abs(z);if(!(a<=2&&b<=2&&c<=2)&&!(a<=1&&b<=3&&c<=1)&&!(a<=3&&b<=1&&c<=1)&&!(a<=1&&b<=1&&c<=3))continue;}
            cells.add(center.offset(x,y,z));
        }job.cells=List.copyOf(cells);enqueue(job);return job;
    }
    private Display.ItemDisplay display(ServerLevel level,Vec3 at,String payload,LbcIndirectKills.Owner cause){
        var entity=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);load(entity,level,payload);entity.setPos(at);entity.setNoGravity(true);
        if(!publish(entity,cause))return null;return entity;
    }
    private boolean falling(ServerLevel level,Vec3 at,Block block,LbcIndirectKills.Owner cause){
        var entity=new FallingBlockEntity(EntityTypes.FALLING_BLOCK,level);
        String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();load(entity,level,"{BlockState:{id:'"+id+"'},Time:0}");entity.setPos(at);return publish(entity,cause);
    }
    private static void load(Entity entity,ServerLevel level,String payload){
        try{entity.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),TagParser.parseCompoundFully(payload)));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("Invalid immutable source cosmetic actor",error);}
    }
    private boolean publish(Entity entity,LbcIndirectKills.Owner cause){
        entity.entityTags().add(ACTOR);entity.entityTags().add("skywars2.cosmetic.author."+cause.player());entity.entityTags().add("skywars2.cosmetic.order."+cause.order());game.items().markCurrentItemEpoch(entity);
        game.items().spellSources().bind(entity,new LbcSpellSource.Source(entity.getUUID(),cause.player(),game.items().epoch(),cause,Set.of()));
        game.items().timeStop().prepareFrozen(entity);nativeActors.put(entity.getUUID(),entity);
        boolean accepted;try(var scope=game.items().spellSources().scope(entity)){accepted=((ServerLevel)entity.level()).addFreshEntity(entity);}if(!accepted){nativeActors.remove(entity.getUUID());entity.discard();}return accepted;
    }
    /** Source fuse0 TNT, native damage/terrain operation. Source still needs existing postgame guards wired. */
    private void blast(ServerPlayer author,ServerLevel level,Vec3 at,float radius,LbcIndirectKills.Owner cause){
        if(!policy.explosion(author,level,at)||!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES))return;
        var source=new PrimedTnt(level,at.x,at.y,at.z,author){@Override public LivingEntity getOwner(){return author;}};
        source.setFuse(0);source.setDeltaMovement(Vec3.ZERO);source.entityTags().add(LbcDamage.UNPROTECTED_TNT);source.entityTags().add(ACTOR);
        source.entityTags().add("skywars2.cosmetic.author."+cause.player());source.entityTags().add("skywars2.cosmetic.order."+cause.order());game.items().markCurrentItemEpoch(source);
        var damage=game.items().indirectKills().captureDamage(level.damageSources().explosion(source,author),cause);
        try(var scope=game.items().spellSources().scope(new LbcSpellSource.Source(source.getUUID(),cause.player(),game.items().epoch(),cause,Set.of()),level)){game.items().indirectKills().capturedWorldChanges(level,cause,()->level.explode(source,damage,null,at.x,at.y,at.z,radius,false,Level.ExplosionInteraction.TNT));}
        finally{game.items().academy().blastFinished(source);game.items().exoperso().blastFinished(source);source.discard();}
    }
    private static Vec3 local(float yaw,float pitch,double left,double up,double forward){
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);var f=new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));
        var l=new Vec3(Math.cos(y),0,Math.sin(y));var u=f.cross(l);return l.scale(left).add(u.scale(up)).add(f.scale(forward));
    }
    private static BlockParticleOption block(Block block){return new BlockParticleOption(ParticleTypes.BLOCK,block.defaultBlockState());}
    private static void particles(ServerLevel level,ParticleOptions particle,Vec3 at,int count,double x,double y,double z,double speed,boolean forced){level.sendParticles(particle,forced,false,at.x,at.y,at.z,count,x,y,z,speed);}
    private static void sound(ServerLevel level,Vec3 at,String id,float volume,float pitch,double radius,float min){LbcOverload.sound(level,at,id,volume,pitch,radius,min);}
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}
    public void loaded(Entity entity){
        if(!managed(entity))return;if(!policy.active(game.items().epoch())||!game.items().currentEpoch(entity)){entity.discard();return;}
        nativeActors.put(entity.getUUID(),entity);for(var job:jobs)if(job.display!=null&&job.display.getUUID().equals(entity.getUUID()))job.display=entity;
    }
    public void unloaded(Entity entity){if(entity.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK)nativeActors.remove(entity.getUUID());}
    public LbcIndirectKills.Owner cause(Entity entity){
        if(!managed(entity))return null;UUID owner=null;long order=0;
        for(var tag:entity.entityTags())try{if(tag.startsWith("skywars2.cosmetic.author."))owner=UUID.fromString(tag.substring("skywars2.cosmetic.author.".length()));
            else if(tag.startsWith("skywars2.cosmetic.order."))order=Long.parseLong(tag.substring("skywars2.cosmetic.order.".length()));}catch(IllegalArgumentException ignored){}
        return owner==null||order<=0?null:new LbcIndirectKills.Owner(owner,order,true,false);
    }
    public ServerPlayer owner(Entity entity){var cause=cause(entity);return cause==null?null:game.server().getPlayerList().getPlayer(cause.player());}
    public LbcMidas midas(){return midas;}
    public void stop(){boundary();for(var job:List.copyOf(jobs))job.cancel();for(var entity:List.copyOf(nativeActors.values()))entity.discard();nativeActors.clear();midas.stop();}

    /** Keep only harmless, already accepted death visuals across the real gameplay-epoch retirement. */
    record EndingVisual(ServerPlayer author,ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause,
                        Effect effect,int age,int selected,boolean impact,long rng,int delay,List<DropBody> drops){}
    void beginCelebration(){
        boundary();
        for(var job:jobs)if(switch(job.effect){case DROPS,SOUL,BLOOD,HAMMER->true;default->false;})job.postgame=true;
    }
    List<EndingVisual> finishVisuals(){
        boundary();int now=game.server().getTickCount();
        return jobs.stream().filter(j->!j.closed&&switch(j.effect){case DROPS,SOUL,BLOOD,HAMMER->true;default->false;})
                .map(j->new EndingVisual(j.author,j.level,j.at,j.cause,j.effect,j.age,j.selected,j.impact,j.rng,
                        Math.max(0,j.due-now),j.drops)).toList();
    }
    void resumeVisuals(List<EndingVisual> images){
        boundary();if(game.stage()!=GameController.Stage.ENDING)return;
        for(var image:images){
            var job=new Job(image.author,image.level,image.at,image.cause,image.effect,image.rng);
            job.age=image.age;job.selected=image.selected;job.impact=image.impact;job.drops=image.drops;
            job.due=game.server().getTickCount()+image.delay;job.postgame=true;enqueue(job);
        }
    }
    void boundary(){if(callbacks!=0)throw new IllegalStateException("Finish cosmetic native quantum before capture/replay");}
    void prepareFrozen(){boundary();restoring=true;jobs=new LinkedHashSet<>();nativeActors=new LinkedHashMap<>();midas.prepareFrozen();}
    void publishFrozen(){boundary();restoring=false;midas.publishFrozen();refreshSuffixes(game.server().getPlayerList().getPlayers());}
    void cancelFrozen(){boundary();stop();restoring=false;midas.publishFrozen();}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){boundary();return new Checkpoint(graph);}
    final class Checkpoint {
        final Set<Job> saved;final LbcMidas.Checkpoint gold;final List<MatchDamageSources.Ref> bodies;
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){saved=new LinkedHashSet<>(jobs);saved.forEach(graph::capture);gold=midas.checkpoint(graph);bodies=nativeActors.values().stream().filter(e->!e.isRemoved()).map(graph.links::key).toList();}
        Runnable prepare(MatchSpellCheckpoint.Links links){var next=new LinkedHashMap<UUID,Entity>();for(var ref:bodies){var entity=links.get(ref);next.put(entity.getUUID(),entity);}
            return ()->{jobs=new LinkedHashSet<>(saved);nativeActors=next;gold.prepare().run();};}
    }
}
