package org.templateEventGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.simpleEventManager.api.EventGame;
import org.templateEventGame.game.MyMiniGame;

import java.util.List;

public class TemplateEventGame extends JavaPlugin implements EventGame {

    private MyMiniGame game;

    @Override
    public void onEnable() {
        getLogger().info("TemplateEventGame enabled!");
    }

    @Override
    public void start(List<Player> players) {
        this.game = new MyMiniGame(players, this);
        game.start();
    }

    @Override
    public void stop() {
        if (game != null) game.stop();
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
        return "template";
    }

    @Override
    public String getEventDescription() {
        return "template event";
    }
}
