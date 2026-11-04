package me.skywars2.game;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Native case transaction and source strip owner. Draft: no public exposure before qualification. */
final class LbcCases {
    private static final Gson JSON = new Gson();
    private static final String[] ROOTS = {"case0/main", "case1/select", "case2/select", "case3/select", "case4/select"};
    private static final int[] COINS = {0, 20, 50, 100, 1000};
    private final GameController game;
    // Derived online indexes/cache only. Durable state is one scalar Profile.casePending value.
    private final LinkedHashSet<UUID> active = new LinkedHashSet<>();
    private final Map<UUID, Map<Token, ItemStack>> icons = new HashMap<>();
    private record Decoded(String serialized, Pending pending) {}
    private final Map<UUID, Decoded> decoded = new HashMap<>();

    LbcCases(GameController game) { this.game = game; }

    /** Complete native token descriptor; no serialized ItemStack/NBT or live server references. */
    record Token(String table, long seed, int output, String family, int value, int tier) {}
    record Payout(String unlock, int coins, int memcoins, String message, boolean manualClaim) {}
    record Pending(int version, int kind, List<Token> strip, int phase, int timer,
                   Payout payout, boolean paid) {
        Pending { strip = List.copyOf(strip); }
    }
    record View(int kind, int page, int phase, int timer, List<ItemStack> strip,
                int winnerSlot, boolean paid) {}

    boolean hasPending(ServerPlayer player) { return !profile(player).casePending.isEmpty(); }

    /** A repeated click resumes the existing opening regardless of which case was clicked. */
    boolean open(ServerPlayer player, int kind) {
        if (kind < 0 || kind >= ROOTS.length || !game.canUseMenus(player)
                || game.vanity().blocked(player) || game.items().timeStop().stopped(player.level())) return false;
        if (hasPending(player)) { hydrate(player, read(player)); active.add(player.getUUID()); return true; }
        var profile = profile(player);
        if (profile.keys[kind] < 1) return false;
        var before = new MatchValueSnapshot(profile);
        try {
            LbcSourceUnlocks.importLegacy(player, profile);
            var strip = new ArrayList<Token>(27);
            for (int n = 0; n < 27; n++) strip.add(draw(player, "swrg:gui/roll/" + ROOTS[kind]).getFirst());
            compensate(player, profile, strip, kind);
            // Q24: both source ownership filters and their native compensation draws
            // belong to acceptance. Future unlock changes cannot alter this opening.
            compensate(player, profile, strip, kind);
            // Source phase1 falls through phase2, shifts once, increments to3, then timer1->0.
            shift(strip);
            profile.keys[kind]--;
            writePending(player,new Pending(2, kind, strip, 3, 0, normalize(strip.get(20), kind), false));
            game.players().saveAtomic();
        } catch (IOException | RuntimeException failure) {
            before.restore(); icons.remove(player.getUUID()); decoded.remove(player.getUUID());
            System.err.println("Case acceptance retained old profile: " + failure.getMessage());
            player.sendSystemMessage(Component.translatable("commands.generic.failed"));
            return false;
        }
        active.add(player.getUUID());
        clickSound(player, kind);
        return true;
    }

    /** Exactly one invocation per server tick; UI refresh never advances this owner. */
    void tick() {
        for (UUID id : List.copyOf(active)) {
            var player = game.server().getPlayerList().getPlayer(id);
            if (player == null) { active.remove(id); icons.remove(id); decoded.remove(id); continue; }
            if (game.items().timeStop().stopped(player.level())) continue;
            if (!hasPending(player)) { active.remove(id); icons.remove(id); decoded.remove(id); continue; }
            step(player);
        }
    }

