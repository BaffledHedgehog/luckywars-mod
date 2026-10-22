package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native fixtures; real client sound/rendering and natural fall trajectories remain manual. */
public final class EnzanDropChecks {
    private EnzanDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Enzan drop: "+text);}
    private static ItemEntity token(ServerLevel level,Vec3 at,int count,ServerPlayer author){
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(LbcLevitationDropItem.ITEM,count));item.setThrower(author);
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;final Vec3 origin;
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());final List<ItemEntity> tokens=new ArrayList<>();
        final int alphaKills,betaKills;ItemEntity stack,paused,unloaded;int phase,lastTick,wait,frozenDuration;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two active actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());alphaKills=kills(alpha);betaKills=kills(beta);
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);level.getAllEntities().forEach(existing::add);
        }
        int kills(ServerPlayer p){return p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));}
        void initialize(){
            game.items().specialDrops().stop();level.getChunkAt(BlockPos.containing(origin));
            for(var p:List.of(alpha,beta)){p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);game.items().mana().noMagic(p,0);p.entityTags().remove("nomagic_active");game.items().magic().inventoryChanged(p);}
            game.items().magic().flushCleansing();place();presentation();game.command("give TestAlpha skywars2:levitation_drop 1");lastTick=game.server().getTickCount();
        }
        void place(){alpha.teleportTo(level,origin.x+20,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);}
        ItemEntity drop(int count){var item=token(level,origin,count,alpha);tokens.add(item);return item;}
        int buckets(ServerPlayer p){int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(ItemStack.isSameItemSameComponents(s,LbcEnzanDrop.bucket()))count+=s.getCount();}return count;}
        void effect(ServerPlayer p,int duration){var e=p.getEffect(MobEffects.LEVITATION);require(e!=null && e.getDuration()==duration && e.getAmplifier()==127 && !e.isAmbient() && e.isVisible() && e.showIcon(),"source visible/icon levitation60 amplifier127");}
        void presentation(){
            var source=new ItemStack(LbcLevitationDropItem.ITEM);var data=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr("levitation",0)==1 && data.getIntOr("drop",0)==1 && LbcLevitationDropItem.matches(source),"exact source flags");
            require(Identifier.parse("lbc:drop_trap").equals(source.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_ff").equals(source.get(DataComponents.TOOLTIP_STYLE)),"source model/tier");
            require(source.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals("drop_enzan") && !source.getHoverName().getStyle().isItalic() && source.getHoverName().getStyle().getColor().getValue()==0xffff55,"source yellow nonitalic translated name");
            var lore=source.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"source white nonitalic lore");
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/levitation_drop");require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(source,loot.getFirst()),"complete public leaf/prototype equality");
            var bucket=LbcEnzanDrop.bucket();require(bucket.is(Items.WATER_BUCKET) && bucket.getCount()==1,"one native water bucket");var label=bucket.get(DataComponents.CUSTOM_NAME);
            require(label!=null && label.getContents().equals(net.minecraft.network.chat.Component.literal("Enzan").getContents()) && !label.getStyle().isItalic() && label.getStyle().getColor().getValue()==0x55ff55 && label.getSiblings().size()==1,"literal green nonitalic Enzan label");var extra=label.getSiblings().getFirst();
            require(extra.getContents().equals(net.minecraft.network.chat.Component.literal(" Beta Server Harem event").getContents()) && !extra.getStyle().isItalic() && extra.getStyle().getColor().getValue()==0x555555,"literal dark gray nonitalic event extra");
            var expected=new ItemStack(Items.WATER_BUCKET);expected.set(DataComponents.CUSTOM_NAME,label.copy());require(ItemStack.isSameItemSameComponents(expected,bucket),"bucket has only source custom name beyond native components");
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public net.minecraft.network.PacketListener getPacketListener(){return alpha.connection;}};var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)connection;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(source,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),alpha.registryAccess());require(proxy.is(Items.REPEATING_COMMAND_BLOCK) && source.get(DataComponents.ITEM_MODEL).equals(proxy.get(DataComponents.ITEM_MODEL)) && source.get(DataComponents.TOOLTIP_STYLE).equals(proxy.get(DataComponents.TOOLTIP_STYLE)),"actual Polymer model/tier/carrier");require(ItemStack.isSameItemSameComponents(source,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),alpha.registryAccess())),"Polymer native roundtrip");});
            for(var item:List.of(Items.BARRIER,Items.REPEATING_COMMAND_BLOCK,Items.PAPER))require(!LbcLevitationDropItem.matches(new ItemStack(item)),"vanilla lookalike rejected");
        }
        void dispatch(ServerPlayer target){
            target.removeEffect(MobEffects.LEVITATION);int before=buckets(target),other=buckets(target==alpha?beta:alpha);var t=drop(2);
            require(game.items().cageDrops().target(t)==target && game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"nearest dispatch with exactly one debit");effect(target,60);
            require(buckets(target)==before+1 && buckets(target==alpha?beta:alpha)==other,"one bucket to nearest only");require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"same tick duplicate rejected");t.discard();target.removeEffect(MobEffects.LEVITATION);
        }
        void targets(){
            dispatch(beta);alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);dispatch(alpha);
            var t=drop(1);alpha.setGameMode(GameType.SPECTATOR);try{require(game.items().cageDrops().target(t)==beta,"native spectator excluded");}finally{alpha.setGameMode(GameType.SURVIVAL);alpha.setNoGravity(true);}
            game.spectators().enter(alpha);try{alpha.setGameMode(GameType.SURVIVAL);require(game.items().cageDrops().target(t)==beta,"explicit observer excluded even Survival");}finally{game.spectators().remove(alpha);alpha.setGameMode(GameType.SURVIVAL);alpha.setNoGravity(true);alpha.getInventory().clearContent();}
            for(String tag:List.of("spectator","rcdenyalways","sauvojen_tuntija","strict_map_object","magic_academy","transformator")){boolean added=alpha.entityTags().add(tag);try{require(game.items().cageDrops().target(t)==beta,"protected service/observer excluded "+tag);}finally{if(added)alpha.entityTags().remove(tag);}}
            t.discard();place();var board=game.server().getScoreboard();var alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());var betaTeam=board.getPlayersTeam(beta.getScoreboardName());var team=board.addPlayerTeam("codex_enzan_probe");
            try{board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);dispatch(beta);var ally=drop(1);require(game.items().specialDrops().spawn(ally),"allied actual helper activation");effect(beta,60);game.items().indirectKills().dying(beta,beta.damageSources().fall());require(beta.getKillCredit()==alpha,"accepted allied helper effect retains thrower fall cause");beta.removeEffect(MobEffects.LEVITATION);game.items().indirectKills().clear(beta);beta.setLastHurtByMob(null);beta.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);ally.discard();}finally{board.removePlayerTeam(team);if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);}
            beta.setGameMode(GameType.CREATIVE);try{dispatch(beta);}finally{beta.setGameMode(GameType.SURVIVAL);}
        }
        void rejectionAndOverflow(){
            for(int type:List.of(5,6)){beta.getInventory().clearContent();var artifact=new ItemStack(LbcMagic.MYSTICAL_ARTIFACT);var data=new net.minecraft.nbt.CompoundTag();data.putInt("mystical",1);data.putInt("type",type);artifact.set(DataComponents.CUSTOM_DATA,CustomData.of(data));beta.getInventory().setItem(0,artifact);game.items().magic().inventoryChanged(beta);
                try{game.items().indirectKills().clear(beta);beta.setLastHurtByMob(null);beta.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);int before=buckets(beta);var t=drop(1);require(game.items().specialDrops().spawn(t) && t.isRemoved() && !beta.hasEffect(MobEffects.LEVITATION) && buckets(beta)==before+1,"immediate cleansing rejects status but preserves independent bucket/debit "+type);game.items().indirectKills().dying(beta,beta.damageSources().fall());require(beta.getKillCredit()!=alpha,"rejected cleansing effect creates no fall cause");t.discard();}finally{beta.getInventory().clearContent();game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();}}
            var marker=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,level);marker.setPos(beta.position());marker.entityTags().add("stopper_magic");require(level.addFreshEntity(marker) && game.items().magic().suppressed(beta),"native antimagic fixture");
            try{int mana=game.items().mana().get(beta);dispatch(beta);require(game.items().mana().get(beta)==mana,"antimagic does not gate effect/bucket or spend mana");}finally{marker.discard();}
            beta.getInventory().clearContent();for(int i=0;i<36;i++)beta.getInventory().setItem(i,new ItemStack(Items.STONE,64));Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());level.getAllEntities().forEach(before::add);var full=drop(1);
            require(game.items().specialDrops().spawn(full),"full inventory still dispatches");effect(beta,60);var loose=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(!before.contains(e) && e instanceof ItemEntity item && ItemStack.isSameItemSameComponents(item.getItem(),LbcEnzanDrop.bucket()))loose.add(item);
            require(loose.size()==1 && loose.getFirst().getItem().getCount()==1 && loose.getFirst().position().distanceTo(beta.position())<3 && buckets(beta)==0,"native give overflow creates one real named bucket at recipient");loose.forEach(Entity::discard);full.discard();beta.removeEffect(MobEffects.LEVITATION);beta.getInventory().clearContent();
            var modes=new LinkedHashMap<ServerPlayer,GameType>();for(var p:level.players())modes.put(p,p.gameMode.getGameModeForPlayer());try{for(var p:modes.keySet())p.setGameMode(GameType.SPECTATOR);var noTarget=drop(2);require(game.items().cageDrops().target(noTarget)==null && game.items().specialDrops().spawn(noTarget) && noTarget.getItem().getCount()==1,"no eligible target still debits one");noTarget.discard();}finally{for(var e:modes.entrySet()){e.getKey().setGameMode(e.getValue());e.getKey().noPhysics=false;}}place();
        }
        void causality(){
            beta.removeEffect(MobEffects.LEVITATION);game.items().indirectKills().clear(beta);require(beta.addEffect(new MobEffectInstance(MobEffects.LEVITATION,60,128),alpha),"preexisting stronger sourced player effect");var original=beta.getEffect(MobEffects.LEVITATION);int beforeBucket=buckets(beta);var weaker=token(level,origin,1,beta);tokens.add(weaker);
            require(game.items().specialDrops().spawn(weaker) && beta.getEffect(MobEffects.LEVITATION)==original && original.getAmplifier()==128 && original.getDuration()==60 && buckets(beta)==beforeBucket+1,"actual weaker helper attempt rejected while independent bucket remains");game.items().indirectKills().dying(beta,beta.damageSources().fall());require(beta.getKillCredit()==alpha,"rejected helper status preserves previous owner's fall cause");beta.removeEffect(MobEffects.LEVITATION);game.items().indirectKills().clear(beta);beta.setLastHurtByMob(null);beta.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);weaker.discard();
            // Exercise the exact accepted player effect from the helper without killing a participant.
            var t=drop(1);require(game.items().specialDrops().spawn(t),"accepted sourced player effect");effect(beta,60);game.items().indirectKills().dying(beta,beta.damageSources().fall());require(beta.getKillCredit()==alpha,"accepted actual helper status retains thrower for fall");game.items().indirectKills().dying(beta,beta.damageSources().fellOutOfWorld());require(beta.getKillCredit()==alpha,"accepted helper status retains thrower for void");beta.removeEffect(MobEffects.LEVITATION);game.items().indirectKills().clear(beta);beta.setLastHurtByMob(null);beta.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);t.discard();
            for(boolean voidDeath:List.of(false,true))for(int scenario=0;scenario<3;scenario++){
                var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);cow.setPos(origin.add(0,4,0));cow.setNoAi(true);cow.setNoGravity(true);require(level.addFreshEntity(cow),"native disposable status victim");
                try{require(cow.addEffect(new MobEffectInstance(MobEffects.LEVITATION,60,127),alpha),"native sourced levitation accepted");if(scenario==1)cow.hurtServer(level,cow.damageSources().playerAttack(beta),1);if(scenario==2)require(!cow.addEffect(new MobEffectInstance(MobEffects.LEVITATION,40,126),beta),"weaker rejected status cannot replace cause");cow.setHealth(1);cow.damageCooldownTime=0;cow.setInvulnerableTime(0);int before=kills(scenario==1?beta:alpha);cow.hurtServer(level,voidDeath?cow.damageSources().fellOutOfWorld():cow.damageSources().fall(),1000);require(cow.isDeadOrDying() && cow.getKillCredit()==(scenario==1?beta:alpha) && kills(scenario==1?beta:alpha)==before+1,"native fall/void status attribution, newer attacker and rejected update "+voidDeath+"/"+scenario);}finally{game.items().indirectKills().clear(cow);cow.discard();}
            }
        }
        void guards(){
            var stale=drop(2);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().specialDrops().spawn(stale) && stale.getItem().getCount()==2,"stale token cannot debit");stale.discard();var fake=drop(2);fake.makeFakeItem();require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount()==2,"native give visual cannot debit");fake.discard();
            var remote=drop(2);int x=(BlockPos.containing(origin).getX()>>4)+128,z=(BlockPos.containing(origin).getZ()>>4)+128;while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;remote.setPos(x*16+8,origin.y,z*16+8);require(!game.items().specialDrops().spawn(remote) && remote.getItem().getCount()==2 && level.getChunkSource().getChunkNow(x,z)==null,"unresident token neither loads nor debits");remote.discard();
            var both=drop(2);var flags=both.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();flags.putInt("applepig",1);both.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(flags));int before=buckets(beta);require(game.items().specialDrops().spawn(both) && both.getItem().getCount()==1 && game.items().luckyPigs().snapshot().size()==4 && buckets(beta)==before+1,"Lucky pigs plus Enzan multi flag has one debit and both effects");effect(beta,60);both.discard();game.items().luckyPigs().stop();beta.removeEffect(MobEffects.LEVITATION);
        }
        boolean step(){require(game.playing(),"actual match retained");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;switch(phase){
            case 0->{require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(0),new ItemStack(LbcLevitationDropItem.ITEM)),"native give prototype");alpha.getInventory().clearContent();guards();targets();rejectionAndOverflow();causality();beta.getInventory().clearContent();stack=drop(2);phase++;}
            case 1->{require(stack.getItem().getCount()==1 && buckets(beta)==1,"first actual tick dispatches one bucket and unit debit");require(beta.hasEffect(MobEffects.LEVITATION) && !game.items().specialDrops().spawn(stack),"actual tick accepted effect and duplicate guard");phase++;}
            case 2->{require(stack.isRemoved() && buckets(beta)==2,"next actual tick consumes final unit and gives second bucket");frozenDuration=beta.getEffect(MobEffects.LEVITATION).getDuration();game.items().timeStop().begin(level,200);paused=drop(2);wait=0;phase++;}
            case 3->{require(paused.getItem().getCount()==2 && buckets(beta)==2 && beta.getEffect(MobEffects.LEVITATION).getDuration()==frozenDuration,"time stop pauses pending debit and active effect duration");if(++wait==3){paused.discard();game.items().timeStop().end(level);beta.removeEffect(MobEffects.LEVITATION);unloaded=drop(2);game.items().specialDrops().unloaded(unloaded);phase++;}}
            case 4->{require(!unloaded.isRemoved() && unloaded.getItem().getCount()==2 && buckets(beta)==2 && !beta.hasEffect(MobEffects.LEVITATION),"unloaded token cannot replay");unloaded.discard();return true;}
            default->throw new IllegalStateException("Unknown Enzan phase "+phase);
        }return false;}
        public void close(){game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();game.items().luckyPigs().stop();game.items().indirectKills().clear(alpha);game.items().indirectKills().clear(beta);
            var leftovers=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e) && !(e instanceof ServerPlayer) && Math.abs(e.getX()-origin.x)<8 && Math.abs(e.getZ()-origin.z)<8)leftovers.add(e);leftovers.forEach(Entity::discard);
            alphaState.restore(game);betaState.restore(game);game.items().magic().inventoryChanged(alpha);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaKills);beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaKills);}
    }
    private static void closeFailure(State state,Throwable error){try{state.close();}catch(Throwable cleanup){if(error!=cleanup)error.addSuppressed(cleanup);}finally{running=null;}}
    public static void run(GameController game){require(running==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error error){closeFailure(running,error);throw error;}}
    public static void tick(GameController game){if(running==null)return;var state=running;try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS enzan-drop");}}catch(Throwable error){closeFailure(state,error);SkyWars2.LOGGER.error("[check] FAIL enzan-drop",error);}}
    public static void stopProbe(GameController game){require(game.playing(),"actual match stop probe");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));game.items().timeStop().end(level);stopEpoch=game.items().epoch();game.items().timeStop().begin(level,200);stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),3,alpha);require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"pending Enzan retained under stop");SkyWars2.LOGGER.info("[check] PASS enzan-drop-stop-probe");}
    public static void cleanup(GameController game){require(!game.items().currentEpoch(stopEpoch) && stopToken!=null && stopToken.getItem().getCount()==3,"actual match stop invalidates pending Enzan without debit");require(!game.items().specialDrops().spawn(stopToken),"stopped match cannot replay Enzan");stopToken.discard();stopToken=null;stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS enzan-drop-cleanup");}
}
