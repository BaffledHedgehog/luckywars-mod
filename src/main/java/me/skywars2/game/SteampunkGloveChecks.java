package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Finite observations of the actual registered clock, plus native admission/attribute controls. */
public final class SteampunkGloveChecks {
    private SteampunkGloveChecks(){}
    private static Trial running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Steampunk glove: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-6,why+" actual="+actual+" expected="+expected);}
    private static final class Trial implements AutoCloseable {
        final GameController game;final ServerPlayer owner;final LbcSteampunkGlove service;
        final RegenerationBeltChecks.Snapshot saved;final ElementalGrimoireChecks.Fixture base;
        final RegenerationBeltChecks.ScheduledClient client;final Set<String> tags;
        final List<Marker> markers=new ArrayList<>();
        ItemStack glove;int updates,stage,elapsed,clock;boolean closed;
        Trial(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(owner!=null&&game.canUseGameplayItems(owner),"real active native participant");
            service=game.items().steampunkGlove();saved=new RegenerationBeltChecks.Snapshot(game,owner);tags=new HashSet<>(owner.entityTags());
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();client=new RegenerationBeltChecks.ScheduledClient(game,owner);
        }
        void initialize(){
            owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setShiftKeyDown(false);
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))owner.setItemSlot(slot,ItemStack.EMPTY);
            for(var tag:List.of("sin_greed_active","nomagic_active","time_stopped","strict_map_object","magic_academy","spectator","rcdenyalways"))owner.entityTags().remove(tag);
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;profile.lbcChallenge=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/steampunk_sword");
            require(loot.size()==1,"registered source native singleton leaf");glove=loot.getFirst();
            require(LbcFartus.data(glove).getIntOr("steampunk_sword",0)==1,"actual legacy item flag");
            owner.setItemSlot(EquipmentSlot.OFFHAND,glove.copy());owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);service.reset(owner);hold();updates=client.updates();stage=-1;
        }
        void hold(){owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);owner.setOnGround(true);owner.getFoodData().setFoodLevel(20);owner.getFoodData().setSaturation(5);owner.setSprinting(true);}
        int timer(){return game.items().mana().score(owner,"sprint_timer").get();}
        int cooldown(){return game.items().mana().score(owner,"sprint_cooldown").get();}
        void bonus(int ticks){
            var speed=owner.getAttribute(Attributes.MOVEMENT_SPEED);var damage=owner.getAttribute(Attributes.ATTACK_DAMAGE);
            var sm=speed.getModifier(LbcSteampunkGlove.SPEED);var dm=damage.getModifier(LbcSteampunkGlove.DAMAGE);
            require(sm!=null&&dm!=null,"native fixed attribute pair exists");near(sm.amount(),ticks*.05,"native speed modifier");near(dm.amount(),ticks*1.25,"native damage modifier");
            require(speed.getModifiers().stream().filter(m->m.id().equals(LbcSteampunkGlove.SPEED)).count()==1&&damage.getModifiers().stream().filter(m->m.id().equals(LbcSteampunkGlove.DAMAGE)).count()==1,"fixed IDs never accumulate");
            require(speed.getValue()>=ticks*.05,"actual native movement value includes bonus");require(damage.getValue()>=damage.getBaseValue()+ticks*1.25,"actual native attack damage includes bonus alongside held item modifiers");
        }
        void empty(){require(owner.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(LbcSteampunkGlove.SPEED)==null&&owner.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(LbcSteampunkGlove.DAMAGE)==null,"both native bonuses removed");}
        Marker jammer(String tag){var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add(tag);require(owner.level().addFreshEntity(marker),"real native jammer");markers.add(marker);game.items().magic().loaded(marker);return marker;}
        void policies(){
            service.reset(owner);owner.setSprinting(true);owner.setShiftKeyDown(false);owner.setItemSlot(EquipmentSlot.MAINHAND,glove.copy());
            game.items().mana().score(owner,"sprint_timer").set(83);service.tickPlayer(owner);require(timer()==1,"ready mid-sprint selection restarts canonical clock at1");bonus(1);
            var mag=jammer("stopper_magic");require(game.items().magic().beaconSuppressed(owner.position()),"actual MAG positive control");game.items().mana().score(owner,"sprint_timer").set(40);service.tickPlayer(owner);bonus(40);mag.discard();
            owner.entityTags().add("nomagic_active");game.items().mana().score(owner,"sprint_timer").set(41);service.tickPlayer(owner);bonus(41);owner.entityTags().remove("nomagic_active");
            try{for(int i=0;i<10&&!game.items().frost().isFrozen(owner);i++)game.items().frost().add(owner,null);require(game.items().actionsBlocked(owner),"actual native frozen action block");service.tickPlayer(owner);
                near(owner.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(LbcSteampunkGlove.SPEED).amount(),2.05,"blocked action retains previous movement modifier");
                near(owner.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(LbcSteampunkGlove.DAMAGE).amount(),51.25,"blocked action retains previous damage modifier");
            }finally{game.items().frost().clear(owner);owner.removeAllEffects();}
            owner.setShiftKeyDown(true);service.tickPlayer(owner);require(!owner.entityTags().contains("sprinted"),"source sneak removes sprinted tag");bonus(41);owner.setShiftKeyDown(false);service.tickPlayer(owner);bonus(41);
            owner.entityTags().add("sprinted");owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);game.items().mana().score(owner,"sprint_timer").set(90);service.tickPlayer(owner);bonus(41);owner.setItemSlot(EquipmentSlot.MAINHAND,glove.copy());
            game.items().timeStop().begin(owner.level(),20);int before=timer(),cd=cooldown();service.tickPlayer(owner);service.secondPlayer(owner);require(timer()==before&&cooldown()==cd,"actual dimension Time Stop pauses updates and recharge");bonus(41);game.items().timeStop().end(owner.level());
            owner.entityTags().add("time_stopped");service.tickPlayer(owner);bonus(41);owner.entityTags().remove("time_stopped");
            var tech=jammer("stopper");require(game.items().magic().mechanismSuppressed(owner),"actual TECH positive control");
            owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);int jamTimer=timer(),jamCooldown=cooldown();service.tickPlayer(owner);empty();
            require(!owner.entityTags().contains("sprinted")&&timer()==jamTimer&&cooldown()==jamCooldown,"TECH cancels switched-away bonus/tag without changing shared clock or stopped cooldown");
            tech.discard();owner.setItemSlot(EquipmentSlot.MAINHAND,glove.copy());service.tickPlayer(owner);empty();require(!owner.entityTags().contains("sprinted"),"leaving TECH cannot revive canceled ramp during cooldown");
            service.reset(owner);service.tickPlayer(owner);bonus(1);var selectedTech=jammer("stopper");service.tickPlayer(owner);empty();require(!owner.entityTags().contains("sprinted")&&cooldown()==5,"TECH also immediately cancels selected glove and retains stopped5 recharge");selectedTech.discard();
            owner.setGameMode(GameType.SPECTATOR);service.tickPlayer(owner);empty();require(cooldown()==0,"role loss resets cooldown");owner.setGameMode(GameType.SURVIVAL);
            owner.entityTags().remove("sprinted");game.items().mana().score(owner,"sprint_timer").set(90);game.items().mana().score(owner,"sprint_cooldown").set(3);service.tickPlayer(owner);empty();require(!owner.entityTags().contains("sprinted"),"cooldown blocks a fresh sprint");
            service.clear(owner);empty();require(timer()==0&&cooldown()==3,"clear removes ramp while preserving source cooldown");
            owner.setSprinting(false);for(int i=0;i<3;i++)service.secondPlayer(owner);require(cooldown()==0,"three actual stopped one-second callbacks recharge remaining3");
            owner.setSprinting(true);service.tickPlayer(owner);bonus(1);service.stop();empty();require(timer()==0&&cooldown()==0&&!owner.entityTags().contains("sprinted"),"global stop clears canonical state");
        }
        boolean step(){
            client.acknowledge();int now=client.updates();if(now==updates)return false;require(now==updates+1,"one native connection update per observation");updates=now;
            switch(stage){
                case -1->{service.reset(owner);hold();stage=0;}
                case 0->{require(timer()==1,"one existing shared clock increment with offhand glove actual="+timer()+" sprint="+owner.isSprinting()+" tags="+owner.entityTags());empty();owner.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);owner.setItemSlot(EquipmentSlot.MAINHAND,glove.copy());hold();stage=1;}
                case 1->{require(timer()==1&&cooldown()==5&&owner.entityTags().contains("sprinted"),"actual selected fresh sprint starts1/cooldown5");bonus(1);elapsed=1;hold();stage=2;}
                case 2->{elapsed++;require(timer()==elapsed,"one shared timer increment per full native tick");bonus(elapsed);require(cooldown()==5,"successful ramp refresh retains cooldown5");hold();if(elapsed==200){stage=3;elapsed=0;}}
                case 3->{elapsed++;require(timer()==200+elapsed,"shared timer continues past ceiling");bonus(200);require(cooldown()==5,"running never recharges");if(elapsed<25){hold();return false;}owner.setSprinting(false);clock=game.server().getTickCount();stage=4;}
                case 4->{empty();require(timer()==0,"actual shared sprint-end resets clock");int seconds=game.server().getTickCount()/20-clock/20;require(cooldown()==Math.max(0,5-seconds),"stopped cooldown follows shared global seconds");if(cooldown()>0)return false;policies();return true;}
                default->throw new IllegalStateException("glove fixture stage");
            }
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{game.items().timeStop().end(owner.level());service.reset(owner);for(var marker:markers){game.items().magic().unloaded(marker);marker.discard();}},client::close,base::close,()->saved.restore(game),saved::restoreAttributes,saved::restoreUse,()->saved.restoreProfile(game),()->{owner.entityTags().clear();owner.entityTags().addAll(tags);game.players().save();}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("glove fixture restoration",failure);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive finite trial");var trial=new Trial(game);try{trial.initialize();running=trial;SkyWars2.LOGGER.info("[check] steampunk-glove running (native full200 sprint and stopped cooldown)");}catch(Throwable error){try{trial.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS steampunk-glove (full200 native ramp/fixed IDs/actual values, retained ceiling, global stopped5 cooldown, selected hand, source tags, TECH/MAG/artifact/Time Stop/role, clear/stop)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL steampunk-glove",error);}}
}
