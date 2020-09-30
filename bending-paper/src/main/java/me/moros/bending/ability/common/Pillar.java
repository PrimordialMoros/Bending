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

package me.moros.bending.ability.common;

import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * You should validate that the pillar blocks have bendable materials
 */
public class Pillar {
	private final User user;
	private final Block origin;
	private final BlockFace direction;
	private final Collection<Block> pillarBlocks;

	private final int length;
	private final long interval;
	private final long duration;

	private int currentLength;
	private long nextUpdateTime;

	private Pillar(User user, Block origin, BlockFace direction, int length, long interval, long duration) {
		this.user = user;
		this.origin = origin;
		this.direction = direction;
		this.length = length;
		this.interval = interval;
		this.duration = duration;
		this.pillarBlocks = new ArrayList<>();
		nextUpdateTime = 0;
	}

	public UpdateResult update() {
		long time = System.currentTimeMillis();
		if (time < nextUpdateTime) return UpdateResult.CONTINUE;
		nextUpdateTime = time + interval;

		Block currentIndex = origin.getRelative(direction, ++currentLength);
		if (currentLength >= length) return UpdateResult.REMOVE;

		// Move entities into the air when standing on top of the RaiseEarth.
		AABB collider = AABB.BLOCK_BOUNDS.at(new Vector3(currentIndex));
		CollisionUtil.handleEntityCollisions(user, collider, entity -> {
			entity.setVelocity(normalizeVelocity(entity.getVelocity()));
			return false;
		}, true, true);

		return move(currentIndex) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private boolean move(Block newBlock) {
		if (newBlock.isPassable()) return false;
		pillarBlocks.add(newBlock);
		for (int i = 1; i < length; i++) {
			Block forwardBlock = newBlock.getRelative(direction.getOppositeFace(), i);
			Block backwardBlock = forwardBlock.getRelative(direction.getOppositeFace());
			TempBlock.create(forwardBlock, MaterialUtil.getSolidType(backwardBlock.getBlockData()), duration);
		}
		TempBlock.create(newBlock.getRelative(direction.getOppositeFace(), length), Material.AIR, duration);
		return true;
	}

	private Vector normalizeVelocity(Vector velocity) {
		switch (direction) {
			case NORTH:
			case SOUTH:
				return velocity.setX(direction.getDirection().getX());
			case EAST:
			case WEST:
				return velocity.setZ(direction.getDirection().getZ());
			case UP:
			case DOWN:
			default:
				return velocity.setY(direction.getDirection().getY());
		}
	}

	public Collection<Block> getPillarBlocks() {
		return Collections.unmodifiableCollection(pillarBlocks);
	}

	public static Optional<Pillar> buildPillar(User user, Block origin, BlockFace direction, int length) {
		return buildPillar(user, origin, direction, length, 100, 0);
	}

	public static Optional<Pillar> buildPillar(User user, Block origin, BlockFace direction, int length, long interval, long duration) {
		if (user == null || origin == null || !BlockMethods.MAIN_FACES.contains(direction) || length < 1)
			return Optional.empty();
		int maxLength = validate(user, origin, direction, length);
		if (maxLength < 1) return Optional.empty();
		return Optional.of(new Pillar(user, origin, direction, maxLength, interval, duration));
	}

	/**
	 * Check region protections and return maximum valid length in blocks
	 */
	private static int validate(User user, Block origin, BlockFace direction, int max) {
		if (Game.getProtectionSystem().canBuild(user, origin)) return 0;
		for (int i = 1; i < max; i++) {
			Block forwardBlock = origin.getRelative(direction, i);
			Block backwardBlock = origin.getRelative(direction.getOppositeFace(), i);
			if (!Game.getProtectionSystem().canBuild(user, forwardBlock) || !Game.getProtectionSystem().canBuild(user, backwardBlock)) {
				return i;
			}
		}
		return max;
	}
}
