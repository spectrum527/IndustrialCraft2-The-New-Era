package com.industrialage.ia2.reactor;

import java.util.List;
import java.util.Set;

/** The IC2 electric reactor's component grid. Each cycle represents twenty loaded game ticks. */
public final class ElectricReactor {
    /** Fixed save-file row width; chambers expose additional columns on each of six rows. */
    public static final int STRIDE = 9;

    public static final int ROWS = 6;
    public static final int SLOTS = STRIDE * ROWS;
    private static final Set<String> REFLECTORS =
            Set.of("neutron_reflector", "thick_neutron_reflector", "iridium_neutron_reflector");
    private static final List<String> RODS =
            List.of(
                    "uranium_fuel_rod",
                    "dual_uranium_fuel_rod",
                    "quad_uranium_fuel_rod",
                    "mox_fuel_rod",
                    "dual_mox_fuel_rod",
                    "quad_mox_fuel_rod");

    /**
     * Inventory methods operate on the native stacks so components keep their names and custom
     * data.
     */
    public interface Grid {
        /** Unqualified IC2 component path at a fixed storage index, or the empty string. */
        String part(int slot);

        /** Returns fuel use, reflector wear, or stored component heat. */
        int wear(int slot);

        /** Native component count, used by stackable heating cells. */
        default int count(int slot) {
            return 1;
        }

        /** Updates only the machine-owned use field of the same stack. */
        void wear(int slot, int value);

        /**
         * Replaces one exhausted component with the result, or clears the slot for an empty path.
         */
        void replace(int slot, String result);
    }

    private int heat;
    private int vented;
    private double rate;
    private int maxHeat = 10_000;

    /** Effective number of columns provided by neighboring reactor chambers. */
    public static int width(int chambers) {
        return Math.max(3, Math.min(9, 3 + chambers));
    }

    /** Whether a fixed row-stride index is accessible with the given chamber count. */
    public static boolean exposed(int slot, int chambers) {
        return slot >= 0 && slot < SLOTS && slot % STRIDE < width(chambers);
    }

    /** Handles the energy, hull heat and installed cooling parts for one component cycle. */
    public void cycle(Grid grid, int chambers, boolean powered, double multiplier) {
        int width = width(chambers);
        vented = 0;
        int[] radiation = new int[SLOTS];
        maxHeat = 10_000;
        for (int s = 0; s < SLOTS; s++) {
            if (s % STRIDE >= width) continue;
            String id = grid.part(s);
            maxHeat +=
                    switch (id) {
                        case "reactor_plating" -> 1000;
                        case "reactor_heat_plating" -> 2000;
                        case "containment_reactor_plating" -> 500;
                        default -> 0;
                    };
            if (!powered || multiplier <= 0 || !RODS.contains(id)) continue;
            int cells = cells(id);
            int pulses = basePulses(cells);
            for (int n : adjacent(s, width)) {
                String part = grid.part(n);
                if (RODS.contains(part) || REFLECTORS.contains(part) || breeder(part)) pulses++;
                if (REFLECTORS.contains(part) || breeder(part)) radiation[n] += cells;
            }
            int generated = 2 * cells * pulses * (pulses + 1);
            heat += distributeHeat(grid, s, width, generated);
        }
        for (int s = 0; s < SLOTS; s++) {
            if (radiation[s] == 0) continue;
            String id = grid.part(s);
            if (breeder(id)) {
                if (id.equals("lithium_fuel_rod")) {
                    if (grid.wear(s) < 9999)
                        grid.wear(s, Math.min(9999, grid.wear(s) + radiation[s] * (heat / 3000)));
                } else {
                    int progress = grid.wear(s) + radiation[s] * (1 + heat / 3000);
                    if (progress >= 10_000) grid.replace(s, "re_enriched_uranium");
                    else grid.wear(s, progress);
                }
                continue;
            }
            if (id.equals("iridium_neutron_reflector")) continue;
            int wear = grid.wear(s) + radiation[s];
            int limit = id.equals("thick_neutron_reflector") ? 120_000 : 30_000;
            if (wear >= limit) grid.replace(s, "");
            else grid.wear(s, wear);
        }
        rate = 0;
        boolean[] spent = new boolean[SLOTS];
        for (int s = 0; s < SLOTS; s++) {
            if (s % STRIDE >= width) continue;
            String id = grid.part(s);
            if (powered && multiplier > 0 && RODS.contains(id)) {
                int cells = cells(id);
                int pulses = basePulses(cells);
                for (int n : adjacent(s, width)) {
                    String part = grid.part(n);
                    if (RODS.contains(part) || REFLECTORS.contains(part)) pulses++;
                }
                boolean mox = id.contains("mox");
                rate += 5.0 * cells * pulses * multiplier * (mox ? 1 + 4.0 * heat / maxHeat : 1);
                int wear = grid.wear(s) + 1;
                if (wear >= (mox ? 10_000 : 20_000)) spent[s] = true;
                else grid.wear(s, wear);
            }
            cool(grid, s, width, powered);
        }
        for (int s = 0; s < SLOTS; s++) {
            if (spent[s]) grid.replace(s, "depleted_" + grid.part(s));
        }
    }

