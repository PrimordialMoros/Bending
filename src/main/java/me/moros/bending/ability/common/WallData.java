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

package me.moros.bending.ability.common;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class WallData {
	private final Collection<Block> wallBlocks;
	private final Predicate<Block> predicate;
	private int health;

	private WallData(Collection<Block> wallBlocks, int health, Predicate<Block> predicate) {
		this.wallBlocks = wallBlocks;
		this.health = health;
		this.predicate = predicate;
		this.wallBlocks.forEach(b -> b.setMetadata(Metadata.DESTRUCTIBLE, Metadata.customMetadata(this)));
	}

	public int getHealth() {
		return health;
	}

	/**
	 * @return unmodifiable collection of blocks belonging to the same wall
	 */
	public Collection<Block> getWallBlocks() {
		return Collections.unmodifiableCollection(wallBlocks);
	}

	/**
	 * Attempt to subtract the specified amount of damage from this wall's health.
	 * If health drops at zero or below then the wall will break.
	 * Note: Provide a non positive damage value to instantly destroy the wall.
	 * @param damage the amount of damage to inflict
	 * @return the remaining wall health
	 */
	private int damageWall(int damage) {
		if (damage > 0 && health > damage) {
			health -= damage;
			return health;
		}
		destroyWall(this);
		return 0;
	}

	public static Optional<WallData> createWallData(@NonNull Collection<Block> blocks, int health, @NonNull Predicate<Block> predicate) {
		if (health < 0 || blocks.isEmpty()) return Optional.empty();
		return Optional.of(new WallData(blocks, health, predicate));
	}

	public static boolean attemptDamageWall(@NonNull Collection<Block> blocks, int damage) {
		for (Block block : blocks) {
			if (block.hasMetadata(Metadata.DESTRUCTIBLE)) {
				WallData wallData = (WallData) block.getMetadata(Metadata.DESTRUCTIBLE).get(0).value();
				if (wallData != null) {
					wallData.damageWall(damage);
					return true;
				}
			}
		}
		return false;
	}

	public static void destroyWall(@NonNull WallData data) {
		for (Block block : data.wallBlocks) {
			if (!data.predicate.test(block)) continue;
			Material mat = block.getType();
			TempBlock.manager.get(block).ifPresent(TempBlock::revert);
			block.removeMetadata(Metadata.DESTRUCTIBLE, Bending.getPlugin());
			Location center = block.getLocation().add(0.5, 0.5, 0.5);
			ParticleUtil.create(Particle.BLOCK_CRACK, center).count(2)
				.offset(0.3, 0.3, 0.3).data(mat.createBlockData()).spawn();
			if (ThreadLocalRandom.current().nextInt(3) == 0) {
				if (WaterMaterials.ICE_BENDABLE.isTagged(mat)) {
					SoundUtil.playSound(center, Sound.BLOCK_GLASS_BREAK, 2F, 2F);
				} else {
					SoundUtil.playSound(center, Sound.BLOCK_STONE_BREAK, 2F, 2F);
				}
			}
		}
	}
}
