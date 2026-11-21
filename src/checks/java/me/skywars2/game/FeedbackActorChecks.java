package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.LbcSpellSource;
import me.skywars2.game.effects.LbcStaticCharge;
import me.skywars2.game.items.drops.LbcNpcDropItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import me.skywars2.game.entities.mobs.LbcTransformator;
import me.skywars2.game.items.drops.LbcChaosTrapActors;
import me.skywars2.game.presentation.effects.LbcRadarDisplays;

/** Native trap payload, private radar transport, real potion cloud and actual damage callbacks. */
final class FeedbackActorChecks {
    private FeedbackActorChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Feedback actors: "+why);}
    static void run(GameController game){
        require(game.server().getPort()==25577,"isolated fixture only");
        try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner)){
            f.prepareParticipants();
            try{trap(f);radar(f);transformer(f);charge(f);}
            finally{game.items().radar().stop();game.items().legacyEffects().staticCharge().clear(f.owner);game.items().legacyEffects().staticCharge().clear(f.other);}
        }
        SkyWars2.LOGGER.info("[check] PASS feedback-actors (unowned Aimbob, native targets/damage; private radar wire; TECH Transformer and empty death loot; actual126 cloud and damage discharge)");
    }
    private static ItemEntity token(WoolBucketChecks.Fixture f,ItemStack stack){
        var token=new ItemEntity(f.level,f.home.x,f.home.y+2,f.home.z,stack);token.setThrower(f.owner);f.own(token);
        f.game.items().markCurrentItemEpoch(token);require(f.level.addFreshEntity(token),"native token admitted");return token;
    }
    private static void trap(WoolBucketChecks.Fixture f){
        var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);var data=new CompoundTag();data.putInt("drop",1);data.putInt("aimbob",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        var service=f.game.items().chaosTrapActors();var before=Set.copyOf(service.snapshot());var token=token(f,stack);
        require(service.spawn(token,"aimbob"),"actual source Aimbob branch");
        var mob=service.snapshot().stream().filter(e->!before.contains(e)&&e instanceof Mob).map(Mob.class::cast).findFirst().orElseThrow();f.own(mob);
        require(LbcSpellSource.unownedHazard(mob)&&LbcIndirectKills.playerSource(mob)==null,"causal thrower is not combat author");
        require(mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==1,"Aimbob knockback immunity");
        f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;f.owner.setAbsorptionAmount(0);
        require(f.owner.hurtServer(f.level,f.owner.damageSources().playerAttack(f.other),1)&&f.owner.getKillCredit()==f.other,"real earlier PvP hit recorded");
        f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
        var board=f.game.server().getScoreboard();var team=board.addPlayerTeam("feedbackAI");
        try{
            board.addPlayerToTeam(f.owner.getScoreboardName(),team);board.addPlayerToTeam(f.other.getScoreboardName(),team);
            require(mob.canAttack(f.owner)&&mob.canAttack(f.other),"thrower and allied participant both native targets");
            mob.setTarget(f.owner);require(mob.getTarget()==f.owner,"native setTarget accepts thrower");
            mob.setPos(f.owner.position().add(0,0,1));float hp=f.owner.getHealth();
            require(mob.doHurtTarget(f.level,f.owner)&&f.owner.getHealth()<hp,"real native trap melee damages thrower");
            require(f.owner.getLastHurtByMob()==mob&&f.owner.getKillCredit()==mob,"native Mob supersedes older player kill credit");
        }finally{board.removePlayerTeam(team);f.discard(mob);f.discard(token);}
    }
    private static void flat(Packet<?> packet,List<Packet<?>> result){
        if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flat(child,result);else result.add(packet);
    }
    private static List<Packet<?>> tail(RegenerationBeltChecks.ScheduledClient client,int start){
        var result=new ArrayList<Packet<?>>();for(var p:client.packets().subList(start,client.packets().size()))flat(p,result);return result;
    }
    private static long displays(List<Packet<?>> packets){return packets.stream().filter(p->p instanceof ClientboundAddEntityPacket add&&add.getType()==EntityTypes.ITEM_DISPLAY).count();}
    private static void radar(WoolBucketChecks.Fixture f){
        var service=f.game.items().radar();service.stop();int own=f.client.packets().size(),other=f.audience.packets().size();
        service.begin();service.mark(f.owner,f.other);service.mark(f.owner,f.other);service.end();
        require(service.count()==1&&displays(tail(f.client,own))==1,"one private native display, deduplicated across producers");
        require(displays(tail(f.audience,other))==0,"target/other audience never receives the mark");
        own=f.client.packets().size();service.begin();service.mark(f.owner,f.other);service.end();
        require(tail(f.client,own).stream().noneMatch(ClientboundSetEntityDataPacket.class::isInstance),"stationary mark does not resend unchanged display metadata");
        own=f.client.packets().size();service.begin();service.end();
        require(service.count()==0&&tail(f.client,own).stream().anyMatch(ClientboundRemoveEntitiesPacket.class::isInstance),"inactive radar removes client display immediately");
        service.begin();service.mark(f.owner,f.other);service.end();own=f.client.packets().size();
        float hp=f.owner.getHealth();
        try{
            f.owner.setHealth(0);service.begin();service.mark(f.owner,f.other);service.end();
            require(service.count()==0&&tail(f.client,own).stream().anyMatch(ClientboundRemoveEntitiesPacket.class::isInstance),
                    "a dead viewer cannot retain or renew radar even if stale armor producers still submit it");
        }finally{f.owner.setHealth(hp);}
        var target=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,f.level);
        target.setPos(f.owner.position().add(0,0,2));target.setNoAi(true);f.own(target);
        require(f.level.addFreshEntity(target),"real radar target");
        service.begin();service.mark(f.owner,target);service.end();
        var spawn=tail(f.client,own).stream().filter(packet->packet instanceof ClientboundAddEntityPacket add
                &&add.getType()==EntityTypes.ITEM_DISPLAY).map(ClientboundAddEntityPacket.class::cast).reduce((earlier,last)->last).orElseThrow();
        for(var delta:List.of(new net.minecraft.world.phys.Vec3(.75,.25,-.5),new net.minecraft.world.phys.Vec3(80,0,0))){
            target.setPos(target.position().add(delta));
            own=f.client.packets().size();other=f.audience.packets().size();
            service.begin();service.mark(f.owner,target);service.end();
            var moves=tail(f.client,own).stream().filter(packet->packet instanceof ClientboundEntityPositionSyncPacket sync
                    &&sync.id()==spawn.getId()).map(ClientboundEntityPositionSyncPacket.class::cast).toList();
            require(moves.size()==1&&moves.getFirst().position().endPosition().distanceToSqr(new net.minecraft.world.phys.Vec3(
                    target.getX(),target.getBoundingBox().maxY+.3,target.getZ()))<.000001,
                    "the existing mark sends the mob's exact new position, including outside ticking client chunks");
            require(tail(f.audience,other).stream().noneMatch(packet->packet instanceof ClientboundEntityPositionSyncPacket sync
                    &&sync.id()==spawn.getId()),"movement stays private to the viewer");
        }
        own=f.client.packets().size();
        target.kill(f.level);
        require(service.count()==0&&tail(f.client,own).stream().anyMatch(ClientboundRemoveEntitiesPacket.class::isInstance),
                "native AFTER_DEATH removes private radar immediately without waiting for another gameplay frame");
    }
    private static void transformer(WoolBucketChecks.Fixture f){
        var service=f.game.items().transformator();var token=token(f,new ItemStack(LbcNpcDropItems.TRANSFORMATOR));
        service.spawn(token);var view=service.snapshot().getLast();f.own(view.actor());if(view.text()!=null)f.own(view.text());
        f.field(true,view.actor().position());service.tick();
        require(view.actor().isRemoved()&&(view.text()==null||view.text().isRemoved()),"TECH immediately destroys actor and visual");f.removeFields();
        service.spawn(token);view=service.snapshot().getLast();f.own(view.actor());if(view.text()!=null)f.own(view.text());
        var before=f.itemIds();view.actor().kill(f.level);
        for(var entity:f.level.getAllEntities())if(entity instanceof ItemEntity item&&!before.contains(item.getUUID())){
            f.own(item);require(!item.getItem().is(Items.ROTTEN_FLESH),"no vanilla zombie loot");
        }
        service.tick();f.discard(token);
    }
    private static void charge(WoolBucketChecks.Fixture f){
        var service=f.game.items().legacyEffects().staticCharge();var cloud=new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,f.level);f.own(cloud);
        cloud.setPos(f.owner.position());cloud.setPotionContents(PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.RAID_OMEN,200,126)));
        require(f.level.addFreshEntity(cloud),"native marker126 cloud admitted");
        require(cloud.getRadius()==5&&cloud.getDuration()==300,"actual cloud converted to source radius/lifetime");
        for(int i=0;i<5;i++)cloud.tick();
        require(f.game.items().mana().hudValue(f.owner,LbcStaticCharge.COUNTER)==10&&!f.owner.hasEffect(MobEffects.RAID_OMEN),"real cloud contact creates charge instead of vanilla omen");
        f.discard(cloud);f.move(f.other,f.owner.position().add(4,0,0));float hp=f.other.getHealth();
        f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
        require(f.owner.hurtServer(f.level,f.owner.damageSources().playerAttack(f.other),1),"accepted damage triggers native status callback");service.tick();
        require(f.other.getHealth()==hp-6,"charge actually discharges six native damage to next target");
        var ray=f.level.getEntitiesOfClass(Display.ItemDisplay.class,f.other.getBoundingBox().inflate(2),LbcStaticCharge::visual).getFirst();
        var impact=f.other.getBoundingBox().clip(f.owner.getEyePosition(),f.other.getEyePosition()).orElseThrow();
        require(ray.position().distanceToSqr(impact)<1e-10,"discharge visual is anchored at the actual target hitbox face");
        DisplayTransformChecks.pose(ray,0,0,0,3,3,(float)-f.owner.getEyePosition().distanceTo(impact));
        require(f.game.items().mana().hudValue(f.other,LbcStaticCharge.COUNTER)==9,"remaining charge transferred");
        service.clear(f.owner);service.clear(f.other);hp=f.other.getHealth();
        var wall=f.owner.blockPosition().offset(2,0,0);
        for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)f.set(wall.offset(0,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        try{discharge(f);require(f.other.getHealth()==hp&&f.game.items().mana().hudValue(f.other,LbcStaticCharge.COUNTER)==0,"native wall stops charge arc and transfer");}
        finally{for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)f.set(wall.offset(0,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}
        f.field(true,f.owner.position().add(2,0,0));
        try{discharge(f);require(f.other.getHealth()==hp,"TECH stops the actual discharge");}finally{f.removeFields();}
        service.accept(f.owner,new MobEffectInstance(MobEffects.RAID_OMEN,200,126));
        f.pause();try{service.tick();require(f.game.items().mana().hudValue(f.owner,LbcStaticCharge.COUNTER)==10,"time stop preserves stored charge");}finally{f.thaw();}
        for(var entity:f.level.getAllEntities())if(LbcStaticCharge.visual(entity))f.own(entity);
    }
    private static void discharge(WoolBucketChecks.Fixture f){
        var service=f.game.items().legacyEffects().staticCharge();service.clear(f.owner);service.clear(f.other);
        require(service.accept(f.owner,new MobEffectInstance(MobEffects.RAID_OMEN,200,126)),"charge producer accepts the next exposure");
        f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
        require(f.owner.hurtServer(f.level,f.owner.damageSources().playerAttack(f.other),1),"native damage starts discharge");service.tick();
    }
}
