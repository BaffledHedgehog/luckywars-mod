package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Composes private ranged properties of one genuinely accepted native volley. */
public final class LbcTmtRanged {
    private LbcTmtRanged(){}

    static boolean flag(LbcTmtAbilities.Ability ability,String key){return ((ItemData.IntReader)(Object)ability.data()).skywars2$readInt(key,0)==1;}
    static boolean physicalImba(LbcTmtAbilities.Ability ability){return flag(ability,"imba_fast_crossbow")||flag(ability,"imba_shotgun");}
    static boolean beamImba(LbcTmtAbilities.Ability ability){return flag(ability,"imba_sniper_crossbow")||flag(ability,"imba_super_shotgun")||flag(ability,"imba_gatlingator");}
    static boolean charged(LbcTmtAbilities.Ability ability){return flag(ability,"railgun_bow")||flag(ability,"shish_gun");}
    static boolean shulker(LbcTmtAbilities.Ability ability){return flag(ability,"shulker")||flag(ability,"shulker_new");}
    private static final List<String> ORDER=List.of("explosive","railgun_bow","shish_gun",
            "imba_fast_crossbow","imba_shotgun","imba_sniper_crossbow","imba_super_shotgun","imba_gatlingator",
            "double_old","shulker","shulker_new","madoka_bow","fast","fast_old");
    private static int order(LbcTmtAbilities.Ability ability){
        for(int i=0;i<ORDER.size();i++)if(flag(ability,ORDER.get(i)))return i;
        return ORDER.size();
    }

    /** Preview uses the real native active charge and each property's own scope. */
    public static boolean charging(GameController game,ServerPlayer owner,ItemStack weapon,int activeCharge){
        if(ItemData.intValue(weapon,"tmt_item",0)!=1)return false;
        if(LbcTmtAbilities.active(weapon))return true;
        for(var ability:LbcTmtAbilities.read(weapon))if(charged(ability))
            LbcTmtAbilities.invoke(weapon,ability.id(),()->{game.items().chargedBows().charging(owner,weapon,activeCharge);return true;});
        return true;
    }

    /**
     * Own the complete accepted-shot continuation so a scoped native automatic shot
     * cannot mark the tick guard before its sibling traits have been dispatched.
     */
    public static boolean onShot(GameController game,ServerPlayer owner,InteractionHand hand,
            ItemStack weapon,ItemStack fired,List<Projectile> nativeFamily){
        if(ItemData.intValue(fired,"tmt_item",0)!=1)return false;
        var family=List.copyOf(nativeFamily);
        LbcTmtAbilities.afterScopes(()->{
            acceptedShot(game,owner,hand,weapon,fired,family);
            game.items().tmt().bowShot(owner,hand,weapon,fired,family);
        });
        return true;
    }

