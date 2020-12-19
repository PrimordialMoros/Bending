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
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.TravellingSource;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WaterRing extends AbilityInstance implements Ability {
	public static final double RING_RADIUS = 2.8;

	private static final Config config = new Config();

	private static AbilityDescription waveDesc;

	private User user;
	private Config userConfig;

	private RemovalPolicy removalPolicy;
	private Block lastBlock;
	private StateChain states;
	private final List<Block> ring = new ArrayList<>(24);
	private final Collection<IceShard> shards = new ArrayList<>(16);
	private final Map<Entity, Boolean> affectedEntities = ExpiringMap.builder()
		.expirationPolicy(ExpirationPolicy.CREATED)
		.expiration(250, TimeUnit.MILLISECONDS).build();

	private boolean ready = false;
	private boolean completed = false;
	private double radius = RING_RADIUS;
	private int index = 0;
	private int sources = 0;
	private long nextShardTime = 0;
	private long ringNextShrinkTime = 0;
	private long sneakStartTime = 0;

	public WaterRing(@NonNull AbilityDescription desc) {
		super(desc);
		if (waveDesc == null) {
			waveDesc = Bending.getGame().getAbilityRegistry()
				.getAbilityDescription("WaterWave").orElseThrow(RuntimeException::new);
		}
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (method == ActivationMethod.PUNCH && user.isSneaking()) {
			if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("WaterRing")) {
				if (Bending.getGame().getAbilityManager(user.getWorld()).destroyInstanceType(user, WaterRing.class)) {
					return false;
				}
			}
		}
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, WaterRing.class)) {
			return false;
		}
		this.user = user;
		recalculateConfig();
		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, WaterMaterials.ALL);
		if (!source.isPresent()) return false;
		List<Block> list = new ArrayList<>();
		list.add(source.get());
		states = new StateChain(list)
			.addState(new TravellingSource(user, Material.WATER.createBlockData(), RING_RADIUS - 0.5, userConfig.selectRange + 5))
			.start();

		removalPolicy = Policies.builder().add(new ExpireRemovalPolicy(userConfig.duration)).build();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	public List<Block> complete() {
		if (!ready) return Collections.emptyList();
		completed = true;
		sources = 0;
		return getOrdered(getDirectionIndex());
	}

	private Block getClosestRingBlock() {
		Vector3 dir = user.getDirection().setY(0).normalize().scalarMultiply(radius);
		Block target = new Vector3(user.getHeadBlock()).add(Vector3.HALF).add(dir).toBlock(user.getWorld());
		Block result = ring.get(0);
		Vector3 targetVector = new Vector3(target);
		double minDistance = Double.MAX_VALUE;
		for (Block block : ring) {
			if (target.equals(block)) return target;
			double d = new Vector3(block.getLocation()).distanceSq(targetVector);
			if (d < minDistance) {
				minDistance = d;
				result = block;
			}
		}
		return result;
	}

	public int getDirectionIndex() {
		Vector3 dir = user.getDirection().setY(0).normalize().scalarMultiply(radius);
		Block target = new Vector3(user.getHeadBlock()).add(Vector3.HALF).add(dir).toBlock(user.getWorld());
		return FastMath.max(0, ring.indexOf(target));
	}

	public List<Block> getOrdered(int index) {
		if (index == 0) return ring;
		return Stream.concat(ring.subList(index, ring.size()).stream(), ring.subList(0, index).stream())
			.collect(Collectors.toList());
	}

	@Override
	public @NonNull UpdateResult update() {
		if (completed || removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		if (!ready) {
			if (states.update() == UpdateResult.REMOVE) {
				if (states.isComplete() && !states.getChainStore().isEmpty()) {
					ring.addAll(createRing());
					sources = ring.size();
					ready = true;
				} else {
					return UpdateResult.REMOVE;
				}
			}
			return UpdateResult.CONTINUE;
		}
		cleanAll();
		if (sources <= 0 || !Bending.getGame().getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return UpdateResult.REMOVE;
		}
		Block current = user.getLocBlock();
		if (!current.equals(lastBlock)) {
			ring.clear();
			ring.addAll(createRing());
			Collections.rotate(ring, index);
			lastBlock = current;
		}

		if (user.isSneaking()) {
			long time = System.currentTimeMillis();
			if (sneakStartTime == 0) {
				sneakStartTime = time;
				ringNextShrinkTime = time + 250;
			} else {
				if (ringNextShrinkTime > time && radius > 1.3) {
					setRadius(radius - 0.3);
					ringNextShrinkTime = time + 250;
				}
				if (time > sneakStartTime + userConfig.chargeTime) {
					if (!complete().isEmpty()) {
						WaterWave wave = new WaterWave(waveDesc);
						if (wave.activate(user, ActivationMethod.SNEAK)) {
							Bending.getGame().getAbilityManager(user.getWorld()).addAbility(user, wave);
						}
					}
					return UpdateResult.REMOVE;
				}
			}
		} else {
			sneakStartTime = 0;
			if (radius < RING_RADIUS) {
				setRadius(FastMath.min(radius + 0.3, RING_RADIUS));
			}
		}

		if (ring.stream().noneMatch(b -> Bending.getGame().getProtectionSystem().canBuild(user, b)))
			return UpdateResult.REMOVE;
		Collections.rotate(ring, 1);
		index = ++index % ring.size();

		for (int i = 0; i < FastMath.min(ring.size(), NumberConversions.ceil(sources * 0.8)); i++) {
			Block block = ring.get(i);
			if (MaterialUtil.isWater(block)) {
				ParticleUtil.create(Particle.WATER_BUBBLE, block.getLocation().add(0.5, 0.5, 0.5))
					.count(5).offset(0.25, 0.25, 0.25).spawn();
			} else if (MaterialUtil.isTransparent(block)) {
				TempBlock.create(block, Material.WATER, 250);
			}
		}

		if (userConfig.affectEntities) {
			CollisionUtil.handleEntityCollisions(user, new Sphere(user.getEyeLocation(), radius + 2), this::checkCollisions, false);
		}

		shards.removeIf(shard -> shard.update() == UpdateResult.REMOVE);
		return UpdateResult.CONTINUE;
	}

	private boolean checkCollisions(Entity entity) {
		for (Block block : ring) {
			if (affectedEntities.containsKey(entity)) return false;
			if (!MaterialUtil.isWater(block)) continue;
			if (!AABB.BLOCK_BOUNDS.at(new Vector3(block.getLocation())).intersects(AABBUtils.getEntityBounds(entity)))
				continue;
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			Vector3 velocity = new Vector3(entity.getLocation()).subtract(user.getEyeLocation()).setY(0).normalize();
			entity.setVelocity(velocity.scalarMultiply(userConfig.knockback).clampVelocity());
			affectedEntities.put(entity, false);
		}
		return false;
	}

	public boolean isReady() {
		return ready;
	}

	public void setRadius(double radius) {
		if (radius < 1 || radius > 8 || this.radius == radius) return;
		this.radius = radius;
		cleanAll();
		ring.clear();
		ring.addAll(createRing());
	}

	public double getRadius() {
		return radius;
	}

	private Collection<Block> createRing() {
		Vector3 center = new Vector3(user.getHeadBlock()).add(Vector3.HALF);
		Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.PI / (5 * radius), RotationConvention.VECTOR_OPERATOR);
		return VectorMethods.rotateInverse(Vector3.PLUS_I.scalarMultiply(radius), rotation, NumberConversions.ceil(10 * radius))
			.stream().map(v -> center.add(v).toBlock(user.getWorld())).distinct().collect(Collectors.toList());
	}

	private void cleanAll() {
		for (Block block : ring) {
			TempBlock.manager.get(block).filter(tb -> MaterialUtil.isWater(tb.getBlock())).ifPresent(TempBlock::revert);
		}
	}

	@Override
	public void onDestroy() {
		if (!completed) cleanAll();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	public void launchShard() {
		if (!user.canBend(getDescription()) || ring.isEmpty()) return;
		long time = System.currentTimeMillis();
		if (time > nextShardTime) {
			nextShardTime = time + 100;
			Vector3 origin = new Vector3(getClosestRingBlock());
			Vector3 lookingDir = user.getDirection().scalarMultiply(userConfig.shardRange + radius);
			shards.add(new IceShard(user, new Ray(origin, lookingDir)));
		}
	}

	public static void launchShard(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("WaterRing")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, WaterRing.class).ifPresent(WaterRing::launchShard);
		}
	}

	private class IceShard extends ParticleStream {
		public IceShard(User user, Ray ray) {
			super(user, ray, 0.3, 0.5);
			livingOnly = true;
			canCollide = Block::isLiquid;
		}

		@Override
		public @NonNull UpdateResult update() {
			for (int i = 0; i < 5; i++) {
				location = location.add(dir);
				Block block = location.toBlock(user.getWorld());
				if (location.distanceSq(ray.origin) > maxRange || !Bending.getGame().getProtectionSystem().canBuild(user, block)) {
					return UpdateResult.REMOVE;
				}
				render();
				postRender();

				if (i % 2 == 0) {
					// Use previous collider for entity checks for visual reasons
					boolean hitEntity = CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, livingOnly, hitSelf);
					if (hitEntity) return UpdateResult.REMOVE;
					collider = collider.at(location);
					if (!MaterialUtil.isTransparent(block)) {
						AABB blockBounds = AABBUtils.getBlockBounds(block);
						if (canCollide.test(block) || blockBounds.intersects(collider)) {
							if (onBlockHit(block)) return UpdateResult.REMOVE;
						}
					}
				}
			}
			return UpdateResult.CONTINUE;
		}

		@Override
		public void render() {
			ParticleUtil.create(Particle.SNOW_SHOVEL, getBukkitLocation())
				.offset(0.25, 0.25, 0.25).spawn();
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.ICE_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			DamageUtil.damageEntity(entity, user, userConfig.shardDamage, "IceShard");
			return true;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			if (MaterialUtil.isLava(block)) {
				BlockMethods.extinguish(user, block);
				return true;
			}
			return false;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		public boolean affectEntities;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public double knockback;
		// Shards
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double shardRange;
		@Attribute(Attribute.DAMAGE)
		public double shardDamage;
		@Attribute(Attribute.AMOUNT)
		public double shardAmount;
		// Wave
		@Attribute(Attribute.CHARGE_TIME)
		public long chargeTime;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "waterring");

			duration = abilityNode.getNode("duration").getLong(30000);
			selectRange = abilityNode.getNode("select-range").getDouble(16.0);
			affectEntities = abilityNode.getNode("affect-entities").getBoolean(true);
			damage = abilityNode.getNode("damage").getDouble(1.0);
			knockback = abilityNode.getNode("knockback").getDouble(1.0);

			CommentedConfigurationNode shardsNode = abilityNode.getNode("shards");
			cooldown = shardsNode.getNode("cooldown").getLong(500);
			shardRange = shardsNode.getNode("range").getDouble(16.0);
			shardDamage = shardsNode.getNode("damage").getDouble(0.5);
			shardAmount = shardsNode.getNode("amount").getInt(10);

			CommentedConfigurationNode waveNode = abilityNode.getNode("waterwave");
			chargeTime = waveNode.getNode("charge-time").getLong(1250);
		}
	}
}
