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

package me.moros.bending.paper.platform.block;

import me.moros.bending.api.platform.block.BlockStateProperties;
import me.moros.bending.common.data.DataProviderRegistry;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.BubbleColumn;
import org.bukkit.block.data.type.Snow;

// TODO replace when paper implements block property api
final class BukkitBlockStateProperties {
  private BukkitBlockStateProperties() {
  }

  static final DataProviderRegistry<BlockData> PROPERTIES;

  static {
    PROPERTIES = DataProviderRegistry.builder(BlockData.class)
      .create(BlockStateProperties.DRAG, BubbleColumn.class, b -> b
        .get(BubbleColumn::isDrag)
        .set(BubbleColumn::setDrag))
      .create(BlockStateProperties.LIT, Lightable.class, b -> b
        .get(Lightable::isLit)
        .set(Lightable::setLit))
      .create(BlockStateProperties.OPEN, Openable.class, b -> b
        .get(Openable::isOpen)
        .set(Openable::setOpen))
      .create(BlockStateProperties.WATERLOGGED, Waterlogged.class, b -> b
        .get(Waterlogged::isWaterlogged)
        .set(Waterlogged::setWaterlogged))
      .create(BlockStateProperties.LAYERS, Snow.class, b -> b
        .get(Snow::getLayers)
        .set(Snow::setLayers))
      .create(BlockStateProperties.LEVEL, Levelled.class, b -> b
        .get(Levelled::getLevel)
        .set(Levelled::setLevel))
      .build();
  }
}
