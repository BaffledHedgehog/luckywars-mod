package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** Actual raw hand input, exact original rewards and immediate repeated admission. */
public final class BigBobChecks {
    private BigBobChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Big Bob: "+why);}
    public static void run(GameController game){
        try(var base=new ElementalGrimoireChecks.Fixture(game)){
            var p=base.owner;var level=p.level();var random=MatchRandomState.capture(level.getRandom());
            var playerRandom=MatchRandomState.capture(p.getRandom());
            int uses=p.getStats().getValue(Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK));
            try{
                base.initialize();
                for(var hand:InteractionHand.values()){
                    game.config().offhandWands=hand==InteractionHand.OFF_HAND;
                    var wand=game.lbc().rollTable(level,p.position(),p,"lbc:big_bob").getFirst();
                    require(wand.is(Items.CARROT_ON_A_STICK)&&LbcBigBob.matches(wand)
                            &&LbcFartus.data(wand).getIntOr("magicwand",0)==1,"actual raw source identity and flags");
                    var unchanged=wand.copy();
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                    p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(hand,wand);
                    var packets=base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
                    if(!packets.isEmpty())p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(packets.getLast().id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
                    p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                    int mana=game.items().mana().get(p);
                    for(int repeat=0;repeat<2;repeat++){
                        var rng=MatchRandomState.capture(level.getRandom());
                        var expected=new ArrayList<>(game.lbc().rollTable(level,p.position().add(0,1,0),p,"lbc:oreshki_10"));rng.restore(level.getRandom());
                        require(expected.size()==10,"source ten independent max-stack-one rewards");
                        require(p.gameMode.useItem(p,level,wand,hand).consumesAction(),"actual raw input "+hand+" repeat="+repeat);
                        var drops=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(e instanceof ItemEntity item&&!base.before.contains(item)&&!item.isRemoved())drops.add(item);
                        require(drops.size()==10,"exact ten native reward actors "+hand);
                        for(var drop:drops){
                            int found=-1;for(int n=0;n<expected.size();n++)if(ItemStack.matches(expected.get(n),drop.getItem())){found=n;break;}
                            require(found>=0,"unchanged source reward components");expected.remove(found);
                            require(LbcFartus.data(drop.getItem()).getIntOr("eat_oreshek",0)==1&&drop.getItem().has(DataComponents.CONSUMABLE),"reward enters existing native nut consumption owner");
                            require(drop.getOwner()==p&&drop.position().distanceToSqr(p.position().add(0,1,0))<1e-12,"exact source position and current thrower");
                            game.items().entityUnloaded(drop);drop.discard();
                        }
                        require(ItemStack.matches(wand,unchanged)&&game.items().mana().get(p)==mana,"no debit, cooldown or stack component mutation");
                    }
                }
                game.config().offhandWands=false;
                require(!game.items().wands().use(p,InteractionHand.OFF_HAND),"disabled offhand rejected");
                p.setItemInHand(InteractionHand.MAIN_HAND,p.getOffhandItem().copy());
                p.entityTags().add("time_stopped");
                try{require(!LbcBigBob.use(game,p,InteractionHand.MAIN_HAND),"frozen input rejected");}finally{p.entityTags().remove("time_stopped");}
            }finally{random.restore(level.getRandom());playerRandom.restore(p.getRandom());p.getStats().setValue(p,Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK),uses);}
        }
        SkyWars2.LOGGER.info("[check] PASS big-bob (actual raw two-hand input; ten exact original nut components/actors/position/author; immediate repeat without mana/debit/cooldown; disabled offhand; scoped restore)");
    }
}
