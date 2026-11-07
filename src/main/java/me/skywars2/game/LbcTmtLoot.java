package me.skywars2.game;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.SingleEntryContainerBase;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Direct loot access to the same factory used by the single all-item-pool TMT slot. */
public final class LbcTmtLoot extends SingleEntryContainerBase {
    public static final MapCodec<LbcTmtLoot> MAP_CODEC=RecordCodecBuilder.mapCodec(i->uniformFields(i).apply(i,LbcTmtLoot::new));
    private LbcTmtLoot(int weight,int quality,Optional<Holder<LootItemCondition>> condition,Optional<Holder<LootItemFunction>> modifier){super(weight,quality,condition,modifier);}
    static void register(){Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE,Identifier.parse("skywars2:tmt"),MAP_CODEC);}
    @Override public MapCodec<LbcTmtLoot> codec(){return MAP_CODEC;}
    @Override public void createItemStack(Consumer<ItemStack> output,LootContext context){
        var game=SkyWars2.game();if(game==null)throw new IllegalStateException("TMT loot requires its native game owner");
        output.accept(game.items().tmt().generate(context.getRandom()));
    }
}
