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
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.PotionUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;

import java.util.Optional;

public class EarthArmor extends AbilityInstance implements Ability {
	private enum Mode {ROCK, IRON, GOLD}

	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Mode mode;
	private BendingFallingBlock fallingBlock;

	private boolean formed = false;
	private int resistance;

	public EarthArmor(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, EarthArmor.class)) return false;

		this.user = user;
		recalculateConfig();

		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, b -> EarthMaterials.isEarthNotLava(user, b));

		if (!source.isPresent()) return false;

		Block block = source.get();
		mode = getType(block);
		if (EarthMaterials.METAL_BENDABLE.isTagged(block)) {
			resistance = userConfig.metalPower;
			SoundUtil.METAL_SOUND.play(block.getLocation());
		} else {
			resistance = userConfig.power;
			SoundUtil.EARTH_SOUND.play(block.getLocation());
		}
		BlockState state = block.getState();
		TempBlock.createAir(block, BendingProperties.EARTHBENDING_REVERT_TIME);
		fallingBlock = new BendingFallingBlock(block, state.getBlockData(), new Vector3(0, 0.2, 0), false, 10000);
		removalPolicy = Policies.builder().add(new ExpireRemovalPolicy(5000)).build();
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

		if (formed) {
			user.getEntity().setFireTicks(0);
			return UpdateResult.CONTINUE;
		}

		return moveBlock() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private Mode getType(Block block) {
		switch (block.getType()) {
			case IRON_BLOCK:
				return Mode.IRON;
			case GOLD_BLOCK:
				return Mode.GOLD;
			default:
				return Mode.ROCK;
		}
	}

	private void formArmor() {
		if (formed) return;
		ItemStack head, chest, leggings, boots;
		switch (mode) {
			case IRON:
				head = new ItemStack(Material.IRON_HELMET, 1);
				chest = new ItemStack(Material.IRON_CHESTPLATE, 1);
				leggings = new ItemStack(Material.IRON_LEGGINGS, 1);
				boots = new ItemStack(Material.IRON_BOOTS, 1);
				break;
			case GOLD:
				head = new ItemStack(Material.GOLDEN_HELMET, 1);
				chest = new ItemStack(Material.GOLDEN_CHESTPLATE, 1);
				leggings = new ItemStack(Material.GOLDEN_LEGGINGS, 1);
				boots = new ItemStack(Material.GOLDEN_BOOTS, 1);
				break;
			case ROCK:
			default:
				head = new ItemStack(Material.LEATHER_HELMET, 1);
				chest = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
				leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1);
				boots = new ItemStack(Material.LEATHER_BOOTS, 1);
				break;
		}

		TempArmor.create(user, new ItemStack[]{boots, leggings, chest, head}, userConfig.duration);
		int duration = NumberConversions.round(userConfig.duration / 50F);
		PotionUtil.addPotion(user.getEntity(), PotionEffectType.DAMAGE_RESISTANCE, duration, resistance);
		removalPolicy = Policies.builder().add(new ExpireRemovalPolicy(userConfig.duration)).build();
		user.setCooldown(getDescription(), userConfig.cooldown);
		formed = true;
	}

	private boolean moveBlock() {
		if (!fallingBlock.getFallingBlock().isValid()) return false;
		Vector3 center = fallingBlock.getCenter();

		Block currentBlock = center.toBlock(user.getWorld());
		BlockMethods.breakPlant(currentBlock);
		if (!(currentBlock.isLiquid() || MaterialUtil.isAir(currentBlock) || EarthMaterials.isEarthbendable(user, currentBlock))) {
			return false;
		}

		final double distanceSquared = user.getEyeLocation().distanceSq(center);
		final double speedFactor = (distanceSquared > userConfig.selectRange * userConfig.selectRange) ? 1.5 : 0.8;
		if (distanceSquared < 0.5) {
			fallingBlock.revert();
			formArmor();
			return true;
		}

		Vector3 dir = user.getEyeLocation().subtract(center).normalize().scalarMultiply(speedFactor);
		fallingBlock.getFallingBlock().setVelocity(dir.clampVelocity());
		return true;
	}

	@Override
	public void onDestroy() {
		Location center;
		if (!formed && fallingBlock != null) {
			center = fallingBlock.getCenter().toLocation(user.getWorld());
			fallingBlock.revert();
		} else {
			center = user.getEntity().getEyeLocation();
		}
		SoundUtil.playSound(center, Sound.BLOCK_STONE_BREAK, 2, 1);
		ParticleUtil.create(Particle.BLOCK_CRACK, center)
			.count(8).offset(0.5, 0.5, 0.5)
			.data(fallingBlock.getFallingBlock().getBlockData()).spawn();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		@Attribute(Attribute.STRENGTH)
		public int power;
		@Attribute(Attribute.STRENGTH)
		public int metalPower;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "eartharmor");

			cooldown = abilityNode.node("cooldown").getLong(20000);
			duration = abilityNode.node("duration").getLong(12000);
			selectRange = abilityNode.node("select-range").getDouble(8.0);
			power = abilityNode.node("power").getInt(2) - 1;
			metalPower = abilityNode.node("metal-power").getInt(3) - 1;
		}
	}
}
