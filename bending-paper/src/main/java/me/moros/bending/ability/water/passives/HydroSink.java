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
import me.moros.bending.model.ability.description.AbilityDescription;
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
		AbilityDescription desc = Game.getAbilityRegistry().getAbilityDescription("HydroSink").orElse(null);
		if (desc == null || !user.canBend(desc)) return false;

		Block block = user.getLocBlock();
		Block baseBlock = block.getRelative(BlockFace.DOWN);
		return WaterMaterials.ICE_BENDABLE.isTagged(baseBlock) ||
			WaterMaterials.PLANT_BENDABLE.isTagged(baseBlock) ||
			WaterMaterials.ICE_BENDABLE.isTagged(block) ||
			WaterMaterials.PLANT_BENDABLE.isTagged(block);
	}

	@Override
	public void destroy() {
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
	public void handleCollision(Collision collision) {
	}
}
