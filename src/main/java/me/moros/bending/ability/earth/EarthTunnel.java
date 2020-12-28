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
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class EarthTunnel extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Predicate<Block> predicate;
	private Vector3 center;

	private double distance = 0;
	private int radius = 0;
	private int angle = 0;

	public EarthTunnel(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, EarthTunnel.class)) return false;

		this.user = user;
		recalculateConfig();

		predicate = b -> EarthMaterials.isEarthNotLava(user, b);
		Optional<Block> block = SourceUtil.getSource(user, userConfig.range, predicate);
		if (!block.isPresent()) return false;

		center = new Vector3(block.get()).add(Vector3.HALF);
		removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();

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
		for (int i = 0; i < 2; i++) {
			if (distance > userConfig.range) {
				return UpdateResult.REMOVE;
			}
			Vector3 offset = VectorMethods.getOrthogonal(user.getDirection(), FastMath.toRadians(angle), radius);
			Block current = center.add(offset).toBlock(user.getWorld());
			if (!Bending.getGame().getProtectionSystem().canBuild(user, current)) {
				return UpdateResult.REMOVE;
			}
			if (predicate.test(current)) {
				extract(current);
				TempBlock.create(current, Material.AIR, userConfig.regen, true);
			}
			if (angle >= 360) {
				angle = 0;
				Optional<Block> block = SourceUtil.getSource(user, userConfig.range, predicate);
				if (!block.isPresent()) return UpdateResult.REMOVE;
				center = new Vector3(block.get()).add(Vector3.HALF);

				if (++radius > userConfig.radius) {
					radius = 0;
					if (++distance > userConfig.range) {
						return UpdateResult.REMOVE;
					}
				}
			} else {
				if (radius <= 0) {
					radius++;
				} else {
					angle += 360 / (radius * 16);
				}
			}
		}
		return UpdateResult.CONTINUE;
	}

	// TODO tweak drop rates
	private void extract(Block block) {
		if (!userConfig.extractOres || !TempBlock.isBendable(block)) return;
		Material mat = block.getType().name().contains("NETHER") ? Material.NETHERRACK : Material.STONE;
		TempBlock.manager.get(block).ifPresent(tb -> tb.overwriteSnapshot(mat.createBlockData()));

		Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
		int rand = ThreadLocalRandom.current().nextInt(100);
		int factor = rand >= 75 ? 3 : rand >= 50 ? 2 : 1;

		switch (block.getType()) {
			case COAL_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.COAL, factor));
				break;
			case LAPIS_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.LAPIS_LAZULI, 9 * factor));
				break;
			case REDSTONE_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.REDSTONE, 5 * factor));
				break;
			case DIAMOND_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.DIAMOND, factor));
				break;
			case EMERALD_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.EMERALD, factor));
				break;
			case NETHER_QUARTZ_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.QUARTZ, factor));
				break;
			case IRON_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.IRON_INGOT, factor));
				break;
			case GOLD_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.GOLD_INGOT, factor));
				break;
			case NETHER_GOLD_ORE:
				block.getWorld().dropItem(dropLocation, new ItemStack(Material.GOLD_NUGGET, 6 * factor));
				break;
		}
	}

	@Override
	public void onDestroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.RADIUS)
		public int radius;
		@Attribute(Attribute.DURATION)
		public long regen;
		public boolean extractOres;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "earthtunnel");

			cooldown = abilityNode.getNode("cooldown").getLong(2000);
			range = abilityNode.getNode("range").getDouble(10.0);
			radius = abilityNode.getNode("radius").getInt(1);
			regen = abilityNode.getNode("revert-time").getLong(0);
			extractOres = abilityNode.getNode("extract-ores").getBoolean(true);
		}
	}
}
