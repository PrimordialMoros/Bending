/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.placeholder;

import java.util.Collection;
import java.util.function.Function;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityDescription.Sequence;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.KeyedValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

interface Placeholders {
  KeyedValue<StaticPlaceholder> ELEMENTS = create("elements", Placeholder.of(Placeholders::userElements));

  KeyedValue<StaticPlaceholder> ELEMENT = create("element", Placeholder.of(Placeholders::findElement));

  KeyedValue<StaticPlaceholder> DISPLAY_NAME = create("display_name", Placeholder.of(Placeholders::displayName));

  KeyedValue<StaticPlaceholder> SELECTED_ABILITY = create("selected_ability", Placeholder.of(Placeholders::selectedAbility));

  KeyedValue<DynamicPlaceholder> ABILITY_INFO = create("ability_info", Placeholder.of(Placeholders::abilityInfo));

  private static <T extends Placeholder> KeyedValue<T> create(String id, T placeholder) {
    return KeyedValue.keyedValue(KeyUtil.simple(id), placeholder);
  }

  private static Component userElements(User user) {
    JoinConfiguration sep = JoinConfiguration.commas(true);
    Collection<Component> elements = user.elements().stream().map(Element::displayName).toList();
    return Component.join(sep, elements).colorIfAbsent(ColorPalette.TEXT_COLOR);
  }

  private static Component findElement(User user) {
    Component empty = Component.text("NonBender");
    Component avatar = Component.text("Avatar");
    return withElementColor(user, Element::displayName, empty, avatar);
  }

  private static Component displayName(User user) {
    Component name = user.pointers().getOrDefaultFrom(Identity.DISPLAY_NAME, user::name);
    return withElementColor(user, e -> name.colorIfAbsent(e.color()), name, name);
  }

  private static Component withElementColor(User user, Function<Element, Component> function, Component nonBender, Component avatar) {
    Collection<Element> userElements = user.elements();
    if (userElements.isEmpty()) {
      return nonBender;
    } else if (userElements.size() > 1) {
      return avatar.colorIfAbsent(ColorPalette.AVATAR);
    } else {
      return function.apply(userElements.iterator().next());
    }
  }

  private static Component selectedAbility(User user) {
    AbilityDescription desc = user.selectedAbility();
    return desc == null ? Component.empty() : desc.displayName();
  }

  private static Component abilityInfo(User user, String abilityName) {
    AbilityDescription desc = Registries.ABILITIES.fromString(abilityName);
    if (desc == null) {
      return Component.empty();
    }
    Component description = Component.translatable(desc.translationKey() + ".description");
    Component instructions;
    if (desc instanceof Sequence sequence) {
      instructions = sequence.instructions();
    } else {
      instructions = Component.translatable(desc.translationKey() + ".instructions");
    }
    return Component.join(JoinConfiguration.newlines(), description, instructions);
  }
}
