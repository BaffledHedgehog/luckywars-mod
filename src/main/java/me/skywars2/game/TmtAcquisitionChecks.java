package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Real native loot and player-dropped stack, using the shared scheduler rather than manual owner calls. */
public final class TmtAcquisitionChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT acquisition: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var s=new Scene(game);running=s;try{s.begin();}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{require(s.game==game,"same controller");if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS tmt-acquisition (real ready loot/native token; two immediate airborne outputs from actual dropped stack, one unit per native tick; no MАГ/TECH suppression; TimeStop, true owner/epoch, private properties/model/codec and no recursive acquisition family)");}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL tmt-acquisition",e);}}
    static void generated(ItemStack stack){
        require(LbcTmtItems.output(stack.getItem())&&stack.getCount()==1&&!LbcTmtAbilities.read(stack).isEmpty(),"one actual randomized native output with private properties");
        require(Identifier.parse("lbc:tmt/procedural").equals(stack.get(DataComponents.ITEM_MODEL))&&stack.get(DataComponents.CUSTOM_MODEL_DATA).colors().getFirst()>>>16==250,"procedural item-atlas model and stable visual marker");
        require(LootTestCatalog.publicNativeStack(stack)&&!LootTestCatalog.publicNativeItem(stack.getItem()),"generated output public, bare transport still excluded");
    }
    static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;final Set<UUID> original=new HashSet<>();
        ItemEntity token;int phase,when,outputs;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcTmtItems.TOKEN));f.level.getAllEntities().forEach(e->original.add(e.getUUID()));}
        void begin(){f.prepareParticipants();
            require(game.items().allItemPools().complete(),"all native item-pool admission is resolved: "+game.items().allItemPools().findings());
            var first=game.lbc().rollTable(f.level,f.home,f.owner,"lbc:ported/tmt");var second=game.lbc().rollTable(f.level,f.home,f.owner,"lbc:ported/tmt");
            require(first.size()==1&&second.size()==1,"actual ready direct table produces one item each roll");generated(first.getFirst());generated(second.getFirst());require(!ItemStack.matches(first.getFirst(),second.getFirst()),"separate actual loot rolls generate fresh properties");
            var stack=game.lbc().rollTable(f.level,f.home,f.owner,"lbc:tmtrainer_drop");require(stack.size()==1&&stack.getFirst().is(LbcTmtItems.TOKEN)&&stack.getFirst().getCount()==1,"original weighted root now returns actual native token");
            require(game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).noneMatch(p->LbcTmtItems.token(LbcTmtComponents.source(game,p))),"acquisition is never recursively sampled as a property");
            require(game.items().allItemPools().slots(LbcAllItemPools.Domain.CUSTOM).stream().filter("factory/tmt"::equals).count()==1,"exactly one logical factory ticket in custom pool");
            f.field(true,f.home.add(2,0,0));f.field(false,f.home.add(-2,0,0));f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack.getFirst().copyWithCount(2));f.pause();
            f.owner.drop(true);require(f.owner.getMainHandItem().isEmpty(),"real player drops entire token stack");
            var fresh=new ArrayList<ItemEntity>();for(var e:f.level.getAllEntities())if(e instanceof ItemEntity item&&!original.contains(e.getUUID())&&item.getItem().is(LbcTmtItems.TOKEN))fresh.add(item);
            require(fresh.size()==1,"one actual thrown token stack");token=fresh.getFirst();f.own(token);require(!token.onGround()&&token.getItem().getCount()==2,"airborne source starts intact");when=game.server().getTickCount();
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-when;
            var fresh=new ArrayList<ItemEntity>();for(var e:f.level.getAllEntities())if(e instanceof ItemEntity item&&!original.contains(e.getUUID())&&LbcTmtItems.matches(item.getItem()))fresh.add(item);
            if(phase==0){require(token.getItem().getCount()==2&&fresh.isEmpty(),"TimeStop pauses acquisition without spending source");if(elapsed<3)return false;f.thaw();phase=1;when=game.server().getTickCount();return false;}
            require(elapsed<=3,"native creation is immediate, without waiting for source landing");
            require(fresh.size()==1,"one generated output per native token tick, no duplicate source execution");var output=fresh.getFirst();f.own(output);generated(output.getItem());
            require(!token.onGround()&&game.items().currentEpoch(output)&&game.items().specialDrops().owner(output)==f.owner,"native airborne output has actual author and epoch");
            var ops=f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);require(ItemStack.matches(output.getItem(),ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,output.getItem()).getOrThrow()).getOrThrow()),"generated private state/appearance survives native component codec");
            f.discard(output);outputs++;
            if(outputs==1){require(!token.isRemoved()&&token.getItem().getCount()==1,"one physical unit consumed, remaining stack retained");return false;}
            require(outputs==2&&token.isRemoved(),"two physical units yield exactly two newly generated outputs");return true;
        }
        public void close(){if(closed)return;closed=true;try{if(f.stopped)f.thaw();stats.close();}finally{f.close();}}
    }
}
