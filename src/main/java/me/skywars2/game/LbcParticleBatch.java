package me.skywars2.game;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Batch adjacent electric trail samples, preserving particle counts and their spatial variance. */
final class LbcParticleBatch {
    private LbcParticleBatch() { }
    static final class Group implements AutoCloseable {
        private final ServerLevel level;
        private final ParticleOptions particle;
        private final double jitterX,jitterY,jitterZ,speed;
        private final boolean force;
        private int samples,count;
        private double x,y,z,xx,yy,zz,baseX,baseY,baseZ;
        private Vec3 last;
        Group(ServerLevel level,ParticleOptions particle,double jitter,double speed,boolean force) {
            this(level,particle,jitter,jitter,jitter,speed,force);
        }
        Group(ServerLevel level,ParticleOptions particle,double dx,double dy,double dz,double speed,boolean force){
            this.level=level;this.particle=particle;this.jitterX=dx;this.jitterY=dy;this.jitterZ=dz;this.speed=speed;this.force=force;
        }
        void add(Vec3 at){add(at,1);}
        void add(Vec3 at,int amount){
            if(last!=null&&last.distanceToSqr(at)>.75*.75)flush();
            if(count==0){baseX=at.x;baseY=at.y;baseZ=at.z;}
            double dx=at.x-baseX,dy=at.y-baseY,dz=at.z-baseZ;
            count+=amount;samples++;x+=dx*amount;y+=dy*amount;z+=dz*amount;
            xx+=dx*dx*amount;yy+=dy*dy*amount;zz+=dz*dz*amount;last=at;
            if(samples==3)flush();
        }
        @Override public void close(){flush();}
        void flush(){
            if(count==0)return;
            double ax=x/count,ay=y/count,az=z/count;
            level.sendParticles(particle,force,false,baseX+ax,baseY+ay,baseZ+az,count,
                    Math.sqrt(jitterX*jitterX+Math.max(0,xx/count-ax*ax)),
                    Math.sqrt(jitterY*jitterY+Math.max(0,yy/count-ay*ay)),
                    Math.sqrt(jitterZ*jitterZ+Math.max(0,zz/count-az*az)),speed);
            count=samples=0;x=y=z=xx=yy=zz=0;last=null;
        }
    }
}
