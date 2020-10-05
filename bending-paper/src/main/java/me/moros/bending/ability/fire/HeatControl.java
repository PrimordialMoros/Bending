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
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;
import java.util.function.Predicate;

public class HeatControl implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private long startTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		startTime = System.currentTimeMillis();
		return false;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		AbilityDescription selectedAbility = user.getSelectedAbility().orElse(null);
		if (!user.canBend(selectedAbility) || !getDescription().equals(selectedAbility)) {
			return UpdateResult.CONTINUE;
		}
		long time = System.currentTimeMillis();
		if (!user.isSneaking()) {
			startTime = time;
		} else {
			ParticleUtil.createFire(user, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
			if (time > startTime + userConfig.cookInterval && cook()) {
				startTime = System.currentTimeMillis();
			}
		}
		return UpdateResult.CONTINUE;
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

	public void act() {
		if (!user.canBend(getDescription())) return;
		Predicate<Block> predicate = b -> MaterialUtil.isIce(b) || MaterialUtil.isFire(b);
		boolean acted = false;
		Location center = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range));
		for (Block block : WorldMethods.getNearbyBlocks(center, userConfig.radius, predicate)) {
			if (!Game.getProtectionSystem().canBuild(user, block)) continue;
			acted = true;
			if (MaterialUtil.isIce(block)) {
				Optional<TempBlock> tb = TempBlock.manager.get(block);
				if (tb.isPresent()) {
					tb.get().revert();
				} else {
					TempBlock.create(block, Material.WATER);
				}
			} else {
				block.setType(Material.AIR);
			}
		}
		if (acted) user.setCooldown(this, userConfig.cooldown);
	}

	public static void act(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("HeatControl")) {
			Game.getAbilityManager(user.getWorld()).getFirstInstance(user, HeatControl.class).ifPresent(HeatControl::act);
		}
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
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.RADIUS)
		public double radius;
		@Attribute(Attributes.CHARGE_TIME)
		public long cookInterval;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "heatcontrol");

			cooldown = abilityNode.getNode("cooldown").getLong(500);
			range = abilityNode.getNode("range").getDouble(20.0);
			radius = abilityNode.getNode("radius").getDouble(7.0);
			cookInterval = abilityNode.getNode("cook-interval").getLong(2000);
		}
	}
}

