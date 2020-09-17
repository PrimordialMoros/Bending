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

package me.moros.bending.ability.fire;

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.function.Predicate;

public class HeatControl implements Ability {
	public static Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;
	private long startTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (method == ActivationMethod.PUNCH) {
			if (melt()) {
				user.setCooldown(this, userConfig.meltCooldown);
			}
			if (extinguish()) {
				user.setCooldown(this, userConfig.extinguishCooldown);
			}
		} else if (method == ActivationMethod.SNEAK) {
			removalPolicy = CompositeRemovalPolicy.defaults()
				.add(Policies.NOT_SNEAKING)
				.add(new SwappedSlotsRemovalPolicy(getDescription()))
				.build();
			startTime = System.currentTimeMillis();
			return true;
		}
		return false;
	}

	@Override
	public void recalculateConfig() {
		this.userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	private boolean melt() {
		return act(userConfig.meltRange, userConfig.meltRadius, MaterialUtil::isIce);
	}

	private boolean extinguish() {
		return act(userConfig.extinguishRange, userConfig.extinguishRadius, MaterialUtil::isFire);
	}

	private boolean cook() {
		if (user instanceof BendingPlayer) {
			PlayerInventory inventory = ((BendingPlayer) user).getEntity().getInventory();
			Material heldItem = inventory.getItemInMainHand().getType();
			if (MaterialUtil.COOKABLE.containsKey(heldItem)) {
				ItemStack cooked = new ItemStack(MaterialUtil.COOKABLE.get(heldItem));
				inventory.addItem(cooked).values().forEach(item -> user.getWorld().dropItem(user.getHeadBlock().getLocation(), item));
				int amount = inventory.getItemInMainHand().getAmount();
				if (amount == 1) {
					inventory.clear(inventory.getHeldItemSlot());
				} else {
					inventory.getItemInMainHand().setAmount(amount - 1);
				}
				return true;
			}
		}
		return false;
	}


	private boolean act(double range, double radius, Predicate<Block> predicate) {
		Ray ray = new Ray(user.getEyeLocation(), user.getDirection());
		Block b = WorldMethods.blockCast(user.getWorld(), ray, (int) range, Collections.emptySet());
		boolean acted = false;
		for (Block block : WorldMethods.getNearbyBlocks(b.getLocation(), radius, predicate)) {
			if (!Game.getProtectionSystem().canBuild(user, block)) continue;
			acted = true;
			TempBlock.create(block, Material.AIR);
		}
		return acted;
	}

	public static boolean canBurn(User user) {
		AbilityDescription current = user.getSelectedAbility().orElse(null);
		if (current == null || !current.getName().equals("HeatControl") || !user.canBend(current)) {
			return true;
		}
		user.getEntity().setFireTicks(0);
		return false;
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.shouldRemove(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		if (System.currentTimeMillis() >= startTime + userConfig.cookDuration && cook()) {
			startTime = System.currentTimeMillis();
		} else {
			ParticleUtil.createFire(user, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
		}
		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "HeatControl";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	public static class Config extends Configurable {
		public boolean enabled;

		@Attribute(Attributes.COOLDOWN)
		public long extinguishCooldown;
		@Attribute(Attributes.RANGE)
		public double extinguishRange;
		@Attribute(Attributes.RADIUS)
		public double extinguishRadius;

		@Attribute(Attributes.COOLDOWN)
		public long meltCooldown;
		@Attribute(Attributes.RANGE)
		public double meltRange;
		@Attribute(Attributes.RADIUS)
		public double meltRadius;

		@Attribute(Attributes.DURATION)
		public long cookDuration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "heatcontrol");

			enabled = abilityNode.getNode("enabled").getBoolean(true);

			CommentedConfigurationNode extinguishNode = abilityNode.getNode("extinguish");

			extinguishCooldown = extinguishNode.getNode("cooldown").getLong(500);
			extinguishRange = extinguishNode.getNode("range").getDouble(20.0);
			extinguishRadius = extinguishNode.getNode("radius").getDouble(7.0);

			CommentedConfigurationNode meltNode = abilityNode.getNode("melt");

			meltCooldown = meltNode.getNode("cooldown").getLong(500);
			meltRange = meltNode.getNode("range").getDouble(15.0);
			meltRadius = meltNode.getNode("radius").getDouble(5.0);

			CommentedConfigurationNode cookNode = abilityNode.getNode("cook");

			cookDuration = cookNode.getNode("duration").getLong(1000);
		}
	}
}

