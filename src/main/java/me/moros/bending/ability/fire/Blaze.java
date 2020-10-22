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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Blaze extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private final List<FireStream> streams = new ArrayList<>();
	private final Set<Block> affectedBlocks = new HashSet<>();

	public Blaze(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, Blaze.class)) return false;

		this.user = user;
		recalculateConfig();
		if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock())) {
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
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	@Override
	public void onDestroy() {
		if (!affectedBlocks.isEmpty()) user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
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

			if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) {
				return UpdateResult.REMOVE;
			}

			if (affectedBlocks.contains(block)) {
				return UpdateResult.CONTINUE;
			}
			affectedBlocks.add(block);
			return TempBlock.create(block, Material.FIRE, 500, true).map(b -> UpdateResult.CONTINUE).orElse(UpdateResult.REMOVE);
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
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

