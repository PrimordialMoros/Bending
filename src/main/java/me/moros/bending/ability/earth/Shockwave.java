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
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.UserMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Shockwave extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<Ripple> streams = new ArrayList<>();
	private final Set<Entity> affectedEntities = new HashSet<>();
	private final Set<Block> affectedBlocks = new HashSet<>();

	private boolean released;
	private long startTime;

	public Shockwave(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock())) {
			return false;
		}
		removalPolicy = Policies.builder().add(new SwappedSlotsRemovalPolicy(getDescription())).build();
		released = false;
		if (method == ActivationMethod.FALL) {
			if (user.getEntity().getFallDistance() < userConfig.fallThreshold || user.isSneaking()) {
				return false;
			}
			release(false);
		}

		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		if (!released) {
			boolean charged = isCharged();
			if (charged) {
				ParticleUtil.create(Particle.SMOKE_NORMAL, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
				if (!user.isSneaking()) {
					release(false);
				}
			} else {
				if (!user.isSneaking()) return UpdateResult.REMOVE;
			}
			return UpdateResult.CONTINUE;
		}
		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	public boolean isCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
	}

	public static void activateCone(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("Shockwave")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, Shockwave.class)
				.ifPresent(s -> s.release(true));
		}
	}

	private void release(boolean cone) {
		if (released || !isCharged()) return;
		released = true;
		double range = cone ? userConfig.coneRange : userConfig.ringRange;
		for (double theta = 0; theta < FastMath.PI * 2; theta += FastMath.toRadians(4)) {
			Vector3 direction = new Vector3(FastMath.cos(theta), 0,  FastMath.sin(theta));
			if (cone && Vector3.angle(direction, user.getDirection()) > FastMath.toRadians(30)) {
				continue;
			}
			Block source = user.getLocation().add(Vector3.MINUS_J).add(direction).toBlock(user.getWorld());
			streams.add(new Ripple(user, source, direction, range));
		}
		removalPolicy = Policies.builder().build();
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public User getUser() {
		return user;
	}

	private class Ripple extends AbstractLine {
		public Ripple(User user, Block source, Vector3 dir, double range) {
			super(user, source, range, 0.8, false);
			livingOnly = false;
			targetLocation = new Vector3(source).add(Vector3.HALF).add(dir.scalarMultiply(range));
			direction = targetLocation.subtract(location).setY(0).normalize();
		}

		@Override
		public void render() {
			Block block = location.toBlock(user.getWorld());
			if (!affectedBlocks.contains(block)) {
				affectedBlocks.add(block);
				double deltaY = FastMath.min(0.35, 0.1 + location.distance(origin) / (1.5 * range));
				BlockData bd = block.getRelative(BlockFace.DOWN).getBlockData();
				new BendingFallingBlock(block, bd, new Vector3(0, deltaY, 0), true, 3000);

				if (ThreadLocalRandom.current().nextInt(6) == 0) {
					SoundUtil.EARTH_SOUND.play(location.toLocation(user.getWorld()));
				}
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			if (!affectedEntities.contains(entity)) {
				DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
				double deltaY = FastMath.min(0.6, 0.3 + location.distance(origin) / (1.5 * range));
				Vector3 push = direction.setY(deltaY).scalarMultiply(userConfig.knockback);
				entity.setVelocity(push.clampVelocity().toVector());
				affectedEntities.add(entity);
			}
			return false;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			return MaterialUtil.isWater(block);
		}

		@Override
		protected boolean isValidBlock(@NonNull Block block) {
			if (!MaterialUtil.isTransparent(block.getRelative(BlockFace.UP))) return false;
			return EarthMaterials.isEarthbendable(user, block) && !block.isLiquid();
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.CHARGE_TIME)
		public long chargeTime;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public double knockback;
		@Attribute(Attribute.RANGE)
		public double ringRange;
		@Attribute(Attribute.RANGE)
		public double coneRange;
		public double fallThreshold;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "shockwave");

			cooldown = abilityNode.getNode("cooldown").getLong(8000);
			chargeTime = abilityNode.getNode("charge-time").getInt(2500);
			damage = abilityNode.getNode("damage").getDouble(4.0);
			knockback = abilityNode.getNode("knockback").getDouble(1.8);
			coneRange = abilityNode.getNode("cone-range").getDouble(14.0);
			ringRange = abilityNode.getNode("ring-range").getDouble(9.0);
			fallThreshold = abilityNode.getNode("fall-threshold").getDouble(12.0);
		}
	}
}
