package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;

/** Measures actual hand consumption and native presentation through the real status scheduler. */
final class TwitchingFeedbackChecks {
    private TwitchingFeedbackChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Twitch feedback: "+message);}
    private static void flatten(Packet<?> packet,List<Packet<?>> result){
        if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flatten(child,result);
        else result.add(packet);
    }
    static void run(GameController game){
        require(game.server().getPort()==25577,"isolated native fixture only");
        try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner)){
            f.prepareParticipants();var service=game.items().psycho();
            require(service.effectCount()==0,"exclusive native status scene");
            var before=new HashSet<UUID>();for(var entity:f.level.getAllEntities())before.add(entity.getUUID());
            try{
                f.owner.setXRot(-90);f.owner.getRandom().setSeed(0x747769746368L);
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SNOWBALL,64));
                f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SNOWBALL,64));
                f.owner.getInventory().setItem(9,new ItemStack(Items.GLASS_BOTTLE,3));
                service.afflict(f.owner,f.other,0,0,3000,0);
                int packets=f.client.packets().size(),main=0,off=0;
                for(int tick=0;tick<6000;tick++){
                    int previous=service.effectValue(f.owner,"twitching");service.tick();
                    int remaining=service.effectValue(f.owner,"twitching");
                    require(previous-remaining==0||previous-remaining==1,"one original counter debit per two-tick opportunity");
                    int a=64-f.owner.getMainHandItem().getCount(),b=64-f.owner.getOffhandItem().getCount();
                    require(a<=1&&b<=1&&a==b,"one matching category handles both native hands once");
                    main+=a;off+=b;
                    f.owner.getMainHandItem().setCount(64);f.owner.getOffhandItem().setCount(64);
                }
                require(service.effectValue(f.owner,"twitching")==0,"exact3000 source opportunities in6000 ticks");
                require(main>=150&&main<=350&&off==main,"real hand rate near1/12, neither unrestricted50% nor double-divided1/72: "+main);
                var wire=new ArrayList<Packet<?>>();
                for(var packet:f.client.packets().subList(packets,f.client.packets().size()))flatten(packet,wire);
                int voices=0,slime=0,dust=0,glass=0;
                var sounds=new HashSet<String>();
                for(var packet:wire){
                    if(packet instanceof ClientboundSoundPacket sound){
                        String id=sound.getSound().value().location().toString();
                        if(id.startsWith("minecraft:lbcsounds.")){
                            sounds.add(id);voices++;
                            require(sound.getVolume()==2.5f&&sound.getPitch()==1,"legacy voice volume/pitch");
                            require(Math.abs(sound.getX()-f.owner.getX())<=.125&&Math.abs(sound.getY()-f.owner.getY())<=.125&&Math.abs(sound.getZ()-f.owner.getZ())<=.125,"voice at affected player");
                        }else if(id.equals("minecraft:entity.slime.jump_small")){slime++;require(sound.getVolume()==.5f&&sound.getPitch()==1,"legacy independent slime sound");}
                    }else if(packet instanceof ClientboundLevelParticlesPacket particles){
                        if(particles.particle().getType()==ParticleTypes.DUST){dust++;require(particles.count()==30,"voice emits30 native dust particles");}
                        else if(particles.particle().getType()==ParticleTypes.ITEM){glass++;require(particles.count()==10,"independent10 glass particles");}
                    }
                }
                require(voices>=10&&voices<=65&&dust==voices,"independent1% voices and accompanying dust: "+voices+" / "+dust);
                require(sounds.size()==6,"all six original voice clips");
                require(glass>=180&&glass<=420&&slime>=950&&slime<=1450,"independent10% glass/40% slime presentation: "+glass+" / "+slime);
                require(f.owner.getInventory().getItem(9).isEmpty(),"real shared sound helper consumes exactly available glass bottles");
                int shots=0,drops=0;
                for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID())){
                    f.own(entity);
                    if(entity instanceof Snowball shot){shots++;require(shot.getOwner()==f.owner,"native projectile owner");}
                    if(entity instanceof ItemEntity) drops++;
                }
                require(shots==main+off&&drops==3,"real matching hand shots and exactly one loot output per bottle: "+shots+" / "+drops);
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,64));
                f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.EGG,64));
                service.afflict(f.owner,f.other,0,0,300,0);
                for(int tick=0;tick<600;tick++)service.tick();
                require(f.owner.getMainHandItem().getCount()==64&&f.owner.getOffhandItem().getCount()==64&&!f.owner.isUsingItem(),"ineligible categories do not fall through to arbitrary main/offhand clicks");
                SkyWars2.LOGGER.info("[check] PASS twitch-feedback (actual3000 two-tick opportunities, main/offhand uses={}/{}, voices={}, glass={}, slime={}; shared native sound/bottle loot, no arbitrary hand fallback)",main,off,voices,glass,slime);
            }finally{
                service.clearStatus(f.owner,"twitching");
                for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID()))f.own(entity);
            }
        }
    }
}
