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

package me.moros.bending.adapter;

import java.util.Collection;

import me.moros.bending.model.math.Vector3d;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface PacketAdapter {
  void sendNotification(@NonNull Player player, @NonNull Material material, @NonNull Component title);

  int createArmorStand(@NonNull World world, @NonNull Vector3d center, @NonNull Material material, @NonNull Vector3d velocity, boolean gravity);

  int createFallingBlock(@NonNull World world, @NonNull Vector3d center, @NonNull BlockData data, @NonNull Vector3d velocity, boolean gravity);

  void fakeBlock(@NonNull World world, @NonNull Vector3d center, @NonNull BlockData data);

  void fakeBreak(@NonNull World world, @NonNull Vector3d center, byte progress);

  void refreshBlocks(@NonNull Collection<@NonNull Block> blocks, @NonNull World world, @NonNull Vector3d center);

  default void destroy(int id) {
    destroy(new int[]{id});
  }

  void destroy(int[] ids);
}
