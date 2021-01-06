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

package me.moros.bending.ability.earth;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO add metalcips
public class EarthGlove extends AbilityInstance implements Ability {
	public enum Side {RIGHT, LEFT}

	private static final Config config = new Config();

	private static final Map<UUID, Side> lastUsedSide = new ConcurrentHashMap<>();
	private static final double GLOVE_SPEED = 1.2;
	private static final double GLOVE_GRABBED_SPEED = 0.6;

	private static final ItemStack stone = new ItemStack(Material.STONE, 1);
	private static final ItemStack ingot = new ItemStack(Material.IRON_INGOT, 1);

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Item glove;
	private Vector3 location;
	private Vector3 lastVelocity;
	private LivingEntity grabbedTarget;

	private boolean isMetal = false;
	private boolean returning = false;
	private boolean grabbed = false;

	public EarthGlove(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, EarthGlove.class).count() > 2)
			return false;

		this.user = user;
		recalculateConfig();

		if (launchEarthGlove()) {
			removalPolicy = Policies.builder()
				.add(Policies.IN_LIQUID)
				.add(new SwappedSlotsRemovalPolicy(getDescription()))
				.add(new OutOfRangeRemovalPolicy(userConfig.range + 5, () -> location))
				.build();
			user.setCooldown(getDescription(), userConfig.cooldown);
			return true;
		}

		return false;
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

		if (glove == null || !glove.isValid()) {
			return UpdateResult.REMOVE;
		}

		location = new Vector3(glove.getLocation());
		if (location.distanceSq(user.getEyeLocation()) > userConfig.range * userConfig.range) {
			returning = true;
		}

		if (!Bending.getGame().getProtectionSystem().canBuild(user, glove.getLocation().getBlock())) {
			shatterGlove();
			return UpdateResult.REMOVE;
		}

		if (returning) {
			if (!user.isSneaking()) {
				shatterGlove();
				return UpdateResult.REMOVE;
			}
			Vector3 returnLocation = user.getEyeLocation().add(user.getDirection().scalarMultiply(1.5));
			if (location.distanceSq(returnLocation) < 1) {
				if (grabbed && grabbedTarget != null) grabbedTarget.setVelocity(new Vector());
				return UpdateResult.REMOVE;
			}
			if (grabbed) {
				if (grabbedTarget == null || !grabbedTarget.isValid() || (grabbedTarget instanceof Player && !((Player) grabbedTarget).isOnline())) {
					shatterGlove();
					return UpdateResult.REMOVE;
				}
				Vector3 dir = returnLocation.subtract(new Vector3(grabbedTarget.getLocation())).normalize().scalarMultiply(GLOVE_GRABBED_SPEED);
				grabbedTarget.setVelocity(dir.clampVelocity());
				glove.teleport(grabbedTarget.getEyeLocation().subtract(0, grabbedTarget.getHeight() / 2, 0));
				return UpdateResult.CONTINUE;
			} else {
				Vector3 dir = returnLocation.subtract(location).normalize().scalarMultiply(GLOVE_SPEED);
				setGloveVelocity(dir);
			}
		} else {
			double velocityLimit = (grabbed ? GLOVE_GRABBED_SPEED : GLOVE_SPEED) - 0.2;
			Vector3 gloveVelocity = new Vector3(glove.getVelocity());
			if (glove.isOnGround() || Vector3.angle(lastVelocity, gloveVelocity) > FastMath.PI / 6 || gloveVelocity.getNormSq() < velocityLimit * velocityLimit) {
				shatterGlove();
				return UpdateResult.REMOVE;
			}

			setGloveVelocity(lastVelocity.normalize().scalarMultiply(GLOVE_SPEED));
			boolean sneaking = user.isSneaking();
			boolean collided = CollisionUtil.handleEntityCollisions(user, new Sphere(location, 0.8), this::onEntityHit, true, false, sneaking);
			if (collided && !grabbed) {
				return UpdateResult.REMOVE;
			}

			if (grabbed) {
				Vector3 returnLocation = user.getEyeLocation().add(user.getDirection().scalarMultiply(1.5));
				Vector3 dir = returnLocation.subtract(location).normalize().scalarMultiply(EarthGlove.GLOVE_GRABBED_SPEED);
				grabbedTarget.setVelocity(dir.clampVelocity());
				setGloveVelocity(dir);
			}
		}
		return UpdateResult.CONTINUE;
	}

	private boolean onEntityHit(Entity entity) {
		if (user.isSneaking()) {
			return grabTarget((LivingEntity) entity);
		}
		DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
		shatterGlove();
		return false;
	}

	private boolean grabTarget(LivingEntity entity) {
		if (grabbed || grabbedTarget != null) {
			return false;
		}
		returning = true;
		grabbed = true;
		grabbedTarget = entity;
		glove.teleport(grabbedTarget.getEyeLocation().subtract(0, grabbedTarget.getHeight() / 2, 0));
		return true;
	}

	private boolean launchEarthGlove() {
		Vector3 gloveSpawnLocation;
		Vector3 offset = UserMethods.PLAYER_OFFSET.add(user.getDirection().scalarMultiply(0.4));
		Side side;
		if (lastUsedSide.getOrDefault(user.getEntity().getUniqueId(), Side.RIGHT) == Side.RIGHT) {
			gloveSpawnLocation = UserMethods.getRightSide(user).add(offset);
			side = Side.LEFT;
		} else {
			gloveSpawnLocation = UserMethods.getLeftSide(user).add(offset);
			side = Side.RIGHT;
		}

		// TODO check for cooldown
		/*if (user.isOnCooldown(side)) {
			return false;
		}*/

		lastUsedSide.put(user.getEntity().getUniqueId(), side);

		Vector3 target = WorldMethods.getTargetEntity(user, userConfig.range)
			.map(VectorMethods::getEntityCenter)
			.orElseGet(() -> WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range)));

		glove = buildGlove(gloveSpawnLocation);
		Vector3 velocity = target.subtract(gloveSpawnLocation).normalize().scalarMultiply(GLOVE_SPEED);
		setGloveVelocity(velocity);
		location = new Vector3(glove.getLocation());
		return true;
	}

	// Currently we only check if they have iron ingots but aren't removing them from the inventory.
	// TODO come up with better sourcing system for earth glove/metal clips
	private Item buildGlove(Vector3 spawnLocation) {
		isMetal = user.hasPermission("bending.metal") && InventoryUtil.hasItem(user, ingot);
		Item item = user.getWorld().dropItem(spawnLocation.toLocation(user.getWorld()), isMetal ? ingot : stone);
		item.setGravity(false);
		item.setInvulnerable(true);
		item.setMetadata(Metadata.NO_PICKUP, Metadata.emptyMetadata());
		item.setMetadata(Metadata.GLOVE_KEY, Metadata.customMetadata(this));
		return item;
	}

	private void setGloveVelocity(Vector3 velocity) {
		glove.setVelocity(velocity.clampVelocity());
		lastVelocity = new Vector3(glove.getVelocity());
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public void onDestroy() {
		if (glove != null) glove.remove();
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		if (glove == null || returning) return Collections.emptyList();
		return Collections.singletonList(new Sphere(location, 0.8));
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	public void shatterGlove() {
		if (!glove.isValid()) {
			return;
		}
		BlockData data = isMetal ? Material.IRON_BLOCK.createBlockData() : Material.STONE.createBlockData();
		ParticleUtil.create(Particle.BLOCK_CRACK, glove.getLocation())
			.count(3).offset(0.1, 0.1, 0.1).data(data).spawn();
		ParticleUtil.create(Particle.BLOCK_DUST, glove.getLocation())
			.count(2).offset(0.1, 0.1, 0.1).data(data).spawn();
		onDestroy();
	}

	public static void attemptDestroy(@NonNull User user) {
		CollisionUtil.handleEntityCollisions(user, new Sphere(user.getEyeLocation(), 8), e -> {
			if (e instanceof Item && user.getEntity().hasLineOfSight(e) && e.hasMetadata(Metadata.GLOVE_KEY)) {
				EarthGlove ability = (EarthGlove) e.getMetadata(Metadata.GLOVE_KEY).get(0).value();
				if (ability != null && !user.equals(ability.getUser())) {
					ability.shatterGlove();
				}
			}
			return true;
		}, false, false);
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.DAMAGE)
		public double damage;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthblast");

			cooldown = abilityNode.node("cooldown").getLong(750);
			range = abilityNode.node("range").getDouble(32.0);
			damage = abilityNode.node("damage").getDouble(2.0);
		}
	}
}
