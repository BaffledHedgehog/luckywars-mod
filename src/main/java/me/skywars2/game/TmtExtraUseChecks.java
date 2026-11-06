package me.skywars2.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Actual input in Nexus follows the selected native owner, while ordinary spell policy remains intact. */
public final class TmtExtraUseChecks {
    private TmtExtraUseChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT extra use: "+why);}
    private static ItemStack item(List<LbcTmtAbilities.Ability> properties){
        var result=LbcTmtItems.prototype(0);var root=result.get(DataComponents.CUSTOM_DATA).copyTag();
        LbcTmtAbilities.write(root,properties);result.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return result;
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var runtime=game.items().tmt();var use=runtime.useProfiles();
            require(game.items().exasperato().pendingSearches()==0&&game.items().nexusCombat().pending()==0,"exclusive native input/search scene");
            var orb=use.families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt("nexus_orb_gun",0)==1).findFirst().orElseThrow();
            var fire=use.families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt("smallfireball",0)==1).findFirst().orElseThrow();
            var tiny=LbcWands.tmtAbilities().stream().filter(p->p.nativeItem().toString().equals("skywars2:tiny_fire_wand")).findFirst().orElseThrow().inMode(0);
            var nexus=game.server().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse("minecraft:nexus")));require(nexus!=null,"native Nexus dimension loaded");
            var ordinary=f.owner.level();var at=f.owner.position();var beforeNexus=new HashSet<java.util.UUID>();for(var actor:nexus.getAllEntities())beforeNexus.add(actor.getUUID());
            var beforeOrdinary=new HashSet<java.util.UUID>();for(var actor:f.level.getAllEntities())beforeOrdinary.add(actor.getUUID());
            var stat=net.minecraft.stats.Stats.ITEM_USED.get(LbcTmtItems.prototype(0).getItem());int used=f.other.getStats().getValue(stat);
            try{
                nexus.getChunk(0,0);require(f.owner.teleportTo(nexus,1,150,1,Set.of(),0,0,true),"actual native dimension transfer");f.client.acknowledge();
                var carried=item(List.of(orb,tiny));f.owner.setItemInHand(InteractionHand.MAIN_HAND,carried);
                game.items().mana().maximum(f.owner,10000);game.items().mana().set(f.owner,1000);
                int charge=game.items().mana().score(f.owner,"crossbow_charge").get();f.owner.experienceLevel=10;f.owner.experienceProgress=.5f;
                require(runtime.admitted(f.owner),"global TMT input does not suppress a valid Nexus owner");
                require(f.owner.gameMode.useItem(f.owner,nexus,carried,InteractionHand.MAIN_HAND).consumesAction(),"actual TMT input reaches native Nexus Orb Gun");
                require(f.owner.experienceLevel<10&&game.items().nexusCombat().pending()==1,"native current-progress XP debit and actual ray admission run once");
                require(game.items().mana().get(f.owner)==1000&&game.items().mana().score(f.owner,"crossbow_charge").get()==charge,"ordinary Tiny spell and ordinary-only charge group remain disabled in Nexus");
                var throwable=item(List.of(fire));f.other.setItemInHand(InteractionHand.MAIN_HAND,throwable);
                require(f.other.gameMode.useItem(f.other,f.level,throwable,InteractionHand.MAIN_HAND).consumesAction()&&throwable.isEmpty(),"native special-fireball owner accepts the scoped carrot source and owns its one debit");
                int actors=0;for(var body:f.level.getAllEntities())if(!beforeOrdinary.contains(body.getUUID())&&LbcSpecialFireballs.managed(body))actors++;
                require(actors>0&&!LbcTmtAbilities.nativePredicatesActive(),"native final fireball body published and source classifier restored");
            }finally{
                game.items().nexusCombat().stop();game.items().exasperato().stop();game.items().specialFireballs().stop();
                f.other.getStats().setValue(f.other,stat,used);
                if(f.owner.level()!=ordinary){f.owner.teleportTo(ordinary,at.x,at.y,at.z,Set.of(),0,0,true);f.client.acknowledge();}
                for(var body:nexus.getAllEntities())if(!beforeNexus.contains(body.getUUID())&&body!=f.owner){game.items().entityUnloaded(body);body.discard();}
                for(var body:f.level.getAllEntities())if(!beforeOrdinary.contains(body.getUUID()))f.own(body);
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-extra-use (actual Nexus input/XP/search with ordinary spell isolation; scoped native fireball class, real final actor and sole debit; dimension/source restoration)");
    }
}
