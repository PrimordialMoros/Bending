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

package me.moros.bending.common;

import java.lang.reflect.Method;

import me.moros.bending.api.GameProvider;
import me.moros.bending.api.game.Game;

public final class GameProviderUtil {
  private GameProviderUtil() {
  }

  private static final Method REGISTER;
  private static final Method UNREGISTER;

  static {
    try {
      REGISTER = GameProvider.class.getDeclaredMethod("register", Game.class);
      REGISTER.setAccessible(true);

      UNREGISTER = GameProvider.class.getDeclaredMethod("unregister");
      UNREGISTER.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static void registerProvider(Game game) {
    try {
      REGISTER.invoke(null, game);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void unregisterProvider() {
    try {
      UNREGISTER.invoke(null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
