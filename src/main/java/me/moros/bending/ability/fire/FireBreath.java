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
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
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
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class FireBreath extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<FireStream> streams = new ArrayList<>();

	public FireBreath(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, FireBreath.class)) return false;

		if (Policies.IN_LIQUID.test(user, getDescription())) return false;

		this.user = user;
		recalculateConfig();

		removalPolicy = Policies.builder()
			.add(Policies.NOT_SNEAKING)
			.add(Policies.IN_LIQUID)
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
		Vector3 offset = new Vector3(0, -0.1, 0);
		Ray ray = new Ray(user.getEyeLocation().add(offset), user.getDirection().scalarMultiply(userConfig.range));
		streams.add(new FireStream(ray));
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

	private class FireStream extends ParticleStream {
		private double distanceTravelled = 0;

		public FireStream(Ray ray) {
			super(user, ray, 0.4, 0.5);
			canCollide = Block::isLiquid;
		}

		@Override
		public void render() {
			distanceTravelled += speed;
			Location spawnLoc = getBukkitLocation();
			double offset = 0.2 * distanceTravelled;
			collider = new Sphere(location, collisionRadius + offset);
			ParticleUtil.createFire(user, spawnLoc).count(NumberConversions.ceil(0.75 * distanceTravelled))
				.offset(offset, offset, offset).extra(0.02).spawn();
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(3) == 0) {
				SoundUtil.FIRE_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			double factor = 1 - FastMath.min(0.9, distanceTravelled / maxRange);
			DamageUtil.damageEntity(entity, user, factor * userConfig.damage, getDescription());
			FireTick.LARGER.apply(entity, NumberConversions.ceil(factor * userConfig.fireTick));
			return false;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			// TODO melt snow/ice
			Block above = block.getRelative(BlockFace.UP);
			if (MaterialUtil.isIgnitable(above) && Bending.getGame().getProtectionSystem().canBuild(user, above)) {
				TempBlock.create(above, Material.FIRE.createBlockData(), BendingProperties.FIRE_REVERT_TIME, true);
			}
			return true;
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.DURATION)
		public int fireTick;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "firebreath");

			cooldown = abilityNode.node("cooldown").getLong(12000);
			range = abilityNode.node("range").getDouble(9.0);
			duration = abilityNode.node("duration").getLong(2000);
			damage = abilityNode.node("damage").getDouble(0.5);
			fireTick = abilityNode.node("fire-tick").getInt(40);
		}
	}
}
