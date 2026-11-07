package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * One finite sampling domain for native item properties. Construct after native
 * defaults finalize and discard on reload. Event metadata describes the native
 * contract; it does not certify that an event adapter has been implemented.
 * Unknown inputs and native component requirements are deliberately reported.
 */
final class LbcTmtCatalog {
    enum Event { USE, USING, HELD, SELF, MELEE, INCOMING, JUMP, INVENTORY, EQUIPMENT, SHOT, AMMO, CONSUME, BLOCK, MINING, DEATH, DROP, WAND_CAST, SUMMON, CRAFT, KILL }
    enum Location { USED_HAND, MAIN_HAND, INVENTORY, EQUIPPED, AMMUNITION, LOOSE_ITEM, CRAFT_GRID }
    enum Disposition { EXCLUDED_SERVICE, EXCLUDED_INERT_KEY, PENDING_NATIVE_DOMAIN, PENDING_COMPONENT_ADAPTER, MISSING_NATIVE_ITEM, LEGACY_OWNER_GAP }

    record Family(String id,String name,List<LbcTmtAbilities.Ability> variants,Set<Event> events,
                  Set<Location> locations,List<String> owners) {
        Family {
            variants=List.copyOf(variants);events=Set.copyOf(events);locations=Set.copyOf(locations);owners=List.copyOf(owners);
            if(variants.isEmpty())throw new IllegalArgumentException("Empty TMT family "+name);
            for(var variant:variants)if(!variant.id().equals(id))throw new IllegalArgumentException("Mixed TMT family IDs");
        }
    }
    record Finding(String item,String source,Disposition disposition,String reason) {}
    record Domains(int registeredItems,int canonicalPrototypes,int families,int variants,List<Finding> findings) {
        Domains { findings=List.copyOf(findings); }
        boolean hasPending(){return findings.stream().anyMatch(row->row.disposition()!=Disposition.EXCLUDED_SERVICE&&row.disposition()!=Disposition.EXCLUDED_INERT_KEY);}
    }
    private record Rule(String family,String owner,Set<Event> events,Set<Location> locations) {}
    private record Trinket(String identity,Event event) {}
    private static final class Collected {
        final String id,name;
        final Map<String,LbcTmtAbilities.Ability> variants=new TreeMap<>();
        final Set<Event> events=EnumSet.noneOf(Event.class);
        final Set<Location> locations=EnumSet.noneOf(Location.class);
        final Set<String> owners=new TreeSet<>();
        Collected(String id,String name){this.id=id;this.name=name;}
        Family freeze(){return new Family(id,name,List.copyOf(variants.values()),events,locations,List.copyOf(owners));}
    }

    // These markers are payload prerequisites/parameters, never separate draws.
    private static final Set<String> SERVICE=Set.of("accessory","arcanum","arcanumt","arcanum2t","arcanum5t",
            "armor","axe","bow","boots","chestplate","crossbow","craftusable","gadget","helmet","hoe",
            "instantitem","leggings","libspell","magicwand","pickaxe","projegadget","raycastspell","shield",
            "shooter","shovel","special","spell","spellitem","spellwithcooldown","spellwithcooldown_n",
            "summoner","summonerwand","sword","swordwithability","tool","transformable","trident","staff",
            "upgradable","upgrade_available","mana","mode","type","upgraded","spellid","spellcount",
            "cooltype","phd","multiply","drop","imba","tmt","tmt_item","tmt_texture_seed","tier","rarity",
            "unresolved_artifact","unresolved_fragment","unresolved_grimoire","cons_material","bolt_material");
    private static final Map<String,Rule> RULES=rules();
    private static final Map<Item,Trinket> TRINKETS=trinkets();
    private final List<Family> families;
    private final Domains domains;

