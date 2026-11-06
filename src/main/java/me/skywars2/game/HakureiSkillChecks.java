package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real inventory touch/death and registered ticks; no artificial loop advances the service. */
public final class HakureiSkillChecks {
    private HakureiSkillChecks(){}
    private static Run running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Hakurei: "+why);}
    private static void near(double value,double expected,String why){require(Math.abs(value-expected)<1e-5,why+" actual="+value+" expected="+expected);}
    public static void start(GameController game){require(running==null,"exclusive staged fixture");var run=new Run(game);try{run.init();running=run;}catch(RuntimeException|Error error){run.close();throw error;}SkyWars2.LOGGER.info("[check] START hakurei-skill");}
    public static void tick(GameController game){if(running==null)return;var run=running;try{if(run.step()){running=null;run.close();SkyWars2.LOGGER.info("[check] PASS hakurei-skill (ten actual pickups/other picker, source3234 styles, one-shot large stacks, native victim items+XP/nested/same-tick/mergedXP, TimeStop and nonempty whole-coordinator rewind, bomb/time inputs)");}}catch(RuntimeException|Error error){running=null;try{run.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL hakurei-skill",error);}}
    private static final class Run implements AutoCloseable{
        final GameController game;final ChefChecks.Fixture f;final LbcTouhouCollectibles intake;final LbcHakureiSkill skill;
        final Set<UUID> before=new HashSet<>();final List<ItemEntity> touched=new ArrayList<>();
        final int ownerLives,otherLives;final Map<String,Integer> ownerScores=new HashMap<>(),otherScores=new HashMap<>();
        RegenerationBeltChecks.ScheduledClient client,audience;
        int stage,at,lifeBase,regenBase,coinBase,pending;double attackBase;UUID unrelated;ItemEntity fullPickup;boolean closed;
        Run(GameController game){this.game=game;f=new ChefChecks.Fixture(game);intake=game.items().touhouCollectibles();skill=game.items().hakureiSkill();ownerLives=game.lives(f.owner.getUUID());otherLives=game.lives(f.other.getUUID());for(String name:List.of("touhou_power","time_orbs","bomb_fragments","life_fragments")){ownerScores.put(name,f.mana.hudValue(f.owner,name));otherScores.put(name,f.mana.hudValue(f.other,name));}}
        ItemStack source(int id,int count){var rolled=game.lbc().rollTable(f.owner.level(),f.at,f.owner,"lbc:ported/touhou_collectible_"+id);require(rolled.size()==1&&LbcTouhouCollectibles.id(rolled.getFirst())==id,"exact source singleton "+id);return rolled.getFirst().copyWithCount(count);}
        void touch(int id,int count){
            var stack=source(id,count);var item=new ItemEntity(f.other.level(),f.other.getX(),f.other.getY(),f.other.getZ(),stack);item.setNoPickUpDelay();item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);require(f.other.level().addFreshEntity(item),"real pickup entity");touched.add(item);item.playerTouch(f.other);require(item.isRemoved(),"actual complete native pickup "+id);
        }
        void init(){
            require(skill.pendingCount()==0,"no unrelated accepted rewards");f.init();client=new RegenerationBeltChecks.ScheduledClient(game,f.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,f.other);
            f.owner.level().getAllEntities().forEach(e->before.add(e.getUUID()));intake.forget(f.owner);intake.forget(f.other);
            game.config().ranked=true;game.config().lbcSkills=true;game.players().get(f.owner.getUUID()).lbcSkill=28;game.players().get(f.other.getUUID()).lbcSkill=0;
            f.move(f.owner,f.at);f.move(f.other,f.at.add(8,0,0));client.acknowledge();audience.acknowledge();
            presentation();lifeBase=game.lives(f.other.getUUID());regenBase=f.mana.baseRegen(f.other);coinBase=game.players().get(f.other.getUUID()).coins;attackBase=f.other.getAttributeValue(Attributes.ATTACK_DAMAGE);f.mana.maximum(f.other,10000);f.mana.set(f.other,0);
            for(int id=1;id<=10;id++)touch(id,1);
            at=game.server().getTickCount();
        }
        void presentation(){
            String[] models={"power_item","big_power_item","full_power_item","score_item","life_item","time_orb","bomb_item_fragment","life_item_fragment","faith_item","money_item"};
            for(int id=1;id<=10;id++){var stack=source(id,1);require(stack.is(LbcTouhouItems.Kind.values()[id-1].item)&&Identifier.parse("lbc:"+models[id-1]).equals(stack.get(DataComponents.ITEM_MODEL)),"authored carrier/model "+id);require(stack.get(DataComponents.CUSTOM_NAME)!=null&&LbcFartus.data(stack).getIntOr("instantitem",0)==1,"authored translatable payload "+id);}
            try(var input=HakureiSkillChecks.class.getResourceAsStream("/data/lbc/loot_table/touhou_item.json")){
                require(input!=null,"canonical original weighted table");var entries=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
                int[] expected={800,100,3,1000,1,10,200,100,10,10,1000};int total=0;require(entries.size()==11,"all source outcomes including nested bomb");for(int n=0;n<expected.length;n++){int weight=entries.get(n).getAsJsonObject().get("weight").getAsInt();require(weight==expected[n],"source exact weight "+n);total+=weight;}require(total==3234,"source total3234");
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
        }
        long inventoryFlag(String flag){long result=0;for(int i=0;i<f.other.getInventory().getContainerSize();i++){var stack=f.other.getInventory().getItem(i);if(LbcFartus.data(stack).getIntOr(flag,0)==1)result+=stack.getCount();}return result;}
        Set<Entity> nativeDrops(){
            var result=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
            for(var entity:f.owner.level().getAllEntities())if(!entity.isRemoved()&&(entity instanceof ItemEntity||entity instanceof ExperienceOrb))result.add(entity);
            return result;
        }
        int newDrops(Set<Entity> previous){int result=0;for(var entity:nativeDrops())if(!previous.contains(entity))result++;return result;}
        void kill(LivingEntity victim){require(victim.hurtServer(f.owner.level(),victim.damageSources().playerAttack(f.owner),1000)&&victim.isDeadOrDying(),"actual credited native death");}
        void deathScopes(){
            int tick=game.server().getTickCount();
            for(int n=0;n<2;n++){
                var previous=nativeDrops();int queued=skill.pendingCount();kill(f.cow(f.at.add(2,0,n*2)));
                int actual=newDrops(previous);require(actual>0&&skill.pendingCount()-queued==4*actual,"two same-tick victims each reward only their own newly admitted bodies");
            }
            require(game.server().getTickCount()==tick,"both genuine kills occur in one server tick");
            var outer=f.cow(f.at.add(-2,0,0));var inner=f.cow(f.at.add(-2,0,2));var previous=nativeDrops();int queued=skill.pendingCount();
            game.items().combatRewards().withDeathLoot(outer,f.owner.level(),outer.damageSources().playerAttack(f.owner),()->{kill(outer);kill(inner);});
            int actual=newDrops(previous);require(actual>0&&skill.pendingCount()-queued==4*actual,"nested genuine deaths keep distinct top scopes without double capture");
            require(CoreEntityMerging.enabled(f.owner.level()),"native XP spawn-merge positive control");
            var victim=f.cow(f.at.add(0,0,12));var resident=new ExperienceOrb(f.owner.level(),victim.position(),Vec3.ZERO,101);
            require(f.owner.level().addFreshEntity(resident)&&!resident.isRemoved(),"actual resident XP body published before death");
            long xp=((CoreEntityMerging.Orb)resident).skywars2$totalExperience();previous=nativeDrops();queued=skill.pendingCount();kill(victim);
            int items=0,orbs=0;for(var entity:nativeDrops())if(!previous.contains(entity)){if(entity instanceof ItemEntity)items++;else orbs++;}
            require(!resident.isRemoved()&&((CoreEntityMerging.Orb)resident).skywars2$totalExperience()>xp&&orbs==0,"native victim XP really merges into resident without publishing another body");
            require(items>0&&skill.pendingCount()-queued==items*4,"absorbed XP keeps all experience and adds zero body rolls");
            // An inner skill18 finish publishes its copies while the outer skill28 scope is live.
            var outer2=f.cow(f.at.add(0,0,-8));var inner2=f.cow(f.at.add(2,0,-8));
            int queuedBefore=skill.pendingCount();int[] outerBodies={0};
            game.players().get(f.other.getUUID()).lbcSkill=18;
            try{
                game.items().combatRewards().withDeathLoot(outer2,f.owner.level(),outer2.damageSources().playerAttack(f.owner),()->{
                    int accepted=skill.pendingCount();var innerPrevious=nativeDrops();
                    require(inner2.hurtServer(f.other.level(),inner2.damageSources().playerAttack(f.other),1000)&&inner2.isDeadOrDying(),"actual inner skill18 death");
                    int innerItems=0;for(var entity:nativeDrops())if(!innerPrevious.contains(entity)&&entity instanceof ItemEntity)innerItems++;
                    require(innerItems>0&&innerItems%2==0&&skill.pendingCount()==accepted,"skill18 publishes original/copy pairs and never enqueues Hakurei");
                    var outerPrevious=nativeDrops();kill(outer2);outerBodies[0]=newDrops(outerPrevious);
                });
                require(outerBodies[0]>0&&skill.pendingCount()-queuedBefore==outerBodies[0]*4,"inner skill18 copy publications cannot leak into enclosing Hakurei death");
            }finally{game.players().get(f.other.getUUID()).lbcSkill=0;}
        }
        boolean step(){
            int elapsed=game.server().getTickCount()-at;require(elapsed<800,"bounded scheduled stage "+stage);
            if(game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(stage==0){
                if(elapsed<2)return false;
                require(intake.counters(f.other).equals(new LbcTouhouCollectibles.Counters(150,1,1,1)),"all ten IDs consumed by actual picker with skill0");require(game.lives(f.other.getUUID())==lifeBase+1&&f.mana.baseRegen(f.other)==regenBase+1&&game.players().get(f.other.getUUID()).coins==coinBase+2&&f.mana.get(f.other)>=100,"native lives/mana/faith/coins");
                near(f.other.getAttributeValue(Attributes.ATTACK_DAMAGE),attackBase+8,"all six power attack modifiers");near(f.other.getAttributeValue(Attributes.MAX_HEALTH),36,"four source multiplied-base health modifiers");
                f.block(f.center.offset(4,0,4),LbcMagic.MAGIC_BEACON_BLOCK);f.move(f.other,f.at.add(4,0,4));audience.acknowledge();require(game.items().magic().suppressed(f.other),"actual MAG field positive control");
                touch(6,99);touch(7,99);touch(8,99);stage=1;at=game.server().getTickCount();return false;
            }
            if(stage==1){
                if(elapsed<2)return false;
                require(intake.counters(f.other).equals(new LbcTouhouCollectibles.Counters(150,75,95,90)),"99-unit large stacks convert each threshold ONCE under source unjammed intake");require(inventoryFlag("time_stopper")==1&&inventoryFlag("bomb_item")==1&&game.lives(f.other.getUUID())==lifeBase+2,"one output each, counters retain overflow");
                if(elapsed<5)return false;require(intake.counters(f.other).bombFragments()==95,"no autonomous repeated conversion after drain");
                var held=f.other.getMainHandItem().copy();
                for(String table:List.of("bomb_item","time_stopper")){
                    f.other.setItemInHand(InteractionHand.MAIN_HAND,game.lbc().rollTable(f.other.level(),f.other.position(),f.other,"lbc:"+table).getFirst());
                    require(!f.other.gameMode.useItem(f.other,f.other.level(),f.other.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&f.other.getMainHandItem().getCount()==1,"MAG blocks "+table+" launch without consuming it");
                }
                f.other.setItemInHand(InteractionHand.MAIN_HAND,held);
                f.block(f.center.offset(4,0,4),Blocks.AIR);f.move(f.other,f.at.add(8,0,0));audience.acknowledge();
                for(int slot=0;slot<36;slot++)f.other.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
                fullPickup=new ItemEntity(f.other.level(),f.other.getX(),f.other.getY(),f.other.getZ(),source(1,1));fullPickup.setNoPickUpDelay();fullPickup.setNoGravity(true);fullPickup.setDeltaMovement(Vec3.ZERO);require(f.other.level().addFreshEntity(fullPickup),"real full-inventory pickup candidate");touched.add(fullPickup);fullPickup.playerTouch(f.other);require(!fullPickup.isRemoved()&&fullPickup.getItem().getCount()==1,"full inventory leaves actual collectible intact");stage=10;at=game.server().getTickCount();return false;
            }
            if(stage==10){if(elapsed<2)return false;require(intake.counters(f.other).bombFragments()==95&&!fullPickup.isRemoved(),"failed pickup never changes saved counters");f.other.getInventory().setItem(9,ItemStack.EMPTY);fullPickup.playerTouch(f.other);require(fullPickup.isRemoved(),"same actual entity succeeds after capacity becomes available");stage=11;at=game.server().getTickCount();return false;}
            if(stage==11){
                if(elapsed<2)return false;require(intake.counters(f.other).equals(new LbcTouhouCollectibles.Counters(150,75,90,80))&&game.lives(f.other.getUUID())==lifeBase+3,"new select converts saved bomb/life threshold once; time orbs require new orb input");
                var fresh=new ItemEntity(f.owner.level(),f.at.x+4,f.at.y,f.at.z,new ItemStack(Items.EMERALD,17));fresh.setPickUpDelay(32767);fresh.setNoGravity(true);fresh.setDeltaMovement(Vec3.ZERO);require(f.owner.level().addFreshEntity(fresh),"unrelated newly published item");unrelated=fresh.getUUID();
                deathScopes();
                var previous=nativeDrops();int queued=skill.pendingCount();kill(f.cow(f.at.add(2,0,-4)));
                int actual=newDrops(previous);
                require(actual>0&&skill.pendingCount()-queued==actual*4,"exactly four rolls per actual native item and XP entity, independent of stack/XP value");pending=skill.pendingCount();game.items().timeStop().begin(f.owner.level(),1000);stage=2;at=game.server().getTickCount();return false;
            }
            if(stage==2){
                require(skill.pendingCount()==pending,"TimeStop retains every accepted roll");if(elapsed<5)return false;require(game.vanity().requestCapture(f.owner,false),"nonempty whole-coordinator capture");stage=3;at=game.server().getTickCount();return false;
            }
            if(stage==3){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;skill.stop();intake.forget(f.other);require(skill.pendingCount()==0,"future queue actually cleared");require(game.vanity().requestRestoreForCheck(),"actual coordinator rewind request");stage=4;at=game.server().getTickCount();return false;}
            if(stage==4){
                if(game.vanity().busy())return false;require(skill.pendingCount()==pending&&intake.counters(f.other).equals(new LbcTouhouCollectibles.Counters(150,75,90,80)),"typed queue and picker counters restored");client.acknowledge();audience.acknowledge();game.vanity().stop();game.items().timeStop().end(f.owner.level());stage=5;at=game.server().getTickCount();return false;
            }
            if(stage==5){
                if(skill.pendingCount()!=0)return false;
                require(f.owner.level().getEntity(unrelated) instanceof ItemEntity item&&item.getItem().getCount()==17,"unrelated newborn not selected by reward");
                f.other.getInventory().clearContent();var bomb=game.lbc().rollTable(f.other.level(),f.other.position(),f.other,"lbc:bomb_item").getFirst();f.other.setItemInHand(InteractionHand.MAIN_HAND,bomb.copyWithCount(2));
                require(f.other.gameMode.useItem(f.other,f.other.level(),f.other.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&f.other.getMainHandItem().getCount()==1,"any picker actual bomb input debits exactly one");
                require(game.items().danmaku().bombs().stream().anyMatch(b->b.owner==f.other),"native shared fast bomb owned by actual picker");stage=6;at=game.server().getTickCount();return false;
            }
            if(stage==6){
                if(elapsed<4)return false;var clock=game.lbc().rollTable(f.other.level(),f.other.position(),f.other,"lbc:time_stopper").getFirst();f.other.setItemInHand(InteractionHand.MAIN_HAND,clock.copyWithCount(2));
                require(f.other.gameMode.useItem(f.other,f.other.level(),f.other.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&f.other.getMainHandItem().getCount()==1&&game.items().timeStop().stopped(f.other.level()),"real source clock input enters shared stop with one debit");
                require(!game.items().timeStop().frozen(f.other),"caster exemption survives consumable debit");
                require(f.other.gameMode.useItem(f.other,f.other.level(),f.other.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&f.other.getMainHandItem().isEmpty()
                        &&game.items().timeStop().remaining(f.other.level())==440,"second clock adds11seconds to same stop with one further debit");
                stage=7;at=game.server().getTickCount();return false;
            }
            if(elapsed<3)return false;require(game.items().timeStop().stopped(f.other.level())&&!game.items().timeStop().frozen(f.other),"caster remains immune on later clock ticks");return true;
        }
        @Override public void close(){if(closed)return;closed=true;game.vanity().stop();game.items().timeStop().end(f.owner.level());skill.stop();intake.forget(f.owner);intake.forget(f.other);game.items().danmaku().forget(f.other);for(var e:List.copyOf(touched))e.discard();var cleanup=new ArrayList<Entity>();for(var e:f.owner.level().getAllEntities())if(e!=null&&!before.contains(e.getUUID())&&(e instanceof ItemEntity||e instanceof ExperienceOrb)&&e.position().distanceToSqr(f.at)<40*40)cleanup.add(e);cleanup.forEach(Entity::discard);game.addRuneLives(f.owner,ownerLives-game.lives(f.owner.getUUID()));game.addRuneLives(f.other,otherLives-game.lives(f.other.getUUID()));if(client!=null)client.close();if(audience!=null)audience.close();f.close();ownerScores.forEach((name,value)->f.mana.score(f.owner,name).set(value));otherScores.forEach((name,value)->f.mana.score(f.other,name).set(value));}
    }
}
