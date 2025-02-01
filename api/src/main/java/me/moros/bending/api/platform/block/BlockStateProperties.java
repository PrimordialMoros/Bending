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

package me.moros.bending.api.platform.block;

import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.property.IntegerProperty;

import static me.moros.bending.api.platform.property.Property.boolProp;
import static me.moros.bending.api.platform.property.Property.intProp;

public final class BlockStateProperties {
  private BlockStateProperties() {
  }

  public static final BooleanProperty DRAG = boolProp("drag");
  public static final BooleanProperty LIT = boolProp("lit");
  public static final BooleanProperty OPEN = boolProp("open");
  public static final BooleanProperty WATERLOGGED = boolProp("waterlogged");

  public static final IntegerProperty LAYERS = intProp("layers", 1, 8);
  public static final IntegerProperty LEVEL = intProp("level", 0, 15);
}
