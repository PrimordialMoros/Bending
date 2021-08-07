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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlaceholderProvider {
  private final Map<String, Placeholder> placeholders;

  PlaceholderProvider() {
    PlaceholderBuilder builder = new PlaceholderBuilder();
    setup(builder);
    this.placeholders = builder.build();
  }

  private void setup(PlaceholderBuilder builder) {
    builder.addStatic("elements", (player, user) -> user.elements().stream().map(Element::displayName)
      .map(this::toLegacy).collect(Collectors.joining(", ")));
    builder.addStatic("element", (player, user) -> user.elements().stream().findFirst().map(Element::displayName)
      .map(this::toLegacy).orElse(""));
    builder.addStatic("element_color", (player, user) -> {
      TextColor color = user.elements().stream().findFirst().map(Element::color).orElse(null);
      return color == null ? "" : ChatColor.of(color.asHexString()).toString();
    });
    builder.addStatic("selected_ability", (player, user) -> {
      AbilityDescription desc = user.selectedAbility();
      return desc == null ? "" : toLegacy(desc.displayName());
    });
    builder.addDynamic("has_element", (player, user, elementName) ->
      formatBoolean(Element.fromName(elementName).map(user::hasElement).orElse(false))
    );
    builder.addDynamic("can_bend", (player, user, abilityName) -> {
      AbilityDescription desc = Registries.ABILITIES.ability(abilityName);
      boolean result = (desc != null && user.canBend(desc));
      return formatBoolean(result);
    });
  }

  private String formatBoolean(boolean value) {
    return value ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
  }

  private String toLegacy(Component component) {
    return LegacyComponentSerializer.legacySection().serialize(component);
  }

  public @Nullable String onPlaceholderRequest(@NonNull Player player, @NonNull String placeholder) {
    User user = Registries.BENDERS.user(player);
    for (var entry : placeholders.entrySet()) {
      String id = entry.getKey();
      Placeholder p = entry.getValue();
      if (p instanceof DynamicPlaceholder dp && placeholder.startsWith(id) && placeholder.length() > id.length()) {
        return dp.handle(player, user, placeholder.substring(id.length()));
      } else if (p instanceof StaticPlaceholder sp && placeholder.equals(id)) {
        return sp.handle(player, user);
      }
    }
    return null;
  }

  private static final class PlaceholderBuilder {
    private final Map<String, Placeholder> placeholders = new LinkedHashMap<>();

    public void addStatic(String id, StaticPlaceholder placeholder) {
      this.placeholders.put(id, placeholder);
    }

    public void addDynamic(String id, DynamicPlaceholder placeholder) {
      this.placeholders.put(id + "_", placeholder);
    }

    public Map<String, Placeholder> build() {
      return Map.copyOf(placeholders);
    }
  }

  private interface Placeholder {
  }

  @FunctionalInterface
  private interface StaticPlaceholder extends Placeholder {
    @NonNull String handle(Player player, User user);
  }

  @FunctionalInterface
  private interface DynamicPlaceholder extends Placeholder {
    @NonNull String handle(Player player, User user, String argument);
  }
}
