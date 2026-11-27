package com.industrialage.ia2.content;

import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.platform.ConfigFacade;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pure recycler blacklist/whitelist evaluation shared across all loader implementations.
 *
 * <p>Consumes {@code balance/recyclerBlacklist} and {@code balance/recyclerWhitelist} from {@link
 * ActiveConfig}, supporting exact item IDs and {@code Tag:}/{@code #} tag prefixes with
 * cross-loader alias expansion via {@link ProcessingMachine#matchesTagAlias(String, Predicate)}.
 */
public final class RecyclerRules {
    /** Active server config key for excluded recycler inputs when the whitelist is empty. */
    public static final ConfigFacade.Key<List<String>> BLACKLIST =
            ActiveConfig.stringListKey("balance/recyclerBlacklist");

    /** Active server config key for explicit recycler inputs; takes precedence when non-empty. */
    public static final ConfigFacade.Key<List<String>> WHITELIST =
            ActiveConfig.stringListKey("balance/recyclerWhitelist");

    /** Default blacklist fallback used when no live server config snapshot is bound yet. */
    public static final List<String> DEFAULT_BLACKLIST =
            List.of(
                    "minecraft:glass_pane",
                    "minecraft:stick",
                    "minecraft:snowball",
                    "minecraft:snow",
                    "minecraft:snow_block",
                    "ic2:wooden_scaffold");

    private RecyclerRules() {}

    /**
     * Evaluates whether an item is excluded from producing scrap in the Recycler.
     *
     * <p>When {@code whitelist} is non-empty, only items matching at least one whitelist entry can
     * produce scrap (all other items are excluded). When {@code whitelist} is empty, an item is
     * excluded if it matches any {@code blacklist} entry or the built-in exclusion tags ({@code
     * ic2:recycler_blacklist}, {@code c:glass_panes}, and {@code c:rods/wooden}).
     */
    public static boolean isExcluded(
            String itemId,
            Predicate<String> matchesTag,
            List<String> blacklist,
            List<String> whitelist) {
        if (itemId == null || itemId.isEmpty()) {
            return true;
        }
        if (whitelist != null && !whitelist.isEmpty()) {
            return !matchesAny(itemId, matchesTag, whitelist);
        }
        if (matchesAny(itemId, matchesTag, blacklist)) {
            return true;
        }
        return matchesTag != null
                && (ProcessingMachine.matchesTagAlias("ic2:recycler_blacklist", matchesTag)
                        || ProcessingMachine.matchesTagAlias("c:glass_panes", matchesTag)
                        || ProcessingMachine.matchesTagAlias("c:rods/wooden", matchesTag));
    }

    /**
     * Checks whether {@code itemId} (and its tags via {@code matchesTag}) matches one configured
     * {@code recipe_inputs} rule entry. Supports exact item IDs, {@code Tag:<ns>:<path>}, and
     * {@code #<ns>:<path>}; ignores {@code Fluid:} entries.
     */
    public static boolean matchesEntry(String itemId, Predicate<String> matchesTag, String entry) {
        if (entry == null || entry.isEmpty() || itemId == null || itemId.isEmpty()) {
            return false;
        }
        if (entry.startsWith("Tag:")) {
            String tag = entry.substring(4);
            return matchesTag != null && ProcessingMachine.matchesTagAlias(tag, matchesTag);
        }
        if (entry.startsWith("#")) {
            String tag = entry.substring(1);
            return matchesTag != null && ProcessingMachine.matchesTagAlias(tag, matchesTag);
        }
        if (entry.startsWith("Fluid:")) {
            return false;
        }
        return itemId.equals(entry);
    }

    private static boolean matchesAny(
            String itemId, Predicate<String> matchesTag, List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (String entry : entries) {
            if (matchesEntry(itemId, matchesTag, entry)) {
                return true;
            }
        }
        return false;
    }
}
