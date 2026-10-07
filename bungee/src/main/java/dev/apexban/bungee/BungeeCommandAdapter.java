package dev.apexban.bungee;

import dev.apexban.core.command.CoreCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;

final class BungeeCommandAdapter extends Command implements TabExecutor {
    private final CoreCommand delegate;

    BungeeCommandAdapter(CoreCommand delegate) {
        super(delegate.name(), null, delegate.aliases().toArray(new String[0]));
        this.delegate = delegate;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        delegate.execute(new BungeeActor(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = delegate.tabComplete(new BungeeActor(sender), args);
        return suggestions;
    }
}
