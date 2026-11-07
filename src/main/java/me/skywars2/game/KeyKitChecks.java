package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Real menu/start and last-use block loot, including the pre-wear TOOL snapshot. */
public final class KeyKitChecks {
    private static Scene running;
    private KeyKitChecks() {}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Key kits: "+why);}
    static void menu(GameController game,boolean lucky){
        require(game.inLobby(),"lobby selection");
        var player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var profile=game.players().get(player.getUUID());
        String gate="lbc:true_advancements/"+(lucky?"hoes/op_hoe":"axes/randchanted");
        int id=lucky?16:14;
        game.config().ranked=false;game.config().lbcKits=true;
        profile.lbcKit=0;profile.lbcUnlocks.remove(gate);
        require(!game.lbc().select(player,LbcLoadouts.Kind.KITS,9000,id-1),"source achievement gate");
        require(game.lbc().unlock(player,gate)&&game.lbc().select(player,LbcLoadouts.Kind.KITS,9000,id-1)&&profile.lbcKit==id,"native source unlock and menu selection");
        game.config().save();
        SkyWars2.LOGGER.info("[check] PASS {} (original source gate and selection)",lucky?"luck-key-kit-menu":"key-kit-menu");
    }
    static void run(GameController game,boolean lucky){
        require(running==null,"exclusive native scene");
        var scene=new Scene(game,lucky);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    static void tick(GameController game){
        var scene=running;if(scene==null)return;
        try{
            require(scene.f.game==game,"same controller");if(!scene.step())return;
            running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS {} (actual selected start grant, native one-use wear, last-use loot, no cross-player cleanup)",scene.lucky?"luck-key-kit":"key-kit");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+(scene.lucky?"luck-key-kit":"key-kit"),error);}
    }
    private static final class Scene implements AutoCloseable {
        final WoolBucketChecks.Fixture f;
        final NativeMeleeChecks.NativeStats stats;
        final boolean lucky;
        ItemStack supplied,unrelated;
        int started,phase;
        Scene(GameController game,boolean lucky){this.lucky=lucky;f=new WoolBucketChecks.Fixture(game,false);stats=new NativeMeleeChecks.NativeStats(f.owner);}
        void begin(){
            var kind=lucky?LbcNativeToolItems.Kind.LUCK_KEY:LbcNativeToolItems.Kind.KEY;
            var keys=new ArrayList<ItemStack>();
            for(int i=0;i<f.owner.getInventory().getContainerSize();i++){var s=f.owner.getInventory().getItem(i);if(s.is(kind.item))keys.add(s);}
            require(keys.size()==1&&keys.getFirst().getCount()==1,"exact real match startup key");supplied=keys.getFirst().copy();
            require(supplied.getMaxDamage()==1&&supplied.getDamageValue()==0&&supplied.get(DataComponents.TOOL)!=null,"one-use native source tool");
            f.prepareParticipants();f.owner.getAttribute(Attributes.LUCK).setBaseValue(0);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,supplied.copy());
            unrelated=new ItemStack(Items.STICK);unrelated.set(DataComponents.MAX_STACK_SIZE,1);unrelated.set(DataComponents.MAX_DAMAGE,2);unrelated.set(DataComponents.DAMAGE,1);var data=new net.minecraft.nbt.CompoundTag();data.putInt("luck_key",1);unrelated.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
            f.other.getInventory().setItem(9,unrelated);started=f.game.server().getTickCount();
        }
        List<ItemStack> mine(LbcLuckyBlocks.Kind kind){
            var pos=f.owner.blockPosition().offset(2,0,0);f.set(pos,LbcLuckyBlocks.block(kind).defaultBlockState());
            var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            require(f.owner.gameMode.destroyBlock(pos)&&f.level.getBlockState(pos).isAir(),"actual native destroyBlock");
            require(f.owner.getMainHandItem().isEmpty(),"key consumed by native wear before loot");
            var drops=f.level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3),e->!before.contains(e.getUUID()));
            var result=new ArrayList<ItemStack>();for(var e:drops){f.own(e);result.add(e.getItem().copy());}return result;
        }
        boolean step(){
            int elapsed=f.game.server().getTickCount()-started;require(elapsed<30,"bounded native equipment progress");if(elapsed<3)return false;
            if(phase==0){
                require(f.owner.getAttributeValue(Attributes.LUCK)==(lucky?32:0),"actual native mainhand luck attribute");
                var out=mine(lucky?LbcLuckyBlocks.Kind.NEKO:LbcLuckyBlocks.Kind.NORMAL);
                if(lucky){require(out.stream().allMatch(s->s.is(Items.AMETHYST_SHARD))&&out.stream().mapToInt(ItemStack::getCount).sum()==2,"last-use +32 luck retains both source high-luck TMT pools: "+out);
                    // Below the high-luck threshold the key excludes both ordinary Neko pools.
                    // Live mainhand is already empty: only the actual TOOL snapshot can do this.
                    f.owner.getAttribute(Attributes.LUCK).setBaseValue(-32);
                }else{
                    require(!out.isEmpty()&&out.stream().allMatch(s->s.is(Items.DIAMOND))&&out.stream().mapToInt(ItemStack::getCount).sum()>=1,"Fortune64 key produces genuine last-use ordinary loot");
                }
                phase++;started=f.game.server().getTickCount();return false;
            }
            if(lucky&&phase==1){
                // Let vanilla observe the empty slot before equipping an identical replacement.
                require(f.owner.getMainHandItem().isEmpty()&&f.owner.getAttributeValue(Attributes.LUCK)==-32,"broken key removes its actual native attribute");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,supplied.copy());phase++;started=f.game.server().getTickCount();return false;
            }
            if(lucky&&phase==2){require(f.owner.getAttributeValue(Attributes.LUCK)==0,"low-luck snapshot control");require(mine(LbcLuckyBlocks.Kind.NEKO).isEmpty(),"pre-wear TOOL marker excludes ordinary Neko rewards after hand emptied");phase++;started=f.game.server().getTickCount();return false;}
            require(f.other.getInventory().getItem(9)==unrelated&&unrelated.getDamageValue()==1,"unrelated damaged legacy key survives beyond old two-tick global cleanup");return true;
        }
        public void close(){try{stats.close();}finally{f.close();}}
    }
}
