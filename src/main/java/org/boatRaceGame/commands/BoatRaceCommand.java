package org.boatRaceGame.commands;

import org.boatRaceGame.BoatRaceEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BoatRaceCommand implements CommandExecutor {

    private final BoatRaceEvent plugin;

    public BoatRaceCommand(BoatRaceEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§aConfiguration de BoatRace rechargée.");
            return true;
        }
        sender.sendMessage("§cUsage: /boatrace reload");
        return true;
    }
}
