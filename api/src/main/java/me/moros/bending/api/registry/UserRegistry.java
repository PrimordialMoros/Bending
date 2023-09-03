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

package me.moros.bending.api.registry;

import java.util.UUID;
import java.util.stream.Stream;

import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.SimpleRegistry.SimpleMutableRegistry;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.TextUtil;

/**
 * Registry for all valid benders.
 */
public final class UserRegistry extends SimpleMutableRegistry<UUID, User> {
  UserRegistry() {
    super(User.NAMESPACE, User::uuid, TextUtil::parseUUID);
  }

  @Override
  public boolean register(User user) {
    if (super.register(user)) {
      user.game().eventBus().postUserRegisterEvent(user);
      return true;
    }
    return false;
  }

  @Override
  public boolean clear() {
    return false;
  }

  /**
   * Provides a stream of the currently registered players.
   * @return stream of online players
   */
  public Stream<User> players() {
    return stream().filter(Player.class::isInstance);
  }

  public int playersCount() {
    return (int) players().count();
  }

  public int nonPlayerCount() {
    return size() - playersCount();
  }
}
