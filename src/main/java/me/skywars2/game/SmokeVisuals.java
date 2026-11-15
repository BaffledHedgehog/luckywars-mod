package me.skywars2.game;

import java.util.List;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Bounded public billboard puffs. Existing client particles own their drift/lifetime. */
final class SmokeVisuals {
    static final int COLOR_BASE=0xff534d00;
    interface Volume { ServerLevel level(); Vec3 center(); double radius(); double density(); }
    private final GameController game;
    private int lastEmission=Integer.MIN_VALUE;
    SmokeVisuals(GameController game){this.game=game;}
    static boolean particle(net.minecraft.core.particles.ParticleOptions option){
        return option instanceof ColorParticleOption color && Math.round(color.getRed()*255)==83 && Math.round(color.getGreen()*255)==77;
    }
    void synchronize(List<? extends Volume> volumes){
        int tick=game.server().getTickCount();
        if(tick==lastEmission||tick%2!=0||!game.server().tickRateManager().runsNormally())return;
        lastEmission=tick;
        for(var volume:volumes){
            var level=volume.level();
            if(game.items().timeStop().stopped(level)||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(volume.center())))continue;
            double radius=Math.max(.1,Math.min(7.5,volume.radius()));
            double density=Math.max(0,Math.min(1,volume.density()));
            int size=Math.max(1,Math.min(127,(int)Math.round(radius/7.5*127)));
            int count=Math.max(1,(int)Math.ceil(12*density*density));
            var particle=ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,COLOR_BASE|size);
            var at=volume.center().add(0,radius/3,0);
            // Source spread was7/4/7 atY2.5. Native drift leaves smoke behind a moved grenade.
            level.sendParticles(particle,true,false,at.x,at.y,at.z,count,radius*.45,radius*.255,radius*.45,.01);
        }
    }
    void stop(){lastEmission=Integer.MIN_VALUE;}
}
