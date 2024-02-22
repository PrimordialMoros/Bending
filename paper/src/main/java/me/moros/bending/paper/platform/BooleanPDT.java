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

package me.moros.bending.paper.platform;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

final class BooleanPDT implements PersistentDataType<Byte, Boolean> {
  static final PersistentDataType<Byte, Boolean> INSTANCE = new BooleanPDT();

  private static final byte FALSE = 0;
  private static final byte TRUE = 1;

  private BooleanPDT() {
  }

  @Override
  public Class<Byte> getPrimitiveType() {
    return Byte.class;
  }

  @Override
  public Class<Boolean> getComplexType() {
    return Boolean.class;
  }

  @Override
  public Byte toPrimitive(Boolean complex, PersistentDataAdapterContext context) {
    return complex ? TRUE : FALSE;
  }

  @Override
  public Boolean fromPrimitive(Byte primitive, PersistentDataAdapterContext context) {
    return primitive == TRUE;
  }
}
