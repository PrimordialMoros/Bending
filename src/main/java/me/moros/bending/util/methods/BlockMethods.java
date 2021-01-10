/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Campfire;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.block.data.Lightable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
	public static void lightBlock(@NonNull Block block) {
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
	 * Plays an extinguish particle and sound effect at the given block location.
	 * @param block the block to play the effect at
	 */
	public static void playLavaExtinguishEffect(@NonNull Block block) {
		Location center = block.getLocation().add(0.5, 0.7, 0.5);
		SoundUtil.LAVA_EXTINGUISH_SOUND.play(center);
		ParticleUtil.create(Particle.CLOUD, center).count(8)
			.offset(0.3, 0.3, 0.3).spawn();
	}

	/**
	 * Attempts to cool down  the given block if it's Lava.
	 * @param user the user trying to cool the lava
	 * @param block the block to check
	 * @return true if lava was cooled down, false otherwise
	 */
	public static boolean coolLava(@NonNull User user, @NonNull Block block) {
		if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) return false;
		if (MaterialUtil.isLava(block)) {
			block.setType(MaterialUtil.isSourceBlock(block) ? Material.OBSIDIAN : Material.COBBLESTONE);
			if (ThreadLocalRandom.current().nextBoolean()) playLavaExtinguishEffect(block);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to extinguish the given block if it's Fire.
	 * @param user the user trying to extinguish the fire
	 * @param block the block to check
	 * @return true if fire was extinguished, false otherwise
	 */
	public static boolean extinguishFire(@NonNull User user, @NonNull Block block) {
		if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) return false;
		if (MaterialUtil.isFire(block)) {
			block.setType(Material.AIR);
			if (ThreadLocalRandom.current().nextInt(4) == 0) {
				SoundUtil.FIRE_EXTINGUISH_SOUND.play(block.getLocation());
			}
			return true;
		}
		return false;
	}

	/**
	 * @return {@link #combineFaces(Block, Set)} with {@link #MAIN_FACES} as the provided set
	 */
	public static @NonNull Collection<@NonNull Block> combineFaces(@NonNull Block center) {
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
	public static @NonNull Collection<@NonNull Block> combineFaces(@NonNull Block center, @NonNull Set<@NonNull BlockFace> faces) {
		return Stream.concat(Stream.of(center), faces.stream().map(center::getRelative)).collect(Collectors.toList());
	}

	/**
	 * Check surrounding blocks to see if an infinite water source can be created.
	 * @param block the center block to check
	 * @return true if there 2 or more water sources around the block
	 */
	public static boolean isInfiniteWater(@NonNull Block block) {
		int sources = 0;
		for (BlockFace face : CARDINAL_FACES) {
			Block adjacent = block.getRelative(face);
			if (!TempBlock.isBendable(adjacent)) continue;
			if (MaterialUtil.isWater(adjacent) && MaterialUtil.isSourceBlock(adjacent)) {
				sources++;
			}
		}
		return sources >= 2;
	}

	/**
	 * Calculate and collect a ring of blocks.
	 * Note: ring blocks are in clockwise order and are unique.
	 * @param center the center block
	 * @param radius the radius of the circle
	 * @return a collection of blocks representing the ring
	 */
	public static @NonNull Collection<Block> createBlockRing(@NonNull Block center, double radius) {
		Vector3 centerVector = new Vector3(center).add(Vector3.HALF);
		Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.PI / (5 * radius), RotationConvention.VECTOR_OPERATOR);
		return VectorMethods.rotate(Vector3.PLUS_I.scalarMultiply(radius), rotation, NumberConversions.ceil(10 * radius))
			.stream().map(v -> centerVector.add(v).toBlock(center.getWorld())).distinct().collect(Collectors.toList());
	}

	/**
	 * Attempts to break the specified block if it's a valid plant ({@link MaterialUtil#BREAKABLE_PLANTS}).
	 * @param block the block to break
	 * @return true if the plant was broken, false otherwise
	 */
	public static boolean breakPlant(@NonNull Block block) {
		if (MaterialUtil.BREAKABLE_PLANTS.isTagged(block)) {
			if (TempBlock.manager.isTemp(block)) return false;
			block.breakNaturally(new ItemStack(Material.AIR));
			return true;
		}
		return false;
	}
}
