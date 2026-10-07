package dev.apexban.core;

import dev.apexban.core.command.ApexBanCommand;
import dev.apexban.core.command.BanCommand;
import dev.apexban.core.command.CoreCommand;
import dev.apexban.core.command.KickCommand;
import dev.apexban.core.command.MuteCommand;
import dev.apexban.core.command.UnpunishCommand;
import dev.apexban.core.config.Layouts;
import dev.apexban.core.config.Messages;
import dev.apexban.core.config.Settings;
import dev.apexban.core.config.YamlFile;
import dev.apexban.core.model.Punishment;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.platform.CommandActor;
import dev.apexban.core.platform.OnlinePlayer;
import dev.apexban.core.platform.Platform;
import dev.apexban.core.platform.PlatformLogger;
import dev.apexban.core.service.PunishmentService;
import dev.apexban.core.storage.SqlStorage;
import dev.apexban.core.text.TextFormatter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Platform independent heart of ApexBan. Each platform module creates one instance, forwards its
 * events to it and registers the commands it exposes.
 */
public final class ApexCore {
    public static final String EXEMPT_PERMISSION = "apexban.exempt";
    public static final String NOTIFY_PERMISSION = "apexban.notify";
    public static final String ADMIN_PERMISSION = "apexban.admin";

    private final Platform platform;
    private final PlatformLogger logger;
    private volatile Settings settings;
    private volatile Messages messages;
    private volatile Layouts layouts;
    private volatile PunishmentService service;
    private volatile SqlStorage storage;
    private volatile ScheduledExecutorService executor;
    private volatile List<CoreCommand> commands = Collections.emptyList();
    private volatile boolean running;

    public ApexCore(Platform platform) {
        this.platform = platform;
        this.logger = platform.logger();
    }

    public synchronized boolean start() {
        if (running) {
            return true;
        }
        loadConfigs();

        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(4, new WorkerThreadFactory());
        pool.setRemoveOnCancelPolicy(true);
        executor = pool;

        SqlStorage newStorage = new SqlStorage(settings, platform.dataFolder(), logger);
        try {
            newStorage.init();
        } catch (SQLException ex) {
            logger.error("Could not initialise the database: " + ex.getMessage(), ex);
            newStorage.close();
            pool.shutdownNow();
            executor = null;
            return false;
        }
        storage = newStorage;
        service = new PunishmentService(newStorage);
        commands = Collections.unmodifiableList(Arrays.<CoreCommand>asList(
                new BanCommand(this),
                new MuteCommand(this),
                new KickCommand(this),
                new UnpunishCommand(this, PunishmentType.BAN, "unban", "apexban.unban",
                        Collections.singletonList("pardon")),
                new UnpunishCommand(this, PunishmentType.MUTE, "unmute", "apexban.unmute",
                        Collections.<String>emptyList()),
                new ApexBanCommand(this)));
        running = true;
        scheduleSync();
        logger.info(ApexInfo.NAME + " " + ApexInfo.VERSION + " enabled on " + platform.platformName()
                + " (server-id: " + settings.serverId + ").");
        return true;
    }

