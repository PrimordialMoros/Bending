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
import me.moros.bending.Bending;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;

public class FerroControl extends AbilityInstance implements PassiveAbility {
	private User user;

	public FerroControl(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		return true;
	}

	@Override
	public void recalculateConfig() {
	}

	@Override
	public @NonNull UpdateResult update() {
		return UpdateResult.CONTINUE;
	}

	public void act() {
		if (!user.canBend(getDescription())) return;
		Block target = WorldMethods.rayTraceBlocks(user.getWorld(), user.getRay(6)).orElse(null);
		if (target == null) return;
		user.setCooldown(getDescription(), 1000);
		if (!Bending.getGame().getProtectionSystem().canBuild(user, target)) {
			return;
		}

		if (target.getType() == Material.IRON_DOOR || target.getType() == Material.IRON_TRAPDOOR) {
			Openable openable = (Openable) target.getBlockData();
			openable.setOpen(!openable.isOpen());
			target.setBlockData(openable);
			Sound sound;
			if (target.getType() == Material.IRON_DOOR) {
				if (openable.isOpen()) {
					sound = Sound.BLOCK_IRON_DOOR_OPEN;
				} else {
					sound = Sound.BLOCK_IRON_DOOR_CLOSE;
				}
			} else {
				if (openable.isOpen()) {
					sound = Sound.BLOCK_IRON_TRAPDOOR_OPEN;
				} else {
					sound = Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
				}
			}

			SoundUtil.playSound(target.getLocation(), sound, 0.5f, 0);
		}
	}

	public static void act(User user) {
		if (user.isSneaking()) return;
		Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, FerroControl.class).ifPresent(FerroControl::act);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}
}

