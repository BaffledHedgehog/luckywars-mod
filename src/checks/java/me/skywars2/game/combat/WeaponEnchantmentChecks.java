package me.skywars2.game.combat;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.WeaponEnchantments;
import me.skywars2.game.items.tmt.LbcTmtItems;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.items.wands.LbcWands.NativeTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.LootTable;

/** Actual table/anvil, native hurt and deterministic entity-loot boundaries for custom weapons. */
public final class WeaponEnchantmentChecks {
    private WeaponEnchantmentChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Weapon enchantments: "+message);}
    public static void run(GameController game,ServerPlayer player,BlockPos center){
        var registry=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var all=registry.listElements().toList();
        for(int type=0;type<5;type++){
            var stack=LbcTmtItems.prototype(type);
            require(stack.isEnchantable(),"all five real TMT carriers accept enchanting-table input: "+type);
            for(var enchantment:all)require(enchantment.value().isPrimaryItem(stack)&&enchantment.value().isSupportedItem(stack),"TMT supports "+enchantment.key());
            var available=new HashSet<net.minecraft.core.Holder<Enchantment>>();
            for(int cost=1;cost<=36;cost++)EnchantmentHelper.getAvailableEnchantmentResults(cost,stack,all.stream().map(h->(net.minecraft.core.Holder<Enchantment>)h))
                    .forEach(entry->available.add(entry.enchantment()));
            require(available.containsAll(all),"full TMT table cost range includes treasure and high-cost enchantments; missing="+all.stream().filter(e->!available.contains(e)).map(e->e.key().identifier()).toList());
            var table=new EnchantmentMenu(48,player.getInventory(),ContainerLevelAccess.create(player.level(),center));
            table.getSlot(0).set(stack.copy());table.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,3));int xp=player.experienceLevel;
            require(table.costs[2]==30&&table.clickMenuButton(player,2),"real TMT table accepts highest option");
            require(player.experienceLevel==xp-3&&table.getSlot(1).getItem().isEmpty()&&!table.getSlot(0).getItem().getEnchantments().isEmpty(),"TMT enchanting consumes exact XP/lapis");
            var book=new ItemStack(Items.ENCHANTED_BOOK);var entries=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            entries.set(registry.getOrThrow(Enchantments.MENDING),1);entries.set(registry.getOrThrow(Enchantments.PROTECTION),4);
            book.set(DataComponents.STORED_ENCHANTMENTS,entries.toImmutable());
            var anvil=new AnvilMenu(49,player.getInventory(),ContainerLevelAccess.create(player.level(),center));
            anvil.getSlot(0).set(stack.copy());anvil.getSlot(1).set(book);anvil.createResult();var result=anvil.getSlot(2).getItem().copy();
            require(!result.isEmpty()&&result.getEnchantments().getLevel(registry.getOrThrow(Enchantments.MENDING))==1
                    &&result.getEnchantments().getLevel(registry.getOrThrow(Enchantments.PROTECTION))==4,"TMT accepts unrelated and treasure books at the real anvil");
            require(Objects.equals(result.get(DataComponents.CUSTOM_DATA),stack.get(DataComponents.CUSTOM_DATA)),"anvil preserves full TMT custom data");
        }
        var wand=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/tesla_wand").getFirst();
        require(wand.getItem() instanceof LbcWands.NativeTool&&wand.isEnchantable(),"canonical Tesla wand is a genuine table input");
        for(var key:List.of(Enchantments.SHARPNESS,Enchantments.LOOTING,Enchantments.KNOCKBACK,Enchantments.FIRE_ASPECT))
            require(registry.getOrThrow(key).value().isPrimaryItem(wand),"wand supports meaningful "+key);
        require(registry.getOrThrow(Enchantments.POWER).value().isPrimaryItem(wand),"wand admits Power on the native enchantment table");
        require(!registry.getOrThrow(Enchantments.SWEEPING_EDGE).value().isSupportedItem(wand),"non-sword wand does not gain native sword sweep");
        var table=new EnchantmentMenu(50,player.getInventory(),ContainerLevelAccess.create(player.level(),center));
        table.getSlot(0).set(wand.copy());table.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,3));
        require(table.clickMenuButton(player,2)&&!table.getSlot(0).getItem().getEnchantments().isEmpty(),"actual wand table result");

        var savedMain=player.getMainHandItem().copy();var savedOff=player.getOffhandItem().copy();
        var actors=new ArrayList<Cow>();
        try{
            var weapon=wand.copy();weapon.enchant(registry.getOrThrow(Enchantments.SHARPNESS),4);
            weapon.enchant(registry.getOrThrow(Enchantments.POWER),1);
            weapon.enchant(registry.getOrThrow(Enchantments.LOOTING),100);weapon.enchant(registry.getOrThrow(Enchantments.FIRE_ASPECT),2);
            weapon.enchant(registry.getOrThrow(Enchantments.KNOCKBACK),2);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));player.setItemInHand(InteractionHand.OFF_HAND,weapon);
            LbcIndirectKills.Owner captured;
            try(var use=WeaponEnchantments.using(player,weapon)){captured=game.items().indirectKills().coldCause(player,true);}
            require(captured.weaponAction()&&captured.enchantmentWeapon().getEnchantments().getLevel(registry.getOrThrow(Enchantments.LOOTING))==100,"offhand weapon snapshot captured at launch");
            var raw=new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),player,player);
            var source=game.items().indirectKills().captureDamage(raw,captured);
            var target=new Cow(EntityTypes.COW,player.level()){@Override public long getLootTableSeed(){return 61983;}};
            target.setPos(player.position().add(2,0,0));target.setNoAi(true);target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);require(player.level().addFreshEntity(target),"native custom-weapon target");actors.add(target);
            player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            require(target.hurtServer(player.level(),source,4),"captured custom-weapon hit accepted after hand swap");
            require(Math.abs(target.getHealth()-994f)<.001,"Power I scales spell 4 to 6 once, Sharpness IV does not modify the spell, using the captured offhand wand; hp="+target.getHealth());
            require(target.getRemainingFireTicks()>0&&target.getDeltaMovement().lengthSqr()>0,"native Fire Aspect and Knockback apply to accepted custom attack");
            target.setHealth(900);
            var spellSource=new me.skywars2.game.combat.LbcSpellSource.Source(player.getUUID(),player.getUUID(),game.items().epoch(),captured,java.util.Set.of());
            try(var scope=game.items().spellSources().scope(spellSource,player.level())){target.heal(4);}
            require(Math.abs(target.getHealth()-906f)<.001,"scoped wand healing receives Power after hand swap");
            target.setHealth(900);target.heal(4);
            require(Math.abs(target.getHealth()-904f)<.001,"ordinary healing outside spell scope is unchanged");
            var wandEffect=me.skywars2.game.combat.WeaponEnchantments.markHealingEffect(
                    me.skywars2.game.effects.ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.INSTANT_HEALTH,1,0),captured);
            var copiedEffect=new net.minecraft.world.effect.MobEffectInstance(wandEffect);
            target.setHealth(900);copiedEffect.tickServer(player.level(),target,()->{});
            require(Math.abs(target.getHealth()-906f)<.001,"copied native wand effect retains captured healing Power");
            var ordinaryEffect=me.skywars2.game.effects.ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.INSTANT_HEALTH,2,1);
            wandEffect.update(ordinaryEffect);
            require(((me.skywars2.game.combat.WandPowerEffectAccess)(Object)wandEffect).skywars2$powerCause()==null,
                    "stronger ordinary potion clears the wand effect provenance");
            target.setHealth(900);ordinaryEffect.tickServer(player.level(),target,()->{});
            require(Math.abs(target.getHealth()-908f)<.001,"ordinary instant-health effect is never scaled by wand Power");
            target.setHealth(900);
            require(me.skywars2.game.effects.BlackHoleDamage.apply(game,target,source,4),"captured wand raw-HP hit accepted");
            require(Math.abs(target.getHealth()-894f)<.001,"raw-HP wand action receives Power exactly once");
            var other=game.server().getPlayerList().getPlayer(me.skywars2.game.GameChecks.offlineUuid("TestBeta"));
            require(other!=null,"reflection control player");
            target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setHealth(900);
            var reflectedRaw=new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),other,other);
            var reflected=game.items().indirectKills().captureDamage(reflectedRaw,captured);
            require(target.hurtServer(player.level(),reflected,4)&&Math.abs(target.getHealth()-896f)<.001,
                    "a different controller cannot borrow the original wand's Power");
            target.clearFire();target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setHealth(1000);
            var another=new DamageSource(raw.typeHolder(),player,player);
            float nativeAmount=EnchantmentHelper.modifyDamage(player.level(),weapon,target,another,4);
            require(target.hurtServer(player.level(),another,nativeAmount),"native-prepared weapon hit accepted");
            require(Math.abs(target.getHealth()-993.5f)<.001,"native bow/melee preprocessing is not counted twice");
            require(EnchantmentHelper.modifyDamage(player.level(),weapon,target,new DamageSource(raw.typeHolder(),player,player),0)==0,"Sharpness cannot create positive damage on zero-damage ability");
            target.clearFire();target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setHealth(1000);
            var marker=game.items().indirectKills().coldCause(player,true);
            require(!marker.weaponAction(),"unrelated skill/tick cause is not an item-use action");
            var markerOnly=game.items().indirectKills().captureDamage(new DamageSource(raw.typeHolder(),player,player),marker);
            player.setItemInHand(InteractionHand.MAIN_HAND,weapon);
            require(target.hurtServer(player.level(),markerOnly,4)&&target.getHealth()==996,"kill marker does not borrow offensive enchants from current held weapon");

            var lootTable=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:entities/cow"));
            var expected=new ArrayList<ItemStack>();var actual=new ArrayList<ItemStack>();
            // Native smelting also examines DIRECT_ATTACKER's current equipment. Isolate Looting;
            // do not turn that unrelated native entity predicate into a snapshot/ownership rule.
            var lootWeapon=wand.copy();lootWeapon.enchant(registry.getOrThrow(Enchantments.LOOTING),100);
            LbcIndirectKills.Owner lootCause;
            try(var use=WeaponEnchantments.using(player,lootWeapon)){lootCause=game.items().indirectKills().coldCause(player,true);}
            var lootSource=game.items().indirectKills().captureDamage(new DamageSource(raw.typeHolder(),player,player),lootCause);
            player.setItemInHand(InteractionHand.MAIN_HAND,lootWeapon);
            target.setLastHurtByPlayer(player.getUUID(),100);target.clearFire();
            target.dropFromLootTable(player.level(),new DamageSource(raw.typeHolder(),player,player),true,lootTable,expected::add);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
            target.dropFromLootTable(player.level(),lootSource,true,lootTable,actual::add);
            require(expected.size()==actual.size(),"captured-looting deterministic stack count");
            for(int i=0;i<expected.size();i++)require(ItemStack.matches(expected.get(i),actual.get(i)),"full deterministic native loot matches original held Looting100 after switch: "+i+" expected="+expected.get(i)+" actual="+actual.get(i));
            require(actual.stream().mapToInt(ItemStack::getCount).sum()>8,"deterministic Looting100 witness exceeds ordinary native loot");
            var plain = new ArrayList<ItemStack>();
            var switched = new ArrayList<ItemStack>();
            var unenchanted = new ItemStack(Items.STICK);
            LbcIndirectKills.Owner plainCause;
            try (var use = WeaponEnchantments.using(player, unenchanted)) {
                plainCause = game.items().indirectKills().coldCause(player, true);
            }
            var plainSource = game.items().indirectKills().captureDamage(
                    new DamageSource(raw.typeHolder(), player, player), plainCause);
            target.dropFromLootTable(player.level(), new DamageSource(raw.typeHolder(), player, player),
                    true, lootTable, plain::add);
            player.setItemInHand(InteractionHand.MAIN_HAND, lootWeapon);
            target.dropFromLootTable(player.level(), plainSource, true, lootTable, switched::add);
            require(plain.size() == switched.size(), "post-launch Looting must not invent new loot stacks");
            for (int i = 0; i < plain.size(); i++) require(ItemStack.matches(plain.get(i), switched.get(i)),
                    "delayed plain source cannot borrow Looting from the new held item: expected="
                            + plain.get(i) + " actual=" + switched.get(i));
            require(source.getEntity()==player&&source.getDirectEntity()==player,"enchantment plumbing preserves native source identities");
            chaos(game,player,target,center);
            SkyWars2.LOGGER.info("[check] custom weapon enchantments: all {} registry entries on five TMT types, actual table/anvil, offhand launch, exact damage, zero damage and deterministic Looting100",all.size());
        }finally{actors.forEach(actor->{game.items().indirectKills().clear(actor);actor.discard();});player.setItemInHand(InteractionHand.MAIN_HAND,savedMain);player.setItemInHand(InteractionHand.OFF_HAND,savedOff);}
    }
    private static void chaos(GameController game,ServerPlayer player,Cow target,BlockPos center){
        var engine=game.items().pendingCombat();require(engine.snapshot().isEmpty(),"exclusive Chaos projectile fixture");
        var wall=center.above(3);var before=game.maps().snapshot(wall);
        var chaos=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/chaos_sword").getFirst();
        var looting=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);chaos.enchant(looting,10);
        player.setItemInHand(InteractionHand.MAIN_HAND,chaos);
        try{
            target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setHealth(1000);
            var source=player.damageSources().playerAttack(player);
            float amount=EnchantmentHelper.modifyDamage(player.level(),chaos,target,source,1);
            require(target.hurtServer(player.level(),source,amount),"real accepted Chaos sword hit");
            var copies=engine.snapshot();require(copies.size()==40,"accepted hit emits forty actual Chaos swords");
            require(copies.stream().allMatch(copy->copy.cause.weaponAction()&&copy.cause.enchantmentWeapon().getEnchantments().getLevel(looting)==10),"all Chaos children retain launching enchanted weapon");
            var copy=copies.getFirst();for(var other:copies)if(other!=copy)other.display.discard();
            player.level().setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),818);
            copy.direction=new net.minecraft.world.phys.Vec3(0,0,1);copy.display.setPos(wall.getX()+.5,wall.getY()+.5,wall.getZ()+.2);
            engine.tick();require(copy.grounded&&!copy.display.isRemoved()&&copy.groundedTicks==0,"actual Chaos block contact begins fresh lifetime");
            for(int tick=0;tick<7;tick++)engine.tick();
            var image=engine.cloneImage(copy.display);var clone=new net.minecraft.world.entity.Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,player.level());
            image.install(copy.display,clone,Map.of(copy.display,clone));
            var cloned=engine.snapshot().stream().filter(c->c.display==clone).findFirst().orElseThrow();
            require(cloned.grounded&&cloned.groundedTicks==7&&cloned.cause==copy.cause,"Chaos clone preserves remaining lifetime and source weapon");
            image.rollback(clone);clone.discard();
            game.items().timeStop().begin(player.level(),40);
            try{for(int tick=0;tick<5;tick++)engine.tick();require(copy.groundedTicks==7&&!copy.display.isRemoved(),"TimeStop pauses grounded lifetime");}
            finally{game.items().timeStop().end(player.level());}
            for(int tick=0;tick<12;tick++)engine.tick();
            require(copy.groundedTicks==19&&!copy.display.isRemoved(),"embedded Chaos sword survives nineteen active ticks");
            engine.tick();require(copy.display.isRemoved()&&engine.snapshot().isEmpty(),"embedded Chaos sword expires on twentieth active tick");
            for(int y:List.of(player.level().getMinY()-5,player.level().getMaxY()+5)){
                target.setInvulnerableTime(0);target.damageCooldownTime=0;
                var hit=player.damageSources().playerAttack(player);float damage=EnchantmentHelper.modifyDamage(player.level(),chaos,target,hit,1);
                require(target.hurtServer(player.level(),hit,damage)&&engine.snapshot().size()==40,"new accepted Chaos volley for height-bound witness");
                var escaped=engine.snapshot();for(var shot:escaped)shot.display.setPos(player.getX(),y,player.getZ());
                engine.tick();require(engine.snapshot().isEmpty()&&escaped.stream().allMatch(shot->shot.display.isRemoved()),"all escaped Chaos swords retire beyond build height "+y);
            }
        }finally{engine.forget(player);game.maps().restoreSnapshot(wall,before);}
    }
}
