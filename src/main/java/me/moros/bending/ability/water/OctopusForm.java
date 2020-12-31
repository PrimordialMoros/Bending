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
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// TODO make tentacle extension animation
public class OctopusForm extends AbilityInstance implements Ability {
	private static final Config config = new Config();
	private static final double RADIUS = 3.0;
	private static final AABB TENTACLE_BOX = new AABB(new Vector3(-1, 0.0, -1), new Vector3(1, 2.5, 1));

	private static AbilityDescription ringDesc;

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<Block> base = new ArrayList<>();
	private final List<Tentacle> tentacles = new ArrayList<>();

	private final Map<Entity, Boolean> affectedEntities = ExpiringMap.builder()
		.expirationPolicy(ExpirationPolicy.CREATED)
		.expiration(250, TimeUnit.MILLISECONDS).build();

	private WaterRing ring;
	private Block lastBlock;

	private boolean formed = false;
	private long nextTentacleFormTime = 0;

	public OctopusForm(@NonNull AbilityDescription desc) {
		super(desc);
		if (ringDesc == null) {
			ringDesc = Bending.getGame().getAbilityRegistry()
				.getAbilityDescription("WaterRing").orElseThrow(RuntimeException::new);
		}
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		OctopusForm octopusForm = Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, OctopusForm.class).orElse(null);
		if (octopusForm != null) {
			octopusForm.punch();
			return false;
		}

		this.user = user;
		recalculateConfig();

		removalPolicy = Policies.builder().build();

		ring = Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, WaterRing.class).orElse(null);
		if (ring == null) {
			ring = new WaterRing(ringDesc);
			if (ring.activate(user, method)) {
				Bending.getGame().getAbilityManager(user.getWorld()).addAbility(user, ring);
				return true;
			} else {
				return false;
			}
		}
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
		if (formed) {
			cleanAll();
			if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock())) {
				return UpdateResult.REMOVE;
			}
			boolean forceUpdate = false;
			Block current = user.getLocBlock();
			if (!current.equals(lastBlock)) {
				base.clear();
				base.addAll(BlockMethods.createBlockRing(user.getLocBlock(), RADIUS));
				lastBlock = current;
				forceUpdate = true;
			}
			if (base.stream().noneMatch(b -> Bending.getGame().getProtectionSystem().canBuild(user, b))) {
				return UpdateResult.REMOVE;
			}
			renderBase();
			int size = tentacles.size();
			if (size < 8 && System.currentTimeMillis() > nextTentacleFormTime) tentacles.add(new Tentacle(size));
			renderTentacles(forceUpdate);
		} else {
			if (!Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, WaterRing.class)) {
				return UpdateResult.REMOVE;
			}
			if (ring.isReady() && user.isSneaking()) form();
		}
		return UpdateResult.CONTINUE;
	}

	private void form() {
		if (!user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("OctopusForm")) return;
		ring.complete().forEach(this::clean);
		formed = true;
		nextTentacleFormTime = System.currentTimeMillis() + 150;
		removalPolicy = Policies.builder()
			.add(Policies.NOT_SNEAKING)
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.build();
	}

	private void renderBase() {
		for (Block block : base) {
			Block below = block.getRelative(BlockFace.DOWN);
			if (MaterialUtil.isWater(below) && TempBlock.isBendable(below)) {
				TempBlock.create(below, Material.ICE, userConfig.iceDuration, true);
			}
			renderWaterBlock(block);
		}
	}

	private void renderTentacles(boolean forceUpdate) {
		Vector3 center = user.getLocation().floor().add(Vector3.HALF);
		long time = System.currentTimeMillis();
		for (Tentacle tentacle : tentacles) {
			if (forceUpdate || time > tentacle.nextUpdateTime) {
				tentacle.updateBlocks(center);
			}
			tentacle.render();
		}
	}

	private void renderWaterBlock(Block block) {
		if (!TempBlock.isBendable(block)) return;
		if (MaterialUtil.isWater(block)) {
			ParticleUtil.create(Particle.WATER_BUBBLE, block.getLocation().add(0.5, 0.5, 0.5))
				.count(5).offset(0.25, 0.25, 0.25).spawn();
		} else if (MaterialUtil.isTransparent(block)) {
			TempBlock.create(block, Material.WATER, 250);
		}
	}

	private void punch() {
		if (!formed) return;
		double r = RADIUS + 0.5;
		for (double phi = 0; phi < FastMath.PI * 2; phi += FastMath.PI / 4) {
			Vector3 tentacleBase = user.getLocation().add(new Vector3(FastMath.cos(phi) * r, 0, FastMath.sin(phi) * r));
			CollisionUtil.handleEntityCollisions(user, TENTACLE_BOX.at(tentacleBase), this::onEntityHit, true);
		}
	}

	private boolean onEntityHit(Entity entity) {
		if (affectedEntities.containsKey(entity)) return false;
		DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
		Vector3 dir = VectorMethods.getEntityCenter(entity).subtract(user.getLocation());
		entity.setVelocity(dir.normalize().scalarMultiply(userConfig.knockback).clampVelocity());
		affectedEntities.put(entity, false);
		return true;
	}

	private void clean(Block block) {
		TempBlock.manager.get(block).filter(tb -> MaterialUtil.isWater(tb.getBlock())).ifPresent(TempBlock::revert);
	}

	private void cleanAll() {
		tentacles.forEach(Tentacle::revert);
		base.forEach(this::clean);
	}

	@Override
	public void onDestroy() {
		if (formed) {
			user.setCooldown(getDescription(), userConfig.cooldown);
			cleanAll();
		}
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class Tentacle {
		private final Block[] blocks;
		private final double cos, sin;
		private final long topFormTime;

		private long nextUpdateTime;

		private Tentacle(int index) {
			double theta = index * FastMath.PI / 4;
			cos = FastMath.cos(theta);
			sin = FastMath.sin(theta);
			blocks = new Block[2];
			topFormTime = System.currentTimeMillis() + 150;
			updateBlocks(user.getLocation().floor().add(Vector3.HALF));
		}

		private void updateBlocks(Vector3 center) {
			long time = System.currentTimeMillis();
			nextUpdateTime = time + ThreadLocalRandom.current().nextLong(150, 450);
			double topOffset = ThreadLocalRandom.current().nextDouble(1);
			double bottomOffset = ThreadLocalRandom.current().nextDouble(1);
			double xBottom = cos * (RADIUS + bottomOffset);
			double zBottom = sin * (RADIUS + bottomOffset);
			blocks[0] = center.add(new Vector3(xBottom, 1, zBottom)).toBlock(user.getWorld());
			if (time > topFormTime) {
				double xTop = cos * (RADIUS + topOffset);
				double zTop = sin * (RADIUS + topOffset);
				blocks[1] = center.add(new Vector3(xTop, 2, zTop)).toBlock(user.getWorld());
			}
		}

		private void revert() {
			for (Block block : blocks) {
				if (block != null) clean(block);
			}
		}

		private void render() {
			for (Block block : blocks) {
				if (block != null) renderWaterBlock(block);
			}
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DURATION)
		public long iceDuration;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public double knockback;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "octopusform");

			cooldown = abilityNode.getNode("cooldown").getLong(0);
			iceDuration = abilityNode.getNode("ice-duration").getLong(30000);
			damage = abilityNode.getNode("damage").getDouble(2.0);
			knockback = abilityNode.getNode("knockback").getDouble(1.75);
		}
	}
}
