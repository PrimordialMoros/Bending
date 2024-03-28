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

public final class Permissions {
  private Permissions() {
  }

  public static final String HELP = create("help");
  public static final String TOGGLE = create("toggle");
  public static final String BOARD = create("board");
  public static final String CHOOSE = create("choose");
  public static final String ADD = create("add");
  public static final String REMOVE = create("remove");
  public static final String BIND = create("bind");
  public static final String MODIFY = create("modify");
  public static final String PRESET = create("preset");
  public static final String VERSION = create("version");
  public static final String RELOAD = create("reload");
  public static final String IMPORT = create("import");
  public static final String EXPORT = create("export");
  public static final String ATTRIBUTE = create("attribute");

  private static String create(String node) {
    return "bending.command" + node;
  }
}
