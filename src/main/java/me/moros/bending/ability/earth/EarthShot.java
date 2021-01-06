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
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class EarthShot extends AbilityInstance implements Ability {
	private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(new Vector3(0.3, 0.3, 0.3));

	private enum Mode {ROCK, METAL, MAGMA}

	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Mode mode;
	private Block readySource;
	private Vector3 location;
	private Vector3 lastVelocity;
	private BendingFallingBlock projectile;

	private boolean ready = false;
	private boolean launched = false;
	private boolean canConvert = false;
	private double damage;
	private int targetY;
	private long magmaStartTime = 0;

	public EarthShot(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		long count = Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, EarthShot.class).filter(e -> !e.launched).count();
		if (count >= userConfig.maxAmount) return false;

		canConvert = userConfig.allowConvertMagma && user.hasPermission("bending.lava");

		return prepare();
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}


	private boolean prepare() {
		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, b -> EarthMaterials.isEarthbendable(user, b));
		if (!source.isPresent()) return false;
		Block block = source.get();
		mode = getType(block);
		int deltaY = 3;
		if (block.getY() >= user.getHeadBlock().getY()) {
			targetY = block.getY() + 2;
		} else {
			targetY = user.getLocBlock().getY() + 2;
			deltaY = 1 + targetY - block.getY();
		}

		for (int i = 1; i <= deltaY; i++) {
			Block temp = block.getRelative(BlockFace.UP, i);
			if (!MaterialUtil.isTransparent(temp)) return false;
			BlockMethods.breakPlant(temp);
		}

		BlockData data;
		if (mode == Mode.MAGMA) {
			data = Material.MAGMA_BLOCK.createBlockData();
			canConvert = false;
		} else {
			data = MaterialUtil.getSolidType(block.getBlockData());
		}
		if (mode == Mode.METAL) {
			SoundUtil.METAL_SOUND.play(block.getLocation());
			canConvert = false;
		} else {
			SoundUtil.EARTH_SOUND.play(block.getLocation());
		}

		projectile = new BendingFallingBlock(block, data, new Vector3(0, 0.65, 0), false, 6000);
		if (!MaterialUtil.isLava(block))
			TempBlock.create(block, Material.AIR, BendingProperties.EARTHBENDING_REVERT_TIME, true);

		location = projectile.getCenter();
		removalPolicy = Policies.builder()
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.add(new OutOfRangeRemovalPolicy(userConfig.selectRange + 5, () -> location))
			.build();

		return true;
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		if (launched) {
			if (projectile == null || !projectile.getFallingBlock().isValid()) {
				return UpdateResult.REMOVE;
			}

			Vector3 velocity = new Vector3(projectile.getFallingBlock().getVelocity());
			if (Vector3.angle(lastVelocity, velocity) > FastMath.PI / 4 || velocity.getNormSq() < 2.25) {
				return UpdateResult.REMOVE;
			}
			if (user.isSneaking()) {
				Vector3 dir = user.getDirection().scalarMultiply(0.2);
				velocity = velocity.add(dir.setY(0));
			}
			projectile.getFallingBlock().setVelocity(velocity.normalize().scalarMultiply(1.8).clampVelocity());
			lastVelocity = new Vector3(projectile.getFallingBlock().getVelocity());
			if (CollisionUtil.handleEntityCollisions(user, BOX.at(projectile.getCenter()), this::onEntityHit, true)) {
				return UpdateResult.REMOVE;
			}
		} else {
			if (!ready) {
				handleSource();
			} else {
				handleMagma();
			}
		}

		return UpdateResult.CONTINUE;
	}

	private boolean onEntityHit(Entity entity) {
		DamageUtil.damageEntity(entity, user, damage, getDescription());
		if (entity instanceof LivingEntity && userConfig.maxAmount > 1) {
			((LivingEntity) entity).setNoDamageTicks(0);
		}
		return true;
	}

	private void handleSource() {
		Block block = projectile.getFallingBlock().getLocation().getBlock();
		if (block.getY() >= targetY) {
			TempBlock.create(block, projectile.getFallingBlock().getBlockData(), false);
			projectile.revert();
			location = new Vector3(block);
			readySource = block;
			ready = true;
		} else {
			location = projectile.getCenter();
		}
	}

	private void handleMagma() {
		if (!canConvert) return;
		Block check = WorldMethods.blockCast(user.getWorld(), user.getRay(), userConfig.selectRange * 2).orElse(null);
		if (user.isSneaking() && readySource.equals(check)) {
			if (magmaStartTime == 0) {
				magmaStartTime = System.currentTimeMillis();
				if (userConfig.chargeTime > 0) SoundUtil.LAVA_SOUND.play(readySource.getLocation());
			}

			Location spawnLoc = readySource.getLocation().add(0.5, 0.5, 0.5);
			ParticleUtil.create(Particle.LAVA, spawnLoc).count(2).offset(0.5, 0.5, 0.5).spawn();
			ParticleUtil.create(Particle.SMOKE_NORMAL, spawnLoc).count(2).offset(0.5, 0.5, 0.5).spawn();
			ParticleUtil.createRGB(spawnLoc, "FFA400").count(2).offset(0.5, 0.5, 0.5).spawn();
			ParticleUtil.createRGB(spawnLoc, "FF8C00").count(4).offset(0.5, 0.5, 0.5).spawn();

			if (userConfig.chargeTime <= 0 || System.currentTimeMillis() > magmaStartTime + userConfig.chargeTime) {
				mode = Mode.MAGMA;
				TempBlock.create(readySource, Material.MAGMA_BLOCK, false);
				canConvert = false;
			}
		} else {
			if (magmaStartTime != 0 && ThreadLocalRandom.current().nextInt(6) == 0) {
				removalPolicy = (u, d) -> true; // Remove in next tick
				return;
			}
			magmaStartTime = 0;
		}
	}

	private Mode getType(Block block) {
		if (EarthMaterials.isLavaBendable(block)) {
			return Mode.MAGMA;
		} else if (EarthMaterials.isMetalBendable(block)) {
			return Mode.METAL;
		} else {
			return Mode.ROCK;
		}
	}

	public static void launch(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("EarthShot")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, EarthShot.class)
				.filter(e -> !e.launched).forEach(EarthShot::launch);
		}
	}

	private void launch() {
		if (launched) return;

		boolean prematureLaunch = false;
		if (!ready) {
			if (!userConfig.allowQuickLaunch) return;
			prematureLaunch = true;
		}

		Vector3 origin;
		if (prematureLaunch) {
			origin = projectile.getCenter();
			Vector3 dir = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range)).subtract(origin);
			projectile.getFallingBlock().setGravity(true);
			projectile.getFallingBlock().setVelocity(dir.normalize().scalarMultiply(1.8).clampVelocity());
		} else {
			origin = new Vector3(readySource).add(Vector3.HALF);
			Vector3 dir = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range), Collections.singleton(readySource.getType())).subtract(origin);
			projectile = new BendingFallingBlock(readySource, readySource.getBlockData(), dir.normalize().scalarMultiply(1.8), true, 30000);
			TempBlock.manager.get(readySource).ifPresent(TempBlock::revert);
		}
		location = projectile.getCenter();
		lastVelocity = new Vector3(projectile.getFallingBlock().getVelocity());

		removalPolicy = Policies.builder()
			.add(new OutOfRangeRemovalPolicy(userConfig.range, origin, () -> location))
			.build();

		user.setCooldown(getDescription(), userConfig.cooldown);

		switch (mode) {
			case METAL:
				damage = userConfig.damage * 1.25;
				break;
			case MAGMA:
				damage = userConfig.damage * 1.5;
				break;
			default:
				damage = userConfig.damage;
				break;
		}
		launched = true;
	}

	@Override
	public void onDestroy() {
		if (projectile.getFallingBlock() != null) {
			if (launched) {
				Location spawnLoc = projectile.getCenter().toLocation(user.getWorld());
				BlockData data = projectile.getFallingBlock().getBlockData();
				ParticleUtil.create(Particle.BLOCK_CRACK, spawnLoc).count(6).offset(1, 1, 1).data(data).spawn();
				ParticleUtil.create(Particle.BLOCK_DUST, spawnLoc).count(4).offset(1, 1, 1).data(data).spawn();
				if (mode == Mode.MAGMA) {
					ParticleUtil.create(Particle.SMOKE_LARGE, spawnLoc).count(12).offset(1, 1, 1).extra(0.05).spawn();
					ParticleUtil.create(Particle.FIREWORKS_SPARK, spawnLoc).count(8).offset(1, 1, 1).extra(0.07).spawn();
					SoundUtil.playSound(spawnLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0);
				}
			}
			projectile.revert();
		}
		if (readySource != null) {
			TempBlock.manager.get(readySource).ifPresent(TempBlock::revert);
		}
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.CHARGE_TIME)
		public long chargeTime;
		@Attribute(Attribute.AMOUNT)
		public int maxAmount;

		public boolean allowConvertMagma;
		public boolean allowQuickLaunch;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthshot");

			cooldown = abilityNode.node("cooldown").getLong(1000);
			selectRange = abilityNode.node("select-range").getDouble(6.0);
			range = abilityNode.node("range").getDouble(60.0);
			damage = abilityNode.node("damage").getDouble(3.0);
			chargeTime = abilityNode.node("charge-time").getLong(1500);
			maxAmount = abilityNode.node("max-sources").getInt(3);
			allowConvertMagma = abilityNode.node("allow-convert-magma").getBoolean(true);
			allowQuickLaunch = abilityNode.node("allow-quick-launch").getBoolean(true);
		}
	}
}
