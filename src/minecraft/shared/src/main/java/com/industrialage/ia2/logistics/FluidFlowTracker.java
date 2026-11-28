package com.industrialage.ia2.logistics;

import java.util.Arrays;
import net.minecraft.core.Direction;

/**
 * Tracks per-face fluid inflow/outflow states with short hold and reversal hysteresis so branched
 * pipes never flicker when downstream links are serviced in round-robin order.
 */
final class FluidFlowTracker {
    static final int NONE = PipeVisuals.FLOW_NONE;
    static final int IN = PipeVisuals.FLOW_IN;
    static final int OUT = PipeVisuals.FLOW_OUT;

    /** Ticks a confirmed face flow holds between round-robin turns (covers up to 5-way forks). */
    static final long HOLD_TICKS = 8L;

    /** Minimum silent or consecutive opposite ticks required before flipping an active face. */
    static final int REVERSE_CONFIRM_TICKS = 3;

    private static final int FACE_COUNT = TransportNode.SIDES.length;

    private final byte[] state = new byte[FACE_COUNT];
    private final long[] confirmedTick = new long[FACE_COUNT];
    private final long[] oppositeTick = new long[FACE_COUNT];
    private final byte[] oppositeStreak = new byte[FACE_COUNT];

    private long activeTick = Long.MIN_VALUE;
    private final int[] tickNet = new int[FACE_COUNT];
    private int tickDirtyBits;

    FluidFlowTracker() {
        Arrays.fill(confirmedTick, Long.MIN_VALUE);
        Arrays.fill(oppositeTick, Long.MIN_VALUE);
    }

    /** Records fluid entering ({@link #IN}) or leaving ({@link #OUT}) through one pipe face. */
    void record(Direction face, int flow, long tick, int amount) {
        if (face == null || amount <= 0 || (flow != IN && flow != OUT)) return;
        if (tickDirtyBits != 0 && tick != activeTick) {
            flushPending();
        }
        activeTick = tick;
        int idx = face.ordinal();
        int signed = flow == OUT ? amount : -amount;
        long sum = (long) tickNet[idx] + signed;
        tickNet[idx] = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, sum));
        tickDirtyBits |= 1 << idx;
    }

    /** Flushes pending transfers, expires stale faces, and returns the packed 12-bit flow mask. */
    int sampleMask(long tick) {
        if (tickDirtyBits != 0) {
            flushPending();
        }
        int mask = 0;
        for (int i = 0; i < FACE_COUNT; i++) {
            int current = state[i];
            if (current != NONE) {
                long last = confirmedTick[i];
                long age = tick - last;
                if (last == Long.MIN_VALUE || age > HOLD_TICKS || age < 0L) {
                    state[i] = NONE;
                    confirmedTick[i] = Long.MIN_VALUE;
                    oppositeTick[i] = Long.MIN_VALUE;
                    oppositeStreak[i] = 0;
                    continue;
                }
                mask |= current << (i * 2);
            }
        }
        return mask;
    }

    /** Clears all tracked face states, used when a pipe switches to a different fluid type. */
    void clear() {
        Arrays.fill(state, (byte) NONE);
        Arrays.fill(confirmedTick, Long.MIN_VALUE);
        Arrays.fill(oppositeTick, Long.MIN_VALUE);
        Arrays.fill(oppositeStreak, (byte) 0);
        Arrays.fill(tickNet, 0);
        tickDirtyBits = 0;
        activeTick = Long.MIN_VALUE;
    }

    private void flushPending() {
        long tick = activeTick;
        int dirty = tickDirtyBits;
        tickDirtyBits = 0;
        for (int i = 0; i < FACE_COUNT; i++) {
            if ((dirty & (1 << i)) == 0) continue;
            int net = tickNet[i];
            tickNet[i] = 0;
            if (net == 0) continue;
            byte observed = net > 0 ? (byte) OUT : (byte) IN;
            applyFace(i, observed, tick);
        }
    }

    private void applyFace(int idx, byte observed, long tick) {
        byte current = state[idx];
        long last = confirmedTick[idx];
        if (current == NONE || current == observed || last == Long.MIN_VALUE) {
            state[idx] = observed;
            confirmedTick[idx] = tick;
            oppositeTick[idx] = Long.MIN_VALUE;
            oppositeStreak[idx] = 0;
            return;
        }
        long age = tick - last;
        if (age < 0L || age >= REVERSE_CONFIRM_TICKS) {
            state[idx] = observed;
            confirmedTick[idx] = tick;
            oppositeTick[idx] = Long.MIN_VALUE;
            oppositeStreak[idx] = 0;
            return;
        }
        if (oppositeTick[idx] != tick) {
            oppositeTick[idx] = tick;
            int streak = oppositeStreak[idx] + 1;
            if (streak >= REVERSE_CONFIRM_TICKS) {
                state[idx] = observed;
                confirmedTick[idx] = tick;
                oppositeTick[idx] = Long.MIN_VALUE;
                oppositeStreak[idx] = 0;
            } else {
                oppositeStreak[idx] = (byte) streak;
            }
        }
    }

    Snapshot snapshot() {
        return new Snapshot(
                state.clone(),
                confirmedTick.clone(),
                oppositeTick.clone(),
                oppositeStreak.clone(),
                activeTick,
                tickNet.clone(),
                tickDirtyBits);
    }

    void restore(Snapshot snapshot) {
        System.arraycopy(snapshot.state, 0, state, 0, FACE_COUNT);
        System.arraycopy(snapshot.confirmedTick, 0, confirmedTick, 0, FACE_COUNT);
        System.arraycopy(snapshot.oppositeTick, 0, oppositeTick, 0, FACE_COUNT);
        System.arraycopy(snapshot.oppositeStreak, 0, oppositeStreak, 0, FACE_COUNT);
        activeTick = snapshot.activeTick;
        System.arraycopy(snapshot.tickNet, 0, tickNet, 0, FACE_COUNT);
        tickDirtyBits = snapshot.tickDirtyBits;
    }

    record Snapshot(
            byte[] state,
            long[] confirmedTick,
            long[] oppositeTick,
            byte[] oppositeStreak,
            long activeTick,
            int[] tickNet,
            int tickDirtyBits) {}
}
