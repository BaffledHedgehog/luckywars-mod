package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.*;

/** Real native placement, private TMT selection, authored carriers, terrain and coordinator replay. */
public final class LegacySlabChecks {
    private LegacySlabChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Legacy slabs: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){
        require(running==null,"exclusive scheduled fixture");var scene=new Scene(game,rewind);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS {} (real native slab placements and single debit; exact source carriers/flags/palette; private white/blood TMT source selection and outer restoration; terrain, stop, WILD authority{} )",scene.rewind?"legacy-slabs-rewind":"legacy-slabs",scene.rewind?", whole coordinator body/source/terrain restore and resumed spread":"");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+(scene.rewind?"legacy-slabs-rewind":"legacy-slabs"),error);}
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;final boolean rewind;final WoolBucketChecks.Fixture f;final LbcLegacySlabs slabs;
        final Set<UUID> initial=new HashSet<>();final Map<net.minecraft.stats.Stat<Item>,Integer> used=new LinkedHashMap<>();
        final int started;BlockPos origin;int phase,when;boolean restoring,closed;
        List<LbcLegacySlabs.View> savedViews;Map<BlockPos,BlockState> savedTerrain;
        Scene(GameController game,boolean rewind){this.game=game;this.rewind=rewind;f=new WoolBucketChecks.Fixture(game,false);slabs=game.items().legacySlabs();started=game.server().getTickCount();
            require(slabs.count()==0&&game.items().stonekivi().count()==0,"exclusive native slab/terrain service");
            for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            for(var item:List.of(LbcLegacySlabs.BLOOD,LbcLegacySlabs.CUM,LbcTmtItems.prototype(2).getItem())){var stat=net.minecraft.stats.Stats.ITEM_USED.get(item);used.put(stat,f.owner.getStats().getValue(stat));}
        }
        void next(int next){phase=next;when=game.server().getTickCount();}
        void begin(){
            f.prepareParticipants();presentation();origin=BlockPos.containing(f.home).offset(0,0,3);chamber(false);place(LbcLegacySlabs.CUM,false,InteractionHand.OFF_HAND);next(0);
        }
        void presentation(){
            require(Blocks.ACACIA_SLAB.asItem()==Items.ACACIA_SLAB&&Blocks.BIRCH_SLAB.asItem()==Items.BIRCH_SLAB
                    &&Blocks.COARSE_DIRT.asItem()==Items.COARSE_DIRT&&Blocks.ANCIENT_DEBRIS.asItem()==Items.ANCIENT_DEBRIS,"all four native aliases preserve vanilla block-to-item mappings for ordinary picks, drops and creative use");
            for(var alias:List.of(LbcImbaTerrainUtilityItems.DIRTER,LbcImbaFollowupItems.DEBRIS)){
                boolean dirter=alias==LbcImbaTerrainUtilityItems.DIRTER;var stack=alias.getDefaultInstance();
                require(alias instanceof BlockItem block&&block.getBlock()==(dirter?Blocks.COARSE_DIRT:Blocks.ANCIENT_DEBRIS)
                        &&ItemData.intValue(stack,dirter?"dirter":"debriser",0)==1&&ItemData.intValue(stack,"imba",0)==1,"shared alias base preserves exact native block and authored source flags");
                require(Identifier.parse(dirter?"minecraft:coarse_dirt":"minecraft:ancient_debris").equals(stack.get(DataComponents.ITEM_MODEL))
                        &&Identifier.parse(dirter?"lbc:tier_b":"lbc:tier_f").equals(stack.get(DataComponents.TOOLTIP_STYLE))
                        &&stack.has(DataComponents.CUSTOM_NAME)&&stack.get(DataComponents.LORE).lines().size()==1&&stack.getMaxStackSize()==64,"shared alias base preserves canonical native display and stack components");
            }
            var blood=LbcLegacySlabs.BLOOD.getDefaultInstance();var white=LbcLegacySlabs.CUM.getDefaultInstance();
            require(blood.getItem() instanceof BlockItem b&&b.getBlock()==Blocks.ACACIA_SLAB&&white.getItem() instanceof BlockItem w&&w.getBlock()==Blocks.BIRCH_SLAB,"actual authored acacia and birch BlockItems");
            require(Identifier.parse("minecraft:acacia_slab").equals(blood.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_k").equals(blood.get(DataComponents.TOOLTIP_STYLE))&&ItemData.intValue(blood,"blood_slab",0)==1,"blood source acacia model/tier K/flag");
            require(Identifier.parse("minecraft:birch_slab").equals(white.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_f").equals(white.get(DataComponents.TOOLTIP_STYLE))&&ItemData.intValue(white,"cum_slab",0)==1,"white source birch model/tier F/flag");
            for(var stack:List.of(blood,white))require(stack.has(DataComponents.CUSTOM_NAME)&&stack.get(DataComponents.LORE).lines().size()==2,"full original translated name and two lore lines");
            var wrong=new ItemStack(Items.ACACIA_SLAB);var tag=new CompoundTag();tag.putInt("cum_slab",1);wrong.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));require(LbcLegacySlabs.canonical(wrong)==null,"cum_slab advancement rejects non-birch carriers");
            var old=new ItemStack(Items.BIRCH_SLAB);old.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));require(LbcLegacySlabs.canonical(old)==LbcLegacySlabs.CUM,"exact original birch source accepted");
            var palette=LbcLegacySlabs.palette();require(palette.size()==32&&palette.get(0)==Blocks.AIR&&palette.get(1)==Blocks.CAVE_AIR&&palette.get(2)==Blocks.VOID_AIR&&palette.get(4)==Blocks.LAVA&&palette.get(14)==Blocks.TNT&&palette.get(7)==Blocks.NETHER_BRICKS&&palette.get(19)==Blocks.NETHER_BRICKS&&palette.get(31)==Blocks.FIRE_CORAL_BLOCK,"literal authored weighted 1..32 blood palette including duplicate weight");
            var bloodTag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse("lbc:blood_slabbed"));
            for(var block:palette)require(block.defaultBlockState().is(bloodTag),"every source replacement is blood_slabbed: "+BuiltInRegistries.BLOCK.getKey(block));
            require(!Blocks.STONE.defaultBlockState().is(bloodTag),"ordinary stone remains infectable");
        }
        void chamber(boolean blood){
            // Netherrack walls/floor are in the original blood_slabbed tag, bounding the test.
            // The low solid ceiling also rules out random blood mob branches in this scene.
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=2;y++){
                var at=origin.offset(x,y,z);var block=y==-1||y==2||Math.abs(x)==4||Math.abs(z)==4?Blocks.NETHERRACK:blood&&Math.abs(x)<=2&&Math.abs(z)<=2?Blocks.STONE:Blocks.AIR;
                f.set(at,block.defaultBlockState());
            }
            // A genuine eye ray starts outside the target and sees the newly placed slab first.
            for(int z=-3;z<=0;z++)for(int y=0;y<=1;y++)f.set(origin.offset(0,y,z),Blocks.AIR.defaultBlockState());
            f.move(f.owner,Vec3.atBottomCenterOf(origin.offset(0,0,-3)));f.owner.setYRot(0);f.owner.setXRot(25);
        }
        ItemStack composed(Item nativeItem){
            var source=nativeItem.getDefaultInstance();var data=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            data.putString(LbcTmtAbilities.NATIVE,BuiltInRegistries.ITEM.getKey(nativeItem).toString());LbcTmtComponents.capture(game,source,data);
            var stack=LbcTmtItems.prototype(2);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            // Contradictory outer flag must never leak into the selected private native source.
            root.putInt(nativeItem==LbcLegacySlabs.CUM?"blood_slab":"cum_slab",1);
            LbcTmtAbilities.write(root,List.of(new LbcTmtAbilities.Ability("native/check/"+BuiltInRegistries.ITEM.getKey(nativeItem).getPath(),CustomData.of(data))));
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return stack;
        }
        void place(Item nativeItem,boolean tmt,InteractionHand hand){
            var stack=tmt?composed(nativeItem):nativeItem.getDefaultInstance();stack.setCount(2);var before=stack.copyWithCount(1);
            f.owner.setItemInHand(hand,stack);var otherHand=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            var other=new ItemStack(Items.STICK,7);f.owner.setItemInHand(otherHand,other);
            var hit=new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false);
            require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,hand,hit).consumesAction(),"actual registered native use-on accepted "+BuiltInRegistries.ITEM.getKey(nativeItem));
            require(stack.getCount()==1&&f.owner.getItemInHand(otherHand)==other&&other.getCount()==7,"one accepted used-hand debit and exact other hand");
            require(ItemStack.matches(stack,before)&&!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"full outer stack and all native scopes restored");
            require(f.level.getBlockState(origin).is(nativeItem==LbcLegacySlabs.CUM?Blocks.BIRCH_SLAB:Blocks.ACACIA_SLAB),"real authored placed block output");
        }
        boolean propagated(boolean blood){
            int count=0;for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var state=f.level.getBlockState(origin.offset(x,0,z));if(blood?!state.is(Blocks.STONE)&&!state.isAir()&&!state.is(Blocks.ACACIA_SLAB):state.is(Blocks.BIRCH_SLAB))count++;}return count>3;
        }
        void authority(boolean blood){
            var views=slabs.views();require(!views.isEmpty(),"real native selector body survives to a later frame");
            for(var view:views){require((view.kind()==LbcLegacySlabs.Kind.BLOOD)==blood,"selected private source launches only its own slab family");var source=view.source();
                require(source.author().equals(f.owner.getUUID())&&source.cause()!=null&&source.cause().player().equals(f.owner.getUUID())&&source.cause().friendlyFire()&&game.items().currentEpoch(source.epoch()),"captured creator/order/epoch and WILD policy");
                require(game.items().spellSources().target(source,f.owner)&&game.items().spellSources().target(source,f.other),"native hazards include creator and other active player");
            }
        }
        Map<BlockPos,BlockState> terrain(){var result=new LinkedHashMap<BlockPos,BlockState>();for(var at:f.terrain.keySet())result.put(at,f.level.getBlockState(at));return result;}
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<1000,"bounded real scheduler phase="+phase);
            if(restoring&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(!propagated(false))return false;authority(false);
                if(rewind){f.pause();savedViews=slabs.views();savedTerrain=terrain();require(game.vanity().requestCapture(f.owner,false),"capture accepted native slab producer state");next(1);}else next(4);return false;}
            if(phase==1){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;require(slabs.views().equals(savedViews)&&terrain().equals(savedTerrain),"TimeStop holds exact accepted bodies and terrain during capture");f.thaw();next(2);return false;}
            if(phase==2){if(elapsed<2)return false;require(!terrain().equals(savedTerrain),"native future spread really advanced after capture");f.pause();for(var actor:slabs.snapshot())f.discard(actor);require(game.vanity().requestRestoreForCheck(),"restore removed slab bodies and changed terrain");restoring=true;next(3);return false;}
            if(phase==3){if(game.vanity().busy())return false;require(slabs.views().equals(savedViews)&&terrain().equals(savedTerrain),"whole coordinator restores exact UUID/kind/authority and terrain");for(var view:savedViews)require(f.level.getEntity(view.body())!=null&&!f.level.getEntity(view.body()).isRemoved(),"same canonical native marker UUID is registered again");require(game.items().timeStop().stopped(f.level),"captured native TimeStop restored");game.vanity().stop();f.thaw();next(4);return false;}
            if(phase==4){if(slabs.count()!=0)return false;require(propagated(false),"actual white downward/horizontal fill survives producer retirement");boolean bottom=false;for(var at:f.terrain.keySet()){var s=f.level.getBlockState(at);bottom|=s.is(Blocks.BIRCH_SLAB)&&s.getValue(SlabBlock.TYPE)==SlabType.BOTTOM;}require(bottom,"white central source produces bottom slabs");
                chamber(false);place(LbcLegacySlabs.CUM,true,InteractionHand.MAIN_HAND);next(5);return false;}
            if(phase==5){if(!propagated(false))return false;authority(false);slabs.stop();chamber(true);place(LbcLegacySlabs.BLOOD,true,InteractionHand.OFF_HAND);next(6);return false;}
            if(phase==6){if(!propagated(true))return false;if(slabs.count()>0)authority(true);slabs.stop();require(slabs.count()==0,"explicit native teardown removes all live slab producers");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();slabs.stop();if(f.stopped)f.thaw();for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(e.getUUID())){f.own(e);game.items().entityUnloaded(e);e.discard();}used.forEach((s,v)->f.owner.getStats().setValue(f.owner,s,v));}finally{f.close();}}
    }
}
