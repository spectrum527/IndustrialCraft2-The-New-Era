package com.industrialage.ia2.power;

import java.util.List;

/** Local, transactional access to electricity already stored in a physical battery box. */
public interface WorldPowerStorage {
    /** Whole EU presently available for a direct consumer. */
    int availableStoredEnergy();

    /** Adds positive EU or removes negative EU only if the complete transaction fits. */
    boolean exchangeStoredEnergy(int delta, boolean simulate);

    /**
     * Share a teleporter debit fairly across adjacent boxes, leaving all untouched on a shortfall.
     */
    static boolean withdraw(List<WorldPowerStorage> stores, int requested) {
        if (requested <= 0 || stores.isEmpty()) return false;
        var available =
                stores.stream().mapToInt(WorldPowerStorage::availableStoredEnergy).toArray();
        long total = 0;
        for (int n : available) total += n;
        if (total < requested) return false;
        int[] debit = new int[available.length];
        int left = requested;
        while (left > 0) {
            int active = 0;
            for (int i = 0; i < available.length; i++) if (available[i] > debit[i]) active++;
            int share = Math.max(1, left / active);
            for (int i = 0; i < available.length && left > 0; i++) {
                int take = Math.min(left, Math.min(available[i] - debit[i], share));
                debit[i] += take;
                left -= take;
            }
        }
        for (int i = 0; i < debit.length; i++)
            if (debit[i] > 0 && !stores.get(i).exchangeStoredEnergy(-debit[i], true)) return false;
        for (int i = 0; i < debit.length; i++)
            if (debit[i] > 0 && !stores.get(i).exchangeStoredEnergy(-debit[i], false)) {
                for (int previous = i - 1; previous >= 0; previous--)
                    if (debit[previous] > 0)
                        stores.get(previous).exchangeStoredEnergy(debit[previous], false);
                return false;
            }
        return true;
    }
}
