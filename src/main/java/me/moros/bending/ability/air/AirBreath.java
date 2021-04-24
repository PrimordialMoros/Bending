/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class AirBreath extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<AirStream> streams = new ArrayList<>();

	public AirBreath(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, AirBreath.class)) return false;

		this.user = user;
		recalculateConfig();

		removalPolicy = Policies.builder()
			.add(Policies.NOT_SNEAKING)
			.add(new ExpireRemovalPolicy(userConfig.duration))
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.build();

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
		user.getEntity().setRemainingAir(user.getEntity().getRemainingAir() - 5);
		Vector3 offset = new Vector3(0, -0.1, 0);
		Ray ray = new Ray(user.getEyeLocation().add(offset), user.getDirection().scalarMultiply(userConfig.range));
		streams.add(new AirStream(ray));
		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	@Override
	public void onDestroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class AirStream extends ParticleStream {
		private double distanceTravelled = 0;

		public AirStream(Ray ray) {
			super(user, ray, userConfig.speed, 0.5);
			canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
			livingOnly = false;
		}

		@Override
		public void render() {
			distanceTravelled += speed;
			Location spawnLoc = getBukkitLocation();
			double offset = 0.15 * distanceTravelled;
			collider = new Sphere(location, collisionRadius + offset);
			if (MaterialUtil.isWater(spawnLoc.getBlock())) {
				ParticleUtil.create(Particle.WATER_BUBBLE, spawnLoc).count(3).offset(offset, offset, offset).spawn();
			} else {
				ParticleUtil.createAir(spawnLoc).count(NumberConversions.ceil(distanceTravelled)).offset(offset, offset, offset).spawn();
			}
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(3) == 0) {
				SoundUtil.AIR_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			entity.setVelocity(ray.direction.normalize().scalarMultiply(userConfig.knockback).clampVelocity());
			entity.setFireTicks(0);
			if (entity instanceof LivingEntity) {
				LivingEntity livingEntity = (LivingEntity) entity;
				livingEntity.setRemainingAir(livingEntity.getRemainingAir() + 1);
			}
			return false;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			if (BlockMethods.tryExtinguishFire(user, block)) return false;
			BlockMethods.tryCoolLava(user, block);
			if (!MaterialUtil.isTransparentOrWater(block) && user.getPitch() > 30) {
				user.getEntity().setVelocity(user.getDirection().scalarMultiply(-userConfig.knockback).clampVelocity());
				user.getEntity().setFireTicks(0);
			}
			return !MaterialUtil.isWater(block);
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.SPEED)
		public double speed;
		@Attribute(Attribute.STRENGTH)
		public double knockback;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airbreath");

			cooldown = abilityNode.node("cooldown").getLong(5000);
			range = abilityNode.node("range").getDouble(7.0);
			duration = abilityNode.node("duration").getLong(1000);
			speed = abilityNode.node("speed").getDouble(1.0);
			knockback = abilityNode.node("knockback").getDouble(0.5);
		}
	}
}
