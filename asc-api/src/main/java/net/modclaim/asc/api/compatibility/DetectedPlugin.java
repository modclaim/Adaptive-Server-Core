package net.modclaim.asc.api.compatibility;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Describes a third-party plugin detected on the server environment.
 */
public final class DetectedPlugin {
    private final String name;
    private final String version;
    private final boolean conflicting;
    private final String actionTaken;

    public DetectedPlugin(@NotNull String name, @NotNull String version, boolean conflicting, @NotNull String actionTaken) {
        this.name = Objects.requireNonNull(name, "name");
        this.version = Objects.requireNonNull(version, "version");
        this.conflicting = conflicting;
        this.actionTaken = Objects.requireNonNull(actionTaken, "actionTaken");
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isConflicting() {
        return conflicting;
    }

    public String getActionTaken() {
        return actionTaken;
    }
}
