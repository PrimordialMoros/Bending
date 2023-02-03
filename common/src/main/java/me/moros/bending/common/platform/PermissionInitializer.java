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

package me.moros.bending.common.platform;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cloud.commandframework.permission.CommandPermission;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.command.CommandPermissions;
import net.kyori.adventure.util.TriState;

public abstract class PermissionInitializer implements Initializer {
  protected PermissionInitializer() {
    init();
  }

  @Override
  public void init() {
    initPlayerNodes();
    initAdminNodes();
    initMisc();
  }

  private void initPlayerNodes() {
    var elementNodes = initAbilityNodes();
    var children = Stream.of(CommandPermissions.BIND, CommandPermissions.BOARD, CommandPermissions.CHOOSE,
        CommandPermissions.HELP, CommandPermissions.PRESET, CommandPermissions.TOGGLE, CommandPermissions.VERSION)
      .map(CommandPermission::toString).collect(Collectors.toSet());
    children.addAll(elementNodes);
    children.add("bending.board");
    registerDefault("bending.player", children);
  }

  private void initAdminNodes() {
    var children = Stream.of(CommandPermissions.ADD, CommandPermissions.MODIFY, CommandPermissions.RELOAD,
      CommandPermissions.REMOVE).map(CommandPermission::toString).collect(Collectors.toSet());
    children.add("bending.player");
    children.add("bending.bluefire");
    registerDefault("bending.admin", children, TriState.NOT_SET);
  }

  private void initMisc() {
    registerDefault("bending.admin.overridelock", List.of(), TriState.FALSE);
  }

  private Collection<String> initAbilityNodes() {
    return List.of(registerAbilityNodes(Element.AIR), registerAbilityNodes(Element.WATER),
      registerAbilityNodes(Element.EARTH), registerAbilityNodes(Element.FIRE));
  }

  private String registerAbilityNodes(Element element) {
    var node = elementParentNode(element);
    var abilityNodes = collect(element);
    abilityNodes.add(CommandPermissions.CHOOSE + "." + element.key().value());
    registerDefault(node, abilityNodes);
    return node;
  }

  private String elementParentNode(Element element) {
    return "bending." + element.key().value();
  }

  private Collection<String> collect(Element element) {
    return Registries.ABILITIES.stream().filter(a -> a.element() == element)
      .map(AbilityDescription::permissions).flatMap(Collection::stream).collect(Collectors.toSet());
  }

  protected void registerDefault(String node, Collection<String> children) {
    registerDefault(node, children, TriState.TRUE);
  }

  protected abstract void registerDefault(String node, Collection<String> children, TriState def);
}
