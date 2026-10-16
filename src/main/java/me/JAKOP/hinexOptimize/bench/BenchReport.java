package me.JAKOP.hinexOptimize.bench;

import me.JAKOP.hinexOptimize.HinexOptimize;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BenchReport {
    private static final String SUMMARY_FILE = "summary-v2.csv";
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private static final String[] COLUMNS = {
        "time", "tag", "scenario", "minecraft", "loader", "mod_version", "backend", "gpu", "driver", "disabled_options",
        "render_distance", "resolution", "seconds", "frames", "avg_fps", "low1_fps", "low01_fps",
        "p50_ms", "p95_ms", "p99_ms", "max_ms", "stutters_50ms",
        "draw_calls_pf", "draw_items_pf", "pipeline_binds_pf", "uniform_binds_pf", "descriptor_updates_pf", "buffer_views_pf",
        "lazy_compiles", "lazy_ms", "gc_count", "gc_ms",
        "startup_ms", "startup_glsl_count", "startup_glsl_ms", "startup_pipeline_count", "startup_pipeline_ms",
        "reload_ms", "reload_glsl_count", "reload_glsl_ms", "reload_pipeline_count", "reload_pipeline_ms",
        "section_compiles", "settle_ms", "max_settle_ms",
        "notes"
    };

    private BenchReport() {
    }

    public static Path write(Path gameDir, BenchResult result) throws IOException {
        Path dir = gameDir.resolve(HinexOptimize.MOD_ID).resolve("bench");
        Files.createDirectories(dir);

        BenchResult.Environment env = result.environment();
        String name = String.join("_", FILE_STAMP.format(result.time()), sanitize(env.minecraft()), env.loader(),
            sanitize(env.backend()), sanitize(result.tag()));
        Path framesFile = dir.resolve(name + "_frames.csv");
        try (Writer writer = Files.newBufferedWriter(framesFile, StandardCharsets.UTF_8)) {
            writer.write("frame,ms\n");
            long[] frames = result.frameNanos();
            for (int i = 0; i < frames.length; i++) {
                writer.write(i + "," + fmt(frames[i] / 1e6, 3) + "\n");
            }
        }

        Path summary = dir.resolve(SUMMARY_FILE);
        boolean newFile = !Files.exists(summary);
        try (Writer writer = Files.newBufferedWriter(summary, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (newFile) {
                writer.write(String.join(",", COLUMNS) + "\n");
            }
            writer.write(String.join(",", row(result)) + "\n");
        }
        return framesFile;
    }

    private static List<String> row(BenchResult r) {
        BenchResult.Environment env = r.environment();
        BenchResult.FrameStats stats = r.frameStats();
        BenchCounters.Snapshot m = r.measureCounters();
        BenchCounters.Snapshot startup = LoadTimes.startupCounters();
        BenchCounters.Snapshot reload = r.reloadCounters();
        double frames = Math.max(1, r.frames());

        List<String> row = new ArrayList<>();
        row.add(r.time().toString());
        row.add(csv(r.tag()));
        row.add(csv(r.scenario()));
        row.add(csv(env.minecraft()));
        row.add(env.loader());
        row.add(csv(env.modVersion()));
        row.add(csv(env.backend()));
        row.add(csv(env.gpu()));
        row.add(csv(env.driver()));
        row.add(csv(env.disabledOptions()));
        row.add(Integer.toString(env.renderDistance()));
        row.add(env.width() + "x" + env.height());
        row.add(fmt(r.seconds(), 2));
        row.add(Integer.toString(r.frames()));
        row.add(fmt(stats.avgFps(), 1));
        row.add(fmt(stats.low1Fps(), 1));
        row.add(fmt(stats.low01Fps(), 1));
        row.add(fmt(stats.p50Ms(), 3));
        row.add(fmt(stats.p95Ms(), 3));
        row.add(fmt(stats.p99Ms(), 3));
        row.add(fmt(stats.maxMs(), 3));
        row.add(Integer.toString(stats.stutters50Ms()));
        row.add(fmt(m.drawCalls() / frames, 1));
        row.add(fmt(m.drawItems() / frames, 1));
        row.add(fmt(m.pipelineBinds() / frames, 1));
        row.add(fmt(m.uniformBinds() / frames, 1));
        row.add(fmt(m.descriptorUpdates() / frames, 1));
        row.add(fmt(m.bufferViews() / frames, 2));
        row.add(Long.toString(m.lazyCount()));
        row.add(fmt(m.lazyNanos() / 1e6, 1));
        row.add(Long.toString(r.gcCount()));
        row.add(Long.toString(r.gcMillis()));
        row.add(Long.toString(LoadTimes.startupMillis()));
        row.add(Long.toString(startup.glslCount()));
        row.add(fmt(startup.glslNanos() / 1e6, 1));
        row.add(Long.toString(startup.pipelineCount()));
        row.add(fmt(startup.pipelineNanos() / 1e6, 1));
        row.add(Long.toString(r.reloadMillis()));
        row.add(reload == null ? "" : Long.toString(reload.glslCount()));
        row.add(reload == null ? "" : fmt(reload.glslNanos() / 1e6, 1));
        row.add(reload == null ? "" : Long.toString(reload.pipelineCount()));
        row.add(reload == null ? "" : fmt(reload.pipelineNanos() / 1e6, 1));
        row.add(Long.toString(m.sectionCompiles()));
        row.add(Long.toString(r.settleMillis()));
        row.add(Long.toString(r.maxSettleMillis()));
        row.add(csv(r.notes()));
        return row;
    }

    public static String summaryLine(BenchResult r) {
        BenchResult.FrameStats s = r.frameStats();
        double frames = Math.max(1, r.frames());
        return String.format(Locale.ROOT,
            "tag=%s scenario=%s backend=%s frames=%d avg_fps=%.1f low1_fps=%.1f low01_fps=%.1f p99_ms=%.2f max_ms=%.1f stutters=%d draws_pf=%.0f uniform_binds_pf=%.0f lazy_compiles=%d reload_ms=%d section_compiles=%d settle_ms=%d",
            r.tag(), r.scenario(), r.environment().backend(), r.frames(), s.avgFps(), s.low1Fps(), s.low01Fps(), s.p99Ms(), s.maxMs(),
            s.stutters50Ms(), r.measureCounters().drawCalls() / frames, r.measureCounters().uniformBinds() / frames,
            r.measureCounters().lazyCount(), r.reloadMillis(), r.measureCounters().sectionCompiles(), r.settleMillis());
    }

    private static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9.+-]", "-");
    }
}