    private void step(ServerPlayer player) {
        var profile = profile(player);
        var before = new MatchValueSnapshot(profile);
        Pending old = read(player);
        boolean click = false, award = false, complete = false, normalized = false;
        Pending next;
        try {
            if (old.timer() > 0) {
                next = new Pending(old.version(),old.kind(),old.strip(),old.phase(),old.timer()-1,old.payout(),old.paid());
            } else {
                int phase = old.phase();
                var strip = new ArrayList<>(old.strip());
                if (phase >= 2 && phase <= 20) { shift(strip); click = true; }
                // Source cases1/2 filter at20; cases0/3/4 filter at22, before phase increment.
                int filter = old.kind()==1 || old.kind()==2 ? 20 : 22;
                Payout payout = old.payout();
                if (phase == filter) {
                    // Resume pre-Q24 journals with their original delayed filter.
                    if (old.version()==1) {
                        compensate(player, profile, strip, old.kind());
                        payout = normalize(strip.get(2), old.kind());
                    }
                    normalized = true;
                }
                int timer = phase<=14 ? 1 : phase<=18 ? 2 : phase==19 ? 3 : phase==20 ? 4 : 0;
                // Driver decrements freshly assigned positive timers in the same source tick.
                timer = Math.max(0, timer-1);
                phase++;
                boolean paid = old.paid();
                if (phase == 25 && !paid) {
                    if (payout == null) throw new IllegalStateException("Payout not durably normalized before phase25");
                    apply(profile, payout);
                    paid = true;
                    award = true;
                }
                next = new Pending(old.version(),old.kind(),strip,phase,timer,payout,paid);
                complete = phase == 30;
            }
            writePending(player,complete ? null : next);
            // Payout+paid are one atomic file replacement; projections occur only after success.
            // Ordinary animation is in-memory. Existing autosave can persist any intermediate
            // phase; a crash otherwise replays that same immutable strip from its last save.
            if (normalized || award) game.players().saveAtomic();
        } catch (IOException | RuntimeException failure) {
            before.restore(); decoded.remove(player.getUUID());
            System.err.println("Case tick retained committed phase: " + failure.getMessage());
            // Pause until the player reopens; a failed disk must not produce per-tick errors.
            active.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("commands.generic.failed"));
            return;
        }
        if (click) clickSound(player, old.kind());
        if (award) {
            LbcSourceUnlocks.publish(player, profile);
            announce(player, next.payout(), next.strip().get(2));
            notifySound(player,SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1, 1);
        }
        if (complete) { active.remove(player.getUUID()); icons.remove(player.getUUID()); decoded.remove(player.getUUID()); }
    }

    /** Nine source backing slots0..8, mapped by GameMenus to menu9..17. Pure cache read. */
    View renderView(ServerPlayer player) {
        if (!hasPending(player)) return null;
        var state = read(player);
        var cached = icons.get(player.getUUID());
        if (cached == null) throw new IllegalStateException("Case view before join/open hydration");
        var visible = new ArrayList<ItemStack>(9);
        for (int n=0;n<9;n++) {
            var icon = cached.get(state.strip().get(n));
            if (icon == null) throw new IllegalStateException("Missing immutable case icon");
            visible.add(icon.copy());
        }
        return new View(state.kind(),5100+100*state.kind(),state.phase(),state.timer(),List.copyOf(visible),2,state.paid());
    }

    void onJoin(ServerPlayer player) {
        var profile = profile(player);
        var before = new MatchValueSnapshot(profile);
        try {
            if (LbcSourceUnlocks.importLegacy(player,profile)) game.players().saveAtomic();
        } catch (IOException failure) { before.restore(); throw new IllegalStateException("Source unlock migration failed",failure); }
        LbcSourceUnlocks.publish(player,profile);
        if (hasPending(player)) { hydrate(player,read(player)); active.add(player.getUUID()); }
    }

    void onLeave(ServerPlayer player) { active.remove(player.getUUID()); icons.remove(player.getUUID()); decoded.remove(player.getUUID()); }

    /** Invoke after whole coordinator restored profiles AND native tags/advancements/RNG. */
    void restored() {
        active.clear(); icons.clear(); decoded.clear();
        for (var player : game.server().getPlayerList().getPlayers()) restored(player);
    }

    /** Delayed rollback login already restored canonical ownership; never import future tags. */
    void restored(ServerPlayer player) {
        active.remove(player.getUUID()); icons.remove(player.getUUID()); decoded.remove(player.getUUID());
        LbcSourceUnlocks.publish(player,profile(player));
        if (hasPending(player)) { hydrate(player,read(player)); active.add(player.getUUID()); }
    }

    private PlayerStore.Profile profile(ServerPlayer player) { return game.players().get(player.getUUID()); }

    private void writePending(ServerPlayer player,Pending pending) {
        String serialized=pending==null?"":JSON.toJson(pending);
        profile(player).casePending=serialized;
        if(pending==null)decoded.remove(player.getUUID());
        else decoded.put(player.getUUID(),new Decoded(serialized,pending));
    }

    private Pending read(ServerPlayer player) {
        String serialized=profile(player).casePending;
        var cached=decoded.get(player.getUUID());
        // Identity follows Profile's immutable scalar. Whole-profile restore/string replacement
        // invalidates this derived decode automatically, without JSON parsing every frame.
        if(cached!=null && cached.serialized()==serialized)return cached.pending();
        Pending pending = JSON.fromJson(serialized,Pending.class);
        if (pending==null || (pending.version()!=1 && pending.version()!=2) || pending.kind()<0 || pending.kind()>4
                || pending.strip().size()!=27 || pending.phase()<3 || pending.phase()>29
                || pending.timer()<0 || pending.timer()>3 || ((pending.paid() || pending.version()==2) && pending.payout()==null))
            throw new IllegalStateException("Malformed committed case; retain profile for repair, never reroll/refund");
        decoded.put(player.getUUID(),new Decoded(serialized,pending));
        return pending;
    }

    private static void shift(List<Token> strip) {
        // Source copies0<-1 ...25<-26. Slot26 remains unchanged, never wrap or append a new roll.
        for (int n=0;n<26;n++) strip.set(n,strip.get(n+1));
    }

    private List<Token> draw(ServerPlayer player,String table) {
        long seed;
        do { seed=player.level().getRandom().nextLong(); } while(seed==0); // native zero seed means unseeded
        return materialize(player,table,seed);
    }

    private List<Token> materialize(ServerPlayer player,String table,long seed) {
        var params=new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN,player.position())
                .withParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.COMMAND);
        var nativeTable=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(table)));
        var stacks=nativeTable.getRandomItems(params,seed);
        int expected=table.contains("/have/")?27:1;
        if(stacks.size()!=expected)throw new IllegalStateException("Source case table output cardinality "+table+": "+stacks.size());
        var tokens=new ArrayList<Token>(expected);
        var cache=icons.computeIfAbsent(player.getUUID(),ignored->new HashMap<>());
        for(int n=0;n<stacks.size();n++) {
            var data=LbcFartus.data(stacks.get(n));
            String family="";int value=0;
            for(String field:new String[]{"rewardkit","rewardskill","rewardbar","rewardsound","rewardcoin","drop"}) {
                int found=data.getIntOr(field,0);
                if(found!=0) { if(!family.isEmpty())throw new IllegalStateException("Ambiguous source token");family=field;value=found; }
            }
            // In case4 reward is an integer; case0 token dispatch uses a string with the same key.
            int suffix=data.getIntOr("reward",0);
            if(suffix!=0) { if(!family.isEmpty())throw new IllegalStateException("Ambiguous suffix token");family="suffix";value=suffix; }
            int tier=tier(family,value);
            Token token=new Token(table,seed,n,family,value,tier);
            cache.put(token,stacks.get(n).copy());tokens.add(token);
        }
        return List.copyOf(tokens);
    }

    private void hydrate(ServerPlayer player,Pending pending) {
        // Deterministic native replay only on cache construction, never on animation/render ticks.
        var cached=icons.computeIfAbsent(player.getUUID(),ignored->new HashMap<>());
        for(Token token:pending.strip())if(!cached.containsKey(token)) {
            var replay=materialize(player,token.table(),token.seed());
            if(token.output()<0||token.output()>=replay.size()||!token.equals(replay.get(token.output())))
                throw new IllegalStateException("Committed token differs from registered source; do not reroll");
        }
    }

    private void compensate(ServerPlayer player,PlayerStore.Profile profile,List<Token> strip,int kind) {
        if(kind==0||kind==1)compensateFamily(player,profile,strip,"rewardkit",4,false);
        if(kind==0||kind==2)compensateFamily(player,profile,strip,"rewardskill",4,false);
        if(kind==0||kind==3) {
            compensateFamily(player,profile,strip,"rewardbar",4,false);
            compensateFamily(player,profile,strip,"rewardsound",4,false);
        }
        if(kind==4) {
            // Source legendary step removes suffix26 AND all three drop entitlements before one insert.
            for(int tier=1;tier<=4;tier++)replaceOwned(player,profile,strip,"suffix",tier,true);
        }
        icons.get(player.getUUID()).keySet().retainAll(strip);
    }

    private void compensateFamily(ServerPlayer player,PlayerStore.Profile profile,List<Token> strip,String family,int tiers,boolean mem) {
        for(int tier=1;tier<=tiers;tier++) {
            if(family.equals("rewardbar")&&tier==2)continue; // absent source b branch
            replaceOwned(player,profile,strip,family,tier,mem);
        }
    }

    private void replaceOwned(ServerPlayer player,PlayerStore.Profile profile,List<Token> strip,String family,int tier,boolean mem) {
        for(int n=0;n<strip.size();n++) {
            Token t=strip.get(n);
            boolean matches=t.family().equals(family)||(mem&&tier==4&&t.family().equals("drop"));
            if(matches&&t.tier()==tier&&LbcSourceUnlocks.has(player,profile,unlock(t)))strip.set(n,null);
        }
        // Each source have step executes the27-output insert even if there are no holes. Preserve RNG draws.
        String table="swrg:gui/roll/have/"+(char)('a'+tier-1)+(mem?"_m":"");
        List<Token> compensation=draw(player,table);
        int output=0;
        for(int n=0;n<strip.size();n++)if(strip.get(n)==null)strip.set(n,compensation.get(output++));
    }

    private static int tier(String family,int n) {
        return switch(family) {
            case "rewardkit" -> range(n,20,n<=7?1:n<=12?2:n<=17?3:4);
            case "rewardskill" -> range(n,11,n<=4?1:n<=7?2:n<=9?3:4);
            case "rewardbar" -> range(n,20,n<=16?1:n<=19?3:4);
            case "rewardsound" -> range(n,13,n<=7?1:n<=10?2:n==11?3:4);
            case "suffix" -> range(n,26,n<=16?1:n<=22?2:n<=25?3:4);
            case "drop" -> range(n,3,4);
            case "rewardcoin" -> range(n,4,n);
            default -> throw new IllegalStateException("Unknown case token family "+family);
        };
    }
    private static int range(int n,int max,int tier) { if(n<1||n>max)throw new IllegalStateException("Out-of-range case token");return tier; }
    static String unlock(Token token) {
        return switch(token.family()) {
            case "rewardkit" -> "swrg.kit"+token.value();
            case "rewardskill" -> "swrg.skill"+token.value();
            case "rewardbar" -> "swrg.bars"+token.value();
            case "rewardsound" -> "swrg.sound"+token.value();
            case "suffix" -> "swrg:rewards/suffix_"+token.value();
            case "drop" -> "swrg:rewards/drop_"+token.value();
            default -> "";
        };
    }
    private static Payout normalize(Token token,int kind) {
        if(token.family().equals("rewardcoin"))return new Payout("",kind==4?0:COINS[token.value()],kind==4?COINS[token.value()]:0,kind==4?"reward.memcoin":"reward.coin",false);
        String message=switch(token.family()) { case "rewardkit" -> "reward.kit";case "rewardskill" -> "reward.skill";
            case "rewardbar" -> "reward.bar";case "rewardsound" -> "reward.sound";case "suffix" -> "new_suffix";case "drop" -> "new_drop_request";default -> throw new IllegalStateException(); };
        return new Payout(unlock(token),0,0,message,token.family().equals("drop"));
    }
    private static void apply(PlayerStore.Profile profile,Payout payout) {
        profile.coins=Math.addExact(profile.coins,payout.coins());
        profile.memcoins=Math.addExact(profile.memcoins,payout.memcoins());
        if(!payout.unlock().isEmpty()) {
            if(!LbcSourceUnlocks.valid(payout.unlock()))throw new IllegalStateException("Invalid normalized payout");
            profile.legacyUnlocks.add(payout.unlock());
        }
    }
    private void announce(ServerPlayer player,Payout payout,Token winner) {
        if(payout.message().startsWith("reward."))player.sendSystemMessage(OriginalGuiDesign.sourceMessage(payout.message(),player,profile(player),game.config()));
        else if(payout.manualClaim())player.sendSystemMessage(Component.translatable("new_drop_request").withStyle(net.minecraft.ChatFormatting.RED,net.minecraft.ChatFormatting.BOLD));
        else player.sendSystemMessage(Component.translatable("new_suffix").append(icons.get(player.getUUID()).get(winner).getHoverName()));
        if(payout.manualClaim()) {
            // Source manual-contact hints only. Never execute an external action or contact a service.
            player.sendSystemMessage(Component.translatable("drop_request_hint").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal("https://discord.gg/eTr7Wybutc").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.BLUE)
                            .withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create("https://discord.gg/eTr7Wybutc"))))));
            player.sendSystemMessage(Component.translatable("drop_request_hint_alt").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal("@BaffledHedgehog").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.BLUE)
                            .withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create("http://t.me/@BaffledHedgehog"))))));
        }
    }
    private static void notifySound(ServerPlayer player,net.minecraft.sounds.SoundEvent sound,SoundSource category,float volume,float pitch){
        var at=LbcFpvDrone.viewpoint(player);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound),category,at.x,at.y,at.z,volume,pitch,0));
    }
    private static void clickSound(ServerPlayer player,int kind) {
        notifySound(player,SoundEvents.UI_BUTTON_CLICK.value(),kind==3?SoundSource.MASTER:SoundSource.PLAYERS,.5f,1);
    }
}