    /** A component path is suitable for the reactor's slots on this electrical implementation. */
    public static boolean accepts(String id) {
        return RODS.contains(id)
                || REFLECTORS.contains(id)
                || heatCapacity(id) > 0
                || breeder(id)
                || id.equals("heatpack")
                || id.equals("component_heat_vent")
                || id.equals("reactor_plating")
                || id.equals("reactor_heat_plating")
                || id.equals("containment_reactor_plating");
    }

    private static boolean breeder(String id) {
        return id.equals("depleted_isotope_fuel_rod") || id.equals("lithium_fuel_rod");
    }

    /** Identifies parts whose use value stores heat. */
    public static boolean heatStore(String id) {
        return heatCapacity(id) > 0;
    }

    /** Heat removed by installed vents in the last twenty-tick cycle. */
    public int ventedHeat() {
        return vented;
    }

    /** Returns heat that the fluid cooling system could not remove to the hull. */
    public void returnHeat(int amount) {
        heat += Math.max(0, amount);
    }

    private static int cells(String id) {
        if (id.startsWith("quad_")) return 4;
        return id.startsWith("dual_") ? 2 : 1;
    }

    private static int basePulses(int cells) {
        return cells == 4 ? 3 : cells;
    }

    private static int[] adjacent(int s, int width) {
        int count = 0;
        int[] adjacent = new int[4];
        if (s % STRIDE > 0) adjacent[count++] = s - 1;
        if (s % STRIDE + 1 < width) adjacent[count++] = s + 1;
        if (s >= STRIDE) adjacent[count++] = s - STRIDE;
        if (s + STRIDE < SLOTS) adjacent[count++] = s + STRIDE;
        return java.util.Arrays.copyOf(adjacent, count);
    }

    private static int heatCapacity(String id) {
        return switch (id) {
            case "heat_vent", "advanced_heat_vent", "reactor_heat_vent", "overclocked_heat_vent" ->
                    1000;
            case "reactor_coolant_cell" -> 10_000;
            case "triple_reactor_coolant_cell" -> 30_000;
            case "sextuple_reactor_coolant_cell" -> 60_000;
            case "heat_exchanger" -> 2500;
            case "advanced_heat_exchanger" -> 10_000;
            case "component_heat_exchanger", "reactor_heat_exchanger" -> 5000;
            case "rsh_condensator" -> 20_000;
            case "lzh_condensator" -> 100_000;
            default -> 0;
        };
    }

    private static int distributeHeat(Grid grid, int slot, int width, int added) {
        int[] neighbors = adjacent(slot, width);
        int count = 0;
        for (int n : neighbors) if (heatCapacity(grid.part(n)) > 0) count++;
        for (int n : neighbors) {
            int capacity = heatCapacity(grid.part(n));
            if (capacity == 0) continue;
            int accepted = (added + count - 1) / count;
            int charged = grid.wear(n) + accepted;
            if (charged > capacity) grid.replace(n, "");
            else grid.wear(n, charged);
            added -= accepted;
            count--;
        }
        return added;
    }

    private static int balance(int stored, int capacity, int other, int otherCapacity, int limit) {
        if (limit == 0 || capacity <= 0 || otherCapacity <= 0) return 0;
        double level = (double) stored / capacity;
        double neighbor = (double) other / otherCapacity;
        if (level == neighbor) return 0;
        double share = (level + neighbor) * 0.5;
        int amount =
                Math.min(
                        limit,
                        Math.max(
                                1,
                                (int)
                                        Math.floor(
                                                (share < 0.01
                                                        ? 1
                                                        : (level > neighbor
                                                                        ? otherCapacity
                                                                        : capacity)
                                                                * share))));
        return level > neighbor
                ? Math.min(stored, Math.min(otherCapacity - other, amount))
                : -Math.min(other, Math.min(capacity - stored, amount));
    }

