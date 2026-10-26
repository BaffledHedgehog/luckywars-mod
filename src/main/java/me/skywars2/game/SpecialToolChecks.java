package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real native damage/death callbacks, item input, terrain writes, ownership and match stop. */
public final class SpecialToolChecks {
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Special tools: "+why);}
    private static ItemStack loot(AllGrimoireChecks.Fixture f,String name){var out=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:ported/"+name);require(out.size()==1,"singleton "+name);return out.getFirst();}
    private static Cow cow(AllGrimoireChecks.Fixture f,Vec3 at,float hp){var c=new Cow(EntityTypes.COW,f.level);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);c.setHealth(hp);c.setPos(at);require(f.level.addFreshEntity(c),"native Cow");return c;}
    private static void reset(AllGrimoireChecks.Fixture f){f.base.game.items().specialTools().stop();f.reset();f.beta.setPos(f.at.add(12,500,0));}
    private static void presentation(AllGrimoireChecks.Fixture f){
        var dimensions=f.base.game.server().getStructureTemplateManager().get(Identifier.parse("lbc:air11x11")).orElseThrow().getSize();
        require(dimensions.getX()==11&&dimensions.getY()==11&&dimensions.getZ()==11,"source air template really is11 cubed");
        for(var name:List.of("random_enchanted_shovel","efficient_shovel","mortis","death_scythe")){
            var s=loot(f,name);require(s.getCount()==1&&s.get(DataComponents.TOOLTIP_STYLE)!=null&&s.get(DataComponents.LORE)!=null&&LbcFartus.data(s).getIntOr("transformable",0)==1,"source presentation "+name);
            if(!name.equals("death_scythe"))require(s.get(DataComponents.TOOL)!=null&&s.getMaxDamage()>0,"real native mining shovel "+name);
        }
        require(loot(f,"mortis").is(Items.NETHERITE_SHOVEL)&&loot(f,"random_enchanted_shovel").is(Items.IRON_SHOVEL),"source vanilla tools preserved");
        var book=loot(f,"death_scythe");require(book.is(LbcSpecialTools.SCYTHE)&&Identifier.parse("lbc:death_skythe").equals(book.get(DataComponents.ITEM_MODEL)),"native scythe and source model spelling");
        var seen=new HashSet<String>();
        for(int n=0;n<256;n++){
            for(var pool:List.of("transformables_shovel","transformables_shovel_all","transformables_hoe_all")){
                var out=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:"+pool);require(out.size()==1&&out.getFirst().getCount()==1,"closed native weighted pool "+pool);
                var s=out.getFirst();if(pool.equals("transformables_shovel_all")){
                    require(s.is(Items.IRON_SHOVEL)||s.is(Items.NETHERITE_SHOVEL)||s.is(LbcHalloweenWind.MACE)||s.is(LbcSoulhunter.MACE),"all expanded shovel branches native");
                    seen.add(s.getItem().toString());
                }else require(LbcFartus.data(s).getIntOr(pool.contains("hoe")?"hoe":"shovel",0)==1,"source family flag "+pool);
            }
        }
        require(seen.size()==4,"all ordinary and both native mace outcomes observed");
    }
    private static void mortis(AllGrimoireChecks.Fixture f){reset(f);var p=f.base.owner;var g=f.base.game;var s=g.items().specialTools();
        p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"mortis"));var victim=cow(f,f.at.add(0,0,3),100);
        var destination=victim.position().add(0,0,4);var wall=BlockPos.containing(destination);f.level.setBlock(wall,Blocks.STONE.defaultBlockState(),2|16|32);
        var floor=BlockPos.containing(destination.add(0,-1,0));f.level.setBlock(floor,Blocks.AIR.defaultBlockState(),2|16|32);
        require(victim.hurtServer(f.level,f.level.damageSources().playerAttack(p),1),"accepted real native melee");
        require(p.position().distanceToSqr(destination)<1e-8,"lunge ends four blocks beyond the actual victim");
        require(f.level.getBlockState(wall).isAir()&&f.level.getBlockState(floor).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLACK)),"passage destroyed, unsupported source floor glass");
        var regeneration=p.getEffect(MobEffects.REGENERATION);require(regeneration!=null&&regeneration.getDuration()==20&&regeneration.getAmplifier()==2,"source RegenIII for one second");
        require(f.base.recording.packets.stream().filter(k->k instanceof ClientboundSoundPacket).count()>=3,"all three source flap sounds emitted");
        var old=p.position();var remote=cow(f,old.add(0,0,3),100);
        require(remote.hurtServer(f.level,f.level.damageSources().cactus(),1)&&p.position().equals(old),"unrelated damage while shovel held cannot cause lunge");
        victim.discard();remote.discard();reset(f);p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"mortis"));p.setYRot(90);
        victim=cow(f,f.at.add(-3,0,0),100);destination=victim.position().add(-4,0,0);
        require(victim.hurtServer(f.level,f.level.damageSources().playerAttack(p),1000)&&p.position().distanceToSqr(destination)<1e-8,"lethal hit and rotated lunge use the struck victim");
        reset(f);p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"mortis"));var ward=cow(f,f.at.add(0,0,3),100);ward.entityTags().add("sin_greed_active");old=p.position();
        require(!ward.hurtServer(f.level,f.level.damageSources().playerAttack(p),1)&&p.position().equals(old),"Greed rejects hit and follow-through");reset(f);
    }
    private static void charge(AllGrimoireChecks.Fixture f){reset(f);var p=f.base.owner;var m=f.base.mana;var service=f.base.game.items().specialTools();p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"death_scythe"));
        for(int n=0;n<2;n++){var victim=cow(f,f.at.add(3,0,n),10);require(victim.hurtServer(f.level,f.level.damageSources().playerAttack(p),1000),"native lethal callback");require(service.charges(p)==(n==1?1:0),"one charge per two actual kills");}
        require(m.hudValue(p,"swordcharging3")==0&&service.hud(p).getString().contains("x1"),"original live charge/progress HUD");
        p.setItemInHand(InteractionHand.OFF_HAND,p.getMainHandItem());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var victim=cow(f,f.at.add(3,0,3),10);victim.hurtServer(f.level,f.level.damageSources().playerAttack(p),1000);
        require(service.charges(p)==1&&m.hudValue(p,"swordcharging3")==0,"offhand-only scythe does not change source mainhand kill criterion");
        var environmental=cow(f,f.at.add(3,0,5),10);environmental.hurtServer(f.level,f.level.damageSources().playerAttack(p),1);
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"death_scythe"));environmental.setInvulnerableTime(0);environmental.damageCooldownTime=0;
        environmental.hurtServer(f.level,f.level.damageSources().cactus(),1000);require(m.hudValue(p,"swordcharging3")==1,"native credited environmental death still counts while holding scythe");
        reset(f);
    }
    private static void beam(AllGrimoireChecks.Fixture f,InteractionHand hand){reset(f);var p=f.base.owner;var g=f.base.game;var service=g.items().specialTools();p.setItemInHand(hand,loot(f,"death_scythe"));f.base.mana.set(p,-10000);f.base.mana.score(p,"swordcharged3").set(1);
        var victim=cow(f,f.at.add(0,0,12),100);var immune=cow(f,f.at.add(1,0,12),100);immune.entityTags().add("sin_greed_active");var technical=cow(f,f.at.add(-1,0,12),100);technical.entityTags().add("strict_map_object");
        f.level.setBlock(BlockPos.containing(f.at.add(0,p.getEyeHeight(),4)),Blocks.STONE.defaultBlockState(),2|16|32);
        require(f.base.input(hand)&&service.charges(p)==0&&f.base.mana.get(p)==-10000,"actual native "+hand+" use consumes one charge, no mana gate/debit");
        require(victim.getHealth()==85&&victim.getKillCredit()==p&&victim.getLastDamageSource().is(DamageTypes.CACTUS),"15 cactus behind wall, native HurtTime and owner");
        require(immune.getHealth()==100&&technical.getHealth()==100,"Greed and service roles remain protected");
        require(f.base.recording.packets.stream().filter(k->k instanceof ClientboundSoundPacket).count()>=6,"six source Warden impact sounds");
        var advancement=g.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/hoes/death_skytche"));require(advancement!=null&&p.getAdvancements().getOrStartProgress(advancement).isDone(),"original skill-unlock achievement awarded");
        while(service.pending()>0)g.items().stonekivi().tick();
        require(!f.base.input(hand),"empty charge cannot recast");
        f.base.mana.score(p,"swordcharged3").set(1);p.entityTags().add("nomagic_active");require(!f.base.input(hand)&&service.charges(p)==1,"antimagic denies without charge debit");p.entityTags().remove("nomagic_active");
        p.entityTags().add("time_stopped");require(!f.base.input(hand)&&service.charges(p)==1,"personal time stop denies without charge debit");p.entityTags().remove("time_stopped");reset(f);
    }
    private static void terrain(AllGrimoireChecks.Fixture f){reset(f);var g=f.base.game;var p=f.base.owner;var service=g.items().specialTools();
        var center=BlockPos.containing(f.at.add(0,p.getEyeHeight(),12));int before=0;
        for(var pos:BlockPos.betweenClosed(center.offset(-4,-3,-3),center.offset(4,3,3))){f.level.setBlock(pos,Blocks.STONE.defaultBlockState(),2|16|32);before++;}
        p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"death_scythe"));f.base.mana.score(p,"swordcharged3").set(1);require(f.base.input(InteractionHand.MAIN_HAND),"terrain native cast");
        for(int i=0;i<100&&service.pending()>0;i++)g.items().stonekivi().tick();require(service.pending()==0,"terrain completes without artificial spell delay");
        int after=0;for(var pos:BlockPos.betweenClosed(center.offset(-4,-3,-3),center.offset(4,3,3)))if(f.level.getBlockState(pos).is(Blocks.STONE))after++;
        require(after>0&&after<before,"source8% overlapping integrity rolls leave dithered remains");reset(f);
    }
    /** Native disconnect path sends its real packet; the fixture has no physical socket to close. */
    private static final class KickRecording extends net.minecraft.network.Connection {
        final net.minecraft.server.level.ServerPlayer player;final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        KickRecording(net.minecraft.server.level.ServerPlayer p){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);player=p;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public net.minecraft.network.PacketListener getPacketListener(){return player.connection;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){}
        @Override public void setReadOnly(){}
        @Override public void handleDisconnection(){}
        long kicks(){return packets.stream().filter(p->p instanceof net.minecraft.network.protocol.common.ClientboundDisconnectPacket).count();}
    }
    private static void hammer(AllGrimoireChecks.Fixture f){reset(f);var g=f.base.game;var p=f.base.owner;var service=g.items().specialTools();var mana=f.base.mana;
        var weapon=loot(f,"banhammer");require(weapon.is(LbcSpecialTools.BANHAMMER)&&Identifier.parse("lbc:banhammer").equals(weapon.get(DataComponents.ITEM_MODEL)),"source Banhammer model/native item");p.setItemInHand(InteractionHand.MAIN_HAND,weapon);
        var victim=cow(f,f.at.add(3,0,0),1000);
        for(int n=0;n<15;n++){victim.setInvulnerableTime(0);victim.damageCooldownTime=0;require(victim.hurtServer(f.level,f.level.damageSources().playerAttack(p),1),"real accepted hammer strike");}
        require(mana.hudValue(p,"swordcharging7")==0&&mana.hudValue(p,"swordcharged7")==1,"15 actual strikes produce one hammer charge");victim.discard();
        var oldSkill=g.players().get(p.getUUID()).lbcSkill;var oldBetaSkill=g.players().get(f.beta.getUUID()).lbcSkill;boolean enabled=g.config().lbcSkills;
        var oldConnection=p.connection;var oldBetaConnection=f.beta.connection;var capture=new KickRecording(p);var betaCapture=new KickRecording(f.beta);
        var board=g.server().getScoreboard();var originalTeam=p.getTeam();var betaTeam=f.beta.getTeam();var team=board.addPlayerTeam("codex_banhammer");
        try{
            p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(g.server(),capture,p,new net.minecraft.server.network.CommonListenerCookie(p.getGameProfile(),0,p.clientInformation(),false));
            f.beta.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(g.server(),betaCapture,f.beta,new net.minecraft.server.network.CommonListenerCookie(f.beta.getGameProfile(),0,f.beta.clientInformation(),false));
            board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(f.beta.getScoreboardName(),team);
            g.config().lbcSkills=true;g.players().get(p.getUUID()).lbcSkill=12;g.players().get(f.beta.getUUID()).lbcSkill=0;
            f.beta.setPos(f.at.add(0,0,4));p.setXRot(25);mana.set(p,-10000);
            // A wall forces a deterministic resident first hit; friendly player bodies are eligible too.
            f.level.setBlock(BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(3))),Blocks.STONE.defaultBlockState(),2|16|32);
            require(f.base.input(InteractionHand.MAIN_HAND)&&service.hammerCount()==1&&mana.get(p)==-10000&&mana.hudValue(p,"swordcharged7")==0,"actual hammer cast, one charge and no mana gate");
            var h=service.hammerSnapshot().getFirst();require(h.point.subtract(h.impact).equals(new Vec3(0,200,0))&&h.display.getScale().equals(new org.joml.Vector3f(4)),"source200-block fall and4x display scale");
            var spawn=h.point;for(int i=0;i<10;i++)service.tick();
            require(!h.impacted&&betaCapture.kicks()==0&&h.point.equals(spawn.add(0,-20,0)),"unobstructed native fall remains exactly2 blocks per tick");
            f.beta.setPos(h.impact.add(1,0,0));p.setPos(h.impact.add(-1,0,0));
            for(int i=0;i<100&&!h.impacted;i++)service.tick();
            require(h.impacted&&h.point.equals(h.impact.add(0,2,0))&&capture.kicks()==0&&betaCapture.kicks()==1,"native first-surface disconnect hits ally, skill12 protects caster, display bottom rests on contact");
            service.impact(h);require(betaCapture.kicks()==1,"one disconnect per impact");
            var disconnect=betaCapture.packets.stream().filter(k->k instanceof net.minecraft.network.protocol.common.ClientboundDisconnectPacket).map(k->(net.minecraft.network.protocol.common.ClientboundDisconnectPacket)k).findFirst().orElseThrow();
            require(disconnect.reason().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr&&tr.getKey().equals("skywars2.banhammer.kick"),"translated native disconnect reason");
            p.entityTags().add("sin_greed_active");g.players().get(p.getUUID()).lbcSkill=0;require(service.immuneToHammer(p),"absolute active Greed protection");p.entityTags().remove("sin_greed_active");
            for(int i=0;i<200&&h.age<200;i++)service.tick();require(h.age==200&&service.hammerCount()==0,"source200-tick display expiry");
            g.config().lbcSkills=false;require(!service.immuneToHammer(p),"disabled skills do not confer hammer immunity");
            // The same native impact must be able to disconnect its own author.
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,weapon);p.setPos(f.at);p.setXRot(25);mana.score(p,"swordcharged7").set(1);
            f.level.setBlock(BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(3))),Blocks.STONE.defaultBlockState(),2|16|32);
            require(f.base.input(InteractionHand.OFF_HAND),"offhand native hammer cast");var second=service.hammerSnapshot().getFirst();p.setPos(second.impact);f.beta.setPos(second.impact.add(10,0,0));
            p.entityTags().add("sin_greed_active");service.impact(second);require(capture.kicks()==0&&betaCapture.kicks()==1,"actual WILD impact preserves absolute active Greed");p.entityTags().remove("sin_greed_active");
            second.destroy();p.setPos(f.at);p.setXRot(25);mana.score(p,"swordcharged7").set(1);
            f.level.setBlock(BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(3))),Blocks.STONE.defaultBlockState(),2|16|32);
            require(f.base.input(InteractionHand.OFF_HAND),"final native self-impact cast");var third=service.hammerSnapshot().getFirst();p.setPos(third.impact);f.beta.setPos(third.impact.add(10,0,0));service.impact(third);require(capture.kicks()==1&&betaCapture.kicks()==1,"WILD impact includes author and respects radius");
        }finally{
            p.entityTags().remove("sin_greed_active");p.connection=oldConnection;f.beta.connection=oldBetaConnection;g.config().lbcSkills=enabled;g.players().get(p.getUUID()).lbcSkill=oldSkill;g.players().get(f.beta.getUUID()).lbcSkill=oldBetaSkill;
            board.removePlayerFromTeam(p.getScoreboardName());board.removePlayerFromTeam(f.beta.getScoreboardName());board.removePlayerTeam(team);if(originalTeam!=null)board.addPlayerToTeam(p.getScoreboardName(),originalTeam);if(betaTeam!=null)board.addPlayerToTeam(f.beta.getScoreboardName(),betaTeam);service.stop();reset(f);
        }
    }
    public static void run(GameController g){try(var f=new AllGrimoireChecks.Fixture(g)){try{presentation(f);mortis(f);charge(f);for(var hand:InteractionHand.values())beam(f,hand);terrain(f);hammer(f);}finally{g.items().specialTools().stop();}}SkyWars2.LOGGER.info("[check] PASS special-tools");}
    private static AllGrimoireChecks.Fixture retained;
    public static void stopProbe(GameController g){require(retained==null,"exclusive stop fixture");var f=new AllGrimoireChecks.Fixture(g,true);try{reset(f);var p=f.base.owner;var center=BlockPos.containing(f.at.add(0,p.getEyeHeight(),12));
        for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-8),center.offset(5,5,8)))f.level.setBlock(pos,Blocks.STONE.defaultBlockState(),2|16|32);
        p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"banhammer"));p.setXRot(25);f.base.mana.score(p,"swordcharged7").set(1);require(f.base.input(InteractionHand.MAIN_HAND)&&g.items().specialTools().hammerCount()==1,"real hammer survives into stop");p.setXRot(0);
        p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"death_scythe"));f.base.mana.score(p,"swordcharged3").set(2);require(f.base.input(InteractionHand.MAIN_HAND),"retained real terrain cast");require(g.items().specialTools().pending()>0,"pending terrain crosses actual match stop");retained=f;
    }catch(Throwable e){g.items().specialTools().stop();f.close();throw e;}SkyWars2.LOGGER.info("[check] PASS special-tools-stop-probe");}
    public static void cleanup(GameController g){require(retained!=null,"actual stop fixture");try{require(g.items().specialTools().pending()==0&&g.items().specialTools().hammerCount()==0&&g.items().specialTools().charges(retained.base.owner)==0,"actual match stop cancels terrain, display and charge state");}finally{retained.close();retained=null;}SkyWars2.LOGGER.info("[check] PASS special-tools-cleanup");}
}
