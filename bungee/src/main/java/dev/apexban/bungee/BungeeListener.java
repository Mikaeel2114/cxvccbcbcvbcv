package dev.apexban.bungee;

import dev.apexban.core.ApexCore;
import dev.apexban.core.model.Punishment;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.sql.SQLException;
import java.util.UUID;

final class BungeeListener implements Listener {
    private final Plugin plugin;
    private final ApexCore core;

    BungeeListener(Plugin plugin, ApexCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    /**
     * Authoritative ban check. The event is held open while a worker thread queries the database, so the
     * proxy's network threads are never blocked.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final LoginEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final PendingConnection connection = event.getConnection();
        final UUID uuid = connection.getUniqueId();
        final String name = connection.getName();
        event.registerIntent(plugin);
        core.async(new Runnable() {
            @Override
            public void run() {
                try {
                    Punishment ban = core.service().onLogin(uuid, name);
                    if (ban != null) {
                        deny(event, core.renderBan(ban));
                    }
                } catch (SQLException ex) {
                    core.logger().error("Could not check the ban status of " + name + ": " + ex.getMessage(), ex);
                    if (!core.settings().failOpen) {
                        deny(event, core.messages().get("login-database-error"));
                    }
                } finally {
                    event.completeIntent(plugin);
                }
            }
        });
    }

    private static void deny(LoginEvent event, String legacyText) {
        BaseComponent[] reason = BungeeText.component(legacyText);
        event.setCancelled(true);
        event.setCancelReason(reason);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDisconnect(PlayerDisconnectEvent event) {
        core.service().onQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        Punishment mute = core.activeMute(player.getUniqueId());
        if (mute == null) {
            return;
        }
        if (event.isCommand() && !core.isBlockedCommand(event.getMessage())) {
            return;
        }
        event.setCancelled(true);
        BungeeText.send(player, core.renderMuteBlocked(mute));
    }
}
