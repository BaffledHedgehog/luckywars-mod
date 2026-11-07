package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real native use-on for all authored glyph kinds and all five physical TMT transports. */
public final class TmtGlyphChecks {
    private TmtGlyphChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT glyph: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scheduled scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-glyph (4 authored kinds x 5 actual TMT carriers, both hands, genuine clicked-face/empty-cell native placement and scheduled lifecycle, independent private source and harmless sibling, sole debit, complete outer/codec restoration, actual MAG rejection/retry, bare-selector and air-use exclusions)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-glyph",error);}
    }
    private static final class Scene implements AutoCloseable {
        private static final String[] SOURCES={"lbc:item_defs/lbc/corrupted_dust__bcc0d073f9b6","lbc:item_defs/lbc/corrupted_dust_lamashtu__87282b19beaa","lbc:item_defs/lbc/corrupted_dust_belial__608e76e04a9c","lbc:item_defs/lbc/corrupted_dust_ahriman__15d3f7afa1d5"};
        private static final String[] MODELS={"lbc:corrupted_pentagram","lbc:lamashtu","lbc:belial","lbc:ahriman"};
        final GameController game;final WoolBucketChecks.Fixture f;final int started;
        final List<ItemStack> sources=new ArrayList<>();final Set<UUID> initial=new HashSet<>();
        final Map<net.minecraft.stats.Stat<Item>,Integer> used=new LinkedHashMap<>();
        LbcCorruptedGlyphs.Glyph pending;int published,trial,lastInput=Integer.MIN_VALUE;ItemStack retry;boolean closed;
        Scene(GameController game){this.game=game;require(game.items().corruptedGlyphs().count()==0,"exclusive native glyph lifecycle");
            f=new WoolBucketChecks.Fixture(game,false);started=game.server().getTickCount();
            for(var actor:f.level.getAllEntities())initial.add(actor.getUUID());
            for(int i=0;i<5;i++){var stat=net.minecraft.stats.Stats.ITEM_USED.get(LbcTmtItems.prototype(i).getItem());used.put(stat,f.owner.getStats().getValue(stat));}
        }
        void begin(){
            f.prepareParticipants();require(LbcTmtBlockUses.canonical(LbcCorruptedGlyphs.DUST.getDefaultInstance())==null,"bare native Dust default has no canonical use-on source");
            require(!LbcTmtBlockUses.airCandidate(LbcCorruptedGlyphs.DUST.getDefaultInstance()),"bare Dust is not an air-use ticket");
            for(int i=0;i<SOURCES.length;i++){
                var rows=game.lbc().rollTable(f.level,f.home,f.owner,SOURCES[i]);require(rows.size()==1,"one real authored glyph leaf "+SOURCES[i]);var source=rows.getFirst();
                require(source.is(LbcCorruptedGlyphs.DUST)&&LbcCorruptedGlyphs.kind(source)==LbcCorruptedGlyphs.Kind.values()[i],"canonical source identity and exact STRING selector "+SOURCES[i]);
                require(LbcTmtBlockUses.canonical(source)==LbcCorruptedGlyphs.DUST&&!LbcTmtBlockUses.airCandidate(source),"valid source admitted only to use-on");sources.add(source);
            }
        }
        LbcTmtAbilities.Ability property(ItemStack source,String id){
            var data=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putString(LbcTmtAbilities.NATIVE,BuiltInRegistries.ITEM.getKey(source.getItem()).toString());
            LbcTmtComponents.capture(game,source,data);return new LbcTmtAbilities.Ability(id,CustomData.of(data));
        }
        ItemStack composed(ItemStack source,int carrier){
            var selected=property(source,"check/glyph");var harmlessSource=new ItemStack(Items.STICK);var siblingTag=new CompoundTag();siblingTag.putString("unchanged_sibling","private nested source");
            harmlessSource.set(DataComponents.CUSTOM_DATA,CustomData.of(siblingTag));harmlessSource.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x12ab34),List.of(),Optional.empty()));
            var harmless=property(harmlessSource,"check/harmless");var stack=LbcTmtItems.prototype(carrier);stack.setCount(2);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            // A real outer selector must never select the source kind for the private factory.
            root.putString("glyphkind",LbcCorruptedGlyphs.kind(source)==LbcCorruptedGlyphs.Kind.BAFOMET?"AHRIMAN":"BAFOMET");
            root.putString("outer_marker","retained full physical item");LbcTmtAbilities.write(root,List.of(harmless,selected));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            stack.set(DataComponents.CUSTOM_NAME,Component.literal("same procedural glyph transport"));
            stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x654321),List.of(),Optional.empty()));
            var entity=new CompoundTag();entity.putBoolean("NoAI",true);entity.putString("outer_entity_marker","never a native glyph payload");stack.set(DataComponents.ENTITY_DATA,TypedEntityData.of(EntityTypes.PIG,entity));
            var nested=new ItemStack(Items.PAPER,3);nested.set(DataComponents.CUSTOM_NAME,Component.literal("independent outer container"));stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(nested)));
            return stack;
        }
        ItemStack attempt(int sourceIndex,int carrier,InteractionHand hand,int geometry,boolean accepted,ItemStack existing){
            var stack=existing==null?composed(sources.get(sourceIndex),carrier):existing;f.owner.setItemInHand(hand,stack);
            var opposite=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;var other=new ItemStack(Items.STICK,7);f.owner.setItemInHand(opposite,other);
            var clicked=BlockPos.containing(f.home.add(0,0,3));var face=geometry==1?Direction.EAST:Direction.UP;
            f.set(clicked,geometry==2?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState());var destination=geometry==2?clicked:clicked.relative(face);
            f.set(destination,Blocks.AIR.defaultBlockState());var expected=Vec3.atBottomCenterOf(destination);var original=stack.copy();var before=stack.copyWithCount(accepted?stack.getCount()-1:stack.getCount());
            if(accepted){
                require(LbcTmtAbilities.invoke(stack,"check/glyph",()->{
                    require(LbcCorruptedGlyphs.kind(stack)==LbcCorruptedGlyphs.Kind.values()[sourceIndex],"native selected identity and private glyphkind override outer data");
                    require(!stack.has(DataComponents.ENTITY_DATA)&&!stack.has(DataComponents.POTION_CONTENTS)&&!stack.has(DataComponents.CONTAINER),"selected factory borrows only native source payload components");
                    require(!LbcTmtBlockUses.airCandidate(stack)&&!LbcTmtBlockUses.air(game,f.owner,hand),"selected valid glyph cannot acquire an air-use event");
                    require(ItemStack.matches(LbcTmtAbilities.outerCopy(stack),original),"outer copy restores the exact independent original source component set");return true;
                }),"private source scope accepted for parameter verification");
            }
            var hit=new BlockHitResult(Vec3.atCenterOf(clicked).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(.5)),face,clicked,false);
            int count=game.items().corruptedGlyphs().count();var result=f.owner.gameMode.useItemOn(f.owner,f.level,stack,hand,hit);
            require(result.consumesAction()==accepted,"actual registered native use-on admission accepted="+accepted+" carrier="+carrier+" kind="+sourceIndex);
            require(ItemStack.matches(stack,before)&&f.owner.getItemInHand(hand)==stack&&f.owner.getItemInHand(opposite)==other&&other.getCount()==7,"sole accepted hand debit with full outer identity/components/sibling and other hand unchanged");
            require(!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"all private native source/identity scopes leave the input boundary");
            var ops=f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);var encoded=ItemStack.CODEC.encodeStart(ops,stack).getOrThrow();
            require(ItemStack.matches(stack,ItemStack.CODEC.parse(ops,encoded).getOrThrow()),"full selected sibling/outer components survive native item codec");
            require(game.items().corruptedGlyphs().count()==count+(accepted?1:0),"exact native glyph publication count");
            if(accepted){pending=game.items().corruptedGlyphs().snapshot().getLast();published=game.server().getTickCount();f.own(pending.display);
                require(pending.owner==f.owner&&pending.kind==LbcCorruptedGlyphs.Kind.values()[sourceIndex]&&pending.display.position().equals(expected),"actual source kind/owner and clicked-face or empty-cell geometry");
                require(MODELS[sourceIndex].equals(pending.display.getItemStack().get(DataComponents.ITEM_MODEL).toString())&&game.items().currentEpoch(pending.epoch),"authored native glyph display and current epoch");
            }
            return stack;
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();require(now-started<300,"bounded actual scheduled scene");
            if(pending!=null){if(now==published)return false;require(!pending.display.isRemoved()&&pending.ticks>0&&f.level.getEntity(pending.display.getUUID())==pending.display,"existing native glyph owner advances a real scheduler tick");
                f.discard(pending.display);pending=null;require(game.items().corruptedGlyphs().count()==0,"existing native unload owner retires the accepted body");}
            if(lastInput==now)return false;lastInput=now;
            if(trial<20){attempt(trial/5,trial%5,trial%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND,trial%3,true,null);trial++;return false;}
            if(trial==20){f.field(false,f.owner.position().add(6,0,0));require(game.items().magic().suppressed(f.owner)&&game.items().magic().suppressedAt(f.level,f.home.add(0,0,3)),"actual positive MAG field covers factory and target");
                retry=attempt(0,2,InteractionHand.OFF_HAND,0,false,null);f.removeFields();trial++;return false;}
            if(trial==21){require(!game.items().magic().suppressed(f.owner),"native MAG cache clears before retry");attempt(0,2,InteractionHand.OFF_HAND,0,true,retry);trial++;return false;}
            // An invalid private transport cannot borrow the valid outer glyphkind.
            var bare=composed(LbcCorruptedGlyphs.DUST.getDefaultInstance(),2);f.owner.setItemInHand(InteractionHand.MAIN_HAND,bare);var before=bare.copy();var clicked=BlockPos.containing(f.home.add(0,0,3));
            var hit=new BlockHitResult(Vec3.atCenterOf(clicked),Direction.UP,clicked,false);
            require(!f.owner.gameMode.useItemOn(f.owner,f.level,bare,InteractionHand.MAIN_HAND,hit).consumesAction()&&ItemStack.matches(bare,before)&&game.items().corruptedGlyphs().count()==0,"bare private Dust has no selector even when outer custom data contains a valid kind");
            return true;
        }
        public void close(){if(closed)return;closed=true;try{f.removeFields();if(pending!=null)f.discard(pending.display);
            for(var actor:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(actor.getUUID())){f.own(actor);game.items().entityUnloaded(actor);actor.discard();}
            used.forEach((stat,value)->f.owner.getStats().setValue(f.owner,stat,value));
        }finally{f.close();}}
    }
}
