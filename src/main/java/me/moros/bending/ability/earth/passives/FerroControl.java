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

package me.moros.bending.ability.earth.passives;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

public class FerroControl extends AbilityInstance implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Minecart controlledEntity;

	private long nextInteractTime;

	public FerroControl(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription()) || !user.canBend(getDescription())) {
			controlledEntity = null;
			return UpdateResult.CONTINUE;
		}

		if (controlledEntity == null || !controlledEntity.isValid()) {
			controlledEntity = user.getTargetEntity(userConfig.entitySelectRange, Minecart.class).orElse(null);
		}

		if (controlledEntity != null) {
			if (!Bending.getGame().getProtectionSystem().canBuild(user, controlledEntity.getLocation().getBlock())) {
				controlledEntity = null;
				return UpdateResult.CONTINUE;
			}
			Vector3 targetLocation = user.getEyeLocation().add(user.getDirection().scalarMultiply(userConfig.entityRange));
			Vector3 entityLocation = new Vector3(controlledEntity.getLocation());
			if (entityLocation.distanceSq(targetLocation) < 1) {
				controlledEntity.setVelocity(new Vector());
			} else {
				Vector3 dir = targetLocation.subtract(entityLocation).normalize().scalarMultiply(userConfig.controlSpeed);
				controlledEntity.setVelocity(dir.clampVelocity());
			}
		}

		return UpdateResult.CONTINUE;
	}

	private void act(Block block) {
		if (!user.canBend(getDescription()) || !user.hasPermission("bending.metal")) return;
		long time = System.currentTimeMillis();
		if (time < nextInteractTime) return;
		nextInteractTime = time + userConfig.cooldown;
		if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) {
			return;
		}
		if (block.getType() == Material.IRON_DOOR || block.getType() == Material.IRON_TRAPDOOR) {
			Openable openable = (Openable) block.getBlockData();
			openable.setOpen(!openable.isOpen());
			block.setBlockData(openable);
			Sound sound;
			if (block.getType() == Material.IRON_DOOR) {
				sound = openable.isOpen() ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
			} else {
				sound = openable.isOpen() ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
			}
			SoundUtil.playSound(block.getLocation(), sound, 0.5F, 0);
		}
	}

	public static void act(User user, Block block) {
		if (block.getType() == Material.IRON_DOOR || block.getType() == Material.IRON_TRAPDOOR) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, FerroControl.class)
				.ifPresent(ability -> ability.act(block));
		}
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.SELECTION)
		public double blockRange;
		@Attribute(Attribute.SELECTION)
		public double entitySelectRange;
		@Attribute(Attribute.RANGE)
		public double entityRange;
		@Attribute(Attribute.SPEED)
		public double controlSpeed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "passives", "ferrocontrol");

			cooldown = abilityNode.node("cooldown").getLong(500);
			blockRange = abilityNode.node("block-range").getDouble(6.0);
			entitySelectRange = abilityNode.node("entity-select-range").getDouble(14.0);
			entityRange = abilityNode.node("entity-control-range").getDouble(8.0);
			controlSpeed = abilityNode.node("entity-control-speed").getDouble(0.8);
		}
	}
}

