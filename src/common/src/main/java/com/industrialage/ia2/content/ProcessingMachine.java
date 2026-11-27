package com.industrialage.ia2.content;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Tracks a single-input electric machine. The host owns item stacks and commits each finished job.
 */
public final class ProcessingMachine {
    private final int baseDuration;
    private final int baseEnergy;
    private final int reserve;
    private ProcessingUpgrades upgrades = ProcessingUpgrades.NONE;
    private ProcessingUpgrades.Stats stats;
    private int completedOperations;
    private double energy;
    private int progress;
    private String recipeKey = "";
    private boolean active;

    /** Constructs a processor for an unqualified IC2 registry path. */
    public ProcessingMachine(String machinePath) {
        switch (machinePath) {
            case "macerator", "compressor", "extractor" -> {
                baseDuration = 300;
                baseEnergy = 2;
                reserve = 600;
            }
            case "electric_furnace" -> {
                baseDuration = 100;
                baseEnergy = 3;
                reserve = 300;
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unknown electric processor: " + machinePath);
        }
        stats = upgrades.stats(baseDuration, baseEnergy, reserve, 1);
    }

    /** Recalculates speed, throughput and EU capacity while retaining the paid progress ratio. */
    public boolean setUpgrades(ProcessingUpgrades installed) {
        if (upgrades.equals(installed)) return false;
        int oldDuration = stats.duration();
        upgrades = Objects.requireNonNull(installed);
        stats = upgrades.stats(baseDuration, baseEnergy, reserve, 1);
        progress = ProcessingUpgrades.progress(progress, oldDuration, stats.duration());
        return true;
    }

    /** Supplies the current operation parameters to the inventory and EU host. */
    public ProcessingUpgrades.Stats stats() {
        return stats;
    }

    /** Number of operations authorized by the current successful tick. */
    public int completedOperations() {
        return completedOperations;
    }

    /** One weighted output stack count entry for probabilistic processing recipes (e.g. ores). */
    public record WeightedCount(int count, int weight) {
        /** Requires a positive stack count and positive selection weight. */
        public WeightedCount {
            if (count < 1 || weight < 1) {
                throw new IllegalArgumentException("Invalid weighted count");
            }
        }
    }

    /** One recipe. The host supplies the current smelting recipe for an electric furnace. */
    public record Recipe(
            String input,
            int inputCount,
            String output,
            int outputCount,
            java.util.List<WeightedCount> weightedCounts) {
        /** Requires an identifiable input and output with positive stack counts. */
        public Recipe {
            Objects.requireNonNull(input);
            Objects.requireNonNull(output);
            weightedCounts =
                    weightedCounts == null
                            ? java.util.List.of()
                            : java.util.List.copyOf(weightedCounts);
            if (input.isEmpty() || output.isEmpty() || inputCount < 1 || outputCount < 1) {
                throw new IllegalArgumentException("Invalid processing recipe");
            }
        }

        /** Deterministic single-output recipe without weighted count distribution. */
        public Recipe(String input, int inputCount, String output, int outputCount) {
            this(input, inputCount, output, outputCount, java.util.List.of());
        }

        /** Maximum output count one operation can produce (used for safe output-slot checks). */
        public int maxOutputCount() {
            int max = outputCount;
            for (var entry : weightedCounts) {
                if (entry.count() > max) {
                    max = entry.count();
                }
            }
            return max;
        }

        /** Rolls total items produced across {@code operations} completed runs. */
        public int rollOutputCount(int operations, java.util.function.IntUnaryOperator nextInt) {
            if (weightedCounts.isEmpty()) {
                return outputCount * operations;
            }
            int totalWeight = 0;
            for (var entry : weightedCounts) {
                totalWeight += entry.weight();
            }
            int produced = 0;
            for (int op = 0; op < operations; op++) {
                int roll = nextInt.applyAsInt(totalWeight);
                int chosen = outputCount;
                for (var entry : weightedCounts) {
                    roll -= entry.weight();
                    if (roll < 0) {
                        chosen = entry.count();
                        break;
                    }
                }
                produced += chosen;
            }
            return produced;
        }

        private String identity() {
            return input + "/" + inputCount + ">" + output + "/" + outputCount;
        }
    }

    /** Finds an intrinsic recipe, or returns null if the input has no recipe on this machine. */
    public static Recipe recipe(String machinePath, String fullInputId) {
        return ProcessingRecipes.BUILTIN.find(machinePath, fullInputId, null);
    }

    /**
     * Resolves tag ingredients against the host's current datapack when a predicate is supplied.
     */
    public static Recipe recipe(
            String machinePath, String fullInputId, Predicate<String> matchesTag) {
        return ProcessingRecipes.BUILTIN.find(machinePath, fullInputId, matchesTag);
    }

    /** Returns all built-in recipes registered for {@code machinePath} (direct and tag-backed). */
    public static java.util.List<Recipe> allRecipes(String machinePath) {
        return ProcessingRecipes.BUILTIN.all(machinePath);
    }

    /**
     * Evaluates {@code matchesTag} against {@code tag} and standard cross-loader tag aliases
     * ({@code c:}/{@code forge:}, multi-way wrench/shears tags, all vanilla ore tags, and 1.21+
     * common tag renames).
     */
    public static boolean matchesTagAlias(String tag, Predicate<String> matchesTag) {
        return ProcessingRecipes.matchesTagAlias(tag, matchesTag);
    }

    /**
     * Returns the ordered candidate tag IDs expanded from {@code tag} for cross-loader matching.
     */
    public static java.util.List<String> tagAliasCandidates(String tag) {
        return ProcessingRecipes.tagAliasCandidates(tag);
    }

    /**
     * Advances from the live input and space. A completion authorizes one atomic inventory
     * transaction containing {@link #completedOperations()} operations.
     */
    public boolean tick(Recipe recipe, int availableInput, int outputSpace) {
        active = false;
        completedOperations = 0;
        int operations =
                recipe == null
                        ? 0
                        : Math.min(
                                stats.operations(),
                                Math.min(
                                        availableInput / recipe.inputCount(),
                                        outputSpace / recipe.maxOutputCount()));
        if (operations <= 0) {
            progress = 0;
            recipeKey = "";
            return false;
        }
        String nextKey = recipe.identity();
        if (!nextKey.equals(recipeKey)) {
            recipeKey = nextKey;
            progress = 0;
        }
        int cost = stats.energyPerTick();
        if (energy < cost) return false;
        energy -= cost;
        active = true;
        if (++progress < stats.duration()) return false;
        completedOperations = operations;
        progress = 0;
        recipeKey = "";
        active =
                energy >= cost
                        && availableInput - operations * recipe.inputCount() >= recipe.inputCount()
                        && outputSpace - operations * recipe.maxOutputCount()
                                >= recipe.maxOutputCount();
        return true;
    }

    /** Accepts up to the remaining capacity and returns the EU actually stored. */
    public double receive(double eu) {
        if (!Double.isFinite(eu) || eu <= 0) {
            return 0;
        }
        double accepted = Math.min(eu, Math.max(0, stats.capacity() - energy));
        energy += accepted;
        return accepted;
    }

    /** Current buffer in EU. */
    public double energy() {
        return energy;
    }

    /** Number of paid working ticks on the current input. */
    public int progress() {
        return progress;
    }

    /** Whether the machine is running after the latest tick. */
    public boolean active() {
        return active;
    }

    /** Paid working ticks per operation. */
    public int duration() {
        return stats.duration();
    }

    /** Maximum buffer in EU. */
    public double capacity() {
        return stats.capacity();
    }

    /** Identity of the input and result for which the current progress was earned. */
    public String recipeKey() {
        return recipeKey;
    }

    /** Restores an in-flight operation written before persisted upgrade timings were added. */
    public void restore(double stored, int ticks, String key) {
        restore(stored, ticks, key, baseDuration);
    }

    /** Restores the saved ratio under the currently installed upgrades. */
    public void restore(double stored, int ticks, String key, int savedDuration) {
        int duration = savedDuration > 0 ? savedDuration : baseDuration;
        if (!Double.isFinite(stored)
                || stored < 0
                || ticks < 0
                || ticks >= duration
                || (ticks > 0 && (key == null || key.isEmpty()))) {
            throw new IllegalArgumentException("Invalid saved processor state");
        }
        energy = stored;
        progress = ProcessingUpgrades.progress(ticks, duration, stats.duration());
        recipeKey = ticks == 0 ? "" : key;
        completedOperations = 0;
        active = false;
    }
}
