package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded native placement/owned field fixture; run within a started two-participant match. */
public final class CorruptedGlyphChecks {
    private CorruptedGlyphChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Corrupted glyph: "+why);}
    private static final class ProfileState {
        final PlayerStore.Profile target;final int coins,memcoins,games,wins,kills,deaths,skill,trinket,challenge,progress,challengeKit;
        final java.util.Set<String> unlocks;
        ProfileState(PlayerStore.Profile p){target=p;coins=p.coins;memcoins=p.memcoins;games=p.games;wins=p.wins;kills=p.kills;deaths=p.deaths;skill=p.lbcSkill;trinket=p.lbcTrinket;challenge=p.lbcChallenge;progress=p.lbcChallengeProgress;challengeKit=p.lbcChallengeKit;unlocks=new java.util.HashSet<>(p.lbcUnlocks);}
        void restore(){target.coins=coins;target.memcoins=memcoins;target.games=games;target.wins=wins;target.kills=kills;target.deaths=deaths;target.lbcSkill=skill;target.lbcTrinket=trinket;target.lbcChallenge=challenge;target.lbcChallengeProgress=progress;target.lbcChallengeKit=challengeKit;target.lbcUnlocks.clear();target.lbcUnlocks.addAll(unlocks);}
    }
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"active TestAlpha/TestBeta participants");
        var service=game.items().corruptedGlyphs();require(service.count()==0,"clean initial glyph service");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());
        var ownerAttributes=new LinkedHashMap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,Double>();
        var otherAttributes=new LinkedHashMap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,Double>();
        for(var attribute:LbcCorruptedGlyphs.DOWN_ATTRIBUTES){var instance=owner.getAttribute(attribute);if(instance!=null)ownerAttributes.put(attribute,instance.getBaseValue());}
        for(var attribute:LbcCorruptedGlyphs.DOWN_ATTRIBUTES){var instance=other.getAttribute(attribute);if(instance!=null)otherAttributes.put(attribute,instance.getBaseValue());}
        var ownerProfile=new ProfileState(game.players().get(owner.getUUID()));var otherProfile=new ProfileState(game.players().get(other.getUUID()));
        var cowStat=net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.COW);int cowKills=owner.getStats().getValue(cowStat),otherCowKills=other.getStats().getValue(cowStat);
        var allAdvancements=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,java.util.Set<String>>();
        var otherAdvancements=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,java.util.Set<String>>();
        for(var advancement:game.server().getAdvancements().getAllAdvancements()){
            var initial=new java.util.HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(initial::add);allAdvancements.put(advancement,initial);
            var otherInitial=new java.util.HashSet<String>();other.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(otherInitial::add);otherAdvancements.put(advancement,otherInitial);
        }
        var existingEntities=new java.util.HashSet<net.minecraft.world.entity.Entity>();owner.level().getAllEntities().forEach(existingEntities::add);
        var profile=game.players().get(owner.getUUID());var unlocks=new java.util.HashSet<>(profile.lbcUnlocks);
        int skill=profile.lbcSkill,trinket=profile.lbcTrinket,challengeProgress=profile.lbcChallengeProgress;
        var statValues=new LinkedHashMap<net.minecraft.stats.Stat<net.minecraft.resources.Identifier>,Integer>();
        var otherStatValues=new LinkedHashMap<net.minecraft.stats.Stat<net.minecraft.resources.Identifier>,Integer>();
        for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);statValues.put(stat,owner.getStats().getValue(stat));otherStatValues.put(stat,other.getStats().getValue(stat));}
        var advancement=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse("lbc:true_advancements/staffs/corruption"));
        var done=new java.util.HashSet<String>();if(advancement!=null)owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);
        var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();var mobs=new ArrayList<Cow>();
        var origin=owner.blockPosition().offset(0,5,0);var at=Vec3.atBottomCenterOf(origin);
        try{
            owner.setGameMode(GameType.SURVIVAL);owner.removeAllEffects();owner.setNoGravity(true);
            owner.teleportTo(owner.level(),at.x+4,at.y,at.z,java.util.Set.of(),0,0,true);
            for(int i=0;i<16;i++){
                var rolled=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:corrupted_dust");require(rolled.size()==1,"live source dust table returns one native item");
                var dust=rolled.getFirst();var kind=LbcCorruptedGlyphs.kind(dust);require(dust.is(LbcCorruptedGlyphs.DUST)&&kind!=null&&dust.getCount()==1,"decoded source table native dust/count/kind");
                String suffix=kind.name().toLowerCase(java.util.Locale.ROOT);
                require(net.minecraft.resources.Identifier.parse(kind==LbcCorruptedGlyphs.Kind.BAFOMET?"lbc:corrupted_dust":"lbc:corrupted_dust_"+suffix).equals(dust.get(DataComponents.ITEM_MODEL))
                        &&dust.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated&&translated.getKey().equals("corrupted_dust_"+suffix)
                        &&net.minecraft.resources.Identifier.parse("lbc:tier_"+switch(kind){case LAMASHTU->"b";case BELIAL->"a";default->"c";}).equals(dust.get(DataComponents.TOOLTIP_STYLE)),"source dust name/model/per-kind style survive live loot decoding");
            }
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                var p=origin.offset(x,y,z);blocks.put(p,game.maps().snapshot(owner.level(),p));
                owner.level().setBlock(p,(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            }
            // All four actual Item.useOn routes use the same registry and debit exactly one unit.
            for(var kind:LbcCorruptedGlyphs.Kind.values()){
                var stack=new ItemStack(LbcCorruptedGlyphs.DUST,2);var data=new CompoundTag();data.putString("glyphkind",kind.name().toLowerCase(java.util.Locale.ROOT));
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(at,Direction.UP,origin.below(),false);
                require(stack.useOn(new UseOnContext(owner,InteractionHand.MAIN_HAND,hit)).consumesAction()&&stack.getCount()==1&&service.count()==1,"native "+kind+" placement/consume");
                var glyph=service.snapshot().getFirst();
                require(glyph.kind==kind&&glyph.owner==owner&&glyph.display.position().equals(at),"placer ownership and exact centered position");
                service.forget(owner);require(glyph.display.isRemoved()&&service.count()==0,"owner cleanup");
            }
            var friend=cow(owner,at.add(3,0,0));mobs.add(friend);var role=cow(owner,at.add(-3,0,0));mobs.add(role);role.entityTags().add("strict_map_object");
            var glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.BAFOMET);require(glyph!=null,"Bafomet spawn");
            service.pulse(glyph);require(friend.getDeltaMovement().x<0&&role.getDeltaMovement().equals(Vec3.ZERO),"source pull excludes technical role");
            require(game.items().indirectKills().altitudeAuthor(friend)==owner&&game.items().indirectKills().altitudeAuthor(role)==null,"pull registers placer displacement cause and protects technical roles");
            var pulled=cow(owner,at.add(5,0,0));mobs.add(pulled);service.pulse(glyph);
            require(pulled.hurtServer(owner.level(),pulled.damageSources().fellOutOfWorld(),1000)&&pulled.getKillCredit()==owner&&pulled.isDeadOrDying(),"native lethal void damage inherits Bafomet placer");
            for(int i=0;i<2001;i++)service.tick();require(service.count()==1&&!glyph.display.isRemoved(),"no artificial glyph timeout");
            int age=glyph.ticks;game.items().timeStop().begin(owner.level(),200);
            try{for(int i=0;i<40;i++)service.tick();require(glyph.ticks==age&&!glyph.display.isRemoved(),"native dimension Time Stop pauses field cadence");}
            finally{game.items().timeStop().end(owner.level());}
            var stopper=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());stopper.setPos(at);stopper.entityTags().add("stopper_magic");owner.level().addFreshEntity(stopper);game.items().magic().loaded(stopper);
            try{service.tick();require(glyph.display.isRemoved()&&service.count()==0,"native antimagic deletes glyph");}
            finally{game.items().magic().unloaded(stopper);stopper.discard();}
            var previousTeam=owner.getTeam();var team=game.server().getScoreboard().addPlayerTeam("glyph_fixture");
            try{
                game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
                glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.LAMASHTU);require(glyph!=null,"Lamashtu spawn");service.pulse(glyph);
                require(friend.hasEffect(MobEffects.WITHER)&&friend.getEffect(MobEffects.WITHER).isInfiniteDuration()&&friend.getEffect(MobEffects.WITHER).getAmplifier()==0,"friendly mob receives owned infinite Wither I");
                require(!owner.hasEffect(MobEffects.WITHER)&&!role.hasEffect(MobEffects.WITHER),"owner and technical roles protected");
                owner.addEffect(new MobEffectInstance(MobEffects.WITHER,100,1));role.addEffect(new MobEffectInstance(MobEffects.WITHER,100,1));service.explode(glyph);
                require(!friend.hasEffect(MobEffects.WITHER)&&!owner.hasEffect(MobEffects.WITHER)&&role.hasEffect(MobEffects.WITHER),"source explosion clears all eligible Wither within16 including owner; role excluded");
            }finally{game.server().getScoreboard().removePlayerTeam(team);if(previousTeam!=null)game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),previousTeam);}
            glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.BAFOMET);owner.level().setBlock(origin.below(),Blocks.AIR.defaultBlockState(),2);service.pulse(glyph);
            require(glyph.display.isRemoved()&&service.count()==0,"unsupported glyph terminates");
            owner.level().setBlock(origin.below(),Blocks.STONE.defaultBlockState(),2);glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.BAFOMET);
            owner.level().setBlock(origin,Blocks.STONE.defaultBlockState(),2);service.pulse(glyph);require(glyph.display.isRemoved(),"obstructed glyph terminates");
            owner.level().setBlock(origin,Blocks.AIR.defaultBlockState(),2);
            dependent(game,owner,other,service,origin,at,blocks,mobs,friend,role);
        }finally{
            service.forget(owner);for(var mob:mobs){game.items().indirectKills().clear(mob);mob.discard();}
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());saved.restore(game);
            ownerAttributes.forEach((key,value)->owner.getAttribute(key).setBaseValue(value));otherAttributes.forEach((key,value)->other.getAttribute(key).setBaseValue(value));otherSaved.restore(game);
            statValues.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));
            otherStatValues.forEach((stat,value)->other.getStats().setValue(other,stat,value));owner.getStats().setValue(owner,cowStat,cowKills);other.getStats().setValue(other,cowStat,otherCowKills);
            allAdvancements.forEach((adv,initial)->{for(var criterion:adv.value().criteria().keySet())if(!initial.contains(criterion))owner.getAdvancements().revoke(adv,criterion);});
            otherAdvancements.forEach((adv,initial)->{for(var criterion:adv.value().criteria().keySet())if(!initial.contains(criterion))other.getAdvancements().revoke(adv,criterion);});
            for(var entity:owner.level().getAllEntities())if(!existingEntities.contains(entity)&&(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb))entity.discard();
            if(advancement!=null)for(var criterion:advancement.value().criteria().keySet())if(!done.contains(criterion))owner.getAdvancements().revoke(advancement,criterion);
            profile.lbcUnlocks.clear();profile.lbcUnlocks.addAll(unlocks);profile.lbcSkill=skill;profile.lbcTrinket=trinket;profile.lbcChallengeProgress=challengeProgress;ownerProfile.restore();otherProfile.restore();game.players().save();
        }
    }
    private static void dependent(GameController game,ServerPlayer owner,ServerPlayer other,LbcCorruptedGlyphs service,
                                  BlockPos origin,Vec3 at,Map<BlockPos,MapManager.BlockSnapshot> blocks,List<Cow> mobs,Cow friend,Cow role){
        var board=game.server().getScoreboard();var oldOwnerTeam=owner.getTeam();var oldOtherTeam=other.getTeam();int oldMode=game.config().gameMode;
        var allied=board.addPlayerTeam("glyph_allied");var enemy=board.addPlayerTeam("glyph_enemy");
        try{
            board.addPlayerToTeam(owner.getScoreboardName(),allied);board.addPlayerToTeam(other.getScoreboardName(),allied);
            owner.teleportTo(owner.level(),at.x+.4,at.y,at.z,java.util.Set.of(),0,0,true);other.setGameMode(GameType.SURVIVAL);other.removeAllEffects();other.setNoGravity(true);
            other.teleportTo(owner.level(),at.x-.4,at.y,at.z,java.util.Set.of(),0,0,true);
            var glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.BAFOMET);service.explode(glyph);
            require(owner.hasEffect(MobEffects.WITHER)&&other.hasEffect(MobEffects.WITHER)&&owner.getEffect(MobEffects.WITHER).getDuration()==100&&other.getEffect(MobEffects.WITHER).getAmplifier()==4,"Bafomet source WILD WitherV includes caster and allied player");
            owner.removeEffect(MobEffects.WITHER);other.removeEffect(MobEffects.WITHER);
            owner.teleportTo(owner.level(),at.x+4,at.y,at.z,java.util.Set.of(),0,0,true);other.teleportTo(owner.level(),at.x+5,at.y,at.z,java.util.Set.of(),0,0,true);
            for(int mode:new int[]{1,2,3,4}){
                game.config().gameMode=mode;board.addPlayerToTeam(friend.getScoreboardName(),allied);board.addPlayerToTeam(role.getScoreboardName(),allied);board.addPlayerToTeam(other.getScoreboardName(),enemy);
                glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.BELIAL);service.pulse(glyph);
                require(friend.getTeam()==null&&role.getTeam()==allied&&owner.getTeam()==allied,"Belial removes friendly mob team and protects caster/technical role");
                require(mode<3?other.getTeam()==null:other.getTeam()==enemy,"Belial foreign player team leave only in source modes1/2");service.forget(owner);
            }
            board.addPlayerToTeam(other.getScoreboardName(),allied);glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.BELIAL);service.pulse(glyph);
            require(owner.getTeam()==allied&&other.getTeam()==allied,"Belial protects allied player teams");
            var attacker=cow(owner,at.add(7,0,0));mobs.add(attacker);friend.setInvulnerableTime(0);friend.damageCooldownTime=0;
            require(service.belialAttack(glyph,friend,attacker)&&friend.getLastHurtByMob()==attacker,"native forced arrow keeps actual mob for revenge AI");
            var retaliation=attacker.damageSources().mobAttack(friend);
            require(service.belialCause(attacker,retaliation)!=null&&service.belialCause(attacker,retaliation).player().equals(owner.getUUID()),"accepted forced hit installs symmetric duel cause");
            attacker.setInvulnerableTime(0);attacker.damageCooldownTime=0;
            require(attacker.hurtServer(owner.level(),retaliation,1000)&&attacker.isDeadOrDying()&&attacker.getKillCredit()==owner,"native retaliation death credits glyph placer with real mob attacker");
            var late=cow(owner,at.add(9,0,0));mobs.add(late);late.setInvulnerableTime(0);late.damageCooldownTime=0;require(service.belialAttack(glyph,late,friend),"later-hit duel preparation");
            late.setInvulnerableTime(0);late.damageCooldownTime=0;require(late.hurtServer(owner.level(),late.damageSources().playerAttack(other),1),"real later allied participant hit accepted");
            late.setInvulnerableTime(0);late.damageCooldownTime=0;require(late.hurtServer(owner.level(),late.damageSources().mobAttack(friend),1000)&&late.getKillCredit()==other,"later accepted player action wins over forced duel cause");
            require(service.belialCause(friend,friend.damageSources().playerAttack(other))==null,"real later player attacks override forced duel causality");service.forget(owner);
            // A single exact cube helper exercises native drops and WILD damage without random map erosion.
            var center=at.add(8,0,0);var cube=BlockPos.containing(center);
            for(var mutable:BlockPos.betweenClosed(cube.offset(-1,-1,-1),cube.offset(1,1,1))){var p=mutable.immutable();blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,Blocks.STONE.defaultBlockState(),2);}
            owner.teleportTo(owner.level(),center.x,center.y,center.z,java.util.Set.of(),0,0,true);other.teleportTo(owner.level(),center.x+1,center.y,center.z,java.util.Set.of(),0,0,true);
            owner.setInvulnerableTime(0);owner.damageCooldownTime=0;other.setInvulnerableTime(0);other.damageCooldownTime=0;
            owner.setHealth(owner.getMaxHealth());other.setHealth(other.getMaxHealth());float beforeOwner=owner.getHealth(),beforeOther=other.getHealth();
            var existing=new java.util.HashSet<net.minecraft.world.entity.Entity>();owner.level().getAllEntities().forEach(existing::add);
            glyph=service.spawn(owner,at,LbcCorruptedGlyphs.Kind.AHRIMAN);
            require(!service.ahrimanCube(glyph,at.add(2,0,0))&&service.ahrimanCube(glyph,center),"Ahriman source radius2.5 center guard and real distant cube");
            require(owner.getHealth()<beforeOwner&&other.getHealth()<beforeOther&&other.getKillCredit()==owner,"Ahriman native outside_border WILD hits caster/allied player with placer credit");
            require(owner.level().getBlockState(cube).isAir()&&owner.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(cube).inflate(3)).stream().anyMatch(item->!existing.contains(item)),"destroy cube removes blocks and produces native drops");
            for(var entity:owner.level().getAllEntities())if(!existing.contains(entity)&&entity instanceof net.minecraft.world.entity.item.ItemEntity)entity.discard();
            var beforeAttributes=new LinkedHashMap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,Double>();
            for(var key:LbcCorruptedGlyphs.DOWN_ATTRIBUTES){var instance=owner.getAttribute(key);if(instance!=null)beforeAttributes.put(key,instance.getBaseValue());}
            var roleHealth=role.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
            owner.level().getRandom().setSeed(75192);service.explode(glyph);
            int changed=0,unchanged=0;for(var entry:beforeAttributes.entrySet()){
                double after=owner.getAttribute(entry.getKey()).getBaseValue();if(after<entry.getValue())changed++;else if(after==entry.getValue())unchanged++;
            }
            require(changed>0&&role.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue()==roleHealth,"Ahriman independent permanent base reductions and technical role guard: changed="+changed+" unchanged="+unchanged+" health="+friend.getHealth()+" distance="+friend.position().distanceTo(glyph.display.position())+" removed="+friend.isRemoved()+" eligible="+game.items().wands().validTarget(friend)+" glyphRemoved="+glyph.display.isRemoved()+" roleBefore="+roleHealth+" roleAfter="+role.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue());
        }finally{
            game.config().gameMode=oldMode;board.removePlayerTeam(allied);board.removePlayerTeam(enemy);
            if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldOtherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOtherTeam);
        }
    }
    private static Cow cow(ServerPlayer owner,Vec3 at){var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);owner.level().addFreshEntity(cow);return cow;}
}
