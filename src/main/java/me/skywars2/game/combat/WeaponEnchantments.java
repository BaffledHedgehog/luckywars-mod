package me.skywars2.game.combat;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import me.skywars2.game.modes.legacy.VanillaExpansionRules;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import me.skywars2.game.items.tmt.LbcTmtAbilities;
import me.skywars2.game.items.tmt.LbcTmtTriggers;

/** Item-use and loot snapshots never change projectile control or the physical damage attacker. */
public final class WeaponEnchantments {
    private WeaponEnchantments(){}
    private record CauseKey(UUID player,long order){}
    private static final ReferenceQueue<LbcIndirectKills.Owner> COLLECTED=new ReferenceQueue<>();
    private static final Map<CauseKey,java.util.concurrent.ConcurrentLinkedDeque<CauseReference>> CAUSES=new ConcurrentHashMap<>();
    private static final class CauseReference extends WeakReference<LbcIndirectKills.Owner> {
        final CauseKey key;
        CauseReference(LbcIndirectKills.Owner cause){super(cause,COLLECTED);key=new CauseKey(cause.player(),cause.order());}
    }
    public interface Scope extends AutoCloseable {@Override void close();}
    private record Use(UUID player,ItemStack item){}
    record Capture(ItemStack item,boolean action){}
    private record Loot(LivingEntity killer, ItemStack boundWeapon, List<ItemStack> weapons) {}
    // Enchantment provenance is independent of spell targeting/wild authority.
    // All access occurs on the server thread, like entity publication and checkpoints.
    private static IdentityHashMap<Entity,LbcIndirectKills.Owner> SPELL_ACTORS=new IdentityHashMap<>();
    private static final Map<UUID,LbcIndirectKills.Owner> UNLOADED_SPELL_ACTORS=new HashMap<>();
    private static final ThreadLocal<Use> USING=new ThreadLocal<>();
    private static final ThreadLocal<Use> ACTOR_USING=new ThreadLocal<>();
    private static final ThreadLocal<LbcIndirectKills.Owner> HEAL_EFFECT=new ThreadLocal<>();
    private record NativeFire(LivingEntity target,DamageSource source) {}
    private static final ThreadLocal<NativeFire> FIRE_TICK=new ThreadLocal<>();
    private static final ThreadLocal<Loot> LOOT=new ThreadLocal<>();
    private static final Map<DamageSource,Map<Entity,Receipt>> HITS=new WeakHashMap<>();
    private static final ThreadLocal<Damage> DAMAGE=new ThreadLocal<>();
    private static final class Receipt {
        final ItemStack item;boolean consumed,posted,knockback;
        Receipt(ItemStack item){this.item=item.copyWithCount(1);}
    }
    private static Receipt receipt(DamageSource source,Entity target){
        var targets=HITS.get(LbcIndirectKills.originalDamage(source));return targets==null?null:targets.get(target);
    }
    /** Remember native preprocessing so the shared damage boundary never adds an enchantment twice. */
    public static void prepared(DamageSource source,Entity target,ItemStack weapon){
        if(VanillaExpansionRules.enabled()&&!weapon.isEmpty())HITS.computeIfAbsent(LbcIndirectKills.originalDamage(source),key->new WeakHashMap<>()).put(target,new Receipt(weapon));
    }
    public static boolean post(DamageSource source,Entity target){
        if(!VanillaExpansionRules.enabled())return true;
        var receipt=receipt(source,target);if(receipt==null||!receipt.consumed)return true;
        if(receipt.posted)return false;receipt.posted=true;return true;
    }
    public static boolean knockback(DamageSource source,Entity target){
        if(!VanillaExpansionRules.enabled())return true;
        var receipt=receipt(source,target);if(receipt==null||!receipt.consumed)return true;
        if(receipt.knockback)return false;receipt.knockback=true;return true;
    }
    public static ItemStack scopedWeapon(DamageSource source,ItemStack nativeWeapon){
        var active=DAMAGE.get();return active!=null&&!active.weapon.isEmpty()&&active.key==LbcIndirectKills.originalDamage(source)?active.weapon:nativeWeapon;
    }
    public static Damage damage(ServerLevel level,LivingEntity target,DamageSource source,float amount){return new Damage(level,target,source,amount);}
    public static final class Damage implements Scope {
        private final Damage previous;
        private final ServerLevel level;private final LivingEntity target;private final DamageSource source,key;
        private final ItemStack weapon;private final Scope using;
        private final boolean added,statusTick;private final float amount;
        private Damage(ServerLevel level,LivingEntity target,DamageSource source,float amount){
            this.previous=DAMAGE.get();this.level=level;this.target=target;this.source=source;this.key=LbcIndirectKills.originalDamage(source);
            Receipt ready=VanillaExpansionRules.enabled()?receipt(source,target):null;
            boolean nativePrepared=ready!=null&&!ready.consumed;
            var game=SkyWars2.game();
            var resolved=nativePrepared?ready.item:VanillaExpansionRules.enabled()&&game!=null?game.items().indirectKills().damageWeapon(source):ItemStack.EMPTY;
            if(!nativePrepared&&VanillaExpansionRules.enabled()&&resolved.isEmpty()&&game!=null){
                var captured=SPELL_ACTORS.get(source.getDirectEntity());
                if(captured!=null&&captured.weaponAction()&&source.getEntity()!=null
                        &&captured.player().equals(source.getEntity().getUUID()))resolved=captured.enchantmentWeapon();
                else if(captured!=null&&captured.weaponAction()&&source.is(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK)
                        &&source.getDirectEntity() instanceof net.minecraft.world.entity.item.FallingBlockEntity
                        &&source.getEntity()==source.getDirectEntity())resolved=captured.enchantmentWeapon();
                else if(captured!=null&&source.getEntity() instanceof net.minecraft.world.entity.Mob mob
                        &&source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile shot && shot.getOwner()==mob){
                    var owner=ownedMob(game,mob);
                    var current=actorCause(mob,owner);
                    if(owner!=target&&current!=null&&current.player().equals(captured.player()))resolved=captured.enchantmentWeapon();
                }
            }
            if(!nativePrepared&&VanillaExpansionRules.enabled()&&resolved.isEmpty()&&game!=null
                    &&source.getEntity() instanceof net.minecraft.world.entity.Mob mob && source.getDirectEntity()==mob){
                var owner=ownedMob(game,mob);
                var cause=actorCause(mob,owner);if(owner!=target&&cause!=null)resolved=cause.enchantmentWeapon();
            }
            if(!nativePrepared&&VanillaExpansionRules.enabled()&&resolved.isEmpty()&&game!=null){
                var spell=game.items().spellSources().current();
                if(spell!=null&&spell.cause()!=null&&spell.cause().weaponAction()
                        &&source.getEntity()!=null&&java.util.Objects.equals(spell.author(),source.getEntity().getUUID()))
                    resolved=spell.cause().enchantmentWeapon();
            }
            boolean statusOnly=false;
            if(!nativePrepared&&VanillaExpansionRules.enabled()&&resolved.isEmpty()&&game!=null){
                resolved=game.items().indirectKills().statusWeapon(target,source);statusOnly=!resolved.isEmpty();
            }
            var fire=FIRE_TICK.get();
            if(!nativePrepared&&VanillaExpansionRules.enabled()&&resolved.isEmpty()&&game!=null&&fire!=null&&fire.target()==target&&fire.source()==LbcIndirectKills.originalDamage(source)){
                resolved=game.items().indirectKills().environmentWeapon(target,source);statusOnly=!resolved.isEmpty();
            }
            statusTick=statusOnly;weapon=resolved;
            using=!weapon.isEmpty()&&source.getEntity() instanceof ServerPlayer player?using(player,weapon):null;
            added=!nativePrepared&&!weapon.isEmpty()&&amount>0&&Float.isFinite(amount);
            DAMAGE.set(this);
            try{
                // A retained wand action is a spell, not a physical melee hit.
                // Sword damage enchantments apply at the native melee boundary;
                // Power scales the captured spell amount after the caster changes hands.
                this.amount=added&&EnchantmentRules.spellSource(weapon)?spellPower(level,weapon,amount)
                        :added?EnchantmentHelper.modifyDamage(level,weapon,target,source,amount):amount;
                ready=receipt(source,target);if(ready!=null)ready.consumed=true;
            }catch(Throwable error){close();throw error;}
        }
        public float amount(){return amount;}
        public void accepted(){
            if(statusTick||!added||amount<=0)return;
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level,target,source,weapon);
            float force=EnchantmentHelper.modifyKnockback(level,weapon,target,source,0);
            Entity attacker=source.getDirectEntity();if(attacker==null)attacker=source.getEntity();
            if(force>0&&attacker!=null)target.knockback(force*.5,attacker.getX()-target.getX(),attacker.getZ()-target.getZ(),source,amount);
        }
        @Override public void close(){if(using!=null)using.close();if(previous==null)DAMAGE.remove();else DAMAGE.set(previous);}
    }
    public static float spellPower(ServerLevel level,ItemStack wand,float amount){
        if(amount<=0||!Float.isFinite(amount)||!me.skywars2.game.entities.LbcDimensionItems.ordinary(level)
                ||!EnchantmentRules.spellSource(wand))return amount;
        var power=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER);
        int enchantment=EnchantmentHelper.getItemEnchantmentLevel(power,wand);
        if(enchantment<=0)return amount;
        return (float)Math.min(Float.MAX_VALUE,(double)amount*(1.0+0.25*((double)enchantment+1.0)));
    }
    /** Only the exact source and target of a native fire/lava callback are scoped. */
    public static Scope nativeFire(LivingEntity target,DamageSource source){
        var previous=FIRE_TICK.get();FIRE_TICK.set(new NativeFire(target,LbcIndirectKills.originalDamage(source)));
        return ()->{if(previous==null)FIRE_TICK.remove();else FIRE_TICK.set(previous);};
    }
    /** Raw-HP paths bypass hurtServer; only a captured wand action is eligible. */
    public static float capturedSpellPower(ServerLevel level,DamageSource source,float amount){
        var game=SkyWars2.game();if(game==null||!VanillaExpansionRules.enabled())return amount;
        return spellPower(level,game.items().indirectKills().damageWeapon(source),amount);
    }
    public static LbcIndirectKills.Owner effectCause(ServerPlayer caster){
        var game=SkyWars2.game();
        return game==null||caster==null||!itemAction(caster.getUUID())?null:game.items().indirectKills().coldCause(caster,true);
    }
    /** Only explicitly identified wand-created effects receive an owner. */
    public static net.minecraft.world.effect.MobEffectInstance markHealingEffect(net.minecraft.world.effect.MobEffectInstance effect,LbcIndirectKills.Owner cause){
        if(cause!=null&&cause.weaponAction()&&EnchantmentRules.spellSource(cause.enchantmentWeapon()))
            ((me.skywars2.game.combat.WandPowerEffectAccess)(Object)effect).skywars2$powerCause(cause);
        return effect;
    }
    public static Scope effectHealing(LbcIndirectKills.Owner cause){
        var before=HEAL_EFFECT.get();if(cause==null)HEAL_EFFECT.remove();else HEAL_EFFECT.set(cause);
        return ()->{if(before==null)HEAL_EFFECT.remove();else HEAL_EFFECT.set(before);};
    }
    public static float scopedSpellHealing(LivingEntity target,float amount){
        var game=SkyWars2.game();if(game==null||!VanillaExpansionRules.enabled()||!(target.level() instanceof ServerLevel level))return amount;
        var effect=HEAL_EFFECT.get();
        if(effect!=null&&effect.weaponAction())return spellPower(level,effect.enchantmentWeapon(),amount);
        var source=game.items().spellSources().current();
        return source==null||source.cause()==null||!source.cause().weaponAction()?amount
                :spellPower(level,source.cause().enchantmentWeapon(),amount);
    }
    private static ServerPlayer ownedMob(me.skywars2.game.GameController game,net.minecraft.world.entity.Mob mob){
        var blazeOwner=mob instanceof net.minecraft.world.entity.monster.Blaze blaze?game.items().blaze().owner(blaze):null;
        if(blazeOwner!=null)return blazeOwner;
        var psychoOwner=game.items().psycho().owner(mob);
        return psychoOwner!=null?psychoOwner:game.items().summons().owner(mob);
    }
    private static Entity controller(Entity actor){
        if(actor instanceof net.minecraft.world.entity.projectile.Projectile projectile)return projectile.getOwner();
        if(actor instanceof net.minecraft.world.entity.projectile.EvokerFangs fang)return fang.getOwner();
        if(actor instanceof net.minecraft.world.entity.item.PrimedTnt tnt)return tnt.getOwner();
        if(actor instanceof net.minecraft.world.entity.AreaEffectCloud cloud)return cloud.getOwner();
        if(actor instanceof net.minecraft.world.entity.LightningBolt lightning)return lightning.getCause();
        return null;
    }
    /** Called on native entity publication; never assigns spell targeting policy. */
    public static void capture(Entity actor){
        if(actor==null||SPELL_ACTORS.containsKey(actor))return;
        var restored=UNLOADED_SPELL_ACTORS.remove(actor.getUUID());
        if(restored!=null){SPELL_ACTORS.put(actor,restored);return;}
        var controller=controller(actor);var game=SkyWars2.game();if(game==null)return;
        if(controller instanceof net.minecraft.world.entity.Mob mob){
            var owner=ownedMob(game,mob);
            var cause=actorCause(mob,owner);if(cause!=null)SPELL_ACTORS.put(actor,cause);
            return;
        }
        var active=USING.get();if(active==null||controller==null||!active.player().equals(controller.getUUID()))return;
        var player=game.server().getPlayerList().getPlayer(active.player());
        if(player!=null&&EnchantmentRules.spellSource(snapshot(player)))
            SPELL_ACTORS.put(actor,game.items().indirectKills().coldCause(player,true));
    }
    /** Only an explicitly tracked summon created during a real wand action inherits its source. */
    public static void captureSummonedMob(ServerPlayer owner,net.minecraft.world.entity.Mob mob){
        if(mob==null)return;var game=SkyWars2.game();var cause=effectCause(owner);
        if(game==null||owner==null||game.items().summons().owner(mob)!=owner||cause==null||!cause.weaponAction()||!EnchantmentRules.spellSource(cause.enchantmentWeapon()))SPELL_ACTORS.remove(mob);
        else SPELL_ACTORS.put(mob,cause);
    }
    /** Psycho ghost ownership is its own native service, never a global wild-source scope. */
    public static void capturePsychoVex(ServerPlayer owner,net.minecraft.world.entity.Mob mob){
        if(mob==null)return;var game=SkyWars2.game();var cause=effectCause(owner);
        if(game==null||owner==null||game.items().psycho().owner(mob)!=owner||cause==null||!cause.weaponAction()||!EnchantmentRules.spellSource(cause.enchantmentWeapon()))SPELL_ACTORS.remove(mob);
        else SPELL_ACTORS.put(mob,cause);
    }
    /** Only an explicitly-created falling block from a captured wand may carry enchantments. */
    public static void captureFalling(net.minecraft.world.entity.item.FallingBlockEntity block,ServerPlayer owner,LbcIndirectKills.Owner cause){
        if(block!=null&&owner!=null&&cause!=null&&cause.weaponAction()&&EnchantmentRules.spellSource(cause.enchantmentWeapon())&&cause.player().equals(owner.getUUID()))SPELL_ACTORS.put(block,cause);
    }
    /** Owned Blaze retains only the wand action that actually created or retamed it. */
    public static void captureBlaze(net.minecraft.world.entity.monster.Blaze blaze,LbcIndirectKills.Owner cause){
        if(blaze==null)return;var game=SkyWars2.game();var owner=game==null?null:game.items().blaze().owner(blaze);
        if(cause==null||!cause.weaponAction()||!EnchantmentRules.spellSource(cause.enchantmentWeapon())||owner==null||!cause.player().equals(owner.getUUID()))SPELL_ACTORS.remove(blaze);
        else SPELL_ACTORS.put(blaze,cause);
    }
    /** Enchantment-only cause follows an actor only while its physical controller still matches. */
    public static LbcIndirectKills.Owner actorCause(Entity actor,ServerPlayer controller){
        var cause=SPELL_ACTORS.get(actor);
        return cause!=null && controller!=null && cause.weaponAction() && cause.player().equals(controller.getUUID()) ? cause : null;
    }
    /** Restore only this captured enchantment source while a delayed actor emits descendants. */
    public static Scope actorScope(Entity actor,ServerPlayer controller){return causeScope(controller,actorCause(actor,controller));}
    public static Scope causeScope(ServerPlayer controller,LbcIndirectKills.Owner cause){
        if(cause!=null&&(controller==null||!cause.weaponAction()||!cause.player().equals(controller.getUUID())))cause=null;
        var previous=USING.get();var previousActor=ACTOR_USING.get();
        // Never let unrelated nested casts or private TMT projections replace an actor's frozen source.
        if(cause==null){USING.remove();if(controller==null)ACTOR_USING.remove();else ACTOR_USING.set(new Use(controller.getUUID(),ItemStack.EMPTY));}
        else {var saved=new Use(controller.getUUID(),cause.enchantmentWeapon().copyWithCount(1));USING.set(saved);ACTOR_USING.set(saved);}
        return ()->{if(previous==null)USING.remove();else USING.set(previous);
                    if(previousActor==null)ACTOR_USING.remove();else ACTOR_USING.set(previousActor);};
    }
    /** Clone private enchantment provenance only; physical owner remains native. */
    public static void inherit(Entity original,Entity clone){
        var cause=SPELL_ACTORS.get(original);if(cause!=null&&clone!=null)SPELL_ACTORS.put(clone,cause);
    }
    /** Keep unloaded chunk bodies, but never retain removed actors. */
    public static void unloaded(Entity actor){
        var cause=SPELL_ACTORS.get(actor);if(cause==null)return;
        var reason=actor.getRemovalReason();
        if(reason==null)return; // Visibility changes need not remove a resident body.
        SPELL_ACTORS.remove(actor);
        if(reason==Entity.RemovalReason.UNLOADED_TO_CHUNK)UNLOADED_SPELL_ACTORS.put(actor.getUUID(),cause);
    }
    public static void stopSpellActors(){SPELL_ACTORS.clear();UNLOADED_SPELL_ACTORS.clear();}
    public static final class ActorCheckpoint {
        private final me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.IdentityImage<Entity,LbcIndirectKills.Owner> active;
        private final Map<UUID,LbcIndirectKills.Owner> dormant;
        private ActorCheckpoint(me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links links){
            active=new me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.IdentityImage<>(SPELL_ACTORS,links);
            dormant=Map.copyOf(UNLOADED_SPELL_ACTORS);
        }
        public Runnable prepare(me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links links){
            var restored=active.restore(links);var asleep=Map.copyOf(dormant);
            return ()->{SPELL_ACTORS=restored;UNLOADED_SPELL_ACTORS.clear();UNLOADED_SPELL_ACTORS.putAll(asleep);};
        }
    }
    public static ActorCheckpoint checkpoint(me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links links){return new ActorCheckpoint(links);}
    private record EffectOwner(Holder<net.minecraft.world.effect.MobEffect> type,List<LbcIndirectKills.Owner> layers) {}
    public static final class EffectCheckpoint {
        private final me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.IdentityImage<LivingEntity,List<EffectOwner>> actors;
        private EffectCheckpoint(GameController game,me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links links){
            var rows=new IdentityHashMap<LivingEntity,List<EffectOwner>>();
            var bodies=new ArrayList<Entity>(me.skywars2.game.match.checkpoints.MatchActorCheckpoint.currentBodies(game).values());
            bodies.addAll(game.server().getPlayerList().getPlayers());
            for(var retained:me.skywars2.game.players.MatchOfflinePlayers.inventoryBodies())
                if(me.skywars2.game.players.MatchOfflinePlayers.retained(retained))bodies.add(retained);
            for(var body:bodies)if(body instanceof LivingEntity living&&!links.omitted(living)){
                var effects=new ArrayList<EffectOwner>();
                for(var effect:living.getActiveEffects()){
                    var layers=new ArrayList<LbcIndirectKills.Owner>();boolean marked=false;
                    for(var node=effect;node!=null;node=((me.skywars2.game.combat.WandPowerEffectAccess)(Object)node).skywars2$hiddenEffect()){
                        var cause=((me.skywars2.game.combat.WandPowerEffectAccess)(Object)node).skywars2$powerCause();
                        layers.add(cause);marked|=cause!=null;
                    }
                    if(marked)effects.add(new EffectOwner(effect.getEffect(),Collections.unmodifiableList(layers)));
                }
                if(!effects.isEmpty())rows.put(living,List.copyOf(effects));
            }
            actors=new me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.IdentityImage<>(rows,links);
        }
        public Runnable prepare(me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links links){
            var restored=actors.restore(links);var installs=new ArrayList<Runnable>();
            restored.forEach((living,effects)->{for(var effect:effects){
                var node=living.getEffect(effect.type());
                for(var cause:effect.layers()){
                    if(node==null)throw new IllegalStateException("Restore native effect layers before wand healing provenance");
                    var access=(me.skywars2.game.combat.WandPowerEffectAccess)(Object)node;
                    installs.add(()->access.skywars2$powerCause(cause));node=access.skywars2$hiddenEffect();
                }
                if(node!=null)throw new IllegalStateException("Restored wand effect has unexpected hidden layers");
            }});
            return ()->installs.forEach(Runnable::run);
        }
    }
    public static EffectCheckpoint effectCheckpoint(GameController game,me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links links){return new EffectCheckpoint(game,links);}
    /** Rebind retained offline effect provenance immediately after native player data load. */
    public static void transferHealingEffects(LivingEntity oldBody,LivingEntity newBody){
        if(oldBody==null||newBody==null||oldBody==newBody)return;
        var installs=new ArrayList<Runnable>();
        for(var effect:oldBody.getActiveEffects()){
            var layers=new ArrayList<LbcIndirectKills.Owner>();boolean marked=false;
            for(var node=effect;node!=null;node=((me.skywars2.game.combat.WandPowerEffectAccess)(Object)node).skywars2$hiddenEffect()){
                var cause=((me.skywars2.game.combat.WandPowerEffectAccess)(Object)node).skywars2$powerCause();
                layers.add(cause);marked|=cause!=null;
            }
            if(!marked)continue;
            var node=newBody.getEffect(effect.getEffect());
            for(var cause:layers){
                if(node==null)throw new IllegalStateException("Restore native player effect layers before healing provenance");
                var access=(me.skywars2.game.combat.WandPowerEffectAccess)(Object)node;
                installs.add(()->access.skywars2$powerCause(cause));node=access.skywars2$hiddenEffect();
            }
            if(node!=null)throw new IllegalStateException("Restored player effect has unexpected hidden layers");
        }
        installs.forEach(Runnable::run);
    }

    /** An explicit sword spell snapshots only a private source; the physical sword stays unchanged. */
    public static Scope usingSpell(ServerPlayer player,ItemStack physical){
        var source=physical.copy();var data=source.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        data.putInt("magicwand",1);source.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
        return using(player,source);
    }
    public static Scope using(ServerPlayer player,ItemStack source){
        var previous=USING.get();
        // A nested borrowed cast may temporarily project another wand; the
        // actual enchanted book or TMT that initiated it owns the spell.
        var retained=previous!=null&&previous.player.equals(player.getUUID())
                &&me.skywars2.game.items.wands.WandCastOrigin.borrowed(player)?previous.item:source;
        USING.set(new Use(player.getUUID(),retained.copyWithCount(1)));
        return ()->{if(previous==null)USING.remove();else USING.set(previous);};
    }
    static ItemStack snapshot(ServerPlayer player){
        if(player==null)return ItemStack.EMPTY;
        var actor=ACTOR_USING.get();if(actor!=null&&actor.player.equals(player.getUUID()))return actor.item.copy();
        var tmt=tmtSource(player);if(!tmt.isEmpty())return tmt;
        var source=USING.get();return source!=null&&source.player.equals(player.getUUID())
                ?source.item.copy():player.getMainHandItem().copyWithCount(1);
    }
    private static ItemStack tmtSource(ServerPlayer player){
        var selected=LbcTmtTriggers.enchantmentSource(player);
        return selected.isEmpty()?LbcTmtAbilities.enchantmentSource(player):selected.copyWithCount(1);
    }
    static boolean itemAction(UUID player){
        var actor=ACTOR_USING.get();if(actor!=null&&actor.player.equals(player))return !actor.item.isEmpty();
        var source=USING.get();if(source!=null&&source.player.equals(player))return true;
        var game=SkyWars2.game();var body=game==null?null:game.server().getPlayerList().getPlayer(player);
        return body!=null&&!tmtSource(body).isEmpty();
    }
    static ItemStack snapshot(UUID player){
        var source=USING.get();if(source!=null&&source.player.equals(player))return source.item.copy();
        var game=SkyWars2.game();return game==null?ItemStack.EMPTY:snapshot(game.server().getPlayerList().getPlayer(player));
    }
    private static void drain(){
        for(CauseReference ref;(ref=(CauseReference)COLLECTED.poll())!=null;){
            var dead=ref;CAUSES.computeIfPresent(ref.key,(key,refs)->{refs.remove(dead);return refs.isEmpty()?null:refs;});
        }
    }
    static Capture retained(UUID player,long order){
        drain();var references=CAUSES.get(new CauseKey(player,order));
        if(references!=null)for(var iterator=references.descendingIterator();iterator.hasNext();){
            var original=iterator.next().get();if(original!=null)return new Capture(original.enchantmentWeapon(),original.weaponAction());
        }
        return new Capture(snapshot(player),itemAction(player));
    }
    static void remember(LbcIndirectKills.Owner owner){
        drain();var reference=new CauseReference(owner);
        CAUSES.computeIfAbsent(reference.key,key->new java.util.concurrent.ConcurrentLinkedDeque<>()).addLast(reference);
    }
    static void clear(){CAUSES.clear();HITS.clear();drain();}
    public static Scope loot(LivingEntity victim,DamageSource source){
        var previous = LOOT.get();
        var game = SkyWars2.game();
        var kills = game == null ? null : game.items().indirectKills();
        var boundWeapon = kills == null ? null : kills.boundLootWeapon(victim, source);
        var weapons = new ArrayList<ItemStack>(3);
        if (boundWeapon == null) {
            var direct = source.getWeaponItem();
            if (direct != null && !direct.isEmpty()) weapons.add(direct.copy());
            if (kills != null) {
                var captured = kills.enchantmentWeapon(source);
                if (!captured.isEmpty()) weapons.add(captured);
                var credited = kills.lootWeapon(victim);
                if (!credited.isEmpty()) weapons.add(credited);
            }
        }
        LivingEntity killer=victim.getLastHurtByPlayer();
        if(killer==null&&source.getEntity() instanceof LivingEntity living)killer=living;
        LOOT.set(new Loot(killer, boundWeapon, List.copyOf(weapons)));
        return ()->{if(previous==null)LOOT.remove();else LOOT.set(previous);};
    }
    /** Restricted to enchantment functions: ordinary loot predicates retain the native attacker. */
    public static Entity lootAttacker(LootContext context,Entity actual){
        if(!VanillaExpansionRules.enabled())return actual;
        var player=context.getOptional(LootContextParams.LAST_DAMAGE_PLAYER);return player!=null?player:actual;
    }
    public static int lootLevel(Holder<Enchantment> enchantment,LivingEntity attacker,int nativeLevel){
        var loot=LOOT.get();
        if(!VanillaExpansionRules.enabled()||loot==null||loot.killer!=attacker||!enchantment.value().matchingSlot(EquipmentSlot.MAINHAND))return nativeLevel;
        if (loot.boundWeapon != null) return EnchantmentHelper.getItemEnchantmentLevel(enchantment, loot.boundWeapon);
        int result=nativeLevel;
        for(var stack:loot.weapons)result=Math.max(result,EnchantmentHelper.getItemEnchantmentLevel(enchantment,stack));
        return result;
    }
}
