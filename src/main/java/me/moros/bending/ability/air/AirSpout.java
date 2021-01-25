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

package me.moros.bending.ability.air;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractSpout;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class AirSpout extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private AbstractSpout spout;

	public AirSpout(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).destroyInstanceType(user, AirSpout.class)) {
			return false;
		}
		if (user.getHeadBlock().isLiquid()) {
			return false;
		}

		this.user = user;
		recalculateConfig();

		double h = userConfig.height + 2;
		if (WorldMethods.distanceAboveGround(user.getEntity()) > h) {
			return false;
		}

		Block block = WorldMethods.blockCast(user.getWorld(), new Ray(user.getLocation(), Vector3.MINUS_J), h).orElse(null);
		if (block == null) {
			return false;
		}

		removalPolicy = Policies.builder().build();

		spout = new Spout();
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

		if (user.getHeadBlock().isLiquid()) {
			return UpdateResult.REMOVE;
		}

		return spout.update();
	}

	@Override
	public void onDestroy() {
		spout.getFlight().setFlying(false);
		spout.getFlight().release();
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(spout.getCollider());
	}

	public void handleMovement(Vector velocity) {
		AbstractSpout.limitVelocity(user, velocity, userConfig.maxSpeed);
	}

	private class Spout extends AbstractSpout {
		private long nextRenderTime;

		public Spout() {
			super(user, userConfig.height, userConfig.maxSpeed);
			nextRenderTime = 0;
		}

		@Override
		public void render() {
			long time = System.currentTimeMillis();
			if (time < nextRenderTime) return;
			for (int i = 0; i < distance; i++) {
				ParticleUtil.createAir(user.getEntity().getLocation().subtract(0, i, 0))
					.count(3).offset(0.4, 0.4, 0.4).spawn();
			}
			nextRenderTime = time + 100;
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(8) == 0) {
				SoundUtil.AIR_SOUND.play(user.getEntity().getLocation());
			}
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.HEIGHT)
		public double height;
		@Attribute(Attribute.SPEED)
		public double maxSpeed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airspout");

			cooldown = abilityNode.node("cooldown").getLong(2000);
			height = abilityNode.node("height").getDouble(11.0);
			maxSpeed = abilityNode.node("max-speed").getDouble(0.2);
		}
	}
}
