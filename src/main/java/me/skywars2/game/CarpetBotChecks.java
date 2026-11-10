package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Real Carpet players and their unmodified native scheduler, movement, inventory menus and combat. */
public final class CarpetBotChecks {
    private CarpetBotChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Carpet bot: "+why);}
    private static Fixture running;
    static class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture base;
        final Vec3 origin;
        final Set<Long> forced=new HashSet<>();final List<ServerPlayer> actors=new ArrayList<>();
        ServerPlayer bot,otherBot;BlockPos firstChest,farChest;int phase,began,when,deathLives,armedAt=-1;float lastYaw,missHealth;boolean missed,closed;
        Fixture(GameController game){this(game,new Vec3(-160.5,120,-160.5));}
        Fixture(GameController game,Vec3 origin){this.game=game;this.origin=origin;base=new WoolBucketChecks.Fixture(game,false);}
        void setup(){
            base.prepareParticipants();
            var center=BlockPos.containing(origin);
            for(int x=(center.getX()-3)>>4;x<=(center.getX()+37)>>4;x++)for(int z=(center.getZ()-6)>>4;z<=(center.getZ()+6)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}
                base.level.getChunk(x,z);
            }
            for(int x=-2;x<=35;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++)
                base.set(center.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            base.move(base.owner,origin.add(80,8,0));base.move(base.other,origin.add(84,8,0));
            firstChest=center.offset(6,0,2);farChest=center.offset(30,0,0);
            chest(firstChest,new ItemStack(Items.IRON_SWORD),new ItemStack(Items.COBBLESTONE,12),new ItemStack(Items.IRON_CHESTPLATE),new ItemStack(Items.COOKED_BEEF,4));
            bot=create("botNativeA",origin);bot.getFoodData().setFoodLevel(8);bot.getFoodData().setSaturation(0);
            began=when=game.server().getTickCount();lastYaw=bot.getYRot();
        }
        void chest(BlockPos pos,ItemStack... items){
            base.set(pos,Blocks.CHEST.defaultBlockState());var container=(ChestBlockEntity)base.level.getBlockEntity(pos);
            for(int i=0;i<items.length;i++)container.setItem(i,items[i]);container.setChanged();
        }
        List<BlockPos> starterChests(){
            var result=new ArrayList<BlockPos>();var center=BlockPos.containing(origin);
            if(base.level.getBlockEntity(firstChest) instanceof net.minecraft.world.Container old)old.clearContent();
            base.set(firstChest,Blocks.AIR.defaultBlockState());
            for(int i=0;i<3;i++){
                var pos=center.offset(5+i*3,0,i%2==0?2:-2);result.add(pos);base.set(pos,Blocks.CHEST.defaultBlockState());
                var chest=(ChestBlockEntity)base.level.getBlockEntity(pos);chest.clearContent();
                chest.setLootTable(MapManager.chestLootTable(2,"default",false));chest.setLootTableSeed(7100+i);chest.getItem(0);MapManager.normalizeLootCounts(chest);
            }
            return result;
        }
        ServerPlayer create(String name,Vec3 pos){
            try{
                var uuid=GameChecks.offlineUuid(name);require(game.server().getPlayerList().getPlayer(uuid)==null,"owned fake identity offline");
                var profile=new com.mojang.authlib.GameProfile(uuid,name);var type=Class.forName("carpet.patches.EntityPlayerMPFake");
                var p=(ServerPlayer)type.getMethod("respawnFake",net.minecraft.server.MinecraftServer.class,net.minecraft.server.level.ServerLevel.class,
                        com.mojang.authlib.GameProfile.class,net.minecraft.server.level.ClientInformation.class).invoke(null,game.server(),base.level,profile,base.owner.clientInformation());
                var connection=(net.minecraft.network.Connection)Class.forName("carpet.patches.FakeClientConnection")
                        .getConstructor(net.minecraft.network.protocol.PacketFlow.class).newInstance(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
                game.server().getPlayerList().placeNewPlayer(connection,p,new net.minecraft.server.network.CommonListenerCookie(profile,0,p.clientInformation(),false));actors.add(p);
                require(game.returnFromGameModeCommand(p,GameType.SURVIVAL),"real Carpet fake enrolls through existing native roster owner");p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();p.removeAllEffects();p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);
                game.items().mana().resetResources(p);
                p.getAttribute(Attributes.ARMOR).setBaseValue(0);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                game.server().getScoreboard().removePlayerFromTeam(p.getScoreboardName());
                move(p,pos,0);p.hasChangedDimension();p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                require(LbcCarpetBots.bot(p)&&p.connection.getClass().getName().equals("carpet.patches.NetHandlerPlayServerFake"),"actual Carpet body/listener without recording substitute");
                return p;
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void move(ServerPlayer p,Vec3 pos,float yaw){
            require(p.teleportTo(base.level,pos.x,pos.y,pos.z,Set.of(),yaw,0,true),"native bot teleport fixture setup");
            p.setNoGravity(false);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setOnGround(true);
            p.setYRot(yaw);p.setYHeadRot(yaw);p.setXRot(0);
        }
        void disconnect(ServerPlayer p){
            game.carpetBots().forget(p);game.items().indirectKills().clear(p);
            var live=game.server().getPlayerList().getPlayer(p.getUUID());
            if(live!=null){
                game.carpetBots().forget(live);game.items().indirectKills().clear(live);
                live.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(Component.literal("Owned Carpet bot fixture cleanup")));
                if(live!=p)live.discard();
            }
            p.discard();require(game.server().getPlayerList().getPlayer(p.getUUID())==null,"owned fake fully disconnected");
        }
        boolean step(){
            base.client.acknowledge();base.audience.acknowledge();
            int now=game.server().getTickCount();require(now-began<1500,"bounded native scene: phase="+phase+" "+game.carpetBots().diagnostics(bot));
            if(phase>=3&&(now-when)%40==0)SkyWars2.LOGGER.info("[check] Carpet progress phase={} pos={} motion={} observed={} {}",phase,bot.position(),bot.getDeltaMovement(),game.carpetBots().observation(bot),game.carpetBots().diagnostics(bot));
            if(phase==8){var replacement=game.server().getPlayerList().getPlayer(bot.getUUID());
                if(replacement==null)throw new IllegalStateException("Carpet bot: remaining-life fake was disconnected");
                if(replacement==bot)return false;
                require(LbcCarpetBots.bot(replacement)&&replacement.connection.getClass().getName().equals("carpet.patches.NetHandlerPlayServerFake"),"native PlayerList/Carpet respawn preserves actual fake body/listener type");
                require(replacement.isAlive()&&game.allowDamage(replacement)&&game.lives(replacement.getUUID())==deathLives-1,"actual native death debits one life and ordinary respawn re-enrolls body");
                actors.remove(bot);actors.add(replacement);bot=replacement;lastYaw=bot.getYRot();when=now;phase=9;
            }
            require(bot.isAlive()&&!bot.isRemoved()&&game.allowDamage(bot),"ordinary finite-health fake remains a live participant phase="+phase+" pos="+bot.position()+" "+game.carpetBots().diagnostics(bot));
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees(bot.getYRot()-lastYaw))<=12.01,"bounded actual camera yaw per native tick");lastYaw=bot.getYRot();
            switch(phase){
                case 0->{
                    if(bot.getInventory().countItem(Items.IRON_SWORD)==0||!bot.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)||bot.isUsingItem()||bot.getFoodData().getFoodLevel()<18)return false;
                    var source=(ChestBlockEntity)base.level.getBlockEntity(firstChest);
                    require(source.isEmpty()&&bot.getInventory().countItem(Items.COBBLESTONE)==12,"real chest emptied and exact finite block stack transferred");
                    require(bot.getInventory().countItem(Items.COOKED_BEEF)<4&&game.carpetBots().observation(bot).foods()>0,"actual native food use consumes owned food and restores hunger");
                    require(!bot.isNoGravity()&&!bot.getAbilities().flying,"movement uses ordinary survival gravity");
                    // Select and charge the real sword before placing the victim behind the camera.
                    // An idle bot may retain its empty looting hand; switching at attack time legitimately adds native cooldown.
                    for(int i=0;i<36;i++)if(bot.getInventory().getItem(i).is(Items.IRON_SWORD)){
                        if(i<9)bot.getInventory().setSelectedSlot(i);
                        else for(int slot=0;slot<bot.inventoryMenu.slots.size();slot++)if(bot.inventoryMenu.slots.get(slot).container==bot.getInventory()&&bot.inventoryMenu.slots.get(slot).getContainerSlot()==i){bot.inventoryMenu.clicked(slot,0,net.minecraft.world.inventory.ContainerInput.SWAP,bot);bot.getInventory().setSelectedSlot(0);break;}
                        break;
                    }
                    if(armedAt<0){armedAt=now;return false;}
                    if(now-armedAt<20||!bot.getMainHandItem().is(Items.IRON_SWORD)||bot.getAttackStrengthScale(.5f)<.99f)return false;
                    move(bot,origin.add(2,0,0),180);lastYaw=180;
                    base.move(base.other,origin.add(2,0,2.5));base.other.setInvulnerableTime(0);base.other.damageCooldownTime=0;missHealth=base.other.getHealth();
                    when=now;phase=1;
                }
                case 1->{
                    var seen=game.carpetBots().observation(bot);
                    if(!missed&&seen.attempts()>0){require(seen.hits()==0&&base.other.getHealth()==missHealth,"real attack attempt misses while target is behind bounded camera: seen="+seen+" hp="+base.other.getHealth()+" before="+missHealth+" "+game.carpetBots().diagnostics(bot));missed=true;}
                    if(seen.hits()==0)return false;
                    require(missed&&base.other.getHealth()<missHealth&&base.other.getLastHurtByMob()==bot,"real aligned native melee damages the actual target with bot attribution");
                    base.move(base.other,origin.add(84,8,0));game.carpetBots().forget(bot);move(bot,origin.add(8,0,0),0);lastYaw=0;
                    bot.setHealth(100);bot.removeAllEffects();bot.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
                    otherBot=create("botNativeB",origin.add(8,0,2.2));otherBot.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));otherBot.getFoodData().setFoodLevel(20);
                    when=now;phase=2;
                }
                case 2->{
                    if(bot.getHealth()>=100||otherBot.getHealth()>=100)return false;
                    require(game.carpetBots().observation(bot).hits()>0&&game.carpetBots().observation(otherBot).hits()>0,"two real bots opportunistically fight each other");
                    disconnect(otherBot);actors.remove(otherBot);otherBot=null;
                    game.carpetBots().forget(bot);bot.removeAllEffects();bot.setHealth(100);bot.getInventory().clearContent();bot.getFoodData().setFoodLevel(20);
                    bot.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,12));move(bot,origin.add(14,0,0),-90);lastYaw=-90;
                    base.set(firstChest,Blocks.AIR.defaultBlockState());
                    var center=BlockPos.containing(origin);for(int x=18;x<=25;x++)for(int z=-4;z<=4;z++)base.set(center.offset(x,-1,z),Blocks.AIR.defaultBlockState());
                    chest(farChest,new ItemStack(Items.DIAMOND));when=now;phase=3;
                }
                case 3->{
                    if(bot.getInventory().countItem(Items.DIAMOND)==0)return false;
                    int remaining=bot.getInventory().countItem(Items.COBBLESTONE),placed=0;var center=BlockPos.containing(origin);
                    for(int x=-2;x<=35;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++)if(base.level.getBlockState(center.offset(x,y,z)).is(Blocks.COBBLESTONE))placed++;
                    require(placed>0&&remaining+placed==12&&game.carpetBots().observation(bot).placements()==placed,"shared Chef navigation crosses real gap with exactly debited native placement, no free blocks");
                    game.carpetBots().forget(bot);bot.getInventory().clearContent();move(bot,origin.add(14,0,0),-90);lastYaw=-90;
                    for(int x=-2;x<=35;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++)if(base.level.getBlockState(center.offset(x,y,z)).is(Blocks.COBBLESTONE))base.set(center.offset(x,y,z),Blocks.AIR.defaultBlockState());
                    chest(farChest,new ItemStack(Items.EMERALD));when=now;phase=4;
                }
                case 4->{
                    if(now-when<60)return false;
                    require(game.carpetBots().observation(bot).placements()==0&&bot.getInventory().countItem(Items.EMERALD)==0,"resource-empty fake cannot conjure another bridge or remotely loot");
                    game.items().timeStop().begin(base.level,100);when=now;phase=5;
                }
                case 5->{
                    if(now-when<3)return false;
                    require(!bot.getLastClientInput().jump()&&bot.getDeltaMovement().lengthSqr()<.0001,"TimeStop suppresses native fake input/velocity");
                    game.items().timeStop().end(base.level);game.carpetBots().reset();require(game.carpetBots().count()==0,"derived intents release on reset");require(!bot.isShiftKeyDown()&&!bot.isSprinting()&&bot.getLastClientInput().equals(new net.minecraft.world.entity.player.Input(false,false,false,false,false,false,false)),"reset releases every held native input");
                    var center=BlockPos.containing(origin);base.set(firstChest,Blocks.AIR.defaultBlockState());base.set(farChest,Blocks.AIR.defaultBlockState());
                    for(int x=0;x<=10;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=3;y++)
                        base.set(center.offset(x,y,z),(y==-1||y>=2||z!=0||x==0||x==10)?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());
                    base.set(center.offset(4,0,0),Blocks.STONE.defaultBlockState());base.set(center.offset(4,1,0),Blocks.STONE.defaultBlockState());
                    chest(center.offset(8,0,0),new ItemStack(Items.EMERALD));bot.getInventory().clearContent();bot.getInventory().setItem(0,new ItemStack(Items.IRON_PICKAXE));
                    bot.getFoodData().setFoodLevel(20);move(bot,origin.add(2,0,0),-90);lastYaw=-90;when=now;phase=6;
                }
                case 6->{
                    if(bot.getInventory().countItem(Items.EMERALD)==0)return false;
                    var center=BlockPos.containing(origin);
                    require(base.level.getBlockState(center.offset(4,0,0)).isAir()&&base.level.getBlockState(center.offset(4,1,0)).isAir(),"native tool mines both tunnel obstructions before looting");
                    ItemStack pick=ItemStack.EMPTY;for(int i=0;i<36;i++)if(bot.getInventory().getItem(i).is(Items.IRON_PICKAXE))pick=bot.getInventory().getItem(i);
                    require(!pick.isEmpty()&&pick.getDamageValue()>=2,"actual native survival mining consumes tool durability");
                    require(bot.getInventory().countItem(Items.COBBLESTONE)>=2,"native mined block loot remains available to bot");
                    require(base.level.getBlockState(center.offset(4,3,0)).is(Blocks.BEDROCK)&&base.level.getBlockState(center.offset(4,0,1)).is(Blocks.BEDROCK),"unbreakable roof and side remain intact");
                    game.carpetBots().reset();require(game.carpetBots().count()==0,"mining intentions release on reset");
                    bot.getInventory().clearContent();for(int x=1;x<10;x++)for(int y=2;y<=3;y++)base.set(center.offset(x,y,0),Blocks.AIR.defaultBlockState());
                    base.set(center.offset(3,0,0),Blocks.STONE.defaultBlockState());chest(center.offset(8,0,0),new ItemStack(Items.DIAMOND));
                    move(bot,origin.add(1,0,0),-90);lastYaw=-90;when=now;phase=7;
                }
                case 7->{
                    if(bot.getInventory().countItem(Items.DIAMOND)==0)return false;
                    require(base.level.getBlockState(BlockPos.containing(origin).offset(3,0,0)).is(Blocks.STONE),"real Carpet native jump passes the intact one-block stair without mining or conjured blocks");
                    game.carpetBots().reset();int beforeLives=game.lives(bot.getUUID());game.addRuneLives(bot,2);deathLives=game.lives(bot.getUUID());require(deathLives==beforeLives+2&&deathLives>1,"native rune life grant prepares remaining-life fixture");
                    bot.removeAllEffects();bot.setAbsorptionAmount(0);bot.setInvulnerableTime(0);bot.damageCooldownTime=0;bot.setHealth(20);
                    require(bot.hurtServer(base.level,bot.damageSources().playerAttack(base.owner),10000),"actual lethal player attack accepted on real fake");when=now;phase=8;
                }
                case 9->{
                    if(now-when<2)return false;
                    require(game.carpetBots().count()>0&&bot.connection.hasClientLoaded(),"normal fake ticking resumes AI on replacement native body");game.carpetBots().reset();return true;
                }
                default->throw new IllegalStateException();
            }
            return false;
        }
        public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            try{game.items().timeStop().end(base.level);}catch(Throwable error){failure=error;}
            for(var actor:List.copyOf(actors))try{disconnect(actor);}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            actors.clear();
            try{base.close();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            finally{for(long key:forced)base.level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);}
            if(failure!=null)throw new IllegalStateException("Carpet fixture cleanup",failure);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.setup();}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS carpet-bot-ai (actual Carpet players/native ticks, chest menus, equipment and consumed food, bounded camera/real misses and attributed melee, mutual combat, shared gap navigation and exact finite block debit, zero-resource control, TimeStop, real two-block tunnel mining/tool wear/loot, actual native stair jump, real lethal death/native remaining-life fake respawn and reset)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL carpet-bot-ai",error);}}
}
