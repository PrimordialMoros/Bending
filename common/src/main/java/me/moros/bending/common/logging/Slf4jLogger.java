/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.logging;

import org.jspecify.annotations.Nullable;

public record Slf4jLogger(org.slf4j.Logger handle) implements Logger {
  @Override
  public void debug(String msg) {
    handle().debug(msg);
  }

  @Override
  public void debug(@Nullable String msg, Throwable t) {
    handle().debug(msg, t);
  }

  @Override
  public void info(String msg) {
    handle().info(msg);
  }

  @Override
  public void info(@Nullable String msg, Throwable t) {
    handle().info(msg, t);
  }

  @Override
  public void warn(String msg) {
    handle().warn(msg);
  }

  @Override
  public void warn(@Nullable String msg, Throwable t) {
    handle().warn(msg, t);
  }

  @Override
  public void error(String msg) {
    handle().error(msg);
  }

  @Override
  public void error(@Nullable String msg, Throwable t) {
    handle().error(msg, t);
  }
}
