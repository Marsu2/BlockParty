package org.blockPartyGame.game;

import org.blockPartyGame.BlockPartyEvent;
import org.blockPartyGame.utils.Clone;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.simpleEventManager.SimpleEventManager;
import org.simpleEventManager.utils.EventUtils;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MyMiniGame implements Listener {

    private final List<Player> players; // TOUS les joueurs qui ont participé (jamais modifiée)
    private final List<Player> alivePlayers; // Joueurs encore en vie (modifiée quand élimination)
    private final List<Player> winners = new ArrayList<>();
    private final BlockPartyEvent plugin;
    private boolean running = false;

    private List<Material> currentBuildColors = new ArrayList<>();
    private Material currentTargetColor;
    private int currentRound = 0;
    private Location buildLocation; // Position où le build a été placé
    private BukkitTask gameTask;
    private double currentTimeToChoose = 8.0;

    // Plus besoin de constante BUILD_SIZE, on va le lire depuis la config

    private final Map<Player, Location> originalLocations = new HashMap<>();

    public MyMiniGame(List<Player> players, BlockPartyEvent plugin) {
        this.players = new ArrayList<>(players); // Liste complète (pour stop())
        this.alivePlayers = new ArrayList<>(players); // Liste des vivants (pour le jeu)
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
            player.setGameMode(GameMode.SURVIVAL);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : alivePlayers) {
                    player.sendTitle("§ePrépare-toi...", "§fDépart imminent", 10, 100, 10);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ConfigurationSection buildsSection = plugin.getConfig().getConfigurationSection("builds");
                        if (buildsSection == null || buildsSection.getKeys(false).isEmpty()) return;

                        // Choisir un build aléatoire
                        List<String> keys = new ArrayList<>(buildsSection.getKeys(false));
                        String randomKey = keys.get(new Random().nextInt(keys.size()));
                        ConfigurationSection build = buildsSection.getConfigurationSection(randomKey);
                        if (build == null) return;

                        String buildName = build.getString("name", "Inconnu");
                        Bukkit.broadcastMessage("§aBuild choisi: §e" + buildName);

                        // Récupérer les coordonnées de destination fixes depuis la config
                        ConfigurationSection destSection = plugin.getConfig().getConfigurationSection("game.clone_destination");
                        if (destSection == null) {
                            plugin.getLogger().warning("Coordonnées de destination manquantes dans la config !");
                            return;
                        }

                        String destWorldName = destSection.getString("world", "event");
                        World destWorld = Bukkit.getWorld(destWorldName);
                        if (destWorld == null) {
                            plugin.getLogger().warning("Monde de destination introuvable: " + destWorldName);
                            return;
                        }

                        // Position fixe où tous les schematics sont placés
                        buildLocation = new Location(destWorld,
                                destSection.getInt("x"),
                                destSection.getInt("y"),
                                destSection.getInt("z"));

                        // Placer le schematic avec WorldEdit
                        boolean success = Clone.pasteSchematic(buildName, buildLocation, plugin);

                        if (!success) {
                            plugin.getLogger().warning("Impossible de placer le schematic: " + buildName);
                            return;
                        }

                        plugin.getLogger().info("[BlockParty] Schematic " + buildName + " placé avec WorldEdit");

                        // Charger les couleurs de ce build
                        currentBuildColors.clear();
                        List<String> colorStrings = build.getStringList("color_blocks");
                        for (String colorString : colorStrings) {
                            try {
                                Material color = Material.valueOf(colorString);
                                currentBuildColors.add(color);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Couleur invalide: " + colorString);
                            }
                        }

                        running = true;
                        startGameLoop(); // Démarrer la logique de jeu !
                    }
                }.runTaskLater(plugin, 15 * 20L);
            }
        }.runTask(plugin);
    }

    /**
     * Vérifie si un joueur est tombé dans l'eau et l'élimine
     */
    private void checkWaterElimination(Player player) {
        if (!running || !alivePlayers.contains(player)) return;

        Location playerLoc = player.getLocation();
        Block currentBlock = playerLoc.getBlock();
        Block blockBelow = playerLoc.clone().subtract(0, 1, 0).getBlock();

        // Vérifier si le joueur est dans l'eau
        if (currentBlock.getType() == Material.WATER || blockBelow.getType() == Material.WATER) {
            eliminatePlayer(player, "est tombé dans l'eau !");
        }
    }

    /**
     * Event handler pour détecter quand un joueur tombe dans l'eau
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!running) return;

        Player player = event.getPlayer();
        if (!alivePlayers.contains(player)) return;

        // Vérifier l'eau dès que le joueur bouge
        checkWaterElimination(player);
    }

    /**
     * Élimine un joueur du jeu
     */
    private void eliminatePlayer(Player player, String reason) {
        if (!alivePlayers.contains(player)) return;

        // Retirer SEULEMENT de la liste des joueurs vivants
        alivePlayers.remove(player);

        // Ajouter au début des winners (ordre inverse d'élimination)
        winners.add(0, player);

        // Mettre en mode spectateur
        player.setGameMode(GameMode.SPECTATOR);

        // Messages
        player.sendMessage("§6[BlockParty] §cTu as été éliminé ! (" + reason + ")");
        player.sendTitle("§cÉliminé !", "", 10, 40, 10);

        // Informer les autres joueurs
        for (Player other : players) {
            other.sendMessage("§6[BlockParty] §c" + player.getName() + " éliminé ! Reste " + alivePlayers.size() + " joueur(s)");
        }

        plugin.getLogger().info("[BlockParty] " + player.getName() + " éliminé - Restants: " + alivePlayers.size());
    }

    /**
     * Démarre la boucle de jeu après le placement du schematic
     */
    private void startGameLoop() {
        if (currentBuildColors.isEmpty()) {
            plugin.getLogger().warning("Aucune couleur trouvée pour ce build !");
            return;
        }

        // Réinitialiser le temps
        currentTimeToChoose = plugin.getConfig().getDouble("game.time_to_choose", 8.0);

        // Message de début
        for (Player player : alivePlayers) {
            player.sendMessage("§6[BlockParty] §eLe jeu commence ! Préparez-vous !");
            player.sendTitle("§6BlockParty", "§ePréparez-vous !", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }

        // Attendre 3 secondes puis démarrer le premier round
        new BukkitRunnable() {
            @Override
            public void run() {
                if (running && alivePlayers.size() > 1) {
                    startRound();
                }
            }
        }.runTaskLater(plugin, 60L); // 3 secondes
    }

    /**
     * Démarre un nouveau round
     */
    private void startRound() {
        if (!running || alivePlayers.size() <= 1) {
            endGame();
            return;
        }

        currentRound++;

        // Réduire le temps de 0.5 seconde à chaque round (sauf le premier)
        if (currentRound > 1) {
            currentTimeToChoose = Math.max(currentTimeToChoose - 0.5, 0.5);
        }

        // Choisir une couleur aléatoire
        currentTargetColor = currentBuildColors.get(new Random().nextInt(currentBuildColors.size()));
        String colorName = getColorDisplayName(currentTargetColor);

        // Annoncer le round
        String timeInfo = currentTimeToChoose < 1.0 ? "§cOVERTIME" : "§e" + String.format("%.1f", currentTimeToChoose) + "s";

        for (Player player : alivePlayers) {
            player.sendMessage("§6[BlockParty] §eRound " + currentRound + " - Allez sur " + colorName + " ! (" + timeInfo + "§e)");
            player.sendTitle("Round " + currentRound, colorName + " §7- " + timeInfo, 10, 60, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
        }

        // Démarrer le countdown
        startCountdown();
    }

    /**
     * Gère le countdown du round
     */
    private void startCountdown() {
        gameTask = new BukkitRunnable() {
            double timeLeft = currentTimeToChoose;

            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    // Temps écoulé ! Faire disparaître les mauvais blocs
                    removeWrongBlocks();

                    // Attendre un peu que les joueurs tombent, puis passer au round suivant
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (running && alivePlayers.size() > 1) {
                                startRound(); // Round suivant
                            } else {
                                endGame();
                            }
                        }
                    }.runTaskLater(plugin, plugin.getConfig().getInt("game.time_between_rounds", 3) * 20L);

                    cancel();
                    return;
                }

                // Afficher le décompte dans l'action bar
                String timeDisplay;
                String colorName = getColorDisplayName(currentTargetColor);

                if (currentTimeToChoose < 1.0) {
                    // Mode OVERTIME
                    timeDisplay = "§c§lOVERTIME §7- " + colorName + " §c§l" + String.format("%.1f", timeLeft);
                } else if (timeLeft <= 3.0) {
                    // Décompte urgent
                    timeDisplay = "§c§l" + String.format("%.1f", timeLeft) + " §7- " + colorName;
                } else {
                    // Décompte normal
                    timeDisplay = "§e" + String.format("%.1f", timeLeft) + " §7- " + colorName;
                }

                // Envoyer l'action bar à tous les joueurs vivants
                for (Player player : alivePlayers) {
                    player.sendActionBar(timeDisplay);

                    // Sons selon le temps restant
                    if (timeLeft <= 3.0 && (timeLeft % 1.0) < 0.1) { // Toutes les secondes pour les 3 dernières
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    }
                }

                timeLeft -= 0.1; // Décrémenter de 0.1 seconde (2 ticks)
            }
        }.runTaskTimer(plugin, 0L, 2L); // Toutes les 2 ticks (0.1 seconde)
    }

    /**
     * Supprime tous les blocs qui ne sont pas de la bonne couleur avec WorldEdit
     */
    private void removeWrongBlocks() {
        if (buildLocation == null) return;

        // Annoncer que les blocs vont disparaître
        for (Player player : alivePlayers) {
            player.sendMessage("§6[BlockParty] §cLes mauvais blocs disparaissent !");
            player.sendTitle("§cATTENTION !", "§7Les blocs disparaissent...", 5, 30, 5);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        }

        // Lire la taille depuis la config
        int buildSize = plugin.getConfig().getInt("game.build_size", 30); // Défaut: 30

        // Définir une zone PLATE autour du build (BlockParty = sol plat)
        Location corner1 = buildLocation.clone().subtract(buildSize/2, 1, buildSize/2); // 1 bloc en dessous
        Location corner2 = buildLocation.clone().add(buildSize/2, 3, buildSize/2); // 3 blocs au-dessus (sol plat)

        // Utiliser WorldEdit pour faire disparaître tous les mauvais blocs
        boolean success = Clone.replaceBlocksExcept(
                buildLocation.getWorld(),
                corner1,
                corner2,
                currentBuildColors,  // Tous les types de blocs de couleur
                currentTargetColor   // Garder seulement cette couleur
        );

        if (!success) {
            plugin.getLogger().warning("[BlockParty] Erreur lors de la suppression des blocs !");
        } else {
            plugin.getLogger().info("[BlockParty] Blocs supprimés avec WorldEdit ! Seuls les blocs " + currentTargetColor.name() + " restent.");
        }
    }

    /**
     * Termine le jeu et détermine les gagnants
     */
    private void endGame() {

        if (gameTask != null) {
            gameTask.cancel();
        }

        // Vider l'action bar pour tous les joueurs
        for (Player player : players) {
            player.sendActionBar("");
        }

        // Déterminer le gagnant
        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);

            // IMPORTANT : Ajouter le gagnant aux winners ET le retirer des alivePlayers
            winners.add(0, winner); // Gagnant en premier
            alivePlayers.remove(winner); // Retirer pour déclencher hasWinner()

            // Messages de victoire
            for (Player player : players) {
                player.sendMessage("§6[BlockParty] §a" + winner.getName() + " a gagné !");
                player.sendTitle("§aVictoire !", "§e" + winner.getName(), 10, 60, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

            plugin.getLogger().info("[BlockParty] Gagnant: " + winner.getName());

        } else if (alivePlayers.size() > 1) {
            // Égalité - tous les joueurs restants sont gagnants
            for (Player player : new ArrayList<>(alivePlayers)) { // Copie pour éviter ConcurrentModificationException
                winners.add(0, player);
            }
            alivePlayers.clear(); // Vider pour déclencher hasWinner()

            for (Player player : players) {
                player.sendMessage("§6[BlockParty] §eÉgalité ! Tous les survivants gagnent !");
            }

        } else {
            // Aucun survivant (très rare)
            for (Player player : players) {
                player.sendMessage("§6[BlockParty] §cAucun gagnant !");
            }
        }

        // Maintenant hasWinner() retournera true car alivePlayers est vide
        // SimpleEventManager va appeler stop() qui fera le nettoyage
    }

    /**
     * Récupère le nom d'affichage d'une couleur
     */
    private String getColorDisplayName(Material color) {
        return switch (color) {
            case RED_CONCRETE -> "§cROUGE";
            case BLUE_CONCRETE -> "§9BLEU";
            case GREEN_CONCRETE -> "§aVERT";
            case YELLOW_CONCRETE -> "§eJAUNE";
            case ORANGE_CONCRETE -> "§6ORANGE";
            case PURPLE_CONCRETE -> "§5VIOLET";
            case PINK_CONCRETE -> "§dROSE";
            case CYAN_CONCRETE -> "§bCYAN";
            case LIME_CONCRETE -> "§aVERT CLAIR";
            case LIGHT_BLUE_CONCRETE -> "§bBLEU CLAIR";
            case WHITE_CONCRETE -> "§fBLANC";
            case BLACK_CONCRETE -> "§8NOIR";
            default -> color.name();
        };
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        running = false;

        // Téléporter TOUS les joueurs (y compris les éliminés) au spawn
        World spawnWorld = Bukkit.getWorld("world");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();

        for (Player player : players) { // Utilise la liste complète !
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(defaultSpawn);
            player.setCollidable(true);
            plugin.getLogger().info("[BlockParty] " + player.getName() + " téléporté au spawn de world");
        }
    }

    public List<Player> getWinners() {
        return winners;
    }

    public boolean hasWinner() {
        return alivePlayers.isEmpty(); // Utilise alivePlayers pour vérifier la fin
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (players.contains(player)) {
            removePlayer2(player);
        }
    }

    public void removePlayer2(Player player) {
        // Retirer de la liste des vivants
        if (alivePlayers.contains(player)){
            alivePlayers.remove(player);
            winners.add(0, player);
        }
        players.remove(player);

        World spawnWorld = Bukkit.getWorld("world");
        Location defaultSpawn = (spawnWorld != null) ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
        Location loc = originalLocations.getOrDefault(player, defaultSpawn);

        player.teleport(loc);
        player.sendMessage("§cTu as quitté la course.");
    }
}