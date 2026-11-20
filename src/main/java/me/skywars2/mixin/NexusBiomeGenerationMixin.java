package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.concurrent.CompletableFuture;
import me.skywars2.game.LbcDimensionItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkStatusTasks.class)
public abstract class NexusBiomeGenerationMixin {
    @Unique
    private static final ResourceKey<Biome> skywars2$NEXUS_BIOME =
            ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("nexus"));

    @WrapOperation(method = "generateBiomes", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/chunk/ChunkGenerator;createBiomes("
                    + "Lnet/minecraft/world/level/levelgen/RandomState;"
                    + "Lnet/minecraft/world/level/levelgen/blending/Blender;"
                    + "Lnet/minecraft/world/level/StructureManager;"
                    + "Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<ChunkAccess> skywars2$fillUniformNexusBiomes(
            ChunkGenerator generator, RandomState randomState, Blender blender, StructureManager structures,
            ChunkAccess chunk, Operation<CompletableFuture<ChunkAccess>> original,
            @Local(argsOnly = true) WorldGenContext context
    ) {
        var level = context.level();
        if (!LbcDimensionItems.nexus(level) || generator.getClass() != FlatLevelSource.class
                || generator.getBiomeSource().getClass() != FixedBiomeSource.class || blender != Blender.empty()
                || chunk.getClass() != ProtoChunk.class || !chunk.getPersistedStatus().isBefore(ChunkStatus.BIOMES)
                || chunk.isUpgrading()) {
            return original.call(generator, randomState, blender, structures, chunk);
        }
        var biome = ((FixedBiomeSource) generator.getBiomeSource()).getNoiseBiome(0, 0, 0);
        if (!biome.is(skywars2$NEXUS_BIOME)) return original.call(generator, randomState, blender, structures, chunk);

        var strategy = level.palettedContainerFactory().biomeStrategy();
        return CompletableFuture.supplyAsync(() -> {
            // Keep the native section layout and a separate mutable palette in every section.
            for (var section : chunk.getSections()) {
                ((SectionBiomes) section).skywars2$setBiomes(new PalettedContainer<>(biome, strategy));
            }
            return chunk;
        }, Util.backgroundExecutor().forName("createBiomes"));
    }

    @Mixin(LevelChunkSection.class)
    public interface SectionBiomes {
        @Accessor("biomes")
        void skywars2$setBiomes(PalettedContainerRO<Holder<Biome>> biomes);
    }
}
