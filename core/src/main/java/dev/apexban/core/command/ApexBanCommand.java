package dev.apexban.core.command;

import dev.apexban.core.ApexCore;
import dev.apexban.core.ApexInfo;
import dev.apexban.core.platform.CommandActor;
import dev.apexban.core.text.Placeholders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ApexBanCommand implements CoreCommand {
    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "reload", "version");

    private final ApexCore core;

    public ApexBanCommand(ApexCore core) {
        this.core = core;
    }

    @Override
    public String name() {
        return "apexban";
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("apexb");
    }

    @Override
    public String permission() {
        return ApexCore.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandActor actor, String[] args) {
        if (!actor.hasPermission(permission())) {
            actor.sendMessage(core.messages().get("no-permission"));
            return;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "help";
        if (sub.equals("reload")) {
            core.reloadConfigs();
            actor.sendMessage(core.messages().get("reload-success"));
        } else if (sub.equals("version")) {
            actor.sendMessage(core.messages().get("version", Placeholders.of(
                    "version", ApexInfo.VERSION, "platform", core.platform().platformName())));
        } else {
            actor.sendMessage(core.messages().get("help"));
        }
    }

    @Override
    public List<String> tabComplete(CommandActor actor, String[] args) {
        if (!actor.hasPermission(permission()) || args.length != 1) {
            return Collections.emptyList();
        }
        return CommandSupport.filter(SUBCOMMANDS, args[0]);
    }
}
