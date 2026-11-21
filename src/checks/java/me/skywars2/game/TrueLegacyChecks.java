package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.skywars2.SkyWars2;
import me.skywars2.game.gui.LbcCosmetics;
import me.skywars2.game.items.gadgets.LbcGadgetItems;
import me.skywars2.game.items.gadgets.LbcTeleportGadgets;
import me.skywars2.game.items.loot.LbcLuckyBlocks;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Runs after the actual Supernova -> Balance transition, not a synthetic legacy flag. */
public final class TrueLegacyChecks {
    private static DamageSource legacyArrow;
    private TrueLegacyChecks() {}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("True legacy: "+why);}
    private static List<ClientboundSetPlayerTeamPacket> teams(Packet<?> packet){
        var result=new ArrayList<ClientboundSetPlayerTeamPacket>();
        if(packet instanceof ClientboundSetPlayerTeamPacket team)result.add(team);
        if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())result.addAll(teams(child));
        return result;
    }
    public static void run(GameController game){
        require(game.playing()&&game.balanceLegacy(),"real completed Balance legacy transition");
        var p=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var saved=new GameChecks.WandThirdPlayerState(p,game.items().mana());
        var profile=game.players().get(p.getUUID());
        int kit=profile.kit,skill=profile.skill,superKit=profile.lbcKit,superSkill=profile.lbcSkill;
        int suffix=profile.suffix,win=profile.winEffect,kill=profile.killEffect;
        boolean kits=game.config().vanillaKits,skills=game.config().vanillaSkills,ranked=game.config().ranked;
        var team=p.getTeam();boolean ownTeam=team==null;
        if(ownTeam){team=game.server().getScoreboard().addPlayerTeam("true_legacy_check");game.server().getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);}
        var prefix=team.getPlayerPrefix();var postfix=team.getPlayerSuffix();
        var target=new Cow(EntityTypes.COW,p.level());target.setNoAi(true);target.setNoGravity(true);
        target.setPos(p.getX()+12,p.getY()+8,p.getZ());
        try(var client=new RegenerationBeltChecks.ScheduledClient(game,p)){
            game.config().vanillaKits=true;game.config().vanillaSkills=true;game.config().ranked=true;
            p.getInventory().clearContent();profile.kit=21;game.events().giveKit(p);
            require(count(p,Items.OAK_SLAB)==1&&count(p,Items.COBBLESTONE_SLAB)==16,"actual selected Half Slabs kit is granted");
            p.getInventory().clearContent();profile.kit=22;game.events().giveKit(p);
            require(p.getInventory().isEmpty(),"post-Half-Slabs kit stays inactive without changing selection");
            for(int id=1;id<=13;id++){profile.skill=id;require(game.events().selectedSkill(p)==(id<=11?id:0),"actual ordinary skill eligibility "+id);}
            profile.lbcKit=6;profile.lbcSkill=2;game.lbc().apply(p);
            require(p.getInventory().isEmpty()&&!game.lbc().skillsEnabled()&&!game.items().inventoryHazards().protectedInventory(p),"selected super kit/skill cannot leak into legacy");
            profile.suffix=1;profile.winEffect=1;profile.killEffect=1;
            require(game.items().cosmetics().choice(p,LbcCosmetics.Kind.SUFFIX)==0&&game.items().cosmetics().choice(p,LbcCosmetics.Kind.WIN)==0&&game.items().cosmetics().choice(p,LbcCosmetics.Kind.KILL)==0,"cosmetic selections are ignored without being erased");
            team.setPlayerPrefix(Component.literal("hidden_prefix"));team.setPlayerSuffix(Component.literal("hidden_postfix"));
            game.items().cosmetics().refreshSuffixes(List.of(p));client.packets().clear();
            p.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team,false));
            var packets=client.packets().stream().flatMap(packet->teams(packet).stream()).filter(packet->packet.getParameters().isPresent()).toList();
            require(!packets.isEmpty()&&packets.stream().allMatch(packet->packet.getParameters().orElseThrow().playerPrefix().getString().isEmpty()&&packet.getParameters().orElseThrow().playerSuffix().getString().isEmpty()),"actual projected team packets hide prefix/postfix");
            require(p.getDisplayName().getString().equals(p.getName().getString()),"actual chat/TAB display name has no decoration");
            game.attributeHud().tick();require(!game.attributeHud().shown(p)&&game.items().hud(p).getString().isEmpty(),"both real HUD owners are disabled");
            require(p.level().addFreshEntity(target),"native scoped combat target");
            var arrow=new Arrow(EntityTypes.ARROW,p.level());arrow.setOwner(p);
            var source=new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),arrow,p);
            legacyArrow=source;
            require(!source.is(DamageTypeTags.BYPASSES_COOLDOWN)&&source.is(DamageTypeTags.IS_PROJECTILE),"actual arrow uses vanilla tag membership");
            require(Math.abs(source.type().exhaustion()-.1f)<.00001f,"actual vanilla arrow definition");
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);
            target.setHealth(20);target.setInvulnerableTime(0);target.damageCooldownTime=0;
            require(target.hurtServer(p.level(),source,3)&&target.getHealth()==17,"first real native arrow damage accepted");
            require(!target.hurtServer(p.level(),source,3)&&target.getHealth()==17,"second same-tick arrow obeys native hurt cooldown");
            require(Math.abs(CombatRules.getDamageAfterAbsorb(target,10,source,100,0)-2)<.0001,"real CombatRules uses vanilla armor cap");
            legacyItemProjection(p);
            classicLuckyBlock(game,p);
            legacySwap(game,p,client);
            TrueLegacyVanillaChecks.run(game,p,target);
        }finally{
            target.discard();saved.restore(game);
            profile.kit=kit;profile.skill=skill;profile.lbcKit=superKit;profile.lbcSkill=superSkill;
            profile.suffix=suffix;profile.winEffect=win;profile.killEffect=kill;
            game.config().vanillaKits=kits;game.config().vanillaSkills=skills;game.config().ranked=ranked;
            team.setPlayerPrefix(prefix);team.setPlayerSuffix(postfix);
            if(ownTeam)game.server().getScoreboard().removePlayerTeam(team);
            game.items().cosmetics().refreshSuffixes(List.of(p));
        }
        SkyWars2.LOGGER.info("[check] PASS true-legacy-rules (real Balance, selected vanilla kit/skills, disabled supers/cosmetics/both HUDs, actual clean team packets, vanilla arrow tags/definition/native cooldown and armor)");
    }
    private static void legacyItemProjection(net.minecraft.server.level.ServerPlayer player){
        var source=new ItemStack(Items.DIAMOND_SWORD);
        source.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_q"));
        var context=player.connection.getPacketContext();
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(player.connection,()->{
            var projected=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(source,
                    net.minecraft.world.item.TooltipFlag.NORMAL,context,player.registryAccess());
            require(!projected.has(DataComponents.TOOLTIP_STYLE)&&source.has(DataComponents.TOOLTIP_STYLE),
                    "stored stacks display a vanilla frame during true legacy and keep their authoritative components");
        });
    }
    private static void classicLuckyBlock(GameController game,net.minecraft.server.level.ServerPlayer player){
        var level=player.level();var pos=BlockPos.containing(player.position()).above(5);
        var previous=game.maps().snapshot(level,pos);
        var state=LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState();
        var block=(eu.pb4.polymer.core.api.block.PolymerBlock)state.getBlock();
        try{
            require(level.setBlock(pos,state,3)&&block.getPolymerBlockState(state,null).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.YELLOW)),
                    "actual native lucky block projects yellow glass in true legacy");
            var attachment=eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment.get(level,pos);
            require(attachment!=null&&!attachment.shouldTick(),"head is a static block-bound virtual visual");
            var head=attachment.holder().getElements().stream()
                    .filter(eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement.class::isInstance)
                    .map(eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement.class::cast).findFirst().orElseThrow().getItem();
            require(head.is(Items.PLAYER_HEAD)&&head.has(DataComponents.PROFILE),"real skull renderer receives the classic skin profile");
            require(level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)
                    &&eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment.get(level,pos)==null,
                    "removing the lucky block also removes its virtual visual");
        }finally{game.maps().restoreSnapshot(level,pos,previous);}
    }

    private static void legacySwap(GameController game,net.minecraft.server.level.ServerPlayer player,
            RegenerationBeltChecks.ScheduledClient client){
        var other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
        var level=player.level();
        var origin=player.position();
        var target=other.position();
        var floor=BlockPos.containing(origin.x,origin.y-1.5,origin.z);
        var feet=BlockPos.containing(origin);
        var tiles=new java.util.LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var pos=floor.offset(x,0,z);
            require(level.hasChunkAt(pos),"legacy swap pad is in the loaded arena");
            tiles.put(pos,game.maps().snapshot(level,pos));
            for(int y=0;y<3;y++){
                pos=feet.offset(x,y,z);
                tiles.put(pos,game.maps().snapshot(level,pos));
            }
        }
        var loot=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                Registries.LOOT_TABLE,Identifier.parse("lbc:legacy_luckyblock")));
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin)
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,player)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        ItemStack oldSwap=ItemStack.EMPTY;
        for(long seed=1;seed<=1024;seed++){
            for(var stack:loot.getRandomItems(params,seed)){
                require(!stack.has(DataComponents.TOOLTIP_STYLE),"true-legacy rolls strip the shared tooltip style");
                if(stack.is(LbcGadgetItems.LEGACY_SWAPPER))oldSwap=stack;
            }
            if(!oldSwap.isEmpty())break;
        }
        require(!oldSwap.isEmpty(),"real legacy loot can issue its separate swapper");
        var edgeSwap=oldSwap.copy();
        float yaw=player.getYRot(),pitch=player.getXRot(),otherYaw=other.getYRot(),otherPitch=other.getXRot();
        ItemStack held=player.getMainHandItem();
        try (var platformChest=new ContainerReplacementChecks(game,level,floor.offset(1,0,-1),"legacy swap platform");
             var clearanceChest=new ContainerReplacementChecks(game,level,feet.offset(1,1,1),"legacy swap arrival clearance")) {
            player.setItemInHand(InteractionHand.MAIN_HAND,oldSwap);
            client.packets().clear();
            require(net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker()
                    .interact(player,level,InteractionHand.MAIN_HAND).consumesAction(),"actual legacy swap consumes a single use");
            require(player.position().distanceToSqr(target)<.0001&&other.position().distanceToSqr(origin)<.0001&&oldSwap.isEmpty(),
                    "legacy swap exchanges the players in the same tick and spends one item");
            require(client.packets().stream().noneMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound
                    &&sound.getSound().value().location().equals(LbcTeleportGadgets.CHARGE_SOUND)),"instant legacy swap never starts charge audio");
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                require(level.getBlockState(floor.offset(x,0,z)).is(Blocks.OBSIDIAN),"legacy swap leaves the full 3x3 obsidian platform");
            require(level.getBlockState(feet.offset(1,1,1)).isAir(),"legacy swap opens the arrival volume");
            platformChest.requireReleased();
            clearanceChest.requireReleased();
        } finally {
            tiles.forEach((pos,image)->game.maps().restoreSnapshot(level,pos,image));
            player.teleportTo(level,origin.x,origin.y,origin.z,java.util.Set.of(),yaw,pitch,true);
            other.teleportTo(level,target.x,target.y,target.z,java.util.Set.of(),otherYaw,otherPitch,true);
            player.setItemInHand(InteractionHand.MAIN_HAND,held);
        }
        var protectedPlayers=new ArrayList<ServerPlayer>();
        for(var candidate:level.players())
            if(candidate!=player&&candidate.entityTags().add("strict_map_object"))protectedPlayers.add(candidate);
        try {
            legacyEdgeSwap(game,player,client,edgeSwap,false);
            legacyEdgeSwap(game,player,client,edgeSwap,true);
        } finally {
            for(var candidate:protectedPlayers)candidate.entityTags().remove("strict_map_object");
        }
    }
    private static void legacyEdgeSwap(GameController game,ServerPlayer player,
            RegenerationBeltChecks.ScheduledClient client,ItemStack source,boolean high){
        var level=player.level();var origin=player.position();
        float yaw=player.getYRot(),pitch=player.getXRot();
        var held=player.getMainHandItem();
        var stat=Stats.ITEM_USED.get(source.getItem());int beforeUse=player.getStats().getValue(stat);
        var platform=BlockPos.containing(origin.x,high?314:1,origin.z);
        var tiles=new java.util.LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var column=platform.offset(x,0,z);
            require(level.hasChunkAt(column),"edge swap pad is in the loaded arena");
            for(int y=0;y<=(high?1:3);y++){
                var cell=column.above(y);
                tiles.put(cell,game.maps().snapshot(level,cell));
            }
        }
        var provenance=game.items().indirectKills().captureTerrain(level,tiles.keySet());
        try {
            var sentinel=platform.above().east();
            level.setBlock(sentinel,Blocks.STONE.defaultBlockState(),2);
            player.teleportTo(level,origin.x,high?340:-10,origin.z,java.util.Set.of(),37,-16,true);
            client.acknowledge();
            var stack=source.copyWithCount(2);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            client.packets().clear();
            require(player.gameMode.useItem(player,level,stack,InteractionHand.MAIN_HAND).consumesAction()
                    &&stack.getCount()==1&&player.getStats().getValue(stat)==beforeUse+1,
                    "an instant edge swap without a target spends exactly one item and use stat");
            var expected=new Vec3(origin.x,high?317:3,origin.z);
            require(player.position().distanceToSqr(expected)<.0001&&player.getYRot()==37&&player.getXRot()==-16,
                    "edge swap without a target immediately rescues the caster at "+expected+" with their own view; actual "+player.position());
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                var column=platform.offset(x,0,z);
                require(level.getBlockState(column).is(Blocks.OBSIDIAN),"edge swap leaves the 3x3 obsidian platform");
                if(!high)for(int y=1;y<=3;y++)
                    require(level.getBlockState(column.above(y)).isAir(),"low edge swap clears Y2 through Y4");
            }
            if(high)require(level.getBlockState(sentinel).is(Blocks.STONE),"high edge swap does not clear above its platform");
            require(client.packets().stream().anyMatch(packet->packet instanceof ClientboundLevelParticlesPacket p
                    &&p.particle().getType()==ParticleTypes.PORTAL&&p.x()==origin.x&&p.y()==(high?314:4)&&p.z()==origin.z),
                    "edge swap emits its native portal burst at the original authored height");
            require(game.items().teleportGadgets().chargingCount()==0,"edge swap does not start a modern charge");
        } finally {
            tiles.forEach((pos,image)->game.maps().restoreSnapshot(level,pos,image));
            game.items().indirectKills().restoreTerrain(provenance);
            player.teleportTo(level,origin.x,origin.y,origin.z,java.util.Set.of(),yaw,pitch,true);
            client.acknowledge();
            player.setItemInHand(InteractionHand.MAIN_HAND,held);
            player.getStats().setValue(player,stat,beforeUse);
        }
    }
    private static int count(net.minecraft.server.level.ServerPlayer p,net.minecraft.world.item.Item item){int result=0;for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var stack=p.getInventory().getItem(slot);if(stack.is(item))result+=stack.getCount();}return result;}
    public static void exit(GameController game){
        require(game.inLobby()&&!game.balanceLegacy(),"normal match exit clears legacy overlay");
        TrueLegacyVanillaChecks.exit(game);
        require(legacyArrow!=null&&legacyArrow.is(DamageTypeTags.BYPASSES_COOLDOWN),"same native DamageSource returns to ordinary server tags after exit");
        legacyArrow=null;
        SkyWars2.LOGGER.info("[check] PASS true-legacy-exit (real stop clears overlay and restores dynamic ordinary damage policy)");
    }
}
