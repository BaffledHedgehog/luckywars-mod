package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
            for(int id=1;id<=13;id++){profile.skill=id;require(game.events().selectedSkill(p)==(id==11||id==13?0:id),"actual ordinary skill eligibility "+id);}
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
    private static int count(net.minecraft.server.level.ServerPlayer p,net.minecraft.world.item.Item item){int result=0;for(int slot=0;slot<p.getInventory().getContainerSize();slot++){var stack=p.getInventory().getItem(slot);if(stack.is(item))result+=stack.getCount();}return result;}
    public static void exit(GameController game){
        require(game.inLobby()&&!game.balanceLegacy(),"normal match exit clears legacy overlay");
        require(legacyArrow!=null&&legacyArrow.is(DamageTypeTags.BYPASSES_COOLDOWN),"same native DamageSource returns to ordinary server tags after exit");
        legacyArrow=null;
        SkyWars2.LOGGER.info("[check] PASS true-legacy-exit (real stop clears overlay and restores dynamic ordinary damage policy)");
    }
}
