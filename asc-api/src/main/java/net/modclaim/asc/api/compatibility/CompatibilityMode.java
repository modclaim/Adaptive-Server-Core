package net.modclaim.asc.api.compatibility;

/**
 * Operating mode determining whether ASC runs full native hooks, reduced feature sets, or degraded fallbacks.
 */
public enum CompatibilityMode {
    FULL("Full native optimizations active"),
    COMPATIBILITY_REDUCED("Active with overlapping features disabled to prevent conflicts"),
    DEGRADED("Running in degraded mode due to unsupported platform features");

    private final String description;

    CompatibilityMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
