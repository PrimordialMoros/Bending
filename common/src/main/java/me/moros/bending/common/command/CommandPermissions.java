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


import org.incendo.cloud.permission.Permission;

public final class CommandPermissions {
  private CommandPermissions() {
  }

  public static final Permission HELP = create("help");
  public static final Permission TOGGLE = create("toggle");
  public static final Permission BOARD = create("board");
  public static final Permission CHOOSE = create("choose");
  public static final Permission ADD = create("add");
  public static final Permission REMOVE = create("remove");
  public static final Permission BIND = create("bind");
  public static final Permission MODIFY = create("modify");
  public static final Permission PRESET = create("preset");
  public static final Permission VERSION = create("version");
  public static final Permission RELOAD = create("reload");
  public static final Permission IMPORT = create("import");
  public static final Permission EXPORT = create("export");
  public static final Permission ATTRIBUTE = create("attribute");

  private static Permission create(String node) {
    return Permission.of("bending.command." + node);
  }
}
