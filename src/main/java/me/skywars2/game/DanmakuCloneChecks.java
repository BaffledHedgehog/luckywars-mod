package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Registered proximity cloning and whole-coordinator replay of actual native Danmaku bodies. */
public final class DanmakuCloneChecks {
    private DanmakuCloneChecks(){}
    private static Run running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Danmaku clones: "+why);}
    public static void start(GameController game){
        require(running==null,"exclusive scheduled fixture");var run=new Run(game);
        try{run.init();running=run;}catch(RuntimeException|Error failure){try{run.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}
        SkyWars2.LOGGER.info("[check] START danmaku-clones");
    }
    public static void tick(GameController game){
        if(running==null)return;var run=running;
        try{if(run.step()){running=null;run.close();SkyWars2.LOGGER.info("[check] PASS danmaku-clones (registered x2 projectiles-only/x4 mob inclusion; managed cards and paired fast bombs; actual consumable input; active typed whole-coordinator rewind; native310+60 retirement and no orphan displays)");}}
        catch(RuntimeException|Error failure){running=null;try{run.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL danmaku-clones",failure);}
    }
    private record CardState(UUID owner,int age,boolean homing,Vec3 direction,Vec3 position){}
    private record BombState(UUID owner,UUID negative,int charge,boolean fast,int age,int empty,Vec3 position){}
    private static final class Run implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture f;final LbcDanmaku danmaku;
        final String cowTag="danmaku_clone_fixture."+UUID.randomUUID();
        final Set<Cow> cows=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<String,Integer> ownerScores=new LinkedHashMap<>(),otherScores=new LinkedHashMap<>();
        RegenerationBeltChecks.ScheduledClient client,audience;
        Map<UUID,CardState> savedCards;Map<UUID,BombState> savedBombs;
        LbcDanmaku.Card sourceCard;LbcDanmaku.Bomb sourceBomb;
        UUID future;int stage,at,started,resumeAge,resumeEmpty,lastAge,lastEmpty,retirementTicks;boolean closed;
        Run(GameController game){
            this.game=game;
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot(),"no unrelated coordinator point");
            require(game.items().cloneSkills().ready()&&game.items().danmaku().count()==0,"installed adapter and empty Danmaku owner");
            f=new ChefChecks.Fixture(game);danmaku=game.items().danmaku();
            for(var player:game.server().getPlayerList().getPlayers()){
                require(danmaku.remaining(player)==0&&danmaku.externalRemaining(player)==0,"no unrelated source volley");
                if(player!=f.owner&&player!=f.other)require(!game.lbc().hasSkill(player,15)&&!game.lbc().hasSkill(player,17),"exclusive proximity holder");
            }
            for(var name:List.of("danmaku","danmaku_score")){ownerScores.put(name,f.mana.score(f.owner,name).get());otherScores.put(name,f.mana.score(f.other,name).get());}
        }
        void init(){
            f.init();client=new RegenerationBeltChecks.ScheduledClient(game,f.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,f.other);
            game.config().lbcSkills=true;game.config().ranked=true;
            for(var p:List.of(f.owner,f.other)){
                if(p.getTeam()!=null)game.server().getScoreboard().removePlayerFromTeam(p.getScoreboardName(),p.getTeam());
                game.server().getScoreboard().addPlayerToTeam(p.getScoreboardName(),f.team);
                f.mana.score(p,"danmaku").set(0);f.mana.score(p,"danmaku_score").set(0);
            }
            for(var entity:f.owner.level().getEntities((Entity)null,new AABB(f.at,f.at).inflate(24),e->e!=f.owner&&e!=f.other))
                require(!(entity instanceof net.minecraft.world.entity.LivingEntity),"exclusive24block damage footprint");
            started=game.server().getTickCount();stage=-1;at=started;
        }
        void select(int skill){game.players().get(f.owner.getUUID()).lbcSkill=skill;game.players().get(f.other.getUUID()).lbcSkill=0;}
        void begin(int skill){
            select(skill);f.move(f.owner,f.at);f.move(f.other,f.at);
            client.acknowledge();audience.acknowledge();
            sourceCard=danmaku.card(f.other,f.at.add(-2,4,-2),new Vec3(0,0,1),false);
            require(sourceCard!=null&&!sourceCard.display.isRemoved(),"actual native card body publication");
            var loot=game.lbc().rollTable(f.other.level(),f.at,f.other,"lbc:ported/touhou_bomb_item");
            require(loot.size()==1,"canonical bomb consumable singleton");
            var stack=loot.getFirst().copyWithCount(2);f.other.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(f.other.gameMode.useItem(f.other,f.other.level(),stack,InteractionHand.MAIN_HAND).consumesAction()
                    &&stack.getCount()==1,"ordinary native consumable input debits exactly one");
            f.other.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            require(danmaku.bombs().size()==1,"one actual source paired fast bomb");sourceBomb=danmaku.bombs().getFirst();
            require(sourceBomb.fast&&sourceBomb.charge==300&&sourceBomb.age==0&&sourceBomb.empty==0,"source fast phase starts at age0/charge300");
            var cow=f.cow(f.at.add(3,0,-3));cow.entityTags().add(cowTag);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);cows.add(cow);
            f.move(f.other,f.at.add(12,0,-12));audience.acknowledge();
            require(game.lbc().hasSkill(f.owner,skill),"actual selected clone skill admission");at=game.server().getTickCount();
        }
        List<Cow> nativeCows(){return f.owner.level().getEntitiesOfClass(Cow.class,new AABB(f.at,f.at).inflate(20),e->e.entityTags().contains(cowTag));}
        void family(int count,String marker){
            require(danmaku.cards().size()==count&&danmaku.bombs().size()==count,"exact native managed family count "+count);
            var ids=new HashSet<UUID>();
            for(var c:danmaku.cards()){
                require(c.owner==f.other&&!c.display.isRemoved()&&c.display.entityTags().contains(marker),"native card author and clone marker");
                require(c.age==sourceCard.age&&!c.homing&&c.direction.equals(sourceCard.direction),"card source age/direction retained independently");
                require(ids.add(c.display.getUUID()),"fresh card UUID");
            }
            for(var b:danmaku.bombs()){
                require(b.owner==f.other&&b.fast&&b.charge==300&&b.age==sourceBomb.age&&b.empty==sourceBomb.empty,"fast source phase/author copied");
                require(!b.positive.isRemoved()&&!b.negative.isRemoved()&&b.positive!=b.negative&&b.positive.position().equals(b.negative.position()),"one colocated live companion per positive");
                require(b.positive.entityTags().contains(marker)&&game.items().cloneSkills().protectedFamilyActor(b.negative),"positive participates and negative remains protected");
                require(ids.add(b.positive.getUUID())&&ids.add(b.negative.getUUID()),"all paired bodies have fresh distinct UUIDs");
            }
            census();
        }
        void census(){
            var expected=new HashSet<UUID>();for(var c:danmaku.cards())expected.add(c.display.getUUID());
            for(var b:danmaku.bombs()){expected.add(b.positive.getUUID());expected.add(b.negative.getUUID());}
            var actual=new HashSet<UUID>();for(var e:f.owner.level().getEntities((Entity)null,new AABB(f.at,f.at).inflate(24),LbcDanmaku::managed))actual.add(e.getUUID());
            require(actual.equals(expected),"no independently cloned negative or orphan managed display");
        }
        Map<UUID,CardState> cards(){var out=new LinkedHashMap<UUID,CardState>();for(var c:danmaku.cards())out.put(c.display.getUUID(),new CardState(c.owner.getUUID(),c.age,c.homing,c.direction,c.display.position()));return out;}
        Map<UUID,BombState> bombs(){var out=new LinkedHashMap<UUID,BombState>();for(var b:danmaku.bombs())out.put(b.positive.getUUID(),new BombState(b.owner.getUUID(),b.negative.getUUID(),b.charge,b.fast,b.age,b.empty,b.positive.position()));return out;}
        void clearCows(){for(var cow:nativeCows()){cows.add(cow);game.items().indirectKills().clear(cow);cow.discard();}}
        boolean step(){
            require(game.server().getTickCount()-started<1000,"bounded scheduled fixture stage "+stage);
            int elapsed=game.server().getTickCount()-at;
            if(game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            client.acknowledge();audience.acknowledge();
            if(stage==-1){begin(15);stage=0;return false;}
            if(stage==0){
                require(nativeCows().size()==1,"registered x2 excludes ordinary mobs");
                require(danmaku.cards().size()<=2&&danmaku.bombs().size()<=2,"x2 never clones paired negative separately");
                if(elapsed<4||danmaku.cards().size()!=2||danmaku.bombs().size()!=2){require(elapsed<12,"registered x2 completes bounded proximity work");return false;}
                family(2,"doubled");require(sourceBomb.age==10*elapsed,"source fast bomb advances10 per actual tick");
                danmaku.forget(f.other);clearCows();begin(17);stage=1;return false;
            }
            if(stage==1){
                require(danmaku.cards().size()<=4&&danmaku.bombs().size()<=4&&nativeCows().size()<=4,"x4 publishes one gameplay family per source");
                if(elapsed<4||danmaku.cards().size()!=4||danmaku.bombs().size()!=4||nativeCows().size()!=4){require(elapsed<12,"registered x4 includes native mob and both managed types");return false;}
                cows.addAll(nativeCows());family(4,"quadried");require(sourceBomb.age==10*elapsed,"x4 fast source advances only by registered native ticks");
                select(0);game.items().timeStop().begin(f.owner.level(),1000);savedCards=cards();savedBombs=bombs();
                require(game.vanity().requestCapture(f.owner,false),"real nonempty coordinator capture request");stage=2;at=game.server().getTickCount();return false;
            }
            if(stage==2){
                require(cards().equals(savedCards)&&bombs().equals(savedBombs),"TimeStop freezes every native source and cloned phase during capture");
                if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                danmaku.forget(f.other);require(danmaku.count()==0,"abandoned future actually removes all saved Danmaku owners");
                var extra=danmaku.card(f.other,f.at.add(0,3,0),new Vec3(1,0,0),true);require(extra!=null,"real abandoned future card");future=extra.display.getUUID();
                require(game.vanity().requestRestoreForCheck(),"actual full coordinator rewind request");stage=3;at=game.server().getTickCount();return false;
            }
            if(stage==3){
                if(game.vanity().busy())return false;
                require(cards().equals(savedCards)&&bombs().equals(savedBombs),"shared typed Danmaku links restore exact active card and both bomb body identities/phase");
                require(f.owner.level().getEntity(future)==null,"abandoned future display removed by canonical body restore");census();
                require(game.items().timeStop().stopped(f.owner.level()),"shared stop is restored with actors");
                game.vanity().stop();
                for(var c:List.copyOf(danmaku.cards())){c.display.discard();danmaku.unloaded(c.display);}
                clearCows();sourceBomb=danmaku.bombs().getFirst();resumeAge=lastAge=sourceBomb.age;resumeEmpty=lastEmpty=sourceBomb.empty;
                require(resumeAge>0&&resumeAge<310&&resumeEmpty==0,"rewind point retains an active expanding fast phase");
                retirementTicks=(310-resumeAge)/10+59;
                game.items().timeStop().end(f.owner.level());stage=4;at=game.server().getTickCount();return false;
            }
            if(danmaku.bombs().isEmpty()){
                require(elapsed==retirementTicks,"restored fast bombs retire at source310 plus60 empty ticks");census();return true;
            }
            require(danmaku.bombs().size()==4,"all four independent restored bomb owners continue together");
            int age=Math.min(310,resumeAge+elapsed*10),empty=resumeEmpty+Math.max(0,elapsed-Math.max(0,(310-resumeAge)/10)+1);
            // Crossing300 counts the first empty tick in the same source invocation.
            require(sourceBomb.age==age&&sourceBomb.empty==empty,"natural restored fast phase and retained empty timer");
            require(sourceBomb.age>=lastAge&&sourceBomb.empty>=lastEmpty,"phase never rewinds outside coordinator");lastAge=sourceBomb.age;lastEmpty=sourceBomb.empty;
            for(var b:danmaku.bombs())require(b.age==age&&b.empty==empty,"all copied fast phases remain independent and equal");census();return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;game.vanity().stop();game.items().timeStop().end(f.owner.level());
            danmaku.forget(f.other);clearCows();for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}
            if(client!=null)client.close();if(audience!=null)audience.close();f.close();
            ownerScores.forEach((name,value)->f.mana.score(f.owner,name).set(value));otherScores.forEach((name,value)->f.mana.score(f.other,name).set(value));
        }
    }
}
