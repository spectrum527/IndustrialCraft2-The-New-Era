package com.industrialage.ia2.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-authoritative one-shot sounds and client-owned loops tied to a loaded machine position.
 */
public final class IndustrialSounds {
    interface Observer {
        void tick(Level level, BlockPos pos, BlockState state);
    }

    private static Observer listener;

    private IndustrialSounds() {}

    static void install(Observer observer) {
        listener = observer;
    }

    /** Plays one registered IC2 sound on all nearby clients. The path is unqualified. */
    public static void play(ServerLevel level, BlockPos pos, String path) {
        level.playSound(null, pos, SoundPlatform.event(path), SoundSource.BLOCKS, 0.65F, 1F);
    }

    /**
     * Reconciles an active machine's loop on the physical client. Safe to invoke from a shared
     * ticker.
     */
    public static void observe(Level level, BlockPos pos, BlockState state) {
        if (listener != null && !(level instanceof ServerLevel)) listener.tick(level, pos, state);
    }

    static String loop(String machine) {
        return switch (machine) {
            case "generator" -> "generator.generator.loop";
            case "geo_generator" -> "generator.geothermal.loop";
            case "nuclear_reactor" -> "generator.nuclear.loop";
            case "water_generator", "water_kinetic_generator" -> "generator.water.loop";
            case "wind_generator", "wind_kinetic_generator" -> "generator.wind.loop";
            case "macerator" -> "machine.macerator.operate";
            case "compressor" -> "machine.compressor.operate";
            case "extractor" -> "machine.extractor.operate";
            case "electric_furnace" -> "machine.furnace.electric.loop";
            case "induction_furnace" -> "machine.furnace.induction.loop";
            case "iron_furnace" -> "machine.furnace.iron.operate";
            case "electrolyzer", "classic_electrolyzer" -> "machine.electrolyzer.loop";
            case "canner", "classic_canner", "solid_canner", "fluid_bottler" ->
                    "machine.canner.operate";
            case "mass_fabricator", "classic_mass_fabricator", "matter_generator" ->
                    "machine.fabricator.loop";
            case "miner", "advanced_miner" -> "machine.miner.operate";
            case "recycler" -> "machine.recycler.operate";
            case "pump" -> "machine.pump.operate";
            case "terraformer" -> "machine.terraformer.loop";
            default -> null;
        };
    }
}
