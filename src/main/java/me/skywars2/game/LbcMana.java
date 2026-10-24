package me.skywars2.game;

import net.minecraft.ChatFormatting;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.LinkedHashSet;

/** Общая мана: исходные scoreboard units, расход, регенерация и временные модификаторы. */
public final class LbcMana {
    public static final Item MANA_POTION=registerPotion("skywars2:mana_potion",Items.POTION);
    public static final Item MANA_SPLASH_POTION=registerPotion("skywars2:mana_splash_potion",Items.SPLASH_POTION);
    public static final Item MANA_LINGERING_POTION=registerPotion("skywars2:mana_lingering_potion",Items.LINGERING_POTION);
    private static final List<String> POTION_TIMERS=List.of("effect_boost_mana","effect_add_manamax","effect_add_manaregen","effect_add_mana","effect_remove_mana");
    private static final int[] STAT_EFFECT_ORDER={3,1,2,4};
    private static final class Drink extends PotionItem implements PolymerItem {
        Drink(Properties p) { super(p); }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context) { return Items.POTION; }
    }
    private static final class Splash extends SplashPotionItem implements PolymerItem {
        Splash(Properties p) { super(p); }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context) { return Items.SPLASH_POTION; }
    }
    private static final class Lingering extends LingeringPotionItem implements PolymerItem {
        Lingering(Properties p) { super(p); }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context) { return Items.LINGERING_POTION; }
    }
    private static Item registerPotion(String id,Item proxy) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        // 26.3 finalizes ITEM_MODEL after component initializers; keep the vanilla model at that step.
        var properties=new Item.Properties() {
            @Override public Identifier effectiveModel() { return BuiltInRegistries.ITEM.getKey(proxy); }
        }.setId(key).stacksTo(1)
                .overrideDescription(proxy.getDescriptionId())
                .component(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        if(proxy==Items.POTION) properties.component(DataComponents.CONSUMABLE,Consumables.DEFAULT_DRINK).usingConvertsTo(Items.GLASS_BOTTLE);
        if(proxy==Items.LINGERING_POTION) properties.component(DataComponents.POTION_DURATION_SCALE,0.25f);
        return Registry.register(BuiltInRegistries.ITEM,key,proxy==Items.POTION ? new Drink(properties)
                : proxy==Items.SPLASH_POTION ? new Splash(properties) : new Lingering(properties));
    }
    static void register() { LbcOverload.register(); }
    private static final CompoundTag BELT=flag("manabelt"), MAGE=flag("mage"), OVERLOAD=flag("overload");
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final int[] ARMOR_REGEN={4,6,7,4};
    private record Gear(int belts,int regeneration,int overload,int mageMask) { }
    private final GameController game;
    private final LbcOverload overload;
    private final Map<UUID,Gear> gear=new HashMap<>();
    private final Set<UUID> dirty=new HashSet<>();
    private final Set<LivingEntity> pendingPotions=new LinkedHashSet<>();
    private final Set<LivingEntity> statEffects=new LinkedHashSet<>();

    LbcMana(GameController game) { this.game=game; this.overload=new LbcOverload(game,this); }
    public LbcOverload overload() { return overload; }
    private static CompoundTag flag(String name) { var tag=new CompoundTag(); tag.putInt(name,1); return tag; }
    private static boolean matches(ItemStack stack,CompoundTag flag) {
        var data=stack.get(DataComponents.CUSTOM_DATA); return !stack.isEmpty() && data!=null && data.matchedBy(flag);
    }
    /** Один bridge для ресурсов и кастомных таймеров; не создаёт отдельный account JSON на каждый тик. */
    ScoreAccess score(net.minecraft.world.scores.ScoreHolder entity,String name) {
        var board=game.server().getScoreboard(); var objective=board.getObjective(name);
        if(objective==null) objective=board.addObjective(name,ObjectiveCriteria.DUMMY,Component.literal(name),ObjectiveCriteria.RenderType.INTEGER,false,null);
        return board.getOrCreatePlayerScore(entity,objective);
    }
    private void write(Entity entity,String name,int value) {
        var score=score(entity,name); if(score.get()!=value) score.set(value);
    }
    public int get(ServerPlayer p) { return score(p,"mana").get(); }
    /** HUD reads only existing scoreboard entries; rendering must not create objectives or player scores. */
    public int hudValue(net.minecraft.world.scores.ScoreHolder holder,String objectiveName) {
        var board=game.server().getScoreboard(); var objective=board.getObjective(objectiveName);
        var value=objective==null ? null : board.getPlayerScoreInfo(holder,objective);
        return value==null ? 0 : value.value();
    }
    public void set(ServerPlayer p,int value) { write(p,"mana",value); }
    public int maximum(ServerPlayer p) { return score(p,"manamax").get(); }
    public void maximum(ServerPlayer p,int value) { write(p,"manamax",value); }
    public int baseRegen(ServerPlayer p) { return score(p,"manaregen").get(); }
    public void baseRegen(ServerPlayer p,int value) { write(p,"manaregen",value); }
    public int regeneration(ServerPlayer p) { return score(p,"manaregen_visual").get(); }
    public int boost(ServerPlayer p) { return score(p,"effect_boost_mana").get(); }
    public void boost(ServerPlayer p,int ticks) {
        if(ticks>0 && game.items().magic().rejectCustomEffect(p)) return;
        write(p,"effect_boost_mana",Math.max(0,ticks));
    }
    private final java.util.Map<ServerPlayer,LbcIndirectKills.Owner> reverseOwners=new java.util.WeakHashMap<>();
    /** A repeated plague is a new player action; duration-only extensions retain the old cause. */
    void applyReverse(ServerPlayer owner,ServerPlayer target,int seconds){
        if(seconds<=0||game.items().magic().rejectCustomEffect(target))return;
        reverse(target,(int)Math.min(Integer.MAX_VALUE,(long)reverse(target)+seconds));
        reverseOwners.put(target,game.items().indirectKills().wildCause(owner));
    }
    public int reverse(ServerPlayer p) { return score(p,"reverse_mana").get(); }
    public void reverse(ServerPlayer p,int seconds) {
        if(seconds>0 && game.items().magic().rejectCustomEffect(p)) return;
        write(p,"reverse_mana",Math.max(0,seconds));
        if(seconds<=0)reverseOwners.remove(p);
    }
    /** Personal anti-magic countdown in seconds; it is not a cast or aura gate. */
    public int noMagic(ServerPlayer p) { return score(p,"nomagic").get(); }
    public void noMagic(ServerPlayer p,int seconds) {
        if(seconds>0 && game.items().magic().rejectCustomEffect(p)) return;
        write(p,"nomagic",Math.max(0,seconds));
    }
    /** Source effect_nomagic: extend the timer and empty the mana pool. */
    public void applyNoMagic(ServerPlayer p,int seconds) {
        if(game.items().magic().rejectCustomEffect(p)) return;
        noMagic(p,noMagic(p)+seconds);
        set(p,0);
    }
    /** Active potion timers are native player counters, separate from mana resources. */
    void addPlayerCounters(ServerPlayer player) {
        if(game.items().magic().rejectCustomEffect(player)) return;
        for(String name:POTION_TIMERS) {
            int remaining=hudValue(player,name);
            if(remaining>0) write(player,name,remaining+1);
        }
        for(String name:List.of("overload_jump","overload_slam")) {
            int remaining=hudValue(player,name);
            if(remaining>0) write(player,name,remaining+1);
        }
    }

    public void add(ServerPlayer p,int amount) { set(p,get(p)+amount); }

    public boolean visible(ServerPlayer p) {
        return game.canUseGameplayItems(p) && p.isAlive() && !p.isRemoved() && p.level()==game.maps().level()
                && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean active(ServerPlayer p) {
        return visible(p) && !p.entityTags().contains("time_stopped") && !game.items().magic().suppressed(p);
    }
    /** Обычный cast не допускает овердрафт; специальные способности используют явный signed add. */
    public boolean trySpend(ServerPlayer p,int cost) {
        if(cost<0) throw new IllegalArgumentException("Mana cost must be nonnegative");
        if(!active(p)) return false;
        int value=get(p); if(value<cost) return false;
        set(p,value-cost); return true;
    }
    /** Полный Overload-комплект разрешает жезлам настоящий овердрафт, без временной подмены ресурса. */
    public boolean trySpendWand(ServerPlayer p,int cost) {
        return trySpendWand(p,cost,cost);
    }
    /** Exoperso (user) допускает каст от 5000, но списывает 25000/10000; Overload обходит только допуск. */
    public boolean trySpendWand(ServerPlayer p,int required,int cost) {
        if(required<0 || cost<0) throw new IllegalArgumentException("Mana cost must be nonnegative");
        if(!active(p)) return false;
        int value=get(p);
        if(value<required && overloadArmor(p)!=15) return false;
        set(p,value-cost); return true;
    }
    public void resetResources(ServerPlayer p) {
        maximum(p,5000); set(p,5000); baseRegen(p,10); write(p,"manaregen_visual",10);
        clearEffects(p); overload.reset(p); write(p,"falltimer",0); write(p,"food_count",0); inventoryChanged(p);
    }
    public void clearEffects(LivingEntity p) {
        game.items().indirectKills().manaEffectCleared(p);
        for(String timer:POTION_TIMERS) write(p,timer,0);
        write(p,"reverse_mana",0); write(p,"void_resistance",0); reverseOwners.remove(p); pendingPotions.remove(p); statEffects.remove(p);
        if(p instanceof ServerPlayer player) noMagic(player,0);
        if(p instanceof ServerPlayer player) overload.clearEffects(player);
    }
    public void effectsChanged(LivingEntity p) {
        if(p.level()!=game.maps().level()) return;
        var effect=p.getEffect(MobEffects.RAID_OMEN);
        if(effect!=null && effect.getAmplifier()>=16 && effect.getAmplifier()<=20) {
            if(p instanceof ServerPlayer player && game.items().magic().rejectCustomEffect(player)) {
                pendingPotions.remove(p); return;
            }
            pendingPotions.add(p);
            // Ordinary stat potions last one native tick: capture before LivingEntity expires them.
            // Resource changes still run in the mana tick; this hook only transfers the timer.
            if(effect.getDuration()==1) consumePotion(p);
        }
        else pendingPotions.remove(p);
    }
    private boolean effectActor(LivingEntity p) {
        return p.isAlive() && !p.isRemoved() && p.level()==game.maps().level() && !p.entityTags().contains("spectator")
                && (!(p instanceof ServerPlayer player) || visible(player));
    }
    void consumePotion(LivingEntity p) {
        if(!pendingPotions.contains(p) || !effectActor(p) || p.entityTags().contains("time_stopped")) return;
        if(p instanceof ServerPlayer player && game.items().magic().rejectCustomEffect(player)) {
            clearEffects(p); p.removeEffect(MobEffects.RAID_OMEN); return;
        }
        pendingPotions.remove(p); var effect=p.getEffect(MobEffects.RAID_OMEN);
        if(effect==null || effect.getAmplifier()<16 || effect.getAmplifier()>20) return;
        int duration=effect.getDuration()==2 ? 1 : effect.getDuration();
        score(p,POTION_TIMERS.get(effect.getAmplifier()-16)).add(duration);
        // Source arrows2_nodebuff checks the selected skill after converting any sentinel.
        if(p instanceof ServerPlayer player && game.config().lbcSkills && game.players().get(player.getUUID()).lbcSkill==19)
            write(p,"effect_remove_mana",0);
        if(effect.getAmplifier()==20 && score(p,"effect_remove_mana").get()>0)
            game.items().indirectKills().manaPotionConsumed(p);
        if(effect.getAmplifier()!=16) statEffects.add(p);
        p.removeEffect(MobEffects.RAID_OMEN);
    }
    /** Permanent resource changes run before regen. Only the destructive potion pauses under Greed. */
    void tickPotions(LivingEntity p) {
        if(!statEffects.contains(p) || !effectActor(p) || p.entityTags().contains("time_stopped")) return;
        if(p instanceof ServerPlayer player && game.items().magic().rejectCustomEffect(player)) { clearEffects(p); return; }
        boolean remaining=false;
        for(int index:STAT_EFFECT_ORDER) {
            var timer=score(p,POTION_TIMERS.get(index)); int ticks=timer.get(); if(ticks<=0) continue;
            if(index==4 && p.entityTags().contains("sin_greed_active")) { remaining=true; continue; }
            switch(index) {
                case 1 -> { score(p,"manamax").add(2500); award(p,"lbc:true_advancements/potions/max_mana"); }
                case 2 -> { score(p,"manaregen").add(5); award(p,"lbc:true_advancements/potions/mana_regen"); }
                case 3 -> { score(p,"mana").add(10000); award(p,"lbc:true_advancements/potions/add_mana"); }
                case 4 -> badPotion(p);
                default -> throw new IllegalArgumentException("Unknown mana timer");
            }
            timer.set(ticks-1); remaining|=ticks>1;
        }
        if(!remaining) statEffects.remove(p);
    }
    private void badPotion(LivingEntity p) {
        game.items().indirectKills().manaLost(p);
        int roll=p.getRandom().nextInt(10)+1;
        score(p,"mana").add(-2000);
        if(roll<=8) score(p,"manamax").add(-1000);
        if(roll<=6) score(p,"manaregen").add(-1);
    }
    void loaded(LivingEntity p) {
        effectsChanged(p);
        var board=game.server().getScoreboard();
        for(int index=1;index<POTION_TIMERS.size();index++) {
            var objective=board.getObjective(POTION_TIMERS.get(index));
            var value=objective==null ? null : board.getPlayerScoreInfo(p,objective);
            if(value!=null && value.value()>0) { statEffects.add(p); break; }
        }
    }
    void unloaded(LivingEntity p) { pendingPotions.remove(p); statEffects.remove(p); }
    public void inventoryChanged(ServerPlayer p) { dirty.add(p.getUUID()); }
    public int overloadArmor(ServerPlayer p) { return equipment(p).overload(); }
    public boolean mageFullSet(ServerPlayer p) { return equipment(p).mageMask()==15; }
    private Gear equipment(ServerPlayer p) {
        UUID id=p.getUUID(); Gear result=gear.get(id);
        if(dirty.remove(id) || result==null) {
            boolean inInventory=false;
            for(int slot=0;slot<p.getInventory().getContainerSize();slot++)
                if(matches(p.getInventory().getItem(slot),BELT)) { inInventory=true; break; }
            // Нативный /clear ... 0 считает inventory + 2x2 craft + cursor без удаления. Inventory gate сохранён.
            int belts=inInventory ? p.getInventory().clearOrCountMatchingItems(stack->matches(stack,BELT),true,0,p.inventoryMenu.getCraftSlots()) : 0;
            int armor=0, overloadMask=0, mageMask=0;
            for(int i=0;i<ARMOR.length;i++) {
                var stack=p.getItemBySlot(ARMOR[i]);
                if(matches(stack,MAGE)) { armor+=ARMOR_REGEN[i]; mageMask|=1<<i; }
                if(matches(stack,OVERLOAD)) overloadMask|=1<<i;
            }
            result=new Gear(belts,armor,overloadMask,mageMask); gear.put(id,result);
        }
        return result;
    }
    void tickPlayer(ServerPlayer p) {
        // После выхода из подавления старые таймеры не должны успеть изменить ману до пассивной очистки.
        if(game.items().magic().rejectCustomEffect(p)
                && (pendingPotions.contains(p) || statEffects.contains(p)
                    || boost(p)>0 || reverse(p)>0 || noMagic(p)>0)) clearEffects(p);
        consumePotion(p); tickPotions(p);
        if(!active(p)) { overload.armor().tick(p); return; }
        if(noMagic(p)>0) {
            overload.armor().tick(p);
            return;
        }
        overload.beforeRegen(p);
        var equipment=equipment(p); int base=baseRegen(p), visual=base+equipment.regeneration()+equipment.belts()*2;
        if(equipment.belts()*2>base) award(p,"lbc:true_advancements/accesories/mana_belt");
        int duration=boost(p);
        if(duration>0) {
            int before=visual; visual=Math.floorDiv(visual*3,2); boost(p,duration-1);
            award(p,"lbc:true_advancements/potions/effect_magic_buff");
            if(visual-before>=15) award(p,"lbc:true_advancements/potions/mana_temp");
        }
        write(p,"manaregen_visual",visual);
        int mana=get(p), max=maximum(p); boolean reversed=reverse(p)>0;
        if(reversed) { mana-=visual; if(visual>0)game.items().indirectKills().manaLost(p,reverseOwners.get(p)); }
        // Source helper выполняется только ниже cap при положительном regen, либо всегда при отрицательном.
        // Уже выданный избыток маны не обрезается произвольно; нижней границы у ресурса нет.
        if(visual>0 && mana<max || visual<0) {
            if(!reversed) mana+=visual;
            if(mana>max) mana=max;
        }
        set(p,mana);
        game.items().indirectKills().manaRecovered(p);
        overload.afterRegen(p);
        overload.armor().tick(p);
    }
    /** Source void resistance returns players below y=0 to y=320, then spends one second. */
    void voidProtectionSecond(ServerPlayer p) {
        int remaining=hudValue(p,"void_resistance");
        if(remaining<=0||!visible(p)||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(p.level())
                ||game.items().timeStop().frozen(p)||p.entityTags().contains("time_stopped")||LbcGreed.active(p))return;
        if(p.getY()<0)p.teleportTo(p.level(),p.getX(),320,p.getZ(),java.util.Set.of(),p.getYRot(),p.getXRot(),true);
        score(p,"void_resistance").set(remaining-1);
        award(p,"lbc:true_advancements/potions/effect_void_resistance");
    }
    void secondPlayer(ServerPlayer p) {
        voidProtectionSecond(p);
        if(visible(p) && !p.entityTags().contains("time_stopped")) {
            if(reverse(p)>0) reverse(p,reverse(p)-1);
            if(noMagic(p)>0) noMagic(p,noMagic(p)-1);
            if(noMagic(p)>0 && game.server().getTickCount()%40==0)
                award(p,"lbc:true_advancements/potions/effect_no_magic");
        }
    }
    void award(LivingEntity entity,String id) {
        if(!(entity instanceof ServerPlayer p)) return;
        var advancement=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(id));
        if(advancement!=null && !p.getAdvancements().getOrStartProgress(advancement).isDone())
            for(String criterion:advancement.value().criteria().keySet()) p.getAdvancements().award(advancement,criterion);
    }
    void tick() {
        for(var p:game.server().getPlayerList().getPlayers()) {
            tickPlayer(p);
            if(game.server().getTickCount()%20==0) secondPlayer(p);
        }
        for(var p:List.copyOf(pendingPotions)) if(!(p instanceof ServerPlayer)) consumePotion(p);
        for(var p:List.copyOf(statEffects)) if(!(p instanceof ServerPlayer)) {
            if(!effectActor(p)) unloaded(p); else tickPotions(p);
        }
    }
    /** Source HUD содержит glyph/числа, без языковой подписи; literal section-sign separators сохранены. */
    public MutableComponent hud(ServerPlayer p) {
        int mana=hudValue(p,"mana");
        ChatFormatting color=mana>=0 ? ChatFormatting.AQUA : mana>=-5000 ? ChatFormatting.YELLOW
                : mana>=-10000 ? ChatFormatting.RED : mana>=-15000 ? ChatFormatting.DARK_RED
                : mana>=-20000 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE;
        return Component.literal(mana>=0 ? "\uE100" : "\uE132").withStyle(style->style.withColor(0x4e5c24))
                .append(" ").append(Component.literal(Integer.toString(mana)).withStyle(color)).append("\u00a7a/")
                .append(Component.literal(Integer.toString(hudValue(p,"manamax"))).withStyle(ChatFormatting.AQUA)).append(" \u00a7a[\u00a7a+")
                .append(Component.literal(Integer.toString(hudValue(p,"manaregen_visual"))).withStyle(ChatFormatting.AQUA)).append("\u00a7a]");
    }
    /** Ported mana-related source actionbar entries, kept in source order after the other status icons. */
    public MutableComponent appendHudEarlyStatuses(ServerPlayer p,MutableComponent hud) {
        appendHudScore(hud,p,"nomagic","\uE115",ChatFormatting.RED);
        appendHudScore(hud,p,"reverse_mana","\uE118",ChatFormatting.RED);
        return hud;
    }
    public MutableComponent appendHudManaStatuses(ServerPlayer p,MutableComponent hud) {
        appendHudScore(hud,p,"effect_add_mana","\uE157",ChatFormatting.GREEN);
        appendHudScore(hud,p,"effect_add_manamax","\uE157",ChatFormatting.GREEN);
        appendHudScore(hud,p,"effect_add_manaregen","\uE157",ChatFormatting.GREEN);
        appendHudScore(hud,p,"effect_remove_mana","\uE160",ChatFormatting.RED);
        appendHudScore(hud,p,"effect_boost_mana","\uE165",ChatFormatting.GREEN);
        return hud;
    }
    private void appendHudScore(MutableComponent hud,ServerPlayer p,String objective,String glyph,ChatFormatting color) {
        int value=hudValue(p,objective); if(value<=0) return;
        hud.append(Component.literal("  "+glyph+" ")).append(Component.literal(Integer.toString(value)).withStyle(color));
    }
    void forget(ServerPlayer p) { write(p,"falltimer",0); gear.remove(p.getUUID()); dirty.remove(p.getUUID()); clearEffects(p); overload.reset(p); }
    void stop() {
        gear.clear(); dirty.clear(); pendingPotions.clear(); reverseOwners.clear();
        for(var p:List.copyOf(statEffects)) for(String timer:POTION_TIMERS) write(p,timer,0);
        statEffects.clear(); for(var p:game.server().getPlayerList().getPlayers()) { write(p,"falltimer",0); clearEffects(p); overload.reset(p); }
    }
}
