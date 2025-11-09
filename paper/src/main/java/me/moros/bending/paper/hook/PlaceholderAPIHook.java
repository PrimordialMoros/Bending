/*
 * Copyright 2020-2025 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.paper.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.Bending;
import me.moros.bending.common.placeholder.PlaceholderProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {
  private final Bending plugin;
  private final PlaceholderProvider provider;
  private final LegacyComponentSerializer serializer;

  public PlaceholderAPIHook(Bending plugin) {
    this.plugin = plugin;
    this.provider = PlaceholderProvider.defaultBuilder().build();
    this.serializer = LegacyComponentSerializer.legacyAmpersand().toBuilder().hexColors().build();
  }

  @Override
  public String getAuthor() {
    return plugin.author();
  }

  @Override
  public String getIdentifier() {
    return "bending";
  }

  @Override
  public String getVersion() {
    return plugin.version();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @Nullable String onPlaceholderRequest(@Nullable Player player, String params) {
    User user = player == null ? null : Registries.BENDERS.get(player.getUniqueId());
    if (user != null) {
      Component result = provider.onPlaceholderRequest(user, params);
      if (result != Component.empty()) {
        return serializer.serialize(GlobalTranslator.render(result, player.locale()));
      }
    }
    return null;
  }
}
