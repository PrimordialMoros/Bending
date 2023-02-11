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

package me.moros.bending.sponge.listener;

import me.moros.bending.api.game.Game;
import me.moros.bending.sponge.platform.PlatformAdapter;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.server.ServerWorld;

abstract class SpongeListener {
  protected final Game game;

  protected SpongeListener(Game game) {
    this.game = game;
  }

  protected boolean disabledWorld(ServerWorld world) {
    return !game.worldManager().isEnabled(PlatformAdapter.fromRsk(world.key()));
  }

  protected boolean disabledWorld(Entity entity) {
    if (entity.world() instanceof ServerWorld world) {
      return disabledWorld(world);
    }
    return true;
  }
}
