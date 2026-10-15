package gg.tame.palladium.command.subcommands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import gg.tame.palladium.async.path.AsyncPathProcessor;
import gg.tame.palladium.command.PalladiumCommand;
import gg.tame.palladium.command.PermissionedPalladiumSubcommand;
import gg.tame.palladium.config.modules.async.AsyncPathfinding;
import gg.tame.palladium.config.modules.async.AsyncChunkSend;
import gg.tame.palladium.config.modules.async.AsyncPlayerDataSave;
import gg.tame.palladium.config.modules.async.MultithreadedTracker;
import gg.tame.palladium.config.modules.async.SparklyPaperParallelWorldTicking;
import gg.tame.palladium.config.modules.opt.VirtualThreadSupport;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public final class PerfCommand extends PermissionedPalladiumSubcommand {

    public static final String LITERAL_ARGUMENT = "perf";
    public static final String PERM = PalladiumCommand.BASE_PERM + "." + LITERAL_ARGUMENT;

    public PerfCommand() {
        super(PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        final String view = args.length == 0 ? "queues" : args[0].toLowerCase(Locale.ROOT);
        switch (view) {
            case "queues" -> sendQueueMetrics(sender);
            case "async" -> sendAsyncMetrics(sender);
            case "memory" -> sendMemoryMetrics(sender);
            case "gc" -> sendGcMetrics(sender);
            case "all" -> {
                sendQueueMetrics(sender);
                sendAsyncMetrics(sender);
                sendMemoryMetrics(sender);
                sendGcMetrics(sender);
            }
            default -> sender.sendMessage(text("Usage: /palladium perf [queues | async | memory | gc | all]", GRAY));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        return args.length == 1 ? List.of("queues", "async", "memory", "gc", "all") : List.of();
    }

    private static void sendQueueMetrics(CommandSender sender) {
        sender.sendMessage(Component.text()
            .content("Palladium Queue Metrics")
            .color(GOLD)
            .build());
        sender.sendMessage(line("enabled", Boolean.toString(AsyncPathfinding.enabled)));
        sender.sendMessage(line("threads", Integer.toString(AsyncPathfinding.asyncPathfindingMaxThreads)));
        sender.sendMessage(line("queue-size", Integer.toString(AsyncPathfinding.asyncPathfindingQueueSize)));
        sender.sendMessage(line("timeout-seconds", Integer.toString(AsyncPathfinding.asyncPathfindingTimeoutSeconds)));
        sender.sendMessage(line("queued", Long.toString(AsyncPathProcessor.queuedTasks())));
        sender.sendMessage(line("completed", Long.toString(AsyncPathProcessor.completedTasks())));
        sender.sendMessage(line("rejected", Long.toString(AsyncPathProcessor.rejectedTasks())));
        sender.sendMessage(line("caller-runs", Long.toString(AsyncPathProcessor.callerRunsTasks())));
        sender.sendMessage(line("timed-out", Long.toString(AsyncPathProcessor.timedOutTasks())));
    }

    private static void sendAsyncMetrics(CommandSender sender) {
        sender.sendMessage(text("Palladium Async Feature State", GOLD));
        sender.sendMessage(line("async-chunk-send", Boolean.toString(AsyncChunkSend.enabled)));
        sender.sendMessage(line("async-playerdata-save", Boolean.toString(AsyncPlayerDataSave.enabled)));
        sender.sendMessage(line("async-pathfinding", Boolean.toString(AsyncPathfinding.enabled)));
        sender.sendMessage(line("multithreaded-tracker", Boolean.toString(MultithreadedTracker.enabled)));
        sender.sendMessage(line("parallel-world-ticking", Boolean.toString(SparklyPaperParallelWorldTicking.enabled)));
        sender.sendMessage(line("virtual-thread-auth-pool", Boolean.toString(VirtualThreadSupport.authPool)));
        sender.sendMessage(line("virtual-thread-auth-max-concurrency", Integer.toString(VirtualThreadSupport.authPoolMaxConcurrency)));
        sender.sendMessage(line("virtual-thread-download-pool", Boolean.toString(VirtualThreadSupport.downloadPool)));
        sender.sendMessage(line("virtual-thread-download-max-concurrency", Integer.toString(VirtualThreadSupport.downloadPoolMaxConcurrency)));
    }

    private static void sendMemoryMetrics(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        sender.sendMessage(text("Palladium JVM Memory", GOLD));
        sender.sendMessage(line("used-mb", Long.toString(used / 1024L / 1024L)));
        sender.sendMessage(line("free-mb", Long.toString(runtime.freeMemory() / 1024L / 1024L)));
        sender.sendMessage(line("total-mb", Long.toString(runtime.totalMemory() / 1024L / 1024L)));
        sender.sendMessage(line("max-mb", Long.toString(runtime.maxMemory() / 1024L / 1024L)));
        sender.sendMessage(line("processors", Integer.toString(runtime.availableProcessors())));
    }

    private static void sendGcMetrics(CommandSender sender) {
        sender.sendMessage(text("Palladium GC (since JVM start)", GOLD));
        final long uptimeMs = Math.max(1L, ManagementFactory.getRuntimeMXBean().getUptime());
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            final long count = Math.max(0L, gc.getCollectionCount());
            final long timeMs = Math.max(0L, gc.getCollectionTime());
            final String avg = count == 0L ? "0" : String.format(Locale.ROOT, "%.1f", (double) timeMs / count);
            final String share = String.format(Locale.ROOT, "%.2f", timeMs * 100.0D / uptimeMs);
            sender.sendMessage(line(gc.getName(), count + " collections, " + timeMs + "ms total, " + avg + "ms avg, " + share + "% of uptime"));
        }
    }

    private static Component line(String key, String value) {
        return text("  " + key + ": ", GRAY).append(text(value, YELLOW));
    }
}
