package dev.apexban.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.apexban.core.ApexCore;
import dev.apexban.core.ApexInfo;
import dev.apexban.core.command.CoreCommand;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "apexban",
        name = ApexInfo.NAME,
        version = ApexInfo.VERSION,
        description = "Enterprise moderation core - bans, mutes and kicks with a network-wide database.",
        authors = {"ApexBan"}
)
public final class ApexBanVelocity {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private ApexCore core;

    @Inject
    public ApexBanVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        core = new ApexCore(new VelocityPlatform(proxy, dataDirectory.toFile(), new VelocityLogger(logger)));
        if (!core.start()) {
            logger.error("ApexBan could not start because the database is unavailable.");
            core = null;
            return;
        }
        CommandManager manager = proxy.getCommandManager();
        for (CoreCommand command : core.commands()) {
            CommandMeta meta = manager.metaBuilder(command.name())
                    .aliases(command.aliases().toArray(new String[0]))
                    .plugin(this)
                    .build();
            manager.register(meta, new VelocityCommandAdapter(command));
        }
        proxy.getEventManager().register(this, new VelocityListener(core));
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            core.shutdown();
            core = null;
        }
    }
}
