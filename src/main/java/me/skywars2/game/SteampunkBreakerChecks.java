package me.skywars2.game;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Actual native break/melee inputs independent of durability, plus asynchronous terrain completion. */
public final class SteampunkBreakerChecks {
    private SteampunkBreakerChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Steampunk breaker: "+why);}
    private static Field randomField(Object p){for(Class<?> c=p.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField("random");f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("native player random field");}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final ServerPlayer p;
        final Field randomField;final Object nativeRandom;final Set<UUID> before=new HashSet<>();
        final List<BlockPos> cells;final BlockPos miss,chest;final int mana;final ItemStack pickaxe;
        float roll;int draws,phase,at;Marker field;boolean closed;
        Fixture(GameController game)throws ReflectiveOperationException{
            this.game=game;base=new AllGrimoireChecks.Fixture(game);base.reset();p=base.base.owner;
            p.setYRot(0);p.setXRot(0);p.level().getAllEntities().forEach(e->before.add(e.getUUID()));
            var loot=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/steampunk_pickaxe");
            require(loot.size()==1&&LbcSteampunkBreaker.matches(loot.getFirst())&&loot.getFirst().isDamageableItem(),"real source loot flag and native durability");
            pickaxe=loot.getFirst();pickaxe.remove(DataComponents.ENCHANTMENTS);pickaxe.setDamageValue(0);pickaxe.set(DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE);p.setItemInHand(InteractionHand.MAIN_HAND,pickaxe);
            cells=LbcSteampunkBreaker.cells(p.getEyePosition(),new Vec3(0,0,1));
            require(cells.size()==171,"17 overlapping axis cubes coalesce to exact3x3x19 cells");
            for(var direction:List.of(new Vec3(1,0,0),new Vec3(0,-1,0),new Vec3(1,1,1).normalize(),new Vec3(-1,.3,-.7).normalize())){
                var eyes=new Vec3(-.1,20.4,-.1);var expected=new HashSet<BlockPos>();
                for(int d=1;d<=17;d++){var center=eyes.add(direction.scale(d));
                    int minX=(int)Math.floor(center.x-1),minY=(int)Math.floor(center.y-1),minZ=(int)Math.floor(center.z-1);
                    for(int x=minX;x<=Math.floor(center.x+1);x++)for(int y=minY;y<=Math.floor(center.y+1);y++)for(int z=minZ;z<=Math.floor(center.z+1);z++)expected.add(new BlockPos(x,y,z));}
                var actual=LbcSteampunkBreaker.cells(eyes,direction);
                require(actual.size()==expected.size()&&new HashSet<>(actual).equals(expected),"independent source fill geometry "+direction);
            }
            chest=BlockPos.containing(p.getEyePosition().add(0,0,8));miss=chest.offset(2,0,0);mana=game.items().mana().get(p);
            for(var pos:cells)p.level().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            randomField=randomField(p);nativeRandom=randomField.get(p);
            randomField.set(p,Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{
                if(m.getName().equals("nextFloat")){draws++;return roll;}
                try{return m.invoke(nativeRandom,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            }));at=game.server().getTickCount();
        }
        void action(){int old=pickaxe.getDamageValue(),beforeDraws=draws,beforeMana=game.items().mana().get(p);
            var pos=BlockPos.containing(base.at.add(0,1,-4));
            p.level().setBlock(pos,Blocks.GLASS.defaultBlockState(),3);
            require(p.gameMode.destroyBlock(pos),"actual native block broken");
            require(pickaxe.getDamageValue()==old,"unbreakable tool does not lose durability");
            require(draws==beforeDraws+1,"one chance draw for actual successful block break");
            require(game.items().mana().get(p)==beforeMana,"no mana cost at actual action boundary");}
        void zone(boolean tech){clearZone();field=new Marker(EntityTypes.MARKER,p.level());field.setPos(p.position());field.entityTags().add(tech?"stopper":"stopper_magic");require(p.level().addFreshEntity(field),"native field");game.items().magic().loaded(field);}
        void clearZone(){if(field!=null){game.items().magic().unloaded(field);field.discard();field=null;}}
        void negatives(){var service=game.items().steampunkBreaker();
            roll=.25f;action();require(service.count()==0,"25percent boundary rejects");
            int attempts=draws;p.gameMode.destroyBlock(BlockPos.containing(base.at.add(0,1,-4)));
            require(draws==attempts&&service.count()==0,"destroying empty space is not a break");
            roll=0;zone(true);require(game.items().magic().mechanismSuppressed(p),"TECH20 live field");action();require(service.count()==0,"TECH denies");clearZone();
            var other=new ItemStack(Items.DIAMOND_HELMET);int n=draws;
            other.hurtAndBreak(1,p,EquipmentSlot.HEAD);require(draws==n&&service.count()==0,"armor durability cannot trigger held pickaxe");
            pickaxe.remove(DataComponents.UNBREAKABLE);pickaxe.setDamageValue(10);
            pickaxe.hurtAndBreak(-1,p,EquipmentSlot.MAINHAND);pickaxe.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);
            require(draws==n&&service.count()==0,"repair and standalone durability loss never trigger");
            pickaxe.set(DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE);
            var sheep=new net.minecraft.world.entity.animal.sheep.Sheep(EntityTypes.SHEEP,p.level());
            sheep.setPos(p.position().add(0,0,2));sheep.setNoGravity(true);sheep.setNoAi(true);
            require(p.level().addFreshEntity(sheep),"native melee contact target");
            try{p.attack(sheep);require(service.count()==1,"actual melee contact procs with unbreakable tool");}
            finally{sheep.discard();game.items().stonekivi().stop();}
            require(service.count()==0,"accepted melee work shared cancellation");n=draws;
            p.entityTags().add("time_stopped");try{service.used(p,pickaxe);require(draws==n&&service.count()==0,"TimeStop input rejects");}finally{p.entityTags().remove("time_stopped");}
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{service.used(p,pickaxe);require(draws==n&&service.count()==0,"global freeze rejects");}finally{game.server().tickRateManager().setFrozen(frozen);}
            p.setGameMode(GameType.SPECTATOR);try{service.used(p,pickaxe);require(draws==n&&service.count()==0,"spectator role rejects");}finally{p.setGameMode(GameType.SURVIVAL);}
        }
        boolean tick(){require(game.server().getTickCount()-at<100,"finite source work completes");var service=game.items().steampunkBreaker();
            if(phase==0){negatives();for(var pos:cells)p.level().setBlock(pos,Blocks.COBBLESTONE.defaultBlockState(),3);
                p.level().setBlock(chest,Blocks.CHEST.defaultBlockState(),3);((ChestBlockEntity)p.level().getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,7));
                p.level().setBlock(miss,Blocks.GOLD_BLOCK.defaultBlockState(),3);zone(false);require(game.items().magic().suppressed(p),"MAG live field");
                roll=Math.nextDown(.25f);action();require(service.count()==1,"MAG permits exact one accepted native work");
                require(!game.items().stonekivi().captureSettled(),"partial accepted terrain blocks Vanity capture");
                // Later camera changes must not bend an already accepted source corridor.
                p.setYRot(90);p.setXRot(45);phase++;return false;}
            if(phase==1){if(service.count()!=0)return false;
                for(var pos:cells)require(p.level().getBlockState(pos).isAir(),"complete source corridor "+pos);
                require(p.level().getBlockState(miss).is(Blocks.GOLD_BLOCK),"adjacent outside cube stays untouched");
                int cobble=0,diamonds=0,chests=0;for(var e:p.level().getAllEntities())if(e instanceof ItemEntity item&&!before.contains(item.getUUID())){
                    var s=item.getItem();if(s.is(Items.COBBLESTONE))cobble+=s.getCount();if(s.is(Items.DIAMOND))diamonds+=s.getCount();if(s.is(Items.CHEST))chests+=s.getCount();}
                require(cobble==170&&diamonds==7&&chests==1,"real native block/container drops exact; overlaps never duplicate");
                require(game.items().mana().get(p)>=mana,"ordinary regeneration may continue while terrain finishes");require(game.items().stonekivi().captureSettled(),"final native work settles Vanity");
                clearZone();p.setYRot(0);p.setXRot(0);roll=0;
                p.level().setBlock(chest,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                action();require(service.count()==1,"repeat accepted before later TECH");
                zone(true);require(game.items().magic().mechanismSuppressed(p),"new TECH reaches accepted caster");phase++;return false;}
            if(phase==2){if(service.count()!=0)return false;
                require(p.level().getBlockState(chest).is(Blocks.GOLD_BLOCK),"TECH cancels pending breaker before native writes");
                require(game.items().stonekivi().captureSettled(),"TECH cancellation settles accepted work");
                clearZone();action();require(service.count()==1,"new proc after TECH removal");game.items().stonekivi().stop();require(service.count()==0,"shared match cancellation retires owner");return true;}
            throw new IllegalStateException("unknown phase");
        }
        public void close(){if(closed)return;closed=true;try{clearZone();game.items().stonekivi().stop();randomField.set(p,nativeRandom);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}finally{base.close();}}
    }
    private static Fixture running;
    public static void start(GameController game){require(running==null,"exclusive fixture");try{running=new Fixture(game);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}SkyWars2.LOGGER.info("[check] START steampunk-breaker");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS steampunk-breaker (native successful breaks and melee contacts with unbreakable tool, source loot/25percent boundary, armor/repair/durability exclusion, TECH input and pending cancellation/MAG/TimeStop/role, fixed17cube terrain, exact native block/container drops, nearby miss, shared capture settlement/cancellation)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL steampunk-breaker",error);}}
}
