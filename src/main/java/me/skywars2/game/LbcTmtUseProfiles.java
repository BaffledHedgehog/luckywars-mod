package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Bootstrap catalog of native direct-use inputs; the ordinary native owners execute every effect. */
final class LbcTmtUseProfiles {
    static final String NATIVE_ITEM="tmt_native_item";
    private static final String PREFIX="native_use/";
    private static final Set<String> SERVICE=Set.of("accessory","arcanum","arcanumt","arcanum2t","arcanum5t",
            "armor","axe","bow","boots","chestplate","crossbow","craftusable","gadget","helmet","hoe",
            "instantitem","leggings","libspell","magicwand","pickaxe","projegadget","raycastspell","shield",
            "shooter","shovel","special","spell","spellitem","spellwithcooldown","spellwithcooldown_n",
            "summoner","summonerwand","sword","swordwithability","tool","transformable","trident","staff",
            "upgradable","upgrade_available","mana","mode","type","upgraded","spellid","spellcount",
            "cooltype","phd","drop","imba","tmt","tmt_item","tmt_texture_seed");

    record Family(String id,List<LbcTmtAbilities.Ability> variants) {
        Family { variants=List.copyOf(variants);if(variants.isEmpty())throw new IllegalArgumentException("Empty native use family"); }
    }
    record Domains(int registeredItems,int canonicalPrototypes,int admittedPrototypes,int variants,
                   List<String> unrecognizedItems,List<String> excludedExistingItems,List<String> missingNativeItems,
                   List<String> legacyOwnerGaps) {
        Domains { unrecognizedItems=List.copyOf(unrecognizedItems);excludedExistingItems=List.copyOf(excludedExistingItems);
            missingNativeItems=List.copyOf(missingNativeItems);legacyOwnerGaps=List.copyOf(legacyOwnerGaps); }
    }
    private final GameController game;
    private final List<Family> families;
    private final Domains domains;
    private final Set<String> familyIds;
    private final Map<String,Integer> sourceOrder;

    /** Call after holder-bound item defaults finalize; retain this instance until the registry generation changes. */
    LbcTmtUseProfiles(GameController game,Predicate<ItemStack> nativeCandidate) {
        this.game=java.util.Objects.requireNonNull(game);java.util.Objects.requireNonNull(nativeCandidate);
        Predicate<ItemStack> supported=nativeCandidate.or(LbcTmtUseProfiles::additionalCandidate);
        var collected=new TreeMap<String,LinkedHashMap<String,LbcTmtAbilities.Ability>>();
        var rejected=new java.util.TreeSet<String>();var existing=new java.util.TreeSet<String>();
        var missing=new java.util.TreeSet<String>();var gaps=new ArrayList<String>();int registered=0,canonical=0,admitted=0;
        for(var id:BuiltInRegistries.ITEM.keySet().stream().filter(id->id.getNamespace().equals("skywars2")).sorted().toList()) {
            registered++;var stack=BuiltInRegistries.ITEM.getValue(id).getDefaultInstance().copyWithCount(1);
            if(add(stack,supported,collected,rejected,existing))admitted++;
        }
        try(var input=LbcTmtUseProfiles.class.getResourceAsStream("/skywars2/tmt_use_profiles.json")) {
            if(input==null)throw new IllegalStateException("Missing native TMT use prototypes");
            var source=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            if(source.get("schema").getAsInt()!=1)throw new IllegalStateException("Unknown native TMT prototype schema");
            for(var value:source.getAsJsonArray("prototypes")) {
                canonical++;var row=value.getAsJsonObject();var id=Identifier.parse(row.get("item").getAsString());
                if(!Set.of("skywars2","minecraft").contains(id.getNamespace())||!BuiltInRegistries.ITEM.containsKey(id)){missing.add(id.toString());continue;}
                var stack=BuiltInRegistries.ITEM.getValue(id).getDefaultInstance().copyWithCount(1);
                var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                // Native defaults and proper SNBT preserve string selectors, compounds and lists.
                for(var raw:row.getAsJsonArray("data"))if(!raw.isJsonNull())
                    data.merge(TagParser.parseCompoundFully(raw.isJsonPrimitive()?raw.getAsString():raw.toString()));
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                if(add(stack,supported,collected,rejected,existing))admitted++;
            }
            for(var value:source.getAsJsonArray("knownLegacyOwnerGaps")) {
                var row=value.getAsJsonObject();gaps.add(row.get("key").getAsString()+": "+row.get("status").getAsString());
            }
        }catch(Exception failure){throw new IllegalStateException("Invalid native TMT use prototypes",failure);}
        var result=new ArrayList<Family>();
        collected.forEach((key,variants)->result.add(new Family(PREFIX+digest(key),List.copyOf(variants.values()))));
        families=List.copyOf(result);
        var order=new java.util.HashMap<String,Integer>();
        for(var family:families){
            int index=Integer.MAX_VALUE;
            for(var variant:family.variants){
                var data=variant.data().copyTag();var nativeId=Identifier.parse(data.getStringOr(NATIVE_ITEM,"minecraft:air"));
                var prototype=BuiltInRegistries.ITEM.getValue(nativeId).getDefaultInstance().copyWithCount(1);
                prototype.set(DataComponents.CUSTOM_DATA,variant.data());
                index=Math.min(index,game.items().wands().tmtNativeUseOrder(prototype));
            }
            order.put(family.id,index);
        }
        sourceOrder=Map.copyOf(order);familyIds=Set.copyOf(order.keySet());
        domains=new Domains(registered,canonical,admitted,families.stream().mapToInt(f->f.variants.size()).sum(),
                List.copyOf(rejected),List.copyOf(existing),List.copyOf(missing),gaps);
    }

