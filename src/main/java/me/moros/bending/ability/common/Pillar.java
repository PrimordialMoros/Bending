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

import me.moros.atlas.cf.checker.index.qual.NonNegative;
import me.moros.atlas.cf.checker.index.qual.Positive;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Pillar implements Updatable {
	private final User user;
	private final Block origin;
	private final BlockFace direction;
	private final BlockFace opposite;
	private final Collection<Block> pillarBlocks;
	private final Predicate<Block> predicate;

	private final boolean solidify;
	private final int length;
	private final long interval;
	private final long duration;

	private int currentLength;
	private long nextUpdateTime;

	protected Pillar(@NonNull PillarBuilder builder) {
		this.user = builder.user;
		this.origin = builder.origin;
		this.direction = builder.direction;
		this.opposite = direction.getOppositeFace();
		this.length = builder.length;
		this.interval = builder.interval;
		this.duration = builder.duration;
		this.predicate = builder.predicate;
		this.pillarBlocks = new ArrayList<>(length);
		solidify = (direction != BlockFace.UP && direction != BlockFace.DOWN);
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
		CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true, true); // Push entities

		return move(currentIndex) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private boolean move(Block newBlock) {
		if (MaterialUtil.isLava(newBlock)) return false;
		if (!MaterialUtil.isTransparent(newBlock) && newBlock.getType() != Material.WATER) return false;
		BlockMethods.breakPlant(newBlock);

		for (int i = 0; i < length; i++) {
			Block forwardBlock = newBlock.getRelative(opposite, i);
			Block backwardBlock = forwardBlock.getRelative(opposite);
			if (!predicate.test(backwardBlock)) {
				TempBlock.create(forwardBlock, Material.AIR, duration, true);
				playSound(forwardBlock);
				return false;
			}
			BlockData data = solidify ? MaterialUtil.getSolidType(backwardBlock.getBlockData()) : backwardBlock.getBlockData();
			TempBlock.create(forwardBlock, data, duration, true);
		}
		pillarBlocks.add(newBlock);
		TempBlock.create(newBlock.getRelative(opposite, length), Material.AIR, duration, true);
		playSound(newBlock);
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

	public @NonNull Collection<Block> getPillarBlocks() {
		return Collections.unmodifiableCollection(pillarBlocks);
	}

	public @NonNull Block getOrigin() {
		return origin;
	}

	public void playSound(@NonNull Block block) {
		SoundUtil.EARTH_SOUND.play(block.getLocation());
	}

	public boolean onEntityHit(@NonNull Entity entity) {
		entity.setVelocity(normalizeVelocity(entity.getVelocity()));
		return true;
	}

	public static @NonNull PillarBuilder builder(@NonNull User user, @NonNull Block origin) {
		return builder(user, origin, Pillar::new);
	}

	public static <T extends Pillar> @NonNull PillarBuilder builder(@NonNull User user, @NonNull Block origin, @NonNull Function<PillarBuilder, T> constructor) {
		return new PillarBuilder(user, origin, constructor);
	}

	public static class PillarBuilder {
		private final User user;
		private final Block origin;
		private final Function<PillarBuilder, ? extends Pillar> constructor;
		private BlockFace direction = BlockFace.UP;
		private int length;
		private long interval = 100;
		private long duration = BendingProperties.EARTHBENDING_REVERT_TIME;
		private Predicate<Block> predicate = b -> true;

		public <T extends Pillar> PillarBuilder(@NonNull User user, @NonNull Block origin, @NonNull Function<PillarBuilder, T> constructor) {
			this.user = user;
			this.origin = origin;
			this.constructor = constructor;
		}

		public @NonNull PillarBuilder setDirection(@NonNull BlockFace direction) {
			if (!BlockMethods.MAIN_FACES.contains(direction)) {
				throw new IllegalStateException("Pillar direction must be one of the 6 main BlockFaces!");
			}
			this.direction = direction;
			return this;
		}

		public @NonNull PillarBuilder setInterval(@NonNegative long interval) {
			this.interval = interval;
			return this;
		}

		public @NonNull PillarBuilder setDuration(@NonNegative long duration) {
			this.duration = duration;
			return this;
		}

		public @NonNull PillarBuilder setPredicate(@NonNull Predicate<Block> predicate) {
			this.predicate = predicate;
			return this;
		}

		public Optional<Pillar> build(@Positive int length) {
			int maxLength = validate(length);
			if (maxLength < 1) return Optional.empty();
			this.length = maxLength;
			return Optional.of(constructor.apply(this));
		}

		/**
		 * Check region protections and return maximum valid length in blocks
		 */
		private int validate(int max) {
			if (!Bending.getGame().getProtectionSystem().canBuild(user, origin)) return 0;
			for (int i = 0; i < max; i++) {
				Block forwardBlock = origin.getRelative(direction, i + 1);
				Block backwardBlock = origin.getRelative(direction.getOppositeFace(), i);
				if (!Bending.getGame().getProtectionSystem().canBuild(user, forwardBlock) || !Bending.getGame().getProtectionSystem().canBuild(user, backwardBlock)) {
					return i;
				}
			}
			return max;
		}
	}
}
