package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Explicit live Magnum checks; shares player snapshots and native cow fixtures with GameChecks. */
public final class MagnumChecks {
    private MagnumChecks() { }
    private static final BlockPos BASE=new BlockPos(32,260,32);
    private static final String[] ROUNDS={"standard","incendiary","electric","acid","void","explosive"};
    private static String stopEpoch;
    private static UUID stopOwner;
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Magnum: "+message);
    }
    private static ServerPlayer player(GameController game,String name) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name));
        require(player!=null,"requires participant "+name); return player;
    }
    private static ItemStack gun(GameController game,ServerPlayer owner,int mode) {
        var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/magnum");
        require(loot.size()==1 && LbcMagnum.isMagnum(loot.getFirst()),"staged source gun exists");
        var stack=loot.getFirst(); mode(stack,mode); return stack;
    }
    private static void mode(ItemStack stack,int mode) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    private static ItemStack[] rounds(GameController game,ServerPlayer owner) {
        var found=new ItemStack[6];
        for(int roll=0;roll<200 && java.util.Arrays.stream(found).anyMatch(java.util.Objects::isNull);roll++)
            for(var stack:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/magnum_ammo"))
                for(int i=0;i<6;i++) if(stack.get(DataComponents.ITEM_MODEL)!=null
                        && stack.get(DataComponents.ITEM_MODEL).toString().equals("lbc:round_"+ROUNDS[i])) {
                    found[i]=stack.copy(); found[i].setCount(1);
                }
        require(java.util.Arrays.stream(found).noneMatch(java.util.Objects::isNull),"all six original ammo entries reachable");
        return found;
    }
    private static void aim(ServerPlayer owner,GameController game) {
        owner.teleportTo(game.maps().level(),BASE.getX()+.5,BASE.getY(),BASE.getZ()+.5,Set.of(),0,0,true);
        owner.setDeltaMovement(Vec3.ZERO); owner.setShiftKeyDown(false);
    }
    private static Cow cow(GameController game,List<Cow> cows,double z) {
        var cow=GameChecks.frostProbeCow(game.maps().level(),cows,BASE.getX()+.5,BASE.getY()+.5,BASE.getZ()+z);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100); return cow;
    }
    private static void ready(GameController game,ServerPlayer owner,ItemStack gun,ItemStack ammo,InteractionHand hand) {
        game.items().magnum().forget(owner); game.items().shooters().resetCooldown(owner);
        owner.getInventory().clearContent(); owner.setItemInHand(hand,gun); owner.getInventory().setItem(9,ammo.copy());
        aim(owner,game);
    }
    public static void run(GameController game) {
        var owner=player(game,"TestAlpha"); var other=player(game,"TestBeta");
        require(game.playing() && game.allowDamage(owner) && game.allowDamage(other),"requires active match");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());
        boolean offhand=game.config().offhandWands,shift=owner.isShiftKeyDown();
        var level=game.maps().level(); var service=game.items().magnum(); var shooters=game.items().shooters();
        var cells=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>(); var cows=new ArrayList<Cow>();
        var board=game.server().getScoreboard(); var ownerTeam=owner.getTeam(); var otherTeam=other.getTeam();
        var team=board.addPlayerTeam("codex_magnum"); team.setAllowFriendlyFire(false);
        var priorEntities=new java.util.HashSet<UUID>(); level.getAllEntities().forEach(e->priorEntities.add(e.getUUID()));
        try {
            owner.closeContainer(); owner.getInventory().clearContent(); owner.removeAllEffects();
            owner.setGameMode(GameType.SURVIVAL); owner.setNoGravity(true); owner.setHealth(owner.getMaxHealth());
            owner.entityTags().removeAll(Set.of("time_stopped","nomagic_active")); game.items().mana().noMagic(owner,0);
            game.config().offhandWands=true; game.items().mana().set(owner,0); aim(owner,game);
            other.teleportTo(level,BASE.getX()+40,260,BASE.getZ()+40,Set.of(),0,0,true);
            for(var cell:BlockPos.betweenClosed(BASE.offset(-6,-5,-5),BASE.offset(6,7,26))) {
                var pos=cell.immutable(); level.getChunk(pos.getX()>>4,pos.getZ()>>4);
                cells.put(pos,game.maps().snapshot(pos)); level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            var ammo=rounds(game,owner);
            ready(game,owner,gun(game,owner,0),ammo[3],InteractionHand.MAIN_HAND);
            require(!service.use(owner,InteractionHand.MAIN_HAND) && service.queued(owner)==0
                    && !owner.getInventory().getItem(9).isEmpty(),"wrong ammo neither fires nor consumes round");
            // Each mode starts with an actual staged round, one source ray, then cancels its remaining batch.
            for(int mode=0;mode<6;mode++) {
                var stack=gun(game,owner,mode); ready(game,owner,stack,ammo[mode],InteractionHand.MAIN_HAND);
                var first=cow(game,cows,5.5); var second=cow(game,cows,9.5);
                int arcs=game.items().wands().electroCount(),acid=game.items().clouds().acidCount();
                require(service.use(owner,InteractionHand.MAIN_HAND),"mode"+mode+" accepts correct round at zero mana");
                require(owner.getInventory().getItem(9).isEmpty() && service.queued(owner)==4,"one round buys initial shot plus four queued");
                if(mode==0 || mode==1 || mode==4) {
                    int damage=mode==0?16:mode==1?12:4;
                    require(first.getHealth()==100-damage && second.getHealth()==100-damage,"mode"+mode+" pierces once per entity"
                            +" expected="+(100-damage)+" first="+first.getHealth()+" second="+second.getHealth()
                            +" removed="+first.isRemoved()+"/"+second.isRemoved()
                            +" indexed="+level.getEntitiesOfClass(Cow.class,new AABB(first.position(),second.position()).inflate(2)).contains(first)
                            +"/"+level.getEntitiesOfClass(Cow.class,new AABB(first.position(),second.position()).inflate(2)).contains(second)
                            +" eye="+owner.getEyePosition()+" yaw/pitch="+owner.getYRot()+"/"+owner.getXRot());
                } else if(mode==3) require(first.getHealth()==94 && second.getHealth()==100
                        && game.items().clouds().acidCount()==acid+6,"acid direct6 stops at first hit and emits six normal charges");
                else if(mode==2) require(first.getHealth()==100 && second.getHealth()==100
                        && game.items().wands().electroCount()>arcs && (game.items().wands().electroCount()-arcs)%4==0,"electric first collision branches into existing four-arc groups");
                else require(first.getHealth()<100 && level.getEntitiesOfClass(PrimedTnt.class,new AABB(Vec3.atLowerCornerOf(BASE),Vec3.atLowerCornerOf(BASE.offset(15,15,25)))).isEmpty(),"explosive native damage creates no live TNT");
                if(mode!=2) require(first.getLastDamageSource()!=null && first.getLastDamageSource().getEntity()==owner,"mode"+mode+" direct damage carries owner");
                if(mode==1) require(first.isOnFire() && second.isOnFire(),"incendiary ignites pierced native targets");
                owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY); service.tick();
                require(service.queued(owner)==0,"unequipping cancels mode"+mode+" immediately");
                cows.forEach(Entity::discard); cows.clear(); game.items().wands().stop(); game.items().clouds().stop();
            }
            board.addPlayerToTeam(owner.getScoreboardName(),team); board.addPlayerToTeam(other.getScoreboardName(),team);
            ready(game,owner,gun(game,owner,0),ammo[0],InteractionHand.MAIN_HAND);
            other.setNoGravity(true); other.removeAllEffects(); other.setHealth(other.getMaxHealth());
            other.teleportTo(level,BASE.getX()+.5,260,BASE.getZ()+5.5,Set.of(),0,0,true);
            var npc=cow(game,cows,3.5); npc.entityTags().add("magic_academy"); var friendly=cow(game,cows,9.5);
            board.addPlayerToTeam(friendly.getScoreboardName(),team); float ownHealth=owner.getHealth(),allyHealth=other.getHealth();
            require(service.use(owner,InteractionHand.MAIN_HAND) && npc.getHealth()==100 && friendly.getHealth()==84
                    && owner.getHealth()==ownHealth && other.getHealth()==allyHealth,"native ray protects owner/allied player/NPC while hitting friendly mob");
            cows.forEach(Entity::discard); cows.clear();
            other.teleportTo(level,BASE.getX()+40,260,BASE.getZ()+40,Set.of(),0,0,true);
            var main=gun(game,owner,0); ready(game,owner,main,ammo[0],InteractionHand.MAIN_HAND);
            var spare=gun(game,owner,3); owner.setItemInHand(InteractionHand.OFF_HAND,spare);
            require(service.use(owner,InteractionHand.MAIN_HAND),"starts five-shot batch"); int shots=service.shots(owner);
            require(!service.use(owner,InteractionHand.OFF_HAND),"other hand shares cooldown");
            mode(main,1); // Queued shots retain the original standard profile even if the held stack changes mode.
            var target=cow(game,cows,5.5); aim(owner,game); service.tick();
            require(target.getHealth()==84 && !target.isOnFire(),"queued mode snapshot survives mode mutation");
            for(int i=0;i<3;i++) { aim(owner,game); service.tick(); }
            require(service.queued(owner)==0 && service.shots(owner)==shots+4 && owner.getInventory().getItem(9).isEmpty(),"exactly five shots consume one round");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.SLR));
            owner.getInventory().setItem(10,new ItemStack(net.minecraft.world.item.Items.ARROW));
            require(!shooters.use(owner,InteractionHand.MAIN_HAND),"Magnum cooldown also gates armed SLR");
            owner.setItemInHand(InteractionHand.MAIN_HAND,main);
            cows.forEach(Entity::discard); cows.clear();
            owner.setShiftKeyDown(true); int cooldown=shooters.cooldownRemaining(owner),balance=game.items().mana().get(owner);
            for(int i=0;i<6;i++) require(service.use(owner,InteractionHand.OFF_HAND),"free offhand cycle during cooldown");
            require(LbcMagnum.mode(spare)==3 && shooters.cooldownRemaining(owner)==cooldown && game.items().mana().get(owner)==balance,"six cycles preserve cooldown/mana and correct stack");
            owner.setShiftKeyDown(false); shooters.setCooldown(owner,25); shooters.setCooldown(other,25); shooters.tick();
            require(shooters.cooldownRemaining(owner)==24 && shooters.cooldownRemaining(other)==24,"shared cooldown decrements once per player");
            ready(game,owner,gun(game,owner,0),ammo[0],InteractionHand.OFF_HAND);
            require(service.use(owner,InteractionHand.OFF_HAND) && service.queued(owner)==4,"correct offhand round starts batch");
            service.purify(owner); require(shooters.cooldownRemaining(owner)==0 && service.queued(owner)==4,"Purify resets cooldown while preserving original batch");
            service.stop(); require(service.queued(owner)==0,"stop clears batch");
            // TECHNO gun admission ignores magic beacon, but obeys mechanism beacon.
            var beacon=BASE.offset(2,1,0);
            ready(game,owner,gun(game,owner,0),ammo[0],InteractionHand.MAIN_HAND);
            level.setBlockAndUpdate(beacon,LbcMagic.MAGIC_BEACON_BLOCK.defaultBlockState()); game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            require(service.use(owner,InteractionHand.MAIN_HAND),"magic beacon does not jam technological gun");
            ready(game,owner,gun(game,owner,0),ammo[0],InteractionHand.MAIN_HAND);
            level.setBlockAndUpdate(beacon,LbcMagic.MECHANISM_BEACON_BLOCK.defaultBlockState()); game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            require(!service.use(owner,InteractionHand.MAIN_HAND) && !owner.getInventory().getItem(9).isEmpty(),"mechanism beacon rejects before ammo debit");
            level.setBlockAndUpdate(beacon,Blocks.AIR.defaultBlockState()); game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            // Void erases bedrock along its path without dropping source blocks.
            var wall=BASE.offset(0,1,5); level.setBlock(wall,Blocks.BEDROCK.defaultBlockState(),2);
            ready(game,owner,gun(game,owner,4),ammo[4],InteractionHand.MAIN_HAND); var behind=cow(game,cows,9.5);
            int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(BASE),Vec3.atLowerCornerOf(BASE.offset(10,10,20)))).size();
            require(service.use(owner,InteractionHand.MAIN_HAND) && level.getBlockState(wall).isAir() && behind.getHealth()==96,"void deletes unbreakable terrain and continues");
            require(level.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(BASE),Vec3.atLowerCornerOf(BASE.offset(10,10,20)))).size()==drops,"void strict terrain erasure has no drops");
            service.forget(owner); cows.forEach(Entity::discard); cows.clear();
            // Wall profiles share the same exact contact point, but invoke different existing effects.
            level.setBlock(wall,Blocks.STONE.defaultBlockState(),2);
            ready(game,owner,gun(game,owner,3),ammo[3],InteractionHand.MAIN_HAND);
            int acidBefore=game.items().clouds().acidCount();
            require(service.use(owner,InteractionHand.MAIN_HAND) && game.items().clouds().acidCount()==acidBefore+1,
                    "acid wall contact emits exactly one big charge");
            require(level.getEntitiesOfClass(LbcClouds.AcidProjectile.class,new AABB(wall).inflate(2)).stream()
                    .anyMatch(acid->acid.getOwner()==owner && acid.entityTags().contains("raycastacidbig")
                            && acid.getDeltaMovement().y<0),"big wall acid retains owner and source downward aim");
            game.items().clouds().stop();
            ready(game,owner,gun(game,owner,1),ammo[1],InteractionHand.MAIN_HAND);
            require(service.use(owner,InteractionHand.MAIN_HAND) && game.items().blaze().terrainCount()>0,"incendiary wall contact queues shared fire cube");
            for(int i=0;i<8 && game.items().blaze().terrainCount()>0;i++) game.items().blaze().tick();
            require(game.items().blaze().terrainCount()==0,"finite incendiary cube drains shared terrain queue");
            // Remove fire so it cannot change the explosion fixture or acid ignition probe.
            for(var pos:BlockPos.betweenClosed(wall.offset(-2,-2,-2),wall.offset(2,2,2)))
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            level.setBlock(wall,Blocks.GLASS.defaultBlockState(),2);
            var radiusProbe=wall.offset(3,0,0); level.setBlock(radiusProbe,Blocks.DIRT.defaultBlockState(),2);
            ready(game,owner,gun(game,owner,5),ammo[5],InteractionHand.MAIN_HAND);
            require(service.use(owner,InteractionHand.MAIN_HAND) && level.getBlockState(wall).isAir()
                    && level.getBlockState(radiusProbe).isAir(),"wall explosion uses reachable radius4 profile");
            require(level.getEntitiesOfClass(PrimedTnt.class,new AABB(wall).inflate(8)).isEmpty(),"wall explosion has no added TNT entity");
            service.forget(owner);
            // Existing big acid profile retains its owned poison/fire effects, no replacement cloud engine.
            var acidVictim=cow(game,cows,6.5); var origin=acidVictim.position().add(0,.5,0);
            game.items().clouds().magnumAcid(owner,origin,new Vec3(0,1,0),true); game.items().clouds().tick();
            require(acidVictim.hasEffect(MobEffects.POISON) && acidVictim.getEffect(MobEffects.POISON).getDuration()==600,"big acid applies original30-second poison");
            for(var acid:level.getEntitiesOfClass(LbcClouds.AcidProjectile.class,new AABB(origin,origin).inflate(2))) acid.entityTags().add("on_fire");
            acidVictim.setInvulnerableTime(0); acidVictim.damageCooldownTime=0; game.items().clouds().tick();
            require(acidVictim.getHealth()==96 && acidVictim.getLastDamageSource().getEntity()==owner,"burning big acid applies four owned native fire damage");
            SkyWars2.LOGGER.info("[check] PASS magnum");
        } finally {
            service.forget(owner); service.forget(other); shooters.resetCooldown(owner); shooters.resetCooldown(other);
            game.items().wands().stop(); game.items().clouds().stop(); game.items().blaze().forget(owner); cows.forEach(Entity::discard);
            for(var entity:List.copyOf(level.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(BASE.offset(-8,-8,-8)),Vec3.atLowerCornerOf(BASE.offset(8,10,30))))))
                if(!priorEntities.contains(entity.getUUID())) entity.discard();
            cells.forEach((pos,snapshot)->{ game.maps().restoreSnapshot(pos,snapshot); game.items().magic().blockChanged(pos,level.getBlockState(pos)); });
            board.removePlayerTeam(team);
            if(ownerTeam!=null) board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);
            if(otherTeam!=null) board.addPlayerToTeam(other.getScoreboardName(),otherTeam);
            game.config().offhandWands=offhand; saved.restore(game); otherSaved.restore(game); owner.setShiftKeyDown(shift);
        }
    }
    public static void stopProbe(GameController game) {
        var owner=player(game,"TestAlpha"); var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        boolean offhand=game.config().offhandWands;
        try {
            owner.removeAllEffects(); owner.entityTags().remove("time_stopped"); game.config().offhandWands=true;
            owner.setGameMode(GameType.SURVIVAL); owner.setNoGravity(true); aim(owner,game);
            // Aim into empty sky so the real match-stop probe cannot alter arena terrain.
            owner.setXRot(-90); var ammo=rounds(game,owner);
            owner.getInventory().clearContent(); owner.setItemInHand(InteractionHand.MAIN_HAND,gun(game,owner,0)); owner.getInventory().setItem(9,ammo[0]);
            game.items().shooters().resetCooldown(owner);
            require(game.items().magnum().use(owner,InteractionHand.MAIN_HAND) && game.items().magnum().queued(owner)==4,"stop probe arms real batch");
            stopEpoch=game.items().epoch(); stopOwner=owner.getUUID();
        } finally { saved.restore(game); game.config().offhandWands=offhand; }
        owner.entityTags().add("time_stopped"); SkyWars2.LOGGER.info("[check] PASS magnum-stop-probe");
    }
    public static void cleanup(GameController game) {
        var owner=stopOwner==null?null:game.server().getPlayerList().getPlayer(stopOwner);
        require(stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && game.inLobby() && owner!=null
                && game.items().magnum().queued(owner)==0 && game.items().magnum().shots(owner)==0
                && game.items().shooters().cooldownRemaining(owner)==0,"actual match stop invalidates epoch and clears batch/counters/shared cooldown");
        owner.entityTags().remove("time_stopped"); stopEpoch=null; stopOwner=null;
        SkyWars2.LOGGER.info("[check] PASS magnum-cleanup");
    }
}
