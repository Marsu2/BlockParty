package org.blockPartyGame.game;

import org.blockPartyGame.BlockPartyEvent;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.simpleEventManager.SimpleEventManager;
import org.simpleEventManager.utils.EventUtils;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class MyMiniGame implements Listener {

    private final List<Player> players;
    private final List<Player> winners = new ArrayList<>();
    private final BlockPartyEvent plugin;
    private boolean running = false;

    private final Map<Player, Location> originalLocations = new HashMap<>();


    public MyMiniGame(List<Player> players, BlockPartyEvent plugin) {
        this.players = new ArrayList<>(players);
        this.plugin = plugin;
    }

    public void start() {
        World eventWorld = Bukkit.getWorld("event");
        SimpleEventManager sem = (SimpleEventManager) Bukkit.getPluginManager().getPlugin("SimpleEventManager");
        Location loc = EventUtils.getEventSpawnLocation(sem, plugin.getEventName());

        Bukkit.getPluginManager().registerEvents(MyMiniGame.this, plugin);

        for (Player player : players) {
            originalLocations.put(player, player.getLocation());
            player.teleport(loc);
            player.getInventory().clear();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : players) {
                    player.sendTitle("§ePrépare-toi...", "§fDépart imminent", 10, 100, 10);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ConfigurationSection buildsSection = plugin.getConfig().getConfigurationSection("builds");
                        if (buildsSection == null || buildsSection.getKeys(false).isEmpty()) return;

                        List<String> keys = new ArrayList<>(buildsSection.getKeys(false));
                        String randomKey = keys.get(new Random().nextInt(keys.size()));
                        ConfigurationSection build = buildsSection.getConfigurationSection(randomKey);
                        if (build == null) return;

                        String name = build.getString("name", "Inconnu");

                        Location c1 = new Location(eventWorld,
                                build.getConfigurationSection("source_corner1").getInt("x"),
                                build.getConfigurationSection("source_corner1").getInt("y"),
                                build.getConfigurationSection("source_corner1").getInt("z"));

                        Location c2 = new Location(eventWorld,
                                build.getConfigurationSection("source_corner2").getInt("x"),
                                build.getConfigurationSection("source_corner2").getInt("y"),
                                build.getConfigurationSection("source_corner2").getInt("z"));

                        Bukkit.broadcastMessage("§aBuild choisi: §e" + name);

                        cloneAreaWorldEdit(c1, c2, loc);

                        running = true;
                    }
                }.runTaskLater(plugin, 15 * 20L);
            }
        }.runTask(plugin);
    }


    public void stop() {
        HandlerList.unregisterAll(this);
        running = false;

        // Téléporter tous les joueurs au spawn du monde world
        World spawnWorld = Bukkit.getWorld("world");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();

        for (Player player : players) {
            player.teleport(defaultSpawn);  // ← Téléporte au spawn de starwars
            player.setCollidable(true);
            plugin.getLogger().info("[BlockParty] " + player.getName() + " téléporté au spawn de world");
        }
    }

    private void cloneArea(Location c1, Location c2, Location dest) {
        World world = c1.getWorld();
        int minX = Math.min(c1.getBlockX(), c2.getBlockX());
        int minY = Math.min(c1.getBlockY(), c2.getBlockY());
        int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
        int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
        int maxY = Math.max(c1.getBlockY(), c2.getBlockY());
        int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());

        int dx = dest.getBlockX();
        int dy = dest.getBlockY();
        int dz = dest.getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block source = world.getBlockAt(x, y, z);
                    Block target = dest.getWorld().getBlockAt(
                            dx + (x - minX),
                            dy + (y - minY),
                            dz + (z - minZ)
                    );
                    target.setBlockData(source.getBlockData());
                }
            }
        }
    }



    public List<Player> getWinners() {
        return winners;
    }

    public boolean hasWinner() {
        return players.isEmpty();
    } //Si plus de joueur en lice = tous ont fini


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (players.contains(player)) {
            removePlayer2(player);
        }
    }
    public void removePlayer2(Player player) {
        players.remove(player);
        winners.add(0,player);


        World spawnWorld = Bukkit.getWorld("world");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
        Location loc = originalLocations.getOrDefault(player, defaultSpawn);

        player.teleport(loc);
        player.sendMessage("§cTu as quitté la course.");
    }

}
