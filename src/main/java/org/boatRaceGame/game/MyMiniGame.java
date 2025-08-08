package org.boatRaceGame.game;

import org.boatRaceGame.BoatRaceEvent;
import org.boatRaceGame.utils.SmoothCanon;

import org.bukkit.*;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.simpleEventManager.SimpleEventManager;
import org.simpleEventManager.utils.EventUtils;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class MyMiniGame implements Listener {

    private final List<Player> players;
    private final List<Player> winners = new ArrayList<>();
    private final BoatRaceEvent plugin;
    private boolean running = false;
    private final List<Location> glassBlocks = new ArrayList<>();
    private Team noCollisionTeam;
    private boolean countdownStarted = false;
    private final Map<Player, Location> originalLocations = new HashMap<>();
    private final Map<UUID, UUID> boatOwners = new HashMap<>();

    public MyMiniGame(List<Player> players, BoatRaceEvent plugin) {
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
        removeAllBoats();
        placeGlassBarrier();
        // Téléporter tous les joueurs au spawn du monde starwars
        World spawnWorld = Bukkit.getWorld("starwars");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();

        for (Player player : players) {
            player.teleport(defaultSpawn);  // ← Téléporte au spawn de starwars
            player.setCollidable(true);
            plugin.getLogger().info("[BoatRace] " + player.getName() + " téléporté au spawn de starwars");
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
        winners.remove(player);
        if (originalLocations.containsKey(player)) {
            player.teleport(originalLocations.get(player));
        }
        player.sendMessage("§cTu as quitté la course.");
    }

    public List<Player> getWinners() {
        return winners;
    }

    public boolean hasWinner() {
        return winners.size() == players.size();
    }

    @EventHandler
    public void onBoatPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Boat boat)) return;
        Player player = event.getPlayer();
        if (player == null) return;

        boatOwners.put(boat.getUniqueId(), player.getUniqueId());
    }

    @EventHandler
    public void onBoatEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(event.getEntered() instanceof Player player)) return;

        UUID owner = boatOwners.get(boat.getUniqueId());
        if (owner != null && !owner.equals(player.getUniqueId())) {
            player.sendMessage("§cCe bateau appartient à un autre joueur !");
            event.setCancelled(true);
            return;
        }

        player.setCollidable(false);
    }

    @EventHandler
    public void onBoatExit(VehicleExitEvent event) {
        if (!running) return;
        if (!(event.getVehicle() instanceof Boat)) return;
        if (!(event.getExited() instanceof Player player)) return;
        if (player.isOp()) return;

        if (players.contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!running) return;
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (boat.getPassengers().isEmpty()) return;

        Player player = (Player) boat.getPassengers().get(0);
        if (!players.contains(player)) return;
        if (winners.contains(player)) return;

        Location under = boat.getLocation().subtract(0, 1, 0);
        Material underMaterial = under.getBlock().getType();

        Location locfinish = boat.getLocation();
        if (isInFinishZone(locfinish)) {
            winners.add(player);
            boat.remove();
            player.setGameMode(GameMode.SPECTATOR);
            Bukkit.broadcastMessage("§6" + player.getName() + " a franchi la ligne d'arrivée !");
            if (!countdownStarted) {
                startEndCountdown();
                countdownStarted = true;
            }
            return;
        }

        if (plugin.getConfig().getConfigurationSection("boat-race.cannon-blocks") != null) {
            if (plugin.getConfig().getConfigurationSection("boat-race.cannon-blocks").contains(underMaterial.name())) {
                Location start = boat.getLocation();
                int offsetX = plugin.getConfig().getInt("boat-race.cannon-blocks." + underMaterial.name() + ".offset-x");
                int offsetY = plugin.getConfig().getInt("boat-race.cannon-blocks." + underMaterial.name() + ".offset-y");
                int offsetZ = plugin.getConfig().getInt("boat-race.cannon-blocks." + underMaterial.name() + ".offset-z");

                SmoothCanon.launchBoatWithSnowball(plugin, boat, start, underMaterial.name());
            }
        }
    }

    private void placeGlassBarrier() {
        World world = Bukkit.getWorld("boatrace");
        if (world == null) {
            Bukkit.getLogger().warning("[BoatRace] Le monde 'boatrace' n'a pas été trouvé !");
            return;
        }

        int minX = Math.min(9, -21);
        int maxX = Math.max(9, -21);
        int minY = Math.min(2, 7);
        int maxY = Math.max(2, 7);
        int z = -20;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Location loc = new Location(world, x, y, z);
                glassBlocks.add(loc);
                if (loc.getBlock().getType() != Material.GLASS) {
                    loc.getBlock().setType(Material.GLASS);
                }
            }
        }
    }

    private void removeGlassBarrier() {
        for (Location loc : glassBlocks) {
            loc.getBlock().setType(Material.AIR);
        }
    }

    private void startFinishChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }

                for (Player player : new ArrayList<>(players)) {
                    if (winners.contains(player)) continue;
                    if (!player.isInsideVehicle()) continue;
                    if (!(player.getVehicle() instanceof Boat boat)) continue;

                    Location locfinish = boat.getLocation();
                    if (isInFinishZone(locfinish)) {
                        winners.add(player);
                        boat.remove();
                        player.setGameMode(GameMode.SPECTATOR);
                        Bukkit.broadcastMessage("§6" + player.getName() + " a franchi la ligne d'arrivée !");
                        if (!countdownStarted) {
                            startEndCountdown();
                            countdownStarted = true;
                        }
                    }
                }
                if (hasWinner()) {
                    Bukkit.broadcastMessage("§aTous les joueurs ont terminé ! Fin de la course !");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        fillPodium();
                        running = false;
                    });
                    cancel(); // Arrêter ce checker
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void startEndCountdown() {
        new BukkitRunnable() {
            int secondsLeft = 90;

            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }

                if (secondsLeft <= 0) {
                    Bukkit.broadcastMessage("§cLe temps est écoulé !");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        fillPodium();
                        running = false;
                    });
                    cancel();
                    return;
                }

                if (secondsLeft % 10 == 0 || secondsLeft <= 10) {
                    Bukkit.broadcastMessage("§eFin de l'event dans §6" + secondsLeft + "§e secondes !");
                }
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void fillPodium() {
        List<Player> remaining = new ArrayList<>(players);
        remaining.removeAll(winners);

        while (!remaining.isEmpty()) {
            Player randomPlayer = remaining.remove((int) (Math.random() * remaining.size()));
            winners.add(randomPlayer);
            randomPlayer.sendMessage("§6Tu as été choisi aléatoirement pour compléter le podium !");
        }
        plugin.getLogger().info("[BoatRace] Total participants: " + winners.size() + " joueurs");
    }

    private boolean isInFinishZone(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        Location under = loc.clone().subtract(0, 1, 0);
        Material underType = under.getBlock().getType();
        return underType == Material.EMERALD_BLOCK;
    }

    private void removeAllBoats() {
        World world = Bukkit.getWorld("boatrace");
        if (world == null) return;
        for (Boat boat : world.getEntitiesByClass(Boat.class)) {
            boat.remove();
        }
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
        winners.remove(player);

        World spawnWorld = Bukkit.getWorld("world");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
        Location loc = originalLocations.getOrDefault(player, defaultSpawn);

        player.teleport(loc);
        player.sendMessage("§cTu as quitté la course.");
    }
}
