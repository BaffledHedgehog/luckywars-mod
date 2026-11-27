package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.effects.EffectLayers;
import me.skywars2.game.entities.CoreEntityMerging;
import me.skywars2.game.items.gadgets.LbcKitUtilityItems;
import me.skywars2.game.modes.legacy.LbcLegacyEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Focused real native boundaries; the integrating runner owns invocation and artifact qualification. */
public final class GameplayBug25Checks {
    private GameplayBug25Checks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Gameplay bugs25: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();f.move(f.owner,f.home.add(0,60,0));f.move(f.other,f.owner.position().add(3,0,0));
            var initial=new HashSet<UUID>();for(var entity:f.level.getAllEntities())initial.add(entity.getUUID());
            try{voidDamage(game,f);raidMarkers(f);ownedSlows(game,f);experience(game,f);experienceFallback(f);crafting(f);}
            finally{
                var fresh=new ArrayList<Entity>();for(var entity:f.level.getAllEntities())if(!initial.contains(entity.getUUID())&&!(entity instanceof net.minecraft.server.level.ServerPlayer))fresh.add(entity);
                for(var entity:fresh){game.items().entityUnloaded(entity);entity.discard();}
                game.items().exasperato().stop();
            }
        }
        SkyWars2.LOGGER.info("[check] PASS gameplay-bugs25 (void admission, zero Raid layers, Exasperato preflight15/17, XP conservation, native x3 preview/take/overflow)");
    }
    private static void voidDamage(GameController game,WoolBucketChecks.Fixture f){
        var score=game.items().mana().score(f.owner,"void_resistance");int old=score.get();float health=f.owner.getHealth();
        var position=f.owner.position();boolean frozen=game.server().tickRateManager().isFrozen(),personal=f.owner.entityTags().contains("time_stopped");
        require(game.server().tickRateManager().frozenTicksToRun()==0,"exclusive native consumer fixture has no borrowed stepped ticks");
        var border=new net.minecraft.world.damagesource.DamageSource(f.owner.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getOrThrow(net.minecraft.world.damagesource.DamageTypes.OUTSIDE_BORDER));
        try{
            score.set(7);
            require(!f.owner.hurtServer(f.level,f.owner.damageSources().fellOutOfWorld(),8)
                    &&!f.owner.hurtServer(f.level,border,8)&&f.owner.getHealth()==health,
                    "positive native void resistance protects both source void damage types");
            // Explicit manual second pulses exercise the actual source consumer, not a second fixture timer.
            score.set(2);f.move(f.owner,new net.minecraft.world.phys.Vec3(position.x,-.25,position.z));
            game.items().mana().secondPlayer(f.owner);
            require(f.owner.getY()==320&&f.owner.getX()==position.x&&f.owner.getZ()==position.z&&score.get()==1,
                    "actual native second consumer returns below-zero player to320 and spends exactly one second");
            // setFrozen publishes intent; native TickRateManager.tick publishes runsNormally for a frame.
            game.server().tickRateManager().setFrozen(true);game.server().tickRateManager().tick();game.items().mana().secondPlayer(f.owner);
            require(score.get()==1,"server-frozen native second consumer does not spend resistance");
            game.server().tickRateManager().setFrozen(false);game.server().tickRateManager().tick();f.owner.entityTags().add("time_stopped");game.items().mana().secondPlayer(f.owner);
            require(score.get()==1,"personal time stop pauses resistance consumption");
            if(!personal)f.owner.entityTags().remove("time_stopped");
            game.items().mana().secondPlayer(f.owner);require(score.get()==0,"next admitted second expires the exact remaining duration");
            game.items().mana().secondPlayer(f.owner);require(score.get()==0,"expired counter stays zero");
            f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
            require(f.owner.hurtServer(f.level,border,1)&&f.owner.getHealth()<health,
                    "expired void resistance restores native outside-border damage");
        }finally{game.server().tickRateManager().setFrozen(frozen);game.server().tickRateManager().tick();if(personal)f.owner.entityTags().add("time_stopped");else f.owner.entityTags().remove("time_stopped");f.move(f.owner,position);score.set(old);f.owner.setHealth(health);f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;}
    }
    private static void raidMarkers(WoolBucketChecks.Fixture f){
        require(!f.owner.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,0,0)),"zero-length native Raid Omen is rejected");
        f.owner.forceAddEffect(new MobEffectInstance(MobEffects.RAID_OMEN,0,0),null);
        LbcLegacyEffects.removeExpiredRaidMarker(f.owner);require(!f.owner.hasEffect(MobEffects.RAID_OMEN),"forced expired Raid Omen removed");
        f.owner.forceAddEffect(new MobEffectInstance(MobEffects.RAID_OMEN,1,1),null);
        var zero=f.owner.getEffect(MobEffects.RAID_OMEN);var live=new MobEffectInstance(MobEffects.RAID_OMEN,40,0);
        require(zero!=null,"native positive carrier admitted before importing expired saved state");
        ((EffectLayers.Access)zero).skywars2$layerDuration(0);
        ((EffectLayers.Access)zero).skywars2$layerHidden(live);
        LbcLegacyEffects.removeExpiredRaidMarker(f.owner);
        require(f.owner.getEffect(MobEffects.RAID_OMEN).getDuration()==40&&f.owner.getEffect(MobEffects.RAID_OMEN).getAmplifier()==0,
                "live hidden Raid layer is promoted and retained");
        f.owner.removeEffect(MobEffects.RAID_OMEN);
        f.owner.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,-1,0));LbcLegacyEffects.removeExpiredRaidMarker(f.owner);
        require(f.owner.getEffect(MobEffects.RAID_OMEN).isInfiniteDuration(),"infinite Raid Omen remains valid");f.owner.removeEffect(MobEffects.RAID_OMEN);
    }
    private static void ownedSlows(GameController game,WoolBucketChecks.Fixture f){
        game.config().lbcSkills=true;game.config().ranked=true;
        for(int skill:new int[]{15,17}){
            game.players().get(f.owner.getUUID()).lbcSkill=skill;game.players().get(f.other.getUUID()).lbcSkill=0;
            game.items().exasperato().cast(f.other,false);var original=game.items().exasperato().snapshot().getFirst();
            var owner=original.getOwner();game.items().exasperato().tick();var family=game.items().exasperato().snapshot();
            require(family.size()==(skill==15?2:4)&&family.stream().allMatch(c->c.age==1&&c.getOwner()==owner
                    &&c.entityTags().contains(skill==15?"doubled":"quadried")),"actual owned Exasperato pre-motion intake and one engine step skill="+skill);
            game.items().exasperato().stop();
        }
    }
    private static void experience(GameController game,WoolBucketChecks.Fixture f){
        double radius=CoreEntityMerging.settings().experienceMergeRadius;
        CoreEntityMerging.settings().experienceMergeRadius=3;
        try{
        game.players().get(f.owner.getUUID()).lbcSkill=17;
        var at=f.owner.position().add(0,4,0);var box=new net.minecraft.world.phys.AABB(at,at).inflate(8);
        var orb=new ExperienceOrb(f.level,at,Vec3.ZERO,7);require(f.level.addFreshEntity(orb),"real native experience orb");
        var extra=new ExperienceOrb(f.level,at,Vec3.ZERO,11);require(f.level.addFreshEntity(extra),"native experience merge admission");
        java.util.function.LongSupplier total=()->f.level.getEntitiesOfClass(ExperienceOrb.class,box).stream()
                .mapToLong(e->((CoreEntityMerging.Orb)e).skywars2$totalExperience()).sum();
        require(total.getAsLong()==18&&game.items().cloneSkills().copyNow(orb,17),"authored grouped XP commits exactly one real x4 clone transaction");
        require(total.getAsLong()==72&&orb.entityTags().contains("quadried"),"18 XP becomes exactly72 with completed source marker");
        CoreEntityMerging.mergeNearby(orb);for(int pass=0;pass<40;pass++)game.items().cloneSkills().tick();
        require(total.getAsLong()==72,"merged completed XP cannot grow through failed publication retries");
        require(game.items().cloneSkills().copyNow(orb,15)&&total.getAsLong()==144,"the other authored skill doubles once independently");
        CoreEntityMerging.mergeNearby(orb);
        require(!game.items().cloneSkills().copyNow(orb,15)&&!game.items().cloneSkills().copyNow(orb,17)&&total.getAsLong()==144,
                "both completion markers survive native XP coalescing");
        var fresh=new ExperienceOrb(f.level,at,Vec3.ZERO,7);require(f.level.addFreshEntity(fresh)&&!fresh.isRemoved(),
                "fresh unprocessed XP cannot merge into a completed group and inherit its markers");
        require(game.items().cloneSkills().copyNow(fresh,17)&&total.getAsLong()==172,"new7 XP independently quadruples to28 beside previous144");
        CoreEntityMerging.mergeNearby(fresh);require(total.getAsLong()==172,"periodic merge preserves distinct processed-marker groups");
        for(var body:f.level.getEntitiesOfClass(ExperienceOrb.class,box))body.discard();
        }finally{CoreEntityMerging.settings().experienceMergeRadius=radius;}
    }
    private static void experienceFallback(WoolBucketChecks.Fixture f){
        double radius=CoreEntityMerging.settings().experienceMergeRadius;
        CoreEntityMerging.settings().experienceMergeRadius=0;
        var at=f.owner.position().add(0,4,0);var box=new net.minecraft.world.phys.AABB(at,at).inflate(2);
        try{
            var marked=new ArrayList<ExperienceOrb>();var residues=new HashSet<Integer>();
            for(int n=0;residues.size()<40&&n<1000;n++){
                var orb=groupedOrb(f,at,7,2);orb.entityTags().add("quadried");
                require(f.level.addFreshEntity(orb)&&!orb.isRemoved(),"zero radius retains native publication");
                marked.add(orb);residues.add(Math.floorMod(orb.getId(),40));
            }
            require(residues.size()==40,"all native award merge residues represented");
            ExperienceOrb.award(f.level,at,7);
            var fresh=f.level.getEntitiesOfClass(ExperienceOrb.class,box,e->!e.entityTags().contains("quadried"));
            require(fresh.size()==1&&((CoreEntityMerging.Orb)fresh.getFirst()).skywars2$totalExperience()==7
                    &&marked.stream().allMatch(e->((CoreEntityMerging.Orb)e).skywars2$totalExperience()==14),
                    "native denomination award keeps fresh eligibility without incrementing a marked Count");
            var origin=marked.getFirst();var peer=marked.stream().filter(e->e!=origin&&(e.getId()-origin.getId())%40==0).findFirst();
            for(int n=0;peer.isEmpty()&&n<1000;n++){
                var candidate=groupedOrb(f,at,7,3);candidate.entityTags().add("quadried");require(f.level.addFreshEntity(candidate),"native matching group publication");
                marked.add(candidate);if((candidate.getId()-origin.getId())%40==0)peer=Optional.of(candidate);
            }
            require(peer.isPresent(),"native-compatible pair selected within bounded fixture work");
            var canMerge=ExperienceOrb.class.getDeclaredMethod("canMerge",ExperienceOrb.class);canMerge.setAccessible(true);
            require((boolean)canMerge.invoke(origin,peer.get()),"same-marker grouped native merge remains eligible");
            peer.get().entityTags().remove("quadried");
            require(!(boolean)canMerge.invoke(origin,peer.get()),"different clone markers reject the same otherwise native-compatible pair");
            peer.get().entityTags().add("quadried");
            long amount=((CoreEntityMerging.Orb)origin).skywars2$totalExperience()+((CoreEntityMerging.Orb)peer.get()).skywars2$totalExperience();
            var merge=ExperienceOrb.class.getDeclaredMethod("merge",ExperienceOrb.class);merge.setAccessible(true);merge.invoke(origin,peer.get());
            require(peer.get().isRemoved()&&origin.getValue()==7&&((CoreEntityMerging.Orb)origin).skywars2$totalExperience()==amount,
                    "native same-value Count addition preserves exact XP and source denomination");
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        finally{
            CoreEntityMerging.settings().experienceMergeRadius=radius;
            for(var body:f.level.getEntitiesOfClass(ExperienceOrb.class,box))body.discard();
        }
    }
    private static ExperienceOrb groupedOrb(WoolBucketChecks.Fixture f,Vec3 at,int value,int count){
        var orb=new ExperienceOrb(f.level,at,Vec3.ZERO,value);
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,f.owner.registryAccess());
        orb.saveWithoutId(out);var tag=out.buildResult();tag.putInt("Count",count);
        orb.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.owner.registryAccess(),tag));
        require(orb.getValue()==value&&((CoreEntityMerging.Orb)orb).skywars2$totalExperience()==(long)value*count,"native persisted Value and Count restored");
        return orb;
    }
    private static void crafting(WoolBucketChecks.Fixture f){
        var menu=(AbstractCraftingMenu)f.owner.inventoryMenu;var grid=menu.getInputGridSlots();
        var saved=grid.stream().map(slot->slot.getItem().copy()).toList();var cursor=menu.getCarried().copy();
        var stats=new IdentityHashMap<net.minecraft.world.item.Item,Integer>();
        for(var type:List.of(Items.DIAMOND,Items.DIAMOND_SWORD))stats.put(type,f.owner.getStats().getValue(Stats.ITEM_CRAFTED.get(type)));
        try{
            for(var donor:List.of(new ItemStack(Items.DIAMOND),new ItemStack(Items.DIAMOND,40),new ItemStack(Items.DIAMOND_SWORD))){
                f.owner.getInventory().clearContent();menu.setCarried(ItemStack.EMPTY);for(var slot:grid)slot.setByPlayer(ItemStack.EMPTY);
                int count=donor.getCount(),expected=Math.min(count*2,donor.getMaxStackSize());
                grid.get(0).setByPlayer(donor.copy());grid.get(1).setByPlayer(new ItemStack(LbcKitUtilityItems.TRIPLE));menu.slotsChanged(f.owner.getInventory());
                require(menu.getResultSlot().getItem().getCount()==expected,"native x3 preview exposes both new copies up to stack cap");
                GameChecks.inventoryClick(f.owner,0,0,ContainerInput.PICKUP);
                require(menu.getCarried().getCount()==expected&&grid.get(0).getItem().getCount()==count&&grid.get(1).getItem().isEmpty(),
                        "native result take retains original full donor stack and spends one tablet");
                int overflow=0;for(var stack:f.owner.getInventory().getNonEquipmentItems())if(stack.is(donor.getItem()))overflow+=stack.getCount();
                require(overflow==count*2-expected,"only count exceeding visible result cap goes to inventory");
            }
        }finally{
            menu.setCarried(ItemStack.EMPTY);for(var slot:grid)slot.setByPlayer(ItemStack.EMPTY);
            stats.forEach((item,n)->f.owner.getStats().setValue(f.owner,Stats.ITEM_CRAFTED.get(item),n));
            for(int i=0;i<grid.size();i++)grid.get(i).setByPlayer(saved.get(i));menu.setCarried(cursor);
        }
    }
}
