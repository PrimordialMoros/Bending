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

package me.moros.bending.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.moros.bending.Bending;
import me.moros.bending.registry.Registries;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BendingExpansion extends PlaceholderExpansion {
  private final PlaceholderProvider provider;

  public BendingExpansion() {
    this.provider = new PlaceholderProvider();
  }

  @Override
  public @NotNull String getAuthor() {
    return Bending.author();
  }

  @Override
  public @NotNull String getIdentifier() {
    return "bending";
  }

  @Override
  public @NotNull String getVersion() {
    return Bending.version();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public String onPlaceholderRequest(Player player, @NotNull String params) {
    if (player == null || !Registries.BENDERS.contains(player.getUniqueId())) {
      return "";
    }
    return provider.onPlaceholderRequest(Registries.BENDERS.user(player), params);
  }
}
