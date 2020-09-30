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
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.FireTick;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.predicates.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class Combustion implements Ability, Explosive {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;

	private Vector3 location;
	private Sphere collider;

	private boolean hasExploded;
	private double distanceTravelled;
	private double randomBeamDistance;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (Policies.IN_LIQUID.test(user, getDescription()) || Game.getAbilityManager(user.getWorld()).hasAbility(user, Combustion.class)) {
			return false;
		}
		location = user.getEyeLocation();
		removalPolicy = CompositeRemovalPolicy.defaults()
			.add(new OutOfRangeRemovalPolicy(userConfig.range, user.getEyeLocation(), () -> location)).build();
		collider = new Sphere(location, userConfig.collisionRadius);

		user.setCooldown(this, userConfig.cooldown);
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (hasExploded || removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		collider = new Sphere(location, userConfig.collisionRadius);
		if (CollisionUtil.handleEntityCollisions(user, collider, entity -> true, true)) {
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
				if (!Game.getProtectionSystem().canBuild(user, block)) {
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
	public void destroy() {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "Combustion";
	}

	@Override
	public Collection<Collider> getColliders() {
		return Collections.singletonList(collider);
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.getSecondAbility() instanceof Combustion) {
			createExplosion(location, userConfig.power * 2, userConfig.damage * 2);
		} else if (collision.getSecondAbility() instanceof Explosive || collision.getSecondAbility().getDescription().getElement() == Element.EARTH) {
			createExplosion(location, userConfig.power, userConfig.damage);
		}
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	public static void explode(User user) {
		Game.getAbilityManager(user.getWorld()).getFirstInstance(user, Combustion.class).ifPresent(Combustion::explode);
	}

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
			DamageUtil.damageEntity(entity, user, damage, getDescription());
			FireTick.LARGER.apply(entity, userConfig.fireTick);
			return true;
		}, true, true);

		if (userConfig.damageBlocks && !loc.getBlock().isLiquid()) {
			Predicate<Block> predicate = b -> !MaterialUtil.isAir(b) && !MaterialUtil.isUnbreakable(b) && !b.isLiquid();
			for (Block block : WorldMethods.getNearbyBlocks(loc, size, predicate)) {
				if (!Game.getProtectionSystem().canBuild(user, block)) break;
				boolean canIgnite = block.getRelative(BlockFace.DOWN).getType().isSolid();
				Material mat = (ThreadLocalRandom.current().nextInt(3) == 0 && canIgnite) ? Material.FIRE : Material.AIR;
				TempBlock.create(block, mat, userConfig.regenTime + ThreadLocalRandom.current().nextInt(1000));
			}
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.STRENGTH)
		public double power;
		@Attribute(Attributes.DURATION)
		public int fireTick;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.COLLISION_RADIUS)
		public double collisionRadius;
		public boolean damageBlocks;
		public long regenTime;
		private int particleRange;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "combustion");

			cooldown = abilityNode.getNode("cooldown").getLong(10000);
			damage = abilityNode.getNode("damage").getDouble(6.0);
			power = abilityNode.getNode("power").getDouble(3.0);
			fireTick = abilityNode.getNode("fire-tick").getInt(60);
			range = abilityNode.getNode("range").getDouble(80.0);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(1.4);
			damageBlocks = abilityNode.getNode("damage-blocks").getBoolean(true);
			regenTime = abilityNode.getNode("regen-time").getLong(15000);
			particleRange = NumberConversions.ceil(range);
		}
	}
}