    List<Family> families(){return families;}
    Domains domains(){return domains;}
    LbcTmtAbilities.Ability draw(RandomSource random) {
        if(families.isEmpty())throw new IllegalStateException("No recognized native TMT use families");
        var family=families.get(random.nextInt(families.size()));
        return family.variants.get(random.nextInt(family.variants.size()));
    }

    /** Real used-hand continuation; the native adapter owns suppression, debit, replacement and producer capture. */
    int use(ServerPlayer player,InteractionHand hand) {
        if(player==null||hand==null||!game.items().tmt().admitted(player)
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return 0;
        var stack=player.getItemInHand(hand);if(LbcTmtAbilities.active(stack))return 0;int completed=0;
        var slots=new ArrayList<>(LbcTmtAbilities.read(stack));
        slots.sort(java.util.Comparator.comparingInt(slot->sourceOrder.getOrDefault(slot.id(),Integer.MAX_VALUE)));
        for(var slot:slots) {
            if(!slot.id().startsWith(PREFIX))continue;
            if(stack.isEmpty()||player.getItemInHand(hand)!=stack)break;
            // Reject a forged/unresolved family ID; native owner performs the actual input admission.
            if(!familyIds.contains(slot.id()))continue;
            boolean used=LbcTmtAbilities.invoke(stack,slot.id(),()->{
                if(additionalCandidate(stack))return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker()
                        .interact(player,player.level(),hand).consumesAction();
                return game.items().wands().cast(player,hand);
            });
            if(used)completed++;
        }
        return completed;
    }

    /** Existing ordinary input callbacks own these abilities before the wand dispatcher. */
    private static boolean additionalCandidate(ItemStack stack){
        return LbcCunnyItems.slab(stack)||ItemData.intValue(stack,"cunny_slab",0)==1
                ||ItemData.intValue(stack,"mellstroy_mask",0)==1
                ||LbcThrownUtilities.matches(stack)||LbcGodDice.matches(stack)||LbcWhiteArrows.matches(stack)
                ||LbcTeleportGadgets.matches(stack)||LbcBouncingPoop.matches(stack)||LbcClownKit.matches(stack)
                ||LbcHedgehogSpikes.legacyThrow(stack)!=0;
    }

    private static boolean add(ItemStack stack,Predicate<ItemStack> candidate,
            Map<String,LinkedHashMap<String,LbcTmtAbilities.Ability>> collected,Set<String> rejected,Set<String> existing) {
        if(stack.isEmpty())return false;
        String nativeId=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(existing(stack)){existing.add(nativeId);return false;}
        // Classification only: never invoke Item.use or an owner with a synthetic player/stack here.
        if(!candidate.test(stack)){rejected.add(nativeId);return false;}
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        data.remove(LbcTmtAbilities.SLOTS);data.remove(LbcTmtAbilities.ACTIVE);data.remove("tmt_item");data.remove("tmt_texture_seed");
        String family=family(nativeId,data),id=PREFIX+digest(family);data.putString(NATIVE_ITEM,nativeId);
        var ability=new LbcTmtAbilities.Ability(id,CustomData.of(data));
        // Full typed payload and native identity determine variants; acquisition count/style never enters this key.
        collected.computeIfAbsent(family,ignored->new LinkedHashMap<>()).putIfAbsent(canonical(ability.data().copyTag()),ability);
        return true;
    }
    private static boolean existing(ItemStack stack) {
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY);
        for(var wand:LbcWands.tmtAbilities())if(id.equals(wand.nativeItem())||data.matchedBy(wand.flags().copyTag()))return true;
        // Self profiles are charge/self continuations, not the same operation as a native use.
        // In particular Triple Golden/Blazefire still need their real native projectile casts.
        return false;
    }
    private static String family(String nativeId,CompoundTag data) {
        // Library IDs are real supported spells, not independent loose selector tags.
        if(data.getIntOr("libspell",0)==1&&data.getIntOr("spellid",0)>0)return "library/"+data.getIntOr("spellid",0);
        var keys=new java.util.TreeSet<String>();
        for(var key:data.keySet())if(!SERVICE.contains(key)&&!key.startsWith("tmt_"))
            keys.add(key.endsWith("_old")?key.substring(0,key.length()-4):key);
        if(!keys.isEmpty())return String.join("/",keys);
        // Identity-only native inputs remain real distinct profiles instead of fabricated flag names.
        String normalized=nativeId.replace("skywars2:academy_","skywars2:");
        return normalized.endsWith("_old")?normalized.substring(0,normalized.length()-4):normalized;
    }
    private static String canonical(CompoundTag data) {
        // CompoundTag textual order is not a semantic variant; sorted root keys give stable bootstrap deduplication.
        return data.keySet().stream().sorted().map(key->key+"="+data.get(key)).collect(java.util.stream.Collectors.joining(";"));
    }
    private static String digest(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
