package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Raw Flesh inputs and real native factories, dragon ticks, rider packets and pause boundaries. */
public final class WitchLustChecks {
    private WitchLustChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Lust: "+why);}
    private static final List<String> SCORES=List.of("lust_cd","lust_cd_small");
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcWitchLust service;
        final ShishEncounterChecks.Client client,otherClient;
        final Map<ServerPlayer,Set<String>> tags=new IdentityHashMap<>();
        final Map<ServerPlayer,Map<String,Integer>> scores=new IdentityHashMap<>();
        final Set<Entity> owners=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Entity> own=new ArrayList<>();
        final net.minecraft.world.scores.ScoreHolder luck=net.minecraft.world.scores.ScoreHolder.forNameOnly("final_luck_modifier");
        final Integer oldLuck;final LbcIndirectKills.TerrainProvenance terrain;
        ItemStack flesh;EnderDragon dragon;Entity swapTarget,jammer,child;net.minecraft.world.entity.animal.cow.Cow contact;LbcSpellSource.Source accepted;
        Vec3 at;int phase,start,mark,cd;float hp;boolean stopped,closed;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);service=game.items().witchLust();
            require(service.snapshot().isEmpty(),"no borrowed Lust actors");client=new ShishEncounterChecks.Client(game,base.owner);otherClient=new ShishEncounterChecks.Client(game,base.other);
            for(var p:List.of(base.owner,base.other)){tags.put(p,new HashSet<>(p.entityTags()));var saved=new HashMap<String,Integer>();for(var name:SCORES)saved.put(name,new LbcSourceCounters(game).read(p,name));scores.put(p,saved);}
            var objective=game.server().getScoreboard().getObjective("lbc.math");var score=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(luck,objective);oldLuck=score==null?null:score.value();
            var cells=new ArrayList<BlockPos>();for(var pos:BlockPos.betweenClosed(base.center.offset(-16,-4,-16),base.center.offset(16,12,16)))cells.add(pos.immutable());terrain=game.items().indirectKills().captureTerrain(base.owner.level(),cells);
        }
        void init(){base.init();base.move(base.owner,base.at.add(-12,0,0));base.move(base.other,base.at.add(12,0,0));game.config().offhandWands=true;
            for(var p:List.of(base.owner,base.other)){p.entityTags().removeAll(Set.of("sin_lust","nomagic_active","sin_greed_active","spectator"));p.setShiftKeyDown(false);}
            flesh=game.lbc().rollTable(base.owner.level(),base.at,base.owner,"lbc:lust_get").stream().filter(LbcWitchLust::matches).findFirst().orElseThrow();
            require(flesh.is(Items.CARROT_ON_A_STICK)&&flesh.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:metamorph_flesh"))&&LbcFartus.data(flesh).getIntOr("magicwand",0)==1,"actual source Flesh payload");
            flesh.setCount(2);base.owner.setItemInHand(InteractionHand.MAIN_HAND,flesh.copy());base.owner.setItemInHand(InteractionHand.OFF_HAND,flesh.copy());base.owner.setOnGround(false);
            require(!input(InteractionHand.MAIN_HAND)&&!input(InteractionHand.OFF_HAND)&&service.snapshot().isEmpty(),"both hands require actual authority");base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);base.owner.entityTags().add("sin_lust");resetCooldowns();
            swapTarget=base.cow(base.at.add(4,0,0));for(var level:game.server().getAllLevels())for(var e:level.getAllEntities())if(e!=swapTarget&&e.entityTags().add("owner"))owners.add(e);
            jammer=jammer("stopper_magic");jammer.setPos(base.at.add(12,0,0));game.items().magic().unloaded(jammer);game.items().magic().loaded(jammer);
            start=mark=game.server().getTickCount();
        }
        boolean input(InteractionHand hand){return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(base.owner,base.owner.level(),hand).consumesAction();}
        void resetCooldowns(){for(var name:SCORES)game.items().mana().score(base.owner,name).set(0);}
        void luck(int value){var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");require(objective!=null,"native math objective");board.getOrCreatePlayerScore(luck,objective,true).set(value);}
        void restoreLuck(){var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");if(objective!=null){if(oldLuck==null)board.resetSinglePlayerScore(luck,objective);else board.getOrCreatePlayerScore(luck,objective,true).set(oldLuck);}}
        void swap(){base.owner.setOnGround(true);base.owner.setDeltaMovement(new Vec3(.17,.23,-.31));swapTarget.setDeltaMovement(new Vec3(-.2,.12,.08));var from=base.owner.position();var dest=swapTarget.position();
            var a=base.owner.getDeltaMovement();var b=swapTarget.getDeltaMovement();
            require(!game.items().magic().suppressed(base.owner)&&game.items().magic().suppressedAt(base.owner.level(),swapTarget.position()),"only target is inside actual antimagic area");
            int mana=game.items().mana().get(base.owner);require(!input(InteractionHand.MAIN_HAND)&&base.owner.position().equals(from)&&swapTarget.position().equals(dest)
                    &&base.owner.getDeltaMovement().equals(a)&&swapTarget.getDeltaMovement().equals(b)&&game.items().mana().get(base.owner)==mana,"protected target cannot swap or change either momentum when caster is outside");
            clearJammer();require(!game.items().magic().suppressedAt(base.owner.level(),swapTarget.position()),"actual target protection ends after jammer removal");
            require(input(InteractionHand.MAIN_HAND),"grounded raw Flesh selects its native random target after antimagic ends");
            require(base.owner.position().distanceTo(dest)<1e-6&&swapTarget.position().distanceTo(from)<1e-6,"real feet positions swap");
            require(base.owner.getDeltaMovement().equals(a)&&swapTarget.getDeltaMovement().equals(b)&&service.snapshot().isEmpty(),"swap preserves BOTH velocities and grounded use creates no dragon");
            for(var e:owners)e.entityTags().remove("owner");owners.clear();swapTarget.discard();base.move(base.owner,base.at);
        }
        void area(){var board=game.server().getScoreboard();for(var p:List.of(base.owner,base.other)){if(p.getTeam()!=null)board.removePlayerFromTeam(p.getScoreboardName());board.addPlayerToTeam(p.getScoreboardName(),base.team);}
            var enemy=base.cow(base.at.add(4,0,0));var ally=base.cow(base.at.add(-4,0,0));board.addPlayerToTeam(ally.getScoreboardName(),base.team);
            base.owner.setOnGround(true);base.owner.setShiftKeyDown(true);double before=base.owner.getAttributeBaseValue(Attributes.MAX_HEALTH);float health=base.owner.getHealth();
            require(input(InteractionHand.MAIN_HAND)&&base.owner.getAttributeBaseValue(Attributes.MAX_HEALTH)==before+8&&enemy.getAttributeBaseValue(Attributes.MAX_HEALTH)==12&&ally.getAttributeBaseValue(Attributes.MAX_HEALTH)==20,"grounded sneak uses shared enemy-only native HP transfer");
            require(base.owner.getHealth()==health&&game.items().mana().score(base.owner,"lust_cd").get()==30&&!input(InteractionHand.MAIN_HAND),"no instant healing; source30-second cooldown gates repeated cast");
            base.remove(enemy);base.remove(ally);base.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);base.owner.setShiftKeyDown(false);resetCooldowns();
        }
        void pool(){require(LbcRandomMobSpawn.ENTRIES.size()>=90,"original90 source pool remains with current additions");
            for(int i=1;i<=LbcRandomMobSpawn.ENTRIES.size();i++){var e=LbcRandomMobSpawn.spawn(game,base.owner,i);require(e!=null,"native source factory index "+i);own.add(e);
                require(e.getType().builtInRegistryHolder().key().identifier().toString().equals(LbcRandomMobSpawn.ENTRIES.get(i-1).type()),"exact source type "+i);
                if(i==19)require(((LivingEntity)e).getMainHandItem().is(Items.TRIDENT),"drowned source trident");
                if(i==75)require(((net.minecraft.world.entity.monster.Creeper)e).isPowered(),"genuine charged creature remains in random pool");
                if(i==84)require(e.isInvulnerable(),"source invulnerable creaking");e.discard();}
            luck(7);base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,flesh.copy());base.owner.setOnGround(false);base.owner.setShiftKeyDown(true);int mana=game.items().mana().get(base.owner);require(input(InteractionHand.OFF_HAND)&&service.snapshot().size()==20,"air sneak creates exactly20 native source draws, offhand included");
            require(service.snapshot().stream().allMatch(e->e instanceof EnderDragon&&game.items().spellSources().source(e).emitter().equals(e.getUUID())),"global source luck7 forces index22, each native actor owns its emitter");
            require(game.items().mana().score(base.owner,"lust_cd").get()==10&&base.owner.getMainHandItem().isEmpty()&&base.owner.getOffhandItem().getCount()==2&&game.items().mana().get(base.owner)==mana,"free unlimited Flesh: source10-second cooldown, no item/mana cost");
            service.stop();restoreLuck();base.owner.setItemInHand(InteractionHand.MAIN_HAND,flesh.copy());base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);base.owner.removeAllEffects();resetCooldowns();base.owner.setShiftKeyDown(false);
        }
        void dragon(){base.move(base.owner,base.at.add(-5,5,0));base.owner.setOnGround(false);base.owner.setYRot(-90);base.owner.setXRot(0);
            require(input(InteractionHand.MAIN_HAND)&&service.snapshot().size()==1,"air normal use creates exactly ONE controlled dragon");dragon=(EnderDragon)service.snapshot().getFirst();
            require(dragon.getHealth()==20&&dragon.getMaxHealth()==20&&dragon.isPersistenceRequired()&&dragon.getPhaseManager().getCurrentPhase().getPhase().getId()==1,"native dragon sourceHP20/persistence/phase1");
            require(base.owner.getVehicle()==dragon&&FullInvisibility.proxyFor(base.owner.getId(),base.other)&&!FullInvisibility.proxyFor(base.owner.getId(),base.owner),"real rider mount; outsiders see only dragon and owner keeps native self");
            require(service.harmless(dragon,base.owner)&&!service.harmless(dragon,base.other),"native dragon contact excludes rider only");
            at=dragon.position();hp=base.owner.getHealth();mark=game.server().getTickCount();
        }
        Entity jammer(String tag){var e=new Marker(EntityTypes.MARKER,base.owner.level());e.setPos(base.owner.position());e.entityTags().addAll(Set.of(tag,"not_block"));own.add(e);base.owner.level().addFreshEntity(e);game.items().magic().loaded(e);return e;}
        void clearJammer(){if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();if(base.blocks.containsKey(jammer.blockPosition()))base.block(jammer.blockPosition(),Blocks.AIR);game.items().magic().flushCleansing();jammer=null;}}
        boolean step(){client.ack();otherClient.ack();int now=game.server().getTickCount();require(now-start<150,"bounded actual-tick scenario");
            if(phase==0){if(now-start<2)return false;swap();area();pool();dragon();phase++;return false;}
            if(phase==1){require(!dragon.isRemoved()&&dragon.position().distanceTo(at.add(2,0,0))<1e-4,"native aiStep advances dragon exactly2 blocks/tick along player look: "+dragon.position().subtract(at));
                require(base.owner.getHealth()==hp,"native wing/head contacts do not hurt own rider");
                contact=base.cow(dragon.head.position());client.listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(0,-90,false,false));at=dragon.position();phase++;return false;}
            if(phase==2){require(contact.getHealth()<20,"actual native multipart head contact still hurts other creatures");base.remove(contact);require(dragon.position().distanceTo(at.add(0,2,0))<1e-4,"actual native look packet controls next dragon step");
                cd=game.items().mana().score(base.owner,"lust_cd_small").get();at=dragon.position();game.items().timeStop().begin(base.owner.level(),100);stopped=true;mark=now;phase++;return false;}
            if(phase==3){require(dragon.position().equals(at)&&game.items().mana().score(base.owner,"lust_cd_small").get()==cd,"real Time Stop freezes native actor and existing cooldown owner");if(now-mark<4)return false;game.items().timeStop().end(base.owner.level());stopped=false;
                accepted=game.items().spellSources().source(dragon);try(var scope=game.items().spellSources().scope(dragon)){var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,base.owner.level());arrow.setPos(dragon.position());arrow.setNoGravity(true);base.owner.level().addFreshEntity(arrow);child=arrow;own.add(arrow);}
                require(service.snapshot().contains(child)&&game.items().spellSources().source(child)==accepted,"native post-cast child birth is owned with immutable accepted cause");
                base.owner.stopRiding();base.owner.entityTags().remove("sin_lust");base.other.entityTags().add("sin_lust");base.move(base.other,dragon.position().add(1,0,0));base.other.setXRot(90);at=dragon.position();phase++;return false;}
            if(phase==4){require(base.other.getVehicle()==dragon&&game.items().communism().owner(dragon)==base.other&&game.items().spellSources().source(dragon).author().equals(base.other.getUUID())&&game.items().spellSources().source(child)==accepted,"current authority transfers real rider and future cause; old child cause stays fixed");
                require(!FullInvisibility.proxyFor(base.owner.getId(),base.other)&&FullInvisibility.proxyFor(base.other.getId(),base.owner),"authority handover restores old rider and hides new rider");
                require(dragon.position().distanceTo(at.add(0,-2,0))<1e-4,"new current holder steers native dragon");jammer=jammer("stopper");phase++;return false;}
            if(phase==5){require(!dragon.isRemoved()&&game.items().magic().mechanismSuppressed(base.other),"TECH does not end Lust dragon");clearJammer();jammer=jammer("stopper_magic");phase++;return false;}
            if(phase==6){require(dragon.isRemoved()&&base.other.getVehicle()==null&&!FullInvisibility.proxyFor(base.other.getId(),base.owner),"MAG ends active transformation, ejects and restores real player view");
                base.owner.entityTags().add("sin_lust");base.other.entityTags().remove("sin_lust");base.move(base.owner,jammer.position());base.owner.setOnGround(false);resetCooldowns();require(!input(InteractionHand.MAIN_HAND),"MAG blocks new raw Flesh cast");clearJammer();service.stop();base.move(base.owner,base.at.add(-5,5,0));base.owner.setYRot(-90);base.owner.setXRot(0);require(input(InteractionHand.MAIN_HAND),"same raw mode resumes after MAG");dragon=(EnderDragon)service.snapshot().getFirst();base.owner.setShiftKeyDown(true);mark=now;phase++;return false;}
            if(phase==7){if(!dragon.isRemoved()){require(now-mark<6,"source crouch retirement is bounded by five ticks");return false;}
                require(base.owner.getVehicle()==null&&!FullInvisibility.proxyFor(base.owner.getId(),base.other),"source global crouch retirement also ejects/reveals");base.owner.setShiftKeyDown(false);game.items().mana().score(base.owner,"lust_cd_small").set(2);mark=now;phase++;return false;}
            if(phase==8){int expected=Math.max(0,2-(now/20-mark/20));require(game.items().mana().score(base.owner,"lust_cd_small").get()==expected,"existing source cooldown drains by source seconds, not each tick");if(now-mark<40)return false;require(expected==0,"source cooldown naturally expires");service.stop();return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{if(stopped)game.items().timeStop().end(base.owner.level());service.stop();clearJammer();for(var e:owners)e.entityTags().remove("owner");for(var e:own)e.discard();restoreLuck();base.close();game.items().indirectKills().restoreTerrain(terrain);
                for(var p:List.of(base.owner,base.other)){p.entityTags().clear();p.entityTags().addAll(tags.get(p));for(var entry:scores.get(p).entrySet())if(entry.getValue()!=null)game.items().mana().score(p,entry.getKey()).set(entry.getValue());else{var objective=game.server().getScoreboard().getObjective(entry.getKey());if(objective!=null)game.server().getScoreboard().resetSinglePlayerScore(p,objective);}}
            }finally{client.close();otherClient.close();}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){running.close();running=null;throw error;}SkyWars2.LOGGER.info("[check] START witch-lust");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS witch-lust (raw source Flesh/both hands, separate ground/air modes, native swap/momentum/HP transfer, original90 plus current factories and20 luck-modified draws, native dragon/multipart rider/look/current holder, accepted child cause, TimeStop/MAG/TECH)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL witch-lust",error);}}
}
