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

import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Campfire;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.block.data.Lightable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class BlockMethods {
	public static final Set<BlockFace> MAIN_FACES = Collections.unmodifiableSet(EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN));
	public static final Set<BlockFace> CARDINAL_FACES = Collections.unmodifiableSet(EnumSet.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH));

	public static void lightFurnaces(Block block) {
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
}
