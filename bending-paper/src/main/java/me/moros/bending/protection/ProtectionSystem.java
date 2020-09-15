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

package me.moros.bending.protection;

import me.moros.bending.Bending;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.model.exception.PluginNotFoundException;
import me.moros.bending.protection.methods.GriefPreventionProtectMethod;
import me.moros.bending.protection.methods.ProtectMethod;
import me.moros.bending.protection.methods.TownyProtectMethod;
import me.moros.bending.protection.methods.WorldGuardProtectMethod;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProtectionSystem extends Configurable {
	private final List<ProtectMethod> protectionMethods = new ArrayList<>();

	private boolean allowHarmless;
	private final ProtectionCache cache = new ProtectionCache();

	public ProtectionSystem() {
		onConfigReload();
		registerProtectMethod("GriefPrevention", GriefPreventionProtectMethod::new);
		registerProtectMethod("Towny", TownyProtectMethod::new);
		registerProtectMethod("WorldGuard", WorldGuardProtectMethod::new);
	}

	@Override
	public void onConfigReload() {
		CommentedConfigurationNode node = ConfigManager.getConfig().getNode("protection");
		allowHarmless = node.getNode("allow-harmless").getBoolean(true);
	}

	public void invalidate(User user) {
		cache.invalidate(user);
	}

	public boolean canBuild(User user, Block block) {
		return canBuild(user, null, block);
	}

	public boolean canBuild(User user, AbilityDescription desc, Block block) {
		boolean isHarmless = false;
		if (desc != null) isHarmless = desc.isHarmless();
		if (isHarmless && allowHarmless) return true;
		Optional<Boolean> result = cache.canBuild(user, block);
		if (result.isPresent()) {
			return result.get();
		}

		boolean allowed = canBuildPostCache(user, desc, block);
		cache.store(user, block, allowed);
		return allowed;
	}

	private boolean canBuildPostCache(User user, AbilityDescription desc, Block block) {
		return protectionMethods.stream().allMatch(m -> m.canBuild(user, desc, block));
	}

	public void registerProtectMethod(String name, MethodCreator creator) {
		CommentedConfigurationNode node = ConfigManager.getConfig().getNode("protection", name);
		if (!node.getBoolean(true)) return;
		try {
			ProtectMethod method = creator.create();
			protectionMethods.add(method);
			Bending.getLog().warning("Registered bending protection for " + name);
		} catch (PluginNotFoundException e) {
			Bending.getLog().warning("ProtectMethod " + name + " not able to be used since plugin was not found.");
		}
	}

	@FunctionalInterface
	public interface MethodCreator {
		ProtectMethod create() throws PluginNotFoundException;
	}
}
