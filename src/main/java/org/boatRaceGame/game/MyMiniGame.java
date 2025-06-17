package org.boatRaceGame.game;

import org.boatRaceGame.BoatRaceEvent;
import org.boatRaceGame.utils.SmoothCanon;

import org.bukkit.*;
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

import java.util.ArrayList;
import java.util.List;

public class MyMiniGame implements Listener {

    private final List<Player> players;
    private final List<Player> winners = new ArrayList<>();
    private final BoatRaceEvent plugin;
    private boolean running = false;
    private final List<Location> glassBlocks = new ArrayList<>();
    private Team noCollisionTeam;
    private boolean countdownStarted = false;

    public MyMiniGame(List<Player> players, BoatRaceEvent plugin) {
        this.players = new ArrayList<>(players);
        this.plugin = plugin;
    }

    public void start() {
        SimpleEventManager sem = (SimpleEventManager) Bukkit.getPluginManager().getPlugin("SimpleEventManager");
        Location loc = EventUtils.getEventSpawnLocation(sem, plugin.getEventName());
        for (Player player : players) {
            player.teleport(loc);
        }

        for (Player player : players) {
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.OAK_BOAT));
        }
        placeGlassBarrier();


        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {

                if (countdown > 0) {
                    for (Player player : players) {
                        player.sendTitle("§a§lDépart dans " + countdown + "...", "", 5, 20, 5);
                    }
                } else {
                    for (Player player : players) {
                        player.sendTitle("§a§lGO !", "", 10, 40, 10);
                    }
                    removeGlassBarrier();
                    Bukkit.getPluginManager().registerEvents(MyMiniGame.this, plugin);
                    running = true;
                    startFinishChecker();
                    cancel();
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        running = false;
        removeAllBoats();
        placeGlassBarrier();
        for (Player player : players) {
            player.setCollidable(true);
        }
    }

    public List<Player> getWinners() {
        return winners;
    }

    public boolean hasWinner() {
        return winners.size() == players.size();
    }


    @EventHandler
    public void onBoatEnter(VehicleEnterEvent event) {
        if (!running) return;
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(event.getEntered() instanceof Player player)) return;

        if (!boat.getPassengers().isEmpty()) {
            player.sendMessage("§cCe bateau est déjà occupé !");
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

        // Si le joueur fait partie du jeu, on l'empêche de sortir
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
            }
        }.runTaskTimer(plugin, 0L, 2L); // Vérifie toutes les 2 ticks
    }

    private void startEndCountdown() {
        new BukkitRunnable() {
            int secondsLeft = 20;

            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }

                if (secondsLeft <= 0) {
                    Bukkit.broadcastMessage("§cLe temps est écoulé !");
                    fillPodium();
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        fillPodium();      // Remplir les winners
                        running = false;   // Stopper le jeu
                    });
                    cancel();
                    return;
                }

                // Broadcast toutes les 10 secondes ou dernière 10 secondes
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

        while (winners.size() < 3 && !remaining.isEmpty()) {
            Player randomPlayer = remaining.remove((int) (Math.random() * remaining.size()));
            winners.add(randomPlayer);
            randomPlayer.sendMessage("§6Tu as été choisi aléatoirement pour compléter le podium !");
        }
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


}
