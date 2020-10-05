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

import me.moros.bending.ability.water.*;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

// TODO maybe use dolphin's grace instead?
public class FastSwim implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (!user.isValid() || !user.isSneaking() || !MaterialUtil.isWater(user.getLocBlock()) || !user.canBend(getDescription())) {
			return UpdateResult.CONTINUE;
		}
		if (Game.getAbilityManager(user.getWorld()).hasAbility(user, WaterSpout.class)) return UpdateResult.CONTINUE;
		if (user.getSelectedAbility().map(desc -> !desc.isActivatedBy(ActivationMethod.SNEAK)).orElse(true)) {
			user.getEntity().setVelocity(user.getDirection().scalarMultiply(userConfig.speed).toVector());
		}
		return UpdateResult.CONTINUE;
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
		return "FastSwim";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.SPEED)
		public double speed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "passives", "fastswim");
			speed = abilityNode.getNode("speed").getDouble(0.6);
		}
	}
}
