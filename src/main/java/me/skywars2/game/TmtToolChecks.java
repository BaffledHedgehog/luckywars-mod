package me.skywars2.game;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;

/** Actual game-mode block destruction and player contact, not manufactured tool-use events. */
public final class TmtToolChecks {
    private TmtToolChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT tools: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var owner=f.owner;var stone=game.items().stonekivi();var hoe=game.items().imbaHoe();var axe=game.items().imbaAxe();
            require(stone.count()==0&&hoe.state().active()==0&&axe.count()==0,"exclusive empty native work queues");
            var stack=LbcTmtItems.prototype(0);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            var slots=new java.util.ArrayList<LbcTmtAbilities.Ability>();
            for(var flag:List.of("chaos_hoe","chaos_sho","chaos_pic")){
                var data=new CompoundTag();data.putInt(flag,1);slots.add(new LbcTmtAbilities.Ability("tool/"+flag,CustomData.of(data)));
            }
            var identity=new CompoundTag();identity.putString(LbcTmtAbilities.NATIVE,"skywars2:imba_axe");
            slots.add(new LbcTmtAbilities.Ability("tool/axe",CustomData.of(identity)));
            LbcTmtAbilities.write(root,slots);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            var original=stack.get(DataComponents.CUSTOM_DATA);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var at=owner.blockPosition().east();f.set(at,Blocks.STONE.defaultBlockState());
            var initialHoe=hoe.state();
            try{
                require(LbcTmtTools.blockCandidate(stack),"private identity-only Axe and flagged tool profiles are eligible");
                require(owner.gameMode.destroyBlock(at)&&f.level.getBlockState(at).isAir(),"one real successful block break");
                require(game.items().imbaPickaxe().snapshot().size()==1&&game.items().imbaShovel().snapshot().size()==1,"connected mining and piston stroke each accepted once");
                require(hoe.state().uses()==initialHoe.uses()+1&&axe.count()==1,"one native hoe ray and identity-only axe job accepted");
                owner.gameMode.destroyBlock(at); // Vanilla returns true even for an already empty cell.
                require(game.items().imbaPickaxe().snapshot().size()==1&&game.items().imbaShovel().snapshot().size()==1
                        &&hoe.state().uses()==initialHoe.uses()+1,"an empty-cell action cannot dispatch property work");
                f.move(f.other,owner.position().add(0,0,1));owner.resetAttackStrengthTicker();owner.attack(f.other);
                require(game.items().imbaPickaxe().snapshot().size()==1&&game.items().imbaShovel().snapshot().size()==2
                        &&hoe.state().uses()==initialHoe.uses()+2,"real melee contact adds stroke/ray without fabricating a mined block");
                require(axe.count()==1,"native axe owner merges repeated activation into its existing job");
                require(owner.getMainHandItem()==stack&&stack.get(DataComponents.CUSTOM_DATA).equals(original)
                        &&!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"actual held identity and four private states restored");
            }finally{
                stone.cancelQueuedForRollback();hoe.stop();axe.stop();
            }
            require(stone.count()==0&&axe.count()==0&&hoe.state().active()==0,"all test-owned accepted work canceled at teardown");
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-tools (real successful/failed block breaks and melee contact; four private native tool owners, captured source, identity-only axe, no duplicate action or invented mining event)");
    }
}
