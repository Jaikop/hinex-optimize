package me.JAKOP.hinexOptimize.core.config;

/**
 * A config switch. Mixin options use the key {@code mixin.<package path>}: disabling
 * {@code mixin.vulkan} disables every mixin under {@code ...mixin.vulkan}.
 *
 * @param risky true when the option may change behaviour compared to vanilla;
 *              such options must default to {@code false}
 */
public record Option(String key, boolean defaultValue, boolean risky, String description) {
    public Option {
        if (risky && defaultValue) {
            throw new IllegalArgumentException("Risky option " + key + " must be disabled by default");
        }
    }

    public static Option safe(String key, String description) {
        return new Option(key, true, false, description);
    }

    public static Option risky(String key, String description) {
        return new Option(key, false, true, description);
    }
}
