package dev.apexban.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import dev.apexban.core.command.CoreCommand;

import java.util.List;

final class VelocityCommandAdapter implements SimpleCommand {
    private final CoreCommand delegate;

    VelocityCommandAdapter(CoreCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Invocation invocation) {
        delegate.execute(new VelocityActor(invocation.source()), invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return delegate.tabComplete(new VelocityActor(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
