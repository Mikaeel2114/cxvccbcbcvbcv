package dev.apexban.core.command;

import dev.apexban.core.ApexCore;
import dev.apexban.core.model.Punishment;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.platform.CommandActor;
import dev.apexban.core.platform.OnlinePlayer;

import java.util.Collections;

public final class MuteCommand extends TimedPunishCommand {
    public MuteCommand(ApexCore core) {
        super(core, PunishmentType.MUTE, "mute", "apexban.mute", Collections.<String>emptyList());
    }

    @Override
    protected String defaultReason() {
        return core.settings().defaultMuteReason;
    }

    @Override
    protected String alreadyKey() {
        return "already-muted";
    }

    @Override
    protected void afterPunish(CommandActor actor, Punishment punishment) {
        OnlinePlayer online = core.platform().findOnlinePlayer(punishment.getUuid());
        if (online != null) {
            core.service().cacheMute(punishment);
            online.sendMessage(core.messages().get(
                    punishment.isPermanent() ? "mute-notice.permanent" : "mute-notice.temporary",
                    core.placeholders(punishment)));
        }
        core.announce("mute", announcementText("mute", punishment), actor);
    }
}
