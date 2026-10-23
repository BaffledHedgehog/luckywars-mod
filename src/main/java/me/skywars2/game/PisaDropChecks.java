package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded synchronous native fixtures; no client rendering or disk unload claim. */
public final class PisaDropChecks {
    private PisaDropChecks() { }
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<AbstractBoat> stopBoats=List.of();
    private static void require(boolean value,String text) {if(!value)throw new IllegalStateException("Pisa drops: "+text);}
    private static ItemEntity token(ServerLevel level,Vec3 at,int count,ServerPlayer author) {
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(LbcPisaDropItem.Kind.NORMAL.item(),count));
        item.setThrower(author);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static void translated(net.minecraft.network.chat.Component component,String key) {
        require(component!=null && component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text && text.getKey().equals(key),"translated "+key);
    }
    private static void relic(ItemStack stack,boolean upgraded) {
        require(stack.is(Items.DIRT) && stack.getCount()==1,"one actual dirt relic");
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        require(data.size()==2 && data.getIntOr("craftusable",0)==1 && data.getIntOr("govnoq",0)==1,"exact relic donor flags");
        require(Identifier.parse(upgraded?"lbc:tier_a":"lbc:tier_b").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source relic tier");
        translated(stack.get(DataComponents.CUSTOM_NAME),"bullshit");require(!stack.get(DataComponents.CUSTOM_NAME).getStyle().isItalic(),"nonitalic relic name");
        var amounts=Map.of("knockback_resistance",upgraded?2d:1d,"attack_damage",upgraded?10d:5d,"armor",upgraded?20d:10d,"armor_toughness",upgraded?20d:10d);
        var ids=Map.of("knockback_resistance",upgraded?"minecraft:1239251353566":"minecraft:kfnfousdangdgrf","attack_damage","minecraft:base_attack_damage","armor",upgraded?"minecraft:6834242597":"minecraft:68942427","armor_toughness",upgraded?"minecraft:3871081226":"minecraft:387081226");
        var modifiers=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);require(modifiers!=null && modifiers.modifiers().size()==4,"four real relic modifiers");
        var seen=new HashSet<String>();for(var entry:modifiers.modifiers()) {
            String type=entry.attribute().unwrapKey().orElseThrow().identifier().getPath();
            require(seen.add(type) && amounts.containsKey(type) && entry.modifier().amount()==amounts.get(type) && entry.modifier().id().toString().equals(ids.get(type)) && entry.slot()==EquipmentSlotGroup.MAINHAND && entry.modifier().operation()==AttributeModifier.Operation.ADD_VALUE,"exact relic attribute ID/amount/operation/slot "+type);
        }
        var ench=stack.get(DataComponents.ENCHANTMENTS);require(ench!=null && ench.entrySet().size()==4,"four native source enchantments");
        var levels=Map.of("smite",upgraded?20:10,"bane_of_arthropods",upgraded?20:10,"knockback",upgraded?2:1,"looting",upgraded?20:10);
        for(var entry:ench.entrySet())require(levels.containsKey(entry.getKey().unwrapKey().orElseThrow().identifier().getPath()) && levels.get(entry.getKey().unwrapKey().orElseThrow().identifier().getPath())==entry.getIntValue(),"exact source enchantment level");
    }
    private static AbstractBoat tower(GameController game,Vec3 at,boolean upgraded) {
        var boats=game.items().pisaDrops().snapshot();require(boats.size()==62 && game.items().pisaDrops().trackedCount()==62,"61 oak boats and one chest boat");
        double length=Math.hypot(at.x,at.z);var outward=length<1e-10?Vec3.ZERO:new Vec3(at.x/length,0,at.z/length);
        var ordered=new ArrayList<>(boats);ordered.sort(Comparator.comparingDouble(Entity::getY));
        for(int n=0;n<62;n++) {
            var boat=ordered.get(n);var expected=at.add(0,20+n,0).add(outward.scale(.4*n));
            // Native local coordinates use the quantized sine table; the independent
            // analytical direction allows its <.005 block error at the 24.4 tip.
            require(boat.position().distanceTo(expected)<.005 && boat.getYRot()==0 && !boat.isNoGravity() && boat.entityTags().contains(LbcPisaDrops.ACTOR) && game.items().currentEpoch(boat),"source fractional native position/yaw/gravity/epoch n="+n);
            require(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boat.getType()).toString().equals(n==61?"minecraft:oak_chest_boat":"minecraft:oak_boat"),"exact native boat type "+n);
            if(n==0 || n==61){translated(boat.getCustomName(),"pizansk");require(boat.isCustomNameVisible(),"source visible first/chest name");}
            else require(boat.getCustomName()==null,"interior boats unnamed");
        }
        var chest=ordered.getLast();require(chest instanceof Container,"native chest inventory");var inventory=(Container)chest;
        require(inventory.getContainerSize()==27,"native chest slots");for(int slot=0;slot<27;slot++)if(slot!=14)require(inventory.getItem(slot).isEmpty(),"only source slot14 filled");
        relic(inventory.getItem(14),upgraded);return chest;
    }
    private static void nativeReward(GameController game,ServerPlayer player,AbstractBoat boat) {
        var level=game.maps().level();var inventory=(Container)boat;var reward=inventory.getItem(14).copy();
        player.teleportTo(level,boat.getX(),boat.getY(),boat.getZ()+2,Set.of(),0,0,true);player.setShiftKeyDown(true);
        try {require(boat.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction() && player.containerMenu!=player.inventoryMenu,"Survival native chest interaction opens menu");}
        finally {player.setShiftKeyDown(false);}
        require(ItemStack.matches(player.containerMenu.getSlot(14).getItem(),reward),"native menu exposes real slot14");
        GameChecks.inventoryClick(player,14,0,ContainerInput.PICKUP);
        require(inventory.getItem(14).isEmpty() && ItemStack.matches(player.containerMenu.getCarried(),reward),"native chest take removes exactly one real relic");
        player.containerMenu.setCarried(ItemStack.EMPTY);player.closeContainer();
        var custom=new LinkedHashMap<net.minecraft.stats.Stat<Identifier>,Integer>();for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);custom.put(stat,player.getStats().getValue(stat));}
        try {
            player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.doTick();
            double damage=player.getAttributeValue(Attributes.ATTACK_DAMAGE),armor=player.getAttributeValue(Attributes.ARMOR),toughness=player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            player.setItemInHand(InteractionHand.MAIN_HAND,reward.copy());player.doTick();
            require(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE)-damage-5)<1e-6 && Math.abs(player.getAttributeValue(Attributes.ARMOR)-armor-10)<1e-6 && Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)-toughness-10)<1e-6,"actual native held relic adds damage/armor/toughness");
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.doTick();
            var menu=player.inventoryMenu;var grid=menu.getCraftSlots();grid.clearContent();menu.setCarried(ItemStack.EMPTY);
            grid.setItem(0,new ItemStack(LbcElectro.CYBERKATANA));grid.setItem(3,reward.copy());
            require(menu.getResultSlot().getItem().is(LbcElectro.SHIT_KATANA),"actual Pisa relic accepted by existing govnoq crafting consumer");
            GameChecks.inventoryClick(player,0,0,ContainerInput.PICKUP);
            require(menu.getCarried().is(LbcElectro.SHIT_KATANA) && grid.getItem(0).isEmpty() && grid.getItem(3).isEmpty(),"native craft result consumes Pisa donor and katana once");
            grid.clearContent();menu.setCarried(ItemStack.EMPTY);
        } finally {custom.forEach((stat,value)->player.getStats().setValue(player,stat,value));}
    }
    public static void run(GameController game) {
        var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.playing() && alpha!=null && beta!=null,"two actual match participants");
        var savedA=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());var savedB=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
        var profileA=game.players().get(alpha.getUUID());var profileB=game.players().get(beta.getUUID());int skillA=profileA.lbcSkill,skillB=profileB.lbcSkill;boolean skills=game.config().lbcSkills;boolean shift=alpha.isShiftKeyDown();
        var at=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,190.7,Math.floor(alpha.getZ()/16)*16+8.81);var tokens=new ArrayList<ItemEntity>();
        var initial=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(initial::add);
        try {
            game.items().specialDrops().stop();game.items().pisaDrops().stop();
            // Fixture prepares chunks explicitly; service itself must never force them.
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk((int)Math.floor(at.x/16)+x,(int)Math.floor(at.z/16)+z);
            for(var player:List.of(alpha,beta)){player.closeContainer();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.removeAllEffects();}
            var prototype=new ItemStack(LbcPisaDropItem.Kind.NORMAL.item());var flags=prototype.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(flags.size()==2 && flags.getIntOr("pizanski",0)==1 && flags.getIntOr("drop",0)==1 && LbcPisaDropItem.matches(prototype,LbcPisaDropItem.Kind.NORMAL) && LbcPisaDropItem.kind(prototype)==LbcPisaDropItem.Kind.NORMAL,"exact normal token flags/recognition");
            require(!LbcPisaDropItem.matches(new ItemStack(Items.REPEATING_COMMAND_BLOCK)),"plain lookalike rejected");
            require(Identifier.parse("lbc:tier_b").equals(prototype.get(DataComponents.TOOLTIP_STYLE)) && Identifier.parse("lbc:drop_not_trap").equals(prototype.get(DataComponents.ITEM_MODEL)),"source token tier/model");
            translated(prototype.get(DataComponents.CUSTOM_NAME),"drop_pizanski");var name=prototype.get(DataComponents.CUSTOM_NAME);require(!name.getStyle().isItalic() && name.getStyle().getColor().getValue()==0xffff55,"yellow nonitalic token name");
            var lore=prototype.get(DataComponents.LORE).lines();require(lore.size()==1,"one token lore line");translated(lore.getFirst(),"trap_desc");require(!lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"white nonitalic token lore");
            var loot=game.lbc().rollTable(level,at,alpha,"lbc:ported/pisa_boats_drop");require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(prototype,loot.getFirst()),"real public acquisition leaf");
            int missingX=(int)Math.floor(at.x/16)+1024,missingZ=(int)Math.floor(at.z/16)+1024;
            require(level.getChunkSource().getChunkNow(missingX,missingZ)==null,"distant chunk fixture starts unloaded");
            var unloadedToken=new ItemEntity(level,missingX*16+8.37,at.y,missingZ*16+8.81,prototype.copyWithCount(2));
            int forced=level.getForceLoadedChunks().size();require(!game.items().pisaDrops().ready(unloadedToken) && !game.items().specialDrops().spawn(unloadedToken) && unloadedToken.getItem().getCount()==2 && level.getChunkSource().getChunkNow(missingX,missingZ)==null && level.getForceLoadedChunks().size()==forced,"unloaded preflight preserves debit and creates no chunk/ticket");
            game.config().lbcSkills=false;profileA.lbcSkill=9;profileB.lbcSkill=0;
            alpha.teleportTo(level,at.x,at.y+20,at.z,Set.of(),0,0,true);beta.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);
            var first=token(level,at,2,alpha);tokens.add(first);require(game.items().pisaDrops().ready(first),"native admission ready");
            require(game.items().specialDrops().spawn(first) && first.getItem().getCount()==1,"shared one-unit debit");require(!game.items().specialDrops().spawn(first) && first.getItem().getCount()==1,"same-tick replay rejected");
            var chest=tower(game,at,false);nativeReward(game,alpha,chest);game.items().pisaDrops().stop();require(game.items().pisaDrops().trackedCount()==0 && chest.isRemoved(),"quiet stop removes native actors");first.discard();
            game.config().lbcSkills=true;
            // Beta is nearest to the token, Alpha nearest to the source's Y+20 selector origin.
            alpha.teleportTo(level,at.x,at.y+20,at.z,Set.of(),0,0,true);beta.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);
            var upgraded=token(level,at,1,beta);tokens.add(upgraded);require(game.items().specialDrops().spawn(upgraded) && upgraded.isRemoved(),"last unit removed");tower(game,at,true);game.items().pisaDrops().stop();
            for(String role:List.of("spectator","rcdenyalways","sauvojen_tuntija","strict_map_object","magic_academy","transformator")) {
                alpha.entityTags().add(role);try {var t=token(level,at,1,beta);tokens.add(t);require(game.items().specialDrops().spawn(t),"role fixture admission");tower(game,at,false);}finally{alpha.entityTags().remove(role);game.items().pisaDrops().stop();}
            }
            alpha.setGameMode(GameType.SPECTATOR);var spectator=token(level,at,1,beta);tokens.add(spectator);require(game.items().specialDrops().spawn(spectator),"spectator fixture admission");tower(game,at,false);game.items().pisaDrops().stop();alpha.setGameMode(GameType.SURVIVAL);
            game.spectators().enter(alpha);try {alpha.setGameMode(GameType.SURVIVAL);alpha.teleportTo(level,at.x,at.y+20,at.z,Set.of(),0,0,true);var observer=token(level,at,1,beta);tokens.add(observer);require(game.items().specialDrops().spawn(observer),"observer fixture admission");tower(game,at,false);}finally{game.items().pisaDrops().stop();game.spectators().remove(alpha);alpha.setGameMode(GameType.SURVIVAL);alpha.setNoGravity(true);}
            var fake=token(level,at,2,alpha);tokens.add(fake);fake.makeFakeItem();require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount()==2,"native give visual cannot debit");fake.discard();
            var frozen=token(level,at,2,alpha);tokens.add(frozen);game.items().timeStop().begin(level,200);try {require(!game.items().pisaDrops().ready(frozen) && !game.items().specialDrops().spawn(frozen) && frozen.getItem().getCount()==2 && game.items().pisaDrops().trackedCount()==0,"time stop preserves pending debit and actors");}finally{game.items().timeStop().end(level);}
            require(game.items().specialDrops().spawn(frozen),"thaw admits once");var actors=game.items().pisaDrops().snapshot();var actor=actors.getFirst();game.items().pisaDrops().unloaded(actor);require(game.items().pisaDrops().trackedCount()==61 && !actor.isRemoved(),"unload callback detaches living native boat");game.items().pisaDrops().loaded(actor);require(game.items().pisaDrops().trackedCount()==62 && !actor.isRemoved(),"current epoch load reenrolls actor");
            double initialY=actor.getY();actor.tick();require(actor.getY()<initialY && actor.getDeltaMovement().y<0,"ordinary native boat tick applies falling physics");
            game.items().pisaDrops().stop();require(actors.stream().allMatch(Entity::isRemoved),"quiet cleanup removes all boats");frozen.discard();
            var staleActor=net.minecraft.world.entity.EntityTypes.OAK_CHEST_BOAT.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(staleActor!=null,"native stale actor fixture");staleActor.snapTo(at.x,at.y,at.z,0,0);staleActor.entityTags().add(LbcPisaDrops.ACTOR);staleActor.entityTags().add("skywars2.items.stale-fixture");staleActor.setItem(14,new ItemStack(Items.DIAMOND,3));require(level.addFreshEntity(staleActor),"stale actor native admission");game.items().pisaDrops().loaded(staleActor);require(staleActor.isRemoved() && game.items().pisaDrops().trackedCount()==0,"stale load callback quietly rejects old epoch chest contents");
            var stale=token(level,at,2,alpha);tokens.add(stale);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().pisaDrops().ready(stale) && !game.items().specialDrops().spawn(stale) && stale.getItem().getCount()==2,"stale token cannot debit");
            for(var entity:level.getAllEntities())if(!initial.contains(entity) && entity instanceof ItemEntity item && !tokens.contains(item) && item.position().distanceTo(at)<150)require(item.isRemoved(),"quiet lifecycle never ejects chest contents or boat items");
            SkyWars2.LOGGER.info("[check] PASS pisa-drops");
        } finally {
            alpha.closeContainer();game.items().timeStop().end(level);game.items().pisaDrops().stop();tokens.forEach(Entity::discard);game.items().specialDrops().stop();
            var added=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!initial.contains(entity) && (entity instanceof AbstractBoat || entity instanceof ItemEntity) && entity.position().distanceTo(at)<150)added.add(entity);for(var entity:added){if(entity instanceof AbstractBoat && entity instanceof Container container)container.clearContent();entity.discard();}
            game.config().lbcSkills=skills;profileA.lbcSkill=skillA;profileB.lbcSkill=skillB;alpha.setShiftKeyDown(shift);savedA.restore(game);savedB.restore(game);
        }
    }
    public static void stopProbe(GameController game) {
        require(game.playing(),"actual match stop probe");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var at=alpha.position().add(0,-30,0);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk((int)Math.floor(at.x/16)+x,(int)Math.floor(at.z/16)+z);
        var live=token(level,at,1,alpha);require(game.items().specialDrops().spawn(live),"live tower before actual stop");stopBoats=game.items().pisaDrops().snapshot();require(stopBoats.size()==62,"live actors before actual stop");
        stopEpoch=game.items().epoch();game.items().timeStop().begin(level,200);stopToken=token(level,at,3,alpha);require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"frozen pending token");SkyWars2.LOGGER.info("[check] PASS pisa-drops-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && stopToken!=null && stopToken.getItem().getCount()==3 && game.items().pisaDrops().trackedCount()==0 && stopBoats.stream().allMatch(Entity::isRemoved),"actual stop invalidates epoch, preserves pending debit and quietly removes boats");
        require(!game.items().specialDrops().spawn(stopToken),"stopped token replay rejected");stopToken.discard();stopToken=null;stopEpoch=null;stopBoats=List.of();SkyWars2.LOGGER.info("[check] PASS pisa-drops-cleanup");
    }
}
