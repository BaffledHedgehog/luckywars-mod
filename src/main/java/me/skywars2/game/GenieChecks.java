package me.skywars2.game;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.genie.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import static me.skywars2.game.genie.GenieBlueprint.*;

/** Loopback model plus real dialog packets, native inventory transactions, melee and reset. */
public final class GenieChecks {
    private static Fixture running;
    private GenieChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Genie: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.begin();}catch(Throwable error){running=null;f.close();throw error;}}
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS genie (real HTTP, dialog nonce, overstack lamp conservation, movable pending lamp, Q/cursor/chest/frame protection, exact reward slot, native lethal hit, denial10 distinct pockets, reset cancellation/late reply, TimeStop-safe HTTP completion, real native-arrow control and snowball/fireball8HP impacts, native consumed/broken source identity)");}
        catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL genie",error);}
    }
    private record Request(String body,CompletableFuture<String> reply){}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture base;final GenieService service;
        final HttpServer http;final ExecutorService executor;final BlockingQueue<Request> requests=new LinkedBlockingQueue<>();
        final GenieModelClient previous,model;final java.lang.reflect.Field modelField;
        final net.minecraft.world.SimpleContainer chest=new net.minecraft.world.SimpleContainer(27);
        int phase,when,shotIndex,inputSequence=300;float health;ItemStack edible;net.minecraft.world.entity.LivingEntity target,referenceTarget;Request request;ItemStack[] before;ItemStack waiting;boolean closed;
        Fixture(GameController game){
            this.game=game;base=new WoolBucketChecks.Fixture(game,false);base.prepareParticipants();service=game.items().genie();
            require(((Map<?,?>)WoolBucketChecks.read(service,"wishes")).isEmpty(),"no user wishes in fixture");
            try{
                http=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
                executor=Executors.newCachedThreadPool(r->{var t=new Thread(r,"skywars2-genie-native-model");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});http.setExecutor(executor);
                http.createContext("/",exchange->{try{
                    var req=new Request(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8),new CompletableFuture<>());requests.add(req);
                    var reply=req.reply.get(20,TimeUnit.SECONDS);var envelope=new JsonObject();var message=new JsonObject();message.addProperty("content",reply);var choice=new JsonObject();choice.add("message",message);var choices=new JsonArray();choices.add(choice);envelope.add("choices",choices);
                    var bytes=envelope.toString().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
                }catch(Exception ignored){}finally{exchange.close();}});http.start();
                model=new GenieModelClient(new GenieModelClient.Options(URI.create("http://127.0.0.1:"+http.getAddress().getPort()+"/"),"native-fixture",30),"fixture-key");
                modelField=GenieService.class.getDeclaredField("model");modelField.setAccessible(true);previous=(GenieModelClient)modelField.get(service);modelField.set(service,model);
            }catch(Exception error){base.close();throw new IllegalStateException(error);}
        }
        void next(int p){phase=p;when=game.server().getTickCount();}
        boolean pending(){return !((Map<?,?>)WoolBucketChecks.read(service,"wishes")).isEmpty();}
        static GenieBlueprint plan(){return new GenieBlueprint("A generous sword","Heals the wielder on every accepted sword hit.","iron_sword",1,
                List.of(new Rule(Trigger.HIT,Target.SELF,Condition.ALWAYS,Effect.HEAL,"",4,0,0,5,1)),List.of(),new Twist(TwistKind.HUNGER_PRICE,1));}
        void submit(){
            base.owner.closeContainer();base.client.acknowledge();base.owner.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());
            base.owner.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.MAIN_HAND,inputSequence++,base.owner.getYRot(),base.owner.getXRot()));
            var dialogs=(Map<?,?>)WoolBucketChecks.read(service,"dialogs");var dialog=dialogs.get(base.owner.getUUID());require(dialog!=null,"authenticated dialog issued");String token=(String)WoolBucketChecks.read(dialog,"token");
            var tag=new CompoundTag();tag.putString("wish","A sword that heals me whenever I hit someone.");
            base.owner.connection.handleCustomClickAction(new ServerboundCustomClickActionPacket(Identifier.parse("skywars2:genie/"+token),Optional.of(tag)));
            require(pending(),"real dialog packet starts request");waiting=base.owner.getMainHandItem();require(service.locked(base.owner,waiting),"pending token is owned by this player");
            base.owner.connection.handleCustomClickAction(new ServerboundCustomClickActionPacket(Identifier.parse("skywars2:genie/"+token),Optional.of(tag)));
            require(((Map<?,?>)WoolBucketChecks.read(service,"wishes")).size()==1,"same dialog token cannot be replayed");
        }
        void click(int slot,ContainerInput input){var menu=base.owner.containerMenu;base.owner.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),(short)slot,(byte)0,input,it.unimi.dsi.fastutil.ints.Int2ObjectMaps.emptyMap(),HashedStack.EMPTY));}
        int ownSlot(int index){return base.owner.containerMenu.findSlot(base.owner.getInventory(),index).orElseThrow();}
        void begin(){
            var source=plan();var item=GenieRuntime.construct(source);require(item.is(Items.IRON_SWORD)&&item.has(DataComponents.MAX_DAMAGE)&&item.has(DataComponents.TOOL)&&item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE),"native sword tool, durability and glint retained");
            var ordinary=new ItemStack(Items.IRON_SWORD);require(item.get(DataComponents.MAX_DAMAGE).equals(ordinary.get(DataComponents.MAX_DAMAGE)),"constructor does not rewrite native durability");
            var invalid=new GenieBlueprint("Bad","Bad","not_a_real_item",1,source.rules(),List.of(),source.twist());try{GenieRuntime.construct(invalid);throw new IllegalStateException("unknown native icon accepted");}catch(IllegalArgumentException expected){}
            base.owner.getInventory().clearContent();base.owner.getInventory().setSelectedSlot(0);base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(GenieItems.LAMP,3));submit();
            require(waiting.getCount()==1&&base.owner.getInventory().getItem(1).is(GenieItems.LAMP)&&base.owner.getInventory().getItem(1).getCount()==2,"reserve one lamp without replacing sibling overstack");
            base.owner.drop(false);require(base.owner.getMainHandItem()==waiting&&waiting.getCount()==1,"actual Q cannot extract pending lamp");
            var frame=EntityTypes.ITEM_FRAME.create(base.level,EntitySpawnReason.TRIGGERED);Objects.requireNonNull(frame);frame.setPos(base.owner.position().add(0,1,1));require(base.level.addFreshEntity(frame),"native item frame");base.own(frame);
            base.owner.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());
            base.owner.connection.handleInteract(new net.minecraft.network.protocol.game.ServerboundInteractPacket(frame.getId(),InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO,false));
            require(frame.getItem().isEmpty()&&base.owner.getMainHandItem()==waiting,"actual frame packet cannot take lamp");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));base.owner.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());
            base.owner.connection.handleInteract(new net.minecraft.network.protocol.game.ServerboundInteractPacket(frame.getId(),InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO,false));
            require(frame.getItem().is(Items.APPLE),"same native frame packet accepts ordinary-item positive control");frame.setItem(ItemStack.EMPTY);base.discard(frame);base.owner.setItemInHand(InteractionHand.MAIN_HAND,waiting);
            var mode=base.owner.gameMode.getGameModeForPlayer();base.owner.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            base.owner.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(ownSlot(0),ItemStack.EMPTY));require(service.locked(base.owner,base.owner.getMainHandItem()),"creative slot deletion denied");
            base.owner.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(ownSlot(2),waiting.copy()));require(service.locked(base.owner,base.owner.getInventory().getItem(2))&&base.owner.getInventory().getItem(0).isEmpty(),"creative move relocates real lamp without duplication");
            base.owner.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(-1,waiting.copy()));require(service.locked(base.owner,base.owner.getInventory().getItem(2)),"creative Q packet denied");
            base.owner.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(ownSlot(0),waiting.copy()));base.owner.setGameMode(mode);base.client.acknowledge();require(service.locked(base.owner,base.owner.getMainHandItem()),"creative move returns lamp and ordinary mode remains playable");
            base.owner.openMenu(new SimpleMenuProvider((id,inventory,p)->ChestMenu.threeRows(id,inventory,chest),net.minecraft.network.chat.Component.literal("Genie fixture")));base.client.acknowledge();
            click(ownSlot(0),ContainerInput.QUICK_MOVE);require(chest.isEmpty()&&service.locked(base.owner,base.owner.getInventory().getItem(0)),"actual Shift-click cannot move pending lamp to chest");
            click(ownSlot(0),ContainerInput.PICKUP);require(service.locked(base.owner,base.owner.containerMenu.getCarried()),"pending lamp can move to own cursor");
            click(-999,ContainerInput.PICKUP);require(service.locked(base.owner,base.owner.containerMenu.getCarried()),"outside cursor throw denied");
            click(0,ContainerInput.PICKUP);require(chest.isEmpty()&&service.locked(base.owner,base.owner.containerMenu.getCarried()),"foreign slot deposit denied");
            click(ownSlot(10),ContainerInput.PICKUP);require(service.locked(base.owner,base.owner.getInventory().getItem(10))&&base.owner.containerMenu.getCarried().isEmpty(),"internal native move allowed");
            click(ownSlot(10),ContainerInput.PICKUP);require(service.locked(base.owner,base.owner.containerMenu.getCarried()),"pending cursor before full inventory close");
            for(int i=0;i<36;i++)if(i!=1)base.owner.getInventory().setItem(i,new ItemStack(Items.APPLE,i+1));
            var existing=base.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,base.owner.getBoundingBox().inflate(3)).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            base.owner.closeContainer();base.client.acknowledge();
            require(service.locked(base.owner,base.owner.getMainHandItem())&&base.owner.containerMenu.getCarried().isEmpty(),"real full-inventory menu close recovers lamp into inventory");
            for(var drop:base.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,base.owner.getBoundingBox().inflate(3)))if(!existing.contains(drop.getUUID()))base.own(drop);
            click(ownSlot(0),ContainerInput.PICKUP);click(ownSlot(10),ContainerInput.PICKUP);click(ownSlot(0),ContainerInput.PICKUP);
            require(service.locked(base.owner,base.owner.getInventory().getItem(10)),"recovered lamp still moves internally");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,17));next(0);
        }
        boolean step(){
            require(game.server().getTickCount()-base.started<1200,"bounded native HTTP/inventory scenario");
            if(phase==0){request=requests.poll();if(request==null)return false;var root=JsonParser.parseString(request.body).getAsJsonObject();require(root.getAsJsonArray("messages").size()==2,"fresh model context");base.pause();service.tick();require(pending(),"temporary TimeStop cannot cancel an accepted wish");request.reply.complete(new Gson().toJson(new Reply(true,"Your sword is ready. A hearty meal may help.","The wish did not forbid hunger.",plan())));next(1);return false;}
            if(phase==1){
                if(pending())return false;
                require(game.items().timeStop().stopped(base.level),"HTTP completion remains valid while world is stopped");base.thaw();
                var reward=base.owner.getInventory().getItem(10);require(GenieRuntime.matches(reward)&&base.owner.getMainHandItem().is(Items.APPLE)&&base.owner.getMainHandItem().getCount()==17,"completion replaces correct moved lamp, not selected slot");
                require(base.owner.getInventory().getItem(1).getCount()==2&&base.owner.getInventory().getItem(1).is(GenieItems.LAMP),"unselected sibling lamps unchanged");
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,reward);base.owner.getInventory().setItem(10,ItemStack.EMPTY);base.owner.setHealth(base.owner.getMaxHealth()-20);base.owner.getFoodData().setFoodLevel(20);
                next(7);return false;
            }
            if(phase==7){
                if(game.server().getTickCount()-when<20)return false;
                require(base.owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)>=6,"native selected sword modifiers have updated before lethal attack");
                var cow=EntityTypes.COW.create(base.level,EntitySpawnReason.TRIGGERED);Objects.requireNonNull(cow);cow.setPos(base.owner.position().add(0,0,1));cow.setHealth(1);require(base.level.addFreshEntity(cow),"real lethal target");base.own(cow);
                float hp=base.owner.getHealth();int food=base.owner.getFoodData().getFoodLevel();base.owner.attack(cow);
                require(!cow.isAlive()&&base.owner.getHealth()==hp+4&&base.owner.getFoodData().getFoodLevel()==18,"real lethal melee dispatch heals and pays drawback exactly once: targetHP="+cow.getHealth()+" owner="+hp+"->"+base.owner.getHealth()+" food="+food+"->"+base.owner.getFoodData().getFoodLevel());
                base.owner.getInventory().clearContent();for(int i=0;i<36;i++)base.owner.getInventory().setItem(i,new ItemStack(Items.APPLE,1+i));base.owner.getInventory().setItem(1,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(GenieItems.LAMP));submit();
                before=new ItemStack[41];for(int i=0;i<before.length;i++)before[i]=base.owner.getInventory().getItem(i).copy();before[0]=ItemStack.EMPTY;next(2);return false;
            }
            if(phase==2){request=requests.poll();if(request==null)return false;request.reply.complete("{\"granted\":false,\"answer\":\"No.\",\"loophole\":\"\",\"item\":null}");next(3);return false;}
            if(phase==3){
                if(pending())return false;int changed=0;for(int i=0;i<before.length;i++)if(!ItemStack.matches(before[i],base.owner.getInventory().getItem(i)))changed++;require(changed==10,"denial replaces exactly ten distinct slots after consuming lamp, actual="+changed);
                base.owner.getInventory().clearContent();base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(GenieItems.LAMP));submit();next(4);return false;
            }
            if(phase==4){request=requests.poll();if(request==null)return false;game.items().clear(base.owner);require(!pending()&&!ItemData.contains(waiting,GenieService.TOKEN)&&waiting.get(DataComponents.ITEM_MODEL).equals(GenieItems.IDLE),"actual reset cancels request and unlocks lamp");request.reply.complete(new Gson().toJson(new Reply(true,"Done.","The wish omitted a cost.",plan())));next(5);return false;}
            if(phase==5){if(game.server().getTickCount()-when<8)return false;require(base.owner.getMainHandItem().is(GenieItems.LAMP)&&!GenieRuntime.matches(base.owner.getMainHandItem()),"late cancelled reply cannot replace item after reset");shoot();next(6);return false;}
            if(phase==6){
                if(target.getHealth()==1000){require(game.server().getTickCount()-when<30,"native projectile reaches target: kind="+List.of("arrow","snowball","small_fireball").get(shotIndex));return false;}
                if(shotIndex==0){
                    if(referenceTarget.getHealth()==1000){require(game.server().getTickCount()-when<30,"matching native reference arrow reaches target");return false;}
                    require(target.getHealth()==referenceTarget.getHealth(),"tagged arrow retains native speed/drag/gravity/rounding damage: genie="+target.getHealth()+" reference="+referenceTarget.getHealth());
                    base.discard(referenceTarget);referenceTarget=null;
                }else require(target.getHealth()==992,"real native tagged projectile applies8 direct damage: kind="+List.of("arrow","snowball","small_fireball").get(shotIndex)+" HP="+target.getHealth());
                base.discard(target);
                if(++shotIndex<3){shoot();next(6);return false;}
                require(requests.isEmpty(),"dialog replay made no extra model request");
                var old=base.owner.position();base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                for(var cell:net.minecraft.core.BlockPos.betweenClosed(base.owner.blockPosition().offset(-1,0,0),base.owner.blockPosition().offset(1,2,4)))base.set(cell.immutable(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                var plan=new GenieBlueprint("A short teleport","Moves three blocks forward.","stick",1,List.of(new Rule(Trigger.USE,Target.SELF,Condition.ALWAYS,Effect.TELEPORT,"",0,3,0,20,1)),List.of(),new Twist(TwistKind.HUNGER_PRICE,1));
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,GenieRuntime.construct(plan));LbcTmtTriggers.input(base.owner,LbcTmtTriggers.Input.RIGHT_CLICK);base.client.acknowledge();
                require(base.owner.position().distanceTo(old)>2.5&&base.owner.position().distanceTo(old)<=3.0001,"bounded native teleport works when aiming into open air");
                var push=new GenieBlueprint("A push","Pushes forward.","stick",1,List.of(new Rule(Trigger.USE,Target.SELF,Condition.ALWAYS,Effect.PUSH,"",1,0,0,20,1)),List.of(),new Twist(TwistKind.HUNGER_PRICE,1));
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,GenieRuntime.construct(push));LbcTmtTriggers.input(base.owner,LbcTmtTriggers.Input.RIGHT_CLICK);require(base.owner.getDeltaMovement().z>.9,"self push follows camera rather than zero owner-to-owner vector");
                base.owner.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);base.owner.stopUsingItem();base.owner.getInventory().clearContent();base.owner.setHealth(base.owner.getMaxHealth()-20);health=base.owner.getHealth();base.owner.getFoodData().setFoodLevel(10);
                edible=GenieRuntime.construct(new GenieBlueprint("A restorative apple","Heals on eating this apple.","apple",1,List.of(new Rule(Trigger.EAT,Target.SELF,Condition.ALWAYS,Effect.HEAL,"",4,0,0,5,1)),List.of(),new Twist(TwistKind.HUNGER_PRICE,1)));
                base.owner.setItemInHand(InteractionHand.OFF_HAND,edible);base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));useFood();next(8);return false;
            }
            if(phase==8){
                if(!base.owner.getMainHandItem().isEmpty()){require(game.server().getTickCount()-when<45,"ordinary native food completes");return false;}
                require(base.owner.getHealth()==health&&edible.getCount()==1&&base.owner.getFoodData().getFoodLevel()==14,"eating ordinary food does not trigger an uneaten offhand genie apple");
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,edible);base.owner.setItemInHand(InteractionHand.OFF_HAND,GenieRuntime.construct(new GenieBlueprint("Another apple","Heals on eating this apple.","apple",1,List.of(new Rule(Trigger.EAT,Target.SELF,Condition.ALWAYS,Effect.HEAL,"",4,0,0,5,1)),List.of(),new Twist(TwistKind.HUNGER_PRICE,1))));
                base.owner.getFoodData().setFoodLevel(10);useFood();next(9);return false;
            }
            if(phase==9){
                if(!base.owner.getMainHandItem().isEmpty()){require(game.server().getTickCount()-when<45,"last native genie apple completes");return false;}
                require(base.owner.getHealth()==health+4&&base.owner.getFoodData().getFoodLevel()==12&&base.owner.getOffhandItem().getCount()==1,"last consumed genie item triggers exactly once; untouched offhand source does not trigger");
                var mining=new GenieBlueprint("A restorative pickaxe","Heals on breaking blocks with this pickaxe.","wooden_pickaxe",1,List.of(new Rule(Trigger.MINING,Target.SELF,Condition.ALWAYS,Effect.HEAL,"",4,0,0,5,1)),List.of(),new Twist(TwistKind.HUNGER_PRICE,1));
                var tool=GenieRuntime.construct(mining);tool.setDamageValue(tool.getMaxDamage()-1);base.owner.setItemInHand(InteractionHand.MAIN_HAND,tool);var other=GenieRuntime.construct(mining);base.owner.setItemInHand(InteractionHand.OFF_HAND,other);
                var block=base.owner.blockPosition().offset(2,0,0);base.set(block,net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());float before=base.owner.getHealth();
                require(base.owner.gameMode.destroyBlock(block),"real native root block breaks");
                require(base.level.getBlockState(block).isAir()&&base.owner.getMainHandItem().isEmpty()&&other.getCount()==1&&other.getDamageValue()==0&&base.owner.getHealth()==before+4,"final durability native break uses captured source exactly once, not offhand tool");
                var repair=new GenieBlueprint("A repairing bow","Repairs its physical bow after shooting.","bow",1,List.of(new Rule(Trigger.SHOT,Target.SELF,Condition.ALWAYS,Effect.REPAIR,"",2,0,0,20,1)),List.of(),new Twist(TwistKind.BRITTLE,1));
                var actual=GenieRuntime.construct(repair);actual.setDamageValue(100);var snapshot=actual.copy();snapshot.setDamageValue(50);var duplicate=actual.copy();base.owner.setItemInHand(InteractionHand.MAIN_HAND,actual);base.owner.setItemInHand(InteractionHand.OFF_HAND,duplicate);
                service.runtime().event(base.owner,Trigger.SHOT,null,snapshot,InteractionHand.MAIN_HAND);
                require(actual.getDamageValue()==98+actual.getMaxDamage()/10&&snapshot.getDamageValue()==50&&duplicate.getDamageValue()==100,"snapshot unchanged; repair/brittle mutate real used bow, not identical offhand clone");
                actual=GenieRuntime.construct(repair);actual.setDamageValue(100);snapshot=actual.copy();base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,actual);
                service.runtime().event(base.owner,Trigger.SHOT,null,snapshot,InteractionHand.MAIN_HAND);require(actual.getDamageValue()==100,"missing physical source cannot mutate offhand clone with same serial");return true;
            }
            throw new IllegalStateException("unknown genie phase");
        }
        void useFood(){
            base.owner.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());base.owner.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.MAIN_HAND,inputSequence++,base.owner.getYRot(),base.owner.getXRot()));
            require(base.owner.isUsingItem(),"real native food input starts without moving inventory");
        }
        void shoot(){
            String kind=List.of("arrow","snowball","small_fireball").get(shotIndex);
            var plan=new GenieBlueprint("A projectile staff","A native projectile.","stick",1,List.of(new Rule(Trigger.USE,Target.SELF,Condition.ALWAYS,Effect.PROJECTILE,kind,8,0,60,20,1)),List.of(),new Twist(TwistKind.HUNGER_PRICE,1));
            base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setItemInHand(InteractionHand.MAIN_HAND,GenieRuntime.construct(plan));
            var cow=EntityTypes.COW.create(base.level,EntitySpawnReason.TRIGGERED);Objects.requireNonNull(cow);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(base.owner.position().add(0,.5,4));cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(base.level.addFreshEntity(cow),"native projectile target");base.own(cow);target=cow;
            var before=base.level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,base.owner.getBoundingBox().inflate(5)).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            LbcTmtTriggers.input(base.owner,LbcTmtTriggers.Input.RIGHT_CLICK);
            var added=base.level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,base.owner.getBoundingBox().inflate(5),e->!before.contains(e.getUUID()));
            require(added.size()==1&&added.getFirst().getOwner()==base.owner,"one real native physically owned "+kind);added.forEach(base::own);
            if(shotIndex==0){
                // A native arrow's ceil(speed*baseDamage) can legitimately be9
                // after bow spread. Compare a real ordinary arrow, not a flat8.
                var offset=new net.minecraft.world.phys.Vec3(8,0,0);
                for(var cell:net.minecraft.core.BlockPos.betweenClosed(base.owner.blockPosition().offset(7,0,0),base.owner.blockPosition().offset(9,3,6)))
                    base.set(cell.immutable(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                var control=EntityTypes.COW.create(base.level,EntitySpawnReason.TRIGGERED);Objects.requireNonNull(control);
                control.setNoAi(true);control.setNoGravity(true);control.setPos(cow.position().add(offset));
                control.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);control.setHealth(1000);
                require(base.level.addFreshEntity(control),"ordinary native arrow control target");base.own(control);referenceTarget=control;
                var original=(net.minecraft.world.entity.projectile.arrow.Arrow)added.getFirst();
                require(((Number)WoolBucketChecks.read(original,"baseDamage")).doubleValue()==4,"requested arrow amount8 retains native speed2 base damage4");
                var reference=new net.minecraft.world.entity.projectile.arrow.Arrow(base.level,base.owner,new ItemStack(Items.ARROW),base.owner.getMainHandItem().copyWithCount(1));
                reference.setPos(original.position().add(offset));reference.setDeltaMovement(original.getDeltaMovement());
                reference.setNoGravity(original.isNoGravity());reference.setBaseDamage(4);
                reference.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;
                require(base.level.addFreshEntity(reference),"ordinary native arrow with exact initial motion");base.own(reference);
            }
        }
        @Override public void close(){if(closed)return;closed=true;service.cancelAll();model.close();http.stop(0);executor.shutdownNow();try{modelField.set(service,previous);}catch(IllegalAccessException error){throw new IllegalStateException(error);}finally{base.close();}}
    }
}
