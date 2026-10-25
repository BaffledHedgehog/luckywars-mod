package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Actual consumed awakened-acid/Exasperato inputs and native shared payload lifecycle. */
public final class CorrosiveGrimoireChecks {
    private CorrosiveGrimoireChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Corrosive grimoire: "+why);}
    private static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;
        final BlockPos wall;final BlockState oldWall;final int rifle;
        boolean retain;
        Fixture(GameController game){
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();
            require(game.items().exasperato().count()==0&&game.items().exasperato().pendingSearches()==0
                    &&game.items().exasperato().pendingBlasts()==0,"exclusive explosive services");
            rifle=base.mana.score(base.owner,"acid_rifle").get();base.mana.score(base.owner,"acid_rifle").set(0);
            wall=BlockPos.containing(base.owner.getEyePosition().add(0,0,8));oldWall=base.owner.level().getBlockState(wall);
            base.owner.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2|16|32);
        }
        void reset(){
            base.game.items().awakenedAcid().stop();base.game.items().clouds().stop();base.game.items().exasperato().stop();
            var bolts=new ArrayList<Entity>();
            for(var entity:base.owner.level().getAllEntities())if(!base.before.contains(entity)&&entity.getType()==EntityTypes.LIGHTNING_BOLT)bolts.add(entity);
            bolts.forEach(Entity::discard);
            base.recording.packets.clear();base.owner.setYRot(0);base.owner.setXRot(0);
        }
        void payload(LbcElementalGrimoires.Kind kind){
            var items=base.game.items();var owner=base.owner;
            var acid=items.clouds().acidSnapshot();var charges=items.exasperato().snapshot();
            if(kind.spellId<=194){
                if(kind==LbcElementalGrimoires.Kind.AWAKENED_ACID_RIFLE){
                    require(acid.isEmpty()&&items.awakenedAcid().pending(owner)==10,"ten queued rifle releases");
                }else{
                    boolean burst=kind==LbcElementalGrimoires.Kind.AWAKENED_ACID_GATLING||kind==LbcElementalGrimoires.Kind.AWAKENED_ACID_GATLING_BURNING;
                    boolean burning=kind==LbcElementalGrimoires.Kind.AWAKENED_ACID_CAUST_BURNING||kind==LbcElementalGrimoires.Kind.AWAKENED_ACID_GATLING_BURNING;
                    int variant=burst?2:kind==LbcElementalGrimoires.Kind.AWAKENED_ACID?0:1;
                    require(acid.size()==(burst?8:1),"native acid count "+kind);
                    for(var shot:acid)require(shot.getOwner()==owner&&items.currentEpoch(shot)&&shot.isNoGravity()
                            &&LbcClouds.awakenedVariant(shot)==variant&&shot.entityTags().contains("on_fire")==burning
                            &&LbcClouds.acidLifetime(shot)==(burst?200:500),"acid variant, burning, lifetime and ownership "+kind);
                }
            }else switch(kind){
                case EXASPERATO -> require(charges.size()==1&&charges.getFirst().mode==0&&!charges.getFirst().academyTrial,"one ordinary charge without main-hand trial leakage");
                case EXASPERATO_TRIPLE -> require(charges.size()==3&&charges.stream().allMatch(p->p.mode==1)
                        &&charges.stream().map(p->p.lifetime).sorted().toList().equals(List.of(0,50,100)),"shared three-charge pattern");
                case EXASPERATO_RAY -> require(charges.size()==1&&charges.getFirst().mode==2&&charges.getFirst().lifetime==200,"one native electro ray");
                case EXASPERATO_CHAOS -> {
                    int bolts=0;for(var entity:owner.level().getAllEntities())if(!base.before.contains(entity)){
                        require(entity.getType()!=EntityTypes.CREEPER&&entity.getType()!=EntityTypes.TNT,"native instant explosions have no carrier mob/TNT");
                        if(entity.getType()==EntityTypes.LIGHTNING_BOLT)bolts++;
                    }
                    require(bolts>0&&charges.isEmpty(),"shared chaos native lightning and instantaneous blasts");
                }
                case EXASPERATO_NUKE -> {
                    require(items.exasperato().pendingSearches()==1,"queued loaded-chunk nuke aim");
                    for(int i=0;i<30&&items.exasperato().pendingSearches()>0;i++)items.exasperato().tick();
                    charges=items.exasperato().snapshot();
                    require(charges.size()==1&&charges.getFirst().mode==4&&charges.getFirst().getY()>owner.getY()+490
                            &&charges.getFirst().getDeltaMovement().y==-1,"native nuke above its actual ray hit");
                }
                default -> throw new IllegalArgumentException("corrosive kind "+kind);
            }
            for(var shot:charges)require(shot.getOwner()==owner&&items.currentEpoch(shot)&&shot.isNoGravity(),"explosive ownership/epoch");
            for(var entity:owner.level().getAllEntities())require(base.before.contains(entity)||entity.getType()!=EntityTypes.SNOWBALL,"no carrier snowball");
        }
        void casts(){
            for(var kind:LbcElementalGrimoires.corrosive()){
                base.presentation(kind);
                for(var hand:InteractionHand.values())for(boolean shift:new boolean[]{false,true}){
                    reset();var stack=base.held(kind,hand,1);base.owner.setShiftKeyDown(shift);base.mana.set(base.owner,shift?-500:0);
                    int balance=base.mana.get(base.owner),used=base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item));
                    require(base.input(hand)&&stack.isEmpty()&&base.mana.get(base.owner)==balance,"final book, no mana admission/debit "+kind+" "+hand+" shift="+shift);
                    require(base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(kind.item))==used+1,"one native use stat "+kind);payload(kind);
                }
            }
            reset();base.owner.setShiftKeyDown(false);
            var ordinary=base.held(LbcElementalGrimoires.Kind.EXASPERATO,InteractionHand.OFF_HAND,1);
            var fakeTrial=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
            var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("upgradable",1);
            fakeTrial.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,fakeTrial);
            require(base.input(InteractionHand.OFF_HAND)&&ordinary.isEmpty()&&!base.game.items().exasperato().snapshot().getFirst().academyTrial,
                    "offhand consumed explosion cannot inherit the unrelated main-hand trial");
            reset();base.owner.setShiftKeyDown(false);var stack=base.held(LbcElementalGrimoires.Kind.AWAKENED_ACID_RIFLE,InteractionHand.MAIN_HAND,2);
            require(base.input(InteractionHand.MAIN_HAND)&&base.input(InteractionHand.MAIN_HAND)&&stack.isEmpty()
                    &&base.game.items().awakenedAcid().pending(base.owner)==20,"consumed rifle accumulates twenty releases");
            for(int i=0;i<5;i++)base.game.items().awakenedAcid().tick();
            require(base.game.items().awakenedAcid().pending(base.owner)==19&&base.game.items().clouds().acidSnapshot().size()==1,"rifle continues after book consumption");reset();
            var main=base.held(LbcElementalGrimoires.Kind.AWAKENED_ACID_GATLING_BURNING,InteractionHand.MAIN_HAND,1);
            var off=base.loot(LbcElementalGrimoires.Kind.EXASPERATO_TRIPLE);base.owner.setItemInHand(InteractionHand.OFF_HAND,off);
            require(base.input(InteractionHand.MAIN_HAND)&&main.isEmpty()&&off.isEmpty()&&base.game.items().clouds().acidSnapshot().size()==8
                    &&base.game.items().exasperato().count()==3,"parallel consumed families");reset();
        }
        void guards(){for(var kind:LbcElementalGrimoires.corrosive()){
            var stack=base.held(kind,InteractionHand.MAIN_HAND,2);base.game.items().timeStop().begin(base.owner.level(),40);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"time stop preserves "+kind);}finally{base.game.items().timeStop().end(base.owner.level());}
            base.field=new Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(base.owner.position());base.field.entityTags().add("stopper_magic");
            require(base.owner.level().addFreshEntity(base.field),"antimagic fixture");base.game.items().magic().loaded(base.field);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"antimagic preserves "+kind);}finally{base.removeField();}
            stack=base.held(kind,InteractionHand.OFF_HAND,2);base.game.config().offhandWands=false;
            try{require(!base.input(InteractionHand.OFF_HAND)&&stack.getCount()==2,"disabled offhand preserves "+kind);}finally{base.game.config().offhandWands=true;}
        }}
        @Override public void close(){if(!retain)reset();base.owner.level().setBlock(wall,oldWall,2|16|32);base.retain=retain;base.close();if(!retain)base.mana.score(base.owner,"acid_rifle").set(rifle);}
    }
    private static String epoch;private static UUID owner;private static int oldRifle;private static List<Entity> retained=List.of();
    public static void run(GameController game){try(var f=new Fixture(game)){f.casts();f.guards();}SkyWars2.LOGGER.info("[check] PASS corrosive-grimoires");}
    public static void stopProbe(GameController game){require(epoch==null,"exclusive stop fixture");try(var f=new Fixture(game)){
        for(var kind:List.of(LbcElementalGrimoires.Kind.AWAKENED_ACID_CAUST_BURNING,LbcElementalGrimoires.Kind.AWAKENED_ACID_RIFLE,LbcElementalGrimoires.Kind.EXASPERATO_TRIPLE)){
            f.base.held(kind,InteractionHand.MAIN_HAND,1);require(f.base.input(InteractionHand.MAIN_HAND),"retained actual input "+kind);
        }
        var actors=new ArrayList<Entity>();actors.addAll(game.items().clouds().acidSnapshot());actors.addAll(game.items().exasperato().snapshot());
        require(actors.size()==4&&game.items().awakenedAcid().pending(f.base.owner)==10,"retained native acids, charges and rifle");
        actors.forEach(e->e.entityTags().add("time_stopped"));retained=List.copyOf(actors);
        epoch=game.items().epoch();owner=f.base.owner.getUUID();oldRifle=f.rifle;f.retain=true;
    }SkyWars2.LOGGER.info("[check] PASS corrosive-grimoires-stop-probe");}
    public static void cleanup(GameController game){require(epoch!=null,"retained epoch");try{
        var p=game.server().getPlayerList().getPlayer(owner);
        require(!game.items().currentEpoch(epoch)&&retained.stream().allMatch(Entity::isRemoved)&&game.items().clouds().acidCount()==0
                &&game.items().exasperato().count()==0&&game.items().exasperato().pendingSearches()==0&&game.items().exasperato().pendingBlasts()==0
                &&game.items().awakenedAcid().pending(p)==0,"actual stop clears shared projectiles, searches, blasts and rifle");
        game.items().mana().score(p,"acid_rifle").set(oldRifle);
    }finally{epoch=null;owner=null;retained=List.of();}SkyWars2.LOGGER.info("[check] PASS corrosive-grimoires-cleanup");}
}
