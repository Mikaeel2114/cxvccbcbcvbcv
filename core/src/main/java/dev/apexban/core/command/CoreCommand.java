package dev.apexban.core.command;

import dev.apexban.core.platform.CommandActor;

import java.util.List;

public interface CoreCommand {
    String name();

    List<String> aliases();

    String permission();

    void execute(CommandActor actor, String[] args);

    List<String> tabComplete(CommandActor actor, String[] args);
}
