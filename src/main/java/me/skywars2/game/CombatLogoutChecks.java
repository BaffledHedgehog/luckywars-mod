package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Actual native leave/save events, complete carried loot, latest credit, PvE/blocked/expired controls. */
final class CombatLogoutChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Combat logout: "+why);}
    private static final class Scene extends CarpetBotChecks.Fixture {
        final Map<UUID,MatchValueSnapshot> profiles;final boolean keep;
        final String marker="logout_qa_"+UUID.randomUUID();int serial,expiryAt,started;ServerPlayer expiry;
        Scene(GameController game){super(game);profiles=game.players().checkpointProfiles();keep=base.level.getGameRules().get(GameRules.KEEP_INVENTORY);
            var stat=net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER);
            for(var snapshot:List.of(base.saved,base.otherSaved))snapshot.stats.put(stat,snapshot.player.getStats().getValue(stat));
        }
        ServerPlayer participant(){
            var p=create("qaCL"+serial++,base.home.add(0,5,0),false);p.setNoGravity(true);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            require(game.allowDamage(p)&&!LbcCarpetBots.bot(p),"actual enrolled uncontrolled native Carpet participant");return p;
        }
        ItemStack item(Item type,int count,String label){var stack=new ItemStack(type,count);var tag=new CompoundTag();tag.putString("logout_case",marker);tag.putString("label",label);tag.putIntArray("payload",new int[]{7,19,91});stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.set(DataComponents.CUSTOM_NAME,Component.literal(label));return stack;}
        boolean ours(ItemEntity e){var tag=e.getItem().get(DataComponents.CUSTOM_DATA);return tag!=null&&marker.equals(tag.copyTag().getStringOr("logout_case",""));}
        List<ItemEntity> drops(){return base.level.getEntitiesOfClass(ItemEntity.class,new AABB(base.home,base.home).inflate(60),this::ours);}
        void clearDrops(){for(var item:drops())item.discard();}
        void hit(ServerPlayer target,ServerPlayer author){target.setInvulnerableTime(0);target.damageCooldownTime=0;require(target.hurtServer(base.level,target.damageSources().playerAttack(author),1),"real accepted nonlethal PvP damage");}
        void quit(ServerPlayer p){p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Owned combat-logout fixture")));require(game.server().getPlayerList().getPlayer(p.getUUID())==null,"actual listener/PlayerList leave completed");require(!CombatLogout.active(p),"synchronous death context released");}
        void saved(ServerPlayer p,boolean empty){
            try{var file=game.server().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");require(java.nio.file.Files.isRegularFile(file),"actual native player-data file saved");var tag=NbtIo.readCompressed(file,NbtAccounter.create(2*1024*1024));
                boolean nothing=tag.getListOrEmpty("Inventory").isEmpty()&&tag.getCompoundOrEmpty("equipment").isEmpty();require(nothing==empty,"native saved inventory/equipment reflect actual leave outcome: "+tag.keySet());
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
        }
        List<ItemStack> richLoot(ServerPlayer p){
            p.getInventory().clearContent();var loot=new ArrayList<ItemStack>();
            var diamonds=item(Items.DIAMOND,7,"inventory");p.getInventory().setItem(9,diamonds);loot.add(diamonds.copy());
            var helmet=item(Items.IRON_HELMET,1,"vanishing armor");helmet.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE),1);p.setItemSlot(EquipmentSlot.HEAD,helmet);loot.add(helmet.copy());
            var offhand=item(Items.SHIELD,1,"offhand");p.setItemSlot(EquipmentSlot.OFFHAND,offhand);loot.add(offhand.copy());
            var totem=item(Items.TOTEM_OF_UNDYING,1,"unconsumed totem");p.getInventory().setItem(1,totem);loot.add(totem.copy());
            var craft=item(Items.EMERALD,2,"crafting input");p.inventoryMenu.getCraftSlots().setItem(0,craft);loot.add(craft.copy());
            var cursor=item(Items.AMETHYST_SHARD,5,"cursor");p.containerMenu.setCarried(cursor);loot.add(cursor.copy());return loot;
        }
        void loot(List<ItemStack> expected){
            var actual=new ArrayList<ItemStack>();for(var item:drops()){actual.add(item.getItem().copy());base.owned.add(item.getUUID());}
            require(actual.size()==expected.size(),"all carried sources drop once: actual="+actual+" expected="+expected);
            for(var wanted:expected){int found=-1;for(int i=0;i<actual.size();i++)if(ItemStack.matches(wanted,actual.get(i))){found=i;break;}require(found>=0,"exact full components/count for "+wanted);actual.remove(found);}
            require(actual.isEmpty(),"no duplicate native armor or leftover stack");clearDrops();
        }
        @Override void setup(){
            base.prepareParticipants();require(!game.developerMode(),"ordinary match statistics");started=game.server().getTickCount();
            game.players().get(base.owner.getUUID()).lbcSkill=0;game.players().get(base.other.getUUID()).lbcSkill=0;
            var quiet=participant();var kept=item(Items.DIAMOND,7,"quiet leave");quiet.getInventory().setItem(9,kept);int deaths=game.players().get(quiet.getUUID()).deaths;quit(quiet);
            require(game.players().get(quiet.getUUID()).deaths==deaths&&ItemStack.matches(quiet.getInventory().getItem(9),kept)&&drops().isEmpty(),"noncombat leave preserves items and statistics");saved(quiet,false);

            var direct=participant();hit(direct,base.owner);hit(direct,base.other);game.addRuneLives(direct,2);require(game.lives(direct.getUUID())>1,"actual extra lives before forfeit");
            var expected=richLoot(direct);int alpha=game.players().get(base.owner.getUUID()).kills,beta=game.players().get(base.other.getUUID()).kills;deaths=game.players().get(direct.getUUID()).deaths;
            base.level.getGameRules().set(GameRules.KEEP_INVENTORY,true,game.server());quit(direct);
            require(game.players().get(base.owner.getUUID()).kills==alpha&&game.players().get(base.other.getUUID()).kills==beta+1&&game.players().get(direct.getUUID()).deaths==deaths+1,"latest accepted player gets exactly one kill/death");
            require(game.lives(direct.getUUID())==0&&!game.awaitingRespawn(direct.getUUID())&&direct.getInventory().isEmpty(),"forfeit exhausts all lives with no offline respawn/inventory");
            require(base.level.getGameRules().get(GameRules.KEEP_INVENTORY),"world keep-inventory rule remains enabled for other players");loot(expected);saved(direct,true);
            game.leave(direct);require(game.players().get(base.other.getUUID()).kills==beta+1&&game.players().get(direct.getUUID()).deaths==deaths+1&&drops().isEmpty(),"repeated leave callback cannot duplicate death, credit or loot");

            var spell=participant();var source=game.items().indirectKills().captureDamage(spell.damageSources().magic(),game.items().indirectKills().coldCause(base.owner,true),null);
            require(spell.hurtServer(base.level,source,1),"real entityless accepted spell damage with captured author");expected=List.of(item(Items.GOLD_INGOT,3,"spell loot"));spell.getInventory().setItem(9,expected.getFirst().copy());alpha=game.players().get(base.owner.getUUID()).kills;quit(spell);require(game.players().get(base.owner.getUUID()).kills==alpha+1,"captured spell author survives native leave and ledger cleanup");loot(expected);saved(spell,true);

            var departed=participant();var offline=participant();hit(offline,base.other);offline.setInvulnerableTime(0);offline.damageCooldownTime=0;
            source=game.items().indirectKills().captureDamage(offline.damageSources().magic(),game.items().indirectKills().coldCause(departed,true),null);
            require(offline.hurtServer(base.level,source,1),"newer captured hit before attacker disconnects");int offlineKills=game.players().get(departed.getUUID()).kills;beta=game.players().get(base.other.getUUID()).kills;
            int nativeBeta=base.other.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER));quit(departed);quit(offline);
            require(game.players().get(departed.getUUID()).kills==offlineKills+1&&game.players().get(base.other.getUUID()).kills==beta,"offline last damager gets persisted profile kill instead of stale native player");
            require(base.other.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER))==nativeBeta,"stale native player never receives vanilla kill score");saved(offline,true);

            var frozen=participant();hit(frozen,base.owner);frozen.entityTags().add("time_stopped");alpha=game.players().get(base.owner.getUUID()).kills;
            expected=List.of(item(Items.IRON_INGOT,4,"frozen logout"));frozen.getInventory().setItem(9,expected.getFirst().copy());quit(frozen);
            require(game.players().get(base.owner.getUUID()).kills==alpha+1&&game.lives(frozen.getUUID())==0,"personal time stop cannot postpone an actual combat disconnect");loot(expected);saved(frozen,true);

            var npc=new Skeleton(EntityTypes.SKELETON,base.level);npc.setPos(base.home.add(5,8,0));npc.setNoAi(true);npc.setNoGravity(true);require(base.level.addFreshEntity(npc),"native hostile mob witness");base.owned.add(npc.getUUID());
            var mixed=participant();hit(mixed,base.owner);mixed.setInvulnerableTime(0);mixed.damageCooldownTime=0;require(mixed.hurtServer(base.level,mixed.damageSources().mobAttack(npc),1),"newer actual NPC damage");alpha=game.players().get(base.owner.getUUID()).kills;deaths=game.players().get(mixed.getUUID()).deaths;quit(mixed);require(game.players().get(base.owner.getUUID()).kills==alpha&&game.players().get(mixed.getUUID()).deaths==deaths+1&&mixed.getKillCredit()==npc,"newer NPC keeps native/mod kill instead of old player");
            var pve=participant();require(pve.hurtServer(base.level,pve.damageSources().mobAttack(npc),1),"actual PvE-only damage");kept=item(Items.DIAMOND,7,"PvE leave");pve.getInventory().setItem(9,kept);deaths=game.players().get(pve.getUUID()).deaths;quit(pve);require(game.players().get(pve.getUUID()).deaths==deaths&&ItemStack.matches(pve.getInventory().getItem(9),kept)&&drops().isEmpty(),"PvE alone does not invent PvP logout penalty");saved(pve,false);

            var blocked=participant();blocked.getAbilities().invulnerable=true;require(!blocked.hurtServer(base.level,blocked.damageSources().playerAttack(base.owner),1),"native rejected player hit");blocked.getAbilities().invulnerable=false;deaths=game.players().get(blocked.getUUID()).deaths;quit(blocked);require(game.players().get(blocked.getUUID()).deaths==deaths&&drops().isEmpty(),"rejected damage never creates a combat receipt");

            expiry=participant();hit(expiry,base.owner);expiry.getInventory().setItem(9,item(Items.DIAMOND,7,"expired leave"));expiryAt=expiry.tickCount;
        }
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();require(game.server().getTickCount()-started<400,"bounded natural combat expiry");
            if(expiry.tickCount-expiryAt<=net.minecraft.world.damagesource.CombatTracker.RESET_COMBAT_STATUS_TIME+5)return false;
            int deaths=game.players().get(expiry.getUUID()).deaths;quit(expiry);require(game.players().get(expiry.getUUID()).deaths==deaths&&!expiry.getInventory().getItem(9).isEmpty()&&drops().isEmpty(),"naturally expired native combat history preserves normal leave");saved(expiry,false);return true;
        }
        @Override public void close(){
            for(var item:drops())base.owned.add(item.getUUID());
            try{super.close();}finally{base.level.getGameRules().set(GameRules.KEEP_INVENTORY,keep,game.server());game.players().restoreProfiles(profiles);game.players().save();}
        }
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS combat-logout (actual native leave/save, last player and captured spell credit, NPC supersession, all inventory/vanishing armor/offhand/totem/craft/cursor drops under keepInventory, once guard and noncombat/PvE/rejected/naturally expired controls)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL combat-logout",error);}}
}
