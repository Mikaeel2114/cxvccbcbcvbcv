package dev.apexban.bukkit;

import dev.apexban.core.ApexCore;
import dev.apexban.core.model.Punishment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;

final class BukkitListener implements Listener {
    private final ApexCore core;

    BukkitListener(ApexCore core) {
        this.core = core;
    }

    /**
     * Authoritative ban check. Runs on an asynchronous thread, so the database query never lags the server.
     * It also records the player and loads an active mute into the cache.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            Punishment ban = core.service().onLogin(event.getUniqueId(), event.getName());
            if (ban != null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, core.renderBan(ban));
            }
        } catch (SQLException ex) {
            core.logger().error("Could not check the ban status of " + event.getName() + ": " + ex.getMessage(), ex);
            if (!core.settings().failOpen) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, core.messages().get("login-database-error"));
            }
        }
    }

    /**
     * Second line of defence on the main thread, using only memory: catches a ban that was issued after the
     * asynchronous check finished but before the player finished logging in.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }
        Punishment ban = core.service().cachedBan(event.getPlayer().getUniqueId());
        if (ban != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, core.renderBan(ban));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        core.service().onQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Punishment mute = core.activeMute(player.getUniqueId());
        if (mute == null) {
            return;
        }
        event.setCancelled(true);
        BukkitText.send(player, core.renderMuteBlocked(mute));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Punishment mute = core.activeMute(player.getUniqueId());
        if (mute == null || !core.isBlockedCommand(event.getMessage())) {
            return;
        }
        event.setCancelled(true);
        BukkitText.send(player, core.renderMuteBlocked(mute));
    }
}
