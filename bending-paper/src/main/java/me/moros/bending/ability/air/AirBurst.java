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

package me.moros.bending.ability.air;

import me.moros.bending.ability.common.BurstAbility;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.methods.UserMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class AirBurst extends BurstAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private long startTime;
	private boolean released;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		this.released = false;
		if (method == ActivationMethod.FALL) {
			if (user.getEntity().getFallDistance() < userConfig.fallThreshold || user.isSneaking()) {
				return false;
			}
			release(false);
		}
		this.startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		this.userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (!released) {
			boolean charged = isCharged();
			if (charged) {
				ParticleUtil.createAir(UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
				if (!user.isSneaking()) {
					release(false);
				}
			} else {
				if (!user.isSneaking()) return UpdateResult.REMOVE;
			}
			return UpdateResult.CONTINUE;
		}
		return updateBurst();
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
		return "AirBurst";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	public boolean isCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
	}

	public static void activateCone(User user) {
		Game.getAbilityInstanceManager(user.getWorld()).getFirstInstance(user, AirBurst.class)
			.ifPresent(b -> b.release(true));
	}

	private void release(boolean cone) {
		if (released || !isCharged()) return;
		released = true;
		if (cone) {
			createCone(user, AirBlast.class, userConfig.coneRange);
		} else {
			createSphere(user, AirBlast.class, userConfig.sphereRange);
		}
		setRenderInterval(100);
		setRenderParticleCount(1);
		user.setCooldown(this, userConfig.cooldown);
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.CHARGE_TIME)
		public int chargeTime;

		@Attribute(Attributes.RANGE)
		public double sphereRange;
		@Attribute(Attributes.RANGE)
		public double coneRange;
		public int fallThreshold;
		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airburst");

			cooldown = abilityNode.getNode("cooldown").getLong(0);
			chargeTime = abilityNode.getNode("charge-time").getInt(3500);
			coneRange = abilityNode.getNode("cone-range").getDouble(16);
			sphereRange = abilityNode.getNode("sphere-range").getDouble(12);
			fallThreshold = abilityNode.getNode("fall-threshold").getInt(10);
		}
	}
}
