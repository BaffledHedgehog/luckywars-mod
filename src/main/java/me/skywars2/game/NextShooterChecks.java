package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Native match fixtures for the two bounded shooter ports. No alternate combat simulation. */
public final class NextShooterChecks {
    private NextShooterChecks() { }
    private static final BlockPos BASE=new BlockPos(32,260,32);
    private static String stopEpoch;
    private static UUID stopOwner;
    private static Entity stopCharge,stopArrow;
    private static void require(boolean test,String message) {
        if(!test) throw new IllegalStateException("Next shooters: "+message);
    }
    private static ServerPlayer player(GameController game,String name) {
        var result=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name));
        require(result!=null,"requires participant "+name); return result;
    }
    private static ItemStack source(GameController game,ServerPlayer owner,String id) {
        var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+id);
        require(loot.size()==1 && loot.getFirst().getCount()==1,id+" source table yields exactly one item");
        return loot.getFirst();
    }
    private static boolean key(Component text,String value) {
        return text.getContents() instanceof TranslatableContents c && c.getKey().equals(value)
                || text.getSiblings().stream().anyMatch(part->key(part,value));
    }
    private static ItemStack client(ServerPlayer viewer,ItemStack source) {
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
            @Override public net.minecraft.network.PacketListener getPacketListener() { return viewer.connection; }
        };
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)connection;
        ItemStack[] result={ItemStack.EMPTY};
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->
            result[0]=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(source,
                net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),viewer.registryAccess()));
        return result[0];
    }
    public static void menu(GameController game) {
        require(game.inLobby(),"presentation checks require lobby");
        var owner=player(game,"TestAlpha");
        for(var id:List.of("govnomet","helicopter_boss")) {
            boolean govno=id.equals("govnomet"); var stack=source(game,owner,id);
            require(govno?stack.is(LbcGovnomet.ITEM):stack.is(LbcShooters.BOSS_HELICOPTER),id+" native registry identity");
            require(govno?LbcGovnomet.isGovnomet(stack):LbcShooters.isBossHelicopter(stack),id+" native matching");
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            for(String flag:govno?new String[]{"govnoq","transformable","shooter","magicwand","craftusable","govnomet"}
                    :new String[]{"transformable","shooter","spellwithcooldown","magicwand","boss_helicopter"})
                require(tag.getIntOr(flag,0)==1,id+" source marker "+flag);
            require(net.minecraft.resources.Identifier.parse("lbc:"+id).equals(stack.get(DataComponents.ITEM_MODEL))
                    && net.minecraft.resources.Identifier.parse("lbc:tier_"+(govno?"d":"s")).equals(stack.get(DataComponents.TOOLTIP_STYLE))
                    && key(stack.getHoverName(),govno?"shit_cannon":"boss_helicopter"),id+" source name/model/tier");
            var lore=stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines();
            require(lore.size()==(govno?4:6) && key(lore.getFirst(),"type") && key(lore.getFirst(),"shooter"),id+" source lore shape");
            if(govno) require(key(lore.get(1),"shit_cannon.lore") && key(lore.get(2),"shitgun.lore")
                    && lore.get(3).getString().equals("Nightmare_23 Beta Server Harem event"),"Govnomet source lore and credit");
            else {
                require(lore.get(1).getString().startsWith("30 ") && key(lore.get(1),"recharge_time"),"Boss recharge row");
                for(int i=1;i<=3;i++) require(key(lore.get(i+1),"boss_helicopter.lore."+i),"Boss source lore "+i);
                require(lore.get(5).getString().equals("Akim04ka Beta Server Harem event"),"Boss literal credit");
            }
            var view=client(owner,stack);
            require(view.is(Items.CARROT_ON_A_STICK) && stack.get(DataComponents.ITEM_MODEL).equals(view.get(DataComponents.ITEM_MODEL))
                    && stack.get(DataComponents.TOOLTIP_STYLE).equals(view.get(DataComponents.TOOLTIP_STYLE)),id+" Polymer client carrier/model/tier");
            var legacy=new ItemStack(Items.CARROT_ON_A_STICK); legacy.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            require(govno?LbcGovnomet.isGovnomet(legacy):LbcShooters.isBossHelicopter(legacy),id+" legacy matching");
            var unrelated=new ItemStack(Items.CARROT_ON_A_STICK);
            require(govno?!LbcGovnomet.isGovnomet(unrelated):!LbcShooters.isBossHelicopter(unrelated),id+" rejects ordinary carrier");
        }
        SkyWars2.LOGGER.info("[check] PASS next-shooters-menu");
    }
    private static void aim(GameController game,ServerPlayer owner) {
        owner.teleportTo(game.maps().level(),BASE.getX()+.5,BASE.getY(),BASE.getZ()+.5,Set.of(),0,0,true);
        owner.setDeltaMovement(Vec3.ZERO); owner.setShiftKeyDown(false);
    }
    private static void prepare(GameController game,ServerPlayer owner) {
        owner.closeContainer(); owner.getInventory().clearContent(); owner.getInventory().setSelectedSlot(0);
        owner.removeAllEffects(); owner.setGameMode(GameType.SURVIVAL); owner.setNoGravity(true);
        owner.entityTags().removeAll(Set.of("developer","time_stopped","nomagic_active"));
        aim(game,owner); game.items().mana().noMagic(owner,0); game.items().mana().set(owner,-25);
        game.items().mana().inventoryChanged(owner); game.items().magic().inventoryChanged(owner);
        game.items().magic().flushCleansing();
    }
    private static void use(ServerPlayer owner,InteractionHand hand) {
        owner.connection.handleClientTickEnd(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.INSTANCE);
        owner.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(hand,0,owner.getYRot(),owner.getXRot()));
    }
    private static List<Arrow> arrows(ServerPlayer owner) {
        var list=new ArrayList<Arrow>();
        for(var entity:owner.level().getAllEntities()) if(entity instanceof Arrow a && a.getOwner()==owner
                && !a.isRemoved() && a.entityTags().contains(LbcShooters.BOSS_HELICOPTER_PROJECTILE_TAG)) list.add(a);
        return list;
    }
    private static void bossTicks(LbcShooters shooters,int count) { for(int i=0;i<count;i++) shooters.tickBossHelicopters(); }
    private static Cow cow(GameController game,List<Cow> cows,double x,double z) {
        var cow=GameChecks.frostProbeCow(game.maps().level(),cows,BASE.getX()+x,BASE.getY()+.5,BASE.getZ()+z);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100); return cow;
    }
    public static void run(GameController game) {
        var owner=player(game,"TestAlpha"); var other=player(game,"TestBeta");
        require(game.playing() && game.allowDamage(owner) && game.allowDamage(other),"requires two active participants");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());
        var level=game.maps().level(); var shooters=game.items().shooters(); var govno=game.items().govnomet();
        var cells=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>(); var cows=new ArrayList<Cow>();
        var initial=new java.util.HashSet<UUID>(); level.getAllEntities().forEach(e->initial.add(e.getUUID()));
        boolean offhand=game.config().offhandWands,skills=game.config().lbcSkills;
        var profile=game.players().get(owner.getUUID()); int skill=profile.lbcSkill;
        try {
            shooters.forget(owner); shooters.forget(other); govno.forget(owner); govno.forget(other);
            prepare(game,owner); prepare(game,other); game.config().offhandWands=true; game.config().lbcSkills=false;
            other.teleportTo(level,BASE.getX()+40,260,BASE.getZ()+40,Set.of(),0,0,true);
            for(var cell:BlockPos.betweenClosed(BASE.offset(-6,-4,-4),BASE.offset(6,6,24))) {
                var pos=cell.immutable(); level.getChunk(pos.getX()>>4,pos.getZ()>>4);
                cells.put(pos,game.maps().snapshot(pos)); level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            var boss=source(game,owner,"helicopter_boss"); owner.setItemInHand(InteractionHand.MAIN_HAND,boss.copy());
            owner.connection.handlePunch(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE);
            require(shooters.bossHelicopterCooldownRemaining(owner)==0,"left click does not arm Boss");
            use(owner,InteractionHand.MAIN_HAND);
            require(shooters.bossHelicopterCooldownRemaining(owner)==600 && game.items().mana().get(owner)==-25,"Boss right click arms600 without mana");
            use(owner,InteractionHand.MAIN_HAND); require(shooters.bossHelicopterCooldownRemaining(owner)==600,"Boss input spam preserves timer");
            bossTicks(shooters,1); var emitted=arrows(owner);
            require(emitted.size()==2 && shooters.bossHelicopterCooldownRemaining(owner)==599,"600 emits two arrows before decrement");
            for(var arrow:emitted) require(arrow.getOwner()==owner && arrow.getPierceLevel()==10
                    && arrow.pickup==net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.CREATIVE_ONLY
                    && Math.abs(arrow.getDeltaMovement().length()-6)<.002
                    && Math.abs(Math.abs(arrow.getX()-owner.getX())-.75)<.0001,"Boss native owner/speed/piercing/main muzzle");
            owner.entityTags().add("time_stopped"); bossTicks(shooters,4);
            require(shooters.bossHelicopterCooldownRemaining(owner)==599 && arrows(owner).size()==2,"time stop pauses Boss timer and arrows");
            owner.entityTags().remove("time_stopped"); owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            bossTicks(shooters,3); require(arrows(owner).size()==2 && shooters.bossHelicopterCooldownRemaining(owner)==596,"unheld Boss recharges without shooting");
            owner.setItemInHand(InteractionHand.OFF_HAND,boss.copy()); bossTicks(shooters,1);
            require(arrows(owner).size()==4 && arrows(owner).stream().anyMatch(a->Math.abs(Math.abs(a.getX()-owner.getX())-1.45)<.0001),"offhand resumes with original muzzle");
            shooters.forget(owner); owner.setItemInHand(InteractionHand.MAIN_HAND,boss.copy());
            owner.setItemInHand(InteractionHand.OFF_HAND,boss.copy()); use(owner,InteractionHand.OFF_HAND);
            require(shooters.bossHelicopterCooldownRemaining(owner)==600,"offhand right click arms shared Boss timer");
            other.setItemInHand(InteractionHand.MAIN_HAND,source(game,other,"helicopter_boss")); use(other,InteractionHand.MAIN_HAND);
            require(shooters.bossHelicopterCooldownRemaining(other)==600,"second participant has independent timer");
            bossTicks(shooters,201);
            require(shooters.bossHelicopterCooldownRemaining(owner)==399 && shooters.bossHelicopterCooldownRemaining(other)==399
                    && arrows(owner).size()==804 && arrows(other).size()==402,"600 through400 inclusive emits402 per hand over201 real ticks");
            bossTicks(shooters,1); require(arrows(owner).size()==804 && arrows(other).size()==402,"399 is recovery only");
            require(game.items().mana().get(owner)==-25 && boss.getCount()==1,"burst leaves mana and weapon intact");
            shooters.forget(owner); shooters.forget(other); owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            bossCollision(game,owner,boss,cows);
            bossSuppression(game,owner,boss);
            govnoFlight(game,owner,other,cows);
            SkyWars2.LOGGER.info("[check] PASS next-shooters");
        } finally {
            shooters.forget(owner); shooters.forget(other); govno.forget(owner); govno.forget(other);
            cows.forEach(Entity::discard);
            for(var entity:List.copyOf(level.getEntitiesOfClass(Entity.class,new AABB(Vec3.atLowerCornerOf(BASE.offset(-10,-10,-10)),Vec3.atLowerCornerOf(BASE.offset(12,12,35))))))
                if(!initial.contains(entity.getUUID()) && !(entity instanceof ServerPlayer)) entity.discard();
            cells.forEach((pos,snapshot)->{game.maps().restoreSnapshot(pos,snapshot); game.items().magic().blockChanged(pos,level.getBlockState(pos));});
            game.config().offhandWands=offhand; game.config().lbcSkills=skills; profile.lbcSkill=skill;
            saved.restore(game); otherSaved.restore(game);
        }
    }
    private static void bossCollision(GameController game,ServerPlayer owner,ItemStack boss,List<Cow> cows) {
        var shooters=game.items().shooters(); aim(game,owner); owner.setItemInHand(InteractionHand.MAIN_HAND,boss.copy());
        require(shooters.beginBossHelicopter(owner,InteractionHand.MAIN_HAND),"native collision fixture arms Boss");
        bossTicks(shooters,1); var arrow=arrows(owner).getFirst(); var victim=cow(game,cows,.5,5.5);
        arrow.setPos(BASE.getX()+.5,BASE.getY()+1,BASE.getZ()+2); arrow.setDeltaMovement(0,0,6); LbcShooters.orient(arrow); arrow.tick();
        require(victim.getHealth()<100 && victim.getLastDamageSource()!=null && victim.getLastDamageSource().getEntity()==owner,"native arrow tick directly damages with shooter ownership");
        cows.forEach(Entity::discard); cows.clear(); shooters.forget(owner);
        require(shooters.beginBossHelicopter(owner,InteractionHand.MAIN_HAND),"terrain fixture arms Boss"); bossTicks(shooters,1);
        arrow=arrows(owner).getFirst(); var wall=BASE.offset(0,1,5); game.maps().level().setBlock(wall,Blocks.DIRT.defaultBlockState(),3);
        game.maps().level().setBlock(wall.above(),Blocks.STONE.defaultBlockState(),3);
        arrow.setPos(BASE.getX()+.5,BASE.getY()+1.5,BASE.getZ()+2); arrow.setDeltaMovement(0,0,6); LbcShooters.orient(arrow); arrow.tick();
        require(arrow.isRemoved() && game.maps().level().getBlockState(wall).isAir()
                && game.maps().level().getBlockState(wall.above()).is(Blocks.STONE),"native block collision drills tiny contact cell and removes Boss arrow");
        game.maps().level().setBlock(wall.above(),Blocks.AIR.defaultBlockState(),2); shooters.forget(owner);
    }
    private static void govnoFlight(GameController game,ServerPlayer owner,ServerPlayer other,List<Cow> cows) {
        var service=game.items().govnomet(); var gun=source(game,owner,"govnomet"); aim(game,owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND,gun.copy());
        owner.connection.handlePunch(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE);
        require(service.count()==0,"left click emits no Govnomet projectile");
        use(owner,InteractionHand.MAIN_HAND); require(service.count()==1,"right click emits one normal Govnomet charge");
        var charge=service.snapshot().getFirst(); var start=charge.position();
        require(charge.getOwner()==owner && !charge.mega && charge.getYRot()>=-20 && charge.getYRot()<=20
                && charge.getXRot()>=-10 && charge.getXRot()<=10,"normal charge has actual owner and source spread bounds");
        owner.setYRot(160); other.setYRot(-90); service.tick();
        require(Math.abs(charge.position().distanceTo(start)-2)<.0001 && charge.age==1,"charge advances2 using captured firing orientation");
        var at=charge.position(); owner.entityTags().add("time_stopped"); service.tick();
        require(charge.position().equals(at) && charge.age==1,"time stop pauses Govnomet age and movement");
        owner.entityTags().remove("time_stopped"); service.forget(owner);
        aim(game,owner); owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY); owner.setItemInHand(InteractionHand.OFF_HAND,gun.copy());
        use(owner,InteractionHand.OFF_HAND); require(service.count()==1,"offhand right click fires Govnomet"); service.forget(owner);
        // Enabled skill9 selects the source no-spread variant; no duplicated projectile math in this fixture.
        game.config().lbcSkills=true; game.players().get(owner.getUUID()).lbcSkill=9;
        owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY); owner.setItemInHand(InteractionHand.MAIN_HAND,gun.copy()); aim(game,owner);
        require(service.use(owner,InteractionHand.MAIN_HAND),"skill9 fires at negative mana");
        charge=service.snapshot().getFirst(); require(charge.mega,"enabled skill9 selects mega");
        start=charge.position(); service.tick(); require(Math.abs(charge.getX()-start.x)<.0001 && Math.abs(charge.getZ()-start.z-2)<.0001,"mega preserves owner yaw without spread");
        service.forget(owner);
        // A real clear downward flight exercises all fifty service ticks without crossing an unloaded chunk.
        var column=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        try {
            for(int y=BASE.getY()-102;y<=BASE.getY()+2;y++) {
                var pos=new BlockPos(BASE.getX(),y,BASE.getZ()); column.put(pos,game.maps().snapshot(pos));
                game.maps().level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            aim(game,owner); owner.setXRot(90); require(service.use(owner,InteractionHand.MAIN_HAND),"TTL fixture fires real mega charge");
            charge=service.snapshot().getFirst();
            for(int i=0;i<49;i++) service.tick();
            require(service.count()==1 && charge.age==49 && !charge.isRemoved(),"Govnomet survives forty-nine active flight ticks");
            service.tick(); require(service.count()==0 && charge.isRemoved() && charge.age==50,"fiftieth active tick expires charge");
        } finally { service.forget(owner); column.forEach(game.maps()::restoreSnapshot); }
        game.config().lbcSkills=false;
        // A wide close wall catches every legal normal spread, and proves one impact only.
        var wall=BASE.offset(0,1,5);
        for(var pos:BlockPos.betweenClosed(wall.offset(-3,-2,0),wall.offset(3,2,0))) game.maps().level().setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        aim(game,owner); require(service.use(owner,InteractionHand.MAIN_HAND),"normal wall fixture fires");
        for(int i=0;i<5 && service.count()>0;i++) service.tick();
        require(service.count()==0,"first wall terminates normal charge");
        long dirt=0; for(var pos:BlockPos.betweenClosed(wall.offset(-4,-3,-1),wall.offset(4,3,1))) if(game.maps().level().getBlockState(pos).is(Blocks.DIRT)) dirt++;
        require(dirt==1,"one wall contact creates exactly one dirt cell");
        for(var pos:BlockPos.betweenClosed(wall.offset(-4,-3,-1),wall.offset(4,3,1))) game.maps().level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        // Exercise the same chunk guard used by live flight without asking the level to load that chunk.
        aim(game,owner); require(service.use(owner,InteractionHand.MAIN_HAND),"unloaded boundary fixture fires");
        charge=service.snapshot().getFirst(); var unloaded=new BlockPos(1000000,260,1000000);
        require(!game.maps().level().hasChunkAt(unloaded),"boundary fixture starts with an unloaded chunk");
        charge.setPos(Vec3.atCenterOf(unloaded)); service.tick();
        require(charge.isRemoved() && service.count()==0 && !game.maps().level().hasChunkAt(unloaded),"flight discards at unloaded chunk without loading terrain: removed="+charge.isRemoved()+", tracked="+service.count()+", loaded="+game.maps().level().hasChunkAt(unloaded));
        normalStatus(game,owner,cows,gun);
        playerStatus(game,owner,other,gun);
        shield(game,owner,other,gun);
        megaImpact(game,owner,other,cows,gun);
        require(game.items().mana().get(owner)==-25,"Govnomet never spends mana");
    }
    private static void megaImpact(GameController game,ServerPlayer owner,ServerPlayer other,List<Cow> cows,ItemStack gun) {
        var service=game.items().govnomet(); var level=game.maps().level(); var board=game.server().getScoreboard();
        var ownerTeam=owner.getTeam(); var otherTeam=other.getTeam(); var team=board.addPlayerTeam("codex_next");
        team.setAllowFriendlyFire(false);
        try {
            board.addPlayerToTeam(owner.getScoreboardName(),team); board.addPlayerToTeam(other.getScoreboardName(),team);
            aim(game,owner); game.config().lbcSkills=true; owner.setItemInHand(InteractionHand.MAIN_HAND,gun.copy());
            other.removeAllEffects(); other.setHealth(other.getMaxHealth());
            other.teleportTo(level,BASE.getX()+1.2,BASE.getY(),BASE.getZ()+3.5,Set.of(),0,0,true);
            var friendly=cow(game,cows,.5,3.5); board.addPlayerToTeam(friendly.getScoreboardName(),team);
            var npc=cow(game,cows,1.5,3.5); npc.entityTags().add("magic_academy");
            float ownHealth=owner.getHealth(),otherHealth=other.getHealth();
            require(service.use(owner,InteractionHand.MAIN_HAND),"mega native target fixture fires");
            var charge=service.snapshot().getFirst();
            for(int i=0;i<3 && service.count()>0;i++) service.tick();
            require(service.count()==0 && service.fallingCount()==10,"first entity impact emits exactly ten native falling dirt blocks");
            var rubble=level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,new AABB(friendly.blockPosition()).inflate(4),
                    block->block.entityTags().contains("skywars2.govnomet.dirt"));
            require(rubble.size()==10 && rubble.stream().allMatch(block->game.items().electro().reflectedOwner(block)==owner
                    && !block.shouldBeSaved()),"all ten falling blocks carry shooter ownership and finite match lifecycle");
            require(friendly.getHealth()<100 && friendly.getLastDamageSource()!=null
                    && friendly.getLastDamageSource().getEntity()==owner,"friendly mob accepts owned native mega explosion");
            var vanilla=new net.minecraft.world.level.EntityBasedExplosionDamageCalculator(charge);
            var probe=new net.minecraft.world.level.ServerExplosion(level,charge,null,vanilla,charge.position(),1,false,
                    net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
            float exposure=net.minecraft.world.level.ServerExplosion.getSeenPercent(charge.position(),friendly);
            float expected=vanilla.getEntityDamageAmount(probe,friendly,exposure)*.2f;
            require(Math.abs((100-friendly.getHealth())-expected)<.0001,"accepted mega explosion health loss equals twenty percent of vanilla native explosion damage");
            require(!friendly.hasEffect(MobEffects.RESISTANCE) && !owner.hasEffect(MobEffects.RESISTANCE)
                    && !other.hasEffect(MobEffects.RESISTANCE),"mega blast grants no resistance status to nearby targets");
            wither(game,owner,friendly,60,"mega");
            require(owner.getHealth()==ownHealth && other.getHealth()==otherHealth && npc.getHealth()==100
                    && !owner.hasEffect(MobEffects.WITHER) && !other.hasEffect(MobEffects.WITHER)
                    && !other.hasEffect(MobEffects.NAUSEA) && !npc.hasEffect(MobEffects.WITHER),
                    "mega explosion protects shooter, allied player and service NPC");
            int rubbleCount=service.fallingCount(); service.tick();
            require(service.count()==0 && service.fallingCount()==rubbleCount,"terminal collision cannot impact again next tick");
        } finally {
            service.forget(owner); cows.forEach(Entity::discard); cows.clear(); board.removePlayerTeam(team);
            if(ownerTeam!=null) board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);
            if(otherTeam!=null) board.addPlayerToTeam(other.getScoreboardName(),otherTeam);
            other.teleportTo(level,BASE.getX()+40,260,BASE.getZ()+40,Set.of(),0,0,true);
        }
    }
    private static void bossSuppression(GameController game,ServerPlayer owner,ItemStack boss) {
        var shooters=game.items().shooters(); var level=game.maps().level(); var beacon=BASE.offset(2,1,0);
        try {
            aim(game,owner); owner.setItemInHand(InteractionHand.MAIN_HAND,boss.copy());
            owner.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));
            game.items().magic().inventoryChanged(owner); game.items().magic().flushCleansing();
            require(shooters.beginBossHelicopter(owner,InteractionHand.MAIN_HAND),"native antimagic artifact permits TECH Boss activation");
            bossTicks(shooters,2); require(arrows(owner).size()==4,"antimagic artifact permits ongoing TECH Boss volleys");
            owner.getInventory().setItem(9,ItemStack.EMPTY); game.items().magic().inventoryChanged(owner); game.items().magic().flushCleansing();
            level.setBlockAndUpdate(beacon,LbcMagic.MECHANISM_BEACON_BLOCK.defaultBlockState());
            game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            int count=arrows(owner).size(),timer=shooters.bossHelicopterCooldownRemaining(owner);
            bossTicks(shooters,2); require(arrows(owner).size()==count && shooters.bossHelicopterCooldownRemaining(owner)==timer-2,
                    "native mechanism beacon suppresses active Boss emissions while cooldown advances");
            shooters.forget(owner); require(!shooters.beginBossHelicopter(owner,InteractionHand.MAIN_HAND),"mechanism beacon rejects new Boss activation");
            level.setBlockAndUpdate(beacon,LbcMagic.MAGIC_BEACON_BLOCK.defaultBlockState());
            game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            require(shooters.beginBossHelicopter(owner,InteractionHand.MAIN_HAND),"native magic beacon permits TECH Boss activation");
            bossTicks(shooters,1); require(arrows(owner).size()==2,"magic beacon permits ongoing TECH Boss volley");
        } finally {
            shooters.forget(owner); owner.getInventory().setItem(9,ItemStack.EMPTY);
            level.setBlockAndUpdate(beacon,Blocks.AIR.defaultBlockState()); game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            game.items().magic().inventoryChanged(owner); game.items().magic().flushCleansing();
        }
    }
    private static void wither(GameController game,ServerPlayer owner,Cow target,int duration,String variant) {
        var effect=target.getEffect(MobEffects.WITHER);
        require(effect!=null && effect.getDuration()==duration && effect.getAmplifier()==10,variant+" accepts native WitherXI for"+duration+"ticks");
        target.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0); target.setLastHurtByMob(null);
        target.setInvulnerableTime(0); target.damageCooldownTime=0; float health=target.getHealth();
        MobEffects.WITHER.value().applyEffectTick(game.maps().level(),target,10);
        require(target.getHealth()<health && target.getKillCredit()==owner,variant+" native Wither tick inherits projectile shooter after prior damage credit is cleared");
    }
    private static void normalStatus(GameController game,ServerPlayer owner,List<Cow> cows,ItemStack gun) {
        var service=game.items().govnomet(); game.config().lbcSkills=false; aim(game,owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND,gun.copy()); require(service.use(owner,InteractionHand.MAIN_HAND),"normal status fixture fires");
        var charge=service.snapshot().getFirst(); var point=charge.position().add(charge.getDeltaMovement().normalize().scale(1.4));
        var target=GameChecks.frostProbeCow(game.maps().level(),cows,point.x,point.y-.6,point.z);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100); target.setHealth(100);
        service.tick(); require(service.count()==0 && target.getHealth()==100,"normal first entity contact applies status without added direct damage");
        wither(game,owner,target,20,"normal"); cows.forEach(Entity::discard); cows.clear();
        // Remove the impact dirt before testing a native shield against the same empty flight lane.
        game.maps().level().setBlock(BlockPos.containing(point),Blocks.AIR.defaultBlockState(),2);
        for(var pos:BlockPos.betweenClosed(BASE.offset(-2,0,0),BASE.offset(2,3,4)))
            if(game.maps().level().getBlockState(pos).is(Blocks.DIRT)) game.maps().level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
    }
    private static void shield(GameController game,ServerPlayer owner,ServerPlayer other,ItemStack gun) {
        var service=game.items().govnomet(); var level=game.maps().level();
        var oldTeam=other.getTeam();
        if(oldTeam!=null) game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName());
        try {
            aim(game,owner); other.removeAllEffects(); other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));
            other.teleportTo(level,BASE.getX()+.5,BASE.getY(),BASE.getZ()+4,Set.of(),180,0,true);
            other.startUsingItem(InteractionHand.MAIN_HAND); for(int i=0;i<6;i++) other.doTick();
            require(other.isBlocking() && other.getUseItem().is(Items.SHIELD),"ordinary vanilla shield completes actual native blocking delay");
            for(boolean mega:List.of(false,true)) {
                game.config().lbcSkills=mega; aim(game,owner); owner.setItemInHand(InteractionHand.MAIN_HAND,gun.copy());
                float health=other.getHealth(); long dirtBefore=dirt(level);
                require(service.use(owner,InteractionHand.MAIN_HAND),"shield fixture fires "+(mega?"mega":"normal"));
                for(int i=0;i<4 && service.count()>0;i++) service.tick();
                require(service.count()==0 && service.fallingCount()==0 && other.getHealth()==health
                        && !other.hasEffect(MobEffects.WITHER) && !other.hasEffect(MobEffects.NAUSEA) && dirt(level)==dirtBefore,
                        "ordinary blocking shield stops "+(mega?"mega":"normal")+" before dirt, rubble, damage or statuses");
            }
            // A native MISS travels beside the player's body and crosses the whole shield sphere.
            // Endpoint-only checks miss this fast acid; test actual native gravity/drag and the sweep.
            var from=new Vec3(BASE.getX()+1.8,BASE.getY()+1,BASE.getZ()+.5);
            game.items().clouds().magnumAcid(owner,from,new Vec3(0,0,1),false);
            var acids=level.getEntitiesOfClass(LbcClouds.AcidProjectile.class,new AABB(from,from).inflate(2),acid->acid.getOwner()==owner);
            require(acids.size()==6,"acid shield fixture creates the existing native six-charge profile");
            var acid=acids.getFirst();
            for(var extra:acids) if(extra!=acid) extra.discard();
            acid.setPos(from); acid.setDeltaMovement(0,0,8);
            float before=other.getHealth(); acid.tick();
            require(acid.isRemoved() && other.isBlocking() && other.getHealth()==before
                    && !other.hasEffect(MobEffects.POISON) && !other.isOnFire(),
                    "ordinary shield stops fast acid crossing its sphere on a native MISS before poison, burning or sticking");
        } finally {
            other.stopUsingItem(); other.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            if(oldTeam!=null) game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),oldTeam);
            other.teleportTo(level,BASE.getX()+40,260,BASE.getZ()+40,Set.of(),0,0,true); service.forget(owner);
        }
    }
    private static void playerStatus(GameController game,ServerPlayer owner,ServerPlayer other,ItemStack gun) {
        var service=game.items().govnomet(); var team=other.getTeam(); var board=game.server().getScoreboard();
        if(team!=null) board.removePlayerFromTeam(other.getScoreboardName());
        try {
            game.config().lbcSkills=false; aim(game,owner); other.removeAllEffects();
            owner.setItemInHand(InteractionHand.MAIN_HAND,gun.copy()); require(service.use(owner,InteractionHand.MAIN_HAND),"native player status fixture fires");
            var charge=service.snapshot().getFirst(); var point=charge.position().add(charge.getDeltaMovement().normalize().scale(1.4));
            other.teleportTo(game.maps().level(),point.x,point.y-1,point.z,Set.of(),180,0,true);
            service.tick(); var nausea=other.getEffect(MobEffects.NAUSEA); var wither=other.getEffect(MobEffects.WITHER);
            require(service.count()==0 && nausea!=null && nausea.getDuration()==300 && nausea.getAmplifier()==0
                    && wither!=null && wither.getDuration()==20 && wither.getAmplifier()==10,"native player contact accepts nausea15seconds and WitherXI1second");
        } finally {
            service.forget(owner); other.removeAllEffects();
            if(team!=null) board.addPlayerToTeam(other.getScoreboardName(),team);
            other.teleportTo(game.maps().level(),BASE.getX()+40,260,BASE.getZ()+40,Set.of(),0,0,true);
            for(var pos:BlockPos.betweenClosed(BASE.offset(-2,0,0),BASE.offset(2,3,4)))
                if(game.maps().level().getBlockState(pos).is(Blocks.DIRT)) game.maps().level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private static long dirt(net.minecraft.server.level.ServerLevel level) {
        long count=0; for(var pos:BlockPos.betweenClosed(BASE.offset(-5,-3,-2),BASE.offset(5,5,10))) if(level.getBlockState(pos).is(Blocks.DIRT)) count++;
        return count;
    }
    public static void stopProbe(GameController game) {
        var owner=player(game,"TestAlpha"); var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        boolean offhand=game.config().offhandWands;
        try {
            prepare(game,owner); game.config().offhandWands=true; owner.setXRot(-90);
            owner.setItemInHand(InteractionHand.MAIN_HAND,source(game,owner,"helicopter_boss"));
            require(game.items().shooters().beginBossHelicopter(owner,InteractionHand.MAIN_HAND),"stop probe arms Boss timer");
            bossTicks(game.items().shooters(),1); stopArrow=arrows(owner).getFirst();
            owner.setItemInHand(InteractionHand.MAIN_HAND,source(game,owner,"govnomet"));
            require(game.items().govnomet().use(owner,InteractionHand.MAIN_HAND),"stop probe emits live Govnomet charge");
            stopCharge=game.items().govnomet().snapshot().getFirst(); stopOwner=owner.getUUID(); stopEpoch=game.items().epoch();
        } finally { saved.restore(game); game.config().offhandWands=offhand; }
        owner.entityTags().add("time_stopped"); SkyWars2.LOGGER.info("[check] PASS next-shooters-stop-probe");
    }
    public static void cleanup(GameController game) {
        var owner=stopOwner==null?null:game.server().getPlayerList().getPlayer(stopOwner);
        require(game.inLobby() && stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && owner!=null
                && stopCharge!=null && stopCharge.isRemoved() && stopArrow!=null && stopArrow.isRemoved()
                && game.items().govnomet().count()==0 && game.items().shooters().bossHelicopterArrowCount()==0
                && game.items().shooters().bossHelicopterCooldownRemaining(owner)==0,"actual match stop invalidates epoch and clears native charges/arrows/timer");
        game.items().govnomet().loaded(stopCharge); game.items().shooters().loaded(stopArrow);
        require(stopCharge.isRemoved() && stopArrow.isRemoved() && game.items().govnomet().count()==0
                && game.items().shooters().bossHelicopterArrowCount()==0,"previous match projectiles cannot reenter tracking through load callbacks");
        owner.entityTags().remove("time_stopped"); stopOwner=null; stopEpoch=null; stopArrow=null; stopCharge=null;
        SkyWars2.LOGGER.info("[check] PASS next-shooters-cleanup");
    }
}
