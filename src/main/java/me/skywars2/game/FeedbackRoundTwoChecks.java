package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual native menus, raid packets, damage callbacks and scheduled display lifetimes. */
public final class FeedbackRoundTwoChecks {
    private FeedbackRoundTwoChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Feedback round two: "+why);}
    private static Fixture running;
    private static java.util.stream.Stream<net.minecraft.network.protocol.Packet<?>> flatten(net.minecraft.network.protocol.Packet<?> packet){
        if(packet instanceof net.minecraft.network.protocol.game.ClientboundBundlePacket bundle)
            return java.util.stream.StreamSupport.stream(bundle.subPackets().spliterator(),false).flatMap(FeedbackRoundTwoChecks::flatten);
        return java.util.stream.Stream.of(packet);
    }
    public static void prepareLuck(GameController game,int value){
        require(game.inLobby(),"actual lobby before fixed LuckyWars start");
        require(game.luckyModifiers().current()==1,"previous actual match result is cleared before next preparation");
        game.configureLobbySetting(null,"gamemode","2");game.configureLobbySetting(null,"luckymodifier",Integer.toString(value));
        require(game.config().gameMode==2&&game.config().luckyModifier==value,"real settings for consecutive LuckyWars test");
        SkyWars2.LOGGER.info("[check] PASS lucky-reset-prepare{}",value);
    }
    public static void startedLuck(GameController game,int value){
        require(game.playing()&&game.gameMode()==2&&game.luckyModifiers().current()==value,"real committed LuckyWars match category "+value);
        var score=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("final_luck_modifier"),"lbc.math");
        require(score.get()==value&&game.config().luckyModifier==value,"real startup scoreboard and untouched selector");
        SkyWars2.LOGGER.info("[check] PASS lucky-start-fixed{}",value);
    }
    public static void endedLuck(GameController game){
        var score=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("final_luck_modifier"),"lbc.math");
        require(game.inLobby()&&score.get()==1&&game.luckyModifiers().current()==1,"real stop/lobby removes previous LuckyWars result");
        game.configureLobbySetting(null,"gamemode","1");
        SkyWars2.LOGGER.info("[check] PASS lucky-reset-lobby");
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcFadingBeams beams;
        int began,when,phase;List<LbcFadingBeams.Image> frozen;boolean whiteFinished;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);beams=(LbcFadingBeams)WoolBucketChecks.read(game.items().hedgehog(),"beamVisuals");}
        void setup(){
            f.prepareParticipants();LuckyModifierChecks.resolver(game);crafts();katana();raid();spawnerTags();rays();began=when=game.server().getTickCount();
        }
        ItemStack tablet(int kind){return f.leaf("multiply_"+kind).copyWithCount(2);}
        ItemStack donor(int kind){
            var stack=f.nested(new ItemStack(Items.DIAMOND,13),91);
            if(kind==2)stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemStack(Items.IRON_SWORD).get(DataComponents.ATTRIBUTE_MODIFIERS));
            if(kind==4){var enchant=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);enchant.set(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),600_000_000);stack.set(DataComponents.ENCHANTMENTS,enchant.toImmutable());}
            return stack;
        }
        void crafts(){
            var p=f.owner;var menu=p.inventoryMenu;var grid=menu.getCraftSlots();
            for(int kind=2;kind<=4;kind++){
                p.getInventory().clearContent();grid.clearContent();menu.setCarried(ItemStack.EMPTY);
                var original=donor(kind);var expected=LbcMultiplierTablets.amplified(original,kind);
                grid.setItem(0,original.copy());grid.setItem(3,tablet(kind));
                require(ItemStack.matches(menu.getResultSlot().getItem(),expected),"x"+kind+" preview copies full13-stack with all untouched components");
                GameChecks.inventoryClick(p,0,0,ContainerInput.PICKUP);
                require(ItemStack.matches(menu.getCarried(),expected)&&grid.getItem(3).getCount()==1,"native pickup spends exactly one x"+kind);
                require(kind==3?ItemStack.matches(grid.getItem(0),original):grid.getItem(0).isEmpty(),"x"+kind+" preserves triple donor or consumes entire transformed donor");
                require(p.getInventory().countItem(Items.DIAMOND)==(kind==3?13:0),"x3 extra stack is natively picked up immediately; x2/x4 do not duplicate");
                grid.clearContent();menu.setCarried(ItemStack.EMPTY);p.getInventory().clearContent();
            }
            for(int kind:new int[]{2,4}){
                grid.setItem(0,new ItemStack(Items.DIAMOND,13));grid.setItem(3,tablet(kind));
                require(menu.getResultSlot().getItem().isEmpty(),"unmodifiable donor is a no-op x"+kind);
                GameChecks.inventoryClick(p,0,0,ContainerInput.PICKUP);
                require(grid.getItem(0).getCount()==13&&grid.getItem(3).getCount()==2,"no-op consumes nothing");grid.clearContent();
            }
            var original=donor(3);grid.setItem(0,original.copy());grid.setItem(3,tablet(3));
            GameChecks.inventoryClick(p,0,0,ContainerInput.QUICK_MOVE);
            require(ItemStack.matches(grid.getItem(0),original)&&grid.getItem(3).isEmpty()&&p.getInventory().countItem(Items.DIAMOND)==52,"shift crafts two tablets: original13 retained and52 exact copies delivered");
            grid.clearContent();p.getInventory().clearContent();
            var at=p.blockPosition().east();f.set(at,Blocks.CRAFTING_TABLE.defaultBlockState());p.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(f.level,at));
            require(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,"native3x3 workbench");
            var work=(net.minecraft.world.inventory.CraftingMenu)p.containerMenu;var slots=work.getInputGridSlots();
            try{
                slots.get(0).set(original.copy());slots.get(8).set(tablet(3));
                for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
                var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
                GameChecks.inventoryClick(p,0,0,ContainerInput.QUICK_MOVE);
                require(ItemStack.matches(slots.get(0).getItem(),original)&&slots.get(8).getItem().getCount()==2,"full-inventory shift craft has no debit or extra stack");
                GameChecks.inventoryClick(p,0,0,ContainerInput.PICKUP);
                var extras=new ArrayList<ItemEntity>();for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof ItemEntity item){f.owned.add(e.getUUID());extras.add(item);}
                require(ItemStack.matches(work.getCarried(),original)&&ItemStack.matches(slots.get(0).getItem(),original)&&slots.get(8).getItem().getCount()==1,"3x3 pickup preserves input and spends one tablet");
                require(extras.size()==1&&ItemStack.matches(extras.getFirst().getItem(),original)&&!extras.getFirst().hasPickUpDelay(),"full inventory leaves exactly one immediately pickable complete stack on player");
                extras.forEach(ItemEntity::discard);
            }finally{slots.forEach(s->s.set(ItemStack.EMPTY));work.setCarried(ItemStack.EMPTY);p.getInventory().clearContent();p.closeContainer();}
        }
        void katana(){
            f.move(f.other,f.owner.position().add(0,0,2));f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(LbcElectro.CYBERKATANA));
            f.other.setInvulnerableTime(0);f.other.damageCooldownTime=0;
            f.owner.attack(f.other);
            require(f.other.getEffect(MobEffects.GLOWING)!=null&&f.other.getEffect(MobEffects.GLOWING).getAmplifier()==2,"real katana attack gives GlowingIII");
            require(f.other.getEffect(MobEffects.BLINDNESS).getAmplifier()==1&&f.other.getEffect(MobEffects.WITHER).getAmplifier()==5&&f.other.getEffect(MobEffects.SLOWNESS).getAmplifier()==10,"other katana levels unchanged");
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.other.removeAllEffects();f.move(f.other,f.home.add(12,0,0));
        }
        void raid(){
            for(var level:game.server().getAllLevels())require(((me.skywars2.mixin.LobbyRaidCleanupMixin)level.getRaids()).skywars2$raids().isEmpty(),"exclusive native raid fixture");
            var raid=new net.minecraft.world.entity.raid.Raid(f.owner.blockPosition(),f.level.getDifficulty());
            var bar=(ServerBossEvent)WoolBucketChecks.read(raid,"raidEvent");var raids=((me.skywars2.mixin.LobbyRaidCleanupMixin)f.level.getRaids()).skywars2$raids();
            raids.put(0x4f013,raid);bar.addPlayer(f.owner);int before=f.client.packets().size();
            GameRaidCleanup.clear(game);boolean[] removed={false};
            for(var packet:f.client.packets().subList(before,f.client.packets().size()))if(packet instanceof ClientboundBossEventPacket event)
                event.dispatch(new ClientboundBossEventPacket.Handler(){@Override public void remove(UUID id){if(id.equals(bar.getId()))removed[0]=true;}});
            require(removed[0]&&bar.getPlayers().isEmpty()&&raids.isEmpty()&&raid.isStopped(),"native raid stop sends actual matching client REMOVE packet and clears owning saved registry");
        }
        void rays(){
            require(beams.count()==0,"exclusive finite-ray scene");var from=f.home.add(0,4,0);
            var muzzles=new ArrayList<Vec3>();
            var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(from.add(0,0,5));cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);
            require(f.level.addFreshEntity(cow),"actual ray target");f.owned.add(cow.getUUID());
            for(var kind:List.of(LbcHedgehog.Kind.SNIPER,LbcHedgehog.Kind.WALL,LbcHedgehog.Kind.DEFAULT)){
                var helper=game.items().hedgehog().spawnHelper(f.owner,f.level,from,kind);require(helper!=null,"native helper "+kind);f.owned.add(helper.getUUID());helper.setNoGravity(true);helper.setNoAi(true);
                muzzles.add(helper.getEyePosition());
                game.items().hedgehog().fireBeam(helper,cow);helper.entityTags().add("time_stopped");
                game.items().hedgehog().retireOwned(f.owner.getUUID(),true);
            }
            require(cow.getHealth()==972&&beams.count()==3,"three new visuals retain native20/4/4 damage and original target tracing");
            @SuppressWarnings("unchecked") var records=(List<Object>)WoolBucketChecks.read(beams,"beams");
            for(int i=0;i<records.size();i++){
                var display=(ItemDisplayElement)WoolBucketChecks.read(records.get(i),"display");float expected=LbcFadingBeams.LASER_WIDTH*(i==0?1.25f:1.5f);
                require(Math.abs(display.getScale().x()-expected)<.00001&&display.getScale().x()==display.getScale().y()&&display.getScale().z()>5,"full initial symmetric width and already-stretched length before first spawn");
                var id=display.getEntityId();boolean positioned=false;
                for(var packet:f.client.packets().stream().flatMap(FeedbackRoundTwoChecks::flatten).toList())if(packet instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket add&&add.getId()==id)
                    positioned=muzzles.get(i).distanceToSqr(new Vec3(add.getX(),add.getY(),add.getZ()))<.000001;
                require(positioned,"actual native spawn packet anchors ray at muzzle, accounting for client ItemDisplay Y180 rotation");
            }
        }
        void spawnerTags(){
            var cause=game.items().indirectKills().wildCause(f.owner);
            for(int trial=0;trial<32;trial++){
                var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,f.level);marker.setPos(f.home.add(0,5,0));
                marker.entityTags().add("skywars2.potion_spawner_drop");marker.entityTags().add("skywars2.potion_spawner_owner."+f.owner.getUUID());
                marker.entityTags().add("skywars2.potion_spawner_event."+UUID.randomUUID()+"."+cause.order()+"."+cause.friendlyFire()+"."+cause.projectile());
                for(int filler=0;filler<64;filler++)marker.entityTags().add("check.spawner.filler."+filler);
                game.items().markCurrentItemEpoch(marker);int before=marker.entityTags().size();f.owned.add(marker.getUUID());
                require(f.level.addFreshEntity(marker)&&!marker.isRemoved(),"actual native entity-load replay completes without concurrent tag mutation");
                require(marker.entityTags().size()>before&&game.items().spellSources().wild(marker)&&game.items().spellSources().projectileController(marker)==null
                        &&game.items().potionSpawnerDrops().owner(marker)==f.owner,"binding really adds tags, retaining WILD/statistical author without projectile control");
                marker.discard();
            }
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();require(now-began<40,"bounded native visual lifetime");
            if(phase==0){if(now-when<3)return false;frozen=beams.checkpoint();game.items().timeStop().begin(f.level,100);when=now;phase=1;return false;}
            if(phase==1){if(now-when<3)return false;require(beams.checkpoint().equals(frozen),"TimeStop pauses all display ages");game.items().timeStop().end(f.level);phase=2;}
            int age=now-began-3;
            if(age>=12&&!whiteFinished){require(beams.checkpoint().stream().allMatch(LbcFadingBeams.Image::sniper),"both white rays complete their10tick fade");whiteFinished=true;}
            if(age<24)return false;require(whiteFinished&&beams.count()==0,"all rays shrink to zero then retire, including20tick sniper");return true;
        }
        public void close(){beams.stop();game.items().timeStop().end(f.level);f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.setup();}catch(Throwable e){running=null;try{f.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS feedback-round-two (all100 source modifier draws/12 fixed commits and neutral lifecycle; native2x2/3x3 tablet previews/debits/whole stacks/extra pickup/full inventory; real katanaGlowingIII; native raid REMOVE packet; actual helper20/4/4 hits/full initial beam scales/native10/20tick fading/TimeStop)");}catch(Throwable e){try{running.close();}catch(Throwable c){e.addSuppressed(c);}running=null;SkyWars2.LOGGER.error("[check] FAIL feedback-round-two",e);}}
}
