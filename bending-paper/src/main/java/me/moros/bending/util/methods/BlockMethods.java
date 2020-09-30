/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util.methods;

import me.moros.bending.game.Game;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Campfire;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.block.data.Lightable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class with useful {@link Block} related methods. Note: This is not thread-safe.
 */
public final class BlockMethods {
	public static final Set<BlockFace> MAIN_FACES = Collections.unmodifiableSet(EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));
	public static final Set<BlockFace> CARDINAL_FACES = Collections.unmodifiableSet(EnumSet.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH));

	/**
	 * Attempts to light a block if it's a furnace, smoker, blast furance or campfire.
	 * @param block the block to light
	 */
	public static void lightBlock(Block block) {
		if (block.getType() == Material.FURNACE) {
			Furnace furnace = (Furnace) block.getState();
			furnace.setBurnTime((short) 800);
			furnace.update();
		} else if (block.getType() == Material.SMOKER) {
			Smoker smoker = (Smoker) block.getState();
			smoker.setBurnTime((short) 800);
			smoker.update();
		} else if (block.getType() == Material.BLAST_FURNACE) {
			BlastFurnace blastF = (BlastFurnace) block.getState();
			blastF.setBurnTime((short) 800);
			blastF.update();
		} else if (block instanceof Campfire && block instanceof Lightable) {
			((Lightable) block.getBlockData()).setLit(true);
		}
	}

	/**
	 * Attempts to extinguish nearby blocks. Fire will be put out while Lava will be turned to Obsidian or Cobblestone.
	 * @param user the user trying to extinguish the block
	 * @param center the location to check
	 * @return true if Lava was cooled down, false otherwise
	 */
	public static boolean extinguish(User user, Location center) {
		Block block = center.getBlock();
		if (!Game.getProtectionSystem().canBuild(user, block)) return false;
		boolean result = true;
		for (Block b : WorldMethods.getNearbyBlocks(center, 1.2, MaterialUtil::isFire)) {
			if (!Game.getProtectionSystem().canBuild(user, b)) continue;
			b.setType(Material.AIR);
			result = false;
		}
		if (MaterialUtil.isLava(block)) {
			block.setType(MaterialUtil.isSourceBlock(block) ? Material.OBSIDIAN : Material.COBBLESTONE);
			result = true;
		}
		return result;
	}

	/**
	 * @return {@link #combineFaces(Block, Set)} with {@link #MAIN_FACES} as the provided set
	 */
	public static Collection<Block> combineFaces(Block center) {
		return combineFaces(center, MAIN_FACES);
	}

	/**
	 * Creates a list of the center block and all surrounding blocks that share a {@link BlockFace}.
	 * @param center the center block
	 * @param faces a set containing various block faces to check
	 * @return the combined list of blocks
	 * @see #MAIN_FACES
	 * @see #CARDINAL_FACES
	 */
	public static Collection<Block> combineFaces(Block center, Set<BlockFace> faces) {
		return Stream.concat(Stream.of(center), faces.stream().map(center::getRelative)).collect(Collectors.toList());
	}
}
