package me.skywars2.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/** Map-owned, persistent disjoint regions. All mutation belongs to the server thread. */
public final class BobaRegionPhysics {
    public record Region(int x0,int y0,int z0,int x1,int y1,int z1,int turns){
        boolean contains(BlockPos p){return p.getX()>=x0&&p.getX()<x1&&p.getY()>=y0&&p.getY()<y1&&p.getZ()>=z0&&p.getZ()<z1;}
    }
    private List<Region> regions=List.of();
    public static final ThreadLocal<Integer> FLUID_FRAME=ThreadLocal.withInitial(()->0);
    private final Path journal;
    BobaRegionPhysics(Path journal){this.journal=journal;read();}
    public List<Region> capture(){return List.copyOf(regions);}
    public void restore(List<Region> image){regions=List.copyOf(image);save();}
    public void clear(){restore(List.of());}
    public int turns(BlockPos pos){for(var region:regions)if(region.contains(pos))return region.turns;return 0;}
    public static int turns(BlockGetter level,BlockPos pos){
        var game=SkyWars2.game();return game!=null&&level==game.maps().level()?game.maps().rotationPhysics().turns(pos):0;
    }
    public static int turns(Entity entity){return entity instanceof Player?0:turns(entity.level(),entity.blockPosition());}
    public static Vec3 rotate(Vec3 v,int turns){
        return switch(turns&3){case 1->new Vec3(v.x,-v.z,v.y);case 2->new Vec3(v.x,-v.y,-v.z);case 3->new Vec3(v.x,v.z,-v.y);default->v;};
    }
    public static Direction rotate(Direction d,int turns){
        var v=rotate(new Vec3(d.getStepX(),d.getStepY(),d.getStepZ()),turns);
        return Direction.getApproximateNearest(v);
    }
    void rotate(BlockPos origin){
        int x0=origin.getX()-160,z0=origin.getZ()-160;
        var cube=new Region(x0,0,z0,x0+320,320,z0+320,1);
        var result=new ArrayList<Region>();var inside=new ArrayList<Region>();
        for(var old:regions){
            int ax=Math.max(old.x0,cube.x0),ay=Math.max(old.y0,cube.y0),az=Math.max(old.z0,cube.z0);
            int bx=Math.min(old.x1,cube.x1),by=Math.min(old.y1,cube.y1),bz=Math.min(old.z1,cube.z1);
            if(ax>=bx||ay>=by||az>=bz){result.add(old);continue;}
            add(result,old.x0,old.y0,old.z0,ax,old.y1,old.z1,old.turns);
            add(result,bx,old.y0,old.z0,old.x1,old.y1,old.z1,old.turns);
            add(result,ax,old.y0,old.z0,bx,ay,old.z1,old.turns);
            add(result,ax,by,old.z0,bx,old.y1,old.z1,old.turns);
            add(result,ax,ay,old.z0,bx,by,az,old.turns);
            add(result,ax,ay,bz,bx,by,old.z1,old.turns);
            inside.add(new Region(ax,320-(bz-z0),z0+ay,bx,320-(az-z0),z0+by,(old.turns+1)&3));
        }
        // Fill newly affected space, then carve old transformed orientations out of it.
        var uncovered=new ArrayList<Region>();uncovered.add(cube);
        for(var overlay:inside){var next=new ArrayList<Region>();for(var base:uncovered)subtract(base,overlay,next);uncovered=next;}
        result.addAll(uncovered);for(var r:inside)if(r.turns!=0)result.add(r);
        regions=List.copyOf(result);save();
        var game=SkyWars2.game();
        if(game!=null&&game.maps().rotationPhysics()==this)for(var entity:game.maps().level().getAllEntities())
            if(!(entity instanceof Player)&&cube.contains(entity.blockPosition()))entity.setPos(entity.position());
    }
    private static void subtract(Region a,Region b,List<Region> out){
        int x=Math.max(a.x0,b.x0),y=Math.max(a.y0,b.y0),z=Math.max(a.z0,b.z0),X=Math.min(a.x1,b.x1),Y=Math.min(a.y1,b.y1),Z=Math.min(a.z1,b.z1);
        if(x>=X||y>=Y||z>=Z){out.add(a);return;}
        add(out,a.x0,a.y0,a.z0,x,a.y1,a.z1,a.turns);add(out,X,a.y0,a.z0,a.x1,a.y1,a.z1,a.turns);
        add(out,x,a.y0,a.z0,X,y,a.z1,a.turns);add(out,x,Y,a.z0,X,a.y1,a.z1,a.turns);
        add(out,x,y,a.z0,X,Y,z,a.turns);add(out,x,y,Z,X,Y,a.z1,a.turns);
    }
    private static void add(List<Region> out,int x,int y,int z,int X,int Y,int Z,int turns){if(x<X&&y<Y&&z<Z&&turns!=0)out.add(new Region(x,y,z,X,Y,Z,turns));}
    private void read(){
        if(!Files.exists(journal))return;
        try{var image=new ArrayList<Region>();for(var line:Files.readAllLines(journal,java.nio.charset.StandardCharsets.UTF_8)){
            var fields=line.split(",");if(fields.length!=7)throw new IllegalArgumentException("Invalid Boba region");
            var v=java.util.Arrays.stream(fields).mapToInt(Integer::parseInt).toArray();
            if(v[0]>=v[3]||v[1]>=v[4]||v[2]>=v[5]||v[6]<1||v[6]>3)throw new IllegalArgumentException("Invalid Boba region bounds");
            image.add(new Region(v[0],v[1],v[2],v[3],v[4],v[5],v[6]));
        }regions=List.copyOf(image);}catch(Exception e){throw new IllegalStateException("Cannot restore Boba region journal "+journal,e);}
    }
    private void save(){
        try{Files.createDirectories(journal.getParent());var temp=journal.resolveSibling(journal.getFileName()+".tmp");
            var rows=regions.stream().map(r->r.x0+","+r.y0+","+r.z0+","+r.x1+","+r.y1+","+r.z1+","+r.turns).toList();
            Files.write(temp,rows,java.nio.charset.StandardCharsets.UTF_8);
            try{Files.move(temp,journal,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(temp,journal,StandardCopyOption.REPLACE_EXISTING);}
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot save Boba regions",e);}
    }
}
