package me.skywars2.game;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Batch nearby trail samples, preserving particle counts and their spatial variance. */
final class LbcParticleBatch {
    private LbcParticleBatch() { }
    /** Keep native particles along short axis-aligned runs, rather than a Gaussian cloud.
     * Client sampling fills the original run uniformly; its bounds, count and
     * particle providers stay unchanged. A turn always starts a separate packet.
     */
    static final class AxisLine implements AutoCloseable {
        private final ServerLevel level;
        private final ParticleOptions[] particles;
        private final Vec3[] points=new Vec3[7];
        private Vec3 step;
        private int count;
        AxisLine(ServerLevel level,ParticleOptions... particles){this.level=level;this.particles=particles.clone();}
        void add(Vec3 at){
            if(count>0){
                var delta=at.subtract(points[count-1]);
                int axes=(Math.abs(delta.x)>1e-6?1:0)+(Math.abs(delta.y)>1e-6?1:0)+(Math.abs(delta.z)>1e-6?1:0);
                if(axes!=1||delta.lengthSqr()>.010001||step!=null&&delta.distanceToSqr(step)>1e-12)flush();
                if(count>0)step=delta;
            }
            points[count++]=at;if(count==points.length)flush();
        }
        @Override public void close(){flush();}
        private void flush(){
            if(count==0)return;
            var from=points[0];var span=points[count-1].subtract(from);
            var randomization=count==1?ClientboundLevelParticlesPacket.RandomizationType.DEFAULT:ClientboundLevelParticlesPacket.RandomizationType.ALTERNATIVE;
            var packets=new ClientboundLevelParticlesPacket[particles.length];
            for(int i=0;i<particles.length;i++)packets[i]=new ClientboundLevelParticlesPacket(particles[i],false,false,
                from.x,from.y,from.z,(float)span.x,(float)span.y,(float)span.z,0,0,0,count,randomization);
            for(var player:level.players()){
                // Preserve the native32-block admission at a range boundary:
                // only full runs batch; mixed visibility keeps individual points.
                int visible=0;for(int i=0;i<count;i++)if(player.blockPosition().closerToCenterThan(points[i],32))visible++;
                if(visible==count){for(var packet:packets)level.sendParticles(player,false,from.x,from.y,from.z,packet);}
                else if(visible>0)for(int i=0;i<count;i++)for(var particle:particles)
                    level.sendParticles(player,particle,false,false,points[i].x,points[i].y,points[i].z,1,0,0,0,0);
            }
            java.util.Arrays.fill(points,0,count,null);count=0;step=null;
        }
    }
    static final class Group implements AutoCloseable {
        private final ServerLevel level;
        private final ParticleOptions particle;
        private final double jitterX,jitterY,jitterZ,speed;
        private final boolean force;
        private final Vec3[] points;
        private final int[] amounts;
        private final double maxStepSquared;
        private int samples,count;
        private double x,y,z,xx,yy,zz,baseX,baseY,baseZ;
        Group(ServerLevel level,ParticleOptions particle,double jitter,double speed,boolean force) {
            this(level,particle,jitter,jitter,jitter,speed,force);
        }
        Group(ServerLevel level, ParticleOptions particle, double jitter, double speed, boolean force,
              double maxStep, int samplesPerPacket) {
            this(level, particle, jitter, jitter, jitter, speed, force, maxStep, samplesPerPacket);
        }
        Group(ServerLevel level,ParticleOptions particle,double dx,double dy,double dz,double speed,boolean force){
            this(level,particle,dx,dy,dz,speed,force,.75,3);
        }
        Group(ServerLevel level,ParticleOptions particle,double dx,double dy,double dz,double speed,boolean force,
              double maxStep,int samplesPerPacket){
            this.level=level;this.particle=particle;this.jitterX=dx;this.jitterY=dy;this.jitterZ=dz;this.speed=speed;this.force=force;
            this.maxStepSquared=maxStep*maxStep;
            this.points=new Vec3[samplesPerPacket];this.amounts=new int[samplesPerPacket];
        }
        void add(Vec3 at){add(at,1);}
        void add(Vec3 at,int amount){
            if(samples>0&&points[samples-1].distanceToSqr(at)>maxStepSquared)flush();
            if(count==0){baseX=at.x;baseY=at.y;baseZ=at.z;}
            double dx=at.x-baseX,dy=at.y-baseY,dz=at.z-baseZ;
            points[samples]=at;amounts[samples]=amount;
            count+=amount;samples++;x+=dx*amount;y+=dy*amount;z+=dz*amount;
            xx+=dx*dx*amount;yy+=dy*dy*amount;zz+=dz*dz*amount;
            if(samples==points.length)flush();
        }
        @Override public void close(){flush();}
        void flush(){
            if(count==0)return;
            double ax=x/count,ay=y/count,az=z/count;
            if(samples==1){
                level.sendParticles(particle,force,false,baseX,baseY,baseZ,count,jitterX,jitterY,jitterZ,speed);
            }else{
                double cx=baseX+ax,cy=baseY+ay,cz=baseZ+az;
                var packet=new ClientboundLevelParticlesPacket(particle,force,false,cx,cy,cz,
                        (float)Math.sqrt(jitterX*jitterX+Math.max(0,xx/count-ax*ax)),
                        (float)Math.sqrt(jitterY*jitterY+Math.max(0,yy/count-ay*ay)),
                        (float)Math.sqrt(jitterZ*jitterZ+Math.max(0,zz/count-az*az)),(float)speed,count);
                double radius=force?512:32;
                for(var player:level.players()){
                    int visible=0;
                    var viewer=player.blockPosition();
                    for(int i=0;i<samples;i++)if(viewer.closerToCenterThan(points[i],radius))visible++;
                    if(visible==samples){
                        level.sendParticles(player,force,cx,cy,cz,packet);
                    }else if(visible>0){
                        for(int i=0;i<samples;i++){
                            var at=points[i];
                            level.sendParticles(player,particle,force,false,at.x,at.y,at.z,amounts[i],
                                    jitterX,jitterY,jitterZ,speed);
                        }
                    }
                }
            }
            java.util.Arrays.fill(points,0,samples,null);
            count=samples=0;x=y=z=xx=yy=zz=0;
        }
    }
}
