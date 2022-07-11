/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.hook.placeholder;

import java.util.Locale;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.moros.bending.Bending;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BendingExpansion extends PlaceholderExpansion {
  private final Bending plugin;
  private final PlaceholderProvider provider;
  private final LegacyComponentSerializer serializer;

  public BendingExpansion(Bending plugin) {
    this.plugin = plugin;
    this.provider = new PlaceholderProvider();
    this.serializer = LegacyComponentSerializer.legacyAmpersand().toBuilder().hexColors().build();
  }

  @Override
  public @NonNull String getAuthor() {
    return plugin.author();
  }

  @Override
  public @NonNull String getIdentifier() {
    return "bending";
  }

  @Override
  public @NonNull String getVersion() {
    return plugin.version();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @Nullable String onPlaceholderRequest(@Nullable Player player, String params) {
    User user = player == null ? null : Registries.BENDERS.get(player.getUniqueId());
    if (user instanceof BendingPlayer bendingPlayer) {
      return translate(provider.onPlaceholderRequest(bendingPlayer, params), player.locale());
    }
    return null;
  }

  private @Nullable String translate(@Nullable Component component, Locale locale) {
    return component == null ? null : serializer.serialize(GlobalTranslator.render(component, locale));
  }
}
