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
import java.util.function.Function;

import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.description.AbilityDescription.Sequence;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.ColorPalette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlaceholderProvider {
  private final Map<String, Placeholder> placeholders;

  PlaceholderProvider() {
    this.placeholders = setup();
  }

  private Map<String, Placeholder> setup() {
    return new Builder()
      .addStatic("elements", this::userElements)
      .addStatic("element", this::findElement)
      .addStatic("display_name", this::displayName)
      .addStatic("selected_ability", this::selectedAbility)
      .addDynamic("ability_info", this::abilityInfo)
      .build();
  }

  private Component userElements(BendingPlayer player) {
    JoinConfiguration sep = JoinConfiguration.commas(true);
    Collection<Component> elements = player.elements().stream().map(Element::displayName).toList();
    return Component.join(sep, elements).colorIfAbsent(ColorPalette.TEXT_COLOR);
  }

  private Component findElement(BendingPlayer player) {
    Component empty = Component.text("NonBender");
    Component avatar = Component.text("Avatar");
    return withElementColor(player, Element::displayName, empty, avatar);
  }

  private Component displayName(BendingPlayer player) {
    Component name = player.entity().displayName();
    return withElementColor(player, e -> name.colorIfAbsent(e.color()), name, name);
  }

  private Component withElementColor(BendingPlayer player, Function<Element, Component> function, Component nonBender, Component avatar) {
    Collection<Element> userElements = player.elements();
    if (userElements.isEmpty()) {
      return nonBender;
    } else if (userElements.size() > 1) {
      return avatar.colorIfAbsent(ColorPalette.AVATAR);
    } else {
      return function.apply(userElements.iterator().next());
    }
  }

  private @Nullable Component selectedAbility(BendingPlayer player) {
    AbilityDescription desc = player.selectedAbility();
    return desc == null ? null : desc.displayName();
  }

  private @Nullable Component abilityInfo(BendingPlayer player, String abilityName) {
    AbilityDescription desc = Registries.ABILITIES.fromString(abilityName);
    if (desc == null) {
      return null;
    }
    Component description = Component.translatable(desc.key() + ".description");
    Component instructions;
    if (desc instanceof Sequence sequence) {
      instructions = sequence.instructions();
    } else {
      instructions = Component.translatable(desc.key() + ".instructions");
    }
    return Component.join(JoinConfiguration.newlines(), description, instructions);
  }

  public @Nullable Component onPlaceholderRequest(BendingPlayer player, String placeholder) {
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

    public Builder addStatic(String id, StaticPlaceholder placeholder) {
      this.placeholders.put(id, placeholder);
      return this;
    }

    public Builder addDynamic(String id, DynamicPlaceholder placeholder) {
      this.placeholders.put(id + "_", placeholder);
      return this;
    }

    public Map<String, Placeholder> build() {
      return Map.copyOf(placeholders);
    }
  }

  private interface Placeholder {
  }

  @FunctionalInterface
  private interface StaticPlaceholder extends Placeholder {
    @Nullable Component handle(BendingPlayer player);
  }

  @FunctionalInterface
  private interface DynamicPlaceholder extends Placeholder {
    @Nullable Component handle(BendingPlayer player, String argument);
  }
}
