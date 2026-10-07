package dev.apexban.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import dev.apexban.core.ApexCore;
import dev.apexban.core.model.Punishment;

import java.sql.SQLException;

final class VelocityListener {
    private final ApexCore core;

    VelocityListener(ApexCore core) {
        this.core = core;
    }

    /**
     * Authoritative ban check, executed asynchronously so the database query never blocks the proxy.
     */
    @Subscribe(order = PostOrder.EARLY)
    public EventTask onLogin(final LoginEvent event) {
        return EventTask.async(new Runnable() {
            @Override
            public void run() {
                if (!event.getResult().isAllowed()) {
                    return;
                }
                Player player = event.getPlayer();
                try {
                    Punishment ban = core.service().onLogin(player.getUniqueId(), player.getUsername());
                    if (ban != null) {
                        event.setResult(ResultedEvent.ComponentResult.denied(
                                VelocityText.component(core.renderBan(ban))));
                    }
                } catch (SQLException ex) {
                    core.logger().error("Could not check the ban status of " + player.getUsername() + ": "
                            + ex.getMessage(), ex);
                    if (!core.settings().failOpen) {
                        event.setResult(ResultedEvent.ComponentResult.denied(
                                VelocityText.component(core.messages().get("login-database-error"))));
                    }
                }
            }
        });
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisconnect(DisconnectEvent event) {
        core.service().onQuit(event.getPlayer().getUniqueId());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onChat(PlayerChatEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }
        Player player = event.getPlayer();
        Punishment mute = core.activeMute(player.getUniqueId());
        if (mute == null) {
            return;
        }
        event.setResult(PlayerChatEvent.ChatResult.denied());
        VelocityText.send(player, core.renderMuteBlocked(mute));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onCommand(CommandExecuteEvent event) {
        if (!event.getResult().isAllowed() || !(event.getCommandSource() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getCommandSource();
        Punishment mute = core.activeMute(player.getUniqueId());
        if (mute == null || !core.isBlockedCommand(event.getCommand())) {
            return;
        }
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        VelocityText.send(player, core.renderMuteBlocked(mute));
    }
}
