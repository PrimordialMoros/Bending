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

package me.moros.bending.ability.fire;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractBurst;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.methods.UserMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FireBurst extends AbstractBurst implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private boolean released;
	private long startTime;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		released = false;
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (!released) {
			boolean charged = isCharged();
			if (charged) {
				ParticleUtil.createFire(user, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
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
	public void onDestroy() {
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull String getName() {
		return "FireBurst";
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
	}

	public boolean isCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
	}

	public static void activateCone(User user) {
		Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, FireBurst.class)
			.ifPresent(b -> b.release(true));
	}

	private void release(boolean cone) {
		if (released || !isCharged()) return;
		released = true;
		if (cone) {
			createCone(user, FireBlast.class, userConfig.coneRange);
		} else {
			createSphere(user, FireBlast.class, userConfig.sphereRange);
		}
		setRenderInterval(100);
		setRenderParticleCount(1);
		user.setCooldown(getDescription(), userConfig.cooldown);
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

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "fireburst");

			cooldown = abilityNode.getNode("cooldown").getLong(0);
			chargeTime = abilityNode.getNode("charge-time").getInt(3500);
			coneRange = abilityNode.getNode("cone-range").getDouble(16);
			sphereRange = abilityNode.getNode("sphere-range").getDouble(12);
		}
	}
}
