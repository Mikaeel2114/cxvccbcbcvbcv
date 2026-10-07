package dev.apexban.bukkit;

import dev.apexban.core.ApexCore;
import dev.apexban.core.command.CoreCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ApexBanBukkit extends JavaPlugin {
    private ApexCore core;

    @Override
    public void onEnable() {
        core = new ApexCore(new BukkitPlatform(this));
        if (!core.start()) {
            getLogger().severe("ApexBan could not start because the database is unavailable. Disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        for (CoreCommand command : core.commands()) {
            PluginCommand registered = getCommand(command.name());
            if (registered == null) {
                getLogger().warning("Command /" + command.name() + " is missing from plugin.yml.");
                continue;
            }
            BukkitCommandAdapter adapter = new BukkitCommandAdapter(command);
            registered.setExecutor(adapter);
            registered.setTabCompleter(adapter);
        }
        getServer().getPluginManager().registerEvents(new BukkitListener(core), this);
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
        }
    }
}
