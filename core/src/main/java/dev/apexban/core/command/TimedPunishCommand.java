package dev.apexban.core.command;

import dev.apexban.core.ApexCore;
import dev.apexban.core.model.Punishment;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.model.Target;
import dev.apexban.core.platform.CommandActor;
import dev.apexban.core.platform.OnlinePlayer;
import dev.apexban.core.text.Placeholders;
import dev.apexban.core.util.DurationParser;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Shared implementation of /ban and /mute: "/cmd player [duration] [reason...]". A valid duration makes
 * the punishment temporary; leaving it out (or using "perm") makes it permanent.
 */
abstract class TimedPunishCommand implements CoreCommand {
    private static final List<String> DURATION_SUGGESTIONS = Arrays.asList("1h", "1d", "7d", "30d", "perm");

    protected final ApexCore core;
    private final PunishmentType type;
    private final String name;
    private final String permission;
    private final List<String> aliases;

    TimedPunishCommand(ApexCore core, PunishmentType type, String name, String permission, List<String> aliases) {
        this.core = core;
        this.type = type;
        this.name = name;
        this.permission = permission;
        this.aliases = aliases;
    }

    protected abstract String defaultReason();

    protected abstract void afterPunish(CommandActor actor, Punishment punishment);

    protected abstract String alreadyKey();

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> aliases() {
        return aliases;
    }

    @Override
    public String permission() {
        return permission;
    }

    @Override
    public void execute(final CommandActor actor, String[] args) {
        if (!actor.hasPermission(permission)) {
            actor.sendMessage(core.messages().get("no-permission"));
            return;
        }
        if (args.length < 1) {
            actor.sendMessage(core.messages().get("usage." + name));
            return;
        }
        final String targetName = args[0];
        if (!CommandSupport.validName(targetName)) {
            actor.sendMessage(core.messages().get("invalid-player-name", Placeholders.of("input", targetName)));
            return;
        }

        long parsedDuration = Punishment.PERMANENT;
        int reasonStart = 1;
        if (args.length > 1) {
            String token = args[1];
            if (DurationParser.isPermanentKeyword(token)) {
                reasonStart = 2;
            } else if (DurationParser.looksLikeDuration(token)) {
                try {
                    parsedDuration = DurationParser.parse(token);
                    reasonStart = 2;
                } catch (IllegalArgumentException ex) {
                    actor.sendMessage(core.messages().get("invalid-duration", Placeholders.of("input", token)));
                    return;
                }
            }
        }
        final long duration = parsedDuration;
        final String reason = CommandSupport.reason(args, reasonStart, defaultReason());

        final OnlinePlayer online = core.platform().findOnlinePlayer(targetName);
        if (online != null) {
            if (actor.uuid() != null && actor.uuid().equals(online.uuid())) {
                actor.sendMessage(core.messages().get("cannot-punish-self"));
                return;
            }
            if (online.hasPermission(ApexCore.EXEMPT_PERMISSION)) {
                actor.sendMessage(core.messages().get("player-exempt", Placeholders.of("player", online.name())));
                return;
            }
        } else if (actor.name().equalsIgnoreCase(targetName)) {
            actor.sendMessage(core.messages().get("cannot-punish-self"));
            return;
        }

        final String operator = actor.name();
        core.async(new Runnable() {
            @Override
            public void run() {
                process(actor, targetName, online, operator, reason, duration);
            }
        });
    }

    private void process(final CommandActor actor, final String targetName, OnlinePlayer online,
                         String operator, String reason, long duration) {
        try {
            final Target target = online != null
                    ? new Target(online.uuid(), online.name())
                    : core.service().resolve(targetName, core.settings().allowUnknownPlayers);
            if (target == null) {
                core.sync(new Runnable() {
                    @Override
                    public void run() {
                        actor.sendMessage(core.messages().get("player-not-found",
                                Placeholders.of("player", targetName)));
                    }
                });
                return;
            }
            if (core.service().findActive(type, target.getUuid()) != null) {
                core.sync(new Runnable() {
                    @Override
                    public void run() {
                        actor.sendMessage(core.messages().get(alreadyKey(),
                                Placeholders.of("player", target.getName())));
                    }
                });
                return;
            }
            final Punishment punishment = core.service().punish(type, target, operator, reason, duration,
                    core.settings().serverId);
            core.logger().info(operator + " issued " + (punishment.isPermanent() ? "permanent " : "temporary ")
                    + type.name() + " to " + target.getName() + " [#" + punishment.getId() + "]: " + reason);
            core.sync(new Runnable() {
                @Override
                public void run() {
                    afterPunish(actor, punishment);
                }
            });
        } catch (SQLException ex) {
            core.logger().error("Database error while processing /" + name + ": " + ex.getMessage(), ex);
            core.sync(new Runnable() {
                @Override
                public void run() {
                    actor.sendMessage(core.messages().get("database-error"));
                }
            });
        }
    }

    protected String announcementText(String key, Punishment punishment) {
        return core.messages().get("broadcast." + key + (punishment.isPermanent() ? "-permanent" : "-temporary"),
                core.placeholders(punishment));
    }

    @Override
    public List<String> tabComplete(CommandActor actor, String[] args) {
        if (!actor.hasPermission(permission)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return CommandSupport.onlineNames(core, args[0]);
        }
        if (args.length == 2) {
            return CommandSupport.filter(DURATION_SUGGESTIONS, args[1]);
        }
        return Collections.emptyList();
    }
}
