package org.blockPartyGame.utils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class Clone {

    /**
     * Place un schematic à une position donnée
     *
     * @param schematicName Nom du fichier schematic (sans extension)
     * @param location Position où placer le schematic
     * @param plugin Instance du plugin pour accéder aux dossiers
     * @return true si réussi
     */
    public static boolean pasteSchematic(String schematicName, Location location, JavaPlugin plugin) {
        try {
            World world = BukkitAdapter.adapt(location.getWorld());

            // Chemin vers le schematic
            File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
            File schematicFile = new File(schematicsFolder, schematicName + ".schem");

            if (!schematicFile.exists()) {
                System.err.println("[BlockParty] Schematic introuvable : " + schematicFile.getPath());
                return false;
            }

            // Charger le schematic
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                System.err.println("[BlockParty] Format non reconnu : " + schematicFile.getName());
                return false;
            }

            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            }

            // Placer le schematic
            try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                        .build();

                Operations.complete(operation);
                System.out.println("[BlockParty] Schematic " + schematicName + " placé à " +
                        location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                return true;
            }

        } catch (IOException | WorldEditException e) {
            System.err.println("[BlockParty] Erreur placement schematic " + schematicName);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime tous les blocs SAUF un type spécifique dans une zone
     *
     * @param world Le monde
     * @param pos1 Premier coin de la zone
     * @param pos2 Deuxième coin de la zone
     * @param blocksToReplace Liste des blocs à supprimer
     * @param keepBlock Type de bloc à garder (ne pas supprimer)
     * @return true si réussi
     */
    public static boolean replaceBlocksExcept(org.bukkit.World world, Location pos1, Location pos2,
                                              List<Material> blocksToReplace, Material keepBlock) {
        try {
            World weWorld = BukkitAdapter.adapt(world);

            BlockVector3 min = BlockVector3.at(
                    Math.min(pos1.getBlockX(), pos2.getBlockX()),
                    Math.min(pos1.getBlockY(), pos2.getBlockY()),
                    Math.min(pos1.getBlockZ(), pos2.getBlockZ())
            );

            BlockVector3 max = BlockVector3.at(
                    Math.max(pos1.getBlockX(), pos2.getBlockX()),
                    Math.max(pos1.getBlockY(), pos2.getBlockY()),
                    Math.max(pos1.getBlockZ(), pos2.getBlockZ())
            );

            CuboidRegion region = new CuboidRegion(weWorld, min, max);
            int blocksReplaced = 0;

            try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1)) {

                for (BlockVector3 point : region) {
                    com.sk89q.worldedit.world.block.BlockState currentBlock = editSession.getBlock(point);
                    Material currentMaterial = BukkitAdapter.adapt(currentBlock.getBlockType());

                    // Si c'est dans la liste à supprimer ET ce n'est pas le bloc à garder
                    if (blocksToReplace.contains(currentMaterial) && currentMaterial != keepBlock) {
                        editSession.setBlock(point, BukkitAdapter.adapt(Material.AIR.createBlockData()));
                        blocksReplaced++;
                    }
                }
            }

            System.out.println("[BlockParty] " + blocksReplaced + " blocs supprimés. Gardé: " + keepBlock.name());
            return true;

        } catch (WorldEditException e) {
            System.err.println("[BlockParty] Erreur suppression blocs");
            e.printStackTrace();
            return false;
        }
    }
}