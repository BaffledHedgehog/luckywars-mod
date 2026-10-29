package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Natural200/100tick expiry, real native player input, masks, projectile sweep and identity restoration. */
public final class PolymorphChecks {
    private PolymorphChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Polymorph: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcPolymorph service;
        final ShishEncounterChecks.Client client;
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final List<Entity> own=new ArrayList<>();
        final LbcApocalypseEvents.Authority authority;
        Cow cow;LbcWtf.Chef chef;Sheep cowBody,chefBody,playerBody,otherBody;LbcPolymorph.Orb probe;
        final Map<UUID,Integer> lives=new LinkedHashMap<>();int allSheepTicks,partSheepTicks;
        ItemStack savedItem;int phase,at,start,scared;long launches;double speed,scale;float health;boolean stopped,closed;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);service=game.items().polymorph();client=new ShishEncounterChecks.Client(game,base.owner);
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(base.owner,objective);if(score!=null)scores.put(objective,score.value());}
            authority=new LbcApocalypseEvents.Authority(base.owner.getUUID(),game.items().indirectKills().wildCause(base.owner),game.items().epoch());
        }
        // Player statistics are restored separately below; no replacement player or fake death engine.
        private final Map<PlayerStat,Integer> entityStats=new LinkedHashMap<>();
        private record PlayerStat(ServerPlayer player,net.minecraft.stats.Stat<net.minecraft.world.entity.EntityType<?>> stat){}
        void init(){base.init();require(service.count()==0&&service.orbs().isEmpty()&&service.volleyCount()==0,"exclusive empty polymorph service");
            require(game.alivePlayers().size()==2&&game.alivePlayers().containsAll(List.of(base.owner,base.other)),"exactly two real remaining match participants");
            for(var p:game.alivePlayers())lives.put(p.getUUID(),game.lives(p.getUUID()));
            for(var p:List.of(base.owner,base.other))for(var type:net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE){var stat=net.minecraft.stats.Stats.ENTITY_KILLED.get(type);entityStats.put(new PlayerStat(p,stat),p.getStats().getValue(stat));}
            cow=base.cow(base.at.add(-6,0,2));cow.setCustomName(Component.literal("keep original identity"));cow.setHealth(13);cow.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.EMERALD,3));cow.addEffect(new MobEffectInstance(MobEffects.SPEED,1000,2));
            var target=base.cow(base.at.add(-3,0,6));chef=base.spawn(base.owner,target);chef.receive(20);chef.setPos(base.at.add(-3,0,2));
            var p=base.owner;base.move(p,base.at.add(6,0,2));savedItem=new ItemStack(Items.DIAMOND,7);p.getInventory().setItem(2,savedItem.copy());
            p.getAttribute(Attributes.ARMOR).addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.Identifier.parse("skywars2:polymorph_fixture_armor"),7,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            p.addEffect(new MobEffectInstance(MobEffects.SPEED,1000,0));p.addEffect(new MobEffectInstance(MobEffects.SPEED,600,2));
            game.items().ownedEffects().grant(p,"polymorph.fixture",new MobEffectInstance(MobEffects.STRENGTH,800,1),p);
            game.items().psycho().afflict(p,p,50,4,0,0);scared=game.items().psycho().hudValue(p,"scared");
            game.items().acidCorruption().applyDeath(base.other,p,120);
            speed=p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();scale=p.getAttribute(Attributes.SCALE).getBaseValue();health=p.getHealth();
            cowBody=service.transform(cow,authority.cause());chefBody=service.transform(chef,authority.cause());playerBody=service.transform(p,authority.cause());
            require(cowBody!=null&&cowBody.position().equals(cow.position())&&chefBody!=null&&chefBody.position().equals(chef.position())&&playerBody!=null&&playerBody.position().equals(p.position())&&service.count()==3,"real cow, knocked Chef and participant become native sheep");
            require(cow.isAlive()&&!cow.isRemoved()&&service.suspended(cow)&&service.suspended(chef),"original UUID and runtime object retained, not reconstructed from type");
            require(p.getActiveEffects().isEmpty()&&p.getInventory().getItem(2).isEmpty()&&game.items().actionsBlocked(p),"native effects/inventory removed and shared actions blocked");
            require(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-.23)<1e-6&&Math.abs(p.getMaxHealth()-8)<1e-6,"sheep speed and eight HP");
            require(game.items().mana().hudValue(p,"scared")==0&&game.items().mana().hudValue(p,"delayed_death")==0,"existing status clocks hidden, not expired");
            require(!p.addEffect(new MobEffectInstance(MobEffects.SPEED,40,5)),"new native potion cannot affect concealed human");
            client.ack();
            client.listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(p.position().add(.15,0,.1),70,15,false,false));
            require(Math.abs(p.getYRot()-70)<.01&&Math.abs(p.getXRot()-15)<.01,"native move packet preserves free controller look");p.setDeltaMovement(new Vec3(.05,0,.02));
            require(p.hurtServer(p.level(),p.damageSources().generic(),2)&&playerBody.getHealth()==6&&p.getHealth()==6,"human shell damage synchronizes both HP immediately, before the next tick");
            p.attack(cowBody);require(cowBody.getHealth()==8,"real native player melee is blocked in sheep form");
            var before=p.getDeltaMovement();playerBody.push(p);require(p.getDeltaMovement().equals(before),"sheep cannot push its human controller");
            playerBody.setInvulnerableTime(0);playerBody.damageCooldownTime=0;
            require(playerBody.hurtServer(p.level(),p.damageSources().playerAttack(base.other),1)&&!p.getDeltaMovement().equals(before),"real body attack transfers its external impulse without replacing controller velocity");
            require(p.getHealth()==playerBody.getHealth()&&p.getHealth()==5,"body damage synchronizes human HP in the same native call");
            playerBody.heal(2);require(p.getHealth()==7&&p.getHealth()==playerBody.getHealth(),"native sheep healing synchronizes human HP immediately");
            p.heal(10);require(p.getHealth()==7,"human food/healing cannot desynchronize sheep HP");
            game.items().timeStop().begin(p.level(),100);stopped=true;start=at=game.server().getTickCount();
        }
        boolean step(){client.ack();int now=game.server().getTickCount();require(now-start<650,"bounded real-tick fixture");
            if(phase<=1){
                require(game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.alivePlayers().size()==2,"match continues with some or all remaining players polymorphed");
                for(var p:List.of(base.owner,base.other))require(game.alivePlayers().contains(p)&&game.lives(p.getUUID())==lives.get(p.getUUID())&&!game.awaitingRespawn(p.getUUID()),"sheep remains the same living participant without consuming a life");
                if(service.active(base.owner))require(base.owner.getHealth()==playerBody.getHealth(),"participant HP equals sheep HP throughout active form");
                if(service.active(base.other)){require(base.other.getHealth()==otherBody.getHealth(),"second participant HP equals sheep HP");allSheepTicks++;}else if(service.active(base.owner))partSheepTicks++;
            }
            if(phase==0){require(service.remaining(cow)==200&&service.remaining(base.owner)==200,"real stopped ticks pause all forms");if(now-at<5)return false;game.items().timeStop().end(base.owner.level());stopped=false;
                otherBody=service.transform(base.other,authority.cause());require(otherBody!=null&&service.count()==4&&game.alivePlayers().stream().allMatch(service::active),"every remaining real player becomes a sheep");phase=1;at=now;return false;}
            if(phase==1){
                if(service.active(base.owner)){
                    require(base.owner.getActiveEffects().isEmpty()&&game.items().psycho().hudValue(base.owner,"scared")==scared,"native old effects and custom Scared remain suspended");
                    require(Math.abs(playerBody.getYRot()-base.owner.getYRot())<.01,"sheep copies controller look");return false;
                }
                require(now-at>=200,"ordinary polymorph lasts200 actual non-stopped ticks");
                require(!service.active(base.other)&&otherBody.isRemoved()&&base.other.getHealth()==20&&allSheepTicks>=190&&partSheepTicks>=5,"both participants return after a whole natural all-sheep interval, without ending the match");
                require(!service.active(cow)&&!service.active(chef)&&!cow.isRemoved()&&!chef.isRemoved()&&cowBody.isRemoved()&&chefBody.isRemoved(),"same cow and Chef return after natural expiry: cow="+cow.isRemoved()+" chef="+chef.isRemoved()+" cowBody="+cowBody.isRemoved()+" chefBody="+chefBody.isRemoved()+" timers="+service.remaining(cow)+"/"+service.remaining(chef)+" bodies="+cowBody.position()+"/"+chefBody.position()+" target="+chef.target.isAlive()+" owner="+base.owner.isAlive());
                require(cow.getHealth()==13&&cow.getCustomName().getString().equals("keep original identity")&&cow.getMainHandItem().getCount()==3,"original HP/name/equipment preserved");
                require(base.service.stasis(chef)>=198,"Chef knock clock stayed paused");
                var p=base.owner;require(ItemStack.isSameItemSameComponents(p.getInventory().getItem(2),savedItem)&&p.getInventory().getItem(2).getCount()==7,"player inventory restored");
                require(p.getHealth()==health&&p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()==speed&&p.getAttribute(Attributes.SCALE).getBaseValue()==scale,"human HP/speed/scale restored");
                require(p.getAttribute(Attributes.ARMOR).getPermanentModifiers().stream().anyMatch(m->m.id().equals(net.minecraft.resources.Identifier.parse("skywars2:polymorph_fixture_armor"))),"permanent attribute modifier stays permanent after restoration");
                var effect=p.getEffect(MobEffects.SPEED);require(effect!=null&&effect.getAmplifier()==2&&effect.getDuration()>=598&&((EffectLayers.Access)effect).skywars2$layerHidden()!=null,"whole frozen native effect chain restored: "+effect);
                require(game.items().acidCorruption().deathRemaining(p)>=119&&game.items().psycho().hudValue(p,"scared")>=45,"custom statuses resume with preserved remaining clocks");
                game.items().ownedEffects().withdraw(p,"polymorph.fixture");require(!p.hasEffect(MobEffects.STRENGTH),"owned effect lease survives suspension and still withdraws correctly");
                game.items().psycho().clearEffects(p);game.items().acidCorruption().clear(p);p.removeAllEffects();base.clear();
                var death=base.cow(base.at.add(0,0,4));var sheep=service.transform(death,authority.cause());sheep.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE);own.add(death);
                phase=2;at=now;return false;
            }
            if(phase==2){require(own.getFirst().isRemoved()&&service.count()==0,"dead temporary sheep never resurrects its original mob");
                cow=base.cow(base.at.add(0,0,4));cowBody=service.transform(cow,authority.cause());var marker=new Marker(EntityTypes.MARKER,base.owner.level());marker.entityTags().addAll(List.of("stopper_magic","not_block"));marker.setPos(cowBody.position());own.add(marker);base.owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);phase=3;return false;
            }
            if(phase==3){require(!service.active(cow)&&cowBody.isRemoved()&&!cow.isRemoved(),"actual MAG field ends polymorph immediately");var marker=own.getLast();game.items().magic().unloaded(marker);marker.discard();base.block(marker.blockPosition(),Blocks.AIR);game.items().magic().flushCleansing();
                base.move(base.owner,base.at.add(-12,0,-12));base.move(base.other,base.at.add(12,0,-12));
                for(var old:base.cows)if(!old.isRemoved())base.remove(old);
                cow=base.cow(base.at.add(0,.9,8));for(int x=-2;x<=2;x++)for(int y=0;y<=3;y++)base.block(BlockPos.containing(base.at.add(x,y,2)),Blocks.STONE);
                probe=service.orb(authority,base.owner.level(),base.at.add(0,2,0),new Vec3(0,0,1));require(probe!=null&&!game.items().magic().suppressedAt(base.owner.level(),probe.display.position()),"native sphere display admitted outside retired MAG block");phase=4;at=now;return false;
            }
            if(phase==4){if(now-at<8)return false;require(!probe.display.isRemoved()&&probe.display.getZ()>base.at.z+3.2,"sphere crosses actual solid wall without block collision: removed="+probe.display.isRemoved()+" at="+probe.display.position()+" age="+probe.age+" motion="+probe.motion);service.stop();
                for(int n=0;n<48;n++)base.cow(base.at.add(n%8-4,0,3+n/8));
                launches=service.emittedTotal();
                try{var method=LbcApocalypseEvents.class.getDeclaredMethod("phase",LbcApocalypseEvents.Authority.class,LbcApocalypseEvents.Kind.class,int.class,net.minecraft.commands.CommandSourceStack.class);method.setAccessible(true);method.invoke(game.items().apocalypseEvents(),authority,LbcApocalypseEvents.Kind.END,49,game.server().createCommandSourceStack().withLevel(base.owner.level()));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
                require(service.volleyCount()==1,"real End49 begins one Magnesia volley sequence");game.items().apocalypseEvents().stop();phase=5;at=now;return false;
            }
            if(phase==5){if(now-at<95)return false;require(service.volleyCount()==0&&service.emittedTotal()-launches==40&&service.orbs().size()<=40,"all10 native volleys finish without duplicate phase scheduling");
                service.stop();for(var c:base.cows)if(!c.isRemoved())c.entityTags().add("strict_map_object");base.owner.entityTags().add("strict_map_object");base.other.entityTags().add("strict_map_object");
                probe=service.orb(authority,base.owner.level(),base.at.add(0,6,0),new Vec3(1,0,0));phase=6;at=now;return false;
            }
            if(phase==6){if(now-at<100){require(!probe.display.isRemoved(),"sphere keeps exact100tick flight lifetime");return false;}require(probe.display.isRemoved(),"sphere naturally retires after five seconds");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{if(stopped)game.items().timeStop().end(base.owner.level());service.stop();game.items().apocalypseEvents().stop();game.items().psycho().clearEffects(base.owner);game.items().acidCorruption().clear(base.owner);game.items().ownedEffects().withdraw(base.owner,"polymorph.fixture");for(var e:own){game.items().magic().unloaded(e);e.discard();}base.owner.getAttribute(Attributes.ARMOR).removeModifier(net.minecraft.resources.Identifier.parse("skywars2:polymorph_fixture_armor"));base.owner.entityTags().remove("strict_map_object");base.other.entityTags().remove("strict_map_object");base.close();for(var entry:entityStats.entrySet())entry.getKey().player.getStats().setValue(entry.getKey().player,entry.getKey().stat,entry.getValue());for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(base.owner,objective).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(base.owner,objective);}finally{client.close();}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}SkyWars2.LOGGER.info("[check] START polymorph");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS polymorph (immediate shared HP, all remaining real participants sheep for200ticks without match end, natural cow/Chef/player identity and layered restoration, death boundary, MAG, End49 volleys, wall crossing and100tick orb lifetime)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL polymorph",error);}}
}
