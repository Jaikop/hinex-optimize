package me.JAKOP.hinexOptimize.core.config;

public record IntOption(String key, int defaultValue, int min, int max, String description) {
    public IntOption {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default of " + key + " is outside its range");
        }
    }

    public int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }
}
