package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.TagValueInput;

/** Native source loot, serialized equipment and migration-priority regression checks. */
public final class SourceKnockbackBalanceChecks {
    private SourceKnockbackBalanceChecks() { }
    private static void require(boolean value,String why) {
        if(!value) throw new IllegalStateException("Source knockback balance: "+why);
    }
    private static CompoundTag parse(String value) {
        try { return TagParser.parseCompoundFully(value); }
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalStateException(error); }
    }
    private static int knockback(ServerPlayer player,ItemStack stack) {
        return stack.getEnchantments().getLevel(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK));
    }
    public static void run(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null,"native player prerequisite");
        for(String leaf:new String[]{"antimagic_sword"}) {
            var loot=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/"+leaf);
            require(loot.size()==1&&loot.getFirst().is(Items.IRON_SWORD)&&knockback(player,loot.getFirst())==5,
                    "actual ordinary authored loot Knockback255 to5 "+leaf);
            require(loot.getFirst().has(DataComponents.CUSTOM_NAME)&&loot.getFirst().has(DataComponents.ATTRIBUTE_MODIFIERS),
                    "source presentation and other attributes retained");
        }
        var alias=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:ported/antimagic_sword_loli")));
        require(alias==net.minecraft.world.level.storage.loot.LootTable.EMPTY,"duplicate antimagic leaf removed");
        var legacy=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/antimagic_sword").getFirst();
        var saved=legacy.copy();legacy.set(DataComponents.TOOLTIP_STYLE,net.minecraft.resources.Identifier.parse("lbc:tier_a"));LbcPendingSwords.canonicalizeAntimagic(legacy);
        require(ItemStack.matches(legacy,saved),"existing duplicate normalizes only tier to canonical S");
        var raw=parse("{attributes:[{id:\"minecraft:attack_knockback\",base:100}],equipment:{mainhand:{id:\"minecraft:bone\",count:1,components:{\"minecraft:enchantments\":{\"minecraft:knockback\":1}}}}}");
        var original=raw.copy();SourceKnockbackBalance.entity(raw);
        require(original.getListOrEmpty("attributes").getCompoundOrEmpty(0).getDoubleOr("base",0)==100,"raw source remains independently available");
        var skeleton=new Skeleton(EntityTypes.SKELETON,player.level());
        try {
            skeleton.load(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),raw));
            require(skeleton.getAttribute(Attributes.ATTACK_KNOCKBACK).getBaseValue()==4&&knockback(player,skeleton.getMainHandItem())==1,
                    "native mob attribute reduced first; held KnockbackI survives");
        } finally { skeleton.discard(); }
        var item=parse("{equipment:{mainhand:{components:{\"minecraft:enchantments\":{\"minecraft:knockback\":9},\"minecraft:attribute_modifiers\":[{type:\"minecraft:attack_knockback\",slot:\"mainhand\",id:\"skywars2:source_probe\",operation:\"add_value\",amount:3}]}}}}");
        SourceKnockbackBalance.entity(item);
        var components=item.getCompoundOrEmpty("equipment").getCompoundOrEmpty("mainhand").getCompoundOrEmpty("components");
        var modifier=components.getListOrEmpty("minecraft:attribute_modifiers").getCompoundOrEmpty(0);
        require(modifier.getDoubleOr("amount",-1)==0&&modifier.getStringOr("id","").equals("skywars2:source_probe")
                &&components.getCompoundOrEmpty("minecraft:enchantments").getIntOr("minecraft:knockback",0)==5,
                "source modifier reaches zero before enchantment reduces; original ID remains");
        var exempt=original.copy();exempt.getCompoundOrEmpty("equipment").getCompoundOrEmpty("mainhand")
                .getCompoundOrEmpty("components").put("minecraft:custom_data",parse("{imba:1}"));
        var before=exempt.copy();SourceKnockbackBalance.entity(exempt);require(exempt.equals(before),"original imba source equipment retains exact values");
        var negative=parse("{attributes:[{id:\"minecraft:attack_knockback\",base:-100}]}");
        before=negative.copy();SourceKnockbackBalance.entity(negative);require(negative.equals(before),"negative authored attribute unchanged");
        var runtime=new ItemStack(Items.STICK);runtime.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK),200);
        var flags=new CompoundTag();flags.putInt("runtime_probe",1);runtime.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
        require(knockback(player,runtime)==200,"ordinary runtime/native enchant remains unrestricted outside migration");
        require(SourceKnockbackBalance.MAX_SOURCE_PUNCH==6&&.4+.6*5<3.9&&.4+.6*6>=3.9,
                "source PunchVI is first conventional stationary cardinal full-hit legacy packet saturation");
        var punchKey=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PUNCH);
        for(int sourceLevel:new int[]{2,255}) {
            var bowSource=parse("{equipment:{mainhand:{id:\"minecraft:bow\",count:1,components:{\"minecraft:enchantments\":{\"minecraft:punch\":"+sourceLevel+"}}}}}");
            SourceKnockbackBalance.entity(bowSource);
            var archer=new Skeleton(EntityTypes.SKELETON,player.level());
            try {
                archer.load(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),bowSource));
                require(archer.getMainHandItem().is(Items.BOW)&&archer.getMainHandItem().getEnchantments().getLevel(punchKey)==Math.min(sourceLevel,6),
                        "native authored mob bow Punch boundary; ordinary PunchII unchanged");
            } finally { archer.discard(); }
        }
        var imbaBow=parse("{equipment:{mainhand:{id:\"minecraft:bow\",count:1,components:{\"minecraft:custom_data\":{imba:1},\"minecraft:enchantments\":{\"minecraft:punch\":255}}}}}");
        before=imbaBow.copy();SourceKnockbackBalance.entity(imbaBow);require(imbaBow.equals(before),"imba source Punch255 remains exact");
        var runtimeBow=new ItemStack(Items.BOW);runtimeBow.enchant(punchKey,200);
        require(runtimeBow.getEnchantments().getLevel(punchKey)==200,"runtime bow Punch200 remains unrestricted");
        var egg=new ItemStack(LbcSpecialEggs.Kind.FAT_SKELETON.item());
        require(egg.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("imba",0)==1,"imba egg source exception remains explicit");
        SkyWars2.LOGGER.info("[check] PASS source-knockback-balance");
    }
}
