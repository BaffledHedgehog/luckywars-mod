package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native fixtures, with no real wait for the forty high-altitude potions. */
public final class CageDropChecks {
    private CageDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Entity> stopActors=List.of();
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Cage drops: "+message);}
    private static CompoundTag saved(Entity entity,ServerLevel level){
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        entity.saveWithoutId(out);return out.buildResult();
    }
    private static ItemEntity token(ServerLevel level,Vec3 at,ItemStack stack,ServerPlayer author){
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);if(author!=null)item.setThrower(author);
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"real native token admission");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final net.minecraft.world.scores.PlayerTeam alphaTeam,betaTeam;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ItemEntity> tokens=new ArrayList<>();final Vec3 origin;final BlockPos center;
        final boolean griefing,tntExplodes,alphaNoPhysics,betaNoPhysics;final double armor;final int protection;
        final String[] flags={"lookupanvil","lookupfireball","lookuppotions","lookuplava","obsidiantrap"};
        final String[] ids={"skywars2:cage_drop_anvil","skywars2:cage_drop_fireball","skywars2:cage_drop_potions","skywars2:cage_drop_lava","skywars2:cage_drop_obsidian"};
        final String[] names={"trap_anvil","trap_fireball","trap_potion","trap_lava","trap_obsidian"};
        ItemEntity stack,paused,unloaded;Entity frozen;Vec3 frozenPos,frozenVelocity;int phase,lastTick,wait;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two active actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            var board=game.server().getScoreboard();alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());betaTeam=board.getPlayersTeam(beta.getScoreboardName());
            alphaNoPhysics=alpha.noPhysics;betaNoPhysics=beta.noPhysics;
            armor=beta.getAttribute(Attributes.ARMOR).getBaseValue();protection=game.config().tntProtection;
            griefing=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
            tntExplodes=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES);
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,240.7,Math.floor(alpha.getZ()/16)*16+8.81);center=BlockPos.containing(origin);
            level.getAllEntities().forEach(existing::add);
        }
        void initialize(){
            // Save every possible native blast cell and the narrow shaft. Only newly forced chunks are released.
            for(var p:BlockPos.betweenClosed(center.offset(-8,-10,-8),center.offset(8,10,8)))save(p.immutable());
            for(int y=center.getY();y<=Math.min(319,level.getMaxY());y++)save(new BlockPos(center.getX(),y,center.getZ()));
            for(var p:BlockPos.betweenClosed(center.offset(-1,29,-1),center.offset(1,33,1)))save(p.immutable());
            game.items().specialDrops().stop();clear();
            var board=game.server().getScoreboard();board.removePlayerFromTeam(alpha.getScoreboardName());board.removePlayerFromTeam(beta.getScoreboardName());
            for(var player:List.of(alpha,beta)){
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
            }
            beta.getAttribute(Attributes.ARMOR).setBaseValue(0);game.config().tntProtection=0;
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,true,game.server());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,true,game.server());
            alpha.teleportTo(level,origin.x+20,origin.y+8,origin.z,Set.of(),0,0,true);placeBeta();
            for(int i=0;i<5;i++)presentation(i);
            lastTick=game.server().getTickCount();
        }
        void save(BlockPos p){
            if(blocks.containsKey(p))return;long key=net.minecraft.world.level.ChunkPos.pack(p.getX()>>4,p.getZ()>>4);
            if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(p.getX()>>4,p.getZ()>>4,true);}
            level.getChunkAt(p);blocks.put(p,game.maps().snapshot(p));
            if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        }
        void placeBeta(){beta.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.setDeltaMovement(Vec3.ZERO);}
        void presentation(int i){
            var kind=LbcCageDropItem.Kind.values()[i];var stack=new ItemStack(kind.item());var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(kind.flag().equals(flags[i]) && kind.id().toString().equals(ids[i]) && kind.nameKey().equals(names[i]),"independent source IDs "+kind);
            require(data.size()==2 && data.getIntOr(flags[i],0)==1 && data.getIntOr("drop",0)==1,"exact source flags "+kind);
            require(Identifier.parse("lbc:drop_trap").equals(stack.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source model/tier "+kind);
            require(stack.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals(names[i])
                    && !stack.getHoverName().getStyle().isItalic() && stack.getHoverName().getStyle().getColor()!=null && stack.getHoverName().getStyle().getColor().getValue()==0xffff55,"source yellow nonitalic name "+kind);
            var lore=stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines();
            require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc")
                    && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor()!=null && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"source white lore "+kind);
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/"+ids[i].substring("skywars2:".length()));
            require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(stack,loot.getFirst()),"native/ported complete presentation "+kind);
            game.command("give TestAlpha "+ids[i]+" 1");
        }
        ItemEntity drop(LbcCageDropItem.Kind kind,int count){var t=token(level,origin,new ItemStack(kind.item(),count),alpha);tokens.add(t);return t;}
        List<Entity> live(){return game.items().cageDrops().snapshot().stream().filter(e->!e.isRemoved()).toList();}
        void clear(){game.items().cageDrops().stop();}
        List<Entity> batch(LbcCageDropItem.Kind kind){
            clear();placeBeta();var t=drop(kind,3);t.setTarget(beta.getUUID());
            require(game.items().cageDrops().target(t)==beta,"nearest player selected at fractional feet");
            require(game.items().cageDrops().ready(t,kind,beta) && game.items().cageDrops().spawn(t,kind,beta),"resident native cage admitted "+kind);
            require(!t.isRemoved() && t.getItem().getCount()==3,"payload service leaves debit to shared dispatcher");t.discard();return live();
        }
        void geometry(LbcCageDropItem.Kind kind){
            int bricks=0,bars=0,obsidian=0,water=0,glass=0,lava=0,air=0;
            int top=kind==LbcCageDropItem.Kind.OBSIDIAN?2:3;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=top;y++){
                var state=level.getBlockState(center.offset(x,y,z));
                if(state.is(Blocks.STONE_BRICKS))bricks++;else if(state.is(Blocks.IRON_BARS))bars++;else if(state.is(Blocks.OBSIDIAN))obsidian++;
                else if(state.is(Blocks.WATER))water++;else if(state.is(Blocks.GLASS))glass++;else if(state.is(Blocks.LAVA))lava++;else if(state.isAir())air++;
            }
            if(kind==LbcCageDropItem.Kind.OBSIDIAN){
                require(obsidian==30 && water==2 && glass==4,"strict 3x4x3 obsidian30/water2/glass4 shell");
                require(level.getBlockState(center).is(Blocks.WATER) && level.getBlockState(center.above()).is(Blocks.WATER),"two center water cells");
                for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)require(level.getBlockState(center.above().relative(d)).is(Blocks.GLASS),"four cardinal glass cells");
            }else require(bricks==9 && bars==32 && (kind==LbcCageDropItem.Kind.LAVA?lava==1 && air==3:air==4),"source solid-filled cage and center carve "+kind+" bricks="+bricks+" bars="+bars+" air="+air);
        }
        void payloads(){
            for(var kind:LbcCageDropItem.Kind.values()){
                var actors=batch(kind);geometry(kind);
                require(actors.size()==switch(kind){case ANVIL->2;case FIREBALL->1;case POTIONS->40;default->0;},"exact immediate payload count "+kind);
                var offsets=new HashSet<Integer>();int low=0,high=0;
                for(var e:actors){
                    require(game.items().cageDrops().owner(e)==alpha && LbcCageDrops.managed(e) && game.items().currentEpoch(e),"actual thrower cause/current epoch "+kind);
                    require(Math.abs(e.getX()-(center.getX()+.5))<1e-8 && Math.abs(e.getZ()-(center.getZ()+.5))<1e-8,"align xz centered payload");
                    int offset=(int)Math.round(e.getY()-origin.y);require(Math.abs(e.getY()-origin.y-offset)<1e-8 && offsets.add(offset),"exact fractional Y distinct offsets");
                    var tag=saved(e,level);
                    if(kind==LbcCageDropItem.Kind.ANVIL){
                        require(e instanceof FallingBlockEntity falling && falling.getBlockState().is(Blocks.ANVIL) && (offset==40||offset==42),"two native +40/+42 anvils");
                        require(tag.getIntOr("Time",-1)==1 && tag.getBooleanOr("DropItem",false) && tag.getBooleanOr("HurtEntities",false) && tag.getIntOr("FallHurtMax",-1)==35 && tag.getFloatOr("FallHurtAmount",-1)==2,"source anvil damage/age/drop configuration");
                        require(e.getDeltaMovement().equals(Vec3.ZERO) && !e.isNoGravity(),"native anvil starts at rest with gravity");
                    }else if(kind==LbcCageDropItem.Kind.FIREBALL){
                        require(e instanceof LargeFireball && offset==70 && tag.getByteOr("ExplosionPower",(byte)-1)==4,"native power4 fireball +70");
                        require(e.getDeltaMovement().distanceTo(new Vec3(0,-.05,0))<1e-8,"fireball exact initial motion");
                        var vanilla=new LargeFireball(net.minecraft.world.entity.EntityTypes.FIREBALL,level);
                        require(tag.getDoubleOr("acceleration_power",.1)==saved(vanilla,level).getDoubleOr("acceleration_power",.1),"unchanged native fireball acceleration default");vanilla.discard();
                    }else if(kind==LbcCageDropItem.Kind.POTIONS){
                        require(e instanceof ThrownSplashPotion && offset>=50 && offset<=2000 && offset%50==0,"forty simultaneous native splash potions +50..2000");
                        var potion=((ThrownSplashPotion)e).getItem();var contents=potion.get(DataComponents.POTION_CONTENTS);
                        require(potion.is(Items.SPLASH_POTION) && potion.getCount()==1 && contents!=null && contents.customColor().orElse(-1)==7543569,"source splash stack and color");
                        var effects=new ArrayList<net.minecraft.world.effect.MobEffectInstance>();contents.getAllEffects().forEach(effects::add);
                        int amp=(offset<=450 || offset>=1050 && offset<=1450)?0:1;
                        require(effects.size()==1 && effects.getFirst().is(net.minecraft.world.effect.MobEffects.INSTANT_DAMAGE) && effects.getFirst().getDuration()==20 && effects.getFirst().getAmplifier()==amp,"independent source potion effect by altitude "+offset);
                        if(amp==0)low++;else high++;
                    }
                }
                if(kind==LbcCageDropItem.Kind.POTIONS)require(low==18 && high==22,"source amplifier counts18/22");
                clear();
            }
        }
        void targets(){
            var t=drop(LbcCageDropItem.Kind.ANVIL,1);
            alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);
            require(game.items().cageDrops().target(t)==alpha,"nearest selection includes actual thrower");
            alpha.setGameMode(GameType.SPECTATOR);require(game.items().cageDrops().target(t)==beta,"spectator excluded");alpha.setGameMode(GameType.SURVIVAL);
            var board=game.server().getScoreboard();var team=board.addPlayerTeam("codex_cage_probe");
            try{board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);placeBeta();
                alpha.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);require(game.items().cageDrops().target(t)==beta,"nearest allied participant included");}
            finally{board.removePlayerTeam(team);}
            t.discard();alpha.teleportTo(level,origin.x+20,origin.y+8,origin.z,Set.of(),0,0,true);placeBeta();
        }
        void transactions(){
            var t=drop(LbcCageDropItem.Kind.ANVIL,2);
            var data=t.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            for(var flag:flags)data.putInt(flag,1);
            t.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            require(game.items().specialDrops().spawn(t),"multiflag shared dispatch admitted");
            require(t.getItem().getCount()==1 && live().size()==43,"all five flags emit source actors with a single debit");
            geometry(LbcCageDropItem.Kind.OBSIDIAN);
            require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1 && live().size()==43,"duplicate same-tick call cannot activate/debit again");t.discard();clear();
            var modes=new LinkedHashMap<ServerPlayer,GameType>();
            for(var p:level.players())modes.put(p,p.gameMode.getGameModeForPlayer());
            try{
                for(var p:modes.keySet())p.setGameMode(GameType.SPECTATOR);
                var empty=drop(LbcCageDropItem.Kind.ANVIL,2);
                require(game.items().cageDrops().target(empty)==null,"no nonspectator yields no selected target");
                require(game.items().specialDrops().spawn(empty) && empty.getItem().getCount()==1 && live().isEmpty(),"source no-target invocation still debits one without payload");empty.discard();
            }finally{for(var e:modes.entrySet())e.getKey().setGameMode(e.getValue());}
            placeBeta();
        }
        void containers(){
            // Native FillCommand strict passes818: includes256 (skip BE side effects) and32 (suppress drops).
            // LevelChunk removes the old BE without preRemoveSideEffects, so this source fill loses its inventory.
            var pos=center.east();level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos);
            var gem=new ItemStack(Items.DIAMOND,7);gem.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("cage replacement probe"));chest.setItem(0,gem.copy());
            int before=0;for(var e:level.getAllEntities())if(e instanceof ItemEntity item && ItemStack.isSameItemSameComponents(item.getItem(),gem))before+=item.getItem().getCount();
            batch(LbcCageDropItem.Kind.OBSIDIAN);
            int after=0;for(var e:level.getAllEntities())if(e instanceof ItemEntity item && ItemStack.isSameItemSameComponents(item.getItem(),gem))after+=item.getItem().getCount();
            require(level.getBlockEntity(pos)==null && level.getBlockState(pos).is(Blocks.OBSIDIAN) && after==before,"source strict fill removes old container without inventory ejection");clear();
        }
        void residency(){
            var t=drop(LbcCageDropItem.Kind.ANVIL,2);var previous=beta.position();
            int x=(center.getX()>>4)+128,z=(center.getZ()>>4)+128;
            while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;
            try{
                // setPos only: do not teleport, request a ticket, generate a chunk or tick this position.
                beta.setPos(x*16+8.37,origin.y,z*16+8.81);
                require(!game.items().cageDrops().ready(t,LbcCageDropItem.Kind.ANVIL,beta)
                        && !game.items().cageDrops().spawn(t,LbcCageDropItem.Kind.ANVIL,beta),"unresident target rejects before native reads/writes");
                require(level.getChunkSource().getChunkNow(x,z)==null && t.getItem().getCount()==2 && live().isEmpty(),"resident guard cannot load target chunk or debit");
            }finally{beta.setPos(previous);t.discard();}
        }
        void highShaft(){
            clear();var high=center.above(30);var below=high.below(2);var above=high.above(20);
            level.setBlock(below,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);level.setBlock(above,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
            var t=drop(LbcCageDropItem.Kind.ANVIL,1);
            try{
                beta.teleportTo(level,origin.x,origin.y+30,origin.z,Set.of(),0,0,true);
                require(game.items().cageDrops().spawn(t,LbcCageDropItem.Kind.ANVIL,beta),"high-Y corrected shaft admitted");
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)require(level.getBlockState(high.offset(x,-1,z)).is(Blocks.STONE_BRICKS),"Y270 complete floor retained by upward-only shaft");
                require(level.getBlockState(below).is(Blocks.DIAMOND_BLOCK) && level.getBlockState(above).isAir(),"shaft preserves lower blocks and clears above target");
            }finally{t.discard();clear();placeBeta();}
        }
        void drowningCredit(){
            batch(LbcCageDropItem.Kind.OBSIDIAN);
            var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
            try{
                cow.setPos(center.getX()+.5,center.getY()+.1,center.getZ()+.5);cow.setNoAi(true);cow.setNoGravity(true);
                require(level.addFreshEntity(cow),"real disposable drowning victim admitted");
                require(level.getBlockState(BlockPos.containing(cow.getEyePosition())).is(Blocks.WATER) && cow.getKillCredit()==null,"victim eyes in actual cage water without prior combat credit");
                // Exercise the real lethal DROWN source and dying hook; avoid spending 300 ticks on air depletion.
                cow.hurtServer(level,cow.damageSources().drown(),1000);
                require(cow.isDeadOrDying() && cow.getLastDamageSource()!=null && cow.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.DROWN)
                        && cow.getKillCredit()==alpha,"native lethal drowning in obsidian water retains actual token author");
            }finally{
                cow.discard();for(var e:level.getAllEntities())if(!before.contains(e) && (e instanceof ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb) && e.position().distanceTo(Vec3.atCenterOf(center))<4)e.discard();clear();
            }
        }
        void wildDamage(){
            var board=game.server().getScoreboard();var team=board.addPlayerTeam("codex_cage_damage");
            try{
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                nativeDamage();potionDamage();
            }finally{board.removePlayerTeam(team);}
            var actors=batch(LbcCageDropItem.Kind.POTIONS);var potion=(ThrownSplashPotion)actors.getFirst();
            for(var e:actors)if(e!=potion)e.discard();potion.setPos(alpha.position());alpha.setHealth(1000);alpha.setInvulnerableTime(0);alpha.damageCooldownTime=0;
            try{
                potion.onHitAsPotion(level,potion.getItem(),new net.minecraft.world.phys.EntityHitResult(alpha));
                require(alpha.getHealth()<1000 && alpha.getLastDamageSource()!=null && alpha.getLastDamageSource().getEntity()==alpha,"WILD native cage splash also harms and attributes self");
            }finally{clear();}
        }
        void nativeDamage(){
            // Spectator transitions above happen in one fixture tick; native Player.tick normally refreshes this field.
            beta.noPhysics=false;
            var anvils=batch(LbcCageDropItem.Kind.ANVIL);var anvil=(FallingBlockEntity)anvils.getFirst();
            for(var e:anvils)if(e!=anvil)e.discard();anvil.setPos(beta.position());
            beta.setHealth(1000);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
            anvil.causeFallDamage(40,1,anvil.damageSources().fall());
            require(beta.getHealth()<1000 && beta.getLastDamageSource()!=null && beta.getLastDamageSource().getEntity()==alpha,"real native falling-anvil damage retains actual thrower");clear();
            var fire=(LargeFireball)batch(LbcCageDropItem.Kind.FIREBALL).getFirst();
            beta.setHealth(1000);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
            // Start outside the target box and clear a short corridor through the cage bars.
            for(var pos:BlockPos.betweenClosed(center,center.offset(3,2,0)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            fire.setPos(beta.position().add(2,.9,0));fire.setDeltaMovement(new Vec3(-1,0,0));
            for(int i=0;i<4 && !fire.isRemoved();i++)fire.tick();
            require(fire.isRemoved() && beta.getHealth()<1000 && beta.getLastDamageSource()!=null && beta.getLastDamageSource().getEntity()==alpha,
                    "native fireball collision/explosion damages with actual cause; removed="+fire.isRemoved()+" firePos="+fire.position()+" betaPos="+beta.position()
                    +" hp="+beta.getHealth()+" noPhysics="+beta.noPhysics+" mode="+beta.gameMode.getGameModeForPlayer()+" source="+beta.getLastDamageSource()+" cause="+(beta.getLastDamageSource()==null?null:beta.getLastDamageSource().getEntity()));clear();
            beta.setRemainingFireTicks(0);
        }
        void potionDamage(){
            var actors=batch(LbcCageDropItem.Kind.POTIONS);var p=(ThrownSplashPotion)actors.getFirst();
            for(var e:actors)if(e!=p)e.discard();p.setPos(beta.position());beta.setHealth(1000);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
            p.onHitAsPotion(level,p.getItem(),new net.minecraft.world.phys.EntityHitResult(beta));
            require(beta.getHealth()<1000 && beta.getLastDamageSource()!=null && beta.getLastDamageSource().getEntity()==alpha,"real native splash instant harm retains causal player");clear();
        }
        boolean step(){
            require(game.playing(),"fixture keeps actual match active");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{for(int i=0;i<5;i++)require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(i),new ItemStack(LbcCageDropItem.Kind.values()[i].item())),"give complete presentation "+ids[i]);require(live().isEmpty(),"fake give visuals cannot activate");phase++;}
                case 1->{require(live().isEmpty(),"held tokens cannot activate");alpha.getInventory().clearContent();targets();payloads();containers();transactions();residency();highShaft();wildDamage();drowningCredit();stack=drop(LbcCageDropItem.Kind.ANVIL,3);phase++;}
                case 2->{require(!stack.isRemoved() && stack.getItem().getCount()==2 && live().size()==2,"shared dispatcher stack3 one-unit first tick");clear();phase++;}
                case 3->{require(!stack.isRemoved() && stack.getItem().getCount()==1 && live().size()==2,"shared dispatcher stack2 one-unit next tick");clear();phase++;}
                case 4->{require(stack.isRemoved() && live().size()==2,"shared dispatcher last unit removes item");clear();unloaded=drop(LbcCageDropItem.Kind.ANVIL,1);game.items().specialDrops().unloaded(unloaded);phase++;}
                case 5->{require(!unloaded.isRemoved() && live().isEmpty(),"unloaded pending token cannot activate");unloaded.discard();frozen=batch(LbcCageDropItem.Kind.ANVIL).getFirst();frozenPos=frozen.position();frozenVelocity=frozen.getDeltaMovement();game.items().timeStop().begin(level,200);paused=drop(LbcCageDropItem.Kind.ANVIL,2);phase++;}
                case 6->{require(!paused.isRemoved() && paused.getItem().getCount()==2 && frozen.position().distanceTo(frozenPos)<1e-8,"time stop holds both payload and token debit");if(++wait>=3){game.items().timeStop().end(level);require(frozen.getDeltaMovement().distanceTo(frozenVelocity)<1e-8,"time stop restores native velocity");paused.discard();clear();phase++;}}
                case 7->{var stale=drop(LbcCageDropItem.Kind.ANVIL,1);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().cageDrops().spawn(stale,LbcCageDropItem.Kind.ANVIL,beta),"stale epoch rejected");stale.discard();game.items().specialDrops().stop();clear();phase++;}
                case 8->{require(live().isEmpty(),"quiet stop prevents delayed activation");return true;}
                default->throw new IllegalStateException("Unknown cage phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();clear();
            // Remove only fixture-created native leftovers (container ejection, anvil items, fire).
            for(var e:level.getAllEntities())if(!existing.contains(e) && !(e instanceof ServerPlayer) && e.position().distanceTo(origin)<30)e.discard();
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,griefing,game.server());level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,tntExplodes,game.server());game.config().tntProtection=protection;
            beta.getAttribute(Attributes.ARMOR).setBaseValue(armor);var board=game.server().getScoreboard();if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);
            alphaState.restore(game);betaState.restore(game);alpha.noPhysics=alphaNoPhysics;beta.noPhysics=betaNoPhysics;
        }
    }
    public static void run(GameController game){require(running==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error e){try{running.close();}finally{running=null;}throw e;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS cage-drops");}}catch(Throwable e){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL cage-drops",e);}}
    public static void stopProbe(GameController game){
        require(game.playing(),"stop probe in actual match");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));game.items().timeStop().end(level);
        // An out-of-build-height target makes native /fill fail without touching match terrain.
        var previous=alpha.position();var at=new Vec3(alpha.getX(),level.getMaxY()+4.7,alpha.getZ());
        stopEpoch=game.items().epoch();stopToken=token(level,previous,new ItemStack(LbcCageDropItem.Kind.ANVIL.item(),3),alpha);
        try{
            alpha.setPos(at);require(game.items().cageDrops().spawn(stopToken,LbcCageDropItem.Kind.ANVIL,alpha),"real live stop probe");
            stopActors=game.items().cageDrops().snapshot();require(stopActors.size()==2,"two delayed native stop-probe anvils");
        }finally{alpha.setPos(previous);}
        game.items().timeStop().begin(level,200);SkyWars2.LOGGER.info("[check] PASS cage-drops-stop-probe");
    }
    public static void cleanup(GameController game){require(!game.items().currentEpoch(stopEpoch) && game.items().cageDrops().trackedCount()==0 && game.items().specialDrops().pendingCount()==0,"real stop clears cage actors, dispatcher and epoch");require(stopActors.stream().allMatch(Entity::isRemoved),"quiet stop removes native payload before landing");if(stopToken!=null)stopToken.discard();stopToken=null;stopActors=List.of();stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS cage-drops-cleanup");}
}
