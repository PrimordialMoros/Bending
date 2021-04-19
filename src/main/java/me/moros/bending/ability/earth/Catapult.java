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

package me.moros.bending.ability.earth;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
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
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import java.util.Comparator;
import java.util.function.Predicate;

public class Catapult extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private Block base;
	private Pillar pillar;

	private boolean sneak;
	private long startTime;

	public Catapult(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (!user.isOnGround()) return false;

		this.user = user;
		recalculateConfig();

		base = getBase();
		if (!TempBlock.isBendable(base) || !Bending.getGame().getProtectionSystem().canBuild(user, base)) {
			return false;
		}

		sneak = method == ActivationMethod.SNEAK;

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

	private Block getBase() {
		AABB entityBounds = AABBUtils.getEntityBounds(user.getEntity()).grow(new Vector3(0, 0.1, 0));
		AABB floorBounds = new AABB(new Vector3(-1, -0.5, -1), new Vector3(1, 0, 1)).at(user.getLocation());
		Predicate<Block> predicate = b -> entityBounds.intersects(AABBUtils.getBlockBounds(b)) && !b.isLiquid() && EarthMaterials.isEarthbendable(user, b);
		return WorldMethods.getNearbyBlocks(user.getWorld(), floorBounds, predicate).stream()
			.min(Comparator.comparingDouble(b -> new Vector3(b).add(Vector3.HALF).distanceSq(user.getLocation())))
			.orElse(user.getLocBlock().getRelative(BlockFace.DOWN));
	}

	private boolean launch() {
		user.setCooldown(getDescription(), userConfig.cooldown);
		double power = sneak ? userConfig.sneakPower : userConfig.clickPower;

		Predicate<Block> predicate = b -> EarthMaterials.isEarthNotLava(user, b);
		pillar = Pillar.builder(user, base, EarthPillar::new).setPredicate(predicate).build(3, 1).orElse(null);
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

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.STRENGTH)
		public double sneakPower;
		@Attribute(Attribute.STRENGTH)
		public double clickPower;
		public double angle;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "catapult");

			cooldown = abilityNode.node("cooldown").getLong(3000);
			sneakPower = abilityNode.node("sneak-power").getDouble(2.65);
			clickPower = abilityNode.node("click-power").getDouble(1.8);
			angle = FastMath.toRadians(abilityNode.node("angle").getInt(60));
		}
	}
}
