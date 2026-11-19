package me.skywars2.game;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

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
    private static final ThreadLocal<Use> USING=new ThreadLocal<>();
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
        if(VanillaExpansionRules.enabled())HITS.computeIfAbsent(LbcIndirectKills.originalDamage(source),key->new WeakHashMap<>()).put(target,new Receipt(weapon));
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
        private final boolean added;private final float amount;
        private Damage(ServerLevel level,LivingEntity target,DamageSource source,float amount){
            this.previous=DAMAGE.get();this.level=level;this.target=target;this.source=source;this.key=LbcIndirectKills.originalDamage(source);
            Receipt ready=VanillaExpansionRules.enabled()?receipt(source,target):null;
            boolean nativePrepared=ready!=null&&!ready.consumed;
            var game=SkyWars2.game();
            weapon=nativePrepared?ready.item:VanillaExpansionRules.enabled()&&game!=null?game.items().indirectKills().damageWeapon(source):ItemStack.EMPTY;
            using=!weapon.isEmpty()&&source.getEntity() instanceof ServerPlayer player?using(player,weapon):null;
            added=!nativePrepared&&!weapon.isEmpty()&&amount>0&&Float.isFinite(amount);
            DAMAGE.set(this);
            try{
                this.amount=added?EnchantmentHelper.modifyDamage(level,weapon,target,source,amount):amount;
                ready=receipt(source,target);if(ready!=null)ready.consumed=true;
            }catch(Throwable error){close();throw error;}
        }
        public float amount(){return amount;}
        public void accepted(){
            if(!added||amount<=0)return;
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level,target,source,weapon);
            float force=EnchantmentHelper.modifyKnockback(level,weapon,target,source,0);
            Entity attacker=source.getDirectEntity();if(attacker==null)attacker=source.getEntity();
            if(force>0&&attacker!=null)target.knockback(force*.5,attacker.getX()-target.getX(),attacker.getZ()-target.getZ(),source,amount);
        }
        @Override public void close(){if(using!=null)using.close();if(previous==null)DAMAGE.remove();else DAMAGE.set(previous);}
    }
    public static Scope using(ServerPlayer player,ItemStack source){
        var previous=USING.get();USING.set(new Use(player.getUUID(),source.copyWithCount(1)));
        return ()->{if(previous==null)USING.remove();else USING.set(previous);};
    }
    static ItemStack snapshot(ServerPlayer player){
        if(player==null)return ItemStack.EMPTY;
        var tmt=tmtSource(player);if(!tmt.isEmpty())return tmt;
        var source=USING.get();return source!=null&&source.player.equals(player.getUUID())
                ?source.item.copy():player.getMainHandItem().copyWithCount(1);
    }
    private static ItemStack tmtSource(ServerPlayer player){
        var selected=LbcTmtTriggers.enchantmentSource(player);
        return selected.isEmpty()?LbcTmtAbilities.enchantmentSource(player):selected.copyWithCount(1);
    }
    static boolean itemAction(UUID player){
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
