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

package me.moros.bending.ability.fire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.PhaseTransformer;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class HeatControl extends AbilityInstance implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Solidify solidify = new Solidify();
	private final Melt melt = new Melt();

	private long startTime;

	public HeatControl(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		removalPolicy = Policies.builder().build();
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription()) || !user.canBend(getDescription())) {
			solidify.clear();
			melt.clear();
			return UpdateResult.CONTINUE;
		}
		melt.processQueue(1);
		if (getDescription().equals(user.getSelectedAbility().orElse(null))) {
			long time = System.currentTimeMillis();
			if (user.isSneaking()) {
				if (isHoldingFood()) {
					ParticleUtil.createFire(user, user.getMainHandSide().toLocation(user.getWorld())).spawn();
					if (time > startTime + userConfig.cookInterval && cook()) {
						startTime = System.currentTimeMillis();
					}
					return UpdateResult.CONTINUE;
				}
				solidify.processQueue(1);
			} else {
				solidify.clear();
			}
			startTime = time;
		}
		return UpdateResult.CONTINUE;
	}

	private boolean isHoldingFood() {
		if (user instanceof BendingPlayer) {
			PlayerInventory inventory = ((BendingPlayer) user).getEntity().getInventory();
			return MaterialUtil.COOKABLE.containsKey(inventory.getItemInMainHand().getType());
		}
		return false;
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

	private void act() {
		if (!user.canBend(getDescription()) || user.isOnCooldown(getDescription())) return;
		boolean acted = false;
		Location center = user.getTarget(userConfig.range).toLocation(user.getWorld());
		Predicate<Block> predicate = b -> TempBlock.isBendable(b) && MaterialUtil.isFire(b) || MaterialUtil.isSnow(b) || WaterMaterials.isIceBendable(b);
		Predicate<Block> safe = b -> Bending.getGame().getProtectionSystem().canBuild(user, b);
		List<Block> toMelt = new ArrayList<>();
		for (Block block : WorldMethods.getNearbyBlocks(center, userConfig.radius, predicate.and(safe))) {
			acted = true;
			if (MaterialUtil.isFire(block)) {
				BlockMethods.tryExtinguishFire(user, block);
			} else if (MaterialUtil.isSnow(block) || WaterMaterials.isIceBendable(block)) {
				toMelt.add(block);
			}
		}
		if (!toMelt.isEmpty()) {
			Collections.shuffle(toMelt);
			melt.fillQueue(toMelt);
		}
		if (acted) {
			user.setCooldown(getDescription(), userConfig.cooldown);
		}
	}

	private void onSneak() {
		if (!user.canBend(getDescription()) || user.isOnCooldown(getDescription())) return;
		Location center = user.getTarget(userConfig.solidifyRange, false).toLocation(user.getWorld());
		Predicate<Block> safe = b -> Bending.getGame().getProtectionSystem().canBuild(user, b);
		List<Block> newBlocks = WorldMethods.getNearbyBlocks(center, userConfig.solidifyRadius, safe.and(MaterialUtil::isLava));
		if (newBlocks.isEmpty()) return;
		Collections.shuffle(newBlocks);
		solidify.fillQueue(newBlocks);
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	public static void act(User user) {
		if (user.getSelectedAbilityName().equals("HeatControl")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, HeatControl.class).ifPresent(HeatControl::act);
		}
	}

	public static void onSneak(User user) {
		if (user.getSelectedAbilityName().equals("HeatControl")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, HeatControl.class).ifPresent(HeatControl::onSneak);
		}
	}

	public static boolean canBurn(User user) {
		return !user.getSelectedAbility()
			.filter(desc -> desc.getName().equals("HeatControl") && user.canBend(desc))
			.isPresent();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class Solidify extends PhaseTransformer {
		@Override
		protected boolean processBlock(@NonNull Block block) {
			if (MaterialUtil.isLava(block) && TempBlock.isBendable(block)) {
				return BlockMethods.tryCoolLava(user, block);
			}
			return false;
		}
	}

	private class Melt extends PhaseTransformer {
		@Override
		protected boolean processBlock(@NonNull Block block) {
			if (!TempBlock.isBendable(block)) return false;
			return BlockMethods.tryMeltSnow(user, block) || BlockMethods.tryMeltIce(user, block);
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.RADIUS)
		public double radius;
		@Attribute(Attribute.RANGE)
		public double solidifyRange;
		@Attribute(Attribute.RADIUS)
		public double solidifyRadius;
		@Attribute(Attribute.CHARGE_TIME)
		public long cookInterval;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "heatcontrol");

			cooldown = abilityNode.node("cooldown").getLong(2000);
			range = abilityNode.node("range").getDouble(10.0);
			radius = abilityNode.node("radius").getDouble(5.0);
			solidifyRange = abilityNode.node("solidify-range").getDouble(5.0);
			solidifyRadius = abilityNode.node("solidify-radius").getDouble(6.0);
			cookInterval = abilityNode.node("cook-interval").getLong(2000);
		}
	}
}

