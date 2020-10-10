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

package me.moros.bending.ability.water.passives;

import me.moros.bending.game.Game;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class HydroSink implements PassiveAbility {
	private User user;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		return true;
	}

	@Override
	public void recalculateConfig() {
	}

	@Override
	public UpdateResult update() {
		return UpdateResult.CONTINUE;
	}

	public static boolean canHydroSink(User user) {
		if (!Game.getAbilityManager(user.getWorld()).hasAbility(user, HydroSink.class)) {
			return false;
		}
		if (!Game.getAbilityRegistry().getAbilityDescription("HydroSink").map(user::canBend).orElse(false))
			return false;

		Block block = user.getLocBlock();
		if (WaterMaterials.ALL.isTagged(block)) return false;
		Block baseBlock = block.getRelative(BlockFace.DOWN);
		return block.isPassable() && WaterMaterials.ALL.isTagged(baseBlock);
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "HydroSink";
	}

	@Override
	public void onCollision(Collision collision) {
	}
}