    /**
     * The caller already owns native ammunition/durability debit and publication. This
     * method never debits that volley or rolls another shot. The existing minigun
     * owner alone may debit its separate next-chamber reload. Return true only for TMT.
     */
    public static boolean acceptedShot(GameController game,ServerPlayer owner,InteractionHand hand,
            ItemStack weapon,ItemStack fired,List<Projectile> nativeFamily){
        if(ItemData.intValue(fired,"tmt_item",0)!=1)return false;
        if(LbcTmtAbilities.active(weapon)||LbcTmtAbilities.active(fired))return true;
        LbcTmtAmmoPayloads.install(game,owner,fired,nativeFamily);
        var abilities=new ArrayList<>(LbcTmtAbilities.read(fired));
        // Source owner order, independent of random draw/serialization order.
        abilities.sort(java.util.Comparator.comparingInt(LbcTmtRanged::order));
        var family=List.copyOf(nativeFamily);
        // Expand the actual accepted family before the single shared trinket roll.
        // A neighboring beam property must not hide a physical shotgun property.
        for(var ability:abilities)if(physicalImba(ability)){
            var input=family;var result=new java.util.concurrent.atomic.AtomicReference<>(input);
            scope(weapon,fired,ability,()->{result.set(LbcImbaCrossbows.augment(game,owner,fired,input));return true;});
            family=result.get();
        }
        for(var ability:abilities)if(flag(ability,"double_old")){
            var input=family;var result=new java.util.concurrent.atomic.AtomicReference<>(input);
            scope(weapon,fired,ability,()->{result.set(game.items().balance().augment(owner,fired,input));return true;});
            family=result.get();
        }
        if(family.isEmpty())return true;
        for(var projectile:family)if(LbcTmtAmmoPayloads.hasVariant(projectile,300)&&!LbcHomingArrows.isManaged(projectile))
            game.items().homingArrows().loaded(projectile);
        game.items().trinkets().shot(owner,family);

        // Each destructive owner gets its own codec image of the very same rolled
        // family. These donors are never published as a second vanilla shot. A beam,
        // TNT shell and shulker conversion therefore cannot consume each other's input.
        // Capture every image before the first destructive callback (which can debit
        // the actual weapon). Native owner admission and suppression stay authoritative.
        var accepted=List.copyOf(family);
        var images=new ArrayList<CompoundTag>();
        boolean hasTerminal=abilities.stream().anyMatch(ability->flag(ability,"explosive")||charged(ability)||beamImba(ability)||shulker(ability));
        if(hasTerminal)for(var projectile:accepted)if(!projectile.isRemoved())images.add(image(game,projectile));
        boolean retired=false,retireAll=false;
        for(var ability:abilities){
            if(!flag(ability,"explosive")&&!charged(ability)&&!beamImba(ability)&&!shulker(ability))continue;
            var donors=donors(owner,images);
            if(donors.isEmpty())continue;
            boolean converted=scope(weapon,fired,ability,()->{
                if(flag(ability,"explosive"))return game.items().balance().explosiveBow(owner,fired,donors);
                if(charged(ability))return game.items().chargedBows().release(owner,fired,owner.getTicksUsingItem(),donors);
                if(beamImba(ability))return LbcImbaCrossbows.release(game,owner,fired,donors);
                var result=flag(ability,"shulker_new")?game.items().shulkerBow().augment(owner,fired,donors)
                        :game.items().balance().augment(owner,fired,donors);
                return result.stream().anyMatch(body->!donors.contains(body)&&!body.isRemoved());
            });
            // Shulker owners publish replacements themselves; surviving donors are
            // only capture contexts and must never survive as untracked entities.
            donors.forEach(Projectile::discard);
            retired|=converted;retireAll|=converted&&charged(ability);
        }

        // Madoka owns an independent physical output when sibling converters took
        // the original arrows. Its native owner registers that output, preserving
        // the source cause and all once-rolled arrow traits.
        for(var ability:abilities)if(flag(ability,"madoka_bow")){
            var input=retired?donors(owner,images):accepted;
            scope(weapon,fired,ability,()->{game.items().madokaBow().augment(owner,fired,input);return true;});
            if(retired){
                for(var projectile:input){
                    if(LbcMadokaBow.recognizes(projectile)&&!projectile.isRemoved()){
                        game.items().timeStop().prepareFrozen(projectile);
                        if(owner.level().addFreshEntity(projectile)){
                            game.items().trinketArrows().loaded(projectile);
                        }else projectile.discard();
                    }else projectile.discard();
                }
            }
        }
        if(retired)for(var projectile:accepted)if(retireAll||projectile instanceof AbstractArrow)projectile.discard();
        // Reload acts on the actual stack/hand; the private fired copy classifies
        // even a native final shot that broke its weapon. A second property sees
        // the first native reload and cannot charge or debit the same chamber twice.
        for(var ability:abilities)if(flag(ability,"fast")||flag(ability,"fast_old"))
            scope(weapon,fired,ability,()->{LbcMinigunCrossbow.afterShot(game,owner,hand,weapon,accepted,fired);return true;});
        return true;
    }

    private static boolean scope(ItemStack weapon,ItemStack fired,LbcTmtAbilities.Ability ability,BooleanSupplier body){
        if(weapon==fired||weapon.isEmpty())return LbcTmtAbilities.invoke(fired,ability.id(),body);
        return LbcTmtAbilities.invoke(weapon,ability.id(),()->LbcTmtAbilities.invoke(fired,ability.id(),body));
    }

    static CompoundTag image(GameController game,Projectile arrow){
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,arrow.registryAccess());
        arrow.saveWithoutId(output);var tag=output.buildResult();
        tag.putString("id",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(arrow.getType()).toString());
        tag.remove("UUID");
        // Frozen native bodies have zero live velocity; copy their canonical
        // persisted motion, as the existing old-double owner does.
        var velocity=game.items().timeStop().persistedVelocity(arrow,arrow.getDeltaMovement());
        var motion=new net.minecraft.nbt.ListTag();motion.add(net.minecraft.nbt.DoubleTag.valueOf(velocity.x));
        motion.add(net.minecraft.nbt.DoubleTag.valueOf(velocity.y));motion.add(net.minecraft.nbt.DoubleTag.valueOf(velocity.z));tag.put("Motion",motion);
        tag.putBoolean("NoGravity",game.items().timeStop().persistedNoGravity(arrow,arrow.isNoGravity()));
        return tag;
    }

    static List<Projectile> donors(ServerPlayer owner,List<CompoundTag> images){
        var result=new ArrayList<Projectile>(images.size());
        for(var image:images){
            var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse(image.getStringOr("id","minecraft:arrow")));
            var body=type.create(owner.level(),EntitySpawnReason.TRIGGERED);
            if(!(body instanceof Projectile arrow))continue;
            arrow.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),image.copy()));
            arrow.setOwner(owner);result.add(arrow);
        }
        return List.copyOf(result);
    }
}
