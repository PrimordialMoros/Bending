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

import cloud.commandframework.permission.CommandPermission;
import cloud.commandframework.permission.Permission;

public final class CommandPermissions {
  private CommandPermissions() {
  }

  public static final CommandPermission HELP = create("help");
  public static final CommandPermission TOGGLE = create("toggle");
  public static final CommandPermission BOARD = create("board");
  public static final CommandPermission CHOOSE = create("choose");
  public static final CommandPermission ADD = create("add");
  public static final CommandPermission REMOVE = create("remove");
  public static final CommandPermission BIND = create("bind");
  public static final CommandPermission MODIFY = create("modify");
  public static final CommandPermission PRESET = create("preset");
  public static final CommandPermission VERSION = create("version");
  public static final CommandPermission RELOAD = create("reload");
  public static final CommandPermission IMPORT = create("import");
  public static final CommandPermission EXPORT = create("export");
  public static final CommandPermission ATTRIBUTE = create("attribute");

  private static Permission create(String node) {
    return Permission.of("bending.command." + node);
  }
}
