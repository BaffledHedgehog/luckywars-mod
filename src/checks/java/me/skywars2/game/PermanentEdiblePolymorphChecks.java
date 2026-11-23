package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;

final class PermanentEdiblePolymorphChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Permanent edible polymorph: "+why);}
    static void start(GameController game){require(running==null,"exclusive check");running=new Scene(game);}
    static void tick(GameController game){var s=running;if(s==null||s.game!=game)return;
        try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS permanent-edible-polymorph (native consumption debit, shared100HP body, permanent real-tick lifetime, active participant/self camera, restored inventory and shared cleanup)");}
        catch(Throwable failure){running=null;try{s.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL permanent-edible-polymorph",failure);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final net.minecraft.server.level.ServerPlayer p;final GameChecks.WandThirdPlayerState saved;final int started;
        boolean closed;
        Scene(GameController game){this.game=game;p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(p!=null&&game.playing()&&game.items().polymorph().count()==0,"active participant and empty shared forms");saved=new GameChecks.WandThirdPlayerState(p,game.items().mana());started=game.server().getTickCount();
            try{p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.getInventory().clearContent();p.getFoodData().setFoodLevel(1);
                p.getInventory().setItem(12,new ItemStack(Items.DIAMOND,17));
                var food=new ItemStack(Items.APPLE,2);var data=new CompoundTag();data.putInt("eat_polymorph",1);food.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                p.setItemInHand(InteractionHand.MAIN_HAND,food);var result=food.finishUsingItem(p.level(),p);
                require(result.getCount()==1&&p.getMainHandItem().getCount()==1,"native shrink finishes before shared inventory snapshot");
            }catch(Throwable failure){close();throw failure;}}
        boolean step(){int elapsed=game.server().getTickCount()-started;if(elapsed<3)return false;
            var service=game.items().polymorph();require(service.active(p)&&service.remaining(p)==-1,"permanent form survives actual shared ticks");
            require(p.gameMode()==GameType.ADVENTURE&&!p.isSpectator()&&!game.isObserver(p)&&game.allowDamage(p)&&p.getCamera()==p,"player remains an active damageable participant, with native camera");
            require(game.items().edibleSpells().polymorphed(p)&&game.items().edibleSpells().polymorphCount()==1,"one common form, no separate edible spectator owner");
            var body=(Sheep)p.level().getEntity(service.sheepViews().getFirst().entity());
            require(body!=null&&body.getHealth()==100&&body.getMaxHealth()==100&&body.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==0,"shared100HP sheep without added knockback resistance");
            if(elapsed<8)return false;
            service.clearDebuffs(p);require(!service.active(p)&&body.isRemoved()&&p.gameMode()==GameType.SURVIVAL&&p.getCamera()==p,"shared restore removes body and restores player mode");
            require(p.getInventory().getItem(12).is(Items.DIAMOND)&&p.getInventory().getItem(12).getCount()==17&&p.getMainHandItem().getCount()==1,"restoration preserves inventory and consumed-food debit");
            return true;
        }
        public void close(){if(closed)return;closed=true;game.items().edibleSpells().forget(p);game.items().polymorph().clearDebuffs(p);saved.restore(game);}
    }
}
