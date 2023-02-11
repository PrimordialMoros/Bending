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

package me.moros.bending.paper.adapter;

import java.util.function.Function;

import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.common.util.ReflectionUtil;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public final class AdapterLoader {
  private AdapterLoader() {
  }

  private static @Nullable NativeAdapter findAdapter(String className) {
    try {
      Class<?> cls = ReflectionUtil.getClassOrThrow(className);
      if (!cls.isSynthetic() && NativeAdapter.class.isAssignableFrom(cls)) {
        Function<BlockState, BlockData> dataMapper = PlatformAdapter::toBukkitData;
        return (NativeAdapter) cls.getDeclaredConstructor(Function.class).newInstance(dataMapper);
      }
    } catch (Exception ignore) {
    }
    return null;
  }

  public static NativeAdapter loadAdapter(Logger logger) {
    String fullName = Bukkit.getServer().getClass().getPackageName();
    String nmsVersion = fullName.substring(1 + fullName.lastIndexOf("."));
    String className = "me.moros.bending.paper.adapter." + nmsVersion + ".NativeAdapterImpl";
    NativeAdapter adapter = findAdapter(className);
    if (adapter != null) {
      logger.info("Successfully loaded native adapter for version " + nmsVersion);
      return adapter;
    } else {
      String s = String.format("""
                
        ****************************************************************
        * Unable to find native adapter for version %s.
        * Some features may be unsupported (for example toast notifications) or induce significant overhead.
        * Packet based abilities will utilize real entities instead which can be slower when spawned in large amounts.
        * It is recommended you find a supported version.
        ****************************************************************
                
        """, nmsVersion);
      logger.warn(s);
    }
    return NativeAdapterImpl.DUMMY;
  }
}
