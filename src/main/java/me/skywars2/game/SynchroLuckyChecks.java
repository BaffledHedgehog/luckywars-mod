package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Actual block loot: more luck advances the same chain; phone filters skip; mask inserts bonuses. */
public final class SynchroLuckyChecks {
    private SynchroLuckyChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Synchro lucky: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final SynchroRanked sync;final SynchroRanked.State old;final WoolBucketChecks.Fixture f;
        Fixture(GameController game){this.game=game;sync=game.synchroRanked();old=sync.checkpoint();f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();game.config().ranked=true;game.lbc().syncRanked();inputs();}
        void inputs(){for(var p:List.of(f.owner,f.other)){p.getAttribute(Attributes.LUCK).setBaseValue(0);p.getInventory().clearContent();game.items().mana().score(p,"hplvl").set(1000);game.items().mana().score(p,"attack_damage").set(1);}}
        void begin(long seed){inputs();sync.begin(true,seed);}
        List<ItemStack> drop(ServerPlayer player,LbcLuckyBlocks.Kind kind,ItemStack tool){var state=LbcLuckyBlocks.block(kind).defaultBlockState();return state.getDrops(new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,player.position()).withParameter(LootContextParams.THIS_ENTITY,player).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.TOOL,tool));}
        long cursor(ServerPlayer p,LbcLuckyBlocks.Kind kind){return sync.position(p.getUUID(),Identifier.parse("lbc:native_lucky/"+kind.name().toLowerCase(Locale.ROOT)));}
        void same(List<ItemStack> left,List<ItemStack> right,String why){require(left.size()==right.size(),why+" stack counts "+left.size()+"/"+right.size());for(int i=0;i<left.size();i++)require(ItemStack.matches(left.get(i),right.get(i)),why+" output"+i+" "+left.get(i)+" / "+right.get(i));}
        void luck(){
            for(var kind:LbcLuckyBlocks.Kind.values()){
                begin(9020+kind.ordinal());var slow=new ArrayList<ItemStack>();var fast=new ArrayList<ItemStack>();var a=new ArrayList<SynchroLuckyDraws.Draw>();var b=new ArrayList<SynchroLuckyDraws.Draw>();
                try(var observer=SynchroLuckyDraws.observe(a::add)){for(int i=0;i<48;i++)slow.addAll(drop(f.owner,kind,ItemStack.EMPTY));}
                f.other.getAttribute(Attributes.LUCK).setBaseValue(3);
                try(var observer=SynchroLuckyDraws.observe(b::add)){for(int i=0;i<12;i++)fast.addAll(drop(f.other,kind,ItemStack.EMPTY));}
                require(cursor(f.owner,kind)==48&&cursor(f.other,kind)==48,"luck0x48 and luck3x12 consume exactly the same48 candidates "+kind);
                require(a.equals(b)&&a.size()==48,"identical actual weighted sequence irrespective of luck roll grouping "+kind);same(slow,fast,"complete native results of grouped luck draws "+kind);
            }
        }
        void phone(){
            for(var kind:List.of(LbcLuckyBlocks.Kind.LOLI,LbcLuckyBlocks.Kind.NEKO)){
                begin(4080+kind.ordinal());f.other.getInventory().setItem(9,f.leaf("admin_zaika"));
                var phone=new ArrayList<SynchroLuckyDraws.Draw>();var ordinary=new ArrayList<SynchroLuckyDraws.Draw>();var actual=new ArrayList<List<ItemStack>>();
                try(var observer=SynchroLuckyDraws.observe(phone::add)){for(int i=0;i<36;i++)actual.add(drop(f.other,kind,ItemStack.EMPTY));}
                long used=cursor(f.other,kind);require(used>36&&used<512,"phone skips real unavailable candidates "+kind+" used="+used);
                try(var observer=SynchroLuckyDraws.observe(ordinary::add)){for(int i=0;i<used;i++)drop(f.owner,kind,ItemStack.EMPTY);}
                require(phone.size()==ordinary.size(),"every rejected candidate advances same base chain "+kind);
                for(int i=0;i<phone.size();i++){var p=phone.get(i);var o=ordinary.get(i);require(p.index()==o.index()&&p.outcome().equals(o.outcome())&&!o.skipped(),"same canonical candidate #"+i+" "+kind);}
                require(phone.stream().filter(p->!p.skipped()).count()==36,"one accepted candidate for each phone opening "+kind);
                if(kind==LbcLuckyBlocks.Kind.NEKO){
                    var families=Set.of("steampunk","techno","paladin","mage","phantom");
                    for(var p:phone)if(!p.skipped())require(families.stream().anyMatch(family->p.outcome().equals("lbc:neko_set_"+family+"_pre")),"accepted phone family belongs to original five-set pool");
                    var accepted=phone.stream().filter(row->!row.skipped()).toList();
                    for(int i=0;i<actual.size();i++){
                        var output=actual.get(i);boolean techno=accepted.get(i).outcome().equals("lbc:neko_set_techno_pre");
                        require(output.size()==(techno?5:4),"phone retains complete native armor reward including techno stand "+output);
                        require(output.stream().filter(s->s.is(Items.ARMOR_STAND)).count()==(techno?1:0),"techno's original extra stand is preserved exactly");
                        var slots=new HashSet<EquipmentSlot>();for(var item:output){var gear=item.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);if(gear!=null)slots.add(gear.slot());}
                        require(slots.equals(Set.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)),"every accepted phone reward contains all four actual armor slots");
                    }
                }
            }
        }
        void mask(){
            begin(523112);f.other.setItemSlot(EquipmentSlot.HEAD,f.leaf("mellstroy_mask"));game.items().mellstroy().inventoryChanged(f.other);
            var masked=new ArrayList<SynchroLuckyDraws.Draw>();var plain=new ArrayList<SynchroLuckyDraws.Draw>();
            try(var observer=SynchroLuckyDraws.observe(masked::add)){for(int i=0;i<96;i++)drop(f.other,LbcLuckyBlocks.Kind.NORMAL,ItemStack.EMPTY);}
            long used=cursor(f.other,LbcLuckyBlocks.Kind.NORMAL);require(used>0&&used<96,"real mask has both successful bonus insertions and failed proc/base draws");
            try(var observer=SynchroLuckyDraws.observe(plain::add)){for(int i=0;i<used;i++)drop(f.owner,LbcLuckyBlocks.Kind.NORMAL,ItemStack.EMPTY);}
            var base=masked.stream().filter(row->!row.bonus()).toList();require(base.equals(plain),"mask bonuses never reroll or shift the next base outcome");
            long next=0;for(var draw:masked){require(draw.index()==next,"bonus leaves base cursor unchanged");if(!draw.bonus())next++;else require(draw.outcome().equals("lbc:lucky_items_high_quality"),"original high-quality bonus pool");}require(next==used,"only base outcomes debit the base chain");
        }
        void fortune(){
            begin(779991);var plain=new ItemStack(Items.DIAMOND_PICKAXE);var enchanted=plain.copy();enchanted.enchant(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),3);boolean more=false;
            var state=Blocks.DIAMOND_ORE.defaultBlockState();for(int i=0;i<24;i++){
                var a=state.getDrops(new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.owner.position()).withParameter(LootContextParams.THIS_ENTITY,f.owner).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.TOOL,plain));
                var b=state.getDrops(new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.other.position()).withParameter(LootContextParams.THIS_ENTITY,f.other).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.TOOL,enchanted));
                require(a.stream().allMatch(s->s.is(Items.DIAMOND))&&b.stream().allMatch(s->s.is(Items.DIAMOND)),"fortune preserves the selected native item");more|=b.stream().mapToInt(ItemStack::getCount).sum()>a.stream().mapToInt(ItemStack::getCount).sum();
            }
            var key=Identifier.parse("minecraft:blocks/diamond_ore");require(more&&sync.position(f.owner.getUUID(),key)==24&&sync.position(f.other.getUUID(),key)==24,"native Fortune changes quantity without extra chain debits");
        }
        @Override public void close(){try{f.close();}finally{sync.restore(old);game.lbc().syncRanked();}}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.luck();f.phone();f.mask();f.fortune();}SkyWars2.LOGGER.info("[check] PASS synchro-lucky (real native blocks: grouped luck1/4 draws, phone Loli/Neko rejected-candidate skips/full sets, Mellstroy bonus insertion with unchanged base stream, native Fortune quantity only)");}
}
