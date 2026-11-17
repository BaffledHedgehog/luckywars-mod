package me.skywars2.game;

import java.util.List;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Short native billows: local four-tick lifetime, with no clock or per-puff entity. */
final class SmokeVisuals {
    static final int COLOR_BASE=0xff534d00;
    static final int PUFFS_PER_TICK=48;
    interface Volume { ServerLevel level(); Vec3 center(); double radius(); double density(); }
    private final GameController game;
    private int lastEmission=Integer.MIN_VALUE;
    SmokeVisuals(GameController game){this.game=game;}
    static boolean particle(net.minecraft.core.particles.ParticleOptions option){
        return option instanceof ColorParticleOption color && color.getType()==ParticleTypes.FLASH
            && Math.round(color.getRed()*255)==83 && Math.round(color.getGreen()*255)==77;
    }
    void synchronize(List<? extends Volume> volumes){
        int tick=game.server().getTickCount();
        if(tick==lastEmission||!game.server().tickRateManager().runsNormally())return;
        lastEmission=tick;
        for(var volume:volumes){
            var level=volume.level();
            if(game.items().timeStop().stopped(level)||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(volume.center())))continue;
            double radius=Math.max(.1,Math.min(7.5,volume.radius()));
            double density=Math.max(0,Math.min(1,volume.density()));
            int size=Math.max(1,Math.min(127,(int)Math.round(radius/7.5*127)));
            int count=Math.max(1,(int)Math.ceil(PUFFS_PER_TICK*density*density));
            var particle=ColorParticleOption.create(ParticleTypes.FLASH,COLOR_BASE|size);
            var at=volume.center().add(0,radius/3,0);
            // Legacy spread is a Gaussian sigma, not a hard radius. At full growth
            // keep its exact7/4/7 andY2.5; existing fading volumes leave a motion trail.
            level.sendParticles(particle,true,false,at.x,at.y,at.z,count,radius*(7/7.5),radius*(4/7.5),radius*(7/7.5),0);
        }
    }
    void stop(){lastEmission=Integer.MIN_VALUE;}
}
