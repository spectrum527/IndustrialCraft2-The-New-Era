package com.industrialage.ia2.platform;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Routes deferred initialization according to the physical side selected by the adapter. */
@FunctionalInterface
public interface ClientFacade {
    /**
     * Constructs and runs an action once during the physical client's initialization window. Only
     * client-side adapter code may construct an action that links client classes.
     *
     * @param actionFactory deferred source of the initialization action
     * @return true after successful execution, or false on a dedicated server
     * @throws NullPointerException if the factory is null
     * @throws IllegalStateException if the window is closed or initialization fails
     */
    boolean initialize(Supplier<? extends Runnable> actionFactory);

    /** {@return a dedicated-server facade that never evaluates or retains the action factory} */
    static ClientFacade dedicatedServer() {
        return factory -> {
            Objects.requireNonNull(factory, "actionFactory");
            return false;
        };
    }

    /**
     * Creates a facade for the physical client.
     *
     * @param initializationOpen adapter check for the current client initialization window
     * @return facade that checks the window before constructing the action
     */
    static ClientFacade physicalClient(BooleanSupplier initializationOpen) {
        Objects.requireNonNull(initializationOpen, "initializationOpen");
        return factory -> {
            Objects.requireNonNull(factory, "actionFactory");
            if (!initializationOpen.getAsBoolean()) {
                throw new IllegalStateException("Client initialization window is closed");
            }
            try {
                Objects.requireNonNull(factory.get(), "action").run();
            } catch (RuntimeException failure) {
                throw new IllegalStateException("Client initialization failed", failure);
            }
            return true;
        };
    }
}
