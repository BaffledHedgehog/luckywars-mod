package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Actual authority tick, native inventory/components/effects, and the existing mutable counter owners. */
public final class WitchEnvyChecks {
    private WitchEnvyChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Witch Envy: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,donor;final ShishEncounterChecks.State saved,otherSaved;
        final SauvojenMeditationChecks.Attribution attribution;ShishEncounterChecks.Client client,other;
        final Vec3 origin;final net.minecraft.server.level.ServerLevel level;int phase,at;boolean closed;ItemStack copied,kept;
        Fixture(GameController game){this.game=game;
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            donor=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            require(game.playing()&&owner.level()==donor.level()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(donor),"actual active participants");
            require(game.items().witchPassives().count()==0&&!game.items().timeStop().stopped(owner.level()),"no borrowed authority/stop");
            require(game.items().psycho().effectCount()==0&&game.items().electro().shockCount()==0&&game.items().shooters().burstCount()==0,"exclusive existing status/queue owners");
            for(var p:List.of(owner,donor))require(game.items().electro().charges(p)==0&&game.items().electro().charging(p)==0
                    &&game.items().electro().yellowCharges(p)==0&&game.items().electro().yellowCharging(p)==0&&game.items().terra().charges(p)==0
                    &&game.items().mobgun().cooldown(p)==0&&game.items().timeStop().cooldownTicks(p)==0&&game.items().reactive().cooldownTicks(p)==0
                    &&game.items().shooters().cooldownRemaining(p)==0,"no borrowed native charges/cooldowns");
            saved=new ShishEncounterChecks.State(game,owner);otherSaved=new ShishEncounterChecks.State(game,donor);
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,donor));level=owner.level();origin=new Vec3(owner.getX(),240,owner.getZ());
        }
        void initialize(){
            for(var p:List.of(owner,donor)){
                require(p.entityTags().stream().noneMatch(t->t.startsWith("sin_")),"no borrowed Witch authority");
                p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;
                p.entityTags().removeAll(Set.of("spectator","time_stopped","sin_greed_active","nomagic_active","strict_map_object","magic_academy","rcdenyalways"));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);
                game.items().magic().inventoryChanged(p);
            }
            game.items().magic().flushCleansing();move(owner,origin);move(donor,origin.add(2,0,0));
            client=new ShishEncounterChecks.Client(game,owner);other=new ShishEncounterChecks.Client(game,donor);
            counters();
            kept=new ItemStack(Items.STONE,13);owner.getInventory().setItem(0,kept);
            copied=new ItemStack(Items.DIAMOND,7);copied.set(DataComponents.CUSTOM_NAME,Component.literal("Envy native payload"));
            var data=new CompoundTag();data.putInt("envy_fixture",347);copied.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            donor.getInventory().setItem(0,new ItemStack(Items.GOLD_INGOT,2));donor.getInventory().setItem(9,copied);
            donor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TORCH,17));donor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            game.items().mana().score(owner,"nitro").set(2);game.items().mana().score(donor,"nitro").set(17);
            game.items().mana().score(owner,"coldown8").set(18);game.items().mana().score(donor,"coldown8").set(3);
            donor.addEffect(ItemEffectLevels.create(MobEffects.SPEED,200,0),donor);
            donor.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,200,0),donor);
            donor.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,200,0),donor);
            donor.addEffect(ItemEffectLevels.create(MobEffects.NIGHT_VISION,800,0),donor);
            owner.addEffect(ItemEffectLevels.create(MobEffects.WATER_BREATHING,400,0),owner);
            owner.addEffect(ItemEffectLevels.create(MobEffects.UNLUCK,400,0),owner);
            owner.entityTags().add("sin_envy");at=game.server().getTickCount();
        }
        void move(ServerPlayer p,Vec3 v){require(p.teleportTo(p.level(),v.x,v.y,v.z,Set.of(),p.getYRot(),p.getXRot(),true),"native relocation");
            if(p==owner&&client!=null)client.ack();if(p==donor&&other!=null)other.ack();}
        void counters(){
            var c=new LbcSourceCounters(game);var e=game.items().electro();
            e.sourceCharge(owner,false,false,2);e.sourceCharge(owner,false,true,7);
            e.sourceCharge(donor,false,false,9);e.sourceCharge(donor,false,true,4);
            require(c.copyBetter(owner,donor,"swordcharged1",true)&&e.charges(owner)==9&&e.charging(owner)==7,"live charge increases independently of progress");
            require(!c.copyBetter(owner,donor,"swordcharging1",true)&&e.charging(owner)==7,"progress never decreases on MAX");
            e.sourceCharge(donor,true,false,12);e.sourceCharge(donor,true,true,6);
            require(c.copyBetter(owner,donor,"swordcharged5",true)&&c.copyBetter(owner,donor,"swordcharging5",true)
                    &&e.yellowCharges(owner)==12&&e.yellowCharging(owner)==6&&e.charges(owner)==9,"yellow/cyber counters have separate native owners");
            game.items().terra().sourceCharges(donor,3);
            require(c.copyBetter(owner,donor,"swordcharged2",true)&&game.items().terra().charges(owner)==3,"actual Terra charge owner");
            game.items().psycho().afflict(owner,donor,80,70,60,55);game.items().psycho().afflict(donor,donor,3,4,5,0);
            for(var name:List.of("scared","lookback","twitching"))require(c.copyBetter(owner,donor,name,false)
                    &&game.items().psycho().hudValue(owner,name)==game.items().psycho().hudValue(donor,name),"live psychological reduction "+name);
            require(game.items().psycho().hudValue(owner,"piss")==55,"unlisted affliction survives comparisons");
            require(e.shock(owner,donor,30)&&e.shock(donor,donor,10),"native shock producers");
            require(c.copyBetter(owner,donor,"shock",false)&&e.shocks(owner)==10,"live shock reduction without new dose");
            game.items().psycho().clearEffects(donor);e.clearEffects(donor);
            for(var name:List.of("scared","lookback","twitching","shock"))require(c.copyBetter(owner,donor,name,false)&&c.read(owner,name)==0,"zero clears actual owner "+name);
            require(game.items().psycho().hudValue(owner,"piss")==55,"zero status cleanup retains unrelated affliction");game.items().psycho().clearEffects(owner);
            game.items().shooters().addPlayerCounters(owner);game.items().timeStop().addPlayerCounters(owner);game.items().mobgun().addCounters(owner);
            game.items().reactive().setShishCounter(owner,34);game.items().reactive().setShishCounter(donor,2);
            for(var name:List.of("coldown1","coldown16","coldown17","coldown25","coldownMagnum"))require(c.copyBetter(owner,donor,name,false)&&c.read(owner,name)==0,"actual cooldown cleared "+name);
            require(c.copyBetter(owner,donor,"coldownshish",false)&&game.items().reactive().cooldownTicks(owner)==10,"source5tick units update real Reactive timer");
            game.items().shooters().forget(owner);game.items().reactive().reduceSourceCooldown(owner,0);game.items().reactive().reduceSourceCooldown(donor,0);
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.PINE));
            owner.getInventory().setItem(9,new ItemStack(Items.WHEAT_SEEDS));
            require(game.items().shooters().use(owner,InteractionHand.MAIN_HAND)&&owner.getInventory().getItem(9).isEmpty()
                    &&c.read(owner,"coldown18")==20&&c.read(owner,"coldown18b")==-4,"actual paid ordinary Pine queue and separate ammo-type counters");
            require(c.copyBetter(owner,donor,"coldown18",false)&&game.items().shooters().burstCount()==0
                    &&game.items().shooters().arrowCount()==0,"Envy cancels existing paid Pine queue without replay, extra shots or ammo refund");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.PHANTOM_KNIVES));
            require(game.items().shooters().use(owner,InteractionHand.MAIN_HAND)&&c.read(owner,"coldown13")==25,"actual native five-wave knife queue");
            require(c.copyBetter(owner,donor,"coldown13",false)&&game.items().shooters().phantomBurstCount()==0
                    &&game.items().shooters().phantomArrowCount()==0,"Envy shortens the same knife clock without rearming any waves");
            game.items().shooters().forget(owner);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            // Genuine scoreboard operands retain native missing-score comparison semantics.
            var obj=game.items().mana().score(owner,"swordcharging7");obj.set(8);
            var objective=game.server().getScoreboard().getObjective("swordcharging7");game.server().getScoreboard().resetSinglePlayerScore(donor,objective);
            require(!c.copyBetter(owner,donor,"swordcharging7",true)&&obj.get()==8,"missing donor scoreboard operand is not fabricated");
            e.forget(owner);e.forget(donor);game.items().terra().sourceCharges(owner,0);game.items().terra().sourceCharges(donor,0);
        }
        boolean step(){int now=game.server().getTickCount();if(client!=null){client.ack();other.ack();}
            require(now-at<60,"bounded actual scheduler deadline phase="+phase);
            switch(phase){
                case 0->{if(now==at)return false;
                    require(owner.getInventory().getItem(0)==kept&&kept.getCount()==13,"occupied hotbar slot retained by identity");
                    var got=owner.getInventory().getItem(9);require(got!=copied&&got.getCount()==7&&ItemStack.isSameItemSameComponents(got,copied),"empty inventory slot receives independent full component/count copy");
                    require(donor.getInventory().getItem(9)==copied&&copied.getCount()==7,"donor is not debited or replaced");
                    require(owner.getOffhandItem().is(Items.TORCH)&&owner.getOffhandItem().getCount()==17&&owner.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET),"native offhand and equipment slots copied");
                    require(game.items().mana().hudValue(owner,"nitro")==17&&game.items().mana().hudValue(owner,"coldown8")<=3,"real authority tick applies MAX and MIN comparisons");
                    phase=1;return false;}
                case 1->{if(!owner.hasEffect(MobEffects.NIGHT_VISION))return false;
                    require(owner.getEffect(MobEffects.SPEED).getAmplifier()==2,"fixed source SpeedIII rather than donor level");
                    require(owner.getEffect(MobEffects.REGENERATION).getAmplifier()==5,"source high Regen uses shared item cap");
                    require(owner.getEffect(MobEffects.INVISIBILITY).getAmplifier()==1&&!owner.getEffect(MobEffects.INVISIBILITY).isVisible(),"source hidden InvisibilityII");
                    require(owner.getEffect(MobEffects.NIGHT_VISION).getDuration()>=495&&!owner.hasEffect(MobEffects.WATER_BREATHING)&&!owner.hasEffect(MobEffects.UNLUCK),"natural second pulse fixed duration and clear-unless-donor branches");
                    owner.entityTags().remove("sin_envy");owner.removeAllEffects();donor.removeAllEffects();
                    // Unbounded native @p is global, unlike a selector with distance/AABB arguments.
                    var nether=Objects.requireNonNull(game.server().getLevel(Level.NETHER));
                    require(owner.teleportTo(nether,origin.x,origin.y,origin.z,Set.of(),0,0,true),"native foreign dimension");client.ack();
                    owner.getInventory().setItem(9,ItemStack.EMPTY);game.items().mana().score(owner,"nitro").set(2);owner.entityTags().add("sin_envy");
                    phase=2;at=now;return false;}
                case 2->{if(now-at<2)return false;
                    require(ItemStack.isSameItemSameComponents(owner.getInventory().getItem(9),copied)&&game.items().mana().hudValue(owner,"nitro")==17,"literal native nearest-player selector includes donor across dimensions");
                    return true;}
                default->throw new IllegalStateException("phase "+phase);
            }
        }
        public void close(){if(closed)return;closed=true;try{
            owner.entityTags().remove("sin_envy");game.items().psycho().clearEffects(owner);game.items().psycho().clearEffects(donor);
            game.items().electro().forget(owner);game.items().electro().forget(donor);game.items().shooters().forget(owner);game.items().shooters().forget(donor);
            game.items().terra().sourceCharges(owner,0);game.items().terra().sourceCharges(donor,0);
            game.items().timeStop().reduceSourceCooldown(owner,0);game.items().mobgun().clearCooldown(owner);
            game.items().reactive().reduceSourceCooldown(owner,0);game.items().reactive().reduceSourceCooldown(donor,0);
        }finally{try{if(owner.level()!=level)owner.teleportTo(level,saved.state.position.x,saved.state.position.y,saved.state.position.z,Set.of(),saved.state.yaw,saved.state.pitch,true);
                saved.restore(game);otherSaved.restore(game);if(client!=null){client.ack();other.ack();}attribution.close();}
            finally{if(client!=null)client.close();if(other!=null)other.close();}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.initialize();}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}
        SkyWars2.LOGGER.info("[check] START witch-envy (actual native authority tick and existing state owners)");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS witch-envy (live charge/status/cooldown owners, missing scoreboard operands, source MAX/MIN, native empty-slot components/equipment, donor preservation, real second pulse/item caps and global native nearest donor; no complete Witch qualification)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL witch-envy",error);}}
}
