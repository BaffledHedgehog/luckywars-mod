package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPostEffectsPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Actual player clock use and native packet send interception, not a policy-only mock. */
public final class TimeStopViewChecks {
    private TimeStopViewChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Time Stop view: "+why);}
    private static List<Packet<?>> flat(RegenerationBeltChecks.ScheduledClient c){return SmokeVolumeChecks.flatten(c.packets());}
    private static long sounds(RegenerationBeltChecks.ScheduledClient c){return flat(c).stream().filter(p->p instanceof ClientboundSoundPacket||p instanceof ClientboundSoundEntityPacket).count();}
    private static long staffMusic(RegenerationBeltChecks.ScheduledClient client){
        return flat(client).stream().filter(packet -> packet instanceof ClientboundSoundPacket sound
                && LbcMusicStaff.MUSIC.equals(sound.getSound().value().location())).count();
    }

    private static boolean useClock(WoolBucketChecks.Fixture f,InteractionHand hand){
        return f.owner.gameMode.useItem(f.owner,f.level,f.owner.getItemInHand(hand),hand).consumesAction();
    }

    private static ItemStack checkOrdinaryWatch(WoolBucketChecks.Fixture f){
        var stop=f.game.items().timeStop();
        // This is the existing weighted result referenced by the Loli pool, rolled deterministically in isolation.
        var expected=f.loot("lbc:item_defs/lbc/time_clock__6b7715f03a34");
        require(expected.getCount()==1,"one actual Loli watch in its canonical loot result");
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,expected.copy());
        require(useClock(f,InteractionHand.MAIN_HAND),"ordinary Loli watch activates through native main-hand use");
        require(ItemStack.matches(f.owner.getMainHandItem(),expected),"ordinary Loli watch keeps its count, damage and components on actual activation");
        try {
            require(stop.remaining(f.level)==200 && stop.ordinaryCooldownTicks(f.owner)==1200 && stop.cooldownTicks(f.owner)==0,
                    "ordinary cast starts ten seconds of stopped time and its separate sixty-second recharge");
            require(!stop.frozen(f.owner)&&stop.frozen(f.other),"only the participant carrying the watch stays active");
            stop.tick();
            require(stop.remaining(f.level)==199&&stop.ordinaryCooldownTicks(f.owner)==1200,
                    "ordinary watch recharge pauses during its active stop");
            require(!useClock(f,InteractionHand.MAIN_HAND)&&stop.remaining(f.level)==199
                            &&ItemStack.matches(f.owner.getMainHandItem(),expected),
                    "recharge blocks a same-hand second activation without changing the item");
            f.other.getInventory().setItem(9,expected.copy());
            stop.tick();
            require(!stop.frozen(f.other)&&!TimeStopView.frozen(f.other),"ordinary stop respects a carried watch outside the hands");
            stop.end(f.level);
            stop.tick();
            f.owner.setItemInHand(InteractionHand.OFF_HAND,f.owner.getMainHandItem());
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            require(stop.ordinaryCooldownTicks(f.owner)==1199&&!useClock(f,InteractionHand.OFF_HAND),
                    "recharge resumes after thaw and follows the player across both hands");
            stop.reduceSourceCooldown(f.owner,0);
            require(useClock(f,InteractionHand.OFF_HAND)&&ItemStack.matches(f.owner.getOffhandItem(),expected)
                            &&stop.remaining(f.level)==200&&stop.ordinaryCooldownTicks(f.owner)==1200,
                    "after recharge the same ordinary watch casts again from the offhand with its full original data");
        }finally{
            stop.end(f.level);
            stop.reduceSourceCooldown(f.owner,0);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            f.other.getInventory().setItem(9,ItemStack.EMPTY);
        }
        var touhou=f.loot("lbc:time_stopper");
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,touhou.copyWithCount(2));
        try {
            require(useClock(f,InteractionHand.MAIN_HAND)&&ItemStack.matches(f.owner.getMainHandItem(),touhou)
                            &&stop.remaining(f.level)==220&&stop.ordinaryCooldownTicks(f.owner)==0,
                    "actual Touhou clock still debits exactly one and grants its separate eleven-second stop");
        }finally{
            stop.end(f.level);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        }
        return expected;
    }

    private static void checkHeldStaff(GameController game,WoolBucketChecks.Fixture f){
        var stop=game.items().timeStop();
        var music=game.items().musicStaff();
        require(!music.playing(),"exclusive held staff scene");
        f.client.packets().clear();
        f.audience.packets().clear();
        try {
            stop.begin(f.level,20);
            require(!TimeStopView.frozen(f.owner)&&TimeStopView.frozen(f.other),"clock holder can equip a staff while a listener is frozen");
            f.owner.setItemInHand(InteractionHand.OFF_HAND,f.leaf("music_staff"));
            music.tick();
            music.tick();
            require(music.playing()&&staffMusic(f.client)==1&&staffMusic(f.audience)==0,
                    "held music starts immediately for active listeners and remains silent for the frozen one");
            stop.end(f.level);
            music.tick();
            music.tick();
            require(staffMusic(f.client)==1&&staffMusic(f.audience)==1,
                    "the still-held staff starts exactly once for the newly thawed listener");

            stop.begin(f.level,20);
            music.tick();
            stop.end(f.level);
            music.tick();
            require(staffMusic(f.audience)==1,"a listener that already received the track does not restart it on thaw");

            f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            music.tick();
            stop.begin(f.level,20);
            f.owner.setItemInHand(InteractionHand.OFF_HAND,f.leaf("music_staff"));
            music.tick();
            f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            music.tick();
            stop.end(f.level);
            music.tick();
            require(!music.playing()&&staffMusic(f.client)==2&&staffMusic(f.audience)==1,
                    "a musical session that ended during the stop is not replayed on thaw");
        } finally {
            stop.end(f.level);
            f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            music.stop();
        }
    }
    public static void run(GameController game){
        var stop=game.items().timeStop();require(!stop.stopped(game.maps().level()),"unpaused native fixture");
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();
            f.owner.entityTags().remove("have_stand");f.other.entityTags().remove("have_stand");
            var ordinary=checkOrdinaryWatch(f);
            var clocks=f.leaf("stand_clocks");f.owner.setItemInHand(InteractionHand.MAIN_HAND,clocks);
            f.other.getInventory().setItem(9,ordinary);
            f.client.packets().clear();f.audience.packets().clear();
            require(stop.useClocks(f.owner,InteractionHand.MAIN_HAND),"actual native clock activation");
            require(!stop.frozen(f.owner)&&stop.frozen(f.other)&&!TimeStopView.frozen(f.owner)&&TimeStopView.frozen(f.other),"only affected player loses view/audio; native clock immunity preserved");
            require(stop.remaining(f.level)==400&&stop.cooldownTicks(f.owner)==800&&stop.ordinaryCooldownTicks(f.owner)==0,
                    "powerful Stand Clock has its own duration and recharge");
            f.other.getInventory().setItem(9,ItemStack.EMPTY);
            require(f.other.getPostEffects().contains(TimeStopView.EFFECT)&&!f.owner.getPostEffects().contains(TimeStopView.EFFECT),"server activates actual player post-effect only for victim");
            f.other.connection.send(new ClientboundStopSoundPacket(Identifier.parse("minecraft:entity.experience_orb.pickup"),SoundSource.PLAYERS));
            require(flat(f.audience).stream().anyMatch(ClientboundStopSoundPacket.class::isInstance),"sound-stop controls remain deliverable while new sounds are suppressed");
            require(flat(f.audience).stream().anyMatch(p->p instanceof ClientboundSoundPacket s&&s.getSound().value().location().toString().equals("minecraft:lbcsounds.zawardo")),"actual activation sound survives after freeze");
            var sound=Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:entity.experience_orb.pickup")));
            var point=new ClientboundSoundPacket(sound,SoundSource.PLAYERS,f.home.x,f.home.y,f.home.z,1,1,123);
            var attached=new ClientboundSoundEntityPacket(sound,SoundSource.RECORDS,f.owner,1,1,123);
            var health=new ClientboundSetHealthPacket(18,20,5);
            f.client.packets().clear();f.audience.packets().clear();
            for(var player:List.of(f.owner,f.other)){player.connection.send(point);player.connection.send(attached);player.connection.send(new ClientboundLevelEventPacket(2001,player.blockPosition(),1,false));player.connection.send(new ClientboundBundlePacket(List.of(point,attached,health)));}
            require(sounds(f.audience)==0&&flat(f.audience).stream().noneMatch(ClientboundLevelEventPacket.class::isInstance),"real packet path removes positioned/attached/bundled/block event sound");
            require(flat(f.audience).stream().anyMatch(ClientboundSetHealthPacket.class::isInstance),"bundle gameplay state retained");
            require(sounds(f.client)==4&&flat(f.client).stream().anyMatch(ClientboundLevelEventPacket.class::isInstance),"immune recipient's normal sound unchanged");
            var fakeStart=new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:lbcsounds.zawardo"))),SoundSource.MASTER,0,0,0,1,1,0);
            f.other.connection.send(fakeStart);require(sounds(f.audience)==0,"Za Warudo exception is scoped to actual activation, not arbitrary later packets");
            var effects=new ClientboundPostEffectsPacket(List.of(TimeStopView.EFFECT,Identifier.parse("lbc:imprinted")));
            f.other.connection.send(effects);var sent=(ClientboundPostEffectsPacket)flat(f.audience).getLast();require(sent.postEffects().getLast().equals(TimeStopView.EFFECT),"freeze shader comes after all other player effects");
            stop.begin(f.level,440);require(TimeStopView.frozen(f.other)&&f.other.getPostEffects().stream().filter(TimeStopView.EFFECT::equals).count()==1,"extending stop does not replace frame or duplicate effect");
            f.other.setItemInHand(InteractionHand.OFF_HAND,clocks.copy());stop.tick();SignedEffectVisuals.synchronize(f.other);require(!TimeStopView.frozen(f.other)&&!f.other.getPostEffects().contains(TimeStopView.EFFECT),"gaining native clock immunity resumes frame and audio");
            f.other.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);stop.tick();SignedEffectVisuals.synchronize(f.other);require(TimeStopView.frozen(f.other),"losing immunity captures a fresh stopped view");
            stop.end(f.level);f.audience.packets().clear();f.other.connection.send(point);require(!TimeStopView.frozen(f.other)&&!f.other.getPostEffects().contains(TimeStopView.EFFECT)&&sounds(f.audience)==1,"native resume removes shader and restores future sound without replay queue");
            checkHeldStaff(game,f);
            stop.begin(f.level,20);stop.forget(f.other);require(!TimeStopView.frozen(f.other),"disconnect/forget withdraws sound gate");stop.stop();
        }finally{stop.stop();}
        SkyWars2.LOGGER.info("[check] PASS time-stop-view (reusable ordinary Loli clock both hands, native recharge, distinct Stand and Touhou clocks, Za Warudo/post-effects/audio, immunity transitions, held staff on thaw, extension/resume/forget cleanup)");
    }
}
