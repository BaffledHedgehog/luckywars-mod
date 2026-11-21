package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.loot.LbcAllItemPools;
import me.skywars2.game.items.loot.LootTestCatalog;
import me.skywars2.game.items.wands.LbcBolts;
import me.skywars2.game.items.wands.LbcWands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.skywars2.game.effects.magic.LbcMana;

/** Acquisition checks use published native loot and the existing Reforge work queue. */
public final class ShooterPoolChecks {
    private ShooterPoolChecks() { }
    private static final String NORMAL="transformables_shooter", ULTRA="transformables_shooter_all";
    private static final String[] ORDINARY={"phantom_knives","mobgun","mobgun","kfc_gun","razorpine","attack_helicopter","laser_minigun","woodificator"};
    private static final String[] EXPANDED={null,"ak47","magnum","terra_razorpine","govnomet","helicopter_boss","slr"};
    private static String stopEpoch;
    private static ItemEntity stopInput;
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Shooter pools: "+message);
    }
    private static ServerPlayer owner(GameController game) {
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null,"requires TestAlpha"); return owner;
    }
    private static LootTable table(GameController game,String id) {
        return game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));
    }
    private static JsonObject encoded(GameController game,ServerPlayer owner,String id) {
        return LootTable.DIRECT_CODEC.encodeStart(game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE),table(game,id)).getOrThrow().getAsJsonObject();
    }
    private static LootParams params(Entity owner) {
        return new LootParams.Builder((net.minecraft.server.level.ServerLevel)owner.level()).withParameter(LootContextParams.ORIGIN,owner.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY,owner).create(LootContextParamSets.COMMAND);
    }
    private static List<ItemStack> isolated(GameController game,ServerPlayer owner,String id,int index,long seed) {
        var root=encoded(game,owner,id); var pool=root.getAsJsonArray("pools").get(0).getAsJsonObject();
        var entries=new JsonArray(); entries.add(pool.getAsJsonArray("entries").get(index).deepCopy()); pool.add("entries",entries);
        var decoded=LootTable.DIRECT_CODEC.parse(game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE),root).getOrThrow();
        return decoded.getRandomItems(params(owner),seed);
    }
    private static void equal(List<ItemStack> actual,List<ItemStack> reference,String label) {
        require(actual.size()==reference.size(),label+" output stack count");
        for(int i=0;i<actual.size();i++) require(actual.get(i).getCount()==reference.get(i).getCount()
                && ItemStack.isSameItemSameComponents(actual.get(i),reference.get(i)),label+" complete native item/components/count "+i);
    }
    /** Score existence and the original fixed-holder value are both restored. */
    private static final class Luck implements AutoCloseable {
        final net.minecraft.world.scores.Scoreboard board;
        final net.minecraft.world.scores.Objective objective;
        final ScoreHolder holder=ScoreHolder.forNameOnly("final_luck_modifier");
        final boolean existed; final int value;
        Luck(GameController game) {
            board=game.server().getScoreboard(); objective=board.getObjective("lbc.math");
            require(objective!=null,"existing lbc.math objective");
            var score=board.getPlayerScoreInfo(holder,objective); existed=score!=null; value=existed?score.value():0;
        }
        void set(int value) { board.getOrCreatePlayerScore(holder,objective).set(value); }
        public void close() { if(existed) set(value); else board.resetSinglePlayerScore(holder,objective); }
    }
    public static void menu(GameController game) {
        var owner=owner(game);
        try(var luck=new Luck(game)) {
            luck.set(0);
            for(String id:List.of(NORMAL,ULTRA)) {
                require(encoded(game,owner,id).equals(encoded(game,owner,"ported/"+id)),id+" published alias equals the actual acquisition table");
                require(LootTestCatalog.isSuggested(Identifier.parse("lbc:ported/"+id)),id+" published pool is test-ready in native suggestions");
                var entries=encoded(game,owner,id).getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
                var references=id.equals(NORMAL)?ORDINARY:EXPANDED;
                require(entries.size()==references.length,id+" source entry count");
                for(int i=0;i<references.length;i++) {
                    var entry=entries.get(i).getAsJsonObject();
                    int weight=entry.has("weight")?entry.get("weight").getAsInt():1;
                    require(weight==(id.equals(NORMAL)?(i==2?10000:10):(i==0?7:1)),id+" source weight "+i);
                    if(id.equals(NORMAL) && i==2) {
                        require(isolated(game,owner,id,i,719).isEmpty(),"conditional Mobgun absent at luck0");
                        luck.set(7);
                        equal(isolated(game,owner,id,i,719),game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/mobgun"),"conditional Mobgun luck7");
                        luck.set(0); continue;
                    }
                    if(references[i]!=null) equal(isolated(game,owner,id,i,719),
                            game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+references[i]),id+" leaf "+i);
                }
            }
            // Exhaustive leaves above are deterministic; seeded full rolls verify the published graph without rare-outcome assertions.
            var accepted=accepted(game,owner);
            for(String id:List.of(NORMAL,ULTRA)) for(int score:new int[]{0,7}) {
                luck.set(score);
                for(int seed=1;seed<=256;seed++) recognized(table(game,id).getRandomItems(params(owner),seed),accepted,id+" seed "+seed);
                for(int i=0;i<32;i++) recognized(game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+id),accepted,id+" native roll");
            }
        }
        SkyWars2.LOGGER.info("[check] PASS shooter-pools-menu (8 ordinary + 6 expanded leaves; luck0/7; seeds1..256 per graph; 128 native rolls)");
    }
    private static List<ItemStack> accepted(GameController game,ServerPlayer owner) {
        var result=new ArrayList<ItemStack>(); var names=new HashSet<String>();
        names.addAll(List.of(ORDINARY)); for(var id:EXPANDED) if(id!=null) names.add(id);
        names.add("ak47_finite"); // The broader uniform shooter category includes the skill variant too.
        for(var id:names) result.addAll(game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+id));
        for(var stack:result) if(!stack.is(Items.WHEAT_SEEDS)) require(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("skywars2"),"qualified native registry identity");
        return result;
    }
    private record ExpectedStack(ItemStack template,int minimumDamage,int maximumDamage){
        boolean matches(ItemStack actual){
            if(minimumDamage==maximumDamage)return ItemStack.isSameItemSameComponents(template,actual);
            int damage=actual.getDamageValue();
            if(damage<minimumDamage||damage>maximumDamage)return false;
            var normalized=actual.copy();normalized.setDamageValue(template.getDamageValue());
            return ItemStack.isSameItemSameComponents(template,normalized);
        }
    }
    private static void uniformProfiles(GameController game,Entity donor,String type,List<ExpectedStack> result){
        // The original weighted menu still has its canonical leaf assertions.
        // Reforge now uses uniform views, which also admit full contextual item
        // profiles. Enumerate those native factory outputs before checking that
        // conversion preserves every component; a carrier-only check is too weak.
        var pools=game.items().allItemPools();pools.initialize();
        {
            var types=List.of(type);var slots=pools.slots(LbcAllItemPools.Domain.CUSTOM,types);
            for(int index=0;index<slots.size();index++){
                var first=new ProfileChoice(0);
                pools.drawSlot(LbcAllItemPools.Domain.CUSTOM,types,index,
                        new net.minecraft.world.level.storage.loot.LootContext.Builder(params(donor)).withOptionalRandomSource(first).create(java.util.Optional.empty()),ignored->{});
                for(int profile=0;profile<first.bound;profile++){
                    var bounds=new ArrayList<List<ItemStack>>();
                    for(float fraction:new float[]{0,Math.nextDown(1f)}){
                        var choice=new ProfileChoice(profile,fraction);var output=new ArrayList<ItemStack>();
                        pools.drawSlot(LbcAllItemPools.Domain.CUSTOM,types,index,
                                new net.minecraft.world.level.storage.loot.LootContext.Builder(params(donor)).withOptionalRandomSource(choice).create(java.util.Optional.empty()),output::add);
                        require(choice.bound==first.bound,"fixed source context retains its exact admitted profile count");bounds.add(output);
                    }
                    require(bounds.get(0).size()==bounds.get(1).size(),"source durability randomization never changes bundle cardinality");
                    for(int item=0;item<bounds.get(0).size();item++){
                        var low=bounds.get(0).get(item);var high=bounds.get(1).get(item);
                        int minimum=Math.min(low.getDamageValue(),high.getDamageValue()),maximum=Math.max(low.getDamageValue(),high.getDamageValue());
                        var expected=new ExpectedStack(low,minimum,maximum);
                        require(expected.matches(low)&&expected.matches(high),"native damage extrema preserve every other component");
                        if(minimum!=maximum){
                            require(low.is(Items.CROSSBOW),"reviewed source random durability is restricted to crossbows");
                            var invalid=low.copy();invalid.setDamageValue(minimum-1);require(!expected.matches(invalid),"reject damage below native source range");
                            invalid.setDamageValue(maximum+1);require(!expected.matches(invalid),"reject damage above native source range");
                        }
                        result.add(expected);
                    }
                }
            }
        }
    }
    private static final class ProfileChoice extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        final int profile;final Float fraction;int bound;boolean selected;
        ProfileChoice(int profile){this(profile,null);}
        ProfileChoice(int profile,Float fraction){super(0x53484f4f544552L);this.profile=profile;this.fraction=fraction;}
        @Override public float nextFloat(){
            // Inverted imba/Halloween chance gates exclude copies of these source
            // profiles. Admit all copies for the oracle, then vary durability only
            // after materialize has selected its profile with nextInt.
            if(!selected)return Math.nextDown(1f);
            return fraction==null?super.nextFloat():fraction;
        }
        @Override public int nextInt(int bound){
            if(!selected){selected=true;this.bound=bound;require(profile<bound,"valid exact native profile index");return profile;}
            return super.nextInt(bound);
        }
    }
    private static int recognized(List<ItemStack> stacks,List<ItemStack> accepted,String label) {
        return recognized(stacks,stack->accepted.stream().anyMatch(ref->ItemStack.isSameItemSameComponents(ref,stack)),label);
    }
    private static int recognized(List<ItemStack> stacks,java.util.function.Predicate<ItemStack> accepted,String label) {
        int weapons=0,seeds=0;
        for(var stack:stacks) {
            require(accepted.test(stack),label+" recognized full native payload: "+stack.getItem()+" "+stack.getComponentsPatch());
            if(stack.is(Items.WHEAT_SEEDS)) seeds+=stack.getCount(); else weapons+=stack.getCount();
        }
        require(weapons>0 && seeds%16==0 && seeds<=weapons*16,label+" one weapon/unit and source16 seed side output");
        return weapons;
    }
    private static ItemStack donor(int count) {
        var stack=new ItemStack(Items.STICK,count); var tag=new net.minecraft.nbt.CompoundTag();
        tag.putInt("transformable",1); tag.putInt("shooter",1); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); return stack;
    }
    private static ItemStack tool(boolean ultra) {
        if(ultra) return new ItemStack(LbcWands.INFBOLT);
        var stack=new ItemStack(LbcWands.ACADEMY_LIGHTNING_BOLTS);
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); tag.putInt("upgraded",1); tag.putInt("mode",2);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); return stack;
    }
    private static ItemEntity input(ServerPlayer owner,int count) {
        var at=owner.getEyePosition().add(owner.getLookAngle().scale(2));
        var item=new ItemEntity(owner.level(),at.x,at.y,at.z,donor(count)); item.setNoGravity(true);
        require(owner.level().addFreshEntity(item),"input enters native level"); return item;
    }
    private static void drain(GameController game) {
        for(int i=0;i<256 && game.items().bolts().pendingReforgeCount()>0;i++) game.items().bolts().flushReforgeJobs();
        require(game.items().bolts().pendingReforgeCount()==0,"shared queue drains");
    }
    private static void prepare(GameController game,ServerPlayer owner) {
        owner.closeContainer(); owner.getInventory().clearContent(); owner.getInventory().setSelectedSlot(0);
        owner.removeAllEffects(); owner.setNoGravity(true); owner.setShiftKeyDown(false); owner.setGameMode(GameType.SURVIVAL);
        owner.entityTags().removeAll(Set.of("time_stopped","nomagic_active","developer"));
        owner.teleportTo(game.maps().level(),32.5,270,32.5,Set.of(),0,0,true);
        for(int x=1;x<=3;x++) for(int z=1;z<=3;z++) owner.level().getChunk(x,z);
        game.items().mana().noMagic(owner,0); game.items().mana().set(owner,0);
    }
    private static void fundReforge(GameController game,ServerPlayer owner) {
        var mana=game.items().mana();
        mana.set(owner,owner.getMainHandItem().is(LbcWands.INFBOLT)?0:
                Math.max(0,mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("lightning_bolt_wand_mana_use"),"lbc.math").get()));
    }
    public static void run(GameController game) {
        var owner=owner(game); require(game.playing() && game.allowDamage(owner),"active native match");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var initial=new HashSet<Entity>(); game.maps().level().getAllEntities().forEach(initial::add);
        try(var luck=new Luck(game)) {
            game.items().bolts().stop(); drain(game); prepare(game,owner); luck.set(0);
            var accepted=accepted(game,owner);
            for(boolean ultra:new boolean[]{false,true}) {
                require(LbcBolts.reforgeTables(donor(5),ultra).equals(List.of(ultra?ULTRA:NORMAL)),"native shooter category routing");
                owner.setItemInHand(InteractionHand.MAIN_HAND,tool(ultra));
                owner.getInventory().setItem(9,new ItemStack(Items.DIAMOND,11));
                var input=input(owner,5);
                var profiles=new ArrayList<ExpectedStack>();
                for(var stack:accepted)profiles.add(new ExpectedStack(stack,stack.getDamageValue(),stack.getDamageValue()));
                uniformProfiles(game,input,ultra?"shooter_all":"shooter",profiles);
                fundReforge(game,owner);
                require(game.items().wands().use(owner,InteractionHand.MAIN_HAND) && game.items().bolts().pendingReforgeCount()>0
                        &&game.items().mana().get(owner)==0,"configured-price cast queues "+ultra);
                fundReforge(game,owner);
                require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==0,"repeat pending cast pays the same configured price"); drain(game);
                var output=new ArrayList<ItemEntity>();
                owner.level().getAllEntities().forEach(e->{if(e instanceof ItemEntity item && !initial.contains(e) && !e.isRemoved()) output.add(item);});
                require(input.isRemoved() && recognized(output.stream().map(ItemEntity::getItem).toList(),stack->profiles.stream().anyMatch(profile->profile.matches(stack)),"actual reforge "+ultra)==5,"full stack converts once without recursive outputs");
                require(owner.getMainHandItem().getCount()==1 && owner.getInventory().getItem(9).is(Items.DIAMOND)
                        && owner.getInventory().getItem(9).getCount()==11 && game.items().mana().get(owner)==0,"weapon/inventory preserved and mana debited");
                output.forEach(Entity::discard);
                for(boolean stop:new boolean[]{false,true}) {
                    var pending=input(owner,64); var before=pending.getItem().copy();
                    fundReforge(game,owner);
                    require(game.items().wands().use(owner,InteractionHand.MAIN_HAND) && game.items().bolts().pendingReforgeCount()>0,"cancellation fixture queued");
                    if(stop) game.items().bolts().stop(); else game.items().bolts().forget(owner);
                    drain(game); require(!pending.isRemoved() && pending.getItem().getCount()==64 && ItemStack.isSameItemSameComponents(before,pending.getItem()),"cancel/stop preserves unprocessed shooter stack"); pending.discard();
                }
            }
        } finally {
            game.items().bolts().stop(); drain(game);
            var created=new ArrayList<Entity>(); game.maps().level().getAllEntities().forEach(e->{if(!initial.contains(e) && !(e instanceof ServerPlayer)) created.add(e);});
            created.forEach(Entity::discard); saved.restore(game);
        }
        SkyWars2.LOGGER.info("[check] PASS shooter-pools");
    }
    public static void stopProbe(GameController game) {
        var owner=owner(game); require(game.playing() && game.allowDamage(owner),"active stop probe");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        try {
            prepare(game,owner); owner.setItemInHand(InteractionHand.MAIN_HAND,tool(true));
            stopInput=input(owner,64); stopEpoch=game.items().epoch();
            fundReforge(game,owner);
            require(game.items().wands().use(owner,InteractionHand.MAIN_HAND) && game.items().bolts().pendingReforgeCount()>0,"old match shooter work queued");
        } finally { saved.restore(game); owner.entityTags().add("time_stopped"); }
        SkyWars2.LOGGER.info("[check] PASS shooter-pools-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(!game.items().currentEpoch(stopEpoch) && game.items().bolts().pendingReforgeCount()==0,"real match stop invalidates shooter work");
        var owner=owner(game); owner.entityTags().remove("time_stopped"); drain(game);
        require(stopInput==null || stopInput.isRemoved() || stopInput.getItem().getCount()==64,"old queue never consumes pending shooter input");
        if(stopInput!=null) stopInput.discard(); stopInput=null; stopEpoch=null;
        SkyWars2.LOGGER.info("[check] PASS shooter-pools-cleanup");
    }
}
