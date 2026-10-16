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

public final class HinexConfig {
    public static final String FILE_NAME = "hinexoptimize.properties";
    public static final String MIXIN_PREFIX = "mixin";

    private static final List<Option> OPTIONS = List.of(
        Option.safe("mixin", "Master switch for all HinexOptimize mixins"),
        Option.safe("mixin.bench", "Frame times, draw counters and compile timings for /hinexbench (negligible overhead)"),
        Option.safe("mixin.render", "Rendering optimizations"),
        Option.safe("mixin.render.smoothview", "Change the render distance without rebuilding every chunk: only the added ring is built"),
        Option.risky("mixin.render.dynamicview", "Lower the rendered distance when FPS drops below dynamicview.target_fps and raise it back when there is headroom")
    );

    private static final List<IntOption> INT_OPTIONS = List.of(
        new IntOption("dynamicview.target_fps", 60, 20, 360, "Dynamic render distance: frame rate to keep"),
        new IntOption("dynamicview.min_distance", 6, 2, 32, "Dynamic render distance: never render fewer chunks than this"),
        new IntOption("dynamicview.max_reduction", 8, 1, 30, "Dynamic render distance: never go more than this many chunks below your setting")
    );

    private static final Set<String> INFRASTRUCTURE = Set.of("mixin", "mixin.bench");

    private static HinexConfig instance;

    private final Map<String, Boolean> values = new LinkedHashMap<>();
    private final Map<String, Integer> ints = new LinkedHashMap<>();

    private HinexConfig() {
        for (Option option : OPTIONS) {
            values.put(option.key(), option.defaultValue());
        }
        for (IntOption option : INT_OPTIONS) {
            ints.put(option.key(), option.defaultValue());
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

    public int getInt(String key) {
        Integer value = ints.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Undeclared setting " + key);
        }
        return value;
    }

    public long enabledCount() {
        return values.values().stream().filter(Boolean::booleanValue).count();
    }

    public List<String> disabledKeys() {
        return values.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).toList();
    }

    public boolean isVanillaMode() {
        return Boolean.getBoolean("hinexoptimize.vanilla");
    }

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
                IntOption intOption = intOption(key);
                if (intOption != null) {
                    config.setInt(intOption, raw, file.getFileName().toString());
                } else if (!config.values.containsKey(key)) {
                    HinexOptimize.LOGGER.warn("Unknown option '{}' in {}", key, file.getFileName());
                } else if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) {
                    config.values.put(key, Boolean.parseBoolean(raw));
                } else {
                    HinexOptimize.LOGGER.warn("Option '{}' must be true or false, got '{}'", key, raw);
                }
            }
        }
        config.save(file);
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
        for (IntOption option : INT_OPTIONS) {
            String override = System.getProperty("hinexoptimize.option." + option.key());
            if (override != null) {
                setInt(option, override.trim(), "system properties");
            }
        }
        if (vanilla) {
            HinexOptimize.LOGGER.info("Vanilla mode: all optimizations disabled for this run");
        }
    }

    private static IntOption intOption(String key) {
        for (IntOption option : INT_OPTIONS) {
            if (option.key().equals(key)) {
                return option;
            }
        }
        return null;
    }

    private void setInt(IntOption option, String raw, String source) {
        try {
            int value = Integer.parseInt(raw);
            int clamped = option.clamp(value);
            if (clamped != value) {
                HinexOptimize.LOGGER.warn("Setting '{}' = {} in {} is outside {}-{}, using {}", option.key(), value, source,
                    option.min(), option.max(), clamped);
            }
            ints.put(option.key(), clamped);
        } catch (NumberFormatException e) {
            HinexOptimize.LOGGER.warn("Setting '{}' in {} must be a whole number, got '{}'", option.key(), source, raw);
        }
    }

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
                for (IntOption option : INT_OPTIONS) {
                    writer.write("# " + option.description() + " (" + option.min() + "-" + option.max()
                        + ", default: " + option.defaultValue() + ")\n");
                    writer.write(option.key() + "=" + ints.get(option.key()) + "\n\n");
                }
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            HinexOptimize.LOGGER.warn("Could not write {}", file, e);
        }
    }
}
