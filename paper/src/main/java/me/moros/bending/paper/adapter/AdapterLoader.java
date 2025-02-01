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

package me.moros.bending.paper.adapter;

import io.papermc.paper.ServerBuildInfo;
import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.common.logging.Logger;

public final class AdapterLoader {
  public static final NativeAdapter DUMMY = new NativeAdapter() {
  };

  private AdapterLoader() {
  }

  public static NativeAdapter loadAdapter(Logger logger, String version) {
    String mcVersion = ServerBuildInfo.buildInfo().minecraftVersionId();
    if (version.endsWith(".0")) {
      version = version.substring(0, version.length() - 2);
    }
    if (mcVersion.equals(version)) {
      logger.info("Successfully loaded native adapter for version " + mcVersion);
      return new NativeAdapterImpl();
    } else {
      String s = """
        
        ****************************************************************
        * Unable to find native adapter for version %s.
        * Some features may be unsupported (for example toast notifications) or induce significant overhead.
        * Packet based abilities will utilize real entities instead which can be slower when spawned in large amounts.
        * It is recommended you use a supported version (%s).
        ****************************************************************
        """.formatted(mcVersion, version);
      logger.warn(s);
    }
    return DUMMY;
  }
}
