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

package me.moros.bending.protection.methods;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.model.exception.PluginNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WorldGuardProtectMethod implements ProtectMethod {
	private final WorldGuardPlugin worldGuard;
	private final StateFlag bendingFlag;

	public WorldGuardProtectMethod() throws PluginNotFoundException {
		worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");

		if (worldGuard == null)
			throw new PluginNotFoundException("WorldGuard");

		bendingFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("bending");
	}

	@Override
	public boolean canBuild(User user, AbilityDescription desc, Block block) {
		RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

		Location adaptedLocation = BukkitAdapter.adapt(block.getLocation());
		World adaptedWorld = BukkitAdapter.adapt(block.getWorld());

		if (user instanceof BendingPlayer) {
			Player player = ((BendingPlayer) user).getEntity();
			LocalPlayer adaptedPlayer = worldGuard.wrapPlayer(player);
			boolean hasBypass = WorldGuard.getInstance()
				.getPlatform()
				.getSessionManager()
				.hasBypass(adaptedPlayer, adaptedWorld);

			if (hasBypass) return true;

			if (bendingFlag != null) {
				boolean bendingResult = query.testState(adaptedLocation, adaptedPlayer, bendingFlag);
				if (bendingResult) return true;
			}
			return query.testState(adaptedLocation, adaptedPlayer, Flags.BUILD);
		}

		// Query WorldGuard to see if a non-member (entity) can build in a region.
		return query.testState(adaptedLocation, list -> Association.NON_MEMBER, Flags.BUILD);
	}

	@Override
	public String getName() {
		return "WorldGuard";
	}
}
