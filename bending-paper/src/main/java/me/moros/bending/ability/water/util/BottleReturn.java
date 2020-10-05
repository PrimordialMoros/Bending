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

package me.moros.bending.ability.water.util;

import me.moros.bending.ability.common.TravellingSource;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SourceUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.Objects;

public class BottleReturn implements Ability {
	public static final Config config = new Config();

	private User user;
	private Config userConfig;

	private RemovalPolicy removalPolicy;
	private StateChain states;
	private final Block source;

	public BottleReturn(Block source) {
		this.source = Objects.requireNonNull(source);
	}

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		states = new StateChain(Collections.singletonList(source))
			.addState(new TravellingSource(user, Material.WATER.createBlockData(), 1.5, userConfig.maxDistance))
			.start();
		removalPolicy = Policies.builder().build();
		return source.getType() == Material.WATER;
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		UpdateResult result = states.update();
		if (states.isComplete()) SourceUtil.fillBottle(user);
		return result;
	}

	@Override
	public void destroy() {
		TempBlock.manager.get(source).filter(tb -> tb.getBlock().getType() == Material.WATER).ifPresent(TempBlock::revert);
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "BottleReturn";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	public static class Config extends Configurable {
		public boolean enabled;
		@Attribute(Attributes.RANGE)
		public double maxDistance;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("properties", "bottlebending");

			enabled = abilityNode.getNode("enabled").getBoolean(true);
			maxDistance = abilityNode.getNode("max-distance").getDouble(40.0);
		}
	}
}
