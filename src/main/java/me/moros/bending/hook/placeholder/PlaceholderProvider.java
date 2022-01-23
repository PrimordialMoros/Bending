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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlaceholderProvider {
  private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer
    .legacyAmpersand().toBuilder().hexColors().build();

  private final Map<String, Placeholder> placeholders;

  PlaceholderProvider() {
    this.placeholders = setup();
  }

  private static Map<String, Placeholder> setup() {
    Builder builder = new Builder();
    builder.addStatic("elements", player -> player.elements().stream().map(Element::displayName)
      .map(e -> toLegacy(player, e)).collect(Collectors.joining(", ")));
    builder.addStatic("element", PlaceholderProvider::findElement);
    builder.addStatic("element_color", player ->
      player.elements().stream().findFirst().map(Element::color).map(TextColor::asHexString).orElse("#ffffff")
    );
    builder.addStatic("selected_ability", player -> {
      AbilityDescription desc = player.selectedAbility();
      return desc == null ? "" : toLegacy(player, desc.displayName());
    });
    builder.addDynamic("has_element", (player, elementName) ->
      formatBoolean(Element.fromName(elementName).map(player::hasElement).orElse(false))
    );
    builder.addDynamic("can_bend", (player, abilityName) -> {
      AbilityDescription desc = Registries.ABILITIES.ability(abilityName);
      boolean result = (desc != null && player.canBend(desc));
      return formatBoolean(result);
    });
    return builder.build();
  }

  private static @NonNull String findElement(BendingPlayer player) {
    Collection<Element> userElements = player.elements();
    if (userElements.isEmpty()) {
      return org.bukkit.ChatColor.RESET + "NonBender";
    } else if (userElements.size() > 1) {
      return org.bukkit.ChatColor.DARK_PURPLE + "Avatar";
    } else {
      return toLegacy(player, userElements.iterator().next().displayName());
    }
  }

  private static String formatBoolean(boolean value) {
    return value ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
  }

  private static String toLegacy(BendingPlayer player, Component component) {
    return SERIALIZER.serialize(GlobalTranslator.render(component, player.entity().locale()));
  }

  public @Nullable String onPlaceholderRequest(@NonNull BendingPlayer player, @NonNull String placeholder) {
    for (var entry : placeholders.entrySet()) {
      String id = entry.getKey();
      Placeholder p = entry.getValue();
      if (p instanceof DynamicPlaceholder dp && placeholder.startsWith(id) && placeholder.length() > id.length()) {
        return dp.handle(player, placeholder.substring(id.length()));
      } else if (p instanceof StaticPlaceholder sp && placeholder.equals(id)) {
        return sp.handle(player);
      }
    }
    return null;
  }

  private static final class Builder {
    private final Map<String, Placeholder> placeholders;

    private Builder() {
      placeholders = new LinkedHashMap<>();
    }

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
    @NonNull String handle(BendingPlayer player);
  }

  @FunctionalInterface
  private interface DynamicPlaceholder extends Placeholder {
    @NonNull String handle(BendingPlayer player, String argument);
  }
}
