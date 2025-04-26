package org.templateEventGame.game;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpleEventManager.SimpleEventManager;
import org.simpleEventManager.utils.EventUtils;
import org.templateEventGame.TemplateEventGame;

import java.util.ArrayList;
import java.util.List;

public class MyMiniGame implements Listener {

    private final List<Player> players;
    private final List<Player> ranking = new ArrayList<>();
    private final List<BlockState> originalBlocks = new ArrayList<>();
    private final TemplateEventGame plugin;
    private int countdownTaskId = -1;
    private Player winner;

    public MyMiniGame(List<Player> players, TemplateEventGame plugin) {
        this.players = new ArrayList<>(players);
        this.plugin = plugin;
    }

    public void start() {
        countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown == 0) {
                    Bukkit.broadcastMessage("§a§lGO !");
                    Bukkit.getPluginManager().registerEvents(MyMiniGame.this, plugin);
                    Bukkit.getScheduler().cancelTask(countdownTaskId);
                    return;
                }

                Bukkit.broadcastMessage("§eDébut dans §l" + countdown + "s...");
                countdown--;
            }
        }, 0L, 20L).getTaskId();
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        Bukkit.broadcastMessage("§c[Template] Événement stoppé.");
        Bukkit.getScheduler().cancelTask(countdownTaskId);
    }

    public boolean hasWinner() {
        return winner != null;
    }

    public List<Player> getWinners() {
        return new ArrayList<>(ranking);
    }

    private void eliminate(Player player) {
        // if player has win or is dead
        if (!players.contains(player)) return;

        players.remove(player);
        ranking.add(0, player);
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("§cTu es éliminé !");

        // pour avoir le spawn
        SimpleEventManager sem = (SimpleEventManager) Bukkit.getPluginManager().getPlugin("SimpleEventManager");
        player.teleport(EventUtils.getEventSpawnLocation(sem, plugin.getEventName()));

        if (players.size() == 1) {
            winner = players.get(0);
            ranking.add(0, winner);
            Bukkit.broadcastMessage("§6[Template] Le gagnant est : §e" + winner.getName());
        }
    }

}