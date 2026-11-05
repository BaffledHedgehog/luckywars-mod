package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Immutable operation-local TECH geometry. No NBT, world scans, chunk requests or tick cache. */
public final class LbcExplosionSuppression {
    private LbcExplosionSuppression(){}
    public static Coverage coverage(GameController game,ServerLevel level){
        if(game==null)return Coverage.EMPTY;
        var fields=game.items().magic().mechanismFields(level);
        return Coverage.of(fields.radius(),fields.centers());
    }
    /** Cheap indexed admission for ServerLevel before it constructs or publishes an explosion. */
    public static boolean suppressed(GameController game,ServerLevel level,Vec3 point){
        return game!=null&&game.items().magic().mechanismSuppressedAt(level,point);
    }
    public static final class Coverage {
        private static final Coverage EMPTY=new Coverage(0,List.of(),Long2ObjectMaps.emptyMap());
        private final double radiusSquared;
        private final List<Vec3> centers;
        private final Long2ObjectMap<List<Vec3>> nearby;
        private Coverage(double radiusSquared,List<Vec3> centers,Long2ObjectMap<List<Vec3>> nearby){this.radiusSquared=radiusSquared;this.centers=centers;this.nearby=nearby;}
        /** Pure scalar factory also permits qualification without mutating the live field registry. */
        public static Coverage of(double radius,List<Vec3> centers){
            if(!Double.isFinite(radius)||radius<0)throw new IllegalArgumentException("Finite nonnegative field radius");
            if(centers.isEmpty())return EMPTY;
            for(var center:centers){
                if(!Double.isFinite(center.x)||!Double.isFinite(center.y)||!Double.isFinite(center.z))throw new IllegalArgumentException("Finite field center");
            }
            return new Coverage(radius*radius,List.copyOf(centers),new Long2ObjectOpenHashMap<>());
        }
        private static int chunk(double value){return ((int)Math.floor(value))>>4;}
        private static long key(int x,int z){return (long)x&0xffffffffL|((long)z&0xffffffffL)<<32;}
        public boolean empty(){return centers.isEmpty();}
        public int fieldCount(){return centers.size();}
        public int indexedChunks(){return nearby.size();}
        /** True denotes protected geometry, including the exact spherical boundary. */
        public boolean contains(Vec3 point){
            return contains(point.x,point.y,point.z);
        }
        private boolean contains(double x,double y,double z){
            if(empty())return false;int cx=chunk(x),cz=chunk(z);long key=key(cx,cz);var candidates=nearby.get(key);
            if(candidates==null){
                // Only chunks actually reached by this blast get a bucket. Far-away fields never
                // build their own index, and every candidate cell reuses the cached nearby list.
                var selected=new ArrayList<Vec3>();double minX=(double)cx*16,minZ=(double)cz*16;
                for(var center:centers){double dx=Math.max(0,Math.max(minX-center.x,center.x-(minX+16)));
                    double dz=Math.max(0,Math.max(minZ-center.z,center.z-(minZ+16)));
                    if(dx*dx+dz*dz<=radiusSquared)selected.add(center);}
                candidates=List.copyOf(selected);nearby.put(key,candidates);
            }
            for(var center:candidates){double dx=center.x-x,dy=center.y-y,dz=center.z-z;if(dx*dx+dy*dy+dz*dz<=radiusSquared)return true;}
            return false;
        }
        public boolean blocks(BlockPos position){return contains(position.getX()+.5,position.getY()+.5,position.getZ()+.5);}
        public boolean targets(Entity entity){return contains(entity.position());}
        /** Return vanilla's mutable list untouched when no candidate is protected. */
        public List<BlockPos> filterBlocks(List<BlockPos> positions){
            if(empty()||positions.isEmpty())return positions;ArrayList<BlockPos> filtered=null;
            for(int i=0;i<positions.size();i++){var position=positions.get(i);
                if(blocks(position)){if(filtered==null)filtered=new ArrayList<>(positions.subList(0,i));}
                else if(filtered!=null)filtered.add(position);
            }return filtered==null?positions:filtered;
        }
        public List<Entity> filterTargets(List<Entity> entities){
            if(empty()||entities.isEmpty())return entities;ArrayList<Entity> filtered=null;
            for(int i=0;i<entities.size();i++){var entity=entities.get(i);
                if(targets(entity)){if(filtered==null)filtered=new ArrayList<>(entities.subList(0,i));}
                else if(filtered!=null)filtered.add(entity);
            }return filtered==null?entities:filtered;
        }
    }
}
