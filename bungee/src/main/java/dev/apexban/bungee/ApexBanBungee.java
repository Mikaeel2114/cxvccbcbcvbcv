package dev.apexban.bungee;

import dev.apexban.core.ApexCore;
import dev.apexban.core.command.CoreCommand;
import net.md_5.bungee.api.plugin.Plugin;

public final class ApexBanBungee extends Plugin {
    private ApexCore core;

    @Override
    public void onEnable() {
        core = new ApexCore(new BungeePlatform(this));
        if (!core.start()) {
            getLogger().severe("ApexBan could not start because the database is unavailable.");
            core = null;
            return;
        }
        for (CoreCommand command : core.commands()) {
            getProxy().getPluginManager().registerCommand(this, new BungeeCommandAdapter(command));
        }
        getProxy().getPluginManager().registerListener(this, new BungeeListener(this, core));
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
            core = null;
        }
    }
}
