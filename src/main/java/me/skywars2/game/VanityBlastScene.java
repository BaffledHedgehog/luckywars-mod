package me.skywars2.game;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Source factories and actual native payloads; bounded seeded schedule, not a full-power blast geometry claim. */
final class VanityBlastScene {
    final GameController game;final ChefChecks.Fixture base;final LbcExasperato first;final LbcExoperso second;
    final LbcExasperato.Charge charge;final LbcExoperso.Charge rocket;final PrimedTnt anchor,chain;
    final List<Object> payloads;List<Object> bursts;final List<Object> rays;final List<Object> rainbow;
    final Probe cohortProbe,trialProbe;final Object cohort;final LbcAcademy.BlastTrial trial;
    final UUID progress;final Vec3 far=new Vec3(1_000_000,100,1_000_000);
    MatchDamageSources capturedSources;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity blasts: "+why);}
    static <T>T get(Object o,String name){return VanitySpellScene.get(o,name);}static void set(Object o,String name,Object value){VanitySpellScene.set(o,name,value);}
    final class Probe implements MatchBlastCheckpoint.Binding {
        final Consumer<PrimedTnt> delegate;final boolean academy;int calls;
        Probe(Consumer<PrimedTnt> delegate,boolean academy){this.delegate=delegate;this.academy=academy;}
        public void accept(PrimedTnt source){delegate.accept(source);calls++;boolean rejected=false;try{new MatchBlastCheckpoint(game,capturedSources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Finish explosive callbacks");}require(rejected,"native initializer cannot capture/reenter its advancing queue");var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));require(source.getOwner()==owner,"actual queued native source uses CURRENT Gamma");var ledger=VanityBlastScene.<Map<Entity,Object>>get(academy?game.items().academy():second,"blastSources");require(ledger.get(source)==(academy?trial:cohort),"real initializer joins the same saved trial/cohort, not a copied achievement state");}
        public void checkpoint(MatchBlastCheckpoint.Graph graph){graph.binding(delegate);graph.remember(this,()->{int count=calls;return links->()->calls=count;});}
    }
    @SuppressWarnings("unchecked") VanityBlastScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;progress=base.other.getUUID();first=game.items().exasperato();second=game.items().exoperso();require(first.count()==0&&second.count()==0&&first.pendingBlasts()==0,"exclusive explosive source state");
        first.cast(gamma,true);charge=first.snapshot().getFirst();charge.setPos(base.at.add(-12,7,10));charge.setDeltaMovement(0,.3,0);charge.entityTags().add("sin_gluttony");
        require(second.cast(gamma,true),"real upgraded Exoperso factory");rocket=second.snapshot().getFirst();rocket.entityTags().add("sin_gluttony");
        var old=gamma.getMainHandItem().copy();gamma.setYRot(180);gamma.setXRot(0);
        var wall=base.center.offset(5,2,-2);require(base.blocks.containsKey(wall),"bounded rainbow/aim wall");gamma.level().setBlock(wall,Blocks.BEDROCK.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        LbcGaycaster.cast(game,gamma);game.items().gaycasterAcademy().cast(gamma,3);first.cast(gamma,4);gamma.setItemInHand(InteractionHand.MAIN_HAND,old);
        var all=VanityBlastScene.<ArrayDeque<Object>>get(first,"searches");rays=all.stream().skip(1).toList();require(rays.size()==3,"ordinary/academic rainbow and actual nuclear aim queued with prior Gluttony ray");rainbow=rays.subList(0,2).stream().map(r->VanityBlastScene.<Object>get(r,"replay")).toList();
        second.edibleBurst(gamma,base.at.add(-10,5,10));var queue=VanityBlastScene.<ArrayDeque<Object>>get(first,"blasts");var series=queue.getLast();require(VanityBlastScene.<Vec3[]>get(series,"offsets").length==32,"real factory still uses all32 nuclear offsets before bounded fixture seed");cohort=get(series,"sourceInit");
        trial=new LbcAcademy.BlastTrial(gamma);for(int n=0;n<9;n++)trial.targets.add(GameChecks.offlineUuid("VanityTrial"+n));
        first.exopersoBurst(gamma,base.at.add(-10,5,10),MatchBlastCheckpoint.academyBinding(game,trial));var academic=queue.getLast();
        cohortProbe=new Probe((Consumer<PrimedTnt>)cohort,false);trialProbe=new Probe((Consumer<PrimedTnt>)get(academic,"sourceInit"),true);set(series,"sourceInit",cohortProbe);set(academic,"sourceInit",trialProbe);
        for(var b:List.of(series,academic)){set(b,"offsets",new Vec3[]{Vec3.ZERO,new Vec3(.2,0,0),new Vec3(.4,0,0)});set(b,"radius",.2f);set(b,"next",1);}
        var nether=game.server().getLevel(net.minecraft.world.level.Level.NETHER);require(nether.getChunkSource().getChunkNow(62500,62500)==null,"cosmos point deliberately nonresident");first.queueCosmos(nether,far,null,game.items().epoch());payloads=List.of(series,academic);bursts=List.copyOf(queue);
        anchor=tnt(gamma,base.at.add(-13,8,13));chain=tnt(gamma,base.at.add(-12,8,13));((Consumer<PrimedTnt>)cohort).accept(anchor);second.inheritBlast(anchor,chain);game.items().academy().blastSource(anchor,trial);game.items().academy().inheritBlast(anchor,chain);
        require(get(VanityBlastScene.<Map<Entity,Object>>get(second,"blastSources").get(chain),"kills")!=null,"real chain blast inherits cohort");
        VanityBlastScene.<Set<UUID>>get(cohort,"kills").add(progress);
    }
    PrimedTnt tnt(ServerPlayer owner,Vec3 at){var tnt=new PrimedTnt(owner.level(),at.x,at.y,at.z,owner);tnt.setFuse(10000);tnt.setNoGravity(true);tnt.setDeltaMovement(Vec3.ZERO);tnt.entityTags().add("sin_gluttony");tnt.entityTags().add("have_stand");require(owner.level().addFreshEntity(tnt),"native achievement/chain source");return tnt;}
    int extraRays(){return rays.size();}
    void arm(){bursts=List.copyOf(VanityBlastScene.<ArrayDeque<Object>>get(first,"blasts"));charge.age=17;charge.mode=0;charge.lifetime=200;charge.academyTrial=true;rocket.age=100;rocket.flightAge=3;rocket.upgraded=true;rocket.aim=new Vec3(.1,.2,.3);rainbow.forEach(r->set(r,"next",1d));}
    void preflight(MatchDamageSources sources){capturedSources=sources;var b=payloads.getFirst();var init=get(b,"sourceInit");set(b,"sourceInit",(Consumer<PrimedTnt>)t->{});boolean rejected=false;try{new MatchBlastCheckpoint(game,sources);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Missing typed blast initializer");}finally{set(b,"sourceInit",init);}require(rejected&&first.pendingBlasts()==5&&cohortProbe.calls==0,"opaque initializer rejects without draining/replaying the queue");
}
    void instantPreflight(){
        var caster=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));int[] attempts={0};
        first.protectedBlast(caster,caster.level(),base.at.add(-10,5,10),.01f,tnt->{attempts[0]++;boolean blocked=false;try{new MatchBlastCheckpoint(game,null);}catch(IllegalStateException expected){blocked=expected.getMessage().contains("Finish explosive callbacks");}require(blocked,"direct instant explosion also rejects mid-callback capture");});
        require(attempts[0]==1,"actual direct native blast initializer executed once");var failure=new IllegalStateException("fixture initializer failure");PrimedTnt[] temporary={null};boolean failed=false;
        try{first.protectedBlast(caster,caster.level(),base.at.add(-10,5,10),.01f,tnt->{temporary[0]=tnt;((Consumer<PrimedTnt>)cohort).accept(tnt);game.items().academy().blastSource(tnt,trial);throw failure;});}catch(IllegalStateException expected){failed=expected==failure;}
        require(failed&&temporary[0]!=null&&!VanityBlastScene.<Map<Entity,Object>>get(second,"blastSources").containsKey(temporary[0])&&!VanityBlastScene.<Map<Entity,Object>>get(game.items().academy(),"blastSources").containsKey(temporary[0]),"failed native initializer cleans BOTH ledgers before propagating the same error");first.blastBoundary();}
    void verify(ServerPlayer gamma){verify(gamma,0);}
    void verify(ServerPlayer gamma,int extra){
        require(first.snapshot().equals(List.of(charge))&&charge.getOwner()==gamma&&charge.age==17&&charge.mode==0&&charge.lifetime==200&&charge.academyTrial,"native charge membership, owner and unsaved private parameters restored");
        require(second.snapshot().equals(List.of(rocket))&&rocket.getOwner()==gamma&&rocket.age==100&&rocket.flightAge==3&&rocket.upgraded&&rocket.aim.equals(new Vec3(.1,.2,.3)),"Exoperso private windup/flight/aim state restored");
        require(List.copyOf(VanityBlastScene.<ArrayDeque<Object>>get(first,"blasts")).equals(bursts)&&first.pendingBlasts()==5&&first.pendingCosmos()==1,"same FIFO series and nonresident cross-world cosmos request restore");
        for(var b:payloads)require(VanityBlastScene.<ServerPlayer>get(b,"owner")==gamma&&VanityBlastScene.<Integer>get(b,"next")==1&&VanityBlastScene.<Float>get(b,"radius")==.2f&&Arrays.equals(VanityBlastScene.<Vec3[]>get(b,"offsets"),new Vec3[]{Vec3.ZERO,new Vec3(.2,0,0),new Vec3(.4,0,0)}),"independent geometry/configuration/cursor and current burst author restored");
        require(VanityBlastScene.<Set<UUID>>get(cohort,"kills").equals(Set.of(progress))&&VanityBlastScene.<Set<UUID>>get(cohort,"requiredPlayers").equals(Set.of(base.owner.getUUID(),base.other.getUUID())),"accepted cohort and partial progress are independent of future mutations");
        var a=VanityBlastScene.<Map<Entity,Object>>get(second,"blastSources");var b=VanityBlastScene.<Map<Entity,Object>>get(game.items().academy(),"blastSources");require(anchor.getOwner()==gamma&&chain.getOwner()==gamma&&a.size()==2&&a.get(anchor)==cohort&&a.get(chain)==cohort&&b.get(anchor)==trial&&b.get(chain)==trial&&trial.targets.size()==9&&!trial.completed,"live/chain sources and queued initializers share ORIGINAL cohort/trial identities");
        require(cohortProbe.calls==0&&trialProbe.calls==0,"restoration never invokes a source initializer");
        var all=List.copyOf(VanityBlastScene.<ArrayDeque<Object>>get(first,"searches"));require(all.subList(1,all.size()-extra).equals(rays),"both rainbow requests and nuclear aim preserve queue order");for(var r:rainbow)require(VanityBlastScene.<Double>get(r,"next")==1,"per-cast sample state rewinds independently of finished terminal");for(var r:rays)require(VanityBlastScene.<ServerPlayer>get(r,"owner")==gamma&&!VanityBlastScene.<Boolean>get(r,"finished"),"replayable request follows current author and reopens exactly its saved terminal");
    }
    boolean ready(){return cohortProbe.calls==2&&trialProbe.calls==2&&rays.stream().allMatch(r->VanityBlastScene.<Boolean>get(r,"finished"));}
    void thawed(){require(ready()&&first.pendingBlasts()==1&&first.pendingCosmos()==1,"actual resumed native blast queue runs each saved offset exactly once; nonresident request remains queued");require(first.snapshot().stream().filter(c->c.mode==4).count()==1,"native nuclear aim creates exactly one falling500-block rocket after each replay");require(rocket.age>=102&&rocket.aim!=null&&charge.age>=19,"native windup/charge advance resumes from saved private ages");
        for(var r:rainbow)require(VanityBlastScene.<Double>get(r,"next")>1&&VanityBlastScene.<Double>get(r,"next")<10,"real resumed rainbow sampling continues from its saved sample");
        first.stop();second.stop();game.items().academy().blastFinished(anchor);game.items().academy().blastFinished(chain);
    }
    void mutate(){for(var b:payloads){VanityBlastScene.<Vec3[]>get(b,"offsets")[1]=new Vec3(900,900,900);set(b,"next",99);set(b,"radius",88f);}charge.age=900;charge.mode=8;charge.lifetime=1;charge.academyTrial=false;rocket.age=900;rocket.flightAge=900;rocket.upgraded=false;rocket.aim=Vec3.ZERO;VanityBlastScene.<Set<UUID>>get(cohort,"kills").clear();VanityBlastScene.<Set<UUID>>get(cohort,"requiredPlayers").clear();trial.targets.clear();trial.completed=true;cohortProbe.calls=99;trialProbe.calls=99;rainbow.forEach(r->set(r,"next",999d));}
    void pending(){require(first.count()==0&&second.count()==0&&first.pendingBlasts()==0&&trial.completed&&cohortProbe.calls==99,"offline burst owner defers ALL private installation, including shared initializer state");}
}
