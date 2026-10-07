package dev.apexban.core.command;

import dev.apexban.core.ApexCore;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.model.Target;
import dev.apexban.core.platform.CommandActor;
import dev.apexban.core.platform.OnlinePlayer;
import dev.apexban.core.text.Placeholders;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Shared implementation of /unban and /unmute.
 */
public final class UnpunishCommand implements CoreCommand {
    private final ApexCore core;
    private final PunishmentType type;
    private final String name;
    private final String permission;
    private final List<String> aliases;

    public UnpunishCommand(ApexCore core, PunishmentType type, String name, String permission, List<String> aliases) {
        this.core = core;
        this.type = type;
        this.name = name;
        this.permission = permission;
        this.aliases = aliases;
    }

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
        final OnlinePlayer online = core.platform().findOnlinePlayer(targetName);
        final String operator = actor.name();
        core.async(new Runnable() {
            @Override
            public void run() {
                process(actor, targetName, online, operator);
            }
        });
    }

    private void process(final CommandActor actor, final String targetName, OnlinePlayer online, final String operator) {
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
            boolean lifted = core.service().revoke(type, target.getUuid(), operator);
            if (!lifted) {
                final String key = type == PunishmentType.BAN ? "not-banned" : "not-muted";
                core.sync(new Runnable() {
                    @Override
                    public void run() {
                        actor.sendMessage(core.messages().get(key, Placeholders.of("player", target.getName())));
                    }
                });
                return;
            }
            core.logger().info(operator + " lifted the " + type.name() + " of " + target.getName());
            core.sync(new Runnable() {
                @Override
                public void run() {
                    OnlinePlayer current = core.platform().findOnlinePlayer(target.getUuid());
                    if (type == PunishmentType.MUTE && current != null) {
                        current.sendMessage(core.messages().get("unmute-notice",
                                Placeholders.of("player", target.getName(), "operator", operator)));
                    }
                    core.announce(name, core.messages().get("broadcast." + name,
                            Placeholders.of("player", target.getName(), "operator", operator)), actor);
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

    @Override
    public List<String> tabComplete(CommandActor actor, String[] args) {
        if (!actor.hasPermission(permission) || args.length != 1) {
            return Collections.emptyList();
        }
        return CommandSupport.onlineNames(core, args[0]);
    }
}
