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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class PhaseChange extends AbilityInstance implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private final PhaseTransformer freeze = new PhaseTransformer(MaterialUtil::isWater, Material.ICE);
	private final PhaseTransformer melt = new PhaseTransformer(WaterMaterials::isIceBendable, Material.WATER);

	public PhaseChange(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		freeze.processQueue();
		if (user.isSneaking() && getDescription().equals(user.getSelectedAbility().orElse(null))) {
			melt.processQueue();
		}
		return UpdateResult.CONTINUE;
	}

	public void freeze() {
		if (!user.canBend(getDescription())) return;
		freeze.fillQueue(userConfig.freezeRange, userConfig.freezeRadius);
	}

	public void melt() {
		if (!user.canBend(getDescription())) return;
		melt.fillQueue(userConfig.meltRange, userConfig.meltRadius);
	}

	public static void freeze(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("PhaseChange")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, PhaseChange.class).ifPresent(PhaseChange::freeze);
		}
	}

	public static void melt(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("PhaseChange")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, PhaseChange.class).ifPresent(PhaseChange::melt);
		}
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class PhaseTransformer {
		private final Queue<Block> queue;
		private final Predicate<Block> predicate;
		private final Material material;

		private final boolean isFreeze;

		private PhaseTransformer(@NonNull Predicate<Block> predicate, @NonNull Material material) {
			queue = new ArrayDeque<>(32);
			this.predicate = predicate;
			this.material = material;
			isFreeze = material == Material.ICE;
		}

		private void fillQueue(double range, double radius) {
			Location center = WorldMethods.getTarget(user.getWorld(), user.getRay(range), !isFreeze).toLocation(user.getWorld());
			boolean acted = false;
			for (Block block : WorldMethods.getNearbyBlocks(center, radius, predicate)) {
				if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) continue;
				queue.offer(block);
				acted = true;
			}
			if (acted) user.setCooldown(getDescription(), userConfig.cooldown);
		}

		private void processQueue() {
			int counter = 0;
			while (!queue.isEmpty() && counter <= userConfig.speed) {
				Block block = queue.poll();
				if (TempBlock.isBendable(block) && predicate.test(block)) {
					Optional<TempBlock> tb = TempBlock.MANAGER.get(block);
					if (tb.isPresent()) {
						tb.get().revert();
					} else {
						TempBlock.create(block, material, true);
						if (isFreeze && ThreadLocalRandom.current().nextInt(12) == 0) {
							SoundUtil.ICE_SOUND.play(block.getLocation());
						}
					}
					counter++;
				}
			}
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.SPEED)
		public long speed;
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.SELECTION)
		public double freezeRange;
		@Attribute(Attribute.RADIUS)
		public double freezeRadius;

		@Attribute(Attribute.SELECTION)
		public double meltRange;
		@Attribute(Attribute.RADIUS)
		public double meltRadius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "phasechange");

			speed = abilityNode.node("speed").getInt(8);
			cooldown = abilityNode.node("cooldown").getLong(3000);

			freezeRange = abilityNode.node("freeze").node("range").getDouble(7.0);
			freezeRadius = abilityNode.node("freeze").node("radius").getDouble(3.5);

			meltRange = abilityNode.node("melt").node("range").getDouble(7.0);
			meltRadius = abilityNode.node("melt").node("radius").getDouble(4.5);

			abilityNode.node("speed").comment("How many blocks can be affected per tick.");
		}
	}
}

