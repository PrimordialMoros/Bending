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
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Bolt implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;

	private Location targetLocation;

	private long startTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		if (Game.getAbilityManager(user.getWorld()).hasAbility(user, getDescription())) return false;
		this.user = user;
		recalculateConfig();

		if (Policies.IN_LIQUID.test(user, getDescription()) || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}
		removalPolicy = CompositeRemovalPolicy.defaults().add(new SwappedSlotsRemovalPolicy(getDescription())).build();

		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
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
		if (entity instanceof ArmorStand) return false;
		if (entity instanceof Creeper) ((Creeper) entity).setPowered(true);
		double distance = entity.getLocation().distance(targetLocation);
		if (distance > 5) return false;
		boolean hitWater = MaterialUtil.isWater(targetLocation.getBlock());
		boolean vulnerable = hitWater;
		if (entity instanceof Player) {
			// TODO add check for conductors
			vulnerable = true;
		}

		double damage = (vulnerable || hitWater) ? userConfig.damage * 2 : userConfig.damage;
		if (distance >= 1.5) {
			damage -= (hitWater ? distance / 3 : distance / 2);
		}
		DamageUtil.damageEntity(entity, user, damage, getDescription());
		return true;
	}

	public boolean isNearbyChannel() {
		Optional<Bolt> instance = Game.getAbilityManager(user.getWorld()).getInstances(Bolt.class)
			.filter(b -> !b.getUser().equals(user))
			.filter(b -> b.getUser().getLocation().distanceSq(new Vector3(targetLocation)) < 4 * 4)
			.findAny();
		instance.ifPresent(bolt -> bolt.startTime = 0);
		return instance.isPresent();
	}

	public void dealDamage() {
		Collider collider = new Sphere(new Vector3(targetLocation), 5);
		CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true, true);
	}

	@Override
	public void destroy() {
	}

	private void strike() {
		Ray ray = new Ray(user.getEyeLocation(), user.getDirection().scalarMultiply(userConfig.range));
		Optional<LivingEntity> target = WorldMethods.getTargetEntity(user, (int) userConfig.range);
		targetLocation = target.map(LivingEntity::getLocation).orElseGet(() -> WorldMethods.getTarget(user.getWorld(), ray));
		if (!Game.getProtectionSystem().canBuild(user, targetLocation.getBlock())) return;
		user.getWorld().strikeLightningEffect(targetLocation);
		user.setCooldown(this, userConfig.cooldown);
		if (!isNearbyChannel()) dealDamage();
	}

	@Override
	public List<Collider> getColliders() {
		return Collections.emptyList();
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "Bolt";
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.CHARGE_TIME)
		public long chargeTime;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "bolt");
			cooldown = abilityNode.getNode("cooldown").getLong(1500);
			damage = abilityNode.getNode("damage").getDouble(3.0);
			range = abilityNode.getNode("range").getDouble(20.0);
			chargeTime = abilityNode.getNode("charge-time").getLong(2000);
		}
	}
}

