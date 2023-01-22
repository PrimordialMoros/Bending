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

package me.moros.bending.platform;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;

final class BooleanPDT implements PersistentDataType<Byte, Boolean> {
  static final PersistentDataType<Byte, Boolean> INSTANCE = new BooleanPDT();

  private static final byte FALSE = 0;
  private static final byte TRUE = 1;

  private BooleanPDT() {
  }

  @Override
  public @NonNull Class<Byte> getPrimitiveType() {
    return Byte.class;
  }

  @Override
  public @NonNull Class<Boolean> getComplexType() {
    return Boolean.class;
  }

  @Override
  public @NonNull Byte toPrimitive(@NonNull Boolean complex, @NonNull PersistentDataAdapterContext context) {
    return complex ? TRUE : FALSE;
  }

  @Override
  public @NonNull Boolean fromPrimitive(@NonNull Byte primitive, @NonNull PersistentDataAdapterContext context) {
    return primitive == TRUE;
  }
}
