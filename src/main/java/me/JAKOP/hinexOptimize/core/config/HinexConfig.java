package me.JAKOP.hinexOptimize.core.config;

import me.JAKOP.hinexOptimize.HinexOptimize;
import me.JAKOP.hinexOptimize.platform.Platform;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Lithium-style switches stored in {@code config/hinexoptimize.properties}.
 * Loaded once, before mixins are applied, so changes need a restart.
 *
 * <p>For benchmarking, values can be overridden without touching the file:
 * {@code -Dhinexoptimize.option.<key>=true|false}, or {@code -Dhinexoptimize.vanilla=true}
 * to switch off every optimization while keeping the core and benchmark hooks.
 */
public final class HinexConfig {
    public static final String FILE_NAME = "hinexoptimize.properties";
    public static final String MIXIN_PREFIX = "mixin";

    // Every option is declared here; features add their own entries.
    private static final List<Option> OPTIONS = List.of(
        Option.safe("mixin", "Master switch for all HinexOptimize mixins"),
        Option.safe("mixin.core", "Core hooks used by the other features"),
        Option.safe("mixin.bench", "Frame times, draw counters and compile timings for /hinexbench (negligible overhead)")
    );

    // Options that are not optimizations; vanilla mode keeps them on
    private static final Set<String> INFRASTRUCTURE = Set.of("mixin", "mixin.core", "mixin.bench");

    private static HinexConfig instance;

    private final Map<String, Boolean> values = new LinkedHashMap<>();

    private HinexConfig() {
        for (Option option : OPTIONS) {
            values.put(option.key(), option.defaultValue());
        }
    }

    public static synchronized HinexConfig get() {
        if (instance == null) {
            instance = load(Platform.INSTANCE.configDir().resolve(FILE_NAME));
        }
        return instance;
    }

    public List<Option> options() {
        return OPTIONS;
    }

    public boolean isEnabled(String key) {
        return values.getOrDefault(key, true);
    }

    public long enabledCount() {
        return values.values().stream().filter(Boolean::booleanValue).count();
    }

    /** Keys of declared options that are currently off, for benchmark reports. */
    public List<String> disabledKeys() {
        return values.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).toList();
    }

    public boolean isVanillaMode() {
        return Boolean.getBoolean("hinexoptimize.vanilla");
    }

    /**
     * A mixin is applied only if every declared option along its package path is enabled,
     * e.g. {@code mixin}, {@code mixin.vulkan}, {@code mixin.vulkan.state} for
     * {@code <root>.mixin.vulkan.state.FooMixin}.
     */
    public boolean isMixinEnabled(String mixinPackagePath) {
        String key = MIXIN_PREFIX;
        if (!isEnabled(key)) {
            return false;
        }
        if (mixinPackagePath.isEmpty()) {
            return true;
        }
        for (String part : mixinPackagePath.split("\\.")) {
            key = key + "." + part;
            if (!isEnabled(key)) {
                return false;
            }
        }
        return true;
    }

    private static HinexConfig load(Path file) {
        HinexConfig config = new HinexConfig();
        if (Files.exists(file)) {
            Properties properties = new Properties();
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                // Editors like Notepad may prepend a BOM, which Properties would read as part of a key
                if (text.startsWith("﻿")) {
                    text = text.substring(1);
                }
                properties.load(new StringReader(text));
            } catch (IOException | IllegalArgumentException e) {
                HinexOptimize.LOGGER.error("Could not read {}, using defaults", file, e);
                return config;
            }
            for (String key : properties.stringPropertyNames()) {
                String raw = properties.getProperty(key).trim();
                if (!config.values.containsKey(key)) {
                    HinexOptimize.LOGGER.warn("Unknown option '{}' in {}", key, file.getFileName());
                } else if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) {
                    config.values.put(key, Boolean.parseBoolean(raw));
                } else {
                    HinexOptimize.LOGGER.warn("Option '{}' must be true or false, got '{}'", key, raw);
                }
            }
        }
        config.save(file);
        // Applied after saving so command-line overrides never end up in the file
        config.applySystemOverrides();
        return config;
    }

    private void applySystemOverrides() {
        boolean vanilla = isVanillaMode();
        for (Option option : OPTIONS) {
            if (vanilla && !INFRASTRUCTURE.contains(option.key())) {
                values.put(option.key(), false);
            }
            String override = System.getProperty("hinexoptimize.option." + option.key());
            if (override != null) {
                values.put(option.key(), Boolean.parseBoolean(override.trim()));
            }
        }
        if (vanilla) {
            HinexOptimize.LOGGER.info("Vanilla mode: all optimizations disabled for this run");
        }
    }

    /** Rewrites the file with every option, keeping user values; written atomically. */
    private void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                writer.write("# HinexOptimize options. Restart the game after changing them.\n");
                writer.write("# Options marked [risky] may change behaviour compared to vanilla.\n\n");
                for (Option option : OPTIONS) {
                    writer.write("# " + option.description() + (option.risky() ? " [risky]" : "")
                        + " (default: " + option.defaultValue() + ")\n");
                    writer.write(option.key() + "=" + values.get(option.key()) + "\n\n");
                }
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            HinexOptimize.LOGGER.warn("Could not write {}", file, e);
        }
    }
}
