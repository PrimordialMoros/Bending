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

import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.Burstable;
import me.moros.bending.model.ability.FireTick;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class FireBlast implements Ability, Burstable {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Set<Entity> affectedEntities = new HashSet<>(); // Needed to ensure entities are hit by 1 burst stream only
	private FireStream stream;

	private boolean charging;
	private double factor = 1.0;
	private int particleCount;
	private long renderInterval;
	private long startTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		startTime = System.currentTimeMillis();
		charging = true;
		particleCount = 6;

		if (user.getHeadBlock().isLiquid() || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}

		removalPolicy = Policies.builder().build();

		for (FireBlast blast : Game.getAbilityManager(user.getWorld()).getUserInstances(user, FireBlast.class).collect(Collectors.toList())) {
			if (blast.charging) {
				blast.launch();
				return false;
			}
		}
		if (method == ActivationMethod.PUNCH) {
			launch();
		}
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
		if (charging) {
			if (!getDescription().equals(user.getSelectedAbility().orElse(null))) {
				return UpdateResult.REMOVE;
			}
			if (user.isSneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
				ParticleUtil.createFire(user, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
			} else if (!user.isSneaking()) {
				launch();
			}
		}
		return (charging || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	@Override
	public void onDestroy() {
	}

	private boolean launch() {
		double timeFactor = (System.currentTimeMillis() - startTime) / (double) userConfig.maxChargeTime;
		factor = FastMath.max(1, FastMath.min(userConfig.chargeFactor, timeFactor * userConfig.chargeFactor));
		charging = false;
		user.setCooldown(this, userConfig.cooldown);
		Vector3 origin = UserMethods.getMainHandSide(user);
		Vector3 lookingDir = user.getDirection().scalarMultiply(userConfig.range * factor);
		stream = new FireStream(user, new Ray(origin, lookingDir), userConfig.collisionRadius * factor);
		return true;
	}

	@Override
	public Collection<Collider> getColliders() {
		if (stream == null) return Collections.emptyList();
		return Collections.singletonList(stream.getCollider());
	}

	@Override
	public void onCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "FireBlast";
	}

	// Used to initialize the blast for bursts.
	@Override
	public void initialize(User user, Vector3 location, Vector3 direction) {
		this.user = user;
		recalculateConfig();
		factor = 1.0;
		charging = false;
		removalPolicy = Policies.builder().build();
		stream = new FireStream(user, new Ray(location, direction), userConfig.collisionRadius);
	}

	@Override
	public void setRenderInterval(long interval) {
		this.renderInterval = interval;
	}

	@Override
	public void setRenderParticleCount(int count) {
		this.particleCount = count;
	}

	private class FireStream extends ParticleStream {
		private final double displayRadius;
		private long nextRenderTime;

		public FireStream(User user, Ray ray, double collisionRadius) {
			super(user, ray, userConfig.speed * factor, collisionRadius);
			livingOnly = true;
			displayRadius = FastMath.max(collisionRadius - 1, 1);
			canCollide = Block::isLiquid;
		}

		@Override
		public void render() {
			long time = System.currentTimeMillis();
			if (time > nextRenderTime) {
				Location loc = getBukkitLocation();
				if (factor < 1.2) {
					ParticleUtil.createFire(user, loc)
						.count(particleCount).offset(0.25, 0.25, 0.25).spawn();
					ParticleUtil.create(Particle.SMOKE_NORMAL, loc)
						.count(particleCount / 2).offset(0.25, 0.25, 0.25).spawn();
				} else {
					for (Block block : WorldMethods.getNearbyBlocks(loc, displayRadius)) {
						ParticleUtil.createFire(user, block.getLocation())
							.count(particleCount).offset(0.5, 0.5, 0.5).spawn();
						ParticleUtil.create(Particle.SMOKE_NORMAL, block.getLocation())
							.count(particleCount / 2).offset(0.5, 0.5, 0.5).spawn();
					}
				}
				nextRenderTime = time + renderInterval;
			}
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.FIRE_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			if (entity instanceof LivingEntity && !affectedEntities.contains(entity)) {
				DamageUtil.damageEntity(entity, user, userConfig.damage * factor, getDescription());
				FireTick.LARGER.apply(entity, 30);
				affectedEntities.add(entity);
			}
			return true;
		}

		@Override
		public boolean onBlockHit(Block block) {
			Vector reverse = ray.direction.scalarMultiply(-1).toVector();
			double rayRange = userConfig.igniteRadius * factor + 2;
			if (user.getLocation().distanceSq(new Vector3(block)) > 4) {
				List<Block> blocks = new ArrayList<>();
				for (Block b : WorldMethods.getNearbyBlocks(getBukkitLocation(), userConfig.igniteRadius * factor)) {
					if (!Game.getProtectionSystem().canBuild(user, b)) continue;
					if (WorldMethods.rayTraceBlocks(b.getLocation(), reverse, rayRange).isPresent()) continue;
					BlockMethods.lightBlock(b);
					if (MaterialUtil.isIgnitable(b)) blocks.add(b);
				}
				blocks.forEach(b -> TempBlock.create(b, Material.FIRE, 10000, true));
			}
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.COLLISION_RADIUS)
		public double collisionRadius;
		@Attribute(Attributes.RADIUS)
		public double igniteRadius;
		@Attribute(Attributes.STRENGTH)
		public double chargeFactor;
		@Attribute(Attributes.CHARGE_TIME)
		public long maxChargeTime;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "fireblast");

			cooldown = abilityNode.getNode("cooldown").getLong(1500);
			damage = abilityNode.getNode("damage").getDouble(3.0);
			range = abilityNode.getNode("range").getDouble(20.0);
			speed = abilityNode.getNode("speed").getDouble(0.8);
			igniteRadius = abilityNode.getNode("ignite-radius").getDouble(1.5);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(1.4);

			chargeFactor = abilityNode.getNode("charge").getNode("factor").getDouble(1.5);
			maxChargeTime = abilityNode.getNode("charge").getNode("max-time").getLong(2000);

			abilityNode.getNode("charge").getNode("factor").setComment("How much the damage, radius, range and speed are multiplied by at full charge");
			abilityNode.getNode("charge").getNode("max-time").setComment("How many milliseconds it takes to fully charge");
		}
	}
}
