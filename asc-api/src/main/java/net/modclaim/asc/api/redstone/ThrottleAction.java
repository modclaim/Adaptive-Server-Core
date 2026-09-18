package net.modclaim.asc.api.redstone;

/**
 * Throttle actions applied to suspicious or heavy mechanisms.
 */
public enum ThrottleAction {
    NONE,
    WARN,
    THROTTLE_PULSE,
    EXTEND_COOLDOWN,
    FREEZE
}
