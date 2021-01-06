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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractWheel;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class FireWheel extends AbilityInstance implements Ability {
	public static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Wheel wheel;

	public FireWheel(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		Vector3 direction = user.getDirection().setY(0).normalize();
		Vector3 location = user.getLocation().add(direction);
		location = location.add(new Vector3(0, userConfig.radius, 0));
		if (location.toBlock(user.getWorld()).isLiquid()) return false;

		wheel = new Wheel(user, new Ray(location, direction));
		if (!wheel.resolveMovement(userConfig.radius)) return false;

		removalPolicy = Policies.builder()
			.add(new OutOfRangeRemovalPolicy(userConfig.range, location, () -> wheel.getLocation())).build();

		user.setCooldown(getDescription(), userConfig.cooldown);
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		return wheel.update();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(wheel.getCollider());
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	private class Wheel extends AbstractWheel {
		public Wheel(User user, Ray ray) {
			super(user, ray, userConfig.radius, userConfig.speed);
		}

		@Override
		public void render() {
			Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(this.ray.direction);
			Rotation rotation = new Rotation(rotateAxis, FastMath.PI / 18, RotationConvention.VECTOR_OPERATOR);
			VectorMethods.rotate(this.ray.direction.scalarMultiply(this.radius), rotation, 36).forEach(v ->
				ParticleUtil.createFire(user, location.add(v).toLocation(user.getWorld())).extra(0.01).spawn()
			);
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.FIRE_SOUND.play(location.toLocation(user.getWorld()));
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			DamageUtil.damageEntity(entity, user, userConfig.damage);
			return true;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			if (MaterialUtil.isIgnitable(block) && Bending.getGame().getProtectionSystem().canBuild(user, block)) {
				TempBlock.create(block, Material.FIRE, BendingProperties.FIRE_REVERT_TIME, true);
			}
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RADIUS)
		public double radius;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.SPEED)
		public double speed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "sequences", "firewheel");

			cooldown = abilityNode.node("cooldown").getLong(6000);
			radius = abilityNode.node("radius").getDouble(1.0);
			damage = abilityNode.node("damage").getDouble(4.0);
			range = abilityNode.node("range").getDouble(20.0);
			speed = abilityNode.node("speed").getDouble(0.55);

			abilityNode.node("speed").comment("How many blocks the wheel advances every tick.");
		}
	}
}
