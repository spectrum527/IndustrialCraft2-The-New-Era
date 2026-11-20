package com.industrialage.ia2.equipment;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Per-tick telemetry for positions that currently have an open EU reader. */
public final class MeterSampling {
    private static final int WINDOW = 20;
    private static final Map<ServerLevel, Map<BlockPos, Samples>> WORLDS = new WeakHashMap<>();

    private MeterSampling() {}

    /** Records the actual energy accepted by the sink and debited from the source. */
    public static void transfer(
            ServerLevel level,
            BlockPos source,
            BlockPos sink,
            double debit,
            double accepted,
            double packet) {
        var watched = WORLDS.get(level);
        if (watched == null) return;
        var emitter = watched.get(source);
        var receiver = watched.get(sink);
        int tier = tier(packet);
        if (emitter != null) emitter.add(0, debit, tier);
        if (receiver != null) receiver.add(accepted, 0, tier);
    }

    /** Records a conductor traversed by a successfully delivered EU packet. */
    public static void conductor(ServerLevel level, BlockPos pos, double amount, double packet) {
        var watched = WORLDS.get(level);
        if (watched == null) return;
        var samples = watched.get(pos);
        if (samples != null) samples.add(amount, amount, tier(packet));
    }

    /** Whether the current level needs telemetry work this tick. */
    public static boolean watching(ServerLevel level) {
        return WORLDS.containsKey(level);
    }

    /** Commits one sample at the end of the level tick, including ticks carrying zero EU. */
    public static void finishTick(ServerLevel level) {
        var watched = WORLDS.get(level);
        if (watched != null) watched.values().forEach(Samples::advance);
    }

    /** Starts measuring a selected endpoint when a reader opens. */
    public static Watch watch(ServerLevel level, BlockPos pos) {
        var immutable = pos.immutable();
        var samples =
                WORLDS.computeIfAbsent(level, ignored -> new HashMap<>())
                        .computeIfAbsent(immutable, ignored -> new Samples());
        samples.viewers++;
        return new Watch(level, immutable, samples);
    }

    private static int tier(double packet) {
        int tier = 1;
        for (double limit = 32; packet > limit && tier < 16; limit *= 4) tier++;
        return tier;
    }

    /** Sample count and summary for one measurement in its original units. */
    public record Reading(int count, double min, double max, double average) {}

    /** Lifetime of one open native meter menu. */
    public static final class Watch implements AutoCloseable {
        private final ServerLevel level;
        private final BlockPos position;
        private final Samples samples;
        private boolean closed;

        private Watch(ServerLevel level, BlockPos position, Samples samples) {
            this.level = level;
            this.position = position;
            this.samples = samples;
        }

        /** Calculates the rolling summary for input, output, gain, or voltage tier. */
        public Reading read(int mode) {
            return samples.read(mode);
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            var watched = WORLDS.get(level);
            if (--samples.viewers == 0 && watched != null) {
                watched.remove(position, samples);
                if (watched.isEmpty()) WORLDS.remove(level);
            }
        }
    }

    private static final class Samples {
        private final double[][] history = new double[4][WINDOW];
        private int viewers;
        private int cursor;
        private int count;
        private double in;
        private double out;
        private double voltage;

        void add(double accepted, double sent, double tier) {
            in += accepted;
            out += sent;
            voltage = Math.max(voltage, tier);
        }

        void advance() {
            history[0][cursor] = in;
            history[1][cursor] = out;
            history[2][cursor] = in - out;
            history[3][cursor] = voltage;
            cursor = (cursor + 1) % WINDOW;
            count = Math.min(count + 1, WINDOW);
            in = out = voltage = 0;
        }

        Reading read(int mode) {
            if (count == 0) return new Reading(0, 0, 0, 0);
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double total = 0;
            for (int sample = 0; sample < count; sample++) {
                double value = history[mode][sample];
                min = Math.min(min, value);
                max = Math.max(max, value);
                total += value;
            }
            return new Reading(count, min, max, total / count);
        }
    }
}
