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

package me.moros.bending.ability.water;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.PotionUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class WaterWave extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Set<Entity> affectedEntities = new HashSet<>();

	private boolean ice = false;
	private long startTime;

	public WaterWave(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, WaterWave.class)) {
			return false;
		}

		this.user = user;
		recalculateConfig();

		removalPolicy = Policies.builder().add(new ExpireRemovalPolicy(userConfig.duration)).build();
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

		if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock())) {
			return UpdateResult.REMOVE;
		}

		// scale down to 0.33 * speed near the end
		double factor = 1 - ((System.currentTimeMillis() - startTime) / (double) userConfig.duration);

		user.getEntity().setVelocity(user.getDirection().scalarMultiply(userConfig.speed * factor).toVector());
		user.getEntity().setFallDistance(0);

		Vector3 center = user.getLocation().add(Vector3.MINUS_J);
		for (Block block : WorldMethods.getNearbyBlocks(center.toLocation(user.getWorld()), userConfig.radius, MaterialUtil::isTransparent)) {
			if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) continue;
			Optional<TempBlock> tb = TempBlock.create(block, Material.WATER, 1000);
			if (ice) {
				tb.ifPresent(t -> t.setRevertTask(() -> TempBlock.create(block, Material.ICE, 1000)));
			}
		}
		if (ice) {
			CollisionUtil.handleEntityCollisions(user, new Sphere(center, userConfig.radius), this::onEntityHit);
		}
		return UpdateResult.CONTINUE;
	}

	private boolean onEntityHit(Entity entity) {
		if (affectedEntities.contains(entity)) return false;
		affectedEntities.add(entity);
		DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
		int potionDuration = NumberConversions.round(userConfig.slowDuration / 50F);
		PotionUtil.addPotion(entity, PotionEffectType.SLOW, potionDuration, userConfig.power);
		return true;
	}

	public void freeze() {
		ice = true;
	}

	public static void freeze(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("PhaseChange")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, WaterWave.class).ifPresent(WaterWave::freeze);
		}
	}

	@Override
	public void onDestroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.SPEED)
		public double speed;
		@Attribute(Attribute.RADIUS)
		public double radius;

		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public int power;
		@Attribute(Attribute.DURATION)
		public long slowDuration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "waterring", "waterwave");

			cooldown = abilityNode.node("cooldown").getLong(6000);
			duration = abilityNode.node("duration").getLong(3500);
			speed = abilityNode.node("speed").getDouble(1.2);
			radius = abilityNode.node("radius").getDouble(1.7);

			damage = abilityNode.node("ice-damage").getDouble(2.0);
			power = abilityNode.node("ice-slow-power").getInt(2) - 1;
			slowDuration = abilityNode.node("ice-slow-duration").getLong(3500);
		}
	}
}
