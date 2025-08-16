package org.blockPartyGame.utils;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;

import org.bukkit.Location;
import org.bukkit.World;

private void cloneAreaWorldEdit(Location c1, Location c2, Location dest) {
    World bukkitWorld = c1.getWorld();
    if (bukkitWorld == null) return;

    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);

    // Définir la région du build
    BlockVector3 v1 = BlockVector3.at(c1.getBlockX(), c1.getBlockY(), c1.getBlockZ());
    BlockVector3 v2 = BlockVector3.at(c2.getBlockX(), c2.getBlockY(), c2.getBlockZ());
    CuboidRegion region = new CuboidRegion(weWorld, v1, v2);

    // Créer le clipboard
    Clipboard clipboard = new BlockArrayClipboard(region);

    // Copier les blocs de la région dans le clipboard
    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
        region.forEach(block -> {
            clipboard.setBlock(block, editSession.getBlock(block));
        });
    }

    // Coller le clipboard à la destination
    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
        Operations.complete(
                new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(dest.getBlockX(), dest.getBlockY(), dest.getBlockZ()))
                        .ignoreAirBlocks(false) // copier aussi l'air pour écraser ce qu'il y a dessous
                        .build()
        );
    } catch (WorldEditException e) {
        e.printStackTrace();
    }
}

