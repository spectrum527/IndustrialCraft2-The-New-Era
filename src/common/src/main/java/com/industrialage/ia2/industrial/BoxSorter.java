package com.industrialage.ia2.industrial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * Pure, deterministic inventory stack merger and sorter shared across storage boxes and safes.
 *
 * <p>Guarantees zero item loss or duplication while respecting both per-item and container stack
 * limits.
 */
public final class BoxSorter {
    private BoxSorter() {}

    /**
     * Deterministic sort key for a stack: item registry ID (ascending), damage value (ascending),
     * display name (ascending), component signature (ascending), and stack count (descending).
     */
    public record SortKey(
            String itemId, int damage, String displayName, String components, int count)
            implements Comparable<SortKey> {
        /** Normalizes nullable text components so comparisons are always total and null-safe. */
        public SortKey {
            itemId = itemId == null ? "" : itemId;
            displayName = displayName == null ? "" : displayName;
            components = components == null ? "" : components;
        }

        @Override
        public int compareTo(SortKey other) {
            int cmp = itemId.compareTo(other.itemId);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(damage, other.damage);
            if (cmp != 0) return cmp;
            cmp = displayName.compareTo(other.displayName);
            if (cmp != 0) return cmp;
            cmp = components.compareTo(other.components);
            if (cmp != 0) return cmp;
            return Integer.compare(other.count, count);
        }
    }

    /** Adapter connecting {@link BoxSorter} to a concrete stack representation. */
    public interface StackOps<T> {
        /** Returns whether the stack represents an empty slot. */
        boolean isEmpty(T stack);

        /** Returns the item count currently held in the stack. */
        int count(T stack);

        /** Returns the stack's intrinsic maximum stack size. */
        int maxStackSize(T stack);

        /** Returns whether two non-empty stacks can merge into one stack. */
        boolean sameItem(T left, T right);

        /** Creates an independent copy of {@code stack} with {@code count} items. */
        T copyWithCount(T stack, int count);

        /** Updates the item count of a merged stack in place. */
        void setCount(T stack, int count);

        /** Extracts the deterministic sort key for a merged non-empty stack. */
        SortKey sortKey(T stack);
    }

    /**
     * Merges compatible non-empty stacks up to {@code min(containerMaxStackSize,
     * stack.maxStackSize)} and sorts them deterministically by {@link SortKey}.
     *
     * @param stacks raw slot contents to consolidate (empty elements are ignored, inputs are not
     *     mutated)
     * @param containerMaxStackSize upper stack limit imposed by the target container
     * @param ops stack adapter operations
     * @param <T> concrete stack type
     * @return newly created, merged and deterministically ordered non-empty stacks
     */
    public static <T> List<T> mergeAndSort(
            List<T> stacks, int containerMaxStackSize, StackOps<T> ops) {
        Objects.requireNonNull(stacks, "stacks");
        Objects.requireNonNull(ops, "ops");
        int containerLimit = Math.max(1, containerMaxStackSize);

        List<T> merged = new ArrayList<>();
        for (T stack : stacks) {
            if (stack == null || ops.isEmpty(stack)) continue;
            int remaining = ops.count(stack);
            if (remaining <= 0) continue;

            int limit = Math.max(1, Math.min(containerLimit, ops.maxStackSize(stack)));
            if (remaining > limit) {
                // Preserve any pre-existing over-stacked entry intact so slot count never expands.
                merged.add(ops.copyWithCount(stack, remaining));
                continue;
            }

            for (int i = 0; i < merged.size() && remaining > 0; i++) {
                T existing = merged.get(i);
                if (!ops.sameItem(existing, stack)) continue;
                int existingCount = ops.count(existing);
                int space = limit - existingCount;
                if (space <= 0) continue;
                int moved = Math.min(space, remaining);
                ops.setCount(existing, existingCount + moved);
                remaining -= moved;
            }
            if (remaining > 0) {
                merged.add(ops.copyWithCount(stack, remaining));
            }
        }

        List<Entry<T>> entries = new ArrayList<>(merged.size());
        for (T stack : merged) {
            entries.add(new Entry<>(stack, ops.sortKey(stack)));
        }
        entries.sort((a, b) -> a.key().compareTo(b.key()));

        List<T> result = new ArrayList<>(entries.size());
        for (Entry<T> entry : entries) {
            result.add(entry.stack());
        }
        return result;
    }

    /** Convenience overload constructing {@link StackOps} from functional callbacks. */
    public static <T> StackOps<T> ops(
            java.util.function.Predicate<T> isEmpty,
            ToIntFunction<T> count,
            ToIntFunction<T> maxStackSize,
            BiPredicate<T, T> sameItem,
            BiFunction<T, Integer, T> copyWithCount,
            ObjIntConsumer<T> setCount,
            Function<T, SortKey> sortKey) {
        return new StackOps<>() {
            @Override
            public boolean isEmpty(T stack) {
                return isEmpty.test(stack);
            }

            @Override
            public int count(T stack) {
                return count.applyAsInt(stack);
            }

            @Override
            public int maxStackSize(T stack) {
                return maxStackSize.applyAsInt(stack);
            }

            @Override
            public boolean sameItem(T left, T right) {
                return sameItem.test(left, right);
            }

            @Override
            public T copyWithCount(T stack, int newCount) {
                return copyWithCount.apply(stack, newCount);
            }

            @Override
            public void setCount(T stack, int newCount) {
                setCount.accept(stack, newCount);
            }

            @Override
            public SortKey sortKey(T stack) {
                return sortKey.apply(stack);
            }
        };
    }

    private record Entry<T>(T stack, SortKey key) {}
}
