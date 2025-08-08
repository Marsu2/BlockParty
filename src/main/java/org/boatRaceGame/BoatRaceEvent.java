package org.boatRaceGame;

import org.boatRaceGame.commands.BoatRaceCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.simpleEventManager.api.EventGame;
import org.boatRaceGame.game.MyMiniGame;

import java.util.List;

public class BoatRaceEvent extends JavaPlugin implements EventGame {

    private MyMiniGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("boatrace").setExecutor(new BoatRaceCommand(this));
        getLogger().info("BoatRace enabled!");
    }

    @Override
    public void start(List<Player> players) {
        this.game = new MyMiniGame(players, this);
        game.start();
    }

    @Override
    public void stop() {
        if (game != null) {
            game.stop();
            game = null;
        }
    }

    @Override
    public boolean hasWinner() {
        return game != null && game.hasWinner();
    }

    @Override
    public List<Player> getWinners() {
        return game != null ? game.getWinners() : List.of();
    }

    @Override
    public String getEventName() {
        return "BoatRace";
    }

    @Override
    public String getEventDescription() {
        return "Prend ton bateau et franchis la ligne d'arrivée avant les autres !";
    }
    @Override
    public void Removeplayer(Player player) {
        if (game != null) {
            game.removePlayer2(player);
        }
    }
}
