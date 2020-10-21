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

import me.moros.bending.Bending;
import me.moros.bending.ability.common.TravellingSource;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.VectorMethods;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WaterRing implements Ability {
	private static final Config config = new Config();

	private static final double RING_RADIUS = 2.8;

	private User user;
	private Config userConfig;

	private RemovalPolicy removalPolicy;
	private Block lastBlock;
	private StateChain states;
	private final List<Block> ring = new ArrayList<>(24);
	private final Map<Entity, Boolean> affectedEntities = ExpiringMap.builder()
		.expirationPolicy(ExpirationPolicy.CREATED)
		.expiration(250, TimeUnit.MILLISECONDS).build();

	private boolean ready = false;
	private boolean completed = false;
	private double radius = RING_RADIUS;
	private int index = 0;
	private int sources = 0;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
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
		if (ring.stream().noneMatch(b -> Bending.getGame().getProtectionSystem().canBuild(user, b)))
			return UpdateResult.REMOVE;
		Collections.rotate(ring, 1);
		index = ++index % ring.size();

		for (int i = 0; i < NumberConversions.ceil(sources * 0.8); i++) {
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
			entity.setVelocity(velocity.scalarMultiply(userConfig.knockback).clampVelocity().toVector());
			affectedEntities.put(entity, false);
		}
		return false;
	}

	public boolean isReady() {
		return ready;
	}

	public void setRadius(double radius) {
		if (radius < 2 || radius > 8) return;
		this.radius = radius;
		lastBlock = null;
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

	@Override
	public @NonNull String getName() {
		return "WaterRing";
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.DURATION)
		public long duration;
		@Attribute(Attributes.SELECTION)
		public double selectRange;
		public boolean affectEntities;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.STRENGTH)
		public double knockback;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "waterring");

			duration = abilityNode.getNode("duration").getLong(30000);
			selectRange = abilityNode.getNode("select-range").getDouble(16.0);
			affectEntities = abilityNode.getNode("affect-entities").getBoolean(true);
			damage = abilityNode.getNode("damage").getDouble(1.0);
			knockback = abilityNode.getNode("knockback").getDouble(1.0);
		}
	}
}
