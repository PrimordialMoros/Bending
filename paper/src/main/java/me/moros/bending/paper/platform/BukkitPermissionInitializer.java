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

package me.moros.bending.paper.platform;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.moros.bending.common.platform.PermissionInitializer;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class BukkitPermissionInitializer extends PermissionInitializer {
  public BukkitPermissionInitializer() {
  }

  protected void registerDefault(String node, Collection<String> children, TriState def) {
    var permDef = switch (def) {
      case TRUE -> PermissionDefault.TRUE;
      case NOT_SET -> PermissionDefault.OP;
      case FALSE -> PermissionDefault.FALSE;
    };
    var map = children.stream().collect(Collectors.toMap(Function.identity(), v -> true));
    Bukkit.getPluginManager().addPermission(new Permission(node, permDef, map));
  }
}
