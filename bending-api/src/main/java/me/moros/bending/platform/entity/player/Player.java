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

package me.moros.bending.platform.entity.player;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.item.Item;
import net.kyori.adventure.text.Component;

public interface Player extends LivingEntity {
  boolean hasPermission(String permission);

  @Override
  Inventory inventory();

  GameMode gamemode();

  boolean canSee(Entity other);

  default void sendNotification(Item item, Component title) {
    NativeAdapter.instance().sendNotification(this, item, title);
  }
}