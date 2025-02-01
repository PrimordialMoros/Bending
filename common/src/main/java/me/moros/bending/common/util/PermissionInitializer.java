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

package me.moros.bending.common.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.util.FeaturePermissions;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.bending.common.command.Permissions;
import net.kyori.adventure.util.TriState;

public abstract class PermissionInitializer implements Initializer {
  protected PermissionInitializer() {
  }

  @Override
  public void init() {
    initPlayerNodes();
    initAdminNodes();
    initMisc();
  }

  private void initPlayerNodes() {
    var elementNodes = initAbilityNodes();
    var children = Stream.of(Permissions.BIND, Permissions.BOARD, Permissions.CHOOSE,
        Permissions.HELP, Permissions.PRESET, Permissions.TOGGLE, Permissions.VERSION)
      .collect(Collectors.toSet());
    children.addAll(elementNodes);
    children.add(FeaturePermissions.BOARD);
    children.add("bending.ability.avatarstate");
    registerDefault("bending.player", children);
  }

  private void initAdminNodes() {
    var children = Stream.of(Permissions.ADD, Permissions.REMOVE, Permissions.MODIFY,
        Permissions.RELOAD, Permissions.IMPORT, Permissions.EXPORT, Permissions.ATTRIBUTE)
      .collect(Collectors.toSet());
    children.add("bending.player");
    children.add(FeaturePermissions.BLUE_FIRE);
    children.add(Permissions.CHOOSE + ".other");
    children.add(Permissions.ADD + ".other");
    children.add(Permissions.REMOVE + ".other");
    registerDefault("bending.admin", children, TriState.NOT_SET);
  }

  private void initMisc() {
    registerDefault(FeaturePermissions.OVERRIDE_LOCK, List.of(), TriState.FALSE);
  }

  private Collection<String> initAbilityNodes() {
    return List.of(registerAbilityNodes(Element.AIR), registerAbilityNodes(Element.WATER),
      registerAbilityNodes(Element.EARTH), registerAbilityNodes(Element.FIRE));
  }

  private String registerAbilityNodes(Element element) {
    var node = elementParentNode(element);
    var abilityNodes = collect(element);
    abilityNodes.add(Permissions.CHOOSE + "." + element.key().value());
    registerDefault(node, abilityNodes);
    return node;
  }

  private String elementParentNode(Element element) {
    return "bending." + element.key().value();
  }

  private Collection<String> collect(Element element) {
    ElementSet singleElementSet = ElementSet.of(element);
    return Registries.ABILITIES.stream().filter(a -> singleElementSet.equals(a.elements()))
      .map(AbilityDescription::permissions).flatMap(Collection::stream).collect(Collectors.toSet());
  }

  protected void registerDefault(String node, Collection<String> children) {
    registerDefault(node, children, TriState.TRUE);
  }

  protected abstract void registerDefault(String node, Collection<String> children, TriState def);
}
