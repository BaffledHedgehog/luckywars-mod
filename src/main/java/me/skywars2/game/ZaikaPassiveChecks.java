package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.scores.ScoreHolder;

/** Deterministic admission checks against loaded native tables, with actual BLOCK outputs. */
public final class ZaikaPassiveChecks {
    private ZaikaPassiveChecks() { }
    private static final int[] EXCLUDED={2,5,6,9,10,12,19,20,22,27,29,32};
    private static final String[] FAMILIES={"steampunk","techno","paladin","mage","phantom"};
    private static final String[] PARTS={"helmet","chestplate","leggings","boots"};
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Zaika passive: "+why);}
    private static Object member(Object target,String name){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(target);
        }catch(NoSuchFieldException ignored){}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        throw new IllegalStateException("missing loaded native field "+name);
    }
    private static java.lang.reflect.Field field(Object target,String name){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{var out=type.getDeclaredField(name);out.setAccessible(true);return out;}catch(NoSuchFieldException ignored){}
        throw new IllegalStateException("missing native field "+name);
    }
    private static final class Score implements AutoCloseable {
        final net.minecraft.world.scores.Scoreboard board;final ScoreHolder holder;
        final net.minecraft.world.scores.Objective objective;final Integer old;
        Score(GameController game,String holder,String objective){
            board=game.server().getScoreboard();this.holder=ScoreHolder.forNameOnly(holder);
            this.objective=Objects.requireNonNull(board.getObjective(objective),objective);
            var info=board.getPlayerScoreInfo(this.holder,this.objective);old=info==null?null:info.value();
        }
        void set(int value){board.getOrCreatePlayerScore(holder,objective,true).set(value);}
        public void close(){if(old==null)board.resetSinglePlayerScore(holder,objective);else set(old);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture world;
        final ServerPlayer p,other;final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final List<Score> scores=new ArrayList<>();final Score modifier,ranked,mode;
        final java.lang.reflect.Field randomField;final Object originalRandom;
        final List<List<ItemStack>> fullSets=new ArrayList<>();ItemStack zaika;Marker marker;
        Fixture(GameController game){
            this.game=game;
            p=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            saved=new RegenerationBeltChecks.Snapshot(game,p);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            world=new AllGrimoireChecks.Fixture(game);world.reset();
            randomField=field(world.level,"random");originalRandom=member(world.level,"random");
            try{randomField.set(world.level,net.minecraft.util.RandomSource.create(0x7a61696b61L));}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            modifier=score("final_luck_modifier","lbc.math");ranked=score("ranked","lbc.event");mode=score("#gamemode","swrg.math");
            modifier.set(1);ranked.set(0);mode.set(1);
            p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK).setBaseValue(0);
        }
        Score score(String name,String objective){var out=new Score(game,name,objective);scores.add(out);return out;}
        LootTable table(String id){return game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));}
        @SuppressWarnings("unchecked") List<LootPool> pools(String id){return (List<LootPool>)member(table(id),"pools");}
        LootParams params(LbcLuckyBlocks.Kind kind){return new LootParams.Builder(world.level)
                .withParameter(LootContextParams.ORIGIN,p.position()).withParameter(LootContextParams.THIS_ENTITY,p)
                .withParameter(LootContextParams.BLOCK_STATE,LbcLuckyBlocks.legacyState(kind))
                .withParameter(LootContextParams.TOOL,ItemStack.EMPTY).create(LootContextParamSets.BLOCK);}
        LootContext context(LbcLuckyBlocks.Kind kind){return new LootContext.Builder(params(kind)).withOptionalRandomSeed(17).create(Optional.empty());}
        List<LootPoolEntry> expand(String id,int pool,int index,LootContext context){
            var out=new ArrayList<LootPoolEntry>();pools(id).get(pool).entries.get(index).expand(context,out::add);return out;
        }
        List<ItemStack> roll(String id){return table(id).getRandomItems(params(LbcLuckyBlocks.Kind.NEKO),17);}
        List<ItemStack> nativeNeko(){
            var state=LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NEKO).defaultBlockState();
            return state.getDrops(new LootParams.Builder(world.level).withParameter(LootContextParams.ORIGIN,p.position()).withParameter(LootContextParams.THIS_ENTITY,p).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.TOOL,ItemStack.EMPTY));
        }
        ItemStack one(String id){var out=roll(id);require(out.size()==1&&out.getFirst().getCount()==1,"canonical singleton "+id);return out.getFirst();}
        void clear(){p.getInventory().clearContent();for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);}
        void carry(int place){clear();switch(place){
            case 0->{} case 1->p.getInventory().setItem(9,zaika.copy());
            case 2->p.setItemSlot(EquipmentSlot.MAINHAND,zaika.copy());
            case 3->p.setItemSlot(EquipmentSlot.OFFHAND,zaika.copy());
            case 4->p.setItemSlot(EquipmentSlot.HEAD,zaika.copy());
            case 5->p.setItemSlot(EquipmentSlot.CHEST,zaika.copy());
            case 6->p.setItemSlot(EquipmentSlot.LEGS,zaika.copy());
            case 7->p.setItemSlot(EquipmentSlot.FEET,zaika.copy());
            default->throw new IllegalArgumentException();
        }game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}
        boolean contains(int[] indices,int index){for(int n:indices)if(n==index)return true;return false;}
        void presentation(){
            zaika=one("ported/admin_zaika");require(ItemStack.matches(zaika,one("item_defs/lbc/zaika__e7f523430e71")),"ready references exact canonical item");
            require(zaika.is(Items.CARROT_ON_A_STICK),"source carrier");
            require(Identifier.parse("lbc:zaika").equals(zaika.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_s").equals(zaika.get(DataComponents.TOOLTIP_STYLE)),"source model/tier");
            require(LbcFartus.data(zaika).getIntOr("zaika",0)==1&&LbcFartus.data(zaika).getIntOr("magicwand",0)==1&&LbcFartus.data(zaika).size()==2,"only original flags, no invented activation flags");
            require(Objects.equals(zaika.get(DataComponents.CUSTOM_NAME),text("[{\"translate\":\"admin_zaika\",\"italic\":false}]")),"exact untranslated name components/style");
            var lore=zaika.get(DataComponents.LORE);require(lore!=null&&lore.lines().size()==4,"four original lore lines");
            for(int n=0;n<3;n++)require(Objects.equals(lore.lines().get(n),text("{\"translate\":\"admin_zaika.lore."+(n+1)+"\",\"color\":\"white\",\"italic\":false}")),"translated lore "+n);
            require(Objects.equals(lore.lines().get(3),text("[{\"text\":\"JustSh1sh_\",\"color\":\"green\",\"italic\":false},{\"text\":\" Happy Birthday 11.12.2023!\",\"color\":\"dark_gray\",\"italic\":false}]")),"exact nested birthday components/inherited styles");
        }
        net.minecraft.network.chat.Component text(String json){return ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(json)).getOrThrow();}
        void sets(){
            var selector=pools("nekolucky_zaika_items");require(selector.size()==1&&selector.getFirst().entries.size()==5,"five source full-set choices");
            var sourceTables=new HashSet<String>();sourceTables.add("nekolucky_zaika_items");
            for(int family=0;family<5;family++){
                String id="neko_set_"+FAMILIES[family];sourceTables.add(id+"_full");
                var ctx=context(LbcLuckyBlocks.Kind.NEKO);var chosen=expand("nekolucky_zaika_items",0,family,ctx);
                require(chosen.size()==1&&chosen.getFirst().getWeight(0)==10,"equal source selector weight10 "+id);
                var actual=new ArrayList<ItemStack>();chosen.getFirst().createItemStack(actual::add,ctx);
                var full=roll(id+"_full");require(same(actual,full),"selector returns exact full-set components "+id);
                var fullPools=pools(id+"_full");require(fullPools.size()==(family==1?5:4),"source basics retained "+id);
                for(int part=0;part<4;part++){
                    String leaf=id+"/"+PARTS[part];sourceTables.add(leaf);var armor=one(leaf);
                    require(full.stream().filter(s->ItemStack.matches(s,armor)).count()==1,"exact corresponding armor leaf "+leaf);
                    require(fullPools.get(part).entries.size()==1,"one original full-set piece branch");
                }
                require(full.size()==(family==1?5:4),"four armor pieces plus original Techno basic");
                if(family==1)require(full.stream().filter(s->s.is(Items.ARMOR_STAND)).count()==1,"original old_exoskeleton basic retained");
                fullSets.add(full);
            }
            require(sourceTables.size()==26,"complete 26 named source-table closure");
        }
        boolean same(List<ItemStack> a,List<ItemStack> b){
            if(a.size()!=b.size())return false;var remaining=new ArrayList<>(b);
            for(var stack:a){int hit=-1;for(int n=0;n<remaining.size();n++)if(ItemStack.matches(stack,remaining.get(n))){hit=n;break;}if(hit<0)return false;remaining.remove(hit);}return remaining.isEmpty();
        }
        void loli(){
            require(pools("loliluckyblock").getFirst().entries.size()==33,"original Loli entry count");
            for(int finalLuck:new int[]{1,7})for(int rankedValue:new int[]{0,1})for(int modeValue:new int[]{1,3,4,5}){
                modifier.set(finalLuck);ranked.set(rankedValue);mode.set(modeValue);carry(0);
                var baseline=context(LbcLuckyBlocks.Kind.LOLI);var before=new boolean[33];
                for(int index=0;index<33;index++)before[index]=!expand("loliluckyblock",0,index,baseline).isEmpty();
                for(int index:EXCLUDED){
                    boolean expected=contains(new int[]{2,5,6,9,10,12},index)?finalLuck!=7:
                            contains(new int[]{19,27},index)?rankedValue!=1:index==20?rankedValue==1:
                            contains(new int[]{29,32},index)?modeValue<3||modeValue>4:true;
                    require(before[index]==expected,"independent source score eligibility index="+index+" final="+finalLuck+" ranked="+rankedValue+" mode="+modeValue);
                    var entry=expand("loliluckyblock",0,index,baseline);
                    if(expected)require(entry.size()==1&&entry.getFirst().getWeight(0)==(index==20?1:10),"exact source excluded-entry weight "+index);
                }
                for(int place=1;place<=7;place++){
                    carry(place);var ctx=context(LbcLuckyBlocks.Kind.LOLI);
                    for(int index=0;index<33;index++)require(!expand("loliluckyblock",0,index,ctx).isEmpty()==(!contains(EXCLUDED,index)&&before[index]),"exact twelve exclusions, no global trap filter index="+index+" place="+place);
                    var own=expand("loliluckyblock",0,23,ctx);var output=new ArrayList<ItemStack>();own.getFirst().createItemStack(output::add,ctx);
                    require(output.size()==1&&ItemStack.matches(output.getFirst(),zaika),"own source Loli entry remains obtainable while carried");
                }
            }
            modifier.set(1);ranked.set(0);mode.set(1);
        }
        void neko(){
            for(int place=0;place<=7;place++){
                carry(place);var ctx=context(LbcLuckyBlocks.Kind.NEKO);
                require(!expand("nekoluckyblock",0,0,ctx).isEmpty()==(place==0)&&!expand("nekoluckyblock",0,1,ctx).isEmpty()==(place>0),"exact native Neko route place="+place);
                if(place>0){
                    var outputs=nativeNeko();
                    require(fullSets.stream().anyMatch(set->same(outputs,set)),"actual native lucky-block BLOCK output is one exact full set place="+place);
                }
                // Source checks the custom-data marker; no canonical key producer is bundled.
                var key=new ItemStack(Items.TRIPWIRE_HOOK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("luck_key",1);
                key.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
                p.setItemSlot(EquipmentSlot.MAINHAND,key);
                ctx=context(LbcLuckyBlocks.Kind.NEKO);
                require(expand("nekoluckyblock",0,0,ctx).isEmpty()&&expand("nekoluckyblock",0,1,ctx).isEmpty(),"mainhand luck_key priority place="+place);
                require(nativeNeko().isEmpty(),"actual native BLOCK luck_key output empty place="+place);
            }
        }
        void passiveInput(){
            for(var hand:net.minecraft.world.InteractionHand.values()){
                clear();p.setItemInHand(hand,zaika.copy());int mana=game.items().mana().get(p),payloads=world.base.count();
                var input=p.getItemInHand(hand);
                p.gameMode.useItem(p,world.level,input,hand);
                require(ItemStack.matches(input,zaika)&&ItemStack.matches(p.getItemInHand(hand),zaika)&&game.items().mana().get(p)==mana&&world.base.count()==payloads,"actual UseItem retains passive token/no mana or native spell payload "+hand);
            }
        }
        void unjam(){if(marker!=null){game.items().magic().unloaded(marker);marker.discard();marker=null;}}
        void suppression(){
            for(String tag:List.of("stopper_magic","stopper")){
                marker=new Marker(EntityTypes.MARKER,world.level);marker.setPos(p.position());marker.entityTags().add(tag);
                require(world.level.addFreshEntity(marker),"native jammer marker");game.items().magic().loaded(marker);
                try{require(tag.equals("stopper_magic")?game.items().magic().suppressed(p):game.items().magic().mechanismSuppressed(p),"actual jammer positive control "+tag);carry(1);nekoOwnOutput();}
                finally{unjam();} // Synchronous: legacy TECH marker cannot reach conversion tick.
            }
            other.teleportTo(world.level,p.getX()+2,p.getY(),p.getZ(),Set.of(),0,0,true);
            other.getInventory().clearContent();other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));
            game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();
            try{require(game.items().magic().artifactSuppressedAt(world.level,p.position()),"strong artifact positive control");carry(1);nekoOwnOutput();}
            finally{other.getInventory().clearContent();game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();}
        }
        void nekoOwnOutput(){
            var outputs=nativeNeko();require(fullSets.stream().anyMatch(set->same(outputs,set)),"own native BLOCK source pool remains unjammable");
            var ctx=context(LbcLuckyBlocks.Kind.LOLI);for(int index:EXCLUDED)require(expand("loliluckyblock",0,index,ctx).isEmpty(),"own Loli negative gate remains unjammable "+index);
        }
        public void close(){
            unjam();Throwable failure=null;
            for(Runnable action:List.<Runnable>of(world::close,()->{for(var score:scores)score.close();},()->saved.restore(game),saved::restoreAttributes,saved::restoreUse,()->otherSaved.restore(game),otherSaved::restoreAttributes,otherSaved::restoreUse,()->{game.items().magic().inventoryChanged(p);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();game.players().save();},()->{try{randomField.set(world.level,originalRandom);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Zaika scoped restore",failure);
        }
    }
    public static void run(GameController game){
        try(var f=new Fixture(game)){f.presentation();f.sets();f.loli();f.neko();f.passiveInput();f.suppression();}
        SkyWars2.LOGGER.info("[check] PASS zaika-passive (canonical birthday components, all12 exact Loli gates/weights and independent score controls, all carried slots, five equal exact Neko full sets/source basics/26 named tables, native BLOCK outputs/luck_key priority, positive MAG/TECH/artifact; passive own-loot scope)");
    }
}