    private void exchange(Grid grid, int slot, int width, String id) {
        int cap = heatCapacity(id);
        int own = grid.wear(slot);
        int sideLimit =
                switch (id) {
                    case "heat_exchanger" -> 12;
                    case "advanced_heat_exchanger" -> 24;
                    case "component_heat_exchanger" -> 36;
                    default -> 0;
                };
        for (int other : adjacent(slot, width)) {
            String part = grid.part(other);
            int capacity = heatCapacity(part);
            if (capacity == 0 || part.endsWith("condensator")) continue;
            int previous = grid.wear(other);
            int sent = balance(own, cap, previous, capacity, sideLimit);
            if (sent != 0) {
                grid.wear(other, previous + sent);
                own -= sent;
            }
        }
        int hullLimit =
                switch (id) {
                    case "heat_exchanger" -> 4;
                    case "advanced_heat_exchanger" -> 8;
                    case "reactor_heat_exchanger" -> 72;
                    default -> 0;
                };
        int toHull = balance(own, cap, heat, maxHeat, hullLimit);
        heat += toHull;
        grid.wear(slot, own - toHull);
    }

    private void cool(Grid grid, int s, int width, boolean powered) {
        String id = grid.part(s);
        if (id.endsWith("heat_exchanger")) {
            exchange(grid, s, width, id);
            return;
        }
        if (powered && id.equals("heatpack")) {
            int count = grid.count(s);
            for (int n : adjacent(s, width)) {
                int cap = heatCapacity(grid.part(n));
                if (cap > 0
                        && !grid.part(n).endsWith("condensator")
                        && grid.wear(n) < Math.min(cap, count * 1000))
                    grid.wear(n, Math.min(cap, grid.wear(n) + count));
            }
            return;
        }
        if (id.equals("component_heat_vent")) {
            for (int n : adjacent(s, width)) {
                if (heatCapacity(grid.part(n)) > 0 && !grid.part(n).endsWith("condensator")) {
                    int removed = Math.min(4, grid.wear(n));
                    vented += removed;
                    grid.wear(n, grid.wear(n) - removed);
                }
            }
            return;
        }
        int cooling =
                switch (id) {
                    case "heat_vent" -> 6;
                    case "advanced_heat_vent" -> 12;
                    case "reactor_heat_vent" -> 5;
                    case "overclocked_heat_vent" -> 20;
                    default -> 0;
                };
        if (cooling == 0) return;
        int suction =
                id.equals("reactor_heat_vent") ? 5 : id.equals("overclocked_heat_vent") ? 36 : 0;
        int moved = Math.min(heat, suction);
        heat -= moved;
        int hot = grid.wear(s) + moved;
        if (hot > heatCapacity(id)) grid.replace(s, "");
        else {
            int removed = Math.min(hot, cooling);
            vented += removed;
            grid.wear(s, hot - removed);
        }
    }

    /** In-game meltdown radius before the server-configured cap. */
    public static float meltdownPower(Grid grid, int chambers) {
        float fuelContribution = 10;
        float attenuation = 1;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!exposed(slot, chambers)) continue;
            String part = grid.part(slot);
            if (RODS.contains(part)) fuelContribution += 2 * cells(part);
            attenuation *=
                    switch (part) {
                        case "reactor_plating" -> .95F;
                        case "reactor_heat_plating" -> .99F;
                        case "containment_reactor_plating" -> .9F;
                        default -> 1F;
                    };
        }
        return fuelContribution * attenuation;
    }

    /** Accumulated hull heat. */
    public int heat() {
        return heat;
    }

    /** Current continuous electrical generation in EU/t. */
    public double output() {
        return rate;
    }

    /** Maximum hull heat, including installed plating. */
    public int limit() {
        return maxHeat;
    }

    /** Restores measured hull conditions between server sessions. */
    public void restore(int storedHeat, double lastOutput) {
        if (storedHeat < 0 || !Double.isFinite(lastOutput) || lastOutput < 0)
            throw new IllegalArgumentException("Invalid reactor save");
        heat = storedHeat;
        rate = lastOutput;
    }
}
