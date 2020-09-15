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

package me.moros.bending.ability.water.util;

import me.moros.bending.util.SourceUtil;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.user.User;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.material.MaterialUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Material;
import org.bukkit.block.Block;

// TODO refactor
public class BottleReturn implements Ability {
	public static final Config config = new Config();

	private User user;
	private Config userConfig;
	private Vector3 location;
	private BendingFallingBlock source;

	public BottleReturn(Vector3 origin) {
		this.location = origin;
	}

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		source = new BendingFallingBlock(location.toLocation(user.getWorld()), Material.WATER.createBlockData(), 30000);
		return true;
	}

	@Override
	public UpdateResult update() {
		location = new Vector3(source.getFallingBlock().getLocation());
		Vector3 target = user.getEyeLocation();
		double distSq = location.distanceSq(target);
		if (distSq <= userConfig.speed * userConfig.speed) {
			SourceUtil.fillBottle(user);
			return UpdateResult.REMOVE;
		}
		if (distSq > userConfig.maxDistance * userConfig.maxDistance) {
			return UpdateResult.REMOVE;
		}

		Block block = location.toLocation(user.getWorld()).getBlock();
		if (!MaterialUtil.isTransparent(block) && block.getType() != Material.WATER && block.getType() != Material.ICE) {
			return UpdateResult.REMOVE;
		}

		if (!Game.getProtectionSystem().canBuild(user, block)) {
			return UpdateResult.REMOVE;
		}

		source.getFallingBlock().setVelocity(target.subtract(location).normalize().scalarMultiply(userConfig.speed).toVector());
		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		source.revert();
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "BottleReturn";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	public static class Config extends Configurable {
		public boolean enabled;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.RANGE)
		public double maxDistance;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("properties", "bottlebending");

			enabled = abilityNode.getNode("enabled").getBoolean(true);
			speed = abilityNode.getNode("speed").getDouble(1.0);
			maxDistance = abilityNode.getNode("max-distance").getDouble(40.0);
		}
	}
}
