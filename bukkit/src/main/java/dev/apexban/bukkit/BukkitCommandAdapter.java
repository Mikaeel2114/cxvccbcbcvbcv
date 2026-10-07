package dev.apexban.bukkit;

import dev.apexban.core.command.CoreCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

final class BukkitCommandAdapter implements TabExecutor {
    private final CoreCommand delegate;

    BukkitCommandAdapter(CoreCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        delegate.execute(new BukkitActor(sender), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return delegate.tabComplete(new BukkitActor(sender), args);
    }
}
