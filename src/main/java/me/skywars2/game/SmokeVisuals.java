package me.skywars2.game;

import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Legacy gray wool markers, sent as one native particle batch per grenade per tick. */
public final class SmokeVisuals {
    private static final BlockParticleOption SMOKE = new BlockParticleOption(ParticleTypes.BLOCK_MARKER,
            Blocks.WOOL.pick(DyeColor.GRAY).defaultBlockState());
    public interface Volume { ServerLevel level(); Vec3 center(); }
    private final GameController game;
    private int lastEmission=Integer.MIN_VALUE;
    public SmokeVisuals(GameController game){this.game=game;}
    public static boolean particle(ParticleOptions option){
        return option instanceof BlockParticleOption block && block.getType()==ParticleTypes.BLOCK_MARKER
                && block.getState()==SMOKE.getState();
    }
    public void synchronize(List<? extends Volume> volumes){
        int tick=game.server().getTickCount();
        if(tick==lastEmission||!game.server().tickRateManager().runsNormally())return;
        lastEmission=tick;
        for(var volume:volumes){
            var level=volume.level();
            if(game.items().timeStop().stopped(level)||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(volume.center())))continue;
            var at=volume.center();
            level.sendParticles(SMOKE,true,false,at.x,at.y+2.5,at.z,100,7,4,7,0);
        }
    }
    public void stop(){lastEmission=Integer.MIN_VALUE;}
}
