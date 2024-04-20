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

package me.moros.bending.common.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.bending.common.locale.Message;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public final class CommandUtil {
  private CommandUtil() {
  }

  public static String mapToSuggestion(AbilityDescription desc) {
    Key key = desc.key();
    return key.namespace().equals(KeyUtil.BENDING_NAMESPACE) ? key.value() : key.asString();
  }

  public static List<Component> presetSlots(Preset preset) {
    List<Component> lines = new ArrayList<>();
    preset.forEach((desc, idx) ->
      lines.add(Component.text((idx + 1) + ". ", ColorPalette.TEXT_COLOR).append(mapToClickComponent(desc)))
    );
    return lines;
  }

  public static Component presetDescription(Preset preset) {
    Component details = Component.text().append(Component.join(JoinConfiguration.newlines(), presetSlots(preset)))
      .append(Component.newline()).append(Component.newline())
      .append(Message.HOVER_PRESET.build()).build();
    return preset.displayName()
      .hoverEvent(HoverEvent.showText(details))
      .clickEvent(ClickEvent.runCommand("/bending preset bind " + preset.name()));
  }

  public static AbilityDisplay collectAll(Predicate<AbilityDescription> permissionChecker, Element element) {
    ElementSet singleElementSet = ElementSet.of(element);
    var all = List.of(
      collectAbilities(permissionChecker, singleElementSet),
      collectSequences(permissionChecker, singleElementSet),
      collectPassives(permissionChecker, singleElementSet)
    );
    return new AbilityDisplay(all);
  }

  private static AbilityDisplay collectAbilities(Predicate<AbilityDescription> permissionChecker, ElementSet singleElementSet) {
    var components = Registries.ABILITIES.stream()
      .filter(desc -> !desc.hidden() && desc.canBind())
      .filter(desc -> singleElementSet.equals(desc.elements()))
      .filter(permissionChecker)
      .sorted(Comparator.comparing(Keyed::key))
      .map(CommandUtil::mapToClickComponent)
      .toList();
    return new AbilityDisplay(Message.ABILITIES.build(), components);
  }

  private static AbilityDisplay collectSequences(Predicate<AbilityDescription> permissionChecker, ElementSet singleElementSet) {
    var components = Registries.SEQUENCES.stream()
      .filter(desc -> !desc.hidden())
      .filter(desc -> singleElementSet.equals(desc.elements()))
      .filter(permissionChecker)
      .sorted(Comparator.comparing(Keyed::key))
      .map(CommandUtil::mapToClickComponent)
      .toList();
    return new AbilityDisplay(Message.SEQUENCES.build(), components);
  }

  private static AbilityDisplay collectPassives(Predicate<AbilityDescription> permissionChecker, ElementSet singleElementSet) {
    var components = Registries.ABILITIES.stream()
      .filter(desc -> !desc.hidden() && !desc.canBind())
      .filter(desc -> singleElementSet.equals(desc.elements()) && desc.isActivatedBy(Activation.PASSIVE))
      .filter(permissionChecker)
      .sorted(Comparator.comparing(Keyed::key))
      .map(CommandUtil::mapToClickComponent)
      .toList();
    return new AbilityDisplay(Message.PASSIVES.build(), components);
  }

  private static Component mapToClickComponent(AbilityDescription desc) {
    return desc.displayName().clickEvent(ClickEvent.runCommand("/bending help " + desc.key().asString()));
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
