package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Actual vanilla lightning ticks and native wand casts; no simulated damage policy. */
public final class LightningRulesChecks {
    private LightningRulesChecks() { }
    private static void require(boolean ok,String message) {
        if(!ok) throw new IllegalStateException("Lightning rules: "+message);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerPlayer owner,ally;
        final GameChecks.WandThirdPlayerState ownerState,allyState;
        final net.minecraft.world.scores.PlayerTeam team,oldOwnerTeam,oldAllyTeam;
        final Set<Entity> initial=new HashSet<>();
        final ArrayList<LivingEntity> targets=new ArrayList<>();
        final Vec3 origin;
        Fixture(GameController game) {
            this.game=game;
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            ally=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && owner!=null && ally!=null && game.allowDamage(owner) && game.allowDamage(ally),"two active participants required");
            ownerState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            allyState=new GameChecks.WandThirdPlayerState(ally,game.items().mana());
            oldOwnerTeam=owner.getTeam(); oldAllyTeam=ally.getTeam();
            var board=game.server().getScoreboard(); team=board.addPlayerTeam("codex_lightning_rules");
            team.setAllowFriendlyFire(false);
            board.addPlayerToTeam(owner.getScoreboardName(),team); board.addPlayerToTeam(ally.getScoreboardName(),team);
            origin=new Vec3(Math.floor(owner.getX()/16)*16+8.5,280,Math.floor(owner.getZ()/16)*16+8.5);
            owner.level().getAllEntities().forEach(initial::add);
            for(var player:new ServerPlayer[]{owner,ally}) {
                player.closeContainer(); player.getInventory().clearContent(); player.removeAllEffects();
                player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setShiftKeyDown(false);
                player.getAbilities().invulnerable=false; player.entityTags().remove("time_stopped");
                player.teleportTo(owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);
                player.hasChangedDimension(); game.items().mana().noMagic(player,0); ready(player);
                targets.add(player);
            }
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) owner.level().getChunk((int)Math.floor(origin.x/16)+x,(int)Math.floor(origin.z/16)+z);
            game.items().bolts().stop();
        }
        Cow cow(double x,double z,boolean allied) {
            var cow=new Cow(EntityTypes.COW,owner.level()); cow.setNoAi(true); cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); cow.setHealth(1000);
            cow.setPos(origin.add(x,0,z)); require(owner.level().addFreshEntity(cow),"cow spawned");
            if(allied) game.server().getScoreboard().addPlayerToTeam(cow.getScoreboardName(),team);
            targets.add(cow); return cow;
        }
        void move(ServerPlayer player,double x,double z) {
            player.teleportTo(owner.level(),origin.x+x,origin.y,origin.z+z,Set.of(),0,0,true);
            player.hasChangedDimension();
        }
        LightningBolt vanilla(boolean owned) {
            var bolt=EntityTypes.LIGHTNING_BOLT.create(owner.level(),EntitySpawnReason.TRIGGERED);
            require(bolt!=null,"native bolt created"); bolt.setPos(origin); if(owned) bolt.setCause(owner);
            require(owner.level().addFreshEntity(bolt),"native bolt spawned"); return bolt;
        }
        @Override public void close() {
            game.items().bolts().stop();
            if(game.isObserver(ally)) game.spectators().remove(ally);
            var created=new ArrayList<Entity>(); owner.level().getAllEntities().forEach(e->{if(!initial.contains(e)) created.add(e);});
            for(var entity:created) { game.server().getScoreboard().removePlayerFromTeam(entity.getScoreboardName()); entity.discard(); }
            var board=game.server().getScoreboard();
            board.removePlayerFromTeam(owner.getScoreboardName()); board.removePlayerFromTeam(ally.getScoreboardName()); board.removePlayerTeam(team);
            if(oldOwnerTeam!=null) board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);
            if(oldAllyTeam!=null) board.addPlayerToTeam(ally.getScoreboardName(),oldAllyTeam);
            ownerState.restore(game); allyState.restore(game);
        }
    }
    private static void ready(LivingEntity target) {
        target.removeAllEffects(); target.setHealth(target.getMaxHealth()); target.setAbsorptionAmount(0);
        target.setInvulnerableTime(0); target.damageCooldownTime=0; target.setRemainingFireTicks(0);
    }
    private static void laterFire(Fixture f,LivingEntity target,String label) {
        target.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0); target.setLastHurtByMob(null);
        require(target.getKillCredit()==null,label+" direct combat credit cleared"+diagnostic(target));
        target.setInvulnerableTime(0); target.damageCooldownTime=0;
        float hp=target.getHealth();
        require(target.hurtServer(f.owner.level(),target.damageSources().onFire(),1) && target.getHealth()<hp
                && target.getKillCredit()==(target==f.owner?null:f.owner),label+" actual later fire damage retains friendly fire and true owner"+diagnostic(target));
    }
    private static String diagnostic(LivingEntity target) {
        var source=target.getLastDamageSource();
        return " [type="+target.getType()+", hp="+target.getHealth()+"/"+target.getMaxHealth()
                +", fire="+target.getRemainingFireTicks()+", onFire="+target.isOnFire()+", fireImmune="+target.fireImmune()
                +", cooldown="+target.damageCooldownTime+", source="+source
                +", owner="+(source==null?null:source.getEntity())+", direct="+(source==null?null:source.getDirectEntity())+"]";
    }
    public static void run(GameController game) {
        try(var f=new Fixture(game)) {
            var friendly=f.cow(0,1,true); var enemy=f.cow(-1,0,false); var npc=f.cow(0,2,false); npc.entityTags().add("magic_academy");
            var summon=game.items().summons().spawnPreset(f.owner,"vex",f.origin.add(1,0,1));
            require(summon!=null && game.items().summons().owner(summon)==f.owner,"real friendly summon retains owner");
            summon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); summon.setHealth(1000); summon.setNoAi(true); summon.setNoGravity(true); f.targets.add(summon);
            f.move(f.ally,1,0);
            require(!f.owner.canHarmPlayer(f.ally),"native scoreboard friendly fire disabled");
            for(int mode=0;mode<3;mode++) {
                for(var target:f.targets) ready(target);
                var bolt=mode==2?game.items().electro().lightning(f.owner,f.origin):f.vanilla(mode==1);
                require(bolt!=null,"strike exists"); bolt.tick();
                var struck=new LivingEntity[]{f.owner,f.ally,friendly,enemy,summon};
                var healthAfterFirst=new java.util.IdentityHashMap<LivingEntity,Float>();
                for(var target:struck) {
                    require(target.getHealth()<target.getMaxHealth(),"mode "+mode+" damages every eligible target"+diagnostic(target));
                    // Vanilla Vex is fireImmune: lightning still hurts it, but isOnFire stays false.
                    require(target.isOnFire()==!target.fireImmune(),"mode "+mode+" preserves native ignition and fire immunity"+diagnostic(target));
                    var source=target.getLastDamageSource();
                    require(source!=null && (mode==2?source.typeHolder().is(LbcDamage.LIGHTNING):source.is(DamageTypes.LIGHTNING_BOLT)),"native source type preserved"+diagnostic(target));
                    if(mode>0) require(source.getEntity()==f.owner,"native strike retains true owner"+diagnostic(target));
                    healthAfterFirst.put(target,target.getHealth());
                }
                // One immediate second active native tick tests all victims in the same hurt window.
                bolt.tick();
                for(var target:struck) require(target.getHealth()==healthAfterFirst.get(target),
                        "repeated native tick respects normal hurt cooldown"+diagnostic(target));
                require(npc.getHealth()==npc.getMaxHealth() && !npc.isOnFire(),"service NPC protected in every lightning mode"+diagnostic(npc));
                if(mode>0) { laterFire(f,f.ally,"allied player mode "+mode); laterFire(f,friendly,"allied cow mode "+mode); laterFire(f,f.owner,"caster mode "+mode); }
                bolt.discard();
            }
            f.move(f.owner,10,0); ready(f.ally); game.spectators().enter(f.ally);
            for(boolean managed:new boolean[]{false,true}) {
                var bolt=managed?game.items().electro().lightning(f.owner,f.origin):f.vanilla(false); bolt.tick(); bolt.discard();
                require(f.ally.getHealth()==f.ally.getMaxHealth() && !f.ally.isOnFire(),"observer remains protected"+diagnostic(f.ally));
            }
            game.spectators().remove(f.ally); f.ally.setGameMode(GameType.SURVIVAL); f.move(f.ally,1,0);
            // Full Techno protection is electrical only; ordinary vanilla lightning stays vanilla.
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
                var stack=new ItemStack(slot==EquipmentSlot.HEAD?Items.IRON_HELMET:slot==EquipmentSlot.CHEST?Items.IRON_CHESTPLATE:slot==EquipmentSlot.LEGS?Items.IRON_LEGGINGS:Items.IRON_BOOTS);
                var data=new net.minecraft.nbt.CompoundTag(); data.putInt("techno",1); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data)); f.ally.setItemSlot(slot,stack);
            }
            f.ally.doTick(); ready(f.ally);
            var managed=game.items().electro().lightning(f.owner,f.origin); managed.tick(); managed.discard();
            require(f.ally.getHealth()==f.ally.getMaxHealth(),"managed lightning preserves full Techno immunity"+diagnostic(f.ally));
            ready(f.ally); var weather=f.vanilla(false); weather.tick(); weather.discard();
            require(f.ally.getHealth()<f.ally.getMaxHealth(),"unmanaged vanilla lightning does not acquire electrical armor immunity"+diagnostic(f.ally));
        }
        SkyWars2.LOGGER.info("[check] PASS lightning-rules");
    }
    private static void cast(Fixture f) {
        var stack=new ItemStack(LbcWands.ACADEMY_LIGHTNING_BOLTS);
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); data.putInt("upgraded",1); data.putInt("mode",3);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data)); f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
        f.game.items().mana().set(f.owner,9000); require(f.game.items().wands().use(f.owner,InteractionHand.MAIN_HAND),"native Chain wand cast accepted");
    }
    public static void chain(GameController game) {
        try(var f=new Fixture(game)) {
            f.move(f.ally,0,8);
            var allied=f.cow(0,20,true); var outside=f.cow(0,25,true);
            var npc=f.cow(2,9,true); npc.entityTags().add("magic_academy");
            var suppressed=f.cow(-2,10,true); suppressed.entityTags().add("nomagic_active");
            cast(f); var bolts=game.items().bolts();
            require(bolts.hasChain(f.ally) && !bolts.hasChain(f.owner) && !bolts.hasChain(allied),"initial ray hits allied player ahead and seeds only its radius3");
            for(int tick=0;tick<40;tick++) bolts.tick();
            require(bolts.hasChain(f.owner) && bolts.hasChain(allied) && !bolts.hasChain(outside),"Chain radius16 reaches caster and allied mob, excludes target17 away");
            require(!bolts.hasChain(npc) && !bolts.hasChain(suppressed),"Chain preserves service NPC and magic suppression guards");
            for(int tick=0;tick<40;tick++) bolts.tick();
            require(bolts.hasChain(f.ally) && bolts.hasChain(outside),"Chain revisits allied player and reaches next allied mob");
            for(var target:f.targets) bolts.clearEffects(target);
            require(bolts.chainCount()==0,"cleansing every affected target ends chain");
            cast(f); require(bolts.hasChain(f.ally),"Chain can restart after cleansing");
            game.spectators().enter(f.ally); bolts.tick();
            require(!bolts.hasChain(f.ally) && bolts.chainCount()==0,"observer transition removes sole initial Chain target");
            game.spectators().remove(f.ally); f.ally.setGameMode(GameType.SURVIVAL); f.move(f.ally,0,8);
            cast(f); require(bolts.chainCount()>0,"chain live before owner lifecycle reset");
            bolts.forget(f.owner); require(bolts.chainCount()==0,"owner reset cancels Chain state");
        }
        SkyWars2.LOGGER.info("[check] PASS lightning-rules-chain");
    }
}
