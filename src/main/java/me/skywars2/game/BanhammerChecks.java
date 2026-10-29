package me.skywars2.game;

import me.skywars2.SkyWars2;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native collision, quiet removal and real Carpet disconnect regressions. */
public final class BanhammerChecks {
    private BanhammerChecks() { }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Banhammer: "+why);}
    private static void safe(AllGrimoireChecks.Fixture f){
        f.base.owner.setPos(f.at.add(-12,0,0));f.beta.setPos(f.at.add(12,0,0));
    }
    private static LbcSpecialTools.Hammer launch(AllGrimoireChecks.Fixture f){
        var g=f.base.game;var s=g.items().specialTools();s.stop();f.base.owner.setPos(f.at);f.base.owner.setYRot(0);f.base.owner.setXRot(0);
        f.base.owner.setItemInHand(InteractionHand.MAIN_HAND,g.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:ported/banhammer").getFirst());
        f.base.mana.score(f.base.owner,"swordcharged7").set(1);
        f.level.setBlock(BlockPos.containing(f.at.add(0,f.base.owner.getEyeHeight(),3)),Blocks.STONE.defaultBlockState(),2|16|32);
        require(f.base.input(InteractionHand.MAIN_HAND)&&s.hammerCount()==1,"native item input registers falling hammer");
        var h=s.hammerSnapshot().getFirst();safe(f);return h;
    }
    private static void collision(AllGrimoireChecks.Fixture f,BlockState state,double top,int edge,boolean elevated){
        var s=f.base.game.items().specialTools();var h=launch(f);var center=BlockPos.containing(f.at.add(0,0,12));
        for(var pos:BlockPos.betweenClosed(center.offset(-3,-2,-2),center.offset(3,10,2)))f.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
        var obstacle=center.offset(edge,elevated?3:0,0);f.level.setBlock(obstacle,state,2|16|32);
        double surface=obstacle.getY()+top;
        h.impact=new Vec3(center.getX()+.5,center.getY()-2,center.getZ()+.5);
        h.point=new Vec3(center.getX()+.5,surface+3.75,center.getZ()+.5);
        // The head bottom starts 1.75 above contact: the obstacle lies inside a two-block step.
        var before=h.point;int age=h.age;f.base.game.items().timeStop().begin(f.level,40);
        try{s.tick();require(h.point.equals(before)&&h.age==age&&!h.impacted,"dimension Time Stop freezes fall and clock");}
        finally{f.base.game.items().timeStop().end(f.level);}
        s.tick();require(h.impacted&&Math.abs(h.impact.y-surface)<1e-8,"first swept surface uses exact collision top: "+state+" edge="+edge);
        require(Math.abs(h.point.y-(surface+2))<1e-8&&Math.abs(h.head().minY-surface)<1e-8,"authored centered display bottom rests on surface");
        require(h.head().maxY>surface&&h.head().minY>=surface-1e-8,"head never tunnels into obstacle");
        // A fresh creature appearing after impact must survive every subsequent tick and explicit replay.
        var late=cow(f,h.impact.add(0,0,1));var landed=h.point;int landedAge=h.age;
        for(int n=0;n<4;n++)s.tick();s.impact(h);
        require(h.point.equals(landed)&&h.age==landedAge+4&&!late.isRemoved(),"landed hammer stops and impact cannot repeat");
        late.discard();s.stop();
    }
    private static Cow cow(AllGrimoireChecks.Fixture f,Vec3 at){
        var c=new Cow(EntityTypes.COW,f.level);c.setNoAi(true);c.setNoGravity(true);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);c.setHealth(1_000_000);c.setPos(at);
        require(f.level.addFreshEntity(c),"native high-health mob");return c;
    }
    private static Set<Entity> lootEntities(AllGrimoireChecks.Fixture f){
        Set<Entity> out=Collections.newSetFromMap(new IdentityHashMap<>());
        for(var e:f.level.getAllEntities())if(e instanceof ItemEntity||e instanceof ExperienceOrb)out.add(e);return out;
    }
    private static void mobs(AllGrimoireChecks.Fixture f){
        var g=f.base.game;var s=g.items().specialTools();safe(f);var impact=f.at.add(0,0,12);
        var h=s.new Hammer(f.base.owner,impact);h.impact=impact;
        var victim=cow(f,impact);victim.setPermanentlyInvulnerable(true);victim.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND));
        var ally=cow(f,impact.add(1,0,0));var greed=cow(f,impact.add(-1,0,0));greed.entityTags().add("sin_greed_active");
        var role=cow(f,impact.add(0,0,1));role.entityTags().add("strict_map_object");var remote=cow(f,impact.add(5,0,5));
        var board=g.server().getScoreboard();var old=f.base.owner.getTeam();require(board.getPlayerTeam("banhammer_mob_check")==null,"exclusive team fixture");
        var team=board.addPlayerTeam("banhammer_mob_check");team.setAllowFriendlyFire(false);
        require(g.items().wtf().count()==0,"exclusive Chef erasure regression");
        var activeChef=g.items().wtf().spawn(f.base.owner,remote);var knockedChef=g.items().wtf().spawn(f.base.owner,remote);
        require(activeChef!=null&&knockedChef!=null,"two native Chef actors");activeChef.setPos(impact.add(.5,0,0));knockedChef.setPos(impact.add(-.5,0,0));knockedChef.receive(20);
        var before=lootEntities(f);var profile=g.players().get(f.base.owner.getUUID());int kills=profile.kills;
        int nativeKills=f.base.owner.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));
        try{
            board.addPlayerToTeam(f.base.owner.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
            require(f.base.owner.isAlliedTo(ally),"real allied mob fixture");s.impact(h);
            require(victim.isRemoved()&&ally.isRemoved(),"discard invulnerable equipped million-HP mob and WILD ally");
            require(activeChef.isRemoved()&&knockedChef.isRemoved()&&g.items().wtf().count()==0&&g.items().wtf().pinnedChunks()==0,"Banhammer erases active and knocked native Chefs without damage/death/ticket leaks");
            require(victim.getHealth()==1_000_000&&victim.deathTime==0&&victim.getLastDamageSource()==null,"discard bypasses native damage and death callbacks");
            require(!greed.isRemoved()&&!role.isRemoved()&&!remote.isRemoved(),"Greed, technical role and spherical radius exclusions");
            require(lootEntities(f).equals(before)&&profile.kills==kills&&f.base.owner.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS))==nativeKills,"no item/XP/death loot or kill rewards");
        }finally{
            g.items().wtf().erase(activeChef);g.items().wtf().erase(knockedChef);h.destroy();for(var c:List.of(victim,ally,greed,role,remote)){board.removePlayerFromTeam(c.getScoreboardName());c.discard();}
            board.removePlayerFromTeam(f.base.owner.getScoreboardName());board.removePlayerTeam(team);if(old!=null)board.addPlayerToTeam(f.base.owner.getScoreboardName(),old);
        }
    }
    public static void run(GameController g){
        try(var f=new AllGrimoireChecks.Fixture(g)){
            f.stats.put(LbcSpecialTools.BANHAMMER,f.base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcSpecialTools.BANHAMMER)));
            try{
                collision(f,Blocks.STONE.defaultBlockState(),1,0,false);
                collision(f,Blocks.STONE_SLAB.defaultBlockState(),.5,0,false);
                collision(f,Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(),1.0/16,0,false);
                collision(f,Blocks.STONE.defaultBlockState(),1,0,true);
                collision(f,Blocks.STONE_SLAB.defaultBlockState(),.5,2,false);
                mobs(f);
            }finally{g.items().specialTools().stop();}
        }
        SkyWars2.LOGGER.info("[check] Banhammer swept collision, model contact and no-drop mob bans passed; awaiting Carpet cleanup");
    }
    private static AllGrimoireChecks.Fixture retained;
    private static ServerPlayer fake;
    /** Finish after three native server ticks; never substitutes either ordinary fixture participant. */
    public static void begin(GameController g){
        require(retained==null&&fake==null,"exclusive Carpet fixture");var f=new AllGrimoireChecks.Fixture(g);
        try{
            safe(f);var type=Class.forName("carpet.patches.EntityPlayerMPFake");
            var profile=new GameProfile(UUID.randomUUID(),"BH"+UUID.randomUUID().toString().replace("-","").substring(0,12));
            fake=(ServerPlayer)type.getMethod("respawnFake",net.minecraft.server.MinecraftServer.class,net.minecraft.server.level.ServerLevel.class,GameProfile.class,net.minecraft.server.level.ClientInformation.class)
                    .invoke(null,g.server(),f.level,profile,f.base.owner.clientInformation());
            var connection=(Connection)Class.forName("carpet.patches.FakeClientConnection").getConstructor(PacketFlow.class).newInstance(PacketFlow.SERVERBOUND);
            g.server().getPlayerList().placeNewPlayer(connection,fake,new CommonListenerCookie(profile,0,fake.clientInformation(),false));
            require(g.returnFromGameModeCommand(fake,GameType.SURVIVAL),"latejoined fake enrolled through native gamemode path");fake.setGameMode(GameType.SURVIVAL);
            g.players().get(fake.getUUID()).lbcSkill=0;fake.getInventory().clearContent();fake.entityTags().remove("sin_greed_active");
            fake.teleportTo(f.level,f.at.x,f.at.y,f.at.z+12,Set.of(),0,0,true);fake.setNoGravity(true);
            require(g.items().wands().validRoleTarget(fake,false)&&fake.getClass()==type,"actual Carpet fake is eligible participant");
            var s=g.items().specialTools();var h=s.new Hammer(f.base.owner,fake.position());h.impact=fake.position();
            try{s.impact(h);require(h.impacted,"actual Carpet impact dispatched");}finally{h.destroy();}
            retained=f;
        }catch(Throwable failure){
            cleanupFake(g);f.close();throw new IllegalStateException("Banhammer Carpet setup",failure);
        }
    }
    public static void finish(GameController g){
        require(retained!=null&&fake!=null,"pending actual Carpet impact");
        try{
            require(g.server().getPlayerList().getPlayer(fake.getUUID())==null&&!fake.level().players().contains(fake)&&fake.isRemoved(),"Carpet kill API removes fake from native player list and world");
        }finally{cleanupFake(g);retained.close();retained=null;}
        SkyWars2.LOGGER.info("[check] PASS banhammer (native swept solid/slab/thin/edge/first-surface contact, authored display bottom offset, Time Stop, no repeated impact, WILD no-drop invulnerable mob discard, Greed/role/radius guards and actual Carpet player removal)");
    }
    private static void cleanupFake(GameController g){
        if(fake!=null){
            if(g.server().getPlayerList().getPlayer(fake.getUUID())==fake)fake.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("Banhammer fixture cleanup")));
            fake.discard();fake=null;
        }
    }
}
