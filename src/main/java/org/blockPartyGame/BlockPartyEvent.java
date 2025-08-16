package org.blockPartyGame;

import org.blockPartyGame.commands.BlockPartyCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.simpleEventManager.api.EventGame;
import org.blockPartyGame.game.MyMiniGame;

import java.util.List;

public class BlockPartyEvent extends JavaPlugin implements EventGame {

    private MyMiniGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("blockparty").setExecutor(new BlockPartyCommand(this));
        getLogger().info("BlockParty enabled!");
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
        return "BlockParty";
    }

    @Override
    public String getEventDescription() {
        return "Tiens-toi sur la bonne couleur et élimine tes adversaires !";
    }
    @Override
    public void Removeplayer(Player player) {
        if (game != null) {
            game.removePlayer2(player);
        }
    }
}
