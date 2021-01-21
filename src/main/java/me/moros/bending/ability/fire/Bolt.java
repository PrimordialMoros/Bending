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
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.Optional;

public class Bolt extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Vector3 targetLocation;

	private boolean struck = false;
	private long startTime;

	public Bolt(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, getDescription())) return false;
		this.user = user;
		recalculateConfig();
		removalPolicy = Policies.builder()
			.add(new ExpireRemovalPolicy(userConfig.duration))
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.build();

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
		if (System.currentTimeMillis() >= startTime + userConfig.chargeTime) {
			if (user.isSneaking()) {
				ParticleUtil.createRGB(UserMethods.getMainHandSide(user).toLocation(user.getWorld()), "01E1FF").spawn();
				return UpdateResult.CONTINUE;
			} else {
				strike();
			}
		} else if (user.isSneaking()) {
			return UpdateResult.CONTINUE;
		}
		return UpdateResult.REMOVE;
	}

	private boolean onEntityHit(Entity entity) {
		if (entity instanceof Creeper) ((Creeper) entity).setPowered(true);
		double distance = VectorMethods.getEntityCenter(entity).distance(targetLocation);
		if (distance > 5) return false;
		boolean hitWater = MaterialUtil.isWater(targetLocation.toBlock(user.getWorld()));

		boolean vulnerable = (entity instanceof LivingEntity && InventoryUtil.hasMetalArmor((LivingEntity) entity));

		double damage = (vulnerable || hitWater) ? userConfig.damage * 2 : userConfig.damage;
		if (distance >= 1.5) {
			damage -= (hitWater ? distance / 3 : distance / 2);
		}
		DamageUtil.damageEntity(entity, user, damage, getDescription());
		return true;
	}

	public boolean isNearbyChannel() {
		Optional<Bolt> instance = Bending.getGame().getAbilityManager(user.getWorld()).getInstances(Bolt.class)
			.filter(b -> !b.getUser().equals(user))
			.filter(b -> b.getUser().getLocation().distanceSq(targetLocation) < 4 * 4)
			.findAny();
		instance.ifPresent(bolt -> bolt.startTime = 0);
		return instance.isPresent();
	}

	public void dealDamage() {
		Collider collider = new Sphere(targetLocation, 5);
		CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true, true);
		FragileStructure.attemptDamageStructure(Collections.singletonList(targetLocation.toBlock(user.getWorld())), 8);
	}

	private void strike() {
		targetLocation = WorldMethods.getTargetEntity(user, userConfig.range)
			.map(VectorMethods::getEntityCenter).orElseGet(() -> WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range)));
		if (!Bending.getGame().getProtectionSystem().canBuild(user, targetLocation.toBlock(user.getWorld()))) return;
		user.getWorld().strikeLightningEffect(targetLocation.toLocation(user.getWorld()));
		user.setCooldown(getDescription(), userConfig.cooldown);
		struck = true;
		if (!isNearbyChannel()) dealDamage();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public void onDestroy() {
		if (!struck && userConfig.duration > 0 && System.currentTimeMillis() > startTime + userConfig.duration) {
			DamageUtil.damageEntity(user.getEntity(), user, userConfig.damage, getDescription());
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.CHARGE_TIME)
		public long chargeTime;
		@Attribute(Attribute.DURATION)
		public long duration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "bolt");
			cooldown = abilityNode.node("cooldown").getLong(1500);
			damage = abilityNode.node("damage").getDouble(3.0);
			range = abilityNode.node("range").getDouble(20.0);
			chargeTime = abilityNode.node("charge-time").getLong(2000);
			duration = abilityNode.node("duration").getLong(8000);
		}
	}
}

