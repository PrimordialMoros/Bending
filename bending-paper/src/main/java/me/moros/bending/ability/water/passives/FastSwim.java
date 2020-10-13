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

import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.PotionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FastSwim implements PassiveAbility {
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
		if (!user.isValid() || !user.canBend(getDescription())) {
			return UpdateResult.CONTINUE;
		}

		if (MaterialUtil.isWater(user.getLocBlock())) {
			if (PotionUtil.canAddPotion(user, PotionEffectType.DOLPHINS_GRACE, 20, 1)) {
				user.getEntity().addPotionEffect(
					new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 1, true, false)
				);
			}
		}
		return UpdateResult.CONTINUE;
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
		return "FastSwim";
	}

	@Override
	public void onCollision(Collision collision) {
	}
}
