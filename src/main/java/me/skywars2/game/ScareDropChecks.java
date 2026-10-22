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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native fixtures; sound/particle counts and real-client rendering are source-reviewed separately. */
public final class ScareDropChecks {
    private ScareDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Scare drops: "+message);}
    private static ItemEntity token(ServerLevel level,Vec3 at,ItemStack stack,ServerPlayer author){
        var t=new ItemEntity(level,at.x,at.y,at.z,stack);t.setThrower(author);
        t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);t.setPickUpDelay(1000);
        require(level.addFreshEntity(t),"native item admission");return t;
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final List<ItemEntity> tokens=new ArrayList<>();final Vec3 origin;
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final int alphaKills,betaKills;
        ItemEntity stack,paused,unloaded;Marker suppressor;
        int phase,lastTick,wait,frozenDuration;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            alphaKills=kills(alpha);betaKills=kills(beta);
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);
            level.getAllEntities().forEach(existing::add);
        }
        int kills(ServerPlayer p){return p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));}
        void initialize(){
            game.items().specialDrops().stop();level.getChunkAt(BlockPos.containing(origin));
            for(var p:List.of(alpha,beta)){p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);game.items().mana().noMagic(p,0);p.entityTags().remove("nomagic_active");game.items().magic().inventoryChanged(p);}
            game.items().magic().flushCleansing();place();presentation();lastTick=game.server().getTickCount();
        }
        void place(){alpha.teleportTo(level,origin.x+20,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);}
        ItemEntity drop(LbcScareDropItem.Kind kind,int n){var t=token(level,origin,new ItemStack(kind.item(),n),alpha);tokens.add(t);return t;}
        void presentation(){
            for(var kind:LbcScareDropItem.Kind.values()){
                var s=new ItemStack(kind.item());var data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(data.size()==2 && data.getIntOr(kind.flag(),0)==1 && data.getIntOr("drop",0)==1,"exact source flags "+kind);
                require(LbcScareDropItem.matches(s,kind) && LbcScareDropItem.kind(s)==kind,"typed recognition "+kind);
                var table=kind==LbcScareDropItem.Kind.OLD?"lbc:ported/scare_drop_old":"lbc:ported/scare_drop";
                var loot=game.lbc().rollTable(level,origin,alpha,table);
                require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(s,loot.getFirst()),"complete public loot/prototype equality "+kind);
                if(kind==LbcScareDropItem.Kind.NORMAL){
                    require(Identifier.parse("lbc:drop_trap").equals(s.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_ff").equals(s.get(DataComponents.TOOLTIP_STYLE)),"normal source model/tier");
                    require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals("trap_screamer") && !s.getHoverName().getStyle().isItalic() && !s.getHoverName().getStyle().isBold() && s.getHoverName().getStyle().getColor().getValue()==0xffff55,"yellow nonitalic source name");
                    var lore=s.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();
                    require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"white nonitalic source lore");
                }else require(Identifier.parse("minecraft:barrier").equals(s.get(DataComponents.ITEM_MODEL)) && !s.has(DataComponents.CUSTOM_NAME) && !s.has(DataComponents.TOOLTIP_STYLE) && s.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines().isEmpty(),"plain old barrier");
                game.command(kind==LbcScareDropItem.Kind.OLD?"give TestAlpha skywars2:scare_drop_old 1":"give TestAlpha skywars2:scare_drop 1");
            }
            for(var vanilla:List.of(Items.BARRIER,Items.REPEATING_COMMAND_BLOCK,Items.PAPER))require(LbcScareDropItem.kind(new ItemStack(vanilla))==null,"vanilla lookalike rejected");
        }
        void effect(ServerPlayer p,int duration){
            var e=p.getEffect(MobEffects.BLINDNESS);require(e!=null && e.getDuration()==duration && e.getAmplifier()==10 && !e.isAmbient() && !e.isVisible() && !e.showIcon(),"exact hidden blindness duration/amplifier "+duration);
        }
        void dispatch(LbcScareDropItem.Kind kind){
            beta.removeEffect(MobEffects.BLINDNESS);alpha.removeEffect(MobEffects.BLINDNESS);
            var otherEffects=new LinkedHashMap<ServerPlayer,String>();for(var p:level.players())if(p!=beta){var e=p.getEffect(MobEffects.BLINDNESS);otherEffects.put(p,e==null?"none":e.getDuration()+":"+e.getAmplifier());}
            var t=drop(kind,2);require(game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"one dispatch/debit "+kind);effect(beta,kind==LbcScareDropItem.Kind.OLD?200:120);
            for(var entry:otherEffects.entrySet()){var e=entry.getKey().getEffect(MobEffects.BLINDNESS);require(entry.getValue().equals(e==null?"none":e.getDuration()+":"+e.getAmplifier()),"every other player retains prior blindness state");}
            require(!alpha.hasEffect(MobEffects.BLINDNESS),"thrower outside nearest target receives no blindness");
            require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"duplicate dispatch same tick rejected");t.discard();
        }
        void guards(){
            var fake=token(level,origin,new ItemStack(Items.PAPER),alpha);tokens.add(fake);require(!game.items().specialDrops().spawn(fake),"unrecognized lookalike rejected");fake.discard();
            var visual=drop(LbcScareDropItem.Kind.NORMAL,2);visual.makeFakeItem();require(!game.items().specialDrops().spawn(visual) && visual.getItem().getCount()==2,"native fake cannot debit");visual.discard();
            var stale=drop(LbcScareDropItem.Kind.NORMAL,2);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().specialDrops().spawn(stale) && stale.getItem().getCount()==2,"stale epoch cannot debit");stale.discard();
            var remote=drop(LbcScareDropItem.Kind.NORMAL,2);int x=(BlockPos.containing(origin).getX()>>4)+128,z=(BlockPos.containing(origin).getZ()>>4)+128;
            while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;remote.setPos(x*16+8,origin.y,z*16+8);
            require(!game.items().specialDrops().spawn(remote) && remote.getItem().getCount()==2 && level.getChunkSource().getChunkNow(x,z)==null,"unresident token cannot synchronously load/debit");remote.discard();
        }
        void targets(){
            var t=drop(LbcScareDropItem.Kind.NORMAL,1);
            alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);
            require(game.items().cageDrops().target(t)==alpha,"nearest includes actual thrower");
            alpha.removeEffect(MobEffects.BLINDNESS);require(game.items().specialDrops().spawn(t),"self activation");effect(alpha,120);t.discard();alpha.removeEffect(MobEffects.BLINDNESS);
            t=drop(LbcScareDropItem.Kind.NORMAL,1);alpha.setGameMode(GameType.SPECTATOR);
            require(game.items().cageDrops().target(t)==beta,"native Spectator excluded");alpha.setGameMode(GameType.SURVIVAL);alpha.noPhysics=false;
            game.spectators().enter(alpha);try{alpha.setGameMode(GameType.SURVIVAL);require(game.isObserver(alpha) && game.items().cageDrops().target(t)==beta,"explicit observer role excluded in Survival");}finally{game.spectators().remove(alpha);alpha.setGameMode(GameType.SURVIVAL);alpha.setNoGravity(true);alpha.getInventory().clearContent();}
            for(var tag:List.of("spectator","rcdenyalways","sauvojen_tuntija","strict_map_object","magic_academy","transformator")){
                alpha.entityTags().add(tag);try{require(game.items().cageDrops().target(t)==beta,"protected observer/service excluded "+tag);}finally{alpha.entityTags().remove(tag);}
            }
            t.discard();place();
            var board=game.server().getScoreboard();var team=board.addPlayerTeam("codex_scare_probe");
            var alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());var betaTeam=board.getPlayersTeam(beta.getScoreboardName());
            try{
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                dispatch(LbcScareDropItem.Kind.NORMAL);require(beta.hasEffect(MobEffects.BLINDNESS),"nearest ally affected");
            }finally{board.removePlayerTeam(team);if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);}
            beta.setGameMode(GameType.CREATIVE);try{dispatch(LbcScareDropItem.Kind.NORMAL);}finally{beta.setGameMode(GameType.SURVIVAL);}
            beta.removeEffect(MobEffects.BLINDNESS);
        }
        void transactions(){
            beta.removeEffect(MobEffects.BLINDNESS);var both=drop(LbcScareDropItem.Kind.NORMAL,2);var flags=both.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();flags.putInt("skrimmer_old",1);both.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
            require(game.items().specialDrops().spawn(both) && both.getItem().getCount()==1,"both flags dispatch with one debit");effect(beta,200);both.discard();beta.removeEffect(MobEffects.BLINDNESS);
            var modes=new LinkedHashMap<ServerPlayer,GameType>();for(var p:level.players())modes.put(p,p.gameMode.getGameModeForPlayer());
            try{for(var p:modes.keySet())p.setGameMode(GameType.SPECTATOR);var empty=drop(LbcScareDropItem.Kind.OLD,2);
                require(game.items().cageDrops().target(empty)==null && game.items().specialDrops().spawn(empty) && empty.getItem().getCount()==1,"no eligible target still debits once");
                require(!alpha.hasEffect(MobEffects.BLINDNESS) && !beta.hasEffect(MobEffects.BLINDNESS),"no eligible target has no blindness");empty.discard();
            }finally{for(var e:modes.entrySet()){e.getKey().setGameMode(e.getValue());e.getKey().noPhysics=false;}}
            place();
        }
        void cleansing(){
            for(int type:List.of(5,6)){
                var artifact=new ItemStack(LbcMagic.MYSTICAL_ARTIFACT);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("mystical",1);flags.putInt("type",type);artifact.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
                beta.removeAllEffects();beta.getInventory().setItem(0,artifact);game.items().magic().inventoryChanged(beta);
                try{for(var kind:LbcScareDropItem.Kind.values()){
                    var t=drop(kind,1);require(game.items().specialDrops().spawn(t),"cleansing does not reject consumption "+type+"/"+kind);
                    require(!beta.hasEffect(MobEffects.BLINDNESS),"native immediate cleansing rejects blindness before delayed tick "+type+"/"+kind);t.discard();
                }}finally{beta.getInventory().clearContent();game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();}
            }
            suppressor=new Marker(EntityTypes.MARKER,level);suppressor.setPos(beta.position());suppressor.entityTags().add("stopper_magic");require(level.addFreshEntity(suppressor) && game.items().magic().suppressed(beta),"native antimagic fixture");
            try{dispatch(LbcScareDropItem.Kind.NORMAL);}finally{suppressor.discard();suppressor=null;beta.removeEffect(MobEffects.BLINDNESS);}
        }
        net.minecraft.world.entity.animal.cow.Cow cow(){var c=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);c.setPos(origin.add(0,4,0));c.setNoAi(true);c.setNoGravity(true);require(level.addFreshEntity(c),"native causal victim");return c;}
        void brokenTotem(){
            require(game.items().curseTicks(alpha)==0,"clean curse baseline for native totem regression");
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:kit/broken_totem");
            require(loot.size()==1 && loot.getFirst().is(Items.TOTEM_OF_UNDYING) && loot.getFirst().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("broken_totem",0)==1,"actual broken-totem acquisition leaf");
            int lives=game.lives(alpha.getUUID());
            try{alpha.removeAllEffects();alpha.setHealth(alpha.getMaxHealth());alpha.setAbsorptionAmount(0);alpha.damageCooldownTime=0;alpha.setInvulnerableTime(0);alpha.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,loot.getFirst());
                alpha.hurtServer(level,alpha.damageSources().generic(),10000);
                require(alpha.isAlive() && alpha.getHealth()==1 && alpha.getOffhandItem().isEmpty() && game.lives(alpha.getUUID())==lives,"native broken totem consumed and prevents lethal damage");
                require(game.items().curseTicks(alpha)==100 && alpha.hasEffect(MobEffects.REGENERATION) && !alpha.hasEffect(MobEffects.BLINDNESS),"existing two-argument screamer is cleansed by native death protection and applies curse100");
            }finally{game.items().clear(alpha);alpha.removeAllEffects();alpha.setHealth(alpha.getMaxHealth());alpha.setAbsorptionAmount(0);alpha.damageCooldownTime=0;alpha.setInvulnerableTime(0);}
        }
        void causalFalls(){
            for(boolean voidDeath:List.of(false,true)){
                var c=cow();try{game.items().screamer(c,alpha,false);require(c.hasEffect(MobEffects.BLINDNESS),"shared helper sourced blindness accepted");c.setHealth(1);c.damageCooldownTime=0;
                    c.hurtServer(level,voidDeath?c.damageSources().fellOutOfWorld():c.damageSources().fall(),1000);
                    require(c.isDeadOrDying() && c.getKillCredit()==alpha && c.getLastDamageSource()!=null && c.getLastDamageSource().getEntity()==null,"native blindness "+(voidDeath?"void":"fall")+" death retains thrower/environment source");
                }finally{game.items().indirectKills().clear(c);c.discard();}
            }
            for(int scenario=0;scenario<5;scenario++){
                var c=cow();try{
                    require(c.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,10,false,false),alpha),"native original cause");
                    if(scenario==0){c.removeEffect(MobEffects.BLINDNESS);}
                    if(scenario==1){c.hurtServer(level,c.damageSources().playerAttack(beta),1);}
                    if(scenario==2)require(!c.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,10,false,false),beta),"native identical effect rejected");
                    if(scenario==3)require(!c.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,9,false,false),beta),"native weaker effect rejected");
                    if(scenario==4){game.items().screamer(c,beta,false);require(c.getEffect(MobEffects.BLINDNESS).getDuration()==200,"normal120 cannot replace old200 blindness");}
                    c.setHealth(1);c.damageCooldownTime=0;c.setInvulnerableTime(0);c.hurtServer(level,c.damageSources().fall(),1000);
                    require(c.isDeadOrDying() && (scenario==0?c.getKillCredit()!=alpha:c.getKillCredit()==(scenario==1?beta:alpha)),"removal invalidates / newer attacker wins / rejected effect preserves cause "+scenario);
                }finally{game.items().indirectKills().clear(c);c.discard();}
            }
        }
        boolean step(){
            require(game.playing(),"actual match retained");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(0),new ItemStack(LbcScareDropItem.Kind.NORMAL.item())) && ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(1),new ItemStack(LbcScareDropItem.Kind.OLD.item())),"native give prototypes");alpha.getInventory().clearContent();guards();targets();transactions();cleansing();brokenTotem();causalFalls();beta.removeEffect(MobEffects.BLINDNESS);stack=drop(LbcScareDropItem.Kind.NORMAL,2);phase++;}
                case 1->{require(stack.getItem().getCount()==1,"one unit consumed on first actual server tick");require(beta.hasEffect(MobEffects.BLINDNESS) && !alpha.hasEffect(MobEffects.BLINDNESS),"actual tick affects nearest player only");require(!game.items().specialDrops().spawn(stack) && stack.getItem().getCount()==1,"same tick cannot repeat");beta.removeEffect(MobEffects.BLINDNESS);phase++;}
                case 2->{require(stack.isRemoved() && beta.hasEffect(MobEffects.BLINDNESS),"next server tick consumes final unit");beta.removeEffect(MobEffects.BLINDNESS);dispatch(LbcScareDropItem.Kind.OLD);frozenDuration=beta.getEffect(MobEffects.BLINDNESS).getDuration();game.items().timeStop().begin(level,200);paused=drop(LbcScareDropItem.Kind.NORMAL,2);wait=0;phase++;}
                case 3->{require(paused.getItem().getCount()==2 && beta.getEffect(MobEffects.BLINDNESS).getDuration()==frozenDuration,"time stop pauses pending debit and blindness duration");if(++wait==3){paused.discard();game.items().timeStop().end(level);beta.removeEffect(MobEffects.BLINDNESS);unloaded=drop(LbcScareDropItem.Kind.OLD,2);game.items().specialDrops().unloaded(unloaded);phase++;}}
                case 4->{require(!unloaded.isRemoved() && unloaded.getItem().getCount()==2 && !beta.hasEffect(MobEffects.BLINDNESS),"unloaded pending token cannot replay");unloaded.discard();return true;}
                default->throw new IllegalStateException("Unknown scare phase "+phase);
            }return false;
        }
        public void close(){game.items().timeStop().end(level);if(suppressor!=null)suppressor.discard();tokens.forEach(Entity::discard);game.items().specialDrops().stop();
            var leftovers=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e) && !(e instanceof ServerPlayer) && Math.abs(e.getX()-origin.x)<6 && Math.abs(e.getZ()-origin.z)<6)leftovers.add(e);leftovers.forEach(Entity::discard);
            alphaState.restore(game);betaState.restore(game);game.items().magic().inventoryChanged(alpha);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();
            alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaKills);beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaKills);
        }
    }
    private static void closeFailure(State state,Throwable failure){try{state.close();}catch(Throwable cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}finally{running=null;}}
    public static void run(GameController game){require(running==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error failure){closeFailure(running,failure);throw failure;}}
    public static void tick(GameController game){if(running==null)return;var state=running;try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS scare-drops");}}catch(Throwable failure){closeFailure(state,failure);SkyWars2.LOGGER.error("[check] FAIL scare-drops",failure);}}
    public static void stopProbe(GameController game){require(game.playing(),"actual match stop probe");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));game.items().timeStop().end(level);stopEpoch=game.items().epoch();game.items().timeStop().begin(level,200);stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),new ItemStack(LbcScareDropItem.Kind.OLD.item(),3),alpha);require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"pending scare retained under stop");SkyWars2.LOGGER.info("[check] PASS scare-drops-stop-probe");}
    public static void cleanup(GameController game){require(!game.items().currentEpoch(stopEpoch) && stopToken!=null && stopToken.getItem().getCount()==3,"actual match stop invalidates pending scare epoch without debit");require(!game.items().specialDrops().spawn(stopToken),"stopped match cannot replay pending scare");stopToken.discard();stopToken=null;stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS scare-drops-cleanup");}
}
