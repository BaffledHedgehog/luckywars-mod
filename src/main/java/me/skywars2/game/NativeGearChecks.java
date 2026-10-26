package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real shield use/blocking, holy pulse geometry and native trident throw/impact/return/Riptide. */
public final class NativeGearChecks {
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Native gear: "+why);}
    private static ItemStack loot(AllGrimoireChecks.Fixture f,String id){var out=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:ported/"+id);require(out.size()==1&&out.getFirst().getCount()==1,"single authored leaf "+id);return out.getFirst();}
    private static void reset(AllGrimoireChecks.Fixture f){f.base.owner.stopUsingItem();f.base.game.items().corruptedGlyphs().stop();f.restoreWorld=true;f.reset();f.base.owner.getInventory().clearContent();f.beta.setPos(f.at.add(30,200,0));f.beta.setNoGravity(true);}
    private static <T extends LivingEntity>T add(AllGrimoireChecks.Fixture f,T target,Vec3 at){target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.setNoGravity(true);if(target instanceof net.minecraft.world.entity.Mob m)m.setNoAi(true);target.setPos(at);require(f.level.addFreshEntity(target),"native target publication");return target;}
    private static WitherSkeleton undead(AllGrimoireChecks.Fixture f,Vec3 at){return add(f,new WitherSkeleton(EntityTypes.WITHER_SKELETON,f.level),at);}
    private static void use(AllGrimoireChecks.Fixture f,ItemStack stack,InteractionHand hand){var p=f.base.owner;p.stopUsingItem();p.setItemInHand(hand,stack);require(p.gameMode.useItem(p,f.level,stack,hand).consumesAction()&&p.isUsingItem()&&p.getUsedItemHand()==hand,"real native use starts from "+hand);}
    private static void presentation(AllGrimoireChecks.Fixture f){
        for(var id:List.of("sea_legend","harpoon","hylian_shield","holy_shield")){var s=loot(f,id);require(s.has(DataComponents.CUSTOM_NAME)&&s.has(DataComponents.LORE)&&s.has(DataComponents.TOOLTIP_STYLE),"source presentation "+id);require(s.is(id.endsWith("shield")?Items.SHIELD:Items.TRIDENT),"real vanilla item "+id);}
        var h=loot(f,"hylian_shield");var holy=loot(f,"holy_shield");require(h.has(DataComponents.UNBREAKABLE)&&h.get(DataComponents.BANNER_PATTERNS).layers().size()==6&&holy.get(DataComponents.BANNER_PATTERNS).layers().size()==3,"source unbreakable shield and six/three native banner layers");
        var sea=loot(f,"sea_legend");var registry=f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);require(sea.getEnchantments().getLevel(registry.getOrThrow(Enchantments.RIPTIDE))==10&&loot(f,"harpoon").getEnchantments().getLevel(registry.getOrThrow(Enchantments.IMPALING))==10,"source RiptideX and ImpalingX");
        var shields=new HashSet<String>();var tridents=new HashSet<String>();for(int i=0;i<512;i++){
            var s=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:transformables_shield").getFirst();var d=LbcFartus.data(s);
            if(d.getIntOr("hylianshield",0)==1)shields.add("hylian");else if(d.getIntOr("holyshield",0)==1)shields.add("holy");else{require(s.is(LbcElectro.SHIELD),"complete shield pool returns the actual native Electro shield");shields.add("electro");}
            s=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:transformables_trident").getFirst();d=LbcFartus.data(s);tridents.add(d.getIntOr("sea_legend",0)==1?"sea":"harpoon");
        }require(shields.size()==3&&tridents.size()==2,"all five exact pool outcomes observed");require(f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:electro_shield").getFirst().is(LbcElectro.SHIELD),"original Electro alias preserves native abilities");
    }
    private static void holy(AllGrimoireChecks.Fixture f){for(var hand:InteractionHand.values()){
        reset(f);var p=f.base.owner;var g=f.base.game;var at=p.getEyePosition().add(p.getLookAngle().scale(3));var victim=undead(f,at);var friend=undead(f,at.add(1,0,0));var boundary=undead(f,at.add(4,0,0));var outside=undead(f,at.add(4.01,0,0));var greed=undead(f,at.add(-1,0,0));greed.entityTags().add("sin_greed_active");var npc=undead(f,at.add(0,1,0));npc.entityTags().add("strict_map_object");var cow=add(f,new Cow(EntityTypes.COW,f.level),at.add(0,0,1));
        g.items().summons().track(p,friend);g.items().mana().set(p,-500);int mana=g.items().mana().get(p);
        var center=BlockPos.containing(at);f.level.setBlock(center,Blocks.STONE.defaultBlockState(),2|16|32);
        use(f,loot(f,"holy_shield"),hand);p.doTick();
        require(victim.getHealth()==94&&friend.getHealth()==94&&boundary.getHealth()==94&&outside.getHealth()==100,"native6 magic within exact eye+look3 sphere, including owned undead, through terrain");
        require(greed.getHealth()==100&&npc.getHealth()==100&&cow.getHealth()==100,"Greed/roles/non-unholy preserved");require(victim.getKillCredit()==p&&g.items().mana().get(p)==mana,"actual source author and free activation without mana admission/debit");
        require(f.base.recording.packets.stream().anyMatch(packet->packet instanceof ClientboundLevelParticlesPacket),"source yellow particle packets emitted");
        p.doTick();require(victim.getHealth()==94,"ordinary magic HurtTime prevents duplicate equal damage");for(int i=0;i<10;i++)victim.tick();p.doTick();require(victim.getHealth()==88,"ordinary native cooldown expiry permits next pulse");
        p.stopUsingItem();for(int i=0;i<12;i++)victim.tick();p.doTick();require(victim.getHealth()==88,"releasing shield immediately ends its pulses");
    }reset(f);}
    private static void dispelAndGuards(AllGrimoireChecks.Fixture f){reset(f);var p=f.base.owner;var g=f.base.game;var at=p.getEyePosition().add(0,0,3);var service=g.items().corruptedGlyphs();
        var glyphs=new ArrayList<LbcCorruptedGlyphs.Glyph>();for(var kind:LbcCorruptedGlyphs.Kind.values())glyphs.add(service.spawn(p,at,kind));var far=service.spawn(p,at.add(4.1,0,0),LbcCorruptedGlyphs.Kind.AHRIMAN);
        var target=undead(f,at);var innocent=add(f,new Cow(EntityTypes.COW,f.level),at.add(0,0,.2));var zombie=new Zombie(EntityTypes.ZOMBIE,f.level);var data=new CompoundTag();data.putString("DeathLootTable","lbc:reactive_shish");zombie.load(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),data));add(f,zombie,at.add(0,0,1));
        var shield=loot(f,"holy_shield");use(f,shield,InteractionHand.MAIN_HAND);p.doTick();
        require(glyphs.stream().allMatch(v->v.display.isRemoved())&&!far.display.isRemoved()&&service.count()==1,"holy purification removes all four in-range powder fields, preserving outside field");
        require(!p.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)&&!innocent.hasEffect(net.minecraft.world.effect.MobEffects.WITHER),"purification does not trigger terminal Bafomet curse");var adv=g.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/shields/holy"));require(glyphs.size()==4&&adv!=null&&p.getAdvancements().getOrStartProgress(adv).isDone(),"source reactive zombie presence awards actual Holy achievement");
        p.stopUsingItem();reset(f);at=p.getEyePosition().add(0,0,3);target=undead(f,at);use(f,loot(f,"holy_shield"),InteractionHand.MAIN_HAND);g.items().timeStop().begin(f.level,40);float hp=target.getHealth();p.doTick();require(target.getHealth()==hp,"dimension stop blocks holy pulse");g.items().timeStop().end(f.level);p.entityTags().add("time_stopped");p.doTick();require(target.getHealth()==hp,"personal stop blocks pulse");p.entityTags().remove("time_stopped");
        p.entityTags().add("nomagic_active");p.doTick();require(target.getHealth()==94,"source holy blessing ignores antimagic");p.entityTags().remove("nomagic_active");reset(f);
    }
    private static void blocking(AllGrimoireChecks.Fixture f){for(var hand:InteractionHand.values()){
        reset(f);var p=f.base.owner;var shield=loot(f,"hylian_shield");var attacker=undead(f,f.at.add(0,0,4));use(f,shield,hand);for(int i=0;i<6;i++)p.doTick();require(p.isBlocking(),"native blocking delay "+hand);float hp=p.getHealth();p.hurtServer(f.level,f.level.damageSources().mobAttack(attacker),100);require(p.getHealth()==hp&&shield.getDamageValue()==0,"native frontal blocking and source unbreakability "+hand);attacker.setPos(f.at.add(0,0,-4));p.setInvulnerableTime(0);p.damageCooldownTime=0;p.hurtServer(f.level,f.level.damageSources().mobAttack(attacker),6);require(p.getHealth()<hp,"native directional shield still permits rear damage");
    }reset(f);}
    private static List<ThrownTrident> tridents(AllGrimoireChecks.Fixture f){var out=new ArrayList<ThrownTrident>();for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&e instanceof ThrownTrident t&&!t.isRemoved())out.add(t);return out;}
    private static ThrownTrident throwHarpoon(AllGrimoireChecks.Fixture f,InteractionHand hand){var s=loot(f,"harpoon");var before=new HashSet<>(tridents(f));use(f,s,hand);for(int i=0;i<11;i++)f.base.owner.doTick();f.base.owner.releaseUsingItem();var shots=tridents(f).stream().filter(t->!before.contains(t)).toList();require(s.isEmpty()&&shots.size()==1&&shots.getFirst().getOwner()==f.base.owner,"native charged throw consumes exact hand and creates one owned trident");require(shots.getFirst().getWeaponItem().getDamageValue()==1&&LbcFartus.data(shots.getFirst().getWeaponItem()).getIntOr("harpoon",0)==1,"projectile keeps authored stack and native one durability debit");return shots.getFirst();}
    private static void throwsAndRiptide(AllGrimoireChecks.Fixture f){for(var hand:InteractionHand.values()){
        reset(f);var p=f.base.owner;var victim=add(f,new Cow(EntityTypes.COW,f.level),f.at.add(0,0,5));var t=throwHarpoon(f,hand);var frozenAt=t.position();var frozenVelocity=t.getDeltaMovement();f.base.game.items().timeStop().begin(f.level,40);f.level.tickNonPassenger(t);require(t.position().equals(frozenAt),"native time stop freezes thrown trident");f.base.game.items().timeStop().end(f.level);require(t.getDeltaMovement().distanceToSqr(frozenVelocity)<1e-10,"resume restores exact native throw velocity");t.setPos(victim.position().add(0,.7,-2));t.setDeltaMovement(0,0,3);t.tick();require(victim.getHealth()==92&&victim.getKillCredit()==p,"actual native trident collision deals8 and retains author");
        t.tick();require(t.isNoPhysics()&&t.getOwner()==p,"native LoyaltyI begins return after impact");t.setPos(p.position().add(0,.5,0));t.playerTouch(p);require(t.isRemoved()&&p.getInventory().getItem(0).is(Items.TRIDENT),"native Loyalty pickup returns the actual trident");reset(f);
        var sea=loot(f,"sea_legend");p.setItemInHand(hand,sea);p.doTick();require(!p.gameMode.useItem(p,f.level,sea,hand).consumesAction()&&!p.isUsingItem(),"native RiptideX dry admission remains vanilla");
        f.level.setBlock(BlockPos.containing(p.position()),Blocks.WATER.defaultBlockState(),2|16|32);p.doTick();require(p.isInWaterOrRain(),"native water contact");p.setOnGround(false);use(f,sea,hand);for(int i=0;i<11;i++)p.doTick();p.releaseUsingItem();require(p.isAutoSpinAttack()&&p.getDeltaMovement().length()>5&&!sea.isEmpty()&&sea.getDamageValue()==1&&tridents(f).isEmpty(),"native RiptideX spins/propels instead of throwing, with native durability "+hand);p.setDeltaMovement(Vec3.ZERO);for(int i=0;i<22;i++)p.doTick();
    }reset(f);}
    public static void run(GameController g){var p=g.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var unlocks=new HashSet<>(g.players().get(p.getUUID()).lbcUnlocks);var progress=new HashMap<net.minecraft.advancements.AdvancementHolder,Set<String>>();for(var adv:g.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);progress.put(adv,done);}
        try(var f=new AllGrimoireChecks.Fixture(g)){try{presentation(f);holy(f);dispelAndGuards(f);blocking(f);throwsAndRiptide(f);}finally{g.items().timeStop().end(f.level);p.entityTags().remove("time_stopped");p.entityTags().remove("nomagic_active");g.items().corruptedGlyphs().stop();f.restoreWorld=true;}}
        finally{progress.forEach((adv,before)->{for(var key:adv.value().criteria().keySet())if(!before.contains(key))p.getAdvancements().revoke(adv,key);});var profile=g.players().get(p.getUUID());profile.lbcUnlocks.clear();profile.lbcUnlocks.addAll(unlocks);}SkyWars2.LOGGER.info("[check] PASS native-gear");}
    private static AllGrimoireChecks.Fixture retained;private static ThrownTrident retainedTrident;private static LbcCorruptedGlyphs.Glyph retainedGlyph;
    public static void stopProbe(GameController g){require(retained==null,"exclusive retained fixture");var f=new AllGrimoireChecks.Fixture(g,true);try{reset(f);retainedTrident=throwHarpoon(f,InteractionHand.MAIN_HAND);retainedGlyph=g.items().corruptedGlyphs().spawn(f.base.owner,f.at.add(0,0,12),LbcCorruptedGlyphs.Kind.BELIAL);use(f,loot(f,"holy_shield"),InteractionHand.OFF_HAND);require(!retainedTrident.isRemoved()&&retainedGlyph!=null,"native throw, powder and active shield retained for actual stop");retained=f;}catch(Throwable e){f.close();throw e;}SkyWars2.LOGGER.info("[check] PASS native-gear-stop-probe");}
    public static void cleanup(GameController g){require(retained!=null,"actual stop fixture exists");try{require(retainedTrident.isRemoved()&&retainedGlyph.display.isRemoved()&&g.items().corruptedGlyphs().count()==0,"actual match stop removes native thrown trident and powder");}finally{retained.close();retained=null;retainedTrident=null;retainedGlyph=null;}SkyWars2.LOGGER.info("[check] PASS native-gear-cleanup");}
}
