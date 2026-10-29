package me.skywars2.game;

import java.util.*;
import java.lang.reflect.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real source authority tags, existing status owners and actual server scheduling; no clock rewriting. */
public final class WitchPassiveChecks {
    private WitchPassiveChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Witch passives: "+why);}
    private static final Identifier MODIFIER=Identifier.parse("minecraft:e916adfe-014b-4ff5-82f1-af6ec9226d21");
    private static Field field(Object value,String name){for(Class<?> t=value.getClass();t!=null;t=t.getSuperclass())try{
        var f=t.getDeclaredField(name);f.setAccessible(true);return f;
    }catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static final class HeartRandom implements AutoCloseable {
        final ServerLevel level;final Object previous;final ArrayDeque<Float> draws=new ArrayDeque<>();int calls;
        HeartRandom(ServerLevel level){this.level=level;try{var f=field(level,"random");previous=f.get(level);
            f.set(level,Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(p,m,args)->{
                if(m.getName().equals("nextFloat")&&StackWalker.getInstance().walk(frames->frames.anyMatch(frame->frame.getClassName().equals(LbcWitchPassives.class.getName())&&frame.getMethodName().equals("hearts")))){
                    calls++;return draws.isEmpty()?.99999994f:draws.removeFirst();
                }
                try{return m.invoke(previous,args);}catch(InvocationTargetException e){throw e.getCause();}
            }));
        }catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        void next(float a,float b){require(draws.isEmpty(),"previous source random draws consumed");draws.add(a);draws.add(b);}
        public void close(){try{field(level,"random").set(level,previous);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,beta;final ServerLevel level,nether;
        final ShishEncounterChecks.State saved,otherSaved;ShishEncounterChecks.Client client,other;
        final SauvojenMeditationChecks.Attribution attribution;final LbcIndirectKills.TerrainProvenance provenance;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Entity> created=new ArrayList<>();final Vec3 origin;HeartRandom random;
        Cow foreign,nearby;Entity magic,technology;int phase,at,start,calls;float beforePause;boolean ownsStop,closed,foreignForced;
        Fixture(GameController game){
            this.game=game;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();nether=Objects.requireNonNull(game.server().getLevel(Level.NETHER));
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.items().witchPassives().count()==0,"exclusive actual participant/authority fixture");
            require(!game.items().timeStop().stopped(level)&&game.items().psycho().effectCount()==0&&game.items().electro().shockCount()==0,"no borrowed stop/psychological/electric status");
            for(var world:game.server().getAllLevels())for(var e:world.getAllEntities()){
                require(!e.entityTags().contains("sin_greed_have_heart"),"no borrowed global heart anchor");before.add(e);
            }
            saved=new ShishEncounterChecks.State(game,owner);otherSaved=new ShishEncounterChecks.State(game,beta);
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta));
            origin=new Vec3(owner.getX(),240,owner.getZ());
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin).offset(-1,-1,-1),BlockPos.containing(origin).offset(1,3,1))){
                require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir(),"resident exclusive native source footprint "+pos);terrain.put(pos.immutable(),game.maps().snapshot(level,pos));
            }
            provenance=game.items().indirectKills().captureTerrain(level,terrain.keySet());
        }
        void initialize(){
            for(var p:List.of(owner,beta)){
                require(p.entityTags().stream().noneMatch(tag->tag.equals("sin_greed")||tag.equals("sin_lust")),"no borrowed Witch authority");
                p.stopUsingItem();p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;
                p.entityTags().removeAll(Set.of("spectator","time_stopped","sin_greed_active","nomagic_active","strict_map_object","magic_academy","rcdenyalways"));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(100);p.setAbsorptionAmount(0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);
                game.items().magic().inventoryChanged(p);
            }
            game.items().magic().flushCleansing();
            owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+20,origin.y,origin.z,Set.of(),0,0,true);
            client=new ShishEncounterChecks.Client(game,owner);other=new ShishEncounterChecks.Client(game,beta);
            var foreignCell=BlockPos.containing(origin);foreignForced=nether.setChunkForced(foreignCell.getX()>>4,foreignCell.getZ()>>4,true);nether.getChunk(foreignCell.getX()>>4,foreignCell.getZ()>>4);
            move(beta,nether,origin);foreign=cow(nether,origin);move(beta,level,origin.add(20,0,0));
            random=new HeartRandom(level);owner.entityTags().add("sin_greed");start=at=game.server().getTickCount();
        }
        Cow cow(ServerLevel world,Vec3 position){var cow=new Cow(EntityTypes.COW,world);cow.setPos(position);cow.setNoAi(true);cow.setNoGravity(true);cow.setSilent(true);require(world.addFreshEntity(cow),"actual native cow publication");created.add(cow);return cow;}
        void move(ServerPlayer p,ServerLevel world,Vec3 position){require(p.teleportTo(world,position.x,position.y,position.z,Set.of(),p.getYRot(),p.getXRot(),true),"actual native player position/dimension change");if(p==owner&&client!=null)client.ack();else if(p==beta&&other!=null)other.ack();}
        int score(String name){return game.items().mana().score(owner,name).get();}
        void score(String name,int value){game.items().mana().score(owner,name).set(value);}
        boolean modifier(){return owner.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(MODIFIER);}
        void unchangedModifiers(){require(owner.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(MODIFIER).amount()==10
                &&owner.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(MODIFIER).amount()==10000,"exact original native modifier identity/amount");}
        Entity beacon(String tag){var marker=EntityTypes.MARKER.create(level,EntitySpawnReason.COMMAND);require(marker!=null,"native suppression marker");marker.setPos(owner.position());marker.entityTags().add(tag);created.add(marker);require(level.addFreshEntity(marker),"native stopper publication");return marker;}
        void discard(Entity entity){if(entity!=null){game.items().entityUnloaded(entity);entity.discard();}}
        boolean step(){int now=game.server().getTickCount();require(game.playing()&&now-start<180,"bounded real native phase duration");if(now<=at)return false;
            switch(phase){
                case 0->{
                    if(nether.getEntity(foreign.getUUID())!=foreign){require(now-start<40&&!foreign.isRemoved(),"native foreign anchor becomes visible in its actually loaded chunk");return false;}
                    require(!LbcGreed.active(owner)&&!modifier(),"authority without a heart has no active guard/modifiers");
                    for(var key:List.of("lookback","scared","twitching","piss"))score(key,500);score("shock",500);game.items().adoptRawFoodStatuses(owner);
                    owner.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,200,0),owner);owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,200,0),owner);
                    owner.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,200,0),owner);score("coldownshish",21);
                    foreign.entityTags().add("sin_greed_have_heart");level.setBlock(BlockPos.containing(origin),Blocks.GLASS.defaultBlockState(),2|16|32);level.setBlock(BlockPos.containing(origin).above(),Blocks.GLASS.defaultBlockState(),2|16|32);
                    owner.setHealth(100);phase=1;at=now;return false;
                }
                case 1->{
                    require(LbcGreed.active(owner)&&modifier()&&owner.getHealth()>100,"remote other-dimension heart activates native guard, modifiers and own healing: active="+LbcGreed.active(owner)+" modifier="+modifier()+" HP="+owner.getHealth()+" foreignRemoved="+foreign.isRemoved()+" foreignVisible="+(nether.getEntity(foreign.getUUID())==foreign)+" tags="+owner.entityTags());unchangedModifiers();
                    require(level.getBlockState(BlockPos.containing(origin)).isAir()&&level.getBlockState(BlockPos.containing(origin).above()).isAir(),"exact source .99..1.01 native destroy cells");
                    require(!owner.hasEffect(MobEffects.BLINDNESS)&&owner.hasEffect(MobEffects.SPEED)&&owner.hasEffect(MobEffects.LEVITATION),"source cleanse retains unlisted speed and shish21 levitation");
                    for(var key:List.of("lookback","scared","twitching","shock"))require(score(key)==0,"actual shared status owner cleared "+key);
                    require(game.items().psycho().effectValue(owner,"piss")==500&&game.items().electro().shocks(owner)==0,"unlisted piss remains; actual shock queue is gone");
                    require(owner.hasEffect(MobEffects.RESISTANCE)&&owner.getEffect(MobEffects.RESISTANCE).getAmplifier()==4&&!owner.getEffect(MobEffects.RESISTANCE).isVisible(),"native hidden ResistanceV source grant");
                    phase=2;at=now;return false;
                }
                case 2->{if(now-at<3)return false;unchangedModifiers();score("coldownshish",20);phase=3;at=now;return false;}
                case 3->{
                    require(!owner.hasEffect(MobEffects.LEVITATION),"source shish20 boundary clears levitation");
                    game.items().timeStop().begin(level,100);ownsStop=true;beforePause=owner.getHealth();foreign.entityTags().remove("sin_greed_have_heart");phase=4;at=now;return false;
                }
                case 4->{require(LbcGreed.active(owner)&&modifier()&&owner.getHealth()==beforePause,"actual stop preserves authority state and pauses pulses");
                    if(now-at<5)return false;game.items().timeStop().end(level);ownsStop=false;client.ack();other.ack();phase=5;at=now;return false;}
                case 5->{require(!LbcGreed.active(owner)&&!modifier(),"thaw reevaluates global anchor and removes only own attribute IDs");
                    game.items().psycho().clearEffects(owner);owner.removeAllEffects();foreign.entityTags().add("sin_greed_have_heart");
                    move(owner,level,new Vec3(origin.x,-31,origin.z));owner.setDeltaMovement(new Vec3(.125,-.0625,.1875));phase=6;at=now;return false;}
                case 6->{require(owner.getY()==322&&owner.getDeltaMovement().lengthSqr()>0,"native void escape reaches source322 without deleting velocity");
                    var packet=client.lastPosition();require(packet!=null&&packet.relatives().containsAll(Set.of(Relative.DELTA_X,Relative.DELTA_Y,Relative.DELTA_Z))&&!packet.relatives().contains(Relative.ROTATE_DELTA),"actual rescue packet preserves independent client motion axes");client.ack();
                    owner.entityTags().remove("sin_greed");foreign.entityTags().remove("sin_greed_have_heart");move(owner,level,origin);owner.removeAllEffects();owner.entityTags().add("sin_lust");owner.setOnGround(false);score("falltimer",9);phase=7;at=now;return false;}
                case 7->{require(!owner.hasEffect(MobEffects.SLOW_FALLING)&&score("falltimer")==10,"Lust reads canonical pre-increment falling score without owning another counter");phase=8;at=now;return false;}
                case 8->{require(owner.hasEffect(MobEffects.SLOW_FALLING)&&owner.getEffect(MobEffects.SLOW_FALLING).getDuration()==20,"source Lust falling threshold10 gives hidden native slow fall");
                    magic=beacon("stopper_magic");owner.removeEffect(MobEffects.SLOW_FALLING);phase=9;at=now;return false;}
                case 9->{require(!owner.hasEffect(MobEffects.SLOW_FALLING),"actual magic beacon blocks new Lust fall grants");
                    if(!owner.hasEffect(MobEffects.REGENERATION)){require(now-at<21,"actual next one-second Lust pulse");return false;}
                    require(owner.getEffect(MobEffects.REGENERATION).getAmplifier()==4&&!owner.getEffect(MobEffects.REGENERATION).isVisible(),"source hidden RegenerationV remains independent of fall beacon gate");
                    discard(magic);magic=null;technology=beacon("stopper");owner.removeEffect(MobEffects.SLOW_FALLING);phase=10;at=now;return false;}
                case 10->{require(owner.hasEffect(MobEffects.SLOW_FALLING),"mechanism beacon does not invent suppression for Lust's magical falling branch");discard(technology);technology=null;
                    owner.entityTags().remove("sin_lust");owner.removeAllEffects();owner.entityTags().add("sin_greed");foreign.entityTags().add("sin_greed_have_heart");nearby=cow(level,owner.position().add(2,0,0));move(beta,level,owner.position().add(3,0,0));
                    calls=random.calls;random.next(.4f,.00015625f);phase=11;at=now;return false;}
                case 11->{if(random.calls==calls){require(now-at<21,"natural heartbeat deadline");return false;}
                    require(random.calls==calls+2&&!nearby.entityTags().contains("sin_greed_have_heart")&&!beta.entityTags().contains("sin_greed_have_heart"),"native first-target independent probability boundaries .4 and .00015625 do not pass");
                    calls=random.calls;random.next(.3999f,.00015624f);phase=12;at=now;return false;}
                case 12->{if(random.calls==calls){require(now-at<21,"second natural heartbeat deadline");return false;}
                    require(random.calls==calls+2&&nearby.entityTags().contains("sin_greed_have_heart")&&nearby.isPersistenceRequired()&&beta.entityTags().contains("sin_greed_have_heart"),"actual native independent mob40percent/player.015625percent heartbeat outputs and mob persistence");
                    game.items().witchPassives().stop();require(!modifier()&&!LbcGreed.active(owner)&&!nearby.entityTags().contains("sin_greed_have_heart")&&!beta.entityTags().contains("sin_greed_have_heart")&&foreign.entityTags().contains("sin_greed_have_heart"),"owned cleanup preserves preexisting foreign anchor");return true;}
                default->throw new IllegalStateException("unknown native phase "+phase);
            }
        }
        public void close(){if(closed)return;closed=true;try{
            if(ownsStop)game.items().timeStop().end(level);game.items().witchPassives().stop();owner.entityTags().remove("sin_greed");owner.entityTags().remove("sin_lust");
            if(random!=null)random.close();game.items().psycho().clearEffects(owner);game.items().electro().clearEffects(owner);
            var fresh=new ArrayList<Entity>();for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())if(!before.contains(e)&&!(e instanceof ServerPlayer))fresh.add(e);
            for(var e:fresh){game.items().entityUnloaded(e);e.discard();}
            for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());game.items().indirectKills().restoreTerrain(provenance);
            move(owner,level,saved.state.position);move(beta,level,otherSaved.state.position);
        }finally{try{saved.restore(game);otherSaved.restore(game);if(client!=null)client.ack();if(other!=null)other.ack();attribution.close();}finally{if(client!=null)client.close();if(other!=null)other.close();if(foreignForced){var cell=BlockPos.containing(origin);nether.setChunkForced(cell.getX()>>4,cell.getZ()>>4,false);}}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive native fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}SkyWars2.LOGGER.info("[check] START witch-passives (real server ticks, no acquisition/full-authority qualification)");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS witch-passives (native global foreign-world heart gate, own Greed heal/attributes/precise destroy/cleanse/shared status owners, actual TimeStop, native void rescue and velocity packet, Lust canonical fall/regen/jammers, real two-draw heartbeat boundaries/outputs/owned cleanup; no complete Witch acquisition/other authorities/client/network qualification)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL witch-passives",error);}}
}
