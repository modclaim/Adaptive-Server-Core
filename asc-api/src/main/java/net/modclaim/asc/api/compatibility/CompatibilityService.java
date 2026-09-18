package net.modclaim.asc.api.compatibility;

import net.modclaim.asc.api.ASCService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Service managing external plugin detections, conflict resolutions, and feature shims.
 */
public interface CompatibilityService extends ASCService {

    /**
     * Gets the current operational compatibility mode.
     *
     * @return current mode
     */
    @NotNull
    CompatibilityMode getMode();

    /**
     * Retrieves the list of all detected integrations and conflicting plugins.
     *
     * @return list of detected plugins
     */
    @NotNull
    List<DetectedPlugin> getDetectedPlugins();

    /**
     * Checks if a specific plugin is detected and active.
     *
     * @param pluginName name of the plugin
     * @return true if detected
     */
    boolean isPluginDetected(@NotNull String pluginName);

    /**
     * Checks if a specific ASC feature is disabled by compatibility shims.
     *
     * @param featureKey feature name
     * @return true if disabled
     */
    boolean isFeatureSuppressed(@NotNull String featureKey);

    /**
     * Generates a diagnostic status report for `/asc diagnose`.
     *
     * @return formatted diagnostic report lines
     */
    @NotNull
    List<String> generateDiagnosticReport();
}
