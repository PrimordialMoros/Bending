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

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.exception.PluginNotFoundException;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.player.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class TownyProtectMethod implements ProtectMethod {
	private final Towny towny;

	public TownyProtectMethod() throws PluginNotFoundException {
		towny = (Towny) Bukkit.getPluginManager().getPlugin("Towny");

		if (towny == null)
			throw new PluginNotFoundException("Towny");
	}

	@Override
	public boolean canBuild(User user, AbilityDescription desc, Block block) {
		if (user instanceof BendingPlayer) {
			Player player = ((BendingPlayer) user).getEntity();
			return canPlayerBuild(player, block);
		}
		try {
			if (TownyAPI.getInstance().getTownBlock(block.getLocation()).getTown() != null) return false;
		} catch (Exception e) {
			// Do nothing
		}
		return true;
	}

	private boolean canPlayerBuild(Player player, Block block) {
		boolean canBuild = PlayerCacheUtil.getCachePermission(player, block.getLocation(), Material.STONE, TownyPermission.ActionType.BUILD);
		if (!canBuild && TownyAPI.getInstance().isWarTime()) {
			PlayerCache cache = towny.getCache(player);
			PlayerCache.TownBlockStatus status = cache.getStatus();
			if (status == PlayerCache.TownBlockStatus.ENEMY && !WarUtil.isPlayerNeutral(player)) {
				try {
					TownyWorld townyWorld = TownyAPI.getInstance().getDataSource().getWorld(player.getWorld().getName());
					WorldCoord worldCoord = new WorldCoord(townyWorld.getName(), Coord.parseCoord(block));
					FlagWar.callAttackCellEvent(towny, player, block, worldCoord);
				} catch (Exception e) {
					// Do nothing
				}
				canBuild = true;
			} else if (status == PlayerCache.TownBlockStatus.WARZONE) {
				canBuild = true;
			}
		}
		return canBuild;
	}

	@Override
	public String getName() {
		return "Towny";
	}
}
