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
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class Blaze implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private final List<FireStream> streams = new ArrayList<>();

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		if (!Game.getProtectionSystem().canBuild(user, user.getLocBlock())) {
			return false;
		}

		Vector3 origin = user.getLocation();
		Vector3 dir = user.getDirection().setY(0).normalize();
		Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.PI / 20, RotationConvention.VECTOR_OPERATOR);
		if (method == ActivationMethod.PUNCH) {
			int steps = userConfig.arc / 8;
			VectorMethods.createArc(dir, rotation, steps).forEach(v -> streams.add(new FireStream(new Ray(origin, v))));
		} else {
			VectorMethods.rotate(dir, rotation, 40).forEach(v -> streams.add(new FireStream(new Ray(origin, v))));
		}

		user.setCooldown(getDescription(), userConfig.cooldown);
		return true;
	}

	@Override
	public void recalculateConfig() {
		this.userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
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
		return "Blaze";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	private class FireStream {
		private long nextUpdate;
		private Vector3 location;
		private final Vector3 origin;
		private final Vector3 direction;

		FireStream(Ray ray) {
			origin = ray.origin;
			direction = ray.direction.normalize();
			location = origin.add(direction);
		}

		private UpdateResult update() {
			long time = System.currentTimeMillis();
			if (time <= nextUpdate) return UpdateResult.CONTINUE;
			nextUpdate = time + 70;

			if (location.distanceSq(origin) > userConfig.range * userConfig.range) {
				return UpdateResult.REMOVE;
			}

			location = location.add(direction);
			Block block = location.toBlock(user.getWorld());
			if (!MaterialUtil.isIgnitable(block)) {
				if (MaterialUtil.isIgnitable(block.getRelative(BlockFace.UP))) {
					location.add(Vector3.PLUS_J);
					block = block.getRelative(BlockFace.UP);
				} else if (MaterialUtil.isIgnitable(block.getRelative(BlockFace.DOWN))) {
					location.add(Vector3.MINUS_J);
					block = block.getRelative(BlockFace.DOWN);
				} else {
					return UpdateResult.REMOVE;
				}
			}

			if (!Game.getProtectionSystem().canBuild(user, block)) {
				return UpdateResult.REMOVE;
			}
			return TempBlock.create(block, Material.FIRE, 500).map(b -> UpdateResult.CONTINUE).orElse(UpdateResult.REMOVE);
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RANGE)
		public double range;
		public int arc;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "blaze");

			cooldown = abilityNode.getNode("cooldown").getLong(500);
			range = abilityNode.getNode("range").getDouble(7.0);
			arc = abilityNode.getNode("arc").getInt(32);

			abilityNode.getNode("arc").setComment("How large the entire arc is in degrees");
		}
	}
}