    public synchronized void shutdown() {
        if (!running && executor == null && storage == null) {
            return;
        }
        running = false;
        ScheduledExecutorService pool = executor;
        executor = null;
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5L, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ex) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        SqlStorage current = storage;
        storage = null;
        if (current != null) {
            current.close();
        }
    }

    /**
     * Re-reads config.yml, messages.yml and layout.yml. Database settings and the sync interval are
     * only applied on restart.
     */
    public void reloadConfigs() {
        loadConfigs();
        logger.info("Configuration reloaded.");
    }

    private void loadConfigs() {
        YamlFile config = YamlFile.load(platform.dataFolder(), "config.yml", logger);
        Settings loaded = Settings.from(config, logger);
        TextFormatter formatter = new TextFormatter(loaded.hexColors);
        YamlFile messageFile = YamlFile.load(platform.dataFolder(), "messages.yml", logger);
        YamlFile layoutFile = YamlFile.load(platform.dataFolder(), "layout.yml", logger);
        this.messages = new Messages(messageFile, formatter);
        this.layouts = new Layouts(layoutFile, formatter);
        this.settings = loaded;
    }

    public Platform platform() {
        return platform;
    }

    public PlatformLogger logger() {
        return logger;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public Layouts layouts() {
        return layouts;
    }

    public PunishmentService service() {
        return service;
    }

    public List<CoreCommand> commands() {
        return commands;
    }

    public void async(final Runnable task) {
        Runnable guarded = new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    logger.error("Unhandled error in ApexBan worker: " + throwable.getMessage(), throwable);
                }
            }
        };
        ScheduledExecutorService pool = executor;
        if (pool == null) {
            guarded.run();
            return;
        }
        try {
            pool.execute(guarded);
        } catch (RejectedExecutionException ex) {
            guarded.run();
        }
    }

    public void sync(final Runnable task) {
        platform.runSync(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    logger.error("Unhandled error in ApexBan task: " + throwable.getMessage(), throwable);
                }
            }
        });
    }

    /**
     * Returns the active mute of the player from the cache, or null. Safe to call from any thread.
     */
    public Punishment activeMute(UUID uuid) {
        PunishmentService current = service;
        return current == null ? null : current.cachedMute(uuid);
    }

    /**
     * True when the chat line is a command that muted players are not allowed to use.
     */
    public boolean isBlockedCommand(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        String label = (space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        return !label.isEmpty() && settings.blockedCommands.contains(label);
    }

    public Map<String, String> placeholders(Punishment punishment) {
        Settings current = settings;
        Messages text = messages;
        long now = System.currentTimeMillis();
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("player", punishment.getTargetName());
        values.put("reason", punishment.getReason());
        values.put("operator", punishment.getOperator());
        values.put("id", punishment.getId() > 0L ? String.valueOf(punishment.getId()) : text.word("not-available"));
        values.put("server", punishment.getServerId());
        values.put("type", punishment.getType().name());
        values.put("date", current.formatDate(punishment.getCreatedAt()));
        if (punishment.getType() == PunishmentType.KICK) {
            values.put("duration", text.word("not-available"));
            values.put("expires", text.word("not-available"));
            values.put("remaining", text.word("not-available"));
        } else if (punishment.isPermanent()) {
            values.put("duration", text.word("permanent"));
            values.put("expires", text.word("never"));
            values.put("remaining", text.word("permanent"));
        } else {
            values.put("duration", text.formatDuration(punishment.getExpiresAt() - punishment.getCreatedAt()));
            values.put("expires", current.formatDate(punishment.getExpiresAt()));
            values.put("remaining", text.formatDuration(Math.max(0L, punishment.getExpiresAt() - now)));
        }
        return values;
    }

    public String renderBan(Punishment punishment) {
        return layouts.render(punishment.isPermanent() ? "ban.permanent" : "ban.temporary",
                placeholders(punishment));
    }

    public String renderKick(Punishment punishment) {
        return layouts.render("kick", placeholders(punishment));
    }

    public String renderMuteBlocked(Punishment punishment) {
        return messages.get(punishment.isPermanent() ? "mute-blocked.permanent" : "mute-blocked.temporary",
                placeholders(punishment));
    }

    /**
     * Broadcasts a punishment message when announcements for that kind are enabled and makes sure the
     * command sender always sees the outcome exactly once.
     */
    public void announce(String kind, String text, CommandActor actor) {
        Settings current = settings;
        boolean enabled = current.announces(kind);
        if (enabled) {
            platform.broadcast(text, current.announcePublic ? null : NOTIFY_PERMISSION);
        }
        boolean reached = enabled
                && (actor.isConsole() || current.announcePublic || actor.hasPermission(NOTIFY_PERMISSION));
        if (!reached) {
            actor.sendMessage(text);
        }
    }

    private void scheduleSync() {
        Settings current = settings;
        ScheduledExecutorService pool = executor;
        if (!current.syncEnabled || pool == null) {
            return;
        }
        long interval = current.syncIntervalSeconds;
        pool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }
                try {
                    platform.runSync(new Runnable() {
                        @Override
                        public void run() {
                            syncTick();
                        }
                    });
                } catch (Throwable throwable) {
                    logger.error("Could not schedule the punishment sync: " + throwable.getMessage(), throwable);
                }
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * Runs on the platform thread: snapshots the online players, then checks the database off-thread
     * so punishments issued on other servers take effect here.
     */
    private void syncTick() {
        if (!running) {
            return;
        }
        final PunishmentService currentService = service;
        final SqlStorage currentStorage = storage;
        if (currentService == null || currentStorage == null) {
            return;
        }
        currentService.pruneBans(System.currentTimeMillis());
        final List<UUID> online = new ArrayList<UUID>();
        for (OnlinePlayer player : platform.onlinePlayers()) {
            online.add(player.uuid());
        }
        if (online.isEmpty()) {
            return;
        }
        async(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Punishment> active = currentStorage.findActiveFor(online, System.currentTimeMillis());
                    final List<Punishment> toEnforce = currentService.applySync(online, active);
                    if (toEnforce.isEmpty()) {
                        return;
                    }
                    sync(new Runnable() {
                        @Override
                        public void run() {
                            for (Punishment ban : toEnforce) {
                                OnlinePlayer player = platform.findOnlinePlayer(ban.getUuid());
                                if (player != null) {
                                    player.kick(renderBan(ban));
                                }
                            }
                        }
                    });
                } catch (SQLException ex) {
                    logger.warn("Punishment sync failed: " + ex.getMessage());
                }
            }
        });
    }

    private static final class WorkerThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ApexBan-Worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
