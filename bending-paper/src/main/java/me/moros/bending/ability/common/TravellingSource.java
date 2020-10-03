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
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class TravellingSource implements State {
	private final Material material;
	private StateChain chain;
	private final User user;
	private Block source;

	private boolean started = false;

	protected double distanceSq;

	public TravellingSource(User user, Material material, double minDistance) {
		this.user = user;
		this.material = material;
		this.distanceSq = minDistance * minDistance;
	}

	@Override
	public void start(StateChain chain) {
		if (started) return;
		this.chain = chain;
		source = chain.getChainStore().stream().findFirst().orElse(null);
		started = source != null;
	}

	@Override
	public void complete() {
		if (!started) return;
		chain.getChainStore().clear();
		chain.getChainStore().add(source);
		chain.nextState();
	}

	@Override
	public UpdateResult update() {
		clean();
		if (!started) return UpdateResult.REMOVE;
		Vector3 target = user.getEyeLocation().floor();
		Vector3 location = new Vector3(source);

		if (target.distanceSq(location) < distanceSq) {
			//TempBlock.manager.get(block).ifPresent(TempBlock::revert);
			complete();
			return UpdateResult.CONTINUE;
		}

		if (isValid(source.getRelative(BlockFace.UP)) && source.getY() < user.getHeadBlock().getY()) {
			source = source.getRelative(BlockFace.UP);
		} else if (isValid(source.getRelative(BlockFace.DOWN)) && source.getY() > user.getHeadBlock().getY()) {
			source = source.getRelative(BlockFace.DOWN);
		} else {
			Vector3 direction = target.subtract(location).normalize();
			Block nextBlock = location.add(direction).toBlock(user.getWorld());
			if (source.equals(nextBlock)) {
				source = findPath(nextBlock);
			} else {
				source = nextBlock;
			}
		}
		if (source == null || !isValid(source) || !Game.getProtectionSystem().canBuild(user, source)) {
			return UpdateResult.REMOVE;
		}
		TempBlock.create(source, material);
		return UpdateResult.CONTINUE;
	}

	private Block findPath(Block check) {
		Location dest = user.getHeadBlock().getLocation();
		Block result = null;
		double minDistance = Double.MAX_VALUE;
		for (BlockFace face : BlockMethods.CARDINAL_FACES) {
			Block block = check.getRelative(face);
			if (!isValid(block)) continue;
			double d = block.getLocation().distanceSquared(dest);
			if (d < minDistance) {
				minDistance = d;
				result = block;
			}
		}
		return result;
	}

	private boolean isValid(Block block) {
		if (material == Material.WATER) return MaterialUtil.isTransparent(block) || MaterialUtil.isWater(block);
		return MaterialUtil.isTransparent(block);
	}

	private void clean() {
		TempBlock.manager.get(source).filter(tb -> material == tb.getBlock().getType()).ifPresent(TempBlock::revert);
	}
}
