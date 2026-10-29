package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Actual raw use dispatch, native source selectors/loot/clear order and real pause boundaries. */
public final class WitchClaimChecks {
    private WitchClaimChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Witch acquisition: "+why);}
    private static final List<String> SCORES=List.of("gens_type","food_count","dmg_dealt","lolidefecation","greed_prestige","sleep_time");
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcWitchClaims service;
        final ShishEncounterChecks.Client client,otherClient;
        final Map<ServerPlayer,Set<String>> tags=new IdentityHashMap<>();
        final Map<ServerPlayer,Map<String,Integer>> scores=new IdentityHashMap<>();
        final List<Entity> own=new ArrayList<>();final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<String,ItemStack> rewards=new HashMap<>();ItemStack token;Entity magic,tech;int start,phase,mana,mode;boolean stopped,closed,forced;
        net.minecraft.server.level.ServerLevel foreign;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);service=game.items().witchClaims();
            require(game.server().getPlayerList().getPlayerCount()==2&&game.items().indolenceFingers().snapshot().isEmpty(),"two exclusive participants/no borrowed fingers");
            client=new ShishEncounterChecks.Client(game,base.owner);otherClient=new ShishEncounterChecks.Client(game,base.other);
            for(var p:List.of(base.owner,base.other)){tags.put(p,new HashSet<>(p.entityTags()));var saved=new HashMap<String,Integer>();for(var name:SCORES)saved.put(name,new LbcSourceCounters(game).read(p,name));scores.put(p,saved);}
            for(var world:game.server().getAllLevels())world.getAllEntities().forEach(before::add);
        }
        void init(){base.init();base.move(base.owner,base.at);base.move(base.other,base.at.add(12,0,0));game.config().offhandWands=true;
            token=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:witch_gens").stream().filter(LbcWitchClaims::matches).findFirst().orElseThrow();
            require(token.is(Items.CARROT_ON_A_STICK)&&token.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:witch_gens"))
                    &&token.get(DataComponents.TOOLTIP_STYLE).equals(Identifier.parse("lbc:tier_q"))&&LbcFartus.data(token).getIntOr("magicwand",0)==1
                    &&token.get(DataComponents.LORE).lines().size()==8,"real packaged source root payload, model/tier/eight lore rows");
            for(int i=0;i<24;i++){var e=new Marker(EntityTypes.MARKER,base.owner.level());e.setPos(base.at.add(i%6-3,2,i/6-2));require(base.owner.level().addFreshEntity(e),"native nonplayer source positions");own.add(e);}
            reset();start=game.server().getTickCount();
        }
        void reset(){for(var p:List.of(base.owner,base.other)){for(var mode:LbcWitchClaims.Mode.values())p.entityTags().remove(mode.tag());p.entityTags().removeAll(Set.of("sin_greed_active","indolence_active","nomagic_active","greed_cursed","have_mom","owner"));}
            base.owner.removeAllEffects();base.owner.getInventory().clearContent();base.owner.inventoryMenu.getCraftSlots().clearContent();base.owner.containerMenu.setCarried(ItemStack.EMPTY);
            base.owner.getInventory().setSelectedSlot(5);base.owner.setShiftKeyDown(false);base.owner.setItemInHand(InteractionHand.MAIN_HAND,token.copyWithCount(3));
            for(var name:SCORES)game.items().mana().score(base.owner,name).set(0);game.items().magic().inventoryChanged(base.owner);}
        void set(ServerPlayer p,String key,int n){game.items().mana().score(p,key).set(n);}
        void mode(int mode){set(base.owner,"gens_type",mode);}
        int mode(){return new LbcSourceCounters(game).read(base.owner,"gens_type");}
        boolean input(){return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(base.owner,base.owner.level(),InteractionHand.MAIN_HAND).consumesAction();}
        static List<?> packets(ShishEncounterChecks.Client client){try{Object wire=client.wire;var f=wire.getClass().getDeclaredField("packets");f.setAccessible(true);return (List<?>)f.get(wire);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        static long failureSounds(ShishEncounterChecks.Client client){return packets(client).stream().filter(p->p instanceof ClientboundSoundPacket s&&s.getSound().value().location().toString().equals("minecraft:entity.wither.ambient")).count();}
        void conditions(){
            for(var m:List.of(LbcWitchClaims.Mode.GLUTTONY,LbcWitchClaims.Mode.WRATH,LbcWitchClaims.Mode.LUST,LbcWitchClaims.Mode.INDOLENCE)){
                reset();set(base.owner,m.counter,m.minimum-1);require(service.verdict(base.owner,m)==LbcWitchClaims.Verdict.UNQUALIFIED,"exact lower threshold "+m);
                set(base.owner,m.counter,m.minimum);require(service.verdict(base.owner,m)==LbcWitchClaims.Verdict.QUALIFIED,"inclusive minimum without incumbent "+m);
                base.other.entityTags().add(m.tag());set(base.other,m.counter,m.minimum);require(service.verdict(base.owner,m)==LbcWitchClaims.Verdict.UNQUALIFIED,"ties cannot replace incumbent "+m);
                set(base.owner,m.counter,m.minimum+1);require(service.verdict(base.owner,m)==LbcWitchClaims.Verdict.QUALIFIED,"strict incumbent comparison "+m);
                base.other.setGameMode(GameType.SPECTATOR);set(base.owner,m.counter,m.minimum);require(service.verdict(base.owner,m)==LbcWitchClaims.Verdict.QUALIFIED,"native spectator incumbent is ignored "+m);base.other.setGameMode(GameType.SURVIVAL);
                base.owner.entityTags().add(m.tag());require(service.verdict(base.owner,m)==LbcWitchClaims.Verdict.ALREADY,"already-held authority does not acquire again "+m);
            }
            reset();var greed=LbcWitchClaims.Mode.GREED;require(service.verdict(base.owner,greed)==LbcWitchClaims.Verdict.UNQUALIFIED,"Greed curse/token mandatory");
            var item=new ItemStack(Items.STONE);var flag=new net.minecraft.nbt.CompoundTag();flag.putInt("greed_greed",1);item.set(DataComponents.CUSTOM_DATA,CustomData.of(flag));base.owner.setItemSlot(EquipmentSlot.HEAD,item);base.owner.entityTags().add("greed_cursed");
            require(service.verdict(base.owner,greed)==LbcWitchClaims.Verdict.QUALIFIED,"raw source Inventory includes equipment token");base.other.entityTags().add(greed.tag());set(base.other,greed.counter,1);
            require(service.verdict(base.owner,greed)==LbcWitchClaims.Verdict.UNQUALIFIED,"Greed strict prestige comparison");set(base.owner,greed.counter,2);require(service.verdict(base.owner,greed)==LbcWitchClaims.Verdict.QUALIFIED,"Greed prestige success");
            reset();var envy=LbcWitchClaims.Mode.ENVY;require(service.verdict(base.owner,envy)==LbcWitchClaims.Verdict.UNQUALIFIED,"Envy requires every other eligible player to have_mom");
            base.other.entityTags().add("have_mom");require(service.verdict(base.owner,envy)==LbcWitchClaims.Verdict.QUALIFIED,"all others match");base.owner.entityTags().add("have_mom");require(service.verdict(base.owner,envy)==LbcWitchClaims.Verdict.UNQUALIFIED,"caster's have_mom forbids Envy");
            base.owner.entityTags().remove("have_mom");base.other.entityTags().remove("have_mom");base.other.setGameMode(GameType.SPECTATOR);require(service.verdict(base.owner,envy)==LbcWitchClaims.Verdict.QUALIFIED,"zero eligible others vacuously succeeds");base.other.setGameMode(GameType.SURVIVAL);
            reset();require(service.verdict(base.owner,LbcWitchClaims.Mode.VANITY)==LbcWitchClaims.Verdict.UNQUALIFIED,"Vanity reads actual early match/lives rather than a fabricated old scoreboard");
        }
        void inputs(){reset();var objective=game.server().getScoreboard().getObjective("gens_type");game.server().getScoreboard().resetSinglePlayerScore(base.owner,objective);
            require(!input()&&!base.owner.hasEffect(MobEffects.WITHER)&&base.owner.getMainHandItem().getCount()==3,"unset nonsneak mode has no branch or punishment");
            base.owner.setShiftKeyDown(true);require(input()&&mode()==1,"missing score increments from0 to1");mode(7);require(input()&&mode()==1,"source seven-mode wrap");
            base.owner.setItemInHand(InteractionHand.OFF_HAND,token.copy());require(input()&&mode()==3,"each hand independently switches the one source mode");game.config().offhandWands=false;require(input()&&mode()==4,"offhand setting disables second switch");game.config().offhandWands=true;
            base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);base.owner.setShiftKeyDown(false);mode(2);set(base.owner,"dmg_dealt",4999);
            long heard=failureSounds(client),other=failureSounds(otherClient);require(input()&&base.owner.getMainHandItem().getCount()==3&&base.owner.getEffect(MobEffects.WITHER).getDuration()==200
                    &&base.owner.getEffect(MobEffects.WITHER).getAmplifier()==ItemEffectLevels.amplifier(MobEffects.WITHER,9),"failed source qualification preserves item and applies item-capped Wither10s");
            require(failureSounds(client)==heard+1&&failureSounds(otherClient)==other,"exactly one owner-only source wither sound");base.owner.removeAllEffects();
            base.owner.entityTags().add("sin_wrath");require(!input()&&base.owner.getMainHandItem().getCount()==3&&!base.owner.hasEffect(MobEffects.WITHER),"already-held mode is a quiet no-op");base.owner.entityTags().remove("sin_wrath");
            for(int n:new int[]{3,5}){mode(n);set(base.owner,"lolidefecation",64);require(!input()&&base.owner.getMainHandItem().getCount()==3&&!base.owner.hasEffect(MobEffects.WITHER),"unfinished actual producer/checkpoint cannot be substituted with a tag grant "+n);}
        }
        void claims(){reset();mode(2);set(base.owner,"dmg_dealt",5001);set(base.other,"dmg_dealt",5000);base.other.entityTags().addAll(Set.of("sin_wrath","sin_envy"));
            base.owner.getInventory().setItem(0,token.copy());base.owner.entityTags().add("sin_gluttony");
            mana=game.items().mana().get(base.owner);require(input()&&base.owner.entityTags().containsAll(Set.of("sin_wrath","sin_gluttony"))&&!base.other.entityTags().contains("sin_wrath")&&base.other.entityTags().contains("sin_envy"),"replace only selected authority, preserve other sins");
            require(base.owner.getInventory().getItem(0).isEmpty()&&base.owner.getMainHandItem().getCount()==3&&game.items().mana().get(base.owner)==mana,"native clear debits first inventory match, not necessarily used hand; no mana");
            reset();mode(4);base.owner.entityTags().add("greed_cursed");base.other.entityTags().addAll(Set.of("sin_greed","sin_greed_active"));set(base.other,"greed_prestige",0);set(base.owner,"greed_prestige",1);
            var item=new ItemStack(Items.STONE);var data=new net.minecraft.nbt.CompoundTag();data.putInt("greed_greed",1);item.set(DataComponents.CUSTOM_DATA,CustomData.of(data));base.owner.getInventory().setItem(7,item);
            require(input()&&base.owner.entityTags().contains("sin_greed")&&!base.owner.entityTags().contains("greed_cursed")&&!base.other.entityTags().contains("sin_greed_active")&&base.owner.getMainHandItem().getCount()==2&&!base.owner.getInventory().getItem(7).isEmpty(),"Greed debits generator, retains token, clears curse and old active authority");
            reset();mode(7);base.other.entityTags().addAll(Set.of("have_mom","sin_envy"));base.other.setGameMode(GameType.SPECTATOR);
            require(input()&&base.owner.entityTags().contains("sin_envy")&&!base.other.entityTags().contains("sin_envy")&&base.owner.getMainHandItem().getCount()==2,"assignment removes old authority even from spectator");base.other.setGameMode(GameType.SURVIVAL);
            reset();mode(1);set(base.owner,"food_count",64);require(input()&&base.owner.entityTags().contains("sin_gluttony")&&base.owner.getMainHandItem().getCount()==2,"actual Gluttony success");
            for(var e:base.owner.level().getAllEntities())if(e instanceof net.minecraft.world.entity.item.ItemEntity itemEntity&&!before.contains(e)){
                var s=itemEntity.getItem();for(var key:List.of("whale","rabbit","snake"))if(LbcFartus.data(s).getIntOr(key,0)==1)rewards.put(key,s.copy());}
            require(rewards.size()==3&&rewards.values().stream().allMatch(LbcWitchGluttony::matches),"all THREE original reward pools produce real native actor inputs");
            reset();mode(6);set(base.owner,"sleep_time",3600);base.other.entityTags().add("indolence_active");require(input()&&base.owner.entityTags().contains("sin_indolence")&&!base.other.entityTags().contains("indolence_active")&&base.owner.getMainHandItem().getCount()==2,"Indolence claim delegates actual native finger producer and clears old active flags");
            require(game.items().indolenceFingers().snapshot().size()==20,"actual successful source claim creates twenty native fingers");for(var actor:game.items().indolenceFingers().snapshot())if(actor instanceof Mob mob){mob.setNoAi(true);mob.setNoGravity(true);}
            clearSins();
        }
        void sharedBonus(){reset();mode(2);set(base.owner,"dmg_dealt",4999);
            var board=game.server().getScoreboard();if(base.owner.getTeam()!=null)board.removePlayerFromTeam(base.owner.getScoreboardName());board.addPlayerToTeam(base.owner.getScoreboardName(),base.team);
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){var item=new ItemStack(Items.LEATHER_HELMET);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("nyauwu",1);flags.putInt("mage",1);item.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));base.owner.setItemSlot(slot,item);}
            var target=base.cow(base.at.add(4,0,0));target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.entityTags().add("magic");
            base.owner.setShiftKeyDown(true);require(input()&&target.getHealth()==100,"mode switching never emits a successful-cast armor pulse");base.owner.setShiftKeyDown(false);mode(2);
            require(input()&&target.getHealth()==100,"failed qualification never emits a successful-cast armor pulse");base.owner.removeAllEffects();set(base.owner,"dmg_dealt",5000);int maximum=game.items().mana().maximum(base.owner);
            require(input()&&target.getHealth()==75&&game.items().mana().maximum(base.owner)==maximum,"actual successful claim reuses ONE common Nyauwu25 pulse without inventing paid mage growth");
            base.remove(target);reset();
        }
        void clearSins(){for(var p:List.of(base.owner,base.other))for(var m:LbcWitchClaims.Mode.values())p.entityTags().remove(m.tag());}
        Entity jammer(String tag){var e=new Marker(EntityTypes.MARKER,base.owner.level());e.setPos(base.at);e.entityTags().addAll(Set.of(tag,"not_block"));own.add(e);base.owner.level().addFreshEntity(e);game.items().magic().loaded(e);return e;}
        boolean step(){client.ack();otherClient.ack();int now=game.server().getTickCount();require(now-start<90,"bounded native claim scenario");
            if(phase==0){if(now-start<2)return false;conditions();inputs();sharedBonus();claims();reset();magic=jammer("stopper_magic");tech=jammer("stopper");start=now;phase++;return false;}
            if(phase==1){if(now-start<2)return false;require(game.items().magic().suppressed(base.owner)&&game.items().magic().mechanismSuppressed(base.owner),"actual native jammer coverage");
                mode(2);set(base.owner,"dmg_dealt",5000);require(input()&&base.owner.entityTags().contains("sin_wrath"),"beacon MAGIC/TECH do not add nonexistent source acquisition gates");clearSins();
                base.owner.entityTags().add("nomagic_active");base.owner.setShiftKeyDown(true);mode=mode();require(!input()&&mode()==mode,"stronger source antimagic artifact blocks mode switch");base.owner.entityTags().remove("nomagic_active");base.owner.setShiftKeyDown(false);
                foreign=Objects.requireNonNull(game.server().getLevel(Level.NETHER));forced=foreign.setChunkForced(0,0,true);foreign.getChunk(0,0);var p=base.other;
                p.teleportTo(foreign,.5,200,.5,TeleportMomentum.positionFlags(Set.of()),0,0,true);p.entityTags().add("sin_wrath");set(p,"dmg_dealt",5001);
                require(service.verdict(base.owner,LbcWitchClaims.Mode.WRATH)==LbcWitchClaims.Verdict.UNQUALIFIED,"unbounded native incumbent selector sees actual foreign-dimension holder");p.entityTags().remove("sin_wrath");
                var back=base.at.add(12,0,0);p.teleportTo(base.owner.level(),back.x,back.y,back.z,TeleportMomentum.positionFlags(Set.of()),0,0,true);base.owner.setShiftKeyDown(true);mode=mode();game.items().timeStop().begin(base.owner.level(),100);stopped=true;start=now;phase++;return false;}
            if(phase==2){require(!input()&&mode()==mode,"real Time Stop pauses mode changes and acquisition");if(now-start<3)return false;game.items().timeStop().end(base.owner.level());stopped=false;require(input()&&mode()!=mode,"same native input resumes after thaw");
                for(var p:List.of(base.owner,base.other)){for(var m:LbcWitchClaims.Mode.values())p.entityTags().add(m.tag());p.entityTags().addAll(Set.of("sin_greed_active","indolence_active","kill_one_skeleton"));}
                int progress=new LbcSourceCounters(game).read(base.owner,"dmg_dealt");game.items().witchClaims().stop();
                for(var p:List.of(base.owner,base.other))require(Arrays.stream(LbcWitchClaims.Mode.values()).noneMatch(m->p.entityTags().contains(m.tag()))&&!p.entityTags().contains("sin_greed_active")&&!p.entityTags().contains("indolence_active")&&!p.entityTags().contains("kill_one_skeleton"),"match-boundary cleanup removes authority and active flags");
                require(new LbcSourceCounters(game).read(base.owner,"dmg_dealt")==progress,"authority cleanup never invents a second progress reset owner");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{if(stopped)game.items().timeStop().end(base.owner.level());clearSins();game.items().indolenceFingers().stop();for(var e:own){game.items().magic().unloaded(e);e.discard();}game.items().magic().flushCleansing();if(forced)foreign.setChunkForced(0,0,false);base.close();
                for(var p:List.of(base.owner,base.other)){p.entityTags().clear();p.entityTags().addAll(tags.get(p));for(var entry:scores.get(p).entrySet())if(entry.getValue()!=null)game.items().mana().score(p,entry.getKey()).set(entry.getValue());else {var objective=game.server().getScoreboard().getObjective(entry.getKey());if(objective!=null)game.server().getScoreboard().resetSinglePlayerScore(p,objective);}}
            }finally{client.close();otherClient.close();}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive scenario");running=new Fixture(game);try{running.init();}catch(Throwable e){running.close();running=null;throw e;}SkyWars2.LOGGER.info("[check] START witch-acquisition");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS witch-acquisition (raw root/both hands, source thresholds/global incumbent transfer, native inventory debit, original rewards/fingers, item-capped private failure, actual MAGIC/TECH/artifact/TimeStop boundaries; Lust/Vanity dependencies remain gated)");}catch(Throwable e){try{running.close();}catch(Throwable c){e.addSuppressed(c);}running=null;SkyWars2.LOGGER.error("[check] FAIL witch-acquisition",e);}}
}
