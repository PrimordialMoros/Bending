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

package me.moros.bending.ability.water;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.PotionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HealingWaters implements Ability {
	private static final org.bukkit.attribute.Attribute healthAttribute = org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH;
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private long startTime;
	private long nextTime;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
		startTime = System.currentTimeMillis();
		nextTime = 0;
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
		if (!MaterialUtil.isWater(user.getLocBlock())) {
			return UpdateResult.REMOVE;
		}
		long time = System.currentTimeMillis();
		if (time > startTime + userConfig.chargeTime) {
			if (time > nextTime) {
				nextTime = time + 250;
				LivingEntity target = getTarget();
				ParticleUtil.createRGB(target.getLocation().add(0, target.getEyeHeight() / 2, 0), "00ffff")
					.count(6).offset(0.35, 0.35, 0.35).spawn();
				removeNegativeEffects(target);
				if (!healEntity(target)) return UpdateResult.REMOVE;
			}
		} else {
			ParticleUtil.createRGB(UserMethods.getMainHandSide(user).toLocation(user.getWorld()), "00ffff").spawn();
		}
		return UpdateResult.CONTINUE;
	}

	private void removeNegativeEffects(LivingEntity livingEntity) {
		livingEntity.getActivePotionEffects().stream().map(PotionEffect::getType).filter(PotionUtil::isNegative)
			.forEach(livingEntity::removePotionEffect);
	}

	private boolean healEntity(LivingEntity livingEntity) {
		AttributeInstance attributeInstance = livingEntity.getAttribute(healthAttribute);
		if (attributeInstance != null && livingEntity.getHealth() < attributeInstance.getValue()) {
			PotionEffectType type = PotionEffectType.REGENERATION;
			if (PotionUtil.canAddPotion(user, type, 20, userConfig.power - 1)) {
				user.getEntity().addPotionEffect(new PotionEffect(type, 60, userConfig.power - 1));
			}
			return true;
		}
		return false;
	}

	private boolean isValidTarget(LivingEntity entity) {
		return entity != null && entity.isValid()
			&& !MaterialUtil.isWater(entity.getLocation().getBlock())
			&& entity.getLocation().distanceSquared(user.getEntity().getLocation()) > userConfig.range * userConfig.range
			&& user.getEntity().hasLineOfSight(entity);
	}

	private LivingEntity getTarget() {
		LivingEntity target = WorldMethods.getTargetEntity(user, userConfig.range).orElse(null);
		return isValidTarget(target) ? target : user.getEntity();
	}

	@Override
	public void onDestroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull String getName() {
		return "HealingWaters";
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.STRENGTH)
		public int power;
		@Attribute(Attribute.CHARGE_TIME)
		public long chargeTime;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "healingwaters");

			cooldown = abilityNode.getNode("cooldown").getLong(3000);
			range = abilityNode.getNode("range").getDouble(5.0);
			power = abilityNode.getNode("power").getInt(2);
			chargeTime = abilityNode.getNode("charge-time").getLong(2000);
		}
	}
}