package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native award boundary, real book/grimoire inputs and scheduled cast continuations. */
public final class CastAchievementChecks {
    private CastAchievementChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Cast achievements: "+why);
    }
    public static void run(GameController game) {
        require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;
        try {f.begin();} catch(Throwable error) {running=null;f.close();throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var f=running;
        try {
            if(!f.step())return;running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS cast-achievements (28 native staff award boundaries, owner isolation, real book/grimoire/direct blood, scheduled teleport/prism with hand swaps, native possession/kill predicates preserved)");
        } catch(Throwable error) {
            running=null;try {f.close();} catch(Throwable cleanup) {error.addSuppressed(cleanup);}
            SkyWars2.LOGGER.error("[check] FAIL cast-achievements",error);
        }
    }
    private static final class Fixture implements AutoCloseable {
        static final List<String> STAFFS=List.of("acid","badwiki","bass","blaze","blood","confuse","corruption","danmaku_staff","ender_random","evoker","exoperso","falling_stars","gaycaster","hedgehog","ice_wand","lightning","neutral","omega_flag","polymorph","prism","psycho","sauvojen_tuntija_wand","snow_wand","teleport","tesla","unstable_book","vampire","wall");
        final GameController game;
        final WoolBucketChecks.Fixture base;
        final MatchPlayerProgress progress,otherProgress;
        final Set<Long> forced=new LinkedHashSet<>();
        final List<BlockPos> wall=new ArrayList<>();
        final Vec3 origin;
        int phase,when,input=1000;
        Cow target;
        boolean closed;
        Fixture(GameController game) {
            this.game=game;base=new WoolBucketChecks.Fixture(game,false);
            progress=new MatchPlayerProgress(base.owner);otherProgress=new MatchPlayerProgress(base.other);
            origin=base.home.add(0,48,0);
        }
        AdvancementHolder advancement(String name) {
            return Objects.requireNonNull(game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/"+name)),name);
        }
        boolean done(String name) {return base.owner.getAdvancements().getOrStartProgress(advancement(name)).isDone();}
        void revoke(String name) {for(var criterion:advancement(name).value().criteria().keySet())base.owner.getAdvancements().revoke(advancement(name),criterion);}
        void begin() {
            require(game.items().prism().snapshot().size()==0&&game.items().exasperato().pendingSearches()==0,"exclusive source services");
            base.prepareParticipants();
            for(var p:List.of(base.owner,base.other))game.players().get(p.getUUID()).lbcUnlocks.clear();
            for(var name:STAFFS)revoke(name);
            var min=BlockPos.containing(origin.add(-2,0,-1));var max=BlockPos.containing(origin.add(2,0,100));
            for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++) {
                long key=ChunkPos.pack(x,z);
                if(!base.level.getForceLoadedChunks().contains(key)) {forced.add(key);base.level.setChunkForced(x,z,true);}
                base.level.getChunk(x,z);
            }
            base.move(base.owner,origin);base.move(base.other,origin.add(12,300,0));
            matrix();nativePossessionCriteria();
            base.owner.getInventory().clearContent();next(0);
        }
        void matrix() {
            try(var castOrigin=WandCastOrigin.scope(base.owner,true)) {
                for(var name:STAFFS) {
                    var holder=advancement(name);
                    for(var criterion:holder.value().criteria().keySet()) {
                        boolean awarded=base.owner.getAdvancements().award(holder,criterion);
                        boolean expected=Set.of("unstable_book","ice_wand","omega_flag").contains(name);
                        require(awarded==expected,"native direct award boundary "+name+"/"+criterion);
                    }
                }
                require(done("unstable_book"),"book's own achievement remains available");
                require(!WandCastOrigin.borrowed(base.other),"one player's borrowed cast never changes another player's origin");
                game.items().mana().award(base.other,"lbc:true_advancements/staffs/blood");
                require(base.other.getAdvancements().getOrStartProgress(advancement("blood")).isDone(),"other owner's native award unaffected");
                try(var ownOrigin=WandCastOrigin.scope(base.owner,false)) {require(!WandCastOrigin.borrowed(base.owner),"nested independent original cast");}
                require(WandCastOrigin.borrowed(base.owner),"outer cast origin restored");
            }
            require(!WandCastOrigin.borrowed(base.owner),"no lexical origin leak");
            try {LbcLibrarySpells.cast(game,base.owner,InteractionHand.MAIN_HAND,10000);throw new AssertionError("expected unsupported spell");}
            catch(IllegalArgumentException expected) {require(!WandCastOrigin.borrowed(base.owner),"exception restores original origin");}
            for(var name:STAFFS)revoke(name);
            game.players().get(base.owner.getUUID()).lbcUnlocks.clear();
        }
        ItemStack staff(String flag) {
            var stack=new ItemStack(Items.CARROT_ON_A_STICK);var tag=new net.minecraft.nbt.CompoundTag();
            tag.putInt("transformable",1);tag.putInt("staff",1);tag.putInt(flag,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
        }
        void nativePossessionCriteria() {
            // These two achievements retain their authored inventory/held-item predicates.
            try(var castOrigin=WandCastOrigin.scope(base.owner,true)) {
                var book=book(2);base.owner.setItemInHand(InteractionHand.MAIN_HAND,book);
                net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(base.owner,base.owner.getInventory(),book);
                require(!done("ice_wand"),"borrowed ice spell alone is not possession of the actual wand");
                var ice=staff("ice_wand");base.owner.getInventory().setItem(9,ice);
                net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(base.owner,base.owner.getInventory(),ice);
                require(done("ice_wand"),"real ice staff still satisfies native inventory predicate during borrowed cast");
                base.owner.getInventory().setItem(9,ItemStack.EMPTY);
                var shot=new Snowball(EntityTypes.SNOWBALL,base.level);shot.setOwner(base.owner);shot.setPos(origin.add(0,5,0));base.own(shot);
                require(base.level.addFreshEntity(shot),"real published native source");
                var damage=new DamageSource(base.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.THROWN),shot,base.owner);
                for(boolean hasStaff:List.of(false,true)) {
                    base.owner.setItemInHand(InteractionHand.MAIN_HAND,hasStaff?staff("omegaflag"):book);
                    net.minecraft.advancements.triggers.CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(base.owner,base.other,damage);
                    require(done("omega_flag")==hasStaff,"original native kill predicate requires actual held staff");
                }
                base.discard(shot);
            }
        }
        ItemStack book(int mode) {
            var result=game.lbc().rollTable(base.level,origin,base.owner,"lbc:items/unstable_spellbook__07345d0ab2");
            require(result.size()==1&&LbcChaosRewardItems.book(result.getFirst()),"canonical unstable spellbook");
            var stack=result.getFirst();var data=LbcFartus.data(stack);data.putInt("upgraded",1);data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;
        }
        void use(ItemStack stack) {
            var p=base.owner;p.stopUsingItem();p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,stack);game.items().mana().set(p,100000);
            p.connection.handleClientTickEnd(new net.minecraft.network.protocol.game.ServerboundClientTickEndPacket());
            p.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.MAIN_HAND,input++,p.getYRot(),p.getXRot()));
            require(!WandCastOrigin.borrowed(p),"native input returns with clean origin");
        }
        void blood(ItemStack stack) {
            base.owner.removeAllEffects();base.owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,100,4));use(stack);
            require(base.owner.hasEffect(MobEffects.WITHER),"actual blood payload executed");
        }
        void next(int value) {phase=value;when=game.server().getTickCount();}
        void ray(boolean borrowed) {
            base.owner.removeAllEffects();base.move(base.owner,origin);base.owner.setYRot(0);base.owner.setXRot(0);
            if(wall.isEmpty())for(var p:BlockPos.betweenClosed(BlockPos.containing(origin.add(-1,0,96)),BlockPos.containing(origin.add(1,3,96)))) {wall.add(p.immutable());base.set(p,Blocks.STONE.defaultBlockState());}
            use(borrowed?book(10):new ItemStack(LbcWands.ADVANCED_ENDER));
            require(game.items().exasperato().pendingSearches()>0,"real ray queued for later server tick");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,borrowed?new ItemStack(LbcWands.ADVANCED_ENDER):book(10));
        }
        void prism(boolean borrowed) {
            base.owner.removeAllEffects();base.move(base.owner,origin);base.owner.setYRot(0);base.owner.setXRot(0);
            base.set(BlockPos.containing(base.owner.getEyePosition().add(0,0,1)),Blocks.STAINED_GLASS.pick(DyeColor.RED).defaultBlockState());
            target=new Cow(EntityTypes.COW,base.level);target.setNoAi(true);target.setNoGravity(true);target.setPos(origin.add(0,1,3));target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);base.own(target);require(base.level.addFreshEntity(target),"native stable prism target");
            use(borrowed?book(14):new ItemStack(LbcWands.PRISM));
            require(game.items().prism().snapshot().size()>0,"native prism released");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,borrowed?new ItemStack(LbcWands.PRISM):book(14));
        }
        boolean step() {
            base.client.acknowledge();base.audience.acknowledge();
            require(game.server().getTickCount()-when<100,"native continuation deadline phase="+phase);
            switch(phase) {
                case 0 -> {blood(book(2));require(!done("blood"),"unstable book cannot unlock foreign blood staff");next(1);}
                case 1 -> {var kind=LbcLibrarySpells.definitions().stream().filter(d->d.spellId()==25).findFirst().orElseThrow();var stack=new ItemStack(kind.item());blood(stack);require(stack.isEmpty()&&!done("blood"),"consumed grimoire cannot unlock foreign blood staff");next(2);}
                case 2 -> {blood(new ItemStack(LbcWands.BLOOD));require(done("blood"),"original staff still earns actual achievement");next(3);}
                case 3 -> {ray(true);next(4);}
                case 4 -> {if(game.items().exasperato().pendingSearches()>0)return false;require(base.owner.position().distanceTo(origin)>90&&!done("teleport"),"borrowed deferred teleport cannot become eligible by hand swap");ray(false);next(5);}
                case 5 -> {if(game.items().exasperato().pendingSearches()>0)return false;require(base.owner.position().distanceTo(origin)>90&&done("teleport"),"original deferred teleport remains eligible after hand swap");for(var p:wall)base.set(p,Blocks.AIR.defaultBlockState());prism(true);next(6);}
                case 6 -> {if(target.getHealth()==1000)return false;require(!done("prism"),"borrowed native stable prism impact does not award after hand swap");game.items().prism().stop();base.discard(target);prism(false);next(7);}
                case 7 -> {if(target.getHealth()==1000)return false;require(done("prism"),"original native prism still awards with book now held");return true;}
                default -> throw new IllegalStateException("unknown phase");
            }
            return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;
            try {game.items().prism().stop();game.items().exasperato().stop();progress.restore(base.owner);otherProgress.restore(base.other);}
            finally {try {base.close();}finally {for(long key:forced)base.level.setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false);}}
        }
    }
}
