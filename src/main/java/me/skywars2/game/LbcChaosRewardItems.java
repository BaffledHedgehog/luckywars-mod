package me.skywars2.game;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Raw Chaos reward hand consumers; existing spell factories own all released continuations. */
public final class LbcChaosRewardItems {
    private LbcChaosRewardItems(){}
    private static final String TARGET="skywars2.whip_target";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String[] TWITCH={"minecraft:lbcsounds.icum","minecraft:lbcsounds.yamai_desu","minecraft:lbcsounds.yamai_desu_2","minecraft:lbcsounds.yamai_desu_3","minecraft:lbcsounds.ooh","minecraft:lbcsounds.nyah"};
    private static final String[] MODES={"bass_wand","blood_wand","corruption_wand","ender_wand","evoker_wand","confuse_wand","vampire_wand","destroyer_spell","wall_wand","advanced_ender_wand","blaze_wand","falling_stars_wand","lightning_bolt_spell","unstable_prism","creeper_army_spell","tnt_fountain_spell","skeleton_army_spell","wither_spell","heal_spell","lava_spell","firestorm_spell","stone_rain_spell","sheep_wand","hedgehog_wand","psycho_wand","lightning_wand","acid_wand","neutral_wand","lightning_bolt_wand","sauvojen_wand_acid","sauvojen_wand_air","sauvojen_wand_blind","sauvojen_wand_electro","sauvojen_wand_fire","sauvojen_wand_ruby","sauvojen_wand_stone","sauvojen_wand_teleport","sauvojen_wand_terra","megumin_wand","acidacid_wand","acidcorrupt_wand","dublicating_wand","boltbolt_wand","consuming_wand","erecto_wand","exahoming_wand","exasperato_wand","fireblaze_wand","psychoblind_wand","stonekivi_wand","gaycaster","ice_wand","danmaku_wand","big_level_magic","sauvojen_wand"};
    public static void prepare(LbcMana mana){mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("unstable_spellbook_mana_use"),"lbc.math").set(2500);}
    public static boolean book(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("unstable_spellbook",0)==1;}
    public static boolean whip(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("anal_whip",0)==1;}
    public static boolean matches(ItemStack stack){return book(stack)||whip(stack);}
    private static boolean active(GameController g,ServerPlayer p,InteractionHand hand){
        return g.playing()&&g.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
                &&p.level()==g.maps().level()&&LbcDimensionItems.ordinary(p)&&g.canUseGameplayItems(p)&&!g.isObserver(p)
                &&g.items().wands().validRoleTarget(p,false)&&!g.items().actionsBlocked(p)
                &&(hand!=InteractionHand.OFF_HAND||g.config().offhandWands)&&g.server().tickRateManager().runsNormally()
                &&!g.items().wands().inputTimeStopped(p)&&!g.items().wands().timeStopped()
                &&!g.items().timeStop().frozen(p)&&!g.items().timeStop().stopped(p.level())&&!p.entityTags().contains("time_stopped");
    }
    public static boolean use(GameController g,ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!matches(stack)||!active(g,p,hand))return false;
        if(g.items().magic().suppressed(p)||g.items().magic().suppressedAt(p.level(),p.getEyePosition()))return false;
        // User clarification: whip teleport is caster-MAG gated; its destination is not forced or damaged.
        if(whip(stack))return teleport(g,p,stack);
        var data=LbcFartus.data(stack);
        int cost=Math.max(0,g.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("unstable_spellbook_mana_use"),"lbc.math").get());
        if(data.getIntOr("upgraded",0)==1&&p.isShiftKeyDown()){
            if(!g.items().mana().trySpendWand(p,cost,0))return false;
            int mode=data.getIntOr("mode",1);
            // Original up/down predicates overlap at pitch zero; preserve their sequential wrap.
            if(p.getXRot()<=0){mode++;if(mode>=56)mode=1;}
            if(p.getXRot()>=0){mode--;if(mode<=0)mode=54;}
            data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            if(mode>=1&&mode<=MODES.length)p.sendSystemMessage(Component.translatable(MODES[mode-1]));
            p.getInventory().setChanged();p.containerMenu.broadcastChanges();return true;
        }
        if(!g.items().mana().trySpendWand(p,cost))return false;
        int roll=data.getIntOr("upgraded",0)==1?data.getIntOr("mode",1):p.getRandom().nextInt(55)+1;
        cast(g,p,hand,roll);
        if(data.getIntOr("upgradable",0)==1&&data.getIntOr("upgraded",0)!=1&&roll<=53&&roll>=1){
            data.putInt("have"+roll,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            boolean complete=true;for(int n=1;n<=53;n++)if(data.getIntOr("have"+n,0)!=1){complete=false;break;}
            if(complete)g.items().academy().ready(p,hand,stack);
        }
        g.items().wands().wandCasted(p);return true;
    }
    /** Source random_spell uses55 branches; the source library random_spell_lib uses only53. */
    static void cast(GameController g,ServerPlayer p,InteractionHand hand,int roll){
        if(roll>=1&&roll<=53){LbcLibraryRandom.castRandom(g,p,hand,roll);return;}
        if(roll==54){
            int spell=p.getRandom().nextInt(242)+1;
            for(int n=0;n<17&&spell>=28&&spell<=111;n++)spell=p.getRandom().nextInt(242)+1;
            // No source command exists for128 in random_spell_lol.
            if(spell==242)flying(g,p,hand);else if(spell!=128)LbcLibrarySpells.cast(g,p,hand,spell);
            award(g,p,"lbc:true_advancements/staffs/unstable_book");
        }else if(roll==55)flying(g,p,hand);
        LbcOverload.sound(p,p.position(),"minecraft:item.book.page_turn",10,1,0);
    }
    private static void flying(GameController g,ServerPlayer p,InteractionHand hand){
        int kind=p.getRandom().nextInt(9);g.items().sauvojen().cast(p,hand,ItemStack.EMPTY,kind);
        var advancement=g.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/sauvojen_tuntija_wand"));
        if(advancement!=null)p.getAdvancements().award(advancement,"wand"+(kind+1));
    }
    /** Called within the existing Academy actor's admitted, deduplicated main-hand interaction. */
    public static boolean academy(GameController g,ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);if(!book(stack))return false;
        var data=LbcFartus.data(stack);
        if(data.getIntOr("pissed_by_madoka_sob_sob_sob_sob_sob",0)==1)return true;
        p.sendSystemMessage(Component.translatable("magic_academy_root").withStyle(ChatFormatting.GREEN));
        if(data.getIntOr("upgraded",0)==1)p.sendSystemMessage(Component.translatable("already_upgraded"));
        else if(data.getIntOr("upgrade_available",0)==1){
            data.putInt("upgraded",1);data.putInt("mode",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            stack.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_ss"));
            stack.set(DataComponents.LORE,stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).withLineAdded(
                    Component.translatable("upgraded_item").withStyle(s->s.withColor(ChatFormatting.GREEN).withItalic(false))));
            p.sendSystemMessage(Component.translatable("upgrade_succesful_unstable_spellbook").withStyle(ChatFormatting.YELLOW));
        }else if(data.getIntOr("upgradable",0)==1)p.sendSystemMessage(Component.translatable("no_conditions"));
        else{data.putInt("upgradable",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            p.sendSystemMessage(Component.translatable("upgrade_condition_unstable_spellbook").withStyle(ChatFormatting.YELLOW));}
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();return true;
    }
    /** Accepted native melee victim is authoritative; never guess a nearby HurtTime body. */
    public static void damaged(GameController g,LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer p)||!LbcDamage.isMeleeAttack(source,p)
                ||!whip(p.getMainHandItem())||!active(g,p,InteractionHand.MAIN_HAND)||!victim.getType().builtInRegistryHolder().is(MOBS)
                ||!g.items().wands().validRoleTarget(victim,true))return;
        var stack=p.getMainHandItem();var data=LbcFartus.data(stack);
        data.putString(TARGET,victim.getUUID().toString());data.putInt("teleportid",(int)(victim.getUUID().getMostSignificantBits()>>>32));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        twitch(g,p,victim.position());
    }
    private static boolean teleport(GameController g,ServerPlayer p,ItemStack stack){
        var data=LbcFartus.data(stack);LivingEntity target=null;
        var full=data.getStringOr(TARGET,"");
        if(!full.isEmpty()){
            try{var body=p.level().getEntity(UUID.fromString(full));if(body instanceof LivingEntity living)target=living;}catch(IllegalArgumentException ignored){}
        }else if(data.contains("teleportid")){
            int id=data.getIntOr("teleportid",0);
            for(var entity:p.level().getAllEntities())if(entity instanceof LivingEntity living&&entity!=p
                    &&(int)(entity.getUUID().getMostSignificantBits()>>>32)==id&&living.getType().builtInRegistryHolder().is(MOBS)
                    &&g.items().wands().validRoleTarget(living,false)){target=living;break;}
        }
        portal(p);
        if(target!=null&&target!=p&&target.isAlive()&&!target.isRemoved()&&target.getType().builtInRegistryHolder().is(MOBS)
                &&g.items().wands().validRoleTarget(target,false)){
            var velocity=p.getDeltaMovement();
            p.teleportTo(p.level(),target.getX(),target.getY(),target.getZ(),TeleportMomentum.positionFlags(Set.of()),p.getYRot(),p.getXRot(),true);
            p.setDeltaMovement(velocity);
            if(target instanceof ServerPlayer)award(g,p,"lbc:true_advancements/swords/anal_whip");
        }
        portal(p);twitch(g,p,p.position());return true;
    }
    private static void portal(ServerPlayer p){
        p.level().sendParticles(ParticleTypes.PORTAL,true,false,p.getX(),p.getY()+.81,p.getZ(),50,0,.5,0,.5);
        LbcOverload.sound(p,"minecraft:entity.enderman.teleport",1,1,0);
    }
    private static void twitch(GameController g,ServerPlayer p,net.minecraft.world.phys.Vec3 at){
        LbcOverload.sound(p,at,TWITCH[p.getRandom().nextInt(TWITCH.length)],2.5f,1,0);
        p.level().sendParticles(new DustParticleOptions(0xffffff,1),at.x,at.y+.8,at.z,30,.1,.1,.1,.2);
        // Source helper runs as the attacker; its bottle is spent at the helper's execution position.
        boolean bottle=false;for(int n=0;n<p.getInventory().getContainerSize();n++)if(p.getInventory().getItem(n).is(Items.GLASS_BOTTLE)){bottle=true;break;}
        if(bottle){
            for(var reward:g.lbc().rollTable(p.level(),at,p,"lbc:succubus_potion")){
                var drop=new ItemEntity(p.level(),at.x,at.y,at.z,reward);drop.setThrower(p);g.items().markCurrentItemEpoch(drop);p.level().addFreshEntity(drop);
            }
            p.getInventory().clearOrCountMatchingItems(s->s.is(Items.GLASS_BOTTLE),false,1,p.inventoryMenu.getCraftSlots());
            p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        }
    }
    private static void award(GameController g,ServerPlayer p,String id){
        var advancement=g.server().getAdvancements().get(Identifier.parse(id));
        if(advancement!=null)for(var criterion:advancement.value().criteria().keySet())p.getAdvancements().award(advancement,criterion);
    }
}
