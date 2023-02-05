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

package me.moros.bending.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ServerMobEvents {
  private ServerMobEvents() {
  }

  public static final Event<Target> TARGET = EventFactory.createArrayBacked(Target.class, callbacks -> (entity, target) -> {
    for (var callback : callbacks) {
      if (!callback.onEntityTarget(entity, target)) {
        return false;
      }
    }
    return true;
  });

  @FunctionalInterface
  public interface Target {
    boolean onEntityTarget(LivingEntity entity, Entity target);
  }
}
