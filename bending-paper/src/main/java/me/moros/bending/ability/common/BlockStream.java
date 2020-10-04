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
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

public abstract class BlockStream implements State {
	private StateChain chain;
	private final User user;
	private final Collection<Collider> colliders = new ArrayList<>();
	private final Material material;

	protected Deque<Block> stream;
	protected Vector3 direction;

	private boolean started = false;

	protected final boolean controllable;
	protected double range;

	public BlockStream(User user, Material material, double range, boolean controllable) {
		this.user = user;
		this.material = material;
		this.range = range;
		this.controllable = controllable;
	}

	@Override
	public void start(StateChain chain) {
		if (started) return;
		this.chain = chain;
		stream = new ArrayDeque<>();
		chain.getChainStore().stream().filter(this::isValid).forEach(block -> {
			if (TempBlock.create(block, material).isPresent()) stream.addLast(block);
		});

		started = !stream.isEmpty();
	}

	@Override
	public void complete() {
		if (!started) return;
		chain.nextState();
	}

	@Override
	public UpdateResult update() {
		if (!started || stream.stream().noneMatch(this::isValid)) return UpdateResult.REMOVE;

		Block head = stream.getFirst();
		Vector3 current = new Vector3(head).add(Vector3.HALF);
		if (current.distanceSq(user.getLocation()) > range * range) {
			return UpdateResult.REMOVE;
		}
		if (controllable || direction == null) {
			Vector3 targetLoc = new Vector3(WorldMethods.getTargetEntity(user, range).map(Entity::getLocation)
				.orElseGet(() -> WorldMethods.getTarget(user.getWorld(), user.getRay(range), Collections.singleton(material))));
			direction = targetLoc.subtract(current).normalize();
		}

		head = current.add(direction).toBlock(user.getWorld());
		if (!Game.getProtectionSystem().canBuild(user, head)) {
			return UpdateResult.REMOVE;
		}

		clean(stream.removeLast());
		if (MaterialUtil.isTransparent(head) || MaterialUtil.isWater(head)) {
			if (material == Material.WATER && MaterialUtil.isWater(head)) {
				ParticleUtil.create(Particle.WATER_BUBBLE, head.getLocation().add(0.5, 0.5, 0.5))
					.count(5).offset(0.25, 0.25, 0.25).spawn();
			} else {
				TempBlock.create(head, Material.WATER);
			}
			stream.addFirst(head);
		}

		colliders.clear();
		boolean hit = false;
		for (Block block : stream) {
			Collider collider = AABB.BLOCK_BOUNDS.at(new Vector3(block));
			colliders.add(collider);
			hit |= CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true, false);
		}

		return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	public abstract boolean onEntityHit(Entity entity);

	public Collection<Collider> getColliders() {
		return colliders;
	}

	public boolean isValid(Block block) {
		if (material == Material.WATER) return MaterialUtil.isWater(block);
		return material == block.getType();
	}

	public void cleanAll() {
		stream.forEach(this::clean);
	}

	private void clean(Block block) {
		TempBlock.manager.get(block).filter(tb -> isValid(tb.getBlock())).ifPresent(TempBlock::revert);
	}
}
