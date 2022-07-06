/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.adapter.impl;

import me.moros.bending.adapter.CompatibilityLayer;
import me.moros.bending.adapter.PacketAdapter;
import me.moros.bending.adapter.VersionUtil;
import me.moros.bending.adapter.VersionUtil.NMSVersion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NativeAdapter implements CompatibilityLayer {
  private static final String PACKAGE_NAME = "me.moros.bending.adapter.impl.";
  private static final CompatibilityLayer DUMMY = new NativeAdapter();

  private static CompatibilityLayer INSTANCE;

  private final PacketAdapter packetAdapter;

  private NativeAdapter() {
    packetAdapter = new DummyPacketAdapter();
  }

  @Override
  public @NonNull PacketAdapter packetAdapter() {
    return packetAdapter;
  }

  public static @NonNull CompatibilityLayer instance() {
    return INSTANCE == null ? DUMMY : INSTANCE;
  }

  public static void init() {
    if (INSTANCE != null) {
      return;
    }
    NMSVersion version = VersionUtil.nmsVersion();
    String className = PACKAGE_NAME + version + ".CompatibilityLayerImpl";
    INSTANCE = findClass(className, CompatibilityLayer.class);
  }

  private static <E> @Nullable E findClass(@NonNull String className, @NonNull Class<E> clazz) {
    try {
      Class<?> cls = Class.forName(className);
      if (!cls.isSynthetic() && clazz.isAssignableFrom(cls)) {
        return clazz.cast(cls.getDeclaredConstructor().newInstance());
      }
    } catch (Exception ignore) {
    }
    return null;
  }
}
