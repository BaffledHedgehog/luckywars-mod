package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Exact source ownership; ordinary SWRG tags are separate from LBC selections. */
public final class LbcSourceUnlocks {
    private LbcSourceUnlocks() {}
    private static final Set<String> ADMINISTRATIVE=administrativeUniverse();
    private static Set<String> administrativeUniverse(){var result=new LinkedHashSet<>(universe());result.removeIf(id->id.startsWith("swrg:rewards/drop_"));return java.util.Collections.unmodifiableSet(result);}
    public static Set<String> administrativeIds(){return ADMINISTRATIVE;}

    /** Load only the finite impossible flags used by permanent case rewards. */
    public static boolean caseAdvancement(Identifier id) {
        return id.getNamespace().equals("swrg") && valid(id.toString());
    }

    static boolean valid(String key) {
        if (key == null) return false;
        for (String prefix : new String[]{"swrg.kit", "swrg.skill", "swrg.sound", "swrg.bars"}) {
            int max = switch (prefix) { case "swrg.kit" -> 20; case "swrg.skill" -> 11;
                case "swrg.sound" -> 13; default -> 20; };
            for (int n = 1; n <= max; n++) if (key.equals(prefix + n)) return true;
        }
        return key.matches("swrg:rewards/(?:suffix_(?:[1-9]|1[0-9]|2[0-6])|drop_[1-3](?:_requested)?|turnir/(?:trophy[1-3]|killer))");
    }

    static boolean has(ServerPlayer player, PlayerStore.Profile profile, String key) {
        if(profile!=null&&profile.revokedLegacyUnlocks.contains(key))return false;
        if (profile != null && (profile.legacyUnlocks.contains(key) || profile.lbcUnlocks.contains(key))) return true;
        if (player == null) return false;
        if (!key.contains(":")) return player.entityTags().contains(key);
        var advancement = player.level().getServer().getAdvancements().get(Identifier.parse(key));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** Call on join and before accepting an opening, then persist in its same transaction. */
    static boolean importLegacy(ServerPlayer player, PlayerStore.Profile profile) {
        boolean changed = false;
        for (String key : universe()) {
            if (key.matches("swrg:rewards/drop_[1-3]_requested")
                    && profile.dropRequestOverrides.contains(key.substring("swrg:rewards/drop_".length(), "swrg:rewards/drop_".length()+1))) continue;
            if (has(player, profile, key)) changed |= profile.legacyUnlocks.add(key);
        }
        return changed;
    }

    /** Projections are repaired after durable commit/rejoin, never used as the payout journal. */
    static void publish(ServerPlayer player, PlayerStore.Profile profile) {
        for(String key:profile.revokedLegacyUnlocks){
            if(!key.contains(":"))player.entityTags().remove(key);
            else{var flag=player.level().getServer().getAdvancements().get(Identifier.parse(key));if(flag!=null)for(String criterion:flag.value().criteria().keySet())player.getAdvancements().revoke(flag,criterion);}
        }
        for (String id : profile.dropRequestOverrides) {
            String key = LbcDropRequests.fulfilledKey(Integer.parseInt(id));
            if (profile.legacyUnlocks.contains(key)) continue;
            var advancement = player.level().getServer().getAdvancements().get(Identifier.parse(key));
            if (advancement != null) for (String criterion : advancement.value().criteria().keySet())
                player.getAdvancements().revoke(advancement, criterion);
        }
        for (String key : profile.legacyUnlocks) {
            if(profile.revokedLegacyUnlocks.contains(key))continue;
            if (!valid(key)) throw new IllegalStateException("Invalid source unlock " + key);
            if (!key.contains(":")) player.entityTags().add(key);
            else {
                var advancement = player.level().getServer().getAdvancements().get(Identifier.parse(key));
                if (advancement == null) throw new IllegalStateException("Missing source advancement " + key);
                for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria())
                    player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    private static Set<String> universe() {
        var keys = new LinkedHashSet<String>();
        for (int n=1;n<=20;n++) { keys.add("swrg.kit"+n); keys.add("swrg.bars"+n); }
        for (int n=1;n<=11;n++) keys.add("swrg.skill"+n);
        for (int n=1;n<=13;n++) keys.add("swrg.sound"+n);
        for (int n=1;n<=26;n++) keys.add("swrg:rewards/suffix_"+n);
        for (int n=1;n<=3;n++) { keys.add("swrg:rewards/drop_"+n); keys.add("swrg:rewards/drop_"+n+"_requested"); keys.add("swrg:rewards/turnir/trophy"+n); }
        keys.add("swrg:rewards/turnir/killer");
        return keys;
    }
}
