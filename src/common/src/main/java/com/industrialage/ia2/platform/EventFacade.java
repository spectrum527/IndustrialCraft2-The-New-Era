package com.industrialage.ia2.platform;

import java.util.function.Consumer;

/**
 * Synchronous event streams selected by explicit type tokens.
 *
 * <p>Subscriptions, publication, and closure for each stream run on the same host thread.
 */
public interface EventFacade {
    /**
     * Appends a callback to the stream for the given type token.
     *
     * <p>A callback added during a dispatch can participate in later dispatches, including
     * reentrant publication, and is excluded from the dispatch already in progress.
     *
     * @param type exact stream token
     * @param callback callback invoked with published values
     * @param <E> event value type
     * @return handle for removing this subscription
     * @throws NullPointerException if any argument is null
     * @throws IllegalStateException if called outside the stream's host thread
     */
    <E> Subscription subscribe(Class<E> type, Consumer<? super E> callback);

    /**
     * Publishes a value synchronously on the stream selected by the exact type token.
     *
     * <p>The dispatch visits subscriptions present at its start in subscription order. It skips
     * subscriptions closed before their callback runs. A reentrant publish starts a separate
     * dispatch using the current subscriptions. A stream without subscriptions completes normally.
     *
     * <p>A callback exception terminates the current dispatch and propagates to the caller.
     * Subscriptions remain active unless explicitly closed.
     *
     * @param type exact stream token
     * @param event event value for this stream
     * @param <E> event value type
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the value is incompatible with the type token
     * @throws IllegalStateException if called outside the stream's host thread
     */
    <E> void publish(Class<E> type, E event);

    /** A removable subscription to a single event stream. */
    @FunctionalInterface
    interface Subscription extends AutoCloseable {
        /**
         * Removes the subscription immediately, even during a dispatch. Repeated calls have no
         * effect.
         *
         * @throws IllegalStateException if called outside the stream's host thread
         */
        @Override
        void close();
    }
}
