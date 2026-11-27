package me.skywars2.game;

import me.skywars2.game.items.tmt.LbcTmtDrops;
import me.skywars2.game.items.tmt.LbcTmtItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;

/** Real shared drop admission for a retained pre-change TMT marker. */
public final class FeedbackTmtDropChecks {
    private FeedbackTmtDropChecks() {}
    public static void run(GameController game){
        try(var fixture=new WoolBucketChecks.Fixture(game,true)){
            fixture.prepareParticipants();
            var stack=LbcTmtItems.prototype(2).copyWithCount(2);
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            var family=game.items().tmt().catalog().families().stream().filter(f->!f.events().isEmpty()
                    &&!f.locations().contains(me.skywars2.game.items.tmt.LbcTmtCatalog.Location.EQUIPPED)
                    &&f.events().stream().allMatch(e->e==me.skywars2.game.items.tmt.LbcTmtCatalog.Event.INVENTORY
                            ||e==me.skywars2.game.items.tmt.LbcTmtCatalog.Event.DROP)).findFirst().orElseThrow();
            me.skywars2.game.items.tmt.LbcTmtAbilities.write(data,java.util.List.of(family.variants().getFirst()));
            data.putInt("tmt_drop",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            if(!me.skywars2.game.items.tmt.LbcTmtTriggers.effective(stack).isEmpty())
                throw new IllegalStateException("old inventory-only TMT property remains active");
            if(!me.skywars2.game.items.tmt.LbcTmtTriggers.inputs(stack).contains(me.skywars2.game.items.tmt.LbcTmtTriggers.Input.TICK))
                throw new IllegalStateException("held TICK binding disappeared");
            var body=new ItemEntity(fixture.level,fixture.home.x,fixture.home.y+2,fixture.home.z,stack);
            fixture.own(body);
            if(!fixture.level.addFreshEntity(body))throw new IllegalStateException("feedback TMT fixture spawn");
            if(LbcTmtDrops.matches(stack)||!LbcTmtDrops.properties(stack).isEmpty()
                    ||game.items().specialDrops().spawn(body)||body.isRemoved()||body.getItem().getCount()!=2)
                throw new IllegalStateException("loose TMT must not dispatch or consume a unit");
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS feedback-tmt-drop");
    }
}
