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

package me.moros.bending.model.raytrace;

import java.util.Set;

import me.moros.bending.model.math.Vector3d;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public record RayTraceContext(@NonNull Vector3d start, @NonNull Vector3d end,
                              boolean ignoreLiquids, boolean ignorePassable,
                              @NonNull Set<@NonNull Block> ignoreBlocks) {
}
