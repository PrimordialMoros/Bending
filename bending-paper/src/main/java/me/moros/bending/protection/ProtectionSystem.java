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
import me.moros.bending.model.exception.PluginNotFoundException;
import me.moros.bending.model.user.User;
import me.moros.bending.protection.instances.GriefPreventionProtection;
import me.moros.bending.protection.instances.Protection;
import me.moros.bending.protection.instances.TownyProtection;
import me.moros.bending.protection.instances.WorldGuardProtection;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the protection system which hooks into other region protection plugins.
 */
public class ProtectionSystem extends Configurable {
	private final List<Protection> protections = new ArrayList<>();
	private final ProtectionCache cache = new ProtectionCache();
	private boolean allowHarmless;

	public ProtectionSystem() {
		onConfigReload();
		registerProtectMethod("WorldGuard", WorldGuardProtection::new);
		registerProtectMethod("GriefPrevention", GriefPreventionProtection::new);
		registerProtectMethod("Towny", TownyProtection::new);
	}

	@Override
	public void onConfigReload() {
		CommentedConfigurationNode node = ConfigManager.getConfig().getNode("protection");
		allowHarmless = node.getNode("allow-harmless").getBoolean(true);
	}

	/**
	 * @see ProtectionCache#invalidate(User)
	 */
	public void invalidate(User user) {
		cache.invalidate(user);
	}

	/**
	 * @see #canBuild(User, Block, boolean)
	 */
	public boolean canBuild(User user, Block block) {
		return canBuild(user, block, false);
	}

	/**
	 * Uses {@link AbilityDescription#isHarmless}
	 * @see #canBuild(User, Block, boolean)
	 */
	public boolean canBuild(User user, Block block, AbilityDescription desc) {
		return canBuild(user, block, desc != null && desc.isHarmless());
	}


	/**
	 * Checks if a user can build at a block location. First it queries the cache.
	 * If no result is found it computes it and adds it to the cache before returning the result.
	 * Harmless actions are automatically allowed if allowHarmless is configured
	 * @param user the user to check
	 * @param block the block to check
	 * @param isHarmless whether the action the user is peforming is harmless
	 * @return the result.
	 * @see #canBuildPostCache(User, Block)
	 */
	public boolean canBuild(User user, Block block, boolean isHarmless) {
		if (isHarmless && allowHarmless) return true;
		Optional<Boolean> result = cache.canBuild(user, block);
		if (result.isPresent()) return result.get();
		boolean allowed = canBuildPostCache(user, block);
		cache.store(user, block, allowed);
		return allowed;
	}

	/**
	 * Checks if a user can build at a block location.
	 * @param user the user to check
	 * @param block the block to check
	 * @return true if all enabled protections allow it, false otherwise
	 */
	private boolean canBuildPostCache(User user, Block block) {
		return protections.stream().allMatch(m -> m.canBuild(user, block));
	}

	/**
	 * Register a new {@link Protection}
	 * @param name the name of the protection to register
	 * @param creator the factory function that creates the protection instance
	 */
	public void registerProtectMethod(String name, ProtectionFactory creator) {
		CommentedConfigurationNode node = ConfigManager.getConfig().getNode("protection", name);
		if (!node.getBoolean(true)) return;
		try {
			Protection method = creator.create();
			protections.add(method);
			Bending.getLog().info("Registered bending protection for " + name);
		} catch (PluginNotFoundException e) {
			Bending.getLog().warning("ProtectMethod " + name + " not able to be used since plugin was not found.");
		}
	}

	@FunctionalInterface
	public interface ProtectionFactory {
		Protection create() throws PluginNotFoundException;
	}
}