    LbcTmtCatalog(GameController game,LbcTmtUseProfiles useProfiles){
        java.util.Objects.requireNonNull(useProfiles);
        var all=new TreeMap<String,Collected>();
        var knownInputs=new java.util.HashSet<String>();
        var findings=new LinkedHashMap<String,Finding>();

        // Retain IDs admitted by the existing real native direct-use dispatcher.
        for(var family:useProfiles.families())for(var ability:family.variants()){
            var originalAbility=ability;
            boolean dropped=LbcSpecialDrops.nativeRecognized(LbcTmtComponents.source(game,ability));
            if(dropped){var data=ability.data().copyTag();data.putInt(LbcTmtDrops.FLAG,1);ability=new LbcTmtAbilities.Ability(ability.id(),CustomData.of(data));}
            add(all,family.id(),family.id(),ability,rule("","LbcTmtUseProfiles/native input",Event.USE,Location.USED_HAND));
            if(dropped){all.get(family.id()).events.add(Event.DROP);all.get(family.id()).locations.add(Location.LOOSE_ITEM);all.get(family.id()).owners.add("LbcSpecialDrops");}
            var traits=ability.data().copyTag();
            for(var entry:RULES.entrySet())if(traits.getIntOr(entry.getKey(),0)==1){
                var collected=all.get(family.id());collected.events.addAll(entry.getValue().events());
                collected.locations.addAll(entry.getValue().locations());collected.owners.add(entry.getValue().owner());
            }
            if(traits.getStringOr(LbcTmtAbilities.NATIVE,"").equals("skywars2:imba_axe")){
                all.get(family.id()).events.add(Event.BLOCK);all.get(family.id()).events.add(Event.MELEE);
                all.get(family.id()).owners.add("LbcImbaAxe/LbcTmtTools");
            }
            if(LbcTmtBlockUses.supports(LbcTmtComponents.nativeItem(ability.data()))){all.get(family.id()).events.add(Event.BLOCK);all.get(family.id()).owners.add("LbcTmtBlockUses/native factory");}
            if(LbcTmtShield.candidate(LbcTmtComponents.source(game,ability))){
                all.get(family.id()).events.add(Event.USING);all.get(family.id()).events.add(Event.INCOMING);
                all.get(family.id()).owners.add("Native BlocksAttacks/LbcTmtShield");
                if(LbcElectro.isShield(LbcTmtComponents.source(game,ability)))all.get(family.id()).owners.add("LbcElectro.tickShields");
            }
            knownInputs.add(canonical(originalAbility.data().copyTag()));
        }
        for(var profile:LbcWands.tmtAbilities()){
            var data=profile.flags().copyTag();data.putInt("mode",1);data.putString(LbcTmtAbilities.NATIVE,profile.nativeItem().toString());
            var ability=new LbcTmtAbilities.Ability("wand/"+profile.family(),CustomData.of(data));
            add(all,ability.id(),"wand/"+profile.family(),ability,rule("","LbcWands",Event.USE,Location.USED_HAND));
        }

        int registered=0,canonical=0;
        for(var id:BuiltInRegistries.ITEM.keySet().stream().filter(id->id.getNamespace().equals("skywars2")).sorted().toList()){
            registered++;
            // A creative default may replace registered payloads (PotionItem substitutes water).
            collect(game,new ItemStack(BuiltInRegistries.ITEM.getValue(id)),"native default",all,knownInputs,findings);
        }
        try(var input=LbcTmtCatalog.class.getResourceAsStream("/skywars2/tmt_use_profiles.json")){
            if(input==null)throw new IllegalStateException("Missing canonical TMT prototypes");
            var source=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            if(source.get("schema").getAsInt()!=1)throw new IllegalStateException("Unknown TMT prototype schema");
            for(var value:source.getAsJsonArray("prototypes")){
                canonical++;var row=value.getAsJsonObject();var id=Identifier.parse(row.get("item").getAsString());
                String origin=row.getAsJsonArray("sources").toString();
                if(!Set.of("skywars2","minecraft").contains(id.getNamespace())||!BuiltInRegistries.ITEM.containsKey(id)){
                    finding(findings,id.toString(),origin,Disposition.MISSING_NATIVE_ITEM,"Canonical item is absent from the bound native registry");continue;
                }
                var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                if(row.has("components")){
                    var parsed=net.minecraft.core.component.DataComponentPatch.CODEC.parse(
                            game.server().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),row.get("components"));
                    if(parsed.error().isPresent()){
                        finding(findings,id.toString(),origin,Disposition.PENDING_COMPONENT_ADAPTER,
                                "Literal native payload component could not be decoded: "+parsed.error().get().message());continue;
                    }
                    stack.applyComponents(parsed.getOrThrow());
                }
                var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                for(var raw:row.getAsJsonArray("data"))if(!raw.isJsonNull())
                    data.merge(TagParser.parseCompoundFully(raw.isJsonPrimitive()?raw.getAsString():raw.toString()));
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                collect(game,stack,origin,all,knownInputs,findings);
            }
            for(var value:source.getAsJsonArray("knownLegacyOwnerGaps")){
                var row=value.getAsJsonObject();finding(findings,row.get("key").getAsString(),"legacy reconciliation",
                        Disposition.LEGACY_OWNER_GAP,row.get("reason").getAsString());
            }
        }catch(Exception failure){throw new IllegalStateException("Invalid unified TMT prototypes",failure);}

