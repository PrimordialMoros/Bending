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

package me.moros.bending.ability.water.sequences;

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
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.UserMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class FrostBreath extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<FrostStream> streams = new ArrayList<>();
	private final Set<Entity> affectedEntities = new HashSet<>();

	private boolean charging;
	private long startTime;

	public FrostBreath(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, FrostBreath.class)) return false;

		this.user = user;
		recalculateConfig();

		removalPolicy = Policies.builder().build();

		charging = true;
		startTime = System.currentTimeMillis();
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

		if (charging) {
			if (!user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("PhaseChange"))
				return UpdateResult.REMOVE;
			if (System.currentTimeMillis() > startTime + userConfig.chargeTime) {
				ParticleUtil.create(Particle.SNOW_SHOVEL, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
				if (!user.isSneaking()) {
					release();
				}
			} else {
				if (!user.isSneaking()) return UpdateResult.REMOVE;
			}
			return UpdateResult.CONTINUE;
		} else {
			streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		}

		return (charging || !streams.isEmpty()) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private void release() {
		if (!charging) return;

		Vector3 origin = user.getEyeLocation().subtract(new Vector3(0, 0.2, 0));
		Vector3 userDir = user.getDirection();
		double angleStep = FastMath.toRadians(20);
		double maxAngle = FastMath.toRadians(45);
		for (double theta = 0; theta < FastMath.PI; theta += angleStep) {
			for (double phi = 0; phi < FastMath.PI * 2; phi += angleStep) {
				double x = FastMath.cos(phi) * FastMath.sin(theta);
				double y = FastMath.cos(phi) * FastMath.cos(theta);
				double z = FastMath.sin(phi);
				Vector3 direction = new Vector3(x, y, z);
				if (Vector3.angle(direction, userDir) <= maxAngle) {
					streams.add(new FrostStream(user, new Ray(origin, direction.normalize().scalarMultiply(userConfig.range))));
				}
			}
		}

		charging = false;
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class FrostStream extends ParticleStream {
		public FrostStream(User user, Ray ray) {
			super(user, ray, 0.3, 0.5);
			livingOnly = true;
			canCollide = Block::isLiquid;
		}

		@Override
		public void render() {
			Location spawnLoc = getBukkitLocation();
			ParticleUtil.create(Particle.SNOW_SHOVEL, spawnLoc).count(2)
				.offset(0.4, 0.4, 0.4).extra(0.05).spawn();
			ParticleUtil.create(Particle.BLOCK_CRACK, spawnLoc).count(2)
				.offset(0.4, 0.4, 0.4).extra(0.05).data(Material.ICE.createBlockData()).spawn();
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			if (!affectedEntities.contains(entity)) {
				affectedEntities.add(entity);
				DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
				if (entity.isValid() && entity instanceof LivingEntity) {
					int potionDuration = NumberConversions.round(userConfig.slowDuration / 50F);
					((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, potionDuration, userConfig.power));
					ParticleUtil.create(Particle.BLOCK_CRACK, ((LivingEntity) entity).getEyeLocation()).count(5)
						.offset(0.5, 0.5, 0.5).data(Material.ICE.createBlockData()).spawn();
				}
			}
			return false;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			if (MaterialUtil.isWater(block)) {
				TempBlock.create(block, Material.ICE, true);
				if (ThreadLocalRandom.current().nextInt(6) == 0) {
					SoundUtil.ICE_SOUND.play(block.getLocation());
				}
			}
			BlockMethods.coolLava(user, block);
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.CHARGE_TIME)
		public long chargeTime;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public int power;
		@Attribute(Attribute.DURATION)
		public long slowDuration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "sequences", "frostbreath");

			cooldown = abilityNode.node("cooldown").getLong(10000);
			range = abilityNode.node("range").getDouble(7.0);
			chargeTime = abilityNode.node("charge-time").getLong(1000);
			damage = abilityNode.node("damage").getDouble(2.0);
			power = abilityNode.node("slow-power").getInt(2) - 1;
			slowDuration = abilityNode.node("slow-duration").getLong(5000);
		}
	}
}
