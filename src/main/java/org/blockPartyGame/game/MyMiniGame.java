package org.blockPartyGame.game;

import org.blockPartyGame.BlockPartyEvent;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
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

    private final List<Player> winners_Deco = new ArrayList<>();
    private final Map<Player, Location> originalLocations = new HashMap<>();
    private final Map<UUID, UUID> boatOwners = new HashMap<>();

    public MyMiniGame(List<Player> players, BlockPartyEvent plugin) {
        this.players = new ArrayList<>(players);
        this.plugin = plugin;
    }

    public void start() {
        SimpleEventManager sem = (SimpleEventManager) Bukkit.getPluginManager().getPlugin("SimpleEventManager");
        Location loc = EventUtils.getEventSpawnLocation(sem, plugin.getEventName());

        Bukkit.getPluginManager().registerEvents(MyMiniGame.this, plugin);
        for (Player player : players) {
            originalLocations.put(player, player.getLocation());
            player.teleport(loc);
        }

        for (Player player : players) {
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.OAK_BOAT));
        }
        placeGlassBarrier();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : players) {
                    player.sendTitle("§ePrépare-toi...", "§fDépart imminent", 10, 100, 10);
                }

                // Lance le compte à rebours après 15 secondes
                new BukkitRunnable() {
                    int countdown = 5;

                    @Override
                    public void run() {
                        if (countdown > 0) {
                            for (Player player : players) {
                                // Jouer le son personnellement
                                if (countdown <= 3) {
                                    player.playSound(player.getLocation(), "iamusic:music_disc.mariostart", SoundCategory.MASTER, 1f, 1f);

                                    String animation = switch (countdown) {
                                        case 3 -> "trois";
                                        case 2 -> "deux";
                                        case 1 -> "un";
                                        default -> null;
                                    };

                                    if (animation != null) {
                                        String animCmd = "iaplaytotemanimation animated_title:" + animation + " " + player.getName();
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), animCmd);
                                    }
                                }
                            }
                        } else {
                            for (Player player : players) {
                                // GO : titre + animation + son

                                player.playSound(player.getLocation(), "iamusic:music_disc.mariostart2", SoundCategory.MASTER, 1f, 1f);

                                String animCmd = "iaplaytotemanimation animated_title:go " + player.getName();
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), animCmd);
                            }

                            removeGlassBarrier();
                            running = true;
                            startFinishChecker();
                            cancel();
                        }

                        countdown--;
                    }
                }.runTaskTimer(plugin, 15 * 20L, 20L);

                // 15 secondes de délai avant le début du décompte
            }
        }.runTask(plugin);

    }

    public void stop() {
        HandlerList.unregisterAll(this);
        running = false;

        // Téléporter tous les joueurs au spawn du monde starwars
        World spawnWorld = Bukkit.getWorld("starwars");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();

        for (Player player : players) {
            player.teleport(defaultSpawn);  // ← Téléporte au spawn de starwars
            player.setCollidable(true);
            plugin.getLogger().info("[BlockParty] " + player.getName() + " téléporté au spawn de starwars");
        }
    }


    public List<Player> getWinners() {
        return winners;
    }

    public boolean hasWinner() {
        return winners.size() == players.size() + winners_Deco.size();
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (players.contains(player)) {
            removePlayer2(player);
        }
    }
    public void removePlayer2(Player player) {
        players.remove(player);

        if (winners.contains(player)) {
            winners_Deco.add(player);
            player.sendMessage("§aTu resteras dans le classement car tu avais franchi la ligne d'arrivée !");
        }

        World spawnWorld = Bukkit.getWorld("world");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
        Location loc = originalLocations.getOrDefault(player, defaultSpawn);

        player.teleport(loc);
        player.sendMessage("§cTu as quitté la course.");
    }
    public void logGameState() {
        plugin.getLogger().info("[BoatRace] État de la course:");
        plugin.getLogger().info("- Joueurs actifs: " + players.size());
        plugin.getLogger().info("- Gagnants connectés: " + winners.size());
        plugin.getLogger().info("- Gagnants déconnectés: " + winners_Deco.size());
        plugin.getLogger().info("- Total gagnants: " + (winners.size() + winners_Deco.size()));
    }
}
