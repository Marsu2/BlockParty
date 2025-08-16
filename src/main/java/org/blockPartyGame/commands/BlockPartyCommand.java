package org.blockPartyGame.commands;

import org.blockPartyGame.BlockPartyEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BlockPartyCommand implements CommandExecutor {

    private final BlockPartyEvent plugin;

    public BlockPartyCommand(BlockPartyEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§aConfiguration de BlockParty rechargée.");
            return true;
        }
        sender.sendMessage("§cUsage: /blockparty reload");
        return true;
    }
}
