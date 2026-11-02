package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

/** Native placement/destruction and real BLOCK loot contexts. Requires the isolated marker datapack. */
public final class LuckyBlockChecks {
    private LuckyBlockChecks() { }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Lucky blocks: "+why);}
    private static final String[] LEAVES={"lucky_block","lucky_block_neko","lucky_block_loli"};
    private static final Item[] MARKERS={Items.DIAMOND,Items.GOLD_INGOT,Items.EMERALD};
    private static java.lang.reflect.Field field(Object target,String name){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new IllegalStateException("missing native field "+name);
    }
    private static final class Score implements AutoCloseable {
        final net.minecraft.world.scores.Scoreboard board;final net.minecraft.world.scores.Objective objective;
        final ScoreHolder holder;final Integer old;
        Score(GameController game,ScoreHolder holder,String name){board=game.server().getScoreboard();this.holder=holder;objective=Objects.requireNonNull(board.getObjective(name),name);var info=board.getPlayerScoreInfo(holder,objective);old=info==null?null:info.value();}
        void set(Integer n){if(n==null)board.resetSinglePlayerScore(holder,objective);else board.getOrCreatePlayerScore(holder,objective,true).set(n);}
        public void close(){set(old);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture world;final ServerPlayer p,beta;
        final RegenerationBeltChecks.Snapshot saved,betaSaved;final Set<String> tags,betaTags;
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>(),betaStats=new LinkedHashMap<>();
        final List<Score> scores=new ArrayList<>();final List<Entity> created=new ArrayList<>();
        final Map<Object,Object> randoms=new IdentityHashMap<>();final int mode;final BlockPos pos;final boolean blockDrops;
        final Score legacy,modifier,luck;boolean closed;
        Fixture(GameController game){
            this.game=game;p=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            saved=new RegenerationBeltChecks.Snapshot(game,p);betaSaved=new RegenerationBeltChecks.Snapshot(game,beta);tags=new HashSet<>(p.entityTags());betaTags=new HashSet<>(beta.entityTags());mode=game.config().gameMode;
            world=new AllGrimoireChecks.Fixture(game);world.reset();pos=BlockPos.containing(world.at).offset(3,0,0);blockDrops=world.level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS);world.level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS,true,game.server());
            legacy=score(ScoreHolder.forNameOnly("legacy"),"lbc.math");modifier=score(ScoreHolder.forNameOnly("final_luck_modifier"),"lbc.math");luck=score(p,"luck_amount");legacy.set(0);modifier.set(1);luck.set(1);
            for(var kind:LbcLuckyBlocks.Kind.values()){
                save(net.minecraft.stats.Stats.ITEM_USED.get(LbcLuckyBlocks.item(kind)));
                save(net.minecraft.stats.Stats.BLOCK_MINED.get(LbcLuckyBlocks.block(kind)));
                var betaMined=net.minecraft.stats.Stats.BLOCK_MINED.get(LbcLuckyBlocks.block(kind));betaStats.put(betaMined,beta.getStats().getValue(betaMined));
                save(net.minecraft.stats.Stats.BLOCK_MINED.get(LbcLuckyBlocks.legacyState(kind).getBlock()));
            }
            save(net.minecraft.stats.Stats.ITEM_USED.get(Items.DIAMOND_PICKAXE));save(net.minecraft.stats.Stats.ITEM_BROKEN.get(Items.DIAMOND_PICKAXE));
            try{for(Object target:List.of(world.level,p,beta)){var f=field(target,"random");randoms.put(target,f.get(target));f.set(target,net.minecraft.util.RandomSource.create(0x6c75636bL));}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            p.entityTags().removeAll(List.of("developer","spectator","time_stopped"));p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.getAttribute(Attributes.LUCK).setBaseValue(0);
            beta.entityTags().removeAll(List.of("developer","spectator","time_stopped"));beta.setGameMode(GameType.SURVIVAL);beta.removeAllEffects();beta.getAttribute(Attributes.LUCK).setBaseValue(0);
            p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setShiftKeyDown(false);
            game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            world.level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2|16|32);world.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
        }
        void save(net.minecraft.stats.Stat<?> stat){stats.put(stat,p.getStats().getValue(stat));}
        Score score(ScoreHolder holder,String name){var s=new Score(game,holder,name);scores.add(s);return s;}
        ItemStack leaf(LbcLuckyBlocks.Kind kind){var out=game.lbc().rollTable(world.level,Vec3.atCenterOf(pos),p,"lbc:ported/"+LEAVES[kind.ordinal()]);require(out.size()==1&&out.getFirst().getCount()==1,"canonical singleton "+kind);return out.getFirst();}
        ItemStack tool(boolean silk,int fortune){var stack=new ItemStack(Items.DIAMOND_PICKAXE);var registry=p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);if(silk)stack.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH),1);if(fortune>0)stack.enchant(registry.getOrThrow(Enchantments.FORTUNE),fortune);return stack;}
        void clearDrops(){for(var e:List.copyOf(created))if(e instanceof ItemEntity)e.discard();}
        List<ItemEntity> drops(){var out=new ArrayList<ItemEntity>();for(var e:world.level.getAllEntities())if(e instanceof ItemEntity item&&!item.isRemoved()&&!world.base.before.contains(item)&&item.position().distanceToSqr(Vec3.atCenterOf(pos))<16){out.add(item);if(!created.contains(item))created.add(item);}return out;}
        List<ItemStack> breakBlock(ServerPlayer actor,BlockState state,ItemStack tool){clearDrops();world.level.setBlock(pos,state,2|16|32);actor.setItemInHand(InteractionHand.MAIN_HAND,tool);require(actor.gameMode.destroyBlock(pos),"real destroyBlock accepted "+state);require(world.level.getBlockState(pos).isAir(),"actual broken cell is air");return drops().stream().map(e->e.getItem().copy()).toList();}
        List<ItemStack> context(BlockState state,Entity actor,ItemStack tool){return context(state,actor,tool,false);}
        List<ItemStack> context(BlockState state,Entity actor,ItemStack tool,boolean explosion){
            var params=new LootParams.Builder(world.level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.TOOL,tool);
            if(actor!=null)params.withParameter(LootContextParams.THIS_ENTITY,actor);
            if(actor instanceof ServerPlayer living)params.withLuck((float)living.getAttributeValue(Attributes.LUCK));
            if(explosion)params.withParameter(LootContextParams.EXPLOSION_RADIUS,4f);
            return state.getDrops(params);
        }
        void only(List<ItemStack> stacks,Item item,int count,String why){int total=0;for(var s:stacks){require(s.is(item),why+" unexpected "+s);total+=s.getCount();}require(total==count,why+" count="+total+" expected="+count);}
        void empty(List<ItemStack> stacks,String why){require(stacks.isEmpty(),why+" got="+stacks);}
        void silk(List<ItemStack> stacks,LbcLuckyBlocks.Kind kind,boolean legacyRoll,String why){
            if(kind!=LbcLuckyBlocks.Kind.NORMAL){only(stacks,LbcLuckyBlocks.item(kind),1,why);return;}
            int blocks=0,tokens=0,legacyCount=0;
            for(var s:stacks){
                if(s.is(LbcLuckyBlocks.item(kind)))blocks+=s.getCount();
                else if(s.is(Items.LAPIS_LAZULI)&&legacyRoll)legacyCount+=s.getCount();
                else {var data=LbcFartus.data(s);require(s.is(Items.REPEATING_COMMAND_BLOCK)&&data.getIntOr("drop",0)==1&&data.getIntOr("adv_check_lbc",0)==1&&net.minecraft.resources.Identifier.parse("lbc:drop_not_trap").equals(s.get(DataComponents.ITEM_MODEL)),why+" exact source advancement token "+s);tokens+=s.getCount();}
            }
            require(blocks==1&&tokens==1&&legacyCount==(legacyRoll?1:0),why+" exact native block + advancement token + optional legacy roll");
        }
        int counter(ServerPlayer player,String name){var objective=Objects.requireNonNull(game.server().getScoreboard().getObjective(name),name);var score=game.server().getScoreboard().getPlayerScoreInfo(player,objective);return score==null?0:score.value();}
        void presentation(LbcLuckyBlocks.Kind kind){
            var s=leaf(kind);var defaults=LbcLuckyBlocks.item(kind).getDefaultInstance();require(s.is(LbcLuckyBlocks.item(kind)),"canonical native item "+kind);
            for(var component:List.of(DataComponents.ITEM_MODEL,DataComponents.CUSTOM_NAME,DataComponents.LORE))require(s.get(component)!=null&&Objects.equals(s.get(component),defaults.get(component)),"source/default component "+component+" "+kind);
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)world.base.recording;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(s,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),p.registryAccess());
                require(proxy.is(LbcLuckyBlocks.legacyState(kind).getBlock().asItem())&&Objects.equals(proxy.get(DataComponents.ITEM_MODEL),s.get(DataComponents.ITEM_MODEL)),"actual authored Polymer projection "+kind);
                require(ItemStack.matches(s,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),p.registryAccess())),"native Polymer roundtrip "+kind);
            });
        }
        void placement(){
            for(var kind:LbcLuckyBlocks.Kind.values())for(var hand:InteractionHand.values()){
                presentation(kind);clearDrops();world.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
                var stack=leaf(kind);stack.setCount(2);p.setItemInHand(hand,stack);var other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;p.setItemInHand(other,new ItemStack(Items.STICK,7));
                var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
                require(stack.useOn(new UseOnContext(p,hand,hit)).consumesAction()&&stack.getCount()==1&&p.getItemInHand(other).getCount()==7,"real native block item placement and exact used-hand debit "+kind+" "+hand);
                var state=world.level.getBlockState(pos);require(state.is(LbcLuckyBlocks.block(kind))&&LbcLuckyBlocks.kind(state)==kind,"placed actual native identity "+kind);
                require(state.getCollisionShape(world.level,pos).bounds().equals(new net.minecraft.world.phys.AABB(0,0,0,1,1,1)),"native full cube collision "+kind);
                var polymer=(eu.pb4.polymer.core.api.block.PolymerBlock)state.getBlock();require(polymer.getPolymerBlockState(state,null).is(Blocks.BARRIER),"collision proxy does not consume real mushroom states "+kind);
                var attachment=eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment.get(world.level,pos);
                require(attachment!=null&&attachment.holder().getElements().size()==1,"one Polymer-owned block visual "+kind);
                var savedState=BlockState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,state).getOrThrow();
                require(BlockState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,savedState).getOrThrow()==state,"registered block state persists without visual entities "+kind);
                world.level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),3);require(LbcLuckyBlocks.kind(world.level.getBlockState(pos))==kind,"neighbor update preserves native kind");require(eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment.get(world.level,pos)==attachment,"neighbor update reuses the single visual");world.level.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),3);
                var breaker=hand==InteractionHand.MAIN_HAND?p:beta;breaker.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);String counter=kind==LbcLuckyBlocks.Kind.NORMAL?"lucky_block_count":kind==LbcLuckyBlocks.Kind.LOLI?"lolidefecation":null;int before=counter==null?0:counter(breaker,counter);require(breaker.gameMode.destroyBlock(pos),"placed native block real break "+kind);require(attachment.isRemoved(),"native break removes Polymer visual");if(counter!=null)require(counter(breaker,counter)==before+1,"actual native mined counter increments once "+counter);var physical=drops();only(physical.stream().map(e->e.getItem()).toList(),MARKERS[kind.ordinal()],1,"placed native physical reward "+kind);
                for(var entity:physical){var source=game.items().spellSources().source(entity);require(source!=null&&source.author().equals(breaker.getUUID())&&source.cause()!=null&&source.cause().player().equals(breaker.getUUID())&&game.items().currentEpoch(source.epoch()),"actual reward cause belongs to breaker, including foreign placer "+kind);}
            }
        }
        void lootRules(){
            for(var kind:LbcLuckyBlocks.Kind.values()){
                var raw=LbcLuckyBlocks.legacyState(kind);var nativeState=LbcLuckyBlocks.block(kind).defaultBlockState();var marker=MARKERS[kind.ordinal()];
                require(LbcLuckyBlocks.kind(raw)==null,"all raw mushroom states remain vanilla "+kind);
                var ordinary=raw.setValue(BlockStateProperties.UP,true);require(LbcLuckyBlocks.kind(ordinary)==null,"one true face is ordinary "+kind);
                for(int rolls:new int[]{0,-1,1,3}){
                    luck.set(rolls);p.getAttribute(Attributes.LUCK).setBaseValue(rolls-1);
                    only(context(nativeState,p,tool(false,0)),marker,Math.max(0,rolls),"actual native BLOCK score rolls "+kind+" "+rolls);
                }
                // A nonliving native actor has no luck attribute: source refresh cannot create its missing score.
                var actor=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,world.level);actor.setPos(Vec3.atCenterOf(pos));require(world.level.addFreshEntity(actor),"native no-score actor");created.add(actor);
                require(game.server().getScoreboard().getPlayerScoreInfo(actor,luck.objective)==null,"genuine absent actor score before fallback");
                only(context(nativeState,actor,tool(false,0)),marker,1,"missing score fallback real native context "+kind);
                require(game.server().getScoreboard().getPlayerScoreInfo(actor,luck.objective)==null,"absent actor score remains absent");
                var mob=new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityTypes.ARMOR_STAND,world.level);
                mob.setPos(Vec3.atCenterOf(pos));require(world.level.addFreshEntity(mob),"native living actor without luck attribute");created.add(mob);
                require(mob.getAttribute(Attributes.LUCK)==null,"living fixture has no player luck attribute");
                only(context(nativeState,mob,ItemStack.EMPTY),marker,1,"living nonplayer source fallback native block");
                require(game.server().getScoreboard().getPlayerScoreInfo(mob,luck.objective)==null,"nonplayer source refresh never creates a player luck score");
                luck.set(null);p.getAttribute(Attributes.LUCK).setBaseValue(0);
                only(context(nativeState,null,tool(false,0)),marker,1,"no actor real native BLOCK fallback "+kind);
                only(context(nativeState,null,ItemStack.EMPTY,true),marker,1,"no-actor explosion native BLOCK source fallback "+kind);
                luck.set(3);p.getAttribute(Attributes.LUCK).setBaseValue(2);
                silk(context(nativeState,p,tool(true,0)),kind,false,"native Silk native item "+kind);
                var authored=game.lbc().rollTable(world.level,Vec3.atCenterOf(pos),p,"lbc:ported/silk_pickaxe").getFirst();
                silk(breakBlock(p,nativeState,authored),kind,false,"authored SilkX pickaxe native break "+kind);
                var highSilk=tool(false,0);highSilk.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1000);
                silk(breakBlock(p,nativeState,highSilk),kind,false,"positive Silk level1000 native break "+kind);
                String mined=kind==LbcLuckyBlocks.Kind.NORMAL?"lucky_block_count":kind==LbcLuckyBlocks.Kind.LOLI?"lolidefecation":null;int minedBefore=mined==null?0:counter(p,mined);
                only(breakBlock(p,raw,tool(true,0)),raw.getBlock().asItem(),1,"vanilla mushroom Silk returns its real block "+kind);
                if(mined!=null)require(counter(p,mined)==minedBefore,"real mushroom does not advance lucky counter "+mined);
                silk(breakBlock(p,nativeState,tool(true,0)),kind,false,"actual native Silk physical item "+kind);
                if(mined!=null)require(counter(p,mined)==minedBefore+1,"actual native mined counter increments once "+mined);
                boolean bonus=false;luck.set(1);p.getAttribute(Attributes.LUCK).setBaseValue(0);
                for(int n=0;n<24;n++){var out=context(nativeState,p,tool(false,255));int count=0;for(var s:out){require(s.is(marker)&&s.getCount()<=99,"Fortune native per-stack cap99 "+kind);count+=s.getCount();}require(count>=1&&count<=99,"one Fortune roll bounds "+kind);bonus|=count>1;}
                require(bonus,"actual ore_drops Fortune changes native reward "+kind);
                luck.set(3);p.getAttribute(Attributes.LUCK).setBaseValue(2);boolean aggregate=false;
                for(int n=0;n<8;n++){int total=0;for(var s:context(nativeState,p,tool(false,255))){require(s.is(marker)&&s.getCount()<=99,"per-generated-stack Fortune cap "+kind);total+=s.getCount();}aggregate|=total>99;}
                require(aggregate,"99 cap is per generated stack, not aggregate rolls "+kind);luck.set(1);p.getAttribute(Attributes.LUCK).setBaseValue(0);
                // Exclusion is categorical: no ordinary shape may enter the fixture marker roots.
                for(int n=0;n<8;n++)for(var s:context(ordinary,p,tool(false,0)))require(!s.is(marker)&&!s.is(LbcLuckyBlocks.item(kind)),"ordinary mushroom excluded from Lucky opening "+kind);
            }
            var normal=LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState();legacy.set(1);luck.set(3);p.getAttribute(Attributes.LUCK).setBaseValue(2);
            only(context(normal,p,tool(false,0)),Items.LAPIS_LAZULI,1,"legacy branch exactly once regardless luck");
            silk(context(normal,p,tool(true,0)),LbcLuckyBlocks.Kind.NORMAL,true,"ordinary legacy plus Silk block and advancement pools");legacy.set(0);
            luck.set(16);p.getAttribute(Attributes.LUCK).setBaseValue(15);
            for(var brown:List.of(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NEKO).defaultBlockState())){
                var high=context(brown,p,tool(false,0));only(high,Items.AMETHYST_SHARD,2,"source brown luck16 disables repeated rewards and executes both TMT pools");
                only(context(brown,p,tool(true,0)),Items.AMETHYST_SHARD,2,"brown high-luck Silk retains both source TMT pools");
            }
        }
        ItemStack flag(String flag){var stack=new ItemStack(Items.STICK);var data=new net.minecraft.nbt.CompoundTag();data.putInt(flag,1);stack.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));return stack;}
        void nekoBranches(){
            var inventory=new ArrayList<ItemStack>();for(int slot=0;slot<p.getInventory().getContainerSize();slot++)inventory.add(p.getInventory().getItem(slot).copy());
            var equipmentSlots=List.of(net.minecraft.world.entity.EquipmentSlot.MAINHAND,net.minecraft.world.entity.EquipmentSlot.OFFHAND,net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET);var equipment=new EnumMap<net.minecraft.world.entity.EquipmentSlot,ItemStack>(net.minecraft.world.entity.EquipmentSlot.class);for(var slot:equipmentSlots)equipment.put(slot,p.getItemBySlot(slot).copy());
            double oldLuck=p.getAttribute(Attributes.LUCK).getBaseValue();var previous=game.server().getScoreboard().getPlayerScoreInfo(p,luck.objective);Integer oldScore=previous==null?null:previous.value();
            try{
                p.getAttribute(Attributes.LUCK).setBaseValue(0);luck.set(1);
                for(var state:List.of(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NEKO).defaultBlockState()))for(int location=0;location<4;location++){
                    p.getInventory().clearContent();for(var slot:equipmentSlots)p.setItemSlot(slot,ItemStack.EMPTY);
                    if(location==1)p.getInventory().setItem(9,flag("zaika"));
                    if(location==2)p.setItemInHand(InteractionHand.OFF_HAND,flag("zaika"));
                    if(location==3)p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,flag("zaika"));
                    game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
                    only(context(state,p,tool(false,0)),location==0?Items.GOLD_INGOT:Items.NETHERITE_SCRAP,1,"real Neko root native entity slots branch location="+location);
                    p.setItemInHand(InteractionHand.MAIN_HAND,flag("luck_key"));
                    empty(context(state,p,tool(false,0)),"actual mainhand luck_key excludes both Neko branches location="+location);
                }
            }finally{p.getInventory().clearContent();for(int slot=0;slot<inventory.size();slot++)p.getInventory().setItem(slot,inventory.get(slot));equipment.forEach(p::setItemSlot);p.getAttribute(Attributes.LUCK).setBaseValue(oldLuck);luck.set(oldScore);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}
        }
        void policies(){
            var kind=LbcLuckyBlocks.Kind.NORMAL;var state=LbcLuckyBlocks.block(kind).defaultBlockState();p.getAttribute(Attributes.LUCK).setBaseValue(0);luck.set(1);
            p.setGameMode(GameType.CREATIVE);only(breakBlock(p,state,ItemStack.EMPTY),Items.DIAMOND,1,"ordinary Creative retains native physical drop");
            p.entityTags().add("developer");try{empty(breakBlock(p,state,ItemStack.EMPTY),"developer Creative suppresses physical drops");}finally{p.entityTags().remove("developer");p.setGameMode(GameType.SURVIVAL);}
            world.level.setBlock(pos,state,2|16|32);p.entityTags().remove("have_stand");
            game.items().timeStop().begin(world.level,40);try{require(game.items().timeStop().frozen(p),"real world stop has no fixture exemption");require(!p.gameMode.destroyBlock(pos)&&world.level.getBlockState(pos).equals(state),"world TimeStop rejects native destruction");}finally{game.items().timeStop().end(world.level);}
            require(!game.isObserver(p),"participant initially not observer");game.spectators().enter(p);try{require(game.isObserver(p),"actual observer positive control");require(!p.gameMode.destroyBlock(pos)&&world.level.getBlockState(pos).equals(state),"observer cannot break reward block");}finally{game.spectators().remove(p);p.setGameMode(GameType.SURVIVAL);}
            int configured=game.config().luckyModifier;var published=score(ScoreHolder.forNameOnly("cfg_lucky_modifier"),"swrg.math");
            try{game.config().gameMode=1;game.config().luckyModifier=12;modifier.set(6);var owner=new GameLuckyModifiers(game);require(owner.startMatch()==6&&game.server().getScoreboard().getPlayerScoreInfo(modifier.holder,modifier.objective).value()==6,"actual modifier owner does not resolve configured12 outside LuckyWars");owner.endMatch();require(game.server().getScoreboard().getPlayerScoreInfo(modifier.holder,modifier.objective).value()==6,"ordinary end retains categorical result");}finally{game.config().luckyModifier=configured;game.config().gameMode=mode;published.close();modifier.set(1);}
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{drops();for(var e:created){game.items().spellSources().unloaded(e);for(var objective:game.server().getScoreboard().getObjectives())game.server().getScoreboard().resetSinglePlayerScore(e,objective);e.discard();}},world::close,()->{for(var s:scores)s.close();},()->saved.restore(game),saved::restoreAttributes,saved::restoreUse,()->betaSaved.restore(game),betaSaved::restoreAttributes,betaSaved::restoreUse,()->{world.level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS,blockDrops,game.server());game.config().gameMode=mode;p.entityTags().clear();p.entityTags().addAll(tags);beta.entityTags().clear();beta.entityTags().addAll(betaTags);stats.forEach((stat,n)->p.getStats().setValue(p,stat,n));betaStats.forEach((stat,n)->beta.getStats().setValue(beta,stat,n));game.items().magic().inventoryChanged(p);game.items().magic().inventoryChanged(beta);game.players().save();},()->{for(var e:randoms.entrySet())try{field(e.getKey(),"random").set(e.getKey(),e.getValue());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Lucky blocks scoped restore",failure);
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.placement();}SkyWars2.LOGGER.info("[check] PASS lucky-blocks (three canonical native leaves/Polymer roundtrip, both actual placement hands/exact debit, full collision/stable proxy/neighbor identity, actual placed-block gameMode destruction/physical root rewards)");}
    public static void rules(GameController game){try(var f=new Fixture(game)){f.lootRules();f.nekoBranches();f.policies();}SkyWars2.LOGGER.info("[check] PASS lucky-block-rules (actual native BLOCK / vanilla mushroom contexts, zero/negative/positive/missing-score/no-actor, Silk physical native returns and exact advancement token, Fortune/per-stack99, ordinary mushroom exclusion, legacy plus Silk pools, Neko real inventory/offhand/armor zaika and mainhand luck_key predicates, separate vanilla/native mined counters, Creative/developer/observer/input/world TimeStop)");}
}
