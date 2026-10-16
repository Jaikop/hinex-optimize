package me.JAKOP.hinexOptimize.core.config;

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
