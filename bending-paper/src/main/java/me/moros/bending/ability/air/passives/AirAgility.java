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

package me.moros.bending.ability.air.passives;

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.PotionUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AirAgility implements PassiveAbility {
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
		if (user.isDead() || !user.canBend(getDescription())) {
			return UpdateResult.CONTINUE;
		}
		if (userConfig.jumpAmplifier > 0) {
			handlePotionEffect(PotionEffectType.JUMP, userConfig.jumpAmplifier - 1);
		}
		if (userConfig.speedAmplifier > 0) {
			handlePotionEffect(PotionEffectType.SPEED, userConfig.speedAmplifier - 1);
		}
		return UpdateResult.CONTINUE;
	}

	private void handlePotionEffect(PotionEffectType type, int amplifier) {
		if (PotionUtil.canAddPotion(user, type, 20, amplifier)) {
			user.getEntity().addPotionEffect(new PotionEffect(type, 100, amplifier, true, false));
		}
	}

	@Override
	public void destroy() {
		user.getEntity().removePotionEffect(PotionEffectType.JUMP);
		user.getEntity().removePotionEffect(PotionEffectType.SPEED);
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "AirAgility";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.STRENGTH)
		public int speedAmplifier;
		@Attribute(Attributes.STRENGTH)
		public int jumpAmplifier;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "passives", "airagility");

			speedAmplifier = abilityNode.getNode("speed-amplifier").getInt(2);
			jumpAmplifier = abilityNode.getNode("jump-amplifier").getInt(2);
		}
	}
}
