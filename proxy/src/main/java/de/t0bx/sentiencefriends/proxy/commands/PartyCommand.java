package de.t0bx.sentiencefriends.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;

import java.util.List;

public class PartyCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {

    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }
}
