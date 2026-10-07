package dev.apexban.core.command;

import dev.apexban.core.ApexCore;
import dev.apexban.core.model.Punishment;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.platform.CommandActor;
import dev.apexban.core.platform.OnlinePlayer;

import java.util.Collections;

public final class BanCommand extends TimedPunishCommand {
    public BanCommand(ApexCore core) {
        super(core, PunishmentType.BAN, "ban", "apexban.ban", Collections.<String>emptyList());
    }

    @Override
    protected String defaultReason() {
        return core.settings().defaultBanReason;
    }

    @Override
    protected String alreadyKey() {
        return "already-banned";
    }

    @Override
    protected void afterPunish(CommandActor actor, Punishment punishment) {
        OnlinePlayer online = core.platform().findOnlinePlayer(punishment.getUuid());
        if (online != null) {
            online.kick(core.renderBan(punishment));
        }
        core.announce("ban", announcementText("ban", punishment), actor);
    }
}
