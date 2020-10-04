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
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;

import java.util.List;
import java.util.stream.Collectors;

public class Ring implements State {
	private StateChain chain;
	private final User user;
	private final Material material;
	private Block lastBlock;
	private List<Block> ring;

	private boolean started = false;
	private double radius;
	private int index = -1;

	public Ring(User user, Material material, double radius) {
		this.user = user;
		this.material = material;
		this.radius = radius;
	}

	@Override
	public void start(StateChain chain) {
		if (started) return;
		this.chain = chain;
		chain.getChainStore().stream().findFirst().ifPresent(this::createRing);
		lastBlock = user.getLocBlock();
		started = ring != null && !ring.isEmpty();
	}

	@Override
	public void complete() {
		if (!started) return;
		chain.getChainStore().clear();
		if (index <= 0) {
			chain.getChainStore().addAll(ring);
		} else {
			chain.getChainStore().addAll(ring.subList(index, ring.size()));
			chain.getChainStore().addAll(ring.subList(0, index));
		}
		chain.nextState();
	}

	@Override
	public UpdateResult update() {
		if (!started) return UpdateResult.REMOVE;
		Block current = user.getLocBlock();
		if (!Game.getProtectionSystem().canBuild(user, current)) {
			return UpdateResult.REMOVE;
		}
		index = ++index % ring.size();
		Block head = ring.get(index);
		if (!current.equals(lastBlock)) {
			createRing(head);
			index = -1;
			lastBlock = current;
		}
		if (material == Material.WATER && MaterialUtil.isWater(head)) {
			ParticleUtil.create(Particle.WATER_BUBBLE, head.getLocation().add(0.5, 0.5, 0.5))
				.count(5).offset(0.25, 0.25, 0.25).spawn();
		} else if (MaterialUtil.isTransparent(head)) {
			TempBlock.create(head, material, 600);
		}
		return UpdateResult.CONTINUE;
	}

	public void incrementRadius() {
		if (radius > 8) return; // Max radius 8
		radius++;
		lastBlock = null;
	}

	public void decrementRadius() {
		if (radius < 2) return; // Min radius 2
		radius--;
		lastBlock = null;
	}

	protected void createRing(Block start) {
		Vector3 center = new Vector3(user.getHeadBlock()).add(Vector3.HALF);
		Vector3 dir = new Vector3(start).subtract(user.getEyeLocation().floor()).setY(0).normalize().scalarMultiply(radius);
		Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.PI / (5 * radius), RotationConvention.VECTOR_OPERATOR);
		ring = VectorMethods.rotateInverse(dir, rotation, NumberConversions.ceil(10 * radius)).stream()
			.map(v -> center.add(v).toBlock(user.getWorld()))
			.distinct().collect(Collectors.toList());
	}
}
