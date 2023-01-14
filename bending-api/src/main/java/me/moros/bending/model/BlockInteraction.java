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

package me.moros.bending.model;

import me.moros.bending.model.data.DataKey;
import me.moros.bending.platform.block.Block;
import me.moros.bending.util.KeyUtil;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

public record BlockInteraction(Block block, @Nullable Vector3d point) {
  public static DataKey<BlockInteraction> KEY = KeyUtil.data("last-interacted-block", BlockInteraction.class);
}
