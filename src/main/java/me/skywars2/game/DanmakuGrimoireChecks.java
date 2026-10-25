package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Isolated synchronous native consumed159 input and external queue contracts. */
public final class DanmakuGrimoireChecks {
    private DanmakuGrimoireChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Danmaku grimoire: "+why);}
    private static int flag(ItemStack stack,String key){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcDanmaku service;final LbcMana mana;final ServerPlayer owner;
        final GameChecks.WandThirdPlayerState saved;final boolean offhand,shift,sprint,ground;final int charge,timer,used;
        boolean retain;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        Marker field;
        Fixture(GameController game){
            this.game=game;service=game.items().danmaku();mana=game.items().mana();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(owner!=null&&game.playing()&&game.canUseGameplayItems(owner)&&!game.items().timeStop().stopped(owner.level())&&!owner.entityTags().contains("time_stopped"),"active unpaused participant");
            require(service.count()==0&&service.remaining(owner)==0&&service.externalRemaining(owner)==0&&owner.containerMenu==owner.inventoryMenu,"exclusive empty service and closed menu");
            for(var online:game.server().getPlayerList().getPlayers())require(service.remaining(online)==0&&service.externalRemaining(online)==0,"all online players have empty Danmaku queues before explicit ticks");
            saved=new GameChecks.WandThirdPlayerState(owner,mana);offhand=game.config().offhandWands;shift=owner.isShiftKeyDown();sprint=owner.isSprinting();ground=owner.onGround();charge=mana.score(owner,"danmaku_score").get();timer=mana.score(owner,"danmaku").get();used=owner.getStats().getValue(Stats.ITEM_USED.get(LbcDanmaku.GRIMOIRE));
            owner.level().getAllEntities().forEach(before::add);
        }
        void initialize(){
            var at=new Vec3(owner.getX(),Math.min(240,owner.level().getMaxY()-40),owner.getZ());
            require(before.stream().noneMatch(e->e!=owner&&e.position().distanceToSqr(at)<40*40),"empty40block flight sphere");
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-28,-4,-28)),BlockPos.containing(at.add(28,8,28))))require(owner.level().hasChunkAt(pos)&&owner.level().getBlockState(pos).isAir(),"resident air flight volume");
            game.config().offhandWands=true;owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setShiftKeyDown(false);owner.entityTags().remove("time_stopped");
            owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);owner.getInventory().setSelectedSlot(0);mana.set(owner,0);mana.score(owner,"danmaku_score").set(0);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
        }
        ItemStack loot(){var out=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/danmaku_grimoire");require(out.size()==1&&out.getFirst().getCount()==1,"narrow singleton loot");return out.getFirst();}
        ItemStack held(InteractionHand hand,int count){owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var stack=loot();stack.setCount(count);owner.setItemInHand(hand,stack);return stack;}
        boolean input(InteractionHand hand){return owner.gameMode.useItem(owner,owner.level(),owner.getItemInHand(hand),hand).consumesAction();}
        void presentation(){
            var stack=loot();require(stack.is(LbcDanmaku.GRIMOIRE)&&LbcDanmaku.isGrimoire(stack)&&!LbcDanmaku.isItem(stack)&&stack.getMaxStackSize()==99,"native consumable identity and source stack99");
            require(Identifier.parse("lbc:spelllib/danmaku").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_c").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source model and C tier");
            for(var key:List.of("transformable","gadget","libspell","unresolved_grimoire","spellwithcooldown"))require(flag(stack,key)==1,"source marker "+key);
            require(flag(stack,"mana")==1200&&flag(stack,"spellid")==159&&flag(stack,"upgraded")==0,"metadata1200 and ordinary159");
            var lore=stack.get(DataComponents.LORE).lines();require(lore.size()==10,"ten authored lore rows");
            var expected=new ArrayList<net.minecraft.network.chat.Component>();
            expected.add(net.minecraft.network.chat.Component.translatable("type").withStyle(net.minecraft.ChatFormatting.AQUA).withStyle(s->s.withItalic(false)).append(net.minecraft.network.chat.Component.translatable("libspell").withStyle(net.minecraft.ChatFormatting.AQUA).withStyle(s->s.withItalic(false))));
            expected.add(net.minecraft.network.chat.Component.literal(""));
            for(var key:List.of("danmaku_wand.lore.1","danmaku_wand.lore.2","danmaku_wand.lore.3","danmaku_wand.lore.4","danmaku_wand.lore.5","danmaku_wand.lore.6","danmaku_wand.lore.7"))expected.add(net.minecraft.network.chat.Component.translatable(key).withStyle(net.minecraft.ChatFormatting.WHITE).withStyle(s->s.withItalic(false)));
            expected.add(net.minecraft.network.chat.Component.translatable("rarity").withStyle(net.minecraft.ChatFormatting.WHITE).withStyle(s->s.withItalic(false)).append(net.minecraft.network.chat.Component.translatable("rare").withStyle(net.minecraft.ChatFormatting.YELLOW).withStyle(s->s.withItalic(false))));
            require(lore.equals(expected),"exact authored grimoire lore components");
        }
        void guards(){
            var stack=held(InteractionHand.MAIN_HAND,3);owner.entityTags().add("time_stopped");try{require(!input(InteractionHand.MAIN_HAND)&&stack.getCount()==3&&service.externalRemaining(owner)==0,"personal stop blocks consumption");}finally{owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(owner.level(),40);try{require(!input(InteractionHand.MAIN_HAND)&&stack.getCount()==3,"dimension stop blocks consumption");}finally{game.items().timeStop().end(owner.level());}
            field=new Marker(EntityTypes.MARKER,owner.level());field.setPos(owner.position());field.entityTags().add("stopper_magic");require(owner.level().addFreshEntity(field),"native antimagic fixture");game.items().magic().loaded(field);
            try{require(!input(InteractionHand.MAIN_HAND)&&stack.getCount()==3,"antimagic blocks consumption");}finally{removeField();}
            stack=held(InteractionHand.OFF_HAND,3);game.config().offhandWands=false;try{require(!input(InteractionHand.OFF_HAND)&&stack.getCount()==3,"disabled offhand blocks consumption");}finally{game.config().offhandWands=true;}
        }
        void patterns(){
            for(var hand:InteractionHand.values()){
                var stack=held(hand,1);mana.set(owner,hand==InteractionHand.MAIN_HAND?0:-100);
                int initial=mana.get(owner),stat=owner.getStats().getValue(Stats.ITEM_USED.get(LbcDanmaku.GRIMOIRE));
                require(input(hand)&&stack.isEmpty()&&service.externalRemaining(owner)==20&&mana.get(owner)==initial,"last actual used stack consumes with zero/negative mana "+hand);
                require(owner.getStats().getValue(Stats.ITEM_USED.get(LbcDanmaku.GRIMOIRE))==stat+1,"native use statistic");
                require(noSnowballs(),"consumption spawns no thrown snowball");owner.setItemInHand(hand,ItemStack.EMPTY);
                game.items().timeStop().begin(owner.level(),40);try{service.tick();require(service.externalRemaining(owner)==20&&service.cards().isEmpty(),"queued grimoire pauses under Time Stop");}finally{game.items().timeStop().end(owner.level());}
                field=new Marker(EntityTypes.MARKER,owner.level());field.setPos(owner.position());field.entityTags().add("stopper_magic");require(owner.level().addFreshEntity(field),"queued antimagic fixture");game.items().magic().loaded(field);
                try{service.tick();require(service.externalRemaining(owner)==20&&service.cards().isEmpty(),"queued grimoire pauses under antimagic");}finally{removeField();}
                Set<UUID> emitted=new HashSet<>();int homing=0;
                for(int i=0;i<20;i++){service.tick();for(var card:service.cards())if(emitted.add(card.display.getUUID())&&card.homing)homing++;}
                require(service.externalRemaining(owner)==0&&emitted.size()==(hand==InteractionHand.MAIN_HAND?40:35)&&homing==(hand==InteractionHand.MAIN_HAND?8:12),"captured ordinary source pattern after final consumption "+hand+" cards="+emitted.size()+" homing="+homing);
                require(mana.get(owner)==initial,"external ticking does not debit mana");service.forget(owner);
            }
            var stack=held(InteractionHand.MAIN_HAND,3);require(input(InteractionHand.MAIN_HAND)&&input(InteractionHand.MAIN_HAND)&&stack.getCount()==1&&service.externalRemaining(owner)==40,"successive consumed casts add40 ticks");service.forget(owner);
        }
        void dual(){
            var main=held(InteractionHand.MAIN_HAND,1);var off=loot();owner.setItemInHand(InteractionHand.OFF_HAND,off);
            require(input(InteractionHand.MAIN_HAND)&&main.isEmpty()&&off.isEmpty()&&service.externalRemaining(owner)==40,"both actual consumed hands queue independently");
            Set<UUID> emitted=new HashSet<>();for(int i=0;i<20;i++){service.tick();for(var card:service.cards())emitted.add(card.display.getUUID());}
            require(service.externalRemaining(owner)==0&&emitted.size()==75,"simultaneous captured hands retain40+35 patterns");service.forget(owner);
        }
        void bombs(){
            for(int charge:new int[]{49,50,1500}){
                var stack=held(InteractionHand.MAIN_HAND,2);owner.setShiftKeyDown(true);mana.set(owner,-100);mana.score(owner,"danmaku_score").set(charge);
                if(charge==1500){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("upgradable",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
                require(input(InteractionHand.MAIN_HAND)&&stack.getCount()==1&&mana.get(owner)==-100&&service.externalRemaining(owner)==0,"consumed free sneak grimoire without refund "+charge);
                if(charge<50)require(service.bombs().isEmpty()&&service.count()==0&&mana.score(owner,"danmaku_score").get()==49,"below50 consumes original throw but creates no bomb");
                else{require(service.bombs().size()==1&&service.count()==1&&mana.score(owner,"danmaku_score").get()==0,"source threshold creates exactly one two-shell bomb "+charge);var bomb=service.bombs().getFirst();require(bomb.charge==charge&&bomb.owner==owner&&!bomb.positive.isRemoved()&&!bomb.negative.isRemoved()&&bomb.positive!=bomb.negative,"two live captured-owner source shells");}
                require(flag(stack,"upgrade_available")==0&&flag(stack,"upgraded")==0,"grimoire bomb never promotes or readies a forged staff trial");service.forget(owner);owner.setShiftKeyDown(false);
            }
        }
        void observer(){game.spectators().enter(owner);var stack=held(InteractionHand.MAIN_HAND,2);require(game.isObserver(owner)&&!input(InteractionHand.MAIN_HAND)&&stack.getCount()==2&&service.externalRemaining(owner)==0,"Adventure observer role blocks copied grimoire");}
        boolean noSnowballs(){for(var entity:owner.level().getAllEntities())if(!before.contains(entity)&&entity.getType()==EntityTypes.SNOWBALL)return false;return true;}
        void removeField(){if(field!=null){game.items().magic().unloaded(field);field.discard();field=null;game.items().magic().flushCleansing();}}
        @Override public void close(){removeField();game.items().timeStop().end(owner.level());if(!retain)service.forget(owner);game.config().offhandWands=offhand;saved.restore(game);owner.setShiftKeyDown(shift);owner.setSprinting(sprint);owner.setOnGround(ground);mana.score(owner,"danmaku_score").set(charge);mana.score(owner,"danmaku").set(timer);owner.getStats().setValue(owner,Stats.ITEM_USED.get(LbcDanmaku.GRIMOIRE),used);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}
    }
    private static List<Entity> retained;
    private static String retainedEpoch;
    public static void stopProbe(GameController game){
        require(retained==null,"exclusive retained grimoire fixture");
        try(var fixture=new Fixture(game)){
            fixture.initialize();fixture.held(InteractionHand.MAIN_HAND,1);require(fixture.input(InteractionHand.MAIN_HAND),"actual retained consumed input");fixture.service.tick();
            var actors=new ArrayList<Entity>();for(var card:fixture.service.cards()){actors.add(card.display);card.display.entityTags().add("time_stopped");}
            require(!actors.isEmpty()&&fixture.service.externalRemaining(fixture.owner)==19,"real retained cards and remaining external queue");
            retained=List.copyOf(actors);retainedEpoch=game.items().epoch();fixture.retain=true;
        }
        SkyWars2.LOGGER.info("[check] PASS danmaku-grimoire-stop-probe");
    }
    public static void cleanup(GameController game){
        var actors=retained;require(actors!=null&&!actors.isEmpty(),"retained consumed cards");
        try{require(!game.items().currentEpoch(retainedEpoch)&&game.items().danmaku().count()==0&&actors.stream().allMatch(Entity::isRemoved),"actual match stop retires consumed cards and epoch");
            for(var player:game.server().getPlayerList().getPlayers())require(game.items().danmaku().externalRemaining(player)==0,"actual stop clears external queues");
        }finally{actors.forEach(Entity::discard);retained=null;retainedEpoch=null;}
        SkyWars2.LOGGER.info("[check] PASS danmaku-grimoire-cleanup");
    }
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.initialize();fixture.presentation();fixture.guards();fixture.patterns();fixture.dual();fixture.bombs();fixture.observer();}SkyWars2.LOGGER.info("[check] PASS danmaku-grimoire");}
}
