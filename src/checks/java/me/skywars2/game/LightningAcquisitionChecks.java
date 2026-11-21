package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Native lightning ticks, real loot and actual server ticks; no test clock or alternate converter. */
public final class LightningAcquisitionChecks {
    private LightningAcquisitionChecks() { }
    private static State running;
    private static ItemEntity stopInput;
    private static String stopEpoch;
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Lightning acquisition: "+message);
    }
    private static ServerPlayer player(GameController game,String name) {
        var result=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name));
        require(result!=null,"requires "+name); return result;
    }
    private static ItemStack donor(int count,boolean typed,String... flags) {
        var result=new ItemStack(Items.STICK,count); var data=new CompoundTag();
        data.putInt("transformable",1); if(typed) data.putString("type","test-carrier");
        for(String flag:flags) data.putInt(flag,1);
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(data)); return result;
    }
    private static ItemEntity drop(ServerLevel level,Vec3 at,ItemStack stack) {
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);
        item.setNoGravity(true); item.setDeltaMovement(Vec3.ZERO); item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native input added"); return item;
    }
    private static LightningBolt strike(ServerLevel level,Vec3 at,ServerPlayer cause) {
        var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
        require(bolt!=null,"native lightning created"); bolt.setPos(at); if(cause!=null) bolt.setCause(cause);
        require(level.addFreshEntity(bolt),"native lightning added"); bolt.tick(); return bolt;
    }
    private static int count(ServerPlayer player) {
        int total=0; for(int i=0;i<player.getInventory().getContainerSize();i++) total+=player.getInventory().getItem(i).getCount(); return total;
    }
    private static void flush(GameController game) { game.items().lightningAcquisition().flushQueued(); }
    private static void mapping() {
        String[] flags={"accessory","axe","bow","chestplate","hoe","pickaxe","potion","shield","shooter","shovel","staff","sword","totem","spellitem","summoner","boots","leggings","helmet","trident","arcanum","metasola"};
        String[] tables={"transformables_accessory","transformables_axe","transformables_bow","transformables_chestplate","transformables_hoe","transformables_pickaxe","transformables_potion","transformables_shield","transformables_shooter","transformables_shovel","transformables_staff","transformables_sword","transformables_totem","transformables_spells","transformables_summons","transformables_boots","transformables_leggings","transformables_helmet","transformables_trident","transformables_arcanum","halloween/metasola"};
        for(int i=0;i<flags.length;i++) require(LbcBolts.lightningReforgeTables(donor(1,false,flags[i])).equals(List.of(tables[i])),"source category "+flags[i]);
        require(LbcBolts.lightningReforgeTables(donor(1,false,flags)).equals(List.of(tables)),"21 independent source flags in source order");
        require(LbcBolts.lightningReforgeTables(donor(64,false,"libspell","moisei","randchanted")).isEmpty(),"wand-only categories excluded");
        require(LbcBolts.lightningReforgeTables(new ItemStack(Items.ARROW,64)).isEmpty(),"no Ultra arrow exception");
    }
    private static final class State implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaSaved,betaSaved;
        final Set<Entity> initial=new HashSet<>();
        final ArrayList<ItemEntity> inputs=new ArrayList<>();
        final ArrayList<ItemStack> accepted=new ArrayList<>();
        final Vec3 at=new Vec3(72.5,280,72.5);
        final net.minecraft.advancements.AdvancementHolder advancement;
        final ArrayList<String> alphaCriteria=new ArrayList<>(),betaCriteria=new ArrayList<>();
        int phase,startedTick,burstBefore,drainTicks;
        ItemEntity input;
        ItemStack identity;
        State(GameController game) {
            this.game=game; level=game.maps().level(); alpha=player(game,"TestAlpha"); beta=player(game,"TestBeta");
            require(game.playing() && game.allowDamage(alpha) && game.allowDamage(beta),"two active match players");
            alphaSaved=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());
            betaSaved=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            level.getAllEntities().forEach(initial::add);
            advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/lightning_smith"));
            require(advancement!=null,"source advancement exists");
            alpha.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(alphaCriteria::add);
            beta.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(betaCriteria::add);
            for(var p:List.of(alpha,beta)) {
                p.closeContainer(); p.getInventory().clearContent(); p.removeAllEffects(); p.setNoGravity(true);
                p.setGameMode(GameType.SURVIVAL); p.entityTags().remove("time_stopped");
                for(String criterion:advancement.value().criteria().keySet()) p.getAdvancements().revoke(advancement,criterion);
            }
            move(alpha,12); move(beta,6);
            for(int x=3;x<=6;x++) for(int z=3;z<=6;z++) level.getChunk(x,z);
            for(String id:List.of("phantom_knives","mobgun","kfc_gun","razorpine","attack_helicopter","laser_minigun","woodificator"))
                accepted.addAll(game.lbc().rollTable(level,at,alpha,"lbc:ported/"+id));
            accepted.add(new ItemStack(Items.WHEAT_SEEDS));
            reset(); startedTick=game.server().getTickCount();
        }
        void move(ServerPlayer player,double offset) { player.teleportTo(level,at.x+offset,at.y,at.z,Set.of(),0,0,true); player.hasChangedDimension(); }
        ItemEntity input(ItemStack stack) { var item=drop(level,at,stack); inputs.add(item); return item; }
        void bolts() { var remove=new ArrayList<Entity>(); level.getAllEntities().forEach(e->{if(!initial.contains(e) && e instanceof LightningBolt) remove.add(e);}); remove.forEach(Entity::discard); }
        void reset() {
            bolts(); game.items().lightningAcquisition().stop(); flush(game);
            inputs.forEach(Entity::discard); inputs.clear(); alpha.getInventory().clearContent(); beta.getInventory().clearContent();
            var outputs=new ArrayList<ItemEntity>();
            level.getAllEntities().forEach(e->{if(e instanceof ItemEntity item && !initial.contains(item)) outputs.add(item);});
            outputs.forEach(Entity::discard);
        }
        void preserved(ItemEntity item,int units,String label) {
            require(!item.isRemoved() && item.getItem().getCount()==units && item.getItem()==identity,label+" retains exact stack identity/count");
        }
        void nativeShooter() {
            int weapons=0;
            for(int slot=0;slot<beta.getInventory().getContainerSize();slot++) {
                var stack=beta.getInventory().getItem(slot); if(stack.isEmpty()) continue;
                require(accepted.stream().anyMatch(ref->ItemStack.isSameItemSameComponents(ref,stack)),"actual native shooter full components");
                if(!stack.is(Items.WHEAT_SEEDS)) weapons+=stack.getCount();
            }
            require(weapons==1,"one actual native weapon per unit");
        }
        boolean step() {
            require(game.playing(),"match remains active through deferred suite");
            if(game.server().getTickCount()==startedTick) return false;
            startedTick=game.server().getTickCount();
            switch(phase++) {
                case 0 -> {
                    input=input(donor(5,false,"shooter")); identity=input.getItem();
                    var first=strike(level,at,alpha); var second=strike(level,at,null);
                    first.tick(); second.tick(); flush(game);
                    preserved(input,4,"untyped native lightning, repeated ticks and overlapping ownerless/owned bolts");
                    require(!input.isInvulnerable(),"no global item invulnerability"); nativeShooter();
                    require(count(alpha)==0 && beta.getAdvancements().getOrStartProgress(advancement).isDone()
                            && !alpha.getAdvancements().getOrStartProgress(advancement).isDone(),"nearest eligible recipient and advancement ignore caster");
                }
                case 1 -> {
                    // The surviving real bolt must capture another unit on the next server tick.
                    game.items().lightningAcquisition().tick(); flush(game); preserved(input,3,"persistent lightning next source tick");
                    reset();
                    input=input(donor(3,true,"shooter")); identity=input.getItem(); strike(level,at,null); flush(game);
                    preserved(input,2,"typed carrier also transforms under actual ownerless lightning"); nativeShooter();
                }
                case 2 -> {
                    reset(); game.spectators().enter(beta);
                    var observerInventory=new ArrayList<ItemStack>();
                    for(int slot=0;slot<beta.getInventory().getContainerSize();slot++) observerInventory.add(beta.getInventory().getItem(slot).copy());
                    input=input(donor(2,false,"shooter")); strike(level,at,null); flush(game);
                    require(input.getItem().getCount()==1 && count(alpha)>0,"near observer excluded in favor of active player");
                    for(int slot=0;slot<observerInventory.size();slot++) {
                        var before=observerInventory.get(slot); var after=beta.getInventory().getItem(slot);
                        require(before.getCount()==after.getCount() && ItemStack.isSameItemSameComponents(before,after),"observer retains only its native spectator tools");
                    }
                    game.spectators().remove(beta); beta.setGameMode(GameType.SURVIVAL); move(beta,6);
                }
                case 3 -> {
                    reset(); var nether=game.server().getLevel(Level.NETHER); require(nether!=null,"native wrong-world fixture available");
                    var foreign=drop(nether,at,donor(2,true,"shooter"));
                    try { var bolt=strike(nether,at,null); flush(game); require(foreign.getItem().getCount()==2,"wrong-world native bolt cannot acquire"); bolt.discard(); }
                    finally { foreign.discard(); }
                    // All actual online participants become spectators: no fabricated offline recipient.
                    var modes=new java.util.HashMap<ServerPlayer,GameType>();
                    for(var p:level.players()) modes.put(p,p.gameMode.getGameModeForPlayer());
                    try {
                        modes.keySet().forEach(p->p.setGameMode(GameType.SPECTATOR));
                        input=input(donor(2,false,"shooter")); identity=input.getItem(); strike(level,at,null); flush(game);
                        preserved(input,2,"no recipient preserves ordinary item through native damage");
                    } finally { modes.forEach(ServerPlayer::setGameMode); }
                }
                case 4 -> {
                    reset(); input=input(donor(3,false,"libspell")); identity=input.getItem(); strike(level,at,null); flush(game);
                    preserved(input,3,"unsupported native category"); require(game.items().lightningAcquisition().pendingCount()==0,"unsupported terminates without job");
                    // Native damage remains live for ordinary unmarked items.
                    var ordinary=input(new ItemStack(Items.PAPER)); var bolt=strike(level,at,null); bolt.discard();
                    require(ordinary.isRemoved(),"ordinary unmarked paper still takes actual native lightning damage");
                }
                case 5 -> {
                    reset(); input=input(donor(3,false,"shooter","metasola")); strike(level,at,null); flush(game);
                    require(input.getItem().getCount()==2 && count(beta)>=2,"independent category rewards consume one unit");
                }
                case 6 -> {
                    reset();
                    for(int i=0;i<12;i++) {
                        var stack=donor(2,i%2==0,"shooter");
                        var data=stack.get(DataComponents.CUSTOM_DATA).copyTag(); data.putInt("check_serial",i);
                        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data)); input(stack);
                    }
                    strike(level,at,null); flush(game);
                    int converted=24-inputs.stream().mapToInt(i->i.getItem().getCount()).sum();
                    require(converted<=8 && game.items().lightningAcquisition().pendingCount()>0,"burst capped at eight units per server tick");
                    burstBefore=converted;
                    // Keep bolts live until the next tick to accumulate source budget behind queued work.
                }
                case 7 -> {
                    game.items().lightningAcquisition().tick(); bolts(); flush(game);
                    require(24-inputs.stream().mapToInt(i->i.isRemoved()?0:i.getItem().getCount()).sum()>=burstBefore,"queued burst progresses");
                }
                case 8 -> {
                    flush(game);
                    if(game.items().lightningAcquisition().pendingCount()==0) {
                        int remaining=inputs.stream().mapToInt(i->i.isRemoved()?0:i.getItem().getCount()).sum();
                        require(remaining<12,"captured pending source ticks accumulate beyond first unit per input"); phase=14;
                    } else {
                        require(++drainTicks<48,"bounded burst eventually drains"); phase=8;
                    }
                }
                case 14 -> {
                    reset(); input=input(donor(4,false,"shooter")); identity=input.getItem();
                    strike(level,at,null); game.items().lightningAcquisition().unloaded(input); flush(game);
                    preserved(input,4,"unload invalidates pending native input");
                }
                case 15 -> {
                    reset(); input=input(donor(4,false,"shooter")); strike(level,at,null);
                    var replacement=input.getItem().copy(); input.setItem(replacement); flush(game);
                    require(input.getItem()==replacement && replacement.getCount()==4,"merge/replacement identity invalidates pending input");
                }
                case 16 -> {
                    reset(); input=input(donor(4,false,"shooter")); identity=input.getItem();
                    game.items().timeStop().begin(level,200); strike(level,at,null); flush(game);
                    preserved(input,4,"stopped real lightning protects untyped input without conversion");
                    require(game.items().lightningAcquisition().pendingCount()==0,"stopped tick captures no source budget");
                }
                case 17 -> {
                    game.items().timeStop().end(level);
                    game.items().lightningAcquisition().tick(); flush(game);
                    preserved(input,3,"resumed persistent lightning acquires one unit");
                    reset();
                    input=input(donor(3,false,"shooter")); identity=input.getItem();
                    var bolt=game.items().electro().lightning(alpha,at); require(bolt!=null,"managed native lightning exists");
                    bolt.tick(); flush(game); preserved(input,2,"managed native lightning uses same ambient acquisition");
                }
                case 18 -> {
                    reset(); move(beta,1);
                    for(int slot=0;slot<beta.getInventory().getContainerSize();slot++) beta.getInventory().setItem(slot,new ItemStack(Items.DIAMOND,64));
                    input=input(donor(3,false,"shooter")); identity=input.getItem();
                    var bolt=strike(level,at,null); flush(game);
                    preserved(input,2,"overflow acquisition consumes one input unit");
                    var outputs=new ArrayList<ItemEntity>();
                    // Only transformable rewards can be recaptured. Ordinary Razor seeds may take native lightning damage.
                    level.getAllEntities().forEach(e->{if(e instanceof ItemEntity item && item!=input && !initial.contains(item)
                            && !item.isRemoved() && LbcLightningAcquisition.transformable(item.getItem())) outputs.add(item);});
                    require(!outputs.isEmpty(),"full inventory uses native transformable item overflow");
                    var snapshots=outputs.stream().map(item->item.getItem().copy()).toList();
                    bolt.tick(); game.items().lightningAcquisition().tick(); flush(game);
                    for(int i=0;i<outputs.size();i++) require(!outputs.get(i).isRemoved()
                            && outputs.get(i).getItem().getCount()==snapshots.get(i).getCount()
                            && ItemStack.isSameItemSameComponents(outputs.get(i).getItem(),snapshots.get(i)),"same source tick never recaptures delivered transformable overflow: "+outputs.get(i).getItem()+" removed="+outputs.get(i).isRemoved());
                    preserved(input,2,"overflow and repeated native tick remain deduplicated"); bolts();
                }
                case 19,20,21,22 -> {
                    reset(); move(beta,1);
                    int scenario=phase-20;
                    boolean creative=scenario>=2,partial=scenario%2==1;
                    beta.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
                    var reward=new ItemStack(Items.WHEAT_SEEDS,16);
                    var data=new CompoundTag(); data.putInt("transformable",1); data.putInt("hoe",1);
                    data.putInt("check_overflow_reward",1); reward.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                    for(int slot=0;slot<beta.getInventory().getContainerSize();slot++)
                        beta.getInventory().setItem(slot,new ItemStack(Items.DIAMOND,64));
                    if(partial) beta.getInventory().setItem(0,reward.copyWithCount(63));
                    var before=new HashSet<Entity>(); level.getAllEntities().forEach(before::add);
                    input=input(donor(3,false,"hoe")); identity=input.getItem();
                    var bolt=strike(level,at,alpha); flush(game);
                    preserved(input,2,"overflow consumes exactly one donor in "+beta.gameMode.getGameModeForPlayer());
                    var outputs=new ArrayList<ItemEntity>();
                    level.getAllEntities().forEach(e->{if(e instanceof ItemEntity item && item!=input
                            && !before.contains(item) && !item.isRemoved()) outputs.add(item);});
                    require(outputs.size()==1,"one overflow stack in scenario "+scenario+": "+outputs.stream().map(ItemEntity::getItem).toList());
                    var output=outputs.getFirst();
                    require(output.getItem().getCount()==(partial?15:16)
                            && ItemStack.isSameItemSameComponents(output.getItem(),reward),"complete native reward/components retained");
                    require(output.position().distanceToSqr(beta.position())<1e-9,"overflow drops at nearest recipient, not bolt or caster");
                    require(output.hasPickUpDelay(),"overflow cannot be picked up immediately");
                    require(beta.getInventory().getItem(0).getCount()==64 && count(alpha)==0,"only available recipient space filled");
                    bolt.tick(); game.items().lightningAcquisition().tick(); flush(game);
                    preserved(input,2,"overflow native bolt remains deduplicated");
                    require(!output.isRemoved() && output.getItem().getCount()==(partial?15:16),"overflow is not immediately reforged again");
                    bolts(); output.discard(); beta.setGameMode(GameType.SURVIVAL);
                }
                default -> { return true; }
            }
            return false;
        }
        public void close() {
            game.items().timeStop().end(level);
            reset();
            var created=new ArrayList<Entity>(); level.getAllEntities().forEach(e->{if(!initial.contains(e) && !(e instanceof ServerPlayer)) created.add(e);}); created.forEach(Entity::discard);
            if(game.isObserver(beta)) game.spectators().remove(beta);
            for(var p:List.of(alpha,beta)) for(String criterion:advancement.value().criteria().keySet()) p.getAdvancements().revoke(advancement,criterion);
            for(String criterion:alphaCriteria) alpha.getAdvancements().award(advancement,criterion);
            for(String criterion:betaCriteria) beta.getAdvancements().award(advancement,criterion);
            alphaSaved.restore(game); betaSaved.restore(game);
        }
    }
    public static void run(GameController game) {
        require(running==null,"suite already running"); mapping(); running=new State(game);
    }
    public static void tick(GameController game) {
        if(running==null) return;
        try { if(running.step()) { running.close(); running=null; SkyWars2.LOGGER.info("[check] PASS lightning-acquisition"); } }
        catch(Throwable error) { try { running.close(); } finally { running=null; } SkyWars2.LOGGER.error("[check] FAIL lightning-acquisition",error); }
    }
    public static void empty(GameController game) {
        try(var f=new State(game)) {
            require(game.lbc().rollTable(f.level,f.at,f.alpha,"lbc:transformables_pickaxe").isEmpty(),"real isolated empty pickaxe override is active");
            var item=f.input(donor(5,false,"pickaxe")); f.identity=item.getItem(); strike(f.level,f.at,null); flush(game);
            f.preserved(item,5,"empty actual native loot"); require(count(f.alpha)==0 && count(f.beta)==0,"empty loot grants nothing");
        }
        SkyWars2.LOGGER.info("[check] PASS lightning-acquisition-empty");
    }
    public static void stopProbe(GameController game) {
        var owner=player(game,"TestAlpha"); var level=game.maps().level();
        require(game.playing() && game.allowDamage(owner),"active stop probe");
        game.items().lightningAcquisition().stop(); stopEpoch=game.items().epoch();
        stopInput=drop(level,owner.position().add(0,12,0),donor(8,false,"shooter"));
        var bolt=strike(level,stopInput.position(),null); bolt.discard();
        require(game.items().lightningAcquisition().pendingCount()>0,"real stop probe queued");
        game.items().timeStop().begin(level,200);
        SkyWars2.LOGGER.info("[check] PASS lightning-acquisition-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(!game.items().currentEpoch(stopEpoch) && game.items().lightningAcquisition().pendingCount()==0,"real match stop clears old epoch work");
        require(stopInput==null || stopInput.isRemoved() || stopInput.getItem().getCount()==8,"stop never consumes stale pending input");
        if(stopInput!=null) stopInput.discard(); stopInput=null; stopEpoch=null;
        SkyWars2.LOGGER.info("[check] PASS lightning-acquisition-cleanup");
    }
}
