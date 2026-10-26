package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual native lethal hits/hand selection/effect restoration, hostile mobs, wild statuses/lava and debt expiry. */
public final class TotemChecks {
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Totems: "+why);}
    private static ItemStack loot(AllGrimoireChecks.Fixture f,String name){var out=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:ported/"+name);require(out.size()==1&&out.getFirst().getCount()==1,"singleton "+name);return out.getFirst();}
    private static void expand(AllGrimoireChecks.Fixture f){var center=BlockPos.containing(f.at);for(var pos:BlockPos.betweenClosed(center.offset(-16,-16,-16),center.offset(16,16,16)))f.cells.putIfAbsent(pos.immutable(),f.base.game.maps().snapshot(f.level,pos));}
    private static void reset(AllGrimoireChecks.Fixture f){f.base.game.items().totems().stop();f.restoreWorld=true;f.reset();f.base.owner.getInventory().clearContent();f.beta.setPos(f.at.add(30,200,0));f.beta.setNoGravity(true);f.base.owner.setAbsorptionAmount(0);}
    private static Cow cow(AllGrimoireChecks.Fixture f,Vec3 at){var c=new Cow(EntityTypes.COW,f.level);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);c.setHealth(100);c.setPos(at);require(f.level.addFreshEntity(c),"native target");return c;}
    private static List<ItemEntity> drops(AllGrimoireChecks.Fixture f){var out=new ArrayList<ItemEntity>();for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&!e.isRemoved()&&e instanceof ItemEntity item&&item.getItem().is(Items.TOTEM_OF_UNDYING))out.add(item);return out;}
    private static void pop(AllGrimoireChecks.Fixture f,ItemStack stack,InteractionHand hand){var p=f.base.owner;p.getInventory().clearContent();p.setItemInHand(hand,stack);p.setHealth(20);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
        require(p.hurtServer(f.level,f.level.damageSources().generic(),1000)&&p.isAlive()&&p.getHealth()==1,"real native lethal hit activates death protection");require(stack.isEmpty(),"native selected hand consumed once");}
    private static void presentation(AllGrimoireChecks.Fixture f){
        for(var name:List.of("infinite_totem","devil_totem")){var s=loot(f,name);require(s.is(Items.TOTEM_OF_UNDYING)&&s.has(DataComponents.DEATH_PROTECTION)&&s.get(DataComponents.ITEM_MODEL)!=null&&s.get(DataComponents.CUSTOM_NAME)!=null&&s.get(DataComponents.LORE)!=null,"real vanilla protection and authored presentation "+name);}
        require(f.base.game.items().totems().infinity(loot(f,"infinite_totem"),f.level)==1,"source InfinityI");var seen=new HashSet<String>();
        for(int n=0;n<256;n++){var out=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:transformables_totem");require(out.size()==1&&out.getFirst().has(DataComponents.DEATH_PROTECTION),"closed complete weighted totem pool");seen.add(out.getFirst().get(DataComponents.ITEM_MODEL).toString());}require(seen.equals(Set.of("lbc:infinite_totem","lbc:deviltotem")),"both exact pool outcomes observed");
    }
    private static void infinity(AllGrimoireChecks.Fixture f){
        for(var hand:InteractionHand.values()){reset(f);var s=loot(f,"infinite_totem");pop(f,s,hand);var p=f.base.owner;var returned=drops(f);require(returned.size()==1&&returned.getFirst().getItem().getCount()==1&&returned.getFirst().position().distanceToSqr(f.at)<1e-8,"one fresh totem returned on the ground for "+hand);require(returned.getFirst().getItem().getComponents().equals(loot(f,"infinite_totem").getComponents()),"refund comes from actual source loot table");require(p.hasEffect(MobEffects.REGENERATION)&&p.hasEffect(MobEffects.FIRE_RESISTANCE)&&p.hasEffect(MobEffects.ABSORPTION),"native death protection effects remain");
            var advancement=f.base.game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/totems/infinite"));require(advancement!=null&&p.getAdvancements().getOrStartProgress(advancement).isDone(),"real source used_totem advancement");}
        reset(f);var p=f.base.owner;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));p.setItemInHand(InteractionHand.OFF_HAND,loot(f,"infinite_totem"));p.setHealth(20);p.setInvulnerableTime(0);p.damageCooldownTime=0;require(p.hurtServer(f.level,f.level.damageSources().generic(),1000)&&p.getMainHandItem().isEmpty()&&!p.getOffhandItem().isEmpty()&&drops(f).isEmpty(),"native mainhand precedence does not refund unused offhand");
        reset(f);var invalid=loot(f,"infinite_totem");invalid.remove(DataComponents.CUSTOM_DATA);pop(f,invalid,InteractionHand.MAIN_HAND);require(drops(f).isEmpty(),"Infinity without both source flags never refunds");reset(f);
    }
    private static void outsideBuildHeight(AllGrimoireChecks.Fixture f){for(int y:new int[]{f.level.getMinY()-5,f.level.getMaxY()+5}){reset(f);var p=f.base.owner;p.setPos(f.at.x,y,f.at.z);pop(f,loot(f,"infinite_totem"),InteractionHand.MAIN_HAND);var returned=drops(f);require(returned.size()==1&&returned.getFirst().position().distanceToSqr(p.position())<1e-8,"native refund is not limited by terrain build height: "+y);}reset(f);}
    private static void copies(AllGrimoireChecks.Fixture f){for(int level:new int[]{2,7,300,Integer.MAX_VALUE}){
        reset(f);var service=f.base.game.items().totems();var s=loot(f,"infinite_totem");EnchantmentHelper.updateEnchantments(s,m->m.set(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY),level));
        s.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Native refund test"));var expected=s.copy();pop(f,s,InteractionHand.MAIN_HAND);
        if(level!=Integer.MAX_VALUE)for(int n=0;n<100&&service.pendingRefunds()>0;n++)service.refundsTick();
        var entities=drops(f);require((long)entities.size()+service.pendingRefunds()==level,"full positive integer refund count retained without overflow: "+level);
        for(var item:entities)require(ItemStack.matches(item.getItem(),expected)&&service.infinity(item.getItem(),f.level)==level,"each refunded clone retains actual level/name/components "+level);
        if(level==Integer.MAX_VALUE)require(entities.size()<=64&&service.pendingRefunds()>2_000_000_000L,"IntMax does not allocate an unbounded damage-callback burst");
    }reset(f);}
    private static void passive(AllGrimoireChecks.Fixture f){reset(f);var p=f.base.owner;var service=f.base.game.items().totems();f.beta.setPos(f.at.add(0,0,8));p.setHealth(100);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"devil_totem"));
        service.heldPulse(p);require(service.actors()==4&&p.getHealth()==88,"four hostile skeletons per enemy and native12 self magic");
        var spawned=service.snapshot();for(var mob:spawned){require(mob.getHealth()==20&&mob.getMainHandItem().is(Items.STONE_SWORD)&&mob.getTeam()==null&&LbcIndirectKills.playerSource(mob)==p,"vanilla finalized hostile skeleton and causal author");mob.setNoAi(true);mob.setNoGravity(true);}
        var positions=new HashSet<Vec3>();for(var m:spawned)positions.add(m.position());require(positions.equals(Set.of(f.beta.position().add(1,0,0),f.beta.position().add(-1,0,0),f.beta.position().add(0,0,1),f.beta.position().add(0,0,-1))),"source cardinal formation about the enemy, not the owner");
        var mob=spawned.getFirst();p.setInvulnerableTime(0);p.damageCooldownTime=0;p.setPos(mob.position().add(1,0,0));float hp=p.getHealth();require(mob.canAttack(p)&&mob.doHurtTarget(f.level,p)&&p.getHealth()<hp,"WILD hostile skeleton may really attack its owner");mob.setTarget(p);mob.setTarget(null);require(mob.getTarget()==null,"native AI can clear a previous target");
        mob.entityTags().removeIf(t->t.startsWith("skywars2.devil_totem_skeleton.owner."));mob.entityTags().add("skywars2.devil_totem_skeleton.owner."+f.beta.getUUID());require(service.owner(mob)==f.beta,"entity owner survives independently of transient actor map");
        service.unloaded(mob);service.loaded(mob);require(service.snapshot().contains(mob)&&service.owner(mob)==f.beta,"native unload/reload retains identity and author");
        reset(f);f.beta.setPos(f.at.add(0,0,8));p.setHealth(100);p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"devil_totem"));p.setItemInHand(InteractionHand.OFF_HAND,loot(f,"devil_totem"));p.setInvulnerableTime(0);p.damageCooldownTime=0;service.heldPulse(p);require(service.actors()==8&&p.getHealth()==88,"both hands spawn independently; ordinary self-damage cooldown remains");
        reset(f);p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"devil_totem"));p.setHealth(100);service.heldPulse(p);require(service.actors()==0&&p.getHealth()==100,"no enemy, no summons or self damage");
        f.beta.setPos(f.at.add(0,0,8));p.entityTags().add("nomagic_active");service.heldPulse(p);require(service.actors()==4,"source passive ignores antimagic");p.entityTags().remove("nomagic_active");reset(f);
    }
    private static void burst(AllGrimoireChecks.Fixture f){reset(f);var p=f.base.owner;var g=f.base.game;var service=g.items().totems();var victim=cow(f,f.at.add(4,0,0));var protectedCow=cow(f,f.at.add(-4,0,0));protectedCow.entityTags().add("sin_greed_active");var role=cow(f,f.at.add(0,0,-4));role.entityTags().add("strict_map_object");
        var undead=new WitherSkeleton(EntityTypes.WITHER_SKELETON,f.level);undead.setNoAi(true);undead.setNoGravity(true);undead.setPos(f.at.add(0,0,4));undead.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);undead.setHealth(50);require(f.level.addFreshEntity(undead),"undead target");
        pop(f,loot(f,"devil_totem"),InteractionHand.OFF_HAND);require(service.pendingDeaths()==1&&g.items().mana().hudValue(p,"death_timer")==5,"native Devil activation records five two-tick units");
        require(victim.getHealth()==88&&victim.getKillCredit()==p&&victim.getEffect(MobEffects.WITHER)!=null&&victim.getEffect(MobEffects.WITHER).getAmplifier()==1&&victim.getEffect(MobEffects.WITHER).getDuration()==1_200_000,"native12 magic, WitherII60000seconds and causal author");require(undead.getHealth()==58,"native Instant DamageII healing for undead preserved");require(protectedCow.getHealth()==100&&!protectedCow.hasEffect(MobEffects.WITHER)&&role.getHealth()==100&&!role.hasEffect(MobEffects.WITHER),"Greed and service roles protected");
        require(!p.hasEffect(MobEffects.WITHER)&&!p.hasEffect(MobEffects.RESISTANCE)&&p.hasEffect(MobEffects.FIRE_RESISTANCE),"native post-criterion restoration clears self source potions, preserving vanilla protection");require(f.base.recording.packets.stream().filter(k->k instanceof ClientboundSoundPacket).count()>=1,"source Wither death sound emitted");
        var center=BlockPos.containing(f.at);for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))require(f.level.getBlockState(pos).is(Blocks.LAVA),"immediate source center lava");
        g.items().timeStop().begin(f.level,40);for(int i=0;i<20;i++)service.debtsTick();require(g.items().mana().hudValue(p,"death_timer")==5,"dimension stop pauses lethal countdown");g.items().timeStop().end(f.level);
        g.items().mana().score(p,"void_resistance").set(5);for(int i=0;i<10;i++)service.debtsTick();require(p.isAlive()&&service.pendingDeaths()==0,"source void resistance exempts final death");
        for(int i=0;i<100&&service.work()>0;i++)g.items().stonekivi().tick();require(service.work()==0,"CPU-budgeted source lava completes");int count=0;for(var pos:BlockPos.betweenClosed(center.offset(-15,-15,-15),center.offset(14,14,14)))if(f.level.getBlockState(pos).is(Blocks.LAVA)&&f.level.getBlockState(pos).getValue(LiquidBlock.LEVEL)==8)count++;require(count>3000&&count<5000,"source30-cube15-percent falling-lava density; actual="+count);reset(f);
    }
    private static void fatal(AllGrimoireChecks.Fixture f){reset(f);var p=f.base.owner;var g=f.base.game;var service=g.items().totems();int lives=g.lives(p.getUUID());int kills=g.players().get(f.beta.getUUID()).kills;
        int uses=p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
        var s=loot(f,"devil_totem");p.setItemInHand(InteractionHand.MAIN_HAND,s);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));p.setHealth(20);p.setInvulnerableTime(0);p.damageCooldownTime=0;
        require(p.hurtServer(f.level,f.level.damageSources().playerAttack(f.beta),1000)&&p.isAlive()&&s.isEmpty(),"real hostile native lethal hit activates Devil Totem");
        for(int i=0;i<9;i++)service.debtsTick();require(p.isAlive()&&g.items().mana().hudValue(p,"death_timer")==1,"no early death in first nine ticks");service.debtsTick();require(!p.isAlive()&&g.lives(p.getUUID())==lives-1&&g.players().get(f.beta.getUUID()).kills==kills+1&&p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING))==uses+1,"tenth tick uses native bypass-totem kill, retaining original attacker credit and match life loss");
    }
    private static void wildAndBroken(AllGrimoireChecks.Fixture f){reset(f);var g=f.base.game;var p=f.base.owner;var board=g.server().getScoreboard();var old=p.getTeam();var other=f.beta.getTeam();var team=board.addPlayerTeam("codex_totem_wild");team.setAllowFriendlyFire(false);
        try{board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(f.beta.getScoreboardName(),team);f.beta.setPos(f.at.add(5,0,0));f.beta.setHealth(100);f.beta.setInvulnerableTime(0);f.beta.damageCooldownTime=0;f.beta.getInventory().clearContent();f.beta.removeAllEffects();
            p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"devil_totem"));p.setHealth(100);g.items().totems().heldPulse(p);require(g.items().totems().actors()==0&&p.getHealth()==100,"allied players are excluded from passive spawn anchors");
            pop(f,loot(f,"devil_totem"),InteractionHand.MAIN_HAND);require(f.beta.getHealth()==88&&f.beta.hasEffect(MobEffects.WITHER),"WILD death burst affects allied player despite disabled friendly fire");
            f.beta.setInvulnerableTime(0);f.beta.damageCooldownTime=0;MobEffects.WITHER.value().applyEffectTick(f.level,f.beta,1);require(f.beta.getHealth()<88,"real native WILD Wither tick retains allied-player damage; HP="+f.beta.getHealth());
            p.entityTags().add("sin_greed_active");for(int n=0;n<10;n++)g.items().totems().debtsTick();require(p.isAlive()&&g.items().totems().pendingDeaths()==0,"active Greed exempts actual final death");p.entityTags().remove("sin_greed_active");
        }finally{p.entityTags().remove("sin_greed_active");board.removePlayerFromTeam(p.getScoreboardName());board.removePlayerFromTeam(f.beta.getScoreboardName());board.removePlayerTeam(team);if(old!=null)board.addPlayerToTeam(p.getScoreboardName(),old);if(other!=null)board.addPlayerToTeam(f.beta.getScoreboardName(),other);reset(f);}
        var broken=new ItemStack(Items.TOTEM_OF_UNDYING);var flag=new net.minecraft.nbt.CompoundTag();flag.putInt("broken_totem",1);broken.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(flag));pop(f,broken,InteractionHand.OFF_HAND);require(g.items().curseTicks(p)>0,"existing broken-totem callback still runs from real native death protection");g.items().clear(p);reset(f);
    }
    public static void run(GameController g){net.minecraft.server.level.ServerPlayer previous=null;try(var f=new AllGrimoireChecks.Fixture(g)){expand(f);try{presentation(f);infinity(f);outsideBuildHeight(f);copies(f);passive(f);burst(f);wildAndBroken(f);fatal(f);previous=f.base.owner;}finally{g.items().timeStop().end(f.level);g.items().totems().stop();f.restoreWorld=true;}}
        if(previous!=null){var fresh=g.server().getPlayerList().respawn(previous,false,Entity.RemovalReason.KILLED);fresh.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());require(fresh!=previous&&fresh.connection.player==fresh&&fresh.isAlive()&&g.canUseGameplayItems(fresh),"real native replacement player and respawn callback restore gameplay after death");var at=fresh.position();var moved=at.add(.25,0,.25);require(fresh.teleportTo(fresh.level(),moved.x,moved.y,moved.z,Set.of(),fresh.getYRot(),fresh.getXRot(),true)&&fresh.position().distanceToSqr(moved)<1e-8,"post-respawn native teleport moves replacement player rather than retired body");fresh.teleportTo(fresh.level(),at.x,at.y,at.z,Set.of(),fresh.getYRot(),fresh.getXRot(),true);}
        SkyWars2.LOGGER.info("[check] PASS totems");}
    private static AllGrimoireChecks.Fixture retained;private static List<WitherSkeleton> retainedActors;
    public static void stopProbe(GameController g){require(retained==null,"exclusive stop fixture");var f=new AllGrimoireChecks.Fixture(g,true);expand(f);try{reset(f);f.beta.setPos(f.at.add(0,0,8));var p=f.base.owner;p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"devil_totem"));p.setHealth(100);g.items().totems().heldPulse(p);require(g.items().totems().actors()==4,"actual hostile actors retained");retainedActors=g.items().totems().snapshot();pop(f,loot(f,"devil_totem"),InteractionHand.MAIN_HAND);require(g.items().totems().work()>0&&g.items().totems().pendingDeaths()==1,"unfinished native lava and debt retained for actual stop");var high=loot(f,"infinite_totem");EnchantmentHelper.updateEnchantments(high,m->m.set(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY),Integer.MAX_VALUE));pop(f,high,InteractionHand.OFF_HAND);require(g.items().totems().pendingRefunds()>2_000_000_000L,"IntMax refund remains as counted work rather than mass entity allocation");retained=f;}catch(Throwable e){g.items().totems().stop();f.close();throw e;}SkyWars2.LOGGER.info("[check] PASS totems-stop-probe");}
    public static void cleanup(GameController g){require(retained!=null,"actual-stop fixture");try{require(g.items().totems().actors()==0&&g.items().totems().work()==0&&g.items().totems().pendingDeaths()==0&&g.items().totems().pendingRefunds()==0&&retainedActors.stream().allMatch(Entity::isRemoved),"actual stop removes actors, source hazards, refund debt and death debt");}finally{retained.close();retained=null;retainedActors=null;}SkyWars2.LOGGER.info("[check] PASS totems-cleanup");}
}
