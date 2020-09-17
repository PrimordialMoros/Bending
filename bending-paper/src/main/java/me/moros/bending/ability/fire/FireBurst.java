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
import org.apache.commons.math3.util.FastMath;

// TODO add range for burst and test activation
public class FireBurst extends BurstAbility {
	public static Config config = new Config();

	private User user;
	private Config userConfig;
	private long startTime;
	private boolean released;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		this.startTime = System.currentTimeMillis();
		this.released = false;

		return true;
	}

	@Override
	public void recalculateConfig() {
		this.userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (!released) {
			boolean coneCharged = isConeCharged();
			boolean sphereCharged = isSphereCharged();

			if (coneCharged || sphereCharged) {
				ParticleUtil.createFire(user, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
			}

			if (!user.isSneaking()) {
				if (sphereCharged) {
					releaseSphere();
					return UpdateResult.REMOVE;
				} else if (coneCharged) {
					releaseCone();
					return UpdateResult.REMOVE;
				}
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
		return "FireBurst";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	public boolean isSphereCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.sphereChargeTime;
	}

	public boolean isConeCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.coneChargeTime;
	}

	public static void activateCone(User user) {
		for (FireBurst burst : Game.getAbilityInstanceManager(user.getWorld()).getPlayerInstances(user, FireBurst.class)) {
			if (!burst.released && burst.isConeCharged()) burst.releaseCone();
		}
	}

	private void releaseSphere() {
		double angle = FastMath.toRadians(10);
		createBurst(user, 0, FastMath.PI, angle, 0, FastMath.PI * 2, angle, FireBlast.class);
		setRenderInterval(userConfig.sphereRenderInterval);
		setRenderParticleCount(userConfig.sphereParticlesPerBlast);
		this.released = true;
		user.setCooldown(this, userConfig.sphereCooldown);
	}

	private void releaseCone() {
		createCone(user, FireBlast.class);
		setRenderInterval(userConfig.coneRenderInterval);
		setRenderParticleCount(userConfig.coneParticlesPerBlast);
		this.released = true;
		user.setCooldown(this, userConfig.coneCooldown);
	}

	public static class Config extends Configurable {
		public boolean enabled;

		public int sphereParticlesPerBlast;
		public int sphereRenderInterval;
		@Attribute(Attributes.CHARGE_TIME)
		public int sphereChargeTime;
		@Attribute(Attributes.COOLDOWN)
		public long sphereCooldown;

		public int coneParticlesPerBlast;
		public int coneRenderInterval;
		@Attribute(Attributes.CHARGE_TIME)
		public int coneChargeTime;
		@Attribute(Attributes.COOLDOWN)
		public long coneCooldown;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "fireburst");

			enabled = abilityNode.getNode("enabled").getBoolean(true);

			CommentedConfigurationNode sphereNode = abilityNode.getNode("sphere");
			sphereRenderInterval = sphereNode.getNode("render-interval").getInt(100);
			sphereParticlesPerBlast = sphereNode.getNode("particles-per-blast").getInt(1);
			sphereChargeTime = sphereNode.getNode("charge-time").getInt(3500);
			sphereCooldown = sphereNode.getNode("cooldown").getLong(0);

			CommentedConfigurationNode coneNode = abilityNode.getNode("cone");
			coneRenderInterval = coneNode.getNode("render-interval").getInt(100);
			coneParticlesPerBlast = coneNode.getNode("particles-per-blast").getInt(1);
			coneChargeTime = coneNode.getNode("charge-time").getInt(1750);
			coneCooldown = coneNode.getNode("cooldown").getLong(0);
		}
	}
}
