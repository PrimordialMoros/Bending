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
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
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
import me.moros.bending.util.methods.EntityMethods;
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
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class Combustion extends AbilityInstance implements Ability, Explosive {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private CombustBeam beam;

	private boolean hasExploded;

	public Combustion(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (method == ActivationMethod.ATTACK) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, Combustion.class).ifPresent(Combustion::explode);
			return false;
		}

		if (user.isOnCooldown(getDescription())) return false;

		this.user = user;
		recalculateConfig();

		if (Policies.IN_LIQUID.test(user, getDescription()) || Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, Combustion.class)) {
			return false;
		}
		beam = new CombustBeam();
		removalPolicy = Policies.builder().build();
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
		if (beam.distanceTravelled > userConfig.range) {
			return UpdateResult.REMOVE;
		}
		return beam.update();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(beam.getCollider());
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		Ability collidedAbility = collision.getCollidedAbility();
		if (collidedAbility instanceof Combustion) {
			createExplosion(beam.getLocation(), userConfig.power * 2, userConfig.damage * 2);
			((Combustion) collidedAbility).hasExploded = true;
		} else if (collidedAbility instanceof Explosive || collidedAbility.getDescription().getElement() == Element.EARTH) {
			explode();
		}
	}

	@Override
	public void explode() {
		createExplosion(beam.getLocation(), userConfig.power, userConfig.damage);
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
			double distance = center.distance(EntityMethods.getEntityCenter(entity));
			double halfSize = size / 2;
			double factor = (distance <= halfSize) ? 1 : (distance - halfSize) / size;
			DamageUtil.damageEntity(entity, user, damage * factor, getDescription());
			FireTick.LARGER.apply(entity, NumberConversions.floor(userConfig.fireTick));
			return true;
		}, true, true);

		FragileStructure.tryDamageStructure(WorldMethods.getNearbyBlocks(loc, size, WaterMaterials::isIceBendable), 0);

		if (loc.getBlock().isLiquid()) return;
		Predicate<Block> predicate = b -> !MaterialUtil.isAir(b) && !MaterialUtil.isUnbreakable(b) && !b.isLiquid();
		Collection<Block> blocks = WorldMethods.getNearbyBlocks(loc, size, predicate);
		for (Block block : blocks) {
			if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) return;
			long delay = BendingProperties.EXPLOSION_REVERT_TIME + ThreadLocalRandom.current().nextInt(1000);
			TempBlock.createAir(block, delay);
		}
		for (Block block : blocks) {
			if (MaterialUtil.isIgnitable(block) && ThreadLocalRandom.current().nextInt(3) == 0) {
				TempBlock.create(block, Material.FIRE.createBlockData(), BendingProperties.FIRE_REVERT_TIME, true);
			}
		}
	}

	private class CombustBeam extends ParticleStream {
		private double randomBeamDistance = 0;
		private double distanceTravelled = 0;

		public CombustBeam() {
			super(user, user.getRay(userConfig.range), 0.35, 1);
			canCollide = Block::isLiquid;
			singleCollision = true;
			controllable = true;
			steps = 3;
		}

		@Override
		public void render() {
			distanceTravelled += speed;
			renderRing();
			Location bukkitLocation = getBukkitLocation();
			ParticleUtil.create(Particle.SMOKE_NORMAL, bukkitLocation, userConfig.particleRange).extra(0.06).spawn();
			ParticleUtil.create(Particle.FIREWORKS_SPARK, bukkitLocation, userConfig.particleRange).extra(0.06).spawn();
		}

		private void renderRing() {
			if (distanceTravelled >= randomBeamDistance) {
				SoundUtil.playSound(getBukkitLocation(), SoundUtil.COMBUSTION_SOUND.getSound(), 1.5F, 0);
				randomBeamDistance = distanceTravelled + 7 + 3 * ThreadLocalRandom.current().nextGaussian();
				double radius = ThreadLocalRandom.current().nextDouble(0.3, 0.6);
				Rotation rotation = new Rotation(user.getDirection(), FastMath.PI / 10, RotationConvention.VECTOR_OPERATOR);
				VectorMethods.rotate(Vector3.ONE, rotation, 20).forEach(v -> {
					Vector3 velocity = v.scalarMultiply(radius);
					ParticleUtil.create(Particle.FIREWORKS_SPARK, location.add(v.scalarMultiply(0.2)).toLocation(user.getWorld()), userConfig.particleRange)
						.count(0).offset(velocity.getX(), velocity.getY(), velocity.getZ()).extra(0.09).spawn();
				});
			}
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(3) == 0) {
				SoundUtil.COMBUSTION_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			explode();
			return true;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			explode();
			return true;
		}

		public @NonNull Vector3 getLocation() {
			return location;
		}
	}

	private static class Config extends Configurable {
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

		public int particleRange;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "combustion");

			cooldown = abilityNode.node("cooldown").getLong(12000);
			damage = abilityNode.node("damage").getDouble(5.0);
			power = abilityNode.node("power").getDouble(3.0);
			fireTick = abilityNode.node("fire-tick").getInt(60);
			range = abilityNode.node("range").getDouble(56.0);

			particleRange = NumberConversions.ceil(range);
		}
	}
}
