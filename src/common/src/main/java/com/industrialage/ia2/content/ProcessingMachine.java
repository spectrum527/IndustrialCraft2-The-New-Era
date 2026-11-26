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

    /** One recipe. The host supplies the current smelting recipe for an electric furnace. */
    public record Recipe(String input, int inputCount, String output, int outputCount) {
        /** Requires an identifiable input and output with positive stack counts. */
        public Recipe {
            Objects.requireNonNull(input);
            Objects.requireNonNull(output);
            if (input.isEmpty() || output.isEmpty() || inputCount < 1 || outputCount < 1) {
                throw new IllegalArgumentException("Invalid processing recipe");
            }
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
     * ({@code c:}/{@code forge:}, {@code raw_ores}/{@code raw_materials}, and {@code blocks}/{@code
     * storage_blocks}).
     */
    public static boolean matchesTagAlias(String tag, Predicate<String> matchesTag) {
        return ProcessingRecipes.matchesTagAlias(tag, matchesTag);
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
                                        outputSpace / recipe.outputCount()));
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
                        && outputSpace - operations * recipe.outputCount() >= recipe.outputCount();
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
