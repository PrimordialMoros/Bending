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

import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Updatable;
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
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

public class Pillar implements Updatable {
	private final User user;
	private final Block origin;
	private final BlockFace direction;
	private final Collection<Block> pillarBlocks;
	private final Predicate<Block> predicate;

	private final int length;
	private final long interval;
	private final long duration;

	private int currentLength;
	private long nextUpdateTime;

	private Pillar(User user, Block origin, BlockFace direction, int length, long interval, long duration, Predicate<Block> predicate) {
		this.user = user;
		this.origin = origin;
		this.direction = direction;
		this.length = length;
		this.interval = interval;
		this.duration = duration;
		this.predicate = predicate;
		this.pillarBlocks = new ArrayList<>();
		currentLength = 0;
		nextUpdateTime = 0;
	}

	@Override
	public @NonNull UpdateResult update() {
		if (currentLength >= length) return UpdateResult.REMOVE;

		long time = System.currentTimeMillis();
		if (time < nextUpdateTime) return UpdateResult.CONTINUE;
		nextUpdateTime = time + interval;

		Block currentIndex = origin.getRelative(direction, ++currentLength);
		AABB collider = AABB.BLOCK_BOUNDS.at(new Vector3(currentIndex));
		CollisionUtil.handleEntityCollisions(user, collider, entity -> {
			entity.setVelocity(normalizeVelocity(entity.getVelocity()));
			return false;
		}, true, true); // Push entities

		return move(currentIndex) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private boolean move(Block newBlock) {
		if (!newBlock.isPassable()) return false;
		pillarBlocks.add(newBlock);
		for (int i = 0; i < length; i++) {
			Block forwardBlock = newBlock.getRelative(direction.getOppositeFace(), i);
			Block backwardBlock = forwardBlock.getRelative(direction.getOppositeFace());
			if (!predicate.test(backwardBlock)) return false;
			TempBlock.create(forwardBlock, MaterialUtil.getSolidType(backwardBlock.getBlockData()), duration, true);
		}
		TempBlock.create(newBlock.getRelative(direction.getOppositeFace(), length), Material.AIR, duration, true);
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
		return buildPillar(user, origin, direction, length, 100, 0, x -> true);
	}

	public static Optional<Pillar> buildPillar(User user, Block origin, BlockFace direction, int length, Predicate<Block> predicate) {
		return buildPillar(user, origin, direction, length, 100, 0, predicate);
	}

	public static Optional<Pillar> buildPillar(User user, Block origin, BlockFace direction, int length, long duration) {
		return buildPillar(user, origin, direction, length, 100, duration, x -> true);
	}

	public static Optional<Pillar> buildPillar(User user, Block origin, BlockFace direction, int length, long duration, Predicate<Block> predicate) {
		return buildPillar(user, origin, direction, length, 100, duration, predicate);
	}

	public static Optional<Pillar> buildPillar(User user, Block origin, BlockFace direction, int length, long interval, long duration) {
		return buildPillar(user, origin, direction, length, interval, duration, x -> true);
	}

	public static Optional<Pillar> buildPillar(User user, Block origin, BlockFace direction, int length, long interval, long duration, Predicate<Block> predicate) {
		if (user == null || origin == null || !BlockMethods.MAIN_FACES.contains(direction) || length < 1)
			return Optional.empty();
		int maxLength = validate(user, origin, direction, length);
		if (maxLength < 1) return Optional.empty();
		return Optional.of(new Pillar(user, origin, direction, maxLength, interval, duration, predicate));
	}

	/**
	 * Check region protections and return maximum valid length in blocks
	 */
	private static int validate(User user, Block origin, BlockFace direction, int max) {
		if (!Bending.getGame().getProtectionSystem().canBuild(user, origin)) return 0;
		for (int i = 1; i <= max; i++) {
			Block forwardBlock = origin.getRelative(direction, i);
			Block backwardBlock = origin.getRelative(direction.getOppositeFace(), i - 1);
			if (!Bending.getGame().getProtectionSystem().canBuild(user, forwardBlock) || !Bending.getGame().getProtectionSystem().canBuild(user, backwardBlock)) {
				return i;
			}
		}
		return max;
	}
}
