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

package me.moros.bending.ability.fire;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.WallData;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class Combustion extends AbilityInstance implements Ability, Explosive {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Vector3 location;
	private Sphere collider;

	private boolean hasExploded;
	private double distanceTravelled;
	private double randomBeamDistance;

	public Combustion(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (Policies.IN_LIQUID.test(user, getDescription()) || Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, Combustion.class)) {
			return false;
		}
		location = user.getEyeLocation();
		removalPolicy = Policies.builder()
			.add(new OutOfRangeRemovalPolicy(userConfig.range, user.getEyeLocation(), () -> location)).build();
		collider = new Sphere(location, 1);

		user.setCooldown(getDescription(), userConfig.cooldown);
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (hasExploded || removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		collider = new Sphere(location, 1);
		if (CollisionUtil.handleEntityCollisions(user, collider, entity -> true, true, false, true)) {
			explode();
			return UpdateResult.REMOVE;
		}
		return advanceLocation() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private boolean advanceLocation() {
		Vector direction = user.getDirection().scalarMultiply(0.4).toVector();
		Location bukkitLocation = location.toLocation(user.getWorld());
		if (distanceTravelled >= randomBeamDistance) {
			SoundUtil.playSound(bukkitLocation, SoundUtil.COMBUSTION_SOUND.getSound(), 1.5F, 0);
			randomBeamDistance = distanceTravelled + 7 + 3 * ThreadLocalRandom.current().nextGaussian();
			double radius = ThreadLocalRandom.current().nextDouble(0.3, 0.6);
			Rotation rotation = new Rotation(user.getDirection(), FastMath.PI / 10, RotationConvention.VECTOR_OPERATOR);
			VectorMethods.rotate(Vector3.ONE, rotation, 20).forEach(v -> {
				Vector3 velocity = v.scalarMultiply(radius);
				ParticleUtil.create(Particle.FIREWORKS_SPARK, bukkitLocation.clone().add(v.scalarMultiply(0.2).toVector()), userConfig.particleRange)
					.count(0).offset(velocity.getX(), velocity.getY(), velocity.getZ()).extra(0.09).spawn();
			});
		}
		for (int i = 0; i < 5; i++) {
			distanceTravelled += 0.4;
			bukkitLocation.add(direction);
			ParticleUtil.create(Particle.SMOKE_NORMAL, bukkitLocation, userConfig.particleRange).extra(0.06).spawn();
			ParticleUtil.create(Particle.FIREWORKS_SPARK, bukkitLocation, userConfig.particleRange).extra(0.06).spawn();
			if (i % 2 != 0) {
				Block block = bukkitLocation.getBlock();
				if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) {
					return false;
				}
				if (ThreadLocalRandom.current().nextInt(3) == 0) {
					SoundUtil.COMBUSTION_SOUND.play(bukkitLocation);
				}
				if (block.isLiquid() || !MaterialUtil.isTransparent(block)) {
					createExplosion(new Vector3(bukkitLocation), userConfig.power, userConfig.damage);
					return false;
				}
			}
		}
		location = new Vector3(bukkitLocation);
		return true;
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(collider);
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		Ability collidedAbility = collision.getCollidedAbility();
		if (collidedAbility instanceof Combustion) {
			createExplosion(location, userConfig.power * 2, userConfig.damage * 2);
			((Combustion) collidedAbility).hasExploded = true;
		} else if (collidedAbility instanceof Explosive || collidedAbility.getDescription().getElement() == Element.EARTH) {
			createExplosion(location, userConfig.power, userConfig.damage);
		}
	}

	public static void explode(User user) {
		Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, Combustion.class).ifPresent(Combustion::explode);
	}

	@Override
	public void explode() {
		createExplosion(location, userConfig.power, userConfig.damage);
	}

	private void createExplosion(Vector3 center, double size, double damage) {
		if (hasExploded) return;
		hasExploded = true;
		Location loc = center.toLocation(user.getWorld());
		ParticleUtil.create(Particle.FLAME, loc, userConfig.particleRange).extra(0.5).count(20)
			.offset(1, 1, 1).spawn();
		ParticleUtil.create(Particle.SMOKE_LARGE, loc, userConfig.particleRange).extra(0.5).count(20)
			.offset(1, 1, 1).spawn();
		ParticleUtil.create(Particle.FIREWORKS_SPARK, loc, userConfig.particleRange).extra(0.5).count(20)
			.offset(1, 1, 1).spawn();
		ParticleUtil.create(Particle.EXPLOSION_HUGE, loc, userConfig.particleRange).extra(0.5).count(5)
			.offset(1, 1, 1).spawn();
		SoundUtil.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE);

		Sphere collider = new Sphere(center, size);
		CollisionUtil.handleEntityCollisions(user, collider, entity -> {
			double distance = center.distance(VectorMethods.getEntityCenter(entity));
			double halfSize = size / 2;
			double factor = (distance <= halfSize) ? 1 : (distance - halfSize) / size;
			DamageUtil.damageEntity(entity, user, damage * factor, getDescription());
			FireTick.LARGER.apply(entity, NumberConversions.floor(userConfig.fireTick));
			return true;
		}, true, true);

		if (userConfig.damageBlocks && !loc.getBlock().isLiquid()) {
			WallData.attemptDamageWall(WorldMethods.getNearbyBlocks(loc, size, WaterMaterials::isIceBendable), 0);
			Predicate<Block> predicate = b -> !MaterialUtil.isAir(b) && !MaterialUtil.isUnbreakable(b) && !b.isLiquid();
			for (Block block : WorldMethods.getNearbyBlocks(loc, size, predicate)) {
				if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) break;
				Material mat = (ThreadLocalRandom.current().nextInt(3) == 0 && MaterialUtil.isIgnitable(block)) ? Material.FIRE : Material.AIR;
				TempBlock.create(block, mat, BendingProperties.EXPLOSION_REVERT_TIME + ThreadLocalRandom.current().nextInt(1000), true);
			}
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public double power;
		@Attribute(Attribute.DURATION)
		public int fireTick;
		@Attribute(Attribute.RANGE)
		public double range;

		public boolean damageBlocks;
		public int particleRange;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "combustion");

			cooldown = abilityNode.node("cooldown").getLong(10000);
			damage = abilityNode.node("damage").getDouble(6.0);
			power = abilityNode.node("power").getDouble(3.0);
			fireTick = abilityNode.node("fire-tick").getInt(60);
			range = abilityNode.node("range").getDouble(80.0);

			damageBlocks = abilityNode.node("damage-blocks").getBoolean(true);
			particleRange = NumberConversions.ceil(range);
		}
	}
}
