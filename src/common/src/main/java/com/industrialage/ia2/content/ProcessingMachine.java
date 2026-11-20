package com.industrialage.ia2.content;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Tracks a single-input electric machine. The host owns item stacks and commits each finished job.
 */
public final class ProcessingMachine {
    private final int baseDuration;
    private final int energyPerTick;
    private final int capacity;
    private double energy;
    private int progress;
    private String recipeKey = "";
    private boolean active;

    /** Constructs a processor for an unqualified IC2 registry path. */
    public ProcessingMachine(String machinePath) {
        switch (machinePath) {
            case "macerator", "compressor", "extractor" -> {
                baseDuration = 300;
                energyPerTick = 2;
                capacity = 1200;
            }
            case "electric_furnace" -> {
                baseDuration = 100;
                energyPerTick = 3;
                capacity = 600;
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unknown electric processor: " + machinePath);
        }
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

    /**
     * Advances from the live input and the space available for this particular result. A true
     * return authorizes exactly one inventory transaction on this same server tick.
     */
    public boolean tick(Recipe recipe, int availableInput, int outputSpace) {
        active = false;
        if (recipe == null
                || availableInput < recipe.inputCount()
                || outputSpace < recipe.outputCount()) {
            progress = 0;
            recipeKey = "";
            return false;
        }
        String nextKey = recipe.identity();
        if (!nextKey.equals(recipeKey)) {
            recipeKey = nextKey;
            progress = 0;
        }
        if (energy < energyPerTick) {
            return false;
        }
        energy -= energyPerTick;
        active = true;
        if (++progress < baseDuration) {
            return false;
        }
        progress = 0;
        recipeKey = "";
        active =
                energy >= energyPerTick
                        && availableInput - recipe.inputCount() >= recipe.inputCount()
                        && outputSpace - recipe.outputCount() >= recipe.outputCount();
        return true;
    }

    /** Accepts up to the remaining capacity and returns the EU actually stored. */
    public double receive(double eu) {
        if (!Double.isFinite(eu) || eu <= 0) {
            return 0;
        }
        double accepted = Math.min(eu, Math.max(0, capacity - energy));
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
        return baseDuration;
    }

    /** Maximum buffer in EU. */
    public double capacity() {
        return capacity;
    }

    /** Identity of the input and result for which the current progress was earned. */
    public String recipeKey() {
        return recipeKey;
    }

    /** Restores the server-side buffer and in-flight recipe before its next tick. */
    public void restore(double stored, int ticks, String key) {
        if (!Double.isFinite(stored)
                || stored < 0
                || ticks < 0
                || ticks >= baseDuration
                || (ticks > 0 && (key == null || key.isEmpty()))) {
            throw new IllegalArgumentException("Invalid saved processor state");
        }
        energy = stored;
        progress = ticks;
        recipeKey = ticks == 0 ? "" : key;
        active = false;
    }
}
