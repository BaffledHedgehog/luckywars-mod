package me.skywars2.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Bound native domains, source selectors and generated item codec round trips. */
public final class TmtCatalogChecks {
    private TmtCatalogChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT catalog: "+why);}
    public static void run(GameController game){
        var runtime=game.items().tmt();var catalog=runtime.catalog();var ids=new HashSet<String>();
        for(var key:List.of("wisp_air","wisp_fire","wisp_light","wisp_nature","wisp_shadow","wisp_water")){
            require(catalog.families().stream().flatMap(f->f.variants().stream()).noneMatch(v->((ItemData.IntReader)(Object)v.data()).skywars2$readInt(key,0)==1),"source-inert wisp flag is never sampled: "+key);
            require(catalog.domains().findings().stream().anyMatch(f->f.disposition()==LbcTmtCatalog.Disposition.EXCLUDED_INERT_KEY&&f.reason().contains(key)),"explicit source-inert reconciliation remains visible: "+key);
        }
        for(var family:catalog.families())require(ids.add(family.id()),"one lottery entry per semantic family");
        for(var wand:LbcWands.tmtAbilities())require(ids.contains("wand/"+wand.family()),"all native common/Tiny/Academy wand families retained");
        require(LbcWands.tmtAbilities()==LbcWands.tmtAbilities(),"immutable finalized native wand domain is reused");
        for(var wand:LbcWands.tmtAbilities()){
            var alias=new ItemStack(net.minecraft.world.item.Items.CARROT_ON_A_STICK);alias.set(DataComponents.CUSTOM_DATA,wand.flags());
            require(LbcTmtUseProfiles.commonWand(alias),"legacy vanilla-carrier wand alias resolves through the native owner classifier");
        }
        require(!LbcTmtUseProfiles.commonWand(new ItemStack(net.minecraft.world.item.Items.CARROT_ON_A_STICK)),"ordinary vanilla carrier is not a wand ability");
        for(var input:runtime.useProfiles().families())require(ids.contains(input.id()),"existing direct-use routing IDs remain valid");
        require(runtime.useProfiles().domains().legacyOwnerGaps().isEmpty(),"three historical gadget gaps resolve only against actual admitted native factories");
        require(catalog.domains().findings().stream().noneMatch(row->row.disposition()==LbcTmtCatalog.Disposition.LEGACY_OWNER_GAP),"unified report agrees with bound owner admission");
        var material=catalog.families().stream().filter(row->row.id().equals("native/material/govno")).findFirst().orElseThrow();
        require(material.variants().size()==1&&material.events().containsAll(Set.of(LbcTmtCatalog.Event.CRAFT,LbcTmtCatalog.Event.DROP,LbcTmtCatalog.Event.INVENTORY,LbcTmtCatalog.Event.EQUIPMENT)),"material aliases are one meaningful family/one variant, not four weighted carriers");
        for(var flag:List.of("crowbar","slimeball","aimbobapple"))require(ids.contains("native/"+flag),"actual criterion/consume property is sampled: "+flag);
        // Ordinary most is admitted through the real wool snowball factory, separately from most_imba.
        var resolvedFlags=List.of("gayradar","steampunk_rocket","trollge","bucks","govnoq","randomgovno","rewardtype","rewardcoin","randomselect","luck_key","crowbar","slimeball","aimbobapple","imbaxe",
                "most","lightsaber","lightsaberrc","wtf_item","infinity_rand","liquidvoid","yellow_lightning","nexus_wire_blue","nexus_wire_white","end_of_everything","baffled_of_everything","shish_of_everything","rasengan","lightning","jack_o_lantern","academy_token","magic_academy","helicopter_boss","crocodildo_boss");
        for(var finding:catalog.domains().findings())if(finding.disposition()==LbcTmtCatalog.Disposition.PENDING_NATIVE_DOMAIN)
            for(var flag:resolvedFlags)require(!finding.reason().matches(".*\\b"+flag+"\\b.*"),"audited source alias/domain no longer falsely pending: "+finding);
        var pills=new HashSet<Integer>();var mystical=new HashSet<String>();var fragments=new HashSet<String>();
        for(var family:catalog.families())for(var variant:family.variants()){
            var data=(ItemData.IntReader)(Object)variant.data();
            if(family.id().equals("native/pill"))pills.add(data.skywars2$readInt("pill",-1));
            for(var key:new String[]{"mystical","mystical_old"}){
                int group=data.skywars2$readInt(key,0);if(group!=0)mystical.add(key+"/"+group+"/"+data.skywars2$readInt("type",0));
            }
            for(var key:new String[]{"fragmentsword","fragmentsword_old"})if(data.skywars2$readInt(key,0)==1)
                fragments.add(key+"/"+data.skywars2$readInt("cooltype",-1));
        }
        require(pills.equals(LbcPills.definitions().stream().map(LbcPills.Definition::id).collect(java.util.stream.Collectors.toSet())),"all actual native pill definitions, including zero and the new tail");
        var expectedMystical=new HashSet<String>();
        for(int group=1;group<=4;group++)for(boolean old:new boolean[]{false,true}){
            int count=group==1?(old?6:8):group==3?3:6;
            for(int type=1;type<=count;type++)expectedMystical.add((old?"mystical_old":"mystical")+"/"+group+"/"+type);
        }
        require(mystical.equals(expectedMystical)&&mystical.size()==44,"all44 native current/old artifact selectors");
        require(fragments.size()==34,"both17-outcome native Fragment versions");
        var ops=game.maps().level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for(int seed=0;seed<32;seed++){
            var first=runtime.generate(RandomSource.create(seed));var second=runtime.generate(RandomSource.create(seed));
            require(ItemStack.matches(first,second),"complete generated item is deterministic for a fixed source seed");
            var slots=LbcTmtAbilities.read(first);require(!slots.isEmpty()&&slots.size()<=7,"seven replacement draws use last-family-wins slots");
            for(var slot:slots)require(ids.contains(slot.id()),"generated property resolves to an audited catalog family");
            var data=first.get(DataComponents.CUSTOM_DATA);
            require(((ItemData.IntReader)(Object)data).skywars2$readInt("type",-1)==-1,"private artifact types never leak into the outer item");
            var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,first).getOrThrow()).getOrThrow();
            require(ItemStack.matches(first,restored),"native item codec preserves all generated properties and procedural seed");
        }
        var shulkerTables=new java.util.HashSet<String>();
        for(var family:catalog.families())for(var property:family.variants()){
            var source=LbcTmtComponents.source(game,property);
            if(LbcTmtBlockUses.authoredShulker(source))shulkerTables.add(source.get(DataComponents.CONTAINER_LOOT).lootTable().identifier().toString());
        }
        require(shulkerTables.containsAll(java.util.Set.of("lbc:infinity","lbc:infinity_upgraded","lbc:infinity_upgraded_luck")),"all authored native shulker payloads survive the actual sampler manifest");
        // Do not require a fabricated pending row: actual owner/domain assertions above own readiness.
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var trinkets=catalog.families().stream().filter(row->row.name().startsWith("trinket/")).limit(2).toList();
            require(trinkets.size()==2,"distinct actual trinket identities exist in the unified catalog");
            var source=LbcTmtItems.prototype(0);var root=source.get(DataComponents.CUSTOM_DATA).copyTag();
            LbcTmtAbilities.write(root,trinkets.stream().map(row->row.variants().getFirst()).toList());
            source.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));f.owner.getInventory().setItem(9,source);
            for(var trinket:trinkets){
                var property=trinket.variants().getFirst();var identity=((ItemData.IntReader)(Object)property.data()).skywars2$readString("trinket","");
                require(!identity.isEmpty()&&LbcReactiveTrinkets.count(f.owner,net.minecraft.world.item.Items.AIR,identity)==1,"each private trinket identity contributes once through the native inventory counter");
            }
        }
        try{
            var file=game.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("debug/tmt-catalog-domains.json");
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(catalog.domains()));
        }catch(java.io.IOException failure){throw new IllegalStateException("Write isolated catalog diagnostic",failure);}
        me.skywars2.SkyWars2.LOGGER.info("[check] TMT unified catalog families={} variants={} findings={}",catalog.domains().families(),catalog.domains().variants(),catalog.domains().findings().size());
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-catalog (native typed pill/artifact/Fragment domains, retained dispatch identities,32 deterministic complete generated item/codec pairs, explicit unresolved owners)");
    }
}
