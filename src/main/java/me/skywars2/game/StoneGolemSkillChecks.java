package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Actual server seconds, native resistance/shield statistics and full coordinator replay. */
public final class StoneGolemSkillChecks {
    private StoneGolemSkillChecks(){}
    private static Run running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Stone Golem: "+why);}
    public static void start(GameController game){
        require(running==null,"exclusive fixture");var r=new Run(game);
        try{r.init();running=r;}catch(RuntimeException|Error failure){r.close();throw failure;}
        SkyWars2.LOGGER.info("[check] START stone-golem (actual source seconds; approximately60 seconds)");
    }
    public static void tick(GameController game){
        if(running==null)return;var r=running;
        try{if(r.step()){running=null;r.close();SkyWars2.LOGGER.info("[check] PASS stone-golem (real10-second pulses/cap5, native taken and ResistanceV/III/IV statistics versus native shield, nested attempt isolation, TimeStop and nonempty full coordinator phase replay, foreign resistance expiry ownership and skill cleanup)");}}
        catch(RuntimeException|Error failure){running=null;try{r.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL stone-golem",failure);}
    }
    private static final class Run implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture f;final LbcStoneGolemSkill skill;
        final Map<Stat<?>,Integer> statistics=new LinkedHashMap<>();
        RegenerationBeltChecks.ScheduledClient client,audience;
        int stage,at,start,savedLayers,savedCharge,savedPhase,previousLayers;
        boolean closed;
        Run(GameController game){
            this.game=game;require(!game.vanity().busy()&&!game.vanity().hasSnapshot(),"empty whole coordinator");
            f=new ChefChecks.Fixture(game);skill=game.items().stoneGolemSkill();
            for(var key:List.of(Stats.DAMAGE_TAKEN,Stats.DAMAGE_RESISTED,Stats.DAMAGE_BLOCKED_BY_SHIELD)){
                var stat=Stats.CUSTOM.get(key);statistics.put(stat,f.owner.getStats().getValue(stat));
            }
        }
        void init(){
            f.init();skill.clear(f.owner);f.move(f.owner,f.at);f.move(f.other,f.at.add(0,0,2));
            client=new RegenerationBeltChecks.ScheduledClient(game,f.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,f.other);
            for(var p:List.of(f.owner,f.other)){p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);}
            game.config().lbcSkills=true;game.config().ranked=true;game.players().get(f.owner.getUUID()).lbcSkill=34;
            start=game.server().getTickCount();begin(0);
        }
        void begin(int next){stage=next;at=game.server().getTickCount();}
        int stat(net.minecraft.resources.Identifier key){return f.owner.getStats().getValue(Stats.CUSTOM.get(key));}
        void ready(){client.acknowledge();f.owner.setHealth(100);f.owner.setAbsorptionAmount(0);f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;}
        void hit(float damage){ready();f.owner.hurtServer(f.owner.level(),f.owner.damageSources().playerAttack(f.other),damage);}
        void resistance(int amplifier){f.owner.removeEffect(MobEffects.RESISTANCE);f.owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,200,amplifier,false,false));}
        boolean step(){
            require(game.server().getTickCount()-start<1900,"bounded actual scheduler at stage "+stage);
            if(game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())
                if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-at;
            switch(stage){
                case 0->{
                    if(skill.layers(f.owner)==0){require(elapsed<201,"first layer by200 active ticks");require(skill.charge(f.owner)<=9,"ten distinct one-second pulses");return false;}
                    require(skill.layers(f.owner)==1&&elapsed>=199&&elapsed<=201,"exact10 real server seconds for first layer");
                    begin(1);
                }
                case 1->{
                    if(skill.charge(f.owner)<2)return false;
                    require(skill.layers(f.owner)==1,"nonempty partial recharge");
                    savedLayers=skill.layers(f.owner);savedCharge=skill.charge(f.owner);savedPhase=skill.phase(f.owner.level());
                    f.block(f.center.offset(-4,0,-4),me.skywars2.game.LbcMagic.MAGIC_BEACON_BLOCK);require(game.items().magic().suppressed(f.owner),"actual MAG field at Golem");begin(9);
                }
                case 9->{
                    require(skill.layers(f.owner)==savedLayers&&skill.charge(f.owner)==savedCharge,"MAG blocks recharge and preserves existing layers");if(elapsed<22)return false;
                    f.block(f.center.offset(-4,0,-4),net.minecraft.world.level.block.Blocks.AIR);require(!game.items().magic().suppressed(f.owner),"Golem MAG field removed");savedPhase=skill.phase(f.owner.level());
                    game.items().timeStop().begin(f.owner.level(),500);begin(2);
                }
                case 2->{
                    require(skill.layers(f.owner)==savedLayers&&skill.charge(f.owner)==savedCharge&&skill.phase(f.owner.level())==savedPhase,"TimeStop freezes per-world phase and charge");
                    if(elapsed<22)return false;
                    require(game.vanity().requestCapture(f.owner,false),"actual full coordinator capture with nonempty stone state");begin(3);
                }
                case 3->{
                    if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                    skill.clear(f.owner);require(skill.layers(f.owner)==0,"post-capture native state mutation");
                    require(game.vanity().requestRestoreForCheck(),"actual full coordinator restore");begin(4);
                }
                case 4->{
                    if(game.vanity().busy()||!game.vanity().status().equals("IDLE"))return false;
                    require(skill.layers(f.owner)==savedLayers&&skill.charge(f.owner)==savedCharge&&skill.phase(f.owner.level())==savedPhase,"full coordinator restores exact layers/recharge and second phase");
                    client.acknowledge();audience.acknowledge();game.vanity().stop();game.items().timeStop().end(f.owner.level());
                    hit(1);require(skill.layers(f.owner)==1&&skill.charge(f.owner)==0,"native subthreshold incoming resets recharge");
                    previousLayers=1;begin(5);
                }
                case 5->{
                    int layers=skill.layers(f.owner);require(layers<=5,"source cap5");
                    if(layers!=previousLayers){require(layers==previousLayers+1,"one layer per source recharge");previousLayers=layers;}
                    if(layers<5)return false;
                    require(skill.charge(f.owner)==0,"cap reaches zero recharge");
                    var effect=f.owner.getEffect(MobEffects.RESISTANCE);
                    require(effect!=null&&effect.getAmplifier()==4&&!effect.isVisible(),"native hidden ResistanceV refresh");begin(6);
                }
                case 6->{
                    require(skill.layers(f.owner)==5&&skill.charge(f.owner)==0,"cap stops recharge");
                    if(elapsed<22)return false;
                    // ResistanceV genuinely awards resisted statistics despite zero HP damage.
                    ready();int before=stat(Stats.DAMAGE_RESISTED);float hp=f.owner.getHealth();
                    for(int attempt=0;attempt<64&&skill.layers(f.owner)==5;attempt++)hit(20);
                    require(stat(Stats.DAMAGE_RESISTED)>before&&f.owner.getHealth()==hp,"actual native resistance-only events");
                    require(skill.layers(f.owner)==4,"bounded50percent source removal eventually reaches4");
                    resistance(3);before=stat(Stats.DAMAGE_RESISTED);hit(12.5f);
                    require(stat(Stats.DAMAGE_RESISTED)-before==100&&skill.layers(f.owner)==3,"native ResistanceIV10HP resisted removes exactly one layer");
                    f.owner.removeEffect(MobEffects.RESISTANCE);f.owner.setYRot(0);f.owner.setXRot(0);
                    f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));f.owner.startUsingItem(InteractionHand.OFF_HAND);begin(7);
                }
                case 7->{
                    if(elapsed<8)return false;
                    f.owner.removeEffect(MobEffects.RESISTANCE);
                    int before=stat(Stats.DAMAGE_BLOCKED_BY_SHIELD),resisted=stat(Stats.DAMAGE_RESISTED);hit(12);
                    require(stat(Stats.DAMAGE_BLOCKED_BY_SHIELD)>before&&stat(Stats.DAMAGE_RESISTED)==resisted,"actual front-facing shield has shield stat and zero resistance stat");
                    require(skill.layers(f.owner)==3,"shield blocking never removes layer3");
                    f.owner.stopUsingItem();f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                    resistance(2);before=stat(Stats.DAMAGE_RESISTED);hit(40f/3f);
                    require(stat(Stats.DAMAGE_RESISTED)-before==80&&skill.layers(f.owner)==2,"native ResistanceIII8HP resisted removes exactly one layer");
                    f.owner.removeEffect(MobEffects.RESISTANCE);int threshold=skill.secondLayerThresholdHP();
                    hit(threshold-0.1f);require(skill.layers(f.owner)==2,"layer2 threshold lower boundary uses actual single attempt");
                    hit(threshold);require(skill.layers(f.owner)==1,"author-selected layer2 HP threshold exact boundary");
                    scopedStatistics();
                    hit(7.9f);require(skill.layers(f.owner)==1,"layer1 lower threshold never accumulates prior damage");
                    hit(8);require(skill.layers(f.owner)==0,"native8HP taken removes layer1");
                    resistance(4);skill.clear(f.owner);require(f.owner.getEffect(MobEffects.RESISTANCE).getAmplifier()==4,"clear never strips foreign resistance");
                    game.players().get(f.owner.getUUID()).lbcSkill=0;begin(8);
                }
                case 8->{if(elapsed<2)return false;require(skill.layers(f.owner)==0&&skill.charge(f.owner)==0,"skill-loss lifecycle clears state");return true;}
                default->throw new IllegalStateException("Unknown Stone stage "+stage);
            }
            return false;
        }
        void scopedStatistics(){
            var source=f.owner.damageSources().generic();
            var point=skill.checkpoint();
            // Supplement native gameplay evidence with a nested same-player isolation boundary.
            skill.withIncomingAttempt(f.owner,()->{
                skill.statistic(f.owner,Stats.CUSTOM.get(Stats.DAMAGE_TAKEN),80);
                skill.withIncomingAttempt(f.owner,()->{skill.statistic(f.owner,Stats.CUSTOM.get(Stats.DAMAGE_RESISTED),100);skill.incoming(f.owner,source,10,0,false);return true;});
                require(skill.layers(f.owner)==1,"nested matching player owns only its own raw statistics");
                boolean rejected=false;try{skill.checkpoint();}catch(IllegalStateException expected){rejected=true;}
                require(rejected,"snapshot cannot capture an in-flight native attempt");
                skill.incoming(f.owner,source,8,8,false);require(skill.layers(f.owner)==0,"outer award survives nested callback");
                skill.incoming(f.owner,source,8,8,false);require(skill.layers(f.owner)==0,"same attempt handled at most once");return true;
            });
            skill.prepareFrozen();var plan=point.prepare(id->game.server().getPlayerList().getPlayer(id));
            require(plan.pending().isEmpty(),"resident scalar checkpoint lookup");plan.install().run();skill.publishFrozen();
            require(skill.layers(f.owner)==1,"supplemental checkpoint restores native boundary fixture");
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->game.vanity().stop(),()->game.items().timeStop().end(f.owner.level()),
                    ()->skill.clear(f.owner),()->{if(client!=null)client.close();if(audience!=null)audience.close();},f::close,
                    ()->statistics.forEach((stat,value)->f.owner.getStats().setValue(f.owner,stat,value))))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Stone scoped restoration",failure);
        }
    }
}
