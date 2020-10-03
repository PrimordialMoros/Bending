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

import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;

public class SelectedSource implements State {
	private StateChain chain;
	private final User user;
	private Vector3 origin;
	private Block block;
	private Material material;

	private boolean started = false;
	private final double distanceSq;

	public SelectedSource(User user, Block block, double maxDistance) {
		this.user = user;
		this.distanceSq = maxDistance * maxDistance;
		attemptRefresh(block);
	}

	public void attemptRefresh(Block block) {
		if (block.equals(this.block)) return;
		Vector3 newOrigin = new Vector3(block).add(Vector3.HALF);
		if (user.getLocation().distanceSq(newOrigin) > distanceSq) return;
		this.block = block;
		this.origin = newOrigin;
		this.material = block.getType();
	}

	@Override
	public void start(StateChain chain) {
		if (started) return;
		this.chain = chain;
		started = true;
	}

	@Override
	public void complete() {
		if (!started) return;
		chain.getChainStore().clear();
		chain.getChainStore().add(block);
		chain.nextState();
	}

	@Override
	public UpdateResult update() {
		if (!started) return UpdateResult.REMOVE;
		if (block.getType() != material || user.getLocation().distanceSq(origin) > distanceSq)
			return UpdateResult.REMOVE;
		ParticleUtil.create(Particle.SMOKE_NORMAL, origin.toLocation(user.getWorld()).add(0, 0.5, 0)).spawn();
		return UpdateResult.CONTINUE;
	}
}
