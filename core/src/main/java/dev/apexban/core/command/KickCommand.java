package dev.apexban.core.command;

import dev.apexban.core.ApexCore;
import dev.apexban.core.model.Punishment;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.model.Target;
import dev.apexban.core.platform.CommandActor;
import dev.apexban.core.platform.OnlinePlayer;
import dev.apexban.core.text.Placeholders;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public final class KickCommand implements CoreCommand {
    private final ApexCore core;

    public KickCommand(ApexCore core) {
        this.core = core;
    }

    @Override
    public String name() {
        return "kick";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "apexban.kick";
    }

    @Override
    public void execute(final CommandActor actor, String[] args) {
        if (!actor.hasPermission(permission())) {
            actor.sendMessage(core.messages().get("no-permission"));
            return;
        }
        if (args.length < 1) {
            actor.sendMessage(core.messages().get("usage.kick"));
            return;
        }
        String targetName = args[0];
        if (!CommandSupport.validName(targetName)) {
            actor.sendMessage(core.messages().get("invalid-player-name", Placeholders.of("input", targetName)));
            return;
        }
        OnlinePlayer online = core.platform().findOnlinePlayer(targetName);
        if (online == null) {
            actor.sendMessage(core.messages().get("player-not-online", Placeholders.of("player", targetName)));
            return;
        }
        if (actor.uuid() != null && actor.uuid().equals(online.uuid())) {
            actor.sendMessage(core.messages().get("cannot-punish-self"));
            return;
        }
        if (online.hasPermission(ApexCore.EXEMPT_PERMISSION)) {
            actor.sendMessage(core.messages().get("player-exempt", Placeholders.of("player", online.name())));
            return;
        }

        final Target target = new Target(online.uuid(), online.name());
        final String reason = CommandSupport.reason(args, 1, core.settings().defaultKickReason);
        final String operator = actor.name();
        core.async(new Runnable() {
            @Override
            public void run() {
                Punishment record;
                try {
                    record = core.service().punish(PunishmentType.KICK, target, operator, reason,
                            Punishment.PERMANENT, core.settings().serverId);
                } catch (SQLException ex) {
                    core.logger().error("Could not record kick: " + ex.getMessage(), ex);
                    record = new Punishment(0L, PunishmentType.KICK, target.getUuid(), target.getName(), operator,
                            reason, System.currentTimeMillis(), Punishment.PERMANENT, core.settings().serverId);
                }
                final Punishment finalRecord = record;
                core.logger().info(operator + " kicked " + target.getName() + ": " + reason);
                core.sync(new Runnable() {
                    @Override
                    public void run() {
                        OnlinePlayer current = core.platform().findOnlinePlayer(target.getUuid());
                        if (current != null) {
                            current.kick(core.renderKick(finalRecord));
                        }
                        core.announce("kick", core.messages().get("broadcast.kick",
                                core.placeholders(finalRecord)), actor);
                    }
                });
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandActor actor, String[] args) {
        if (!actor.hasPermission(permission()) || args.length != 1) {
            return Collections.emptyList();
        }
        return CommandSupport.onlineNames(core, args[0]);
    }
}
