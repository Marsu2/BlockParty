package org.boatRaceGame.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SmoothCanon {

    private static final Set<UUID> activePlayers = new HashSet<>();

    public static void launchBoatWithSnowball(JavaPlugin plugin, Boat boat, Location start, String blockType) {
        Player player = (Player) boat.getPassengers().get(0);

        // Prevent multiple simultaneous launches
        if (activePlayers.contains(player.getUniqueId())) {
            player.sendMessage("§cTu es déjà en train d'être propulsé !");
            return;
        }
        activePlayers.add(player.getUniqueId());

        // Apply speed effect for FOV
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 99, false, false, false));

        World world = start.getWorld();

        // Read from config
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("boat-race.cannon-blocks." + blockType);
        if (section == null) {
            player.sendMessage("§cAucun paramétrage de canon trouvé pour ce bloc : " + blockType);
            activePlayers.remove(player.getUniqueId());
            return;
        }

        int offsetX = section.getInt("offset-x", 0);
        int offsetY = section.getInt("offset-y", 0);
        int offsetZ = section.getInt("offset-z", 0);
        double speed = section.getDouble("speed", plugin.getConfig().getDouble("boat-race.default-canon-speed", 0.7));

        Location end = start.clone().add(offsetX, offsetY, offsetZ);

        // Play initial sound
        world.playSound(start, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);

        // Launch Snowball as projectile
        Snowball snowball = (Snowball) world.spawnEntity(start, EntityType.SNOWBALL);
        snowball.setSilent(true);
        snowball.setShooter(player);

        Vector velocity = end.toVector().subtract(start.toVector()).normalize().multiply(speed);
        snowball.setVelocity(velocity);

        // Ride the snowball
        snowball.addPassenger(boat);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removePotionEffect(PotionEffectType.SPEED);
                activePlayers.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 85L);
    }
}
