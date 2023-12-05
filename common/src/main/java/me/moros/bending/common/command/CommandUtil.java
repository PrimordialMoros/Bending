/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.common.command;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;

public final class CommandUtil {
  private CommandUtil() {
  }

  public static <C extends Audience> List<String> combinedSuggestions(C sender) {
    return Stream.of(Element.NAMES, abilityCompletions(sender, false, true)).flatMap(Collection::stream).toList();
  }

  public static <C extends Audience> List<String> abilityCompletions(C sender, boolean validOnly, boolean sequenceSuggestions) {
    Predicate<AbilityDescription> predicate = d -> !d.hidden();
    if (!sequenceSuggestions) {
      predicate = predicate.and(AbilityDescription::canBind);
    }
    if (validOnly) {
      User user = sender.get(Identity.UUID).map(Registries.BENDERS::get).orElse(null);
      if (user != null) {
        predicate = predicate.and(user::hasPermission).and(d -> user.hasElement(d.element()));
      }
    }
    return Registries.ABILITIES.stream().filter(predicate).map(AbilityDescription::name).toList();
  }

  public static AbilityDisplay collectAll(Predicate<AbilityDescription> permissionChecker, Element element) {
    var all = List.of(
      collectAbilities(permissionChecker, element),
      collectSequences(permissionChecker, element),
      collectPassives(permissionChecker, element)
    );
    return new AbilityDisplay(all);
  }

  public static AbilityDisplay collectAbilities(Predicate<AbilityDescription> permissionChecker, Element element) {
    var components = Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden() && desc.canBind())
      .filter(permissionChecker)
      .map(AbilityDescription::meta)
      .toList();
    return new AbilityDisplay(Message.ABILITIES.build(), components);
  }

  public static AbilityDisplay collectSequences(Predicate<AbilityDescription> permissionChecker, Element element) {
    var components = Registries.SEQUENCES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(permissionChecker)
      .map(AbilityDescription::meta)
      .toList();
    return new AbilityDisplay(Message.SEQUENCES.build(), components);
  }

  public static AbilityDisplay collectPassives(Predicate<AbilityDescription> permissionChecker, Element element) {
    var components = Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden() && !desc.canBind() && desc.isActivatedBy(Activation.PASSIVE))
      .filter(permissionChecker)
      .map(AbilityDescription::meta)
      .toList();
    return new AbilityDisplay(Message.PASSIVES.build(), components);
  }

  public static final class AbilityDisplay implements Iterable<Component> {
    private final Collection<Component> display;

    private AbilityDisplay(Collection<AbilityDisplay> children) {
      this.display = children.stream().flatMap(AbilityDisplay::components).toList();
    }

    private AbilityDisplay(Component header, Collection<Component> abilities) {
      if (abilities.isEmpty()) {
        display = List.of();
      } else {
        JoinConfiguration sep = JoinConfiguration.commas(true);
        Component component = Component.join(sep, abilities).colorIfAbsent(ColorPalette.TEXT_COLOR);
        display = List.of(header, component.hoverEvent(HoverEvent.showText(Message.ABILITY_HOVER.build())));
      }
    }

    private Stream<Component> components() {
      return display.stream();
    }

    public boolean isEmpty() {
      return display.isEmpty();
    }

    @Override
    public Iterator<Component> iterator() {
      return display.iterator();
    }
  }
}
