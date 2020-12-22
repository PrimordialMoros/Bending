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
import me.moros.bending.ability.common.Pillar;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

public class Catapult extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private Block base;
	private Pillar pillar;

	private long startTime;

	public Catapult(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (!WorldMethods.isOnGround(user.getEntity())) return false;

		this.user = user;
		recalculateConfig();

		base = user.getLocBlock().getRelative(BlockFace.DOWN);
		if (!Bending.getGame().getProtectionSystem().canBuild(user, base) || !Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock())) {
			return false;
		}
		if (base.isLiquid() || !EarthMaterials.isEarthbendable(user, base) || !TempBlock.isBendable(base)) {
			return false;
		}
		launch();
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (System.currentTimeMillis() > startTime + 100) {
			return pillar == null ? UpdateResult.REMOVE : pillar.update();
		}
		return UpdateResult.CONTINUE;
	}

	public boolean launch() {
		user.setCooldown(getDescription(), userConfig.cooldown);
		double power = user.isSneaking() ? userConfig.power * 0.666 : userConfig.power;

		Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b) && !b.isLiquid();
		pillar = Pillar.builder(user, base, EarthPillar::new).setPredicate(predicate).build(1).orElse(null);
		SoundUtil.EARTH_SOUND.play(base.getLocation());

		double angle = Vector3.angle(Vector3.PLUS_J, user.getDirection());
		Vector3 direction = angle > userConfig.angle ? Vector3.PLUS_J : user.getDirection();
		Collider collider = new Sphere(new Vector3(user.getLocBlock()).add(Vector3.HALF), 1.5);
		return CollisionUtil.handleEntityCollisions(user, collider, e -> {
			e.setVelocity(direction.scalarMultiply(power).clampVelocity());
			return true;
		}, true, true);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class EarthPillar extends Pillar {
		protected EarthPillar(@NonNull PillarBuilder builder) {
			super(builder);
		}

		@Override
		public void playSound(@NonNull Block block) {
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.STRENGTH)
		public double power;
		public double angle;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "catapult");

			cooldown = abilityNode.getNode("cooldown").getLong(2000);
			power = abilityNode.getNode("power").getDouble(2.4);
			angle = FastMath.toRadians(abilityNode.getNode("angle").getInt(85));
		}
	}
}
