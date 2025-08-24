package org.blockPartyGame.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public class Clone {

    /**
     * Clone une zone d'une position à une autre en utilisant Bukkit pur
     *
     * @param c1 Premier coin de la zone source
     * @param c2 Deuxième coin de la zone source
     * @param dest Position de destination (coin où coller)
     * @return true si le clonage a réussi, false sinon
     */
    public static boolean cloneAreaBukkit(Location c1, Location c2, Location dest) {
        World world = c1.getWorld();
        if (world == null) {
            System.err.println("[BlockParty] Clone: Monde null !");
            return false;
        }

        try {
            // Calculer les dimensions de la zone source
            int minX = Math.min(c1.getBlockX(), c2.getBlockX());
            int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
            int minY = Math.min(c1.getBlockY(), c2.getBlockY());
            int maxY = Math.max(c1.getBlockY(), c2.getBlockY());
            int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
            int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());

            // Position de destination
            int destX = dest.getBlockX();
            int destY = dest.getBlockY();
            int destZ = dest.getBlockZ();

            int blocksCloned = 0;

            // Copier bloc par bloc
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        // Bloc source
                        Block sourceBlock = world.getBlockAt(x, y, z);

                        // Calculer la position relative et la position de destination
                        int offsetX = x - minX;
                        int offsetY = y - minY;
                        int offsetZ = z - minZ;

                        // Bloc de destination
                        Block destBlock = world.getBlockAt(
                                destX + offsetX,
                                destY + offsetY,
                                destZ + offsetZ
                        );

                        // Copier le type de bloc
                        destBlock.setType(sourceBlock.getType());
                        destBlock.setBlockData(sourceBlock.getBlockData());

                        blocksCloned++;
                    }
                }
            }

            System.out.println("[BlockParty] " + blocksCloned + " blocs clonés de (" +
                    minX + "," + minY + "," + minZ + ") vers (" + destX + "," + destY + "," + destZ + ")");

            return true;

        } catch (Exception e) {
            System.err.println("[BlockParty] Erreur lors du clonage:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remplace des types de blocs spécifiques par de l'air dans une région (version Bukkit pure)
     *
     * @param world Le monde
     * @param pos1 Première coordonnée de la région
     * @param pos2 Deuxième coordonnée de la région
     * @param blocksToReplace Liste des matériaux à remplacer
     * @param keepBlock Le type de bloc à garder (ne pas remplacer)
     * @return true si réussi
     */
    public static boolean replaceBlocksExcept(World world, Location pos1, Location pos2,
                                              List<Material> blocksToReplace, Material keepBlock) {
        if (world == null || pos1 == null || pos2 == null || blocksToReplace == null) {
            return false;
        }

        try {
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

            int blocksReplaced = 0;

            // Parcourir tous les blocs de la région
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        Material type = block.getType();

                        // Si c'est un bloc de couleur mais pas la bonne couleur à garder
                        if (blocksToReplace.contains(type) && type != keepBlock) {
                            block.setType(Material.AIR);
                            blocksReplaced++;
                        }
                    }
                }
            }

            System.out.println("[BlockParty] " + blocksReplaced + " blocs remplacés par de l'air. Couleur gardée: " + keepBlock.name());
            return true;

        } catch (Exception e) {
            System.err.println("[BlockParty] Erreur lors du remplacement des blocs:");
            e.printStackTrace();
            return false;
        }
    }
}