        // All real pill outcomes, including typed zero and the native tail after129.
        // Definition IDs (not the acquisition OUTPUT_COUNT) are the native consumer domain.
        for(var definition:LbcPills.definitions()){
            var stack=new ItemStack(LbcPills.PILL);var data=new CompoundTag();data.putInt("pill",definition.id());
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            collect(game,stack,"LbcPills.definitions",all,knownInputs,findings);
        }
        // The native bit consumers explicitly support these complete parameter ranges.
        // Current passive types1..8; legacy passive1..6; both reactive1..6,
        // jump1..3 and incoming1..6. Legacy passive admission stays in LbcBalance.
        for(int group=1;group<=4;group++)for(boolean old:List.of(false,true)){
            int count=switch(group){case 1->old?6:8;case 3->3;default->6;};
            for(int type=1;type<=count;type++){
                var stack=new ItemStack(LbcMagic.MYSTICAL_ARTIFACT);
                var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                data.remove("mystical");data.remove("mystical_old");
                data.putInt(old?"mystical_old":"mystical",group);data.putInt("type",type);data.putInt("accessory",1);
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                collect(game,stack,"native mystical bit consumers",all,knownInputs,findings);
            }
        }
        // Both Fragment versions index the native17-entry branch table by cooltype.
        for(boolean old:List.of(false,true))for(int type=0;type<=16;type++){
            var stack=new ItemStack(LbcMeleeItems.Kind.FRAGMENT_SWORD.item);
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            data.remove("fragmentsword");data.remove("fragmentsword_old");
            data.putInt(old?"fragmentsword_old":"fragmentsword",1);data.putInt("cooltype",type);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            collect(game,stack,"LbcPendingSwords branch domain",all,knownInputs,findings);
        }
        families=all.values().stream().map(Collected::freeze).toList();
        domains=new Domains(registered,canonical,families.size(),families.stream().mapToInt(family->family.variants().size()).sum(),List.copyOf(findings.values()));
        if(families.isEmpty())throw new IllegalStateException("Empty unified TMT catalog");
    }

    List<Family> families(){return families;}
    Domains domains(){return domains;}
    /** Each semantic family has one lottery entry; a second uniform draw chooses its private variant. */
    LbcTmtAbilities.Ability draw(RandomSource random){
        var family=families.get(random.nextInt(families.size()));
        return family.variants().get(random.nextInt(family.variants().size()));
    }
    /** Seven independent draws with replacement; the generator's write applies last-family-wins. */
    List<LbcTmtAbilities.Ability> drawSeven(RandomSource random){
        var result=new ArrayList<LbcTmtAbilities.Ability>(7);
        for(int i=0;i<7;i++)result.add(draw(random));
        return List.copyOf(result);
    }

    private static void collect(GameController game,ItemStack stack,String source,Map<String,Collected> all,Set<String> knownInputs,
                                Map<String,Finding> findings){
        if(stack.isEmpty())return;
        var nativeId=BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean nativeTool=false;
        for(var tool:LbcNativeToolItems.Kind.values())if(stack.is(tool.item)){
            nativeTool=true;finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,
                    "Native tool "+tool.id+": ordinary "+BuiltInRegistries.ITEM.getKey(tool.carrier)+" mining/strip/path/till/ignite carrier behavior is not an independent custom ability; stats/enchantments belong to the property generator, while admitted custom melee/use/mining traits remain separate");break;
        }
        boolean nativeAmmo=LbcImbaNativeItems.specialArrows().contains(stack.getItem())
                ||LbcImbaNativeItems.quiverArrows().contains(stack.getItem())
                ||stack.is(LbcImbaNativeItems.INFINITE_ARROW)||stack.is(LbcImbaNativeItems.HOMING_ARROW)||LbcStand.isArrow(stack);
        if(nativeAmmo){
            if(LbcTmtAmmoPayloads.supports(stack)){
                var canonical=LbcStand.isArrow(stack)?LbcStand.ARROW.getDefaultInstance():stack;
                var ammoId=BuiltInRegistries.ITEM.getKey(canonical.getItem());String family="ammo/"+ammoId.getPath(),id="native/"+family;
                var ammoData=canonical.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();LbcTmtComponents.capture(game,canonical,ammoData);
                add(all,id,family,ability(id,ammoId,ammoData),
                        new Rule(family,"LbcTmtAmmoPayloads/LbcQuiver/LbcBeamAmmo",Set.of(Event.SHOT,Event.AMMO),Set.of(Location.USED_HAND)));
            }else finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,
                    "Infinite-arrow economy belongs only to the actual selected ammunition; a weapon property must not grant free shots");
            return;
        }
        if(LbcTmtUseProfiles.commonWand(stack))return;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        LbcTmtNativeIdentities.normalize(stack,data);
        // These native consume owners accept identity as well as legacy flags.
        if(stack.is(LbcKfcFood.KFC_CHICK))data.putInt("kfc_chick",1);
        if(stack.is(LbcKfcFood.GOVNO_EATEN))data.putInt("eat_shit",1);
        // Several registered trinkets intentionally have no default custom data;
        // their real native identity and authored STRING identity are equivalent.
        var nativeTrinket=TRINKETS.get(stack.getItem());
        if(nativeTrinket!=null&&!data.contains("trinket"))data.putString("trinket",nativeTrinket.identity());
        var blockItem=LbcTmtBlockUses.canonical(stack);
        if(blockItem!=null)nativeId=BuiltInRegistries.ITEM.getKey(blockItem);
        LbcTmtComponents.capture(game,stack,blockItem==null?stack.getItem():blockItem,data);
        var probe=ability("probe",nativeId,data);
        if(knownInputs.contains(canonical(probe.data().copyTag())))return;
        if(LbcBadApple.recognized(stack)){
            String id="native/drop/badapple";
            add(all,id,"drop/badapple",ability(id,nativeId,data),
                    rule("","LbcBadApple/native whole-body claim",Event.DROP,Location.LOOSE_ITEM));return;
        }
        if(LbcMagnum.roundMode(stack)>=0){
            String id="native/magnum/ammunition";
            add(all,id,"magnum/ammunition",ability(id,nativeId,data),
                    new Rule("","LbcMagnum/LbcTmtInventory",Set.of(Event.AMMO),
                            Set.of(Location.INVENTORY,Location.CRAFT_GRID,Location.AMMUNITION)));return;
        }
        int looseKind=LbcTmtLooseRewards.prototypeKind(stack);
        if(looseKind!=0){
            String id=looseKind==1?LbcTmtLooseRewards.COPIER_FAMILY:LbcTmtLooseRewards.MULTIPLY_FAMILY;
            add(all,id,looseKind==1?"loose/copier":"loose/multiply",ability(id,nativeId,data),
                    rule("","LbcLooseRewards/LbcTmtLooseRewards",Event.DROP,Location.LOOSE_ITEM));return;
        }
        if(LbcSpecialDrops.nativeRecognized(stack)){
            data.putInt(LbcTmtDrops.FLAG,1);var keys=new TreeSet<String>();
            for(var key:data.keySet())if(!SERVICE.contains(key)&&!key.startsWith("tmt_"))keys.add(normal(key));
            String family="drop/"+(keys.isEmpty()?nativeId.getPath().replaceFirst("^old_","").replaceFirst("_old$",""):String.join("/",keys));String id="native/"+family;
            add(all,id,family,ability(id,nativeId,data),rule("","LbcSpecialDrops/native dispatch",Event.DROP,Location.LOOSE_ITEM));
            if(blockItem!=null){all.get(id).events.add(Event.BLOCK);all.get(id).locations.add(Location.USED_HAND);}return;
        }
        if(blockItem!=null){String id="native/block/"+nativeId.getPath();add(all,id,"block/"+nativeId.getPath(),ability(id,nativeId,data),
                rule("","LbcTmtBlockUses/native factory",Event.BLOCK,Location.USED_HAND));return;}
        if(stack.is(LbcClockFragments.ITEM)&&LbcClockFragments.duration(stack)==0){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,"Unparameterized clock transport; sixteen authored selectors use the native whole-world simulation owner.");return;
        }
        if(stack.is(LbcCorruptedGlyphs.DUST)&&LbcCorruptedGlyphs.kind(stack)==null){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,
                    "Unparameterized glyph transport; four authored glyphkind payloads are admitted by the real native use-on factory, not a blank dust ability");return;
        }
        if(LbcTmtItems.output(stack.getItem())||stack.is(LbcTmtItems.TOKEN)){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,"TMT transport/acquisition is not a recursively sampled property");return;
        }

        if(LbcTmtManaPotions.delivery(stack)==LbcTmtManaPotions.Delivery.DRINK){
            String family=LbcTmtManaPotions.family(stack),id="native/"+family;
            var events=EnumSet.of(Event.CONSUME);var locations=EnumSet.of(Location.USED_HAND);
            if(LbcSheep.potion(stack)){events.add(Event.INVENTORY);events.add(Event.DROP);locations.add(Location.INVENTORY);locations.add(Location.LOOSE_ITEM);}
            add(all,id,family,ability(id,nativeId,data),new Rule(family,"Native PotionContents.onConsume/LbcTmtManaPotions/LbcSheep",events,locations));return;
        }
        if(stack.is(LbcMana.MANA_POTION)||stack.is(LbcMana.MANA_SPLASH_POTION)||stack.is(LbcMana.MANA_LINGERING_POTION)){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,
                    "Dose-free native potion transport has no independent effect; actual authored PotionContents doses are sampled through their native drink/splash/lingering domains");return;
        }
        String trinket=data.getStringOr("trinket","");
        if(!trinket.isEmpty()){
            Trinket admitted=null;
            for(var candidate:TRINKETS.values())if(candidate.identity().equals(trinket)){admitted=candidate;break;}
            if(admitted==null){
                finding(findings,nativeId.toString(),source,Disposition.PENDING_NATIVE_DOMAIN,"Trinket STRING selector has no audited native consumer: "+trinket);return;
            }
            String id="native/trinket/"+trinket;
            String owner=Set.of("drojji","mandoahuet","psycho_soul","banangay","hedgehog_gauntlet","twilight_sparkle","poly_gem","sunprism").contains(trinket)?
                    "LbcReactiveTrinkets/LbcTrinkets":"LbcTrinkets";
            var events=EnumSet.of(Event.INVENTORY,admitted.event());
            add(all,id,"trinket/"+trinket,ability(id,nativeId,data),new Rule("trinket/"+trinket,owner,events,Set.of(Location.INVENTORY)));return;
        }
        if(LbcKaraSlabs.isSlab(stack)){
            // Copper oxidation has no branch in the native damage owner. Keep
            // one family and the three native identities; vanilla loot aliases
            // resolve to those identities instead of adding acquisition variants.
            var canonicalItem=nativeId.getPath().contains("weathered")?LbcKaraSlabs.WEATHERED:
                    nativeId.getPath().contains("exposed")?LbcKaraSlabs.EXPOSED:LbcKaraSlabs.OXIDIZED;
            var flags=new CompoundTag();flags.putInt("kara",1);
            var id="native/kara";
            add(all,id,"kara",ability(id,BuiltInRegistries.ITEM.getKey(canonicalItem),flags),rule("kara","LbcKaraSlabs",Event.MELEE,Location.MAIN_HAND));return;
        }

        Rule selected=nativeId.toString().equals("skywars2:imba_axe")?
                rule("imba_axe","LbcImbaAxe/LbcTmtTools",Event.BLOCK,Location.MAIN_HAND):null;
        int mystical=data.getIntOr("mystical",0),legacy=data.getIntOr("mystical_old",0);
        if(mystical!=0||legacy!=0){
            int group=mystical!=0?mystical:legacy,type=data.getIntOr("type",0);
            int maximum=switch(group){case 1->mystical==0?6:8;case 2,4->6;case 3->3;default->0;};
            if(type<1||type>maximum){
                finding(findings,nativeId.toString(),source,Disposition.PENDING_NATIVE_DOMAIN,"Unsupported mystical group/type/version: "+group+"/"+type+"/"+(mystical==0?"old":"current"));return;
            }
            selected=switch(group){
                case 1->rule("mystical/passive",mystical==0?"LbcBalancePassive (legacy mode)":"LbcMagic",Event.INVENTORY,Location.INVENTORY);
                case 2->rule("mystical/reactive","LbcReactiveArtifacts",Event.MELEE,Location.INVENTORY);
                case 3->rule("mystical/jump","LbcJumpArtifacts",Event.JUMP,Location.INVENTORY);
                default->rule("mystical/incoming","LbcIncomingArtifacts",Event.INCOMING,Location.INVENTORY);
            };
        }else if(LbcTouhouCollectibles.id(stack)>=1&&LbcTouhouCollectibles.id(stack)<=10){
            selected=rule("touhou/"+LbcTouhouCollectibles.id(stack),"LbcTouhouCollectibles",Event.INVENTORY,Location.INVENTORY);
        }else if(data.contains("pill")){
            int id=LbcPills.pillId(stack);
            if(LbcPills.definition(id)==null){
                finding(findings,nativeId.toString(),source,Disposition.PENDING_NATIVE_DOMAIN,"Pill selector has no native typed definition");return;
            }
            selected=rule("pill","LbcPills",Event.CONSUME,Location.USED_HAND);
        }else{
            // Named food identities are a finite manifest owned by their real consume consumers.
            for(var food:LbcNamedFoodItems.entries())if(data.getIntOr(food.flag(),0)==1){
                selected=rule("food/"+normal(food.flag()),"LbcNamedFoods/LbcFoodLifecycle/LbcAttributeFood",Event.CONSUME,Location.USED_HAND);break;
            }
            if(selected==null)for(var entry:RULES.entrySet())if(data.getIntOr(entry.getKey(),0)==1){selected=entry.getValue();break;}
        }
        if(selected!=null){
            // A flag alone cannot preserve these native component/class contracts.
            if(selected.events().contains(Event.DEATH)&&!selected.family().equals("broken_totem")){
                // The native source requires these flags together. Infinite's
                // minimum Infinity comes from its real per-property native item.
                if(selected.family().equals("infinite_totem")&&!nativeId.toString().equals("skywars2:infinite_totem")){
                    finding(findings,nativeId.toString(),source,Disposition.PENDING_COMPONENT_ADAPTER,
                            "Standalone totem category is not Infinite Totem: native identity/default Infinity prerequisite is missing");return;
                }
                data.putInt("transformable",1);data.putInt("totem",1);
            }
            String id="native/"+selected.family();
            add(all,id,selected.family(),ability(id,nativeId,data),selected);return;
        }
        if(LbcPills.certificate(stack)){
            String id="native/phd";add(all,id,"phd",ability(id,nativeId,data),rule("phd","LbcPhdCrafting",Event.CRAFT,Location.CRAFT_GRID));return;
        }
        if(nativeTool||nativeAmmo)return;
        if(nativeId.toString().equals("skywars2:bucket_head")||nativeId.toString().equals("skywars2:oguzok_sword")){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,
                    "Ordinary equipment/weapon components and presentation, with no independent custom item-data ability reader; physical items and the separate thrown-bucket/Chef owners remain intact, generated stats/equipment stay with LbcTmtProperties");return;
        }
        if(data.getIntOr("multiplying",0)==1||data.getIntOr("randchanted",0)==1){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,
                    "Multiplying/RandChanted flags classify LbcBolts reforge recipes; native benefits are attribute/enchantment components already owned by the TMT property generator, not a separate flag effect");return;
        }
        if(LbcWands.isBoltMaterial(stack)||nativeId.toString().equals("skywars2:phd_pill")&&!data.contains("pill")){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,"Recipe material or unparameterized pill prototype; actual supported pill outcomes are sampled from LbcPills.definitions");return;
        }
        if(nativeId.toString().equals("skywars2:dev_tools")||nativeId.toString().equals("skywars2:consumed_block")){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,"Developer UI/generated terrain representation, not an independent gameplay item ability");return;
        }
        if(data.getIntOr("academy_token",0)==1||data.getIntOr("magic_academy",0)==1){
            finding(findings,nativeId.toString(),source,Disposition.PENDING_NATIVE_DOMAIN,
                    "LbcAcademy owns a real loose-item structure placement/acquisition operation; it requires its DROP/LOOSE_ITEM event and is not an inert service token or a held-use ability");return;
        }
        var keys=new TreeSet<String>(data.keySet());keys.removeAll(SERVICE);keys.removeIf(key->key.startsWith("tmt_"));
        if(!keys.isEmpty()&&Set.of("wisp_air","wisp_fire","wisp_light","wisp_nature","wisp_shadow","wisp_water").containsAll(keys)){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_INERT_KEY,
                    "Source-inert TMT item fields "+keys+": exhaustive lbc/lbcr item-data and dynamic-dispatch audit finds no reader; separate companion ENTITY tags do not activate from these fields (TMT_WISP_REACHABILITY_2026_11_07)");return;
        }
        if(keys.equals(Set.of("god"))&&data.getIntOr("god",0)==1){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_INERT_KEY,
                    "God-hedgehog trophy marker: both legacy namespaces contain only item producers/presentation and no item-data reader; the separate boss entity engine is not an item ability");return;
        }
        if(keys.equals(Set.of("slow_falling"))&&data.getIntOr("slow_falling",0)==1){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_INERT_KEY,
                    "Source-inert item field: aimbob and TMT tag pool only write slow_falling1; neither legacy namespace nor native item-data consumers read it. The actual slow-falling effect/potion/pill domains remain separate");return;
        }
        // Identity-only drops, arrows, placeables and collectible outputs can have no
        // custom data. An empty payload is not proof of an inert item.
        if(keys.isEmpty()&&nativeId.getNamespace().equals("minecraft")){
            finding(findings,nativeId.toString(),source,Disposition.EXCLUDED_SERVICE,"Vanilla carrier with only category/parameter markers and no admitted native operation");return;
        }
        finding(findings,nativeId.toString(),source,Disposition.PENDING_NATIVE_DOMAIN,
                "No audited semantic native domain yet; keys="+keys+". Identity/components may own the effect; do not infer deadness or support from owner-name matches.");
    }

    private static LbcTmtAbilities.Ability ability(String family,Identifier nativeId,CompoundTag source){
        var data=source.copy();data.putString(LbcTmtAbilities.NATIVE,nativeId.toString());
        return new LbcTmtAbilities.Ability(family,CustomData.of(data));
    }
    private static void add(Map<String,Collected> all,String id,String name,LbcTmtAbilities.Ability ability,Rule rule){
        var family=all.computeIfAbsent(id,ignored->new Collected(id,name));
        family.variants.putIfAbsent(canonical(ability.data().copyTag()),ability);
        family.events.addAll(rule.events());family.locations.addAll(rule.locations());family.owners.add(rule.owner());
    }
    private static void finding(Map<String,Finding> findings,String item,String source,Disposition disposition,String reason){
        findings.putIfAbsent(item+"\n"+source+"\n"+disposition+"\n"+reason,new Finding(item,source,disposition,reason));
    }
    private static String normal(String flag){return flag.endsWith("_old")?flag.substring(0,flag.length()-4):flag;}
    /** Preserve tag types and list order while removing compound insertion order from variant weights. */
    private static String canonical(Tag value){
        if(value instanceof CompoundTag compound)return compound.keySet().stream().sorted()
                .map(key->key.length()+":"+key+"="+canonical(compound.get(key))).collect(java.util.stream.Collectors.joining(",","{","}"));
        if(value instanceof ListTag list){var values=new ArrayList<String>();for(int i=0;i<list.size();i++)values.add(canonical(list.get(i)));return "["+String.join(",",values)+"]";}
        return value.getId()+":"+value;
    }
    private static Rule rule(String family,String owner,Event event,Location location){return new Rule(family,owner,Set.of(event),Set.of(location));}
    private static void flags(Map<String,Rule> rules,String owner,Event event,Location location,String... flags){
        for(var flag:flags)rules.put(flag,rule(normal(flag),owner,event,location));
    }
    private static Map<String,Rule> rules(){
        // Ordered by actual event owners; selectors remain inside these complete payloads.
        var result=new LinkedHashMap<String,Rule>();
        // Legacy TMT rolls these achievement labels; neither flag converts its carrier to a trident.
        flags(result,"Native player_killed_entity/lbc:true_advancements/tridents/harpoon",Event.KILL,Location.MAIN_HAND,"harpoon");
        flags(result,"Native using_item/lbc:true_advancements/tridents/sea_legend",Event.USING,Location.USED_HAND,"sea_legend");
        flags(result,"LbcMagic dirty inventory cache/LbcElectro",Event.INVENTORY,Location.INVENTORY,"electroblocker");
        result.put("halloween_mace",new Rule("halloween_mace","LbcHalloweenWind/LbcTmtSelfAbilities",
                Set.of(Event.SELF,Event.JUMP),Set.of(Location.USED_HAND)));
        flags(result,"LbcPendingSwords",Event.MELEE,Location.MAIN_HAND,"cool","cool_old","fragmentsword","fragmentsword_old","ender","antimagic");
        flags(result,"LbcPendingCombat",Event.MELEE,Location.MAIN_HAND,"chaos_swo","gravitationsword","piston_sword");
        flags(result,"LbcClassicMelee/LbcEvolutionSwordItem",Event.SELF,Location.MAIN_HAND,"feeding","vampire","seaguardian","evolution");
        flags(result,"LbcChargedSwords",Event.SELF,Location.MAIN_HAND,"triple_golden_sword","blazefire_sword");
        flags(result,"LbcDevilSword",Event.MELEE,Location.MAIN_HAND,"heal");
        flags(result,"LbcSpecialTools",Event.MELEE,Location.MAIN_HAND,"mortis","banhammer","death_skythe");
        flags(result,"LbcMetalPipe",Event.MELEE,Location.MAIN_HAND,"metal_pipe","divider_by_metal_pipe");
        flags(result,"LbcImbaPickaxe/LbcTmtTools",Event.MINING,Location.MAIN_HAND,"chaos_hoe");
        flags(result,"LbcImbaShovel/LbcTmtTools",Event.MINING,Location.MAIN_HAND,"chaos_sho");
        flags(result,"LbcImbaHoe/LbcTmtTools",Event.MINING,Location.MAIN_HAND,"chaos_pic");
        flags(result,"LbcSteampunkBreaker/LbcTmtTools",Event.MINING,Location.MAIN_HAND,"steampunk_pickaxe");
        flags(result,"LbcBalance",Event.SHOT,Location.USED_HAND,"explosive","double_old");
        flags(result,"LbcShulkerBow",Event.SHOT,Location.USED_HAND,"shulker","shulker_new");
        result.put("shulker_new",rule("shulker","LbcShulkerBow",Event.SHOT,Location.USED_HAND));
        flags(result,"LbcMadokaBow",Event.SHOT,Location.USED_HAND,"madoka_bow");
        flags(result,"LbcChargedBows",Event.SHOT,Location.USED_HAND,"railgun_bow","shish_gun");
        flags(result,"LbcMinigunCrossbow",Event.SHOT,Location.USED_HAND,"fast","fast_old");
        flags(result,"LbcLegacyRanged/LbcBalanceExtras",Event.SHOT,Location.USED_HAND,"double","fastfired","fastexplode","penisgun");
        flags(result,"LbcImbaCrossbows",Event.SHOT,Location.USED_HAND,"imba_fast_crossbow","imba_shotgun","imba_sniper_crossbow","imba_super_shotgun","imba_gatlingator");
        flags(result,"LbcOrdinaryAccessories",Event.INVENTORY,Location.INVENTORY,"save","magnet","demonkishki");
        flags(result,"LbcRegenerationBelt",Event.INVENTORY,Location.INVENTORY,"heall");
        flags(result,"LbcBalancePassive (legacy mode)",Event.INVENTORY,Location.INVENTORY,"heall_old","manabelt_old");
        flags(result,"LbcMana",Event.INVENTORY,Location.INVENTORY,"manabelt");
        result.put("zaika",new Rule("zaika","Native Neko/Loli loot slot predicates",Set.of(Event.INVENTORY),Set.of(Location.INVENTORY,Location.EQUIPPED)));
        result.put("reactive_old",new Rule("reactive_old","LbcBalancePassive (legacy mode)/LbcJumpArtifacts/LbcTmtPassiveQueries",Set.of(Event.EQUIPMENT,Event.SELF,Event.JUMP),Set.of(Location.EQUIPPED)));
        flags(result,"LbcDeathCompass",Event.INVENTORY,Location.INVENTORY,"death_compass");
        result.put("pumpkin_curse",new Rule("pumpkin_curse","LbcCursedPumpkin",Set.of(Event.INVENTORY,Event.DROP),Set.of(Location.INVENTORY,Location.LOOSE_ITEM)));
        result.put("malevich",new Rule("malevich","LbcMalevich",Set.of(Event.INVENTORY,Event.DROP),Set.of(Location.INVENTORY,Location.LOOSE_ITEM)));
        flags(result,"LbcCursedPumpkin",Event.MELEE,Location.MAIN_HAND,"pumpkin_king");
        flags(result,"LbcEternalWealth",Event.INVENTORY,Location.INVENTORY,"greedstone");
        flags(result,"LbcRerollCrafting",Event.CRAFT,Location.CRAFT_GRID,"reroll_machine");
        flags(result,"LbcSteampunkGlove",Event.HELD,Location.MAIN_HAND,"steampunk_sword");
        flags(result,"LbcKfcFood",Event.CONSUME,Location.USED_HAND,"kfc_chick","eat_shit");
        flags(result,"LbcOreshek",Event.CONSUME,Location.USED_HAND,"eat_oreshek");
        flags(result,"LbcClassicMelee",Event.HELD,Location.MAIN_HAND,"one_punch","hylian");
        flags(result,"LbcItems/LbcFrost",Event.MELEE,Location.MAIN_HAND,"icesword");
        flags(result,"LbcIceMaterials",Event.INVENTORY,Location.INVENTORY,"runic_ice_shard");
        flags(result,"LbcIceMaterials",Event.HELD,Location.USED_HAND,"neutral_ball");
        flags(result,"LbcClownKit",Event.EQUIPMENT,Location.EQUIPPED,"clown_mask");
        flags(result,"LbcAimbobRewards",Event.EQUIPMENT,Location.EQUIPPED,"night_vision");
        result.put("dynamite",rule("dynamite_chestplate","LbcBalanceExtras",Event.INCOMING,Location.EQUIPPED));
        flags(result,"LbcMana/LbcOverload",Event.EQUIPMENT,Location.EQUIPPED,"mage","overload");
        result.put("explosive_feets",new Rule("explosive_feets","LbcItems/RocketState",Set.of(Event.EQUIPMENT,Event.JUMP),Set.of(Location.EQUIPPED)));
        flags(result,"LbcNexusWires",Event.INVENTORY,Location.INVENTORY,"nexus_wire_red","nexus_wire_black");
        flags(result,"LbcQuiver",Event.INVENTORY,Location.INVENTORY,"arrowgenerator");
        flags(result,"LbcGaaraPot",Event.INCOMING,Location.INVENTORY,"gaara_tikva");
        flags(result,"LbcIncomingArtifacts",Event.MELEE,Location.INVENTORY,"gaara","gaara_old");
        flags(result,"LbcFoodLifecycle",Event.CONSUME,Location.USED_HAND,"inf","inf_old","halloween_food","halloween_chorus","bad_apple");
        flags(result,"LbcAttributeFood",Event.CONSUME,Location.USED_HAND,"random_health","random_damage","random_speed",
                "armorattribute","armor_toughnessattribute","attack_damageattribute","attack_speedattribute",
                "knockback_resistanceattribute","luckattribute","max_healthattribute","movement_speedattribute");
        flags(result,"LbcCannedFood",Event.CONSUME,Location.USED_HAND,"konserva");
        flags(result,"LbcEdibleSpells",Event.CONSUME,Location.USED_HAND,"eat_damage","eat_fire","eat_explode","eat_big_explode","eat_chaos_explode",
                "eat_haste","eat_strength","eat_weakness","eat_poison","eat_wither","eat_regeneration","eat_buffs","eat_lightning",
                "eat_psycho","eat_polymorph","eat_teleport","eat_teleport2","eat_slab","eat_book","eat_evoker","eat_cum","gain_bucket",
                "eat_danmaku","yellow_lightings_bottle","eat_glowing","eat_damage_all");
        flags(result,"LbcTotems/LbcTmtTotems",Event.DEATH,Location.USED_HAND,"deviltotem","broken_totem");
        result.put("totem",rule("infinite_totem","LbcTotems/LbcTmtTotems",Event.DEATH,Location.USED_HAND));
        flags(result,"LbcHolyShield",Event.USING,Location.USED_HAND,"holyshield");
        flags(result,"LbcCunny",Event.USING,Location.USED_HAND,"nahida_dakimakura");
        flags(result,"LbcDefensiveArcanums",Event.HELD,Location.USED_HAND,"healing_arcanum","barrier_arcanum");
        flags(result,"LbcTanner",Event.HELD,Location.USED_HAND,"tannerkivi");
        flags(result,"LbcBabylon",Event.HELD,Location.USED_HAND,"gate_of_babylon");
        flags(result,"LbcSyringe",Event.HELD,Location.USED_HAND,"siggewinne_syringe");
        flags(result,"LbcVoidArcanum",Event.HELD,Location.USED_HAND,"void_curse");
        flags(result,"LbcKiuas",Event.HELD,Location.USED_HAND,"kiuaskivi","sun_arcanum");
        flags(result,"LbcMoiseiBoots",Event.EQUIPMENT,Location.EQUIPPED,"moisei");
        flags(result,"LbcArmorSets",Event.EQUIPMENT,Location.EQUIPPED,"assasin","erosion","gownoleggings","paladin");
        flags(result,"LbcShulkerArmor",Event.EQUIPMENT,Location.EQUIPPED,"shulkshell");
        flags(result,"LbcPhantomArmor",Event.EQUIPMENT,Location.EQUIPPED,"phantom");
        flags(result,"LbcNanobotArmor",Event.EQUIPMENT,Location.EQUIPPED,"nanobots");
        flags(result,"LbcGlassArmor",Event.EQUIPMENT,Location.EQUIPPED,"glass");
        flags(result,"LbcBerserkArmor",Event.EQUIPMENT,Location.EQUIPPED,"berserk");
        flags(result,"LbcFleshArmor",Event.EQUIPMENT,Location.EQUIPPED,"flesh");
        flags(result,"LbcNyauwuArmor",Event.EQUIPMENT,Location.EQUIPPED,"nyauwu");
        flags(result,"LbcTechnoArmor",Event.EQUIPMENT,Location.EQUIPPED,"techno");
        flags(result,"LbcTechnoArmor/LbcTechnoFlight",Event.EQUIPMENT,Location.EQUIPPED,"reactive","technomk2");
        flags(result,"LbcTraveller",Event.EQUIPMENT,Location.EQUIPPED,"jumpboost");
        flags(result,"LbcBalancePassive (legacy mode)",Event.EQUIPMENT,Location.EQUIPPED,"jumpboost_old","magichat_old");
        flags(result,"LbcChaosArmor",Event.EQUIPMENT,Location.EQUIPPED,"chaoschestplate");
        return java.util.Collections.unmodifiableMap(result);
    }

    /** Exact23 STRING selectors consumed by native shot, damage, cast, summon and periodic owners. */
    private static Map<Item,Trinket> trinkets(){
        var result=new LinkedHashMap<Item,Trinket>();
        result.put(LbcTrinkets.BLAZE,new Trinket("blaze",Event.SHOT));
        result.put(LbcTrinkets.GHAST,new Trinket("ghast",Event.SHOT));
        result.put(LbcTrinkets.PENIS_GUN,new Trinket("penis_gun",Event.SHOT));
        result.put(LbcTrinkets.BEDROCK_BALLS,new Trinket("bedrcock",Event.SHOT));
        result.put(LbcTrinkets.VOLOS,new Trinket("volos",Event.SHOT));
        result.put(LbcTrinkets.COCKTAIL,new Trinket("cocktail",Event.SHOT));
        result.put(LbcTrinkets.FOURIER,new Trinket("fourier",Event.SHOT));
        result.put(LbcTrinkets.ROCKET_SPELL,new Trinket("rocket_spell",Event.SHOT));
        result.put(LbcTrinkets.DROJJI,new Trinket("drojji",Event.MELEE));
        result.put(LbcTrinkets.MANDOAHUET,new Trinket("mandoahuet",Event.MELEE));
        result.put(LbcTrinkets.PSYCHO_SOUL,new Trinket("psycho_soul",Event.MELEE));
        result.put(LbcTrinkets.INTERCAL,new Trinket("intercal",Event.MELEE));
        result.put(LbcTrinkets.SNOW_GEM,new Trinket("snow_pearl",Event.MELEE));
        result.put(LbcTrinkets.BANANGAY,new Trinket("banangay",Event.INCOMING));
        result.put(LbcTrinkets.HEDGEHOG_GAUNTLET,new Trinket("hedgehog_gauntlet",Event.INCOMING));
        result.put(LbcTrinkets.ICE_GEM,new Trinket("ice_gem",Event.INCOMING));
        result.put(LbcTrinkets.GNEISS_STONE,new Trinket("gneiss_stone",Event.INCOMING));
        result.put(LbcTrinkets.TWILIGHT_SPARKLE,new Trinket("twilight_sparkle",Event.INVENTORY));
        result.put(LbcTrinkets.POLY_GEM,new Trinket("poly_gem",Event.INVENTORY));
        result.put(LbcTrinkets.GLASS_SHARD,new Trinket("sunprism",Event.INVENTORY));
        result.put(LbcTrinkets.BLOOD_ANAL_BOOM,new Trinket("blood_anal_boom",Event.WAND_CAST));
        result.put(LbcTrinkets.MANA_GEM,new Trinket("mana_gem",Event.WAND_CAST));
        result.put(LbcTrinkets.NETHER_PLUG,new Trinket("nether_plug",Event.SUMMON));
        return java.util.Collections.unmodifiableMap(result);
    }
}
