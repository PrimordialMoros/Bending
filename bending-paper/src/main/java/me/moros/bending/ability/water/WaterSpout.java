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

import me.moros.bending.ability.common.Spout;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
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
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class WaterSpout implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<TempBlock> column = new ArrayList<>();
	private final Predicate<Block> predicate = b -> MaterialUtil.isWater(b) || MaterialUtil.isIce(b);
	private Spout spout;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		if (Game.getAbilityManager(user.getWorld()).destroyInstanceType(user, WaterSpout.class)) {
			return false;
		}

		this.user = user;
		recalculateConfig();

		double h = userConfig.height + 2;
		if (WorldMethods.distanceAboveGround(user.getEntity()) > h) {
			return false;
		}

		Block block = WorldMethods.blockCast(user.getWorld(), new Ray(user.getLocation(), Vector3.MINUS_J), h).orElse(null);
		if (block == null || !predicate.test(block)) {
			return false;
		}

		removalPolicy = Policies.builder().build();

		spout = new BlockSpout(user);
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

		return spout.update();
	}

	@Override
	public void onDestroy() {
		spout.getFlight().setFlying(false);
		spout.getFlight().release();
		column.forEach(TempBlock::revert);
		user.setCooldown(this, userConfig.cooldown);
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "WaterSpout";
	}

	@Override
	public Collection<Collider> getColliders() {
		return Collections.singletonList(spout.getCollider());
	}

	@Override
	public void onCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	public void handleMovement(Vector velocity) {
		Spout.limitVelocity(user, velocity, userConfig.maxSpeed);
	}

	private class BlockSpout extends Spout {
		private BlockVector blockVector;
		private final Vector g = new Vector(0, -0.1, 0); // Applied as extra gravity

		public BlockSpout(User user) {
			super(user, userConfig.height, userConfig.maxSpeed);
			validBlock = predicate;
		}

		@Override
		public void render(double distance) {
			BlockVector userBlockVector = new BlockVector(user.getLocation().toVector());
			if (userBlockVector.equals(blockVector)) return;
			blockVector = userBlockVector;
			column.forEach(TempBlock::revert);
			column.clear();
			ignore.clear();
			Block block = user.getLocBlock();
			for (int i = 0; i < distance - 1; i++) {
				TempBlock.create(block.getRelative(BlockFace.DOWN, i), Material.WATER).ifPresent(column::add);
			}
			column.stream().map(TempBlock::getBlock).forEach(ignore::add);
		}

		@Override
		public void postRender() {
			if (!user.isFlying()) {
				user.getEntity().setVelocity(user.getEntity().getVelocity().add(g));
			}
			if (ThreadLocalRandom.current().nextInt(8) == 0) {
				SoundUtil.WATER_SOUND.play(user.getEntity().getLocation());
			}
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.HEIGHT)
		public double height;
		@Attribute(Attributes.SPEED)
		public double maxSpeed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "waterspout");

			cooldown = abilityNode.getNode("cooldown").getLong(0);
			height = abilityNode.getNode("height").getDouble(14.0);
			maxSpeed = abilityNode.getNode("max-speed").getDouble(0.2);
		}
	}
}
