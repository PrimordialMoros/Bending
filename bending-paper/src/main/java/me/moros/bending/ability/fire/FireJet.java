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

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.predicates.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Flight;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;

public class FireJet implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Flight flight;

	private double speed;
	private long duration;
	private long startTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		if (Game.getAbilityManager(user.getWorld()).hasAbility(user, FireJet.class)) {
			return false;
		}

		this.user = user;
		recalculateConfig();

		Block block = user.getLocBlock();
		boolean ignitable = MaterialUtil.isIgnitable(block);
		if (!ignitable && !MaterialUtil.isAir(block.getType())) {
			return false;
		}

		if (!Game.getProtectionSystem().canBuild(user, block)) {
			return false;
		}

		speed = userConfig.speed;
		duration = userConfig.duration;

		flight = Flight.get(user);
		if (ignitable) TempBlock.create(block, Material.FIRE, 3000, true);

		removalPolicy = Policies.builder()
			.add(Policies.IN_LIQUID)
			.add(new ExpireRemovalPolicy(userConfig.duration))
			.build();

		user.setCooldown(this, userConfig.cooldown);
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		// scale down to 0.5 speed near the end
		double factor = 1 - ((System.currentTimeMillis() - startTime) / (2.0 * duration));

		user.getEntity().setVelocity(user.getDirection().scalarMultiply(speed * factor).toVector());
		user.getEntity().setFallDistance(0);
		ParticleUtil.createFire(user, user.getEntity().getLocation()).count(10)
			.offset(0.3, 0.3, 0.3).spawn();
		ParticleUtil.create(Particle.SMOKE_NORMAL, user.getEntity().getLocation()).count(5)
			.offset(0.3, 0.3, 0.3).spawn();

		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		flight.release();
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "FireJet";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	public void setSpeed(double newSpeed) {
		this.speed = newSpeed;
	}

	public void setDuration(long duration) {
		this.duration = duration;
		removalPolicy = Policies.builder().add(Policies.IN_LIQUID).add(new ExpireRemovalPolicy(duration)).build();
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.DURATION)
		private long duration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "firejet");

			cooldown = abilityNode.getNode("cooldown").getLong(7000);
			speed = abilityNode.getNode("speed").getDouble(0.8);
			duration = abilityNode.getNode("duration").getLong(2000);
		}
	}
}
