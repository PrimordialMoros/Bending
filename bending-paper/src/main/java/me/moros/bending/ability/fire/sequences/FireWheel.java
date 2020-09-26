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

package me.moros.bending.ability.fire.sequences;

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.predicates.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.List;

// TODO refactor and optimize
public class FireWheel implements Ability {
	public static Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;

	private Vector3 location;
	private Vector3 direction;
	private Disk collider;

	private enum CollisionResolution {NONE, RESOLVED, FAILURE}

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		direction = user.getDirection().setY(0).normalize();
		location = user.getLocation().add(direction.scalarMultiply(userConfig.speed));

		location = location.add(new Vector3(0, userConfig.radius, 0));

		if (location.toBlock(user.getWorld()).isLiquid()) return false;

		AABB bounds = new AABB(new Vector3(-0.1, -userConfig.radius, -userConfig.radius), new Vector3(0.1, userConfig.radius, userConfig.radius));
		OBB obb = new OBB(bounds, new Rotation(Vector3.PLUS_J, FastMath.toRadians(user.getEntity().getLocation().getYaw()), RotationConvention.VECTOR_OPERATOR));
		Sphere sphere = new Sphere(location, userConfig.radius);
		collider = new Disk(obb, sphere);

		CollisionResolution resolution = resolveInitialCollisions(WorldMethods.getNearbyBlocks(location.toLocation(user.getWorld()), userConfig.radius * 5), userConfig.radius * 2);

		if (resolution == CollisionResolution.FAILURE) {
			return false;
		}

		removalPolicy = CompositeRemovalPolicy.defaults().add(new OutOfRangeRemovalPolicy(userConfig.range, location, () -> location)).build();
		user.setCooldown(this, userConfig.cooldown);

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

		location = location.add(direction.scalarMultiply(userConfig.speed));

		if (!Game.getProtectionSystem().canBuild(user, location.toBlock(user.getWorld()))) {
			return UpdateResult.REMOVE;
		}

		if (!resolveMovement()) {
			return UpdateResult.REMOVE;
		}

		if (location.subtract(new Vector3(0, userConfig.radius + 1, 0)).toBlock(user.getWorld()).isLiquid()) {
			return UpdateResult.REMOVE;
		}

		Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(direction).normalize();
		Rotation rotation = new Rotation(rotateAxis, FastMath.PI / 36, RotationConvention.VECTOR_OPERATOR);
		VectorMethods.rotate(direction, rotation, 72).forEach(v ->
			ParticleUtil.createFire(user, location.add(v).toLocation(user.getWorld())).count(1).spawn()
		);

		boolean hit = CollisionUtil.handleEntityCollisions(user, collider.addPosition(location), (entity) -> {
			DamageUtil.damageEntity(entity, user, userConfig.damage);
			return true;
		}, true);

		if (hit) {
			return UpdateResult.REMOVE;
		}

		return UpdateResult.CONTINUE;
	}

	// Try to resolve wheel location by checking collider-block intersections.
	private boolean resolveMovement() {
		List<Block> nearbyBlocks = WorldMethods.getNearbyBlocks(location.toLocation(user.getWorld()), userConfig.radius * 5);
		CollisionResolution resolution = resolveInitialCollisions(nearbyBlocks, 1.0);
		if (resolution == CollisionResolution.FAILURE) {
			return false;
		}
		Disk checkCollider = collider.addPosition(location);
		// Try to fall if the block below doesn't have a bounding box.
		Block blockBelow = location.subtract(new Vector3(0, collider.getHalfExtents().getY() + 1, 0)).toBlock(user.getWorld());
		if (resolution == CollisionResolution.NONE && AABBUtils.getBlockBounds(blockBelow).getHalfExtents().getY() == 0) {
			location = location.add(Vector3.MINUS_J);
			checkCollider = collider.addPosition(location);
			for (Block block : nearbyBlocks) {
				AABB blockBounds = AABBUtils.getBlockBounds(block);
				if (blockBounds.intersects(checkCollider)) {
					// Go back up if there is a collision after falling.
					this.location = location.add(Vector3.PLUS_J);
					checkCollider = collider.addPosition(location);
					break;
				}
			}
		}
		// Check if there's any final collisions after all movements.
		for (Block block : nearbyBlocks) {
			AABB blockBounds = AABBUtils.getBlockBounds(block);
			if (blockBounds.intersects(checkCollider)) {
				return false;
			}
		}
		return true;
	}

	private CollisionResolution resolveInitialCollisions(List<Block> nearbyBlocks, double maxResolution) {
		Disk checkCollider = collider.addPosition(location);
		for (Block block : nearbyBlocks) {
			AABB blockBounds = AABBUtils.getBlockBounds(block);
			if (blockBounds.intersects(checkCollider)) {
				Vector3 pos = blockBounds.getPosition();
				double blockY = blockBounds.getHalfExtents().getY() * 2;
				if (pos.getY() > location.getY()) {
					// There's a collision from above, so don't resolve it.
					return CollisionResolution.FAILURE;
				}
				double colliderY = collider.getHalfExtents().getY();
				double resolution = (colliderY + blockY) - (location.getY() - pos.getY());
				if (resolution > maxResolution) {
					return CollisionResolution.FAILURE;
				} else {
					location = location.add(new Vector3(0, resolution, 0));
					return CollisionResolution.RESOLVED;
				}
			}
		}
		return CollisionResolution.NONE;
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
		return "FireWheel";
	}

	@Override
	public List<Collider> getColliders() {
		return Collections.singletonList(collider.addPosition(location));
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RADIUS)
		public double radius;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.COLLISION_RADIUS)
		public double entityCollisionRadius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "sequences", "firewheel");

			cooldown = abilityNode.getNode("cooldown").getLong(6000);
			radius = abilityNode.getNode("radius").getDouble(1.0);
			damage = abilityNode.getNode("damage").getDouble(4.0);
			range = abilityNode.getNode("range").getDouble(20.0);
			speed = abilityNode.getNode("speed").getDouble(0.55);
			entityCollisionRadius = abilityNode.getNode("collision-radius").getDouble(1.5);

			abilityNode.getNode("speed").setComment("How many blocks the wheel advances every tick.");
		}
	}
}

