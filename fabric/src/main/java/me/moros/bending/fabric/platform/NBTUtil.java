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

package me.moros.bending.fabric.platform;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import me.moros.bending.api.util.data.DataKey;
import net.minecraft.nbt.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NBTUtil {
  private NBTUtil() {
  }

  private static final Map<Class<?>, Adapter<?>> ADAPTERS;

  static {
    ADAPTERS = Map.ofEntries(
      entry(String.class, CompoundTag::getString, CompoundTag::putString),
      entry(UUID.class, CompoundTag::getUUID, CompoundTag::putUUID),
      entry(Boolean.class, CompoundTag::getBoolean, CompoundTag::putBoolean),
      entry(Short.class, CompoundTag::getShort, CompoundTag::putShort),
      entry(Float.class, CompoundTag::getFloat, CompoundTag::putFloat),
      entry(Double.class, CompoundTag::getDouble, CompoundTag::putDouble),
      entry(Byte.class, CompoundTag::getByte, CompoundTag::putByte),
      entry(byte[].class, CompoundTag::getByteArray, CompoundTag::putByteArray),
      entry(Integer.class, CompoundTag::getInt, CompoundTag::putInt),
      entry(int[].class, CompoundTag::getIntArray, CompoundTag::putIntArray),
      entry(Long.class, CompoundTag::getLong, CompoundTag::putLong),
      entry(long[].class, CompoundTag::getLongArray, CompoundTag::putLongArray)
    );
  }

  private static <T> Entry<Class<T>, Adapter<T>> entry(Class<T> classType, Reader<T> reader, Writer<T> writer) {
    return Map.entry(classType, new Adapter<>(reader, writer));
  }

  public static <T> @Nullable T read(CompoundTag tag, DataKey<T> key) {
    if (!tag.contains(key.asString())) {
      return null;
    }
    var adapter = getAdapter(key.type());
    return adapter == null ? null : adapter.reader().read(tag, key.asString());
  }

  public static <T> void write(CompoundTag tag, DataKey<T> key, T value) {
    var adapter = getAdapter(key.type());
    if (adapter != null) {
      adapter.writer().write(tag, key.asString(), value);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> @Nullable Adapter<T> getAdapter(Class<T> type) {
    return (Adapter<T>) ADAPTERS.get(type);
  }

  private record Adapter<T>(Reader<T> reader, Writer<T> writer) {
  }

  @FunctionalInterface
  private interface Reader<T> {
    T read(CompoundTag tag, String key);
  }

  @FunctionalInterface
  private interface Writer<T> {
    void write(CompoundTag tag, String key, T value);
  }
}
