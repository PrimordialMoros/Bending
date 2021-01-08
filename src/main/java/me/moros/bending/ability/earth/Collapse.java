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
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

public class Collapse extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Predicate<Block> predicate;
	private final Collection<Pillar> pillars = new ArrayList<>();

	private int height;

	public Collapse(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		predicate = b -> EarthMaterials.isEarthNotLava(user, b);
		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, predicate, true);
		if (!source.isPresent()) return false;
		Block origin = source.get();

		height = userConfig.maxHeight;

		boolean sneak = method == ActivationMethod.SNEAK;
		if (sneak) {
			int offset = NumberConversions.ceil(userConfig.radius);
			int size = offset * 2 + 1;
			// Micro optimization, construct 2d map of pillar locations to avoid instantiating pillars in the same x, z with different y
			boolean[][] checked = new boolean[size][size];
			for (Block block : WorldMethods.getNearbyBlocks(origin.getLocation(), userConfig.radius, predicate)) {
				if (block.getY() < origin.getY()) continue;
				int dx = offset + origin.getX() - block.getX();
				int dz = offset + origin.getZ() - block.getZ();
				if (checked[dx][dz]) continue;
				Optional<Pillar> pillar = getBottomValid(block).flatMap(this::createPillar);
				if (pillar.isPresent()) {
					checked[dx][dz] = true;
					pillars.add(pillar.get());
				}
			}
		} else {
			getBottomValid(origin).flatMap(this::createPillar).ifPresent(pillars::add);
		}
		if (!pillars.isEmpty()) {
			user.setCooldown(getDescription(), userConfig.cooldown);
			removalPolicy = Policies.builder().build();
			return true;
		}
		return false;
	}

	public boolean activate(@NonNull User user, @NonNull Collection<Block> sources, int height) {
		this.user = user;
		recalculateConfig();
		predicate = b -> EarthMaterials.isEarthNotLava(user, b);
		this.height = height;
		for (Block block : sources) {
			getBottomValid(block).flatMap(this::createPillar).ifPresent(pillars::add);
		}
		if (!pillars.isEmpty()) {
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

	private Optional<Pillar> createPillar(Block block) {
		if (!predicate.test(block) || !TempBlock.isBendable(block)) return Optional.empty();
		return Pillar.builder(user, block)
			.setDirection(BlockFace.DOWN)
			.setInterval(125)
			.setPredicate(predicate).build(height);
	}

	private Optional<Block> getBottomValid(Block block) {
		for (int i = 1; i <= height; i++) {
			Block check = block.getRelative(BlockFace.DOWN, i);
			if (!predicate.test(check)) return Optional.of(check.getRelative(BlockFace.UP));
		}
		return Optional.empty();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		@Attribute(Attribute.RADIUS)
		public double radius;
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.HEIGHT)
		public int maxHeight;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "collapse");

			selectRange = abilityNode.node("select-range").getDouble(20.0);
			radius = abilityNode.node("radius").getDouble(5.0);
			maxHeight = abilityNode.node("max-height").getInt(6);
			cooldown = abilityNode.node("cooldown").getLong(500);
		}
	}
}
