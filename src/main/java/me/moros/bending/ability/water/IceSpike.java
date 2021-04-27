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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class IceSpike extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<IcePillar> pillars = new ArrayList<>();
	private final Collection<Entity> affectedEntities = new HashSet<>();

	public IceSpike(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		boolean field = method == ActivationMethod.SNEAK;
		if (field) {
			Collider collider = new Sphere(user.getLocation(), userConfig.radius);
			CollisionUtil.handleEntityCollisions(user, collider, this::createPillar, true);
		} else {
			Block source = null;
			Optional<LivingEntity> entity = user.getTargetEntity(userConfig.selectRange);
			if (entity.isPresent()) {
				Block base = entity.get().getLocation().getBlock().getRelative(BlockFace.DOWN);
				if (Bending.getGame().getProtectionSystem().canBuild(user, base) && WaterMaterials.isIceBendable(base) && TempBlock.isBendable(base)) {
					source = base;
				}
			}
			if (source == null) {
				Optional<Block> targetBlock = SourceUtil.getSource(user, userConfig.selectRange, WaterMaterials::isIceBendable);
				if (!targetBlock.isPresent()) return false;
				source = targetBlock.get();
			}
			buildPillar(source);
		}
		if (!pillars.isEmpty()) {
			user.setCooldown(getDescription(), field ? userConfig.fieldCooldown : userConfig.columnCooldown);
			removalPolicy = Policies.builder().build();
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

		pillars.removeIf(pillar -> pillar.update() == UpdateResult.REMOVE);
		return pillars.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	private boolean createPillar(Entity entity) {
		Block base = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
		boolean unique = pillars.stream()
			.noneMatch(p -> p.origin.getX() == base.getX() && p.origin.getZ() == base.getZ());
		if (WaterMaterials.isIceBendable(base)) {
			if (unique) {
				ParticleUtil.create(Particle.BLOCK_DUST, entity.getLocation())
					.count(8).offset(1, 0.1, 1).data(base.getBlockData()).spawn();
				buildPillar(base);
			}
			return true;
		}
		return false;
	}

	private void buildPillar(Block block) {
		int h = validate(block);
		if (h > 0) pillars.add(new IcePillar(block, h));
	}

	private int validate(Block block) {
		int height = userConfig.columnMaxHeight;
		if (!WaterMaterials.isIceBendable(block) || !TempBlock.isBendable(block)) return 0;
		if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) return 0;
		for (int i = 0; i < height; i++) {
			Block forwardBlock = block.getRelative(BlockFace.UP, i + 1);
			if (!Bending.getGame().getProtectionSystem().canBuild(user, forwardBlock)) {
				return i;
			}
			if (!MaterialUtil.isTransparent(forwardBlock) && forwardBlock.getType() != Material.WATER) {
				return i;
			}
		}
		return height;
	}

	private void clean(Block block) {
		if (WaterMaterials.isIceBendable(block)) TempBlock.createAir(block);
	}

	@Override
	public void onDestroy() {
		for (IcePillar pillar : pillars) {
			pillar.pillarBlocks.forEach(this::clean);
		}
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class IcePillar implements Updatable {
		private final Block origin;
		private final Material material;
		private final Deque<Block> pillarBlocks;

		private final int length;

		private boolean reverting = false;
		private int currentLength = 0;
		private long nextUpdateTime = 0;

		private IcePillar(@NonNull Block origin, int length) {
			this.origin = origin;
			this.material = origin.getType();
			this.length = length;
			this.pillarBlocks = new ArrayDeque<>(length);
		}

		@Override
		public @NonNull UpdateResult update() {
			if (reverting && pillarBlocks.isEmpty()) return UpdateResult.REMOVE;
			if (!reverting && currentLength >= length) reverting = true;

			long time = System.currentTimeMillis();
			if (time < nextUpdateTime) return UpdateResult.CONTINUE;
			nextUpdateTime = time + 70;

			if (reverting) {
				if (pillarBlocks.isEmpty()) return UpdateResult.REMOVE;
				Block block = pillarBlocks.pollFirst();
				clean(block);
				SoundUtil.ICE_SOUND.play(block.getLocation());
				return UpdateResult.CONTINUE;
			}

			Block currentIndex = origin.getRelative(BlockFace.UP, ++currentLength);
			AABB collider = AABB.BLOCK_BOUNDS.at(new Vector3(currentIndex));
			CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true, true);

			if (canMove(currentIndex)) {
				pillarBlocks.offerFirst(currentIndex);
				TempBlock.create(currentIndex, material.createBlockData());
				SoundUtil.ICE_SOUND.play(currentIndex.getLocation());
			} else {
				reverting = true;
			}

			return UpdateResult.CONTINUE;
		}

		private boolean canMove(Block newBlock) {
			if (MaterialUtil.isLava(newBlock)) return false;
			if (!MaterialUtil.isTransparent(newBlock) && newBlock.getType() != Material.WATER) return false;
			BlockMethods.tryBreakPlant(newBlock);
			return true;
		}

		private boolean onEntityHit(@NonNull Entity entity) {
			if (affectedEntities.contains(entity) || entity.equals(user.getEntity())) return false;
			affectedEntities.add(entity);
			entity.setVelocity(Vector3.PLUS_J.scalarMultiply(userConfig.knockup).clampVelocity());
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			return true;
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public double knockup;

		@Attribute(Attribute.COOLDOWN)
		public long columnCooldown;
		@Attribute(Attribute.HEIGHT)
		public int columnMaxHeight;
		@Attribute(Attribute.COOLDOWN)
		public long fieldCooldown;
		@Attribute(Attribute.RADIUS)
		public double radius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "icespike");

			selectRange = abilityNode.node("select-range").getDouble(10.0);
			damage = abilityNode.node("damage").getDouble(3.0);
			knockup = abilityNode.node("knock-up").getDouble(0.8);

			CommentedConfigurationNode columnNode = abilityNode.node("column");
			columnCooldown = columnNode.node("cooldown").getLong(1500);
			columnMaxHeight = columnNode.node("max-height").getInt(5);

			CommentedConfigurationNode fieldNode = abilityNode.node("field");

			fieldCooldown = fieldNode.node("cooldown").getLong(5000);
			radius = fieldNode.node("radius").getDouble(10.0);
		}
	}
}
