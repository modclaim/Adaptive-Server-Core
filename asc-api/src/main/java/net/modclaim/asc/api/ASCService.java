package net.modclaim.asc.api;

/**
 * Base service contract for Adaptive Server Core modular components.
 */
public interface ASCService {

    /**
     * Initializes and enables the service.
     */
    void enable();

    /**
     * Gracefully disables the service, flushing pending state and releasing resources.
     */
    void disable();

    /**
     * Reloads configuration and refreshes runtime settings.
     */
    void reload();

    /**
     * Checks whether this service is currently operational.
     *
     * @return true if enabled and active
     */
    boolean isEnabled();

    /**
     * Returns the human-readable unique identifier of this service.
     *
     * @return service name
     */
    String getName();
}
