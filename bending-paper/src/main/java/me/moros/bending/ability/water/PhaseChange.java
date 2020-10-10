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

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PhaseChange implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private final Queue<Block> freezeQueue = new ArrayDeque<>(32);
	private final Queue<Block> meltQueue = new ArrayDeque<>(32);

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		processQueue(freezeQueue, MaterialUtil::isWater, block -> {
			Optional<TempBlock> tb = TempBlock.manager.get(block);
			if (tb.isPresent()) {
				tb.get().revert();
			} else {
				TempBlock.create(block, Material.ICE, true);
			}
		});

		if (user.isSneaking() && getDescription().equals(user.getSelectedAbility().orElse(null))) {
			processQueue(meltQueue, MaterialUtil::isIce, block -> {
				Optional<TempBlock> tb = TempBlock.manager.get(block);
				if (tb.isPresent()) {
					tb.get().revert();
				} else {
					TempBlock.create(block, Material.WATER, true);
				}
			});
		}

		return UpdateResult.CONTINUE;
	}

	private boolean fillQueue(double range, double radius, Predicate<Block> predicate, Queue<Block> queue) {
		if (!user.canBend(getDescription())) return false;
		Location center = WorldMethods.getTarget(user.getWorld(), user.getRay(range));
		boolean acted = false;
		for (Block block : WorldMethods.getNearbyBlocks(center, radius, predicate)) {
			if (!Game.getProtectionSystem().canBuild(user, block)) continue;
			queue.offer(block);
			acted = true;
		}
		return acted;
	}

	private void processQueue(Queue<Block> queue, Predicate<Block> valid, Consumer<Block> alter) {
		int counter = 0;
		while (!queue.isEmpty() && counter <= userConfig.speed) {
			Block block = queue.poll();
			if (TempBlock.isBendable(block) && valid.test(block)) {
				alter.accept(block);
				counter++;
			}
		}
	}

	public void freeze() {
		if (fillQueue(userConfig.freezeRange, userConfig.freezeRadius, MaterialUtil::isWater, freezeQueue)) {
			user.setCooldown(this, userConfig.cooldown);
		}
	}

	public void melt() {
		if (fillQueue(userConfig.meltRange, userConfig.meltRadius, MaterialUtil::isIce, meltQueue)) {
			user.setCooldown(this, userConfig.cooldown);
		}
	}

	public static void freeze(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("PhaseChange")) {
			Game.getAbilityManager(user.getWorld()).getFirstInstance(user, PhaseChange.class).ifPresent(PhaseChange::freeze);
		}
	}

	public static void melt(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("PhaseChange")) {
			Game.getAbilityManager(user.getWorld()).getFirstInstance(user, PhaseChange.class).ifPresent(PhaseChange::melt);
		}
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "PhaseChange";
	}

	@Override
	public void onCollision(Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.SPEED)
		public long speed;
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.SELECTION)
		public double freezeRange;
		@Attribute(Attributes.RADIUS)
		public double freezeRadius;

		@Attribute(Attributes.SELECTION)
		public double meltRange;
		@Attribute(Attributes.RADIUS)
		public double meltRadius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "phasechange");

			speed = abilityNode.getNode("speed").getInt(8);
			cooldown = abilityNode.getNode("cooldown").getLong(3000);

			freezeRange = abilityNode.getNode("freeze").getNode("range").getDouble(7.0);
			freezeRadius = abilityNode.getNode("freeze").getNode("radius").getDouble(3.0);

			meltRange = abilityNode.getNode("melt").getNode("range").getDouble(7.0);
			meltRadius = abilityNode.getNode("melt").getNode("radius").getDouble(4.0);

			abilityNode.getNode("speed").setComment("How many blocks can be affected per tick.");
		}
	}
}

